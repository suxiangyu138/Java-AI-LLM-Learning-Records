# Linux 与 JVM 调优 —— 线上问题排查必备

> **原则：能写业务代码是合格，能排查线上问题是优秀。**
> Linux 命令 + JVM 工具链 + GC 日志分析 = 生产事故快速定位的三板斧。

---

## 一、Linux 高频命令（★★★★☆）

### 1.1 进程管理

```bash
# CPU 占用排查
top                  实时进程监控（按 1 看每核 CPU，按 P 按 CPU 排序，按 M 按内存排序）
ps aux | grep java   列出所有 Java 进程
jps -l               只列出 Java 进程（JDK 自带）

# 找到 CPU 占用最高的线程
top -H -p <pid>      查看进程中所有线程的 CPU 占用
printf "%x\n" <tid>  线程 ID 转 16 进制（用于匹配 jstack 输出）
```

### 1.2 内存分析

```bash
free -h              查看系统内存使用（-h 人类可读）
jmap -heap <pid>     查看 JVM 堆内存配置和使用情况
jstat -gc <pid> 1000 每秒输出 GC 统计

# 生成堆转储文件
jmap -dump:format=b,file=/tmp/heap.hprof <pid>

# 内存泄漏排查思路
1. top 发现 Java 进程内存持续增长
2. jstat -gc <pid> 1000 观察 FGC 频率（频繁 FGC 说明内存不够）
3. jmap -dump 导出堆快照
4. MAT / JProfiler 分析哪个对象占用了内存
```

### 1.3 网络调试

```bash
netstat -tlnp        查看所有监听端口
netstat -anp | grep <port>  查看端口连接数
ss -s                查看 socket 统计（比 netstat 更快）

# 网络连通性测试
curl -v http://localhost:8080/api/health  查看 HTTP 响应详情
telnet 192.168.1.100 6379                 测试 Redis 端口是否通

# 抓包分析
tcpdump -i eth0 port 8080 -w capture.pcap  抓指定端口的包
```

### 1.4 日志分析

```bash
# 实时查看日志
tail -f app.log                         实时跟踪
tail -n 100 app.log                     最后 100 行

# 关键字搜索
grep "ERROR" app.log                    搜索 ERROR
grep -A 5 -B 5 "ERROR" app.log         上下文各 5 行
grep "2026-05-27 14:" app.log | grep "ERROR"  组合过滤

# 统计分析
grep "ERROR" app.log | awk '{print $5}' | sort | uniq -c | sort -rn
# 统计各类型错误出现次数，从高到低排列

# 按时间范围提取日志
sed -n '/2026-05-27 14:00/,/2026-05-27 15:00/p' app.log
```

### 1.5 文件与权限

```bash
# 找到大文件
find / -type f -size +100M 2>/dev/null

# 磁盘使用
df -h                 磁盘分区使用情况
du -sh /path/*        目录大小汇总
du -h --max-depth=1   一层子目录大小

# 权限
chmod 755 file.sh     设置可执行权限
chown -R app:app /opt/app  递归修改所有者
```

---

## 二、JVM 调优工具链（★★★★☆）

### 2.1 工具矩阵

| 工具 | 用途 | 使用场景 |
|------|------|---------|
| jps | 列出 Java 进程 | 找到进程 PID |
| jstat | JVM 统计监控 | 实时看 GC 情况、类加载 |
| jinfo | JVM 参数查看 | 确认 JVM 启动参数是否生效 |
| jmap | 内存映射 | 生成 Heap Dump、查看内存占用 |
| jstack | 线程堆栈 | 死锁排查、线程状态分析 |
| jconsole | GUI 监控 | 开发环境监控 |
| jvisualvm | GUI 分析 | 性能分析 + 内存分析 |
| **Arthas** | 阿里线上诊断 | **推荐首选，在线 Debug** |

### 2.2 Arthas 核心命令

```bash
# 安装启动
curl -O https://arthas.aliyun.com/arthas-boot.jar
java -jar arthas-boot.jar       # 选择目标 Java 进程

# 核心命令
dashboard        仪表盘（实时线程/内存/GC）
thread           查看所有线程（thread -n 3 看 CPU 最高的 3 个线程）
thread -b        检测死锁
jad Demo.class   反编译类（确认线上代码版本）
watch Demo method "{params, returnObj, throwExp}" -x 3
                 观察方法入参、返回值、异常
trace Demo method 追踪方法调用链路（显示每步耗时）
monitor Demo method -c 5  监控方法调用统计（5 秒汇总一次）
tt -t Demo method  记录方法每次调用的入参和返回值（支持回放）
```

### 2.3 Thread Dump 分析

```bash
# 生成 Thread Dump
jstack <pid> > thread_dump.txt
# 或 kill -3 <pid>（输出到 stdout/日志文件）

# 线程状态关注：
RUNNABLE       正在执行或等待 CPU（关注 CPU 高的线程）
BLOCKED        等待获取锁（关注谁持有锁——"waiting to lock"）
WAITING        无限等待（关注 wait/join/park）
TIMED_WAITING  限时等待（关注 sleep/parkNanos）
```

