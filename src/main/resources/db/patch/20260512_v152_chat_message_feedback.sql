CREATE TABLE IF NOT EXISTS chat_message_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  session_id BIGINT NOT NULL,
  message_id BIGINT NOT NULL,
  feedback_type VARCHAR(32) NOT NULL,
  reason VARCHAR(64),
  remark VARCHAR(500),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIT NOT NULL DEFAULT b'0',
  UNIQUE KEY uk_user_message_feedback (user_id, message_id),
  INDEX idx_feedback_message_id (message_id),
  INDEX idx_feedback_session_id (session_id),
  INDEX idx_feedback_type (feedback_type),
  INDEX idx_feedback_reason (reason),
  INDEX idx_feedback_created_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
