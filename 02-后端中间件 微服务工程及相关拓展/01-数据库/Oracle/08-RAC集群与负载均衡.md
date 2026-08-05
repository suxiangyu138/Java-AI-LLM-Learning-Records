# 08 - RAC 集群与负载均衡

> 🎯 RAC (Real Application Clusters) 是 Oracle 的横向扩展方案——多个节点共享同一份数据，任一节点故障自动切换到其他节点，对应用透明

---

## 目录

1. [RAC 架构](#1-rac-架构)
2. [负载均衡](#2-负载均衡)
3. [RAC 运维诊断](#3-rac-运维诊断)

---

## 1. RAC 架构

```text
RAC = 多个实例共享同一个数据库

     ┌─────────┐    ┌─────────┐    ┌─────────┐
     │ Node 1  │    │ Node 2  │    │ Node 3  │
     │ Instance│    │ Instance│    │ Instance│
     └────┬────┘    └────┬────┘    └────┬────┘
          │              │              │
          └──────────────┼──────────────┘
                         │
              ┌──────────▼──────────┐
              │   Shared Storage    │  ← SAN / ASM
              │  (同一份数据文件)     │
              └─────────────────────┘

关键组件：
  → SCAN (Single Client Access Name) — 统一入口
  → GES (Global Enqueue Service) — 全局锁管理
  → GRD (Global Resource Directory) — 全局资源目录
  → ASM (Automatic Storage Management) — 自动存储管理
  → 心跳网络 (Interconnect) — 节点间通信
```

## 2. 负载均衡

```text
RAC 两级负载均衡：

客户端侧 (Client-side LB)：
  JDBC URL: jdbc:oracle:thin:@(DESCRIPTION=
    (LOAD_BALANCE=on)
    (ADDRESS=(PROTOCOL=TCP)(HOST=node1)(PORT=1521))
    (ADDRESS=(PROTOCOL=TCP)(HOST=node2)(PORT=1521))
    (CONNECT_DATA=(SERVICE_NAME=ORCL)))

服务端侧 (Server-side LB)：
  → Listener 根据节点负载分配连接
  → LBA (Load Balancing Advisor) 动态调整
```

```sql
-- 查看 RAC 节点状态
SELECT inst_id, instance_name, host_name, status FROM gv$instance;

-- 查看服务分布
SELECT name, inst_id FROM gv$active_services;

-- 查看节点间通信延迟
SELECT * FROM v$im_header;  -- In-Memory 集群信息
```

## 3. RAC 运维诊断

```sql
-- 全局等待事件（多节点视角）
SELECT inst_id, event, total_waits, time_waited
FROM gv$system_event WHERE wait_class != 'Idle'
ORDER BY time_waited DESC;

-- 全局锁等待（关键！RAC 最大的性能瓶颈）
SELECT inst_id, sid, event, seconds_in_wait
FROM gv$session WHERE event LIKE '%gc%';  -- gc = Global Cache

-- 查看心跳网络状态
SELECT * FROM gv$cluster_interconnects;

-- CRS 集群状态（命令行）
-- crsctl stat res -t      # 查看所有资源状态
-- crsctl check cluster    # 检查集群健康
-- srvctl status database -d ORCL  # 数据库服务状态
```

### RAC 独有的性能问题

```text
1. GC Buffer Busy（全局缓存争用）
    → 多节点频繁访问同一数据块
    → 解决：应用分区（不同节点访问不同数据）

2. Interconnect 延迟
    → 心跳网络带宽不足或配置错误
    → 解决：专用心跳网络 + 大带宽

3. Sequence 争用
    → 自增序列在高并发下是全局热点
    → 解决：ALTER SEQUENCE seq CACHE 1000 ORDER;
```

## 核心要点回顾

- RAC = 多个 Instance + 共享存储（任意节点故障透明切换）
- SCAN = 统一入口（客户端不需要知道有几个节点）
- 全局缓存 `gc*` 等待 = RAC 最大的性能瓶颈
- 心跳网络 = RAC 的"神经"（带宽不够直接废）
- `gv$` 视图 = 全局视图（`v$` 只能看当前节点）

## 参考资料

1. Oracle RAC Administration Guide
