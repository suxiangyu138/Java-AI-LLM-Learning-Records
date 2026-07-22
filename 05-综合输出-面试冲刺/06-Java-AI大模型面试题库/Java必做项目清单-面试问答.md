# Java 必做项目清单 面试问答
> 🎯 基于从入门到高阶架构的项目实战清单，涵盖面试高频问题与完美解答方案，帮助你在面试中脱颖而出。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理

> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：请说一下面向对象编程的三大特性，并结合项目中的实际应用举例

**面试官意图：** 考察 OOP 基础理解深度，以及能否将理论应用到实际项目中，而不是背概念。

**完美解答：**

面向对象三大特性是封装、继承和多态。

**封装**：将对象的属性和行为包装在类内部，通过访问修饰符控制外部访问权限。在图书管理系统中，我将 `Book` 类的属性设为 `private`，只暴露 `getter/setter` 和业务方法。这样做的好处是可以在 `setPrice()` 方法中加入价格校验逻辑，防止价格被设为负数，保证了数据的完整性。

**继承**：子类复用父类属性和方法，并可以扩展自己的特有行为。在 ATM 银行系统中，我设计了 `Account` 基类包含余额、密码等通用属性，`VIPAccount` 继承它并扩展了透支额度、专属理财等功能。继承减少了重复代码，也让类层次关系更加清晰。

**多态**：同一接口在不同实现下表现出不同的行为。在成绩管理系统中，我定义了一个 `Sorter` 接口，`ScoreSorter` 实现按分数排序，`NameSorter` 实现按姓名排序。调用时只需要传入 `Sorter` 接口引用，具体排序行为由运行时对象决定。这就是"父类引用指向子类对象"的多态体现。

**延伸追问应对：** 如果面试官追问"继承和组合怎么选"，你要回答"优先使用组合而非继承"。组合更加灵活，不会破坏封装性，运行时可以动态改变行为。例如在贪吃蛇项目中，蛇的移动行为通过组合 `DirectionController` 来实现，而不是继承某个移动基类。

---

### Q2：请说说 ArrayList 和 LinkedList 的区别，你项目中是怎么选择的？

**面试官意图：** 考察集合底层数据结构理解，以及是否在实际开发中有意识地选择合适的数据结构。

**完美解答：**

两者的核心区别在于底层数据结构不同。`ArrayList` 基于动态数组，`LinkedList` 基于双向链表。

| 对比维度 | ArrayList | LinkedList |
|---------|-----------|------------|
| 底层结构 | 动态数组 | 双向链表 |
| 随机访问 (get) | O(1) | O(n) |
| 尾部插入 | O(1) 均摊 | O(1) |
| 指定位置插入/删除 | O(n)，需要移动元素 | O(n)，但只需修改指针 |
| 内存占用 | 连续内存，只需存数据 | 需额外存储前后指针 |
| 批量删除 | 效率较低 | 迭代器删除效率高 |

在图书管理系统中，我使用 `ArrayList` 存储图书信息，因为核心场景是遍历展示图书列表和按 ID 快速查找，这些场景下 `ArrayList` 性能更好。而在留言板的评论列表中，我用了 `LinkedList`，因为用户会在列表中间插入新的回复，`LinkedList` 在插入操作上更优。

> 💡 实际开发中，90% 的场景用 `ArrayList` 就够了。只有在头尾插入删除频繁时才考虑 `LinkedList`。

**延伸追问应对：** 如果问扩容机制，回答 `ArrayList` 默认容量 10，扩容时按 1.5 倍增长（`oldCapacity + (oldCapacity >> 1)`），通过 `Arrays.copyOf()` 将原数组复制到新数组，频繁扩容会影响性能，所以如果能预估数据量，建议指定初始容量。

---

### Q3：Java 异常体系你是怎么理解和运用的？

**面试官意图：** 考察异常处理是否规范，有没有在实际项目中处理过异常场景。

**完美解答：**

