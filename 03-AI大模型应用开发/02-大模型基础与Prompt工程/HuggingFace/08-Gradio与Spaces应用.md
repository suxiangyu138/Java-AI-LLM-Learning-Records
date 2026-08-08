# 08 Gradio 与 Spaces 应用
> 模型的"门面"：Gradio 组件与事件、Blocks 布局、Spaces 云端部署与生产接入

## 📚 目录
1. [Gradio 定位：模型 Demo 的事实标准](#1-gradio-定位模型-demo-的事实标准)
2. [Interface：最快 Demo](#2-interface最快-demo)
3. [核心组件速查](#3-核心组件速查)
4. [Blocks：复杂交互布局](#4-blocks复杂交互布局)
5. [模型推理接入：流式与并发](#5-模型推理接入流式与并发)
6. [Spaces：云端部署](#6-spaces云端部署)
7. [生产化：Gradio vs FastAPI](#7-生产化gradio-vs-fastapi)
8. [常见坑](#8-常见坑)
9. [核心要点](#9-核心要点)

---

## 1. Gradio 定位：模型 Demo 的事实标准

```text
AI 应用的交付光谱：
  原型/演示 → Gradio（几分钟出 Demo）→ Spaces（免费托管）
  内部工具 → Gradio Blocks（带界面）
  生产 API → FastAPI / transformers serve（无界面，高并发）

Gradio 的生态地位：HF Spaces 数十万应用中，Gradio 占绝对主流
```

| 维度 | Gradio | FastAPI |
|------|:---:|:---:|
| 界面 | 自带 Web UI | 无（返回 JSON） |
| 上手 | 10 分钟出 Demo | 30 分钟+ |
| 并发 | 一般（有 Queue 机制） | 高（异步） |
| 定位 | Demo/交互工具 | 生产 API |
| 结合 | — | 可内嵌 Gradio（挂载） |

> 🎯 **核心要点**：Gradio 解决"**模型给谁看/谁用**"——把模型包装成带界面的应用，几分钟可交付。生产高并发 API 用 FastAPI（Python 异步 + FastAPI 体系），Gradio 做演示/内网工具。

## 2. Interface：最快 Demo

```python
import gradio as gr

def chat(message, history):
    # 模型推理逻辑（04 章）
    return "你好！我是演示助手。"   # 示例：换成真实模型调用

# 一行 Interface：输入 + 输出 + 函数
demo = gr.Interface(
    fn=chat,
    inputs=gr.Textbox(label="输入", lines=2),
    outputs=gr.Textbox(label="回复"),
    title="客服助手 Demo",
    description="基于 Qwen 微调模型的演示",
)

demo.launch()          # 本地 http://127.0.0.1:7860
```

| Interface 参数 | 作用 |
|---------------|------|
| `fn` | 处理函数（输入 → 输出） |
| `inputs` / `outputs` | 组件或组件列表 |
| `title` / `description` | 页面文案 |
| `examples` | 示例输入（一键填充） |
| `theme` | 主题样式 |
| `share=True` | 生成公网链接（临时） |

> 💡 `fn` 可以是同步函数（Gradio 自动线程池）或 async 函数（流式友好，§5）——同步模型调用直接写即可。

## 3. 核心组件速查

```python
# 输入组件
gr.Textbox(label="文本", lines=2, max_lines=10)
gr.Slider(minimum=0, maximum=1, step=0.1, value=0.7, label="temperature")
gr.Dropdown(choices=["中文", "英文"], value="中文", label="语言")
gr.Radio(choices=["精简", "详细"], label="风格")
gr.Checkbox(label="流式输出")
gr.Image(type="pil", label="图片")          # 图像输入
gr.Audio(type="filepath")                    # 音频
gr.File(label="上传文件")
gr.Dataframe(headers=["列1", "列2"])         # 表格

# 输出组件
gr.Textbox(label="输出")
gr.Markdown()                                # 渲染富文本
gr.Image()                                   # 图像输出
gr.JSON()                                    # 结构化输出
gr.Dataframe()
gr.Label(num_top_classes=3)                  # 分类置信度
```

| 组件类型 | 场景 |
|---------|------|
| Textbox/Markdown | ⭐ 文本模型 90% 场景 |
| Slider/Dropdown | 参数调节（temperature/top_p） |
| Image/Audio | 多模态模型 |
| Dataframe | 表格/结构化输出 |
| Chatbot | 对话界面（§4 Blocks） |

## 4. Blocks：复杂交互布局

```python
# Blocks：自由布局 + 多组件联动 + 事件链
with gr.Blocks(title="客服 Agent") as demo:
    gr.Markdown("# 🛎️ 智能客服演示")

    with gr.Row():                             # 横向布局
        with gr.Column(scale=2):               # 左列（占 2/3）
            chatbot = gr.Chatbot(label="对话", height=400)
            msg = gr.Textbox(label="输入", placeholder="描述你的问题...")
            with gr.Row():
                clear = gr.Button("清空对话")
                stop = gr.Button("停止生成")
        with gr.Column(scale=1):               # 右列（参数面板）
            temp = gr.Slider(0, 2, value=0.7, label="temperature")
            top_p = gr.Slider(0, 1, value=0.9, label="top_p")

    # 事件：提交 → 处理函数（history 为对话状态）
    def respond(message, history, temperature, top_p):
        # history:  [(user, bot), ...]
        # 调用模型（04/05 章），返回新回复
        history.append((message, "回复内容"))
        return history, ""                      # 更新 chatbot + 清空输入框

    msg.submit(respond, [msg, chatbot, temp, top_p], [chatbot, msg])
    clear.click(lambda: [], None, chatbot)
    # 流式：fn 改成生成器 + queue()（§5）
```

| Blocks 能力 | 说明 |
|------------|------|
| Row/Column/Tab | 布局系统 |
| 事件绑定 | `.submit()` / `.click()` / `.change()` |
| 状态传递 | history 参数（对话上下文） |
| 多组件联动 | 事件链（滑块 → 重新生成） |
| Queue | `demo.queue()` 排队机制（流式/并发必需） |

> 🎯 **核心要点**：**Blocks = "带状态的交互应用"**——比 Interface 多三样：布局、多组件联动、对话状态。客服/Agent 类 Demo 用 Blocks + Chatbot + queue() 是标准组合。

## 5. 模型推理接入：流式与并发

```python
import gradio as gr
from transformers import AutoModelForCausalLM, AutoTokenizer

# 全局加载一次（03 章：显存/耗时）
tokenizer = AutoTokenizer.from_pretrained("Qwen/Qwen2.5-7B-Instruct")
model = AutoModelForCausalLM.from_pretrained(
    "Qwen/Qwen2.5-7B-Instruct", device_map="auto", torch_dtype="auto")

# 流式生成器：yield 逐段输出（打字机效果）
def stream_chat(message, history, temperature):
    messages = [{"role": "user", "content": message}]
    inputs = tokenizer.apply_chat_template(
        messages, tokenize=True, add_generation_prompt=True, return_tensors="pt"
    ).to(model.device)
    text = ""
    for token in model.generate(**inputs, max_new_tokens=512,
                                temperature=temperature, do_sample=True,
                                streamer=TextIteratorStreamer(tokenizer)):
        text += token
        yield text                      # ⭐ 逐段 yield → 前端打字机

with gr.Blocks() as demo:
    chatbot = gr.Chatbot()
    msg = gr.Textbox()
    msg.submit(stream_chat, [msg, chatbot, gr.Slider(0, 2, 0.7)], [chatbot])

demo.queue()          # ⭐ 流式必须开 queue（管理并发请求）
demo.launch()
```

| 流式要素 | 说明 |
|---------|------|
| 生成器函数 | `yield` 逐段输出 |
| `TextIteratorStreamer` | Transformers 流式 token 迭代器 |
| `demo.queue()` | 排队机制（流式/并发必需） |
| 全局模型 | 加载一次，函数闭包引用 |

> ⚠️ Gradio 并发纪律：**模型只加载一次（模块级）**，不能每次请求加载；并发请求走 `queue()` 排队（默认限流）；多用户并发时注意显存（单模型 + 批处理）。高并发生产还是 FastAPI + vLLM（04 章 §6）。

## 6. Spaces：云端部署

```text
Spaces = HF 托管的应用运行环境（免费/付费 GPU）
  https://huggingface.co/spaces/{owner}/{space-name}
```

**部署三要素**：

```text
① app.py        # Gradio 应用代码（demo.launch() 不要写 host/port）
② requirements.txt  # 依赖（gradio、transformers、torch 等）
③ README.md     # 配置（sdk: gradio 在 frontmatter 里）
```

```markdown
---
title: 客服助手 Demo
emoji: 🛎️
colorFrom: blue
colorTo: green
sdk: gradio
sdk_version: "5.x"
app_file: app.py
pinned: false
---
```

```bash
# 上传（01 章 §7 方式）
huggingface-cli upload my-org/cs-demo . --repo-type space
# 或 git push（Spaces 本质是 git 仓库）
git clone https://huggingface.co/spaces/my-org/cs-demo
```

| Spaces 配置 | 说明 |
|------------|------|
| sdk | gradio / streamlit / static / docker |
| 硬件 | 免费 CPU / 付费 GPU（T4/A10/A100） |
| 环境变量 | 设置页配置（HF_TOKEN 等） |
| 版本 | 锁定 gradio 版本（升级破坏兼容） |
| 私有 Space | 团队内部用（private=True） |

> 🎯 **核心要点**：Spaces = "**把 Demo 变成可访问的 URL**"——代码 push 即上线，适合给客户/同事/面试展示。GPU 付费按秒计费，演示完记得停（睡眠设置）。

## 7. 生产化：Gradio vs FastAPI

| 场景 | 方案 |
|------|------|
| 演示/原型 | Gradio + Spaces（免费/快） |
| 内部工具 | Gradio Blocks（内网部署） |
| 生产 API | FastAPI + transformers serve / vLLM |
| 两者都要 | FastAPI 挂载 Gradio 路由（`app.mount("/demo", gradio_app)`） |

```python
# 生产 API + 内置 Demo 面板（FastAPI 挂载 Gradio）
from fastapi import FastAPI
import gradio as gr

api = FastAPI()
demo = gr.Interface(fn=chat, inputs="text", outputs="text")

api.mount("/demo", demo.app)      # Gradio 面板在 /demo
# API 主路径照常业务
```

> 💡 生产架构建议：**API 走 FastAPI（认证/限流/监控，FastAPI 体系），Demo 面板挂 /demo 给内部用**——一份模型服务两种入口。

## 8. 常见坑

| # | 坑 | 现象 | 解法 |
|:---:|-----|------|------|
| 1 | 每次请求加载模型 | 慢到超时 | 模块级加载一次 |
| 2 | 流式没开 queue | 请求串行卡死 | demo.queue() |
| 3 | 多用户并发 OOM | 显存爆 | queue 限流 + 小模型/量化 |
| 4 | Space 依赖版本冲突 | 部署失败 | 锁定版本 |
| 5 | Space 内存不够 | OOM 重启 | 选大硬件 + 量化（06 章 QLoRA 部署） |
| 6 | launch() 参数错误 | Space 部署不生效 | Space 里不写 host/port |
| 7 | history 状态混乱 | 对话错乱 | 事件参数顺序对照组件列表 |
| 8 | 中文乱码 | UI 显示问题 | 组件 label + theme 正常即可（数据侧 utf-8） |

## 9. 核心要点

| 序号 | 要点 |
|:---:|------|
| 1 | Gradio = 模型 Demo 事实标准；生产 API 用 FastAPI |
| 2 | Interface 最快 Demo：fn + inputs + outputs |
| 3 | Blocks：布局 + 多组件联动 + Chatbot 对话 |
| 4 | 流式 = 生成器 yield + TextIteratorStreamer + queue() |
| 5 | 模型全局加载一次，并发走 queue |
| 6 | Spaces：app.py + requirements + README 三件套即上线 |
| 7 | 生产架构：FastAPI 主 API + mount("/demo") 内置面板 |
| 8 | 八大坑：重复加载/无 queue/版本/显存 |

---

**下一模块**：[09-推理优化与生产实践](09-推理优化与生产实践.md) / **返回总览**：[00-HuggingFace知识体系总览](00-HuggingFace知识体系总览.md)

## 参考来源

- [Gradio 官方文档](https://www.gradio.app/docs)
- [HF Spaces 官方文档](https://huggingface.co/docs/hub/spaces)
- [Gradio：Blocks 指南](https://www.gradio.app/guides/blocks-and-event-listeners)
- [HF 官方：Spaces 配置参考](https://huggingface.co/docs/hub/spaces-config-reference)
