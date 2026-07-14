# Claude Code Skill 系统与斜杠命令详解

> **核心摘要**：Skill（技能/插件）是 Claude Code 的功能扩展包，为特定任务提供预设的指令、工具链和工作流。本文涵盖内置命令速查和常用 Skill 实战流程。

---

## 一、Skill 是什么

Skill 是 Claude Code 的功能扩展包，为特定任务提供专门的指令、工具链和工作流。

```
普通对话：通用 AI 能力，每次要从零开始描述需求
加载 Skill：预设了领域知识 + 工作流 + 工具组合，一步到位
```

### 1.1 加载方式

- **方式一**：用户输入 `/skill-name`，如 `/review`
- **方式二**：AI 根据任务自动匹配
- **方式三**：通过 Skill 工具显式调用

## 二、内置 Slash Commands 速查

### 对话控制

| 命令 | 作用 |
|------|------|
| `/help` | 查看帮助 |
| `/clear` | 清空当前对话 |
| `/compact` | 压缩上下文（释放 Token） |
| `/resume` | 恢复上一个被压缩的对话 |

### 配置与信息

| 命令 | 作用 |
|------|------|
| `/config` | 配置主题、模型 |
| `/cost` | 查看 Token 消耗 |
| `/context` | 查看当前上下文大小 |
| `/doctor` | 诊断环境问题 |
| `/memory` | 打开记忆管理 |

### 工作流命令

| 命令 | 作用 |
|------|------|
| `/init` | 初始化项目 CLAUDE.md |
| `/review` | 代码审查当前分支 |
| `/commit` | 创建 git commit |
| `/pr` | 创建 Pull Request |
| `/simplify` | 简化/重构代码 |
| `/security-review` | 安全审查 |

### 自动化

| 命令 | 作用 |
|------|------|
| `/loop` | 定时循环执行命令 |
| `/fast` | 切换快速模式 |

## 三、可用 Skill 分类

### 开发类

| Skill | 触发 | 功能 |
|-------|------|------|
| `code-review` | `/review` | 多层级代码审查 |
| `feature-dev` | 开发新功能 | 引导式功能开发 |
| `commit` | `/commit` | 规范提交 |
| `pr` | `/pr` | 创建 PR |
| `init` | `/init` | 初始化 CLAUDE.md |
| `security-review` | 安全审查 | 安全漏洞检查 |

### 文档类

| Skill | 功能 |
|-------|------|
| `pdf` | 读取/创建/合并/拆分 PDF |
| `docx` | 创建/编辑 Word 文档 |
| `pptx` | 创建/编辑演示文稿 |

### 设计类

| Skill | 功能 |
|-------|------|
| `frontend-design` | 高质量 UI 界面 |
| `canvas-design` | 视觉设计 |

## 四、常见 Skill 实战

### /review — 代码审查

1. Skill 加载代码审查专用 prompt
2. 读取 git diff
3. 启动多个审查子代理（安全/性能/测试/风格）
4. 汇总发现 + 合并去重
5. 输出审查报告

### /commit — 智能提交

1. 检查 git status + git diff
2. 分析改动内容
3. 根据仓库 commit 风格生成 message
4. 创建 commit

### /loop — 循环任务

```
/loop 5m /check-ci     → 每 5 分钟检查 CI 状态
/loop                  → 自定节奏循环
```

## 核心要点回顾

- Skill 是功能包，`/command` 是触发 Skill 的方式
- Skill 优势：预设工作流和领域知识，避免每次从零开始
- 最常用 Skill：`/review`（审查）、`/commit`（提交）、`/init`（初始化）、`/plan`（规划）
- 可通过 `skill-creator` skill 创建自定义 Skill
- Skill 工作原理：注入 System Prompt + 接管对话 → 执行工作流 → 返回标准模式

## 参考资料

1. Anthropic 官方文档 - Claude Code Skills
2. Claude Code 斜杠命令参考
