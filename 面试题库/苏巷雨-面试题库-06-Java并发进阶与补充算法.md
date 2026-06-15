# 苏巷雨 — 面试题库 06：Java 进阶难点 + JDK 新特性 + Python + 补充算法

> 考察技术深度和广度，区分普通和优秀的候选人的关键

---

## 一、Java 并发进阶

### 1. AQS 原理？ReentrantLock 如何基于 AQS 实现？

**AQS（AbstractQueuedSynchronizer）**：JUC 并发工具的基础框架。

**核心三要素**：
- **state**（volatile int）：同步状态。0=未锁定，1=已锁定，>1=重入次数
- **CLH 队列**：FIFO 双向链表，存放等待获取锁的线程
- **CAS**：修改 state 和队列操作的原子保证

**ReentrantLock 基于 AQS 的实现**：

```java
// 加锁流程
lock() {
    // 1. CAS 尝试将 state 从 0 改为 1
    if (compareAndSetState(0, 1)) {
        setExclusiveOwnerThread(currentThread);  // 成功！获取锁
        return;
    }
    // 2. CAS 失败 → 进入 acquire()
    acquire(1);
}

acquire(int arg) {
    // 3. tryAcquire() → 判断是否可以获取（包括重入判断）
    if (!tryAcquire(arg)) {
        // 4. 不能获取 → 入队（CLH 队列尾部）
        // 5. 入队后自旋/阻塞，等前驱唤醒
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg);
    }
}

// tryAcquire：判断是否可以获取锁
//   state == 0 → CAS 尝试获取
//   state > 0 && owner == currentThread → 重入，state += arg
//   否则 → 失败

// 解锁流程
unlock() {
    tryRelease(1);
}

tryRelease(int arg) {
    int newState = state - arg;
    if (owner != currentThread) throw IllegalMonitorStateException;
    if (newState == 0) {
        setExclusiveOwnerThread(null);  // 完全释放
        return true;                    // → 唤醒 CLH 队列中的后继节点
    }
    setState(newState);  // 重入锁还未完全释放
    return false;
}
```

**FairSync vs NonfairSync**：
- 公平锁：`tryAcquire()` 前先检查 CLH 队列中是否有前驱等待者
- 非公平锁：直接 CAS 抢锁，不管队列（吞吐量更高，但可能饥饿）

📌 **项目实践**：
- **Flavor Dash**：Redis 分布式锁（Redisson 的 RLock）底层基于类似 AQS 的语义实现，通过 Lua 脚本+CAS 保证原子性，WatchDog 机制类比 AQS 的 CLH 队列等待唤醒。
- **SuGuangMall**：秒杀扣减库存使用 CAS 语义（compareAndSet → 重试/失败），与 AQS 的 CAS 修改 state 异曲同工。

### 2. CountDownLatch 和 CyclicBarrier 的区别？

| | CountDownLatch | CyclicBarrier |
|--|---------------|---------------|
| 作用 | 一个线程等 N 个线程完成 | N 个线程互相等待，齐了再一起出发 |
| 计数器 | 不可重置（减到 0 就没了） | 可循环使用（到 barrier 后自动重置） |
| 回调 | 无 | 可传 Runnable，到达后执行 |
| 场景 | 主线程等子任务完成、启动前等待依赖就绪 | 多线程计算 → 等齐再下一阶段 |

```java
// CountDownLatch：火箭发射等各部门检查完毕
CountDownLatch latch = new CountDownLatch(3);
for (int i = 0; i < 3; i++) {
    new Thread(() -> {
        check();  // 部门检查
        latch.countDown();  // 检查完毕！
    }).start();
}
latch.await();  // 等 3 个都 countDown
launch();       // 发射！

// CyclicBarrier：三个选手到齐了出发
CyclicBarrier barrier = new CyclicBarrier(3, () -> System.out.println("全部到齐，出发！"));
for (int i = 0; i < 3; i++) {
    new Thread(() -> {
        prepare();
        barrier.await();  // 等其他人
        run();  // 一起跑
    }).start();
}
```

📌 **项目实践**：
- **LingShu**：`CompletableFuture.allOf(taskA, taskB, taskC)` 等待多模型并行返回后合并排序，语义等价于 CountDownLatch（N 个任务完成后触发主线程继续）。
- **SuGuangMall**：异步编排中多个子任务（库存扣减→订单创建→支付链接生成）并行执行，`allOf().join()` 等待全部完成后统一返回前端。

