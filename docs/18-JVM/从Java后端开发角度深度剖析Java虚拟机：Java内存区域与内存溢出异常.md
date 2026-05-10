# Java 虚拟机：内存区域与内存溢出异常（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | JVM 内存区域与 OOM 排查  
> **前置基础**：Java 基础语法、JVM 基本概念  
> **关联章节**：走进 Java → **本章** → GC 与内存分配 → JVM 调优

---

## 一、核心概念

### 1.1 JVM 运行时数据区总览

| 区域 | 线程 | 存储内容 | OOM 风险 |
|------|------|----------|----------|
| **程序计数器** | 私有 | 当前线程执行的字节码指令地址 | 不会 OOM |
| **虚拟机栈** | 私有 | 栈帧（局部变量表、操作数栈等） | `StackOverflowError` / OOM |
| **本地方法栈** | 私有 | Native 方法栈帧 | `StackOverflowError` / OOM |
| **Java 堆**（核心） | 共享 | 对象实例和数组 | `Java heap space` OOM |
| **方法区/元空间** | 共享 | 类元数据、常量池、静态变量 | `Metaspace` OOM |
| **直接内存** | — | NIO Buffer（堆外） | `Direct buffer memory` OOM |

### 1.2 内存泄漏 vs 内存溢出

| 概念 | 定义 | 因果关系 |
|------|------|----------|
| **内存泄漏（Memory Leak）** | 对象已无用但被强引用持有，GC 无法回收 | **导致** OOM 的主要原因之一 |
| **内存溢出（OOM）** | JVM 无法申请到足够内存，GC 也无法释放出足够空间 | 可能是泄漏积累的结果，也可能是配置不足 |

---

## 二、底层原理

### 2.1 线程私有区域

#### 程序计数器
- 记录当前线程执行的字节码指令地址，线程切换后恢复执行位置
- JVM 中唯一 **不会发生 OOM** 的区域
- 多线程并发执行的基础支撑

#### Java 虚拟机栈
- 每个方法调用创建一个栈帧（局部变量表 + 操作数栈 + 动态链接 + 返回地址）
- 参数：`-Xss`（默认 ~1MB/线程）
- **递归过深 / 方法嵌套层级过深** → `StackOverflowError`
- **线程过多（未用线程池）** → `OutOfMemoryError: unable to create new native thread`

#### 本地方法栈
- 为 Native 方法（JNI）服务
- HotSpot 中与虚拟机栈合并实现

### 2.2 线程共享区域（GC 主战场）

#### Java 堆（核心）
- 存储所有对象实例和数组
- 分为新生代（Eden + S0 + S1，默认 8:1:1）和老年代
- 参数：`-Xms`（初始堆）、`-Xmx`（最大堆）
- 大对象（> `PretenureSizeThreshold`）直接进老年代

#### 方法区 / 元空间（JDK 8+）
- 存储类元数据、运行时常量池、静态变量、JIT 编译后代码
- JDK 7 之前：永久代（PermGen），有固定上限
- JDK 8+：元空间（Metaspace），使用本地内存，默认无上限
- 参数：`-XX:MetaspaceSize`、`-XX:MaxMetaspaceSize`
- 动态生成大量类（CGLIB 代理）→ `Metaspace OOM`

#### 直接内存
- 不属于 JVM 规范，由 NIO `ByteBuffer.allocateDirect()` 分配
- 参数：`-XX:MaxDirectMemorySize`（默认 = `-Xmx`）
- Netty、大文件 IO 高频使用

---

## 三、代码实现

### 3.1 常见 OOM 场景与解决方案

#### 堆内存溢出（最常见，占线上 OOM 80%+）

```
异常：java.lang.OutOfMemoryError: Java heap space
```

**触发场景**：
| 场景 | 原因 | 解决方案 |
|------|------|----------|
| 批量加载大量数据 | `SELECT *` 100 万条直接装入 List | 分页查询（`LIMIT`），流式处理 |
| 大对象创建 | 500MB 字节数组 | 拆分对象，流式处理 |
| 内存泄漏 | 静态集合持有对象从不清理 | 定期清理、用 Guava Cache / Caffeine |
| 无限制缓存 | HashMap 做缓存无淘汰策略 | 使用带 TTL 和容量限制的缓存框架 |