Java 异常体系分为两大类：`Throwable` 下的 `Error` 和 `Exception`。`Error` 是 JVM 层面的严重问题（如 `OutOfMemoryError`），程序无法处理；`Exception` 又分为受检异常（`IOException`、`SQLException`）和运行时异常（`NullPointerException`、`IllegalArgumentException`）。

在我的 ATM 银行系统项目中，我设计了一套异常处理机制：

```java
// 自定义业务异常
public class InsufficientBalanceException extends RuntimeException {
    private double currentBalance;
    private double requiredAmount;
    
    public InsufficientBalanceException(double currentBalance, double requiredAmount) {
        super(String.format("余额不足：当前余额 %.2f，需要 %.2f", currentBalance, requiredAmount));
        this.currentBalance = currentBalance;
        this.requiredAmount = requiredAmount;
    }
}

// 业务层使用
public void withdraw(double amount) {
    if (amount > this.balance) {
        throw new InsufficientBalanceException(this.balance, amount);
    }
    // 正常扣款逻辑
}
```

规范做法是：**异常一定要有业务含义**，不要直接抛出 `Exception` 或 `RuntimeException`；**捕获异常要具体**，不要 `catch(Exception e)` 一把抓；**资源用完要关闭**，使用 try-with-resources 语法。

> 💡 在实际项目中，我更推荐使用运行时异常而不是受检异常，因为受检异常会导致方法签名污染，而且大部分异常无法在编译期被妥善处理。

---

## 2. 项目实战深度问答

> 💡 面试官会深挖你的项目细节，验证你"真的做过"而不是"背过"

### Q4：你做的图书管理系统讲讲它的架构设计？遇到过什么坑？

**面试官意图：** 考察最基础的项目是否理解了分层思想和设计原则，以及是否有真实的踩坑经历。

**完美解答：**

这是一个控制台版图书管理系统，做了三层架构设计：

**数据层**：使用 `ArrayList` 存储图书对象，通过 IO 流将数据持久化到文本文件。每本图书的字段用特殊分隔符拼接存入 `books.txt`，启动时加载到内存。

**业务层**：`BookService` 封装了 CRUD 的核心逻辑，包括参数校验（ISBN 格式、价格非负等）、业务规则（同一 ISBN 不能重复添加）、搜索功能（按书名模糊匹配）。

**表示层**：`ConsoleUI` 处理菜单展示和用户输入，通过 `Scanner` 读取命令并调用业务层方法。

遇到过最大的坑是**线程安全问题**：虽然没有用多线程，但在文件 IO 频繁操作时，如果程序异常退出，会导致 `books.txt` 文件损坏，数据全丢。解决方案是引入**备份机制**：每次操作前先备份原文件，操作成功后删除备份，启动时如果发现主文件损坏则从备份恢复。

此外还遇到**集合遍历时删除元素的坑**，在用 `for-each` 循环删除图书时抛出了 `ConcurrentModificationException`，后改为使用 `Iterator.remove()` 解决。

```java
// 错误写法
for (Book book : bookList) {
    if (book.getId().equals(targetId)) {
        bookList.remove(book); // 抛出 ConcurrentModificationException
    }
}

// 正确写法
Iterator<Book> iterator = bookList.iterator();
while (iterator.hasNext()) {
    Book book = iterator.next();
    if (book.getId().equals(targetId)) {
        iterator.remove(); // 使用迭代器的 remove 方法
    }
}
```

---

### Q5：在 SSM 校园二手商城中，你是怎么做事务控制的？遇到过事务失效的情况吗？

**面试官意图：** 考察是否真正用过声明式事务，以及有没有遇到过事务失效的经典场景。

**完美解答：**

在 SSM 项目中，我使用 `@Transactional` 注解进行声明式事务控制。核心场景是商品发布和订单创建流程。

订单创建涉及多张表的操作：扣减库存、生成订单、扣款记录，任何一个步骤失败都需要回滚：

