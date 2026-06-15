# ⚙️ OpenClaw 部署与运维实战

> **核心摘要**：OpenClaw 开源 Agent 框架的完整部署运维指南，涵盖 npm 快速部署、Docker Compose 生产部署、多渠道接入（Telegram/Discord/Slack）、自定义 Skills 部署及 Prometheus 监控配置。

> **前置阅读**：[[OpenClaw 核心知识点]]、[[快速精通OpenClaw]]

---

## 一、部署方式对比

| 方式 | 复杂度 | 适用场景 | 优势 |
|---|---|---|---|
| npm 全局安装 | 低 | 个人开发 | 最简单 |
| Docker Compose | 中 | 单机生产 | 环境隔离 |
| Docker + Nginx | 高 | 生产单机 | 完整方案 |
| Kubernetes | 极高 | 生产集群 | 弹性伸缩 |

---

## 二、npm 快速部署

```bash
# 前置要求：Node.js 22+
node --version

# 安装
npm install -g openclaw

# 初始化配置
openclaw init

# 启动
openclaw start

# Web 控制台
openclaw dashboard    # http://localhost:18789
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
      - "18789:18789"
    environment:
      - NODE_ENV=production
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}
      - TELEGRAM_BOT_TOKEN=${TELEGRAM_BOT_TOKEN}
    volumes:
      - ./data:/app/data
      - ./config:/app/config
      - ./skills:/app/skills
      - ./memory:/app/memory
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:18789/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

---

## 四、多渠道接入配置

### Telegram

```bash
# 通过 @BotFather 创建 Bot 获取 Token
openclaw channel add telegram --token "YOUR_BOT_TOKEN"
```

```yaml
# config/channels.yaml
channels:
  telegram:
    enabled: true
    token: ${TELEGRAM_BOT_TOKEN}
    allowlist: ["@your_username"]
```

### Discord

```yaml
channels:
  discord:
    enabled: true
    token: ${DISCORD_BOT_TOKEN}
    client_id: "YOUR_CLIENT_ID"
    guild_id: "YOUR_SERVER_ID"
    channels: ["ai-chat"]
```

### Slack

```yaml
channels:
  slack:
    enabled: true
    token: ${SLACK_BOT_TOKEN}
    signing_secret: ${SLACK_SIGNING_SECRET}
```

---

## 五、自定义 Skills 部署

```
skills/
└── my-java-reviewer/
    ├── SKILL.md              # Skill 定义
    ├── description.md        # 简短描述
    └── tools/
        └── custom_tool.ts    # 自定义工具
```

```markdown
# SKILL.md
## name: my-java-reviewer
## description: Java 代码审查专家

## 工作流
1. 读取用户指定的 Java 文件
2. 按阿里巴巴规范检查
3. 按 OWASP Top 10 检查安全
4. 输出结构化审查报告
```

---

## 六、运维命令

```bash
# 状态管理
openclaw status
openclaw doctor                # 诊断

# 日志
openclaw logs --tail 100
openclaw logs --filter error

# Channel 管理
openclaw channel list
openclaw channel enable telegram
openclaw channel disable discord

# Agent 管理
openclaw agent list
openclaw agent restart <agent-id>

# 备份
openclaw backup --output /backup/openclaw_$(date +%Y%m%d).tar.gz

# 更新
openclaw update
```

---

## 七、监控配置

```yaml
# config/monitoring.yaml
monitoring:
  prometheus:
    enabled: true
    port: 9091
  health_check:
    interval: 30s
    endpoints: [llm_api, telegram, discord]
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

## 核心要点回顾

- 快速部署：`npm install -g openclaw && openclaw init && openclaw start`
- 生产部署推荐 Docker Compose，配置数据持久化 + 健康检查 + 自动重启
- 多渠道：Telegram/Discord/Slack 通过 `openclaw channel add` 一键接入
- 自定义 Skill：`skills/` 目录下创建 SKILL.md + description.md
- 监控：内置 Prometheus 指标 + Webhook 告警

## 参考资料

1. OpenClaw GitHub：https://github.com/openclaw/openclaw
2. OpenClaw 文档：https://docs.openclaw.ai
3. OpenClaw Docker：https://github.com/openclaw/openclaw/pkgs/container/openclaw
