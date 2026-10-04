BEGIN;
DO $$
DECLARE u uuid:=gen_random_uuid(); d uuid:=gen_random_uuid();
BEGIN
 INSERT INTO app_user(id,email,password_hash,display_name) VALUES(u,'vault-synthetic@example.invalid','not-a-real-password-hash','SYNTHETIC');
 INSERT INTO medical_document(id,owner_id,original_filename,mime_type,byte_size,storage_key,sha256) VALUES(d,u,'synthetic.pdf','application/pdf',100,repeat('a',32),repeat('0',64));
 BEGIN UPDATE medical_document SET page_count=201 WHERE id=d; RAISE EXCEPTION 'page limit not enforced'; EXCEPTION WHEN check_violation THEN NULL; END;
 BEGIN INSERT INTO vault_document_tag(document_id,tag) VALUES(gen_random_uuid(),'orphan'); RAISE EXCEPTION 'tag FK not enforced'; EXCEPTION WHEN foreign_key_violation THEN NULL; END;
 BEGIN INSERT INTO vault_document_tag(document_id,tag) VALUES(d,'  '); RAISE EXCEPTION 'blank tag accepted'; EXCEPTION WHEN check_violation THEN NULL; END;
 BEGIN INSERT INTO vault_blob_cleanup(storage_key,not_before) VALUES('../escape',now()); RAISE EXCEPTION 'unsafe cleanup key accepted'; EXCEPTION WHEN check_violation THEN NULL; END;
 INSERT INTO vault_document_tag(document_id,tag) VALUES(d,'synthetic');
 INSERT INTO vault_blob_cleanup(storage_key,not_before) VALUES(repeat('a',32),now());
 DELETE FROM medical_document WHERE id=d;
 IF EXISTS(SELECT 1 FROM vault_document_tag WHERE document_id=d) OR NOT EXISTS(SELECT 1 FROM vault_blob_cleanup WHERE storage_key=repeat('a',32)) THEN RAISE EXCEPTION 'deletion lifecycle incorrect'; END IF;
END $$;
ROLLBACK;
