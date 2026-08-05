# Skill 与 Plugin 能力体系

> Skill 将"怎么做"沉淀为可复用模板，Plugin 将能力组合打包为可分发单元——WorkBuddy 的能力工程化体系

---

## 📚 目录

1. [Skill 概念与定位](#1-skill-概念与定位)
2. [Skill 的设计与结构](#2-skill-的设计与结构)
3. [Skill 生命周期管理](#3-skill-生命周期管理)
4. [Plugin 架构与组合](#4-plugin-架构与组合)
5. [能力分发与生态](#5-能力分发与生态)
6. [Skill vs Plugin vs Prompt vs Agent](#6-skill-vs-plugin-vs-prompt-vs-agent)

---

## 1. Skill 概念与定位

### 1.1 什么是 Skill

Skill 是 WorkBuddy 四层能力模型的 **L3 层**，它将一类任务的执行流程沉淀为**结构化、可版本化、可复用的模板**。

```text
Skill 回答的核心问题：
┌────────────────────────────────────────────┐
│                                            │
│  Prompt 告诉模型：  "做什么"                 │
│  Skill 告诉系统：   "这类任务应该怎么做"      │
│                                            │
│  Skill = 步骤流程 + 工具选择 + 约束条件       │
│         + 验收标准 + 异常处理 + 输出格式       │
│                                            │
└────────────────────────────────────────────┘
```

### 1.2 Skill 的组成部分

```text
Skill
├── 元信息（Meta）
│   ├── name：技能名称
│   ├── version：语义化版本
│   ├── description：描述（何时使用）
│   └── author：作者与维护者
│
├── 执行流程（Workflow）
│   ├── steps：步骤序列
│   │   ├── 每步的 tool：使用哪个工具
│   │   ├── 每步的 prompt：给模型的指令
│   │   ├── 每步的 timeout：超时限制
│   │   └── 每步的 on_error：异常处理
│   └── dependencies：步骤间的依赖关系
│
├── 约束条件（Constraints）
│   ├── 权限要求：需要哪些工具权限
│   ├── 资源限制：Token 预算、时间上限
│   └── 前置条件：执行前必须满足的条件
│
├── 验收标准（Acceptance Criteria）
│   ├── 输出格式：Markdown / JSON / YAML
│   ├── 内容要求：必须包含哪些部分
│   └── 质量门禁：自动检查项
│
└── 异常处理（Error Handling）
    ├── 工具调用失败 → 重试或降级
    ├── 超时 → 部分结果 + 提示
    └── 权限不足 → 明确告知用户
```

---

## 2. Skill 的设计与结构

### 2.1 一个完整的 Skill 示例

```yaml
# code-review.skill.yaml
name: "代码审查"
version: "2.1.0"
description: >
  对代码变更进行多维度审查，包括安全、性能、可读性、规范遵从性。
  当用户说"review"、"审查"、"检查代码"时触发。

author: "dev-team"
tags: ["code", "review", "quality"]

# 前置条件
prerequisites:
  tools: ["read_file", "grep", "git_diff"]
  permissions: ["read:workspace"]
  max_tokens: 32000
  timeout: 300s

# 执行步骤
steps:
  - id: "get_changes"
    name: "获取变更内容"
    tool: "git_diff"
    args:
      staged_only: true
    timeout: 15s
    on_error: "abort"

  - id: "security_check"
    name: "安全检查"
    depends_on: ["get_changes"]
    prompt: |
      检查以下代码变更中的安全问题：
      - SQL 注入风险
      - XSS 漏洞
      - 敏感信息泄露（密钥、密码）
      - 不安全的反序列化
      每个问题标注严重程度（Critical/High/Medium/Low）
    tool: "llm_analyze"
    timeout: 60s

  - id: "performance_check"
    name: "性能检查"
    depends_on: ["get_changes"]
    prompt: |
      检查以下代码变更中的性能问题：
      - N+1 查询
      - 不必要的对象创建
      - 同步阻塞操作
      - 大循环中的重复计算
    tool: "llm_analyze"
    timeout: 60s

  - id: "style_check"
    name: "代码风格检查"
    depends_on: ["get_changes"]
    tool: "run_linter"
    args:
      rules: ["checkstyle", "pmd"]
    timeout: 30s
    on_error: "continue"

  - id: "generate_report"
    name: "生成审查报告"
    depends_on: ["security_check", "performance_check", "style_check"]
    prompt: |
      基于以下检查结果，生成代码审查报告：
      - 安全：{security_check.result}
      - 性能：{performance_check.result}
      - 风格：{style_check.result}

      报告格式：
      1. 总体评估（1-2 句）
      2. Critical 问题（必须修复）- 表格
      3. Warning 建议（建议修复）- 表格
      4. Info 信息（可选优化）- 列表
    output_format: "markdown"

# 验收标准
acceptance_criteria:
  - "所有 Critical 问题必须有具体的修复建议代码"
  - "报告不超过 3000 字"
  - "代码片段使用正确的语言标注"
  - "每个问题标注所在文件和行号"
```

### 2.2 Skill 设计原则

| 原则 | 说明 | 示例 |
|------|------|------|
| **单一任务类型** | 一个 Skill 只覆盖一种任务 | "代码审查"而不是"代码质量全流程" |
| **可组合** | Skill 可以嵌套调用其他 Skill | "发布检查"调用"代码审查"+"测试运行" |
| **参数化** | 通过参数适应不同场景 | `focus: security` vs `focus: performance` |
| **可观测** | 每步执行输出进度和耗时 | `[1/5] 获取变更内容... ✅ 0.8s` |
| **幂等结果** | 相同输入得出一致结果 | 不受会话历史影响 |

### 2.3 Skill 触发方式

```text
触发机制：
├── 关键词匹配 → 用户说"review 代码" → 触发 code-review Skill
├── 上下文推断 → 刚写完代码 → 建议运行 lint Skill
├── 显式调用 → 用户说"用 code-review Skill 检查"
├── 管道串联 → A Skill 的输出触发 B Skill
└── Hook 触发 → PreCommit Hook → 自动运行 lint Skill
```

---

## 3. Skill 生命周期管理

### 3.1 生命周期阶段

```text
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌──────────┐    ┌──────────┐
│  创建    │───→│  测试    │───→│  发布    │───→│  使用     │───→│  退役     │
│ Create  │    │  Test   │    │Release  │    │  Use     │    │ Retire   │
└─────────┘    └─────────┘    └─────────┘    └──────────┘    └──────────┘
     │              │              │               │               │
     │              │              │               │               │
     ▼              ▼              ▼               ▼               ▼
  编写 YAML     Mock 测试      语义化版本       收集反馈         标记废弃
  定义步骤      集成测试       Changelog        Bug 修复         迁移指南
  验收标准      回归测试       发布通知         版本迭代         下线公告
```

### 3.2 版本管理

```text
版本格式：MAJOR.MINOR.PATCH

MAJOR：不兼容的 API 变更
  - 修改了步骤间的依赖关系
  - 改变了输出格式
  - 移除了某个步骤

MINOR：向后兼容的功能新增
  - 新增了可选步骤
  - 增加了新的检查维度
  - 添加了新的参数

PATCH：向后兼容的问题修复
  - Prompt 优化
  - 超时参数调整
  - Bug 修复
```

### 3.3 Skill 评审与回滚

```text
评审清单：
├── 工具权限是否最小化？
├── 异常处理是否完善？
├── 验收标准是否可自动化检查？
├── 是否有死循环风险？
├── Token 消耗是否在预算内？
└── 输出格式是否与下游兼容？

回滚策略：
├── 保留最近 3 个版本的 Skill 定义
├── 回滚不影响进行中的任务（完成当前任务再切换版本）
└── 回滚记录：原因、时间、影响范围
```

---

## 4. Plugin 架构与组合

### 4.1 Plugin = 能力的组合包

```text
Plugin 是 WorkBuddy 四层能力模型的 L4 层：

Plugin = MCP Server 配置
       + Skills 定义集合
       + Rules 规则文件
       + Hooks 事件处理
       + 安装配置
       ─────────────────
       = 一个完整的、可安装分发的能力包
```

### 4.2 Plugin 的目录结构

```text
my-plugin/
├── plugin.json              # 插件元信息与清单
├── mcp/
│   ├── servers.json         # MCP Server 配置
│   └── custom-server/       # 自定义 MCP Server 代码
│       └── server.py
├── skills/
│   ├── code-review.skill.yaml
│   ├── doc-generator.skill.yaml
│   └── test-runner.skill.yaml
├── rules/
│   ├── always.md            # 始终生效的规则
│   └── manual.md            # 手动触发的规则
├── hooks/
│   ├── pre-commit.sh        # 提交前执行的 Hook
│   └── on-error.sh         # 错误时的 Hook
├── README.md                # 安装与使用说明
├── CHANGELOG.md             # 版本变更记录
└── LICENSE
```

### 4.3 plugin.json 清单

```json
{
  "name": "dev-toolkit",
  "version": "1.3.0",
  "description": "开发工具集：代码审查、文档生成、自动化测试",
  "author": "dev-team",
  "license": "MIT",
  "requirements": {
    "workbuddy": ">=2.0.0",
    "mcp_protocol": ">=2024-11-05"
  },
  "capabilities": {
    "skills": [
      "code-review",
      "doc-generator",
      "test-runner"
    ],
    "mcp_servers": [
      "filesystem",
      "github"
    ],
    "hooks": [
      "pre-commit"
    ]
  },
  "permissions": {
    "filesystem": ["read", "write:/tmp"],
    "network": ["api.github.com"],
    "commands": ["git", "npm"]
  },
  "config": {
    "github_token": {
      "type": "secret",
      "description": "GitHub Personal Access Token",
      "required": true
    }
  }
}
```

---

## 5. 能力分发与生态

### 5.1 分发模型

```text
分发渠道：
├── 官方市场 → WorkBuddy Plugin Marketplace（审核上架）
├── Git 仓库 → 直接从 GitHub/GitLab 安装
├── 本地文件 → 本地 .mcpb 文件安装
├── 企业私有 → 内部 Registry 分发
└── 项目内置 → 项目 .claude/ 目录下的 Skills 和 Plugins
```

### 5.2 安装流程

```text
用户执行：workbuddy plugin install dev-toolkit

安装步骤：
1. 解析 plugin.json，检查兼容性
2. 安装依赖的 MCP Server（npm install / pip install）
3. 注册 Skills 到 Skill Registry
4. 加载 Rules 到规则引擎
5. 配置 Hooks 到事件系统
6. 提示用户配置必需的 Secret（如 API Key）
7. 完成安装，输出可用能力清单
```

### 5.3 生态角色

```text
┌──────────────────────────────────────────┐
│            WorkBuddy 生态                  │
│                                           │
│  👨‍💻 开发者（Plugin Developer）              │
│    创建 Skill / Plugin，发布到市场          │
│                                           │
│  👤 用户（End User）                        │
│    安装 Plugin，使用 Skill，评价反馈        │
│                                           │
│  🏢 企业管理员（Admin）                     │
│    审核 Plugin，管理权限，监控使用           │
│                                           │
│  🛠️ 平台方（WorkBuddy Team）                │
│    维护市场，制定规范，运营生态              │
└──────────────────────────────────────────┘
```

---

## 6. Skill vs Plugin vs Prompt vs Agent

### 6.1 对比矩阵

| 维度 | Prompt | Agent | Skill | Plugin |
|------|--------|-------|-------|--------|
| **定义** | 给模型的文本指令 | 具有自主能力的 AI 实体 | 任务执行流程模板 | 能力组合包 |
| **粒度** | 单次、临时 | 会话级、有状态 | 任务类型级、无状态 | 应用级、持久化 |
| **复用性** | 低（每次手写） | 中（会话内复用） | 高（跨会话、跨用户） | 最高（可分发） |
| **版本化** | 不支持 | 间接 | 原生支持 | 原生支持 |
| **可组合** | 不可组合 | 可嵌套 | 可嵌套调用 | 可打包多个 Skill |
| **分发** | 复制粘贴 | 不支持 | 通过 Plugin | 通过市场/Git |
| **权限管理** | 无 | 运行时控制 | 声明式声明 | 声明式 + 用户授权 |
| **典型场景** | "帮我写个排序函数" | "帮我管理这个项目" | "每次代码提交前跑审查" | "安装 Java 开发工具包" |

### 6.2 选择决策树

```text
需要做什么？
│
├── 一次性简单任务 → 直接用 Prompt
│   例："把这个 JSON 格式化成表格"
│
├── 需要多轮交互、有状态的对话 → 用 Agent
│   例："帮我从零搭建一个 Spring Boot 项目"
│
├── 需要反复做同一类任务 → 封装为 Skill
│   例："每次写完代码都要做安全审查"
│
└── 需要分享给团队/社区 → 打包为 Plugin
    例："我们团队的代码规范检查工具套装"
```

---

> 🎯 **核心要点**：Skill 是 WorkBuddy 能力体系的核心资产——好的 Skill 将专家经验编码为可执行的、可验证的流程模板。Plugin 则是能力的"集装箱"，让 Skill、MCP、Rules 可以像安装 App 一样分发和复用。

---

**上一模块**：[03-工具调用与 MCP 集成](03-工具调用与MCP集成.md) ｜ **下一模块**：[05-记忆系统与状态管理](05-记忆系统与状态管理.md) ｜ **返回总览**：[00-WorkBuddy 总览](00-WorkBuddy总览.md)
