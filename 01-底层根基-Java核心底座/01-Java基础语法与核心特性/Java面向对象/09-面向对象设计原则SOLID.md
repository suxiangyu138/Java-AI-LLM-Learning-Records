# 09 面向对象设计原则与 SOLID

> 三大特性是语法，SOLID 是纪律——把"怎么组织类"从经验变成可检查的规则，是迈向架构设计的第一步

---

## 📚 目录

1. [设计原则总览](#1-设计原则总览)
2. [S：单一职责原则 SRP](#2-s单一职责原则-srp)
3. [O：开闭原则 OCP](#3-o开闭原则-ocp)
4. [L：里氏替换原则 LSP](#4-l里氏替换原则-lsp)
5. [I：接口隔离原则 ISP](#5-i接口隔离原则-isp)
6. [D：依赖倒置原则 DIP](#6-d依赖倒置原则-dip)
7. [组合优于继承与最少知识原则](#7-组合优于继承与最少知识原则)
8. [常见设计模式速览](#8-常见设计模式速览)
9. [贫血模型 vs 充血模型](#9-贫血模型-vs-充血模型)

---

## 1. 设计原则总览

| 原则 | 一句话 | 违反的代价 | 落地手段 |
|------|--------|-----------|---------|
| **S**RP 单一职责 | 一个类只有一个变化理由 | 改 A 功能连带 B 功能回归 | 类按职责拆、方法按意图拆 |
| **O**CP 开闭 | 对扩展开放、对修改关闭 | 加需求就改旧代码，牵一发动全身 | 接口/多态/策略/模板方法 |
| **L**SP 里氏替换 | 子类必须能替换父类而不破坏程序 | 父类引用处行为异常，多态失效 | 不改变父类契约（前置/后置条件） |
| **I**SP 接口隔离 | 客户端不应依赖它用不到的接口 | 接口庞大，实现类被迫实现空方法 | 细粒度接口 + 组合 |
| **D**IP 依赖倒置 | 依赖抽象，不依赖具体实现 | 高层依赖底层细节，改动传导 | 接口注入、依赖注入容器 |

> 🎯 **核心要点**：SOLID 是"如何应对变化"的五个可检查视角——S 管拆分、O 管扩展、L 管替换、I 管接口尺寸、D 管依赖方向。

---

## 2. S：单一职责原则 SRP

**定义**：一个类应当只有一个引起它变化的原因（职责）。"职责"是变化的维度——**一个类只有一个变化理由**。

**反例**（一个类承担了三种变化）：

```java
public class UserService {
    public void createUser(User u) { /* 校验 + 落库 */ }
    public void sendWelcomeEmail(User u) { /* 发邮件 */ }
    public void exportUsers() { /* 导出 Excel */ }
    // 变化理由 1：用户规则改 → 2：邮件模板改 → 3：导出格式改
    // 任何一个变化都要改这个类、重新测试整个类
}
```

**正例**（按职责拆分，三个类各管一个变化方向）：

```java
public class UserService {                // 职责：用户业务
    private final UserRepository repository;
    private final EmailNotifier notifier;
    private final UserExporter exporter;

    public void createUser(User u) {
        repository.save(u);
        notifier.sendWelcome(u);
    }
}

public class EmailNotifier { }            // 职责：通知
public class UserExporter { }             // 职责：导出
```

> 💡 判断方法：给类写"这个类的变化理由是……"，能写出两个以上 → 拆。注意 SRP 不是"类越小越好"，过细会导致类爆炸，粒度到"内聚的职责"即可。

---

## 3. O：开闭原则 OCP

**定义**：软件实体应对扩展开放、对修改关闭——**新增功能通过新增代码实现，而不是修改已有代码**。

**反例**（改开关）：

```java
public class Discount {
    public double apply(String type, double price) {
        if ("vip".equals(type)) return price * 0.8;        // 每次新增类型都改这里
        else if ("new".equals(type)) return price * 0.9;
        return price;
    }
}
```

**正例**（多态开关——策略模式的雏形，与 04 模块的 Payment 同构）：

```java
public interface Discount { double apply(double price); }

public class VipDiscount implements Discount {
    @Override public double apply(double price) { return price * 0.8; }
}

public class NewUserDiscount implements Discount {
    @Override public double apply(double price) { return price * 0.9; }
}

// 新增"节假日活动"：加一个类，Discount 的使用方零修改
public class HolidayDiscount implements Discount {
    @Override public double apply(double price) { return price * 0.7; }
}
```

> 🎯 **核心要点**：OCP 的本质是"把变化点封装在抽象背后"——先识别哪里在变（折扣规则、支付渠道、报表格式），再让它们成为接口的实现。滥用则变成"过度抽象"，见 04 模块第 8 节的判断标准。

---

## 4. L：里氏替换原则 LSP

**定义**：所有引用父类的地方，都能用任意子类替换而不产生错误（程序行为不破坏）。

**反例**（子类改变父类契约——经典"正方形继承矩形"）：

```java
public class Rectangle {
    protected int width, height;
    public void setWidth(int w) { width = w; }
    public void setHeight(int h) { height = h; }
    public int area() { return width * height; }
}

public class Square extends Rectangle {          // Square is-a Rectangle？数学上"是"，工程上不是
    @Override public void setWidth(int w) { width = w; height = w; }  // 破坏 setHeight 的契约
    @Override public void setHeight(int h) { width = h; height = h; }
}

// 调用方假设"设置宽高后面积 = 宽 × 高"，Square 替换进来后行为被破坏
Rectangle r = new Square();
r.setWidth(5);
r.setHeight(10);
// 期望 area = 50，实际 Square 强制正方形 → area = 100
```

**LSP 的具体约束**（子类重写时不能违反）：

| 约束 | 反例 |
|------|------|
| 不能强化前置条件 | 父类允许任意金额，子类却要求金额 > 0，调用方原本合法输入崩溃 |
| 不能弱化后置条件 | 父类保证返回非 null，子类返回 null |
| 不能抛出未声明的异常 | 父类方法声明不抛异常，子类抛 RuntimeException |
| 不能改变方法的语义契约 | 重写 `contains` 却返回"不包含" |
| 集合实现特例 | `Stack` 继承 `Vector`（Java 历史包袱）：Stack 禁止 add 到中间，替换 Vector 时行为违反契约 |

> ⚠️ 现实提示：LSP 违反通常不是语法错误，而是**行为契约破坏**——所以重写前问自己：父类调用方的所有假设，子类都保持吗？`@Override` 只保证签名，不保证契约。

---

## 5. I：接口隔离原则 ISP

**定义**：客户端不应被迫依赖它用不到的接口方法——**接口按"使用方"切分，而不是按"实现方"堆砌**。

**反例**（胖接口：实现类被迫实现无意义方法）：

```java
public interface Worker {
    void work();
    void eat();
    void sleep();
}

public class Robot implements Worker {           // 机器人被迫实现 eat/sleep
    @Override public void work() { }
    @Override public void eat() { throw new UnsupportedOperationException(); }  // 丑
    @Override public void sleep() { throw new UnsupportedOperationException(); }
}
```

**正例**（按角色拆成细接口，实现类自由组合）：

```java
public interface Workable { void work(); }
public interface Feedable { void eat(); }
public interface Sleepable { void sleep(); }

public class HumanWorker implements Workable, Feedable, Sleepable { }
public class RobotWorker implements Workable { }          // 只需实现用得上的
```

> 💡 ISP 与 SRP 的关系：SRP 管类，ISP 管接口；判断标准一致——"是不是同一个变化理由"。JDK 自身大量运用：`List` 与 `RandomAccess`、`Runnable` 与 `Callable` 分离。

---

## 6. D：依赖倒置原则 DIP

**定义**：高层模块不应依赖低层模块，二者都应依赖抽象；抽象不应依赖细节，细节应依赖抽象。

**反例**（高层直接 new 底层）：

```java
public class OrderService {
    private final MySqlOrderDao dao = new MySqlOrderDao();   // 直接依赖具体数据库实现

    public void create(Order o) {
        dao.insert(o);          // 换 Oracle、换缓存，都要改这里
    }
}
```

**正例**（依赖抽象 + 依赖注入）：

```java
public interface OrderDao { void insert(Order o); }        // 抽象

public class MySqlOrderDao implements OrderDao { ... }     // 细节实现

public class OrderService {
    private final OrderDao dao;                             // 依赖抽象

    public OrderService(OrderDao dao) {                     // 依赖注入：谁实现，由外部决定
        this.dao = dao;
    }
}
// 换实现：OrderService 零修改；测试时注入 Mock —— 这正是 Spring IoC 做的事情
```

**依赖注入三形式**：构造器注入（推荐，保证依赖齐全）、setter 注入、接口注入。

> 🎯 **核心要点**：DIP 就是"面向接口编程"的原则化表述——依赖方向永远指向抽象。Spring 的 Bean 容器、MyBatis 的 Mapper 接口代理、JDBC 的 `DriverManager` 都是 DIP 的教科书实现。

---

## 7. 组合优于继承与最少知识原则

**组合优于继承**（详见 03 模块第 7 节）：只为复用方法时用组合（has-a），is-a 成立且需多态时才用继承。**最少知识原则（迪米特法则）**：一个对象应尽量少地与其他对象通信——"只和你的朋友说话"。

```java
// 违反迪米特：深链调用，Order 内部结构被层层剥开
double fee = order.getUser().getAddress().getCity().getTaxRate() * order.getAmount();

// 符合迪米特：Order 自己负责"算费"，外界只依赖一个方法
double fee = order.calculateFee();   // 内部怎么取税，与调用方无关
```

**好处**：耦合面缩小，`Order` 内部结构变化不影响调用方。

---

## 8. 常见设计模式速览

模式是"已验证的类结构解决方案"，与 SOLID 一脉相承。以下五个是最常用的 OOP 基础模式：

| 模式 | 解决的问题 | 核心结构 | 典型例证 |
|------|-----------|---------|---------|
| **策略模式** | 同一行为有多种算法，可替换 | 接口 + 多个实现 + 运行时注入 | 折扣、支付渠道、压缩算法 |
| **模板方法** | 算法骨架固定，步骤可变 | 抽象类 + final 骨架 + 抽象扩展点 | 05 模块的 AbstractPayChannel、AQS |
| **工厂方法** | 创建对象逻辑集中，客户端不 new | 工厂接口 + 具体工厂 | `BeanFactory`、`NumberFormat.getInstance` |
| **单例** | 全类唯一实例 | 私有构造器 + 静态实例 | 配置中心、线程池（见 06 模块） |
| **观察者** | 状态变化通知多个依赖方 | 主题 + 监听器列表 + 回调 | Spring 事件、`Observable`、MQ 发布订阅 |

**策略模式完整小例子**（与 04 模块 Payment 衔接）：

```java
// 策略：接口 + 实现 + 注入
public interface TaxStrategy { BigDecimal taxOf(BigDecimal amount); }

public class NormalTax implements TaxStrategy {
    @Override public BigDecimal taxOf(BigDecimal amount) { return amount.multiply(BigDecimal.valueOf(0.06)); }
}
public class ZeroTax implements TaxStrategy {
    @Override public BigDecimal taxOf(BigDecimal amount) { return BigDecimal.ZERO; }
}

// 客户端持有接口，运行时决定策略（构造器注入 / Spring @Autowired）
public class TaxCalculator {
    private final TaxStrategy strategy;
    public TaxCalculator(TaxStrategy strategy) { this.strategy = strategy; }
    public BigDecimal calculate(BigDecimal amount) { return strategy.taxOf(amount); }
}
```

> 💡 现代 Java 中，只有一个方法的策略接口直接用**函数式接口 + Lambda**（见 05 模块第 6 节）——`TaxStrategy` 可以就是 `Function<BigDecimal, BigDecimal>`。模式是思想，语法随时代演进。

---

## 9. 贫血模型 vs 充血模型

| 维度 | 贫血模型（Anemic） | 充血模型（Rich） |
|------|-------------------|----------------|
| 对象状态 | 纯数据容器（POJO + getter/setter） | 数据 + 业务行为（方法操作自己的状态） |
| 业务逻辑位置 | Service 层 | 领域对象内部 |
| 数据层集成 | 极简单（ORM 友好） | 需处理无参构造器、持久化与行为分离 |
| 测试 | 逻辑集中在 Service，易测 | 领域逻辑内聚，可脱离框架测试 |
| 典型场景 | CRUD 系统、Spring 传统分层 | 复杂业务规则、DDD 领域驱动设计 |

```java
// 贫血：行为在 Service
public class Account {
    private double balance;                // 只有数据
    public double getBalance() { return balance; }
    public void setBalance(double b) { this.balance = b; }
}
// AccountService 里写：if (amount <= balance) { a.setBalance(balance - amount); ... }

// 充血：行为在自身
public class Account {
    private double balance;
    public void withdraw(double amount) {  // 不变量由对象自己守护
        if (amount <= 0 || amount > balance) throw new IllegalArgumentException();
        balance -= amount;
    }
}
```

> 🎯 **核心要点**：充血模型本质是"把封装做到位"——不变量保护、行为内聚。但充血对象被 MyBatis/JSON 重建时往往靠无参构造器 + setter，与"行为守卫状态"冲突，所以工程上常见"持久化用贫血 DTO + 领域用充血模型"的折中（DDD 的 Assembler 模式）。

---

**下一模块**：[10-面试高频考点与总结](./10-面试高频考点与总结.md) / **返回总览**：[00-Java面向对象知识体系总览](./00-Java面向对象知识体系总览.md)
