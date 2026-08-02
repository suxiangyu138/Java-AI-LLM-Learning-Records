# 00 - Python 模块对象知识体系总览

> 🎯 Python 的"模块"和"对象"是 AI 开发的两大基石 — 模块决定你能用什么，对象决定你怎么用。本体系分两条主线：① 标准库模块按应用领域分类速查（系统/数据/类型/并发）② Python 对象模型深度（魔术方法/描述符/元类）。聚焦 AI 开发场景

---

## 目录

1. [知识全景](#1-知识全景)
2. [文件导航](#2-文件导航)
3. [与现有 Python 体系的互补关系](#3-与现有-python-体系的互补关系)
4. [核心概念速查](#4-核心概念速查)

---

## 1. 知识全景

```
Python 模块对象体系（8个文件 — 标准库速查 + 对象模型深挖 + AI第三方模块）
│
├── 📦 模块系统（01）
│   └── 01-Python模块系统与导入机制.md     # import/__init__/命名空间包/相对导入/循环导入
│
├── ⚙️ 系统交互（02）
│   └── 02-系统交互与文件处理模块.md        # os/sys/pathlib/subprocess/shutil/tempfile/argparse/logging
│
├── 📊 数据处理（03）
│   └── 03-数据处理与序列化模块.md          # json/csv/re/collections/itertools/functools/datetime/hashlib
│
├── 🏷️ 类型系统（04）
│   └── 04-类型系统与结构化模块.md          # typing/dataclasses/enum/abc/Protocol/NamedTuple
│
├── 🧬 对象模型（05）
│   └── 05-Python对象模型与魔术方法.md      # __init__/__call__/__getitem__/描述符/元类
│
├── ⚡ 并发异步（06）
│   └── 06-并发与异步编程模块.md            # asyncio/threading/multiprocessing/concurrent.futures
│
├── 🤖 AI 第三方对象（07）
│   └── 07-AI开发核心第三方模块对象.md       # numpy/pandas/torch/PIL/httpx 核心对象
│
└── 📌 00-Python模块对象知识体系总览.md       # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | 总览 | 全景导航 + 分工 + 速查 | — |
| 01 | 模块系统 | import 机制/包/命名空间/循环导入解决 | ⭐⭐ |
| 02 | 系统交互模块 | os/sys/pathlib/subprocess/logging 共 8 个模块 | ⭐⭐⭐ |
| 03 | 数据处理模块 | json/re/collections/itertools/functools 共 7 个 | ⭐⭐⭐ |
| 04 | 类型与结构化 | typing/dataclasses/enum/Protocol 共 6 种 | ⭐⭐ |
| 05 | 对象模型 | 魔术方法 20+ 张表 + 描述符 + 元类实战 | ⭐⭐⭐ |
| 06 | 并发与异步 | asyncio/threading/multiprocessing 三大范式 | ⭐⭐ |
| 07 | AI 第三方对象 | numpy.ndarray/pandas.DataFrame/torch.Tensor/PIL.Image | ⭐⭐ |

---

## 3. 与现有 Python 体系的互补关系

| 已有系统 | 做什么 | 本体系补充什么 |
|----------|--------|----------------|
| `07-Python语言` | 基础语法（变量/循环/函数/类） | 模块导入机制 + 对象模型深挖 |
| `Python高级语法` | 装饰器/生成器/上下文管理器 | 描述符/元类/属性访问底层机制 |
| `Python生态/`（刚建成） | Agent框架/项目管理/uv/HF | **标准库模块速查 + 对象模型原理** |
| `pip/` | 包管理 | 模块安装后的导入机制 |
| `Python数据分析/` | 数据分析实践 | numpy/pandas 核心对象 ndarray/DataFrame |

> 💡 **定位**：已有 Python 系统教"怎么用"，本体系教"怎么组织的（模块系统）& 为什么能这样用（对象模型）"。

---

## 4. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| 模块 | 一个 `.py` 文件 = 一个模块对象，`import` 即执行 |
| 包 | 含 `__init__.py` 的目录，可嵌套 |
| 命名空间包 | 无 `__init__.py` 的包（Python 3.3+），允许多目录拼合 |
| 魔术方法 | `__xxx__` 格式、由解释器自动调用的 80+ 个特殊方法 |
| 描述符 | 实现 `__get__`/`__set__`/`__delete__` 的类，控制属性访问 |
| 元类 | "创建类的类"，默认是 `type`，99% 时候不需要 |
| `sys.modules` | 所有已加载模块的字典缓存 |
| `ndarray` | numpy 的核心对象：同质多维数组，AI 数据的通用交换格式 |
| `Tensor` | PyTorch 的核心对象：可 GPU 加速的多维数组，支持自动微分 |

---

> 🎯 **核心要点**：Python 模块对象 = 两条学习主线 — **① 标准库按领域速查**（遇到需求知道用哪个模块）**② 对象模型理解**（知道 `obj.attr`、`obj[key]`、`obj()` 背后的机制）。AI 开发中四条最重要的模块线：`os/pathlib`（文件管理）→ `json/dataclasses`（数据序列化）→ `asyncio`（LLM 并发调用）→ `numpy/torch`（模型数据处理）。

**下一模块**：[01-Python模块系统与导入机制](01-Python模块系统与导入机制.md)
