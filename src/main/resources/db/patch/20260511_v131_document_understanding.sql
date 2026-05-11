ALTER TABLE document_summary
    ADD COLUMN IF NOT EXISTS knowledge_base_id BIGINT NULL AFTER document_id,
    ADD COLUMN IF NOT EXISTS model_name VARCHAR(128) NULL AFTER summary,
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) NULL AFTER model_name,
    ADD COLUMN IF NOT EXISTS error_message VARCHAR(1024) NULL AFTER status,
    ADD COLUMN IF NOT EXISTS generated_at DATETIME NULL AFTER error_message;

ALTER TABLE knowledge_base_summary
    ADD COLUMN IF NOT EXISTS covered_document_count INT NULL AFTER summary,
    ADD COLUMN IF NOT EXISTS model_name VARCHAR(128) NULL AFTER covered_document_count,
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) NULL AFTER model_name,
    ADD COLUMN IF NOT EXISTS error_message VARCHAR(1024) NULL AFTER status,
    ADD COLUMN IF NOT EXISTS generated_at DATETIME NULL AFTER error_message;

CREATE TABLE IF NOT EXISTS keyword_extract_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    knowledge_base_id BIGINT NULL,
    user_id BIGINT NOT NULL,
    keyword VARCHAR(128) NOT NULL,
    weight DOUBLE NULL,
    model_name VARCHAR(128) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIT NOT NULL DEFAULT b'0',
    INDEX idx_keyword_target (target_type, target_id),
    INDEX idx_keyword_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
