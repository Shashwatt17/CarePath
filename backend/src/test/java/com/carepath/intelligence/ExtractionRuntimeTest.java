package com.carepath.intelligence;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.cos.COSName;
class ExtractionRuntimeTest {
    @TempDir Path directory;
    static Path sample(String name) { return Path.of("../sample-data/phase4",name); }
    IsolatedExtractionEngine engine(String command,int timeout,int pages) { return new IsolatedExtractionEngine(new ProcessingProperties(directory.toString(),command,timeout,1,pages,250000,false),new ObjectMapper().findAndRegisterModules()); }
    @Test void actualTextPdfPreservesPageTwoAndCleansTemporaryFiles() throws Exception {
        var result=engine("tesseract",30,20).extract(Files.readAllBytes(sample("september-lab.pdf")),"application/pdf");
        assertEquals(5,result.candidates().size());assertEquals(2,result.candidates().getFirst().source().page());assertFalse(result.ocrUsed());assertTrue(result.needsReview());assertNull(result.candidates().getLast().numericValue());
        try(var entries=Files.list(directory)) { assertEquals(0,entries.count()); }
    }
    @Test void corruptedPdfFailsWithoutAlteringInput() throws Exception {
        byte[] bytes=Files.readAllBytes(sample("malformed.pdf"));var copy=bytes.clone();assertThrows(ProcessingFailure.class,()->engine("tesseract",30,20).extract(bytes,"application/pdf"));assertArrayEquals(copy,bytes);
    }
    @Test void unavailableOcrIsAnExplicitRetryableFailure() throws Exception {
        var failure=assertThrows(ProcessingFailure.class,()->engine(directory.resolve("nonexistent-ocr").toString(),30,20).extract(Files.readAllBytes(sample("scanned-lab.png")),"image/png"));assertEquals("OCR_UNAVAILABLE",failure.code());assertTrue(failure.retryable());
    }
    @Test void maximumProcessingPagesEnforced() throws Exception { var failure=assertThrows(ProcessingFailure.class,()->engine("tesseract",30,1).extract(Files.readAllBytes(sample("september-lab.pdf")),"application/pdf"));assertEquals("RESOURCE_LIMIT",failure.code()); }
    @Test void malformedImageRejectedBeforeOcr() { var failure=assertThrows(ProcessingFailure.class,()->engine("tesseract",30,20).extract("<script>synthetic</script>".getBytes(),"image/png"));assertEquals("INVALID_DOCUMENT",failure.code()); }
    @Test void oversizedImageDimensionsRejected() throws Exception {
        var image=new java.awt.image.BufferedImage(5001,4000,java.awt.image.BufferedImage.TYPE_BYTE_GRAY);var out=new ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",out);image.flush();
        assertEquals("RESOURCE_LIMIT",assertThrows(ProcessingFailure.class,()->engine("tesseract",30,20).extract(out.toByteArray(),"image/png")).code());
    }
    @Test void ocrProcessTimeoutActuallyKillsProcess() throws Exception {
        // Test-only executable sleeps; provider is real ProcessBuilder, never a production fallback.
        Path script=directory.resolve("slow-ocr");Files.writeString(script,"#!/bin/sh\nexec sleep 10\n");assertTrue(script.toFile().setExecutable(true));
        long start=System.nanoTime();var failure=assertThrows(ProcessingFailure.class,()->new TesseractOcr(script.toString(),1).recognize(sample("scanned-lab.png"),1,1900,1050));assertEquals("OCR_TIMEOUT",failure.code());assertTrue((System.nanoTime()-start)/1e9<7);
    }
    @Test void extractionDeadlineTerminatesChildJvmAndOcr() throws Exception {
        Path script=directory.resolve("slow-ocr");Files.writeString(script,"#!/bin/sh\nexec sleep 20\n");assertTrue(script.toFile().setExecutable(true));
        var properties=new ProcessingProperties(directory.resolve("jobs").toString(),script.toString(),1,30,20,250000,false);
        var engine=new IsolatedExtractionEngine(properties,new ObjectMapper().findAndRegisterModules());
        assertEquals("EXTRACTION_TIMEOUT",assertThrows(ProcessingFailure.class,()->engine.extract(Files.readAllBytes(sample("scanned-lab.png")),"image/png")).code());
        try(var entries=Files.list(directory.resolve("jobs"))) { assertEquals(0,entries.count()); }
    }
    @Test void staleStagingIsReapedWithoutFollowingSymlinks() throws Exception {
        Path stale=Files.createDirectory(directory.resolve("job-123")),fresh=Files.createDirectory(directory.resolve("job-456"));
        Path outside=Files.createTempFile("carepath-outside-", ".txt");
        try {
            Files.writeString(outside,"synthetic");Files.createSymbolicLink(stale.resolve("link"),outside);
            Files.setLastModifiedTime(stale,java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis()-7200000));
            engine("tesseract",30,20).reapAbandoned();
            assertFalse(Files.exists(stale));assertTrue(Files.exists(fresh));assertEquals("synthetic",Files.readString(outside));
        } finally { Files.deleteIfExists(outside); }
    }
    @Test void temporaryRootSymlinkRejected() throws Exception {
        Path target=Files.createDirectory(directory.resolve("outside")),link=directory.resolve("root");Files.createSymbolicLink(link,target);
        var engine=new IsolatedExtractionEngine(new ProcessingProperties(link.toString(),"tesseract",30,1,20,250000,false),new ObjectMapper().findAndRegisterModules());
        assertEquals("WORKER_UNAVAILABLE",assertThrows(ProcessingFailure.class,()->engine.extract(Files.readAllBytes(sample("january-lab.pdf")),"application/pdf")).code());
        try(var files=Files.list(target)) { assertEquals(0,files.count()); }
    }
    @Test void compressedContentAbuseFailsInBoundedWorker() throws Exception {
        try(var pdf=new PDDocument();var out=new ByteArrayOutputStream()) {
            var page=new PDPage();pdf.addPage(page);var content=new PDStream(pdf);
            try(var stream=content.createOutputStream(COSName.FLATE_DECODE)) { byte[] zeros=new byte[8192];for(int i=0;i<6401;i++) stream.write(zeros); }
            page.setContents(content);pdf.save(out);assertThrows(ProcessingFailure.class,()->engine("tesseract",10,20).extract(out.toByteArray(),"application/pdf"));
        }
    }
    @Test void lostSupervisorPipeTerminatesWorker() throws Exception {
        Path slow=directory.resolve("pipe-ocr");Files.writeString(slow,"#!/bin/sh\nexec sleep 10\n");assertTrue(slow.toFile().setExecutable(true));
        var cmd=IsolatedExtractionEngine.workerCommand();cmd.addAll(List.of(sample("scanned-lab.png").toAbsolutePath().toString(),"image/png",directory.resolve("reply.json").toString(),slow.toString(),"30","20","250000",Long.toString(ProcessHandle.current().pid()),"30"));
        Process worker=new ProcessBuilder(cmd).redirectError(ProcessBuilder.Redirect.DISCARD).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        try { worker.getOutputStream().close();assertTrue(worker.waitFor(5,java.util.concurrent.TimeUnit.SECONDS));assertEquals(75,worker.exitValue()); }
        finally { if(worker.isAlive())worker.destroyForcibly(); }
    }

}
