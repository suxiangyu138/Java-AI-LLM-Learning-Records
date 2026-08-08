# Python 环境与爬虫工具箱
> 动手第一课：Python 3.14 安装、venv/uv 虚拟环境、爬虫三件套（requests/beautifulsoup4/lxml）、命令行工具与 IDE——先把"工具架"搭好

## 📚 目录
1. [Python 版本选择（2026）](#1-python-版本选择2026)
2. [安装与验证](#2-安装与验证)
3. [虚拟环境：venv 与 uv](#3-虚拟环境venv-与-uv)
4. [爬虫三件套安装](#4-爬虫三件套安装)
5. [命令行工具箱](#5-命令行工具箱)
6. [IDE 与开发习惯](#6-ide-与开发习惯)

## 1. Python 版本选择（2026）

| 版本 | 状态 | 说明 |
|------|------|------|
| **3.14.6** | 当前稳定（2026-06-10） | **学爬虫选它** |
| 3.15.0 | 将于 2026-10-01 发布（PEP 790） | 新特性预览期，先不用 |
| 3.12 / 3.13 | 稳定维护期 | 老项目/生态兼容考虑 |
| 3.11 及以下 | 接近 EOL | 不建议新装 |

> 🎯 版本结论：**装 3.14.x 最新维护版**。爬虫生态（requests/bs4/lxml/playwright）对 3.14 兼容良好；等 3.15 正式发布（2026-10）后按需升级。注意 3.15 将启用 **UTF-8 默认编码**（PEP 686，乱码问题将大幅减少，见 [05-数据格式与编码](05-数据格式与编码.md)）。

## 2. 安装与验证

```bash
# Windows：python.org 下载安装包，务必勾选 "Add Python to PATH"
# macOS：brew install python@3.14
# Linux：apt install python3.14 / 官方源码编译

# 验证（终端执行）
python --version        # Python 3.14.6
pip --version           # pip 25.x

# 交互式验证爬虫三件套
python -c "import requests, bs4, lxml; print('crawler toolchain OK')"
```

> ⚠️ Windows 坑：① 忘勾 PATH → `python` 不是内部命令（重装或手动加环境变量）；② 微软商店版与官网版并存 → 命令冲突（统一用官网版）。

## 3. 虚拟环境：venv 与 uv

```bash
# ═══ 方式一：venv（Python 内置，零依赖）═══
python -m venv venv                # 创建
venv\Scripts\activate              # Windows 激活
source venv/bin/activate           # macOS/Linux 激活
pip install requests beautifulsoup4 lxml

# ═══ 方式二：uv（2026 推荐，极快 + 锁文件）═══
# pip install uv  或 独立安装脚本
uv venv                            # 创建虚拟环境（毫秒级）
uv pip install requests beautifulsoup4 lxml
uv pip freeze > requirements.txt   # 依赖锁定
```

| 工具 | 速度 | 锁文件 | 2026 定位 |
|------|:---:|:---:|---------|
| venv | 中 | ❌ | 内置兜底，必会 |
| **uv** | 极快（Rust） | ✅ | **新项目首选** |
| conda | 慢 | 部分 | 数据科学环境 |

> 🎯 环境隔离是**第一习惯**：每个爬虫项目独立 venv——全局装包版本冲突（requests 升级破坏兼容）是新手第一灾难。uv 的 lock 文件让"换机器/上服务器"一条命令复现。

## 4. 爬虫三件套安装

```bash
# 静态网页爬虫的标准三件套（2026 教程一致推荐）
uv pip install requests beautifulsoup4 lxml

# 版本参考（2026-08）
# requests        2.34.x（2026-05 发布 2.34.2）
# beautifulsoup4  4.12+
# lxml            5.x（bs4 的高速解析后端）
```

| 库 | 用途 | 什么时候用 |
|----|------|-----------|
| **requests** | HTTP 请求 | 所有静态页面抓取（阶段 2 主力） |
| **beautifulsoup4** | HTML 解析 | 从 HTML 里提取数据（阶段 2 主力） |
| **lxml** | 高速解析 + XPath | bs4 后端 / 大规模数据 / XPath 场景 |

```python
# 装完立即验证三件套协同工作：
import requests
from bs4 import BeautifulSoup

resp = requests.get("https://example.com", timeout=10)
soup = BeautifulSoup(resp.text, "lxml")      # lxml 作解析后端
print(soup.title.text)                        # 页面标题
```

> 💡 依赖关系：**bs4 的解析器可插拔**——`html.parser`（内置，慢）/ `lxml`（快、容错强，**推荐**）/ `html5lib`（最标准，慢）。爬虫一律 `"lxml"` 后端。其他场景库（Selenium/Playwright/aiohttp）到对应阶段再装。

## 5. 命令行工具箱

| 工具 | 用途 | 场景 |
|------|------|------|
| **curl** | 命令行 HTTP 请求 | 快速验证接口/看响应头（见 07） |
| **ping / nslookup** | 网络连通性 | 服务器连不上时先查网络 |
| **python -m http.server** | 本地静态服务器 | 测试本地页面 |
| **redis-cli / mysql** | 存储相关 | 阶段 3 用 |
| **jq** | JSON 格式化 | 接口返回乱成一团时格式化查看 |

```bash
# curl 四连（爬虫调试基础）
curl -i https://example.com              # 看响应头+体
curl -I https://example.com              # 只看头（轻量探测）
curl -A "Mozilla/5.0" https://example.com  # 自定义 UA
curl -b "session=abc" https://example.com  # 带 Cookie
```

> 🎯 铁律：**写爬虫前先用 curl 把接口调通**——curl 通 → 是 Python 代码问题；curl 不通 → 是网络/接口问题。curl 是爬虫的"探针"（[阶段 2](../阶段%202：静态网页爬虫（入门）/00-阶段2静态网页爬虫总览.md) 会全程用它）。

## 6. IDE 与开发习惯

| 工具 | 定位 | 建议 |
|------|------|------|
| **VS Code** | 轻量主力 | Python 扩展 + 内置终端 + 调试器 |
| **PyCharm** | 重量级 | 爬虫项目/调试体验佳（社区版免费） |
| **Jupyter** | 交互式探索 | 数据清洗/接口调试（阶段 3 用） |

```text
爬虫开发习惯（第一天就养成）：
  ① 每个项目一个 venv + requirements.txt
  ② 请求和解析分开写（先拿到 HTML，再解析）
  ③ 响应先打印状态码与头，再处理内容
  ④ 每个请求都设 timeout（永不裸奔）
  ⑤ 脚本文件命名：spider_xxx.py / tools_xxx.py
```

> 💡 与 [Python 体系](../../Python/00-Python学习体系总览.md) 的分工：本阶段只讲"爬虫需要的最小环境"；Python 语法、工程化、工具链深挖见该体系（[开发环境与工具链](../../Python/08-开发环境与工具链.md)、[虚拟环境体系](../../Python虚拟环境/00-Python虚拟环境知识体系总览.md)）。

---

**下一模块**：[02-HTTP协议基础（爬虫视角）](02-HTTP协议基础（爬虫视角）.md) / **返回总览**：[00-阶段1前置基础总览](00-阶段1前置基础总览.md)
