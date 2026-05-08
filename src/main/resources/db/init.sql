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

CREATE TABLE IF NOT EXISTS chat_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  message_id BIGINT NOT NULL,
  feedback_type VARCHAR(32) NOT NULL,
  reason VARCHAR(512),
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  deleted BIT NOT NULL DEFAULT 0,
  INDEX idx_feedback_message (message_id),
  INDEX idx_feedback_user (user_id)
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

CREATE TABLE IF NOT EXISTS notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  content VARCHAR(1024) NOT NULL,
  read_flag BIT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  deleted BIT NOT NULL DEFAULT 0,
  INDEX idx_notification_user (user_id, read_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS announcement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128) NOT NULL,
  content VARCHAR(2048) NOT NULL,
  enabled BIT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  deleted BIT NOT NULL DEFAULT 0,
  INDEX idx_announcement_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- password: admin123
INSERT INTO sys_user (username, email, password, nickname, status, role, create_time, update_time, deleted)
SELECT 'admin', 'admin@knowflow.local', '$2a$10$bhW3J..X41KH56.YrkCMcu.TTaEdec3CPrnSPyf193dFoRFVKHPB2', 'admin', 'ENABLED', 'ADMIN', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'admin');

INSERT INTO sys_user (username, email, password, nickname, status, role, create_time, update_time, deleted)
SELECT 'demo', 'demo@knowflow.local', '$2a$10$bhW3J..X41KH56.YrkCMcu.TTaEdec3CPrnSPyf193dFoRFVKHPB2', 'demo', 'ENABLED', 'USER', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'demo');

INSERT INTO ai_model_config (name, provider, base_url, api_key, model_name, enabled, default_model, thinking_enabled, description, create_time, update_time, deleted)
SELECT 'DeepSeek Reasoner', 'DEEPSEEK', 'https://api.deepseek.com', NULL, 'deepseek-reasoner', 1, 1, 1, 'Default DeepSeek model. Configure API key with DEEPSEEK_API_KEY or admin API.', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM ai_model_config WHERE provider = 'DEEPSEEK' AND model_name = 'deepseek-reasoner');

INSERT INTO prompt_template (code, name, content, scene, enabled, default_template, description, create_time, update_time, deleted)
SELECT 'rag_default', 'Default RAG Prompt', 'You are KnowFlow AI. Answer only from the provided document chunks. If evidence is insufficient, say that the current knowledge base has no sufficient evidence.', 'RAG', 1, 1, 'Default RAG prompt', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM prompt_template WHERE code = 'rag_default');

INSERT INTO system_config (config_key, config_value, description, create_time, update_time, deleted)
SELECT 'upload.maxFileSizeMb', '20', 'Maximum upload file size for MVP', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'upload.maxFileSizeMb');

INSERT INTO system_config (config_key, config_value, description, create_time, update_time, deleted)
SELECT 'rag.topK', '5', 'Default retrieval topK', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'rag.topK');

INSERT INTO knowledge_base (user_id, name, description, icon, category, status, document_count, create_time, update_time, deleted)
SELECT u.id, 'KnowFlow Demo KB', 'Demo knowledge base for backend smoke tests', 'book', 'demo', 'NORMAL', 1, NOW(), NOW(), 0
FROM sys_user u
WHERE u.username = 'demo'
  AND NOT EXISTS (SELECT 1 FROM knowledge_base kb WHERE kb.user_id = u.id AND kb.name = 'KnowFlow Demo KB');

INSERT INTO document (user_id, knowledge_base_id, name, original_name, file_type, file_size, file_path, parse_status, embedding_status, error_message, create_time, update_time, deleted)
SELECT u.id, kb.id, 'knowflow-demo.md', 'knowflow-demo.md', 'md', 256, './data/uploads/knowflow-demo.md', 'SUCCESS', 'SUCCESS', NULL, NOW(), NOW(), 0
FROM sys_user u
JOIN knowledge_base kb ON kb.user_id = u.id AND kb.name = 'KnowFlow Demo KB'
WHERE u.username = 'demo'
  AND NOT EXISTS (SELECT 1 FROM document d WHERE d.user_id = u.id AND d.knowledge_base_id = kb.id AND d.name = 'knowflow-demo.md');

INSERT INTO document_chunk (user_id, knowledge_base_id, document_id, chunk_index, content, token_count, embedding, create_time, update_time, deleted)
SELECT u.id, kb.id, d.id, 0,
       'KnowFlow AI is a personal knowledge base and intelligent document question-answering platform. It supports document upload, parsing, chunking, retrieval, RAG answers, citations, summaries, admin management, and audit logs.',
       42,
       '0.37828585,0.52600720,0.54054841,0.63165977,0.30420113,0.66078814,0.49101945,0.21338082,0.39665671,0.61594286,0.64134881,0.68248698,0.47470512,0.65106847,0.46459099,0.52428385,0.34326363,0.40138634,0.47084482,0.27733226,0.12792969,0.14062500,0.12109375,0.18066406,0.14843750,0.24218750,0.07519531,0.08984375,0.10937500,0.10546875,0.12011719,0.14062500,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000,0.00000000',
       NOW(), NOW(), 0
FROM sys_user u
JOIN knowledge_base kb ON kb.user_id = u.id AND kb.name = 'KnowFlow Demo KB'
JOIN document d ON d.user_id = u.id AND d.knowledge_base_id = kb.id AND d.name = 'knowflow-demo.md'
WHERE u.username = 'demo'
  AND NOT EXISTS (SELECT 1 FROM document_chunk c WHERE c.document_id = d.id AND c.chunk_index = 0);

INSERT INTO notification (user_id, title, content, read_flag, create_time, update_time, deleted)
SELECT u.id, 'Demo document parsed', 'Your demo document is ready for question answering.', 0, NOW(), NOW(), 0
FROM sys_user u
WHERE u.username = 'demo'
  AND NOT EXISTS (SELECT 1 FROM notification n WHERE n.user_id = u.id AND n.title = 'Demo document parsed');

INSERT INTO announcement (title, content, enabled, create_time, update_time, deleted)
SELECT 'Welcome to KnowFlow AI', 'This is the default announcement for backend testing.', 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM announcement WHERE title = 'Welcome to KnowFlow AI');

INSERT INTO login_log (user_id, account, success, message, create_time, update_time, deleted)
SELECT u.id, 'demo', 1, 'seed login log', NOW(), NOW(), 0
FROM sys_user u
WHERE u.username = 'demo'
  AND NOT EXISTS (SELECT 1 FROM login_log WHERE account = 'demo' AND message = 'seed login log');

INSERT INTO ai_call_log (user_id, model_name, call_type, elapsed_ms, success, fail_reason, create_time, update_time, deleted)
SELECT u.id, 'deepseek', 'CHAT', 1200, 1, NULL, NOW(), NOW(), 0
FROM sys_user u
WHERE u.username = 'demo'
  AND NOT EXISTS (SELECT 1 FROM ai_call_log WHERE user_id = u.id AND model_name = 'deepseek' AND call_type = 'CHAT');
