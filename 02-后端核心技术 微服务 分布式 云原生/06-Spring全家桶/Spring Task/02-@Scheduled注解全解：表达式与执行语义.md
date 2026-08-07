# 02 @Scheduled 注解全解：表达式与执行语义

> @Scheduled 是使用率最高的注解，也是最容易写错的：fixedDelay/fixedRate/cron 三种模式语义不同、cron 表达式六位七位规则微妙、时区与占位符的隐藏细节——本模块把这些"执行语义"一次讲透

---

## 📚 目录

1. [三种触发模式与执行语义](#1-三种触发模式与执行语义)
2. [cron 表达式全解](#2-cron-表达式全解)
3. [常用属性：时区、initialDelay 与占位符](#3-常用属性时区initialdelay-与占位符)
4. [执行语义陷阱：重叠、错过与阻塞](#4-执行语义陷阱重叠错过与阻塞)
5. [自调用与代理失效](#5-自调用与代理失效)
6. [任务取消与生命周期管理](#6-任务取消与生命周期管理)

---

## 1. 三种触发模式与执行语义

### 1.1 模式对比

| 模式 | 语义 | 间隔起算 | 任务超长时 | 典型场景 |
|------|------|---------|-----------|---------|
| `fixedDelay` | 上次**结束后**再等 N 毫秒 | 结束时刻 | 永不叠加（自动延后） | 数据同步、清理（**默认推荐**） |
| `fixedRate` | 按**开始时刻**固定间隔 | 开始时刻 | **可能叠加执行**（并发）或跳过 | 心跳、刷新（追求准时） |
| `cron` | 日历表达式（秒级精度） | 日历时刻 | 错过即跳过（默认单线程） | 报表（每天 2 点）、定时开关 |

```java
@Scheduled(fixedDelay = 5000)          // 上一次执行完 5 秒后
@Scheduled(fixedRate = 5000)           // 每 5 秒一次（按开始时刻）
@Scheduled(cron = "0 0 2 * * ?")       // 每天凌晨 2 点
@Scheduled(fixedDelayString = "${task.clean.delay:5000}")   // 占位符+默认值
```

### 1.2 fixedDelay vs fixedRate 的时序图

```text
任务耗时 3 秒，间隔 5 秒：

fixedDelay（固定延迟）：
  [任务A: 0-3s] [等5s] [任务B: 8-11s] [等5s] ...
  间隔 = 3s + 5s = 8s 一次

fixedRate（固定频率）：
  [任务A: 0-3s] [任务B: 5-8s] [任务C: 10-13s] ...
  若任务耗时 8 秒 > 间隔 5 秒：
    单线程下：B 在 A 结束后立即执行（错过 5s 时刻点，不叠加）
    多线程调度器下：B 与 A 可能同时运行（叠加执行！）
```

> 🎯 **要点**：fixedRate 在"任务耗时 > 间隔"时的行为取决于调度器线程数——单线程=错过不补、多线程=并发叠加。**对执行时长没有严格上界的任务，用 fixedDelay 更安全**（永不叠加）。

## 2. cron 表达式全解

### 2.1 格式：六位（秒起）与七位

```
秒  分  时  日  月  周  [年]
0   0   2   *   *   ?   [2026]
```

| 位 | 取值范围 | 特殊字符 |
|:---:|---------|---------|
| 秒 | 0-59 | `* , - /` |
| 分 | 0-59 | `* , - /` |
| 时 | 0-23 | `* , - /` |
| 日 | 1-31 | `* , - / ? L W` |
| 月 | 1-12 或 JAN-DEC | `* , - /` |
| 周 | 1-7 或 SUN-SAT（1=SUN） | `* , - / ? L #` |
| 年（可选） | 空或 1970-2099 | `* , - /` |

### 2.2 特殊字符速查

| 字符 | 含义 | 示例 |
|:---:|------|------|
| `*` | 任意值 | `* * * * * ?` 每秒 |
| `?` | 不指定（日/周互斥，必用其一） | `0 0 2 * * ?` 每天 2 点 |
| `-` | 区间 | `0 0 9-18 * * ?` 9-18 点整点 |
| `/` | 步长 | `0 */5 * * * ?` 每 5 分钟 |
| `,` | 枚举 | `0 0 9,12,18 * * ?` 每天 9/12/18 点 |
| `L` | 最后 | `0 0 0 L * ?` 每月最后一天 |
| `W` | 最近工作日 | `0 0 0 15W * ?` 15 号最近的工作日 |
| `#` | 第几个周几 | `0 0 0 ? * 2#1` 每月第一个周一 |

### 2.3 高频表达式

| 表达式 | 语义 |
|--------|------|
| `0 0 2 * * ?` | 每天凌晨 2 点 |
| `0 */10 * * * ?` | 每 10 分钟 |
| `0 0/30 9-18 * * ?` | 9-18 点每 30 分钟 |
| `0 0 0 1 * ?` | 每月 1 号零点 |
| `0 0 0 L * ?` | 每月最后一天零点 |
| `0 0 0 ? * MON` | 每周一零点 |
| `0 15 10 ? * MON-FRI` | 工作日 10:15 |

> ⚠️ **日与周互斥**：cron 中"日"与"周"不能同时指定具体值，必须一方为 `?`（如 `0 0 0 1 * MON` 非法）。这是最常见的 cron 语法错误。

## 3. 常用属性：时区、initialDelay 与占位符

```java
@Scheduled(
    cron = "0 0 8 * * ?",
    zone = "Asia/Shanghai",           // 时区：服务器 UTC 时区用"本地 8 点"必须指定！
    initialDelay = 60_000             // 首次执行延迟 1 分钟（避开启动风暴）
)
public void morningReport() { ... }
```

| 属性 | 说明 | 坑 |
|------|------|-----|
| `zone` | cron 的时区 | 服务器 UTC、业务中国时区时**必配**，否则任务差 8 小时 |
| `initialDelay` | 首次执行前的延迟 | 多任务同时启动会打满线程池，错峰用 |
| `initialDelayString` | 占位符形式 | `${task.delay:60000}` |
| 占位符 | 全部属性支持 `${...}` | 配置中心化、可带默认值 |

> 💡 **启动风暴**：应用启动瞬间所有 @Scheduled 任务同时触发（尤其整点 cron）——用 `initialDelay` 或随机延迟错峰；数据库连接池也要扛住首波。

## 4. 执行语义陷阱：重叠、错过与阻塞

| 陷阱 | 现象 | 根因 | 解法 |
|------|------|------|------|
| 任务互相阻塞 | 多个任务不按时跑 | 默认单线程调度器 | 多线程调度器（01 篇） |
| fixedRate 叠加 | 同任务并发执行 | 耗时>间隔+多线程 | 换 fixedDelay 或加锁 |
| cron 错过 | 任务当天没跑 | 单线程被占/执行超长 | 多线程 + 监控 |
| 任务永远重叠 | 前次未结束又触发 | 无并发保护 | `@Scheduled` + 执行中标记/锁 |
| 时区错位 | 任务晚 8 小时 | zone 未指定 | 显式 `zone` |

**重叠执行保护（生产刚需）：**

```java
@Component
public class SyncTask {

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(fixedDelay = 5000)
    public void sync() {
        if (!running.compareAndSet(false, true)) {
            log.warn("sync still running, skip this round");   // 防重叠
            return;
        }
        try {
            doSync();
        } finally {
            running.set(false);
        }
    }
}
```

> 🎯 **要点**：多实例部署时 `AtomicBoolean` 只能防"本实例重叠"，跨实例重叠需分布式锁（08 篇）。

## 5. 自调用与代理失效

与 @Transactional/@Async 同族的经典坑：**同类内 `this.method()` 调用绕过了代理**——但 @Scheduled 有其特殊性：

```java
@Component
public class TaskRunner {

    @Scheduled(fixedDelay = 1000)
    public void trigger() {
        // ✅ 正常：@Scheduled 由 BeanPostProcessor 直接注册，this 调用不受影响
        // （@Scheduled 的注册不依赖 AOP 代理，扫描器直接拿方法注册）
        doWork();
    }

    public void doWork() { ... }
}
```

| 注解 | 自调用是否失效 | 原因 |
|------|:---:|------|
| @Scheduled | ❌ 不失效 | 扫描器直接注册方法，不走代理 |
| @Async | ✅ **失效** | 走 AOP 代理（04 篇） |
| @Transactional | ✅ **失效** | 走 AOP 代理 |

> 💡 **易混点**：很多人以为 @Scheduled 也有自调用问题——其实没有。真正会失效的是**任务内部调用的 @Async/@Transactional 方法**（代理跨类才生效）。

## 6. 任务取消与生命周期管理

```java
// 方式一：通过注册器管理（高级）
@Resource
private ScheduledTaskRegistrar taskRegistrar;   // 通常注入不到，用 SchedulingConfigurer 持有

// 方式二：自行持有 ScheduledFuture
@Autowired
private TaskScheduler taskScheduler;

ScheduledFuture<?> future = taskScheduler.schedule(() -> {...}, new CronTrigger("0 0 2 * * ?"));
future.cancel(false);          // 取消（false=不中断正在执行）

// 方式三：运行期开关（业务常用）
@Component
public class SwitchableTask {
    private volatile boolean enabled = true;

    @Scheduled(fixedDelay = 5000)
    public void run() {
        if (!enabled) return;              // 配置中心开关：true/false 动态启停
        doWork();
    }
}
```

> ⚠️ **优雅关闭注意**：默认关闭时容器直接打断调度线程——正在执行的任务可能被中断。生产务必配置 `waitForTasksToCompleteOnShutdown=true` + `awaitTerminationSeconds`（05 篇）。

> 🎯 **核心要点**：@Scheduled 的三个语义武器——fixedDelay（结束起算，安全）、fixedRate（开始起算，准时但有叠加风险）、cron（日历语义，配 zone）。写任务的纪律：耗时不可控用 fixedDelay、跨时区必配 zone、启动错峰用 initialDelay、防重叠加运行中标记。

---

**上一模块**：[01-任务调度架构：TaskScheduler与ScheduledTaskRegistrar](01-任务调度架构：TaskScheduler与ScheduledTaskRegistrar.md)　**下一模块**：[03-动态定时任务：SchedulingConfigurer与Trigger](03-动态定时任务：SchedulingConfigurer与Trigger.md)
