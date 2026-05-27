# 快速精通 OpenClaw

> 36 万+ GitHub Stars 的开源个人 AI 代理框架。将 AI 从"对话工具"升级为"7×24 自主执行的数字助理"。最早由奥地利开发者 Peter Steinberger（2025 年 11 月）创建，是 2026 年最火的 AI Agent 项目。俗称"**龙虾**"。

> GitHub：[openclaw/openclaw](https://github.com/openclaw/openclaw)

---

## 目录

1. [OpenClaw 是什么](#1-openclaw-是什么)
2. [核心架构](#2-核心架构)
3. [安装与部署](#3-安装与部署)
4. [配置详解](#4-配置详解)
5. [消息平台集成](#5-消息平台集成)
6. [Skills 技能系统](#6-skills-技能系统)
7. [Memory 三层记忆](#7-memory-三层记忆)
8. [高级功能](#8-高级功能)
9. [安全加固](#9-安全加固)
10. [生态与衍生](#10-生态与衍生)
11. [最佳实践](#11-最佳实践)

---

## 1. OpenClaw 是什么

OpenClaw 是一个**个人 AI 代理运行时**。它的核心思路：把你和 AI 的对话从一个聊天框，扩展成能够在 Telegram、Slack、微信等所有日常通讯平台上 24×7 自主运行的智能体。

### 它和 ChatGPT / Claude 的根本区别

```
传统 AI 对话：           OpenClaw：
你打开网页              AI 在你手机/电脑上持续运行
你问 → AI 答            AI 主动感知、主动执行
对话结束 = 忘了         跨会话持续记忆
每次从头开始            越用越了解你
```

### 关键时间线

| 时间 | 事件 |
|------|------|
| 2025.11 | Peter Steinberger 以 Clawdbot 之名发布 |
| 2026.01 | 正式定名 OpenClaw，腾讯云/阿里云支持 |
| 2026.02 | 创始人加入 OpenAI，项目移交开源基金会 |
| 2026.03 | v3.7 发布，引入 ACP 全链路指令溯源 |
| 2026.04 | GitHub 超 36 万 Stars |

---

## 2. 核心架构

```
┌──────────────────────────────────────────────────┐
│                  Gateway（网关）                    │
│   Telegram · Slack · Discord · 飞书 · 钉钉 · 微信   │
│      WhatsApp · iMessage · Signal · Email ...      │
├──────────────────────────────────────────────────┤
│                  Agent（代理）                      │
│   ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│   │ 意图理解  │ │ 任务规划  │ │ 执行调度  │          │
│   └──────────┘ └──────────┘ └──────────┘          │
│   Lane Queue 并发控制 —— 每会话独立执行通道         │
├──────────────────────────────────────────────────┤
│               Skills（技能层）                      │
│   ClawHub 市场 5700+ 技能 · 自定义技能 · 定时任务    │
├──────────────────────────────────────────────────┤
│               Memory（记忆层）                      │
│   SOUL.md（人格） + 短期上下文 + MEMORY.md（事件）  │
├──────────────────────────────────────────────────┤
│              LLM 后端（可插拔）                      │
│   Claude · GPT · Gemini · DeepSeek · 本地模型      │
└──────────────────────────────────────────────────┘
```

### 三项核心技术

**Hub-and-Spoke 架构**：以 Gateway 为中心，各模块松耦合，新增平台/技能/模型不影响核心。

**Lane Queue**：每个会话绑定唯一执行通道，天然解决多用户并发下的状态不一致。

**可插拔上下文引擎（v3.8+）**：允许第三方插件接管上下文管理，例如 lossless-claw 实现永不丢上下文。

---

## 3. 安装与部署

### 3.1 前置条件

- Node.js 20+
- 至少一个 LLM 的 API Key（如 Anthropic / OpenAI）
- （可选）Docker

### 3.2 三种部署方式

```bash
# 方式一：Docker（推荐——隔离好，升级快）
docker run -d \
  --name openclaw \
  -v ~/.openclaw:/app/data \
  -e ANTHROPIC_API_KEY=sk-ant-xxx \
  -e OPENAI_API_KEY=sk-xxx \
  -p 3000:3000 \
  ghcr.io/openclaw/openclaw:latest

# 查看日志
docker logs -f openclaw

# 升级
docker pull ghcr.io/openclaw/openclaw:latest
docker stop openclaw && docker rm openclaw
# 重新 run
```

```bash
# 方式二：Node.js 直接运行（方便调试和二次开发）
git clone https://github.com/openclaw/openclaw.git
cd openclaw
npm install
cp .env.example .env

# 编辑 .env，至少填一个 API Key
# ANTHROPIC_API_KEY=sk-ant-xxx
# OPENAI_API_KEY=sk-xxx
# TELEGRAM_BOT_TOKEN=xxx

npm run build
npm run start
```

```bash
# 方式三：一键脚本（最快上手）
curl -fsSL https://get.openclaw.ai | bash
# 交互式引导设置
```

### 3.3 守护进程（生产必配）

```bash
# systemd 服务（Linux）
sudo cat > /etc/systemd/system/openclaw.service << 'EOF'
[Unit]
Description=OpenClaw AI Agent
After=network.target

[Service]
Type=simple
User=openclaw
WorkingDirectory=/home/openclaw/openclaw
Environment=NODE_ENV=production
ExecStart=/usr/bin/npm start
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl enable openclaw
sudo systemctl start openclaw

# Docker Compose（任何平台）
# docker-compose.yaml
```

```yaml
version: '3.8'
services:
  openclaw:
    image: ghcr.io/openclaw/openclaw:latest
    container_name: openclaw
    restart: unless-stopped
    volumes:
      - ./data:/app/data
      - ./config.yaml:/app/config.yaml
    environment:
      - ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - TELEGRAM_BOT_TOKEN=${TELEGRAM_BOT_TOKEN}
    ports:
      - "3000:3000"
```

---

## 4. 配置详解

### 4.1 完整配置示例

```yaml
# config.yaml —— OpenClaw 的完整配置文件

# === 人格设定 ===
persona:
  name: "小 C"
  role: |
    我是一个全栈工程师 + 项目经理助理。
    风格直接、技术性、偶尔幽默。
  tone: "专业但不刻板，用中文，代码术语保留英文"
  avatar: "🤖"  # 可选，在各平台的头像

# === 模型配置 ===
models:
  # 默认模型（大部分任务）
  default:
    provider: anthropic
    model: claude-sonnet-4-6
    temperature: 0.7
    max_tokens: 4096

  # 快速模型（简单查问/分类）
  fast:
    provider: google
    model: gemini-2.5-flash
    temperature: 0.3
    max_tokens: 1024

  # 推理模型（复杂分析/代码审查）
  reasoning:
    provider: openai
    model: o4-mini
    max_completion_tokens: 10000

# === 技能 ===
skills:
  enabled:
    - code-review        # 代码审查
    - daily-report       # 日报生成
    - web-search         # 联网搜索
    - github-helper      # GitHub 辅助
    - translate          # 翻译

  # 自定义技能路径
  custom_path: ./my-skills/

# === 记忆 ===
memory:
  long_term: SOUL.md             # 持久的"人格和偏好"
  episodic: MEMORY.md            # 事件和决策
  working_memory_size: 50        # 保留最近 N 轮对话
  auto_save_interval: 300         # 每 5 分钟自动保存

# === 平台 ===
gateways:
  telegram:
    enabled: true
    token: "${TELEGRAM_BOT_TOKEN}"

  slack:
    enabled: false
    token: "${SLACK_BOT_TOKEN}"
    signing_secret: "${SLACK_SIGNING_SECRET}"
    app_token: "${SLACK_APP_TOKEN}"

  discord:
    enabled: false
    token: "${DISCORD_TOKEN}"

# === 限制 ===
limits:
  max_tokens_per_request: 16000
  max_tokens_per_day: 1000000
  max_cost_per_day: 50            # 美元
  max_concurrent_requests: 5
  rate_limit:
    messages_per_minute: 20
    messages_per_hour: 200

# === 安全 ===
security:
  admin_ids: ["telegram:123456789"]  # 管理员，可以执行敏感操作
  blocked_commands: []               # 阻止的命令列表
  url_whitelist: []                  # 可访问的 URL 白名单
  require_confirmation_for:
    - file_delete
    - shell_exec
    - git_push
    - deploy

# === 系统 ===
system:
  timezone: "Asia/Shanghai"
  log_level: info
  data_dir: ./data
```

### 4.2 环境变量

```bash
# .env 文件
# —— LLM API Keys ——
ANTHROPIC_API_KEY=sk-ant-xxx
OPENAI_API_KEY=sk-proj-xxx
GEMINI_API_KEY=AIza...
DEEPSEEK_API_KEY=sk-xxx

# —— 平台 Tokens ——
TELEGRAM_BOT_TOKEN=7483xxx:AAHxxx
DISCORD_TOKEN=MTE0NjYx...
SLACK_BOT_TOKEN=xoxb-...
SLACK_SIGNING_SECRET=abc123
SLACK_APP_TOKEN=xapp-...

# —— 其他服务 ——
GITHUB_TOKEN=ghp_xxx
SERPER_API_KEY=xxx  # 联网搜索

# —— 安全 ——
ENCRYPTION_KEY=your-random-key-here  # 加密存储
```

---

## 5. 消息平台集成

### 5.1 Telegram（最推荐）

```bash
# 1. 找 @BotFather 创建 Bot
# 2. 获取 Token
# 3. 配置 webhook 或 polling

# config.yaml
gateways:
  telegram:
    enabled: true
    token: "${TELEGRAM_BOT_TOKEN}"
    mode: polling        # polling / webhook
    webhook_url: ""      # webhook 模式时填写
    allowed_users: []    # 空 = 所有人可用；填 ID = 白名单
```

### 5.2 飞书

```yaml
gateways:
  feishu:
    enabled: true
    app_id: "cli_xxx"
    app_secret: "${FEISHU_APP_SECRET}"
    verification_token: "${FEISHU_VERIFICATION_TOKEN}"
    encrypt_key: ""     # 如果开启了加密
```

### 5.3 钉钉

```yaml
gateways:
  dingtalk:
    enabled: true
    app_key: "dingxxx"
    app_secret: "${DINGTALK_APP_SECRET}"
    robot_code: "dingxxx"
```

### 5.4 微信（企业微信）

```yaml
gateways:
  wecom:
    enabled: true
    corp_id: "wwxxx"
    agent_id: "1000002"
    secret: "${WECOM_SECRET}"
    token: "${WECOM_TOKEN}"
    encoding_aes_key: "${WECOM_AES_KEY}"
```

### 5.5 Discord

```yaml
gateways:
  discord:
    enabled: true
    token: "${DISCORD_TOKEN}"
    guild_ids: []       # 限制哪些服务器可用
    allowed_roles: []   # 限制哪些角色可用
```

### 5.6 多平台消息路由

```python
# 效果演示：你在 Telegram 上对 OpenClaw 说
"每天早上 9 点在 Slack #general 频道发今日站会提醒"

# OpenClaw 会：
# 1. 通过 Telegram 接收命令
# 2. 理解意图：创建定时任务
# 3. 设置 cron：0 9 * * 1-5
# 4. 每天早上 9 点，通过 Slack 网关发消息到 #general
```

---

## 6. Skills 技能系统

### 6.1 ClawHub 技能市场

ClawHub 是 OpenClaw 的社区技能市场，收录 5700+ 技能。

```bash
# 搜索技能
openclaw skill search "code review"

# 安装技能
openclaw skill install clawhub/advanced-code-review

# 查看已安装
openclaw skill list

# 卸载
openclaw skill remove clawhub/old-skill
```

### 6.2 编写自定义技能

```yaml
# skills/daily-standup.yaml
name: daily-standup
version: "1.0"
description: 自动在 Slack 发起每日站会，收集成员更新并汇总
author: "your-name"

# 触发方式
trigger:
  type: cron                # cron / command / mention / webhook
  cron: "0 9 * * 1-5"      # 工作日早 9 点
  timezone: Asia/Shanghai

# 技能逻辑
steps:
  - name: ask-updates
    platform: slack
    channel: "#team-standup"
    message: |
      🤖 每日站会时间！请回复你的更新：
      1. 昨天完成了什么？
      2. 今天计划做什么？
      3. 有什么阻碍？
    wait_for_replies: true
    timeout: 900  # 等 15 分钟收集回复

  - name: summarize
    prompt: |
      将以下站会回复汇总为简洁的团队日报。
      格式：每人一段（姓名 + 3 点），最后列出需要关注的风险项。

      回复内容：
      {replies}

  - name: post-summary
    platform: slack
    channel: "#team-general"
    message: "{summary}"

  - name: save-to-notion
    type: webhook
    url: "https://api.notion.com/v1/pages"
    method: POST
    body:
      parent: { database_id: "xxx" }
      properties:
        Date: { date: { start: "{today}" } }
        Summary: { rich_text: [{ text: { content: "{summary}" } }] }
```

```yaml
# skills/github-pr-review.yaml
name: github-pr-review
version: "1.0"
description: 当有新 PR 时自动进行 AI 代码审查

trigger:
  type: webhook
  source: github

steps:
  - name: fetch-diff
    command: gh pr diff {pr_number}

  - name: ai-review
    prompt: |
      你是资深代码审查员。审查以下 diff，只关注：
      1. 安全漏洞
      2. 逻辑错误
      3. 性能问题

      忽略：代码风格、命名建议。

      输出格式：表格（文件 | 行号 | 问题 | 严重度）

      Diff:
      {diff_output}

  - name: post-review
    command: |
      gh pr review {pr_number} --body "{review_result}"
```

---

## 7. Memory 三层记忆

### 7.1 SOUL.md — 长期人格

```markdown
# SOUL.md

## 我的身份
- 名字：小C
- 角色：全栈工程师 + 项目经理助理
- 风格：直接、技术导向、有幽默感

## 我的主人
- 名字：张三
- 角色：技术负责人
- 偏好：不喜欢冗长的解释，直接给方案和代码
- 技术栈：Java Spring Boot + React + PostgreSQL
- 正在学的：Go 语言、Kubernetes

## 我的原则
- 代码优先于解释
- 遇到不确定的问题直接说，不要编造
- 涉及安全时务必谨慎并提醒
- 回复用中文，技术术语保留英文
```

### 7.2 MEMORY.md — 事件与决策

```markdown
# MEMORY.md

## 2026-05-20
- 用户提到项目切换到 Spring Boot 3.2，需注意 Jakarta EE 命名空间
- 用户偏好 @Transactional 放在 Service 层，不在 Controller

## 2026-05-18
- 数据库迁移使用 Flyway，不要用 Liquibase（团队决策）
- API 错误码统一用 ResultCode 枚举，不要再新增散落的数字

## 2026-05-15
- 前端 build 报错是 Node 版本问题，项目锁定 Node 20 LTS
```

### 7.3 会话上下文自动管理

```yaml
# OpenClaw 自动做上下文管理，不需要手动干预
memory:
  working_memory_size: 50     # 保留最近 50 轮对话

  # 超出 50 轮后自动压缩：
  # - 前 10 轮：生成一次摘要
  # - 中间的：保留关键决策点
  # - 后 20 轮：保留原文

  # 当上下文超过模型窗口的 70% 时：
  # 自动触发压缩，优先保留最近的交互
```

---

## 8. 高级功能

### 8.1 定时任务（Cron）

```bash
# 通过对话创建定时任务（不需要写 cron 表达式！）

"每天早上 8 点给我发今日待办提醒"        → cron: 0 8 * * *
"每周五下午 5 点自动生成本周工作总结"     → cron: 0 17 * * 5
"每 30 分钟检查一下 CI 构建状态"         → cron: */30 * * * *
"每月 1 号早上提醒我交房租"              → cron: 0 9 1 * *
```

```yaml
# 结果：OpenClaw 会自动生成并管理这些 cron 任务
# 你随时可以查看和修改
openclaw cron list
openclaw cron remove 3
```

### 8.2 Webhook 触发

```yaml
# OpenClaw 可以接收外部 Webhook 触发
# 例如：GitHub PR → Webhook → OpenClaw → 自动审查

# 端点：POST http://your-server:3000/webhook/github
# 支持：GitHub / GitLab / Jira / 自定义

# 配置 webhook 技能：
triggers:
  - name: github-pr-hook
    type: webhook
    source: github
    event: pull_request.opened
    skill: github-pr-review        # 自动触发这个技能
```

### 8.3 子代理委派

```bash
# 大任务可以拆分成子代理并行执行
"分析这 3 个微服务，每个写一份技术债务报告"
# OpenClaw 自动创建 3 个子代理，各负责一个微服务

"对比这 5 个开源库，帮我选一个"
# 自动创建 5 个子代理分别调研，最后汇总对比
```

### 8.4 可插拔上下文引擎（v3.8+）

```yaml
# 安装 lossless-claw 插件
# 实现"永不丢失上下文"——即使对话跨月，AI 也能回忆起之前的所有细节

context_engine:
  plugin: lossless-claw
  config:
    storage: sqlite    # sqlite / postgres / redis
    index_method: fts5  # 全文搜索
    summarization: auto  # 自动摘要归档
```

---

## 9. 安全加固

### 9.1 必须做的事

```yaml
# 1. 管理面板加密码
security:
  admin_panel:
    enabled: true
    password: "${ADMIN_PASSWORD}"   # 强密码
    port: 3001
    bind: 127.0.0.1                  # 只绑本地

# 2. 配置白名单
security:
  url_whitelist:
    - "api.github.com"
    - "api.openai.com"
    - "api.anthropic.com"
  # 其他 URL 一律拒绝

# 3. 命令确认
security:
  require_confirmation_for:
    - shell_exec
    - file_delete
    - git_push
    - npm_publish
    - docker_push

# 4. 限制管理员
security:
  admin_ids:
    - "telegram:YOUR_TELEGRAM_ID"  # 只有你能执行敏感操作
```

### 9.2 公网部署安全

```bash
# 永远不要在公网直接暴露 OpenClaw！
# 正确做法：Nginx 反向代理 + HTTPS + 认证

# nginx.conf
server {
    listen 443 ssl;
    server_name claw.yourdomain.com;

    ssl_certificate /etc/ssl/cert.pem;
    ssl_certificate_key /etc/ssl/key.pem;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;

        # 基础认证
        auth_basic "Restricted";
        auth_basic_user_file /etc/nginx/.htpasswd;
    }

    # Webhook 端点可以不加认证（GitHub 需要访问）
    location /webhook/ {
        proxy_pass http://127.0.0.1:3000;
        # 但要做来源校验
    }
}
```

### 9.3 已知漏洞与防御

```
CVE-2026-25253：高危 RCE 漏洞 (CVSS 8.8)
- 影响版本：< v2026.2.17
- 修复：升级到最新版即可

ClawHub 供应链风险：
- 约 12% 社区技能含恶意代码或后门
- 防御：安装技能前审查源码，只用高下载量的知名技能

公网暴露风险：
- 超过 3 万个实例曾暴露在公网
- 防御：永远绑定在 127.0.0.1 或内网 IP
```

---

## 10. 生态与衍生

### 10.1 主要衍生项目

| 项目 | 开发商 | 特点 |
|------|--------|------|
| **MaxClaw** | MiniMax | 由 M2.7 大模型驱动 |
| **AutoClaw** | 智谱 | 基于 GLM 生态 |
| **NemoClaw** | NVIDIA | 企业级平台，基于 Nemotron 3 |
| **AWS 部署方案** | Amazon | Bedrock AgentCore 多租户 Serverless |

### 10.2 国内云端部署

```bash
# 腾讯云 —— 一键部署
# 控制台搜索 "OpenClaw 专属部署"
# 自动配置：云服务器 + 数据库 + 监控 + 域名

# 阿里云 —— Serverless 方案
# 按调用量计费，适合轻量使用
# 控制台搜索 "函数计算 AI Agent 模板"
```

---

## 11. 最佳实践

### 11.1 起步建议

```bash
# 第一周：只接 Telegram，用默认配置
# 第二周：添加 2~3 个自定义技能
# 第三周：配置 SOUL.md 和记忆系统
# 第四周：根据日志和成本调整模型策略
```

### 11.2 成本控制

```yaml
# 降本三板斧
models:
  default: gemini-2.5-flash    # 主力用最便宜的
  # 只有复杂任务才升级
  upgrade_triggers:
    - keyword: ["code review", "代码审查"]
      model: claude-sonnet-4-6
    - keyword: ["analyze", "分析"]
      model: gemini-2.5-pro

limits:
  max_cost_per_day: 10        # 每天预算
  cost_alert: 8               # 80% 预算时告警
```

### 11.3 注意事项

```
✅ 适合 OpenClaw：
- 7×24 个人 AI 助理
- 跨平台消息中转
- 自动化的定时任务
- 团队日报/周报自动化

⚠️ 不适合：
- 实时性要求极高的交易系统
- 完全无人工审查的自动部署（危险！）
- 需要处理敏感金融/医疗数据的场景
- 完全离线无网络的环境
```

---

> **参考资源**
> - GitHub: [openclaw/openclaw](https://github.com/openclaw/openclaw)
> - ClawHub 技能市场: [clawhub.ai](https://clawhub.ai)
> - 社区文档: [docs.openclaw.ai](https://docs.openclaw.ai)
> - Docker Hub: [ghcr.io/openclaw/openclaw](https://github.com/openclaw/openclaw/pkgs/container/openclaw)
