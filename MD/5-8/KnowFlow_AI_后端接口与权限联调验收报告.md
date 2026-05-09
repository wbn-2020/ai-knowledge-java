# KnowFlow AI 后端接口与权限联调验收报告

验收时间：2026-05-09  
验收仓库：`C:\codex-project\ai-knowledge-java`  
验收方式：PowerShell + `curl` 实测（仅检查，不改代码）

## 1. 后端启动结果

- 执行命令：`mvn spring-boot:run`
- 启动结果：成功启动
- 端口检查：`8080` 监听成功（`netstat` 显示 LISTENING）
- context-path 检查：`/api`（启动日志显示 `Tomcat started ... with context path '/api'`）
- 关键日志文件：`C:\codex-project\ai-knowledge-java\target\backend-full.log`

## 2. 数据库连接结果

- 结果：**失败**
- 证据（启动日志）：
  - `HikariPool-1 - Exception during pool initialization`
  - `java.sql.SQLException: Access denied for user 'root'@'localhost' (using password: NO)`
- 影响：所有依赖数据库的主链路接口（注册、登录、知识库、文档、问答、后台数据接口）均不可用或返回业务错误。

## 3. 编译结果

- 执行命令：`mvn clean compile`
- 结果：**BUILD SUCCESS**

---

## 4. 接口验收表

| 模块 | 接口 | 请求方式 | 测试结果 | 状态码 | 问题说明 |
|---|---|---|---|---:|---|
| 认证 | `/api/auth/register` | POST | 失败 | 200 | 响应体 `{"code":500,"message":"系统错误"}`，数据库连接失败导致 |
| 认证 | `/api/auth/login` | POST | 失败 | 200 | 同上，未拿到 token |
| 用户 | `/api/users/me` | GET | 失败 | 403 | 因无法登录获取 token，且未登录访问被拒绝 |
| 知识库 | `/api/knowledge-bases` | POST | 失败 | 403 | 无有效 token，未进入业务流程 |
| 知识库 | `/api/knowledge-bases?pageNo=1&pageSize=10` | GET | 失败 | 403 | 同上 |
| 知识库 | `/api/knowledge-bases/{id}` | GET | 阻塞 | N/A | 前置创建知识库失败，无法获得 id |
| 文档 | `/api/documents/upload` | POST | 阻塞 | N/A | 前置知识库创建失败 |
| 文档 | `/api/documents?pageNo=1&pageSize=10` | GET | 失败 | 403 | 无有效 token |
| 文档 | `/api/documents?knowledgeBaseId={id}&parseStatus=SUCCESS&pageNo=1&pageSize=10` | GET | 阻塞 | N/A | 前置知识库创建失败 |
| 问答 | `/api/chat/ask` | POST | 阻塞 | N/A | 前置知识库创建失败 |
| 问答 | `/api/chat/sessions` | GET | 失败 | 403 | 无有效 token |
| 问答 | `/api/chat/sessions/{id}/messages` | GET | 阻塞 | N/A | 前置问答未成功，无法取得 sessionId |
| 后台认证 | `/api/admin/auth/login` | POST | 失败 | 200 | 响应体 `{"code":500,"message":"系统错误"}`，数据库连接失败 |
| 后台用户 | `/api/admin/users` | GET | 失败 | 403 | 无有效 admin token |
| 后台知识库 | `/api/admin/knowledge-bases?status=NORMAL&pageNo=1&pageSize=10` | GET | 失败 | 403 | 无有效 admin token |
| 后台文档 | `/api/admin/documents?parseStatus=FAILED&pageNo=1&pageSize=10` | GET | 失败 | 403 | 无有效 admin token |
| 后台任务 | `/api/admin/document-tasks?status=FAILED&pageNo=1&pageSize=10` | GET | 失败 | 403 | 无有效 admin token |
| 后台任务重试 | `/api/admin/document-tasks/{id}/retry` | POST | 阻塞 | N/A | 前置任务列表不可用，无法取得 taskId |
| 后台日志 | `/api/admin/logs/operations` | GET | 失败 | 403 | 无有效 admin token |
| 后台日志 | `/api/admin/logs/logins` | GET | 失败 | 403 | 无有效 admin token |
| 后台日志 | `/api/admin/logs/ai-calls` | GET | 失败 | 403 | 无有效 admin token |

> 说明：原始请求结果已保存到  
> `C:\codex-project\ai-knowledge-java\target\mvp_acceptance_raw_results_curl.json`

---

## 5. 权限验收表