### 3. Semaphore 的使用场景？

**Semaphore**：信号量，控制同时访问资源的线程数。

```java
// 场景 1：API 限流（最多 10 个并发请求）
Semaphore semaphore = new Semaphore(10);
if (semaphore.tryAcquire(3, TimeUnit.SECONDS)) {
    try {
        doRequest();
    } finally {
        semaphore.release();
    }
} else {
    return "系统繁忙，请稍后重试";
}

// 场景 2：数据库连接池（最多 20 个连接）
class ConnectionPool {
    private Semaphore semaphore = new Semaphore(20);
    
    public Connection getConnection() throws InterruptedException {
        semaphore.acquire();  // 无可用连接时阻塞
        return getFromPool();
    }
    
    public void release(Connection conn) {
        returnToPool(conn);
        semaphore.release();
    }
}
```

**和线程池的区别**：
- 线程池控制**线程数量**（并发执行者数量）
- Semaphore 控制**访问资源数量**（被访问的资源数）

📌 **项目实践**：
- **Flavor Dash**：LLM Client Factory 中使用 Semaphore 控制多模型 provider 的并发调用数（如 GPT-4o 最多 5 路并发，Claude 最多 3 路），`tryAcquire(timeout)` 超时降级返回兜底结果，防止 API 限流被打满。

### 4. ThreadLocal 内存泄漏详解

```
Thread
  └── ThreadLocalMap (每个线程内部一个)
        ├── Entry (key=ThreadLocal的弱引用, value=实际值)
        ├── Entry
        └── Entry ...

GC 时：
  key(弱引用) → 被回收 → 变成 null
  value(强引用) → 不会被回收 ❌
  
  只要 Thread 不销毁（线程池中线程复用），
  key=null 的 Entry 越来越多 → 内存泄漏
```

**ThreadLocal 的防护设计**：
- 每次 `get()/set()/remove()` 时，顺带清理 key=null 的 Entry
- 但仍存在泄漏风险（如果长期不调用 get/set）

**最佳实践**：
```java
// 1. 用完就清理（finally 保证）
ThreadLocal<User> userHolder = new ThreadLocal<>();
try {
    userHolder.set(user);
    doSomething();
} finally {
    userHolder.remove();  // ★ 必须！
}

// 2. 声明为 static final（防止 ThreadLocal 本身被回收）
private static final ThreadLocal<User> userHolder = new ThreadLocal<>();
```

📌 **项目实践**：
- **SuGuangMall**：Dubbo RPC 调用链中通过 ThreadLocal 透传 traceId 和用户上下文（`RpcContext` 底层基于 ThreadLocal），Filter 拦截器在 finally 块中 `remove()` 防止内存泄漏。
- **Flavor Dash**：WebSocket 会话上下文管理，每个连接对应一个 ThreadLocal 存储 Session 元数据，连接断开时在 finally 中清理。

### 5. CompletableFuture 高级用法

```java
// 1. thenApply vs thenCompose
CompletableFuture<User> userFuture = findUser(1L);
// thenApply: 同步转换（类似 map）
CompletableFuture<String> nameFuture = userFuture.thenApply(User::getName);
// thenCompose: 异步组合（类似 flatMap），避免嵌套 CompletableFuture<CompletableFuture<>>
CompletableFuture<Order> orderFuture = userFuture.thenCompose(this::findLatestOrder);

// 2. thenCombine: 并行执行两个任务并合并结果
CompletableFuture<String> weather = getWeather("合肥");
CompletableFuture<String> news = getNews();  // 并行执行！
CompletableFuture<String> report = weather.thenCombine(news, (w, n) ->
    "今日天气：" + w + "\n今日新闻：" + n
);

// 3. allOf + 收集结果
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> "A");
CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> "B");
CompletableFuture<String> f3 = CompletableFuture.supplyAsync(() -> "C");
CompletableFuture.allOf(f1, f2, f3).join();
// 注意：allOf 返回值是 Void，需要单独 get 各个 Future

// 更好的方式：join 后收集
Stream.of(f1, f2, f3)
    .map(CompletableFuture::join)
    .collect(Collectors.toList());

// 4. anyOf: 任意一个完成就返回
CompletableFuture<Object> fastest = CompletableFuture.anyOf(f1, f2, f3);

// 5. 异常处理
CompletableFuture.supplyAsync(() -> riskyOperation())
    .exceptionally(ex -> "降级默认值")  // 只处理异常
    .handle((result, ex) -> {          // 正常+异常都处理
        if (ex != null) return "出错：" + ex.getMessage();
        return "成功：" + result;
    });

// 6. 自定义线程池（默认 ForkJoinPool 不适合 IO 密集型）
Executor ioExecutor = Executors.newFixedThreadPool(10);
CompletableFuture.supplyAsync(() -> callLLMAPI(), ioExecutor);
```

