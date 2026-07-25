# Meta Llama & Muse 详细知识点（2026最新）

> **定位**：全球顶级开源大模型系列（Scout/Maverick），但前沿研发已转向闭源Muse品牌（Spark 1.1），Behemoth已于2026年3月正式放弃，Llama 5延迟至2027年。Scout仍是最佳开源长上下文选项（10M），Maverick编程推理已落后。Muse成为Meta新旗舰品牌。

---

## 目录

1. [产品概述与发展历程](#1-产品概述与发展历程)
2. [模型体系详解](#2-模型体系详解)
3. [技术架构深度解析](#3-技术架构深度解析)
4. [部署方案与实践](#4-部署方案与实践)
5. [微调与定制开发](#5-微调与定制开发)
6. [开源生态与社区](#6-开源生态与社区)
7. [适用场景与典型案例](#7-适用场景与典型案例)
8. [优势与短板深度分析](#8-优势与短板深度分析)
9. [与其他开源模型对比](#9-与其他开源模型对比)
10. [Java开发者集成指南](#10-java开发者集成指南)

---

## 1. 产品概述与发展历程

### 1.1 基本信息

| 维度 | 详情 |
|------|------|
| **开发商** | Meta（原Facebook，美国） |
| **首发时间** | 2023年2月（Llama 1）；2023年7月（Llama 2，开源商用） |
| **当前版本** | Llama 4 Scout/Maverick（2025年4月）；**Muse Spark 1.1**（2026年7月，Meta新旗舰） |
| **开源协议** | Llama 4 Community License（免费商用，月活<7亿）；Muse为**闭源** |
| **产品形态** | Llama 4：开源模型权重；Muse：API-only（Meta Model API） |
| **核心卖点** | Scout 10M长上下文 + Maverick开源多模态 + Muse闭源推理旗舰 |
| **总部** | 美国门洛帕克 |

### 1.2 发展里程碑

```
2023.02  Llama 1 发布（7B/13B/33B/65B），仅研究许可
2023.07  Llama 2 发布（7B/13B/70B），首次开放商用许可
2024.04  Llama 3 发布（8B/70B），性能大幅提升
2024.07  Llama 3.1 405B，首个开源前沿级别模型
2025.04  Llama 4 Scout/Maverick发布，Behemoth预览（2T参数MoE）
2025.10  Scout长上下文优化，10M token上下文窗口
2026.01  Behemoth训练遇阻，MoE路由不稳定
2026.03  ⚠️ **Behemoth正式放弃**，Meta重组AI团队
2026.04  **Muse Spark发布**，Meta新闭源旗舰品牌（API-only）
2026.07  **Muse Spark 1.1更新**，Llama 5延迟至2027年
```

> 💡 **关键变化**：Meta的前沿AI战略已从开源Llama转向闭源Muse。Llama 4 Scout/Maverick仍是可用的开源模型，但已不再是Meta的研发重心。Muse Spark成为Meta和GPT/Claude/Gemini竞争的前沿产品。

---

## 2. 模型体系详解

### 2.1 当前模型矩阵（2026年7月）

#### Llama 系列（开源，维护模式）

| 模型 | 参数规模 | 上下文 | 定位 | 状态 |
|------|---------|--------|------|------|
| ~~**Llama 4 Behemoth**~~ | 2T MoE | 256K | 旗舰级 | ❌ **2026年3月放弃** |
| **Llama 4 Maverick** | 400B/17B激活 | 1M | 开源多模态 | ⚠️ SWE-bench仅24%，编程已落后 |
| **Llama 4 Scout** | 109B/17B激活 | **10M** | 🥇 长上下文检索 | ✅ 仍是最佳开源长上下文选项 |

#### Muse 系列（Meta新闭源旗舰）

| 模型 | 发布时间 | 定位 | 获取方式 |
|------|---------|------|---------|
| **Muse Spark** | 2026年4月 | Meta前沿推理模型 | API-only |
| **Muse Spark 1.1** | 2026年7月 | 更新版 | Meta Model API |

> ⚠️ **关键变化**：Meta于2026年3月重组AI团队为Meta Superintelligence Labs (MSL)，新任首席AI官Alexandr Wang（前Scale AI）。前沿研发全部转向闭源Muse，Llama 5（代号Avocado）延迟至2027年。

### 2.2 MoE架构详解

> 🎯 **MoE（Mixture of Experts，混合专家）**：不是每次推理都激活所有参数，而是根据输入动态选择最相关的"专家"子网络。

```
传统Dense模型（如GPT-4）：
  每次推理 → 激活全部参数 → 计算量 = 参数量

MoE模型（Llama 4）：
  每次推理 → 路由选择Top-K专家 → 仅激活约10-25%参数
  
  举例：Llama 4 Maverick（总400B参数）
  → 每次推理仅激活约50B参数
  → 推理速度接近50B Dense模型
  → 但性能达到400B级别
```

**MoE的优势**：
- 🚀 推理速度快（只激活部分参数）
- 💰 推理成本低（相同性能下计算量更小）
- 🧠 知识容量大（不同专家掌握不同领域知识）
- 📈 可扩展性强（增加专家即可扩展总参数量）

**MoE的挑战**：
- 显存占用大（所有专家都要加载到内存/显存）
- 训练不稳定（负载均衡是关键难点）
- 微调困难（需要特殊技术处理专家路由）

### 2.3 模型能力对比

| 能力维度 | Llama 4 Behemoth | Llama 4 Maverick | Llama 4 Scout | GPT-5.6（参考） |
|---------|-----------------|------------------|---------------|----------------|
| **通用推理** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **代码生成** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **多语言** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **数学推理** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **多模态** | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **推理速度** | ⭐⭐（需大集群） | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | N/A |
| **部署成本** | 💰💰💰💰💰 | 💰💰💰 | 💰💰 | N/A |

---

## 3. 技术架构深度解析

### 3.1 核心架构特性

| 特性 | Llama 4 | Llama 3 | 说明 |
|------|---------|---------|------|
| **架构类型** | MoE（混合专家） | Dense（密集） | Llama 4首次使用MoE |
| **注意力机制** | GQA（分组查询注意力） | GQA | 推理效率优化 |
| **位置编码** | RoPE（旋转位置编码） | RoPE | 支持长上下文扩展 |
| **激活函数** | SwiGLU | SwiGLU | 训练稳定性好 |
| **分词器** | TikToken-based | SentencePiece | Llama 4更换分词器 |
| **上下文窗口** | 原生128K/256K | 原生8K/128K | 大幅扩展 |
| **多模态** | 早期融合 | 部分支持 | Llama 4从训练开始就融合视觉 |

### 3.2 训练数据

| 维度 | Llama 4 |
|------|---------|
| **训练数据量** | 约20万亿token（估计） |
| **数据来源** | 公开网页、代码仓库、书籍、学术论文、多语言语料 |
| **多语言占比** | 约30%非英语（含中文、日语、韩语、阿拉伯语等） |
| **数据清洗** | 去重、质量过滤、PII去除、毒性过滤 |
| **知识截止** | 2024年12月（Llama 4初版） |

### 3.3 安全性设计

| 安全措施 | 说明 |
|---------|------|
| **Llama Guard** | 输入输出安全分类器，拦截有害内容 |
| **Prompt Guard** | 检测恶意提示注入攻击 |
| **CyberSec Eval** | 网络安全风险评估工具 |
| **Code Shield** | 检测AI生成代码中的安全漏洞 |

---

## 4. 部署方案与实践

### 4.1 部署方式对比

| 部署方式 | 适合模型 | 硬件需求 | 难度 | 成本 |
|---------|---------|---------|------|------|
| **Ollama本地部署** | Scout及以下 | 消费级GPU（RTX 4090 24GB+） | ⭐ 极简 | 仅硬件成本 |
| **vLLM推理服务** | Maverick及以下 | 企业级GPU（A100/H100×2-8） | ⭐⭐ 简单 | 硬件+电费 |
| **HuggingFace TGI** | 全部 | 对应量级GPU | ⭐⭐ 简单 | 硬件+电费 |
| **云端API服务** | 全部 | 无需 | ⭐ 极简 | 按量付费 |
| **自建集群** | Behemoth | H100×16+ | ⭐⭐⭐⭐⭐ | 极高 |

### 4.2 Ollama 本地部署（推荐入门）

```bash
# 1. 安装 Ollama（Windows/Mac/Linux）
# 下载：https://ollama.com

# 2. 拉取并运行 Llama 4 Scout（100B MoE，需要大显存）
ollama run llama4:scout

# 3. 或使用较小的 Llama 3.2 模型
ollama run llama3.2:3b    # 3B，仅需4GB显存
ollama run llama3.2:11b   # 11B 多模态，需要8GB显存

# 4. 通过 REST API 调用
curl http://localhost:11434/api/generate -d '{
  "model": "llama4:scout",
  "prompt": "解释Java中的volatile关键字",
  "stream": false
}'
```

### 4.3 vLLM 生产级部署

```bash
# vLLM：高性能推理引擎，PagedAttention优化
pip install vllm

# 启动 Llama 4 Scout 推理服务
vllm serve meta-llama/Llama-4-Scout-100B \
  --tensor-parallel-size 2 \
  --max-model-len 131072 \
  --port 8000 \
  --gpu-memory-utilization 0.95

# API调用
curl http://localhost:8000/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "meta-llama/Llama-4-Scout-100B",
    "messages": [{"role": "user", "content": "Hello!"}]
  }'
```

### 4.4 云端API服务商

| 服务商 | 支持模型 | 定价（每1M token） | 特点 |
|--------|---------|-------------------|------|
| **Together AI** | 全系列 | $0.10-$1.50（输入） | 推理速度快，模型全 |
| **Replicate** | 全系列 | 按GPU时间计费 | 一键部署，简单 |
| **Groq** | Scout | 免费层/低价 | LPU推理，极快 |
| **Fireworks AI** | 全系列 | $0.10-$1.00 | 企业级可靠 |
| **HuggingFace Inference** | 全系列 | $0.10-$1.50 | 社区友好 |

### 4.5 硬件需求参考

| 模型 | 量化方式 | 最低显存 | 推荐配置 |
|------|---------|---------|---------|
| Llama 4 Scout (100B MoE) | FP16 | ~200GB（8×H100） | 4×A100 80GB |
| Llama 4 Scout (100B MoE) | INT4 | ~50GB | 2×RTX 4090 24GB |
| Llama 3.2 70B | INT4 | ~40GB | 2×RTX 4090 24GB |
| Llama 3.2 11B Vision | INT4 | ~8GB | RTX 4070 12GB+ |
| Llama 3.2 3B | FP16 | ~6GB | RTX 3060 12GB |

---

## 5. 微调与定制开发

### 5.1 微调方法对比

| 方法 | 原理 | 显存需求 | 效果 | 适合场景 |
|------|------|---------|------|---------|
| **Full Fine-tuning** | 更新全部参数 | 极高（模型×4-6倍） | 🥇 最佳 | 有充足算力，彻底定制 |
| **LoRA** | 低秩适配，只训练少量参数 | 较低（模型×1.5倍） | ⭐⭐⭐⭐ 很好 | 垂直领域适配首选 |
| **QLoRA** | 量化+LoRA，更省显存 | 低（模型×1.2倍） | ⭐⭐⭐⭐ 很好 | 单卡微调大模型 |
| **Prefix Tuning** | 只训练前缀向量 | 极低 | ⭐⭐⭐ 良好 | 简单任务适配 |
| **Prompt Tuning** | 只训练软提示 | 极低 | ⭐⭐⭐ 良好 | 分类/生成任务 |

### 5.2 LoRA微调实战

```python
# 使用 QLoRA 微调 Llama 4 Scout（单卡A100/RTX 4090可跑）
from transformers import AutoModelForCausalLM, AutoTokenizer, BitsAndBytesConfig
from peft import LoraConfig, get_peft_model, prepare_model_for_kbit_training
from datasets import load_dataset

# 1. 4-bit量化加载模型
bnb_config = BitsAndBytesConfig(
    load_in_4bit=True,
    bnb_4bit_compute_dtype="float16",
    bnb_4bit_use_double_quant=True,
)

model = AutoModelForCausalLM.from_pretrained(
    "meta-llama/Llama-4-Scout-100B",
    quantization_config=bnb_config,
    device_map="auto",
    trust_remote_code=True,
)
model = prepare_model_for_kbit_training(model)

# 2. 配置 LoRA
lora_config = LoraConfig(
    r=16,                    # LoRA秩
    lora_alpha=32,           # 缩放参数
    target_modules=[         # 目标层
        "q_proj", "k_proj", "v_proj", "o_proj",
        "gate_proj", "up_proj", "down_proj"
    ],
    lora_dropout=0.05,
    bias="none",
    task_type="CAUSAL_LM",
)
model = get_peft_model(model, lora_config)
print(f"可训练参数: {model.print_trainable_parameters()}")

# 3. 加载领域数据并训练
dataset = load_dataset("your-domain-dataset")
# ... (TrainingArguments + Trainer配置)
```

### 5.3 微调数据准备

```json
// 微调数据格式（JSONL，每行一个对话）
{
  "messages": [
    {"role": "system", "content": "你是一个Java后端代码审查专家"},
    {"role": "user", "content": "请审查这段代码的性能问题..."},
    {"role": "assistant", "content": "发现以下性能问题：1. ... 2. ..."}
  ]
}
```

**数据量建议**：
- 最少：500-1000条高质量样本
- 推荐：5000-10000条
- 领域适配充足：50000+条

---

## 6. 开源生态与社区

### 6.1 核心工具链

| 工具 | 用途 | 链接 |
|------|------|------|
| **Ollama** | 一键本地运行LLM | ollama.com |
| **vLLM** | 高性能推理引擎 | github.com/vllm-project/vllm |
| **llama.cpp** | CPU/边缘设备推理 | github.com/ggerganov/llama.cpp |
| **Ollama** | 模型运行与API | ollama.com |
| **LM Studio** | 桌面端模型管理 | lmstudio.ai |
| **Axolotl** | 简化微调流程 | github.com/OpenAccess-AI-Collective/axolotl |
| **Unsloth** | 2-5倍加速微调 | github.com/unslothai/unsloth |
| **Text Generation Inference** | HuggingFace推理框架 | github.com/huggingface/text-generation-inference |

### 6.2 衍生模型生态

Llama开源后催生了庞大的衍生模型生态：

| 衍生模型 | 基座 | 特色 |
|---------|------|------|
| **CodeLlama** | Llama 2 | Meta官方代码专精版 |
| **Vicuna** | Llama 1 | 早期知名微调版 |
| **Alpaca** | Llama 1 | Stanford指令微调 |
| **WizardLM** | Llama 2 | 复杂指令遵循 |
| **Chinese-Llama** | Llama 2/3 | 中文词汇扩展+中文微调 |
| **Llama-3-8B-Chinese** | Llama 3 | 社区中文优化版 |

### 6.3 中文生态

| 项目 | 说明 |
|------|------|
| **Chinese-Llama-Alpaca** | 中文词表扩充+指令微调 |
| **Llama3-Chinese** | Llama 3专用中文优化 |
| **FlagAlpha/Llama3-Chinese** | 中文对话能力增强 |
| **OpenBuddy** | 多语言跨文化对话模型 |

---

## 7. 适用场景与典型案例

### 7.1 场景评级总览

| 场景 | 适合度 | 说明 |
|------|--------|------|
| **企业私有化AI** | 🥇 最佳 | 数据不出企业内网，最高安全合规保证 |
| **垂直领域定制** | 🥇 最佳 | 金融/医疗/法律等行业专属模型 |
| **学术研究** | 🥇 最佳 | 完全透明，可深入研究和改进 |
| **大规模离线推理** | 🥇 最佳 | 一次部署长期使用，无API费用 |
| **端侧/边缘部署** | 🥇 最佳 | 1B/3B模型可在手机/PC本地运行 |
| **通用日常使用** | ⭐⭐ 有限 | 无官方网页端，需自行搭建前端 |
| **快速上手体验** | ⭐ 极低 | 不适合非技术用户 |

### 7.2 典型案例

```
🏢 金融企业私有化部署：
   需求：某证券公司需要AI辅助研报撰写，但研报涉及未公开数据
   方案：部署Llama 4 Scout + 内部研报数据LoRA微调 → 
   完全内网运行，数据不出公司，成本可控

📱 移动端AI助手：
   需求：在App内集成离线翻译功能
   方案：使用Llama 3.2 3B量化版 → 
   手机本地运行，无需联网，响应<500ms

🔬 高校研究平台：
   需求：某大学需要可自由修改的基座模型进行AI安全研究
   方案：基于Llama 4全系列 → 
   对比不同尺寸模型的安全性，发表论文

🏥 医疗病历处理：
   需求：医院需要AI辅助病历撰写，但患者数据绝对不能外传
   方案：Llama 4 Scout私有化部署 + 脱敏病历微调 →
   内网运行，符合HIPAA/GDPR合规
```

---

## 8. 优势与短板深度分析

### 8.1 核心优势（护城河）

| 优势 | 详细说明 |
|------|---------|
| **1. 完全开源自由** | 权重开放，可商用，无数据锁限制，企业可放心深度使用 |
| **2. 私有化部署** | 数据不出内网，满足金融/医疗/政府最高合规要求 |
| **3. 可定制微调** | LoRA/QLoRA低成本微调，打造企业专属垂直模型 |
| **4. 成本完全可控** | 一次硬件投入长期使用，无API调用费用 |
| **5. 透明可审计** | 模型权重/训练数据/方法论公开，可深入审计和学术研究 |
| **6. 社区生态丰富** | 工具链完善（Ollama/vLLM/llama.cpp），衍生模型众多 |
| **7. 端侧部署** | 1B/3B小模型可在手机/PC本地运行 |

### 8.2 核心短板

| 短板 | 严重程度 | 详细说明 |
|------|---------|---------|
| **无消费级产品** | ⭐⭐⭐⭐⭐ | 无官方网页端/App，非技术用户无法直接使用 |
| **部署门槛高** | ⭐⭐⭐⭐ | 需要GPU/运维/ML工程能力，不适合个人开发者 |
| **能力不如闭源** | ⭐⭐⭐ | Behemoth虽强但部署成本极高，Scout/Maverick能力不如GPT-5.6/Claude |
| **多模态还在追赶** | ⭐⭐⭐⭐ | 多模态能力不如Gemini/ChatGPT原生多模态 |
| **中文不够强** | ⭐⭐⭐ | 中文能力不如国产模型，中文微调后才有竞争力 |
| **缺少生态整合** | ⭐⭐⭐⭐⭐ | 无GPTs/插件/Operator/Workspace集成等消费生态 |
| **持续更新依赖社区** | ⭐⭐⭐ | 基座模型更新慢于闭源模型（2-3代差距） |
| **知识产权争议** | ⭐⭐⭐ | 训练数据来源和版权问题仍在法律争议中 |

### 8.3 适用/不适用场景决断

```
✅ 应该用Llama 4：
├── 金融/医疗/政府等数据敏感行业
├── 需要深度定制（微调）的垂直领域
├── 大规模API调用（成本敏感）
├── 学术研究和实验
├── 边缘/端侧设备部署
└── 需要完全透明和可控的AI基础设施

❌ 不应该用Llama 4：
├── 需要最前沿AI能力（选ChatGPT/Claude）
├── 需要多模态处理（选Gemini/ChatGPT）
├── 需要开箱即用的用户体验（选豆包/ChatGPT）
├── 没有GPU和技术团队
└── 需要频繁更新最新知识（不如联网的闭源模型）
```

---

## 9. 与其他开源模型对比

### 9.1 开源模型生态对比

| 维度 | Llama 4 | Qwen3.8 | DeepSeek-V4 | Mistral Large | Gemma 3 |
|------|---------|---------|-------------|---------------|---------|
| **开发商** | Meta | 阿里 | 深度求索 | Mistral AI | Google |
| **最大开源模型** | Maverick 400B | **2.4T**（承诺开源） | V4-Pro 1.6T | 123B | 27B |
| **前沿模型** | Muse（闭源） | Qwen3.8-Max（即将开源） | V4-Pro（MIT开源） | Mistral Large 3 | 无 |
| **开源协议** | 特殊（月活<7亿） | Apache 2.0 | 🥇 MIT | Apache 2.0 | Gemma License |
| **中文能力** | ⭐⭐⭐ | 🥇 国产最强 | 🥇 国产最强 | ⭐⭐ | ⭐⭐ |
| **SWE-bench** | Maverick ~24% | ~77% | 🥇 80.6% | ~30% | ⭐⭐⭐ |
| **长上下文** | 🥇 Scout 10M | 1M | 🥇 1M | 256K | 128K |
| **当前趋势** | ⚠️ 开源停滞 | 🚀 快速迭代 | 🚀 V4领先 | 稳定 | 稳定 |

### 9.2 中文场景选开源模型建议

```
个人开发者（无GPU）：
  → Qwen3-7B + Ollama（电脑本地跑）
  → DeepSeek-V3 API（便宜好用）

企业团队（有GPU）：
  → Qwen3-72B 部署（中文最佳 + Apache 2.0自由商用）
  → DeepSeek-V3 部署（代码最强 + MIT完全自由）

全球化产品：
  → Llama 4 Scout/Maverick（英文+ 多语言平衡）
  → Mistral Large（欧洲合规首选）
```

---

## 10. Java开发者集成指南

### 10.1 通过 Ollama REST API 调用

```java
// 本地Ollama部署的Llama模型，通过HTTP调用
import java.net.http.*;
import java.net.URI;

public class OllamaClient {
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String generate(String model, String prompt) throws Exception {
        String json = String.format(
            "{\"model\":\"%s\",\"prompt\":\"%s\",\"stream\":false}",
            model, prompt.replace("\"", "\\\"")
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(OLLAMA_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = httpClient.send(
            request, HttpResponse.BodyHandlers.ofString());

        return response.body(); // 解析JSON提取response字段
    }

    public static void main(String[] args) throws Exception {
        OllamaClient client = new OllamaClient();
        String result = client.generate(
            "llama4:scout",
            "用Java实现线程安全的LRU缓存"
        );
        System.out.println(result);
    }
}
```

### 10.2 Spring AI 集成 Ollama

```java
// application.yml
// spring.ai.ollama.base-url=http://localhost:11434
// spring.ai.ollama.chat.options.model=llama4:scout

@RestController
public class LlamaController {

    private final ChatClient chatClient;

    public LlamaController(ChatClient.Builder builder) {
        this.chatClient = builder
            .defaultSystem("你是一个Java后端开发助手")
            .build();
    }

    @GetMapping("/llama/chat")
    public String chat(@RequestParam String question) {
        return chatClient.prompt()
            .user(question)
            .call()
            .content();
    }
}
```

### 10.3 通过托管API调用（Together AI示例）

```java
// 使用标准的OpenAI兼容API格式调用Llama
// Together AI 等托管服务商提供OpenAI兼容接口
// 可直接使用OpenAI Java SDK指向Together AI endpoint

OpenAIClient client = OpenAIClient.builder()
    .apiKey(System.getenv("TOGETHER_API_KEY"))
    .baseUrl("https://api.together.xyz/v1")
    .build();

ChatCompletion completion = client.chat().completions().create(
    ChatCompletionCreateParams.builder()
        .model("meta-llama/Llama-4-Maverick-400B")
        .addUserMessage("解释JVM垃圾回收机制")
        .maxTokens(2048)
        .build()
);
```

---

## 核心要点回顾

- **2026重大变化**：Behemoth已放弃（2026.03），Meta前沿AI战略从开源Llama转向闭源**Muse Spark**
- **Llama 4现状**：Scout（10M长上下文，仍是最佳开源长上下文选项）、Maverick（SWE-bench仅24%，编程已落后）
- **Muse Spark**：Meta新闭源推理旗舰（2026.04发布，7月更新1.1），API-only，不对外开放模型权重
- **Llama 5**：代号Avocado，延迟至**2027年**
- **开源格局变化**：DeepSeek-V4-Pro（MIT）和Qwen3.8（承诺开源）已取代Llama成为开源社区新中心
- **适用建议**：Scout用于长上下文检索场景，Maverick仅在需要英文开源多模态时考虑
- **中文场景**：建议Qwen3.8/DeepSeek-V4-Pro，不要选Llama 4

---

## 参考资料

1. Meta Llama 官方 - https://llama.meta.com
2. Muse Spark - Meta Model API
3. Llama GitHub - https://github.com/meta-llama
4. Ollama - https://ollama.com
5. vLLM - https://github.com/vllm-project/vllm
6. Llama 4 Complete Guide 2026 - https://codersera.com/blog/llama-4-complete-guide-2026/

---

## 参考资料

1. Meta Llama 官方 - https://llama.meta.com
2. Llama GitHub - https://github.com/meta-llama
3. Ollama - https://ollama.com
4. vLLM - https://github.com/vllm-project/vllm
5. Together AI - https://www.together.ai
6. HuggingFace Llama - https://huggingface.co/meta-llama
7. llama.cpp - https://github.com/ggerganov/llama.cpp
8. Chinese-Llama-Alpaca - https://github.com/ymcui/Chinese-LLaMA-Alpaca
