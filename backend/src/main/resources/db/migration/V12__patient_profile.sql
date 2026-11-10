CREATE TABLE patient_profile (
 owner_id uuid PRIMARY KEY REFERENCES app_user(id) ON DELETE CASCADE,
 introduction varchar(2000) NOT NULL DEFAULT '',
 medical_history varchar(4000) NOT NULL DEFAULT '',
 medications varchar(2000) NOT NULL DEFAULT '',
 allergies varchar(2000) NOT NULL DEFAULT '',
 reason_for_visit varchar(2000) NOT NULL DEFAULT '',
 updated_at timestamptz NOT NULL DEFAULT now()
);
