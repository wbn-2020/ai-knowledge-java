# KnowFlow AI：AI 协作开发 SOP（Java + Vue 全栈）

> 角色定位：你是项目 Owner；AI 编程助手是“初级开发 + 代码生成器 + Review 助手”；我建议你不要把 AI 当成“全权架构师”。所有架构边界、数据模型、API 契约、任务拆分和验收标准，都应先固化成文档，再让 AI 分模块生成代码。

---

## 0. 总体原则

### 0.1 不要用“一句话生成整个项目”

错误方式：

```text
根据 PRD 帮我生成 KnowFlow AI 完整项目代码。
```

这种方式容易导致：

- 工程结构混乱；
- 表结构、接口、前端字段不一致；
- AI 自动脑补不存在的功能；
- 后期调试成本远高于手写；
- RAG、文档解析、异步任务、权限校验等核心链路失控。

正确方式：

```text
每次只让 AI 完成一个明确模块、一个明确接口、一个明确页面或一个明确服务类。
每次输入都带上：项目约束 + 当前模块 PRD 切片 + 已存在代码结构 + 输出要求 + 验收标准。
```

---

## 1. 上下文管理 SOP：如何切片投喂 PRD

### 1.1 建议先把 PRD 拆成 8 个上下文文档

在项目根目录创建：

```text
docs/
├── 00-project-brief.md              # 项目总览，一页纸
├── 01-mvp-scope.md                  # MVP 范围，只保留第一版必须做的功能
├── 02-domain-glossary.md            # 领域对象、状态枚举、术语表
├── 03-database-design.md            # 数据库表、字段、索引、状态流转
├── 04-api-contract.md               # 前后端 API 契约
├── 05-rag-pipeline.md               # 文档解析、切片、向量化、检索、问答流程
├── 06-frontend-pages.md             # 前台/后台页面清单与路由
├── 07-task-cards.md                 # 分阶段任务卡
└── 08-ai-coding-rules.md            # AI 编码规范与禁止事项
```

这些文档是你和 AI 协作的“项目记忆”。不要每次把完整 PRD 贴给 AI，而是根据任务引用对应文档。

---

### 1.2 每次投喂 AI 时，只给 3 层上下文

#### 第 1 层：全局项目约束，固定不变

每次对 Codex 或其他 AI 编程助手下指令时，建议都带上这段：

```text
你正在参与开发 KnowFlow AI：个人知识库与智能文档问答平台。
技术栈：Spring Boot 3.x、Java 17、MyBatis-Plus 或 JPA、MySQL、Redis、Vue 3、TypeScript、Element Plus。
项目采用前后端分离架构。
第一版只做 MVP，不要实现扩展功能。
后端必须分层：Controller、Service、ServiceImpl、Mapper/Repository、Entity、DTO、VO、Convert。
所有接口必须做用户身份校验和数据归属校验。
不要修改与当前任务无关的文件。
如果发现现有设计缺失，请先说明问题，再给出最小修改方案。
```

#### 第 2 层：当前模块 PRD 切片

例如你要开发“知识库管理模块”，只投喂以下内容：

```text
当前模块：知识库管理。
MVP 功能：创建知识库、知识库列表、知识库详情、编辑知识库、删除知识库、状态管理、基础权限控制。
约束：
1. 用户只能访问自己创建的知识库。
2. 删除建议使用逻辑删除。
3. 知识库状态包括：正常、处理中、解析失败、已禁用。
4. 列表支持关键词搜索、创建时间排序、更新时间排序。
```

不要把文档上传、RAG 问答、后台管理一起贴进去。

#### 第 3 层：当前代码现状与目标

```text
当前已有代码：
- 已完成用户登录、JWT 鉴权。
- 当前用户 ID 可通过 SecurityUtils.getCurrentUserId() 获取。
- 已有统一响应 Result<T>。
- 已有分页对象 PageResult<T>。

本次任务：
请只实现知识库 CRUD 后端接口，不要实现文档上传和问答。
输出要求：
1. 先列出将新增/修改的文件清单。
2. 再生成代码。
3. 最后给出接口测试用例。
```

---

### 1.3 建立“项目上下文胶囊”

每完成一个阶段，让 AI 帮你更新一段不超过 1000 字的上下文摘要，保存到：

```text
docs/current-context.md
```

模板：

