# 04 - Docker 容器管理

## 4.1 容器生命周期

```
           docker create
  Image ──────────────────▶ Created（已创建）
    │                            │
    │ docker run                 │ docker start
    │                            ▼
    │              ┌──▶ Running（运行中）
    │              │         │
    │              │         │ docker stop / docker kill
    │              │         ▼
    │              │    Stopped（已停止）
    │              │         │
    │              │         │ docker start
    │              │         │
    │              │         │ docker rm
    │              │         ▼
    │              └──▶ Deleted（已删除）
    │
    │ docker run = docker create + docker start + docker attach
```

容器有 7 种状态：**created、running、paused、restarting、exited、dead、removing**。

## 4.2 核心命令详解

### 创建并运行容器

```bash
# docker run 最常用命令
docker run [选项] 镜像 [命令]

# 基础示例
docker run -it ubuntu:22.04 bash

# 选项说明：
#   -d          后台运行（detach）
#   -it         交互模式 + 分配伪终端
#   --name      指定容器名称
#   -p          端口映射 宿主机端口:容器端口
#   -v          挂载数据卷 宿主机路径:容器路径
#   -e          设置环境变量
#   --rm        容器退出后自动删除
#   --restart   重启策略 (no/always/on-failure/unless-stopped)
#   --network   指定网络
#   -m / --memory  内存限制
#   --cpus      CPU 限制
```

### 实际案例

```bash
# 1. 运行一个 Nginx Web 服务器
docker run -d \
  --name my-nginx \
  -p 8080:80 \
  -v $(pwd)/html:/usr/share/nginx/html:ro \
  --restart unless-stopped \
  nginx:1.25-alpine

# 访问 http://localhost:8080

# 2. 运行一个交互式 Ubuntu 环境
docker run -it --rm \
  --name my-ubuntu \
  ubuntu:22.04 bash

# 3. 运行 MySQL 数据库
docker run -d \
  --name my-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=my-secret-pw \
  -v mysql-data:/var/lib/mysql \
  mysql:8.0

# 4. 一次性任务：运行完就删除
docker run --rm node:20 node -e "console.log('Hello from Docker')"
```

### 查看容器

```bash
# 查看运行中的容器
docker ps

# 查看所有容器（包括已停止的）
docker ps -a

# 输出列解读：
# CONTAINER ID  IMAGE        COMMAND     CREATED      STATUS       PORTS                  NAMES
# a1b2c3d4e5f6  nginx:alpine "/docker..." 2 min ago   Up 2 min    0.0.0.0:8080->80/tcp   my-nginx

# 只显示容器 ID
docker ps -q

# 显示最近创建的 N 个容器
docker ps -n 5

# 按状态过滤
docker ps -f status=exited
docker ps -f status=running

# 显示容器大小
docker ps -s
```

### 容器操作

```bash
# 启动已停止的容器
docker start my-nginx

# 停止运行中的容器（发送 SIGTERM，10秒后 SIGKILL）
docker stop my-nginx

# 强制停止（直接 SIGKILL）
docker kill my-nginx

# 重启容器
docker restart my-nginx

# 暂停/恢复容器（冻结进程，不释放内存）
docker pause my-nginx
docker unpause my-nginx

# 删除容器（必须先停止）
docker rm my-nginx
docker rm -f my-nginx    # 强制删除（即使运行中）

# 删除所有已停止的容器
docker container prune
docker rm $(docker ps -aq)   # 删除所有容器（慎用）
```

### 进入容器 & 调试

