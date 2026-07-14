# Java 高级语法实战项目：企业级任务管理系统（TaskMaster）

> 基于 Java 高级语法 + Spring Boot 构建的轻量级任务管理系统，覆盖泛型、反射、Lambda、Stream、函数式接口、注解、多线程、设计模式等核心高级特性。

---

## 目录

- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [核心代码实现](#核心代码实现)
- [项目核心高级语法总结](#项目核心高级语法总结)
- [运行说明](#运行说明)

---

## 技术栈

| 分类 | 技术 |
|------|------|
| 核心语法 | 泛型、Lambda、Stream、函数式接口、方法引用、反射、注解、枚举 |
| 框架 | Spring Boot、Spring AOP、Spring Task |
| 工具 | Lombok、Hutool、Guava |
| 数据库 | MySQL |
| 其他 | 多线程、线程池、单例模式、策略模式、模板方法模式 |

---

## 项目结构

```
com.taskmaster
├── config        // 配置类（线程池、动态配置）
├── annotation    // 自定义注解
├── enums         // 枚举（任务状态、操作类型）
├── entity        // 实体类（泛型基类）
├── mapper        // 数据访问层
├── service       // 业务层（策略模式、模板方法）
├── aspect        // AOP 切面（日志、权限校验）
├── util          // 工具类（反射、通用泛型方法）
├── task          // 定时任务、异步任务
└── controller    // 控制层
```

---

## 核心代码实现

### 1. 泛型基类（Entity）

```java
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 泛型实体基类
 * O(1) 字段初始化，O(1) 固定字段占用
 */
@Data
public abstract class BaseEntity<ID> {
    /** 泛型 ID，适配不同主键类型（Long / String） */
    private ID id;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
    /** 逻辑删除标识（0-未删除，1-已删除） */
    private Integer isDeleted;
}
```

### 2. 任务状态枚举（枚举 + 函数式接口）

> 枚举与 Predicate 函数式接口结合，实现状态流转校验，替代冗余的 if-else。

```java
import java.util.function.Predicate;

public enum TaskStatus {
    PENDING("待处理", task -> task.getIsDeleted() == 0),
    PROCESSING("处理中", task -> task.getIsDeleted() == 0),
    COMPLETED("已完成", task -> false),
    CANCELED("已取消", task -> false);

    private final String desc;
    private final Predicate<Task> allowOperation;

    TaskStatus(String desc, Predicate<Task> allowOperation) {
        this.desc = desc;
        this.allowOperation = allowOperation;
    }

    /** 校验是否允许操作 */
    public boolean isAllow(Task task) {
        return allowOperation.test(task);
    }
}
```

| 状态 | 允许操作 | 说明 |
|------|----------|------|
| PENDING | 编辑、删除 | 待处理状态 |
| PROCESSING | 完成、取消 | 处理中状态 |
| COMPLETED | 只读 | 已完成状态 |
| CANCELED | 只读 | 已取消状态 |

### 3. 自定义注解（反射解析）

```java
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    /** 操作描述 */
    String value();
    /** 操作类型（新增 / 删除 / 修改 / 查询） */
    String type() default "查询";
}
```

### 4. AOP 切面（注解 + 反射 + Lambda）

```java
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 操作日志切面
 * O(n) n 为方法参数个数
 */
@Slf4j
@Aspect
@Component
public class LogAspect {
    @Around("@annotation(com.taskmaster.annotation.OperationLog)")
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        // 反射获取方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperationLog annotation = method.getAnnotation(OperationLog.class);

        String logInfo = String.format("操作类型：%s，描述：%s，参数：%s",
                annotation.type(), annotation.value(),
                Arrays.toString(joinPoint.getArgs()));
        log.info("开始执行 -> {}", logInfo);

        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long costTime = System.currentTimeMillis() - startTime;

        log.info("执行完成 -> 耗时：{}ms，结果：{}", costTime, result);
        return result;
    }
}
```

### 5. 泛型工具类（反射 + 泛型方法）

```java
import java.lang.reflect.Field;

/**
 * 泛型工具类
 * O(n) n 为对象字段个数
 */
public class GenericUtil {
    /** 泛型方法：对象属性复制（忽略 null 值） */
    public static <T> void copyPropertiesIgnoreNull(T source, T target) {
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
```

### 6. 任务服务层（策略模式 + Stream + Lambda）

```java
import com.taskmaster.enums.TaskStatus;
import com.taskmaster.entity.Task;
import com.taskmaster.mapper.TaskMapper;
import com.taskmaster.util.GenericUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskMapper taskMapper;

    /**
     * 根据状态查询任务（Stream + Lambda）
     * O(n) Stream 过滤
     */
    public List<Task> listByStatus(TaskStatus status) {
        List<Task> allTasks = taskMapper.selectList(null);
        return allTasks.stream()
                .filter(task -> task.getStatus() == status)
                .filter(task -> task.getIsDeleted() == 0)
                .collect(Collectors.toList());
    }

    /**
     * 更新任务（泛型工具类 + 状态校验）
     * O(1) 单对象操作
     */
    @OperationLog(value = "更新任务", type = "修改")
    public boolean updateTask(Task task) {
        Task oldTask = taskMapper.selectById(task.getId());
        // 枚举函数式接口校验状态
        if (!oldTask.getStatus().isAllow(oldTask)) {
            throw new RuntimeException("当前状态不允许修改");
        }
        // 泛型方法复制非空属性
        GenericUtil.copyPropertiesIgnoreNull(task, oldTask);
        oldTask.setUpdateTime(java.time.LocalDateTime.now());
        return taskMapper.updateById(oldTask) > 0;
    }
}
```

### 7. 线程池配置（多线程 + 单例模式）

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadPoolConfig {
    /** 单例异步任务线程池 */
    @Bean
    public ExecutorService taskExecutor() {
        return Executors.newFixedThreadPool(5);
    }
}
```

### 8. 异步通知任务（多线程 + Lambda）

```java
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.concurrent.ExecutorService;

@Component
@RequiredArgsConstructor
public class AsyncNotifyTask {
    private final ExecutorService taskExecutor;

    /** 异步发送任务完成通知（Lambda + 多线程） */
    public void sendCompleteNotify(Long taskId) {
        taskExecutor.execute(() -> {
            System.out.println("异步通知：任务" + taskId + "已完成");
        });
    }
}
```

---

## 项目核心高级语法总结

| 序号 | 技术 | 应用场景 |
|------|------|----------|
| 1 | **泛型** | 通用基类、泛型工具类，实现代码复用 |
| 2 | **Lambda + Stream** | 集合操作、函数式接口，简化代码逻辑 |
| 3 | **反射** | 自定义注解解析、属性复制，实现动态功能 |
| 4 | **枚举 + 函数式接口** | 状态流转校验，替代冗余 if-else |
| 5 | **AOP** | 切面编程，实现日志、权限统一处理 |
| 6 | **多线程 + 线程池** | 异步任务、定时任务，提升系统性能 |
| 7 | **设计模式** | 单例、策略、模板方法，优化代码结构 |

---

## 运行说明

| 步骤 | 操作 |
|------|------|
| 1 | 环境准备：JDK 17+、MySQL 8.0+、Maven 3.6+ |
| 2 | 数据库：创建 taskmaster 库，执行任务表 SQL |
| 3 | 配置：application.yml 配置数据库连接、线程池参数 |
| 4 | 启动：运行 TaskMasterApplication 启动类 |
| 5 | 测试：通过 Controller 接口测试任务 CRUD、状态流转、日志记录 |
