# 06 — Agent 智能体

> **目标**：理解 Agent 的架构原理、掌握主流框架、学会设计企业级 Agent 系统。

---

## 1. Agent 是什么

### 1.1 定义

> **Agent（智能体）** = LLM（大脑）+ 规划（Planning）+ 工具（Tools）+ 记忆（Memory）。它能自主分解任务、调用工具、多步执行，完成"一次性问答"无法解决的复杂任务。

```
普通 LLM 调用：
  User Question → LLM → Answer
  (一问一答，无外部交互)

Agent 调用：
  User Task → [Plan] → [Tool Call] → [Observe] → [Re-plan] → ... → Final Answer
  (多步自主循环，有外部交互)
```

### 1.2 Agent 能力矩阵

| 能力 | 描述 | 是否需要 Agent |
|------|------|:---:|
| 简单问答 | "什么是RAG？" | ❌ |
| 带检索的问答 | "根据文档库回答..." | ❌ (RAG 就够了) |
| 多步数据查询 | "查数据库 → 分析趋势 → 画图表 → 写报告" | ✅ |
| 自主决策 | "帮我订一张性价比最高的机票" | ✅ |
| 工具编排 | "先搜索，再对比，最后发邮件" | ✅ |
| 自主纠错 | "写代码 → 运行 → 发现错误 → 修复" | ✅ |

---

## 2. Agent 核心架构

### 2.1 四要素

```
┌───────────────────────────────────────────────────────┐
│                       Agent                             │
│                                                         │
│   ┌──────────┐   ┌──────────┐   ┌──────────────────┐  │
│   │  LLM     │   │ Planning │   │ Memory           │  │
│   │ (推理引擎)│   │ (规划器)  │   │ (记忆系统)        │  │
│   │          │   │          │   │                   │  │
│   │ - 理解   │   │ - 任务分解│   │ - Working Memory │  │
│   │ - 推理   │   │ - 路径规划│   │ - Episodic Memory│  │
│   │ - 决策   │   │ - 动态调整│   │ - Semantic Memory│  │
│   └────┬─────┘   └────┬─────┘   └────────┬─────────┘  │
│        │              │                   │              │
│        └──────────────┼───────────────────┘              │
│                       │                                  │
│               ┌───────┴────────┐                         │
│               │   Tool Use     │                         │
│               │  (工具执行层)   │                         │
│               └───────┬────────┘                         │
└───────────────────────┼──────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
   ┌─────────┐    ┌──────────┐    ┌─────────┐
   │ Search  │    │ Database │    │ Code    │
   │ Engine  │    │ Query    │    │ Executor│
   └─────────┘    └──────────┘    └─────────┘
```

### 2.2 Agent 循环

```
基础 Agent 循环 (ReAct 变体)：

while (not finished) {
    1. Think  (LLM 推理): "我现在应该做什么？"
    2. Act    (执行动作): 调用工具 / 返回结果
    3. Observe(观察结果): 获取工具返回的结果
    4. Reflect(反思):     "结果够好吗？需要调整吗？"
}
```

---

## 3. Agent 架构模式

### 3.1 四种核心模式

```
模式 1：ReAct (Reasoning + Acting)
  交替进行推理和行动
  Thought₁ → Action₁ → Observation₁ → Thought₂ → Action₂ → ... → Answer
  最常用，效果稳定

模式 2：Plan-and-Execute
  先制定完整计划 → 逐步执行 → 执行中可调整
  Plan → Step1 → Step2 → Step3 → Review → Final
  适合多步、可预见的任务

模式 3：Reflection
  Do → Self-Critic → Refine → Redo
  自主检查和改进输出
  适合代码生成、写作

模式 4：Multi-Agent
  多个 Agent 各司其职、相互协作
  Agent_A (研究员) + Agent_B (分析师) + Agent_C (写手)
  适合复杂、多维度任务
```

### 3.2 ReAct 实现示例