**你项目的应用**：
- **LingShu（灵枢模型网关）**：多模型 Provider 并行调用，`allOf()` 合并所有结果后 RRF 融合排序，`anyOf()` 实现"谁快用谁"的快速响应模式。RAG 多路检索（向量 + BM25 + ES）同样使用 `allOf` 并行。
- **SuGuangMall**：商品详情页异步编排，`CompletableFuture.supplyAsync(() -> skuInfo(), ioExecutor).thenCombineAsync(futurePrice, ...)` 并行查询基础信息、价格、库存、优惠券，`allOf().join()` 汇总后响应。

### 6. Java 中的四种引用类型实战

```java
// 1. 强引用 - 默认
Object obj = new Object();
// 只要 obj 还指向对象，GC 永不回收

// 2. 软引用 - 内存不足时回收（★ 适合做缓存）
SoftReference<byte[]> cache = new SoftReference<>(new byte[10 * 1024 * 1024]);
byte[] data = cache.get();
if (data == null) {
    data = loadFromDB();  // 被回收了，重新加载
    cache = new SoftReference<>(data);
}
// 你项目中可以用于：本地缓存（作为 Caffeine 的补充）

// 3. 弱引用 - GC 时立即回收
WeakReference<Object> weak = new WeakReference<>(new Object());
System.gc();
assert weak.get() == null;  // 被回收了
// ThreadLocal 的 key 就是弱引用
// WeakHashMap：key 被回收后自动删除 Entry

// 4. 虚引用 - 任何时候都可能被回收，配合 ReferenceQueue
ReferenceQueue<Object> queue = new ReferenceQueue<>();
PhantomReference<Object> phantom = new PhantomReference<>(new Object(), queue);
// get() 永远返回 null
// 用途：对象回收时收到通知（NIO DirectByteBuffer 清理堆外内存）
```

📌 **项目实践**：
- **SuGuangMall**：多级缓存体系中，Caffeine（L1）配合 Redis（L2），软引用（`SoftReference`）作为 JVM 堆内缓存的补充兜底——内存充足时缓存命中，内存紧张时 GC 自动回收，防止 OOM。
- **LingShu**：双层会话存储（Caffeine L1 + Redis L2），弱引用（`WeakReference`）用于会话元数据的临时缓存，会话过期后 GC 自然回收。

---

## 二、JDK 8~21 核心新特性

### 7. Lambda 表达式和函数式接口

```java
// Lambda 的本质：函数式接口的匿名实现
// 可以用的地方：接口只有一个抽象方法（@FunctionalInterface）

// 常用函数式接口：
Consumer<String> printer = s -> System.out.println(s);  // 消费：void accept(T)
Supplier<User> factory = () -> new User();              // 生产：T get()
Function<String, Integer> len = s -> s.length();        // 转换：R apply(T)
Predicate<String> isEmpty = s -> s.isEmpty();           // 判断：boolean test(T)
BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b;  // 双参数

// 方法引用（更简写法）：
Consumer<String> printer = System.out::println;         // 实例方法引用
Function<String, Integer> len = String::length;          // 对象方法引用
Supplier<User> factory = User::new;                      // 构造器引用
```

📌 **项目实践**：
- **SuGuangMall**：订单列表处理使用 `orders.stream().filter(...).map(OrderDTO::new).collect(toList())`，Lambda + 方法引用简化集合转换；`groupingBy` 中配合 `Collectors.summingInt` 实现按城市/品类聚合。
- **Flavor Dash**：LLM Client Factory 中使用函数式回调 `Consumer<Response>` 处理流式输出，`Function<Prompt, Response>` 做 provider 适配转换。

