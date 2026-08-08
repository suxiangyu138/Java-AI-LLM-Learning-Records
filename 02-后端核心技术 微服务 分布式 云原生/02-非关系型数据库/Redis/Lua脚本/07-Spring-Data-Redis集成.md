# Spring Data Redis 集成
> DefaultRedisScript、RedisTemplate.execute 执行脚本、序列化陷阱、NOSCRIPT 处理与 Function 调用——Java 端把脚本跑起来的完整姿势

## 📚 目录
1. [集成总览与分工](#1-集成总览与分工)
2. [DefaultRedisScript：脚本的 Java 封装](#2-defaultredisscript脚本的-java-封装)
3. [执行脚本：execute 全解](#3-执行脚本execute-全解)
4. [序列化陷阱](#4-序列化陷阱)
5. [NOSCRIPT 与脚本管理](#5-noscript-与脚本管理)
6. [调用 Redis Function](#6-调用-redis-function)
7. [生产集成规范](#7-生产集成规范)

## 1. 集成总览与分工

| 内容 | 位置 |
|------|------|
| RedisTemplate 配置/连接工厂/缓存注解 | [Redis 主体系 08](../../Redis/08-Spring%20Boot集成Redis与Lua.md)（不重复） |
| **脚本执行细节**（本文件） | DefaultRedisScript、execute、序列化、NOSCRIPT、Function |

> 🎯 分工说明：主体系的 08 篇已有紧凑 Lua 章节（基础语法/案例/Lua vs 事务），本文件做**工程化深挖**——尤其序列化与 NOSCRIPT 这两个生产高频坑。

## 2. DefaultRedisScript：脚本的 Java 封装

```java
// 脚本定义为 Spring Bean（启动时加载，便于 SCRIPT LOAD 预热）
@Configuration
public class RedisScriptConfig {

    @Bean
    public DefaultRedisScript<Long> rateLimitScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/rate_limit.lua"));  // 脚本文件
        script.setResultType(Long.class);   // ⚠️ 返回类型必须声明！
        return script;
    }
}
```

```lua
-- resources/scripts/rate_limit.lua（固定窗口限流）
local current = tonumber(redis.call('GET', KEYS[1]) or '0')
if current >= tonumber(ARGV[1]) then
    return 0
end
redis.call('INCR', KEYS[1])
redis.call('EXPIRE', KEYS[1], ARGV[2])
return 1
```

| DefaultRedisScript 要素 | 说明 |
|------------------------|------|
| `setLocation` / `setScriptText` | 脚本来源（classpath 文件 / 内联文本） |
| **`setResultType`** | 返回类型（Long/List/自定义）——**不设或设错 → 反序列化异常** |
| `sha1` | 自动计算（execute 时走 EVALSHA） |
| 线程安全 | Bean 单例可并发使用 |

> ⚠️ **ResultType 是头号坑**：脚本返回 `1`（整数），ResultType 设 String → 拿到字符串或序列化异常；返回 nil 时 ResultType 必须能表达 null（Long 可）。规则：**脚本返回什么类型，ResultType 就声明什么**。

## 3. 执行脚本：execute 全解

```java
// ═══ 方式一：默认执行（RedisTemplate 自动走 EVALSHA + NOSCRIPT 回退）═══
@Service
public class RateLimitService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;   // 注入 Bean

    public boolean tryAcquire(String key, int limit, long windowSeconds) {
        Long result = redisTemplate.execute(rateLimitScript,   // 脚本
                List.of(key),                                  // KEYS
                limit, windowSeconds);                         // ARGV
        return result != null && result == 1L;
    }
}

// ═══ 方式二：显式指定序列化器（应对 RedisTemplate 泛型不一致）═══
Long result = redisTemplate.execute(rateLimitScript,
        stringRedisSerializer,        // keySerializer
        stringRedisSerializer,        // valueSerializer
        List.of(key),
        limit, windowSeconds);
```

| execute 参数 | 说明 |
|-------------|------|
| `script` | DefaultRedisScript |
| `keys` | KEYS 列表 |
| `args...` | ARGV 参数（按声明顺序） |
| 序列化器重载 | 显式指定 key/value 序列化器（泛型混乱时的急救） |

> 💡 内部机制：`execute` 默认使用 **RedisTemplate 的序列化器**处理 KEYS/ARGV 与返回值——这就是"脚本没问题但结果不对"的根源（见 §4）。

## 4. 序列化陷阱

```text
序列化链路（每次 execute 都会发生）：
  Java 参数 → 序列化 → Redis（Lua 拿到字节）
  Lua 返回 → Redis 类型 → 反序列化 → Java 对象

三条链各自独立：
  keySerializer      ：KEYS[i] 的序列化
  valueSerializer    ：ARGV[i] 与返回值的序列化
  hashKey/Value      ：HASH 字段相关
```

| 陷阱 | 现象 | 解法 |
|------|------|------|
| RedisTemplate 用 JDK 序列化 | KEYS 带 `\xac\xed\x00\x05` 前缀，键对不上 | 业务键用 StringRedisTemplate 或统一 JSON 序列化 |
| ARGV 数字被序列化成字节 | Lua `tonumber(ARGV[1])` 拿到 nil | ARGV 传字符串（`String.valueOf(limit)`） |
| 返回值反序列化失败 | 脚本返回 1 但 ResultType=String | ResultType 与返回值对齐（§2） |
| 脚本内 redis.call 的键 | 与 Java 侧 keySerializer 不一致 → MISS | 全链路统一序列化器 |

```java
// ⚠️ 生产实践：脚本操作用 StringRedisTemplate（最稳）
// String 序列化 = 无歧义（键就是字符串，ARGV 就是字符串）
private final StringRedisTemplate redis;    // 注入 StringRedisTemplate

Long result = redis.execute(rateLimitScript, List.of(key),
        String.valueOf(limit), String.valueOf(windowSeconds));
```

> 🎯 铁律：**脚本相关操作统一用 StringRedisTemplate**——JDK 序列化 + Lua 字节交互是踩坑重灾区；键和参数全部按字符串传递，Lua 侧 `tonumber()` 转换。参考 [Redis 主体系序列化章节](../../Redis/08-Spring%20Boot集成Redis与Lua.md)。

## 5. NOSCRIPT 与脚本管理

```text
Spring Data Redis 的 NOSCRIPT 处理（DefaultRedisScript.execute 内部）：
  · 默认先 EVALSHA（sha1 由 setLocation/setScriptText 自动计算）
  · 收到 NOSCRIPT → 自动 SCRIPT LOAD → 重试一次（框架已处理！）

手动场景（原生连接/自定义）才需要自己写回退：
  · 应用启动时预热：redisTemplate.execute(script, ...) 一次（触发 LOAD）
  · Redis 重启后首个请求：框架自动重载（无需应用干预）
```

| 场景 | 行为 |
|------|------|
| 正常调用 | EVALSHA（脚本已缓存） |
| 缓存丢失（重启/FLUSH） | 框架捕获 NOSCRIPT → 重新 LOAD → 重试 |
| 事务内 EVALSHA | 若 NOSCRIPT 则整个事务失败（**事务内不做重试**，脚本需预热） |
| 手动预热 | 启动时 `@PostConstruct` 调一次 execute |

> ⚠️ **事务 + 脚本的坑**：`@Transactional`（Redis 事务）内执行脚本，若脚本未缓存 → NOSCRIPT 导致整个事务失败且**无法重试**——解决：启动时预热脚本（确保已 LOAD）。

## 6. 调用 Redis Function

```java
// Spring Data Redis 执行 Function（FCALL）——原生 Connection API
// 方式一：execute 原生命令
String result = redisTemplate.execute((RedisCallback<String>) connection -> {
    // FCALL myFunc 1 key arg
    Object r = connection.execute("FCALL",
            new byte[][]{"myFunc".getBytes(),
                         "1".getBytes(),
                         key.getBytes(),
                         arg.getBytes()});
    return r == null ? null : new String((byte[]) r);
});

// 方式二：Lettuce 原生 API（绕过 Spring 封装）
// redisTemplate.getConnectionFactory().getConnection() → fcall(...)
```

> 💡 Spring Data Redis 对 Function 的封装落后于原生命令——**生产中直接走 connection.execute 或 Lettuce 原生 API**。或者：**Function 注册仍用 redis-cli（FUNCTION LOAD），Java 侧只负责 FCALL 调用**——职责分离最清晰。

## 7. 生产集成规范

```text
生产清单（照着做）：
  □ 脚本文件放 resources/scripts/（版本入库，CI 可审计）
  □ DefaultRedisScript 注册为 Bean + ResultType 声明
  □ 脚本操作统一 StringRedisTemplate
  □ 启动时预热（@PostConstruct 执行一次触发 SCRIPT LOAD）
  □ Function 方案：FUNCTION LOAD 走发布流水线，Java 只 FCALL
  □ 脚本变更走灰度（新脚本名/新版本，双跑验证）
  □ 慢脚本监控（SLOWLOG + 脚本耗时埋点）
```

| 规范 | 原因 |
|------|------|
| 脚本文件化 | 可版本管理、可 Code Review、可复用（多服务共享） |
| 预热 | 避免首个请求的 LOAD 延迟与事务内 NOSCRIPT |
| String 序列化 | 消除 JDK 序列化与 Lua 字节交互的坑（§4） |
| Function 流水线 | 服务器脚本纳入发布管理（[05 §5](05-Redis-Function机制.md)） |
| 慢脚本监控 | 脚本卡顿 = 全局阻塞（[03 §3](03-原子性与执行语义.md)） |

---

**下一模块**：[08-性能调试与生产实践](08-性能调试与生产实践.md) / **返回总览**：[00-Lua脚本总览](00-Lua脚本总览.md)
