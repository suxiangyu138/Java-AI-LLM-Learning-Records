# Agent 文档解析工具链

> 把解析能力封装成 Agent 工具的正确姿势：路由式 parse 工具、显式错误处理（扫描/加密/损坏）、缓存与审计。文档工具链的成败 = 错误处理质量，而非引擎选择。

## 1. 工具设计总原则

| 原则 | 说明 |
|---|---|
| 一个工具一件事 | parse_pdf / extract_table / ocr_page 分离，别做万能解析器 |
| 先路由后解析 | 文档类型判断是第一步（有文本层？扫描？表格重？） |
| 显式失败 | 扫描 PDF 返回空 → 报"需 OCR 路由"而非静默空文本 |
| 结果带元数据 | 页码/heading path/来源随结果返回 |
| 可审计 | 每次解析记录：引擎、耗时、页数、结果大小 |

> 🎯 核心要点：**文档工具链的第一 bug 永远是"静默丢内容"**——扫描页返回空文本、加密文档报错、超大文档截断，都必须显式抛出可路由的错误，让 Agent 换路而非假装成功。

## 2. 工具集设计（四件套 + 路由）

```text
parse_document（路由入口）
  ├─ 检测：扩展名 + 文本层探测（PyMuPDF 提取 3 行试读）
  ├─ 分发：
  │   数字化文本 → fast_extract（pymupdf4llm）
  │   表格重     → docling_parse
  │   扫描件     → ocr_page（PaddleOCR-VL）
  │   疑难页     → vlm_read（云端大模型，<5% 兜底）
  └─ 返回：Markdown/JSON + 元数据

辅助工具：
  extract_table(doc_id, page)    单表结构化（TEDS 高引擎）
  ocr_page(page_image)           单页 OCR
  doc_question(page, question)   VLM 直读问答（06 篇 7 节）
```

```python
def parse_document(path: str) -> dict:
    if not has_text_layer(path):
        # 显式失败：不给模型"空文档"的错觉
        return {"status": "needs_ocr", "hint": "请调用 ocr_page 逐页识别",
                "pages": page_count(path)}
    if table_density(path) > 0.3:        # 表格面积占比 >30%
        return docling_parse(path)
    return fast_extract(path)
```

## 3. 错误处理矩阵：每类失败都有"下一步"

| 失败 | 检测 | Agent 下一步 |
|---|---|---|
| 扫描 PDF 无文本层 | 试读为空/过短 | 路由 OCR 工具 |
| 加密 PDF | 提取抛异常 | 问用户要密码 / 声明无法处理 |
| 损坏文件 | 解析异常 | 报告文件损坏，不重试 |
| 超大文档（>500 页） | 页数探测 | 分页流式 + 提示范围 |
| 低置信度 OCR | 置信度阈值 | 局部重试 / 转人工 |
| 解析结果过短 | 与页数比例校验 | 换引擎（升级路由） |
| 引擎超时 | 超时控制 | 降级到备用引擎 |

> 💡 错误信息必须"可行动"：告诉 Agent 该调用哪个工具、下一步做什么——而不是抛出原始异常。这是 2026 年工具工程（含[数据库交互](..%2F数据库交互%2F00-数据库交互总览.md) 04 篇）的统一手法。

## 4. 路由分类器：怎么判断"走哪条路"

```text
规则优先（零成本）：
  扩展名 → pdf/docx/xlsx/png...
  文本层探测 → fitz 试读前 3 页
  表格密度 → 版面分析/启发式
机器学习（可选）：
  页面分类模型（文本页/表格页/图像页）→ 精度更高
```

| 层 | 成本 | 精度 |
|---|---|---|
| 扩展名+试读 | 0 | 粗分够用 |
| 版面分析密度 | 低 | 表格页识别 |
| 页面分类模型 | 中 | 最准 |

> 💡 大多数生产系统用"扩展名+文本层试读+表格密度启发式"就够——路由的目的是"别把扫描件送进快库、别把文本送进 OCR"，不是完美分类。

## 5. MCP 文档服务器（生态接入）

| 现有方案 | 能力 |
|---|---|
| MCP 文档解析服务器（社区） | parse_pdf/extract_text 等工具 |
| 商用 API 的 MCP 封装 | TextIn/百度/阿里 文档解析的 MCP 暴露 |
| 自研封装 | 把 2 节工具集注册为 MCP Server，供所有客户端复用 |

```toml
# MCP 配置示例（Claude Code 接入自研文档解析服务）
[mcp_servers.docs]
command = "python"
args = ["-m", "my_doc_mcp_server"]
```

> 💡 与[数据库交互](..%2F数据库交互%2F00-数据库交互总览.md) 08 篇同理：**自研工具链是深度定制的主体，MCP 是标准化接入的插座**——先把工具链做对，再考虑暴露为 MCP。

## 6. 缓存与幂等

| 项 | 做法 | 收益 |
|---|---|---|
| 解析结果缓存 | 文件哈希 → 缓存 Markdown/JSON | 同文档不重复解析（贵引擎尤其） |
| 增量入库 | 只解析变更文档 | 大批量迭代省 90%+ |
| 幂等入库 | 文档哈希作为唯一键 | 重复上传不重复入库 |
| OCR 缓存 | 页图像哈希 | 跨任务复用 |

```python
import hashlib

def parse_cached(path: str) -> dict:
    h = hashlib.sha256(Path(path).read_bytes()).hexdigest()
    if cached := cache_get(h):
        return cached                     # 命中缓存：秒回
    result = parse_document(path)         # 未命中：全链路解析
    cache_set(h, result, ttl=7 * 86400)   # 一周 TTL
    return result
```

