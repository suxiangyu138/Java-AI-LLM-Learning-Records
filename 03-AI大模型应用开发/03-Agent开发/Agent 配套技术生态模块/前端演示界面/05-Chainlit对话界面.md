# Chainlit 对话界面

> 为对话而生的 LLM/Agent 界面框架（2.11.1，2026-04-22，社区维护）：Step 可视化推理、Element 承载多模态、Action 支持按钮交互、内置认证与数据层——"Streamlit for Chat"里最接近产品的那一个。

## 1. 定位与适用边界

| 维度 | Chainlit 表现 | 说明 |
|---|---|---|
| 对话原生性 | 最强 | 生来为聊天/Agent，ChatGPT 式体验开箱即用 |
| 过程可视化 | 原生 | Step（思考/工具调用）、Sources（引用）、Element（文件/图表） |
| 多模态输入 | 全格式 | 图片/PDF/音频/视频拖拽即传 |
| 状态/会话 | 原生 | 会话隔离、数据层持久化、多用户线程 |
| 认证 | 内置 | 密码/OAuth（含 AzureAD）/Header |
| MCP | 原生 | 2.7+ 显式开启，SSE/streamable http/stdio 三型 |
| 数据可视化 | 弱 | 文本为主，图表靠 Element 嵌入 |
| 生态现状 | 社区维护 | 2025-05 起社区主导；AI 辅助开发已引入 |

> 🎯 核心要点：**Chainlit 是 Python 阵营里唯一"以对话产品为目标"的界面框架**——Step/Element/Action 三件套专为"展示 Agent 做了什么"设计，从演示到小规模产品可平滑过渡。

## 2. 三大核心抽象：Message / Step / Element / Action

| 抽象 | 作用 | 关键参数 |
|---|---|---|
| `cl.Message` | 聊天气泡（流式可用） | `content`、`stream=True` 生成器 |
| `cl.Step` | 过程节点（思考/工具调用/子任务） | `name`、`type`（tool/llm/...）、`parent_id` 形成树 |
| `cl.Element` | 附加内容（文件/图表/图片/PDF/Dataframe） | `name`、`display="inline"` |
| `cl.Action` | 消息下方的交互按钮 | `name`、`value`、`callback` |
| `cl.ChatProfile` | 会话级配置（切换模型/提示词） | `name`、`markdown_description` |

```python
import chainlit as cl
from langchain_openai import ChatOpenAI

@cl.on_chat_start
async def start():
    cl.user_session.set("model", ChatOpenAI(model="deepseek-chat"))
    await cl.Message(content="你好！我可以联网搜索、查数据库，问点什么？").send()

@cl.on_message
async def main(message: cl.Message):
    # Step 1：思考/调用工具 → 可视化
    async with cl.Step(name="web_search", type="tool") as step:
        step.input = message.content
        result = await run_search(message.content)
        step.output = f"找到 {result['n']} 条结果"
        await step.update()

    # Step 2：流式回复
    msg = cl.Message(content="")
    async for token in agent.stream(answer_prompt(message.content, result)):
        await msg.stream_token(token)
    await msg.send()

    # Step 3：附加来源引用
    await cl.Element(name="来源", content=result["urls"], display="side").send()
```

## 3. 生命周期钩子（等价于后端路由）

| 钩子 | 触发时机 | 典型用途 |
|---|---|---|
| `@cl.on_chat_start` | 新会话开始 | 初始化模型/工具/会话变量 |
| `@cl.on_message` | 用户发消息 | Agent 主循环入口 |
| `@cl.on_chat_resume` | 恢复历史会话 | 重挂模型/上下文 |
| `@cl.on_stop` | 用户点停止 | 取消生成、清理任务 |
| `@cl.on_settings_update` | 用户改 UI 设置 | 热切换参数 |
| `@cl.password_auth_callback` | 登录（密码模式） | 用户校验 |
| `@cl.oauth_callback` | OAuth 登录 | 第三方认证 |
| `@cl.action_callback` | 点击 Action 按钮 | 按钮交互逻辑 |

## 4. Step 树与工具调用可视化

Step 支持 `parent_id` 嵌套——天然表达"工具调用→子步骤→结果"的树形过程：

```text
Step: 用户问题
└─ Step: 规划（type=llm，思考过程）
   ├─ Step: web_search（type=tool，参数→结果）
   │   └─ Step: 提取链接（type=retrieval）
   └─ Step: db_query（type=tool）
```

- 前端按树渲染，可折叠/展开，默认折叠以保持简洁（渐进式披露）。
- `type="tool"` 的 Step 自动带工具图标与状态色。
- **⚠️ 注意**：Step 会暴露思考/工具中间过程——对内演示是卖点，对外产品可能泄露内部提示词与工具参数，发布前要评估（见 [10 生产实践](10-生产实践与面试冲刺.md)）。

