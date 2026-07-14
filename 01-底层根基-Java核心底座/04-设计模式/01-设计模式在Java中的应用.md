# 设计模式在Java中的应用

## 一、设计模式概述

### 1.1 什么是设计模式

设计模式（Design Pattern）是前辈们经过反复验证、总结出的在特定场景下解决常见问题的**可复用方案**。它不是代码框架，而是一套**经验范式**——就像建筑行业的蓝图，告诉我们"当遇到这类问题时，这样组织代码最稳妥"。1995年，GoF（Gang of Four，四位作者Erich Gamma、Richard Helm、Ralph Johnson、John Vlissides）出版了《Design Patterns: Elements of Reusable Object-Oriented Software》，正式确立了23种经典设计模式的分类体系。

学习设计模式的意义不在于记住23个名词，而在于掌握**面向对象设计的核心思想**：封装变化、面向接口编程、组合优于继承、高内聚低耦合。当你理解了这些思想，写出的代码自然会更灵活、更易于维护。

### 1.2 六大设计原则（SOLID + 额外两条）

设计模式建立在六大设计原则之上。这六条原则是判断代码质量的"尺子"，也是选择设计模式的"指南针"。

| 原则 | 英文缩写 | 核心思想 |
|------|---------|---------|
| 单一职责原则 | SRP | 一个类只负责一个职责，只有一个引起它变化的原因 |
| 开闭原则 | OCP | 对扩展开放，对修改关闭 |
| 里氏替换原则 | LSP | 子类必须能替换父类且程序行为不变 |
| 接口隔离原则 | ISP | 不强迫客户端依赖它不需要的接口方法 |
| 依赖倒置原则 | DIP | 面向抽象编程，不面向具体实现编程 |
| 迪米特法则 | LoD | 最小知识原则，只与直接朋友通信 |

**单一职责原则（SRP）**

一个类只应有一个引起它变化的原因。换句话说，一个类只做一件事。例如，一个`UserService`只处理用户业务逻辑，而不应该同时负责用户数据的持久化和用户请求的HTTP序列化。

```java
// 违反SRP：一个类承担了职责
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

**开闭原则（OCP）**

对扩展开放，对修改关闭。即当需要新增功能时，应该通过扩展现有代码（新增类、实现接口）来实现，而不是修改已有代码。这是设计模式追求的最高目标之一。

```java
// 违反OCP：每次新增支付方式都要修改switch
public class PaymentService {
    public void pay(String type, double amount) {
        switch (type) {
            case "alipay": /* 支付宝逻辑 */ break;
            case "wechat": /* 微信逻辑 */ break;
            // 新增银行卡支付：必须修改此处 ❌
        }
    }
}

// 符合OCP：通过策略模式扩展
public interface PaymentStrategy {
    void pay(double amount);
}

public class AlipayStrategy implements PaymentStrategy { /* 支付宝逻辑 */ }
public class WechatStrategy implements PaymentStrategy { /* 微信逻辑 */ }
// 新增银行卡支付：新增类即可，无需修改原有代码 ✅
```

**里氏替换原则（LSP）**

所有引用父类的地方必须能透明地使用子类对象。子类可以扩展父类的功能，但不能改变父类原有的功能语义。经典的反例是"正方形继承矩形"：

```java
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

