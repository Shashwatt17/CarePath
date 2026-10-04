package com.carepath.intelligence;
import static com.carepath.intelligence.ExtractionData.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.io.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
@Component
public class IsolatedExtractionEngine implements ExtractionEngine {
    private final ProcessingProperties properties;private final ObjectMapper mapper;
    public IsolatedExtractionEngine(ProcessingProperties properties,ObjectMapper mapper) { this.properties=properties;this.mapper=mapper; }
    public Result extract(byte[] original,String mime) {
        Path work=null;Process process=null;
        try {
            Path root=Path.of(properties.tempRoot()).toAbsolutePath().normalize();Files.createDirectories(root);
            for(Path part=root;part!=null;part=part.getParent()) if(Files.isSymbolicLink(part)) throw new IOException("Unsafe temporary root");
            if(Files.getFileStore(root).supportsFileAttributeView("posix")) Files.setPosixFilePermissions(root,PosixFilePermissions.fromString("rwx------"));
            work=Files.createTempDirectory(root,"job-");
            Path input=work.resolve("input"),output=work.resolve("result.json");Files.write(input,original,StandardOpenOption.CREATE_NEW);
            List<String> command=workerCommand();command.addAll(List.of(input.toString(),mime,output.toString(),properties.tesseractCommand(),Integer.toString(properties.ocrTimeoutSeconds()),Integer.toString(properties.maxPages()),Integer.toString(properties.maxCharacters()),Long.toString(ProcessHandle.current().pid()),Integer.toString(properties.timeoutSeconds())));
            var builder=new ProcessBuilder(command);Map<String,String> env=builder.environment();String path=env.get("PATH");env.clear();if(path!=null) env.put("PATH",path);env.put("LANG","C.UTF-8");env.put("OMP_THREAD_LIMIT","1");
            builder.redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD);
            process=builder.start();
            if(!process.waitFor(properties.timeoutSeconds(),TimeUnit.SECONDS)) throw new ProcessingFailure("EXTRACTION_TIMEOUT",true);
            if(process.exitValue()!=0 || !Files.isRegularFile(output,LinkOption.NOFOLLOW_LINKS)) throw new ProcessingFailure("PARSER_FAILED",false);
            if(Files.size(output)>4194304) throw new ProcessingFailure("RESOURCE_LIMIT",false);
            WorkerReply result=mapper.readValue(Files.readAllBytes(output),WorkerReply.class);
            if(result.errorCode()!=null) throw new ProcessingFailure(allowedCode(result.errorCode()),result.retryable());
            new ProvenanceValidator().validate(result.result(),properties.maxPages(),properties.maxCharacters());return result.result();
        } catch(IOException e) { throw new ProcessingFailure("WORKER_UNAVAILABLE",true); }
        catch(InterruptedException e) { Thread.currentThread().interrupt();throw new ProcessingFailure("PROCESSING_INTERRUPTED",true); }
        finally {
            if(process!=null && process.isAlive()) TesseractOcr.terminate(process);
            if(work!=null) removeDirectory(work);
        }
    }
    /** Reap abandoned staging only after every valid worker lease/deadline has elapsed. */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString="3600000",initialDelayString="10000")
    public void reapAbandoned() {
        Path root=Path.of(properties.tempRoot()).toAbsolutePath().normalize();
        if(!Files.isDirectory(root,LinkOption.NOFOLLOW_LINKS)) return;
        for(Path part=root;part!=null;part=part.getParent()) if(Files.isSymbolicLink(part)) return;
        try(var entries=Files.list(root)) {
            for(Path entry:entries.filter(p->p.getFileName().toString().matches("job-[0-9]+")).toList()) {
                if(Files.isDirectory(entry,LinkOption.NOFOLLOW_LINKS) && Files.getLastModifiedTime(entry,LinkOption.NOFOLLOW_LINKS).toMillis()<System.currentTimeMillis()-3600000L) removeDirectory(entry);
            }
        } catch(IOException | ProcessingFailure failure) {
            org.slf4j.LoggerFactory.getLogger(IsolatedExtractionEngine.class).warn("Extraction staging cleanup unavailable");
        }
    }
    public static List<String> workerCommand() throws IOException {
        String classpath=System.getProperty("surefire.test.class.path",System.getProperty("java.class.path"));
        List<String> result=new ArrayList<>(List.of(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-Xmx256m","-XX:MaxDirectMemorySize=64m","-XX:+ExitOnOutOfMemoryError","-Djava.awt.headless=true"));
        boolean boot=false;
        if(!classpath.contains(File.pathSeparator) && classpath.endsWith(".jar")) try(var jar=new java.util.jar.JarFile(classpath)) { boot=jar.getJarEntry("BOOT-INF/classes/")!=null; }
        if(boot) result.add("-Dloader.main="+ExtractionWorkerMain.class.getName());
        result.addAll(List.of("-cp",classpath,boot?"org.springframework.boot.loader.launch.PropertiesLauncher":ExtractionWorkerMain.class.getName()));return result;
    }
    private String allowedCode(String code) {
        return Set.of("OCR_UNAVAILABLE","OCR_TIMEOUT","OCR_FAILED","RESOURCE_LIMIT","INVALID_DOCUMENT","NO_TEXT","PARSER_FAILED","PROCESSING_INTERRUPTED").contains(code)?code:"PARSER_FAILED";
    }
    static void removeDirectory(Path directory) {
        try(var files=Files.walk(directory)) { for(Path path:files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); }
        catch(IOException e) { throw new ProcessingFailure("TEMP_CLEANUP_FAILED",true); }
    }
}
