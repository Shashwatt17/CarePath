package com.carepath.audit;

import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    public enum Action { USER_REGISTERED, LOGIN_SUCCEEDED, LOGIN_FAILED, LOGOUT, TOKEN_REFRESHED, TOKEN_REUSE_DETECTED, DOCUMENT_UPLOADED, DOCUMENT_VIEWED, DOCUMENT_DOWNLOADED, DOCUMENT_METADATA_UPDATED, DOCUMENT_DELETED, DOCUMENT_INTEGRITY_FAILURE, PROCESSING_REQUESTED, PROCESSING_SUCCEEDED, PROCESSING_FAILED, EXTRACTION_VIEWED, EXTRACTION_CONFIRMED, EXTRACTION_CORRECTED, EXTRACTION_REJECTED, AI_EXPLANATION_REQUESTED, AI_PROVIDER_FAILED, SAVED_QUESTION_CREATED, SAVED_QUESTION_UPDATED, SAVED_QUESTION_DELETED, SYMPTOM_CREATED, SYMPTOM_UPDATED, SYMPTOM_RESOLVED, SYMPTOM_DELETED, APPOINTMENT_CREATED, APPOINTMENT_UPDATED, APPOINTMENT_CANCELLED, APPOINTMENT_COMPLETED, APPOINTMENT_DELETED, FOLLOW_UP_CONFIRMED, FOLLOW_UP_EDITED, FOLLOW_UP_IGNORED, REMINDER_CREATED, REMINDER_TRIGGERED, VISIT_PACK_CREATED, VISIT_PACK_UPDATED, VISIT_PACK_GENERATED, VISIT_PACK_DOWNLOADED, VISIT_PACK_DELETED, SHARE_CREATED, SHARE_ACCESSED, SHARE_REVOKED }
    public enum Reason { INVALID_CREDENTIALS, REFRESH_REUSE, USER_REQUEST }
    private final JdbcTemplate jdbc;
    public AuditService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    // Joins the caller's transaction: success events cannot outlive a rolled-back operation.
    // Failure paths deliberately return outcomes so their audit/revocation transactions commit.
    public void record(UUID owner, Action action, String outcome, UUID session, Reason reason) {
        write(owner,action,outcome,"AUTH_SESSION",session,reason);
    }
    public void document(UUID owner,Action action,String outcome,UUID document) { write(owner,action,outcome,"MEDICAL_DOCUMENT",document,null); }
    public void systemDocument(UUID owner,Action action,String outcome,UUID document) { write(owner,action,outcome,"MEDICAL_DOCUMENT",document,null,true); }
    public void assistant(UUID owner,Action action,String outcome,UUID question) { write(owner,action,outcome,question==null?"ASSISTANT":"SAVED_QUESTION",question,null); }
    public void care(UUID owner,Action action,String resource,UUID id,boolean system) { write(owner,action,"SUCCESS",resource,id,null,system); }
    public void shareAccess(UUID owner, UUID id) {
        String request=MDC.get("requestId");
        jdbc.update("INSERT INTO audit_event(id,owner_id,action,resource_type,resource_id,request_id,outcome,actor_kind) VALUES(?,?,?,?,?,?,?,?)",
            UUID.randomUUID(),owner,Action.SHARE_ACCESSED.name(),"SHARE",id,request==null?null:UUID.fromString(request),"SUCCESS","SHARE");
    }
    private void write(UUID owner,Action action,String outcome,String resource,UUID session,Reason reason) { write(owner,action,outcome,resource,session,reason,false); }
    private void write(UUID owner,Action action,String outcome,String resource,UUID session,Reason reason,boolean system) {
        String request=MDC.get("requestId");
        jdbc.update("INSERT INTO audit_event(id,owner_id,action,resource_type,resource_id,request_id,outcome,actor_kind,reason_code) VALUES(?,?,?,?,?,?,?,?,?)",
            UUID.randomUUID(), owner, action.name(), resource, session,
            request == null ? null : UUID.fromString(request), outcome, system || owner == null ? "SYSTEM" : "OWNER", reason == null ? null : reason.name());
    }
}
