-- Core content is relational; the V1 reserved snapshot JSON column is not used.
ALTER TABLE visit_pack DROP CONSTRAINT visit_pack_status_check;
ALTER TABLE visit_pack ADD CONSTRAINT visit_pack_status_check CHECK(status IN ('DRAFT','GENERATED','READY','INVALIDATED','FAILED'));
ALTER TABLE visit_pack ADD COLUMN previous_pack_id uuid UNIQUE REFERENCES visit_pack(id) ON DELETE SET NULL;
ALTER TABLE visit_pack ADD COLUMN appointment_selected boolean NOT NULL DEFAULT false;
ALTER TABLE visit_pack ADD COLUMN pack_date date;
ALTER TABLE visit_pack ADD COLUMN renderer_version varchar(30);
ALTER TABLE visit_pack DROP CONSTRAINT visit_pack_appointment_id_owner_id_fkey;
ALTER TABLE visit_pack ADD CONSTRAINT pack_appointment_owner FOREIGN KEY(appointment_id,owner_id) REFERENCES appointment(id,owner_id) ON DELETE SET NULL (appointment_id);
ALTER TABLE visit_pack_item DROP CONSTRAINT visit_pack_item_check;
ALTER TABLE visit_pack_item ADD COLUMN item_type varchar(24) NOT NULL DEFAULT 'DOCUMENT' CHECK(item_type IN ('SYMPTOM','OBSERVATION','CHANGE','DOCUMENT','APPOINTMENT','FOLLOW_UP','SAVED_QUESTION','MANUAL_QUESTION'));
ALTER TABLE visit_pack_item ADD COLUMN other_observation_id uuid;
ALTER TABLE visit_pack_item ADD COLUMN include_notes boolean NOT NULL DEFAULT false;
ALTER TABLE visit_pack_item ADD COLUMN question_override varchar(1000);
ALTER TABLE visit_pack_item ADD CONSTRAINT pack_other_observation_owner FOREIGN KEY(other_observation_id,owner_id) REFERENCES medical_observation(id,owner_id) ON DELETE SET NULL (other_observation_id);
ALTER TABLE visit_pack_item DROP CONSTRAINT visit_pack_item_document_id_owner_id_fkey;
ALTER TABLE visit_pack_item ADD CONSTRAINT pack_item_document_owner FOREIGN KEY(document_id,owner_id) REFERENCES medical_document(id,owner_id) ON DELETE SET NULL (document_id);
ALTER TABLE visit_pack_item DROP CONSTRAINT visit_pack_item_observation_id_owner_id_fkey;
ALTER TABLE visit_pack_item ADD CONSTRAINT pack_item_observation_owner FOREIGN KEY(observation_id,owner_id) REFERENCES medical_observation(id,owner_id) ON DELETE SET NULL (observation_id);
ALTER TABLE visit_pack_item DROP CONSTRAINT visit_pack_item_symptom_id_owner_id_fkey;
ALTER TABLE visit_pack_item ADD CONSTRAINT pack_item_symptom_owner FOREIGN KEY(symptom_id,owner_id) REFERENCES symptom(id,owner_id) ON DELETE SET NULL (symptom_id);
ALTER TABLE visit_pack_item DROP CONSTRAINT visit_pack_item_prescription_id_owner_id_fkey;
ALTER TABLE visit_pack_item ADD CONSTRAINT pack_item_prescription_owner FOREIGN KEY(prescription_id,owner_id) REFERENCES prescription(id,owner_id) ON DELETE SET NULL (prescription_id);
ALTER TABLE visit_pack_item DROP CONSTRAINT visit_pack_item_visit_id_owner_id_fkey;
ALTER TABLE visit_pack_item ADD CONSTRAINT pack_item_visit_owner FOREIGN KEY(visit_id,owner_id) REFERENCES clinical_visit(id,owner_id) ON DELETE SET NULL (visit_id);
ALTER TABLE visit_pack_item DROP CONSTRAINT visit_pack_item_question_id_owner_id_fkey;
ALTER TABLE visit_pack_item ADD CONSTRAINT pack_item_question_owner FOREIGN KEY(question_id,owner_id) REFERENCES saved_question(id,owner_id) ON DELETE SET NULL (question_id);
ALTER TABLE visit_pack_item DROP CONSTRAINT visit_pack_item_appointment_id_owner_id_fkey;
ALTER TABLE visit_pack_item ADD CONSTRAINT pack_item_appointment_owner FOREIGN KEY(appointment_id,owner_id) REFERENCES appointment(id,owner_id) ON DELETE SET NULL (appointment_id);
ALTER TABLE visit_pack_item DROP CONSTRAINT visit_pack_item_follow_up_id_owner_id_fkey;
ALTER TABLE visit_pack_item ADD CONSTRAINT pack_item_follow_up_owner FOREIGN KEY(follow_up_id,owner_id) REFERENCES follow_up(id,owner_id) ON DELETE SET NULL (follow_up_id);

