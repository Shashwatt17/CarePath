package com.carepath.visitpack;

import static com.carepath.visitpack.PackDtos.*;
import com.carepath.care.*;
import com.carepath.assistant.SavedQuestionService;
import com.carepath.longitudinal.*;
import com.carepath.longitudinal.HistoryDtos.Point;
import com.carepath.vault.DocumentRepository;
import com.carepath.security.Ownership;
import com.carepath.foundation.ApiFailure;
import java.util.*;
import org.springframework.stereotype.Component;

/** One deterministic assembly path for preview and generation. No model or original-file reads. */
@Component
public class PackAssembler {
    private final HistoryService history;
    private final SymptomService symptoms;
    private final AppointmentService appointments;
    private final FollowUpService followUps;
    private final SavedQuestionService questions;
    private final DocumentRepository documents;
    private final Ownership ownership;
    public PackAssembler(HistoryService h,SymptomService s,AppointmentService a,FollowUpService f,
                         SavedQuestionService q,DocumentRepository d,Ownership o) {
        history=h;symptoms=s;appointments=a;followUps=f;questions=q;documents=d;ownership=o;
    }
    public static ApiFailure invalid() { return new ApiFailure(422,"PACK_SELECTION_INVALID","Review selected records, field lengths and missing sources."); }
    public Content assemble(Pack p, boolean appointmentSelected) {
        List<Item> items=new ArrayList<>();
        if(appointmentSelected) {
            if(p.appointmentId()==null)throw invalid();
            items.add(appointment(p.appointmentId(),false,"Visit appointment"));
        }
        for(int i=0;i<p.selections().size();i++) items.add(item(p.selections().get(i)));
        int characters=p.title().length()+p.reasonForVisit().length();
        for(int i=0;i<items.size();i++) {
            Item item=items.get(i);
            for(int j=0;j<item.fields().size();j++) characters+=item.fields().get(j).value().length();
            for(int j=0;j<item.evidence().size();j++) characters+=item.evidence().get(j).snippet().length();
        }
        if(characters>80000)throw invalid();
        return new Content(p.title(),p.reasonForVisit(),p.packDate(),p.revision(),List.copyOf(items));
    }
    public Item item(Selection x) {
        validate(x);
        List<Field> fields=new ArrayList<>();List<Evidence> evidence=new ArrayList<>();String heading;
        switch(x.type()) {
            case SYMPTOM -> {
                var s=symptoms.get(x.sourceId());heading=s.name();
                field(fields,"Data type","User-reported symptom");field(fields,"Started (UTC)",s.startedAt());
                field(fields,"Resolved (UTC)",s.resolvedAt());field(fields,"Status",s.status());
                field(fields,"Severity (self-reported)",s.severity()+" / 10");field(fields,"Frequency",s.frequency());
                if(x.includeNotes())field(fields,"User notes",s.notes());
            }
            case OBSERVATION -> {
                var p=history.evidence(x.sourceId());heading=p.concept();
                field(fields,"Verified value",join(p.effective().text(),p.effective().unit()));
                field(fields,"Original source value",join(p.original().text(),p.original().unit()));
                if(p.normalizedValue()!=null)field(fields,"Normalized value",join(p.normalizedValue().toPlainString(),p.normalizedUnit()));
                field(fields,"Report date",p.date());field(fields,"Laboratory",p.provider());
                field(fields,"Supplied reference interval",p.effective().reference());
                field(fields,"Source abnormal flag",p.effective().sourceFlag());
                field(fields,"Verification",p.verificationStatus());field(fields,"Verified at (UTC)",p.verifiedAt());
                evidence.add(evidence(p));
            }
            case CHANGE -> {
                var c=history.compare(x.sourceId(),x.otherId());heading=c.concept();
                field(fields,"Observed change",c.type());field(fields,"Record comparison",c.explanation());
                field(fields,"Previous report date",c.previous().date());field(fields,"Current report date",c.current().date());
                field(fields,"Absolute delta",c.absoluteDelta());field(fields,"Percentage delta",c.percentageDelta()==null?null:c.percentageDelta()+"%");
                field(fields,"Supplied-interval comparison",c.referenceTransition());if(!"COMPARABLE_NUMERIC_ONLY".equals(c.reason()))field(fields,"Evidence limitation",c.reason());
                evidence.add(evidence(c.previous()));evidence.add(evidence(c.current()));
            }
            case DOCUMENT -> {
                var d=ownership.require(x.sourceId(),documents::findByIdAndOwnerId).view();heading=d.originalFilename();
                field(fields,"Document category",d.documentType());field(fields,"Document date",d.documentDate());
                field(fields,"Provider",d.providerName());field(fields,"Processing state",d.status());
                field(fields,"Reference only","Original file is not embedded. Inclusion does not verify its medical contents.");
                evidence.add(new Evidence(d.id(),null,d.originalFilename(),1,"Selected document reference"));
            }
            case APPOINTMENT -> { return appointment(x.sourceId(),x.includeNotes(),null); }
            case FOLLOW_UP -> {
                var f=followUps.get(x.sourceId());if(!Set.of("CONFIRMED","EDITED").contains(f.status()))throw invalid();
                heading="Confirmed follow-up";field(fields,"Source instruction",f.instruction());
                field(fields,"Machine-suggested date",f.suggestedDate());field(fields,"User-confirmed time",f.confirmedAt().atZone(java.time.ZoneId.of(f.timeZone())));
                field(fields,"Decision",f.status());evidence.add(new Evidence(f.documentId(),null,f.filename(),f.page(),f.instruction()));
            }
            case SAVED_QUESTION -> {
                var q=questions.get(x.sourceId());heading="Question for clinician";
                field(fields,"User draft",x.questionText()==null?q.text():x.questionText());
                if(q.evidenceMissing())field(fields,"Evidence notice","Some original question evidence is no longer available.");
                for(int i=0;i<q.evidence().size();i++)evidence.add(evidence(q.evidence().get(i)));
            }
            case MANUAL_QUESTION -> { heading="Question for clinician";field(fields,"Pack-only user draft",x.questionText()); }
            default -> throw invalid();
        }
        return new Item(x.type(),heading,List.copyOf(fields),List.copyOf(evidence));
    }
    private Item appointment(UUID id,boolean notes,String title) {
        var a=appointments.get(id);List<Field> fields=new ArrayList<>();
        field(fields,"Provider",a.providerName());field(fields,"Specialty",a.specialty());
        field(fields,"Appointment time",a.startsAt().atZone(java.time.ZoneId.of(a.timeZone())));
        field(fields,"Location",a.location());field(fields,"Status",a.status());if(notes)field(fields,"User notes",a.notes());
        return new Item(Type.APPOINTMENT,title==null?a.providerName():title,List.copyOf(fields),List.of());
    }
    private Evidence evidence(Point p) {
        var e=p.evidence();return new Evidence(e.documentId(),p.id(),e.filename(),e.page(),e.text());
    }
    public static void validate(Selection x) {
        if(x==null||x.type()==null)throw invalid();
        if(x.type()==Type.MANUAL_QUESTION) {
            if(x.sourceId()!=null||x.otherId()!=null||x.questionText()==null||x.questionText().isBlank())throw invalid();
        } else if(x.sourceId()==null)throw invalid();
        if(x.type()==Type.CHANGE) { if(x.otherId()==null||x.sourceId().equals(x.otherId()))throw invalid(); }
        else if(x.otherId()!=null)throw invalid();
        if(x.questionText()!=null&&(x.questionText().isBlank()||x.questionText().length()>1000||!Set.of(Type.SAVED_QUESTION,Type.MANUAL_QUESTION).contains(x.type())))throw invalid();
        if(x.includeNotes()&&!Set.of(Type.SYMPTOM,Type.APPOINTMENT).contains(x.type()))throw invalid();
    }
    private static String join(String value,String unit) {return Objects.toString(value,"")+(unit==null?"":" "+unit);}
    private static void field(List<Field> fields,String label,Object value) {
        if(value==null||value.toString().isBlank())return;
        String text=value.toString();if(text.length()>4000)throw invalid();fields.add(new Field(label,text));
    }
}
