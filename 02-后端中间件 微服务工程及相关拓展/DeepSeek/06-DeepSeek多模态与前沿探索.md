# DeepSeek 多模态与前沿探索

> 👁️ 统一理解与生成：Janus 双编码器架构、DeepSeek-VL2 视觉语言模型、以及多模态 AI 的前沿探索

---

## 📚 目录

1. [多模态模型概览](#1-多模态模型概览)
2. [DeepSeek-VL2：视觉语言理解](#2-deepseek-vl2视觉语言理解)
3. [Janus：统一理解与生成](#3-janus统一理解与生成)
4. [Janus-Pro：升级版本](#4-janus-pro升级版本)
5. [多模态能力对比](#5-多模态能力对比)
6. [使用指南](#6-使用指南)

---

## 1. 多模态模型概览

### 1.1 DeepSeek 多模态产品线

```text
DeepSeek 多模态模型家族：

  DeepSeek-VL (2024.03)
    └── 第一代视觉语言模型
    └── 支持图像理解和描述

  DeepSeek-VL2 (2024.12)
    └── 第二代视觉语言模型
    └── MoE 架构，支持动态分辨率
    └── 三种规格：Tiny / Small / Large

  Janus (2024.11)
    └── 统一多模态理解与生成
    └── 双编码器设计：理解路径 + 生成路径
    └── 1.3B 参数的小型高效模型

  Janus-Pro (2025.01)
    └── Janus 的升级版本
    └── 7B 参数
    └── 更强的理解和生成能力
```

### 1.2 核心设计哲学

```text
DeepSeek 多模态的核心思想：

  传统做法：
    视觉理解模型 + 图像生成模型 = 两个独立系统

  DeepSeek 的做法（Janus）：
    理解 + 生成 → 一个统一模型
    → 双编码器解耦视觉表示
    → 共享同一个 LLM Backbone
    → 用同一个模型既能"看"又能"画"
```

---

## 2. DeepSeek-VL2：视觉语言理解

### 2.1 模型架构

```text
DeepSeek-VL2 架构：

  Image → Vision Encoder → Dynamic Resolution → LLM Backbone → Text Output

  ┌──────────────────────────────────────────────────┐
  │                                                  │
  │   输入图片                                       │
  │      │                                           │
  │      ▼                                           │
  │   Vision Encoder                                 │
  │   (SigLIP / SAM 混合编码)                        │
  │      │                                           │
  │      ▼                                           │
  │   动态分辨率处理                                  │
  │   (Dynamic Tile Strategy)                        │
  │      │                                           │
  │      ▼                                           │
  │   MLP Projector → 映射到 LLM 空间                │
  │      │                                           │
  │      ▼                                           │
  │   MoE LLM Backbone                               │
  │   (DeepSeekMoE + MLA)                            │
  │      │                                           │
  │      ▼                                           │
  │   文本回答                                        │
  │                                                  │
  └──────────────────────────────────────────────────┘
```

### 2.2 动态分辨率策略

```text
问题：
  标准 ViT 将图片固定裁剪为 224×224 或 336×336
  → 丢失细节信息（文字、小物体）

DeepSeek-VL2 的动态分辨率方案：

  1. 将图片按长宽比分割为多个 tile
     例如：1920×1080 → 分割为 6×3=18 个 336×336 的 tile

  2. 每个 tile 独立编码（Vision Encoder）

  3. 额外编码一个全局缩略图（Global View）
     → 捕获整体布局

  4. 所有编码拼接后送入 LLM

  效果：
    ✅ 高分辨率图片不丢失细节
    ✅ 支持任意长宽比
    ✅ 文档 OCR、图表识别能力显著提升
```

### 2.3 三种规格

| 规格 | 总参数 | 激活参数 | 视觉编码器 | 适用场景 |
|------|:-----:|:-------:|----------|---------|
| **VL2-Tiny** | 4.5B | 1.0B | SigLIP-400M | 移动端/边缘 |
| **VL2-Small** | 17B | 3.5B | SigLIP-400M | 消费级 GPU |
| **VL2-Large** | 107B | 12B | SigLIP-400M | 高性能服务 |

### 2.4 视觉理解能力

```text
DeepSeek-VL2 支持的任务：

  📷 图像描述（Image Captioning）
     "请描述这张图片"

  ❓ 视觉问答（Visual QA）
     "图中的猫是什么颜色？"

  📄 文档理解（Document Understanding）
     "总结这份 PDF 的内容"

  📊 图表分析（Chart Analysis）
     "分析这个柱状图的数据趋势"

  🔍 光学字符识别（OCR）
     "提取图片中的所有文字"

  🌐 网页截图理解（Screenshot → Code）
     "将这个 UI 截图转为 HTML/CSS"
```

---

## 3. Janus：统一理解与生成

### 3.1 核心创新：解耦视觉编码

```text
传统统一模型的问题：

  理解任务需要：语义信息 → 高层特征
  生成任务需要：像素信息 → 低层细节

  共用同一个编码器 → 矛盾！
    理解好的特征 = 丢失细节（不利于生成）
    生成好的特征 = 充满噪声（不利于理解）

Janus 的解决方案 —— 双编码器架构：

  ┌────────────────────────────────────────────────┐
  │                                                │
  │          输入图片                               │
  │             │                                  │
  │   ┌─────────┴─────────┐                       │
  │   ▼                   ▼                        │
  │  Understanding       Generation               │
  │  Encoder             Encoder                   │
  │  (SigLIP)            (VQ Tokenizer)           │
  │   │                   │                        │
  │   │  语义特征         │  离散视觉 token        │
  │   │                   │                        │
  │   └─────────┬─────────┘                       │
  │             ▼                                  │
  │      LLM Backbone                              │
  │      (DeepSeek-LLM)                            │
  │             │                                  │
  │   ┌─────────┴─────────┐                       │
  │   ▼                   ▼                        │
  │  文本输出            图像输出                   │
  │  ("一只猫")          (VQ Decode → PNG)          │
  │                                                │
  └────────────────────────────────────────────────┘
```

### 3.2 两个编码器的角色

```text
理解编码器（Understanding Encoder —— SigLIP）：

  → 输入：原始像素
  → 输出：高层语义特征向量
  → 通过 MLP Projector 映射到 LLM 输入空间
  → 用于：看图回答、识别物体、OCR 等

生成编码器（Generation Encoder —— VQ Tokenizer）：

  → 输入：原始像素
  → 输出：离散的视觉 Token（类似文本 token）
  → 直接拼接到文本 token 序列
  → 用于：图像生成、图像编辑
```

### 3.3 训练流程

```text
Janus 的三阶段训练：

  Stage 1：适配器训练（Adaptor Training）
    ├── 冻结 LLM 和视觉编码器
    ├── 仅训练 Projector / Connector
    └── 目标：让 LLM 理解视觉 token

  Stage 2：统一预训练（Unified Pre-training）
    ├── 解冻 LLM
    ├── 混合训练：多模态理解 + 图像生成 + 纯文本
    └── 目标：统一三种能力到一个模型

  Stage 3：监督微调（SFT）
    ├── 高质量多模态指令数据
    └── 目标：对齐人类偏好、提升指令遵循能力
```

### 3.4 纯文本能力保持

```text
Janus 的一个关键设计考量：

  问题：
    加入多模态训练后，纯文本能力如何保持？

  DeepSeek 的解决方案：
    → 训练数据中保留 60%+ 纯文本数据
    → 视觉 token 和文本 token 共享同一个 LLM
    → 纯文本推理时，完全不引入视觉编码器开销

  结果：
    Janus-1.3B 的纯文本能力 ≈ DeepSeek-LLM-1.3B
    (多模态训练几乎没有损害纯文本能力)
```

---

## 4. Janus-Pro：升级版本

### 4.1 相对 Janus 的改进

| 改进方向 | Janus (1.3B) | Janus-Pro (7B) |
|---------|:-----------:|:-------------:|
| **模型规模** | 1.3B | 7B |
| **训练数据量** | 基础 | 扩大 3x+ |
| **分辨率** | 384×384 | 768×768 |
| **图像生成质量** | 基础 | 显著提升 |
| **多轮对话能力** | 有限 | 增强 |
| **编码器升级** | 基础 SigLIP | 改进的 SigLIP |
| **合成数据** | 少量 | 大量使用（扩增训练） |

### 4.2 图像生成对比

```text
Janus vs Janus-Pro 图像生成对比：

  场景：根据文本描述生成图片

  Janus-1.3B：
    → 能生成与描述相关的内容
    → 细节不足、分辨率有限
    → 适合概念验证

  Janus-Pro-7B：
    → 生成质量接近专用生成模型（SDXL 级别）
    → 细节丰富、分辨率高
    → 同时保留强大的理解能力
    → 可以"看着自己生成的图片"进行讨论和修改
```

### 4.3 统一模型的独特优势

```text
理解 + 生成统一的独特能力：

  1. Self-Refinement（自我改进）：
     模型生成图片 → 模型自己看图 → 判断好坏 → 改进生成

  2. Image Editing by Conversation（对话式编辑）：
     "把这个猫的颜色改成橙色"
     → 模型理解指令 → 修改图片 → 输出新图片

  3. Multi-turn Visual Dialogue（多轮视觉对话）：
     Human: [发一张图] "这是什么？"
     AI: "这是一个苹果"
     Human: "生成一个被咬了一口的苹果"
     AI: [生成图片]

  4. Visual Reasoning + Generation：
     "画一个函数 f(x)=x² 的图像"
     → 模型先推理函数形状 → 生成图像
```

---

## 5. 多模态能力对比

### 5.1 理解任务基准

| 基准 | Janus-Pro | GPT-4V | Gemini Pro Vision | Qwen-VL-Max |
|------|:-------:|:------:|:---------------:|:----------:|
| **MMBench** | 78.2 | 83.1 | 80.3 | 82.0 |
| **MME** | 1680 | 1926 | 1850 | - |
| **SEED-Bench** | 74.5 | 76.3 | - | 72.8 |
| **OCRBench** | 780 | 836 | 800 | - |
| **DocVQA** | 85.3 | 88.4 | 86.5 | 87.1 |
| **ChartQA** | 78.9 | 85.7 | 82.1 | 80.5 |

### 5.2 生成能力对比

| 维度 | Janus-Pro | SDXL | DALL-E 3 | Midjourney v6 |
|------|:-------:|:----:|:--------:|:-----------:|
| **生成质量** | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **指令遵循** | ⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **理解能力** | ⭐⭐⭐⭐⭐ | - | - | - |
| **多轮交互** | ⭐⭐⭐⭐⭐ | - | - | - |
| **统一模型** | ✅ | ❌ | ❌ | ❌ |
| **开源** | ✅ | ✅ | ❌ | ❌ |

> 💡 Janus-Pro 的独特价值在于"统一"——虽然单一维度（生成）不如专用模型，但它是目前唯一同时具备强理解和生成能力的开源统一模型。

---

## 6. 使用指南

### 6.1 Janus-Pro 推理示例

```python
from transformers import AutoModelForCausalLM, AutoTokenizer
import torch
from PIL import Image

# 加载模型
model = AutoModelForCausalLM.from_pretrained(
    "deepseek-ai/Janus-Pro-7B",
    trust_remote_code=True,
    torch_dtype=torch.bfloat16,
    device_map="auto"
)

# 图像理解
image = Image.open("photo.jpg")
response = model.chat(
    image=image,
    question="请详细描述这张图片的内容和氛围。"
)
print(response)

# 图像生成
response = model.generate_image(
    prompt="一只橙色的猫坐在窗台上，阳光从窗户照进来"
)
response.save("generated_cat.png")

# 多模态对话
conversation = [
    {"image": "chart.png", "question": "分析这个销售图表"},
    {"question": "根据分析，生成一张改进后的图表"},  # 模型可生成新图表
]
```

### 6.2 DeepSeek-VL2 API 调用

```python
import openai

client = openai.OpenAI(
    base_url="https://api.deepseek.com/v1",
    api_key="sk-xxx"
)

response = client.chat.completions.create(
    model="deepseek-vl2",
    messages=[
        {
            "role": "user",
            "content": [
                {
                    "type": "image_url",
                    "image_url": {
                        "url": "https://example.com/chart.png"
                    }
                },
                {
                    "type": "text",
                    "text": "这张图表显示了什么趋势？请详细分析。"
                }
            ]
        }
    ],
    max_tokens=1024
)

print(response.choices[0].message.content)
```

### 6.3 部署建议

| 模型 | 推荐显存 | 推理框架 | 适用场景 |
|------|:------:|---------|---------|
| VL2-Tiny (4.5B) | 8 GB | vLLM / Transformers | 边缘设备 |
| VL2-Small (17B) | 24 GB | vLLM | 单卡服务 |
| VL2-Large (107B) | 80 GB+ | vLLM (TP) | 高性能服务 |
| Janus-1.3B | 4 GB | Transformers | 极低成本 |
| Janus-Pro-7B | 16 GB | Transformers / vLLM | 平衡选择 |

---

> 🎯 **一句话总结**：DeepSeek 多模态的独特路线是 **"统一而非分离"** —— Janus 通过双编码器解耦视觉表示，让同一个模型同时具备理解和生成能力，代表了多模态 AI 向通用智能迈进的重要方向。

---

**下一模块**：[07-DeepSeek-API与工程落地实践](./07-DeepSeek-API与工程落地实践.md) → 从 API 调用到本地部署的完整工程实践

---

*最后更新：2026年7月*
