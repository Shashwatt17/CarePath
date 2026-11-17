package com.carepath.vision;
import static com.carepath.vision.VisionData.*;
import com.carepath.intelligence.*;
import com.carepath.vault.*;
import com.carepath.security.Ownership;
import com.carepath.foundation.ApiFailure;
import com.carepath.audit.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.concurrent.Semaphore;
@Service
public class VisionService {
    private final Ownership ownership;private final DocumentRepository documents;private final DocumentService vault;
    private final ExtractionRepository extraction;private final VisionProvider provider;private final JdbcTemplate jdbc;
    private final ObjectMapper json;private final TransactionTemplate tx;private final AuditService audit;
    private final Semaphore requests=new Semaphore(1);
    public VisionService(Ownership ownership,DocumentRepository documents,DocumentService vault,ExtractionRepository extraction,
        VisionProvider provider,JdbcTemplate jdbc,ObjectMapper json,org.springframework.transaction.PlatformTransactionManager manager,AuditService audit) {
        this.ownership=ownership;this.documents=documents;this.vault=vault;this.extraction=extraction;this.provider=provider;this.jdbc=jdbc;this.json=json;this.tx=new TransactionTemplate(manager);this.audit=audit;
    }
    private ExtractionData.View latest(UUID id) {
        ownership.require(id,documents::findByIdAndOwnerId);
        return extraction.latest(id,ownership.currentOwnerId()).orElseThrow(()->new ApiFailure(409,"EXTRACTION_NOT_READY","Process the document before requesting a second reading."));
    }
    public Status status(UUID id) {
        var view=latest(id);
        return new Status(provider.available(),"OpenAI",provider.model(),CONSENT,new VisionRouting().pages(view.result()));
    }
    public View get(UUID id,int page) {
        var view=latest(id);return saved(id,view.id(),ownership.currentOwnerId(),page).orElse(new View(view.id(),page,"NOT_REQUESTED",null));
    }
    private Optional<View> saved(UUID doc,UUID extractionId,UUID owner,int page) {
        return jdbc.query("SELECT state,result_json,created_at FROM vision_page_proposal WHERE document_id=? AND extraction_id=? AND owner_id=? AND page_number=?",(r,n)->{
            Result result=null;
            try {if(r.getString("result_json")!=null)result=json.readValue(r.getString("result_json"),Result.class);}catch(Exception e){throw VisionValidator.invalid();}
            String state=r.getString("state");
            if(state.equals("REQUESTED") && r.getTimestamp("created_at").toInstant().isBefore(java.time.Instant.now().minusSeconds(120)))state="FAILED";
            return new View(extractionId,page,state,result);
        },doc,extractionId,owner,page).stream().findFirst();
    }
    public View read(UUID id,Request request) {
        var view=latest(id);UUID owner=ownership.currentOwnerId();
        if(request==null || !view.id().equals(request.extractionId()))throw new ApiFailure(409,"EXTRACTION_CHANGED","Reload the report before requesting a second reading.");
        if(!request.consent() || !CONSENT.equals(request.consentVersion()))throw new ApiFailure(422,"VISION_CONSENT_REQUIRED","Consent is required before sending this page to OpenAI.");
        if(new VisionRouting().pages(view.result()).stream().noneMatch(p->p.page()==request.page()))throw new ApiFailure(422,"VISION_NOT_NEEDED","No fallback is offered for this page.");
        var cached=saved(id,view.id(),owner,request.page());if(cached.isPresent())return cached.get();
        if(!provider.available())throw new ApiFailure(503,"VISION_UNAVAILABLE","External second reading is not configured.");
        if(!requests.tryAcquire())throw new ApiFailure(429,"VISION_BUSY","A second reading is already in progress. Try shortly.");
        boolean reserved=false;
        try {
            Boolean claimed=tx.execute(s->{
                ownership.require(id,documents::lockByIdAndOwnerId);
                if(saved(id,view.id(),owner,request.page()).isPresent())return false;
                var day=java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(86400));
                if(jdbc.queryForObject("SELECT count(*) FROM vision_page_proposal WHERE document_id=? AND owner_id=?",Integer.class,id,owner)>=3 ||
                    jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE owner_id=? AND action='VISION_PAGE_REQUESTED' AND created_at>?",Integer.class,owner,day)>=10 ||
                    jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='VISION_PAGE_REQUESTED' AND created_at>?",Integer.class,day)>=100)
                    throw new ApiFailure(429,"VISION_LIMIT","Second-reading limit reached. Use the original report and manual review.");
                jdbc.update("INSERT INTO vision_page_proposal(id,owner_id,document_id,extraction_id,page_number,state,provider,model,consent_version,policy_version) VALUES(?,?,?,?,?,'REQUESTED','OPENAI',?,?,?)",UUID.randomUUID(),owner,id,view.id(),request.page(),provider.model(),CONSENT,POLICY);
                audit.document(owner,AuditService.Action.VISION_PAGE_REQUESTED,"SUCCESS",id);return true;
            });
            if(!Boolean.TRUE.equals(claimed))return get(id,request.page());reserved=true;
            // Existing preview path is owner-scoped, integrity-checked and bounds PDF rendering.
            var image=vault.content(id,true,request.page());
            if(image.bytes().length>6000000)throw new ApiFailure(422,"VISION_IMAGE_LIMIT","Upload a smaller, clear page image for the second reading.");
            var page=view.result().pages().stream().filter(p->p.number()==request.page()).findFirst().orElseThrow();
            Result result=new VisionValidator().validate(provider.read(image.bytes(),image.mime()),page,provider.model());
            String stored=json.writeValueAsString(result);
            tx.executeWithoutResult(s->{
                ownership.require(id,documents::lockByIdAndOwnerId);
                jdbc.update("UPDATE vision_page_proposal SET state='SUCCEEDED',result_json=? WHERE document_id=? AND extraction_id=? AND owner_id=? AND page_number=? AND state='REQUESTED'",stored,id,view.id(),owner,request.page());
                audit.document(owner,AuditService.Action.VISION_PAGE_COMPLETED,"SUCCESS",id);
            });
            return new View(view.id(),request.page(),"SUCCEEDED",result);
        }catch(Exception e){
            if(reserved)tx.executeWithoutResult(s->{jdbc.update("UPDATE vision_page_proposal SET state='FAILED' WHERE document_id=? AND extraction_id=? AND owner_id=? AND page_number=? AND state='REQUESTED'",id,view.id(),owner,request.page());audit.document(owner,AuditService.Action.VISION_PAGE_FAILED,"FAILURE",id);});
            if(e instanceof ApiFailure failure)throw failure;
            throw new ApiFailure(503,"VISION_UNAVAILABLE","Second reading failed. Original results are unchanged.");
        }finally{requests.release();}
    }
}
