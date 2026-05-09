# KnowFlow AI 前后端 MVP 功能缺口与联调问题清单

> 生成时间：2026-05-08  
> 检查对象：
> - 后端：`https://github.com/wbn-2020/ai-knowledge-java.git`
> - 前端：`https://github.com/wbn-2020/ai-knowledge-vue.git`
> - 参考文档：`知识库PRD_V1.md`、`KnowFlow_AI_Codex_Development_SOP.md`
>
> 检查方式：静态代码结构与接口对照检查，未本地启动、未编译、未真实请求接口。下面清单优先服务于 MVP 演示闭环：登录注册 → 创建知识库 → 上传文档 → 解析/向量化 → 知识库问答 → 引用来源 → 问答历史 → 后台基础管理。

---

## 0. 结论概览

当前前后端已经具备较完整的页面与接口雏形，但存在四类关键问题：

1. **后台权限后端未统一收口**：前端有管理员路由判断，但后端 `/admin/**` 只做登录校验风险较高。
2. **前端开发代理缺失**：前端 `baseURL = /api`，但 `vite.config.ts` 未配置 `/api` 代理，本地联调容易 404。
3. **前端已经写了大量后台页面 API，但后端接口未完全覆盖**：AI 配置、Prompt、系统参数、日志、公告等后台 MVP 页面会出现 404 或不可用。
4. **部分接口路径、参数、枚举不一致**：文档列表、任务重试、反馈/重新生成等接口存在前后端参数或路径不一致风险。

---

# 一、P0 阻塞 MVP 演示的问题

## P0-01 后端 `/admin/**` 未统一限制 ADMIN 角色

| 字段 | 内容 |
|---|---|
| 所属模块 | 后台权限 / 安全认证 |
| 前端相关文件 | `src/router/index.ts` |
| 后端相关文件 | `src/main/java/com/knowflow/security/SecurityConfig.java` |
| 当前现象 | 前端通过 `to.meta.requiresAdmin && authStore.user?.role !== 'ADMIN'` 控制后台页面访问，但后端 `SecurityConfig` 目前只放行 `/auth/register`、`/auth/login`、`/admin/auth/login`，其他接口只要求 authenticated。 |
| 影响 | 普通登录用户可能直接请求后台接口，绕过前端页面限制。 |
| 是否阻塞 MVP | 是，属于安全高风险。 |
| 建议修复 | 后端增加 `/admin/**` 的 `hasRole("ADMIN")` 或 `hasAuthority("ROLE_ADMIN")` 规则，并确认 JWT 中的角色映射一致。 |

建议修改：

```java
.requestMatchers("/auth/register", "/auth/login", "/admin/auth/login").permitAll()
.requestMatchers("/admin/**").hasRole("ADMIN")
.anyRequest().authenticated()
```

同时确认：

```text
前端 user.role = ADMIN
后端 Security 权限 = ROLE_ADMIN
JWT filter 中角色转换逻辑一致
```

---

## P0-02 前端 `/api` 代理缺失，开发环境无法稳定联调

| 字段 | 内容 |
|---|---|
| 所属模块 | 前端请求封装 / 本地联调 |
| 前端相关文件 | `src/api/request.ts`、`vite.config.ts` |
| 后端相关文件 | 全部 Controller |
| 当前现象 | 前端 `axios.create({ baseURL: '/api' })`，但 `vite.config.ts` 只有 Vue 插件和 alias，没有 `server.proxy`。 |
| 影响 | 本地 `npm run dev` 时，请求 `/api/...` 会先发到 Vite 服务，若没有代理到后端，会出现 404。 |
| 是否阻塞 MVP | 是，阻塞前后端联调。 |
| 建议修复 | 在 `vite.config.ts` 添加 `/api` 代理，并确认后端是否统一配置了 `/api` context-path。 |

建议修改：

```ts
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
}
```

如果后端没有 `server.servlet.context-path=/api`，则需要改成：

```ts
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      rewrite: path => path.replace(/^\/api/, ''),
    },
  },
}
```

---

## P0-03 后端接口是否带 `/api` 前缀需要确认

| 字段 | 内容 |
|---|---|
| 所属模块 | 全局接口路径 |
| 前端相关文件 | `src/api/request.ts` |
| 后端相关文件 | `application.yml`、所有 Controller |
| 当前现象 | 前端所有请求默认加 `/api`，例如 `/api/auth/login`、`/api/knowledge-bases`。后端 Controller 注解是 `/auth`、`/knowledge-bases`、`/documents`、`/chat`、`/admin` 等。 |
| 影响 | 如果后端没有配置 `server.servlet.context-path=/api`，前端请求将整体路径不匹配。 |
| 是否阻塞 MVP | 是。 |
| 建议修复 | 二选一：后端统一配置 `/api` context-path；或者前端 Vite 代理 rewrite 去掉 `/api`。生产环境 Nginx 也要保持一致。 |

验收方式：

