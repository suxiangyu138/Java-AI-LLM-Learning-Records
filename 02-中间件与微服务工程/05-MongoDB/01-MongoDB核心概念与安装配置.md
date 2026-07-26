# MongoDB 核心概念与安装配置
> 开源文档型 NoSQL 数据库，BSON 格式存储，无固定 Schema，高灵活、高扩展——文档模型的分布式数据库。

## 目录
1. [MongoDB 是什么](#1-mongodb-是什么)
2. [MongoDB vs MySQL 核心差异](#2-mongodb-vs-mysql-核心差异)
3. [核心层级与 BSON](#3-核心层级与-bson)
4. [WiredTiger 存储引擎](#4-wiredtiger-存储引擎)
5. [Docker 部署](#5-docker-部署)
6. [Linux 部署](#6-linux-部署)
7. [核心配置详解](#7-核心配置详解)
8. [用户与权限管理](#8-用户与权限管理)
9. [生产部署 Checklist](#9-生产部署-checklist)

---

## 1. MongoDB 是什么

| 属性 | 说明 |
|------|------|
| 类型 | 文档型 NoSQL 数据库 |
| 数据格式 | BSON（Binary JSON） |
| 开发语言 | C++ |
| 默认端口 | 27017 |
| 存储引擎 | WiredTiger（3.2+ 默认） |
| 许可证 | SSPL |
| 首次发布 | 2009 年 |
| 维护者 | MongoDB Inc. |

### 1.1 发展简史

| 时间 | 里程碑 |
|------|--------|
| 2007 | 10gen 公司成立，开始研发 MongoDB |
| 2009 | MongoDB 1.0 发布，开源 |
| 2012 | MongoDB 2.2 引入聚合框架 |
| 2013 | 10gen 更名为 MongoDB Inc. |
| 2015 | MongoDB 3.0 引入 WiredTiger 引擎 |
| 2017 | MongoDB 3.6 引入 Change Streams |
| 2018 | 上市（NASDAQ: MDB），4.0 支持多文档事务 |
| 2019 | MongoDB 4.2 支持分布式事务 |
| 2023 | MongoDB 7.0 发布，性能大幅提升 |

### 1.2 五大核心价值

| 价值 | 说明 |
|------|------|
| **非结构化数据** | 天然支持 JSON/嵌套/数组等灵活结构 |
| **敏捷开发** | 无需 DDL，字段随时添加修改，Schema 动态 |
| **分布式扩展** | 原生分片集群，GB 到 TB 线性扩展 |
| **高性能** | 文档级锁 + WAL 日志预写 + 内存映射 |
| **强大查询** | 聚合管道 + 地理空间 + 全文检索 |

### 1.3 适合场景与不适合场景

**适合场景：**

| 场景 | 说明 |
|------|------|
| 用户画像 | 字段多变，每个用户标签不同，Schema 灵活 |
| 内容管理 | 文章/商品属性各异，无固定结构 |
| IoT 时序数据 | 海量写入 + 横向扩展，天然 JSON |
| LBS 应用 | 地理空间索引，附近查询 |
| 日志存储 | JSON 格式 + 高吞吐 + TTL 自动过期 |
| 实时分析 | 聚合管道支持实时数据处理 |
| 缓存层 | 比 Redis 更丰富的查询能力 |

**不适合场景：**

| 场景 | 原因 |
|------|------|
| 金融交易 | 弱事务（虽已支持，但不如传统数据库成熟） |
| 复杂 JOIN | MongoDB 反范式设计，关联查询性能差 |
| 简单 CRUD | MySQL 更成熟，运维成本更低 |
| 强 Schema 约束 | 关系型数据库更适合 |
| 复杂报表 | 数据仓库 + SQL 更适合 |

---

## 2. MongoDB vs MySQL 核心差异

### 2.1 全面对比

| 维度 | MongoDB | MySQL |
|------|---------|-------|
| 数据模型 | 文档模型（BSON），无固定 Schema | 关系模型（表-行-列），固定 Schema |
| 层级 | Database > Collection > Document | Database > Table > Row |
| Schema | 动态，字段随时增减 | 固定，需 ALTER TABLE |
| 关联 | 嵌套文档 / `$lookup` | JOIN / 外键 |
| 事务 | 支持多文档/跨分片，性能略弱 | ACID 强事务，成熟稳定 |
| 扩展 | 原生分片，横向扩展 | 分库分表，复杂度高 |
| 查询语言 | JSON-like 查询语法 | SQL |
| 索引 | B-Tree + 地理 + 文本 + TTL + 数组 | B+Tree |
| 存储引擎 | WiredTiger（插件化） | InnoDB / MyISAM 等 |
| 主键 | ObjectId（12 字节自动生成） | AUTO_INCREMENT / UUID |
| 开发效率 | 高（无 DDL，快速迭代） | 中（需设计 Schema） |

### 2.2 术语对照

| MongoDB | MySQL | 说明 |
|---------|-------|------|
| Database | Database | 数据库 |
| Collection | Table | 集合/表 |
| Document | Row | 文档/行 |
| Field | Column | 字段/列 |
| Index | Index | 索引 |
| `_id` | Primary Key | 主键 |
| Aggregation Pipeline | GROUP BY / JOIN | 聚合 |
| `$lookup` | LEFT JOIN | 关联查询 |

### 2.3 选型建议

```text
┌─────────────────────────────────────┐
│           数据模型决策               │
│                                     │
│   关系明确，强一致性需求              │
│   ────────────────→  MySQL           │
│                                     │
│   文档嵌套，Schema 多变              │
│   ────────────────→  MongoDB         │
│                                     │
│   海量数据，需要水平扩展              │
│   ────────────────→  MongoDB         │
│                                     │
│   复杂报表，统计分析                  │
│   ────────────────→  ClickHouse/ES   │
└─────────────────────────────────────┘
```

---

## 3. 核心层级与 BSON

### 3.1 层级结构

```
MongoDB Instance（端口 27017）
├── admin（系统库：认证与权限）
│   ├── system.users       （用户信息）
│   ├── system.roles       （角色定义）
│   └── system.version     （Schema 版本）
├── local（系统库：本地节点数据）
│   ├── startup_log        （启动日志）
│   ├── replset.oplogTruncatedAfterPoint
│   └── replset.minvalid   （副本集元数据）
├── config（系统库：分片配置）
│   ├── databases          （分片数据库）
│   ├── shards             （分片节点）
│   └── chunks             （数据块分布）
├── db_user（业务库）
│   ├── user_profile（集合）
│   │   ├── {_id: 1, name: "张三"}（文档）
│   │   └── {_id: 2, name: "李四"}（文档）
│   └── user_log（集合）
└── db_order（业务库）
    ├── orders（集合）
    └── products（集合）
```

### 3.2 三层模型详解

| 层级 | 说明 | 类比 MySQL |
|------|------|-----------|
| Instance | 单个 MongoDB 进程，默认端口 27017 | MySQL Server |
| Database | 逻辑隔离，有独立权限，独立文件 | Database |
| Collection | 文档的集合，无 Schema 约束 | Table |
| Document | BSON 格式键值对，有唯一 `_id` | Row |

### 3.3 BSON 数据格式

BSON = Binary JSON，相比 JSON 扩展了更多数据类型：

```json
{
  "_id": ObjectId("60d21b4667d0d8992e610c85"),
  "username": "zhangsan",
  "age": 25,
  "height": 175.5,
  "tags": ["Java", "Spring", "MongoDB"],
  "address": {
    "city": "Beijing",
    "street": "长安街 100 号",
    "coordinates": [-73.97, 40.77]
  },
  "create_time": ISODate("2024-01-15T08:00:00Z"),
  "is_active": true,
  "score": 95.5,
  "metadata": null,
  "profile_pic": BinData(0, "..."),
  "large_number": NumberLong("9007199254740993")
}
```

**BSON 特性和 JSON 对比：**

| 特性 | JSON | BSON |
|------|------|------|
| 编码 | UTF-8 文本 | 二进制 |
| 数据类型 | String, Number, Boolean, Null, Array, Object | + Date, ObjectId, BinData, Int32, Int64, Decimal128 |
| 解析速度 | 较慢（文本解析） | 快（二进制直接读取） |
| 空间效率 | 一般 | 较高（压缩存储） |
| 遍历效率 | 需解析全文 | 快（长度前缀） |

### 3.4 ObjectId 主键

MongoDB 默认主键 `_id`，12 字节，分布式唯一：

```
ObjectId：507f1f77bcf86cd799439011
        ├─ 4 字节 Timestamp（秒级时间戳）
        ├─ 5 字节 Random（机器标识 + 进程 ID）
        └─ 3 字节 Counter（随机自增计数）
```

**ObjectId 特性：**

| 特性 | 说明 |
|------|------|
| 全局唯一 | 分布式环境下无需中心化 ID 生成器 |
| 包含时间 | 前 4 字节是时间戳，可提取创建时间 |
| 有序递增 | 按时间排序 ≈ 按创建时间排序 |
| 生成高效 | 客户端生成，无需数据库交互 |

```javascript
// 从 ObjectId 提取时间戳
ObjectId("60d21b4667d0d8992e610c85").getTimestamp()
// ISODate("2021-06-23T07:51:34Z")

// 自定义 _id（字符串、数字均可）
db.users.insertOne({ _id: "user_001", name: "张三" })
```

---

## 4. WiredTiger 存储引擎

MongoDB 3.2+ 默认引擎（已取代 MMAPv1）。

### 4.1 核心特性

| 特性 | 说明 |
|------|------|
| 压缩存储 | snappy/zlib 压缩，节省 50%-80% 空间 |
| 文档级并发 | 文档级锁，高并发写入 |
| 多文档事务 | 支持 ACID 事务（4.0+） |
| 内存管理 | 默认使用 50% 物理内存作为 Cache |
| 预写日志 | Journal（WAL）保障 crash 后数据恢复 |
| 快照隔离 | MVCC 实现一致性读 |

### 4.2 写入流程

```
写请求
  └→ 内存 Cache（WiredTiger Cache，默认物理内存 50%）
      ├→ Checkpoint（每 60s 或 2GB 数据）→ 磁盘 B-Tree（持久化数据）
      └→ Journal（WAL 预写日志，每 100ms 或 100MB 刷盘）→ Journal 文件
```

**写入保证：**

| 写入级别 | 说明 | 性能 |
|----------|------|------|
| w: 1 | 主节点写入即返回 | 最快 |
| w: majority | 大多数节点写入才返回 | 中 |
| j: true | Journal 刷盘后才返回 | 数据最安全 |

### 4.3 与 MMAPv1 对比

| 特性 | WiredTiger | MMAPv1 |
|------|-----------|--------|
| 默认版本 | 3.2+ | 3.0 及之前 |
| 锁粒度 | 文档级 | 集合级 |
| 压缩 | snappy/zlib | 无 |
| 事务 | 支持 | 不支持 |
| 内存管理 | Cache 大小可控 | OS 管理 |
| 文档大小修改 | 原地更新 | 需移动文档 |

### 4.4 缓存配置

```yaml
storage:
  wiredTiger:
    engineConfig:
      cacheSizeGB: 4    # 建议 50% 物理内存
      journalCompressor: snappy
    collectionConfig:
      blockCompressor: snappy
    indexConfig:
      prefixCompression: true
```

---

## 5. Docker 部署

### 5.1 快速启动单节点

```bash
# 拉取镜像
docker pull mongo:7.0

# 运行容器
docker run -d \
  --name mongodb \
  -p 27017:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=admin \
  -e MONGO_INITDB_ROOT_PASSWORD=admin123 \
  -v mongodb_data:/data/db \
  -v mongodb_config:/data/configdb \
  --restart always \
  mongo:7.0

# 验证
docker exec -it mongodb mongosh -u admin -p admin123 --authenticationDatabase admin
```

### 5.2 Docker Compose（含 Mongo Express 管理界面）

```yaml
version: '3.8'
services:
  mongodb:
    image: mongo:7.0
    container_name: mongodb
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: admin123
    volumes:
      - mongodb_data:/data/db
      - mongodb_config:/data/configdb
      - ./init-mongo.js:/docker-entrypoint-initdb.d/init-mongo.js:ro
    networks:
      - mongo_net
    restart: always

  mongo-express:
    image: mongo-express:latest
    container_name: mongo-express
    ports:
      - "8081:8081"
    environment:
      ME_CONFIG_MONGODB_ADMINUSERNAME: admin
      ME_CONFIG_MONGODB_ADMINPASSWORD: admin123
      ME_CONFIG_MONGODB_SERVER: mongodb
      ME_CONFIG_BASICAUTH_USERNAME: admin
      ME_CONFIG_BASICAUTH_PASSWORD: admin123
    depends_on:
      - mongodb
    networks:
      - mongo_net
    restart: always

volumes:
  mongodb_data:
  mongodb_config:

networks:
  mongo_net:
```

启动与访问：

```bash
docker compose up -d
# Mongo Express 访问：http://localhost:8081
```

### 5.3 初始化脚本示例

```javascript
// init-mongo.js
db = db.getSiblingDB("admin");
db.auth("admin", "admin123");

// 创建业务数据库和用户
db = db.getSiblingDB("app_db");
db.createUser({
  user: "app_user",
  pwd: "app_pass",
  roles: [{ role: "readWrite", db: "app_db" }]
});

// 创建测试数据
db.users.insertOne({ name: "admin", role: "admin", createdAt: new Date() });
```

### 5.4 Docker 常用运维命令

```bash
# 进入容器
docker exec -it mongodb bash

# 连接 MongoDB Shell
docker exec -it mongodb mongosh -u admin -p admin123

# 查看日志
docker logs mongodb -f

# 备份
docker exec mongodb mongodump --username admin --password admin123 --authenticationDatabase admin --out /tmp/backup

# 容器资源监控
docker stats mongodb
```

---

## 6. Linux 部署

### 6.1 CentOS / RHEL 部署

```bash
# 1. 配置 yum 源
cat > /etc/yum.repos.d/mongodb-org-7.0.repo <<'EOF'
[mongodb-org-7.0]
name=MongoDB Repository
baseurl=https://repo.mongodb.org/yum/redhat/$releasever/mongodb-org/7.0/x86_64/
gpgcheck=1
enabled=1
gpgkey=https://www.mongodb.org/static/pgp/server-7.0.asc
EOF

# 2. 安装
yum install -y mongodb-org

# 3. 创建数据目录
mkdir -p /data/mongodb
chown -R mongod:mongod /data/mongodb

# 4. 配置防火墙
firewall-cmd --add-port=27017/tcp --permanent
firewall-cmd --reload

# 5. 修改配置文件 /etc/mongod.conf，修改 dbPath
# 见下一节

# 6. 启动
systemctl start mongod
systemctl enable mongod

# 7. 验证
mongosh --port 27017
```

### 6.2 Ubuntu / Debian 部署

```bash
# 1. 导入 GPG 密钥
wget -qO - https://www.mongodb.org/static/pgp/server-7.0.asc | sudo apt-key add -

# 2. 添加仓库
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list

# 3. 更新并安装
sudo apt-get update
sudo apt-get install -y mongodb-org

# 4. 启动
sudo systemctl start mongod
sudo systemctl enable mongod
```

### 6.3 基础安全设置

```bash
# 创建管理员用户（首次无密码登录后执行）
mongosh
use admin
db.createUser({
  user: "admin",
  pwd: "your_secure_password",
  roles: ["root"]
})

# 开启认证后重启
# 修改 /etc/mongod.conf，添加 security.authorization: enabled
systemctl restart mongod

# 认证登录
mongosh -u admin -p your_secure_password --authenticationDatabase admin
```

---

## 7. 核心配置详解

### 7.1 mongod.conf 完整配置

```yaml
# mongod.conf - MongoDB 全配置参考

# 网络配置
net:
  port: 27017
  bindIp: 0.0.0.0          # 生产环境建议绑定内网 IP
  # bindIp: 127.0.0.1,192.168.1.100
  maxIncomingConnections: 65536

# 存储配置
storage:
  dbPath: /data/mongodb    # 数据文件存储路径
  journal:
    enabled: true          # 开启 WAL 日志，防崩溃丢数据
    commitIntervalMs: 100  # Journal 刷盘间隔（ms）
  wiredTiger:
    engineConfig:
      cacheSizeGB: 4       # WiredTiger 缓存大小（建议 50% 物理内存）
      journalCompressor: snappy   # Journal 压缩算法：snappy / zlib / zstd
    collectionConfig:
      blockCompressor: snappy     # 集合数据压缩
    indexConfig:
      prefixCompression: true     # 索引前缀压缩

# 日志配置
systemLog:
  destination: file
  path: /var/log/mongodb/mongod.log  # 日志文件路径
  logAppend: true                     # 日志追加而非覆盖
  logRotate: reopen                   # 日志轮转方式
  verbosity: 0                        # 日志级别：0-5，越大越详细

# 安全配置（生产必开）
security:
  authorization: enabled    # 开启认证
  # keyFile: /etc/mongodb-keyfile  # 副本集密钥文件

# 进程管理
processManagement:
  fork: true
  pidFilePath: /var/run/mongodb/mongod.pid
  timeZoneInfo: /usr/share/zoneinfo

# 副本集配置
replication:
  replSetName: rs0          # 副本集名称
  oplogSizeMB: 10240        # oplog 大小（MB）

# 分片配置
sharding:
  clusterRole: shardsvr     # shardsvr / configsvr

# 性能分析
operationProfiling:
  mode: slowOp              # off / slowOp / all
  slowOpThresholdMs: 100    # 慢查询阈值（ms）
```

### 7.2 连接字符串格式

```text
# 标准格式
mongodb://[username:password@]host[:port][/database][?options]

# 无认证
mongodb://localhost:27017

# 有认证
mongodb://admin:admin123@localhost:27017/admin

# 副本集（自动发现主节点）
mongodb://host1:27017,host2:27017,host3:27017/?replicaSet=rs0

# 分片集群（连接 mongos）
mongodb://mongos1:27017,mongos2:27017,mongos3:27017

# 常用参数
mongodb://admin:admin123@localhost:27017/admin?connectTimeoutMS=3000&socketTimeoutMS=30000&maxPoolSize=100
```

### 7.3 mongosh 常用命令

```javascript
// 连接测试
mongosh "mongodb://admin:admin123@localhost:27017/admin"

// 查看版本
db.version()
// 7.0.x

// 数据库统计
db.stats()

// 查看当前连接
db.serverStatus().connections

// 慢查询设置（>100ms 记录到 system.profile）
db.setProfilingLevel(1, { slowms: 100 })

// 查看慢查询日志
db.system.profile.find().sort({ ts: -1 }).limit(5).pretty()

// 查看当前正在执行的操作
db.currentOp()

// 终止操作
db.killOp(opid)

// 查看集合大小
db.collection.stats()

// 查看数据库列表
show dbs

// 切换数据库
use mydb

// 查看集合列表
show collections
```

---

## 8. 用户与权限管理

### 8.1 内置角色

| 角色 | 权限范围 | 适用场景 |
|------|----------|----------|
| `read` | 只读 | 数据分析、报表查询 |
| `readWrite` | 读写 | 应用程序 |
| `dbAdmin` | 索引管理、统计、Schema 操作 | 管理员 |
| `userAdmin` | 用户和角色管理 | 权限管理员 |
| `dbOwner` | readWrite + dbAdmin + userAdmin | 库所有者 |
| `clusterAdmin` | 集群管理最高权限 | 集群管理员 |
| `backup` | 备份相关权限 | 备份工具 |
| `restore` | 恢复相关权限 | 恢复工具 |
| `root` | 所有权限（超级管理员） | 超级管理员 |

### 8.2 创建用户

```javascript
// 切换到管理员库
use admin

// 创建超级管理员
db.createUser({
  user: "admin",
  pwd: passwordPrompt(),  // 交互式输入密码
  roles: ["root"]
})

// 或明文密码
db.createUser({
  user: "admin",
  pwd: "SecurePass123!",
  roles: ["root"]
})

// 创建业务库读写用户
use mydb
db.createUser({
  user: "app_user",
  pwd: "app_pass",
  roles: [
    { role: "readWrite", db: "mydb" },
    { role: "read", db: "report_db" }
  ]
})

// 创建只读用户
db.createUser({
  user: "readonly",
  pwd: "read_pass",
  roles: [{ role: "read", db: "mydb" }]
})
```

### 8.3 用户管理操作

```javascript
// 查看当前库所有用户
db.getUsers()

// 查看全局所有用户
use admin
db.system.users.find().pretty()

// 修改密码
db.changeUserPassword("admin", "new_password")

// 修改用户角色
db.updateUser("app_user", {
  roles: [
    { role: "readWrite", db: "mydb" },
    { role: "dbAdmin", db: "mydb" }
  ]
})

// 删除用户
db.dropUser("readonly")

// 删除当前库所有用户
db.dropAllUsers()

// 查看用户权限
db.runCommand({
  usersInfo: "app_user",
  showPrivileges: true
})
```

### 8.4 自定义角色

```javascript
use admin
db.createRole({
  role: "customReportRole",
  privileges: [
    {
      resource: { db: "mydb", collection: "" },
      actions: ["find", "aggregate"]
    }
  ],
  roles: []
})
```

---

## 9. 生产部署 Checklist

### 9.1 基础设施

| 检查项 | 要求 | 检查方法 |
|--------|------|----------|
| 绑定 IP | 内网 `bindIp: 0.0.0.0`，外网需防火墙白名单 | `cat /etc/mongod.conf \| grep bindIp` |
| 认证开启 | `security.authorization: enabled` | `db.serverStatus().security` |
| Journal 日志 | 开启，防止崩溃丢数据 | `cat /etc/mongod.conf \| grep journal` |
| WiredTiger 缓存 | 建议 50% 物理内存 | `db.serverStatus().wiredTiger.cache` |
| 磁盘 | SSD 首选，IOPS >= 5000 | `fio --randwrite --ioengine=libaio` |
| 文件描述符 | >= 64000 | `ulimit -n` |
| 透明大页 | 关闭，减少内存碎片 | `cat /sys/kernel/mm/transparent_hugepage/enabled` |
| NUMA | 禁用或配置 interleave | `numactl --interleave=all` |

### 9.2 系统参数优化

```bash
# 1. 文件描述符
cat >> /etc/security/limits.conf <<EOF
mongod soft nofile 64000
mongod hard nofile 64000
mongod soft nproc 64000
mongod hard nproc 64000
EOF

# 2. 关闭透明大页
cat >> /etc/rc.local <<EOF
echo never > /sys/kernel/mm/transparent_hugepage/enabled
echo never > /sys/kernel/mm/transparent_hugepage/defrag
EOF

# 3. 关闭 NUMA（MongoDB 启动时）
numactl --interleave=all mongod --config /etc/mongod.conf
```

### 9.3 备份策略

| 备份方式 | 特点 | 适用场景 |
|----------|------|----------|
| mongodump | 逻辑备份，可用性高 | 小数据量、单库 |
| 文件快照 | 物理备份，速度快 | 大数据量、整机 |
| Atlas 备份 | 云托管 | Atlas 用户 |

### 9.4 监控指标

```javascript
// 连接数
db.serverStatus().connections

// 内存使用
db.serverStatus().mem

// WiredTiger Cache 命中率
db.serverStatus().wiredTiger.cache['bytes currently in the cache']

// opcounters（QPS）
db.serverStatus().opcounters

// 复制延迟
rs.status().members.forEach(m => print(m.name + ': ' + m.optimeDate))
```

### 9.5 日常运维命令速查

```bash
# 备份
mongodump --host localhost --port 27017 --username admin --password pass --authenticationDatabase admin --db mydb --out /backup/$(date +%Y%m%d)

# 还原
mongorestore --host localhost --port 27017 --username admin --password pass --authenticationDatabase admin --db mydb /backup/20240726/mydb

# 压缩导出
mongodump --archive=mydb.gz --gzip --db mydb

# 导入 JSON
mongoimport --db mydb --collection users --file users.json

# 导出 JSON
mongoexport --db mydb --collection users --out users.json
```

---

## 面试核心

| 问题 | 答案 |
|------|------|
| MongoDB 是什么类型数据库？ | 文档型 NoSQL，BSON 格式存储 |
| 与 MySQL 最大区别？ | 无固定 Schema、横向扩展容易、事务弱于 MySQL |
| `_id` 默认类型？ | ObjectId，12 字节，分布式唯一 |
| WiredTiger 三个关键特性？ | 文档级并发锁 + 压缩存储 + 多文档事务 |
| MongoDB 事务支持情况？ | 4.0 多文档事务，4.2 分布式事务，性能弱于传统 RDBMS |
| BSON 对比 JSON 优势？ | 更多数据类型（Date, ObjectId, BinData）、解析更快、支持二进制 |
| 什么时候选 MongoDB？ | Schema 多变、需要横向扩展、文档嵌套结构 |
| 什么时候不选 MongoDB？ | 强事务需求、复杂 JOIN、固定 Schema 的简单 CRUD |

> **极简总结**：MongoDB 是文档型 NoSQL，BSON 格式，动态 Schema。WiredTiger 引擎提供文档级并发和压缩。部署首选 Docker，生产必开认证和 Journal。
