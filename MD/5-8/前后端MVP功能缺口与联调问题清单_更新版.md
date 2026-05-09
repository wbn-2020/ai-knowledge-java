# KnowFlow AI 前后端 MVP 功能缺口与联调问题清单（更新版）

> 更新时间：2026-05-08  
> 后端仓库：<https://github.com/wbn-2020/ai-knowledge-java.git>  
> 前端仓库：<https://github.com/wbn-2020/ai-knowledge-vue.git>  
> 参考文档：`知识库PRD_V1.md`、`KnowFlow_AI_Codex_Development_SOP.md`、上一版 `前后端MVP功能缺口与联调问题清单.md`  
> 检查方式：静态代码检查 + 前后端接口对照 + PRD 对照。未本地启动、未执行 `mvn clean compile`、未执行 `npm run build`、未真实请求接口。

---

## 0. 本次更新结论

后端相对上一版已经补了一部分后台 MVP 能力，尤其是：

- `/admin/config/models`、`/admin/config/prompts`、`/admin/config/system` 已存在；
- `/admin/logs/operations`、`/admin/logs/logins`、`/admin/logs/ai-calls`、`/admin/logs/alerts` 已存在；
- `/admin/announcements` 已存在；
- `/dashboard/overview`、`/knowledge-bases/{id}/detail`、`/search/*`、`/summaries/*`、`/notifications/*` 已存在；
- 用户资料与修改密码接口已存在。

但当前仍然存在几类会影响 MVP 演示和联调的问题：

1. **前端本地代理仍缺失**：前端 `baseURL=/api`，后端已有 `/api` context-path，但 Vite 没有 `/api` proxy，本地开发环境请求会打到前端 dev server。
2. **后端安全层仍未统一限制 `/admin/**` 为 ADMIN**：虽然部分 Service 内部调用了 `SecurityUtils.requireAdmin()`，但安全配置层仍是 `/admin/auth/login` 放行后其他接口只要登录即可。
3. **文档列表、后台文档列表、任务列表的筛选参数仍与前端不一致**。
4. **后台任务重试接口 `/admin/document-tasks/{id}/retry` 仍缺失**。
5. **配置文件仍包含数据库密码和固定 JWT Secret**，不适合公开仓库和作品集展示。
6. **后台看板、任务详情、日志字段仍偏简化**，和 PRD 的后台 MVP 展示要求还有差距。

---

# 一、已确认覆盖 / 不再作为 P0 的问题

## DONE-01 后端 `/api` 前缀已配置

| 项 | 内容 |
|---|---|
| 结论 | 已覆盖 |
| 证据 | 后端 `application.yml` 中已配置 `server.servlet.context-path=/api`。 |
| 当前处理 | 不再要求后端改 `/api` 前缀；前端本地只需要配置 Vite proxy，不需要 rewrite。 |

建议前端代理：

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

---

## DONE-02 后台配置、日志、公告接口已补齐主要路径

| 模块 | 当前后端状态 | 前端对应调用 | 结论 |
|---|---|---|---|
| AI 模型配置 | `ConfigController` 已有 `/admin/config/models` CRUD 与测试接口 | `getModelConfigs/create/update/delete/testModelConfig` | 已覆盖主要接口 |
| Prompt 模板 | `ConfigController` 已有 `/admin/config/prompts` CRUD 与启停接口 | `getPromptConfigs/create/update/delete/setPromptEnabled` | 已覆盖主要接口 |
| 系统参数 | `ConfigController` 已有 `/admin/config/system` 查询、保存、删除 | `getSystemConfigs/saveSystemConfig/deleteSystemConfig` | 已覆盖主要接口 |
| 日志审计 | `LogController` 已有 operations/logins/ai-calls/alerts/export | `getOperationLogs/getLoginLogs/getAiCallLogs/getAlerts/exportAdminLog` | 已覆盖主要接口 |
| 公告管理 | `NoticeController` 已有 `/admin/announcements` CRUD | `getAdminAnnouncements/create/update/delete` | 已覆盖主要接口 |

注意：这些接口虽然存在，但仍建议保留后端安全层 `/admin/**` 统一权限收口，避免只依赖 Service 内部校验。

