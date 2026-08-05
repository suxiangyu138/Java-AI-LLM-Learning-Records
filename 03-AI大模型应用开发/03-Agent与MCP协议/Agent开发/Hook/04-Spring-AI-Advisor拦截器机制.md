# 04 - Spring AI Advisor 拦截器机制

> 🎯 Spring AI 的 Advisor = 适配 AI 对话场景的拦截器 — 洋葱模型 + 栈式顾问链 + 框架自动追加末端 LLM 调用。Java 后端最熟悉的 Hook 范式：与 Spring MVC Interceptor 同源设计

---

## 目录

1. [Advisor 核心接口](#1-advisor-核心接口)
2. [洋葱模型与栈式链](#2-洋葱模型与栈式链)
3. [Callback vs Stream 两种模式](#3-callback-vs-stream-两种模式)
4. [ToolCallingAdvisor：工具循环拦截（2.0）](#4-toolcallingadvisor工具循环拦截20)
5. [内置 Advisor 清单](#5-内置-advisor-清单)
6. [自定义 Advisor 实战](#6-自定义-advisor-实战)

---

## 1. Advisor 核心接口

```java
// ① 顶层接口：名字 + 排序
public interface Advisor extends Ordered {
    String getName();
    default int getOrder() { return Ordered.LOWEST_PRECEDENCE; }
}

// ② 同步拦截器（阻塞等待完整响应）
public interface CallAdvisor extends Advisor {
    ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain);
}

// ③ 流式拦截器（返回 Flux 流）
public interface StreamAdvisor extends Advisor {
    Flux<ChatClientResponse> adviseStream(ChatClientRequest request,
                                          StreamAdvisorChain chain);
}
```

**排序策略：** 数值越小越先执行（请求阶段先执行，响应阶段后执行）。预留 `DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER = Ordered.HIGHEST_PRECEDENCE + 1000` — 给用户自定义 Advisor 留了 1000 个更高优先级空位。

---

## 2. 洋葱模型与栈式链

### 2.1 执行顺序（栈式 Deque）

```java
// DefaultAroundAdvisorChain 内部实现
// ① 所有 Advisor 按 order 排序 → 压栈
// ② nextCall() 每次从栈顶 pop()
// ③ 执行 adviseCall → 内部调用 chain.nextCall(request)
// ④ 递归直到栈空

// 最后一个 Advisor 由框架自动添加：ChatModelCallAdvisor
// 它不调用 chain.next()，而是直接调用 chatModel.call() 真正触发 LLM
```

```text
请求方向（Order 小→大，先进先执行）：
  Advisor A(order=1) → Advisor B(order=2) → ChatModelCallAdvisor(★LLM调用)
响应方向（逆序返回）：
  ChatModelCallAdvisor → Advisor B → Advisor A
```

### 2.2 AdvisorContext 状态共享

```java
// 顾问链间通过 advise-context（Map）共享状态
request.getContext().put("request_id", requestId);    // A 存入
request.getContext().get("request_id");               // B 读取
```

---

## 3. Callback vs Stream 两种模式

| 对比 | CallAdvisor（同步） | StreamAdvisor（流式） |
|------|--------------------|------------------------|
| 返回值 | `ChatClientResponse` | `Flux<ChatClientResponse>` |
| 适用 | 普通问答、结构化输出、统计 Token | 聊天窗口打字机效果、实时敏感词过滤 |
| 阻塞 | 是 | 否 |

```java
// BaseAdvisor 的默认实现（开发者只需重写 before/after）
public ChatClientResponse adviseCall(ChatClientRequest req, CallAdvisorChain chain) {
    ChatClientRequest processed = before(req);           // 前置处理
    ChatClientResponse response = chain.nextCall(processed); // → 下一环
    return after(response);                              // 后置处理
}
```

---

## 4. ToolCallingAdvisor：工具循环拦截（2.0）

Spring AI 2.0 新增的 `ToolCallingAdvisor` — 将工具调用循环纳入顾问链，让其他 Advisor 也能拦截工具调用过程：

| 生命周期钩子 | 时机 |
|-------------|------|
| `doInitializeLoop` / `doInitializeLoopStream` | 工具循环开始 |
| `doBeforeCall` / `doBeforeStream` | 循环中每次 LLM 调用前 |
| `doAfterCall` / `doAfterStream` | 每次 LLM 调用后 |
| `doGetNextInstructionsForToolCall` | 决定下一轮工具调用指令 |
| `doFinalizeLoop` / `doFinalizeLoopStream` | 工具循环结束 |

**被 `ChatClient` 自动注册**（除非显式禁用），实现 `ToolAdvisor` 标记接口防止重复注册。

---

## 5. 内置 Advisor 清单

| Advisor | 类型 | 功能 |
|---------|------|------|
| **MessageChatMemoryAdvisor** | 记忆 | 将对话历史注入 Prompt |
| **VectorStoreChatMemoryAdvisor** | 记忆 | 从向量库检索注入 system text |
| **QuestionAnswerAdvisor** | RAG | Naive RAG（检索→拼接→问答） |
| **RetrievalAugmentationAdvisor** | RAG | 模块化 RAG 架构（可插拔检索模块） |
| **ReReadingAdvisor** | 推理 | RE2 重读策略（提升推理质量） |
| **ToolCallingAdvisor** | 工具 | 工具调用循环拦截（2.0） |
| **SafeGuardAdvisor** | 安全 | 防止生成有害内容 |

---

## 6. 自定义 Advisor 实战

```java
// ① 日志 Advisor
public class LoggingAdvisor implements CallAdvisor {
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest req, CallAdvisorChain chain) {
        long start = System.currentTimeMillis();
        ChatClientResponse resp = chain.nextCall(req);
        log.info("LLM call {}ms, tokens={}, model={}",
            System.currentTimeMillis() - start,
            resp.getMetadata().getUsage(),
            resp.getMetadata().getModel());
        return resp;
    }
    @Override public String getName() { return "logging"; }
    @Override public int getOrder() { return Ordered.LOWEST_PRECEDENCE; }
}

// ② 注入 Advisor
ChatClient client = ChatClient.builder(chatModel)
    .defaultAdvisors(new LoggingAdvisor(), new MessageChatMemoryAdvisor(memory))
    .build();
```

**追问：** Advisor 和 Spring MVC Interceptor 的核心差异？→ Interceptor 拦截 HTTP 请求；Advisor 拦截 AI 对话的 Prompt→LLM→Response 链路。两者都是栈式链+Order 排序+after 逆序 — 同一种设计模式在不同领域。

---

> 🎯 **核心要点**：Spring AI Advisor = **栈式链实现洋葱模型**（order 排序→压栈→pop 递归执行→框架追加末端 LLM 调用）。生产标配：**MessageChatMemoryAdvisor（记忆）+ SafeGuardAdvisor（安全）+ LoggingAdvisor（可观测）+ QuestionAnswerAdvisor（RAG）**。Java 后端的优势 — 这跟写 Spring MVC Interceptor 完全一个套路。

**下一模块**：[05-Spring AI Alibaba Hooks与Interceptors](05-Spring%20AI%20Alibaba%20Hooks与Interceptors.md) / **返回总览**：[00-Hook总览](00-Hook知识体系总览.md)
