# KnowFlow AI 后端 MVP 接口冒烟测试报告

测试时间：2026-05-09  
项目路径：`C:\codex-project\ai-knowledge-java`  
测试原则：仅测试与记录，不修改代码。

## 1. 后端启动结果

- 启动方式：`mvn spring-boot:run`
- 端口：`8080`（监听成功）
- context-path：`/api`（日志显示 `Tomcat started ... with context path '/api'`）
- 启动结论：成功启动，无启动阶段 ERROR。

## 2. 测试账号信息

- 普通用户A：`mvp_smoke_a_1778300644`
- 普通用户B：`mvp_smoke_b_1778300644`
- 管理员：`admin`（项目内置）
- 本次关键对象：
  - A 的知识库 `kbA=17`
  - A 的文档 `docA=7`
  - B 的知识库 `kbB=19`
  - 会话 `sessionId=3`

## 3. 接口测试表

| 接口 | 请求方式 | 状态码 | 结果 | 问题 |
|---|---|---:|---|---|
| `/api/auth/register` | POST | 200 | 通过（`code=0`） | 无 |
| `/api/auth/login` | POST | 200 | 通过（`code=0`） | 无 |
| `/api/users/me` | GET | 200 | 通过（`code=0`） | 无 |
| `/api/knowledge-bases` | POST | 200 | 通过（`code=0`） | 无 |
| `/api/knowledge-bases?pageNo=1&pageSize=10` | GET | 200 | 通过（`code=0`） | 无 |
| `/api/knowledge-bases/{id}` | GET | 200 | 通过（`code=0`） | 无 |
| `/api/documents/upload` | POST | 200 | 通过（`code=0`） | 文档初始状态为 `PENDING`（符合异步处理） |
| `/api/documents?pageNo=1&pageSize=10` | GET | 200 | 通过（`code=0`） | 无 |
| `/api/documents?knowledgeBaseId={id}&pageNo=1&pageSize=10` | GET | 200 | 通过（`code=0`） | 无 |
| `/api/admin/document-tasks?pageNo=1&pageSize=10`（USER token） | GET | 403 | 预期内失败 | 普通用户无后台权限 |
| `/api/chat/ask` | POST | 200 | 通过（`code=0`） | `answer` 返回正常，`references` 为空数组 |
| `/api/chat/sessions` | GET | 200 | 通过（`code=0`） | 无 |
| `/api/chat/sessions/{id}/messages` | GET | 200 | 通过（`code=0`） | 无 |
| `/api/admin/auth/login` | POST | 200 | 通过（`code=0`） | 无 |
| `/api/admin/users` | GET | 200 | 通过（`code=0`） | 无 |
| `/api/admin/knowledge-bases?pageNo=1&pageSize=10` | GET | 200 | 通过（`code=0`） | 无 |
| `/api/admin/documents?pageNo=1&pageSize=10` | GET | 200 | 通过（`code=0`） | 无 |
| `/api/admin/logs/operations` | GET | 200 | 通过（`code=0`） | 无 |
| `/api/admin/logs/logins` | GET | 200 | 通过（`code=0`） | 无 |
| `/api/admin/logs/ai-calls` | GET | 200 | 通过（`code=0`） | 无 |

补充观察：
- 上传后 `document` 记录已生成（`id=7`，`parseStatus=PENDING`）。
- 后台任务列表可查到 `documentId=7` 对应 `document_process_task`（状态 `PENDING`）。

## 4. 权限测试表

| 场景 | 预期 | 实际 | 结论 |
|---|---|---|---|
| 未登录访问 `/api/admin/users` | 401 | 403（空响应体） | 不通过（状态码不符预期） |
| 普通用户访问 `/api/admin/users` | 403 | 403 | 通过 |
| 管理员访问 `/api/admin/users` | 200 | 200（`code=0`） | 通过 |
| A 用户访问 B 用户知识库 | 拒绝访问 | HTTP 200 + `code=404` (`knowledge base not found`) | 通过（按业务语义已阻断） |
| A 用户查看 B 用户文档 | 拒绝访问 | HTTP 200 + `code=404` (`knowledge base not found`) | 通过（按业务语义已阻断） |

## 5. P0/P1/P2 问题清单

### P0
- 无阻塞主链路的 P0 问题。

### P1
1. 未登录访问后台接口状态码与验收期望不一致  
   - 现状：`/api/admin/users` 未登录返回 403  
   - 期望：401  
   - 影响：联调脚本若按 HTTP 状态严格断言，会判失败。

### P2
1. 新上传文档在测试窗口内仍为 `PENDING`  
   - 说明：当前看起来是异步解析，短时间未转 `SUCCESS`。  
   - 影响：演示时可能暂时看不到解析完成态与引用命中。
2. `chat/ask` 本次返回 `references: []`  
   - 说明：接口结构正确，字段存在，但当前未命中可引用片段。  
   - 影响：演示“引用来源”时稳定性一般，需准备已有可检索数据。

## 6. 当前后端是否可进入前端联调

结论：**可以进入前端联调**（主链路接口整体可用）。  
需要前端/联调侧注意：
- 未登录访问后台接口当前是 403（不是 401）。
- 文档解析与引用属于异步效果，联调时需给处理时间或用已有成功样例数据。