```bash
curl http://localhost:8080/api/auth/login
curl http://localhost:8080/auth/login
```

根据实际可访问路径决定前端代理是否 rewrite。

---

## P0-04 后台 AI 配置、Prompt、系统参数、日志、公告接口缺失

| 字段 | 内容 |
|---|---|
| 所属模块 | 后台管理 |
| 前端相关文件 | `src/api/knowledge.ts`、`src/router/index.ts`、`src/views/admin/*` |
| 后端相关文件 | `src/main/java/com/knowflow/modules/admin/AdminController.java`、`modules/config`、`modules/log`、`modules/notice` |
| 当前现象 | 前端已经调用 `/admin/config/models`、`/admin/config/prompts`、`/admin/config/system`、`/admin/logs/*`、`/admin/announcements`；后端 `AdminController` 当前主要覆盖 dashboard、users、knowledge-bases、documents、document-tasks。 |
| 影响 | 后台 AI 配置、Prompt 模板、系统参数、日志审计、公告管理页面无法真实联调。 |
| 是否阻塞 MVP | 是，阻塞后台 MVP 完整演示。 |
| 建议修复 | 优先补后端 Controller，或短期隐藏对应菜单。建议先补 AI 配置、Prompt、日志，再补公告。 |

前端已调用但后端需要补齐的接口：

```text
GET    /admin/config/models
POST   /admin/config/models
PUT    /admin/config/models/{id}
DELETE /admin/config/models/{id}
POST   /admin/config/models/{id}/test

GET    /admin/config/prompts
GET    /admin/config/prompts/{id}
POST   /admin/config/prompts
PUT    /admin/config/prompts/{id}
DELETE /admin/config/prompts/{id}
PUT    /admin/config/prompts/{id}/enabled

GET    /admin/config/system
POST   /admin/config/system
DELETE /admin/config/system/{id}

GET    /admin/logs/operations
GET    /admin/logs/logins
GET    /admin/logs/ai-calls
GET    /admin/logs/alerts
GET    /admin/logs/{type}/export

GET    /admin/announcements
POST   /admin/announcements
PUT    /admin/announcements/{id}
DELETE /admin/announcements/{id}
```

---

## P0-05 文档列表接口参数前后端不一致

| 字段 | 内容 |
|---|---|
| 所属模块 | 文档管理 |
| 前端相关文件 | `src/api/knowledge.ts` -> `getDocumentPage()` |
| 后端相关文件 | `DocumentController.page()` |
| 当前现象 | 前端 `knowledgeBaseId` 是可选，同时传 `parseStatus`、`embeddingStatus`；后端 `/documents` 要求 `knowledgeBaseId` 必填，只支持 `keyword/pageNo/pageSize`。 |
| 影响 | 用户进入全局文档管理页时，如果未选择知识库可能直接报 400；状态筛选前端传了但后端不处理。 |
| 是否阻塞 MVP | 是，阻塞文档管理页。 |
| 建议修复 | 推荐后端增强为 `knowledgeBaseId` 可选，同时支持 `parseStatus`、`embeddingStatus`；也可以短期前端强制选择知识库后再查询。 |

建议后端接口参数：

```java
@GetMapping
public ApiResponse<PageResponse<DocumentVO>> page(
    @RequestParam(required = false) Long knowledgeBaseId,
    @RequestParam(defaultValue = "") String keyword,
    @RequestParam(required = false) DocumentParseStatus parseStatus,
    @RequestParam(required = false) EmbeddingStatus embeddingStatus,
    @RequestParam(defaultValue = "1") int pageNo,
    @RequestParam(defaultValue = "10") int pageSize
)
```

注意：即使 `knowledgeBaseId` 可选，也必须按当前登录 `userId` 过滤数据。

---

## P0-06 后台文档列表筛选参数前后端不一致

| 字段 | 内容 |
|---|---|
| 所属模块 | 后台文档管理 |
| 前端相关文件 | `src/api/knowledge.ts` -> `getAdminDocuments()` |
| 后端相关文件 | `AdminController.documents()` |
| 当前现象 | 前端传 `keyword`、`knowledgeBaseId`、`parseStatus`、`pageNo`、`pageSize`；后端后台文档列表只接收 `keyword`、`pageNo`、`pageSize`。 |
| 影响 | 后台文档页面的知识库筛选、解析状态筛选无效。 |
| 是否阻塞 MVP | 是，影响后台文档管理演示。 |
| 建议修复 | 后端 `AdminController.documents()` 和 `AdminService.documents()` 增加 `knowledgeBaseId`、`parseStatus`、`fileType` 等筛选条件。 |

---

## P0-07 后台任务重试接口前端调用但后端缺失

