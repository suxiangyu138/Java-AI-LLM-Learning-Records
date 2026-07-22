# Java 高并发必做项目清单 面试问答
> 🎯 基于从基础到企业级实战的高并发项目清单，涵盖面试高频问题与完美解答方案，帮助你在面试中脱颖而出。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理

> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：请说一下 ThreadPoolExecutor 的核心参数，以及如何合理设置线程池大小？

**面试官意图：** 考察对线程池的理解深度，特别是参数含义和实际场景下的配置能力。

**完美解答：**

`ThreadPoolExecutor` 有七大核心参数：

| 参数 | 含义 | 说明 |
|------|------|------|
| `corePoolSize` | 核心线程数 | 即使空闲也保留的线程数 |
| `maximumPoolSize` | 最大线程数 | 队列满后最多能创建的线程数 |
| `keepAliveTime` | 空闲存活时间 | 超过核心线程数的线程空闲后的存活时间 |
| `unit` | 时间单位 | keepAliveTime 的时间单位 |
| `workQueue` | 任务队列 | 核心线程满后，任务存入此队列 |
| `threadFactory` | 线程工厂 | 创建线程的工厂，建议自定义命名 |
| `handler` | 拒绝策略 | 队列和最大线程都满后的处理方式 |

**线程池大小设置：**

这是一个经典面试题。核心原则是区分任务是 **CPU 密集**还是 **IO 密集**。

```java
// CPU 密集型任务：计算密集型，线程数不宜超过 CPU 核心数
int cpuCoreCount = Runtime.getRuntime().availableProcessors();
// 推荐：corePoolSize = CPU核心数 + 1
new ThreadPoolExecutor(cpuCoreCount + 1, cpuCoreCount + 1, 
    60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

// IO 密集型任务：等待 IO 时 CPU 空闲，可以多开线程
// 推荐：corePoolSize = CPU核心数 * 2
new ThreadPoolExecutor(cpuCoreCount * 2, cpuCoreCount * 2,
    60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
```

更精确的估算公式：**线程数 = CPU核心数 * (1 + 等待时间 / 计算时间)**。例如 IO 等待占比 90%、计算占比 10%，则最佳线程数 ≈ 8 * (1 + 9) = 80。

> ⚠️ **切记**：不要用 `Executors.newFixedThreadPool()`，它默认使用无界 `LinkedBlockingQueue`，任务堆积会导致 OOM。也不要 `Executors.newCachedThreadPool()`，它的最大线程数为 `Integer.MAX_VALUE`，在高并发下会创建海量线程。

**延伸追问应对：** 如果问拒绝策略，回答四种：AbortPolicy（抛异常）、CallerRunsPolicy（调用者线程执行）、DiscardPolicy（丢弃）、DiscardOldestPolicy（丢弃最旧任务）。项目中推荐 **CallerRunsPolicy**，因为它不会丢失任务，同时能反向压到调用方，降低请求提交速度。

---

### Q2：请说说 synchronized 的锁升级过程，以及和 ReentrantLock 的对比

**面试官意图：** 考察对 Java 锁机制的底层理解，包括 JDK 6 之后的锁优化。

**完美解答：**

**synchronized 锁升级路径（JDK 6 引入）：**

```
无锁 -> 偏向锁 -> 轻量级锁 -> 重量级锁
```

**偏向锁**：当锁被同一个线程多次获取时，Mark Word 中记录线程 ID，后续该线程进入同步块无需 CAS 操作。适用于只有一个线程访问同步块的场景。

**轻量级锁**：当有第二个线程竞争时，偏向锁撤销，升级为轻量级锁。通过 CAS 在 Mark Word 中记录锁记录指针，如果 CAS 成功则获取锁，失败则自旋。适用于线程交替执行的场景。

**重量级锁**：当自旋超过一定次数或线程数太多时，升级为重量级锁。由操作系统 Mutex 实现，未获取锁的线程会进入阻塞队列。适用于竞争激烈的场景。

