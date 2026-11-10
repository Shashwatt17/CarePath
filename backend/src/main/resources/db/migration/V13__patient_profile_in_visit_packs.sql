-- Explicit profile selection uses existing immutable snapshot storage.
ALTER TABLE visit_pack_item DROP CONSTRAINT visit_pack_item_item_type_check;
ALTER TABLE visit_pack_item ADD CONSTRAINT visit_pack_item_item_type_check CHECK(item_type IN ('PROFILE','SYMPTOM','OBSERVATION','CHANGE','DOCUMENT','APPOINTMENT','FOLLOW_UP','SAVED_QUESTION','MANUAL_QUESTION'));
ALTER TABLE visit_pack_snapshot_item DROP CONSTRAINT visit_pack_snapshot_item_item_type_check;
ALTER TABLE visit_pack_snapshot_item ADD CONSTRAINT visit_pack_snapshot_item_item_type_check CHECK(item_type IN ('PROFILE','SYMPTOM','OBSERVATION','CHANGE','DOCUMENT','APPOINTMENT','FOLLOW_UP','SAVED_QUESTION','MANUAL_QUESTION'));