```java
@Service
@Transactional(rollbackFor = Exception.class)
public class OrderService {
    
    public Order createOrder(Long userId, Long productId, Integer quantity) {
        // 1. 扣减库存
        productMapper.deductStock(productId, quantity);
        // 2. 生成订单
        Order order = new Order(userId, productId, quantity, totalPrice);
        orderMapper.insert(order);
        // 3. 记录支付信息
        paymentMapper.initPayment(order.getId(), totalPrice);
        return order;
    }
}
```

事务失效我遇到过好几种情况：

**情况一：`@Transactional` 加在了 `private` 方法上**。Spring 的声明式事务基于 AOP 动态代理实现，`private` 方法不会生成代理拦截，事务不生效。解决方案是将方法改为 `public`。

**情况二：同类方法内部调用**。`saveA()` 调用 `saveB()`，如果 `saveA()` 上有事务而 `saveB()` 没有，实际走的是 `this.saveB()`，代理不生效。解决方案是注入自身的代理，或者把事务边界提升到外部调用方法。

**情况三：`rollbackFor` 没配置**。默认情况下 `@Transactional` 只对 `RuntimeException` 回滚，遇到受检异常不会回滚，必须显式指定 `rollbackFor = Exception.class`。

---

### Q6：你做的 Redis 秒杀系统怎么防止超卖的？说下技术方案

**面试官意图：** 考察高并发场景下的核心问题意识，以及 Redis 分布式锁、Lua 脚本等实操能力。

**完美解答：**

秒杀防止超卖是核心难点，我用了三层防护方案：

**第一层：Redis 缓存预热 + Lua 脚本扣库存**

在秒杀开始前，将商品库存预加载到 Redis。扣库存操作使用 Lua 脚本保证原子性：

```lua
-- 秒杀扣库存 Lua 脚本
local key = KEYS[1]           -- 库存 key
local userId = ARGV[1]        -- 用户 ID
local buyNum = tonumber(ARGV[2]) -- 购买数量
local stock = redis.call('get', key)

if not stock then
    return -1  -- 商品不存在
end

if tonumber(stock) < buyNum then
    return 0   -- 库存不足
end

-- 扣减库存
redis.call('decrby', key, buyNum)
return 1  -- 成功
```

**第二层：分布式锁保证防重入**

每个用户每件商品限制只能抢一次，使用 Redis 分布式锁 + 用户 ID 去重：

```java
String lockKey = "seckill:lock:" + productId + ":" + userId;
Boolean locked = redisTemplate.opsForValue()
    .setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
if (!locked) {
    throw new BusinessException("您已经参与过秒杀，请勿重复提交");
}
```

**第三层：异步削峰**

通过 RabbitMQ 将秒杀请求转为异步处理，直接返回"排队中"，由消费者线程池处理下单逻辑，避免大量请求打垮数据库。

最终这套方案在压测环境下支撑了 8000+ QPS，库存和订单完全一致，零超卖。

---

### Q7：你做的 RAG 知识库问答系统的核心流程是什么样的？

**面试官意图：** 考察 Java + AI 的融合能力，这是拉开差距的差异化技能。

**完美解答：**

RAG（检索增强生成）的核心思想是"先检索、后生成"，解决大模型知识过时和幻觉问题。我的系统流程如下：

**第一步：文档处理**
上传的 PDF/MD 文档先被解析成纯文本，然后按固定大小（如 512 tokens）+ 重叠窗口（128 tokens）进行分块，每个块保留原始章节标题作为元数据。

**第二步：向量化存储**
将每个文本块调用 Ollama 的 embedding 接口生成向量，存入 Milvus 向量数据库，同时保留原始文本内容在 MySQL 中以备展示。

**第三步：检索增强**
用户提问时，先将问题向量化，从 Milvus 中召回 top-K 最相似的文本块。将召回结果和原始问题拼接成增强 Prompt：

