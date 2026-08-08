# 第三方搜索 API 全景

> Agent 搜索的另一条路：第三方 API。2026 市场剧变（Bing API 退役、Tavily 被收购、Google 起诉 SerpApi）重塑了格局——按"输出形态"选型而非按厂商。

## 1. 2026 市场剧变（三件大事）

| 事件 | 时间 | 影响 |
|---|---|---|
| **Bing Search API 退役** | 2025-08-11 | 由 Azure AI Foundry "Grounding with Bing" 替代（~$35/千次）；老牌渠道消失 |
| **Tavily 被 Nebius 收购** | 2026-02-10 | ~$2.75 亿；路线图或向企业级倾斜 |
| **Google 起诉 SerpApi（DMCA）** | 2025-12 | 爬取型 SERP API 的合法性蒙上阴影 |

**两大阵营分化**：

```text
Agent 导向 API（Tavily/Exa）：返回干净的、排好序的、LLM 友好文本（含摘要/引用）
SERP 爬取 API（Serper/SerpApi）：返回 Google 原始结果 JSON，自己处理
```

## 2. 主流 API 对比（2026 基准）

| API | 阵营 | 核心卖点 | 价格（/千次） | 免费额度 | MCP |
|---|---|---|---|---|---|
| **Tavily** | Agent 导向 | 官方插件全生态（LangChain/CrewAI/OpenAI/Anthropic）、一次调用含摘要+相关性分数+可选原文、`search_depth` 可调 | ~$4-8 | 1000 积分/月 | 官方 |
| **Exa** | Agent 导向（语义） | Embedding 语义搜索、`/findSimilar` 概念发现、搜索+提取一次调用、Websets | ~$7（带内容） | 1000 请求/月 | 官方 |
| **Brave** | 独立索引 | **真正独立索引（~300 亿页）**、不依赖 Google/Bing、Goggles 重排、`extra_snippets`、2026 基准最高准确率与最低延迟 | $5 | $5 额度/月（~1000 查询） | 社区 |
| **Serper** | SERP 爬取 | Google 结果 JSON、10 个垂直（web/news/images/maps/places/shopping/scholar）、最便宜 | ~$0.30-1.0 | 2500 次（一次性） | 社区 |
| **SerpApi** | SERP 爬取 | Google 全面（含知识面板）、法律风险（被 Google 起诉） | 按量 | 100 次/月 | 社区 |

### 各家关键数字

| API | 延迟 | 2026 基准成绩 | 备注 |
|---|---|---|---|
| Tavily | basic ~998ms / advanced 2-3s | DeepResearch Bench RACE **52.44（第一）** | 结果与 Google 高度重叠（聚合索引非独立） |
| Exa | ~1.5s（neural） | 事实查询准确率 8.7（AIMultiple，低于 Brave 14.89） | 概念发现强、精确事实弱；免费额度耗尽后 429 很硬 |
| Brave | **669ms（Firecrawl 基准最低）** | agentic 基准 **14.89（最高）** | 长尾查询有洞、JS 重页面提取质量差 |

## 3. 深度对比：Tavily vs Exa（Agent 导向双雄）

| 维度 | Tavily | Exa |
|---|---|---|
| 检索哲学 | 关键词 + 聚合索引（与 Google 重叠大） | **Embedding 语义/神经检索** |
| 端点 | `/search` `/extract` `/crawl` `/map` `/research` | `/contents` `/answer` `/findSimilar` `/research`（async）/Websets |
| 结果形态 | LLM-ready 摘要 + 相关性分数（可编程过滤） | 干净解析文本 + 高亮提取 |
| 最强场景 | RAG/Agent 默认上下文 | 研究/竞品分析/概念相似检索/RAG 多样性 |
| 最弱场景 | 需要独立索引来源 | 精确事实查询、实时新闻 |
| 风险 | 后端不透明；被 Nebius 收购路线未定 | 免费层 429 硬限制；延迟偏高 |

## 4. 开源自建：SearXNG + 生态

- **SearXNG**：自托管元搜索，聚合 **70+ 引擎**（Google/Bing/DuckDuckGo/学术/新闻…），隐私优先，无云 API 依赖
- 前置要求：`settings.yml` 开启 JSON 格式（`search.formats`，常默认关闭）
- **MCP 生态活跃**（2025-2026 持续更新）：

