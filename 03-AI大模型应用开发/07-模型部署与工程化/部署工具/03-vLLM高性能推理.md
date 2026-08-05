# 03 - vLLM 高性能推理

> 🎯 vLLM = LLM 推理的事实标准 — PagedAttention 显存利用率提升 4×、连续批处理 QPS 提升 10×、OpenAI 兼容 API 零迁移成本

---

## 目录

1. [vLLM 核心优势](#1-vllm-核心优势)
2. [PagedAttention 原理](#2-pagedattention-原理)
3. [Quick Start](#3-quick-start)
4. [生产配置调优](#4-生产配置调优)
5. [Java 端调用](#5-java-端调用)

---

## 1. vLLM 核心优势

```text
vLLM vs HuggingFace Transformers：

  指标            HF Transformers    vLLM      提升
  ────────────────────────────────────────────
  显存利用率       ~30%             ~90%      3×
  吞吐 (QPS)      10-20            100-200   10×
  首 Token 延迟   2-5s             0.5-1s    3×
  API 支持        需自己封装        OpenAI兼容 零成本

核心：PagedAttention = 块状管理 KV Cache → 显存零碎片
```

---

## 2. PagedAttention 原理

```text
传统 KV Cache：
  为每个请求预分配 max_len 的连续显存
  → 大部分用不满 → 碎片化 → 利用率 ~30%

PagedAttention：
  将 KV Cache 分成固定大小的"页"（Page）
  → 所有请求共享显存池
  → 按需分配，不浪费
  → 利用率 ~90%

类比：OS 虚拟内存分页 → PagedAttention = 显存分页
```

---

## 3. Quick Start

```bash
# 安装
pip install vllm

# 启动 OpenAI 兼容 API 服务
python -m vllm.entrypoints.openai.api_server \
    --model Qwen/Qwen2-7B-Instruct \
    --max-model-len 8192 \
    --gpu-memory-utilization 0.9
```

```python
# 使用 OpenAI SDK 直接调用（兼容！）
from openai import OpenAI

client = OpenAI(base_url="http://localhost:8000/v1", api_key="not-needed")

response = client.chat.completions.create(
    model="Qwen/Qwen2-7B-Instruct",
    messages=[{"role": "user", "content": "解释 Java GC"}],
    max_tokens=512,
    temperature=0.7
)
print(response.choices[0].message.content)
```

---

## 4. 生产配置调优

```python
from vllm import LLM, SamplingParams

llm = LLM(
    model="Qwen/Qwen2-7B-Instruct",
    tensor_parallel_size=1,          # 单卡
    # tensor_parallel_size=4,        # 4 卡张量并行
    max_model_len=8192,
    gpu_memory_utilization=0.90,     # 显存利用率
    max_num_seqs=256,                 # 最大并发请求数
    dtype="bfloat16",                 # BF16 推理
    quantization="awq",               # AWQ 量化（可选）
    enforce_eager=False,              # CUDA Graph 加速
)

# 批量推理
prompts = ["问题1", "问题2", "问题3"]
outputs = llm.generate(prompts, SamplingParams(
    temperature=0.7, top_p=0.9, max_tokens=512
))
```

---

## 5. Java 端调用

```java
// vLLM 提供 OpenAI 兼容 API → Java 使用 OpenAI SDK
// 或直接用 HTTP Client

HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("http://vllm-service:8000/v1/chat/completions"))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString("""
        {"model":"Qwen/Qwen2-7B","messages":[...],"max_tokens":512}
    """))
    .build();

HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
```

---

## 核心要点回顾

- PagedAttention = 显存分页 → 利用率 30%→90%
- 连续批处理 = 动态拼 batch → QPS 10×
- OpenAI 兼容 API → 零迁移成本
- 生产参数：`gpu_memory_utilization=0.9, max_num_seqs=256`

---

## 6. vLLM 的 2026 现状

**最新状态（2026-08 检索核实）**：

```text
版本：v0.7.3（2026-03）→ v0.21.0（05）→ v0.22.x V1（06）
  → V1 引擎自 v0.7.0 起为默认（架构重写）
份额：~45% 部署量（GPU 生产默认首选）
采用：Anyscale/IBM/Databricks/Cloudflare 等大厂
硬件：NVIDIA/AMD ROCm/CPU/TPU/Ascend（最广）
```

**V1 引擎的新能力**：

```text
① Hopper 自动 FP8 权重校准（v0.7.3）
② 长上下文（>64k tokens）内存效率 +12%
③ 与 SGLang 的竞争主线（2026 年）：前缀缓存效率 SGLang 更优
```

**生产调优速查**（2026 实践）：

```text
--max-model-len：上下文长度（按业务定，越长显存越大）
--gpu-memory-utilization：0.9（KV Cache 分配）
--tensor-parallel-size：多卡张量并行（70B 需 2-4 卡）
--enable-prefix-caching：前缀缓存（RAG 系统提示共享场景）
--quantization fp8：FP8 量化（吞吐↑显存↓，H100 最优）
环境：VLLM_USE_V1=1（V1 引擎开关）
```

> 🎯 **核心要点**：vLLM 2026 = "**生产默认首选（45% 份额）**"——V1 引擎默认、FP8 支持、硬件最广；**调优四参数（max-len/显存利用率/张量并行/前缀缓存）**是生产标准动作。

---

## 7. 部署与调用速查

**vLLM 启动与验证**：

```bash
# 启动 OpenAI 兼容服务
vllm serve Qwen/Qwen2.5-7B-Instruct \
  --port 8000 \
  --gpu-memory-utilization 0.9 \
  --max-model-len 8192

# 验证
curl http://localhost:8000/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"model": "Qwen/Qwen2.5-7B-Instruct",
       "messages": [{"role": "user", "content": "你好"}]}'

# 指标
curl http://localhost:8000/metrics    # Prometheus 指标
```

**Java 调用速查**（Spring AI / OpenAI SDK）：

```java
// OpenAI 兼容：Java 端直接用 OpenAI SDK 指向 vLLM
OpenAiApi api = new OpenAiApi("http://localhost:8000/v1", "EMPTY");
// Spring AI：spring.ai.openai.base-url=http://localhost:8000/v1
// 或 langchain4j：OpenAiChatModel.builder().baseUrl("http://localhost:8000/v1")
```

**常见启动参数速查**：

| 参数 | 说明 |
|------|------|
| --tensor-parallel-size N | 张量并行卡数（70B 需 2-4） |
| --quantization fp8 | FP8 量化（H100 最优） |
| --enable-prefix-caching | 前缀缓存（RAG 共享提示词） |
| --max-num-seqs | 并发批大小（显存换吞吐） |
| --dtype bfloat16 | 精度（默认） |

> 🎯 **核心要点**：vLLM 使用 = "**一条命令启动 + OpenAI 兼容调用 + 三参数调优（显存利用率/张量并行/前缀缓存）**"——Java 端零特殊适配（OpenAI SDK 直连）；/metrics 端点开箱即用。