**与 ReentrantLock 的对比：**

| 对比维度 | synchronized | ReentrantLock |
|---------|-------------|---------------|
| 实现方式 | JVM 层面（C++ 实现） | JDK 层面（Java 代码实现） |
| 锁类型 | 非公平锁 | 公平/非公平可配置 |
| 锁释放 | 自动释放（退出同步块） | 需手动 unlock，建议在 finally 中释放 |
| 可中断性 | 不支持中断等待 | 支持 lockInterruptibly() |
| 条件等待 | wait/notify，一个条件队列 | 支持多个 Condition |
| 性能 | JDK 6 后已无差距 | 竞争激烈时维护同步队列略重 |

**实际建议：** JDK 6 之后，**优先使用 synchronized**，代码更简洁，不易出错。只有在需要高级功能（可中断、超时、多条件）时才用 ReentrantLock。

```java
// ReentrantLock 高级用法示例
ReentrantLock lock = new ReentrantLock(true); // 公平锁
Condition notFull = lock.newCondition();
Condition notEmpty = lock.newCondition();

lock.lock();
try {
    // 等待条件满足，可响应中断
    while (queue.size() == capacity) {
        notFull.await(1, TimeUnit.SECONDS);
    }
    queue.add(item);
    notEmpty.signal();
} finally {
    lock.unlock();
}
```

---

### Q3：请说说 ConcurrentHashMap 的底层实现原理，它是怎么保证线程安全的？

**面试官意图：** 考察对并发容器的源码理解深度，是区分初中级工程师的重要分水岭。

**完美解答：**

ConcurrentHashMap 是面试中**绕不开的高频题**，不同 JDK 版本的实现差异很大。

**JDK 7 实现：分段锁 (Segment)**

内部维护一个 Segment 数组，每个 Segment 继承 ReentrantLock，包含一个 HashEntry 数组。**不同的 Segment 可以并发访问**，理论上最多支持 Segment 数组长度个线程同时并发写入。

```text
ConcurrentHashMap
    ├── Segment[0]  ->  HashEntry[] (加锁)
    ├── Segment[1]  ->  HashEntry[] (加锁)
    ├── ...
    └── Segment[15] ->  HashEntry[] (加锁)
```

缺点：Segment 数量固定（默认 16），扩容困难，且定位需要两次哈希。

**JDK 8 实现：CAS + synchronized + 红黑树**

JDK 8 放弃了分段锁，改用**数组 + 链表 + 红黑树**结构，并发控制粒度从 Segment 级降级到**单个桶位**。

```java
// 核心源码思路
final V putVal(K key, V value, boolean onlyIfAbsent) {
    // 1. 计算 hash
    // 2. 如果数组为空，先初始化 (CAS 保证并发安全)
    // 3. 如果桶位为空，CAS 直接放入
    // 4. 如果当前在扩容，协助扩容
    // 5. 否则，synchronized(桶位) 锁定后插入或更新
    // 6. 链表长度 > 8 转为红黑树
}
```

关键点：
- **初始化**：通过 `sizeCtl` 变量的 CAS 操作保证只有一个线程执行数组初始化
- **写入空桶**：用 `CAS` 无锁操作，性能极高
- **写入非空桶**：对桶位头节点加 `synchronized` 锁，粒度极细
- **扩容**：支持多线程并发扩容，每个线程负责迁移一部分桶
- **计数**：使用 `CounterCell` 数组 + CAS 进行高并发统计

**延伸追问应对：** 如果问"ConcurrentHashMap 的 size() 是精确的吗？"，回答 JDK 8 中 size() 先不加锁统计，如果两次结果不一致且处于并发扩容中，才加锁统计，返回的是一个近似值而不是精确值。

---

## 2. 项目实战深度问答

> 💡 面试官会深挖你的项目细节，验证你"真的做过"而不是"背过"

### Q4：你项目中用 CompletableFuture 做过什么？怎么编排异步任务的？

