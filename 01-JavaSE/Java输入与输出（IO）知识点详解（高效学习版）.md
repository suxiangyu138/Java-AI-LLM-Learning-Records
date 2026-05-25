# Java输入与输出（IO）知识点详解（高效学习版）

> 文档版本更新日期：03.17 23:33

Java输入与输出（简称IO）是Java核心基础知识点，核心作用是实现程序与外部设备（文件、键盘、屏幕、网络等）的数据交互——输入（Input）是程序从外部获取数据（如读取文件、接收键盘输入），输出（Output）是程序向外部传递数据（如写入文件、在屏幕打印）。

学习IO的关键是"分清流的类型、掌握核心API、理解装饰者模式"，结合"输出倒逼输入"的学习方法，每学一种流就搭配代码练习，就能快速上手。以下内容全面覆盖IO核心知识点，循序渐进、重点突出，适配Java学习节奏。

---

## 一、核心概念

### 1.1 流（Stream）

**流（Stream）：** IO操作的核心载体，是"数据的流动通道"，数据以字节或字符为单位，从一个地方流向另一个地方（如从文件流向程序、从程序流向屏幕）。

**数据源/目的地：** 流的起点（输入时）或终点（输出时），常见的有：文件、键盘、屏幕、网络连接、内存等。

**IO分类：** 核心分为两大体系——**字节流**（处理所有类型数据）、**字符流**（仅处理文本数据），后续所有IO类都围绕这两大体系展开。

### 1.2 IO核心分类（重中之重）

Java IO的分类有两个核心维度，必须分清，避免混淆：

**按数据单位分类（核心维度）：**

- **字节流**：以字节（byte）为单位处理数据（1字节=8位），可处理所有类型数据（文本、图片、视频、音频等）。核心父类：InputStream（输入）、OutputStream（输出）。
- **字符流**：以字符（char）为单位处理数据（1字符=2字节，适配中文），仅能处理文本数据（.txt、.java等）。核心父类：Reader（输入）、Writer（输出）。

**按流向分类：**

- **输入流**：数据从外部流向程序（读数据）。核心父类：InputStream、Reader。
- **输出流**：数据从程序流向外部（写数据）。核心父类：OutputStream、Writer。

**记忆技巧：** 字节流"万能"（处理所有数据），字符流"专一"（只处理文本）；输入流"读"、输出流"写"，对应父类名称就能区分（Input/Reader=读，Output/Writer=写）。

### 1.3 核心原则

1. **资源自动关闭**：所有IO流类都实现了AutoCloseable接口，建议使用try-with-resources语法（Java7及以上），自动关闭流，避免手动关闭遗漏导致资源泄露。
2. **顺序操作**：流的操作是"顺序的"，不能反向读取/写入（如读取文件时，只能从开头读到末尾，不能倒着读，需倒读需特殊处理）。
3. **编码/解码**：字符流底层依赖字节流，会进行"编码/解码"（如UTF-8、GBK），需注意编码格式，避免中文乱码。

### 1.4 字节流体系

字节流是IO的基础，可处理所有类型数据，重点掌握"核心父类+常用子类+基本操作"，搭配代码练习，无需死记硬背。

#### 1.4.1 核心父类（抽象类，不能直接实例化）

**InputStream（字节输入流）**：所有字节输入流的父类，核心方法（抽象方法，子类必须重写）：

| 方法 | 说明 |
|------|------|
| `int read()` | 读取一个字节，返回读取的字节值（0-255），读取到末尾返回-1 |
| `int read(byte[] b)` | 读取多个字节，存入字节数组b，返回实际读取的字节数，末尾返回-1（推荐使用，效率更高） |
| `void close()` | 关闭流，释放资源（try-with-resources可自动关闭） |

**OutputStream（字节输出流）**：所有字节输出流的父类，核心方法：

| 方法 | 说明 |
|------|------|
| `void write(int b)` | 写入一个字节（注意：传入的int值，仅取低8位作为字节） |
| `void write(byte[] b)` | 写入字节数组b中的所有字节 |
| `void flush()` | 刷新流，将缓冲区中的数据强制写入目的地（字节流可选，字符流必须） |
| `void close()` | 关闭流，关闭前会自动刷新 |

#### 1.4.2 常用子类

**（1）文件相关字节流（最常用）**

用于读取/写入文件，是日常开发中最常用的字节流，核心类：**FileInputStream**、**FileOutputStream**。

> **注意：** FileOutputStream构造方法中，可添加第二个参数（boolean append），true表示"追加写入"，false表示"覆盖写入"（默认false）。

