-- Additive authentication migration. V1 remains unchanged.
CREATE TABLE auth_session (
  id uuid PRIMARY KEY,
  owner_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  expires_at timestamptz NOT NULL,
  revoked_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
  UNIQUE (id, owner_id),
  CHECK (expires_at > created_at)
);
CREATE TRIGGER auth_session_updated BEFORE UPDATE ON auth_session FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
CREATE INDEX auth_session_owner_idx ON auth_session(owner_id, expires_at);
-- Preserve referential integrity if a development database already contains V1 token rows.
INSERT INTO auth_session(id, owner_id, expires_at, revoked_at, created_at)
SELECT family_id, owner_id, max(expires_at), max(revoked_at), min(created_at)
FROM refresh_token GROUP BY family_id, owner_id;
ALTER TABLE refresh_token ADD CONSTRAINT refresh_session_fk
  FOREIGN KEY (family_id, owner_id) REFERENCES auth_session(id, owner_id) ON DELETE CASCADE;
CREATE UNIQUE INDEX refresh_one_child_idx ON refresh_token(parent_id) WHERE parent_id IS NOT NULL;
-- Unknown-account login failures must be recordable without inventing an owner.
ALTER TABLE audit_event ALTER COLUMN owner_id DROP NOT NULL;
ALTER TABLE audit_event ADD COLUMN reason_code varchar(40);
CREATE INDEX audit_event_action_time_idx ON audit_event(action, occurred_at DESC);
