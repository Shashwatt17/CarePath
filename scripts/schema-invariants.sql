-- Executable schema checks. Run against a migrated disposable/local database.
-- All synthetic rows are rolled back, including on failure by the caller.
BEGIN;
DO $$
DECLARE
  a uuid := gen_random_uuid(); b uuid := gen_random_uuid();
  da uuid := gen_random_uuid(); db uuid := gen_random_uuid();
  ea uuid := gen_random_uuid(); eb uuid := gen_random_uuid();
  pa uuid := gen_random_uuid(); pb uuid := gen_random_uuid();
  obs uuid := gen_random_uuid(); pack uuid := gen_random_uuid();
BEGIN
  INSERT INTO app_user(id,email,password_hash,display_name) VALUES
    (a,a::text || '@example.invalid','not-a-credential','SYNTHETIC A'),
    (b,b::text || '@example.invalid','not-a-credential','SYNTHETIC B');
  INSERT INTO medical_document(id,owner_id,original_filename,mime_type,byte_size,storage_key,sha256) VALUES
    (da,a,'SYNTHETIC-A.pdf','application/pdf',100,da::text,repeat('a',64)),
    (db,b,'SYNTHETIC-B.pdf','application/pdf',100,db::text,repeat('b',64));
  INSERT INTO document_extraction(id,owner_id,document_id,revision,pipeline_version,extractor) VALUES
    (ea,a,da,1,'test','test'), (eb,b,db,1,'test','test');
  INSERT INTO document_page(id,owner_id,document_id,extraction_id,page_number,extracted_text) VALUES
    (pa,a,da,ea,1,'SYNTHETIC Hb 12'),(pb,b,db,eb,1,'SYNTHETIC Hb 13');
  INSERT INTO medical_observation(id,owner_id,document_id,extraction_id,original_test_name,original_value_text)
    VALUES(obs,a,da,ea,'Hb','12');
  IF EXISTS(SELECT 1 FROM trusted_medical_observation WHERE id=obs) THEN
    RAISE EXCEPTION 'Pending observation entered trusted view';
  END IF;
  UPDATE medical_observation SET verification_status='CONFIRMED',verified_at=now() WHERE id=obs;
  IF EXISTS(SELECT 1 FROM trusted_medical_observation WHERE id=obs) THEN
    RAISE EXCEPTION 'Sourceless observation entered trusted view';
  END IF;
  INSERT INTO observation_source(owner_id,observation_id,page_id,extraction_id,document_id,evidence_text)
    VALUES(a,obs,pa,ea,da,'SYNTHETIC Hb 12');
  IF EXISTS(SELECT 1 FROM trusted_medical_observation WHERE id=obs) THEN
    RAISE EXCEPTION 'Observation without candidate/human decision entered trusted view';
  END IF;
  BEGIN
    INSERT INTO document_extraction(owner_id,document_id,revision,pipeline_version,extractor)
      VALUES(a,db,2,'test','test');
    RAISE EXCEPTION 'Cross-owner document link allowed';
  EXCEPTION WHEN foreign_key_violation THEN NULL; END;
  BEGIN
    INSERT INTO observation_source(owner_id,observation_id,page_id,extraction_id,document_id,evidence_text)
      VALUES(a,obs,pb,ea,da,'WRONG SOURCE');
    RAISE EXCEPTION 'Mismatched source page allowed';
  EXCEPTION WHEN foreign_key_violation THEN NULL; END;
  BEGIN
    UPDATE medical_observation SET extraction_confidence=1.1 WHERE id=obs;
    RAISE EXCEPTION 'Invalid confidence allowed';
  EXCEPTION WHEN check_violation THEN NULL; END;
  BEGIN
    UPDATE medical_document SET storage_key='../private' WHERE id=da;
    RAISE EXCEPTION 'Traversal key allowed';
  EXCEPTION WHEN check_violation THEN NULL; END;
  BEGIN
    UPDATE medical_observation SET normalized_value=12,normalized_unit=NULL WHERE id=obs;
    RAISE EXCEPTION 'Normalized value without unit allowed';
  EXCEPTION WHEN check_violation THEN NULL; END;
  INSERT INTO visit_pack(id,owner_id,title,reason_for_visit) VALUES(pack,a,'SYNTHETIC','Schema check');
  BEGIN
    INSERT INTO visit_pack_item(owner_id,visit_pack_id,position,document_id) VALUES(a,pack,0,db);
    RAISE EXCEPTION 'Cross-owner Visit Pack item allowed';
  EXCEPTION WHEN foreign_key_violation THEN NULL; END;
  BEGIN
    INSERT INTO visit_pack_item(owner_id,visit_pack_id,position,document_id,observation_id)
      VALUES(a,pack,1,da,obs);
    RAISE EXCEPTION 'Multiple Visit Pack targets allowed';
  EXCEPTION WHEN check_violation THEN NULL; END;
  BEGIN
    INSERT INTO share_token(owner_id,visit_pack_id,pack_revision,token_hash,expires_at)
      VALUES(a,pack,1,repeat('c',64),now()-interval '1 minute');
    RAISE EXCEPTION 'Already expired new token allowed';
  EXCEPTION WHEN check_violation THEN NULL; END;
  BEGIN
    INSERT INTO share_token(owner_id,visit_pack_id,pack_revision,token_hash,expires_at)
      VALUES(a,pack,1,repeat('c',64),now()+interval '25 hours');
    RAISE EXCEPTION 'Overlong share allowed';
  EXCEPTION WHEN check_violation THEN NULL; END;
  BEGIN
    INSERT INTO reminder(owner_id,scheduled_at,idempotency_key) VALUES(a,now(),gen_random_uuid());
    RAISE EXCEPTION 'Reminder without target allowed';
  EXCEPTION WHEN check_violation THEN NULL; END;
  BEGIN
    INSERT INTO follow_up(owner_id,document_id,status) VALUES(a,da,'CONFIRMED');
    RAISE EXCEPTION 'Confirmed follow-up without date allowed';
  EXCEPTION WHEN check_violation THEN NULL; END;
END $$;
ROLLBACK;
SELECT 'PASS: 14 schema invariants (not product authorization tests)' AS result;