```markdown
# 当前项目上下文

## 已完成
- 用户注册、登录、JWT 鉴权
- 知识库 CRUD
- 文档元数据表设计

## 当前技术约定
- Java 17
- Spring Boot 3.x
- MySQL
- Vue 3 + TypeScript + Element Plus
- 后端统一响应：Result<T>
- 当前用户 ID 获取方式：SecurityUtils.getCurrentUserId()

## 关键领域对象
- User
- KnowledgeBase
- Document
- DocumentChunk
- ChatSession
- ChatMessage
- AiModelConfig

## 关键状态枚举
- KnowledgeBaseStatus: NORMAL, PROCESSING, FAILED, DISABLED
- DocumentParseStatus: PENDING, PARSING, SUCCESS, FAILED
- EmbeddingStatus: PENDING, PROCESSING, SUCCESS, FAILED

## 下一步
- 开发文档上传与异步解析任务
```

以后每次开新对话或切换 AI 工具时，先贴这个上下文胶囊，再贴当前任务。

---

## 2. PRD 切片建议

### 2.1 MVP 总切片

第一版只保留这条闭环：

```text
注册/登录
  -> 创建知识库
    -> 上传文档
      -> 文档解析
        -> 文本切片
          -> 向量化
            -> 知识库问答
              -> 展示回答与引用来源
                -> 查看问答历史
                  -> 管理员查看用户、知识库、文档、AI 配置、日志
```

扩展功能暂时全部冻结，例如：

- 第三方登录；
- 知识库协作；
- 知识库分享；
- 批量上传；
- OCR；
- 表格解析；
- 流式输出；
- 多知识库问答；
- Prompt 多模式；
- Token 成本统计；
- 复杂 RBAC。

---

### 2.2 模块切片顺序

#### Slice A：用户与权限

包含：

- 注册；
- 登录；
- 退出；
- 当前用户信息；
- 修改资料；
- 修改密码；
- 管理员登录；
- 普通用户与管理员权限区分。

不要同时开发知识库、文档和 RAG。

#### Slice B：知识库管理

包含：

- 创建知识库；
- 列表；
- 详情；
- 编辑；
- 删除；
- 状态字段；
- 用户归属校验。

#### Slice C：文档管理

包含：

- 上传文档；
- 文件格式校验；
- 文件大小校验；
- 文档元数据记录；
- 文档列表；
- 文档详情；
- 删除文档；
- 重新解析入口。

#### Slice D：文档解析与索引

包含：

- PDF / DOCX / TXT / MD 文本提取；
- 文本切片；
- 切片入库；
- Embedding 调用；
- 向量入库；
- 解析任务状态流转；
- 失败原因记录。

#### Slice E：智能问答 RAG

包含：

- 选择知识库提问；
- 问题向量化；
- 向量召回 TopK；
- 构造 Prompt；
- 调用大模型；
- 保存会话和消息；
- 返回回答和引用来源。

#### Slice F：前端普通用户端

包含：

- 登录/注册页；
- 工作台；
- 知识库列表；
- 知识库详情；
- 文档上传；
- 文档列表；
- 问答页面；
- 历史会话页面；
- 用户设置页面。

#### Slice G：后台管理端

包含：

- 后台登录；
- 数据看板；
- 用户管理；
- 知识库管理；
- 文档管理；
- AI 模型配置；
- Prompt 模板管理；
- 系统任务；
- 日志审计。

---

## 3. 推荐开发顺序

## 阶段 0：冻结 MVP 边界与工程规范

### 目标

先避免项目失控。明确第一版只做什么，不做什么。

### 让 AI 做什么

```text
请基于 KnowFlow AI PRD，帮我提炼第一版 MVP 范围。
要求：
1. 只保留第一版必须完成的功能。
2. 明确暂不实现的扩展功能。
3. 输出模块清单、页面清单、接口清单、核心业务流程。
4. 不要写代码。
```

### 产出物

```text
docs/01-mvp-scope.md
docs/08-ai-coding-rules.md
```

### 验收标准

- MVP 范围不超过 6 条主链路；
- 每个模块都有“做 / 不做”边界；
- AI 后续不能随意添加扩展功能。

---

## 阶段 1：领域建模与数据库设计

### 目标

先把业务对象和数据关系定下来。数据库是后续所有代码的地基。

### 推荐核心表

```text
sys_user                    用户表
sys_role                    角色表，可 MVP 简化
sys_user_role               用户角色关系，可 MVP 简化
knowledge_base              知识库表
document                    文档表
document_chunk              文档切片表
document_process_task       文档处理任务表
vector_index_ref            向量索引引用表，可选
chat_session                问答会话表
chat_message                问答消息表
chat_message_reference      回答引用来源表
ai_model_config             AI 模型配置表
prompt_template             Prompt 模板表
operation_log               操作日志表
login_log                   登录日志表
ai_call_log                 AI 调用日志表
system_config               系统配置表
notification                通知表
```

