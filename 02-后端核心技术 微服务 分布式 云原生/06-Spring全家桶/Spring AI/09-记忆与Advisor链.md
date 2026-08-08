# 09-记忆与 Advisor 链
> 多轮会话记忆的实现层次、ChatMemory 抽象、记忆与工具循环的交互、Advisor 链排序与自定义扩展

## 📚 目录
1. [记忆的三个层次](#1-记忆的三个层次)
2. [ChatMemoryRepository 抽象](#2-chatmemoryrepository-抽象)
3. [窗口裁剪策略](#3-窗口裁剪策略)
4. [两个记忆 Advisor 的区别](#4-两个记忆-advisor-的区别)
5. [记忆与工具循环的交互（2.0 排序）](#5-记忆与工具循环的交互20-排序)
6. [自定义 Advisor 实战](#6-自定义-advisor-实战)
7. [核心要点](#7-核心要点)
8. [参考来源](#8-参考来源)

## 1. 记忆的三个层次

| 层次 | 载体 | 作用域 | 实现 |
|------|------|--------|------|
| 上下文记忆 | Prompt 内消息 | 单次对话 | `Message` 列表 |
| 会话记忆 | `ChatMemory` | 多轮对话（同会话） | `ChatMemoryRepository` + 记忆 Advisor |
| 长期记忆 | 外部存储 | 跨会话（用户画像/偏好） | 向量库/DB 检索（RAG 化记忆） |

> 🎯 **核心要点**：Spring AI 的"记忆"默认指**会话记忆**——把历史消息注入当前 prompt（上下文窗口管理），并随轮次回写存储。跨会话长期记忆是 RAG 的活学活用（用户资料入库检索），不是框架内置。

## 2. ChatMemoryRepository 抽象

```text
ChatMemoryRepository（接口）
 ├── add(conversationId, messages)        写入消息
 ├── get(conversationId, lastN)           读取最近 N 条
 ├── clear(conversationId)                清空会话
 └── 实现
      ├── InMemoryChatMemoryRepository（默认，进程内）
      ├── Redis 实现（spring-ai-starter-memory-redis）
      └── 其他外部存储实现（DB/向量库自定义）
```

```java
@Bean
ChatMemory chatMemory() {
    return new ChatMemory(chatMemoryRepository, MessageWindowChatMemory.builder()
            .maxMessages(20)
            .build());
}
```

## 3. 窗口裁剪策略

| 策略 | 机制 | 特点 |
|------|------|------|
| `MessageWindowChatMemory` | 按消息条数滑窗（默认） | 简单；2.0 裁剪点**前移到最近 USER 消息**，避免切分对话轮次 |
| 时间窗口 | 按时间过期 | 会话时效管理 |
| token 预算 | 按 token 容量滑窗 | 与模型上下文对齐 |

```java
MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
        .maxMessages(20)                    // 最近 20 条
        .build();
```

> ⚠️ **裁剪切分问题（2.0 修复）**：旧版按条数硬切可能把"用户提问-助手回答"切在中间（上下文残缺）；2.0 将裁剪点吸附到最近 USER 消息边界，保证轮次完整。

## 4. 两个记忆 Advisor 的区别

| Advisor | 注入方式 | 适用 |
|---------|---------|------|
| `MessageChatMemoryAdvisor` | 注入原始消息列表（含角色/工具消息） | 保留完整历史，配合长上下文模型 |
| `PromptChatMemoryAdvisor` | 将历史**摘要为一段文本**注入 system prompt | 历史长、上下文预算紧 |

| 选择依据 | 建议 |
|----------|------|
| 历史短（< 窗口） | MessageChatMemoryAdvisor（保真） |
| 历史长 / 成本敏感 | PromptChatMemoryAdvisor（压缩） |
| 工具调用密集 | 两者都可，注意与工具循环的交互（见下节） |

## 5. 记忆与工具循环的交互（2.0 排序）

```text
2.0 默认链序：
[记忆 Advisor] → [ToolCallingAdvisor（工具循环）] → [ChatModel]
     ▲                                            │
     └────────── 记忆在循环之外，只注入一次 ────────┘
```

| 设计决策 | 原因 |
|----------|------|
| 记忆在工具循环之外 | 大多数 `ChatMemoryRepository` 不存储工具调用内容；若记忆进循环，每轮都会重复注入历史导致上下文膨胀 |
| 工具循环内部历史 | `ToolCallingAdvisor` 自己维护中间对话历史（工具请求/结果），不落记忆存储 |
| `DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER` 下调 | 2.0 调整记忆 Advisor 优先级，使其包裹工具循环而非参与每轮 |

> ⚠️ **易错点**：自定义记忆逻辑时别把记忆 Advisor 排在 `ToolCallingAdvisor` 之内——每轮工具循环都注入记忆会造成 prompt 膨胀与工具判定漂移。

## 6. 自定义 Advisor 实战

```java
// 自定义 Advisor：记录调用成本
public class CostLoggingAdvisor implements CallAdvisor {

    @Override
    public ChatResponse aroundCall(AdvisorCallContext context, CallAdvisorChain chain) {
        ChatResponse response = chain.next(context);   // 继续链
        Usage usage = response.getMetadata().getUsage(); // token 用量
        log.info("会话={}, promptTokens={}, completionTokens={}",
                context.getConversationId(), usage.getPromptTokens(), usage.getCompletionTokens());
        return response;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;   // 最内层，靠近模型
    }
}
```

```java
// 注册与挂载
@Bean
CostLoggingAdvisor costLoggingAdvisor() { return new CostLoggingAdvisor(); }

ChatClient client = builder.defaultAdvisors(costLoggingAdvisor()).build();
```

| Advisor 扩展点 | 签名 |
|----------------|------|
| 同步调用 | `CallAdvisor.aroundCall(context, chain)` |
| 流式调用 | `StreamAdvisor.aroundStream(context, chain)`（返回 `Flux<ChatResponse>`） |
| 递归支持 | 链上 `copy()` 可构造子链（见 [04](04-工具调用与Agent开发.md) 第 5 节） |

## 7. 核心要点

> 🎯 **核心要点**：
> - 记忆 = ChatMemoryRepository 存储 + 记忆 Advisor 注入 + 窗口裁剪策略；
> - 两 Advisor 选择：保真（Message）vs 压缩（Prompt），按历史长度定；
> - 2.0 记忆在工具循环之外、只注入一次——自定义记忆别排进工具循环；
> - 自定义能力（成本、审计、安全）统一走 `CallAdvisor`/`StreamAdvisor` 实现。

## 8. 参考来源

- [Spring AI Reference：Chat Memory](https://docs.spring.io/spring-ai/reference/api/chat-memory.html)
- [Spring AI Reference：Advisors](https://docs.spring.io/spring-ai/reference/api/advisors.html)
- [Spring AI 2.0 API：advisor 包](https://docs.spring.io/spring-ai/docs/2.0.0/api/org/springframework/ai/chat/client/advisor/package-summary.html)
- [Spring AI 2.0.0 GA 发布博客（记忆排序调整）](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now)

---

**下一模块**：[10-可观测性评估与生产避坑](10-可观测性评估与生产避坑.md)　/　**返回总览**：[00-总览](00-Spring%20AI总览.md)
