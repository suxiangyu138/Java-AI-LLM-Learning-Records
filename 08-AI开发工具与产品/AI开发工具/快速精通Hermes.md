# 快速精通 Hermes Agent

> Nous Research 开源的**自进化** AI 代理框架。"The agent that grows with you" —— 核心卖点是 AI 能从经验中自动学习、创建技能、优化行为，真正越用越聪明。支持 200+ 模型、17+ 通讯平台。

> GitHub：[NousResearch/hermes-agent](https://github.com/NousResearch/hermes-agent) | 最新版本：v0.14.0

---

## 目录

1. [Hermes 是什么](#1-hermes-是什么)
2. [自我进化机制](#2-自我进化机制)
3. [安装与部署](#3-安装与部署)
4. [配置详解](#4-配置详解)
5. [模型与提供商](#5-模型与提供商)
6. [多平台接入](#6-多平台接入)
7. [Skills 自动学习系统](#7-skills-自动学习系统)
8. [Memory 与跨会话记忆](#8-memory-与跨会话记忆)
9. [子代理与并行](#9-子代理与并行)
10. [高级命令](#10-高级命令)
11. [Hermes vs OpenClaw](#11-hermes-vs-openclaw)
12. [最佳实践](#12-最佳实践)

---

## 1. Hermes 是什么

Hermes Agent 由 **Nous Research** 团队于 2026 年 2 月开源，短短三个月就积累了 1.4 万+ GitHub Stars，是 2026 年上半年增长最快的 AI Agent 项目之一。

### 核心定位

```
普通 AI Agent（OpenClaw 等）：
  用户定义规则 → Agent 执行 → 用户改进规则 → Agent 再执行
  人驱动改进循环

Hermes Agent：
  用户说需求 → Agent 执行 → Agent 自己发现规律
  → Agent 自动创建 Skill → 下次自动优化
  自我驱动进化循环
```

### 关键版本演进

| 版本 | 日期 | 关键更新 |
|------|------|----------|
| **开源** | 2026.02 | 首次公开，基础 Agent + Telegram |
| **v0.5.0** | 2026.03.28 | 安全加固，HuggingFace 提供商，Nix flake |
| **v0.7.0** | 2026.04 初 | 长时间运行稳定性大幅提升 |
| **v0.8.0** | 2026.04.08 | MiMo V2 Pro 接入，实时模型切换，Google AI Studio |
| **v0.9.0** | 2026.04.13 | Termux/Android 移动端，微信/WeChat，Web 仪表板 |
| **v0.10.0** | 2026.04.16 | Nous Tool Gateway（搜索/绘图/TTS/浏览器） |
| **v0.11.0** | 2026.04.23 | React/Ink TUI 重写，AWS Bedrock，QQ，GPT-5.5 |
| **v0.14.0** | 2026.05.16 | Native Grok，本地 API Proxy，800+ commits |

### 团队背景

Nous Research 以**开源模型微调**起家。Hermes 模型系列（Hermes 2、Hermes 3）是 Llama/Mistral 架构上最受欢迎的指令微调版本之一。Hermes Agent 是他们在 Agent 框架领域的集大成之作。

---

## 2. 自我进化机制

### 2.1 学习循环

```
┌─────────────────────────────────────────────────┐
│               Hermes 自我进化循环                 │
├─────────────────────────────────────────────────┤
│                                                 │
│   ① 用户提出新任务                                │
│      ↓                                          │
│   ② Hermes 分解并执行                             │
│      ↓                                          │
│   ③ 成功？→ 提取执行模式 → 自动创建 Skill         │
│      失败？→ 分析原因 → 调整策略 → 重试            │
│      ↓                                          │
│   ④ Skill 存入技能库                              │
│      ↓                                          │
│   ⑤ 下次遇到类似任务 → 自动匹配已有 Skill          │
│      ↓                                          │
│   ⑥ 执行过程中继续优化 Skill                      │
│                                                 │
└─────────────────────────────────────────────────┘
```

### 2.2 实际案例

```
用户：帮我每天 9 点抓取 Hacker News 头条，
      翻译成中文，发到飞书群。

Hermes 第 1 次执行：
  1. 写 Python 爬虫 → 运行 → 成功
  2. 调翻译 API → 成功
  3. 调飞书 webhook → 格式不对 → 修正 → 成功
  4. 设置 cron 任务
  5. 自动创建 Skill: "news-translate-broadcast"

第 2 天自动执行时：
  1. 爬虫失败（网站改版）→ 自动调整 CSS 选择器 → 成功
  2. 翻译 API 超时 → 自动加重试 → 成功
  3. 发送成功
  4. 更新 Skill: 添加了"爬虫自适应"和"翻译重试"子步骤

第 30 天：
  这个 Skill 已经非常健壮，基本不需要人工干预。
```

### 2.3 进化范围

```
✅ 会自动进化：
- 执行策略（失败后换方案）
- 错误处理（自动添加重试、降级）
- 格式适配（网站/API 变化时自动调整）
- 效率优化（发现更快路径时自动切换）

❌ 不会自动进化：
- 安全边界（不会自行降低安全检查）
- 核心人格（SOUL.md 不会被自动改写）
- 成本无上限（不会无视预算限制）
```

---

## 3. 安装与部署

### 3.1 环境准备

- Node.js 20+
- 至少一个 LLM API Key
- （可选）Docker
- （可选）Termux（Android 手机）

### 3.2 三种安装方式

```bash
# 方式一：npm 全局安装（推荐）
npm install -g hermes-agent

# 初始化（交互式配置）
hermes init

# 这会引导你：
# 1. 选择默认模型
# 2. 配置 API Key
# 3. 选择消息平台
# 4. 创建 SOUL.md

# 启动
hermes start
```

```bash
# 方式二：Docker
docker run -d \
  --name hermes-agent \
  -v ~/.hermes:/app/data \
  -e ANTHROPIC_API_KEY=sk-ant-xxx \
  -e OPENAI_API_KEY=sk-xxx \
  ghcr.io/nousresearch/hermes-agent:latest
```

```bash
# 方式三：Termux（Android 手机运行）
# 在手机 Termux 中：
pkg update
pkg install nodejs git
npm install -g hermes-agent
hermes init
hermes start --mobile  # 移动端优化模式
```

### 3.3 目录结构

```
~/.hermes/
├── config.yaml            # 主配置
├── SOUL.md                # 人格定义
├── MEMORY.md              # 事件记忆
├── skills/                # 已学技能库
│   ├── auto/              # 自动创建的技能
│   └── custom/            # 手动创建的技能
├── sessions/              # 会话记录
├── logs/                  # 运行日志
└── data/                  # 缓存和临时数据
```

---

## 4. 配置详解

### 4.1 config.yaml 全部选项

```yaml
# config.yaml —— 完整配置参考

# === 基本设定 ===
name: "Hermes"
version: "0.14.0"
locale: zh-CN
timezone: Asia/Shanghai

# === 人格 ===
persona:
  name: "我的 AI 伙伴"
  soul_file: SOUL.md          # 人格定义文件
  language: Chinese            # 回复语言
  style: concise-&-technical   # concise / detailed / casual / technical

# === 默认模型 ===
default_model: claude-sonnet-4-6

# === 模型路由（按任务类型自动切换） ===
model_routing:
  code:
    model: claude-sonnet-4-6
    provider: anthropic
  review:
    model: gpt-4.1
    provider: openai
  fast:
    model: gemini-2.5-flash
    provider: google
  reasoning:
    model: o4-mini
    provider: openai
  creative:
    model: claude-sonnet-4-6
    provider: anthropic

# === 学习设置 ===
learning:
  auto_create_skills: true       # 自动从成功经验创建技能
  auto_improve_skills: true      # 自动优化已有技能
  min_confidence_to_save: 0.7    # 成功率 > 70% 才保存
  max_auto_skills: 100           # 最多自动创建 100 个技能
  review_before_save: true       # 保存前询问

# === 记忆 ===
memory:
  enable_fts5: true              # SQLite 全文搜索
  embedding_model: text-embedding-3-small
  max_memories: 10000
  auto_summarize: true
  summarize_after_turns: 30      # 30 轮后自动摘要

# === 子代理 ===
subagents:
  max_concurrent: 5              # 最多同时 5 个子代理
  default_model: gemini-2.5-flash
  timeout: 300                   # 5 分钟超时

# === 定时任务 ===
cron:
  enabled: true
  max_tasks: 50

# === 网络 ===
network:
  proxy: ""                      # HTTP 代理
  timeout: 120
  user_agent: "Hermes-Agent/0.14"

# === 安全 ===
security:
  require_confirm:
    - shell_exec
    - file_delete
    - git_push
  blocked_tools: []
  admin_ids: ["telegram:123456789"]

# === 成本控制 ===
budget:
  daily_limit: 30              # 美元
  monthly_limit: 300
  alert_at_percent: 80         # 80% 预算时告警

# === Web 仪表板 ===
dashboard:
  enabled: true
  port: 3100
  bind: 127.0.0.1
  password: "${DASHBOARD_PASSWORD}"

# === 日志 ===
logging:
  level: info
  file: logs/hermes.log
  max_size_mb: 50
  keep_days: 30
```

### 4.2 SOUL.md 范例

```markdown
# SOUL.md

## 身份
- 我叫 Hermes，是一个 AI 开发助手
- 我的风格：直接、技术导向、偶尔有点幽默
- 语言：中文回复，代码/术语保留英文

## 我的用户
- 资深 Java/Spring Boot 开发者
- 也写 React、Python
- 偏好：给方案和代码，不废话
- 不喜欢：被问"要不要我帮你×××"——直接做

## 工作原则
- 代码优先于文字解释
- 遇到不确定的就说不知道，不要编
- 涉及安全/钱的决策必须二次确认
- 修改文件前先读，改完显示 diff

## 日常惯例
- 早上 9 点：显示今天待办
- 下午 5 点：询问是否需要日报
- 每次代码提交前：自动做一次审查

## 技术偏好
- Java 17+，不用 Java 8 写法
- 日期时间用 java.time
- 数据库查询优先 JPA Criteria，其次原生 SQL
- 测试框架 JUnit 5 + Mockito
```

---

## 5. 模型与提供商

### 5.1 支持的模型（200+）

Hermes 支持所有主流提供商以及国内模型：

```bash
# 运行中热切换模型
/model list                        # 列出所有可用模型
/model switch claude-sonnet-4-6    # Anthropic
/model switch gpt-4.1              # OpenAI
/model switch gemini-2.5-flash     # Google
/model switch grok-4.3             # xAI（v0.14.0+）
/model switch minimax/m2.7         # MiniMax
/model switch xiaomi/mimo-v2-pro  # 小米 MiMo
/model switch deepseek-v3          # DeepSeek
/model switch ollama/qwen3:32b     # 本地模型
/model switch huggingface/meta-llama-4  # HuggingFace

# 查看当前模型
/model current
```

### 5.2 提供商配置

```yaml
# providers 配置（在 config.yaml 中）
providers:
  anthropic:
    api_key: "${ANTHROPIC_API_KEY}"
    models:
      - claude-sonnet-4-6
      - claude-opus-4-7
      - claude-haiku-4-5

  openai:
    api_key: "${OPENAI_API_KEY}"
    models:
      - gpt-4.1
      - gpt-4o
      - gpt-4o-mini
      - o4-mini
      - o3

  google:
    api_key: "${GEMINI_API_KEY}"
    models:
      - gemini-2.5-flash
      - gemini-2.5-pro

  grok:
    api_key: "${GROK_API_KEY}"
    models:
      - grok-4.3

  minimax:
    api_key: "${MINIMAX_API_KEY}"
    models:
      - m2.7

  xiaomi:
    api_key: "${MIMO_API_KEY}"
    models:
      - mimo-v2-pro

  ollama:
    base_url: http://localhost:11434
    models:
      - qwen3:32b
      - deepseek-coder-v3

  openrouter:
    api_key: "${OPENROUTER_API_KEY}"
    # 通过 OpenRouter 接入 200+ 模型
```

### 5.3 Hermes Proxy（v0.14.0 新功能）

```bash
# 将 Claude Pro / ChatGPT Pro / SuperGrok 的 web 订阅
# 转换为标准 OpenAI API 格式，给 Hermes 本地使用

# 启动代理
hermes proxy start

# 这会在 localhost:8787 启动 OpenAI 兼容接口
# 然后配置：
providers:
  hermes-proxy:
    base_url: http://localhost:8787/v1
    api_key: "not-needed"
    models:
      - claude-pro        # 你 Claude Pro 订阅的模型
      - chatgpt-pro       # ChatGPT Pro 的模型
      - grok-super        # SuperGrok 的模型
```

---

## 6. 多平台接入

### 6.1 支持的平台（17+）

| 平台 | 配置难度 | 适合场景 |
|------|----------|----------|
| **Telegram** | 极低 | 首选平台，功能最完整 |
| **Discord** | 低 | 社区/团队 |
| **Slack** | 中 | 工作场景 |
| **企业微信** | 中 | 国内企业 |
| **飞书** | 中 | 国内企业 |
| **钉钉** | 中 | 国内企业 |
| **微信（个人）** | 高 | 个人日常 |
| **QQ** | 中 | 国内社交 |
| **iMessage** | 高 | Apple 生态 |
| **WhatsApp** | 中 | 海外社交 |
| **Signal** | 低 | 隐私优先 |
| **Email** | 低 | 异步通信 |
| **SMS** | 低 | 无网络环境 |
| **Termux** | - | Android 终端 |

### 6.2 微信配置

```yaml
gateways:
  wechat:
    enabled: true
    # 方式一：企业微信（推荐）
    corp_id: "wwxxx"
    agent_id: "1000002"
    secret: "${WECOM_SECRET}"

    # 方式二：个人微信（需要 puppet 桥接）
    puppet: wechaty
    puppet_token: "${WECHATY_TOKEN}"
```

### 6.3 多平台消息同步

```bash
# 效果：你在 Telegram 上说的话，Hermes 记住；
# 切换到微信继续聊，Hermes 认得你、记得上下文
# 这就是 SOUL.md + MEMORY.md + FTS5 的威力
```

---

## 7. Skills 自动学习系统

### 7.1 手动创建技能

```bash
# 在对话中创建
/skills create "Java Code Review"
# Hermes 会引导你定义技能内容

# 或者手动编辑 YAML 文件
# ~/.hermes/skills/custom/java-review.yaml
```

```yaml
name: java-code-review
version: "1.0"
type: custom
description: Java 代码审查，检查安全、性能、规范

trigger_keywords:
  - 审查
  - code review
  - 检查代码
  - review this

execution:
  model: gpt-4.1   # 审查专用模型
  temperature: 0.2  # 审查需要确定性

steps:
  - analyze:
      prompt: |
        审查以下 Java 代码，按维度评分（1-10）：
        1. 安全性：注入、权限、敏感信息
        2. 正确性：逻辑、边界、null
        3. 性能：算法复杂度、资源使用
        4. 规范：命名、结构、注释
        每个问题标注文件路径和行号。

  - suggest:
      prompt: |
        基于上一步的问题，给出改进后的代码。
        只改有问题的部分，不要重写整个文件。
```

### 7.2 自动创建的技能

```bash
# 当 Hermes 成功完成一个复杂任务后
# 它会自动问：
"我成功完成了这个任务。要将这个工作流保存为技能吗？"

# 同意后，Hermes 自动生成 skill 文件
# 你可以在 ~/.hermes/skills/auto/ 查看
```

```markdown
# 自动技能示例（Hermes 自己生成的）
名称: deploy-spring-boot-to-k8s
触发: "部署到K8s" "上线" "deploy"
步骤:
  1. 运行 mvn clean package
  2. 构建 Docker 镜像
  3. 推送到私有仓库
  4. 更新 k8s deployment
  5. 等待滚动更新完成
  6. 检查健康检查
  7. 发送部署通知到 Slack
```

### 7.3 技能管理

```bash
/skills list                    # 列出所有技能
/skills show 3                  # 查看第 3 号技能详情
/skills improve 3               # 手动触发技能优化
/skills delete 5                # 删除技能
/skills disable 2               # 禁用但保留
/skills enable 2                # 重新启用
/skills export 3 > my-skill.yaml  # 导出分享

# 技能状态
/skills stats                   # 查看技能使用频率/成功率
```

---

## 8. Memory 与跨会话记忆

### 8.1 三层记忆架构

```
SOUL.md（人格层）
- 手写，定义你是谁、用户是谁、你的行为准则
- 不会自动修改
- 约 2~5KB

MEMORY.md（事件层）
- 自动记录重要事件、决策、用户偏好
- Hermes 自动维护
- 用 FTS5 全文搜索 + LLM 摘要检索

短期上下文（对话层）
- 当前对话的前 N 轮
- 超过窗口 70% 自动压缩
```

### 8.2 记忆搜索

```bash
# 直接问 Hermes（它自动检索记忆）
/memory "上周我们讨论的那个部署方案是什么？"
/memory "我之前说过不喜欢什么代码风格？"
/memory "那个 API Key 是怎么配置的？"

# Hermes 会：
# 1. FTS5 搜索 MEMORY.md
# 2. LLM 从搜索结果中提取最相关内容
# 3. 用自然语言回答
```

### 8.3 手动管理记忆

```bash
/memory add "用户偏好：数据库迁移用 Flyway，不用 Liquibase"
/memory search "Flyway"
/memory delete 42       # 删除第 42 条记忆
/memory summary         # 查看记忆概要
/memory export          # 导出全部记忆
```

---

## 9. 子代理与并行

### 9.1 子代理概念

```bash
# 当任务可以并行分解时，Hermes 自动派生子代理
"分析这 3 个微服务各自的性能瓶颈"
# → 自动创建 3 个子代理，并行分析
# → 每个子代理独立运行，有自己的上下文
# → 主代理收集结果并汇总
```

### 9.2 手动词用子代理

```bash
# 显式创建子代理
/subagent "分析 UserService 的性能瓶颈"

# 指定模型的子代理
/subagent --model o3 "审查 AuthService 的安全漏洞"

# 列出活跃的子代理
/subagent list

# 查看子代理结果
/subagent result 3
```

### 9.3 实战：代码审查 + 测试生成并行

```bash
# 用户一条命令
"审查 UserService 并生成测试"

# Hermes 自动并行：
# 子代理 1（claude-sonnet-4-6）：审查代码质量
# 子代理 2（gpt-4.1）：审查安全性
# 子代理 3（gemini-2.5-flash）：生成 JUnit 测试
# 主代理：汇总审查报告 + 测试代码 → 输出
```

---

## 10. 高级命令

### 10.1 命令全表

```bash
# 模型
/model list              # 列出模型
/model switch <name>     # 切换
/model current           # 当前模型
/model compare <a> <b>   # 对比两个模型

# 技能
/skills create <name>    # 创建
/skills list             # 列出
/skills improve <id>     # 优化
/skills import <file>    # 导入

# 记忆
/memory search <query>   # 搜索
/memory add <text>       # 添加
/memory summary          # 概要

# 任务
/cron list               # 定时任务列表
/cron create "<desc>"    # 创建（自然语言）
/cron delete <id>        # 删除

# 子代理
/subagent "<task>"       # 创建子代理
/subagent list           # 列出

# 平台
/platforms list          # 已接入平台
/platforms status        # 连接状态

# 会话
/session save            # 保存当前会话
/session load <name>     # 加载
/session list            # 列出

# 系统
/status                  # 系统状态
/stats                   # 使用统计
/config show             # 查看配置
/config set <k> <v>      # 修改配置
/logs                    # 查看日志
/dashboard               # 打开 Web 仪表板
/restart                 # 重启
/update                  # 检查更新
```

### 10.2 定时任务

```bash
# 自然语言创建（不需要写 cron 表达式）
/cron create "每天早上 8 点提醒我今天的会议"
/cron create "每周五下午 4 点生成周报发飞书"
/cron create "每 30 分钟检查 GitLab CI 状态"
/cron create "每月 1 号统计上月 API 费用"

# Hermes 会自动转换为 cron 表达式并执行
```

### 10.3 Web 仪表板

```bash
# 启动仪表板
hermes dashboard

# 浏览器打开 http://localhost:3100
# 功能：
# - 实时对话记录
# - 技能使用频次图表
# - 成本追踪
# - 记忆图谱
# - 系统健康检查
```

---

## 11. Hermes vs OpenClaw

### 11.1 维度对比

| 维度 | Hermes Agent | OpenClaw |
|------|-------------|----------|
| **开源时间** | 2026.02 | 2025.11 |
| **Stars** | 1.4 万+ | 36 万+ |
| **成熟度** | 快速迭代中 | 相对成熟稳定 |
| **核心差异** | **自我进化**：自动学习创建技能 | **稳定性**：可靠的手动配置工作流 |
| **技能来源** | Agent 自动生成 + 手动创建 | ClawHub 市场 + 手动创建 |
| **技能数量** | 动态增长（自动化） | 5700+（人工上传） |
| **模型支持** | 200+（含国内全系） | 主流模型 |
| **平台支持** | 17+（含微信/QQ/钉钉/飞书） | 12+ |
| **学习成本** | 较高（理念新、功能多） | 中等 |
| **适合谁** | 愿意尝试前沿技术 | 需要稳定可靠 |
| **稳定性** | 快速迭代中偶有 bug | 相对稳定 |
| **创新性** | 自进化机制是行业首创 | Hub-and-Spoke 架构经典 |
| **生态** | Nous Portal（400+模型） | ClawHub（5700+技能） |

### 11.2 选型建议

```
选 Hermes 如果你：
- 对"自我进化"这个概念感兴趣
- 需要微信/QQ/钉钉等国内平台
- 希望 AI 长期积累经验、越来越懂你
- 技术探索型，能接受快速迭代中的小问题

选 OpenClaw 如果你：
- 需要现在就能稳定跑的生产级方案
- 依赖 ClawHub 5700+ 现成技能
- 不想花精力在"教 AI 学习"上
- 追求可预测、可控制的行为
```

---

## 12. 最佳实践

### 12.1 起步路线

```
第 1 天：安装 + 接 Telegram + 随便聊天
第 2 天：写 SOUL.md（定义清楚你是谁）
第 3 天：让 Hermes 帮你做 3 件事，观察它怎么学
第 4 天：配置微信/飞书，在多个平台测试
第 5 天：创建 2 个手动 Skill，对比自动 Skill
第 1 周：调整模型路由，控制成本
第 2 周：启用 cron 定时任务
```

### 12.2 写好 SOUL.md 的诀窍

```markdown
写完 SOUL.md 后，问自己：
□ Hermes 看完知道自己的角色吗？
□ 知道用户是谁、用什么技术、什么风格吗？
□ 知道什么该做、什么不该做吗？
□ 知道什么情况下需要二次确认吗？
□ 有没有日常惯例可以写进去？

好的 SOUL.md 不是一次性写完的。
前两周每天改一点，慢慢打磨。
```

### 12.3 成本管理

```yaml
# 三层降本策略
# 1. 默认用最便宜的
default_model: gemini-2.5-flash

# 2. 只在特定任务升级
model_routing:
  code: claude-sonnet-4-6      # 代码用最贵的
  review: gpt-4.1              # 审查用中档
  fast: gemini-2.5-flash       # 聊天用最便宜的

# 3. 硬性预算
budget:
  daily_limit: 20              # 每天最多 $20
  monthly_limit: 200           # 每月最多 $200
  alert_at_percent: 80         # 80% 告警
```

### 12.4 安全注意事项

```
- Hermes 会自己写代码并执行，沙箱隔离是必须的
- 不要给 Hermes sudo 权限
- 敏感操作（git push / deploy / rm）必须设置 require_confirm
- 公网部署必须加 HTTPS + 认证
- 定期检查自动创建的 Skill，确保没有危险操作
- 微信个人号方案不稳定（有封号风险），企业微信更靠谱
```

---

> **参考资源**
> - GitHub: [NousResearch/hermes-agent](https://github.com/NousResearch/hermes-agent)
> - Nous Portal: [portal.nousresearch.com](https://portal.nousresearch.com/)
> - Nous Research: [nousresearch.com](https://nousresearch.com/)
> - Hermes 模型系列: [HuggingFace/NousResearch](https://huggingface.co/NousResearch)
