package com.carepath.review;
import static com.carepath.review.ReviewDtos.*;
import com.carepath.security.Ownership;
import com.carepath.vault.DocumentRepository;
import com.carepath.foundation.ApiFailure;
import com.carepath.audit.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
@Service
public class ReviewService {
    private final Ownership ownership;private final ReviewRepository repository;private final ReviewPolicy policy;private final DocumentRepository documents;private final AuditService audit;private final Clock clock;private final ObjectMapper mapper;private final TransactionTemplate tx;
    public ReviewService(Ownership ownership,ReviewRepository repository,ReviewPolicy policy,DocumentRepository documents,AuditService audit,Clock clock,ObjectMapper mapper,org.springframework.transaction.PlatformTransactionManager manager) {
        this.ownership=ownership;this.repository=repository;this.policy=policy;this.documents=documents;this.audit=audit;this.clock=clock;this.mapper=mapper;tx=new TransactionTemplate(manager);
    }
    private CandidateView view(ReviewRepository.Entry e) { return new CandidateView(e.candidate().id(),e.document(),e.filename(),e.state(),e.version(),e.candidate(),policy.decision(e.candidate()),e.observation()); }
    public CandidateView get(UUID id) { return view(ownership.require(id,repository::find)); }
    public ReviewDtos.Queue list(UUID document,String state,int page,int size) {
        if(document!=null) ownership.require(document,documents::findByIdAndOwnerId);
        if(state!=null && !Set.of("PENDING_REVIEW","CONFIRMED","CORRECTED_AND_CONFIRMED","REJECTED").contains(state)) throw new ApiFailure(400,"VALIDATION_FAILED","Choose a valid review state.");
        UUID owner=ownership.currentOwnerId();return new ReviewDtos.Queue(repository.list(owner,document,state,page,size).stream().map(this::view).toList(),repository.count(owner,document,state),page,size);
    }
    public Observation observation(UUID id) { return ownership.require(id,repository::observation); }
    public Preview preview(UUID id,Fields fields) { ownership.require(id,repository::find);return policy.preview(fields,true); }
    public ActionResult act(UUID id,String action,long version,Fields corrected,String reason,Object payload) {
        return tx.execute(s->{
            var initial=ownership.require(id,repository::find);
            ownership.require(initial.document(),documents::lockByIdAndOwnerId);
            repository.lock(id,initial.owner());var entry=ownership.require(id,repository::find);
            String hash=hash(payload);
            if(!entry.state().equals("PENDING_REVIEW")) {
                if(repository.previousHash(entry,action).filter(hash::equals).isPresent()) return result(entry);
                throw new ApiFailure(409,"REVIEW_STATE_CONFLICT","This candidate has already been resolved. Reload the review.");
            }
            if(entry.version()!=version) throw new ApiFailure(409,"VERSION_CONFLICT","This candidate changed. Reload before reviewing.");
            repository.validateEvidence(entry);UUID observation=null;String changed="";
            if(!action.equals("REJECT")) {
                Fields fields=action.equals("CONFIRM")?policy.original(entry.candidate()):corrected;
                if(action.equals("CONFIRM") && !policy.decision(entry.candidate()).canConfirm()) throw new ApiFailure(422,"CORRECTION_REQUIRED","Inspect the source and use Correct and verify for this result.");
                Preview normalized=policy.preview(fields,action.equals("CORRECT"));
                if(!normalized.canVerify()) throw ReviewPolicy.invalid();
                observation=repository.insertObservation(entry,fields,normalized,action,clock.instant());
                if(action.equals("CORRECT")) changed=changedFields(policy.original(entry.candidate()),fields);
            }
            repository.resolve(entry,switch(action) {case "CONFIRM"->"CONFIRMED";case "CORRECT"->"CORRECTED_AND_CONFIRMED";default->"REJECTED";});
            repository.history(entry,observation,action,reason,changed,hash,clock.instant());repository.updateDocument(entry);
            audit.document(entry.owner(),switch(action){case "CONFIRM"->AuditService.Action.EXTRACTION_CONFIRMED;case "CORRECT"->AuditService.Action.EXTRACTION_CORRECTED;default->AuditService.Action.EXTRACTION_REJECTED;},"SUCCESS",entry.document());
            return result(ownership.require(id,repository::find));
        });
    }
    private ActionResult result(ReviewRepository.Entry e) {return new ActionResult(view(e),e.observation()==null?null:repository.observation(e.observation(),e.owner()).orElseThrow());}
    private String hash(Object payload) {try{return com.carepath.vault.DocumentService.hash(mapper.writeValueAsBytes(payload));}catch(Exception e){throw ReviewPolicy.invalid();}}
    private String changedFields(Fields a,Fields b) {
        List<String> names=new ArrayList<>();
        if(!Objects.equals(a.testName(),b.testName())) names.add("testName");if(!Objects.equals(a.value(),b.value())) names.add("value");
        if(!Objects.equals(a.unit(),b.unit())) names.add("unit");if(!Objects.equals(a.referenceRange(),b.referenceRange())) names.add("referenceRange");
        if(!Objects.equals(a.date(),b.date())) names.add("date");if(b.conceptId()!=null) names.add("concept");
        return String.join(",",names);
    }
}
