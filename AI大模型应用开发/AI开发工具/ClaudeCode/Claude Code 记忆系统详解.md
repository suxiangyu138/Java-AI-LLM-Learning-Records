# Claude Code 记忆系统详解（实战版）

> **文档定位**：Claude Code CLI 记忆系统深度解析
> **核心问题**：Claude Code 如何"记住"用户偏好和项目信息？如何在跨会话复用知识？

---

## 一、记忆系统架构

```
Claude Code 记忆系统
│
├── CLAUDE.md（项目级指令）
│   └── 项目根目录，每次对话自动加载
│
├── .claude/memory/（持久化记忆文件）
│   ├── MEMORY.md        ← 记忆索引
│   ├── user_role.md     ← 用户角色
│   ├── feedback_test.md ← 用户反馈
│   ├── project_xxx.md   ← 项目背景
│   └── reference_yyy.md ← 外部引用
│
├── settings.json（配置记忆）
│   └── 权限、钩子、偏好设置
│
└── 会话上下文（短期记忆）
    └── 当前对话历史和操作轨迹
```

---

## 二、四种记忆类型

| 类型 | 何时保存 | 示例 |
|---|---|---|
| **user** | 了解用户角色、偏好、技术背景 | "他是 Java 后端，熟悉 Spring Boot" |
| **feedback** | 用户纠正或确认你的工作方式 | "不要在回复末尾加总结" |
| **project** | 项目背景、目标、约束、时间线 | "下周四前冻结代码合并" |
| **reference** | 外部系统资源指针 | "bug 追踪在 Linear 项目 INGEST" |

---

## 三、记忆文件格式

### 3.1 文件模板

```markdown
---
name: user-role-java-backend
description: 用户是 Java 后端工程师，SpringBoot 全栈方向
metadata:
  type: user
---

用户是 Java 后端开发，5 年经验，主攻 SpringBoot + 微服务。
正在同时学习 AI 大模型开发（Agent/RAG/MCP），目标是 Java + AI 混合架构。

[[project-java-ai-learning]] [[feedback-no-trailing-summary]]
```

### 3.2 关键字段

| 字段 | 说明 |
|---|---|
| `name` | 唯一标识（kebab-case） |
| `description` | 用于判断何时读取该记忆 |
| `metadata.type` | user / feedback / project / reference |
| 正文 | 记忆具体内容，Why + How to apply |
| `[[link]]` | 关联其他记忆 |

---

## 四、MEMORY.md —— 记忆索引

```markdown
- [用户角色：Java后端 AI全栈](user_role.md) — 5年Java经验，正在学Agent开发
- [反馈：简洁回复](feedback_concise.md) — 不要写多行注释和尾部总结
- [项目：学习笔记仓库](project_learning_repo.md) — 知识仓库持续更新中
- [参考：微服务文档](reference_microservice_docs.md) — Confluence 地址
```

**原则**：MEMORY.md 只是索引（每行 < 150 字符），正文在各 `*.md` 文件中。

---

## 五、什么时候写入记忆

### 5.1 User 类型

```
触发：了解了用户的角色、偏好、技术背景
内容：用户是什么角色、用什么技术栈、当前在做什么
```

### 5.2 Feedback 类型

```
触发：用户纠正了我的行为，或确认了一个不符合直觉的做法
结构：
  - 规则本身
  - Why：为什么这么做（往往是过去踩过的坑）
  - How to apply：什么时候适用这条规则
```

### 5.3 Project 类型

```
触发：了解了项目的目标、时间线、约束
结构：
  - 事实/决策
  - Why：动机（约束、截止日期、需求方要求）
  - How to apply：如何影响后续决策
注意：项目记忆变化快，记录原因而非状态快照
```

### 5.4 Reference 类型

```
触发：知道了外部系统中资源的地址
示例：
  - "Pipeline bugs 追踪在 Linear 项目 INGEST"
  - "Oncall 看 grafana.internal/d/api-latency 面板"
```

---

## 六、记忆的生命周期

```
当前会话 → 写入 .claude/memory/
           ↓
未来会话 → 系统自动加载 .claude/memory/MEMORY.md
           ↓
         → 根据 description 选择性加载相关记忆
           ↓
需要验证 → 文件路径/函数名/外部 URL 需要当前验证
           ↓
过时清理 → 与代码现状冲突时更新或删除
```

---

## 七、记忆与 CLAUDE.md 的区别

| 维度 | CLAUDE.md | Memory 系统 |
|---|---|---|
| **粒度** | 项目级通用指令 | 细粒度话题记忆 |
| **变化频率** | 低频（项目基本不变） | 中高频（学习/纠正/决策） |
| **内容** | 技术栈/规范/命令 | 用户偏好/项目背景/外部参照 |
| **加载时机** | 每次对话全量加载 | 按需选择性加载 |
| **维护方式** | 手动编辑 | AI 自动写入 |
| **和 Git 关系** | 通常提交到 Git | 通常不提交（个人/敏感） |

---

## 八、记忆最佳实践

```markdown
✅ 写记忆（Do）：
  - 用户纠正你的行为时，立刻写入 feedback
  - 了解到新的外部工具地址时，写入 reference
  - 用户告诉你他的角色和背景时，写入 user
  - 知道项目时间线和约束时，写入 project

❌ 不写记忆（Don't）：
  - 代码模式、目录结构（读代码就能知道）
  - Git 历史、提交信息（git log 就能知道）
  - 当前对话中的临时状态（换会话无用）
  - 已写在 CLAUDE.md 中的内容
  - 修复 bug 的具体步骤（修好就完了）
```

---

## 九、核心要点

1. **四种记忆类型？** user（角色） / feedback（工作方式） / project（项目上下文） / reference（外部指针）
2. **MEMORY.md 是什么？** 记忆文件的索引，每行一条，不超过 150 字符
3. **memory 和 CLAUDE.md 区别？** CLAUDE.md 是项目指令，memory 是 AI 自动积累的知识
4. **什么时候不写记忆？** 代码结构、Git 历史、临时状态等能从现有信息推导的内容
5. **如何验证记忆是否过时？** 使用前检查文件是否存在、函数是否还在、URL 是否有效

---

## 十、极简总结

```
记忆 = 跨会话持久化知识
四种类型 = user(你) + feedback(怎么配合) + project(在做什么) + reference(去哪找)
MEMORY.md = 索引，memory/*.md = 正文
写入 = 用户纠正/确认/告知时，立即写入相关类型
验证 = 使用记忆前检查是否过时（文件存在、函数还在）
不写入 = 代码结构、Git 历史、临时状态
```
