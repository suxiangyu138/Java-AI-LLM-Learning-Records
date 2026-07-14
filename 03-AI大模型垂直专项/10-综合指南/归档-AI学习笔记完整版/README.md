# 🚀 AI大模型应用开发 — 从零到企业级实战

> **作者：** 苏巷雨  
> **学习路线：** Python基础 → 机器学习 → NLP → Transformer → 大模型API → Agent → RAG → 全栈 → 部署  
> **目标：** 掌握AI大模型应用全栈开发能力，具备独立交付企业级AI项目的能力  

---

## 📋 目录总览

| 阶段 | 主题 | 核心内容 | 实战项目 |
|:---:|------|----------|----------|
| 01 | [Python编程与核心库](./docs/01-Python编程与核心库/README.md) | Python语法 · NumPy · Matplotlib · Pandas | 数据分析脚本 |
| 02 | [机器学习基础](./docs/02-机器学习基础/README.md) | 线性回归 · 决策树 · 模型训练概念 | 房价预测模型 |
| 03 | [NLP基础](./docs/03-NLP基础/README.md) | 文本清洗 · Token化 · 词向量 | 文本分类器 |
| 04 | [Transformer架构与Attention](./docs/04-Transformer架构与Attention/README.md) | Attention机制 · BERT · GPT原理 | 实现简易Transformer |
| 05 | [HuggingFace与开源模型库](./docs/05-HuggingFace与开源模型库/README.md) | Model Hub · Datasets · Pipelines | 模型选型工具 |
| 06 | [大模型API应用基础](./docs/06-大模型API应用基础/README.md) | Token定价 · Prompt基础 · API Key管理 | API调用封装库 |
| 07 | [Prompt Engineering](./docs/07-PromptEngineering/README.md) | Zero-shot · Few-shot · CoT · 高级技巧 | Prompt模板管理器 |
| 08 | [AI Agent概念](./docs/08-AI-Agent概念/README.md) | 感知-规划-执行循环 · 工具调用 | 简单Agent框架 |
| 09 | [RAG基础](./docs/09-RAG基础/README.md) | RAG概念 · 搜索增强 · 数据接入 | 文档问答系统 |
| 10 | [向量数据库与数据加载](./docs/10-向量数据库与数据加载/README.md) | Milvus · Chroma · 数据管理 | 知识库搭建 |
| 11 | [AI全栈开发基础](./docs/11-AI全栈开发基础/README.md) | FastAPI · 前端基础 · UI设计 | API服务搭建 |
| 12 | [后端与API集成](./docs/12-后端与API集成/README.md) | 前后端联调 · 外部API集成 | 全栈AI应用 |
| 13 | [实时交互与多模态](./docs/13-实时交互与多模态/README.md) | Streaming · 图像/音频处理 | 多模态聊天 |
| 14 | [复杂AI Agent实战](./docs/14-复杂AI-Agent实战/README.md) | 工具编排 · 决策逻辑 · 记忆系统 | 智能助理Agent |
| 15 | [AI模型微调](./docs/15-AI模型微调/README.md) | PEFT · LoRA · QLoRA · 实战微调 | 垂直领域模型 |
| 16 | [模型部署与评估](./docs/16-模型部署与评估/README.md) | 本地/云部署 · 性能指标 · 成本优化 | 生产级部署 |
| 17 | [综合AI大模型项目落地](./docs/17-综合AI大模型项目落地/README.md) | 全周期项目 · 功能完备应用 | 5大综合项目 |

---

## 🗂️ 项目结构