**（2）其他常用字节流（了解即可，按需使用）**

- **ByteArrayInputStream / ByteArrayOutputStream**：以内存中的字节数组为数据源/目的地，用于在内存中操作字节数据（无需文件）。
- **DataInputStream / DataOutputStream**：用于读取/写入基本数据类型（int、long、float等），避免手动转换字节。

### 1.5 字符流体系

字符流仅处理文本数据（如.txt、.java文件），底层依赖字节流，会进行编码/解码，核心解决"中文乱码"问题，重点掌握与字节流的区别和常用子类。

#### 1.5.1 核心父类（抽象类，不能直接实例化）

**Reader（字符输入流）**：所有字符输入流的父类，核心方法：

| 方法 | 说明 |
|------|------|
| `int read()` | 读取一个字符，返回字符的Unicode值（0-65535），末尾返回-1 |
| `int read(char[] cbuf)` | 读取多个字符，存入字符数组，返回实际读取的字符数，末尾返回-1 |
| `void close()` | 关闭流，释放资源 |

**Writer（字符输出流）**：所有字符输出流的父类，核心方法：

| 方法 | 说明 |
|------|------|
| `void write(int c)` | 写入一个字符（传入的int值，取低16位作为字符） |
| `void write(char[] cbuf)` | 写入字符数组 |
| `void write(String str)` | 直接写入字符串（最常用，无需转换） |
| `void flush()` | 必须调用，字符流有缓冲区，不刷新会导致数据无法写入目的地 |
| `void close()` | 关闭流，关闭前会自动刷新 |

#### 1.5.2 常用子类

**（1）文件相关字符流（最常用）**

用于读取/写入文本文件，核心类：**FileReader**、**FileWriter**，注意指定编码格式（避免中文乱码）。

> **易错点：** FileReader/FileWriter默认使用系统编码（如Windows默认GBK，Linux默认UTF-8），直接使用易出现中文乱码，推荐搭配InputStreamReader/OutputStreamWriter，手动指定编码格式（UTF-8）。

**（2）其他常用字符流**

- **BufferedReader / BufferedWriter**：缓冲字符流（下文重点讲），提升读取/写入效率，新增readLine()方法（读取一行文本）。
- **CharArrayReader / CharArrayWriter**：以内存中的字符数组为数据源/目的地，用于内存中文本操作。
- **PrintWriter**：打印字符流，可直接打印字符串、基本数据类型，常用作控制台输出或文件写入（替代System.out.println）。

### 1.6 缓冲流体系

缓冲流（BufferedStream）是"装饰器模式"的典型应用，本身不直接操作数据源，而是包装字节流/字符流，通过"缓冲区"提升IO效率（减少与磁盘/外部设备的交互次数），核心分为缓冲字节流和缓冲字符流，必须掌握。

#### 1.6.1 缓冲字节流

包装字节流，核心类：**BufferedInputStream**、**BufferedOutputStream**，构造方法需传入对应的字节流对象。

#### 1.6.2 缓冲字符流（最常用）

包装字符流，核心类：**BufferedReader**、**BufferedWriter**，新增实用方法，效率大幅提升，是文本操作的首选。

> **关键优势：** BufferedReader的readLine()方法的逐行读取，BufferedWriter的newLine()方法的跨平台换行，是日常文本处理的核心方法。

### 1.7 其他常用IO流

#### 1.7.1 打印流（PrintStream / PrintWriter）

用于便捷打印数据，可打印字符串、基本数据类型，无需手动转换，常用作控制台输出或文件写入。

#### 1.7.2 转换流（InputStreamReader / OutputStreamWriter）

**核心作用：** 将字节流转换为字符流，解决字符流的编码问题（手动指定编码格式），是字符流与字节流的桥梁，前文已在字符流示例中使用，此处重点强调其作用。

> **关键：** 所有字符流底层都依赖转换流，将字节流转换为字符流，再进行编码/解码，避免中文乱码的核心就是通过转换流指定编码格式（UTF-8）。

---

## 二、底层原理

### 2.1 装饰者模式（Decorator Pattern）

缓冲流（如BufferedInputStream、BufferedReader）是装饰器模式的典型应用。装饰器模式的核心思想是：通过包装（Wrapper）的方式，在不修改原有类代码的情况下，动态地为对象添加额外的功能。

在IO体系中的体现：