```bash
# 在运行中的容器里执行命令
docker exec -it my-nginx bash

# 执行单条命令
docker exec my-nginx cat /etc/nginx/nginx.conf

# 以 root 身份执行（当容器默认非 root 时）
docker exec -u root -it my-nginx bash

# 查看容器日志
docker logs my-nginx
docker logs -f my-nginx              # 实时跟踪（类似 tail -f）
docker logs --tail 100 my-nginx       # 最后 100 行
docker logs --since 30m my-nginx      # 最近 30 分钟
docker logs -t my-nginx               # 显示时间戳

# 查看容器内进程
docker top my-nginx

# 查看容器资源使用情况
docker stats
docker stats my-nginx --no-stream     # 一次性输出

# 查看容器详细信息（JSON 格式）
docker inspect my-nginx

# 获取特定字段
docker inspect -f '{{.NetworkSettings.IPAddress}}' my-nginx
docker inspect -f '{{.State.Status}}' my-nginx

# 从容器复制文件到宿主机
docker cp my-nginx:/etc/nginx/nginx.conf ./nginx.conf

# 从宿主机复制文件到容器
docker cp ./nginx.conf my-nginx:/etc/nginx/nginx.conf
```

## 4.3 docker run 常用选项速查

| 选项 | 说明 | 示例 |
|---|---|---|
| `-d` | 后台运行 | `docker run -d nginx` |
| `-it` | 交互 + 终端 | `docker run -it ubuntu bash` |
| `--name` | 容器名称 | `docker run --name web nginx` |
| `-p 8080:80` | 端口映射 | `docker run -p 8080:80 nginx` |
| `-p 127.0.0.1:8080:80` | 仅本地端口映射 | |
| `-v /host:/container` | 挂载数据卷 | `docker run -v ./html:/usr/share/nginx/html nginx` |
| `-v vol-name:/path` | 挂载命名卷 | `docker run -v data:/app/data nginx` |
| `-e KEY=VALUE` | 环境变量 | `docker run -e MYSQL_ROOT_PW=123 mysql` |
| `--env-file .env` | 从文件加载环境变量 | `docker run --env-file .env myapp` |
| `--rm` | 退出后自动删除 | `docker run --rm node:20 node -e "1+1"` |
| `--restart always` | 总是重启 | `docker run --restart always nginx` |
| `--network mynet` | 指定网络 | `docker run --network mynet nginx` |
| `-m 512m` | 内存限制 | `docker run -m 512m nginx` |
| `--cpus 1.5` | CPU 限制 | `docker run --cpus 1.5 nginx` |
| `--add-host host:ip` | 添加 hosts 记录 | `docker run --add-host api.local:10.0.0.1 nginx` |
| `--dns 8.8.8.8` | 指定 DNS | `docker run --dns 8.8.8.8 nginx` |
| `-u 1000:1000` | 指定用户 | `docker run -u 1000:1000 myapp` |
| `-w /app` | 工作目录 | `docker run -w /app myapp` |

## 4.4 容器生命周期管理最佳实践

```bash
# 1. 开发调试时使用 --rm，避免堆积垃圾容器
docker run --rm -it myapp:dev bash

# 2. 生产服务使用 --restart unless-stopped
docker run -d --restart unless-stopped myapp:prod

# 3. 合理命名，方便管理
docker run -d --name prod-nginx-v2 nginx:1.25

# 4. 及时清理不用的资源
docker container prune -f      # 清理已停止的容器
docker image prune -a -f       # 清理未使用的镜像
docker system prune -a -f      # 一键清理（镜像+容器+网络+缓存）
```

## 4.5 容器 vs docker run 一次性和长期任务

### 一次性任务

```bash
# 运行 Python 脚本
docker run --rm -v $(pwd):/app -w /app python:3.11 python script.py

# 编译 Go 项目
docker run --rm -v $(pwd):/app -w /app golang:1.21 go build -o main .

# 数据库备份
docker run --rm -v $(pwd):/backup \
  --network my-net \
  mysql:8.0 mysqldump -h my-mysql -u root -p$PASS dbname > backup.sql
```

### 长期服务

```bash
# Web 应用
docker run -d \
  --name my-web \
  --restart unless-stopped \
  -p 80:3000 \
  -e NODE_ENV=production \
  myapp:latest
```

---

> **下一步**：[05-Docker数据卷与持久化](05-Docker数据卷与持久化.md)
