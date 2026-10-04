DROP ALL OBJECTS;
CREATE TABLE app_user (
id uuid DEFAULT random_uuid() PRIMARY KEY,
email varchar(254) NOT NULL CHECK (email = lower(email)),
password_hash varchar(255) NOT NULL,
display_name varchar(120) NOT NULL,
time_zone varchar(80) NOT NULL DEFAULT 'UTC',
locale varchar(20) NOT NULL DEFAULT 'en',
status varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','DISABLED','DELETION_PENDING')),
onboarding_completed_at timestamp with time zone,
last_login_at timestamp with time zone,
created_at timestamp with time zone NOT NULL DEFAULT now(),
updated_at timestamp with time zone NOT NULL DEFAULT now(),
version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
UNIQUE (email)
);

CREATE TABLE refresh_token (
id uuid DEFAULT random_uuid() PRIMARY KEY,
owner_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
token_hash char(64) NOT NULL UNIQUE CHECK (token_hash ~ '^[0-9a-f]{64}$'),
family_id uuid NOT NULL,
parent_id uuid,
expires_at timestamp with time zone NOT NULL,
revoked_at timestamp with time zone,
used_at timestamp with time zone,
device_label varchar(160),
CHECK (expires_at > created_at),
created_at timestamp with time zone NOT NULL DEFAULT now(),
updated_at timestamp with time zone NOT NULL DEFAULT now(),
version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
UNIQUE (id, owner_id),
FOREIGN KEY (parent_id, owner_id) REFERENCES refresh_token(id, owner_id) ON DELETE CASCADE
);

CREATE TABLE audit_event (
id uuid DEFAULT random_uuid() PRIMARY KEY,
owner_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
action varchar(80) NOT NULL,
resource_type varchar(40),
resource_id uuid,
request_id uuid,
outcome varchar(12) NOT NULL CHECK (outcome IN ('SUCCESS','DENIED','FAILURE')),
actor_kind varchar(12) NOT NULL CHECK (actor_kind IN ('OWNER','SHARE','SYSTEM')),
-- No free-form payload: never put tokens, documents, questions, or health values in audit events.
occurred_at timestamp with time zone NOT NULL DEFAULT now(),
created_at timestamp with time zone NOT NULL DEFAULT now(),
updated_at timestamp with time zone NOT NULL DEFAULT now(),
version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
UNIQUE (id, owner_id)
);
-- Additive authentication migration. V1 remains unchanged.
CREATE TABLE auth_session (
  id uuid PRIMARY KEY,
  owner_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  expires_at timestamp with time zone NOT NULL,
  revoked_at timestamp with time zone,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
  UNIQUE (id, owner_id),
  CHECK (expires_at > created_at)
);

CREATE INDEX auth_session_owner_idx ON auth_session(owner_id, expires_at);
-- Preserve referential integrity if a development database already contains V1 token rows.
INSERT INTO auth_session(id, owner_id, expires_at, revoked_at, created_at)
SELECT family_id, owner_id, max(expires_at), max(revoked_at), min(created_at)
FROM refresh_token GROUP BY family_id, owner_id;
ALTER TABLE refresh_token ADD CONSTRAINT refresh_session_fk
  FOREIGN KEY (family_id, owner_id) REFERENCES auth_session(id, owner_id) ON DELETE CASCADE;
CREATE UNIQUE INDEX refresh_one_child_idx ON refresh_token(parent_id);
-- Unknown-account login failures must be recordable without inventing an owner.
ALTER TABLE audit_event ALTER COLUMN owner_id DROP NOT NULL;
ALTER TABLE audit_event ADD COLUMN reason_code varchar(40);
CREATE INDEX audit_event_action_time_idx ON audit_event(action, occurred_at DESC);

CREATE TABLE medical_document (
id uuid DEFAULT random_uuid() PRIMARY KEY,
owner_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
original_filename varchar(255) NOT NULL,
document_type varchar(32) NOT NULL DEFAULT 'OTHER' CHECK (document_type IN ('LAB_REPORT','PRESCRIPTION','DIAGNOSTIC_REPORT','DISCHARGE_SUMMARY','VACCINATION_RECORD','DOCTOR_NOTE','REFERRAL','MEDICAL_BILL','OTHER')),
document_date date,
date_precision varchar(12) CHECK (date_precision IN ('DAY','MONTH','YEAR','UNKNOWN')),
uploaded_at timestamp with time zone NOT NULL DEFAULT now(),
provider_name varchar(255),
mime_type varchar(50) NOT NULL CHECK (mime_type IN ('application/pdf','image/jpeg','image/png')),
byte_size bigint NOT NULL CHECK (byte_size > 0 AND byte_size <= 20971520),
storage_key varchar(255) NOT NULL UNIQUE CHECK (storage_key ~ '^[a-zA-Z0-9_-]+$'),
storage_backend varchar(20) NOT NULL DEFAULT 'LOCAL' CHECK (storage_backend IN ('LOCAL','S3')),
sha256 char(64) NOT NULL CHECK (sha256 ~ '^[0-9a-f]{64}$'),
status varchar(20) NOT NULL DEFAULT 'UPLOADED' CHECK (status IN ('UPLOADED','PROCESSING','NEEDS_REVIEW','COMPLETED','FAILED')),
extraction_confidence numeric(5,4) CHECK (extraction_confidence BETWEEN 0 AND 1),
error_code varchar(80),
processed_at timestamp with time zone,
created_at timestamp with time zone NOT NULL DEFAULT now(),
updated_at timestamp with time zone NOT NULL DEFAULT now(),
version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
UNIQUE (id, owner_id)
);

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
  not_before timestamp with time zone NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now()
);
CREATE INDEX vault_cleanup_due_idx ON vault_blob_cleanup(not_before);
CREATE INDEX medical_document_upload_idx ON medical_document(owner_id,uploaded_at DESC,id DESC);


CREATE TABLE document_extraction (
id uuid DEFAULT random_uuid() PRIMARY KEY,
owner_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
document_id uuid NOT NULL,
revision integer NOT NULL CHECK (revision > 0),
pipeline_version varchar(60) NOT NULL,
extractor varchar(80) NOT NULL,
ocr_used boolean NOT NULL DEFAULT false,
classification varchar(32),
confidence numeric(5,4) CHECK (confidence BETWEEN 0 AND 1),
completed_at timestamp with time zone,
UNIQUE (document_id, revision),
created_at timestamp with time zone NOT NULL DEFAULT now(),
updated_at timestamp with time zone NOT NULL DEFAULT now(),
version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
UNIQUE (id, owner_id),
FOREIGN KEY (document_id, owner_id) REFERENCES medical_document(id, owner_id) ON DELETE CASCADE
);


CREATE TABLE document_page (
id uuid DEFAULT random_uuid() PRIMARY KEY,
owner_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
document_id uuid NOT NULL,
extraction_id uuid NOT NULL,
page_number integer NOT NULL CHECK (page_number > 0),
extracted_text text NOT NULL,
confidence numeric(5,4) CHECK (confidence BETWEEN 0 AND 1),
UNIQUE (extraction_id, page_number),
created_at timestamp with time zone NOT NULL DEFAULT now(),
updated_at timestamp with time zone NOT NULL DEFAULT now(),
version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
UNIQUE (id, owner_id),
FOREIGN KEY (document_id, owner_id) REFERENCES medical_document(id, owner_id) ON DELETE CASCADE,
FOREIGN KEY (extraction_id, owner_id) REFERENCES document_extraction(id, owner_id) ON DELETE CASCADE
);