```java
public String buildRagPrompt(String userQuestion, List<Document> relatedDocs) {
    StringBuilder context = new StringBuilder();
    for (Document doc : relatedDocs) {
        context.append(doc.getContent()).append("\n---\n");
    }
    return String.format("基于以下参考资料:\n%s\n\n请回答: %s", context, userQuestion);
}
```

**第四步：生成回答**
将增强后的 Prompt 发给大模型，开启流式输出（SSE），用户体验更流畅。

项目的难点在于：**文档解析质量直接影响检索效果**、**分块大小需要根据业务场景调优**、**embedding 模型的选择对相似度召回率影响巨大**。

---

### Q8：你在 SpringCloud 微服务电商项目中，怎么做服务间调用和异常处理的？

**面试官意图：** 考察微服务架构的实战经验，包括服务发现、远程调用、熔断降级等。

**完美解答：**

我们使用了 Nacos + OpenFeign + Sentinel 这套 SpringCloud Alibaba 技术栈。

**服务注册与发现**：所有服务通过 Nacos 注册，订单服务需要调用库存服务时，通过 Feign 声明式接口调用：

```java
@FeignClient(name = "inventory-service", fallback = InventoryFallback.class)
public interface InventoryClient {
    
    @PostMapping("/inventory/deduct")
    Result<Void> deduct(@RequestBody DeductRequest request);
}
```

**熔断降级**：使用 Sentinel 配置熔断规则，当库存服务接口的慢调用比例超过阈值时触发熔断，直接返回兜底数据：

```java
@Component
public class InventoryFallback implements InventoryClient {
    @Override
    public Result<Void> deduct(DeductRequest request) {
        // 降级处理：异步记录请求到 MQ，等待恢复后重试
        mqTemplate.send("order.delay", request);
        return Result.error("系统繁忙，请稍后重试");
    }
}
```

**全链路追踪**：通过 MDC 实现请求 TraceId 透传，在 Feign 拦截器中传递，方便排查分布式问题。

```java
@Bean
public RequestInterceptor traceInterceptor() {
    return request -> {
        String traceId = MDC.get("traceId");
        if (StringUtils.hasText(traceId)) {
            request.header("X-Trace-Id", traceId);
        }
    };
}
```

这套方案让我们在线上支撑住了双 11 大促的流量冲击，单个服务宕机不会拖垮整个系统。

---

## 3. 进阶与系统设计

> 💡 这是拉开差距的环节，考察架构思维

### Q9：设计一个高并发直播弹幕系统，你会怎么做架构设计？

**面试官意图：** 考察对高并发实时通信系统的理解，包括协议选型、消息分发、限流等。

**完美解答：**

直播弹幕系统的核心挑战是：**高并发写入、低延迟推送、消息不丢失**。

**架构设计：**

**1. 通信协议选型——WebSocket**
采用 Netty 实现 WebSocket 服务器，相比轮询和 SSE，WebSocket 全双工通信延迟最低，适合弹幕这种高频双向交互场景。

**2. 消息分发——异步广播**
弹幕发送流程：客户端 -> WebSocket -> MQ -> 广播给同房间所有连接。使用 MQ 的目的是削峰填谷，防止瞬时弹幕风暴压垮推送线程：

```java
public void onMessage(ChannelHandlerContext ctx, String message) {
    // 1. 限流检查
    if (!rateLimiter.tryAcquire(userId)) {
        ctx.writeAndFlush(new TextWebSocketFrame("发送太频繁，请稍后"));
        return;
    }
    // 2. 发往 MQ 异步处理
    mqTemplate.send("danmaku.topic", message);
}

@RabbitListener(queues = "danmaku.queue")
public void handleDanmaku(String message) {
    // 解析消息，获取房间号
    Danmaku danmaku = JSON.parseObject(message, Danmaku.class);
    // 广播给房间内所有 WebSocket 连接
    ChannelGroup group = channelManager.getGroup(danmaku.getRoomId());
    group.writeAndFlush(new TextWebSocketFrame(message));
}
```

