# 04 第三方 SDK 集成实战

> 支付、云、AI SDK 的集成是后端日常——从评估到接入、从签名验签到防坑，一套完整的方法论让"接 SDK"变成可控工程

---

## 📚 目录

1. [集成前评估：四维检查清单](#1-集成前评估四维检查清单)
2. [接入流程六步](#2-接入流程六步)
3. [鉴权机制：API Key / OAuth / 签名验签](#3-鉴权机制api-key--oauth--签名验签)
4. [可靠性设计：超时、重试与熔断](#4-可靠性设计超时重试与熔断)
5. [常见坑与防坑手册](#5-常见坑与防坑手册)
6. [AI SDK 集成：新时代的新坑](#6-ai-sdk-集成新时代的新坑)

---

## 1. 集成前评估：四维检查清单

**接入任何第三方 SDK 前，先回答四个维度的问题**（2025 最佳实践的结构化评估）：

| 维度 | 检查项 | 红线 |
|------|--------|------|
| **质量** | 文档版本是否与代码锁定？quick-start 能否不改动跑通？发布节奏？issue 响应？ | 文档与版本脱节 = 弃用 |
| **安全** | 密钥如何处理？默认安全配置？已知漏洞与修复速度？依赖树是否干净？ | 依赖里有高危 CVE 未修 = 弃用 |
| **兼容** | Java 版本要求？依赖冲突面？序列化/框架集成（Spring/Jackson）？ | 强制升级你的基线 = 谨慎 |
| **维护** | 是否活跃（最近 6 个月提交）？商业支持？许可证（Apache/MIT/商业）？ | 停滞项目 + 无支持 = 高风险 |

```java
// 评估动作：依赖树审查（安全面）
mvn dependency:tree                     // 看传递依赖
mvn versions:display-dependency-updates // 看版本更新
// 漏洞扫描：OWASP Dependency-Check / GitHub Dependabot
```

> 🎯 **核心要点**：**评估优先级 = 安全 > 维护 > 兼容 > 功能**——功能缺失可以绕过，安全漏洞无法容忍。线上事故统计中"第三方依赖漏洞"是高频来源。

---

## 2. 接入流程六步

```text
① 依赖引入（评估通过） → ② 配置与初始化 → ③ 最小调用验证 → ④ 错误处理与封装
→ ⑤ 回调/异步处理 → ⑥ 监控与灰度
```

```java
// 步骤 2-3：初始化与最小验证（以支付 SDK 为例）
@Configuration
public class PayConfig {
    @Bean
    public PayClient payClient(PayProperties props) {      // 配置集中管理（08 模块）
        return PayClient.builder()
                .appId(props.appId())
                .privateKey(props.privateKey())            // 私钥从环境变量/密钥管理注入
                .gatewayUrl(props.gatewayUrl())
                .timeout(Duration.ofSeconds(10))
                .build();
    }
}

// 步骤 4：业务层封装 —— SDK 异常统一转为业务异常（见 Java异常体系/06）
@Service
public class PayService {
    public PayResult pay(Order o) {
        try {
            return payClient.pay(new PayRequest(o.getId(), o.getAmount()));
        } catch (PaySdkException e) {                      // SDK 自己的异常
            throw new PayException(ErrorCode.PAY_FAILED, "支付调用失败", e);
        }
    }
}
```

**六步的完成标准**：每一步有验证动作——"最小调用验证"必须在沙箱环境跑通一个真实事务；"监控与灰度"必须有失败率与耗时指标（见第 4 节）。

> 🎯 **核心要点**：**SDK 调用必须包一层自己的服务**（Service 层包装）——调用方不直接依赖 SDK 类型，换 SDK/升级 SDK 时只改一个类（依赖倒置，见 `Java面向对象/09`）。

---

## 3. 鉴权机制：API Key / OAuth / 签名验签

**三种主流鉴权**：

| 机制 | 原理 | 适用 |
|------|------|------|
| API Key | 请求头带静态密钥，明文传输（配合 TLS） | 服务端到服务端（简单场景） |
| OAuth 2.0 | 授权码/客户端凭证换 token，token 短期有效 | 用户授权、跨平台 |
| **签名验签** | 请求参数 + 密钥 → HMAC/RSA 签名，服务端验签 | **支付/资金类（强安全）** |

**签名验签为什么必须**（支付 SDK 的核心安全机制）：

```text
客户端签名：参数 → 排序 → 拼接 → HMAC-SHA256(密钥) 或 RSA 私钥签名 → 附签名发送
服务端验签：收到参数 → 同样规则算签名 → 比对 / 用公钥验签
  目的：① 防篡改（改一个字段签名就不匹配）② 防伪造（无密钥无法生成合法签名）
回调验签：支付结果回调同样要验签 —— 防止伪造回调（资金安全红线）
```

```java
// 回调验签的工程要点（支付 SDK 集成必考）
@PostMapping("/pay/callback")
public String onCallback(@RequestBody String body, @RequestHeader("X-Signature") String sign) {
    boolean valid = payClient.verifySignature(body, sign);     // ① 先验签！
    if (!valid) {
        log.warn("回调验签失败: {}", body);
        return "fail";                                          // 拒绝，不处理
    }
    // ② 验签通过 → 幂等处理（按订单号去重，防重复回调）
    orderService.markPaid(orderId);
    return "success";                                           // ③ 返回成功，停止重推
}
```

**回调处理的三个铁律**：**先验签 → 后幂等 → 快返回**（"success" 字面要匹配，否则平台重推）。

> 🎯 **核心要点**：集成任何涉及资金的 SDK，**验签是安全底线**——私钥/密钥必须走密钥管理（环境变量、KMS、Vault），禁止进代码库与日志；验签失败一律拒绝并告警。

---

## 4. 可靠性设计：超时、重试与熔断

**SDK 调用是外部依赖——必须有超时、重试与降级**：

```java
// 客户端配置：超时（TCP 连接 + 读超时都要设！）
PayClient client = PayClient.builder()
        .connectTimeout(Duration.ofSeconds(3))    // 连接超时
        .readTimeout(Duration.ofSeconds(10))      // 读超时 —— 不设则可能无限挂起
        .build();

// 重试：只对"可重试错误"重试（网络抖动/5xx/超时），业务失败（4xx/验签失败）绝不重试
@Retryable(retryFor = {IOException.class, PayTimeoutException.class},
           noRetryFor = {PayBusinessException.class},
           backoff = @Backoff(delay = 500, multiplier = 2, maxDelay = 5000))
public PayResult pay(Order o) { return payClient.pay(...); }
// Spring 6.3+/Boot 4：@Retryable 已是原生注解（Spring Framework 7.0 内置）

// 熔断与降级：连续失败快速失败，不再打外部系统
// Spring 7.0 原生 @ConcurrencyLimit；或 Resilience4j CircuitBreaker
```

**重试三问**（面试必问）：

| 问题 | 答案 |
|------|------|
| 哪些错误可重试？ | 网络错误、超时、5xx——**4xx 与业务拒绝不可重试** |
| 重试要考虑幂等吗？ | **必须**——重试前确认接口幂等（支付单号去重） |
| 重试策略？ | 指数退避 + 抖动（jitter），防重试风暴 |

> 🎯 **核心要点**：SDK 调用的可靠性三层 = **超时（不挂起）→ 重试（可重试错误 + 退避）→ 熔断（连续失败快速失败）**——缺一层，线上事故多三分。Spring 7.0 已把 @Retryable/@ConcurrencyLimit 收进框架（无需 Spring Retry 依赖）。

---

## 5. 常见坑与防坑手册

| # | 坑 | 表现 | 防坑 |
|:-:|-----|------|------|
| 1 | **依赖版本冲突** | SDK 传递依赖（如 Jackson）与项目版本冲突 | 评估时看依赖树；`dependencyManagement` 统一版本 |
| 2 | **密钥泄漏** | 私钥打进配置/日志/仓库 | 密钥走环境变量/KMS；日志脱敏（见 Java异常体系/08） |
| 3 | **线程安全误用** | SDK 客户端非线程安全，却在多线程共享 | 看文档（多数官方客户端线程安全可共享；不安全的用 ThreadLocal/池） |
| 4 | **未设超时** | 调用永久挂起，线程池耗尽 | 连接/读超时必设 |
| 5 | **不设重试/乱重试** | 业务失败被重试放大 | 按第 4 节分类 |
| 6 | **回调处理错误** | 不验签/不幂等/不返回成功 | 第 3 节三铁律 |
| 7 | **升级不回归** | SDK 升级后行为变化（签名算法/字段变更） | 升级走灰度；变更记录对照 |
| 8 | **忽略关闭** | 客户端连接池不关闭（应用退出/重建泄漏） | 客户端实现 AutoCloseable → TWR 或容器管理（见 Java异常体系/04） |
| 9 | **序列化不兼容** | 与 Jackson 版本/命名策略冲突 | 评估时验证序列化 |
| 10 | **错误处理一刀切** | 所有 SDK 异常都当业务失败 | 区分可重试/不可重试/需人工（见第 4 节） |

> 🎯 **核心要点**：SDK 坑的**总根源 = 把外部依赖当本地代码**——外部调用必须"带超时、防泄漏、能降级、可观测"，四件套缺一不可。

---

## 6. AI SDK 集成：新时代的新坑

**AI SDK（OpenAI/DeepSeek/Spring AI）在传统 SDK 问题之上，新增了独特维度**：

| 新维度 | 说明 | 工程应对 |
|--------|------|---------|
| **流式响应** | 输出是流（SSE），不是一次性 JSON | `stream()` API + 背压；断流重连 |
| **Token 成本** | 每次调用按 token 计费，异常重试会烧钱 | 重试策略更保守；缓存结果（`Spring AI` 缓存） |
| **模型版本漂移** | 模型升级后输出行为变化 | 锁定模型版本（`deepseek-chat` 固定）；契约测试 |
| **延迟波动** | 生成式响应秒级~十秒级 | 超时要放宽（30-60s）；异步化 |
| **内容安全** | 输出可能含敏感/有害内容 | 输出过滤；审计日志 |

```java
// AI SDK 集成要点（Spring AI 为例）
@Configuration
public class AiConfig {
    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.defaultSystem("你是订单助手，只回答订单相关问题").build();
    }
}

// 调用：流式 + 超时 + 降级
public String ask(String question) {
    try {
        return chatClient.prompt().user(question)
                .call().content();                    // 或 .stream() 流式
    } catch (AiOpenAiException e) {
        log.error("AI 调用失败", e);
        return "AI 服务暂不可用，请稍后重试";            // 降级：兜底文案
    }
}
```

**AI SDK 的成本控制**（独特课题）：

- **缓存**：相同/相似问题结果缓存（Redis + 语义哈希）；
- **Token 预算**：上下文裁剪（只送必要历史）、max_tokens 限制；
- **重试策略**：失败重试的 token 成本 = 原始调用 × 重试次数——**AI 场景重试必须更谨慎**。

> 🎯 **核心要点**：AI SDK = 传统 SDK 问题（超时/重试/密钥）+ **流式、成本、模型漂移**三个新维度。集成前先回答"模型挂了怎么办、超时了怎么办、烧钱了怎么办"（详见 03-AI 层系统）。

---

**下一模块**：[05-自研SDK设计指南](./05-自研SDK设计指南.md) / **返回总览**：[00-SDK知识体系总览](./00-SDK知识体系总览.md)
