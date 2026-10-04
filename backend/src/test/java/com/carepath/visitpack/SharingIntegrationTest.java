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
class SharingIntegrationTest extends AuthTestSupport {
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
 @Test void secureTokenCreationDigestOnlyAndMetadataLists()throws Exception {
  String id=prepared(a).path("id").asText();Set<String> seen=new HashSet<>();
  int[] durations={15,30,60,1440};
  for(int i=0;i<durations.length;i++){
   var c=share(a,id,durations[i]);String token=c.path("token").asText();assertEquals(32,Base64.getUrlDecoder().decode(token).length);assertTrue(seen.add(token));assertEquals(43,token.length());
   String digest=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
   assertEquals(digest,db.queryForObject("SELECT token_hash FROM share_token WHERE id=?",String.class,UUID.fromString(c.path("share").path("id").asText())));
   assertEquals(clock.instant().plusSeconds(durations[i]*60L),Instant.parse(c.path("share").path("expiresAt").asText()));
   assertFalse(body(req(a,"GET","/api/v1/shares",null).andExpect(status().isOk())).toString().contains(token));
  }
  assertEquals(4,db.queryForObject("SELECT count(DISTINCT token_hash) FROM share_token",Integer.class));
 }
 @Test void revokeImmediatelyDeniesSameCapabilityAndIsIdempotent()throws Exception {
  var c=share(a,prepared(a).path("id").asText(),30);String token=c.path("token").asText(),id=c.path("share").path("id").asText();
  access(token).andExpect(status().isOk());
  req(a,"POST","/api/v1/shares/"+id+"/revoke",null).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REVOKED"));
  access(token).andExpect(status().isNotFound());req(a,"POST","/api/v1/shares/"+id+"/revoke",null).andExpect(status().isOk());
  assertEquals(1,db.queryForObject("SELECT count(*) FROM audit_event WHERE action='SHARE_REVOKED'",Integer.class));
 }
 @Test void expiryAtExactBoundaryAndBeyondUsesClock()throws Exception {
  var c=share(a,prepared(a).path("id").asText(),15);String token=c.path("token").asText();
  clock.advance(899);access(token).andExpect(status().isOk());clock.advance(1);access(token).andExpect(status().isNotFound());clock.advance(1);access(token).andExpect(status().isNotFound());
  a=body(mvc.perform(post("/api/v1/auth/login").header("Origin","http://localhost:3000").header("X-CarePath-Client","web").contentType("application/json").content(json.writeValueAsString(Map.of("email","pack-a@example.invalid","password","Synthetic password 123!")))).andExpect(status().isOk())).path("accessToken").asText();
  req(a,"GET","/api/v1/shares",null).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].status").value("EXPIRED"));
  assertEquals(1L,db.queryForObject("SELECT access_count FROM share_token",Long.class));
 }
 @Test void snapshotSurvivesUnderlyingEditAndDeletion()throws Exception {
  String sym=sid(a);var p=generate(a,pack(a,input(List.of(selection("SYMPTOM",sym)))));String token=share(a,p.path("id").asText(),30).path("token").asText();
  JsonNode before=body(access(token).andExpect(status().isOk()));var s=symptom();s.put("name","Changed private symptom");req(a,"PUT","/api/v1/care/symptoms/"+sym,s).andExpect(status().isOk());
  req(a,"DELETE","/api/v1/care/symptoms/"+sym,null).andExpect(status().isNoContent());assertEquals(before,body(access(token).andExpect(status().isOk())));
  assertTrue(before.toString().contains("Synthetic headache"));assertFalse(before.toString().contains(sym));
 }
 @Test void foreignPackManagementRejectedBothDirections()throws Exception {
  String[] users={a,b};for(int i=0;i<2;i++){String owner=users[i],other=users[1-i],id=prepared(owner).path("id").asText();var s=share(owner,id,30);String sid=s.path("share").path("id").asText();
   req(other,"POST","/api/v1/shares",Map.of("packId",id,"expiryMinutes",30)).andExpect(status().isNotFound());
   req(other,"POST","/api/v1/shares/"+sid+"/revoke",null).andExpect(status().isNotFound());
   String list=body(req(other,"GET","/api/v1/shares",null).andExpect(status().isOk())).toString();assertFalse(list.contains(sid));
   access(s.path("token").asText()).andExpect(status().isOk());
  }
 }
 @Test void publicCapabilityNeverAuthorizesOtherResourcesOrIdManipulation()throws Exception {
  String packA=prepared(a).path("id").asText(),packB=prepared(b).path("id").asText(),doc=upload(a),sym=sid(a);String token=share(a,packA,30).path("token").asText();
  String[] paths={base+"/"+packB,base+"/"+packA+"/pdf","/api/v1/documents/"+doc+"/download","/api/v1/documents/"+doc+"/preview","/api/v1/care/symptoms/"+sym,"/api/v1/history/observations/"+UUID.randomUUID()+"/evidence","/api/v1/care/appointments","/api/v1/auth/me","/api/v1/shares"};
  for(int i=0;i<paths.length;i++){mvc.perform(get(paths[i])).andExpect(status().isUnauthorized());req(token,"GET",paths[i],null).andExpect(status().isUnauthorized());}
  mvc.perform(post("/api/v1/public/share/access").contentType("application/json").content(json.writeValueAsString(Map.of("token",token,"packId",packB)))).andExpect(status().isBadRequest());
  mvc.perform(get("/api/v1/public/share/"+packB+"/evidence")).andExpect(status().isUnauthorized());
 }
 @Test void evidenceIsSnapshotTextWithNoNavigableIdentifiers()throws Exception {
  String doc=upload(a);var p=generate(a,pack(a,input(List.of(selection("DOCUMENT",doc)))));String pack=p.path("id").asText(),token=share(a,pack,30).path("token").asText();
  var result=body(access(token).andExpect(status().isOk()));assertEquals("synthetic.pdf",result.path("items").get(0).path("evidence").get(0).path("filename").asText());
  assertEquals(1,result.path("items").get(0).path("evidence").get(0).path("page").asInt());assertFalse(result.toString().contains(doc));assertFalse(result.toString().contains(pack));assertFalse(result.toString().contains("documentId"));assertFalse(result.toString().contains("observationId"));
  req(a,"DELETE","/api/v1/documents/"+doc,null).andExpect(status().isNoContent());assertEquals(result,body(access(token).andExpect(status().isOk())));
  mvc.perform(get("/api/v1/documents/"+doc+"/preview")).andExpect(status().isUnauthorized());
 }
 @Test void deletingPackCascadesSharesAndClosesAccess()throws Exception {
  String p=prepared(a).path("id").asText();String token=share(a,p,30).path("token").asText();req(a,"DELETE",base+"/"+p,null).andExpect(status().isNoContent());access(token).andExpect(status().isNotFound());assertEquals(0,db.queryForObject("SELECT count(*) FROM share_token",Integer.class));
 }
 @Test void draftAndInvalidExpiryAndMassAssignmentRejected()throws Exception {
  String draft=pack(a,input(List.of())).path("id").asText();req(a,"POST","/api/v1/shares",Map.of("packId",draft,"expiryMinutes",30)).andExpect(status().isConflict());
  String p=prepared(a).path("id").asText();int[] invalid={0,-1,16,1441,Integer.MAX_VALUE};for(int i=0;i<invalid.length;i++)req(a,"POST","/api/v1/shares",Map.of("packId",p,"expiryMinutes",invalid[i])).andExpect(status().isBadRequest());
  req(a,"POST","/api/v1/shares",Map.of("packId",p,"expiryMinutes",30,"owner",UUID.randomUUID())).andExpect(status().isBadRequest());assertEquals(0,db.queryForObject("SELECT count(*) FROM share_token",Integer.class));
 }
 @Test void malformedRandomTruncatedModifiedTokensSafe()throws Exception {
  String token=share(a,prepared(a).path("id").asText(),30).path("token").asText();String[] invalid={"",token.substring(1),"z".repeat(43),"../secret",token+"A",(token.charAt(0)=='A'?"B":"A")+token.substring(1),"<script>alert(1)</script>"};
  for(int i=0;i<invalid.length;i++){var r=body(access(invalid[i]).andExpect(status().isNotFound()));assertEquals("SHARE_UNAVAILABLE",r.path("code").asText());assertFalse(r.toString().contains(token));}
  access("a".repeat(9000)).andExpect(status().isPayloadTooLarge());
 }
 @Test void cacheHeadersAndGenericErrors()throws Exception {
  String token=share(a,prepared(a).path("id").asText(),30).path("token").asText();
  for(int i=0;i<2;i++)access(i==0?token:"x".repeat(43)).andExpect(header().string("Cache-Control","no-store")).andExpect(header().string("Referrer-Policy","no-referrer")).andExpect(header().string("X-Robots-Tag","noindex, nofollow, noarchive")).andExpect(header().string("X-Content-Type-Options","nosniff")).andExpect(header().string("Content-Security-Policy","default-src 'none'; frame-ancestors 'none'"));
  mvc.perform(post("/api/v1/public/share/access").contentType("application/json").content("{broken")).andExpect(status().isBadRequest()).andExpect(header().string("Cache-Control","no-store"));
 }
 @Test void rateLimitAndStoreOutageFailClosedWithoutMedicalData()throws Exception {
  String token=share(a,prepared(a).path("id").asText(),30).path("token").asText();for(int i=0;i<60;i++)access("x".repeat(43)).andExpect(status().isNotFound());access(token).andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
  limits.clear();limits.unavailable=true;try{access(token).andExpect(status().isServiceUnavailable()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Synthetic headache"))));}finally{limits.clear();}
 }
 @Test void auditContainsNoTokensUrlsOrMedicalContents()throws Exception {
  var c=share(a,prepared(a).path("id").asText(),30);String token=c.path("token").asText();access(token).andExpect(status().isOk());req(a,"POST","/api/v1/shares/"+c.path("share").path("id").asText()+"/revoke",null).andExpect(status().isOk());
  var rows=db.queryForList("SELECT * FROM audit_event WHERE action LIKE 'SHARE_%'");assertEquals(3,rows.size());String text=rows.toString();assertFalse(text.contains(token));assertFalse(text.contains("/share#"));assertFalse(text.contains("Synthetic headache"));assertFalse(text.contains("token_hash"));assertEquals("SHARE",db.queryForObject("SELECT actor_kind FROM audit_event WHERE action='SHARE_ACCESSED'",String.class));
 }
 @Test void concurrentRevokeAccessHasNoPostRevocationAccess()throws Exception {
  var s=share(a,prepared(a).path("id").asText(),30);String token=s.path("token").asText(),id=s.path("share").path("id").asText();
  try(var pool=Executors.newFixedThreadPool(2)){var start=new CountDownLatch(1);var read=pool.submit(()->{start.await();return access(token).andReturn().getResponse().getStatus();});var revoke=pool.submit(()->{start.await();return req(a,"POST","/api/v1/shares/"+id+"/revoke",null).andReturn().getResponse().getStatus();});start.countDown();assertEquals(200,revoke.get(15,TimeUnit.SECONDS));assertTrue(Set.of(200,404).contains(read.get(15,TimeUnit.SECONDS)));}
  for(int i=0;i<3;i++)access(token).andExpect(status().isNotFound());
 }
 @Test void concurrentRevokeIsIdempotent()throws Exception {
  var s=share(a,prepared(a).path("id").asText(),30);String id=s.path("share").path("id").asText();
  try(var pool=Executors.newFixedThreadPool(2)){var start=new CountDownLatch(1);var x=pool.submit(()->{start.await();return req(a,"POST","/api/v1/shares/"+id+"/revoke",null).andReturn().getResponse().getStatus();});var y=pool.submit(()->{start.await();return req(a,"POST","/api/v1/shares/"+id+"/revoke",null).andReturn().getResponse().getStatus();});start.countDown();assertEquals(200,x.get(15,TimeUnit.SECONDS));assertEquals(200,y.get(15,TimeUnit.SECONDS));}
  assertEquals(1,db.queryForObject("SELECT count(*) FROM audit_event WHERE action='SHARE_REVOKED'",Integer.class));
 }
 @Test void managementRequiresSessionAndPaginatesWithoutCapabilities()throws Exception {
  mvc.perform(get("/api/v1/shares")).andExpect(status().isUnauthorized());String p=prepared(a).path("id").asText();for(int i=0;i<21;i++)share(a,p,30);
  var first=body(req(a,"GET","/api/v1/shares",null).andExpect(status().isOk()));var second=body(req(a,"GET","/api/v1/shares?page=1",null).andExpect(status().isOk()));assertEquals(20,first.path("items").size());assertEquals(1,second.path("items").size());assertEquals(21,first.path("total").asInt());assertFalse(first.toString().contains("token"));assertFalse(first.toString().contains("hash"));
  req(a,"GET","/api/v1/shares?page=-1",null).andExpect(status().isBadRequest());
 }
}
