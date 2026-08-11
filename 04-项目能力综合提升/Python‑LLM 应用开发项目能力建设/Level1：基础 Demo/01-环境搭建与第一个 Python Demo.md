# 01 环境搭建与第一个 Python Demo

> Level1 的第一块地基：装好 Python 3.14 + uv 虚拟环境，配置 API Key，跑通"Python 脚本 → 大模型"的第一条链路。环境是后续全部 Demo 的载体，本模块必须一次配到位。

## 📚 目录

1. [目标与验收](#1-目标与验收)
2. [装 Python：用 uv 一把梭](#2-装-python用-uv-一把梭)
3. [建项目：uv init 与虚拟环境](#3-建项目uv-init-与虚拟环境)
4. [配密钥：.env 文件与 API Key](#4-配密钥env-文件与-api-key)
5. [第一个 Demo：调用 DeepSeek 冒烟](#5-第一个-demo调用-deepseek-冒烟)
6. [环境验证清单与常见坑](#6-环境验证清单与常见坑)
7. [uv 常用命令速查](#7-uv-常用命令速查)

---

## 1. 目标与验收

本模块的产出：一个能独立运行的最小 Python 项目（pyproject.toml + uv.lock + .venv + main.py + .env），终端执行 `uv run main.py` 能打印出大模型的回答。验收标准：**能讲清 uv、pip、venv 三者的关系**；**知道 .env 为什么不该提交进 Git**；**换一台新电脑能在 15 分钟内复现这套环境**。版本基线（2026-08）：Python 3.14.6（3.14 系列最新补丁版）、uv 0.11.x（需 0.9+ 才能稳定安装 3.14）。

## 2. 装 Python：用 uv 一把梭

2026 年 Python 环境管理的推荐做法是 **uv**——Rust 写的工具，替代 pip + venv + pyenv + poetry 四件套，安装依赖比 pip 快 10-100 倍。uv 可以自管理 Python 解释器：不用先去官网装 Python，uv 会按需下载。

Windows 安装 uv（PowerShell）：

```powershell
powershell -ExecutionPolicy ByPass -c "irm https://astral.sh/uv/install.ps1 | iex"
```

安装后用 uv 装 Python 3.14：

```bash
uv python install 3.14
uv python list   # 确认已安装
```

两个细节：uv 版本必须 **0.9 或更新**，老版本会把 3.14 解析成预发布版；如果之前用旧 uv 装过，执行 `uv self update` 后再 `uv python install 3.14.0 --reinstall` 重装。装完检查 `python --version` 输出 3.14.x——注意在 PowerShell 里 `python` 可能指向 Windows 商店的占位符，用 `uv python list` 看 uv 管理的解释器更可靠。

为什么用 uv 而不是传统组合，值得花两分钟想清楚（面试可能被问）：**pip + venv 的组合**是官方标配但慢且麻烦（装包先下载再安装、环境要手动激活）；**conda** 体积大且与 pip 生态有依赖打架的隐患；**uv** 一个工具管解释器安装、依赖解析、虚拟环境、锁文件，Rust 实现解析快一个数量级。2026 年新项目的社区共识就是 uv——本目录所有 Demo 统一用它，遇到"环境不一致"问题先检查 uv 版本与命令。

## 3. 建项目：uv init 与虚拟环境

在项目目录（建议路径不要有中文与空格，后续所有 Level 的代码都放这里）执行：

```bash
uv init python-llm-level1    # 生成 pyproject.toml、.python-version、main.py、README
cd python-llm-level1
uv add openai python-dotenv   # 安装依赖，自动创建 .venv 并生成 uv.lock
uv run main.py                # 在虚拟环境里运行，无需手动激活
```

理解这套工作流的三块基石：**pyproject.toml** 是项目清单（依赖、Python 版本、构建信息），取代了老式 requirements.txt；**uv.lock** 锁定每个依赖的精确版本，提交进 Git，别人 `uv sync` 一键还原完全一致的环境——这是"环境可复现"的根；**.venv** 是本项目的隔离运行环境（Python 解释器 + 装好的包），不同项目依赖互不污染。`uv run` 会自动找 .venv 里的解释器执行，也可以 `source .venv/Scripts/activate`（Windows 是 `.venv\Scripts\Activate.ps1`）激活后直接用 `python` 命令——Demo 阶段用 `uv run` 更省心，不依赖激活状态。

项目目录的结构建议（Level1 全系列共用这一个项目目录，每个 Demo 一个文件）：

```text
python-llm-level1/
├── pyproject.toml      # 项目清单：依赖与 Python 版本
├── uv.lock             # 锁文件：精确版本，提交 Git
├── .python-version     # 项目绑定的 Python 版本
├── .env                # 密钥，已加入 .gitignore
├── .gitignore          # 至少包含 .env、.venv、__pycache__、data/
├── main.py             # 01 篇：冒烟脚本
├── app.py              # 05 篇起的 Streamlit 界面
└── data/               # 文档与 Chroma 数据目录
```

这个结构与 Level2 工程化目录是同一套骨架的雏形——现在就养成"密钥不进库、锁文件必提交、数据与代码分离"三个习惯，后面全是受益。

## 4. 配密钥：.env 文件与 API Key

AI 应用的密钥（API Key）绝不能硬编码进代码。标准做法：项目根目录建 `.env` 文件存密钥，代码里用 `python-dotenv` 读取：

```bash
# .env  （已加入 .gitignore，永不提交）
DEEPSEEK_API_KEY=sk-你的Key
```

先去 DeepSeek 开放平台（platform.deepseek.com）注册并创建 API Key——充值少量余额即可（本层全部 Demo 消耗不足 1 元），新用户常有赠送额度。Key 的格式是 `sk-` 开头，它是你的"数据库密码"：泄露了别人能用你的账号调模型，所以务必在 .gitignore 里加 `.env`。验证：`uv add` 时已装 python-dotenv，在 main.py 里 `from dotenv import load_dotenv; load_dotenv()` 后 `os.getenv("DEEPSEEK_API_KEY")` 能取到值即配置成功。

Key 泄漏与误提交的处理预案（提前知道，遇到不慌）：发现 Key 已经提交进 Git 仓库时，**第一动作是去平台删除/重置该 Key**（泄漏即作废，不要只删 Git 历史——历史里还能翻出来），然后重新生成 Key 更新 .env；如果 Git 仓库是公开的（GitHub），还应考虑平台是否提供用量告警（DeepSeek 平台支持额度告警设置），平时养成"充值小额、定期看用量"的习惯——AI 应用的钱包意识从 Key 管理开始。

## 5. 第一个 Demo：调用 DeepSeek 冒烟

写完环境就做第一个真 AI 调用——这比 hello world 更有意义，直接验证"密钥 + 网络 + 协议"三段链路：

```python
# main.py
import os
from dotenv import load_dotenv
from openai import OpenAI

load_dotenv()

client = OpenAI(
    api_key=os.getenv("DEEPSEEK_API_KEY"),
    base_url="https://api.deepseek.com/v1",  # /v1 后缀必加，漏掉会 404
)

resp = client.chat.completions.create(
    model="deepseek-v4-flash",
    messages=[{"role": "user", "content": "用一句话介绍你自己"}],
)
print(resp.choices[0].message.content)
```

两个必背细节：**base_url 必须带 /v1**（openai SDK 会自动拼接 /chat/completions，漏掉后缀所有请求 404）；**模型名用 deepseek-v4-flash**——2026-08 的 V4 新命名，旧的 deepseek-chat / deepseek-reasoner 已于 2026-07-24 弃用。运行 `uv run main.py`，终端打印出模型回答即全链路打通。价格参考：flash 输入约 1 元/百万 token、输出约 2 元/百万 token，本 Demo 消耗不到 1 分钱。

验证的两个技巧：**确认 Key 真的读到了**——在调用前临时 `print(os.getenv("DEEPSEEK_API_KEY")[:6])` 打印前 6 位（不要打印全量，Key 会留在终端历史里），看到 sk- 开头且与平台一致即读到了；**确认响应结构**——`print(resp.usage)` 看 token 消耗（prompt_tokens + completion_tokens），这行输出能顺便验证计费数据在哪取。如果打印出 None，回第 4 节检查 .env 与 load_dotenv 的调用顺序；如果报 401，检查 Key 是否复制完整（`sk-` 后是长串，常见的复制残缺是只复制了前缀）。

## 6. 环境验证清单与常见坑

环境类问题在后续所有 Demo 都会遇到，一次配好后面全是坦途。验证清单：`uv --version` 正常输出；`uv python list` 有 3.14.x；`uv run python -c "import openai"` 不报错；.env 里 Key 能读取；上面冒烟脚本能打印回答。

常见坑按出现频率排：**`python` 命令指向错误解释器**（Windows 商店占位符或全局解释器）——统一用 `uv run`，不依赖系统 python；**uv 解析到 Python 3.14 预发布版**——升级 uv 到 0.9+ 后重装；**.env 没生效**——load_dotenv() 要在读取 Key 之前调用，且 .env 必须放在运行目录（uv run 以项目根为工作目录，一般没问题）；**API Key 读取为 None**——检查 .env 是否有引号残留（`KEY="sk-xxx"` 两边引号要去掉）或编码是否为 UTF-8；**公司网络代理导致连不上**——先 `curl.exe https://api.deepseek.com/v1/chat/completions` 测连通性（注意 PowerShell 里 curl 是别名，要用 curl.exe），代理问题在 09 篇有专门处理。

Windows 特有的两个细节：**安装 uv 后新终端才能用**（install 脚本修改了用户 PATH，已打开的终端不会自动刷新——重开 PowerShell 或执行 `refreshenv`）；**PowerShell 执行策略**——install 脚本开头已带 `-ExecutionPolicy ByPass`，如果手动跑别的 ps1 报"禁止运行脚本"，用 `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` 放开当前用户即可（只影响当前用户，不动系统策略）。这两个都是 Windows 新手最常见的"装好了但用不了"的原因。

## 7. uv 常用命令速查

本层会用到的 uv 命令不多，一次记全： 

| 命令 | 作用 | 说明 |
|---|---|---|
| `uv python install 3.14` | 安装 Python 3.14 | 首次使用会自动下载 |
| `uv python list` | 列出已安装解释器 | 排查"python 不对"的利器 |
| `uv init` | 初始化新项目 | 生成 pyproject.toml 与目录 |
| `uv add openai` | 安装依赖 | 自动建 .venv、自动更新锁文件 |
| `uv remove openai` | 卸载依赖 | 同步清理锁文件 |
| `uv run main.py` | 在虚拟环境运行 | 无需手动激活，推荐主用法 |
| `uv sync` | 按锁文件还原环境 | 新电脑克隆项目后的第一步 |
| `uv tree` | 查看依赖树 | 排查版本冲突 |
| `uv self update` | 升级 uv 自身 | 版本过老时的首选修复 |

三个使用习惯：**依赖一律 uv add 管理**（不要手动 pip install，否则装进了错误的解释器）；**uv.lock 每次提交 Git**（它是环境可复现的保证）；**克隆项目后先 uv sync 再跑**（15 分钟复现的最后一环）。记不清某个命令时 `uv --help` 就在手边。

> 🎯 **核心要点**：环境搭建的唯一标准是"**15 分钟在新机器复现**"——uv 管解释器、uv.lock 锁依赖、.env 藏密钥，三者齐全这套环境才真正合格。第一个 AI 调用同时验证了密钥、网络、协议三段，是本层所有 Demo 的起跑线。

---

**下一模块**：[02 Python 核心语法快速 Demo](./02-Python%20核心语法快速%20Demo.md) | **返回总览**：[Level1 总览](./00-Level1%20基础%20Demo%20总览.md)

【参考来源】
- [How to Install Python 3.14 with uv](https://training.talkpython.fm/installing-python)
- [uv 0.9.30 on Python PyPI](https://newreleases.io/project/pypi/uv/release/0.9.30)
- [Using OpenAI SDK with DeepSeek base URL](https://theneuralbase.com/deepseek-api/learn/beginner/using-openai-sdk-with-deepseek-base-url/)
- [模型 & 价格 | DeepSeek API Docs](https://api-docs.deepseek.com/zh-cn/quick_start/pricing/)
