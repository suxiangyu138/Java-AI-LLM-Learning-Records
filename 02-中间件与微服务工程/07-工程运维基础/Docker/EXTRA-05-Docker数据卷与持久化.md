# 05 - Docker 数据卷与持久化

## 5.1 为什么需要数据持久化？

容器是无状态的——容器删除后，其内部数据也随之消失。对于数据库、日志、上传文件等需要持久化的数据，必须使用 Docker 的存储机制。

```
容器生命周期     存储生命周期
──────────────────────────────────
Created ──┐
Running   │    容器存储层（随容器删除）
Stopped   │
Deleted  ◀┘
                Volume / Bind Mount（独立于容器，手动删除）
```

## 5.2 三种存储方式对比

| 特性 | Volume | Bind Mount | tmpfs |
|---|---|---|---|
| 存储位置 | `/var/lib/docker/volumes/` | 宿主机任意路径 | 内存 |
| 管理方式 | Docker 管理 | 用户管理 | Docker 管理 |
| 可移植性 | 高（Docker 命令管理） | 低（依赖宿主机路径结构） | 容器级别 |
| 性能 | 较好 | 原生性能 | 极高（内存） |
| 跨容器共享 | 支持 | 支持 | 不支持 |
| 适用场景 | 生产数据库、应用数据 | 开发热重载、配置文件 | 敏感临时数据 |

## 5.3 Volume（命名卷）【推荐】

### 基本操作

```bash
# 创建 Volume
docker volume create my-volume

# 查看所有 Volume
docker volume ls

# 查看 Volume 详情（挂载点路径等）
docker volume inspect my-volume

# 删除 Volume
docker volume rm my-volume

# 清理所有未使用的 Volume
docker volume prune
```

### 使用 Volume

```bash
# 启动容器时挂载 Volume
docker run -d \
  --name my-mysql \
  -v mysql-data:/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=secret \
  mysql:8.0

# -v 语法：volume名称:容器内路径

# 使用 --mount 语法（更明确，推荐）
docker run -d \
  --name my-mysql \
  --mount source=mysql-data,target=/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=secret \
  mysql:8.0
```

### Volume 的备份与迁移

```bash
# 1. 备份 Volume 数据
docker run --rm \
  -v mysql-data:/source \
  -v $(pwd):/backup \
  alpine tar czf /backup/mysql-backup.tar.gz -C /source .

# 2. 恢复 Volume 数据
docker run --rm \
  -v mysql-data:/target \
  -v $(pwd):/backup \
  alpine tar xzf /backup/mysql-backup.tar.gz -C /target

# 3. 迁移 Volume 到另一台机器
# 源机器导出
docker run --rm -v mysql-data:/data alpine tar czf - -C /data . > backup.tar.gz
# 拷贝到目标机器
scp backup.tar.gz user@target-host:/tmp/
# 目标机器导入
docker volume create mysql-data
docker run --rm -v mysql-data:/data -v /tmp:/backup \
  alpine tar xzf /backup/backup.tar.gz -C /data
```

## 5.4 Bind Mount（绑定挂载）

将宿主机文件或目录**直接映射**到容器内，适合**开发环境**。

```bash
# 语法：-v 宿主机绝对路径:容器内路径[:选项]
# 选项：ro（只读）、rw（读写，默认）

# 示例1：挂载代码目录，实现热重载
docker run -d \
  --name my-dev-app \
  -v /home/user/project/app:/app \
  -p 3000:3000 \
  node:20 npm run dev

# 示例2：挂载配置文件（只读）
docker run -d \
  --name my-nginx \
  -v /home/user/nginx.conf:/etc/nginx/nginx.conf:ro \
  -v /home/user/html:/usr/share/nginx/html:ro \
  -p 80:80 \
  nginx:alpine

# 示例3：使用当前目录的相对路径（仅 docker-compose 支持）
docker run -v $(pwd)/config:/app/config myapp
# PowerShell：-v ${PWD}/config:/app/config
```

### 开发场景：Node.js 热重载

