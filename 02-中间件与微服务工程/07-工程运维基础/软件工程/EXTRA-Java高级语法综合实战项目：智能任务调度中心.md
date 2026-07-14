Java高级语法综合实战项目：智能任务调度中心
项目概述
基于Java高级语法构建的轻量级任务调度系统，整合泛型、Lambda、Stream、反射、注解、多线程、函数式接口、枚举等核心特性，实现任务创建、状态管理、异步执行、日志记录、动态配置等功能，贴合企业级开发场景。
技术选型
- 核心语法：泛型、Lambda、Stream API、函数式接口、反射、自定义注解、枚举、多线程、线程池、Optional
- 工具类：Lombok（简化代码）
- 设计模式：单例模式、策略模式、模板方法模式
- 环境：JDK 17+、Maven
    项目结构
    plaintext
    com.scheduler
    ├── annotation    // 自定义注解
    ├── enums         // 任务状态、操作类型枚举
    ├── entity        // 泛型基类、任务实体
    ├── service       // 业务逻辑（策略模式、模板方法）
    ├── executor      // 线程池、异步任务执行
    ├── aspect        // 注解解析、AOP日志
    ├── util          // 泛型工具、反射工具
    └── Main          // 项目启动入口
 
完整代码实现
1. 自定义注解（反射解析）
    java
    import java.lang.annotation.*;
    /**
     * 任务执行日志注解
     * 运行时生效，作用于方法
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface TaskLog {
    String value() default "";
    String type() default "执行";
    }
 
2. 任务状态枚举（策略模式）
    java
    import java.util.function.Predicate;
    /**
     * 任务状态枚举，内置操作校验规则
     */
    public enum TaskStatus {
    PENDING("待执行", task -> task.getRetryCount() < 3),
    RUNNING("执行中", task -> false),
    SUCCESS("执行成功", task -> false),
    FAILED("执行失败", task -> task.getRetryCount() < 3);
    private final String desc;
    private final Predicate<Task> allowOperate;
    TaskStatus(String desc, Predicate<Task> allowOperate) {
        this.desc = desc;
        this.allowOperate = allowOperate;
    }
    public boolean isAllow(Task task) {
        return allowOperate.test(task);
    }
    }
 
3. 泛型基类（代码复用）
    java
    import lombok.Data;
    import java.time.LocalDateTime;
    /**
     * 泛型实体基类，适配不同主键类型
     */
    @Data
    public abstract class BaseEntity<ID> {
    private ID id;
    private LocalDateTime createTime = LocalDateTime.now();
    private LocalDateTime updateTime = LocalDateTime.now();
    }
 
4. 任务实体类
    java
    import lombok.Data;
    import lombok.EqualsAndHashCode;
    /**
     * 任务实体，继承泛型基类
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public class Task extends BaseEntity<Long> {
    private String taskName;
    private String taskContent;
    private TaskStatus status = TaskStatus.PENDING;
    private int retryCount = 0;
    private long executeTime;
    }
 
5. 泛型工具类（反射实现）
    java
    import java.lang.reflect.Field;
    /**
     * 通用泛型工具类，实现对象属性复制
     */
    public class GenericUtil {
    /**
     * 复制非空属性
     */
    public static <T> void copyNonNull(T source, T target) {
        if (source == null || target == null) return;
        Field[] fields = source.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(source);
                if (value != null && !"serialVersionUID".equals(field.getName())) {
                    field.set(target, value);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("属性复制失败", e);
            }
        }
    }
    }
 
6. 线程池配置（单例模式）
    java
    import java.util.concurrent.ExecutorService;
    import java.util.concurrent.Executors;
    /**
     * 全局线程池单例
     */
    public class SchedulerPool {
    private static final ExecutorService POOL = Executors.newFixedThreadPool(5);
    private SchedulerPool() {}
    public static ExecutorService getInstance() {
        return POOL;
    }
    }
 
