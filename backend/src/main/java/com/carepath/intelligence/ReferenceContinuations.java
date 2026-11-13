package com.carepath.intelligence;
import static com.carepath.intelligence.ExtractionData.*;
import java.util.*;
/** Preserve ordered reference fragments verbatim; do not interpret category thresholds. */
final class ReferenceContinuations {
    List<Candidate> extend(List<Page> pages,List<Candidate> candidates){
        List<Candidate> output=new ArrayList<>();
        for(Candidate c:candidates){
            Page page=pages.stream().filter(p->p.number()==c.source().page()).findFirst().orElseThrow();
            String reference=c.referenceText();int end=c.source().end();Box box=c.source().box();boolean changed=false;
            if(reference!=null){
                int examined=0;
                for(Line line:page.lines()){
                    if(line.start()<c.source().end())continue;
                    if(++examined>10)break;
                    // A new reading always terminates the previous reference block.
                    if(candidates.stream().anyMatch(n->n!=c && n.source().page()==page.number() && n.source().end()==line.end()))break;
                    String raw=page.text().substring(line.start(),line.end()).strip();
                    if(raw.isEmpty())break;
                    String[] cells=raw.split("\\t|\\|",-1);String fragment=cells[cells.length-1].strip();
                    boolean method=raw.matches("(?i)^(?:Method|Machine)\\s*:.*");
                    boolean category=fragment.matches("(?i)^(?:desirable|borderline|very high|high|low|optimal|near|moderate|elevated|line/).*\\d.*");
                    boolean wrapped=reference.stripTrailing().endsWith("-") && fragment.matches("\\d+(?:\\.\\d+)?");
                    if(!category && !wrapped){if(method)continue;break;}
                    if(reference.length()+fragment.length()+1>255)break;
                    reference+="\n"+fragment;end=line.end();changed=true;
                    if(box!=null && line.box()!=null){Box b=line.box();double x=Math.min(box.x(),b.x()),y=Math.min(box.y(),b.y());box=new Box(x,y,Math.max(box.x()+box.width(),b.x()+b.width())-x,Math.max(box.y()+box.height(),b.y()+b.height())-y);}
                }
            }
            if(!changed){output.add(c);continue;}
            List<String> reasons=new ArrayList<>(c.reasons());reasons.add("MULTILINE_REFERENCE_REVIEW_REQUIRED");
            Source source=new Source(page.number(),c.source().start(),end,page.text().substring(c.source().start(),end),page.method(),box);
            output.add(new Candidate(c.id(),c.originalTestName(),c.originalValue(),c.numericValue(),c.comparator(),c.originalUnit(),null,null,reference,c.abnormalFlag(),c.reportDate(),c.providerName(),Band.LOW,reasons,source));
        }
        return output;
    }
}
