# ⚙️ 快速精通 OpenCode

> **核心摘要**：OpenCode 是 SST 团队推出的开源终端 AI 编码代理，14 万+ GitHub Stars、月活 750 万+。支持 75+ 模型、LSP 深度代码理解、多会话并行。MIT 协议完全免费，是 Claude Code 的最强开源替代。

> **前置阅读**：[[快速精通-GPT-Gemini-OpenCode-OpenClaw-Hermes]]、[[OpenClaw 核心知识点]]

---

## 目录

1. [OpenCode 是什么](#1-opencode-是什么)
2. [安装配置](#2-安装配置)
3. [核心工作流](#3-核心工作流)
4. [模型与提供商](#4-模型与提供商)
5. [Skills 技能系统](#5-skills-技能系统)
6. [MCP 集成](#6-mcp-集成)
7. [配置详解](#7-配置详解)
8. [对比与选型](#8-对比与选型)
9. [最佳实践](#9-最佳实践)

---

## 1. OpenCode 是什么

由 **SST（Serverless Stack）团队**推出，2025 年 7 月发布，MIT 协议。

### 核心竞争力

- **完全免费**：MIT 开源，不付费
- **多模型**：75+ 提供商，Claude/GPT/Gemini/本地 Ollama
- **LSP 理解**：零配置理解 Java/TS/Python 等代码语义
- **终端原生**：精美 TUI，纯键盘操作
- **隐私优先**：代码不存云端
- **MCP 协议**：Model Context Protocol 无限扩展工具链
- **双代理**：Build（写）+ Plan（读）
- **离线可用**：结合 Ollama 本地模型完全离网

### 架构

```
终端 TUI / CLI / Web → OpenCode Core → 模型路由 → 75+ 模型
                          ├── LSP 客户端（代码语义理解）
                          ├── MCP 客户端（外部工具）
                          ├── Skills 引擎（可复用工作流）
                          └── Session Mgr（多会话并行）
```

---

## 2. 安装配置

### 2.1 安装

```bash
# 推荐：一键脚本（跨平台）
curl -fsSL https://opencode.ai/install | bash

# npm 全局安装
npm install -g opencode-ai

# macOS
brew install opencode

# Ollama 一键启动（完全离线）
ollama launch opencode

# Windows
winget install opencode
```

### 2.2 首次配置

```bash
opencode init  # 创建 ~/.config/opencode/config.yaml
```

```yaml
# config.yaml 最小配置
default_model: claude-sonnet-4-6
auto_accept: false
```

### 2.3 文件引用语法

```
@src/main/java/com/app/UserService.java    引用文件
@src/main/java/                            引用目录
@src/ #findAll                             搜索符号
@git:diff                                  引用 git diff
@git:staged                                引用已暂存改动
```

---

## 3. 核心工作流

### 3.1 双代理模式

| 模式 | 权限 | 适用场景 |
|---|---|---|
| **/plan** | 只读 | 架构分析、代码审查、可行性研究 |
| **/build** | 可写 | 创建文件、编辑代码、运行命令 |

### 3.2 典型开发流程

```bash
cd my-spring-boot-project
opencode

# 1. 先规划
/plan 分析这个项目的整体架构，识别可以优化的点。

# 2. 开始构建
/build 实现以下改进：添加参数校验、统一异常处理

# 3. 审查改动
/diff 审查当前所有改动

# 4. 生成测试
/build 为新增功能生成 JUnit 5 + Mockito 测试

# 5. 提交
!git commit -m "feat: implement user module"
```

### 3.3 快捷键

| 快捷键 | 作用 |
|---|---|
| `Ctrl+T` | 新建会话标签 |
| `Ctrl+W` | 关闭当前会话 |
| `Ctrl+Tab` | 切换标签 |
| `Ctrl+K` | 清除当前对话 |
| `Ctrl+P` | 命令面板 |

---

## 4. 模型与提供商

### 4.1 支持 75+ 提供商

```bash
# 交互式切换
/model claude-sonnet-4-6        # Anthropic
/model gpt-4.1                  # OpenAI
/model gemini-2.5-flash         # Google
/model deepseek-v3              # DeepSeek
/model ollama/qwen3:32b         # 本地 Ollama

# 命令行指定模型
opencode --model gemini-2.5-flash "解释这段代码"
```

### 4.2 本地模型配置

```yaml
# config.yaml
providers:
  ollama:
    endpoint: http://localhost:11434
    models:
      - qwen3:32b
      - deepseek-coder-v3
default_model: ollama/qwen3:32b
```

---

## 5. Skills 技能系统

Skills 是可复用的工作流模板：

```yaml
# .opencode/skills/code-review.yaml
name: code-review
steps:
  - id: analyze
    agent: plan
    prompt: |
      审查代码，按安全性、性能、可读性、健壮性打分（1-10）
      每个问题给出具体文件和行号。

  - id: report
    agent: build
    prompt: |
      生成 Markdown 审查报告，保存到 .opencode/reports/
```

```bash
# 使用技能
/skill code-review @src/main/java/com/app/service/
/skill generate-java-test target_class=UserService
/skill list
```

---

## 6. MCP 集成

Model Context Protocol 让 OpenCode 接入外部工具：

```yaml
# config.yaml
mcp:
  servers:
    - name: github
      command: npx -y "@modelcontextprotocol/server-github"
      env:
        GITHUB_PERSONAL_ACCESS_TOKEN: "${GITHUB_TOKEN}"

    - name: postgres
      command: npx -y "@modelcontextprotocol/server-postgres"
      env:
        DATABASE_URL: "postgresql://localhost:5432/mydb"
```

```bash
# 配置后可直接对话查询数据库
/build 查询 users 表中注册超 30 天未激活的用户，生成清理脚本
```

---

## 7. 配置详解

### 7.1 全局配置

```yaml
# ~/.config/opencode/config.yaml
default_model: claude-sonnet-4-6
auto_accept: false
max_concurrent_agents: 3

session:
  max_history_tokens: 100000
  auto_summarize: true

edit:
  create_backups: true

# 权限控制
permissions:
  allow_shell: true
  allow_file_write: true
  ask_before:
    - git push
    - npm publish
```

### 7.2 项目级配置

```yaml
# .opencode/project.yaml
name: "My Spring Boot App"
rules:
  - "所有 API 返回统一格式 Result<T>"
  - "Controller 不写业务逻辑"
preferred_models:
  code: claude-sonnet-4-6
  review: gpt-4.1
  simple: gemini-2.5-flash
```

---

## 8. 对比与选型

| 维度 | OpenCode | Claude Code | Cursor |
|---|---|---|---|
| 价格 | **完全免费** | $18/月(Pro) | $20/月(Pro) |
| 开源 | **MIT 完全开源** | 闭源 | 闭源 |
| 模型绑定 | 75+ 任意选 | 仅 Claude | 多模型 |
| 本地模型 | Ollama 深度支持 | 不支持 | 有限 |
| LSP 集成 | 零配置内置 | 需手动配置 | IDE 自带 |
| 离网可用 | 是（Ollama） | 否 | 否 |
| 隐私 | **代码不上云** | 代码上传 | 代码上传 |

### 场景推荐

- 个人开发者想省钱 → OpenCode
- 内网开发数据不能出外网 → OpenCode + Ollama
- 只用 Claude 追求体验 → Claude Code
- IDE 重度用户不切终端 → Cursor

---

## 9. 最佳实践

### 9.1 日常工作流

```bash
# 别名
alias oc="opencode"
alias ocr="opencode --model gpt-4.1"          # review
alias occ="opencode --model claude-sonnet-4-6" # code

# 管道使用
git log --oneline -20 | opencode "总结最近的开发活动"
cat error.log | opencode "分析错误并给出修复"

# CI/CD 集成（GitHub Actions）
- run: opencode --model claude-sonnet-4-6 "@git:diff 审查 PR 改动" > review.md
```

### 9.2 注意事项

- 生成的代码必须人工审查
- 不要完全信任 AI 的安全相关决策
- 敏感项目建议只用本地模型（Ollama）
- 团队可共享 `.opencode/skills/` 目录到 git

---

## 核心要点回顾

- OpenCode 是 MIT 协议的开源终端 AI 编码代理，75+ 模型支持，完全免费
- 双代理模式：Plan（只读分析）+ Build（可写开发）
- LSP 零配置集成，支持 MCP 协议扩展外部工具链
- 结合 Ollama 可完全离线运行，数据不出本地
- 适合个人开发者、内网环境、以及想替代 Claude Code 的用户

## 参考资料

1. OpenCode 官网：https://opencode.ai
2. OpenCode GitHub：https://github.com/opencode-ai/opencode
3. OpenCode 文档：https://opencode.ai/docs
4. Model Context Protocol：https://modelcontextprotocol.io
