# Vim 进阶：文本对象、宏、寄存器与配置

> 🚀 效率翻倍：文本对象精准选择、宏录制批量操作、寄存器妙用、搜索替换、vimrc 个性化配置

---

## 📚 目录

1. [文本对象](#1-文本对象)
2. [可视模式](#2-可视模式)
3. [寄存器](#3-寄存器)
4. [宏录制](#4-宏录制)
5. [搜索替换](#5-搜索替换)
6. [vimrc 配置](#6-vimrc-配置)

---

## 1. 文本对象

### 1.1 操作 = 动作 + 文本对象

```text
Vim 编辑公式：{operator}{text-object}

  operator：d(删) c(改) y(复制) v(选中)
  text-object：iw(单词) i"(引号内) i((括号内)

示例：
  diw  → 删除光标单词
  ci"  → 修改双引号内内容
  yi(  → 复制括号内内容
  da"  → 删除包含引号（a=around）
  c2iw → 修改两个单词
```

### 1.2 文本对象速查

| 对象 | i (inside) | a (around/含边界) |
|:--:|------|------|
| 单词 | `iw` | `aw` |
| 双引号 | `i"` | `a"`（删引号） |
| 括号 | `i(` / `i)` | `a(` |
| 标签 | `it` | `at`（含 HTML 标签） |
| 段落 | `ip` | `ap` |

```text
实战：
  ci"  → 光标在 "hello world" 中间某处 → 清空引号内 → 输入新内容
  da(  → 删除括号及所有内容
  >ap  → 缩进整段
```

---

## 2. 可视模式

| 按键 | 模式 | 用途 |
|:--:|------|------|
| `v` | 字符可视 | 选中字符 |
| `V` | 行可视 | 选中整行 |
| `Ctrl+v` | 块可视 | 矩形选择 ⭐ |

```text
块可视实战（Ctrl+v）：

  多行批量注释：
    Ctrl+v → jjj 向下选多行 → I → // → Esc
    （所有选中行行首都加了 //）

  矩形替换：
    Ctrl+v → 选中矩形区域 → c → 输入 → Esc
```

---

## 3. 寄存器

| 寄存器 | 说明 |
|:--:|------|
| `""` | 无名寄存器（默认） |
| `"0` | yank 专用 |
| `"+` | 系统剪贴板 ⭐ |
| `"a-z` | 命名寄存器（27个槽位） |
| `"_` | 黑洞寄存器（彻底删除） |

```text
实战：
  "+y    → 复制到系统剪贴板（Vim→外部）
  "+p    → 从系统剪贴板粘贴
  "a5yy  → 复制 5 行到寄存器 a
  "ap    → 粘贴寄存器 a 的内容
  "_dd   → 删除但不污染默认寄存器
  插入模式 Ctrl+r %  → 粘贴当前文件名
```

---

## 4. 宏录制

```text
录制：q{寄存器} → 操作 → q
播放：@{寄存器}
重复：@@
批量：{N}@{寄存器}

实战：给 100 行加引号和逗号

  qa            → 开始录制到 a
  I" → Esc      → 行首加引号
  A", → Esc     → 行尾加引号逗号
  j             → 下一行
  q             → 结束

  99@a → 剩下 99 行瞬间完成！
```

---

## 5. 搜索替换

```text
搜索：
  /word   向前     ?word   向后
  n/N     下/上一个  *      搜光标单词

替换：
  :%s/old/new/g          全文替换
  :s/old/new/g           当前行替换
  :%s/old/new/gc         确认替换
  选中后 :s/old/new/g    选区替换

常用：
  :%s/\s\+$//e       删行尾空格
  :%s/\v(a|b)/x/g    替换 a 或 b 为 x
```

---

## 6. vimrc 配置

```vim
" ~/.vimrc

set number relativenumber   " 行号 + 相对行号
set cursorline              " 高亮当前行
set hlsearch incsearch      " 搜索高亮 + 增量
set ignorecase smartcase    " 智能大小写
set tabstop=4 shiftwidth=4 expandtab

" 快捷键
let mapleader = " "        " Leader 设为空格
nnoremap <leader>w :w<CR>  " 空格+w 保存
nnoremap <leader>q :q<CR>  " 空格+q 退出
nnoremap <leader>h :noh<CR>" 空格+h 清除搜索高亮
nnoremap <leader>ev :e ~/.vimrc<CR>  " 空格+ev 编辑配置
nnoremap <C-h> <C-w>h      " Ctrl+hjkl 切换分屏
nnoremap <C-j> <C-w>j
nnoremap <C-k> <C-w>k
nnoremap <C-l> <C-w>l
```

---

> 🎯 **ci"** 改引号内、**Ctrl+v** 列编辑、**q+@** 宏批处理、**vimrc** 定制化、**"+"** 系统剪贴板无缝切换。掌握这些 = 效率翻倍。

---

*创建于：2026年7月*