// 客户端代码期望的是矩形行为
Rectangle r = new Square();
r.setWidth(5);
r.setHeight(4);
// 期望面积20, 实际得到16 —— 违反LSP
```

**接口隔离原则（ISP）**

客户端不应该被迫依赖它不使用的方法。接口应该小而专，而不是大而全。

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

**依赖倒置原则（DIP）**

高层模块不应依赖低层模块，二者都应依赖抽象。抽象不应依赖细节，细节应依赖抽象。简单说就是：**面向接口编程，不要面向实现编程**。

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

**迪米特法则（LoD）**

一个对象应该对其他对象有最少的了解。不要和"陌生人"说话，只与直接朋友通信。这降低了类之间的耦合度。

```java
// 违反LoD：直接获取了内部对象并调用其方法
public class Company {
    private Department department;
    public Department getDepartment() { return department; }
}
public class Boss {
    public void check(Company company) {
        // Boss直接和Department的"陌生人"Manager交互
        company.getDepartment().getManager().report();
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

### 1.3 23种设计模式分类

GoF将23种设计模式按用途分为三大类：

| 类别 | 数量 | 模式名称 | 核心作用 |
|------|------|---------|---------|
| **创建型** | 5 | 单例、工厂方法、抽象工厂、建造者、原型 | 解决对象的创建问题，封装创建逻辑 |
| **结构型** | 7 | 代理、适配器、装饰器、享元、组合、外观、桥接 | 解决类或对象的组合、结构关系 |
| **行为型** | 11 | 策略、模板方法、观察者、责任链、命令、状态、迭代器、访问者、中介者、解释器、备忘录 | 解决对象间的职责分配与通信 |

> 创建型模式的核心思想是：**将"创建"与"使用"分离**，让系统不依赖于具体类的实例化方式。
> 结构型模式的核心思想是：**通过组合/继承来组织类的关系**，实现更灵活的结构。
> 行为型模式的核心思想是：**封装变化的行为**，让对象间的交互更灵活。

---

## 二、创建型模式（Creational Patterns）

创建型模式关注的是**如何灵活地创建对象**，将对象的创建逻辑封装起来，使系统与具体类解耦。

### 2.1 单例模式（Singleton Pattern）

**概念**：确保一个类在JVM中只有一个实例，并提供一个全局访问点。

**适用场景**：配置文件读取、日志记录器、数据库连接池、Spring Bean默认作用域等。

#### 饿汉式

在类加载时就创建实例，由JVM保证线程安全。缺点是可能造成资源浪费（如果从未使用的话）。

```java
public class EagerSingleton {
    // 类加载时直接初始化，JVM保证只初始化一次，天然线程安全
    private static final EagerSingleton INSTANCE = new EagerSingleton();

    private EagerSingleton() {} // 私有构造，防止外部new

    public static EagerSingleton getInstance() {
        return INSTANCE;
    }
}
```

**优点**：实现简单，线程安全，没有同步开销。
**缺点**：类加载时就创建，可能造成资源浪费。如果创建开销很大且不一定被使用，不推荐。

#### 懒汉式（双检锁 DCL + volatile）

延迟加载，用的时候才创建。通过双重检查锁定（Double-Checked Locking）减少同步开销。

```java
public class LazySingleton {
    // volatile 关键字：禁止指令重排序，保证可见性
    // 为什么要加volatile？
    // instance = new LazySingleton() 在字节码层面分为三步：
    // 1. 分配内存空间 2. 初始化对象 3. 将引用指向内存地址
    // 如果不加volatile，JVM可能重排序为 1→3→2，导致另一个线程拿到未初始化完毕的对象
    private static volatile LazySingleton instance;

    private LazySingleton() {}

    public static LazySingleton getInstance() {
        if (instance == null) {               // 第一次检查：避免不必要的同步
            synchronized (LazySingleton.class) {
                if (instance == null) {       // 第二次检查：确保只有一个线程创建实例
                    instance = new LazySingleton();
                }
            }
        }
        return instance;
    }
}
```

**volatile的作用详解**：
1. **禁止指令重排序**：`new LazySingleton()`不是原子操作。JVM可能先分配内存、再将引用指向内存地址（此时对象尚未初始化），另一个线程判断`instance != null`后直接返回使用，导致空指针异常。volatile的`happens-before`规则禁止了这种重排序。
2. **保证可见性**：volatile保证一个线程对`instance`的修改立即被其他线程看到。

#### 静态内部类（推荐方案）

利用JVM类加载机制实现延迟加载和线程安全，兼具饿汉式的简洁性和懒汉式的延迟加载优势。

```java
public class StaticInnerSingleton {
    // 静态内部类只有在被显式调用时才会被加载
    private static class SingletonHolder {
        private static final StaticInnerSingleton INSTANCE = new StaticInnerSingleton();
    }

    private StaticInnerSingleton() {}

    public static StaticInnerSingleton getInstance() {
        return SingletonHolder.INSTANCE;
    }
}
```

**为什么推荐？**：JVM加载外部类时不会加载内部类，只有调用`getInstance()`时才会加载`SingletonHolder`，此时JVM的类加载机制保证`INSTANCE`只被初始化一次。无需同步，没有性能损耗。

#### 枚举实现（防反射破坏，Joshua Bloch推荐）

《Effective Java》作者Joshua Bloch强烈推荐的实现方式，可以防止反射和序列化攻击。

```java
public enum EnumSingleton {
    INSTANCE;

    private String config;
    private int value;

    public void doSomething() {
        // 业务方法
    }

    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
}
```

**为什么枚举能防反射？**
- Java的反射机制无法通过`Constructor.newInstance()`创建枚举实例（JDK源码中对此做了限制）。
- 枚举的序列化由JVM保证，反序列化不会创建新实例，因此也防止了序列化破坏单例。

```java
// 反射攻击测试
Constructor<EnumSingleton> constructor = EnumSingleton.class.getDeclaredConstructor();
constructor.setAccessible(true);
// 运行会抛出：java.lang.NoSuchMethodException —— 枚举的构造器签名不同
// 即使是正确签名，也会抛出 java.lang.IllegalArgumentException: Cannot reflectively create enum objects
```

#### Spring中的单例应用

Spring IoC容器中的Bean默认就是单例的（`@Scope("singleton")`），但与GoF单例模式不同：Spring管理的是**每个Bean名称对应一个实例**，而不是每个类只有一个实例。

```java
@Component
@Scope("singleton") // 默认就是singleton，可省略
public class UserService {
    // ...
}
```

Spring通过**单例注册表（Singleton Registry）** 实现Bean的单例管理：

```java
// Spring源码简化示意
public class DefaultSingletonBeanRegistry {
    // 一级缓存：完整创建好的单例Bean
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

    // 三级缓存：解决循环依赖的早期引用
    private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);
    private final Map<String, Object> earlySingletonObjects = new HashMap<>(16);

    public Object getSingleton(String beanName) {
        Object bean = singletonObjects.get(beanName);
        if (bean == null) {
            // 处理循环依赖...
            bean = singletonObjects.get(beanName);
        }
        return bean;
    }
}
```

### 2.2 工厂方法模式（Factory Method Pattern）

**概念**：定义一个用于创建对象的接口，让子类决定实例化哪个类。工厂方法将类的实例化延迟到子类。

**适用场景**：当一个类无法预知它需要创建哪个类的对象时，例如Spring的BeanFactory。

```java
// 产品接口
public interface Product {
    void use();
}

// 具体产品
public class ConcreteProductA implements Product {
    @Override
    public void use() { System.out.println("使用产品A"); }
}

public class ConcreteProductB implements Product {
    @Override
    public void use() { System.out.println("使用产品B"); }
}

// 工厂接口（核心）
public interface ProductFactory {
    Product createProduct();
}

// 具体工厂
public class ProductAFactory implements ProductFactory {
    @Override
    public Product createProduct() { return new ConcreteProductA(); }
}

public class ProductBFactory implements ProductFactory {
    @Override
    public Product createProduct() { return new ConcreteProductB(); }
}

// 客户端使用
public class Client {
    public static void main(String[] args) {
        ProductFactory factory = new ProductAFactory();
        Product product = factory.createProduct();
        product.use(); // 输出：使用产品A
    }
}
```

**Spring中的应用——FactoryBean**

Spring的`FactoryBean`接口是工厂方法模式的典型应用。开发者通过实现`FactoryBean`可以自定义创建Bean的逻辑，Spring将FactoryBean创建的对象注册到容器中。

```java
@Component
public class MyServiceFactoryBean implements FactoryBean<MyService> {
    @Override
    public MyService getObject() throws Exception {
        // 可以在这里做复杂的初始化逻辑
        MyService service = new MyService();
        service.setConfig(loadConfig());
        return service;
    }

    @Override
    public Class<?> getObjectType() {
        return MyService.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private String loadConfig() {
        // 从配置中心加载
        return "production-config";
    }
}
```

### 2.3 抽象工厂模式（Abstract Factory Pattern）

**概念**：提供一个创建一系列相关或相互依赖对象的接口，而无需指定它们的具体类。它解决的是"**产品族**"的创建问题。

如果说工厂方法模式是一个工厂生产一种产品，那么抽象工厂模式就是一个工厂可以生产**一整套产品族**。

```java
// 产品接口
public interface Car { void drive(); }
public interface Engine { void start(); }

// 丰田产品族
public class ToyotaCar implements Car {
    @Override
    public void drive() { System.out.println("驾驶丰田汽车"); }
}
public class ToyotaEngine implements Engine {
    @Override
    public void start() { System.out.println("丰田发动机启动"); }
}

// 宝马产品族
public class BmwCar implements Car {
    @Override
    public void drive() { System.out.println("驾驶宝马车"); }
}
public class BmwEngine implements Engine {
    @Override
    public void start() { System.out.println("宝马发动机启动"); }
}

// 抽象工厂：定义创建一整套产品的方法
public interface CarFactory {
    Car createCar();
    Engine createEngine();
}

// 具体工厂：丰田工厂生产一整套丰田产品
public class ToyotaFactory implements CarFactory {
    @Override
    public Car createCar() { return new ToyotaCar(); }
    @Override
    public Engine createEngine() { return new ToyotaEngine(); }
}

// 具体工厂：宝马工厂生产一整套宝马产品
public class BmwFactory implements CarFactory {
    @Override
    public Car createCar() { return new BmwCar(); }
    @Override
    public Engine createEngine() { return new BmwEngine(); }
}
```

**Spring中的应用**：Spring的`BeanFactory`体系在一定程度上借鉴了抽象工厂的思想——不同的`BeanFactory`实现（`XmlBeanFactory`、`AnnotationConfigApplicationContext`等）可以创建不同类型的Bean定义。

### 2.4 建造者模式（Builder Pattern）

**概念**：将一个复杂对象的构建与它的表示分离，使得同样的构建过程可以创建不同的表示。

**适用场景**：当对象有大量可选参数、构造方法参数过多（超过4-5个）时，建造者模式比重叠构造器（Telescoping Constructor）和JavaBeans模式更优。

```java
// 产品类
public class Computer {
    private String cpu;
    private String gpu;
    private int ram;       // GB
    private int storage;   // GB
    private boolean hasBluetooth;
    private boolean hasWifi;

    // 私有构造器，只能通过Builder创建
    private Computer(Builder builder) {
        this.cpu = builder.cpu;
        this.gpu = builder.gpu;
        this.ram = builder.ram;
        this.storage = builder.storage;
        this.hasBluetooth = builder.hasBluetooth;
        this.hasWifi = builder.hasWifi;
    }

    // 静态内部类作为Builder
    public static class Builder {
        private String cpu = "Intel i5";  // 默认值
        private String gpu = "集成显卡";
        private int ram = 8;
        private int storage = 256;
        private boolean hasBluetooth = false;
        private boolean hasWifi = true;

        public Builder cpu(String cpu) { this.cpu = cpu; return this; }
        public Builder gpu(String gpu) { this.gpu = gpu; return this; }
        public Builder ram(int ram) { this.ram = ram; return this; }
        public Builder storage(int storage) { this.storage = storage; return this; }
        public Builder bluetooth(boolean has) { this.hasBluetooth = has; return this; }
        public Builder wifi(boolean has) { this.hasWifi = has; return this; }

        public Computer build() { return new Computer(this); }
    }
}

// 客户端使用——链式调用
Computer computer = new Computer.Builder()
        .cpu("Intel i9")
        .gpu("NVIDIA RTX 4090")
        .ram(32)
        .storage(1024)
        .bluetooth(true)
        .build();
```

**实际应用**：
- **Lombok @Builder**：一行注解自动生成Builder模式，极大简化代码。
- **StringBuilder**：`append()`方法返回`this`，实现链式调用，最后通过`toString()`生成字符串——这是建造者模式的变体。
- **OkHttp / Retrofit**：`new OkHttpClient.Builder().connectTimeout(...).readTimeout(...).build()`

```java
// Lombok版本——一行搞定
@Data
@Builder
public class ComputerLombok {
    private String cpu;
    private String gpu;
    private int ram;
    private int storage;
    private boolean hasBluetooth;
    private boolean hasWifi;
}

// 使用
ComputerLombok computer = ComputerLombok.builder()
        .cpu("AMD Ryzen 9")
        .ram(64)
        .build();
```

### 2.5 原型模式（Prototype Pattern）

**概念**：通过复制现有实例来创建新实例，而不是通过new创建。Java中通过`clone()`方法实现。

**适用场景**：对象创建成本高（如数据库查询、远程调用）、需要保存对象当前状态的副本。

```java
public class User implements Cloneable {
    private String name;
    private int age;
    private List<String> roles; // 引用类型

    public User(String name, int age, List<String> roles) {
        this.name = name;
        this.age = age;
        this.roles = roles;
    }

    // 浅拷贝：基本类型复制值，引用类型复制引用（指向同一对象）
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}

// 深拷贝：引用类型也复制一份新对象
@Override
protected Object clone() throws CloneNotSupportedException {
    User cloned = (User) super.clone();
    // 手动复制引用类型
    cloned.roles = new ArrayList<>(this.roles); // 创建新的List
    return cloned;
}
```

**浅拷贝 vs 深拷贝**：
- **浅拷贝**：基本类型复制值，引用类型复制引用地址。修改克隆对象的`roles`会影响原对象。
- **深拷贝**：引用类型也创建新对象。修改克隆对象不影响原对象。实现方式：重写`clone()`手动复制引用字段，或通过序列化实现。

**Spring中的应用 — @Scope("prototype")**：

```java
@Component
@Scope("prototype") // 每次获取都创建一个新Bean
public class PrototypeBean {
    // ...
}

// 结合ObjectFactory避免每次注入都创建
@Component
public class SingletonBean {
    @Autowired
    private ObjectFactory<PrototypeBean> prototypeBeanFactory;

    public void usePrototype() {
        PrototypeBean bean = prototypeBeanFactory.getObject();
        // ...
    }
}
```

---

## 三、结构型模式（Structural Patterns）

结构型模式关注如何**组合类或对象**以形成更大的结构，解决模块之间的耦合问题。

### 3.1 代理模式（Proxy Pattern）——重点

**概念**：为其他对象提供一种代理以控制对这个对象的访问。代理模式和装饰器模式结构相似，但目的不同——代理是为了**控制访问**，装饰器是为了**增强功能**。

**适用场景**：AOP（面向切面编程）、延迟加载、权限控制、日志记录、事务管理。

#### 静态代理

代理类和目标类实现相同接口，在代理类中调用目标方法并在前后增加逻辑。

```java
// 业务接口
public interface UserService {
    void saveUser(String name);
    void deleteUser(Long id);
}

// 目标类
public class UserServiceImpl implements UserService {
    @Override
    public void saveUser(String name) {
        System.out.println("保存用户: " + name);
    }
    @Override
    public void deleteUser(Long id) {
        System.out.println("删除用户: " + id);
    }
}

// 代理类：和目标类实现同样的接口，加入额外逻辑
public class UserServiceProxy implements UserService {
    private UserService target;

    public UserServiceProxy(UserService target) {
        this.target = target;
    }

    @Override
    public void saveUser(String name) {
        System.out.println("[日志] 开始保存用户: " + name);
        long start = System.currentTimeMillis();

        target.saveUser(name);

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("[日志] 保存完成，耗时: " + elapsed + "ms");
    }

    @Override
    public void deleteUser(Long id) {
        System.out.println("[日志] 开始删除用户: " + id);
        target.deleteUser(id);
        System.out.println("[日志] 删除完成");
    }
}
```

**缺点**：每个目标类都需要手动编写一个代理类，随着方法增多，代理类会变得庞大。

#### JDK动态代理

基于接口的代理。目标类必须实现接口，JDK通过`Proxy.newProxyInstance()`在运行时动态生成代理类。

```java
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

// 通用代理处理器
public class LogInvocationHandler implements InvocationHandler {
    private final Object target;

    public LogInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("[JDK动态代理] 方法执行前: " + method.getName());

        long start = System.currentTimeMillis();
        Object result = method.invoke(target, args); // 反射调用目标方法
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("[JDK动态代理] 方法执行后，耗时: " + elapsed + "ms");
        return result;
    }
}

// 客户端创建代理
UserService target = new UserServiceImpl();
UserService proxy = (UserService) Proxy.newProxyInstance(
        target.getClass().getClassLoader(),     // 类加载器
        target.getClass().getInterfaces(),       // 目标实现的接口
        new LogInvocationHandler(target)         // 调用处理器
);
proxy.saveUser("张三");
```

**底层原理**：JDK动态代理在运行时生成一个`$Proxy0`类，它实现了目标接口并继承`Proxy`。所有方法调用都转发到`InvocationHandler.invoke()`。

**限制**：必须基于接口，不能代理没有实现接口的类。

#### CGLIB动态代理

基于继承的代理。通过字节码技术（ASM）动态生成目标类的子类，覆盖目标类的方法。适用于没有接口或希望代理具体类的情况。

```java
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

// 没有接口的目标类
public class UserDao {
    public void save(String name) {
        System.out.println("保存用户: " + name);
    }
    public final void delete(Long id) { // final方法无法被代理
        System.out.println("删除用户: " + id);
    }
}

// 方法拦截器
public class LogInterceptor implements MethodInterceptor {
    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        System.out.println("[CGLIB] 方法执行前: " + method.getName());

        // 注意是调用proxy.invokeSuper，而不是proxy.invoke（否则会无限递归）
        Object result = proxy.invokeSuper(obj, args);

        System.out.println("[CGLIB] 方法执行后: " + method.getName());
        return result;
    }
}

// 创建代理
Enhancer enhancer = new Enhancer();
enhancer.setSuperclass(UserDao.class);
enhancer.setCallback(new LogInterceptor());
UserDao proxy = (UserDao) enhancer.create();
proxy.save("李四");
```

**CGLIB的限制**：
- 不能代理`final`方法（因为无法覆盖）
- 不能代理`final`类

#### 三种代理对比

| 特性 | 静态代理 | JDK动态代理 | CGLIB代理 |
|------|---------|------------|-----------|
| 实现方式 | 手动编写代理类 | 运行时生成的接口实现类 | 运行时生成的子类 |
| 是否需要接口 | 需要 | 需要 | 不需要 |
| 性能（调用） | 直接调用，最快 | 反射调用，较慢 | 方法调用，较快 |
| 灵活性 | 低 | 高 | 高 |
| 适用范围 | 小，方法固定 | 有接口的类 | 无接口或需继承的类 |

#### Spring AOP中的应用

Spring AOP是代理模式的集大成者。当Bean有接口时默认使用JDK动态代理，否则使用CGLIB。可以通过`@EnableAspectJAutoProxy(proxyTargetClass = true)`强制使用CGLIB。

```java
// 声明式事务——优雅的代理应用
@Service
public class OrderService {
    @Transactional // Spring通过代理为这个方法添加事务管理
    public void createOrder(Order order) {
        orderDao.insert(order);
        inventoryService.deduct(order.getProductId(), order.getQuantity());
        // 如果上面抛出异常，事务会自动回滚
    }
}

// 缓存
@Cacheable(value = "users", key = "#id")
public User getUser(Long id) {
    // 第一次调用执行方法并缓存结果，后续调用直接返回缓存
    return userDao.findById(id);
}

// 权限校验
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long id) {
    userDao.deleteById(id);
}
```

### 3.2 适配器模式（Adapter Pattern）

**概念**：将一个类的接口转换成客户希望的另一个接口，使原本不兼容的类可以一起工作。

**适用场景**：系统需要使用第三方库、老系统需要与新系统对接、统一多种不同接口。

#### 类适配器（通过继承）

```java
// 目标接口：新系统期望的USB接口
public interface Usb {
    void transferData();
}

// 被适配者：老系统的PS2接口
public class Ps2Keyboard {
    public void typeWithPs2() {
        System.out.println("通过PS2接口输入");
    }
}

// 类适配器：继承被适配者，实现目标接口
public class Ps2ToUsbAdapter extends Ps2Keyboard implements Usb {
    @Override
    public void transferData() {
        // 内部转换为PS2的操作
        super.typeWithPs2();
    }
}

// 使用
Usb usbKeyboard = new Ps2ToUsbAdapter();
usbKeyboard.transferData(); // 输出：通过PS2接口输入
```

#### 对象适配器（通过组合——推荐）

```java
// 对象适配器：持有被适配者的引用（组合）
public class Ps2ToUsbAdapterObject implements Usb {
    private Ps2Keyboard ps2Keyboard; // 组合

    public Ps2ToUsbAdapterObject(Ps2Keyboard ps2Keyboard) {
        this.ps2Keyboard = ps2Keyboard;
    }

    @Override
    public void transferData() {
        ps2Keyboard.typeWithPs2();
    }
}

// 使用：适配任意Ps2Keyboard子类
Ps2Keyboard oldKeyboard = new Ps2Keyboard();
Usb adapter = new Ps2ToUsbAdapterObject(oldKeyboard);
adapter.transferData();
```

**组合（对象适配器） vs 继承（类适配器）**：对象适配器更灵活，可以适配某个接口的所有子类，符合"组合优于继承"原则。

#### Spring MVC HandlerAdapter

Spring MVC中的`HandlerAdapter`是适配器模式的经典应用。不同的Controller（`@Controller`、`HttpRequestHandler`、`SimpleControllerHandlerAdapter`等）有不同的调用方式，`HandlerAdapter`将他们统一适配。

```java
// Spring源码：HandlerAdapter接口
public interface HandlerAdapter {
    // 判断是否支持该handler
    boolean supports(Object handler);

    // 统一调用handler处理请求
    ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception;
}

// 适配不同类型的Controller
public class SimpleControllerHandlerAdapter implements HandlerAdapter {
    @Override
    public boolean supports(Object handler) {
        return (handler instanceof Controller);
    }
    @Override
    public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        return ((Controller) handler).handleRequest(request, response);
    }
}

public class RequestMappingHandlerAdapter implements HandlerAdapter {
    @Override
    public boolean supports(Object handler) {
        return (handler instanceof HandlerMethod);
    }
    @Override
    public ModelAndView handle(...) {
        // 通过反射调用@Controller中的@RequestMapping方法
        // ...
    }
}
```

### 3.3 装饰器模式（Decorator Pattern）

**概念**：动态地给一个对象添加额外的功能，比继承更灵活。装饰器和被装饰者实现相同接口，装饰器持有被装饰者的引用。

**适用场景**：需要为对象动态添加功能，且功能可以自由组合（典型的"套娃"模式）。

#### Java I/O流——最经典的装饰器

```java
// 核心组件
InputStream fileInputStream = new FileInputStream("test.txt");

// 逐层装饰
InputStream buffered = new BufferedInputStream(fileInputStream);   // 添加缓冲功能
InputStream data = new DataInputStream(buffered);                  // 添加基本类型读写功能

// 甚至可以……
InputStream fullyDecorated = new DataInputStream(                 // 基本类型读写
        new BufferedInputStream(                                  // 缓冲
                new FileInputStream("test.txt")));                // 文件读取

// 一行流的完整装饰
try (InputStream in = new BufferedInputStream(
        new FileInputStream("test.txt"))) {
    byte[] buffer = new byte[1024];
    int len;
    while ((len = in.read(buffer)) != -1) {
        System.out.write(buffer, 0, len);
    }
}
```

**装饰器模式 vs 继承**：
- 继承是静态的、编译期的，子类在编译时就知道自己有哪些功能。
- 装饰器是动态的、运行时的，可以在运行时自由组合功能。例如，`FileInputStream`可以包`BufferedInputStream`获得缓冲，也可以包`DataInputStream`获得基本类型读写，还可以两层都包。

```java
// 自定义装饰器示例
public interface Coffee {
    double cost();
    String description();
}

public class PlainCoffee implements Coffee {
    @Override
    public double cost() { return 5.0; }
    @Override
    public String description() { return "原味咖啡"; }
}

// 装饰器基类
public abstract class CoffeeDecorator implements Coffee {
    protected Coffee coffee; // 持有被装饰者
    public CoffeeDecorator(Coffee coffee) {
        this.coffee = coffee;
    }
    @Override
    public double cost() { return coffee.cost(); }
    @Override
    public String description() { return coffee.description(); }
}

// 具体装饰器
public class MilkDecorator extends CoffeeDecorator {
    public MilkDecorator(Coffee coffee) { super(coffee); }
    @Override
    public double cost() { return super.cost() + 2.0; }
    @Override
    public String description() { return super.description() + " + 牛奶"; }
}

public class SugarDecorator extends CoffeeDecorator {
    public SugarDecorator(Coffee coffee) { super(coffee); }
    @Override
    public double cost() { return super.cost() + 1.0; }
    @Override
    public String description() { return super.description() + " + 糖"; }
}

// 自由组合
Coffee coffee = new PlainCoffee();
coffee = new MilkDecorator(coffee);
coffee = new SugarDecorator(coffee);
System.out.println(coffee.description() + " = " + coffee.cost() + "元");
// 输出：原味咖啡 + 牛奶 + 糖 = 8.0元
```

### 3.4 享元模式（Flyweight Pattern）

**概念**：通过共享已有的对象来减少内存占用和对象创建的开销。核心是**对象复用**。

**适用场景**：大量细粒度的对象、相同状态的对象可以共享、系统内存资源有限。

#### Integer.valueOf() 的缓存机制

```java
Integer a = Integer.valueOf(100);
Integer b = Integer.valueOf(100);
System.out.println(a == b); // true —— 享元模式：-128~127之间的Integer共享对象

Integer c = Integer.valueOf(200);
Integer d = Integer.valueOf(200);
System.out.println(c == d); // false —— 超出缓存范围，创建新对象
```

JDK源码中的实现（简化的享元工厂）：

```java
// Integer内部类 IntegerCache
private static class IntegerCache {
    static final Integer cache[] = new Integer[-(-128) + 127 + 1];
    static {
        for (int i = 0; i < cache.length; i++)
            cache[i] = new Integer(i - 128);
    }
}

public static Integer valueOf(int i) {
    if (i >= IntegerCache.low && i <= IntegerCache.high)
        return IntegerCache.cache[i + (-IntegerCache.low)];
    return new Integer(i);
}
```

#### 数据库连接池

连接池是享元模式的典型应用——复用数据库连接对象，避免频繁创建和销毁TCP连接的开销。

```java
// HikariCP连接池（Spring Boot 2.x默认连接池）简化示例
public class ConnectionPool {
    private final List<Connection> pool = new ArrayList<>();
    private final int maxSize = 20;

    public Connection getConnection() {
        synchronized (pool) {
            for (Connection conn : pool) {
                if (!((HikariProxyConnection) conn).isClosed()) {
                    // 状态重置（享元的外部状态）
                    conn.setAutoCommit(true);
                    return conn;
                }
            }
            if (pool.size() < maxSize) {
                Connection newConn = DriverManager.getConnection("jdbc:...");
                pool.add(newConn);
                return newConn;
            }
            throw new RuntimeException("连接池已满");
        }
    }
}
```

**享元模式的关键概念**：
- **内部状态（Intrinsic State）**：可共享的、不变的状态，存储在享元对象内部。
- **外部状态（Extrinsic State）**：不可共享的、随环境变化的状态，由客户端传入方法参数。

例如连接池中的内部状态是连接本身（`Connection`对象），外部状态是SQL语句和参数。

#### Java常量池

字符串常量池也是享元模式的应用：

```java
String s1 = "hello";
String s2 = "hello";
System.out.println(s1 == s2); // true —— 享元：常量池中同一个对象

String s3 = new String("hello");
System.out.println(s1 == s3); // false —— new强制创建新对象
```

### 3.5 组合模式（Composite Pattern）

**概念**：将对象组合成树形结构以表示"部分-整体"的层次结构，使得客户端对单个对象和组合对象的使用具有一致性。

**适用场景**：树形菜单、文件系统、组织结构、XML解析。

```java
// 组件接口：统一对待叶子节点和容器节点
public interface FileSystemNode {
    String getName();
    long getSize();       // 获取大小
    void display(String indent); // 显示树形结构
}

// 叶子节点：文件
public class FileLeaf implements FileSystemNode {
    private String name;
    private long size;

    public FileLeaf(String name, long size) {
        this.name = name;
        this.size = size;
    }

    @Override
    public String getName() { return name; }

    @Override
    public long getSize() { return size; }

    @Override
    public void display(String indent) {
        System.out.println(indent + "📄 " + name + " (" + size + " bytes)");
    }
}

// 容器节点：文件夹
public class FolderComposite implements FileSystemNode {
    private String name;
    private List<FileSystemNode> children = new ArrayList<>();

    public FolderComposite(String name) {
        this.name = name;
    }

    public void add(FileSystemNode node) {
        children.add(node);
    }

    public void remove(FileSystemNode node) {
        children.remove(node);
    }

    @Override
    public String getName() { return name; }

    @Override
    public long getSize() {
        // 递归计算所有子节点大小之和
        return children.stream().mapToLong(FileSystemNode::getSize).sum();
    }

    @Override
    public void display(String indent) {
        System.out.println(indent + "📁 " + name + "/");
        for (FileSystemNode child : children) {
            child.display(indent + "  ");
        }
    }
}

// 客户端使用：文件和文件夹统一对待
public class Client {
    public static void main(String[] args) {
        FolderComposite root = new FolderComposite("项目");

        FileLeaf readme = new FileLeaf("README.md", 500);
        FolderComposite src = new FolderComposite("src");
        src.add(new FileLeaf("Main.java", 1024));
        src.add(new FileLeaf("Utils.java", 2048));

        FolderComposite test = new FolderComposite("test");
        test.add(new FileLeaf("MainTest.java", 512));

        root.add(readme);
        root.add(src);
        root.add(test);

        // 统一显示，无论文件还是文件夹，都调用display
        root.display("");
        System.out.println("总大小: " + root.getSize() + " bytes");
    }
}
```

**透明模式 vs 安全模式**：
- **透明模式**：在`Component`接口中声明所有方法（包括`add`/`remove`），叶子节点也要实现但抛出异常。优点是客户端无需区分。
- **安全模式**：只在容器节点声明`add`/`remove`方法，但客户端需要区分叶子节点和容器节点。

---

## 四、行为型模式（Behavioral Patterns）

行为型模式关注的是**对象之间的职责分配和通信方式**，解决"怎么干"的问题。

### 4.1 策略模式（Strategy Pattern）——重点

**概念**：定义一系列算法，将每个算法封装起来，并使它们可以互相替换。策略模式让算法的变化独立于使用算法的客户端。

**适用场景**：多重条件判断（if-else/switch）替代方案、支付方式选择、促销活动、比较器、资源加载方式。

```java
// 策略接口
public interface PaymentStrategy {
    void pay(BigDecimal amount);
}

// 具体策略：支付宝
public class AlipayStrategy implements PaymentStrategy {
    private String alipayAccount;

    public AlipayStrategy(String alipayAccount) {
        this.alipayAccount = alipayAccount;
    }

    @Override
    public void pay(BigDecimal amount) {
        System.out.println("支付宝 [" + alipayAccount + "] 支付: " + amount + "元");
        // 调用支付宝SDK...
    }
}

// 具体策略：微信支付
public class WechatPayStrategy implements PaymentStrategy {
    private String openId;

    public WechatPayStrategy(String openId) {
        this.openId = openId;
    }

    @Override
    public void pay(BigDecimal amount) {
        System.out.println("微信支付 [" + openId + "] 支付: " + amount + "元");
        // 调用微信支付SDK...
    }
}

// 具体策略：银行卡支付
public class BankCardStrategy implements PaymentStrategy {
    private String cardNo;
    public BankCardStrategy(String cardNo) {
        this.cardNo = cardNo;
    }
    @Override
    public void pay(BigDecimal amount) {
        System.out.println("银行卡 [" + cardNo.substring(cardNo.length()-4) + "] 支付: " + amount + "元");
    }
}

// 上下文：持有策略引用，由客户端决定用哪个策略
public class PaymentContext {
    private PaymentStrategy strategy;

    public void setStrategy(PaymentStrategy strategy) {
        this.strategy = strategy;
    }

    public void executePayment(BigDecimal amount) {
        if (strategy == null) {
            throw new IllegalStateException("请先选择支付方式");
        }
        strategy.pay(amount);
    }
}

// 客户端使用
PaymentContext context = new PaymentContext();
context.setStrategy(new AlipayStrategy("alipay@example.com"));
context.executePayment(new BigDecimal("99.99"));

// 切换到微信支付
context.setStrategy(new WechatPayStrategy("wechat_openid_123"));
context.executePayment(new BigDecimal("199.00"));
```

**Comparator —— 函数式接口作为策略**：

```java
// Comparator<T> 就是一个策略接口
List<User> users = getUserList();

// 按年龄排序（年龄策略）
users.sort(Comparator.comparingInt(User::getAge));

// 按姓名排序（姓名策略）
users.sort(Comparator.comparing(User::getName));

// 自定义排序策略
users.sort((u1, u2) -> u1.getScore() - u2.getScore());
```

**Spring中的策略模式**：

```java
// Resource接口的不同实现——策略模式
Resource resource;
if (classpath) {
    resource = new ClassPathResource("application.yml");
} else if (file) {
    resource = new FileSystemResource("/etc/config/application.yml");
} else {
    resource = new UrlResource("http://config-server/application.yml");
}
```

**@SentinelResource 的 fallback**：

```java
@SentinelResource(
    value = "getUser",
    fallback = "getUserFallback",           // 失败时降级策略
    blockHandler = "getUserBlockHandler"    // 限流时的处理策略
)
public User getUser(Long id) {
    // 正常业务逻辑
}

// 降级策略
public User getUserFallback(Long id, Throwable t) {
    return new User(id, "默认用户", "缓存不可用");
}
```

### 4.2 模板方法模式（Template Method Pattern）

**概念**：在一个方法中定义一个算法的骨架（模板），将一些步骤延迟到子类中实现。模板方法使得子类可以在不改变算法结构的情况下，重新定义算法的某些步骤。

**适用场景**：多个子类有相同的行为逻辑骨架、框架中的扩展点设计。

```java
// 抽象类：定义制作饮料的骨架
public abstract class BeverageMaker {
    // 模板方法（final防止子类修改骨架）
    public final void makeBeverage() {
        boilWater();      // 1. 烧水（通用）
        brew();           // 2. 冲泡（子类实现）
        pourInCup();      // 3. 倒入杯子（通用）
        addCondiments();  // 4. 添加调料（子类实现）
        if (wantPackage()) { // 5. 钩子方法——可选步骤
            packageUp();
        }
    }

    private void boilWater() {
        System.out.println("烧开水");
    }

    private void pourInCup() {
        System.out.println("倒入杯中");
    }

    // 抽象方法：子类必须实现
    protected abstract void brew();
    protected abstract void addCondiments();

    // 钩子方法：子类可以选择性覆盖
    protected boolean wantPackage() { return false; }

    private void packageUp() {
        System.out.println("打包带走");
    }
}

// 具体子类：咖啡
public class CoffeeMaker extends BeverageMaker {
    @Override
    protected void brew() {
        System.out.println("冲泡咖啡粉");
    }
    @Override
    protected void addCondiments() {
        System.out.println("加糖和牛奶");
    }
    @Override
    protected boolean wantPackage() { return true; }
}

// 具体子类：茶
public class TeaMaker extends BeverageMaker {
    @Override
    protected void brew() {
        System.out.println("浸泡茶叶");
    }
    @Override
    protected void addCondiments() {
        System.out.println("加柠檬");
    }
}
```

#### Spring中的模板方法——AbstractApplicationContext.refresh()

Spring IoC容器的核心刷新流程就是模板方法的典范：

```java
// Spring源码：AbstractApplicationContext.refresh() 简化版
@Override
public void refresh() throws BeansException, IllegalStateException {
    synchronized (this.startupShutdownMonitor) {
        // 1. 准备刷新上下文
        prepareRefresh();

        // 2. 获取BeanFactory（抽象方法，子类实现）
        ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

        // 3. 准备BeanFactory
        prepareBeanFactory(beanFactory);

        // 4. 子类扩展点：允许子类在BeanFactory初始化后做额外处理
        postProcessBeanFactory(beanFactory);

        // 5. 调用BeanFactoryPostProcessor
        invokeBeanFactoryPostProcessors(beanFactory);

        // 6. 注册BeanPostProcessor
        registerBeanPostProcessors(beanFactory);

        // 7. 初始化消息源
        initMessageSource();

        // 8. 初始化事件多播器
        initApplicationEventMulticaster();

        // 9. 子类扩展点：初始化其他特殊的Bean（模板方法模式的关键钩子）
        onRefresh();

        // 10. 注册监听器
        registerListeners();

        // 11. 实例化所有非延迟加载的单例Bean
        finishBeanFactoryInitialization(beanFactory);

        // 12. 完成刷新
        finishRefresh();
    }
}
```

`refresh()` 方法的骨架固定不变，但其中的 `obtainFreshBeanFactory()`、`postProcessBeanFactory()`、`onRefresh()` 等都是由子类实现的抽象或空方法——这就是模板方法模式的精髓。

其他Spring模板方法应用：`JdbcTemplate`、`RestTemplate`、`JmsTemplate`。

### 4.3 观察者模式（Observer Pattern）

**概念**：定义对象间的一种一对多依赖关系，当一个对象的状态发生改变时，所有依赖于它的对象都得到通知并被自动更新。

**适用场景**：事件驱动系统、消息通知、状态同步、发布-订阅模型。

```java
// Java原生实现
import java.util.Observable;
import java.util.Observer;

// 被观察者：新闻发布者
public class NewsPublisher extends Observable {
    private String latestNews;

    public void publishNews(String news) {
        this.latestNews = news;
        System.out.println("发布新闻: " + news);

        setChanged(); // 标记状态已改变
        notifyObservers(news); // 通知所有观察者
    }
}

// 观察者：邮件订阅者
public class EmailSubscriber implements Observer {
    private String name;

    public EmailSubscriber(String name) {
        this.name = name;
    }

    @Override
    public void update(Observable o, Object arg) {
        String news = (String) arg;
        System.out.println(name + " 收到邮件通知: " + news);
    }
}

// 使用
NewsPublisher publisher = new NewsPublisher();
publisher.addObserver(new EmailSubscriber("张三"));
publisher.addObserver(new EmailSubscriber("李四"));
publisher.publishNews("Java 21发布啦！");
```

#### Spring事件机制

Spring的事件机制是观察者模式的进阶实现，功能更强大且与IoC容器无缝集成。

```java
// 1. 自定义事件
public class OrderCreatedEvent extends ApplicationEvent {
    private final Long orderId;
    private final String username;

    public OrderCreatedEvent(Object source, Long orderId, String username) {
        super(source);
        this.orderId = orderId;
        this.username = username;
    }

    public Long getOrderId() { return orderId; }
    public String getUsername() { return username; }
}

// 2. 事件监听器（两种方式）

// 方式一：实现接口
@Component
public class EmailNotificationListener implements ApplicationListener<OrderCreatedEvent> {
    @Override
    public void onApplicationEvent(OrderCreatedEvent event) {
        System.out.println("发送邮件通知: 用户 " + event.getUsername()
                + " 的订单 " + event.getOrderId() + " 已创建");
    }
}

// 方式二：@EventListener 注解（推荐——更简洁）
@Component
public class SmsNotificationListener {
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        System.out.println("发送短信通知: 用户 " + event.getUsername()
                + " 的订单 " + event.getOrderId() + " 已创建");
    }

    // 一个监听器处理多个事件
    @EventListener({OrderCreatedEvent.class, OrderCancelledEvent.class})
    public void handleOrderEvents(ApplicationEvent event) {
        // 通用处理
    }

    // 支持SpEL条件过滤
    @EventListener(condition = "#event.username.equals('vip')")
    public void handleVipOrder(OrderCreatedEvent event) {
        // 只处理VIP用户的订单
    }
}

// 3. 发布事件
@Service
public class OrderService {
    @Autowired
    private ApplicationEventPublisher publisher;

    @Transactional
    public void createOrder(Order order) {
        // 业务逻辑...
        orderDao.save(order);

        // 发布事件（同步/异步取决于配置）
        publisher.publishEvent(new OrderCreatedEvent(this, order.getId(), order.getUsername()));
    }
}
```

**@Async + @EventListener 实现异步监听**：

```java
@Component
public class LoggingListener {
    @Async // 异步执行，不影响主流程
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 日志记录等非核心操作
        Thread.sleep(1000); // 模拟耗时
        System.out.println("异步记录日志: 订单 " + event.getOrderId());
    }
}
```

#### Guava EventBus

Google Guava提供的轻量级事件总线，无需Spring容器即可使用：

```java
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

// 事件
public class OrderEvent {
    private final Long orderId;
    // constructors, getters...
}

// 监听器
public class OrderEventListener {
    @Subscribe
    public void onOrderCreated(OrderEvent event) {
        System.out.println("Guava EventBus 收到订单事件: " + event.getOrderId());
    }
}

// 使用
EventBus eventBus = new EventBus("order-bus");
eventBus.register(new OrderEventListener());
eventBus.post(new OrderEvent(1001L));
```

### 4.4 责任链模式（Chain of Responsibility Pattern）

**概念**：将请求的发送者和接收者解耦，使多个对象都有机会处理请求。将这些对象连成一条链，并沿着这条链传递请求，直到有一个对象处理它为止。

**适用场景**：过滤器、拦截器、审批流（请假/报销）、日志框架（Level过滤）。

```java
// 抽象处理器
public abstract class Approver {
    protected Approver next; // 下一个审批人

    public void setNext(Approver next) {
        this.next = next;
    }

    // 提交审批
    public abstract void approve(LeaveRequest request);

    // 模板方法：交给下一个处理
    protected void nextApprove(LeaveRequest request) {
        if (next != null) {
            next.approve(request);
        } else {
            System.out.println("审批完成，无人继续处理");
        }
    }
}

// 具体处理器
public class TeamLeader extends Approver {
    @Override
    public void approve(LeaveRequest request) {
        if (request.getDays() <= 3) {
            System.out.println("组长审批通过: " + request.getName() + " 请假 " + request.getDays() + "天");
        } else {
            System.out.println("组长无法审批，转交上级");
            nextApprove(request);
        }
    }
}

public class Manager extends Approver {
    @Override
    public void approve(LeaveRequest request) {
        if (request.getDays() <= 7) {
            System.out.println("经理审批通过: " + request.getName() + " 请假 " + request.getDays() + "天");
        } else {
            System.out.println("经理无法审批，转交上级");
            nextApprove(request);
        }
    }
}

public class Director extends Approver {
    @Override
    public void approve(LeaveRequest request) {
        System.out.println("总监审批通过: " + request.getName() + " 请假 " + request.getDays() + "天");
    }
}

// 请假请求
public class LeaveRequest {
    private String name;
    private int days;
    // constructor, getters...
}

// 客户端：组装责任链
TeamLeader teamLeader = new TeamLeader();
Manager manager = new Manager();
Director director = new Director();
teamLeader.setNext(manager);
manager.setNext(director);

// 提交请求
teamLeader.approve(new LeaveRequest("张三", 2));  // 组长审批
teamLeader.approve(new LeaveRequest("李四", 5));  // 经理审批
teamLeader.approve(new LeaveRequest("王五", 10)); // 总监审批
```

#### Filter链（Servlet）

```java
@WebFilter("/*")
public class LoggingFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        System.out.println("请求前: 记录日志");
        // 调用下一个Filter（关键：继续传递）
        chain.doFilter(request, response);
        System.out.println("请求后: 记录日志");
    }
}
```

#### Interceptor链（Spring MVC）

```java
@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            response.setStatus(401);
            return false; // 阻断链，不再执行后续拦截器和处理器
        }
        return true; // 继续执行链上下一个拦截器或处理器
    }
}
```

#### Sentinel规则链

Sentinel的职责链模式是其核心架构，不同`Slot`各司其职，一链处理流量控制的各个环节：

```java
// Sentinel 源码简化
public class DefaultProcessorSlotChain extends ProcessorSlotChain {
    @Override
    public void entry(Context context, ResourceWrapper resourceWrapper, Object t, int count, Object... args) {
        first.transformEntry(context, resourceWrapper, t, count, args);
    }
}

// 各个Slot组成责任链：
// NodeSelectorSlot → ClusterBuilderSlot → StatisticSlot → FlowSlot → DegradeSlot → SystemSlot
```

### 4.5 命令模式（Command Pattern）

**概念**：将请求封装为对象，从而可以用不同的请求对客户进行参数化、对请求排队或记录请求日志、支持可撤销操作。

**适用场景**：Runnable接口、线程池、操作队列、事务回滚、编辑器撤销。

```java
// 命令接口
public interface Command {
    void execute();
    void undo(); // 可撤销
}

// 接收者：实际执行操作的对象
public class Light {
    public void on() { System.out.println("打开电灯"); }
    public void off() { System.out.println("关闭电灯"); }
}

// 具体命令：开灯
public class LightOnCommand implements Command {
    private Light light;

    public LightOnCommand(Light light) {
        this.light = light;
    }

    @Override
    public void execute() { light.on(); }
    @Override
    public void undo() { light.off(); }
}

// 具体命令：关灯
public class LightOffCommand implements Command {
    private Light light;
    public LightOffCommand(Light light) { this.light = light; }
    @Override
    public void execute() { light.off(); }
    @Override
    public void undo() { light.on(); }
}

// 调用者：遥控器
public class RemoteControl {
    private Command command;
    private Command lastCommand;

    public void setCommand(Command command) {
        this.command = command;
    }

    public void pressButton() {
        command.execute();
        lastCommand = command; // 记录上一步操作
    }

    public void pressUndo() {
        if (lastCommand != null) {
            lastCommand.undo();
        }
    }
}
```

**Runnable与线程池**——命令模式的日常应用：

```java
// Runnable就是一个命令接口
Runnable task = () -> System.out.println("执行任务");

// 线程池作为调用者，将Runnable放入工作队列
ExecutorService executor = Executors.newFixedThreadPool(10);
executor.execute(task); // 命令模式：将请求封装为对象交给调用者
executor.execute(() -> System.out.println("另一个任务"));
executor.shutdown();
```

### 4.6 状态模式（State Pattern）

**概念**：允许一个对象在其内部状态改变时改变它的行为，对象看起来似乎修改了它的类。

**适用场景**：订单状态流转、游戏角色状态、工作流引擎、有限状态机。

```java
// 订单状态上下文
public class OrderContext {
    private OrderState currentState;

    public OrderContext() {
        this.currentState = new PendingPaymentState(); // 初始状态：待支付
    }

    public void setState(OrderState state) {
        this.currentState = state;
    }

    public void pay() {
        currentState.pay(this);
    }

    public void ship() {
        currentState.ship(this);
    }

    public void confirm() {
        currentState.confirm(this);
    }

    public void cancel() {
        currentState.cancel(this);
    }
}

// 状态接口
public interface OrderState {
    void pay(OrderContext context);
    void ship(OrderContext context);
    void confirm(OrderContext context);
    void cancel(OrderContext context);
}

// 具体状态：待支付
public class PendingPaymentState implements OrderState {
    @Override
    public void pay(OrderContext context) {
        System.out.println("支付成功");
        context.setState(new PaidState());
    }
    @Override
    public void ship(OrderContext context) {
        System.out.println("请先支付"); // 非法操作
    }
    @Override
    public void confirm(OrderContext context) {
        System.out.println("请先支付");
    }
    @Override
    public void cancel(OrderContext context) {
        System.out.println("取消订单（未支付）");
        context.setState(new CancelledState());
    }
}

// 具体状态：已支付
public class PaidState implements OrderState {
    @Override
    public void pay(OrderContext context) {
        System.out.println("订单已支付，不能重复支付");
    }
    @Override
    public void ship(OrderContext context) {
        System.out.println("发货成功");
        context.setState(new ShippedState());
    }
    @Override
    public void confirm(OrderContext context) {
        System.out.println("请先等待发货");
    }
    @Override
    public void cancel(OrderContext context) {
        System.out.println("取消订单（已支付，需退款）");
        context.setState(new CancelledState());
    }
}

// 具体状态：已发货
public class ShippedState implements OrderState {
    @Override
    public void pay(OrderContext context) { System.out.println("已支付"); }
    @Override
    public void ship(OrderContext context) { System.out.println("已发货"); }
    @Override
    public void confirm(OrderContext context) {
        System.out.println("确认收货成功");
        context.setState(new CompletedState());
    }
    @Override
    public void cancel(OrderContext context) {
        System.out.println("取消订单（需退回商品）");
    }
}

// 具体状态：已完成 / 已取消
public class CompletedState implements OrderState {
    @Override
    public void pay(OrderContext context) { System.out.println("已完成"); }
    @Override
    public void ship(OrderContext context) { System.out.println("已完成"); }
    @Override
    public void confirm(OrderContext context) { System.out.println("已完成"); }
    @Override
    public void cancel(OrderContext context) { System.out.println("已完成订单不可取消"); }
}

public class CancelledState implements OrderState {
    @Override
    public void pay(OrderContext context) { System.out.println("订单已取消"); }
    @Override
    public void ship(OrderContext context) { System.out.println("订单已取消"); }
    @Override
    public void confirm(OrderContext context) { System.out.println("订单已取消"); }
    @Override
    public void cancel(OrderContext context) { System.out.println("订单已取消"); }
}

// 使用
OrderContext order = new OrderContext();
order.pay();    // 支付成功 → 状态变为已支付
order.ship();   // 发货成功 → 状态变为已发货
order.confirm();// 确认收货成功 → 状态变为已完成
```

**状态模式 vs 策略模式**：两者结构相似，但目的不同。策略模式的策略由客户端主动选择，彼此独立；状态模式的状态是自动流转的，状态之间有关联。

---

## 五、项目实践指南

### 5.1 如何选择设计模式

设计模式的选用应该**根据实际场景反推**，而不是先列出所有模式再从中挑选。推荐的决策流程：

1. **识别变化点**：找到代码中"可能发生变化"的部分——什么是需要扩展的？哪里是经常改的？
2. **封装变化**：将变化的部分封装起来，不要让变化扩散到整个系统。
3. **看看模式工具箱**：看看经典模式是否正好解决了这个场景的问题。

一个简单的对照思路：

| 你的问题 | 对应的模式 |
|---------|-----------|
| 需要全局唯一实例 | 单例模式 |
| 需要一套可互相替换的算法 | 策略模式 |
| 需要为对象动态增加功能 | 装饰器模式 |
| 需要控制对象访问 | 代理模式 |
| 需要统一处理一系列相关对象的创建 | 工厂方法或抽象工厂 |
| 需要在不修改代码的情况下新增功能 | 开闭原则 → 策略/模板方法/装饰器 |
| 需要在多个对象间传递请求直到有人处理 | 责任链模式 |
| 需要一个对象状态变化时通知其他对象 | 观察者模式 |
| 对象创建过程很复杂，构造器参数太多 | 建造者模式 |

**核心原则：先有设计问题，后有设计模式。不要为了用设计模式而写代码。**

### 5.2 过度设计警示

设计模式是一把双刃剑。滥用设计模式会导致代码复杂度急剧上升，得不偿失。

**常见过度设计场景：**

1. **用一个模式硬套简单逻辑**：
```java
// ❌ 错误：只有两种支付方式，却硬套策略模式+工厂模式+单例+
// 还搞了个支付策略工厂+反射注册……

// ✅ 正确：先用简单的 if-else，当支付方式增长到4-5个以上时再重构为策略模式
```

2. **抽象工厂套娃**：
```java
// ❌ 错误：只有一种实现，却创建了接口→抽象类→工厂接口→具体工厂四层结构
```
当某个接口只有1-2个实现且很长时间内都不会增加时，不需要抽象工厂模式。

3. **过度设计真的比不够设计更糟糕**：
   - 不够设计 → 代码耦合度高，但逻辑清晰（容易看到）。
   - 过度设计 → 复杂的类层次结构，变相增加了理解和维护成本。

**实用的建议**：
- 先写出能工作的简单代码。
- 当发现修改代码变得困难、同一个地方频繁修改时重构。
- 重构时引入设计模式，而不是从一开始就预测未来需求。

### 5.3 组合优于继承——设计模式的核心思想

面向对象设计中一个被反复强调的原则：**Favor composition over inheritance（组合优于继承）**。

| 方式 | 定义 | 优缺点 |
|------|-----|--------|
| 继承 | `class B extends A` | 编译期确定，静态复用，破坏封装，子类依赖父类实现 |
| 组合 | `class B { private A a; }` | 运行期可替换，动态灵活，不破坏封装，更低的耦合 |

许多设计模式的本质都是"用组合替代继承"：

| 模式 | 如何体现组合 |
|------|------------|
| 策略模式 | 上下文持有策略对象的引用（组合），运行时可替换 |
| 装饰器模式 | 装饰器持有被装饰者引用，层层组合添加功能 |
| 适配器模式 | 对象适配器持有被适配者引用，通过组合适配接口 |
| 观察者模式 | 被观察者持有观察者列表，通过组合实现通知 |
| 命令模式 | 调用者持有命令对象引用，通过组合实现参数化 |

### 5.4 Spring中的设计模式复习

Spring框架本身是设计模式的"活教材"，在每个角落都能看到设计模式的身影：

| 设计模式 | Spring中的应用 |
|---------|--------------|
| **单例模式** | Bean默认作用域`@Scope("singleton")`，单例注册表 |
| **工厂方法** | `FactoryBean`接口、`BeanFactory` |
| **抽象工厂** | `BeanFactory`的不同实现（`XmlBeanFactory`, `AnnotationConfigApplicationContext`） |
| **建造者模式** | `BeanDefinitionBuilder`、`MvcUriComponentsBuilder` |
| **原型模式** | `@Scope("prototype")`、`@Lookup`注解 |
| **代理模式** | AOP（`@Transactional`、`@Cacheable`）、`ProxyFactoryBean` |
| **适配器模式** | `HandlerAdapter`适配不同Controller |
| **装饰器模式** | `BeanWrapper`对Bean实例的装饰、`HttpHeadersDecorator` |
| **享元模式** | 连接池、Bean池 |
| **策略模式** | `Resource`接口的不同实现、`InstantiationStrategy` |
| **模板方法** | `AbstractApplicationContext.refresh()`、`JdbcTemplate`、`RestTemplate` |
| **观察者模式** | `ApplicationEvent` + `ApplicationListener`事件机制 |
| **责任链模式** | `HandlerInterceptor`链、`Filter`链 |
| **命令模式** | `PlatformTransactionManager`将事务抽象为命令 |
| **桥接模式** | Spring JDBC的驱动管理 |
| **外观模式** | `JdbcTemplate`封装了JDBC的复杂性，提供简洁API |
| **访问者模式** | `PropertyAccessor`、`BeanDefinitionVisitor` |

深入理解Spring源码是学习设计模式最高效的途径之一。建议从`AbstractApplicationContext.refresh()`方法开始，逐行追踪其调用链，你会发现每步操作背后都有设计模式在支撑。

---

## 六、总结——设计模式快速参考表

| 分类 | 模式 | 关键词 | 一句话识别 | 实际应用 |
|------|------|-------|-----------|---------|
| 创建型 | 单例 | 一个实例 | 全局唯一，`getInstance()` | Spring Bean、Logger、Runtime |
| 创建型 | 工厂方法 | 一个产品 | 定义一个创建对象的接口，让子类决定实例化哪个类 | `FactoryBean`、`Collection.iterator()` |
| 创建型 | 抽象工厂 | 产品族 | 创建一整套相关产品 | `DocumentBuilderFactory` |
| 创建型 | 建造者 | 分步构建 | 链式调用，`build()`返回产品 | `@Builder`、`StringBuilder` |
| 创建型 | 原型 | 克隆 | `clone()`创建副本 | `@Scope("prototype")` |
| 结构型 | 代理 | 控制访问 | 在调用目标前后插入逻辑 | AOP、延迟加载、权限控制 |
| 结构型 | 适配器 | 接口转换 | 让不兼容的接口一起工作 | `HandlerAdapter` |
| 结构型 | 装饰器 | 动态增强 | 套娃式叠加功能 | `BufferedInputStream` |
| 结构型 | 享元 | 共享复用 | 池化、缓存复用 | 连接池、`Integer.valueOf()` |
| 结构型 | 组合 | 树形结构 | 部分-整体统一处理 | 文件系统、菜单树 |
| 结构型 | 外观 | 统一入口 | 封装复杂子系统，提供简化接口 | `JdbcTemplate` |
| 结构型 | 桥接 | 抽象与实现分离 | 两个独立变化的维度 | JDBC驱动 |
| 行为型 | 策略 | 算法族 | 不同的算法可互相替换 | `Comparator`、支付方式 |
| 行为型 | 模板方法 | 骨架 | 父类定义流程，子类实现细节 | `refresh()`、`JdbcTemplate` |
| 行为型 | 观察者 | 发布订阅 | 一个变化通知多个依赖者 | Spring Event机制 |
| 行为型 | 责任链 | 链式处理 | 请求沿着链传递直到被处理 | Filter、Interceptor、审批流 |
| 行为型 | 命令 | 请求封装 | 将请求封装为对象，支持排队/撤销 | `Runnable`、线程池 |
| 行为型 | 状态 | 状态流转 | 状态改变，行为改变 | 订单状态机 |
| 行为型 | 迭代器 | 遍历 | 提供统一方式遍历集合元素 | `Iterator`、`for-each` |
| 行为型 | 访问者 | 数据与操作分离 | 在不改变元素类的前提下增加操作 | `BeanDefinitionVisitor` |
| 行为型 | 中介者 | 对象间解耦 | 通过中介者协调对象间交互 | `DispatcherServlet` |
| 行为型 | 解释器 | 语法解析 | 定义语言的文法，解释句子 | Spring EL表达式 |
| 行为型 | 备忘录 | 状态恢复 | 保存和恢复对象内部状态 | 序列化、事务回滚 |

---

> **最后的话**：设计模式不是银弹，而是工具箱中的工具。好的开发者在合适的时候拿起合适的工具，而不是拿着锤子看什么都像钉子。真正的高手写的代码中，设计模式是"化于无形"的——你感觉不到模式的存在，但代码就是那么清晰、优雅、易于扩展。这需要大量的练习和思考，希望这篇总结能为你打下坚实的基础。