### 重要建模原则

#### 1. 业务数据和向量数据分开

推荐：

```text
MySQL：存业务元数据、文档切片文本、任务状态、会话记录。
向量库：存 chunkId 与 embedding 向量。
```

MVP 可选方案：

- 方案 A：MySQL + Qdrant，推荐作为作品集展示；
- 方案 B：MySQL + Milvus，偏工程化但部署稍重；
- 方案 C：MySQL 只存切片，向量检索先用模拟服务，适合最早期打通流程；
- 不建议：把高维向量直接塞进 MySQL 再自己计算相似度，后期不利于展示 RAG 工程能力。

#### 2. 所有用户数据表必须带 user_id

至少这些表必须有 `user_id`：

```text
knowledge_base
document
chat_session
chat_message
notification
operation_log
```

这样才能做数据归属校验。

#### 3. 状态字段必须枚举化

例如：

```text
DocumentParseStatus:
- PENDING
- PARSING
- SUCCESS
- FAILED

EmbeddingStatus:
- PENDING
- PROCESSING
- SUCCESS
- FAILED
```

不要让 AI 随意写字符串，例如：`done`、`ok`、`successed`、`finish`。

### 给 AI 的指令模板

```text
你现在是后端架构师。请基于 KnowFlow AI MVP 范围设计数据库。

约束：
1. 数据库使用 MySQL 8。
2. 所有表使用 snake_case。
3. 主键统一为 bigint。
4. 所有业务表包含 create_time、update_time、deleted。
5. 涉及用户私有数据的表必须包含 user_id。
6. 状态字段必须给出枚举值说明。
7. 输出建表 SQL、字段说明、索引说明、表关系说明。
8. 不要写 Java 代码。
```

### 验收标准

- 表之间关系清晰；
- 每个业务对象有主键和外键逻辑；
- 查询高频字段有索引；
- 软删除字段统一；
- 状态枚举统一；
- 可以支撑 PRD 的 MVP 闭环。

---

## 阶段 2：基础工程脚手架

### 目标

搭建稳定的前后端项目骨架，不急着写业务。

### 后端工程结构建议

```text
knowflow-server/
├── src/main/java/com/knowflow/
│   ├── KnowFlowApplication.java
│   ├── common/
│   │   ├── response/
│   │   ├── exception/
│   │   ├── enums/
│   │   ├── config/
│   │   ├── security/
│   │   └── util/
│   ├── modules/
│   │   ├── auth/
│   │   ├── user/
│   │   ├── knowledge/
│   │   ├── document/
│   │   ├── rag/
│   │   ├── chat/
│   │   ├── admin/
│   │   ├── ai/
│   │   └── log/
│   └── infrastructure/
│       ├── storage/
│       ├── vector/
│       ├── llm/
│       └── parser/
└── src/main/resources/
    ├── application.yml
    ├── application-dev.yml
    └── mapper/
```

### 前端工程结构建议

```text
knowflow-web/
├── src/
│   ├── api/
│   ├── assets/
│   ├── components/
│   ├── layouts/
│   ├── router/
│   ├── stores/
│   ├── utils/
│   ├── views/
│   │   ├── auth/
│   │   ├── dashboard/
│   │   ├── knowledge/
│   │   ├── document/
│   │   ├── chat/
│   │   ├── history/
│   │   ├── settings/
│   │   └── admin/
│   └── main.ts
└── vite.config.ts
```

### 让 AI 做什么

```text
请帮我创建 KnowFlow AI 后端基础脚手架。

要求：
1. Spring Boot 3.x + Java 17。
2. 使用统一响应 Result<T>。
3. 增加全局异常处理。
4. 增加基础分页对象。
5. 增加参数校验异常处理。
6. 增加统一错误码枚举。
7. 暂时不要实现业务模块。
8. 输出项目结构和核心代码。
```

### 验收标准

- 项目可以启动；
- 有统一返回格式；
- 有全局异常处理；
- 有基础日志；
- 业务模块尚未耦合进来。

---

## 阶段 3：认证、用户与权限基础

### 目标

先完成所有模块依赖的登录态和用户身份体系。

### 开发内容

```text
前台：
- 注册
- 登录
- 退出
- 当前用户信息
- 修改资料
- 修改密码

后台：
- 管理员登录
- 判断是否管理员
- 后台接口访问拦截

基础能力：
- JWT 生成与校验
- 密码加密
- 登录失败提示
- 禁用账号拦截
- 当前用户上下文
```

### 给 AI 的指令模板

