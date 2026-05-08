# ai-knowledge-java

KnowFlow AI backend MVP for a personal knowledge base and RAG question answering platform.

Backend stack: Spring Boot 3, Java 17, MyBatis-Plus, MySQL 8, Spring Security, JWT, DeepSeek Chat Completions, PDFBox and Apache POI.

## Run

1. Create schema:

```bash
mysql -u<username> -p<password> --default-character-set=utf8mb4 --execute="source C:/codex-project/ai-knowledge-java/src/main/resources/db/init.sql"
```

2. Adjust `src/main/resources/application.yml` if your MySQL username/password differ.

3. Configure DeepSeek API key:

```bash
export DEEPSEEK_API_KEY=your-api-key
```

4. Start:

```bash
mvn spring-boot:run
```

Base URL: `http://localhost:8080/api`

Default admin:

- account: `admin`
- password: `admin123`

## MVP APIs

- `POST /auth/register`
- `POST /auth/login`
- `GET /users/me`
- `POST /knowledge-bases`
- `GET /knowledge-bases`
- `POST /documents/upload`
- `GET /documents`
- `POST /chat/ask`
- `GET /chat/sessions`
- `POST /admin/auth/login`
- `GET /admin/dashboard/overview`
- `GET /admin/users`
- `GET /admin/knowledge-bases`
- `GET /admin/documents`
- `GET /admin/document-tasks`

The current AI implementation is mock-based so the MVP can run without external API keys. Embeddings are stored in MySQL and cosine similarity is computed in Java.
