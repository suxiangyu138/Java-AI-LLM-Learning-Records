# Google Gemini 详细知识点（2026最新）

> **定位**：原生多模态最强（视频长片段理解、实时视频问答领先），深度打通谷歌生态（搜索/Docs/Colab/Android），Gemini 3.1 Pro推理大幅增强（ARC-AGI-2达77.1%），100万token上下文，中等思考模式新增精细控制。

---

## 目录

1. [产品概述与发展历程](#1-产品概述与发展历程)
2. [模型体系详解](#2-模型体系详解)
3. [多模态能力深度解析](#3-多模态能力深度解析)
4. [谷歌生态集成](#4-谷歌生态集成)
5. [Gemini 3.1 Pro 深度解析](#5-gemini-31-pro-深度解析)
6. [开发者工具与API](#6-开发者工具与api)
7. [付费体系与定价](#7-付费体系与定价)
8. [适用场景与典型案例](#8-适用场景与典型案例)
9. [优势与短板深度分析](#9-优势与短板深度分析)
10. [与其他产品对比](#10-与其他产品对比)
11. [Java开发者集成指南](#11-java开发者集成指南)

---

## 1. 产品概述与发展历程

### 1.1 基本信息

| 维度 | 详情 |
|------|------|
| **开发商** | Google DeepMind（美国） |
| **首发时间** | 2023年12月（Gemini 1.0）；2024年2月（Gemini 1.5）；2026年2月（Gemini 3.1 Pro） |
| **当前版本** | **Gemini 3.1 Pro** / 3 Pro / 2.5 Flash系列（2026年7月） |
| **产品形态** | Web端（gemini.google.com）、移动端（Android原生集成/iOS App）、API |
| **核心卖点** | 原生多模态 + 谷歌生态 + 3.1 Pro推理大增强（ARC-AGI-2 77.1%） |
| **总部** | 美国山景城 |

### 1.2 发展里程碑

```
2023.12  Gemini 1.0 发布（Ultra/Pro/Nano三档），首次多模态原生设计
2024.02  Gemini 1.5 Pro，100万token上下文，业界首个百万级
2024.05  Gemini 1.5 Flash 发布，轻量快速，低价高效
2024.08  Gemini 1.5 Pro 002，性能优化，Gemini Live语音对话
2024.12  Gemini 2.0 Flash 发布，Agent能力增强，多模态实时API
2025.03  Gemini 2.5 Pro 发布，200万token上下文，"思考模式"推理增强
2025.10  Gemini 2.5 Pro 全面升级，视频理解+数理推理达到新高度
2026.03  Gemini Deep Research + AI Mode 上线谷歌搜索
2026.06  Gemini 全面整合 Google Workspace，企业级能力加强
```

> 💡 **关键洞察**：Gemini的核心战略是"原生多模态+谷歌生态捆绑"。Gemini不是ChatGPT的竞争者，而是在**视频/音频/数学/搜索整合**这些差异化赛道上建立护城河。

---

## 2. 模型体系详解

### 2.1 当前模型矩阵（2026年7月）

| 模型 | 定位 | 上下文 | 核心优势 | 适用场景 |
|------|------|--------|---------|---------|
| **Gemini 3.1 Pro** | 旗舰推理 | 100万token | 🥇 推理大增强（ARC-AGI-2 77.1%） | 科研、复杂分析、Agent |
| **Gemini 3 Pro** | 主力全能 | 100万token | 多模态+推理平衡 | 日常重度使用 |
| **Gemini 2.5 Flash** | 主力快速 | 100万token | 速度+性价比最佳 | 日常开发、批量处理 |
| **Gemini 2.5 Flash-Lite** | 超轻量 | 100万token | 极低延迟+最低成本 | 分类、提取、高频API |
| **Gemini Nano** | 端侧模型 | 本地 | 手机/PC本地运行 | 离线使用、隐私保护 |

### 2.2 模型能力对比（2026年7月）

| 能力维度 | 3.1 Pro | 3 Pro | 2.5 Flash | 2.5 Flash-Lite |
|---------|---------|-------|-----------|----------------|
| **通用推理** | 🥇 顶尖（ARC-AGI-2 77.1%） | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **数学推理** | 🥇 最强 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **代码生成** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **视频理解** | 🥇 最强 | 🥇 最强 | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **音频分析** | 🥇 最强 | 🥇 最强 | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **上下文窗口** | 1M token | 1M token | 1M token | 1M token |
| **输出长度** | 64K token | 64K token | 32K token | 32K token |
| **思考模式** | 低/中/高 三档 | 低/高 | 低/高 | 不支持 |
| **价格（每1M token ≤200K）** | $2/$12 | 待确认 | $0.15/$0.60 | $0.075/$0.30 |

---

## 3. 多模态能力深度解析

### 3.1 多模态能力全景

> 🎯 **Gemini的核心差异化优势**：原生多模态设计，从第一天就以处理多种数据类型为目标，而非后期拼接。

| 模态 | 输入 | 输出 | 能力评级 | 杀手级场景 |
|------|------|------|---------|-----------|
| **文本** | ✅ | ✅ | ⭐⭐⭐⭐ | 通用对话、写作、翻译 |
| **图片理解** | ✅ | ❌ | ⭐⭐⭐⭐⭐ | 图表分析、数学公式、医学影像 |
| **图片生成** | ✅（文字描述） | ✅ | ⭐⭐⭐⭐ | Imagen 3 集成，文生图 |
| **音频输入** | ✅ | ❌ | 🥇 业界最强 | 会议音频→纪要、音乐分析、声纹识别 |
| **音频输出** | ❌ | ❌ | ❌ | 无TTS输出（需配合Google TTS） |
| **视频输入** | ✅ | ❌ | 🥇 业界最强 | 长视频→摘要、监控分析、体育解说 |
| **视频生成** | ✅（文字描述） | ✅ | ⭐⭐⭐⭐ | Veo 2 集成，文生视频 |
| **代码执行** | ✅ | ✅ | ⭐⭐⭐⭐ | 代码生成+解释+运行 |
| **PDF/文档** | ✅ | ❌ | ⭐⭐⭐⭐⭐ | 长文档分析、表格提取 |

### 3.2 视频理解能力详解

> 🎯 **Gemini最独特的杀招**：ChatGPT和Claude都无法原生处理长视频，Gemini可以。

| 能力 | 说明 | 典型应用 |
|------|------|---------|
| **长视频分析** | 输入长达1小时的视频，理解完整叙事线 | 电影分析、教学录播总结 |
| **实时视频问答** | 摄像头实时画面，边看边回答 | 维修指导、医疗远程诊断 |
| **关键帧提取** | 自动识别视频中的关键时刻 | 体育赛事精彩片段定位 |
| **多镜头理解** | 剪辑切换、多角度拍摄的画面统一理解 | 影视制作分析 |
| **动作识别** | 识别视频中的具体行为和动作序列 | 体育动作分析、安防监控 |

```
📹 视频分析实际工作流程：
1. 上传视频 → Gemini自动按时间轴分帧
2. 每帧提取视觉信息 + 音频转文字
3. 跨时间轴串联理解 → 生成带时间戳的详细摘要
4. 可针对特定时间点提问："第3分20秒那个人说了什么？"
```

### 3.3 音频分析能力

| 能力 | 说明 |
|------|------|
| **会议音频→纪要** | 多人对话自动识别→区分说话人→生成会议纪要+待办事项 |
| **音乐分析** | 曲风识别、乐器分离、BPM检测、歌词转录 |
| **多语种识别** | 100+种语言语音识别，方言识别效果好 |
| **声纹识别** | 区分不同说话人（Diarization） |
| **情感识别** | 从语调判断说话人情绪状态 |

### 3.4 图片生成（Imagen 3）

| 特性 | 说明 |
|------|------|
| **模型** | Imagen 3（集成于Gemini） |
| **文本渲染** | 在图中准确渲染文字（英文），业界最强 |
| ** photorealism** | 照片级真实感，光影/材质细腻 |
| **风格** | 水彩/油画/插画/3D渲染/像素等多种风格 |
| **安全** | SynthID数字水印，生成图片可追溯 |

### 3.5 视频生成（Veo 2）

| 特性 | 说明 |
|------|------|
| **视频长度** | 最长2分钟+ |
| **分辨率** | 最高4K |
| **物理准确性** | 运动轨迹、光影变化高度真实 |
| **风格** | 写实/动画/延时/航拍等多种风格 |
| **可用性** | Gemini Advanced订阅用户，有限制 |

---

## 4. 谷歌生态集成

### 4.1 生态全景图

```
                    ┌──────────────────────────┐
                    │      Google Gemini        │
                    │    （AI核心引擎）          │
                    └──────────┬───────────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
   ┌────▼────┐          ┌──────▼──────┐        ┌──────▼──────┐
   │ 搜索生态 │          │  办公生态    │        │  开发生态    │
   └────┬────┘          └──────┬──────┘        └──────┬──────┘
        │                      │                      │
  Google搜索              Google Docs            Google Colab
  AI Overviews            Google Sheets           Android Studio
  AI Mode                 Google Slides           Firebase
  Google Lens             Gmail                    Project IDX
                          Google Calendar          Gemini API
                          Google Meet             Vertex AI
```

### 4.2 各生态集成详解

#### 搜索生态

| 功能 | 说明 |
|------|------|
| **AI Overviews** | 谷歌搜索结果顶部AI摘要，日处理数十亿查询 |
| **AI Mode** | 搜索结果页新增AI深度对话模式，多轮追问 |
| **Google Lens** | 拍照搜索，Gemini视觉能力直接融入 |

#### 办公生态（Google Workspace）

| 产品 | Gemini集成功能 |
|------|---------------|
| **Gmail** | 邮件起草、摘要、智能回复、邮件分类 |
| **Google Docs** | 文档撰写、改写、扩写、翻译、总结 |
| **Google Sheets** | 公式生成、数据洞察、图表建议、自动分类 |
| **Google Slides** | 幻灯片生成、图片生成、演讲大纲 |
| **Google Meet** | 实时字幕、会议纪要自动生成、背景替换 |
| **Google Calendar** | 智能日程安排、会议时间建议 |

#### 开发生态

| 产品 | Gemini集成功能 |
|------|---------------|
| **Google Colab** | 代码补全、错误解释、数据分析辅助 |
| **Android Studio** | 代码生成、Bug修复、UI设计建议 |
| **Firebase** | AI辅助配置、安全规则生成 |
| **Project IDX** | 云端IDE，Gemini全流程辅助 |
| **Vertex AI** | 企业级Gemini API，模型微调/部署/监控 |

---

## 5. 数学与理科推理能力

### 5.1 数学推理能力评级

| 数学领域 | 能力评级 | 说明 |
|---------|---------|------|
| **代数** | ⭐⭐⭐⭐⭐ | 方程求解、不等式、多项式全部优秀 |
| **微积分** | ⭐⭐⭐⭐⭐ | 极限/导数/积分/微分方程求解准确 |
| **线性代数** | ⭐⭐⭐⭐⭐ | 矩阵运算、特征值、SVD分解 |
| **概率统计** | ⭐⭐⭐⭐⭐ | 分布计算、假设检验、贝叶斯推断 |
| **几何** | ⭐⭐⭐⭐ | 平面/立体几何证明 |
| **数论** | ⭐⭐⭐⭐ | 质数/同余/数论定理 |
| **竞赛数学** | 🥇 顶尖 | IMO/AMC级别题目求解 |

### 5.2 理科推理能力

| 学科 | 能力评级 | 说明 |
|------|---------|------|
| **物理** | 🥇 顶尖 | 力学/电磁学/热力学/量子力学 |
| **化学** | ⭐⭐⭐⭐⭐ | 方程式配平/结构分析/反应预测 |
| **生物** | ⭐⭐⭐⭐ | 分子生物学/遗传学/生态学 |
| **计算机科学** | ⭐⭐⭐⭐ | 算法分析/复杂度/自动机 |

### 5.3 "思考模式"（Thinking Mode）

> 🎯 **Gemini 2.5系列的杀手级推理功能**：模型在回答前进行内部多步推理，可以看到思考过程。

```
普通模式：  用户提问 → 直接给出答案
思考模式：  用户提问 → 内部推理（可见过程）
            ├── 步骤1：理解问题并拆解
            ├── 步骤2：列出已知条件和未知量
            ├── 步骤3：尝试方法A（被否决，原因：...）
            ├── 步骤4：尝试方法B（正确，继续）
            └── 步骤5：验证答案 → 给出最终答案
```

---

## 6. 开发者工具与API

### 6.1 Gemini API 核心能力

| API类型 | 说明 |
|---------|------|
| **Gemini API** | 标准文本/多模态对话接口 |
| **Vertex AI Gemini** | 企业级Gemini API，含监控/日志/权限管理 |
| **Gemini Live API** | 实时音视频流处理（WebSocket） |
| **Embeddings API** | 文本/多模态向量化（`text-embedding-004`等） |
| **Imagen API** | 文生图专用API |
| **Veo API** | 文生视频专用API（受限访问） |

### 6.2 关键API参数

| 参数 | 说明 | 典型值 |
|------|------|--------|
| `temperature` | 控制随机性 | 0.0（确定）~ 1.0（创意） |
| `topP` | 核采样阈值 | 0.95 |
| `maxOutputTokens` | 最大输出长度 | 2048 ~ 8192 |
| `stopSequences` | 停止序列 | `["\n\n\n"]` |
| `safetySettings` | 安全过滤级别 | 可关闭/可调整 |
| `thinkingConfig` | 思考模式配置 | `thinkingBudget: 1024` |

### 6.3 多模态API输入格式

```java
// Gemini API 支持在一次请求中混合多种输入类型
// 文本 + 图片 + 视频 + 音频 + PDF 同时输入

// 伪代码示例：
Content content = Content.builder()
    .addText("请分析这个视频的关键帧和其中的对话")
    .addVideo(ByteString.fromBytes(videoData), "video/mp4")
    .build();
```

---

## 7. 付费体系与定价

### 7.1 消费级订阅

| 方案 | 月费 | 核心权益 |
|------|------|---------|
| **Free** | $0 | Gemini 2.5 Flash-Lite，基础功能 |
| **Gemini Advanced** | $19.99 | Gemini 2.5 Pro/Flash全部，Google One Premium（2TB存储），Gemini in Workspace |
| **Google One AI Premium** | $19.99 | 同Gemini Advanced + Google One全家桶 |

> 💡 **性价比分析**：$19.99不仅获得Gemini全部模型，还有2TB云存储+Workspace集成，对谷歌生态用户极具吸引力。

### 7.2 API定价（2026年7月）

| 模型 | 输入价格（每1M token） | 输出价格（每1M token） | 缓存输入 |
|------|----------------------|----------------------|---------|
| Gemini 2.5 Pro | $2.50 | $10.00 | $0.625 |
| Gemini 2.5 Flash | $0.15 | $0.60 | $0.0375 |
| Gemini 2.5 Flash-Lite | $0.075 | $0.30 | 待定 |
| Imagen 3（生图） | 按图片计费 | $0.02-0.04/张 | N/A |

> 💡 **成本优势**：Flash系列价格极具竞争力，$0.15/$0.60远低于GPT-5.6和Claude Sonnet，适合大规模API调用。

### 7.3 免费额度

- **Gemini API免费层**：每秒1次请求，每天1500次请求（Flash-Lite模型）
- **Google AI Studio**：Web端免费测试所有模型（有限制）
- **Gemini Code Assist**：个人开发者免费（代码补全）

---

## 8. 适用场景与典型案例

### 8.1 场景评级总览

| 场景 | 适合度 | 说明 |
|------|--------|------|
| **视频分析** | 🥇 最佳 | 业界唯一原生支持长视频深度分析的通用AI |
| **数理化解题** | 🥇 最佳 | 数学/物理/化学推理全球最强梯队 |
| **实时资讯** | 🥇 最佳 | 谷歌搜索原生集成，时效性无可匹敌 |
| **谷歌办公** | 🥇 最佳 | Docs/Sheets/Gmail深度集成，办公场景最流畅 |
| **音频分析** | 🥇 最佳 | 会议音频→纪要、多语种识别 |
| **代码生成** | ⭐⭐⭐⭐ 优秀 | 日常编程优秀，但工程能力不如Claude |
| **长文档分析** | ⭐⭐⭐⭐ 优秀 | 200万token超大窗口，但实用体验不如Claude |
| **创意写作** | ⭐⭐⭐ 良好 | 偏学术/技术风格，创意文采不如ChatGPT |
| **中文能力** | ⭐⭐⭐ 良好 | 可用但不如国产模型自然 |
| **国内直连** | ❌ 不可用 | 需科学上网 |

### 8.2 典型案例

```
📹 视频分析场景：
   需求：分析一场1小时的足球比赛录像，找所有进球的关键时刻
   Gemini做法：上传完整比赛视频→自动分析→
   定位每个进球时刻（附时间戳）→生成每个进球的战术分析→
   输出含GIF截图的比赛报告

🧮 数学解题场景：
   需求：求解一道IMO级别的几何证明题
   Gemini做法：思考模式开启→逐步分解条件→尝试多种辅助线→
   找到正确证法→输出完整证明过程+辅助线示意图

🔍 实时资讯场景：
   需求："2026年世界杯最新战况？"
   Gemini做法：实时调用谷歌搜索→整合数十条最新新闻→
   输出结构化摘要+比分表+后续赛程+来源链接
```

---

## 9. 优势与短板深度分析

### 9.1 核心优势（护城河）

| 优势 | 详细说明 |
|------|---------|
| **1. 原生多模态最强** | 视频/音频/图片/文本全部原生支持，非后天拼接，质量最高 |
| **2. 谷歌生态无缝集成** | 搜索+办公+开发+Android，2B+用户基础无可匹敌 |
| **3. 数学理科最强** | 物理/数学/化学推理全球顶尖，思考模式透明可查 |
| **4. 上下文窗口最大** | 200万token，虽然不如Claude实用但数字上领先 |
| **5. 性价比最高** | Flash系列$0.15/$0.60，大规模使用成本远低于竞品 |
| **6. 端侧部署** | Gemini Nano手机/PC本地运行，离线可用 |

### 9.2 核心短板

| 短板 | 严重程度 | 详细说明 |
|------|---------|---------|
| **对话自然度** | ⭐⭐⭐ | 对话流畅度不如ChatGPT，偶有机械感和答非所问 |
| **代码工程能力** | ⭐⭐⭐ | 日常代码可以，复杂工程/多文件重构不如Claude |
| **创意写作** | ⭐⭐⭐ | 创意文采不如ChatGPT灵动，偏学术/技术 |
| **中文自然度** | ⭐⭐⭐ | 中文可用但不如国产模型地道 |
| **国内访问** | ⭐⭐⭐⭐⭐ | 需科学上网，Google全家桶均不可用 |
| **隐私顾虑** | ⭐⭐⭐⭐ | 谷歌商业模式以数据/广告为核心，企业数据隐私存疑 |
| **产品策略多变** | ⭐⭐⭐⭐ | 谷歌历史上多次停掉AI产品，企业采用信心不足 |

### 9.3 使用注意事项

> ⚠️ **关键提醒**：
> 1. **视频处理有限制**：免费版视频时长有限制，长视频需付费
> 2. **中文不是强项**：中文理解不如国产模型，专业中文文档处理效果打折
> 3. **谷歌生态依赖**：脱离开谷歌生态后，独立体验不如ChatGPT/Claude
> 4. **API版本迭代快**：模型名称和定价变化频繁，需持续关注更新

---

## 10. 与其他产品对比

### 10.1 Gemini vs ChatGPT

| 维度 | Gemini | ChatGPT | 胜出 |
|------|--------|---------|------|
| 视频理解 | 🥇 原生最强 | 支持但不原生 | Gemini |
| 音频分析 | 🥇 原生最强 | 支持但不原生 | Gemini |
| 数学理科 | 🥇 思考模式透明 | o3推理也很强 | 持平 |
| 实时资讯 | 🥇 谷歌搜索原生 | 需手动联网 | Gemini |
| 对话自然度 | 偏机械 | 🥇 最流畅 | ChatGPT |
| 创意写作 | 偏学术 | 🥇 灵动有文采 | ChatGPT |
| 生态 | 谷歌全家桶 | 🥇 GPTs+Operator | 各有优势 |
| 性价比API | 🥇 Flash极便宜 | 较贵 | Gemini |

### 10.2 Gemini vs Claude

| 维度 | Gemini | Claude | 胜出 |
|------|--------|--------|------|
| 多模态 | 🥇 视频+音频全支持 | 仅图片 | Gemini |
| 代码工程 | 良好 | 🥇 顶尖 | Claude |
| 文档严谨性 | 较严谨 | 🥇 最严谨 | Claude |
| 数学推理 | 🥇 最强 | 很强 | Gemini |
| 上下文 | 200万token | 🥇 100万token更实用 | Claude（体验更好） |
| 生态 | 谷歌全家桶 | MCP协议 | 各有优势 |

---

## 11. Java开发者集成指南

### 11.1 使用 Google Gemini Java SDK

```java
// 依赖：com.google.genai:google-genai:1.0.0+

import com.google.genai.Client;
import com.google.genai.types.*;

public class GeminiDemo {
    public static void main(String[] args) {
        Client client = Client.builder()
            .apiKey(System.getenv("GEMINI_API_KEY"))
            .build();

        // 基础文本对话
        GenerateContentResponse response = client.models.generateContent(
            "gemini-2.5-flash",
            Content.fromUser("用Java实现一个LRU缓存（LinkedHashMap方式）"),
            GenerateContentConfig.builder()
                .temperature(0.7f)
                .maxOutputTokens(2048)
                .build()
        );

        System.out.println(response.text());
    }
}
```

### 11.2 多模态输入（图片分析）

```java
// 上传图片让Gemini分析架构图
import com.google.genai.types.Part;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImageAnalysisDemo {
    public static void main(String[] args) throws Exception {
        Client client = Client.builder()
            .apiKey(System.getenv("GEMINI_API_KEY"))
            .build();

        byte[] imageBytes = Files.readAllBytes(
            Path.of("architecture-diagram.png"));

        Content content = Content.builder()
            .parts(List.of(
                Part.fromText("分析这个系统架构图的优缺点"),
                Part.fromBytes(imageBytes, "image/png")
            ))
            .build();

        GenerateContentResponse response = client.models.generateContent(
            "gemini-2.5-pro",
            content
        );

        System.out.println(response.text());
    }
}
```

### 11.3 Spring AI 集成 Gemini

```java
// application.yml
// spring.ai.vertex.ai.gemini.project-id=${PROJECT_ID}
// spring.ai.vertex.ai.gemini.location=us-central1

@RestController
public class GeminiController {
    
    private final ChatClient chatClient;
    
    public GeminiController(ChatClient.Builder builder) {
        this.chatClient = builder
            .defaultSystem("你是一个数学辅导老师")
            .build();
    }
    
    @GetMapping("/solve")
    public String solve(@RequestParam String problem) {
        return chatClient.prompt()
            .user("请解答这道数学题，并给出详细步骤：" + problem)
            .call()
            .content();
    }
}
```

### 11.4 流式输出

```java
// Gemini流式输出：逐token返回
client.models.generateContentStream(
    "gemini-2.5-flash",
    Content.fromUser("解释CAP定理"),
    GenerateContentConfig.builder().build()
).forEach(chunk -> {
    System.out.print(chunk.text());
});
```

---

## 核心要点回顾

- **最新版本**：**Gemini 3.1 Pro**（2026年2月发布），推理大幅增强（ARC-AGI-2 77.1%）
- **核心壁垒**：原生多模态（视频+音频+图片）+ 谷歌生态 + 3.1 Pro新增中等思考模式
- **3.1 Pro 亮点**：推理能力翻倍（vs 3 Pro），支持1M token输入/64K输出，Agent能力增强
- **定价**：Gemini Advanced $19.99/月（含2TB存储+Workspace）；API ≤200K context: $2/$12 per 1M token
- **最佳场景**：视频分析、数理化解题、谷歌办公套件、Agent开发
- **最大短板**：对话自然度不如ChatGPT、代码工程不如Claude/DeepSeek V4、国内不可用

---

## 参考资料

1. Google Gemini 官方 - https://deepmind.google/technologies/gemini/
2. Gemini API 文档 - https://ai.google.dev/gemini-api/docs
3. Vertex AI Gemini - https://cloud.google.com/vertex-ai/generative-ai
4. Google GenAI Java SDK - https://github.com/googleapis/java-genai
5. Gemini Cookbook - https://github.com/google-gemini/cookbook
6. Google AI Studio - https://aistudio.google.com