```text
请只实现 KnowFlow AI 的用户认证模块。

已存在：
- Spring Boot 基础工程
- Result<T>
- GlobalExceptionHandler

本次任务：
1. 设计用户注册、登录、当前用户信息接口。
2. 使用 JWT 做登录态。
3. 密码必须使用 BCrypt 加密。
4. 登录后返回 token 和用户基本信息。
5. 后续业务接口可以通过 SecurityUtils.getCurrentUserId() 获取当前用户 ID。
6. 普通用户和管理员用 role 字段区分，MVP 不做复杂 RBAC。
7. 输出 Controller、Service、DTO、VO、Entity、Mapper。
8. 给出接口测试样例。
```

### 验收标准

- 未登录不能访问业务接口；
- 禁用用户不能登录；
- 密码不明文存储；
- 能获取当前用户 ID；
- 管理员接口能区分普通用户。

---

## 阶段 4：知识库管理模块

### 目标

完成用户私有知识库的 CRUD，这是文档和问答的上层容器。

### 开发内容

```text
- 创建知识库
- 知识库列表
- 知识库详情
- 编辑知识库
- 删除知识库
- 状态管理
- 用户归属校验
```

### 最关键防坑点

所有知识库查询必须带：

```text
user_id = 当前登录用户 ID
```

不要只根据 `knowledge_base_id` 查询，否则会出现越权访问。

### 给 AI 的指令模板

```text
请实现 KnowFlow AI 的知识库管理模块后端接口。

约束：
1. 用户只能操作自己创建的知识库。
2. 删除使用逻辑删除。
3. 列表支持关键词搜索、创建时间排序、更新时间排序。
4. 状态枚举：NORMAL、PROCESSING、FAILED、DISABLED。
5. 不要实现文档上传和问答。
6. 每个详情、编辑、删除接口都必须做归属校验。
7. 输出接口路径、请求参数、响应结构和代码。
```

### 验收标准

- A 用户不能查看 B 用户知识库；
- 删除知识库后列表不再展示；
- 编辑知识库不能修改所有者；
- 状态枚举不混乱。

---

## 阶段 5：核心 API 契约冻结

### 目标

在前端大规模开发前，先冻结 API 契约，避免前后端字段反复不一致。

### 建议先冻结这些接口

```text
/auth/register
/auth/login
/auth/logout
/auth/me

/knowledge-bases
/knowledge-bases/{id}
/knowledge-bases/{id}/documents

/documents/upload
/documents/{id}
/documents/{id}/retry-parse
/documents/{id}/status

/chat/sessions
/chat/sessions/{id}/messages
/chat/ask

/admin/users
/admin/knowledge-bases
/admin/documents
/admin/ai-model-configs
/admin/tasks
/admin/logs
```

### 给 AI 的指令模板

```text
请基于已完成的用户、知识库、文档、问答 MVP 范围，输出 OpenAPI 风格的接口契约。

要求：
1. 使用 RESTful 风格。
2. 每个接口包含 method、path、说明、请求参数、响应示例、错误码。
3. 字段命名统一使用 camelCase。
4. 分页参数统一为 pageNo、pageSize。
5. 不写业务代码。
6. 输出 Markdown 表格。
```

### 验收标准

- 前端可以直接按契约写 API 调用；
- 后端可以直接按契约实现 Controller；
- 同一个字段不出现多种命名；
- 所有核心业务都有错误码。

---

## 阶段 6：文档上传与文档元数据管理

### 目标

先完成“文件可上传、元数据可管理”，暂时不急着做复杂解析。

### 开发内容

```text
- 选择知识库上传文档
- 文件格式校验：PDF、DOCX、TXT、MD
- 文件大小限制
- 文件存储
- 文档记录入库
- 解析状态初始化为 PENDING
- 文档列表
- 文档详情
- 文档删除
- 重新解析入口
```

### 关键设计

文档上传成功后，不要在 HTTP 请求里同步完成全部解析和向量化。

推荐流程：

```text
上传文件
  -> 保存原始文件
  -> 插入 document 记录
  -> 插入 document_process_task 任务
  -> 返回上传成功
  -> 后台异步执行解析和向量化
```

### 给 AI 的指令模板

```text
请实现 KnowFlow AI 的文档上传与文档元数据管理模块。

约束：
1. 用户必须选择目标知识库。
2. 上传前校验知识库归属。
3. MVP 支持 PDF、DOCX、TXT、MD。
4. 上传成功后只创建文档记录和处理任务，不要在上传接口里直接解析全文。
5. 文档状态初始化：parseStatus=PENDING，embeddingStatus=PENDING。
6. 删除文档时先做逻辑删除，并预留后续清理原始文件、切片和向量索引的能力。
7. 输出 Controller、Service、Entity、DTO、VO、Mapper。
```

