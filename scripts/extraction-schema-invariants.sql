BEGIN;
DO $$
DECLARE a uuid:=gen_random_uuid(); b uuid:=gen_random_uuid(); d uuid:=gen_random_uuid(); e uuid:=gen_random_uuid(); p uuid:=gen_random_uuid(); j uuid:=gen_random_uuid(); c uuid:=gen_random_uuid();
BEGIN
 INSERT INTO app_user(id,email,password_hash,display_name) VALUES(a,'extraction-a@example.invalid','not-real','SYNTHETIC'),(b,'extraction-b@example.invalid','not-real','SYNTHETIC');
 INSERT INTO medical_document(id,owner_id,original_filename,mime_type,byte_size,storage_key,sha256) VALUES(d,a,'synthetic.pdf','application/pdf',100,repeat('a',32),repeat('0',64));
 INSERT INTO processing_job(id,owner_id,document_id) VALUES(j,a,d);
 INSERT INTO document_extraction(id,owner_id,document_id,job_id,revision,pipeline_version,extractor) VALUES(e,a,d,j,1,'synthetic','rules');
 INSERT INTO document_page(id,owner_id,document_id,extraction_id,page_number,extracted_text) VALUES(p,a,d,e,1,'Hb | 12 | g/dL | 10 - 15');
 INSERT INTO extraction_candidate(id,owner_id,document_id,extraction_id,page_id,original_test_name,confidence_band,confidence_reasons,source_text,source_start,source_end) VALUES(c,a,d,e,p,'Hb','LOW','UNVERIFIED','Hb | 12',0,7);
 BEGIN UPDATE extraction_candidate SET owner_id=b WHERE id=c; RAISE EXCEPTION 'candidate owner FK absent'; EXCEPTION WHEN foreign_key_violation THEN NULL; END;
 BEGIN UPDATE extraction_candidate SET page_id=gen_random_uuid() WHERE id=c; RAISE EXCEPTION 'candidate page FK absent'; EXCEPTION WHEN foreign_key_violation THEN NULL; END;
 BEGIN UPDATE extraction_candidate SET source_end=source_start WHERE id=c; RAISE EXCEPTION 'source offset check absent'; EXCEPTION WHEN check_violation THEN NULL; END;
 BEGIN INSERT INTO processing_job(owner_id,document_id) VALUES(a,d); RAISE EXCEPTION 'duplicate active job accepted'; EXCEPTION WHEN unique_violation THEN NULL; END;
 IF EXISTS(SELECT 1 FROM medical_observation WHERE owner_id=a) THEN RAISE EXCEPTION 'candidate became trusted observation'; END IF;
 DELETE FROM medical_document WHERE id=d;
 IF EXISTS(SELECT 1 FROM extraction_candidate WHERE id=c) OR EXISTS(SELECT 1 FROM document_extraction WHERE id=e) OR EXISTS(SELECT 1 FROM document_page WHERE id=p) OR EXISTS(SELECT 1 FROM processing_job WHERE id=j) THEN RAISE EXCEPTION 'extraction deletion did not cascade'; END IF;
END $$;
ROLLBACK;
