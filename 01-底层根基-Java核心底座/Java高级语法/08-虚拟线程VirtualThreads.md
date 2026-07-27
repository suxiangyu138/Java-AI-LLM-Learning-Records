# 08 - 虚拟线程 Virtual Threads

> 🎯 Java 21 的虚拟线程是并发编程的革命——百万级并发不再是 Go 的专利。AI 应用中大量的 I/O 等待（API 调用、向量检索）是最适合虚拟线程的场景

---

## 目录

1. [平台线程 vs 虚拟线程](#1-平台线程-vs-虚拟线程)
2. [虚拟线程实战](#2-虚拟线程实战)
3. [适用场景与局限](#3-适用场景与局限)

---

## 1. 平台线程 vs 虚拟线程

```text
平台线程 (Platform Thread)：
  Java Thread → OS Thread (1:1 映射)
  每个线程占用 ~1MB 栈空间
  1000 个线程 = 1GB 内存 → 不适合大量并发

虚拟线程 (Virtual Thread)：
  Java Thread → Carrier Thread (M:N 映射)
  虚拟线程在 JVM 内调度，不绑定 OS 线程
  每个虚拟线程占用 ~几 KB
  100 万个虚拟线程 → 几 GB 内存 → 可以！
```

| 维度 | 平台线程 | 虚拟线程 |
|------|:---:|:---:|
| 栈大小 | ~1MB | ~几KB（堆上分配） |
| 创建成本 | 高 (~1ms) | 极低 (~1μs) |
| 数量上限 | ~几千 | **百万级** |
| 适用 | CPU 密集 | **I/O 密集** ✅ |
| 阻塞代价 | 阻塞 OS 线程 | **只阻塞虚拟线程** |
| API | 完全兼容 | 完全兼容（Thread API） |

## 2. 虚拟线程实战

```java
// 创建虚拟线程的三种方式

// 方式 1: Thread.ofVirtual()
Thread vt1 = Thread.ofVirtual()
    .name("worker-1")
    .start(() -> System.out.println("Hello from virtual thread"));

// 方式 2: Executors.newVirtualThreadPerTaskExecutor()
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    // 提交 10000 个任务，全部用虚拟线程执行！
    for (int i = 0; i < 10_000; i++) {
        int taskId = i;
        executor.submit(() -> {
            // 模拟 I/O 操作（API 调用、数据库查询...）
            Thread.sleep(Duration.ofMillis(100));
            return "Task " + taskId + " done";
        });
    }
} // try-with-resources 自动等待所有任务完成

// 方式 3: Thread.startVirtualThread()
Thread vt2 = Thread.startVirtualThread(() -> processRequest(request));
```

### AI 开发实战：并行 API 调用

```java
// 调用 3 个 AI API 并等待结果
public AiResponse callAiApis(String prompt) {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        var future1 = executor.submit(() -> callOpenAI(prompt));
        var future2 = executor.submit(() -> callClaude(prompt));
        var future3 = executor.submit(() -> callDeepSeek(prompt));

        // 收集结果（失败不影响其他）
        List<String> responses = Stream.of(future1, future2, future3)
            .map(f -> {
                try { return f.get(30, TimeUnit.SECONDS); }
                catch (Exception e) { return "Error: " + e.getMessage(); }
            })
            .toList();

        return aggregateResponses(responses);
    }
}
```

```java
// Spring Boot 3.2+ 集成虚拟线程
@Configuration
public class VirtualThreadConfig {
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerCustomizer() {
        return protocolHandler -> protocolHandler.setExecutor(
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
}
// application.yml
// spring.threads.virtual.enabled=true  ← Spring Boot 3.2+ 一句搞定
```

## 3. 适用场景与局限

```text
✅ 最适合虚拟线程的场景：
  → 大量并发 I/O 操作（API 调用、数据库查询、文件读写）
  → 每个请求需要等待多个外部服务
  → REST API 服务端（每个请求一个虚拟线程）
  → AI Agent 并行工具调用

❌ 不适合虚拟线程的场景：
  → CPU 密集型计算（没有 I/O 等待，虚拟线程无优势）
  → 需要线程局部变量（ThreadLocal 在虚拟线程下可能膨胀）
  → 被 synchronized 块长时间锁住（会 pin 住载体线程）

经典反例：
  synchronized(obj) {
      Thread.sleep(10000);  // ❌ 整个载体线程被锁住 10 秒
  }
  // 解决：用 ReentrantLock 代替 synchronized
```

## 核心要点回顾

- 虚拟线程 = 廉价线程（百万级并发）、适合 I/O 密集
- API：`Executors.newVirtualThreadPerTaskExecutor()` 最简单
- Spring Boot 3.2+ 一行开启：`spring.threads.virtual.enabled=true`
- AI 场景：并行调用多个 API → 每个用虚拟线程 → 不阻塞
- 避坑：`synchronized` 块内不要调用阻塞操作，用 `ReentrantLock`

## 参考资料

1. JEP 444: Virtual Threads
2. Spring Boot 3.2 Virtual Threads 支持