CREATE TABLE processing_job (
id uuid DEFAULT random_uuid() PRIMARY KEY,
owner_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
document_id uuid NOT NULL,
job_type varchar(32) NOT NULL DEFAULT 'EXTRACT_DOCUMENT' CHECK (job_type IN ('EXTRACT_DOCUMENT','DELETE_BLOB')),
status varchar(16) NOT NULL DEFAULT 'QUEUED' CHECK (status IN ('QUEUED','RUNNING','SUCCEEDED','FAILED')),
attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
max_attempts integer NOT NULL DEFAULT 3 CHECK (max_attempts BETWEEN 1 AND 10),
available_at timestamp with time zone NOT NULL DEFAULT now(),
locked_until timestamp with time zone,
locked_by varchar(120),
last_error_code varchar(80),
finished_at timestamp with time zone,
created_at timestamp with time zone NOT NULL DEFAULT now(),
updated_at timestamp with time zone NOT NULL DEFAULT now(),
version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
UNIQUE (id, owner_id),
FOREIGN KEY (document_id, owner_id) REFERENCES medical_document(id, owner_id) ON DELETE CASCADE
);


ALTER TABLE document_extraction ADD CONSTRAINT extraction_document_unique UNIQUE (id,document_id,owner_id);
ALTER TABLE document_page ADD CONSTRAINT page_extraction_document_fk FOREIGN KEY(extraction_id,document_id,owner_id) REFERENCES document_extraction(id,document_id,owner_id) ON DELETE CASCADE;
-- Raw extraction candidates are deliberately separate from medical_observation / trusted views.
ALTER TABLE processing_job ADD COLUMN request_id uuid;
ALTER TABLE processing_job ADD COLUMN error_retryable boolean NOT NULL DEFAULT false;
ALTER TABLE document_extraction ADD COLUMN job_id uuid UNIQUE;
ALTER TABLE processing_job ADD CONSTRAINT processing_job_document_identity UNIQUE(id,document_id,owner_id);
ALTER TABLE document_extraction ADD CONSTRAINT extraction_job_owner_fk FOREIGN KEY(job_id,document_id,owner_id) REFERENCES processing_job(id,document_id,owner_id) ON DELETE CASCADE;
ALTER TABLE document_extraction ADD COLUMN confidence_band varchar(10) NOT NULL DEFAULT 'LOW' CHECK(confidence_band IN ('HIGH','MEDIUM','LOW'));
ALTER TABLE document_extraction ADD COLUMN classification_method varchar(60) NOT NULL DEFAULT 'UNKNOWN';
ALTER TABLE document_extraction ADD COLUMN classification_evidence text NOT NULL DEFAULT '[]';
ALTER TABLE document_extraction ADD COLUMN document_information text NOT NULL DEFAULT '{"date":null,"provider":null,"warnings":[]}';
ALTER TABLE document_extraction ADD COLUMN needs_review boolean NOT NULL DEFAULT true;
ALTER TABLE document_page ADD COLUMN extraction_method varchar(24) NOT NULL DEFAULT 'PDFBOX_TEXT' CHECK(extraction_method IN ('PDFBOX_TEXT','TESSERACT_OCR'));
ALTER TABLE document_page ADD COLUMN line_layout text NOT NULL DEFAULT '[]';
ALTER TABLE document_page ADD CONSTRAINT candidate_page_identity UNIQUE(id,extraction_id,document_id,owner_id);
CREATE TABLE extraction_candidate (
 id uuid PRIMARY KEY,
 owner_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
 document_id uuid NOT NULL,
 extraction_id uuid NOT NULL,
 page_id uuid NOT NULL,
 original_test_name varchar(160) NOT NULL,
 original_value varchar(80),
 numeric_value numeric(30,10),
 value_comparator varchar(3),
 original_unit varchar(50),
 reference_lower numeric(30,10),
 reference_upper numeric(30,10),
 reference_text varchar(255),
 abnormal_flag varchar(10) CHECK(abnormal_flag IN ('HIGH','LOW','ABNORMAL')),
 report_date date,
 provider_name varchar(255),
 confidence_band varchar(10) NOT NULL CHECK(confidence_band IN ('HIGH','MEDIUM','LOW')),
 confidence_reasons varchar(500) NOT NULL,
 source_text text NOT NULL CHECK(length(source_text)>0),
 source_start integer NOT NULL CHECK(source_start>=0),
 source_end integer NOT NULL CHECK(source_end>source_start),
 source_box text,
 created_at timestamp with time zone NOT NULL DEFAULT now(),
 FOREIGN KEY(page_id,extraction_id,document_id,owner_id) REFERENCES document_page(id,extraction_id,document_id,owner_id) ON DELETE CASCADE,
 CHECK(reference_lower IS NULL OR reference_upper IS NULL OR reference_lower<=reference_upper)
);
CREATE INDEX extraction_candidate_owner_document_idx ON extraction_candidate(owner_id,document_id,extraction_id);

CREATE TABLE canonical_medical_concept (
id uuid DEFAULT random_uuid() PRIMARY KEY,
canonical_name varchar(160) NOT NULL UNIQUE,
identifier_system varchar(40),
identifier_code varchar(80),
specimen varchar(100),
method varchar(100),
property varchar(100),
canonical_unit varchar(50),
mapping_source text,
vocabulary_version varchar(80) NOT NULL,
CHECK ((identifier_system IS NULL) = (identifier_code IS NULL)),
CHECK (identifier_code IS NULL OR mapping_source IS NOT NULL),
UNIQUE (identifier_system, identifier_code),
created_at timestamp with time zone NOT NULL DEFAULT now(),
updated_at timestamp with time zone NOT NULL DEFAULT now(),
version bigint NOT NULL DEFAULT 0 CHECK (version >= 0)
);



CREATE TABLE concept_alias (
id uuid DEFAULT random_uuid() PRIMARY KEY,
concept_id uuid NOT NULL REFERENCES canonical_medical_concept(id) ON DELETE CASCADE,
normalized_alias varchar(160) NOT NULL,
context_key varchar(160) NOT NULL DEFAULT '',
mapping_confidence numeric(5,4) NOT NULL CHECK (mapping_confidence BETWEEN 0 AND 1),
reviewed boolean NOT NULL DEFAULT false,
source text NOT NULL,
UNIQUE (normalized_alias, context_key, concept_id),
created_at timestamp with time zone NOT NULL DEFAULT now(),
updated_at timestamp with time zone NOT NULL DEFAULT now(),
version bigint NOT NULL DEFAULT 0 CHECK (version >= 0)
);



CREATE TABLE unit_conversion_rule (
id uuid DEFAULT random_uuid() PRIMARY KEY,
concept_id uuid NOT NULL REFERENCES canonical_medical_concept(id) ON DELETE CASCADE,
source_unit varchar(50) NOT NULL,
target_unit varchar(50) NOT NULL,
multiplier numeric NOT NULL CHECK (multiplier > 0),
offset_value numeric NOT NULL DEFAULT 0,
rule_version varchar(60) NOT NULL,
source_citation text NOT NULL,
reviewed boolean NOT NULL DEFAULT false,
UNIQUE (concept_id, source_unit, target_unit, rule_version),
created_at timestamp with time zone NOT NULL DEFAULT now(),
updated_at timestamp with time zone NOT NULL DEFAULT now(),
version bigint NOT NULL DEFAULT 0 CHECK (version >= 0)
);



