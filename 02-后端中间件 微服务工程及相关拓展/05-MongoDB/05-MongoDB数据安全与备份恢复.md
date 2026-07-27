# MongoDB 数据安全与备份恢复
> 数据是企业的生命线——从访问控制到传输加密，从定时备份到灾难恢复，完整的安全防护体系。

## 目录
1. [认证机制](#1-认证机制)
2. [授权与角色](#2-授权与角色)
3. [网络安全](#3-网络安全)
4. [加密与审计](#4-加密与审计)
5. [备份策略](#5-备份策略)
6. [数据恢复](#6-数据恢复)
7. [备份方案对比](#7-备份方案对比)
8. [安全 Checklist](#8-安全-checklist)

---

## 1. 认证机制

### 1.1 开启认证

```yaml
# mongod.conf
security:
  authorization: enabled
```

```bash
# 启动命令
mongod --auth --config /etc/mongod.conf
```

> ⚠️ 生产环境必须开启认证。未开启认证的 MongoDB 容易被攻击勒索。

### 1.2 三种认证方式

| 方式 | 说明 | 适用场景 |
|------|------|----------|
| **SCRAM**（默认） | 用户名 + 密码，MongoDB 原生实现 | 通用场景 |
| **x.509 证书** | TLS/SSL 证书认证，双向验证 | 高安全环境 |
| **LDAP / Kerberos** | 企业统一认证，集成 AD | 大型企业 |

### 1.3 SCRAM 认证详解

SCRAM（Salted Challenge Response Authentication Mechanism）是 MongoDB 默认认证机制：

```text
1. 客户端发送用户名给服务端
2. 服务端返回随机盐值 + 迭代次数
3. 客户端用密码+盐值计算哈希，发送给服务端
4. 服务端验证哈希 → 认证通过

特点：密码不传输明文，防重放攻击
```

### 1.4 用户管理

```javascript
// 创建管理员
use admin
db.createUser({
  user: "admin",
  pwd: passwordPrompt(),   // 交互式输入密码
  roles: ["root"]
})

// 创建业务用户（最小权限）
use mydb
db.createUser({
  user: "app_user",
  pwd: "app_pass",
  roles: [
    { role: "readWrite", db: "mydb" },
    { role: "read", db: "logs" }
  ]
})

// 修改密码
db.changeUserPassword("admin", "newPass")

// 查看所有用户
db.getUsers()

// 查看当前用户权限
db.runCommand({ usersInfo: "app_user", showPrivileges: true })

// 删除用户
db.dropUser("app_user")
```

### 1.5 密码策略建议

| 要求 | 建议 |
|------|------|
| 密码长度 | 至少 12 位 |
| 复杂度 | 大写 + 小写 + 数字 + 特殊字符 |
| 定期更换 | 每 90 天更换一次 |
| 密码存储 | 密码管理器，不要硬编码在配置文件 |

---

## 2. 授权与角色

### 2.1 内置角色

| 角色类型 | 角色名 | 权限 | 适用 |
|----------|--------|------|------|
| **数据库用户** | `read` | 只读 | 报表查询 |
| | `readWrite` | 读写 | 应用业务账号 |
| **数据库管理** | `dbAdmin` | 索引管理、统计 | DBA 日常维护 |
| | `dbOwner` | 数据库全部权限（含用户管理）| 数据库负责人 |
| **集群管理** | `clusterAdmin` | 集群全部管理权限 | 集群管理员 |
| | `clusterMonitor` | 集群只读监控 | 监控系统 |
| **备份恢复** | `backup` | 备份数据 | 备份脚本 |
| | `restore` | 恢复数据 | 恢复操作 |
| **超级权限** | `root` | 所有权限（含集群管理）| 超级管理员 |

### 2.2 自定义角色

```javascript
// 创建自定义角色：仅允许对 orders 集合进行插入和查询
use mydb
db.createRole({
  role: "orderOperator",
  privileges: [
    {
      resource: { db: "mydb", collection: "orders" },
      actions: ["insert", "find", "update"]
    }
  ],
  roles: []
})

// 创建用户并赋予自定义角色
db.createUser({
  user: "order_service",
  pwd: "order_pass",
  roles: ["orderOperator"]
})
```

### 2.3 最小权限原则

```javascript
// ✅ 正确：应用只给需要的最小权限
db.createUser({
  user: "order_service",
  pwd: "order_pass",
  roles: [
    { role: "readWrite", db: "order_db" },
    { role: "read", db: "config" }
  ]
})

// ❌ 错误：不要给应用 root 权限
db.createUser({
  user: "order_service",
  pwd: "order_pass",
  roles: ["root"]   // ❌ 权限过大！
})
```

> 💡 最小权限原则：每个用户只应拥有完成其任务所需的最小权限集合。应用账号绝不能用 root。

### 2.4 角色权限速查表

| Action | 说明 |
|--------|------|
| `find` | 查询文档 |
| `insert` | 插入文档 |
| `update` | 更新文档 |
| `remove` | 删除文档 |
| `createCollection` | 创建集合 |
| `createIndex` | 创建索引 |
| `dropCollection` | 删除集合 |
| `shutdown` | 关闭服务器（高危）|

---

## 3. 网络安全

### 3.1 网络层防护

```yaml
# mongod.conf
net:
  port: 27017
  bindIp: 127.0.0.1,192.168.1.100   # 只绑定内网 IP
  # 生产绝不要绑 0.0.0.0
```

```bash
# 防火墙限制访问来源
iptables -A INPUT -p tcp --dport 27017 -s 192.168.0.0/16 -j ACCEPT
iptables -A INPUT -p tcp --dport 27017 -j DROP
```

### 3.2 TLS/SSL 传输加密

```yaml
# mongod.conf
net:
  tls:
    mode: requireTLS
    certificateKeyFile: /etc/ssl/mongodb.pem
    CAFile: /etc/ssl/ca.pem
```

| TLS 模式 | 说明 |
|:--------:|------|
| `disabled` | 不启用 TLS（默认）|
| `allowTLS` | 允许 TLS，不强制 |
| `preferTLS` | 优先 TLS，非 TLS 也接受 |
| `requireTLS` | 强制 TLS，拒绝非 TLS 连接 |

```java
// Java 客户端使用 TLS
MongoClientSettings settings = MongoClientSettings.builder()
    .applyToSslSettings(builder -> builder.enabled(true))
    .build();
```

```bash
# 生成自签名证书（测试用）
openssl req -new -x509 -days 365 -out mongodb.crt -keyout mongodb.key
cat mongodb.key mongodb.crt > mongodb.pem
```

> ⚠️ 生产环境需要使用权威 CA 签发的证书，自签名证书只适合测试。

### 3.3 静态加密（Enterprise Only）

```yaml
# mongod.conf — 数据文件加密
security:
  enableEncryption: true
  encryptionKeyFile: /etc/mongodb/encryption.key
  # 可选：KMIP 服务器管理密钥
  # kmip:
  #   serverName: kmip.example.com
  #   port: 5696
```

静态加密使用 AES-256-CBC 对数据文件进行透明加密，即使物理磁盘被盗也无法读取数据。

### 3.4 安全加固清单

| 措施 | 配置 | 优先级 |
|------|------|:------:|
| 禁用 HTTP 接口 | `net.http.enabled: false` | P0 |
| 禁用 REST 接口 | `net.rest.enabled: false` | P0 |
| 禁用 数据库脚本执行 | `security.javascriptEnabled: false` | P1 |
| 设置最大连接数 | `net.maxIncomingConnections: 1000` | P1 |
| 开启访问日志 | `auditLog.destination: file` | P1 |

---

## 4. 加密与审计

### 4.1 审计日志（Enterprise Only）

```yaml
# mongod.conf
auditLog:
  destination: file
  format: JSON
  path: /var/log/mongodb/audit.log
  filter: '{ atype: { $in: ["authenticate", "createUser", "dropCollection", "dropDatabase"] } }'
```

```javascript
// 查看审计过滤器
db.getSiblingDB("admin").runCommand({ getParameter: 1, auditAuthorizationSuccess: 1 })
```

| 审计事件 | 说明 |
|----------|------|
| `authenticate` | 认证成功/失败 |
| `createUser` | 创建用户 |
| `dropCollection` | 删除集合 |
| `dropDatabase` | 删除数据库 |
| `createIndex` | 创建索引 |

### 4.2 数据脱敏

MongoDB Enterprise 支持字段级加密（Field Level Encryption）：

```javascript
// 客户端字段级加密
const client = new MongoClient(uri, {
  autoEncryption: {
    keyVaultNamespace: "encryption.__keyVault",
    kmsProviders: {
      local: { key: localKey }
    }
  }
})
```

数据库本身无法读取加密字段，仅在客户端解密，实现真正的"零信任"安全。

---

## 5. 备份策略

### 5.1 mongodump（推荐）

```bash
# 全量备份
mongodump --host localhost:27017 -u admin -p password \
  --out /backup/mongo/$(date +%Y%m%d)

# 单库备份
mongodump --db mydb --out /backup/mongo/mydb_$(date +%Y%m%d)

# 单集合备份
mongodump --db mydb --collection users --out /backup/mongo/users

# 压缩备份（推荐，节省空间）
mongodump --archive=/backup/mongo/backup_$(date +%Y%m%d).gz --gzip

# 带 oplog 备份（支持时间点恢复，推荐）
mongodump --oplog --out /backup/mongo/backup_with_oplog

# 指定认证
mongodump --host localhost --port 27017 \
  -u backup_user --authenticationDatabase admin \
  --oplog --gzip --archive=/backup/mongo/full_$(date +%Y%m%d).gz
```

### 5.2 定时备份（crontab）

```bash
# 每日凌晨 2 点全量备份，保留 7 天
0 2 * * * mongodump --out /backup/mongo/$(date +\%Y\%m\%d) \
  && find /backup/mongo/ -type d -mtime +7 -exec rm -rf {} \;

# 使用压缩存储，保留 30 天
0 2 * * * mongodump --archive=/backup/mongo/full_$(date +\%Y\%m\%d).gz --gzip \
  && find /backup/mongo/ -name "*.gz" -mtime +30 -delete

# 每小时备份 oplog（增量备份）
0 * * * * mongodump --db local --collection oplog.rs \
  --out /backup/mongo/oplog/$(date +\%Y\%m\%d_\%H) \
  -u admin -p "password" --authenticationDatabase admin
```

### 5.3 文件系统快照

```bash
# LVM 快照（生产推荐，速度快）
# 1. 创建快照
lvcreate -L 10G -s -n mongo_snap /dev/vg/mongo_data

# 2. 挂载快照
mount /dev/vg/mongo_snap /mnt/backup

# 3. 复制数据
cp -r /mnt/backup/* /backup/mongo/

# 4. 清理
umount /mnt/backup
lvremove /dev/vg/mongo_snap

# 需要开启 journal 以保证一致性
```

> 💡 LVM 快照速度极快（秒级创建），适合大数据量的日常备份。缺点是占用相同磁盘空间。

### 5.4 Atlas 云备份

MongoDB Atlas 提供自动备份（云托管）：

```text
- 快照备份：每 6 小时一次（可配置），保留 1-35 天
- 连续备份：实时备份 oplog，支持时间点恢复到任意秒
- 恢复：一键恢复到新集群，或导出到本地
```

### 5.5 备份原则

```text
① 全量 + 增量：每周全量，每日增量（oplog）
② 异地备份：备份文件存不同机房或云存储（OSS、S3）
③ 定期演练：每月至少一次恢复演练，验证备份可用
④ 自动化：crontab/定时任务，不要手工操作
⑤ 监控告警：备份失败要能及时通知
```

---

## 6. 数据恢复

### 6.1 mongorestore

```bash
# 全量恢复
mongorestore --drop /backup/mongo/20260101

# 单库恢复
mongorestore --db mydb --drop /backup/mongo/20260101/mydb

# 单集合恢复
mongorestore --db mydb --collection users \
  --drop /backup/mongo/20260101/mydb/users.bson

# 从压缩文件恢复
mongorestore --archive=/backup/mongo/backup_20260101.gz --gzip

# 恢复时指定认证
mongorestore -u admin -p "password" --authenticationDatabase admin \
  --gzip --archive=/backup/mongo/full_20260101.gz
```

### 6.2 时间点恢复（Point-in-Time）

```bash
# 方案一：带 oplog 的备份恢复
# 1. 先恢复全量备份
mongorestore --oplogReplay --drop /backup/mongo/full_backup

# 2. 重放 oplog 到指定时间点
mongorestore --oplogFile /backup/mongo/oplog.bson \
  --oplogLimit "2026-01-01T14:00:00" \
  --drop /backup/mongo/full_backup

# 方案二：使用 oplog 增量恢复（更精细）
# 1. 恢复全量备份
mongorestore --drop /backup/mongo/full_20260101

# 2. 重放 oplog 到目标时间
mongorestore --db local /backup/mongo/oplog/20260101_02/local/oplog.rs.bson
# 然后用 mongo shell 重放指定时间范围的 oplog
```

### 6.3 误操作恢复场景

```text
场景：管理员在 14:23 误执行 db.users.drop()
```

```bash
# 恢复步骤
# 1. 找到删除前的最近全量备份
# 2. 恢复到临时实例
mongorestore --port 27018 --drop /backup/mongo/full_20260101

# 3. 从 oplog 重放 14:00-14:22 的操作（跳过 drop 操作）
# 4. 导出 users 集合
mongodump --port 27018 --db mydb --collection users

# 5. 恢复到生产环境
mongorestore --db mydb --collection users --drop /backup/mongo/users.bson
```

> ⚠️ 恢复演练务必在测试环境执行，确认备份文件可用。

### 6.4 恢复验证

```bash
# 恢复后验证数据完整性
# 1. 文档数量对比
mongo --eval "db.users.count()"

# 2. 最新数据时间核对
mongo --eval "db.users.find().sort({createTime:-1}).limit(1)"

# 3. 索引检查
mongo --eval "db.users.getIndexes()"
```

---

## 7. 备份方案对比

### 7.1 备份方式对比

| 方案 | 速度 | 存储空间 | 时间点恢复 | 适用场景 |
|------|:----:|:--------:|:---------:|----------|
| **mongodump** | 中等 | 小（BSON 压缩） | ✅（带 oplog）| 日常备份（推荐）|
| **文件系统快照** | 快 | 大（原始数据） | ✅（配合 oplog）| 大数据量（TB 级）|
| **MongoDB Atlas** | 自动化 | 云存储 | ✅（连续备份）| 云托管环境 |
| **mongodump + oplog** | 较慢 | 中等 | ✅（精确到秒）| 生产标准方案 |

### 7.2 备份频率建议

| 数据重要程度 | 全量备份 | 增量备份 | 保留时间 |
|:-----------:|:--------:|:--------:|:--------:|
| 核心业务数据 | 每天 | 每小时 | 30-90 天 |
| 一般业务数据 | 每周 | 每天 | 7-30 天 |
| 日志型数据 | 每月 | 每周 | 7 天 |

### 7.3 备份存储建议

```text
┌──────────────────────────────────┐
│ 本地磁盘（当天备份，快速恢复）      │
│  ↓                               │
│ NAS / SAN（近线存储，保留 7 天）   │
│  ↓                               │
│ 云存储 OSS/S3（异地容灾，长期归档） │
└──────────────────────────────────┘
```

### 7.4 备份成本估算

| 数据量 | mongodump 耗时 | 压缩大小 | 云存储成本（月）|
|:------:|:-------------:|:--------:|:--------------:|
| 10 GB | ~3 分钟 | ~2 GB | ~$0.05 |
| 100 GB | ~30 分钟 | ~20 GB | ~$0.50 |
| 1 TB | ~5 小时 | ~200 GB | ~$5.00 |

---

## 8. 安全 Checklist

### 8.1 必须项（P0）

| 检查项 | 配置 | 说明 |
|--------|------|------|
| 开启认证 | `security.authorization: enabled` | 未认证等于裸奔 |
| 强密码策略 | 12 位 + 混合字符 | 防暴力破解 |
| 内网绑定 | `bindIp: 内网IP` | 不绑 0.0.0.0 |
| 最小权限 | 应用用 readWrite，不用 root | 防误操作和越权 |
| 防火墙 | 限制 27017 端口访问来源 | 网络层隔离 |
| 开启 Journal | `storage.journal.enabled: true` | 崩溃恢复保障 |
| 定时备份 | 每日自动备份 | 数据不丢底线 |

### 8.2 建议项（P1）

| 检查项 | 配置 | 说明 |
|--------|------|------|
| TLS 传输加密 | `net.tls.mode: requireTLS` | 防中间人攻击 |
| 备份异地存储 | 备份文件存到远程 | 防本地灾难 |
| 禁用 HTTP 接口 | `net.http.enabled: false` | 减少攻击面 |
| 版本升级 | 使用最新稳定版 | 修复已知漏洞 |
| 监控告警 | MongoDB 指标监控 | 及时发现异常 |

### 8.3 最佳实践项（P2）

| 检查项 | 说明 |
|--------|------|
| 定期恢复演练 | 每月至少一次测试备份恢复流程 |
| 审计日志 | 开启审计，记录关键操作 |
| 定期用户审查 | 每季度审查数据库用户，清理僵尸账号 |
| 配置漂移检测 | 确保生产配置环境一致 |

### 8.4 安全事件响应流程

```text
1. 发现异常 → 立即切断受影响实例的网络
2. 保留现场 → 保存日志、数据文件副本
3. 从备份恢复 → 在干净环境恢复数据
4. 安全加固 → 修复漏洞，更新密码
5. 复盘总结 → 分析原因，改进防护措施
```

---

## 面试核心

**Q: MongoDB 认证有哪些方式？** SCRAM（默认）、x.509 证书、LDAP/Kerberos。

**Q: 最小权限原则？** 每个用户只给完成其任务所需的最小权限，应用账号绝不能用 root。

**Q: mongodump 和文件快照的区别？** mongodump 逻辑备份（跨版本/小数据量），快照物理备份（大数据量/速度快）。

**Q: 如何实现时间点恢复？** 全量备份 + oplog 重放到指定时间点。

**Q: 备份文件应该怎么存储？** 本地（快速恢复）+ 异地（灾备）双份存储。

**Q: 安全中最重要的是什么？** 认证开启 + 内网绑定 + 最小权限 + 定时备份。

**Q: 生产环境需要哪些安全措施？** 至少开启认证、内网绑定、防火墙、定时备份四项。建议加上 TLS 传输加密。

**Q: 审计日志记录什么？** 认证事件、用户管理、数据定义操作（drop/create）等关键操作。

> 🎯 **极简总结**：认证防入侵，备份防丢失，演练保可用——安全无小事。