---

## DONE-03 用户资料、知识库详情、工作台、搜索、摘要、通知接口已存在

| 模块 | 当前后端状态 | 前端对应调用 | 结论 |
|---|---|---|---|
| 当前用户 | `GET /users/me` | `getCurrentUser()` | 已存在 |
| 修改资料 | `PUT /users/me` | `updateCurrentUser()` | 已存在 |
| 修改密码 | `PUT /users/me/password` | `updatePassword()` | 已存在 |
| 工作台 | `GET /dashboard/overview` | `getDashboardOverview()` | 已存在 |
| 知识库详情聚合 | `GET /knowledge-bases/{id}/detail` | `getKnowledgeBaseDetail()` | 已存在 |
| 语义搜索 | `GET /search/semantic` | `semanticSearch()` | 已存在 |
| 关键词搜索 | `GET /search/keyword` | `keywordSearch()` | 已存在 |
| 文档摘要 | `POST /summaries/document` | `summarizeDocument()` | 已存在 |
| 知识库摘要 | `POST /summaries/knowledge-base` | `summarizeKnowledgeBase()` | 已存在 |
| 通知中心 | `/notifications/*` | 通知相关 API | 已存在 |

---

# 二、P0：当前仍阻塞 MVP 联调 / 演示的问题

## P0-01 前端 `/api` 代理仍缺失

| 字段 | 内容 |
|---|---|
| 所属模块 | 前端本地联调 |
| 前端文件 | `vite.config.ts`、`src/api/request.ts` |
| 后端文件 | `application.yml` |
| 当前现象 | 前端 `request.ts` 使用 `baseURL: '/api'`，后端已有 `/api` context-path，但 `vite.config.ts` 没有 `server.proxy`。 |
| 影响 | `npm run dev` 时 `/api/...` 默认请求 Vite 服务，而不是后端 `http://localhost:8080`，登录、知识库、文档、问答都会联调失败。 |
| 是否阻塞 MVP | 是 |
| 建议修复位置 | 前端 |

建议修改：

