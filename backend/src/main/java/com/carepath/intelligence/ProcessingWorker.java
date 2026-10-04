package com.carepath.intelligence;
import com.carepath.vault.*;
import com.carepath.audit.AuditService;
import com.carepath.foundation.ApiFailure;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.*;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;
@Component
public class ProcessingWorker {
    private static final Logger LOG=LoggerFactory.getLogger(ProcessingWorker.class);
    private final com.carepath.care.FollowUpService followUps;
    private final AtomicBoolean busy=new AtomicBoolean();private final ProcessingJobs jobs;private final DocumentRepository documents;private final DocumentService vault;private final ExtractionEngine engine;private final ExtractionRepository extraction;private final ProcessingProperties properties;private final TransactionTemplate tx;private final AuditService audit;private final MeterRegistry metrics;
    public ProcessingWorker(ProcessingJobs jobs,DocumentRepository documents,DocumentService vault,ExtractionEngine engine,ExtractionRepository extraction,ProcessingProperties properties,org.springframework.transaction.PlatformTransactionManager manager,AuditService audit,MeterRegistry metrics,com.carepath.care.FollowUpService followUps) {
        this.followUps=followUps;
        this.jobs=jobs;this.documents=documents;this.vault=vault;this.engine=engine;this.extraction=extraction;this.properties=properties;tx=new TransactionTemplate(manager);this.audit=audit;this.metrics=metrics;
    }
    @Scheduled(fixedDelayString="${carepath.processing.poll-ms:1000}")
    public void poll() { if(properties.workerEnabled()) runOne(); }
    /** Also invoked by deterministic integration tests; uses exactly the production claim/execution path. */
    public boolean runOne() {
        if(!busy.compareAndSet(false,true)) return false;
        String previous=MDC.get("requestId");long start=System.nanoTime();String outcome="idle";
        try {
            var claim=tx.execute(s->jobs.claim(properties.timeoutSeconds()));if(claim==null || claim.isEmpty()) return false;
            var job=claim.get();if(job.requestId()!=null) MDC.put("requestId",job.requestId().toString());
            try {
                if(job.attempts()>job.maxAttempts()) throw new ProcessingFailure("WORKER_INTERRUPTED",true);
                var doc=documents.findByIdAndOwnerId(job.document(),job.owner());if(doc.isEmpty()) return true;
                byte[] bytes=vault.verifyIntegrity(doc.get());var result=engine.extract(bytes,doc.get().view().mimeType());
                boolean committed=Boolean.TRUE.equals(tx.execute(s->{
                    var owned=documents.lockByIdAndOwnerId(job.document(),job.owner());if(owned.isEmpty() || !jobs.current(job)) return false;
                    extraction.save(job.owner(),job.document(),job.id(),result);followUps.detect(job.owner(),job.document());jobs.success(job);
                    jobs.documentState(job.document(),job.owner(),result.needsReview() || !result.candidates().isEmpty()?"NEEDS_REVIEW":"COMPLETED",null);
                    audit.systemDocument(job.owner(),AuditService.Action.PROCESSING_SUCCEEDED,"SUCCESS",job.document());return true;
                }));
                if(!committed) { outcome="cancelled";return true; }
                outcome="success";metrics.counter("carepath.extraction.completed","category",result.classification().category()).increment();
                if(result.ocrUsed()) metrics.counter("carepath.extraction.ocr_fallback").increment();
            } catch(ProcessingFailure failure) { fail(job,failure);outcome="failure"; }
            catch(ApiFailure failure) { fail(job,new ProcessingFailure(failure.code().equals("INTEGRITY_FAILURE")?"INTEGRITY_FAILURE":"FILE_UNAVAILABLE",false));outcome="failure"; }
            catch(RuntimeException failure) { fail(job,new ProcessingFailure("PROCESSING_UNAVAILABLE",true));outcome="failure"; }
            LOG.info("Document processing attempt finished outcome={} attempt={}",outcome,job.attempts());return true;
        } catch(org.springframework.dao.DataAccessException failure) { metrics.counter("carepath.extraction.persistence_unavailable").increment();return false; }
        finally { if(!outcome.equals("idle")) metrics.timer("carepath.extraction.duration","outcome",outcome).record(Duration.ofNanos(System.nanoTime()-start));if(previous==null) MDC.remove("requestId");else MDC.put("requestId",previous);busy.set(false); }
    }
    private void fail(ProcessingJobs.Job job,ProcessingFailure failure) {
        tx.executeWithoutResult(s->{
            if(documents.lockByIdAndOwnerId(job.document(),job.owner()).isEmpty() || !jobs.current(job)) return;
            boolean retry=jobs.failure(job,failure);jobs.documentState(job.document(),job.owner(),retry?"PROCESSING":"FAILED",failure.code());
            audit.systemDocument(job.owner(),AuditService.Action.PROCESSING_FAILED,"FAILURE",job.document());
        });
        metrics.counter("carepath.extraction.failed","code",failure.code()).increment();
        if(job.attempts()>1) metrics.counter("carepath.extraction.retries").increment();
    }
}
