# 05 - Java 后端上云实战

> **核心摘要**：从「本地跑通」到「云上可用」——Spring Boot 项目上云五步：服务器选型 → 环境配置 → 中间件选型 → 部署发布 → 安全加固。本文件是 Java 后端的云上最佳实践（以阿里云为例，三家差异标注）。

> **前置阅读**：[[04-三厂商核心对比]]

---

## 📚 目录

1. [上云五步全景](#1-上云五步全景)
2. [服务器选型与开通](#2-服务器选型与开通)
3. [Java 环境配置](#3-java-环境配置)
4. [中间件选型](#4-中间件选型)
5. [Spring Boot 部署](#5-spring-boot-部署)
6. [微服务上云架构](#6-微服务上云架构)
7. [安全加固](#7-安全加固)
8. [上云检查清单](#8-上云检查清单)
9. [核心要点](#9-核心要点)

---

## 1. 上云五步全景

> **背景**：Java 后端上云 = 从「本机可跑」到「云上可用、可维护、可扩展」。
> **目的**：建立完整的上云路径（不只是部署）。
> **适用范围**：Spring Boot/微服务项目的云上部署。
> **前提假设**：本地已跑通（上云解决「环境与运维」而非「代码」）。

```text
上云五步
┌─────────────────────────────────────────────┐
│ ① 服务器选型：规格/地域/计费/网络            │
│ ② 环境配置：JDK/构建工具/部署目录/环境变量   │
│ ③ 中间件选型：数据库/缓存/MQ（托管 or 自建） │
│ ④ 部署发布：打包/部署脚本/进程管理/灰度      │
│ ⑤ 安全加固：防火墙/密钥/监控/备份            │
└─────────────────────────────────────────────┘
→ 金句：上云不是「换个地方跑」——是「按云的方式运行」
```

---

## 2. 服务器选型与开通

### 2.1 选型决策

```text
服务器选型（Java 后端）
├── ① 规格：
│   ├── 入门/个人：2C4G（轻量服务器）
│   ├── 中小服务：4C8G（标准型）
│   ├── 生产：8C16G+（视并发）
│   └── 高并发：多节点 + 负载均衡
├── ② 地域：就近原则（用户集中地）
│   ├── 华东（上海）/华南（广州）/华北（北京）
├── ③ 计费：包年包月（生产）/按量（弹性）
├── ④ 镜像：CentOS/Ubuntu/Alinux（阿里）
├── ⑤ 网络：公网带宽 + 安全组
└── ⑥ 密钥：SSH 密钥对（禁密码）

开通流程（以阿里云为例）
├── ① 控制台 → 云服务器 ECS → 创建实例
├── ② 选地域/规格/镜像/网络/安全组
├── ③ 设置登录（密钥对）
├── ④ 绑定弹性 IP（EIP）
└── ⑤ SSH 登录验证
```

### 2.2 安全组配置

```text
安全组（云防火墙）——端口放行
├── ① 必须放行：22（SSH 限源）/80/443
├── ② 业务端口：8080（可限源或经 Nginx）
├── ③ 数据库：不对公网（内网访问）
├── ④ 管理端口：限办公 IP
└── ⑤ 原则：最小化（只开必要的）
→ 详见本仓库「端口号与程序员常识」系列
```

---

## 3. Java 环境配置

### 3.1 环境安装

```bash
# ① 安装 JDK（以 Ubuntu 为例）
sudo apt update
sudo apt install openjdk-17-jdk    # 或 21（LTS）
java -version

# ② 构建工具
sudo apt install maven              # Maven
# 或 Gradle（手动安装）

# ③ 环境变量（/etc/profile 或 ~/.bashrc）
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# ④ 部署目录规划
mkdir -p /opt/app            # 应用目录
mkdir -p /opt/app/logs       # 日志
mkdir -p /opt/app/backup     # 备份
```

### 3.2 多环境配置

```text
云上多环境（dev/test/prod）
├── ① Spring Profile：application-prod.yml
├── ② 配置外部化：环境变量注入（见配置文件系列）
│   export SPRING_PROFILES_ACTIVE=prod
│   export DB_URL=jdbc:mysql://...
│   export DB_PASSWORD=...
├── ③ 敏感信息：不走配置文件（环境变量/密钥管理）
└── ④ 日志：统一目录 + 轮转（logrotate）

启动脚本（部署模板）
#!/bin/bash
# /opt/app/start.sh
export SPRING_PROFILES_ACTIVE=prod
nohup java -jar /opt/app/order-service.jar \
    --server.port=8080 \
    > /opt/app/logs/app.log 2>&1 &
echo $! > /opt/app/app.pid
```

---

## 4. 中间件选型

### 4.1 托管 vs 自建

| 中间件 | 托管（推荐） | 自建（成本低） |
|--------|:---:|:---:|
| MySQL | 云数据库 RDS | ECS 自装 |
| Redis | 云 Redis/Tair | ECS 自装 |
| MQ | 云消息队列 | ECS 自装 |
| ES | 云 ES | ECS 自装 |
| 优势 | 高可用/备份/监控 | 便宜/可控 |
| 劣势 | 贵 | 运维成本 |

```text
选型建议（Java 后端）
├── ① 生产：托管（用成本换运维时间）
│   ├── RDS（高可用 + 备份 + 监控）
│   ├── 云 Redis（主备 + 持久化策略）
│   └── 云 MQ（消息可靠性保障）
├── ② 个人/学习：ECS 自装（Docker 部署）
├── ③ 混合：核心用托管 + 辅助自建
└── ④ 金句：托管 = 把「运维时间」换成「开发时间」
```

### 4.2 连接配置（Spring Boot）

```yaml
# application-prod.yml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:3306/order_db
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  data:
    redis:
      host: ${REDIS_HOST}
      port: 6379
      password: ${REDIS_PASSWORD}
# 环境变量部署时注入（见配置文件系列）
```

---

## 5. Spring Boot 部署

### 5.1 打包与发布

```bash
# ① 打包（本地/CI）
mvn clean package -DskipTests
# → target/order-service.jar

# ② 上传（scp/rsync——见 SSH 系列）
rsync -avz target/order-service.jar deploy@server:/opt/app/

# ③ 启动
ssh deploy@server 'bash /opt/app/start.sh'

# ④ 验证
curl http://server:8080/actuator/health
```

### 5.2 进程管理（systemd）

```ini
# /etc/systemd/system/order-service.service
[Unit]
Description=Order Service
After=network.target

[Service]
User=app
Environment=SPRING_PROFILES_ACTIVE=prod
Environment=DB_PASSWORD=xxx
ExecStart=/usr/bin/java -jar /opt/app/order-service.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now order-service
sudo systemctl status order-service
sudo journalctl -u order-service -f    # 日志
```

### 5.3 部署策略（2026）

```text
生产部署策略
├── ① 简单部署：systemd（单机）
├── ② 蓝绿部署：新旧版本并行（切流量）
├── ③ 滚动发布：集群逐个更新
├── ④ 灰度：小流量验证（云负载均衡支持）
├── ⑤ 容器化：Docker + TKE/ACK（云 K8s）
└── ⑥ CI/CD：云效（阿里）/CODING（腾讯）等
→ 金句：先 systemd 跑通，再容器化规模化
```

---

## 6. 微服务上云架构

### 6.1 架构拓扑

```text
Java 微服务上云（阿里云示例）
┌─────────────────────────────────────────────┐
│ 入口层：SLB（负载均衡）→ 80/443              │
├─────────────────────────────────────────────┤
│ 应用层：ECS 集群 × N（Spring Cloud/微服务）   │
│   ├── 网关服务（Gateway）                    │
│   ├── 业务服务（order/user/payment）         │
│   └── 注册中心（Nacos——云上可用）            │
├─────────────────────────────────────────────┤
│ 数据层：RDS（MySQL）+ Tair（Redis）+ 云 MQ   │
├─────────────────────────────────────────────┤
│ 治理层：ARMS（监控）+ 日志服务 + 云效（CI/CD）│
└─────────────────────────────────────────────┘
```

### 6.2 上云要点

```text
微服务上云要点
├── ① 注册中心：Nacos 云上部署（或使用云服务）
├── ② 配置中心：Nacos Config（环境隔离）
├── ③ 网关：Spring Cloud Gateway（云 SLB 前置）
├── ④ 可观测：ARMS（链路追踪）+ 日志服务
├── ⑤ 弹性：容器化后 HPA 自动扩缩容
├── ⑥ 容灾：多可用区（AZ）部署
└── ⑦ 金句：微服务上云 = 微服务本身 + 云原生能力（弹性/观测）
```

---

## 7. 安全加固

### 7.1 服务器安全

```text
云服务器安全加固（上线前）
├── ① SSH：密钥认证 + 禁密码 + 改端口 + fail2ban
├── ② 防火墙：安全组最小化（限源）
├── ③ 系统更新：安全补丁（定期）
├── ④ 非 root 运行：应用专用用户（app）
├── ⑤ 密钥管理：数据库密码等用环境变量/云 KMS
├── ⑥ 高危端口：6379/3306 等绝不公网（见端口系列）
└── ⑦ 备份：数据定期备份（云快照/OSS）

数据库安全
├── ① 私有子网（无公网路由）
├── ② 白名单：只允许应用服务器 IP
├── ③ 强密码 + 最小权限账号
├── ④ 备份策略：自动备份 + 定期恢复演练
└── ⑤ 金句：数据库 = 数据大门——网络隔离 + 白名单双保险
```

### 7.2 应用安全

```text
应用层安全（云上）
├── ① HTTPS：证书（云 SSL 证书服务）
├── ② WAF：云 WAF（防注入/攻击）
├── ③ 密钥：云 KMS（加密存储）
├── ④ 日志：敏感信息脱敏
├── ⑤ 依赖：SCA 扫描（漏洞依赖）
└── ⑥ 备份：代码 + 配置 + 数据三层备份
```

---

## 8. 上云检查清单

```text
Java 后端上云检查清单（上线前）
[ ] 服务器：规格够用？地域就近？密钥登录？
[ ] 安全组：只开必要端口？SSH 限源？
[ ] JDK：版本正确？JAVA_HOME 配置？
[ ] 配置：多环境（dev/prod）？敏感信息环境变量？
[ ] 中间件：RDS/Redis 白名单？高可用？
[ ] 部署：systemd 托管？启动脚本？日志轮转？
[ ] 健康检查：/actuator/health 可用？
[ ] 安全：SSH 加固？数据库私有子网？HTTPS？
[ ] 监控：日志采集？告警配置？
[ ] 备份：数据库备份 + 恢复演练？
[ ] 文档：部署手册 + 端口清单 + 运维交接？
→ 11 项全过 = 可上线；缺项 = 补完再上
```

---

## 9. 核心要点

> 🎯 **核心要点**：
> 1. 上云五步：服务器选型 → 环境配置 → 中间件选型 → 部署发布 → 安全加固
> 2. 托管 vs 自建：生产用托管（高可用/备份/监控）——用成本换运维时间
> 3. 部署链路：打包（mvn）→ 上传（rsync）→ systemd 托管 → 健康检查验证
> 4. 多环境：Spring Profile + 环境变量注入（敏感信息不进文件）
> 5. 微服务上云：SLB + 网关 + Nacos + 可观测 + 弹性（容器化）
> 6. 安全三件套：SSH 加固 + 安全组最小化 + 数据库私有子网白名单
> 7. 上云检查清单 11 项——全过再上线

---

**下一模块**：[06-大模型AI平台开发实战](06-大模型AI平台开发实战.md) | **返回总览**：[00-三大云厂商知识体系总览](00-三大云厂商知识体系总览.md)
