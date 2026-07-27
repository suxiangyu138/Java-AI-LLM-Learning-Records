# 06 - GGUF 量化技术深度解析

> 🎯 GGUF 是 llama.cpp 生态的标准模型格式，让 70B 模型从 140GB 压缩到 40GB 而质量几乎无损。理解 K-quant 和 I-quant 的区别、选对量化级别，是本地部署大模型的必修课

> **前置阅读**：[[03-Ollama量化部署与性能优化]]、[[02-Ollama快速上手与基础操作]]

---

## 目录

1. [量化基础原理](#1-量化基础原理)
2. [GGUF 格式详解](#2-gguf-格式详解)
3. [量化类型对比](#3-量化类型对比)
4. [场景选型实战](#4-场景选型实战)
5. [量化工具链](#5-量化工具链)

---

## 1. 量化基础原理

### 1.1 什么是量化

```text
FP16（半精度）：每个权重 16 bit = 2 bytes
   ↓ 量化
Q4（4-bit）：每个权重 4 bit = 0.5 bytes

压缩比：4x
准确率损失：通常 < 1%（Q4_K_M 级别）
```

### 1.2 量化类型分类

| 类别 | 说明 | 适用 |
|------|------|------|
| **训练后量化 (PTQ)** | 训练完成后直接量化 | 通用，无需重新训练 |
| **量化感知训练 (QAT)** | 训练时就考虑量化影响 | 追求极致质量 |
| **动态量化** | 推理时动态计算 scale | 简单但精度较低 |
| **静态量化** | 提前校准 scale/zero-point | GGUF 主流方案 |

> 🎯 GGUF 的 K-quant 属于**静态训练后量化**——在量化前用小批量数据校准关键层

## 2. GGUF 格式详解

### 2.1 GGUF vs GGML vs SafeTensors

| 格式 | 时代 | 特点 | 状态 |
|------|:---:|------|:---:|
| **GGML** | 2023 | 第一代格式，仅支持 CPU | ❌ 已废弃 |
| **GGUF** | 2023-现在 | 新一代格式，CPU+GPU | ✅ 当前标准 |
| **SafeTensors** | 2023-现在 | HuggingFace 安全格式 | ✅ 训练/存储 |

```text
模型分发流程：
HuggingFace (SafeTensors/FP16)
  → llama.cpp convert → GGUF FP16
  → llama-quantize → GGUF Q4_K_M
  → Ollama / llama.cpp 加载推理
```

### 2.2 GGUF 文件结构

```text
GGUF 文件结构
├── Header
│   ├── Magic Number: "GGUF"
│   ├── Version: 2 / 3
│   ├── Tensor Count
│   └── Metadata (KV pairs)
├── Metadata KV Pairs
│   ├── "general.architecture": "llama"
│   ├── "general.name": "Hermes 3"
│   ├── "llama.context_length": 32768
│   ├── "llama.embedding_length": 4096
│   └── "tokenizer.ggml.model": "gpt2" (or "llama")
├── Tensor Info (offset + shape + type)
└── Tensor Data (aligned to 32 bytes)
```

## 3. 量化类型对比

### 3.1 K-quant 系列（推荐）

| 类型 | Bits | 8B 大小 | 质量 | 适用场景 |
|------|:---:|:---:|:---:|------|
| **Q2_K** | 2.6 | 2.9 GB | ⭐⭐ | 极限压缩，质量明显下降 |
| Q3_K_S | 3.3 | 3.5 GB | ⭐⭐⭐ | 轻量嵌入 |
| Q3_K_M | 3.5 | 3.8 GB | ⭐⭐⭐ | 轻量均衡 |
| Q3_K_L | 3.6 | 4.1 GB | ⭐⭐⭐ | 轻量优质 |
| **Q4_K_S** | 4.3 | 4.6 GB | ⭐⭐⭐⭐ | 推荐轻量 |
| **Q4_K_M** | 4.5 | **4.9 GB** | ⭐⭐⭐⭐ | **🏆 性价比之王** |
| Q5_K_S | 5.2 | 5.5 GB | ⭐⭐⭐⭐⭐ | 生产推荐 |
| Q5_K_M | 5.4 | 5.8 GB | ⭐⭐⭐⭐⭐ | 生产优质 |
| Q6_K | 6.6 | 6.6 GB | ⭐⭐⭐⭐⭐ | 近乎无损 |
| **Q8_0** | 8.0 | 7.7 GB | ⭐⭐⭐⭐⭐ | 基准对照 |

### 3.2 I-quant 系列（新）

| 类型 | Bits | 特点 |
|------|:---:|------|
| IQ1_S / IQ1_M | 1.5-1.7 | 极限 1-bit 量化 |
| IQ2_XXS / IQ2_XS | 2.0-2.2 | 超低比特，质量尚可 |
| IQ2_S / IQ2_M | 2.5-2.7 | 接近 Q2_K 质量 |
| IQ3_XXS / IQ3_S | 3.0-3.3 | 3-bit 优化版 |
| IQ4_XS / IQ4_NL | 4.0-4.3 | 4-bit 新方案 |

### 3.3 质量损失量化

```python
# 以 8B 模型为基准（FP16 = 100% 质量）
QUALITY_MAP = {
    "F16":   100,    # 16.0 GB — 无损基准
    "Q8_0":   99.5,  # 7.7 GB — 几乎无损
    "Q6_K":   98.5,  # 6.6 GB
    "Q5_K_M": 97.0,  # 5.8 GB — 生产推荐
    "Q4_K_M": 95.0,  # 4.9 GB — 🏆 最佳性价比
    "Q3_K_M": 90.0,  # 3.8 GB
    "Q2_K":   82.0,  # 2.9 GB — 可感知的质量下降
}
```

### 3.4 _S / _M / _L 后缀含义

| 后缀 | 含义 | 说明 |
|:---:|------|------|
| `_S` | Small | 压缩更多，体积更小，质量稍低 |
| `_M` | Medium | 均衡——**推荐默认选择** |
| `_L` | Large | 压缩更少，体积稍大，质量更好 |

## 4. 场景选型实战

```text
你的场景是什么？
│
├── 🏠 个人学习 / 原型开发
│   └── Q4_K_M（5GB 跑 8B，零感知质量损失）
│
├── 🏢 生产环境 / 面向用户
│   ├── 预算充足 → Q5_K_M 或 Q6_K
│   └── 预算紧张 → Q4_K_M（性价比首选）
│
├── 📱 移动端 / 边缘设备
│   └── Q3_K_M 或 IQ3_XXS（体积优先）
│
├── 🔬 评测 / 基准测试
│   └── Q8_0 或 FP16（排除量化干扰）
│
└── 💾 极限压缩
    └── Q2_K（质量损失明显，仅应急）
```

### 显存计算器

```python
def estimate_vram(model_params_b, quant_type, context_len=4096):
    """估算推理所需显存（GB）"""
    # 量化比特映射
    bits_map = {
        "Q2_K": 2.6, "Q3_K_M": 3.5, "Q4_K_M": 4.5,
        "Q5_K_M": 5.4, "Q6_K": 6.6, "Q8_0": 8.0, "F16": 16.0
    }
    bits = bits_map.get(quant_type, 16.0)

    # 模型权重
    model_gb = model_params_b * bits / 8

    # KV Cache (粗略估计)
    # 每层: 2 * n_heads * head_dim * n_layers * context_len * bytes_per_elem
    # 简化: 0.5 GB per 1B params per 4096 context
    kv_cache_gb = model_params_b * 0.5 * (context_len / 4096)

    # Overhead (10%)
    overhead = (model_gb + kv_cache_gb) * 0.10

    return model_gb + kv_cache_gb + overhead

# 示例
print(f"Qwen 7B Q4_K_M: {estimate_vram(7, 'Q4_K_M'):.1f} GB")    # ~4.5 GB
print(f"Llama 70B Q4_K_M: {estimate_vram(70, 'Q4_K_M'):.1f} GB")   # ~42 GB
```

## 5. 量化工具链

### 5.1 llama.cpp 量化

```bash
# 克隆并编译 llama.cpp
git clone https://github.com/ggerganov/llama.cpp
cd llama.cpp && make -j

# 1. 转换 HuggingFace → FP16 GGUF
python convert_hf_to_gguf.py \
  /path/to/model \
  --outfile model-f16.gguf \
  --outtype f16

# 2. 量化
./llama-quantize model-f16.gguf model-Q4_K_M.gguf Q4_K_M

# 3. 验证
./llama-perplexity -m model-Q4_K_M.gguf -f wiki-test.txt
# Perplexity 越低越好，对比 FP16 的下降幅度
```

### 5.2 Ollama 内置量化

```bash
# Ollama 在 pull 时自动选择量化版本
ollama pull llama3.1:8b           # 默认 Q4_K_M
ollama pull llama3.1:8b-q5_K_M    # 明确指定 Q5_K_M

# 查看已下载模型的量化信息
ollama show llama3.1:8b --modelfile
# PARAMETER num_ctx 2048
# TEMPLATE """..."""   ← 这里能看到是否量化模板
```

## 核心要点回顾

- GGUF = llama.cpp 标准格式，取代 GGML，支持 CPU+GPU
- Q4_K_M = 性价比之王：4.5 bit，5GB 跑 8B，质量损失 < 5%
- Q5_K_M = 生产推荐：5.4 bit，5.8GB 跑 8B，质量损失 < 3%
- K-quant (_M 后缀) > 传统量化：按层重要性分配不同精度
- 70B Q4_K_M 仅需 ~42GB 显存，单张 A6000 (48GB) 可跑
- 量化流程：HuggingFace → FP16 GGUF → quantize → Ollama 加载

## 参考资料

1. llama.cpp GitHub — github.com/ggerganov/llama.cpp
2. GGUF 格式规范 — github.com/ggerganov/ggml
3. TheBloke 量化模型集合 — HuggingFace
