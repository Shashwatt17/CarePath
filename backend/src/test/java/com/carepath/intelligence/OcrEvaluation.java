package com.carepath.intelligence;

import static com.carepath.intelligence.ExtractionData.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

/** Offline synthetic evaluation only. Never called by the application or used to replace readings. */
public final class OcrEvaluation {
    record Draw(int x,int y,String text) {}
    record Expected(String name,String value,String unit,String date) {}
    record Fixture(String id,String split,String kind,String text,List<Draw> draws,List<Expected> expected,String date,String provider) {}
    record Manifest(String version,String note,List<Fixture> cases) {}
    record Metrics(int expected,int returned,int matchedRows,int exactValues,int exactUnits,int exactDates,int missingRows,int extraRows,int numericAbstentions) {}
    record Retry(String name,String originalValue,String status,String alternativeText,List<String> alternativeValues,boolean requiresReview,boolean canAutoReplace) {}
    record CaseResult(String id,String split,String kind,String error,Metrics metrics,boolean dateExact,boolean providerExact,List<Expected> actual,List<Retry> retries,long elapsedMillis) {}
    static final ObjectMapper JSON=new ObjectMapper().findAndRegisterModules();

    public static void main(String[] args)throws Exception {
        if(args.length<2)throw new IllegalArgumentException("Usage: OcrEvaluation manifest.json output-directory [tesseract]");
        Path output=Path.of(args[1]);Files.createDirectories(output);
        String command=args.length>2?args[2]:"tesseract";
        Manifest manifest=JSON.readValue(Files.readAllBytes(Path.of(args[0])),Manifest.class);
        List<CaseResult> results=new ArrayList<>();
        for(Fixture f:manifest.cases()){
            if(!f.id().matches("[a-z0-9-]{1,80}"))throw new IllegalArgumentException("Invalid fixture id");
            Path work=Files.createTempDirectory(output,f.id()+"-");long start=System.nanoTime();Result result=null;String error=null;BufferedImage input=null;
            try {
                if(f.kind().equals("text"))result=new ExtractionPipeline(null).parse(List.of(TextPages.nativePage(1,f.text())));
                else {
                    input=render(f);Path image=work.resolve("input.png");ImageIO.write(input,"png",image.toFile());
                    result=new ExtractionPipeline(new DocumentTextExtractor(new TesseractOcr(command,15),20,250000)).extract(image,"image/png",work);
                }
                new ProvenanceValidator().validate(result,20,250000);
            }catch(ProcessingFailure failure){error=failure.code();}
            List<Candidate> candidates=result==null?List.of():result.candidates();
            List<Retry> retries=input==null||result==null?List.of():retryAmbiguous(input,result,work,command);
            List<Expected> actual=candidates.stream().map(c->new Expected(c.originalTestName(),c.originalValue(),c.originalUnit(),c.reportDate()==null?null:c.reportDate().toString())).toList();
            results.add(new CaseResult(f.id(),f.split(),f.kind(),error,measure(f.expected(),candidates),result!=null&&result.information().date()!=null&&Objects.equals(f.date(),result.information().date().value()),result!=null&&result.information().provider()!=null&&Objects.equals(f.provider(),result.information().provider().value()),actual,retries,TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start)));
            if(input!=null)input.flush();
            try(var paths=Files.walk(work)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
            System.out.println(f.id()+": "+results.getLast().metrics());
        }
        Map<String,Object> report=new LinkedHashMap<>();report.put("corpusVersion",manifest.version());report.put("pipelineVersion",ExtractionPipeline.VERSION);report.put("javaVersion",System.getProperty("java.version"));report.put("note",manifest.note());report.put("retryPolicy","Experiment only: at most two ambiguous rows, 5 seconds per process, 8 seconds total budget, 1 MP maximum crop; alternatives never replace originals.");report.put("cases",results);
        JSON.writerWithDefaultPrettyPrinter().writeValue(output.resolve("report.json").toFile(),report);
    }

    static Metrics measure(List<Expected> expected,List<Candidate> actual){
        Set<Integer> used=new HashSet<>();int matched=0,values=0,units=0,dates=0,abstentions=0;
        for(Expected e:expected){
            int best=-1;
            // Identity matching is lexical name + date when possible, never numeric proximity/range fit.
            for(int i=0;i<actual.size();i++)if(!used.contains(i)&&e.name().equals(actual.get(i).originalTestName())){
                if(best<0)best=i;
                if(Objects.equals(e.date(),actual.get(i).reportDate()==null?null:actual.get(i).reportDate().toString())){best=i;break;}
            }
            if(best<0)continue;used.add(best);matched++;Candidate c=actual.get(best);
            if(Objects.equals(e.value(),c.originalValue()))values++;
            if(Objects.equals(e.unit(),c.originalUnit()))units++;
            if(Objects.equals(e.date(),c.reportDate()==null?null:c.reportDate().toString()))dates++;
            if(c.numericValue()==null)abstentions++;
        }
        return new Metrics(expected.size(),actual.size(),matched,values,units,dates,expected.size()-matched,actual.size()-used.size(),abstentions);
    }

