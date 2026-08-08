# OCR：扫描件与图像文档

> 扫描件是 Agent 文档输入的"最后一公里"：传统 OCR 已被"OCR+版面分析一体化"取代，2026 年 SOTA 是 PaddleOCR-VL-1.6（OmniDocBench 96.33% 全球第一）。本章讲透选型与实战。

## 1. OCR 能力演进：从"认字"到"懂版面"

```text
V1 传统 OCR（Tesseract 等）：只认字，输出坐标+文本
  → V2 版面分析+OCR（PP-Structure）：先分版面再认字
    → V3 端到端多模态解析（PaddleOCR-VL）：版面+文字+表格+公式一次出
```

| 代际 | 代表 | 输出 | 局限 |
|---|---|---|---|
| V1 | Tesseract/EasyOCR | 文本+框 | 版面盲、顺序乱、表格毁 |
| V2 | PP-StructureV3 | 结构化元素 | 需多模型串联 |
| V3 | PaddleOCR-VL-1.6 | Markdown/JSON 全结构化 | 需 GPU（0.9B 模型） |

> 🎯 核心要点：**2026 年别再用"纯 OCR"做文档**——V3 模型把版面分析+文字+表格+公式一体输出，质量与成本都优于"传统 OCR + 自己拼版面"。

## 2. PaddleOCR-VL-1.6（2026 SOTA，开源）

| 维度 | 数据 |
|---|---|
| 参数 | 0.9B（轻量，单卡可部署） |
| 基准 | OmniDocBench 96.33%（全球第一，超 Gemini-3 Pro/DeepSeek-OCR2/GPT-5.2） |
| 语言 | 111 种（含中英日韩阿俄，手写/竖排/艺术字） |
| 架构 | 两阶段：版面分析（PP-DocLayoutV2/V3）→ 元素识别（VL-0.9B） |
| 能力 | 表格结构+Markdown 输出、跨页表格自动合并、公式转 LaTeX、古籍/印章/生僻字 |
| 部署 | 需 PaddlePaddle 3.2.1+、`paddleocr[doc-parser]>=3.6.0`、vLLM 加速 |

```bash
# CLI 一行解析（输出 Markdown + 结构化 JSON）
paddleocr doc_parser --pipeline_version v1.6 --input_path scan.pdf --output_dir out/
```

```python
from paddleocr import PaddleOCRVL

engine = PaddleOCRVL()                     # 0.9B 多模态解析模型
result = engine.predict("scan.pdf")        # 版面+表格+公式全结构化
print(result.to_markdown())                # 直接可用 Markdown
```

## 3. 传统 OCR 的定位（Tesseract 等）

| 场景 | 是否还用 Tesseract |
|---|---|
| 海量同模板表单（10 万份/月） | 是——便宜快，配合模板区域抽取 |
| 纯英文印刷体、无版面要求 | 可以 |
| 复杂中文文档/表格/公式 | 否——直接 PaddleOCR-VL |
| 需要版面与表格结构 | 否 |

```python
import pytesseract
from PIL import Image

text = pytesseract.image_to_string(Image.open("scan.png"), lang="chi_sim+eng")
# 注意：输出纯文本流，无版面无表格结构——仅适合"只要可搜索文本"的场景
```

> ⚠️ Tesseract 是版面盲的（layout-blind）：它不知道标题和正文的区别、会把表格摊成乱字。凡是下游要"结构"的场景，传统 OCR 都不合格。

## 4. 版面分析：结构化输出的地基

| 元素类型 | PP-DocLayoutV2/V3 识别 | 下游用途 |
|---|---|---|
| 标题（doc_title/heading） | 层级与顺序 | 分块边界（08 篇） |
| 段落（text） | 阅读顺序 | 正文分块 |
| 表格（table） | 行列结构 | 表格入库 |
| 图表（chart/figure） | 区域+建议描述 | 图表问答 |
| 公式（display_formula） | LaTeX 输出 | 数学检索 |
| 页眉页脚/印章 | 识别并剔除 | 去噪音 |
| 竖排文本/多边形框 | 倾斜/扭曲文档 | 拍照文档兜底 |

> 💡 版面分析的准确率直接决定分块质量——**"标题认错了"比"字认错了"更伤 RAG**（分块边界全错），这就是为什么 2026 年选型要版面分析一体化模型，而不是传统 OCR。

## 5. 公式识别

