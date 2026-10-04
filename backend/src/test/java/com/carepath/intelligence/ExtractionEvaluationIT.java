package com.carepath.intelligence;
import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
/** Opt-in real-engine evaluation. Requires Tesseract with eng; never substitutes OCR output. */
class ExtractionEvaluationIT {
    @TempDir Path directory;
    @Test void measureAgainstIndependentSyntheticGroundTruth() throws Exception {
        var mapper=new ObjectMapper().findAndRegisterModules();
        var truth=mapper.readTree(Files.readString(Path.of("../evaluation/ground-truth.json")));
        var engine=new IsolatedExtractionEngine(new ProcessingProperties(directory.toString(),"tesseract",120,30,20,250000,false),mapper);
        Map<String,Integer> correct=new LinkedHashMap<>();for(String k:List.of("classification","name","value","unit","reference","date","provider","flag","provenance")) correct.put(k,0);
        List<Map<String,Object>> documents=new ArrayList<>();int expectedRows=0,predictedRows=0,matchedRows=0,classification=0;
        for(var expected:truth.path("documents")) {
            String filename=expected.path("file").asText();byte[] bytes=Files.readAllBytes(ExtractionRuntimeTest.sample(filename));
            var result=engine.extract(bytes,filename.endsWith(".png")?"image/png":"application/pdf");
            assertFalse(result.pages().isEmpty());assertFalse(result.candidates().isEmpty(),"Real extraction returned no candidates: "+filename);
            boolean ocr=expected.path("method").asText().equals("TESSERACT_OCR");assertEquals(ocr,result.ocrUsed());if(ocr) assertTrue(result.needsReview());
            if(result.classification().category().equals(expected.path("category").asText())) {classification++;correct.merge("classification",1,Integer::sum);}
            predictedRows+=result.candidates().size();List<String> mismatches=new ArrayList<>();Set<UUID> matched=new HashSet<>();
            for(var row:expected.path("rows")) {
                expectedRows++;var found=result.candidates().stream().filter(c->!matched.contains(c.id()) && normalize(c.originalTestName()).equals(normalize(row.path("name").asText()))).findFirst();
                if(found.isEmpty()) {mismatches.add("Missing test: "+row.path("name").asText());continue;}
                var c=found.get();matched.add(c.id());matchedRows++;correct.merge("name",1,Integer::sum);
                compare(correct,mismatches,"value",row.path("value").asText(),c.originalValue());
                compare(correct,mismatches,"unit",row.path("unit").asText(),c.originalUnit());
                compare(correct,mismatches,"reference",row.path("reference").asText(),c.referenceText());
                compare(correct,mismatches,"date",expected.path("date").asText(),c.reportDate()==null?null:c.reportDate().toString());
                compare(correct,mismatches,"provider",expected.path("provider").asText(),c.providerName());
                compare(correct,mismatches,"flag",row.path("flag").isNull()?null:row.path("flag").asText(),c.abnormalFlag());
                var source=c.source();var page=result.pages().get(source.page()-1);
                boolean provenance=source.page()==row.path("page").asInt() && source.method().name().equals(expected.path("method").asText())
                    && page.text().substring(source.start(),source.end()).equals(source.text())
                    && normalizeEvidence(source.text()).equals(normalizeEvidence(row.path("evidence").asText()));
                if(provenance) correct.merge("provenance",1,Integer::sum);else mismatches.add("Evidence mismatch: "+c.originalTestName());
            }
            documents.add(Map.of("file",filename,"state",result.needsReview()?"NEEDS_REVIEW":"COMPLETED","ocr",result.ocrUsed(),"expectedRows",expected.path("rows").size(),"predictedRows",result.candidates().size(),"mismatches",mismatches));
        }
        Map<String,Object> metrics=new LinkedHashMap<>();
        correct.forEach((k,v)->metrics.put(k,Map.of("correct",v,"total",k.equals("classification")?truth.path("documents").size():0)));
        for(var entry:correct.entrySet()) if(!entry.getKey().equals("classification")) metrics.put(entry.getKey(),Map.of("correct",entry.getValue(),"total",expectedRows,"ratio",entry.getValue()/(double)expectedRows));
        metrics.put("classification",Map.of("correct",classification,"total",documents.size(),"ratio",classification/(double)documents.size()));
        metrics.put("rowDetection",Map.of("matched",matchedRows,"expected",expectedRows,"predicted",predictedRows,"precision",matchedRows/(double)predictedRows,"recall",matchedRows/(double)expectedRows));
        var report=Map.of("executedAt",Instant.now().toString(),"dataset","synthetic phase4 only; not clinical accuracy","engine","PDFBox 3.0.8 + locally installed Tesseract eng","metrics",metrics,"documents",documents);
        Files.createDirectories(Path.of("../evaluation/results"));mapper.writerWithDefaultPrettyPrinter().writeValue(Path.of("../evaluation/results/latest.json").toFile(),report);
    }
    @Test void realJpegOcrPreservesCandidateEvidence() throws Exception {
        var image=javax.imageio.ImageIO.read(ExtractionRuntimeTest.sample("scanned-lab.png").toFile());
        var bytes=new java.io.ByteArrayOutputStream();assertTrue(javax.imageio.ImageIO.write(image,"jpeg",bytes));
        var engine=new IsolatedExtractionEngine(new ProcessingProperties(directory.toString(),"tesseract",120,30,20,250000,false),new ObjectMapper().findAndRegisterModules());
        var result=engine.extract(bytes.toByteArray(),"image/jpeg");
        assertTrue(result.ocrUsed());assertTrue(result.needsReview());
        assertTrue(result.candidates().stream().anyMatch(c->c.originalTestName().equals("Hemoglobin") && c.source().text().contains("10.4")));
        assertTrue(result.candidates().stream().allMatch(c->c.source().box()!=null && c.confidence()!=ExtractionData.Band.HIGH));
    }
    private static void compare(Map<String,Integer> correct,List<String> mismatch,String field,String expected,String actual) { if(Objects.equals(expected,actual)) correct.merge(field,1,Integer::sum);else mismatch.add(field+": expected "+expected+", actual "+actual); }
    private static String normalize(String value) { return value.strip().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT); }
    private static String normalizeEvidence(String value) { return value.strip().replaceAll("\\s*\\|\\s*","|").replaceAll("\\s+"," ").replaceAll("\\|$",""); }
}