CREATE TABLE medical_observation (
id uuid DEFAULT random_uuid() PRIMARY KEY,
owner_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
document_id uuid NOT NULL,
extraction_id uuid NOT NULL,
original_test_name varchar(255) NOT NULL,
original_value_text varchar(255) NOT NULL,
original_numeric_value numeric,
original_unit varchar(80),
original_reference_text varchar(255),
original_reference_low numeric,
original_reference_high numeric,
original_abnormal_flag varchar(30),
value_comparator varchar(4) CHECK (value_comparator IN ('=','<','<=','>','>=')),
concept_id uuid REFERENCES canonical_medical_concept(id),
canonical_name_snapshot varchar(160),
mapping_confidence numeric(5,4) CHECK (mapping_confidence BETWEEN 0 AND 1),
normalization_version varchar(60),
normalized_value numeric,
normalized_unit varchar(50),
normalized_reference_low numeric,
normalized_reference_high numeric,
conversion_rule_id uuid REFERENCES unit_conversion_rule(id),
observed_date date,
date_basis varchar(20) CHECK (date_basis IN ('SAMPLE','REPORT','USER_CONFIRMED','UNKNOWN')),
specimen varchar(100),
method varchar(100),
panel_key varchar(120),
laboratory varchar(255),
extraction_confidence numeric(5,4) CHECK (extraction_confidence BETWEEN 0 AND 1),
verification_status varchar(20) NOT NULL DEFAULT 'PENDING' CHECK (verification_status IN ('PENDING','AUTO_VERIFIED','CONFIRMED','CORRECTED','REJECTED')),
verified_at timestamp with time zone,
CHECK (original_reference_low IS NULL OR original_reference_high IS NULL OR original_reference_low <= original_reference_high),
CHECK (normalized_reference_low IS NULL OR normalized_reference_high IS NULL OR normalized_reference_low <= normalized_reference_high),
CHECK ((normalized_value IS NULL) = (normalized_unit IS NULL)),
CHECK (normalized_value IS NULL OR concept_id IS NOT NULL),
CHECK (verification_status IN ('PENDING','REJECTED') OR verified_at IS NOT NULL),
created_at timestamp with time zone NOT NULL DEFAULT now(),
updated_at timestamp with time zone NOT NULL DEFAULT now(),
version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
UNIQUE (id, owner_id),
FOREIGN KEY (document_id, owner_id) REFERENCES medical_document(id, owner_id) ON DELETE CASCADE,
FOREIGN KEY (extraction_id, owner_id) REFERENCES document_extraction(id, owner_id) ON DELETE CASCADE,
FOREIGN KEY (extraction_id, document_id, owner_id) REFERENCES document_extraction(id, document_id, owner_id) ON DELETE CASCADE
);



CREATE INDEX medical_observation_owner_created_idx ON medical_observation (owner_id, created_at DESC);

CREATE INDEX medical_observation_history_idx ON medical_observation(owner_id, concept_id, observed_date DESC);

ALTER TABLE medical_observation ADD CONSTRAINT observation_extraction_unique UNIQUE (id, extraction_id, document_id, owner_id);

ALTER TABLE document_page ADD CONSTRAINT page_extraction_unique UNIQUE (id, extraction_id, document_id, owner_id);

CREATE TABLE observation_source (
id uuid DEFAULT random_uuid() PRIMARY KEY,
owner_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
observation_id uuid NOT NULL,
page_id uuid NOT NULL,
extraction_id uuid NOT NULL,
document_id uuid NOT NULL,
evidence_text text NOT NULL CHECK (length(evidence_text) > 0),
char_start integer CHECK (char_start >= 0),
char_end integer,
bounding_box text,
CHECK ((char_start IS NULL AND char_end IS NULL) OR (char_start IS NOT NULL AND char_end IS NOT NULL AND char_end > char_start)),

created_at timestamp with time zone NOT NULL DEFAULT now(),
updated_at timestamp with time zone NOT NULL DEFAULT now(),
version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
UNIQUE (id, owner_id),
FOREIGN KEY (observation_id, extraction_id, document_id, owner_id) REFERENCES medical_observation(id, extraction_id, document_id, owner_id) ON DELETE CASCADE,
FOREIGN KEY (page_id, extraction_id, document_id, owner_id) REFERENCES document_page(id, extraction_id, document_id, owner_id) ON DELETE CASCADE
);



CREATE INDEX observation_source_owner_created_idx ON observation_source (owner_id, created_at DESC);


CREATE VIEW trusted_medical_observation AS SELECT * FROM medical_observation;
-- Immutable V1–V4; raw extraction columns are never overwritten by review.
ALTER TABLE canonical_medical_concept ADD COLUMN active boolean NOT NULL DEFAULT true;
ALTER TABLE extraction_candidate ADD COLUMN review_state varchar(30) NOT NULL DEFAULT 'PENDING_REVIEW'
 CHECK(review_state IN ('PENDING_REVIEW','CONFIRMED','CORRECTED_AND_CONFIRMED','REJECTED'));
ALTER TABLE extraction_candidate ADD COLUMN review_version bigint NOT NULL DEFAULT 0 CHECK(review_version>=0);
ALTER TABLE extraction_candidate ADD CONSTRAINT candidate_source_identity UNIQUE(id,extraction_id,document_id,owner_id);
CREATE INDEX candidate_review_queue_idx ON extraction_candidate(owner_id,review_state,created_at,id);
ALTER TABLE medical_observation ALTER COLUMN original_value_text DROP NOT NULL;
ALTER TABLE medical_observation ADD COLUMN source_candidate_id uuid UNIQUE;
ALTER TABLE medical_observation ADD CONSTRAINT observation_candidate_fk FOREIGN KEY(source_candidate_id,extraction_id,document_id,owner_id)
 REFERENCES extraction_candidate(id,extraction_id,document_id,owner_id) ON DELETE CASCADE;
ALTER TABLE medical_observation ADD COLUMN verified_test_name varchar(160);
ALTER TABLE medical_observation ADD COLUMN verified_value_text varchar(80);
ALTER TABLE medical_observation ADD COLUMN verified_numeric_value numeric;
ALTER TABLE medical_observation ADD COLUMN verified_unit varchar(50);
ALTER TABLE medical_observation ADD COLUMN verified_reference_text varchar(255);
ALTER TABLE medical_observation ADD COLUMN verified_reference_low numeric;
ALTER TABLE medical_observation ADD COLUMN verified_reference_high numeric;
ALTER TABLE medical_observation ADD COLUMN verified_by uuid REFERENCES app_user(id);
ALTER TABLE medical_observation ADD COLUMN mapping_status varchar(24);
ALTER TABLE medical_observation ADD COLUMN unit_status varchar(30);
ALTER TABLE medical_observation ADD COLUMN derived_range_status varchar(24) NOT NULL DEFAULT 'NOT_COMPARABLE'
 CHECK(derived_range_status IN ('BELOW','WITHIN','ABOVE','NOT_COMPARABLE'));
