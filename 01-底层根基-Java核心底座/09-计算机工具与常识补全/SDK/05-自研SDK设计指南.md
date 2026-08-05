# 05 自研 SDK 设计指南

> 给内部服务或外部平台设计 SDK，本质是设计"开发者体验"——抽象与透明的平衡、类型安全、版本策略与开箱即用，决定 SDK 的生死

---

## 📚 目录

1. [设计原则：抽象与透明的平衡](#1-设计原则抽象与透明的平衡)
2. [API 设计：流畅、类型安全、可发现](#2-api-设计流畅类型安全可发现)
3. [错误处理设计](#3-错误处理设计)
4. [版本化策略：语义化与兼容](#4-版本化策略语义化与兼容)
5. [文档与示例：开箱即用](#5-文档与示例开箱即用)
6. [测试与沙箱](#6-测试与沙箱)
7. [发布流水线](#7-发布流水线)

---

## 1. 设计原则：抽象与透明的平衡

**好 SDK 的第一原则（2025 行业共识）**：**自动处理 90% 常见场景，同时为 10% 边缘场景留"逃生通道"**——

```text
抽象过多（黑盒）：一切帮你做 → 出问题无法排查、特殊场景无法绕过
透明过多（裸 API）：啥都自己来 → SDK 没有存在意义
平衡点：默认智能（重试/签名/超时）+ escape hatch（rawRequest / 底层客户端可访问）
```

| 原则 | 说明 | 反例 |
|------|------|------|
| **封装协议，不封装语义** | 签名/重试/序列化交给 SDK；业务决策留给调用方 | SDK 偷偷改变业务数据 |
| **默认安全** | 密钥不落日志、TLS 默认、超时默认合理 | 默认不校验返回 |
| **最小暴露** | 公开 API 少而精——**"以后加容易，现在删很难"** | 一上来几十个 public 方法 |
| **类型安全** | 参数用类型表达（枚举/record），不用字符串魔法 | `String channelType` 随意传 |
| **可观测** | 请求日志可开关、错误带上下文 | 出问题只能靠猜 |
| **零配置起步** | 默认值让"第一个 Demo"零配置跑通 | 要配 10 个参数才能跑 |

> 🎯 **核心要点**：SDK 是"**产品**"而非"代码"——它的用户是开发者，设计标准是"第一次用能不能 5 分钟跑通、出问题能不能 5 分钟定位"。**默认智能 + 逃生通道**是这一切的支点。

---

## 2. API 设计：流畅、类型安全、可发现

**Builder 模式做初始化**（参数多 + 默认值多）：

```java
// 客户端初始化：Builder + 必填校验 + 默认值（见 Java面向对象/06 Builder）
PayClient client = PayClient.builder()
        .appId("wx123")
        .privateKey(pem)                    // 必填
        .gatewayUrl("https://api.pay.com")  // 可选，有默认
        .timeout(Duration.ofSeconds(10))    // 可选，有默认
        .build();                           // 校验：缺 appId 直接抛
```

**领域模型用类型表达**（record 是 SDK 建模的现代标配）：

```java
// ✅ 类型安全：请求/响应用 record（不可变 + 值相等 + 组件即文档）
public record PayRequest(String orderId, BigDecimal amount, String channel) {
    public PayRequest {                     // 紧凑构造器：参数校验内建
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("金额必须为正");
        }
    }
}

public record PayResult(String transactionId, PayStatus status, Instant paidAt) { }
public enum PayStatus { PENDING, SUCCESS, FAILED }
// ❌ 反例：Map<String, Object> request = Map.of("orderId", ...) —— 无类型、无校验、无文档
```

**方法签名设计**：

| 原则 | 示例 |
|------|------|
| 动词 + 名词 | `pay(request)`、`queryOrder(id)` |
| 参数收敛 | 请求对象传一个，不传 5 个散参 |
| 返回不可变 | record / 不可变集合（见 Java集合框架/08） |
| 同步 + 异步并存 | `pay()` 与 `payAsync()` / `CompletableFuture` |

> 🎯 **核心要点**：SDK API 的可发现性 = **"看一眼方法名和参数就知道怎么用"**——record + 枚举 + Builder 三件套让 SDK 的"签名即文档"。

---

## 3. 错误处理设计

**SDK 错误处理是开发者体验的关键**——三条设计线：

**① 结构化异常体系**（继承层次 + 分类）：

```java
// 异常层次：让调用方可以按粒度捕获
public class PaySdkException extends RuntimeException { }          // 顶层（基类）
public class PayAuthException extends PaySdkException { }          // 鉴权失败（检查密钥）
public class PayTimeoutException extends PaySdkException { }       // 超时（可重试）
public class PayBusinessException extends PaySdkException { }      // 业务拒绝（不可重试）

// 调用方按需捕获：
catch (PayAuthException e) { /* 提示检查密钥 */ }
catch (PayTimeoutException e) { /* 重试 */ }
catch (PaySdkException e) { /* 兜底 */ }
```

**② 错误信息带上下文与建议**：

```java
// ✅ 消息 = 发生了什么 + 为什么 + 怎么办
throw new PayAuthException("鉴权失败：appId 或密钥错误（appId=wx123），请检查凭据配置");
// ❌ 反例：throw new RuntimeException("请求失败")  —— 无从下手
```

**③ 错误对象与响应解耦**：SDK 异常携带 HTTP 状态、错误码、traceId（见 `Java异常体系/06` 的错误码与追踪设计）：

```java
public class PaySdkException extends RuntimeException {
    private final int httpStatus;
    private final String errorCode;      // 平台返回的业务错误码
    private final String traceId;        // 定位关联
    private final String rawResponse;    // 原始响应（排查用，不含密钥）
}
```

> 🎯 **核心要点**：SDK 异常设计的验收标准——**"调用方不看文档也能正确处理"**：可重试的异常类型上能区分、消息里能定位、错误码能对应平台文档。

---

## 4. 版本化策略：语义化与兼容

**语义化版本（SemVer）是 SDK 的生命线**：`主版本.次版本.修订号`

| 版本位 | 变更类型 | 例子 |
|:------:|---------|------|
| 主版本 | **破坏性变更**（改签名/删 API/改默认行为） | 2.0.0 |
| 次版本 | 向后兼容的新功能 | 1.5.0 |
| 修订号 | 向后兼容的缺陷修复 | 1.5.2 |

**兼容性策略**（SDK 特有的三条铁律）：

```text
① 永不删除 public API —— 弃用（@Deprecated + 文档说明替代品）→ 保留至少一个大版本
② 新增 API 永远是"加"不是"改" —— 扩展参数用新方法/新重载
③ 破坏性变更必须：主版本号 + 迁移指南 + 变更日志（Changelog）
```

**SDK 版本与平台版本**（两个版本都要管理）：

```java
// 场景：平台 API v1 → v2 时，SDK 怎么办？
// 方案：SDK 同时支持（PayClientV1 / PayClientV2）→ 给迁移期
// 或：SDK 版本映射平台版本（pay-sdk-2.x 对应平台 v2）
```

> 🎯 **核心要点**：**SDK 的版本承诺 = 消费者的信任**——"升级 SDK 不会破坏我的代码"是 SDK 传播的根基。破坏性变更 ≠ 禁止，而是必须"走流程"（版本号 + 迁移指南 + 过渡期）。

---

## 5. 文档与示例：开箱即用

**SDK 的文档标准（决定采用率的隐形因素）**：

| 交付物 | 标准 | 反例 |
|--------|------|------|
| **Quick-start** | 复制粘贴即可运行，3 分钟出结果 | 文档里到处是"请先配置您的环境" |
| **API 参考** | 与代码版本锁定（生成式 javadoc） | 文档描述的 API 在代码里不存在 |
| **示例工程** | 可运行的独立工程（含沙箱配置） | 示例代码片段缺 import 缺配置 |
| **迁移指南** | 每个主版本破坏性变更逐条列 | 升级全靠自己 diff |
| **Changelog** | 每个版本变更可追溯 | 版本说明一句话带过 |

```java
// Javadoc 标准：每个 public 方法三件套（做什么/参数/异常）
/**
 * 发起支付。
 *
 * @param request 支付请求（金额必须为正）
 * @return 支付结果
 * @throws PayAuthException    凭据错误，请检查 appId/密钥
 * @throws PayTimeoutException 请求超时，可重试（注意幂等）
 */
public PayResult pay(PayRequest request) { ... }
```

> 💡 现代实践：文档与代码**同源**（javadoc/OpenAPI 注释生成），避免"文档漂移"；quick-start 是 CI 里**真实执行的测试**（而不是手写示例）——"文档里能跑的代码"才算数。

---

## 6. 测试与沙箱

**SDK 的测试分层**（好 SDK 的隐形竞争力）：

| 层 | 手段 | 目标 |
|----|------|------|
| 单元测试 | Mock 传输层 | 签名/参数/错误处理逻辑全覆盖 |
| 契约测试 | 录制平台响应（fixtures） | 响应解析与平台真实行为一致 |
| **Mock 服务器** | SDK 自带 mock server | **调用方离线可测**（无需真实环境） |
| 沙箱集成 | 平台沙箱环境 | 端到端真实流程 |

```java
// 测试能力是 SDK 的产品特性：mock server 让调用方 CI 离线可测
// 伪代码：SDK 提供 mock 模式
PayClient client = PayClient.builder()
        .mode(PayClient.Mode.MOCK)          // 测试模式：本地 mock 响应
        .build();
```

**SDK 自带 mock 的价值**：调用方集成测试不依赖外部环境（CI 稳定、速度快、无成本）——**"能不能离线测"是开发者选择 SDK 的隐藏标准**（2025 行业最佳实践：mock servers + sandboxes + fixtures 三件套）。

> 🎯 **核心要点**：SDK 的测试不是"自己的测试"，而是"**让调用方更好测试**"——契约测试（防平台变更破坏解析）+ mock 能力（调用方离线集成）双管齐下。

---

## 7. 发布流水线

**SDK 的交付物与发布流程**：

```text
源码 → 构建（Maven/Gradle）→ 测试 → 发布制品 → 中央仓库/私服 → 文档站点 → 版本公告
```

```xml
<!-- 发布到 Maven Central 的关键元数据（pom.xml） -->
<groupId>com.example</groupId>
<artifactId>pay-sdk</artifactId>
<version>1.5.2</version>          <!-- 语义化版本 -->
<licenses>...</licenses>
<scm>...</scm>                    <!-- 源码地址：可审计性 -->
<properties><automatic-module-name>com.example.paysdk</automatic-module-name></properties>
```

**发布红线**：

| 红线 | 说明 |
|------|------|
| **发布即不可变** | 制品一旦发布不能改（Maven Central 规则）——版本号错了就发新版本 |
| 快照与正式版分离 | `-SNAPSHOT` 用于开发，正式版禁止依赖快照 |
| 签名与校验 | 发布 PGP 签名（Maven Central 要求），消费者校验 |
| 公告与变更日志 | 每个正式版有 Changelog + 升级提示 |
| 自动化 | GitHub Actions/流水线：构建 → 测试 → 发布 → 文档更新一体 |

> 🎯 **核心要点**：SDK 发布 = "**发布一次，服务千次**"——制品的不可变性、版本语义、文档同步，任何一环出错都会在消费者侧放大。发布流水线自动化是底线（详见 `Maven与Gradle/` 系统）。

---

**下一模块**：[06-多版本JDK管理与SDKMAN](./06-多版本JDK管理与SDKMAN.md) / **返回总览**：[00-SDK知识体系总览](./00-SDK知识体系总览.md)
