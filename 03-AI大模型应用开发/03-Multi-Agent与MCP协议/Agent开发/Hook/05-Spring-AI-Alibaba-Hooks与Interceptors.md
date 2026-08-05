# 05 - Spring AI Alibaba Hooks 与 Interceptors

> 🎯 Spring AI Alibaba 独创 Hooks + Interceptors 双层架构 — Hooks 管 Agent 高层流程节点（流程级控制），Interceptors 管模型/工具调用的底层数据交互（数据级修改）。与 LangChain Middleware、Spring AI Advisor 对照阅读

---

## 目录

1. [Hooks vs Interceptors：两种机制的分工](#1-hooks-vs-interceptors两种机制的分工)
2. [ModelHook 与 AgentHook](#2-modelhook-与-agenthook)
3. [ModelInterceptor 与 ToolInterceptor](#3-modelinterceptor-与-toolinterceptor)
4. [完整执行流程](#4-完整执行流程)
5. [内置 Hook/Interceptor 清单](#5-内置-hookinterceptor-清单)
6. [框架对比（LangChain vs Spring AI vs Alibaba）](#6-框架对比langchain-vs-spring-ai-vs-alibaba)

---

## 1. Hooks vs Interceptors：两种机制的分工

| 维度 | Hooks | Interceptors |
|------|-------|--------------|
| 定位 | **流程级控制** | **数据级修改** |
| 作用对象 | Agent 生命周期的关键节点 | 模型/工具调用的请求与响应 |
| 能力 | 中断执行、跳转、状态修改 | 修改输入输出、添加元数据、内容过滤 |
| 实现 | `ModelHook` / `AgentHook` | `ModelInterceptor` / `ToolInterceptor` |
| 类比 | Web Filter（可中断、可跳转） | Web HandlerInterceptor（数据处理） |

**核心区别一句话：** Hooks 决定"要不要继续执行"，Interceptors 决定"传给模型的数据长什么样"。

---

## 2. ModelHook 与 AgentHook

### 2.1 ModelHook — 模型调用级别拦截

```java
// 在每次模型调用前后执行
// @HookPositions 注解声明在哪个生命周期节点执行
public class SummarizationHook extends ModelHook {

    @HookPositions({BEFORE_MODEL, AFTER_MODEL})
    @Override
    public Map<String, Object> beforeModel(AgentState state, RunConfig config) {
        // 调用前：检查上下文长度 → 超限则自动摘要压缩
        if (estimateTokens(state) > maxTokens) {
            String summary = summarize(state);
            return Map.of("messages", compressMessages(state, summary));
        }
        return Map.of();  // 返回空 = 不变更
    }

    @Override
    public Map<String, Object> afterModel(AgentState state, RunConfig config) {
        // 调用后：记录 Token 消耗
        return Map.of("total_tokens", state.getTokenCount());
    }
}
```

### 2.2 AgentHook — Agent 级别拦截

```java
// 在 Agent 开始/结束时执行
public class AuditHook extends AgentHook {

    @Override
    public Map<String, Object> beforeAgent(AgentState state, RunConfig config) {
        // Agent 启动前：权限校验
        if (!hasPermission(state.getUserId(), state.getAction())) {
            throw new SecurityException("无权限执行此操作");
        }
        return Map.of();
    }

    @Override
    public Map<String, Object> afterAgent(AgentState state, RunConfig config) {
        // Agent 结束后：审计日志
        auditLog(state.getUserId(), state.getActions(), state.getResult());
        return Map.of();
    }
}
```

---

## 3. ModelInterceptor 与 ToolInterceptor

### 3.1 ModelInterceptor — 模型调用包裹

```java
// 包裹模型调用：在请求发出前/响应返回后修改数据
public class ContentFilterInterceptor implements ModelInterceptor {

    @Override
    public ModelResponse interceptModel(ModelRequest req, ModelCallHandler handler) {
        // 请求前：过滤敏感词
        req.addSystemMessage("不要输出任何手机号、身份证号。");
        // 调用模型
        ModelResponse resp = handler.handle(req);
        // 响应后：检查是否有敏感信息泄漏
        if (containsPII(resp.getContent())) {
            resp.setContent(redactPII(resp.getContent()));
        }
        return resp;
    }
}
```

### 3.2 ToolInterceptor — 工具调用包裹

```java
// 包裹工具调用：重试、校验结果
public class ToolRetryInterceptor implements ToolInterceptor {

    @Override
    public ToolResponse interceptTool(ToolRequest req, ToolCallHandler handler) {
        for (int i = 0; i < 3; i++) {
            try {
                return handler.handle(req);
            } catch (Exception e) {
                if (i == 2) throw e;
                Thread.sleep((long) Math.pow(2, i) * 1000);  // 指数退避
            }
        }
        return null;
    }
}
```

---

## 4. 完整执行流程

```text
用户输入
→ Before Agent Hooks (hook1.beforeAgent → hook2.beforeAgent)     [正序]
→ ┌─ Agent 循环开始 ─────────────────────────────┐
│  → Before Model Hooks (hook1.beforeModel → hook2) [正序]     │
│  → Model Interceptors (interceptor1 → interceptor2) [正序]    │
│  → ★ 模型调用 ★                                                  │
│  → After Model Hooks (hook2.afterModel → hook1) [逆序]       │
│  → 工具调用判断：                                                  │
│       └→ Tool Interceptors → ★ 工具执行 ★                       │
│  → 循环继续或结束                                                  │
└────────────────────────────────────────────────┘
→ After Agent Hooks (hook2.afterAgent → hook1.afterAgent)     [逆序]
返回结果
```

**规律：Hook 的 `before*` 正序、`after*` 逆序 — 与 Spring MVC Interceptor 的 `preHandle`/`postHandle` 规则一致。Interceptor 正序嵌套包裹。**

---

## 5. 内置 Hook / Interceptor 清单

| 类型 | 名称 | 功能 |
|------|------|------|
| Hook | `SummarizationHook` | 消息压缩 |
| Hook | `HumanInTheLoopHook` | 人工介入审批 |
| Hook | `ModelCallLimitHook` | 模型调用次数限制 |
| Hook | `PIIDetectionHook` | PII 检测 |
| Interceptor | `ToolRetryInterceptor` | 指数退避重试 |
| Interceptor | `TodoListInterceptor` | 强制规划 |
| Interceptor | `ToolSelectionInterceptor` | LLM 选工具 |
| Interceptor | `ContextEditingInterceptor` | 上下文编辑 |

---

## 6. 框架对比（LangChain vs Spring AI vs Alibaba）

| 框架 | 机制名称 | 层级 | 特点 |
|------|----------|------|------|
| **LangChain 1.x** | AgentMiddleware / Hooks | 单层混合 | Node 式 + Wrap 式，装饰器或类实现 |
| **Spring AI 2.0** | Advisor | 单层 | 栈式链洋葱模型，`CallAdvisor`/`StreamAdvisor` |
| **Alibaba** | Hooks + Interceptors | **双层分离** | Hooks 管流程、Interceptors 管数据 |
| Semantic Kernel | Filters | 单层 | 接口/装饰器式过滤器 |

**共同设计模式：** 洋葱模型（请求正序→处理→响应逆序）+ 顺序控制 + 状态跨链共享。

---

> 🎯 **核心要点**：Spring AI Alibaba 把"流程控制"和"数据修改"拆成两个层 — Hooks 决定"要不要继续"（中断/跳转/审批）、Interceptors 决定"数据和结果"。双层分离的优势：Hook 的异常终止不影响 Interceptor 的数据处理逻辑，两者职责清晰，可独立测试。

**下一模块**：[06-Agent Hooks生产实践与框架对比](06-Agent%20Hooks生产实践与框架对比.md) / **返回总览**：[00-Hook总览](00-Hook知识体系总览.md)
