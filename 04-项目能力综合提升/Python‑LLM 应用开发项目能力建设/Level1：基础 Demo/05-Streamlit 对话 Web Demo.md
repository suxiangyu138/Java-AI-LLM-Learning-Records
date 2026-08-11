# 05 Streamlit 对话 Web Demo

> 第一个有界面的 AI 应用：用 Streamlit 把 03 篇的模型调用包成一个聊天网页。理解 st.chat_message / st.chat_input / st.write_stream 三个核心原语与 session_state 的运行机制，这是 08 篇闭环 Demo 的界面层。

## 📚 目录

1. [目标与验收](#1-目标与验收)
2. [为什么是 Streamlit](#2-为什么是-streamlit)
3. [第一个界面：页面骨架](#3-第一个界面页面骨架)
4. [聊天交互：三原语组合](#4-聊天交互三原语组合)
5. [流式打字机：st.write_stream](#5-流式打字机stwrite_stream)
6. [运行机制与性能细节](#6-运行机制与性能细节)
7. [常见坑](#7-常见坑)

---

## 1. 目标与验收

本 Demo 的产出：一个可运行的聊天网页（`streamlit run app.py` 打开浏览器），能多轮对话、流式输出。验收标准：**能说清 session_state 为什么能跨 rerun 保存消息**；**能讲出 Streamlit 脚本的 rerun 机制**（为什么每次交互整个脚本重跑）；**能独立写出三原语的组合代码**。版本基线（2026-08）：Streamlit 1.59.0（2026-07-06 发布，chat_input 新增 submit_mode 与文件粘贴能力）。

## 2. 为什么是 Streamlit

AI Demo 阶段的界面选型，Streamlit 是事实标准：**纯 Python 写界面**（不用学 HTML/JS/前后端分离），**交互组件内置**（聊天、上传、图表开箱即用），**几分钟出成品**。它的定位是"数据/AI 应用的快速界面"，与 Flask/FastAPI 分工明确：**FastAPI 是给程序用的 API**（返回 JSON），**Streamlit 是给人用的界面**（渲染页面）——Level2 工程化项目会两者结合（FastAPI 做服务，Streamlit 或前端做界面），Demo 阶段只用 Streamlit。一个脚本文件就是一个应用，`streamlit run app.py` 启动，浏览器自动打开，改代码保存后页面热更新——开发体验对新手极其友好。

## 3. 第一个界面：页面骨架

同为快速界面方案，Streamlit 与 Gradio 的选型值得知道：**Gradio 更偏"模型演示"**（几分钟出单模型 Demo、拖拽上传、界面组件少而专），**Streamlit 更偏"数据/业务应用"**（组件生态全、页面组织灵活、与 pandas 图表配合强）。做"给用户用的应用"（本层闭环 Demo 的定位）选 Streamlit；做"给模型加个壳"（模型评估、演示页）选 Gradio——一句话：Gradio 是模型展示框，Streamlit 是应用画布。

任何 Streamlit 应用的第一步是页面配置与静态内容：

```python
# app.py
import streamlit as st

st.set_page_config(page_title="我的 AI 助手", page_icon="🤖", layout="centered")
st.title("Python AI 助手")
st.caption("Level1 Demo · DeepSeek V4 驱动")

# 侧边栏：模型选择等配置
with st.sidebar:
    st.header("设置")
    model = st.selectbox("模型", ["deepseek-v4-flash", "deepseek-v4-pro"])
    temperature = st.slider("温度", 0.0, 2.0, 0.7, 0.1)
```

三个基础组件：**st.set_page_config 必须是脚本第一个 Streamlit 调用**（设页面标题与布局）；**st.title / st.caption** 是标题与副标题；**st.selectbox / st.slider** 是表单控件，它们的**返回值就是当前选中值**（这是 Streamlit 声明式编程的核心——控件即变量）。侧边栏用 with st.sidebar 包裹，Demo 阶段够用。启动：`streamlit run app.py`（在项目根，确保 .env 同目录可被读取）。

## 4. 聊天交互：三原语组合

聊天界面的核心是三个原语：**st.chat_message(role)** 渲染一条消息气泡、**st.chat_input()** 底部输入框、**st.session_state** 跨 rerun 保存数据。完整聊天逻辑：

```python
import streamlit as st
from openai import OpenAI
from dotenv import load_dotenv
import os

load_dotenv()

# 缓存客户端：避免每次 rerun 重建（见第 6 节）
@st.cache_resource
def get_client():
    return OpenAI(api_key=os.getenv("DEEPSEEK_API_KEY"),
                  base_url="https://api.deepseek.com/v1")

client = get_client()

# 初始化会话历史：首次运行才执行
if "messages" not in st.session_state:
    st.session_state.messages = [{"role": "system", "content": "你是 Python 学习助手"}]

# 渲染历史消息
for msg in st.session_state.messages:
    if msg["role"] != "system":
        with st.chat_message(msg["role"]):
            st.markdown(msg["content"])

# 接收新输入 —— walrus 操作符：输入框为空时 prompt 为 None
if prompt := st.chat_input("说点什么..."):
    st.session_state.messages.append({"role": "user", "content": prompt})
    with st.chat_message("user"):
        st.markdown(prompt)

    resp = client.chat.completions.create(
        model="deepseek-v4-flash", messages=st.session_state.messages)
    answer = resp.choices[0].message.content
    st.session_state.messages.append({"role": "assistant", "content": answer})
    with st.chat_message("assistant"):
        st.markdown(answer)
```

理解这段代码的运行方式——**Streamlit 脚本是"状态机重放"**：每次交互（点按钮、发消息）都会把整个脚本从头到尾重跑一遍，**session_state 是重跑之间唯一保留的数据**（按浏览器会话隔离）。所以代码结构是"渲染已有状态 + 处理新事件"两段式：先 for 循环渲染历史，再处理本次输入。**messages 里必须存 system 消息**（模型上下文靠它），但渲染时跳过它——这个"渲染层与上下文层分离"的小设计在 08 篇会变成正式约定（历史存 user/assistant，上下文每轮现拼）。

侧边栏的控件与请求参数联动（第 3 节配置要真正生效）：把 create 调用改成从 session_state 取值——`model` 与 `temperature` 是 selectbox/slider 的返回值（脚本每次重跑都会重新取到用户当前选择），所以直接在调用处引用即可；注意**控件值不是持久的配置，而是"当前选择"**——中途改温度只影响下一次请求，已生成的历史不受影响（这是合理行为）。如果希望配置在多个页面共享，把控件值存进 `st.session_state` 再统一读取，Demo 阶段直接引用返回值更简单。

## 5. 流式打字机：st.write_stream

一次性返回体验差（长回答要干等几十秒），用 **st.write_stream** 消费流式生成器实现打字机效果——它接受一个逐段 yield 的生成器：

```python
def stream_answer(messages: list[dict]):
    stream = client.chat.completions.create(
        model="deepseek-v4-flash", messages=messages, stream=True)
    for chunk in stream:
        if chunk.choices[0].delta.content:
            yield chunk.choices[0].delta.content

# 在消息处理分支里：
if prompt := st.chat_input("说点什么..."):
    st.session_state.messages.append({"role": "user", "content": prompt})
    with st.chat_message("user"):
        st.markdown(prompt)

    with st.chat_message("assistant"):
        answer = st.write_stream(stream_answer(st.session_state.messages))
    st.session_state.messages.append({"role": "assistant", "content": answer})
```

两个要点：**生成器函数 yield 逐段文本**（03 篇的流式循环包装成生成器即可）；**st.write_stream 返回完整拼接结果**（赋给 answer 存进历史——历史里必须存完整内容，不能只存片段）。st.write_stream 也接受可迭代的列表/字符串序列（不只生成器），接口统一是"可迭代的文本段"。流式是 AI 应用体验标配：首字 1 秒内出现，用户感知"模型在工作"。也可以 `st.write_stream(stream_answer(...))` 前面加 `with st.spinner("思考中...")` 做加载提示。

## 6. 运行机制与性能细节

理解三个运行机制，能避免 80% 的 Streamlit 坑。**rerun 全量重跑**：脚本每次交互都从头执行，所以"只初始化一次"的操作必须用 session_state 判断（`if "messages" not in st.session_state`）；**@st.cache_resource 缓存重资源**：OpenAI 客户端是连接池对象，每次 rerun 重建浪费且可能撑爆连接，用装饰器缓存后只建一次（同理可缓存 Chroma 客户端、embedding 模型）；**控件即变量**：selectbox 的返回值在重跑时自动带上用户选择——所以配置类控件放脚本顶部，下面代码直接用。

性能与体验细节：**消息多了渲染慢**（几百条历史全量重渲染）——Demo 阶段不管，Level2 用分页/虚拟滚动；**长回答用 st.markdown 渲染**（支持富文本）；**出错要有反馈**：调用模型 try/except，失败时删掉刚 append 的用户消息并提示（否则用户看到自己的消息没有回复，不知道发生了什么）：

```python
if prompt := st.chat_input("说点什么..."):
    st.session_state.messages.append({"role": "user", "content": prompt})
    with st.chat_message("user"):
        st.markdown(prompt)
    try:
        answer = client.chat.completions.create(
            model="deepseek-v4-flash", messages=st.session_state.messages
        ).choices[0].message.content
        st.session_state.messages.append({"role": "assistant", "content": answer})
        with st.chat_message("assistant"):
            st.markdown(answer)
    except Exception as e:
        st.session_state.messages.pop()          # 回滚失败的用户消息
        st.error(f"调用失败：{e}")                 # 页面级错误提示
```

还有两个容易忽略的体验细节：**输入框状态**——Streamlit 1.59 的 `st.chat_input(submit_mode="stop")` 可以在生成过程中把发送键变成停止键（长时间生成时用户能中断）；**清空对话按钮**——侧边栏加 `if st.button("清空对话"): st.session_state.messages = [...]`，多轮测试的刚需（注意清空时要保留 system 首条，否则下次请求没有角色设定）。这些细节是"Demo 做得像产品"的分水岭，也是简历里"体验优化"素材的来源。

## 7. 常见坑

**NameError: prompt 未定义**：忘了 walrus 操作符（`if prompt := st.chat_input(...)` 而不是先取再判）——st.chat_input 在输入为空时返回 None，必须一行内完成"取值+判空"。

**历史消息不完整/上下文丢失**：历史数组里只存了渲染用的消息，没存 system——检查 messages 列表首条是否 system。

**每次交互都很慢**：客户端在 rerun 中反复重建——加 @st.cache_resource；或 embedding 模型每次重新加载（06 篇会遇到，同样用缓存装饰器）。

**浏览器页面没更新**：Streamlit 检测文件变化才 rerun，确认保存的是 app.py 本身；多实例时用 `--server.headless true` 跑在后台。

**端口占用**：默认 8501 被占，`streamlit run app.py --server.port 8502` 换端口。

**密钥读取失败**：.env 的 load_dotenv() 要在 import streamlit 之后、创建 client 之前调用，且 streamlit run 的工作目录是项目根（.env 在根目录就没问题）。

**按钮/输入框"点了没反应"**：控件回调后 rerun 了但界面没变化——检查代码是否依赖了某个"只初始化一次"的状态（比如 message 列表被重新赋值覆盖了历史——初始化语句要放在 `if "messages" not in st.session_state` 判断里，而不是每次重跑都执行）；**多标签页相互干扰**：session_state 按浏览器会话（标签页）隔离，两个标签页互不影响是正常行为，想共享数据才需要持久化（Level2 用数据库）。

> 🎯 **核心要点**：Streamlit 的心智模型是"**状态机重放**"——脚本每轮重跑、session_state 保留数据、控件即变量、重资源用缓存装饰器。记住这个模型，界面层就通了；08 篇会把这里的三原语直接复用进闭环 Demo。

---

**下一模块**：[06 RAG 入门 Demo](./06-RAG%20入门%20Demo.md) | **返回总览**：[Level1 总览](./00-Level1%20基础%20Demo%20总览.md)

【参考来源】
- [Release notes - Streamlit Docs（1.59.0）](https://docs.streamlit.io/develop/quick-reference/release-notes)
- [Built-in chat primitives | Streamlit For AI](https://theneuralbase.com/streamlit-for-ai/learn/intermediate/built-in-chat-primitives/)
- [Building Chat UIs with chat_input() and chat_message()](https://theneuralbase.com/streamlit-for-ai/learn/intermediate/combined-with-chat-message/)
