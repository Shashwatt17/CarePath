package com.carepath.intelligence;
import static com.carepath.intelligence.ExtractionData.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.io.*;
/** No shell; command configured by operator, all file paths generated internally. */
public class TesseractOcr implements OcrProvider {
    private final String command;private final int timeoutSeconds;
    public TesseractOcr(String command,int timeoutSeconds) { this.command=command;this.timeoutSeconds=timeoutSeconds; }
    public Page recognize(Path image,int page,int width,int height) {
        Path output=image.resolveSibling("ocr-"+UUID.randomUUID()+".tsv");Process process=null;
        try {
           var builder=new ProcessBuilder(command,image.toAbsolutePath().toString(),"stdout","-l","eng","--psm","3","tsv");
            builder.environment().put("OMP_THREAD_LIMIT","1");
            builder.redirectOutput(output.toFile()).redirectError(ProcessBuilder.Redirect.DISCARD);
            process=builder.start();
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(timeoutSeconds);
            while(!process.waitFor(100,TimeUnit.MILLISECONDS)) {
                if(System.nanoTime()>deadline) throw new ProcessingFailure("OCR_TIMEOUT",true);
                if(Files.exists(output) && Files.size(output)>2097152) throw new ProcessingFailure("RESOURCE_LIMIT",false);
            }
            if(process.exitValue()!=0) throw new ProcessingFailure("OCR_FAILED",true);
            if(Files.size(output)>2097152) throw new ProcessingFailure("RESOURCE_LIMIT",false);
            return parse(Files.readString(output),page,width,height);
        } catch(IOException e) { throw new ProcessingFailure("OCR_UNAVAILABLE",true); }
        catch(InterruptedException e) { Thread.currentThread().interrupt();throw new ProcessingFailure("PROCESSING_INTERRUPTED",true); }
        finally { if(process!=null && process.isAlive()) terminate(process);try { Files.deleteIfExists(output); } catch(IOException ignored) {} }
    }
    public static void terminate(Process process) { process.descendants().forEach(p->p.destroyForcibly());process.destroyForcibly();try { process.waitFor(5,TimeUnit.SECONDS); } catch(InterruptedException e) { Thread.currentThread().interrupt(); } }
    public static Page parse(String tsv,int page,int width,int height) {
        class Row { StringBuilder text=new StringBuilder();double min=100;int left=Integer.MAX_VALUE,top=Integer.MAX_VALUE,right=0,bottom=0; }
        Map<String,Row> groups=new LinkedHashMap<>();
        for(String line:tsv.split("\n")) {
            String[] c=line.split("\t",12);if(c.length!=12 || !c[0].equals("5") || c[11].isBlank()) continue;
            try {
                double confidence=Double.parseDouble(c[10]);int x=Integer.parseInt(c[6]),y=Integer.parseInt(c[7]),w=Integer.parseInt(c[8]),h=Integer.parseInt(c[9]);
                if(!Double.isFinite(confidence) || confidence<0 || confidence>100 || x<0 || y<0 || w<1 || h<1 || (long)x+w>width || (long)y+h>height) throw new ProcessingFailure("OCR_FAILED",true);
                Row row=groups.computeIfAbsent(c[2]+":"+c[3]+":"+c[4],k->new Row());
                if(!row.text.isEmpty()) row.text.append(' ');row.text.append(c[11].replace("\u0000",""));row.min=Math.min(row.min,confidence);
                row.left=Math.min(row.left,x);row.top=Math.min(row.top,y);row.right=Math.max(row.right,x+w);row.bottom=Math.max(row.bottom,y+h);
            } catch(NumberFormatException e) { throw new ProcessingFailure("OCR_FAILED",true); }
        }
        StringBuilder text=new StringBuilder();List<Line> lines=new ArrayList<>();double total=0;
        for(Row row:groups.values()) {
            int start=text.length();text.append(row.text);int end=text.length();text.append('\n');
            double conf=Math.round(row.min)/100.0;total+=conf;
            lines.add(new Line(start,end,conf,new Box((double)row.left/width,(double)row.top/height,(double)(row.right-row.left)/width,(double)(row.bottom-row.top)/height)));
        }
        return new Page(page,text.toString(),Method.TESSERACT_OCR,lines.isEmpty()?0:Math.round(total/lines.size()*100)/100.0,lines);
    }
}
