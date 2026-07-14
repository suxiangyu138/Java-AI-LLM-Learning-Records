# 06 - Docker 网络管理

## 6.1 Docker 网络模型概述

Docker 的网络子系统使用**可插拔驱动**架构，默认提供以下几种网络驱动：

| 驱动 | 说明 | 适用场景 |
|---|---|---|
| **bridge** | 默认网络驱动，创建虚拟网桥 | 单机容器通信 |
| **host** | 直接使用宿主机网络栈 | 高性能场景 |
| **overlay** | 跨多台 Docker 主机的网络 | Swarm 集群 |
| **none** | 禁用网络 | 不需要网络的容器 |
| **macvlan/ipvlan** | 容器直接获得物理网络 IP | 与传统系统集成 |

## 6.2 默认 Bridge 网络详解

当你安装 Docker 时，会自动创建一个名为 `bridge` 的默认网络（驱动为 `bridge`）。

```
┌─────────────────────────────────────────────┐
│              宿主机 (192.168.1.100)           │
│  ┌──────────────────────────────────────┐   │
│  │           docker0 (172.17.0.1)        │   │
│  │         ┌────────┴────────┐           │   │
│  │         │  虚拟交换机/网桥  │           │   │
│  │         └──┬──────┬──────┬┘           │   │
│  │  ┌────────▼──┐ ┌──▼──────▼──┐        │   │
│  │  │ ContainerA │ │ ContainerB │        │   │
│  │  │172.17.0.2  │ │172.17.0.3  │        │   │
│  │  └────────────┘ └────────────┘        │   │
│  └──────────────────────────────────────┘   │
│                     │ NAT (iptables)        │
└─────────────────────┼───────────────────────┘
                      │
                   外部网络
```

**默认 bridge 的特点：**
- 容器自动获得 `172.17.0.0/16` 网段的 IP
- 通过 `docker0` 虚拟网桥通信
- 容器之间**默认可以用 IP 互通**，但**不能用容器名互相发现**
- 出站流量通过宿主机的 NAT 转发

## 6.3 自定义 Bridge 网络【推荐】

自定义 bridge 网络比默认 bridge 更强大：

```bash
# 创建自定义网络
docker network create my-net

# 创建时指定子网和网关
docker network create \
  --driver bridge \
  --subnet 172.20.0.0/16 \
  --gateway 172.20.0.1 \
  my-net

# 查看网络列表
docker network ls

# 查看网络详情（包含连接的容器列表）
docker network inspect my-net

# 删除网络
docker network rm my-net

# 清理未使用的网络
docker network prune
```

### 使用自定义网络

```bash
# 在自定义网络中运行容器
docker run -d --name web --network my-net nginx:alpine
docker run -d --name db --network my-net -e MYSQL_ROOT_PW=123 mysql:8.0

# 同一自定义网络中的容器可以互相 ping 通（通过容器名）
docker exec web ping db     # ✅ 可以直接用容器名！

# 在默认 bridge 中则不行
docker run -d --name web2 nginx:alpine
docker run -d --name db2 mysql:8.0
docker exec web2 ping db2   # ❌ 默认 bridge 不支持 DNS 解析
```

### 自定义 bridge vs 默认 bridge

| 功能 | 默认 bridge | 自定义 bridge |
|---|---|---|
| DNS 自动解析（容器名→IP） | ❌ 不支持 | ✅ 支持 |
| 容器隔离 | 依赖 --link（已废弃） | ✅ 网络级隔离 |
| 运行时连接/断开 | ❌ 需要重启 | ✅ `docker network connect/disconnect` |
| 共享环境变量 | 仅 --link | ❌ 不支持（更安全） |
| 配置灵活性 | 低 | 高 |

## 6.4 容器间通信方式

### 方式 1：通过容器名（自定义网络）

```bash
docker network create my-net
docker run -d --name app --network my-net myapp
docker run -d --name db --network my-net mysql:8.0

docker exec app ping db     # Docker 内置 DNS 解析
```

### 方式 2：通过端口映射（外部访问）