```java
@Service
public class ReActAgent {

    private final ChatClient chatClient;
    private final ToolRegistry toolRegistry;
    private static final int MAX_ITERATIONS = 10;

    public String execute(String task) {
        List<Message> history = new ArrayList<>();
        history.add(new SystemMessage("""
            你是一个 ReAct Agent。你可以使用工具完成任务。
            请按以下格式回复：

            Thought: 分析当前状态，思考下一步
            Action: tool_name(param1=value1, param2=value2)
            Observation: (工具返回后自动填充)

            当任务完成时：
            Thought: 任务已完成
            Final Answer: 最终答案
            """));

        history.add(new UserMessage(task));

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            // 1. LLM 推理
            String response = chatClient.prompt()
                .messages(history)
                .call()
                .content();

            history.add(new AssistantMessage(response));

            // 2. 解析 Action
            Action action = parseAction(response);
            if (action == null) {
                // 没有 Action → 检查是否有 Final Answer
                String finalAnswer = parseFinalAnswer(response);
                if (finalAnswer != null) {
                    return finalAnswer;
                }
                continue;
            }

            // 3. 执行 Tool
            String observation = toolRegistry.execute(action.name, action.params);
            history.add(new UserMessage("Observation: " + observation));
        }

        return "达到最大迭代次数，任务未完成";
    }

    record Action(String name, Map<String, String> params) {}

    private Action parseAction(String response) {
        // 解析 "Action: tool_name(param1=val1)"
        Pattern p = Pattern.compile("Action:\\s*(\\w+)\\((.+)\\)");
        Matcher m = p.matcher(response);
        if (m.find()) {
            return new Action(m.group(1), parseParams(m.group(2)));
        }
        return null;
    }
}
```

---

## 4. 规划 (Planning)

### 4.1 任务分解策略

```java
@Service
public class TaskPlanner {

    private final ChatClient chatClient;

    /**
     * 将复杂任务分解为子任务
     */
    public Plan decomposeTask(String complexTask) {
        String result = chatClient.prompt()
            .system("""
                你是一个任务规划专家。请将复杂任务拆解为可执行的子任务。
                每个子任务需包含：
                - id: 序号
                - description: 任务描述
                - tool: 需要的工具（可选）
                - depends_on: 依赖的子任务 ID（可选）
                - expected_output: 期望产出

                返回 JSON 格式。
                """)
            .user(complexTask)
            .call()
            .entity(Plan.class);

        return result;
    }

    /**
     * 动态规划：根据上一步结果调整后续步骤
     */
    public Plan replan(Plan originalPlan, int currentStep,
                        String observation) {
        String result = chatClient.prompt()
            .system("""
                你是动态规划专家。根据当前执行结果，调整后续计划。
                原计划：%s
                当前步骤：%d
                执行结果：%s

                返回调整后的后续计划（JSON）。
                """.formatted(toJson(originalPlan), currentStep, observation))
            .user("请调整")
            .call()
            .entity(Plan.class);

        return result;
    }
}

@Data
class Plan {
    private List<SubTask> tasks;
    private String overallGoal;
    private int estimatedSteps;
}

@Data
class SubTask {
    private int id;
    private String description;
    private String tool;
    private List<Integer> dependsOn;
    private String expectedOutput;
}
```

---

## 5. 记忆系统 (Memory)

### 5.1 三种记忆

```java
/**
 * Agent 记忆系统三层架构
 */
public class AgentMemorySystem {

    // ──── 1. Working Memory（工作记忆）────
    // 当前任务的中间状态，最活跃
    private final List<Message> conversationHistory;
    private final Map<String, Object> taskContext;
    private final Deque<String> actionHistory;

    // ──── 2. Episodic Memory（情景记忆）────
    // 过往任务的经验/案例，用于检索相似场景
    private final VectorStore episodicStore;

    // ──── 3. Semantic Memory（语义记忆）────
    // 用户偏好、知识、事实，长期积累
    private final Map<String, String> userPreferences;
    private final VectorStore knowledgeBase;

    /**
     * 检索相似历史经验
     */
    public List<String> recallSimilarEpisodes(String currentTask, int topK) {
        return episodicStore.similaritySearch(
            SearchRequest.query(currentTask).withTopK(topK)
        ).stream()
         .map(Document::getContent)
         .collect(Collectors.toList());
    }

    /**
     * 记录成功经验
     */
    public void rememberEpisode(String task, String plan, String result) {
        String episode = String.format("""
            任务: %s
            执行计划: %s
            结果: %s
            经验教训: (后续由 LLM 总结)
            """, task, plan, result);
        episodicStore.add(List.of(new Document(episode)));
    }
}
```

