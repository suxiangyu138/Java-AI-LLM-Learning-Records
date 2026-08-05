# 03 try-catch-finally 执行机制

> try-catch-finally 是异常处理的语法骨架——执行顺序、return 与 finally 的交互、finally 的边界，每一处都是面试考点

---

## 📚 目录

1. [try-catch-finally 的完整执行流程](#1-try-catch-finally-的完整执行流程)
2. [return 与 finally 的交互：返回值快照机制](#2-return-与-finally-的交互返回值快照机制)
3. [finally 中写 return / throw：绝对禁止](#3-finally-中写-return--throw绝对禁止)
4. [finally 不执行的三种特殊情况](#4-finally-不执行的三种特殊情况)
5. [catch 顺序：子类在前、父类在后](#5-catch-顺序子类在前父类在后)
6. [try 块的最小化原则](#6-try-块的最小化原则)

---

## 1. try-catch-finally 的完整执行流程

```text
执行 try 块
├── 无异常
│     ├── 有 finally → 执行 finally → 正常返回/继续
│     └── 无 finally → 正常返回/继续
└── 抛异常
      ├── 有匹配 catch → 执行 catch → 执行 finally → 继续（或 return）
      ├── 无匹配 catch → 执行 finally → 异常继续上抛
      └── catch 内部又抛异常 → 新异常覆盖（finally 仍执行）
```

```java
public void demo() {
    try {
        System.out.println("1. try");
        throw new RuntimeException("boom");
        // System.out.println("不会执行到这里");
    } catch (RuntimeException e) {
        System.out.println("2. catch: " + e.getMessage());
    } finally {
        System.out.println("3. finally");
    }
    System.out.println("4. 正常继续");
}
// 输出：1. try → 2. catch: boom → 3. finally → 4. 正常继续
```

**三条铁律**：

1. **finally 一定执行**（除第 4 节的三种特殊情况）——无论 try 是否抛异常、是否有匹配的 catch；
2. **没有匹配 catch 的异常**：先执行 finally，再沿调用栈上抛；
3. **catch 内抛新异常**：finally 仍执行，最终向上传播的是**新异常**（旧异常会作为新异常的 suppressed 或直接丢失——JDK 7+ 若新异常非 null 则旧异常自动挂到 suppressed）。

> 🎯 **核心要点**：finally 的职责是"**无论成败都必须做的事**"——释放资源、关闭连接、恢复状态、清理锁。JDK 7 的 try-with-resources 把最常见的"关资源"场景封装进了语法（见 04 模块）。

---

## 2. return 与 finally 的交互：返回值快照机制

**核心机制**：try 中执行 `return x` 时，**先把 x 的值快照**（压入返回值槽），再执行 finally，finally 结束后**返回快照值**。

```java
public static int test1() {
    int x = 1;
    try {
        return x;                    // ① 快照 x=1
    } finally {
        x = 100;                     // ② 修改局部变量 —— 不影响快照
    }
}
// 返回 1（不是 100）

public static StringBuilder test2() {
    StringBuilder sb = new StringBuilder("a");
    try {
        return sb;                   // ① 快照的是"引用"（对象地址）
    } finally {
        sb.append("b");              // ② 通过引用修改对象内容 —— 影响的是同一对象
    }
}
// 返回 "ab"（引用没变，对象内容变了）
```

**快照语义总结**（面试标准答案）：

| 场景 | 结果 |
|------|------|
| finally 修改基本类型局部变量 | 不影响返回值（快照的是值） |
| finally 修改引用类型局部变量的**指向** | 不影响返回值（快照的是引用） |
| finally 修改引用类型局部变量指向的**对象内容** | **影响**返回值（同一个对象） |
| finally 中写 `return` | **覆盖** try 的返回值（见下节） |

> ⚠️ 这背后的字节码原理：`return` 编译为 `ireturn/areturn` 前先把值放入操作数栈或返回值槽，finally 块内联在 return 指令之前——所以 finally 的赋值无法改变已入槽的值。

---

## 3. finally 中写 return / throw：绝对禁止

**finally 中写 `return` 会覆盖 try/catch 的返回值，吞掉异常**：

```java
// ❌ 反模式 1：finally 的 return 覆盖正常返回值
public static int bad1() {
    try { return 1; }
    finally { return 2; }      // 返回 2 —— try 的 1 被丢弃
}

// ❌ 反模式 2：finally 的 return 吞掉异常
public static int bad2() {
    try {
        throw new RuntimeException("boom");
    } finally {
        return 0;              // 异常被"吃掉"！调用方以为成功了 —— 最隐蔽的坑
    }
}

// ❌ 反模式 3：finally 抛异常覆盖原异常
public static void bad3() {
    try {
        throw new RuntimeException("原始异常");
    } finally {
        throw new IllegalStateException("finally 异常");   // 原始异常被覆盖
    }
}
// 调用方只看到 IllegalStateException，真正的"原始异常"线索丢失（JDK 7+ 挂到 suppressed）
```

**阿里巴巴 Java 开发手册明确禁止**：`finally` 块中禁止使用 `return`（try 中 return 或异常会被 finally 的 return 覆盖）。理由：

1. **返回值不可预期**——正常路径与异常路径的返回值都被覆盖；
2. **吞异常**——try 抛出的异常被静默丢弃，故障完全隐藏；
3. **排查极难**——线上表现为"偶发数据不对"而非报错。

> 🎯 **核心要点**：**finally 只做"清理动作"，永远不返回值、不抛业务异常**。需要返回结果就放到 finally 之后；finally 中真正的失败（如 close 失败）用 try-with-resources 的 suppressed 机制表达（见 04 模块）。

---

## 4. finally 不执行的三种特殊情况

| 情况 | 原因 | 说明 |
|------|------|------|
| `System.exit(0)` | JVM 直接终止 | 进程退出，所有代码都不再执行 |
| JVM 崩溃/断电 | 进程死亡 | 无法执行任何代码 |
| 执行 finally 的线程被杀死 | `Thread.stop()`（已废弃）等极端手段 | 现代 JDK 无正常手段，仅作理论 |

```java
public static void demo() {
    try {
        System.out.println("try");
        System.exit(0);              // JVM 终止 —— finally 不会执行
    } finally {
        System.out.println("finally");   // ❌ 不会打印
    }
}
```

> 💡 注意区分：`return`、抛异常、`break`/`continue` 都**不会**阻止 finally 执行——只有"JVM 没了"或"线程没了"才会。这也是"finally 一定执行"这句话的唯一例外说明。

---

## 5. catch 顺序：子类在前、父类在后

**编译器强制**：catch 块按顺序匹配，**第一个匹配的生效**——子类异常必须写在父类前面，否则父类 catch 会"截胡"：

```java
// ❌ 编译错误：FileNotFoundException 是 IOException 的子类，必须先捕获子类
try {
    Files.readString(Path.of("a"));
} catch (IOException e) {          // 父类在前 → 编译报错
} catch (FileNotFoundException e) {
}

// ✅ 正确：先子类后父类
try {
    Files.readString(Path.of("a"));
} catch (FileNotFoundException e) {      // 具体：文件不存在，单独处理（提示用户）
} catch (IOException e) {                // 通用：其他 IO 错误，统一处理
}
```

**匹配原则**：异常类型按**继承关系**匹配（`instanceof` 语义），与抛出的具体实例无关。

> 🎯 **核心要点**：catch 顺序 = "从具体到通用"——先处理能精准应对的异常（重试、降级、提示），最后才轮到兜底的通用异常。**永远不要 `catch (Exception)` 打头**。

---

## 6. try 块的最小化原则

**try 块只包裹"可能抛出该异常"的代码**——包裹范围越大，排查越难：

```java
// ❌ 反例：整个方法都包进去
try {
    String name = user.getName();                    // 不可能抛 IOException
    Files.writeString(path, name);                   // 唯一可能抛 IOException 的
    sendNotification(user);                          // 业务逻辑，不该被"误伤"
} catch (IOException e) {
    // 到底是哪一步失败了？—— 排查困难
}

// ✅ 正例：最小范围
String name = user.getName();
try {
    Files.writeString(path, name);                   // 只包真正会失败的
} catch (IOException e) {
    throw new PersistException("保存用户配置失败", e);
}
sendNotification(user);                              // 独立于异常处理之外
```

**最小化原则的三个收益**：

1. **定位快**——异常发生时，try 块内语句少，一眼锁定失败点；
2. **不误伤**——业务逻辑不会因为无关代码的异常被跳过；
3. **分工清晰**——每段 try-catch 处理一个独立风险点，可读性高。

> ⚠️ 与"包装"结合：最小 try 块内抛出的异常**统一包装**（`throw new XxxException("上下文", e)`）——上下文信息（哪个路径、哪个用户）让异常消息自带排错线索（见 05 模块异常链）。

---

**下一模块**：[04-try-with-resources与资源管理](./04-try-with-resources与资源管理.md) / **返回总览**：[00-Java异常体系知识体系总览](./00-Java异常体系知识体系总览.md)
