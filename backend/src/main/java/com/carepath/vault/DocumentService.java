package com.carepath.vault;
import static com.carepath.vault.DocumentDtos.*;
import com.carepath.audit.AuditService;
import static com.carepath.audit.AuditService.Action.*;
import com.carepath.security.Ownership;
import com.carepath.foundation.ApiFailure;
import java.util.*;
import java.security.*;
import java.io.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
@Service
public class DocumentService {
    private final Ownership ownership; private final DocumentRepository repository; private final DocumentStorage storage;
    private final FileValidation validation; private final BlobCleanup cleanup; private final AuditService audit; private final TransactionTemplate tx;
    // Bounds concurrent parser/render allocations in the modular monolith. No medical extraction occurs.
    private final java.util.concurrent.Semaphore parsers=new java.util.concurrent.Semaphore(2);
    public DocumentService(Ownership ownership,DocumentRepository repository,DocumentStorage storage,FileValidation validation,
        BlobCleanup cleanup,AuditService audit,org.springframework.transaction.PlatformTransactionManager manager) {
        this.ownership=ownership;this.repository=repository;this.storage=storage;this.validation=validation;this.cleanup=cleanup;this.audit=audit;tx=new TransactionTemplate(manager);
    }
    public static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); } catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private void acquire() { if(!parsers.tryAcquire()) throw new ApiFailure(429,"VAULT_BUSY","The vault is busy. Please retry shortly."); }
    public View upload(MultipartFile file,Metadata data) {
        UUID owner=ownership.currentOwnerId(); FileValidation.Validated valid;
        acquire();try { valid=validation.validate(file); } finally { parsers.release(); }
        String key=UUID.randomUUID().toString().replace("-","");UUID id=UUID.randomUUID();
        // Commit intent before touching disk; rollback/crash leaves a durable cleanup record.
        tx.executeWithoutResult(s -> cleanup.reserve(key,true));
        try {
            return tx.execute(s -> {
                cleanup.lock(key);
                try { storage.store(key,valid.bytes()); } catch(IOException e) { throw unavailable(); }
                repository.insert(id,owner,key,valid,hash(valid.bytes()),data);cleanup.finish(key);
                audit.document(owner,DOCUMENT_UPLOADED,"SUCCESS",id);
                return repository.findByIdAndOwnerId(id,owner).orElseThrow().view();
            });
        } catch(RuntimeException e) { cleanup.clean(key);throw e; }
    }
    public View metadata(UUID id) {
        var document=owned(id);audit.document(document.ownerId(),DOCUMENT_VIEWED,"SUCCESS",id);return document.view();
    }
    public Page list(Filters f) {
        if(f.page()<0 || f.page()>100000 || f.size()<1 || f.size()>100 || (f.filename()!=null && f.filename().length()>255) ||
           (f.provider()!=null && f.provider().length()>255) || (f.from()!=null && f.to()!=null && f.from().isAfter(f.to())) ||
           (f.uploadedFrom()!=null && f.uploadedTo()!=null && f.uploadedFrom().isAfter(f.uploadedTo()))) throw new ApiFailure(400,"VALIDATION_FAILED","Check the search filters.");
        return repository.search(ownership.currentOwnerId(),f);
    }
    public View update(UUID id,Update update) {
        UUID owner=ownership.currentOwnerId();return tx.execute(s -> {
            owned(id);
            if(!repository.update(id,owner,update)) throw new ApiFailure(409,"VERSION_CONFLICT","This document changed. Reload it before saving.");
            audit.document(owner,DOCUMENT_METADATA_UPDATED,"SUCCESS",id);return repository.findByIdAndOwnerId(id,owner).orElseThrow().view();
        });
    }
    public boolean delete(UUID id) {
        String key=tx.execute(s -> {
            var d=ownership.require(id,repository::lockByIdAndOwnerId); cleanup.reserve(d.storageKey(),false);
            if(repository.delete(id,d.ownerId())!=1) throw new ApiFailure(409,"VERSION_CONFLICT","This document changed. Reload it.");
            audit.document(d.ownerId(),DOCUMENT_DELETED,"SUCCESS",id); return d.storageKey();
        });
        return cleanup.clean(key);
    }
    private MedicalDocument owned(UUID id) { return ownership.require(id,repository::findByIdAndOwnerId); }
    public record Content(byte[] bytes,String mime,String filename) {}
    /** Integrity is checked before any bytes can leave the service. */
    public byte[] verifyIntegrity(MedicalDocument d) {
        byte[] bytes;
        try { bytes=storage.retrieve(d.storageKey()); }
        catch(IOException e) { audit.document(d.ownerId(),DOCUMENT_INTEGRITY_FAILURE,"FAILURE",d.view().id());throw unavailable(); }
        if(bytes.length!=d.view().byteSize() || !MessageDigest.isEqual(hash(bytes).getBytes(java.nio.charset.StandardCharsets.US_ASCII),d.view().sha256().getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
            audit.document(d.ownerId(),DOCUMENT_INTEGRITY_FAILURE,"FAILURE",d.view().id());
            throw new ApiFailure(409,"INTEGRITY_FAILURE","The stored file could not be verified. Download is blocked.");
        }
        return bytes;
    }
    public Content content(UUID id,boolean preview,int page) {
        var d=owned(id);byte[] bytes=verifyIntegrity(d);
        if(preview) {
            if(page<1 || page>d.view().pageCount()) throw new ApiFailure(400,"VALIDATION_FAILED","Choose an available page.");
            if(d.view().mimeType().equals("application/pdf")) {
                acquire();try(var pdf=FileValidation.openPdf(bytes);var out=new ByteArrayOutputStream()) {
                    var box=pdf.getPage(page-1).getMediaBox();float scale=Math.min(1.5f,1600f/Math.max(box.getWidth(),box.getHeight()));
                    var image=new org.apache.pdfbox.rendering.PDFRenderer(pdf).renderImage(page-1,scale,org.apache.pdfbox.rendering.ImageType.RGB);
                    javax.imageio.ImageIO.write(image,"png",out);bytes=out.toByteArray();
                } catch(IOException e) { throw unavailable(); } finally { parsers.release(); }
            }
        }
        audit.document(d.ownerId(),preview?DOCUMENT_VIEWED:DOCUMENT_DOWNLOADED,"SUCCESS",id);
        return new Content(bytes,preview && d.view().mimeType().equals("application/pdf")?"image/png":d.view().mimeType(),d.view().originalFilename());
    }
    static ApiFailure unavailable() { return new ApiFailure(503,"FILE_UNAVAILABLE","The file is temporarily unavailable. Please retry."); }
}