| 字段 | 内容 |
|---|---|
| 所属模块 | 文档处理任务 |
| 前端相关文件 | `src/api/knowledge.ts` -> `retryDocumentTask()` |
| 后端相关文件 | `AdminController` |
| 当前现象 | 前端调用 `POST /admin/document-tasks/{id}/retry`；后端只有 `GET /admin/document-tasks` 和 `GET /admin/document-tasks/{id}`。 |
| 影响 | 后台任务详情页或任务列表中的“重试任务”按钮不可用。 |
| 是否阻塞 MVP | 是，PRD 后台任务管理要求失败任务可重试。 |
| 建议修复 | 后端补 `POST /admin/document-tasks/{id}/retry`，内部调用任务重试服务。 |

建议接口：

```java
@PostMapping("/document-tasks/{id}/retry")
public ApiResponse<DocumentTaskVO> retryTask(@PathVariable Long id) {
    return ApiResponse.ok(adminService.retryTask(id));
}
```

---

# 二、P1 MVP 功能缺口清单

## P1-01 用户资料和修改密码能力需要确认完整实现

| 字段 | 内容 |
|---|---|
| 所属模块 | 用户认证 / 用户设置 |
| 前端相关文件 | `src/api/auth.ts`、`src/views/app/SettingsView.vue` |
| 后端相关文件 | `modules/auth`、`modules/user` |
| 当前现象 | 前端调用 `GET /users/me`、`PUT /users/me`、`PUT /users/me/password`。需要确认后端是否完整实现资料修改、密码修改、旧密码校验和重新登录策略。 |
| 影响 | 用户设置页可能只显示信息，修改能力不完整。 |
| 是否阻塞 MVP | 部分阻塞。 |
| 建议修复 | 确认 UserController 是否覆盖上述接口；密码修改必须校验旧密码并使用 BCrypt；修改成功后前端清 token 并跳回登录页。 |

验收点：

```text
[ ] GET /users/me 返回用户名、邮箱、头像、简介、角色、状态
[ ] PUT /users/me 可修改昵称/头像/邮箱/简介
[ ] PUT /users/me/password 校验 oldPassword
[ ] 修改密码后旧 token 处理策略明确
```

---

## P1-02 工作台接口需要确认与前端字段一致

| 字段 | 内容 |
|---|---|
| 所属模块 | 工作台 |
| 前端相关文件 | `src/api/knowledge.ts` -> `getDashboardOverview()`、`DashboardView.vue` |
| 后端相关文件 | `modules/dashboard` |
| 当前现象 | 前端调用 `/dashboard/overview`，期望返回 `knowledgeBaseCount`、`documentCount`、`chatSessionCount`、`recentKnowledgeBases`、`recentDocuments`、`recentSessions`。 |
| 影响 | 工作台统计卡片和最近记录可能无法显示或字段不匹配。 |
| 是否阻塞 MVP | 是，影响首页演示。 |
| 建议修复 | 对齐 DashboardOverview VO 字段；所有统计必须按当前 userId 过滤。 |

---

## P1-03 知识库详情接口存在路径分裂风险

| 字段 | 内容 |
|---|---|
| 所属模块 | 知识库管理 |
| 前端相关文件 | `src/api/knowledge.ts` -> `getKnowledgeBase()`、`getKnowledgeBaseDetail()` |
| 后端相关文件 | `modules/knowledge` |
| 当前现象 | 前端同时调用 `/knowledge-bases/{id}` 和 `/knowledge-bases/{id}/detail`。需要确认后端是否同时实现。 |
| 影响 | 知识库详情页可能某些区块加载失败。 |
| 是否阻塞 MVP | 是。 |
| 建议修复 | 统一接口语义：`GET /knowledge-bases/{id}` 返回基础信息；`GET /knowledge-bases/{id}/detail` 返回详情聚合信息，包括文档列表、最近会话、处理状态。 |

建议详情返回：

```json
{
  "id": 1,
  "name": "Java 学习资料",
  "description": "...",
  "documentCount": 3,
  "status": "NORMAL",
  "documents": [],
  "recentSessions": []
}
```

---

## P1-04 文档删除清理链路需要确认

| 字段 | 内容 |
|---|---|
| 所属模块 | 文档管理 / 文档索引 |
| 前端相关文件 | `DocumentManageView.vue`、`DocumentDetailView.vue` |
| 后端相关文件 | `DocumentService.delete()`、`DocumentChunkRepository`、向量存储相关类 |
| 当前现象 | 后端提供 `DELETE /documents/{id}`，但需要确认是否同步清理原始文件、chunk、向量索引和任务数据。 |
| 影响 | 文档删除后可能产生脏切片、脏向量，RAG 检索召回已删除文档。 |
| 是否阻塞 MVP | 是，影响数据正确性。 |
| 建议修复 | 删除文档时至少逻辑删除 document，并删除或标记 chunk/vector；向量库暂未接入时，也要预留 `VectorStore.deleteByDocumentId()`。 |

验收点：

```text
[ ] 删除后文档列表不显示
[ ] 删除后知识库文档数量减少
[ ] 删除后问答不会引用该文档
[ ] 删除后预览/下载返回 404 或业务错误
```

---

## P1-05 重新解析文档需要清理旧切片和旧向量

