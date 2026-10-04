package com.carepath.longitudinal;
import java.nio.file.*;
import java.util.*;
import java.time.Instant;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LongitudinalEvaluationIT {
 @Test void measureComparabilityDirectionAndReferenceFixtures()throws Exception {
  var json=new ObjectMapper();var truth=json.readTree(Files.readString(Path.of("../evaluation/phase6/comparison-ground-truth.json")));var fixtures=new ComparisonTest();
  int total=0,directions=0,comparable=0,references=0;List<Map<String,Object>> details=new ArrayList<>();
  for(var c:truth.path("cases")){
   var a=fixtures.point(c.path("before").asText(),1);var b=fixtures.point(c.path("after").asText(),c.path("day").asInt(),c.path("unit").asText(),nullable(c,"low"),nullable(c,"high"),c.path("lab").asText(),ComparisonTest.CONCEPT,"CONFIRMED",null);
   var result=fixtures.engine.compare(a,b);total++;boolean d=result.type().equals(c.path("direction").asText()),match=(!result.type().equals("INSUFFICIENT_EVIDENCE"))==c.path("comparable").asBoolean(),r=result.referenceTransition().equals(c.path("reference").asText());
   if(d)directions++;if(match)comparable++;if(r)references++;details.add(Map.of("case",total,"directionCorrect",d,"comparabilityCorrect",match,"referenceCorrect",r));
  }
  var report=Map.of("label",truth.path("label").asText(),"scope",truth.path("scope").asText(),"executedAt",Instant.now().toString(),"cases",total,"directionCorrect",directions,"comparabilityCorrect",comparable,"referenceOutcomeCorrect",references,"details",details);
  Files.createDirectories(Path.of("../evaluation/results"));Files.writeString(Path.of("../evaluation/results/phase6-comparison.json"),json.writerWithDefaultPrettyPrinter().writeValueAsString(report)+"\n");assertEquals(total,directions);assertEquals(total,comparable);assertEquals(total,references);
 }
 static String nullable(JsonNode n,String key){return n.path(key).isNull()?null:n.path(key).asText();}
}
