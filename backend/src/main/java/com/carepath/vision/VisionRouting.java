package com.carepath.vision;
import com.carepath.intelligence.ExtractionData.*;
import java.util.*;
/** Cheap routing is a reason to offer a second read, never an accuracy guarantee. */
public final class VisionRouting {
    public List<VisionData.PageRoute> pages(Result result) {
        List<VisionData.PageRoute> routes=new ArrayList<>();
        for(Page page:result.pages()) {
            Set<String> reasons=new LinkedHashSet<>();
            var rows=result.candidates().stream().filter(c->c.source().page()==page.number()).toList();
            if(page.method()==Method.TESSERACT_OCR)reasons.add("SCANNED_PAGE");
            if(page.ocrConfidence()!=null && page.ocrConfidence()<.85)reasons.add("LOW_OCR_CONFIDENCE");
            if(rows.isEmpty())reasons.add("NO_SUPPORTED_READINGS");
            if(rows.stream().anyMatch(c->c.confidence()!=Band.HIGH))reasons.add("UNCERTAIN_READINGS");
            if(result.information().warnings().contains("MULTIPLE_DATED_COLUMNS"))reasons.add("HISTORICAL_COLUMNS");
            if(!reasons.isEmpty())routes.add(new VisionData.PageRoute(page.number(),List.copyOf(reasons)));
        }
        return List.copyOf(routes);
    }
}
