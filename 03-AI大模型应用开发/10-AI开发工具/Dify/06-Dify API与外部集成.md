# Dify API 与外部集成

> 🔌 RESTful API、Webhook 触发、前端嵌入（iframe/WebComponent）、Java Spring Boot 集成 —— 把 Dify 的 AI 能力嵌入你的应用生态

---

## 📚 目录

1. [Dify API 概述](#1-dify-api-概述)
2. [聊天 API 完整参考](#2-聊天-api-完整参考)
3. [工作流 API 与 Webhook](#3-工作流-api-与-webhook)
4. [前端嵌入方案](#4-前端嵌入方案)
5. [Java Spring Boot 集成实战](#5-java-spring-boot-集成实战)

---

## 1. Dify API 概述

### 1.1 两类 API

```text
Dify 提供两类 API：

1. Service API (应用级)
   ├── 路径：/v1/chat-messages (聊天)
   │        /v1/workflows/run (工作流)
   │        /v1/completion-messages (文本生成)
   ├── 认证：Bearer Token (应用 → API 访问 → API 密钥)
   ├── 用途：外部系统调用 Dify 应用
   └── 场景：Java/Python/前端 调用 AI 能力

2. Console API (管理级)
   ├── 路径：/console/api/
   ├── 认证：Session Cookie (管理员)
   ├── 用途：管理 Dify 平台（创建应用、管理知识库...）
   └── 场景：管理后台 / CI/CD 自动化
```

### 1.2 获取 API 密钥

```text
应用 → 发布 → 访问 API

1. 点击"API 密钥" → 创建密钥
2. 复制密钥（只显示一次！）
3. 保存：sk-xxxxxxxxxxxxxxxxxxxx

API Base URL：
├── Docker 部署：http://localhost/v1
├── 自定义域名：https://dify.yourdomain.com/v1
└── Dify Cloud：https://api.dify.ai/v1
```

### 1.3 通用请求头

```http
Authorization: Bearer {api_key}
Content-Type: application/json
```

---

## 2. 聊天 API 完整参考

### 2.1 发送对话消息

```http
POST /v1/chat-messages
Content-Type: application/json
Authorization: Bearer app-xxxxxxxxxxxx

{
  "inputs": {
    "user_name": "张三",
    "user_level": "vip"
  },
  "query": "我的订单什么时候发货？",
  "response_mode": "streaming",
  "conversation_id": "",
  "user": "user-12345",
  "files": [
    {
      "type": "image",
      "transfer_method": "remote_url",
      "url": "https://example.com/screenshot.png"
    }
  ]
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `inputs` | object | ✅ | 传入应用定义的变量 |
| `query` | string | ✅ | 用户的问题 |
| `response_mode` | string | ✅ | `streaming` (SSE) 或 `blocking` |
| `conversation_id` | string | ❌ | 对话 ID（新对话留空） |
| `user` | string | ✅ | 用户标识（用于区分不同用户） |
| `files` | array | ❌ | 上传的文件列表 |

### 2.2 流式响应 (SSE)

```text
response_mode: "streaming"

服务端通过 Server-Sent Events 逐 token 推送：

event: message
data: {"event": "message", "id": "msg_xxx", "answer": "您", "created_at": 1722...}

event: message
data: {"event": "message", "id": "msg_xxx", "answer": "的订单", "created_at": 1722...}

event: message
data: {"event": "message", "id": "msg_xxx", "answer": "已发货", "created_at": 1722...}

event: message_end
data: {"event": "message_end", "id": "msg_xxx", "conversation_id": "conv_xxx",
       "metadata": {"usage": {"prompt_tokens": 120, "completion_tokens": 45}}}
```

事件类型说明：

| 事件 | 说明 |
|------|------|
| `message` | 逐 token 推送 LLM 生成的文本 |
| `message_end` | 消息完成，含 Token 用量和 conversation_id |
| `agent_message` | Agent 的思考过程（仅 Agent 应用） |
| `agent_thought` | Agent 的推理步骤 |
| `message_file` | 文件下载链接 |
| `error` | 错误信息 |
| `ping` | 心跳保活 |

### 2.3 阻塞式响应 (Blocking)

```json
// 请求同上，response_mode: "blocking"

// 响应：
{
  "event": "message",
  "id": "msg_xxx",
  "conversation_id": "conv_xxx",
  "mode": "chat",
  "answer": "您好张三！您的订单 #20260730 已于7月28日发货...",
  "metadata": {
    "usage": {
      "prompt_tokens": 120,
      "completion_tokens": 85,
      "total_tokens": 205
    },
    "retriever_resources": [
      {
        "dataset_name": "产品帮助文档",
        "document_name": "订单与物流.md",
        "content": "发货后3-5个工作日内送达..."
      }
    ]
  },
  "created_at": 1722345678
}
```

### 2.4 获取对话列表 & 历史

```http
# 获取用户的对话列表
GET /v1/conversations?user=user-12345&limit=20
Authorization: Bearer app-xxxxxxxxxxxx

# 响应
{
  "data": [
    {
      "id": "conv_xxx",
      "name": "订单查询",
      "inputs": {},
      "created_at": 1722345600
    }
  ],
  "has_more": false
}

# 获取特定对话的消息历史
GET /v1/messages?conversation_id=conv_xxx&user=user-12345
Authorization: Bearer app-xxxxxxxxxxxx
```

### 2.5 文本生成 API

```http
# 适用于"文本生成"类型应用（无对话历史，单次输入输出）
POST /v1/completion-messages
Authorization: Bearer app-xxxxxxxxxxxx
Content-Type: application/json

{
  "inputs": {
    "language": "中文",
    "tone": "专业"
  },
  "query": "请将以下英文翻译成中文：...",
  "response_mode": "streaming",
  "user": "user-12345"
}
```

---

## 3. 工作流 API 与 Webhook

### 3.1 工作流执行 API

```http
POST /v1/workflows/run
Authorization: Bearer app-xxxxxxxxxxxx
Content-Type: application/json

{
  "inputs": {
    "order_id": "20260730001",
    "user_name": "张三"
  },
  "response_mode": "streaming",
  "user": "user-12345"
}
```

```json
// 阻塞式响应的 data.outputs 包含工作流结束节点的输出
{
  "data": {
    "id": "wf_run_xxx",
    "workflow_id": "wf_xxx",
    "status": "succeeded",
    "outputs": {
      "summary": "订单分析完成...",
      "risk_level": "low",
      "recommendation": "建议优先处理"
    },
    "error": null
  }
}
```

### 3.2 Webhook 触发

```text
Webhook = HTTP 回调 → 触发工作流执行

场景：
├── Git 提交 → 触发代码审查 Agent
├── 订单创建 → 触发 AI 风险评估
├── 表单提交 → 触发智能分类与回复
├── 定时 Cron → 触发数据摘要生成
└── CI/CD 管道 → 触发构建日志分析

配置：
1. 工作流中勾选"允许 Webhook 触发"
2. 获取 Webhook URL
3. 外部系统 POST 请求到 Webhook URL
```

```http
POST /v1/workflows/{workflow_id}/webhook
Authorization: Bearer app-xxxxxxxxxxxx
Content-Type: application/json

{
  "inputs": {
    "event_type": "order_created",
    "order_data": {...}
  },
  "user": "system-automation"
}
```

### 3.3 Webhook + 代码审查场景

```yaml
# GitHub Action 中触发 Dify Webhook
name: AI Code Review Trigger
on:
  pull_request:
    types: [opened, synchronize]

jobs:
  ai-review:
    runs-on: ubuntu-latest
    steps:
      - name: Get PR diff
        run: |
          git diff origin/main > pr.diff

      - name: Trigger Dify Workflow
        run: |
          DIFF_CONTENT=$(cat pr.diff | jq -sR .)
          curl -X POST https://dify.yourdomain.com/v1/workflows/code-review/webhook \
            -H "Authorization: Bearer ${{ secrets.DIFY_API_KEY }}" \
            -H "Content-Type: application/json" \
            -d "{
              \"inputs\": {
                \"pr_diff\": $DIFF_CONTENT,
                \"pr_title\": \"${{ github.event.pull_request.title }}\"
              },
              \"user\": \"github-actions\"
            }" | jq '.data.outputs.review_result' >> comment.md

      - name: Post AI review comment
        uses: actions/github-script@v7
        with:
          script: |
            const fs = require('fs');
            const body = fs.readFileSync('comment.md', 'utf8');
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: body
            });
```

---

## 4. 前端嵌入方案

### 4.1 三种嵌入方式

| 方式 | 适用场景 | 自定义程度 | 技术栈 |
|------|---------|:---:|------|
| **iframe** | 快速集成 | 低 | 纯 HTML |
| **Web Component** | 现代前端项目 | 中 | React/Vue/Angular |
| **API + 自定义 UI** | 需要品牌定制 | 高 | 任意前端框架 |

### 4.2 iframe 嵌入

```html
<!-- Dify 应用 → 发布 → 嵌入 → 复制代码 -->
<script>
window.difyChatbotConfig = {
  token: 'YOUR_APP_TOKEN',
  baseUrl: 'https://dify.yourdomain.com'
}
</script>
<script src="https://dify.yourdomain.com/embed.min.js"
        id="YOUR_APP_TOKEN" defer>
</script>

<!-- 自定义样式：通过 data-icon 等属性 -->
<style>
  #dify-chatbot-bubble-button {
    background-color: #1C64F2 !important;
  }
