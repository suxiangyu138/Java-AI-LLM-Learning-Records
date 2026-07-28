# 04 JDK 诊断与故障排查工具

> jps/jstat/jstack/jmap——这四个工具是Java后端线上排障的"四大名捕"，面试必问，工作必用

## 📚 目录

1. [jps — JVM进程状态](#1-jps--jvm进程状态)
2. [jstat — JVM统计监控（重点）](#2-jstat--jvm统计监控重点)
3. [jstack — 线程堆栈（重中之重）](#3-jstack--线程堆栈重中之重)
4. [jmap — 内存映射（重点）](#4-jmap--内存映射重点)
5. [jcmd — 综合诊断命令](#5-jcmd--综合诊断命令)
6. [jinfo — JVM参数查看与动态修改](#6-jinfo--jvm参数查看与动态修改)
7. [jhat — 堆分析（已废弃）](#7-jhat--堆分析已废弃)
8. [诊断工具速查表](#8-诊断工具速查表)

---

## 1. jps — JVM进程状态

### 1.1 基本用法

`jps`（JVM Process Status）是JDK中最轻量的工具，用于列出当前系统中所有的Java进程及其JVM信息。

```bash
# 基本用法：列出Java进程ID和主类名
jps

# 输出示例
# 12345 Application
# 12346 Jps
```

### 1.2 常用选项

| 选项 | 作用 | 示例 | 应用场景 |
|------|------|------|---------|
| `-l` | 输出完整主类名或JAR路径 | `jps -l` | 区分同名主类的进程 |
| `-v` | 输出JVM传入参数 | `jps -v` | 查看进程启动的JVM配置 |
| `-m` | 输出main方法参数 | `jps -m` | 查看启动参数 |
| `-V` | 仅输出进程ID | `jps -V` | 脚本中使用 |

```bash
# -l：显示完整包名
jps -l
# 12345 org.springframework.boot.loader.JarLauncher
# 12346 sun.tools.jps.Jps

# -v：查看JVM参数（对排查启动参数特别有用）
jps -v
# 12345 Application -Xms512m -Xmx1024m -XX:+UseG1GC -Dspring.profiles.active=prod
```

### 1.3 实战场景

> 💡 **场景**：服务器上运行了多个Spring Boot应用，无法区分进程对应哪个服务。

```bash
# 使用 -l 查看完整路径，区分不同应用
jps -l -v | grep -v Jps
# 23456 /opt/app/user-service.jar -Xms512m -Xmx1024m ...
# 23457 /opt/app/order-service.jar -Xms256m -Xmx512m ...
```

### 1.4 常见问题

> ⚠️ **注意**：
> - `jps` 依赖于 `/tmp/hsperfdata_<username>` 文件，如果该目录被清理或权限不足，`jps` 将无法显示对应进程
> - 使用 `-v` 时输出可能较长，建议配合 `grep` 过滤
> - `jps` 只能看到当前用户的Java进程（除非有root权限）

> 🎯 **核心要点**：jps是JVM工具链的入口，它告诉你"系统上运行了哪些Java进程、每个进程的PID是什么"。先用jps拿到PID，后续所有诊断工具（jstat/jstack/jmap）都依赖这个PID。

---

## 2. jstat — JVM统计监控（重点）

### 2.1 基本用法

`jstat`（JVM Statistics Monitoring Tool）是JVM统计监控工具，可以实时查看GC、类加载、JIT编译等数据。

```bash
jstat -<option> [-t] [-h<lines>] <vmid> [<interval> [<count>]]
```

### 2.2 核心选项

| 选项 | 作用 | 使用场景 |
|------|------|---------|
| `-class` | 类加载统计 | 排查类加载/卸载异常，如OOM前的类加载风暴 |
| `-compiler` | JIT编译统计 | 看JIT编译情况，判断热点方法编译压力 |
| `-gc` | GC统计（详细信息） | 各代容量、使用量、GC次数、GC耗时 |
| `-gccapacity` | 各代容量 | 堆各区域的容量配置 |
| `-gcutil` | GC百分比（推荐） | 各代使用率百分比，最常用 |
| `-gccause` | GC原因 | 同-gcutil，额外显示GC原因 |
| `-gcnew` | 新生代GC统计 | 新生代Eden/S0/S1的详细统计 |
| `-gcold` | 老年代GC统计 | 老年代详细统计 |

### 2.3 `-gc` 输出详解

```bash
jstat -gc <PID> 1000 5
# 每1000ms输出一次，共5次
```

**输出字段详解**：

| 字段 | 含义 | 单位 | 说明 |
|------|------|:----:|------|
| S0C | Survivor0区容量 | KB | S0当前大小 |
| S1C | Survivor1区容量 | KB | S1当前大小 |
| S0U | Survivor0区已使用 | KB | S0已用空间 |
| S1U | Survivor1区已使用 | KB | S1已用空间 |
| EC | Eden区容量 | KB | Eden当前大小 |
| EU | Eden区已使用 | KB | Eden已用空间 |
| OC | Old区容量 | KB | 老年代当前大小 |
| OU | Old区已使用 | KB | 老年代已用空间 |
| MC | Metaspace容量 | KB | 元空间当前大小（JDK8+） |
| MU | Metaspace已使用 | KB | 元空间已用空间 |
| YGC | Young GC次数 | 次 | 新生代GC总次数 |
| YGCT | Young GC总耗时 | 秒 | 新生代GC累计时间 |
| FGC | Full GC次数 | 次 | Full GC总次数 |
| FGCT | Full GC总耗时 | 秒 | Full GC累计时间 |
| GCT | GC总耗时 | 秒 | 所有GC累计时间 |

```text
# 输出示例分析
 S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU    YGC   YGCT    FGC   FGCT    GCT
5120.0 5120.0 2048.0  0.0   33792.0  18432.0   67584.0    45200.0   48640.0 46208.0  15   0.235   2     0.180   0.415
```

> 💡 **解读**：
> - Survivor区S0用了2MB，S1未使用 → 正常，每次YGC后存活对象从Eden+From区移到To区
> - Eden区使用18MB/33MB → 使用率约55%，还算健康
> - 老年代使用45MB/67MB → 使用率约67%，需要关注增长趋势
> - YGC=15次，总耗时0.235s → 平均每次15ms，正常
> - FGC=2次，总耗时0.180s → 发生了一次Full GC，需要关注原因

### 2.4 `-gcutil` 输出详解（最常用）

```bash
jstat -gcutil <PID> 2000 10
# 每2秒输出一次，共10次
```

```text
  S0     S1     E      O      M     CCS    YGC     YGCT    FGC    FGCT    GCT
 40.00   0.00  55.00  67.00  95.00  90.00  15     0.235    2     0.180  0.415
 40.00   0.00  58.00  67.00  95.00  90.00  15     0.235    2     0.180  0.415
 40.00   0.00  62.00  67.00  95.00  90.00  15     0.235    2     0.180  0.415
```

**百分比字段**:

| 字段 | 含义 | 健康阈值 |
|------|------|---------|
| S0/S1 | Survivor使用率 | 波动正常，若长期为0或100%需关注 |
| E | Eden使用率 | YGC触发后应大幅下降 |
| O | Old使用率 | <70%正常，>80%警惕，持续增长需排查 |
| M | Metaspace使用率 | 检查是否有类加载泄漏 |
| CCS | 压缩类空间使用率 | 一般应保持稳定 |
| YGC/YGCT | Young GC次数/耗时 | 每次耗时<50ms为佳 |
| FGC/FGCT | Full GC次数/耗时 | 尽量避免FGC，每次应<1s |

### 2.5 实战：持续监控GC频率

> 💡 **场景**：线上应用响应变慢，怀疑GC过于频繁。

```bash
# 每1秒输出一次GC统计，持续监控
jstat -gcutil <PID> 1000

# 第一次输出
S0 S1 E O M YGC YGCT FGC FGCT GCT
0.00 100.00 65.00 45.00 92.00 120 1.200 3 0.500 1.700
# 第二次输出（1秒后）
S0 S1 E O M YGC YGCT FGC FGCT GCT
100.00 0.00 2.00 46.00 92.00 121 1.220 3 0.500 1.720
# 第三次输出（2秒后）
S0 S1 E O M YGC YGCT FGC FGCT GCT
0.00 100.00 2.00 47.00 92.00 122 1.240 3 0.500 1.740

# 发现：每秒触发一次YGC！（YGC从120→121→122）
# 老年代使用率持续上升（45%→47%），说明对象晋升过快
# 可能原因：Eden区过小 or 大对象过多 or 内存泄漏
```

**排查思路**：
1. 确认Eden区大小是否合理（`jstat -gccapacity`）
2. 检查新生代对象晋升速率（观察OU增长）
3. 使用`jmap -histo`检查对象分布
4. 若OU持续增长且不下降 → 怀疑内存泄漏，dump堆分析

### 2.6 其他辅助选项

```bash
# 类加载统计
jstat -class <PID>
# Loaded  Bytes  Unloaded  Bytes   Time
#   4520  8750.3       20    45.2   2.34

# JIT编译统计
jstat -compiler <PID>
# Compiled Failed Invalid   Time   FailedType FailedMethod
#     4520      1       0   45.20          1   com/example/Service/init

# GC容量
jstat -gccapacity <PID>
# NGCMN    NGCMX     NGC     S0C   S1C       EC      OGCMN      OGCMX       OGC         OC      MCMN     MCMX      MC
# 65536.0 524288.0 262144.0 51200.0 51200.0 159744.0  131072.0 1048576.0   524288.0   524288.0    0.0  1075200.0  48640.0
```

> 🎯 **核心要点**：
> - `jstat -gcutil <PID> 1000` 是线上监控GC的第一命令，用它可以快速判断当前GC频率是否正常
> - 重点关注 **YGC频率** 和 **FGC次数**：YGC每秒超过1次说明新生代太小；FGC出现说明老年代已到瓶颈
> - 结合 `OU`（Old Used）的增长趋势判断是否存在内存泄漏：持续增长不下降 = 危险信号
> - Young GC平均耗时 = YGCT / YGC，>50ms说明GC耗时偏高

---

## 3. jstack — 线程堆栈（重中之重）

### 3.1 基本用法

`jstack` 是诊断Java线程问题的核心工具，可以导出JVM中所有线程的堆栈快照（thread dump）。

```bash
jstack [-l] [-e] <PID>
```

| 选项 | 作用 | 说明 |
|------|------|------|
| `-l` | 打印锁信息 | 死锁检测必备，会显示拥有的锁和等待的锁 |
| `-e` | 打印附加信息 | 如线程本地阻塞情况 |
| `-F` | 强制dump | 当jstack无响应时使用（jstack -F PID） |

### 3.2 线程状态解读

在线程dump中，每个线程有以下几种状态：

| 状态 | 含义 | 诊断意义 |
|------|------|---------|
| `RUNNABLE` | 正在执行中 | 正在使用CPU，可能正在运算或进行I/O |
| `BLOCKED` | 等待锁释放 | 线程被阻塞等待获取锁，大量BLOCKED可能是锁竞争激烈 |
| `WAITING` | 无限期等待 | 调用了`wait()`/`park()`，等待被唤醒 |
| `TIMED_WAITING` | 限期等待 | `sleep()`/`wait(timeout)`/`parkNanos()` |
| `TERMINATED` | 已终止 | 线程已结束 |

### 3.3 线程dump示例解读

```text
# 线程dump典型输出（jstack -l PID）

"http-nio-8080-exec-7" #27 daemon prio=5 os_prio=0 tid=0x00007f8c9c080000 nid=0x2a3c runnable [0x00007f8c7bffa000]
   java.lang.Thread.State: RUNNABLE
        at com.example.service.OrderService.getOrder(OrderService.java:45)
        at com.example.controller.OrderController.getOrder(OrderController.java:23)
        at org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:205)
        - locked <0x000000076b4f3e50> (a java.lang.Object)
        at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61)
        at java.lang.Thread.run(Thread.java:748)

"dubbo-thread-12" #31 daemon prio=5 os_prio=0 tid=0x00007f8c9c100000 nid=0x2a3d waiting on condition [0x00007f8c7befe000]
   java.lang.Thread.State: WAITING (parking)
        at sun.misc.Unsafe.park(Native Method)
        - parking to wait for  <0x000000076b6f8e50> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)
        at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)
        at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2039)

"Thread-3" #15 daemon prio=5 os_prio=0 tid=0x00007f8c9bf80000 nid=0x2a3e blocked [0x00007f8c7c1fe000]
   java.lang.Thread.State: BLOCKED (on object monitor)
        at com.example.service.InventoryService.deduct(InventoryService.java:30)
        - waiting to lock <0x000000076b4f3e50> (a java.lang.Object)
        at com.example.controller.OrderController.submitOrder(OrderController.java:55)
```

> 💡 **解读要点**：
> - 线程名称：`http-nio-8080-exec-7`是Tomcat工作线程，`dubbo-thread-12`是Dubbo线程
> - `nid`：操作系统级别的线程ID（十六进制），排查CPU问题时需要用到
> - `Thread.State`：当前线程状态
> - `locked <0x...>`：该线程已持有的锁
> - `waiting to lock <0x...>`：该线程正在等待的锁

### 3.4 死锁检测

`jstack -l` 会自动检测死锁，并在输出末尾打印检测结果：

```bash
jstack -l <PID>
```

```text
# 输出末尾的死锁检测
Found one Java-level deadlock:
=============================
"Thread-A":
    waiting to lock monitor 0x00007f8c9c083800 (object 0x000000076b4f3e50, a java.lang.Object),
    which is held by "Thread-B"
"Thread-B":
    waiting to lock monitor 0x00007f8c9c083600 (object 0x000000076b4f3d50, a java.lang.Object),
    which is held by "Thread-A"

Java stack information for the threads listed above:
===================================================
"Thread-A":
    at com.example.service.OrderService.getOrder(OrderService.java:45)
    - waiting to lock <0x000000076b4f3e50> (a java.lang.Object)
    - locked <0x000000076b4f3d50> (a java.lang.Object)
"Thread-B":
    at com.example.service.InventoryService.deduct(InventoryService.java:30)
    - waiting to lock <0x000000076b4f3d50> (a java.lang.Object)
    - locked <0x000000076b4f3e50> (a java.lang.Object)
```

> 💡 **诊断**：Thread-A持有锁A等待锁B，Thread-B持有锁B等待锁A，典型的死锁场景。修复方式是保证两个线程加锁顺序一致。

### 3.5 CPU飙高排查（完整案例）

这是面试中最高频的实战题——"线上某个服务CPU飙到100%，如何排查？"

**完整排查流程**：

```bash
# Step 1: 找到CPU占用最高的Java进程
top
# 输出：PID 23456 占用CPU 980% → 确认是Java进程

# Step 2: 找到该进程中CPU最高的线程
top -Hp 23456
# 输出：线程PID 23460 占用CPU 85%
#       线程PID 23461 占用CPU 75% ...

# Step 3: 将线程ID转为十六进制（jstack中nid是十六进制）
printf '%x\n' 23460
# 输出：5ba4

# Step 4: 导出线程栈，查找该线程
jstack 23456 | grep -A 100 '5ba4'

# 输出示例：
"http-nio-8080-exec-23" #42 daemon prio=5 os_prio=0 tid=0x00007f...
   java.lang.Thread.State: RUNNABLE
        at java.util.regex.Pattern$Loop.match(Pattern.java:4785)
        at java.util.regex.Matcher.match(Matcher.java:1281)
        at java.util.regex.Matcher.matches(Matcher.java:612)
        at com.example.utils.TextParser.extractKeywords(TextParser.java:35)
        at com.example.service.SearchService.search(SearchService.java:78)
        ...

# Step 5: 定位到具体代码行——发现TextParser.extractKeywords方法有低效正则匹配
# 排查结束：优化正则表达式，或使用String.indexOf替代
```

**命令速查总结**：
```
top → top -Hp <PID> → printf '%x\n' <TID> → jstack <PID> | grep -A 50 '<nid>'
```

> 💡 **Step-by-step解释**：
> - `top`：找到CPU占用最高的进程（PID），确认是Java进程
> - `top -Hp <PID>`：查看该进程中哪个线程占用CPU最高（TID）
> - `printf '%x\n' <TID>`：将十进制线程ID转为十六进制（小写），因为jstack用十六进制表示nid
> - `jstack <PID> | grep -A 50 '0x5ba4'`：找到对应线程的堆栈，定位到有问题的代码行

### 3.6 线程池耗尽排查

**场景**：线上请求大量超时，怀疑Tomcat线程池耗尽。

```bash
# 导出线程dump
jstack <PID> > threaddump_1.txt

# 分析：统计各状态的线程数
grep "java.lang.Thread.State" threaddump_1.txt | sort | uniq -c

# 输出示例
#   200   java.lang.Thread.State: BLOCKED (on object monitor)
#   150   java.lang.Thread.State: WAITING (on object monitor)
#   50    java.lang.Thread.State: TIMED_WAITING (sleeping)
#   50    java.lang.Thread.State: RUNNABLE

# 解读：200个线程BLOCKED！
# 说明大量线程在等待某个锁，可能是数据库连接池耗尽、HTTP连接池耗尽等
# 查看BLOCKED线程的堆栈确认具体在等待什么资源
```

**排查步骤**：

```bash
# 查看等待锁的线程
grep -B 5 "BLOCKED" threaddump_1.txt | head -50

# 定位到：
"http-nio-8080-exec-7" #27 daemon prio=5 ...
   java.lang.Thread.State: BLOCKED (on object monitor)
        at com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:222)
        - waiting to lock <0x000000076b6f8e50> (a com.zaxxer.hikari.pool.HikariPool)
# → 确认是HikariCP连接池耗尽！

# 解决方案：
# 1. 调大连接池大小：spring.datasource.hikari.maximum-pool-size=50
# 2. 优化SQL查询速度，减少连接占用时间
# 3. 增加慢查询监控，排查慢SQL
```

### 3.7 死锁排查实战

> 💡 **场景**：线上业务出现间歇性卡死，部分请求完全无法处理。

```bash
# 多次dump（间隔3秒），对比分析
jstack -l <PID> > dump1.txt
sleep 3
jstack -l <PID> > dump2.txt

# 方法1：查看jstack自带的死锁检测
grep -A 20 "deadlock" dump1.txt

# 方法2：对比两次dump，如果同一组线程保持不变且同时BLOCKED → 死锁
```

**死锁检测脚本**（快速检查）：

```bash
# 一键检测死锁
jstack -l <PID> | grep -E "deadlock|waiting to lock|locked <" --color=always

# 或使用Python/Shell脚本定期检测
for i in {1..5}; do
    jstack -l <PID> | grep -A 20 "deadlock" >> deadlock_check.log
    sleep 2
done
```

> 🎯 **核心要点**：
> - `jstack` 是线程诊断的"核武器"，尤其 **CPU飙高** 排查是面试必考题
> - 排查流程四步法：`top` → `top -Hp` → `printf '%x'` → `jstack`（必须背熟！）
> - `BLOCKED` 线程过多 → 锁竞争严重或资源池耗尽
> - `jstack -l` 自带死锁检测，优先使用
> - 建议连续dump 3-5次（间隔1秒）对比分析，单次dump可能误判

---

## 4. jmap — 内存映射（重点）

### 4.1 基本用法

`jmap`（JVM Memory Map）是Java内存诊断的核心工具，用于查看堆配置、对象统计信息和导出堆转储文件。

```bash
jmap [option] <PID>
```

### 4.2 `-heap` — 堆配置与使用概况

```bash
jmap -heap <PID>
```

```text
Attaching to process ID 23456, please wait...
Debugger attached successfully.
Server compiler detected.
JVM version is 25.222-b10

using parallel threads in the new generation.
using thread-local object allocation.
Concurrent Mark-Sweep GC

Heap Configuration:
   MinHeapFreeRatio         = 40
   MaxHeapFreeRatio         = 70
   MaxHeapSize              = 2147483648 (2048.0MB)
   NewSize                  = 1073741824 (1024.0MB)    # 新生代大小
   MaxNewSize               = 1073741824 (1024.0MB)
   OldSize                  = 1073741824 (1024.0MB)    # 老年代大小
   MetaspaceSize            = 268435456 (256.0MB)
   CompressedClassSpaceSize = 33554432 (32.0MB)
   G1HeapRegionSize         = 0 (0MB)

Heap Usage:
New Generation (Eden + 1 Survivor Space):
   capacity = 971063296 (926.0MB)
   used     = 786432000 (750.0MB)          # 使用了81%
   free     = 184631296 (176.0MB)
   80.9935% used

Eden Space:
   capacity = 864026624 (824.0MB)
   used     = 698351616 (666.0MB)
   free     = 165675008 (158.0MB)
   80.8685% used

From Space:
   capacity = 107036672 (102.0MB)
   used     = 88080384 (84.0MB)
   free     = 18956288 (18.0MB)
   82.3529% used

To Space:
   capacity = 107036672 (102.0MB)
   used     = 0 (0.0MB)
   free     = 107036672 (102.0MB)
   0.0% used

Old Generation:
   capacity = 1073741824 (1024.0MB)
   used     = 912680550 (870.0MB)          # 使用了85%
   free     = 161061274 (154.0MB)
   84.9609% used                            # 老年代已满！

43685 interned Strings occupying 4891696 bytes.
```

> 💡 **解读**：
> - 老年代使用率84.96% → 接近满，需要FGC或即将OOM
> - 新生代From区使用率82%，To区为0 → 说明From刚完成GC回收
> - `MaxHeapSize=2048MB`，当前总使用约870+666=1536MB → 内存压力较大

### 4.3 `-histo` — 对象统计

```bash
# 查看所有对象（包括不可达对象）
jmap -histo <PID>

# 只查看存活对象（会触发Full GC）
jmap -histo:live <PID>
```

```text
# jmap -histo:live 23456 输出示例

 num     #instances         #bytes  class name
----------------------------------------------
   1:       1524000      121920000  [B                           # byte数组
   2:        890000       56960000  java.util.HashMap$Node       # HashMap节点
   3:        785000       37680000  com.example.order.OrderEntity # 业务实体
   4:        650000       31200000  java.lang.String
   5:        420000       30240000  [Ljava.lang.Object;           # 对象数组
   6:        380000       18240000  java.util.ArrayList
   7:        320000       12800000  java.util.LinkedHashMap$Entry
   8:        280000       11200000  java.util.concurrent.ConcurrentHashMap$Node
   9:        250000       10000000  org.springframework.cache.Cache$ValueWrapper
  10:        200000        9600000  com.example.service.CacheManager
...
Total:  15240000个实例, 总内存约2GB

# 重点关注：
# - 第1位 [B（byte数组）占121MB → 检查是否有大Buffer未释放
# - 第3位 OrderEntity 75万个实例 → 怀疑是业务缓存或查询结果未释放
```

**排查技巧**：

```bash
# 对比前后两次histo，看哪些对象在持续增长
jmap -histo:live <PID> > histo_before.txt
# ... 等待一段时间 ...
jmap -histo:live <PID> > histo_after.txt

# 用diff找出增长最快的对象
diff histo_before.txt histo_after.txt
```

| 字段 | 含义 | 排查要点 |
|------|------|---------|
| `#instances` | 实例数量 | 持续增长 = 泄漏嫌疑 |
| `#bytes` | 占用内存 | 重点关注占用最大TOP10 |
| `class name` | 类名 | `[B`是byte[]，`[C`是char[]，`[I`是int[] |

> ⚠️ **注意**：`jmap -histo:live` 会触发一次Full GC，在线上慎用！生产环境建议使用`jmap -histo`（不带live）或改用jcmd。

### 4.4 `-dump` — 导出堆快照

堆转储（Heap Dump）是排查OOM的最重要手段。

```bash
# 导出全部对象（推荐线上用）
jmap -dump:format=b,file=/tmp/heap.hprof <PID>

# 只导出存活对象（会触发FGC，线上慎用）
jmap -dump:live,format=b,file=/tmp/heap_live.hprof <PID>

# 文件格式：.hprof 是标准格式，可以直接用MAT/Eclipse Memory Analyzer打开
```

### 4.5 OOM排查完整案例

> 💡 **场景**：线上应用报错 `java.lang.OutOfMemoryError: Java heap space`。

**排查步骤**：

```bash
# Step 1: 查看堆概况，确认OOM情况
jmap -heap <PID>
# → 发现Old Generation使用率99.9%，几乎占满

# Step 2: 查看对象分布，找内存大户
jmap -histo <PID> | head -30
# → 发现 OrderEntity 有500万个实例，而且还在增长

# Step 3: 导出堆快照（保存现场）
jmap -dump:format=b,file=/tmp/oom_dump_$(date +%Y%m%d_%H%M%S).hprof <PID>

# Step 4: 用MAT分析hprof文件
# MAT下载：https://eclipse.dev/mat/
```

**MAT分析步骤**：
1. 打开hprof文件 → "Leak Suspects Report"（自动分析泄漏嫌疑）
2. 查看Dominator Tree（支配树）→ 找到占用内存最大的对象
3. 查看GC Root引用路径 → 确认为什么对象没有被回收
4. 查看Thread Stack → 定位到具体的代码行

**OOM自愈配置**（生产环境建议）：
```bash
# JVM参数：OOM时自动dump堆
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/data/logs/heapdump/
# 可选：OOM后自动重启
-XX:+ExitOnOutOfMemoryError
```

### 4.6 生产环境注意事项

> ⚠️ **生产环境使用jmap的禁忌**：
>
> 1. **`jmap -histo:live` 会触发Full GC**！如果Full GC耗时很长（比如几十秒），业务会完全卡顿
> 2. **`jmap -dump:live` 也会触发Full GC**
> 3. **`jmap -dump` 导出大堆时**（比如8GB以上），会触发"stop-the-world"暂停JVM进行快照，耗时可能几十秒甚至分钟级
> 4. **替代方案**：使用 `jcmd`（不会触发STW的持久化dump）或 `HeapDumpOnOutOfMemoryError` 参数自动dump

**安全建议**：
- 线上首选 `jcmd <PID> GC.heap_dump /path/to/dump`（更轻量）
- 或配置 `-XX:+HeapDumpOnOutOfMemoryError` 让JVM在OOM时自动dump
- 预分配dump目录，确保磁盘空间足够（dump文件 ≈ 堆大小）

> 🎯 **核心要点**：
> - `jmap -heap` 看配置和总览，`jmap -histo` 看对象分布，`jmap -dump` 导出堆快照
> - OOM排查三件套：**自动dump配置 + jmap -histo定位嫌疑对象 + MAT分析泄漏根源**
> - 注意 `:live` 的FGC副作用，生产环境慎用
> - 大堆dump会导致进程停顿，需在低峰期执行

---

## 5. jcmd — 综合诊断命令

### 5.1 基本用法

`jcmd` 是JDK 7+引入的综合诊断命令，功能覆盖jstack+jmap+jinfo，而且是官方推荐的方式。

```bash
# 列出所有Java进程
jcmd -l

# 列出目标进程支持的所有命令
jcmd <PID> help
```

### 5.2 常用子命令

| 子命令 | 作用 | 类比旧工具 |
|--------|------|-----------|
| `VM.version` | JVM版本 | - |
| `VM.flags` | 所有JVM参数 | jinfo |
| `VM.uptime` | JVM运行时间 | - |
| `VM.system_properties` | 系统属性 | - |
| `Thread.print` | 导出线程栈 | jstack |
| `GC.heap_dump` | 导出堆快照 | jmap -dump |
| `GC.class_histogram` | 对象统计 | jmap -histo |
| `GC.run` | 触发Full GC | System.gc() |
| `GC.heap_info` | 堆信息 | jmap -heap |
| `PerfCounter.print` | 性能计数器 | - |

### 5.3 实战示例

```bash
# 查看所有JVM参数
jcmd <PID> VM.flags
# -XX:CICompilerCount=3 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp
# -XX:InitialHeapSize=1073741824 -XX:MaxHeapSize=2147483648 -XX:+UseG1GC ...

# 导出线程堆栈（比jstack更轻量）
jcmd <PID> Thread.print > thread_dump.txt

# 导出堆快照（比jmap -dump对应用影响更小）
jcmd <PID> GC.heap_dump /tmp/heap_dump.hprof

# 查看对象直方图
jcmd <PID> GC.class_histogram | head -30
# 输出格式与 jmap -histo 相同

# 查看堆信息
jcmd <PID> GC.heap_info
# garbage-first heap   total 2097152K, used 1524000K
#  regions: size 1024K, count 2048
#  young: 512 regions, survivors: 20
#  old: 1024 regions
```

> 💡 **推荐**：从JDK 8开始，`jcmd` 应作为首选的诊断工具，因为它设计更现代、对运行中应用的影响更小。一个 `jcmd` 基本覆盖了jstack/jmap/jinfo的所有功能。

> 🎯 **核心要点**：jcmd是"一把刀切所有"的综合诊断工具，JDK官方推荐替代jstack+jmap+jinfo。记忆方法：`jcmd <PID> <command>` 格式统一，`Thread.print`=线程，`GC.heap_dump`=堆dump，`GC.class_histogram`=对象统计，`VM.flags`=JVM参数。

---

## 6. jinfo — JVM参数查看与动态修改

### 6.1 基本用法

`jinfo` 用于查看和动态修改JVM参数配置。

```bash
# 查看所有JVM参数
jinfo <PID>

# 查看指定参数的值
jinfo -flag <name> <PID>

# 查看指定参数的默认值
jinfo -flag -<name> <PID>
```

### 6.2 实战示例

```bash
# 查看所有JVM参数
jinfo 23456
# Attaching to process ID 23456, please wait...
# Debugger attached successfully.
# Server compiler detected.
# JVM version is 25.222-b10
# Java System Properties:
# user.dir=/opt/app
# spring.profiles.active=prod
# ...
# VM Flags:
# -XX:InitialHeapSize=1073741824
# -XX:MaxHeapSize=2147483648
# -XX:+PrintGCDetails
# -XX:+UseG1GC

# 查看单个参数
jinfo -flag MaxHeapSize 23456
# -XX:MaxHeapSize=2147483648

jinfo -flag UseG1GC 23456
# -XX:+UseG1GC

# 查看参数是否开启（- 前缀表示查询关闭值）
jinfo -flag -PrintGCDetails 23456
# -XX:-PrintGCDetails   → 说明当前开启
```

### 6.3 动态修改参数

只有 `manageable` 类型的JVM参数才能运行时动态修改。

```bash
# 开启GC日志（动态生效，无需重启）
jinfo -flag +PrintGCDetails <PID>
jinfo -flag +PrintGCDateStamps <PID>
jinfo -flag +PrintGCTimeStamps <PID>

# 设置GC日志文件（动态生效）
jinfo -flag +PrintGC <PID>

# 修改堆大小（⚠️ 不可动态修改）
jinfo -flag MaxHeapSize=4g <PID>
# 报错：java.lang.IllegalArgumentException: 'MaxHeapSize' is not a manageable flag
```

**可以动态修改的常用参数**：

| 参数 | 说明 | 应用场景 |
|------|------|---------|
| `PrintGCDetails` | 打印GC详细信息 | 动态开启GC日志排查问题 |
| `PrintGCDateStamps` | 打印GC时间戳 | 配合GC日志 |
| `PrintGCTimeStamps` | 打印GC相对时间 | 配合GC日志 |
| `HeapDumpOnOutOfMemoryError` | OOM自动dump | 动态开启dump |
| `HeapDumpPath` | dump路径 | 修改dump目录 |
| `UseG1GC` | 切换GC（⚠️ 不推荐动态改） | - |

> 🎯 **核心要点**：jinfo最实用的场景是**在线上不重启应用的前提下，动态开启GC日志**来排查问题。但大部分JVM参数（如堆大小）不支持动态修改。

---

## 7. jhat — 堆分析（已废弃）

### 7.1 简介

`jhat`（JVM Heap Analysis Tool）用于分析导出的.hprof堆转储文件。

```bash
# 启动jhat内置Web服务器（默认端口7000）
jhat heap_dump.hprof

# 指定端口
jhat -port 8000 heap_dump.hprof
```

启动后可以通过浏览器 `http://localhost:7000` 查看分析结果。

### 7.2 为什么已废弃

| 问题 | 说明 |
|------|------|
| 性能差 | 加载大堆（>1GB）极慢，容易OOM |
| 分析能力弱 | 仅提供基础查询，无泄漏分析 |
| 界面原始 | 浏览器页面简陋，交互极差 |
| 无更新维护 | JDK 9+已移除该工具 |

### 7.3 替代方案

| 工具 | 说明 | 推荐度 |
|------|------|--------|
| **Eclipse MAT** | 行业标准，自动泄漏分析 | 强烈推荐 |
| **JProfiler** | 商业级，功能最全 | 推荐（付费） |
| **VisualVM + plugins** | 开源全能 | 推荐 |
| **GCeasy** | 在线GC日志分析 | 推荐（网页版） |

> 🎯 **核心要点**：jhat已废弃，记住它能做什么即可（打开hprof文件），实际的堆分析工作用 **Eclipse MAT** 完成。

---

## 8. 诊断工具速查表

### 8.1 按场景速查

| 场景 | 工具 | 命令 | 说明 |
|------|------|------|------|
| 查看Java进程列表 | `jps` | `jps -l -v` | 获取所有Java进程的PID |
| 查看GC频率 | `jstat` | `jstat -gcutil <PID> 1000` | 实时监控GC百分比 |
| 查看堆配置 | `jmap` | `jmap -heap <PID>` | 堆大小、各代配置 |
| 查看对象统计 | `jmap` | `jmap -histo:live <PID>` | 按类统计实例数和占用内存 |
| 导出堆快照 | `jmap` | `jmap -dump:format=b,file=heap.hprof <PID>` | 保存堆现场 |
| | `jcmd` | `jcmd <PID> GC.heap_dump /tmp/heap.hprof` | 更轻量的替代方案 |
| 导出线程堆栈 | `jstack` | `jstack -l <PID>` | 查看所有线程状态 |
| | `jcmd` | `jcmd <PID> Thread.print` | 更轻量的替代方案 |
| CPU飙高排查 | `jstack` | 四步法：top→top-Hp→printf→jstack | CPU问题定位 |
| 死锁检测 | `jstack` | `jstack -l <PID> | grep deadlock` | 自动检测死锁 |
| 查看JVM参数 | `jinfo` | `jinfo -flag MaxHeapSize <PID>` | 查看单个参数 |
| 动态修改参数 | `jinfo` | `jinfo -flag +PrintGCDetails <PID>` | 无需重启开启GC日志 |
| 综合诊断 | `jcmd` | `jcmd <PID> help` | 查看支持的所有命令 |

### 8.2 按工具速查

```bash
# ============ jps ============
jps -l -v                           # 查看所有Java进程及启动参数

# ============ jstat ============
jstat -gcutil <PID> 1000            # 实时监控GC（每秒刷新）
jstat -gc <PID> 1000 5              # 详细GC统计（输出5次）
jstat -class <PID>                  # 类加载统计
jstat -gccapacity <PID>             # 各代容量配置

# ============ jstack ============
jstack -l <PID>                     # 导出线程堆栈（含锁信息）
jstack -l <PID> > thread.dump       # 保存到文件
jstack -F <PID>                     # 强制dump（jstack无响应时）

# ============ jmap ============
jmap -heap <PID>                    # 堆配置和使用情况
jmap -histo:live <PID> | head -20   # 存活对象TOP20
jmap -dump:format=b,file=dump.hprof <PID>  # 堆转储

# ============ jcmd ============
jcmd <PID> VM.flags                 # JVM参数
jcmd <PID> Thread.print             # 线程堆栈
jcmd <PID> GC.heap_dump /tmp/dump   # 堆转储
jcmd <PID> GC.class_histogram       # 对象统计
jcmd <PID> GC.heap_info             # 堆信息
jcmd <PID> VM.uptime                # JVM运行时间

# ============ jinfo ============
jinfo -flag +PrintGCDetails <PID>   # 动态开启GC日志
jinfo -flag MaxHeapSize <PID>       # 查看参数值
```

### 8.3 面试高频问题

> 💡 **面试官常问**：
> 1. "线上CPU飙到100%，怎么排查？" → 四步法：top→top -Hp→printf→jstack
> 2. "怎么判断是否有内存泄漏？" → jstat观察OU持续增长 + jmap -histo对比对象增长
> 3. "怎么定位死锁？" → jstack -l自动检测，或连续dump对比
> 4. "如何不重启应用开启GC日志？" → jinfo -flag +PrintGCDetails
> 5. "jmap -histo和jmap -histo:live有什么区别？" → :live触发FGC且仅统计存活对象
> 6. "生产环境dump堆要注意什么？" → 避免使用live（触发FGC），确保磁盘空间，低峰期操作
> 7. "jcmd和jstack/jmap有什么区别？" → jcmd是官方推荐的综合工具，更轻量，功能更全
> 8. "jstat输出中FGC次数很多怎么办？" → 检查老年代使用率，排查内存泄漏，考虑调大堆内存

---

> 🎯 **总结**：四个工具的定位可以用一句话记住——**jps找进程，jstat看GC，jstack查线程，jmap分析内存**。建议优先级：`jcmd` > `jstack+jmap` > 其他。线上排查四步法（CPU场景）必须烂熟于心。

---

**下一模块**：[05 JDK性能监控与调优工具](./05-JDK性能监控与调优工具.md) | **返回总览**：[总览](./00-JDK知识体系总览.md)