```
AI大模型应用开发学习笔记/
├── README.md                    # 👈 你在这里 — 项目总览与导航
├── requirements.txt             # 统一依赖管理（按阶段分组）
├── .gitignore                   # Git忽略规则
│
├── docs/                        # 📚 核心学习文档（17个阶段）
│   ├── 01-Python编程与核心库/
│   │   ├── README.md            # 阶段主文档（理论+实践）
│   │   ├── 01-Python基础语法.md
│   │   ├── 02-NumPy数据处理.md
│   │   ├── 03-Matplotlib图表可视化.md
│   │   └── exercises/           # 练习与答案
│   ├── 02-机器学习基础/
│   ├── ...                      # （其余15个阶段结构同）
│   └── 17-综合AI大模型项目落地/
│
├── code/                        # 💻 代码示例（按阶段组织）
│   ├── 01-python-basics/        # 与 docs/ 一一对应
│   ├── 02-ml-basics/
│   ├── ...
│   └── 17-capstone/
│
├── projects/                    # 🎯 五大综合实战项目
│   ├── intelligent-customer-service/  # 智能客服
│   ├── code-assistant/               # 代码助手
│   ├── smart-translator/             # 智能翻译
│   ├── text-generator/               # 文本生成
│   └── personal-assistant/           # 个人助手
│
├── resources/                   # 📦 参考资源
│   ├── 机器学习与深度学习/        # 原有补充笔记（经典ML/DL/CV）
│   ├── papers/                  # 必读论文清单
│   ├── tutorials/               # 外部教程链接
│   ├── cheatsheets/            # 速查表
│   └── models/                  # 模型配置记录
│
├── scripts/                     # 🔧 工具脚本
└── .vscode/                     # 编辑器配置
```

---

## 🎯 学习路线图

```
                    第1步              第2步              第3步
              ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
              │ Python编程与  │  │  机器学习基础  │  │  NLP基础     │
              │   核心库      │─▶│  线性回归     │─▶│  文本清洗    │
              │ NumPy·Pandas  │  │  决策树·训练  │  │  Token·词向量│
              └──────────────┘  └──────────────┘  └──────────────┘
                     │                  │                  │
                     ▼                  ▼                  ▼
              ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
              │  Transformer  │  │  HuggingFace │  │  大模型API   │
              │  Attention    │─▶│  开源模型库   │─▶│  应用基础    │
              │  BERT·GPT     │  │  Pipelines   │  │  Token·定价  │
              └──────────────┘  └──────────────┘  └──────────────┘
                     │                  │                  │
                     ▼                  ▼                  ▼
              ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
              │  Prompt      │  │  AI Agent    │  │  RAG基础     │
              │  Engineering │─▶│  智能体概念   │─▶│  检索增强    │
              │  CoT·Few-shot│  │  感知规划执行 │  │  搜索·接入   │
              └──────────────┘  └──────────────┘  └──────────────┘
                     │                  │                  │
                     ▼                  ▼                  ▼
              ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
              │  向量数据库   │  │  AI全栈开发  │  │  后端与API   │
              │  Milvus·Chroma│─▶│  FastAPI·前端│─▶│  集成        │
              │  数据管理     │  │  UI设计      │  │  外部API     │
              └──────────────┘  └──────────────┘  └──────────────┘
                     │                  │                  │
                     ▼                  ▼                  ▼
              ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
              │  实时交互与   │  │  复杂Agent   │  │  模型微调    │
              │  多模态      │─▶│  实战        │─▶│  PEFT·LoRA   │
              │  Streaming   │  │  工具调用    │  │  参数高效    │
              └──────────────┘  └──────────────┘  └──────────────┘
                     │                  │                  │
                     ▼                  ▼                  ▼
              ┌──────────────┐  ┌──────────────────────────────┐
              │  模型部署与   │  │  综合AI大模型项目落地        │
              │  评估        │─▶│  5大综合项目实战              │
              │  云部署·监控 │  │  智能客服·代码助手·翻译·     │
              └──────────────┘  │  文本生成·个人助手            │
                                └──────────────────────────────┘
```

---

## 🚦 使用指南

### 初学者（零基础 → 入门）

