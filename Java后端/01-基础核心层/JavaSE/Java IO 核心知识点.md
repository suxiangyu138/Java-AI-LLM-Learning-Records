# Java IO 核心知识点
Java IO 是**输入/输入（Input/Output）**，用于处理**数据传输**（文件读写、网络通信、内存交换等），是 Java 基础核心模块。

---

## 一、IO 整体分类（最重要的结构图）
Java IO 分两大类：
1. **传统 IO（BIO）**：也叫**阻塞IO**，基于**流（Stream）**
2. **NIO**：也叫**非阻塞IO**，基于**通道（Channel）+缓冲区（Buffer）**

还有一个 **AIO（异步IO）**，用得极少。

---

## 二、传统 IO（BIO）核心

### 1. 两大抽象基类（所有流的祖宗）
- **字节流**：`InputStream` / `OutputStream`
  处理一切文件（图片、视频、音频、文本）
- **字符流**：`Reader` / `Writer`
  专门处理**文本**，解决中文乱码

### 2. 流的分类方式
1. 按方向：**输入流 / 输出流**
2. 按单位：**字节流 / 字符流**
3. 按功能：**节点流（直接操作文件）/ 处理流（包装增强）**

---

## 三、四大基础流（必须背）
```
文件字节输入流：FileInputStream
文件字节输出流：FileOutputStream

文件字符输入流：FileReader
文件字符输出流：FileWriter
```

### 3.1 字节流 vs 字符流
- **字节流**：万能，什么都能读
- **字符流**：只适合读文本，解决乱码

### 3.2 缓冲流（提高性能）
包装基础流，**减少IO次数，速度极快**
```
BufferedInputStream / BufferedOutputStream
BufferedReader / BufferedWriter
```

> 面试常问：**缓冲流为什么快？**
> 答：自带缓冲区，减少磁盘交互次数。

---

## 四、IO 核心设计模式：装饰者模式
处理流（如 Buffered）包装节点流，**动态增强功能**
```java
new BufferedInputStream(new FileInputStream("a.txt"));
```

---

## 五、序列化与反序列化
- **ObjectInputStream / ObjectOutputStream**
- 要求：实体类必须实现 **Serializable** 接口
- 作用：把对象存文件/网络传输，再恢复

---

## 六、NIO（非阻塞 IO，面试高频）

### NIO 三大核心组件
1. **Buffer（缓冲区）**：数据载体
2. **Channel（通道）**：双向传输，类似高速公路
3. **Selector（选择器）**：一个线程管理多个连接

### BIO vs NIO（面试必背）
| 特性 | BIO | NIO |
|------|-----|-----|
| 阻塞 | 阻塞 | 非阻塞 |
| 方式 | 流（单向） | 通道+缓冲区（双向） |
| 线程 | 一个连接一个线程 | 一个线程管理多个连接 |
| 效率 | 低 | 高 |

---

## 七、NIO Buffer 核心机制
Buffer 三个关键指针：
- **position**：当前操作位置
- **limit**：可读/写上限
- **capacity**：容量

两个关键方法：
- `flip()`：**写模式切换为读模式**
- `clear()`：清空，恢复写模式

---

## 八、File 类（文件操作）
- 不负责读写，只负责**文件/目录的增删改查**
- 常用方法：
  - `exists()`
  - `mkdir() / mkdirs()`
  - `delete()`
  - `listFiles()`

---

## 九、IO 异常与关闭
- 旧写法：`try-catch-finally` 手动关闭
- **JDK7+ 最佳写法**：try-with-resources
  流会**自动关闭**，不用手动 close

```java
try (InputStream in = new FileInputStream("a.txt")) {
    // 读写
}
```

---

## 十、面试高频 10 问（背会稳过）
1. **字节流和字符流的区别？**
2. **flush() 和 close() 的作用？**
3. **缓冲流为什么高效？**
4. **BIO 和 NIO 的区别？**
5. **NIO 三大组件是什么？**
6. **Buffer 的 flip() 作用？**
7. **序列化为什么要实现 Serializable？**
8. **try-with-resources 是什么？**
9. **装饰者模式在 IO 中的应用？**
10. **为什么字符流能解决乱码？**

---

### 总结
1. **IO = 传统 BIO（流） + NIO（通道+缓冲区）**
2. **字节流万能，字符流专治文本乱码**
3. **缓冲流 = 性能提升神器**
4. **NIO 核心：非阻塞、Selector、Buffer、Channel**
5. **开发必须用 try-with-resources 自动关闭流**