### 验收标准

- 未登录不能上传；
- 不能向别人的知识库上传；
- 不支持的文件格式会被拒绝；
- 上传接口响应速度稳定；
- 文档记录能在知识库详情页查询到。

---

## 阶段 7：文档解析、切片与异步任务

### 目标

打通 RAG 的数据准备链路。

### 后端流程

```text
扫描 PENDING 任务
  -> 标记任务 PROCESSING
    -> 读取原始文件
      -> 文本提取
        -> 文本清洗
          -> 文本切片
            -> 保存 document_chunk
              -> 调用 Embedding
                -> 写入向量库
                  -> 更新 document 状态 SUCCESS
```

### 文本切片建议

MVP 可先用简单策略：

```text
chunkSize = 800 ~ 1200 字符
overlap = 100 ~ 200 字符
```

每个切片至少保存：

```text
chunkId
userId
knowledgeBaseId
documentId
chunkIndex
content
contentHash
tokenCount，可选
```

### 给 AI 的指令模板

```text
请实现 KnowFlow AI 的文档解析与切片任务。

已存在：
- document 表
- document_process_task 表
- document_chunk 表
- 文档上传接口

本次任务：
1. 实现异步任务执行器。
2. 支持 PDF、DOCX、TXT、MD 文本提取。
3. 实现固定长度 + overlap 的文本切片。
4. 保存切片到 document_chunk。
5. 正确更新 document.parseStatus 和 task.status。
6. 失败时记录 failReason。
7. 不要实现 AI 问答接口。
8. 给出可测试的最小样例。
```

### 验收标准

- 解析成功后能看到切片数量；
- 解析失败能看到失败原因；
- 重试任务不会重复产生脏切片；
- 大文件不会导致接口超时；
- 日志能定位到具体 documentId 和 taskId。

---

## 阶段 8：Embedding 与向量检索

### 目标

让系统能根据问题召回相关文档片段。

### 推荐抽象接口

后端不要把某个供应商 SDK 写死在业务里。建议抽象：

```java
public interface EmbeddingClient {
    List<Float> embed(String text);
    List<List<Float>> embedBatch(List<String> texts);
}

public interface VectorStoreClient {
    void upsert(VectorRecord record);
    List<VectorSearchResult> search(VectorSearchRequest request);
    void deleteByDocumentId(Long documentId);
}
```

### 给 AI 的指令模板

```text
请为 KnowFlow AI 设计并实现 Embedding 与向量检索抽象层。

要求：
1. 业务层只能依赖 EmbeddingClient 和 VectorStoreClient 接口。
2. 具体供应商实现放到 infrastructure 包。
3. 向量记录必须包含 userId、knowledgeBaseId、documentId、chunkId。
4. 检索时必须限制 knowledgeBaseId 和 userId，避免跨用户召回。
5. MVP 先实现一个可替换的 MockVectorStoreClient，后续可替换成 Qdrant/Milvus。
6. 不要在 Controller 里直接调用供应商 SDK。
```

### 验收标准

- Embedding 维度一致；
- 向量检索不会跨知识库；
- 向量服务失败时有日志和错误码；
- 业务代码不绑定某个模型供应商。

---

## 阶段 9：RAG 智能问答主链路

### 目标

完成项目最核心的演示能力：基于知识库问答，并展示引用来源。

### RAG 服务流程

```text
用户选择知识库并提问
  -> 校验知识库归属和状态
    -> 将问题生成向量
      -> 在当前知识库召回 TopK 文档切片
        -> 过滤低相似度结果
          -> 组装 Prompt
            -> 调用大模型
              -> 保存会话消息
                -> 保存引用来源
                  -> 返回回答 + 引用片段
```

### Prompt MVP 模板

```text
你是 KnowFlow AI 的知识库问答助手。
请只基于给定的文档片段回答用户问题。
如果文档片段中没有答案，请明确说明“当前知识库中没有找到足够依据”。
不要编造不存在的信息。

用户问题：
{question}

相关文档片段：
{chunks}

回答要求：
1. 先直接回答问题。
2. 再列出依据来源。
3. 保持简洁准确。
```

### 给 AI 的指令模板

```text
请实现 KnowFlow AI 的 RAG 问答后端服务。

已存在：
- 用户认证
- 知识库管理
- 文档上传
- document_chunk
- EmbeddingClient
- VectorStoreClient
- LlmClient

本次任务：
1. 实现 /chat/ask 接口。
2. 输入：knowledgeBaseId、sessionId 可选、question。
3. 校验知识库归属和状态。
4. 检索 TopK 文档切片。
5. 组装 Prompt 调用大模型。
6. 保存 chat_session、chat_message、chat_message_reference。
7. 返回 answer、sessionId、references。
8. 如果没有召回结果，不要编造答案。
9. 本阶段不做流式输出。
```

