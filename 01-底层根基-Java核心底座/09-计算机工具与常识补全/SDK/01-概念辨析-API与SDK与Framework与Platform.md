# 01 概念辨析：API vs SDK vs Framework vs Platform

> "API 和 SDK 有什么区别"是面试与选型的第一道概念题——四个概念层层递进，边界清楚了，技术选型就不糊涂

---

## 📚 目录

1. [SDK 的定义与四大组成](#1-sdk-的定义与四大组成)
2. [四概念层层递进的关系](#2-四概念层层递进的关系)
3. [API：接口契约](#3-api接口契约)
4. [SDK：工具箱](#4-sdk工具箱)
5. [Framework：骨架](#5-framework骨架)
6. [Platform：生态](#6-platform生态)
7. [从 OpenAPI 到 SDK 的生成](#7-从-openapi-到-sdk-的生成)
8. [SDK 的类型全景](#8-sdk-的类型全景)

---

## 1. SDK 的定义与四大组成

**SDK（Software Development Kit，软件开发工具包）**：面向某平台/服务的一组**开发工具集合**，让开发者用最少的摩擦接入该平台能力。

**SDK 的四大组成**（判断"是否算 SDK"的标准）：

| 组成 | 内容 | 说明 |
|------|------|------|
| **库（Library）** | 封装 API 调用的代码（jar/包） | 核心：把 HTTP/协议细节包装成方法调用 |
| **工具（Tools）** | 命令行工具、代码生成器、调试器 | 如 Android SDK 的 adb、gradle 插件 |
| **文档（Docs）** | API 参考、指南、示例 | quick-start 必须开箱即用 |
| **示例（Samples）** | 可运行的最小工程 | 让"第一个 Demo"三分钟跑起来 |

```java
// 一个典型 SDK 的消费体验（以某云对象存储 SDK 为例）
// 没有 SDK：自己拼签名、拼 URL、写上传逻辑（几百行 + 容易错）
// 有 SDK：
OSSClient client = new OSSClient(endpoint, credentials);        // 初始化（封装了签名）
client.putObject(bucket, key, inputStream);                     // 一行上传（封装了分片/重试/校验）
```

> 🎯 **核心要点**：SDK 的使命 = **把"低层协议的复杂度"消化掉，把"90% 常见场景"做成一行调用**。判断一个包是不是 SDK：看它是否同时提供**库 + 文档 + 示例**，且封装了协议细节。

---

## 2. 四概念层层递进的关系

```text
API（接口契约）
  │  定义"能调什么、怎么调"—— 菜单
  ▼
SDK（工具箱）
  │  封装 API 的便捷实现 + 文档 + 示例 —— 厨师上门
  ▼
Framework（骨架）
  │  规定"代码怎么组织"，你填空 —— 施工标准（不可改结构）
  ▼
Platform（平台）
  │  提供 API + SDK + 生态 + 规则 —— 购物中心
```

| 维度 | API | SDK | Framework | Platform |
|------|:---:|:---:|:---------:|:--------:|
| 本质 | 契约 | 工具集 | 骨架+规则 | 生态 |
| 控制权 | 你决定一切 | 你决定调用 | **框架决定流程**（好莱坞原则） | 平台决定规则 |
| 交付物 | 规范/端点 | 库+工具+文档 | 基类+容器+约定 | 服务+市场+治理 |
| 例子 | REST 端点 | JDK、AWS SDK | Spring、MyBatis | AWS、微信开放平台、Android |
| 关系 | SDK 封装 API | Framework 内嵌 API | Platform 提供 SDK+Framework | 最大容器 |

> 🎯 **核心要点**：四个概念不是互斥的，而是**层层包含**——SDK 封装 API，Framework 依赖 API/SDK，Platform 提供前两者。面试答"区别"时要指出**控制权的转移**：API 你掌控、SDK 你使用、Framework 你服从、Platform 你寄生。

---

## 3. API：接口契约

**API（Application Programming Interface）**：**接口规范**——定义"能调用什么、参数是什么、返回什么"，不关心实现。

```java
// API 是契约（HTTP 层面的表述）
GET /api/v1/orders/{id}     → 200 { "id": "A1", "status": "PAID" }
                            → 404 { "code": "ORDER_NOT_FOUND" }

// API 的稳定维度：路径、方法、参数、响应结构、错误码、版本
```

**API 的三个特点**：

| 特点 | 说明 |
|------|------|
| 与语言无关 | REST/gRPC 是跨语言的契约 |
| 关注稳定性 | 一旦发布，破坏性变更代价极高（调用方全崩） |
| 是最小单位 | 只有 API 的接口 ≠ SDK——缺工具、文档、示例 |

> 💡 一个只有 REST 接口的服务 + 一个第三方写的 Java 客户端封装 = 可以算"事实上的 SDK"——**SDK 的边界是模糊的，价值在于封装质量**。

---

## 4. SDK：工具箱

**SDK = API 的"开发者友好实现"**。同样一个云服务，SDK 比裸 API 多交付了：

| 裸 API 调用 | SDK 调用 |
|------------|---------|
| 手写签名逻辑（HMAC、时间戳、nonce） | `new Client(credentials)` 自动签名 |
| 手写超时/重试/退避 | 内置重试策略与连接池 |
| 手动解析 JSON、处理错误码 | 类型安全的对象 + 结构化异常 |
| 找文档拼参数 | IDE 补全 + Javadoc + 示例 |

```java
// 裸 API（伪代码）：签名 + 请求 + 解析，每个调用方都要写一遍
String sign = hmacSha256(secret, timestamp + nonce + payload);
HttpResponse resp = httpClient.post(url, headers(sign), payload);
Order o = parseOrder(resp.body());

// SDK：一行（鉴权、重试、解析全部内置）
Order o = orderClient.getOrder("A1");
```

**SDK 的职责边界**（好 SDK 的标准，见 05 模块）：

- **封装但透明**：90% 场景自动处理，10% 边缘场景留"逃生通道"（`rawRequest()`）；
- **默认安全**：密钥不落日志、TLS 默认开启、超时默认合理；
- **类型安全**：编译期检查参数与响应，而不是运行时才炸。

> 🎯 **核心要点**：**SDK 是"API 的产品化"**——API 解决"能不能调"，SDK 解决"调得爽不爽"。面试答区别的标准句式：**"API 是契约，SDK 是契约的开发者友好实现；SDK = API + 封装 + 工具 + 文档 + 示例"**。

---

## 5. Framework：骨架

**Framework（框架）**：**控制反转的骨架**——框架规定代码组织方式与调用流程，开发者填"扩展点"（好莱坞原则：Don't call us, we'll call you）。

```java
// Framework 的典型体验：框架控制流程，你实现接口
// Spring：容器管生命周期，你写 @Service；MyBatis：框架管 SQL 执行，你写 Mapper 接口
@Transactional
public void transfer(...) { ... }     // 事务边界由框架代理控制 —— 你只写业务
```

| 维度 | SDK | Framework |
|------|:---:|:---------:|
| 控制流 | **你调用 SDK** | **框架调用你** |
| 侵入性 | 低（局部使用） | 高（项目骨架即框架） |
| 退出成本 | 低（换实现） | 高（重构项目结构） |
| 典型 | JDK、AWS SDK、支付 SDK | Spring、MyBatis、Netty |

**判断口诀**：**"调用它"的是 SDK，"被它调用"的是 Framework**——一句话区分。

> 💡 混合体：Spring 既是 Framework（容器与 AOP）也包含 SDK（`spring-web` 的 RestClient 等工具库）——概念是光谱不是二分。

---

## 6. Platform：生态

**Platform（平台）**：提供**能力 + 规则 + 生态**的最上层——开发者在平台上构建应用与业务：

| 平台 | 提供的 SDK/Framework | 生态 |
|------|---------------------|------|
| Android | Android SDK、Jetpack | 应用市场、设备矩阵 |
| 微信开放平台 | 微信支付 SDK、小程序框架 | 商户生态、流量 |
| AWS | AWS SDK（Java/Go/Python…） | 云服务市场、认证体系 |
| Java 生态（JDK 本身） | JDK（Java 的 Platform + SDK 一体） | 构建工具/框架/中间件 |

**Platform 的关键特征**：

1. **多语言 SDK**——同一平台能力面向 Java/Go/Python 等多语言提供 SDK；
2. **治理规则**——认证、审核、计费、SLA；
3. **网络效应**——开发者越多，平台价值越大。

> 🎯 **核心要点**：平台 = "SDK/Framework 的提供者 + 规则制定者 + 生态运营者"。JDK 的特殊之处：**Java 平台与其 SDK（JDK）由同一实体交付**——所以"Java 的 SDK 是什么"答案是 JDK 本身。

---

## 7. 从 OpenAPI 到 SDK 的生成

**现代 SDK 生产方式**：从接口规范（OpenAPI/Protobuf）**自动生成**多语言 SDK——保证"规范即真相"：

```text
OpenAPI 规范（YAML：端点/参数/响应/错误码）
        │  生成器（OpenAPI Generator / Swagger Codegen）
        ▼
Java SDK（ApiClient + DTO + 异常）  Go SDK  Python SDK  TS SDK
```

```yaml
# openapi.yaml（片段）：规范即契约
paths:
  /orders/{id}:
    get:
      summary: 查询订单
      parameters: [{ name: id, in: path, required: true, schema: { type: string } }]
      responses:
        '200': { $ref: '#/components/schemas/Order' }
        '404': { description: 订单不存在 }
```

**生成式 SDK 的优劣**：

| 优点 | 缺点 |
|------|------|
| 规范驱动，多语言一致 | 生成代码"不够地道"（命名/风格差） |
| 零手写，随规范更新 | 复杂交互（签名、流式、回调）需手工增强 |
| 适合云 API 等标准场景 | 领域 SDK（支付/区块链）需专家手工打磨 |

> 💡 现代实践（Strapi 等 2025 最佳实践）：**规范生成打底 + 手工增强封装**——生成器负责"骨架与 DTO"，工程师负责"鉴权、重试、错误语义、文档示例"这些 SDK 的灵魂。

---

## 8. SDK 的类型全景

| 类型 | 代表 | 封装的核心复杂度 |
|------|------|----------------|
| 语言 SDK | JDK（Java 平台自身） | 编译器、运行时、标准库 |
| 云 SDK | AWS SDK、阿里云 SDK | 签名、分页、限流、重试 |
| 支付 SDK | 微信支付、支付宝 SDK | 签名验签、回调验签、证书管理 |
| 消息/中间件 SDK | Kafka 客户端、RabbitMQ 客户端 | 协议、连接管理、消费语义 |
| 移动 SDK | Android SDK、iOS SDK | 平台框架、生命周期 |
| 数据 SDK | JDBC、Redis 客户端 | 连接池、协议、类型映射 |
| **AI SDK** | OpenAI SDK、DeepSeek SDK、Spring AI | **流式响应、Token 计费、模型版本、上下文管理** |

**AI SDK 的新维度**（2024-2026 新兴）：

```java
// AI SDK 封装的不是"接口调用"，而是"对话状态机"：
ChatClient client = ChatClient.builder(openAiApi).build();
String answer = client.prompt()
        .system("你是订单助手")
        .user("查一下 A1 订单状态")
        .call()          // 或 .stream() —— 流式输出是 AI SDK 的核心能力之一
        .content();
```

> 🎯 **核心要点**：SDK 的类型随平台演进——**AI SDK 是当前增长最快的类型**，它封装的复杂度从"协议细节"升级到"模型交互语义"（流式、上下文、函数调用）。理解通用 SDK 原理，AI SDK 只是新的封装对象（详见 04 模块与 03-AI 层）。

---

**下一模块**：[02-JDK解剖-Java开发套件的组成](./02-JDK解剖-Java开发套件的组成.md) / **返回总览**：[00-SDK知识体系总览](./00-SDK知识体系总览.md)
