# 08 - Vision Transformer 与多模态

> 🎯 Transformer 不仅统治了 NLP，还入侵了 CV 领域——ViT 证明"图片切成 patch 当 token"就能用纯 Transformer 处理图像。CLIP 和 DALL·E 则用 Transformer 桥接了文本和图像的鸿沟

---

## 目录

1. [ViT：视觉 Transformer](#1-vit视觉-transformer)
2. [CLIP：图文对齐](#2-clip图文对齐)
3. [多模态架构演化](#3-多模态架构演化)

---

## 1. ViT：视觉 Transformer

### 1.1 核心思想

```text
ViT = 把图片当"句子"处理

传统 CNN：卷积核扫描图片（局部感知）
ViT：把图片切成固定大小的 patch → 当成 token

图片 (224×224×3)
  → 切成 16×16 patches (14×14=196个)
  → 每个 patch 通过线性投影变成 embedding
  → 加上位置编码 → 送入标准 Transformer Encoder
  → 分类头（[CLS] token）

发现：当数据量足够大时（JFT-300M），ViT 效果超越 CNN
```

### 1.2 ViT 架构

```python
class ViT(nn.Module):
    def __init__(self, image_size=224, patch_size=16, num_classes=1000,
                 dim=768, depth=12, heads=12):
        super().__init__()
        num_patches = (image_size // patch_size) ** 2

        # Patch Embedding
        self.patch_embed = nn.Conv2d(3, dim, kernel_size=patch_size,
                                      stride=patch_size)  # 卷积实现 patch

        # [CLS] Token + Position Embedding
        self.cls_token = nn.Parameter(torch.randn(1, 1, dim))
        self.pos_embed = nn.Parameter(torch.randn(1, num_patches + 1, dim))

        # Transformer Encoder
        self.transformer = nn.TransformerEncoder(
            nn.TransformerEncoderLayer(d_model=dim, nhead=heads),
            num_layers=depth
        )

        # 分类头
        self.mlp_head = nn.Linear(dim, num_classes)

    def forward(self, x):
        # x: (batch, 3, 224, 224)
        x = self.patch_embed(x)          # → (batch, dim, 14, 14)
        x = x.flatten(2).transpose(1, 2) # → (batch, 196, dim)

        # 添加 [CLS] token
        cls_tokens = self.cls_token.expand(x.shape[0], -1, -1)
        x = torch.cat([cls_tokens, x], dim=1)  # → (batch, 197, dim)

        x = x + self.pos_embed
        x = self.transformer(x)

        return self.mlp_head(x[:, 0])  # 只用 [CLS] 的输出
```

## 2. CLIP：图文对齐

```text
CLIP (Contrastive Language-Image Pre-training)

核心思想：图文匹配——让模型学会"这张图和这段文字是不是在说同一件事"

训练：
  输入：4 亿图文对（图片 + 对应文本描述）
  架构：Image Encoder + Text Encoder
  目标：图文对的特征向量尽可能接近
  
  Image → ViT → image_features (d)
  Text  → GPT → text_features  (d)
  
  Loss = Contrastive Loss (同对的 Similarity 大，不同对的 Similarity 小)

应用：
  → 零样本分类："一张猫的照片" + 图片 → 匹配度
  → DALL·E 的文本理解基础
  → 图搜/文搜的基础模型
```

## 3. 多模态架构演化

```text
第一代（2021）：独立编码器
  Image → ViT → 特征 ─┐
  Text → BERT → 特征 ─┤→ Fusion Module → 输出
  (CLIP, ALIGN)

第二代（2023）：LLM 作为统一处理器
  Image → ViT → Image Tokens ─┐
                               ├→ LLM Decoder → 文本输出
  Text → Text Tokens ──────────┘
  (GPT-4V, LLaVA, Qwen-VL, Claude Vision)

第三代（2024+）：原生多模态
  文本 + 图片 + 音频 → 统一的 Tokenizer → 统一的 Transformer → 统一输出
  (GPT-4o, Gemini 2.5, Chameleon)
```

### 当前多模态 LLM 代表

| 模型 | 架构 | 视觉编码器 | 特点 |
|------|------|----------|------|
| GPT-4V/4o | 原生多模态 | 内置 | 最好但闭源 |
| Claude Vision | ViT + LLM | — | 处理 PDF/图表极强 |
| Gemini 2.5 | 原生多模态 | 内置 | 1M 上下文+视频 |
| LLaVA 1.6 | ViT-L + Llama | CLIP-ViT | 开源标杆 |
| Qwen-VL | ViT-G + Qwen | 自研 | 中文最佳 |

## 核心要点回顾

- ViT = 图片切 patch → 当 token → Transformer → 数据够多就超越 CNN
- CLIP = 图文对比学习 → 零样本分类 + 多模态基础
- 多模态三阶段：独立编码器 → LLM 统一 → 原生多模态
- 2025 年 GPT-4o / Gemini 证明：原生多模态 = 统一理解一切媒体

## 参考资料

1. ViT 论文 (Dosovitskiy et al., 2021)
2. CLIP 论文 (Radford et al., 2021)
3. LLaVA 论文 (Liu et al., 2023)
