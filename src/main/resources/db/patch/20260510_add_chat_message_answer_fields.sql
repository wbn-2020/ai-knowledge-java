ALTER TABLE chat_message
    ADD COLUMN IF NOT EXISTS answer_type VARCHAR(32) NULL AFTER token_count,
    ADD COLUMN IF NOT EXISTS can_use_general_answer BIT NOT NULL DEFAULT b'0' AFTER answer_type,
    ADD COLUMN IF NOT EXISTS references_json LONGTEXT NULL AFTER can_use_general_answer;
