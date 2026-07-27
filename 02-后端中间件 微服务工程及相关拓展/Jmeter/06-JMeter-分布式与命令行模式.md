# JMeter 分布式与命令行模式

> 🌐 CLI 非 GUI 模式、分布式压测架构、最佳配置调优、插件系统 —— 从单机到集群的完整方案

---

## 📚 目录

1. [CLI 命令行模式](#1-cli-命令行模式)
2. [分布式压测架构](#2-分布式压测架构)
3. [性能最佳配置](#3-性能最佳配置)
4. [JMeter Plugins 插件系统](#4-jmeter-plugins-插件系统)

---

## 1. CLI 命令行模式

### 1.1 为什么必须用 CLI

```text
GUI vs CLI 对比：

  GUI 模式（jmeter.bat / jmeter）：
    → 适合：脚本开发、调试、验证
    → 缺点：消耗大量内存和 CPU，严重拖慢压测结果！
    → 压测时绝对不能用 GUI！

  CLI 模式（jmeter -n）：
    → 适合：正式压测、CI/CD 集成
    → 优点：资源开销极小，结果更准确
    → 正式压测的唯二选择（或分布式）
```

### 1.2 常用 CLI 命令

```bash
# 基本运行
jmeter -n -t test.jmx -l result.jtl

# 完整参数
jmeter -n \
  -t test-plan.jmx \              # 测试计划文件
  -l result.jtl \                 # 结果输出文件
  -e -o report/ \                 # 生成 HTML Dashboard
  -j jmeter.log \                 # JMeter 运行日志
  -Jthreads=200 \                 # 传递属性
  -Jduration=600 \
  -GbaseUrl=https://api.example.com  # 分布式场景传给所有 slave

# 参数说明：
# -n：非 GUI 模式
# -t：测试计划文件路径
# -l：JTL 结果文件路径
# -e：生成报告（需 -o）
# -o：报告输出目录（必须为空或不存在）
# -j：JMeter 日志文件
# -J：设置 Java 属性
# -G：分布式场景传属性给所有 agent
# -R：远程 agent 列表
# -r：启动所有 remote agent
```

### 1.3 JTL 结果文件

```text
JTL (JMeter Test Log) 格式 — CSV：

  timeStamp,elapsed,label,responseCode,responseMessage,
  threadName,dataType,success,failureMessage,bytes,
  sentBytes,grpThreads,allThreads,Latency,IdleTime,Connect

典型内容：
  1750000000000,325,用户注册,200,OK,
  Thread Group 1-1,text,true,,1024,
  256,50,50,320,0,45

关键字段：
  elapsed    → 响应时间 (ms)
  Latency    → 延迟 (ms)
  Connect    → 连接时间 (ms)
  grpThreads → 当前活跃线程数
  success    → 是否成功
```

---

## 2. 分布式压测架构

### 2.1 为什么需要分布式

```text
单机 JMeter 的瓶颈：

  普通 PC：      500-1000 并发线程
  优化后：       2000-3000 并发线程
  极限调优：     5000+ 并发（内存大、CPU多）

  当需要模拟 10000+ 并发时 →
  JMeter 分布式 = 1 Controller + N Generators（Agent）

  架构：
  ┌──────────┐
  │Controller│ ← 主控：管理测试、收集结果
  │ (Master) │   不产生负载！
  └────┬─────┘
       │ RMI 通信
  ┌────┼───────────────┐
  │    ▼               ▼
  │ ┌──────┐      ┌──────┐
  │ │ Agent1│      │AgentN│  ← 从控：实际产生负载
  │ │ 1000  │      │ 1000 │
  │ │ threads│     │ threads│
  │ └──────┘      └──────┘
  └────────────────────────

  总并发 = Agent1并发 + Agent2并发 + ... = N × 1000
```

### 2.2 分布式配置

```bash
# ===== Agent (Slave) 配置 =====
# 1. 每个 Agent 机器上启动 jmeter-server
./jmeter-server        # Linux/Mac
jmeter-server.bat      # Windows

# 2. 修改 jmeter.properties（Agent 侧）：
server.rmi.localport=4000
server.rmi.ssl.disable=true   # 内网可关闭 SSL

# ===== Controller (Master) 配置 =====
# 1. 修改 jmeter.properties（Controller 侧）：
remote_hosts=192.168.1.10:4000,192.168.1.11:4000,192.168.1.12:4000

# 2. 执行分布式压测：
jmeter -n -t test.jmx -r -l result.jtl
# -r：启动所有 remote hosts

# 或指定 Agent：
jmeter -n -t test.jmx -R 192.168.1.10,192.168.1.11 -l result.jtl
```

### 2.3 分布式注意事项

```text
分布式压测的关键注意点：

  ✅ 所有 Agent 的 JMeter 版本必须一致
  ✅ 所有 Agent 的 Java 版本一致（推荐）
  ✅ 测试数据文件（CSV）要在每个 Agent 的相同路径
     → 或用 props 传递路径
  ✅ 函数/变量跨 Agent 不共享
     → ${__counter(FALSE)} 每个 Agent 各自计数
  ✅ 关闭防火墙或开放 RMI 端口
  ✅ 内网环境（RMI 不建议暴露到公网）
  ✅ CSV 数据 Sharing mode = All threads 时要注意
     → 各 Agent 独立读取，不会互相协调
```

---

## 3. 性能最佳配置

### 3.1 JMeter 本身调优

```properties
# jmeter.properties 关键配置

# 堆内存（jmeter.bat 中设置）
HEAP="-Xms2g -Xmx4g -XX:MaxMetaspaceSize=256m"

# 关闭不需要的功能
jmeter.save.saveservice.assertion_results=none    # 不保存断言详情
jmeter.save.saveservice.response_data=false        # 不保存响应体
jmeter.save.saveservice.url=false                  # 不保存完整 URL
jmeter.save.saveservice.samplerData=false          # 不保存请求体

# HTTP Sampler 优化
httpclient4.time_to_live=60000                     # 连接存活时间
httpclient.reset_state_on_thread_group_iteration=true

# 结果收集
jmeter.save.saveservice.output_format=csv          # CSV 比 XML 快
resultcollector.action_if_file_exists=APPEND       # 追加而非覆盖
```

### 3.2 OS 层面调优（Linux）

```bash
# 增大文件描述符限制
ulimit -n 65535

# 调整内核参数
sysctl -w net.ipv4.tcp_tw_reuse=1
sysctl -w net.ipv4.ip_local_port_range="1024 65535"
sysctl -w net.core.somaxconn=65535

# 关闭防火墙（内网环境）
systemctl stop firewalld
```

---

## 4. JMeter Plugins 插件系统

### 4.1 安装方式

```bash
# 方式 1：Plugin Manager（推荐）
# 1. 下载 JMeter Plugins Manager .jar → lib/ext/
# 2. 重启 JMeter → Options → Plugins Manager
# 3. 搜索安装需要的插件

# 方式 2：手动安装
# 下载 .jar → 放入 lib/ext/ → 重启 JMeter
```

### 4.2 必装插件

| 插件 | 用途 | 推荐度 |
|------|------|:-----:|
| **JMeter Plugins Manager** | 插件管理 | ⭐⭐⭐⭐⭐ |
| **Custom Thread Groups** | Stepping/Ultimate Thread Group | ⭐⭐⭐⭐⭐ |
| **3 Basic Graphs** | 响应时间/吞吐量实时图 | ⭐⭐⭐⭐ |
| **PerfMon (Servers Perf. Monitoring)** | 监控服务器 CPU/Memory | ⭐⭐⭐⭐ |
| **Throughput Shaping Timer** | 精确控制 QPS 形状 | ⭐⭐⭐⭐ |
| **Dummy Sampler** | 占位请求 | ⭐⭐⭐ |
| **WebSocket Sampler** | WebSocket 压测 | ⭐⭐⭐ |
| **Random CSV Data Set** | 随机取 CSV 行 | ⭐⭐⭐ |

### 4.3 Stepping Thread Group

```text
Custom Thread Groups → Stepping Thread Group

  This group will start：
    100 threads

  First, wait for：0 seconds

  Then start：20 threads every 30 seconds
    → 每 30 秒增加 20 个线程

  Using ramp-up：10 seconds
    → 新增线程在 10 秒内启动

  Then hold load for：300 seconds
    → 达到 100 线程后保持 5 分钟

  Finally, stop：20 threads every 30 seconds
    → 每 30 秒减少 20 个线程

  效果：逐步增加负载 → 保持峰值 → 逐步降载
  好处：观察不同并发级别下的系统表现
```

---

> 🎯 **核心要点**：**正式压测用 CLI 模式**（jmeter -n），**大并发用分布式**（Master+Agent），**插件必装 Stepping Thread Group**（阶梯加压），**OS 调优**避免出现文件描述符耗尽。

---

*创建于：2026年7月*