### 8. Stream 流的高级操作

```java
// 1. flatMap: 扁平化嵌套集合
List<Order> orders = ...;
List<OrderItem> allItems = orders.stream()
    .flatMap(order -> order.getItems().stream())
    .collect(toList());

// 2. collect + groupingBy
Map<String, List<User>> byCity = users.stream()
    .collect(Collectors.groupingBy(User::getCity));

Map<String, Long> cityCount = users.stream()
    .collect(Collectors.groupingBy(User::getCity, Collectors.counting()));

// 3. partitioningBy（二分分组）
Map<Boolean, List<User>> adultMap = users.stream()
    .collect(Collectors.partitioningBy(u -> u.getAge() >= 18));

// 4. reducing（自定义归约）
int totalAge = users.stream()
    .collect(Collectors.reducing(0, User::getAge, Integer::sum));

// 5. 收集为 Immutable（JDK 10+）
List<String> list = stream.collect(Collectors.toUnmodifiableList());

// 6. takeWhile / dropWhile（JDK 9+）
// takeWhile: 条件满足则取，遇到第一个不满足的停止
Stream.of(1, 2, 3, 4, 0, 1).takeWhile(n -> n > 0) // [1,2,3,4]
// dropWhile: 条件满足则丢弃，遇到第一个不满足的开始取
Stream.of(1, 2, 3, 4, 0, 1).dropWhile(n -> n < 4) // [4,0,1]
```

📌 **项目实践**：
- **SuGuangMall**：`flatMap` 打平订单-订单项一对多关系；`groupingBy` 按城市/品类聚合统计销量；`reducing` 计算金额汇总；`partitioningBy` 区分有效/无效订单——全部通过 Stream 链式处理，代码简洁无副作用。

### 9. JDK 9~21 重大新特性

| 版本 | 特性 | 说明 |
|------|------|------|
| JDK 9 | 模块化系统（Jigsaw） | `module-info.java`，强封装 |
| JDK 9 | 集合工厂方法 | `List.of(a,b,c)`、`Set.of()`、`Map.of(k,v)`，不可变 |
| JDK 10 | var 局部变量推断 | `var list = new ArrayList<String>();` |
| JDK 11 | HttpClient | 标准 HTTP 客户端，支持 HTTP/2 + WebSocket |
| JDK 12 | Switch 表达式（预览） | 箭头语法，无 fall-through |
| JDK 14 | Records（预览） | `record Point(int x, int y) {}`，不可变数据载体 |
| JDK 14 | Pattern Matching instanceof | `if (obj instanceof String s && s.length() > 0)` |
| JDK 15 | Text Blocks | `"""多行字符串"""` |
| JDK 16 | Records 正式 | - |
| JDK 17 | **LTS**，Sealed Classes | `sealed class Shape permits Circle, Square` |
| JDK 17 | Pattern Matching switch（预览） | - |
| JDK 19 | **Virtual Threads**（预览） | 虚拟线程，轻量并发 |
| JDK 21 | **LTS**，Virtual Threads 正式 | Project Loom 核心特性 |
| JDK 21 | Pattern Matching switch 正式 | - |
| JDK 21 | Record Patterns | `if (r instanceof Point(int x, int y))` |
| JDK 21 | Sequenced Collections | `getFirst()/getLast()/addFirst()/addLast()` |

**你面试时最应该重点掌握的**：

```java
// 1. Records（替代写 getter/setter/equals/hashCode 的 POJO）
public record UserDTO(Long id, String name, String email) {}
// 自动生成：构造器、getter（id() 不是 getId()）、equals、hashCode、toString
// 不可变（所有字段 final）

// 2. Pattern Matching
// instanceof
if (obj instanceof String s && s.length() > 0) {
    System.out.println(s.toUpperCase());
}
// switch
String result = switch (obj) {
    case Integer i -> "整数：" + i;
    case String s -> "字符串：" + s;
    case null -> "空值";
    default -> "未知";
};

// 3. Virtual Threads（★ ★ ★ 面试高频）
// 传统线程池（重量级，和 OS 线程 1:1）
ExecutorService pool = Executors.newFixedThreadPool(200);
// 虚拟线程（轻量级，和 OS 线程 M:N 映射）
ExecutorService virtualPool = Executors.newVirtualThreadPerTaskExecutor();
// 优势：可以创建百万虚拟线程，阻塞时自动释放底层 OS 线程
// 适用：IO 密集型任务（如调用 LLM API、数据库查询）

// 4. Text Blocks
String json = """
    {
        "name": "张三",
        "age": 20
    }
    """;

// 5. HttpClient
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://api.example.com"))
    .GET()
    .build();
HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
```