| 字段 | 内容 |
|---|---|
| 所属模块 | 文档解析 / 任务重试 |
| 前端相关文件 | `retryDocument()`、`retryAdminDocument()` |
| 后端相关文件 | `DocumentService.retry()`、`DocumentProcessService` |
| 当前现象 | 用户侧和后台侧都有重试入口，但需要确认重试前是否清理旧 chunk/vector。 |
| 影响 | 失败重试或重复解析后可能产生重复切片，导致问答召回重复内容。 |
| 是否阻塞 MVP | 是。 |
| 建议修复 | 重试流程必须：清理旧 chunk → 删除旧 vector → 重置状态 → 创建新 task → 异步执行。 |

---

## P1-06 RAG 问答主链路需要重点验收

| 字段 | 内容 |
|---|---|
| 所属模块 | 智能问答 |
| 前端相关文件 | `ChatView.vue`、`HistoryView.vue`、`SessionDetailView.vue`、`src/api/knowledge.ts` |
| 后端相关文件 | `ChatController.java`、`ChatService.java`、`infrastructure/ai`、`modules/document/DocumentChunk*` |
| 当前现象 | 后端已提供 `/chat/ask`、会话、消息、删除、重命名、清空、导出等接口；需要确认实际 RAG 流程是否完整：问题向量化、知识库过滤、TopK 检索、Prompt、LLM、引用落库。 |
| 影响 | 项目核心演示能力不稳定。 |
| 是否阻塞 MVP | 是。 |
| 建议修复 | 先只验收 MVP：单知识库问答、引用来源、会话历史。多知识库、指定文档、反馈、导出可以后置或隐藏。 |

MVP 验收点：

```text
[ ] POST /chat/ask 可以基于指定 knowledgeBaseId 提问
[ ] 检索时必须限制 userId + knowledgeBaseId
[ ] 无召回结果时不调用大模型或返回“不足以回答”
[ ] 返回 answer + references
[ ] references 包含 documentName/content/score/chunkId
[ ] 会话和消息成功落库
[ ] 历史会话可继续追问
```

---

## P1-07 前端问答“重新生成”接口路径与后端不一致

| 字段 | 内容 |
|---|---|
| 所属模块 | 智能问答 |
| 前端相关文件 | `src/api/knowledge.ts` -> `regenerateAnswer()` |
| 后端相关文件 | `ChatController.regenerate()` |
| 当前现象 | 前端调用 `POST /chat/sessions/{sessionId}/regenerate`，后端也是该路径；但早期后端可能存在 `/chat/messages/{id}/regenerate` 的设计，需要确认页面实际传的是 sessionId 还是 messageId。 |
| 影响 | 如果页面按 messageId 调用但 API 函数按 sessionId，重新生成会失败或语义错误。 |
| 是否阻塞 MVP | 否，重新生成属于扩展功能，可后置。 |
| 建议修复 | MVP 阶段隐藏重新生成按钮；后续明确重新生成是按最后一个用户问题重新生成，还是按某条 assistant message 重生成。 |

---

## P1-08 语义搜索和关键词搜索需要确认后端接口与字段

| 字段 | 内容 |
|---|---|
| 所属模块 | 搜索 |
| 前端相关文件 | `SearchView.vue`、`src/api/knowledge.ts` |
| 后端相关文件 | `modules/search` |
| 当前现象 | 前端调用 `/search/semantic` 和 `/search/keyword`，传 `knowledgeBaseId/query/topK` 或 `knowledgeBaseId/keyword/topK`。 |
| 影响 | 搜索页可能无法返回片段、文档名和相似度。 |
| 是否阻塞 MVP | 是，PRD 将知识库内搜索列入前台 MVP。 |
| 建议修复 | 对齐搜索 VO：必须返回 `documentId`、`documentName`、`chunkId`、`content`、`score`。并做 userId + knowledgeBaseId 过滤。 |

---

## P1-09 文档摘要接口路径需要确认

| 字段 | 内容 |
|---|---|
| 所属模块 | 知识整理 / 摘要 |
| 前端相关文件 | `KnowledgeSummaryView.vue`、`src/api/knowledge.ts` |
| 后端相关文件 | `modules/summary` |
| 当前现象 | 前端调用 `POST /summaries/document?documentId=...` 和 `POST /summaries/knowledge-base?knowledgeBaseId=...`。需要确认后端路径是否完全一致。 |
| 影响 | 知识整理页无法演示摘要能力。 |
| 是否阻塞 MVP | 部分阻塞，PRD 将文档摘要列为前台 MVP。 |
| 建议修复 | 后端保持上述路径，或者前端同步后端路径；摘要结果建议保存到数据库，支持重新生成。 |

---

## P1-10 通知与公告用户侧接口需要确认

