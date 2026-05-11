ALTER TABLE document_process_task
    ADD COLUMN IF NOT EXISTS document_name_snapshot VARCHAR(255) NULL AFTER task_type,
    ADD COLUMN IF NOT EXISTS logs_json LONGTEXT NULL AFTER fail_reason,
    ADD COLUMN IF NOT EXISTS started_at DATETIME NULL AFTER logs_json,
    ADD COLUMN IF NOT EXISTS finished_at DATETIME NULL AFTER started_at,
    ADD COLUMN IF NOT EXISTS duration_ms BIGINT NULL AFTER finished_at;