**面试官意图：** 考察异步编程的实战能力，特别是任务编排和异常处理的细节。

**完美解答：**

在我们的微服务电商项目中，**商品详情页聚合**是 CompletableFuture 最典型的应用。一个详情页需要从 5 个服务获取数据：商品基本信息、价格库存、评价、推荐商品、促销活动。

```java
public ProductDetailVO getProductDetail(Long productId) {
    CompletableFuture<ProductInfo> productFuture = 
        CompletableFuture.supplyAsync(() -> productService.getInfo(productId), bizExecutor);
    
    CompletableFuture<PriceStockVO> priceFuture = 
        CompletableFuture.supplyAsync(() -> priceService.getPriceAndStock(productId), bizExecutor);
    
    CompletableFuture<List<ReviewVO>> reviewFuture = 
        CompletableFuture.supplyAsync(() -> reviewService.getReviews(productId), bizExecutor);
    
    CompletableFuture<List<ProductVO>> recommendFuture = 
        CompletableFuture.supplyAsync(() -> recommendService.getRecommend(productId), bizExecutor);
    
    CompletableFuture<PromotionVO> promotionFuture = 
        CompletableFuture.supplyAsync(() -> promotionService.getPromotion(productId), bizExecutor);
    
    // 等所有任务完成，聚合结果，设置超时
    CompletableFuture<Void> allFuture = CompletableFuture.allOf(
        productFuture, priceFuture, reviewFuture, recommendFuture, promotionFuture);
    
    try {
        allFuture.get(3, TimeUnit.SECONDS); // 最多等 3 秒
    } catch (TimeoutException e) {
        log.warn("商品详情聚合超时，返回部分数据");
        // 超时后，已完成的任务结果仍然可用
    } catch (Exception e) {
        log.error("商品详情聚合异常", e);
        throw new BusinessException("获取商品详情失败");
    }
    
    // 聚合结果（已完成的任务 get 不会阻塞）
    return ProductDetailVO.builder()
        .productInfo(productFuture.getNow(null))
        .priceStock(priceFuture.getNow(null))
        .reviews(reviewFuture.getNow(List.of()))
        .recommends(recommendFuture.getNow(List.of()))
        .promotion(promotionFuture.getNow(null))
        .build();
}
```

**异常处理的关键点：**
- 使用自定义线程池 `bizExecutor`，不要用默认的 `ForkJoinPool.commonPool()`
- `allOf().get(3, TimeUnit.SECONDS)` 设置超时，防止某个服务挂掉导致请求卡死
- 部分任务超时后使用 `getNow(null)` 获取已完成的结果，实现优雅降级
- 配合 `exceptionally()` 对单个任务设置兜底值

**优化效果**：之前串行调用 5 个接口耗时 500ms+，改成并行后降到 150ms 以内，提效 70%。

---

### Q5：你的分布式锁是怎么实现的？讲一下 Redisson 看门狗机制

**面试官意图：** 考察分布式锁的实现深度，特别是 Redisson 的核心机制理解。

**完美解答：**

**基础实现：Redis SETNX + Lua**

最基础的分布式锁：

```java
// 加锁
String value = UUID.randomUUID().toString();
Boolean success = redisTemplate.opsForValue()
    .setIfAbsent("lock:key", value, 30, TimeUnit.SECONDS);

// 解锁 - 必须用 Lua 保证原子性
String lua = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
redisTemplate.execute(new DefaultRedisScript<>(lua, Long.class), 
    List.of("lock:key"), value);
```

但基础实现有几个痛点：锁超时释放导致并发问题、不可重入、没有自动续期。

**Redisson 的高级实现：**

Redisson 解决了这些问题，核心是**看门狗 (WatchDog) 机制**：

```java
RLock lock = redissonClient.getLock("order:" + orderId);
try {
    // 默认 30 秒过期，WatchDog 每 10 秒自动续期
    lock.lock();
    // 执行业务代码
} finally {
    lock.unlock();
}
```

