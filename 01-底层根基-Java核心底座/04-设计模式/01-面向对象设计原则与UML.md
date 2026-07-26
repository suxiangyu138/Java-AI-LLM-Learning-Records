# 面向对象设计原则与UML类图

> 面向对象设计的"内功心法"——SOLID原则是所有设计模式的底层规范，UML类图是架构设计的通用语言，二者共同构成设计模式的根基。

## 目录

1. [设计原则概述](#1-设计原则概述)
2. [SOLID五大核心原则](#2-solid五大核心原则)
3. [两大补充原则](#3-两大补充原则)
4. [UML类图基础](#4-uml类图基础)
5. [六种类间关系](#5-六种类间关系)
6. [实战规范与面试考点](#6-实战规范与面试考点)

---

## 1. 设计原则概述

### 1.1 设计原则与设计模式的关系

```
面向对象设计原则（SOLID + 补充）→ 设计模式（23种）→ 框架底层设计
     ↑ 内功心法              ↑ 具体招式           ↑ 实践应用
```

### 1.2 核心目标

降低耦合度 → 提高复用性 → 增强扩展性与可维护性 → 减少需求迭代改造成本

---

## 2. SOLID五大核心原则

### S - 单一职责原则（Single Responsibility Principle）

**一个类只做一件事。** 职责分离、粒度细化，避免"万能类"。

```java
// 违反SRP：一个类承担了多个职责
public class UserService {
    public void saveUser(User user) {
        // 既要拼接SQL
        String sql = "INSERT INTO user VALUES (...)";
        // 又要执行数据库操作
        jdbcTemplate.execute(sql);
        // 还要发邮件通知
        sendEmail(user.getEmail(), "注册成功");
    }
}

// 符合SRP：拆分职责
public class UserService {
    private UserRepository userRepository;
    private NotificationService notificationService;

    public void register(User user) {
        userRepository.save(user);
        notificationService.sendWelcomeEmail(user.getEmail());
    }
}
```

### O - 开放封闭原则（Open Closed Principle）

**对扩展开放，对修改关闭。** 新增功能通过扩展实现，不修改已有代码。

```java
// 违反OCP：每次新增支付方式都要修改switch
public class PaymentService {
    public void pay(String type, double amount) {
        switch (type) {
            case "alipay": /* 支付宝逻辑 */ break;
            case "wechat": /* 微信逻辑 */ break;
            // 新增银行卡支付：必须修改此处
        }
    }
}

// 符合OCP：通过策略模式扩展
public interface PaymentStrategy {
    void pay(double amount);
}

public class AlipayStrategy implements PaymentStrategy { /* 支付宝逻辑 */ }
public class WechatStrategy implements PaymentStrategy { /* 微信逻辑 */ }
// 新增银行卡支付：新增类即可，无需修改原有代码
```

### L - 里氏替换原则（Liskov Substitution Principle）

**子类可替换父类，不破坏原有逻辑。** 子类可扩展，不重写父类核心方法。

```java
// 经典反例：正方形继承矩形
public class Rectangle {
    private int width, height;
    public void setWidth(int w) { this.width = w; }
    public void setHeight(int h) { this.height = h; }
    public int getArea() { return width * height; }
}

public class Square extends Rectangle {
    @Override
    public void setWidth(int w) {
        super.setWidth(w);
        super.setHeight(w); // 强行让高等于宽
    }
    @Override
    public void setHeight(int h) {
        super.setHeight(h);
        super.setWidth(h);
    }
}

// 客户端代码期望的是矩形行为，但Square破坏了LSP
Rectangle r = new Square();
r.setWidth(5);
r.setHeight(4);
// 期望面积20，实际得到16 —— 违反LSP
```

### I - 接口隔离原则（Interface Segregation Principle）

**不依赖不需要的接口。** 大接口拆分为多个专用小接口。

```java
// 违反ISP：胖接口
public interface Worker {
    void work();
    void eat();
    void sleep();
}
public class Robot implements Worker {
    public void work() { /* 干活 */ }
    public void eat() { throw new UnsupportedOperationException("机器人不用吃饭"); }
    public void sleep() { throw new UnsupportedOperationException("机器人不用睡觉"); }
}

// 符合ISP：拆分接口
public interface Workable { void work(); }
public interface Eatable { void eat(); }
public interface Sleepable { void sleep(); }
public class Robot implements Workable {
    public void work() { /* 干活 */ }
}
```

### D - 依赖倒置原则（Dependency Inversion Principle）

**依赖抽象而非具体实现。** 面向接口编程。

```java
// 违反DIP：高层依赖低层具体实现
public class NotificationService {
    private EmailSender emailSender = new EmailSender(); // 直接依赖具体类
    public void send(String msg) { emailSender.send(msg); }
}

// 符合DIP：依赖抽象
public interface MessageSender { void send(String msg); }
public class EmailSender implements MessageSender { /* ... */ }
public class SmsSender implements MessageSender { /* ... */ }
public class NotificationService {
    private MessageSender sender; // 依赖抽象
    public NotificationService(MessageSender sender) {
        this.sender = sender;
    }
    public void send(String msg) { sender.send(msg); }
}
```

### SOLID落地技巧

| 原则 | 落地技巧 |
|------|----------|
| **SRP** | 类名能准确描述其职责，方法不超过50行 |
| **OCP** | 用策略模式/模板方法替代if-else |
| **LSP** | 子类只扩展不修改，继承前先问"is-a"是否成立 |
| **ISP** | 接口方法数控制在5个以内 |
| **DIP** | 成员变量声明为接口类型，构造函数注入 |

---

## 3. 两大补充原则

| 原则 | 定义 | 说明 |
|------|------|------|
| **迪米特法则**（最少知道原则） | 只与直接朋友通信 | 一个对象应尽量少地了解其他对象，减少类间交互 |
| **合成复用原则** | 优先组合/聚合，而非继承 | 组合更灵活，不破坏封装，降低耦合 |

### 迪米特法则示例

```java
// 违反LoD：直接获取了内部对象并调用其方法
public class Boss {
    public void check(Company company) {
        company.getDepartment().getManager().report(); // 和"陌生人"交互
    }
}

// 符合LoD：由Company提供封装的方法
public class Company {
    private Department department;
    public void showManagerReport() {
        department.getManager().report();
    }
}
public class Boss {
    public void check(Company company) {
        company.showManagerReport(); // 只和直接朋友Company交互
    }
}
```

---

## 4. UML类图基础

### 4.1 类的UML表示

UML类图中，类用矩形表示，分为3层：

- **第一层**：类名（居中），普通类直接写类名；抽象类斜体表示；接口用`<<interface>>`标识
- **第二层**：属性（成员变量），格式：`可见性 属性名 : 数据类型 [= 默认值]`
- **第三层**：方法（成员方法），格式：`可见性 方法名(参数列表) : 返回值类型`

**可见性符号**：`+` public、`#` protected、`-` private、`~` package-private

```java
// Java实体类
public class User {
    private Long id;
    public String username;
    protected Integer age;

    public void login(String password) {}
    private String getUsername() { return username; }
}
```

对应UML类表示：

```
┌─────────────────────────┐
│          User           │
├─────────────────────────┤
│ - id : Long             │
│ + username : String     │
│ # age : Integer         │
├─────────────────────────┤
│ + login(password: String) : void │
│ - getUsername() : String         │
└─────────────────────────┘
```

### 4.2 接口的UML表示

```java
public interface UserService {
    void addUser(User user);
    User getUserById(Long id);
}
```

UML表示：

```
┌─────────────────────────────┐
│   <<interface>>             │
│     UserService             │
├─────────────────────────────┤
│ + addUser(user: User) : void│
│ + getUserById(id: Long) : User │
└─────────────────────────────┘
```

---

## 5. 六种类间关系

| 关系 | 含义 | UML表示 | Java对应 |
|------|------|---------|----------|
| **继承** | 子类继承父类 | 空心三角+实线 | `extends` |
| **实现** | 类实现接口 | 空心三角+虚线 | `implements` |
| **关联** | 固定的成员变量关系 | 实线箭头 | 成员变量 |
| **聚合** | 整体-部分，部分可独立存在 | 空心菱形+实线 | 成员变量，部分可独立创建 |
| **组合** | 整体-部分，部分不可独立存在 | 实心菱形+实线 | 构造中创建，同生命周期 |
| **依赖** | 临时使用关系 | 虚线箭头 | 方法参数/局部变量 |

### 5.1 继承关系（Generalization）

```
        ┌──────────┐
        │ Animal   │
        └────┬─────┘
             ▲
             │  extends
        ┌────┴─────┐
        │  Dog     │
        └──────────┘
```

### 5.2 实现关系（Realization）

```
  ┌──────────────┐
  │ <<interface>>│
  │   Runnable   │
  └──────┬───────┘
         ▲
         ║  implements
    ┌────┴───────┐
    │  Thread    │
    └────────────┘
```

### 5.3 关联关系（Association）

```
  ┌───────┐          ┌───────────┐
  │ Order │──────────▶│   User   │
  └───────┘  关联     └───────────┘
```

### 5.4 聚合关系（Aggregation）—— 弱关联

```
  ┌────────┐        ┌──────────┐
  │  Team  │◇───────│  Member  │
  └────────┘  聚合   └──────────┘
```

> 成员可脱离团队独立存在（如转组），聚合是"弱依赖"。

### 5.5 组合关系（Composition）—— 强关联

```
  ┌──────┐        ┌───────────┐
  │ User │◆───────│  Address  │
  └──────┘  组合   └───────────┘
```

> 地址无法脱离用户存在（用户销毁，地址也销毁），组合是"强依赖、同生命周期"。

### 5.6 依赖关系（Dependency）—— 临时使用

```
  ┌───────────┐     ┌──────────────┐
  │UserService│····▶│ UserDao      │
  └───────────┘ 依赖 └──────────────┘
```

> 仅方法参数、局部变量中临时使用，是最弱的类间关系。

---

## 6. 实战规范与面试考点

### 实战规范

| 规范 | 说明 |
|------|------|
| **命名一致** | 类名、方法名与Java代码完全一致，避免缩写 |
| **标注可见性** | 必须标注`+`/`-`/`#`，禁止省略 |
| **优先单向关联** | 避免双向关联，降低耦合 |
| **聚合vs组合区分** | 严格区分，不可混淆 |
| **实战简化** | 仅标注核心属性和方法，避免类图臃肿 |

### 面试高频考点

1. 区分6种类间关系（重点：聚合vs组合、依赖vs关联），说明Java中的实现方式
2. 绘制指定设计模式的UML类图，标注核心角色和关系
3. 说明抽象类、接口的UML表示方式与Java代码的对应关系
4. SOLID五大原则的定义、反例和修正方法

### 避坑总结

| 坑点 | 正确做法 |
|------|----------|
| **继承滥用** | 优先用组合而非继承，避免继承层级过深 |
| **接口过胖** | 一个接口10+个方法时应当拆分 |
| **违反OCP** | 每新增功能都修改核心类时，重构为扩展模式 |
