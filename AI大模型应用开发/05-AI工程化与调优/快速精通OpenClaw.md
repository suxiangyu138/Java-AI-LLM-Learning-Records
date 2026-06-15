# ⚙️ 快速精通 OpenClaw

> **核心摘要**：OpenClaw 是 36 万+ GitHub Stars 的开源个人 AI 代理框架，将 AI 从"对话工具"升级为"7x24 自主执行的数字助理"。本文涵盖核心架构（Hub-and-Spoke）、安装部署、配置详解、技能系统（ClawHub 5700+ 技能）、三层记忆、安全加固与最佳实践。

> **前置阅读**：[[快速精通-GPT-Gemini-OpenCode-OpenClaw-Hermes]]、[[OpenClaw 核心知识点]]

---

## 目录

1. [OpenClaw 是什么](#1-openclaw-是什么)
2. [核心架构](#2-核心架构)
3. [安装与部署](#3-安装与部署)
4. [配置详解](#4-配置详解)
5. [Skills 技能系统](#5-skills-技能系统)
6. [Memory 三层记忆](#6-memory-三层记忆)
7. [高级功能](#7-高级功能)
8. [安全加固](#8-安全加固)
9. [最佳实践](#9-最佳实践)

---

## 1. OpenClaw 是什么

OpenClaw 是一个**个人 AI 代理运行时**，由奥地利开发者 Peter Steinberger 于 2025 年 11 月创建。核心思路：将 AI 对话扩展到 Telegram、Slack、微信等所有日常通讯平台上，实现 24x7 自主运行。

### 与传统 AI 对话的根本区别

```
传统 AI：你打开网页 → 你问 → AI 答 → 对话结束 = 忘了
OpenClaw：AI 持续运行 → 主动感知 → 主动执行 → 跨会话记忆
```

### 关键时间线

| 时间 | 事件 |
|---|---|
| 2025.11 | Peter Steinberger 以 Clawdbot 之名发布 |
| 2026.01 | 正式定名 OpenClaw |
| 2026.02 | 创始人加入 OpenAI，移交开源基金会 |
| 2026.03 | v3.7 ACP 全链路指令溯源 |
| 2026.04 | GitHub 超 36 万 Stars |

---

## 2. 核心架构

```
Gateway（网关层）: Telegram · Slack · 微信 · 飞书 · 钉钉 · Discord
Agent（代理层）: 意图理解 → 任务规划 → 执行调度（Lane Queue）
Skills（技能层）: ClawHub 5700+ 技能 · 自定义技能 · 定时任务
Memory（记忆层）: SOUL.md（人格） + 短期上下文 + MEMORY.md（事件）
LLM 后端（可插拔）: Claude · GPT · Gemini · DeepSeek · 本地模型
```

### 三项核心技术

- **Hub-and-Spoke 架构**：各模块松耦合，新增平台/技能/模型不影响核心
- **Lane Queue**：每会话独立执行通道，解决多用户并发状态不一致
- **可插拔上下文引擎（v3.8+）**：插件可接管上下文管理，如 lossless-claw

---

## 3. 安装与部署

### 3.1 前置条件

- Node.js 20+
- 至少一个 LLM API Key
- （可选）Docker

### 3.2 部署方式

```bash
# Docker（推荐）
docker run -d --name openclaw \
  -v ~/.openclaw:/app/data \
  -e ANTHROPIC_API_KEY=sk-ant-xxx \
  -p 3000:3000 \
  ghcr.io/openclaw/openclaw:latest

# Node.js 直接运行
git clone https://github.com/openclaw/openclaw.git
cd openclaw && npm install
cp .env.example .env  # 填写 API Key
npm run build && npm run start

# 一键脚本
curl -fsSL https://get.openclaw.ai | bash
```

### 3.3 Docker Compose 生产部署

```yaml
version: '3.8'
services:
  openclaw:
    image: ghcr.io/openclaw/openclaw:latest
    restart: unless-stopped
    volumes:
      - ./data:/app/data
      - ./config.yaml:/app/config.yaml
    environment:
      - ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}
      - TELEGRAM_BOT_TOKEN=${TELEGRAM_BOT_TOKEN}
    ports:
      - "3000:3000"
```

---

## 4. 配置详解

### 4.1 核心配置

```yaml
persona:
  name: "小C"
  role: "全栈工程师 + 项目经理助理"
  tone: "专业但不刻板，用中文"

models:
  default:  { provider: anthropic, model: claude-sonnet-4-6 }
  fast:     { provider: google, model: gemini-2.5-flash }
  reasoning:{ provider: openai, model: o4-mini }

skills:
  enabled: [code-review, daily-report, web-search, translate]

gateways:
  telegram:
    enabled: true
    token: "${TELEGRAM_BOT_TOKEN}"

limits:
  max_cost_per_day: 50
  rate_limit:
    messages_per_minute: 20
```

### 4.2 环境变量

```bash
# LLM API Keys
ANTHROPIC_API_KEY=sk-ant-xxx
OPENAI_API_KEY=sk-proj-xxx
GEMINI_API_KEY=AIza...

# 平台 Tokens
TELEGRAM_BOT_TOKEN=7483xxx:AAHxxx
```

---

## 5. Skills 技能系统

### 5.1 ClawHub 技能市场

```bash
# 搜索、安装技能
openclaw skill search "code review"
openclaw skill install clawhub/advanced-code-review
openclaw skill list
openclaw skill remove clawhub/old-skill
```

> **注意**：约 12% 社区技能含恶意代码，安装前审查来源，只用高下载量知名技能。

### 5.2 编写自定义技能

```yaml
# skills/daily-standup.yaml
name: daily-standup
trigger:
  type: cron
  cron: "0 9 * * 1-5"      # 工作日早 9 点
  timezone: Asia/Shanghai
steps:
  - name: ask-updates
    platform: slack
    channel: "#team-standup"
    message: "每日站会！请回复你的更新：1. 昨天做了什么？2. 今天计划？3. 阻碍？"
    wait_for_replies: true
    timeout: 900

  - name: summarize
    prompt: "将以下站会回复汇总为简洁的团队日报"

  - name: post-summary
    platform: slack
    channel: "#team-general"
    message: "{summary}"
```

### 5.3 消息平台集成

| 平台 | 配置难度 | 说明 |
|---|---|---|
| Telegram | 极低 | 推荐首选，@BotFather 创建 |
| 飞书 | 中 | 企业常用 |
| 钉钉 | 中 | 国内企业 |
| 企业微信 | 中 | 国内企业 |
| Discord | 低 | 社区场景 |

---

## 6. Memory 三层记忆

### 6.1 SOUL.md — 长期人格

```markdown
# SOUL.md
## 我的身份
- 名字：小C
- 角色：全栈工程师
- 风格：直接、技术导向

## 我的主人
- 技术栈：Java Spring Boot + React + PostgreSQL
- 偏好：直接给方案和代码，不废话

## 我的原则
- 代码优先于解释
- 涉及安全时务必谨慎
- 回复用中文，技术术语保留英文
```

### 6.2 MEMORY.md — 事件与决策

自动记录重要偏好和团队决策，如"数据库迁移使用 Flyway"、"API 错误码统一用 ResultCode 枚举"等。

### 6.3 上下文自动管理

自动压缩超过 50 轮的对话：前 10 轮生成摘要，中间保留关键决策，后 20 轮保留原文。

---

## 7. 高级功能

### 7.1 定时任务

```bash
"每天早上 8 点发今日待办"     → 0 8 * * *
"每周五下午 5 点自动生成本周总结" → 0 17 * * 5
```

### 7.2 子代理委派

大任务自动拆分为子代理并行执行，例如"分析这 3 个微服务"自动创建 3 个子代理。

### 7.3 Webhook 触发

支持 GitHub / GitLab / Jira 等外部 Webhook 触发技能，如 PR 创建时自动审查代码。

---

## 8. 安全加固

### 8.1 必须配置

```yaml
security:
  admin_ids: ["telegram:YOUR_ID"]     # 仅管理员可执行敏感操作
  require_confirmation_for:
    - shell_exec
    - file_delete
    - git_push
    - deploy
  url_whitelist: ["api.github.com", "api.openai.com"]
```

### 8.2 公网部署

```nginx
server {
    listen 443 ssl;
    server_name claw.yourdomain.com;
    location / {
        proxy_pass http://127.0.0.1:3000;
        auth_basic "Restricted";
        auth_basic_user_file /etc/nginx/.htpasswd;
    }
}
```

### 8.3 已知漏洞

- **CVE-2026-25253**：高危 RCE 漏洞 (CVSS 8.8)，升级到最新版修复
- **ClawHub 供应链**：约 12% 技能含恶意代码，审查后再安装

---

## 9. 最佳实践

### 9.1 起步路线

```
第 1 周：只接 Telegram，用默认配置
第 2 周：添加 2~3 个自定义技能
第 3 周：配置 SOUL.md 和记忆系统
第 4 周：根据日志和成本调整模型策略
```

### 9.2 成本控制

```yaml
models:
  default: gemini-2.5-flash    # 主力用最便宜的
  upgrade_triggers:
    - keyword: ["code review"]
      model: claude-sonnet-4-6
limits:
  max_cost_per_day: 10
```

---

## 核心要点回顾

- OpenClaw 是 24x7 运行的个人 AI 代理框架，支持 12+ 通讯平台
- 核心架构：Hub-and-Spoke 松耦合 + Lane Queue 并发控制
- ClawHub 市场提供 5700+ 社区技能，但需注意供应链安全
- 三层记忆：SOUL.md（人格） + MEMORY.md（事件） + 短期上下文
- 安全第一：配置管理员白名单、命令确认、Nginx 反向代理

## 参考资料

1. OpenClaw GitHub：https://github.com/openclaw/openclaw
2. ClawHub 技能市场：https://clawhub.ai
3. OpenClaw 文档：https://docs.openclaw.ai
4. OpenClaw Docker 镜像：https://github.com/openclaw/openclaw/pkgs/container/openclaw
