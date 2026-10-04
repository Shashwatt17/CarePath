package com.carepath.identity;

import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in: build frontend first, then mvn -Dtest=FrontendProxyIT test. Ports 8080/3000 must be free. */
class FrontendProxyIT extends AuthTestSupport {
    @Autowired ObjectMapper json;
    @Autowired com.carepath.intelligence.ProcessingWorker processingWorker;
    @DynamicPropertySource static void proxyPort(DynamicPropertyRegistry r) { r.add("server.port",()->8080); }
    @Test void productionNextProxyPreservesCookiesOriginAndAuthFlow() throws Exception {
        Path frontend=Path.of("../frontend").toAbsolutePath().normalize();
        Path log=Files.createTempFile("carepath-next-proxy", ".txt");
        Process process=new ProcessBuilder("node","node_modules/next/dist/bin/next","start","--hostname","127.0.0.1","--port","3000")
            .directory(frontend.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            var client=HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(2)).build();
            boolean ready=false;
            for(int i=0;i<50;i++) {
                try { ready=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:3000/login")).GET().build(),HttpResponse.BodyHandlers.discarding()).statusCode()==200; }
                catch(java.io.IOException e) { /* Wait only for startup, never suppress a test failure. */ }
                if(ready) break; Thread.sleep(200);
            }
            assertTrue(ready,"Next.js production server did not start: "+Files.readString(log));
            String root="http://127.0.0.1:3000/api/v1/auth/";
            java.util.function.BiFunction<String,String,HttpRequest.Builder> req=(path,body)->HttpRequest.newBuilder(URI.create(root+path))
                .header("Origin","http://localhost:3000").header("X-CarePath-Client","web").header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
            String credentials=json.writeValueAsString(Map.of("email","proxy@example.invalid","password","Synthetic proxy password!"));
            assertEquals(201,client.send(req.apply("register",json.writeValueAsString(Map.of("email","proxy@example.invalid","password","Synthetic proxy password!","displayName","Synthetic Proxy"))).build(),HttpResponse.BodyHandlers.ofString()).statusCode());
            var login=client.send(req.apply("login",credentials).build(),HttpResponse.BodyHandlers.ofString());assertEquals(200,login.statusCode());
            String token=json.readTree(login.body()).get("accessToken").asText();
            String cookie=login.headers().firstValue("set-cookie").orElseThrow().split(";",2)[0];
            assertEquals(200,client.send(HttpRequest.newBuilder(URI.create(root+"me")).header("Authorization","Bearer "+token).GET().build(),HttpResponse.BodyHandlers.ofString()).statusCode());
            // Real multipart over HTTP through production Next, including >10 MiB proxy transport.
            var image=new java.awt.image.BufferedImage(2000,2000,java.awt.image.BufferedImage.TYPE_INT_RGB);
            var random=new java.util.Random(2026);
            for(int y=0;y<2000;y++) for(int x=0;x<2000;x++) image.setRGB(x,y,random.nextInt());
            var png=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",png);
            byte[] original=png.toByteArray();assertTrue(original.length>10*1024*1024);
            String boundary="CarePathSyntheticBoundary";
            var multipart=new java.io.ByteArrayOutputStream();
            multipart.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"metadata\"\r\nContent-Type: application/json\r\n\r\n{\"documentType\":\"OTHER\",\"tags\":[]}\r\n--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"synthetic.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            multipart.write(original);multipart.write(("\r\n--"+boundary+"--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String docs="http://127.0.0.1:3000/api/v1/documents";
            var upload=client.send(HttpRequest.newBuilder(URI.create(docs)).header("Authorization","Bearer "+token)
                .header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(multipart.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(201,upload.statusCode(),upload.body()+Files.readString(log));String id=json.readTree(upload.body()).get("id").asText();
            java.util.function.Function<String,HttpRequest.Builder> documentRequest=(suffix)->HttpRequest.newBuilder(URI.create(docs+suffix)).header("Authorization","Bearer "+token);
            assertEquals(1,json.readTree(client.send(documentRequest.apply("").GET().build(),HttpResponse.BodyHandlers.ofString()).body()).get("total").asInt());
            for(String suffix:java.util.List.of("/preview","/download")) {
                var content=client.send(documentRequest.apply("/"+id+suffix).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
                assertEquals(200,content.statusCode());assertArrayEquals(original,content.body());
            }
            assertEquals(204,client.send(documentRequest.apply("/"+id).DELETE().build(),HttpResponse.BodyHandlers.discarding()).statusCode());
            assertEquals(404,client.send(documentRequest.apply("/"+id+"/download").GET().build(),HttpResponse.BodyHandlers.discarding()).statusCode());
            // Phase 4: the real same-origin proxy carries processing/status/evidence routes too.
            byte[] lab=Files.readAllBytes(Path.of("../sample-data/phase4/september-lab.pdf"));
            multipart.reset();
            multipart.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"metadata\"\r\nContent-Type: application/json\r\n\r\n{\"documentType\":\"LAB_REPORT\",\"tags\":[]}\r\n--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"september.pdf\"\r\nContent-Type: application/pdf\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            multipart.write(lab);multipart.write(("\r\n--"+boundary+"--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var labUpload=client.send(HttpRequest.newBuilder(URI.create(docs)).header("Authorization","Bearer "+token).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(multipart.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(201,labUpload.statusCode());String labId=json.readTree(labUpload.body()).path("id").asText();
            assertEquals(202,client.send(documentRequest.apply("/"+labId+"/process").POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString()).statusCode());
            assertTrue(processingWorker.runOne());
            var processed=client.send(documentRequest.apply("/"+labId+"/processing-status").GET().build(),HttpResponse.BodyHandlers.ofString());
            assertEquals("NEEDS_REVIEW",json.readTree(processed.body()).path("state").asText());
            var extracted=client.send(documentRequest.apply("/"+labId+"/extraction").GET().build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(200,extracted.statusCode());var rows=json.readTree(extracted.body()).path("result").path("candidates");assertEquals(5,rows.size());
            var evidence=client.send(documentRequest.apply("/"+labId+"/extraction/evidence/"+rows.get(0).path("id").asText()).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(200,evidence.statusCode());assertEquals(2,json.readTree(evidence.body()).path("page").asInt());
            assertEquals(200,client.send(documentRequest.apply("/"+labId+"/preview?page=2").GET().build(),HttpResponse.BodyHandlers.discarding()).statusCode());
            // Phase 5: owner-scoped review and trusted read through the actual Next HTTP proxy.
            String reviewRoot="http://127.0.0.1:3000/api/v1/review/candidates";
            var queue=client.send(HttpRequest.newBuilder(URI.create(reviewRoot+"?documentId="+labId)).header("Authorization","Bearer "+token).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(200,queue.statusCode());var reviewRows=json.readTree(queue.body()).path("items");assertEquals(5,reviewRows.size());
            for(var row:reviewRows) if(row.path("extracted").path("originalTestName").asText().equals("Hemoglobin")) {
                var accepted=client.send(HttpRequest.newBuilder(URI.create(reviewRoot+"/"+row.path("id").asText()+"/confirm")).header("Authorization","Bearer "+token).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{\"version\":0}")).build(),HttpResponse.BodyHandlers.ofString());
                assertEquals(200,accepted.statusCode());var observation=json.readTree(accepted.body()).path("observation");assertEquals("10.4",observation.path("normalization").path("normalized").path("value").asText());
                assertEquals(200,client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:3000/api/v1/observations/"+observation.path("id").asText())).header("Authorization","Bearer "+token).GET().build(),HttpResponse.BodyHandlers.discarding()).statusCode());
                var history=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:3000/api/v1/history/concepts/"+observation.path("verified").path("conceptId").asText())).header("Authorization","Bearer "+token).GET().build(),HttpResponse.BodyHandlers.ofString());
                assertEquals(200,history.statusCode());var historyPoint=json.readTree(history.body()).path("history").path("items").get(0);assertEquals(observation.path("id"),historyPoint.path("id"));assertEquals(2,historyPoint.path("evidence").path("page").asInt());
                assertEquals(200,client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:3000/api/v1/history/events")).header("Authorization","Bearer "+token).GET().build(),HttpResponse.BodyHandlers.discarding()).statusCode());

                String assistantRoot="http://127.0.0.1:3000/api/v1/assistant";
                var explained=client.send(HttpRequest.newBuilder(URI.create(assistantRoot+"/ask")).header("Authorization","Bearer "+token).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(java.util.Map.of("question","Explain this reading","observationId",observation.path("id").asText(),"useAi",false)))).build(),HttpResponse.BodyHandlers.ofString());
                assertEquals(200,explained.statusCode());assertEquals("DETERMINISTIC",json.readTree(explained.body()).path("mode").asText());assertEquals(2,json.readTree(explained.body()).path("evidence").get(0).path("observation").path("evidence").path("page").asInt());
                var saved=client.send(HttpRequest.newBuilder(URI.create(assistantRoot+"/questions")).header("Authorization","Bearer "+token).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(java.util.Map.of("text","Could we discuss this result?","observationIds",java.util.List.of(observation.path("id").asText()))))).build(),HttpResponse.BodyHandlers.ofString());assertEquals(201,saved.statusCode());String savedId=json.readTree(saved.body()).path("id").asText();
                assertEquals(200,client.send(HttpRequest.newBuilder(URI.create(assistantRoot+"/questions/"+savedId)).header("Authorization","Bearer "+token).GET().build(),HttpResponse.BodyHandlers.discarding()).statusCode());
                assertEquals(204,client.send(HttpRequest.newBuilder(URI.create(assistantRoot+"/questions/"+savedId)).header("Authorization","Bearer "+token).DELETE().build(),HttpResponse.BodyHandlers.discarding()).statusCode());

            }
            assertEquals(204,client.send(documentRequest.apply("/"+labId).DELETE().build(),HttpResponse.BodyHandlers.discarding()).statusCode());
            var refresh=client.send(req.apply("refresh","").header("Cookie",cookie).build(),HttpResponse.BodyHandlers.ofString());assertEquals(200,refresh.statusCode());
            String rotated=refresh.headers().firstValue("set-cookie").orElseThrow().split(";",2)[0];
            assertNotEquals(cookie,rotated);
            assertEquals(204,client.send(req.apply("logout","").header("Cookie",rotated).build(),HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(401,client.send(req.apply("refresh","").header("Cookie",rotated).build(),HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(401,client.send(HttpRequest.newBuilder(URI.create(root+"me")).header("Authorization","Bearer "+token).GET().build(),HttpResponse.BodyHandlers.ofString()).statusCode());
        } finally { process.destroyForcibly(); process.waitFor(); Files.deleteIfExists(log); }
    }
}
