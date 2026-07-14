# JVM性能监控与故障处理工具（后端面试+线上实战完整版）
## 一、监控核心五大维度（所有工具围绕这5项采集指标）
| 监控维度 | 核心指标 | 线上故障场景 |
|--------|---------|------------|
| 堆内存 | Eden/Survivor/Old使用率、元空间、堆外内存 | OOM堆溢出、Metaspace溢出、内存泄漏 |
| GC | YGC/FGC次数、单次耗时、总GC耗时、内存晋升速率 | 接口RT飙升、服务卡顿、吞吐量暴跌 |
| 线程 | 线程总数、RUNNABLE/BLOCKED/WAITING、死锁、线程池队列 | 服务假死、请求堆积、CPU居高不下 |
| CPU | 进程总CPU、GC线程CPU、业务热点方法占用 | 死循环、复杂计算、频繁反射/序列化 |
| 类加载 | 加载/卸载类数量、加载耗时 | 元空间持续上涨、动态代理类泄漏 |

工具分为两大类：**JDK原生命令行（线上Linux无GUI首选）**、**可视化/专业诊断工具（本地测试、深度分析）**。

## 二、JDK自带命令行工具（线上必备，零依赖）
### 1. jps 定位Java进程（排查第一步）
作用：只筛选JVM进程，替代`ps -ef | grep java`，快速获取PID
```shell
# 仅输出PID
jps -q
# PID + 主类全限定名（多服务区分）
jps -l
# PID + 启动JVM参数（核对-Xmx/-Xms等配置）
jps -v
```
注意：仅能查看当前用户进程；Alpine极简JDK可能缺失。

### 2. jstat GC/类加载实时监控（GC排查核心）
底层依托HotSpot内置性能计数器，低损耗可长时间监控
通用格式：`jstat -选项 PID 间隔ms 打印次数`
```shell
# 每秒打印GC完整数值，持续输出
jstat -gc 1234 1000
# 百分比展示各区使用率，快速判断内存压力
jstat -gcutil 1234 1000
# 监控类加载、卸载数量（排查元空间泄漏）
jstat -class 1234
```
核心指标解读：
- YGC/YGCT：年轻代GC总次数、总耗时
- FGC/FGCT：Full GC总次数、总耗时（持续上涨代表严重内存问题）
- GCT：全部GC累计耗时，占程序运行时间比例过高则卡顿严重

### 3. jstack 线程栈快照（死锁、CPU高、阻塞专用）
抓取全部线程调用栈、锁持有信息
```shell
# 输出线程快照到文件，带锁详情
jstack -l 1234 > thread_dump.log
# 进程卡死无响应，强制打印（慎用，会STW）
jstack -F 1234
```
三大实战场景：
1. **死锁排查**：日志出现 `Found one Java-level deadlock`，直接定位两行互斥锁代码
2. **CPU高定位**
    1）`top -Hp 1234` 查看占用CPU最高的线程十进制ID
    2）`printf "%x 线程id"` 转16进制
    3）在线程dump搜索nid=十六进制值，查看对应业务代码
3. **请求堆积阻塞**：大量BLOCKED线程卡在同一把锁，锁竞争剧烈

### 4. jmap 堆内存快照（内存泄漏、OOM定位）
```shell
# 查看堆分区配置、GC收集器、内存占用概况
jmap -heap 1234
# 打印存活对象直方图，按占用排序（触发Full GC，线上低峰使用）
jmap -histo:live 1234
# 生成hprof堆快照文件，用于MAT分析
jmap -dump:format=b,file=heap.hprof 1234
```
生产最佳实践：JVM启动参数配置OOM自动dump，避免宕机丢失现场
```
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/data/log/heap.hprof
```
缺点：`histo:live / dump` 会触发Full GC，大堆快照文件体积巨大，预留磁盘空间。

### 5. jinfo 动态查看/修改JVM参数
```shell
# 查看全部JVM参数+系统属性
jinfo 1234
# 单独查看某参数
jinfo -flag MaxHeapSize 1234
# 动态开启GC日志，无需重启
jinfo -flag +PrintGC 1234
```
限制：仅标记`manageable`的参数支持动态修改，堆大小、收集器类型不可动态调整。

### 6. jcmd 全能整合工具（JDK7+推荐，替代分散工具）
集成jps/jstat/jstack/jmap/jinfo全部能力，一站式诊断
```shell
# 查看支持指令
jcmd 1234 help
# 等价jstack
jcmd 1234 Thread.print
# 等价jmap -heap
jcmd 1234 GC.heap_info
# 开启飞行记录JFR（低损耗采样）
jcmd 1234 JFR.start duration=60s filename=rec.jfr
```

## 三、可视化&专业诊断工具
### 1. VisualVM（JDK自带，本地测试首选）
开箱即用，内置监控、线程、堆分析、Visual GC插件
核心能力：
- 实时曲线：堆、线程、CPU、类加载
- 一键检测线程死锁
- 堆Dump可视化浏览对象分布
- Visual GC插件：直观展示Eden/Survivor/Old区GC全过程
远程监控：启动服务添加JMX参数，外网连接查看测试环境进程