| 字段 | 内容 |
|---|---|
| 所属模块 | 通知 / 公告 |
| 前端相关文件 | `NotificationCenterView.vue`、`src/api/knowledge.ts` |
| 后端相关文件 | `modules/notice` |
| 当前现象 | 前端调用 `/notifications`、`/notifications/unread-count`、`/notifications/{id}/read`、`/notifications/read-all`、`/announcements`。需要确认后端是否完整实现。 |
| 影响 | 通知中心、未读数量、公告展示可能不可用。 |
| 是否阻塞 MVP | 通知是 MVP，公告管理可后置。 |
| 建议修复 | 用户侧通知接口先补齐；公告如果后端未完成，可前端先隐藏入口。 |

---

# 三、P2 非阻塞但建议整理的问题

## P2-01 前端放了大量扩展功能占位路由，MVP 演示建议隐藏

| 字段 | 内容 |
|---|---|
| 所属模块 | 前端路由 / MVP 范围控制 |
| 前端相关文件 | `src/router/index.ts` |
| 当前现象 | 前端已有大量扩展路由，如知识库分享、协作、模板、归档、批量上传、版本管理、高级问答、模型路由、Token 成本等，使用 `ExtensionFeatureView.vue` 占位。 |
| 影响 | 演示时容易被误认为功能未完成，影响作品集完整度观感。 |
| 是否阻塞 MVP | 否。 |
| 建议修复 | MVP 演示环境隐藏扩展菜单；README 中标注“扩展规划”。 |

---

## P2-02 后端已经实现部分扩展接口，建议不要优先联调

| 字段 | 内容 |
|---|---|
| 所属模块 | RAG 扩展功能 |
| 后端相关文件 | `ChatController.java` |
| 当前现象 | 后端已有 `/chat/ask/document`、`/chat/ask/multi`、`/chat/feedback`、`/chat/sessions/{id}/export/*` 等扩展接口。 |
| 影响 | 如果优先联调扩展功能，会拖慢 MVP 主链路修复。 |
| 是否阻塞 MVP | 否。 |
| 建议修复 | 先确保 `/chat/ask` 单知识库问答、引用和历史稳定，再处理多知识库、指定文档、导出、反馈。 |

---

## P2-03 统一响应结构和文件下载响应需要单独处理

| 字段 | 内容 |
|---|---|
| 所属模块 | 前端请求封装 / 后端响应规范 |
| 前端相关文件 | `src/api/request.ts` |
| 后端相关文件 | `DocumentController.download()`、`ChatController.export*()` |
| 当前现象 | 前端响应拦截器对普通 JSON 要求 `code === 0`，对 blob、PDF、octet-stream、openxmlformats 做了绕过。后端下载/导出接口直接返回 `ResponseEntity<Resource>` 或 `ResponseEntity<byte[]>`。 |
| 影响 | 普通接口必须全部用 `ApiResponse`，二进制接口必须在前端设置 `responseType: 'blob'`。 |
| 是否阻塞 MVP | 否，但影响下载/导出体验。 |
| 建议修复 | 梳理所有非 JSON 接口，并在前端 API 层明确 `responseType: 'blob'` 或文本处理。 |

---

## P2-04 后端 Service 职责较重，后续建议拆分

| 字段 | 内容 |
|---|---|
| 所属模块 | 后端架构 |
| 后端相关文件 | `ChatService.java`、`DocumentService.java`、`DocumentProcessService.java` |
| 当前现象 | ChatService 承担问答、RAG 检索、Prompt、LLM、会话、反馈、导出等职责；DocumentService 承担上传、管理、预览、下载、重试等职责。 |
| 影响 | 后续调试和扩展困难。 |
| 是否阻塞 MVP | 否。 |
| 建议修复 | MVP 稳定后再拆分：`RagRetrievalService`、`PromptBuilder`、`AnswerGenerationService`、`ChatExportService`、`FileStorageService`、`DocumentParser` 策略类。 |

---

# 四、前后端接口对照清单

## 4.1 认证与用户

| 功能 | 前端调用 | 后端期望 | 状态 | 处理建议 |
|---|---|---|---|---|
| 用户注册 | `POST /auth/register` | `POST /auth/register` | 基本一致 | 测试字段：username/email/password/confirmPassword |
| 用户登录 | `POST /auth/login`，字段 account/password | `POST /auth/login` | 需确认 | 后端 LoginRequest 是否接收 account |
| 管理员登录 | `POST /admin/auth/login` | `POST /admin/auth/login` | 基本一致 | 确认非 ADMIN 账号禁止登录后台 |
| 退出登录 | `DELETE /auth/logout` | `DELETE /auth/logout` | 需确认 | JWT 无状态时可前端清 token；后端可返回 ok |
| 当前用户 | `GET /users/me` | UserController | 需确认 | 返回 role/status/avatar/email 等 |
| 修改用户 | `PUT /users/me` | UserController | 需确认 | 前后端字段统一 |
| 修改密码 | `PUT /users/me/password` | UserController | 需确认 | 旧密码校验，成功后重新登录 |

---

## 4.2 工作台

