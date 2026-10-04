BEGIN;
DO $$
DECLARE a uuid:=gen_random_uuid(); b uuid:=gen_random_uuid(); d uuid:=gen_random_uuid(); o uuid:=gen_random_uuid(); q uuid:=gen_random_uuid(); e uuid:=gen_random_uuid();
BEGIN
 INSERT INTO app_user(id,email,password_hash,display_name) VALUES(a,'assistant-schema-a@example.invalid','not-real','SYNTHETIC'),(b,'assistant-schema-b@example.invalid','not-real','SYNTHETIC');
 INSERT INTO medical_document(id,owner_id,original_filename,mime_type,byte_size,storage_key,sha256) VALUES(d,a,'synthetic.pdf','application/pdf',100,repeat('f',32),repeat('0',64));
 INSERT INTO document_extraction(id,owner_id,document_id,revision,pipeline_version,extractor) VALUES(e,a,d,1,'synthetic','rules');
 INSERT INTO medical_observation(id,owner_id,document_id,extraction_id,original_test_name,original_value_text) VALUES(o,a,d,e,'Hb','12');
 INSERT INTO saved_question(id,owner_id,question_text,expected_evidence_count) VALUES(q,a,'Synthetic question',1);
 BEGIN INSERT INTO saved_question_evidence(question_id,owner_id,observation_id) VALUES(q,b,o); RAISE EXCEPTION 'foreign question evidence allowed'; EXCEPTION WHEN foreign_key_violation THEN NULL; END;
 BEGIN UPDATE saved_question SET expected_evidence_count=13 WHERE id=q; RAISE EXCEPTION 'evidence bound missing'; EXCEPTION WHEN check_violation THEN NULL; END;
 INSERT INTO saved_question_evidence(question_id,owner_id,observation_id) VALUES(q,a,o);
 BEGIN INSERT INTO saved_question_evidence(question_id,owner_id,observation_id) VALUES(q,a,o); RAISE EXCEPTION 'duplicate evidence allowed'; EXCEPTION WHEN unique_violation THEN NULL; END;
 DELETE FROM medical_document WHERE id=d;
 IF EXISTS(SELECT 1 FROM saved_question_evidence WHERE question_id=q) OR NOT EXISTS(SELECT 1 FROM saved_question WHERE id=q AND expected_evidence_count=1) THEN RAISE EXCEPTION 'source deletion must detach evidence and retain marked draft'; END IF;
END $$;
ROLLBACK;
