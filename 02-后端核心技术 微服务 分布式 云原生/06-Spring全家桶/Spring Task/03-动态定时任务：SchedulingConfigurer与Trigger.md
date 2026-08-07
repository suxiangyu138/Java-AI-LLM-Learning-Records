# 03 动态定时任务：SchedulingConfigurer 与 Trigger

> @Scheduled 的 cron 在启动时"锁死"，改配置必须重启——SchedulingConfigurer + Trigger 在**每次触发前**重新计算下一次执行时间，让 cron 从配置中心/数据库热更新；本模块讲透动态化的本质与生效时机

---

## 📚 目录

1. [@Scheduled 静态化的本质](#1-scheduled-静态化的本质)
2. [SchedulingConfigurer + TriggerTask：动态化方案](#2-schedulingconfigurer--triggertask动态化方案)
3. [Trigger 接口与三种触发策略](#3-trigger-接口与三种触发策略)
4. [配置中心/数据库驱动的 cron](#4-配置中心数据库驱动的-cron)
5. [动态生效时机与边界](#5-动态生效时机与边界)
6. [动态任务的管理与监控](#6-动态任务的管理与监控)

---

## 1. @Scheduled 静态化的本质

```text
@Scheduled(cron = "${task.report.cron}")   // 看起来是"配置"，实际是"一次性"：
启动时 → 解析占位符 → 构建 CronTrigger → schedule() 注册完成
运行中 → 改配置文件/配置中心 → 触发器不变 → 任务仍按旧 cron 执行
```

**为什么改配置不生效：** `ScheduledAnnotationBeanPostProcessor` 在容器启动阶段把 cron 解析进 `CronTrigger` 并注册——之后 Trigger 对象不再重读配置。**注解的 cron 是"注册时快照"，不是"每次执行时读取"**。

| 场景 | @Scheduled 是否够用 |
|------|:---:|
| cron 长期不变 | ✅ |
| 上线后调整频率（配置中心改） | ❌ 必须重启 |
| 灰度期间频繁调整 | ❌ |
| 按业务数据动态算间隔 | ❌ 需要编程式 |

> 🎯 **要点**：判断标准一句话——"cron 会不会在运行时变"。会变，就上 SchedulingConfigurer。

## 2. SchedulingConfigurer + TriggerTask：动态化方案

```java
@Configuration
@EnableScheduling
public class DynamicTaskConfig implements SchedulingConfigurer {

    // 从配置中心/DB 读取（这里用 @Value 演示；生产走 Nacos/DB）
    @Value("${task.sync.cron:0 */5 * * * ?}")
    private String cron;

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(
            // ① 任务逻辑
            () -> doSync(),
            // ② 触发策略：每次执行前都会调用 → 总能读到最新 cron
            triggerContext -> new CronTrigger(cron)
                    .nextExecutionTime(triggerContext)
        );
    }

    private void doSync() { /* 业务 */ }
}
```

**为什么这是"动态"的：**

```text
CronTrigger 的 nextExecutionTime() 在每次"安排下一次执行"时被调用
→ 每次调用都 new CronTrigger(cron) 用最新值
→ cron 变了，下一次触发时间立即按新值计算
```

### 2.1 关键代码语义

| 代码 | 作用 |
|------|------|
| `implements SchedulingConfigurer` | 让配置类参与任务注册 |
| `registrar.addTriggerTask(runnable, trigger)` | 注册"任务+触发策略" |
| `triggerContext` | 上次执行上下文（上次实际执行时间） |
| `new CronTrigger(cron).nextExecutionTime(ctx)` | **每次执行前重算**——动态的根源 |

> 💡 更规范的做法是把 cron 读取封装成服务（`CronService.getCron("task.sync")`），配置类里只调服务——配置中心切换、缓存、日志全在服务里。

## 3. Trigger 接口与三种触发策略

```java
public interface Trigger {
    Instant nextExecutionTime(TriggerContext triggerContext);
}
```

| 触发策略 | 语义 | 适用 |
|---------|------|------|
| `CronTrigger` | 日历表达式 | 定时执行（最常用） |
| `PeriodicTrigger` | 固定间隔（delay/rate 语义） | 按毫秒间隔 |
| 自定义 Trigger | 任意计算规则 | 按业务数据/随机/条件 |

```java
// PeriodicTrigger：等价动态 fixedDelay
registrar.addTriggerTask(task, context ->
        new PeriodicTrigger(Duration.ofSeconds(5)).nextExecutionTime(context));

// 自定义 Trigger：按业务状态决定间隔
registrar.addTriggerTask(task, context -> {
    if (isIdle()) {
        return Instant.now().plus(1, ChronoUnit.MINUTES);   // 空闲 1 分钟查一次
    }
    return Instant.now().plus(10, ChronoUnit.SECONDS);      // 忙碌 10 秒查一次
});
```

> 🎯 **要点**：Trigger 的接口只有一个方法——"给我上下文，告诉我下次什么时候"。**动态化的全部秘密就是"每次重算"**。

## 4. 配置中心/数据库驱动的 cron

### 4.1 配置中心（Nacos/Spring Cloud Config）

```java
@Configuration
@EnableScheduling
@RefreshScope          // 配置刷新时重建 Bean
public class DynamicCronTask implements SchedulingConfigurer {

    @Value("${task.report.cron:0 0 2 * * ?}")
    private String cron;

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(this::report, ctx -> {
            // 每次执行前读最新值（@RefreshScope 保证 cron 字段已刷新）
            return new CronTrigger(cron).nextExecutionTime(ctx);
        });
    }
}
```

### 4.2 数据库驱动

```java
@Configuration
@EnableScheduling
public class DbCronTask implements SchedulingConfigurer {

    @Resource
    private CronMapper cronMapper;          // SELECT cron FROM task_cron WHERE name='xxx'

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(this::cleanup, ctx -> {
            String cron = cronMapper.findByName("cleanup");       // 每次执行前查库
            return new CronTrigger(cron).nextExecutionTime(ctx);
        });
    }
}
```

> ⚠️ **DB 驱动注意**：每次触发查一次 DB 的开销可忽略，但**要加缓存 + 失败兜底**（查库失败用上次 cron 或默认值，不能让任务瘫痪）；配置管理台改 DB 后任务在**下一次触发点**生效（见下节）。

## 5. 动态生效时机与边界

### 5.1 生效时机

```text
当前 cron：每月 1 号 0 点（0 0 0 1 * ?）
运行中修改为：每天 0 点（0 0 0 * * ?）

Trigger 行为：在下一次"按旧 cron 算出的执行时间"到达后，
下一次调用 nextExecutionTime 时才用新 cron 计算
→ 极端场景：改了配置，但下一次执行可能还在 30 天后
```

| 变更场景 | 生效时机 | 建议 |
|---------|---------|------|
| 秒/分级调整 | 立即（下一触发点即新值） | 无感 |
| 日/周/月级调整 | **可能延迟到旧 cron 的下一次触发** | 修改后手动触发一次 |
| 停止任务（cron 置空） | 需要"禁用"语义 | 用开关位而非空 cron |

> ⚠️ **生产技巧**：动态 cron 变更后，管理台应提供"立即触发一次"按钮（调用 `taskRegistrar` 里的 Runnable）——解决"改月级任务要等一个月才生效"的尴尬。

### 5.2 边界与坑

| 坑 | 说明 | 解法 |
|----|------|------|
| cron 非法值 | 配置写错直接触发异常，任务瘫痪 | 校验 + 默认值兜底 |
| Trigger 内抛异常 | nextExecutionTime 异常会导致调度中断 | try-catch + 记录 |
| 多个动态任务 | 每个任务一个 addTriggerTask | 一配置类可注册多个 |
| 与 @Scheduled 混用 | 可共存（不同注册通道） | 语义清晰即可 |
| 线程安全 | cron 字段被配置刷新线程修改 | 用 volatile 或读一次快照 |

## 6. 动态任务的管理与监控

```java
// 持有注册器引用：管理动态任务
@Configuration
public class TaskManagementConfig implements SchedulingConfigurer {

    private final Map<String, Runnable> tasks = new ConcurrentHashMap<>();
    private ScheduledTaskRegistrar registrar;

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        this.registrar = registrar;
        // 注册可管理的动态任务
        tasks.forEach((name, task) ->
                registrar.addTriggerTask(task, ctx -> new CronTrigger(getCron(name)).nextExecutionTime(ctx)));
    }

    // 管理接口：改 cron
    public void updateCron(String name, String cron) { saveToConfig(name, cron); }

    // 管理接口：立即执行一次
    public void triggerNow(String name) { tasks.get(name).run(); }
}
```

**管理台必备三功能：**

1. **查看**：任务列表 + 当前 cron + 上次执行时间/结果；
2. **修改**：cron 热更新（配置中心/DB）；
3. **触发**：手动立即执行一次（补跑/验证）。

> 🎯 **核心要点**：动态化的本质是"把 Trigger 的创建从启动时搬到每次执行前"——`nextExecutionTime` 每次重算，cron 自然热更新。三大纪律：①配置读写封装成服务（含失败兜底）；②修改后提供手动触发（避免月级任务等一个月）；③Trigger 内代码必须 try-catch 防瘫痪。

---

**上一模块**：[02-@Scheduled注解全解：表达式与执行语义](02-@Scheduled注解全解：表达式与执行语义.md)　**下一模块**：[04-异步任务：@Async与线程池](04-异步任务：@Async与线程池.md)
