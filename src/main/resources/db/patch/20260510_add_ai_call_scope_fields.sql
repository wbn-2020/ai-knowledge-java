ALTER TABLE ai_call_log
    ADD COLUMN IF NOT EXISTS knowledge_base_id BIGINT NULL AFTER user_id,
    ADD COLUMN IF NOT EXISTS session_id BIGINT NULL AFTER knowledge_base_id;
