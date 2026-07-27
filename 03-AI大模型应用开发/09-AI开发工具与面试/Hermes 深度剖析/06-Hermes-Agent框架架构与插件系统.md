# 06 - Hermes-Agent 框架架构与插件系统

> 🎯 Hermes-Agent 是 Nous Research 打造的**闭环自学习** AI Agent 框架——Agent 自主管理记忆、创建技能、在多次会话中不断进化，支持多平台接入和完整的插件扩展体系

---

## 目录

1. [框架总体架构](#1-框架总体架构)
2. [闭环自学习机制](#2-闭环自学习机制)
3. [插件系统详解](#3-插件系统详解)
4. [生命周期 Hooks](#4-生命周期-hooks)
5. [多平台接入（Gateway）](#5-多平台接入gateway)
6. [代码级上手](#6-代码级上手)

---

## 1. 框架总体架构

### 1.1 核心设计理念

```
Hermes-Agent 闭环
┌──────────────────────────────────────────────────┐
│                                                  │
│   ┌──────────┐    学习/进化     ┌──────────┐     │
│   │  记忆系统  │ ←─────────── │  技能系统  │     │
│   │ MEMORY.md │               │  Skills   │     │
│   │ USER.md   │               │ Auto-gen  │     │
│   └────┬─────┘               └────┬─────┘     │
│        │                          │            │
│   ┌────▼──────────────────────────▼─────┐      │
│   │          LLM 推理引擎               │      │
│   │  Hermes 3 / DeepHermes 3 / 任何模型  │      │
│   └────┬──────────────────────────┬─────┘      │
│        │                          │            │
│   ┌────▼─────┐              ┌────▼─────┐      │
│   │  工具执行  │              │  多平台   │      │
│   │  插件系统  │              │  Gateway  │      │
│   └──────────┘              └──────────┘      │
│                                                  │
└──────────────────────────────────────────────────┘
```

> 🎯 **核心哲学**：Agent 不是一次性工具，而是一个在使用中不断变聪明的伙伴

### 1.2 关键能力矩阵

| 能力 | 描述 | 实现方式 |
|------|------|---------|
| 记忆自主管理 | Agent 自动写 MEMORY.md/USER.md | Built-in Memory 系统 |
| 技能自动创建 | 复杂任务完成后自动生成可复用 Skill | Skill Auto-generation |
| 跨会话学习 | USER.md 跨会话持久化用户画像 | Frozen Snapshot 模式 |
| 子 Agent 委托 | 复杂任务拆解给子 Agent 并行处理 | Subagent Delegation |
| 定时自动化 | Cron 表达式触发定时任务 | Scheduled Automations |
| 多终端后端 | 本地 Shell / Docker / SSH / Modal / Daytona | 6 种 Terminal Backends |

## 2. 闭环自学习机制

### 2.1 学习循环

```text
Session Start
    │
    ├── 加载 MEMORY.md + USER.md（Frozen Snapshot）
    │
    ▼
[交互循环]
    │
    ├── 用户提问 → LLM 推理
    ├── 执行工具/技能
    ├── 写入记忆（内存中）
    │
    ▼
[技能创建 Check]
    │
    ├── 本次完成了复杂多步任务？
    │   YES → Auto-gen Skill → 下次一键调用
    │   NO  → 跳过
    │
    ▼
[记忆同步]
    │
    ├── 更新 MEMORY.md（环境知识、项目规范、经验）
    ├── 更新 USER.md（用户偏好、习惯画像）
    │
    ▼
Session End → 下次 Session 加载最新记忆
```

### 2.2 技能自动生成示例

```text
用户：帮我部署这个 Spring Boot 项目到 K8s

Agent 执行了 7 步：
  1. 检查 pom.xml → 确认 Spring Boot 版本
  2. 构建 Dockerfile
  3. 生成 k8s deployment.yaml
  4. 生成 k8s service.yaml
  5. 检查 configmap
  6. 执行 kubectl apply
  7. 验证 pod running

完成后 → 自动生成 Skill: "deploy-spring-boot-to-k8s"
下次用户说 "部署项目" → Agent 直接调用该 Skill
```

## 3. 插件系统详解

### 3.1 插件发现优先级

```text
插件加载顺序（高优先级覆盖低优先级）
│
├── 1. Bundled Plugins     <repo>/plugins/
├── 2. User Plugins        ~/.hermes/plugins/<name>/
├── 3. Project Plugins     ./.hermes/plugins/<name>/
│     (需 HERMES_ENABLE_PROJECT_PLUGINS=true)
└── 4. Pip Plugins          pip 包中的 entry-point
```

### 3.2 插件类型

| 类型 | 说明 | 示例 |
|------|------|------|
| `standalone` | 独立插件，注册工具和命令 | 天气查询、数据库操作 |
| `backend` | 工具底层实现 | Docker 终端后端、SSH 后端 |
| `exclusive` | 独占型（记忆/上下文引擎，只能选一个） | Mem0 记忆提供者、Honcho 提供者 |

### 3.3 插件结构

```
my-hermes-plugin/
├── plugin.yaml          # 插件清单（必需）
├── __init__.py          # register(ctx) 入口（必需）
├── tools.py             # 工具实现
├── config.py            # 插件配置
└── requirements.txt     # 依赖声明
```

**plugin.yaml 示例：**
```yaml
name: my-custom-plugin
version: 1.0.0
description: Custom weather and stock plugin
kind: standalone
author: dev
```

**`__init__.py` 入口：**
```python
from hermes_agent.plugin_manager import PluginContext

def register(ctx: PluginContext):
    """插件注册入口，每个插件必须有"""
    from .tools import get_weather, get_stock_price

    ctx.register_tool("get_weather", get_weather)
    ctx.register_tool("get_stock_price", get_stock_price)
```

### 3.4 可扩展接口一览

| 接口类型 | 用途 | 竞争模式 |
|---------|------|:---:|
| **Model Provider** | 推理后端（OpenAI/Anthropic/Bedrock） | 可多选 |
| **Memory Provider** | 跨会话知识持久化 | **单选** |
| **Context Engine** | 上下文压缩/缓存策略 | **单选** |
| **Platform Adapter** | Discord/Telegram/Slack/IRC | 可多选 |
| **Image/Video Gen Provider** | FAL.ai 等生成后端 | 可多选 |
| **Web Search Provider** | 搜索后端 | 可多选 |
| **Browser Provider** | 云浏览器会话 | 可多选 |
| **Secret Source** | 密钥管理（Vault/1Password） | 可多选 |
| **TTS/STT** | 语音输入输出 | 配置驱动 |
| **MCP Server** | 外部 MCP 工具 | 可多选 |

## 4. 生命周期 Hooks

### 4.1 完整 Hook 体系

```text
Session 生命周期
    │
    ├── on_session_start       ← 会话开始
    │
    ├── [用户输入]
    │
    ├── pre_llm_call           ← LLM 调用前
    ├── post_llm_call          ← LLM 调用后
    │
    ├── pre_tool_call          ← 工具执行前
    ├── post_tool_call         ← 工具执行后
    │
    ├── pre_approval_request   ← 用户审批前
    │
    ├── transform_llm_output   ← 输出转换（在送到 Gateway 前）
    │
    ├── pre_gateway_dispatch   ← Gateway 分发前
    │
    ├── on_memory_write        ← 记忆写入时
    ├── on_pre_compress        ← 上下文压缩前（保存关键信息）
    │
    ├── on_session_end         ← 会话结束
    │
    └── shutdown               ← 插件卸载时
```

### 4.2 Middleware 系统

```python
# 三种拦截点
@middleware("llm_request")    # 拦截发往 LLM 的请求 → 修改/增强 prompt
@middleware("tool_request")   # 拦截工具调用请求 → 参数校验/权限检查
@middleware("tool_execution") # 拦截工具执行结果 → 结果过滤/格式化
```

## 5. 多平台接入（Gateway）

```text
              ┌──────────────┐
              │ Hermes-Agent │
              │    Core      │
              └──────┬───────┘
                     │
              ┌──────▼───────┐
              │   Gateway    │
              └──────┬───────┘
                     │
     ┌───────────────┼───────────────┬───────────────┐
     │               │               │               │
┌────▼────┐   ┌────▼────┐   ┌────▼────┐   ┌────▼────┐
│Telegram │   │ Discord │   │ Slack  │   │WhatsApp │
└─────────┘   └─────────┘   └─────────┘   └─────────┘
```

| 平台 | 状态 | 特点 |
|------|:---:|------|
| **Telegram** | ✅ Production | 机器人 API 最成熟 |
| **Discord** | ✅ Production | 社区运营首选 |
| **Slack** | ✅ Production | 企业内部协作 |
| **WhatsApp** | ✅ Beta | 个人助手场景 |
| **Signal** | ✅ Preview | 隐私优先 |
| **Terminal (TUI)** | ✅ Production | 开发者原生体验 |

## 6. 代码级上手

### 6.1 安装与启动

```bash
# 克隆仓库
git clone https://github.com/NousResearch/hermes-agent.git
cd hermes-agent

# 安装依赖
pip install -e .

# 配置（编辑 config.yaml）
# 设置 LLM provider、API key、启用的平台等

# 启动（开发模式）
HERMES_ENABLE_PROJECT_PLUGINS=true hermes run
```

### 6.2 最小化配置

```yaml
# config.yaml
model_provider:
  type: openai_compatible
  api_base: http://localhost:11434/v1    # Ollama
  model: hermes3:8b
  api_key: ollama

platforms:
  - type: terminal                       # TUI 模式

memory:
  provider: builtin                      # 内置 MEMORY.md
```

### 6.3 自定义插件开发流程

```text
1. 创建目录：~/.hermes/plugins/my-plugin/
2. 编写 plugin.yaml
3. 实现 register(ctx) 函数
4. 注册工具/生命周期 hooks
5. 在 config.yaml 中启用
6. 重启 Agent
```

## 核心要点回顾

- Hermes-Agent = 闭环自学习 AI Agent 框架（记忆→技能→进化）
- 插件系统 4 层发现优先级，支持 standalone/backend/exclusive 三种类型
- 8+ 种可扩展接口（Model/Memory/Context/Platform/Search/Browser/Secret/MCP）
- 10+ 个生命周期 Hooks + 3 种 Middleware 拦截点
- 6 种终端后端 + 6 个平台 Gateway
- 最小化配置只需 Ollama + Hermes 3 8B 即可本地运行

## 参考资料

1. Nous Research - Hermes-Agent GitHub 仓库
2. Hermes Agent 官方文档 - hermes-agent.nousresearch.com
3. DeepWiki - Hermes-Agent 插件与记忆系统详解
