# 视觉感知：图像与 GUI 理解

> 视觉感知 = Agent 的"眼睛"：图像理解、图表推理、GUI 界面感知三大战场。2026 主线：**纯视觉 GUI Agent 取代元数据依赖**——OmniParser 把截图解析成结构化元素、DRS-GUI 训练免费区域搜索、视觉上下文管理（HierVA）让长图推理可用。本章给视觉感知完整地图。

## 1. 视觉感知的三大战场

| 战场 | 任务 | 典型输入 |
|---|---|---|
| 图像理解 | 内容描述、物体识别、文档 OCR | 照片、扫描件、PDF |
| 图表推理 | 数值阅读、趋势分析 | 折线图、表格、仪表盘 |
| GUI 感知 | 界面元素定位、动作 grounding | 屏幕截图、Web 页面 |

> 🎯 核心要点：三者的共性难点是**空间 grounding**——"模型看到了什么、它在哪、怎么操作它"。2026 实证：MLLM 在复杂界面里难以直接理解原始数字坐标——grounding 需要专门机制（视觉标记/区域搜索/解析工具）。

## 2. OmniParser：纯视觉 GUI 感知的事实标准

| 项 | 说明 |
|---|---|
| 定位 | Microsoft 屏幕解析工具：UI 截图 → 结构化元素（含坐标/类型/文本） |
| 作用 | 让纯视觉 Agent 有"元素层"感知——动作 grounding 的输入源 |
| V2 成绩 | ScreenSpot Pro grounding 基准 **39.5%** |
| 2026-07 更新 | YOLOv9-E 交互区域检测器 |
| 配套 | OmniTool：用视觉模型控制 Windows 11 VM |

```text
OmniParser 感知链路：
  截图 ──→ 交互区域检测（YOLOv9-E）──→ 元素识别/OCR ──→ 结构化元素列表
       [{"type": "button", "text": "提交", "bbox": [120, 340, 210, 380]}]
  → 元素列表注入上下文 → 模型 grounding 到具体坐标 → 执行动作
```

> 💡 OmniParser 的定位是**感知前置**：它把"看图"变成"读结构化列表"——模型不用自己数像素，但 grounding 精度仍然决定 Agent 上限（V2 的 39.5% 说明还有巨大空间）。

## 3. GUI Grounding：三路技术

| 路线 | 机制 | 2026 代表 | 效果 |
|---|---|---|---|
| 训练方法 | 视觉-语言联合预训练 | SeeClick / ShowUI / TinyClick | 零样本截图 grounding |
| 训练免费搜索 | 人类式感知范围调整 + MCTS | DRS-GUI（CVPR 2026） | ScreenSpot-Pro **+14%** |
| 视觉标记辅助 | 截图注入视觉锚点，模型学习对应关系 | HiViG | 成功率 **+5.8-9.0%** |

> 🎯 核心要点：**grounding 的三条路不互斥**——HiViG 的视觉标记（在截图上画标记帮助模型定位）是最便宜的增量收益：MLLM 对原始坐标不敏感，给它"空间锚点"就能大幅改善（+5.8% Qwen3-VL / +9.0% Gemini-3-Flash）。

## 4. 视觉上下文管理：HierVA（ACL 2026）

| 项 | 说明 |
|---|---|
| 问题 | 长图/多图场景视觉上下文膨胀，模型"注意力稀释" |
| 方案 | 高低层分工：高层 manager 维护紧凑上下文（只保留关键信息），专业 worker 子 Agent 深入推理后返回蒸馏结果 |
| 机制 | 文本/视觉上下文分离管理 + zoom-in 工具（缩放限制视觉上下文范围）+ worker 封装 |
| 效果 | CharXiv 图表推理 **64.2%**（比基线 +1.8-5.3 点） |
| 推广 | 作者论证"限定范围+蒸馏的多模态上下文"原则适用于任何细粒度视觉推理 |

> ⚠️ 与 Tool 05 篇（namespace/tool_search）同源思想：**"给模型的上下文是选择的结果，不是全量"**——视觉侧同样适用：高分辨率全图不是给模型的默认配置，按需 zoom-in 才是。

## 5. 视觉 Agent 的失败诊断：CUADebug（2026）

| 失败类别 | 占比特征 | 诊断手段 |
|---|---|---|
| 推理/控制 | 主导 | 轨迹分析 |
| 感知 | 显著占比 | 截图级证据审查 |
| grounding/交互 | 显著占比 | 目标定位验证、屏幕转场追踪 |

