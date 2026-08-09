# ZCode 知识体系总览

> 智谱 AI（Z.ai）出品的桌面端 Agent 驱动开发环境（ADE），深度适配 GLM-5.2 的官方 AI 编程工作区——注意：**ZCode 是智谱产品，不是字节跳动产品**（字节对应产品为 Trae）。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [关键澄清：ZCode ≠ 字节跳动](#5-关键澄清zcode--字节跳动)

## 1. 知识体系导图

```
ZCode（智谱 AI · Agent 驱动开发环境 ADE）
│
├── 01 产品定位与版本演进      定位：你当项目经理，ZCode 当开发团队
│   ├── ADE vs IDE vs AI 编辑器
│   ├── 版本线：2.13 → 3.0（自研 Agent 内核）→ 3.2.x（子代理）
│   └── 与 GLM-5.2 的深度耦合（1M 上下文）
│
├── 02 安装部署与账号配置      三平台 + 登录 + 首次配置三件事
│   ├── macOS / Windows / Linux(Beta) 安装与踩坑
│   ├── 账号登录与 5 天免费体验
│   └── 连接模型三入口（BigModel / Z.ai / BYOK）
│
├── 03 额度计费与模型接入      GLM Coding Plan 三档 + 额度规则
│   ├── 免费额度 / 三档订阅价格（国内 vs 海外）
│   ├── 高峰期 3 倍扣费 / 150% 配额奖励
│   └── BYOK 多模型接入与 Coding 专用端点
│
├── 04 界面交互与任务工作区    分组式任务面板 + 符号体系
│   ├── 三视图（分组 / 工作区 / 时间线）
│   ├── 快捷符号：@ 文件 # 对话 / 命令 $ 技能 + 附件
│   └── 状态看板 / 内置预览浏览器 / Git 图谱
│
├── 05 Agent 执行模式与任务流  核心：计划 → 执行 → 验证闭环
│   ├── 四种执行模式（确认 / 自动编辑 / 计划 / 完全访问）
│   ├── Goal 目标模式（/goal 命令族）
│   └── 推理强度 / 会话级版本回滚
│
├── 06 多 Agent 协作与子智能体 并行任务 + 子代理分工
│   ├── 分组多任务并发管理
│   ├── 内置子智能体（general-purpose / Explore）
│   └── 远程控制（扫码）/ Bot Channel（微信 / 飞书）
│
├── 07 上下文工程             AGENTS.md + Skills + Commands
│   ├── 两级 AGENTS.md 项目指令
│   ├── Skill 机制（$skill 调用，SKILL.md）
│   └── 上下文管理（/compact、@#/$ 符号降本）
│
├── 08 MCP 扩展与工具生态      三种类型 + 三位置配置 + 导入
│   ├── MCP（stdio / HTTP-SSE / JSON）
│   ├── 官方推荐：zai-mcp-server / web-search-prime / web-reader
│   └── Git 集成 / Release Bot / Zread 项目知识库
│
└── 09 生产实践与竞品对比      避坑清单 + 选型决策
    ├── 12 大避坑点
    ├── vs Claude Code / Cursor / Trae / Codex
    └── 面试速记
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 01 | 产品定位与版本演进 | ADE 概念、产品线、3.0 内核切换、GLM-5.2 耦合 | 入门必读 |
| 02 | 安装部署与账号配置 | 三平台安装、踩坑修复、登录、首次配置、模型连接 | 新手第一步 |
| 03 | 额度计费与模型接入 | 免费体验、订阅三档、额度规则、BYOK、多模型矩阵 | 所有用户 |
| 04 | 界面交互与任务工作区 | 任务面板三视图、快捷键、@#/$+ 符号、状态看板 | 日常高频 |
| 05 | Agent 执行模式与任务流 | 四权限模式、Goal 目标模式、计划-执行-验证闭环 | 核心进阶 |
| 06 | 多 Agent 协作与子智能体 | 并发任务、子代理配置、远程控制、Bot Channel | 进阶/团队 |
| 07 | 上下文工程 | AGENTS.md、Skills、Commands、上下文压缩 | 高阶玩家 |
| 08 | MCP 扩展与工具生态 | MCP 三类型、配置位置、官方工具、Git/Release 自动化 | 高阶玩家 |
| 09 | 生产实践与竞品对比 | 避坑、选型决策树、vs 竞品、面试题 | 面试/落地 |

## 3. 学习路线推荐

**路线一：新手速成（1 天上手）**
01 产品定位 → 02 安装配置 → 04 界面交互 → 跑通第一个任务（参照 02 的计数器示例）

**路线二：效率进阶（1 周）**
05 执行模式与 Goal 模式 → 07 AGENTS.md 与 Skills → 08 MCP 接入 → 06 多 Agent 并行

**路线三：深度用户 / 迁移者（Claude Code / Codex 用户）**
07 上下文工程（对比你的 CLAUDE.md 习惯）→ 08 MCP 导入 → 09 竞品对比 → 03 额度精打细算

## 4. 核心概念速查

| 概念 | 一句话速记 |
|---|---|
| ADE | Agentic Development Environment，Agent 主导的开发环境，区别于传统 IDE |
| ZCode Agent 内核 | 3.0 起自研的 Agent 执行内核，深度适配 GLM-5.2 长程推理与工具调用 |
| GLM-5.2 | 智谱旗舰模型，1M 上下文窗口，ZCode 的默认模型 |
| 执行模式 | 变更前确认 / 自动编辑 / 计划模式 / 完全访问，Shift+Tab 切换 |
| Goal 模式 | 设目标后 Agent 自主迭代直至完成，/goal 命令族管理 |
| 子智能体 | 主 Agent 派生的独立子代理，可配置不同模型，v3.2.0 起 |
| Zread | 自动生成项目结构化文档的智能项目知识库 |
| AGENTS.md | 项目指令文件，Agent 长期遵守的项目约定 |
| Skill | 可复用的完整工作说明，`$skill-name` 调用 |
| Command | 常用提示词快捷指令，`/command-name` 调用 |
| MCP | Model Context Protocol，工具扩展协议，三类型三位置配置 |
| BYOK | Bring Your Own Key，自带 API Key 接入任意兼容模型 |
| Coding Plan | 智谱模型订阅套餐（Lite/Pro/Max），ZCode 内享 150% 配额 |
| 配额/Token | 额度制计费，高峰期（14:00-18:00）3 倍扣费，非高峰 2 倍 |
| Release Bot | 自动处理 changelog、GitHub Release、tag 的发布机器人 |

## 5. 关键澄清：ZCode ≠ 字节跳动

ZCode 是**智谱 AI（Z.ai / bigmodel.cn）**于 2025 年推出的 AI 编程工具，2026 年 6 月发布 3.0 版本后定位为桌面端 ADE。**字节跳动的 AI 编程 IDE 是 Trae**（豆包驱动），两者是竞争关系：

| 维度 | ZCode（智谱） | Trae（字节跳动） |
|---|---|---|
| 形态 | 桌面端 ADE（Agent 主导） | AI 原生 IDE（对话即开发） |
| 核心模型 | GLM-5.2（1M 上下文） | 豆包 Seed Code + DeepSeek 等 |
| 商业模式 | 应用免费 + Coding Plan 订阅/BYOK | 完全免费 |
| 差异化 | 自研 Agent 内核、Goal 模式、私有化生态 | SOLO 智能体、Builder 模式 |

> ⚠️ 目录名为 "Zcode"，本体系全部内容针对**智谱 ZCode**。若想学习字节 Trae，参见同级 `国内外主流的 AI 产品/` 或 `AI编码/05-国产AI编码工具.md` 中的相关章节。

---

## 参考来源

- [ZCode 版本发布与更新（官方 changelog）](https://zcode.z.ai/changelog)
- [智谱 AI 编程工具 ZCode 3.0 版本发布：切换自研 ZCode Agent 内核，深度适配 GLM-5.2（IT之家）](https://www.ithome.com/0/963/985.htm)
- [ZCode 3.0 安装全攻略：下载、配置、跑通第一个任务（CSDN）](https://sgknight.blog.csdn.net/article/details/163299168)
- [GLM Coding Plan & ZCode Explained: Pricing & More (2026)](https://lorphic.com/glm-coding-plan-and-zcode/)
- [Zhipu's Free ZCode Undercuts Claude Code on Price](https://servola.de/journal/zhipu-zcode-glm-5-2-undercuts-claude-code/)
- [ZCode vs Claude Code: The better coding agent in 2026 | Composio](https://composio.dev/content/zcode-vs-claude-code)
- [国产 AI 编程工具实测：ZCode、Trae、MiMo Code 到底选哪个？](https://www.toutiao.com/article/7651836168648196649/)
- [从 IDE 到 ADE：Z Code 全新版本正式上线（智谱官方公众号）](https://mp.weixin.qq.com/s/S1XlaKsGoZGzVzu_RNSjkg)
- [ZCode 3.0 来了，但替代 Claude Code 不只是模型的事（博客园）](https://www.cnblogs.com/youring2/p/21365156)

---

**下一模块**：[01-ZCode概述-产品定位与版本演进](01-ZCode概述-产品定位与版本演进.md)