**死锁识别**：`jstack <pid> | grep "deadlock"` 或在 Arthas 中执行 `thread -b`。

### 2.4 Heap Dump 分析

```bash
# 生成 Heap Dump
jmap -dump:live,format=b,file=heap.hprof <pid>
# -XX:+HeapDumpOnOutOfMemoryError 自动生成（推荐配置）

# 用 MAT（Memory Analyzer Tool）分析：
1. Leak Suspects Report：自动检测内存泄漏嫌疑
2. Dominator Tree：按内存占有量排序的对象树
3. Path to GC Roots：找到对象为什么没有被回收

# OOM 排查流程
1. 确认是堆溢出还是元空间溢出
   - java.lang.OutOfMemoryError: Java heap space → 堆溢出
   - java.lang.OutOfMemoryError: Metaspace → 元空间溢出
2. 堆溢出 → 分析 Heap Dump，找大对象
3. 元空间溢出 → 检查是否动态生成了大量类（如 CGLIB 代理）
```

---

## 三、JVM 参数调优（★★★★☆）

### 3.1 通用配置模板

```bash
# JDK 17+ G1 推荐配置（4 核 8G 内存）
java -jar app.jar \
  -Xms4g -Xmx4g \                          # 堆 4G，min=max 避免动态调整
  -XX:+UseG1GC \                           # G1 收集器
  -XX:MaxGCPauseMillis=200 \               # 期望最大停顿 200ms
  -XX:G1HeapRegionSize=4m \                # Region 大小 4MB
  -XX:ConcGCThreads=2 \                    # 并发 GC 线程数
  -XX:InitiatingHeapOccupancyPercent=45 \  # 堆占用 45% 启动并发标记
  -XX:+HeapDumpOnOutOfMemoryError \        # OOM 自动生成 Dump
  -XX:HeapDumpPath=/opt/logs/heap.hprof \  # Dump 路径
  -XX:+PrintGCDetails \                    # GC 日志（JDK 8）
  -XX:+PrintGCDateStamps \
  -Xloggc:/opt/logs/gc.log \
  -Dfile.encoding=UTF-8
```

### 3.2 参数速查表

| 参数 | 说明 | 推荐值 |
|------|------|--------|
| `-Xms` / `-Xmx` | 初始/最大堆 | 设为相同值（避免动态调整） |
| `-Xss` | 线程栈大小 | 256k-1m（默认 1m，可适当减小） |
| `-XX:MetaspaceSize` | 元空间初始 | 128m-256m |
| `-XX:MaxMetaspaceSize` | 元空间最大 | 256m-512m（防止无限增长） |
| `-XX:MaxDirectMemorySize` | 堆外内存最大 | 默认 = -Xmx |
| `-XX:SurvivorRatio` | Eden:S0 | 默认 8（不太需要改） |

---

## 四、常见线上问题排查手册

### 4.1 CPU 飙升 100%

```
排查步骤：
1. top 找到高 CPU 的 Java 进程 PID
2. top -H -p <pid> 找到高 CPU 的线程 TID
3. printf "%x\n" <tid> 转 16 进制
4. jstack <pid> | grep <hex_tid> -A 20  定位到具体代码行
5. Arthas: thread -n 3 直接看 CPU 最高的 3 个线程

常见原因：
- 死循环（代码 Bug）
- 频繁 GC（内存不足，jstat -gc 确认）
- JSON 序列化大对象
- 正则表达式回溯
```

### 4.2 内存溢出 OOM

```
排查步骤：
1. 查看 OOM 类型：Java heap space / Metaspace / Direct buffer memory
2. jmap -dump 导出堆快照
3. MAT 分析：Dominator Tree 找大对象
4. 追溯代码：哪个业务逻辑创建了大量对象？

常见原因：
- 堆溢出：缓存无限增长、数据库查询返回大量数据、ThreadLocal 没清理
- 元空间溢出：动态代理类过多、大量 Groovy 脚本
- 堆外溢出：NIO 的 DirectByteBuffer 没释放
```

### 4.3 死锁

```
排查步骤：
1. Arthas: thread -b（一行命令检测死锁）
2. jstack <pid> | grep "BLOCKED" -A 10
3. 查看"waiting to lock"和"locked"对应的是哪个对象

常见原因：
- 不同顺序获取多个锁
- 事务中调用远程服务
- 数据库死锁（并发更新同一行）
```

---

## 面试自查清单

```
□ top -H -p + jstack 排查 CPU 飙升的完整流程
□ Arthas watch/trace/tt 三个命令的使用场景
□ jstat -gc 各列的含义（S0C/S1C/EU/OU/FGC/FGCT）
□ OOM 三种类型（堆/元空间/堆外）的区分和应对
□ G1 核心参数 MaxGCPauseMillis/IHOP 的含义
□ 死锁的 Arthas 和 jstack 两种排查方式
□ 线上 JVM 参数建议配置为什么 -Xms = -Xmx
```