### 验收标准

- 能基于上传文档回答；
- 回答包含引用来源；
- 没有相关内容时不胡编；
- 会话和消息能落库；
- 问答失败时能查看 AI 调用日志。

---

## 阶段 10：前端普通用户端

### 目标

把核心业务闭环串起来，让项目可以演示。

### 推荐页面开发顺序

```text
1. 登录 / 注册页
2. 主布局 / 菜单 / 路由守卫
3. 工作台
4. 知识库列表页
5. 知识库详情页
6. 文档上传组件
7. 文档列表与解析状态展示
8. 智能问答页面
9. 历史会话页面
10. 用户设置页
```

### 给 AI 的指令模板

```text
请基于已冻结的 API 契约实现 KnowFlow AI 前端知识库列表页。

技术栈：Vue 3 + TypeScript + Element Plus + Pinia + Vue Router。

要求：
1. 只实现知识库列表页，不要实现其他页面。
2. 调用 /knowledge-bases 分页接口。
3. 支持关键词搜索。
4. 支持创建知识库弹窗。
5. 支持编辑、删除操作。
6. 删除前二次确认。
7. 使用统一 api client。
8. 输出新增/修改的文件清单和代码。
```

### 验收标准

- 页面可以真实调用后端；
- token 过期能跳回登录页；
- 表单校验完整；
- 加载、空状态、错误状态都有展示；
- 前端字段和 API 契约一致。

---

## 阶段 11：后台管理端

### 目标

完善作品集的“工程管理能力”展示。

### MVP 后台优先级

```text
1. 后台登录与菜单权限
2. 数据看板
3. 用户管理
4. 知识库管理
5. 文档管理
6. AI 模型配置
7. 系统任务管理
8. 日志审计
9. 公告管理，可后置
```

### 给 AI 的指令模板

```text
请实现 KnowFlow AI 后台用户管理模块。

约束：
1. 只有管理员可以访问。
2. 支持用户分页列表。
3. 支持按用户名、账号状态筛选。
4. 支持查看用户详情。
5. 支持禁用、启用用户。
6. 禁用用户后，该用户不能继续登录。
7. 所有管理员操作写入 operation_log。
8. 不要实现复杂 RBAC。
```

### 验收标准

- 普通用户访问后台接口会被拒绝；
- 禁用用户后登录失败；
- 管理员操作有日志；
- 后台列表分页正常。

---

## 阶段 12：测试、调试与部署

### 目标

把 AI 生成代码变成可运行、可演示、可解释的项目。

### 必做测试

```text
1. 单元测试
- TextChunkerTest
- PromptBuilderTest
- AuthServiceTest
- KnowledgeBaseServiceTest

2. 接口测试
- 注册登录
- 创建知识库
- 上传文档
- 查询解析状态
- 发起问答
- 查看引用来源

3. 异常测试
- 未登录访问
- 访问他人知识库
- 上传非法文件
- 文档解析失败
- AI 调用失败
- 向量检索为空

4. 演示测试
- 准备一份技术 PDF
- 上传
- 等待解析成功
- 提问 3 个文档内问题
- 提问 1 个文档外问题
- 检查是否拒绝编造
```

### 部署建议

MVP 可采用：

```text
前端：Nginx
后端：Spring Boot Jar
数据库：MySQL
缓存：Redis，可选
向量库：Qdrant 或 Milvus，可选
文件存储：本地磁盘，后续可换 MinIO
```

---

## 4. 每个任务的 AI 指令标准格式

以后每次给 AI 发任务，建议固定用这个格式。

```text
【项目背景】
KnowFlow AI 是一个个人知识库与智能文档问答平台。当前只开发 MVP。

【技术栈】
后端：Spring Boot 3.x、Java 17、MySQL、MyBatis-Plus。
前端：Vue 3、TypeScript、Element Plus。

【当前已完成】
请填写当前已完成模块，例如：用户认证、知识库 CRUD。

【当前任务】
请填写本次只做什么，例如：实现文档上传接口。

【PRD 切片】
请贴当前模块的 MVP 功能点，不要贴完整 PRD。

【现有代码约束】
例如：
- 统一响应 Result<T>
- 当前用户 ID 通过 SecurityUtils.getCurrentUserId() 获取
- 分页参数 pageNo/pageSize
- 逻辑删除字段 deleted

【输出要求】
1. 先列出新增/修改文件。
2. 再说明设计思路。
3. 再生成代码。
4. 最后给出测试步骤。

【禁止事项】
1. 不要实现扩展功能。
2. 不要修改无关文件。
3. 不要改变已有接口字段。
4. 不要硬编码 API Key。
5. 不要绕过权限校验。
```

