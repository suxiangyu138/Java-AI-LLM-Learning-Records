# 05 - Java 后端上云实战

> 🎯 从"本地跑通"到"云上可用" — Spring Boot 项目上云五步：服务器选型 → 环境配置 → 中间件选型 → 微服务上云 → 部署发布。Java 后端的云上最佳实践

---

## 目录

1. [上云五步全景](#1-上云五步全景)
2. [服务器选型与开通](#2-服务器选型与开通)
3. [Java 环境配置](#3-java-环境配置)
4. [中间件选型](#4-中间件选型)
5. [Spring Boot 部署](#5-spring-boot-部署)
6. [微服务上云架构](#6-微服务上云架构)

---

## 1. 上云五步全景

```text
① 服务器选型 → ② 环境配置 → ③ 中间件 → ④ 部署 → ⑤ 发布

① 服务器：按预算/规格选 ECS/CVM/轻量
② 环境：JDK 21 + Maven + 基础工具
③ 中间件：RDS/Redis/MQ（托管或自建）
④ 部署：jar 运行 / Docker / K8s
⑤ 发布：域名 + HTTPS + 备案 + 监控
```

---

## 2. 服务器选型与开通

### 2.1 按场景选配置

| 场景 | 配置建议 | 参考价格 |
|------|----------|:---:|
| 个人学习/轻量应用 | 2核2G（轻量服务器） | 99-199 元/年 |
| 小型业务 | 2核4G | 300-600 元/年 |
| 中型业务 | 2核8G/4核8G | 600-1200 元/年 |
| 高并发核心 | 8核16G+ 负载均衡 | 按需 |

### 2.2 开通步骤（以阿里云为例）

```bash
# ① 控制台 → ECS → 创建实例
#    地域：选择离用户近的（华东/华南/华北）
#    镜像：Ubuntu 22.04 / CentOS / Windows
#    配置：CPU/内存/带宽/数据盘

# ② 安全组放行端口
#    22 (SSH) / 80 (HTTP) / 443 (HTTPS) / 8080 (应用)

# ③ SSH 连接
ssh root@<公网IP>

# ④ 安全加固
#    修改 root 密码 / 创建新用户 / 密钥登录 / 关闭密码登录
```

---

## 3. Java 环境配置

```bash
# Ubuntu 上安装 JDK 21（LTS）
sudo apt update
sudo apt install -y openjdk-21-jdk

# 验证
java -version    # openjdk version "21.0.x"

# 安装 Maven
sudo apt install -y maven

# 安装 Docker（可选，推荐）
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# 安装 Nginx（反向代理）
sudo apt install -y nginx
```

**生产 JVM 参数：**

```bash
java -Xms512m -Xmx1g -XX:+UseZGC \
     -XX:MaxGCPauseMillis=50 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/app/ \
     -jar app.jar
```

---

## 4. 中间件选型

### 4.1 托管 vs 自建

| 对比 | 托管（RDS/Redis 服务） | 自建（ECS 上装） |
|------|:---:|:---:|
| 运维成本 | 零（自动备份/监控/升级） | 高（自己管） |
| 成本 | 略高 | 低（服务器复用） |
| 高可用 | ✅ 内置主备 | 需自己搭 |
| 适合 | **生产环境** | 学习/测试 |

### 4.2 中间件选型表

| 需求 | 阿里云 | 腾讯云 | 华为云 | 自建方案 |
|------|--------|--------|--------|----------|
| 关系库 | RDS MySQL / PolarDB | TDSQL-C | RDS MySQL | MySQL 8 + 主备 |
| 缓存 | Redis（Tair） | Redis | DCS | Redis 7 |
| 消息 | RocketMQ / Kafka | CKafka | DMS | RocketMQ |
| 注册配置 | MSE（Nacos） | TSE | CSE | Nacos 2.x |
| 对象存储 | OSS | COS | OBS | MinIO |

---

## 5. Spring Boot 部署

### 5.1 传统 jar 部署

```bash
# ① 本地构建
mvn clean package -DskipTests

# ② 上传
scp target/app.jar root@<IP>:/opt/app/

# ③ 启动（systemd 管理）
# /etc/systemd/system/app.service
[Service]
ExecStart=/usr/bin/java -jar /opt/app/app.jar
Restart=always
Environment=SPRING_PROFILES_ACTIVE=prod
```

### 5.2 Docker 部署（推荐）

```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/app.jar .
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
docker build -t my-app .
docker run -d --name app -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  --restart=always my-app
```

### 5.3 Nginx 反向代理 + HTTPS

```nginx
server {
    listen 80;
    server_name api.example.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
# 再用 certbot 申请免费 HTTPS 证书
```

---

## 6. 微服务上云架构

```text
Java 微服务云上架构（以阿里云为例）：

┌─ 接入层 ──────────────────────────┐
│ SLB 负载均衡 / API 网关           │
├─ 应用层 ──────────────────────────┤
│ ECS 集群（多个微服务实例）         │
│   order-service / user-service    │
│   stock-service / payment-service │
├─ 中间件层 ────────────────────────┤
│ MSE Nacos（注册配置中心）          │
│ RDS MySQL / PolarDB（数据库）     │
│ Redis（缓存/分布式锁）             │
│ RocketMQ（消息/削峰）              │
│ Seata（分布式事务）                │
├─ 数据层 ──────────────────────────┤
│ OSS（对象存储）                    │
│ Elasticsearch（搜索）              │
└─ 运维层 ──────────────────────────┘
│ 云监控 / 日志服务 / 链路追踪        │
│ CI/CD（流水线 + 容器服务 ACK）      │
```

**上云避坑清单：**

| 坑 | 对策 |
|----|------|
| 安全组忘放端口 | 开通实例时确认 8080 等端口 |
| 数据库裸奔公网 | RDS 只内网访问 |
| 备份缺失 | 开启 RDS/Redis 自动备份 |
| 成本失控 | 设置预算告警 + 预留实例 |
| 域名未备案 | 国内服务器必须 ICP 备案（10-20 天） |
| 单点故障 | 至少 2 台 ECS + 负载均衡 |

---

> 🎯 **核心要点**：Java 后端上云五步 — **① 服务器（轻量 2核2G 起步）② JDK 21 + Docker ③ 中间件托管（RDS/Redis）④ Docker 部署 + systemd ⑤ Nginx + HTTPS**。生产必做：安全组、自动备份、预算告警、域名备案、至少双机 + 负载均衡。Java 微服务选阿里云中间件兼容最佳。

**下一模块**：[06-大模型AI平台开发实战](06-大模型AI平台开发实战.md) / **返回总览**：[00-总览](00-三大云厂商知识体系总览.md)
