# 06 - Cursor AI 编程指南

> 🎯 Cursor 是 2025 年最火的 AI IDE——不是"带 AI 插件的编辑器"，而是"AI 为核心的编程环境"。Tab 补全、Composer 多文件编辑、Agent 自主模式，三位一体的 AI 编程体验

---

## 目录

1. [Cursor 核心功能](#1-cursor-核心功能)
2. [Composer：多文件编辑](#2-composer多文件编辑)
3. [Agent 模式](#3-agent-模式)
4. [规则与配置](#4-规则与配置)

---

## 1. Cursor 核心功能

```text
Cursor = VS Code 内核 + AI 深度集成

三大 AI 功能：
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ Tab 补全      │  │ Composer     │  │ Chat         │
│ (单文件建议)   │  │ (多文件编辑)  │  │ (对话问答)    │
└──────────────┘  └──────────────┘  └──────────────┘

快捷键：
  Tab     → 接受 AI 建议
  Ctrl+K  → 内联编辑（选中代码 → 用自然语言修改）
  Ctrl+L  → 侧边栏 AI Chat
  Ctrl+I  → Composer（多文件编辑）
  Ctrl+Shift+Y → Agent 模式
```

## 2. Composer：多文件编辑

```text
Composer 是 Cursor 的核心差异点：

传统 AI：一次只生成一段代码
Composer：一次性创建/修改多个文件

示例："创建一个 Spring Boot 用户管理模块"
→ Composer 生成：
  ├── User.java (实体)
  ├── UserRepository.java
  ├── UserService.java
  ├── UserController.java
  └── application.yml 更新

→ 一次性生成 5 个文件，自动判断依赖关系
```

## 3. Agent 模式

```text
Agent 模式 = AI 自主编程

Agent 能做什么：
  → 读取项目文件（理解项目结构）
  → 运行终端命令（npm install / mvn test）
  → 执行 Git 操作（commit / branch）
  → 查看错误信息 → 自主修复
  → 迭代改进直到代码能运行

Agent 循环：
  理解任务 → 修改代码 → 运行测试 → 看报错 → 修复 → 再测试 → ✅

关键：Agent 不是一次生成，而是"迭代直到能跑"
```

## 4. 规则与配置

```text
.cursorrules — 项目级 AI 行为配置（类似 CLAUDE.md）

示例 .cursorrules：
  你是一个 Java 17+ 后端开发专家。
  - 始终使用 Spring Boot 3.x 最佳实践
  - 所有 public 方法加 Javadoc
  - 使用 Lombok 减少样板代码
  - 测试用 JUnit 5 + Mockito
  - 数据库操作加 @Transactional
  - 响应实体用 Java Record

.cursorignore — 排除不需要 AI 关注的文件：
  *.log
  target/
  node_modules/
```

```text
模型选择：
  → GPT-4o / Claude Opus → 复杂重构/架构设计
  → Claude Sonnet → 日常编码（推荐默认）
  → GPT-4o-mini → Tab 补全（快速+便宜）
  → 本地 Ollama → 免费但效果一般
```

## 核心要点回顾

- Tab = 单行补全，Composer = 多文件编辑，Agent = 自主迭代
- `.cursorrules` = 项目级的 AI 行为指南
- Agent 的核心价值 = 迭代直到代码能跑（不是一次生成）
- 日常编码选 Sonnet（性价比），复杂重构选 Opus
- Cursor = VS Code + 原生 AI 能力（不是插件堆叠）

## 参考资料

1. Cursor 官方文档 — docs.cursor.com
2. Cursor Rules 最佳实践