    static BufferedImage render(Fixture f){
        BufferedImage image=new BufferedImage(1000,900,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        g.setColor(Color.WHITE);g.fillRect(0,0,1000,900);g.setColor(f.kind().equals("contrast")?new Color(185,185,185):Color.BLACK);
        g.setFont(new Font("SansSerif",Font.PLAIN,f.kind().equals("small")?12:20));
        for(Draw d:f.draws())g.drawString(d.text(),d.x(),d.y());g.dispose();
        if(!f.kind().equals("rotated"))return image;
        BufferedImage rotated=new BufferedImage(900,1000,BufferedImage.TYPE_INT_RGB);g=rotated.createGraphics();g.translate(900,0);g.rotate(Math.PI/2);g.drawImage(image,0,0,null);g.dispose();image.flush();return rotated;
    }

    static List<Retry> retryAmbiguous(BufferedImage input,Result result,Path work,String command)throws Exception {
        List<Retry> retries=new ArrayList<>();BufferedImage prepared=null;long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
        try {
            for(Candidate c:result.candidates()){
                if(retries.size()>=2||System.nanoTime()>=deadline)break;
                if(!c.reasons().contains("AMBIGUOUS_NUMERIC_PUNCTUATION"))continue;
                Page page=result.pages().get(c.source().page()-1);
                Line line=page.lines().stream().filter(l->l.box()!=null&&TextPages.source(page,l).text().contains(c.originalTestName())&&TextPages.source(page,l).text().contains(c.originalValue())).findFirst().orElse(null);
                if(line==null)continue;
                if(prepared==null)prepared=OcrImagePreparation.prepare(input);
                Box b=line.box();int x=Math.max(0,(int)(b.x()*prepared.getWidth())-10),y=Math.max(0,(int)(b.y()*prepared.getHeight())-10);
                int right=Math.min(prepared.getWidth(),(int)Math.ceil((b.x()+b.width())*prepared.getWidth())+10),bottom=Math.min(prepared.getHeight(),(int)Math.ceil((b.y()+b.height())*prepared.getHeight())+10);
                int width=right-x,height=bottom-y;
                if(width<1||height<1||(long)width*height>1000000)continue;
                Path crop=work.resolve("retry.png"),tsv=work.resolve("retry.tsv");Process process=null;
                String status="UNAVAILABLE",text="";List<String> alternatives=List.of();
                try {
                    ImageIO.write(prepared.getSubimage(x,y,width,height),"png",crop.toFile());
                    var builder=new ProcessBuilder(command,crop.toString(),"stdout","-l","eng","--psm","7","tsv");builder.environment().put("OMP_THREAD_LIMIT","1");builder.redirectOutput(tsv.toFile()).redirectError(ProcessBuilder.Redirect.DISCARD);process=builder.start();
                    long until=Math.min(deadline,System.nanoTime()+TimeUnit.SECONDS.toNanos(5));
                    while(process.isAlive()&&System.nanoTime()<until){if(Files.size(tsv)>2097152)break;process.waitFor(50,TimeUnit.MILLISECONDS);}
                    if(process.isAlive()){status="BUDGET_EXHAUSTED";TesseractOcr.terminate(process);}
                    else if(process.exitValue()==0&&Files.size(tsv)<=2097152){
                        Page retry=TesseractOcr.parse(Files.readString(tsv),1,width,height);text=retry.text();
                        alternatives=new LabObservationExtractor().extract(List.of(retry),result.information()).stream().filter(a->a.originalTestName().equals(c.originalTestName())&&Objects.equals(a.originalUnit(),c.originalUnit())).map(Candidate::originalValue).filter(Objects::nonNull).distinct().toList();
                        status=alternatives.isEmpty()?"NO_MATCHING_ROW":alternatives.stream().allMatch(v->v.equals(c.originalValue()))?"UNCHANGED_UNVERIFIED":"DISAGREEMENT_REQUIRES_REVIEW";
                    }
                }finally{if(process!=null&&process.isAlive())TesseractOcr.terminate(process);Files.deleteIfExists(crop);Files.deleteIfExists(tsv);}
                retries.add(new Retry(c.originalTestName(),c.originalValue(),status,text,alternatives,true,false));
            }
        }finally{if(prepared!=null)prepared.flush();}
        return retries;
    }
}