### 5.2 记忆管理策略

| 策略 | 做法 | 适用场景 |
|------|------|---------|
| **滑动窗口** | 只保留最近 N 轮对话 | 短对话 |
| **摘要压缩** | 对话过长时，LLM 摘要前半部分 | 长对话 |
| **向量检索** | 从长期记忆检索相关历史 | 跨会话、复杂 Agent |
| **实体记忆** | 提取并持久化关键实体信息 | 用户画像、知识积累 |
| **层次记忆** | 按重要性分级存储 | 大规模记忆系统 |

---

## 6. 工具管理 (Tool Management)

### 6.1 企业级 Tool Registry

```java
@Service
public class ToolRegistry {

    private final Map<String, ToolDefinition> tools = new ConcurrentHashMap<>();

    /**
     * 注册工具
     */
    public void register(String name, String description,
                          Map<String, ParameterDef> params,
                          Function<Map<String, Object>, String> executor) {
        tools.put(name, new ToolDefinition(name, description, params, executor));
    }

    /**
     * 生成 OpenAI Function Calling 格式的工具列表
     */
    public List<Map<String, Object>> toOpenAIFunctions() {
        return tools.values().stream()
            .map(tool -> {
                Map<String, Object> func = new LinkedHashMap<>();
                func.put("name", tool.name);
                func.put("description", tool.description);
                func.put("parameters", buildJsonSchema(tool.params));
                return Map.of("type", "function", "function", func);
            })
            .collect(Collectors.toList());
    }

    /**
     * 执行工具（含超时和异常处理）
     */
    public String execute(String name, Map<String, Object> params) {
        ToolDefinition tool = tools.get(name);
        if (tool == null) {
            return "Error: Tool not found: " + name;
        }
        try {
            return CompletableFuture
                .supplyAsync(() -> tool.executor.apply(params))
                .get(30, TimeUnit.SECONDS);  // 30 秒超时
        } catch (TimeoutException e) {
            return "Error: Tool execution timeout";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Data
    @AllArgsConstructor
    static class ToolDefinition {
        private String name;
        private String description;
        private Map<String, ParameterDef> params;
        private Function<Map<String, Object>, String> executor;
    }

    record ParameterDef(String type, String description, boolean required) {}
}
```

---

## 7. 多 Agent 协作

### 7.1 协作模式

```
模式 1：顺序接力
  Agent₁ → Agent₂ → Agent₃ → Output
  适合：流水线任务

模式 2：分工协作
       ┌── Agent₁ (研究员) ──┐
  Task ─┼── Agent₂ (分析师) ──┼── Aggregator → Output
       └── Agent₃ (写手)   ──┘
  适合：需要多角度分析的任务

模式 3：辩论
  Agent_A (Pro) ──┐
                  ├── Judge → 最优方案
  Agent_B (Con) ──┘
  适合：决策类任务

模式 4：层级
          Manager Agent
         /      |      \
     Agent₁  Agent₂  Agent₃
  适合：大型复杂项目
```

### 7.2 Java 实现多 Agent

```java
@Service
public class MultiAgentOrchestrator {

    // Manager Agent: 负责任务分配
    private final ChatClient managerAgent;

    // Worker Agents: 各司其职
    private final ReActAgent researcherAgent;
    private final ReActAgent analystAgent;
    private final ReActAgent writerAgent;

    public String execute(String complexTask) {
        // Phase 1: Manager 分解任务
        String plan = chatClient.prompt()
            .system("""
                你是项目经理。将以下任务分配给合适的成员：
                - Researcher: 负责搜索和收集信息
                - Analyst: 负责数据分析和推理
                - Writer: 负责撰写最终报告

                以 JSON 返回分配结果：
                {"researcher": "搜索任务描述", "analyst": "分析任务描述", "writer": "写作任务描述"}
                """)
            .user(complexTask)
            .call()
            .content();

        Assignment assignment = parseAssignment(plan);

        // Phase 2: 各 Agent 并行工作
        CompletableFuture<String> researchFuture = CompletableFuture
            .supplyAsync(() -> researcherAgent.execute(assignment.researcherTask));

        // 研究员完成后再启动分析师（依赖关系）
        CompletableFuture<String> analysisFuture = researchFuture
            .thenCompose(researchResult ->
                CompletableFuture.supplyAsync(() ->
                    analystAgent.execute(
                        assignment.analystTask + "\n\n研究结果:\n" + researchResult
                    )
                )
            );

        // 分析师完成后再启动写手
        CompletableFuture<String> reportFuture = analysisFuture
            .thenCompose(analysisResult ->
                CompletableFuture.supplyAsync(() ->
                    writerAgent.execute(
                        assignment.writerTask + "\n\n分析结果:\n" + analysisResult
                    )
                )
            );

        return reportFuture.join();
    }
}
```

