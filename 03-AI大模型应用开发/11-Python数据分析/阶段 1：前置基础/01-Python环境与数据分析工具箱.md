# 01-Python 环境与数据分析工具箱

> 学习路径第一站的第一课：装好 Python 3.14、学会 venv-uv 隔离、装齐三件套（NumPy/Pandas/Matplotlib）、跑通 JupyterLab——"环境装不好，后面全白学"

---

## 📚 目录

1. [环境全景](#1-环境全景)
2. [Python 与版本选择](#2-python-与版本选择)
3. [虚拟环境：venv 与 uv](#3-虚拟环境venv-与-uv)
4. [数据分析三件套](#4-数据分析三件套)
5. [JupyterLab 工作台](#5-jupyterlab-工作台)
6. [Windows 环境特有坑与可选方案](#6-windows-环境特有坑与可选方案)
7. [常见问题速查](#7-常见问题速查)
8. [装库顺序与依赖心智](#8-装库顺序与依赖心智)
9. [Python 3.14 的免费线程说明](#9-python-314-的免费线程说明)
10. [环境验证清单](#10-环境验证清单)

---

## 1. 环境全景

数据分析的环境 = **四件东西**：Python 解释器（3.14.6）、虚拟环境（venv/uv——项目隔离）、三件套库（NumPy/Pandas/Matplotlib）、工作台（JupyterLab）——**顺序**：装 Python → 建虚拟环境 → 装库 → 起工作台——**"环境是项目级的，不是全局级的"**（不同项目不同依赖——这是环境的第一原则）。

**2026-08 版本基线**：Python 3.14.6（3.15 将于 2026-10 发布——PEP 686 将 UTF-8 设为默认编码）；NumPy 2.5.1；Pandas 3.0.3（3.0 是 2026 年初的大版本——需要 Python 3.11+，从 2.3 升级路径平滑）；Matplotlib ≥3.10；JupyterLab ≥4.5——**写文档时以本基线为准，安装时以 pip 实际解析为准**（`pip list` 验证）。

## 2. Python 与版本选择

**下载**：python.org 下载 3.14.x（Windows 安装时**勾选 "Add python.exe to PATH"**——这是新手第一坑：没勾 PATH 导致 `python` 命令找不到）。**验证**：

```bash
python --version    # Python 3.14.6
```

**版本纪律**：数据分析优先稳定版（3.14 已稳定）；**不装 3.13 以下**（pandas 3.0 需 Python 3.11+，但新项目直接上 3.14）；**多个版本共存用 `py` 启动器**（Windows：`py -3.14` 指定版本）。

## 3. 虚拟环境：venv 与 uv

**为什么必须隔离**：不同项目需要不同版本的库（项目 A 要 pandas 2.x、项目 B 要 3.x）——**全局混装 = 版本地狱**。**两种工具**：

**venv（官方内置，零安装）**：

```bash
python -m venv .venv          # 建环境（目录名 .venv 是惯例）
.venv\Scripts\activate        # Windows 激活（PowerShell 允许脚本需先: Set-ExecutionPolicy -Scope CurrentUser RemoteSigned）
pip install pandas            # 装库——只进当前环境
```

**uv（2026 主流，快 10-100 倍）**：替代 pip/venv 的现代工具（Rust 编写、锁文件、极快）——**新项目推荐直接 uv**：

```bash
uv init data-proj && cd data-proj   # 初始化项目（自动建 .venv）
uv add pandas numpy matplotlib      # 装库 + 写 pyproject.toml
uv run jupyter lab                  # 在环境里运行
```

**激活与纪律**：命令行出现 `(.venv)` 前缀 = 环境已激活；**装库前先确认在哪个环境**（`which python` / `where python`）；**环境坏了重建**（删 .venv 重来——比修快）。

## 4. 数据分析三件套

```bash
pip install numpy pandas matplotlib    # 三件套一次装齐（pandas 会带上 numpy）
```

**三件套分工**（2026 基线：NumPy 2.5.1 / Pandas 3.0.3 / Matplotlib ≥3.10）：

| 库 | 职责 | 一句话 |
|------|------|--------|
| NumPy | 数值计算 | 多维数组 + 数学运算——Pandas 的地基 |
| Pandas | 表格分析 | DataFrame——数据分析的"货币" |
| Matplotlib | 可视化 | 画图——"图即证据" |

**验证**（环境健康检查——`python -c` 一行验证）：

```bash
python -c "import numpy, pandas, matplotlib; print(numpy.__version__, pandas.__version__, matplotlib.__version__)"
# 输出示例: 2.5.1 3.0.3 3.10.9 —— 三个版本号都出来 = 三件套就绪
```

**版本纪律**：`pip freeze > requirements.txt` 记录版本（可复现）；**升级前看兼容**（pandas 3.0 升级路径：先升 2.3 清掉 DeprecationWarning 再上 3.0——官方建议）。

## 5. JupyterLab 工作台

**JupyterLab 是数据分析的"IDE"**——单元格式交互（代码 + 结果 + 图表 + 笔记同屏）——**为什么不用普通 IDE**：数据分析是"探索式"的（跑一步看一步——图表/中间结果随时可见），Jupyter 的单元格就是为这个设计的。

```bash
pip install jupyterlab
jupyter lab                    # 启动（浏览器自动打开 http://localhost:8888）
```

**工作台纪律**：一个项目一个 notebook 目录；**文件名用语义名**（`titanic_eda.ipynb` 而不是 `未命名.ipynb`）；**代码和结果都在 notebook 里**（可复现——这是面试展示的素材）；详细操作见 `07-Jupyter Notebook 实战.md`。

## 6. Windows 环境特有坑与可选方案

**Windows 三大特有坑**：

| 坑 | 现象 | 解法 |
|------|------|------|
| PATH 未配置 | `python` 命令找不到 | 重装勾选 "Add to PATH" 或手动加环境变量 |
| PowerShell 执行策略 | 激活 venv 报错 | `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`（一次即可） |
| 中文路径 | 某些库/工具对中文路径敏感 | 项目目录**用英文**（`D:\data-proj` 优于 `D:\数据`） |

**conda 什么时候用**：需要 Python 版本管理 + 科学计算全家桶（科研/深度学习环境）时用 Anaconda/Miniconda；**纯数据分析项目 venv/uv 足够**（更轻、更快、更标准）——**"不装全家桶也能分析"**是 2026 的共识（pip 生态已成熟——装什么用什么）。

**requirements.txt 的可复现纪律**：

```bash
pip freeze > requirements.txt        # 记录当前环境全部版本
# 换机器/换人：pip install -r requirements.txt  一键复原
```

**"环境能复现"是生产纪律**——面试讲项目时"requirements.txt 一跑就还原环境"是加分项（阶段 4 实战项目会用到）。

## 7. 常见问题速查

| 问题 | 原因 | 解法 |
|------|------|------|
| `python 不是内部或外部命令` | PATH 未配 | 重装勾 PATH / 手动加 |
| `No module named 'pandas'` | 装错环境/没装 | `where python` 确认环境 → `pip install pandas` |
| 下载慢/超时 | 网络 | 换镜像源：`pip install -i https://pypi.tuna.tsinghua.edu.cn/simple pandas` |
| `jupyter 不是内部或外部命令` | 库装了但命令未进 PATH | `python -m jupyter lab`（模块方式必可跑） |
| 杀毒误报 python.exe | 误报 | 添加信任（安全来源的官方安装包） |

## 8. 装库顺序与依赖心智

**装库顺序**（一次装齐时的依赖关系）：`pip install numpy pandas matplotlib jupyterlab`——一条命令 pip 会自动解析依赖（pandas 依赖 numpy、Matplotlib 依赖 numpy/contourpy 等）——**不需要手动分开装**；**只装需要的**（不用 Seaborn 就先不装——"用多少装多少"是环境整洁第一原则）。

**NumPy 的底层**（理解"为什么装 numpy"）：NumPy 是 C 语言实现 + BLAS 优化库（OpenBLAS/MKL——矩阵运算的加速引擎）——**"Pandas 的快来自 NumPy、NumPy 的快来自 BLAS"**——理解这层，装库时就不会困惑"为什么 pandas 自带 numpy"（依赖自动装）。

**升级/降级纪律**：

```bash
pip install --upgrade pandas            # 升级
pip uninstall pandas && pip install pandas==3.0.3   # 指定版本（锁版本用）
```

**"环境崩了怎么办"**：**删掉重建比修快**（`rm -rf .venv` + `uv sync` 或重新 `pip install -r requirements.txt`——5 分钟的事）——**"环境是消耗品，pyproject/requirements 是它的 DNA"**（有锁文件 = 随时重生）。

## 9. Python 3.14 的免费线程说明

**Python 3.14 的实验性自由线程（PEP 703）**：允许真正多线程并行（去掉 GIL）——NumPy 2.5 已支持 free-threaded 构建下的 ufunc 扩展——**对数据分析的影响**：**当前默认构建仍走 GIL**（稳定优先——生态兼容是第一位）；free-threaded 是性能探索方向（多核并行场景）——**版本纪律**：**生产用默认构建、尝鲜在虚拟环境里试**（`python3.14t` 自由线程版——普通用户暂不需要）。

**一句话总结**：Python 3.14 默认构建 + 虚拟环境 + 三件套 + JupyterLab——这是 2026 数据分析的"标准配置"（生态兼容、开箱即用）——**"别让版本焦虑绑架学习——默认构建 + 稳定版永远是第一选择"**（新特性在虚拟环境里尝鲜，生产永远用稳的）。

## 10. 环境验证清单

**毕业检查**（全过 = 环境就绪）：

- ☐ `python --version` 输出 3.14.x
- ☐ 虚拟环境激活（命令行有 `(.venv)` 前缀）
- ☐ 三件套验证命令输出三个版本号
- ☐ `jupyter lab` 能启动并打开浏览器
- ☐ 在 Jupyter 里 `import pandas as pd; pd.DataFrame({"a":[1,2]})` 能显示表格
- ☐ `requirements.txt` 已记录版本

> 🎯 **核心要点**：环境四件套 = Python 3.14.6 + 虚拟环境（venv/uv）+ 三件套（NumPy 2.5.1/Pandas 3.0.3/Matplotlib ≥3.10）+ JupyterLab ≥4.5——**项目级隔离是环境第一原则**；2026-08 基线：Python 3.15 将于 2026-10 发布（PEP 686 UTF-8 默认）、pandas 3.0 是 2026 大版本——**"环境装好且可复现 = 第一课通过"**。

---

**上一模块**：[00-数据分析前置基础总览.md](./00-数据分析前置基础总览.md) / **下一模块**：[02-数据分析全流程与心智模型.md](./02-数据分析全流程与心智模型.md)