**3. 连接管理——ChannelGroup**
每个直播间维护一个 `ChannelGroup`，保存该房间所有 WebSocket 连接，方便批量推送。用户断开时自动移除。

**4. 限流策略**
每个用户每秒最多发 5 条弹幕，使用令牌桶算法；全房间总弹幕数做二级限流，防止恶意刷屏。

**5. 消息持久化与回放**
弹幕异步写入数据库，支持回放功能。为提升写入性能，采用批量写入策略，每 500ms 或累积 100 条 flush 一次。

---

### Q10：如果让你设计一个多级缓存商品详情页系统，你会怎么做？

**面试官意图：** 考察缓存分层架构设计能力，以及缓存一致性问题的处理经验。

**完美解答：**

多级缓存的目标是**逐层过滤请求，最大化降低对数据库的压力**。我设计了三级缓存结构：

**一级缓存：Caffeine 本地缓存**
每个服务实例在 JVM 内存中缓存热点商品信息。使用 Caffeine 是因为它性能极高（读延迟微秒级），支持基于时间和容量的自动淘汰。

```java
@Bean
public Cache<Long, Product> localCache() {
    return Caffeine.newBuilder()
        .maximumSize(10000)          // 最多缓存 1 万条
        .expireAfterWrite(5, TimeUnit.MINUTES) // 写入后 5 分钟过期
        .recordStats()               // 记录命中率
        .build();
}
```

**二级缓存：Redis 分布式缓存**
所有实例共享 Redis 缓存，存放完整的商品详情 JSON。过期时间设为 30 分钟 + 随机偏移，防止缓存雪崩。

**三级缓存：MySQL 数据库**
最后一道防线，扛住最终查询。

**缓存更新策略（保证一致性）：**

采用 **Cache Aside Pattern** 模式——读取时先查本地缓存，未命中查 Redis，再未命中查 DB 并回填两级缓存。更新时采用"先更新 DB，后删除缓存"策略，配合延迟双删：

```java
@Transactional
public void updateProduct(Product product) {
    // 1. 更新数据库
    productMapper.updateById(product);
    // 2. 立即删除 Redis 缓存
    redisTemplate.delete("product:" + product.getId());
    // 3. 延迟 500ms 再次删除，解决并发读请求导致缓存脏数据问题
    scheduler.schedule(() -> {
        redisTemplate.delete("product:" + product.getId());
    }, 500, TimeUnit.MILLISECONDS);
}
```

这个方案在我们压测中 QPS 从单缓存方案的 5000 提升到了 35000+，本地缓存命中率约 60%，Redis 命中率约 35%，真正打到数据库的不到 5%。

---

## 4. 场景题与故障排查

> 💡 考察实际解决问题的能力

### Q11：线上接口突然变慢，你怎么从 Java 后端排查？

**面试官意图：** 考察线上问题排查的思路链和工具使用熟练度，这是大厂面试必考题。

**完美解答：**

这是一个体系化的排查流程，我会按照"外部到内部、现象到根因"的思路进行：

**第一步：确认问题范围**
用 `top` 命令看 CPU 和内存使用率，用 `free -m` 看内存余量，通过 `df -h` 确认磁盘是否写满。有时慢是因为磁盘 IO 满了（日志打太多），不一定是应用问题。

**第二步：查看 JVM 状态**
用 `jps -l` 找到 Java 进程 PID，再用一系列命令诊断：

```bash
# GC 情况 - 重点关注 YGC/FGC 次数和耗时
jstat -gcutil <pid> 1000 5

# 堆内存分布
jmap -heap <pid>

# 线程状态 - 是否有大量 BLOCKED 线程
jstack <pid> > thread_dump.txt
```

**第三步：定位瓶颈**
- **CPU 高**：用 `top -Hp <pid>` 查看最耗 CPU 的线程，将线程 ID 转十六进制后去 `jstack` 输出里找
- **频繁 Full GC**：GC 日志中查看 GC 间隔，用 `jmap -dump:live,format=b,file=heap.hprof` 导 dump，MAT 分析
- **线程池耗尽/死锁**：`jstack` 中搜索 `BLOCKED` 和 `DEADLOCK` 关键字

