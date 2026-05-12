-- Compatible schema patch for log/feedback tables when historical patches were not fully applied.

-- operation_log: module/path/result/failure_reason
SET @op_has_module := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'operation_log' AND COLUMN_NAME = 'module'
);
SET @op_sql_module := IF(@op_has_module = 0,
    'ALTER TABLE operation_log ADD COLUMN module VARCHAR(128) NULL AFTER action',
    'SELECT 1');
PREPARE stmt_op_module FROM @op_sql_module;
EXECUTE stmt_op_module;
DEALLOCATE PREPARE stmt_op_module;

SET @op_has_path := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'operation_log' AND COLUMN_NAME = 'path'
);
SET @op_sql_path := IF(@op_has_path = 0,
    'ALTER TABLE operation_log ADD COLUMN path VARCHAR(512) NULL AFTER module',
    'SELECT 1');
PREPARE stmt_op_path FROM @op_sql_path;
EXECUTE stmt_op_path;
DEALLOCATE PREPARE stmt_op_path;

SET @op_has_result := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'operation_log' AND COLUMN_NAME = 'result'
);
SET @op_sql_result := IF(@op_has_result = 0,
    'ALTER TABLE operation_log ADD COLUMN result VARCHAR(32) NULL AFTER path',
    'SELECT 1');
PREPARE stmt_op_result FROM @op_sql_result;
EXECUTE stmt_op_result;
DEALLOCATE PREPARE stmt_op_result;

SET @op_has_failure_reason := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'operation_log' AND COLUMN_NAME = 'failure_reason'
);
SET @op_sql_failure_reason := IF(@op_has_failure_reason = 0,
    'ALTER TABLE operation_log ADD COLUMN failure_reason VARCHAR(1024) NULL AFTER result',
    'SELECT 1');
PREPARE stmt_op_failure_reason FROM @op_sql_failure_reason;
EXECUTE stmt_op_failure_reason;
DEALLOCATE PREPARE stmt_op_failure_reason;

-- login_log: ip/user_agent/failure_reason
SET @login_has_ip := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'ip'
);
SET @login_sql_ip := IF(@login_has_ip = 0,
    'ALTER TABLE login_log ADD COLUMN ip VARCHAR(64) NULL AFTER account',
    'SELECT 1');
PREPARE stmt_login_ip FROM @login_sql_ip;
EXECUTE stmt_login_ip;
DEALLOCATE PREPARE stmt_login_ip;

SET @login_has_user_agent := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'user_agent'
);
SET @login_sql_user_agent := IF(@login_has_user_agent = 0,
    'ALTER TABLE login_log ADD COLUMN user_agent VARCHAR(512) NULL AFTER ip',
    'SELECT 1');
PREPARE stmt_login_user_agent FROM @login_sql_user_agent;
EXECUTE stmt_login_user_agent;
DEALLOCATE PREPARE stmt_login_user_agent;

SET @login_has_failure_reason := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'failure_reason'
);
SET @login_sql_failure_reason := IF(@login_has_failure_reason = 0,
    'ALTER TABLE login_log ADD COLUMN failure_reason VARCHAR(255) NULL AFTER message',
    'SELECT 1');
PREPARE stmt_login_failure_reason FROM @login_sql_failure_reason;
EXECUTE stmt_login_failure_reason;
DEALLOCATE PREPARE stmt_login_failure_reason;

-- ai_call_log: key v1.2+ fields
SET @ai_has_model := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_call_log' AND COLUMN_NAME = 'model'
);
SET @ai_sql_model := IF(@ai_has_model = 0,
    'ALTER TABLE ai_call_log ADD COLUMN model VARCHAR(128) NULL AFTER user_id',
    'SELECT 1');
PREPARE stmt_ai_model FROM @ai_sql_model;
EXECUTE stmt_ai_model;
DEALLOCATE PREPARE stmt_ai_model;

SET @ai_has_model_type := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_call_log' AND COLUMN_NAME = 'model_type'
);
SET @ai_sql_model_type := IF(@ai_has_model_type = 0,
    'ALTER TABLE ai_call_log ADD COLUMN model_type VARCHAR(64) NULL AFTER model_name',
    'SELECT 1');
PREPARE stmt_ai_model_type FROM @ai_sql_model_type;
EXECUTE stmt_ai_model_type;
DEALLOCATE PREPARE stmt_ai_model_type;

SET @ai_has_provider := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_call_log' AND COLUMN_NAME = 'provider'
);
SET @ai_sql_provider := IF(@ai_has_provider = 0,
    'ALTER TABLE ai_call_log ADD COLUMN provider VARCHAR(64) NULL AFTER model_type',
    'SELECT 1');
PREPARE stmt_ai_provider FROM @ai_sql_provider;
EXECUTE stmt_ai_provider;
DEALLOCATE PREPARE stmt_ai_provider;

SET @ai_has_prompt_tokens := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_call_log' AND COLUMN_NAME = 'prompt_tokens'
);
SET @ai_sql_prompt_tokens := IF(@ai_has_prompt_tokens = 0,
    'ALTER TABLE ai_call_log ADD COLUMN prompt_tokens INT NULL AFTER call_type',
    'SELECT 1');
PREPARE stmt_ai_prompt_tokens FROM @ai_sql_prompt_tokens;
EXECUTE stmt_ai_prompt_tokens;
DEALLOCATE PREPARE stmt_ai_prompt_tokens;

SET @ai_has_completion_tokens := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_call_log' AND COLUMN_NAME = 'completion_tokens'
);
SET @ai_sql_completion_tokens := IF(@ai_has_completion_tokens = 0,
    'ALTER TABLE ai_call_log ADD COLUMN completion_tokens INT NULL AFTER prompt_tokens',
    'SELECT 1');
