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
 created_at timestamptz NOT NULL DEFAULT now(),
 FOREIGN KEY(page_id,extraction_id,document_id,owner_id) REFERENCES document_page(id,extraction_id,document_id,owner_id) ON DELETE CASCADE,
 CHECK(reference_lower IS NULL OR reference_upper IS NULL OR reference_lower<=reference_upper)
);
CREATE INDEX extraction_candidate_owner_document_idx ON extraction_candidate(owner_id,document_id,extraction_id);