</style>
```

### 4.3 通过 API 自建 UI（推荐自由度高）

```javascript
// 自己用 fetch/axios 调用 API，构建自定义对话界面

async function sendMessage(query, conversationId = '') {
  const response = await fetch('https://dify.yourdomain.com/v1/chat-messages', {
    method: 'POST',
    headers: {
      'Authorization': 'Bearer YOUR_API_KEY',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      inputs: {},
      query: query,
      response_mode: 'streaming',
      conversation_id: conversationId,
      user: currentUser.id
    })
  });

  // 处理 SSE 流
  const reader = response.body.getReader();
  const decoder = new TextDecoder();

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    const chunk = decoder.decode(value);
    const lines = chunk.split('\n');

    for (const line of lines) {
      if (line.startsWith('data: ')) {
        const data = JSON.parse(line.slice(6));
        if (data.event === 'message') {
          // 逐字渲染到界面上
          appendToChatBubble(data.answer);
        } else if (data.event === 'message_end') {
          // 保存 conversation_id 供下一轮使用
          currentConversationId = data.conversation_id;
        }
      }
    }
  }
}
```

---

## 5. Java Spring Boot 集成实战

### 5.1 DifyClient 封装

```java
// DifyClient.java —— Dify API 的 Java 客户端封装

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSources;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Component
public class DifyClient {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;

