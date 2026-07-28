# 02 OOP 编程规约

> OOP 规约的核心是"契约精神"——equals 和 hashCode 必须同时重写，构造器只做初始化，POJO 分层让职责清晰

---

## 📚 目录

1. [equals / hashCode / toString 契约](#1-equals--hashcode--tostring-契约)
2. [构造器规范](#2-构造器规范)
3. [静态工厂方法](#3-静态工厂方法)
4. [POJO 分层对象](#4-pojo-分层对象)
5. [接口设计原则](#5-接口设计原则)
6. [继承与组合](#6-继承与组合)
7. [常见反模式](#7-常见反模式)
8. [动手练习](#8-动手练习)

---

## 1. equals / hashCode / toString 契约

### 1.1 equals 四大特性

```java
// 1. 自反性：x.equals(x) 必须为 true
// 2. 对称性：x.equals(y) == y.equals(x)
// 3. 传递性：x.equals(y) && y.equals(z) → x.equals(z)
// 4. 一致性：多次调用结果一致（对象未修改时）
```

### 1.2 标准重写模板

```java
public class User {
    private Long id;
    private String name;
    private Integer age;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) &&
               Objects.equals(name, user.name) &&
               Objects.equals(age, user.age);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, age);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
```

### 1.3 getClass vs instanceof

| 方式 | 优点 | 缺点 | 适用场景 |
|------|------|------|---------|
| `getClass()` | 严格类型匹配 | 子类不等于父类 | 非继承体系（推荐） |
| `instanceof` | 支持继承 | 可能违反对称性 | 需要支持子类比较 |

> 🎯 **阿里手册推荐**：使用 `getClass()`，除非明确需要支持继承体系比较。

### 1.4 Lombok 等价写法

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private Long id;
    private String name;
    private Integer age;
}
```

> 💡 `@Data` 自动生成 equals/hashCode/toString/getter/setter，与手写模板等价。

---

## 2. 构造器规范

### 2.1 构造器只做初始化

```java
// ❌ 反例：构造器里放业务逻辑
public UserService(UserRepository repo) {
    this.repo = repo;
    this.cache = loadCache();  // ❌ 业务逻辑
    this.mq = connectMQ();     // ❌ 外部依赖
}

// ✅ 正确：构造器只做字段赋值
public UserService(UserRepository repo) {
    this.repo = repo;
}

public void init() {
    this.cache = loadCache();
    this.mq = connectMQ();
}
```

### 2.2 构造器链式调用

```java
public class User {
    private Long id;
    private String name;
    private Integer age;

    public User() {
        this(null, null, null);
    }

    public User(Long id) {
        this(id, null, null);
    }

    public User(Long id, String name, Integer age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }
}
```

### 2.3 私有构造器防实例化

```java
// 工具类禁止实例化
public class StringUtils {
    private StringUtils() {
        throw new AssertionError("工具类不能实例化");
    }
    
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
```

---

## 3. 静态工厂方法

### 3.1 优势对比

| 优势 | 构造器 | 静态工厂 |
|------|--------|---------|
| 有名字 | ❌ | ✅ `List.of()` 比 `new ArrayList<>()` 清晰 |
| 可以缓存 | ❌ 每次新建 | ✅ `Integer.valueOf()` 缓存 |
| 返回子类型 | ❌ | ✅ 可返回接口实现 |
| 泛型推断 | ❌ | ✅ `List.of()` 自动推断 |

### 3.2 命名约定

```java
// of：多个参数聚合
List<String> list = List.of("a", "b", "c");

// valueOf：类型转换
Integer num = Integer.valueOf("123");

// from：从其他对象转换
LocalDate date = LocalDate.from(temporal);

// getInstance：获取单例或缓存
Calendar cal = Calendar.getInstance();

// newInstance：每次返回新实例
ArrayList<String> list = ArrayList.newInstance();

// create：创建对象
UserService service = UserServiceFactory.create();
```

---

## 4. POJO 分层对象

### 4.1 分层定义

| 类型 | 全称 | 用途 | 示例 |
|------|------|------|------|
| DO | Data Object | 数据库表映射 | `UserDO` |
| DTO | Data Transfer Object | 服务间传输 | `UserDTO` |
| VO | View Object | 前端展示 | `UserVO` |
| BO | Business Object | 业务逻辑封装 | `UserBO` |
| AO | Application Object | 应用层复用 | `UserAO` |
| Query | Query Object | 查询条件封装 | `UserQuery` |

### 4.2 分层示例

```java
// DO：数据库映射
@TableName("user")
public class UserDO {
    private Long id;
    private String name;
    private Integer age;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}

// DTO：服务间传输
public class UserDTO {
    private Long id;
    private String name;
    private Integer age;
}

// VO：前端展示
public class UserVO {
    private Long id;
    private String name;
    private String ageDesc;  // "18岁"
}

// Query：查询条件
public class UserQuery {
    private String name;
    private Integer minAge;
    private Integer maxAge;
    private Integer pageNum;
    private Integer pageSize;
}
```

### 4.3 分层转换

```java
// 使用 MapStruct
@Mapper
public interface UserConverter {
    UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);
    
    UserDTO toDTO(UserDO userDO);
    UserVO toVO(UserDTO userDTO);
}

// 使用 BeanUtils（Spring）
UserDTO dto = new UserDTO();
BeanUtils.copyProperties(userDO, dto);
```

> ⚠️ **禁止**：DO 直接返回给前端（泄露数据库结构）、VO 直接传给 DAO 层（职责混乱）。

---

## 5. 接口设计原则

### 5.1 接口只放抽象方法和常量

```java
// ✅ 正确
public interface UserRepository {
    User findById(Long id);
    void save(User user);
}

// ❌ 错误：接口里放实现细节
public interface UserRepository {
    default User findById(Long id) {
        // ❌ 具体实现不应该在接口里
        String sql = "SELECT * FROM user WHERE id = ?";
        // ...
    }
}
```

### 5.2 接口 vs 抽象类

| 维度 | 接口 | 抽象类 |
|------|------|--------|
| 多继承 | ✅ 可实现多个 | ❌ 只能单继承 |
| 字段 | ❌ 只能常量 | ✅ 可有实例字段 |
| 构造器 | ❌ | ✅ |
| 方法实现 | default 方法 | 普通方法 |
| 适用场景 | 定义能力/契约 | 代码复用/模板 |

### 5.3 接口演进策略

```java
// Java 8+ 使用 default 方法向后兼容
public interface UserRepository {
    User findById(Long id);
    
    // 新增方法不破坏已有实现
    default List<User> findAll() {
        throw new UnsupportedOperationException("暂未实现");
    }
}
```

---

## 6. 继承与组合

### 6.1 组合优先于继承

```java
// ❌ 继承导致脆弱基类
public class ArrayListStack<E> extends ArrayList<E> {
    public void push(E e) {
        add(e);
    }
    
    public E pop() {
        return remove(size() - 1);
    }
}
// 问题：ArrayList 的实现变更可能影响 Stack

// ✅ 组合更安全
public class Stack<E> {
    private final List<E> list = new ArrayList<>();
    
    public void push(E e) {
        list.add(e);
    }
    
    public E pop() {
        return list.remove(list.size() - 1);
    }
}
```

### 6.2 is-a 关系判断

```java
// ✅ 正确：Dog is-a Animal
public class Dog extends Animal { }

// ❌ 错误：Stack is-not-a ArrayList
public class Stack extends ArrayList { }  // 应该用组合
```

---

## 7. 常见反模式

| 反模式 | 问题 | 正确做法 |
|--------|------|---------|
| equals 只重写不重写 hashCode | HashMap/HashSet 行为异常 | 同时重写 |
| 构造器里调用可覆写方法 | 子类覆写导致未初始化完成 | 私有方法或 final 方法 |
| POJO 里放业务逻辑 | 职责混乱 | 业务逻辑放 Service |
| 接口里放实现细节 | 接口污染 | 实现放实现类 |
| 滥用继承 | 脆弱基类问题 | 组合优先 |
| DO 直接返回前端 | 泄露数据库结构 | 转 VO |

---

## 8. 动手练习

### 练习 1：重写 equals/hashCode

```java
public class Order {
    private String orderNo;
    private Long userId;
    private BigDecimal amount;
    // TODO: 重写 equals/hashCode/toString
}
```

<details>
<summary>参考答案</summary>

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Order order = (Order) o;
    return Objects.equals(orderNo, order.orderNo) &&
           Objects.equals(userId, order.userId) &&
           Objects.equals(amount, order.amount);
}

@Override
public int hashCode() {
    return Objects.hash(orderNo, userId, amount);
}

@Override
public String toString() {
    return "Order{" +
            "orderNo='" + orderNo + '\'' +
            ", userId=" + userId +
            ", amount=" + amount +
            '}';
}
```

</details>

---

**上一模块**：[01-命名与格式规范](./01-命名与格式规范.md)  
**下一模块**：[03-集合处理规范](./03-集合处理规范.md)  
**返回总览**：[00-Java代码规范知识体系总览](./00-Java代码规范知识体系总览.md)
