package com.carepath.care;
import com.carepath.identity.AuthTestSupport;import com.carepath.intelligence.ProcessingWorker;import com.fasterxml.jackson.databind.*;import java.util.*;import java.time.*;import java.nio.file.*;import java.util.concurrent.*;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.mock.web.MockMultipartFile;import org.springframework.test.web.servlet.*;import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.junit.jupiter.api.Assertions.*;import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class ClosureIntegrationTest extends AuthTestSupport {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate db;@Autowired TestClock clock;@Autowired TestLimits limits;@Autowired ReminderService reminders;@Autowired ProcessingWorker worker;
 String a,b;
 @BeforeEach void setup()throws Exception{clock.reset();limits.clear();db.update("DELETE FROM audit_event");db.update("DELETE FROM app_user");a=account("closure-a");b=account("closure-b");}
 String account(String name)throws Exception{var reg=Map.of("email",name+"@example.invalid","password","Synthetic password 123!","displayName","Synthetic");mvc.perform(post("/api/v1/auth/register").header("Origin","http://localhost:3000").header("X-CarePath-Client","web").contentType("application/json").content(json.writeValueAsString(reg))).andExpect(status().isCreated());return body(mvc.perform(post("/api/v1/auth/login").header("Origin","http://localhost:3000").header("X-CarePath-Client","web").contentType("application/json").content(json.writeValueAsString(Map.of("email",reg.get("email"),"password",reg.get("password"))))).andExpect(status().isOk())).path("accessToken").asText();}
 JsonNode body(ResultActions r)throws Exception{return json.readTree(r.andReturn().getResponse().getContentAsString());}
 ResultActions req(String token,String method,String path,Object data)throws Exception{MockHttpServletRequestBuilder r=switch(method){case "POST"->post(path);case "PUT"->put(path);case "DELETE"->delete(path);default->get(path);};r.header("Authorization","Bearer "+token);if(data!=null)r.contentType("application/json").content(json.writeValueAsString(data));return mvc.perform(r);}
 JsonNode read(String token,String path)throws Exception{return body(req(token,"GET","/api/v1/care/"+path,null).andExpect(status().isOk()));}
 Map<String,Object> symptom(){var m=new LinkedHashMap<String,Object>();m.put("name","Synthetic headache");m.put("startedAt",clock.instant().minusSeconds(3600).toString());m.put("resolvedAt",null);m.put("severity",3);m.put("frequency","OCCASIONAL");m.put("notes","SYNTHETIC DEMO DATA — NOT A REAL PATIENT");m.put("version",0);return m;}
 Map<String,Object> appointment(long seconds){var m=new LinkedHashMap<String,Object>();m.put("providerName","Synthetic Clinic");m.put("startsAt",clock.instant().plusSeconds(seconds).atOffset(ZoneOffset.UTC).toString());m.put("timeZone","UTC");m.put("notes","private synthetic notes");m.put("documentIds",List.of());m.put("symptomIds",List.of());m.put("questionIds",List.of());m.put("offsets",List.of(0,60));m.put("version",0);return m;}
 JsonNode create(String token,String kind,Object input)throws Exception{return body(req(token,"POST","/api/v1/care/"+kind,input).andExpect(status().isCreated()));}
 String upload(String token)throws Exception{return body(mvc.perform(multipart("/api/v1/documents").file(new MockMultipartFile("file","synthetic.pdf","application/pdf",Files.readAllBytes(Path.of("../sample-data/phase4/january-lab.pdf")))).file(new MockMultipartFile("metadata","","application/json","{\"documentType\":\"LAB_REPORT\",\"documentDate\":\"2026-09-01\",\"tags\":[]}".getBytes())).header("Authorization","Bearer "+token)).andExpect(status().isCreated())).path("id").asText();}
 String candidate(String token,String instruction)throws Exception{String doc;try(var pdf=new org.apache.pdfbox.pdmodel.PDDocument();var out=new java.io.ByteArrayOutputStream()){var page=new org.apache.pdfbox.pdmodel.PDPage();pdf.addPage(page);try(var content=new org.apache.pdfbox.pdmodel.PDPageContentStream(pdf,page)){content.beginText();content.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA),12);content.newLineAtOffset(50,700);content.showText("SYNTHETIC DEMO DATA - NOT A REAL PATIENT");content.newLineAtOffset(0,-30);content.showText(instruction);content.endText();}pdf.save(out);doc=body(mvc.perform(multipart("/api/v1/documents").file(new MockMultipartFile("file","synthetic-followup.pdf","application/pdf",out.toByteArray())).file(new MockMultipartFile("metadata","","application/json","{\"documentType\":\"DOCTOR_NOTE\",\"documentDate\":\"2026-09-01\",\"tags\":[]}".getBytes())).header("Authorization","Bearer "+token)).andExpect(status().isCreated())).path("id").asText();}req(token,"POST","/api/v1/documents/"+doc+"/process",null).andExpect(status().isAccepted());assertTrue(worker.runOne());req(token,"POST","/api/v1/care/documents/"+doc+"/detect-follow-ups",null).andExpect(status().isNoContent());return read(token,"follow-ups").path("items").get(0).path("id").asText();}
 Map<String,Object> decision(String action){var m=new LinkedHashMap<String,Object>();m.put("action",action);m.put("version",0);m.put("confirmedAt",clock.instant().minusSeconds(1).atOffset(ZoneOffset.UTC).toString());m.put("timeZone","UTC");m.put("offsets",List.of(0));return m;}

 Map<String,Object> query(String text){var q=new LinkedHashMap<String,Object>();q.put("q",text);q.put("kind","ALL");q.put("page",0);return q;}
 JsonNode search(String token,Map<String,Object> q)throws Exception{return body(req(token,"POST","/api/v1/search",q).andExpect(status().isOk()));}
 JsonNode activity(String token,String suffix)throws Exception{return body(req(token,"GET","/api/v1/activity"+suffix,null).andExpect(status().isOk()));}
 @Test void activityProjectionAndOwnerIsolation()throws Exception{
  create(a,"symptoms",symptom());create(b,"appointments",appointment(3600));
  var aa=activity(a,"?category=CARE");assertEquals(1,aa.path("total").asInt());assertEquals("SYMPTOM_CREATED",aa.path("items").get(0).path("action").asText());
  var bb=activity(b,"?category=CARE");assertFalse(bb.toString().contains("SYMPTOM_CREATED"));
  var fields=new HashSet<String>();aa.path("items").get(0).fieldNames().forEachRemaining(fields::add);assertEquals(Set.of("action","outcome","actor","occurredAt"),fields);
  assertFalse(aa.toString().contains("headache"));assertFalse(aa.toString().contains(a));assertFalse(aa.toString().contains("resourceId"));
  assertTrue(activity(a,"?category=SECURITY").toString().contains("LOGIN_SUCCEEDED"));
 }
 @Test void activityPaginationValidationAndNoOwnerOverride()throws Exception{
  for(int i=0;i<21;i++)create(a,"symptoms",symptom());
  assertEquals(20,activity(a,"?category=CARE").path("items").size());assertEquals(1,activity(a,"?category=CARE&page=1").path("items").size());
  req(a,"GET","/api/v1/activity?page=-1",null).andExpect(status().isBadRequest());req(a,"GET","/api/v1/activity?category=INVALID",null).andExpect(status().isBadRequest());
  assertEquals(0,activity(b,"?category=CARE&userId=forged").path("total").asInt());
 }
 @Test void searchCoversDocumentsMonthCategoryAndProvider()throws Exception{
  String id=upload(a);db.update("UPDATE medical_document SET document_type='PRESCRIPTION',provider_name='Synthetic Lab' WHERE id=?",UUID.fromString(id));
  db.update("INSERT INTO vault_document_tag(document_id,tag) VALUES(?,?)",UUID.fromString(id),"CBC");assertEquals(1,search(a,query("CBC")).path("total").asInt());
  assertEquals(id,search(a,query("September prescription")).path("items").get(0).path("id").asText());
  assertEquals(0,search(a,query("April prescription")).path("total").asInt());
  var q=query("");q.put("provider","Synthetic Lab");q.put("from","2026-09-01");q.put("to","2026-09-30");assertEquals(1,search(a,q).path("total").asInt());
  q.put("from","2026-10-01");req(a,"POST","/api/v1/search",q).andExpect(status().isBadRequest());
  assertEquals(0,search(b,query("September prescription")).path("total").asInt());
 }
 @Test void searchSymptomsAppointmentsAndLiteralWildcards()throws Exception{
  var sx=symptom();sx.put("name","Synthetic 100%_symptom");create(a,"symptoms",sx);create(a,"appointments",appointment(3600));create(b,"appointments",appointment(3600));
  assertEquals(1,search(a,query("100%_symptom")).path("total").asInt());assertEquals(0,search(a,query("%evil")).path("total").asInt());
  var q=query("Synthetic");q.put("kind","APPOINTMENT");assertEquals(1,search(a,q).path("total").asInt());
  assertEquals(0,search(a,query("private synthetic notes")).path("total").asInt());
  assertEquals(0,search(a,query("' OR 1=1 --")).path("total").asInt());
 }
 @Test void searchExcludesCandidatesAndPreservesVerifiedSource()throws Exception{
  String doc=upload(a);req(a,"POST","/api/v1/documents/"+doc+"/process",null).andExpect(status().isAccepted());assertTrue(worker.runOne());
  assertEquals(0,search(a,query("hemoglobin")).path("total").asInt());
  var candidates=body(req(a,"GET","/api/v1/review/candidates?documentId="+doc,null).andExpect(status().isOk())).path("items");
  String candidate="";for(var c:candidates){if(c.toString().contains("Hemoglobin")){candidate=c.path("id").asText();break;}}
  assertFalse(candidate.isBlank());req(a,"POST","/api/v1/review/candidates/"+candidate+"/confirm",Map.of("version",0)).andExpect(status().isOk());
  var result=search(a,query("hemoglobin"));assertEquals(1,result.path("total").asInt());var r=result.path("items").get(0);assertEquals(doc,r.path("documentId").asText());assertEquals(candidate,r.path("candidateId").asText());assertTrue(r.path("summary").asText().contains("12.1"));
  assertEquals(0,search(b,query("hemoglobin")).path("total").asInt());
  req(a,"DELETE","/api/v1/documents/"+doc,null).andExpect(status().isNoContent());assertEquals(0,search(a,query("hemoglobin")).path("total").asInt());
 }
 @Test void searchBoundsPaginationAndMassAssignment()throws Exception{
  for(int i=0;i<21;i++)create(a,"symptoms",symptom());var q=query("headache");assertEquals(20,search(a,q).path("items").size());q.put("page",1);assertEquals(1,search(a,q).path("items").size());
  q.put("owner",UUID.randomUUID());req(a,"POST","/api/v1/search",q).andExpect(status().isBadRequest());
  q=query("x".repeat(81));req(a,"POST","/api/v1/search",q).andExpect(status().isBadRequest());
  q=query("x".repeat(9000));req(a,"POST","/api/v1/search",q).andExpect(status().isPayloadTooLarge());
 }
 @Test void searchRateLimitAndSafeOutage()throws Exception{
  for(int i=0;i<60;i++)search(a,query(""));req(a,"POST","/api/v1/search",query("")).andExpect(status().isTooManyRequests());
  limits.clear();limits.unavailable=true;try{var response=req(a,"POST","/api/v1/search",query("private query")).andExpect(status().isServiceUnavailable()).andReturn().getResponse();assertFalse(response.getContentAsString().contains("private query"));}finally{limits.clear();}
 }
 @Test void newRoutesRequireActiveAuthentication()throws Exception{
  mvc.perform(get("/api/v1/activity")).andExpect(status().isUnauthorized());mvc.perform(post("/api/v1/search").contentType("application/json").content("{}" )).andExpect(status().isUnauthorized());
  req(a,"POST","/api/v1/activity",Map.of()).andExpect(status().isForbidden());
 }
 @Test void timelineAppointmentsOrderedAndOwnerScoped()throws Exception{
  String late=create(a,"appointments",appointment(7200)).path("id").asText(), early=create(a,"appointments",appointment(3600)).path("id").asText(), other=create(b,"appointments",appointment(1800)).path("id").asText();
  var events=body(req(a,"GET","/api/v1/history/events",null).andExpect(status().isOk()));assertEquals(2,events.path("total").asInt());assertEquals(early,events.path("items").get(0).path("id").asText());assertEquals(late,events.path("items").get(1).path("id").asText());assertFalse(events.toString().contains(other));assertEquals("APPOINTMENT",events.path("items").get(0).path("type").asText());
  assertTrue(events.path("items").get(0).path("observation").isNull());assertEquals(0,body(req(a,"GET","/api/v1/history/concepts",null).andExpect(status().isOk())).path("total").asInt());
 }
 @Test void timelineOnlyConfirmedFollowupsWithExactProvenance()throws Exception{
  String id=candidate(a,"Review after 6 weeks");var f=read(a,"follow-ups/"+id);String doc=f.path("documentId").asText();
  assertFalse(body(req(a,"GET","/api/v1/history/events",null).andExpect(status().isOk())).toString().contains("FOLLOW_UP"));
  req(a,"POST","/api/v1/care/follow-ups/"+id+"/decision",decision("CONFIRM")).andExpect(status().isOk());
  var events=body(req(a,"GET","/api/v1/history/events?documentId="+doc,null).andExpect(status().isOk())).path("items");JsonNode follow=null;for(var e:events)if(e.path("type").asText().equals("FOLLOW_UP"))follow=e;
  assertNotNull(follow);assertEquals(id,follow.path("id").asText());assertEquals(doc,follow.path("documentId").asText());assertEquals(1,follow.path("sourcePage").asInt());assertEquals(read(a,"follow-ups/"+id).path("confirmedDate"),follow.path("date"));
  assertFalse(body(req(b,"GET","/api/v1/history/events",null).andExpect(status().isOk())).toString().contains(id));
  req(b,"GET","/api/v1/history/events?documentId="+doc,null).andExpect(status().isNotFound());
 }
}
