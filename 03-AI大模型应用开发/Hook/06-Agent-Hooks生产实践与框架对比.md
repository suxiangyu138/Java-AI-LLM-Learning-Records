# 06 - Agent Hooks 生产实践与框架对比

> 🎯 生产级 Agent 的四大标配 Hook + 四框架横向对比 + 选型决策。本章将前面所有 Hook 知识落地为工业级实践方案

---

## 目录

1. [四大标配 Hook + 工业级组合](#1-四大标配-hook--工业级组合)
2. [四大框架横向对比表](#2-四大框架横向对比表)
3. [按团队技术栈选框架](#3-按团队技术栈选框架)
4. [Hook 开发清单与反模式](#4-hook-开发清单与反模式)

---

## 1. 四大标配 Hook + 工业级组合

### 1.1 生产 Agent 必配 Hook

```text
① 安全护栏 Hook — 最优先执行
   ├── before_model：注入安全 Prompt（"不要输出xx"）
   ├── after_model：检测响应敏感内容 → 拦截/脱敏
   └── 工具层：限制工具调用次数（防死循环），校验工具参数

② 成本控制 Hook
   ├── 限制 LLM 调用次数上限
   ├── 限制 Token 总消耗上限（超出→降级或终止）
   └── 统计每次调用的耗时/Token，暴露给监控系统

③ 人工审批 Hook
   ├── 置信度 < 阈值 → 暂停、推送审批
   ├── 敏感操作（删除/支付/发送）→ 强制人工确认
   └── 超时自动降级（审批 30 分钟未响应 → 自动拒绝）

④ 可观测性 Hook
   ├── 每步记录：调用链/耗时/Token/工具调用次数/状态变化
   ├── OpenTelemetry 链路追踪
   └── 异常告警：Hook 自身异常→降级（不阻断主流程）+ 告警
```

### 1.2 Spring AI 工业级 Advisor 组合

```java
ChatClient client = ChatClient.builder(chatModel)
    .defaultAdvisors(
        new SafeGuardAdvisor(),                    // ① 安全（order=1，最优先）
        new TokenLimitAdvisor(maxTokens=100000),    // ② 成本（order=2）
        new HumanApprovalAdvisor(threshold=0.7),    // ③ 人工审批（order=3）
        new MessageChatMemoryAdvisor(memory),       // ④ 记忆注入（order=1000+）
        new QuestionAnswerAdvisor(vectorStore),     // ⑤ RAG（order=1001+）
        new LoggingAdvisor()                        // ⑥ 日志（order=LOWEST）
    ).build();
```

### 1.3 LangChain 工业级 Middleware 组合

```python
agent = create_agent(
    model="claude-sonnet-4-5",
    tools=[...],
    middleware=[
        PIIMiddleware(),                 # ① 敏感信息脱敏
        ToolCallLimitMiddleware(limit=20), # ② 工具调用上限
        ModelCallLimitMiddleware(limit=50),# ③ 模型调用上限
        SummarizationMiddleware(),        # ④ 上下文压缩
        HumanInTheLoopMiddleware(         # ⑤ 敏感操作人工审批
            approval_trigger=lambda s: s.contains_sensitive_operation()
        ),
    ]
)
```

---

## 2. 四大框架横向对比表

| 对比维度 | LangChain Middleware | Spring AI Advisor | Alibaba Hooks+Interceptors | Semantic Kernel Filters |
|----------|---------------------|-------------------|---------------------------|------------------------|
| 语言 | **Python** | **Java** | **Java** | .NET / Java / Python |
| 架构 | 单层（Node+Wrap混合） | 单层（栈式链洋葱模型） | **双层分离**（流程+数据） | 单层（过滤器） |
| 流程控制 | ✅（jump_to 跳转） | ⚠️（需抛异常中断） | ✅（Hook 可中断+跳转） | ⚠️ |
| 数据修改 | ✅（返回 dict 合并） | ✅（request/response 修改） | ✅（Interceptor 包裹） | ✅ |
| 内置 Hook 数 | **10+**（最丰富） | 7+ | 8+ | 5+ |
| 状态共享 | AgentState | AdvisorContext | AgentState | Kernel Data |
| 学习成本 | 中 | **低**（Spring 开发者零成本） | 中 | 中 |
| 生态成熟度 | ⭐⭐⭐ | ⭐⭐（2026 快速成长） | ⭐⭐ | ⭐⭐ |
| 生产案例 | 最多 | 增长中 | 阿里生态内 | 增长中 |

---

## 3. 按团队技术栈选框架

| 团队技术栈 | 推荐框架 | 理由 |
|-----------|----------|------|
| **Python + LangChain** | LangChain Middleware | 生态最成熟、内置最多 |
| **Java + Spring Boot** | **Spring AI Advisor** | 与现有 Spring 体系无缝集成 |
| **Java + 阿里云 + 通义** | Alibaba Hooks+Interceptors | 国产模型深度适配 + 双层架构 |
| **多语言/.NET** | Semantic Kernel Filters | Microsoft 生态 |
| **需要 Webhook 平台集成** | 02-章 Webhook + n8n | 平台层而非框架层 |

---

## 4. Hook 开发清单与反模式

### 4.1 开发清单

| 阶段 | 检查项 |
|------|--------|
| 设计 | Hook 职责单一（安全/成本/日志分开）、执行顺序明确（order） |
| 实现 | 钩子异常用 try-catch 兜底（不崩溃 Agent）、耗时钩子异步化 |
| 配置 | Hook 可动态启用/禁用（Feature Flag）、阈值可配置化 |
| 测试 | 单元测试每个 Hook、集成测试 Hook 组合、性能测试（钩子开销<5%总耗时） |
| 上线 | 灰度发布（先 10% 流量）、Hook 指标监控、异常告警 |

### 4.2 常见反模式

| 反模式 | 问题 | 正确做法 |
|--------|------|----------|
| **钩子里调 LLM** | 递归爆炸、Token 成本翻倍 | Hook 内只用规则/缓存（除非必要，且设置上限） |
| **阻塞式钩子** | 拖慢整个 Agent 响应 | 异步处理（Webhook 模式：快速返回、后台异步） |
| **钩子修改了原始请求** | 后续钩子拿到的是被改过的数据 | modify_request 只改当前请求的副本、不修改传入对象 |
| **钩子顺序混乱** | 日志在安全之前→敏感信息被记录 | 安全→成本→审批→业务→日志 |
| **钩子吞异常** | 静默失败、问题无法发现 | 异常记录+告警+降级（不阻断主流程） |

---

> 🎯 **核心要点**：生产级 Agent Hook 的三大原则 — **① 职责单一**（安全/成本/审批/日志分开）② **洋葱顺序**（安全最先、日志最后、after 逆序）③ **优雅降级**（Hook 异常不崩溃 Agent、成本超限降级而非中断）。四个框架选型核心判断：Python 选 LangChain、Java 选 Spring AI、阿里云选 Alibaba。

**返回总览**：[00-Hook知识体系总览](00-Hook知识体系总览.md)
