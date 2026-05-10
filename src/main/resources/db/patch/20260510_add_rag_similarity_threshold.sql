INSERT INTO system_config (config_key, config_value, description, create_time, update_time, deleted)
SELECT 'rag.similarityThreshold', config_value, 'RAG similarity threshold', NOW(), NOW(), 0
FROM system_config
WHERE config_key = 'rag.minScore'
  AND NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'rag.similarityThreshold')
LIMIT 1;

INSERT INTO system_config (config_key, config_value, description, create_time, update_time, deleted)
SELECT 'rag.similarityThreshold', '0.65', 'RAG similarity threshold', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'rag.similarityThreshold');
