# VLM 直读：视觉大模型解析

> 把页面当图片，让多模态模型"看一眼"直接输出结构化内容——2026 年的新范式。arXiv 实证：强多模态模型图像直读可媲美 OCR 增强。但幻觉、成本、延迟决定了它只能当"贵而准的兜底"，不是默认路径。

## 1. VLM 直读 vs 传统解析 vs OCR

| 维度 | 传统提取（PyMuPDF） | OCR（PaddleOCR-VL） | VLM 直读（Claude/GPT/Gemini） |
|---|---|---|---|
| 延迟 | 毫秒/页 | 0.3-1s/页 | 1-5s/页 |
| 成本 | 免费 | 分/页（自托管） | 最高（按 token） |
| 确定性 | 确定 | 确定 | **随机（可能幻觉）** |
| 语义理解 | 无 | 无 | 强（标题/表格/图表语义） |
| 输出 | 文本/坐标 | 结构化 Markdown | 任意结构化（按提示词） |
| 失败模式 | 静默丢内容 | 版面盲 | **编造不存在的值** |
| 可审计性 | 强 | 强 | 弱（需交叉验证） |

> 🎯 核心要点：VLM 直读的本质是"用语义能力换结构与理解"，代价是**幻觉与不可审计**——它解决"版面复杂到任何解析器都搞不定"的场景，而不是替代解析器。

## 2. 直读的正确姿势：页转图 + 结构化输出

```python
import fitz, base64, json

def page_to_image(pdf_path, page_no, dpi=200):
    doc = fitz.open(pdf_path)
    pix = doc[page_no].get_pixmap(dpi=dpi)
    return base64.b64encode(pix.tobytes("png")).decode()

# 提示词工程：明确 schema + 要求逐字转录（降低幻觉）
PROMPT = """请解析这张文档页面，输出 JSON：
{"reading_order": [...], "tables": [{"rows": [...]}], "text_blocks": [...]}
规则：1) 只能转录图中真实存在的内容，禁止补全 2) 表格按行列输出
3) 公式输出 LaTeX"""
```

| 提示词要点 | 作用 |
|---|---|
| 输出 schema 明确 | 结构化、可校验 |
| "只能转录，禁止补全" | 直接压幻觉 |
| 示例（few-shot） | 复杂版面的模仿对象 |
| 分页处理 | 单页输入，避免长上下文 |

## 3. 开源小模型：单卡直读（2026 清单）

| 模型 | 参数 | 特点 |
|---|---|---|
| GOT-OCR 2.0 | 580M | 极小，~4GB 显存，文档解析 |
| PaddleOCR-VL | 0.9B | 111 语言、版面+表格+公式一体 |
| Nemotron Parse 2.0 | 0.9B | 中日韩强（NVIDIA） |
| dots.ocr | ~1.7B | 100+ 语言 |
| olmOCR 2 | 7B | 英文优先、精度高 |

> 💡 开源小模型 + vLLM 加速 = 内网自托管 VLM 直读，成本从"按 token 计费"变成"GPU 一次投入"——合规与批量场景的可行解。

## 4. 幻觉治理：VLM 输出的四道闸

| 闸 | 做法 |
|---|---|
| 提示词约束 | "只能转录图内内容" + 结构化 schema |
| 交叉验证 | 关键字段与 OCR 结果比对（"VLM 报的金额在 OCR 文本里出现吗"） |
| 置信度路由 | 低置信度字段/整页送人工复核 |
| 日志留痕 | 每次直读存：输入页、输出、验证结果（可审计） |

```python
def verify_field(vlm_value, ocr_text):
    """交叉验证：VLM 提取的值必须能在 OCR 全文里找到"""
    return str(vlm_value) in ocr_text   # 找不到 → 疑似幻觉，转人工
```

> ⚠️ 幻觉是 VLM 直读的结构性缺陷：它"看起来对但编的"比"明显错"更危险（字段抽取场景）。凡进入业务决策的值，必须过交叉验证。

## 5. 混合路由：2026 生产共识

```text
文档/页面分类 → 分发
├── 干净数字化文本 → PyMuPDF 快路径（免费）
├── 表格重（可解析） → Docling / PaddleOCR-VL
├── 扫描/复杂版面 → PaddleOCR-VL 或小模型 VLM
└── 疑难页面（手写/超复杂/解析器失败） → 云端大模型 VLM 兜底
```

| 层 | 用途 | 单页成本 |
|---|---|---|
| 快路径 | 90%+ 页面 | ~0 |
| 专用引擎 | 表格/扫描 | 分 |
| VLM 兜底 | <5% 疑难页 | 元级 |

> 💡 路由逻辑：先跑便宜层，失败/低置信度才升级——**"先便宜后贵，逐级兜底"**让整体成本可控，同时保证最难的文档也有解。

## 6. 什么时候不该用 VLM 直读

