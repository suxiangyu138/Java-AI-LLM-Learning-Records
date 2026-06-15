# Spring Bean 的装配模式（新手易懂，结合实操）

> **文档定位**：Java 后端企业级技术文档 | Spring Bean 装配  
> **前置基础**：Spring IOC/DI 概念、Spring 基本应用

---

## 一、核心概念

### 1.1 Bean 装配的定义

Bean 装配是 **Spring 容器创建 Bean 并注入依赖的具体方式**，是 IOC 的落地实现。所有被装配的 Bean 必须被 Spring 容器识别。

### 1.2 三大装配模式

| 模式 | 配置方式 | 推荐度 |
|------|----------|--------|
| **手动装配** | XML `<constructor-arg>` / `<property>` | ★★（理解底层原理用） |
| **自动装配** | XML `autowire` 属性 | ★★★（简化 XML） |
| **注解装配**（推荐） | `@Autowired` / `@Resource` | ★★★★★（**企业开发主流**） |

---

## 二、底层原理

### 2.1 装配流程

```
1. Spring 扫描 Bean 定义（XML / 注解）
2. 解析依赖关系（构造方法参数 / setter / @Autowired 字段）
3. 按依赖顺序创建 Bean 实例
4. 注入依赖 → 放入容器 → 对外提供
```

### 2.2 `@Autowired` vs `@Resource`

| 维度 | `@Autowired` | `@Resource` |
|------|-------------|------------|
| 默认策略 | 按 **类型**（byType） | 按 **名称**（byName）→ 类型（byType） |
| 来源 | Spring 提供 | JDK 提供（`javax.annotation`） |
| 多 Bean 冲突 | + `@Qualifier` | `@Resource(name = "xxx")` |

---

## 三、代码实现

### 3.1 构造方法装配（强制依赖）

```java
public class UserService {
    private UserDao userDao;

    /** 构造方法注入（依赖必须存在） */
    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public void getUserInfo() {
        userDao.queryUser();
    }
}
```

```xml
<bean id="userDao" class="com.example.dao.UserDao"/>
<bean id="userService" class="com.example.service.UserService">
    <constructor-arg ref="userDao"/>
</bean>
```

### 3.2 Setter 方法装配（可选依赖）

```xml
<bean id="userDao" class="com.example.dao.UserDao"/>
<bean id="userService" class="com.example.service.UserService">
    <property name="userDao" ref="userDao"/>
</bean>
```

> setter 方法命名规则：`set` + 属性名首字母大写（如 `setUserDao`）

### 3.3 自动装配（XML）

| 模式 | 规则 |
|------|------|
| `byName` | 依赖属性名 = Bean 的 `id`，区分大小写 |
| `byType` | 按属性类型匹配，容器中只能有一个同类型 Bean |
| `constructor` | 按构造方法参数类型自动注入 |

```xml
<bean id="userDao" class="com.example.dao.UserDao"/>
<bean id="userService" class="com.example.service.UserService" autowire="byName"/>
```

### 3.4 注解装配（推荐，企业主流）

```java
/** 标识 Bean */
@Repository
public class UserDao {
    public void queryUser() {
        System.out.println("查询用户信息");
    }
}

/** 自动注入依赖 */
@Service
public class UserService {
    @Autowired  // 按类型注入
    private UserDao userDao;

    public void getUserInfo() {
        userDao.queryUser();
    }
}
```

```xml
<!-- 注解扫描（必须配置） -->
<context:component-scan base-package="com.example"/>
```

**Bean 标识注解速查**：

| 注解 | 用途 |
|------|------|
| `@Component` | 通用 Bean |
| `@Service` | 业务层 Bean |
| `@Repository` | 数据访问层 Bean |
| `@Controller` | 控制层 Bean |

---

## 四、实战要点

### 4.1 装配模式选型

| 场景 | 推荐模式 |
|------|----------|
| 日常开发 | `@Autowired` + `@Service`/`@Repository` |
| 强制依赖 | 构造方法注入 |
| 可选依赖 | setter 方法注入 |
| 多实现类 | `@Autowired` + `@Qualifier("beanId")` |

---

## 五、避坑总结

| 坑点 | 原因 | 解决方案 |
|------|------|----------|
| **`byType` 报 NoUniqueBeanDefinition** | 同类型有多个 Bean | 改用 `byName` 或 `@Qualifier` |
| **`byName` 装配失败** | 属性名与 Bean id 不一致（大小写） | 确保完全一致 |
| **`@Autowired` 注入 null** | 类未被扫描（不在 component-scan 路径下） | 检查包路径 |
| **setter 未规范命名** | 未遵循 `setXxx` 规范 | 确保 setter 命名规范 |

---

## 六、企业级最佳实践

- **注解装配优先**：`@Autowired` + 构造方法注入（Spring 官方推荐）
- **面向接口编程**：注入时声明接口类型，不直接依赖实现类
- **构造方法注入用于强制依赖**，setter 用于可选依赖
- **多实现类用 `@Qualifier`** 明确指定 Bean 名称
