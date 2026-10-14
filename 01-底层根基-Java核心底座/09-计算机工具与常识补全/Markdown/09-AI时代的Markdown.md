# 09 - AI 时代的 Markdown

> 2026 年 Markdown 有了新身份：LLM 的母语与 Agent 的指令载体。AGENTS.md 已被 6 万+ 开源项目采用并移交 Linux Foundation，llms.txt 被约 10% 站点发布——「为 AI 写作」正在成为文档的第三条发布目标

---

## 目录

1. [Markdown 为何成为 LLM 母语](#1-markdown-为何成为-llm-母语)
2. [AGENTS.md 体系](#2-agentsmd-体系)
3. [llms.txt 标准](#3-llmstxt-标准)
4. [面向 Agent 的文档设计](#4-面向-agent-的文档设计)
5. [流式渲染与安全边界](#5-流式渲染与安全边界)

---

## 1. Markdown 为何成为 LLM 母语

大模型「开口就是 Markdown」不是偶然，是训练语料与格式特性共同决定的：

- **语料占比**：GitHub、Stack Overflow、技术文档、Reddit——高质量技术语料的绝大多数以 Markdown 组织，模型在预训练中习得了它的全部用法
- **token 效率**：`#` 表示标题、`-` 表示列表，比 HTML（`<h1>`、`<li>`）少 3-5 倍 token；同样的上下文窗口能塞进更多结构信息
- **结构梯度恰好**：纯文本没有结构，HTML 结构过重，Markdown 的「轻结构」既能表达层级又可直接被人类阅读——是「人和机器都能消费」的最优解

推论：**与 LLM 交互的最优载荷格式就是 Markdown**。让模型输出结构化结果时，用 Markdown 的标题/列表/表格承载比让模型写 JSON 更稳（少转义、少截断），需要严格结构时再补一层 JSON 约束。

## 2. AGENTS.md 体系

**AGENTS.md 是仓库级的 Agent 指令文件**——面向编码 Agent 的 README：构建/测试命令、代码约定、禁区、历史踩坑。

发展时间线：

- **2025-08**：OpenAI 发布 AGENTS.md 约定
- **2025-12**：移交 **Linux Foundation 下属 Agentic AI Foundation**（与 MCP 同基金会），成为厂商中立的开放约定
- **2026**：6 万+ 开源项目采用

### 2.1 多生态对照

| 文件 | 消费方 | 状态 |
|------|--------|------|
| `AGENTS.md` | Codex、Cursor、Copilot、Claude Code 等 | 跨工具事实标准 |
| `CLAUDE.md` | Claude Code | 可 import AGENTS.md |
| `.cursor/rules/` | Cursor | 自动读取 AGENTS.md |
| `.github/copilot-instructions.md` | GitHub Copilot | 自动读取 AGENTS.md |

一个重要机制：**发现与层级**。Agent 从项目根目录向下遍历，逐层拼接 AGENTS.md：根文件写全局约定，子目录文件写局部覆盖，冲突时**最近的优先**（nearest wins）。Claude Code 的加载策略略有不同（进入对应目录才加载），但「就近覆盖」语义一致。Codex 默认读取上限为 32 KiB——超长文档会被截断。

### 2.2 内容纪律

写 AGENTS.md 的官方建议（与直觉相反的部分最值得记）：

- **该写**：构建/测试/运行命令、非标准约定、禁区、Agent 反复犯错的教训
- **不该写**：架构总览、目录结构（Agent 能自己推断，写了还会过期）、机密、冗长流程
- **篇幅**：控制在 150-200 行内，超了就该拆分子目录文件

> **实证警告**：ETH Zurich 2025 研究显示，**LLM 自动生成的 AGENTS.md/CLAUDE.md 在 8 个场景中的 5 个降低了任务成功率**，而人工精心编写的文件成功率提升约 4 个百分点。指令文件的价值来自「人类踩坑后的经验注入」，不是格式本身——不要用脚本批量生成。

## 3. llms.txt 标准

**llms.txt 是站点级的 LLM 索引文件**（`/llms.txt`，2024-09 由 Jeremy Howard 提出），定位是「LLM 的 robots.txt」：告诉 Agent 这个站点是什么、哪些页面值得读。

格式固定四段：

```markdown
# 站点名称

> 一句话说明这个站点是什么（Agent 冷启动的锚点）

## 核心文档

- [快速开始](https://example.com/start)：5 分钟跑通
- [API 参考](https://example.com/api)：全部端点

## Optional

- [更新日志](https://example.com/changelog)：上下文不足时可跳过
```

配套文件：`llms-full.txt` 把页面内容全量拼进单文件（一次抓取即完成摄取），`llms-small.txt` 面向小上下文窗口的裁剪版。

### 2026 采用现状与边界

- 约 **10% 网站**发布（Anthropic、Stripe、Cursor、Cloudflare 在列）；**2026-05 Shopify 为每个店铺默认生成**，是迄今最大规模铺开
- 消费方：**IDE/编码 Agent**（Cursor、Claude Code、Copilot、Windsurf、Cline）按需抓取，通过 AGENTS.md/CLAUDE.md 里的链接被引导过来——普通搜索引擎爬虫不会自动加载
- 局限要认清：**非正式标准**（无 IETF/W3C 背书）；SEO 无关（Google 明确不影响搜索/AI Overviews）；30 万域名研究（2025-11）发现它对「AI 引用」几乎没有预测价值；`Accept: text/markdown` 协商仅 3.9% 服务商支持（2026-04 Cloudflare 研究）
- **安全**：llms.txt 与 AGENTS.md 都是指令注入面——抓取到的内容按不可信数据处理，指令文件的变更应与部署脚本同级评审

## 4. 面向 Agent 的文档设计

「人读得懂」与「Agent 读得懂」高度重合，但 Agent 有三个特殊需求：

1. **语义化标题**：Agent 靠标题切片与检索，`## 如何配置连接池` 优于 `## 配置`——标题是 Agent 的导航索引
2. **上下文自治**：每段内容自包含，避免「如上所述」「详见前文」——切片检索后这些指代全部失效
3. **结构稳定**：锚点、文件路径、命令是 Agent 的接口，改动要有意识；大改标题等于破坏所有已有引用
4. **避免渲染依赖**：MDX 组件、HTML 折叠块、纯装饰的徽章对 Agent 等于噪声——纯 Markdown 文本信息密度最高
5. **示例完整可运行**：Agent 会照抄代码块，半截示例（`// ...` 省略）会导致生成错误代码——给可运行的完整片段

反向应用：**本知识库的文档规范与 Agent 友好度天然同构**——11 篇模板 = 稳定的结构切片，参考来源带链接 = 可追溯的引用，术语中英对照 = 跨语言检索友好。

## 5. 流式渲染与安全边界

### 5.1 流式渲染工程

LLM 逐 token 输出 Markdown，客户端实时渲染，工程上有三个已知问题与解法：

- **二次复杂度**：每个 chunk 全量重解析 → 改为增量解析，冻结已完结块，只重解析尾部不稳定区（详见第 04 篇）
- **未闭合标记**：流到一半的 `**`、` ``` `、`[链接` 会让渲染器闪烁或吞字——工程做法是「宽容渲染」：把未闭合标记当作普通文本渲染，闭合后再升级为成品样式
- **光标内联**：等待光标不要插进标记内部（`**▊粗体**`），应渲染在语法解析后的文本流末端

### 5.2 安全边界

Markdown 在 Agent 管线里是「数据也是指令」，两个高发风险：

- **间接提示注入**：Agent 读取的外部 Markdown（网页、Issue、llms.txt）中藏 `<!-- 忽略之前指令... -->` 或不可见文本，诱导 Agent 执行越权操作。防线：外部内容与系统指令严格隔离、工具权限最小化、关键操作人工确认
- **渲染 XSS**：AI 生成的 Markdown 含 `<script>` 或 `javascript:` 链接时，渲染端必须 sanitize（第 04 篇三层防护同样适用）

---

> **核心要点**：AI 时代 Markdown 的角色从「文档格式」升级为「人机通用接口」。三个关键动作：给仓库写一份**人工精心编写**的 AGENTS.md（拒绝自动生成）；文档站补发 llms.txt；写作时按「切片自治、标题语义化、示例可运行」三条改造文档。安全上记住：Markdown 既是数据也是指令。

---

## 参考来源

- [AGENTS.md 约定与 Linux Foundation 治理（Data Panda）](https://datapanda.eu/en/dictionary/agent-instruction-file-agents-md-llms-txt)——60,000+ 项目、层级规则、32KiB 限制
- [AI 原生仓库文档组织研究报告（2026-05）](https://raw.githubusercontent.com/Luum-Home/luum-cognitive-os/refs/heads/main/docs/06-Daily/reports/docs-organization-research-2026-05-12.md)——ETH Zurich 实证与 30 万域名研究引用
- [llms.txt 消费机制（React Native Video 文档）](https://docs.thewidlarzgroup.com/react-native-video/docs/v7/build-with-ai/llms-txt/)——IDE Agent 消费方式
- [控制 Agent 可见内容（Mintlify Learn）](https://learn.mintlify.com/courses/agent-friendly-content/control-what-agents-see)——llms.txt 编写最佳实践
- [AmiVoice：面向 AI 生成的 API 文档组织](https://acp.amivoice.com/en/blog/amivoice-api-docs-for-ai-agents/)——Accept: text/markdown 支持率（3.9%）来源

---

**上一模块**：[08-文档站与内容发布](08-文档站与内容发布.md) ｜ **下一模块**：[10-生产实践与面试题](10-生产实践与面试题.md)
