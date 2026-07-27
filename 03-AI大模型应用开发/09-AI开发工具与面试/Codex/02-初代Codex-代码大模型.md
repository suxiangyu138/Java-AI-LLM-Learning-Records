# 02 - 初代 Codex：代码大模型

> 🎯 2021 年全球首个大规模代码生成模型。它驱动了第一版 GitHub Copilot，打开了 AI 编码时代的大门

---

## 1. 基础定义

初代 Codex 是基于 GPT-3 架构、在 159GB Python 代码上微调的代码专用 Transformer 大模型。

- 系列模型：`code-cushman`（12B）、`code-davinci-002`（175B）
- 训练数据：5400 万 GitHub 公开仓库
- API 形态：Completion API（非 Chat API）
- 核心能力：代码生成、代码补全、代码翻译、代码解释

---

## 2. 发展时间线

| 时间 | 事件 |
|:---:|------|
| 2020.06 | GPT-3 发布，展现初步代码生成能力 |
| 2021.06 | GitHub Copilot 技术预览（底层 = Codex） |
| 2021.08 | Codex API 开放 beta，论文发表 |
| 2022.06 | Copilot GA，$10/月 |
| 2023.03 | OpenAI 宣布关闭 Codex API |
| 2023.03 | Copilot 引擎升级到 GPT-3.5 |
| 2023.12 | Copilot Chat 升级到 GPT-4 |
| 2024.01 | Codex 模型从 API 完全移除 |

---

## 3. 底层原理

### 架构

标准 GPT Decoder-Only Transformer，与 GPT-3 同构。核心创新是 **FIM（Fill-in-the-Middle）训练**：

```text
传统自回归：上文 → 预测下文
FIM 训练：   <PRE>上文<SUF>下文<MID>要预测的中间代码

这使得 Codex 能够：
  → 在已有代码中间插入新代码
  → Copilot 的"光标位置补全"体验！
```

### HumanEval 表现

```text
Codex-12B:     28.8% (Pass@1)
Codex-12B+RM:  37.7% (Pass@1, 加Reward Model重排序)
GPT-3:          0.0%
GPT-4:         67.0%
GPT-4o:       ~87.0%
```

---

## 4. 退役原因

1. **GPT-3.5/GPT-4 全面超越**：代码能力 + 指令遵循 + 上下文窗口三维碾压
2. **维护成本**：维护独立模型和 API 不再有价值
3. **API 统一**：OpenAI 将所有能力整合到 Chat Completions API
4. **Copilot 已迁移**：微软 2023 年已将 Copilot 切换到 GPT-4

---

## 5. 历史遗产

- **FIM 训练标准**：StarCoder、CodeLlama、DeepSeek-Coder 全部沿用
- **Copilot 的诞生**：没有 Codex 就没有 Copilot
- **代码生成可行性证明**：首次证明 LLM 可以产生商用价值的代码
- **安全/版权讨论**：引发的训练数据版权争议至今仍在继续
