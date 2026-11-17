-- Separate model proposals: never substitute them into extraction or verified observations.
CREATE TABLE vision_page_proposal (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    document_id UUID NOT NULL,
    extraction_id UUID NOT NULL,
    page_number INTEGER NOT NULL CHECK (page_number > 0),
    state VARCHAR(16) NOT NULL CHECK (state IN ('REQUESTED','SUCCEEDED','FAILED')),
    provider VARCHAR(32) NOT NULL,
    model VARCHAR(100) NOT NULL,
    consent_version VARCHAR(40) NOT NULL,
    policy_version VARCHAR(40) NOT NULL,
    result_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_id,owner_id) REFERENCES medical_document(id,owner_id) ON DELETE CASCADE,
    FOREIGN KEY (extraction_id,document_id,owner_id) REFERENCES document_extraction(id,document_id,owner_id) ON DELETE CASCADE,
    UNIQUE (extraction_id,page_number)
);
CREATE INDEX vision_owner_created ON vision_page_proposal(owner_id,created_at);

CREATE INDEX vision_audit_budget ON audit_event(action,created_at);
