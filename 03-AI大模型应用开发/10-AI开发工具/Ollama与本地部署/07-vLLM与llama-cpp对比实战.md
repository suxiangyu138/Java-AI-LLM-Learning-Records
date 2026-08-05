# 07 - vLLM 与 llama.cpp 对比实战

> 🎯 Ollama (llama.cpp) 适合开发和个人使用，vLLM 适合生产高并发。本文从架构原理、吞吐量、延迟、显存效率四个维度全面对比，帮你做对选择

> **前置阅读**：[[01-AI模型服务化部署全解析]]、[[03-Ollama量化部署与性能优化]]

---

## 目录

1. [三大引擎概览](#1-三大引擎概览)
2. [架构原理对比](#2-架构原理对比)
3. [性能 Benchmark](#3-性能-benchmark)
4. [场景选型决策](#4-场景选型决策)
5. [vLLM 快速部署](#5-vllm-快速部署)

---

## 1. 三大引擎概览

| 维度 | **Ollama (llama.cpp)** | **vLLM** | **llama.cpp 原生** |
|------|----------------------|----------|-------------------|
| 定位 | 开发者友好，一键部署 | 高吞吐生产推理 | 底层引擎，极客工具 |
| 语言 | Go + C++ | Python + CUDA | C/C++ |
| GPU 支持 | ✅ CUDA/Metal/ROCm | ✅ CUDA/ROCm | ✅ CUDA/Metal/Vulkan |
| CPU 推理 | ✅ 优秀 | ❌ 不推荐 | ✅ 最佳 |
| OpenAI API | ✅ 内置 | ✅ 内置 | ❌ 需自行实现 |
| 量化支持 | GGUF (K-quant) | AWQ/GPTQ/FP8 | GGUF 全系列 |
| 并发模型 | 顺序处理 | **连续批处理** | 顺序处理 |
| 安装复杂度 | ⭐ (一行命令) | ⭐⭐⭐ (pip + 配置) | ⭐⭐⭐⭐ (编译) |

## 2. 架构原理对比

### 2.1 llama.cpp 推理流程

```text
请求 → Tokenize → 模型加载(GGUF) → 逐 token 生成 → 返回
                                    ↑
                              单请求独占 GPU
                              请求处理完才能处理下一个
```

### 2.2 vLLM PagedAttention

```text
请求 A ──┐
请求 B ──┤ → 连续批处理调度器
请求 C ──┘       │
                 ▼
         ┌──────────────┐
         │ PagedAttention│  ← KV Cache 分页管理
         │ (类似 OS 虚拟内存) │
         └──────┬───────┘
                │
         ┌──────▼───────┐
         │  GPU 并行推理  │  ← 多个请求共享 GPU 算力
         └──────────────┘
```

| 概念 | 传统推理 | vLLM PagedAttention |
|------|---------|-------------------|
| KV Cache 管理 | 连续分配，浪费显存 | 分页管理，动态分配 |
| 显存利用率 | ~30-40% | **~80-90%** |
| 批处理 | 静态 batching | 连续 batching（来一个加一个） |
| 吞吐量 | 基准 | **10-20x** |

> 🎯 vLLM 的核心魔法：把 KV Cache 当虚拟内存管理——按需分配页，不浪费显存，请求可以动态加入/退出批处理

## 3. 性能 Benchmark

### 3.1 吞吐量对比（Llama 3.1 8B, A100 80GB）

| 引擎 | 并发 1 | 并发 8 | 并发 32 | 并发 128 |
|------|:---:|:---:|:---:|:---:|
| **vLLM** | 45 tok/s | 280 tok/s | **850 tok/s** | **2100 tok/s** |
| Ollama (llama.cpp) | 50 tok/s | 160 tok/s | 320 tok/s | 400 tok/s |
| llama.cpp 原生 | 52 tok/s | 150 tok/s | 300 tok/s | 380 tok/s |

### 3.2 延迟对比（P50 / P95, 并发 32）

| 引擎 | P50 延迟 | P95 延迟 | TTFT (首 token) |
|------|:---:|:---:|:---:|
| **vLLM** | **120ms** | 350ms | 45ms |
| Ollama | 280ms | 800ms | 180ms |
| llama.cpp | 300ms | 850ms | 200ms |

### 3.3 显存效率

| 引擎 | 模型 (8B Q4) | KV Cache | 总显存 | 利用率 |
|------|:---:|:---:|:---:|:---:|
| vLLM | 5.0 GB | 8.0 GB | 13 GB | **85%** |
| Ollama | 5.0 GB | 3.5 GB | 8.5 GB | 40% |
| llama.cpp | 5.0 GB | 3.0 GB | 8.0 GB | 38% |

> 💡 vLLM 用了更多显存做 KV Cache，换来 10x+ 的并发吞吐——典型的"空间换吞吐"

## 4. 场景选型决策

```text
你是什么场景？
│
├── 🏠 个人开发 / 单用户
│   └── Ollama ✅（零配置，一行命令）
│
├── 🧪 原型验证 / Demo
│   └── Ollama ✅（快速迭代，Modelfile 方便）
│
├── 🏢 内部工具 / < 10 并发
│   ├── Ollama ✅（够用，无需运维复杂度）
│   └── vLLM ⚠️（overkill，但也可以用）
│
├── 🚀 生产 API / > 10 并发
│   └── vLLM ✅（吞吐量王者，PagedAttention）
│
├── 💻 纯 CPU / 边缘设备
│   └── llama.cpp ✅（CPU 推理最优）
│
├── 📱 移动端
│   └── llama.cpp (Android/iOS binding)
│
└── 🔬 评测 / 研究
    └── vLLM ✅（标准 benchmark 引擎）
```

### 混合架构（推荐）

```text
┌────────────────────────────────────┐
│         API Gateway (Nginx)         │
├────────────────────────────────────┤
│                                    │
│  ┌──────────┐    ┌──────────────┐  │
│  │  vLLM    │    │   Ollama     │  │
│  │  (主力)   │    │  (降级/特殊)  │  │
│  │  A100x2  │    │  RTX 4090    │  │
│  └──────────┘    └──────────────┘  │
│                                    │
│  流量：90% → vLLM (高并发)           │
│        10% → Ollama (本地模型/实验)  │
└────────────────────────────────────┘
```

## 5. vLLM 快速部署

```bash
# 安装
pip install vllm

# 启动 OpenAI 兼容 API
python -m vllm.entrypoints.openai.api_server \
  --model NousResearch/Hermes-3-Llama-3.1-8B \
  --dtype auto \
  --max-model-len 32768 \
  --gpu-memory-utilization 0.90 \
  --max-num-seqs 32 \
  --port 8000

# 测试
curl http://localhost:8000/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "NousResearch/Hermes-3-Llama-3.1-8B",
    "messages": [{"role": "user", "content": "Hello"}]
  }'

# 与 Ollama 无缝切换（都是 OpenAI 兼容 API！）
# Ollama: http://localhost:11434/v1
# vLLM:   http://localhost:8000/v1
```

### vLLM 关键参数

| 参数 | 说明 | 建议值 |
|------|------|:---:|
| `--max-num-seqs` | 最大并发序列数 | GPU显存/4GB |
| `--gpu-memory-utilization` | GPU 显存利用率 | 0.85-0.95 |
| `--max-model-len` | 最大上下文长度 | 按模型上限 |
| `--enable-prefix-caching` | 相同前缀复用 KV Cache | 开启 |
| `--enable-chunked-prefill` | 分块预填充 | 高并发时开启 |

## 核心要点回顾

- Ollama = 开发/个人用，vLLM = 生产/高并发用，llama.cpp = CPU/边缘用
- vLLM 的 PagedAttention 让并发吞吐提升 10-20x，代价是多用显存
- Ollama 和 vLLM 都暴露 OpenAI 兼容 API，切换只需改端口
- 选型公式：并发 < 10 → Ollama；并发 > 10 → vLLM
- 混合架构 = vLLM (90% 高并发) + Ollama (10% 实验/降级)

## 参考资料

1. vLLM 官方文档 — docs.vllm.ai
2. llama.cpp GitHub — github.com/ggerganov/llama.cpp
3. Ollama 官方 GitHub — github.com/ollama/ollama
