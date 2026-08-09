# 01 PyTorch 概述：深度学习框架

> PyTorch 2.x 是 AI 工程的事实标准框架（2.10.0，2026）——动态图让研究友好，torch.compile 让生产不慢；官方已把 torch.compile 定位为 torch.jit 的替代。

## 📚 目录

1. [PyTorch 是什么](#1-pytorch-是什么)
2. [2.x 时代：编译与动态图的统一](#2-2x-时代编译与动态图的统一)
3. [发展简史](#3-发展简史)
4. [与 TensorFlow / JAX 对比](#4-与-tensorflow--jax-对比)
5. [适用场景判断](#5-适用场景判断)
6. [面试高频问法](#6-面试高频问法)
7. [常见误区](#7-常见误区)
8. [经典应用场景](#8-经典应用场景)

## 1. PyTorch 是什么

| 维度 | 说明 |
|---|---|
| 出品方 | Meta AI（开源，BSD 许可） |
| 定位 | 深度学习框架（训练 + 推理） |
| 版本 | 2.10.0（2026-01/02） |
| 核心特性 | 动态图（Eager）+ torch.compile（编译） |
| 生态 | 事实标准（transformers/HF 全系基于它） |
| 应用面 | 研究、生产、LLM 全栈 |

一句话定义：

> PyTorch 用"张量 + 自动微分 + 动态图"让深度学习开发像写普通 Python——定义模型（nn.Module）、自动求梯度（autograd）、四步训练循环；2.x 用 torch.compile 把动态图编译加速，兼顾灵活与性能。

## 2. 2.x 时代：编译与动态图的统一

### 1.x vs 2.x

| 维度 | 1.x | 2.x |
|---|---|---|
| 执行模式 | 纯动态（Eager） | Eager + torch.compile |
| 性能 | 动态图慢 | 编译后接近静态图 |
| 部署 | TorchScript | torch.export / AOT |
| 定位 | 研究友好 | 研究 + 生产统一 |

### torch.compile（2.x 核心）

```
torch.compile = 把动态图编译为优化内核：
① 追踪（Dynamo 捕获计算图）
② 图优化（算子融合）
③ 代码生成（Inductor 生成内核）
效果：训练加速 30-50%+（典型模型）
```

```python
model = torch.compile(model)    # 一行接入
```

### 2026 关键结论（2.10）

```
① torch.compile 全面支持 Python 3.14
② aot_compile：预编译消除生产冷启动
③ 确定性模式：开启后编译结果可复现（调试友好）
④ torch.jit 在 3.14 不保证——官方建议迁移 compile/export
```

## 3. 发展简史

| 时间 | 事件 | 意义 |
|---|---|---|
| 2017 | PyTorch 发布 | 动态图路线挑战 TensorFlow |
| 2019-2020 | 研究社区主导 | 论文实现事实标准 |
| 2022 | **2.0 发布（torch.compile）** | 编译时代开启 |
| 2023-2025 | 2.x 持续迭代 | 编译生态成熟 |
| 2026 | **2.10.0** | Python 3.14/确定性/AOT |

### 生态地位

```
深度学习框架生态（2026）：
PyTorch：研究 + 生产事实标准（LLM 全栈）
TensorFlow：存量企业（Keras 生态）
JAX：研究实验（函数式/编译）
PyTorch 的护城河：HF 生态（transformers）基于它
```

## 4. 与 TensorFlow / JAX 对比

| 维度 | PyTorch | TensorFlow | JAX |
|---|---|---|---|
| 图模式 | 动态（Eager）+ compile | 静态为主 | 函数式 JIT |
| 上手 | **最易**（像写 Python） | 中 | 难（函数式） |
| 调试 | **最友好**（Eager） | 中 | 难 |
| 生产部署 | export/ONNX/AOT | TF Serving | 实验为主 |
| LLM 生态 | **最强**（HF 基于它） | 弱 | 中 |
| 性能 | compile 后接近 | 强（XLA） | 强（XLA） |
| 适用 | **通用首选** | 存量 TF 项目 | 研究实验 |

### 选型结论

```
新项目 → PyTorch（生态/上手/LLM 全栈）
存量 TF → 继续（迁移成本高）
研究实验（XLA 编译研究）→ JAX
```

## 5. 适用场景判断

### 适合 PyTorch

| 场景 | 为什么 |
|---|---|
| LLM 训练/微调 | transformers 生态基于它 |
| 深度学习研究 | 动态图调试友好 |
| 生产推理 | export/ONNX/编译 |
| 计算机视觉/NLP | 生态最全 |
| 学习深度学习 | 上手最平滑 |

### 边界

| 场景 | 说明 |
|---|---|
| 纯推理部署（已训好模型） | ONNX Runtime/TensorRT 更专（见推理体系） |
| 大数据管线 | 配 PyTorch 用（不是替代） |
| 强化学习 | RLlib 等框架基于它（仍用它） |

## 6. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| PyTorch 为什么流行？ | 动态图 + 自动微分 + HF 生态 |
| 1.x vs 2.x？ | torch.compile 编译加速（30-50%+） |
| torch.compile 原理？ | Dynamo 追踪→图优化→Inductor 生成内核 |
| 与 TensorFlow 区别？ | 动态 vs 静态；上手与生态 |
| jit 还能用吗？ | 3.14 不保证——官方建议 compile/export |
| 2026 版本状态？ | 2.10.0（AOT/确定性/Python 3.14） |

### 面试加分表达

> "PyTorch 2.x 解决了'动态图慢'的历史问题：torch.compile 把 Dynamo 追踪、图优化、Inductor 代码生成串起来，一行接入带来 30-50% 加速。2.10 的 aot_compile 消除了生产冷启动编译延迟，确定性模式方便大规模调试——研究灵活与生产性能在 2.x 统一了。"

## 7. 常见误区

| 误区 | 真相 |
|---|---|
| "PyTorch 只适合研究" | 2.x compile 后生产性能补齐 |
| "torch.jit 还能用" | 3.14 不保证——官方迁移 compile/export |
| "必须 CUDA" | CPU 可学习（小模型）；训练加速才要 GPU |
| "TensorFlow 已死" | 存量企业仍在（Keras 生态）——新项目不选 |
| "框架替代理论" | 框架是写法，理论是理解（深度学习体系互补） |
| "学习必须从零写" | 内置数据集/预训练模型可加速学习 |

## 8. 经典应用场景

| 场景 | PyTorch 的姿势 |
|---|---|
| 图像分类 | CNN + torchvision 数据集 |
| LLM 微调 | transformers + PEFT（LoRA） |
| NLP 任务 | transformers + 训练循环 |
| 生产推理 | torch.compile/AOT + FastAPI |
| 强化学习 | 基于 PyTorch 的 RL 库 |
| 生成模型 | 自研/库（扩散模型） |

### 场景落地骨架（图像分类）

```
① 数据：torchvision 数据集 + DataLoader（03 篇）
② 模型：预训练 CNN + 迁移学习（04 篇）
③ 训练：AdamW + AMP + compile（05/06 篇）
④ 部署：safetensors 保存 + FastAPI 服务（07 篇）
（08 篇有完整代码）
```

> 🎯 核心要点：PyTorch = 张量 + autograd + 动态图（上手最易）+ torch.compile（性能补齐）；2.x 统一研究灵活与生产性能；torch.compile 三阶段（追踪/优化/生成）；jit 被官方弃用（迁移 compile/export）；LLM 全栈靠 HF 生态；选型新项目首选、存量 TF 继续、实验 JAX；六个误区校准认知。

---

**下一模块**：[02-张量与自动微分](02-张量与自动微分.md) / **返回总览**：[00-Pytorch学习体系总览](00-Pytorch学习体系总览.md)
