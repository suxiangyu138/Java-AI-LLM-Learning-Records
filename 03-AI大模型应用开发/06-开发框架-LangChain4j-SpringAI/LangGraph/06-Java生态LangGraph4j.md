# 06 - Java 生态：LangGraph4j

> LangGraph 目前以 Python 为主，但 Java 生态正在快速追赶。langgraph4j 提供了接近 Python API 的 Java 实现，配合 Spring AI 和 LangChain4j 可以构建完整的 Java Agent 工作流。

---

## 📚 目录

1. [Java 生态 LangGraph 方案对比](#1-java-生态-langgraph-方案对比)
2. [langgraph4j 核心 API](#2-langgraph4j-核心-api)
3. [构建第一个 Java Agent Graph](#3-构建第一个-java-agent-graph)
4. [Spring Boot 集成实战](#4-spring-boot-集成实战)
5. [混合架构：Java 主控 + Python LangGraph](#5-混合架构java-主控--python-langgraph)
6. [Java 原生 Agent 模式实现](#6-java-原生-agent-模式实现)

---

## 1. Java 生态 LangGraph 方案对比

```text
方案 A：langgraph4j（社区 Java 实现）
  ├── GitHub: bsideup/langgraph4j（实验性）
  ├── API 接近 Python LangGraph
  ├── StateGraph, Node, Edge, Conditional Edge
  ├── 支持 MemorySaver, SqliteSaver
  └── 成熟度：⭐⭐⭐（适合学习和原型）

方案 B：Java + Python LangGraph 混合（推荐生产）
  ├── Java Spring Boot 做主控
  ├── Python LangGraph 做 Agent 编排
  ├── 通信：REST API / gRPC / Redis Pub-Sub
  └── 成熟度：⭐⭐⭐⭐⭐（生产验证）

方案 C：Spring AI + 手动控制流
  ├── 纯 Java，零 Python 依赖
  ├── 在 Java 中手动实现图结构的控制流
  ├── 用 Spring AI 的 ChatClient 做 LLM 调用
  └── 成熟度：⭐⭐⭐⭐（灵活但需手写控制流）

方案 D：LangChain4j AiServices
  ├── 纯 Java
  ├── AiServices 提供部分 Agent 能力
  ├── 但缺少图的循环/分支/持久化
  └── 成熟度：⭐⭐⭐（适合简单 Agent）
```

| 方案 | 语言 | Checkpoint | 复杂控制流 | 生产就绪 | 推荐场景 |
|------|:---:|:---:|:---:|:---:|------|
| langgraph4j | Java | ⚠️ 有限 | ✅ | ❌ | 学习/原型 |
| Java + Python | 混合 | ✅ Postgres | ✅ | ✅ | **生产首选** |
| Spring AI 手工 | Java | 需自建 | ⚠️ 手写 | ✅ | 简单 Agent |
| LangChain4j | Java | ❌ | ❌ | ✅ | 链式 Agent |

---

## 2. langgraph4j 核心 API

### 2.1 Maven 依赖

```xml
<!-- langgraph4j（社区实现，需确认最新版本） -->
<dependency>
    <groupId>com.github.bsideup</groupId>
    <artifactId>langgraph4j</artifactId>
    <version>0.1.0</version>
</dependency>

<!-- 或使用 jlanggraph（另一个社区实现） -->
<dependency>
    <groupId>io.github.kvendingoldo</groupId>
    <artifactId>jlanggraph</artifactId>
    <version>0.0.1</version>
</dependency>
```

> ⚠️ **注意**：langgraph4j 目前处于早期阶段，API 可能变化。生产环境建议用混合架构（方案 B）。

### 2.2 核心类映射

```text
Python LangGraph        →  langgraph4j (Java)
────────────────────────────────────────────────
StateGraph(State)       →  StateGraph<State>.builder()
graph.add_node()        →  builder.addNode()
graph.add_edge()        →  builder.addEdge()
graph.add_conditional   →  builder.addConditionalEdges()
graph.compile()         →  builder.compile()
graph.invoke()          →  graph.invoke()
graph.stream()          →  graph.stream()
interrupt()             →  ⚠️ 尚不支持
Command()               →  ⚠️ 尚不支持
Checkpointer            →  MemorySaver（有限支持）
```

### 2.3 State 定义

```java
// Java 中用 record 或 class 定义 State
// 使用 @StateKey 注解标记字段，指定 Reducer 策略

import java.util.*;
import java.util.function.BinaryOperator;

// 方式 1：使用 Record（Java 17+）
public record AgentState(
    @StateKey(reducer = "append")    // ← operator.add 等效
    List<Message> messages,

    @StateKey                        // ← 默认覆盖模式
    String currentStep,

    @StateKey(reducer = "merge")     // ← 合并字典
    Map<String, Object> toolResults
) {
    // Reducer 方法
    public static List<Message> appendReducer(
            List<Message> old, List<Message> update) {
        List<Message> merged = new ArrayList<>(old);
        merged.addAll(update);
        return merged;
    }

    public static Map<String, Object> mergeReducer(
            Map<String, Object> old, Map<String, Object> update) {
        Map<String, Object> merged = new HashMap<>(old);
        merged.putAll(update);
        return merged;
    }
}

// 方式 2：使用普通类 + Builder
@Data
@Builder
public class AgentState {
    private List<Message> messages;
    private String currentStep;
    private int iterationCount;

    // Reducer：messages 追加
    public static List<Message> appendMessages(
            List<Message> old, List<Message> update) {
        List<Message> result = new ArrayList<>(
            old != null ? old : List.of());
        if (update != null) result.addAll(update);
        return result;
    }
}
```

---

## 3. 构建第一个 Java Agent Graph

### 3.1 ReAct Agent（完整实现）

```java
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel.OpenAiChatModelBuilder;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;

/**
 * 纯 Java 实现 ReAct Agent Loop
 * 不使用任何第三方 Agent 框架
 */
public class JavaReActAgent {

    private final OpenAiChatModel llm;
    private final Map<String, ToolExecutor> tools;
    private final int maxIterations;

    // 工具定义
    public record ToolDef(String name, String description, String parameters) {}

    // 工具执行器
    @FunctionalInterface
    public interface ToolExecutor {
        String execute(String arguments);
    }

    public JavaReActAgent(OpenAiChatModel llm,
                          Map<String, ToolExecutor> tools,
                          int maxIterations) {
        this.llm = llm;
        this.tools = tools;
        this.maxIterations = maxIterations;
    }

    /**
     * 执行 Agent Loop
     */
    public String run(String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage(buildSystemPrompt()));
        messages.add(new UserMessage(userMessage));

        for (int i = 0; i < maxIterations; i++) {
            // 调用 LLM
            var response = llm.chat(messages);
            messages.add(response.aiMessage());

            // 检查是否有工具调用
            if (!response.aiMessage().hasToolCalls()) {
                // 没有工具调用 → 返回最终回答
                return response.aiMessage().text();
            }

            // 执行工具
            for (var toolCall : response.aiMessage().toolCalls()) {
                String toolName = toolCall.name();
                String arguments = toolCall.arguments();

                ToolExecutor executor = tools.get(toolName);
                if (executor == null) {
                    messages.add(new ToolMessage(
                        "Error: unknown tool " + toolName, toolCall.id()));
                    continue;
                }

                String result = executor.execute(arguments);
                messages.add(new ToolMessage(result, toolCall.id()));
            }
        }

        return "达到最大迭代次数限制";
    }

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个智能助理，可以使用以下工具：\n\n");
        for (var entry : tools.entrySet()) {
            sb.append("- ").append(entry.getKey())
              .append("：可用工具\n");
        }
        sb.append("\n如果需要使用工具，请在回答中说明。");
        return sb.toString();
    }
}
```

### 3.2 使用 Spring AI 实现图结构

```java
/**
 * 用 Spring AI + 纯 Java 控制流实现类似 LangGraph 的图结构
 */
@Service
public class SpringAiGraphAgent {

    private final ChatClient chatClient;

    // ===== State =====
    @Data
    @AllArgsConstructor
    public static class GraphState {
        private List<Message> messages;
        private String currentNode;
        private int stepCount;
        private Map<String, Object> data;  // 自定义数据

        public GraphState() {
            this.messages = new ArrayList<>();
            this.data = new HashMap<>();
        }

        public GraphState withMessages(List<Message> newMessages) {
            GraphState next = new GraphState();
            next.messages = new ArrayList<>(this.messages);
            next.messages.addAll(newMessages);
            next.currentNode = this.currentNode;
            next.stepCount = this.stepCount;
            next.data = new HashMap<>(this.data);
            return next;
        }

        public <T> T getData(String key, Class<T> type) {
            return type.cast(data.get(key));
        }

        public void putData(String key, Object value) {
            this.data.put(key, value);
        }
    }

    // ===== Edge =====
    @FunctionalInterface
    public interface EdgeFunction {
        String route(GraphState state);  // 返回下一个 Node 名
    }

    // ===== Node =====
    @FunctionalInterface
    public interface NodeFunction {
        GraphState execute(GraphState state);
    }

    // ===== Graph Builder =====
    public static class GraphBuilder {
        // 静态 Graph 定义
        private final Map<String, NodeFunction> nodes = new LinkedHashMap<>();
        private final Map<String, EdgeFunction> conditionalEdges = new HashMap<>();
        private final Map<String, String> normalEdges = new HashMap<>();
        private String entryNode;

        public GraphBuilder addNode(String name, NodeFunction node) {
            nodes.put(name, node);
            return this;
        }

        public GraphBuilder setEntryPoint(String name) {
            this.entryNode = name;
            return this;
        }

        public GraphBuilder addEdge(String from, String to) {
            normalEdges.put(from, to);
            return this;
        }

        public GraphBuilder addConditionalEdges(
                String from, EdgeFunction router) {
            conditionalEdges.put(from, router);
            return this;
        }

        public GraphState run(GraphState initialState, int maxSteps) {
            GraphState state = initialState;
            String current = entryNode;
            Set<String> visited = new HashSet<>();

            for (int step = 0; step < maxSteps; step++) {
                state.currentNode = current;
                state.stepCount = step;

                // 执行当前节点
                NodeFunction node = nodes.get(current);
                if (node == null) {
                    throw new IllegalStateException("Unknown node: " + current);
                }
                state = node.execute(state);

                // 确定下一个节点
                EdgeFunction cond = conditionalEdges.get(current);
                if (cond != null) {
                    String next = cond.route(state);
                    if (next == null || "END".equals(next)) {
                        return state;
                    }
                    current = next;
                } else {
                    String next = normalEdges.get(current);
                    if (next == null || "END".equals(next)) {
                        return state;
                    }
                    current = next;
                }

                // 防止死循环
                String visitKey = current + ":" + state.stepCount;
                if (!visited.add(visitKey)) {
                    throw new IllegalStateException(
                        "Possible infinite loop at: " + current);
                }
            }
            return state;
        }
    }

    // ===== 使用示例：构建 ReAct Loop =====
    public GraphState runReActAgent(String userInput) {
        GraphBuilder builder = new GraphBuilder();

        // Agent Node：调用 LLM
        builder.addNode("agent", state -> {
            String response = chatClient.prompt()
                .messages(state.getMessages())
                .call()
                .content();
            return state.withMessages(
                List.of(new AssistantMessage(response)));
        });

        // Tool Node：执行工具
        builder.addNode("tools", state -> {
            // 解析最后一个消息，执行工具
            var lastMsg = (AssistantMessage) state.getMessages()
                .get(state.getMessages().size() - 1);
            // ... 工具执行逻辑
            return state;
        });

        // 路由：有没有 Tool Call？
        builder.addConditionalEdges("agent", state -> {
            var last = (AssistantMessage) state.getMessages()
                .get(state.getMessages().size() - 1);
            if (last.getToolCalls() != null && !last.getToolCalls().isEmpty()) {
                return "tools";
            }
            return "END";  // 没有工具调用 → 结束
        });

        builder.addEdge("tools", "agent");  // 工具执行完 → 回到 agent
        builder.setEntryPoint("agent");

        // 初始状态
        GraphState initialState = new GraphState()
            .withMessages(List.of(new UserMessage(userInput)));

        return builder.run(initialState, 10);  // 最多 10 步
    }
}
```

---

## 4. Spring Boot 集成实战

### 4.1 项目结构

```text
src/main/java/com/example/agent/
├── AgentApplication.java          ← Spring Boot 入口
├── config/
│   └── AgentConfig.java           ← LLM + Graph 配置
├── graph/
│   ├── AgentState.java            ← State 定义
│   ├── AgentGraph.java            ← Graph 组装
│   ├── nodes/
│   │   ├── LlmNode.java           ← LLM 调用节点
│   │   ├── ToolNode.java          ← 工具执行节点
│   │   └── SummarizeNode.java     ← 总结节点
│   └── edges/
│       └── AgentRouter.java       ← 路由逻辑
├── tools/
│   ├── WeatherTool.java
│   └── SearchTool.java
├── controller/
│   ├── ChatController.java        ← 对话 API
│   └── StreamController.java      ← 流式 API
├── service/
│   └── AgentService.java          ← 业务编排
└── persistence/
    └── CheckpointRepository.java  ← 检查点持久化
```

### 4.2 核心配置

```java
@Configuration
public class AgentConfig {

    @Bean
    public ChatClient chatClient(
            @Value("${agent.llm.base-url}") String baseUrl,
            @Value("${agent.llm.api-key}") String apiKey,
            @Value("${agent.llm.model}") String model) {

        return ChatClient.builder(
            OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build()
        )
        .defaultOptions(OpenAiChatOptions.builder()
            .model(model)
            .temperature(0.1)  // Agent 场景：低温度确保稳定
            .build())
        .build();
    }

    @Bean
    public AgentGraph agentGraph(ChatClient chatClient,
                                  List<Tool> tools) {
        return AgentGraph.builder()
            .chatClient(chatClient)
            .tools(tools)
            .maxIterations(10)
            .build();
    }
}
```

### 4.3 REST API

```java
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentGraph agentGraph;
    private final CheckpointService checkpointService;

    @PostMapping("/chat")
    public AgentResponse chat(
            @RequestBody ChatRequest request,
            @RequestHeader("X-Thread-Id") String threadId) {

        // 加载之前的 Checkpoint（如果有）
        GraphState state = checkpointService.load(threadId)
            .orElse(new GraphState());

        // 添加用户消息
        state = state.withMessages(
            List.of(new UserMessage(request.getMessage())));

        // 执行图
        GraphState result = agentGraph.run(state);

        // 保存 Checkpoint
        checkpointService.save(threadId, result);

        return AgentResponse.from(result);
    }

    @GetMapping(value = "/stream", produces = TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(
            @RequestParam String message,
            @RequestParam String threadId) {

        return agentGraph.stream(message, threadId)
            .map(event -> ServerSentEvent.<String>builder()
                .data(event.toJson())
                .build());
    }
}
```

### 4.4 Checkpoint 持久化

```java
/**
 * 基于 Redis 的 Checkpoint 服务
 */
@Service
public class CheckpointService {

    private final RedisTemplate<String, GraphState> redis;

    public Optional<GraphState> load(String threadId) {
        GraphState state = redis.opsForValue()
            .get("checkpoint:" + threadId);
        return Optional.ofNullable(state);
    }

    public void save(String threadId, GraphState state) {
        redis.opsForValue().set(
            "checkpoint:" + threadId, state,
            Duration.ofHours(24));  // 24小时 TTL
    }

    public void delete(String threadId) {
        redis.delete("checkpoint:" + threadId);
    }
}
```

---

## 5. 混合架构：Java 主控 + Python LangGraph

### 5.1 架构图

```text
┌─────────────────────────────────────────────────────┐
│                  Java Spring Boot                    │
│                                                     │
│  ┌─────────────┐   ┌──────────────┐                │
│  │ REST API    │   │ Business     │                 │
│  │ Controller  │──▶│ Logic        │                 │
│  └─────────────┘   │ (权限/校验)   │                │
│                    └──────┬───────┘                │
│                           │                         │
│                    ┌──────▼───────┐                │
│                    │ Agent Client  │  HTTP/gRPC     │
│                    │ (调用Python)  │─────────────┐  │
│                    └──────────────┘             │  │
└─────────────────────────────────────────────────┼──┘
                                                  │
┌─────────────────────────────────────────────────┼──┐
│                Python LangGraph Server          │  │
│                                                 │  │
│  ┌──────────────────┐   ┌──────────────────┐   │  │
│  │ FastAPI Endpoint │──▶│ LangGraph App    │   │  │
│  │ /agent/invoke    │   │ (StateGraph)     │◀──┘  │
│  │ /agent/stream    │   └──────────────────┘      │
│  └──────────────────┘                             │
│           │                                       │
│  ┌────────▼──────────┐                            │
│  │ PostgresSaver     │  ← Checkpoints             │
│  └───────────────────┘                            │
└───────────────────────────────────────────────────┘
```

### 5.2 Java 端 Agent Client

```java
@Service
public class LangGraphClient {

    private final WebClient webClient;

    public LangGraphClient(
            @Value("${langgraph.server.url}") String serverUrl) {
        this.webClient = WebClient.builder()
            .baseUrl(serverUrl)
            .build();
    }

    /**
     * 同步调用 Agent
     */
    public GraphResult invoke(String threadId, String message) {
        return webClient.post()
            .uri("/agent/invoke")
            .bodyValue(Map.of(
                "thread_id", threadId,
                "message", message
            ))
            .retrieve()
            .bodyToMono(GraphResult.class)
            .block();
    }

    /**
     * 流式调用 Agent
     */
    public Flux<String> stream(String threadId, String message) {
        return webClient.post()
            .uri("/agent/stream")
            .bodyValue(Map.of(
                "thread_id", threadId,
                "message", message
            ))
            .accept(MediaType.TEXT_EVENT_STREAM)
            .retrieve()
            .bodyToFlux(String.class)
            .filter(line -> line.startsWith("data: "))
            .map(line -> line.substring(6));
    }

    /**
     * 人工审批：恢复暂停的 Graph
     */
    public GraphResult resume(String threadId, Map<String, Object> approval) {
        return webClient.post()
            .uri("/agent/resume")
            .bodyValue(Map.of(
                "thread_id", threadId,
                "approval", approval
            ))
            .retrieve()
            .bodyToMono(GraphResult.class)
            .block();
    }
}
```

### 5.3 Python 端 Server

```python
# langgraph_server.py
# 这个文件部署在 Python 环境中

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from langgraph.checkpoint.postgres import PostgresSaver
import uvicorn

app = FastAPI()

# 加载 Graph + Checkpointer
checkpointer = PostgresSaver.from_conn_string(DATABASE_URL)
graph = build_agent_graph().compile(checkpointer=checkpointer)

class InvokeRequest(BaseModel):
    thread_id: str
    message: str

class ResumeRequest(BaseModel):
    thread_id: str
    approval: dict

@app.post("/agent/invoke")
async def invoke(req: InvokeRequest):
    config = {"configurable": {"thread_id": req.thread_id}}
    result = await graph.ainvoke(
        {"messages": [HumanMessage(content=req.message)]},
        config
    )
    return {
        "messages": [msg_to_dict(m) for m in result["messages"]],
        "status": "completed"
    }

@app.post("/agent/stream")
async def stream(req: InvokeRequest):
    """SSE 流式端点"""
    from fastapi.responses import StreamingResponse
    import json

    async def generate():
        config = {"configurable": {"thread_id": req.thread_id}}
        async for mode, event in graph.astream(
            {"messages": [HumanMessage(content=req.message)]},
            config,
            stream_mode=["messages", "updates"]
        ):
            yield f"data: {json.dumps({'mode': mode, 'event': str(event)})}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(generate(), media_type="text/event-stream")

@app.post("/agent/resume")
async def resume(req: ResumeRequest):
    from langgraph.types import Command
    config = {"configurable": {"thread_id": req.thread_id}}
    result = await graph.ainvoke(
        Command(resume=req.approval),
        config
    )
    return {"status": "completed"}

if __name__ == "__main__":
    uvicorn.run(app, port=8100)
```

---

## 6. Java 原生 Agent 模式实现

### 6.1 简单的状态机 Agent

```java
/**
 * 当不需要 LangGraph 的全部复杂性时，
 * 用枚举状态机实现轻量 Agent
 */
public class StateMachineAgent {

    private enum State {
        INIT, THINKING, ACTING, OBSERVING, DONE, ERROR
    }

    private final ChatClient llm;
    private final Map<String, Function<String, String>> tools;

    public String run(String input) {
        State state = State.INIT;
        List<Message> messages = new ArrayList<>();
        messages.add(new UserMessage(input));

        int maxSteps = 10;
        for (int step = 0; step < maxSteps; step++) {
            switch (state) {
                case INIT -> {
                    messages.add(0, new SystemMessage(
                        "你是有工具调用能力的助手..."));
                    state = State.THINKING;
                }

                case THINKING -> {
                    String response = llm.prompt()
                        .messages(messages)
                        .call().content();
                    messages.add(new AssistantMessage(response));

                    // 判断是否需要行动
                    if (needsAction(response)) {
                        state = State.ACTING;
                    } else {
                        state = State.DONE;
                    }
                }

                case ACTING -> {
                    String action = extractAction(messages);
                    String result = executeAction(action);
                    messages.add(new ToolMessage(result, "tool-1"));
                    state = State.OBSERVING;
                }

                case OBSERVING -> {
                    // 观察结果，决定继续或结束
                    state = State.THINKING;  // 回到思考
                }

                case DONE -> {
                    return messages.get(messages.size() - 1).text();
                }

                case ERROR -> {
                    return "执行出错";
                }
            }
        }
        return "达到最大步骤限制";
    }

    private boolean needsAction(String response) {
        return response.contains("ACTION:") || response.contains("TOOL:");
    }

    private String executeAction(String action) {
        // 解析 + 执行工具
        return "执行结果";
    }

    private String extractAction(List<Message> messages) {
        return messages.get(messages.size() - 1).text();
    }
}
```

---

> 🎯 **核心要点**：Java 生态中 LangGraph 的最优解是**混合架构**——Java 做业务逻辑、权限、API，Python 做 Agent 编排。langgraph4j 适合原型和学习。对于简单 Agent，Spring AI + 手动状态机完全可以胜任，不需要引入 Python。

---

**下一模块**：[07 - LangGraph Platform 与生产部署](./07-LangGraph-Platform与生产部署.md)  
**返回总览**：[00 - LangGraph 知识体系总览](./00-LangGraph知识体系总览.md)
