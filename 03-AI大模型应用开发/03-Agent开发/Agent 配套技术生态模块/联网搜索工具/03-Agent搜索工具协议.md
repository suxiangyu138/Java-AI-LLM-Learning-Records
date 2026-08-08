# Agent 搜索工具协议：Claude / OpenAI / Gemini 原生能力

> 2025-2026 最大变化：搜索从"第三方 API 拼装"变为"模型平台的 server-side 原生工具"——模型自己决定何时搜、搜什么，结果带引用回传。本文逐一解剖三大平台。

## 1. Claude：web_search + web_fetch 工具

### 1.1 工具版本演进

| 版本 | 日期 | 关键变化 |
|---|---|---|
| `web_search_20260209` | 2026-02 | **动态过滤**（默认开启）：自动写代码过滤查询结果，只把相关内容送进上下文；`allowed_callers` 默认 `["code_execution_20260120"]` |
| `web_search_20260318` | 2026-03 | 新增 `response_inclusion` 参数 |
| `web_fetch_2026xxxx` | 配套 | 抓取工具同版本演进 |

> 💡 动态过滤基准（Sonnet 4.6 / Opus 4.6）：BrowseComp Sonnet 33.3%→46.6%、Opus 45.3%→61.6%；DeepsearchQA F1 Sonnet 52.6%→59.4%、Opus 69.8%→77.3%——**准确率平均 +11%，输入 token -24%**。

### 1.2 核心参数

| 参数 | 说明 |
|---|---|
| `max_uses` | 单请求最多搜索次数：简单查询 1-3 次，多实体研究 10+ 次 |
| `allowed_domains` / `blocked_domains` | 域名过滤；**两者同用返回 400** |
| `user_location` | 按城市/地区/国家（ISO 3166-1 alpha-2）/时区本地化 |
| `response_inclusion` | `"excluded"` 丢弃已被代码执行消费的嵌套结果块，**多步 Agent 大幅降输出 token**（默认 `"full"`） |

### 1.3 多轮与流式行为（易错点密集）

| 行为 | 规则 |
|---|---|
| `encrypted_content` | 多轮会话**必须原样回传**，否则 400 |
| 引用 | 始终启用；`cited_text`/`title`/`url` **不计 token** |
| `pause_turn` | 长搜索可返回 `stop_reason: "pause_turn"`——把助手消息原样回传继续 |
| 流式 | 搜索期间发出事件并暂停 |
| 错误 | **HTTP 200 + 体内错误对象**：`too_many_requests` / `invalid_tool_input` / `max_uses_exceeded` / `query_too_long` / `request_too_large` / `unavailable` |

> ⚠️ 服务端工具限流与模型 token 限流是**两套独立配额**——`too_many_requests` 出现在响应体内，客户端重试无效，需退避等待。

### 1.4 限制与生态

- **Amazon Bedrock 不支持 web search**；动态过滤在 Vertex AI 不可用
- 支持模型：Fable 5、Opus 4.8、Mythos 5、Opus 4.7/4.6、Sonnet 4.6
- Claude Desktop 内建 websearch MCP server（支持 Brave/Tavily/Exa/自定义后端）
- Cloudflare AI Gateway 支持 Anthropic provider 的 web search

## 2. OpenAI：Responses API 原生 web_search

### 2.1 架构要点

- **不是独立工具，是 Responses API 的 server-side 内建能力**：`tools: [{ "type": "web_search" }]`，模型原生决定何时搜索
- Codex 通过 Codex 后端端点 `chatgpt.com/backend-api/codex/responses` 调用（ChatGPT OAuth token，需 `stream: true` + `store: false`）
- 注意：直接调 `api.openai.com/v1/responses` 需要 API key（`api.responses.write` scope）——**ChatGPT OAuth token 会 401**

### 2.2 配置项

| 参数 | 说明 |
|---|---|
| `external_web_access` | 缓存（cached）还是实时（live）内容 |
| `filters` | `allowed_domains` 域名限制 |
| `user_location` | 地理位置 grounding |
| `search_context_size` | 上下文大小控制（low/medium/high） |
| `search_content_types` | 内容类型过滤 |

### 2.3 2026 生态

- **Responses Lite**（不执行 hosted 工具）：web search 路由到 Codex 自有的 standalone executor（`web.run` 工具）
- 第三方集成：hermes-agent 把 Codex 搜索作为 provider（ChatGPT Pro/Plus 用户免 API key、免搜索计费）；Pi 生态多个 npm 包封装（`pi-web-search`、`pi-codex-search`：1-32 并行查询、live/indexed/cached 新鲜度、流式进度+引用）

## 3. Google Gemini：Vertex AI Grounding

### 3.1 Grounding 类型全景

