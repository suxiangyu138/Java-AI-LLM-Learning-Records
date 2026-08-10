# 04-pdf合并拆分与编辑
> 定位：PdfWriter 是 PDF 操作的主力——合并、拆分、旋转、裁剪、插入、加密解密，全部是「页面对象」的搬运与变换；掌握页面对象模型，一切操作都是组合。

## 📚 目录
1. [PdfWriter 与页面对象模型](#1-pdfwriter-与页面对象模型)
2. [合并：多文件拼接](#2-合并多文件拼接)
3. [拆分：按页抽取](#3-拆分按页抽取)
4. [旋转与裁剪](#4-旋转与裁剪)
5. [插入、重排与复制](#5-插入重排与复制)
6. [加密与解密](#6-加密与解密)
7. [常见坑](#7-常见坑)
8. [练习](#8-练习)

## 1. PdfWriter 与页面对象模型

pypdf 的一切操作都围绕一个模型：**PDF = 页面对象的集合**。`PdfReader.pages` 是「只读的页面视图」，`PdfWriter` 是「可拼装的页面容器」——把 reader 的页面 add 进 writer，再 write 出去，就完成了操作。这个「读→写」模型覆盖全部编辑需求：

```python
from pypdf import PdfReader, PdfWriter

reader = PdfReader("source.pdf")
writer = PdfWriter()
for page in reader.pages:
    writer.add_page(page)          # 逐页搬运
writer.write(open("copy.pdf", "wb"))
```

**writer 是累加器**：可以跨多个 reader add（合并）、挑页 add（拆分）、add 后变换（旋转裁剪）。所有操作不修改原文件——`PdfWriter` 从零组装新文件，原始 PDF 纹丝不动，这是安全性的根基（操作失败顶多坏输出，不会毁源文件）。`writer.write` 接受文件对象或路径（pypdf 2.10+ 支持路径），但规范仍是 `open(..., "wb")`。

## 2. 合并：多文件拼接

合并是最高频场景（合同附件、报告汇总、扫描件归档）：

```python
from pypdf import PdfReader, PdfWriter

writer = PdfWriter()
for path in ["cover.pdf", "body.pdf", "appendix.pdf"]:
    reader = PdfReader(path)
    for page in reader.pages:
        writer.add_page(page)
writer.write("merged.pdf")
```

要点：**页码顺序 = add 顺序**——想调整顺序就调整 add 次序；**合并保留各页原有内容与样式**（字体、图片都在页面内部）；**合并不重新压缩**——多个大文件合并后体积 ≈ 各文件之和，不会优化；**元数据**——合并后可用 `writer.add_metadata(reader.metadata)` 保留源信息（05 篇）。**大量文件循环合并**（几百个）会同时持有所有 reader——用后即弃或流式处理，09 篇性能纪律。

## 3. 拆分：按页抽取

拆分的核心操作是「选页」——切片、条件筛选、均分：

```python
from pypdf import PdfReader, PdfWriter

reader = PdfReader("book.pdf")

# 抽指定页（如第 3-5 页）
writer = PdfWriter()
for page in reader.pages[2:5]:
    writer.add_page(page)
writer.write("chunk.pdf")

# 条件拆分（按页数均分或按内容标记）
def split_in_half(reader, prefix="part"):
    n = len(reader.pages)
    for i, start in enumerate(range(0, n, n // 2)):
        w = PdfWriter()
        for page in reader.pages[start:start + n // 2]:
            w.add_page(page)
        w.write(f"{prefix}{i}.pdf")

# 按页提取文本判断分界（章节拆分）
for i, page in enumerate(reader.pages):
    if "第 1 章" in (page.extract_text() or ""):
        # 以此为分界点写入新文件
        pass
```

`reader.pages` 支持完整切片语法，配合 `len()` 与条件判断，拆分逻辑全是「选页」的组合。注意 `reader.pages[i:j]` 返回新的列表视图——大文件循环里注意内存。

## 4. 旋转与裁剪

页面变换是「add 后处理」——每个 `writer.pages[i]` 是可变页面对象，旋转裁剪直接作用其上：

```python
from pypdf import PdfReader, PdfWriter
from pypdf.generic import RectangleObject

reader = PdfReader("rotated_source.pdf")
writer = PdfWriter()
for page in reader.pages:
    page.rotate(90)                  # 旋转 90°（顺时针；-90 逆时针）
    writer.add_page(page)

# 裁剪：保留上半页（坐标：左下 x, y, 右上 x, y）
writer.pages[0].cropbox.lower_left = (0, 0)
writer.pages[0].cropbox.upper_right = (612, 396)   # Letter 半页
writer.write("fixed.pdf")
```

**旋转语义**：`rotate(90)` 顺时针；多页文档里个别页扫描方向反了（手机拍照件），按页修正是高频场景。**裁剪坐标**：PDF 坐标原点在左下角，单位是点（1/72 英寸）——A4 是 595×842 点，Letter 是 612×792。裁剪后打印、提取都按新边界——**「去掉扫描件的黑边」就是这个操作**。

## 5. 插入、重排与复制

页面操作不止「追加」，还有插入与重排——`writer.pages` 是列表式接口，支持插入与重排：

```python
writer.pages.insert(1, new_page)         # 插到第 2 个位置
writer.pages.append(new_page)            # 追加
writer.pages.extend([p1, p2])            # 批量
# 重排：直接构造新顺序（倒序示例）
reader = PdfReader("a.pdf")
writer = PdfWriter()
for page in reversed(reader.pages):      # 逆序输出
    writer.add_page(page)

# 复制页面并裁剪（合并两页到一页的雏形）
from pypdf import PdfReader, PdfWriter
reader = PdfReader("two.pdf")
writer = PdfWriter()
p1 = reader.pages[0].transfer_rotation_to_content()
writer.add_page(reader.pages[0])
writer.add_page(reader.pages[0])         # 同一页可以 add 多次
writer.write("dup.pdf")
```

「同一页 add 多次」是实用技巧（重复页、测试页）；`transfer_rotation_to_content()` 把旋转固化进内容（旋转后的裁剪坐标更可控）。高级合并（两页并一页、拼接名片）用 `page.merge_page(other)`——把另一页内容叠加到当前页（05 篇进阶）。

## 6. 加密与解密

PDF 加密是「权限密码」体系——加密后打开需要密码，且可限制打印/复制权限：

```python
from pypdf import PdfReader, PdfWriter

# 加密：写文件前 encrypt
writer = PdfWriter()
writer.append(reader)                    # 或逐页 add
writer.encrypt(
    user_password="user123",             # 打开密码
    owner_password="owner123",           # 权限密码（可省略=等于用户密码）
    permissions_flag=-1,                 # -1 = 允许一切；按位控制打印/复制
)
writer.write("protected.pdf")

# 解密：打开时传密码
reader = PdfReader("protected.pdf", password="user123")
print(reader.is_encrypted)               # True → 解密后 False
```

要点：**`is_encrypted` 先查**——处理未知来源 PDF 时先判加密，否则提取/编辑直接报错；**解密后对象才可读**——`reader.pages` 在解密前访问报错；**权限位**——`-1` 全允许，`4` 禁止打印、`8` 禁止复制（按位或组合），但注意权限限制只防君子（PDF 权限是软限制，专业工具可绕过）——真正要防拷贝的内容用内容级保护（水印、图像化）。**加密是常见的「提取为空」原因**（03 篇乱码五问第二问）。

**批量操作的组合模式**：实际项目的 PDF 操作极少是单步——「合并 + 加密 + 加书签」三步连做是常见需求。组合的技巧：**每一步都是独立的 writer 操作，按顺序叠加**（先 add 页 → 再变换 → 再加密 → 最后 write 一次）；**中间状态检查**——每步之后可打印 `len(writer.pages)` 验证页数符合预期，组合出错时能定位到具体步骤；**操作与业务分离**——把「合并三件套」封装成函数（`merge_with_encrypt(paths, password)`），业务代码只调函数，管道可测可复用。**先小样本验证再全量跑**：对 2-3 个文件试跑确认输出正确，再对 500 个文件批量——批量出错的成本远高于验证成本。

## 7. 常见坑

**坑一：解密前访问 pages**。`is_encrypted` 为 True 时 `reader.pages` 报错——先解密再访问。

**坑二：合并大文件内存爆**。几百个 reader 同时持有——流式处理，用完即关。

**坑三：裁剪坐标搞反**。`lower_left` 与 `upper_right` 传反了输出空白页——记住坐标原点左下、lower 是较小值。

**坑四：加密密码写死在代码**。密码进配置或环境变量，别进 git 历史。

**坑五：误以为 writer 修改原文件**。`PdfWriter` 是组装新文件——原文件不会被改，这是特性不是 bug。

**坑六：旋转多页整体转**。个别页反了整体 rotate 全转——按页判断（`page.rotation` 读取当前角度）。

**坑七：输出文件名冲突**。循环写 `out.pdf` 同名覆盖只剩最后一页——输出名带序号或源文件名（`f"{name}_merged.pdf"`）。

**坑八：encrypt 与 append 顺序错**。先 encrypt 后 append 的页面不受保护——先 append 完再 encrypt，页面与权限一并写入。

## 8. 练习

1. 「PDF = 页面对象的集合」这个模型怎么解释合并与拆分？
2. 合并的页码顺序由什么决定？
3. 裁剪的坐标系统原点在哪里？单位是什么？
4. 加密的 user/owner 密码分别管什么？
5. 为什么说 writer 是累加器？这带来什么安全性？

> 🎯 **核心要点**：PdfWriter = 页面累加器，add 顺序即页面顺序；合并拆分全是「选页」组合；旋转裁剪作用于 writer.pages[i]，坐标原点左下角；加密先查 `is_encrypted` 再访问；所有操作不碰原文件。

---

**下一模块**：[05-pdf表单与元数据](05-pdf表单与元数据.md)｜**返回总览**：[00-pypdf与python-docx总览](00-pypdf与python-docx总览.md)
