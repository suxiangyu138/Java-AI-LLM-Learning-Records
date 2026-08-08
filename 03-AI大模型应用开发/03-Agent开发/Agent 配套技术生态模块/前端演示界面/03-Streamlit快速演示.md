# Streamlit 快速演示

> Python 数据应用事实标准（1.59.0，2026-07-06）：30 分钟把 Agent 逻辑变成可交互演示界面。聊天是组件，数据可视化是主场——Agent 演示选它的前提是"数据/图表比对话更重"。

## 1. 定位与适用边界

| 维度 | Streamlit 表现 | 说明 |
|---|---|---|
| 上手速度 | 极快（几十分钟出 Demo） | 纯 Python，脚本即 UI |
| 聊天能力 | ✅ 够用 | chat_input/chat_message/write_stream |
| 数据可视化 | 最强 | Pandas/Plotly/组件生态数万 |
| 生产级聊天 | ⚠️ 勉强 | rerun 模型 + session_state 需谨慎管理 |
| 前端定制 | 受限 | 布局模板化，深度定制靠 CSS 硬改 |
| 多用户隔离 | ⚠️ 需自行设计 | session_state 按会话隔离，跨会话存储要数据库 |

> 🎯 核心要点：**Streamlit 是"以数据应用为主、聊天为辅"的演示方案**。Agent 输出图表/报表为主 → 选它；Agent 以对话为主 → [05 Chainlit](05-Chainlit对话界面.md) 更对口。

## 2. 聊天 API 全家桶（v1.59 基准）

| API | 作用 | 要点 |
|---|---|---|
| `st.chat_input()` | 输入框 | 1.53 重设计；1.59 新增 `submit_mode`（提交后禁用/变停止按钮）与文件粘贴 |
| `st.chat_message()` | 角色气泡容器 | `with` 块内放任意组件（文本/图表/文件） |
| `st.session_state` | 跨 rerun 状态 | 消息历史、工具结果、会话 id 都放这里 |
| `st.write_stream()` | 流式渲染 | 1.27+；1.59 起支持 OpenAI **Responses API** 流对象 |
| `st.status()` | 过程展示容器 | 工具调用进度、思考步骤的折叠区 |
| `st.spinner()` | 临时占位 | 1.53 起视为 transient，rerun 不残留 |

```python
import streamlit as st

# 1. 会话状态：消息历史 + 工具结果
if "messages" not in st.session_state:
    st.session_state.messages = []

# 2. 渲染历史
for msg in st.session_state.messages:
    with st.chat_message(msg["role"]):
        st.markdown(msg["content"])

# 3. 输入与生成
if prompt := st.chat_input("问点什么"):
    st.session_state.messages.append({"role": "user", "content": prompt})
    with st.chat_message("user"):
        st.markdown(prompt)

    with st.chat_message("assistant"):
        # 工具调用过程：st.status 折叠展示
        with st.status("调用工具中...", expanded=False) as s:
            tool_result = agent.run_tool("web_search", prompt)
            s.write(f"web_search 完成：{tool_result['n']} 条结果")
        # 流式回答：write_stream 直接吃生成器
        resp = agent.stream_answer(prompt, tool_result)
        st.write_stream(resp)
    st.session_state.messages.append({"role": "assistant", "content": "..."})
```

## 3. 核心机制：rerun 模型

Streamlit 把脚本当"函数"，任何交互都触发**全脚本重跑**：

```text
用户输入 → 重跑脚本 → 渲染新状态 → 等待下一个输入
```

| 心智模型 | 正确姿势 | 常见坑 |
|---|---|---|
| 脚本即一次渲染 | 所有状态存 session_state | 局部变量每次重跑清零 |
| 自上而下执行 | 用 `if key not in st.session_state` 初始化 | 初始化写在渲染之后会反复覆盖 |
| 组件即函数返回值 | 把返回值立刻存进 state | 组件 id 变化导致状态丢失 |
| 多用户=多 session | 每用户一份 session_state | 跨用户共享数据要落库（SQLite/Redis） |

> ⚠️ 多用户并发演示：Streamlit 单进程多 session，**内存型缓存（st.cache_data）跨用户共享**——不要把用户私有数据放 cache；要落库用 SQLite 或外部数据库（配合本体系[数据库交互]模块）。

## 4. Agent 演示的进阶姿势

| 需求 | 方案 |
|---|---|
| 工具调用可视化 | `st.status(expanded=False)` 折叠区 + `st.toast` 通知 |
| 文件上传（RAG 输入） | `st.file_uploader` / chat_input 直接粘贴文件（1.59） |
| 引用来源 | 答案后用 `st.expander("参考来源")` + markdown 链接 |
| 停止生成 | 1.59 `submit_mode="stop"` 让输入框变停止按钮，配合生成循环检查 |
| 多页面 | 每页一个 .py，`st.Page` 导航（如"聊天页 + 数据页"） |
| 自定义路由/集成 FastAPI | 实验性 `st.App`（ASGI 入口，1.53+），挂进现有 FastAPI 应用 |

## 5. 与 Agent 后端对接的两种模式

```text
模式 A（推荐，演示期）：Agent 逻辑与 UI 同进程
   Streamlit 脚本里直接调 agent.run() / stream_answer()

模式 B（生产雏形）：UI 与 Agent 服务分离
   Streamlit 当纯前端 → 用 requests 调后端 SSE 接口 → st.write_stream(迭代器)
```

模式 B 的适配层：

