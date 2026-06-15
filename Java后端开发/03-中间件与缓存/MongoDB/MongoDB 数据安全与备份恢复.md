# MongoDB 数据安全与备份恢复（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | MongoDB 安全与备份方案
> **版本**：MongoDB 6.x/7.x
> **核心场景**：生产数据保护、灾备方案、数据恢复

---

## 一、数据安全机制

### 1.1 访问控制

```javascript
// 启动时加 --auth 参数
mongod --auth --config /etc/mongod.conf

// 配置文件
security:
  authorization: enabled
```

### 1.2 内置角色

| 角色类型 | 角色名 | 权限范围 |
|---|---|---|
| **数据库用户** | `read` | 只读 |
| | `readWrite` | 读写 |
| **数据库管理** | `dbAdmin` | 索引管理、统计 |
| | `userAdmin` | 用户管理 |
| **集群管理** | `clusterAdmin` | 集群配置 |
| | `clusterManager` | 监控、分片 |
| **备份恢复** | `backup` / `restore` | 备份恢复 |
| **超级权限** | `root` | 所有权限 |

### 1.3 创建用户

```javascript
use admin

// 管理员
db.createUser({
  user: "admin",
  pwd: passwordPrompt(),  // 交互式输入密码（安全）
  roles: ["root"]
})

// 业务用户（只读写自己的库）
use mydb
db.createUser({
  user: "app",
  pwd: "app_password",
  roles: [{ role: "readWrite", db: "mydb" }]
})

// 报表用户（只读）
db.createUser({
  user: "report",
  pwd: "report_password",
  roles: [{ role: "read", db: "mydb" }]
})

// 查看用户
db.getUsers()

// 修改密码
db.changeUserPassword("app", "new_password")

// 删除用户
db.dropUser("app")
```

---

## 二、传输与存储加密

### 2.1 TLS/SSL 传输加密

```yaml
# mongod.conf
net:
  tls:
    mode: requireTLS
    certificateKeyFile: /etc/ssl/mongodb.pem
    CAFile: /etc/ssl/ca.pem
```

### 2.2 存储加密（WiredTiger）

```yaml
# 企业版功能 — 开启加密存储
security:
  enableEncryption: true
  encryptionKeyFile: /etc/mongodb/keyfile
```

---

## 三、备份恢复方案

### 3.1 mongodump / mongorestore（官方工具）

```bash
# 全量备份
mongodump \
  --host localhost:27017 \
  --username admin --password admin123 \
  --authenticationDatabase admin \
  --out /backup/mongodb_$(date +%Y%m%d)

# 备份指定库
mongodump --db mydb --out /backup/mydb

# 备份指定集合
mongodump --db mydb --collection users --out /backup/mydb_users

# 压缩备份
mongodump --gzip --archive=/backup/mydb.archive

# 恢复
mongorestore --db mydb /backup/mydb

# 恢复指定集合
mongorestore --db mydb --collection users /backup/mydb_users/mydb/users.bson

# 只恢复匹配条件的文档
mongorestore --db mydb --collection users --filter='{"age": {"$gte": 18}}' /backup/mydb
```

### 3.2 副本集备份最佳实践

```bash
# 从 Secondary 备份（不影响主库性能）
mongodump \
  --host secondary_host:27017 \
  --readPreference secondary \
  --out /backup/$(date +%Y%m%d)
```

### 3.3 文件系统快照备份

```bash
# 1. 锁住写入（或从 Secondary 备份，不需锁）
# 2. 文件系统快照
# 3. 解锁
```

**依赖文件系统**：LVM、AWS EBS、Azure Disk 等支持快照的存储。

### 3.4 时间点恢复（PITR）

```
前提：有全量备份 + oplog 连续记录

流程：
1. mongorestore 恢复全量备份到指定时间点之前
2. 通过 oplog 重放到指定时间点
```

---

## 四、定期备份策略

| 策略 | 频率 | 保留时间 | 适用 |
|---|---|---|---|
| 每日全量 | 每天 | 7-30 天 | 大部分业务 |
| 每小时增量 | 每小时 | 24-48h | 核心业务 |
| 持续 PITR | 实时 oplog | 7 天 | 金融级别 |

```bash
# crontab 示例：每天凌晨 3 点备份
0 3 * * * mongodump --out /backup/$(date +\%Y\%m\%d) >> /var/log/mongo_backup.log 2>&1

# 删除 30 天前的备份
0 4 * * * find /backup/ -type d -mtime +30 -exec rm -rf {} \;
```

---

## 五、恢复测试 Checklist

| 检查项 | 内容 |
|---|---|
| **数据完整性** | 对比原始库和恢复库的 count、校验和 |
| **索引** | 确认恢复后索引存在 |
| **用户/角色** | 确认权限正常恢复 |
| **应用连接** | 应用切到恢复库，功能回归测试 |

---

## 六、副本集灾难恢复

```
场景 1：主节点宕机
→ Secondary 自动选举新 Primary（秒级）
→ 旧 Primary 恢复后变为 Secondary 重新加入

场景 2：全部节点数据损坏
→ 停止所有节点
→ 选择数据最新的节点作为 Primary 启动
→ mongorestore 恢复最新备份
→ 其他节点重新同步

场景 3：误删除集合
→ 从备份恢复该集合 → 或 PITR 时间点恢复
```

---

## 七、安全最佳实践

```yaml
# 生产安全清单
1. 开启认证（security.authorization: enabled）
2. 使用内部认证（keyfile 或 x509）
3. 网络隔离（bindIp 内网地址，防火墙白名单）
4. 最小权限原则（不用 root 角色跑业务）
5. 定期轮换密码
6. TLS 加密传输（公网场景）
7. 审计日志（auditLog.destination: file）
8. 关闭 HTTP 状态接口（net.http.enabled: false，7.x 默认关闭）
```

---

## 八、面试核心要点

1. **怎么备份 MongoDB？** mongodump 全量/增量，建议从 Secondary 备份
2. **Write Concern 与 Journal 关系？** journal 保证单机 crash 恢复，write concern "majority" 保证集群不丢
3. **误删除后如何恢复？** mongorestore 全量备份 + oplog 回放到误删前一刻
4. **副本集灾备怎么做？** 多机房部署 Secondary，延迟同步防误删
5. **如何防止删库跑路？** 权限最小化 + 定期备份 + 备份异地存储

---

## 九、极简总结

```
mongodump = 备份（全量/库/集合）
mongorestore = 恢复
auth = security.authorization: enabled（生产必开）
备份 = 从 Secondary 备份 + 每天全量 + 保留 30 天
PITR = 全量备份 + oplog → 可恢复到任意时间点
权限 = 业务用 readWrite，报表用 read，管理员用 root
```
