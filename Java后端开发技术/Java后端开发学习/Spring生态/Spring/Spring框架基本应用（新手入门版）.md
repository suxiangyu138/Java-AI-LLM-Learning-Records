# Spring 框架基本应用（新手入门版）

> **文档定位**：Java 后端企业级技术文档 | Spring 框架入门  
> **核心特性**：IOC（控制反转）+ AOP（面向切面编程）  
> **前置基础**：Java 基础、面向对象

---

## 一、核心概念

### 1.1 Spring 框架定位

Spring 是 Java EE 领域的轻量级开源框架，核心定位是 **简化开发、解耦依赖**，通过 IOC 和 AOP 两大核心特性，解决传统 Java 开发中代码耦合度高、维护困难的问题。

### 1.2 三大核心概念

#### IOC（控制反转）

传统开发中对象由开发者手动 `new` 创建。Spring IOC 将 **对象的创建、依赖注入、生命周期管理** 全部交给 Spring 容器，开发者只需通过配置告诉 Spring"需要什么对象"。

> 简单理解：以前"开发者管对象"，现在"Spring 管对象"，开发者只需"用对象"。

#### DI（依赖注入）

DI 是 IOC 的具体实现方式，指 Spring 容器在创建对象时，**自动将依赖对象注入** 到当前对象中。

| 注入方式 | 说明 | 适用场景 |
|----------|------|----------|
| **构造方法注入** | 通过构造方法传入依赖 | 强制依赖（依赖必须存在） |
| **setter 方法注入** | 通过 setter 方法注入依赖 | 可选依赖 |
| **注解注入**（`@Autowired`） | 通过注解自动注入 | **最常用**，简化配置 |

#### Bean

Spring 容器中管理的所有对象都称为 Bean。由 Spring 容器创建和管理，生命周期由 Spring 控制（创建 → 初始化 → 销毁）。

---

## 二、底层原理

### 2.1 Spring IOC 容器工作流程

```
1. 读取配置（XML / 注解）→ 2. 解析 Bean 定义 → 3. 创建 Bean 实例
    → 4. 注入依赖（DI）→ 5. 初始化 Bean → 6. 存入容器 → 7. 提供获取
```

### 2.2 核心依赖

| 依赖 | 作用 | Maven 坐标 |
|------|------|------------|
| `spring-core` | IOC/DI 核心实现 | `org.springframework:spring-core` |
| `spring-beans` | Bean 创建和管理 | `org.springframework:spring-beans` |
| `spring-context` | 容器初始化和 Bean 装配 | `org.springframework:spring-context` |
| `spring-expression` | 表达式语言支持 | `org.springframework:spring-expression` |

---

## 三、代码实现

### 3.1 项目环境

- JDK 8+（推荐 JDK 8）
- Spring 5.x
- IntelliJ IDEA

#### Maven 依赖

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>5.3.28</version>
</dependency>
```

### 3.2 定义 Bean

```java
/** 数据访问层 — 模拟查询 */
public class UserDao {
    public void queryUser() {
        System.out.println("查询用户信息：id=1，name=张三");
    }
}

/** 业务逻辑层 — 依赖 UserDao */
public class UserService {
    private UserDao userDao;

    /** setter 方法注入：供 Spring 注入 UserDao */
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    /** 业务方法 */
    public void getUserInfo() {
        userDao.queryUser();
    }
}
```

### 3.3 方式 1：XML 配置

```xml
<!-- applicationContext.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
           http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!-- 配置 UserDao Bean -->
    <bean id="userDao" class="com.example.dao.UserDao"/>

    <!-- 配置 UserService Bean，注入 userDao -->
    <bean id="userService" class="com.example.service.UserService">
        <property name="userDao" ref="userDao"/>
    </bean>

</beans>
```

### 3.4 方式 2：注解配置（推荐）

```java
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

/** 数据访问层 */
@Component  // 标识为 Spring Bean
public class UserDao {
    public void queryUser() {
        System.out.println("查询用户信息：id=1，name=张三");
    }
}

/** 业务逻辑层 */
@Component  // 标识为 Spring Bean
public class UserService {
    @Autowired  // 自动注入 UserDao
    private UserDao userDao;

    public void getUserInfo() {
        userDao.queryUser();
    }
}
```

```xml
<!-- applicationContext.xml — 注解扫描 -->
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
           http://www.springframework.org/schema/beans/spring-beans.xsd
           http://www.springframework.org/schema/context
           http://www.springframework.org/schema/context/spring-context.xsd">

    <!-- 扫描 com.example 包下所有 Spring 注解的类 -->
    <context:component-scan base-package="com.example"/>

</beans>
```

### 3.5 获取容器并使用 Bean

```java
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/** Spring 容器启动与 Bean 获取示例 */
public class TestSpring {
    public static void main(String[] args) {
        // 1. 初始化 Spring 容器（加载配置文件）
        ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");

        // 2. 从容器获取 Bean（按 id）
        UserService userService = (UserService) context.getBean("userService");

        // 3. 使用 Bean（UserDao 已被 Spring 自动注入）
        userService.getUserInfo();
    }
}
```

### 3.6 常用注解速查

| 注解 | 作用 |
|------|------|
| `@Component` | 标记普通 Bean |
| `@Service` | 标记业务层 Bean（语义同 @Component） |
| `@Repository` | 标记数据访问层 Bean |
| `@Autowired` | 按类型自动注入依赖 |
| `@Resource` | 按名称自动注入依赖 |
| `@Qualifier` | 配合 @Autowired 按名称指定 Bean |

---

## 四、实战要点

### 4.1 核心流程

```
定义 Bean → 配置 Bean（XML/注解） → 获取 Spring 容器 → 获取 Bean → 使用
```

### 4.2 注入方式选型

- **强制依赖**（如 Service 必须依赖 Dao）→ 构造方法注入
- **可选依赖** → setter 方法注入
- **日常开发** → `@Autowired` 注解注入（最简洁）

---

## 五、避坑总结

| 坑点 | 原因 | 解决方案 |
|------|------|----------|
| **`@Autowired` 注入为 null** | 类未被 Spring 扫描到 | 确保类在 `component-scan` 扫描路径下 |
| **Bean 未找到异常** | Bean id 拼写错误或未配置 | 检查 XML `<bean id>` 与 `getBean()` 参数一致 |
| **依赖未注入** | 忘记在 XML 中配置 `<property>` 或未加 `@Autowired` | 检查配置完整性 |
| **配置加载失败** | XML 文件名或路径错误 | 确认 `applicationContext.xml` 在 resources 目录下 |

---

## 六、企业级最佳实践

### 6.1 开发规范

| 规范 | 说明 |
|------|------|
| **注解优先** | 日常开发优先使用 `@Component` + `@Autowired` 注解，简洁高效 |
| **语义化注解** | Service 用 `@Service`，Dao 用 `@Repository`，Controller 用 `@Controller` |
| **接口声明** | 注入时用接口类型声明（`private UserService userService`），面向接口编程 |
| **构造器注入** | Spring 4.3+ 推荐构造器注入（确保依赖不可变 + 便于单元测试） |

### 6.2 学习路线

1. Spring IOC 容器 → 2. Bean 生命周期 → 3. AOP 切面编程 → 4. Spring MVC → 5. Spring Boot → 6. Spring Cloud
