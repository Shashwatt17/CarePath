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
class PatientSummaryIntegrationTest extends AuthTestSupport {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate db;
 @Autowired TestClock clock;@Autowired TestLimits limits;
 String a,b;final String base="/api/v1/visit-packs";
 @BeforeEach void setup()throws Exception {clock.reset();limits.clear();db.update("DELETE FROM audit_event");db.update("DELETE FROM app_user");a=account("pack-a");b=account("pack-b");}
 String account(String name)throws Exception{var reg=Map.of("email",name+"@example.invalid","password","Synthetic password 123!","displayName","Synthetic");mvc.perform(post("/api/v1/auth/register").header("Origin","http://localhost:3000").header("X-CarePath-Client","web").contentType("application/json").content(json.writeValueAsString(reg))).andExpect(status().isCreated());return body(mvc.perform(post("/api/v1/auth/login").header("Origin","http://localhost:3000").header("X-CarePath-Client","web").contentType("application/json").content(json.writeValueAsString(Map.of("email",reg.get("email"),"password",reg.get("password"))))).andExpect(status().isOk())).path("accessToken").asText();}
 JsonNode body(ResultActions r)throws Exception{return json.readTree(r.andReturn().getResponse().getContentAsString());}
 ResultActions req(String token,String method,String path,Object data)throws Exception{MockHttpServletRequestBuilder r=switch(method){case "POST"->post(path);case "PUT"->put(path);case "DELETE"->delete(path);default->get(path);};r.header("Authorization","Bearer "+token);if(data!=null)r.contentType("application/json").content(json.writeValueAsString(data));return mvc.perform(r);}
 Map<String,Object> symptom(){var m=new LinkedHashMap<String,Object>();m.put("name","Synthetic headache");m.put("startedAt",clock.instant().minusSeconds(3600).toString());m.put("resolvedAt",null);m.put("severity",3);m.put("frequency","OCCASIONAL");m.put("notes","SYNTHETIC DEMO DATA — NOT A REAL PATIENT");m.put("version",0);return m;}
 JsonNode create(String token,String kind,Object input)throws Exception{return body(req(token,"POST","/api/v1/care/"+kind,input).andExpect(status().isCreated()));}
 String upload(String token)throws Exception{return body(mvc.perform(multipart("/api/v1/documents").file(new MockMultipartFile("file","synthetic.pdf","application/pdf",Files.readAllBytes(Path.of("../sample-data/phase4/january-lab.pdf")))).file(new MockMultipartFile("metadata","","application/json","{\"documentType\":\"LAB_REPORT\",\"documentDate\":\"2026-09-01\",\"tags\":[]}".getBytes())).header("Authorization","Bearer "+token)).andExpect(status().isCreated())).path("id").asText();}
 Map<String,Object> selection(String type,String source){return new LinkedHashMap<>(Map.of("type",type,"sourceId",source));}
 Map<String,Object> input(List<?> items){return new LinkedHashMap<>(Map.of("title","SYNTHETIC DEMO DATA - NOT A REAL PATIENT","reasonForVisit","Prepare to discuss selected records.","items",items,"version",0));}
 JsonNode pack(String token,Map<String,Object> input)throws Exception{return body(req(token,"POST",base,input).andExpect(status().isCreated()));}
 JsonNode preview(String token,String id)throws Exception{return body(req(token,"GET",base+"/"+id+"/preview",null).andExpect(status().isOk()));}
 JsonNode generate(String token,JsonNode p)throws Exception {String id=p.path("id").asText();return body(req(token,"POST",base+"/"+id+"/generate",Map.of("version",p.path("version").asLong(),"previewHash",preview(token,id).path("hash").asText())).andExpect(status().isOk()));}
 String sid(String token)throws Exception{return create(token,"symptoms",symptom()).path("id").asText();}
 JsonNode share(String token,String pack,int minutes)throws Exception{return body(req(token,"POST","/api/v1/shares",Map.of("packId",pack,"expiryMinutes",minutes)).andExpect(status().isCreated()));}
 JsonNode prepared(String token)throws Exception{return generate(token,pack(token,input(List.of(selection("SYMPTOM",sid(token))))));}
 ResultActions access(String token)throws Exception{return mvc.perform(post("/api/v1/public/share/access").contentType("application/json").content(json.writeValueAsString(Map.of("token",token))));}

