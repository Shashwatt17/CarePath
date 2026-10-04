-- Existing digest-only share_token table is reused. Bind the historical revision as well as owner.
ALTER TABLE visit_pack ADD CONSTRAINT visit_pack_revision_owner_uq UNIQUE(id,owner_id,revision);
ALTER TABLE share_token ADD CONSTRAINT share_token_revision_fk
 FOREIGN KEY(visit_pack_id,owner_id,pack_revision) REFERENCES visit_pack(id,owner_id,revision) ON DELETE CASCADE;
CREATE INDEX share_token_pack_owner_idx ON share_token(visit_pack_id,owner_id);
