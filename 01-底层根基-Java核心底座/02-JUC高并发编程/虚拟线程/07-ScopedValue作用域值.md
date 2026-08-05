# ScopedValue 作用域值

> ThreadLocal 在百万虚拟线程时代的内存放大问题，催生了 ScopedValue——不可变、有界生命周期、读取接近局部变量的上下文传递方案。JEP 506 已于 JDK 25 LTS 转正，是 Loom 三件套中第二块落地的拼图

---

## 📚 目录

1. [ThreadLocal 在虚拟线程时代的三大痛点](#1-threadlocal-在虚拟线程时代的三大痛点)
2. [JEP 演进史：从孵化到转正](#2-jep-演进史从孵化到转正)
3. [核心 API 与基本用法](#3-核心-api-与基本用法)
4. [与 ThreadLocal 的全方位对比](#4-与-threadlocal-的全方位对比)
5. [实现原理：为何"几乎免费"](#5-实现原理为何几乎免费)
6. [继承机制：子任务自动传递](#6-继承机制子任务自动传递)
7. [生产实践：traceId 与用户上下文](#7-生产实践traceid-与用户上下文)
8. [限制与边界](#8-限制与边界)
9. [核心要点与思考题](#9-核心要点与思考题)

---

## 1. ThreadLocal 在虚拟线程时代的三大痛点

### 1.1 痛点一：内存放大

```java
// 平台线程时代：几十个线程 × 每个 1KB 上下文 = 几十 KB，无所谓
ThreadLocal<RequestContext> ctx = new ThreadLocal<>();

// 虚拟线程时代：10 万并发 × 每个 1KB = 100MB 堆，仅用于"传参数"
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 100_000; i++) {
        executor.submit(() -> {
            ctx.set(new RequestContext(traceId, userId));   // 每虚拟线程一份副本
            service.handle();
            ctx.remove();                                    // 忘了 remove 就被 GC 兜底
        });
    }
}
```

- 线程数提升 3-4 个数量级，ThreadLocal 副本数同比例放大；
- 每个虚拟线程的 ThreadLocal 值**即使被 GC，也造成了分配与回收的 CPU/GC 压力**；
- JDK 自己都因此移除了若干 ThreadLocal 缓存（如内部 byte[] 缓冲）——见 08 章。

### 1.2 痛点二：生命周期不可控

```java
// ThreadLocal 的写入（set）与清理（remove）是"手动约定"，代码里处处可能泄漏
// 线程池场景：set 后忘 remove → 下一个任务读到上一个任务的脏数据（经典 bug）
// 虚拟线程场景：用完即弃，脏数据少一些，但"何时清理"仍无结构性保证
```

### 1.3 痛点三：继承与异步断裂

```java
ThreadLocal 不跨虚拟线程 / 不跨异步链传播：
- 虚拟线程不继承普通 ThreadLocal（仅 InheritableThreadLocal，且官方建议关闭）
- CompletableFuture.thenApplyAsync() 换线程后 ThreadLocal 全丢
- 所以 traceId 在异步代码里"断链"，排查全靠手动传递参数
```

> 🎯 **核心要点**：ThreadLocal 的三个问题（内存放大、生命周期手动、异步断链）在虚拟线程时代被集中引爆——JDK 需要一个新的上下文传递原语，这就是 ScopedValue。

## 2. JEP 演进史：从孵化到转正

| JEP | 版本 | 阶段 | 关键变化 |
|-----|:---:|:---:|---------|
| JEP 429 | JDK 20 | 孵化器 | `ScopedValue` 初版 |
| JEP 446 | JDK 21 | 预览 | API 细化 |
| JEP 464 | JDK 22 | 预览 | 增量完善 |
| JEP 481 | JDK 23 | 预览 | 增量完善 |
| JEP 487 | JDK 24 | 预览 | 增量完善 |
| **JEP 506** | **JDK 25 LTS** | **✅ 转正** | 唯一变更：**`orElse` 不再接受 `null` 参数**（禁止返回 null 的缺省值，逼开发者明确表达"无值"） |

> ⚠️ **版本窗口**：JDK 25 之前是预览 API（需 `--enable-preview`）；JDK 25+ 直接使用，无任何 flag。与结构化并发（JEP 525 仍是预览）形成对比——ScopedValue 已是生产标准特性。

## 3. 核心 API 与基本用法

### 3.1 三件套：声明 → 绑定 → 读取

```java
// 1. 声明：static final，类加载时创建
class RequestHandler {
    private static final ScopedValue<String> TRACE_ID = ScopedValue.newInstance();
    private static final ScopedValue<User> CURRENT_USER = ScopedValue.newInstance();

    // 2. 绑定：where().run()，绑定只作用于 run 的执行期内
    void handle(String traceId, User user) {
        ScopedValue.where(TRACE_ID, traceId)
                   .where(CURRENT_USER, user)        // 可连续绑定多个
                   .run(() -> {
                       doWork();                     // 此处及所有间接调用可读取
                   });
        // 离开 run 后：绑定自动销毁，无需任何清理代码
    }

    void doWork() {
        String traceId = TRACE_ID.get();             // 3. 读取：任意深度调用栈内
        User user = CURRENT_USER.get();
        log.debug("trace={} user={}", traceId, user);
    }
}
```

### 3.2 完整方法清单

| 方法 | 语义 |
|------|------|
| `ScopedValue.newInstance()` | 创建作用域值（static final 持有） |
| `where(ScopedValue<T>, T)` | 绑定一个值，返回绑定组合（可链式） |
| `run(Runnable)` | 在绑定生效期间执行（无返回值） |
| `call(Callable<T>)` | 在绑定生效期间执行并返回值 |
| `get()` | 读取绑定值；**未绑定时抛 `NoSuchElementException`** |
| `orElse(T other)` | 未绑定时返回缺省值；**JDK 25 起 other 不能为 null** |
| `orElseThrow()` / `orElseThrow(Supplier)` | 未绑定时抛异常 |
| `isBound()` | 当前线程是否绑定了该值 |

```java
// 未绑定时的三种处理
String id = TRACE_ID.get();                        // 未绑定 → NoSuchElementException（fail fast）
String id = TRACE_ID.orElse("unknown");            // 未绑定 → "unknown"
String id = TRACE_ID.orElseThrow(() -> new IllegalStateException("缺少 traceId"));
```

> ⚠️ **易错点**：`orElse(null)` 在 JDK 25+ 是编译错误。如果业务上确实需要"缺省为空"，用 `orElseThrow` 或 `Optional` 包装——JDK 的意图是逼迫显式处理缺失，而不是静默吞掉。

## 4. 与 ThreadLocal 的全方位对比

| 维度 | ThreadLocal | ScopedValue |
|------|:---:|:---:|
| 可变性 | 可变（set 任意次数） | **不可变**（绑定后不可改） |
| 生命周期 | 线程存续期，手动 remove | **有界**：仅 run/call 内，离开自动销毁 |
| 写入方式 | 任意位置 set | 只能在绑定处 `where()` 指定一次 |
| 内存 | 每线程一份副本（Map + 引用链） | **单副本**，随调用栈传递（绑定栈帧内共享） |
| 读取速度 | Map 查找（有 JIT 优化） | 接近**局部变量读取** |
| 清理保证 | 手动 remove / 线程复用泄漏风险 | **结构性保证**：离开作用域自动清理 |
| 继承（子线程） | 普通不继承；Inheritable 可继承但被官方建议禁用 | **结构化并发子任务自动继承**（JDK 25+） |
| 跨异步链 | 断裂 | 结构化并发场景可用 |
| 转正版本 | JDK 1.2 | **JDK 25（JEP 506）** |

> 💡 **一句话选型**：只读上下文（traceId、userId、租户、配置快照）→ **ScopedValue**；确需线程内可变的局部状态（少见）→ 谨慎用 ThreadLocal 并严控 size 与生命周期。

## 5. 实现原理：为何"几乎免费"

### 5.1 与 ThreadLocal 的实现差异

```text
ThreadLocal 实现（每线程哈希表）：
Thread → ThreadLocalMap（数组散列）→ 每个 ThreadLocal 一个 Entry
  读取 = 哈希 + 探测，写入 = 分配 Entry；线程多 → 表多 → 内存线性放大

ScopedValue 实现（栈式绑定链，JDK 内部）：
线程执行栈上维护"绑定链"（ScopedValueBindings，类似方法调用栈的 SideTable）
  绑定 = 把 (ScopedValue, value) 压入绑定链（不可变快照语义）
  读取 = 沿绑定链查找（深度浅，常数级）
  离开 run = 弹栈（结构上自动清理）
```

关键点：

- **值不随线程数复制**：绑定与"线程"解耦，挂在**执行上下文**（可随虚拟线程迁移）上；
- **虚拟线程友好**：虚拟线程的 Continuation 保存/恢复执行状态时，绑定链随之迁移——挂起期间绑定不丢，恢复后照常读取；
- **读取极快**：JIT 可以把 `get()` 优化为对绑定链的局部访问，官方数据：**与读取局部变量同数量级**。

### 5.2 为什么不可变反而更强

```text
可变（ThreadLocal）→ 谁都能改 → 读取方永远不知道当前是什么值 → 必须小心
不可变（ScopedValue）→ 只有绑定处定义一次 → 整个调用子树内"值恒定"
    → 并发安全：不需要同步，因为没人能改
    → 可缓存：值不变，JIT 可以大胆优化
    → 可传递：子任务看到的必然是父任务绑定时的值（无竞态）
```

> 🎯 **核心要点**：不可变 + 有界生命周期 + 单副本 = ScopedValue 三个"快"的来源（无同步、无每线程复制、常量优化）。它不是 ThreadLocal 的小改款，而是不同性质的机制。

## 6. 继承机制：子任务自动传递

### 6.1 与结构化并发联动（JDK 25+）

```java
// 父作用域绑定 → fork 的子任务自动继承（JEP 505 起成为正式行为）
static final ScopedValue<String> TRACE_ID = ScopedValue.newInstance();

void parent(String traceId) throws Exception {
    ScopedValue.where(TRACE_ID, traceId).run(() -> {
        try (var scope = StructuredTaskScope.open(Joiner.allSuccessfulOrThrow())) {
            scope.fork(() -> fetchA());      // 子任务1：能读到 TRACE_ID
            scope.fork(() -> fetchB());      // 子任务2：能读到 TRACE_ID
            scope.join();
        }
    });
}
```

| 场景 | ThreadLocal | ScopedValue |
|------|:---:|:---:|
| 同线程内直接调用 | ✅ | ✅ |
| 结构化并发的子任务（fork） | ❌（不继承） | ✅ 自动继承 |
| 普通 `new Thread` / 执行器任务 | 部分（Inheritable，不推荐） | 不自动（需手动传递或换结构化并发） |
| 虚拟线程 | 不继承 | 结构化并发下继承 |

### 6.2 非结构化场景的手动传递

```java
// 老代码（普通 ExecutorService）的过渡方案：显式传递绑定
String traceId = TRACE_ID.get();
executor.submit(() -> ScopedValue.where(TRACE_ID, traceId)
                                 .run(() -> childTask()));
```

## 7. 生产实践：traceId 与用户上下文

### 7.1 标准模板：Web 请求上下文

```java
// 过滤器/拦截器入口绑定
public class TraceFilter implements Filter {
    static final ScopedValue<String> TRACE_ID = ScopedValue.newInstance();

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
        String traceId = req.getHeader("X-Trace-Id") != null
                ? req.getHeader("X-Trace-Id") : UUID.randomUUID().toString();
        ScopedValue.where(TRACE_ID, traceId).run(() -> chain.doFilter(req, res));
        // 请求结束：自动销毁，无需 remove；所有下游代码（含异步子任务）可读取
    }
}
```

### 7.2 最佳实践清单

```text
□ 声明一律 static final，放在持有/入口类上
□ 绑定点放在"作用域入口"（过滤器、切面、消息消费入口），而不是深层次代码
□ 值只读——需要可变上下文就用普通参数或组合对象
□ 读取处用 orElseThrow 显式处理缺失（不要 orElse(null)）
□ 日志框架（SLF4J 2.x MDC 适配器）已在跟进 ScopedValue 支持，切换前先查版本
□ 与 ThreadLocal 混用场景：明确区分"可变线程状态"（TL）与"只读上下文"（SV）
```

### 7.3 典型收益量化

```text
场景：10 万并发虚拟线程，每线程 ThreadLocal 存 1KB 上下文
    ThreadLocal：峰值 +100MB 堆，GC 压力明显
    ScopedValue：绑定链在栈上共享，额外内存 ≈ 常量级，读取更快
```

## 8. 限制与边界

| 限制 | 说明 | 应对 |
|------|------|------|
| 不可变 | 绑定后不能修改值 | 需要变化的值用可变对象包装或参数 |
| 不能嵌套重绑定同一 key | `where(X, 1).run(() -> where(X, 2))` 内层 get 会报错 | 分层用不同 ScopedValue 或显式传递 |
| 读取未绑定值抛异常 | `get()` 抛 `NoSuchElementException` | `orElse` / `orElseThrow` |
| 非结构化线程不自动继承 | 普通线程池/`new Thread` 场景不传递 | 手动 `where(...).run()` 包裹任务 |
| 调试可见性 | 转储中无 ThreadLocal 那样的可视化 | JFR/JMX 生态适配中 |
| 库生态适配 | 多数库仍读 ThreadLocal（如旧版 MDC） | 入口处做 TL→SV 桥接 |

## 9. 核心要点与思考题

> 🎯 **本章三句话总结**：
> 1. ThreadLocal 的"每线程副本 + 手动清理 + 异步断链"在百万虚拟线程下是内存与正确性双重负担；
> 2. ScopedValue（JEP 506，JDK 25 转正）用"不可变 + 有界生命周期 + 单副本"实现接近局部变量的读取成本，且结构化并发子任务自动继承；
> 3. 生产选型：只读上下文一律 ScopedValue，可变的线程局部状态才考虑 ThreadLocal（严控生命周期）。

**思考题**：

1. 为什么 ScopedValue 不需要同步机制？（→ 5.2）
2. `orElse(null)` 为什么在 JDK 25 被禁止？（→ 3.2）
3. 虚拟线程挂起（unmount）期间 ScopedValue 绑定会丢吗？（→ 5.1）
4. 普通线程池 + ThreadLocal 的老代码怎么平滑过渡到 ScopedValue？（→ 6.2/7.2）

---

**下一模块**：[08-性能、可观测性与生产实践](08-性能、可观测性与生产实践.md)｜**返回总览**：[00-虚拟线程知识体系总览](00-虚拟线程知识体系总览.md)
