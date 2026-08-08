---
name: doc-quality
description: 校验 Markdown 技术文档质量：代码围栏配对、内部链接目标存在、TOC 锚点匹配、表格完整性、导航完整性。用户要求"自检"、"校验"、"检查文档"时使用。
---

# Doc Quality：文档质量校验

对 Markdown 文档/知识体系做全面质量校验，输出问题清单。

## 校验项

| 项 | 检查内容 | 失败示例 |
|----|---------|---------|
| 代码围栏 | ` ``` ` 数量成对、语言标注齐全 | 围栏余数 1 |
| 内部链接 | 相对路径链接目标存在（含 `%20` 解码）；跨目录层级正确 | 少一级 `../` |
| TOC 锚点 | 目录锚点与标题匹配（小写、空格→`-`、去标点） | `#5-xxx` 对不上标题 |
| 表格 | 无孤立分隔行；表头下紧跟 `|---|` | 分隔行无表头 |
| 导航 | 非首篇有「返回总览」，非末篇有「下一模块」 | 缺导航 |
| 链接编码 | 含空格的文件名链接用 `%20` 编码 | `01-xxx 篇.md` 未编码 |
| 参考来源 | 【参考来源】每条带 URL 超链接 | 纯文本无链接 |

## 校验脚本

对单个文件或整个目录运行：

```bash
# 单文件围栏检查
grep -c '^```' "文件.md"   # 结果应为偶数

# 目录全量校验（链接/锚点/导航）
python -c "
import re, urllib.parse
from pathlib import Path
base = Path('.')
for f in sorted(base.glob('*.md')):
    text = f.read_text(encoding='utf-8')
    # 围栏
    fences = len(re.findall(r'^\`\`\`', text, re.M))
    if fences % 2: print(f'{f.name}: 围栏不成对 ({fences})')
    # 内部链接存在性
    for m in re.finditer(r'\]\(([^)]+)\)', text):
        link = m.group(1)
        if link.startswith(('http','#','mcp')) or '://' in link: continue
        path_part = urllib.parse.unquote(link.split('#')[0])
        if path_part and not (f.parent / path_part).resolve().exists() and not (base / path_part).resolve().exists():
            print(f'{f.name}: 链接缺失 -> {link}')
    # 锚点
    if '## 📚 目录' in text:
        toc = text.split('## 📚 目录')[1].split('## 1.')[0]
        headers = re.findall(r'^#{1,3} (.+)$', text, re.M)
        for a in re.findall(r'\]\(#([^)]+)\)', toc):
            if not any(a == re.sub(r'[^\w一-鿿 -]','',h).lower().replace(' ','-') for h in headers):
                print(f'{f.name}: 锚点缺失 #{a}')
print('校验完成')
"
```

## 修复规则

- 链接缺失 → 先 `ls` 确认真实文件名/层级，再修正（不猜）
- 锚点不匹配 → 以标题实际生成规则为准修正 TOC
- 围栏不成对 → 定位缺失的 ``` 行

## 禁忌

- 禁止"看起来没问题"就跳过——必须跑脚本
- 禁止凭印象修链接——先确认目标文件真实存在