---

## 8. Agent 框架选型

### 8.1 主流框架对比

| 框架 | 语言 | 特点 | 适用于 |
|------|------|------|--------|
| **LangGraph** | Python/JS | 有状态图、循环、分支、人工介入 | 复杂 Agent 流水线 |
| **CrewAI** | Python | 角色扮演式多 Agent | 多角色协作 |
| **AutoGen** | Python | 微软出品，对话驱动 | 对话式多 Agent |
| **MetaGPT** | Python | 模拟软件公司 SOP | 代码生成 Agent |
| **LangChain4j Agent** | Java | Java 生态最完善 | Java 后端集成 |
| **Spring AI Agent** | Java | Spring 原生，工具链完整 | Spring Boot 项目 |
| **Semantic Kernel** | .NET/Java/Python | 微软出品，企业级 | 微软技术栈 |
| **Dify** | 低代码 | 拖拽式工作流 | 快速原型 |

### 8.2 Java Agent 选型建议

```
企业 Java 项目 Agent 选型：

小项目 / POC：
  └── Spring AI + Tool Annotations (零额外依赖)

中型项目：
  └── LangChain4j Agent (功能全面，中文社区好)

大型项目 / 复杂流程：
  └── LangGraph (Python) 通过 HTTP/gRPC 调用
      或 Spring AI + 自研编排引擎
```

---

## 9. Agent 安全与治理

### 9.1 安全边界

```java
@Component
public class AgentGuard {

    private final ChatClient chatClient;

    /**
     * 工具调用前：确认操作是否安全
     */
    public boolean approveAction(String toolName, Map<String, Object> params) {
        // 1. 高风险操作名单
        Set<String> destructiveTools = Set.of("delete_file", "drop_table", "send_email");
        if (destructiveTools.contains(toolName)) {
            // 需要人工审批
            return requestHumanApproval(toolName, params);
        }

        // 2. LLM 安全判断
        String judgment = chatClient.prompt()
            .system("判断以下 Agent 操作是否安全。只回复 SAFE 或 UNSAFE:reason")
            .user("Tool: %s, Params: %s".formatted(toolName, params))
            .call()
            .content();

        return judgment.startsWith("SAFE");
    }

    /**
     * Human-in-the-Loop: 高风险操作需人工确认
     */
    private boolean requestHumanApproval(String toolName, Map<String, Object> params) {
        // 发送审批请求（企业微信/钉钉/飞书/MQ）
        // 等待审批结果
        return false; // 简化实现
    }
}
```

### 9.2 治理规则

```
Agent 治理检查清单：
├── 最大迭代次数限制 (如 MAX_ITERATIONS=20)
├── 单次任务 Token 预算 (如 MAX_TOKENS=100K)
├── 单次任务时间限制 (如 TIMEOUT=5min)
├── 高风险操作审批机制
├── 操作审计日志 (谁/什么时候/调了什么工具/结果)
├── 降级策略 (Agent 失败时的 Fallback)
└── 用户可见度 (让用户知道 Agent 在做什么)
```

---

## 10. 快速复习

```
□ Agent 四要素：LLM + Planning + Tools + Memory
□ 四种架构模式：ReAct / Plan-Execute / Reflection / Multi-Agent
□ ReAct 循环：Think → Act → Observe → Reflect
□ 记忆三层：Working / Episodic / Semantic
□ 任务分解与动态重规划
□ Tool Registry 的企业级设计（超时/异常/审批）
□ 多 Agent 协作模式：顺序/分工/辩论/层级
□ LangGraph / CrewAI / AutoGen 各自定位
□ Agent 安全：Human-in-the-Loop / 操作审计 / 降级策略
```

---

> **下一步**：[07 — Function Calling 与 Tool Use](./07-Function-Calling与Tool-Use.md)