📌 **项目实践**：
- **Virtual Threads**：Flavor Dash/LingShu 的 LLM API 调用为 IO 密集型（发送请求 → 等待响应），适合使用虚拟线程替代传统线程池，`Executors.newVirtualThreadPerTaskExecutor()` 简化并发，无需调优线程池参数。
- **Records**：任意项目的 DTO 简化——`record LLMResponse(String content, int tokens, long latency)` 替代传统 POJO，自动生成构造器/equals/hashCode。
- **Text Blocks**：Flavor Dash 中构建 LLM API 的 JSON 请求体，""" 多行字符串直接嵌入 prompt 模板，无需转义和拼接。

---

## 三、NIO 与 Netty

### 10. BIO / NIO / AIO 的区别和选择？

| | BIO | NIO | AIO |
|--|-----|-----|-----|
| 模型 | 阻塞 IO | 非阻塞 IO（多路复用） | 异步 IO |
| 线程模型 | 一个连接一个线程 | 一个线程处理多个连接 | 回调通知 |
| API | InputStream/OutputStream | Channel + Buffer + Selector | AsynchronousChannel |
| 复杂度 | 低 | 中 | 高 |
| 吞吐量 | 低 | 高 | 高 |
| 适用场景 | 连接少、固定 | 连接多、短连接 | 连接多、长连接 |

**Java NIO 三大核心**：
- **Channel**：双向通道（FileChannel、SocketChannel、ServerSocketChannel）
- **Buffer**：数据容器（ByteBuffer、CharBuffer），position/limit/capacity
- **Selector**：多路复用器，一个线程监听多个 Channel 的事件

```java
// NIO 服务端骨架
Selector selector = Selector.open();
ServerSocketChannel server = ServerSocketChannel.open();
server.bind(new InetSocketAddress(8080));
server.configureBlocking(false);
server.register(selector, SelectionKey.OP_ACCEPT);

while (true) {
    selector.select();  // 阻塞等事件
    Set<SelectionKey> keys = selector.selectedKeys();
    for (SelectionKey key : keys) {
        if (key.isAcceptable()) {
            // 接受新连接
            SocketChannel client = server.accept();
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ);
        } else if (key.isReadable()) {
            // 读取数据
            SocketChannel client = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            client.read(buffer);
            // ... 处理
        }
    }
    keys.clear();
}
```

📌 **项目实践**：
- **Flavor Dash**：Spring Boot 3.4 内嵌 Tomcat NIO 处理 Web 请求（一个线程处理多个连接），WebSocket 基于 NIO 长连接实现实时推送，无需为每个连接分配独立线程。
- **SuGuangMall**：Dubbo RPC 默认基于 Netty（NIO 框架）进行网络通信，一个 Boss 线程 accept 连接，多个 Worker 线程处理读写事件，支撑秒杀场景的高吞吐。

### 11. Netty 相比原生 NIO 的优势？

**你项目中虽然没用 Netty，但面试可能问对比**：

| 原生 NIO | Netty |
|---------|-------|
| 手动处理 epoll bug（空轮询 CPU 100%） | 自动修复 epoll bug |
| 手动管理 ByteBuffer | ByteBuf（读写索引分离、池化、零拷贝） |
| 手动管理线程模型 | 主从 Reactor 线程模型（Boss + Worker） |
| 手动处理粘包/拆包 | 内置编解码器（LineBased、LengthFieldBased） |
| 手动管理连接 | Channel 生命周期 + Pipeline 责任链 |

**为什么你的项目没用 Netty**：Spring Boot 内置 Tomcat/WebFlux 已满足需求，不需要自建通信协议。但了解 Netty 对理解 NIO 底层有帮助。

---

## 四、Python 能力（简历中的技能）

### 12. Python 在项目中具体做了什么？

**场景 1：网络爬虫（灵枢医疗知识库构建）**
```python
import requests
from bs4 import BeautifulSoup
import pandas as pd

# 爬取医疗公开文献/知识
def crawl_medical_articles(base_url, pages=10):
    articles = []
    for page in range(1, pages + 1):
        resp = requests.get(f"{base_url}?page={page}", 
                           headers={"User-Agent": "Mozilla/5.0"})
        soup = BeautifulSoup(resp.text, 'html.parser')
        for item in soup.select('.article-item'):
            articles.append({
                'title': item.select_one('.title').text,
                'content': item.select_one('.content').text,
                'source_url': item.select_one('a')['href']
            })
    return articles

# 数据清洗后存入文件，再通过 Java 服务导入 Milvus
df = pd.DataFrame(articles)
df.to_json('corpus/medical_articles.json', orient='records', force_ascii=False)
```

**场景 2：数据分析**
```python
import matplotlib.pyplot as plt
import seaborn as sns

# 分析用户反馈数据
df = pd.read_csv('feedback.csv')
# 情感分布统计
sns.countplot(data=df, x='sentiment')
# 关键词词云
from wordcloud import WordCloud
WordCloud(font_path='simhei.ttf').generate(' '.join(df['comment']))
```

**面试时怎么说**："Python 主要用于数据采集、清洗、分析，作为 Java 服务的辅助。最终知识库构建和 API 服务都是 Java 完成的。"

### 13. Python 和 Java 对比？

| | Java | Python |
|--|------|--------|
| 类型 | 静态强类型 | 动态强类型 |
| 性能 | 快（JIT 编译） | 慢（解释执行） |
| 并发 | Thread（重量级）+ Virtual Thread（21+） | GIL 限制多线程（需多进程/协程） |
| 生态 | 企业级、大数据、微服务 | AI/ML、数据科学、脚本 |
| 适合 | 大型系统、高并发 | 快速开发、数据分析、AI |
| 互补 | 主力后端 | 辅助工具 |

---

## 五、补充算法题

### 14. 三数之和（排序 + 双指针）

```java
public List<List<Integer>> threeSum(int[] nums) {
    List<List<Integer>> result = new ArrayList<>();
    Arrays.sort(nums);  // 排序是关键
    int n = nums.length;
    
    for (int i = 0; i < n - 2; i++) {
        if (i > 0 && nums[i] == nums[i - 1]) continue;  // 去重
        if (nums[i] > 0) break;  // 最小值 > 0，后面不可能和为 0
        
        int left = i + 1, right = n - 1;
        while (left < right) {
            int sum = nums[i] + nums[left] + nums[right];
            if (sum == 0) {
                result.add(Arrays.asList(nums[i], nums[left], nums[right]));
                while (left < right && nums[left] == nums[left + 1]) left++;  // 去重
                while (left < right && nums[right] == nums[right - 1]) right--;
                left++;
                right--;
            } else if (sum < 0) {
                left++;
            } else {
                right--;
            }
        }
    }
    return result;
}
```

### 15. 二叉树层序遍历

```java
public List<List<Integer>> levelOrder(TreeNode root) {
    List<List<Integer>> result = new ArrayList<>();
    if (root == null) return result;
    
    Queue<TreeNode> queue = new LinkedList<>();
    queue.offer(root);
    
    while (!queue.isEmpty()) {
        int size = queue.size();  // ★ 当前层的节点数
        List<Integer> level = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            TreeNode node = queue.poll();
            level.add(node.val);
            if (node.left != null) queue.offer(node.left);
            if (node.right != null) queue.offer(node.right);
        }
        result.add(level);
    }
    return result;
}
```

### 16. 最长无重复子串（滑动窗口）

```java
public int lengthOfLongestSubstring(String s) {
    Map<Character, Integer> window = new HashMap<>();  // char → 最后出现位置
    int maxLen = 0, left = 0;
    
    for (int right = 0; right < s.length(); right++) {
        char c = s.charAt(right);
        if (window.containsKey(c)) {
            // 重复了，左边界跳到重复字符的下一个位置
            // 但要注意：不能跳到更左的位置（用 max 保证）
            left = Math.max(left, window.get(c) + 1);
        }
        window.put(c, right);
        maxLen = Math.max(maxLen, right - left + 1);
    }
    return maxLen;
}
// 时间复杂度 O(n)，空间 O(字符集大小)
```

### 17. 手写线程安全的生产者-消费者

```java
// Lock + Condition 实现（精确唤醒，比 synchronized+wait/notify 更灵活）
class BoundedBuffer {
    private final LinkedList<String> buffer = new LinkedList<>();
    private final int capacity;
    private final Lock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();
    
    public BoundedBuffer(int capacity) { this.capacity = capacity; }
    
    public void put(String item) throws InterruptedException {
        lock.lock();
        try {
            while (buffer.size() == capacity) {
                notFull.await();  // 满了，等不满的信号
            }
            buffer.add(item);
            notEmpty.signal();  // 唤醒等待的消费者
        } finally {
            lock.unlock();
        }
    }
    
    public String take() throws InterruptedException {
        lock.lock();
        try {
            while (buffer.isEmpty()) {
                notEmpty.await();  // 空了，等不空的信号
            }
            String item = buffer.removeFirst();
            notFull.signal();  // 唤醒等待的生产者
            return item;
        } finally {
            lock.unlock();
        }
    }
}
// 面试加分：说出 BlockingQueue 内部就是用 Lock + Condition 实现的
```

📌 **项目实践**：
- **Flavor Dash**：RabbitMQ 消息队列本质是架构层面的生产者-消费者模式——LLM 调用请求作为消息生产到队列，消费者从队列拉取并调用 API，实现异步削峰和解耦。
- **SuGuangMall**：秒杀订单异步落库，订单创建后通过消息队列异步写入数据库，生产者（Controller）快速响应前端，消费者（Listener）批量落盘，提升 TPS。

### 18. 用栈实现队列（两个栈）

```java
class MyQueue {
    private Stack<Integer> inStack = new Stack<>();   // 入队栈
    private Stack<Integer> outStack = new Stack<>();  // 出队栈
    
    public void push(int x) {
        inStack.push(x);
    }
    
    public int pop() {
        if (outStack.isEmpty()) {
            while (!inStack.isEmpty()) {
                outStack.push(inStack.pop());  // 倒过来
            }
        }
        return outStack.pop();
    }
    
    public int peek() {
        if (outStack.isEmpty()) {
            while (!inStack.isEmpty()) {
                outStack.push(inStack.pop());
            }
        }
        return outStack.peek();
    }
    
    public boolean empty() {
        return inStack.isEmpty() && outStack.isEmpty();
    }
}
// 均摊时间复杂度 O(1)
```

---

## 面试自查清单

### Java 并发进阶
- [ ] 能讲清 AQS 的 state + CLH 队列 + CAS 三要素
- [ ] 知道 CountDownLatch、CyclicBarrier、Semaphore 的区别和场景
- [ ] 能讲清 ThreadLocal 内存泄漏的原因和解决方案
- [ ] 能写出 CompletableFuture 的组合操作（thenCombine、allOf）

### JDK 新特性
- [ ] 了解 Records、Pattern Matching、Virtual Threads
- [ ] 能讲出 Virtual Threads 的原理和适用场景
- [ ] 真正用过 Stream API 的高级操作（flatMap、groupingBy、reducing）

### NIO
- [ ] 理解 BIO/NIO/AIO 的区别
- [ ] 知道 NIO 三核心：Channel、Buffer、Selector
- [ ] 知道 Netty 相比原生 NIO 的优势

### Python
- [ ] 能说清 Python 在项目中做了什么
- [ ] 知道 Python 和 Java 的差异和各自适合的场景

### 算法
- [ ] LRU Cache（HashMap + 双向链表）
- [ ] 反转链表（迭代 + 递归）
- [ ] 三数之和（排序 + 双指针 + 去重）
- [ ] 二叉树层序遍历（BFS + 记录层大小）
- [ ] 最长无重复子串（滑动窗口）

---

> **提示**：算法题不要追求全部刷完，重点掌握高频题。面试时**先讲思路再写代码**，边写边讲。如果写不出来，至少把思路讲清楚："我会用 HashMap + 双向链表，HashMap 做 O(1) 查找，双向链表维护访问顺序..."。