ALTER TABLE medical_observation ADD CONSTRAINT human_verification_owner CHECK(verified_by IS NULL OR verified_by=owner_id);
ALTER TABLE medical_observation ADD CONSTRAINT verified_reference_order CHECK(verified_reference_low IS NULL OR verified_reference_high IS NULL OR verified_reference_low<=verified_reference_high);
CREATE TABLE candidate_verification (
 id uuid PRIMARY KEY,
 candidate_id uuid NOT NULL UNIQUE,
 owner_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
 extraction_id uuid NOT NULL,
 document_id uuid NOT NULL,
 observation_id uuid,
 actor_id uuid NOT NULL REFERENCES app_user(id),
 action varchar(12) NOT NULL CHECK(action IN ('CONFIRM','CORRECT','REJECT')),
 reason varchar(500),
 changed_fields varchar(400) NOT NULL,
 request_hash varchar(64) NOT NULL,
 occurred_at timestamp with time zone NOT NULL DEFAULT now(),
 CHECK(actor_id=owner_id),
 CHECK((action='REJECT' AND observation_id IS NULL) OR (action IN ('CONFIRM','CORRECT') AND observation_id IS NOT NULL)),
 FOREIGN KEY(candidate_id,extraction_id,document_id,owner_id) REFERENCES extraction_candidate(id,extraction_id,document_id,owner_id) ON DELETE CASCADE,
 FOREIGN KEY(observation_id,owner_id) REFERENCES medical_observation(id,owner_id) ON DELETE CASCADE
);
CREATE INDEX candidate_verification_owner_idx ON candidate_verification(owner_id,occurred_at);
-- Older AUTO_VERIFIED rows cannot enter the Phase 5 trusted read model.
DROP VIEW trusted_medical_observation;
CREATE VIEW trusted_medical_observation AS
 SELECT o.* FROM medical_observation o
 JOIN extraction_candidate c ON c.id=o.source_candidate_id AND c.owner_id=o.owner_id
 WHERE o.verification_status IN ('CONFIRMED','CORRECTED') AND o.verified_by=o.owner_id
 AND c.review_state IN ('CONFIRMED','CORRECTED_AND_CONFIRMED')
 AND EXISTS(SELECT 1 FROM observation_source s WHERE s.observation_id=o.id AND s.owner_id=o.owner_id)
 AND EXISTS(SELECT 1 FROM candidate_verification v WHERE v.observation_id=o.id AND v.candidate_id=c.id AND v.owner_id=o.owner_id);
-- Existing completed extraction batches are pending explicit human review.
UPDATE medical_document SET status='NEEDS_REVIEW'
 WHERE status='COMPLETED' AND EXISTS(SELECT 1 FROM extraction_candidate c WHERE c.document_id=medical_document.id AND c.review_state='PENDING_REVIEW');

