package com.carepath.intelligence;
import java.nio.file.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import static com.carepath.intelligence.ExtractionData.*;
/** Dedicated child JVM entry point. Never starts Spring or receives credentials. */
public class ExtractionWorkerMain {
    public static void main(String[] args) throws Exception {
        if(args.length>=9) {
            // A supervisor-owned stdin pipe works even when /proc hides the parent PID.
            // EOF means the supervisor disappeared; neither this guard nor the deadline is optional.
            long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(Integer.parseInt(args[8])+5L);
            Runtime.getRuntime().addShutdownHook(new Thread(()->ProcessHandle.current().descendants().forEach(ProcessHandle::destroyForcibly)));
            Thread parentWatch=new Thread(()->{
                try { while(System.in.read()!=-1) { /* No input is expected on the liveness pipe. */ } }
                catch(java.io.IOException ignored) { /* A broken pipe also means supervision is lost. */ }
                System.exit(75);
            },"extraction-parent-pipe");parentWatch.setDaemon(true);parentWatch.start();
            Thread watchdog=new Thread(()->{
                try { while(System.nanoTime()<deadline) Thread.sleep(500); }
                catch(InterruptedException ignored) { Thread.currentThread().interrupt(); }
                System.exit(75);
            },"extraction-deadline");watchdog.setDaemon(true);watchdog.start();
        }
        // Child stdout/stderr are discarded by its supervisor, including untrusted parser diagnostics.
        Path input=Path.of(args[0]),output=Path.of(args[2]);WorkerReply reply;
        try {
            var ocr=new TesseractOcr(args[3],Integer.parseInt(args[4]));
            var pipeline=new ExtractionPipeline(new DocumentTextExtractor(ocr,Integer.parseInt(args[5]),Integer.parseInt(args[6])));
            reply=new WorkerReply(pipeline.extract(input,args[1],input.getParent()),null,false);
        } catch(ProcessingFailure failure) { reply=new WorkerReply(null,failure.code(),failure.retryable()); }
        catch(Exception | LinkageError failure) { reply=new WorkerReply(null,"PARSER_FAILED",false); }
        var mapper=new ObjectMapper().findAndRegisterModules();byte[] bytes=mapper.writeValueAsBytes(reply);
        if(bytes.length>4194304) bytes=mapper.writeValueAsBytes(new WorkerReply(null,"RESOURCE_LIMIT",false));
        Files.write(output,bytes,StandardOpenOption.CREATE_NEW);
    }
}