- 缓冲流本身不直接操作数据源，而是包装字节流/字符流，通过"缓冲区"提升IO效率（减少与磁盘/外部设备的交互次数）。
- 例如：`BufferedInputStream bis = new BufferedInputStream(new FileInputStream("test.txt"))` — FileInputStream负责读取文件数据，BufferedInputStream负责为其提供缓冲功能。
- 这种包装可以多层叠加，实现功能的灵活组合，是Java IO框架的核心设计思想。掌握装饰者模式的思想，理解流的包装逻辑，是进阶学习的关键。

### 2.2 字符流编码/解码机制

字符流底层依赖字节流，会进行"编码/解码"（如UTF-8、GBK），需注意编码格式，避免中文乱码。

- **编码（Encoding）：** 将字符转换为其对应的字节表示。例如，字符'A'在UTF-8编码下转换为一个字节0x41，中文字符'中'在UTF-8编码下转换为三个字节0xE4 0xB8 0xAD。
- **解码（Decoding）：** 将字节数据转换回字符表示。例如，字节序列0xE4 0xB8 0xAD在UTF-8解码下转换为字符'中'。

**编码格式不统一的后果：** 如果读取文件时使用的编码格式与写入时使用的编码格式不一致（如读取用GBK，写入用UTF-8），会导致中文乱码。

### 2.3 转换流底层原理

转换流（InputStreamReader/OutputStreamWriter）是字符流与字节流的桥梁，核心作用是将字节流转换为字符流，解决字符流的编码问题（手动指定编码格式）。

控制台输入System.in是字节流，需通过InputStreamReader包装为字符流才能使用BufferedReader。所有字符流底层都依赖转换流，将字节流转换为字符流，再进行编码/解码，避免中文乱码的核心就是通过转换流指定编码格式（UTF-8）。

### 2.4 缓冲流工作机制

缓冲流内部维护一个固定大小的缓冲区（默认8192字节，即8KB），其工作流程如下：

1. **读取时**：缓冲流会一次性从磁盘/外部设备读取尽可能多的数据填充到缓冲区中，后续的读取操作直接从内存缓冲区中获取数据，避免频繁的磁盘I/O。
2. **写入时**：缓冲流先将数据写入内存缓冲区，待缓冲区满或手动调用flush()时，才一次性将数据写入磁盘/外部设备，减少I/O次数。

这种"批量处理"机制能显著提升IO性能，特别是在大量小数据量的读写场景中效果尤为明显。

### 2.5 AutoCloseable与try-with-resources原理

所有IO流类都实现了AutoCloseable接口，建议使用try-with-resources语法（Java7及以上），自动关闭流，避免手动关闭遗漏导致资源泄露。

try-with-resources语法（Java7及以上）会在try代码块执行完毕后，自动调用每个资源的close()方法，无论是否发生异常。这不仅简化了代码，更重要的是避免了因异常导致的资源泄露问题，确保资源安全释放。

---

## 三、代码实现

### 3.1 字节流代码实现

#### 3.1.1 FileInputStream — 使用字节流读取文件

```java
/**
 * 使用FileInputStream读取文件（字节流读文件）
 * 演示字节输入流的基本用法：创建流、读取数据、自动关闭
 */
try (InputStream is = new FileInputStream("test.txt")) {
    byte[] buffer = new byte[1024]; // 缓冲区，提升读取效率
    int len;
    // 循环读取，len为实际读取的字节数，-1表示读取完毕
    while ((len = is.read(buffer)) != -1) {
        // 将字节数组转为字符串（注意编码格式，避免乱码）
        System.out.print(new String(buffer, 0, len, StandardCharsets.UTF_8));
    }
} catch (IOException e) {
    e.printStackTrace();
}
```

#### 3.1.2 FileOutputStream — 使用字节流写入文件

```java
/**
 * 使用FileOutputStream写入文件（字节流写文件）
 * 演示字节输出流的基本用法：创建流、写入数据、刷新关闭
 */
try (OutputStream os = new FileOutputStream("test.txt")) {
    String content = "Java IO 字节流示例";
    // 将字符串转为字节数组，写入文件
    os.write(content.getBytes(StandardCharsets.UTF_8));
    os.flush(); // 可选，关闭时会自动刷新
} catch (IOException e) {
    e.printStackTrace();
}
```

> **注意：** FileOutputStream构造方法中，可添加第二个参数（boolean append），true表示"追加写入"，false表示"覆盖写入"（默认false）。

### 3.2 字符流代码实现

#### 3.2.1 使用字符流读取文本文件（推荐方式：转换流指定编码）

