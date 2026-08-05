# Cursor教程 面试宝典
> 基于课程大纲全面覆盖Cursor AI编程工具面试高频考点，涵盖Tab补全、内联编辑、三大模式、上下文管理、规则配置、Chrome插件开发及AI编程工具对比

## 目录
1. [基础概念速答](#一基础概念速答12-18题)
2. [深度原理剖析](#二深度原理剖析8-12题)
3. [实战场景题](#三实战场景题6-10题)
4. [配置与快捷键题](#四配置与快捷键题5-8题)
5. [系统设计题](#五系统设计题3-5题)
6. [常见坑点与最佳实践](#六常见坑点与最佳实践表格)
7. [面试回答模板](#七面试回答模板top-5)
8. [快速查漏补缺Checklist](#八快速查漏补缺checklist)

---

## 一、基础概念速答（12-18题）

### Q1：Cursor 是什么？与 VS Code 是什么关系？
> Cursor 是一款基于 VS Code 内核深度改造的 AI 原生代码编辑器。它继承了 VS Code 的全部扩展生态、主题和快捷键，同时在编辑器底层深度集成了大语言模型（Claude / GPT-4o 等），提供 Tab 补全、内联编辑（Cmd+K）、Chat / Composer / Agent 三种交互模式。本质上是 "VS Code 的体验 + AI 原生的能力层"。

### Q2：Tab 补全（Tab Completion）的特点是什么？
- **多行预测**：不仅补全当前行，还能预测后续 3-10 行甚至完整函数体
- **上下文感知**：利用项目中的文件类型、函数签名、变量命名风格生成匹配代码
- **即时触发**：光标停留时自动给出灰色建议，按 Tab 接受，无需手动唤醒
- **跨文件关联**：引用其他模块的类型或函数时，补全考虑导入文件中定义

### Q3：内联编辑（Inline Editing）是什么？快捷键是什么？
内联编辑是选中代码后按 **Cmd+K（Mac）/ Ctrl+K（Windows）** 唤起 AI 编辑框，输入自然语言指令直接修改选中代码。修改后以 diff 形式展示变更，开发者决定接受或拒绝。适合局部重构、提取函数、修改变量名等精确操作。

### Q4：Chat 模式（Ask 模式）如何工作？
Chat 模式以侧边栏对话窗口存在，特点：
- 支持 `@file`、`@folder`、`@web`、`@git` 等上下文标记
- 回答包含代码片段时可点击 "Apply" 插入光标位置
- AI 不会主动修改文件，适合问答咨询、代码解读、Bug 排查

### Q5：Composer 模式的核心能力是什么？
Composer（`Cmd+I` / `Ctrl+I`）是**多文件编辑工作区**：
- 一次 prompt 可创建或修改多个文件
- **全项目感知**：自动分析文件间依赖关系
- **实时 diff 预览**：Accept All / Reject All 或逐文件确认
- 适合新增功能模块、生成完整 CRUD、跨文件重构

### Q6：Agent 模式相比 Composer 有何升级？
Agent 模式是 Cursor 最强模式，在 Composer 基础上增加：
- **自动执行命令**：自动运行 `npm install`、`git` 等终端命令
- **日志读取**：捕获终端报错并自动分析修复
- **自动 Lint & Fix**：运行 linter 并修正错误
- **联网搜索**：遇到未知 API 时自动联网查询文档
- **递归修复循环**：写代码 → 运行 → 发现错误 → 修复 → 再运行

> 💡 Agent 模式适用于 "创建一个 Web 应用并启动" 这类端到端任务。

### Q7：Cursor 的上下文标记（@ 符号）有哪些？
| 标记 | 用途 | 典型场景 |
|------|------|----------|
| `@file` | 引用单个文件全部内容 | 让 AI 理解特定文件逻辑 |
| `@folder` | 引用整个文件夹摘要 | 重构时了解模块结构 |
| `@web` | 联网搜索最新文档 | 查询新版 API，避免知识过时 |
| `@git` | 引用 git diff/log 信息 | 对当前变更做 Code Review |
| `@docs` | 引用官方文档库 | 查询框架官方用法 |
| `@code` | 引用特定符号定义 | 精确定位某函数/类的源码 |
| `@terminal` | 引用终端最后输出 | 让 AI 读取报错日志 |

### Q8：.cursorrules 文件的作用？
放置在项目根目录的项目级指令文件，告诉 AI：
- 技术栈（如 "React 18 + TypeScript + Tailwind"）
- 编码规范（如 "函数组件优先"）
- 命名约定（如 "API 路由使用 kebab-case"）
- 禁止模式（如 "不要使用 any 类型"）

优先级：Project Rules（`.cursor/rules/*.mdc`） > `.cursorrules` > Global Rules。

### Q9：MDC 语法是什么？解决了什么问题？
MDC（MarkDown Code rules）是 Cursor 结构化规则文件格式。每个 `.mdc` 文件在 `.cursor/rules/` 目录下，通过 YAML frontmatter 定义元数据：
```yaml
---
description: React 组件规范
globs: src/**/*.tsx, src/**/*.jsx
---
- 使用函数组件 + Hooks
- Props 用 TypeScript interface 定义
- 每个组件一个文件
```
解决了：规则无法分类、无法按文件类型自动激活、团队规范难以分发的问题。

### Q10：Global Rules 与 Project Rules 有何区别？
| 维度 | Global Rules | Project Rules（.mdc） | .cursorrules |
|------|-------------|----------------------|--------------|
| 作用范围 | Cursor 全局所有项目 | 单个项目，按文件类型 | 单个项目根目录 |
| 配置位置 | Settings > Rules for AI | `.cursor/rules/*.mdc` | 项目根目录文件 |
| 语法 | 纯文本 | MDC（YAML + Markdown） | 纯文本 |
| 优先级 | 最低 | 最高 | 中间 |
| 适用场景 | 个人编码偏好 | 团队分类规范 | 项目全局指令 |

### Q11：Cursor 支持哪些底层 AI 模型？
- **Claude 系列**：Claude 3.5 Sonnet、Claude 4（默认，代码生成质量最高）
- **GPT 系列**：GPT-4o、GPT-4o-mini
- **Cursor-small**：自研轻量模型，专为 Tab 补全优化
- 用户可在 Settings > Models 切换默认模型，也可单次对话中切换

### Q12：Privacy Mode（隐私模式）有什么用？
- 代码不用于模型训练
- 请求走 OpenAI/Anthropic API 而非 Cursor 代理
- SOC 2 合规，适合金融、医疗等企业环境
- 开启位置：Settings > Privacy > Privacy Mode

### Q13：Cursor 如何与 GitHub Copilot 对比？
| 对比维度 | Cursor | GitHub Copilot |
|---------|--------|----------------|
| 产品形态 | AI 原生编辑器（基于 VS Code 内核） | VS Code 插件 |
| 多文件能力 | Composer / Agent 模式跨文件编辑 | 仅单文件建议 |
| 上下文引用 | @file @folder @web @git 等丰富标记 | 有限的上下文引用 |
| 模型选择 | Claude + GPT 多模型切换 | 仅 OpenAI Codex 系列 |
| 规则配置 | .cursorrules + MDC 分层规则 | 仅能通过注释暗示 |
| Agent 能力 | 自主执行命令、自动修复循环 | 无 |
| 价格 | $20/月 Pro | $10-39/月 |
| 企业合规 | Privacy Mode + SOC 2 | Azure OpenAI 企业合规 |

### Q14：Cursor 如何与 Windsurf 对比？
| 对比维度 | Cursor | Windsurf |
|---------|--------|----------|
| 底层 IDE | VS Code 深度改造 | VS Code 改造 |
| 核心模型 | Claude + GPT 混合 | 自研 + 第三方模型 |
| 特色模式 | Agent + Composer + Ask | Cascade 流式模式 |
| 规则体系 | .cursorrules + MDC | .windsurfrules |
| 上下文管理 | 手动 @ 标记 + 自动推理 | 自动上下文推理 |
| 社区生态 | 成熟，文档丰富 | 较新，生态建设中 |

### Q15：Cursor 的 "Not Indexed" 状态是什么？
项目文件未完成索引时的状态，此时 AI 对项目理解受限。原因和对策：
- 原因：大型项目索引耗时长
- 解决：等待完成，或手动 `Cmd+Shift+P` > "Cursor: Rescan Index"
- 预防：配置 `.cursorignore` 排除 `node_modules`、`dist` 等目录

### Q16：Cursor 支持 Jupyter Notebook 吗？
支持。Cursor 可编辑 `.ipynb` 文件，AI 能理解 notebook cell 上下文，生成代码和 markdown 文档。

### Q17：什么是 "Apply" 功能？
Chat 或 Composer 返回代码建议时，点击 Apply 按钮将建议以 diff 形式嵌入当前编辑器，逐行审阅后再确认接受。比直接粘贴更安全、可追溯。

### Q18：Cursor 终端支持 AI 辅助吗？
支持。终端命令报错后，右上角出现 "Fix with AI" 按钮，点击后 AI 自动读取错误输出并生成修复方案。Agent 模式下可自动执行修复。

---

## 二、深度原理剖析（8-12题）

### Q19：Tab 补全底层实现原理是什么？
Cursor 使用自研 **Cursor-small** 轻量 Transformer 模型，工作流程：
1. **Token 级预测**：在光标左右构建上下文窗口
2. **多候选生成**：同时生成多个候选补全序列
3. **排序过滤**：按语法正确性、项目风格匹配度排序
4. **延迟渲染**：用户暂停输入 150-300ms 后渲染灰色建议
5. **差异合并**：接受补全时智能合并避免重复

模型经大量代码语料预训练 + 用户反馈微调，形成跨文件感知能力。

### Q20：Chat / Composer / Agent 三种模式的技术架构差异
```
                    ┌──────────────────────────────────────┐
                    │          Cursor AI Engine             │
                    ├──────────┬───────────┬───────────────┤
                    │   Chat   │  Composer │    Agent      │
                    ├──────────┼───────────┼───────────────┤
                    │ 单轮问答  │ 多文件编辑 │ 多文件+命令执行│
                    │ 只读分析  │ 生成diff  │ 自动循环修复   │
                    │ Apply单块 │ AcceptAll │ 完全自主      │
                    │ 无状态    │ 有会话状态 │ 有执行循环状态│
                    └──────────┴───────────┴───────────────┘
```
- **Chat**：无状态或少状态，适合独立的问答咨询
- **Composer**：维护编辑会话状态，跨文件 diff 缓存
- **Agent**：完整 Observe → Plan → Act → Evaluate 循环，工具调用权限最全

### Q21：Agent 模式基于什么框架实现自主规划？
基于 **ReAct（Reasoning + Acting）框架**：
1. **观察（Observe）**：读取工作区状态、文件内容、终端输出
2. **推理（Reason）**：分析用户需求，分解为子任务，规划执行顺序
3. **行动（Act）**：选择工具（读文件、写文件、终端命令、搜索）
4. **反馈（Evaluate）**：获取执行结果，判断是否完成
5. **循环（Loop）**：未完成则调整计划继续执行

Agent 维护一个**任务队列**，支持优先级动态调整，最大迭代次数默认 10 次。

### Q22：Cursor 的上下文窗口管理策略是什么？
底层模型上下文窗口有限（如 128K），Cursor 通过以下策略优化：
1. **智能截断**：超出窗口时优先保留文件头部（import）和尾部（主逻辑）
2. **相关性排序**：基于 TF-IDF 和引用图对上下文片段排序，丢弃低相关片段
3. **增量更新**：对话中只发送变化的 token，非全部重发
4. **用户标记优先**：`@file` `@folder` 等显式标记的内容权重最高，不被截断
5. **分层缓存**：不常变动的文件缓存在本地向量数据库

### Q23：MDC 规则的匹配注入机制
```
用户打开 src/components/Button.tsx
  │
  ▼
Cursor 扫描 .cursor/rules/*.mdc 所有文件
  │
  ▼
解析每个 .mdc 的 YAML frontmatter 中 globs 字段
  │
  ▼
globs 匹配 src/**/*.tsx → 匹配成功
  │
  ▼
将该 .mdc 规则内容注入 AI 的 system prompt
  │
  ▼
后续 AI 回答遵循该规则
```
多文件匹配时按字母序合并注入。

### Q24：Cursor 项目索引是如何构建的？
```
1. 扫描项目所有文件（排除 .cursorignore 中路径）
2. 为每个文件构建 AST（抽象语法树）
3. 提取符号（函数、类、变量名、接口定义）
4. 构建引用图（哪些文件引用哪些符号）
5. 生成向量嵌入（用于语义搜索）
6. 持久化索引到 .cursor/ 目录
7. 文件变更时增量更新（而非全量重建）
```
索引完成后，AI 可通过符号名直接定位定义位置，即使定义在另一个文件中。

### Q25：为什么 Cursor 的代码生成质量优于通用 ChatGPT？
1. **项目上下文**：ChatGPT 只能看粘贴片段，Cursor 看整个项目
2. **索引感知**：维护符号索引，可精准引用跨文件定义
3. **编辑感知**：理解文件编辑历史，区分用户手写和 AI 生成
4. **风格一致**：通过 `.cursorrules` 复制项目既有代码风格
5. **多文件联动**：修改一个函数时自动更新所有引用处

### Q26：Cursor 如何处理大型项目的性能问题？
- **`.cursorignore` 排除**：排除 `node_modules`、`dist` 等
- **增量索引**：只对变更文件重新索引
- **懒加载**：非当前编辑文件按需加载
- **本地优先**：补全推理本地缓存，减少 API 调用
- **索引分片**：大型项目分片索引，按需查询

### Q27：Cursor 如何保证安全？
1. **Privacy Mode**：代码不用于训练
2. **沙箱执行**：Agent 执行的命令在本地环境，用户可确认后再执行
3. **diff 审阅**：所有代码变更通过 diff 展示，逐行确认
4. **企业 SSO**：支持单点登录
5. **审计日志**：企业版记录所有 AI 交互

---

## 三、实战场景题（6-10题）

### Q28：如何用 Cursor 快速搭建 React + TypeScript 项目？
```
Agent 模式 prompt：
"创建一个 React + TypeScript 项目，使用 Vite 构建。
- 配置 Tailwind CSS
- 创建 components / pages / hooks / utils 目录
- 添加 react-router-dom 路由示例
- 配置 ESLint + Prettier
在终端中运行初始化命令并启动。"
```
Agent 自动执行 `npm create vite`、`npm install` 等命令，创建所有配置文件。

### Q29：如何重构一个复杂函数？
```
步骤：
1. 选中要重构的函数代码
2. Cmd+K（内联编辑）
3. 输入："将此函数拆分为 3 个单一职责的子函数，保持原有 API"
4. 查看 diff 确认
5. 如需跨文件调整，切换 Composer 模式处理
```

### Q30：如何在 Cursor 中调试 Python 报错？
```
1. 运行脚本 → 终端报错
2. 点击终端右上角 "Fix with AI"
3. AI 自动读取错误栈和上下文
4. 若用 Agent 模式：自动修复并重新运行
5. 若用 Chat 模式：给出修复方案，手动 Apply
```

### Q31：如何使用 Cursor 开发 Chrome 插件？
```
Composer prompt：
"创建 Chrome 浏览器插件（Manifest V3），包含：
- manifest.json（权限最小化，仅声明所需权限）
- popup.html + popup.js（弹出窗口 UI）
- content.js（内容脚本，注入页面）
- background.js（Service Worker）
功能：选中文字后右键菜单发送到自定义 API 并展示结果"
```
Cursor 生成完整目录结构，Agent 模式下可自动打包和初步验证。

### Q32：如何用 `.cursorrules` 统一团队代码风格？
```markdown
# Java 团队规范
## 命名
- 类名 PascalCase（UserService）
- 方法名 camelCase（getUserById）
- 常量 UPPER_SNAKE_CASE（MAX_RETRY）

## 约束
- 禁止 System.out.println，使用 Logger
- Service 层必须写接口
- Controller 使用 @Valid 校验入参
- SQL 写在 XML 中，禁止拼接字符串

## 代码风格
- 缩进 4 空格，行宽 120
- import 禁止使用通配符 *
```
文件放入项目根目录，团队所有成员的 AI 遵循同一规范。

### Q33：如何用 Cursor 做 Code Review？
```
Chat 模式 + @git：
"@git 对当前 diff 做 Code Review，关注：
1. 空指针或类型安全风险
2. 性能问题（不必要的渲染、复杂循环）
3. 代码重复
4. 边界条件覆盖
给出严重评级：Critical / Major / Minor"
```

### Q34：大型 Monorepo 中使用 Cursor 的技巧？
1. **`.cursorignore`** 排除无关 package 的 `node_modules`
2. **`.cursorrules`** 定义 monorepo 结构，说明 package 间依赖
3. 善用 **`@folder`** 标记相关 package
4. 缩小索引范围：手动指定需要索引的目录
5. **Project Rules 分目录**：每个 package 放独立 `.mdc` 规则

### Q35：如何使用 Cursor 创建 REST API？
```
Agent prompt：
"使用 Express + Prisma + PostgreSQL 创建 REST API。
- User 和 Post 模型（一对多）
- 全部 CRUD 路由
- zod 入参校验
- 生成 Prisma migration
- Swagger 文档"
```

### Q36：如何利用 Cursor 进行数据库 Schema 设计与代码同步？
```
Composer prompt：
"设计用户系统数据库表：users、roles、permissions，
生成建表 SQL 和对应的 JPA Entity + Repository 接口。"
```
Cursor 保证字段类型、关联关系在 SQL 和 Java 代码中一致。

---

## 四、配置与快捷键题（5-8题）

### Q37：Cursor 核心快捷键大全
| 快捷键 | 功能 | 说明 |
|--------|------|------|
| `Ctrl+K` / `Cmd+K` | 内联编辑 | 选中代码后唤起 AI 编辑框 |
| `Ctrl+L` / `Cmd+L` | 打开 Chat | 侧边栏问答模式 |
| `Ctrl+I` / `Cmd+I` | 打开 Composer | 多文件编辑工作区 |
| `Ctrl+Shift+I` / `Cmd+Shift+I` | 打开 Agent | 带命令执行能力的 Composer |
| `Tab` | 接受 AI 补全 | 接受灰色建议 |
| `Ctrl+Enter` / `Cmd+Enter` | 接受全部变更 | Composer 中批量 Accept |
| `Ctrl+Shift+Enter` / `Cmd+Shift+Enter` | Apply 选中代码 | Chat 中 Apply 代码到光标 |
| `Ctrl+Shift+P` / `Cmd+Shift+P` | 命令面板 | 输入 "Cursor" 查看所有命令 |
| `Ctrl+Shift+R` / `Cmd+Shift+R` | 重新扫描索引 | 索引异常时使用 |
| `Alt+[/]` | 跳转到下一个/上一个编辑点 | Tab 补全后快速导航 |

### Q38：如何配置 Project Rules（.mdc 规则）？
```bash
# 1. 创建规则目录
mkdir -p .cursor/rules

# 2. 创建规则文件，如 frontend.mdc
```

```yaml
# .cursor/rules/frontend.mdc
---
description: React + TypeScript 开发规范
globs: src/**/*.tsx, src/**/*.ts
---
- 使用 React 18 函数组件，禁用 class 组件
- Props 类型用 interface 定义并导出
- 使用 Tailwind CSS
- 事件处理函数用 useCallback 包裹
- 组件文件不超过 200 行
```

```bash
# 3. 验证：打开 src/ 下 .tsx 文件时规则自动注入
```

### Q39：如何配置 Global Rules？
1. 打开 Settings：`Cmd+Shift+P` → "Preferences: Open Settings (UI)"
2. 导航至：General > Rules for AI
3. 输入全局规则：
```text
Always use TypeScript strict mode.
Use async/await instead of .then().
Write unit tests for all utility functions.
Use descriptive English variable names.
```
4. 保存后立即生效，适用于所有项目

### Q40：.cursorignore 配置最佳实践
```text
# .cursorignore
node_modules
dist
build
.next
.git
*.log
coverage
.cache
__pycache__
*.pyc
vendor
.venv
target
*.class
```

> ⚠️ 大型项目中务必配置 `.cursorignore`，否则 Cursor 会尝试索引整个 `node_modules` 导致严重卡顿。

### Q41：如何配置 AI 模型切换？
- **全局默认**：Settings > Models > Default model
- **单次切换**：Chat / Composer 输入框底部点击模型名称下拉菜单
- **按任务选择**：代码生成用 Claude，简单问答用 GPT-4o-mini 节省配额
- **长对话用大窗口模型**：复杂跨文件任务选择上下文窗口大的模型

### Q42：Cursor 的 Rules for AI 配置项详解
| 配置项 | 位置 | 说明 | 示例 |
|--------|------|------|------|
| Global Rules | Settings > General > Rules for AI | 全局系统 prompt，所有项目生效 | "使用 TypeScript 严格模式" |
| .cursorrules | 项目根目录 | 项目级规则文件 | 技术栈、架构约定 |
| .cursor/rules/*.mdc | `.cursor/rules/` | 按文件类型分类的模块规则 | 前端规范、后端规范、测试规范 |
| @Rules 临时规则 | 对话中 | 临时引入，优先级最高 | "本次对话不要使用 lodash" |

---

## 五、系统设计题（3-5题）

### Q43：设计一个企业级 AI 编码规范治理平台
**需求**：50+ 开发者的团队需要统一的 AI 编码规范。

**方案架构**：
```
企业 AI 编码规范治理平台
├── 规则仓库（Git 管理）
│   ├── base.mdc          → 通用规范（所有项目）
│   ├── frontend.mdc      → React/Vue 规范
│   ├── backend.mdc       → Java/Spring 规范
│   ├── security.mdc      → OWASP 安全规范
│   └── test.mdc          → 测试规范
├── CI/CD 流水线
│   ├── 规则同步（自动拉取最新规则到各项目）
│   ├── 合规检查（AI 生成代码是否符合规则）
│   └── PR 审批流程
└── 审计与监控
    ├── AI 交互日志
    ├── 违规统计报表
    └── 开发者反馈收集
```
**关键设计**：
- 规则通过 Git 子模块或 npm package 分发到各项目
- 项目 `.cursorrules` 只保留特有配置，通用规则从中心同步
- CI 检查 AI 生成代码的规则合规性

### Q44：设计基于 Cursor Agent 的自动化测试生成系统
**流程**：
```text
1. 指定模块：src/services/OrderService.ts
2. Agent 分析文件：
   - 提取所有导出函数/类签名
   - 识别外部依赖和 mock 点
   - 分析输入输出类型
3. Agent 生成测试文件：
   - Unit test: 正常路径 + 边界 + 错误路径
   - Mock: 自动生成依赖 mock
   - 覆盖率目标 >= 80%
4. Agent 运行测试：
   - 执行 npm test
   - 收集覆盖率报告
   - 补充缺失用例
5. 输出测试报告
```

### Q45：设计 Cursor + Chrome 插件开发的完整工作流
```
开发工作流：
+------------------+       +------------------+
|   Cursor IDE      |       |  Chrome 浏览器    |
|  + Agent 模式     |       |  + popup UI      |
|  + 代码生成        |       |  + content script |
|  + 调试阶段        |       |  + Service Worker |
+--------+---------+       +--------+---------+
         |                           |
         +--- 生成代码 ---→ 加载解压扩展
         |                           |
         ←--- 调试反馈 ---  开发者工具调试
         |                           |
         +--- 修复问题 ---→ 重新加载
         |                           |
         +--- 打包发布 ---→ Chrome Web Store
```
**关键步骤**：
1. Cursor Agent 生成插件完整代码
2. 终端中 `npm run build`
3. Chrome 加载 `dist` 目录调试
4. 修改问题直接在 Cursor 完成
5. 发布前安全检查（权限最小化、CSP 策略）

### Q46：如何为团队做 Cursor vs Copilot vs Windsurf 选型评估？
| 评估维度 | Cursor（推荐） | GitHub Copilot | Windsurf |
|---------|-------------|----------------|----------|
| 团队编码规范一致性 | 高（.cursorrules + MDC） | 低（仅靠注释暗示） | 中（.windsurfrules） |
| 多文件复杂任务 | 强（Agent + Composer） | 弱（单文件建议） | 中（Cascade） |
| 模型灵活性 | 高（Claude/GPT 切换） | 低（仅 OpenAI） | 中（自研+第三方） |
| 数据安全 | Privacy Mode + SOC 2 | Azure 企业合规 | 企业版可用 |
| 中文理解 | 优秀（Claude 天然优势） | 一般 | 良好 |
| 价格 | $20/月 Pro | $10-39/月 | $15/月 Pro |
| 团队协作 | 规则文件可入库 | 无团队规则机制 | 规则文件可入库 |

> 🎯 选型建议：注重 AI 原生体验和团队规范选 Cursor；深度绑定 GitHub 生态选 Copilot；追求简洁自动推理选 Windsurf。

---

## 六、常见坑点与最佳实践（表格）

### 常见坑点

| 坑点 | 现象 | 原因 | 解决方案 |
|------|------|------|----------|
| AI 生成不存在的 API | 代码运行报错 | 模型训练数据过时 | 使用 `@web` 联网搜索最新文档 |
| 代码有安全漏洞 | SQL 注入、XSS 等 | 模型重功能轻安全 | `.cursorrules` 中加入安全规则 |
| 大型项目响应慢 | 编辑器卡顿 | 索引占用资源 | 配置 `.cursorignore` |
| Agent 跑偏 | 做了多余的事情 | 任务分解不清晰 | 拆分为多个小任务 |
| Cmd+K 改错代码 | 修改了无关部分 | 选中范围不精确 | 精确选择代码行，确认 diff |
| import 路径错误 | 跨文件重构后编译失败 | Composer 未完全理解结构 | 加指令"检查所有 import 路径" |
| 规则不生效 | AI 不按规范编码 | 文件位置/格式错误 | 检查 `.cursorrules` 在项目根目录 |
| 多人规则冲突 | 不同成员 AI 行为不同 | 全局规则不一致 | 规则纳入 Git 管理 |
| 中文注释/命名 | 代码中混合中文 | 未指定语言规范 | 规则中明确"全部使用英文命名" |
| Privacy Mode 无效 | 代码仍被使用 | 未正确开启 | 检查 Settings > Privacy |
| 补全频繁干扰 | 灰色建议影响阅读 | 补全粒度过大 | 调整补全延迟或关闭自动触发 |
| 搜索结果不相关 | @web 返回无关内容 | 检索词不精确 | 细化搜索关键词 |

### 最佳实践速查表

| 实践 | 具体做法 | 收益 |
|------|----------|------|
| 规则精细拆分 | 按文件类型分多个 .mdc | 规则精准，节省上下文 |
| 最小上下文 | 只提供 AI 真正需要的文件 | 降低成本，提高准确率 |
| 分步提问 | 复杂任务拆分为子任务 | 避免上下文溢出 |
| Review diff | 接受前总是检查 diff | 避免引入 bug |
| prompt 模板化 | 维护常用 prompt 模板 | 提高效率，保证一致性 |
| 规则纳入版本控制 | `.cursor/rules/` 入 Git | 团队共享，历史可追溯 |
| 冷启动测试 | 先让 AI 生成骨架，再精调 | 快速搭建基础结构 |
| 指定版本号 | prompt 中明确框架版本 | 避免生成过时代码 |

---

## 七、面试回答模板（Top 5）

### 模板1：介绍 Cursor 与 VS Code 的关系
> "Cursor 是基于 VS Code 内核构建的 AI 原生代码编辑器。它兼容 VS Code 的全部扩展和快捷键，但在此基础上深度集成了 LLM，提供了 Tab 补全、内联编辑、Chat 问答、Composer 多文件编辑和 Agent 自主编程五种能力。VS Code 是一个编辑器，Cursor 是一个自带 AI 结对编程伙伴的编辑器。两者的关系可以理解为——Cursor 保留了 VS Code 的皮，换上了 AI 原生的骨。"

### 模板2：解释 Chat / Composer / Agent 三种模式的递进关系
> "这三种模式代表了 AI 辅助编程的三个递进层级。Chat 用于问答和代码解释，AI 只读不写；Composer 用于多文件编辑，可以同时创建和修改多个文件，但需要手动运行命令；Agent 在 Composer 基础上增加了命令执行、错误检测和自动修复循环，可以自主完成从写代码到运行验证的全流程。选择标准是任务复杂度——简单咨询用 Chat，多文件开发用 Composer，复杂自动化流程用 Agent。"

### 模板3：如何保证 Cursor 生成代码的质量和安全
> "我有四个层面的保障：第一，通过 .cursorrules 预定义安全规范和代码风格，从源头减少问题；第二，所有 AI 生成代码以 diff 形式展示，我坚持逐行 Review 后再接受；第三，关键模块先让 AI 写单元测试再写实现，用测试验证正确性；第四，启用 Privacy Mode 确保代码安全。核心原则是：AI 生成 + 人工审查 + 自动化测试，三道防线缺一不可。"

### 模板4：如何用 Cursor 提升团队开发效率
> "我从三个维度推动团队采纳：技术维度——建立统一的 .cursor/rules/ 规则库并纳入 Git 管理，确保所有人的 AI 行为一致；流程维度——推行 AI 生成 -> 人工 Code Review -> CI 自动化测试的工作流，AI 提效、人工保质量、CI 守底线；培训维度——编写团队 AI 辅助开发指南，分享高效 prompt 模式和快捷键技巧。企业版还会开启审计日志和 Privacy Mode 满足合规要求。"

### 模板5：介绍 Cursor 上下文管理机制的高级用法
> "Cursor 的上下文管理是多层的。最底层是自动项目索引，扫描整个项目的 AST 和符号引用图；中间层是 @ 标记系统，用户通过 @file、@folder、@web、@git 显式引入上下文；最上层是规则系统，包括 Global Rules、Project Rules 和 .cursorrules，在每次请求时作为系统指令注入。高级用法是组合使用——例如在做跨文件重构时，@folder 引入模块结构、@git 了解历史变更、再加一条临时 @Rules 约束本次重构风格。这三层叠加使得 Cursor 对项目的理解远超通用 AI 工具。"

---

## 八、快速查漏补缺Checklist

### 基础概念
- [ ] 能说清 Cursor 与 VS Code 的关系
- [ ] 知道 Tab 补全、内联编辑（Cmd+K）、Chat、Composer、Agent 五种能力
- [ ] 理解 @file / @folder / @web / @git / @docs / @code / @terminal 七种标记
- [ ] 知道 .cursorrules 的作用和配置位置
- [ ] 了解 MDC 语法和 YAML frontmatter 结构

### 规则层次
- [ ] 清楚 Global Rules / Project Rules / .cursorrules 的优先级
- [ ] 知道如何创建 .cursor/rules/*.mdc 文件
- [ ] 能解释 globs 字段作用
- [ ] 知道如何配置 .cursorignore

### 快捷键
- [ ] Cmd+K（内联编辑）
- [ ] Cmd+L（Chat）
- [ ] Cmd+I（Composer）
- [ ] Cmd+Shift+I（Agent）
- [ ] Cmd+Shift+P（命令面板）
- [ ] Cmd+Shift+R（重新索引）

### 高级功能
- [ ] 理解 Agent ReAct 循环的原理（Observe → Reason → Act → Evaluate）
- [ ] 知道 Privacy Mode 如何开启
- [ ] 了解索引构建流程（AST → 符号 → 引用图 → 向量嵌入）
- [ ] 知道如何切换 AI 模型
- [ ] 了解上下文窗口管理策略（截断、排序、增量更新）

### 实战能力
- [ ] 能用 Cursor 快速搭建全栈项目
- [ ] 能配置团队级编码规范
- [ ] 能用 Cursor 开发 Chrome 插件
- [ ] 能用 Cursor 做 Code Review
- [ ] 能解释 Chrome 插件开发调试与发布全流程

### 对比分析
- [ ] 能对比 Cursor vs GitHub Copilot vs Windsurf
- [ ] 能说明 Cursor 生成质量优于通用 ChatGPT 的原因
- [ ] 能为团队做 AI 编程工具的选型评估

### 坑点意识
- [ ] 知道规则冲突的排查方法
- [ ] 知道大型项目性能问题的优化手段
- [ ] 知道 Agent 模式可能无限循环及其限制

> 🎯 **面试复盘**：Cursor 面试题的核心考察不是功能列表背诵，而是对 "AI 如何赋能开发者" 的理解深度。回答时应突出三个维度：上下文管理策略（如何让 AI 理解项目）、规则治理体系（如何约束 AI 行为）、人机协作工作流（如何将 AI 融入开发流程）。能结合团队管理和工程实践的候选人会获得更高评价。
