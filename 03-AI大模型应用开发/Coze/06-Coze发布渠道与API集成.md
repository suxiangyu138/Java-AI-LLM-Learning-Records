# Coze 发布渠道与 API 集成

> 📤 一键发布豆包/飞书/微信/掘金/Web、Bot API 完整参考、Web SDK 嵌入、Java Spring Boot 调用 Coze —— 把 AI Bot 送到用户所在的每一个地方

---

## 📚 目录

1. [多渠道发布指南](#1-多渠道发布指南)
2. [Bot API 完整参考](#2-bot-api-完整参考)
3. [Web SDK 嵌入](#3-web-sdk-嵌入)
4. [Java Spring Boot 集成实战](#4-java-spring-boot-集成实战)
5. [发布后运营优化](#5-发布后运营优化)

---

## 1. 多渠道发布指南

### 1.1 发布渠道对比

| 渠道 | 用户覆盖 | 接入难度 | 适用场景 | 费用 |
|------|:---:|:---:|------|:---:|
| **豆包 (Doubao)** | ⭐⭐⭐⭐⭐ 亿级 | ⭐ (一键) | C 端用户服务 | 免费 |
| **飞书** | ⭐⭐⭐⭐ | ⭐⭐ | 企业内部 | 免费 |
| **微信客服** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | 公众号/小程序客服 | 需认证 |
| **掘金** | ⭐⭐⭐ | ⭐ | 技术社区 | 免费 |
| **Web SDK** | ⭐⭐⭐⭐⭐ | ⭐⭐ | 自有网站 | 免费 |
| **API** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 后端系统集成 | 免费 |

### 1.2 各渠道发布详情

```text
┌─────────────────────────────────────────────────────────┐
│ 豆包 (Doubao)                                           │
├─────────────────────────────────────────────────────────┤
│ 字节跳动旗下 AI 助手，日活 3000万+                        │
│ 发布后用户在豆包 App 中搜索 Bot 名称即可使用              │
│ 发布步骤：发布 → 勾选"豆包" → 填写标签/分类 → 发布       │
│ 审核时间：几分钟 ~ 几小时                                │
│                                                         │
│ 建议：必选渠道！免费流量入口，曝光量最大                   │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ 飞书 (Feishu)                                           │
├─────────────────────────────────────────────────────────┤
│ 发布到飞书后 → 飞书工作台中添加 Bot → 群聊中 @ 使用       │
│ 发布步骤：                                              │
│   1. 飞书管理后台创建企业应用                            │
│   2. 获取 App ID / App Secret                          │
│   3. Coze 发布页 → 飞书 → 填入凭证                      │
│   4. 飞书审核通过 → 企业内可用                           │
│                                                         │
│ 建议：企业内部效率工具、知识库问答的首选                   │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ 微信客服                                                │
├─────────────────────────────────────────────────────────┤
│ 发布到微信 → 用户通过公众号/小程序/视频号发起客服对话     │
│ 前提：需要一个认证过的微信公众号或小程序                  │
│ 发布步骤：                                              │
│   1. 微信公众平台 → 客服功能 → 接入 API                  │
│   2. Coze 发布页 → 微信客服 → 配置 Token/EncodingAESKey │
│   3. 回调 URL 配置 → 验证通过                           │
│                                                         │
│ 建议：电商、SaaS 企业客服接入                            │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ Web SDK                                                 │
├─────────────────────────────────────────────────────────┤
│ 嵌入到自己的网站 → 任何网页都能出现 AI 对话窗口            │
│ 发布步骤：                                              │
│   1. 发布页 → Web SDK → 复制代码                        │
│   2. 粘贴到网站 HTML 中                                 │
│   3. (可选)自定义样式：颜色、位置、Logo                   │
│                                                         │
│ 建议：官网客服、SaaS 产品内置 AI 助手                     │
└─────────────────────────────────────────────────────────┘
```

---

## 2. Bot API 完整参考

### 2.1 获取 API Token

```text
Bot 编辑页 → 发布 → API → 创建 Token

1. 生成访问令牌
   令牌名称：如 "production-api-key"
   权限范围：选择需要的权限
   有效期：永久 / 30天 / 90天
   
2. 复制 Token
   ⚠️ Token 只显示一次！请立即保存

3. API 端点
   https://api.coze.cn/v1/bots/{bot_id}/chat
```

### 2.2 对话 API

```http
POST https://api.coze.cn/v1/bots/{bot_id}/chat
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "user_id": "user-12345",
  "query": "Java 中 HashMap 和 TreeMap 的区别是什么？",
  "stream": true,
  "chat_history": [
    {
      "role": "user",
      "content": "我在学 Java 集合框架"
    },
    {
      "role": "assistant",
      "content": "很好！集合框架是 Java 的核心..."
    }
  ],
  "custom_variables": {
    "user_level": "beginner",
    "preferred_language": "zh-CN"
  }
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `user_id` | string | ✅ | 用户唯一标识 |
| `query` | string | ✅ | 用户问题 |
| `stream` | boolean | ❌ | 是否流式返回（默认 false） |
| `chat_history` | array | ❌ | 对话历史（多轮对话） |
| `custom_variables` | object | ❌ | Bot 中定义的变量值 |

### 2.3 响应格式

```json
// 非流式响应
{
  "code": 0,
  "msg": "success",
  "data": {
    "conversation_id": "conv_xxx",
    "bot_id": "bot_xxx",
    "role": "assistant",
    "content": "HashMap 和 TreeMap 的主要区别在于...",
    "content_type": "text",
    "created_at": 1722345678
  }
}
```

```text
// 流式响应 (SSE)
event: conversation.message.delta
data: {"role":"assistant","content":"Hash","content_type":"text"}

event: conversation.message.delta
data: {"role":"assistant","content":"Map","content_type":"text"}

event: conversation.message.completed
data: {"role":"assistant","content":"HashMap 基于哈希表...","content_type":"text"}

事件类型：
├── conversation.message.delta → 增量文本
├── conversation.message.completed → 消息完成
├── conversation.chat.completed → 对话结束
├── error → 错误信息
└── done → SSE 流结束
```

### 2.4 工作流 API

```http
# 独立运行工作流（不依赖 Bot）
POST https://api.coze.cn/v1/workflow/run
Authorization: Bearer {workflow_token}
Content-Type: application/json

{
  "workflow_id": "wf_xxx",
  "parameters": {
    "order_id": "20260730001",
    "user_name": "张三"
  }
}

# 响应
{
  "code": 0,
  "msg": "success",
  "data": {
    "output": {
      "summary": "订单分析完成...",
      "risk_level": "low",
      "actions": ["发货", "发送确认短信"]
    },
    "usage": {
      "token_count": 1234,
      "execution_time_ms": 2345
    }
  }
}
```

### 2.5 知识库 API

```http
# 创建文档
POST https://api.coze.cn/v1/knowledge/{knowledge_id}/document
Authorization: Bearer {token}
Content-Type: application/json

{
  "documents": [
    {
      "name": "产品手册 v2.3.md",
      "content": "# 产品手册\n\n## 功能介绍...",
      "document_type": "markdown"
    }
  ]
}

# 查询文档列表
GET https://api.coze.cn/v1/knowledge/{knowledge_id}/documents?page=1&size=20
Authorization: Bearer {token}
```

---

## 3. Web SDK 嵌入

### 3.1 基础嵌入

```html
<!-- Coze Bot 发布 → Web SDK → 复制代码 -->

<!-- 方式一：悬浮气泡式（推荐） -->
<div id="coze-chat"></div>
<script>
  new CozeWebSDK({
    botId: 'your_bot_id',
    container: document.getElementById('coze-chat'),
    config: {
      mode: 'bubble',              // bubble | widget | fullscreen
      position: 'right',           // left | right
      offsetBottom: 80,            // 离底部距离
      title: 'Java学习助手-Ace',    // 窗口标题
      placeholder: '输入你的Java问题...',
      icon: 'https://example.com/icon.png',
      color: '#1D4ED8',            // 主题色
      locale: 'zh-CN'
    }
  });
</script>
```

### 3.2 自定义样式

```html
<!-- 嵌入式 Chat Widget -->
<div id="coze-chat-widget" style="width:400px;height:600px;"></div>
<script>
  new CozeWebSDK({
    botId: 'your_bot_id',
    container: document.getElementById('coze-chat-widget'),
    config: {
      mode: 'widget',
      title: '智能客服',
      color: '#FF6600',
      // 自定义 CSS 变量
      cssVariables: {
        '--coze-primary-color': '#FF6600',
        '--coze-bg-color': '#F8F9FA',
        '--coze-font-family': '-apple-system, BlinkMacSystemFont, sans-serif'
      },
      // 首次打开时发送消息
      onChatCreated: function() {
        // 可在此处做初始化逻辑
      }
    }
  });
</script>
```

---

## 4. Java Spring Boot 集成实战

### 4.1 CozeClient 封装

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Component
public class CozeClient {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiBase = "https://api.coze.cn/v1";
    private final String accessToken;
    private final String botId;

    public CozeClient(CozeProperties props) {
        this.accessToken = props.getAccessToken();
        this.botId = props.getBotId();
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 同步对话（阻塞式）
     */
    public String chat(String userId, String query) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("user_id", userId);
            body.put("query", query);
            body.put("stream", false);

            Request request = new Request.Builder()
                .url(apiBase + "/bots/" + botId + "/chat")
                .header("Authorization", "Bearer " + accessToken)
                .post(RequestBody.create(
                    objectMapper.writeValueAsString(body),
                    MediaType.parse("application/json")))
                .build();

            Response response = httpClient.newCall(request).execute();
            Map<String, Object> result = objectMapper.readValue(
                response.body().string(), Map.class);

            Map<String, Object> data = (Map<String, Object>) result.get("data");
            return (String) data.get("content");
        } catch (Exception e) {
            throw new RuntimeException("Coze API call failed", e);
        }
    }

    /**
     * 流式对话（SSE 回调）
     */
    public void chatStream(String userId, String query,
                           Consumer<String> onToken,
                           Consumer<String> onComplete,
                           Consumer<Throwable> onError) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("user_id", userId);
            body.put("query", query);
            body.put("stream", true);

            Request request = new Request.Builder()
                .url(apiBase + "/bots/" + botId + "/chat")
                .header("Authorization", "Bearer " + accessToken)
                .post(RequestBody.create(
                    objectMapper.writeValueAsString(body),
                    MediaType.parse("application/json")))
                .build();

            EventSource.Factory factory = EventSources.createFactory(httpClient);
            factory.newEventSource(request, new EventSourceListener() {

                private final StringBuilder fullContent = new StringBuilder();

                @Override
                public void onEvent(EventSource es, String id, String type, String data) {
                    try {
                        Map<String, Object> event = objectMapper.readValue(data, Map.class);
                        String contentType = (String) event.get("content_type");

                        if ("text".equals(contentType)) {
                            String content = (String) event.get("content");
                            fullContent.append(content);
                            onToken.accept(content);
                        }
                    } catch (Exception e) {
                        onError.accept(e);
                    }
                }

                @Override
                public void onClosed(EventSource eventSource) {
                    onComplete.accept(fullContent.toString());
                }

                @Override
                public void onFailure(EventSource es, Throwable t, Response r) {
                    onError.accept(t);
                }
            });
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    /**
     * 异步工作流调用
     */
    public CompletableFuture<Map<String, Object>> runWorkflow(
            String workflowId, Map<String, Object> params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("workflow_id", workflowId);
                body.put("parameters", params);

                Request request = new Request.Builder()
                    .url(apiBase + "/workflow/run")
                    .header("Authorization", "Bearer " + accessToken)
                    .post(RequestBody.create(
                        objectMapper.writeValueAsString(body),
                        MediaType.parse("application/json")))
                    .build();

                Response response = httpClient.newCall(request).execute();
                Map<String, Object> result = objectMapper.readValue(
                    response.body().string(), Map.class);
                return (Map<String, Object>) result.get("data");
            } catch (Exception e) {
                throw new RuntimeException("Workflow run failed", e);
            }
        });
    }
}
```

### 4.2 配置与 Controller

```java
// CozeProperties.java
@Data
@Configuration
@ConfigurationProperties(prefix = "coze")
public class CozeProperties {
    private String accessToken;
    private String botId;
}
```

```yaml
# application.yml
coze:
  access-token: pat_xxxxxxxxxxxxxxxxxxxxxxxxxxxx
  bot-id: bot_xxxxxxxxxxxxxxxxxxxx
```

```java
// AIController.java
@RestController
@RequestMapping("/api/coze")
public class CozeController {

    private final CozeClient cozeClient;

    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody ChatRequest req) {
        String answer = cozeClient.chat(req.getUserId(), req.getQuery());
        return ResponseEntity.ok(answer);
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(
            @RequestParam String userId,
            @RequestParam String query) {
        return Flux.create(sink -> {
            cozeClient.chatStream(userId, query,
                token -> sink.next("data: " + token + "\n\n"),
                full -> {
                    sink.next("data: [DONE]\n\n");
                    sink.complete();
                },
                error -> {
                    sink.next("data: [ERROR] " + error.getMessage() + "\n\n");
                    sink.complete();
                }
            );
        });
    }

    @PostMapping("/workflow/order-analysis")
    public Map<String, Object> analyzeOrder(@RequestBody OrderRequest req) {
        return cozeClient.runWorkflow("wf_order_analysis", Map.of(
            "order_id", req.getOrderId(),
            "user_name", req.getUserName()
        )).join();
    }
}
```

---

## 5. 发布后运营优化

### 5.1 数据分析指标

```text
Coze Bot 控制台 → 数据分析

关键指标：
├── 📈 DAU/MAU：日活/月活用户
├── 💬 对话轮次：平均每用户对话轮数
├── ⏱️ 平均响应时间
├── 👍 用户满意度：点赞率
├── 🔄 留存率：次日/7日/30日
├── 📊 高频问题：用户最常问的 Top 10 问题
├── ⚡ 触发插件频率：哪些插件被调用最多
└── 💰 Token 消耗：按天/周/月的趋势
```

### 5.2 持续优化

```text
基于数据驱动的优化循环：

1. 分析高频问题
   → 用户最常问什么？→ 优化知识库覆盖
   → 用户总问不到点上？→ 优化示例问题引导

2. 分析低满意度对话
   → 查看点踩的对话 → 复现问题 → 修复
   → 常见原因：知识库缺内容 / 人设回答太模糊 / 插件未触发

3. 分析未触发的技能
   → 插件/工作流没被用过？→ 人设中未引导用户使用
   → 修改人设："如果你需要查询天气，可以直接告诉我城市名"
```

---

**上一模块**：[05-Coze插件系统与开发](./05-Coze插件系统与开发.md) ｜ **下一模块**：[07-Coze高级功能与最佳实践](./07-Coze高级功能与最佳实践.md) ｜ **返回总览**：[00-Coze知识体系总览](./00-Coze知识体系总览.md)

---

*创建于：2026年7月*
