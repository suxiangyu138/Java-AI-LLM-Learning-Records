# 03 - ZooKeeper 数据模型与 ACL 权限

> 🎯 ZNode 是 ZK 的一等公民 — 持久/临时/顺序节点四种类型 + Stat 元数据 + ACL 五种权限，构成了 ZK 协调能力的基础

---

## 目录

1. [ZNode 数据模型](#1-znode-数据模型)
2. [四种 ZNode 类型](#2-四种-znode-类型)
3. [Stat 结构体](#3-stat-结构体)
4. [ACL 权限控制](#4-acl-权限控制)
5. [命令行操作](#5-命令行操作)

---

## 1. ZNode 数据模型

```text
ZooKeeper 的命名空间 — 类 Unix 文件系统：

/
├── /services
│   ├── /services/user-service      (data: ip:port)
│   └── /services/order-service
├── /config
│   └── /config/db-url               (data: jdbc:mysql://...)
├── /locks
│   └── /locks/order-000000001       (临时顺序节点)
└── /election
    └── /election/000000002          (临时顺序节点)

每个 ZNode 特点：
  → 可存储数据（默认 ≤ 1MB）
  → 有 ACL 权限控制
  → 有版本号（乐观锁）
  → 大小写敏感
  → 路径无相对路径（绝对路径）
```

---

## 2. 四种 ZNode 类型

| 类型 | 创建命令 | 生命周期 | 典型用途 |
|------|----------|----------|----------|
| **持久 (PERSISTENT)** | `create /path data` | 显式删除才消失 | 配置数据 |
| **临时 (EPHEMERAL)** | `create -e /path data` | Session 断开自动删除 | 服务注册、分布式锁 |
| **持久顺序 (PERSISTENT_SEQUENTIAL)** | `create -s /path data` | 显式删除 + 序号后缀 | 分布式 ID |
| **临时顺序 (EPHEMERAL_SEQUENTIAL)** | `create -e -s /path data` | Session 断开 + 序号后缀 | ⭐ 分布式锁、Leader 选举 |

### 临时顺序节点的妙用

```text
创建 3 个临时顺序节点：
  /locks/order-0000000001  (Client A — 最小序号 → 获得锁)
  /locks/order-0000000002  (Client B — Watch 前驱 001)
  /locks/order-0000000003  (Client C — Watch 前驱 002)

Client A 释放 → 001 删除 → B 收到通知 → B 获得锁
  → 避免了"羊群效应"（所有客户端同时争抢）

Leader 选举同理 — 最小序号节点当选 Leader
```

---

## 3. Stat 结构体

```bash
# 查看 ZNode 状态
get -s /config/db-url
# jdbc:mysql://...
# cZxid = 0x100000001     # 创建事务 ID
# mZxid = 0x100000005     # 最后修改事务 ID
# ctime = ...             # 创建时间
# mtime = ...             # 最后修改时间
# dataVersion = 3          # 数据版本（乐观锁）
# cversion = 0             # 子节点版本
# aclVersion = 0           # ACL 版本
# dataLength = 52          # 数据长度
# numChildren = 0          # 子节点数量
```

| 字段 | 说明 | 用途 |
|------|------|------|
| `dataVersion` | 数据版本号 | ⭐ CAS 乐观锁（修改时带 version，版本不匹配拒绝写入） |
| `cversion` | 子节点列表版本 | 监测子节点变化 |
| `numChildren` | 子节点数量 | — |
| `zxid` | ZooKeeper Transaction ID | 全局唯一递增，判断事件顺序 |

---

## 4. ACL 权限控制

### 4.1 五种权限

| 权限 | 值 | 说明 |
|------|:---:|------|
| **CREATE** | 1 | 创建子节点 |
| **READ** | 2 | 获取节点数据和子节点列表 |
| **WRITE** | 4 | 设置节点数据 |
| **DELETE** | 8 | 删除子节点 |
| **ADMIN** | 16 | 设置 ACL |

### 4.2 四种认证模式

| 模式 | 说明 | 示例 |
|------|------|------|
| **world** | 所有用户 | `world:anyone:cdrwa` |
| **auth** | 已认证用户 | `auth:user:password:cdrwa` |
| **digest** | 用户名密码（SHA1） | `digest:user:pwd:cdrwa` |
| **ip** | IP 地址 | `ip:192.168.1.0/24:cdrwa` |

```bash
# 设置 ACL
setAcl /config world:anyone:r              # 任何人只读
setAcl /config digest:admin:xyz123:cdrwa   # admin 全权限
setAcl /config ip:192.168.1.0/24:cdrwa     # 内网全权限

# 查看 ACL
getAcl /config
```

> ⚠️ 生产环境建议开启 ACL 认证（`-Dzookeeper.skipACL=no`），防止误操作或恶意删除。

---

## 5. 命令行操作

```bash
# 连接
zkCli.sh -server 127.0.0.1:2181

# ═══ CRUD ═══
create /config "hello"            # 创建持久节点
create -e /session "tmp"          # 创建临时节点
create -s /seq ""                 # 创建顺序节点
get -s /config                    # 读取 + Stat
set /config "new value" 3         # 更新（带版本号 3）
delete /config                    # 删除（无子节点）
deleteall /config                 # 递归删除

# ═══ 监听 ═══
get -w /config                    # 监听数据变化（一次性）
ls -w /config                     # 监听子节点变化

# ═══ 配额 ═══
setquota -n 1000 /data            # 限制子节点数量 ≤ 1000
setquota -b 100M /data            # 限制数据总量 ≤ 100MB
listquota /data                   # 查看配额
```

> 🎯 **记住**：临时节点随 Session 自动消失（释放锁/注销服务）、顺序节点自动加序号、版本号实现乐观锁、Watch 一次性触发。这四点构成 ZK 分布式协调的全部基础。
