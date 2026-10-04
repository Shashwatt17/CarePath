BEGIN;
DO $$
DECLARE
  a uuid := gen_random_uuid(); b uuid := gen_random_uuid();
  session_id uuid := gen_random_uuid(); parent uuid := gen_random_uuid();
BEGIN
  INSERT INTO app_user(id,email,password_hash,display_name) VALUES
    (a,a::text || '@example.invalid','SYNTHETIC-NOT-A-PASSWORD','Synthetic A'),
    (b,b::text || '@example.invalid','SYNTHETIC-NOT-A-PASSWORD','Synthetic B');
  INSERT INTO auth_session(id,owner_id,expires_at) VALUES(session_id,a,now()+interval '1 hour');
  INSERT INTO refresh_token(id,owner_id,family_id,token_hash,expires_at)
    VALUES(parent,a,session_id,repeat('d',64),now()+interval '1 hour');
  BEGIN
    INSERT INTO refresh_token(owner_id,family_id,token_hash,expires_at)
      VALUES(b,session_id,repeat('e',64),now()+interval '1 hour');
    RAISE EXCEPTION 'Cross-owner token/session accepted';
  EXCEPTION WHEN foreign_key_violation THEN NULL; END;
  BEGIN
    INSERT INTO refresh_token(owner_id,family_id,token_hash,expires_at)
      VALUES(a,gen_random_uuid(),repeat('e',64),now()+interval '1 hour');
    RAISE EXCEPTION 'Orphan refresh session accepted';
  EXCEPTION WHEN foreign_key_violation THEN NULL; END;
  INSERT INTO refresh_token(owner_id,family_id,parent_id,token_hash,expires_at)
    VALUES(a,session_id,parent,repeat('e',64),now()+interval '1 hour');
  BEGIN
    INSERT INTO refresh_token(owner_id,family_id,parent_id,token_hash,expires_at)
      VALUES(a,session_id,parent,repeat('f',64),now()+interval '1 hour');
    RAISE EXCEPTION 'Two children of rotated token accepted';
  EXCEPTION WHEN unique_violation THEN NULL; END;
  BEGIN
    INSERT INTO auth_session(id,owner_id,expires_at) VALUES(gen_random_uuid(),a,now()-interval '1 minute');
    RAISE EXCEPTION 'Invalid session expiry accepted';
  EXCEPTION WHEN check_violation THEN NULL; END;
  INSERT INTO audit_event(action,outcome,actor_kind,reason_code)
    VALUES('LOGIN_FAILED','FAILURE','SYSTEM','INVALID_CREDENTIALS');
  IF NOT EXISTS(SELECT 1 FROM audit_event WHERE owner_id IS NULL AND action='LOGIN_FAILED') THEN
    RAISE EXCEPTION 'Anonymous failure audit missing';
  END IF;
END $$;
ROLLBACK;
SELECT 'PASS: 5 authentication schema invariants' AS result;
