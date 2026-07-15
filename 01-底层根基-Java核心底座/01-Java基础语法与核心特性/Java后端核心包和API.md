# Java 后端核心包和 API

> **定位**：Java 后端核心 API 均来自 JDK 内置包，无需额外依赖，是后端开发的"基本功"。覆盖日常开发 80% 以上的基础场景，后续框架（Spring、MyBatis）均基于这些核心 API 封装。

---

## 目录

1. [java.lang — 核心基础包](#1-javalang--核心基础包)
2. [java.util — 工具类集合包](#2-javautil--工具类集合包)
3. [java.io / java.nio — IO 文件操作包](#3-javaio--javanio--io-文件操作包)
4. [java.sql — 数据库操作包](#4-javasql--数据库操作包)
5. [java.net — 网络编程包](#5-javanet--网络编程包)
6. [核心 API 使用注意事项](#6-核心-api-使用注意事项)

---

## 1. java.lang — 核心基础包

> JVM 自动导入，无需手动 `import`。包含 Java 语言最基础的类和接口。

### 核心类与高频 API

| 类 | 说明 | 高频方法 |
|----|------|----------|
| **`String`** | 字符串操作核心 | `equals()`、`length()`、`substring()`、`replace()`、`split()` |
| **`Integer`** | int 包装类 | `valueOf()`、`parseInt()`、`toString()` |
| **`Long`** | long 包装类 | `valueOf()`、`parseLong()` |
| **`Double`** | double 包装类 | `valueOf()`、`parseDouble()` |
| **`Boolean`** | boolean 包装类 | `valueOf()`、`parseBoolean()` |
| **`Object`** | 所有类的根类 | `equals()`、`hashCode()`、`toString()`、`wait()`/`notify()` |
| **`Thread`** | 线程类 | `start()`、`run()`、`sleep()`、`join()` |
| **`Runnable`** | 线程接口 | `run()` |
| **`System`** | 系统工具 | `out.println()`、`currentTimeMillis()`、`arraycopy()` |

### 常用代码示例

```java
// String 操作
String text = "Hello,Java,World";
String[] parts = text.split(",");      // ["Hello", "Java", "World"]
String sub = text.substring(0, 5);     // "Hello"
boolean eq = "abc".equals("abc");      // true

// 包装类：基本类型 ↔ 字符串
int num = Integer.parseInt("123");     // 字符串 → int
String str = Integer.toString(456);    // int → 字符串

// Object 方法
@Override
public boolean equals(Object obj) { ... }

@Override
public int hashCode() { ... }

// 线程
new Thread(() -> {
    System.out.println("新线程运行");
}).start();
```

---

## 2. java.util — 工具类集合包

> 后端开发中使用频率最高的包，覆盖集合、日期、随机数、比较器等。

### 2.1 集合框架（核心中的核心）

| 接口 | 特点 | 主要实现类 |
|------|------|-----------|
| `List` | 有序可重复 | `ArrayList` |
| `Map` | 键值对 | `HashMap` |
| `Set` | 无序不可重复 | `HashSet` |

**ArrayList**：

| 方法 | 用途 |
|------|------|
| `add(E e)` | 添加元素 |
| `get(int index)` | 按索引获取 |
| `remove(int index)` / `remove(Object o)` | 删除元素 |
| `size()` | 获取长度 |
| `forEach(Consumer)` | Lambda 遍历 |

**HashMap**：

| 方法 | 用途 |
|------|------|
| `put(K key, V value)` | 添加键值对 |
| `get(Object key)` | 按键取值 |
| `containsKey(Object key)` | 判断键是否存在 |
| `keySet()` | 获取所有键 |
| `entrySet()` | 遍历键值对 |

**HashSet**：

| 方法 | 用途 |
|------|------|
| `add(E e)` | 添加元素 |
| `contains(Object o)` | 判断是否存在 |
| `remove(Object o)` | 删除元素 |

```java
// ArrayList
List<String> list = new ArrayList<>();
list.add("Java");
list.add("Python");
list.forEach(System.out::println);

// HashMap
Map<String, Integer> map = new HashMap<>();
map.put("apple", 3);
map.put("banana", 5);
for (Map.Entry<String, Integer> entry : map.entrySet()) {
    System.out.println(entry.getKey() + ": " + entry.getValue());
}

// HashSet
Set<Integer> set = new HashSet<>();
set.add(1);
set.add(2);
set.add(1);  // 重复元素不会添加
System.out.println(set.size());  // 2
```

### 2.2 日期时间（JDK 8+ 推荐）

| 类 | 说明 | 高频方法 |
|----|------|----------|
| `LocalDate` | 日期（年月日） | `now()`、`of()`、`plusDays()`、`minusDays()` |
| `LocalTime` | 时间（时分秒） | `now()`、`of()`、`plusHours()` |
| `LocalDateTime` | 日期+时间 | `now()`、`of()`、`format()` |
| `DateTimeFormatter` | 格式化器 | `ofPattern("yyyy-MM-dd")`、`format()`、`parse()` |

```java
LocalDate today = LocalDate.now();                  // 2026-07-15
LocalDate nextWeek = today.plusDays(7);
LocalDateTime now = LocalDateTime.now();

DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
String formatted = now.format(fmt);
LocalDateTime parsed = LocalDateTime.parse("2026-07-15 10:30:00", fmt);
```

> ⚠️ 优先使用 `LocalDateTime` 等新 API，替代旧的 `Date`、`Calendar`（线程不安全、API 繁琐）。

### 2.3 其他实用工具

| 类 | 用途 | 高频方法 |
|----|------|----------|
| `Optional` | 空值处理，防 NPE | `ofNullable()`、`isPresent()`、`orElse()`、`ifPresent()` |
| `Random` | 随机数 | `nextInt()`、`nextDouble()` |
| `Collections` | 集合工具 | `sort()`、`reverse()`、`emptyList()` |

```java
// Optional 防空指针
String name = null;
String display = Optional.ofNullable(name).orElse("匿名用户");

// Collections 工具
List<Integer> nums = Arrays.asList(3, 1, 2);
Collections.sort(nums);           // [1, 2, 3]
Collections.reverse(nums);        // [3, 2, 1]
```

---

## 3. java.io / java.nio — IO 文件操作包

### 3.1 java.io（传统 IO）

| 类 | 用途 | 高频方法 |
|----|------|----------|
| `File` | 文件/目录操作 | `exists()`、`createNewFile()`、`delete()`、`listFiles()` |
| `BufferedReader` | 字符流读取 | `readLine()`、`close()` |
| `BufferedWriter` | 字符流写入 | `write()`、`newLine()`、`close()` |
| `InputStream` | 字节流读取（抽象类） | `read()`、`close()` |
| `OutputStream` | 字节流写入（抽象类） | `write()`、`close()` |

```java
// 文件读取（try-with-resources 自动关闭）
try (BufferedReader reader = new BufferedReader(new FileReader("data.txt"))) {
    String line;
    while ((line = reader.readLine()) != null) {
        System.out.println(line);
    }
}

// 文件写入
try (BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"))) {
    writer.write("Hello, Java IO");
    writer.newLine();
}
```

### 3.2 java.io vs java.nio

| 维度 | `java.io`（传统 IO） | `java.nio`（NIO） |
|------|---------------------|-------------------|
| 模型 | 阻塞 IO（BIO） | 非阻塞 IO（NIO） |
| 核心类 | `File`、`Stream` | `ByteBuffer`、`FileChannel`、`Charset` |
| 性能 | 一般 | ✅ 更优 |
| 适用场景 | 小文件、简单读写 | 大文件、高并发网络通信 |

---

## 4. java.sql — 数据库操作包

> JDBC 编程基础，MyBatis、JPA 等框架均基于此封装。

### 核心类与 API

| 类/接口 | 用途 | 高频方法 |
|----------|------|----------|
| `DriverManager` | 获取数据库连接 | `getConnection(url, user, password)` |
| `Connection` | 数据库连接 | `createStatement()`、`prepareStatement()`、`close()` |
| `PreparedStatement` | 预编译 SQL（防注入） | `setString()`、`setInt()`、`executeQuery()`、`executeUpdate()` |
| `ResultSet` | 查询结果集 | `next()`、`getString()`、`getInt()` |

### 标准 JDBC 流程

```java
String url = "jdbc:mysql://localhost:3306/demo";
String user = "root";
String password = "123456";

// try-with-resources 自动关闭资源
try (Connection conn = DriverManager.getConnection(url, user, password);
     PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE id = ?")) {

    ps.setInt(1, 100);                          // 设置参数
    try (ResultSet rs = ps.executeQuery()) {    // 执行查询
        while (rs.next()) {
            String name = rs.getString("name");
            int age = rs.getInt("age");
            System.out.println(name + ": " + age);
        }
    }
}
```

> ⚠️ `PreparedStatement` 优先于 `Statement`——**防 SQL 注入** + 预编译提升效率。

---

## 5. java.net — 网络编程包

> TCP/UDP/HTTP 通信基础。

### 核心类与 API

| 类 | 角色 | 高频方法 |
|----|------|----------|
| `Socket` | TCP 客户端 | `getOutputStream()`、`getInputStream()`、`close()` |
| `ServerSocket` | TCP 服务端 | `accept()`、`close()` |
| `URL` | 统一资源定位 | `openStream()`、`getProtocol()`、`getPath()` |

### TCP 通信示例

```java
// 服务端
try (ServerSocket server = new ServerSocket(8080)) {
    Socket client = server.accept();  // 阻塞等待连接
    BufferedReader in = new BufferedReader(
        new InputStreamReader(client.getInputStream()));
    String msg = in.readLine();
    System.out.println("收到: " + msg);
}

// 客户端
try (Socket socket = new Socket("localhost", 8080)) {
    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
    out.println("Hello, Server");
}
```

---

## 6. 核心 API 使用注意事项

| 原则 | 说明 |
|------|------|
| **资源关闭** | IO 流、DB 连接、Socket 使用后必须关闭，优先用 **try-with-resources**（JDK 7+） |
| **String 不可变** | 频繁修改用 `StringBuilder`（非线程安全）或 `StringBuffer`（线程安全） |
| **遍历不修改** | 遍历集合时不要直接 `remove`，避免 `ConcurrentModificationException` |
| **新日期 API** | JDK 8+ 优先用 `LocalDateTime`，替代旧的 `Date`/`Calendar` |
| **防 SQL 注入** | `PreparedStatement` > `Statement` |
| **线程安全** | `HashMap` / `ArrayList` 非线程安全，并发场景用 `ConcurrentHashMap` / `CopyOnWriteArrayList` |

---

## 包速查总表

| 包 | 自动导入 | 核心功能 | 最常用类 |
|----|:--------:|----------|----------|
| `java.lang` | ✅ | 语言基础 | `String`、`Integer`、`Object`、`Thread` |
| `java.util` | ❌ | 集合 + 日期 + 工具 | `ArrayList`、`HashMap`、`LocalDateTime`、`Optional` |
| `java.io` | ❌ | 传统文件 IO | `File`、`BufferedReader`、`BufferedWriter` |
| `java.nio` | ❌ | 高性能 NIO | `ByteBuffer`、`FileChannel` |
| `java.sql` | ❌ | 数据库操作 | `Connection`、`PreparedStatement`、`ResultSet` |
| `java.net` | ❌ | 网络通信 | `Socket`、`ServerSocket`、`URL` |

---

> 🎯 **核心总结**：以上 6 个核心包是 Java 后端的"基本功"。掌握这些内容就能完成基础的业务开发、文件操作、数据库交互和网络通信。后续所有框架（Spring、MyBatis、Netty）均基于这些核心 API 封装——**熟练掌握它们是成为合格 Java 后端开发者的前提**。