**看门狗机制原理：**

1. 加锁时设置默认过期时间 30 秒
2. Redisson 启动一个后台定时任务（Netty 的 Timeout 调度），每隔 `leaseTime / 3`（约 10 秒）检查一次锁是否还持有
3. 如果业务还在执行，就自动续期 30 秒
4. 如果业务执行完毕释放了锁，看门狗自动取消
5. 如果持有锁的进程宕机，锁在 30 秒后自动释放，避免死锁

**与基础实现的对比：**

| 特性 | 基础 SETNX | Redisson |
|------|-----------|----------|
| 可重入 | 不支持 | 支持（同一线程可重复加锁） |
| 自动续期 | 不支持 | 看门狗自动续期 |
| 锁释放 | 需自己写 Lua | 自动 + 手动 |
| 公平锁 | 不支持 | 支持 |
| 读写锁 | 不支持 | 支持 |
| 红锁 | 不支持 | 支持（多节点） |

> ⚠️ 使用 Redisson 时特别注意：如果业务代码耗时超过了锁续期次数限制（默认 3 次），锁仍然会释放。建议根据业务耗时调整 `lock(leaseTime, unit)` 的租期参数。

---

### Q6：你怎么用 Redis + Lua 实现分布式限流的？

**面试官意图：** 考察限流算法的落地能力，以及是否在真实场景中应用过。

**完美解答：**

我用令牌桶算法和滑动窗口算法分别在两个项目里实现过限流，其中**令牌桶**应对秒杀场景，**滑动窗口**应对 API 防刷场景。

**滑动窗口限流（Redis + Lua）：**

```lua
-- 滑动窗口限流 Lua 脚本
local key = KEYS[1]            -- 限流 key (如 rate:limit:userId:api)
local windowSize = tonumber(ARGV[1])  -- 时间窗口大小 (ms)
local maxPermits = tonumber(ARGV[2])  -- 窗口内最大请求数
local currentTime = tonumber(ARGV[3]) -- 当前时间戳 (ms)

-- 移除窗口外的请求记录
redis.call('ZREMRANGEBYSCORE', key, 0, currentTime - windowSize)

-- 统计当前窗口内的请求数
local currentCount = redis.call('ZCARD', key)

if currentCount >= maxPermits then
    return 0  -- 限流
end

-- 记录当前请求，设置过期时间
redis.call('ZADD', key, currentTime, currentTime .. ':' .. math.random())
redis.call('EXPIRE', key, windowSize / 1000 + 1)
return 1  -- 放行
```

**Java 调用：**

```java
@Component
public class SlidingWindowRateLimiter {
    
    public boolean tryAcquire(String key, int maxPermits, long windowMs) {
        String luaScript = "..."; // 上面的 Lua 脚本
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(luaScript, Long.class),
            List.of(key),
            String.valueOf(windowMs),
            String.valueOf(maxPermits),
            String.valueOf(System.currentTimeMillis())
        );
        return result != null && result == 1;
    }
}
```

**应用场景**：每秒最多允许 100 次请求，如果超过则返回"请求过于频繁"。在秒杀系统中，网关层和业务层做了两层限流：网关层按 IP 限流（每人每秒 5 次），业务层按用户 ID 限流（每人每秒 1 次秒杀请求）。

---

### Q7：讲一下你实现的秒杀系统的完整架构设计

**面试官意图：** 考察高并发系统设计的综合能力，这是简历中的王牌项目。

**完美解答：**

秒杀系统的核心挑战是**高并发下的库存一致性**。我设计的架构分了 5 层：

```
客户端 -> CDN(页面静态化) -> Nginx(负载+限流) -> 网关层(Sentinel限流) -> 业务层 -> Redis/DB
```

**前端层：**
- 页面静态化，静态资源上 CDN，减少后端压力
- 按钮置灰 + 本地倒计时，防止重复点击
- 随机路径隐藏真实秒杀接口

