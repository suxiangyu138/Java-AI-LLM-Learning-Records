# 05 - 流式输出与 Computer Use

> 🎯 Claude 的 SSE 流式输出 + Computer Use 桌面操控 + 视觉输入= 三大差异化能力。流式实现逐 Token 打字机效果，Computer Use 让 Claude 操控桌面 GUI（业界独有）

---

## 目录

1. [流式输出 SSE 实战](#1-流式输出-sse-实战)
2. [思考流式：可视化推理过程](#2-思考流式可视化推理过程)
3. [Computer Use：AI 操控桌面](#3-computer-useai-操控桌面)
4. [视觉输入：图片理解](#4-视觉输入图片理解)

---

## 1. 流式输出 SSE 实战

```python
# Python SDK 流式（最简单）
async with client.messages.stream(
    model="claude-sonnet-4-6",
    max_tokens=1024,
    messages=[{"role": "user", "content": "写一个快速排序的 Go 实现"}]
) as stream:
    async for text in stream.text_stream:
        print(text, end="", flush=True)    # 逐 Token 打印

# 最终消息（含完整响应元数据）
final_message = await stream.get_final_message()
print(f"\nToken 消耗: {final_message.usage.input_tokens} → {final_message.usage.output_tokens}")
```

```java
// Java SDK 流式（Spring Boot SSE）
@PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> streamChat(@RequestBody String message) {
    return Flux.create(sink -> {
        client.messages().createStreaming(params, new MessageStreamHandler() {
            @Override public void onText(String text) { sink.next(text); }
            @Override public void onComplete() { sink.complete(); }
            @Override public void onError(Throwable e) { sink.error(e); }
        });
    });
}
```

### 流式事件类型

| 事件 | 含义 |
|------|------|
| `message_start` | 消息开始（含 usage.input_tokens） |
| `content_block_start` | 内容块开始（thinking / text / tool_use） |
| `content_block_delta` | 增量内容（thinking_delta / text_delta / input_json_delta） |
| `content_block_stop` | 内容块结束 |
| `message_delta` | 消息级元数据（stop_reason、usage.output_tokens） |
| `message_stop` | 消息结束 |

---

## 2. 思考流式：可视化推理过程

```python
# 流式状态下显示 AI 思考过程
async with client.messages.stream(
    model="claude-sonnet-4-6",
    max_tokens=8192,
    thinking={"type": "adaptive", "display": "summarized"},
    messages=[...]
) as stream:
    thinking_panel = []
    answer_panel = []

    async for event in stream:
        # ① 思考阶段
        if event.type == "content_block_delta" and event.delta.type == "thinking_delta":
            thinking_panel.append(event.delta.thinking)
            # 前端：更新"思考中..."可折叠面板

        # ② 回答阶段
        elif event.type == "content_block_delta" and event.delta.type == "text_delta":
            answer_panel.append(event.delta.text)
            # 前端：逐 Token 打印最终回答
```

```javascript
// 前端 SSE 消费示例
const eventSource = new EventSource("/api/chat/stream?message=写一个排序算法");

let inThinking = false;
eventSource.onmessage = (e) => {
    const data = JSON.parse(e.data);
    if (data.type === "thinking") {
        if (!inThinking) { showThinkingPanel(); inThinking = true; }
        appendToThinking(data.content);     // 可折叠面板
    } else if (data.type === "text") {
        if (inThinking) { collapseThinkingPanel(); inThinking = false; }
        appendToAnswer(data.content);       // 打字机效果
    }
};
```

---

## 3. Computer Use：AI 操控桌面

Computer Use 是 Claude 业界独有的能力 — 通过截图 + 鼠标/键盘输入操控桌面环境。

```python
# Computer Use 工具定义
tools = [{
    "type": "computer_20251124",
    "display_width_px": 1920,
    "display_height_px": 1080,
    "environment": "browser"              # browser / linux / mac / windows
}]

# Claude 可用操作
# ① 鼠标：click、double_click、move、drag
# ② 键盘：type（输入文本）、key（按键组合）
# ③ 截图：screenshot（获取当前屏幕）
# ④ 等待：wait（等待渲染/加载）

# 典型场景：
# "打开浏览器 → 搜索 GitHub → 找到 Spring Boot 仓库 → 截图 Star 数量"
```

**Computer Use 的适用边界：**
| ✅ 适合 | ❌ 不适合 |
|---------|----------|
| 旧系统无 API 可调 | 有稳定 API 的服务 |
| 一次性任务（无需重建 API） | 高频/大规模自动化 |
| 原型验证（快速试方案） | 可靠性要求高（100% 准确） |

**性能：** OSWorld 得分 ~72.5%（2024 年 <15%）— 有进步但仍较慢且不可靠。

---

## 4. 视觉输入：图片理解

```python
import base64

with open("architecture.png", "rb") as f:
    img_b64 = base64.b64encode(f.read()).decode()

response = client.messages.create(
    model="claude-sonnet-4-6",
    max_tokens=1024,
    messages=[{
        "role": "user",
        "content": [
            {"type": "text", "text": "分析这个系统架构图，列出潜在的性能瓶颈"},
            {"type": "image", "source": {
                "type": "base64",
                "media_type": "image/png",
                "data": img_b64
            }}
        ]
    }]
)
```

**支持的图片格式：** JPEG、PNG、GIF、WebP。建议分辨率 ≤ 8000×8000px，单图 ≤ 10MB。

---

> 🎯 **核心要点**：Claude 三大差异化能力 — **① 思考流式（thinking_delta → UI 可折叠推理面板）② Computer Use（操控桌面 GUI，OSWorld 72.5%）③ 视觉输入（base64 图片分析）**。流式事件顺序：thinking_delta（先） → text_delta（后），映射到前端 = "思考面板" → "回答打字机"。

**下一模块**：[06-生产最佳实践与成本优化](06-生产最佳实践与成本优化.md) / **返回总览**：[00-总览](00-Claude-API知识体系总览.md)
