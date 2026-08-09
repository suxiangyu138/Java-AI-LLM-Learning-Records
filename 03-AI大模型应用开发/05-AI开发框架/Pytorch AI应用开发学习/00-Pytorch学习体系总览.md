# PyTorch AI 应用开发学习体系总览

> PyTorch 2.x 框架实操体系（2026-08 基准：2.10.0）——张量→自动微分→数据→模型→训练→GPU→部署→LLM 应用，与 `03-AI.../深度学习/`（理论体系）分工互补：理论讲"是什么"，本体系讲"怎么写"。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [2026 年关键状态](#5-2026-年关键状态)
6. [与仓库其他体系的分工](#6-与仓库其他体系的分工)
7. [学习计划与 FAQ](#7-学习计划与-faq)
8. [核心数据速记](#8-核心数据速记)

## 1. 知识体系导图

```
PyTorch AI 应用开发（框架实操 · 2.10.0 · 2026-08）
│
├── 01 概述              2.x 时代的 PyTorch
│   ├── torch.compile 生态成熟
│   ├── vs TensorFlow / JAX
│   └── 与深度学习理论体系分工
│
├── 02 张量与自动微分     一切的基础
│   ├── Tensor 创建/运算/视图
│   ├── autograd 计算图
│   └── 梯度流与控制
│
├── 03 数据加载           数据管道
│   ├── Dataset / DataLoader
│   ├── transforms 预处理
│   └── 并行与性能
│
├── 04 模型构建           nn.Module 体系
│   ├── 常用层/激活/损失
│   ├── 自定义模型（forward）
│   └── Sequential / 参数管理
│
├── 05 训练循环           训练三要素
│   ├── 前向/损失/反向/更新
│   ├── 优化器与调度器
│   └── torch.compile 接入
│
├── 06 GPU 与分布式       加速与扩展
│   ├── CUDA 设备管理
│   ├── AMP 混合精度
│   └── DataParallel / DDP
│
├── 07 保存部署与推理     从训练到上线
│   ├── 保存/加载/检查点
│   ├── torch.export / ONNX
│   ├── torch.compile AOT
│   └── 推理服务化
│
└── 08 LLM 与实战面试      PyTorch 与 LLM
    ├── transformers 加载微调
    ├── 显存/精度实践
    └── 面试速记
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 01 | 概述 | 2.x 定位、torch.compile、框架对比 | 入门必读 |
| 02 | 张量与自动微分 | Tensor/autograd/计算图 | 入门必读 |
| 03 | 数据加载 | Dataset/DataLoader/transforms | 必备 |
| 04 | 模型构建 | nn.Module/层/自定义模型 | 必备 |
| 05 | 训练循环 | 三要素/优化器/compile | 核心 |
| 06 | GPU 与分布式 | CUDA/AMP/DDP | 进阶 |
| 07 | 保存部署 | export/ONNX/AOT/服务化 | 生产 |
| 08 | LLM 实战面试 | transformers/微调/面试 | 面试/落地 |

## 3. 学习路线推荐

**路线一：入门实操（3 天）**
01 概述 → 02 张量 → 03 数据 → 04 模型 → 05 训练

**路线二：生产进阶（2 天）**
06 GPU → 07 部署 → 08 LLM 应用

**路线三：AI 工程师全栈（5 天）**
路线一 + 路线二 + 对照深度学习理论体系

> 前置建议：先有 Python 基础；深度学习的"理论"（神经网络/CNN/损失函数）看 `03-AI.../深度学习/` 体系，本体系专注框架实操。

## 4. 核心概念速查

| 概念 | 一句话速记 |
|---|---|
| PyTorch | 动态图深度学习框架（2.x 时代） |
| Tensor | 张量（多维数组，GPU 可加速） |
| autograd | 自动微分（计算图 + 反向传播） |
| nn.Module | 模型基类（参数管理 + forward） |
| Dataset/DataLoader | 数据封装与批量加载 |
| transforms | 数据预处理（ToTensor/归一化） |
| 损失函数 | 衡量预测与真值的差距 |
| 优化器 | 更新参数（SGD/Adam/AdamW） |
| 训练循环 | 前向→损失→反向→更新（四步） |
| torch.compile | 编译加速（2.x 核心，jit 的替代） |
| AMP | 自动混合精度（FP16+FP32） |
| DDP | 分布式数据并行（多卡训练） |
| torch.export | 导出模型（静态图，生产部署） |
| ONNX | 跨框架模型格式 |
| AOT 编译 | aot_compile 预编译（消除冷启动） |

## 5. 2026 年关键状态

| 维度 | 状态（2026-08 基准） |
|---|---|
| 版本 | 2.10.0（2026-01/02 发布） |
| Python | 3.14 全面支持（torch.compile）；jit 在 3.14 不保证 |
| 编译器 | torch.compile 成熟：aot_compile/缓存隔离/确定性 |
| 定位 | torch.compile 为 jit 的官方替代 |
| 新算子 | varlen_attn（ragged/packed 序列） |
| 硬件 | Intel GPU 扩展（FP8/Windows SYCL） |

## 6. 与仓库其他体系的分工

| 体系 | 位置 | 分工 |
|---|---|---|
| 深度学习（理论） | `03-AI.../深度学习/` | 神经网络原理/CNN/RNN/迁移（是什么） |
| 模型推理与部署 | `03-AI.../模型推理与部署/` | KV Cache/量化/压测（推理侧） |
| **PyTorch 实操** | **本体系** | 框架写法/训练/部署代码（怎么写） |
| Python 生态 | `03-AI.../01-Python语言/` | Python 基础与生态 |

> 用法：理论不懂查深度学习体系，代码不会查本体系，部署推理查模型推理与部署体系——三套互补。

## 7. 学习计划与 FAQ

### 学习计划

| 天 | 内容 | 目标 |
|---|---|---|
| Day 1-2 | 01-02（概述/张量） | 地基：Tensor + autograd |
| Day 3 | 03-04（数据/模型） | 数据管道 + 模型构建 |
| Day 4 | 05-06（训练/GPU） | 训练循环 + 加速 |
| Day 5 | 07-08（部署/LLM） | 上线 + LLM 应用 |

### 常见疑问快答

| 疑问 | 回答 |
|---|---|
| 与深度学习理论体系区别？ | 理论讲"是什么"，本体系讲"怎么写" |
| 需要先学 NumPy 吗？ | 建议——Tensor 与 numpy 相似 |
| 必须 GPU 吗？ | 学习 CPU 够（小模型）；训练加速要 GPU |
| 与 TensorFlow 学哪个？ | 新项目 PyTorch（生态/LLM 全栈） |
| LLM 与 PyTorch 关系？ | LLM 生态建立在 PyTorch 之上 |
| 安全要注意什么？ | torch.load 必须 weights_only=True |

## 8. 核心数据速记

| 数据 | 数值 |
|---|---|
| 当前版本 | 2.10.0（2026-01） |
| torch.compile 加速 | 30-50%+ |
| AMP 收益 | 显存减半、速度 1.5-3 倍 |
| 显存公式（推理） | 参数 × 2 字节（FP16） |
| 显存公式（训练） | 参数 × 18 字节 |
| LoRA 可训练参数 | <1% |
| 7B 推理/训练 | 14GB / 126GB（LoRA ~25GB） |
| DataLoader workers | CPU 核数一半 |

---

## 参考来源

- [PyTorch 2.10.0 Release（GitHub）](https://github.com/pytorch/pytorch/releases/tag/v2.10.0)
- [PyTorch 2.10 发布博客（PyTorch Taiwan）](https://pytorch.com.tw/blog/pytorch-2-10-release-blog/)
- [Ahead-of-Time Compilation with torch.compile（官方文档）](https://docs.pytorch.org/docs/2.13/user_guide/torch_compiler/torch.compiler_aot_compile.html)
- [The PyTorch Tsunami of 2026（Plain English）](https://python.plainenglish.io/the-pytorch-tsunami-of-2026-whats-changing-whats-broken-and-what-you-ll-brag-about-tomorrow-7b7a9d14a463)

---

**下一模块**：[01-PyTorch概述-深度学习框架](01-PyTorch概述-深度学习框架.md)