```bash
# 1. 克隆/进入项目
cd AI大模型应用开发学习笔记

# 2. 创建虚拟环境
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 3. 按阶段安装依赖
pip install numpy pandas matplotlib jupyter  # 第1步
# ... 逐步安装后续依赖

# 4. 按顺序学习 docs/ 中的文档
# 每学完一个阶段，运行 code/ 中对应的示例代码
```

### 有经验者（跳过基础 → 直接深入）

```bash
# 安装全部依赖
pip install -r requirements.txt

# 从第6步开始（大模型API应用基础）
# 或直接跳到感兴趣的主题：
# - 想了解RAG → docs/09-RAG基础/
# - 想做微调 → docs/15-AI模型微调/
# - 想做Agent → docs/14-复杂AI-Agent实战/
```

### 面试准备

| 目标岗位 | 重点阶段 |
|----------|----------|
| **AI应用开发工程师** | 1→6→7→9→10→11→12→16 |
| **Prompt Engineer** | 1→6→7（重点）→8→13 |
| **AI产品经理** | 1→2→6→7→8→9→17 |
| **MLOps工程师** | 1→2→15→16（重点）→17 |
| **全栈AI工程师** | 全部17个阶段 |

---

## 📊 学习进度跟踪

| 阶段 | 状态 | 开始日期 | 完成日期 | 笔记 |
|:---:|:----:|:--------:|:--------:|------|
| 01 | ⬜ 待学习 | - | - | |
| 02 | ⬜ 待学习 | - | - | |
| 03 | ⬜ 待学习 | - | - | |
| 04 | ⬜ 待学习 | - | - | |
| 05 | ⬜ 待学习 | - | - | |
| 06 | ⬜ 待学习 | - | - | |
| 07 | ⬜ 待学习 | - | - | |
| 08 | ⬜ 待学习 | - | - | |
| 09 | ⬜ 待学习 | - | - | |
| 10 | ⬜ 待学习 | - | - | |
| 11 | ⬜ 待学习 | - | - | |
| 12 | ⬜ 待学习 | - | - | |
| 13 | ⬜ 待学习 | - | - | |
| 14 | ⬜ 待学习 | - | - | |
| 15 | ⬜ 待学习 | - | - | |
| 16 | ⬜ 待学习 | - | - | |
| 17 | ⬜ 待学习 | - | - | |

> 💡 在对应阶段文档中标记 `✅ 已完成` 来追踪进度

---

## 🔗 推荐资源

### 必读论文
- [Attention Is All You Need (2017)](https://arxiv.org/abs/1706.03762)
- [BERT: Pre-training of Deep Bidirectional Transformers (2018)](https://arxiv.org/abs/1810.04805)
- [GPT-3: Language Models are Few-Shot Learners (2020)](https://arxiv.org/abs/2005.14165)
- [LoRA: Low-Rank Adaptation of Large Language Models (2021)](https://arxiv.org/abs/2106.09685)

### 推荐书籍
- 《Deep Learning》 — Ian Goodfellow
- 《Speech and Language Processing》 — Jurafsky & Martin
- 《动手学深度学习》(D2L) — 李沐

### 在线平台
- [Hugging Face](https://huggingface.co/)
- [Google Colab](https://colab.research.google.com/)
- [Kaggle](https://www.kaggle.com/)

---

## ⚠️ 重要提醒

1. **顺序不要乱：** 本路线经过精心设计，每个阶段都是下一个的基础。切勿跳过基础直接学Agent或微调。
2. **动手实践：** 每个阶段至少完成 `code/` 中的对应示例，光看不练等于白学。
3. **API Key安全：** 从第6步开始使用API Key时，务必使用 `.env` 管理，不要硬编码。
4. **成本意识：** 大模型API调用有成本，学习时尽量使用免费额度或本地小模型。

---

> 📝 *这份笔记持续更新中，欢迎提Issue和PR。*
> 
> 🎓 *学习路上遇到问题？每个阶段文档末尾都有常见问题解答。*
