package com.carepath.longitudinal;
import com.carepath.identity.AuthTestSupport;
import com.carepath.intelligence.ProcessingWorker;
import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class HistoryIntegrationTest extends AuthTestSupport {
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate jdbc;@Autowired TestLimits limits;@Autowired TestClock clock;@Autowired ProcessingWorker worker;
    String a,b;
    @BeforeEach void setup() throws Exception {clock.reset();limits.clear();jdbc.update("DELETE FROM audit_event");jdbc.update("DELETE FROM app_user");a=account("review-a");b=account("review-b");}
    String account(String name) throws Exception {
        var registration=Map.of("email",name+"@example.invalid","password","Synthetic password 123!","displayName","Synthetic");
        mvc.perform(post("/api/v1/auth/register").header("Origin","http://localhost:3000").header("X-CarePath-Client","web").contentType("application/json").content(json.writeValueAsString(registration))).andExpect(status().isCreated());
        return json.readTree(mvc.perform(post("/api/v1/auth/login").header("Origin","http://localhost:3000").header("X-CarePath-Client","web").contentType("application/json").content(json.writeValueAsString(Map.of("email",registration.get("email"),"password",registration.get("password"))))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("accessToken").asText();
    }
    String process(String token,String file) throws Exception {
        String id=json.readTree(mvc.perform(multipart("/api/v1/documents").file(new MockMultipartFile("file",file,"application/pdf",Files.readAllBytes(Path.of(file.equals("normalization-lab.pdf")?"../sample-data/phase5":"../sample-data/phase4",file)))).file(new MockMultipartFile("metadata","","application/json","{\"documentType\":\"LAB_REPORT\",\"tags\":[]}".getBytes())).header("Authorization","Bearer "+token)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asText();
        mvc.perform(post("/api/v1/documents/"+id+"/process").header("Authorization","Bearer "+token)).andExpect(status().isAccepted());assertTrue(worker.runOne());return id;
    }
    JsonNode queue(String token,String doc) throws Exception {return body(mvc.perform(get("/api/v1/review/candidates").param("documentId",doc).header("Authorization","Bearer "+token)).andExpect(status().isOk()));}
    JsonNode body(ResultActions action) throws Exception {return json.readTree(action.andReturn().getResponse().getContentAsString());}
    ResultActions action(String token,String id,String action,Object value) throws Exception {return mvc.perform(post("/api/v1/review/candidates/"+id+"/"+action).header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(value)));}
    Map<String,Object> correction(JsonNode c,String value,String unit,String concept) {
        var fields=new LinkedHashMap<String,Object>();var raw=c.path("extracted");
        fields.put("testName",raw.path("originalTestName").asText());fields.put("value",value);fields.put("unit",unit);fields.put("referenceRange",raw.path("referenceText").isNull()?null:raw.path("referenceText").asText());fields.put("date",raw.path("reportDate").isNull()?null:raw.path("reportDate").asText());fields.put("conceptId",concept);
        return Map.of("version",c.path("version").asLong(),"fields",fields,"sourceReviewed",true,"reason","Compared carefully with the synthetic source.");
    }

    JsonNode read(String token,String path) throws Exception {return body(mvc.perform(get("/api/v1/history/"+path).header("Authorization","Bearer "+token)).andExpect(status().isOk()));}
    List<JsonNode> accept(String token,String doc) throws Exception {
        List<JsonNode> out=new ArrayList<>();for(var c:queue(token,doc).path("items")) {
            String id=c.path("id").asText();JsonNode result;
            if(c.path("decision").path("canConfirm").asBoolean())result=body(action(token,id,"confirm",Map.of("version",0)).andExpect(status().isOk()));
            else result=body(action(token,id,"correct",correction(c,"1.8","mIU/L",null)).andExpect(status().isOk()));
            out.add(result.path("observation"));
        }return out;
    }
    String concept(String name) {return jdbc.queryForObject("SELECT id FROM canonical_medical_concept WHERE canonical_name=?",UUID.class,name).toString();}
    @Test void independentOutOfOrderReportsProduceExactChronologyChangesAndEvidence() throws Exception {
        String sep=process(a,"september-lab.pdf");accept(a,sep);String jan=process(a,"january-lab.pdf");accept(a,jan);String apr=process(a,"april-lab.pdf");accept(a,apr);
        var h=read(a,"concepts/"+concept("Hemoglobin"));var pts=h.path("history").path("items");assertEquals(3,pts.size());
        assertEquals(List.of("2026-01-12","2026-04-12","2026-09-12"),List.of(pts.get(0).path("date").asText(),pts.get(1).path("date").asText(),pts.get(2).path("date").asText()));
        assertEquals(List.of("12.1","11.3","10.4"),List.of(pts.get(0).path("normalizedValue").asText(),pts.get(1).path("normalizedValue").asText(),pts.get(2).path("normalizedValue").asText()));assertEquals("DECREASED",h.path("trend").path("series").get(0).path("pattern").asText());
        var changes=read(a,"changes?previousReport="+jan+"&currentReport="+sep).path("changes");Map<String,String> expected=Map.of("Hemoglobin","DECREASED","Vitamin D","INCREASED","Sodium","APPROXIMATELY_STABLE","Ferritin","NEWLY_OBSERVED","TSH","NEWLY_OBSERVED");int provenance=0;
        for(var change:changes){assertEquals(expected.get(change.path("concept").asText()),change.path("type").asText());for(String side:List.of("previous","current")){var point=change.path(side);if(point.isNull())continue;var ev=point.path("evidence");assertEquals(side.equals("previous")?jan:sep,ev.path("documentId").asText());assertEquals(side.equals("previous")?1:2,ev.path("page").asInt());var raw=body(mvc.perform(get("/api/v1/documents/"+ev.path("documentId").asText()+"/extraction/evidence/"+ev.path("candidateId").asText()).header("Authorization","Bearer "+a)).andExpect(status().isOk()));assertEquals(raw.path("text"),ev.path("text"));provenance++;}}
        assertEquals(5,changes.size());assertEquals(8,provenance);assertEquals(3,read(a,"events?conceptId="+concept("Hemoglobin")+"&size=1&page=2").path("total").asInt());assertEquals(1,read(a,"events?q=Hemoglobin&size=1").path("items").size());
        Files.createDirectories(Path.of("../evaluation/results"));Files.writeString(Path.of("../evaluation/results/phase6-history.json"),json.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("label","SYNTHETIC DEMO DATA — NOT A REAL PATIENT","executedAt",java.time.Instant.now().toString(),"groupedChronologicalPointsCorrect",3,"groupedChronologicalPointsTotal",3,"changeLabelsCorrect",5,"changeLabelsTotal",5,"sourceReferencesCorrect",provenance,"sourceReferencesTotal",8,"scope","Actual upload/extract/verify/history API on authored synthetic reports; not clinical accuracy"))+"\n");
    }
    @Test void pendingRejectedAndUnverifiedNeverEnterHistory() throws Exception {
        String doc=process(a,"january-lab.pdf");assertEquals(0,read(a,"concepts").path("total").asInt());assertEquals(1,read(a,"events").path("total").asInt());
        var rows=queue(a,doc).path("items");action(a,rows.get(0).path("id").asText(),"reject",Map.of("version",0)).andExpect(status().isOk());var accepted=body(action(a,rows.get(1).path("id").asText(),"confirm",Map.of("version",0)).andExpect(status().isOk())).path("observation");
        assertEquals(1,read(a,"concepts").path("total").asInt());jdbc.update("UPDATE medical_observation SET verification_status='PENDING' WHERE id=?",UUID.fromString(accepted.path("id").asText()));assertEquals(0,read(a,"concepts").path("total").asInt());
    }
    @Test void bothDirectionsIdorIncludingAggregationsAndEvidence() throws Exception {
        for(var pair:List.of(new String[]{a,b},new String[]{b,a})){String doc=process(pair[0],"january-lab.pdf");var obs=accept(pair[0],doc).getFirst();String id=obs.path("id").asText();String conceptId=obs.path("verified").path("conceptId").asText();
            for(String path:List.of("events?documentId="+doc,"concepts/"+conceptId,"events?conceptId="+conceptId,"observations/"+id+"/evidence","compare?previous="+id+"&current="+id,"changes?previousReport="+doc+"&currentReport="+doc)) {
                // A shared canonical concept may legitimately have the other user's history; resource/document/observation IDs must still be denied.
                if(path.contains("concept") && repositoryHasConcept(pair[1],conceptId)){var owned=read(pair[1],path);var list=owned.has("history")?owned.path("history").path("items"):owned.path("items");for(var item:list){var evidence=item.has("evidence")?item.path("evidence"):item.path("observation").path("evidence");assertNotEquals(doc,evidence.path("documentId").asText());}continue;}
                mvc.perform(get("/api/v1/history/"+path).header("Authorization","Bearer "+pair[1])).andExpect(status().isNotFound());
            }
            var events=read(pair[1],"events").path("items");for(var event:events)assertNotEquals(doc,event.path("documentId").asText());
            mvc.perform(get("/api/v1/documents/"+doc+"/preview").header("Authorization","Bearer "+pair[1])).andExpect(status().isNotFound());
        }
    }
    boolean repositoryHasConcept(String token,String id)throws Exception {for(var c:read(token,"concepts").path("items"))if(c.path("id").asText().equals(id))return true;return false;}
    @Test void sourceDeletionAndFutureCorrectionRecomputeWithoutCache() throws Exception {
        String jan=process(a,"january-lab.pdf");accept(a,jan);String apr=process(a,"april-lab.pdf");accept(a,apr);String id=concept("Hemoglobin");var rows=read(a,"concepts/"+id).path("history").path("items");
        jdbc.update("UPDATE medical_observation SET normalized_value=15,verified_numeric_value=15 WHERE id=?",UUID.fromString(rows.get(1).path("id").asText()));assertEquals("INCREASED",read(a,"compare?previous="+rows.get(0).path("id").asText()+"&current="+rows.get(1).path("id").asText()).path("type").asText());
        mvc.perform(delete("/api/v1/documents/"+jan).header("Authorization","Bearer "+a)).andExpect(status().isNoContent());assertEquals(1,read(a,"concepts/"+id).path("history").path("total").asInt());mvc.perform(get("/api/v1/history/observations/"+rows.get(0).path("id").asText()+"/evidence").header("Authorization","Bearer "+a)).andExpect(status().isNotFound());
    }
    @Test void sameDayAndUndatedRemainVisibleWithoutInventedTrend() throws Exception {
        String doc=process(a,"january-lab.pdf");accept(a,doc);String other=process(a,"january-lab.pdf");accept(a,other);String id=concept("Hemoglobin");var h=read(a,"concepts/"+id);assertEquals(2,h.path("history").path("total").asInt());assertEquals(0,h.path("trend").path("series").size());
        jdbc.update("UPDATE medical_observation SET observed_date=NULL WHERE document_id=?",UUID.fromString(other));h=read(a,"concepts/"+id);assertTrue(h.path("history").path("items").get(1).path("date").isNull());assertTrue(h.path("trend").path("limitations").toString().contains("DATE_MISSING"));
    }
    @Test void missingMeasurementsRequireExplicitPanelAndCompleteReview() throws Exception {
        String jan=process(a,"january-lab.pdf");accept(a,jan);String apr=process(a,"april-lab.pdf");var accepted=accept(a,apr);
        for(var o:accepted)if(o.path("verified").path("testName").asText().equals("Vitamin D"))jdbc.update("DELETE FROM extraction_candidate WHERE id=?",UUID.fromString(o.path("candidateId").asText()));
        var result=read(a,"changes?previousReport="+jan+"&currentReport="+apr);assertMissingType(result,"INSUFFICIENT_EVIDENCE");
        // Test-only panel evidence: append an exact heading after existing snippets; original offsets remain valid.
        jdbc.update("UPDATE document_page SET extracted_text=extracted_text || CHR(10) || 'CBC' WHERE document_id IN (?,?)",UUID.fromString(jan),UUID.fromString(apr));
        assertMissingType(read(a,"changes?previousReport="+jan+"&currentReport="+apr),"PREVIOUSLY_TRACKED_NOT_PRESENT");
        jdbc.update("UPDATE medical_document SET status='NEEDS_REVIEW' WHERE id=?",UUID.fromString(apr));assertMissingType(read(a,"changes?previousReport="+jan+"&currentReport="+apr),"INSUFFICIENT_EVIDENCE");
    }
    void assertMissingType(JsonNode result,String expected){for(var c:result.path("changes"))if(c.path("concept").asText().equals("Vitamin D")){assertEquals(expected,c.path("type").asText());return;}fail("Expected Vitamin D row");}
    @Test void validatedPaginationSearchAndNoPublicHistory() throws Exception {mvc.perform(get("/api/v1/history/events")).andExpect(status().isUnauthorized());for(String q:List.of("size=101","page=-1","conceptId=bad","q="+"a".repeat(81)))mvc.perform(get("/api/v1/history/events?"+q).header("Authorization","Bearer "+a)).andExpect(status().isBadRequest());assertEquals(0,read(a,"events?q=%25").path("total").asInt());mvc.perform(post("/api/v1/history/events").header("Authorization","Bearer "+a)).andExpect(status().isForbidden());}
    @Test void boundedHistoryWindowAndPaginationNeverDropTheVisibleTotal() throws Exception {
        String doc=process(a,"january-lab.pdf");accept(a,doc);String id=concept("Hemoglobin");
        var history=read(a,"concepts/"+id+"?size=1&page=0");assertEquals(1,history.path("history").path("total").asInt());assertFalse(history.path("trend").path("truncated").asBoolean());
        assertEquals(500,history.path("trend").path("windowLimit").asInt());assertEquals(0,read(a,"concepts/"+id+"?size=1&page=1").path("history").path("items").size());
    }

}
