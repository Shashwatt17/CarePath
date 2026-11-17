package com.carepath.intelligence;
import com.carepath.vision.*;
import com.carepath.foundation.ApiFailure;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
/** Synthetic-only explicit runner. No live provider calls unless --send-synthetic is supplied. */
public final class VisionEvaluation {
    public static void main(String[] args)throws Exception {
        if(args.length<2)throw new IllegalArgumentException("Usage: VisionEvaluation manifest.json output-directory [--send-synthetic]");
        boolean send=args.length==3 && args[2].equals("--send-synthetic");
        if(args.length>2 && !send)throw new IllegalArgumentException("Unknown evaluation mode");
        var json=OcrEvaluation.JSON;var manifest=json.readValue(Files.readAllBytes(Path.of(args[0])),OcrEvaluation.Manifest.class);
        if(manifest.cases().size()>100)throw new IllegalArgumentException("Corpus limit");
        Path output=Path.of(args[1]);Files.createDirectories(output);
        var provider=new OpenAiVisionProvider(send,true,System.getenv().getOrDefault("VISION_API_KEY",""),System.getenv().getOrDefault("VISION_MODEL",""),json);
        if(send && !provider.available())throw new IllegalArgumentException("Set VISION_API_KEY and VISION_MODEL before explicitly sending synthetic fixtures");
        List<Map<String,Object>> cases=new ArrayList<>();int calls=0;
        for(var fixture:manifest.cases()) {
            if(fixture.kind().equals("text"))continue;
            if(!fixture.id().matches("[a-z0-9-]{1,80}"))throw new IllegalArgumentException("Invalid case ID");
            var image=OcrEvaluation.render(fixture);Path png=output.resolve(fixture.id()+".png");
            try{ImageIO.write(image,"png",png.toFile());}finally{image.flush();}
            Path response=output.resolve(fixture.id()+".response.json");
            Map<String,Object> row=new LinkedHashMap<>();row.put("id",fixture.id());row.put("expected",fixture.expected().size());
            long start=System.nanoTime();
            try {
                // A successful recorded response is reused. Delete it deliberately to pay for a new trial.
                if(send && !Files.exists(response)){if(++calls>8)throw new IllegalArgumentException("Eight-call evaluation budget reached");Files.writeString(response,provider.read(Files.readAllBytes(png),"image/png"),StandardOpenOption.CREATE_NEW);}
                if(!Files.exists(response)){row.put("state","AWAITING_PROVIDER_RUN");cases.add(row);continue;}
                var original=new ExtractionData.Page(1,"",ExtractionData.Method.TESSERACT_OCR,0.0,List.of());
                var result=new VisionValidator().validate(Files.readString(response),original,provider.model());
                // Strict name/date matching, never nearest number or reference-range plausibility.
                Set<Integer> used=new HashSet<>();int values=0,units=0,dates=0,matched=0;
                for(var expected:fixture.expected()){
                    int best=-1;
                    for(int i=0;i<result.readings().size();i++)if(!used.contains(i) && expected.name().equals(result.readings().get(i).reading().testName())){
                        if(best<0)best=i;
                        if(Objects.equals(expected.date(),result.readings().get(i).reading().date())){best=i;break;}
                    }
                    if(best<0)continue;used.add(best);matched++;var actual=result.readings().get(best).reading();
                    if(Objects.equals(expected.value(),actual.value()))values++;
                    if(Objects.equals(expected.unit(),actual.unit()))units++;
                    if(Objects.equals(expected.date(),actual.date()))dates++;
                }
                row.put("state","SCORED_UNVERIFIED");row.put("matchedRows",matched);row.put("exactValues",values);row.put("exactUnits",units);row.put("exactDates",dates);row.put("missingRows",fixture.expected().size()-matched);row.put("extraRows",result.readings().size()-matched);
            }catch(ApiFailure e){row.put("state",e.code());}
            row.put("elapsedMillis",(System.nanoTime()-start)/1000000);cases.add(row);
        }
        var report=Map.of("policyVersion",VisionData.POLICY,"model",provider.model(),"corpusVersion",manifest.version(),"callsThisRun",calls,"note","Synthetic regression corpus only. Replay does not prove provider accuracy. No patient data. Reference ranges and clinical coverage are not scored.","cases",cases);
        json.writerWithDefaultPrettyPrinter().writeValue(output.resolve("vision-report.json").toFile(),report);
        System.out.println("Saved synthetic vision evaluation; provider calls this run: "+calls);
    }
}
