# 06-HBase 集群部署与运维

> HBase 本身是"无状态计算层 + HDFS 存储层 + ZooKeeper 协调层"的组合，部署的难点不在装软件，而在版本矩阵、配置基线、容量规划与日常巡检。本篇给出一套可落地的 3.0 集群部署与运维流程。

---

## 目录

1. [部署形态选型](#1-部署形态选型)
2. [版本矩阵与前置依赖](#2-版本矩阵与前置依赖)
3. [节点规划](#3-节点规划)
4. [配置基线](#4-配置基线)
5. [部署步骤](#5-部署步骤)
6. [启动与验证](#6-启动与验证)
7. [高可用设计](#7-高可用设计)
8. [监控体系](#8-监控体系)
9. [日常运维操作](#9-日常运维操作)
10. [备份与恢复](#10-备份与恢复)
11. [扩容与缩容](#11-扩容与缩容)
12. [安全加固](#12-安全加固)
13. [常见部署问题](#13-常见部署问题)
14. [小结](#14-小结)

---

## 1. 部署形态选型

| 形态 | 组成 | 适用 |
|------|------|------|
| Standalone | 单 JVM，本地文件系统，内置 ZK | 本地开发、功能验证 |
| Pseudo-distributed | 单机，HDFS + 独立 ZK 进程 | 学习、集成测试 |
| Fully-distributed | 多机，独立 ZK 集群 + HDFS 集群 | 生产环境 |

```text
生产标准形态：
  ZooKeeper 集群（3 或 5 节点，独立部署）
  HDFS 集群（NameNode HA + 多 DataNode）
  HBase（2 个 HMaster + N 个 RegionServer，RS 与 DN 同机）
```

不推荐的形态：

```text
单机伪分布式跑生产：无副本、无容错、重启即停服
RS 与 DN 分离部署：失去数据本地化与短路读
ZK 与 HBase 混部：ZK 对延迟敏感，容易被 HBase 的 GC/IO 拖累
```

---

## 2. 版本矩阵与前置依赖

### 2.1 2026 年版本基线

| 组件 | 版本 | 说明 |
|------|------|------|
| HBase | 3.0.0 | 2026-08-05 GA，强制 JDK 17 |
| HBase（保守线） | 2.6.6 / 2.5.15 | 2026-06-09，仍维护 |
| JDK | 17（3.0 强制） | 2.x 可用 8/11/17 |
| Hadoop | 3.3.x / 3.4.x | HBase 3.0 要求 Hadoop 3 |
| ZooKeeper | 3.8.x / 3.9.x | 独立集群 |
| Phoenix | 与 HBase 版本匹配 | 需要 SQL 时部署 |
| 操作系统 | Linux（CentOS/Rocky/Ubuntu） | 关闭 swap、透明大页 |

### 2.2 前置检查清单

```text
① JDK 17 安装并设为默认（3.0）
② Hadoop 3 集群可用，hdfs dfs -ls / 正常
③ ZooKeeper 集群可用，zkCli.sh 能连接
④ 所有节点时钟同步（NTP/chrony）
⑤ 主机名与 /etc/hosts 一致（HBase 依赖主机名注册）
⑥ 关闭 swap 或 vm.swappiness=1
⑦ 关闭透明大页（THP），避免 GC 抖动
⑧ 文件句柄数 ulimit -n ≥ 65536
⑨ 节点间 SSH 免密（启动脚本需要）
⑩ 防火墙放通：16000（Master RPC）、16020（RS RPC）、16010/16030（Web UI）、2181（ZK）
```

版本兼容是升级事故的第一来源，必须逐项核对：

```text
HBase 3.0 客户端 ↔ 3.0 服务端：兼容
HBase 2.x 客户端 ↔ 3.0 服务端：不保证
HBase 3.0 客户端 ↔ 2.x 服务端：不保证
自定义 Coprocessor：必须针对目标版本重新编译与回归
Phoenix / Spark / Flink connector：查各自的兼容矩阵
```

---

## 3. 节点规划

一个中等规模生产集群（日增 1-5TB）的参考：

```text
节点角色划分：

  ZK 集群：3 台（可与 NameNode 混部，但不与 RS 混部）
  NameNode：2 台（HA）
  HMaster：2 台（HA，可与 NameNode 混部）
  RegionServer + DataNode：N 台（同机部署）

单 RS 节点配置参考：
  CPU：16-32 核
  内存：64-128GB（堆 32-64GB，剩余给堆外 BucketCache 与系统）
  磁盘：12-24 块 SATA/SAS HDD 或 NVMe，RAID10 或 JBOD
  网络：10GbE 起步
```

单机容量估算：

```text
单 RS 可托管 Region 数：20-200（建议不超过 200）
单 Region 目标大小：10-50GB
单 RS 管理数据量：2-10TB（取决于磁盘与 Region 大小）

例：100TB 数据、单 Region 30GB → 约 3400 个 Region
    按每 RS 100 个 Region → 34 台 RS
```

> **提示**：Region 数量不是越多越好。每个 Region 都有 MemStore、文件句柄、管理开销。Region 太小（< 1GB）会导致文件数爆炸与 Compaction 压力；Region 太大（> 50GB）会导致分裂与恢复时间过长。

---

## 4. 配置基线

### 4.1 hbase-site.xml 核心项

```xml
<configuration>
  <property>
    <name>hbase.rootdir</name>
    <value>hdfs://nameservice1/hbase</value>
  </property>

  <property>
    <name>hbase.cluster.distributed</name>
    <value>true</value>
  </property>

  <property>
    <name>hbase.zookeeper.quorum</name>
    <value>zk1,zk2,zk3</value>
  </property>

  <property>
    <name>hbase.zookeeper.property.dataDir</name>
    <value>/data/zookeeper</value>
  </property>

  <property>
    <name>hbase.tmp.dir</name>
    <value>/data/hbase/tmp</value>
  </property>

  <property>
    <name>hbase.regionserver.global.memstore.size</name>
    <value>0.4</value>
  </property>

  <property>
    <name>hfile.block.cache.size</name>
    <value>0.4</value>
  </property>

  <property>
    <name>hbase.hregion.majorcompaction</name>
    <value>0</value>
  </property>

  <property>
    <name>hbase.hstore.blockingStoreFiles</name>
    <value>32</value>
  </property>

  <property>
    <name>hbase.regionserver.handler.count</name>
    <value>64</value>
  </property>

  <property>
    <name>hbase.regionserver.thread.compaction.large</name>
    <value>4</value>
  </property>

  <property>
    <name>zookeeper.session.timeout</name>
    <value>120000</value>
  </property>
</configuration>
```

### 4.2 hbase-env.sh 关键项

```bash
export JAVA_HOME=/usr/lib/jvm/java-17
export HBASE_HEAPSIZE=32G
export HBASE_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
  -XX:+ParallelRefProcEnabled -XX:+UnlockExperimentalVMOptions \
  -XX:G1NewSizePercent=5 -XX:G1MaxNewSizePercent=30 \
  -XX:InitiatingHeapOccupancyPercent=45"
export HBASE_MASTER_OPTS="-Xmx8G"
export HBASE_REGIONSERVER_OPTS="-Xmx32G -Xms32G"
export HBASE_MANAGES_ZK=false
```

G1 参数的意图：

```text
MaxGCPauseMillis=200：把单次停顿控制在 200ms 内
InitiatingHeapOccupancyPercent=45：提前启动并发标记，避免 Full GC
G1NewSizePercent/G1MaxNewSizePercent：限制新生代范围，减少波动
```

### 4.3 regionservers 文件

```text
rs1.example.com
rs2.example.com
rs3.example.com
...
```

### 4.4 系统参数

```bash
# /etc/security/limits.conf
hbase soft nofile 65536
hbase hard nofile 65536
hbase soft nproc 32768
hbase hard nproc 32768

# 关闭透明大页
echo never > /sys/kernel/mm/transparent_hugepage/enabled
echo never > /sys/kernel/mm/transparent_hugepage/defrag

# swappiness
vm.swappiness = 1
```

---

## 5. 部署步骤

以 3 节点最小生产集群为例（1 Master + 3 RS + 3 ZK，实际会拆分角色）：

```text
第 1 步：基础环境
  所有节点安装 JDK 17、配置 hosts、NTP、SSH 免密、ulimit

第 2 步：部署 ZooKeeper
  3 节点，myid 分别为 1/2/3，配置 zoo.cfg 的 server.1/2/3
  启动并验证：echo ruok | nc zk1 2181 → imok

第 3 步：部署 Hadoop
  NameNode HA + 多 DataNode
  验证：hdfs dfsadmin -report

第 4 步：部署 HBase
  解压 hbase-3.0.0-bin.tar.gz 到 /opt/hbase
  配置 hbase-site.xml、hbase-env.sh、regionservers
  分发到所有节点（路径一致）

第 5 步：启动
  先在 Master 执行 start-hbase.sh
  → 启动本机 HMaster
  → 按 regionservers 启动各 RS
  → 注册到 ZK

第 6 步：验证
  jps 检查：HMaster、HRegionServer、QuorumPeerMain
  Web UI：http://master:16010
  Shell：hbase shell → status → list
```

> **提示**：生产环境建议用 Ansible/SaltStack 做配置分发与启停，不要手工逐台操作。容器化部署时，HBase 的 StatefulSet 需要持久化 WAL 与数据目录，且 RS 与 DN 的本地盘要绑定。

---

## 6. 启动与验证

### 6.1 启动顺序

```text
ZooKeeper → HDFS（NameNode/DataNode）→ HBase Master → RegionServer

关闭顺序相反：
HBase → HDFS → ZooKeeper
```

### 6.2 验证命令

```text
# 集群状态
hbase shell
status
status 'detailed'

# 表与 Region
list
describe 'app:user_profile'
scan 'hbase:meta', {LIMIT => 5}

# 读写冒烟
create 'smoke', 'd'
put 'smoke', 'row1', 'd:value', 'hello'
get 'smoke', 'row1'
disable 'smoke'
drop 'smoke'
```

### 6.3 健康检查脚本（示例）

```bash
#!/usr/bin/env bash
set -euo pipefail

if ! echo "status" | hbase shell -n | grep -q "servers"; then
  echo "HBase shell check failed"
  exit 1
fi

if ! curl -sf http://master:16010/master-status > /dev/null; then
  echo "Master UI unreachable"
  exit 1
fi

echo "HBase health check passed"
```

---

## 7. 高可用设计

```text
① HMaster HA
   两个 Master 实例，ZK 选举，Active 宕机 Backup 秒级接管
   → 控制面高可用

② RegionServer 故障
   ZK 会话超时 → Master 重新分配 Region → WAL 回放
   → 数据面由 HDFS 副本 + WAL 保证

③ ZooKeeper 高可用
   3 或 5 节点，容忍 (n-1)/2 台故障

④ HDFS 高可用
   NameNode HA + DataNode 三副本
   → HBase 的数据底座不丢

⑤ 机架感知
   HDFS 副本跨机架，RegionServer 分配尽量分散

⑥ 跨集群复制
   replication 做异地容灾（异步，RPO 秒级）
```

单点风险清单：

| 组件 | 单点后果 | 冗余方式 |
|------|---------|---------|
| HMaster | 无法建表/迁移，读写不受影响 | 2 个 Master |
| ZooKeeper | 集群不可用（无法选主/注册） | 3 或 5 节点 |
| NameNode | HDFS 不可用，HBase 停摆 | NameNode HA |
| 单台 RS | 部分 Region 短暂不可用 | 多 RS + WAL 恢复 |
| 单块磁盘 | 该盘上的副本失效 | RAID / JBOD + 副本 |

---

## 8. 监控体系

### 8.1 必看指标

| 层 | 指标 | 阈值参考 |
|----|------|---------|
| RegionServer | `memStoreSize` | 接近 global memstore 上限需告警 |
| RegionServer | `storeFileCount`（单 Store） | > 10 关注，> 16 阻塞 |
| RegionServer | `compactionQueueLength` | 持续 > 10 需扩容或调参 |
| RegionServer | `blockCacheHitRatio` | < 80% 说明缓存不足 |
| RegionServer | `slowGetCount` / `slowPutCount` | 持续增长需排查 |
| RegionServer | `regionCount` | > 200 需拆分 RS |
| RegionServer | `hlogFileCount` | 接近 32 会阻塞写入 |
| Master | `ritCount` | 长时间 > 0 即故障 |
| Master | `deadRegionServers` | 非 0 立即处理 |
| JVM | GC 停顿、老年代占用 | 单次 > 500ms 需调优 |
| HDFS | 写入带宽、DataNode 磁盘 | 打满即瓶颈 |

### 8.2 监控栈

```text
采集：HBase Metrics2 → JMX / Prometheus exporter
存储：Prometheus
展示：Grafana
告警：Alertmanager → 钉钉/飞书/企业微信

关键面板：
  集群总览（RS 数、Region 数、QPS、延迟）
  单 RS 资源（CPU、内存、磁盘、网络）
  Region 热点（TopN Region 的请求量）
  Compaction 队列与耗时
  RIT 与故障事件
```

### 8.3 日志位置

```text
$HBASE_HOME/logs/hbase-<user>-master-<host>.log
$HBASE_HOME/logs/hbase-<user>-regionserver-<host>.log
$HBASE_HOME/logs/hbase-<user>-master-<host>.out
```

排查顺序：先看 Master 日志的 Region 分配与 RIT，再看 RS 日志的 GC、Compaction、异常。

---

## 9. 日常运维操作

### 9.1 滚动重启

```text
滚动重启用于升级配置或版本，避免整体停服：

① 逐个 RS 执行 graceful_stop.sh
   → 先把 Region 迁移走，再停进程
② 更新配置/版本
③ 启动 RS，等待 Region 迁回
④ 观察一段时间，再处理下一台
⑤ 最后切换 Master（先停 Backup，再切 Active）

工具：
  $HBASE_HOME/bin/graceful_stop.sh <hostname>
  $HBASE_HOME/bin/rolling-restart.sh
```

### 9.2 Region 迁移

```text
手动迁移（负载不均时）：
  move 'encodedRegionName', 'targetServerName'

取消迁移：
  unassign / assign

批量均衡：
  balancer
  balancer_switch（开关）
```

### 9.3 Region 合并

```text
小 Region 过多时合并：
  merge_region 'encodedRegion1', 'encodedRegion2'

注意：
  合并会触发数据重写，选低峰期
  合并后 Region 可能再次分裂
```

### 9.4 快照

```text
创建快照（秒级，不复制数据，只记录文件引用）：
  snapshot 'app:user_profile', 'snap_20261026'

恢复：
  disable 'app:user_profile'
  restore_snapshot 'snap_20261026'
  enable 'app:user_profile'

删除：
  delete_snapshot 'snap_20261026'

用途：
  升级前保护、误删恢复、数据回滚
```

> **警告**：快照依赖 HFile 不可变特性，恢复时如果原 HFile 已被 Compaction 删除会失败。生产上快照要配合定期校验，不能只建不验。

---

## 10. 备份与恢复

### 10.1 备份方式对比

| 方式 | 原理 | RPO | 恢复速度 | 适用 |
|------|------|-----|---------|------|
| Snapshot | 记录 HFile 引用 | 创建时刻 | 快 | 同集群回滚 |
| Export/Import | MapReduce 导出到 HDFS | 导出时刻 | 慢 | 跨集群迁移 |
| CopyTable | MR 复制到目标表 | 执行时刻 | 中 | 表级复制 |
| Replication | WAL 异步复制 | 秒级 | 中 | 异地容灾 |
| HBase Backup API | 增量备份 | 备份周期 | 中 | 生产标准备份 |

### 10.2 备份策略

```text
3-2-1 原则：
  3 份数据（生产 + 本地备份 + 异地备份）
  2 种介质（HDFS + 对象存储/磁带）
  1 份离线

推荐组合：
  每日增量备份（Backup API）
  每周全量快照
  跨集群 replication 做实时容灾
  每季度做恢复演练

备份前检查：
  HDFS 空间是否足够
  Region 是否处于 RIT
  是否有大 Compaction 在跑
```

### 10.3 恢复演练

```text
演练步骤：
  ① 在隔离环境恢复指定表
  ② 校验行数、抽样数据、关键字段
  ③ 记录恢复耗时（作为 RTO 基线）
  ④ 更新应急预案

常见失败原因：
  快照引用的 HFile 已被清理
  备份时表结构已变更
  目标集群版本不一致
```

---

## 11. 扩容与缩容

### 11.1 扩容

```text
加 RegionServer（水平扩容）：
  ① 新节点安装相同版本、相同配置
  ② 加入 regionservers 文件
  ③ 启动 HRegionServer
  ④ 执行 balancer 迁移 Region
  ⑤ 观察新节点负载

加磁盘（垂直扩容）：
  ① 挂载新盘，配置 DataNode 数据目录
  ② 重启 DataNode
  ③ 执行 hdfs balancer 均衡数据
```

### 11.2 缩容

```text
缩容前提：集群负载低、数据量可容纳

步骤：
  ① 关闭 balancer
  ② 将该 RS 上的 Region 手动迁移到其他节点
  ③ graceful_stop 停止 RS
  ④ 从 regionservers 移除
  ⑤ 重新开启 balancer
  ⑥ 观察 RIT 与负载
```

> **警告**：缩容时如果一次迁移太多 Region，会造成 WAL Split 与 Compaction 集中爆发。建议每批迁移不超过单 RS Region 数的 20%，并留出观察窗口。

---

## 12. 安全加固

```text
① 认证：Kerberos
   HBase 支持 Kerberos 认证，客户端需 keytab
   开启后所有 RPC 需认证

② 授权：ACL
   grant 'user', 'RW', 'app:user_profile'
   revoke 'user', 'app:user_profile'
   按 namespace/table/column family 授权

③ 传输加密：TLS
   hbase.ssl.enabled=true
   配置 keystore/truststore

④ 审计
   开启 audit log，记录 DDL 与敏感数据访问

⑤ 网络隔离
   RS/Master 端口不暴露公网
   Web UI 加认证或仅内网访问

⑥ 最小权限
   业务账号只授予所需表的读写
   运维账号与业务账号分离
```

---

## 13. 常见部署问题

| 现象 | 根因 | 处理 |
|------|------|------|
| Master 启动失败，ZK 连接超时 | ZK 未启动/防火墙/hosts 不一致 | 检查 ZK 与 2181 端口，核对主机名 |
| RS 启动后立即退出 | JDK 版本不匹配（3.0 需要 17） | 检查 java -version 与 JAVA_HOME |
| `hbase.rootdir` 报错 | HDFS 不可用或路径权限 | hdfs dfs -ls 验证，检查目录权限 |
| RegionServer 频繁掉线 | ZK 会话超时太短、GC 停顿长 | 调大 session timeout，优化 G1 |
| 写入报 `RegionTooBusyException` | MemStore 打满、文件数超限 | 查 flush/compaction 队列，扩容或调参 |
| RIT 长时间不结束 | Region 分配卡住、HDFS 异常 | 查 Master 日志，hbck2 检查 |
| `hbase:meta` 损坏 | 异常关机、HDFS 故障 | 用 hbck2 修复，严重时从备份恢复 |
| 读延迟高但 CPU 不高 | 磁盘 IO 瓶颈、缓存命中低 | 查 BlockCache 命中率与 HDFS 带宽 |
| 磁盘使用率持续上涨 | 多版本/墓碑/未清理快照 | 检查 VERSIONS/TTL，跑 major compaction，清理快照 |

---

## 14. 小结

| 问题 | 答案 |
|------|------|
| 生产部署形态 | 独立 ZK + HDFS HA + 2 Master + N RS（RS 与 DN 同机） |
| 3.0 硬依赖 | JDK 17、Hadoop 3、ZooKeeper 3.8+ |
| 启动顺序 | ZK → HDFS → Master → RS |
| 高可用四层 | Master HA、RS 故障转移、ZK 集群、HDFS 副本 |
| 必看指标 | MemStore、StoreFile 数、Compaction 队列、RIT、GC |
| 备份组合 | 快照 + 增量备份 + 跨集群复制 |
| 扩容方式 | 加 RS + balancer；加盘 + HDFS balancer |
| 缩容风险 | Region 集中迁移导致 WAL/Compaction 峰值 |
| 安全四件套 | Kerberos、ACL、TLS、审计 |

**下一模块**：[07-HBase性能调优与故障排查](07-HBase性能调优与故障排查.md)　**返回总览**：[00-HBase知识体系总览](00-HBase知识体系总览.md)
