# 05-Docker 网络配置与通信
> Docker 网络对 Java 后端的核心价值是"简化网络配置、实现灵活通信、保障环境隔离"——4 种场景、自定义网络容器名通信、跨主机方案

## 📚 目录
1. [网络模式与 Java 后端关联逻辑](#1-网络模式与-java-后端关联逻辑)
2. [场景 1：外部访问容器内应用（端口映射）](#2-场景-1外部访问容器内应用端口映射)
3. [场景 2：容器内应用访问外部服务](#3-场景-2容器内应用访问外部服务)
4. [场景 3：容器间通信（自定义桥接网络）](#4-场景-3容器间通信自定义桥接网络)
5. [场景 4：跨宿主机容器通信](#5-场景-4跨宿主机容器通信)
6. [进阶痛点与解决方案](#6-进阶痛点与解决方案)
7. [最佳实践表](#7-最佳实践表)
8. [核心要点](#8-核心要点)
9. [参考来源](#9-参考来源)

## 1. 网络模式与 Java 后端关联逻辑

**核心认知**：Java 应用运行离不开网络——单体应用对外提供 HTTP 接口（外部访问）、微服务间相互调用（内部通信）、连接 MySQL/Redis 等中间件（跨容器/外部访问）。适配点是**"网络模式选择 → 端口配置 → 通信地址适配"**三者环环相扣。

**Docker 默认 3 种网络**（`docker network ls` 查看）：

| 网络模式 | 说明 | 适用场景 |
|----------|------|----------|
| bridge（桥接网络） | 默认网络模式，实现"容器与宿主机、容器与容器"通信 | Java 单体应用、中小型微服务 |
| host（主机网络） | 容器直接使用宿主机网络栈，无需端口映射 | 网络性能要求高、本地开发调试 |
| none（无网络） | 容器无网络配置 | 特殊隔离场景（几乎不用于 Java） |

> 💡 除默认网络外，Docker 支持**自定义网络**（桥接模式为主），是 Java 微服务间通信的核心方案：实现"容器名通信""网络隔离"，解决端口冲突、地址依赖问题。

## 2. 场景 1：外部访问容器内应用（端口映射）

**底层逻辑**：容器默认有独立网络 IP（bridge 模式下为 `172.17.0.x`），外部无法直接访问，需通过"宿主机 IP + 映射端口"转发。类比：容器是"独立房间"，需通过"宿主机大门（宿主机 IP）+ 房间门牌号（映射端口）"访问。

```bash
docker run -d -p 8080:8080 --name java-app java-app:1.0
```

访问方式：宿主机 `http://localhost:8080/xxx`；其他机器 `http://宿主机IP:8080/xxx`（需开放防火墙 8080）。

**Java 后端适配技巧与避坑**：

| 技巧/避坑 | 说明 |
|-----------|------|
| 端口冲突 | 提示 "port is already allocated" 时改宿主机映射端口（如 `-p 8081:8080`） |
| 绑定宿主机 IP（多网卡） | `docker run -d -p 192.168.1.100:8080:8080 java-app:1.0` |
| **应用绑定地址（最易踩的坑）** | Spring Boot 默认绑定 `0.0.0.0`（允许所有 IP）；若配置为 `127.0.0.1` 则仅容器内可访问，外部无法通过端口映射访问 |

## 3. 场景 2：容器内应用访问外部服务

### 3.1 访问宿主机服务（高频）

容器内 `localhost:3306` 指向容器自身而非宿主机，需用特殊地址：

| 系统 | 宿主机地址 | 示例 |
|------|-----------|------|
| Linux/Mac | `172.17.0.1`（Docker 桥接网络默认网关，`ip addr show docker0` 查看） | `jdbc:mysql://172.17.0.1:3306/db` |
| Windows（Docker Desktop） | `host.docker.internal`（Docker 提供的特殊地址） | `jdbc:mysql://host.docker.internal:3306/db` |

> ⚠️ 若宿主机服务绑定了 `127.0.0.1`，即使 IP 配置正确容器内也连不上——需修改宿主机服务绑定 `0.0.0.0`（如 MySQL 的 `bind-address = 0.0.0.0`）。

### 3.2 访问外部服务器服务（阿里云 RDS、第三方接口）

无需额外配置网络，默认 bridge 模式可访问外网，直接配置 IP+端口：

```text
jdbc:mysql://rm-xxx.mysql.aliyuncs.com:3306/db
```

> 💡 容器无法访问外网时：检查宿主机网络 + Docker 的 DNS 配置；DNS 解析失败可在启动时指定：`docker run -d -p 8080:8080 --dns 8.8.8.8 java-app:1.0`。

## 4. 场景 3：容器间通信（自定义桥接网络）

### 4.1 为什么不推荐默认 bridge（两个致命问题）

- **容器 IP 不固定**：重启后 IP 重新分配，配置固定容器 IP 会导致通信失败；
- **无网络隔离**：所有容器同一网络，可能端口冲突、通信干扰。

### 4.2 最佳方案：自定义桥接网络

支持"容器名通信"（无需依赖 IP），容器 IP 固定（重启后不变）：

```bash
# 1. 创建自定义桥接网络
docker network create java-microservice-network

# 2. 启动用户服务容器，加入自定义网络
docker run -d -p 8081:8081 --name user-service \
  --network java-microservice-network user-service:1.0

# 3. 启动订单服务容器，加入同一个自定义网络
docker run -d -p 8082:8082 --name order-service \
  --network java-microservice-network order-service:1.0
```

**Java 微服务适配代码**（用容器名而非 IP）：

```java
// 订单服务调用用户服务的 HTTP 接口
String userUrl = "http://user-service:8081/api/user/getById?id=1";
String userInfo = restTemplate.getForObject(userUrl, String.class);
```

> 🎯 **核心原理**：Docker 自定义网络自动维护"容器名 → IP"映射表，容器间可通过容器名直接解析 IP，且容器重启后映射关系不变。

### 4.3 微服务与中间件容器通信

```bash
# 启动 MySQL 容器，加入自定义网络
docker run -d -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=123456 \
  --name mysql \
  --network java-microservice-network \
  mysql:8.0
```

Java 配置：`jdbc:mysql://mysql:3306/db_name?useSSL=false`（直接使用容器名 `mysql` 作为地址）。

## 5. 场景 4：跨宿主机容器通信

**方案 1：端口映射 + 宿主机 IP（中小型分布式项目）**

```bash
# 宿主机 A（192.168.1.100）
docker run -d -p 8081:8081 --name user-service user-service:1.0
# 宿主机 B（192.168.1.101）
docker run -d -p 8082:8082 --name order-service order-service:1.0
# 订单服务调用：http://192.168.1.100:8081/api/user/getById
```

优势：配置简单无需额外搭建网络；劣势：端口管理繁琐、不利于扩展。

**方案 2：Docker Swarm / K8s 网络（大型分布式项目）**

- 核心逻辑：Swarm/K8s 创建"跨宿主机的虚拟网络"，所有宿主机容器加入该网络，通过容器名/服务名直接通信，无需关注宿主机 IP 和端口映射（详见 [09](09-多主机与Swarm集群.md)）；
- Java 适配：微服务调用无需改代码，部署到集群后用服务名调用，契合 Spring Cloud 服务发现机制（Eureka、Nacos）。

## 6. 进阶痛点与解决方案

**痛点 1：连接外部服务"连接超时"**

| 原因 | 解决方案 |
|------|----------|
| 宿主机防火墙/安全组未开放端口 | 开放对应端口（Linux：`firewall-cmd --add-port=3306/tcp --permanent`） |
| Java 应用配置的地址/端口错误 | `docker exec -it 容器名 ping 目标IP` 测试连通性；`telnet 目标IP 端口` 测试端口 |
| DNS 解析失败 | 启动时指定 DNS：`--dns 8.8.8.8`，或修改 Docker 全局 DNS 配置 |

**痛点 2：微服务间"服务不可达"（Connection refused）**

| 原因 | 解决方案 |
|------|----------|
| 容器未加入同一自定义网络 | `docker network inspect 网络名` 查看；未加入则 `docker network connect 网络名 容器名` |
| 微服务未启动成功或端口配置错误 | `docker logs 容器名` 查看启动日志 |
| 调用地址错误 | 必须用**"容器名+容器内端口"**而非宿主机映射端口（如 `user-service:8081`，而非 `user-service:8080`） |

**痛点 3：容器重启后通信失败**

- 优先用自定义网络，启动时用 `--name` 指定容器名（映射固定）；
- 结合 Spring Cloud 服务发现（如 Nacos），通过服务名获取地址，彻底解决地址依赖。

**痛点 4：网络占用过多资源导致性能下降**

- 高并发场景用 host 网络模式消除转发开销（注意 host 模式下避免端口冲突）；
- 限制容器网络连接数；定期清理无用的 Docker 网络和容器。

## 7. 最佳实践表

| 场景 | 推荐方案 |
|------|----------|
| 开发环境 | host 网络模式，无需配置端口映射，简化调试 |
| 测试/生产（单体应用） | bridge 模式 + 端口映射，绑定宿主机内网 IP，提升安全性 |
| 测试/生产（微服务） | 自定义桥接网络，通过容器名通信；中间件也加入该网络 |
| 分布式微服务（多宿主机） | 小型项目用"端口映射+宿主机 IP"，大型项目用 K8s 网络 |
| 网络调试 | `docker exec -it 容器名 ping 目标IP` → `telnet 目标IP 端口` → 查看容器日志 |
| 安全性配置 | 生产禁止 host 模式；自定义网络控制访问权限；仅开放必要端口且限制访问 IP |

## 8. 核心要点

> 🎯 **核心要点**：
> - 三种网络模式：bridge（默认，端口映射）、host（无隔离，性能）、none（无网络）；
> - 容器访问宿主机：Linux 用 `172.17.0.1`、Windows 用 `host.docker.internal`；
> - 微服务间通信**必须用自定义网络 + 容器名**（IP 不稳定 + 无隔离是默认 bridge 两大硬伤）；
> - 调用地址永远用"容器名 + 容器内端口"，不是宿主机映射端口；
> - 排查三板斧：ping（连通）→ telnet（端口）→ docker logs（应用层）。

## 9. 参考来源

- [Docker 网络官方文档](https://docs.docker.com/engine/network/)
- [Docker 网络模式详解（bridge/host/overlay）](https://docs.docker.com/network/)
- [Docker bridge 网络驱动文档](https://docs.docker.com/engine/network/drivers/bridge/)

---

**下一模块**：[06-存储卷与数据持久化](06-存储卷与数据持久化.md)　/　**返回总览**：[00-总览](00-Docker知识体系总览.md)
