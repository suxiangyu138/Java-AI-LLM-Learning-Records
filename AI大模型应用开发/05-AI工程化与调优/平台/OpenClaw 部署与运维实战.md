# OpenClaw 部署与运维实战（实战版）

> **文档定位**：OpenClaw 开源 Agent 框架部署运维指南
> **版本**：OpenClaw 2026
> **核心场景**：本地部署、Docker 部署、多渠道接入、运维监控

---

## 一、部署方式对比

| 方式 | 复杂度 | 适用 | 优势 |
|---|---|---|---|
| **npm 全局安装** | ⭐ | 个人开发者 | 最简单 |
| **Docker Compose** | ⭐⭐ | 单机部署 | 环境隔离 |
| **Docker + Nginx** | ⭐⭐⭐ | 生产单机 | 完整生产方案 |
| **Kubernetes** | ⭐⭐⭐⭐ | 生产集群 | 弹性伸缩 |

---

## 二、npm 快速部署

```bash
# 前置要求：Node.js 22+ + Bun
node --version  # >= 22
bun --version   # 或 npm

# 安装
npm install -g openclaw

# 初始化配置
openclaw init

# 启动
openclaw start

# 访问 Gateway
openclaw dashboard        # Web 控制台
# http://localhost:18789
```

---

## 三、Docker Compose 生产部署

```yaml
version: '3.8'
services:
  openclaw:
    image: openclaw/openclaw:latest
    container_name: openclaw
    restart: unless-stopped
    ports:
      - "18789:18789"        # Gateway WebSocket + HTTP
    environment:
      - NODE_ENV=production
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}
      - TELEGRAM_BOT_TOKEN=${TELEGRAM_BOT_TOKEN}
      - DISCORD_BOT_TOKEN=${DISCORD_BOT_TOKEN}
    volumes:
      - ./data:/app/data            # 持久化数据
      - ./config:/app/config        # 配置文件
      - ./skills:/app/skills        # 自定义 Skills
      - ./memory:/app/memory        # 记忆文件
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:18789/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  # 可选：沙箱容器
  sandbox:
    image: node:22-alpine
    container_name: openclaw-sandbox
    read_only: true
    tmpfs: /tmp
    network_mode: none
    restart: no
    profiles: ["sandbox"]
```

---

## 四、多渠道接入配置

### 4.1 Telegram

```bash
# 1. 通过 @BotFather 创建 Bot 获取 Token
# 2. 配置
openclaw channel add telegram --token "YOUR_BOT_TOKEN"

# 3. 或直接编辑 config
```

```yaml
# config/channels.yaml
channels:
  telegram:
    enabled: true
    token: ${TELEGRAM_BOT_TOKEN}
    allowlist:
      - "@your_username"
    groups:
      - chat_id: "-100xxx"
        sandbox: true            # 群组强制沙箱
```

### 4.2 Discord

```yaml
channels:
  discord:
    enabled: true
    token: ${DISCORD_BOT_TOKEN}
    client_id: "YOUR_CLIENT_ID"
    guild_id: "YOUR_SERVER_ID"
    channels:
      - "ai-chat"
```

### 4.3 Slack

```yaml
channels:
  slack:
    enabled: true
    token: ${SLACK_BOT_TOKEN}
    signing_secret: ${SLACK_SIGNING_SECRET}
    app_token: ${SLACK_APP_TOKEN}
```

---

## 五、自定义 Skills 部署

```
skills/
└── my-custom-skill/
    ├── SKILL.md              # Skill 定义
    ├── description.md        # 简短描述（Agent 扫描时读）
    └── tools/
        └── custom_tool.ts    # 自定义工具
```

```markdown
# SKILL.md

## name: my-java-reviewer
## description: Java 代码审查专家，检查编码规范、安全漏洞、性能问题

## 触发条件
当用户需要审查 Java 代码时

## 工作流
1. 读取用户指定的 Java 文件
2. 按阿里巴巴规范检查
3. 按 OWASP Top 10 检查安全
4. 检查性能问题（N+1 查询、内存泄漏）
5. 输出结构化审查报告
```

---

## 六、运维命令

```bash
# 查看状态
openclaw status
openclaw doctor                # 诊断

# 查看日志
openclaw logs --tail 100
openclaw logs --filter error

# 管理 Channels
openclaw channel list
openclaw channel enable telegram
openclaw channel disable discord

# 管理 Agents
openclaw agent list
openclaw agent restart <agent-id>

# 备份
openclaw backup --output /backup/openclaw_$(date +%Y%m%d).tar.gz

# 更新
openclaw update
openclaw update --channel beta
```

---

## 七、监控与指标

```yaml
# config/monitoring.yaml
monitoring:
  prometheus:
    enabled: true
    port: 9091
  health_check:
    interval: 30s
    endpoints:
      - llm_api
      - telegram
      - discord
  alerting:
    webhook: ${ALERT_WEBHOOK_URL}
    rules:
      - name: llm_api_error
        condition: error_rate > 5%
        action: notify+fallback_model
      - name: agent_timeout
        condition: p95 > 30s
        action: notify
```

---

## 八、核心要点

1. **最快的部署方式？** `npm install -g openclaw && openclaw init && openclaw start`
2. **生产用什么？** Docker Compose（单机）/ Kubernetes（集群）
3. **怎么接入 Telegram？** `openclaw channel add telegram --token XXX`
4. **自定义 Skill 放哪？** `skills/` 目录，含 SKILL.md + description
5. **怎么监控？** `openclaw status` + Prometheus 指标 + Webhook 告警

---

## 九、极简总结

```
安装 = npm install -g openclaw
初始化 = openclaw init
启动 = openclaw start
生产 = Docker Compose（数据持久化 + 健康检查 + 自动重启）
渠道 = telegram/discord/slack 一键接入
Skill = skills/目录 + SKILL.md 定义
运维 = status/logs/backup/update 子命令
```