```python
import requests

def sse_to_iter(url: str, payload: dict):
    with requests.post(url, json=payload, stream=True) as r:
        for line in r.iter_lines():
            if line.startswith(b"data: "):
                yield line[6:].decode()   # 交给 st.write_stream 渲染

st.write_stream(sse_to_iter("http://agent-svc:8000/api/chat", {"q": prompt}))
```

### 5.1 会话历史持久化（演示升级必需品）

session_state 只在内存，刷新即丢。要"刷新不丢 + 多用户隔离"，落库（演示用 SQLite 即可）：

```python
import sqlite3, json
from pathlib import Path

DB = Path("chat.db")

def init_db():
    DB.parent.mkdir(exist_ok=True)
    with sqlite3.connect(DB) as c:
        c.execute("""CREATE TABLE IF NOT EXISTS sessions(
            sid TEXT PRIMARY KEY, messages TEXT, updated_at TEXT)""")

def load(sid: str) -> list:
    with sqlite3.connect(DB) as c:
        row = c.execute("SELECT messages FROM sessions WHERE sid=?", (sid,)).fetchone()
    return json.loads(row[0]) if row else []

def save(sid: str, messages: list):
    with sqlite3.connect(DB) as c:
        c.execute("INSERT OR REPLACE INTO sessions VALUES(?,?,datetime('now'))",
                  (sid, json.dumps(messages)))

# 使用：sid 放 session_state，每条消息后 save(sid, messages)
```

> 💡 多用户场景直接用 PostgreSQL + 连接池（本仓库[数据库交互]模块有完整方案）；SQLite 适合单机演示。

## 6. 部署与生产注意

### 6.1 常用配置速查

| 项 | 配置/建议 |
|---|---|
| 启动 | `streamlit run app.py --server.port 8501` |
| 容器 | 官方镜像 `python:slim` + `streamlit run`；健康检查探针加 `/healthz` |
| 鉴权 | 无内置用户体系 → 前置 Nginx basic-auth / SSO 反代 |
| 上传限制 | `server.maxUploadSize`，chat_input 单文件大小可用参数覆盖（1.53+） |
| 日志 | `st.set_page_config` + Python logging；慢交互排查看 rerun 时长 |
| 规模上限 | 演示/内部工具级（数十用户）；对外产品建议换 [07 现代前端方案](07-现代前端方案：AI-SDK与React生态.md) |

常用配置速查（`~/.streamlit/config.toml` 或命令行参数）：

| 配置 | 默认 | 用途 |
|---|---|---|
| `server.port` | 8501 | 端口 |
| `server.maxUploadSize` | 200MB | 上传上限 |
| `server.enableCORS` | true | 跨域开关（挂到别站时注意） |
| `browser.gatherUsageStats` | true | 关闭遥测（`false`） |
| `client.showSidebarNavigation` | true | 多页应用侧边导航 |
| `server.headless` | false | CI/容器跑 `true` |

## 6.2 常见坑（rerun 陷阱以外的）

| 坑 | 现象 | 解法 |
|---|---|---|
| 生成期间用户再发消息 | 状态错乱、重复生成 | chat_input 提交后置 disabled，或 submit_mode="stop" 变停止按钮 |
| 工具结果放局部变量 | rerun 后工具结果消失、重复调用 | 结果写 session_state（如 `st.session_state.tool_results`） |
| 大列表整页重渲染 | 卡顿 | 用 `st.dataframe`/`st.table`（服务端分页）而非 markdown 手写表格 |
| 长回答一次性写入 | 渲染卡死 | `st.write_stream` 生成器必须边产出边 yield，不要先组装完整字符串 |
| 多线程共享模块级变量 | 用户互相串数据 | 一切按 session_state；跨会话数据走数据库 |
| st.cache_data 缓存了含隐私的对象 | 隐私泄露 | 只缓存"公共只读"数据，用户数据一律不缓存 |

## 7. 速查：一句话决策

| 你的场景 | 结论 |
|---|---|
| 验证 Agent 逻辑、给导师/同事演示 | ✅ Streamlit，30 分钟出活 |
| 大量图表报表 + 聊天 | ✅ Streamlit 主场 |
| 纯对话产品、要过程可视化与鉴权 | ❌ 换 Chainlit |
| 要品牌化对外上线 | ❌ 换 AI SDK/前端团队 |

> 🎯 核心要点：Streamlit 的价值在于**把演示期成本压到小时级**——用它验证逻辑，用结论决定是否投入前端重写。

---

**下一模块**：[04-Gradio 快速演示](04-Gradio快速演示.md)　**返回总览**：[00-前端演示界面总览](00-前端演示界面总览.md)

## 参考来源

- [Streamlit 2026 release notes（官方）](https://streamlit-docs.netlify.app/develop/quick-reference/release-notes/2026)
- [Built-in chat primitives（Streamlit for AI Course）](https://theneuralbase.com/streamlit-for-ai/learn/intermediate/built-in-chat-primitives/)
- [Building Chat UIs with chat_input() and chat_message()（Streamlit for AI Course）](https://theneuralbase.com/streamlit-for-ai/learn/intermediate/combined-with-chat-message/)
- [Streamlit LLM Chat App（OSS AI Hub Code Starters）](https://ossaihub.com/code/streamlit-llm-chat-app/)
- [Vibe code Streamlit apps with AI using AGENTS.md（Streamlit Blog）](https://blog.streamlit.io/vibe-code-streamlit-apps-with-ai-using-agents-md-04b7480f754e)
