-- Optional database storage for originals; local storage remains the default.
-- No FK: cleanup intents must remain valid after document metadata is removed.
CREATE TABLE document_blob (
    storage_key varchar(32) PRIMARY KEY CHECK (storage_key ~ '^[0-9a-f]{32}$'),
    contents bytea NOT NULL CHECK (octet_length(contents) BETWEEN 1 AND 20971520)
);
