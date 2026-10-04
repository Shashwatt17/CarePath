-- Conversations remain transient. Questions are private user-controlled drafts.
ALTER TABLE saved_question ADD COLUMN expected_evidence_count integer NOT NULL DEFAULT 0 CHECK (expected_evidence_count BETWEEN 0 AND 12);
CREATE TABLE saved_question_evidence (
 question_id uuid NOT NULL,
 owner_id uuid NOT NULL,
 observation_id uuid NOT NULL,
 PRIMARY KEY(question_id, observation_id),
 FOREIGN KEY(question_id,owner_id) REFERENCES saved_question(id,owner_id) ON DELETE CASCADE,
 FOREIGN KEY(observation_id,owner_id) REFERENCES medical_observation(id,owner_id) ON DELETE CASCADE
);
CREATE INDEX saved_question_evidence_observation_idx ON saved_question_evidence(observation_id,owner_id);
