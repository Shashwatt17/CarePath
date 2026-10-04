package com.carepath.intelligence;
import static com.carepath.intelligence.ExtractionData.*;
import java.util.*;
/** Validate the entire worker result before any candidate is persisted. */
public class ProvenanceValidator {
    public void validate(Result result,int maxPages,int maxCharacters) {
        if(result==null || result.pages()==null || result.pages().isEmpty() || result.pages().size()>maxPages || result.candidates()==null || result.classification()==null || result.information()==null || result.candidates().size()>500) fail();
        Map<Integer,Page> pages=new HashMap<>();int total=0;
        for(Page page:result.pages()) {
            if(page.number()!=pages.size()+1 || page.text()==null || page.text().contains("\u0000") || page.text().length()>50000 || page.lines()==null || page.method()==null) fail();
            total+=page.text().length();if(total>maxCharacters) fail();pages.put(page.number(),page);
            for(Line line:page.lines()) { if(line.start()<0 || line.end()<line.start() || line.end()>page.text().length()) fail();validateSource(TextPages.source(page,line),pages); }
        }
        for(Candidate candidate:result.candidates()) {
            validateSource(candidate.source(),pages);
            String evidence=candidate.source().text();
            if(!evidence.contains(candidate.originalTestName()) || candidate.originalValue()!=null && !evidence.contains(candidate.originalValue()) || candidate.originalUnit()!=null && !evidence.contains(candidate.originalUnit()) || candidate.referenceText()!=null && !evidence.contains(candidate.referenceText())) fail();
            if(candidate.reportDate()!=null && (result.information().date()==null || !candidate.reportDate().toString().equals(result.information().date().value())) || candidate.providerName()!=null && (result.information().provider()==null || !candidate.providerName().equals(result.information().provider().value()))) fail();
        }
        for(Source evidence:result.classification().evidence()) validateSource(evidence,pages);
        if(result.information().date()!=null) validateSource(result.information().date().source(),pages);
        if(result.information().provider()!=null) validateSource(result.information().provider().source(),pages);
    }
    private void validateSource(Source source,Map<Integer,Page> pages) {
        Page page=pages.get(source.page());
        if(page==null || source.start()<0 || source.end()<source.start() || source.end()>page.text().length() || source.method()!=page.method() || !page.text().substring(source.start(),source.end()).equals(source.text())) fail();
        if(source.box()!=null) {
            var box=source.box();if(!Double.isFinite(box.x()+box.y()+box.width()+box.height()) || box.x()<0 || box.y()<0 || box.width()<0 || box.height()<0 || box.x()+box.width()>1.000001 || box.y()+box.height()>1.000001) fail();
        }
    }
    private static void fail() { throw new ProcessingFailure("INVALID_PROVENANCE",false); }
}
