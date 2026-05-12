ALTER TABLE chat_session
    ADD COLUMN IF NOT EXISTS scope_type VARCHAR(32) NULL AFTER knowledge_base_id,
    ADD COLUMN IF NOT EXISTS document_id BIGINT NULL AFTER scope_type;
