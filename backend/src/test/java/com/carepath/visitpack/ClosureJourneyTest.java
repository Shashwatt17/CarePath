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
class ClosureJourneyTest extends AuthTestSupport {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate db;
 @Autowired TestClock clock;@Autowired TestLimits limits;@Autowired ProcessingWorker worker;
 String a,b;final String base="/api/v1/visit-packs";
 @BeforeEach void setup()throws Exception {clock.reset();limits.clear();db.update("DELETE FROM audit_event");db.update("DELETE FROM app_user");a=account("journey-a");b=account("journey-b");}
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

 @Autowired com.carepath.care.ReminderService reminders;
 @Test void syntheticRecordToSharedPackAndActivityJourney()throws Exception {
  String jan=process(a,"january-lab.pdf"),apr=process(a,"april-lab.pdf"),sep=process(a,"september-lab.pdf");
  var j=accept(a,jan);var m=accept(a,apr);var s=accept(a,sep);
  String prior=oid(m,"Hemoglobin"),current=oid(s,"Hemoglobin");
  var change=body(req(a,"GET","/api/v1/history/compare?previous="+prior+"&current="+current,null).andExpect(status().isOk()));
  assertEquals("DECREASED",change.path("type").asText());assertEquals("11.3",change.path("previousValue").asText());assertEquals("10.4",change.path("currentValue").asText());
  assertEquals(sep,change.path("current").path("evidence").path("documentId").asText());assertEquals(2,change.path("current").path("evidence").path("page").asInt());
  var answer=body(req(a,"POST","/api/v1/assistant/ask",Map.of("question","How has my hemoglobin changed?","useAi",false)).andExpect(status().isOk()));
  assertEquals("DETERMINISTIC",answer.path("mode").asText());assertTrue(answer.toString().contains("12.1"));assertTrue(answer.toString().contains("10.4"));
  String sym=sid(a),ap=create(a,"appointments",appointment(3600)).path("id").asText(),f=candidate(a,"Review after 6 weeks");
  req(a,"POST","/api/v1/care/follow-ups/"+f+"/decision",decision("CONFIRM")).andExpect(status().isOk());
  for(UUID reminder:reminders.due())reminders.deliver(reminder);
  assertTrue(readCare(a,"notifications").path("total").asInt()>0);
  var events=body(req(a,"GET","/api/v1/history/events?size=100",null).andExpect(status().isOk()));
  var kinds=new HashSet<String>();for(var e:events.path("items"))kinds.add(e.path("type").asText());assertTrue(kinds.containsAll(Set.of("OBSERVATION","DOCUMENT","SYMPTOM","APPOINTMENT","FOLLOW_UP")));
  String question=qid(a,"Should we discuss the observed changes?");var h=selection("CHANGE",prior);h.put("otherId",current);
  var x=input(List.of(selection("OBSERVATION",oid(j,"Hemoglobin")),h,selection("SYMPTOM",sym),selection("FOLLOW_UP",f),selection("SAVED_QUESTION",question)));x.put("appointmentId",ap);
  var generated=generate(a,pack(a,x));String pid=generated.path("id").asText();try(var doc=Loader.loadPDF(pdf(a,pid))){String text=new PDFTextStripper().getText(doc);assertTrue(text.contains("10.4"));assertTrue(text.contains("Page 2"));}
  var share=body(req(a,"POST","/api/v1/shares",Map.of("packId",pid,"expiryMinutes",30)).andExpect(status().isCreated()));String token=share.path("token").asText();
  var publicPack=body(mvc.perform(post("/api/v1/public/share/access").contentType("application/json").content(json.writeValueAsString(Map.of("token",token)))).andExpect(status().isOk()));assertTrue(publicPack.toString().contains("10.4"));assertFalse(publicPack.toString().contains(sep));
  req(a,"POST","/api/v1/shares/"+share.path("share").path("id").asText()+"/revoke",null).andExpect(status().isOk());
  mvc.perform(post("/api/v1/public/share/access").contentType("application/json").content(json.writeValueAsString(Map.of("token",token)))).andExpect(status().isNotFound());
  var activity=body(req(a,"GET","/api/v1/activity?category=SHARE",null).andExpect(status().isOk()));assertTrue(activity.toString().contains("SHARE_REVOKED"));assertFalse(activity.toString().contains(token));assertEquals(0,body(req(b,"GET","/api/v1/activity?category=SHARE",null).andExpect(status().isOk())).path("total").asInt());
  req(b,"GET",base+"/"+pid,null).andExpect(status().isNotFound());
 }
}
