# AI 示例 (AI Examples)

> Python AI/LLM 实战示例集：API 调用、Prompt 工程、RAG、Agent、向量数据库

## 项目概述

AI 大模型应用开发综合示例项目，涵盖从基础 API 调用到高级 RAG/Agent 模式的完整实践。使用 OpenAI API / Anthropic API / 本地模型（Ollama），掌握 Prompt Engineering、Function Calling、Embedding、向量检索、Agent 开发等核心技能。

## 技术栈

| 技术 | 说明 |
|------|------|
| Python | 3.10+ |
| OpenAI SDK | GPT 系列模型调用 |
| Anthropic SDK | Claude 系列模型调用 |
| LangChain | LLM 应用开发框架 |
| Ollama | 本地模型部署与调用 |
| Chroma / FAISS | 向量数据库 |
| Streamlit / Gradio | AI 应用 UI（可选） |

## 学习路线

### 第一阶段：API 基础调用
- OpenAI Chat Completions API
- Anthropic Messages API
- 流式输出（Streaming）
- Token 计算与管理
- 多轮对话（Conversation History）

### 第二阶段：Prompt Engineering
- System Prompt 设计
- Few-shot Prompting
- Chain of Thought（CoT）
- 结构化输出（JSON Mode）
- Prompt 模板化

### 第三阶段：RAG（检索增强生成）
- Document Loader（PDF/TXT/Web）
- Text Splitter（字符分割/语义分割）
- Embedding 向量化
- 向量数据库（Chroma / FAISS）
- 相似度检索 + LLM 生成
- RAG Pipeline 完整流程

### 第四阶段：Function Calling / Tool Use
- 工具定义（JSON Schema）
- Function Calling 流程
- 多工具组合调用
- 错误处理与重试

### 第五阶段：Agent 开发
- ReAct Agent 模式
- 规划与执行分离
- Memory 管理
- MCP（Model Context Protocol）
- Multi-Agent 协作

### 第六阶段：模型微调与部署
- LoRA 微调基础
- 本地模型部署（Ollama / vLLM）
- API 服务化

## 项目结构

```
AI示例/
├── api_basics/
│   ├── openai_demo.py               # OpenAI API 调用
│   ├── anthropic_demo.py            # Anthropic API 调用
│   └── ollama_demo.py               # Ollama 本地调用
├── prompt_engineering/
│   ├── system_prompt.py
│   ├── few_shot.py
│   └── chain_of_thought.py
├── rag/
│   ├── document_loader.py           # 文档加载
│   ├── embedding_demo.py            # Embedding 向量化
│   ├── vector_store.py              # 向量数据库
│   └── rag_pipeline.py              # 完整 RAG 流程
├── function_calling/
│   └── tool_use_demo.py             # Function Calling
├── agent/
│   ├── simple_agent.py              # 基础 Agent
│   └── react_agent.py               # ReAct Agent
├── .env.example                     # API Key 模板
├── requirements.txt
├── main.py
└── README.md
```

## 快速开始

```bash
# 1. 安装依赖
pip install -r requirements.txt

# 2. 配置 API Key
cp .env.example .env
# 编辑 .env 填入 API Key

# 3. 运行示例
python main.py
```

## 环境变量配置 (.env)

```bash
OPENAI_API_KEY=sk-xxx
ANTHROPIC_API_KEY=sk-ant-xxx
OLLAMA_BASE_URL=http://localhost:11434
```

## 核心知识点

| 知识点 | 说明 |
|--------|------|
| Chat Completion API | 多轮对话、System/User/Assistant 角色 |
| Streaming | 流式逐字输出，提升用户体验 |
| Prompt Engineering | System Prompt、Few-shot、CoT |
| Token 管理 | Token 计数、上下文窗口限制 |
| Embedding | 文本向量化、语义相似度 |
| Vector Store | 向量检索、Top-K 相似度搜索 |
| RAG Pipeline | Load → Split → Embed → Store → Retrieve → Generate |
| Function Calling | 工具定义、参数解析、结果回传 |
| Agent | ReAct 模式（Thought → Action → Observation） |
| MCP | 模型上下文协议，标准化工具接口 |

## 注意事项

- API Key 务必放在 .env 文件中，不要提交到 Git
- 大模型 API 调用会产生费用，开发时注意控制调用频率
- 本地模型（Ollama）需要较好的硬件配置（建议 16GB+ RAM）
- Embedding 模型建议使用 text-embedding-3-small（OpenAI）或 bge-large（本地）
- RAG 的检索质量取决于文档分割策略和 Embedding 模型选择
