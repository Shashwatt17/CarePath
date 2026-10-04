package com.carepath.intelligence;
import static com.carepath.intelligence.ExtractionData.*;
import java.util.*;
import java.util.regex.*;
public class DocumentInformationExtractor {
    private final MedicalDateParser dates=new MedicalDateParser();
    public Information extract(List<Page> pages) {
        List<Fact> reports=new ArrayList<>(),samples=new ArrayList<>(),providers=new ArrayList<>();List<String> warnings=new ArrayList<>();
        Pattern pattern=Pattern.compile("(?i)^(Report date|Sample date|Laboratory|Lab|Provider):\\s*(.+)$");
        for(Page page:pages) for(Line line:page.lines()) {
            var source=TextPages.source(page,line);var match=pattern.matcher(source.text().strip());if(!match.matches()) continue;
            String label=match.group(1).toLowerCase(Locale.ROOT),value=match.group(2).strip();
            if(label.endsWith("date")) {
                var date=dates.parse(value);if(date==null) { warnings.add("UNPARSED_DATE");continue; }
                (label.startsWith("report")?reports:samples).add(new Fact(date.toString(),source));
            } else if(value.length()<=255) providers.add(new Fact(value,source));else warnings.add("PROVIDER_TOO_LONG");
        }
        return new Information(unique(reports.isEmpty()?samples:reports,"CONFLICTING_DATES",warnings),unique(providers,"CONFLICTING_PROVIDERS",warnings),warnings.stream().distinct().toList());
    }
    private Fact unique(List<Fact> facts,String warning,List<String> warnings) {
        if(facts.stream().map(Fact::value).distinct().count()>1) { warnings.add(warning);return null; }
        return facts.isEmpty()?null:facts.getFirst();
    }
}