---

## 5. AI 生成代码后的 Review SOP

每次 AI 生成代码后，不要直接继续下一个模块。先让 AI 自查一次，再由你运行。

### 5.1 代码自查 Prompt

```text
请对你刚才生成的代码做一次严格 Code Review。

重点检查：
1. 是否有编译错误。
2. 是否有字段名不一致。
3. 是否有未注入的 Bean。
4. 是否有循环依赖。
5. 是否有 NPE 风险。
6. 是否有事务遗漏。
7. 是否有权限校验遗漏。
8. 是否会访问到其他用户的数据。
9. 是否修改了与本任务无关的文件。
10. 是否违反了 MVP 范围。

请按“问题 -> 影响 -> 修改建议”的格式输出。
```

### 5.2 接口验收 Prompt

```text
请基于当前模块代码，生成接口测试用例。

要求：
1. 包含正常场景。
2. 包含异常场景。
3. 包含权限越权场景。
4. 给出请求 URL、请求体、预期响应。
5. 如果适合，请输出 Postman/Apifox 测试步骤。
```

### 5.3 Bug 修复 Prompt

```text
下面是运行时报错信息和相关代码。
请只分析这个错误，不要重构整个模块。

【错误日志】
粘贴完整报错。

【相关代码】
粘贴相关类或文件。

【要求】
1. 判断根因。
2. 给出最小修改方案。
3. 明确需要修改哪些文件。
4. 不要改动无关逻辑。
```

---

## 6. 防坑指南

## 6.1 工程结构混乱

### 常见表现

- Controller 里直接写业务逻辑；
- Service 里直接操作 HTTP 对象；
- AI SDK 调用散落在各业务类；
- Entity、DTO、VO 混用；
- 前后端字段名不一致。

### 规避方式

固定分层：

```text
Controller：接收请求、参数校验、返回结果
Service：业务编排
Domain/Entity：数据库对象
DTO：请求对象
VO：响应对象
Mapper/Repository：数据访问
Infrastructure：文件存储、文档解析、向量库、模型调用
```

给 AI 明确禁止：

```text
不要在 Controller 中直接写数据库操作。
不要把 Entity 直接返回给前端。
不要在业务 Service 中硬编码模型供应商 SDK。
```

---

## 6.2 前后端 API 契约漂移

### 常见表现

后端返回：

```json
{"knowledge_base_id": 1}
```

前端使用：

```ts
knowledgeBaseId
```

或者分页字段一会儿叫 `pageNum`，一会儿叫 `pageNo`。

### 规避方式

先冻结：

```text
docs/04-api-contract.md
```

统一约定：

```text
请求/响应字段：camelCase
数据库字段：snake_case
分页参数：pageNo、pageSize
分页响应：list、total、pageNo、pageSize
```

---

## 6.3 权限校验遗漏

### 常见表现

用户只要猜到 documentId，就能查看或删除别人的文档。

### 规避方式

所有私有资源查询都必须同时带：

```text
id = 资源 ID
user_id = 当前用户 ID
```

重点接口：

```text
知识库详情
知识库编辑
知识库删除
文档上传
文档详情
文档删除
重新解析文档
智能问答
历史会话查看
```

AI 指令中必须写：

```text
每个详情、编辑、删除、问答接口都必须做 userId 归属校验。
```

---

## 6.4 文档处理链路同步阻塞

### 常见表现

上传一个 PDF 后接口卡住几十秒，甚至超时。

### 规避方式

上传接口只做：

```text
保存文件 -> 写 document -> 写 task -> 返回
```

解析、切片、向量化全部异步执行。

---

## 6.5 文档重试导致脏数据

### 常见表现

同一个文档反复重试后，产生重复切片和重复向量。

### 规避方式

重新解析前必须：

```text
删除旧 chunk
删除旧 vector
重置 parseStatus
重置 embeddingStatus
新建 task
```

并建议使用事务保护 MySQL 数据变更。

---

## 6.6 向量检索跨用户污染

### 常见表现

A 用户提问时召回了 B 用户的文档片段。

### 规避方式

向量写入时必须带 metadata：

```text
userId
knowledgeBaseId
documentId
chunkId
```

向量检索时 filter 必须包含：

```text
userId = 当前用户 ID
knowledgeBaseId = 当前知识库 ID
```

---

