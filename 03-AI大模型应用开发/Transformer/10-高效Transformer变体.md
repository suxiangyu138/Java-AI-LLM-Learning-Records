# 10 - 高效 Transformer 变体

> 🎯 标准 Attention 的 O(n²) 复杂度是长序列的天敌。本章覆盖 Sparse Attention、Linformer、Reformer、Longformer、Flash Attention 等高效方案，理解它们的设计思想比记住名字更重要

---

## 目录

1. [复杂度瓶颈回顾](#1-复杂度瓶颈回顾)
2. [Sparse Attention 系列](#2-sparse-attention-系列)
3. [Linformer (线性注意力)](#3-linformer-线性注意力)
4. [Flash Attention](#4-flash-attention)

---

## 1. 复杂度瓶颈回顾

```text
标准 Attention 复杂度：O(n² · d)

当 n = 32K：≈ 1B 次运算（还能忍）
当 n = 128K：≈ 16B 次运算（开始吃力）
当 n = 1M：≈ 1T 次运算（不可接受）

显存：N×N 的注意力矩阵
  32K 序列 → 32K×32K×2 bytes (FP16) = 2GB
  128K → 32GB ← 爆显存！

→ 两条路：降低复杂度（算法创新）或 优化计算（系统工程）
```

## 2. Sparse Attention 系列

### 2.1 核心思路

```text
不是每个 token 都需要看所有其他 token

直觉：
  → 相邻 token 通常更重要（局部性）
  → 少数 token 需要全局视野（[CLS] / 特殊 token）
  → 远距离 token 大部分不需要直接 attention

做法：不计算全 N×N 注意力，只计算"重要的"那些对
```

### 2.2 变体对比

| 模型 | 策略 | 复杂度 | 核心创新 |
|------|------|:---:|------|
| **Sparse Transformer** | 跨步 + 局部 | O(n·√n) | 固定稀疏模式 |
| **Longformer** | 滑动窗口 + 全局 token | O(n·w) | 任务特定全局注意力 |
| **BigBird** | 随机 + 局部 + 全局 | O(n) | 图稀疏化理论保证 |
| **Reformer** | LSH 分桶 | O(n·log n) | 用哈希找到"相似的"key |

```text
Longformer 的注意力模式：

Token:  1  2  3  4  5  6  7  8  9
        1  ✓  ✓           ✓        ← Token 1: 局部窗口 + 全局
        2  ✓  ✓  ✓                  ← Token 2: 滑动窗口
        3     ✓  ✓  ✓               ← Token 3: 滑动窗口
        4        ✓  ✓  ✓            ← ...
```

## 3. Linformer (线性注意力)

```text
核心发现：注意力矩阵 N×N 是低秩的！

→ 可以用两个小矩阵来近似：
  K': (k, d) 代替 K: (n, d)，其中 k << n（如 k=256）
  V': (k, d) 代替 V: (n, d)

复杂度：O(n·k·d) → 当 k 固定时 = O(n)
         而不是 O(n²·d)

代价：损失少量精度，换取线性复杂度
```

## 4. Flash Attention

```text
Flash Attention ≠ 改变复杂度，而是优化计算方式

核心创新：
  → 不存储完整的 N×N 注意力矩阵
  → 分块计算 (Tiling) + 需要时重算 (Recomputation)

为什么重要？
  标准 Attention 的瓶颈不是计算 FLOPs，而是显存带宽
  → GPU 算力富余，但数据读写太慢
  → Flash Attention: 减少显存读写 = 加速

效果：
  ✅ 显存：O(N²) → O(N)
  ✅ 速度：2-4x 加速
  ✅ 精度：数学等价（不是近似）
  ✅ 长序列：支持到 64K+

版本：
  Flash Attention 1 (2022)：基础 tiling + recomputation
  Flash Attention 2 (2023)：更好的并行，~2x 再加速
  Flash Attention 3 (2024)：H100 优化，支持 FP8

→ 所有现代 LLM（GPT/Llama/Mistral/Qwen）标配 Flash Attention 2+
```

### 算法变体总结

| 方法 | 复杂度 | 精度损失 | 使用状态 |
|------|:---:|:---:|------|
| 标准 Attention | O(n²) | 0% | 基准 |
| Sparse (Longformer) | O(n·w) | 小 | 特定场景 |
| Linformer | O(n) | 中等 | 实验/小众 |
| **Flash Attention** | **O(n²)** | **0%** | **💯 标配！** |
| **Flash Attn 2** | **O(n²)** | **0%** | **💯 标配！** |

> 🎯 Flash Attention 之所以成为标配：因为它不是近似，而是等价实现——零精度损失 + 显存 O(N) + 速度 2-4x

## 核心要点回顾

- O(n²) 瓶颈 → 两条路：算法降复杂度（Sparse/Linformer）或 工程优化（Flash Attn）
- Sparse Attention = 不计算全矩阵，只计算"重要"的 pairs
- Flash Attention = 不改变复杂度，但优化计算模式 → 标配
- Flash Attention ≠ 近似 = 等价实现（数学上相同结果）
- 2025 年：Flash Attention 2/3 + GQA (减少 KV Cache) = LLM 推理标配

## 参考资料

1. Flash Attention 1/2/3 论文 (Dao et al., 2022-2024)
2. Longformer 论文 (Beltagy et al., 2020)
3. Linformer 论文 (Wang et al., 2020)