**第四步：实时分析**
如果问题偶发，我会用 Arthas 附加到 JVM：

```bash
# 查看最耗时的方法
trace com.example.service.OrderService createOrder '#cost > 100'

# 查看方法的调用参数和返回值
watch com.example.service.OrderService createOrder '{params, returnObj}' -x 2
```

**第五步：分析慢 SQL**
开启 MySQL 慢查询日志，配合 `EXPLAIN` 查看 SQL 执行计划，定位全表扫描或索引失效的查询。

> ⚠️ 最常遇到的场景：接口慢不是因为代码逻辑，而是因为 Redis 缓存失效导致大量请求穿透到 DB，DB 连接池打满，线程阻塞。所以排查时一定要先确认缓存是否命中。

---

### Q12：你的系统上线后出现了频繁 Full GC，怎么排查和解决？

**面试官意图：** 考察 JVM 调优和内存泄漏排查的真实经验。

**完美解答：**

**复现与确认：**
首先通过 `jstat -gcutil <pid> 2000 10` 观察 GC 频率。如果 Full GC 间隔从正常的小时级别缩短到分钟甚至秒级，说明问题严重。同时查看 GC 日志确认老年代是否一直在增长。

**排查根因（三大常见原因）：**

**原因一：堆内存太小**
如果老年代在 Full GC 后使用率仍然很高（80%+），说明业务本身需要更大的堆。解决方案：调整 `-Xms` 和 `-Xmx`，比如从 2G 调大到 4G。

**原因二：大对象直接进入老年代**
检查是否有一次性加载大量数据的操作，比如导出报表时把所有数据加载到内存。用 MAT 分析 heap dump 查看大对象。解决方案：分批查询、流式处理。

**原因三：内存泄漏**
堆使用率在 Full GC 后持续走高，这是典型的内存泄漏特征。我通过 MAT 找到泄漏的引用链：

```bash
jmap -dump:live,format=b,file=/tmp/heap.hprof <pid>
```

在 MAT 中看 `Histogram` -> `Dominator Tree`，定位占用内存最大的对象。有一次我发现 `HashMap` 里不断添加用户查询记录但从未清理，导致 OOM。修复后加上定时清理逻辑，用 `WeakHashMap` 替代普通 `HashMap`。

**优化措施与效果：**

| 优化项 | 优化前 | 优化后 |
|--------|--------|--------|
| 堆大小 | 2G | 4G |
| Full GC 间隔 | 3 分钟 | 45 分钟 |
| GC 总停顿 | 500ms/次 | 120ms/次 |
| 系统吞吐量 | 1200 QPS | 3500 QPS |

优化后系统稳定运行，Full GC 回归正常频率。

---

### Q13：在高并发下，你怎么保证接口的幂等性？讲一下具体方案

**面试官意图：** 考察对幂等设计的理解以及在实际项目中的实现经验。

**完美解答：**

幂等是指**同一个请求无论执行多少次，结果都保持一致**。高并发下重复提交（用户双击、网络重试、MQ 重复消费）都会导致非幂等问题。

我做过三种方案，分别适用于不同场景：

**方案一：Token 机制（适用于表单提交）**
前端请求时先获取一个唯一 Token，后端校验 Token 是否存在，存在则正常处理并删除 Token，不存在则说明是重复提交。

```java
// 获取 Token
@GetMapping("/token")
public Result<String> getToken() {
    String token = UUID.randomUUID().toString();
    redisTemplate.opsForValue().set("token:" + token, "1", 30, TimeUnit.MINUTES);
    return Result.success(token);
}

// 提交时校验
@PostMapping("/submit")
public Result<Void> submit(@RequestParam String token, @RequestBody OrderReq req) {
    // Lua 脚本原子性校验并删除
    String lua = "if redis.call('get', KEYS[1]) then return redis.call('del', KEYS[1]) else return 0 end";
    Long result = redisTemplate.execute(new DefaultRedisScript<>(lua, Long.class), 
                                        List.of("token:" + token));
    if (result == 0) {
        return Result.error("请勿重复提交");
    }
    // 处理业务...
}
```