## 6.7 AI 幻觉回答

### 常见表现

知识库里没有相关内容，但模型仍然编造答案。

### 规避方式

Prompt 中必须明确：

```text
只能基于给定文档片段回答。
如果没有足够依据，必须说明无法从当前知识库找到答案。
```

后端也要做兜底：

```text
如果召回结果为空或最高相似度低于阈值，直接返回“当前知识库中没有找到足够依据”。
不要继续调用大模型。
```

---

## 6.8 模型配置与 API Key 泄露

### 常见表现

AI 直接把 API Key 写进代码或前端配置。

### 规避方式

- API Key 只能在后端配置；
- 前端永远不直接调用大模型；
- 本地开发使用环境变量；
- 后台 AI 配置入库时要加密或至少脱敏展示；
- 日志不能打印完整 Key。

---

## 6.9 日志不足，后期无法调试

### 常见表现

问答失败只返回“系统错误”，不知道是上传、解析、Embedding、检索还是 LLM 调用失败。

### 规避方式

关键链路必须记录：

```text
documentId
knowledgeBaseId
taskId
sessionId
messageId
modelName
elapsedMs
failReason
```

至少这些表/日志要可查：

```text
document_process_task
ai_call_log
operation_log
login_log
```

---

## 6.10 AI 一次改太多文件

### 常见表现

为了修一个 bug，AI 顺手重构了多个模块，导致更多问题。

### 规避方式

每次修复都要求：

```text
只给出最小修改方案。
只修改与当前错误直接相关的文件。
修改前先列出文件清单。
```

---

## 7. 推荐里程碑

### Milestone 1：基础系统可登录

完成：

```text
后端脚手架
前端脚手架
注册登录
JWT
用户信息
基础布局
```

### Milestone 2：知识库和文档可管理

完成：

```text
知识库 CRUD
文档上传
文档列表
文档状态
文档删除
```

### Milestone 3：文档可解析和切片

完成：

```text
异步任务
PDF/DOCX/TXT/MD 解析
文本切片
切片入库
失败重试
```

### Milestone 4：RAG 问答可用

完成：

```text
Embedding
向量检索
Prompt 构造
LLM 调用
回答返回
引用来源
问答历史
```

### Milestone 5：后台管理可演示

完成：

```text
后台登录
数据看板
用户管理
知识库管理
文档管理
AI 配置
任务管理
日志审计
```

### Milestone 6：项目作品集化

完成：

```text
README
架构图
数据库 ER 图
RAG 流程图
接口文档
部署文档
演示数据
面试讲解稿
```

---

## 8. 最推荐的实际执行方式

### 第一步：让 AI 先写文档，不写代码

顺序：

```text
1. MVP 范围文档
2. 数据库设计文档
3. API 契约文档
4. RAG 流程设计文档
5. 前端页面路由文档
```

### 第二步：按模块生成代码

顺序：

```text
1. 后端基础工程
2. 用户认证
3. 知识库管理
4. 文档上传
5. 文档解析任务
6. 向量检索
7. RAG 问答
8. 前端页面
9. 后台管理
10. 测试与部署
```

### 第三步：每完成一个模块就做三件事

```text
1. 运行项目
2. 接口测试
3. 更新 docs/current-context.md
```

---

## 9. 你给 Codex 的第一句推荐指令

```text
你现在是我的 Java 全栈项目开发助手，参与开发 KnowFlow AI：个人知识库与智能文档问答平台。

在开始写代码前，请先阅读我提供的 PRD，并只完成第一步：提炼 MVP 范围和工程开发计划。

要求：
1. 不要生成代码。
2. 按前台用户端、后台管理员端、后端基础能力、RAG 核心链路拆分 MVP 功能。
3. 明确哪些 PRD 功能暂时不做。
4. 输出 docs/01-mvp-scope.md 的内容。
5. 输出后续开发阶段顺序：数据库设计、后端脚手架、认证权限、知识库管理、文档上传、文档解析、Embedding、RAG 问答、前端页面、后台管理、测试部署。
6. 每个阶段给出输入材料、输出产物、验收标准。
```

---

## 10. 最终提醒

AI 协作开发的核心不是“让 AI 多写代码”，而是“让 AI 在严格边界内写小块代码”。

你真正要控制的是：

```text
MVP 边界
数据库模型
API 契约
状态枚举
权限校验
异步任务链路
RAG 检索边界
错误日志
测试验收
```

只要这些被你牢牢控制，AI 生成代码的效率会很高；如果这些没有先固定，项目后期会变成字段不一致、接口不一致、权限漏洞、任务状态混乱和 RAG 难以调试的集合。
