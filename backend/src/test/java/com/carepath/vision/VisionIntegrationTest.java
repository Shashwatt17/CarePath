package com.carepath.vision;
import com.carepath.identity.AuthTestSupport;
import com.carepath.intelligence.*;
import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class VisionIntegrationTest extends AuthTestSupport {
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate jdbc;@Autowired TestLimits limits;@Autowired TestClock clock;@Autowired ProcessingWorker worker;
    @MockitoBean VisionProvider provider;@MockitoBean ExtractionEngine engine;
    String a,b,doc,extraction;
    @BeforeEach void setup() throws Exception {
        clock.reset();limits.clear();jdbc.update("DELETE FROM audit_event");jdbc.update("DELETE FROM app_user");
        a=account("vision-a");b=account("vision-b");
        var p=TextPages.nativePage(1,"LAB REPORT\nReport date: 18/Jul/2026\nGlucose\t262\tmg/dL\t70 - 110\n");
        var scanned=new ExtractionData.Page(1,p.text(),ExtractionData.Method.TESSERACT_OCR,.8,p.lines());
        when(engine.extract(any(),any())).thenReturn(new ExtractionPipeline(null).parse(List.of(scanned)));
        when(provider.available()).thenReturn(true);when(provider.model()).thenReturn("synthetic-model");
        when(provider.read(any(),any())).thenReturn("{\"readings\":[{\"testName\":\"Glucose\",\"value\":\"26.2\",\"unit\":\"mg/dL\",\"referenceRange\":\"70-110\",\"date\":\"2026-07-18\",\"column\":\"Current\",\"box\":null}],\"unreadable\":false}");
        doc=body(mvc.perform(multipart("/api/v1/documents").file(new MockMultipartFile("file","synthetic.pdf","application/pdf",Files.readAllBytes(Path.of("../sample-data/phase4/january-lab.pdf")))).file(new MockMultipartFile("metadata","","application/json","{\"documentType\":\"LAB_REPORT\",\"tags\":[]}".getBytes())).header("Authorization","Bearer "+a)).andExpect(status().isCreated())).path("id").asText();
        mvc.perform(post("/api/v1/documents/"+doc+"/process").header("Authorization","Bearer "+a)).andExpect(status().isAccepted());assertTrue(worker.runOne());
        extraction=body(mvc.perform(get("/api/v1/documents/"+doc+"/extraction").header("Authorization","Bearer "+a)).andExpect(status().isOk())).path("id").asText();
    }
    String account(String name) throws Exception {
        var registration=Map.of("email",name+"@example.invalid","password","Synthetic password 123!","displayName","Synthetic");
        mvc.perform(post("/api/v1/auth/register").header("Origin","http://localhost:3000").header("X-CarePath-Client","web").contentType("application/json").content(json.writeValueAsString(registration))).andExpect(status().isCreated());
        return body(mvc.perform(post("/api/v1/auth/login").header("Origin","http://localhost:3000").header("X-CarePath-Client","web").contentType("application/json").content(json.writeValueAsString(Map.of("email",registration.get("email"),"password",registration.get("password"))))).andExpect(status().isOk())).path("accessToken").asText();
    }
    JsonNode body(ResultActions a) throws Exception{return json.readTree(a.andReturn().getResponse().getContentAsString());}
    ResultActions read(String token,boolean consent,String version,String eid) throws Exception{return mvc.perform(post("/api/v1/documents/"+doc+"/vision").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(Map.of("extractionId",eid,"page",1,"consent",consent,"consentVersion",version))));}
    @Test void noTransmissionWithoutCurrentConsent() throws Exception{read(a,false,VisionData.CONSENT,extraction).andExpect(status().isUnprocessableEntity());read(a,true,"old",extraction).andExpect(status().isUnprocessableEntity());verify(provider,never()).read(any(),any());assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM vision_page_proposal",Integer.class));}
    @Test void ownerIsolationBeforeProviderAccess() throws Exception{read(b,true,VisionData.CONSENT,extraction).andExpect(status().isNotFound());mvc.perform(get("/api/v1/documents/"+doc+"/vision/1").header("Authorization","Bearer "+b)).andExpect(status().isNotFound());verify(provider,never()).read(any(),any());}
    @Test void proposalsAreCachedAndNeverOverwriteReadings() throws Exception{var first=body(read(a,true,VisionData.CONSENT,extraction).andExpect(status().isOk()));assertEquals("SUCCEEDED",first.path("state").asText());read(a,true,VisionData.CONSENT,extraction).andExpect(status().isOk());verify(provider,times(1)).read(any(),any());assertEquals("262",jdbc.queryForObject("SELECT original_value FROM extraction_candidate WHERE document_id=?",String.class,UUID.fromString(doc)));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM medical_observation",Integer.class));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='VISION_PAGE_REQUESTED'",Integer.class));}
    @Test void invalidProviderOutputFailsClosedWithoutRetry() throws Exception{when(provider.read(any(),any())).thenReturn("{\"confirmed\":true}");read(a,true,VisionData.CONSENT,extraction).andExpect(status().isBadGateway());assertEquals("FAILED",body(read(a,true,VisionData.CONSENT,extraction).andExpect(status().isOk())).path("state").asText());verify(provider,times(1)).read(any(),any());assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM medical_observation",Integer.class));}
    @Test void disabledAndStaleRequestsDoNotCallProvider() throws Exception{when(provider.available()).thenReturn(false);read(a,true,VisionData.CONSENT,extraction).andExpect(status().isServiceUnavailable());read(a,true,VisionData.CONSENT,UUID.randomUUID().toString()).andExpect(status().isConflict());verify(provider,never()).read(any(),any());}
    @Test void dailyBudgetBlocksProvider() throws Exception {
        UUID owner=jdbc.queryForObject("SELECT owner_id FROM medical_document WHERE id=?",UUID.class,UUID.fromString(doc));
        for(int i=0;i<10;i++)jdbc.update("INSERT INTO audit_event(id,owner_id,action,resource_type,resource_id,outcome,actor_kind) VALUES(?,?,'VISION_PAGE_REQUESTED','MEDICAL_DOCUMENT',?,'SUCCESS','OWNER')",UUID.randomUUID(),owner,UUID.randomUUID());
        read(a,true,VisionData.CONSENT,extraction).andExpect(status().isTooManyRequests());verify(provider,never()).read(any(),any());
    }
    @Test void anonymousAndUnsupportedMethodsAreDenied() throws Exception {
        mvc.perform(get("/api/v1/documents/"+doc+"/vision")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/documents/"+doc+"/vision").header("Authorization","Bearer "+a)).andExpect(status().isForbidden());
    }
    @Test void deletingDocumentRemovesProposals() throws Exception{read(a,true,VisionData.CONSENT,extraction).andExpect(status().isOk());mvc.perform(delete("/api/v1/documents/"+doc).header("Authorization","Bearer "+a)).andExpect(status().isNoContent());assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM vision_page_proposal",Integer.class));}
}
