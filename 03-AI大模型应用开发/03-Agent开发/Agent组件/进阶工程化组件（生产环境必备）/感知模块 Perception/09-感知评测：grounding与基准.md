# 感知评测：grounding 与基准

> 感知评测 = 回答"Agent 真的看对了吗"：grounding 精度、多模态理解、感知失败定位。2026 主线：**从静态截图评测走向像素级、因果密集的监督数据**（CUA-Suite 560 万元素）；失败诊断从"笼统报错"走向分类学（CUADebug）。本章给感知评测全景。

## 1. 感知评测的三个层面

| 层 | 评测对象 | 代表基准 |
|---|---|---|
| 元素级 | 感知管道产出（解析/提取/grounding） | ScreenSpot Pro、GroundCUA |
| 任务级 | 端到端 Agent 完成度（含感知环节） | OSWorld、MM-BrowserComp、SWE-Bench |
| 组件级 | 单感知组件（OCR/ASR/分类器） | 传统视觉/语音基准 |

> 🎯 核心要点：三层缺一不可——**元素级告诉你"感知哪里错"，任务级告诉你"感知对任务的影响"，组件级告诉你"换哪个组件"**。只测任务级的团队会陷入"整体差但不知道差在哪"。

## 2. Grounding 基准：GUI 感知的度量衡

| 基准 | 内容 | 2026 标杆 |
|---|---|---|
| ScreenSpot | 截图元素定位（文本/图标/组件） | OmniParser V2 **39.5%** |
| ScreenSpot-Pro | 高难变体（乱序/密集界面） | DRS-GUI **+14%** 提升 |
| UI-Vision | 元素 grounding + 布局理解 + 动作预测 | CUA-Suite 整合 |
| GroundCUA | 56K 标注截图、**560 万**元素标注 | 训练 GroundNext 系列 |

> ⚠️ grounding 精度是 GUI Agent 的**硬天花板**——39.5% 意味着每 5 次定位错 3 次；ScreenSpot-Pro 的高难变体说明"真实界面"与"干净截图"差距巨大（04 篇 grounding 三路技术）。

## 3. CUA-Suite：统一训练与评测（ICLR 2026）

| 组件 | 内容 |
|---|---|
| UI-Vision | 元素 grounding/布局/动作预测基准 |
| GroundCUA | 56K 截图 + 560 万元素标注（像素级监督） |
| ActCUA | ~1 万专家任务：连续视频 + 运动学光标轨迹 + 多层推理标注（平均 497 词/步） |

| 发现 | 说明 |
|---|---|
| 像素级监督 | 地面真值是"像素精确"的——不是"元素框" |
| 因果密集 | 每步有推理标注——不是只有最终状态 |
| 现实差距 | 现有基础动作模型在专业桌面应用上**显著挣扎** |

> 🎯 核心要点：CUA-Suite 的范式意义——**感知监督从"结果对错"走向"因果密集"**：要训练/评测"为什么这样感知"，而不是"最终点对了没"。这是感知评测 2026 的分水岭。

## 4. 多模态理解基准（Omni 时代）

| 基准 | 测什么 |
|---|---|
| MOV-Bench | 多跳音视频推理（AOP-Agent 用） |
| OmniGAIA | 通用多模态 Agent 任务（MiMo-V2-Omni 达领先） |
| MM-BrowserComp | 多模态浏览器操作 |
| CharXiv | 图表推理（HierVA 64.2%） |
| OmniVideoBench | 视频理解 |

> 💡 Omni 评测的注意点：**"无 omni 税"要用双轨验证**——既测纯文本/Agent 基准（SWE-Bench），又测多模态基准（OmniGAIA）；只测一个轨道的"全能"是自欺。

## 5. 感知失败诊断：CUADebug（2026）

| 输出 | 内容 |
|---|---|
| 错误分类学 | CUA 特有错误分类（推理/感知/grounding 分层） |
| CUAErrorBench | 人工标注失败基准 |
| 根因分析 Agent | 工具增强：截图级证据 + 目标定位 + 屏幕转场追踪 → 结构化诊断 + 重执行指导 |

