# 00 - Python 学习体系总览（Java 后端视角）

> 🎯 Python 是 AI 开发的"普通话" — 作为 Java 后端，不必成为 Python 专家，但必须能用 Python 调模型、写脚本、跑实验

## 1. 知识全景

```
Python 体系（12个文件）— Java后端做AI的最低必要知识
│
├── 🏗️ 语法篇（01-03）
│   ├── 01-语法速通-Java后端视角.md     # 用Java对照学Python/变量/控制流/集合/OOP
│   ├── 02-进阶语法与工程基础.md         # 装饰器/生成器/上下文管理器/类型注解/异常
│   └── 03-标准库与脚本利器.md           # os/pathlib/json/re/argparse/自动化脚本
│
├── 🔬 AI生态篇（04-07）
│   ├── 04-AI与数据科学生态.md           # NumPy/Pandas/Matplotlib/Jupyter
│   ├── 05-Python与Java联动实战.md       # FastAPI服务/进程调用/gRPC/Java调Python
│   ├── 06-LLM开发必备Python技能.md       # API调用/流式/异步/Token计数/LangChain最小集
│   └── 07-PyTorch与Transformers实战.md   # 张量/训练循环/微调/HuggingFace Pipeline
│
├── 🛠️ 工程篇（08-09）
│   ├── 08-开发环境与工具链.md            # Conda/venv/poetry/VSCode/Jupyter
│   └── 09-项目结构与工程化.md            # 项目布局/打包/测试/lint/CI
│
├── 📋 实战篇（10-11）
│   ├── 10-Python脚本实战集.md            # 10个高频实用脚本模板
│   └── 11-Python面试题与速查.md          # 30道面试题+语法速查卡
│
└── 📌 00-Python学习体系总览.md            # ← 本文件
```

## 2. 文件导航

| # | 文件 | 核心 | 级别 |
|---|------|------|:---:|
| 00 | 体系总览 | 全景+Java对照路线 | — |
| 01 | 语法速通 | Java vs Python对照/基础语法 | ⭐⭐⭐⭐ |
| 02 | 进阶语法 | 装饰器/生成器/类型注解/异常 | ⭐⭐⭐ |
| 03 | 标准库 | os/pathlib/json/re/argparse | ⭐⭐⭐⭐ |
| 04 | AI生态 | NumPy/Pandas/Matplotlib/Jupyter | ⭐⭐⭐⭐ |
| 05 | Java联动 | FastAPI/进程调用/gRPC | ⭐⭐⭐ |
| 06 | LLM开发 | API/流式/异步/Token/LangChain | ⭐⭐⭐⭐ |
| 07 | PyTorch | 张量/训练/HuggingFace Pipeline | ⭐⭐⭐⭐ |
| 08 | 环境工具 | Conda/venv/poetry/VSCode | ⭐⭐ |
| 09 | 项目工程 | 布局/打包/pytest/lint/CI | ⭐⭐ |
| 10 | 脚本集 | 10个实用脚本模板 | ⭐⭐⭐ |
| 11 | 面试速查 | 30题+语法速查卡 | ⭐⭐⭐ |

## 3. Java 后端学习 Python 的最低路线

```text
🟢 能跑代码（2小时）：
  01-语法速通 → 08-环境 → 跑通一个脚本

🔵 能做 AI（4小时）：
  04-NumPy/Pandas → 07-PyTorch → 06-LLM开发 → 调用API/运行模型

🟣 工程化（2小时）：
  02-进阶 → 03-标准库 → 05-Java联动 → 09-项目工程
```
