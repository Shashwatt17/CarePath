package com.carepath.intelligence;
import static com.carepath.intelligence.ExtractionData.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.*;
public class LabObservationExtractor {
    private static final Set<String> UNITS=Set.of("g/dL","mg/dL","ng/mL","mmol/L","%","U/L","IU/L","mIU/L","10^9/L","10^12/L","fL","pg","mg/L","ug/L","µg/L","umol/L","µmol/L");
    private static final Pattern NUMBER=Pattern.compile("^([<>≤≥]=?)?([+-]?\\d{1,9}(?:\\.\\d{1,8})?)$");
    private static final Pattern LOOSE=Pattern.compile("^([\\p{L}][\\p{L} ()/._%-]{1,100}?)\\s+([<>≤≥]?[+-]?\\d+(?:\\.\\d+)?)\\s*(.*)$");
    private final ReferenceRangeParser ranges=new ReferenceRangeParser();private final ExtractionConfidence confidence=new ExtractionConfidence();
    public List<Candidate> extract(List<Page> pages,Information information) {
        List<Candidate> candidates=new ArrayList<>();
        for(Page page:pages) for(Line line:page.lines()) {
            Source source=TextPages.source(page,line);String raw=source.text().strip();if(raw.length()>1000) continue;
            boolean structured=raw.contains("|") || raw.contains("\t") || raw.matches(".*\\s{2,}.*");
            String[] cells=structured?raw.split(raw.contains("|")?"\\|":"\\s{2,}|\\t",-1):new String[0];
            if(!structured) {
                var match=LOOSE.matcher(raw);if(!match.matches()) continue;
                String tail=match.group(3).strip();String unit=null,reference=tail;
                for(String known:UNITS) if(tail.equals(known) || tail.startsWith(known+" ")) { unit=known;reference=tail.substring(known.length()).strip();break; }
                if(unit==null) continue; // Unstructured prose with a number is not a lab row.
                cells=new String[]{match.group(1),match.group(2),unit,reference};
            }
            if(cells.length<3 || cells.length>5) continue;
            String name=clean(cells[0]),value=clean(cells[1]);
            if(name==null || name.length()>160 || value!=null && value.length()>80 || name.contains(":") || !Character.isLetter(name.codePointAt(0)) || Set.of("test","test name","measurement","analyte").contains(name.toLowerCase(Locale.ROOT))) continue;
            // Avoid treating generic text instructions/headings as lab rows.
            if(!structured && (value==null || !value.matches(".*[0-9].*"))) continue;
            String unit=cells.length>2?clean(cells[2]):null,reference=cells.length>3?clean(cells[3]):null,flag=cells.length>4?flag(clean(cells[4])):null;
            if(unit!=null && unit.length()>50 || reference!=null && reference.length()>255) continue;
            var numeric=NUMBER.matcher(value==null?"":value);BigDecimal number=null;String comparator=null;
            if(numeric.matches()) { number=new BigDecimal(numeric.group(2));comparator=numeric.group(1); }
            var range=ranges.parse(reference);var score=confidence.score(number!=null,unit!=null && UNITS.contains(unit),range.text()!=null,range.valid(),structured,line,page.method());
            candidates.add(new Candidate(UUID.randomUUID(),name,value,number,comparator,unit,range.lower(),range.upper(),range.text(),flag,
                information.date()==null?null:LocalDate.parse(information.date().value()),information.provider()==null?null:information.provider().value(),score.band(),score.reasons(),source));
            if(candidates.size()>500) throw new ProcessingFailure("RESOURCE_LIMIT",false);
        }
        Map<String,Set<String>> values=new HashMap<>();for(var candidate:candidates) values.computeIfAbsent(candidate.originalTestName(),k->new HashSet<>()).add(String.valueOf(candidate.originalValue()));
        return candidates.stream().map(c -> {
            if(values.get(c.originalTestName()).size()<2) return c;
            var reasons=new ArrayList<>(c.reasons());reasons.add("CONFLICTING_VALUES");
            return new Candidate(c.id(),c.originalTestName(),c.originalValue(),c.numericValue(),c.comparator(),c.originalUnit(),c.referenceLower(),c.referenceUpper(),c.referenceText(),c.abnormalFlag(),c.reportDate(),c.providerName(),Band.LOW,reasons,c.source());
        }).toList();
    }
    private static String clean(String value) { return value==null || value.isBlank() || value.strip().equals("-")?null:value.strip(); }
    private static String flag(String value) { if(value==null) return null;return switch(value.toUpperCase(Locale.ROOT)) { case "HIGH","H" -> "HIGH";case "LOW","L" -> "LOW";case "ABNORMAL","A" -> "ABNORMAL";default -> null; }; }
}
