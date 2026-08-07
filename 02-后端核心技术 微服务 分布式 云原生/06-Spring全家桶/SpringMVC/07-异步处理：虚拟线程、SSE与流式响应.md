# 07 异步处理：虚拟线程、SSE 与流式响应

> 长任务不占线程、实时推送不轮询、大结果流式吐——SpringMVC 的三类异步能力（虚拟线程默认化、SSE、流式响应）是 2026 年 Web 层的性能与体验分水岭，本模块讲透机制与生产注意

---

## 📚 目录

1. [Boot 4 虚拟线程：异步的新默认答案](#1-boot-4-虚拟线程异步的新默认答案)
2. [SSE：Server-Sent Events 实时推送](#2-sseserver-sent-events-实时推送)
3. [SSE 与 WebSocket 的选型](#3-sse-与-websocket-的选型)
4. [流式响应：StreamingResponseBody](#4-流式响应streamingresponsebody)
5. [DeferredResult 与 Callable](#5-deferredresult-与-callable)
6. [异步场景的拦截器与异常处理](#6-异步场景的拦截器与异常处理)
7. [生产注意：超时、背压与断连](#7-生产注意超时背压与断连)

---

## 1. Boot 4 虚拟线程：异步的新默认答案

**Spring Boot 4.0 在 Java 21+ 默认用虚拟线程处理请求**（`spring.threads.virtual.enabled=true` 默认开启）：

```yaml
spring:
  threads:
    virtual:
      enabled: true      # Boot 4 默认（Java 21+）
```

| 对比 | 平台线程（Tomcat 线程池） | 虚拟线程（Boot 4 默认） |
|------|:---:|:---:|
| 线程上限 | 200（默认）| 无实际上限 |
| 阻塞成本 | 占住 Tomcat 线程 → 池满 503 | 让出 carrier，吞吐量级提升 |
| 配置 | 调 server.tomcat.threads.max | 无需调 |
| 实测 | 基准 | I/O 密集场景约 +39% 吞吐（实测案例） |
| 适用 | 任意 | I/O 密集最佳 |

**对开发的影响：**

```text
① 同步代码（阻塞 DB/HTTP 调用）就是最佳实践——无需为长任务写异步 API
② 长任务接口（报表导出）直接同步写，不再担心占满线程池
③ 新陷阱：
   - ThreadLocal 泄漏（carrier 复用 → 上下文串请求）→ 用完必清
   - synchronized pinning（JDK 24 JEP 491 已修复）
   - 监控工具需适配虚拟线程
```

> 🎯 **核心要点**：虚拟线程默认化后，"异步接口"（DeferredResult/Callable）的意义从"不占线程"变为"满足协议需求"（SSE/长连接/流式）——**同步代码重新成为首选**。

## 2. SSE：Server-Sent Events 实时推送

**SSE**：服务端单向实时推送的 HTTP 协议（`text/event-stream`）——基于普通 HTTP 长连接，无需 WebSocket 的握手与协议切换。

### 2.1 基础实现

```java
@RestController
public class NotifyController {

    // 简单推送：一次性返回（不适合持续）
    // 持续推送：SseEmitter
    @GetMapping("/api/notify/subscribe")
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);          // 超时：0=不超时（或 30_000）
        // 业务线程推送
        executor.execute(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    emitter.send(SseEmitter.event()
                            .id(String.valueOf(i))
                            .name("order-update")         // 事件名（前端 addEventListener 区分）
                            .data(Map.of("orderId", 1001, "status", "PAID")));
                    Thread.sleep(1000);
                }
                emitter.complete();                        // 完成
            } catch (Exception e) {
                emitter.completeWithError(e);              // 异常结束
            }
        });
        return emitter;
    }
}
```

```javascript
// 前端消费
const es = new EventSource('/api/notify/subscribe');
es.addEventListener('order-update', (e) => {
    const data = JSON.parse(e.data);   // {orderId: 1001, status: "PAID"}
    console.log(data);
});
es.onerror = () => console.log('connection broken, will auto-reconnect');
```

### 2.2 SSE 的协议细节

| 特性 | 说明 |
|------|------|
| 传输 | 纯 HTTP（text/event-stream），**自动重连**（浏览器内建） |
| 方向 | 服务端 → 客户端单向 |
| 心跳 | 需定期 send 注释/事件防中间层断连（proxy 空闲超时） |
| 断连恢复 | 客户端 Last-Event-ID 头 → 服务端可从断点续推 |

```java
// 心跳 + 断点续传（生产要点）
emitter.send(SseEmitter.event().comment("heartbeat"));    // 心跳注释行

// 断连恢复：读 Last-Event-ID 头
@GetMapping("/api/notify/subscribe")
public SseEmitter subscribe(@RequestHeader(value = "Last-Event-ID", required = false) String lastId) {
    SseEmitter emitter = new SseEmitter(0L);
    // lastId 非空 → 从断点后的数据开始推
    ...
}
```

## 3. SSE 与 WebSocket 的选型

| 维度 | SSE | WebSocket |
|------|:---:|:---:|
| 方向 | 服务端单向 | 双向 |
| 协议 | 普通 HTTP | 独立协议（握手升级） |
| 自动重连 | ✅ 内建 | ❌ 需自建 |
| 穿透性 | ✅ 友好（HTTP） | 需网关/代理支持升级 |
| 负载均衡 | 天然兼容 | 需处理连接粘滞 |
| 适用 | 通知/进度/行情推送 | 聊天/协作/双向交互 |

> 🎯 **选型口诀**："服务端要往客户端推" → SSE（简单可靠）；"客户端也要发" → WebSocket。**多数通知类场景 SSE 就够**，且运维成本远低于 WebSocket。

## 4. 流式响应：StreamingResponseBody

大结果集（导出百万行 CSV / 大文件）**边生成边发送**，避免一次性装入内存：

```java
@GetMapping("/api/export/users.csv")
public ResponseEntity<StreamingResponseBody> exportUsers() {
    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(out -> {
                // 流式写出（Workbook/Writer 的流版本）
                try (Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                    writer.write("id,name,status\n");
                    userService.streamAll(chunk -> {
                        for (User u : chunk) {
                            writer.write(u.getId() + "," + u.getName() + "," + u.getStatus() + "\n");
                        }
                        writer.flush();          // 每批 flush：客户端可渐进接收
                    });
                }
            });
}
```

| 要点 | 说明 |
|------|------|
| 内存 | 只驻留当前批次（分页/游标取数） |
| 响应头 | Content-Disposition 触发下载 |
| flush | 每批 flush 客户端才能渐进看到 |
| 超时 | 流式接口**不适合长超时等待**——配合异步/虚拟线程 |

> ⚠️ **注意**：同步 StreamingResponseBody 在容器线程上执行（长时间流式占线程）；Boot 4 虚拟线程下无碍，平台线程模式下可用 `Callable<StreamingResponseBody>` 或 DeferredResult 外包。

## 5. DeferredResult 与 Callable

**DeferredResult**：请求线程立即返回、业务线程稍后完成——异步响应的经典形态（虚拟线程时代价值降低，但理解机制仍必要）：

```java
@GetMapping("/api/async-task")
public DeferredResult<String> asyncTask() {
    DeferredResult<String> result = new DeferredResult<>(10_000L);  // 超时

    // 请求线程返回；业务线程完成
    executor.execute(() -> {
        String output = expensiveWork();
        result.setResult(output);          // 完成 → 容器写响应
    });

    // 超时兜底
    result.onTimeout(() -> result.setErrorResult(ResponseEntity.status(504).build()));
    return result;
}
```

| 形态 | 语义 | 7.x 定位 |
|------|------|---------|
| `Callable<T>` | 容器线程池执行回调 | 旧式异步 |
| `DeferredResult<T>` | 外部线程完成响应 | 队列/消息驱动场景 |
| `SseEmitter` | SSE 推送 | 实时推送 |
| `StreamingResponseBody` | 流式写 | 大结果导出 |

> 💡 **虚拟线程时代的定位**：DeferredResult/Callable 的"不占线程"动机被虚拟线程取代；**保留价值在消息驱动场景**（MQ 回调完成 HTTP 请求）与长轮询。

## 6. 异步场景的拦截器与异常处理

```java
// 异步拦截器：适配 DeferredResult/SSE 场景（回调线程触发）
@Component
public class AsyncAuditInterceptor extends AsyncHandlerInterceptor {

    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request,
                                               HttpServletResponse response, Object handler) {
        // 异步开始：记录上下文（原线程即将释放）
        auditContext.set(request.getAttribute("requestId"));
    }
}

// 注册：与普通拦截器相同
registry.addInterceptor(asyncAuditInterceptor).addPathPatterns("/api/async/**");
```

| 异步坑 | 说明 | 处理 |
|--------|------|------|
| afterCompletion 提前执行 | 异步未完成时线程已返回 | 用 `AsyncHandlerInterceptor.afterConcurrentHandlingStarted` |
| 异常响应错位 | 业务线程异常在线程池 | DeferredResult.setErrorResult / emitter.completeWithError |
| ThreadLocal 串扰 | 回调线程上下文残留 | 每次进入清理/显式传递 |
| 超时无响应 | 客户端空等 | onTimeout / onError 兜底 |

## 7. 生产注意：超时、背压与断连

| 风险 | 对策 |
|------|------|
| SSE 长连接堆积 | 客户端数量监控 + 连接上限（信号量） |
| 断连后服务端继续推 | 检测 `emitter` 发送异常 → complete 并清理 |
| 中间代理空闲断连 | 心跳注释（30s 内必须发数据） |
| 推送积压（下游慢） | 发送队列监控 + 背压（丢弃或降频） |
| 流式导出超大 | 限行数 + 分片下载 + 进度提示 |
| 超时配置 | 网关/代理/Tomcat 三层超时联动评估 |

**SSE 连接管理的生产模板：**

```java
@Component
public class SseHub {

    private final Map<String, SseEmitter> clients = new ConcurrentHashMap<>();
    private final Semaphore connectionLimit = new Semaphore(1000);   // 连接上限

    public SseEmitter subscribe(String userId) {
        if (!connectionLimit.tryAcquire()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "连接数超限");
        }
        SseEmitter emitter = new SseEmitter(0L);
        clients.put(userId, emitter);
        emitter.onCompletion(() -> { clients.remove(userId); connectionLimit.release(); });
        emitter.onTimeout(() -> { emitter.complete(); });
        emitter.onError(e -> { emitter.complete(); });
        return emitter;
    }

    public void pushTo(String userId, Object data) {
        SseEmitter emitter = clients.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("notify").data(data));
            } catch (Exception e) {
                clients.remove(userId);    // 断连清理
                connectionLimit.release();
            }
        }
    }
}
```

> 🎯 **核心要点**：Boot 4 虚拟线程默认化后，同步代码回归首选，异步 API 聚焦协议需求（SSE 推送、流式导出、消息驱动）。SSE 三件套：心跳防断、断点续传（Last-Event-ID）、连接管理（上限+清理）；流式响应三件套：分批取数、逐批 flush、超时兜底。

---

**上一模块**：[06-拦截器与过滤器](06-拦截器与过滤器.md)　**下一模块**：[08-文件上传下载与内容协商](08-文件上传下载与内容协商.md)
