# MongoDB 安装与部署（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | MongoDB 环境搭建速查
> **版本**：MongoDB 6.x/7.x
> **环境**：Docker / Linux / Windows

---

## 一、Docker 部署（推荐）

### 1.1 单节点

```bash
docker run -d \
  --name mongodb \
  -p 27017:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=admin \
  -e MONGO_INITDB_ROOT_PASSWORD=admin123 \
  -v mongodb_data:/data/db \
  mongo:7.0
```

### 1.2 开发环境（无密码）

```bash
docker run -d \
  --name mongodb \
  -p 27017:27017 \
  -v mongodb_data:/data/db \
  mongo:7.0
```

### 1.3 Docker Compose（MongoDB + Mongo Express 可视化管理）

```yaml
version: '3'
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
    networks:
      - mongo_net

  mongo-express:
    image: mongo-express:latest
    container_name: mongo-express
    ports:
      - "8081:8081"
    environment:
      ME_CONFIG_MONGODB_ADMINUSERNAME: admin
      ME_CONFIG_MONGODB_ADMINPASSWORD: admin123
      ME_CONFIG_MONGODB_SERVER: mongodb
    depends_on:
      - mongodb
    networks:
      - mongo_net

volumes:
  mongodb_data:

networks:
  mongo_net:
```

访问 `http://localhost:8081` 可视化管理。

---

## 二、Linux 部署

### 2.1 安装步骤（CentOS/RHEL）

```bash
# 1. 配置 yum 源
cat > /etc/yum.repos.d/mongodb-org-7.0.repo <<EOF
[mongodb-org-7.0]
name=MongoDB Repository
baseurl=https://repo.mongodb.org/yum/redhat/\$releasever/mongodb-org/7.0/x86_64/
gpgcheck=1
enabled=1
gpgkey=https://www.mongodb.org/static/pgp/server-7.0.asc
EOF

# 2. 安装
yum install -y mongodb-org

# 3. 配置
vi /etc/mongod.conf
```

### 2.2 核心配置 `/etc/mongod.conf`

```yaml
# 网络
net:
  port: 27017
  bindIp: 0.0.0.0          # 允许远程访问

# 存储
storage:
  dbPath: /data/mongodb
  journal:
    enabled: true            # 预写日志
  wiredTiger:
    engineConfig:
      cacheSizeGB: 4         # WiredTiger 缓存大小

# 日志
systemLog:
  destination: file
  path: /var/log/mongodb/mongod.log
  logAppend: true

# 安全（生产必开）
security:
  authorization: enabled

# 副本集（集群时启用）
replication:
  replSetName: rs0
```

### 2.3 防火墙与启动

```bash
# 防火墙开放端口
firewall-cmd --add-port=27017/tcp --permanent
firewall-cmd --reload

# 启动
systemctl start mongod
systemctl enable mongod

# 验证
mongosh --port 27017
```

---

## 三、Windows 部署

```powershell
# 1. 下载 MSI 安装包，双击安装
# 2. 或解压 zip 版，配置环境变量

# 创建数据目录
mkdir C:\data\db

# 启动（默认端口 27017）
mongod --dbpath C:\data\db

# 新终端连接
mongosh
```

**注意事项**：
- 数据目录 `C:\data\db` 需手动创建
- Windows 建议安装为服务：`mongod --install`
- 防火墙需允许 27017 端口

---

## 四、连接与验证

### 4.1 mongosh 连接

```bash
# 本地无密码
mongosh

# 有密码
mongosh -u admin -p admin123 --authenticationDatabase admin

# 远程
mongosh mongodb://admin:admin123@192.168.1.100:27017/admin
```

### 4.2 Java SpringBoot 连接串

```yaml
# application.yml — 单节点
spring:
  data:
    mongodb:
      uri: mongodb://admin:admin123@localhost:27017/mydb

# 副本集
spring:
  data:
    mongodb:
      uri: mongodb://admin:admin123@host1:27017,host2:27017,host3:27017/mydb?replicaSet=rs0
```

---

## 五、MongoDB Atlas（云托管）

无需自建，MongoDB 官方云服务：

```
免费层 M0：512MB 存储，共享集群，适合学习和原型
付费层 M10+：2GB+ 存储，专用集群，生产可用
```

获取连接串 → 配置到 SpringBoot 即可使用。

---

## 六、用户与权限管理

```javascript
// 1. 切换到 admin 数据库
use admin

// 2. 创建管理员
db.createUser({
  user: "admin",
  pwd: "admin123",
  roles: ["root"]
})

// 3. 为业务库创建用户
use mydb
db.createUser({
  user: "app_user",
  pwd: "app_pass",
  roles: [
    { role: "readWrite", db: "mydb" }
  ]
})

// 4. 查看所有用户
db.getUsers()
```

---

## 七、生产部署 Checklist

| 检查项 | 要求 |
|---|---|
| **绑定 IP** | 内网 `bindIp: 0.0.0.0`，外网需防火墙白名单 |
| **认证开启** | `security.authorization: enabled` |
| **Journal 日志** | 开启，防止崩溃丢数据 |
| **WiredTiger 缓存** | 建议 50% 物理内存 |
| **磁盘** | SSD 首选 |
| **文件描述符** | >= 64000 |
| **关闭 transparent_hugepage** | 减少内存碎片 |
| **备份** | 定期 mongodump |

---

## 八、常用运维命令

```javascript
// 查看版本
db.version()

// 数据库统计
db.stats()

// 当前连接数
db.serverStatus().connections

// 慢查询日志设置（>100ms）
db.setProfilingLevel(1, { slowms: 100 })

// 查看慢查询
db.system.profile.find().sort({ ts: -1 }).limit(5)
```

---

## 九、极简总结

```
Docker 单节点 = mongo:7.0 + MONGO_INITDB_ROOT_USERNAME
生产必配 = auth + bindIp + WiredTiger cacheSizeGB
SpringBoot 连接 = spring.data.mongodb.uri
Mongo Express = 8081 端口可视化管理
Atlas = 官方云托管，分层免费
```
