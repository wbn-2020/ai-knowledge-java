CREATE DATABASE IF NOT EXISTS knowflow_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE knowflow_ai;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  email VARCHAR(128) UNIQUE,
  password VARCHAR(255) NOT NULL,
  nickname VARCHAR(128),
  avatar VARCHAR(512),
  bio VARCHAR(512),
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  role VARCHAR(32) NOT NULL DEFAULT 'USER',
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  deleted BIT NOT NULL DEFAULT 0,
  INDEX idx_user_status (status),
  INDEX idx_user_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS knowledge_base (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(512),
  icon VARCHAR(128),
  category VARCHAR(64),
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  document_count INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  deleted BIT NOT NULL DEFAULT 0,
  INDEX idx_kb_user (user_id, deleted),
  INDEX idx_kb_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS document (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  knowledge_base_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  file_type VARCHAR(32) NOT NULL,
  file_size BIGINT NOT NULL,
  file_path VARCHAR(1024) NOT NULL,
  parse_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  embedding_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  error_message VARCHAR(1024),
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  deleted BIT NOT NULL DEFAULT 0,
  INDEX idx_doc_user_kb (user_id, knowledge_base_id, deleted),
  INDEX idx_doc_parse_status (parse_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS document_chunk (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  knowledge_base_id BIGINT NOT NULL,
  document_id BIGINT NOT NULL,
  chunk_index INT NOT NULL,
  content LONGTEXT NOT NULL,
  token_count INT NOT NULL,
  embedding LONGTEXT,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  deleted BIT NOT NULL DEFAULT 0,
  INDEX idx_chunk_scope (user_id, knowledge_base_id, deleted),
  INDEX idx_chunk_document (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS document_process_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  knowledge_base_id BIGINT NOT NULL,
  document_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  fail_reason VARCHAR(1024),
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  deleted BIT NOT NULL DEFAULT 0,
  INDEX idx_task_document (document_id),
  INDEX idx_task_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  knowledge_base_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  deleted BIT NOT NULL DEFAULT 0,
  INDEX idx_session_user (user_id, deleted),
  INDEX idx_session_kb (knowledge_base_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  session_id BIGINT NOT NULL,
  role VARCHAR(32) NOT NULL,
  content LONGTEXT NOT NULL,
  model_name VARCHAR(128),
  token_count INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  deleted BIT NOT NULL DEFAULT 0,
  INDEX idx_message_session (user_id, session_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_message_reference (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  message_id BIGINT NOT NULL,
  document_id BIGINT NOT NULL,
  chunk_id BIGINT NOT NULL,
  document_name VARCHAR(255) NOT NULL,
  content LONGTEXT NOT NULL,
  score DOUBLE NOT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  deleted BIT NOT NULL DEFAULT 0,
  INDEX idx_ref_message (user_id, message_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  action VARCHAR(64) NOT NULL,
  target_type VARCHAR(128),
  target_id BIGINT,
  detail VARCHAR(1024),
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  deleted BIT NOT NULL DEFAULT 0,
  INDEX idx_operation_user (user_id),
  INDEX idx_operation_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS login_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  account VARCHAR(128) NOT NULL,
  success BIT NOT NULL,
  message VARCHAR(255),
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  deleted BIT NOT NULL DEFAULT 0,
  INDEX idx_login_user (user_id),
  INDEX idx_login_account (account)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_call_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  model_name VARCHAR(128),
  call_type VARCHAR(64),
  elapsed_ms BIGINT,
  success BIT NOT NULL,
  fail_reason VARCHAR(1024),
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  deleted BIT NOT NULL DEFAULT 0,
  INDEX idx_ai_call_user (user_id),
  INDEX idx_ai_call_type (call_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_model_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  provider VARCHAR(64) NOT NULL,
  base_url VARCHAR(255) NOT NULL,
  api_key VARCHAR(255),
  model_name VARCHAR(128) NOT NULL,
  enabled BIT NOT NULL DEFAULT 1,
  default_model BIT NOT NULL DEFAULT 0,
  thinking_enabled BIT NOT NULL DEFAULT 1,
  max_tokens INT,
  temperature DOUBLE,
  description VARCHAR(512),
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  deleted BIT NOT NULL DEFAULT 0,
  INDEX idx_ai_model_provider (provider),
  INDEX idx_ai_model_default (default_model)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS prompt_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  content LONGTEXT NOT NULL,
  scene VARCHAR(64) NOT NULL,
  enabled BIT NOT NULL DEFAULT 1,
  default_template BIT NOT NULL DEFAULT 0,
  description VARCHAR(512),
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  deleted BIT NOT NULL DEFAULT 0,
  INDEX idx_prompt_scene (scene)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS system_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_key VARCHAR(128) NOT NULL UNIQUE,
  config_value VARCHAR(1024) NOT NULL,
  description VARCHAR(512),
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  deleted BIT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- password: admin123
INSERT INTO sys_user (username, email, password, nickname, status, role, create_time, update_time, deleted)
SELECT 'admin', 'admin@knowflow.local', '$2a$10$bhW3J..X41KH56.YrkCMcu.TTaEdec3CPrnSPyf193dFoRFVKHPB2', '???', 'ENABLED', 'ADMIN', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'admin');

INSERT INTO ai_model_config (name, provider, base_url, api_key, model_name, enabled, default_model, thinking_enabled, description, create_time, update_time, deleted)
SELECT 'DeepSeek Reasoner', 'DEEPSEEK', 'https://api.deepseek.com', NULL, 'deepseek-reasoner', 1, 1, 1, 'Default DeepSeek model. Configure API key with DEEPSEEK_API_KEY or admin API.', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM ai_model_config WHERE provider = 'DEEPSEEK' AND model_name = 'deepseek-reasoner');

INSERT INTO prompt_template (code, name, content, scene, enabled, default_template, description, create_time, update_time, deleted)
SELECT 'rag_default', '?????????', '?? KnowFlow AI ???????????????????????????????????????????????????', 'RAG', 1, 1, 'Default RAG prompt', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM prompt_template WHERE code = 'rag_default');
