# Happens-Before 规则
> 7 条规则与传递性：Java 并发可见性的全部契约——掌握 HB 边，就掌握了判断"这段并发代码是否正确"的规范方法。

---

## 📚 目录

1. [HB 的定义与意义](#1-hb-的定义与意义)
2. [规则一至四：程序顺序与同步](#2-规则一至四程序顺序与同步)
3. [规则五至七：线程生命周期](#3-规则五至七线程生命周期)
4. [传递性与偏序](#4-传递性与偏序)
5. [HB 的判定方法](#5-hb-的判定方法)
6. [经典应用分析](#6-经典应用分析)

---

## 1. HB 的定义与意义

### 1.1 定义

如果操作 A **happens-before** 操作 B（记 A HB B），则：

```text
① A 的所有写操作对执行 B 的线程可见
② A 在 B 之前完成（顺序保证）
③ 该保证由 JMM 规范强制（所有平台一致）

反过来说：没有 HB 边 → 什么都不能保证（允许乱序、允许旧值）
```

### 1.2 与"时间先后"的区别

| 概念 | 含义 |
|------|------|
| 时间先后 | 实际执行顺序（可能因重排序与直觉不符） |
| Happens-Before | **规范保证**的顺序（程序语义层面的契约） |

```text
关键：HB 不是"实际发生顺序"的描述，而是"必须保证的语义顺序"
  即使实际执行中 B 的机器指令先完成，
  只要 A HB B，观察结果必须像"先 A 后 B"

类比：HB 是"法律上的先后"，时间顺序是"物理上的先后"
```

> 🎯 一句话：**HB 边 = "我保证你看到我写的东西"的合同**。并发代码正确性的判断，就是找"关键读写之间有没有 HB 边"。

---

## 2. 规则一至四：程序顺序与同步

### 2.1 规则一：程序顺序规则

**同一线程内，按程序书写顺序，前面的操作 HB 后面的操作。**

```text
线程内：a = 1; b = 2; → a=1 HB b=2（按书写顺序）
注意：这不禁止重排序（as-if-serial 保证结果），
     但语义上"先写的先完成"是成立的
```

### 2.2 规则二：监视器锁规则

**对同一监视器（monitor），unlock HB 之后对该锁的 lock。**

```text
线程 A：synchronized(lock) { x = 42; }   // unlock
线程 B：synchronized(lock) { read x; }   // lock

→ A 的 unlock HB B 的 lock → x=42 对 B 可见

关键：同一把锁！不同锁之间无此保证
```

### 2.3 规则三：volatile 规则

**对同一 volatile 变量，写 HB 之后对该变量的读。**

```text
线程 A：ready = true;                     // volatile 写
线程 B：while(!ready) {} 然后读 data      // volatile 读

→ ready 的写 HB ready 的读 → 配合程序顺序 + 传递性：
  data = 42（A）HB ready = true（A）HB 读到 true（B）HB 读 data（B）
  → data 必然可见（01 模块第 6 节的例子）

JDK 5+ 补充：对 volatile 的读写还有"半同步"语义
  （读写 volatile 相当于"半个监视器"）
```

### 2.4 规则四：传递性

**若 A HB B 且 B HB C，则 A HB C。**

```text
传递性是串联一切的关键：
  程序顺序 + 锁/volatile + 线程生命周期 → 跨线程可见性链

例（经典发布模式）：
  x = 42（线程1 程序顺序）→ ready = true（volatile 写）
  → 读到 ready=true（volatile 读，线程2）→ 读 x
  传递性：x=42 HB 读 x → 必定读到 42
```

---

## 3. 规则五至七：线程生命周期

### 3.1 规则五：线程启动规则

**线程 A 中执行 Thread.start() HB 新线程 B 中的任何操作。**

```text
线程 A：config = load(); thread.start();   // start HB B 内一切
线程 B：使用 config                        // 必然可见

→ 启动线程前准备好的数据，新线程必然能看到
（这正是"线程传参无需额外同步"的规范依据）
```

### 3.2 规则六：线程终止规则

**线程 B 中的所有操作 HB 线程 A 中检测到 B 终止（join 返回或 isAlive 为 false）。**

```text
线程 B：写入结果 result；执行完毕
线程 A：b.join(); 读取 result          // 必然可见

→ 等待线程结束的 join 是"最可靠的可见性同步"
（join 内部通过 Object.wait/notify 实现，语义等价监视器规则）
```

### 3.3 规则七：中断规则

**线程 A 调用 B.interrupt() HB B 检测到中断（抛出 InterruptedException 或 isInterrupted 为 true）。**

```text
线程 A：b.interrupt();
线程 B：catch (InterruptedException e) { 读 A 写的数据 }

→ interrupt 之前 A 的写对 B 可见
（interrupt 内部用 volatile 中断标志实现——规则三的实例）
```

### 3.4 补充：final 的特殊规则

```text
JMM 对 final 的额外保证（对象安全发布）：
  正确构造的对象，其 final 字段在构造完成后
  对其他线程可见（无需 volatile）

实现：final 字段写后插入 StoreStore 屏障（x86 上为编译期屏障）
应用：不可变对象（String/包装类）的线程安全基础
```

---

## 4. 传递性与偏序

### 4.1 HB 是偏序

```text
HB 关系满足：
  自反？—— 不严格自反（通常不含同一操作）
  传递 ✓（规则四）
  反对称：A HB B 且 B HB A 不可能（无环）

性质：HB 是偏序不是全序
  → 两个线程间可能"既无 A HB B 也无 B HB A"（无同步）
  → 无 HB 边 = 无保证 = 数据竞争区域
```

### 4.2 同步边全景图

```text
跨线程 HB 边的全部来源：
  ① 锁：unlock → lock（同一监视器）
  ② volatile：写 → 读（同一变量）
  ③ 线程启动/终止/中断
  ④ 传递性组合

工程口诀："同步点之间画边，边连起来就有保证"
```

---

## 5. HB 的判定方法

### 5.1 三步判定法

```text
判断"线程 A 的写 W 对线程 B 的读 R 是否可见"：
  Step 1  找同步边：A 与 B 之间是否存在
          锁/volatile/线程生命周期边？
  Step 2  验证链：W → … → R 是否构成 HB 链（含程序顺序与传递性）？
  Step 3  结论：有链 → 保证可见；无链 → 无保证（数据竞争）

注意：实际"碰巧可见"不算数——规范无保证 = 可能错
```

### 5.2 常见错误判定

| 错误直觉 | 正确判定 |
|---------|---------|
| "synchronized 里写，外面读也对" | 错——读也要在同一把锁内（锁的 HB 是成对的） |
| "volatile 修饰了就行" | 需确认"写 HB 读"（另一线程确实读该 volatile） |
| "Thread.sleep 提供同步" | 错——sleep 不产生任何 HB 边 |
| "yield/优先级提供同步" | 错——同上，无 HB 边 |

> ⚠️ 高频面试题：`Thread.sleep`、`yield`、`setPriority` 都不产生 HB 边——"等一会儿就能看到"是错误直觉。

---

## 6. 经典应用分析

### 6.1 安全的延迟初始化（volatile 版）

```java
class Holder {
    static volatile Config config;          // volatile 保证发布

    static Config get() {
        Config c = config;                   // ① volatile 读
        if (c == null) {
            synchronized (Holder.class) {    // ② 锁
                c = config;
                if (c == null) {
                    c = new Config();
                    config = c;              // ③ volatile 写
                }
            }
        }
        return c;
    }
}
// 可见性链：③（写）HB ①（读）→ 初始化完整可见
// （再加 final 字段的双重保险，见 03 模块 DCL）
```

### 6.2 发布-订阅模型（生产者消费者）

```java
// 生产者：数据写 + volatile 标志
data = compute();          // 普通写
ready = true;              // volatile 写

// 消费者：volatile 标志 + 数据读
while (!ready) spin();     // volatile 读
use(data);                 // 普通读
// 传递性保证：data 的写对消费者可见 ✓
```

### 6.3 对照：无同步的后果

```java
// 无任何同步（无 HB 边）：
//  线程 A：x = 42;     线程 B：print(x);
// 结果：可能 0（旧值）、可能 42、甚至"看起来永不更新"
// 这不是 bug，是 JMM 允许的行为——程序员的错，不是 JVM 的错
```

> 🎯 **核心要点**：HB 是并发正确性的"判定器"——7 条规则 + 传递性覆盖了 Java 全部跨线程可见性保证。掌握三步判定法（找边→验链→结论），任何"这段并发代码对不对"的问题都有规范答案；而无 HB 边的代码，无论本地跑多少次"碰巧正确"，在规范层面都是错的。

---

## 7. 小结

| 问题 | 答案 |
|------|------|
| HB 是什么？ | 可见性保证的规范顺序（法律上的先后） |
| 七条规则？ | 程序顺序/锁/volatile/传递性/start/join/中断 |
| 传递性价值？ | 串联跨线程可见性链（发布模式的依据） |
| HB 是偏序？ | 是——无边 = 无保证 = 数据竞争 |
| sleep/yield 有 HB 吗？ | 没有——不产生任何同步边 |
| 判定三步？ | 找边 → 验链 → 结论 |

**下一模块**：[05-volatile与synchronized的内存语义](05-volatile与synchronized的内存语义.md)　**返回总览**：[00-JMM知识体系总览](00-JMM知识体系总览.md)
