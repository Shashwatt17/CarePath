package com.carepath.review;
import com.carepath.identity.AuthTestSupport;
import com.carepath.terminology.*;
import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;
class NormalizationEvaluationIT extends AuthTestSupport {
    @Autowired Terminology terms;@Autowired UnitNormalizer units;@Autowired ObjectMapper json;
    @Test void measureAuthoredNormalizationCases() throws Exception {
        var truth=json.readTree(Files.readString(Path.of("../evaluation/phase5/ground-truth.json")));
        int mappingCorrect=0,conversionCorrect=0,abstentions=0,total=0;List<Map<String,Object>> rows=new ArrayList<>();
        for(var c:truth.path("cases")) {
            total++;String term=c.path("term").asText();var m=terms.normalize(term);var n=units.normalize(m.concept(),new BigDecimal(c.path("value").asText()),nullable(c,"unit"),null);
            boolean mapped=Objects.equals(nullable(c,"concept"),m.concept()==null?null:m.concept().name()) && c.path("mapping").asText().equals(m.status());
            boolean converted=Objects.equals(nullable(c,"normalized"),n.value()==null?null:n.value().toPlainString()) && Objects.equals(nullable(c,"normalizedUnit"),n.unit()) && c.path("status").asText().equals(n.status());
            if(mapped)mappingCorrect++;if(converted)conversionCorrect++;if(n.value()==null)abstentions++;
            rows.add(Map.of("term",term,"mappingCorrect",mapped,"conversionCorrect",converted,"actualStatus",n.status()));
        }
        var result=new LinkedHashMap<String,Object>();result.put("label",truth.path("label").asText());result.put("scope",truth.path("scope").asText());result.put("executedAt",Instant.now().toString());result.put("cases",total);result.put("conceptMappingCorrect",mappingCorrect);result.put("unitOutcomeCorrect",conversionCorrect);result.put("normalizationAbstentions",abstentions);result.put("rows",rows);
        Files.createDirectories(Path.of("../evaluation/results"));Files.writeString(Path.of("../evaluation/results/phase5-normalization.json"),json.writerWithDefaultPrettyPrinter().writeValueAsString(result)+"\n");
        assertEquals(total,mappingCorrect);assertEquals(total,conversionCorrect);
    }
    static String nullable(JsonNode c,String field) {return c.path(field).isNull()?null:c.path(field).asText();}
}
