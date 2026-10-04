package com.carepath.intelligence;
import static com.carepath.intelligence.ExtractionData.*;
import com.carepath.vault.*;
import com.carepath.security.Ownership;
import com.carepath.foundation.ApiFailure;
import com.carepath.audit.AuditService;
import java.util.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
@Service
public class ProcessingService {
    private final Ownership ownership;private final DocumentRepository documents;private final ProcessingJobs jobs;private final ExtractionRepository extraction;private final AuditService audit;private final TransactionTemplate tx;
    public ProcessingService(Ownership ownership,DocumentRepository documents,ProcessingJobs jobs,ExtractionRepository extraction,AuditService audit,org.springframework.transaction.PlatformTransactionManager manager) {
        this.ownership=ownership;this.documents=documents;this.jobs=jobs;this.extraction=extraction;this.audit=audit;this.tx=new TransactionTemplate(manager);
    }
    public Status request(UUID id,boolean retry) {
        return tx.execute(s->{
            var doc=ownership.require(id,documents::lockByIdAndOwnerId);var latest=jobs.latest(id,doc.ownerId());
            if(doc.view().status()!=DocumentDtos.State.UPLOADED) {
                if(!retry) return status(id);
                if(doc.view().status()!=DocumentDtos.State.FAILED || latest.isEmpty() || !latest.get().retryable()) throw new ApiFailure(409,"RETRY_NOT_ALLOWED","This document cannot be retried. Check the original and upload a new copy if needed.");
            } else if(retry) throw new ApiFailure(409,"RETRY_NOT_ALLOWED","Start processing this document first.");
            if(jobs.count(id,doc.ownerId())>=3 || jobs.active(doc.ownerId())>=5) throw new ApiFailure(429,"PROCESSING_LIMIT","Processing limit reached. Wait for current jobs or upload a corrected document.");
            String correlation=MDC.get("requestId");jobs.enqueue(id,doc.ownerId(),correlation==null?UUID.randomUUID():UUID.fromString(correlation));
            jobs.documentState(id,doc.ownerId(),"PROCESSING",null);audit.document(doc.ownerId(),AuditService.Action.PROCESSING_REQUESTED,"SUCCESS",id);return status(id);
        });
    }
    public Status status(UUID id) {
        var doc=ownership.require(id,documents::findByIdAndOwnerId);var job=jobs.latest(id,doc.ownerId());
        return new Status(doc.view().status().name(),job.map(ProcessingJobs.Job::state).orElse(null),job.map(ProcessingJobs.Job::attempts).orElse(0),job.map(ProcessingJobs.Job::error).orElse(null),doc.view().status()==DocumentDtos.State.FAILED && job.map(ProcessingJobs.Job::retryable).orElse(false) && jobs.count(id,doc.ownerId())<3);
    }
    public View extraction(UUID id) {
        UUID owner=ownership.currentOwnerId();ownership.require(id,documents::findByIdAndOwnerId);
        View result=extraction.latest(id,owner).orElseThrow(()->new ApiFailure(404,"EXTRACTION_NOT_READY","No completed extraction is available yet."));
        audit.document(owner,AuditService.Action.EXTRACTION_VIEWED,"SUCCESS",id);return result;
    }
    public Source evidence(UUID id,UUID candidate) {
        ownership.require(id,documents::findByIdAndOwnerId);return extraction.evidence(id,ownership.currentOwnerId(),candidate).orElseThrow(()->new ApiFailure(404,"NOT_FOUND","The requested resource was not found."));
    }
}
