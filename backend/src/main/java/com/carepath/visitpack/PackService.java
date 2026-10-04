package com.carepath.visitpack;

import static com.carepath.visitpack.PackDtos.*;
import static com.carepath.audit.AuditService.Action.*;
import java.time.*;
import java.util.*;
import java.security.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.carepath.security.Ownership;
import com.carepath.audit.AuditService;
import com.carepath.foundation.ApiFailure;
import com.carepath.longitudinal.HistoryDtos.Page;

@Service
@Transactional(isolation=Isolation.REPEATABLE_READ)
public class PackService {
    private final PackRepository repository;private final PackAssembler assembler;private final Ownership ownership;
    private final AuditService audit;private final Clock clock;private final ObjectMapper json;private final PackPdf pdf;
    public PackService(PackRepository r,PackAssembler a,Ownership o,AuditService log,Clock c,ObjectMapper j,PackPdf pdf) {
        repository=r;assembler=a;ownership=o;audit=log;clock=c;json=j;this.pdf=pdf;
    }
    private UUID owner(){return ownership.currentOwnerId();}
    private static ApiFailure conflict(){return new ApiFailure(409,"PACK_STATE_CONFLICT","The pack or its sources changed. Reload and preview again.");}
    public Pack get(UUID id){return repository.get(id,owner());}
    public Page<Summary> list(int page){return repository.list(owner(),page);}
    public Pack create(Input x){return create(x,1,null);}
    private Pack create(Input x,int revision,UUID previous) {
        validate(x);UUID id=UUID.randomUUID();Instant at=clock.instant();
        assembler.assemble(new Pack(id,x.title(),x.reasonForVisit(),x.appointmentId(),x.packDate(),"DRAFT",revision,0,at,at,null,previous,x.items(),null),x.appointmentId()!=null);
        repository.insert(id,owner(),x,revision,previous);audit.care(owner(),VISIT_PACK_CREATED,"VISIT_PACK",id,false);return get(id);
    }
    public Pack edit(UUID id,Input x) {
        repository.lock(id,owner());Pack p=get(id);draft(p,x.version());validate(x);
        assembler.assemble(new Pack(id,x.title(),x.reasonForVisit(),x.appointmentId(),x.packDate(),"DRAFT",p.revision(),p.version(),p.createdAt(),p.updatedAt(),null,p.previousPackId(),x.items(),null),x.appointmentId()!=null);
        repository.update(id,owner(),x);audit.care(owner(),VISIT_PACK_UPDATED,"VISIT_PACK",id,false);return get(id);
    }
    private void validate(Input x) {
        if(x.items().size()>40||x.packDate()!=null&&(x.packDate().getYear()<1900||x.packDate().getYear()>2200))throw PackAssembler.invalid();
        Set<String> seen=new HashSet<>();
        for(int i=0;i<x.items().size();i++) {var s=x.items().get(i);PackAssembler.validate(s);String key=s.type()+":"+s.sourceId()+":"+s.otherId()+":"+(s.type()==Type.MANUAL_QUESTION?s.questionText():"");if(!seen.add(key))throw PackAssembler.invalid();}
    }
    private void draft(Pack p,long version){if(!p.status().equals("DRAFT")||p.version()!=version)throw conflict();}
    private String hash(Content content) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(content)));}
        catch(Exception e){throw new IllegalStateException("Pack fingerprint unavailable");}
    }
    public Preview preview(UUID id) {
        Pack p=get(id);Content c=p.snapshot()==null?assembler.assemble(p,repository.appointmentSelected(id,owner())):p.snapshot();return new Preview(c,hash(c));
    }
    public Pack generate(UUID id,Generate x) {
        repository.lock(id,owner());Pack p=get(id);draft(p,x.version());
        Content c=assembler.assemble(p,repository.appointmentSelected(id,owner()));
        if(!MessageDigest.isEqual(hash(c).getBytes(java.nio.charset.StandardCharsets.US_ASCII),x.previewHash().getBytes(java.nio.charset.StandardCharsets.US_ASCII)))throw conflict();
        Instant at=clock.instant();pdf.render(c,at); // Validate renderability before committing the immutable snapshot.
        repository.freeze(id,owner(),c,at);audit.care(owner(),VISIT_PACK_GENERATED,"VISIT_PACK",id,false);return get(id);
    }
    public Pack revise(UUID id) {
        repository.lock(id,owner());Pack p=get(id);if(!p.status().equals("GENERATED"))throw conflict();
        UUID existing=repository.child(id,owner());if(existing!=null)return get(existing);
        return create(new Input(p.title(),p.reasonForVisit(),p.appointmentId(),p.packDate(),p.selections(),0),p.revision()+1,id);
    }
    public byte[] download(UUID id) {
        Pack p=get(id);if(!p.status().equals("GENERATED"))throw conflict();
        byte[] bytes=pdf.render(p.snapshot(),p.generatedAt());audit.care(owner(),VISIT_PACK_DOWNLOADED,"VISIT_PACK",id,false);return bytes;
    }
    public void delete(UUID id){repository.lock(id,owner());repository.delete(id,owner());audit.care(owner(),VISIT_PACK_DELETED,"VISIT_PACK",id,false);}
}
