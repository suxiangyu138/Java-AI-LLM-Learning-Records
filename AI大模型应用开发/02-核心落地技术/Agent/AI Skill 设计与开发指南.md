# AI Skill 设计与开发指南（实战版）

> **文档定位**：AI Agent 核心技术 | Skill 系统设计与开发
> **核心问题**：Skill 是什么？和 Tool、Agent 什么关系？怎么设计一个 Skill？

---

## 一、Skill 的概念定位

```
AI 开发中的三个层级：

Tool（工具）      —— 单一功能："查询天气"、"执行 SQL"
Skill（技能）     —— 工作流 + 知识 + 工具组合："代码审查"、"安全审计"
Agent（智能体）   —— 自主决策 + 多 Skill + 多 Tool："全栈开发助手"
```

| 维度 | Tool | Skill | Agent |
|---|---|---|---|
| **粒度** | 原子操作 | 工作流 | 自主系统 |
| **决策** | 无决策 | 有限决策 | 自主决策 |
| **组合** | 不可再拆 | Tool + Prompt + 知识 | 多 Skill + 多 Tool |
| **示例** | `web_search()` | 代码审查技能 | 全栈开发 Agent |

---

## 二、Skill 核心组成

```markdown
一个完整的 Skill 包含：

1. SKILL.md           ← Skill 定义（名称、描述、触发条件、工作流）
2. Prompt Template    ← 专用 System Prompt
3. Tools              ← 该 Skill 专用的工具
4. Knowledge          ← 领域知识（规则、最佳实践）
5. Output Schema      ← 标准化输出格式
```

### 2.1 SKILL.md 模板

```markdown
# Skill: Java 代码审查专家

## 元信息
- name: java-code-reviewer
- version: 1.0.0
- author: team-backend
- description: Java 后端代码审查，按阿里巴巴规范 + OWASP 安全标准

## 触发条件
- 用户说 "review" "审查" "检查这段代码"
- 文件是 .java 后缀
- 当前是代码审查场景

## 工作流
1. 读取目标 Java 文件
2. 检查编码规范（命名、格式、注释）
3. 检查安全漏洞（SQL 注入、XSS、敏感信息泄露）
4. 检查性能问题（N+1 查询、内存泄漏、不合理的循环）
5. 检查异常处理（是否吞异常、是否有兜底）
6. 输出结构化审查报告

## 工具
- builtin: Read, Grep, Glob
- custom: sonarqube_scanner, dependency_check

## 输出格式
{
  "file": "文件路径",
  "summary": "1 个严重问题，3 个警告，5 个建议",
  "issues": [
    {
      "severity": "critical|warning|suggestion",
      "line": 42,
      "title": "SQL 注入风险",
      "description": "使用 Statement 而非 PreparedStatement",
      "fix": "改用 PreparedStatement + 参数化查询"
    }
  ]
}
```

---

## 三、Skill 的两阶段加载机制

```
阶段 1：扫描（Scan）
  Agent 浏览所有 Skill 的 description → 判断哪些 Skill 相关
  只加载 Skill 列表（~1KB），不加载具体内容

阶段 2：加载（Load）
  Agent 选定最匹配的 Skill → 读取 SKILL.md 完整内容
  加载专用 Prompt + 工具 + 知识
  执行 Skill 定义的工作流
```

### 3.1 为什么两阶段

```
❌ 一次性加载所有 Skill：
  - 100+ Skills × 5KB each = 500KB+ 浪费 Token
  - 无关 Skill 干扰 Agent 决策

✅ 两阶段加载：
  - 先扫描 description（1KB）
  - 再按需加载完整 Skill（5KB）
  - Token 节省 100 倍
```

---

## 四、Skill 设计原则

| 原则 | 说明 | 坏例 | 好例 |
|---|---|---|---|
| **单一专注** | 一个 Skill 只做一类事 | "万能开发助手" | "Java 代码审查" |
| **好 description** | 扫描阶段靠它匹配 | "做开发" | "审查 Java 代码规范和安全漏洞" |
| **可组合** | Skill A 的输出可以是 Skill B 的输入 | 紧耦合成一个大 Skill | 代码审查 → 问题修复，两个独立 Skill |
| **有边界** | 明确什么情况不适用 | 不说明限制 | "仅适用于 Spring Boot 项目" |
| **结构化输出** | 固定输出 Schema | 自由文本 | JSON 带字段定义 |

---

## 五、Skill 开发实战

### 5.1 创建一个简单的 Skill

```markdown
# SKILL.md — 数据库慢查询分析

## name: sql-slow-query-analyzer
## description: 分析 MySQL 慢查询日志，找出需要优化的 SQL

## 工作流
1. 读取慢查询日志文件
2. 解析日志提取 SQL 和执行时间
3. 按执行时间倒序排列
4. 对 Top 5 慢 SQL 分析原因（缺索引/全表扫描/大表 JOIN）
5. 给出优化建议（建索引/改写 SQL/分表）

## 需要的工具
- Read（读取日志文件）
- Bash（执行 EXPLAIN）

## 输出格式
{
  "total_queries": 1523,
  "slow_threshold_ms": 100,
  "top_slow": [
    {
      "sql": "SELECT * FROM orders WHERE ...",
      "avg_time_ms": 3500,
      "count": 230,
      "problem": "缺少 user_id 索引导致全表扫描",
      "fix": "ALTER TABLE orders ADD INDEX idx_user_id (user_id)"
    }
  ]
}
```

### 5.2 触发 Skill

```
用户："分析一下慢查询日志"
    ↓
Agent 扫描所有 Skill description
    ↓ 匹配 "sql-slow-query-analyzer" 的 description
Agent 加载 SKILL.md 完整内容
    ↓
Agent 按 Skill 定义的工作流执行
    ↓
输出结构化分析报告
```

---

## 六、Skill 与 Agent 的边界

```
什么时候做成 Skill：
  ✅ 可重复执行的工作流
  ✅ 有明确的输入和输出
  ✅ 依赖领域知识而非通用能力
  ✅ 值得为它写一份专用 Prompt

什么时候留给 Agent 自己处理：
  ❌ 一次性任务
  ❌ 需要完全自主决策
  ❌ 通用能力（写代码、回答问题）
```

---

## 七、面试核心要点

1. **Skill vs Tool vs Agent？** Tool 是原子操作，Skill 是工作流+知识+工具组合，Agent 是自主系统
2. **为什么两阶段加载？** 先扫描 description（省 Token），再加载匹配的 SKILL.md
3. **SKILL.md 包含什么？** 名称、描述、触发条件、工作流、工具、输出格式
4. **Skill 的核心设计原则？** 单一专注、好 description、结构化输出、有边界

---

## 八、极简总结

```
Tool = 一个函数（查询天气）
Skill = 工作流 + 领域知识 + 工具组合（代码审查）
Agent = 自主决策 + 多 Skill + 多 Tool（全栈开发助手）

SKILL.md = 名字 + description + 工作流 + 工具 + 输出格式
两阶段加载 = 先扫 description → 再加载完整内容
设计原则 = 单一、好描述、可组合、有边界
```
