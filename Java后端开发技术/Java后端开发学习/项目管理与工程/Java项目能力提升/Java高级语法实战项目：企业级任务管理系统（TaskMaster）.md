03.31 01:17
Java高级语法实战项目：企业级任务管理系统（TaskMaster）
项目简介
基于Java高级语法+Spring Boot构建的轻量级任务管理系统，覆盖泛型、反射、Lambda、Stream、函数式接口、注解、多线程、设计模式等核心高级特性，实现任务CRUD、状态流转、异步通知、数据校验、动态配置等企业级功能。
技术栈
- 核心语法：泛型、Lambda、Stream、函数式接口、方法引用、反射、注解、枚举
- 框架：Spring Boot、Spring AOP、Spring Task
- 工具：Lombok、Hutool、Guava
- 数据库：MySQL
- 其他：多线程、线程池、单例模式、策略模式、模板方法模式
项目结构
plaintext
com.taskmaster
├── config        // 配置类（线程池、动态配置）
├── annotation    // 自定义注解
├── enums         // 枚举（任务状态、操作类型）
├── entity        // 实体类（泛型基类）
├── mapper        // 数据访问层
├── service       // 业务层（策略模式、模板方法）
├── aspect        // AOP切面（日志、权限校验）
├── util          // 工具类（反射、通用泛型方法）
├── task          // 定时任务、异步任务
└── controller    // 控制层
 
核心代码实现（企业级注释+复杂度分析）
1. 泛型基类（Entity）
java
import lombok.Data;
import java.time.LocalDateTime;
/**
 * 泛型实体基类
 * 时间复杂度：O(1) 字段初始化
 * 空间复杂度：O(1) 固定字段占用
 */
@Data
public abstract class BaseEntity<ID> {
    /**
     * 泛型ID，适配不同主键类型（Long/String）
     */
    private ID id;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    /**
     * 逻辑删除标识（0-未删除，1-已删除）
     */
    private Integer isDeleted;
}
 
2. 任务状态枚举（枚举+函数式接口）
java
import java.util.function.Predicate;
/**
 * 任务状态枚举
 * 时间复杂度：O(1) 枚举常量固定
 * 空间复杂度：O(1) 常量占用固定内存
 */
public enum TaskStatus {
    // 待处理：允许编辑、删除
    PENDING("待处理", task -> task.getIsDeleted() == 0),
    // 处理中：允许完成、取消
    PROCESSING("处理中", task -> task.getIsDeleted() == 0),
    // 已完成：只读
    COMPLETED("已完成", task -> false),
    // 已取消：只读
    CANCELED("已取消", task -> false);
    private final String desc;
    /**
     * 函数式接口：判断当前状态是否允许操作
     */
    private final Predicate<Task> allowOperation;
    TaskStatus(String desc, Predicate<Task> allowOperation) {
        this.desc = desc;
        this.allowOperation = allowOperation;
    }
    /**
     * 校验是否允许操作
     * @param task 任务对象
     * @return true-允许，false-不允许
     */
    public boolean isAllow(Task task) {
        return allowOperation.test(task);
    }
}
 
3. 自定义注解（反射解析）
java
import java.lang.annotation.*;
/**
 * 操作日志注解
 * 作用范围：方法
 * 生命周期：运行时
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    /**
     * 操作描述
     */
    String value();
    /**
     * 操作类型（新增/删除/修改/查询）
     */
    String type() default "查询";
}
 
4. AOP切面（注解+反射+Lambda）
java
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
 * 时间复杂度：O(n) n为方法参数个数
 * 空间复杂度：O(n) 存储参数信息
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
        // Lambda记录日志
        String logInfo = String.format("操作类型：%s，描述：%s，参数：%s",
                annotation.type(),
                annotation.value(),
                Arrays.toString(joinPoint.getArgs()));
        log.info("开始执行 -> {}", logInfo);
        long startTime = System.currentTimeMillis();
        // 执行目标方法
        Object result = joinPoint.proceed();
        // 执行耗时计算
        long costTime = System.currentTimeMillis() - startTime;
        log.info("执行完成 -> 耗时：{}ms，结果：{}", costTime, result);
        return result;
    }
}
 
