# 09 - 硬件选型与 GPU 性能调优

> 🎯 "买什么显卡跑大模型"是最高频问题之一。本文从显存容量、带宽、算力三个维度系统分析，给出从 3B 到 405B 模型的完整硬件方案

> **前置阅读**：[[06-GGUF量化技术深度解析]]、[[07-vLLM与llama-cpp对比实战]]

---

## 目录

1. [GPU 选型核心指标](#1-gpu-选型核心指标)
2. [显卡推荐清单](#2-显卡推荐清单)
3. [显存需求速算](#3-显存需求速算)
4. [多卡与集群方案](#4-多卡与集群方案)
5. [性能调优实战](#5-性能调优实战)

---

## 1. GPU 选型核心指标

### 1.1 三大指标

| 指标 | 含义 | 影响 | 优先级 |
|------|------|------|:---:|
| **显存容量 (VRAM)** | 能放多大的模型 | 模型尺寸上限 | ⭐⭐⭐⭐⭐ |
| **显存带宽** | 数据读取速度 | Token 生成速度 | ⭐⭐⭐⭐ |
| **算力 (TFLOPS)** | 计算速度 | 首 token 延迟 | ⭐⭐⭐ |

> 🎯 **显存 > 带宽 > 算力**——推理场景下，能不能跑看显存，跑多快看带宽

### 1.2 主流 GPU 规格对比

| GPU | 显存 | 带宽 | FP16 TFLOPS | 价格 (参考) | 推荐度 |
|------|:---:|:---:|:---:|:---:|:---:|
| **RTX 3060 12GB** | 12 GB | 360 GB/s | 12.7 | ¥1,800 | ⭐⭐⭐⭐ 入门首选 |
| RTX 4060 Ti 16GB | 16 GB | 288 GB/s | 22.1 | ¥3,200 | ⭐⭐⭐ 带宽偏低 |
| **RTX 4090 24GB** | 24 GB | 1008 GB/s | 82.6 | ¥13,000 | ⭐⭐⭐⭐⭐ 个人旗舰 |
| RTX A6000 48GB | 48 GB | 768 GB/s | 38.7 | ¥30,000 | ⭐⭐⭐⭐ 专业工作站 |
| **A100 80GB** | 80 GB | 2039 GB/s | 312 | 云端 $1-3/h | ⭐⭐⭐⭐⭐ 数据中心 |
| H100 80GB | 80 GB | 3352 GB/s | 756 | 云端 $2-5/h | ⭐⭐⭐⭐⭐ 最新旗舰 |
| **Mac M2 Ultra** | 192 GB* | 800 GB/s | 27.2 | ¥30,000+ | ⭐⭐⭐⭐ 推理专用 |

> \* Mac 的统一内存架构，CPU 和 GPU 共享 192GB

## 2. 显卡推荐清单

### 2.1 按预算推荐

```text
¥0          → Google Colab 免费 T4 (16GB)
¥1,500-2,000 → RTX 3060 12GB — 入门最佳，跑 8B Q4
¥3,000-4,000 → RTX 4060 Ti 16GB — 跑 8B 有余，13B Q4 勉强
¥6,000-8,000 → 二手 RTX 3090 24GB — 高带宽，跑 13B-34B
¥12,000-15,000 → RTX 4090 24GB — 个人旗舰，跑 70B Q3
¥25,000-35,000 → RTX A6000 48GB — 专业级，跑 70B Q4
云端按需      → A100 80GB $1-3/h — 企业级
```

### 2.2 按模型推荐

| 模型 | Q4_K_M 显存 | 最低显卡 | 推荐显卡 |
|------|:---:|------|------|
| 3B (Qwen/Mini) | 2 GB | 树莓派 5 8GB | 任何独显 |
| **7-8B (Llama/Qwen)** | **5 GB** | GTX 1060 6GB | **RTX 3060 12GB** |
| 13-14B (Qwen) | 8 GB | RTX 3060 12GB | RTX 4060 Ti 16GB |
| 24B-34B (Mistral) | 15 GB | RTX 3090 24GB | RTX 4090 24GB |
| 70-72B (Llama/Qwen) | 40 GB | 2×RTX 3090 | A6000 48GB |
| 405B (Llama) | 200 GB | 5×A100 80GB | 8×H100 |

## 3. 显存需求速算

### 3.1 精确计算公式

```python
def calculate_vram(
    params_b: float,        # 模型参数量（B）
    quant_bits: float = 4.5,  # 量化位宽（Q4_K_M=4.5）
    context_len: int = 4096,
    batch_size: int = 1,
    overhead_pct: float = 0.20  # 框架+KV Cache overhead
) -> dict:
    """精确显存需求计算"""
    # 1. 模型权重
    model_gb = params_b * quant_bits / 8

    # 2. KV Cache (简化公式)
    # 每 token 每层大约需要 2 * hidden_dim * num_layers * dtype_bytes
    # 粗略估计: ~0.5KB/token per 1B params
    kv_cache_gb = params_b * 0.5 * (context_len / 4096) * batch_size / 1024

    # 3. 激活值 + 框架 overhead
    overhead_gb = (model_gb + kv_cache_gb) * overhead_pct

    total = model_gb + kv_cache_gb + overhead_gb

    return {
        "model_weights": round(model_gb, 1),
        "kv_cache": round(kv_cache_gb, 1),
        "overhead": round(overhead_gb, 1),
        "total": round(total, 1),
        "recommendation": "✅ OK" if total < 22 else "⚠️ 需要更大显存"
    }

# 示例
for model, params in [("Qwen-7B", 7), ("Llama-70B", 70)]:
    result = calculate_vram(params, context_len=8192)
    print(f"{model}: {result}")
```

### 3.2 速查表（Q4_K_M, 4096 context, batch=1）

| 参数量 | 模型权重 | KV Cache | Overhead | **总需求** |
|:---:|:---:|:---:|:---:|:---:|
| 1B | 0.6 GB | 0.1 GB | 0.1 GB | **0.8 GB** |
| 3B | 1.7 GB | 0.2 GB | 0.4 GB | **2.3 GB** |
| 7B | 3.9 GB | 0.4 GB | 0.9 GB | **5.2 GB** |
| 8B | 4.5 GB | 0.5 GB | 1.0 GB | **6.0 GB** |
| 13B | 7.3 GB | 0.7 GB | 1.6 GB | **9.6 GB** |
| 34B | 19.1 GB | 1.7 GB | 4.2 GB | **25.0 GB** |
| 70B | 39.4 GB | 3.4 GB | 8.6 GB | **51.4 GB** |
| 405B | 227.8 GB | 19.8 GB | 49.5 GB | **297 GB** |

## 4. 多卡与集群方案

### 4.1 多卡策略

```bash
# vLLM Tensor Parallelism（模型拆分到多卡）
python -m vllm.entrypoints.openai.api_server \
  --model meta-llama/Llama-3.1-70B \
  --tensor-parallel-size 4 \    # 拆分到 4 张 GPU
  --gpu-memory-utilization 0.90

# Ollama 多卡（自动使用所有可用 GPU）
# 设置环境变量即可
export CUDA_VISIBLE_DEVICES=0,1,2,3
ollama run llama3.1:70b
```

### 4.2 成本对比

| 方案 | 配置 | 月成本 | 可跑模型 |
|------|------|:---:|------|
| 自建 | RTX 4090 × 1 | ~¥500 (电费) | 8B-34B |
| 自建 | RTX A6000 × 1 | ~¥800 (电费) | 70B Q4 |
| 云端 | A100 80GB 按需 | ~$2,160/月 (24/7) | 70B 全量 |
| 云端 | A100 80GB Spot | ~$650/月 (可中断) | 70B 全量 |
| 混合 | 4090(日常) + 云端A100(高峰) | ¥500 + ~$300 | 灵活 |

## 5. 性能调优实战

### 5.1 Ollama 调优

```bash
# 环境变量调优
export OLLAMA_NUM_PARALLEL=4          # 并行请求数（看显存余量）
export OLLAMA_MAX_LOADED_MODELS=2     # 最多常驻模型
export OLLAMA_KEEP_ALIVE=24h          # 模型驻留时间
export OLLAMA_FLASH_ATTENTION=1       # 开启 Flash Attention
export OLLAMA_MMAP=1                  # 内存映射加载（加速启动）

# 模型参数调优
ollama run qwen2.5:7b
>>> /set parameter num_ctx 8192       # 增大上下文
>>> /set parameter num_predict -1     # 不限制生成长度
>>> /set parameter temperature 0.5    # 降低随机性
```

### 5.2 vLLM 调优

```bash
python -m vllm.entrypoints.openai.api_server \
  --model model-name \
  --max-num-seqs 32 \                    # 并发数（显存越多越大）
  --max-num-batched-tokens 8192 \        # 批处理 token 数
  --gpu-memory-utilization 0.92 \        # GPU 利用率
  --enable-prefix-caching \             # 前缀缓存
  --enable-chunked-prefill \            # 分块预填充
  --max-model-len 32768
```

### 5.3 常见性能瓶颈

| 瓶颈 | 症状 | 解决 |
|------|------|------|
| **显存不足** | OOM / 模型加载失败 | 更大量化 / 换大显存卡 |
| **带宽瓶颈** | GPU 利用率低但延迟高 (4060 Ti) | 换高带宽卡 (3090/4090) |
| **CPU 瓶颈** | GPU 等待 CPU 预处理 | 增加 num_workers |
| **并发瓶颈** | 排队时间 > 推理时间 | vLLM 替换 Ollama |
| **KV Cache 不足** | 上下文被截断 | 增大 `num_ctx` / 减 `max-num-seqs` |

## 核心要点回顾

- 显存 > 带宽 > 算力：能不能跑看显存，跑多快看带宽
- RTX 3060 12GB = 入门首选，4090 24GB = 个人旗舰，A100 80GB = 企业标准
- 7-8B Q4_K_M 需 ~6GB 显存 → RTX 3060 完美承载
- 70B Q4_K_M 需 ~51GB 显存 → 至少 RTX A6000 48GB 或 2×RTX 3090
- 多卡：vLLM tensor-parallel 自动拆分，Ollama 自动使用 CUDA_VISIBLE_DEVICES
- 性能调优核心：增大上下文 → 调并发 → 开 prefix caching → 换 vLLM

## 参考资料

1. Tim Dettmers — GPU 推荐指南
2. NVIDIA GPU 规格表
3. vLLM 性能调优文档