| 类型 | 用途 |
|---|---|
| Grounding with Google Search | 标准搜索 grounding |
| **EnterpriseWebSearch** | 合规向：不记录客户查询、支持 VPC Service Controls、多区域处理（US/EU） |
| **Parallel Web Search** | LLM 优化的专用索引，Vertex AI 独占（`parallelAiSearch` 工具类型） |
| Google Maps / Vertex AI Search / RAG Engine / Elasticsearch | 结构化与私有数据 grounding |

### 3.2 核心机制

- **Dynamic Retrieval**：模型每查询自判——快变事实（最新电影）才 grounding，稳定知识（法国首都）直接用内在知识——**省成本保质量**
- 返回 `grounding_metadata`：`groundingChunks`（来源标题+URL）、`groundingSupports`（内容片段→引用映射）、`web_search_queries`（模型实际用的查询）
- 内联引用：结构化元数据支持"文本段 ↔ 来源"精确映射；低相关性时**可能不返回 grounding 元数据**（即不 grounding）
- 图像搜索：`searchTypes` 支持 `web_search`/`image_search`；要求链接到含图页面而非图片直链

```python
# Gemini Grounding 最小示例
response = client.models.generate_content(
    model="gemini-3.1-pro-preview",
    contents="Where will the next FIFA World Cup be held?",
    config=types.GenerateContentConfig(
        tools=[types.Tool(google_search=types.GoogleSearch())],
    ),
)
```

### 3.3 注意事项

- Grounding 有**额外处理费用**；Parallel 启用可能静默丢弃其他内建工具（集成时验证）
- 生产用户：Quora（Poe）、Palo Alto Networks

## 4. 三方对比速查

| 维度 | Claude | OpenAI | Gemini |
|---|---|---|---|
| 接入方式 | Messages API 工具 | Responses API 内建 | generateContent + tool |
| 动态过滤/自动处理 | ✓（代码执行后处理） | 部分（context_size） | 无（需自己过滤） |
| 域名过滤 | ✓ | ✓ | Parallel 支持 include/exclude |
| 本地化 | ✓ | ✓ | ✓ |
| 合规企业版 | -（Bedrock 无） | - | **EnterpriseWebSearch** |
| 引用 | 内置，不计 token | 结构化引用 | grounding_metadata |
| 专用索引 | - | - | **Parallel Web Search** |

## 5. 选型建议

- **Claude 生态**：动态过滤 + 引用不计 token → 多步研究类 Agent 的 token 最优解
- **OpenAI 生态**：Responses 内建 + Codex 集成 → ChatGPT/Codex 用户顺滑
- **Gemini 生态**：EnterpriseWebSearch（合规）+ Parallel（专用索引）+ dynamic retrieval → 企业级/成本敏感
- 通用结论：**优先原生工具**（server-side 搜索免去自管抓取/清洗/引用），需要独立索引/特殊形态时再上第三方 API（[04](04-第三方搜索API全景.md)）

## 面试速记

1. 原生搜索 = server-side 工具：模型自决何时搜，引用回传；三方为 Claude/OpenAI/Gemini。
2. Claude 动态过滤：准确率 +11%、token -24%；`response_inclusion="excluded"` 多步降本；`pause_turn` 回传继续。
3. Claude 搜索错误是 HTTP 200 + 体内错误对象；服务端限流重试无效。
4. OpenAI：Responses `web_search` 内建；ChatGPT OAuth 只能走 codex 后端端点。
5. Gemini：dynamic retrieval（自判要不要搜）、EnterpriseWebSearch（合规）、Parallel Web Search（专用索引）。

---

**下一模块**：[04-第三方搜索 API 全景](04-第三方搜索API全景.md) ｜ **返回总览**：[00-联网搜索工具总览](00-联网搜索工具总览.md)

## 参考来源

- [Web search tool - Claude Docs](https://platform.claude.com/docs/en/agents-and-tools/tool-use/web-search-tool)
- [Improved Web Search with Dynamic Filtering（Anthropic）](https://claude.com/blog/improved-web-search-with-dynamic-filtering)
- [Reproduce Claude's agentic search benchmark scores（Anthropic Cookbook）](https://platform.claude.com/cookbook/evals-agentic-search-reproduce-agentic-search-benchmarks)
- [Grounding with Google Search - Vertex AI Docs](https://docs.cloud.google.com/vertex-ai/generative-ai/docs/grounding/grounding-with-google-search)
- [Grounding overview - Vertex AI Docs](https://docs.cloud.google.com/vertex-ai/generative-ai/docs/grounding/overview)
- [openai/codex tool_spec.rs（GitHub 源码）](https://github.com/openai/codex/blob/d36a3ead/codex-rs/tools/src/tool_spec.rs)
