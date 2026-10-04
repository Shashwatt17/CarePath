package com.carepath.intelligence;
import static com.carepath.intelligence.ExtractionData.*;
import java.util.*;
/** Rule grades, not estimated probabilities of medical correctness. */
public class ExtractionConfidence {
    public record Score(Band band,List<String> reasons) {}
    public Score score(boolean numeric,boolean knownUnit,boolean hasRange,boolean rangeValid,boolean structured,Line line,Method method) {
        List<String> reasons=new ArrayList<>();Band band=Band.HIGH;
        if(!knownUnit) { reasons.add("UNIT_MISSING_OR_UNRECOGNIZED");band=Band.MEDIUM; }
        if(!hasRange) { reasons.add("REFERENCE_MISSING");band=Band.MEDIUM; }
        if(!structured) { reasons.add("UNSTRUCTURED_ROW");band=Band.MEDIUM; }
        if(method==Method.TESSERACT_OCR) { reasons.add("OCR_REQUIRES_REVIEW");band=Band.MEDIUM; }
        if(!numeric || !rangeValid || (line.ocrConfidence()!=null && line.ocrConfidence()<0.85)) {
            band=Band.LOW;
            if(!numeric) reasons.add("VALUE_NOT_NUMERIC");if(!rangeValid) reasons.add("INVALID_REFERENCE_RANGE");
            if(line.ocrConfidence()!=null && line.ocrConfidence()<0.85) reasons.add("LOW_OCR_CONFIDENCE");
        }
        return new Score(band,reasons);
    }
}