| 场景 | 为什么不用 |
|---|---|
| 海量同模板文档 | OCR+模板区域抽取更快更便宜（10-100 倍） |
| 合规/审计要求 | 随机输出不可审计（需日志+交叉验证铺垫） |
| 纯文本数字化 PDF | PyMuPDF 免费毫秒级，VLM 纯浪费 |
| 大文档全量处理 | 图像转 token 撑爆上下文/账单爆炸 |
| 需要逐字精确转录 | 转录不是 VLM 强项，OCR 更可靠 |

## 7. 文档问答：直读的"最聪明"用法

直接问答（Document Q&A）是 VLM 的杀手锏——**跳过全量解析**，只把问题+相关页图喂给模型：

```text
用户问题 → 定位相关页（标题/关键词/向量检索）→ 该页转图 → VLM 问答
```

| 优势 | 说明 |
|---|---|
| 免全量解析 | 100 页文档只处理 2-3 页 |
| 语义理解 | "这张表 2025Q3 利润多少"直接答 |
| 引用页码 | 返回页码供溯源 |
| 成本 | 按页付费，总量可控 |

> 🎯 核心要点：VLM 直读的正确心智是**"精准狙击"而非"全面轰炸"**——全量解析交给快库与专用引擎，VLM 只处理两类页面：解析器搞不定的疑难页、用户直接提问的相关页。

## 8. 与解析流水线的整合示例

```python
def parse_with_vlm_fallback(path: str) -> dict:
    """先便宜后贵：快库 → 专用引擎 → VLM 兜底"""
    try:
        result = parse_document(path)              # 路由入口（09 篇）
        if result["status"] == "ok":
            return result
        if result["status"] == "needs_ocr":
            result = ocr_pipeline(path)            # PaddleOCR-VL
            if result["confidence"] > 0.9:
                return result
    except Exception:
        pass
    # 兜底：疑难页送云端 VLM（注意数据出境合规）
    return vlm_parse_pages(path, max_pages=10,
                           cross_verify=True)      # 交叉验证防幻觉
```

| 整合要点 | 说明 |
|---|---|
| 升级顺序 | 快库 → 专用 → VLM，每级失败才升级 |
| 失败判定 | 显式状态（ok/needs_ocr/低置信度）而非异常 |
| VLM 限量 | max_pages 限制（防账单失控） |
| 交叉验证 | 关键字段与 OCR 结果比对 |
| 合规 | 涉密文档禁止走云端 VLM |

## 9. VLM 直读常见坑

| 坑 | 现象 | 解法 |
|---|---|---|
| 幻觉字段 | 提取出文档里没有的值 | 交叉验证 + 低置信度转人工 |
| 长文档撑爆上下文 | 多页图像 token 超限 | 分页 + 只送相关页 |
| 随机输出 | 同页两次结果不同 | 结构化 schema + temperature 最低 + 重试投票 |
| 成本失控 | 全量直读账单爆炸 | 路由限量（<5% 页面） |
| 数据出境 | 涉密文档送云端 | 自托管小模型（GOT-OCR 580M 等） |
| 低 DPI 模糊 | 识别质量差 | 200-300 DPI 渲染 + 预处理 |

## 10. VLM 选型速查（云端 vs 开源）

| 场景 | 选型 | 理由 |
|---|---|---|
| 最高精度、不差钱 | 云端旗舰（Claude/GPT/Gemini） | 语义最强、复杂版面稳 |
| 内网合规、批量 | 开源小模型 + vLLM（GOT-OCR 580M / PaddleOCR-VL 0.9B） | 数据不出内网、按 GPU 计 |
| 中文文档 | PaddleOCR-VL / Nemotron Parse 2.0 | 中日韩专项强 |
| 英文为主 | olmOCR 2（7B） | 英文精度高 |
| 极简部署 | GOT-OCR 2.0（~4GB 显存） | 最小可用 |

| 评估维度 | 关注 |
|---|---|
| 幻觉率 | 交叉验证通过率（抽查 20 页） |
| 结构化输出稳定性 | 同一页两次结果一致性 |
| 延迟 | 秒/页（决定批量吞吐） |
| 显存 | 参数 × 量化（GOT-OCR 580M 可 4GB 跑） |
| 语言覆盖 | 文档语种 vs 模型支持表 |

---

**下一模块**：[07-表格与版面结构化](07-表格与版面结构化.md)　**返回总览**：[00-文档解析总览](00-文档解析总览.md)

## 参考来源

- [OCR or Not? Rethinking Document Information Extraction in the MLLMs Era（arXiv 2603.02789）](https://arxiv.org/abs/2603.02789v1)
- [Zero-Shot Local Document Parsing with Gemma 4: Treating PDFs as Images（KDnuggets）](https://www.kdnuggets.com/zero-shot-local-document-parsing-with-gemma-4-treating-pdfs-as-images)
- [Document AI vs. traditional OCR（Nutrient）](https://www.nutrient.io/blog/document-ai-vs-ocr/)
- [Nemotron Parse 2.0 vs olmOCR: Layout vs Linearize（OrcaRouter）](https://www.orcarouter.ai/blog/nemotron-parse-2-0-vs-olmocr)
- [PaddleOCR-VL 1.6（PaddlePaddle GitHub）](https://github.com/PaddlePaddle/PaddleOCR)