```ts
// vite.config.ts
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

验收：

```bash
npm run dev
curl -X POST http://localhost:5173/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"account":"admin","password":"admin123"}'
```

---

## P0-02 后端安全层仍未统一限制 `/admin/**` 为 ADMIN

| 字段 | 内容 |
|---|---|
| 所属模块 | 后台权限 / 安全认证 |
| 前端文件 | `src/router/index.ts` |
| 后端文件 | `src/main/java/com/knowflow/security/SecurityConfig.java`、`JwtAuthenticationFilter.java` |
| 当前现象 | `SecurityConfig` 当前只放行 `/auth/register`、`/auth/login`、`/admin/auth/login`，其余接口只要求 `authenticated()`，没有 `.requestMatchers("/admin/**").hasRole("ADMIN")`。 |
| 现有缓解 | 多个后台 Service 内部调用了 `SecurityUtils.requireAdmin()`，但不是安全层统一收口。 |
| 影响 | 权限规则分散，后续新增 `/admin/**` 接口时容易漏掉 `requireAdmin()`；前端路由限制无法替代后端安全控制。 |
| 是否阻塞 MVP | 是，安全风险 |
| 建议修复位置 | 后端 |

建议修改：

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/auth/register", "/auth/login", "/admin/auth/login").permitAll()
    .requestMatchers("/admin/**").hasRole("ADMIN")
    .anyRequest().authenticated()
)
```

验收：

```text
[ ] 未登录访问 /api/admin/users 返回 401
[ ] 普通 USER 访问 /api/admin/users 返回 403
[ ] ADMIN 访问 /api/admin/users 返回 200
[ ] /api/admin/auth/login 仍允许未登录访问
```

---

## P0-03 用户侧 `/documents` 分页查询仍与前端参数不一致

| 字段 | 内容 |
|---|---|
| 所属模块 | 用户侧文档管理 |
| 前端文件 | `src/api/knowledge.ts` -> `getDocumentPage()`；`DocumentManageView.vue` |
| 后端文件 | `DocumentController.java`、`DocumentService.java`、`DocumentRepository.java` |
| 当前前端参数 | `knowledgeBaseId?`、`keyword?`、`parseStatus?`、`embeddingStatus?`、`pageNo?`、`pageSize?` |
| 当前后端参数 | `knowledgeBaseId` 必填，只支持 `keyword/pageNo/pageSize` |
| 影响 | 全局文档管理页不选知识库时会 400；解析状态和向量化状态筛选无效。 |
| 是否阻塞 MVP | 是 |
| 建议修复位置 | 后端优先 |

建议后端支持：

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

Repository 查询必须始终包含：

```text
user_id = 当前登录用户 ID
```

验收：

```text
[ ] GET /api/documents?pageNo=1&pageSize=10 可以返回当前用户全部文档
[ ] GET /api/documents?knowledgeBaseId=1 只返回该知识库文档
[ ] GET /api/documents?parseStatus=FAILED 只返回当前用户解析失败文档
[ ] A 用户不能查到 B 用户文档
```

---

## P0-04 后台文档列表筛选参数仍不完整

| 字段 | 内容 |
|---|---|
| 所属模块 | 后台文档管理 |
| 前端文件 | `src/api/knowledge.ts` -> `getAdminDocuments()` |
| 后端文件 | `AdminController.java`、`AdminService.java`、`DocumentRepository.java` |
| 当前前端参数 | `keyword?`、`knowledgeBaseId?`、`parseStatus?`、`pageNo?`、`pageSize?` |
| 当前后端参数 | `keyword/pageNo/pageSize` |
| 影响 | 后台文档管理页的知识库筛选、解析状态筛选无效。 |
| 是否阻塞 MVP | 是，影响后台演示 |
| 建议修复位置 | 后端 |

建议后端支持：

```text
GET /admin/documents
参数：keyword、knowledgeBaseId、parseStatus、fileType、pageNo、pageSize
```

验收：

```text
[ ] 管理员可按 keyword 查询文档
[ ] 管理员可按 knowledgeBaseId 查询文档
[ ] 管理员可按 parseStatus 查询文档
[ ] 管理员可按 fileType 查询文档，若前端暂未传 fileType，可先预留
```

---

## P0-05 后台知识库列表缺少 `status` 筛选

| 字段 | 内容 |
|---|---|
| 所属模块 | 后台知识库管理 |
| 前端文件 | `src/api/knowledge.ts` -> `getAdminKnowledgeBases()` |
| 后端文件 | `AdminController.java`、`AdminService.java`、`KnowledgeBaseRepository.java` |
| 当前前端参数 | `keyword?`、`status?`、`pageNo?`、`pageSize?` |
| 当前后端参数 | `keyword/pageNo/pageSize` |
| 影响 | 后台知识库状态筛选无效；PRD 要求后台知识库列表支持按状态筛选。 |
| 是否阻塞 MVP | 是，影响后台演示 |
| 建议修复位置 | 后端 |

建议后端支持：

```text
GET /admin/knowledge-bases?keyword=&status=NORMAL&pageNo=1&pageSize=10
```

---

## P0-06 后台文档任务列表筛选参数仍不完整

| 字段 | 内容 |
|---|---|
| 所属模块 | 文档处理任务管理 |
| 前端文件 | `src/api/knowledge.ts` -> `getDocumentTasks()` |
| 后端文件 | `AdminController.java`、`AdminService.java`、`DocumentProcessTaskRepository.java`、`DocumentProcessTask.java` |
| 当前前端参数 | `status?`、`taskType?`、`keyword?`、`pageNo?`、`pageSize?` |
| 当前后端参数 | `pageNo/pageSize` |
| 影响 | 任务状态筛选、关键词筛选无效；后台任务管理无法有效定位失败任务。 |
| 是否阻塞 MVP | 是，PRD 后台任务管理要求查看失败任务和失败原因 |
| 建议修复位置 | 后端 |

建议处理：

```text
1. 本轮先支持 status、keyword。
2. taskType 当前 DocumentProcessTask 实体没有字段，暂不硬做；要么前端隐藏 taskType 筛选，要么后续扩表增加 task_type。
```

---

## P0-07 后台任务重试接口 `/admin/document-tasks/{id}/retry` 仍缺失

| 字段 | 内容 |
|---|---|
| 所属模块 | 文档处理任务管理 |
| 前端文件 | `src/api/knowledge.ts` -> `retryDocumentTask()` |
| 后端文件 | `AdminController.java`、`AdminService.java`、`DocumentService.java` |
| 当前现象 | 前端调用 `POST /admin/document-tasks/{id}/retry`，后端只有 `GET /admin/document-tasks` 和 `GET /admin/document-tasks/{id}`。 |
| 影响 | 后台任务列表/详情的“重试任务”按钮不可用。 |
| 是否阻塞 MVP | 是 |
| 建议修复位置 | 后端 |

建议实现：

```java
@PostMapping("/document-tasks/{id}/retry")
public ApiResponse<DocumentTaskVO> retryTask(@PathVariable Long id) {
    return ApiResponse.ok(adminService.retryTask(id));
}
```

服务逻辑：

```text
taskId -> 查询任务 -> 获取 documentId -> 复用 documentService.adminRetry(documentId)
```

注意：

```text
[ ] 防止同一个 document 已有 PROCESSING/PENDING 任务时重复创建
[ ] 明确返回 DocumentTaskVO 还是 DocumentVO，建议返回最新任务信息
```

---

## P0-08 `application.yml` 仍包含数据库密码和固定 JWT Secret

| 字段 | 内容 |
|---|---|
| 所属模块 | 配置安全 / 作品集规范 |
| 后端文件 | `src/main/resources/application.yml` |
| 当前现象 | `application.yml` 中存在明文 MySQL 密码和固定 JWT Secret。 |
| 影响 | 公开仓库不适合提交真实密码；JWT Secret 固定会降低安全性；面试时容易被追问配置安全。 |
| 是否阻塞 MVP | 不阻塞功能，但属于 P0 安全整改 |
| 建议修复位置 | 后端配置 |

建议改成：

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/knowflow_ai?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}

knowflow:
  jwt:
    secret: ${JWT_SECRET:please-change-this-secret-in-dev}
```

同时新增：

```text
application.example.yml
README 本地环境变量说明
```

---

# 三、P1：MVP 功能完整度与展示质量问题

## P1-01 后台看板字段偏简化，未覆盖 PRD 核心指标

| 字段 | 内容 |
|---|---|
| 所属模块 | 后台数据看板 |
| 后端现状 | `AdminOverviewVO` 只有 `userCount`、`knowledgeBaseCount`、`documentCount`、`failedDocumentCount`、`disabledKnowledgeBaseCount`。 |
| PRD 要求 | 用户总数、知识库总数、文档总数、问答总次数、今日新增用户数、今日新增文档数、今日问答次数、处理状态统计、最近操作记录。 |
| 影响 | 后台首页能展示，但信息量不足，作品集观感偏弱。 |
| 建议 | 补 `chatCount/todayUserCount/todayDocumentCount/todayChatCount/parseStatusStats/recentUsers/recentDocuments/recentFailedTasks/recentAiErrors`。 |

---

## P1-02 用户工作台缺少“累计问答次数”或字段语义不够明确

| 字段 | 内容 |
|---|---|
| 所属模块 | 用户工作台 |
| 后端现状 | `DashboardVO` 有 `knowledgeBaseCount`、`documentCount`、`chatSessionCount`、最近知识库/文档/会话。 |
| PRD 要求 | 展示累计问答次数、最近问答记录。 |
| 影响 | 如果前端把 `chatSessionCount` 当问答次数，会存在语义偏差。 |
| 建议 | 增加 `questionCount` 或 `chatMessageCount`，保留 `chatSessionCount` 用作会话数量。 |

---

## P1-03 文档任务实体缺少 `taskType`、开始/完成时间、执行日志摘要

| 字段 | 内容 |
|---|---|
| 所属模块 | 文档处理任务 |
| 后端现状 | `DocumentProcessTask` 只有 `userId`、`knowledgeBaseId`、`documentId`、`status`、`failReason`。 |
| PRD 要求 | 任务类型、关联文档、执行日志、异常堆栈摘要、创建时间和完成时间。 |
| 影响 | 后台任务详情页无法完整展示“解析任务 / 向量化任务 / 失败原因 / 执行耗时”。 |
| 建议 | 下一阶段扩展 `task_type`、`started_at`、`finished_at`、`log_summary`、`stack_trace_summary`。 |

---

## P1-04 日志审计字段偏简化

| 日志类型 | 当前实体字段 | PRD/前端期望 | 建议 |
|---|---|---|---|
| 登录日志 | `userId/account/success/message` | IP、UserAgent、失败原因 | 增加 `ip`、`userAgent`、`failReason` 或统一使用 `message` 映射 |
| 操作日志 | `userId/action/targetType/targetId/detail` | 操作模块、方法、路径、IP、结果 | 增加 `module/method/path/ip/success/result` 或前端适配当前字段 |
| AI 调用日志 | `modelName/callType/elapsedMs/success/failReason` | token 统计、响应耗时、调用场景 | MVP 可保留，后续加 `promptTokens/completionTokens/totalTokens` |

---

## P1-05 知识库/文档 VO 与前端展示字段存在兼容风险

| 对象 | 后端字段 | 前端类型/页面可能使用 | 风险 |
|---|---|---|---|
| 知识库 | `documentCount` | `docCount`、`documentCount` 混用风险 | 列表文档数量可能显示为空 |
| 文档 | `fileType/fileSize/originalName` | `type/size/knowledgeBaseName` 也在类型中出现 | 文档类型、大小、所属知识库名称可能显示不完整 |
| 用户 | `createTime` | `createTime/createdAt` 均有兼容 | 风险较低 |

建议：

```text
1. 前端统一通过 view-adapters 取值，已有部分适配。
2. 后端 DocumentVO 可补 knowledgeBaseName、chunkCount。
3. 后端 KnowledgeBaseVO 可兼容输出 documentCount，前端不要再依赖 docCount。
```

---

## P1-06 文档删除/重试当前清理了 chunk，但向量存储抽象仍需收口

| 字段 | 内容 |
|---|---|
| 所属模块 | 文档管理 / RAG 索引 |
| 当前现状 | `DocumentService.delete/retry/adminDelete/adminRetry` 会删除 chunk，并重置状态。当前 README 说明 Embedding 存 MySQL、Java 计算相似度。 |
| 风险 | 如果后续接 Qdrant/Milvus/pgvector，删除文档时必须同步删除向量索引，否则会召回已删除文档。 |
| 建议 | 抽象 `VectorStore.deleteByDocumentId(documentId)`，即使当前 MySQL 实现为空或删除 chunk 即可，也保留接口。 |

---

## P1-07 后台配置接口存在，但 API Key 存储与脱敏需要补强

| 字段 | 内容 |
|---|---|
| 所属模块 | AI 模型配置 |
| 当前现状 | `ConfigService` 可保存 `apiKey`，但需要确认是否加密存储和脱敏展示。 |
| 影响 | 作品集展示时，后台配置页容易暴露 Key。 |
| 建议 | 入库前加密或至少不回显完整 Key；VO 返回 `sk-****abcd`；日志禁止打印 Key。 |

---

## P1-08 注册表单中的 nickname 可能被前端丢弃

| 字段 | 内容 |
|---|---|
| 所属模块 | 注册/用户资料 |
| 前端现状 | `RegisterForm` 类型包含 `nickname`。 |
| 前端 API | `register()` 只发送 `username/email/password/confirmPassword`，没有发送 `nickname`。 |
| 后端 DTO | `RegisterRequest` 也没有 `nickname`。 |
| 影响 | 如果注册页有昵称输入，提交后不会保存。 |
| 建议 | 二选一：MVP 删除注册页昵称输入；或前后端都补 `nickname`。 |

---

## P1-09 后台配置/日志/公告接口虽然存在，但仍需真实页面联调

| 模块 | 接口状态 | 仍需验证 |
|---|---|---|
| AI 模型配置 | 接口存在 | 创建、编辑、删除、默认模型、测试调用是否和前端字段一致 |
| Prompt 模板 | 接口存在 | `code/name/scene/content/enabled/defaultTemplate` 字段是否一致 |
| 系统参数 | 接口存在 | `configKey/configValue/description` 字段是否一致 |
| 日志审计 | 接口存在 | 前端字段如 `ip/path/module/result` 是否能显示或适配为空 |
| 公告管理 | 接口存在 | 前端 `enabled` 与 PRD 状态“草稿/发布/下线”的语义差异 |

---

# 四、P2：非阻塞但建议整理

## P2-01 前端仍暴露大量扩展功能占位路由

| 字段 | 内容 |
|---|---|
| 所属模块 | 前端路由 / MVP 范围控制 |
| 当前现象 | 路由中存在知识库分享、协作、模板、归档、批量上传、版本管理、高级问答、模型路由、Token 成本统计等扩展占位。 |
| 影响 | 演示时容易被误认为“很多功能没做完”。 |
| 建议 | MVP 演示时隐藏扩展菜单；README 中作为后续规划展示。 |

---

## P2-02 后端源码被格式化成大量单行文件，可维护性较差

| 字段 | 内容 |
|---|---|
| 所属模块 | 工程规范 |
| 当前现象 | 多个 Java/Vue/TS 文件在 raw 中呈现为一行或极少数长行。 |
| 影响 | Code Review、Git diff、定位问题困难；AI 修改时更容易产生大范围 diff。 |
| 建议 | 后端统一使用 Spotless/Google Java Format，前端使用 Prettier；先格式化，再进行下一轮功能修改。 |

---

## P2-03 ChatService、DocumentService 职责仍偏重

| 字段 | 内容 |
|---|---|
| 所属模块 | 后端架构 |
| 当前现象 | `DocumentService` 同时负责上传、列表、详情、删除、预览、下载、重试、后台操作；`ChatService` 预计承担 RAG、会话、导出、反馈等。 |
| 影响 | 后续接真实向量库、真实 LLM、流式输出时会变难维护。 |
| 建议 | MVP 稳定后拆：`FileStorageService`、`DocumentRetryService`、`RagRetrievalService`、`PromptBuilder`、`AnswerGenerationService`、`ChatExportService`。 |

---

# 五、前后端接口对照更新表

## 5.1 认证与用户

| 功能 | 前端调用 | 后端接口 | 当前状态 | 处理建议 |
|---|---|---|---|---|
| 用户注册 | `POST /auth/register` | `POST /auth/register` | 基本一致 | 如需要昵称，前后端补 `nickname` |
| 用户登录 | `POST /auth/login` | `POST /auth/login` | 一致 | 字段 `account/password` 已一致 |
| 管理员登录 | `POST /admin/auth/login` | `POST /admin/auth/login` | 一致 | 需确保非 ADMIN 账号不能登录后台 |
| 退出登录 | `DELETE /auth/logout` | `DELETE /auth/logout` | 一致 | JWT 无状态，前端清 token 即可 |
| 当前用户 | `GET /users/me` | `GET /users/me` | 一致 | 已存在 |
| 修改资料 | `PUT /users/me` | `PUT /users/me` | 一致 | 已存在 |
| 修改密码 | `PUT /users/me/password` | `PUT /users/me/password` | 一致 | 已存在 |

## 5.2 工作台与知识库

| 功能 | 前端调用 | 后端接口 | 当前状态 | 处理建议 |
|---|---|---|---|---|
| 用户工作台 | `GET /dashboard/overview` | `GET /dashboard/overview` | 接口存在 | 补累计问答次数更好 |
| 知识库分页 | `GET /knowledge-bases` | `GET /knowledge-bases` | 一致 | 确认排序值 `sortBy` 映射 |
| 知识库详情 | `GET /knowledge-bases/{id}` | `GET /knowledge-bases/{id}` | 一致 | 校验归属 |
| 知识库聚合详情 | `GET /knowledge-bases/{id}/detail` | `GET /knowledge-bases/{id}/detail` | 已存在 | 继续联调字段 |
| 后台知识库分页 | `GET /admin/knowledge-bases?status=` | `GET /admin/knowledge-bases` | 参数不一致 | 后端补 `status` |

## 5.3 文档与任务

| 功能 | 前端调用 | 后端接口 | 当前状态 | 处理建议 |
|---|---|---|---|---|
| 上传文档 | `POST /documents/upload` | `POST /documents/upload` | 一致 | 继续测试文件大小/格式/归属 |
| 用户文档分页 | `GET /documents` | `GET /documents` | 参数不一致 | 后端补可选知识库和状态筛选 |
| 文档详情 | `GET /documents/{id}` | `GET /documents/{id}` | 一致 | 校验归属 |
| 文档重试 | `POST /documents/{id}/retry` | `POST /documents/{id}/retry` | 一致 | 验证清理旧 chunk |
| 后台文档分页 | `GET /admin/documents` | `GET /admin/documents` | 参数不一致 | 后端补筛选 |
| 后台文档重试 | `POST /admin/documents/{id}/retry` | `POST /admin/documents/{id}/retry` | 一致 | 已存在 |
| 任务列表 | `GET /admin/document-tasks` | `GET /admin/document-tasks` | 参数不一致 | 后端补 `status/keyword`，taskType 后置 |
| 任务重试 | `POST /admin/document-tasks/{id}/retry` | 缺失 | 不一致 | 后端补接口 |

## 5.4 问答、搜索、摘要、通知、后台配置

| 功能 | 前端调用 | 后端接口 | 当前状态 | 处理建议 |
|---|---|---|---|---|
| 单知识库问答 | `POST /chat/ask` | `POST /chat/ask` | 一致 | MVP 核心，重点跑通 |
| 会话列表 | `GET /chat/sessions` | `GET /chat/sessions` | 一致 | 校验 userId |
| 会话消息 | `GET /chat/sessions/{id}/messages` | 同路径 | 一致 | 校验 userId |
| 语义搜索 | `GET /search/semantic` | 同路径 | 一致 | 校验返回字段与 userId 过滤 |
| 关键词搜索 | `GET /search/keyword` | 同路径 | 一致 | 校验返回字段 |
| 文档摘要 | `POST /summaries/document` | 同路径 | 一致 | 验证是否落库或只是即时生成 |
| 知识库摘要 | `POST /summaries/knowledge-base` | 同路径 | 一致 | 验证空知识库情况 |
| 通知中心 | `/notifications/*` | 同路径 | 一致 | 校验 userId |
| 后台配置 | `/admin/config/*` | 同路径 | 已存在 | 继续字段联调 |
| 日志审计 | `/admin/logs/*` | 同路径 | 已存在 | 继续字段联调 |
| 公告管理 | `/admin/announcements` | 同路径 | 已存在 | 继续字段联调 |

---

# 六、建议修复顺序

## 第 1 批：前后端联调基础

```text
[ ] 前端 vite.config.ts 增加 /api proxy
[ ] 后端 SecurityConfig 增加 /admin/** hasRole("ADMIN")
[ ] 验证 USER 访问后台 403，ADMIN 访问后台 200
```

## 第 2 批：文档和任务接口参数

```text
[ ] /documents 支持 knowledgeBaseId 可选
[ ] /documents 支持 parseStatus、embeddingStatus
[ ] /admin/documents 支持 knowledgeBaseId、parseStatus、fileType
[ ] /admin/knowledge-bases 支持 status
[ ] /admin/document-tasks 支持 status、keyword
[ ] 新增 POST /admin/document-tasks/{id}/retry
```

## 第 3 批：MVP 展示质量补齐

```text
[ ] 后台看板补今日指标、问答次数、处理状态统计
[ ] 任务详情补 taskType / 时间 / 日志摘要，或前端隐藏暂不支持字段
[ ] 日志字段前后端适配
[ ] 配置页 API Key 脱敏
[ ] README 增加本地启动、默认账号、环境变量说明
```

## 第 4 批：真实主链路验收

```text
[ ] 注册普通用户
[ ] 登录普通用户
[ ] 创建知识库
[ ] 上传 PDF/DOCX/TXT/MD
[ ] 上传后创建文档记录和处理任务
[ ] 文档解析成功后生成 chunk / embedding
[ ] 基于知识库提问
[ ] 回答返回 references
[ ] 查看问答历史和消息详情
[ ] 管理员登录后台
[ ] 管理员查看用户/知识库/文档/任务/日志/配置
[ ] 普通用户无法访问后台
```

---

# 七、给 Codex 的下一步提示词

## 7.1 第一批修复提示词

```text
请基于当前 ai-knowledge-java 与 ai-knowledge-vue，只修复第一批 P0 联调基础问题。

本次只做：
1. 前端 vite.config.ts 增加 /api 代理到 http://localhost:8080。
2. 后端 SecurityConfig 增加 /admin/** 必须 ADMIN 权限。

约束：
1. 不要修改业务接口路径。
2. 不要修改数据库表结构。
3. 不要调整包结构。
4. 不要顺手实现其他功能。
5. /admin/auth/login 必须继续 permitAll。
6. 普通业务接口保持 authenticated。

完成后运行：
- 前端：npm run build
- 后端：mvn clean compile

最后输出：
- 修改文件清单
- 编译结果
- 验证普通用户访问 /api/admin/users 返回 403 的测试步骤
```

## 7.2 第二批修复提示词

```text
请只修复文档、知识库、任务接口参数不一致问题。

本次只做：
1. /documents 支持 knowledgeBaseId 可选、parseStatus、embeddingStatus。
2. /admin/documents 支持 knowledgeBaseId、parseStatus、fileType。
3. /admin/knowledge-bases 支持 status。
4. /admin/document-tasks 支持 status、keyword。
5. 新增 POST /admin/document-tasks/{id}/retry。

约束：
1. 用户侧查询必须始终按当前 userId 过滤。
2. 后台接口必须只有 ADMIN 可访问。
3. taskType 当前实体没有字段，本次不要扩表，不要硬做。
4. 不要修改前端 API 字段名。
5. 不要实现扩展功能。
6. 修改后运行 mvn clean compile。

最后输出：
- 每个接口的参数清单
- 修改文件清单
- 编译结果
- Postman/Apifox 测试用例
```

---

# 八、当前 MVP 可演示状态判断

| 验收项 | 当前判断 | 说明 |
|---|---|---|
| 注册/登录 | 基本可用 | 需本地验证 |
| 用户信息管理 | 基本可用 | 接口已存在 |
| 知识库 CRUD | 基本可用 | 接口已存在，需验证字段和权限 |
| 文档上传 | 基本可用 | 接口已存在，需验证解析任务 |
| 文档列表/筛选 | 不稳定 | 参数不一致 |
| 文档解析/重试 | 基本可用 | 需验证失败重试和重复任务 |
| 知识库问答 | 需重点验收 | 接口存在，但必须真实跑上传文档后问答 |
| 引用来源 | 需重点验收 | 需验证 references 字段 |
| 问答历史 | 基本可用 | 接口存在，需验证会话归属 |
| 后台登录 | 基本可用 | 需验证非 ADMIN 禁止后台登录 |
| 后台用户/知识库/文档 | 部分可用 | 筛选参数不完整 |
| 后台任务管理 | 不完整 | 缺任务重试接口和筛选 |
| 后台配置/日志/公告 | 接口已存在 | 需字段级联调 |
| 安全权限 | 不完整 | `/admin/**` 安全层未统一收口 |
| 本地联调 | 不完整 | Vite 代理缺失 |

---

## 最终结论

当前项目已经从“接口缺失较多”推进到“接口主体基本具备，但联调参数和权限收口仍需修”的阶段。

下一步不要继续新增功能，优先处理：

```text
1. vite.config.ts /api 代理
2. SecurityConfig /admin/** 权限收口
3. /documents 参数对齐
4. /admin/documents、/admin/knowledge-bases、/admin/document-tasks 筛选参数对齐
5. /admin/document-tasks/{id}/retry
```

这 5 项完成后，再进入真实 MVP 主链路联调：注册登录 → 创建知识库 → 上传文档 → 解析成功 → 提问 → 引用来源 → 历史会话 → 后台查看任务与日志。
