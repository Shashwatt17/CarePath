-- V1/V2 are immutable. Originals remain UPLOADED; no intelligence is claimed.
ALTER TABLE medical_document ADD COLUMN page_count integer NOT NULL DEFAULT 1 CHECK (page_count BETWEEN 1 AND 200);
CREATE TABLE vault_document_tag (
  document_id uuid NOT NULL REFERENCES medical_document(id) ON DELETE CASCADE,
  tag varchar(40) NOT NULL CHECK (length(trim(tag)) > 0),
  PRIMARY KEY (document_id, tag)
);
-- Separate from processing_job: survives metadata deletion and aborted uploads.
CREATE TABLE vault_blob_cleanup (
  storage_key varchar(32) PRIMARY KEY CHECK (storage_key ~ '^[a-f0-9]{32}$'),
  not_before timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX vault_cleanup_due_idx ON vault_blob_cleanup(not_before);
CREATE INDEX medical_document_upload_idx ON medical_document(owner_id,uploaded_at DESC,id DESC);
CREATE INDEX medical_document_provider_idx ON medical_document(owner_id,lower(provider_name));