5. 泛型工具类（反射+泛型方法）
java
import java.lang.reflect.Field;
/**
 * 泛型工具类
 * 时间复杂度：O(n) n为对象字段个数
 * 空间复杂度：O(1) 临时变量占用
 */
public class GenericUtil {
    /**
     * 泛型方法：对象属性复制（忽略null值）
     * @param source 源对象
     * @param target 目标对象
     * @param <T> 泛型类型
     */
    public static <T> void copyPropertiesIgnoreNull(T source, T target) {
        if (source == null || target == null) {
            return;
        }
        // 反射获取所有字段
        Field[] fields = source.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(source);
                // 忽略null值和序列化字段
                if (value != null && !"serialVersionUID".equals(field.getName())) {
                    field.set(target, value);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("属性复制失败", e);
            }
        }
    }
}
 
6. 任务服务层（策略模式+Stream+Lambda）
java
import com.taskmaster.enums.TaskStatus;
import com.taskmaster.entity.Task;
import com.taskmaster.mapper.TaskMapper;
import com.taskmaster.util.GenericUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
/**
 * 任务服务实现
 * 时间复杂度：
 * - listByStatus：O(n) Stream过滤
 * - update：O(1) 单对象操作
 * 空间复杂度：O(n) 存储查询结果
 */
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskMapper taskMapper;
    /**
     * 根据状态查询任务（Stream+Lambda）
     * @param status 任务状态
     * @return 任务列表
     */
    public List<Task> listByStatus(TaskStatus status) {
        List<Task> allTasks = taskMapper.selectList(null);
        // Stream过滤+Lambda表达式
        return allTasks.stream()
                .filter(task -> task.getStatus() == status)
                .filter(task -> task.getIsDeleted() == 0)
                .collect(Collectors.toList());
    }
    /**
     * 更新任务（泛型工具类+状态校验）
     * @param task 待更新任务
     * @return 更新结果
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
 
7. 线程池配置（多线程+单例模式）
java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 * 线程池配置（单例模式）
 * 时间复杂度：O(1) 线程池初始化
 * 空间复杂度：O(1) 固定线程池大小
 */
@Configuration
public class ThreadPoolConfig {
    /**
     * 单例异步任务线程池
     * @return 固定大小线程池
     */
    @Bean
    public ExecutorService taskExecutor() {
        // 单例模式：全局唯一线程池
        return Executors.newFixedThreadPool(5);
    }
}
 
8. 异步通知任务（多线程+Lambda）
java
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.concurrent.ExecutorService;
/**
 * 异步通知任务
 * 时间复杂度：O(1) 异步提交
 * 空间复杂度：O(1) 临时任务存储
 */
@Component
@RequiredArgsConstructor
public class AsyncNotifyTask {
    private final ExecutorService taskExecutor;
    /**
     * 异步发送任务完成通知（Lambda+多线程）
     * @param taskId 任务ID
     */
    public void sendCompleteNotify(Long taskId) {
        taskExecutor.execute(() -> {
            // 模拟通知逻辑
            System.out.println("异步通知：任务" + taskId + "已完成");
        });
    }
}
 
项目核心高级语法总结
1. 泛型：通用基类、泛型工具类，实现代码复用
2. Lambda+Stream：集合操作、函数式接口，简化代码逻辑
3. 反射：自定义注解解析、属性复制，实现动态功能
4. 枚举+函数式接口：状态流转校验，替代冗余if-else
5. AOP：切面编程，实现日志、权限统一处理
6. 多线程+线程池：异步任务、定时任务，提升系统性能
7. 设计模式：单例、策略、模板方法，优化代码结构
运行说明
1. 环境准备：JDK17+、MySQL8.0+、Maven3.6+
2. 数据库：创建taskmaster库，执行任务表SQL
3. 配置：application.yml配置数据库连接、线程池参数
4. 启动：运行TaskMasterApplication启动类
5. 测试：通过Controller接口测试任务CRUD、状态流转、日志记录等功能

