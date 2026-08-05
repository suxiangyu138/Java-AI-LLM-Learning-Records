# 06 - Agent Skills 实战与最佳实践

> 🎯 从设计到上线：掌握 Skill 的命名、验参、容错、监控、版本管理等生产级最佳实践

---

## 目录

1. [Skill 设计原则](#1-skill-设计原则)
2. [命名与描述规范](#2-命名与描述规范)
3. [参数校验与类型安全](#3-参数校验与类型安全)
4. [容错与弹性设计](#4-容错与弹性设计)
5. [可观测性设计](#5-可观测性设计)
6. [版本管理与兼容性](#6-版本管理与兼容性)
7. [常见反模式](#7-常见反模式)

---

## 1. Skill 设计原则

### 1.1 六大设计原则

```text
┌─────────────────────────────────────────────────────────┐
│  Skill 设计六大原则                                       │
├─────────────────────────────────────────────────────────┤
│  ① 单一职责：一个 Skill 只做一件事，做好它                 │
│  ② 显式契约：输入输出 Schema 就是合约，不可含糊            │
│  ③ 自描述性：描述完整到 LLM 不需猜测何时用、怎么用         │
│  ④ 容错内置：错误在 Skill 内部处理，不让 LLM 猜恢复策略    │
│  ⑤ 可观测性：每个 Skill 调用必须可追踪、可度量            │
│  ⑥ 向后兼容：升级 Skill 不破坏已有调用方                   │
└─────────────────────────────────────────────────────────┘
```

### 1.2 粒度决策指南

```text
Skill 粒度判断公式：

  原子 Skill（对 LLM 暴露）：
    token_cost < 500 tokens/次  ✅
    调用频率 > 10 次/会话       ✅
    可被其他 Skill 复用         ✅
    LLM 需要在中间干预决策      ✅

  复合 Skill（内部组合，对外暴露一个）：
    3-8 个原子步骤             ✅
    固定流程，不需要 LLM 决策  ✅
    整体调用频率高             ✅
    
  过粗的 Skill（应该拆分的信号）：
    description 超过 300 字    ⚠️
    参数超过 8 个               ⚠️
    输出结构深度 > 3 层         ⚠️
    名称包含 "and" 或 "or"     ⚠️
```

---

## 2. 命名与描述规范

### 2.1 命名规范

```text
Skill 命名公式：{动词}_{名词}_{可选限定词}

✅ 好的命名：
  search_code         动词+名词，清晰
  analyze_security    动词+名词，明确
  deploy_k8s          动词+名词+限定词
  generate_test_java  动词+名词+限定词

❌ 不好的命名：
  doStuff             太模糊
  handle              不完整
  processData         太泛化
  searchAndAnalyze    违反单一职责的信号
```

### 2.2 描述模板

```yaml
# 推荐的 Skill 描述模板
skill_description:
  summary: >
    {一句话总结：做什么 + 对谁有用}
  
  use_cases:
    - "场景1：当用户说 X 时使用"
    - "场景2：当需要 Y 结果时使用"
    - "场景3：作为 Z 流程的一环使用"
  
  anti_use_cases:
    - "不用场景1：当用户意图是 A 时，应使用 other_skill_A"
    - "不用场景2：当数据量超过 B 时，应分批处理"
  
  input_highlights:
    - "关键参数1：为什么重要，常见值是什么"
    - "关键参数2：与参数1的关系"
  
  output_highlights:
    - "返回什么，什么格式"
    - "空结果/错误时的返回值"
  
  side_effects:
    - "会修改文件系统"
    - "会发送网络请求"
    - "无副作用"  # 如果确实没有
  
  caveats:
    - "注意1：超时时间 30s"
    - "注意2：每分钟限 10 次调用"
```

### 2.3 描述验证清单

```text
✅ Skill 描述质量检查清单：

□ 读到 description 前两句话就能判断何时使用
□ when_to_use 覆盖了最常见的 3 种触发场景
□ when_not_to_use 排除了最容易混淆的 2 种场景
□ 每个参数 description 回答了"这个值填什么"
□ 枚举参数说明了每个选项的含义和适用场景
□ 明确了是否修改外部状态（sideEffects）
□ 明确了资源限制（timeout, rate_limit）
□ 用 LLM 盲测：只给 description，能正确选对 Skill  > 95%
```

---

## 3. 参数校验与类型安全

### 3.1 多层校验策略

```java
/**
 * 参数校验三道防线
 */
public class ParameterValidator {

    /**
     * 防线 1：Schema 层（框架自动）
     * JSON Schema 校验类型、必填、格式
     */
    public void validateBySchema(SkillDefinition skill, Map<String, Object> params) {
        SchemaValidator validator = SchemaValidator.from(skill.getInputSchema());
        ValidationResult result = validator.validate(params);
        if (!result.isValid()) {
            throw new SkillParamException(result.getErrors());
        }
    }

    /**
     * 防线 2：业务规则层（Skill 内部）
     * 超越 Schema 能表达的约束
     */
    public void validateBusinessRules(Map<String, Object> params) {
        // 示例：文件大小限制
        if (params.containsKey("file_path")) {
            Path path = Path.of((String) params.get("file_path"));
            if (Files.size(path) > 10 * 1024 * 1024) {
                throw new SkillParamException("文件超过 10MB，请使用 file_process_large skill");
            }
        }

        // 示例：日期范围
        if (params.containsKey("start_date") && params.containsKey("end_date")) {
            LocalDate start = (LocalDate) params.get("start_date");
            LocalDate end = (LocalDate) params.get("end_date");
            if (start.isAfter(end)) {
                throw new SkillParamException("start_date 必须在 end_date 之前");
            }
            if (ChronoUnit.DAYS.between(start, end) > 365) {
                throw new SkillParamException("日期范围不能超过 365 天");
            }
        }
    }

    /**
     * 防线 3：运行时校验（执行中）
     * 输入合法但运行时条件不满足
     */
    public void validateRuntime(Map<String, Object> params) {
        // 示例：权限检查
        if (!currentUser.hasPermission("file:write")) {
            throw new SkillPermissionException("当前用户无文件写入权限");
        }
    }
}
```

### 3.2 错误信息设计

```text
好的错误信息 = LLM 能据此调整策略

❌ 坏错误：
  "Error: invalid parameter"
  → LLM 不知道哪个参数错了、怎么改

✅ 好错误：
  {
    "error": "INVALID_PARAMETER",
    "parameter": "max_results",
    "value": 1000,
    "constraint": "必须在 1-100 之间",
    "suggestion": "将 max_results 改为 100 或更小的值，如需更多结果请使用分页参数 page"
  }
  → LLM 读完直接修正参数重试
```

---

## 4. 容错与弹性设计

### 4.1 容错策略矩阵

| 场景 | 策略 | 实现 | 对 LLM 透明 |
|------|------|------|:---:|
| **网络超时** | 指数退避重试 | RetryDecorator | ✅ |
| **限流(429)** | 等待 Retry-After | RateLimitHandler | ✅ |
| **服务降级** | 返回缓存/默认值 | FallbackHandler | ⚠️ 需告知 |
| **部分失败** | 返回部分结果+警告 | PartialResultWrapper | ❌ 必须告知 |
| **数据校验失败** | 立即返回错误 | ValidationError | ❌ 必须告知 |
| **依赖 Skill 失败** | 跳过/终止/人工 | CircuitBreaker | ⚠️ 需告知 |

### 4.2 弹性模式代码

```java
/**
 * 生产级 Skill 执行器：内置弹性策略
 */
@Component
public class ResilientSkillExecutor {

    private final RetryRegistry retryRegistry;
    private final CircuitBreakerRegistry cbRegistry;
    private final MeterRegistry meterRegistry;

    /**
     * 带完整弹性策略的 Skill 执行
     */
    public SkillResult executeWithResilience(
            SkillDefinition skill,
            Map<String, Object> params) {

        // 1. 限流检查
        RateLimiter limiter = getRateLimiter(skill.getName());
        if (!limiter.acquirePermission()) {
            return SkillResult.rateLimited(skill.getName(),
                limiter.getTimeUntilNextPermission());
        }

        // 2. 熔断检查
        CircuitBreaker cb = cbRegistry.circuitBreaker(skill.getName());
        if (cb.getState() == CircuitBreaker.State.OPEN) {
            return SkillResult.circuitOpen(skill.getName());
        }

        // 3. 超时 + 重试 + 熔断
        return Try.of(() -> {
                // 带超时的执行
                return skill.getHandler().execute(params);
            })
            .timeout(Duration.ofSeconds(skill.getTimeout()))
            .retry(skill.getRetryPolicy().getMaxRetries(),
                   this::shouldRetry)
            .recover(throwable -> {
                cb.onError(throwable);  // 记录熔断
                return handleFailure(skill, params, throwable);
            })
            .get();
    }

    private boolean shouldRetry(Throwable t) {
        return t instanceof TimeoutException
            || t instanceof ConnectException
            || (t instanceof HttpClientErrorException ex
                && ex.getStatusCode().value() == 429);  // Rate Limit
    }

    /**
     * 失败降级策略
     */
    private SkillResult handleFailure(SkillDefinition skill,
                                       Map<String, Object> params,
                                       Throwable error) {
        // 策略 1：返回缓存结果
        if (skill.isCacheable()) {
            Optional<SkillResult> cached = cache.get(skill.getName(), params);
            if (cached.isPresent()) {
                return SkillResult.fromCache(cached.get())
                    .withWarning("返回缓存结果，原因：" + error.getMessage());
            }
        }

        // 策略 2：返回降级默认值
        if (skill.hasFallback()) {
            return skill.getFallback().execute(params);
        }

        // 策略 3：返回错误（LLM 自行处理）
        return SkillResult.error(skill.getName(), error);
    }
}
```

---

## 5. 可观测性设计

### 5.1 必须采集的指标

```text
┌─────────────────────────────────────────────────────────┐
│  Skill 调用必须记录的维度                                  │
├─────────────────────────────────────────────────────────┤
│  📊 调用指标（Metrics）                                    │
│  • 调用次数（按 skill_name）                              │
│  • 成功率、失败率                                         │
│  • P50/P95/P99 延迟                                     │
│  • Token 消耗（LLM 调用 Skill 的决策 Token）             │
│                                                          │
│  📝 调用日志（Logging）                                    │
│  • 调用时间、Skill 名称、输入参数（脱敏）                   │
│  • 执行耗时、输出摘要、错误信息                             │
│                                                          │
│  🔍 调用追踪（Tracing）                                    │
│  • TraceID: 完整 Agent 调用链                             │
│  • SpanID: 单个 Skill 调用                               │
│  • Parent Span: 上层编排 Skill                            │
│  • 关联 Agent 决策日志                                    │
└─────────────────────────────────────────────────────────┘
```

### 5.2 可观测性实现

```java
/**
 * Skill 拦截器：自动采集指标、日志、追踪
 */
@Component
@Slf4j
public class SkillObservabilityInterceptor {

    private final MeterRegistry meterRegistry;
    private final Tracer tracer;

    @Around("@annotation(com.example.skills.SkillInfo)")
    public Object observeSkill(ProceedingJoinPoint joinPoint) throws Throwable {
        String skillName = getSkillName(joinPoint);
        Timer.Sample timer = Timer.start(meterRegistry);

        // 追踪
        Span span = tracer.spanBuilder(skillName)
            .setParent(Context.current())
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // 记录输入
            span.setAttribute("skill.input", truncate(joinPoint.getArgs(), 500));
            log.info("[SKILL] {} called with: {}", skillName,
                sanitize(joinPoint.getArgs()));

            // 执行
            Object result = joinPoint.proceed();

            // 记录成功指标
            timer.stop(Timer.builder("skill.duration")
                .tag("skill", skillName)
                .tag("status", "success")
                .register(meterRegistry));
            meterRegistry.counter("skill.calls",
                "skill", skillName, "status", "success").increment();

            span.setAttribute("skill.status", "success");
            return result;

        } catch (Exception e) {
            // 记录失败指标
            timer.stop(Timer.builder("skill.duration")
                .tag("skill", skillName)
                .tag("status", "error")
                .tag("error_type", e.getClass().getSimpleName())
                .register(meterRegistry));
            meterRegistry.counter("skill.calls",
                "skill", skillName, "status", "error").increment();

            span.setAttribute("skill.status", "error");
            span.setAttribute("skill.error", e.getMessage());
            span.setStatus(StatusCode.ERROR);

            log.error("[SKILL] {} failed: {}", skillName, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

---

## 6. 版本管理与兼容性

### 6.1 语义化版本策略

```text
Skill 版本号：MAJOR.MINOR.PATCH

  PATCH (1.0.x)：Bug 修复，行为不变
    ✅ 修复 regex 匹配错误
    ✅ 优化内部实现，结果不变
    → 自动升级，无需通知

  MINOR (1.x.0)：新增参数/能力，向后兼容
    ✅ 新增可选参数
    ✅ 新增 output 字段
    ✅ 支持新的输入格式
    → 建议升级，旧调用方不受影响

  MAJOR (x.0.0)：破坏性变更
    ⚠️ 移除参数
    ⚠️ 修改 output 结构
    ⚠️ 改变行为语义
    → 旧版本保留 N 个月，新调用方用新版本
```

### 6.2 多版本共存

```java
/**
 * 多版本 Skill 共存
 */
@Component
public class VersionedSkillRegistry {

    // 同一 Skill 的多个版本同时在线
    private final Map<String, VersionedSkill> skills = new ConcurrentHashMap<>();

    public void register(SkillDefinition skill) {
        String key = skill.getName() + "@" + skill.getVersion();
        skills.put(key, new VersionedSkill(skill));

        // 更新默认版本指向（最新稳定版）
        skills.put(skill.getName() + "@latest", new VersionedSkill(skill));
    }

    /**
     * 版本路由：
     * - 新调用方 → 使用 latest
     * - 已指定版本 → 使用指定版本
     */
    public SkillExecutor getExecutor(String skillName, String version) {
        String resolvedVersion = version != null
            ? version
            : resolveDefaultVersion(skillName);

        return skills.get(skillName + "@" + resolvedVersion).getExecutor();
    }

    /**
     * 废弃窗口管理
     */
    @Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨 2 点
    public void cleanupDeprecatedVersions() {
        skills.values().stream()
            .filter(vs -> vs.isDeprecated()
                && vs.getDeprecatedAt().isBefore(Instant.now().minus(90, ChronoUnit.DAYS)))
            .forEach(vs -> {
                skills.remove(vs.getKey());
                log.info("Removed deprecated skill: {}", vs.getKey());
            });
    }
}
```

---

## 7. 常见反模式

### 7.1 十大反模式

| # | 反模式 | 问题 | 正确做法 |
|---|--------|------|----------|
| 1 | **万能 Skill** | 一个 Skill 做所有事 | 按单一职责拆分 |
| 2 | **裸 Tool 暴露** | 把 DB 查询直接暴露 | 封装业务语义的 Skill |
| 3 | **描述缺失** | `name: "doit" description: ""` | 完善三层描述 |
| 4 | **静默失败** | `catch(Exception e) { return null; }` | 返回结构化错误 |
| 5 | **无限等待** | 不设 timeout | 所有 Skill 必须有超时 |
| 6 | **无幂等性** | 扣款 Skill 不检查重复 | 引入幂等键 |
| 7 | **硬编码配置** | Skill 内写死 URL/Key | 外部化配置 |
| 8 | **无版本管理** | 直接改代码上线 | 语义化版本 + 共存 |
| 9 | **Token 浪费** | 返回大量无用数据 | 只返回 LLM 需要的字段 |
| 10 | **无监控** | 上线后完全黑盒 | 至少记录调用次数+错误 |

### 7.2 反模式代码示例

```java
// ❌ 反模式 1：万能 Skill
@Tool("处理用户请求")
public Object handleRequest(String request) {
    // 一个函数里做了搜索、分析、创建、通知所有事
    if (request.contains("搜索")) { ... }
    else if (request.contains("分析")) { ... }
    else if (request.contains("创建")) { ... }
    else { ... }
    // LLM 无法精确选择，只能全量传入
}

// ✅ 正确：拆分为独立 Skill
@Tool("搜索知识库中的文档") public SearchResult searchDocs(...) { }
@Tool("分析代码质量")        public AnalysisResult analyzeCode(...) { }
@Tool("创建工作项")          public WorkItem createTask(...) { }

// ❌ 反模式 4：静默失败
@Tool("部署服务")
public String deploy(String serviceName) {
    try {
        return deployService.deploy(serviceName);
    } catch (Exception e) {
        return "部署失败";  // LLM 不知道原因，无法决策
    }
}

// ✅ 正确：返回结构化错误
@Tool("部署服务")
public DeployResult deploy(String serviceName) {
    try {
        return deployService.deploy(serviceName);
    } catch (DeployException e) {
        return DeployResult.error()
            .code(e.getCode())
            .message(e.getMessage())
            .retryable(e.isRetryable())
            .suggestion(e.getSuggestion())
            .build();
    }
}

// ❌ 反模式 9：Token 浪费
@Tool("读取文件")
public String readFile(String path) {
    return Files.readString(Path.of(path));  // 10万行代码全返回！
}

// ✅ 正确：返回摘要 + 分页接口
@Tool("读取文件")
public FileContent readFile(String path,
    @ToolParam("起始行，默认1") Integer startLine,
    @ToolParam("读取行数，默认200") Integer maxLines
) {
    return fileService.read(path, startLine, maxLines);
}
```

> 🎯 **核心要点**：好的 Skill = 精准的命名描述 + 严格的参数校验 + 内置的容错机制 + 完善的可观测性 + 规范的版本管理。避开十大反模式，Skill 体系才能持续健康运转

---

**上一模块**：[05 - Agent Skills 框架实现对比](./05-Agent%20Skills框架实现对比.md)  
**下一模块**：[07 - Agent Skills 安全与测试](./07-Agent%20Skills安全与测试.md)  
**返回总览**：[00 - Agent Skills 知识体系总览](./00-Agent%20Skills知识体系总览.md)
