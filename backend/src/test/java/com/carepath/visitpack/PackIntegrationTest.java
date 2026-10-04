package com.carepath.visitpack;
import com.carepath.identity.AuthTestSupport;
import com.carepath.intelligence.ProcessingWorker;
import com.fasterxml.jackson.databind.*;
import java.util.*;import java.time.*;import java.nio.file.*;import java.util.concurrent.*;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.*;import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.apache.pdfbox.Loader;import org.apache.pdfbox.text.PDFTextStripper;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class PackIntegrationTest extends AuthTestSupport {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate db;
 @Autowired TestClock clock;@Autowired TestLimits limits;@Autowired ProcessingWorker worker;
 String a,b;final String base="/api/v1/visit-packs";
 @BeforeEach void setup()throws Exception {clock.reset();limits.clear();db.update("DELETE FROM audit_event");db.update("DELETE FROM app_user");a=account("pack-a");b=account("pack-b");}
 String account(String name)throws Exception{var reg=Map.of("email",name+"@example.invalid","password","Synthetic password 123!","displayName","Synthetic");mvc.perform(post("/api/v1/auth/register").header("Origin","http://localhost:3000").header("X-CarePath-Client","web").contentType("application/json").content(json.writeValueAsString(reg))).andExpect(status().isCreated());return body(mvc.perform(post("/api/v1/auth/login").header("Origin","http://localhost:3000").header("X-CarePath-Client","web").contentType("application/json").content(json.writeValueAsString(Map.of("email",reg.get("email"),"password",reg.get("password"))))).andExpect(status().isOk())).path("accessToken").asText();}
 JsonNode body(ResultActions r)throws Exception{return json.readTree(r.andReturn().getResponse().getContentAsString());}
 ResultActions req(String token,String method,String path,Object data)throws Exception{MockHttpServletRequestBuilder r=switch(method){case "POST"->post(path);case "PUT"->put(path);case "DELETE"->delete(path);default->get(path);};r.header("Authorization","Bearer "+token);if(data!=null)r.contentType("application/json").content(json.writeValueAsString(data));return mvc.perform(r);}
 JsonNode readCare(String token,String path)throws Exception{return body(req(token,"GET","/api/v1/care/"+path,null).andExpect(status().isOk()));}
 Map<String,Object> symptom(){var m=new LinkedHashMap<String,Object>();m.put("name","Synthetic headache");m.put("startedAt",clock.instant().minusSeconds(3600).toString());m.put("resolvedAt",null);m.put("severity",3);m.put("frequency","OCCASIONAL");m.put("notes","SYNTHETIC DEMO DATA — NOT A REAL PATIENT");m.put("version",0);return m;}
 Map<String,Object> appointment(long seconds){var m=new LinkedHashMap<String,Object>();m.put("providerName","Synthetic Clinic");m.put("startsAt",clock.instant().plusSeconds(seconds).atOffset(ZoneOffset.UTC).toString());m.put("timeZone","UTC");m.put("notes","private synthetic notes");m.put("documentIds",List.of());m.put("symptomIds",List.of());m.put("questionIds",List.of());m.put("offsets",List.of(0,60));m.put("version",0);return m;}
 JsonNode create(String token,String kind,Object input)throws Exception{return body(req(token,"POST","/api/v1/care/"+kind,input).andExpect(status().isCreated()));}
 String upload(String token)throws Exception{return body(mvc.perform(multipart("/api/v1/documents").file(new MockMultipartFile("file","synthetic.pdf","application/pdf",Files.readAllBytes(Path.of("../sample-data/phase4/january-lab.pdf")))).file(new MockMultipartFile("metadata","","application/json","{\"documentType\":\"LAB_REPORT\",\"documentDate\":\"2026-09-01\",\"tags\":[]}".getBytes())).header("Authorization","Bearer "+token)).andExpect(status().isCreated())).path("id").asText();}
 String candidate(String token,String instruction)throws Exception{String doc;try(var pdf=new org.apache.pdfbox.pdmodel.PDDocument();var out=new java.io.ByteArrayOutputStream()){var page=new org.apache.pdfbox.pdmodel.PDPage();pdf.addPage(page);try(var content=new org.apache.pdfbox.pdmodel.PDPageContentStream(pdf,page)){content.beginText();content.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA),12);content.newLineAtOffset(50,700);content.showText("SYNTHETIC DEMO DATA - NOT A REAL PATIENT");content.newLineAtOffset(0,-30);content.showText(instruction);content.endText();}pdf.save(out);doc=body(mvc.perform(multipart("/api/v1/documents").file(new MockMultipartFile("file","synthetic-followup.pdf","application/pdf",out.toByteArray())).file(new MockMultipartFile("metadata","","application/json","{\"documentType\":\"DOCTOR_NOTE\",\"documentDate\":\"2026-09-01\",\"tags\":[]}".getBytes())).header("Authorization","Bearer "+token)).andExpect(status().isCreated())).path("id").asText();}req(token,"POST","/api/v1/documents/"+doc+"/process",null).andExpect(status().isAccepted());assertTrue(worker.runOne());req(token,"POST","/api/v1/care/documents/"+doc+"/detect-follow-ups",null).andExpect(status().isNoContent());return readCare(token,"follow-ups").path("items").get(0).path("id").asText();}
 Map<String,Object> decision(String action){var m=new LinkedHashMap<String,Object>();m.put("action",action);m.put("version",0);m.put("confirmedAt",clock.instant().minusSeconds(1).atOffset(ZoneOffset.UTC).toString());m.put("timeZone","UTC");m.put("offsets",List.of(0));return m;}
 Map<String,Object> selection(String type,String source){return new LinkedHashMap<>(Map.of("type",type,"sourceId",source));}
 Map<String,Object> input(List<?> items){return new LinkedHashMap<>(Map.of("title","SYNTHETIC DEMO DATA - NOT A REAL PATIENT","reasonForVisit","Prepare to discuss selected records.","items",items,"version",0));}
 JsonNode pack(String token,Map<String,Object> input)throws Exception{return body(req(token,"POST",base,input).andExpect(status().isCreated()));}
 JsonNode readPack(String token,String id)throws Exception{return body(req(token,"GET",base+"/"+id,null).andExpect(status().isOk()));}
 JsonNode preview(String token,String id)throws Exception{return body(req(token,"GET",base+"/"+id+"/preview",null).andExpect(status().isOk()));}
 JsonNode generate(String token,JsonNode p)throws Exception {String id=p.path("id").asText();return body(req(token,"POST",base+"/"+id+"/generate",Map.of("version",p.path("version").asLong(),"previewHash",preview(token,id).path("hash").asText())).andExpect(status().isOk()));}
 String sid(String token)throws Exception{return create(token,"symptoms",symptom()).path("id").asText();}
 String qid(String token,String text)throws Exception{return body(req(token,"POST","/api/v1/assistant/questions",Map.of("text",text,"observationIds",List.of())).andExpect(status().isCreated())).path("id").asText();}
 byte[] pdf(String token,String id)throws Exception{return req(token,"GET",base+"/"+id+"/pdf",null).andExpect(status().isOk()).andExpect(header().string("Content-Type","application/pdf")).andExpect(header().string("Cache-Control","no-store")).andExpect(header().string("Content-Disposition","attachment; filename=carepath-visit-pack.pdf")).andReturn().getResponse().getContentAsByteArray();}
 @Test void draftPreviewGenerateDownloadDelete()throws Exception {
  String sym=sid(a);var p=pack(a,input(List.of(selection("SYMPTOM",sym))));String id=p.path("id").asText();
  assertEquals("DRAFT",p.path("status").asText());var before=preview(a,id).path("content");
  var generated=generate(a,p);assertEquals(before,generated.path("snapshot"));assertEquals("GENERATED",generated.path("status").asText());
  try(var d=Loader.loadPDF(pdf(a,id))){String text=new PDFTextStripper().getText(d);assertTrue(text.contains("USER-REPORTED SYMPTOMS"));assertTrue(text.contains("Synthetic headache"));assertFalse(text.contains(sym));assertFalse(text.contains("private synthetic notes"));assertNull(d.getDocumentCatalog().getOpenAction());}
  req(a,"DELETE",base+"/"+id,null).andExpect(status().isNoContent());req(a,"GET",base+"/"+id+"/pdf",null).andExpect(status().isNotFound());assertEquals(0,db.queryForObject("SELECT count(*) FROM visit_pack_snapshot_item",Integer.class));
 }
 @Test void generatedSnapshotSurvivesSourceEditsAndDeletion()throws Exception {
  String sym=sid(a),q=qid(a,"Original clinician question");var p=generate(a,pack(a,input(List.of(selection("SYMPTOM",sym),selection("SAVED_QUESTION",q)))));String id=p.path("id").asText();JsonNode frozen=p.path("snapshot");
  var s=symptom();s.put("name","Changed symptom");req(a,"PUT","/api/v1/care/symptoms/"+sym,s).andExpect(status().isOk());
  req(a,"PUT","/api/v1/assistant/questions/"+q,Map.of("text","Changed question","version",0)).andExpect(status().isOk());assertEquals(frozen,readPack(a,id).path("snapshot"));
  req(a,"DELETE","/api/v1/care/symptoms/"+sym,null).andExpect(status().isNoContent());req(a,"DELETE","/api/v1/assistant/questions/"+q,null).andExpect(status().isNoContent());assertEquals(frozen,readPack(a,id).path("snapshot"));
  try(var d=Loader.loadPDF(pdf(a,id))){String text=new PDFTextStripper().getText(d);assertTrue(text.contains("Original clinician question"));assertFalse(text.contains("Changed question"));}
 }
 @Test void staleSourcePreviewRequiresNewPreview()throws Exception {
  String sym=sid(a);var p=pack(a,input(List.of(selection("SYMPTOM",sym))));String id=p.path("id").asText();String hash=preview(a,id).path("hash").asText();var s=symptom();s.put("severity",8);req(a,"PUT","/api/v1/care/symptoms/"+sym,s).andExpect(status().isOk());
  req(a,"POST",base+"/"+id+"/generate",Map.of("version",0,"previewHash",hash)).andExpect(status().isConflict());assertEquals("DRAFT",readPack(a,id).path("status").asText());generate(a,p);
 }
 @Test void draftsCanEditAndReorderButGeneratedCannotMutate()throws Exception {
  var p=pack(a,input(List.of()));String id=p.path("id").asText();var x=input(List.of(Map.of("type","MANUAL_QUESTION","questionText","Second"),Map.of("type","MANUAL_QUESTION","questionText","First")));
  p=body(req(a,"PUT",base+"/"+id,x).andExpect(status().isOk()));req(a,"PUT",base+"/"+id,x).andExpect(status().isConflict());generate(a,p);x.put("version",2);
  req(a,"PUT",base+"/"+id,x).andExpect(status().isConflict());req(a,"POST",base+"/"+id+"/generate",Map.of("version",2,"previewHash","0".repeat(64))).andExpect(status().isConflict());
 }
 @Test void explicitRevisionPreservesOldPackAndIsIdempotent()throws Exception {
  var old=generate(a,pack(a,input(List.of())));String id=old.path("id").asText();var next=body(req(a,"POST",base+"/"+id+"/revise",null).andExpect(status().isOk()));assertEquals(2,next.path("revision").asInt());assertEquals("DRAFT",next.path("status").asText());assertEquals(old.path("snapshot"),readPack(a,id).path("snapshot"));
  assertEquals(next.path("id"),body(req(a,"POST",base+"/"+id+"/revise",null).andExpect(status().isOk())).path("id"));generate(a,next);
 }
 @Test void bidirectionalPackIdorAllOperations()throws Exception {
  for(String[] pair:List.of(new String[]{a,b},new String[]{b,a})) {
   var p=pack(pair[0],input(List.of()));String id=p.path("id").asText();
   for(String suffix:List.of("","/preview","/pdf"))req(pair[1],"GET",base+"/"+id+suffix,null).andExpect(status().isNotFound());
   req(pair[1],"PUT",base+"/"+id,input(List.of())).andExpect(status().isNotFound());
   req(pair[1],"POST",base+"/"+id+"/generate",Map.of("version",0,"previewHash","0".repeat(64))).andExpect(status().isNotFound());
   req(pair[1],"POST",base+"/"+id+"/revise",null).andExpect(status().isNotFound());req(pair[1],"DELETE",base+"/"+id,null).andExpect(status().isNotFound());
   generate(pair[0],p);req(pair[1],"GET",base+"/"+id,null).andExpect(status().isNotFound());req(pair[1],"GET",base+"/"+id+"/pdf",null).andExpect(status().isNotFound());
   assertFalse(body(req(pair[1],"GET",base,null).andExpect(status().isOk())).toString().contains(id));
  }
 }
 @Test void crossOwnerSourceRelationshipInjection()throws Exception {
  String sym=sid(b),doc=upload(b),q=qid(b,"Foreign question"),ap=create(b,"appointments",appointment(3600)).path("id").asText();
  for(var entry:Map.of("SYMPTOM",sym,"DOCUMENT",doc,"SAVED_QUESTION",q,"APPOINTMENT",ap).entrySet())req(a,"POST",base,input(List.of(selection(entry.getKey(),entry.getValue())))).andExpect(status().isNotFound());
  var x=input(List.of());x.put("appointmentId",ap);req(a,"POST",base,x).andExpect(status().isNotFound());assertEquals(0,db.queryForObject("SELECT count(*) FROM visit_pack",Integer.class));
 }
 @Test void followUpOnlyConfirmedAndOwnerScoped()throws Exception {
  String id=candidate(b,"Review after 6 weeks");req(b,"POST",base,input(List.of(selection("FOLLOW_UP",id)))).andExpect(status().isUnprocessableEntity());
  req(b,"POST","/api/v1/care/follow-ups/"+id+"/decision",decision("CONFIRM")).andExpect(status().isOk());req(a,"POST",base,input(List.of(selection("FOLLOW_UP",id)))).andExpect(status().isNotFound());
  var p=generate(b,pack(b,input(List.of(selection("FOLLOW_UP",id)))));assertTrue(p.path("snapshot").toString().contains("Review after 6 weeks"));assertTrue(p.path("snapshot").toString().contains("2026-10-13"));
 }
 @Test void manualAndOverriddenQuestionNeverMutateSource()throws Exception {
  String q=qid(a,"Original draft");var item=selection("SAVED_QUESTION",q);item.put("questionText","Pack-specific edit");
  var p=generate(a,pack(a,input(List.of(item,Map.of("type","MANUAL_QUESTION","questionText","Another question")))));
  assertTrue(p.path("snapshot").toString().contains("Pack-specific edit"));assertEquals("Original draft",body(req(a,"GET","/api/v1/assistant/questions/"+q,null).andExpect(status().isOk())).path("text").asText());
 }
 @Test void deletedDraftSourceFailsRatherThanSilentlyOmitting()throws Exception {
  String sym=sid(a);var p=pack(a,input(List.of(selection("SYMPTOM",sym))));req(a,"DELETE","/api/v1/care/symptoms/"+sym,null).andExpect(status().isNoContent());req(a,"GET",base+"/"+p.path("id").asText()+"/preview",null).andExpect(status().isUnprocessableEntity());
 }
 @Test void massAssignmentAndLimits()throws Exception {
  var x=input(List.of());x.put("owner",UUID.randomUUID());req(a,"POST",base,x).andExpect(status().isBadRequest());
  req(a,"POST",base,input(List.of(Map.of("type","DIAGNOSIS","questionText","unsupported")))).andExpect(status().isBadRequest());
  req(a,"POST",base,input(Collections.nCopies(41,Map.of("type","MANUAL_QUESTION","questionText","Question")))).andExpect(status().isBadRequest());
  x=input(List.of());x.put("reasonForVisit","x".repeat(9000));req(a,"POST",base,x).andExpect(status().isPayloadTooLarge());mvc.perform(get(base)).andExpect(status().isUnauthorized());
 }
 @Test void duplicateGenerationRaceHasOneWinner()throws Exception {
  var p=pack(a,input(List.of()));String id=p.path("id").asText(),hash=preview(a,id).path("hash").asText();var pool=Executors.newFixedThreadPool(2);
  try {Callable<Integer> task=()->req(a,"POST",base+"/"+id+"/generate",Map.of("version",0,"previewHash",hash)).andReturn().getResponse().getStatus();var results=pool.invokeAll(List.of(task,task));List<Integer> codes=new ArrayList<>();for(int i=0;i<results.size();i++)codes.add(results.get(i).get());Collections.sort(codes);assertEquals(List.of(200,409),codes);assertEquals(1,db.queryForObject("SELECT count(*) FROM audit_event WHERE action='VISIT_PACK_GENERATED'",Integer.class));}finally{pool.shutdownNow();}
 }
 @Test void listPaginationAndSafeAudits()throws Exception {
  for(int i=0;i<21;i++)pack(a,input(List.of()));assertEquals(20,body(req(a,"GET",base,null).andExpect(status().isOk())).path("items").size());assertEquals(1,body(req(a,"GET",base+"?page=1",null).andExpect(status().isOk())).path("items").size());assertFalse(db.queryForList("SELECT * FROM audit_event").toString().contains("Prepare to discuss"));
 }
    String process(String token,String file) throws Exception {
        String id=json.readTree(mvc.perform(multipart("/api/v1/documents").file(new MockMultipartFile("file",file,"application/pdf",Files.readAllBytes(Path.of(file.equals("normalization-lab.pdf")?"../sample-data/phase5":"../sample-data/phase4",file)))).file(new MockMultipartFile("metadata","","application/json","{\"documentType\":\"LAB_REPORT\",\"tags\":[]}".getBytes())).header("Authorization","Bearer "+token)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asText();
        mvc.perform(post("/api/v1/documents/"+id+"/process").header("Authorization","Bearer "+token)).andExpect(status().isAccepted());assertTrue(worker.runOne());return id;
    }
    JsonNode queue(String token,String doc) throws Exception {return body(mvc.perform(get("/api/v1/review/candidates").param("documentId",doc).header("Authorization","Bearer "+token)).andExpect(status().isOk()));}

    ResultActions action(String token,String id,String action,Object value) throws Exception {return mvc.perform(post("/api/v1/review/candidates/"+id+"/"+action).header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(value)));}
    Map<String,Object> correction(JsonNode c,String value,String unit,String concept) {
        var fields=new LinkedHashMap<String,Object>();var raw=c.path("extracted");
        fields.put("testName",raw.path("originalTestName").asText());fields.put("value",value);fields.put("unit",unit);fields.put("referenceRange",raw.path("referenceText").isNull()?null:raw.path("referenceText").asText());fields.put("date",raw.path("reportDate").isNull()?null:raw.path("reportDate").asText());fields.put("conceptId",concept);
        return Map.of("version",c.path("version").asLong(),"fields",fields,"sourceReviewed",true,"reason","Compared carefully with the synthetic source.");
    }


    List<JsonNode> accept(String token,String doc) throws Exception {
        List<JsonNode> out=new ArrayList<>();for(var c:queue(token,doc).path("items")) {
            String id=c.path("id").asText();JsonNode result;
            if(c.path("decision").path("canConfirm").asBoolean())result=body(action(token,id,"confirm",Map.of("version",0)).andExpect(status().isOk()));
            else result=body(action(token,id,"correct",correction(c,"1.8","mIU/L",null)).andExpect(status().isOk()));
            out.add(result.path("observation"));
        }return out;
    }
 String oid(List<JsonNode> observations,String name){for(int i=0;i<observations.size();i++)if(observations.get(i).path("verified").path("testName").asText().equals(name))return observations.get(i).path("id").asText();throw new AssertionError(name);}
 @Test void exactThreeReportFactsEvidenceAndPdf()throws Exception {
  String jan=process(a,"january-lab.pdf"),apr=process(a,"april-lab.pdf"),sep=process(a,"september-lab.pdf");var j=accept(a,jan);var m=accept(a,apr);var s=accept(a,sep);
  String ap=create(a,"appointments",appointment(3600)).path("id").asText(),sym=sid(a),q=qid(a,"Which record changes should we discuss?");
  var h=selection("CHANGE",oid(m,"Hemoglobin"));h.put("otherId",oid(s,"Hemoglobin"));var v=selection("CHANGE",oid(m,"Vitamin D"));v.put("otherId",oid(s,"Vitamin D"));
  var x=input(List.of(selection("OBSERVATION",oid(j,"Hemoglobin")),selection("OBSERVATION",oid(s,"Hemoglobin")),h,v,selection("DOCUMENT",jan),selection("DOCUMENT",apr),selection("DOCUMENT",sep),selection("SYMPTOM",sym),selection("SAVED_QUESTION",q)));x.put("appointmentId",ap);
  var pack=generate(a,pack(a,x));String id=pack.path("id").asText();JsonNode frozen=pack.path("snapshot");
  JsonNode change=frozen.path("items").get(3);assertTrue(change.toString().contains("DECREASED"));assertEquals(apr,change.path("evidence").get(0).path("documentId").asText());assertEquals(1,change.path("evidence").get(0).path("page").asInt());assertEquals(sep,change.path("evidence").get(1).path("documentId").asText());assertEquals(2,change.path("evidence").get(1).path("page").asInt());
  var canonical=body(req(a,"GET","/api/v1/history/compare?previous="+oid(m,"Hemoglobin")+"&current="+oid(s,"Hemoglobin"),null).andExpect(status().isOk()));assertEquals(canonical.path("explanation").asText(),change.path("fields").get(1).path("value").asText());
  byte[] bytes=pdf(a,id);try(var d=Loader.loadPDF(bytes)){String t=new PDFTextStripper().getText(d);for(String text:List.of("12.1","11.3","10.4","24","31","INCREASED","Synthetic Clinic","Synthetic headache","Page 2","september-lab.pdf"))assertTrue(t.contains(text),text);assertFalse(t.contains("Ferritin"));assertFalse(t.contains("storage_key"));assertFalse(t.contains(id));assertNull(d.getDocumentInformation().getAuthor());}
  Files.createDirectories(Path.of("../sample-data/phase9"));Files.write(Path.of("../sample-data/phase9/synthetic-visit-pack.pdf"),bytes);
  db.update("UPDATE medical_observation SET verified_value_text='15',verified_numeric_value=15,normalized_value=15 WHERE id=?",UUID.fromString(oid(s,"Hemoglobin")));assertEquals(frozen,readPack(a,id).path("snapshot"));
  req(a,"DELETE","/api/v1/documents/"+sep,null).andExpect(status().isNoContent());assertEquals(frozen,readPack(a,id).path("snapshot"));pdf(a,id);
  req(b,"GET","/api/v1/documents/"+apr+"/preview?page=1",null).andExpect(status().isNotFound());
 }
 @Test void pendingRejectedUntrustedAndForeignObservationsExcluded()throws Exception {
  String doc=process(b,"january-lab.pdf");var rows=queue(b,doc).path("items");String candidate=rows.get(0).path("id").asText();
  req(b,"POST",base,input(List.of(selection("OBSERVATION",candidate)))).andExpect(status().isNotFound());action(b,candidate,"reject",Map.of("version",0)).andExpect(status().isOk());req(b,"POST",base,input(List.of(selection("OBSERVATION",candidate)))).andExpect(status().isNotFound());
  var o=body(action(b,rows.get(1).path("id").asText(),"confirm",Map.of("version",0)).andExpect(status().isOk())).path("observation");String id=o.path("id").asText();
  req(a,"POST",base,input(List.of(selection("OBSERVATION",id)))).andExpect(status().isNotFound());
  db.update("UPDATE medical_observation SET verification_status='PENDING' WHERE id=?",UUID.fromString(id));req(b,"POST",base,input(List.of(selection("OBSERVATION",id)))).andExpect(status().isNotFound());
 }
}
