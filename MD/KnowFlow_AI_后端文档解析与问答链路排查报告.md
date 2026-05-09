# KnowFlow AI 后端文档解析与问答链路排查报告

## 1. 测试账号
- 用户名：`chain_user_1778304879`
- 邮箱：`chain_user_1778304879@example.com`
- 角色：`USER`
- 注册/登录结果：`code=0`

## 2. 知识库 ID
- `knowledgeBaseId = 25`

## 3. 文档 ID
- `documentId = 8`

## 4. 任务 ID
- `taskId = 10`（来自 `/api/admin/document-tasks` 列表中该文档对应任务）

## 5. 上传接口响应
- 请求：`POST /api/documents/upload`
- 文件：`test.md`（内容为题目给定内容）
- 响应：HTTP `200`
- 响应体关键字段：
  - `code=0`
  - `data.id=8`
  - `data.knowledgeBaseId=25`
  - `data.parseStatus=PENDING`
  - `data.embeddingStatus=PENDING`

## 6. document 表记录状态
- 查询结果（docId=8）：
  - `parse_status = PENDING`
  - `embedding_status = PENDING`
  - `error_message = NULL`
- 轮询 12 次后状态仍未变化（始终 PENDING/PENDING）。

## 7. document_process_task 表记录状态
- 查询结果（docId=8 最新任务）：
  - `id=10`
  - `status = PENDING`
  - `fail_reason = NULL`
- 轮询期间任务状态未从 `PENDING` 前进。

## 8. document_chunk 数量
- 查询结果（docId=8）：
  - `count = 0`

## 9. 异步任务是否执行
- 结论：**异步方法被触发，但执行早期异常中断，未完成处理。**
- 证据（`target/chain_check.log`）：
  - `Unexpected exception occurred invoking async method: ... DocumentProcessService.processAsync(Long)`
  - `java.lang.NullPointerException: Cannot invoke "com.knowflow.entity.DocumentProcessTask.getDocumentId()" because "task" is null`
  - 栈定位：`com.knowflow.service.DocumentProcessService.processAsync(DocumentProcessService.java:51)`

## 10. 如果未执行，真实原因
- 真实原因：`processAsync` 中先执行 `taskRepository.selectById(taskId)`，随后直接 `task.getDocumentId()`；当 `task == null` 时在 `try` 之前触发 NPE，导致流程中断。
- 影响：
  1. 任务状态卡在 `PENDING`
  2. 文档状态卡在 `PENDING/PENDING`
  3. 不会生成 chunk
  4. 后续问答无可检索证据

## 11. chat/ask 请求与响应
- 针对知识库 `25` 提问 4 个问题（均为 `POST /api/chat/ask`）：
  1. KnowFlow AI 是什么？
  2. KnowFlow AI 的技术栈有哪些？
  3. MVP 的核心流程是什么？
  4. 管理员可以管理哪些内容？
- 结果：4 次均 HTTP `200`，`code=0`，但回答一致为：
  - `answer = "The current knowledge base has no sufficient evidence."`
  - `references = []`

## 12. references 为空或非空的原因
- 本次为 `references=[]`，直接原因是 `document_chunk` 为 `0`，检索层没有命中可用片段。
- 根因仍是上游异步解析任务异常中断（见第 9/10 节）。

## 13. 当前是否具备“可问答演示数据”
- 结论：**当前不具备稳定可问答演示数据。**
- 原因：新上传文档未完成解析与切片，问答只能走“无证据兜底回答”。

## 14. 最小修复建议（不改代码，仅建议）
1. 修复 `DocumentProcessService.processAsync` 的空值防护：
   - 在使用 `task.getDocumentId()` 前判空；
   - 或将关键读取放入 `try`，并在异常时明确落库 `fail_reason`。
2. 校验任务创建后传入的 `taskId` 与查询条件一致，避免 `selectById` 查不到。
3. 为异步异常补充统一告警/落库，避免任务长期停留 `PENDING` 无可见错误。
4. 修复后按同链路回归：上传 -> task 进入 `PROCESSING/SUCCESS` -> chunk > 0 -> `chat/ask` 返回有效 answer/references。

---

## 附：本次关键证据摘要
- 上传成功：`code=0`，`documentId=8`
- 文档状态：`PENDING / PENDING`（持续不变）
- 任务状态：`PENDING`（持续不变）
- chunk：`0`
- 异常日志：`NullPointerException ... task is null` at `DocumentProcessService.java:51`
- 问答：HTTP 200 但 `references=[]` 且 answer 为无证据兜底文本
