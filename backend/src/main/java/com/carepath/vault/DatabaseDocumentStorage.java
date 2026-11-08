package com.carepath.vault;

import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Durable originals for small deployments without a persistent application disk.
 * Ownership remains enforced by DocumentService; keys never appear as public URLs.
 * JdbcTemplate participates in the upload transaction, including rollback.
 */
@Component
@ConditionalOnProperty(name="carepath.vault.backend", havingValue="database")
public class DatabaseDocumentStorage implements DocumentStorage {
    private final JdbcTemplate jdbc;
    private final int maxBytes;
    public DatabaseDocumentStorage(JdbcTemplate jdbc, VaultProperties properties) {
        this.jdbc=jdbc; this.maxBytes=properties.maxBytes();
    }
    private void validateKey(String key) throws IOException {
        if(key==null || !key.matches("[a-f0-9]{32}")) throw new IOException("Invalid storage key");
    }
    public void store(String key, byte[] bytes) throws IOException {
        validateKey(key);
        if(bytes==null || bytes.length==0 || bytes.length>maxBytes) throw new IOException("Invalid storage size");
        try { jdbc.update("INSERT INTO document_blob(storage_key, contents) VALUES (?, ?)",key,bytes); }
        catch(DataAccessException e) { throw new IOException("Document storage write failed"); }
    }
    public byte[] retrieve(String key) throws IOException {
        validateKey(key);
        try {
            var rows=jdbc.query("SELECT contents FROM document_blob WHERE storage_key=? AND octet_length(contents)<=?",
                (rs,n)->rs.getBytes(1),key,maxBytes);
            if(rows.size()!=1 || rows.getFirst().length==0) throw new IOException("File unavailable");
            return rows.getFirst();
        } catch(DataAccessException e) { throw new IOException("Document storage read failed"); }
    }
    public void delete(String key) throws IOException {
        validateKey(key);
        try { jdbc.update("DELETE FROM document_blob WHERE storage_key=?",key); }
        catch(DataAccessException e) { throw new IOException("Document storage delete failed"); }
    }
    public boolean exists(String key) throws IOException {
        validateKey(key);
        try { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM document_blob WHERE storage_key=?)",Boolean.class,key)); }
        catch(DataAccessException e) { throw new IOException("Document storage check failed"); }
    }
}