| 方式 | 工具 | 质量 |
|---|---|---|
| 文档级 | Docling（PDF 内公式） | 好 |
| 扫描/图像 | PaddleOCR-VL（公式→LaTeX） | 2026 SOTA |
| 传统 | 公式专用模型（pix2tex 等） | 中 |

论文/教材场景：**公式必须转 LaTeX**——纯 OCR 会把公式变成一行乱码，RAG 检索与引用全部失效。

## 6. OCR 质量治理

| 问题 | 手段 |
|---|---|
| 低分辨率 | 预处理：放大/二值化/去噪（PaddleOCR-VL 自带优化） |
| 倾斜/扭曲 | 检测后旋转/透视校正（不规则框检测是 VL-1.6 卖点） |
| 手写 | 用手写优化模型/提示词；低置信度转人工 |
| 印章遮挡 | 版面分析识别印章区域，正文不受影响 |
| 置信度 | 输出带置信度，低于阈值路由人工复核 |
| 校对 | 术语表后处理（领域词典纠错，如"LangChain4j"不拆词） |

## 7. 成本与部署

| 方案 | 成本 | 适合 |
|---|---|---|
| 百度云 API（文档解析服务） | 免费 1000 页测试、后按量 | 快速验证、无 GPU |
| 自托管 PaddleOCR-VL | GPU 一次投入，长期近乎 0 | 数据不出内网、批量大 |
| 传统 OCR（CPU） | 最低 | 海量同模板、只求可搜索 |

```text
部署决策：数据合规 > 批量 > 成本
  涉密/合规 → 自托管（PaddleOCR-VL 单卡）
  演示/小批量 → 云 API
  超大批量同模板 → 传统 OCR + 模板
```

> 🎯 核心要点：扫描件解析在 2026 年只有两条路——**PaddleOCR-VL（准、贵、要 GPU）或传统 OCR（便宜、版面盲）**；选择依据是"下游要不要结构"：要结构走前者，只求可搜索走后者的模板化玩法。

## 8. 中文 vs 英文：OCR 差异与对策

| 维度 | 中文 | 英文 |
|---|---|---|
| 语言支持 | 111 语言覆盖（PaddleOCR-VL 天然优势） | 传统 OCR 成熟（Tesseract） |
| 识别难点 | 字形多、易混淆字、竖排/古籍 | 单词连写、装饰字体 |
| 表格 | 行列密集、合并多 | 结构相对规整 |
| 公式 | 常见于论文/教材 | 同左 |
| 推荐 | PaddleOCR-VL（中文 SOTA） | PaddleOCR-VL 或 Tesseract（低成本） |

> 💡 中文文档的 OCR 结论很明确：**别折腾 Tesseract 中文模型**（需 chi_sim 包、长文本效果一般），PaddleOCR-VL 在中文+表格+公式上都是压倒性优势，且开源免费。

## 9. 自建 OCR 质量测试集

| 步骤 | 做法 |
|---|---|
| 取样 | 从真实文档抽 20-50 页（覆盖：印刷/手写/表格/倾斜/低分辨率）；**测试集必须来自真实业务文档而非公开样例**——公开样例达标不等于你的文档达标 |
| 标注 | 每页人工转录为基准文本 |
| 标注 | 每页人工转录为基准文本 |
| 指标 | CER（字符错误率）+ 表格 TEDS + 版面元素命中率 |
| 回归 | 每次换模型/参数跑一次，对比基线的提升/回退 |
| 阈值 | 设定"可接受 CER"（如 <5%），超阈值该路由人工复核 |

---

**下一模块**：[06-VLM 直读：视觉大模型解析](06-VLM直读：视觉大模型解析.md)　**返回总览**：[00-文档解析总览](00-文档解析总览.md)

## 参考来源

- [PaddleOCR-VL 1.6 发布（百度 AI）](https://ai.baidu.com/support/news?action=detail&id=3270)
- [96.33% 新 SOTA！PaddleOCR-VL-1.6（阿里云开发者社区）](https://developer.aliyun.com/article/1739500)
- [PaddleOCR-VL 文档解析（百度 AI Cloud）](https://ai.baidu.com/ai-doc/OCR/Qmncwhwdt)
- [PP-StructureV3（PaddleOCR GitHub）](https://github.com/PaddlePaddle/PaddleOCR/blob/c1664488/docs/version3.x/pipeline_usage/PP-StructureV3.en.md)
- [Document AI: From OCR to Agentic Doc Extraction（DeepLearning.AI）](https://learn.deeplearning.ai/courses/document-ai-from-ocr-to-agentic-doc-extraction/)
