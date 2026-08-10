# 05-pdf表单与元数据
> 定位：PDF 不止是文本容器——表单（AcroForm）可程序化填写、书签可读可建、元数据可查可改；这些「结构化信息」是把 PDF 当数据库用的基础。

## 📚 目录
1. [PDF 的结构化信息](#1-pdf-的结构化信息)
2. [表单：读取与填写](#2-表单读取与填写)
3. [书签：大纲结构](#3-书签大纲结构)
4. [元数据](#4-元数据)
5. [页面叠加：merge_page](#5-页面叠加merge_page)
6. [常见坑](#6-常见坑)
7. [练习](#7-练习)

## 1. PDF 的结构化信息

PDF 文件内部不只有页面，还有三类「文档级」结构：**AcroForm 表单**（可填写的输入框、复选框、下拉框——发票、申请表、政府表格常见）、**Outline 书签**（文档大纲，侧边栏的目录树）、**Metadata 元数据**（标题、作者、创建时间等属性，藏于文件头部）。pypdf 对三者都可读可写——这意味着 PDF 可以当「结构化数据容器」用：批量填表、读大纲建目录、按元数据归档。

三类信息在 RAG 场景的用途：**书签 → 文档结构**（章节标题即切分边界，08 篇分块的锚点）；**元数据 → 溯源字段**（作者/时间/来源入库）；**表单 → 结构化数据**（发票字段直接提取，不用解析版面）。

## 2. 表单：读取与填写

**读取字段**：`reader.get_fields()` 返回字段字典（字段名 → 字段对象）：

```python
from pypdf import PdfReader, PdfWriter

reader = PdfReader("form.pdf")
fields = reader.get_fields()               # {'姓名': Field, '身份证号': Field, ...}
for name, field in fields.items():
    print(name, field.get("/V"))           # 字段名与当前值（V = Value 条目）
    print(field.get("/FT"))                # 字段类型：/Tx 文本 /Btn 按钮 /Ch 选择
```

**填写字段**：`writer.update_page_form_field_values` 是批量填写入口：

```python
writer = PdfWriter()
writer.append(reader)                      # 复制原文档
writer.update_page_form_field_values(
    writer.pages[0],
    {"姓名": "张三", "身份证号": "110101200001011234", "性别": "男"},
)
writer.write("filled.pdf")
```

要点：**字段名必须与 get_fields 返回的键完全一致**（含大小写与空格）；**「性别」这类值来自字段的选项值**（读 `/Opt` 选项列表，填不在选项里的值无效）；**填写后需要「展平」吗**——不展平的 PDF 字段仍可编辑（签回、篡改风险），归档场景用 `writer.append(reader, flatten=True)` 展平（内容固化成页面，字段不可再编辑）。批量填表（500 张发票）就是这个 API 的循环，09 篇性能纪律适用。

**批量填表的实战形态**——「数据行 → PDF 表单」的映射循环是发票/合同/证书生成的骨架：

```python
import csv
from pypdf import PdfReader, PdfWriter

records = list(csv.DictReader(open("invoices.csv", encoding="utf-8")))
# 字段映射：CSV 列名 → 表单字段名（一次建立，反复使用）
FIELD_MAP = {"name": "客户名称", "amount": "金额", "no": "发票号"}

for i, rec in enumerate(records):
    writer = PdfWriter()
    writer.append(PdfReader("template.pdf"))      # 模板每张一读（或缓存 reader）
    writer.update_page_form_field_values(
        writer.pages[0],
        {form_name: rec[col] for col, form_name in FIELD_MAP.items()},
    )
    with open(f"out/invoice_{i:04d}.pdf", "wb") as f:
        writer.write(f)
```

要点：**模板与数据分离**——模板 PDF 是排版资产（设计师管），数据是业务资产（程序管），映射表是两者的契约；**字段映射表显式维护**——模板字段改名时只改映射表，不碰业务代码；**reader 可缓存**——同模板循环里 `PdfReader("template.pdf")` 每个 writer.append 内部会重新解析，性能敏感时先建 reader 复用（09 篇）。

## 3. 书签：大纲结构

**读书签**：`reader.outline` 返回嵌套大纲（书签树）：

```python
def walk(outline, depth=0):
    for item in outline:
        if isinstance(item, list):         # 嵌套子书签
            walk(item, depth + 1)
        else:
            print("  " * depth, item.title)

walk(reader.outline)                       # 章节 → 小节 的树状输出
```

书签的价值在 RAG：**书签标题 = 语义章节边界**——按书签把整本书切成「标题 + 内容」的块，比按页切分质量高一个量级（08 篇分块策略）。**建书签**：`writer.add_outline_item(title, page_number, parent=None)`：

```python
writer.add_outline_item("第一章 概述", 0)      # 指向第 1 页
writer.add_outline_item("第二章 原理", 5)
chapter2 = writer.add_outline_item("2.1 核心", 6, parent=?)  # 嵌套层级
```

生成带目录的 PDF 时用（报告导出场景：Word 生成 + PDF 化后补书签）。

## 4. 元数据

**读元数据**：`reader.metadata` 是文档信息字典：

```python
meta = reader.metadata
print(meta.title)          # 标题
print(meta.author)         # 作者
print(meta.creation_date)  # 创建时间
```

**写元数据**：`writer.add_metadata()`：

```python
writer.add_metadata({
    "/Title": "2026 年度报告",
    "/Author": "数据组",
    "/Subject": "自动生成的年度汇总",
    "/Keywords": "report, 2026",
})
```

生产用途：**归档索引**（按 Title/Author 检索文件库）、**溯源**（RAG 文档入库时把元数据写进向量库的 metadata 字段——「这份回答来自哪个文件」的答案就在这里）。注意 `creation_date` 是 UTC 时间，显示时注意时区换算。

## 5. 页面叠加：merge_page

`merge_page` 把另一页的内容叠加到当前页——「盖水印」「并两页」的底层操作：

```python
from pypdf import PdfReader, PdfWriter

watermark = PdfReader("watermark.pdf").pages[0]    # 水印页
writer = PdfWriter()
for page in PdfReader("doc.pdf").pages:
    page.merge_page(watermark, over=True)          # over=True 水印在上层
    writer.add_page(page)
writer.write("watermarked.pdf")
```

要点：**`over=True`**（默认）叠加内容在上，`over=False` 在底（底色页）；**合并的页坐标对齐**——水印页与目标页尺寸不同时先缩放（`page.scale_to(595, 842)` 对齐 A4）；**水印文本页 vs 图像页**——文本水印（「机密」「DRAFT」）用 pypdf 画不了，需要先造水印 PDF（ReportLab 或 python-docx 导出），或直接用图像水印页。叠加页会携带自身资源（字体等），大量叠加后文件可能变大——正常现象。

## 6. 常见坑

**坑一：字段名对不上**。填写没生效、报 KeyError——先 `get_fields()` 打印键名，含空格/大小写逐字核对。

**坑二：填了不存在的选项值**。复选框/下拉框填非法值无效——读 `/Opt` 列表再填。

**坑三：填完字段还能改**。归档场景未展平——`flatten=True` 固化内容。

**坑四：书签是嵌套列表**。`reader.outline` 里 item 可能是 list——遍历必须递归判断。

**坑五：元数据时间时区**。`creation_date` 是 UTC——本地化显示要换算。

**坑六：水印尺寸不匹配**。水印页覆盖不全或偏移——先 `scale_to` 对齐目标页尺寸。

**表单的版本与兼容现实**：表单 PDF 有两代实现——**AcroForm**（传统，pypdf 完整支持，本节所讲）与 **XFA**（Adobe 专有 XML 表单，现代 Adobe 导出的表单常见）。pypdf 对 XFA 表单的支持有限：`get_fields()` 可能拿到空 dict、填写可能无效——**先试读，读不到字段就检查 `reader.trailer` 里是否有 XFA 条目**；XFA 表单的替代方案是转 AcroForm（用 Adobe/Acrobat 另存）或换 PyMuPDF。另一个现实是**填写后的兼容性**：`update_page_form_field_values` 填出的表单在不同阅读器（Adobe/WPS/浏览器）显示可能不一致——交付前用目标阅读器打开验证，别只看代码输出成功。

**书签与表单在 RAG 的完整用法**：书签树转章节映射（05 篇第 3 节的 walk 函数输出 → `{"章节": "起止页"}` 字典），配合逐页提取的文本按章节重组——**「书签驱动的按章切块」是 PDF RAG 里质量最高的切分方式之一**（比定长切、按页切都优，08 篇第 4 节）；表单字段同理——发票库的「发票号/金额/日期」字段直接提出来进结构化表，检索「某发票」是精确匹配而不是向量模糊搜索。**元数据与向量库 metadata 的映射**：`reader.metadata.title/author` → chromadb 的 metadata 字段（`{"title": ..., "author": ...}`），检索结果天然带来源属性——「这份回答出自哪份文件、谁写的」自动可答。**三者的共同纪律**：先 `reader.is_encrypted` 解密，再读结构信息——加密 PDF 的结构读取同样失败。

## 7. 练习

1. PDF 的三类文档级结构化信息是什么？RAG 里各怎么用？
2. `update_page_form_field_values` 的字段名匹配为什么必须逐字核对？
3. 「展平」是什么？归档场景为什么需要？
4. 书签树怎么转成 RAG 的分块边界？
5. merge_page 的 over 参数语义是什么？

> 🎯 **核心要点**：get_fields 读字段、update_page_form_field_values 填表、归档必展平；书签是 RAG 切分锚点；元数据是溯源字段；merge_page + scale_to 是水印与叠加的标准姿势。

---

**下一模块**：[06-python-docx文档模型](06-python-docx文档模型.md)｜**返回总览**：[00-pypdf与python-docx总览](00-pypdf与python-docx总览.md)