| 功能 | 前端调用 | 后端期望 | 状态 | 处理建议 |
|---|---|---|---|---|
| 用户工作台 | `GET /dashboard/overview` | DashboardController | 需确认 | 返回知识库数、文档数、会话数、最近数据 |
| 后台看板 | `GET /admin/dashboard/overview` | `AdminController.overview()` | 基本一致 | 确认统计字段与前端类型一致 |

---

## 4.3 知识库

| 功能 | 前端调用 | 后端期望 | 状态 | 处理建议 |
|---|---|---|---|---|
| 创建知识库 | `POST /knowledge-bases` | KnowledgeController | 需确认 | 必须按当前用户创建 |
| 知识库分页 | `GET /knowledge-bases` | KnowledgeController | 需确认 | 支持 keyword/pageNo/pageSize/sortBy |
| 基础详情 | `GET /knowledge-bases/{id}` | KnowledgeController | 需确认 | 必须校验 userId |
| 聚合详情 | `GET /knowledge-bases/{id}/detail` | KnowledgeController | 风险 | 后端若没有需补 |
| 编辑知识库 | `PUT /knowledge-bases/{id}` | KnowledgeController | 需确认 | 必须校验 userId |
| 删除知识库 | `DELETE /knowledge-bases/{id}` | KnowledgeController | 需确认 | 清理文档/chunk/vector 或异步清理 |
| 后台列表 | `GET /admin/knowledge-bases` | `AdminController.knowledgeBases()` | 基本一致 | 前端 status 筛选后端目前未接收，需补 |
| 后台状态 | `PUT /admin/knowledge-bases/{id}/status` | `AdminController.setKnowledgeBaseStatus()` | 基本一致 | status 枚举值需统一 |

---

## 4.4 文档

| 功能 | 前端调用 | 后端期望 | 状态 | 处理建议 |
|---|---|---|---|---|
| 上传文档 | `POST /documents/upload` form-data | `DocumentController.upload()` | 基本一致 | 字段 knowledgeBaseId/file 一致 |
| 文档分页 | `GET /documents` | `DocumentController.page()` | 不一致 | 前端可选 knowledgeBaseId + 状态筛选；后端 knowledgeBaseId 必填且无状态筛选 |
| 文档详情 | `GET /documents/{id}` | `DocumentController.detail()` | 基本一致 | 校验 userId |
| 删除文档 | `DELETE /documents/{id}` | `DocumentController.delete()` | 基本一致 | 清理 chunk/vector |
| 重命名 | `PUT /documents/{id}/rename` | `DocumentController.rename()` | 基本一致 | 请求体 `{ name }` |
| 预览 | `GET /documents/{id}/preview` | `DocumentController.preview()` | 基本一致 | 返回 JSON string，前端可直接展示 |
| 下载 | `GET /documents/{id}/download` | `DocumentController.download()` | 基本一致 | 前端 responseType blob 已设置 |
| 重新解析 | `POST /documents/{id}/retry` | `DocumentController.retry()` | 基本一致 | 重试前清理旧数据 |
| 后台文档列表 | `GET /admin/documents` | `AdminController.documents()` | 不一致 | 前端多筛选条件，后端只 keyword/page |
| 后台文档重试 | `POST /admin/documents/{id}/retry` | `AdminController.retryDocument()` | 基本一致 | 需确认权限和日志 |

---

## 4.5 文档任务

| 功能 | 前端调用 | 后端期望 | 状态 | 处理建议 |
|---|---|---|---|---|
| 任务列表 | `GET /admin/document-tasks` | `AdminController.tasks()` | 部分一致 | 前端传 status/taskType/keyword，后端只 pageNo/pageSize |
| 任务详情 | `GET /admin/document-tasks/{id}` | `AdminController.taskDetail()` | 基本一致 | 返回 failReason、taskType、status、时间 |
| 任务重试 | `POST /admin/document-tasks/{id}/retry` | 缺失 | 不一致 | 后端补接口 |

---

## 4.6 智能问答与历史

| 功能 | 前端调用 | 后端期望 | 状态 | 处理建议 |
|---|---|---|---|---|
| 知识库问答 | `POST /chat/ask` | `ChatController.ask()` | 基本一致 | MVP 核心，优先验收 |
| 指定文档问答 | `POST /chat/ask/document` | `ChatController.askDocument()` | 基本一致 | 扩展功能，可后置 |
| 多知识库问答 | `POST /chat/ask/multi` | `ChatController.askMulti()` | 基本一致 | 扩展功能，可后置 |
| 会话分页 | `GET /chat/sessions` | `ChatController.sessions()` | 基本一致 | 确认返回 knowledgeBaseName/messageCount |
| 会话消息 | `GET /chat/sessions/{id}/messages` | `ChatController.messages()` | 基本一致 | 校验 userId |
| 删除会话 | `DELETE /chat/sessions/{id}` | `ChatController.deleteSession()` | 基本一致 | 删除消息和引用 |
| 重命名会话 | `PUT /chat/sessions/{id}/rename` | `ChatController.renameSession()` | 基本一致 | 请求体 `{ title }` |
| 清空消息 | `DELETE /chat/sessions/{id}/messages` | `ChatController.clearSession()` | 基本一致 | 前端函数名与路径一致 |
| 重新生成 | `POST /chat/sessions/{id}/regenerate` | `ChatController.regenerate()` | 基本一致 | 扩展，MVP 可隐藏 |
| 反馈 | `POST /chat/feedback` | `ChatController.feedback()` | 基本一致 | 扩展，MVP 可隐藏 |
| 导出 | `GET /chat/sessions/{id}/export/{type}` | `ChatController.export*()` | 基本一致 | 前端 responseType blob 已设置 |

