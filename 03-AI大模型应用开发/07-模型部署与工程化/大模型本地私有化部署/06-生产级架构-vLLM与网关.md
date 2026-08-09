# 06 生产级架构：vLLM 与网关

> 生产级私有化的骨架是"vLLM 扛吞吐 + 网关管安全 + 反向代理做收敛"——vLLM 只管把 token 吐快，认证、限流、审计这些它一概不管，也绝不该让它管。

## 📚 目录

1. [vLLM 定位与启动](#1-vllm-定位与启动)
2. [核心参数调优](#2-核心参数调优)
3. [连续批处理与吞吐原理](#3-连续批处理与吞吐原理)
4. [多卡部署](#4-多卡部署)
5. [网关与反向代理](#5-网关与反向代理)
6. [多模型与生产形态](#6-多模型与生产形态)
7. [本模块小结](#7-本模块小结)

## 1. vLLM 定位与启动

vLLM 是生产环境的吞吐王者（UC Berkeley 出品，Apache 2.0），仅支持 Linux + NVIDIA GPU。启动一条命令：

```bash
pip install vllm
vllm serve Qwen/Qwen3-32B-AWQ \
  --gpu-memory-utilization 0.9 \
  --max-model-len 32768 \
  --port 8000
```

启动后即 OpenAI 兼容 API：`http://localhost:8000/v1`。与 Ollama 的切换对应用零改动——这是"开发用 Ollama、生产用 vLLM"能成立的前提。冷启动注意：加载权重 + 编译 CUDA kernel 可能需数分钟，容器化后首次启动更明显。

## 2. 核心参数调优

| 参数 | 作用 | 建议 |
|---|---|---|
| `--gpu-memory-utilization` | 显存使用上限 | 0.85-0.95，留 KV 与余量 |
| `--max-model-len` | 最大上下文长度 | 按业务定（RAG 场景 32K+），越长 KV 显存越大 |
| `--tensor-parallel-size` | 张量并行卡数 | 多卡时 = 卡数 |
| `--enable-prefix-caching` | 前缀缓存（RAG/多轮神器） | 前缀复用场景必开 |
| `--max-num-seqs` | 最大并发序列 | 按显存余量调，默认即可 |

调参铁律：**先定上下文长度，再算 KV 显存，最后决定并发上限**——三者互相制约，`--max-model-len` 每翻倍，KV 缓存也翻倍。

## 3. 连续批处理与吞吐原理

vLLM 性能优势来自两个机制：

1. **PagedAttention**：KV 缓存按页管理，消除碎片，显存浪费减少 60-80%——同等显存塞下更多并发请求。
2. **连续批处理（Continuous Batching）**：新请求动态加入正在推理的批次，旧请求完成立即让位——GPU 利用率可达 90%+，而传统批处理要等整批完成。

```text
传统批处理：请求攒满一批 → 全批算完 → 下一批（GPU 空转在等待期）
连续批处理：请求随时进批 → 完成的先出 → 新请求补位（GPU 永不空转）
```

量化效果：10+ 并发时吞吐是 llama.cpp 系引擎的 2-4 倍，单卡支撑 50-100 并发。但注意：**单用户低延迟场景下优势不体现**——这也是"小团队别上 vLLM"的原因。

## 4. 多卡部署

| 模式 | 命令/配置 | 适用 |
|---|---|---|
| 张量并行 | `--tensor-parallel-size 4` | 单模型拆到多卡，需 NVLink 优先 |
| 多实例 | 每卡一个 vLLM 实例，网关路由 | 多模型共存、故障隔离 |
| 多副本 + LB | K8s Deployment + Service | 高可用、滚动升级 |

多卡实践：70B 量化 2×48GB 或 4×24GB 用张量并行；多模型场景每个模型独立实例更稳（一个模型崩不影响其他）；生产高可用用 K8s 编排（GPU 节点亲和 + 探针 + 滚动更新），vLLM 官方 Docker 镜像开箱即用。

## 5. 网关与反向代理

vLLM 与 Ollama 都无内置认证——网关是生产私有化的**安全大门**：

```nginx
# Nginx 反代 vLLM：TLS + 认证 + 限流 + 流式
server {
    listen 443 ssl;
    location /v1/ {
        proxy_pass http://127.0.0.1:8000;
        proxy_http_version 1.1;
        proxy_buffering off;            # SSE 流式必须关缓冲
        proxy_read_timeout 300s;
        auth_request /auth;             # 统一认证（OIDC/LDAP）
        limit_req zone=llm burst=20;    # 按 IP/令牌限流
    }
}
```

| 网关职责 | 实现 |
|---|---|
| 认证 | OIDC（对接企业 SSO）/ API Key / 内部令牌 |
| 限流 | 按用户/按 IP 的请求速率与并发上限 |
| TLS | 443 终止，内网也建议启用 |
| 审计 | 记录调用者/模型/参数摘要/结果类型（脱敏） |
| 路由 | 多模型/多实例按路径分发 |

> ⚠️ 流式接口的网关三个必配：`proxy_buffering off`（否则 SSE 不即时）、长超时、认证透传。漏掉任何一个，流式对话就会出现"客户端等不到字"的疑难故障。

## 6. 多模型与生产形态

```text
客户端 → 网关（认证/限流/路由） → vLLM-A（Qwen3-32B：通用助手）
                                  → vLLM-B（Qwen3-8B：代码补全，高并发）
                                  → vLLM-C（Embedding：RAG 向量化）
```

生产形态三条建议：

1. **按场景分模型**：通用对话 32B、高频低延迟 8B、RAG 向量化用 embedding 模型——各司其职，别一个模型扛所有。
2. **embedding 模型单独部署**：RAG 的向量化与推理解耦，embedding 服务挂了不影响对话。
3. **全链路 OpenAI 兼容**：网关对外只暴露一个 `/v1` 端点，内部按 `model` 字段路由——调用方无感知，后续换引擎/加模型不动应用。

### 6.1 生产架构的典型排障路径

生产环境出问题时的排查顺序建议固定下来，比随机试错高效得多。第一步看网关层：认证失败/限流触发/路由错误，这类问题在网关日志里 5 秒定位；第二步看服务指标：TTFT 劣化是显存问题还是并发打满，吞吐下降是批处理失效还是上游变慢，Grafana 看板直接给答案；第三步看引擎日志：Ollama 查 journalctl、vLLM 查容器日志，模型加载失败/显存 OOM 都在这里；第四步才动配置：调参数要一次只改一个变量，改完观察 10 分钟再改下一个——多参数同时调整会让问题无法归因。

这条路径的本质是**分层定位：网关 → 指标 → 日志 → 配置**，每层有明确的证据来源，不靠猜。生产环境 80% 的问题在网关层（配置/限流）就能解决，值得先查。

### 6.2 K8s 部署要点

生产级推荐容器化 + K8s 编排：vLLM 官方镜像 + GPU 节点亲和调度 + 存活/就绪探针 + 滚动更新。三个要点：GPU 资源声明（`nvidia.com/gpu: 1`）必须精确，多分配会浪费、少分配会 OOM；探针要区分存活与就绪——就绪探针等模型加载完成（冷启动数分钟）再放流量，否则滚动更新期间流量打到未就绪实例上全是 503；多副本时网关/Service 做负载均衡，模型实例无状态（权重只读），天然支持多副本。补充一个生产细节：vLLM 的 `--served-model-name` 参数决定了 API 里的 model 字段——多模型路由、网关鉴权按模型名过滤、灰度按模型名分流，全都依赖这个字段的规范性，命名要提前定好（如 `qwen3-32b-v1` 带版本），上线后再改会破坏所有下游配置。

## 7. 本模块小结

> 🎯 **核心要点**：生产级架构 = vLLM（PagedAttention + 连续批处理扛并发，单卡 50-100 并发）+ 网关（认证/限流/TLS/审计，因为引擎无内置认证）+ 按场景分模型（通用/高频/向量各一实例）。参数调优的因果链：上下文长度 → KV 显存 → 并发上限。流式接口网关三必配：关缓冲、长超时、认证透传。多模型走"一个 /v1 端点 + model 字段路由"，全链路 OpenAI 兼容。

---

**下一模块**：[07-数据安全与合规](07-数据安全与合规.md) / **返回总览**：[00-大模型本地私有化部署知识体系总览](00-大模型本地私有化部署知识体系总览.md)

---

**参考来源**：
- [Local LLM Deployment: Ollama vs vLLM vs LM Studio Compared（SitePoint）](https://www.sitepoint.com/local-llm-deployment-ollama-vs-vllm-vs-lm-studio-compared/)
- [vLLM vs Ollama vs LM Studio: The 2026 Production Self-Host Benchmark（Codersera）](https://codersera.com/blog/vllm-vs-ollama-vs-lm-studio-production-2026/)
- [大模型部署全指南——从 4GB 笔记本到 8 卡服务器怎么选（腾讯云）](https://cloud.tencent.cn/developer/article/2662098)
- [Ollama vs vLLM vs LM Studio for OpenClaw（2026）](https://www.clawctl.com/blog/ollama-vs-vllm-vs-lm-studio)
