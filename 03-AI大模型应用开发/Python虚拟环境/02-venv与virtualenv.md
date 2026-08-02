# 02 - venv 与 virtualenv

> **核心摘要**：venv 是 Python 标准库内置的虚拟环境工具——零安装、随处可用，是受限环境与教学入门的兜底方案。本文覆盖 venv 完整使用、与 virtualenv 的区别、requirements.txt 管理与升级迁移路径。

> **前置阅读**：[[01-虚拟环境核心原理]]

---

## 📚 目录

1. [venv：标准库的兜底方案](#1-venv标准库的兜底方案)
2. [venv 完整使用指南](#2-venv-完整使用指南)
3. [requirements.txt 管理](#3-requirementstxt-管理)
4. [venv vs virtualenv](#4-venv-vs-virtualenv)
5. [venv 的局限与边界](#5-venv-的局限与边界)
6. [升级路径：venv → uv](#6-升级路径venv--uv)
7. [核心要点](#7-核心要点)

---

## 1. venv：标准库的兜底方案

### 1.1 定位

**venv**：Python 3.3+ 标准库内置的虚拟环境工具（PEP 405 的官方实现）。**零安装、零依赖**——Python 装好就有。

| 维度 | 说明 |
|------|------|
| 来源 | 标准库（`python -m venv`） |
| 安装要求 | 无（Python 3.3+ 自带） |
| 适用场景 | 受限企业环境、临时脚本、教学入门 |
| 2026 地位 | 兜底方案（新项目默认 uv） |

### 1.2 何时选 venv

```text
venv 的适用场景
├── ✅ 受限环境：无法安装第三方工具（内网/无权限）
├── ✅ 临时脚本：快速隔离一下就跑
├── ✅ 教学入门：先懂原理，再上效率工具
├── ✅ Docker 最小化：官方镜像自带 venv
└── ❌ 大型项目：缺锁文件、缺依赖解析、缺 Python 管理
```

> 🎯 **一句话**：venv 是「随时可用的基础方案」——能力有限但永不缺席。

---

## 2. venv 完整使用指南

### 2.1 基础命令（Windows + Linux/macOS）

```bash
# 创建虚拟环境（项目目录内）
python -m venv .venv

# ─── 激活 ───
# Windows (PowerShell)
.venv\Scripts\Activate.ps1
# Windows (cmd)
.venv\Scripts\activate.bat
# Linux/macOS
source .venv/bin/activate

# 验证（应显示 .venv 路径）
which python        # Linux/macOS
where python        # Windows

# 安装依赖
pip install requests

# 退出环境
deactivate
```

### 2.2 常用选项

```bash
# 指定 Python 版本创建
py -3.12 -m venv .venv       # Windows（用 py launcher）
python3.12 -m venv .venv     # Linux/macOS

# 让全局包可见（一般不用）
python -m venv --system-site-packages .venv

# 指定目录名
python -m venv myenv
```

### 2.3 检查环境状态

```bash
# 是否在虚拟环境中
python -c "import sys; print(sys.prefix != sys.base_prefix)"

# 查看环境内已装包
pip list

# 查看 python 指向
where python   # Windows：第一行是 .venv 才对
```

> 💡 **命名规范**：目录用 `.venv`（带点）——Git 默认忽略点文件开头目录，天然防误提交。

---

## 3. requirements.txt 管理

### 3.1 基本用法

```bash
# 导出当前环境的依赖
pip freeze > requirements.txt

# 在新环境安装
pip install -r requirements.txt
```

### 3.2 requirements.txt 的局限

| 局限 | 说明 |
|------|------|
| **无精确锁定** | `pip freeze` 只记录当前版本号，不记录哈希 |
| **无解析器** | 装 A 依赖 B 时手动处理版本冲突 |
| **无环境区分** | 生产/开发依赖混在一起（要手动拆分） |
| **不可复现保证弱** | 同样的文件，不同时间装可能不同（依赖被更新） |

```text
# requirements.txt（冻结版）
flask==3.0.3
requests==2.32.3
pandas==2.2.2

# 生产/开发拆分（手动约定）
requirements.txt          # 生产依赖
requirements-dev.txt      # 开发依赖（-r requirements.txt + 额外）
```

> ⚠️ **venv + requirements.txt 的定位**：够用于简单项目，但不满足现代「精确可复现」要求——需要锁文件时升级 uv/Poetry。

---

## 4. venv vs virtualenv

### 4.1 对比

| 维度 | venv | virtualenv |
|------|------|-----------|
| 来源 | 标准库（Python 3.3+） | 第三方包（`pip install virtualenv`） |
| Python 版本支持 | 3.3+ | 2.7 + 3.3+（历史遗留优势） |
| 创建速度 | 快 | 快（略慢） |
| 需要安装 | 否 | 是 |
| 2026 地位 | **官方推荐（继续使用）** | 兼容 Python 2 时代的遗留 |

### 4.2 结论

> 🎯 **2026 年结论**：Python 3.3+ 一律用 venv——virtualenv 的存在意义（Python 2 支持）已消失，除非维护极老项目。

---

## 5. venv 的局限与边界

```text
venv 的三大边界
├── ① 无锁文件：requirements.txt 只是安装清单，不保证可复现
├── ② 无依赖解析：版本冲突靠手动
├── ③ 无 Python 版本管理：装几个 Python 全靠系统管理
└── 解决：这些边界正是 uv 的强项（见 04 篇）
```

| 能力 | venv | uv | Poetry |
|------|:---:|:---:|:---:|
| 创建环境 | ✅ | ✅ | ✅ |
| 依赖安装 | ✅（pip） | ✅（快 10-100 倍） | ✅ |
| 锁文件 | ❌ | ✅（uv.lock） | ✅（poetry.lock） |
| 依赖解析 | ❌ | ✅ | ✅ |
| Python 管理 | ❌ | ✅（uv python install） | ❌ |

> 💡 **定位总结**：venv 是「打火机」（随手可用），uv/Poetry 是「燃气灶」（功能齐全）——日常做饭用燃气灶，但打火机永远备着。

---

## 6. 升级路径：venv → uv

### 6.1 迁移步骤

```bash
# 1. 在项目目录初始化 uv 项目
uv init

# 2. 从 requirements.txt 导入依赖
uv add -r requirements.txt

# 3. 生成锁文件（提交版本控制）
uv lock

# 4. 同步环境（替代 venv + pip install）
uv sync
```

### 6.2 迁移后的收益

```text
迁移收益
├── 创建环境：秒级（uv 缓存复用）
├── 安装依赖：快 10-100 倍（并行 + 缓存）
├── 可复现：uv.lock 精确锁定（含哈希）
├── 免激活：uv run 直接运行
└── Python 管理：uv python install 自带解释器
```

> 🎯 **迁移铁律**：venv 项目随时可迁 uv（低成本高收益）；但 **Poetry 健康项目不要为速度迁移**（见 04 篇原则）。

---

## 7. 核心要点

> 🎯 **核心要点**：
> 1. venv = 标准库兜底方案：零安装、受限环境/临时脚本/教学首选
> 2. 完整流程：`python -m venv .venv` → 激活 → pip 安装 → `pip freeze > requirements.txt`
> 3. 边界清晰：无锁文件、无解析器、无 Python 管理——大项目必须升级
> 4. 升级路径：`uv init` → `uv add -r requirements.txt` → `uv sync`，秒级完成

---

**下一模块**：[03-conda与mamba](03-conda与mamba.md) | **返回总览**：[00-Python虚拟环境知识体系总览](00-Python虚拟环境知识体系总览.md)
