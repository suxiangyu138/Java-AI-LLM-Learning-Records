# 快速吃透 Spring Framework 核心
## 一、核心定位
Spring Framework 是 **Spring 全家桶底层基石**，核心解决 3 大问题：
1. **IOC 控制反转**：对象交给容器管理，不用手动 `new`
2. **DI 依赖注入**：自动装配类与类之间的依赖
3. **AOP 面向切面**：无侵入增强业务（日志、事务、权限）
附加能力：事务管理、资源管理、事件机制、工具封装。

> Spring Boot 只是对它做了**自动配置+starter依赖**，底层完全依赖 Spring Framework。

---

## 二、核心五大模块（必学）
1. **Core & Beans**：IOC容器、Bean工厂、DI注入（核心中的核心）
2. **Context**：容器扩展、国际化、事件、资源加载
3. **AOP**：切面编程、动态代理
4. **JDBC/ORM**：数据库事务、JDBC 模板、整合MyBatis/JPA
5. **Web**：SpringMVC 底层支撑

---

## 三、核心概念极简理解
### 1. IOC 控制反转
- 传统开发：程序员手动 `new 对象` → 程序员控制对象创建
- Spring IOC：**Spring 容器创建、管理、销毁对象** → 控制权反转给框架
- 容器中管理的对象统称：**Bean**

### 2. DI 依赖注入
容器自动把需要的依赖对象注入到当前类，解耦代码，无需硬编码 `new`。
三种注入方式：
- 构造器注入（官方推荐）
- Setter 注入
- 字段注入（@Autowired，开发常用）

### 3. Bean 作用域
- `singleton`：单例（默认，整个容器只一个）
- `prototype`：多例（每次获取新建）
- request / session / application：web 专属

### 4. AOP 面向切面
**不修改原有业务代码**，统一增强公共逻辑：
- 适用场景：全局日志、接口耗时统计、事务控制、权限校验、限流
- 底层：JDK动态代理（接口） / CGLIB代理（类）

### 5. 事务管理
Spring 统一声明式事务，通过注解 `@Transactional` 一键开启，保证数据一致性。

---

## 四、快速上手代码（纯Spring 原生，无Boot）
### 步骤1：导入核心Maven依赖
```xml
<dependencies>
    <!-- Spring 核心容器 -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
        <version>5.3.30</version>
    </dependency>
</dependencies>
```

### 步骤2：普通业务类
```java
public class UserService {
    public void saveUser() {
        System.out.println("执行用户保存业务");
    }
}
```

### 步骤3：Spring 配置类（注解开发，替代xml）
```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringConfig {
    // 将UserService交给IOC容器管理，成为Bean
    @Bean
    public UserService userService(){
        return new UserService();
    }
}
```

### 步骤4：启动IOC容器，获取Bean
```java
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class MainTest {
    public static void main(String[] args) {
        // 加载配置类，初始化IOC容器
        AnnotationConfigApplicationContext context 
            = new AnnotationConfigApplicationContext(SpringConfig.class);
        
        // 从容器中获取Bean
        UserService userService = context.getBean(UserService.class);
        userService.saveUser();
        
        // 关闭容器
        context.close();
    }
}
```
运行结果：
```
执行用户保存业务
```

---

## 五、开发常用注解（必背）
### 1. IOC 容器注解
- `@Configuration`：标记配置类
- `@Bean`：手动注册Bean
- `@Component / @Service / @Controller / @Repository`：
  批量扫描，自动将类交给容器管理
- `@ComponentScan`：开启包扫描

### 2. DI 依赖注入
- `@Autowired`：自动装配（byType）
- `@Qualifier`：配合Autowired，按名称注入
- `@Value`：注入普通变量、配置文件内容

### 3. AOP 核心注解
- `@Aspect`：标记切面类
- `@Before / @After / @Around`：切面通知
- `@Pointcut`：定义切点（拦截哪些方法）

### 4. 事务
- `@Transactional`：声明式事务

---

## 六、Bean 完整生命周期
1. 实例化（创建对象）
2. 依赖注入（赋值）
3. 初始化（InitializingBean / @PostConstruct）
4. 正常使用
5. 销毁（DisposableBean / @PreDestroy）

---

## 七、AOP 最简实战示例
1. 切面类
```java
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LogAspect {
    @Around("execution(* com.service.*.*(..))")
    public Object around(org.aspectj.lang.ProceedingJoinPoint pjp) throws Throwable {
        System.out.println("方法执行前：记录日志");
        Object result = pjp.proceed();
        System.out.println("方法执行后：记录日志");
        return result;
    }
}
```
无需修改业务代码，自动拦截所有 Service 方法，完成日志增强。

---

## 八、学习总结 + 企业重点
1. **必掌握**
   - IOC容器、Bean管理、三种依赖注入
   - 包扫描、常用注解
   - AOP核心概念+环绕通知
   - Spring 声明式事务
2. **底层理解**
   - 工厂模式 + 反射 实现Bean创建
   - 动态代理实现AOP
3. **衔接后续**
   Spring Boot = SpringFramework + 自动配置 + 内置web容器，学完本节无缝过渡Boot。

---