### 2. JMC + JFR Java飞行记录器（生产低损耗监控）
JFR是JDK内置极低开销采样工具（开销<2%），生产环境唯一允许长时间采样的内置工具
核心优势：不停业务、完整记录GC、锁竞争、方法耗时、IO、线程阻塞事件
使用方式：
1. JMC图形界面手动启动录制
2. JVM参数开机自动录制：`-XX:+StartFlightRecording`
录制生成`.jfr`文件，可离线分析全链路性能瓶颈。

### 3. Eclipse MAT 堆快照专业分析（内存泄漏神器，免费开源）
专门解析`hprof`堆文件，线上OOM标准分析工具
核心功能：
1. 直方图：按对象实例数、占用内存排序
2. 支配树Dominator Tree：找出独占大量内存的对象
3. 自动泄漏疑点报告，直接提示可疑代码
4. 引用链分析：定位对象无法被GC的静态/全局引用
标准流程：jmap生成heap.hprof → MAT打开 → 支配树找大对象 → 追溯引用链定位泄漏代码

### 4. Arthas 阿里开源线上诊断神器（微服务生产高频使用）
无需重启服务、无需修改启动参数，Java进程内附着诊断，弥补原生命令短板
核心高频命令：
```shell
# 总面板：CPU、内存、线程、GC实时汇总
dashboard
# 按CPU排序线程，定位热点、死锁
thread -n 10
# 追踪方法完整调用栈、每层耗时（定位慢接口）
trace 包名.类名 方法名
# 观测方法入参、返回值、异常
watch 包名.类名 方法名 "{params,returnObj,throwExp}"
# 生成堆快照（不强制Full GC）
heapdump /tmp/arthas-heap.hprof
# 反编译线上类，核对线上代码版本
jad 包名.类名
```
适用场景：线上临时排查慢接口、参数异常、CPU高，无需导出大量文件。

### 5. JProfiler 商业级深度性能分析
付费工具，适合复杂性能调优、高并发压测分析
特色：
- CPU调用树精准采样，区分业务/框架耗时
- 数据库、MQ、HTTP等中间件专属探针
- 锁竞争、线程阻塞可视化图谱

## 四、三大线上典型故障完整排查流程
### 场景1：服务OOM堆内存溢出
1. 查看日志区分：`Java heap space`堆溢出 / `Metaspace`元空间溢出
2. 现场保留：若未宕机，jmap导出堆快照；已宕机读取OOM自动dump文件
3. MAT打开快照，查看**支配树**，定位占用最大对象
4. 分析引用链：
   - 静态集合缓存未清理、缓存淘汰策略缺失 → 内存泄漏
   - 对象瞬时峰值过大、堆-Xmx分配过小 → 扩容堆内存/调整新生代比例
5. 修复代码或调整JVM参数，jstat持续观测内存曲线验证

### 场景2：频繁Full GC，接口延迟持续走高
1. `jstat -gcutil PID 1000` 观测FGC持续上涨、Old区使用率90%+
2. 区分诱因：
   - Eden过小 → Minor GC频繁，大量对象快速晋升老年代
   - 大对象频繁创建直接进入Old区
   - 内存泄漏，老年代对象只增不减
3. 手段：JFR记录GC事件、MAT看老年代存活对象、Arthas追踪高频创建对象的方法
4. 优化：调大新生代、优化业务减少临时对象、添加缓存过期策略、更换低延迟GC(ZGC/G1)

### 场景3：CPU打满、服务吞吐下降
1. `top -Hp PID` 找到占用最高的线程十进制ID
2. 转16进制，jstack线程dump匹配nid，查看栈信息
3. 两种典型结果：
   - 业务代码死循环、复杂计算、大量序列化反射 → 优化代码逻辑
   - GC线程CPU占比极高 → 内存压力大，走GC优化流程

## 五、工具选型最佳实践
1. **开发本地**：VisualVM + Arthas，实时监控、调试慢接口
2. **测试压测环境**：JMC(JFR) + JProfiler，全链路性能采样
3. **线上紧急快速排查**：jps → jstat → jstack → Arthas（无需导出大文件）
4. **内存泄漏/OOM事后分析**：jmap dump + MAT
5. **长期低损耗线上监控**：JFR飞行记录器，不影响业务吞吐量

## 六、面试核心背诵总结
1. 原生命令分工：
    jps找进程；jstat看GC/类加载；jstack查线程死锁/CPU热点；jmap导出堆快照；jinfo动态改参数；jcmd全能整合。
2. 线程死锁定位：jstack -l 线程dump自带死锁检测；CPU高采用top + jstack十六进制匹配线程。
3. 内存泄漏标准流程：dump堆文件 → MAT支配树 → 引用链定位静态长生命周期对象。
4. JFR优势：极低性能损耗，唯一可生产长期采样的官方工具，完整记录GC、锁、方法耗时。
5. Arthas核心价值：线上无需重启，动态trace/watch观测方法入参耗时，反编译线上代码，微服务线上排查首选。
6. 线上禁忌：高峰期执行`jmap -histo:live`，会触发Full GC造成业务卡顿，优先低峰操作或使用Arthas无GC快照。