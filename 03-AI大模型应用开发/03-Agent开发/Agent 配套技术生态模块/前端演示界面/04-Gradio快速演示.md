# Gradio 快速演示

> ML 模型演示之王（6.20.0，2026）：3-5 行代码出可分享的演示界面，Hugging Face Spaces 一键托管。聊天是它多种输入输出形态之一——Agent 演示选它的前提是"多模态输入输出"比"对话深度"更重要。

## 1. 定位与适用边界

| 维度 | Gradio 表现 | 说明 |
|---|---|---|
| 上手速度 | 最快（3-5 行起） | `gr.ChatInterface` 一行出聊天页 |
| 多模态 | 最强 | 图像/音频/视频/文件原生组件 |
| 分享 | 即开即用 | HF Spaces 免费托管 + 公开链接 |
| 聊天/Agent | ⚠️ 可用但浅 | 面向单轮交互，复杂状态管理弱 |
| 品牌定制 | 受限 | 布局组件化，深度定制靠 HTML 组件 |
| 生产级产品 | ❌ 不建议 | 定位原型/演示/评测 |

> 🎯 核心要点：**Gradio 是"给模型做演示和评测界面"的工具**——模型评测表、多模态输入输出、给非技术同事体验，是它的主场；做产品级聊天 UI 请用 [05 Chainlit](05-Chainlit对话界面.md) 或 [07 AI SDK](07-现代前端方案：AI-SDK与React生态.md)。

## 2. 一行起步：ChatInterface

```python
import gradio as gr

def chat(message, history):
    # message: 用户输入；history: 多轮历史（list[tuple]）
    return agent.run(message, history)

gr.ChatInterface(fn=chat, type="messages").launch()
```

| 参数 | 作用 |
|---|---|
| `fn` | 核心函数：入参用户消息+历史，返回助手回复（或生成器实现流式） |
| `type="messages"` | 6.x 推荐消息格式（list[dict]，与 OpenAI 消息结构对齐） |
| `multimodal=True` | 输入框支持文本+文件+图像+音频 |
| `examples` | 预设示例问题，演示必备 |
| `theme` | 内置主题/品牌色 |

## 3. 流式与状态

```python
def chat(message, history):
    def gen():
        for chunk in agent.stream(message, history):
            yield chunk          # yield 生成器 → 自动打字机效果
    return gen()

gr.ChatInterface(fn=chat, type="messages", streaming=True).launch()
```

- 返回生成器即自动流式，无需任何额外配置。
- `history` 由框架维护，多轮对话零手写状态。
- 复杂 Agent 状态（工具结果、会话 id）建议用 `gr.State` 组件显式持有。

## 4. Blocks：自定义布局

ChatInterface 不够时用 Blocks 搭任意布局：

```python
with gr.Blocks(title="Agent Demo") as demo:
    gr.Markdown("# 我的 Agent 演示")
    chatbot = gr.Chatbot(type="messages", height=500)
    with gr.Row():
        inp = gr.Textbox(placeholder="问点什么...", scale=4)
        btn = gr.Button("发送", scale=1)
    status = gr.Markdown("")          # 展示工具调用状态

    def respond(message, history):
        status.update("🔍 正在搜索...")   # 过程提示
        ...  # Agent 调用
        status.update("")
        return history + [{"role": "assistant", "content": answer}]

    btn.click(respond, [inp, chatbot], [chatbot, status])
```

| 组件 | 用途 |
|---|---|
| `gr.Chatbot` | 对话展示区（type="messages" 支持角色/附件/思考块） |
| `gr.ChatMessage` | 单条消息（含图标、元数据、可折叠块） |
| `gr.State` | 隐藏状态（会话 id、工具结果） |
| `gr.HTML` | 自定义 HTML/JS 组件（6.x 可完全自定义 UI） |
| `gr.Dataframe` | 表格/评测结果展示 |

## 5. Gradio 6 新能力（2026 基准）

| 能力 | 说明 |
|---|---|
| 更快前端 | Svelte 重写，加载与交互提速 |
| MCP 集成 | 可把 Gradio 应用作为 MCP Server 暴露 / 调用 MCP 工具 |
| 完整 HTML 组件 | 基础 HTML/CSS/JS 自定义任意 UI 组件 |
| 流式多模态输出 | 生成中实时输出图像/音频 |
| 自动 API 文档 | 每个组件自带 live 文档（gr.ChatInterface 即 REST 端点） |
| 轻量安装 | 6.x 依赖精简，体积更小 |

> 💡 与 HuggingFace 生态的关系：本仓库 [HuggingFace 知识体系](..%2F..%2F..%2F02-大模型基础与Prompt工程%2FHuggingFace%2F00-HuggingFace知识体系总览.md) 第 09 篇（Gradio 与 Spaces）已讲 HF 视角的部署细节；本篇聚焦"Agent 演示界面"选型视角，Spaces 部署细节见彼处。

