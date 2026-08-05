# 03 - Sentinel 核心原理与扩展机制

> Sentinel 不是简单的"if (QPS > 100) reject"，它的 Slot Chain 架构、滑动窗口统计和 SPI 扩展体系才是真正的内核。理解内核，才能驾驭各种流控场景。

---

## 📚 目录

1. [Sentinel 整体架构](#1-sentinel-整体架构)
2. [Slot Chain 责任链深度解析](#2-slot-chain-责任链深度解析)
3. [核心 Slot 源码级分析](#3-核心-slot-源码级分析)
4. [流控算法原理](#4-流控算法原理)
5. [滑动窗口统计机制](#5-滑动窗口统计机制)
6. [SPI 扩展机制](#6-spi-扩展机制)
7. [Sentinel vs Resilience4j vs Hystrix](#7-sentinel-vs-resilience4j-vs-hystrix)

---

## 1. Sentinel 整体架构

### 1.1 核心组件关系

```text
┌─────────────────────────────────────────────────────────┐
│                    Sentinel 整体架构                       │
│                                                          │
│  ┌─────────────┐     ┌─────────────┐    ┌────────────┐  │
│  │  Dashboard  │────▶│  Transport  │───▶│  App (JVM) │  │
│  │  (控制台)    │     │  (规则下发)  │    │            │  │
│  └─────────────┘     └─────────────┘    └─────┬──────┘  │
│                                               │         │
│                                        ┌──────▼──────┐  │
│                                        │  Slot Chain │  │
│                                        │  (核心链路)  │  │
│                                        └──────┬──────┘  │
│                                               │         │
│                     ┌─────────────────────────┼─────┐   │
│                     │                         │     │   │
│                ┌────▼────┐  ┌──────────┐  ┌──▼───┐│   │
│                │ 统计模块 │  │ 规则管理  │  │日志  ││   │
│                │(滑动窗口)│  │          │  │(Metric)│   │
│                └─────────┘  └──────────┘  └──────┘│   │
│                     └─────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 1.2 一次请求经过 Sentinel 的完整路径

```text
Entry entry = null;
try {
    entry = SphU.entry("resourceName");
    // → 进入 Slot Chain

    // 你的业务逻辑
    doBusiness();

} catch (BlockException e) {
    // 被流控/熔断 → 降级处理
    fallback();
} finally {
    if (entry != null) {
        entry.exit();  // 退出 → 统计完成
    }
}
```

---

## 2. Slot Chain 责任链深度解析

### 2.1 7 个核心 Slot（按顺序）

```text
SphU.entry("resource")
  │
  ▼
┌──────────────────────────────────────┐
│ 1. NodeSelectorSlot                  │  构建调用树节点
│    → 为每个上下文(context)创建       │
│      不同的 DefaultNode              │
├──────────────────────────────────────┤
│ 2. ClusterBuilderSlot                │  构建集群节点
│    → 为每个资源创建唯一的            │
│      ClusterNode（统计汇总用）       │
├──────────────────────────────────────┤
│ 3. LogSlot                           │  记录日志
│    → 记录 BlockException 日志        │
├──────────────────────────────────────┤
│ 4. StatisticSlot                     │  ⭐ 统计核心
│    → 先申请后面的 Slot 检查          │
│    → 通过：记录 pass + RT            │
│    → 拒绝：记录 block                │
├──────────────────────────────────────┤
│ 5. AuthoritySlot                     │  授权规则
│    → 黑白名单检查                    │
├──────────────────────────────────────┤
│ 6. SystemSlot                        │  系统规则
│    → 系统负载/CPU 保护               │
├──────────────────────────────────────┤
│ 7. FlowSlot                          │  流控规则
│    → QPS / 并发线程 / 排队           │
├──────────────────────────────────────┤
│ 8. DegradeSlot                       │  熔断规则
│    → 慢调用 / 异常比例 / 异常数      │
└──────────────────────────────────────┘
  │
  ▼
(业务逻辑执行)
  │
  ▼
entry.exit() → StatisticSlot 记录 RT 和完成状态
```

### 2.2 Slot Chain 构建（SPI）

```java
// Sentinel 通过 SPI 加载 Slot Chain
// 文件：META-INF/services/com.alibaba.csp.sentinel.slotchain.SlotChainBuilder

// 默认实现：
@Spi(isDefault = true)
public class DefaultSlotChainBuilder implements SlotChainBuilder {
    @Override
    public ProcessorSlotChain build() {
        ProcessorSlotChain chain = new DefaultProcessorSlotChain();
        chain.addLast(new NodeSelectorSlot());    // 1
        chain.addLast(new ClusterBuilderSlot());  // 2
        chain.addLast(new LogSlot());             // 3
        chain.addLast(new StatisticSlot());       // 4
        chain.addLast(new AuthoritySlot());       // 5
        chain.addLast(new SystemSlot());          // 6
        chain.addLast(new FlowSlot());            // 7
        chain.addLast(new DegradeSlot());         // 8
        return chain;
    }
}
```

---

## 3. 核心 Slot 源码级分析

### 3.1 StatisticSlot：统计核心

```java
// StatisticSlot 核心逻辑（简化）
@Override
public void entry(Context context, ResourceWrapper resource,
                  DefaultNode node, int count, boolean prioritized,
                  Object... args) throws Throwable {

    try {
        // 1. 先触发后续 Slot（FlowSlot、DegradeSlot 等）
        fireEntry(context, resource, node, count, prioritized, args);

        // 2. 走到这里 = 通过了所有规则检查
        //    记录通过统计
        node.increaseThreadNum();             // 并发线程数 +1
        node.addPassRequest(count);           // 通过请求数 +count

    } catch (PriorityWaitException ex) {
        // 排队等待（匀速器模式）
        node.increaseThreadNum();
        // ... 等待逻辑
        node.decreaseThreadNum();

    } catch (BlockException e) {
        // 3. 被拦截
        node.increaseBlockQps(count);         // 拦截数 +count
        throw e;

    } catch (Throwable e) {
        // 4. 业务异常（非流控异常）
        context.getCurEntry().setError(e);
        throw e;
    }
}

@Override
public void exit(Context context, ResourceWrapper resource,
                 int count, Object... args) {
    DefaultNode node = (DefaultNode) context.getCurNode();

    // 记录响应时间
    long rt = TimeUtil.currentTimeMillis() - context.getCurEntry().getCreateTime();
    node.addRtAndSuccess(rt, count);          // RT + 成功数

    node.decreaseThreadNum();                 // 并发线程数 -1

    fireExit(context, resource, count, args);
}
```

### 3.2 FlowSlot：流控检查

```java
// FlowSlot 核心逻辑
@Override
public void entry(Context context, ResourceWrapper resource,
                  DefaultNode node, int count, boolean prioritized,
                  Object... args) throws Throwable {

    // 1. 获取该资源的所有流控规则
    List<FlowRule> rules = FlowRuleManager.getRules();

    // 2. 逐条检查
    for (FlowRule rule : rules) {
        if (!FlowRuleChecker.checkFlow(rule, context, node, count)) {
            // 被流控！
            throw new FlowException(rule.getLimitApp(), rule);
        }
    }

    // 3. 通过 → 继续下一个 Slot
    fireEntry(context, resource, node, count, prioritized, args);
}
```

### 3.3 DegradeSlot：熔断检查

```java
// DegradeSlot 核心逻辑
@Override
public void entry(Context context, ResourceWrapper resource,
                  DefaultNode node, int count, boolean prioritized,
                  Object... args) throws Throwable {

    // 1. 获取该资源的所有熔断规则
    List<DegradeRule> rules = DegradeRuleManager.getRules();

    // 2. 逐条检查
    for (DegradeRule rule : rules) {
        if (!DegradeRuleChecker.checkDegrade(rule, context, node, count)) {
            // 熔断打开！
            throw new DegradeException(rule.getLimitApp(), rule);
        }
    }

    fireEntry(context, resource, node, count, prioritized, args);
}
```

---

## 4. 流控算法原理

### 4.1 四种流控模式

```text
模式 1：直接拒绝（默认）
  QPS > 阈值 → 直接抛出 FlowException
  实现：简单判断 slidingWindow.pass() >= threshold

模式 2：Warm Up（预热/令牌桶）
  启动时 QPS 阈值从 threshold/3 逐步升到 threshold
  实现：Guava RateLimiter 的 SmoothWarmingUp
  场景：防止冷启动时打垮下游

模式 3：排队等待（匀速器/漏桶）
  超过阈值的请求排队，等待 timeout 毫秒
  实现：LeakyBucket，线程 wait(timeout)
  场景：处理突发流量（消息队列消费）

模式 4：关联流控
  当资源 B 的 QPS 超过阈值时，限制资源 A
  实现：检查关联资源的统计值
  场景：写操作（B）压力大时，限制读操作（A）
```

### 4.2 流控效果代码实现

```java
// Sentinel 流控效果的核心实现路径

// 快速失败（默认）
public class DefaultController implements TrafficShapingController {
    @Override
    public boolean canPass(Node node, int acquireCount, boolean prioritized) {
        // 直接对比 QPS 和阈值
        return node.passQps() + acquireCount <= threshold;
    }
}

// Warm Up（令牌桶）
public class WarmUpController implements TrafficShapingController {
    // 基于 Guava SmoothWarmingUp 实现
    // storedTokens: 当前存储的令牌数
    // warningToken: 预警令牌数
    // maxToken: 最大令牌数

    @Override
    public boolean canPass(Node node, int acquireCount, boolean prioritized) {
        // 1. 计算当前令牌数
        long previousQps = node.previousPassQps();
        syncToken(previousQps);

        // 2. 令牌数 > 预警值 → 开始限流
        if (storedTokens.get() > warningToken) {
            long aboveToken = storedTokens.get() - warningToken;
            // 预热期越长，限流越严格
            double warmingQps = 1.0 / (aboveToken * slope + 1.0 / maxCount);
            // ...
        }
    }
}

// 排队等待（漏桶）
public class RateLimiterController implements TrafficShapingController {
    private final long maxQueueingTimeMs;  // 最大等待时间

    @Override
    public boolean canPass(Node node, int acquireCount, boolean prioritized) {
        // 计算预期通过时间
        long currentTime = TimeUtil.currentTimeMillis();
        long costTime = Math.round(1.0 * acquireCount / count * 1000);

        long expectedTime = costTime + latestPassedTime.get();

        if (expectedTime <= currentTime) {
            // 无需等待，直接通过
            latestPassedTime.set(currentTime);
            return true;
        } else {
            // 需要等待
            long waitTime = costTime + latestPassedTime.get() - currentTime;
            if (waitTime > maxQueueingTimeMs) {
                return false;  // 等待超时 → 拒绝
            }
            // 等待 waitTime ms...
            Thread.sleep(waitTime);
            return true;
        }
    }
}
```

---

## 5. 滑动窗口统计机制

### 5.1 滑动窗口数据结构

```text
Sentinel 使用 LeapArray（环形滑动窗口）统计

时间轴 →
│ 窗口0 │ 窗口1 │ 窗口2 │ ... │ 窗口N-1 │ 窗口N │
│ 9:00  │ 9:01  │ 9:02  │     │ 9:59    │10:00  │
└──────────────────────────────────────────────┘
                ← 滑动窗口（1分钟 = 60 个 1s 窗口）

样本数 = 统计时长 / 窗口长度
  例：统计过去 60 秒 → 60 个 1 秒窗口

每个窗口内记录：
  ├── passQps: 通过数
  ├── blockQps: 拦截数
  ├── completeQps: 完成数
  ├── exceptionQps: 异常数
  ├── rt: 平均响应时间
  └── threadCount: 当前并发线程数
```

### 5.2 源码结构

```java
// LeapArray 核心结构
public abstract class LeapArray<T> {
    protected int windowLengthInMs;  // 窗口长度（如 1000ms）
    protected int sampleCount;       // 样本数（如 60）
    protected int intervalInMs;      // 总时间间隔（如 60000ms）

    protected final AtomicReferenceArray<WindowWrap<T>> array;

    // 获取当前窗口
    public WindowWrap<T> currentWindow() {
        long timeMillis = TimeUtil.currentTimeMillis();
        long timeId = timeMillis / windowLengthInMs;
        int idx = (int)(timeId % array.length());

        // CAS 重置过期窗口
        WindowWrap<T> old = array.get(idx);
        if (old == null || old.windowStart() < timeMillis - intervalInMs) {
            WindowWrap<T> window = new WindowWrap<>(windowLengthInMs, timeMillis, ...);
            if (array.compareAndSet(idx, old, window)) {
                return window;
            }
        }
        return old;
    }

    // 统计所有窗口的值
    public List<T> values() {
        return getValidValues(TimeUtil.currentTimeMillis());
    }
}
```

### 5.3 为什么用滑动窗口

```text
固定窗口 vs 滑动窗口：

固定窗口：
  窗口: [0s ~ 1s]
  QPS 阈值: 100
  问题：0.5s~1s 来了 100 个请求（打满阈）
       1s~1.5s 又来了 100 个请求
       实际上 0.5s~1.5s 这个 1 秒窗口有 200 个请求！
  → 统计"错位"导致实际流量是阈值的 2 倍

滑动窗口：
  窗口: 以 1s 为单位滑动，统计过去 60 个窗口
  任何 1 秒窗口都不会超过阈值
  → 精确控制
```

---

## 6. SPI 扩展机制

### 6.1 Sentinel 的 SPI 扩展点

```text
核心扩展接口：

1. ProcessorSlot / SlotChainBuilder
   → 自定义 Slot，插入到责任链

2. InitFunc
   → 初始化回调（Sentinel 启动时执行）
   → 用于注册自定义规则数据源

3. Transport
   → 自定义通信协议（默认 HTTP）
   → 可实现 gRPC 传输

4. MetricExtension
   → 自定义指标扩展（对接 Prometheus 等）

5. UrlCleaner
   → REST URL 清洗（/user/123 → /user/{id}）
```

### 6.2 自定义 Slot 示例

```java
// 实现一个"日志审计 Slot"：记录所有被流控的请求
@Spi(isSingleton = false, order = -5000)  // 插入到最前面
public class AuditLogSlot extends AbstractLinkedProcessorSlot<DefaultNode> {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("sentinel-audit");

    @Override
    public void entry(Context context, ResourceWrapper resource,
                      DefaultNode node, int count, boolean prioritized,
                      Object... args) throws Throwable {
        try {
            fireEntry(context, resource, node, count, prioritized, args);
            // 通过：记录
            AUDIT_LOG.info("PASS | resource={} | context={} | count={}",
                resource.getName(), context.getName(), count);
        } catch (BlockException e) {
            // 被拦截：记录
            AUDIT_LOG.warn("BLOCK | resource={} | rule={} | context={}",
                resource.getName(), e.getRule().getClass().getSimpleName(),
                context.getName());
            throw e;
        }
    }

    @Override
    public void exit(Context context, ResourceWrapper resource,
                     int count, Object... args) {
        fireExit(context, resource, count, args);
    }
}
```

---

## 7. Sentinel vs Resilience4j vs Hystrix

```text
┌──────────────┬──────────────┬──────────────┬──────────────┐
│    特性       │  Sentinel     │ Resilience4j │   Hystrix    │
├──────────────┼──────────────┼──────────────┼──────────────┤
│ 维护状态      │ ✅ 活跃       │ ✅ 活跃      │ ❌ 停维      │
│ 流控模式      │ QPS/线程/排队 │ 信号量/线程  │ 线程池      │
│ 熔断策略      │ 慢调用/异常   │ 慢调用/异常  │ 异常比例    │
│ 统计方式      │ 滑动窗口     │ 滑动窗口     │ 滑动窗口    │
│ 实时监控      │ ✅ 控制台    │ ❌ 无自带    │ ✅ Dashboard│
│ 规则动态修改  │ ✅ 实时      │ ⚠️ 需代码    │ ⚠️ 需代码   │
│ 规则持久化    │ ✅ Nacos/文件│ ❌           │ ❌          │
│ 集群流控      │ ✅           │ ❌           │ ❌          │
│ 系统自适应    │ ✅           │ ❌           │ ❌          │
│ 网关流控      │ ✅           │ ⚠️           │ ❌          │
│ 扩展性(SPI)   │ ✅ 丰富      │ ⚠️ 有限      │ ⚠️ 有限     │
│ 性能损耗      │ ⭐⭐⭐ 极低   │ ⭐⭐⭐ 极低  │ ⭐⭐ 较高   │
└──────────────┴──────────────┴──────────────┴──────────────┘

结论：
  → 国内 Java 微服务 → Sentinel（生态、控制台、规则管理）
  → 国际/非 Spring 项目 → Resilience4j（轻量、函数式）
  → 遗留系统 → 迁移到 Sentinel 或 Resilience4j（Hystrix 已死）
```

---

> 🎯 **核心要点**：Sentinel 的 Slot Chain 是理解其运行机制的关键——每个 Slot 职责单一，通过 SPI 可插拔。流控的三种效果（快速失败/WarmUp/排队等待）覆盖了削峰填谷的全部场景。滑动窗口的精确统计 + SPI 的灵活扩展，是 Sentinel 在 Java 微服务生态中力压 Hystrix 和 Resilience4j 的核心竞争力。

---

**下一模块**：[04 - Sentinel 生产级规则治理](./04-Sentinel生产级规则治理.md)  
**返回总览**：[00 - 组件体系总览](./00-SpringCloudAlibaba组件体系总览.md)
