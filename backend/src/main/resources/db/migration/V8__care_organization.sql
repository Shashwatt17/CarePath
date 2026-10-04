ALTER TABLE symptom ADD COLUMN started_at timestamptz;
ALTER TABLE symptom ADD COLUMN resolved_at timestamptz;
UPDATE symptom SET started_at=start_date::timestamp AT TIME ZONE 'UTC', resolved_at=resolved_date::timestamp AT TIME ZONE 'UTC';
ALTER TABLE symptom ALTER COLUMN started_at SET NOT NULL;
ALTER TABLE symptom ADD CONSTRAINT symptom_times CHECK(resolved_at IS NULL OR resolved_at>=started_at);
ALTER TABLE appointment ADD COLUMN phone varchar(40);
ALTER TABLE appointment ADD COLUMN external_url varchar(500);
CREATE TABLE appointment_question (
 appointment_id uuid NOT NULL, owner_id uuid NOT NULL, question_id uuid NOT NULL,
 PRIMARY KEY(appointment_id,question_id),
 FOREIGN KEY(appointment_id,owner_id) REFERENCES appointment(id,owner_id) ON DELETE CASCADE,
 FOREIGN KEY(question_id,owner_id) REFERENCES saved_question(id,owner_id) ON DELETE CASCADE
);
ALTER TABLE follow_up ADD COLUMN instruction_key varchar(64);
ALTER TABLE follow_up ADD COLUMN confirmed_at_time timestamptz;
ALTER TABLE follow_up ADD COLUMN time_zone varchar(80);
CREATE UNIQUE INDEX follow_up_instruction_unique ON follow_up(document_id,instruction_key);
-- Evidence removal must not strand follow-ups or block document deletion.
ALTER TABLE follow_up DROP CONSTRAINT follow_up_page_id_owner_id_fkey;
ALTER TABLE follow_up ADD CONSTRAINT follow_up_page_owner_fk FOREIGN KEY(page_id,owner_id) REFERENCES document_page(id,owner_id) ON DELETE CASCADE;
ALTER TABLE notification DROP CONSTRAINT notification_reminder_id_owner_id_fkey;
ALTER TABLE notification ADD CONSTRAINT notification_reminder_owner_fk FOREIGN KEY(reminder_id,owner_id) REFERENCES reminder(id,owner_id) ON DELETE CASCADE;
CREATE INDEX notification_unread_idx ON notification(owner_id,read_at,created_at);
CREATE INDEX follow_up_status_idx ON follow_up(owner_id,status,confirmed_date);
CREATE INDEX symptom_active_idx ON symptom(owner_id,resolved_at,started_at);
CREATE UNIQUE INDEX reminder_appointment_offset ON reminder(appointment_id,offset_minutes);
CREATE UNIQUE INDEX reminder_follow_up_offset ON reminder(follow_up_id,offset_minutes);

ALTER TABLE document_page ADD CONSTRAINT care_page_document_owner UNIQUE(id,document_id,owner_id);
ALTER TABLE follow_up ADD CONSTRAINT care_follow_source FOREIGN KEY(page_id,document_id,owner_id) REFERENCES document_page(id,document_id,owner_id) ON DELETE CASCADE;
ALTER TABLE reminder ADD CONSTRAINT care_reminder_offset CHECK(offset_minutes IN (0,60,1440,2880,10080));
ALTER TABLE symptom ADD CONSTRAINT care_symptom_frequency CHECK(frequency IS NULL OR frequency IN ('ONCE','OCCASIONAL','DAILY','CONSTANT'));
