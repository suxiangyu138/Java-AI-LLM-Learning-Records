# 08 - LangChain4j Agent 与高级特性

> 🎯 Agent = LLM + Tools + Memory + RAG 的四合一编排。LangChain4j 的 AiServices 让 Agent 开发变成"定义接口 → 注册能力 → 自动编排"。本章覆盖 Agent 模式、自定义 Chain、流式 Tool Calling、生产级 Agent 架构

---

## 目录

1. [Agent 的本质：四个能力的编排](#1-agent-的本质四个能力的编排)
2. [AiServices Agent 完整示例](#2-aiservices-agent-完整示例)
3. [Agent 内部执行循环揭秘](#3-agent-内部执行循环揭秘)
4. [自定义 Chain：显式步骤编排](#4-自定义-chain显式步骤编排)
5. [流式 Agent + Tool 调用](#5-流式-agent--tool-调用)
6. [Spring Boot 集成与生产配置](#6-spring-boot-集成与生产配置)
7. [LangChain4j vs LangChain (Python)](#7-langchain4j-vs-langchain-python)

---

## 1. Agent 的本质：四个能力的编排

```text
Agent = LLM（大脑） + Tools（手脚） + Memory（记忆） + RAG（参考书）

执行循环：
用户提问 → LLM 思考 → 需要工具？→ 调用工具 → 工具结果返回
         → 需要检索？→ 检索知识库 → 检索结果注入
         → 再思考 → 生成最终回复
```

**AiServices 自动编排这个循环** — 开发者只需声明"Agent 有什么能力和用什么模型"，框架处理其余。

---

## 2. AiServices Agent 完整示例

```java
// ===== 第 1 层：定义工具 =====
public class DevTools {
    @Tool("搜索公司内部代码库，返回匹配的文件路径和摘要")
    public String searchCode(@P("搜索关键词") String keyword) {
        return codeSearchService.search(keyword);
    }

    @Tool("查询数据库，仅支持 SELECT 语句")
    public List<Map<String, Object>> queryDB(
        @P("SQL SELECT 语句") String sql) {
        return jdbcTemplate.queryForList(sql);
    }

    @Tool("执行 Python 脚本进行数据分析")
    public String runPython(@P("Python 脚本内容") String script) {
        return pythonExecutor.execute(script);
    }

    @Tool("发送企业微信消息给指定用户")
    public String sendWechatMsg(
        @P("收件人") String to,
        @P("消息内容") String content) {
        wechatApi.send(to, content);
        return "消息已发送";
    }
}

// ===== 第 2 层：定义 Agent 接口 =====
public interface DevAgent {
    @SystemMessage("""
        你是全栈开发助手，可以：
        - 搜索公司代码库（searchCode）
        - 查询数据库（queryDB，仅支持 SELECT）
        - 执行 Python 数据分析（runPython）
        - 发送企微消息通知团队（sendWechatMsg）
        执行复杂任务时，先规划步骤再逐步执行。
        """)
    String execute(String task);

    // 流式版本
    @SystemMessage(fromResource = "prompts/dev-agent.st")
    TokenStream executeStreaming(String task);
}

// ===== 第 3 层：Spring Boot 装配 =====
@Configuration
public class AgentConfig {

    @Bean
    public DevAgent devAgent(ChatLanguageModel model,
                              StreamingChatLanguageModel streamingModel,
                              DevTools devTools,
                              ContentRetriever codeRetriever,
                              ChatMemoryProvider memoryProvider) {
        return AiServices.builder(DevAgent.class)
            .chatLanguageModel(model)
            .streamingChatLanguageModel(streamingModel)
            .tools(devTools)                           // ← 手脚
            .contentRetriever(codeRetriever)           // ← 参考书
            .chatMemoryProvider(memoryProvider)         // ← 记忆
            .build();
    }
}
```

---

## 3. Agent 内部执行循环揭秘

```text
AiServices 动态代理内部的执行流程：

① 拦截 DevAgent.execute("查数据库找到活跃用户，发企微通知")
   ↓
② 构建 ChatRequest：SystemMessage + UserMessage + ToolSpecifications + ChatMemory
   ↓
③ 调用 ChatLanguageModel.generate(request)
   ↓
④ LLM 返回 → 检查是否有 ToolExecutionRequest？
   ├── 有 → ⑤ 反射执行 Tool（如 queryDB("SELECT ...")）
   │         → ⑥ 工具结果拼回 ChatRequest（作为 FunctionMessage）
   │         → ⑦ 再次调用 LLM（goto ③，最多循环 N 次）
   └── 无 → ⑧ 返回 AiMessage（最终回复）
```

**关键源码位置：** `dev.langchain4j.service.AiServiceStreamingResponseHandler` 和 `DefaultToolExecutor`。

---

## 4. 自定义 Chain：显式步骤编排

AiServices 适合"LLM 自主决策"的 Agent 场景。当你需要显式控制步骤顺序时，用 Chain。

```java
// Chain = 步骤序列的显式编码
// 场景：用户输入 → 翻译 → 润色 → 摘要

Chain<String, String> pipeline = SequentialChain.<String, String>builder()
    .addStep(input -> {
        log.info("[Step1] 翻译成英文");
        return model.generate("Translate to English: " + input);
    })
    .addStep(input -> {
        log.info("[Step2] 润色为正式语气");
        return model.generate("Make this more formal: " + input);
    })
    .addStep(input -> {
        log.info("[Step3] 生成摘要");
        return model.generate("Summarize in one sentence: " + input);
    })
    .build();

String result = pipeline.execute("今天天气真好呀，适合出去玩！");
// Step1 → "The weather is great today..."
// Step2 → "The meteorological conditions are..."
// Step3 → "Summary: Favorable weather is reported."

// ★ 与 Agent 的区别：
//   Agent：LLM 自主决定步骤（灵活但不可预测）
//   Chain：开发者显式控制步骤（固定但可预测）
//   生产建议：关键业务流程用 Chain、灵活查询用 Agent
```

---

## 5. 流式 Agent + Tool 调用

```java
// 流式 Agent → 每 token 即时输出，工具调用在后台自动执行

@RestController
public class StreamAgentController {

    private final DevAgent devAgent;

    @PostMapping(value = "/agent/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamAgent(@RequestBody String task) {
        return Flux.create(sink -> {
            TokenStream stream = devAgent.executeStreaming(task);
            stream.onNext(token -> sink.next(
                    ServerSentEvent.<String>builder().data(token).build()))
                  .onToolExecuted(tool -> log.info("Agent 调用了工具: {}", tool.name()))
                  .onComplete(response -> sink.complete())
                  .onError(sink::error)
                  .start();
        });
    }
}

// TokenStream 的事件类型：
// onNext(token)    → LLM 输出的每个 token
// onToolExecuted() → Agent 调用工具时触发（运行时可见）
// onComplete()     → 最终回复完成
// onError()        → 异常处理
```

---

## 6. Spring Boot 集成与生产配置

```yaml
# application.yml — LangChain4j 生产配置
langchain4j:
  open-ai:
    chat-model:
      api-key: ${OPENAI_API_KEY}
      model-name: gpt-5-turbo
      temperature: 0.3            # Agent 场景建议低温度（减少幻象工具调用）
      timeout: 60s
      max-retries: 3
      log-requests: false         # 生产关闭（Token 包含敏感信息）
      log-responses: false
    streaming-chat-model:         # 流式模型独立配置
      api-key: ${OPENAI_API_KEY}
      model-name: gpt-5-turbo
      temperature: 0.3
    embedding-model:
      api-key: ${OPENAI_API_KEY}
      model-name: text-embedding-3-small

logging:
  level:
    dev.langchain4j: INFO         # 生产用 INFO，调试用 DEBUG
```

**生产级 Agent 的配置要点：**
| 配置 | 建议值 | 原因 |
|------|--------|------|
| `temperature` | 0.1–0.3 | 降低幻象工具调用 |
| `timeout` | 60s | Agent 可能有多次 LLM 调用+工具执行 |
| `max-retries` | 3 | 网络抖动自动恢复 |
| `maxMessages`（ChatMemory） | 20 | 控制 Token 消耗 |
| 工具执行超时 | 每个工具独立超时（10-30s） | 防止单个工具卡住整个 Agent |
| 最大工具调用次数 | ≤10 次 | 防止死循环 |

---

## 7. LangChain4j vs LangChain (Python)

| 特性 | LangChain4j | LangChain (Python) |
|------|:---:|:---:|
| 声明式 AiServices | ✅ **核心特色** | ❌ 需手动实现 |
| Agent 编排 | ✅ Chain/Agent | ✅ **更成熟**（LangGraph） |
| RAG | ✅ 完整 | ✅ **更丰富** |
| 文档加载器 | 30+ | **80+** |
| 社区活跃度 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 版本稳定 | ⚠️ beta 迭代 | ✅ 1.x 稳定 |
| Java/Spring 集成 | ✅ **原生** | ❌ |
| 生产案例 | 增长中 | **最多** |
| MCP 支持 | ✅ | ✅ |

> 💡 LangChain4j 不追求 1:1 复刻 Python LangChain。它的 AiServices 声明式接口是 Python LangChain 没有的核心特色 — 利用 Java 的静态类型 + 动态代理实现"接口即服务"。

---

> 🎯 **核心要点**：LangChain4j Agent = **接口定义能力** + **AiServices 自动编排循环**。Agent 与 Chain 的分工：Agent 让 LLM 自主决策步骤（灵活）、Chain 让开发者显式控制步骤（可预测）。生产 Agent 的三个关键配置：**temperature 0.1-0.3（防幻象工具调用）+ maxMessages 20（控制 Token 成本）+ 工具独立超时（防单个工具卡死）**。

**下一模块**：[09-SpringAI vs LangChain4j对比](09-SpringAI-vs-LangChain4j对比.md) / **返回总览**：[00-总览](00-Java-AI开发框架体系总览.md)
