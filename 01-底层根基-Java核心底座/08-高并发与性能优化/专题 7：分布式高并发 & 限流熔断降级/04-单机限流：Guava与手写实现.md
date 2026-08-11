# 04 单机限流：Guava 与手写实现

> 分布式限流之前，先掌握单机限流的两个实现范式：Guava RateLimiter 的令牌桶变体与 Sentinel 的滑动窗口采样——它们也是面试手写的两个标准答案

---

## 📚 目录

1. [Guava RateLimiter：令牌桶的 Java 表达](#1-guava-ratelimiter令牌桶的-java-表达)
2. [SmoothBursty 与 SmoothWarmingUp](#2-smoothbursty-与-smoothwarmingup)
3. [手写令牌桶：一个可以写进简历的实现](#3-手写令牌桶一个可以写进简历的实现)
4. [Sentinel 单机滑动窗口采样](#4-sentinel-单机滑动窗口采样)
5. [单机限流的边界](#5-单机限流的边界)

---

## 1. Guava RateLimiter：令牌桶的 Java 表达

Guava 的 RateLimiter 是经典教科书实现，两个核心状态：

- `storedPermits`：当前积攒的令牌数，上限为桶容量（SmoothBursty 默认 `maxBurstSeconds=1`，即最多攒 1 秒的令牌）。
- `nextFreeTicketMicros`：下一次可以发放令牌的时间——这是它区别于"定时补充"的关键：**没有定时器**，采用惰性计算，获取令牌时按时间差一次性补算应生成的令牌数，再扣减。

`tryAcquire()` 拿不到令牌直接返回 false（非阻塞快速失败），`acquire()` 则阻塞排队等待。核心逻辑一句话：**按时间差补令牌，扣令牌后推进 nextFreeTicketMicros**。因为所有操作只依赖当前时间戳与两个 long 变量，单机无锁即可，性能纳秒级。

两个 API 的语义要分清：`acquire()` 的阻塞是"预算排队"——它记录自己需要等待的令牌量并推进 nextFreeTicketMicros，调用线程真正 sleep 到放行时间，多个 acquire 并发时按到达顺序排队，天然公平；`tryAcquire(timeout, unit)` 是带超时版本，限时内拿到令牌返回 true，超时返回 false——生产接口的默认选择，它把"等待成本"控制在上层给定的预算内（超时预算与[08 篇](08-隔离与舱壁：线程池与信号量.md)的超时金字塔一致）。一个常见的用法误区是拿 `Semaphore` 当限流器用：Semaphore 是并发数控制（最多 N 个同时执行），RateLimiter 是速率控制（每秒 N 个），语义完全不同——并发数 100 的 Semaphore 在 1 秒内可以放行任意多个请求，限流必须用速率语义。

## 2. SmoothBursty 与 SmoothWarmingUp

RateLimiter.create(permitsPerSecond) 默认是 **SmoothBursty**：匀速补充 + 桶容量内全量突发——闲时攒 5 个令牌，瞬间 5 个请求可同时通过，之后回到匀速。适合 API 网关这类容忍短促突发的场景。

`RateLimiter.create(permitsPerSecond, warmupPeriod)` 创建 **SmoothWarmingUp** 冷启动模式：令牌生成速率从冷启动速率（约为目标值的 1/3）平滑爬升到目标值，桶的冷却因子为 3。它的用途正是 [02 篇](02-扩展模式与容量边界.md) 讲的场景——服务冷启动、JIT 未编译、缓存未预热时，放行速率渐进增长，避免开闸即打满。两个模式的选择与业务"是否容忍瞬时突发"直接对应，**预热模式不是降速而是保护**。

实现层面的差别值得深挖：SmoothWarmingUp 的桶里存的不只是令牌数，还带"冷却因子"——冷启动期（warmupPeriod 内）令牌生成速率按曲线爬升，桶容量（storedPermits）也随冷却程度变化。它与 SmoothBursty 的算法骨架相同（都是时间差惰性补充），只是补充速率从恒定变成随冷却度变化。理解这一点，面试手写"预热限流"就能从"3x 冷却因子"讲到"曲线爬升"，比背结论深一层。

## 3. 手写令牌桶：一个可以写进简历的实现

面试手写限流，核心是"惰性补充 + 原子性"，一个简化的 Java 版本：

```java
public class TokenBucket {
    private final long capacity;      // 桶容量
    private final double refillPerSec;// 每秒补充速率
    private double tokens;            // 当前令牌
    private long lastRefillNanos;     // 上次补充时间

    public synchronized boolean tryAcquire(int n) {
        refill();                     // 按时间差惰性补充
        if (tokens >= n) { tokens -= n; return true; }
        return false;
    }

    private void refill() {
        long now = System.nanoTime();
        double delta = (now - lastRefillNanos) / 1e9 * refillPerSec;
        tokens = Math.min(capacity, tokens + delta);
        lastRefillNanos = now;
    }
}
```

两个要点：**补充必须用时间差计算而非定时任务**（定时器有精度问题且浪费线程）；**并发场景必须加锁或 CAS**（扣减非原子会超发）。生产级实现可以直接用 Guava，手写版本的价值在于面试展示对"惰性补充"模型的理解。

进阶考察点：上面的 `synchronized` 在极端高并发下有锁竞争，生产级单机限流会改成 **CAS 无锁版**（用 `AtomicLong` 存令牌，`compareAndSet` 循环重试）——令牌桶的状态只有"令牌数 + 时间戳"两个 long，天然适合 CAS；Guava 内部正是无锁实现。另一个考点是**多线程补充的原子性问题**：两个线程同时 refill 会把令牌补重，所以补充与扣减必须在同一个原子临界区完成，这是手写版最容易写错的地方。

## 4. Sentinel 单机滑动窗口采样

Sentinel 单机限流用的是[滑动窗口计数](03-限流算法四选一.md)思路：一个窗口（默认 1s）切多个采样格子（默认 2 格），每个格子一个原子计数器，格子滑动覆盖时旧计数清零。统计当前 QPS = 当前格计数 + 前一格计数按流逝时间折算，等价于滑动窗口计数的加权公式。它在高并发下的核心设计是**数组分槽 + AtomicLong 原子更新**，无锁、无 GC 压力，单机百万级 QPS 统计开销可忽略。

Sentinel 的 QPS 限流会按**拒绝策略**处理超出请求：直接拒绝、预热限流、排队等待（匀速排队模式 rate limiter 控制排队间隔，适合削峰填谷）。与 Guava 不同，Sentinel 的计数器在规则里，天然支持多维度（QPS、并发线程数、热点参数），单机模式即可用，集群模式（[05 篇](05-分布式限流：Redis与多级限流.md)）把计数下沉到 Token Server。

## 5. 单机限流的边界

单机限流最大的问题一句话：**多实例各计各的数**。3 个节点各限 100 QPS，全局瞬间可通过 300 QPS——负载均衡不均（一台 80%、两台 10%）时更糟：热点实例先被限，能力却闲置在其他实例上。

所以单机限流的适用场景是：**客户端侧防本地滥用、无状态工具类、以及作为分布式限流的本地兜底层**（[05 篇](05-分布式限流：Redis与多级限流.md) 的双层限流把单机哨兵放第一层，挡掉大部分本地流量，Redis 层只处理跨实例协调）。服务端生产限流必须走分布式方案，单机只是分布式的前奏。

> 🎯 **核心要点**：Guava RateLimiter 用"storedPermits + nextFreeTicketMicros"惰性补令牌，无定时器无锁；SmoothBursty 容忍突发，SmoothWarmingUp 冷启动平滑保护；手写令牌桶的关键是时间差补充与原子扣减；Sentinel 用分槽采样实现 O(1) 内存的滑动窗口统计；单机限流各实例独立计数，服务端必须上分布式方案。

---

**下一模块**：[05 分布式限流：Redis 与多级限流](05-分布式限流：Redis与多级限流.md)

**返回总览**：[00-总览](00-总览.md)
