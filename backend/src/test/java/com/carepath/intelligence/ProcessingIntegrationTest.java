package com.carepath.intelligence;
import com.carepath.identity.AuthTestSupport;
import com.carepath.vault.*;
import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class ProcessingIntegrationTest extends AuthTestSupport {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc;
    @Autowired TestClock clock; @Autowired TestLimits limits; @Autowired ProcessingWorker worker;
    @Autowired ProcessingJobs jobs; @Autowired org.springframework.transaction.PlatformTransactionManager manager;
    @Autowired VaultProperties vault;
    String a,b;
    @BeforeEach void setup() throws Exception {
        clock.reset();limits.clear();jdbc.update("DELETE FROM audit_event");jdbc.update("DELETE FROM app_user");
        a=account("extract-a");b=account("extract-b");
    }
    String account(String name) throws Exception {
        var data=Map.of("email",name+"@example.invalid","password","Synthetic password 123!","displayName","Synthetic");
        mvc.perform(post("/api/v1/auth/register").header("Origin","http://localhost:3000").header("X-CarePath-Client","web").contentType("application/json").content(json.writeValueAsString(data))).andExpect(status().isCreated());
        return json.readTree(mvc.perform(post("/api/v1/auth/login").header("Origin","http://localhost:3000").header("X-CarePath-Client","web").contentType("application/json").content(json.writeValueAsString(Map.of("email",data.get("email"),"password",data.get("password"))))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("accessToken").asText();
    }
    String login(String name) throws Exception {
        limits.clear();
        return json.readTree(mvc.perform(post("/api/v1/auth/login").header("Origin","http://localhost:3000").header("X-CarePath-Client","web").contentType("application/json").content(json.writeValueAsString(Map.of("email",name+"@example.invalid","password","Synthetic password 123!")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("accessToken").asText();
    }
    String upload(String token,String name) throws Exception {
        return json.readTree(mvc.perform(multipart("/api/v1/documents")
            .file(new MockMultipartFile("file",name,"application/pdf",Files.readAllBytes(ExtractionRuntimeTest.sample(name))))
            .file(new MockMultipartFile("metadata","","application/json","{\"documentType\":\"OTHER\",\"tags\":[]}".getBytes()))
            .header("Authorization","Bearer "+token)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asText();
    }
    ResultActions process(String token,String id) throws Exception { return mvc.perform(post("/api/v1/documents/"+id+"/process").header("Authorization","Bearer "+token)); }
    JsonNode extraction(String token,String id) throws Exception { return json.readTree(mvc.perform(get("/api/v1/documents/"+id+"/extraction").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()); }
    void state(String id,String expected) throws Exception { mvc.perform(get("/api/v1/documents/"+id+"/processing-status").header("Authorization","Bearer "+a)).andExpect(status().isOk()).andExpect(jsonPath("$.state").value(expected)); }
    @Test void realPipelinePersistsCandidatesProvenanceAndNeverOverwritesUserCategory() throws Exception {
        String id=upload(a,"january-lab.pdf");
        process(a,id).andExpect(status().isAccepted()).andExpect(jsonPath("$.state").value("PROCESSING"));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM document_extraction",Integer.class));
        assertTrue(worker.runOne());state(id,"NEEDS_REVIEW");
        var result=extraction(a,id).path("result");assertEquals(3,result.path("candidates").size());
        assertEquals("LAB_REPORT",result.path("classification").path("category").asText());
        for(var c:result.path("candidates")) {
            var source=c.path("source");String page=result.path("pages").get(source.path("page").asInt()-1).path("text").asText();
            assertEquals(source.path("text").asText(),page.substring(source.path("start").asInt(),source.path("end").asInt()));
            mvc.perform(get("/api/v1/documents/"+id+"/extraction/evidence/"+c.path("id").asText()).header("Authorization","Bearer "+a)).andExpect(status().isOk()).andExpect(jsonPath("$.text").value(source.path("text").asText()));
        }
        mvc.perform(get("/api/v1/documents/"+id).header("Authorization","Bearer "+a)).andExpect(jsonPath("$.documentType").value("OTHER"));
        mvc.perform(get("/api/v1/documents/"+id+"/download").header("Authorization","Bearer "+a)).andExpect(content().bytes(Files.readAllBytes(ExtractionRuntimeTest.sample("january-lab.pdf"))));
        for(String action:List.of("PROCESSING_REQUESTED","PROCESSING_SUCCEEDED","EXTRACTION_VIEWED")) assertTrue(jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action=?",Integer.class,action)>0);
        assertFalse(jdbc.queryForList("SELECT * FROM audit_event").toString().contains("Hemoglobin"));
    }
    @Test void uncertainValueStaysCandidateAndPageTwoEvidenceIsExact() throws Exception {
        String id=upload(a,"september-lab.pdf");process(a,id);worker.runOne();state(id,"NEEDS_REVIEW");
        var c=extraction(a,id).path("result").path("candidates").get(4);
        assertEquals("TSH",c.path("originalTestName").asText());assertEquals("l.8?",c.path("originalValue").asText());assertTrue(c.path("numericValue").isNull());assertEquals("LOW",c.path("confidence").asText());assertEquals(2,c.path("source").path("page").asInt());
        assertTrue(c.path("source").path("text").asText().contains("TSH | l.8?"));
    }
    @Test void bidirectionalIdorThroughActualSecurityStack() throws Exception {
        for(String[] pair:List.of(new String[]{a,b},new String[]{b,a})) {
            String id=upload(pair[0],"january-lab.pdf");process(pair[0],id);worker.runOne();
            String candidate=extraction(pair[0],id).path("result").path("candidates").get(0).path("id").asText();
            for(String suffix:List.of("/processing-status","/extraction","/extraction/evidence/"+candidate)) mvc.perform(get("/api/v1/documents/"+id+suffix).header("Authorization","Bearer "+pair[1])).andExpect(status().isNotFound());
            for(String suffix:List.of("/process","/retry")) mvc.perform(post("/api/v1/documents/"+id+suffix).header("Authorization","Bearer "+pair[1])).andExpect(status().isNotFound());
            String own=upload(pair[1],"april-lab.pdf");
            mvc.perform(get("/api/v1/documents/"+own+"/extraction/evidence/"+candidate).header("Authorization","Bearer "+pair[1])).andExpect(status().isNotFound());
        }
    }
    @Test void simultaneousRequestsCreateOnlyOneJobAndOneExtraction() throws Exception {
        String id=upload(a,"april-lab.pdf");
        try(var pool=Executors.newFixedThreadPool(2)) {
            var x=pool.submit(()->process(a,id).andReturn().getResponse().getStatus());var y=pool.submit(()->process(a,id).andReturn().getResponse().getStatus());
            assertEquals(202,x.get());assertEquals(202,y.get());
        }
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM processing_job",Integer.class));worker.runOne();assertFalse(worker.runOne());
        process(a,id).andExpect(status().isAccepted());assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM document_extraction",Integer.class));
    }
    @Test void expiredWorkerLeaseIsReclaimedAndOldFenceCannotCommit() throws Exception {
        String id=upload(a,"january-lab.pdf");process(a,id);var tx=new TransactionTemplate(manager);
        var old=tx.execute(s->jobs.claim(5).orElseThrow());clock.advance(66);
        var current=tx.execute(s->jobs.claim(5).orElseThrow());assertNotEquals(old.fence(),current.fence());
        assertEquals(false,tx.execute(s->jobs.current(old)));assertEquals(true,tx.execute(s->jobs.current(current)));
        clock.advance(66);assertTrue(worker.runOne());a=login("extract-a");state(id,"NEEDS_REVIEW");
    }
    @Test void retryableFailuresBackOffThenRequireExplicitBoundedRetry() throws Exception {
        String id=upload(a,"january-lab.pdf");process(a,id);var tx=new TransactionTemplate(manager);
        for(int i=1;i<=3;i++) {
            var job=tx.execute(s->jobs.claim(5).orElseThrow());
            tx.executeWithoutResult(s->{boolean retry=jobs.failure(job,new ProcessingFailure("OCR_UNAVAILABLE",true));jobs.documentState(job.document(),job.owner(),retry?"PROCESSING":"FAILED","OCR_UNAVAILABLE");});
            assertEquals(true,tx.execute(s->jobs.claim(5).isEmpty()));clock.advance(50);
        }
        a=login("extract-a");state(id,"FAILED");
        mvc.perform(post("/api/v1/documents/"+id+"/retry").header("Authorization","Bearer "+a)).andExpect(status().isAccepted()).andExpect(jsonPath("$.state").value("PROCESSING"));
        worker.runOne();state(id,"NEEDS_REVIEW");assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM processing_job",Integer.class));
    }
    @Test void integrityFailureIsPermanentAndPreservesOriginalBlob() throws Exception {
        String id=upload(a,"january-lab.pdf");String key=jdbc.queryForObject("SELECT storage_key FROM medical_document WHERE id=?",String.class,UUID.fromString(id));Path file=Path.of(vault.root()).resolve(key);Files.writeString(file,"tampered synthetic");
        process(a,id);worker.runOne();state(id,"FAILED");
        mvc.perform(post("/api/v1/documents/"+id+"/retry").header("Authorization","Bearer "+a)).andExpect(status().isConflict());
        assertTrue(Files.exists(file));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM document_extraction",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='DOCUMENT_INTEGRITY_FAILURE'",Integer.class));
    }
    @Test void deletionCascadesExtractionAndQueuedJobs() throws Exception {
        String id=upload(a,"january-lab.pdf");process(a,id);worker.runOne();
        mvc.perform(delete("/api/v1/documents/"+id).header("Authorization","Bearer "+a)).andExpect(status().isNoContent());
        for(String table:List.of("processing_job","document_extraction","document_page","extraction_candidate")) assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM "+table,Integer.class));
        mvc.perform(get("/api/v1/documents/"+id+"/extraction").header("Authorization","Bearer "+a)).andExpect(status().isNotFound());
    }
    @Test void queueQuotaValidationAndUnauthenticatedRoutes() throws Exception {
        for(int i=0;i<5;i++) process(a,upload(a,"january-lab.pdf")).andExpect(status().isAccepted());
        String id=upload(a,"january-lab.pdf");process(a,id).andExpect(status().isTooManyRequests());
        for(String suffix:List.of("/process","/retry")) mvc.perform(post("/api/v1/documents/"+id+suffix)).andExpect(status().isUnauthorized());
        for(String suffix:List.of("/processing-status","/extraction","/extraction/evidence/"+UUID.randomUUID())) mvc.perform(get("/api/v1/documents/"+id+suffix)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/documents/invalid/process").header("Authorization","Bearer "+a)).andExpect(status().isBadRequest());
    }
}
