# 07-python-docx进阶
> 定位：表格、图片、页眉页脚、注释、模板替换——报告生成的完整弹药库；数据进表格、图表进图片、批注进注释，一篇专业文档的要素这里全有。

## 📚 目录
1. [表格：数据可视化](#1-表格数据可视化)
2. [图片与图表](#2-图片与图表)
3. [页眉页脚与节](#3-页眉页脚与节)
4. [注释与超链接](#4-注释与超链接)
5. [模板替换工作流](#5-模板替换工作流)
6. [底层 XML 操作入门](#6-底层-xml-操作入门)
7. [常见坑](#7-常见坑)
8. [练习](#8-练习)

## 1. 表格：数据可视化

表格是数据进 Word 的主要通道——`add_table` 建骨架，逐行填充：

```python
from docx import Document
from docx.shared import Pt

doc = Document()
data = [["产品", "销量", "增长率"],
        ["A", "1200", "12%"],
        ["B", "980", "8%"]]

table = doc.add_table(rows=len(data), cols=len(data[0]))
table.style = "Table Grid"                     # 带边框的表格样式

for i, row_data in enumerate(data):
    for j, cell_text in enumerate(row_data):
        cell = table.cell(i, j)
        cell.text = cell_text
        if i == 0:                              # 表头加粗
            for run in cell.paragraphs[0].runs:
                run.bold = True
```

要点：**`table.style` 必须设**——默认无边框样式，Word 里看起来像没表格（"Table Grid" 是常用内置样式）；**单元格内是段落**——`cell.paragraphs[0]` 拿第一个段落操作格式，单元格可以有多个段落（换行）；**行列遍历**——`table.rows`/`table.columns` 遍历，`cell.text =` 是「整格文本」快捷写，`cell.add_paragraph` 是追加段落；**合并单元格**：`cell_a.merge(cell_b)`（跨行跨列合并，表头场景常用）。**从 pandas 直接建表**是高频需求：DataFrame → 行列循环 → 表格（09 篇给完整函数）。

## 2. 图片与图表

```python
doc.add_picture("chart.png", width=Inches(5.5))     # 图片（自动居中？不——需手动）
doc.paragraphs[-1].alignment = WD_ALIGN_PARAGRAPH.CENTER   # 上一步的图片段落居中

# 指定位置插入：在段落前插图片
p = doc.add_paragraph("见图 1：趋势分析")
run = p.add_run()
run.add_picture("chart.png", width=Inches(4.0))
```

要点：**`add_picture` 会把图片放进「当前末尾段落」**——`doc.add_picture` 实际在文档末尾新起段落并内嵌；**宽高控制**——`width`/`height` 传 `Inches`/`Cm`/`Mm`，只设 width 保持宽高比；**图表生成**——matplotlib 出图存 PNG（dpi≥150 保证清晰）再插入；**图片格式**——PNG/JPG 直接支持，SVG 需先转栅格。**占位图**场景：先 `add_picture` 空图占位，后替换（改 `inline_shapes` 里的 image blob，进阶技巧）。

## 3. 页眉页脚与节

页眉页脚挂在 `section` 上——文档可以有多个节，每节独立页面设置：

```python
section = doc.sections[0]                        # 第一节
header = section.header
header.paragraphs[0].text = "2026 年度报告"       # 页眉文字
section.footer.paragraphs[0].text = f"第 {1} 页"  # 页脚（页码需要域代码）

# 页码域（Word 自动页码，不是写死数字）
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
fld = OxmlElement("w:fldSimple")                 # 简单域：PAGE
fld.set(qn("w:instr"), "PAGE")
section.footer.paragraphs[0]._p.append(fld)
```

要点：**页码必须用域代码**（`PAGE` 字段），写死数字每页相同；**节的差异**——`doc.sections[0]` 是默认节，横版插页/不同页边距 = 新节（`doc.add_section(WD_SECTION.NEW_PAGE)`）；**首页不同**——`section.different_first_page_header_footer = True` 单独设首页；**「奇偶页不同」**同理（书籍排版）。页眉页脚也是段落——样式与格式同正文段落一样操作。

## 4. 注释与超链接

**注释（1.2.0 新能力）**：

```python
# 1.2.0 起支持 comments（此前版本不可用）
# 需要 python-docx >= 1.2.0
from docx.comments import Comments

# 给段落加注释（1.2.0 API）
p = doc.add_paragraph("待复核的数据")
comment = Comments(...)   # 详见官方文档示例（1.2.0 新增，API 以官方为准）
```

**超链接**：python-docx 无内置 add_hyperlink 方法，用底层 XML 造：

```python
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

def add_hyperlink(paragraph, url, text):
    part = paragraph.part
    r_id = part.relate_to(url, "http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink", is_external=True)
    hyperlink = OxmlElement("w:hyperlink")
    hyperlink.set(qn("r:id"), r_id)
    run = OxmlElement("w:r")
    rPr = OxmlElement("w:rPr")
    color = OxmlElement("w:color"); color.set(qn("w:val"), "0563C1")
    rPr.append(color)
    run.append(rPr)
    t = OxmlElement("w:t"); t.text = text
    run.append(t)
    hyperlink.append(run)
    paragraph._p.append(hyperlink)
```

超链接是「无内置方法」的典型——**python-docx 覆盖 95% 需求，剩余 5% 用底层 XML 补**（下节）。

**页眉页脚的进阶组合**：三类高频需求——**首页不同**（封面无页眉）：`section.different_first_page_header_footer = True` 后 `section.first_page_header` 单独设置；**奇偶页不同**（书籍排版，左页书名右页章节）：`section.even_page_header` 单独设置；**每节独立**（横版插页用自己的页眉）：新节自动带独立页眉页脚容器。**页眉页脚里插图片**（公司 Logo）：`section.header.paragraphs[0].add_run().add_picture("logo.png", width=Inches(0.8))`——与正文插图同一 API。注意页眉页脚默认「链接到上一节」，新节要独立必须关掉链接（`header.is_linked_to_previous = False`），否则改一节全文档跟着变——这是「页眉没改对」的头号原因。

## 5. 模板替换工作流

生产级 docx 生成的标准姿势：**模板 + 占位符 + 替换**，而不是纯代码从零搭（排版规范都在模板里）：

```python
doc = Document("report_template.docx")          # 模板：含 {{title}} {{name}} 占位
replace_map = {"{{title}}": "2026 年度报告", "{{name}}": "张三"}

def replace_in_doc(doc, mapping):
    for p in doc.paragraphs:
        for run in p.runs:
            for key, val in mapping.items():
                if key in run.text:
                    run.text = run.text.replace(key, val)   # 改 run.text 保格式
    return doc

doc = replace_in_doc(doc, replace_map)
doc.save("report_2026.docx")
```

要点：**占位符必须完整落在一个 run 里**（Word 编辑时可能拆 run——模板生成时用代码写入保证）；**改 run.text 保留格式**（06 篇坑三）；**复杂替换（表格内、图片）**要扩展遍历 tables 与 inline_shapes。模板工作流的价值：排版、字体、样式在 Word 里定好，代码只填数据——设计者与程序员的职责分离。

**模板资产的工程管理**：模板是「文档资产」，与代码同仓管理——模板放 `templates/` 目录、占位符命名规范（`{{name}}` 双花括号是通用约定）、模板版本化（改模板要回归测试——字段名变了代码没跟着变，运行时报 KeyError 或空替换）。**占位符缺失的兜底**：替换后扫描残留 `{{`——残留说明模板与映射不一致，直接抛错而不是静默输出坏文档：`assert "{{" not in doc._element.xml`（06 篇 XML 入口）。**模板 vs 纯代码**的取舍：模板适合「固定版式、大量数据」；纯代码适合「版式随数据变」——先模板后纯代码是大多数项目的演进路径。

## 6. 底层 XML 操作入门

docx = ZIP + XML，python-docx 的封装层下有完整的 OOXML 世界。三个常用入口：

```python
# 1. 元素级操作（rPr/rFonts 等）
from docx.oxml.ns import qn
run._element.rPr.rFonts.set(qn("w:eastAsia"), "宋体")   # 06 篇中文设置

# 2. 造 XML 元素
from docx.oxml import OxmlElement
el = OxmlElement("w:pageBreak")

# 3. 原生 XML 解析（完全控制）
import docx
xml_str = doc._element.xml                       # 文档全文 XML
```

何时需要 XML：**无内置 API 的功能**（超链接、域代码、部分格式）、**批量格式微调**（遍历元素改属性）、**调试**（看文档实际 XML 排查「为什么没生效」）。不需要时别碰——封装层 95% 够用，XML 是逃生舱不是日常。

## 7. 常见坑

**坑一：表格没边框**。忘设 `table.style`——"Table Grid" 起步。

**坑二：图片超大**。忘设 width——原尺寸插入，A4 放不下；设 `width=Inches(5.5)` 以内。

**坑三：页码写死**。`footer.paragraphs[0].text = "第 1 页"` 每页都 1——用 PAGE 域代码。

**坑四：注释 API 版本不满足**。1.2.0 以下没有 comments——先升级再写。

**坑五：占位符被拆 run**。Word 手工编辑的模板可能拆 run，替换不到——模板用代码生成或合并 run 再替换。

**坑六：超链接直接 add_run**。普通 run 不可点——必须造 `w:hyperlink` 元素。

**坑七：图片居中失败**。`add_picture` 后图片段落是 `doc.paragraphs[-1]`，对齐要设在该段落。

**坑八：表格合并单元格坐标错乱**。`merge` 后行列索引以合并前为准，遍历 cells 会重复——合并后按需手动构造访问路径。

**坑九：模板替换后占位符残留**。映射表漏键——替换后断言扫描 `{{` 残留（「模板资产的工程管理」一节）。

**坑十：add_picture 路径含中文/空格报错**。部分场景路径解析问题——先 `os.path.abspath` 或用 `Path` 对象传入。

## 8. 练习

1. 表格样式「Table Grid」的作用是什么？
2. 页码为什么必须用域代码？
3. 模板替换工作流的价值与前提（占位符落 run）？
4. 超链接为什么没有内置方法？底层怎么补？
5. 什么时候需要碰 XML？

> 🎯 **核心要点**：表格设 style、图片控 width、页码用域、注释要 1.2.0+；模板 + 占位符 + 改 run.text 是生产标准工作流；python-docx 覆盖 95%，超链接/域代码走 XML 逃生舱。

---

**下一模块**：[08-提取质量与RAG衔接](08-提取质量与RAG衔接.md)｜**返回总览**：[00-pypdf与python-docx总览](00-pypdf与python-docx总览.md)