---

## 4.7 搜索、摘要、通知、公告

| 功能 | 前端调用 | 后端模块 | 状态 | 处理建议 |
|---|---|---|---|---|
| 语义搜索 | `GET /search/semantic` | `modules/search` | 需确认 | 返回文档片段和 score |
| 关键词搜索 | `GET /search/keyword` | `modules/search` | 需确认 | 支持 knowledgeBaseId/keyword/topK |
| 文档摘要 | `POST /summaries/document?documentId=` | `modules/summary` | 需确认 | 路径需确认 |
| 知识库摘要 | `POST /summaries/knowledge-base?knowledgeBaseId=` | `modules/summary` | 需确认 | 路径需确认 |
| 通知分页 | `GET /notifications` | `modules/notice` | 需确认 | 用户侧通知 MVP |
| 未读数量 | `GET /notifications/unread-count` | `modules/notice` | 需确认 | 顶部消息角标 |
| 单条已读 | `PUT /notifications/{id}/read` | `modules/notice` | 需确认 | 校验 userId |
| 全部已读 | `PUT /notifications/read-all` | `modules/notice` | 需确认 | 校验 userId |
| 用户公告 | `GET /announcements` | `modules/notice` | 需确认 | 可后置 |
| 后台公告 | `/admin/announcements` | Admin/Notice | 缺失风险 | 后端补或前端隐藏 |

---

# 五、统一枚举值清单

必须在前后端统一这些枚举，否则筛选、状态标签、权限判断会出错。

## 5.1 用户状态

```text
NORMAL
DISABLED
```

## 5.2 用户角色

```text
USER
ADMIN
```

注意：

```text
前端展示/判断：ADMIN
Spring Security 权限：ROLE_ADMIN
```

## 5.3 知识库状态

```text
NORMAL
PROCESSING
FAILED
DISABLED
```

## 5.4 文档解析状态

```text
PENDING
PARSING
SUCCESS
FAILED
```

## 5.5 向量化状态

```text
PENDING
PROCESSING
SUCCESS
FAILED
```

## 5.6 文档任务状态

```text
PENDING
PROCESSING
SUCCESS
FAILED
```

## 5.7 文档任务类型

```text
PARSE
EMBEDDING
REPARSE
```

具体值以数据库和后端 Enum 为准，前端不要自行定义另一套中文或小写字符串。

---

# 六、建议修复顺序

## 第 1 步：先打通本地联调基础

```text
[ ] 检查后端是否配置 /api context-path
[ ] 前端 vite.config.ts 添加 /api 代理
[ ] 确认登录接口可通
[ ] 确认 Authorization: Bearer <token> 后端可解析
[ ] 确认登录返回 user.role = USER / ADMIN
```

验收命令：

```bash
npm run dev
mvn spring-boot:run
curl -X POST http://localhost:5173/api/auth/login
```

---

## 第 2 步：修权限

```text
[ ] 后端 /admin/** 增加 ADMIN 权限
[ ] 后端用户侧所有资源接口按 userId 校验归属
[ ] 文档、知识库、会话、搜索、RAG 检索全部限制当前用户
[ ] 向量检索 metadata 必须包含 userId + knowledgeBaseId
```

必须测试：

```text
[ ] 普通用户访问 /admin/users 返回 403
[ ] A 用户访问 B 用户知识库返回 403 或 404
[ ] A 用户删除 B 用户文档失败
[ ] A 用户问答不会召回 B 用户文档
```

---

## 第 3 步：统一文档接口

```text
[ ] 用户文档列表支持 knowledgeBaseId 可选
[ ] 用户文档列表支持 parseStatus / embeddingStatus
[ ] 后台文档列表支持 knowledgeBaseId / parseStatus / fileType
[ ] 任务列表支持 status / taskType / keyword
[ ] 补 /admin/document-tasks/{id}/retry
```

---

## 第 4 步：补后台 MVP 接口

优先级：

```text
1. /admin/config/models
2. /admin/config/prompts
3. /admin/config/system
4. /admin/logs/operations
5. /admin/logs/logins
6. /admin/logs/ai-calls
7. /admin/logs/alerts
8. /admin/announcements
```

如果时间不够：

```text
[ ] 先隐藏公告管理
[ ] 先隐藏日志导出
[ ] 先隐藏模型测试
[ ] 只保留后台用户/知识库/文档/任务/基础配置
```