    public DifyClient(DifyProperties properties) {
        this.baseUrl = properties.getBaseUrl();
        this.apiKey = properties.getApiKey();
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 流式聊天 —— 通过 Consumer 回调逐字推送
     */
    public void chatStream(String query, String userId,
                           Consumer<String> onMessage,
                           Consumer<Throwable> onError) {
        Map<String, Object> body = new HashMap<>();
        body.put("inputs", Map.of());
        body.put("query", query);
        body.put("response_mode", "streaming");
        body.put("user", userId);

        try {
            Request request = new Request.Builder()
                .url(baseUrl + "/v1/chat-messages")
                .header("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(
                    objectMapper.writeValueAsString(body),
                    MediaType.parse("application/json")))
                .build();

            EventSource.Factory factory = EventSources.createFactory(httpClient);
            factory.newEventSource(request, new EventSourceListener() {
                @Override
                public void onEvent(EventSource eventSource, String id,
                                    String type, String data) {
                    try {
                        Map<String, Object> event = objectMapper.readValue(data, Map.class);
                        String eventType = (String) event.get("event");

                        if ("message".equals(eventType)) {
                            String answer = (String) event.get("answer");
                            onMessage.accept(answer);
                        } else if ("error".equals(eventType)) {
                            onError.accept(new RuntimeException("Dify error: " + data));
                        }
                    } catch (Exception e) {
                        onError.accept(e);
                    }
                }

                @Override
                public void onFailure(EventSource eventSource, Throwable t,
                                      Response response) {
                    onError.accept(t);
                }
            });
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    /**
     * 阻塞式聊天 —— 等待完整回答返回
     */
    public CompletableFuture<DifyChatResponse> chatBlocking(
            String query, String userId, Map<String, Object> inputs) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("inputs", inputs);
                body.put("query", query);
                body.put("response_mode", "blocking");
                body.put("user", userId);

                Request request = new Request.Builder()
                    .url(baseUrl + "/v1/chat-messages")
                    .header("Authorization", "Bearer " + apiKey)
                    .post(RequestBody.create(
                        objectMapper.writeValueAsString(body),
                        MediaType.parse("application/json")))
                    .build();

                Response response = httpClient.newCall(request).execute();
                return objectMapper.readValue(
                    response.body().string(), DifyChatResponse.class);
            } catch (Exception e) {
                throw new RuntimeException("Dify chat failed", e);
            }
        });
    }

    /**
     * 执行工作流
     */
    public CompletableFuture<Map<String, Object>> runWorkflow(
            Map<String, Object> inputs, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("inputs", inputs);
                body.put("response_mode", "blocking");
                body.put("user", userId);

                Request request = new Request.Builder()
                    .url(baseUrl + "/v1/workflows/run")
                    .header("Authorization", "Bearer " + apiKey)
                    .post(RequestBody.create(
                        objectMapper.writeValueAsString(body),
                        MediaType.parse("application/json")))
                    .build();

                Response response = httpClient.newCall(request).execute();
                Map<String, Object> result = objectMapper.readValue(
                    response.body().string(), Map.class);
                return (Map<String, Object>) ((Map) result.get("data")).get("outputs");
            } catch (Exception e) {
                throw new RuntimeException("Workflow run failed", e);
            }
        });
    }
}
```

### 5.2 配置类

```java
// DifyProperties.java
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "dify")
public class DifyProperties {
    private String baseUrl = "http://localhost";
    private String apiKey;
}
```

```yaml
# application.yml
dify:
  base-url: https://dify.yourdomain.com
  api-key: app-xxxxxxxxxxxxxxxxxxxx
