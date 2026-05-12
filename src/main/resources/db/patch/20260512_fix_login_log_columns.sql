SET @has_ip := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'ip'
);
SET @sql_ip := IF(@has_ip = 0,
    'ALTER TABLE login_log ADD COLUMN ip VARCHAR(64) NULL AFTER account',
    'SELECT 1');
PREPARE stmt_ip FROM @sql_ip;
EXECUTE stmt_ip;
DEALLOCATE PREPARE stmt_ip;

SET @has_user_agent := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'user_agent'
);
SET @sql_user_agent := IF(@has_user_agent = 0,
    'ALTER TABLE login_log ADD COLUMN user_agent VARCHAR(512) NULL AFTER ip',
    'SELECT 1');
PREPARE stmt_user_agent FROM @sql_user_agent;
EXECUTE stmt_user_agent;
DEALLOCATE PREPARE stmt_user_agent;

SET @has_failure_reason := (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_log' AND COLUMN_NAME = 'failure_reason'
);
SET @sql_failure_reason := IF(@has_failure_reason = 0,
    'ALTER TABLE login_log ADD COLUMN failure_reason VARCHAR(255) NULL AFTER message',
    'SELECT 1');
PREPARE stmt_failure_reason FROM @sql_failure_reason;
EXECUTE stmt_failure_reason;
DEALLOCATE PREPARE stmt_failure_reason;