```java
/**
 * 使用InputStreamReader包装FileInputStream读取文本文件
 * 推荐指定编码格式（UTF-8），避免系统默认编码导致乱码
 */
try (Reader reader = new InputStreamReader(new FileInputStream("test.txt"), StandardCharsets.UTF_8)) {
    char[] buffer = new char[1024];
    int len;
    while ((len = reader.read(buffer)) != -1) {
        System.out.print(new String(buffer, 0, len));
    }
} catch (IOException e) {
    e.printStackTrace();
}
```

#### 3.2.2 使用字符流写入文本文件（推荐方式：转换流指定编码）

```java
/**
 * 使用OutputStreamWriter包装FileOutputStream写入文本文件
 * 支持指定编码格式和追加写入模式
 */
try (Writer writer = new OutputStreamWriter(new FileOutputStream("test.txt", true), StandardCharsets.UTF_8)) {
    String content = "Java IO 字符流示例（追加写入）";
    writer.write(content);
    writer.flush(); // 必须调用，否则数据可能未写入
} catch (IOException e) {
    e.printStackTrace();
}
```

### 3.3 缓冲流代码实现

#### 3.3.1 BufferedInputStream — 缓冲字节流读取文件

```java
/**
 * 使用BufferedInputStream缓冲字节流读取文件
 * 效率比FileInputStream高，通过缓冲区减少磁盘I/O次数
 * 默认缓冲区大小8192字节，可手动指定
 */
try (
    InputStream is = new FileInputStream("test.txt");
    // 包装字节流，默认缓冲区大小8192字节，可手动指定
    BufferedInputStream bis = new BufferedInputStream(is)
) {
    byte[] buffer = new byte[1024];
    int len;
    while ((len = bis.read(buffer)) != -1) {
        System.out.print(new String(buffer, 0, len, StandardCharsets.UTF_8));
    }
} catch (IOException e) {
    e.printStackTrace();
}
```

#### 3.3.2 BufferedReader — 缓冲字符流读取文本（逐行读取）

```java
/**
 * 使用BufferedReader逐行读取文本文件（新增readLine()方法，读取一行）
 * 是日常文本操作的核心方法，配合InputStreamReader指定编码格式，避免乱码
 */
try (
    Reader reader = new InputStreamReader(new FileInputStream("test.txt"), StandardCharsets.UTF_8);
    BufferedReader br = new BufferedReader(reader)
) {
    String line;
    // 逐行读取，line为null表示读取完毕
    while ((line = br.readLine()) != null) {
        System.out.println(line);
    }
} catch (IOException e) {
    e.printStackTrace();
}
```

#### 3.3.3 BufferedWriter — 缓冲字符流写入文本（跨平台换行）

```java
/**
 * 使用BufferedWriter写入文本文件（新增newLine()方法，换行）
 * newLine()跨平台兼容，比"\n"更规范
 * 写入后必须调用flush()刷新缓冲区
 */
try (
    Writer writer = new OutputStreamWriter(new FileOutputStream("test.txt", true), StandardCharsets.UTF_8);
    BufferedWriter bw = new BufferedWriter(writer)
) {
    bw.write("缓冲字符流示例");
    bw.newLine(); // 换行（跨平台兼容，比"\n"更规范）
    bw.write("逐行写入文本");
    bw.flush(); // 必须刷新
} catch (IOException e) {
    e.printStackTrace();
}
```

### 3.4 打印流代码实现

```java
/**
 * 使用PrintWriter便捷写入文件
 * 可直接打印字符串、基本数据类型，无需手动转换
 * 关闭时自动刷新，无需手动调用flush()
 */
try (PrintWriter pw = new PrintWriter("test.txt", StandardCharsets.UTF_8, true)) {
    pw.println("打印流示例");
    pw.println(123);       // 直接打印int类型
    pw.println(true);      // 直接打印boolean类型
    // 无需手动flush，关闭时自动刷新
} catch (IOException e) {
    e.printStackTrace();
}
```

### 3.5 实战场景代码

#### 场景1：文本文件的读取与写入（最常用）

**需求：** 读取test1.txt中的内容，过滤掉空行，写入test2.txt中。

