package com.carepath.vision;
import com.carepath.intelligence.ExtractionData.Box;
import com.carepath.intelligence.ExtractionData.Page;
import com.carepath.foundation.ApiFailure;
import com.fasterxml.jackson.databind.*;
import java.time.LocalDate;
import java.util.*;
import static com.carepath.vision.VisionData.*;
/** Structural validation and reader disagreement checks. Never repairs a numeric value. */
public final class VisionValidator {
    private static final Set<String> UNITS=Set.of("mg/dL","mg/dl","g/dL","g/dl","mmol/L","mmol/l","µg/dL","ug/dL","ng/mL","ng/ml","pg/mL","pg/ml","%","U/L","IU/L","mIU/L","mL/min/1.73m2","10^3/µL","10^6/µL","fL","pg","ratio");
    public Result validate(String content,Page page,String model) {
        try {
            if(content==null || content.length()>65536)throw invalid();
            ObjectMapper mapper=new ObjectMapper().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
            JsonNode root=mapper.readTree(content);
            if(!root.isObject() || root.size()!=2 || !root.path("readings").isArray() || !root.path("unreadable").isBoolean() || root.path("readings").size()>50)throw invalid();
            for(JsonNode row:root.path("readings")) {
                if(!row.isObject() || row.size()!=7)throw invalid();
                for(String key:List.of("testName","value","unit","referenceRange","date","column")) {
                    JsonNode value=row.get(key);
                    if(value==null || (!value.isNull() && !value.isTextual()) || (value.isTextual() && (value.asText().length()>500 || value.asText().contains("\u0000"))))throw invalid();
                }
                if(!row.path("testName").isTextual() || row.path("testName").asText().isBlank() || row.path("testName").asText().length()>200)throw invalid();
                JsonNode b=row.get("box");
                if(b==null)throw invalid();
                if(!b.isNull()) {
                    if(!b.isObject() || b.size()!=4)throw invalid();
                    for(String key:List.of("x","y","width","height"))if(!b.path(key).isNumber())throw invalid();
                    double x=b.path("x").asDouble(),y=b.path("y").asDouble(),w=b.path("width").asDouble(),h=b.path("height").asDouble();
                    if(!Double.isFinite(x+y+w+h)||x<0||y<0||w<=0||h<=0||x+w>1||y+h>1)throw invalid();
                }
            }
            Output output=mapper.treeToValue(root,Output.class);
            List<Checked> checked=new ArrayList<>();Set<String> keys=new HashSet<>();
            for(Reading row:output.readings()) {
                List<String> warnings=new ArrayList<>(List.of("MODEL_OUTPUT_UNVERIFIED","SOURCE_REVIEW_REQUIRED"));
                if(row.value()==null || !row.value().matches("[<>≤≥]?\\s*-?\\d+(?:\\.\\d+)?"))warnings.add("NON_NUMERIC_OR_AMBIGUOUS_VALUE");
                if(row.unit()==null || !UNITS.contains(row.unit()))warnings.add("UNIT_UNSUPPORTED");
                if(row.date()==null)warnings.add("DATE_NOT_AVAILABLE");
                else try {LocalDate.parse(row.date());} catch(Exception e){warnings.add("INVALID_DATE");}
                if(row.column()==null || row.column().isBlank())warnings.add("COLUMN_UNSPECIFIED");
                if(row.box()==null)warnings.add("REGION_NOT_AVAILABLE");
                // Restrict comparison to overlapping OCR lines. A match is corroboration, not verification.
                String evidence=page.lines().stream().filter(l->overlap(l.box(),row.box())).map(l->page.text().substring(l.start(),l.end())).reduce("",(a,b)->a+"\n"+b);
                if(row.value()==null || !token(evidence,row.value()) || !evidence.toLowerCase(Locale.ROOT).contains(row.testName().toLowerCase(Locale.ROOT)) || row.unit()!=null && !token(evidence,row.unit()))warnings.add("OCR_DISAGREEMENT_OR_MISSING_EVIDENCE");
                if(!keys.add(row.testName()+"|"+row.date()+"|"+row.column()))warnings.add("DUPLICATE_COLUMN_READING");
                checked.add(new Checked(row,List.copyOf(warnings)));
            }
            return new Result(List.copyOf(checked),output.unreadable(),model,POLICY);
        } catch(ApiFailure e){throw e;}catch(Exception e){throw invalid();}
    }
    private boolean overlap(Box a,Box b){return a!=null && b!=null && a.x()<b.x()+b.width() && b.x()<a.x()+a.width() && a.y()<b.y()+b.height() && b.y()<a.y()+a.height();}
    private boolean token(String text,String value){return java.util.regex.Pattern.compile("(?<![\\p{L}\\p{N}.])"+java.util.regex.Pattern.quote(value)+"(?![\\p{L}\\p{N}.])").matcher(text).find();}
    public static ApiFailure invalid(){return new ApiFailure(502,"VISION_INVALID_OUTPUT","The second reading could not be validated. Your original results are unchanged.");}
}
