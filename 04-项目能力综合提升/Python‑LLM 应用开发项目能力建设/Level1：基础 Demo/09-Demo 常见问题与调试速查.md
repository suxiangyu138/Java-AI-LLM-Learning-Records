# 09 Demo 常见问题与调试速查

> Level1 的排错手册：把前面 8 篇踩过的坑集中成五类问题的排查路径（环境、依赖、网络、编码、API 与模型名），按"现象 → 原因 → 修复"三段式组织，卡住先查这里。

## 📚 目录

1. [调试方法论：先分层再动手](#1-调试方法论先分层再动手)
2. [环境类：解释器与虚拟环境](#2-环境类解释器与虚拟环境)
3. [依赖类：安装与版本冲突](#3-依赖类安装与版本冲突)
4. [网络类：连不上与超时](#4-网络类连不上与超时)
5. [编码类：乱码与解析失败](#5-编码类乱码与解析失败)
6. [API 与模型名类：调模型报错](#6-api-与模型名类调模型报错)
7. [错误码速查表](#7-错误码速查表)
8. [排障实战案例：三个真实场景的排查过程](#8-排障实战案例三个真实场景的排查过程)

---

## 1. 调试方法论：先分层再动手

AI 应用调用链长（界面 → 脚本 → SDK → HTTP → 网关 → 模型），报错在哪一层先定位，比直接搜报错文本有效得多。**分层排查**：先确认问题在"我的代码"还是"环境/网络/API"——最快的判定工具是**最小复现**：单独跑一个只含一步操作的脚本（比如只调一次模型、只读一次文件），出错层立即暴露。**排查顺序**：终端报错先读**最后 3 行**（Python 堆栈的根因在 Traceback 底部，前面是调用过程）；网络问题先 curl 再代码；编码问题先看字节再猜。记住一个原则：**先工具后代码**——curl 能调通说明 Key 和网络没问题，问题在自己代码里；curl 都调不通就别改代码了。

两个日常习惯让排查更快：**日志先行**——关键步骤打一行日志（"请求发出""收到响应 200""检索到 3 块"），出问题看日志时间线，比对着报错猜快得多；**复现最小化**——报错堆栈里第一个"你的文件"的帧（过滤掉 SDK 内部帧）就是出错点，把那一行的输入打印出来看。这两个习惯在 Level2 演变成正式的可观测性与调试工具，现在养成是零成本。

## 2. 环境类：解释器与虚拟环境

**`python` 命令不是项目解释器**：Windows 上 `python` 可能指向商店占位符或全局解释器——统一用 `uv run python`，不依赖系统 python；检查 `uv run python -c "import sys; print(sys.executable)"` 输出的路径是否在项目 .venv 内。

**uv 装到 3.14 预发布版**：uv 版本低于 0.9 时 3.14 会解析成 alpha/beta——`uv self update` 后 `uv python install 3.14 --reinstall` 重装。

**模块明明装了却 ImportError**：装到了全局解释器而非 .venv（用 pip install 而非 uv add）——`uv add openai` 统一入口；或运行方式不对（直接 `python app.py` 而没用 `uv run`）。

**uv run 报"项目不是 uv 项目"**：当前目录没有 pyproject.toml——`uv init` 或在项目根目录运行；确认终端工作目录是项目根。

**路径含中文/空格导致奇怪报错**：uv 与部分工具对非 ASCII 路径支持不完整——项目目录建议纯英文（D:\projects\python-llm 之类），本仓库的文档目录是知识库不是代码目录。

## 3. 依赖类：安装与版本冲突

**uv add 很慢或失败**：默认源是 PyPI，国内网络慢——配置镜像源（清华/阿里云 PyPI 镜像），`uv add` 时自动走镜像。

**版本冲突（ResolutionError）**：两个包依赖同一包的不同版本——`uv add "openai>=1.0"` 等显式约束，或 `uv tree` 看依赖树找冲突点；装不进去就先别锁死版本，让 uv 自己解析。

**装完导入报错"module not found"但 uv list 有**：包名与导入名不同（如 `python-dotenv` 导入是 `dotenv`）——报错信息里找"安装包名 vs 导入名"的对应关系，这是最容易被忽略的一类。

**sentence-transformers 首次下载模型失败**：模型文件从 Hugging Face 下载，国内网络需要镜像——设 `HF_ENDPOINT=https://hf-mirror.com` 环境变量（写在代码 import 前或 .env 里），或手动下载模型到本地目录后 `SentenceTransformer("本地路径")`。

## 4. 网络类：连不上与超时

**curl 测试连通性**（PowerShell 用 curl.exe，curl 是 Invoke-WebRequest 别名）：

```bash
curl.exe https://api.deepseek.com/v1/chat/completions -H "Authorization: Bearer sk-xxx" -H "Content-Type: application/json" -d '{"model":"deepseek-v4-flash","messages":[{"role":"user","content":"hi"}]}'
```

**超时**：SDK 默认等待时间长，构造 client 时设 `timeout=30.0`；公司代理环境给 SDK 配 proxy：`OpenAI(..., http_client=requests.Session())` 走系统代理，或直接设环境变量 `HTTPS_PROXY`。

**SSL 证书错误**：代理或自签证书环境——Demo 阶段换网络解决，不推荐 `verify=False` 绕过（安全坏习惯，Level2 讲正规做法）。

**请求成功但响应极慢**：模型思考模式开启时首字慢（thinking 在推理）——非必要关掉 thinking；或高峰期排队（DeepSeek 2026-08 预告的高峰加价时段 9-12 点、14-18 点）。

## 5. 编码类：乱码与解析失败

**文件读写乱码**：Windows 默认编码 GBK，Python 读写文件必须显式 `encoding="utf-8"`——读取用 `errors="replace"` 兜底（个别坏字符变 � 而不崩溃）；终端打印乱码是终端代码页问题（`chcp 65001` 切 UTF-8）。

**json.loads 失败**：模型输出 JSON 里混了说明文字或 `json` 代码块标记——解析前清理（strip 掉围栏标记、截取第一个 { 到最后一个 }）；用 response_format={"type": "json_object"} 从源头限制；解析包 try/except，失败提示模型重试。

**HTTP 响应乱码**：requests 自动按 header 解码一般没问题；手工处理时 `resp.encoding = "utf-8"` 再取 text。

**.env 里 Key 读取带引号**：`DEEPSEEK_API_KEY="sk-xxx"` 引号会变成 Key 的一部分导致 401——.env 值不要加引号，或代码里 `.strip()` 清理。

## 6. API 与模型名类：调模型报错

**401 AuthenticationError**：Key 错误/过期/格式错——curl 验证 Key（第 4 节）；检查 .env 是否真的被读取（代码里 print 一段 Key 前 4 位确认）；检查 base_url 是否拼错。

**404 或模型不存在**：模型名写旧名（deepseek-chat 已弃用）或 base_url 漏 /v1——2026-08 用 deepseek-v4-flash / deepseek-v4-pro；SDK 的 base_url 必须带 /v1 后缀。

**429 RateLimitError**：请求太频繁或余额不足——指数退避重试（02 篇的 call_with_retry）；检查平台余额。

**400 BadRequest**：messages 结构不合法（role 拼错、system 消息重复）、thinking 模式下回传缺 reasoning_content、max_tokens 超限——读响应体里 error.message 的详情字段，它直接告诉你怎么改。

**流式响应解析报错**：把流式 chunk 当完整响应解析——流式响应是增量，`chunk.choices[0].delta.content` 才是文本段；服务端不保证每 chunk 内容完整。

## 7. 错误码速查表

| 错误码/报错 | 现象 | 头号原因 | 修复 |
|---|---|---|---|
| 401 | 鉴权失败 | Key 错误或 .env 没加载 | curl 验证 Key、检查 .env |
| 404 | 端点不存在 | base_url 漏 /v1、模型名错 | 补 /v1、换 V4 新模型名 |
| 429 | 限流 | 请求过频/余额不足 | 退避重试、查余额 |
| 400 | 参数错误 | messages 结构坏、缺 reasoning_content | 读 error.message 详情 |
| ModuleNotFoundError | 导入失败 | 装错环境/包名≠导入名 | uv add 统一入口 |
| UnicodeDecodeError | 读文件崩 | 缺 encoding 参数 | 显式 utf-8 |
| ResolutionError | 依赖冲突 | 版本锁死 | 放宽约束、uv tree 排查 |
| NameError | 变量未定义 | 忘了 walrus 操作符 | `if prompt := st.chat_input(...)` |

## 8. 排障实战案例：三个真实场景的排查过程

方法论的验证靠案例。下面三个是 Level1 学习中出现率最高的场景，按"现象 → 排查 → 根因 → 修复"走一遍：

**案例一：脚本报 401，但昨天还好好的。** 排查：curl 同款请求也 401 → 排除代码问题；检查平台控制台发现 Key 是测试用临时 Key 已过期 → 根因是 Key 生命周期，不是代码 → 修复：重新创建 Key 更新 .env。教训：**401 先 curl，curl 过了看代码，不过就查 Key 本身**。

**案例二：RAG 回答总是"文档中未找到"，明明文档里有答案。** 排查：按 06 篇"先看检索再看生成"——打印 results["documents"]，发现检出来的块全是文档开头几段，与问题无关 → 根因：分块 size=500 从第一段开始切，且 embedding 模型加载失败静默回退成了空向量？不，实际是文档太长、检索 top_k=3 命中的都是前缀块 → 修复：检查 embed 函数是否真正执行（打印向量长度）、top_k 调到 5、分块加段落感知。教训：**检索链路要打印中间产物，不要猜**。

**案例三：Streamlit 页面每次交互转圈很久才出结果。** 排查：观察日志发现每次 rerun 都在重建 OpenAI 客户端与 embedding 模型（打印 client 地址比对 id）→ 根因：没加 @st.cache_resource → 修复：客户端与 embedding 都包上缓存装饰器，首载后秒回。教训：**"越来越慢"通常是重资源被反复重建**。

三个案例的共同模式：**打印中间产物（curl 结果、检索结果、对象 id）、按层定位（代码/资源/外部依赖）、最小改动验证**。把这个模式内化，比背任何错误码都管用。

> 🎯 **核心要点**：调试的本质是**分层定位 + 最小复现**——先分清"我的代码 / 环境 / 网络 / API"哪一层的问题，再用最小脚本复现。本表列的是 Level1 最高频的 8 类，全部亲身踩过一遍后，Level2 的排错就会变成直觉。

---

**返回总览**：[Level1 总览](./00-Level1%20基础%20Demo%20总览.md) | **下一模块**：[10 验收与进入 Level2](./10-验收与进入%20Level2.md)

【参考来源】
- [Using OpenAI SDK with DeepSeek base URL](https://theneuralbase.com/deepseek-api/learn/beginner/using-openai-sdk-with-deepseek-base-url/)
- [DeepSeek API Docs](https://api-docs.deepseek.com/zh-cn/)
- [uv Documentation](https://docs.astral.sh/uv/)