**排查方法**：
```bash
# 1. OOM 时自动生成堆转储
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/data/dump.hprof

# 2. 使用 MAT / VisualVM 分析 heap dump
# 3. jmap -heap <pid> 查看堆使用情况
# 4. jstat -gc <pid> 1000 查看 GC 频率
```

#### 元空间溢出

```
异常：java.lang.OutOfMemoryError: Metaspace
```

**触发场景**：大量第三方 Jar 包、CGLIB 动态代理生成类、多服务共享 JVM

**解决方案**：
```bash
-XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m  # 限制元空间上限
```
- 减少不必要的动态类生成（优先 JDK 动态代理）
- 清理无用类加载器

#### 直接内存溢出

```
异常：java.lang.OutOfMemoryError: Direct buffer memory
```

**触发场景**：Netty 高频 IO、NIO 未释放 Buffer

**解决方案**：
```bash
-XX:MaxDirectMemorySize=256m  # 限制直接内存
```
- 显式释放：`((DirectBuffer) buffer).cleaner().clean()`
- 优先用堆内内存（`ByteBuffer.allocate()`）

#### 栈溢出

| 类型 | 异常 | 原因 | 解决 |
|------|------|------|------|
| 栈深度溢出 | `StackOverflowError` | 无限递归 | 添加递归终止条件 |
| 线程过多 | `OOM: unable to create new native thread` | 未用线程池直接 `new Thread` | 使用 `ThreadPoolExecutor` |

#### GC 开销限制溢出

```
异常：java.lang.OutOfMemoryError: GC overhead limit exceeded
```

**原因**：GC 频繁执行但每次回收 < 2%，JVM 为避免 CPU 耗尽抛出此异常
**解决**：优先排查内存泄漏 → 增大堆内存 → 切换 GC 算法（G1）

---

## 四、实战要点

### 4.1 微服务 JVM 参数配置模板（JDK 8+）

```bash
# 堆内存（初始与最大一致，避免动态扩展）
-Xms4g -Xmx4g

# 元空间
-XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m

# 直接内存
-XX:MaxDirectMemorySize=256m

# GC 配置（大堆推荐 G1）
-XX:+UseG1GC -XX:MaxGCPauseMillis=200

# OOM 诊断
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/data/dump

# GC 日志
-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:/data/gc.log
```

### 4.2 代码编写规范

| 规范 | 说明 |
|------|------|
| **避免静态集合无限制存储** | 使用 Caffeine/Guava Cache 设置过期时间和容量限制 |
| **批量数据用分页** | MyBatis 分页、JPA 分页，禁止一次性加载全部 |
| **大文件流式解析** | 用 InputStream 流式处理，不一次性读入内存 |
| **使用线程池** | `ThreadPoolExecutor` 控制线程数，禁止直接 `new Thread` |
| **及时释放资源** | `try-with-resources` 关闭 IO/DB/Socket |

---

## 五、避坑总结

| OOM 类型 | 常见原因 | 优先排查方向 |
|----------|----------|-------------|
| `Java heap space` | 内存泄漏、数据量过大、缓存扩容 | MAT 分析 heap dump |
| `Metaspace` | CGLIB 代理过多、类加载泄漏 | `jmap -clstats <pid>` |
| `Direct buffer memory` | Netty/NIO 未释放 | 检查 `MaxDirectMemorySize` |
| `StackOverflowError` | 无限递归 | 检查递归终止条件 |
| `unable to create new native thread` | 线程数过多 | 改用线程池 |
| `GC overhead limit` | 内存泄漏导致频繁 GC | 先排查内存泄漏 |

---

## 六、企业级最佳实践

### 6.1 内存监控体系

- **Prometheus + Grafana**：堆内存使用率、GC 频率、元空间使用率监控
- **告警阈值**：堆内存 > 85%、Full GC > 1 次/小时、元空间 > 80%
- **OOM 自动 dump**：所有生产环境必须配置 `-XX:+HeapDumpOnOutOfMemoryError`

### 6.2 预防原则

1. **参数配置先行**：根据服务类型合理配置各内存区域上限
2. **代码规范为本**：避免静态集合泄漏、使用线程池、流式处理大数据
3. **监控兜底**：实时监控 + 告警 + OOM 自动 dump，缩短 MTTR