| 项目 | 特点 |
|---|---|
| TadMSTR/searxng-mcp | ML 重排 + 4 级抓取级联（Firecrawl/Crawl4AI/raw/Wayback）+ 每域学习 + Ollama 查询扩展 + Valkey 缓存，**全无云 API** |
| 88plug/searxng-mcp | token 高效（紧凑输出 + `_meta` 藏全量）；工具 `search`/`search_many`（并行扇出）/`search_and_fetch`/`research`；Playwright 渲染提取 JS 页 |
| magnus919/mcp-searxng | 最大 API 兼容：完整 JSON（results/infoboxes/answers/suggestions/corrections）；支持 `site:`/`!bang`/`lang` 语法 |
| BrandonStudio/SearXNG-MCP | stdio / streamable HTTP / **Cloudflare Workers** 三种传输 |
| codeprimate/searxng_docker | Docker 全家桶：SearXNG + Redis 缓存 + MCP + LLM 结构化提取侧车 |

- 客户端兼容：Claude Desktop、Claude Code、Cursor、Copilot、Open WebUI、Cherry Studio 等
- 安全建议：实例保持私网或 IP 白名单（防被滥用当公共代理）

## 5. 组合策略（业界共识）

1. **分离搜索与提取**：搜索（Brave/Serper 便宜）+ 提取（Firecrawl/Jina Reader）组合，常强于一站式 API
2. **分层供应商**：廉价先打（Brave/Tavily 免费层）→ 结果弱再升级（Exa 语义/深度）
3. **生产至少两家**：索引、限流、失败模式互不相同——一家挂了另一家兜底
4. **按"成功 grounded 答案的总成本"算账**：便宜的链接级 SERP + 自建爬虫往往比贵的一站式更贵

## 6. 选型速查

```text
要 LLM-ready 默认上下文     → Tavily（生态最全）
要语义/概念相似检索          → Exa（findSimilar/研究）
要独立索引/隐私/最低延迟     → Brave（+ 单独提取层）
要海量 Google 结果最便宜     → Serper（有提取层时）
要合规/不出内网              → SearXNG 自建（+ MCP server）
```

## 面试速记

1. 2026 三件事：Bing API 退役（→Azure Grounding）、Tavily 被 Nebius 收购、Google 诉 SerpApi——爬取型 SERP API 有法律风险。
2. 两大阵营：Agent 导向（Tavily/Exa，LLM-ready 输出）vs SERP 爬取（Serper/SerpApi，原始 JSON）。
3. Tavily 默认上下文、Exa 语义发现、Brave 独立索引+最快、Serper 最便宜。
4. 组合共识：search 与 extract 分离、分层供应商、生产双供应商、按总成本算账。
5. SearXNG 聚合 70+ 引擎自建，MCP 生态活跃；实例须私网隔离。

---

**下一模块**：[05-搜索工具集成实战](05-搜索工具集成实战.md) ｜ **返回总览**：[00-联网搜索工具总览](00-联网搜索工具总览.md)

## 参考来源

- [Exa vs Tavily vs Serper vs Brave Search for AI Agents（rhumb）](https://rhumb.dev/blog/exa-vs-tavily-vs-serper-vs-brave-search)
- [Best AI Search APIs for Agents 2026（apiscout）](https://apiscout.dev/guides/best-ai-search-apis-2026)
- [2026 年给 AI Agent 用的最佳网页搜索 API（apipick）](https://www.apipick.com/zh-CN/blog/best-web-search-apis-for-ai-agents-2026)
- [Best Web Search API for AI Agents & RAG（apiserpent）](https://apiserpent.com/blog/best-web-search-api-ai-agents-rag)
- [Best Search Tools for AI Agents in 2026（Firecrawl）](https://www.firecrawl.dev/blog/best-search-tools-for-agents)
- [TadMSTR/searxng-mcp（GitHub）](https://github.com/TadMSTR/searxng-mcp)
- [88plug/searxng-mcp（GitHub）](https://github.com/88plug/searxng-mcp)