## 5. MCP 集成（2.7+）

```toml
# .chainlit/config.toml
[features]
enable_mcp = true
```

| 连接类型 | 配置 | 场景 |
|---|---|---|
| SSE | `mcp_servers = [{url = "http://xxx/sse"}]` | 远程工具服务器 |
| streamable http | `{url = "http://xxx/mcp"}` | 2025+ 主流传输 |
| stdio | `{command = "npx", args = ["-y", "xxx"]}` | 本地工具进程 |

集成后 MCP 工具自动进入 Agent 工具池，调用过程以 Step 可视化。**配置项从 2.7.0 起必须显式开启**，未开启时 MCP 不可用。

## 6. 认证与数据层（产品化关键）

| 能力 | 说明 |
|---|---|
| 认证三模式 | 密码（password_auth_callback）、OAuth（Google/GitHub/AzureAD 等）、Header（企业网关） |
| 数据层（Data Layer） | SQLite/PostgreSQL 持久化会话、消息、步骤；重启不丢 |
| 数据层接口 | 实现 `BaseDataLayer`（`get_user`/`get_thread`/`update_thread` 等）可接自定义存储 |
| 线程分享 | `allow_thread_sharing` 控制分享链接开关（2.9 修正开关失效 bug） |
| 多用户隔离 | 会话按用户隔离，消息归属线程；生产必须配数据层+认证 |

> ⚠️ **默认公开**：Chainlit 应用默认无需登录即可使用。内网部署也要配认证——应用是公网可访问的，任何拿到 URL 的人都能调用你的模型（烧钱 + 数据泄露）。

## 7. 部署与扩展

```bash
pip install chainlit
chainlit run app.py --host 0.0.0.0 --port 8000 --headless
# 生产：Docker 镜像 + 前置 Nginx + 数据层指向 PostgreSQL
```

| 项 | 建议 |
|---|---|
| 容器 | 官方镜像；`--headless` 无开发模式跑 |
| 反向代理 | Nginx 转发 WebSocket（Chainlit 内部用 WS 推流）→ 配置 Upgrade 头 |
| 水平扩展 | 多实例 + Redis/PG 数据层；WS 粘滞会话 |
| 多模型切换 | ChatProfile 定义多个配置，UI 一键切换 |
| 监控 | 与 Langfuse 等 LLM 可观测平台集成（走 LangChain 回调） |

## 8. 2.11 版本要点（2026-08 基准）

| 更新 | 说明 |
|---|---|
| Copilot 侧边栏 | 可拖拽缩放的侧边面板模式（=嵌入其他应用） |
| Polars 支持 | Dataframe Element 支持 Polars |
| PDF 阅读器 | 改进的 PDF 查看 |
| Lucide 图标 | Step/消息头像可用图标替代 |
| i18n | 多语言（新增 pt-PT） |
| 修复 | MCP streamable-http 错误提示、线程元数据、附件清理等 |
| 维护状态 | 社区主导（2025-05 起）；引入 AI 辅助开发 |

## 9. 速查：一句话决策

| 你的场景 | 结论 |
|---|---|
| 对话为主、要过程可视化+认证+持久化 | ✅ Chainlit，Python 里的最优解 |
| 纯内部演示、数据图表为重 | Streamlit 更顺手 |
| 对外品牌产品 | 前端团队 + [07 AI SDK](07-现代前端方案：AI-SDK与React生态.md) |

> 🎯 核心要点：Chainlit 用 **Step（过程）+ Element（内容）+ Action（交互）** 把"Agent 做了什么"变成了 UI 原生能力——这是它与 Streamlit/Gradio 的本质差异，也是"对话产品演示"场景下的默认推荐。

---

**下一模块**：[06-FastAPI+原生前端手写流式对话](06-FastAPI+原生前端手写流式对话.md)　**返回总览**：[00-前端演示界面总览](00-前端演示界面总览.md)

## 参考来源

- [Chainlit 2.11.0 Release（GitHub）](https://github.com/Chainlit/chainlit/blob/main/CHANGELOG.md)
- [Chainlit on PyPI](https://pypi.org/project/chainlit/)
- [Chainlit MCP 文档](https://docs.chainlit.io/advanced-features/mcp)
- [7 Best UI Frameworks for AI Agents (2026 Guide)（fast.io）](https://fast.io/resources/best-ui-frameworks-ai-agents/)
- [Chainlit: Python Framework for Conversational AI（DEV.co）](https://dev.co/ai/frameworks/chainlit)
