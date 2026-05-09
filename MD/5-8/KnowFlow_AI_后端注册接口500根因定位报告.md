# KnowFlow AI 后端注册接口 500 根因定位报告

时间：2026-05-09  
项目：`C:\codex-project\ai-knowledge-java`  
说明：本次仅排查，不修改任何代码/数据库结构。

---

## 1. 复现请求

### 1.1 按题面最小请求（有效 JSON）复现

- URL：`http://localhost:8080/api/auth/register`
- Method：`POST`
- Headers：`Content-Type: application/json`
- Request Body：

```json
{
  "username": "testuser001",
  "email": "testuser001@example.com",
  "password": "123456",
  "confirmPassword": "123456"
}
```

- Response Body（实测）：

```json
{"code":0,"message":"ok","data":{"token":"***","user":{"id":6,"username":"testuser001","email":"testuser001@example.com","nickname":"testuser001","avatar":null,"bio":null,"status":"ENABLED","role":"USER","createTime":"2026-05-09T12:01:55.5150139"}}}
```

- HTTP Status：`200`

结论：**该最小请求在当前环境可成功注册，不触发 500**。

### 1.2 500 复现（最小触发）

为了定位“500 的真实来源”，使用了与上面同字段但**被转义破坏后的 JSON**（字段名未被双引号包裹）进行复现，得到 500：

- Response Body：

```json
{"code":500,"message":"系统错误","data":null}
```

- HTTP Status：`200`

---

## 2. 后端控制台真实异常

### 异常类型

- `org.springframework.http.converter.HttpMessageNotReadableException`

### 异常 message

- `JSON parse error: Unexpected character ('u' (code 117)): was expecting double-quote to start field name`

### 关键堆栈/关键日志证据

日志文件：`C:\codex-project\ai-knowledge-java\target\backend-diagnose.log`

- `112` 行：

```text
Resolved [org.springframework.http.converter.HttpMessageNotReadableException: JSON parse error: Unexpected character ('u' (code 117)): was expecting double-quote to start field name]
```

这说明异常发生在 **请求体 JSON 反序列化阶段**，Controller 业务方法尚未执行。

### 触发类 / 方法 / 行号

1. 异常被兜底处理的位置：
   - `src/main/java/com/knowflow/common/GlobalExceptionHandler.java:26`
   - 方法：`handleUnknown(Exception ex)`
2. 注册入口（仅在 JSON 正常时会进入）：
   - `src/main/java/com/knowflow/controller/AuthController.java:26`
   - 方法：`register(@Valid @RequestBody RegisterRequest request)`

---

## 3. 注册调用链

### 正常链路（有效 JSON）

`AuthController.register`  
-> `AuthService.register`  
-> `UserRepository.existsBy...` / `insert(user)`  
-> `KnowledgeBaseRepository.insert(kb)`  
-> `JwtService.createToken`  
-> `ApiResponse.ok`

对应日志证据（同一文件）：
- `119-140` 行可见 `sys_user` 与 `knowledge_base` 插入成功。

### 500 链路（当前定位到的真实触发）

`DispatcherServlet`  
-> `HttpMessageConverter` 反序列化请求体  
-> 抛出 `HttpMessageNotReadableException`  
-> `GlobalExceptionHandler.handleUnknown` 返回 `ApiResponse.fail(500, "系统错误")`

---

## 4. 根因判断

### 问题类型

- `请求体格式/反序列化` + `全局异常处理兜底策略`

### 根因说明

当前 500 不是注册业务逻辑（User/KB/JWT/事务）本身报错，而是**请求 JSON 在进入 Controller 前解析失败**。  
`HttpMessageNotReadableException` 未被单独分类处理，落入 `handleUnknown`，统一返回了 `code=500`。

### 证据

1. 500 时日志明确为 `HttpMessageNotReadableException`（`backend-diagnose.log:112`）。  
2. 同一最小请求在有效 JSON 方式下可成功注册并返回 token。  
3. 有效 JSON 时 SQL 执行正常：  
   - `INSERT INTO sys_user ...`（`133-135` 行）  
   - `INSERT INTO knowledge_base ...`（`138-140` 行）

### 影响范围

- 所有需要 JSON 反序列化的接口都可能出现“客户端 JSON 格式错误 -> 服务端返回 500”的现象（语义上应更接近 400）。

---

## 5. 涉及文件

1. `src/main/java/com/knowflow/common/GlobalExceptionHandler.java`
2. `src/main/java/com/knowflow/controller/AuthController.java`
3. `src/main/java/com/knowflow/service/AuthService.java`
4. `src/main/java/com/knowflow/dto/RegisterRequest.java`
5. `src/main/java/com/knowflow/mapper/UserRepository.java`
6. `src/main/java/com/knowflow/mapper/KnowledgeBaseRepository.java`
7. `src/main/java/com/knowflow/entity/User.java`
8. `src/main/java/com/knowflow/entity/KnowledgeBase.java`
9. `src/main/java/com/knowflow/security/JwtService.java`
10. `src/main/java/com/knowflow/security/SecurityConfig.java`
11. `src/main/resources/application.yml`
12. `target/backend-diagnose.log`

---

## 6. 最小修复建议（仅建议，不改代码）

1. 建议修改文件  
   - `GlobalExceptionHandler.java`

2. 应该改什么  
   - 新增对 `HttpMessageNotReadableException` 的显式处理，返回业务 `code=400`（或项目约定的参数错误码），并给出可读错误信息（例如“请求 JSON 格式错误”）。
   - 在 `handleUnknown` 中补充完整异常日志（至少 `log.error("...", ex)`），不要只返回通用文案。

3. 为什么这样改  
   - 当前真实客户端输入错误被包装为 500，误导联调方向，增加排障成本。

4. 是否需要改数据库  
   - **不需要**。本次定位未发现注册链路的表结构不匹配问题。

5. 是否影响前端接口  
   - 接口路径/参数无需改；仅错误码语义会更准确（JSON 错误从“500”变“400”）。

---

## 7. 风险判断

- 是否只影响注册：**否**。凡是 `@RequestBody` JSON 反序列化失败的接口都会受影响（被误报 500）。
- 是否也影响登录：**是（同类风险）**，若登录 JSON 格式异常同样可能走 500。
- 是否也影响后台登录：**是（同类风险）**。
- 是否影响知识库/文档/问答主链路：  
  - 对“合法 JSON 请求”主链路：**当前排查结果显示可正常执行注册入库与返回 token**。  
  - 对“非法 JSON 请求”：会误判成 500，影响联调判断质量。

---

## 8. 下一步建议

若允许修改，建议下一轮最小修复顺序：

1. 在 `GlobalExceptionHandler` 中新增 `HttpMessageNotReadableException` -> 400 映射，并打印完整异常栈。  
2. 保持现有注册业务逻辑不变，再回归验证：  
   - 合法 JSON：`/api/auth/register` 返回 `code=0`  
   - 非法 JSON：返回参数错误码（400 语义）且日志可见完整异常  
3. 按同样策略复验 `/api/auth/login`、`/api/admin/auth/login`。