**网关层：**
- Sentinel 做接口级限流（秒杀接口最大 1000 QPS）
- IP 黑名单过滤恶意请求

**业务层核心流程：**

```java
@RestController
public class SeckillController {
    
    @PostMapping("/seckill/{productId}")
    public Result<String> seckill(@PathVariable Long productId, 
                                   @RequestParam Long userId) {
        // 1. Token 校验防重复提交（幂等）
        // 2. Redis Lua 预扣库存
        // 3. 库存充足 -> 发送 MQ 异步下单
        // 4. 立即返回"排队中"
        // 5. 用户通过轮询接口查询秒杀结果
    }
}
```

**库存扣减（最核心的逻辑）：**

```
Redis Lua 扣库存 -> 成功 -> 发送 MQ 消息 -> 消费者处理
                                        -> 生成订单
                                        -> 更新数据库库存
                                        -> 记录秒杀结果
```

**压测结果：**

| 指标 | 优化前 | 优化后 |
|------|--------|--------|
| 峰值 QPS | 1200 | 8500 |
| 下单成功率 | 98% | 99.97% |
| 库存一致性 | 有超卖 | 零超卖 |
| TP99 | 800ms | 120ms |

> 💡 秒杀系统最核心的优化思路是：**把写请求压缩到最小，把热点数据留在缓存，把异步做到极致**。最终打到底层数据库的请求量不到原始请求的 1%。

---

## 3. 进阶与系统设计

> 💡 这是拉开差距的环节，考察架构思维

### Q8：分布式幂等设计有哪些方案？你在哪些场景用过？

**面试官意图：** 考察幂等设计的系统化理解，以及在实际业务场景中的应用经验。

**完美解答：**

幂等性设计是高并发系统必备能力。我总结三种核心方案：

**方案一：唯一约束（数据库层削峰）**

最适合：订单号、支付流水号、交易流水号。

```java
@Transactional
public Order createOrder(OrderCreateReq req) {
    // order_no 有唯一索引
    Order order = new Order();
    order.setOrderNo(req.getOrderNo()); // 业务唯一键
    // ... 填充其他字段
    try {
        orderMapper.insert(order);
    } catch (DuplicateKeyException e) {
        // 重复订单，查询已存在的订单返回
        return orderMapper.selectByOrderNo(req.getOrderNo());
    }
    return order;
}
```

**方案二：Token 预生成（防止页面重复提交）**

最适合：表单提交、下单页面。

```java
// 获取 Token
@GetMapping("/token")
public Result<String> getToken() {
    String token = UUID.randomUUID().toString();
    redisTemplate.opsForValue().set("token:" + token, "1", 30, TimeUnit.MINUTES);
    return Result.success(token);
}

// 提交时校验并删除 Token（Lua 保证原子性）
@PostMapping("/submit")
public Result<Void> submit(@RequestHeader("Idempotent-Token") String token) {
    String lua = "if redis.call('get', KEYS[1]) then return redis.call('del', KEYS[1]) else return 0 end";
    Long result = redisTemplate.execute(new DefaultRedisScript<>(lua, Long.class), 
                                        List.of("token:" + token));
    if (result == 0) {
        return Result.error("请勿重复提交");
    }
    // 处理业务...
}
```

**方案三：状态机（业务状态幂等）**

最适合：订单状态流转、审批流程。

```
已支付 -> 已发货 -> 已完成
  ↓(重复回调)
已支付 (状态不变，直接返回成功)
```

```java
@Update("UPDATE order SET status = #{targetStatus} WHERE id = #{orderId} AND status = #{currentStatus}")
int updateStatus(Long orderId, Integer currentStatus, Integer targetStatus);

// 幂等 check：如果已经支付，相同的支付回调不会重复处理
int rows = orderMapper.updateStatus(orderId, OrderStatus.UNPAID, OrderStatus.PAID);
if (rows == 0) {
    // 可能是重复回调，或者状态已变更，返回成功
    return Result.success("订单已处理");
}
```