CREATE TABLE visit_pack_snapshot_item (
 id uuid PRIMARY KEY, owner_id uuid NOT NULL, visit_pack_id uuid NOT NULL, position integer NOT NULL CHECK(position>=0),
 item_type varchar(24) NOT NULL CHECK(item_type IN ('SYMPTOM','OBSERVATION','CHANGE','DOCUMENT','APPOINTMENT','FOLLOW_UP','SAVED_QUESTION','MANUAL_QUESTION')),
 heading varchar(255) NOT NULL,
 UNIQUE(id,owner_id), UNIQUE(visit_pack_id,position),
 FOREIGN KEY(visit_pack_id,owner_id) REFERENCES visit_pack(id,owner_id) ON DELETE CASCADE
);
CREATE TABLE visit_pack_snapshot_field (
 item_id uuid NOT NULL, owner_id uuid NOT NULL, position integer NOT NULL CHECK(position>=0),
 label varchar(100) NOT NULL, field_value text NOT NULL CHECK(length(field_value)<=4000),
 PRIMARY KEY(item_id,position), FOREIGN KEY(item_id,owner_id) REFERENCES visit_pack_snapshot_item(id,owner_id) ON DELETE CASCADE
);
-- Snapshot identifiers are historical references, not live read authority. No source FK may erase frozen evidence.
CREATE TABLE visit_pack_snapshot_evidence (
 item_id uuid NOT NULL, owner_id uuid NOT NULL, position integer NOT NULL CHECK(position>=0),
 document_id uuid NOT NULL, observation_id uuid, filename varchar(255) NOT NULL, page_number integer NOT NULL CHECK(page_number>=1),
 snippet text NOT NULL CHECK(length(snippet)<=4000),
 PRIMARY KEY(item_id,position), FOREIGN KEY(item_id,owner_id) REFERENCES visit_pack_snapshot_item(id,owner_id) ON DELETE CASCADE
);
CREATE INDEX pack_snapshot_owner_idx ON visit_pack_snapshot_item(owner_id,visit_pack_id,position);
CREATE INDEX pack_drafts_idx ON visit_pack(owner_id,status,created_at DESC);
-- A deleted source may become null, but multiple primary targets can never be injected.
ALTER TABLE visit_pack_item ADD CONSTRAINT pack_item_single_target CHECK(num_nonnulls(document_id,observation_id,symptom_id,prescription_id,visit_id,question_id,appointment_id,follow_up_id)<=1);
ALTER TABLE visit_pack_item ADD CONSTRAINT pack_item_shape CHECK(
 (document_id IS NULL OR item_type='DOCUMENT') AND
 (observation_id IS NULL OR item_type IN ('OBSERVATION','CHANGE')) AND
 (other_observation_id IS NULL OR item_type='CHANGE') AND
 (symptom_id IS NULL OR item_type='SYMPTOM') AND
 (question_id IS NULL OR item_type='SAVED_QUESTION') AND
 (appointment_id IS NULL OR item_type='APPOINTMENT') AND
 (follow_up_id IS NULL OR item_type='FOLLOW_UP') AND prescription_id IS NULL AND visit_id IS NULL AND
 (question_override IS NULL OR item_type IN ('SAVED_QUESTION','MANUAL_QUESTION')));
