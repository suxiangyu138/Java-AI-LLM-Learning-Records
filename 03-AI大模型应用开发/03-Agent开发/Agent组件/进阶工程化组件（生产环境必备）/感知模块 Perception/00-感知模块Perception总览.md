# 感知模块 Perception 知识体系总览

> 定位：Agent 生产工程化组件之「感知模块」——Agent 的感官接口：把环境原始输入（文本/图像/音频/传感器/系统状态）转化为结构化上下文的组件。2026 核心共识：**感知从"被动全量摄入"走向"主动按需感知"**（token 省 57% 准确率不变）；多模态从"拼装管道"走向"原生 Omni 统一入口"；上下文装配成为一等架构关注点。总览做索引与速查，子主题各一篇。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [2026 生态基准](#5-2026-生态基准)

## 1. 知识体系导图

```text
感知模块 Perception
├── 01 感知模块全景：Agent 的感官接口        定位 / 被动vs主动 / 与相邻组件分工
├── 02 感知管道：从原始输入到结构化上下文     连接器→装配 / 压缩四法 / 延迟预算
├── 03 文本感知：输入清洗与预处理            清洗 / 去噪 / 感知失败模式
├── 04 视觉感知：图像与 GUI 理解             OmniParser / GUI grounding / 视觉上下文
├── 05 多模态感知：Omni 统一入口             统一流水线 / 时间对齐 / 实时全双工
├── 06 环境与传感器感知                      系统状态 / 可穿戴 / 传感器融合 / 时序
├── 07 主动感知：感知即推理                  active perception / 有界观察 / Perceive 工具
├── 08 上下文装配：感知的产出工程             U 型注意力 / 五失败模式 / 预计算上下文
├── 09 感知评测：grounding 与基准            ScreenSpot / CUA-Suite / 感知失败分类
└── 10 生产实践与面试冲刺                     12 避坑 / 面试题
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 01 | [感知模块全景：Agent 的感官接口](01-感知模块全景：Agent的感官接口.md) | 定位、感知五组件、主动感知趋势 | 全部（地基） |
| 02 | [感知管道：从原始输入到结构化上下文](02-感知管道：从原始输入到结构化上下文.md) | 四段管道、压缩四法族、延迟预算 | Agent 工程师 |
| 03 | [文本感知：输入清洗与预处理](03-文本感知：输入清洗与预处理.md) | 清洗、去噪、噪声/数量实证 | 落地开发者 |
| 04 | [视觉感知：图像与 GUI 理解](04-视觉感知：图像与GUI理解.md) | OmniParser、grounding、视觉上下文 | Agent 工程师 |
| 05 | [多模态感知：Omni 统一入口](05-多模态感知：Omni统一入口.md) | Omni 模型、时间对齐、实时全双工 | 架构师 |
| 06 | [环境与传感器感知](06-环境与传感器感知.md) | 系统状态、可穿戴、传感器融合 | 前沿关注者 |
| 07 | [主动感知：感知即推理](07-主动感知：感知即推理.md) | 按需感知、有界观察、Perceive 工具 | Agent 工程师 |
| 08 | [上下文装配：感知的产出工程](08-上下文装配：感知的产出工程.md) | U 型注意力、五失败模式、预计算 | 架构师 |
| 09 | [感知评测：grounding 与基准](09-感知评测：grounding与基准.md) | ScreenSpot、CUA-Suite、失败分类 | 评测关注者 |
| 10 | [生产实践与面试冲刺](10-生产实践与面试冲刺.md) | 落地清单、12 避坑、面试题 | 面试/上线前 |

## 3. 学习路线推荐

| 路线 | 路径 | 目标 |
|---|---|---|
| 入门（1 天） | 01 → 03 → 08 → 10 | 能设计文本感知与上下文装配 |
| 进阶（1 周） | 01-03 → 04 → 07-08 → 10 | 能接入视觉/GUI 感知与主动感知 |
| 高级（2 周） | 全量 + 05-06 → 09 | 能做 Omni 多模态与感知评测 |

## 4. 核心概念速查

| 概念 | 一句话定义 |
|---|---|
| 感知模块（Perception） | Agent 的感官接口：原始环境输入 → 结构化上下文的管道 |
| 感知管道 | 连接器 → 嵌入 → 特征提取 → 上下文装配 四段式 |
| 上下文装配 | 把感知结果组装成模型看到的提示（Assembly = 一等架构问题） |
| 主动感知（Active Perception） | 按当前推理需求选择性请求信息，而非全量摄入 |
| 感知压缩 | 输入侧 token 优化：transformation/selection/aggregation/resampling 四法族 |
| GUI Grounding | 把屏幕截图映射到可操作元素（坐标/ID）的能力 |
| OmniParser | Microsoft 屏幕解析工具（V2：ScreenSpot Pro 39.5%） |
| 视觉上下文管理 | 高低层分工：紧凑高层上下文 + 专业子 Agent 深入（HierVA 64.2%） |
| Omni 感知 | 统一多模态入口：UI 状态 + 视觉 + 语音单管道进入（X-OmniClaw） |
| 时间对齐 | 语音与视频按时间戳对齐分解为结构化多模态意图 |
| 有界观察 | 主动感知轮次上限（2-3 轮最优，超限引入噪声） |
| U 型注意力 | 注意力两端强中间弱——关键内容放首尾，中间放参考资料 |
| 上下文五失败 | 毒化/分心/混淆/冲突/腐烂——感知装配的失败模式 |
| Knowledge Indicators | 预提取原子化事实单元，Agent 读一句就够（成本 -3.6x，精度 62.5%→91.7%） |
| ScreenSpot Pro | GUI grounding 基准（OmniParser V2 39.5%） |
| CUA-Suite | 计算机使用 Agent 统一基准与训练语料（87 桌面应用，560 万元素标注） |

## 5. 2026 生态基准

> 📅 基准窗口：2026-08。版本与事实以各模块【参考来源】为准。

- **感知定位定型**：Redis 2026 把感知模块定义为五组件之一（感知/推理规划/记忆/行动/反馈）：连接器接入数据源、嵌入向量化、特征提取、上下文装配——"每个提示是被装配的，不是被写的"。
- **主动感知成为主流方向**：Perceive Before Reasoning 预推理感知框架同准确率下 token 减 **57.1%**；AOP-Agent 实证有界观察 **2-3 轮最优**（超限引入噪声）；OmniAgent 视频采样密度随时长自适应（16.9→5.7 轮/段保持准确率）；ToolScope 把 Perceive 做成工具（ACL 2026）。
- **视觉/GUI 感知工程化**：OmniParser V2 在 ScreenSpot Pro 达 39.5%（2026-07 YOLOv9-E 交互区域检测器 + OmniTool 控制 Windows 11 VM）；DRS-GUI（CVPR 2026）训练免费动态区域搜索 +14%；HiViG 视觉标记锚点 +5.8-9.0%；CUA-Suite（ICLR 2026）87 桌面应用、560 万元素标注——grounding 从"静态截图"走向"像素级、因果密集监督"。
- **Omni 统一入口**：NVIDIA Nemotron 3 Nano Omni（2026-04，30B-A3B MoE，吞吐最高 9x，1920×1080 原生输入）与小米 MiMo-V2-Omni（无 omni 税：SWE-Bench/OmniGAIA 达领先水平）——**单一模型统一视觉+音频+语言**取代"ASR+LLM+TTS"拼装。
- **实时感知预算**：2026 多模态框架延迟预算——视觉 112ms（≤4K/30fps）、音频 98ms（VAD 激活）、文本 17ms；端到端 80ms 已有实证；OpenAI Realtime API（WebRTC）全双工是语音 Agent 标配。
- **上下文工程成为学科**：U 型注意力实证（相关文档居中 20 文档测试精度 -20%；GPT-3.5 第 10/20 文档 52.9% < 无文档 56.1%）——**把关键内容埋进中段比不检索更糟**；上下文五失败模式（毒化/分心/混淆/冲突/腐烂）成为装配检查单；Knowledge Indicators 预计算上下文成本 -3.6x、精度 62.5%→91.7%。
- **感知压缩有代价**：激进压缩实证——JSON→TOON/TRON 省 27% token 但精度回归 9-14 点；源码压缩省 42% 但 SWE-bench 50%→38%——**省 token 前先算精度账**（08 篇）。
- **与体系分工**：[Memory 记忆系统](..%2F..%2F..%2FAgent%20子组件专项学习%2FMemory%20记忆系统%2F00-Memory记忆系统总览.md) 管记忆层（工作/长期），本体系管输入处理层（感知管道）；[RAG 检索增强生成](..%2F..%2F..%2F..%2F04-RAG检索增强生成%2F00-RAG知识体系总览.md) 是感知的一种通道（检索），本体系管通用输入管道；[安全护栏 02 输入护栏](..%2F安全护栏%20Guardrails%2F02-输入护栏：预-LLM防线.md) 是感知管道上的拦截点；[Output Parser](..%2F..%2F..%2FAgent%20子组件专项学习%2FOutput%20Parser%20输出解析器%2F00-OutputParser输出解析器总览.md) 管输出侧，感知管输入侧——两端对称。

---

**下一模块**：[01-感知模块全景：Agent 的感官接口](01-感知模块全景：Agent的感官接口.md)

## 参考来源

- [Agentic AI System Components: Building Production-Ready Agents（Redis）](https://redis.io/blog/agentic-ai-system-components/)
- [Perceive Before Reasoning: A Pre-Reasoning Perception Framework（arXiv 2606.03236）](https://arxiv-org.ezproxy.obspm.fr/html/2606.03236v1)
- [X-OmniClaw: A Unified Mobile Agent for Multimodal Understanding（arXiv 2605.05765）](https://huggingface.co/papers/2605.05765)
- [Hierarchical Visual Agent: Managing Contexts in Joint Image-Text Space（ACL 2026 Findings）](https://aclanthology.org/2026.findings-acl.1914/)
- [Microsoft OmniParser（GitHub）](https://github.com/microsoft/OmniParser)
- [NVIDIA Launches Nemotron 3 Nano Omni Model（NVIDIA Blog）](https://blogs.nvidia.com/blog/nemotron-3-nano-omni-multimodal-ai-agents/)
- [Token Compression in the AI Agent Lifecycle（SJTU Media Lab）](https://medialab.sjtu.edu.cn/post/26-07-25-token-compression-in-the-ai-agent-lifecycle/)
- [Context Assembly: Building the Prompt the Model Sees（Redis）](https://redis.io/blog/context-assembly-building-the-prompt-the-model-sees.md)
- [CUA-Suite: Expert Trajectories and Pixel-Precise Grounding（ICLR 2026）](https://iclr.cc/virtual/2026/10012527)