```text
感知失败根因分析（示意）：
  症状：点击了错误的按钮
  证据：目标定位验证失败（grounding 错）/ 截图解析漏元素（感知错）
  诊断："屏幕解析将『提交』误识别为『保存』（OCR 混淆）→ 重执行建议：解析层换 OCR 配置"
```

> 🎯 核心要点：**失败分类学是评测到生产的桥梁**——把"Agent 又失败了"升级为"感知层 OCR 混淆率 12%"，改进动作就从"换模型"变成"换 OCR 配置"。没有分类学的感知评测无法指导工程。

## 6. 生产感知评测：指标与闭环

| 指标 | 内容 |
|---|---|
| 感知精度 | 元素级命中率（按类型分：文本/图标/组件） |
| Grounding 成功率 | 动作定位正确率（像素容差内） |
| 感知成本 | 每模态 token/延迟（02 篇预算对照） |
| 失败分布 | 分类学占比（推理/感知/grounding）——趋势监控 |
| 感知-任务归因 | 任务失败中感知环节贡献率 |

> ⚠️ 评测闭环与 Guardrails 08 篇同构：**感知失败样本 → 分类学标注 → 入评测集 → 组件改进 → CI 回归**——没有样本闭环的感知评测是静态报表。

## 7. 常见误区

| 误区 | 真相 |
|---|---|
| "端到端通过就行" | 任务级通过掩盖感知弱点——三层都要测 |
| "元素框够精确" | CUA-Suite 用像素级——元素框掩盖 grounding 误差 |
| "干净截图达标即可" | ScreenSpot-Pro 高难变体证明真实界面差距巨大 |
| "失败=模型问题" | 先分类（CUADebug）——感知/grounding/推理不同药 |
| "Omni 只测多模态" | 双轨验证——SWE-Bench + OmniGAIA 都要 |
| "评测一次管一年" | 样本闭环持续——感知组件在演进 |
| "基准分高=生产好用" | 基准与生产分布不同——生产要自己的 heldout 集 |

## 8. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 感知评测三层？ | 元素级（哪里错）/任务级（影响多大）/组件级（换哪个） |
| ScreenSpot Pro？ | GUI grounding 高难基准——OmniParser V2 39.5% |
| CUA-Suite？ | ICLR 2026：UI-Vision + GroundCUA（560 万元素）+ ActCUA |
| 像素级监督？ | 地面真值像素精确，不是元素框 |
| 因果密集？ | 每步有推理标注——评测"为什么这样感知" |
| MOV-Bench？ | 多跳音视频推理基准 |
| 双轨验证？ | 纯文本/Agent 基准 + 多模态基准都测——防 omni 税自欺 |
| CUADebug？ | 失败分类学 + CUAErrorBench + 根因分析 Agent |
| 感知失败三类？ | 推理/感知/grounding——不同病不同药 |
| 生产指标？ | 感知精度/grounding 成功率/成本/失败分布 |
| 评测闭环？ | 失败样本→分类→评测集→组件改进→CI |
| grounding 的意义？ | GUI Agent 硬天花板——39.5% 意味每 5 次错 3 次 |

---

**下一模块**：[10-生产实践与面试冲刺](10-生产实践与面试冲刺.md)　**返回总览**：[00-感知模块 Perception 总览](00-感知模块Perception总览.md)

## 参考来源

- [CUA-Suite: Expert Trajectories and Pixel-Precise Grounding（ICLR 2026）](https://iclr.cc/virtual/2026/10012527)
- [CUADebug: Diagnosing and Repairing Computer-Use Agent Failures（arXiv 2608.02643）](https://arxiv-org.ezproxy.obspm.fr/html/2608.02643v1)
- [DRS-GUI: Dynamic Region Search for Training-Free GUI Grounding（CVPR 2026）](https://openaccess.thecvf.com/content/CVPR2026/html/Liu_DRS-GUI_Dynamic_Region_Search_for_Training-Free_GUI_Grounding_CVPR_2026_paper.html)
- [Agentic Active Omni-Modal Perception（arXiv 2605.28192）](https://arxiv-org.ezproxy.obspm.fr/html/2605.28192v1)
- [Xiaomi MiMo-V2-Omni](https://mimo.xiaomi.com/mimo-v2-omni)