PREPARE stmt_ai_completion_tokens FROM @ai_sql_completion_tokens;
EXECUTE stmt_ai_completion_tokens;
DEALLOCATE PREPARE stmt_ai_completion_tokens;

SET @ai_has_total_tokens := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_call_log' AND COLUMN_NAME = 'total_tokens'
);
SET @ai_sql_total_tokens := IF(@ai_has_total_tokens = 0,
    'ALTER TABLE ai_call_log ADD COLUMN total_tokens INT NULL AFTER completion_tokens',
    'SELECT 1');
PREPARE stmt_ai_total_tokens FROM @ai_sql_total_tokens;
EXECUTE stmt_ai_total_tokens;
DEALLOCATE PREPARE stmt_ai_total_tokens;

SET @ai_has_username := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_call_log' AND COLUMN_NAME = 'username'
);
SET @ai_sql_username := IF(@ai_has_username = 0,
    'ALTER TABLE ai_call_log ADD COLUMN username VARCHAR(64) NULL AFTER session_id',
    'SELECT 1');
PREPARE stmt_ai_username FROM @ai_sql_username;
EXECUTE stmt_ai_username;
DEALLOCATE PREPARE stmt_ai_username;

SET @ai_has_question := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_call_log' AND COLUMN_NAME = 'question'
);
SET @ai_sql_question := IF(@ai_has_question = 0,
    'ALTER TABLE ai_call_log ADD COLUMN question VARCHAR(1000) NULL AFTER username',
    'SELECT 1');
PREPARE stmt_ai_question FROM @ai_sql_question;
EXECUTE stmt_ai_question;
DEALLOCATE PREPARE stmt_ai_question;

SET @ai_has_retrieve_count := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_call_log' AND COLUMN_NAME = 'retrieve_count'
);
SET @ai_sql_retrieve_count := IF(@ai_has_retrieve_count = 0,
    'ALTER TABLE ai_call_log ADD COLUMN retrieve_count INT NULL AFTER question',
    'SELECT 1');
PREPARE stmt_ai_retrieve_count FROM @ai_sql_retrieve_count;
EXECUTE stmt_ai_retrieve_count;
DEALLOCATE PREPARE stmt_ai_retrieve_count;

SET @ai_has_effective_retrieve_count := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_call_log' AND COLUMN_NAME = 'effective_retrieve_count'
);
SET @ai_sql_effective_retrieve_count := IF(@ai_has_effective_retrieve_count = 0,
    'ALTER TABLE ai_call_log ADD COLUMN effective_retrieve_count INT NULL AFTER retrieve_count',
    'SELECT 1');
PREPARE stmt_ai_effective_retrieve_count FROM @ai_sql_effective_retrieve_count;
EXECUTE stmt_ai_effective_retrieve_count;
DEALLOCATE PREPARE stmt_ai_effective_retrieve_count;

SET @ai_has_top_k := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_call_log' AND COLUMN_NAME = 'top_k'
);
SET @ai_sql_top_k := IF(@ai_has_top_k = 0,
    'ALTER TABLE ai_call_log ADD COLUMN top_k INT NULL AFTER effective_retrieve_count',
    'SELECT 1');
PREPARE stmt_ai_top_k FROM @ai_sql_top_k;
EXECUTE stmt_ai_top_k;
DEALLOCATE PREPARE stmt_ai_top_k;

SET @ai_has_similarity_threshold := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_call_log' AND COLUMN_NAME = 'similarity_threshold'
);
SET @ai_sql_similarity_threshold := IF(@ai_has_similarity_threshold = 0,
    'ALTER TABLE ai_call_log ADD COLUMN similarity_threshold DOUBLE NULL AFTER top_k',
    'SELECT 1');
PREPARE stmt_ai_similarity_threshold FROM @ai_sql_similarity_threshold;
EXECUTE stmt_ai_similarity_threshold;
DEALLOCATE PREPARE stmt_ai_similarity_threshold;

SET @ai_has_max_similarity_score := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_call_log' AND COLUMN_NAME = 'max_similarity_score'
);
SET @ai_sql_max_similarity_score := IF(@ai_has_max_similarity_score = 0,
    'ALTER TABLE ai_call_log ADD COLUMN max_similarity_score DOUBLE NULL AFTER similarity_threshold',
    'SELECT 1');
PREPARE stmt_ai_max_similarity_score FROM @ai_sql_max_similarity_score;
EXECUTE stmt_ai_max_similarity_score;
DEALLOCATE PREPARE stmt_ai_max_similarity_score;

SET @ai_has_llm_called := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_call_log' AND COLUMN_NAME = 'llm_called'
);
SET @ai_sql_llm_called := IF(@ai_has_llm_called = 0,
    'ALTER TABLE ai_call_log ADD COLUMN llm_called BIT NOT NULL DEFAULT 1 AFTER max_similarity_score',
    'SELECT 1');
PREPARE stmt_ai_llm_called FROM @ai_sql_llm_called;
EXECUTE stmt_ai_llm_called;
DEALLOCATE PREPARE stmt_ai_llm_called;

-- chat_message_feedback: ensure key columns exist (for old environments)
SET @fb_has_deleted := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chat_message_feedback' AND COLUMN_NAME = 'deleted'
);
SET @fb_sql_deleted := IF(@fb_has_deleted = 0,
    'ALTER TABLE chat_message_feedback ADD COLUMN deleted BIT NOT NULL DEFAULT 0 AFTER update_time',
    'SELECT 1');
PREPARE stmt_fb_deleted FROM @fb_sql_deleted;
EXECUTE stmt_fb_deleted;
DEALLOCATE PREPARE stmt_fb_deleted;

