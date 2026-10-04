package com.carepath.vault;
import com.carepath.identity.AuthTestSupport;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VaultIntegrationTest extends AuthTestSupport {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc;
    @Autowired TestClock clock; @Autowired TestLimits limits; @Autowired VaultProperties properties;
    @Autowired DocumentStorage storage; @Autowired BlobCleanup cleanup;
    String a,b;
    @BeforeEach void setup() throws Exception {
        clock.reset();limits.clear();jdbc.update("DELETE FROM audit_event");jdbc.update("DELETE FROM app_user");jdbc.update("DELETE FROM vault_blob_cleanup");
        try(var files=Files.list(Path.of(properties.root()))) { for(var p:files.toList()) Files.delete(p); }
        a=account("a");b=account("b");
    }
    String account(String name) throws Exception {
        String body=json.writeValueAsString(Map.of("email",name+"@example.invalid","password","Synthetic password 123!","displayName","Synthetic "+name));
        mvc.perform(post("/api/v1/auth/register").header("Origin","http://localhost:3000").header("X-CarePath-Client","web").contentType("application/json").content(body)).andExpect(status().isCreated());
        return json.readTree(mvc.perform(post("/api/v1/auth/login").header("Origin","http://localhost:3000").header("X-CarePath-Client","web").contentType("application/json").content(json.writeValueAsString(Map.of("email",name+"@example.invalid","password","Synthetic password 123!"))))
            .andReturn().getResponse().getContentAsString()).path("accessToken").asText();
    }
    static byte[] pdf() throws Exception { try(var d=new PDDocument();var out=new ByteArrayOutputStream()) { d.addPage(new PDPage());d.save(out);return out.toByteArray(); } }
    static byte[] image(String format) throws Exception { var image=new BufferedImage(20,20,BufferedImage.TYPE_INT_RGB);try(var out=new ByteArrayOutputStream()){ImageIO.write(image,format,out);return out.toByteArray();} }
    MockMultipartFile metadata() { return new MockMultipartFile("metadata","","application/json","{\"documentType\":\"LAB_REPORT\",\"documentDate\":\"2026-01-01\",\"providerName\":\"Synthetic Lab\",\"tags\":[\"synthetic\"]}".getBytes()); }
    ResultActions upload(String token,String name,String mime,byte[] bytes) throws Exception {
        return mvc.perform(multipart("/api/v1/documents").file(new MockMultipartFile("file",name,mime,bytes)).file(metadata()).header("Authorization","Bearer "+token));
    }
    JsonNode uploaded(String token) throws Exception { return json.readTree(upload(token,"synthetic.pdf","application/pdf",pdf()).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()); }
    String key(String id) { return jdbc.queryForObject("SELECT storage_key FROM medical_document WHERE id=?",String.class,UUID.fromString(id)); }
    String updateBody(long version) { return "{\"version\":"+version+",\"metadata\":{\"documentType\":\"OTHER\",\"documentDate\":null,\"providerName\":\"Updated\",\"tags\":[]}}"; }
    @Test void pdfLifecycleHashAuditAndSafeHeaders() throws Exception {
        byte[] original=pdf();var result=json.readTree(upload(a,"synthetic.pdf","application/pdf",original).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());String id=result.get("id").asText();
        assertEquals(DocumentService.hash(original),result.get("sha256").asText());assertEquals("UPLOADED",result.get("status").asText());assertFalse(result.has("ownerId"));assertFalse(result.has("storageKey"));assertTrue(key(id).matches("[a-f0-9]{32}"));
        mvc.perform(get("/api/v1/documents").header("Authorization","Bearer "+a)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/v1/documents/"+id).header("Authorization","Bearer "+a)).andExpect(status().isOk()).andExpect(header().string("Cache-Control",org.hamcrest.Matchers.containsString("no-store")));
        mvc.perform(get("/api/v1/documents/"+id+"/preview").header("Authorization","Bearer "+a)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        mvc.perform(get("/api/v1/documents/"+id+"/download").header("Authorization","Bearer "+a)).andExpect(status().isOk()).andExpect(content().bytes(original)).andExpect(header().string("Content-Disposition",org.hamcrest.Matchers.startsWith("attachment;"))).andExpect(header().string("X-Content-Type-Options","nosniff"));
        mvc.perform(put("/api/v1/documents/"+id).header("Authorization","Bearer "+a).contentType("application/json").content(updateBody(0))).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        String key=key(id);
        mvc.perform(delete("/api/v1/documents/"+id).header("Authorization","Bearer "+a)).andExpect(status().isNoContent());assertFalse(storage.exists(key));
        mvc.perform(get("/api/v1/documents/"+id+"/download").header("Authorization","Bearer "+a)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/documents/"+id).header("Authorization","Bearer "+a)).andExpect(status().isNotFound());
        for(String event:List.of("DOCUMENT_UPLOADED","DOCUMENT_VIEWED","DOCUMENT_DOWNLOADED","DOCUMENT_METADATA_UPDATED","DOCUMENT_DELETED")) assertTrue(jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action=?",Integer.class,event)>0);
        assertFalse(jdbc.queryForList("SELECT * FROM audit_event").toString().contains("synthetic.pdf"));
    }
    @ParameterizedTest @ValueSource(strings={"png","jpg","jpeg"}) void validImages(String extension) throws Exception {
        String mime=extension.equals("png")?"image/png":"image/jpeg";byte[] bytes=image(extension.equals("png")?"png":"jpeg");
        var doc=json.readTree(upload(a,"image."+extension,mime,bytes).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        mvc.perform(get("/api/v1/documents/"+doc.get("id").asText()+"/preview").header("Authorization","Bearer "+a)).andExpect(status().isOk()).andExpect(content().bytes(bytes));
    }
    @Test void bothDirectionsIdorDeniedThroughApi() throws Exception {
        for(String[] pair:List.of(new String[]{a,b},new String[]{b,a})) {
            String id=uploaded(pair[0]).get("id").asText();String other=pair[1];
            for(String suffix:List.of("","/preview","/download")) mvc.perform(get("/api/v1/documents/"+id+suffix).header("Authorization","Bearer "+other)).andExpect(status().isNotFound());
            mvc.perform(put("/api/v1/documents/"+id).header("Authorization","Bearer "+other).contentType("application/json").content(updateBody(0))).andExpect(status().isNotFound());
            mvc.perform(delete("/api/v1/documents/"+id).header("Authorization","Bearer "+other)).andExpect(status().isNotFound());
            var list=json.readTree(mvc.perform(get("/api/v1/documents?filename=synthetic&type=LAB_REPORT&provider=Lab").header("Authorization","Bearer "+other)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            for(var item:list.get("items")) assertNotEquals(id,item.get("id").asText());
        }
    }
    @ParameterizedTest @ValueSource(strings={"../../secret.pdf","..\\..\\secret.pdf","/etc/passwd","C:\\Windows\\test.pdf","report/../../../secret.pdf","%2e%2e%2fsecret.pdf","bad\u0000.pdf","x.html","report.exe","report.png"})
    void maliciousNamesAndMismatch(String name) throws Exception { upload(a,name,"application/pdf",pdf()).andExpect(status().isUnprocessableEntity());assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM medical_document",Integer.class)); }
    @Test void unicodeDuplicateAndLongNames() throws Exception {
        for(int i=0;i<2;i++) upload(a,"résultat血液.pdf","application/pdf",pdf()).andExpect(status().isCreated());
        assertEquals(2,jdbc.queryForObject("SELECT count(DISTINCT storage_key) FROM medical_document",Integer.class));
        upload(a,"x".repeat(256)+".pdf","application/pdf",pdf()).andExpect(status().isUnprocessableEntity());
    }
    @Test void emptyMimeSpoofingCorruptionAndScriptRejected() throws Exception {
        upload(a,"x.pdf","application/pdf",new byte[0]).andExpect(status().isUnprocessableEntity());
        upload(a,"x.pdf","text/html",pdf()).andExpect(status().isUnprocessableEntity());
        upload(a,"x.pdf","application/pdf","<script>alert(1)</script>".getBytes()).andExpect(status().isUnprocessableEntity());
        upload(a,"x.pdf","application/pdf","%PDF-1.7\n<script>x</script>\n%%EOF".getBytes()).andExpect(status().isUnprocessableEntity());
        upload(a,"x.png","image/png",new byte[]{(byte)137,80,78,71,13,10,26,10}).andExpect(status().isUnprocessableEntity());
        byte[] img=image("jpeg");upload(a,"x.jpg","image/jpeg",Arrays.copyOf(img,15)).andExpect(status().isUnprocessableEntity());
    }
    @Test void oversizedUploadRejected() throws Exception { upload(a,"x.pdf","application/pdf",new byte[properties.maxBytes()+1]).andExpect(status().isPayloadTooLarge()); }
    @Test void activePdfRejected() throws Exception {
        try(var d=new PDDocument();var out=new ByteArrayOutputStream()) {
            d.addPage(new PDPage());d.getDocumentCatalog().setOpenAction(new org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript("app.alert('no')"));d.save(out);
            upload(a,"active.pdf","application/pdf",out.toByteArray()).andExpect(status().isUnprocessableEntity());
        }
    }
    @Test void integrityTamperFailsAndAuditCommits() throws Exception {
        String id=uploaded(a).get("id").asText();Files.write(Path.of(properties.root()).resolve(key(id)),"tampered".getBytes());
        mvc.perform(get("/api/v1/documents/"+id+"/download").header("Authorization","Bearer "+a)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INTEGRITY_FAILURE"));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='DOCUMENT_INTEGRITY_FAILURE'",Integer.class));
    }
    @Test void deletionFailureIsDurableAndRetryRemovesBlob() throws Exception {
        String id=uploaded(a).get("id").asText(),key=key(id);Path p=Path.of(properties.root()).resolve(key);Files.delete(p);Files.createDirectory(p);Files.writeString(p.resolve("blocker"),"synthetic");
        mvc.perform(delete("/api/v1/documents/"+id).header("Authorization","Bearer "+a)).andExpect(status().isAccepted());
        mvc.perform(get("/api/v1/documents/"+id).header("Authorization","Bearer "+a)).andExpect(status().isNotFound());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM vault_blob_cleanup",Integer.class));Files.delete(p.resolve("blocker"));cleanup.retry();assertFalse(Files.exists(p));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM vault_blob_cleanup",Integer.class));
    }
    @Test void filtersPaginationAndUnknownInput() throws Exception {
        uploaded(a);uploaded(a);uploaded(a);uploaded(b);
        mvc.perform(get("/api/v1/documents?filename=synthetic&provider=lab&type=LAB_REPORT&from=2026-01-01&to=2026-01-01&status=UPLOADED&size=2&page=1").header("Authorization","Bearer "+a)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(3)).andExpect(jsonPath("$.items.length()").value(1));
        mvc.perform(get("/api/v1/documents?filename=%25").header("Authorization","Bearer "+a)).andExpect(jsonPath("$.total").value(0));
        for(String query:List.of("size=101","page=-1","type=INVALID","from=bad","from=2026-09-01&to=2026-01-01")) mvc.perform(get("/api/v1/documents?"+query).header("Authorization","Bearer "+a)).andExpect(status().isBadRequest());
    }
    @Test void optimisticConflictAndMassAssignment() throws Exception {
        String id=uploaded(a).get("id").asText();
        mvc.perform(put("/api/v1/documents/"+id).header("Authorization","Bearer "+a).contentType("application/json").content(updateBody(0))).andExpect(status().isOk());
        mvc.perform(put("/api/v1/documents/"+id).header("Authorization","Bearer "+a).contentType("application/json").content(updateBody(0))).andExpect(status().isConflict());
        mvc.perform(put("/api/v1/documents/"+id).header("Authorization","Bearer "+a).contentType("application/json").content(updateBody(1).replace("\"version\":1","\"ownerId\":\"ignored\",\"version\":1"))).andExpect(status().isBadRequest());
    }
    @Test void concurrentSameNameUploadsAndDownloadDelete() throws Exception {
        try(var pool=Executors.newFixedThreadPool(2)) {
            var one=pool.submit(()->uploaded(a));var two=pool.submit(()->uploaded(a));String id=one.get().get("id").asText();two.get();
            assertEquals(2,jdbc.queryForObject("SELECT count(DISTINCT storage_key) FROM medical_document",Integer.class));
            var read=pool.submit(()->mvc.perform(get("/api/v1/documents/"+id+"/download").header("Authorization","Bearer "+a)).andReturn().getResponse());
            var del=pool.submit(()->mvc.perform(delete("/api/v1/documents/"+id).header("Authorization","Bearer "+a)).andReturn().getResponse().getStatus());
            var response=read.get();assertTrue(Set.of(200,404,503).contains(response.getStatus()));assertEquals(204,del.get());
        }
    }
    @Test void storageRejectsTraversalAndSymlinkAndOverwrite() throws Exception {
        for(String key:List.of("../secret","/etc/passwd","a/b","%2e%2e")) { assertThrows(IOException.class,()->storage.retrieve(key));assertThrows(IOException.class,()->storage.delete(key)); }
        String key="a".repeat(32);storage.store(key,pdf());assertThrows(IOException.class,()->storage.store(key,pdf()));
        Path outside=Files.createTempFile("carepath-outside",".tmp"),link=Path.of(properties.root()).resolve("b".repeat(32));
        try { Files.createSymbolicLink(link,outside);assertThrows(IOException.class,()->storage.retrieve("b".repeat(32)));assertThrows(IOException.class,()->storage.delete("b".repeat(32)));assertTrue(Files.exists(outside)); } finally { Files.deleteIfExists(link);Files.delete(outside); }
    }
    @Test void unauthenticatedOperationsRemainClosed() throws Exception {
        mvc.perform(get("/error")).andExpect(status().isUnauthorized());
        mvc.perform(get("/error").header("Authorization","Bearer "+a)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/documents")).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/v1/documents").file(new MockMultipartFile("file","x.pdf","application/pdf",pdf())).file(metadata())).andExpect(status().isUnauthorized());
    }
    @Test void encryptedPdfAndExcessivePagesRejected() throws Exception {
        try(var d=new PDDocument();var out=new ByteArrayOutputStream()) {
            d.addPage(new PDPage());d.protect(new org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy("synthetic-owner","synthetic-reader",new org.apache.pdfbox.pdmodel.encryption.AccessPermission()));d.save(out);
            upload(a,"encrypted.pdf","application/pdf",out.toByteArray()).andExpect(status().isUnprocessableEntity());
        }
        try(var d=new PDDocument();var out=new ByteArrayOutputStream()) {
            for(int i=0;i<201;i++) d.addPage(new PDPage());d.save(out);
            upload(a,"large.pdf","application/pdf",out.toByteArray()).andExpect(status().isUnprocessableEntity());
        }
    }
    @Test void abandonedUploadRecoveryAndLiveOriginalProtection() throws Exception {
        String orphan="c".repeat(32);cleanup.reserve(orphan,false);storage.store(orphan,pdf());cleanup.retry();assertFalse(storage.exists(orphan));
        String id=uploaded(a).get("id").asText(),live=key(id);cleanup.reserve(live,false);cleanup.retry();assertTrue(storage.exists(live));
    }
    @Test void simultaneousUpdatesAndDeletesHaveOneWinner() throws Exception {
        String id=uploaded(a).get("id").asText();
        try(var pool=Executors.newFixedThreadPool(2)) {
            Callable<Integer> update=()->mvc.perform(put("/api/v1/documents/"+id).header("Authorization","Bearer "+a).contentType("application/json").content(updateBody(0))).andReturn().getResponse().getStatus();
            var x=pool.submit(update);var y=pool.submit(update);assertEquals(Set.of(200,409),Set.of(x.get(),y.get()));
            Callable<Integer> delete=()->mvc.perform(delete("/api/v1/documents/"+id).header("Authorization","Bearer "+a)).andReturn().getResponse().getStatus();
            x=pool.submit(delete);y=pool.submit(delete);assertEquals(Set.of(204,404),Set.of(x.get(),y.get()));
        }
    }
    @Test void multiPagePreviewAndRangeValidation() throws Exception {
        try(var d=new PDDocument();var out=new ByteArrayOutputStream()) {
            d.addPage(new PDPage());d.addPage(new PDPage());d.save(out);
            var uploaded=json.readTree(upload(a,"pages.pdf","application/pdf",out.toByteArray()).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
            assertEquals(2,uploaded.get("pageCount").asInt());String url="/api/v1/documents/"+uploaded.get("id").asText()+"/preview?page=";
            mvc.perform(get(url+2).header("Authorization","Bearer "+a)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
            mvc.perform(get(url+3).header("Authorization","Bearer "+a)).andExpect(status().isBadRequest());
        }
    }

    @Test void compressedPdfStreamBudgetIsEnforced() throws Exception {
        try(var d=new PDDocument();var out=new ByteArrayOutputStream()) {
            var page=new PDPage();d.addPage(page);var stream=new PDStream(d);
            try(var compressed=stream.createOutputStream(org.apache.pdfbox.cos.COSName.FLATE_DECODE)) {
                byte[] zeros=new byte[8192];for(int i=0;i<6401;i++) compressed.write(zeros);
            }
            page.setContents(stream);d.save(out);
            assertTrue(out.size()<1024*1024);
            upload(a,"compressed.pdf","application/pdf",out.toByteArray()).andExpect(status().isUnprocessableEntity());
        }
    }

}
