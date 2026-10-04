package com.carepath.intelligence;
import java.util.*;
import java.time.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class ProcessingJobs {
    public record Job(UUID id,UUID owner,UUID document,String state,int attempts,int maxAttempts,String fence,UUID requestId,String error,boolean retryable) {}
    private final JdbcTemplate jdbc;private final Clock clock;
    public ProcessingJobs(JdbcTemplate jdbc,Clock clock) { this.jdbc=jdbc;this.clock=clock; }
    private Job row(java.sql.ResultSet r,int n) throws java.sql.SQLException {
        return new Job(r.getObject("id",UUID.class),r.getObject("owner_id",UUID.class),r.getObject("document_id",UUID.class),r.getString("status"),r.getInt("attempt_count"),r.getInt("max_attempts"),r.getString("locked_by"),r.getObject("request_id",UUID.class),r.getString("last_error_code"),r.getBoolean("error_retryable"));
    }
    public Optional<Job> latest(UUID document,UUID owner) { return jdbc.query("SELECT * FROM processing_job WHERE document_id=? AND owner_id=? AND job_type='EXTRACT_DOCUMENT' ORDER BY created_at DESC,id DESC LIMIT 1",this::row,document,owner).stream().findFirst(); }
    public int count(UUID document,UUID owner) { return jdbc.queryForObject("SELECT count(*) FROM processing_job WHERE document_id=? AND owner_id=? AND job_type='EXTRACT_DOCUMENT'",Integer.class,document,owner); }
    public int active(UUID owner) { return jdbc.queryForObject("SELECT count(*) FROM processing_job WHERE owner_id=? AND status IN ('QUEUED','RUNNING')",Integer.class,owner); }
    public void enqueue(UUID document,UUID owner,UUID request) {
        jdbc.update("INSERT INTO processing_job(id,document_id,owner_id,request_id,available_at) VALUES(?,?,?,?,?)",UUID.randomUUID(),document,owner,request,clock.instant().atOffset(ZoneOffset.UTC));
    }
    public Optional<Job> claim(int timeoutSeconds) {
        var now=clock.instant().atOffset(ZoneOffset.UTC);
        var jobs=jdbc.query("SELECT * FROM processing_job WHERE job_type='EXTRACT_DOCUMENT' AND ((status='QUEUED' AND available_at<=?) OR (status='RUNNING' AND locked_until<?)) ORDER BY available_at,id LIMIT 1 FOR UPDATE SKIP LOCKED",this::row,now,now);
        if(jobs.isEmpty()) return Optional.empty();Job j=jobs.getFirst();String fence=UUID.randomUUID().toString();
        jdbc.update("UPDATE processing_job SET status='RUNNING',attempt_count=attempt_count+1,locked_by=?,locked_until=?,updated_at=CURRENT_TIMESTAMP,version=version+1 WHERE id=?",fence,clock.instant().plusSeconds(timeoutSeconds+60L).atOffset(ZoneOffset.UTC),j.id());
        return Optional.of(new Job(j.id(),j.owner(),j.document(),"RUNNING",j.attempts()+1,j.maxAttempts(),fence,j.requestId(),null,false));
    }
    public boolean current(Job job) {
        return jdbc.query("SELECT * FROM processing_job WHERE id=? AND owner_id=? FOR UPDATE",this::row,job.id(),job.owner()).stream().anyMatch(j->j.state().equals("RUNNING") && Objects.equals(j.fence(),job.fence()));
    }
    public void success(Job job) { jdbc.update("UPDATE processing_job SET status='SUCCEEDED',locked_by=NULL,locked_until=NULL,finished_at=CURRENT_TIMESTAMP,last_error_code=NULL,error_retryable=false,version=version+1 WHERE id=? AND locked_by=?",job.id(),job.fence()); }
    public boolean failure(Job job,ProcessingFailure failure) {
        boolean retry=failure.retryable() && job.attempts()<job.maxAttempts();
        jdbc.update("UPDATE processing_job SET status=?,last_error_code=?,error_retryable=?,available_at=?,locked_by=NULL,locked_until=NULL,finished_at=?,version=version+1 WHERE id=? AND locked_by=?",retry?"QUEUED":"FAILED",failure.code(),failure.retryable(),clock.instant().plusSeconds(5L*(1L<<Math.min(job.attempts(),5))).atOffset(ZoneOffset.UTC),retry?null:clock.instant().atOffset(ZoneOffset.UTC),job.id(),job.fence());return retry;
    }
    public void documentState(UUID document,UUID owner,String state,String code) {
        jdbc.update("UPDATE medical_document SET status=?,error_code=?,processed_at=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=? AND owner_id=?",state,code,Set.of("COMPLETED","NEEDS_REVIEW").contains(state)?clock.instant().atOffset(ZoneOffset.UTC):null,document,owner);
    }
}
