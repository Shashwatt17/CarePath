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
        record Word(String text,int x,int y,int w,int h,double confidence) { double center(){return y+h/2.0;} }
        class Row {
            final List<Word> words=new ArrayList<>();double center=0,height=0;
            void add(Word word){int n=words.size();center=(center*n+word.center())/(n+1);height=(height*n+word.h())/(n+1);words.add(word);}
        }
        Map<String,Row> groups=new LinkedHashMap<>();
        for(String line:tsv.split("\n")) {
            String[] c=line.split("\t",12);if(c.length!=12 || !c[0].equals("5") || c[11].isBlank())continue;
            try {
                double confidence=Double.parseDouble(c[10]);int x=Integer.parseInt(c[6]),y=Integer.parseInt(c[7]),w=Integer.parseInt(c[8]),h=Integer.parseInt(c[9]);
                if(!Double.isFinite(confidence) || confidence<0 || confidence>100 || x<0 || y<0 || w<1 || h<1 || (long)x+w>width || (long)y+h>height)throw new ProcessingFailure("OCR_FAILED",true);
                groups.computeIfAbsent(c[2]+":"+c[3]+":"+c[4],k->new Row()).add(new Word(c[11].replace("\u0000",""),x,y,w,h,confidence));
            }catch(NumberFormatException e){throw new ProcessingFailure("OCR_FAILED",true);}
        }
        // OCR block order is often column-major. Reconstruct aligned rows from geometry,
        // retaining horizontal gaps instead of flattening all result columns into prose.
        List<Row> ordered=new ArrayList<>(groups.values());ordered.sort(Comparator.comparingDouble(r->r.center));
        List<Row> rows=new ArrayList<>();
        for(Row group:ordered){
            Row best=null;double distance=Double.MAX_VALUE;
            for(int i=rows.size()-1;i>=Math.max(0,rows.size()-8);i--){
                Row row=rows.get(i);double delta=Math.abs(row.center-group.center);
                boolean separateColumns=row.words.stream().noneMatch(a->group.words.stream().anyMatch(b->a.x()<b.x()+b.w() && b.x()<a.x()+a.w()));
                if(separateColumns && delta<=Math.max(row.height,group.height)*.85 && delta<distance){best=row;distance=delta;}
            }
            if(best==null)rows.add(group);else for(Word word:group.words)best.add(word);
        }
        rows.sort(Comparator.comparingDouble(r->r.center));
        StringBuilder text=new StringBuilder();List<Line> lines=new ArrayList<>();double total=0;
        for(Row row:rows){
            row.words.sort(Comparator.comparingInt(Word::x));int start=text.length(),left=Integer.MAX_VALUE,top=Integer.MAX_VALUE,right=0,bottom=0,lastRight=-1;double minimum=100;
            for(Word word:row.words){
                if(lastRight>=0)text.append(word.x()-lastRight>Math.max(12,row.height*1.4)?'\t':' ');
                text.append(word.text());lastRight=word.x()+word.w();minimum=Math.min(minimum,word.confidence());
                left=Math.min(left,word.x());top=Math.min(top,word.y());right=Math.max(right,lastRight);bottom=Math.max(bottom,word.y()+word.h());
            }
            int end=text.length();text.append('\n');double confidence=Math.round(minimum)/100.0;total+=confidence;
            lines.add(new Line(start,end,confidence,new Box((double)left/width,(double)top/height,(double)(right-left)/width,(double)(bottom-top)/height)));
        }
        return new Page(page,text.toString(),Method.TESSERACT_OCR,lines.isEmpty()?0:Math.round(total/lines.size()*100)/100.0,lines);
    }
}
