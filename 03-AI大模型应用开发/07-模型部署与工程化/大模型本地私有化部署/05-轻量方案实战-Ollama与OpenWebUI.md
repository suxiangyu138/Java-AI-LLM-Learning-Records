# 05 轻量方案实战：Ollama 与 Open WebUI

> 一条命令装好 Ollama、一条命令拉模型、一个容器起 Open WebUI——轻量方案的价值不是"能跑"，而是 10 分钟内让团队看到私有化 AI 的真实样子。

## 📚 目录

1. [安装与模型拉取](#1-安装与模型拉取)
2. [Modelfile 定制模型](#2-modelfile-定制模型)
3. [OpenAI 兼容 API 接入](#3-openai-兼容-api-接入)
4. [Open WebUI 团队界面](#4-open-webui-团队界面)
5. [内网共享与生产化配置](#5-内网共享与生产化配置)
6. [常见故障速查](#6-常见故障速查)
7. [本模块小结](#7-本模块小结)

## 1. 安装与模型拉取

Linux 一条命令安装（2026-08 基线：v0.31.2）：

```bash
curl -fsSL https://ollama.com/install.sh | sh
ollama run qwen3:8b        # 一条命令：下载 + 启动 + 进入对话
```

```text
安装后即注册 systemd 服务，开机自启，日志在 journalctl -u ollama
模型默认存放在 ~/.ollama/models（Linux）/ %USERPROFILE%\.ollama（Windows）
```

Windows/macOS 用户下载安装包即可；Apple Silicon 自动走 MLX 引擎（v0.30 起 prefill 提速 57%、decode 提速 93%）。`ollama pull qwen3:8b` 单独拉模型、`ollama list` 查看本地模型、`ollama rm` 删除——命令面就这几个。

## 2. Modelfile 定制模型

Modelfile 是 Ollama 的"模型配方"（类似 Dockerfile）：

```dockerfile
FROM qwen3:8b
SYSTEM "你是企业知识库助手。回答只基于给定资料，资料没有就明确说不知道。"
PARAMETER temperature 0.3
PARAMETER num_ctx 8192          # 上下文窗口
```

```bash
ollama create my-assistant -f Modelfile
ollama run my-assistant
```

定制要点：`SYSTEM` 注入系统提示词（角色/边界）、`PARAMETER` 调温度与上下文、FROM 可直接指向本地 GGUF 文件（v0.30 起原生支持 `FROM ./my-model.Q4_K_M.gguf`）。团队级实践：把 Modelfile 纳入 git 管理，模型配置变更可追溯。

## 3. OpenAI 兼容 API 接入

Ollama 原生提供 OpenAI 兼容端点 `http://localhost:11434/v1`，应用零改造接入：

```python
from openai import OpenAI
client = OpenAI(base_url="http://localhost:11434/v1", api_key="ollama")
resp = client.chat.completions.create(
    model="my-assistant",
    messages=[{"role": "user", "content": "公司请假流程是什么？"}])
print(resp.choices[0].message.content)
```

关键点：api_key 随便填（Ollama 不做鉴权，鉴权交给网关层——见 06 篇）；`OLLAMA_HOST` 控制监听地址；流式输出 `stream=True` 与 OpenAI 一致。接入 LangChain/LangGraph/Claude Code 等框架同样只改 base_url。

## 4. Open WebUI 团队界面

```bash
docker run -d --name open-webui \
  -p 3000:8080 \
  -e OLLAMA_BASE_URL=http://host.docker.internal:11434 \
  -v open-webui:/app/backend/data \
  ghcr.io/open-webui/open-webui:main
```

| 能力 | 说明 |
|---|---|
| 多用户 | 自带注册/登录/角色管理（admin/user） |
| 多模型切换 | 一个界面切换所有本地模型，对比回答质量 |
| RAG 接入 | 内置知识库上传（本地 embedding 模型，数据不出域） |
| 对话管理 | 历史/分享/导出，团队协作 |

Open WebUI 是私有化"交付感"的关键：团队看到的不是 API 而是产品界面，POC 验收基本靠它。

## 5. 内网共享与生产化配置

```bash
# 环境变量（服务配置）——生产化三件套
OLLAMA_HOST=0.0.0.0:11434      # ① 监听内网（默认 localhost）
OLLAMA_KEEP_ALIVE=5m           # ② 模型驻留显存时间，调优首 token 延迟
OLLAMA_NUM_PARALLEL=4          # ③ 并发请求数（默认 1，按显存余量调）
```

| 配置 | 说明 |
|---|---|
| 防火墙 | 只对业务网段开放 11434/3000，不暴露公网 |
| 多模型共存 | 显存够则多模型常驻，不够靠 KEEP_ALIVE 换入换出 |
| CPU 兜底 | 无 GPU 时自动回退 CPU（量化模型可跑，速度慢），POC 验证足够 |
| 离线分发 | 外网下载好模型 → `ollama save` 导出 → 内网 `ollama load` 导入 |

> ⚠️ 安全警告：不要直接把 `OLLAMA_HOST=0.0.0.0` 暴露公网——Ollama 无内置认证，公网裸奔等于开放任意模型调用。生产必须前置网关（认证 + 限流 + TLS，见 06/07 篇）。

## 6. 常见故障速查

| 现象 | 原因 | 解法 |
|---|---|---|
| 模型加载慢/首次很慢 | 首次加载 + 量化转换 | 预热（启动后空调用一次） |
| 显存不足 OOM | 并发/上下文超显存 | 降 `OLLAMA_NUM_PARALLEL`、缩 `num_ctx` |
| GPU 没用上 | Vulkan/驱动问题 | 查 `ollama ps` 看是否 GPU 加载；弱核显禁用 `OLLAMA_IGPU_ENABLE=0` |
| 局域网访问不了 | 默认只监听 localhost | 设 `OLLAMA_HOST=0.0.0.0` + 防火墙放行 |
| 流式输出卡顿 | 网关缓冲了 SSE | 反代关 buffering（见 06 篇网关配置） |

排障的通用顺序建议固定为"先看模型驻留、再看并发参数、最后看网络层"：`ollama ps` 确认模型在显存里（被换出的模型首 token 会明显变慢），然后查 `OLLAMA_NUM_PARALLEL` 是否被并发打满，最后才怀疑网络与代理。按这个顺序排查，大多数"变慢"问题两分钟内能定位，避免在错误层浪费时间。这条顺序同样适用于 06 篇的 vLLM 生产环境——显存 → 并发 → 网络，是私有化排障的通用路径。记住一点：排障的目标是快速缩小范围，而不是一次找到根因，每一层排除都让问题空间减半，比反复猜疑高效得多。

### 6.1 从轻量到生产的升级路径

Ollama 方案并不是"玩具"，它是很多企业私有化的第一阶段，且可以平滑升级：第一步单机 Ollama + Open WebUI 跑通业务验证；第二步并发上来后调 `OLLAMA_NUM_PARALLEL` 与环境变量做纵向扩展；第三步在 Ollama 前面加 Nginx 网关（认证 + 限流），补上安全短板——此时已具备小团队生产服务的形态；第四步负载进一步增长，在网关后追加 vLLM 实例，Ollama 退为开发环境。每一步都只改一层，不推倒重来，这是轻量方案最大的工程价值。

升级的信号判断：单模型并发稳定超过 10、TTFT 持续劣化、或需要多模型常驻——三者出现其一，就该评估 vLLM 了。

### 6.2 Ollama 常用命令速查

```bash
ollama list                  # 本地模型列表
ollama pull qwen3:8b         # 拉取模型
ollama rm qwen3:8b           # 删除模型
ollama ps                    # 正在运行的模型（显存驻留状态）
ollama show qwen3:8b         # 查看模型详情（参数量/量化档）
ollama cp src dst            # 复制模型
ollama save/load             # 导出/导入模型（离线分发）
ollama run qwen3:8b "问题"   # 单次问答（脚本/CI 可用）
```

`ollama ps` 是排障第一命令：模型是否驻留显存、占用多少显存、num_ctx 是多少，全在这一条里。再补充一条工程习惯：**把部署步骤写成脚本而不是手工操作**——安装、拉模型、Modelfile 创建、环境变量、防火墙放行，五步各一个脚本文件入 git，新机器 10 分钟复现整套环境，这也是"轻量方案运维面小"承诺的兑现方式。手工操作三次以上的步骤，都值得脚本化。最后提醒一个容易踩的坑：`ollama run` 在脚本/CI 里直接传问题参数时，模型首次加载会占掉十几秒，脚本超时设置要留足——先 `ollama pull` 预热，或对脚本超时放宽到 60s+，否则第一行输出等不到的"卡死"会让排查走弯路。

## 7. 本模块小结

> 🎯 **核心要点**：Ollama 轻量方案的完整链路 = 一条命令安装 + `ollama run` 拉模型 + Modelfile 定制 + OpenAI 兼容 API 接入 + Open WebUI 交付界面 + 环境变量生产化。价值判断：10 分钟让团队用上私有化 AI，数据不出域的承诺从第一天就成立。上线前必做三件事：前置网关（无内置认证）、防火墙收敛端口、离线分发模型。故障排查看 `ollama ps` 与 journalctl。

---

**下一模块**：[06-生产级架构：vLLM 与网关](06-生产级架构-vLLM与网关.md) / **返回总览**：[00-大模型本地私有化部署知识体系总览](00-大模型本地私有化部署知识体系总览.md)

---

**参考来源**：
- [Ollama vs vLLM vs LM Studio: Running Local LLMs in Production（dev.to）](https://dev.to/ayinedjimi-consultants/ollama-vs-lm-studio-vs-vllm-running-local-llms-in-production-2eal)
- [Ollama v0.30.0 发布公告（GGUF 原生支持/llama.cpp）](https://ollama.com/blog/improved-performance-and-model-support-with-gguf)
- [Ollama 本地私有化大模型完整工程实战（CSDN）](https://blog.csdn.net/qq_36729037/article/details/163565763)
- [本地大模型部署（cnblogs）](https://www.cnblogs.com/hewei-blogs/articles/19799713)
