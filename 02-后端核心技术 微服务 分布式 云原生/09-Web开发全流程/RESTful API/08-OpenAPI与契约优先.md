# 08-OpenAPI与契约优先
> 定位：OpenAPI 3.1 是 API 契约的事实标准——Spec-first 工作流让「契约先行、代码后行」成为工程规范；文档、Mock、SDK、测试全部从契约生成，契约是唯一事实来源。

## 📚 目录
1. [为什么需要契约](#1-为什么需要契约)
2. [OpenAPI 3.1 核心](#2-openapi-31-核心)
3. [Spec-first 工作流](#3-spec-first-工作流)
4. [从契约生成一切](#4-从契约生成一切)
5. [CI 契约工具链](#5-ci-契约工具链)
6. [AI agent 时代的契约](#6-ai-agent-时代的契约)
7. [常见坑](#7-常见坑)
8. [练习](#8-练习)

## 1. 为什么需要契约

没有契约的 API 团队协作是「口头协议」：后端改字段不通知、前端按旧文档对接、测试靠猜——**契约（Contract）把「接口长什么样」变成显式的、机器可读的、可校验的文档**：

**契约解决的问题**：**沟通**——前后端/跨团队以契约为准（不是聊天记录）；**变更管理**——破坏性变更检测（06 篇 oasdiff）的前提；**自动化**——文档、Mock、SDK、测试从同一份契约生成，不再各写各的（文档滞后于代码是 API 工程的顽疾，契约化根治）；**可发现性**——消费方（包括 AI agent）通过契约了解 API 能力。

**OpenAPI 是事实标准**（Swagger 的继承者，3.1 版与 JSON Schema 对齐）——GitHub、Stripe、AWS 的 API 都以 OpenAPI 发布；替代品（RAML、API Blueprint）已边缘化。**契约优先（Spec-first）vs 代码优先（Code-first）**：Spec-first 先写契约再写实现（契约是源头）；Code-first 从代码生成契约（注解驱动，SpringDoc 即此路）——**2026 推荐 Spec-first**（契约真被当作契约而非代码的投影），但 Code-first 在快速迭代的内部项目仍常见——**无论哪条路，契约必须存在且与实现一致**。

## 2. OpenAPI 3.1 核心

一份 OpenAPI 文档的核心结构（YAML 是常规书写格式）：

```yaml
openapi: 3.1.0
info:
  title: 订单 API
  version: 1.0.0
servers:
  - url: https://api.example.com/v1
paths:
  /orders:
    get:
      summary: 订单列表
      parameters:
        - name: status
          in: query
          schema: { type: string, enum: [pending, paid] }
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema: { $ref: '#/components/schemas/OrderPage' }
        '429':
          description: 限流
          content: { application/json: { schema: { $ref: '#/components/schemas/Problem' } } }
components:
  schemas:
    Order:
      type: object
      required: [id, status]
      properties:
        id: { type: string }
        status: { type: string, enum: [pending, paid, cancelled] }
```

要点：**paths 定义端点**（参数、请求体、响应——**每个状态码的响应都要写**，包括 4xx/5xx——错误响应是契约的一部分）；**components/schemas 定义数据模型**（可复用、$ref 引用）；**3.1 与 JSON Schema 2020-12 对齐**（nullable 用 `type: [string, 'null']` 而非 3.0 的 `nullable: true`）。**契约的完整性标准**：请求/响应示例（examples）、错误响应、认证方式（security）、限流行为——「契约里没有的，就是没定义的」。

## 3. Spec-first 工作流

Spec-first 的五步循环，契约先行、代码后行：

**第一步，写契约**——设计端点、请求响应、错误（02-07 篇的决策全部落在契约里）；工具：Stoplight Studio（可视化设计）、编辑器 + OpenAPI 插件。**第二步，共享与评审**——契约进仓库（git 评审=API 评审——PR 里 diff 的就是契约，这是「API 评审」最自然的形态）；**Mock 先行**——契约 → Mock 服务（Prism、Mockoon——前端与 agent 对接时后端还没写，契约即可用）。**第三步，契约测试**——消费方按契约生成测试（Dredd/Pact——「契约即测试用例」）。**第四步，实现**——后端按契约实现（Spring 的 springdoc 可从契约生成 Controller 骨架，或按契约手写 + 契约校验）。**第五步，CI 把关**——契约 lint + 破坏性变更检测（下节）。

**Spec-first 的收益时刻**：前端「后端还没写完就能对接」（Mock）；「改契约先评审后改码」（PR 即评审）；「文档永远最新」（文档从契约生成——没有「文档滞后」这个 bug）。

## 4. 从契约生成一切

一份契约，四类产物：

**文档**——Swagger UI / Redoc（契约 → 交互式文档——「文档是产物不是手写品」）；**Mock 服务**——Prism/Mockoon（契约 → 可调用的假 API）；**客户端 SDK**——OpenAPI Generator（契约 → Java/TS/Python 客户端——类型安全、零手写）；**服务端骨架**——契约 → Controller 接口（springdoc/OpenAPI Generator 的服务端模式）。

**工程纪律**：**产物不进手改**（生成的代码带「DO NOT EDIT」头——改契约重新生成，别手改生成物——否则契约与实现脱节）；**生成物进不进仓库**（SDK 进制品库、文档进发布站点——按团队约定）；**契约是唯一事实来源**（实现与契约不一致时，改契约（如果实现是对的）或改实现（如果契约是对的）——不允许「契约摆着、实现私改」。

## 5. CI 契约工具链

契约的 CI 把关是 2026 工程化的核心——四道闸：

**Lint**——Spectral / Redocly：契约风格与正确性检查（路径命名规范、必填字段、缺响应——「契约本身的质量」）；**破坏性变更检测**——oasdiff：PR 里契约 diff 是否破坏性（06 篇——破坏性变更必须有版本/弃用配套，工具拦在合并前）；**契约测试**——Pact（消费者驱动契约测试：前端按契约生成期望，后端实现跑契约验证——「前后端的合同」）与 Dredd（契约 vs 实现的一致性测试——实现跑一遍契约声明的请求，响应是否符合）；**示例校验**——契约里的 examples 与实际响应比对（Mock 与真实响应的一致性）。

**落地的渐进路径**：第一天只做「契约进仓库」（评审即契约评审）→ 第二周加 lint → 加 oasdiff → 加 Dredd 契约测试——**从最便宜的开始，逐步加闸**，别等「完美的工具链」再动手。

## 6. AI agent 时代的契约

2026 年契约的消费方多了一个重要角色——**AI agent**：agent 通过 OpenAPI 文档发现 API、理解参数、生成调用（MCP 协议的 tool 定义与 OpenAPI 同源思想——[[../../../03-AI大模型应用开发/05-AI开发框架|AI 开发框架]] 与 [[../../../03-AI大模型应用开发/06-MCP协议与Agent Skill|MCP 体系]]）。**契约质量成为「API 可被 AI 使用」的前提**：

**给 agent 看的契约要点**——**描述（description）写清楚**（端点做什么、参数语义、返回什么——agent 靠描述理解，不是靠猜）；**示例（examples）齐全**（agent 少犯错）；**错误契约完整**（agent 能处理失败）；**幂等键声明**（agent 自动重试依赖幂等——05 篇——「契约里声明 Idempotency-Key 支持」是 agent 友好 API 的标志）。**测试你的契约可被 agent 读懂**：把契约丢给 LLM 问「怎么下单」——答对了说明契约合格。**「契约 = agent 的使用说明书」**是 2026 API 设计的视角升级。

## 7. 常见坑

**坑一：契约与实现脱节**。代码改了契约没改——文档误导消费方——CI 契约测试（Dredd）把关。

**坑二：错误响应不进契约**。契约只有 200——客户端不知道失败长什么样——每个 4xx/5xx 都要写。

**坑三：手改生成代码**。SDK 手改后重新生成覆盖丢失——「DO NOT EDIT」纪律。

**坑四：契约无评审**。PR 不审契约——API 变更逃过评审——契约进 PR 是默认。

**坑五：description 空白**。端点只有名字没有描述——agent 与对接方全靠猜——描述是契约的一部分。

**坑六：契约 lint 不过也合**。风格问题堆积成债——lint 进 CI。

**坑七：Mock 与实现漂移**。Mock 按旧契约、实现是新契约——前端被 Mock 误导——Mock 从契约生成、随契约更新。

## 8. 练习

1. 契约解决的四个问题是什么？
2. Spec-first 五步循环逐一说。
3. 契约生成的四类产物与「DO NOT EDIT」纪律？
4. CI 四道闸（lint/oasdiff/契约测试/示例）各拦什么？
5. AI agent 时代的契约三要点？

> 🎯 **核心要点**：OpenAPI 3.1 是契约事实标准；Spec-first 让契约成为唯一事实来源（评审即契约评审、文档是产物）；契约生成文档/Mock/SDK/骨架；CI 四道闸（lint/oasdiff/Pact-Dredd/示例）；agent 时代 description 与示例就是 API 的说明书。

---

**下一模块**：[09-安全与生产实践](09-安全与生产实践.md)｜**返回总览**：[00-RESTful API总览](00-RESTful%20API总览.md)
