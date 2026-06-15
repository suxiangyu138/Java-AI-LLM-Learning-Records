## 一、先把 JVM 原理搭成一张图

从面试和实战的角度，你要搞清楚 JVM 至少这 4 件事：它是什么、内存怎么分、GC 怎么干活、字节码怎么被执行。 [blog.csdn](https://blog.csdn.net/lzhcoder/article/details/86244290)

1. JVM 是什么  
   - JVM 是一个“虚拟的计算机”，负责加载 class 字节码、分配内存、执行指令和垃圾回收。 [sagamiyun](https://sagamiyun.me/archives/jvm-ji-gc-de-jie-gou-yuan-li-yu-diao-you)
   - 任何能编译成符合 class 文件规范的语言都可以跑在 JVM 上，不只 Java。 [sagamiyun](https://sagamiyun.me/archives/jvm-ji-gc-de-jie-gou-yuan-li-yu-diao-you)

2. 运行时数据区（内存结构）  
   核心是要能画出来，并说出每块干嘛用： [developer.aliyun](https://developer.aliyun.com/article/434362)
   - 堆（Heap）：存放对象实例，是 GC 的主战场，新生代（Eden + Survivor）和老年代划分要清楚。 [developer.aliyun](https://developer.aliyun.com/article/434362)
   - 虚拟机栈：每个线程一个栈，里面是栈帧（局部变量表、操作数栈等），对应方法调用。 [blog.csdn](https://blog.csdn.net/lzhcoder/article/details/86244290)
   - 程序计数器（PC）：记录当前线程下一条要执行的字节码指令位置。 [developer.aliyun](https://developer.aliyun.com/article/434362)
   - 本地方法栈：给 native 方法用的调用栈。 [blog.csdn](https://blog.csdn.net/lzhcoder/article/details/86244290)
   - 方法区 / 元空间：存放类元数据、常量、静态变量等（HotSpot 里 JDK8 之后换成元空间）。 [sagamiyun](https://sagamiyun.me/archives/jvm-ji-gc-de-jie-gou-yuan-li-yu-diao-you)

3. 类加载机制  
   关键是两点：生命周期和双亲委派。 [sagamiyun](https://sagamiyun.me/archives/jvm-ji-gc-de-jie-gou-yuan-li-yu-diao-you)
   - 生命周期：加载 → 验证 → 准备 → 解析 → 初始化 → 使用 → 卸载。 [developer.aliyun](https://developer.aliyun.com/article/434362)
   - 双亲委派：类加载请求优先交给父加载器，防止重复加载和核心类被篡改，SPI 等场景会打破这一模型。 [sagamiyun](https://sagamiyun.me/archives/jvm-ji-gc-de-jie-gou-yuan-li-yu-diao-you)

4. 执行引擎与字节码执行  
   - JVM 将字节码解释执行，同时配合 JIT（即时编译）把热点代码编译成本地机器码，提高性能。 [sagamiyun](https://sagamiyun.me/archives/jvm-ji-gc-de-jie-gou-yuan-li-yu-diao-you)
   - 重点理解“解释执行 + JIT 优化”这个组合，而不是死记具体指令。 [sagamiyun](https://sagamiyun.me/archives/jvm-ji-gc-de-jie-gou-yuan-li-yu-diao-you)

***

## 二、GC 原理：怎么收垃圾、收谁、什么时候收

调优绕不开 GC，至少要能把“分代 + 收集器 + 回收触发时机”讲明白。 [cloud.tencent](https://cloud.tencent.com/developer/article/1812722)

1. 分代收集思想  
   - 绝大多数对象“朝生夕死”，少数对象会存活很久，这是分代收集的理论基础。 [sagamiyun](https://sagamiyun.me/archives/jvm-ji-gc-de-jie-gou-yuan-li-yu-diao-you)
   - 新生代：频繁 Minor GC，通过复制算法在 Eden 和两个 Survivor 区之间拷贝存活对象。 [developer.aliyun](https://developer.aliyun.com/article/434362)
   - 老年代：存放长寿命对象，发生 Major/Full GC，采用标记-整理等算法。 [developer.aliyun](https://developer.aliyun.com/article/434362)

2. 常见 GC 算法  
   - 标记-清除：先标记再清除，速度快但会产生内存碎片。 [sagamiyun](https://sagamiyun.me/archives/jvm-ji-gc-de-jie-gou-yuan-li-yu-diao-you)
   - 标记-整理：在清除时顺便把对象压缩整理，减少碎片。 [sagamiyun](https://sagamiyun.me/archives/jvm-ji-gc-de-jie-gou-yuan-li-yu-diao-you)
   - 复制算法：在新生代使用，牺牲一部分空间换更高的回收吞吐量。 [sagamiyun](https://sagamiyun.me/archives/jvm-ji-gc-de-jie-gou-yuan-li-yu-diao-you)

3. 常见垃圾收集器（按 JDK8 为主）  
   名字不用记太多，但要知道大致特点： [developer.aliyun](https://developer.aliyun.com/article/434362)
   - Serial / ParNew：单线程 / 多线程新生代收集器，小内存场景可用。 [developer.aliyun](https://developer.aliyun.com/article/434362)
   - Parallel Scavenge / Parallel Old：吞吐量优先型，适合后台批处理等场景。 [cloud.tencent](https://cloud.tencent.com/developer/article/1812722)
   - CMS：以低停顿为目标的老年代收集器，缺点是会产生碎片。 [sagamiyun](https://sagamiyun.me/archives/jvm-ji-gc-de-jie-gou-yuan-li-yu-diao-you)
   - G1：面向服务端应用、可预测停顿时间、按 Region 管理，JDK9 以后默认收集器。 [comate.baidu](https://comate.baidu.com/zh/page/dewx8n0ek14)

4. GC 日志与指标  
   在调优前必须能看懂 GC 日志，关注这些指标： [cnblogs](https://www.cnblogs.com/crazymakercircle/p/18200975)
   - 吞吐量：业务执行时间 / (业务时间 + GC 时间)。 [cnblogs](https://www.cnblogs.com/crazymakercircle/p/18200975)
   - 停顿时间：每次 GC 造成 STW（Stop-The-World）的时间，尤其是最大停顿。 [cnblogs](https://www.cnblogs.com/crazymakercircle/p/18200975)
   - Full GC 频率：Full GC 频繁通常说明老年代、元空间、代码有问题。 [cloud.tencent](https://cloud.tencent.com/developer/article/1812722)

***

## 三、JVM 调优：什么时候调、调什么、怎么下手

这里的“调优”不是背一堆 -XX 参数，而是掌握一个完整的方法论：什么时候该考虑调 JVM，目标是什么，用哪些工具和步骤。 [liaoxuefeng](https://liaoxuefeng.com/blogs/all/2020-03-12-jvm-tuning/index.html)

1. 什么时候需要 JVM 调优  
   一般认为是“性能优化的最后一颗子弹”，先优化代码再动 JVM 参数。 [liaoxuefeng](https://liaoxuefeng.com/blogs/all/2020-03-12-jvm-tuning/index.html)
   常见需要调优的信号： [cloud.tencent](https://cloud.tencent.com/developer/article/1812722)
   - 老年代内存持续上涨接近上限。  
   - Full GC 次数频繁，甚至每几秒一次。  
   - GC 停顿时间过长（如单次超过 1s）。  
   - 出现 OOM（堆、元空间、本地内存等）。  
   - 吞吐量明显不达标、接口响应时间波动大。  

2. 调优目标：吞吐量 vs 延迟 vs 内存占用  
   这三个构成 “不可能三角”，调优时只能取舍两项。 [comate.baidu](https://comate.baidu.com/zh/page/dewx8n0ek14)
   - 吞吐量优先：偏后台批任务，可接受较长单次 GC 停顿。 [cnblogs](https://www.cnblogs.com/crazymakercircle/p/18200975)
   - 延迟优先：面向用户的在线服务，要求低停顿。 [cloud.tencent](https://cloud.tencent.com/developer/article/1812722)
   - 内存占用：资源紧张时需要控制。 [cnblogs](https://www.cnblogs.com/crazymakercircle/p/18200975)

3. 实际调优步骤（面试好讲，工作也能用）  
   综合几篇实战文章，总结出一套比较认同的流程： [liaoxuefeng](https://liaoxuefeng.com/blogs/all/2020-03-12-jvm-tuning/index.html)

   - 第一步：采集数据，确认是否真的需要调优  
     - 打开 GC 日志，收集一段时间的 GC 情况。  
     - 配合监控（如 GC 次数、堆使用、RT、QPS）分析是否存在瓶颈。 [liaoxuefeng](https://liaoxuefeng.com/blogs/all/2020-03-12-jvm-tuning/index.html)

   - 第二步：设定量化目标  
     - 比如“Full GC 不超过 1 次/小时”，“P99 延迟 < 200ms”，“堆使用不超过 70%”。 [cloud.tencent](https://cloud.tencent.com/developer/article/1812722)

   - 第三步：选择收集器与基本堆大小  
     - 在线服务：优先 G1 或者 CMS（老系统），目标是平衡吞吐和停顿。 [cnblogs](https://www.cnblogs.com/crazymakercircle/p/18200975)
     - 大内存、强一致低延迟服务也常用 G1，通过 MaxGCPauseMillis 控制停顿预期。 [comate.baidu](https://comate.baidu.com/zh/page/dewx8n0ek14)

   - 第四步：调整堆与分代比例  
     - 控制新生代大小，让大部分短命对象在新生代被回收，减少晋升老年代。 [cloud.tencent](https://cloud.tencent.com/developer/article/1812722)
     - 调整 Survivor 区比例，避免频繁晋升老年代。 [developer.aliyun](https://developer.aliyun.com/article/434362)

   - 第五步：反复压测 + 调参  
     - 不同参数组合进行压测，对比吞吐量、延迟、GC 日志。 [comate.baidu](https://comate.baidu.com/zh/page/dewx8n0ek14)
     - 找到最合适的配置后，在生产做灰度发布，并持续观察。 [liaoxuefeng](https://liaoxuefeng.com/blogs/all/2020-03-12-jvm-tuning/index.html)

4. 常见 JVM 参数维度（知道分类就够，细节可以边查边用）  
   - 堆、栈、元空间大小相关：Xms/Xmx/Xss/MaxMetaspaceSize 等。 [github](https://github.com/alleriagit/Java-Tutorial/blob/master/docs/java/jvm/%E6%B7%B1%E5%85%A5%E7%90%86%E8%A7%A3JVM%E8%99%9A%E6%8B%9F%E6%9C%BA10%EF%BC%9AJVM%E5%B8%B8%E7%94%A8%E5%8F%82%E6%95%B0%E4%BB%A5%E5%8F%8A%E8%B0%83%E4%BC%98%E5%AE%9E%E8%B7%B5.md)
   - GC 行为相关：选择收集器、配置 G1、CMS 等。 [github](https://github.com/alleriagit/Java-Tutorial/blob/master/docs/java/jvm/%E6%B7%B1%E5%85%A5%E7%90%86%E8%A7%A3JVM%E8%99%9A%E6%8B%9F%E6%9C%BA10%EF%BC%9AJVM%E5%B8%B8%E7%94%A8%E5%8F%82%E6%95%B0%E4%BB%A5%E5%8F%8A%E8%B0%83%E4%BC%98%E5%AE%9E%E8%B7%B5.md)
   - 日志与诊断：打印 GC 日志、HeapDump、JFR 等。 [github](https://github.com/alleriagit/Java-Tutorial/blob/master/docs/java/jvm/%E6%B7%B1%E5%85%A5%E7%90%86%E8%A7%A3JVM%E8%99%9A%E6%8B%9F%E6%9C%BA10%EF%BC%9AJVM%E5%B8%B8%E7%94%A8%E5%8F%82%E6%95%B0%E4%BB%A5%E5%8F%8A%E8%B0%83%E4%BC%98%E5%AE%9E%E8%B7%B5.md)

***

## 四、你可以按这个顺序系统学完（适配你本科+后端路线）

给你一个“2–3 周搞懂 JVM 原理 + 入门调优”的自学节奏，适合边看书边做实验。 [github](https://github.com/alleriagit/Java-Tutorial/blob/master/docs/java/jvm/%E6%B7%B1%E5%85%A5%E7%90%86%E8%A7%A3JVM%E8%99%9A%E6%8B%9F%E6%9C%BA10%EF%BC%9AJVM%E5%B8%B8%E7%94%A8%E5%8F%82%E6%95%B0%E4%BB%A5%E5%8F%8A%E8%B0%83%E4%BC%98%E5%AE%9E%E8%B7%B5.md)

1. 第 1 周：原理打底  
   - 目标：能画出 JVM 运行时数据区、类加载流程，能用自己的话讲出来。 [blog.csdn](https://blog.csdn.net/lzhcoder/article/details/86244290)
   - 任务：  
     - 整理一份你自己的 JVM 笔记（内存结构 + 类加载 + 执行引擎）。 [developer.aliyun](https://developer.aliyun.com/article/434362)
     - 写一段简单代码，观察栈溢出（递归深度）、堆 OOM（大量列表），感受栈、堆的差异。 [blog.csdn](https://blog.csdn.net/lzhcoder/article/details/86244290)

2. 第 2 周：GC 与收集器  
   - 目标：能解释“为什么要分代”“不同收集器适用于什么场景”，能看懂基础 GC 日志。 [cloud.tencent](https://cloud.tencent.com/developer/article/1812722)
   - 任务：  
     - 在本地写一个简单的循环分配对象程序，多次尝试不同堆大小、不同收集器，观察 GC 行为变化。 [github](https://github.com/alleriagit/Java-Tutorial/blob/master/docs/java/jvm/%E6%B7%B1%E5%85%A5%E7%90%86%E8%A7%A3JVM%E8%99%9A%E6%8B%9F%E6%9C%BA10%EF%BC%9AJVM%E5%B8%B8%E7%94%A8%E5%8F%82%E6%95%B0%E4%BB%A5%E5%8F%8A%E8%B0%83%E4%BC%98%E5%AE%9E%E8%B7%B5.md)
     - 把每次实验的堆设置、GC 日志、GC 次数和停顿时间记录下来。 [comate.baidu](https://comate.baidu.com/zh/page/dewx8n0ek14)

3. 第 3 周：调优方法论 + 小实验  
   - 目标：能说出一套完整的 JVM 调优流程，知道在什么场景下怎么下手。 [liaoxuefeng](https://liaoxuefeng.com/blogs/all/2020-03-12-jvm-tuning/index.html)
   - 任务：  
     - 模拟一个“接口响应慢”的场景（例如频繁创建大对象），用 GC 日志 + 工具（JVisualVM 等）定位问题。 [cloud.tencent](https://cloud.tencent.com/developer/article/1812722)
     - 设计两套 JVM 启动参数：一套吞吐量优先，一套延迟优先，比对结果。 [comate.baidu](https://comate.baidu.com/zh/page/dewx8n0ek14)