 Map<String,String> profile(String intro){return Map.of("introduction",intro,"medicalHistory","Synthetic history","medications","Patient reported medication","allergies","Not known by patient","reasonForVisit","Discuss reports");}
 JsonNode saveProfile(String token,String intro)throws Exception{return body(req(token,"PUT","/api/v1/patient-profile",profile(intro)).andExpect(status().isOk()));}
 ResultActions report(String token,int item,int evidence)throws Exception{return mvc.perform(post("/api/v1/public/share/report").contentType("application/json").content(json.writeValueAsString(Map.of("token",token,"itemIndex",item,"evidenceIndex",evidence))));}
 JsonNode reportShare(String token,String id)throws Exception{return body(req(token,"POST","/api/v1/shares",Map.of("packId",id,"expiryMinutes",15,"includeReports",true)).andExpect(status().isCreated()));}
 @Test void profileIsPrivateAndPersistsUpdatesWithoutDuplicatingRows()throws Exception {
  mvc.perform(get("/api/v1/patient-profile")).andExpect(status().isUnauthorized());
  req(a,"GET","/api/v1/patient-profile",null).andExpect(status().isOk()).andExpect(jsonPath("$.introduction").value(""));
  saveProfile(a,"Synthetic patient A");saveProfile(b,"Synthetic patient B");saveProfile(a,"Updated A");
  req(a,"GET","/api/v1/patient-profile",null).andExpect(jsonPath("$.introduction").value("Updated A"));
  req(b,"GET","/api/v1/patient-profile",null).andExpect(jsonPath("$.introduction").value("Synthetic patient B"));
  assertEquals(2,db.queryForObject("SELECT count(*) FROM patient_profile",Integer.class));
 }
 @Test void profileRejectsOversizeMissingAndInjectedOwnerFields()throws Exception {
  req(a,"PUT","/api/v1/patient-profile",profile("x".repeat(2001))).andExpect(status().isBadRequest());
  req(a,"PUT","/api/v1/patient-profile",Map.of("introduction","Incomplete")).andExpect(status().isBadRequest());
  var forged=new HashMap<String,Object>(profile("Synthetic"));forged.put("ownerId",UUID.randomUUID());
  req(a,"PUT","/api/v1/patient-profile",forged).andExpect(status().isBadRequest());
  assertEquals(0,db.queryForObject("SELECT count(*) FROM patient_profile",Integer.class));
 }
 @Test void profileIsExplicitFrozenAndPreviewChangesRequireReview()throws Exception {
  saveProfile(a,"Original introduction");saveProfile(b,"Other owner secret");
  var p=pack(a,input(List.of(Map.of("type","PROFILE"))));String id=p.path("id").asText();
  var old=preview(a,id);saveProfile(a,"Reviewed introduction");
  req(a,"POST",base+"/"+id+"/generate",Map.of("version",p.path("version").asLong(),"previewHash",old.path("hash").asText())).andExpect(status().isConflict());
  var frozen=generate(a,p);assertTrue(frozen.toString().contains("Reviewed introduction"));assertFalse(frozen.toString().contains("Other owner secret"));
  saveProfile(a,"New private introduction");String token=share(a,id,30).path("token").asText();
  var shared=body(access(token).andExpect(status().isOk()));assertTrue(shared.toString().contains("Reviewed introduction"));assertFalse(shared.toString().contains("New private introduction"));
  byte[] pdf=req(a,"GET",base+"/"+id+"/pdf",null).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
  try(var document=Loader.loadPDF(pdf)){assertTrue(new PDFTextStripper().getText(document).contains("Reviewed introduction"));}
  var without=generate(a,pack(a,input(List.of())));assertFalse(without.toString().contains("New private introduction"));
  req(a,"POST",base,input(List.of(selection("PROFILE",UUID.randomUUID().toString())))).andExpect(status().isUnprocessableEntity());
 }
 @Test void reportsRequireOptInAndOnlySelectedEvidenceCanDownload()throws Exception {
  String doc=upload(a);upload(b);var p=generate(a,pack(a,input(List.of(selection("DOCUMENT",doc)))));String id=p.path("id").asText();
  String denied=share(a,id,15).path("token").asText();report(denied,0,0).andExpect(status().isNotFound());
  String token=reportShare(a,id).path("token").asText();
  report(token,0,0).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(content().bytes(Files.readAllBytes(Path.of("../sample-data/phase4/january-lab.pdf"))));
  report(token,1,0).andExpect(status().isNotFound());report(token,0,1).andExpect(status().isNotFound());report(token,-1,0).andExpect(status().isBadRequest());
  req(a,"DELETE","/api/v1/documents/"+doc,null).andExpect(status().isNoContent());report(token,0,0).andExpect(status().isNotFound());
 }
 @Test void reportAccessClosesOnRevokeAndExpiry()throws Exception {
  String doc=upload(a);String id=generate(a,pack(a,input(List.of(selection("DOCUMENT",doc))))).path("id").asText();
  var revoked=reportShare(a,id);String token=revoked.path("token").asText();
  req(a,"POST","/api/v1/shares/"+revoked.path("share").path("id").asText()+"/revoke",null).andExpect(status().isOk());report(token,0,0).andExpect(status().isNotFound());
  String expiring=reportShare(a,id).path("token").asText();clock.advance(900);report(expiring,0,0).andExpect(status().isNotFound());
 }
}