```

### 5.3 Controller 集成示例

```java
@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final DifyClient difyClient;

    public AIController(DifyClient difyClient) {
        this.difyClient = difyClient;
    }

    /**
     * SSE 流式 AI 对话
     * GET /api/ai/chat?query=xxx&userId=xxx
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(
            @RequestParam String query,
            @RequestParam String userId) {

        return Flux.create(sink -> {
            difyClient.chatStream(
                query, userId,
                token -> sink.next("data: " + token + "\n\n"),
                error -> {
                    sink.next("data: [ERROR] " + error.getMessage() + "\n\n");
                    sink.complete();
                }
            );
        });
    }

    /**
     * 同步 AI 问答案
     * POST /api/ai/ask
     */
    @PostMapping("/ask")
    public DifyChatResponse ask(
            @RequestBody AskRequest request) {
        return difyClient.chatBlocking(
            request.getQuery(),
            request.getUserId(),
            request.getInputs()
        ).join();
    }

    /**
     * 触发 AI 工作流
     * POST /api/ai/workflow/order-analysis
     */
    @PostMapping("/workflow/order-analysis")
    public Map<String, Object> analyzeOrder(
            @RequestBody OrderAnalysisRequest request) {
        Map<String, Object> inputs = Map.of(
            "order_id", request.getOrderId(),
            "user_name", request.getUserName()
        );
        return difyClient.runWorkflow(inputs, request.getUserId()).join();
    }
}
```

### 5.4 Spring Boot 集成架构图

```text
┌─────────────────────────────────────────────────────────┐
│                    用户请求                               │
│                       │                                  │
│  ┌────────────────────▼──────────────────────────────┐  │
│  │              Spring Boot 后端                       │  │
│  │                                                    │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────────────┐ │  │
│  │  │Controller│→ │ Service  │→ │   DifyClient     │ │  │
│  │  │          │  │          │  │ (HTTP + SSE)     │ │  │
│  │  └──────────┘  └──────────┘  └────────┬─────────┘ │  │
│  │                                       │            │  │
│  │  ┌────────────────────────────────────┼─────────┐  │  │
│  │  │ 业务逻辑层                          │         │  │  │
│  │  │ ├── 用户认证 (Spring Security)     │         │  │  │
│  │  │ ├── 数据持久化 (JPA/MyBatis)       │         │  │  │
│  │  │ ├── 业务规则校验                    │         │  │  │
│  │  │ └── 缓存 (Redis)                   │         │  │  │
│  │  └────────────────────────────────────┼─────────┘  │  │
│  └───────────────────────────────────────┼────────────┘  │
│                                          │                │
│  ┌───────────────────────────────────────▼────────────┐  │
│  │                   Dify 平台                         │  │
│  │  ┌──────────┐  ┌──────────┐  ┌─────────────────┐  │  │
│  │  │ 知识库    │  │ 工作流    │  │ Agent           │  │  │
│  │  │ (RAG)    │  │ (编排)    │  │ (工具调用)      │  │  │
│  │  └──────────┘  └──────────┘  └─────────────────┘  │  │
│  └────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘

职责划分：
├── Spring Boot：业务逻辑、用户认证、数据持久化、API 网关
├── Dify：AI 推理、知识库检索、工作流编排
└── 通信：REST API + SSE（通过 DifyClient）
```

---

**上一模块**：[05-Dify工作流与Agent编排](./05-Dify工作流与Agent编排.md) ｜ **返回总览**：[00-Dify知识体系总览](./00-Dify知识体系总览.md)

---

*创建于：2026年7月*
