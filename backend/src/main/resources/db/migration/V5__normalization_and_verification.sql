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
 occurred_at timestamptz NOT NULL DEFAULT now(),
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
