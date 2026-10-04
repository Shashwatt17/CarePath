package com.carepath.review;
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
class ReviewIntegrationTest extends AuthTestSupport {
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
    @Test void confirmCreatesSingleTrustedObservationWithRawProvenanceAndAudit() throws Exception {
        String doc=process(a,"january-lab.pdf");var rows=queue(a,doc).path("items");assertEquals(3,rows.size());var c=rows.get(0);String id=c.path("id").asText();
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM trusted_medical_observation",Integer.class));
        var result=body(action(a,id,"confirm",Map.of("version",0)).andExpect(status().isOk()));var obs=result.path("observation");
        assertEquals("CONFIRMED",obs.path("verificationStatus").asText());assertEquals(c.path("extracted"),obs.path("original"));assertEquals(c.path("extracted").path("source"),obs.path("source"));
        String before=jdbc.queryForObject("SELECT original_value FROM extraction_candidate WHERE id=?",String.class,UUID.fromString(id));
        action(a,id,"confirm",Map.of("version",0)).andExpect(status().isOk());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM medical_observation",Integer.class));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM candidate_verification",Integer.class));
        assertEquals(before,jdbc.queryForObject("SELECT original_value FROM extraction_candidate WHERE id=?",String.class,UUID.fromString(id)));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='EXTRACTION_CONFIRMED'",Integer.class));
        mvc.perform(get("/api/v1/observations/"+obs.path("id").asText()).header("Authorization","Bearer "+a)).andExpect(status().isOk());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM trusted_medical_observation",Integer.class));
    }
    @Test void uncertainCorrectionPreservesMachineValueAndPageTwoSource() throws Exception {
        String doc=process(a,"september-lab.pdf");var rows=queue(a,doc).path("items");JsonNode c=null;for(var row:rows) if(row.path("extracted").path("originalTestName").asText().equals("TSH")) c=row;assertNotNull(c);
        String id=c.path("id").asText();action(a,id,"confirm",Map.of("version",0)).andExpect(status().isUnprocessableEntity());
        var result=body(action(a,id,"correct",correction(c,"1.8","mIU/L",null)).andExpect(status().isOk()));
        var obs=result.path("observation");assertEquals("l.8?",obs.path("original").path("originalValue").asText());assertEquals("1.8",obs.path("verified").path("value").asText());
        assertEquals("1.8",obs.path("normalization").path("normalized").path("value").asText());assertEquals(2,obs.path("source").path("page").asInt());assertEquals("CORRECTED",obs.path("verificationStatus").asText());
        assertEquals("l.8?",jdbc.queryForObject("SELECT original_value FROM extraction_candidate WHERE id=?",String.class,UUID.fromString(id)));
        assertEquals("value",jdbc.queryForObject("SELECT changed_fields FROM candidate_verification WHERE candidate_id=?",String.class,UUID.fromString(id)));
    }
    @Test void rejectCannotBecomeConfirmedOrCreateObservation() throws Exception {var c=queue(a,process(a,"january-lab.pdf")).path("items").get(0);String id=c.path("id").asText();action(a,id,"reject",Map.of("version",0,"reason","Not applicable")).andExpect(status().isOk());action(a,id,"confirm",Map.of("version",0)).andExpect(status().isConflict());action(a,id,"correct",correction(c,"10","g/dL",null)).andExpect(status().isConflict());assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM medical_observation",Integer.class));}
    @Test void simultaneousConfirmationIsIdempotent() throws Exception {
        String id=queue(a,process(a,"january-lab.pdf")).path("items").get(0).path("id").asText();
        try(var pool=Executors.newFixedThreadPool(2)) {var x=pool.submit(()->action(a,id,"confirm",Map.of("version",0)).andReturn().getResponse().getStatus());var y=pool.submit(()->action(a,id,"confirm",Map.of("version",0)).andReturn().getResponse().getStatus());assertEquals(200,x.get());assertEquals(200,y.get());}
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM trusted_medical_observation",Integer.class));
    }
    @Test void staleCorrectionAndChangedReplayRejected() throws Exception {
        var c=queue(a,process(a,"january-lab.pdf")).path("items").get(0);String id=c.path("id").asText();
        action(a,id,"correct",Map.of("version",1,"fields",correction(c,"10",c.path("extracted").path("originalUnit").asText(),null).get("fields"),"sourceReviewed",true,"reason","Synthetic correction reason")).andExpect(status().isConflict());
        var request=correction(c,c.path("extracted").path("originalValue").asText(),c.path("extracted").path("originalUnit").asText(),null);
        action(a,id,"correct",request).andExpect(status().isOk());action(a,id,"correct",request).andExpect(status().isOk());
        action(a,id,"correct",correction(c,"99",c.path("extracted").path("originalUnit").asText(),null)).andExpect(status().isConflict());
    }
    @Test void ownerIsolationInBothDirectionsIncludingNestedEvidenceAndObservation() throws Exception {
        for(var pair:List.of(new String[]{a,b},new String[]{b,a})) {
            String doc=process(pair[0],"january-lab.pdf");var c=queue(pair[0],doc).path("items").get(0);String id=c.path("id").asText();
            mvc.perform(get("/api/v1/review/candidates").param("documentId",doc).header("Authorization","Bearer "+pair[1])).andExpect(status().isNotFound());
            var other=body(mvc.perform(get("/api/v1/review/candidates").header("Authorization","Bearer "+pair[1])).andExpect(status().isOk()));for(var row:other.path("items")) assertNotEquals(doc,row.path("documentId").asText());
            mvc.perform(get("/api/v1/review/candidates/"+id).header("Authorization","Bearer "+pair[1])).andExpect(status().isNotFound());
            for(String action:List.of("confirm","reject")) action(pair[1],id,action,Map.of("version",0)).andExpect(status().isNotFound());
            action(pair[1],id,"correct",correction(c,"10","g/dL",null)).andExpect(status().isNotFound());
            action(pair[1],id,"preview",correction(c,"10","g/dL",null).get("fields")).andExpect(status().isNotFound());
            var obs=body(action(pair[0],id,"confirm",Map.of("version",0)).andExpect(status().isOk())).path("observation").path("id").asText();
            mvc.perform(get("/api/v1/observations/"+obs).header("Authorization","Bearer "+pair[1])).andExpect(status().isNotFound());
        }
    }
    @Test void massAssignmentAndArbitraryConceptRejected() throws Exception {
        var c=queue(a,process(a,"january-lab.pdf")).path("items").get(0);String id=c.path("id").asText();
        for(String field:List.of("ownerId","verifiedBy","documentId","confidence","source","verificationStatus")) action(a,id,"confirm",Map.of("version",0,field,UUID.randomUUID())).andExpect(status().isBadRequest());
        action(a,id,"correct",correction(c,"10","g/dL",UUID.randomUUID().toString())).andExpect(status().isUnprocessableEntity());
        mvc.perform(put("/api/v1/observations/"+UUID.randomUUID()).header("Authorization","Bearer "+a).contentType("application/json").content("{}")).andExpect(status().isForbidden());
    }
    @Test void invalidUnitNumberRangeAndUnacknowledgedSourceCannotBeConfirmed() throws Exception {
        var rows=queue(a,process(a,"january-lab.pdf")).path("items");JsonNode c=null;for(var r:rows)if(r.path("extracted").path("originalTestName").asText().equals("Hemoglobin"))c=r;assertNotNull(c);String id=c.path("id").asText();
        action(a,id,"correct",correction(c,"10","mmol/L",null)).andExpect(status().isUnprocessableEntity());
        action(a,id,"correct",correction(c,"NaN","g/dL",null)).andExpect(status().isUnprocessableEntity());
        var fields=new LinkedHashMap<Object,Object>((Map<?,?>)correction(c,"10","g/dL",null).get("fields"));fields.put("referenceRange","16 - 12");
        action(a,id,"correct",Map.of("version",0,"fields",fields,"sourceReviewed",true,"reason","Checked synthetic source")).andExpect(status().isUnprocessableEntity());
        action(a,id,"correct",Map.of("version",0,"fields",fields,"sourceReviewed",false,"reason","Checked synthetic source")).andExpect(status().isBadRequest());
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM medical_observation",Integer.class));
    }
    @Test void allResolvedCompletesDocumentAndDeletionRemovesAllDerivatives() throws Exception {
        String doc=process(a,"april-lab.pdf");mvc.perform(get("/api/v1/documents/"+doc).header("Authorization","Bearer "+a)).andExpect(jsonPath("$.status").value("NEEDS_REVIEW"));
        for(var c:queue(a,doc).path("items")) action(a,c.path("id").asText(),"confirm",Map.of("version",0)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/documents/"+doc).header("Authorization","Bearer "+a)).andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(delete("/api/v1/documents/"+doc).header("Authorization","Bearer "+a)).andExpect(status().isNoContent());
        for(String table:List.of("medical_observation","observation_source","candidate_verification","extraction_candidate")) assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM "+table,Integer.class));
    }
    @Test void corruptProvenanceCannotBeTrusted() throws Exception {
        var c=queue(a,process(a,"january-lab.pdf")).path("items").get(0);String id=c.path("id").asText();
        jdbc.update("UPDATE extraction_candidate SET source_text='tampered synthetic evidence' WHERE id=?",UUID.fromString(id));
        action(a,id,"confirm",Map.of("version",0)).andExpect(status().isConflict());assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM medical_observation",Integer.class));
    }
    @Test void tamperedMachineValueCannotBePromoted() throws Exception {
        var c=queue(a,process(a,"january-lab.pdf")).path("items").get(0);String id=c.path("id").asText();
        jdbc.update("UPDATE extraction_candidate SET original_value='99999' WHERE id=?",UUID.fromString(id));
        action(a,id,"confirm",Map.of("version",0)).andExpect(status().isConflict());assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM medical_observation",Integer.class));
    }
    @Test void pendingQueuePaginationAndRequestLimits() throws Exception {
        String doc=process(a,"january-lab.pdf");mvc.perform(get("/api/v1/review/candidates?size=2&page=1").header("Authorization","Bearer "+a)).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.total").value(3));
        mvc.perform(get("/api/v1/review/candidates?size=101").header("Authorization","Bearer "+a)).andExpect(status().isBadRequest());
        String id=queue(a,doc).path("items").get(0).path("id").asText();
        action(a,id,"reject",Map.of("version",0,"reason","x".repeat(9000))).andExpect(status().isPayloadTooLarge());
        mvc.perform(get("/api/v1/review/candidates")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/review/candidates/"+id+"/confirm").contentType("application/json").content("{\"version\":0}")).andExpect(status().isUnauthorized());
    }
    @Test void syntheticAliasConversionAndUnsupportedUnitUseActualPipeline() throws Exception {
        var rows=queue(a,process(a,"normalization-lab.pdf")).path("items");assertEquals(3,rows.size());
        for(var c:rows) {
            var preview=c.path("decision").path("preview");String name=c.path("extracted").path("originalTestName").asText();
            if(name.equals("Sodium")) {assertEquals("UNSUPPORTED_UNIT",preview.path("normalized").path("status").asText());action(a,c.path("id").asText(),"confirm",Map.of("version",0)).andExpect(status().isUnprocessableEntity());}
            else {assertEquals(name.equals("Hb")?"10.4":"5.0",preview.path("normalized").path("value").asText());JsonNode accepted;
                if(name.equals("Hb")) {action(a,c.path("id").asText(),"confirm",Map.of("version",0)).andExpect(status().isUnprocessableEntity());accepted=body(action(a,c.path("id").asText(),"correct",correction(c,"104","g/L",null)).andExpect(status().isOk()));}
                else accepted=body(action(a,c.path("id").asText(),"confirm",Map.of("version",0)).andExpect(status().isOk()));
                assertEquals(c.path("extracted"),accepted.path("observation").path("original"));}
        }
    }
    @Test void evaluateThreeReportsThroughHumanReviewWithExactProvenance() throws Exception {
        int candidates=0,confirmed=0,corrected=0,rejected=0,provenanceCorrect=0;
        for(String file:List.of("january-lab.pdf","april-lab.pdf","september-lab.pdf")) {
            String doc=process(a,file);
            for(var c:queue(a,doc).path("items")) {
                candidates++;String id=c.path("id").asText();var raw=c.path("extracted");JsonNode outcome;
                if(raw.path("originalTestName").asText().equals("TSH")) {outcome=body(action(a,id,"correct",correction(c,"1.8","mIU/L",null)).andExpect(status().isOk()));corrected++;}
                else if(raw.path("originalTestName").asText().equals("Ferritin")) {outcome=body(action(a,id,"reject",Map.of("version",0)).andExpect(status().isOk()));rejected++;}
                else {outcome=body(action(a,id,"confirm",Map.of("version",0)).andExpect(status().isOk()));confirmed++;}
                var evidence=body(mvc.perform(get("/api/v1/documents/"+doc+"/extraction/evidence/"+id).header("Authorization","Bearer "+a)).andExpect(status().isOk()));
                assertEquals(raw.path("source"),evidence);assertTrue(evidence.path("text").asText().contains(raw.path("originalTestName").asText()));
                assertTrue(evidence.path("text").asText().contains(raw.path("originalValue").asText()));
                if(!outcome.path("observation").isNull()) {assertEquals(raw,outcome.path("observation").path("original"));assertEquals(evidence,outcome.path("observation").path("source"));}
                provenanceCorrect++;
            }
        }
        assertEquals(11,candidates);assertEquals(confirmed+corrected,jdbc.queryForObject("SELECT count(*) FROM trusted_medical_observation",Integer.class));
        var report=Map.of("label","SYNTHETIC DEMO DATA — NOT A REAL PATIENT","executedAt",java.time.Instant.now().toString(),"documents",3,"candidates",candidates,"confirmed",confirmed,"corrected",corrected,"rejected",rejected,"sourceAndOriginalPreserved",provenanceCorrect,"scope","Three authored reports; API workflow/provenance assertions, not clinical accuracy");
        Files.createDirectories(Path.of("../evaluation/results"));Files.writeString(Path.of("../evaluation/results/phase5-review.json"),json.writerWithDefaultPrettyPrinter().writeValueAsString(report)+"\n");
    }

}