> 💡 CUADebug 的价值：把视觉 Agent 失败从"笼统报错"拆成**分类学 + 根因分析**——感知失败（看错了）与 grounding 失败（找到了错误元素）是不同病，用药不同（09 篇评测展开）。

## 6. 视觉模型选型（2026）

| 模型 | 特点 | 适用 |
|---|---|---|
| Qwen3-VL（32B） | 中文生态、GUI 基准强 | 通用+GUI |
| Gemini-3-Flash | 原生视觉强、吞吐高 | 高吞吐场景 |
| ShowUI | 视觉-语言-动作统一流式 | GUI Agent 专用 |
| SeeClick | 截图专用预训练 | grounding 基线 |
| TinyClick | 轻量单轮（Florence-2-Base） | 边缘/轻量 |
| Nemotron 3 Nano Omni | 原生多模态 + GUI（1920×1080） | Omni 场景（05 篇） |

> 🎯 核心要点：选型权衡是**视觉精度 × 吞吐 × 分辨率支持**——GUI 场景优先"原生 1080p 输入 + grounding 基准好"的模型；图像理解场景看通用基准。

## 7. 常见误区

| 误区 | 真相 |
|---|---|
| "给模型全分辨率截图就行" | 高分辨率全图=token 爆炸+注意力稀释——按需 zoom-in |
| "grounding 是模型的事" | OmniParser/视觉标记/区域搜索都是工程前置——grounding 是系统工程 |
| "视觉 Agent 报错=模型差" | 感知失败与 grounding 失败是不同病——先分类再用药（CUADebug） |
| "一张图一个 token 成本" | 高分辨率图=上千 token——视觉上下文也要治理 |
| "图表推理=OCR" | 图表要数值+空间+语义三重理解——OCR 只是第一步 |
| "视觉标记是作弊" | HiViG 实证 +9%——给模型空间锚点是合法工程手段 |
| "元数据辅助够了" | 2026 趋势是纯视觉——元数据依赖在落地性上输了 |

## 8. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 视觉感知三战场？ | 图像理解/图表推理/GUI 感知 |
| OmniParser 是什么？ | MS 屏幕解析：截图→结构化元素，V2 ScreenSpot Pro 39.5% |
| GUI grounding 三路？ | 训练法（SeeClick）/训练免费搜索（DRS-GUI +14%）/视觉标记（HiViG +9%） |
| HiViG 原理？ | 截图注入视觉锚点——MLLM 对原始坐标不敏感 |
| HierVA 方案？ | 高层紧凑上下文+worker 子 Agent 蒸馏——CharXiv 64.2% |
| zoom-in 工具？ | 限制视觉上下文范围的工具——按需放大而非全图 |
| 视觉失败分类？ | 推理/感知/grounding 三类——不同病不同药 |
| 与 Tool 05 同源思想？ | 上下文是选择的结果不是全量——视觉也适用 |
| 高分辨率截图问题？ | token 爆炸+注意力稀释——需要治理 |
| 模型选型关键维度？ | 视觉精度×吞吐×分辨率支持 |
| CUADebug 产出？ | CUA 错误分类学 + CUAErrorBench 基准 |
| 2026 GUI 趋势？ | 纯视觉（无元数据）Agent + grounding 工程化 |

---

**下一模块**：[05-多模态感知：Omni 统一入口](05-多模态感知：Omni统一入口.md)　**返回总览**：[00-感知模块 Perception 总览](00-感知模块Perception总览.md)

## 参考来源

- [Microsoft OmniParser（GitHub）](https://github.com/microsoft/OmniParser)
- [DRS-GUI: Dynamic Region Search for Training-Free GUI Grounding（CVPR 2026）](https://openaccess.thecvf.com/content/CVPR2026/html/Liu_DRS-GUI_Dynamic_Region_Search_for_Training-Free_GUI_Grounding_CVPR_2026_paper.html)
- [A History-Aware Visually Grounded Critic for Computer Use Agents（arXiv 2606.11078）](https://arxiv-org.ezproxy.obspm.fr/html/2606.11078v1)
- [CUA-Suite: Expert Trajectories and Pixel-Precise Grounding（ICLR 2026）](https://iclr.cc/virtual/2026/10012527)
- [Hierarchical Visual Agent: Managing Contexts in Joint Image-Text Space（ACL 2026 Findings）](https://aclanthology.org/2026.findings-acl.1914/)
- [CUADebug: Diagnosing and Repairing Computer-Use Agent Failures（arXiv 2608.02643）](https://arxiv-org.ezproxy.obspm.fr/html/2608.02643v1)