-- Limited demo laboratory terminology. No LOINC/specimen/method inference.
-- Sources and scope: docs/NORMALIZATION_AND_REVIEW.md; semantic aliases require human clinical review before deployment.
INSERT INTO canonical_medical_concept(id,canonical_name,canonical_unit,mapping_source,vocabulary_version) VALUES('1ae1add1-f6bd-56a7-b9a4-6b91de411054','Hemoglobin','g/dL','CarePath demo terminology v1; MedlinePlus lab test names; no specimen/method equivalence claim','carepath-demo-v1');
INSERT INTO concept_alias(id,concept_id,normalized_alias,mapping_confidence,reviewed,source) VALUES('31bf88ec-472d-5584-b836-119df4783de7','1ae1add1-f6bd-56a7-b9a4-6b91de411054','hb',1,true,'carepath-demo-v1; exact lexical alias only');
INSERT INTO concept_alias(id,concept_id,normalized_alias,mapping_confidence,reviewed,source) VALUES('9bf03760-240e-5755-bdd1-beda6dd4761b','1ae1add1-f6bd-56a7-b9a4-6b91de411054','hgb',1,true,'carepath-demo-v1; exact lexical alias only');
INSERT INTO concept_alias(id,concept_id,normalized_alias,mapping_confidence,reviewed,source) VALUES('fcc3c2d7-2bf7-5e1b-8f6d-decae303ed28','1ae1add1-f6bd-56a7-b9a4-6b91de411054','haemoglobin',1,true,'carepath-demo-v1; exact lexical alias only');
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('a3a3fdd3-6ac6-54b5-a9b4-4e884f260732','1ae1add1-f6bd-56a7-b9a4-6b91de411054','g/dL','g/dL',1,'carepath-units-v1','SI identity for explicitly listed concept/unit',true);
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('47ef4a36-e9d2-57d3-b468-4afafe3c5ef5','1ae1add1-f6bd-56a7-b9a4-6b91de411054','g/L','g/dL',0.1,'carepath-units-v1','SI prefix conversion: 1 dL = 0.1 L',true);
INSERT INTO canonical_medical_concept(id,canonical_name,canonical_unit,mapping_source,vocabulary_version) VALUES('9ac73aa5-f3b3-5f13-9535-d9d2c859fe8b','Hematocrit','%','CarePath demo terminology v1; MedlinePlus lab test names; no specimen/method equivalence claim','carepath-demo-v1');
INSERT INTO concept_alias(id,concept_id,normalized_alias,mapping_confidence,reviewed,source) VALUES('c4b4f7cc-3af0-54aa-b72a-f5dcf3fe3688','9ac73aa5-f3b3-5f13-9535-d9d2c859fe8b','hct',1,true,'carepath-demo-v1; exact lexical alias only');
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('412ef247-e5d1-52de-a824-4c95b408796c','9ac73aa5-f3b3-5f13-9535-d9d2c859fe8b','%','%',1,'carepath-units-v1','SI identity for explicitly listed concept/unit',true);
INSERT INTO canonical_medical_concept(id,canonical_name,canonical_unit,mapping_source,vocabulary_version) VALUES('0772d251-7294-5b18-ab5d-02862705df61','RBC count','10^12/L','CarePath demo terminology v1; MedlinePlus lab test names; no specimen/method equivalence claim','carepath-demo-v1');
INSERT INTO concept_alias(id,concept_id,normalized_alias,mapping_confidence,reviewed,source) VALUES('a2a6eb0d-45d8-5cc3-bb26-0326dab79724','0772d251-7294-5b18-ab5d-02862705df61','rbc',1,true,'carepath-demo-v1; exact lexical alias only');
INSERT INTO concept_alias(id,concept_id,normalized_alias,mapping_confidence,reviewed,source) VALUES('9e849dc8-5307-57a6-9506-bff759bbd0c3','0772d251-7294-5b18-ab5d-02862705df61','red blood cell count',1,true,'carepath-demo-v1; exact lexical alias only');
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('b299252c-37bf-509e-a660-d4a34eee71b8','0772d251-7294-5b18-ab5d-02862705df61','10^12/L','10^12/L',1,'carepath-units-v1','SI identity for explicitly listed concept/unit',true);
INSERT INTO canonical_medical_concept(id,canonical_name,canonical_unit,mapping_source,vocabulary_version) VALUES('eb6880b4-eb08-5465-94ab-4b41d72b814f','WBC count','10^9/L','CarePath demo terminology v1; MedlinePlus lab test names; no specimen/method equivalence claim','carepath-demo-v1');
INSERT INTO concept_alias(id,concept_id,normalized_alias,mapping_confidence,reviewed,source) VALUES('5040c41f-e59e-5845-ab7e-20e737bc84a4','eb6880b4-eb08-5465-94ab-4b41d72b814f','wbc',1,true,'carepath-demo-v1; exact lexical alias only');
INSERT INTO concept_alias(id,concept_id,normalized_alias,mapping_confidence,reviewed,source) VALUES('e9758bfe-ecd3-5584-8a8e-f4edcbe5294f','eb6880b4-eb08-5465-94ab-4b41d72b814f','white blood cell count',1,true,'carepath-demo-v1; exact lexical alias only');
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('4a3dbf36-3c94-53b3-9e96-31ee8c9ede0b','eb6880b4-eb08-5465-94ab-4b41d72b814f','10^9/L','10^9/L',1,'carepath-units-v1','SI identity for explicitly listed concept/unit',true);
INSERT INTO canonical_medical_concept(id,canonical_name,canonical_unit,mapping_source,vocabulary_version) VALUES('a6b5204f-04c9-5914-9a4e-3164c618658c','Platelet count','10^9/L','CarePath demo terminology v1; MedlinePlus lab test names; no specimen/method equivalence claim','carepath-demo-v1');
INSERT INTO concept_alias(id,concept_id,normalized_alias,mapping_confidence,reviewed,source) VALUES('b0ca28ef-5d5e-5118-9651-5ef3eeed7b8f','a6b5204f-04c9-5914-9a4e-3164c618658c','platelets',1,true,'carepath-demo-v1; exact lexical alias only');
INSERT INTO concept_alias(id,concept_id,normalized_alias,mapping_confidence,reviewed,source) VALUES('b059e0a0-2cb7-5edd-aa96-4707e716b086','a6b5204f-04c9-5914-9a4e-3164c618658c','plt',1,true,'carepath-demo-v1; exact lexical alias only');
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('4a6807be-d4b8-5321-afe5-a2449ae6fbeb','a6b5204f-04c9-5914-9a4e-3164c618658c','10^9/L','10^9/L',1,'carepath-units-v1','SI identity for explicitly listed concept/unit',true);
INSERT INTO canonical_medical_concept(id,canonical_name,canonical_unit,mapping_source,vocabulary_version) VALUES('3c79eaa2-196b-5579-9eb6-cbb51f109dc1','MCV','fL','CarePath demo terminology v1; MedlinePlus lab test names; no specimen/method equivalence claim','carepath-demo-v1');
INSERT INTO concept_alias(id,concept_id,normalized_alias,mapping_confidence,reviewed,source) VALUES('3d915638-20d0-5cbe-a4ac-ce3757d8fdb9','3c79eaa2-196b-5579-9eb6-cbb51f109dc1','mean corpuscular volume',1,true,'carepath-demo-v1; exact lexical alias only');
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('2aac9e14-f441-5ee6-a515-3535f3484304','3c79eaa2-196b-5579-9eb6-cbb51f109dc1','fL','fL',1,'carepath-units-v1','SI identity for explicitly listed concept/unit',true);
INSERT INTO canonical_medical_concept(id,canonical_name,canonical_unit,mapping_source,vocabulary_version) VALUES('cecfd877-0dd0-57e0-bc1e-49a1dcda21cb','MCH','pg','CarePath demo terminology v1; MedlinePlus lab test names; no specimen/method equivalence claim','carepath-demo-v1');
INSERT INTO concept_alias(id,concept_id,normalized_alias,mapping_confidence,reviewed,source) VALUES('b14d5cd4-8706-5e44-8cbb-55e8777b6564','cecfd877-0dd0-57e0-bc1e-49a1dcda21cb','mean corpuscular hemoglobin',1,true,'carepath-demo-v1; exact lexical alias only');
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('f8e77ef3-66b9-50e0-9765-29577efc0d9e','cecfd877-0dd0-57e0-bc1e-49a1dcda21cb','pg','pg',1,'carepath-units-v1','SI identity for explicitly listed concept/unit',true);
INSERT INTO canonical_medical_concept(id,canonical_name,canonical_unit,mapping_source,vocabulary_version) VALUES('4666dd53-a262-505e-8607-4caf54b3dc49','MCHC','g/dL','CarePath demo terminology v1; MedlinePlus lab test names; no specimen/method equivalence claim','carepath-demo-v1');
INSERT INTO concept_alias(id,concept_id,normalized_alias,mapping_confidence,reviewed,source) VALUES('859e0d9b-887c-55c2-93f7-7c1197ac48ee','4666dd53-a262-505e-8607-4caf54b3dc49','mean corpuscular hemoglobin concentration',1,true,'carepath-demo-v1; exact lexical alias only');
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('608ae3c3-b71e-5756-9770-4c4ec00a8d7f','4666dd53-a262-505e-8607-4caf54b3dc49','g/dL','g/dL',1,'carepath-units-v1','SI identity for explicitly listed concept/unit',true);
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('4644560c-05d1-5ba4-82eb-d2da922ce19d','4666dd53-a262-505e-8607-4caf54b3dc49','g/L','g/dL',0.1,'carepath-units-v1','SI prefix conversion: 1 dL = 0.1 L',true);
INSERT INTO canonical_medical_concept(id,canonical_name,canonical_unit,mapping_source,vocabulary_version) VALUES('855cd97d-928e-586a-bccb-cd67339c11ba','Vitamin D','ng/mL','CarePath demo terminology v1; MedlinePlus lab test names; no specimen/method equivalence claim','carepath-demo-v1');
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('6c836b61-1b03-5fc2-a119-738e3a144eb6','855cd97d-928e-586a-bccb-cd67339c11ba','ng/mL','ng/mL',1,'carepath-units-v1','SI identity for explicitly listed concept/unit',true);
INSERT INTO canonical_medical_concept(id,canonical_name,canonical_unit,mapping_source,vocabulary_version) VALUES('4bc0ebb5-ca0f-50ea-a7b8-4cc8807be529','Vitamin B12','pg/mL','CarePath demo terminology v1; MedlinePlus lab test names; no specimen/method equivalence claim','carepath-demo-v1');
INSERT INTO concept_alias(id,concept_id,normalized_alias,mapping_confidence,reviewed,source) VALUES('94fcda40-df11-53e3-b0e4-6df5e776db66','4bc0ebb5-ca0f-50ea-a7b8-4cc8807be529','vitamin b-12',1,true,'carepath-demo-v1; exact lexical alias only');
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('7bc93cab-cbdc-5683-9194-909dca1c54fb','4bc0ebb5-ca0f-50ea-a7b8-4cc8807be529','pg/mL','pg/mL',1,'carepath-units-v1','SI identity for explicitly listed concept/unit',true);
INSERT INTO canonical_medical_concept(id,canonical_name,canonical_unit,mapping_source,vocabulary_version) VALUES('3fc88f23-88b7-5009-8a50-d1ff1beb7307','Glucose','mmol/L','CarePath demo terminology v1; MedlinePlus lab test names; no specimen/method equivalence claim','carepath-demo-v1');
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('0b4fa6cd-017a-5a8b-ada4-394ab0297593','3fc88f23-88b7-5009-8a50-d1ff1beb7307','mmol/L','mmol/L',1,'carepath-units-v1','SI identity for explicitly listed concept/unit',true);
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('4ac20092-82cf-5b59-aa41-1df028faa421','3fc88f23-88b7-5009-8a50-d1ff1beb7307','mg/dL','mmol/L',0.05551,'carepath-units-v1','https://www.mayocliniclabs.com/order-tests/si-unit-conversion.html; glucose molecular conversion only',true);
INSERT INTO canonical_medical_concept(id,canonical_name,canonical_unit,mapping_source,vocabulary_version) VALUES('85184c21-ef7d-5484-947e-ac819717cb63','HbA1c','%','CarePath demo terminology v1; MedlinePlus lab test names; no specimen/method equivalence claim','carepath-demo-v1');
INSERT INTO concept_alias(id,concept_id,normalized_alias,mapping_confidence,reviewed,source) VALUES('e474b8d6-0704-5b7f-9550-31d53569b69d','85184c21-ef7d-5484-947e-ac819717cb63','hemoglobin a1c',1,true,'carepath-demo-v1; exact lexical alias only');
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('b0aa6783-f3ab-5e80-a43d-6233a6f45349','85184c21-ef7d-5484-947e-ac819717cb63','%','%',1,'carepath-units-v1','SI identity for explicitly listed concept/unit',true);
INSERT INTO canonical_medical_concept(id,canonical_name,canonical_unit,mapping_source,vocabulary_version) VALUES('4d4069ac-0ac7-5555-8d4b-bfbefd08b5e2','Creatinine','mg/dL','CarePath demo terminology v1; MedlinePlus lab test names; no specimen/method equivalence claim','carepath-demo-v1');
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('085c8496-f7a3-58a4-aa1f-249f9be12fc8','4d4069ac-0ac7-5555-8d4b-bfbefd08b5e2','mg/dL','mg/dL',1,'carepath-units-v1','SI identity for explicitly listed concept/unit',true);
INSERT INTO canonical_medical_concept(id,canonical_name,canonical_unit,mapping_source,vocabulary_version) VALUES('862e14af-0c27-53d3-baa8-0ab6daf19076','Urea','mg/dL','CarePath demo terminology v1; MedlinePlus lab test names; no specimen/method equivalence claim','carepath-demo-v1');
INSERT INTO concept_alias(id,concept_id,normalized_alias,mapping_confidence,reviewed,source) VALUES('5b1b9d4a-240a-5ba2-b145-0d4f4839d7a6','862e14af-0c27-53d3-baa8-0ab6daf19076','urea/bun',1,true,'carepath-demo-v1; exact lexical alias only');
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('c221209e-2212-51ec-b9d7-5e4a0e3cbfe6','862e14af-0c27-53d3-baa8-0ab6daf19076','mg/dL','mg/dL',1,'carepath-units-v1','SI identity for explicitly listed concept/unit',true);
INSERT INTO canonical_medical_concept(id,canonical_name,canonical_unit,mapping_source,vocabulary_version) VALUES('a7874287-53ed-5c70-9867-bba14b6eef63','Blood urea nitrogen','mg/dL','CarePath demo terminology v1; MedlinePlus lab test names; no specimen/method equivalence claim','carepath-demo-v1');
INSERT INTO concept_alias(id,concept_id,normalized_alias,mapping_confidence,reviewed,source) VALUES('11fb7f4b-1788-59ab-963f-59e3144a0a40','a7874287-53ed-5c70-9867-bba14b6eef63','bun',1,true,'carepath-demo-v1; exact lexical alias only');
INSERT INTO concept_alias(id,concept_id,normalized_alias,mapping_confidence,reviewed,source) VALUES('f2d7de1a-09a4-51fe-b851-e903e7a2eee9','a7874287-53ed-5c70-9867-bba14b6eef63','urea/bun',1,true,'carepath-demo-v1; exact lexical alias only');
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('c7917635-96e9-5a5b-b53f-b908b495b91c','a7874287-53ed-5c70-9867-bba14b6eef63','mg/dL','mg/dL',1,'carepath-units-v1','SI identity for explicitly listed concept/unit',true);
INSERT INTO canonical_medical_concept(id,canonical_name,canonical_unit,mapping_source,vocabulary_version) VALUES('94d0edfd-51c4-559a-8a16-26668e6075a4','Sodium','mmol/L','CarePath demo terminology v1; MedlinePlus lab test names; no specimen/method equivalence claim','carepath-demo-v1');
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('fab11ec5-188d-5b61-815b-d2e4fdde2429','94d0edfd-51c4-559a-8a16-26668e6075a4','mmol/L','mmol/L',1,'carepath-units-v1','SI identity for explicitly listed concept/unit',true);
INSERT INTO canonical_medical_concept(id,canonical_name,canonical_unit,mapping_source,vocabulary_version) VALUES('2bba79bd-cbbe-527d-bf8c-6ae4536fb2b9','Potassium','mmol/L','CarePath demo terminology v1; MedlinePlus lab test names; no specimen/method equivalence claim','carepath-demo-v1');
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('18505b44-820f-53c8-90a5-0da4d5e791b6','2bba79bd-cbbe-527d-bf8c-6ae4536fb2b9','mmol/L','mmol/L',1,'carepath-units-v1','SI identity for explicitly listed concept/unit',true);
INSERT INTO canonical_medical_concept(id,canonical_name,canonical_unit,mapping_source,vocabulary_version) VALUES('99217703-5162-5907-b19d-9e251d3a89dd','Total Cholesterol','mg/dL','CarePath demo terminology v1; MedlinePlus lab test names; no specimen/method equivalence claim','carepath-demo-v1');
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('8c89a811-8e42-5ebb-b77a-27cec2a367e1','99217703-5162-5907-b19d-9e251d3a89dd','mg/dL','mg/dL',1,'carepath-units-v1','SI identity for explicitly listed concept/unit',true);
INSERT INTO canonical_medical_concept(id,canonical_name,canonical_unit,mapping_source,vocabulary_version) VALUES('20aa0fdf-d4a9-567e-9745-1e948075bbe0','LDL Cholesterol','mg/dL','CarePath demo terminology v1; MedlinePlus lab test names; no specimen/method equivalence claim','carepath-demo-v1');
INSERT INTO concept_alias(id,concept_id,normalized_alias,mapping_confidence,reviewed,source) VALUES('a788ec2a-0e21-5b1d-8d3f-c65574b53f15','20aa0fdf-d4a9-567e-9745-1e948075bbe0','ldl-c',1,true,'carepath-demo-v1; exact lexical alias only');
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('4f117a76-a00b-5d19-bc24-726514d728f1','20aa0fdf-d4a9-567e-9745-1e948075bbe0','mg/dL','mg/dL',1,'carepath-units-v1','SI identity for explicitly listed concept/unit',true);
INSERT INTO canonical_medical_concept(id,canonical_name,canonical_unit,mapping_source,vocabulary_version) VALUES('493c1c25-6017-55d8-8d10-5203e36f517f','HDL Cholesterol','mg/dL','CarePath demo terminology v1; MedlinePlus lab test names; no specimen/method equivalence claim','carepath-demo-v1');
INSERT INTO concept_alias(id,concept_id,normalized_alias,mapping_confidence,reviewed,source) VALUES('2cb15227-92a9-5c82-8e67-bbcd35aa1a14','493c1c25-6017-55d8-8d10-5203e36f517f','hdl-c',1,true,'carepath-demo-v1; exact lexical alias only');
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('fedd9782-011f-5160-ab45-dbb184084318','493c1c25-6017-55d8-8d10-5203e36f517f','mg/dL','mg/dL',1,'carepath-units-v1','SI identity for explicitly listed concept/unit',true);
INSERT INTO canonical_medical_concept(id,canonical_name,canonical_unit,mapping_source,vocabulary_version) VALUES('9dc5252b-62d7-52bf-9d8f-9575b89c52b8','Triglycerides','mg/dL','CarePath demo terminology v1; MedlinePlus lab test names; no specimen/method equivalence claim','carepath-demo-v1');
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('81e7811a-fe82-5f79-aed2-17391a3c7d1d','9dc5252b-62d7-52bf-9d8f-9575b89c52b8','mg/dL','mg/dL',1,'carepath-units-v1','SI identity for explicitly listed concept/unit',true);
INSERT INTO canonical_medical_concept(id,canonical_name,canonical_unit,mapping_source,vocabulary_version) VALUES('5e53bf9d-67df-59f7-87eb-18bcc203f2f8','TSH','mIU/L','CarePath demo terminology v1; MedlinePlus lab test names; no specimen/method equivalence claim','carepath-demo-v1');
INSERT INTO concept_alias(id,concept_id,normalized_alias,mapping_confidence,reviewed,source) VALUES('007616b7-9181-5cc2-8cff-8876c4d17b69','5e53bf9d-67df-59f7-87eb-18bcc203f2f8','thyroid stimulating hormone',1,true,'carepath-demo-v1; exact lexical alias only');
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('a3cf6835-b8e0-551a-9717-61b30e037d96','5e53bf9d-67df-59f7-87eb-18bcc203f2f8','mIU/L','mIU/L',1,'carepath-units-v1','SI identity for explicitly listed concept/unit',true);
INSERT INTO canonical_medical_concept(id,canonical_name,canonical_unit,mapping_source,vocabulary_version) VALUES('5eb67839-1e9c-5b2e-a853-a3cb7815f512','Ferritin','ng/mL','CarePath demo terminology v1; MedlinePlus lab test names; no specimen/method equivalence claim','carepath-demo-v1');
INSERT INTO unit_conversion_rule(id,concept_id,source_unit,target_unit,multiplier,rule_version,source_citation,reviewed) VALUES('c4137806-6552-509f-8ef0-b6e69516a583','5eb67839-1e9c-5b2e-a853-a3cb7815f512','ng/mL','ng/mL',1,'carepath-units-v1','SI identity for explicitly listed concept/unit',true);