> 💡 面试时回答这个问题的关键是**区分场景**：没有哪种方案是万能的，要根据不同的业务场景选择最合适的方案，或者组合使用。

---

### Q9：高并发系统设计中，你们是怎么做异步化设计的？

**面试官意图：** 考察对异步解耦、削峰填谷、最终一致性的理解深度。

**完美解答：**

异步化设计是高并发系统的核心思想。我在项目中做了四个层面的异步化：

**1. 消息队列削峰**
秒杀下单流程：接受请求 -> 检查参数 -> 预扣库存 -> **发送 MQ** -> 立即返回，由消费者异步处理真实下单。这样即使瞬间涌入 10 万请求，MQ 也会慢慢消化，不会打垮数据库。

**2. CompletableFuture 并行调用**
如前所述，商品详情聚合、数据导出等场景，把多个独立查询并行执行，提升响应速度。

**3. 异步日志**
高并发下同步日志会拖慢业务线程。使用 Logback 的 AsyncAppender：

```xml
<appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
    <!-- 不丢失日志，默认 256，队列容量 80% 时丢弃 TRACE/DEBUG/INFO -->
    <discardingThreshold>0</discardingThreshold> 
    <queueSize>1024</queueSize>
    <appender-ref ref="FILE"/>
</appender>
```

**4. 异步事件驱动**
使用 Spring 的 `@EventListener` + `@Async` 解耦业务：

```java
// 订单创建事件 - 同步发布，异步处理
@Component
public class OrderEventPublisher {
    @Autowired
    private ApplicationEventPublisher publisher;
    
    public void publishOrderCreated(Order order) {
        publisher.publishEvent(new OrderCreatedEvent(this, order));
    }
}

@Component
public class OrderEventHandlers {
    @Async
    @EventListener
    public void sendSms(OrderCreatedEvent event) {
        // 异步发送短信通知 - 不影响主线程
    }
    
    @Async
    @EventListener
    public void updateStatistics(OrderCreatedEvent event) {
        // 异步更新统计 - 不影响主线程
    }
}
```

**异步化的代价**：最终一致性、增加了系统复杂度、需要处理消息丢失和重复消费。但相比带来的性能提升，这些代价是值得的。

---

## 4. 场景题与故障排查

> 💡 考察实际解决问题的能力

### Q10：生产环境出现死锁，你是怎么排查的？

**面试官意图：** 考察对线程死锁的理解以及实际排查能力。

**完美解答：**

**现象**：部分接口请求卡住不动，没有超时返回。

**排查步骤：**

**第一步：导线程 Dump**
```bash
jstack <pid> > dump.txt
```

**第二步：在 Dump 中查找死锁**
`jstack` 会自动检测死锁，在 dump 文件末尾会有类似这样的信息：

```
Found one Java-level deadlock:
=============================
"Thread-1":
  waiting to lock monitor 0x00007f8c8c006b08 (object 0x000000076b5d6f80, a java.lang.String),
  which is held by "Thread-0"

"Thread-0":
  waiting to lock monitor 0x00007f8c8c006e08 (object 0x000000076b5d6fb8, a java.lang.String),
  which is held by "Thread-1"
```

**第三步：分析死锁原因**
从 dump 中可以看到两个线程各持有一把锁，同时在等待对方的锁。

**模拟死锁的代码：**

```java
public class DeadlockDemo {
    private final Object lockA = new Object();
    private final Object lockB = new Object();
    
    public void method1() {
        synchronized (lockA) {
            Thread.sleep(100); // 确保死锁发生
            synchronized (lockB) {
                // 业务逻辑
            }
        }
    }
    
    public void method2() {
        synchronized (lockB) {
            Thread.sleep(100);
            synchronized (lockA) {  // 与 method1 获取锁的顺序相反
                // 业务逻辑
            }
        }
    }
}
```

