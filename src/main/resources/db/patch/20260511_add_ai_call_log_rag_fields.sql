ALTER TABLE ai_call_log
    ADD COLUMN IF NOT EXISTS username VARCHAR(64) NULL AFTER session_id,
    ADD COLUMN IF NOT EXISTS question VARCHAR(1000) NULL AFTER username,
    ADD COLUMN IF NOT EXISTS retrieve_count INT NULL AFTER question,
    ADD COLUMN IF NOT EXISTS effective_retrieve_count INT NULL AFTER retrieve_count,
    ADD COLUMN IF NOT EXISTS top_k INT NULL AFTER effective_retrieve_count,
    ADD COLUMN IF NOT EXISTS similarity_threshold DOUBLE NULL AFTER top_k,
    ADD COLUMN IF NOT EXISTS max_similarity_score DOUBLE NULL AFTER similarity_threshold,
    ADD COLUMN IF NOT EXISTS llm_called BIT NOT NULL DEFAULT b'1' AFTER max_similarity_score;

INSERT INTO system_config (config_key, config_value, description, create_time, update_time, deleted)
SELECT 'rag.contextMaxLength', '4000', 'RAG max context length', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'rag.contextMaxLength');

UPDATE system_config
SET config_value = '0.55',
    description = 'RAG similarity threshold'
WHERE config_key IN ('rag.minScore', 'rag.similarityThreshold')
  AND (config_value IS NULL OR config_value = '' OR config_value = '0.05' OR config_value = '0.65');
