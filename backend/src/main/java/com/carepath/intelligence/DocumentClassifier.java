package com.carepath.intelligence;
import static com.carepath.intelligence.ExtractionData.*;
import java.util.*;
public class DocumentClassifier {
    private static final Map<String,String> HEADINGS=Map.ofEntries(
        Map.entry("LAB REPORT","LAB_REPORT"),Map.entry("LABORATORY REPORT","LAB_REPORT"),Map.entry("PRESCRIPTION","PRESCRIPTION"),
        Map.entry("DIAGNOSTIC REPORT","DIAGNOSTIC_REPORT"),Map.entry("DISCHARGE SUMMARY","DISCHARGE_SUMMARY"),
        Map.entry("VACCINATION RECORD","VACCINATION_RECORD"),Map.entry("DOCTOR NOTE","DOCTOR_NOTE"),
        Map.entry("REFERRAL","REFERRAL"),Map.entry("MEDICAL BILL","MEDICAL_BILL"));
    public Classification classify(List<Page> pages) {
        Map<String,List<Source>> found=new LinkedHashMap<>();
        for(Page page:pages) for(Line line:page.lines()) {
            Source source=TextPages.source(page,line);String category=HEADINGS.get(source.text().strip().toUpperCase(Locale.ROOT));
            if(category!=null) found.computeIfAbsent(category,k->new ArrayList<>()).add(source);
        }
        if(found.size()!=1) return new Classification("OTHER",Band.LOW,"EXACT_HEADING_V1",found.values().stream().flatMap(List::stream).limit(10).toList());
        var entry=found.entrySet().iterator().next();boolean nativeHeading=entry.getValue().stream().allMatch(s->s.method()==Method.PDFBOX_TEXT);
        return new Classification(entry.getKey(),nativeHeading?Band.HIGH:Band.MEDIUM,"EXACT_HEADING_V1",entry.getValue());
    }
}
