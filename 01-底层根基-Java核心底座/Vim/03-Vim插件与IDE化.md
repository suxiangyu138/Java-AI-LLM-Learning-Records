# Vim 插件与 IDE 化

> 🔌 插件管理器、文件树、模糊查找、代码补全 LSP、Git 集成 —— 把 Vim 打造成 Java/全栈 IDE

---

## 📚 目录

1. [插件管理器](#1-插件管理器)
2. [必装插件清单](#2-必装插件清单)
3. [模糊查找与文件导航](#3-模糊查找与文件导航)
4. [代码补全与 LSP](#4-代码补全与-lsp)
5. [Java 开发配置](#5-java-开发配置)

---

## 1. 插件管理器

```vim
" 推荐 vim-plug（轻量、并行安装）
" 安装：
" curl -fLo ~/.vim/autoload/plug.vim --create-dirs \
"   https://raw.githubusercontent.com/junegunn/vim-plug/master/plug.vim

call plug#begin('~/.vim/plugged')

" 在这里列插件...

call plug#end()

" 命令：
" :PlugInstall   安装插件
" :PlugUpdate    更新插件
" :PlugClean     清理未使用的插件
```

---

## 2. 必装插件清单

| 插件 | 功能 | 推荐度 |
|------|------|:-----:|
| **NERDTree** | 文件树侧边栏 | ⭐⭐⭐⭐ |
| **fzf.vim** | 模糊查找一切 | ⭐⭐⭐⭐⭐ |
| **vim-airline** | 状态栏美化 | ⭐⭐⭐⭐ |
| **vim-commentary** | 快速注释（gcc） | ⭐⭐⭐⭐⭐ |
| **surround.vim** | 快速包围修改 | ⭐⭐⭐⭐⭐ |
| **auto-pairs** | 自动补全括号引号 | ⭐⭐⭐ |
| **vim-gitgutter** | Git 修改标记 | ⭐⭐⭐⭐ |
| **undotree** | 可视化撤销树 | ⭐⭐⭐⭐ |
| **Ale / Coc / nvim-lsp** | 语法检查 / LSP 补全 | ⭐⭐⭐⭐⭐ |

```vim
" vim-plug 配置示例
call plug#begin('~/.vim/plugged')

" 文件导航
Plug 'preservim/nerdtree'
Plug 'junegunn/fzf', { 'do': { -> fzf#install() } }
Plug 'junegunn/fzf.vim'

" 编辑增强
Plug 'tpope/vim-surround'       " 包围修改
Plug 'tpope/vim-commentary'     " 注释 gc
Plug 'jiangmiao/auto-pairs'     " 括号配对

" 美观
Plug 'vim-airline/vim-airline'

" Git
Plug 'airblade/vim-gitgutter'
Plug 'tpope/vim-fugitive'       " Git 命令集成

" 撤销树
Plug 'mbbill/undotree'

" LSP 代码补全
Plug 'neoclide/coc.nvim', {'branch': 'release'}

call plug#end()
```

---

## 3. 模糊查找与文件导航

### 3.1 fzf（模糊查找一切）

```vim
" 快捷键
nnoremap <C-p> :Files<CR>       " Ctrl+p 搜文件
nnoremap <leader>f :Rg<CR>      " 空格+f 全文搜索
nnoremap <leader>b :Buffers<CR> " 空格+b 切换 Buffer

" fzf 使用：
" Files     → 输入关键字 → 模糊匹配文件名 → 回车打开
" Rg        → 项目中搜索文本内容
" Buffers   → 切换打开的文件
```

### 3.2 NERDTree（文件树）

```vim
" 快捷键
nnoremap <leader>n :NERDTreeToggle<CR>   " 空格+n 开关文件树

" NERDTree 内快捷键：
" o    打开文件/展开目录
" t    在新 Tab 打开
" m    显示菜单（创建/删除/移动文件）
" ?    帮助
```

---

## 4. 代码补全与 LSP

### 4.1 Coc.nvim（VS Code 级别补全）

```vim
" Coc.nvim 基本配置
" 使用 Tab 接受补全
inoremap <silent><expr> <TAB>
      \ coc#pum#visible() ? coc#pum#confirm() :
      \ coc#expandableOrJumpable() ? "\<C-r>=coc#rpc#request('doKeymap', ['snippets-expand-jump',''])\<CR>" :
      \ <SID>check_back_space() ? "\<TAB>" :
      \ coc#refresh()

" GoTo 跳转
nmap <leader>gd <Plug>(coc-definition)     " 跳到定义
nmap <leader>gr <Plug>(coc-references)     " 查找引用
nmap <leader>rn <Plug>(coc-rename)         " 重命名

" 显示文档
nnoremap <leader>k :call CocActionAsync('doHover')<CR>

" 安装 Coc 扩展：
" :CocInstall coc-java        → Java LSP
" :CocInstall coc-json        → JSON
" :CocInstall coc-yaml        → YAML
" :CocInstall coc-html        → HTML/CSS
" :CocInstall coc-tsserver    → TypeScript
```

### 4.2 LSP 功能一览

```text
Coc.nvim 提供的能力：

  ✅ 自动补全（模糊匹配 + 代码片段）
  ✅ 跳转定义 / 查找引用
  ✅ 变量重命名
  ✅ 悬停文档
  ✅ 代码诊断（错误警告标注）
  ✅ 代码格式化
  ✅ 自动 import
```

---

## 5. Java 开发配置

### 5.1 一键编译运行

```vim
" ~/.vimrc — Java 开发快捷键

" 编译当前文件
nnoremap <leader>jc :!javac %<CR>

" 运行当前文件
nnoremap <leader>jr :!java %:r<CR>

" 编译 + 运行（Maven 项目）
nnoremap <leader>mt :!mvn test -Dtest=%:r<CR>

" Maven 编译
nnoremap <leader>mc :!mvn clean compile<CR>

" Spring Boot 启动
nnoremap <leader>mb :!mvn spring-boot:run<CR>
```

### 5.2 必备开发习惯

```text
Vim 作为 Java IDE 的工作流：

  1. Ctrl+p → 快速打开文件（替代鼠标点击）
  2. 空格+n → 文件树浏览项目结构
  3. gd → 跳转到方法定义
  4. gr → 查看所有引用
  5. 空格+rn → 重命名变量/方法
  6. gc → 快速注释/取消注释（vim-commentary）
  7. cs"' → 把双引号改成单引号（surround.vim）
  8. 空格+f → 全文搜索某个关键字

  配合终端：另一 Tab 跑 mvn / docker / curl
```

### 5.3 surround.vim 实战

```text
surround.vim — 快速修改包围字符：

  cs"'     → 把 "hello" 改成 'hello'（change surround）
  cs"{     → 把 "hello" 改成 {hello}
  ds"      → 删除双引号： "hello" → hello（delete surround）
  ysiw)    → 给单词加括号： word → (word)
  yss(     → 给整行加括号
  S<p>     → 可视模式选中后加 HTML 标签
```

---

> 🎯 **必装三件套**：**fzf**（模糊查找）→ Ctrl+p 打开任何文件；**Coc.nvim**（LSP）→ VS Code 级别的代码补全和跳转；**surround.vim** → 秒改引号括号。三者配齐 = 不需要 IDE 的鼠标操作。

---

**返回总览**：[00-Vim知识体系总览](./00-Vim知识体系总览.md)

---

*创建于：2026年7月*
