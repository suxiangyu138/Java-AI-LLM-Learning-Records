# 04 - Agent Skills 组合与编排

> 🎯 单个 Skill 的能力有限，真正的威力来自 Skill 的组合编排 — 像搭乐高一样，用原子 Skill 构建复杂工作流

---

## 目录

1. [组合基础：链式调用](#1-组合基础链式调用)
2. [并行编排](#2-并行编排)
3. [条件分支与路由](#3-条件分支与路由)
4. [DAG 编排与 SkillGraph](#4-dag-编排与-skillgraph)
5. [子技能与嵌套编排](#5-子技能与嵌套编排)
6. [编排框架对比](#6-编排框架对比)

---

## 1. 组合基础：链式调用

### 1.1 顺序链 (Sequential Chain)

```text
最基础的组合模式：A 的输出 → B 的输入

  [FileRead] → [CodeAnalyze] → [ReportGenerate] → [NotifySend]
     │              │                │                │
   读取代码      分析质量         生成报告          发送通知
```

```java
// 链式组合：声明式 DSL
SkillPipeline pipeline = SkillPipeline.create()
    .then("file_read", input -> Map.of("path", input.getFilePath()))
    .then("code_analyze", prev -> Map.of("code", prev.get("content")))
    .then("report_generate", prev -> Map.of("findings", prev.get("issues")))
    .then("notify_send", prev -> Map.of("message", prev.get("report")));

PipelineResult result = pipeline.execute(Map.of("filePath", "/src/Main.java"));
```

### 1.2 数据传递模式

```text
链式调用中的数据传递策略：

① Pass-through（全量传递）
  每个 Skill 的输出全部传给下一个 Skill
  优点：简单  缺点：数据膨胀、Token 浪费

② Selective Mapping（字段映射）
  只传递需要的字段：prev.output.summary → next.input.text
  优点：精准  缺点：需要显式声明

③ Context Bag（上下文袋）
  公共数据放 Context，Skill 按需取用
  优点：灵活  缺点：隐式依赖
```

```yaml
# 声明式字段映射
pipeline:
  name: "code_review_pipeline"
  steps:
    - skill: "git_diff"
      id: "diff_step"
    - skill: "code_review"
      mapping:
        changed_files: "$diff_step.output.files"
        context: "$context.project_config"
    - skill: "report_format"
      mapping:
        findings: "$code_review.output.issues"
        template: "review_report"
```

---

## 2. 并行编排

### 2.1 扇出-扇入模式 (Fan-out/Fan-in)

```text
并行执行多个 Skill，汇总结果

                    ┌── [UnitTest] ──┐
  [GitDiff] → FanOut├── [LintCheck] ─├→ FanIn → [ReportMerge] → [Notify]
                    ├── [SecScan]  ──┤
                    └── [PerfCheck] ─┘

  扇出：将一个输入分发到 N 个并行 Skill
  扇入：等待所有 Skill 完成，汇总结果
```

```java
// 并行编排
ParallelResult result = SkillOrchestrator.parallel()
    .fanOut(diffResult -> List.of(
        SkillTask.of("unit_test", diffResult),
        SkillTask.of("lint_check", diffResult),
        SkillTask.of("security_scan", diffResult),
        SkillTask.of("perf_check", diffResult)
    ))
    .fanIn(results -> {
        // 汇总所有结果
        return MergeResult.builder()
            .testResults(results.get("unit_test"))
            .lintIssues(results.get("lint_check"))
            .vulnerabilities(results.get("security_scan"))
            .perfIssues(results.get("perf_check"))
            .build();
    })
    .execute();
```

### 2.2 并行策略对比

| 策略 | 描述 | 适用场景 | 资源消耗 |
|------|------|----------|:---:|
| **全并行** | 所有 Skill 同时启动 | 无依赖的独立分析 | 高 |
| **限制并发** | 信号量控制并行数 | 资源受限环境 | 可控 |
| **分批并行** | N 个一批执行 | 大量 Skill + 有限资源 | 中 |
| **竞速模式** | 多个 Skill 竞速，取最先完成的 | 有多个实现方案时 | 高 |
| **全完成** | 等待所有完成（默认） | 需要完整结果 | — |

```java
// 竞速模式：多个搜索策略同时执行，取最快结果
SearchResult result = SkillOrchestrator.race()
    .add(SkillTask.of("fast_search", query))
    .add(SkillTask.of("deep_search", query))
    .add(SkillTask.of("code_search", query))
    .timeout(Duration.ofSeconds(3))  // 3秒内必须有一个返回
    .execute();
```

---

## 3. 条件分支与路由

### 3.1 基于结果的条件分支

```text
根据上一个 Skill 的输出决定下一步

                    ┌─ result=="success" → [DeploySkill]
  [BuildSkill] ──┤
                    └─ result=="failure" → [NotifySkill] → [RollbackSkill]
```

```java
SkillPipeline pipeline = SkillPipeline.create()
    .then("build_project", input)
    .branch("build_result",
        // 分支 1：构建成功
        result -> result.get("status").equals("success"),
        branch -> branch
            .then("run_tests")
            .then("deploy")
    )
    .branch("build_result",
        // 分支 2：构建失败
        result -> result.get("status").equals("failure"),
        branch -> branch
            .then("notify_failure")
            .then("collect_error_logs")
    );
```

### 3.2 LLM 路由 (LLM-as-Router)

```text
让 LLM 根据上下文动态选择下一个 Skill

  User: "帮我优化这个函数的性能"
      │
      ▼
  ┌─────────────────────────┐
  │  LLM Router             │
  │  分析意图 + 上下文       │
  │  决策下一步 Skill        │
  └───────┬─────────────────┘
          │
    ┌─────┼─────────┐
    ▼     ▼         ▼
 [Profiler] [Refactor] [Benchmark]
```

```java
// LLM 路由实现
public class LLMRouter {
    public SkillTask route(Context ctx, List<SkillDefinition> available) {
        String prompt = """
            当前上下文：%s
            可用 Skill：%s
            
            请选择下一步最合适的 Skill，只需回答 Skill 名称。
            """.formatted(ctx.summary(), available.stream()
                .map(s -> "- %s: %s".formatted(s.getName(), s.getDescription()))
                .collect(Collectors.joining("\n")));

        String selected = llm.chat(prompt);
        return SkillTask.of(selected, ctx);
    }
}
```

---

## 4. DAG 编排与 SkillGraph

### 4.1 什么是 SkillGraph

```text
SkillGraph = 有向无环图（DAG）组织的 Skill 网络

               ┌──────────────┐
               │  git_diff     │
               └──────┬───────┘
                      │
          ┌───────────┼───────────┐
          │           │           │
          ▼           ▼           ▼
   ┌──────────┐ ┌──────────┐ ┌──────────┐
   │lint_check│ │unit_test │ │sec_scan  │
   └────┬─────┘ └────┬─────┘ └────┬─────┘
        │            │            │
        └────────────┼────────────┘
                     │
                     ▼
             ┌──────────────┐
             │ report_merge  │
             └──────┬───────┘
                     │
              ┌──────┴──────┐
              ▼             ▼
       ┌──────────┐  ┌──────────┐
       │notify    │  │archive   │
       └──────────┘  └──────────┘
```

### 4.2 SkillGraph DSL

```yaml
# SkillGraph 声明式定义
graph:
  name: "comprehensive_code_review"
  nodes:
    - id: "diff"
      skill: "git_diff"
    - id: "lint"
      skill: "lint_check"
      depends_on: ["diff"]
    - id: "tests"
      skill: "unit_test"
      depends_on: ["diff"]
    - id: "security"
      skill: "security_scan"
      depends_on: ["diff"]
    - id: "merge"
      skill: "report_merge"
      depends_on: ["lint", "tests", "security"]
    - id: "notify"
      skill: "send_notification"
      depends_on: ["merge"]
      condition: "merge.output.total_issues > 0"

  edges:
    - from: "diff"
      to: "lint"
      mapping:
        files: "diff.output.changed_files"
    - from: "diff"
      to: "tests"
      mapping:
        target: "diff.output.test_targets"
    - from: "diff"
      to: "security"
      mapping:
        files: "diff.output.changed_files"

  error_handling:
    on_node_failure: "continue"  # 或 "abort"
    retry:
      max_attempts: 2
      backoff: "exponential"
```

### 4.3 拓扑排序执行

```java
/**
 * 基于拓扑排序的 DAG 执行引擎
 */
public class SkillGraphExecutor {

    public GraphResult execute(SkillGraph graph, Map<String, Object> initialInput) {
        // 1. 拓扑排序：确定执行顺序
        List<String> executionOrder = topologicalSort(graph);

        // 2. 按层级并行执行
        Map<String, SkillResult> results = new ConcurrentHashMap<>();

        for (List<String> level : groupByLevel(graph, executionOrder)) {
            // 同层节点并行执行
            CompletableFuture.allOf(
                level.stream()
                    .map(nodeId -> CompletableFuture.runAsync(() -> {
                        SkillResult result = executeNode(graph, nodeId, results);
                        results.put(nodeId, result);
                    }))
                    .toArray(CompletableFuture[]::new)
            ).join();
        }

        // 3. 返回最终结果
        return buildGraphResult(results);
    }

    /**
     * 执行单个节点：从上游节点收集输入
     */
    private SkillResult executeNode(SkillGraph graph, String nodeId,
                                     Map<String, SkillResult> upstream) {
        GraphNode node = graph.getNode(nodeId);
        // 从依赖节点收集输入
        Map<String, Object> input = node.getDependencies().stream()
            .flatMap(depId -> graph.getEdge(depId, nodeId)
                .applyMapping(upstream.get(depId)).entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return node.getSkill().execute(input);
    }
}
```

---

## 5. 子技能与嵌套编排

### 5.1 复合 Skill 模式

```text
将常用的 Skill 组合封装为新的"复合 Skill"，对外像一个原子 Skill

  CompositeSkill: "full_code_review"
  ├── git_diff          (子 Skill)
  ├── parallel:
  │   ├── lint_check    (子 Skill)
  │   ├── security_scan (子 Skill)
  │   └── complexity    (子 Skill)
  └── report_merge      (子 Skill)
```

```java
/**
 * 复合 Skill：对外是普通 Skill，内部是 SkillGraph
 */
@SkillInfo(
    name = "full_code_review",
    description = "完整的代码审查流程：获取变更→并行检查→汇总报告",
    input_schema = @Schema(properties = {
        @Property(name = "target_path", type = "string")
    })
)
public class FullCodeReviewSkill implements CompositeSkill {

    @Override
    public SkillGraph getInternalGraph() {
        return SkillGraph.build()
            .node("diff", "git_diff")
            .node("lint", "lint_check").after("diff")
            .node("security", "security_scan").after("diff")
            .node("complexity", "complexity_check").after("diff")
            .node("merge", "report_merge").after("lint", "security", "complexity")
            .build();
    }

    // 外部接口：Agent 不知道内部是组合的
    @Override
    public ReviewReport execute(Map<String, Object> input) {
        return (ReviewReport) getInternalGraph()
            .execute(input)
            .getFinalOutput();
    }
}
```

### 5.2 组合层级

```text
Level 0：原子 Skill（不可再分）
  → file_read, web_search, bash_exec

Level 1：简单链（2-3 个原子 Skill）
  → read + analyze = "代码分析"

Level 2：复合 Skill（DAG 编排）
  → diff + parallel(lint, test, security) + merge = "代码审查"

Level 3：自治 Workflow（含条件 + 循环 + 人工审批）
  → review + fix + verify + deploy = "自动修复并部署"

Level 4：Multi-Agent 协作（多个 Agent 各司其职）
  → code_agent + review_agent + test_agent + ops_agent
```

---

## 6. 编排框架对比

| 框架 | 编排方式 | 声明式 | DAG | 条件分支 | LLM路由 | 人工审批 |
|------|---------|:---:|:---:|:---:|:---:|:---:|
| **LangChain LCEL** | `\|` 管道 + RunnableLambda | ✅ | ✅ | ✅ | ❌ | ❌ |
| **LangGraph** | StateGraph + Node + Edge | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Spring AI** | TaskFlow + @Bean 注册 | ✅ | ❌ | ❌ | ❌ | ❌ |
| **Temporal** | Workflow as Code | ❌ | ✅ | ✅ | ❌ | ✅ |
| **Dify** | 可视化拖拽 | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Coze** | 可视化 + 插件市场 | ✅ | ✅ | ✅ | ✅ | ❌ |

### 6.1 框架选型建议

```text
简单链式（3-5步）       → LangChain LCEL / Spring AI
复杂 DAG（10+节点）      → LangGraph / Temporal
可视化编排（非开发人员）  → Dify / Coze
多 Agent 协作            → LangGraph / AutoGen / CrewAI
生产级工作流（SLA要求）  → Temporal / AWS Step Functions
```

> 🎯 **核心要点**：链式是基础，DAG 是核心，条件分支是灵魂。好的编排 = 原子 Skill 的松耦合 + 声明式的执行图 + 可靠的错误处理

---

**上一模块**：[03 - Agent Skills 描述与注册机制](./03-Agent%20Skills描述与注册机制.md)  
**下一模块**：[05 - Agent Skills 框架实现对比](./05-Agent%20Skills框架实现对比.md)  
**返回总览**：[00 - Agent Skills 知识体系总览](./00-Agent%20Skills知识体系总览.md)