-- Read-only longitudinal projections; no persisted trend/derived fact table.
-- Match owner + report scans and the trusted-view source/history existence checks.
CREATE INDEX medical_observation_owner_document_idx ON medical_observation(owner_id,document_id,concept_id,observed_date,id);
CREATE INDEX observation_source_observation_owner_idx ON observation_source(observation_id,owner_id);
CREATE INDEX candidate_verification_observation_owner_idx ON candidate_verification(observation_id,owner_id,candidate_id);

CREATE TABLE saved_question (
id uuid DEFAULT random_uuid() PRIMARY KEY,
owner_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
question_text text NOT NULL CHECK (length(question_text) BETWEEN 1 AND 4000),
origin varchar(20) NOT NULL DEFAULT 'USER' CHECK (origin IN ('USER','RULE','AI_DRAFT')),
observation_id uuid,
status varchar(16) NOT NULL DEFAULT 'SAVED' CHECK (status IN ('DRAFT','SAVED','ARCHIVED')),
created_at timestamp with time zone NOT NULL DEFAULT now(),
updated_at timestamp with time zone NOT NULL DEFAULT now(),
version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
UNIQUE (id, owner_id),
FOREIGN KEY (observation_id, owner_id) REFERENCES medical_observation(id, owner_id) ON DELETE RESTRICT
);

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