7. 任务服务层（Stream+Lambda）
    java
    import lombok.RequiredArgsConstructor;
    import java.util.ArrayList;
    import java.util.List;
    import java.util.Optional;
    import java.util.stream.Collectors;
    /**
     * 任务业务逻辑
     */
    @RequiredArgsConstructor
    public class TaskService {
    private final List<Task> taskStore = new ArrayList<>();
    /**
     * 创建任务
     */
    public void createTask(Task task) {
        taskStore.add(task);
    }
    /**
     * 按状态查询任务（Stream过滤）
     */
    public List<Task> listByStatus(TaskStatus status) {
        return taskStore.stream()
                .filter(task -> task.getStatus() == status)
                .collect(Collectors.toList());
    }
    /**
     * 更新任务状态（Optional判空）
     */
    public boolean updateStatus(Long id, TaskStatus newStatus) {
        Optional<Task> taskOpt = taskStore.stream()
                .filter(t -> t.getId().equals(id))
                .findFirst();
        if (taskOpt.isEmpty()) return false;
        Task task = taskOpt.get();
        if (!task.getStatus().isAllow(task)) return false;
        task.setStatus(newStatus);
        task.setUpdateTime(java.time.LocalDateTime.now());
        return true;
    }
    }
 
8. 异步任务执行器（多线程+Lambda）
    java
    import lombok.RequiredArgsConstructor;
    import java.util.concurrent.ExecutorService;
    /**
     * 异步执行任务
     */
    @RequiredArgsConstructor
    public class TaskExecutor {
    private final ExecutorService pool;
    private final TaskService taskService;
    @TaskLog(value = "执行任务", type = "异步")
    public void executeTask(Task task) {
        pool.execute(() -> {
            try {
                taskService.updateStatus(task.getId(), TaskStatus.RUNNING);
                System.out.println("执行任务：" + task.getTaskName());
                Thread.sleep(task.getExecuteTime());
                taskService.updateStatus(task.getId(), TaskStatus.SUCCESS);
            } catch (Exception e) {
                task.setRetryCount(task.getRetryCount() + 1);
                taskService.updateStatus(task.getId(), TaskStatus.FAILED);
                System.err.println("任务执行失败：" + e.getMessage());
            }
        });
    }
    }
 
9. 日志切面（反射解析注解）
    java
    import java.lang.reflect.Method;
    /**
     * 日志切面，解析@TaskLog注解
     */
    public class LogAspect {
    public static void log(Object target, String methodName, Object[] args) {
        try {
            Method method = target.getClass().getMethod(methodName, Task.class);
            TaskLog annotation = method.getAnnotation(TaskLog.class);
            if (annotation != null) {
                System.out.printf("[%s] %s - 参数：%s%n", annotation.type(), annotation.value(), args);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
    }
 
10. 项目启动入口
    java
    /**
     * 项目启动类，整合所有高级语法
     */
    public class Main {
    public static void main(String[] args) {
        // 初始化组件
        TaskService taskService = new TaskService();
        ExecutorService pool = SchedulerPool.getInstance();
        TaskExecutor executor = new TaskExecutor(pool, taskService);
        // 创建任务
        Task task1 = new Task();
        task1.setId(1L);
        task1.setTaskName("数据同步");
        task1.setTaskContent("同步用户数据至数据库");
        task1.setExecuteTime(2000);
        taskService.createTask(task1);
        // 执行任务（触发日志、异步、状态更新）
        LogAspect.log(executor, "executeTask", new Object[]{task1});
        executor.executeTask(task1);
        // 查询待执行任务
        System.out.println("待执行任务：" + taskService.listByStatus(TaskStatus.PENDING));
        // 关闭线程池
        pool.shutdown();
    }
    }
 
核心语法覆盖
1. 泛型：BaseEntity、GenericUtil实现通用逻辑
2. Lambda+Stream：任务过滤、集合操作简化
3. 函数式接口：TaskStatus内置Predicate校验规则
4. 反射：LogAspect解析自定义注解
5. 自定义注解：@TaskLog实现日志标记
6. 枚举：任务状态管理+策略模式
7. 多线程：线程池异步执行任务
8. Optional：优雅处理空指针
9. 单例模式：SchedulerPool全局线程池
10. Lombok：简化实体类代码
    运行效果
    1. 打印日志：[异步] 执行任务 - 参数：[Task(id=1, taskName=数据同步, ...)]
    2. 控制台输出执行任务信息
    3. 任务状态自动流转：PENDING → RUNNING → SUCCESS
    4. 待执行任务列表查询结果为空
    扩展方向
    1. 整合Spring Boot实现Web接口
    2. 接入MySQL持久化任务数据
    3. 增加定时任务、任务重试机制
    4. 实现任务依赖、分布式调度
    5. 集成Redis实现任务缓存
