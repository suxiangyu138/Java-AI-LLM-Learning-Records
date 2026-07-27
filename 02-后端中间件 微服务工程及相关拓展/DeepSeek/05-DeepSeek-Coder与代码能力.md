# DeepSeek-Coder 与代码能力

> 💻 从代码补全到仓库级理解：Fill-in-the-Middle 训练、Repository-level 上下文、16B 到 236B 的代码模型进化之路

---

## 📚 目录

1. [DeepSeek-Coder 概述](#1-deepseek-coder-概述)
2. [训练数据与方法论](#2-训练数据与方法论)
3. [Fill-in-the-Middle (FIM) 训练](#3-fill-in-the-middle-fim-训练)
4. [仓库级代码理解](#4-仓库级代码理解)
5. [DeepSeek-Coder-V2：MoE 代码模型](#5-deepseek-coder-v2moe-代码模型)
6. [性能基准与对比](#6-性能基准与对比)
7. [使用指南与工具集成](#7-使用指南与工具集成)

---

## 1. DeepSeek-Coder 概述

### 1.1 模型演进

```text
DeepSeek-Coder 发展历程：

2023.07  DeepSeek-Coder-1.3B  →  轻量代码模型
2023.11  DeepSeek-Coder-6.7B  →  消费级 GPU 可用
2023.11  DeepSeek-Coder-33B   →  Dense 代码模型的 SOTA
2024.06  DeepSeek-Coder-V2    →  MoE+MLA，对标 GPT-4o
2024.12  DeepSeek-Coder-V2-0324 → 重大更新
```

### 1.2 模型规格对比

| 模型 | 参数量 | 架构 | 上下文 | 编程语言 | 代码训练 Token |
|------|:-----:|------|:-----:|---------|:-----:|
| Coder-1.3B | 1.3B | Dense | 16K | 87种 | 1T |
| Coder-6.7B | 6.7B | Dense | 16K | 87种 | 1T |
| Coder-33B | 33B | Dense | 16K | 87种 | 2T |
| Coder-V2 | 236B/21B | MoE+MLA | 128K | 338种 | 6T |
| Coder-V2-0324 | 671B/37B | MoE+MLA | 128K | 338种 | 更多 |

---

## 2. 训练数据与方法论

### 2.1 数据来源

```text
DeepSeek-Coder 训练数据组成：

  GitHub 公开仓库（经严格过滤）：
    ├── 按 Stars / 活跃度过滤
    ├── 去除个人项目、实验代码
    └── 保留高质量开源项目

  依赖关系分析：
    ├── 分析仓库内文件依赖
    ├── 按依赖拓扑排序组织训练
    └── 保留跨文件引用的上下文

  代码-文本配比：
    ├── 87% 纯代码（Code）
    └── 13% 代码相关自然语言（Issues, PRs, Docs, Commit Messages）
```

### 2.2 支持的语言

```text
DeepSeek-Coder 支持 87 种编程语言：

  主力语言（训练占比 > 1%）：
    Python, Java, JavaScript, TypeScript, C, C++, C#, Go,
    Rust, Ruby, PHP, Swift, Kotlin, HTML, CSS, SQL, Shell

  覆盖场景：
    ├── 通用编程：Python, Java, C++, Go...
    ├── 前端开发：TypeScript, React, Vue...
    ├── 移动开发：Swift, Kotlin, Dart...
    ├── 数据科学：SQL, R, Julia...
    ├── 系统编程：Rust, C, Assembly...
    └── 脚本工具：Shell, PowerShell, Perl...

DeepSeek-Coder-V2 扩展到 338 种编程语言！
```

### 2.3 数据处理 Pipeline

```text
代码数据处理流程：

  原始仓库
      │
      ▼
  1. 仓库质量过滤
      ├── Stars ≥ threshold（可配置）
      ├── 排除 fork 和镜像
      └── 排除 test/example 比例过高的仓库
      │
      ▼
  2. 文件级过滤
      ├── 根据扩展名识别语言
      ├── 排除自动生成的代码（如 min.js, generated/）
      ├── 排除过短/过长的文件
      └── 去重（文件级 + 函数级 MinHash）
      │
      ▼
  3. 仓库级结构化
      ├── 分析 import / include 依赖
      ├── 按依赖拓扑排序
      └── 构建跨文件引用图
      │
      ▼
  4. 代码 Mask 与 PII 移除
      ├── 替换邮箱、API Key 等敏感信息
      ├── 标准化路径引用
      └── 统一文件编码（UTF-8）
      │
      ▼
  5. Tokenization
      ├── 代码专用词表（含大量代码符号）
      └── 训练用 token
```

---

## 3. Fill-in-the-Middle (FIM) 训练

### 3.1 FIM 是什么

```text
FIM = Fill-in-the-Middle（中间填充）

传统自回归生成：只能补全光标后面的内容
  cursor → | "hello, world!"
           → 只能生成右边

FIM：可以从中间任意位置补全
  "hello, " |MASK| "!"
  → 模型填空 → "world"

应用场景：
  ✅ 代码补全（在已有的代码框架中插入新代码）
  ✅ 代码修复（替换中间的 bug 代码）
  ✅ Inline Chat（在代码中间插入 AI 建议）
  ✅ 重构（替换方法体）
```

### 3.2 FIM 训练格式

```text
FIM 训练使用特殊的 Sentinel Token：

  <fim_prefix>  →  前缀标记（光标前的代码）
  <fim_suffix>  →  后缀标记（光标后的代码）
  <fim_middle>  →  中间标记（需要生成的代码）

训练数据构造（PSM 模式）：

  原始代码：
    def add(a, b):
        return a + b

  切分（随机位置）：
    前缀 = "def add(a, b):\n    "
    中间 = "return a + b"       ← 训练目标
    后缀 = ""（末尾）

  FIM 格式输入：
    <fim_prefix>def add(a, b):
        <fim_suffix><fim_middle>return a + b

模型需要预测 <fim_middle> 之后的内容 = "return a + b"
```

### 3.3 FIM 模式：PSM vs SPM

```text
PSM 模式（Prefix-Suffix-Middle，DeepSeek 采用）：
  <fim_prefix>PREFIX<fim_suffix>SUFFIX<fim_middle>MIDDLE

SPM 模式（Suffix-Prefix-Middle，CodeLlama 等采用）：
  <fim_suffix>SUFFIX<fim_prefix>PREFIX<fim_middle>MIDDLE

DeepSeek 为什么选择 PSM？
  ✅ 更自然：先看到上下文，再填空
  ✅ 前缀的注意力模式更符合人类编码习惯
  ✅ 实验表明 PSM 性能略优于 SPM
```

### 3.4 FIM 训练策略

| 参数 | 设置 | 说明 |
|------|------|------|
| **FIM 比例** | 50% | 50% 样本用 FIM，50% 用标准自回归 |
| **切分位置** | 随机（几何分布） | 偏向前缀更长 |
| **切分策略** | Token 级（非行级） | 任意位置都可以切 |
| **<fim_middle> 标记** | 使用 | 明确标记中间段的开始 |
| **Span 平均长度** | ~32% 的文件总长 | 遮住的中间段平均约 1/3 文件长度 |

```python
# FIM 数据构造伪代码
def create_fim_sample(code_tokens, fim_rate=0.5):
    if random() > fim_rate:
        return code_tokens  # 标准自回归

    # 随机选择切分位置（几何分布，偏向前缀长）
    split_ratio = geometric_sample(p=0.2)  # 前面长
    prefix_len = int(len(code_tokens) * split_ratio)

    # 随机选择中间段长度
    middle_len = int(len(code_tokens) * random() * 0.5)

    prefix = code_tokens[:prefix_len]
    middle = code_tokens[prefix_len:prefix_len + middle_len]
    suffix = code_tokens[prefix_len + middle_len:]

    # PSM 格式
    return [FIM_PREFIX] + prefix + [FIM_SUFFIX] + suffix + [FIM_MIDDLE] + middle
```

---

## 4. 仓库级代码理解

### 4.1 为什么需要仓库级理解

```text
传统代码补全的局限：

  仅看当前文件 → 无法感知：
    ❌ 其他文件中定义的类和函数签名
    ❌ 项目的整体架构和设计模式
    ❌ 配置文件中的设置
    ❌ 依赖库的 API

仓库级理解 → 模型能：
    ✅ 感知跨文件依赖关系
    ✅ 理解项目结构和架构
    ✅ 知道自定义类/函数的签名
    ✅ 遵循项目的编码规范
    ✅ 生成符合项目风格的代码
```

### 4.2 仓库级上下文的实现

```text
DeepSeek-Coder 的仓库级上下文策略：

  1. 依赖分析
     ├── 解析 import/include/require 语句
     └── 构建文件依赖图（DAG）

  2. 上下文组装
     ├── 当前文件（完整）→ 主要输入
     ├── 依赖文件（摘要）→ 提供接口信息
     │   └── 提取：类定义、函数签名、类型注解、注释
     └── 同级文件（摘要）→ 提供项目模式参考

  3. Topological Sort
     ├── 按依赖顺序排列文件
     └── 确保先出现的文件不引用后出现的文件

  4. 窗口裁剪
     ├── 128K 上下文窗口
     ├── 当前文件占主要空间
     └── 依赖摘要被压缩以节省空间
```

### 4.3 Repo-level 训练数据的构造

```text
仓库级训练样本构造：

  一个样本 = 仓库中所有文件按依赖拓扑排序

  Sample:
    [file1.py 的全部内容]
    # 依赖 file1.py 的类
    [file2.py: 摘要 file1 的导出接口 + file2.py 内容]
    # 依赖 file1 和 file2
    [file3.py: 摘要 file1+file2 的导出接口 + file3.py 内容]

  关键设计：
    → 每个文件既是训练目标又是上下文的提供者
    → 依赖接口摘要 → 模拟真实 IDE 中的跨文件引用
    → 训练模型在"部分信息"下做准确推断
```

---

## 5. DeepSeek-Coder-V2：MoE 代码模型

### 5.1 架构升级

| 维度 | Coder-V1 (33B) | Coder-V2 (236B) |
|------|:------------:|:-------------:|
| **架构** | Dense | MoE + MLA |
| **总参数** | 33B | 236B |
| **激活参数** | 33B | 21B |
| **上下文** | 16K | 128K |
| **编程语言** | 87 | 338 |
| **训练 Token** | 2T | 6T |
| **核心创新** | - | MoE 稀疏激活 + MLA 长上下文 |

### 5.2 V2 的核心提升

```text
DeepSeek-Coder-V2 相对 V1 的五大提升：

  1. 代码生成质量 ↑
     → MoE 架构：236B 参数提供更丰富的知识表示

  2. 长上下文理解 ↑
     → 128K 上下文 + MLA 高效处理
     → 可以加载整个中小型项目

  3. 语言覆盖 ↑
     → 从 87 种扩展到 338 种

  4. 推理效率 ↑
     → 激活参数仅 21B，推理速度快于 33B Dense 模型

  5. 数学与推理能力 ↑
     → 训练数据融入了数学和逻辑推理
     → 不仅会写代码，还会"思考"
```

---

## 6. 性能基准与对比

### 6.1 HumanEval / MBPP

| 模型 | HumanEval | MBPP | 参数量 |
|------|:-------:|:----:|:-----:|
| DeepSeek-Coder-1.3B | 65.8 | 63.8 | 1.3B |
| DeepSeek-Coder-6.7B | 78.6 | 72.0 | 6.7B |
| DeepSeek-Coder-33B | 82.5 | 76.2 | 33B |
| DeepSeek-Coder-V2 | **90.2** | **81.4** | 236B/21B |
| GPT-4-Turbo | 87.1 | 79.0 | - |
| GPT-4o | 90.2 | - | - |
| Claude 3.5 Sonnet | 92.0 | 84.8 | - |
| Llama-3-70B | 81.7 | 73.6 | 70B |
| CodeLlama-70B | 67.8 | 62.2 | 70B |

### 6.2 仓库级补全（RepoBench / CrossCodeEval）

| 模型 | RepoBench | CrossCodeEval |
|------|:-------:|:------------:|
| DeepSeek-Coder-6.7B | 78.1 | 67.5 |
| DeepSeek-Coder-V2 | 85.3 | 76.8 |
| GPT-4-Turbo | 79.6 | 73.5 |

### 6.3 SWE-bench（软件工程任务）

| 模型 | SWE-bench Verified | SWE-bench Lite |
|------|:---------------:|:------------:|
| DeepSeek-Coder-V2 | 12.3% | 15.7% |
| DeepSeek-R1 | 49.2% | - |
| GPT-4o | 38.8% | 24.7% |

> 💡 R1 的 SWE-bench 大幅提升说明推理能力对软件工程至关重要！

---

## 7. 使用指南与工具集成

### 7.1 常见使用场景

```text
DeepSeek-Coder 的使用场景矩阵：

  IDE 代码补全：
    └── FIM 能力 → 光标处智能补全
    └── 工具：Continue.dev, Cursor, Copilot (API 代理)

  代码对话：
    └── 解释代码、重构建议、Bug 修复
    └── 支持多轮对话，保持上下文

  仓库级任务：
    └── 加载整个仓库到 128K 上下文
    └── 跨文件理解、架构分析

  API 集成：
    └── OpenAI 兼容 API
    └── Chat Completions + FIM Completions
```

### 7.2 FIM API 调用示例

```python
# OpenAI 兼容的 FIM API 调用
import openai

client = openai.OpenAI(
    base_url="https://api.deepseek.com/v1",
    api_key="sk-xxx"
)

# FIM 代码补全
response = client.completions.create(
    model="deepseek-coder",
    prompt="def fibonacci(n):\n    if n <= 1:\n        return n\n    else:\n        ",
    suffix="\n\n# Test\nprint(fibonacci(10))",  # 后缀（光标后的代码）
    max_tokens=100,
    temperature=0.0,
    stop=["\n\n"]
)

print(response.choices[0].text)
# 输出: "return fibonacci(n-1) + fibonacci(n-2)"
```

### 7.3 Chat 模式

```python
# DeepSeek-Coder-V2 Chat 模式
response = client.chat.completions.create(
    model="deepseek-coder",
    messages=[
        {
            "role": "system",
            "content": "You are a senior Python developer."
        },
        {
            "role": "user",
            "content": """请帮我优化这段代码的性能：

def find_duplicates(lst):
    duplicates = []
    for i in range(len(lst)):
        for j in range(i+1, len(lst)):
            if lst[i] == lst[j] and lst[i] not in duplicates:
                duplicates.append(lst[i])
    return duplicates"""
        }
    ],
    temperature=0.3
)

print(response.choices[0].message.content)
```

### 7.4 工具/IDE 集成

| 工具 | 集成方式 | 说明 |
|------|---------|------|
| **Continue.dev** | 直接选择 DeepSeek 作为 Provider | VS Code / JetBrains 插件 |
| **Cursor** | 自定义 API Endpoint | 配置 OpenAI-compatible endpoint |
| **Cline** | 自定义 Provider | VS Code AI 编程助手 |
| **ollama** | `ollama pull deepseek-coder-v2` | 本地一键部署 |
| **vLLM** | OpenAI-compatible server | 高性能推理服务 |
| **TabbyML** | 后端集成 | 自托管代码补全 |

### 7.5 本地部署建议

```bash
# Ollama 本地部署
ollama pull deepseek-coder-v2:16b  # 轻量版
ollama pull deepseek-coder-v2:236b # 完整版（需大显存）

# vLLM 高性能部署
python -m vllm.entrypoints.openai.api_server \
    --model deepseek-ai/DeepSeek-Coder-V2-Instruct \
    --tensor-parallel-size 4 \
    --max-model-len 65536 \
    --port 8000

# Transformers 加载
from transformers import AutoModelForCausalLM, AutoTokenizer

model = AutoModelForCausalLM.from_pretrained(
    "deepseek-ai/DeepSeek-Coder-V2-Instruct",
    trust_remote_code=True,
    torch_dtype="auto",
    device_map="auto"
)
```

---

> 🎯 **一句话总结**：DeepSeek-Coder 通过 **FIM 训练 + 仓库级上下文 + MoE 架构** 三条技术路线，构建了从 1.3B 到 236B 的完整代码模型矩阵，其中 V2 以 21B 激活参数达到 GPT-4o 级别的代码能力。

---

**下一模块**：[06-DeepSeek多模态与前沿探索](./06-DeepSeek多模态与前沿探索.md) → Janus 统一多模态理解与生成

---

*最后更新：2026年7月*
