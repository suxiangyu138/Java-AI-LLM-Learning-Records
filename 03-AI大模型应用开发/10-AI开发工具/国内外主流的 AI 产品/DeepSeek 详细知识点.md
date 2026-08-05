# DeepSeek 详细知识点（2026最新 · V4 Pro）

> **定位**：全球顶级开源大模型，DeepSeek-V4-Pro（1.6T参数MoE/49B激活/100万token上下文）MIT开源，LiveCodeBench 93.5和Codeforces 3206分登顶编程基准测试，Agent能力开源最强，API价格仅为Claude Opus的1/7，适合自研AI平台、算法竞赛、高并发API调用。

---

## 目录

1. [产品概述与发展历程](#1-产品概述与发展历程)
2. [模型体系详解](#2-模型体系详解)
3. [代码能力深度解析](#3-代码能力深度解析)
4. [DeepSeek-V4 Pro 深度解析](#4-deepseek-v4-pro-深度解析)
5. [部署方案与实践](#5-部署方案与实践)
6. [API与定价](#6-api与定价)
7. [适用场景与典型案例](#7-适用场景与典型案例)
8. [优势与短板深度分析](#8-优势与短板深度分析)
9. [与其他代码模型对比](#9-与其他代码模型对比)
10. [Java开发者集成指南](#10-java开发者集成指南)

---

## 1. 产品概述与发展历程

### 1.1 基本信息

| 维度 | 详情 |
|------|------|
| **开发商** | 深度求索（DeepSeek，中国·杭州） |
| **创始背景** | 幻方量化旗下AI公司 |
| **首发时间** | 2023年11月（DeepSeek Coder）；2024年12月（DeepSeek-V3）；2026年4月（DeepSeek-V4 Pro） |
| **当前版本** | **DeepSeek-V4-Pro** / V4-Flash（2026年7月） |
| **产品形态** | 开源模型 + API + 网页端（chat.deepseek.com） |
| **开源协议** | MIT（完全自由商用，无任何限制） |
| **付费模式** | 开源免费自部署 + API按量（极低价） |
| **核心卖点** | MIT开源 + 1.6T MoE + 100万token上下文 + Codeforces 3206分 |
| **总部** | 中国杭州 |

### 1.2 发展里程碑

```
2023.11  DeepSeek Coder 发布（1.3B-33B），开源代码模型
2024.05  DeepSeek-V2 发布，MoE架构，性能大幅提升
2024.12  DeepSeek-V3 发布，671B MoE，训练成本仅$5.6M震惊业界
2025.01  DeepSeek-R1 发布，推理模型，对标OpenAI o1
2025.03  DeepSeek App登顶多国应用商店榜首
2025.10  API价格极低，引发国内大模型价格战
2026.01  内部已用V4替代Claude作为编码标配
2026.04  **DeepSeek-V4-Pro 预览版发布**，1.6T MoE，100万token上下文，MIT开源
2026.07  V4稳定完整版上线，旧版API标识（deepseek-chat/reasoner）正式下线
```

> 💡 **关键洞察**：DeepSeek V4 Pro是开源模型的里程碑——LiveCodeBench 93.5和Codeforces 3206分在编程基准上登顶，Agent能力开源最强，NIST评估落后美国前沿约8个月，但API成本仅为Claude Opus的1/7。V4已替代Claude成为DeepSeek内部编码标配模型。

---

## 2. 模型体系详解

### 2.1 当前模型矩阵（2026年7月）

| 模型 | 总参数 | 激活参数 | 上下文 | 定位 |
|------|--------|---------|--------|------|
| **DeepSeek-V4-Pro** | 1.6T MoE | 49B | **1M token** | 🥇 旗舰，编程/Agent/推理全能 |
| **DeepSeek-V4-Flash** | 284B MoE | 13B | 1M token | 快速+低价，日常主力 |
| **DeepSeek-V3** | 671B MoE | 37B | 128K | 上代旗舰（已由V4取代） |
| **DeepSeek-R1** | 671B MoE | 37B | 128K | 深度推理（V4已整合推理模式） |

> ⚠️ **2026年7月24日**：旧版API标识（`deepseek-chat`、`deepseek-reasoner`）正式下线，全面切换至V4专用模型ID。

### 2.2 V4 Pro 架构创新

```
V4 Pro 相比 V3 的四大架构突破：

1. 混合注意力架构（Hybrid Attention）
   ├── CSA（压缩稀疏注意力）：高效处理长距离依赖
   └── HCA（重度压缩注意力）：极致压缩KV Cache
   → 1M上下文下，推理FLOPs仅为V3.2的27%，KV缓存仅10%

2. 流形约束超连接（mHC）
   → 替代传统残差连接，稳定深层网络信号传播
   → 1.6T参数、深层架构仍能稳定训练

3. Muon优化器
   → 替代AdamW，收敛更快、训练更稳定
   → 在32-33万亿token预训练数据上高效学习

4. FP4/FP8混合精度训练
   → MoE专家参数使用FP4精度（业界首创）
   → 其余参数使用FP8精度
   → 大幅降低训练显存和计算开销
```

### 2.3 三档推理模式

| 模式 | 说明 | 适用场景 |
|------|------|---------|
| **Non-think** | 快速直出，低延迟 | 日常编码、简单问答、高并发API |
| **Think High** | 常规深度思考 | 复杂算法、系统设计、代码审查 |
| **Think Max**（V4-Pro-Max） | 最大推理深度 | 竞赛数学、极难算法、前沿研究 |

### 2.4 训练成本优势（V3时代经典数据）

| 模型 | 训练成本（估算） | 训练算力 |
|------|----------------|---------|
| DeepSeek-V3 | **$5.6M** | 2,788K H800 GPU小时 |
| Llama 3.1 405B | ~$60M | 30,840K H100 GPU小时 |
| GPT-5（估算） | ~$100M+ | 未知 |

> 🎯 **V3以1/10-1/20的成本达到同级性能**。V4 Pro的训练成本未公开，但延续了DeepSeek极致成本效率的基因。

---

## 3. 代码能力深度解析

### 3.1 代码能力全景（V4 Pro实测）

| 能力 | 评级 | 基准成绩 | 说明 |
|------|------|---------|------|
| **竞赛编程** | 🥇 全球顶尖 | Codeforces **3206分** | 超越Gemini-3.1-Pro和Opus 4.6 |
| **实时代码** | 🥇 全球顶尖 | LiveCodeBench **93.5** | 在所有对比模型中排名第一 |
| **软件工程** | 🥇 顶尖 | SWE Verified **80.6%** | 与Gemini、Opus持平 |
| **Agent编程** | 🥇 开源最强 | GDPval-AA **1554** | 已替代Claude成为DeepSeek内部编码标配 |
| **算法生成** | 🥇 顶尖 | — | 动态规划/图论/贪心/回溯 |
| **底层代码** | 🥇 顶尖 | — | C/C++内存管理、指针操作、系统编程 |
| **数学代码** | 🥇 顶尖 | Apex Shortlist **90.2** | 领先Opus-4.6 Max (85.9) |
| **Java代码** | ⭐⭐⭐⭐ | — | 企业级应用，但Agent不如GLM-5-Code/Claude |
| **前端代码** | ⭐⭐⭐ | — | 可用但非强项 |

### 3.2 代码能力特色

```
DeepSeek-Coder特别擅长的代码类型：

1. 算法竞赛代码：
   ✅ LeetCode/Codeforces/AtCoder题目
   ✅ ACM-ICPC级别算法
   ✅ 多解法对比（暴力→优化→最优）

2. 底层系统代码：
   ✅ 内存池实现
   ✅ 无锁数据结构
   ✅ SIMD向量化优化

3. 数学/科学计算：
   ✅ 矩阵运算优化
   ✅ FFT快速傅里叶变换
   ✅ 蒙特卡洛模拟
   ✅ 微分方程数值解

4. 性能优化代码：
   ✅ Cache-friendly代码
   ✅ 并行化改造（多线程/异步）
   ✅ JVM调优建议
```

### 3.3 V4 Pro推理速度（含局限性）

> ⚠️ **V4 Pro的一个重要trade-off**：Think Max模式推理极深但输出token量大、速度偏慢。

| 指标 | V4-Pro | 竞品中位数 | 说明 |
|------|--------|-----------|------|
| **输出速度** | 34.6 tok/s | 53.1 tok/s | ⚠️ 偏慢，约为竞品65% |
| **Max模式输出量** | ~1.9亿token | ~4500万token | ⚠️ 输出冗长，Token消耗大 |
| **幻觉率（AA-Omniscience）** | 94% | — | ⚠️ 不知道时倾向于猜测而非承认 |

> 💡 **实际影响**：V4 Pro在Think Max模式下"深度思考"带来了更好的答案质量，但代价是输出更长、更慢。对于低成本快速场景，建议用V4-Flash或Non-think模式。

---

## 4. DeepSeek-V4 Pro 深度解析

> 🎯 **V4 Pro于2026年4月24日发布预览版，7月24日上线稳定完整版**，是DeepSeek迄今最强的开源模型。

### 4.1 V4 Pro基本参数

| 参数项 | DeepSeek-V4-Pro | DeepSeek-V4-Flash |
|-------|----------------|-------------------|
| **总参数** | **1.6万亿（1.6T）** | 2840亿（284B） |
| **激活参数** | 490亿（49B） | 130亿（13B） |
| **上下文长度** | **100万token（1M）** | 100万token（1M） |
| **最大输出长度** | 38.4万token | 38.4万token |
| **模态** | 文本（不支持图片输入） | 文本 |
| **开源协议** | MIT | MIT |
| **预训练数据** | ~33万亿token | ~32万亿token |

### 4.2 V4 Pro 基准测试成绩

| 基准测试 | V4-Pro-Max | GPT-5.4 xHigh | Gemini-3.1-Pro High | Opus-4.6 Max |
|---------|-----------|---------------|---------------------|--------------|
| **LiveCodeBench** (Pass@1) | 🥇 **93.5** | — | 91.7 | 88.8 |
| **Codeforces** Rating | 🥇 **3206** | 3168 | 3052 | — |
| **SWE Verified** (Resolved) | **80.6** | — | 80.6 | 80.8 |
| **Apex Shortlist** (Pass@1) | 🥇 **90.2** | — | — | 85.9 |
| **GPQA Diamond** (Pass@1) | 90.1 | 93.0 | 🥇 94.3 | 91.3 |
| **MMLU** (EM) | **90.1** | — | — | — |
| **MMLU-Pro** (EM) | 73.5 | — | — | — |
| **MRCR 1M** (长上下文) | 83.5 | — | 76.3 | 🥇 **92.9** |
| **GDPval-AA** (Agent) | 🥇 **1554** | — | — | — |

### 4.3 V4 Pro的竞争定位

**DeepSeek官方自评**：V4 Pro落后前沿闭源模型（GPT-5.4、Gemini-3.1-Pro）约**3-6个月**。

**NIST/CAISI独立评估**（2026年5月）：V4 Pro落后美国前沿约**8个月**，性能与约8个月前发布的GPT-5相当。CAISI特别指出V4 Pro在DeepSeek自报基准上得分高于CAISI的非公开评估。

**成本效率**：CAISI发现V4 Pro在7个基准中的5个上比GPT-5.4 Mini更具成本效率。

### 4.4 V4 Pro 已知局限性

| 局限 | 说明 |
|------|------|
| **纯文本** | 不支持图片/视频/音频输入 |
| **高幻觉率** | AA-Omniscience基准94%，不确定时仍倾向回答而非承认不知道 |
| **输出冗长** | Max模式评测~1.9亿token输出（竞品中位数~4500万） |
| **速度偏慢** | 34.6 tok/s（竞品中位数53.1 tok/s） |

### 4.5 V4-Flash（轻量版）

| 特性 | 说明 |
|------|------|
| **定位** | 快速+低价的日常开发主力 |
| **能力** | 推理能力接近V4 Pro，简单Agent性能持平 |
| **差距** | 知识评估和复杂高难度Agent任务落后于V4 Pro |
| **速度** | 比V4 Pro快，适合高并发API场景 |
| **价格** | 输入¥1/百万token，输出¥2/百万token |

### 4.6 V3/V4/R1 选择建议

```
日常编码/问答 → V4-Flash（快+便宜=性价比最高）
复杂算法/竞赛 → V4-Pro Think Max（深度推理）
高并发API → V4-Flash Non-think（最低成本）
Agent任务 → V4-Pro Think High（Agent能力开源最强）
简单补全 → V4-Flash（够用+便宜）
学术研究 → V4-Pro Think Max（需要深度推理）
代码审查 → V4-Pro Think High（系统级理解）
低成本场景 → V4-Flash（¥1/百万token输入）
```

---

## 5. 部署方案与实践

### 5.1 部署方式

| 方式 | 适用模型 | 硬件需求 | 难度 | 成本 |
|------|---------|---------|------|------|
| **Ollama本地** | V4-Flash (13B激活) | RTX 4090 24GB | ⭐ 极简 | 仅硬件 |
| **vLLM推理** | V4-Pro (1.6T总参数) | 8×H100/A100 | ⭐⭐⭐ | 硬件+电费 |
| **官方API** | V4-Pro/V4-Flash | 无需 | ⭐ 极简 | 按量极低价 |
| **第三方API** | V4-Pro/V4-Flash | 无需 | ⭐ 极简 | DeepInfra等 |

### 5.2 Ollama本地部署

```bash
# V4-Flash轻量版（13B激活，推荐单卡部署）
ollama run deepseek-v4-flash

# REST API调用
curl http://localhost:11434/api/generate -d '{
  "model": "deepseek-v4-flash",
  "prompt": "用Java实现红黑树",
  "stream": false
}'
```

### 5.3 NVIDIA Blackwell + SGLang 部署 V4 Pro

```bash
# V4 Pro全量部署需要8×H100/H200/B200
# 推荐使用SGLang推理框架（支持V4的Hybrid Attention）

pip install sglang[all]

python -m sglang.launch_server \
  --model deepseek-ai/DeepSeek-V4-Pro \
  --tp 8 \
  --trust-remote-code \
  --context-length 131072
```

---

## 6. API与定价

### 6.1 官方API定价（2026年7月V4最新）

| 模型 | 输入（每1M token） | 输出（每1M token） | 缓存命中输入 |
|------|-------------------|-------------------|-------------|
| **DeepSeek-V4-Pro** | ¥12（~$1.74） | ¥24（~$3.48） | ¥1（~$0.145） |
| **DeepSeek-V4-Flash** | ¥1（~$0.14） | ¥2（~$0.28） | ¥0.2 |

> ⚠️ **2026年7月24日起**：旧版 `deepseek-chat` 和 `deepseek-reasoner` API标识已下线，必须使用V4专用模型ID。

### 6.2 V4 Pro 价格竞争力

| 对比 | V4-Pro vs Claude Opus 4.7 | V4-Pro vs GPT-5.4 |
|------|--------------------------|-------------------|
| **输出价格** | ~1/7 | 有竞争力 |
| **性价比** | 🥇 顶级性能+开源价格的组合 | — |

> 🎯 **成本对比（生成一篇3000字文章，约2000输出token）**：

| 模型 | 约花费 |
|------|--------|
| **DeepSeek-V4-Flash** | ~$0.0006 💰 |
| DeepSeek-V4-Pro | ~$0.007 |
| Qwen3-Turbo | ~$0.003 |
| GPT-5.6 Mini | ~$0.004 |
| Gemini 2.5 Flash | ~$0.005 |
| GPT-5.6 | ~$0.03 |
| Claude Sonnet 5 | ~$0.05 |
| Claude Opus 4.7 | ~$0.15 |

> 💡 **V4-Flash的¥1/¥2定价依然是业界最低价格区间，适合大规模API调用。**

---

## 7. 适用场景与典型案例

### 7.1 场景评级

| 场景 | 适合度 | 说明 |
|------|--------|------|
| **竞赛编程** | 🥇 全球第一 | Codeforces 3206分，LiveCodeBench 93.5 |
| **Agent开发** | 🥇 开源最强 | 已替代Claude成为DeepSeek内部编码标配 |
| **算法竞赛** | 🥇 最佳 | 竞赛算法开源最强 |
| **底层库开发** | 🥇 最佳 | C/C++底层代码、数据结构 |
| **数学建模** | 🥇 最佳 | 数值计算、优化算法 |
| **自研AI平台** | 🥇 最佳 | MIT协议完全自由商用 |
| **百万token长上下文** | 🥇 1M token | 代码库/长文档全局分析 |
| **高并发API** | 🥇 最佳 | V4-Flash极低价，适合大规模调用 |
| **企业应用开发** | ⭐⭐⭐⭐ | Java/SpringBoot可用 |
| **前端开发** | ⭐⭐⭐ | 非强项 |
| **多模态** | ❌ 不支持 | 纯文本模型，不能处理图片/视频/音频 |

### 7.2 典型案例

```
🏆 算法竞赛场景：
   需求：解决一道Codeforces 2400分的图论题
   DeepSeek做法：分析题目→设计算法→给出多种解法→
   复杂度分析（O(n log n)）→完整C++/Python代码→
   注释清晰的解题思路

🔧 底层库开发场景：
   需求：实现一个高性能的内存池（C++）
   DeepSeek做法：
   → 多种分配策略（first-fit/best-fit/slab）
   → 线程安全方案（lock-free / TLS）
   → Cache-line对齐优化
   → 完整的单元测试+benchmark

🏢 自研AI平台场景：
   需求：创业公司搭建代码审查AI平台
   方案：DeepSeek-V3 API（极低成本）
   → 客户代码→API→AI审查→返回结果
   → 每月100万次API调用，成本仅~¥1000
```

---

## 8. 优势与短板深度分析

### 8.1 核心优势

| 优势 | 说明 |
|------|------|
| **1. 编程基准登顶** | LiveCodeBench 93.5、Codeforces 3206、Apex 90.2三项全球第一/领先 |
| **2. Agent开源最强** | GDPval-AA 1554分，已替代Claude成为DeepSeek内部标配 |
| **3. MIT开源最自由** | 1.6T参数模型完全自由商用，无任何限制条款 |
| **4. 100万token上下文** | MoE架构+Hybrid Attention，长上下文效率极高 |
| **5. 极致性价比** | V4-Pro输出$3.48/1M（Claude Opus的1/7），V4-Flash仅$0.28 |
| **6. 三档推理模式** | Non-think/Think High/Think Max灵活匹配场景和预算 |
| **7. 国产自主可控** | 国内开发，直连使用，无合规风险 |
| **8. 训练效率基因** | V3仅$5.6M开创高效率训练先河，V4延续极致效率 |

### 8.2 核心短板（V4 Pro已知局限）

| 短板 | 说明 |
|------|------|
| **纯文本模型** | ❌ 不支持图片/视频/音频输入，多模态能力缺失 |
| **高幻觉率** | ⚠️ AA-Omniscience 94%，不确定时倾向于猜而非承认不知道 |
| **输出冗长** | Think Max模式输出~1.9亿token（竞品中位数~4500万），token消耗大 |
| **推理速度偏慢** | 34.6 tok/s（竞品中位数53.1 tok/s），约为竞品65% |
| **落后美国前沿~8个月** | NIST独立评估V4 Pro与约8个月前的GPT-5相当 |
| **Java企业级一般** | 企业级Java/SpringBoot深度不如GLM-5-Code/Claude |
| **前端非强项** | 前端代码质量一般 |
| **生态工具较少** | 周边工具链不如Llama丰富 |

---

## 9. 与其他代码模型对比

| 维度 | DeepSeek-V4-Pro | GLM-5-Code | Claude Sonnet 5 | Qwen3-Max | GPT-5.6 |
|------|----------------|------------|-----------------|-----------|---------|
| **开源协议** | 🥇 MIT | 有限制 | ❌ 闭源 | 🥇 Apache 2.0 | ❌ 闭源 |
| **竞赛编程** | 🥇 CF 3206 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **LiveCodeBench** | 🥇 93.5 | — | — | — | — |
| **SWE-bench** | 80.6 | 🥇 ~48% | 🥇 ~53% | ~30% | ~45% |
| **Agent能力** | 🥇 开源最强 | ⭐⭐⭐⭐ | 🥇 闭源最强 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **上下文长度** | 🥇 1M token | 256K | 🥇 1M token | 🥇 1M token | 128K |
| **企业Java** | ⭐⭐⭐⭐ | 🥇 最强 | 🥇 最强 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **API价格（输出）** | $3.48/1M | 中等 | $15/1M | 中等 | $10/1M |
| **推理速度** | ⚠️ 34.6 tok/s | 较快 | 中等 | 较快 | 较快 |
| **多模态** | ❌ 不支持 | 支持 | 仅图片 | 支持 | 🥇 全模态 |
| **C端产品** | ⭐⭐ 基础 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 🥇 最完善 |

---

## 10. Java开发者集成指南

### 10.1 DeepSeek V4 API（兼容OpenAI格式）

```java
// ⚠️ 2026年7月起旧模型ID已下线，必须使用V4专用ID
// 依赖：com.openai:openai-java

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.*;

public class DeepSeekV4Demo {
    public static void main(String[] args) {
        OpenAIClient client = OpenAIClient.builder()
            .apiKey(System.getenv("DEEPSEEK_API_KEY"))
            .baseUrl("https://api.deepseek.com/v1")
            .build();

        // 使用V4-Flash（日常开发，快+便宜）
        ChatCompletion completion = client.chat().completions().create(
            ChatCompletionCreateParams.builder()
                .model("deepseek-v4-flash")  // V4 Flash
                .addUserMessage("用Java实现一个线程安全的LRU缓存")
                .temperature(0.3)
                .maxTokens(2048)
                .build()
        );

        System.out.println(
            completion.choices().get(0).message().content());
    }
}
```

### 10.2 V4-Pro Think Max 深度推理

```java
// V4-Pro Think Max：最强推理模式（适合竞赛算法/数学难题）
ChatCompletion completion = client.chat().completions().create(
    ChatCompletionCreateParams.builder()
        .model("deepseek-v4-pro")  // V4 Pro
        .addUserMessage("""
            解决以下算法问题：
            给定n个点的坐标，找出能覆盖所有点的最小圆。
            请给出O(n)时间复杂度的Welzl算法实现。
            """)
        // V4 Pro会自动根据问题难度选择推理深度
        // 也可以通过 extra_body 参数指定 thinking 模式
        .build()
);
```

### 10.3 Spring AI 集成 DeepSeek V4

```java
// application.yml
// spring.ai.openai.base-url=https://api.deepseek.com/v1
// spring.ai.openai.api-key=${DEEPSEEK_API_KEY}
// spring.ai.openai.chat.options.model=deepseek-v4-flash

@RestController
public class DeepSeekV4Controller {

    private final ChatClient chatClient;

    public DeepSeekV4Controller(ChatClient.Builder builder) {
        this.chatClient = builder
            .defaultSystem("你是一个精通算法和数据结构的程序员")
            .build();
    }

    @PostMapping("/deepseek/algorithm")
    public String algorithm(@RequestBody String problem) {
        return chatClient.prompt()
            .user("解决以下算法问题，给出最优解和复杂度分析：\n" + problem)
            .call()
            .content();
    }
}
```

### 10.4 本地Ollama调用 V4 Flash

```java
// 本地部署DeepSeek-V4-Flash（13B激活，单卡RTX 4090可跑）
HttpClient client = HttpClient.newHttpClient();
String json = """
    {
      "model": "deepseek-v4-flash",
      "prompt": "用Java实现一个布隆过滤器",
      "stream": false
    }
    """;

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:11434/api/generate"))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(json))
    .build();
```

---

## 核心要点回顾

- **最新版本**：DeepSeek-V4-Pro（2026年4月预览，7月稳定），1.6T参数 MoE/49B激活/1M上下文/MIT开源
- **编程基准登顶**：LiveCodeBench 93.5（第一）、Codeforces 3206（第一）、Apex 90.2（领先Opus）
- **Agent开源最强**：GDPval-AA 1554，已替代Claude成为DeepSeek内部编码标配
- **三档推理模式**：Non-think（快速）/ Think High（日常）/ Think Max（深度），灵活匹配场景
- **V4-Flash轻量版**：284B/13B激活，推理接近V4 Pro，价格仅¥1/¥2每百万token
- **NIST独立评估**：落后美国前沿约8个月，与GPT-5（约8个月前发布）相当
- **已知局限**：纯文本（无多模态）、高幻觉率（94%）、Think Max输出冗长、推理速度偏慢
- **最佳场景**：竞赛编程、Agent开发、算法竞赛、底层库开发、百万token长上下文分析
- **Java集成**：API兼容OpenAI格式，V4专用模型ID（`deepseek-v4-pro`/`deepseek-v4-flash`）

---

## 参考资料

1. DeepSeek 官网 - https://www.deepseek.com
2. DeepSeek API 文档 - https://api-docs.deepseek.com
3. DeepSeek V4 技术报告 - https://arxiv.org/abs/2606.19348
4. DeepSeek V4 Pro HuggingFace - https://huggingface.co/deepseek-ai/DeepSeek-V4-Pro
5. DeepInfra V4 Pro Overview - https://deepinfra.com/blog/deepseek-v4-pro-model-overview
6. NIST CAISI V4 Pro评估 - https://www.nist.gov/news-events/news/2026/05/caisi-evaluation-deepseek-v4-pro
7. DeepSeek GitHub - https://github.com/deepseek-ai
