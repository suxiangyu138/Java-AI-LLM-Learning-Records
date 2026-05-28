# 快速精通 OpenCode

> 开源、免费、终端原生的 AI 编码代理。支持 75+ 模型、LSP 深度代码理解、多会话并行。14 万+ GitHub Stars，月活 750 万+，是 Claude Code 最强开源替代品。

> GitHub：[opencode-ai/opencode](https://github.com/opencode-ai/opencode) | 官网：[opencode.ai](https://opencode.ai)

---

## 目录

1. [OpenCode 是什么](#1-opencode-是什么)
2. [安装配置](#2-安装配置)
3. [界面与交互](#3-界面与交互)
4. [核心工作流](#4-核心工作流)
5. [模型与提供商](#5-模型与提供商)
6. [Skills 技能系统](#6-skills-技能系统)
7. [MCP 集成](#7-mcp-集成)
8. [配置详解](#8-配置详解)
9. [高级用法](#9-高级用法)
10. [OpenCode Zen](#10-opencode-zen)
11. [对比与选型](#11-对比与选型)
12. [最佳实践](#12-最佳实践)

---

## 1. OpenCode 是什么

OpenCode 是由 **SST（Serverless Stack）团队**推出的开源终端 AI 编码代理，2025 年 7 月正式发布，MIT 协议。

### 核心竞争力

```
┌────────────────────────────────────────────┐
│            OpenCode 核心能力                │
├────────────────────────────────────────────┤
│  🆓 完全免费         MIT 开源，不付费       │
│  🔀 多模型           Claude/GPT/Gemini/本地  │
│  🧠 LSP 理解         Java/TS/Python 等语义   │
│  📟 终端原生         纯键盘操作，TUI 精美     │
│  🔒 隐私优先         代码不存云端            │
│  🔌 MCP 协议         无限扩展工具链          │
│  🤖 双代理           Build（写）+ Plan（读） │
│  📱 多端             CLI + Web + 移动端      │
└────────────────────────────────────────────┘
```

### 架构概念

```
User (终端 TUI / CLI / Web)
    │
    ▼
OpenCode Core ── 模型路由 ──→ Claude / GPT / Gemini / Ollama / ...
    │
    ├── LSP 客户端 ──→ 代码语义理解（定义跳转、引用、诊断）
    ├── MCP 客户端 ──→ 外部工具（数据库、GitHub、文件系统）
    ├── Skills 引擎 ──→ 可复用工作流模板
    └── Session Mgr ──→ 多会话并行的对话管理
```

---

## 2. 安装配置

### 2.1 四种安装方式

```bash
# 方式一：快速脚本（Windows/macOS/Linux 通用，推荐）
curl -fsSL https://opencode.ai/install | bash

# 方式二：npm 全局安装
npm install -g opencode-ai

# 方式三：Homebrew（macOS）
brew install opencode

# 方式四：Ollama 一键启动本地版（2026年4月新增）
ollama launch opencode
# 自动下载 OpenCode + 内置模型，完全离线可用
```

```bash
# Windows 也可以直接用 winget
winget install opencode
```

### 2.2 首次配置

```bash
# 初始化配置文件
opencode init

# 这会创建 ~/.config/opencode/config.yaml
# 目录结构：
# ~/.config/opencode/
# ├── config.yaml        # 主配置
# ├── providers.yaml     # LLM 提供商
# ├── skills/            # 自定义技能
# └── sessions/          # 会话记录
```

```yaml
# config.yaml 最小配置
default_model: claude-sonnet-4-6
auto_accept: false

# 配置你的 API Key（环境变量方式，推荐）
# export ANTHROPIC_API_KEY=sk-ant-xxx
# export OPENAI_API_KEY=sk-xxx
# export GEMINI_API_KEY=AIza...
```

### 2.3 验证安装

```bash
opencode --version
opencode --help
opencode "hello, who are you?"  # 直接提问（非交互模式）
```

---

## 3. 界面与交互

### 3.1 启动

```bash
cd my-project
opencode        # 启动交互式 TUI
```

### 3.2 快捷键

| 快捷键 | 作用 |
|--------|------|
| `Enter` | 发送消息 |
| `Shift+Enter` | 换行 |
| `Ctrl+T` | 新建会话标签 |
| `Ctrl+W` | 关闭当前会话 |
| `Ctrl+Tab` | 切换下一个标签 |
| `Ctrl+Shift+Tab` | 切换上一个标签 |
| `Ctrl+K` | 清除当前对话 |
| `Ctrl+P` | 命令面板 |
| `Ctrl+C` | 中断模型生成 |
| `Ctrl+D` | 退出 |
| `/` | 输入斜杠命令 |

### 3.3 斜杠命令

| 命令 | 作用 | 示例 |
|------|------|------|
| `/plan` | 切换到规划模式（只读） | `/plan 分析这个项目的架构` |
| `/build` | 切换到构建模式（可写） | `/build 实现新功能` |
| `/model` | 切换当前模型 | `/model gpt-4.1` |
| `/diff` | 查看当前改动 | `/diff` |
| `/context` | 查看上下文窗口用量 | `/context` |
| `/clear` | 清空当前对话 | `/clear` |
| `/sessions` | 查看和管理会话 | `/sessions` |
| `/help` | 帮助 | `/help` |
| `/exit` | 退出 | `/exit` |

### 3.4 文件引用语法

```bash
# @ 引用文件
@src/main/java/com/app/UserService.java 请解释这个类

# @ 引用目录
@src/main/java/ 分析这个项目的包结构

# 引用多个文件
@UserService.java @UserController.java 对比这两个文件的逻辑

# 搜索符号
@src/ #findAll 这个方法在哪里被调用？

# 引用 git diff
@git 当前未提交的改动有什么问题？
@git:staged 已暂存的改动
```

---

## 4. 核心工作流

### 4.1 规划模式（Plan Mode）

Plan 模式下的 Agent 只能**读取和分析**代码，不能修改任何文件，适合架构分析、代码审查、可行性研究。

```bash
# 进入 OpenCode 后
/plan

# 分析整个项目
@整个项目 分析其架构设计，指出潜在的扩展性问题

# 审查具体模块
@src/main/java/com/app/service/ 这个 service 层的职责划分是否合理？

# 依赖分析
分析 pom.xml 中的依赖，有没有过时或存在安全漏洞的？
```

### 4.2 构建模式（Build Mode）

Build 模式下 Agent 有完整权限：创建文件、编辑代码、运行命令、管理 git。

```bash
/build

# 全自动开发
根据这个需求文档 @docs/REQ-001.md，实现完整的 CRUD 功能

# 测试驱动
为 @src/main/java/com/app/service/UserService.java 生成完整的单元测试

# 重构
将 UserController 中的业务逻辑提取到 UserService，保持 API 不变
```

### 4.3 典型开发流程

```bash
# 1. 启动 OpenCode
cd my-spring-boot-project
opencode

# 2. 先规划
/plan
分析这个项目的整体架构，识别可以优化的点。

# 3. 开始构建
/build
基于你的分析，实现以下改进：
1. 将 UserService 中的异常处理统一化
2. 添加参数校验

# 4. 审查改动
/diff
审查当前所有改动，找出 bug 和规范问题。

# 5. 生成测试
/build
为新增的功能生成单元测试和集成测试。

# 6. 提交
生成一个符合 Conventional Commits 规范的提交信息，然后提交。
```

### 4.4 非交互模式

```bash
# 单次提问
opencode "这个项目用了哪些设计模式？"

# 指定文件分析
opencode "@UserService.java 解释这个类的设计"

# 批量操作
opencode "为所有 Service 类生成 JavaDoc 注释"

# 管道传入
cat error.log | opencode "分析这些错误日志，归因并给出修复建议"

# 输出到文件
opencode "生成项目的 README.md" > README.md
```

---

## 5. 模型与提供商

### 5.1 支持的提供商（75+）

```bash
# 查看所有支持的提供商
opencode providers list

# 配置提供商
opencode providers add openai
opencode providers add anthropic
opencode providers add google
opencode providers add deepseek
opencode providers add ollama    # 本地模型
opencode providers add groq      # 快速推理
opencode providers add together  # 开源模型云托管
```

### 5.2 模型切换

```bash
# 交互模式下热切换
/model claude-sonnet-4-6        # Anthropic
/model gpt-4.1                  # OpenAI
/model gemini-2.5-flash         # Google
/model deepseek-v3              # DeepSeek
/model ollama/qwen3:32b         # 本地 Ollama
/model groq/llama-4-maverick    # Groq 超高速

# 命令行指定模型
opencode --model gemini-2.5-flash "解释这段代码"

# 为不同任务预设模型
opencode --model o4-mini "@src/ 审查潜在的性能问题"
opencode --model gpt-4.1 "@src/ 实现新功能"
```

### 5.3 本地模型配置（Ollama）

```bash
# 安装 Ollama
# https://ollama.com

# 下载模型
ollama pull qwen3:32b
ollama pull deepseek-coder-v3
ollama pull llama3.3:70b

# OpenCode 中使用
opencode --model ollama/qwen3:32b

# 优点：完全离线、数据不外传、零费用
# 缺点：小模型能力有限，大模型需要好显卡
```

```yaml
# config.yaml 中指定默认本地模型
providers:
  ollama:
    endpoint: http://localhost:11434
    models:
      - qwen3:32b
      - deepseek-coder-v3

default_model: ollama/qwen3:32b
```

---

## 6. Skills 技能系统

Skills 是可复用的工作流模板，让 OpenCode 按预设流程执行任务。

### 6.1 技能文件结构

```yaml
# .opencode/skills/code-review.yaml
name: code-review
version: "1.0"
description: 标准代码审查流程，检查安全、性能、规范

steps:
  - id: analyze
    type: agent
    agent: plan  # 只读分析
    prompt: |
      审查以下文件的代码，按以下维度打分（1-10）：
      1. **安全性**：SQL注入、XSS、CSRF、敏感信息泄露
      2. **性能**：N+1 查询、不必要的对象创建、算法复杂度
      3. **可读性**：命名规范、注释质量、方法长度
      4. **健壮性**：null 检查、异常处理、边界条件
      5. **可测试性**：耦合度、依赖注入、纯函数比例

      每个问题给出具体的文件和行号。

  - id: report
    type: agent
    agent: build  # 可写，生成报告
    prompt: |
      基于上一步的分析结果，生成一个 Markdown 格式的审查报告，
      保存到 .opencode/reports/code-review-{timestamp}.md，
      包含：
      - 各维度评分表
      - 关键问题列表（按严重度排序）
      - 改进建议
```

```yaml
# .opencode/skills/generate-test.yaml
name: generate-java-test
version: "1.0"
description: 为 Java 类生成完整的 JUnit 5 测试

steps:
  - id: analyze-class
    type: agent
    agent: plan
    prompt: |
      分析 {target_class} 的：
      1. 所有 public 方法签名
      2. 依赖项（需要 Mock 的外部服务）
      3. 边界条件和异常路径

  - id: generate
    type: agent
    agent: build
    prompt: |
      基于分析结果，为 {target_class} 生成完整的 JUnit 5 + Mockito 测试：
      - 正常路径测试
      - 异常路径测试
      - 边界值测试
      - mock 所有外部依赖
      - 覆盖率目标 > 85%

      保存到 src/test/java/{package_path}/{class_name}Test.java
```

### 6.2 使用技能

```bash
# 在对话中调用技能
/skill code-review @src/main/java/com/app/service/

# 带参数调用
/skill generate-java-test target_class=UserService

# 创建新技能
/skill create deploy-check

# 列出所有技能
/skill list
```

---

## 7. MCP 集成

Model Context Protocol（MCP）让 OpenCode 可以接入任意外部工具。

### 7.1 配置 MCP 服务器

```yaml
# config.yaml
mcp:
  servers:
    - name: github
      command: npx
      args:
        - -y
        - "@modelcontextprotocol/server-github"
      env:
        GITHUB_PERSONAL_ACCESS_TOKEN: "${GITHUB_TOKEN}"

    - name: postgres
      command: npx
      args:
        - -y
        - "@modelcontextprotocol/server-postgres"
      env:
        DATABASE_URL: "postgresql://localhost:5432/mydb"

    - name: filesystem
      command: npx
      args:
        - -y
        - "@modelcontextprotocol/server-filesystem"
        - "/path/to/allowed/dir"
```

### 7.2 实战：数据库查询

```bash
# 配置好 Postgres MCP 后，直接在对话中查询
/build
查询 users 表中有多少注册超过 30 天但未激活的用户，
生成一个清理脚本。
# OpenCode 会自动通过 MCP 连接数据库、查询、生成脚本

# 实战：GitHub 操作
/build
查看这个 repo 最近 10 个 PR，总结主要改动方向。
# OpenCode 通过 GitHub MCP 获取数据
```

---

## 8. 配置详解

### 8.1 完整配置参考

```yaml
# ~/.config/opencode/config.yaml

# 默认模型
default_model: claude-sonnet-4-6

# 自动接受（跳过确认，谨慎开启）
auto_accept: false

# 最大并发 Agent
max_concurrent_agents: 3

# 会话设置
session:
  max_history_tokens: 100000  # 上下文窗口保护
  auto_summarize: true        # 自动压缩长历史

# 代码编辑
edit:
  create_backups: true         # 修改前备份
  backup_dir: .opencode/backups

# LSP 设置
lsp:
  java:                         # Java 项目自动检测
    enabled: true
  typescript:
    enabled: true
  python:
    enabled: true

# 排除文件
exclude:
  - node_modules/
  - target/          # Maven build
  - build/           # Gradle build
  - .git/
  - *.log
  - .env

# 权限控制
permissions:
  allow_shell: true        # 允许执行命令
  allow_file_write: true   # 允许写文件
  allow_network: true      # 允许网络请求
  ask_before:
    - git push
    - npm publish
    - rm -rf

# 主题
theme: dark  # dark / light / system

# 自定义提示词
system_prompt_append: |
  始终使用中文回复。
  代码注释用英文。
```

### 8.2 项目级配置

```yaml
# 项目根目录 .opencode/project.yaml
name: "My Spring Boot App"
description: "企业级后台管理系统"

# 项目特定的技能
skills:
  - .opencode/skills/deploy.yaml
  - .opencode/skills/db-migration.yaml

# 项目特定规则
rules:
  - "所有 API 返回统一格式 Result<T>"
  - "日期用 java.time.*，不用 java.util.Date"
  - "Controller 不写业务逻辑"

# 推荐的模型
preferred_models:
  code: claude-sonnet-4-6
  review: gpt-4.1
  simple: gemini-2.5-flash
```

---

## 9. 高级用法

### 9.1 多会话并行

```bash
# 一个会话写后端，一个会话写前端
Ctrl+T  # 新建标签 → 会话 2
/build 实现 REST API
Ctrl+T  # 新建标签 → 会话 3
/build 基于 API 实现 React 前端
Ctrl+Tab  # 在会话间切换
```

### 9.2 Web 仪表板

```bash
# 启动 Web 界面（可在另一台设备访问）
opencode web
# 打开 http://localhost:3100

# 指定端口
opencode web --port 8888

# 远程访问（注意安全）
opencode web --host 0.0.0.0 --port 3100
# 然后用手机/平板浏览器访问 http://<your-ip>:3100
```

### 9.3 CI/CD 集成

```yaml
# GitHub Actions 中使用 OpenCode
# .github/workflows/code-review.yml
name: AI Code Review
on: [pull_request]

jobs:
  review:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Install OpenCode
        run: npm install -g opencode-ai
      - name: AI Review
        env:
          ANTHROPIC_API_KEY: ${{ secrets.ANTHROPIC_API_KEY }}
        run: |
          opencode --model claude-sonnet-4-6 \
            "@git:diff 审查这个 PR 的改动，只输出严重和中等问题" \
            > review.md
      - name: Post Review
        uses: actions/github-script@v7
        with:
          script: |
            const fs = require('fs');
            const review = fs.readFileSync('review.md', 'utf8');
            github.rest.issues.createComment({
              ...context.repo,
              issue_number: context.issue.number,
              body: review
            });
```

### 9.4 自定义 Provider

```yaml
# 接入自部署的 vLLM / 任意 OpenAI 兼容接口
# providers.yaml
providers:
  custom:
    - name: my-vllm-server
      base_url: http://192.168.1.100:8000/v1
      api_key: "not-needed"
      models:
        - qwen3-72b
        - deepseek-v3

# 使用
# opencode --model my-vllm-server/qwen3-72b
```

---

## 10. OpenCode Zen

OpenCode Zen 是可选的**付费精选模型层**，提供经过验证的生产级模型配置。

| 层级 | 价格 | 说明 |
|------|------|------|
| **Free** | $0 | 自带 API Key，无限使用 |
| **Zen Starter** | $10/月 | 提供 Zen 模型 + 基础用量 |
| **Zen Pro** | $30/月 | 更多额度 + 优先队列 + 高级模型 |

```bash
# 开通 Zen
opencode zen login
opencode zen subscribe starter

# Zen 模型命名
opencode --model zen/sonnet-4  # 优化过的 Sonnet 配置
```

> 大部分用户不需要 Zen——用自己的 API Key 就足够。

---

## 11. 对比与选型

### 11.1 OpenCode vs Claude Code vs Cursor vs Copilot

| 维度 | OpenCode | Claude Code | Cursor | GitHub Copilot |
|------|----------|-------------|--------|----------------|
| **价格** | 完全免费 | $18/月(Pro) | $20/月(Pro) | $10/月 |
| **开源** | MIT 完全开源 | 闭源 | 闭源 | 闭源 |
| **模型绑定** | 75+ 任意选 | 仅 Claude | 多模型 | GPT/Claude |
| **本地模型** | Ollama 深度支持 | 不支持 | 有限 | 不支持 |
| **终端原生** | 核心体验 | 核心体验 | IDE 插件 | IDE 插件 |
| **LSP 集成** | 零配置内置 | 需手动配置 | IDE 自带 | IDE 自带 |
| **MCP 支持** | 原生支持 | 原生支持 | 有限 | 不支持 |
| **多会话** | 标签页并行 | 单会话 | 多窗口 | IDE 多窗口 |
| **离网可用** | 是 (Ollama) | 否 | 否 | 否 |
| **隐私** | 代码不上云 | 代码上传 Anthropic | 代码上传 | 代码上传 |
| **Best For** | 自由、省钱、隐私 | Claude 生态深度用户 | IDE 重度用户 | 轻量补全 |

### 11.2 场景推荐

```
场景                                → 推荐
────────────────────────────────────────────
个人开发者，想省钱                   → OpenCode (免费)
公司内网开发，数据不能出外网          → OpenCode + Ollama
只用 Claude，追求最好体验            → Claude Code
IDEA/VS Code 重度用户，不想切终端     → Cursor / Copilot
需要同时处理前后端多个模块            → OpenCode (多会话)
学生党，预算有限                     → OpenCode + Gemini 免费层
```

---

## 12. 最佳实践

### 12.1 日常工作流

```bash
# 早上：审查昨天的改动
opencode "@git:diff:yesterday 审查昨天的所有改动，生成日报"

# 开发前：先规划再动手
opencode
/plan
@src/ 实现用户积分系统，分析需要改哪些文件，给出实施步骤
# ... 确认计划 ...
/build
按照你的计划，开始实现。

# 提交前：自动审查
/diff
审查所有改动，生成 Conventional Commit 信息
!git add -A
!git commit -m "feat: 照搬上面的commit信息"
```

### 12.2 效率技巧

```bash
# 1. 别名设置
alias oc="opencode"
alias ocr="opencode --model gpt-4.1"        # review 专用
alias occ="opencode --model claude-sonnet-4-6" # code 专用
alias ocf="opencode --model gemini-2.5-flash"  # fast 专用

# 2. 预设上下文
opencode --context docs/architecture.md "基于架构文档，..."

# 3. 管道使用
git log --oneline -20 | opencode "总结最近的开发活动"
cat error.log | opencode "分析错误并给出修复"

# 4. Shell 集成
# 在 .bashrc/.zshrc 中添加
eval "$(opencode shell-integration)"
# 之后可以用 Ctrl+O 在任何命令前触发 OpenCode
```

### 12.3 团队协作

```yaml
# 团队共享技能库
# 把 .opencode/skills/ 提交到 git
# 团队成员 clone 后自动获得所有技能

# .opencode/skills/onboard.yaml
name: onboard-new-dev
description: 为新成员生成项目入门指南
steps:
  - type: agent
    agent: plan
    prompt: |
      分析项目结构：技术栈、目录布局、核心模块、构建流程。
      生成一份新人入门指南，包含：
      - 环境搭建步骤
      - 核心模块介绍
      - 常见开发任务的操作流程
      - 编码规范和注意事项
```

### 12.4 注意事项

```
✅ 适合 OpenCode：
- 日常编码、重构、生成测试
- 新功能的全流程开发
- 代码审查和优化建议
- 项目文档和注释生成
- 学习新代码库

⚠️ 注意：
- 生成的代码一定要人工审查
- 不要完全信任 AI 的安全相关决策
- 复杂业务逻辑需要清晰的 prompt
- 超大项目（>5万文件）可能索引较慢
- 敏感项目建议只用本地模型
```

---

> **速查**
> - GitHub: [opencode-ai/opencode](https://github.com/opencode-ai/opencode)
> - 官网: [opencode.ai](https://opencode.ai)
> - 文档: [opencode.ai/docs](https://opencode.ai/docs)
> - Discord: [OpenCode Community](https://discord.gg/opencode)