**解决方案：**
1. **保证锁的顺序一致**：所有线程以相同的顺序获取锁
2. **使用 `tryLock` 超时机制**：避免长时间等待
3. **使用 `Lock` 替代 `synchronized`**：`ReentrantLock` 的 `tryLock(timeout)` 可以超时放弃

```java
// 使用 tryLock 避免死锁
ReentrantLock lockA = new ReentrantLock();
ReentrantLock lockB = new ReentrantLock();

public boolean safeMethod() {
    if (lockA.tryLock(1, TimeUnit.SECONDS)) {
        try {
            if (lockB.tryLock(1, TimeUnit.SECONDS)) {
                try {
                    // 业务逻辑
                    return true;
                } finally {
                    lockB.unlock();
                }
            }
        } finally {
            lockA.unlock();
        }
    }
    return false; // 获取锁失败，不阻塞
}
```

---

### Q11：你们项目遇到过线程池耗尽的情况吗？怎么解决的？

**面试官意图：** 考察线程池监控和实际调优经验。

**完美解答：**

**问题描述**：线上某个服务突然出现大量接口超时，错误日志显示"Task rejected from executor"。

**排查过程：**

1. 查看监控面板，发现线程池活跃线程数打满到 200（核心线程数），队列大小 1000 也满了
2. 用 `jstack` 查看线程都在执行什么——大部分线程卡在 Redis 的 `BLPOP` 命令上
3. 进一步发现 Redis 主从切换导致连接超时，从节点还未完全同步，大量请求阻塞

**根本原因**：线程池配置不合理——核心线程数太小，队列太大，任务堆积后线程全被 IO 操作阻塞。

**解决方案：**

```java
// 优化后的线程池配置
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    50,                    // 核心线程数（根据压测结果调整）
    100,                   // 最大线程数
    60L, TimeUnit.SECONDS,
    new SynchronousQueue<>(), // 直接提交，不堆积任务
    new ThreadPoolExecutor.CallerRunsPolicy() // 调用者执行
);

// 添加监控
executor.setRejectedExecutionHandler((r, e) -> {
    log.warn("线程池已满，当前活跃: {}, 队列: {}, 任务交由调用者线程执行", 
             e.getActiveCount(), e.getQueue().size());
    // 监控告警
    alertService.sendAlert("线程池告警", String.format("..."));
    // 交给调用者线程
    r.run();
});
```

**预防措施：**
- 线程池添加监控：活跃线程数、队列深度、任务耗时分布
- 设置线程池使用率告警，超过 80% 时通知
- 接口级别设置超时时间，避免 IO 阻塞无限等待
- 根据业务类型分拆线程池：DB 操作、RPC 调用、非关键任务使用不同线程池

---

### Q12：高并发下怎么处理缓存穿透、缓存击穿、缓存雪崩？

**面试官意图：** 考察对缓存三大经典问题的理解和解决方案，这是高并发场景面试必考题。

**完美解答：**

**缓存穿透**：查询一个不存在的数据，缓存和数据库都没有，请求直接打到数据库。

解决方案：**布隆过滤器** + **缓存空值**

```java
public Product getProduct(Long id) {
    // 1. 布隆过滤器拦截（判断 id 是否可能存在）
    if (!bloomFilter.mightContain(id)) {
        return null; // 肯定不存在，直接返回
    }
    
    // 2. 查缓存
    Product product = redisTemplate.opsForValue().get("product:" + id);
    if (product != null) {
        // 判断是否是空值缓存
        if (product.isEmptyMarker()) return null;
        return product;
    }
    
    // 3. 查数据库
    product = productMapper.selectById(id);
    if (product == null) {
        // 缓存空值，过期时间短一些（如 60 秒）
        redisTemplate.opsForValue().set("product:" + id, EMPTY_MARKER, 60, TimeUnit.SECONDS);
        return null;
    }
    redisTemplate.opsForValue().set("product:" + id, product);
    return product;
}
```

