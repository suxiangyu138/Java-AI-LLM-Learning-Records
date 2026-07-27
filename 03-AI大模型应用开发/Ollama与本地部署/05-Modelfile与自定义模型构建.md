# 05 - Modelfile 与自定义模型构建

> 🎯 Modelfile 是 Ollama 的 "Dockerfile"——通过它你可以定制 System Prompt、绑定模型参数、导入 GGUF 文件、甚至从 Safetensors 创建模型，让每个项目拥有专属的 AI 助手

> **前置阅读**：[[02-Ollama快速上手与基础操作]]、[[03-Ollama量化部署与性能优化]]

---

## 目录

1. [Modelfile 基础语法](#1-modelfile-基础语法)
2. [实战场景：构建专属模型](#2-实战场景构建专属模型)
3. [从 GGUF 导入模型](#3-从-gguf-导入模型)
4. [从 Safetensors 创建模型](#4-从-safetensors-创建模型)
5. [模型版本管理](#5-模型版本管理)

---

## 1. Modelfile 基础语法

### 1.1 指令速查

| 指令 | 用途 | 示例 |
|------|------|------|
| `FROM` | 指定基础模型（必需） | `FROM llama3.1:8b` |
| `SYSTEM` | 设置系统提示词 | `SYSTEM "You are a Java expert"` |
| `PARAMETER` | 覆盖默认推理参数 | `PARAMETER temperature 0.7` |
| `TEMPLATE` | 自定义 ChatML 模板 | `TEMPLATE """..."""` |
| `ADAPTER` | 加载 LoRA 适配器 | `ADAPTER ./lora-weights.bin` |
| `LICENSE` | 声明许可证 | `LICENSE "MIT"` |
| `MESSAGE` | 预设对话历史 | `MESSAGE user "Hello"` |

### 1.2 可用参数

| 参数 | 默认 | 说明 |
|------|:---:|------|
| `temperature` | 0.8 | 随机性（0=确定，1=创意） |
| `top_p` | 0.9 | 核采样阈值 |
| `top_k` | 40 | Top-K 采样 |
| `num_ctx` | 2048 | 上下文窗口大小 |
| `num_predict` | 128 | 最大生成 token 数（-1=无限） |
| `repeat_penalty` | 1.1 | 重复惩罚（>1 减少重复） |
| `seed` | 0 | 随机种子（0=随机） |
| `stop` | — | 停止序列 |
| `mirostat` | 0 | Mirostat 采样（0=关闭，1=v1，2=v2） |

## 2. 实战场景：构建专属模型

### 2.1 Java 后端专家

```dockerfile
# Modelfile — 文件名: JavaExpert.Modelfile
FROM qwen2.5:7b

SYSTEM """
You are a Java 17+ backend expert with 10 years of experience.
Follow these rules strictly:
1. Always provide complete, production-ready code
2. Use Spring Boot 3.x conventions
3. Include error handling and logging
4. Mention Java version and dependencies used
5. Use modern Java features (records, sealed classes, virtual threads)
6. Write in Chinese when the user asks in Chinese
"""

PARAMETER temperature 0.5
PARAMETER top_p 0.9
PARAMETER num_ctx 8192
PARAMETER repeat_penalty 1.05
PARAMETER stop "</answer>"
```

```bash
# 创建模型
ollama create java-expert -f JavaExpert.Modelfile

# 运行
ollama run java-expert
```

### 2.2 代码审查助手

```dockerfile
# CodeReviewer.Modelfile
FROM deepseek-coder-v2:16b

SYSTEM """
You are a senior code reviewer. For each code snippet, output:

<review>
## Summary
Brief overview of the code

## Issues
| Severity | Line | Issue | Suggestion |
|----------|------|-------|------------|

## Positive Aspects
- What was done well

## Suggested Refactor
```language
// improved code
```
</review>
"""

PARAMETER temperature 0.3      # 审查需要确定性强
PARAMETER num_ctx 16384        # 大文件需要长上下文
PARAMETER num_predict -1
```

### 2.3 多角色切换模型

```dockerfile
# MultiPersona.Modelfile
FROM llama3.1:8b

SYSTEM """
You can switch between three personas based on user needs:

1. 📚 Teacher Mode — Explain concepts step by step with examples
2. 🔧 Engineer Mode — Direct, code-first answers
3. 🎨 Creative Mode — Brainstorming, explore multiple angles

Default to Teacher Mode. Switch when user explicitly asks.
Always state which mode you're in at the start of the response.
"""

PARAMETER temperature 0.7
```

### 2.4 使用 MESSAGE 预设知识

```dockerfile
# CompanyKB.Modelfile
FROM qwen2.5:7b

SYSTEM "You are the internal knowledge base assistant for Acme Corp."

MESSAGE user "What is our tech stack?"
MESSAGE assistant "Acme Corp uses: Java 21 + Spring Boot 3.2 + PostgreSQL 16 + Redis 7 + Kafka 3.6, deployed on K8s (EKS)."

MESSAGE user "What is our deployment process?"
MESSAGE assistant "We use GitOps with ArgoCD. All merges to main trigger automatic staging deployment. Production requires manual approval."

# 更多公司知识...
```

## 3. 从 GGUF 导入模型

### 3.1 从 HuggingFace 导入

```bash
# 1. 下载 GGUF 文件
# HuggingFace: TheBloke/Llama-3.1-8B-GGUF

# 2. 创建 Modelfile 指向本地 GGUF 文件
cat > Modelfile << 'EOF'
FROM ./llama-3.1-8b-Q4_K_M.gguf

TEMPLATE """<|start_header_id|>system<|end_header_id|>

{{ .System }}<|eot_id|><|start_header_id|>user<|end_header_id|>

{{ .Prompt }}<|eot_id|><|start_header_id|>assistant<|end_header_id|>
"""

PARAMETER stop "<|eot_id|>"
PARAMETER stop "<|start_header_id|>"
EOF

# 3. 创建并运行
ollama create my-llama -f Modelfile
ollama run my-llama
```

### 3.2 导入自定义微调模型

```bash
# 从微调后的 GGUF 文件创建
ollama create my-finetuned-model -f Modelfile

# 查看模型详情
ollama show my-finetuned-model

# 模型文件位置
# Linux: /usr/share/ollama/.ollama/models/
# macOS: ~/.ollama/models/
# Windows: C:\Users\<user>\.ollama\models\
```

## 4. 从 Safetensors 创建模型

```python
# convert_to_gguf.py — 将 HuggingFace Safetensors 转 GGUF
from llama_cpp import Llama
import subprocess

# 方法一：使用 llama.cpp 的 convert 工具
# git clone https://github.com/ggerganov/llama.cpp
# cd llama.cpp && make

# 1. 转换 Safetensors → FP16 GGUF
subprocess.run([
    "python", "convert_hf_to_gguf.py",
    "path/to/huggingface/model",
    "--outfile", "model-f16.gguf",
    "--outtype", "f16"
])

# 2. 量化 FP16 → Q4_K_M
subprocess.run([
    "./llama-quantize",
    "model-f16.gguf",
    "model-Q4_K_M.gguf",
    "Q4_K_M"
])
```

```bash
# 3. 通过 Ollama 创建
cat > Modelfile << 'EOF'
FROM ./model-Q4_K_M.gguf
EOF

ollama create my-custom-model -f Modelfile
```

## 5. 模型版本管理

```bash
# 查看所有本地模型
ollama list
# NAME              ID              SIZE      MODIFIED
# java-expert:latest abc123def456   4.7 GB    2 days ago
# java-expert:v1     def456abc789   4.7 GB    1 week ago

# 复制模型（创建新版本）
ollama cp java-expert:latest java-expert:v2

# 更新 Modelfile → 重新创建
ollama create java-expert:v2 -f JavaExpert-v2.Modelfile

# 删除旧版本
ollama rm java-expert:v1

# 导出模型为 GGUF（分享给他人）
# Ollama 暂无内置导出命令，需从 models 目录手动复制 blob 文件
```

```text
推荐版本策略：
├── :latest  → 当前生产版本
├── :v1, :v2 → 历史版本（保留最近 3 个）
├── :dev     → 开发中版本
└── :test    → A/B 测试版本（不同 system prompt）
```

## 核心要点回顾

- Modelfile = Ollama 的 Dockerfile：FROM + SYSTEM + PARAMETER + TEMPLATE
- 关键参数：temperature（创意度）、num_ctx（上下文窗口）、repeat_penalty（防重复）
- SYSTEM 指令是最核心的定制能力——决定模型的"人格"
- MESSAGE 指令可预设知识，将公司文档灌入模型
- 从 GGUF 导入只需 `FROM ./model.gguf` 一行
- 版本管理：`:latest` + `:v1/v2` + `:dev` 三级策略

## 参考资料

1. Ollama 官方 Modelfile 文档 — github.com/ollama/ollama
2. llama.cpp GGUF 格式规范
3. HuggingFace GGUF 模型集合 — TheBloke
