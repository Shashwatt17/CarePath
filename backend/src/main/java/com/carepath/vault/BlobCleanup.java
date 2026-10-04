package com.carepath.vault;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
/** Durable write intent / delete outbox. No medical contents or filename are retained here. */
@Service
public class BlobCleanup {
    private final JdbcTemplate jdbc; private final DocumentStorage storage; private final TransactionTemplate tx;
    public BlobCleanup(JdbcTemplate jdbc,DocumentStorage storage,org.springframework.transaction.PlatformTransactionManager manager) {
        this.jdbc=jdbc;this.storage=storage;this.tx=new TransactionTemplate(manager);
    }
    public void reserve(String key,boolean upload) {
        jdbc.update("INSERT INTO vault_blob_cleanup(storage_key,not_before) VALUES(?,?)",key,OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(upload?3600:0));
    }
    public void lock(String key) { jdbc.queryForObject("SELECT storage_key FROM vault_blob_cleanup WHERE storage_key=? FOR UPDATE",String.class,key); }
    public void finish(String key) { jdbc.update("DELETE FROM vault_blob_cleanup WHERE storage_key=?",key); }
    public boolean clean(String key) {
        try { return Boolean.TRUE.equals(tx.execute(status -> {
            var found=jdbc.queryForList("SELECT storage_key FROM vault_blob_cleanup WHERE storage_key=? FOR UPDATE",String.class,key);
            if(found.isEmpty()) return true;
            // Never remove a live original, even if an operator introduced a stale queue entry.
            if(jdbc.queryForObject("SELECT count(*) FROM medical_document WHERE storage_key=?",Integer.class,key)>0) return false;
            try { storage.delete(key); finish(key); return true; } catch(java.io.IOException e) { return false; }
        })); } catch(org.springframework.dao.DataAccessException e) { return false; }
    }
    @Scheduled(fixedDelayString="${carepath.vault.cleanup-delay-ms:60000}")
    public void retry() {
        try { for(String key:jdbc.queryForList("SELECT storage_key FROM vault_blob_cleanup WHERE not_before<=CURRENT_TIMESTAMP ORDER BY not_before LIMIT 100",String.class)) clean(key); }
        catch(org.springframework.dao.DataAccessException ignored) { /* Persistent rows remain for next run; no sensitive logging. */ }
    }
}