**缓存击穿**：**单个热点 key** 过期，大量请求同时打到数据库。

解决方案：**互斥锁** 或 **逻辑过期**

```java
public Product getProduct(Long id) {
    String cacheKey = "product:" + id;
    Product product = redisTemplate.opsForValue().get(cacheKey);
    if (product != null) return product;
    
    // 互斥锁——只让一个线程去查数据库
    String lockKey = "lock:product:" + id;
    if (redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 3, TimeUnit.SECONDS)) {
        try {
            product = productMapper.selectById(id);
            redisTemplate.opsForValue().set(cacheKey, product, 30, TimeUnit.MINUTES);
        } finally {
            redisTemplate.delete(lockKey);
        }
    } else {
        Thread.sleep(50);
        return redisTemplate.opsForValue().get(cacheKey); // 重试
    }
    return product;
}
```

**缓存雪崩**：**大量 key 同时过期**，大量请求打到数据库。

解决方案：**过期时间加随机值** + **多级缓存** + **限流降级**

```java
// 过期时间 = 基础时间 + 随机偏移，避免同时过期
int baseExpire = 30 * 60; // 30 分钟
int randomOffset = new Random().nextInt(300); // 0-5 分钟随机
redisTemplate.opsForValue()
    .set("product:" + id, product, baseExpire + randomOffset, TimeUnit.SECONDS);
```

| 问题 | 根因 | 核心方案 |
|------|------|---------|
| 缓存穿透 | 查询不存在的数据 | 布隆过滤器 + 空值缓存 |
| 缓存击穿 | 热点 key 过期 | 互斥锁 / 逻辑过期 |
| 缓存雪崩 | 大量 key 同时过期 | 随机过期时间 + 多级缓存 |

---

## 💎 面试加分金句

1. "高并发设计没有银弹，核心思路就四个字：**分流、异步、缓存、兜底**。每个层面都做到极致，系统自然扛得住。"
2. "我坚信**压测是检验高并发设计的唯一标准**。没有压测数据支撑的方案，都是纸上谈兵。"
3. "线程池调优不是一个数学公式能解决的，它需要结合 **CPU 密集 vs IO 密集**、**业务容忍延迟**、**系统资源**等多方面因素综合判断。"
4. "ConcurrentHashMap 的源码我读过三遍，每次都有新收获。JDK 8 的 CAS + synchronized 替换 JDK 7 的分段锁，体现了**无锁竞争优先于粗粒度锁**的设计思想。"
5. "Redis 分布式锁不是万能的，当锁竞争非常激烈时，分布式锁本身会成为性能瓶颈。这时候需要考虑**乐观锁**或者**Lua 脚本原子操作**来替代。"

---

## 📋 高频追问清单

| 追问方向 | 应对策略 |
|----------|----------|
| "你压测过你的系统吗？QPS 多少？" | 准备好 JMeter 压测数据，说清楚服务器配置、压测参数、QPS/RT/错误率 |
| "ThreadLocal 有什么问题？" | 内存泄漏（WeakReference 但 value 强引用），使用后一定要 remove |
| "你们用的 Redis 是单机还是集群？" | 如果是单机，回答哨兵模式做高可用，或 Cluster 做分片 |
| "你对异步和同步怎么取舍？" | 核心链路同步保证一致性，非核心链路异步提高性能，需要具体场景具体分析 |
| "MQ 消息丢失怎么处理？" | 生产端 confirm 机制、消费端手动 ack、消息持久化、补偿任务 |

---

## 🔗 关联知识点

- [JVM必做项目清单-面试问答](JVM必做项目清单-面试问答.md) — JVM 并发调优与锁升级
- [SpringBoot必做项目清单-面试问答](SpringBoot必做项目清单-面试问答.md) — 高并发秒杀与消息队列
- [Java必做项目清单-面试问答](Java必做项目清单-面试问答.md) — 分布式锁与缓存实战
