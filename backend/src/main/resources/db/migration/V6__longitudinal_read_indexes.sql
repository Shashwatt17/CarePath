-- Read-only longitudinal projections; no persisted trend/derived fact table.
-- Match owner + report scans and the trusted-view source/history existence checks.
CREATE INDEX medical_observation_owner_document_idx ON medical_observation(owner_id,document_id,concept_id,observed_date,id);
CREATE INDEX observation_source_observation_owner_idx ON observation_source(observation_id,owner_id);
CREATE INDEX candidate_verification_observation_owner_idx ON candidate_verification(observation_id,owner_id,candidate_id);