```
项目目录结构：
./my-node-app/
├── package.json
├── node_modules/       ← 容器内安装，不映射（避免平台差异）
├── src/
│   └── index.js        ← 映射到容器，修改后自动生效
└── docker-compose.yml
```

```bash
# 绑定源码和 node_modules 分离处理
docker run -d \
  --name node-dev \
  -v $(pwd):/app \                  # 映射整个项目
  -v /app/node_modules \             # 匿名卷，保留容器内 node_modules
  -w /app \
  -p 3000:3000 \
  node:20 npm run dev
```

> 注意：`-v /app/node_modules` 创建的是匿名 Volume，会覆盖 bind mount 中的同名目录，避免宿主机的 node_modules 覆盖容器内的。

## 5.5 tmpfs Mount

数据存储在**内存**中，容器停止即消失，适合**敏感数据**（密钥、token）。

```bash
# 创建 tmpfs 挂载
docker run -d \
  --name my-app \
  --tmpfs /app/tmp:rw,size=128m \
  --tmpfs /run/secrets:ro,noexec \
  myapp:latest

# 使用 --mount 语法
docker run -d \
  --name my-app \
  --mount type=tmpfs,destination=/app/tmp,tmpfs-size=128m \
  myapp:latest
```

### tmpfs vs Volume vs Bind Mount 适用场景

| 场景 | 推荐方式 |
|---|---|
| 数据库数据 | Volume |
| 应用日志 | Volume 或 Bind Mount |
| 开发代码热重载 | Bind Mount |
| 配置文件 | Bind Mount (ro) |
| Session / Token / 密钥 | tmpfs |
| 上传临时文件 | tmpfs |

## 5.6 数据卷容器（Data Volume Container）

一种通过容器来共享数据卷的模式（已不推荐，但需要了解）。

```bash
# 1. 创建数据卷容器
docker create \
  -v /data \
  --name data-store \
  alpine /bin/true

# 2. 其他容器通过 --volumes-from 继承
docker run -d --name app1 --volumes-from data-store myapp
docker run -d --name app2 --volumes-from data-store myapp

# 现在 app1 和 app2 共享同一个 /data 目录
```

> 现代 Docker 推荐直接使用 `docker volume create` + `--mount`，而不是数据卷容器模式。

## 5.7 存储驱动 (Storage Driver)

Docker 使用存储驱动来实现镜像分层和容器可写层。

| 驱动 | 说明 | 推荐场景 |
|---|---|---|
| **overlay2** | 当前默认，性能好，推荐 | 所有 Linux 发行版 |
| aufs | 最早的支持，已逐步废弃 | 旧版 Ubuntu |
| devicemapper | 基于 LVM | CentOS/RHEL 旧版 |
| btrfs/zfs | 高级文件系统特性 | 需要快照/压缩等特性 |

```bash
# 查看当前存储驱动
docker info | grep "Storage Driver"
```

## 5.8 实战：MySQL + Volume

```bash
# 创建专用 Volume
docker volume create mysql-data
docker volume create mysql-config

# 运行 MySQL，数据持久化到 Volume
docker run -d \
  --name mysql-prod \
  --mount source=mysql-data,target=/var/lib/mysql \
  --mount source=mysql-config,target=/etc/mysql/conf.d \
  -e MYSQL_ROOT_PASSWORD=StrongP@ssw0rd \
  -e MYSQL_DATABASE=myapp \
  --restart unless-stopped \
  -p 3306:3306 \
  mysql:8.0

# 备份数据库
docker exec mysql-prod mysqldump -u root -pStrongP@ssw0rd myapp \
  > myapp-backup.sql

# 升级 MySQL 版本
docker stop mysql-prod
docker rm mysql-prod
# 使用同一个 Volume 启动新版本
docker run -d \
  --name mysql-prod \
  --mount source=mysql-data,target=/var/lib/mysql \
  --mount source=mysql-config,target=/etc/mysql/conf.d \
  -e MYSQL_ROOT_PASSWORD=StrongP@ssw0rd \
  --restart unless-stopped \
  -p 3306:3306 \
  mysql:8.4    # 升级小版本，数据不丢
```

---

> **下一步**：[06-Docker网络管理](06-Docker网络管理.md)
