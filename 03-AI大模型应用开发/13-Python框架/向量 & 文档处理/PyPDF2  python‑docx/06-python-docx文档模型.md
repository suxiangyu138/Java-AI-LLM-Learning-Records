# 06-python-docx文档模型
> 定位：python-docx 的核心是对象模型——Document 管全局、Paragraph 管段落、Run 管格式，样式体系是「模板化写作」的钥匙；模型懂了，一切操作都是对象调用。

## 📚 目录
1. [对象模型三层](#1-对象模型三层)
2. [打开与新建](#2-打开与新建)
3. [段落与 run 详解](#3-段落与-run-详解)
4. [样式体系](#4-样式体系)
5. [中文排版](#5-中文排版)
6. [读取既有文档](#6-读取既有文档)
7. [常见坑](#7-常见坑)
8. [练习](#8-练习)

## 1. 对象模型三层

python-docx 把 docx 文件映射成三层对象模型，理解层级是一切操作的起点：

```text
Document（文档）
├── paragraphs（段落列表）
│   ├── runs（文本片段列表）
│   │   └── text / bold / italic / font
│   └── style（段落样式）
├── tables（表格列表）
├── sections（节：页面设置/页眉页脚）
└── inline_shapes（内嵌对象：图片等）
```

**段落（Paragraph）是排版单位**——Word 里按回车分隔的每一行块；**run 是格式单位**——一个段落里格式相同的一段连续文本。规则：**「一段文字里部分加粗/变色/换字体」必须拆成多个 run**；只设整个段落的统一格式，直接操作段落属性。文档结构则靠 `sections`（节）管理页面方向、页边距、页眉页脚（07 篇）——「横版插页」「每节不同页眉」都是节的差异。

## 2. 打开与新建

两种入口，一个模型：

```python
from docx import Document

doc = Document()                        # 新建：基于默认模板（空文档）
doc2 = Document("template.docx")        # 打开：编辑既有文档

# 打开后是「增删改」——在原文基础上操作
doc2.add_paragraph("追加的一段")         # 追加到末尾
doc2.save("template_updated.docx")      # 保存（新路径，不覆盖原文件）
```

三个要点：**`Document()` 与 `Document(path)` 返回同一类对象**——打开即编辑，没有只读模式（防误改自己把关）；**保存到新路径**是防覆盖纪律（02 篇坑五）；**模板化**是 python-docx 的高级用法——用占位文本（如 `{{name}}`）做模板，打开后替换 run 文本生成成品（07 篇）。docx 是 ZIP 压缩的 XML——python-docx 负责解包与封装，你永远只碰对象。

## 3. 段落与 run 详解

```python
doc = Document()
p = doc.add_paragraph()                       # 空段落
p.add_run("普通文本")
bold_run = p.add_run("加粗片段")
bold_run.bold = True                          # run 级格式
bold_run.font.size = Pt(14)
bold_run.font.color.rgb = RGBColor(0xC0, 0x00, 0x00)

# 段落级设置
p.alignment = WD_ALIGN_PARAGRAPH.CENTER       # 居中
p.paragraph_format.first_line_indent = Pt(24) # 首行缩进
p.paragraph_format.space_after = Pt(12)       # 段后间距
```

要点：**run.font 是字体对象**（size/name/color/bold/italic 全在这）；**段落格式在 `paragraph_format`**（缩进、间距、对齐、行距）；**`add_run` 返回 run 对象**——链式设置格式的惯用写法。**清空重写**：`p.text = "新内容"` 会重建段落文本（保留段落格式）；删 run 用 `run._element.getparent().remove(run._element)`（底层 XML 操作，07 篇避坑）。

## 4. 样式体系

样式是「格式的命名集合」——Word 里「正文」「标题 1」「引用」都是样式。python-docx 完整支持样式体系，这是模板化写作的核心：

```python
doc = Document()

# 用内置样式（style 参数）
doc.add_heading("一级标题", level=1)          # 内部是 "Heading 1" 样式
doc.add_paragraph("正文段落", style="Normal")
doc.add_paragraph("引用块", style="Quote")

# 修改样式 = 全局生效（一处改，所有用它的段落跟着变）
styles = doc.styles
styles["Normal"].font.name = "微软雅黑"        # 全局正文字体
styles["Heading 1"].font.size = Pt(18)

# 自定义样式
from docx.enum.style import WD_STYLE_TYPE
style = styles.add_style("MyNote", WD_STYLE_TYPE.PARAGRAPH)
style.font.size = Pt(10)
style.font.color.rgb = RGBColor(0x60, 0x60, 0x60)
p = doc.add_paragraph("灰色小字", style="MyNote")
```

**样式的价值是「一处定义、处处生效」**：报告模板改标题颜色只改一处样式，而不是遍历所有标题段落；换模板 = 换样式集合。内置样式名（"Heading 1"、"Normal"、"Title"）是固定的，自定义样式 `add_style` 后按名字使用。

## 5. 中文排版

python-docx 处理中文的完整姿势（02 篇坑六的解法）：

```python
from docx.oxml.ns import qn

def set_cn_font(run, name="微软雅黑"):
    """run 级中文设置：西文字体 + 东亚字体两套都要设"""
    run.font.name = name                       # 西文字体
    run._element.rPr.rFonts.set(qn("w:eastAsia"), name)   # 东亚（中文）字体

# 正文全局设置（模板化写作的基础）
style = doc.styles["Normal"]
style.font.name = "微软雅黑"
style.element.rPr.rFonts.set(qn("w:eastAsia"), "微软雅黑")
```

要点：**Word 的字体分两套**——西文（`font.name`）与东亚（`w:eastAsia`），只设前者中文不生效，这是「设置了字体没变化」的唯一原因；**`qn()` 是命名空间工具**（07 篇底层操作入门）；**常见中文字体**——正文微软雅黑/宋体、标题黑体/思源黑体，先问项目规范再设。

## 6. 读取既有文档

读取与生成是对称的——同样的对象模型，遍历即读取：

```python
doc = Document("report.docx")

for p in doc.paragraphs:
    print(p.style.name, "|", p.text)          # 段落样式 + 文本
    for run in p.runs:
        print("  run:", run.text, "bold=", run.bold)

for table in doc.tables:                      # 表格读取
    for row in table.rows:
        print([cell.text for cell in row.cells])
```

**读取的常见用途**：内容提取（docx → 纯文本进 RAG——`"\n".join(p.text for p in doc.paragraphs)`）、格式检查（哪些段落用了非法样式）、模板替换（找占位 run 换文本）。注意 `doc.paragraphs` 只含顶层段落——**表格里的段落不在其中**（遍历表格 cells 再取 paragraphs，07 篇）。

**段落格式的完整清单**（报告排版高频，一次记全）：对齐 `paragraph.alignment`（LEFT/CENTER/RIGHT/JUSTIFY）；首行缩进 `first_line_indent = Pt(24)`（中文正文 2 字符）；段前段后间距 `space_before`/`space_after`；行距 `line_spacing = 1.5`（倍距）或 `Pt(20)`（固定值）；分页控制 `keep_with_next = True`（标题与正文不分离）、`page_break_before = True`（段前分页）。**缩进与间距的单位**：`Pt(点)` 用于字号与缩进、`Cm/Inches` 用于宽高、`EMU` 是底层单位——`docx.shared` 里 `Pt/Inches/Cm/Mm` 都可用，混用没问题但别把 Pt 当 Cm 用（数值差 28 倍）。

**遍历与查找的惯用法**（读取既有文档的常用姿势一次给全）：

```python
doc = Document("report.docx")

# 按样式筛选（找所有标题段落）
headings = [p for p in doc.paragraphs if p.style.name.startswith("Heading")]

# 找包含关键字的段落
target = next((p for p in doc.paragraphs if "结论" in p.text), None)

# 逐段提取时跳过空段
lines = [p.text.strip() for p in doc.paragraphs if p.text.strip()]

# 段落序号（溯源场景）
for i, p in enumerate(doc.paragraphs, start=1):
    if p.text.strip():
        print(i, p.text[:50])
```

「按样式筛选」是读取最有价值的姿势——报告的结构（标题层级）藏在样式里，`p.style.name` 就是结构标签：提取标题 = 遍历段落按样式过滤，文档大纲即刻可得。这在 RAG 场景（docx 文档结构化）是核心操作。

**docx 读取的 RAG 价值**：docx 是「天生带结构」的格式——标题样式、段落边界、表格网格都是语义信息，比 PDF 提取的文本流好处理一个量级。docx 文档进 RAG 的标准路径：**按样式过滤标题 → 标题作章节锚点 → 段落在章节内聚合成块**（08 篇第 4 节）——全程零正则，结构是现成的。「段落即块」的前提是文档排版规范（用样式而非手改格式）——**遇到不用样式的文档（全靠手动加粗），结构信息就丢了，与 PDF 提取无异**——这是文档治理问题不是代码问题，但代码可以兜底（按字号/缩进启发式识别标题）。

## 7. 常见坑

**坑一：中文字体设置了不生效**。只设 `font.name` 没设 `w:eastAsia`——两套字体都要设。

**坑二：`doc.paragraphs` 漏表格内容**。表格内文本在 `doc.tables`——完整提取要两者都遍历。

**坑三：直接改 `p.text` 丢格式**。`p.text = ...` 重建段落，run 级格式全丢——改 run.text 保格式。

**坑四：样式名拼错**。`doc.styles["Heding 1"]` 报 KeyError——内置样式名照文档抄。

**坑五：`save` 覆盖原文件**。编辑原文件习惯性 save 同名——先存新路径。

**坑六：run 的对象删除用错方法**。`runs.remove(run)` 报错——run 没有 remove 方法，用底层 `_element.getparent().remove(...)`。

## 8. 练习

1. 三层对象模型分别管什么？run 为什么是格式单位？
2. 一段里「部分加粗」为什么必须拆 run？
3. 样式的「一处定义、处处生效」怎么实现？
4. 中文字体为什么必须设 eastAsia？
5. 完整提取 docx 内容（含表格）的代码怎么写？

> 🎯 **核心要点**：Document→Paragraph→Run 三层模型；run 是格式最小单位；样式是模板化写作核心；中文字体双设置（name + eastAsia）；读取与生成同一套模型，完整提取要含表格。

---

**下一模块**：[07-python-docx进阶](07-python-docx进阶.md)｜**返回总览**：[00-pypdf与python-docx总览](00-pypdf与python-docx总览.md)
