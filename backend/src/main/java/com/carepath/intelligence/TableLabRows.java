package com.carepath.intelligence;
import static com.carepath.intelligence.ExtractionData.*;
import java.util.*;
import java.math.BigDecimal;
/** Only explicit cell boundaries and matching date/value counts are associated. */
final class TableLabRows {
    private final MedicalDateParser dates=new MedicalDateParser();
    private List<String> headers=List.of();
    private Line header;
    boolean header(Page page,Line line) {
        String text=TextPages.source(page,line).text().strip();
        if(!text.matches("(?i)^Date(?:\\s|:|\\|).*$"))return false;
        headers=dates.tokens(text);header=line;return true;
    }
    List<Candidate> extract(Page page,Line line,Information info) {
        String raw=TextPages.source(page,line).text().strip();
        String[] cells=raw.split("\\t|\\|\\s*| {2,}",-1);
        if(!raw.contains("\t") && headers.size()<2)return null;
        if(cells.length<4 || cells.length>8)return null;
        for(int i=0;i<cells.length;i++)cells[i]=cells[i].strip();
        // Numeric cells precede the unit. Keep damaged reference text verbatim,
        // including its cell separators; never turn OCR punctuation into a range.
        int unitIndex=1;
        while(unitIndex<cells.length && cells[unitIndex].matches("[<>≤≥]?[+-]?\\d+(?:\\.\\d+)?"))unitIndex++;
        int count=unitIndex-1;
        if(count<1 || count>4 || unitIndex>=cells.length-1 || !cells[0].matches("[\\p{L}][\\p{L} ()/._%-]{1,159}"))return null;
        if(cells[unitIndex].isBlank() || cells[unitIndex].matches(".*\\d.*"))return null;
        String separator=raw.contains("\t")?"\t":raw.contains("|")?"|":"  ";
        String reference=String.join(separator,Arrays.copyOfRange(cells,unitIndex+1,cells.length));
        // Only keep a reference string that actually occurs in the source (no repaired spaces).
        if(!raw.contains(reference) || !reference.matches(".*[0-9].*") || !(reference.contains("-") || reference.contains("–") || reference.contains(".")))return null;
        var range=new ReferenceRangeParser().parse(reference);
        // No date header is needed for a single value. Multiple values are kept separate,
        // but their dates stay absent unless the header count matches exactly.
        boolean aligned=header!=null && headers.size()==count;
        List<Candidate> result=new ArrayList<>();
        for(int i=0;i<count;i++){
            var date=aligned?dates.parse(headers.get(i)):null;
            List<String> reasons=new ArrayList<>();reasons.add("TABLE_CELL_REVIEW_REQUIRED");
            if(!range.valid() || range.lower()==null || range.upper()==null)reasons.add("REFERENCE_RANGE_UNREADABLE");
            if(page.method()==Method.TESSERACT_OCR)reasons.add("OCR_REQUIRES_REVIEW");
            if(line.ocrConfidence()!=null && line.ocrConfidence()<.85)reasons.add("LOW_OCR_CONFIDENCE");
            if(count>1)reasons.add("MULTIPLE_DATED_COLUMNS");
            if(!aligned && (count>1 || header!=null))reasons.add("TABLE_DATE_UNRESOLVED");
            if(aligned && date==null)reasons.add("TABLE_DATE_UNRESOLVED");
            if(header==null && count==1 && info.date()!=null)date=dates.parse(info.date().value());
            Source source=TextPages.source(page,line);
            if(aligned){
                Box box=null;
                if(header.box()!=null && line.box()!=null){var a=header.box();var b=line.box();double x=Math.min(a.x(),b.x()),y=Math.min(a.y(),b.y());box=new Box(x,y,Math.max(a.x()+a.width(),b.x()+b.width())-x,Math.max(a.y()+a.height(),b.y()+b.height())-y);}
                source=new Source(page.number(),header.start(),line.end(),page.text().substring(header.start(),line.end()),page.method(),box);
            }
            String value=cells[i+1];String comparator=value.matches("^[<>≤≥].*")?value.substring(0,1):null;
            result.add(new Candidate(UUID.randomUUID(),cells[0],value,new BigDecimal(comparator==null?value:value.substring(1)),comparator,cells[unitIndex],range.lower(),range.upper(),range.text(),null,date,info.provider()==null?null:info.provider().value(),Band.LOW,reasons,source));
        }
        return result;
    }
}