---

## 第 5 步：验收 MVP 主链路

```text
[ ] 注册普通用户
[ ] 登录普通用户
[ ] 创建知识库
[ ] 上传 PDF/DOCX/TXT/MD
[ ] 上传后立即返回，不阻塞解析
[ ] 后台或轮询任务看到解析状态变化
[ ] 解析成功后看到 chunk 数量
[ ] 基于知识库提问
[ ] 回答中展示引用来源
[ ] 查看问答历史
[ ] 删除会话
[ ] 管理员登录后台
[ ] 后台查看用户、知识库、文档、任务
[ ] 管理员禁用普通用户
[ ] 被禁用用户无法登录
```

---

# 七、给 Codex 的修复提示词

## 7.1 修复 P0 联调基础提示词

```text
请检查当前前端 ai-knowledge-vue 与后端 ai-knowledge-java 的本地联调配置。

目标：修复 /api 代理和后端接口前缀不一致问题。

要求：
1. 先检查后端 application.yml 是否配置 server.servlet.context-path=/api。
2. 检查前端 src/api/request.ts 的 baseURL。
3. 检查 vite.config.ts 是否配置 server.proxy。
4. 给出最小修改方案。
5. 修改 vite.config.ts，确保本地开发时 /api 能转发到 http://localhost:8080。
6. 不要修改业务页面。
7. 修改后给出验证步骤。
```

## 7.2 修复后台权限提示词

```text
请修复后端后台接口权限问题。

当前问题：
SecurityConfig 只对 /admin/auth/login 放行，其他接口只要求 authenticated，没有统一限制 /admin/** 必须是 ADMIN。

要求：
1. /auth/register、/auth/login、/admin/auth/login 允许匿名访问。
2. /admin/** 必须要求 ADMIN 角色。
3. 其他业务接口要求登录。
4. 检查 JwtAuthenticationFilter 中角色权限是否映射为 ROLE_ADMIN / ROLE_USER。
5. 不要修改 Controller 业务逻辑。
6. 给出普通用户访问 /admin/users 返回 403 的测试步骤。
```

## 7.3 修复后台缺失接口提示词

```text
请对照前端 src/api/knowledge.ts 和后端 AdminController，补齐后台 MVP 缺失接口。

优先补齐：
- /admin/config/models
- /admin/config/prompts
- /admin/config/system
- /admin/logs/operations
- /admin/logs/logins
- /admin/logs/ai-calls
- /admin/logs/alerts
- /admin/document-tasks/{id}/retry

要求：
1. 不要一次性重构项目结构。
2. 先列出前端调用但后端不存在的接口。
3. 按最小可用原则补 Controller、Service、DTO、VO、Repository。
4. 所有后台接口必须要求 ADMIN 权限。
5. 返回结构必须统一 ApiResponse。
6. 分页响应必须统一 PageResponse。
7. 最后运行 mvn clean compile。
```

## 7.4 修复文档接口参数提示词

```text
请修复文档列表和任务列表的前后端参数不一致问题。

问题：
1. 前端 getDocumentPage 支持 knowledgeBaseId 可选、parseStatus、embeddingStatus。
2. 后端 /documents 当前 knowledgeBaseId 必填，且不支持 parseStatus/embeddingStatus。
3. 前端 getAdminDocuments 支持 knowledgeBaseId、parseStatus。
4. 后端 /admin/documents 当前只支持 keyword/pageNo/pageSize。
5. 前端 getDocumentTasks 支持 status/taskType/keyword。
6. 后端 /admin/document-tasks 当前只支持 pageNo/pageSize。

要求：
1. 后端补齐这些查询参数。
2. 用户侧查询必须按当前 userId 过滤。
3. 后台查询必须要求 ADMIN。
4. 不要修改前端字段名。
5. 给出接口测试用例。
```

---

# 八、MVP 最小可演示版本判定标准

满足下面条件，可以认为第一版 MVP 可以演示：

```text
[ ] 普通用户可以注册、登录、退出
[ ] 普通用户可以创建、编辑、删除知识库
[ ] 普通用户可以上传 PDF/DOCX/TXT/MD 文档
[ ] 上传后系统创建文档记录和解析任务
[ ] 系统可以解析文档并生成切片/向量
[ ] 用户可以基于知识库提问
[ ] AI 回答包含引用来源
[ ] 用户可以查看问答历史和会话消息
[ ] 用户不能访问其他用户的数据
[ ] 管理员可以登录后台
[ ] 普通用户不能访问后台接口
[ ] 管理员可以查看用户、知识库、文档、任务
[ ] 管理员可以配置模型或至少查看模型配置页面
[ ] 管理员可以查看日志或至少查看日志列表页面
```

当前最应该先修：

```text
1. /api 代理与接口前缀
2. /admin/** 后端权限
3. 后台缺失接口
4. 文档列表/任务列表参数不一致
5. RAG 问答主链路验收
```
