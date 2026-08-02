# 03 Java I/O 与 NIO 体系全解

> Java I/O 用了 20 年的装饰器模式让流可以灵活组合——字节流处理二进制、字符流处理文本、NIO.2 的 Files/Path 则是现代文件操作的首选

---

## 📚 目录

1. [I/O 体系全景](#1-io-体系全景)
2. [字节流 InputStream / OutputStream](#2-字节流-inputstream--outputstream)
3. [字符流 Reader / Writer](#3-字符流-reader--writer)
4. [装饰器模式实战——层层拆解](#4-装饰器模式实战层层拆解)
5. [try-with-resources 深度解析](#5-try-with-resources-深度解析)
6. [NIO.2 文件操作——Path 与 Files 工具箱](#6-nio2-文件操作path-与-files-工具箱)
7. [序列化概要](#7-序列化概要)
8. [选型速查与练习](#8-选型速查与练习)

---

## 1. I/O 体系全景

### 1.1 四大抽象基类

Java I/O 以四个抽象类为根基，所有流类都是从它们派生出来的：

| 类别 | 字节流 | 字符流 | 方向 |
|------|-------|-------|------|
| **输入** | `InputStream` | `Reader` | 数据流入程序（读操作） |
| **输出** | `OutputStream` | `Writer` | 数据流出程序（写操作） |

> 💡 口诀：**字节流处理 .class/.jpg/.zip 等二进制数据，字符流处理 .txt/.java/.xml 等文本数据**。用错类别会导致乱码或数据损坏。

### 1.2 字节流 vs 字符流核心区别

| 对比维度 | 字节流 | 字符流 |
|---------|-------|-------|
| 数据单位 | 1 字节（8 bit） | 2 字节（Unicode 码点，一个 char） |
| 基类 | `InputStream` / `OutputStream` | `Reader` / `Writer` |
| 编码感知 | 无——只读写原始字节 | 有——使用字符集编码/解码 |
| 主要用途 | 图像、视频、序列化、网络传输 | 文本处理、配置文件、日志 |
| 性能特点 | 适合大文件二进制流 | 适合字符密集型操作（搭配缓冲后性能佳） |
| 底层关系 | — | **字符流底层依赖字节流**（通过 InputStreamReader/OutputStreamWriter 转换） |

> 🎯 **核心要点**：字符流并不是"比字节流更高级"，而是"字节流 + 编码表"的封装。所有数据在磁盘和网络中本质都是字节，字符流是给人类看文本的便利层。

### 1.3 输入流 vs 输出流

```text
输入流（读）                     输出流（写）
┌─────────────┐                ┌─────────────┐
│  外部数据源   │──read()──▶│   程序内存    │──write()──▶│  外部目标   │
│ 文件/网络/键盘 │                │  字节数组/对象 │                │ 文件/网络/屏幕 │
└─────────────┘                └─────────────┘                └─────────────┘
```

> ⚠️ 常见误区："输入"和"输出"是**相对于程序（内存）而言**的。从文件读数据叫"输入流"（数据进入程序），往文件写数据叫"输出流"（数据从程序出去）。

### 1.4 节点流 vs 处理流

| 类型 | 定义 | 特点 | 例子 |
|------|-----|------|------|
| **节点流** | 直接连接到数据源或目标 | 有真实的 I/O 操作，是数据的第一站 | `FileInputStream`、`FileReader`、`ByteArrayInputStream` |
| **处理流** | 包装在其他流之上 | 在数据经过时做转换、缓冲、计数等 | `BufferedInputStream`、`DataOutputStream`、`InputStreamReader` |

```text
数据源 → FileInputStream（节点流：读文件字节）
       → BufferedInputStream（处理流：加 8KB 缓冲区）
       → DataInputStream（处理流：读基本类型）
       → 程序
```

### 1.5 装饰器模式在 I/O 中的体现

Java I/O 库是**装饰器（Decorator）模式**最经典的教科书案例：

- **Component（抽象组件）**：`InputStream` / `OutputStream` / `Reader` / `Writer`
- **ConcreteComponent（具体组件）**：`FileInputStream` / `ByteArrayInputStream` / `StringReader`
- **Decorator（抽象装饰器）**：`FilterInputStream` / `FilterOutputStream` / `FilterReader` / `FilterWriter`
- **ConcreteDecorator（具体装饰器）**：`BufferedInputStream` / `DataInputStream` / `PushbackInputStream`

> 💡 **为什么叫装饰器？** 每一层包装都在原有的功能之上"装饰"了新能力——缓冲、类型读取、行号追踪——而不需要修改底层类的代码。组合的自由度是指数级的。

---

## 2. 字节流 InputStream / OutputStream

### 2.1 InputStream 核心方法

```java
// 读取一个字节（0~255），返回 -1 表示到达流末尾
public abstract int read() throws IOException;

// 读取一批字节到数组 b，返回实际读取的字节数，-1 表示末尾
public int read(byte[] b) throws IOException;

// 从偏移 off 开始读取最多 len 个字节
public int read(byte[] b, int off, int len) throws IOException;

// 跳过并丢弃 n 个字节，返回实际跳过的字节数
public long skip(long n) throws IOException;

// 返回可不阻塞读取的字节数（估计值）
public int available() throws IOException;

// 关闭流，释放系统资源（实现 AutoCloseable）
public void close() throws IOException;
```

> ⚠️ `available()` 返回的只是**估计值**，不要依赖它做精确控制。网络流可能总是返回 0。

### 2.2 OutputStream 核心方法

```java
// 写入一个字节（低 8 位，高 24 位被忽略）
public abstract void write(int b) throws IOException;

// 写入字节数组 b 的所有字节
public void write(byte[] b) throws IOException;

// 从偏移 off 开始写入 len 个字节
public void write(byte[] b, int off, int len) throws IOException;

// 冲刷缓冲区，确保所有数据写出到目标
public void flush() throws IOException;

// 关闭流（实现 AutoCloseable）
public void close() throws IOException;
```

### 2.3 FileInputStream / FileOutputStream

读写文件最基本的节点流。

```java
// ---------- 传统写法（Java 7 之前，不推荐） ----------
FileInputStream in = null;
try {
    in = new FileInputStream("data.bin");
    int b;
    while ((b = in.read()) != -1) {   // 逐字节读，极慢！
        process(b);
    }
} finally {
    if (in != null) in.close();
}

// ---------- 推荐的 try-with-resources 写法 ----------
try (FileInputStream in = new FileInputStream("data.bin");
     FileOutputStream out = new FileOutputStream("out.bin")) {
    byte[] buf = new byte[8192];       // 8KB 缓冲区
    int len;
    while ((len = in.read(buf)) != -1) {
        out.write(buf, 0, len);
    }
} // 自动 close
```

> 🎯 **核心要点**：
> - `FileInputStream(String name)` 接受文件路径字符串或 `File` 对象
> - `FileOutputStream` 默认**覆盖**文件；使用 `FileOutputStream(f, true)` 追加写入
> - 逐字节 `read()` 每次调用都陷入内核态，性能极差——始终用缓冲区

### 2.4 BufferedInputStream / BufferedOutputStream——缓冲原理

这两个类是"装饰器"的典型：为底层流加一个字节数组作为缓冲区。

```java
// 默认缓冲区大小 8192 字节（8KB）
// 构造时也可自定义：
try (BufferedInputStream bis = new BufferedInputStream(
         new FileInputStream("large.dat"), 65536)) {  // 64KB 缓冲区
    byte[] buf = new byte[4096];
    int len;
    while ((len = bis.read(buf)) != -1) { ... }
}
```

**缓冲原理图解：**

```text
无缓冲：
程序 ──read()──▶ 内核 ──read()──▶ 磁盘      每次 read() 都是系统调用
      ◀───────      ◀───────

有缓冲（BufferedInputStream）：
程序 ──read()──▶ 缓冲池 ──bulk read──▶ 内核 ──read()──▶ 磁盘
      ◀───────    (8KB)                只触发一次系统调用
```

> 💡 **典型误区：** 很多人认为 `BufferedInputStream` 本身很快。它的价值是**减少系统调用次数**。如果上层已经用了 8KB 的 byte[] 读取，再套 `BufferedInputStream` 就收益有限了（双层缓冲）。

### 2.5 DataInputStream / DataOutputStream

以**平台无关的方式**读写 Java 基本类型和字符串。

```java
// ---------- 写入结构化数据 ----------
try (DataOutputStream dos = new DataOutputStream(
         new BufferedOutputStream(new FileOutputStream("user.dat")))) {
    dos.writeInt(1001);         // 4 字节
    dos.writeUTF("张三");        // 先写 2 字节长度，再写 UTF-8 编码的字节
    dos.writeDouble(99.5);      // 8 字节
    dos.writeBoolean(true);     // 1 字节
}

// ---------- 读取 ----------
try (DataInputStream dis = new DataInputStream(
         new BufferedInputStream(new FileInputStream("user.dat")))) {
    int id = dis.readInt();
    String name = dis.readUTF();       // readUTF 自动读长度前缀
    double score = dis.readDouble();
    boolean active = dis.readBoolean();
}
```

> ⚠️ **必须按写入顺序读取**，否则数据错乱。`DataInputStream` 没有"标记"能力，适合固定格式的记录。

| 方法 | 写出字节数 | 说明 |
|------|-----------|------|
| `writeByte(int)` | 1 | 写出低 8 位 |
| `writeShort(int)` | 2 | Big-endian 写出 short |
| `writeInt(int)` | 4 | Big-endian 写出 int |
| `writeLong(long)` | 8 | Big-endian 写出 long |
| `writeFloat(float)` | 4 | 基于 Float.floatToIntBits |
| `writeDouble(double)` | 8 | 基于 Double.doubleToLongBits |
| `writeUTF(String)` | 2+len | 用修改版 UTF-8（长度前缀 + 内容） |
| `writeChars(String)` | 2*len | 每个字符作为 2 字节写入 |

### 2.6 ByteArrayInputStream / ByteArrayOutputStream

在**内存中的字节数组**上操作，不涉及文件或网络，是一种"内存流"。

```java
// ---------- 内存读取：将字节数组包装为流 ----------
byte[] data = {0x48, 0x65, 0x6C, 0x6C, 0x6F};  // "Hello"
try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
    int b;
    while ((b = bais.read()) != -1) {
        System.out.print((char) b);   // H e l l o
    }
}

// ---------- 内存写入：收集输出到字节数组 ----------
ByteArrayOutputStream baos = new ByteArrayOutputStream(32);  // 初始容量 32
try (DataOutputStream dos = new DataOutputStream(baos)) {
    dos.writeInt(42);
    dos.writeUTF("Hello");
}
byte[] result = baos.toByteArray();   // 获取所有写入的数据
// 注意：ByteArrayOutputStream.close() 是空实现，不需要关闭也可以
```

> 🎯 **常见用途**：
> - 在不能直接写文件的 API 中暂存输出（如生成 CSV/JSON 后转为 String）
> - 测试时模拟输入流，避免依赖真实文件
> - `ByteArrayOutputStream.toString(StandardCharsets.UTF_8)` 直接取字符串

### 2.7 ObjectInputStream / ObjectOutputStream——序列化概述

`ObjectOutputStream` 可以将 Java 对象转为字节序列（序列化），`ObjectInputStream` 可以将字节序列还原为对象（反序列化）。

```java
// ---------- 写对象 ----------
try (ObjectOutputStream oos = new ObjectOutputStream(
         new FileOutputStream("user.ser"))) {
    oos.writeObject(new User(1, "Alice", "secret"));
}

// ---------- 读对象 ----------
try (ObjectInputStream ois = new ObjectInputStream(
         new FileInputStream("user.ser"))) {
    User u = (User) ois.readObject();
}
```

> ⚠️ 使用前提：目标类必须实现 `Serializable` 接口。具体序列化细节见 [第 7 节](#7-序列化概要)。

---

## 3. 字符流 Reader / Writer

### 3.1 Reader / Writer 核心方法

```java
// ------ Reader ------
public int read() throws IOException;                    // 读一个字符（0~65535），-1 末尾
public int read(char[] cbuf) throws IOException;        // 读一批字符到数组
public int read(char[] cbuf, int off, int len) throws IOException;
public long skip(long n) throws IOException;
public boolean ready() throws IOException;              // 是否可读
public void close() throws IOException;

// ------ Writer ------
public void write(int c) throws IOException;            // 写一个字符
public void write(char[] cbuf) throws IOException;
public void write(String str) throws IOException;
public void write(String str, int off, int len) throws IOException;
public Writer append(CharSequence csq);                 // 支持 Appendable 接口
public void flush() throws IOException;
public void close() throws IOException;
```

### 3.2 FileReader / FileWriter——编码陷阱

```java
// ⚠️ 问题代码：FileReader 使用系统默认编码
try (FileReader fr = new FileReader("data.txt")) {      // 默认编码：UTF-8?
    int ch;
    while ((ch = fr.read()) != -1) {
        System.out.print((char) ch);
    }
}

// ⚠️ FileWriter 同理
try (FileWriter fw = new FileWriter("output.txt")) {    // 默认编码写文件
    fw.write("中文内容");
}
```

> ⚠️ **FileReader/FileWriter 的最大问题：无法指定编码。** 它们永远使用 `Charset.defaultCharset()`（通常是操作系统编码，Windows 上可能是 GBK，Linux 上可能是 UTF-8）。这导致代码在不同平台上行为不一致。

```text
Windows 开发：     FileReader 读 UTF-8 文件 → 乱码 ❌
Linux 部署：       FileReader 读 UTF-8 文件 → 正常 ✅
JVM 参数指定编码：  -Dfile.encoding=UTF-8 后改变 → 不可控
```

> 🎯 **最佳实践：永远不要直接使用 FileReader / FileWriter 处理跨平台场景。** 使用 `InputStreamReader` 显式指定编码。

### 3.3 BufferedReader / BufferedWriter

```java
// ---------- 读取文本的标准模板 ----------
Path path = Paths.get("large.txt");
try (BufferedReader br = new BufferedReader(new InputStreamReader(
         new FileInputStream(path.toFile()), StandardCharsets.UTF_8))) {
    String line;
    while ((line = br.readLine()) != null) {   // readLine() 不返回行尾换行符
        process(line);
    }
}

// ---------- 写入文本的标准模板 ----------
try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
         new FileOutputStream("output.txt"), StandardCharsets.UTF_8))) {
    bw.write("第一行");
    bw.newLine();                              // 跨平台换行（\n 或 \r\n）
    bw.write("第二行");
    bw.newLine();
}

// ---------- Java 8 更简洁的写法 ----------
Files.write(Paths.get("output.txt"), lines, StandardCharsets.UTF_8);
Files.readAllLines(Paths.get("input.txt"), StandardCharsets.UTF_8);
```

> 💡 `BufferedReader.readLine()` 不包含换行符，且当返回 `null` 时表示到达文件末尾。`BufferedWriter.newLine()` 写入系统属性 `line.separator` 定义的换行符，跨平台友好。

### 3.4 InputStreamReader / OutputStreamWriter——字节↔字符桥梁

这两个类是最重要的转换流，是"字节流到字符流"的桥梁。

```java
// ---------- 指定编码读取 ----------
// 正确的现代写法
try (BufferedReader br = new BufferedReader(
         new InputStreamReader(new FileInputStream("data.txt"), StandardCharsets.UTF_8))) {
    String line;
    while ((line = br.readLine()) != null) {
        System.out.println(line);
    }
}

// ---------- 指定编码写入 ----------
try (BufferedWriter bw = new BufferedWriter(
         new OutputStreamWriter(new FileOutputStream("data.txt"), StandardCharsets.UTF_8))) {
    bw.write("中文内容");
}

// ---------- 指定编码的不同方式 ----------
// 方式一：StandardCharsets 常量（Java 7+，推荐）
new InputStreamReader(in, StandardCharsets.UTF_8)

// 方式二：Charset.forName() 字符串（兼容性好但易拼写错误）
new InputStreamReader(in, Charset.forName("UTF-8"))

// 方式三：直接传 String（已过时，不推荐）
new InputStreamReader(in, "UTF-8")     // 可能会抛 UnsupportedCharsetException
```

| `StandardCharsets` 常量 | 对应字符集 | 字节数/字符 |
|------------------------|-----------|------------|
| `StandardCharsets.UTF_8` | UTF-8 | 1~4（英文 1 字节，中文 3 字节） |
| `StandardCharsets.UTF_16` | UTF-16 | 2 或 4 |
| `StandardCharsets.ISO_8859_1` | Latin-1 | 1（不支持中文） |
| `StandardCharsets.US_ASCII` | ASCII | 1（仅 0~127） |

> 🎯 **核心要点**：`InputStreamReader` 读字节时将其按指定编码解码为字符；`OutputStreamWriter` 写字符时将其按指定编码编码为字节。**现代代码中应当显式指定 `StandardCharsets.UTF_8`，而不是依赖默认编码。**

### 3.5 StringReader / StringWriter

将字符串包装为字符流，适合在需要 Reader/Writer 参数的 API 中传递字符串。

```java
// ---------- StringReader ----------
String data = "Hello\nWorld\nJava";
try (StringReader sr = new StringReader(data);
     BufferedReader br = new BufferedReader(sr)) {
    br.lines().forEach(System.out::println);   // Java 8 的 lines() 方法
}

// ---------- StringWriter：收集输出到 String ----------
StringWriter sw = new StringWriter();
try (PrintWriter pw = new PrintWriter(sw)) {
    pw.println("Line 1");
    pw.println("Line 2");
}
String result = sw.toString();  // "Line 1\nLine 2\n"
// 注意：StringWriter 不需要关闭，close() 是空实现
```

### 3.6 PrintWriter——自动 flush 与不抛 IOException 的设计争议

`PrintWriter` 是 Java 中一个"异类"：它的 `print()` / `println()` 等方法**不声明抛出 `IOException`**。

```java
// ---------- PrintWriter 常见用法 ----------
try (PrintWriter pw = new PrintWriter(
         new OutputStreamWriter(new FileOutputStream("log.txt"), StandardCharsets.UTF_8),
         true)) {   // autoFlush: true → 每调用 println 就 flush
    pw.println("这是第一行日志");
    pw.printf("用户 %s 登录失败 %d 次%n", "admin", 3);
    pw.println(42);             // 自动调用 toString
}

// ---------- 检查错误 ----------
pw.checkError();    // 返回 true 表示之前有 I/O 错误，但不会抛异常
```

**设计争议分析：**

| 优点 | 缺点 |
|------|------|
| 使用方便，不需要每个 print 都 try-catch | 错误被吞没，需主动 `checkError()` 检查 |
| 符合 `PrintStream`（System.out）的惯用法 | 违反了"异常应当被及时处理"的原则 |
| 适合日志等"尽力而为"的场景 | 不适合关键数据传输 |

> 💡 **何时使用 PrintWriter：** 日志输出、控制台输出、生成文本报告等对错误容忍度高的场景。**何时避免：** 金融交易、文件传输等要求数据完整性的场景。

---

## 4. 装饰器模式实战——层层拆解

### 4.1 经典组合拆解分析

```java
new BufferedReader(
    new InputStreamReader(
        new FileInputStream("data.txt"),
        StandardCharsets.UTF_8
    )
)
```

**从内到外每一层的作用：**

```text
第 1 层：new FileInputStream("data.txt")
│   └── 节点流：打开文件，从操作系统获取文件描述符
│   └── 只提供 read() 返回 int（一个字节）或 read(byte[]) 读字节数组
│   └── 数据形态：二进制字节
│
第 2 层：new InputStreamReader(..., StandardCharsets.UTF_8)
│   └── 转换流（桥梁）：将字节流入解析为字符流出
│   └── 每次 read() 返回 int（一个 char，0~65535）
│   └── 内部维护一个 byte[] 缓冲区，按 UTF-8 解码
│   └── 功劳：指定了编码，避免 FileReader 的默认编码陷阱
│
第 3 层：new BufferedReader(...)
│   └── 缓冲流装饰器：内部维护 char[] 缓冲区（默认 8192 字符）
│   └── 提供 readLine() —— 一次读一行，这是最常用的文本读取方式
│   └── 功劳：减少字符解码次数；提供了行级读能力
```

### 4.2 为什么不能 `new BufferedReader(new FileReader(f))` 指定编码？

```java
// ❌ 无法指定编码——FileReader 内部调用的是系统默认编码
FileReader fr = new FileReader("data.txt");

// ❌ 即使套了 BufferedReader 也改不了编码
BufferedReader br = new BufferedReader(new FileReader("data.txt"));
// 编码仍然是默认的！
```

**根本原因：** `FileReader` 的构造方法直接调用 `InputStreamReader(FileInputStream)`，但没有暴露 charset 参数：

```java
// FileReader 的构造方法（JDK 源码）
public FileReader(String fileName) throws FileNotFoundException {
    super(new FileInputStream(fileName));  // 这里用了默认编码！
}
```

> 🎯 **结论：只要涉及文本文件，一律使用 `InputStreamReader` + `FileInputStream` 组合来显式指定编码。`FileReader` 和 `FileWriter` 应当被标记为 deprecated 对待。**

### 4.3 常用装饰器组合速查

| 需求 | 推荐组合 |
|------|---------|
| 读文本文件（指定编码） | `new BufferedReader(new InputStreamReader(new FileInputStream(f), UTF_8))` |
| 写文本文件（指定编码） | `new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), UTF_8))` |
| 读二进制文件（缓冲） | `new BufferedInputStream(new FileInputStream(f))` |
| 读基本类型 | `new DataInputStream(new BufferedInputStream(new FileInputStream(f)))` |
| 读对象（反序列化） | `new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)))` |
| 从 Socket 读文本 | `new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8))` |
| 从 byte[] 读文本 | `new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes), UTF_8))` |
| 写格式化输出 | `new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), UTF_8), true)` |

---

## 5. try-with-resources 深度解析

### 5.1 传统 try-finally 的问题

```java
// ❌ 传统方式：代码冗长、资源泄漏风险
InputStream in = null;
OutputStream out = null;
try {
    in = new FileInputStream("src.txt");
    out = new FileOutputStream("dst.txt");
    byte[] buf = new byte[8192];
    int len;
    while ((len = in.read(buf)) != -1) {
        out.write(buf, 0, len);
    }
} finally {
    // ⚠️ 如果 in.close() 抛异常，out.close() 就不会执行
    if (in != null) in.close();
    if (out != null) out.close();
}
```

**传统方式的三个问题：**
1. `finally` 块中也需要 try-catch 包裹 `close()`
2. 如果 `try` 块和 `finally` 块都抛异常，`finally` 的异常会覆盖 `try` 的异常（丢失原始异常信息）
3. 关闭顺序需要手动管理——后打开的资源应先关

### 5.2 try-with-resources 语法

```java
// ✅ 标准写法（Java 7+）
try (InputStream in = new FileInputStream("src.txt");
     OutputStream out = new FileOutputStream("dst.txt")) {
    byte[] buf = new byte[8192];
    int len;
    while ((len = in.read(buf)) != -1) {
        out.write(buf, 0, len);
    }
}
// in.close() 和 out.close() 自动调用，顺序与声明顺序相反（out 先关，in 后关）
```

**对应编译后生成的代码：**

```java
// 编译器生成的代码（简化示意）
InputStream in = null;
OutputStream out = null;
Throwable primaryException = null;
try {
    in = new FileInputStream("src.txt");
    out = new FileOutputStream("dst.txt");
    // ... 读写操作 ...
} catch (Throwable t) {
    primaryException = t;
    throw t;
} finally {
    if (out != null) {
        try { out.close(); } catch (Throwable t) {
            if (primaryException != null) primaryException.addSuppressed(t);
        }
    }
    if (in != null) {
        try { in.close(); } catch (Throwable t) {
            if (primaryException != null) primaryException.addSuppressed(t);
        }
    }
}
```

### 5.3 AutoCloseable vs Closeable

| 接口 | 方法 | 引入版本 | close() 是否抛 IOException |
|------|------|---------|---------------------------|
| `AutoCloseable` | `void close() throws Exception` | Java 7 | 可抛任何异常 |
| `Closeable` | `void close() throws IOException` | Java 5 | 仅抛 IOException，是 AutoCloseable 的子接口 |

```java
// Closeable 是 AutoCloseable 的子接口，同时具有幂等性要求：
// 多次调用 close() 应该没有任何效果
public interface Closeable extends AutoCloseable {
    @Override
    public void close() throws IOException;
}

public interface AutoCloseable {
    void close() throws Exception;    // 注意：可抛更广泛的 Exception
}
```

> 💡 **实际使用中**：I/O 类都实现 `Closeable`。`AutoCloseable` 更通用，资源类（如 `Connection`、`Socket`）都可以实现它来使用 try-with-resources。

### 5.4 关闭顺序——后开先关

```java
// 声明顺序: in → out
try (InputStream in = new FileInputStream("src.txt");
     OutputStream out = new FileOutputStream("dst.txt")) {
    // 使用 in 和 out
}
// 关闭顺序：out 先关（后开），in 后关（先开）
```

> 💡 **"后开先关"原则**：`out` 可能依赖 `in` 写入的数据，先关 `out` 确保数据全部 flush 到目标，再关 `in`。这是资源管理的通用原则。

如果想显式控制，可以用嵌套：

```java
try (InputStream in = new FileInputStream("src.txt")) {
    try (OutputStream out = new FileOutputStream("dst.txt")) {
        // ...
    }
}
```

### 5.5 异常抑制（Suppressed Exception）

```java
// 模拟：try 块抛异常，close() 也抛异常
try (MyResource r = new MyResource()) {
    throw new RuntimeException("主异常");
}

class MyResource implements AutoCloseable {
    @Override
    public void close() {
        throw new RuntimeException("关闭异常");
    }
}

// 上面代码捕获到的异常是 "主异常"，"关闭异常"被抑制
// 可以通过 getSuppressed() 获取
try (MyResource r = new MyResource()) {
    throw new RuntimeException("主异常");
} catch (RuntimeException e) {
    System.out.println(e.getMessage());   // "主异常"
    Throwable[] suppressed = e.getSuppressed();
    for (Throwable t : suppressed) {
        System.out.println(t.getMessage()); // "关闭异常"
    }
}
```

**异常抑制规则：**
1. `try` 块的异常是**主异常**，`close()` 的异常被附加为主异常的**被抑制异常**
2. 如果 `try` 块正常执行，但 `close()` 抛异常，则该异常直接抛出
3. 如果 `try` 块和多个 `close()` 都抛异常，只有第一个 `close()` 异常被抑制，后续的 `close()` 异常被抑制到上一个异常中（形成抑制链）

> 🎯 **核心要点：** try-with-resources 保证**原始异常不会丢失**，这是比传统 try-finally 最大的优势。排查问题时通过 `getSuppressed()` 检查被抑制的异常。

### 5.6 Java 9+ 的 effectively final 改进

Java 9 允许在 try-with-resources 中使用**已经声明且 effectively final** 的变量：

```java
// Java 7/8：必须在 try 中声明
try (BufferedReader br = new BufferedReader(...)) { ... }

// Java 9+：可以引用外部变量（必须是 effectively final）
BufferedReader br = new BufferedReader(new FileReader("data.txt"));
try (br) {               // ✅ Java 9 支持
    String line;
    while ((line = br.readLine()) != null) { ... }
}
// br 在此处已关闭

// 更实用的场景：两个资源都用外部变量
BufferedReader in = Files.newBufferedReader(Paths.get("a.txt"));
BufferedWriter out = Files.newBufferedWriter(Paths.get("b.txt"));
try (in; out) {         // ✅ 同时管理多个外部资源
    // 读写操作
}
```

> 💡 这使得可以从工厂方法返回资源对象，然后在调用方用 try-with-resources 管理生命周期，代码更灵活。

---

## 6. NIO.2 文件操作——Path 与 Files 工具箱

### 6.1 Path 替代 File

Java 7 引入的 NIO.2（`java.nio.file` 包）提供了全新的文件操作 API，核心是 `Path` 接口和 `Files` 工具类。

**Path vs File 对比：**

| 对比维度 | `java.io.File` | `java.nio.file.Path` |
|---------|---------------|---------------------|
| 引入时间 | JDK 1.0 | JDK 7 |
| 接口/类 | 类（final） | 接口（可有多实现） |
| 路径操作 | 字符串拼接，易出错 | `resolve()`、`relativize()` 等方法 |
| 符号链接 | 弱支持 | 强支持，可检测/创建/读取符号链接 |
| 文件系统 | 单一 | 可扩展，`FileSystems` 支持 ZIP 等虚拟文件系统 |
| 属性访问 | `length()`、`lastModified()` 等 | `Files.readAttributes()` 统一访问各种属性 |
| 遍历 | `listFiles()` 返回数组 | `DirectoryStream`、`FileVisitor`、`Files.walk()` |
| 原子操作 | 不支持 | `Files.move()` 支持 `REPLACE_EXISTING` 等原子选项 |
| 缺陷 | 大量方法返回 boolean 而非抛异常 | 统一抛出 IOException |

```java
// ---------- 创建 Path 的不同方式 ----------
// Java 7 方式
Path p1 = Paths.get("C:\\data\\config.properties");      // Windows
Path p2 = Paths.get("/home/user/config.properties");     // Linux/Mac
Path p3 = Paths.get("config.properties");                 // 相对路径

// Java 11+ 推荐方式
Path p4 = Path.of("config.properties");                   // Path.of() 是 Paths.get() 的简写

// 从 URI
Path p5 = Paths.get(URI.create("file:///C:/data/file.txt"));

// 组合路径
Path base = Paths.get("C:\\data");
Path resolved = base.resolve("subdir\\file.txt");         // C:\data\subdir\file.txt
Path parent = resolved.getParent();                        // C:\data\subdir
Path fileName = resolved.getFileName();                    // file.txt

// ---------- 路径操作 ----------
Path dir = Paths.get("C:\\data");
Path config = dir.resolve("config");                      // 追加子路径
Path absPath = config.toAbsolutePath();                   // 转为绝对路径
Path normalized = absPath.normalize();                     // 规范化（去掉 . 和 ..）
Path relative = dir.relativize(config);                    // 计算相对路径
```

### 6.2 Files 工具类全景

`Files` 类集中了所有文件操作的方法，是 NIO.2 最重要的工具类。下面是按功能分类的全景速查：

#### 6.2.1 读写文本（最常用，Java 8+）

```java
// ---------- 读全部行返回 List<String> ----------
List<String> lines = Files.readAllLines(
    Path.of("data.txt"), StandardCharsets.UTF_8);

// ---------- 读整个文件为 String（Java 11+） ----------
String content = Files.readString(
    Path.of("data.txt"), StandardCharsets.UTF_8);

// ---------- 写字符串（覆盖） ----------
Files.writeString(
    Path.of("output.txt"), "Hello World", StandardCharsets.UTF_8);

// ---------- 写行集合 ----------
Files.write(
    Path.of("output.txt"), lines, StandardCharsets.UTF_8);

// ---------- 追加写入 ----------
Files.write(
    Path.of("output.txt"), "追加内容".getBytes(StandardCharsets.UTF_8),
    StandardOpenOption.APPEND);

// ---------- 流式读行（大文件友好） ----------
try (Stream<String> stream = Files.lines(
         Path.of("large.txt"), StandardCharsets.UTF_8)) {
    stream.filter(line -> line.contains("ERROR"))
          .forEach(System.out::println);
}
```

> 🎯 **`Files.readAllLines()` vs `Files.lines()`：**
> - `readAllLines()` 一次性载入内存，适合小文件（<100MB）
> - `Files.lines()` 返回 `Stream<String>`，惰性加载，适合大文件
> - 注意：`Files.lines()` 返回的 Stream 包含一个已打开的底层资源，**必须用 try-with-resources 或 `stream.close()` 关闭**

#### 6.2.2 读写字节

```java
// ---------- 读全部字节 ----------
byte[] data = Files.readAllBytes(Path.of("image.jpg"));

// ---------- 写字节 ----------
Files.write(Path.of("output.dat"), data);

// ---------- 使用 InputStream/OutputStream ----------
try (InputStream is = Files.newInputStream(Path.of("data.bin"));
     OutputStream os = Files.newOutputStream(Path.of("out.bin"))) {
    is.transferTo(os);   // Java 9+ 直接将输入流转到输出流
}

// ---------- 使用 BufferedReader/BufferedWriter ----------
try (BufferedReader br = Files.newBufferedReader(
         Path.of("data.txt"), StandardCharsets.UTF_8);
     BufferedWriter bw = Files.newBufferedWriter(
         Path.of("out.txt"), StandardCharsets.UTF_8)) {
    String line;
    while ((line = br.readLine()) != null) {
        bw.write(line);
        bw.newLine();
    }
}
```

#### 6.2.3 复制、移动、删除

```java
// ---------- 复制 ----------
Files.copy(Path.of("src.txt"), Path.of("dst.txt"),
           StandardCopyOption.REPLACE_EXISTING);   // 覆盖目标

// 复制 InputStream 到文件
try (InputStream is = url.openStream()) {
    Files.copy(is, Path.of("downloaded.zip"),
               StandardCopyOption.REPLACE_EXISTING);
}

// ---------- 移动（重命名） ----------
Files.move(Path.of("old.txt"), Path.of("new.txt"),
           StandardCopyOption.REPLACE_EXISTING,
           StandardCopyOption.ATOMIC_MOVE);   // 原子移动（文件系统支持时）

// ---------- 删除 ----------
Files.delete(Path.of("temp.txt"));                 // 文件不存在抛 NoSuchFileException
boolean deleted = Files.deleteIfExists(Path.of("temp.txt"));  // 不抛异常
```

#### 6.2.4 创建文件与目录

```java
// ---------- 创建目录 ----------
Files.createDirectory(Path.of("newdir"));           // 父目录必须存在
Files.createDirectories(Path.of("a/b/c/d"));       // 创建所有不存在的父目录

// ---------- 创建临时文件/目录 ----------
Path tmpFile = Files.createTempFile("prefix", ".txt");
Path tmpDir = Files.createTempDirectory("prefix");

// ---------- 创建文件 ----------
Files.createFile(Path.of("newfile.txt"));           // 文件已存在抛异常

// ---------- 设置文件属性 ----------
Files.setAttribute(Path.of("file.txt"), "dos:hidden", true);
```

#### 6.2.5 文件信息查询

```java
// ---------- 判断 ----------
boolean exists = Files.exists(Path.of("file.txt"));
boolean notExists = Files.notExists(Path.of("file.txt"));
boolean isDir = Files.isDirectory(Path.of("dir"));
boolean isReg = Files.isRegularFile(Path.of("file.txt"));
boolean isReadable = Files.isReadable(Path.of("file.txt"));
boolean isSymLink = Files.isSymbolicLink(Path.of("link"));

// ---------- 大小和修改时间 ----------
long size = Files.size(Path.of("file.txt"));
FileTime lastModified = Files.getLastModifiedTime(Path.of("file.txt"));

// ---------- 读取属性（批量，高效） ----------
// 方式一：基本属性（跨文件系统）
BasicFileAttributes attrs = Files.readAttributes(
    Path.of("file.txt"), BasicFileAttributes.class);
attrs.creationTime();
attrs.lastModifiedTime();
attrs.size();
attrs.isDirectory();
attrs.isSymbolicLink();

// 方式二：POSIX 属性（Linux/Mac）
PosixFileAttributes posix = Files.readAttributes(
    Path.of("file.txt"), PosixFileAttributes.class);
posix.permissions();      // rwxr-xr-x
posix.owner();
```

### 6.3 FileVisitor 递归遍历

`Files.walkFileTree()` 带 FileVisitor 实现目录递归遍历：

```java
// ---------- 查找所有 .java 文件 ----------
Path start = Paths.get("C:\\project\\src");
Files.walkFileTree(start, new SimpleFileVisitor<>() {
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        if (file.toString().endsWith(".java")) {
            System.out.println(file);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        System.err.println("访问失败: " + file + " -> " + exc.getMessage());
        return FileVisitResult.CONTINUE;   // 继续遍历其他文件
    }
});
```

**FileVisitResult 返回值：**

| 返回值 | 行为 |
|--------|------|
| `CONTINUE` | 继续遍历 |
| `TERMINATE` | 立即终止遍历 |
| `SKIP_SUBTREE` | 跳过当前目录及其子目录（仅在 preVisitDirectory 返回） |
| `SKIP_SIBLINGS` | 跳过当前目录的兄弟节点 |

**简易版：`Files.walk()` 返回 Stream（Java 8+）**

```java
// ---------- 递归列出所有文件（Stream 方式，更简洁） ----------
try (Stream<Path> stream = Files.walk(Paths.get("C:\\project\\src"))) {
    stream.filter(Files::isRegularFile)
          .map(Path::toString)
          .filter(name -> name.endsWith(".java"))
          .forEach(System.out::println);
}

// ---------- 限制深度 ----------
Files.walk(start, 3);   // 只遍历 3 层

// ---------- 查找文件 ----------
Path found = Files.walk(start)
    .filter(p -> p.endsWith("target.txt"))
    .findFirst()
    .orElse(null);

// ---------- 统计文件数量 ----------
long count = Files.walk(start)
    .filter(Files::isRegularFile)
    .count();
```

> 💡 `Files.walk()` 返回的是 `Stream<Path>`，惰性求值，适合链式操作。但**必须使用 try-with-resources**，因为底层打开了一个目录流。

### 6.4 DirectoryStream 目录过滤

`DirectoryStream` 用于遍历单个目录（不递归子目录），支持 glob 模式过滤。

```java
// ---------- 过滤 .txt 文件 ----------
try (DirectoryStream<Path> stream = Files.newDirectoryStream(
         Path.of("C:\\data"), "*.txt")) {
    for (Path entry : stream) {
        System.out.println(entry.getFileName());
    }
}

// ---------- 自定义过滤器 ----------
try (DirectoryStream<Path> stream = Files.newDirectoryStream(
         Path.of("C:\\data"), entry -> Files.isDirectory(entry))) {
    // 只处理目录
}

// vs Files.list() 的 Stream 版本（Java 8+）
try (Stream<Path> stream = Files.list(Path.of("C:\\data"))) {
    stream.filter(Files::isRegularFile)
          .forEach(System.out::println);
}
```

> ⚠️ `DirectoryStream` 不是 `Stream`，它实现了 `Iterable` 和 `Closeable`。Java 8 后推荐优先使用 `Files.list()` 返回的 `Stream<Path>`。

### 6.5 FileAttribute 与文件属性

```java
// ---------- 设置文件权限（POSIX） ----------
Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-xr-x");
Files.setPosixFilePermissions(Path.of("script.sh"), perms);

// ---------- 创建文件时指定属性 ----------
FileAttribute<Set<PosixFilePermission>> attr =
    PosixFilePermissions.asFileAttribute(
        PosixFilePermissions.fromString("rwx------"));
Files.createFile(Path.of("secret.sh"), attr);

// ---------- DOS 属性（Windows） ----------
Files.setAttribute(Path.of("data.txt"), "dos:hidden", true);
Files.setAttribute(Path.of("data.txt"), "dos:readonly", true);
```

**BasicFileAttributes 常用属性：**

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `creationTime()` | `FileTime` | 创建时间 |
| `lastModifiedTime()` | `FileTime` | 最后修改时间 |
| `lastAccessTime()` | `FileTime` | 最后访问时间 |
| `size()` | `long` | 文件大小（字节） |
| `isRegularFile()` | `boolean` | 是否普通文件 |
| `isDirectory()` | `boolean` | 是否目录 |
| `isSymbolicLink()` | `boolean` | 是否符号链接 |
| `isOther()` | `boolean` | 是否其他（设备等） |
| `fileKey()` | `Object` | 文件唯一标识（用于判断是否为同一文件） |

### 6.6 WatchService 文件监控入门

`WatchService` 可以监控目录中的文件变化（创建、修改、删除）。

```java
// ---------- 文件监控基本框架 ----------
Path dir = Path.of("C:\\watched");
WatchService watcher = FileSystems.getDefault().newWatchService();

// 注册监控事件
dir.register(watcher,
    StandardWatchEventKinds.ENTRY_CREATE,
    StandardWatchEventKinds.ENTRY_MODIFY,
    StandardWatchEventKinds.ENTRY_DELETE);

// 监控线程（异步执行）
new Thread(() -> {
    try {
        while (true) {
            WatchKey key = watcher.take();   // 阻塞直到有事件
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                Path fileName = (Path) event.context();
                System.out.printf("事件: %s, 文件: %s%n", kind.name(), fileName);
            }
            // 重置 key，否则不再接收事件
            if (!key.reset()) break;
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}, "FileWatcher").start();
```

> 💡 `WatchService` 适合**轻量级**文件监控，如配置文件热更新。不适用于海量文件监控或需要稳定生产级监控的场景，此时应考虑 Apache Commons VFS、JNotify 或操作系统原生 API（如 Windows 的 ReadDirectoryChangesW）。

---

## 7. 序列化概要

### 7.1 Serializable 标记接口

```java
public class User implements Serializable {
    private static final long serialVersionUID = 1L;  // 强烈建议显式声明
    private int id;
    private String name;
    private transient String password;  // 不会序列化
}
```

> 💡 `Serializable` 是一个**标记接口**（Marker Interface），没有任何方法。实现它只是告诉 JVM："这个类的对象可以被序列化"。所有需要序列化的类**必须**实现它，否则会抛 `NotSerializableException`。

### 7.2 serialVersionUID 的作用与生成规则

```java
// 显式声明（推荐）
private static final long serialVersionUID = 1L;

// 也可以让 IDE 根据类结构生成唯一值
private static final long serialVersionUID = 7832197352389174201L;
```

**serialVersionUID 的作用：**
- 序列化时将 UID 写入流中
- 反序列化时比较流的 UID 与本地类的 UID 是否一致
- 如果不一致，抛 `InvalidClassException`

**为什么必须显式声明：**
1. 如果不声明，JVM 会根据类结构**自动生成**一个 UID
2. 类结构只要稍有变化（如增加一个字段），自动生成的 UID 就会改变
3. 这意味着**旧版本序列化的数据无法反序列化到新版本的类**中
4. 显式声明后，开发者可以控制版本兼容性

> 🎯 **最佳实践：所有 Serializable 类都必须显式声明 `serialVersionUID`，并使用 `private static final long` 修饰。**

### 7.3 transient 关键字

`transient` 修饰的字段不会被序列化：

```java
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private transient String password;        // 敏感信息，不序列化
    private transient Socket socket;          // 不可序列化的系统资源
    private transient Thread worker;          // 运行态对象
}
```

**transient 的使用场景：**
| 场景 | 原因 |
|------|------|
| 密码、密钥等敏感字段 | 避免明文写入磁盘或网络 |
| 不可序列化的对象（`Thread`、`Socket`、`Connection`） | 不实现 Serializable |
| 派生/缓存字段（如根据 id 计算出的 hashCode） | 可以重新计算 |
| 运行态上下文（如当前状态码、监听器列表） | 序列化无意义 |

### 7.4 序列化代理模式（Serialization Proxy）

这是 Effective Java 推荐的一种**安全序列化模式**，防止攻击者通过伪造序列化流来构造恶意对象：

```java
public class Period implements Serializable {
    private final Date start;
    private final Date end;

    public Period(Date start, Date end) {
        // 防御性拷贝
        this.start = new Date(start.getTime());
        this.end = new Date(end.getTime());
        if (this.start.after(this.end))
            throw new IllegalArgumentException("开始时间必须早于结束时间");
    }

    // 序列化代理——替代 Period 本身被序列化
    private Object writeReplace() {
        return new SerializationProxy(this);
    }

    // 防止反序列化直接创建 Period 实例
    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Proxy required");
    }

    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Date start;
        private final Date end;

        SerializationProxy(Period p) {
            this.start = p.start;
            this.end = p.end;
        }

        // 反序列化代理时，创建真正的 Period 对象
        private Object readResolve() {
            return new Period(start, end);  // 经过构造器校验
        }
    }
}
```

> 💡 **为什么要用序列化代理？** 普通序列化可以绕过构造器约束创建对象。代理模式确保反序列化也经过构造器校验，防止被恶意利用。对于包含约束（如时间范围检查）的类，序列化代理是标准的安全做法。

### 7.5 Externalizable 接口

`Externalizable` 是 `Serializable` 的子接口，允许**完全自定义序列化逻辑**：

```java
public class User implements Externalizable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String name;

    // 必须有无参构造器
    public User() {}

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(id);
        out.writeUTF(name);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.id = in.readInt();
        this.name = in.readUTF();
    }
}
```

**Externalizable vs Serializable：**

| 对比维度 | Serializable | Externalizable |
|---------|-------------|---------------|
| 控制粒度 | JVM 自动序列化所有非 transient 字段 | 开发者完全控制写哪些字段 |
| 性能 | 需要反射，较慢 | 手动编码，更快 |
| 是否需无参构造 | 不需要 | **必须**有无参构造器 |
| 版本兼容 | 依赖 serialVersionUID | 版本兼容逻辑由开发者实现 |
| 复杂度 | 简单 | 较复杂，容易出错 |

### 7.6 序列化的安全问题与替代方案

**Java 原生序列化的问题：**

| 问题 | 说明 |
|------|------|
| 反序列化漏洞 | 恶意构造的序列化流可以触发任意代码执行（如 Apache Commons Collections 反序列化漏洞） |
| 性能问题 | 基于反射，序列化/反序列化速度慢，生成的数据体积大 |
| 跨语言困难 | 只有 Java 能读取 Java 序列化数据 |
| 版本耦合 | 服务端和客户端必须使用相同版本的类 |
| 类型污染 | `readObject()` 返回 `Object`，需要强制类型转换 |

**替代方案对比：**

| 方案 | 格式 | 性能 | 跨语言 | 适用场景 |
|------|------|------|-------|---------|
| **JSON** (Jackson/Gson) | 文本 | 中等 | ✅ 优秀 | REST API、日志、配置 |
| **Protobuf** (Google) | 二进制 | 快（比 JSON 快 3-10 倍） | ✅ 优秀 | RPC、微服务间通信、高性能存储 |
| **Avro** (Apache) | 二进制 | 快 | ✅ 好 | Hadoop 生态、大数据 |
| **Kryo** | 二进制 | 极快 | 有限 | Java 内部 RPC、缓存 |
| **Java 序列化** | 二进制 | 慢 | ❌ 仅 Java | 已不推荐，仅用于 RMI |

```java
// ---------- 现代替代：Jackson JSON 序列化 ----------
ObjectMapper mapper = new ObjectMapper();

// 序列化为 JSON 字符串
String json = mapper.writeValueAsString(user);

// 反序列化
User user = mapper.readValue(json, User.class);

// ---------- 现代替代：Protobuf（需先定义 .proto 文件）----------
// 生成代码后使用：
UserProto.User user = UserProto.User.newBuilder()
    .setId(1)
    .setName("Alice")
    .build();
byte[] data = user.toByteArray();
UserProto.User parsed = UserProto.User.parseFrom(data);
```

> 🎯 **核心要点：** 除非是遗留系统或 RMI 场景，**新系统不应使用 Java 原生序列化**。JSON 适合简单场景，Protobuf 适合高性能且需要跨语言的场景。

---

## 8. 选型速查与练习

### 8.1 场景速查表

| 你的需求 | 推荐方案 |
|---------|---------|
| 读文本文件（小文件，<50MB） | `Files.readString()` (Java 11+) 或 `Files.readAllLines()` |
| 读文本文件（大文件，>50MB） | `Files.lines()` 返回 `Stream<String>`，配合 try-with-resources |
| 写文本文件 | `Files.writeString()` (Java 11+) 或 `Files.write()` |
| 读二进制文件（小文件） | `Files.readAllBytes()` |
| 读二进制文件（大文件） | `BufferedInputStream` + `FileInputStream` 分批读 |
| 写二进制文件 | `Files.write(byte[])` 或 `BufferedOutputStream` |
| 读基本类型数据 | `DataInputStream` + `BufferedInputStream` + `FileInputStream` |
| 读/写 Java 对象 | 推荐 JSON 替代；必须用 Java 序列化时用 `ObjectInputStream`/`ObjectOutputStream` |
| 文件复制 | `Files.copy()` |
| 文件移动/重命名 | `Files.move()` |
| 文件删除 | `Files.deleteIfExists()` |
| 递归遍历目录 | `Files.walk()` (Stream 方式) 或 `Files.walkFileTree()` (FileVisitor 方式) |
| 目录单层遍历/过滤 | `Files.list()` 或 `Files.newDirectoryStream()` |
| 文件属性查询 | `Files.readAttributes()` (一次读取批量属性) |
| 路径拼接/处理 | `Path.of()` / `Path.resolve()` / `Path.relativize()` |
| 从网络流读文本 | `BufferedReader` + `InputStreamReader` + `socket.getInputStream()` |
| 格式化输出 | `PrintWriter` + OutputStreamWriter |
| 文件监控 | `WatchService` |

### 8.2 错误模式清单

```java
// ❌ 错误 1：逐字节读取
FileInputStream in = new FileInputStream("big.txt");
int b;
while ((b = in.read()) != -1) { ... }    // 极慢！每次 read 都是系统调用

// ✅ 正确：用缓冲区
byte[] buf = new byte[8192];
int len;
while ((len = in.read(buf)) != -1) { ... }


// ❌ 错误 2：不指定编码
FileReader fr = new FileReader("data.txt");    // 依赖系统默认编码

// ✅ 正确：显式指定编码
new InputStreamReader(new FileInputStream("data.txt"), StandardCharsets.UTF_8);


// ❌ 错误 3：不关闭流
InputStream in = new FileInputStream("file");
// 使用完后没调用 close() → 文件描述符泄漏

// ✅ 正确：try-with-resources
try (InputStream in = new FileInputStream("file")) { ... }


// ❌ 错误 4：用 File 做路径拼接
File dir = new File("C:\\data");
File file = new File(dir, "subdir" + "\\" + "file.txt");

// ✅ 正确：用 Path
Path path = Path.of("C:\\data").resolve("subdir").resolve("file.txt");


// ❌ 错误 5：Files.lines() 没关闭
Stream<String> lines = Files.lines(Paths.get("big.txt"));
lines.filter(...).forEach(...);   // 流关闭了，但底层文件句柄没释放！

// ✅ 正确：try-with-resources 包裹
try (Stream<String> lines = Files.lines(Paths.get("big.txt"))) {
    lines.filter(...).forEach(...);
}
```

### 8.3 常见面试自测题

| # | 问题 | 关键要点 |
|:-:|------|---------|
| 1 | `BufferedInputStream` 的缓冲原理是什么？ | 内部维护 byte[] 缓冲区（默认 8KB），批量从底层读取，减少系统调用 |
| 2 | `FileReader` 为什么不能指定编码？如何解决？ | 构造器调用 `super(new FileInputStream(f))`，未暴露 charset 参数；改用 `InputStreamReader`+`FileInputStream` |
| 3 | try-with-resources 如何处理多个异常？ | 主异常保留，close 异常被抑制，通过 `getSuppressed()` 获取 |
| 4 | `Path` 和 `File` 核心区别是什么？ | Path 是接口，有丰富的 `resolve()`/`relativize()` 方法；File 是类，字符串拼接操作 |
| 5 | `Files.walk()` 和 `Files.walkFileTree()` 的区别？ | walk() 返回 `Stream`（Java 8），惰性求值，适合链式操作；walkFileTree() 用 FileVisitor 回调，控制更精细 |
| 6 | Java 序列化中 `transient` 关键字的作用？ | 标记字段不参与序列化，用于密码、不可序列化对象、派生字段 |
| 7 | `serialVersionUID` 不声明会怎样？ | JVM 自动生成，类结构微小变化就导致 UID 改变，反序列化失败 |
| 8 | `Externalizable` 和 `Serializable` 的关系？ | Externalizable 继承 Serializable，要求开发者完全自定义序列化逻辑，必须无参构造 |
| 9 | 如何安全地反序列化对象？ | 使用序列化代理模式（`writeReplace`/`readResolve`），或放弃 Java 序列化改用 JSON/Protobuf |
| 10 | `Files.readAllLines()` 和 `Files.lines()` 适用场景？ | 前者小文件一次性读入内存；后者大文件惰性流式读，适合 >100MB 文件 |

### 8.4 实现练习建议

**基础练习：**
1. 用 `try-with-resources` + `BufferedReader` 实现一个读取 CSV 文件并统计每行字段数的程序
2. 用 `Files.walk()` 递归查找某目录下所有大于 10MB 的文件
3. 用 `Files.readAllLines()` 读文件，用 Stream 过滤、排序后写回新文件

**进阶练习：**
4. 用 `FileVisitor` 实现目录树打印（类似 `tree` 命令的输出），包含目录层级缩进
5. 用 `WatchService` 监控目录，当 `.txt` 文件被修改时自动打印变化
6. 用 `ByteArrayOutputStream` + `DataOutputStream` 构造一个自定义二进制格式，再用 `ByteArrayInputStream` + `DataInputStream` 解析

**综合练习：**
7. 实现一个简易的、面向行的键值对配置文件读写器（类似 `.properties` 格式），支持 UTF-8 编码和注释行（`#` 开头）

---

> 🎯 **本章核心总结：**
> 1. **I/O 选型三问**：数据是字节还是字符？文件大小是否适合一次性读入？是否可接受依赖默认编码？
> 2. **永远指定编码**：`StandardCharsets.UTF_8` 应成为肌肉记忆
> 3. **始终 try-with-resources**：Java 7+ 没有理由再写传统 try-finally
> 4. **File 已死，Path 当立**：新代码全部用 `Path.of()` / `Files` API
> 5. **序列化用 JSON**：除非你确信需要 Java 原生序列化，否则默认选 Jackson 或 Protobuf

---

**上一模块**：[02-Java集合框架原理与实战](./02-Java集合框架原理与实战.md)
**下一模块**：[04-Java时间日期API深度解析](./04-Java时间日期API深度解析.md)
**返回总览**：[00-Java API知识体系总览](./00-Java API知识体系总览.md)
