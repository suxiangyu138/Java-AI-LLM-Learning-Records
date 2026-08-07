# 01 任务调度架构：TaskScheduler 与 ScheduledTaskRegistrar

> 一切定时任务都汇聚到两个中枢：TaskScheduler（谁在什么线程上执行）与 ScheduledTaskRegistrar（任务怎么注册）——理解装配机制，才能解释"为什么默认单线程会互相阻塞""为什么动态任务要换 Trigger"

---

## 📚 目录

1. [整体架构：任务从注解到执行的两条路径](#1-整体架构任务从注解到执行的两条路径)
2. [TaskScheduler：调度的核心抽象](#2-taskscheduler调度的核心抽象)
3. [ThreadPoolTaskScheduler：生产默认实现](#3-threadpooltaskscheduler生产默认实现)
4. [ScheduledTaskRegistrar：任务注册中枢](#4-scheduledtaskregistrar任务注册中枢)
5. [@EnableScheduling 的装配机制](#5-enablescheduling-的装配机制)
6. [单线程调度器的坑与多线程改造](#6-单线程调度器的坑与多线程改造)
7. [与 TaskExecutor 的分工](#7-与-taskexecutor-的分工)

---

## 1. 整体架构：任务从注解到执行的两条路径

```text
路径一：注解声明（@Scheduled）
  @Scheduled 方法
    → ScheduledAnnotationBeanPostProcessor 扫描
    → 解析 fixedDelay/fixedRate/cron → 构建 Trigger
    → ScheduledTaskRegistrar.addFixedDelayTask/addCronTask...
    → TaskScheduler.schedule(task, trigger)   ← 真正安排执行

路径二：编程式注册（SchedulingConfigurer）
  SchedulingConfigurer.configureTasks(reg)
    → reg.addTriggerTask/addFixedDelayTask...
    → TaskScheduler.schedule(task, trigger)

殊途同归：两条路径都落在 ScheduledTaskRegistrar → TaskScheduler
```

| 层 | 组件 | 职责 |
|----|------|------|
| 注解层 | `@Scheduled` | 声明式任务定义 |
| 扫描层 | `ScheduledAnnotationBeanPostProcessor` | 收集注解方法、解析参数 |
| 注册层 | `ScheduledTaskRegistrar` | 统一任务注册入口（静态+动态） |
| 调度层 | `TaskScheduler` | 管理线程、安排触发时间 |
| 执行层 | 调度线程池 | 实际执行任务 |

> 🎯 **核心要点**：@Scheduled 只是"注册声明"，真正的执行靠 TaskScheduler。理解"注解 → 注册器 → 调度器"三层，就理解了为什么换 Trigger 能动态化、为什么换 TaskScheduler 能改线程模型。

## 2. TaskScheduler：调度的核心抽象

```java
public interface TaskScheduler {
    ScheduledFuture<?> schedule(Runnable task, Trigger trigger);      // 通用触发策略
    ScheduledFuture<?> schedule(Runnable task, Instant startTime);     // 定点执行
    ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period);
    ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay);
}
```

| 方法 | 语义 | 对应注解 |
|------|------|---------|
| `schedule(task, trigger)` | 自定义 Trigger 决定每次触发时间 | 动态任务（03 篇） |
| `scheduleAtFixedRate` | 固定频率：按**起点**间隔 | `@Scheduled(fixedRate=)` |
| `scheduleWithFixedDelay` | 固定延迟：**上次结束后**再等 delay | `@Scheduled(fixedDelay=)` |

> ⚠️ **fixedRate 与 fixedDelay 的本质区别**：fixedRate 按"开始时刻"排队（任务执行超长会叠加/错过）；fixedDelay 按"结束时刻"排队（永不叠加）。面试必考，02 篇展开。

## 3. ThreadPoolTaskScheduler：生产默认实现

**Spring Boot 自动配置默认提供的调度器**（虚拟线程关闭时）：

```java
// 手动构建示例
ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
scheduler.setPoolSize(10);                      // 调度线程数
scheduler.setThreadNamePrefix("sched-");
scheduler.setWaitForTasksToCompleteOnShutdown(true);   // 优雅关闭
scheduler.setAwaitTerminationSeconds(30);
scheduler.setErrorHandler(throwable -> log.error("task error", throwable));
scheduler.initialize();
```

| 配置 | 默认 | 说明 |
|------|:---:|------|
| poolSize | 1 | **默认单线程**——多任务互相阻塞的根源 |
| threadNamePrefix | `scheduling-1` | 线程命名（排查必备） |
| waitForTasksToCompleteOnShutdown | false | 关闭时是否等任务完成 |
| awaitTerminationSeconds | 0 | 等待上限（秒） |
| errorHandler | 无 | 任务异常处理器（默认只是日志） |
| rejectedExecutionHandler | AbortPolicy | 拒绝策略 |

> 🎯 **默认单线程**是 Spring Task 第一坑：两个 @Scheduled 任务，一个执行 10 秒，另一个只能等它结束——**凡是"定时任务互相拖累"的生产现象，根因多半是这里**。

## 4. ScheduledTaskRegistrar：任务注册中枢

`ScheduledTaskRegistrar` 是"注解任务 + 动态任务"的汇聚点：

```java
// ScheduledTaskRegistrar 的核心 API
public class ScheduledTaskRegistrar {
    public void addFixedDelayTask(Runnable task, long delay);
    public void addFixedRateTask(Runnable task, long period);
    public void addCronTask(Runnable task, String cron);
    public void addTriggerTask(Runnable task, Trigger trigger);   // 动态触发（03 篇）
    public void setTaskScheduler(TaskScheduler taskScheduler);    // 指定调度器
    public void setScheduler(Object scheduler);                   // 兼容原始 Executor
}
```

**注册时机与生命周期：**

```text
应用启动 → 扫描器收集 @Scheduled → 注册器收集全部任务
  → afterPropertiesSet()：为每个任务调用 scheduler.schedule(...)
  → 返回 ScheduledTask（含 ScheduledFuture，可 cancel）
  → 应用关闭 → 优雅取消所有任务
```

## 5. @EnableScheduling 的装配机制

```java
@SpringBootApplication
@EnableScheduling        // 激活调度能力
public class Application { }
```

**@EnableScheduling 做了什么：**

```text
@EnableScheduling
  → @Import(SchedulingConfiguration)
  → 注册 ScheduledAnnotationBeanPostProcessor
  → 该后置处理器：
      ① 扫描所有 Bean 的 @Scheduled 方法
      ② 若容器中没有 TaskScheduler Bean → 自动创建单线程默认调度器
      ③ 把任务注册进 ScheduledTaskRegistrar
      ④ 注册 SmartLifecycle：容器关闭时优雅停止
```

| 装配点 | 行为 |
|--------|------|
| 无 TaskScheduler Bean | 自动创建**单线程**默认调度器（坑的来源） |
| 有 TaskScheduler Bean | 使用自定义（多线程/虚拟线程） |
| 有 SchedulingConfigurer | 动态任务并入同一注册器 |
| 关闭顺序 | SmartLifecycle 优先级：任务停止先于容器销毁 |

> ⚠️ **注意**：`@EnableScheduling` 与 `@EnableAsync` 是**两个独立开关**——定时任务默认在调度线程同步执行；要让定时任务与异步配合，需自行组合（04 篇）。

## 6. 单线程调度器的坑与多线程改造

### 6.1 事故现场

```java
@Component
public class BadTasks {

    @Scheduled(fixedDelay = 1000)
    public void slowTask() {
        // 模拟耗时 10 秒的报表生成
        Thread.sleep(10_000);
    }

    @Scheduled(fixedDelay = 1000)   // 这个任务每天少执行 N 次
    public void heartBeat() {
        // 心跳/清理任务被 slowTask 阻塞
    }
}
```

**现象**：heartBeat 与 slowTask 共用 1 个调度线程，slowTask 执行期间 heartBeat 完全卡死——"定时任务明明配了，就是不按时跑"。

### 6.2 三种改造方案

```java
// 方案一：自建多线程调度器（最常用）
@Bean
public TaskScheduler taskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(10);
    scheduler.setThreadNamePrefix("task-sched-");
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    scheduler.setAwaitTerminationSeconds(30);
    return scheduler;
}

// 方案二：用配置属性（Boot 4 虚拟线程场景除外，见 06 篇）
// spring.task.scheduling.pool.size=10
// spring.task.scheduling.thread-name-prefix=task-sched-

// 方案三：Boot 4 + Java 21：虚拟线程调度器（06 篇详解）
// 每个任务独立虚拟线程，天然互不阻塞
```

> 💡 **经验值**：调度线程数 = 预期并发执行的任务数上限 + 余量（一般 5-10 足够；调度线程≠业务线程，任务内部的耗时操作应交给 @Async 线程池）。**多线程调度器只解决"互相阻塞"，不解决"任务自身耗时"**——耗时的业务逻辑要异步化。

## 7. 与 TaskExecutor 的分工

| 维度 | TaskScheduler（调度器） | TaskExecutor（执行器） |
|------|:---:|:---:|
| 职责 | 决定"何时执行"（触发时间） | 决定"在哪执行"（线程池） |
| 对应注解 | @Scheduled | @Async |
| 典型实现 | ThreadPoolTaskScheduler | ThreadPoolTaskExecutor |
| 队列 | 延迟队列（DelayedWorkQueue） | 阻塞队列 |
| 配置前缀 | `spring.task.scheduling.*` | `spring.task.execution.*` |

**协同模式（生产标准姿势）：**

```text
定时任务（调度线程：快速触发）
  → 内部调 @Async 方法（业务线程：真正干活）
  → 调度线程立即空闲，不阻塞其它定时任务
```

> 🎯 **核心要点**：Spring Task 的架构就是"调度器 + 执行器"双引擎——调度器管时间（何时）、执行器管线程（哪里）。默认单线程调度器是头号坑；多线程 + @Async 卸载耗时逻辑是生产标准解；Boot 4 的虚拟线程直接消灭了"调度线程被阻塞"这一类问题（06 篇）。

---

**上一模块**：[00-Spring Task知识体系总览](00-Spring Task知识体系总览.md)　**下一模块**：[02-@Scheduled注解全解：表达式与执行语义](02-@Scheduled注解全解：表达式与执行语义.md)