-- Phase 8 care tables (test-only H2 equivalents)
CREATE TABLE symptom (
id uuid DEFAULT random_uuid() PRIMARY KEY,
owner_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
name varchar(160) NOT NULL,
start_date date NOT NULL,
severity smallint NOT NULL CHECK (severity BETWEEN 1 AND 10),
frequency varchar(160),
notes text,
resolved_date date,
CHECK (resolved_date IS NULL OR resolved_date >= start_date),
created_at timestamp with time zone NOT NULL DEFAULT now(),
updated_at timestamp with time zone NOT NULL DEFAULT now(),
version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
UNIQUE (id, owner_id)
);
CREATE TABLE appointment (
id uuid DEFAULT random_uuid() PRIMARY KEY,
owner_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
provider_name varchar(255) NOT NULL,
specialty varchar(160),
starts_at timestamp with time zone NOT NULL,
time_zone varchar(80) NOT NULL,
location text,
notes text,
status varchar(20) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED','COMPLETED','CANCELLED','MISSED')),
follow_up_date date,
created_at timestamp with time zone NOT NULL DEFAULT now(),
updated_at timestamp with time zone NOT NULL DEFAULT now(),
version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
UNIQUE (id, owner_id)
);
CREATE TABLE appointment_document (
id uuid DEFAULT random_uuid() PRIMARY KEY,
owner_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
appointment_id uuid NOT NULL,
document_id uuid NOT NULL,
UNIQUE (appointment_id, document_id),
created_at timestamp with time zone NOT NULL DEFAULT now(),
updated_at timestamp with time zone NOT NULL DEFAULT now(),
version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
UNIQUE (id, owner_id),
FOREIGN KEY (appointment_id, owner_id) REFERENCES appointment(id, owner_id) ON DELETE CASCADE,
FOREIGN KEY (document_id, owner_id) REFERENCES medical_document(id, owner_id) ON DELETE CASCADE
);
CREATE TABLE appointment_symptom (
id uuid DEFAULT random_uuid() PRIMARY KEY,
owner_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
appointment_id uuid NOT NULL,
symptom_id uuid NOT NULL,
UNIQUE (appointment_id, symptom_id),
created_at timestamp with time zone NOT NULL DEFAULT now(),
updated_at timestamp with time zone NOT NULL DEFAULT now(),
version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
UNIQUE (id, owner_id),
FOREIGN KEY (appointment_id, owner_id) REFERENCES appointment(id, owner_id) ON DELETE CASCADE,
FOREIGN KEY (symptom_id, owner_id) REFERENCES symptom(id, owner_id) ON DELETE CASCADE
);
CREATE TABLE follow_up (
id uuid DEFAULT random_uuid() PRIMARY KEY,
owner_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
document_id uuid,
page_id uuid,
appointment_id uuid,
source_text text,
anchor_date date,
interval_value integer CHECK (interval_value > 0),
interval_unit varchar(12) CHECK (interval_unit IN ('DAY','WEEK','MONTH')),
suggested_date date,
confirmed_date date,
confidence numeric(5,4) CHECK (confidence BETWEEN 0 AND 1),
status varchar(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','CONFIRMED','EDITED','IGNORED','COMPLETED')),
CHECK (document_id IS NOT NULL OR appointment_id IS NOT NULL),
CHECK (status NOT IN ('CONFIRMED','EDITED','COMPLETED') OR confirmed_date IS NOT NULL),
created_at timestamp with time zone NOT NULL DEFAULT now(),
updated_at timestamp with time zone NOT NULL DEFAULT now(),
version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
UNIQUE (id, owner_id),
FOREIGN KEY (document_id, owner_id) REFERENCES medical_document(id, owner_id) ON DELETE CASCADE,
FOREIGN KEY (page_id, owner_id) REFERENCES document_page(id, owner_id) ON DELETE CASCADE,
FOREIGN KEY (appointment_id, owner_id) REFERENCES appointment(id, owner_id) ON DELETE CASCADE
);
CREATE TABLE reminder (
id uuid DEFAULT random_uuid() PRIMARY KEY,
owner_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
appointment_id uuid,
follow_up_id uuid,
offset_minutes integer NOT NULL DEFAULT 1440 CHECK (offset_minutes >= 0),
scheduled_at timestamp with time zone NOT NULL,
channel varchar(12) NOT NULL DEFAULT 'IN_APP' CHECK (channel IN ('IN_APP','EMAIL')),
status varchar(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','PROCESSING','DELIVERED','FAILED','CANCELLED')),
idempotency_key uuid NOT NULL UNIQUE,
attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
next_attempt_at timestamp with time zone NOT NULL DEFAULT now(),
locked_until timestamp with time zone,
delivered_at timestamp with time zone,
CHECK ((appointment_id IS NULL) <> (follow_up_id IS NULL)),
created_at timestamp with time zone NOT NULL DEFAULT now(),
updated_at timestamp with time zone NOT NULL DEFAULT now(),
version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
UNIQUE (id, owner_id),
FOREIGN KEY (appointment_id, owner_id) REFERENCES appointment(id, owner_id) ON DELETE CASCADE,
FOREIGN KEY (follow_up_id, owner_id) REFERENCES follow_up(id, owner_id) ON DELETE CASCADE
);
CREATE TABLE notification (
id uuid DEFAULT random_uuid() PRIMARY KEY,
owner_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
reminder_id uuid,
kind varchar(40) NOT NULL,
title varchar(160) NOT NULL,
body text NOT NULL,
read_at timestamp with time zone,
UNIQUE (reminder_id),
created_at timestamp with time zone NOT NULL DEFAULT now(),
updated_at timestamp with time zone NOT NULL DEFAULT now(),
version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
UNIQUE (id, owner_id),
FOREIGN KEY (reminder_id, owner_id) REFERENCES reminder(id, owner_id) ON DELETE CASCADE
);
ALTER TABLE symptom ADD COLUMN started_at timestamp with time zone NOT NULL;
ALTER TABLE symptom ADD COLUMN resolved_at timestamp with time zone;
ALTER TABLE appointment ADD COLUMN phone varchar(40);
ALTER TABLE appointment ADD COLUMN external_url varchar(500);
CREATE TABLE appointment_question (
 appointment_id uuid NOT NULL, owner_id uuid NOT NULL, question_id uuid NOT NULL,
 PRIMARY KEY(appointment_id,question_id),
 FOREIGN KEY(appointment_id,owner_id) REFERENCES appointment(id,owner_id) ON DELETE CASCADE,
 FOREIGN KEY(question_id,owner_id) REFERENCES saved_question(id,owner_id) ON DELETE CASCADE
);
ALTER TABLE follow_up ADD COLUMN instruction_key varchar(64);
ALTER TABLE follow_up ADD COLUMN confirmed_at_time timestamp with time zone;
ALTER TABLE follow_up ADD COLUMN time_zone varchar(80);
CREATE UNIQUE INDEX follow_up_instruction_unique ON follow_up(document_id,instruction_key);
CREATE UNIQUE INDEX reminder_appointment_offset ON reminder(appointment_id,offset_minutes);
CREATE UNIQUE INDEX reminder_follow_up_offset ON reminder(follow_up_id,offset_minutes);

ALTER TABLE document_page ADD CONSTRAINT care_page_document_owner UNIQUE(id,document_id,owner_id);
ALTER TABLE follow_up ADD CONSTRAINT care_follow_source FOREIGN KEY(page_id,document_id,owner_id) REFERENCES document_page(id,document_id,owner_id) ON DELETE CASCADE;
ALTER TABLE reminder ADD CONSTRAINT care_reminder_offset CHECK(offset_minutes IN (0,60,1440,2880,10080));
ALTER TABLE symptom ADD CONSTRAINT care_symptom_frequency CHECK(frequency IS NULL OR frequency IN ('ONCE','OCCASIONAL','DAILY','CONSTANT'));

-- Phase 9 test schema. H2 source nulling uses single-column FKs; production retains composite owner FKs.
CREATE TABLE visit_pack (
id uuid DEFAULT random_uuid() PRIMARY KEY, owner_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
title varchar(160) NOT NULL, reason_for_visit text NOT NULL, appointment_id uuid REFERENCES appointment(id) ON DELETE SET NULL,
status varchar(16) NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','GENERATED','READY','INVALIDATED','FAILED')),
revision integer NOT NULL DEFAULT 1, version bigint NOT NULL DEFAULT 0,
created_at timestamp with time zone NOT NULL DEFAULT now(), updated_at timestamp with time zone NOT NULL DEFAULT now(),
generated_at timestamp with time zone, previous_pack_id uuid UNIQUE REFERENCES visit_pack(id) ON DELETE SET NULL,
appointment_selected boolean NOT NULL DEFAULT false, pack_date date, renderer_version varchar(30), UNIQUE(id,owner_id));
CREATE TABLE visit_pack_item (
id uuid PRIMARY KEY,owner_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,visit_pack_id uuid NOT NULL,
position integer NOT NULL, item_type varchar(24) NOT NULL, include_notes boolean NOT NULL DEFAULT false, question_override varchar(1000),
document_id uuid REFERENCES medical_document(id) ON DELETE SET NULL,
observation_id uuid REFERENCES medical_observation(id) ON DELETE SET NULL,
other_observation_id uuid REFERENCES medical_observation(id) ON DELETE SET NULL,
symptom_id uuid REFERENCES symptom(id) ON DELETE SET NULL,question_id uuid REFERENCES saved_question(id) ON DELETE SET NULL,
appointment_id uuid REFERENCES appointment(id) ON DELETE SET NULL,follow_up_id uuid REFERENCES follow_up(id) ON DELETE SET NULL,
UNIQUE(visit_pack_id,position), FOREIGN KEY(visit_pack_id,owner_id) REFERENCES visit_pack(id,owner_id) ON DELETE CASCADE);
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

ALTER TABLE visit_pack ADD CONSTRAINT visit_pack_revision_owner_uq UNIQUE(id,owner_id,revision);
CREATE TABLE share_token (
id uuid PRIMARY KEY, owner_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
visit_pack_id uuid NOT NULL, pack_revision integer NOT NULL CHECK(pack_revision>0),
token_hash char(64) NOT NULL UNIQUE, created_at timestamp with time zone NOT NULL,
expires_at timestamp with time zone NOT NULL, revoked_at timestamp with time zone,
last_accessed_at timestamp with time zone, access_count bigint NOT NULL DEFAULT 0 CHECK(access_count>=0),
updated_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP, version bigint NOT NULL DEFAULT 0,
CHECK(expires_at>created_at), CHECK(expires_at<=DATEADD('HOUR',24,created_at)),
FOREIGN KEY(visit_pack_id,owner_id,pack_revision) REFERENCES visit_pack(id,owner_id,revision) ON DELETE CASCADE);
