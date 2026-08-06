# 09 - JVM 排查工具详解

> JVM 排查工具是 Java 后端处理线上问题的核心武器：jps/jstack/jstat/jmap/MAT/jinfo + Arthas，覆盖进程、线程、GC、堆、在线诊断全链路。从命令到实战，一章掌握。

## 📚 目录

1. [jps —— 查看 Java 进程](#1-jps-查看-java-进程)
2. [jstack —— 线程栈分析](#2-jstack-线程栈分析)
3. [jstat —— JVM 统计监控](#3-jstat-jvm-统计监控)
4. [jmap —— 堆内存分析](#4-jmap-堆内存分析)
5. [MAT —— 堆快照分析](#5-mat-堆快照分析)
6. [jinfo —— JVM 参数查看与修改](#6-jinfo-jvm-参数查看与修改)
7. [Arthas —— 在线诊断工具](#7-arthas-在线诊断工具)

---

## 1. jps —— 查看 Java 进程

`jps` 是最基本的JVM工具，类似于Linux的 `ps`，但只显示Java进程。

```bash
# 基本用法
jps                                     # 显示Java进程的PID和主类名
jps -l                                  # 显示全类名（推荐，避免同名类混淆）
jps -v                                  # 显示JVM启动参数（查看JVM配置是否正确）
jps -m                                  # 显示main方法的参数

# 常用组合
jps -lv | grep -E "order|payment"       # 过滤指定服务
jps -lv | grep -v "Bootstrap"           # 排除Tomcat内置进程

# 输出示例
# 5678 order-service.jar -Xms2g -Xmx2g -Dspring.profiles.active=prod
# 9012 payment-service.jar -Xms1g -Xmx1g -Dspring.profiles.active=prod
```

> **注意**：如果 `jps` 找不到Java进程，可能因为当前用户权限不足，尝试切换到运行该Java进程的用户，或使用 `ps -ef | grep java`。

**jps与ps对比**：

| 场景 | jps | ps -ef \| grep java |
|------|-----|---------------------|
| 仅看Java进程 | 简洁，直接显示 | 输出杂乱 |
| 查看JVM参数 | jps -v 直接显示 | 需要看完整命令行 |
| 进程不存在时 | 明确不显示 | 有时残留grep自身进程 |
| 权限问题 | 某些环境受限 | 一直可用 |

## 2. jstack —— 线程栈分析

`jstack` 是解决死锁、线程挂起、CPU飙升等问题的一把利器。

```bash
jstack <pid>                            # 打印所有线程栈
jstack -l <pid>                         # 显示锁信息（检测死锁必备）
jstack -m <pid>                         # 混合模式，显示本地方法栈

# 常用操作
jstack <pid> | grep -A 30 "BLOCKED"     # 查看所有被阻塞的线程
jstack <pid> | grep -A 30 "WAITING"     # 查看所有等待状态的线程
jstack <pid> > /tmp/thread.dump         # 保存线程栈到文件

# 在线程栈中搜索指定线程ID（十六进制）
jstack <pid> | grep -A 30 "nid=0x1234"
```

**线程状态解读**：

| 状态 | 含义 | 典型原因 |
|------|------|----------|
| RUNNABLE | 正在执行或等待CPU | 正常，但如果大量线程持续RUNNABLE可能CPU过高 |
| BLOCKED | 等待获取锁（被阻塞） | 锁竞争激烈，其他线程持有锁不释放 |
| WAITING | 无限期等待（Object.wait()不带超时、LockSupport.park()） | 池化线程空闲等待，数量多正常 |
| TIMED_WAITING | 有超时等待（sleep、wait(timeout)、parkNanos） | 正常，长时间存在可能有问题 |
| NEW | 线程已创建但未启动 | 罕见 |
| TERMINATED | 线程已结束 | 正常 |

**死锁检测**：

```bash
jstack -l <pid>

# 如果存在死锁，输出中会有明显提示：
# Found one Java-level deadlock:
# =============
# "thread-1":
#   waiting to lock <0x000000076b5f3e78> (a java.lang.String)
#   which is held by "thread-2"
# "thread-2":
#   waiting to lock <0x000000076b5f3e48> (a java.lang.String)
#   which is held by "thread-1"
#
# Found 1 deadlock.
```

**实战排查CPU高**：

```bash
# Step 1: 找到CPU高的Java进程
top -c                                 # 按P（大写P）按CPU排序
# 找到PID: 5678

# Step 2: 查看进程中哪些线程CPU高
top -H -p 5678                         # 按P排序
# 找到TID: 5790（十进制的线程ID）

# Step 3: 将TID转十六进制
printf "%x\n" 5790                     # 输出: 169e

# Step 4: 在线程栈中定位代码
jstack 5678 | grep -A 30 "nid=0x169e"  # 就能看到具体哪行代码在消耗CPU
```

## 3. jstat —— JVM 统计监控

`jstat` 是监控JVM GC行为的最佳工具，可以实时观察堆内存各区域的变化和GC频率。

```bash
# 基本用法（每1000ms采样一次，共10次）
jstat -gc <pid> 1000 10

# 常用输出格式
jstat -gc <pid>                         # 各区域容量和使用量
jstat -gcutil <pid> 1000                # 各区域使用率百分比（最常用）
jstat -gccause <pid>                    # 最近一次GC的原因
jstat -gcold <pid>                      # 老年代GC情况
```

**jstat -gc输出解读**：

```
S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU    YGC   YGCT   FGC   FGCT    GCT
512.0  512.0  128.0  0.0    2048.0   1024.0   4096.0     2048.0   256.0  240.0  150  3.450   5    1.200   4.650
```

| 字段 | 含义 | 排查要点 |
|------|------|----------|
| S0C/S1C | Survivor区容量 | 通常相等 |
| S0U/S1U | Survivor区使用量 | 一空一满正常；S0U>S0C说明对象晋升前就放不下 |
| EC/EU | Eden区容量/使用量 | EU接近EC时即将发生Minor GC |
| OC/OU | 老年代容量/使用量 | OU持续增长且不降，可疑内存泄漏 |
| MC/MU | 元空间容量/使用量 | MU接近MC说明加载类太多 |
| YGC/YGCT | Young GC次数/耗时 | 频率高说明Eden过小 |
| FGC/FGCT | Full GC次数/耗时 | FGC频繁且OU不降，典型内存泄漏信号 |
| GCT | 总GC耗时 | 占比过高影响吞吐量 |

**jstat -gcutil输出解读**：

```
S0     S1     E      O      M     YGC   YGCT   FGC   FGCT   GCT
25.00   0.00  80.00  50.00  93.75  150  3.450    5   1.200  4.650
```

这将各区域的使用量转换为百分比，更加直观。

**线上排查三板斧**：

```bash
# 1. 持续观察GC情况（每2秒输出一次）
jstat -gcutil <pid> 2000

# 2. 判断是否有内存泄漏
# 如果 Full GC 次数（FGC）持续增长，且老年代使用率（O）在每次FGC后不下降或下降很少
# 几乎可以断定存在内存泄漏

# 3. 查看GC原因
jstat -gccause <pid>
# 输出：LGCC（上次GC原因）和GCC（当前GC原因）
# 常见原因：Allocation Failure（分配失败）, System.gc(), Metadata GC Threshold等
```

## 4. jmap —— 堆内存分析

`jmap` 是Java堆内存的"CT机"，可以查看堆配置、统计对象分布、导出堆快照。

```bash
# 查看堆概要信息
jmap -heap <pid>

# 查看对象统计（按对象数量/占用内存排序）
jmap -histo <pid> | head -20            # 显示前20个最多的类

# 只统计存活对象（会触发Full GC！慎用！）
jmap -histo:live <pid> | head -20

# 导出堆快照（核心功能）
jmap -dump:format=b,file=/tmp/heap.hprof <pid>      # 导出包含非存活对象的堆
jmap -dump:live,format=b,file=/tmp/heap.hprof <pid>  # 只导出存活对象（推荐，文件更小）
```

**jmap -heap 输出解读**：

```
Attaching to process ID 5678, please wait...
Debugger attached successfully.

using thread-local object allocation.
Garbage Collector (GC) G1 Young Generation, G1 Old Generation

Heap Configuration:
   MinHeapFreeRatio         = 40
   MaxHeapFreeRatio         = 70
   MaxHeapSize              = 2147483648 (2048.0MB)    # -Xmx
   NewSize                  = 536870912 (512.0MB)      # 新生代初始大小
   MaxNewSize               = 536870912 (512.0MB)      # 新生代最大大小
   OldSize                  = 1610612736 (1536.0MB)    # 老年代大小

Heap Usage:
G1 Heap:
   regions  = 2048
   capacity = 2147483648 (2048.0MB)
   used     = 1572864000 (1500.0MB)     # 已使用堆内存
   free     = 574619648 (548.0MB)
   73.24% used

G1 Young Generation:
   Eden regions: 512 -> capacity = 536870912 (512.0MB)
   Survivor regions: 32 -> capacity = 33554432 (32.0MB)

G1 Old Generation:
   regions = 956 -> capacity = 1543503872 (1472.0MB)
   used    = 1472.0MB                   # 老年代几乎占满
```

**jmap -histo 输出解读**：

```
 num     #instances         #bytes  class name
----------------------------------------------
   1:       1200000      96000000  [B          # byte[] 数组
   2:        800000      64000000  [C          # char[] 数组
   3:       1000000      24000000  java.lang.String
   4:        500000      24000000  com.example.service.Order
   5:        300000      19200000  java.util.HashMap$Node
```

**排查实战**：

```bash
# 1. 快速判断内存泄漏
jstat -gcutil <pid> 2000 5
# 如果 FGC 频繁（10分钟内多次）且 O区使用率不降 -> 内存泄漏

# 2. 找到占用内存最多的对象
jmap -histo:live <pid> | head -30
# 找到 byte[], Order, String 等占用最大的类

# 3. 导出堆快照（线上需谨慎，会STW）
jmap -dump:live,format=b,file=/tmp/heap_$(date +%Y%m%d_%H%M%S).hprof <pid>

# 4. 将hprof文件下载到本地，用MAT分析
```

> **线上注意事项**：`jmap -histo:live` 和 `jmap -dump:live` 会触发Full GC，造成STW（Stop-The-World）。生产环境高峰期间慎用，建议在低峰期操作。如果不想触发Full GC，去掉 `:live` 参数，但导出的文件会更大。

## 5. MAT —— 堆快照分析

MAT是Eclipse开发的堆内存分析工具，是定位内存泄漏的最终利器。

**核心功能**：

1. **Leak Suspects（泄漏嫌疑分析）**：一键分析，MAT自动找出最可能的内存泄漏点
2. **Dominator Tree（支配树）**：按对象保留的堆大小排序，快速定位大对象
3. **Histogram（直方图）**：与jmap -histo类似，但可交互式查看引用关系
4. **GC Roots追踪**：从GC Roots到对象的完整引用链，判断对象为何没有被回收

**使用步骤**：

```
1. 使用 jmap -dump 导出堆快照
2. 在MAT中打开 .hprof 文件
3. 点击 "Leak Suspects Report" 生成泄漏分析
4. 查看 "Problem Suspects" 查看疑似泄漏点
5. 点击 Details 查看引用链
6. 在代码中找到对应的根对象，修复泄漏
```

**关键视图说明**：

- **Leak Suspects**：给出最可能的内存泄漏点，描述对象大小和保留原因
- **Dominator Tree**：按 "Retained Heap"（保留堆）排序，数值越大说明该对象及其子对象占用的总内存越多
- **Path to GC Roots**：显示GC Roots到目标对象的引用路径，排除不应有的强引用

**MAT替代工具**：

- **VisualVM**：免费可视化工具，集成了jstat/jstack/jmap功能，适合快速观察
- **JProfiler**：商业工具，功能全面，支持CPU/Memory/线程等全面分析
- **Arthas**：阿里开源工具（见下节），可以部分替代MAT的在线分析

## 6. jinfo —— JVM 参数查看与修改

```bash
# 查看所有JVM参数
jinfo <pid>

# 查看指定参数
jinfo -flag MaxHeapSize <pid>           # 查看最大堆大小
jinfo -flag PrintGCDetails <pid>        # 查看GC日志是否开启
jinfo -flags <pid>                      # 查看JVM参数（非默认值）

# 动态修改JVM参数（仅限manageable参数）
jinfo -flag +PrintGCDetails <pid>       # 开启GC日志
jinfo -flag -PrintGCDetails <pid>       # 关闭GC日志
jinfo -flag HeapDumpPath=/tmp/ <pid>    # 设置堆转储路径

# 注意：不是所有参数都支持动态修改，不可修改的参数会报错
```

## 7. Arthas —— 在线诊断工具

Arthas是阿里巴巴开源的Java诊断工具，无需修改代码、无需重启应用，即可在线排查问题，是Java线上排查的神器。

```bash
# 安装与启动
curl -O https://arthas.aliyun.com/arthas-boot.jar
java -jar arthas-boot.jar              # 选择目标Java进程
# 或直接指定PID
java -jar arthas-boot.jar <pid>

# 在支持的环境中也可用
wget -O arthas-boot.jar https://arthas.aliyun.com/arthas-boot.jar && java -jar arthas-boot.jar
```

#### dashboard —— 实时数据面板

```bash
# 输入dashboard后显示实时面板，每5秒刷新一次
dashboard

# 显示内容：
# - 线程信息：活跃线程数、守护线程数
# - 内存信息：堆各区域使用量、GC统计
# - 系统信息：CPU使用率、负载等
# - GC信息：YGC次数、FGC次数、GC耗时

# 等同于：top + jstat -gcutil + free 的集合体
```

#### thread —— 线程分析

```bash
thread                               # 显示所有线程及其CPU使用率
thread -b                            # 检测死锁（自动检测并显示死锁线程）
thread -n 3                          # 显示CPU最忙的前3个线程（定位CPU飙高）
thread -n 3 -i 2000                  # 每2秒采集一次，共3次（避免偶然性）

thread <tid>                         # 显示指定线程的栈信息
thread --state BLOCKED               # 显示所有阻塞状态的线程

# 定位CPU高最实用的方式：
# 在Arthas中直接执行 thread -n 3，无需手动转换线程ID
```

#### watch —— 方法观测

`watch` 可以监控方法的入参、返回值、异常，是排查业务逻辑问题的核心命令。

```bash
# 监控方法调用（观察返回值）
watch com.example.service.OrderService getOrder "{params, returnObj}"

# 监控方法异常
watch com.example.service.OrderService getOrder "{params, throwExp}"

# 展开对象深度（-x控制展开层级）
watch com.example.service.OrderService getOrder "{params, returnObj}" -x 3

# 条件过滤（只观测耗时超过100ms的调用）
watch com.example.service.OrderService getOrder "{params, returnObj}" "#cost>100"

# 限制输出次数（-n）
watch com.example.service.OrderService getOrder "{params, returnObj}" -n 5

# 按照方法执行条件观察（只有当第一个参数="123"时才观测）
watch com.example.service.OrderService getOrder "{params, returnObj}" "params[0]=="123""
```

**参数说明**：

| 表达式 | 含义 |
|--------|------|
| `params` | 方法入参数组 |
| `returnObj` | 返回值 |
| `throwExp` | 抛出的异常 |
| `target` | 当前对象（this） |
| `#cost` | 方法执行耗时（毫秒） |
| `-x N` | 展开对象的深度，默认1 |

#### trace —— 方法调用链路追踪

`trace` 是最强大的定位性能瓶颈的命令，可以追踪方法内部每个子调用的耗时。

```bash
# 追踪方法调用链路
trace com.example.service.OrderService getOrder

# 只显示耗时超过100ms的调用
trace com.example.service.OrderService getOrder "#cost>100"

# 跳过JDK类库的追踪
trace --skipJDKMethod false com.example.service.OrderService getOrder

# 按调用次数采样（-n）
trace com.example.service.OrderService getOrder -n 3

# 输出示例：
# `---ts=2024-06-13 14:23:45 thread=http-nio-8080-exec-10
#     `---[10.1234ms] com.example.service.OrderService:getOrder()
#         +---[5.0123ms] com.example.service.OrderService:validateOrder()   # 验证花了5ms
#         +---[3.0012ms] com.example.service.OrderService:calculatePrice()  # 计算花了3ms
#         `---[2.0011ms] com.example.service.OrderService:saveOrder()       # 保存花了2ms
```

#### jad —— 反编译

`jad` 可以实时反编译JVM中加载的类，验证部署的代码是否是最新版本。

```bash
# 反编译整个类
jad com.example.controller.OrderController

# 反编译指定方法
jad com.example.controller.OrderController getOrder

# 仅显示类信息（不显示源代码）
jad --source-only com.example.controller.OrderController

# 反编译并显示行号（对比行号定位问题代码）
jad --lineNumber com.example.controller.OrderController

# 应用场景：怀疑部署的代码不是最新版本时
# 直接反编译对比，比查git版本还快
```

#### monitor —— 方法调用统计

```bash
# 监控方法调用统计（每5秒输出一次）
monitor -c 5 com.example.service.OrderService getOrder

# 输出示例：
# timestamp            class                            method    total  success  fail  avg-rt(ms)
# 2024-06-13 14:23:45  com.example.service.OrderService getOrder  120    118      2     45.23

# 非常适合监控接口的成功率、平均响应时间
```

#### tt（TimeTunnel）—— 时空隧道

`tt` 可以记录方法的每次调用，支持回放历史调用，是复现问题的利器。

```bash
# 记录方法的每次调用
tt -t com.example.service.OrderService getOrder

# 查看历史记录
tt -l

# 查看某次调用的详情（-i 指定索引）
tt -i 1000

# 回放某次调用（重新执行）
tt -i 1000 -p

# 应用场景：线上偶发问题，记录下异常调用后，回放分析
```

#### redefine —— 热替换class

`redefine` 可以在不重启JVM的情况下替换class文件，用于线上紧急修复。

```bash
# 将编译好的 class 文件热替换到 JVM 中
redefine -p /tmp/OrderService.class

# 也可以替换多个类
redefine -p /tmp/OrderService.class /tmp/OrderController.class
```

> **注意**：`redefine` 不能添加/删除字段或方法，只能修改方法体。这是最后的手段，正规修复仍然需要走发布流程。

---


---

**上一模块**：[08-Linux 权限与用户管理深度](08-Linux权限与用户管理深度.md) ｜ **下一模块**：[10-线上问题排查流程与速查表](10-线上问题排查流程与速查表.md) ｜ **返回总览**：[00-知识体系总览](00-Linux知识体系总览.md)

**【参考来源】**
- JDK 工具参考（jps/jstack/jstat/jmap/jinfo）：https://docs.oracle.com/en/java/javase/21/docs/specs/man/
- Arthas 官方文档：https://arthas.aliyun.com/doc/
