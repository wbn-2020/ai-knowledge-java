CREATE TABLE IF NOT EXISTS knowledge_base_summary (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  knowledge_base_id BIGINT NOT NULL,
  summary LONGTEXT,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  deleted BIT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_user_kb_summary (user_id, knowledge_base_id),
  INDEX idx_kb_summary (knowledge_base_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
