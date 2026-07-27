# 06 - Dubbo SPI 与 Filter 链

> 🎯 Dubbo 的可扩展性源于其 SPI 机制 — 自适应扩展、AOP 包装、IOC 注入；Filter 链则实现了调用过程的拦截编排。这两者是 Dubbo 高级用法的核心

---

## 目录

1. [Dubbo SPI 概述](#1-dubbo-spi-概述)
2. [自适应扩展（@Adaptive）](#2-自适应扩展adaptive)
3. [SPI AOP 包装](#3-spi-aop-包装)
4. [Filter 链机制](#4-filter-链机制)
5. [泛化调用](#5-泛化调用)
6. [隐式传参与上下文](#6-隐式传参与上下文)

---

## 1. Dubbo SPI 概述

> Dubbo SPI 是对 JDK SPI 的增强：按需加载、IOC 注入、AOP 包装、自适应扩展。

### 1.1 JDK SPI vs Dubbo SPI

| 维度 | JDK SPI | Dubbo SPI |
|------|---------|-----------|
| 加载方式 | 全量加载（`ServiceLoader`） | 按 Key 加载（按需） |
| IOC | ❌ | ✅ 自动注入 |
| AOP | ❌ | ✅ Wrapper 包装 |
| 扩展点 | 接口 | 接口 + @SPI |
| 配置文件 | `META-INF/services/` | `META-INF/dubbo/` |

### 1.2 基本用法

```java
// 1. 定义扩展点接口
@SPI("hessian2")   // 默认实现
public interface Serialization {
    byte[] serialize(Object obj);
    <T> T deserialize(byte[] data, Class<T> clazz);
}

// 2. 实现
public class KryoSerialization implements Serialization {
    @Override
    public byte[] serialize(Object obj) { /* Kryo 序列化 */ }
    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) { }
}

// 3. 配置文件 META-INF/dubbo/org.apache.dubbo.common.serialize.Serialization
// kryo=com.example.KryoSerialization

// 4. 使用
Serialization s = ExtensionLoader
    .getExtensionLoader(Serialization.class)
    .getExtension("kryo");
```

---

## 2. 自适应扩展（@Adaptive）

> @Adaptive 根据 URL 参数动态选择扩展实现。

### 2.1 两种方式

```java
// 方式1：注解在类上 — 手动指定为自适应实现
@Adaptive
public class AdaptiveSerialization implements Serialization {
    public byte[] serialize(Object obj) {
        // 从上下文 URL 中获取 serialization 参数 → 选择具体实现
        String name = RpcContext.getContext().getUrl()
            .getParameter("serialization", "hessian2");
        return ExtensionLoader.getExtensionLoader(Serialization.class)
            .getExtension(name).serialize(obj);
    }
}

// 方式2：注解在方法上 — Dubbo 自动生成自适应类（⭐ 常用）
@SPI("hessian2")
public interface Serialization {
    @Adaptive("serialization")    // 从 URL 参数 serialization 获取扩展名
    byte[] serialize(Object obj);
}
```

### 2.2 自适应原理

```text
Dubbo 为 @Adaptive 方法自动生成代理类：

1. 从 RpcContext 获取 URL
2. 从 URL 中取 @Adaptive("xxx") 指定的参数值
3. 如果取不到 → 使用 @SPI 指定的默认值
4. 通过 ExtensionLoader 获取对应实现

示例：URL 中 serialization=kryo → 加载 KryoSerialization
```

---

## 3. SPI AOP 包装

### 3.1 Wrapper 机制

```java
// 包装类：构造参数是接口类型
public class LoggingSerialization implements Serialization {

    private final Serialization delegate;

    public LoggingSerialization(Serialization delegate) {
        this.delegate = delegate;   // Dubbo 自动注入真正实现
    }

    @Override
    public byte[] serialize(Object obj) {
        System.out.println("开始序列化");
        byte[] result = delegate.serialize(obj);
        System.out.println("序列化完成，大小：" + result.length);
        return result;
    }
}

// 配置文件：logging=com.example.LoggingSerialization
// → Dubbo 自动识别为 Wrapper（构造参数是接口类型）
```

```text
最终调用链：
  LoggingSerialization → KryoSerialization
  (AOP 包装)             (真正实现)

→ 类似 Spring AOP，自动将多个 Wrapper 层层包裹
```

---

## 4. Filter 链机制

> Filter 类似 Servlet Filter，在 Dubbo 调用前后插入自定义逻辑。

### 4.1 内置 Filter

| Filter | 作用 |
|--------|------|
| `AccessLogFilter` | 访问日志 |
| `ActiveLimitFilter` | 并发限制 |
| `ExecuteLimitFilter` | 执行限制 |
| `TimeoutFilter` | 超时记录 |
| `TpsLimitFilter` | TPS 限流 |
| `ExceptionFilter` | 异常处理 |
| `TokenFilter` | Token 验证 |
| `ContextFilter` | 隐式传参 |

### 4.2 自定义 Filter

```java
@Activate(group = {PROVIDER, CONSUMER})
public class TraceFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String traceId = RpcContext.getServiceContext()
            .getAttachment("traceId");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
            RpcContext.getServiceContext().setAttachment("traceId", traceId);
        }

        long start = System.currentTimeMillis();
        try {
            return invoker.invoke(invocation);  // 调用下一个 Filter
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            System.out.printf("[%s] %s 耗时: %dms%n", traceId, invocation.getMethodName(), elapsed);
        }
    }
}
```

```properties
# META-INF/dubbo/org.apache.dubbo.rpc.Filter
traceFilter=com.example.TraceFilter
```

### 4.3 Filter 链执行顺序

```text
Consumer 端：
  TraceFilter → TokenFilter → MonitorFilter → → → 网络调用

Provider 端：
  ← ← ← 网络请求 → ContextFilter → ExceptionFilter → TraceFilter → 业务实现

顺序由 @Activate(group, order) 控制，order 越小越靠前
```

---

## 5. 泛化调用

> 💡 不需要引入服务接口的 class，直接通过接口名和方法名调用 — 适合网关/测试平台。

```java
// 配置泛化调用
@DubboReference(interfaceName = "com.example.UserService", generic = true)
private GenericService userService;

// 调用
public User getUser(Long id) {
    Object result = userService.$invoke(
        "getUser",                    // 方法名
        new String[]{"java.lang.Long"},  // 参数类型
        new Object[]{1L}              // 参数值
    );
    // result 是 HashMap，需手动转 User 对象
    return convertToUser((Map<String, Object>) result);
}

// 返回 POJO 的泛化
@DubboReference(interfaceName = "com.example.UserService",
                generic = "bean")     // 返回 JavaBean
private GenericService userService;
```

| 参数 | 说明 |
|------|------|
| `generic = true` | 返回 `HashMap` |
| `generic = "bean"` | 返回 POJO（需 Consumer 有对应类） |
| `generic = "protobuf-json"` | 返回 Protobuf JSON |

---

## 6. 隐式传参与上下文

### 6.1 RpcContext 隐式传参

```java
// Consumer 端：设置隐式参数
RpcContext.getServiceContext().setAttachment("traceId", "abc123");
RpcContext.getServiceContext().setAttachment("userId", "10086");
userService.getUser(1L);

// Provider 端：获取隐式参数
String traceId = RpcContext.getServiceContext().getAttachment("traceId");
```

```java
// 对端异步获取
CompletableFuture<User> future = RpcContext.getServiceContext().getCompletableFuture();
future.whenComplete((user, ex) -> {
    if (ex == null) { /* 成功 */ }
});
```

### 6.2 隐式传参 vs 显式传参

| 维度 | 隐式传参 | 显式传参 |
|------|----------|----------|
| 方式 | `RpcContext.setAttachment` | 方法参数 |
| 接口侵入 | ❌ 无侵入 | ✅ 需要改接口 |
| 适用 | 横切关注点（traceId/用户信息） | 业务数据 |

> 🎯 **最佳实践**：全链路追踪 ID 通过自定义 Filter + RpcContext 隐式传参；业务数据通过接口参数显式传递。两者互补。
