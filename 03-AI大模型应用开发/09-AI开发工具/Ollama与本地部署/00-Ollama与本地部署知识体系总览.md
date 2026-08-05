# 00 - Ollama 与本地部署 知识体系总览

> 🎯 Ollama 让本地部署大模型像 `docker pull` 一样简单——7B 模型仅需 4GB 显存，API 100% 兼容 OpenAI，是 Java 后端集成 AI 的最快路径

> 🎯 本系列共 **12 篇**，从部署全景到 Ollama 实战、从量化技术到容器化、从硬件选型到面试冲刺，覆盖本地 AI 部署全链路

---

## 1. 知识全景

```
Ollama 与本地部署体系（12个文件）
│
├── 🏗️ 基础篇（01-02）
│   ├── 01-AI模型服务化部署全解析.md        # 云端/边缘/混合部署全景
│   └── 02-Ollama快速上手与基础操作.md       # 安装/模型管理/REST API/OpenAI兼容
│
├── 🔧 实战篇（03-05）
│   ├── 03-Ollama量化部署与性能优化.md       # GGUF选型/Modelfile/Docker Compose
│   ├── 04-Ollama与Java Client集成.md       # Spring AI/LangChain4j/HTTP Client
│   └── 05-Modelfile与自定义模型构建.md      # 系统提示词定制/参数调优/Safetensors
│
├── 🚀 进阶篇（06-09）
│   ├── 06-GGUF量化技术深度解析.md          # K-quant/I-quant/性能-质量权衡
│   ├── 07-vLLM与llama.cpp对比实战.md       # 三框架性能对比/场景选型
│   ├── 08-Docker与K8s容器化部署.md         # Docker Compose/K8s/GPU透传
│   └── 09-硬件选型与GPU性能调优.md         # GPU选型/显存计算/并发优化
│
├── 📋 运维篇（10）
│   └── 10-安全加固与运维监控.md            # API鉴权/HTTPS/日志/告警
│
└── 📌 冲刺篇（11）
    └── 11-面试高频考点与总结.md             # 面试题库/选型答辩/最佳实践
```

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | 知识体系总览 | 全景+路线 | — |
| 01 | 部署全解析 | 云端/边缘/混合/Docker/K8s | ⭐⭐⭐⭐ |
| 02 | Ollama快速上手 | 安装/模型管理/API | ⭐⭐⭐⭐⭐ |
| 03 | 量化部署与优化 | GGUF/Modelfile/Docker Compose | ⭐⭐⭐⭐⭐ |
| 04 | Java Client集成 | Spring AI/LangChain4j/HTTP | ⭐⭐⭐⭐⭐ |
| 05 | Modelfile自定义 | System Prompt/参数/模板 | ⭐⭐⭐⭐ |
| 06 | GGUF量化深度 | K-quant/I-quant/质量权衡 | ⭐⭐⭐⭐ |
| 07 | vLLM vs llama.cpp | 三框架对比/场景选型 | ⭐⭐⭐⭐ |
| 08 | Docker+K8s部署 | Compose/K8s/GPU透传 | ⭐⭐⭐ |
| 09 | 硬件选型与调优 | GPU/显存/并发/延迟 | ⭐⭐⭐⭐ |
| 10 | 安全与运维 | 鉴权/HTTPS/监控/告警 | ⭐⭐⭐ |
| 11 | 面试高频考点 | 题库/选型答辩/最佳实践 | ⭐⭐⭐⭐⭐ |

## 3. 学习路线

```text
🟢 上手（30min）：02-Ollama上手 → 在本地跑起第一个模型
🔵 实战（1h）：03-量化部署 → 04-Java集成 → 05-自定义模型
🟣 进阶（45min）：06-GGUF深度 → 07-vLLM对比 → 08-容器化
🟡 运维（30min）：09-硬件选型 → 10-安全监控
🔴 冲刺（30min）：01-全景回顾 → 11-面试
```

## 4. 核心概念速查

| 术语 | 含义 |
|------|------|
| **Ollama** | 一键运行本地大模型的轻量工具（底层 llama.cpp） |
| **GGUF** | GPT-Generated Unified Format — llama.cpp 的模型量化格式 |
| **Modelfile** | Ollama 的 Dockerfile 等价物，定义模型+参数 |
| **Q4_K_M** | 最推荐的量化格式：4bit + 中等质量，5GB 跑 8B 模型 |
| **vLLM** | 高吞吐推理引擎，PagedAttention + 连续批处理 |
| **llama.cpp** | CPU 优先的 C++ 推理框架，GGUF 格式的创造者 |
| **OpenAI 兼容 API** | Ollama 暴露 `/v1/chat/completions`，与 OpenAI SDK 无缝对接 |