## 7. 安全与合规

| 风险 | 防线 |
|---|---|
| 恶意文档（解析器漏洞） | 解析在沙箱/隔离进程执行（衔接[代码执行沙盒](../代码执行沙盒/00-代码执行沙盒总览.md)） |
| 文档含敏感数据 | 入库前脱敏（PII 扫描）；解析日志不落全文 |
| 上传限制 | 大小/类型白名单；页数上限 |
| 外部 API 数据出境 | 涉密文档禁止送云端 VLM/商用 API |
| 提示注入（文档内指令） | 解析结果与指令分域（RAG 内容不执行）；见[幻觉、格式错误、安全、token 超限](../../幻觉、格式错误、安全、token%20超限/00-Agent四大失败模式知识体系总览.md)体系 |

## 8. 工具链自检清单

```text
□ 路由入口：扩展名+文本层试读+表格密度启发式
□ 扫描件显式路由 OCR，绝不静默空文本
□ 每类失败都有"可行动的下一步"
□ 结果带元数据（页码/heading path/来源/引擎）
□ 解析缓存（文件哈希）+ 幂等入库
□ 引擎超时与降级链（快→准→VLM）
□ 日志审计：引擎/耗时/页数/结果大小
□ 沙箱执行解析；敏感文档不出内网
□ 文档级提示注入有防护
```

> 🎯 核心要点：Agent 文档工具链的成熟标志不是"引擎多先进"，而是**"任何输入都能给出可行动的结果"**——能路由、会失败、有缓存、留审计，四者齐备，Agent 才敢把文档交给工具链。

## 9. 工具链常见坑速查

| 坑 | 现象 | 解法 |
|---|---|---|
| 扫描件静默空文本 | Agent 答"资料里没有" | 文本层试读 + needs_ocr 状态 |
| 引擎异常直接抛 | Agent 卡死重试 | 捕获 → 降级链（快→准→VLM） |
| 无缓存重复解析 | 贵引擎反复烧钱 | 文件哈希缓存 |
| 工具描述含糊 | 模型乱选工具 | 描述写明输入输出与失败条件 |
| 超时无降级 | 大文档挂死 | 页数上限 + 流式分页 |
| 结果无元数据 | 无法溯源 | 返回页码/heading_path |
| 解析器裸奔 | 恶意文档漏洞 | 沙箱/隔离进程 |
| 并发无限制 | 资源耗尽 | 信号量限流（同任务 ≤2 并发） |
| 无审计 | 出问题查不到 | 日志：引擎/耗时/页数/大小 |

## 10. 端到端流程示例：文档进入 Agent 上下文

```text
用户上传 合同.pdf
  → parse_document：文本层探测 ✅ → 表格密度 0.1（低）→ fast_extract
  → 输出：Markdown + 元数据（页码/来源/引擎=pymupdf4llm）→ 缓存
  → 分块：标题感知 + 表格原子块 + heading_path
  → 向量库入库（幂等：文档哈希唯一键）
  → 用户提问："合同违约金比例？"
  → 检索命中第 5 块 → 注入上下文 → 回答 + 引用[第 5 页]
  → 全程审计：解析耗时 0.8s、4 页、缓存命中
```

## 11. 工具链监控指标

| 指标 | 信号 |
|---|---|
| 路由分布（各引擎占比） | 快路径应占 80%+；VLM 占比过高=路由失效 |
| 解析失败率 | 升高 → 上游文档质量/引擎问题 |
| 静默空文本率 | 任何非 0 都要查（最危险的错误） |
| 缓存命中率 | 升高 = 好（重复文档多） |
| 平均耗时（s/页） | 超基线 → 引擎退化或文档变复杂 |
| 表格结构异常率 | TEDS 抽检下降 → 引擎升级回退 |
| VLM 幻觉拦截率 | 交叉验证拦截比例 → 决定是否降级路由 |
| 工具描述与调用匹配率 | 模型选错工具比例 → 描述待改进 |

## 12. 与数据库交互的联动

文档解析与[数据库交互](../数据库交互/00-数据库交互总览.md)体系是 Agent"数据入口"的两端：

| 联动点 | 说明 |
|---|---|
| 表格 → 入库 | 解析出的表格 JSON → 结构化入库（数据库交互 07 篇） |
| 文档 → 记忆 | 解析结果分块 → 向量库/claim 表（数据库交互 05 篇） |
| 元数据 → 检索过滤 | 文档元数据（类型/时间）→ 数据库查询条件 |
| 审计 → 一体化 | 解析审计与查询审计共用请求 id 链路 |

> 💡 工程上建议：文档解析输出遵循"统一契约"（07 篇表格 schema），让数据库交互层零适配消费——两个模块的接口契约比实现更重要。

---

**下一模块**：[10-生产实践与面试冲刺](10-生产实践与面试冲刺.md)　**返回总览**：[00-文档解析总览](00-文档解析总览.md)

## 参考来源

- [Best PDF Parsing Tools for RAG in 2026（fast.io）](https://fast.io/resources/best-pdf-parsing-tools-rag/)
- [Which PDF extractor should you use?（pdfmux）](https://pdfmux.com/blog/which-pdf-extractor-should-you-use/)
- [Document AI: From OCR to Agentic Doc Extraction（DeepLearning.AI）](https://learn.deeplearning.ai/courses/document-ai-from-ocr-to-agentic-doc-extraction/)
- [Best AI Document Parsers for Developers（LlamaIndex）](https://www.llamaindex.cloud/insights/best-ai-document-parsers)