## 6. 用 Gradio 演示 Agent 的姿势

| 需求 | 做法 |
|---|---|
| 工具调用过程展示 | `gr.Markdown` 状态区 + `gr.ChatMessage` 元数据（metadata 渲染工具卡片） |
| 引用来源 | 答案 markdown 内嵌链接；Chatbot 支持 message.metadata 渲染 |
| 图像/音频输入 | `multimodal=True` 或 Blocks 加 `gr.Image`/`gr.Audio` |
| 评测模式 | 表格展示 prompt/回答/得分，`gr.Dataframe` 直接渲染评测结果 |
| 分享 | 本地 `launch(share=True)` 临时链接 或 推到 HF Spaces |

### 6.1 工具调用卡片：用 message.metadata 渲染

Chatbot 的 `type="messages"` 消息支持 metadata，工具调用结果可挂上去：

```python
def respond(message, history):
    tool_logs = run_agent_with_tools(message)      # [{"name","status","dur_ms"}, ...]
    metadata = {
        "tool_calls": [
            {"icon": "🔧", "name": t["name"], "status": t["status"], "duration_ms": t["dur_ms"]}
            for t in tool_logs
        ]
    }
    history.append({
        "role": "assistant",
        "content": answer_text,
        "metadata": metadata,                       # 前端按 metadata 渲染工具卡片
    })
    return history
```

配合 `gr.ChatMessage` 的 `metadata` 参数即可在消息上方渲染"工具摘要行 + 折叠详情"，演示 Agent 工作过程（完整过程可视化方案见 [08 模块](08-Agent专属界面：思考过程与工具调用可视化.md)）。

### 6.2 评测模式：一行表格出对比报告

Gradio 常用于模型/提示词对比评测（它的原生定位）：

```python
def evaluate(model_a, model_b, questions):
    rows = []
    for q in questions:
        rows.append({
            "问题": q,
            "A 回答": call(model_a, q)[:50] + "...",
            "B 回答": call(model_b, q)[:50] + "...",
            "人工评分": "",        # 表格内直接手填
        })
    return rows

gr.Interface(fn=evaluate,
    inputs=[gr.Dropdown(["deepseek-v4", "gpt-5.x"]), gr.Dropdown(["gpt-5.x", "claude-sonnet-5"]),
            gr.Dataframe(headers=["问题"])],
    outputs=gr.Dataframe(headers=["问题", "A 回答", "B 回答", "人工评分"]),
    title="模型对比评测").launch()
```

### 6.3 常见坑

| 坑 | 现象 | 解法 |
|---|---|---|
| 并发请求互相阻塞 | 多人同时点按钮卡死 | `launch(concurrency_count=...)` 调并发数；Queue（`gr.Interface(queue=True)`）排队 |
| 长生成无反馈 | 用户以为卡死 | 用生成器流式返回（`yield`），配合 `gr.Progress` 显示进度 |
| history 格式混用 | 渲染报错 | `type="messages"` 下统一 dict 格式 `{"role","content"}`，勿混 tuple |
| 旧版 API 迁移 | 组件报 deprecated 警告 | 6.x 统一用新式组件参数（如 `gr.update` 已废弃改为返回组件对象） |
| 主题定制失效 | 品牌色不生效 | `theme=gr.themes.Base(primary_hue=...)` 在 Blocks 构建时传入，运行时改无效 |

## 7. 速查：一句话决策

| 你的场景 | 结论 |
|---|---|
| 给模型/Agent 做可分享 Demo、模型对比评测 | ✅ Gradio |
| 需要图像/音频/视频输入输出 | ✅ Gradio（无出其右） |
| 纯文本 Agent 对话演示 | 两者皆可，Streamlit 数据能力更全 |
| 产品级对话应用 | ❌ 换 Chainlit / AI SDK |

> 🎯 核心要点：Gradio 的定位是**"演示与评测"而非"产品"**——它解决的问题是"让不懂技术的人 30 秒上手体验你的 Agent"，Spaces 免费托管让它成为课堂、开源项目、模型卡的默认演示载体。

---

**下一模块**：[05-Chainlit 对话界面](05-Chainlit对话界面.md)　**返回总览**：[00-前端演示界面总览](00-前端演示界面总览.md)

## 参考来源

- [gradio@6.20.0 Release（GitHub）](https://github.com/gradio-app/gradio/releases/tag/gradio%406.20.0)
- [7 Best UI Frameworks for AI Agents (2026 Guide)（fast.io）](https://fast.io/resources/best-ui-frameworks-ai-agents/)
- [Streamlit vs Gradio vs Chainlit vs Marimo（heyclau.de）](https://heyclau.de/compare/ml-app-ui-frameworks)
- [Build Lightning-Fast AI Apps with Hugging Face Gradio（SambaNova）](https://sambanova.ai/videos/build-lightning-fast-ai-apps-with-hugging-face-gradio)