| 场景 | 预期结果 | 实际结果 | 是否通过 |
|---|---|---|---|
| 未登录访问 `/api/admin/users` | 401 | 403（空响应体） | 否 |
| 普通 USER 访问 `/api/admin/users` | 403 | 阻塞（未能成功登录获取 USER token） | 否（阻塞） |
| ADMIN 访问 `/api/admin/users` | 200 | 阻塞（未能成功登录获取 ADMIN token） | 否（阻塞） |
| A 用户不能访问 B 用户知识库 | 拒绝访问（403/404） | 阻塞（无法完成 A/B 用户与知识库创建） | 否（阻塞） |
| A 用户不能查看 B 用户文档 | 拒绝访问（403/404） | 阻塞（无法完成 A/B 用户与文档创建） | 否（阻塞） |
| A 用户不能基于 B 用户知识库提问 | 拒绝访问（403/404） | 阻塞（无法完成 A/B 用户与知识库创建） | 否（阻塞） |

---

## 6. 后端问题清单（P0/P1/P2）

### P0-01 数据库连接失败，阻塞所有主链路接口
- 问题标题：MySQL 鉴权失败（`root` 无密码）导致业务不可用
- 复现步骤：
  1. 启动后端 `mvn spring-boot:run`
  2. 调用 `/api/auth/register` 或 `/api/auth/login`
  3. 查看 `target/backend-full.log`
- 预期结果：注册/登录成功并返回 token
- 实际结果：接口返回 `{"code":500,"message":"系统错误"}`；日志报 `Access denied for user 'root'@'localhost' (using password: NO)`
- 涉及接口或文件：
  - 接口：`/api/auth/register`、`/api/auth/login`（及所有依赖 DB 的接口）
  - 文件：`src/main/resources/application.yml`
  - 日志：`target/backend-full.log`
- 建议修复方向：
  - 在启动环境正确设置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`
  - 确认 `knowflow_ai` 库可访问，且用户权限包含读写

### P0-02 未登录访问后台接口返回 403（与验收预期 401 不一致）
- 问题标题：未登录访问 `/api/admin/**` 的状态码不符合验收口径
- 复现步骤：
  1. 不带 token 调用 `GET /api/admin/users?pageNo=1&pageSize=10`
- 预期结果：401
- 实际结果：403（响应体为空）
- 涉及接口或文件：
  - 接口：`/api/admin/users`（推测 `/api/admin/**` 同类）
  - 文件：`src/main/java/com/knowflow/security/SecurityConfig.java`
- 建议修复方向：
  - 配置统一 `AuthenticationEntryPoint` 返回 401（未认证）
  - `AccessDeniedHandler` 保持 403（已认证但无权限）

### P1-01 数据层异常被包装为 HTTP 200 + 业务 code=500
- 问题标题：关键失败场景 HTTP 状态不可区分
- 复现步骤：
  1. 在当前数据库连接失败情况下调用 `/api/auth/login`
- 预期结果：HTTP 5xx（或至少可明确区分系统异常）
- 实际结果：HTTP 200，响应体 `code=500`
- 涉及接口或文件：
  - 接口：`/api/auth/register`、`/api/auth/login`、`/api/admin/auth/login`
  - 文件：全局异常处理相关类（`common` 包下异常处理）
- 建议修复方向：
  - 评估是否保留“统一 200”策略；若保留，前后端需明确约定并统一处理 `code != 0`

### P2-01 本轮无法完成任务重试与问答引用的实证验收
- 问题标题：链路后半段验证受前置故障阻塞
- 复现步骤：
  1. 在数据库不可用状态下执行 `/api/documents/upload`、`/api/chat/ask`、`/api/admin/document-tasks/{id}/retry`
- 预期结果：可验证文档入库、任务创建、失败原因、重试防重、引用来源
- 实际结果：前置登录/知识库创建失败，后续均无法执行
- 涉及接口或文件：文档、问答、后台任务相关接口
- 建议修复方向：先解决 P0-01，再重跑整套验收

---

## 7. 最终结论

- **当前后端尚不具备前端 MVP 联调条件。**
- 当前必须先修复：
  1. **P0-01**：数据库连接配置（否则注册/登录/业务链路全部不可用）
  2. **P0-02**：未登录访问后台接口返回码与验收标准对齐（401/403边界）
- 待以上修复后，建议按同一清单重新跑 1~21 接口与 6 条权限场景，重点复验：
  - 文档上传后 `document`/`document_process_task` 是否创建
  - `chat/ask` 是否返回 `answer`、`sessionId`、`references`
  - 后台任务重试是否防止重复 `PENDING/PROCESSING` 任务