```java
/**
 * 文本文件过滤：读取test1.txt，过滤空行后写入test2.txt
 * 组合使用：缓冲字符流 + 转换流 + 指定编码格式
 */
try (
    // 读取流（缓冲字符流+转换流，指定编码）
    BufferedReader br = new BufferedReader(
        new InputStreamReader(new FileInputStream("test1.txt"), StandardCharsets.UTF_8)
    );
    // 写入流（缓冲字符流+转换流，指定编码，追加写入）
    BufferedWriter bw = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream("test2.txt", true), StandardCharsets.UTF_8)
    )
) {
    String line;
    while ((line = br.readLine()) != null) {
        // 过滤空行（trim()去除前后空格，判断是否为空）
        if (!line.trim().isEmpty()) {
            bw.write(line);
            bw.newLine(); // 换行
        }
    }
    bw.flush();
} catch (IOException e) {
    e.printStackTrace();
}
```

#### 场景2：字节流复制文件（适配所有文件类型）

**需求：** 将一张图片（或视频、音频）从D盘复制到E盘（字节流可处理所有类型文件）。

```java
/**
 * 使用字节缓冲流复制文件（适配所有文件类型：图片、视频、音频等）
 * 字节流可处理所有类型文件，配合缓冲流提升效率
 */
// 复制文件（字节缓冲流，提升效率）
try (
    BufferedInputStream bis = new BufferedInputStream(new FileInputStream("D:\\test.jpg"));
    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("E:\\test.jpg"))
) {
    byte[] buffer = new byte[1024 * 8]; // 8KB缓冲区，效率更高
    int len;
    while ((len = bis.read(buffer)) != -1) {
        bos.write(buffer, 0, len); // 写入实际读取的字节数
    }
    bos.flush();
} catch (IOException e) {
    e.printStackTrace();
}
```

#### 场景3：键盘输入处理

**需求：** 从键盘接收用户输入的字符串，打印到屏幕上，输入"exit"退出。

```java
/**
 * 键盘输入交互：读取用户输入并回显，输入"exit"退出
 * 键盘输入（BufferedReader包装System.in，System.in是字节流）
 * 需通过InputStreamReader包装为字符流才能使用BufferedReader
 */
try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
    String input;
    while (true) {
        System.out.print("请输入内容（输入exit退出）：");
        input = br.readLine();
        if ("exit".equals(input)) {
            break;
        }
        System.out.println("你输入的内容：" + input);
    }
} catch (IOException e) {
    e.printStackTrace();
}
```

---

## 四、实战要点

### 4.1 高效学习技巧

**1. 先记分类，再记API：** 先分清"字节流/字符流""输入流/输出流"，记住核心父类，再逐个学习常用子类，避免杂乱无章。

**2. 代码驱动学习：** 每学一种流，写一个简单demo（如读取文件、写入文件），直观感受流的使用方式，比死记方法更高效。

**3. 对比记忆：** 对比字节流与字符流的区别、缓冲流与普通流的区别，重点记住"字符流需刷新、字节流可不用""缓冲流提升效率"。

### 4.2 文本文件操作要点

- 始终使用**转换流**（InputStreamReader/OutputStreamWriter）指定编码格式（推荐UTF-8），避免中文乱码。
- 首选**缓冲字符流**（BufferedReader/BufferedWriter）进行文本操作，利用readLine()逐行读取和newLine()跨平台换行。
- 字符流写入后必须调用**flush()** 刷新缓冲区，确保数据写入目的地。

### 4.3 文件复制要点

- 对于非文本文件（图片、视频、音频等），必须使用**字节流**，字符流无法处理二进制数据。
- 搭配**缓冲流**（BufferedInputStream/BufferedOutputStream）显著提升读写效率。
- 读取时使用适当的**缓冲区大小**（如8KB），过大浪费内存，过小增加I/O次数。
- 写入时必须指定**0到实际读取长度**的范围，避免缓冲区末尾的无效数据被写入。

### 4.4 键盘输入处理要点

- System.in是**字节流**，不能直接用于字符操作，需包装为**字符流**。
- 标准键盘输入模式：`BufferedReader br = new BufferedReader(new InputStreamReader(System.in))`。
- 使用readLine()读取整行输入，使用字符串的equals()方法判断退出条件。

---

## 五、避坑总结

### 5.1 中文乱码

**问题描述：** 字符流未指定编码格式，或编码格式不统一（如读取用GBK，写入用UTF-8）。

**解决方案：** 始终用转换流指定UTF-8编码。不要直接使用FileReader/FileWriter，而是使用InputStreamReader/OutputStreamWriter包装FileInputStream/FileOutputStream并指定StandardCharsets.UTF_8。

### 5.2 流未关闭

**问题描述：** 手动关闭流时遗漏，导致资源泄露。

