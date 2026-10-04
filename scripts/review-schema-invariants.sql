BEGIN;
DO $$
DECLARE a uuid:=gen_random_uuid(); b uuid:=gen_random_uuid(); d uuid:=gen_random_uuid(); e uuid:=gen_random_uuid(); p uuid:=gen_random_uuid(); c uuid:=gen_random_uuid(); o uuid:=gen_random_uuid(); v uuid:=gen_random_uuid();
BEGIN
 INSERT INTO app_user(id,email,password_hash,display_name) VALUES(a,'review-schema-a@example.invalid','not-real','SYNTHETIC'),(b,'review-schema-b@example.invalid','not-real','SYNTHETIC');
 INSERT INTO medical_document(id,owner_id,original_filename,mime_type,byte_size,storage_key,sha256) VALUES(d,a,'synthetic.pdf','application/pdf',100,repeat('e',32),repeat('0',64));
 INSERT INTO document_extraction(id,owner_id,document_id,revision,pipeline_version,extractor) VALUES(e,a,d,1,'synthetic','rules');
 INSERT INTO document_page(id,owner_id,document_id,extraction_id,page_number,extracted_text) VALUES(p,a,d,e,1,'Hb | 12 | g/dL');
 INSERT INTO extraction_candidate(id,owner_id,document_id,extraction_id,page_id,original_test_name,confidence_band,confidence_reasons,source_text,source_start,source_end) VALUES(c,a,d,e,p,'Hb','HIGH','','Hb | 12',0,7);
 INSERT INTO medical_observation(id,owner_id,document_id,extraction_id,source_candidate_id,original_test_name,original_value_text,verified_by,verified_at,verification_status) VALUES(o,a,d,e,c,'Hb','12',a,now(),'CONFIRMED');
 INSERT INTO observation_source(owner_id,observation_id,page_id,extraction_id,document_id,evidence_text) VALUES(a,o,p,e,d,'Hb | 12');
 IF EXISTS(SELECT 1 FROM trusted_medical_observation WHERE id=o) THEN RAISE EXCEPTION 'human history required'; END IF;
 UPDATE extraction_candidate SET review_state='CONFIRMED' WHERE id=c;
 INSERT INTO candidate_verification(id,candidate_id,owner_id,extraction_id,document_id,observation_id,actor_id,action,changed_fields,request_hash) VALUES(v,c,a,e,d,o,a,'CONFIRM','',repeat('a',64));
 IF NOT EXISTS(SELECT 1 FROM trusted_medical_observation WHERE id=o) THEN RAISE EXCEPTION 'valid verification absent'; END IF;
 BEGIN UPDATE medical_observation SET verified_by=b WHERE id=o; RAISE EXCEPTION 'forged verification actor allowed'; EXCEPTION WHEN check_violation THEN NULL; END;
 BEGIN UPDATE candidate_verification SET actor_id=b WHERE id=v; RAISE EXCEPTION 'forged history actor allowed'; EXCEPTION WHEN check_violation THEN NULL; END;
 BEGIN INSERT INTO medical_observation(owner_id,document_id,extraction_id,source_candidate_id,original_test_name,original_value_text) VALUES(a,d,e,c,'Hb','12'); RAISE EXCEPTION 'duplicate candidate promotion allowed'; EXCEPTION WHEN unique_violation THEN NULL; END;
 DELETE FROM medical_document WHERE id=d;
 IF EXISTS(SELECT 1 FROM candidate_verification WHERE id=v) OR EXISTS(SELECT 1 FROM medical_observation WHERE id=o) THEN RAISE EXCEPTION 'review derivatives not removed'; END IF;
END $$;
ROLLBACK;