```bash
# 将容器 80 端口映射到宿主机 8080
docker run -d -p 8080:80 --name web nginx:alpine

# 多端口映射
docker run -d -p 8080:80 -p 8443:443 --name web nginx:alpine

# 只绑定本地回环地址（仅本机访问）
docker run -d -p 127.0.0.1:8080:80 nginx:alpine

# 随机端口映射
docker run -d -P --name web nginx:alpine
# -P 将所有 EXPOSE 的端口映射到宿主机随机端口
```

### 方式 3：通过宿主机网络 (host 模式)

```bash
# 容器直接使用宿主机网络栈，没有网络隔离
docker run -d --network host --name web nginx:alpine
# 现在直接访问 localhost:80 就是容器的 80 端口

# 优点：性能最高，无 NAT 开销
# 缺点：端口冲突风险，安全性降低
```

### 方式 4：容器间通过共享网络栈

```bash
# 新容器直接使用另一个容器的网络栈
docker run -d --name web nginx:alpine
docker run -it --name debug --network container:web alpine sh

# debug 容器中没有自己的网络，完全共享 web 的网络
# 在 debug 中 ifconfig 看到的是 web 的网络配置
```

## 6.5 端口映射详解

```bash
# 基本语法
-p <宿主机IP>:<宿主机端口>:<容器端口>/<协议>
-p <宿主机端口>:<容器端口>

# 示例
-p 8080:80                 # 所有接口上 8080 → 容器 80 (TCP)
-p 8080:80/tcp             # 同上，明确 TCP
-p 53:53/udp               # UDP 端口映射
-p 127.0.0.1:8080:80       # 仅本地 8080 → 容器 80
-p 8080-8090:8080-8090     # 范围端口映射

# 查看容器端口映射
docker port my-nginx
# 输出：80/tcp -> 0.0.0.0:8080
```

## 6.6 DNS 与网络故障排查

### Docker 内置 DNS

Docker 内置了一个 DNS 服务器（`127.0.0.11`），容器中的 `/etc/resolv.conf` 指向它。

```bash
# 查看容器 DNS 配置
docker exec my-container cat /etc/resolv.conf
```

### 常用排错命令

```bash
# 1. 测试连通性（从容器内）
docker exec web ping db

# 2. DNS 解析测试
docker exec web nslookup db
docker exec web dig db

# 3. 查看容器的网络配置
docker inspect -f '{{.NetworkSettings.IPAddress}}' web

# 4. 查看容器 hosts 文件
docker exec web cat /etc/hosts

# 5. 查看 iptables 规则（宿主机）
iptables -t nat -L -n | grep DOCKER

# 6. 使用 netshoot 进行网络调试
docker run --rm -it --network my-net nicolaka/netshoot
# 内置了常用的网络工具：ping, nslookup, curl, tcpdump, iperf 等
```

## 6.7 实战：WordPress + MySQL 双容器部署

```bash
# 1. 创建自定义网络
docker network create wordpress-net

# 2. 启动 MySQL
docker run -d \
  --name wp-db \
  --network wordpress-net \
  -e MYSQL_ROOT_PASSWORD=secret \
  -e MYSQL_DATABASE=wordpress \
  -e MYSQL_USER=wpuser \
  -e MYSQL_PASSWORD=wppass \
  -v wp-db-data:/var/lib/mysql \
  --restart unless-stopped \
  mysql:8.0

# 3. 启动 WordPress
docker run -d \
  --name wp-app \
  --network wordpress-net \
  -p 8080:80 \
  -e WORDPRESS_DB_HOST=wp-db:3306 \
  -e WORDPRESS_DB_USER=wpuser \
  -e WORDPRESS_DB_PASSWORD=wppass \
  -e WORDPRESS_DB_NAME=wordpress \
  -v wp-app-data:/var/www/html \
  --restart unless-stopped \
  wordpress:latest

# 4. 访问 http://localhost:8080 完成 WordPress 安装

# 网络隔离验证：
# - wp-db 的 3306 端口没有 -p 映射，外部无法直接访问
# - wp-app 通过容器名 "wp-db" 在自定义网络内访问数据库
# - 只有 wp-app 的 80 端口对外暴露
```

---

> **下一步**：[07-Dockerfile最佳实践](07-Dockerfile最佳实践.md)