**解决方案：** 优先使用try-with-resources语法，自动关闭流。try代码块执行完毕后，会自动调用每个资源的close()方法，无论是否发生异常。

### 5.3 字符流未刷新

**问题描述：** 写入文本后未调用flush()，导致数据未写入目的地。

**解决方案：** 字符流写入后必须调用flush()（或关闭流）。字符流内部有缓冲区，数据先写入缓冲区，只有调用flush()或缓冲区满时才会真正写入目的地。

### 5.4 缓冲区使用不当

**问题描述：** 读取时未判断实际读取的字节数（len），直接使用整个缓冲区，导致末尾出现乱码或多余数据。

**解决方案：** 写入时指定"0到len"的范围。例如：`bos.write(buffer, 0, len)`，确保只写入实际读取的有效数据。

### 5.5 常见易错点汇总

| 易错点 | 问题描述 | 解决方案 |
|--------|----------|----------|
| 中文乱码 | 字符流未指定编码格式，或编码格式不统一 | 始终用转换流指定UTF-8编码 |
| 流未关闭 | 手动关闭流时遗漏，导致资源泄露 | 优先使用try-with-resources语法 |
| 字符流未刷新 | 写入文本后未调用flush()，数据未写入目的地 | 字符流写入后必须调用flush()（或关闭流） |
| 缓冲区使用不当 | 未判断实际读取长度，直接使用整个缓冲区 | 写入时指定"0到len"的范围 |

---

## 六、企业级最佳实践

### 6.1 流的选择策略

| 场景 | 推荐使用 | 原因 |
|------|----------|------|
| 处理所有类型文件（文本、图片、视频等） | 字节流（FileInputStream/FileOutputStream + BufferedInputStream/BufferedOutputStream） | 字节流可处理所有数据格式 |
| 仅处理文本文件 | 字符流（InputStreamReader/OutputStreamWriter + BufferedReader/BufferedWriter） | 字符流自动处理编码/解码，避免手动转换 |
| 大文件读写 | 缓冲流 + 适当缓冲区大小（8KB~64KB） | 减少磁盘I/O次数，显著提升效率 |
| 需要逐行读取文本 | BufferedReader的readLine() | 提供直接按行读取的能力 |
| 需要格式化输出 | PrintWriter/PrintStream | 直接支持println()、printf()等方法 |

### 6.2 编码规范实践

- **始终指定编码格式：** 在构造InputStreamReader/OutputStreamWriter时，明确指定StandardCharsets.UTF_8，不依赖系统默认编码。
- **统一编码：** 整个项目中统一使用UTF-8编码，避免因编码不一致导致乱码问题。
- **使用StandardCharsets常量：** 使用`StandardCharsets.UTF_8`而非字符串"UTF-8"，避免拼写错误。

### 6.3 资源管理最佳实践

- **优先使用try-with-resources：** 所有IO流类都实现了AutoCloseable接口，使用try-with-resources自动关闭流，避免资源泄露。
- **避免在finally中手动关闭：** try-with-resources不仅代码更简洁，还能正确处理异常屏蔽问题。
- **注意关闭顺序：** 外层包装流的close()会自动调用内层流的close()，只需关闭最外层的流即可。

### 6.4 性能优化实践

- **使用缓冲流：** 在字节流、字符流外层包装缓冲流（BufferedInputStream/BufferedOutputStream/BufferedReader/BufferedWriter），显著提升IO效率。
- **合理设置缓冲区大小：** 默认缓冲区大小为8192字节（8KB），对于大文件读写可适当增大（如32KB、64KB），但不宜过大以免浪费内存。
- **批量读写：** 使用read(byte[])/read(char[])方法批量读写，避免单字节/单字符读写带来的性能开销。
- **及时flush：** 在关键节点调用flush()，确保数据及时写入，防止宕机导致数据丢失。

### 6.5 进阶学习建议

学习Java IO，核心是"多练、多用"，结合之前提到的计算机学习方法，重点做好2件事：

**基础阶段：** 每天练1个简单demo，比如用字节流读文件、用字符流写文件、用缓冲流提升效率，熟悉核心API的用法。

**进阶阶段：** 结合小场景练习，比如文件复制、文本过滤、键盘交互，尝试组合使用不同的流（如缓冲流+转换流），掌握装饰者模式的思想，理解流的包装逻辑。

IO是Java开发的基础，无论是文件操作、网络通信，还是框架中的数据交互，都离不开IO流，掌握好IO知识点，能为后续学习Java高级特性（如NIO、网络编程）打下坚实基础。