**方案二：唯一索引/唯一主键（适用于订单号、流水号）**
数据库表对业务唯一键建唯一索引，重复插入会报 DuplicateKeyException，捕获后直接返回成功，不认为是异常。

**方案三：去重表（适用于 MQ 消费）**
消费消息前先查去重表（或 Redis）判断消息 ID 是否已处理过：

```java
@RabbitListener(queues = "order.queue")
public void handleOrder(Message msg) {
    String msgId = msg.getMessageProperties().getMessageId();
    // SETNX 实现幂等
    Boolean success = redisTemplate.opsForValue()
        .setIfAbsent("msg:" + msgId, "done", 1, TimeUnit.DAYS);
    if (Boolean.FALSE.equals(success)) {
        log.info("消息已处理，跳过：{}", msgId);
        return;
    }
    // 处理业务逻辑...
}
```

> 💡 在秒杀系统中我将三种方案组合使用：Token 防止页面重复提交，唯一订单号防止重复下单，去重表保证 MQ 消费幂等。

---

## 💎 面试加分金句

1. "我做项目的原则是：**先跑通，再优化，最后抽象成可复用的方案**。比如图书管理的 IO 备份机制后来被我抽象成了工具类，用在多个项目中。"
2. "秒杀系统的核心不在于代码多复杂，而在于**对每一层压力的精确把控**：前端限流 -> 网关限流 -> 业务层预热 + 分布式锁 -> MQ 削峰 -> 最终 DB 事务保证。"
3. "我觉得 RAG 项目最大的价值是证明了 **Java 后端也可以做 AI 应用**，技术栈从 SpringBoot 到 Milvus 到 Ollama 全部是成熟的 Java 生态组件，落地成本远低于 Python。"
4. "选型时我坚持一个原则：**先选择最熟悉的技术解决 80% 的问题**，而不是为了炫技引入新框架。比如缓存先用 Redis 就够了，当达到瓶颈才考虑引入 Caffeine 做多级缓存。"
5. "线上问题排查我总结了一套 SOP：**看监控 -> 定范围 -> 查日志 -> 导堆栈 -> 定位代码 -> 修复验证**，每个步骤都有对应工具和方法。"

---

## 📋 高频追问清单

| 追问方向 | 应对策略 |
|----------|----------|
| "你这个项目并发量多少？怎么压测的？" | 用 JMeter 做压测，关注 QPS、RT、TP99、错误率，压测前先估算服务器吞吐上限 |
| "项目上线后遇到过什么线上故障？" | 准备好 1-2 个真实案例，按"发现 -> 排查 -> 定位 -> 修复 -> 复盘"结构讲 |
| "为什么用这个技术选型？" | 从团队技术栈、社区活跃度、业务匹配度三个角度回答，不要只说"大家都用" |
| "如果让你重新做这个项目，你会改进什么？" | 展现复盘思维：架构上可以怎么优化、测试怎么补全、文档怎么完善 |
| "你这个项目的性能瓶颈在哪里？" | 提前做好压测，知道系统的极限在哪里，瓶颈是 CPU/IO/网络/DB 哪一层 |

---

## 🔗 关联知识点

- [JVM必做项目清单-面试问答](JVM必做项目清单-面试问答.md) — JVM 调优与故障排查深度问答
- [Java高并发必做项目清单-面试问答](Java高并发必做项目清单-面试问答.md) — 高并发与线程安全深度问答
- [SpringBoot必做项目清单-面试问答](SpringBoot必做项目清单-面试问答.md) — SpringBoot 实战问答
