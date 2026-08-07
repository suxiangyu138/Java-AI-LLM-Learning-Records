# 02 - Agent Skills 类型与设计模式

> 🎯 Skill 的类型决定了它的职责边界和调用方式。掌握分类法和设计模式，是设计高质量 Skill 体系的关键

---

## 目录

1. [按能力类型分类](#1-按能力类型分类)
2. [按调用范式分类](#2-按调用范式分类)
3. [按领域归属分类](#3-按领域归属分类)
4. [Skill 设计模式](#4-skill-设计模式)
5. [选择策略与决策树](#5-选择策略与决策树)

---

## 1. 按能力类型分类

### 1.1 全面分类矩阵

| 大类 | 子类 | 典型 Skill | 输入 → 输出 | Agent 使用场景 |
|------|------|-----------|-------------|---------------|
| **📡 感知型** | 搜索 | `WebSearch` | query → results[] | 信息获取、事实核查 |
| | 读取 | `FileRead` | path → content | 代码分析、文档阅读 |
| | 监听 | `EventListen` | channel → events[] | 实时监控、告警 |
| **🔧 操作型** | 创建 | `FileCreate` | content+path → file | 文件生成、项目初始化 |
| | 修改 | `CodeEdit` | file+diff → result | 代码重构、Bug 修复 |
| | 删除 | `FileDelete` | path → status | 清理、回滚 |
| | 执行 | `BashExec` | command → output | 构建、部署、测试 |
| **🧠 分析型** | 审查 | `CodeReview` | diff → findings[] | PR Review、质量检查 |
| | 测试 | `TestRunner` | target → report | CI/CD、回归测试 |
| | 诊断 | `LogAnalyzer` | logs → diagnosis | 故障排查、根因分析 |
| **🔄 转换型** | 格式 | `JsonToYaml` | json → yaml | 配置转换 |
| | 翻译 | `I18nTranslate` | text+locale → text | 国际化 |
| | 摘要 | `TextSummarize` | long_text → summary | 文档处理 |
| **🤝 协调型** | 编排 | `WorkflowOrch` | plan → execution | 多步骤任务 |
| | 通信 | `NotifySkill` | message+channel → status | 消息推送、告警 |
| | 调度 | `TaskScheduler` | tasks[] → schedule | 定时任务、批处理 |

### 1.2 感知型 Skill 详解

```java
// 感知型 Skill：获取信息但不改变状态
public class WebSearchSkill implements Skill {
    @Override
    public SkillSchema getSchema() {
        return SkillSchema.builder()
            .name("web_search")
            .description("搜索互联网获取最新信息。适用于：查找最新文档、技术方案对比、事实核查")
            .category("perception")
            .input(Schema.obj()
                .add("query", Schema.str().desc("搜索关键词"))
                .add("max_results", Schema.int_().desc("最大结果数，默认5"))
                .required("query"))
            .output(Schema.arr(Schema.obj()
                .add("title", Schema.str())
                .add("url", Schema.str())
                .add("snippet", Schema.str())))
            .sideEffects(false)  // 关键：无副作用
            .build();
    }
}
```

### 1.3 操作型 Skill 详解

```java
// 操作型 Skill：改变系统状态，需要权限控制和回滚能力
public class CodeEditSkill implements Skill {
    @Override
    public SkillSchema getSchema() {
        return SkillSchema.builder()
            .name("code_edit")
            .description("精确修改代码文件。适用于：Bug修复、重构、功能添加")
            .category("operation")
            .input(Schema.obj()
                .add("file_path", Schema.str().desc("目标文件绝对路径"))
                .add("old_string", Schema.str().desc("要替换的原文本"))
                .add("new_string", Schema.str().desc("替换后的新文本"))
                .required("file_path", "old_string", "new_string"))
            .sideEffects(true)     // 关键：会修改文件
            .requiresConfirm(true)  // 关键：需要确认
            .rollback("git checkout -- {file_path}")
            .build();
    }
}
```

---

## 2. 按调用范式分类

### 2.1 同步 vs 异步 vs 流式

```text
┌────────────────────────────────────────────────────────────┐
│                    调用范式对比                              │
├──────────┬─────────────────┬─────────────────┬─────────────┤
│   范式   │  适用场景        │  返回方式         │  超时策略    │
├──────────┼─────────────────┼─────────────────┼─────────────┤
│ 同步     │ 简单查询、计算   │ 直接返回结果     │ 短超时(5s)  │
│ 异步     │ 部署、训练、批处理│ 返回 TaskID      │ 长超时(5min)│
│ 流式     │ 代码生成、长文本  │ SSE/WebSocket    │ 心跳检测    │
│ 事件驱动 │ 监控、通知       │ 回调/Webhook     │ 事件超时    │
└──────────┴─────────────────┴─────────────────┴─────────────┘
```

```java
// 异步 Skill 的标准模式
public class DeploySkill implements AsyncSkill {

    @Override
    public CompletableFuture<DeployResult> execute(DeployInput input) {
        return CompletableFuture
            .supplyAsync(() -> startDeploy(input))
            .thenApply(this::waitForCompletion)
            .orTimeout(5, TimeUnit.MINUTES)
            .exceptionally(this::handleFailure);
    }

    // 状态查询接口
    @Override
    public DeployStatus getStatus(String taskId) {
        return statusStore.get(taskId);
    }
}
```

### 2.2 幂等型 vs 非幂等型

```text
幂等 Skill：重复调用 N 次 = 调用 1 次（安全重试）
  ✅ FileCreate（文件已存在 → 跳过）
  ✅ ConfigSet（设置相同值 → 无变化）
  ✅ RecordUpsert（存在则更新）

非幂等 Skill：每次调用都有新副作用（不能盲目重试）
  ⚠️ EmailSend（重复发送 → 用户收到多封）
  ⚠️ OrderCreate（重复创建 → 重复订单）
  ⚠️ ChargeMoney（重复扣款 → 资金损失）

解决方案：
  ① 引入幂等键（idempotency_key）
  ② Skill 内部去重检查
  ③ 框架层重试策略区分
```

### 2.3 可中断型 vs 不可中断型

```text
可中断 Skill：支持暂停/恢复/取消
  长任务（部署、数据迁移、模型训练）
  → 需要 Checkpoint 机制

不可中断 Skill：原子操作，要么全成功要么全失败
  简单操作（文件写入、API 调用）
  → 需要事务或补偿机制
```

---

## 3. 按领域归属分类

### 3.1 领域分类全景

```text
Agent Skills 领域分布
│
├── 💻 软件工程领域
│   ├── code_review      代码审查
│   ├── code_generate    代码生成
│   ├── test_write       测试编写
│   ├── refactor         重构
│   ├── bug_fix          缺陷修复
│   └── doc_generate     文档生成
│
├── 🔒 安全领域
│   ├── vulnerability_scan  漏洞扫描
│   ├── secret_detect       密钥检测
│   ├── dependency_check    依赖检查
│   └── compliance_audit    合规审计
│
├── 📊 数据领域
│   ├── data_analyze     数据分析
│   ├── data_clean       数据清洗
│   ├── data_visualize   数据可视化
│   └── etl_execute      ETL执行
│
├── 🌐 DevOps领域
│   ├── deploy           部署
│   ├── monitor          监控
│   ├── alert_handle     告警处理
│   └── incident_response 故障响应
│
└── 📝 知识领域
    ├── research          调研
    ├── summarize         摘要
    ├── translate         翻译
    └── qa_answer         问答
```

---

## 4. Skill 设计模式

### 4.1 策略模式 (Strategy Pattern)

```text
问题：同一任务有多种实现方式，Agent 需要根据上下文选择

                     ┌──────────────┐
                     │   Agent      │
                     └──────┬───────┘
                            │ 选择策略
              ┌─────────────┼─────────────┐
              ▼             ▼             ▼
       ┌──────────┐  ┌──────────┐  ┌──────────┐
       │FastSearch│  │DeepSearch│  │CodeSearch│
       │ (快速查) │  │ (深度查)  │  │ (代码查)  │
       └──────────┘  └──────────┘  └──────────┘
```

```java
// 策略模式 Skill 定义
public interface SearchStrategy extends Skill {
    SearchResult search(SearchContext ctx);
}

// Skill 注册时声明策略标签
@SkillInfo(
    name = "fast_search",
    strategy = "speed",    // 策略标签
    priority = 100         // 优先级：Agent 优先选高优先级
)
public class FastSearchSkill implements SearchStrategy { ... }

@SkillInfo(
    name = "deep_search",
    strategy = "quality",
    priority = 50
)
public class DeepSearchSkill implements SearchStrategy { ... }

// Agent 根据用户意图选择
// "quickly find..." → fast_search (priority 100)
// "thoroughly research..." → deep_search (priority 50)
```

### 4.2 适配器模式 (Adapter Pattern)

```text
问题：外部服务接口不统一，需要适配为统一 Skill 接口

    外部 API                    Skill 接口
  ┌──────────┐              ┌──────────────┐
  │GitHub API │──┐           │   GitSkill   │
  │REST v3    │  │  ┌────────┤              │
  └──────────┘  ├──┤Adapter │ • clone()     │
                │  │        │ • commit()    │
  ┌──────────┐  │  └────────┤ • push()      │
  │GitLab API │──┘           │ • createPR()  │
  │GraphQL    │              └──────────────┘
  └──────────┘
```

### 4.3 装饰器模式 (Decorator Pattern)

```text
问题：在不修改原 Skill 的情况下增加横切关注点

  RawSkill
    │
    ├── LoggingDecorator    → 记录每次调用的输入/输出/耗时
    ├── RetryDecorator      → 失败自动重试 N 次
    ├── CircuitBreaker      → 连续失败后熔断
    ├── CacheDecorator      → 缓存相同输入的结果
    └── AuthDecorator       → 注入认证信息
```

```java
// 装饰器链构建
Skill enhancedSkill = SkillBuilder
    .from(new CodeReviewSkill())
    .decorateWith(new LoggingDecorator())
    .decorateWith(new RetryDecorator(3))
    .decorateWith(new CircuitBreakerDecorator(5, Duration.ofMinutes(1)))
    .build();

// 执行时自动按装饰顺序包装
ReviewResult result = enhancedSkill.execute(input);
```

### 4.4 责任链模式 (Chain of Responsibility)

```text
问题：一个请求可能需要多个 Skill 依次处理，每个 Skill 决定是否处理

  Request → [AuthSkill] → [ValidateSkill] → [ProcessSkill] → [NotifySkill] → Response
                │               │                │                │
                ├─ 通过         ├─ 通过          ├─ 通过          ├─ 通过
                └─ 拒绝→返回401 └─ 不合法→返回400 └─ 失败→重试    └─ 失败→告警
```

### 4.5 观察者模式 (Observer Pattern)

```text
问题：Skill 执行状态变化时，需要通知多个关注方

  Skill.onStatusChange()
    │
    ├── Logger.observe()      → 记录日志
    ├── Metrics.observe()     → 更新指标
    ├── Notifier.observe()    → 推送通知
    └── Audit.observe()       → 审计记录
```

---

## 5. 选择策略与决策树

### 5.1 Skill 类型选择决策树

```text
开始
│
├─ 是否需要修改外部状态？
│   ├─ 是 → 操作型 Skill（需要权限控制 + 回滚）
│   └─ 否 → 感知型 Skill
│       ├─ 需要复杂分析？→ 分析型 Skill
│       ├─ 需要格式转换？→ 转换型 Skill
│       └─ 只是查询     → 感知型 Skill（基础）
│
├─ 执行时间 > 5秒？
│   ├─ 是 → 异步 Skill
│   └─ 否 → 同步 Skill
│
├─ 需要逐步输出？
│   ├─ 是 → 流式 Skill
│   └─ 否 → 批量 Skill
│
└─ 可安全重试？
    ├─ 是 → 标记为幂等，框架自动重试
    └─ 否 → 标记为非幂等，需要幂等键
```

### 5.2 何时封装为 Skill

```text
✅ 应该封装为 Skill：
  • 3 个以上 Tool 的组合且经常一起使用
  • 包含领域知识判断逻辑
  • 需要在多处复用
  • 有明确的输入输出契约
  • 需要独立的测试和版本管理

❌ 不需要封装为 Skill：
  • 只有一个 Tool 调用（直接用 Tool）
  • 一次性使用（不值得封装）
  • 逻辑极度简单（封装成本 > 收益）
  • 强依赖特定上下文无法复用
```

> 🎯 **核心要点**：按能力+范式+领域三维分类，善用策略/适配器/装饰器/责任链四种模式，让 Skill 体系可扩展、可维护、可复用

---

**上一模块**：[01 - Agent Skills 核心概念](./01-Agent%20Skills核心概念.md)  
**下一模块**：[03 - Agent Skills 描述与注册机制](./03-Agent%20Skills描述与注册机制.md)  
**返回总览**：[00 - Agent Skills 知识体系总览](./00-Agent%20Skills知识体系总览.md)
