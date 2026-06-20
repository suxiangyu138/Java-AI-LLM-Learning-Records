# 苏巷雨 · 开发环境全量参考手册

> 最后更新：2026-06-21 | 机器：Windows 11 Home China (10.0.26200) | CPU：i5-13500H 16核 | 内存：16GB | D盘：331G已用 / 420G空闲

---

## 〇、架构全景图

### 一个典型的 Java 后端 + AI 项目请求链路

```
用户/前端
    │
    ▼
┌─────────────────────────────────────────────────────┐
│  Nginx :80                                            │
│  → 反向代理 / 静态资源 / HTTPS                          │
└─────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────┐
│  Spring Boot (Tomcat :8080)                           │
│  → 业务逻辑层                                          │
│  ┌──────────┬──────────┬──────────┬──────────────┐   │
│  │ Spring   │ Spring   │ MyBatis  │ Spring AI    │   │
│  │ MVC      │ Security │ -Plus    │ (Ollama)     │   │
│  │ (接口)   │ (认证)   │ (数据库) │ (AI调用)     │   │
│  └──────────┴──────────┴──────────┴──────────────┘   │
└─────────────────────────────────────────────────────┘
    │          │          │           │
    ▼          ▼          ▼           ▼
┌────────┐ ┌──────┐ ┌──────────┐ ┌──────────┐
│ MySQL  │ │Redis │ │RabbitMQ/ │ │ Ollama   │
│ :3306  │ │:6379 │ │Kafka/RM  │ │ :11434   │
│ 持久化  │ │ 缓存  │ │ 异步消息  │ │本地模型  │
└────────┘ └──────┘ └──────────┘ └──────────┘
    │                      │           │
    ▼                      ▼           ▼
┌────────┐          ┌──────────┐ ┌──────────┐
│MongoDB │          │Nacos     │ │ChromaDB  │
│文档存储 │          │注册/配置  │ │向量检索   │
│:27017  │          │:8848     │ │(RAG)     │
└────────┘          └──────────┘ └──────────┘
    │
    ▼
┌─────────────────────────────────────────────────────┐
│  搜索引擎: Elasticsearch :9200                        │
│  流量控制: Sentinel :8089                             │
│  分布式事务: Seata :8091                              │
│  链路追踪: SkyWalking / Arthas / Prometheus           │
└─────────────────────────────────────────────────────┘
```

### 项目中的分工表

| 层 | 技术 | 一句话作用 |
|----|------|-----------|
| **入口** | Nginx | 请求的第一道门：转发、限流、静态文件 |
| **业务** | Spring Boot | 所有业务逻辑的"胶水框架" |
| **数据** | MySQL | 订单、用户、商品——核心业务数据 |
| **加速** | Redis | 热点数据缓存、分布式锁、验证码过期 |
| **文档** | MongoDB | 日志、评论、非结构化数据 |
| **搜索** | Elasticsearch | 商品搜索、自动补全、日志查询 |
| **消息** | RabbitMQ | 下单后异步发短信、减库存 |
| **流式** | Kafka | 用户行为埋点、实时日志收集 |
| **治理** | Nacos | 微服务互相发现对方在哪 |
| **防护** | Sentinel | 接口限流防刷、服务熔断 |
| **事务** | Seata | 跨多个数据库/服务的统一提交 |
| **监控** | Arthas/Prometheus | 线上问题排查、性能看板 |
| **AI** | Ollama + Spring AI | 智能问答、文档分析、代码生成 |

---

## 一、系统基础信息

### 1.1 硬件

| 项目 | 详情 |
|------|------|
| CPU | 13th Gen Intel Core i5-13500H (16核) |
| 内存 | 16GB (可用约15GB) |
| 磁盘D | 751GB总量，420GB空闲 |
| 显卡 | 集成显卡 |

### 1.2 JDK 版本

| 版本 | 路径 | 用途 |
|------|------|------|
| JDK 8 | `D:\JDK8` | 遗留项目/工具兼容 |
| JDK 17 | `D:\JDK17` | Kafka 等需要 LTS 旧版本的服务 |
| JDK 21 | `D:\JDK21` | 备选 LTS |
| JDK 25 | `D:\JDK25` | **主开发环境**（JAVA_HOME） |

### 1.3 核心工具链

| 工具 | 版本 | 路径 |
|------|------|------|
| Maven | 3.9.16 | `D:\maven` |
| Node.js | 22.14.0 | nvm4w 管理 |
| Git | — | `D:\Git` |
| 7-Zip | 26.01 | `D:\7-Zip` |
| IDEA | 2026.1.2 | `D:\IDEA` |
| PyCharm | 2026.1.2 | `D:\PyCharm` |
| VS Code | — | `D:\Microsoft VS Code` |
| Cursor | — | `D:\cursor` |
| Oh My Posh | 29.17.0 | `winget` 安装，终端美化 |

### 1.4 数据库客户端

| 工具 | 路径 |
|------|------|
| DBeaver | `D:\DBeaver` |
| Navicat Premium Lite 17 | `D:\Navicat Premium Lite 17` |
| FinalShell | `D:\FinalShell` |
| ApiFox | `D:\ApiFox` |
| Postman | `D:\Postman` |

### 1.5 诊断与性能工具

| 工具 | 路径 | 用途 |
|------|------|------|
| **Arthas** | `D:\Tools\arthas\` | Java 运行时诊断（阿里开源） |

**Arthas 快速使用**

```powershell
# 启动 Arthas，选择目标 Java 进程
java -jar D:\Tools\arthas\arthas-boot.jar

# 常用命令
dashboard              # 实时面板（CPU/内存/GC/线程）
thread -n 3            # 最忙的3个线程
jad com.example.MyClass  # 反编译类
trace com.example.Service method  # 方法调用追踪
watch com.example.DAO getById returnObj  # 查看方法返回值
```

**Oh My Posh 启用（可选）**

```powershell
# 编辑 PowerShell 配置文件
notepad $PROFILE
# 添加这行：
oh-my-posh init pwsh | Invoke-Expression
# 重新打开终端即可生效
```

---

## 二、开发服务全量清单

### 2.1 服务概览

```
开机自启（Windows Service，4个）：
  MySQL 8.0.46      :3306
  Redis             :6379
  MongoDB           :27017
  RabbitMQ 3.12.13  :5672  (管理后台 :15672)

手动启动（一键脚本，10个）：
  Elasticsearch 9.4.2  :9200
  Nginx 1.30.2         :80
  Tomcat 9             :8080
  Sentinel Dashboard   :8089
  Nacos                :8848
  Seata 2.6.0          :8091
  Zookeeper            :2181
  Kafka 3.6.0          :9092
  RocketMQ NS 5.5.0    :9876
  RocketMQ Broker      :10911
```

### 2.2 一键启动脚本

```powershell
cd D:\本地学习项目\Java-AI-LLM-Learning-Records

.\dev-start-all.ps1           # 启动所有手动服务
.\dev-start-all.ps1 -status   # 查看全部15个服务状态
.\dev-start-all.ps1 -stop     # 停止所有手动服务
```

---

## 三、核心数据库服务

### 3.1 MySQL 8.0.46

**基本信息**

| 项目 | 值 |
|------|-----|
| 安装路径 | `D:\MySQL\MySQL Server 8.0\` |
| 配置文件 | `D:\MySQL\ProgramData\MySQL Server 8.0\my.ini` |
| 数据目录 | `D:/MySQL/ProgramData/MySQL Server 8.0/Data` |
| 启动方式 | Windows Service `MySQL80`，开机自启 |
| 连接地址 | `localhost:3306` |
| 账号密码 | `root / 123456` |
| 认证插件 | `mysql_native_password` |
| 字符集 | 默认 UTF-8 |

**关键配置**

```ini
# 连接
port=3306
bind-address=127.0.0.1
max_connections=151

# 存储引擎
default-storage-engine=INNODB
innodb_buffer_pool_size=128M
innodb_file_per_table=1

# 日志
log-bin=SUXIANGYU138-bin
slow-query-log=1
long_query_time=10
```

**已有数据库（29个）**

| 数据库 | 大小 | 所属项目 |
|--------|------|----------|
| flavor_dash | 5.6MB | FlavorDash 餐饮SaaS |
| suguangmall | 1.0MB | 微服务商城 |
| sky_take_out | 0.3MB | 苍穹外卖 |
| medical_db | 2.8MB | 小智医疗 |
| blog_db | 0.3MB | 学习博客 |
| mall4cloud_* | ~3MB | 微服务商城（多模块） |

**Spring Boot 集成**

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/your_database?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
```

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>
```

**常用命令**

```powershell
# 客户端连接
D:\MySQL\MySQL Server 8.0\bin\mysql.exe -u root -p -h 127.0.0.1 -P 3306

# 服务管理（管理员权限）
Get-Service MySQL80 | Start-Service
Get-Service MySQL80 | Stop-Service
Get-Service MySQL80 | Restart-Service
```

**在项目中的角色**

| 维度 | 说明 |
|------|------|
| **存什么** | 用户表、订单表、商品表、配置表——所有结构化业务数据 |
| **为什么用它** | ACID 事务保证数据一致性；InnoDB 行锁支持高并发写；生态成熟 |
| **和 MongoDB 的区别** | MySQL 存"表单数据"（结构固定），MongoDB 存"文档数据"（结构多变） |
| **在项目里的位置** | Service 层通过 MyBatis/JPA 操作，是业务逻辑的"数据大脑" |
| **典型面试场景** | 索引优化（Explain）、事务隔离级别、MVCC 原理、分库分表 |

---

### 3.2 Redis

**基本信息**

| 项目 | 值 |
|------|-----|
| 安装路径 | `D:\Redis\` |
| 配置文件 | `D:\Redis\redis.windows-service.conf` |
| 持久化文件 | `D:\Redis\dump.rdb` |
| 启动方式 | Windows Service `Redis`，开机自启 |
| 连接地址 | `localhost:6379` |
| 密码 | 无（开发环境） |

**关键配置**

```
bind 127.0.0.1
port 6379
# requirepass （未设置）
```

**Spring Boot 集成**

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:           # 无密码
      database: 0
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

**常用命令**

```powershell
# 客户端连接
D:\Redis\redis-cli.exe -h 127.0.0.1 -p 6379

# 测试
PING                     # 返回 PONG
INFO server              # 服务器信息
KEYS *                   # 列出所有 key
FLUSHALL                 # 清空所有数据
```

**在项目中的角色**

| 维度 | 说明 |
|------|------|
| **核心能力** | 内存级读写（微秒级）、数据过期、发布订阅、分布式锁 |
| **为什么必须有它** | 数据库每秒撑几千 QPS 是极限，Redis 轻松 10万+ QPS；高频读必须靠缓存挡在前面 |
| **典型模式** | 查询前先查 Redis → 没命中再查 MySQL → 结果写入 Redis（Cache Aside） |
| **和消息队列的区别** | Redis 适合"轻量异步"（几十条/秒），MQ 适合"可靠异步"（不丢消息） |
| **典型面试场景** | 缓存穿透/击穿/雪崩、分布式锁实现、Redis 单线程为什么快、持久化 RDB vs AOF |

---

### 3.3 MongoDB

**基本信息**

| 项目 | 值 |
|------|-----|
| 安装路径 | `D:\MongoDB\` |
| 配置文件 | `D:\MongoDB\bin\mongod.cfg` |
| 数据目录 | `D:\MongoDB\data` |
| 日志目录 | `D:\MongoDB\log` |
| 启动方式 | Windows Service `MongoDB`，开机自启 |
| 连接地址 | `localhost:27017` |
| 认证 | 无（开发环境） |

**关键配置**

```yaml
storage:
  dbPath: D:\MongoDB\data
net:
  port: 27017
  bindIp: 127.0.0.1
# security 段注释掉，无认证
```

**Spring Boot 集成**

```yaml
spring:
  data:
    mongodb:
      host: localhost
      port: 27017
      database: your_db
      # username:  # 无需认证
      # password:
```

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

**连接客户端**

- IDEA 内置 MongoDB 插件
- Navicat Premium Lite
- DBeaver
- Java 驱动直连（无命令行客户端，无需安装）

**在项目中的角色**

| 维度 | 说明 |
|------|------|
| **存什么** | 用户行为日志、评论、动态 Feed、物联网事件——非固定结构的数据 |
| **和 MySQL 的区别** | MySQL 需要先定义表结构才能写入；MongoDB 直接写入 JSON，字段随时可加 |
| **典型场景** | App 用户每次点击/浏览产生的埋点数据，结构复杂多变，存 MongoDB 比 MySQL 灵活 10 倍 |
| **AI 方向价值** | RAG 应用中，MongoDB 可存储文档原文和管理元数据，与向量库配合使用 |

---

## 四、消息中间件

### 4.1 RabbitMQ 3.12.13

**基本信息**

| 项目 | 值 |
|------|-----|
| 安装路径 | `D:\RabbitMQ Server\rabbitmq_server-3.12.13\` |
| 启动方式 | Windows Service `RabbitMQ`，开机自启 |
| AMQP 端口 | `localhost:5672` |
| 管理后台 | `http://localhost:15672` |
| 管理员账号 | `admin / admin123` |
| 默认账号 | `guest / guest` |
| VHost | `/` (默认)、`flavor` |
| Erlang 版本 | OTP 26.2.1 |

**Spring Boot 集成**

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: admin
    password: admin123
    virtual-host: flavor
    listener:
      simple:
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 5000
```

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

**管理命令**

```powershell
# 命令行管理（可能因 Erlang cookie 问题失败，优先用 HTTP API）
D:\RabbitMQ Server\rabbitmq_server-3.12.13\sbin\rabbitmqctl.bat status

# 推荐：用 HTTP API 管理
curl -u admin:admin123 http://localhost:15672/api/overview
curl -u admin:admin123 http://localhost:15672/api/queues
```

**在项目中的角色**

| 维度 | 说明 |
|------|------|
| **核心能力** | 可靠异步消息、复杂路由（Direct/Topic/Fanout/Headers）、消息确认+持久化 |
| **解决什么问题** | 把"不需要立即完成的操作"从主流程剥离。比如用户下单后，发短信/减库存/记日志全部异步，接口 200ms 内返回 |
| **和 Kafka 的区别** | RabbitMQ 走"经纪人模式"（精细路由控制），Kafka 走"日志模式"（高吞吐顺序消费） |
| **什么时候选它** | 业务消息（订单、支付通知）、延迟任务、需要 ACK+重试的可靠场景 |
| **典型面试场景** | 消息可靠性（确认/持久化）、死信队列、消息积压排查、幂等消费 |

---

### 4.2 Kafka 3.6.0

**基本信息**

| 项目 | 值 |
|------|-----|
| 安装路径 | `D:\Kafka\` |
| 配置文件 | `D:\Kafka\config\server.properties` |
| 数据目录 | `D:\kafka\kafka-logs` |
| 启动方式 | `dev-start-all.ps1`（需先启动 Zookeeper） |
| 连接地址 | `localhost:9092` |
| ZK 地址 | `localhost:2181` |
| Java 版本 | 使用 JDK 17（已修复 wmic 兼容性） |

**关键配置**

```properties
broker.id=0
listeners=PLAINTEXT://localhost:9092
log.dirs=D:/kafka/kafka-logs
zookeeper.connect=localhost:2181
```

**Spring Boot 集成**

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    consumer:
      group-id: my-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
```

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

**已知问题**

- ~~`wmic` 命令被 Windows 11 24H2 移除~~ → 已修复 `kafka-server-start.bat`、`kafka-server-stop.bat`、`zookeeper-server-stop.bat`
- Kafka 3.6.0 需 JDK 17 启动（已内置在 dev-start-all.ps1 中处理）
- 如需全量重置：删除 `D:\kafka\kafka-logs\meta.properties` + 重启 ZK

**常用命令**

```powershell
# 创建 Topic
D:\Kafka\bin\windows\kafka-topics.bat --create --topic test --bootstrap-server localhost:9092

# 列出 Topic
D:\Kafka\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092

# 生产者
D:\Kafka\bin\windows\kafka-console-producer.bat --topic test --bootstrap-server localhost:9092

# 消费者
D:\Kafka\bin\windows\kafka-console-consumer.bat --topic test --from-beginning --bootstrap-server localhost:9092
```

**在项目中的角色**

| 维度 | 说明 |
|------|------|
| **核心能力** | 海量数据按顺序写入磁盘日志，支持多消费者独立回溯（像读 Git 历史一样读消息） |
| **为什么不是 RabbitMQ** | RabbitMQ 消息消费完就删；Kafka 消息保留 7 天，多个消费者可以各自独立读取 |
| **典型场景** | App 上的每一次点击/曝光/滑动 → 进入 Kafka → Flink/Spark 实时分析 → 写入用户画像 |
| **AI 方向价值** | RAG 数据摄入管道：文档变更 → Kafka → 消费端更新向量库；模型推理日志收集 |
| **典型面试场景** | 分区机制、消费者组 Rebalance、Exactly Once 语义、高吞吐原理（顺序IO+零拷贝） |

---

### 4.3 RocketMQ 5.5.0

**基本信息**

| 项目 | 值 |
|------|-----|
| 安装路径 | `D:\RocketMQ\rocketmq-all-5.5.0-bin-release\` |
| 配置文件 | `D:\RocketMQ\rocketmq-all-5.5.0-bin-release\conf\broker.conf` |
| 启动方式 | `dev-start-all.ps1` |
| NameServer | `localhost:9876` |
| Broker | `localhost:10911` |

**关键配置**

```properties
brokerClusterName=DefaultCluster
brokerName=broker-a
brokerId=0
brokerIP1=127.0.0.1
namesrvAddr=127.0.0.1:9876
autoCreateTopicEnable=true
```

**Spring Boot 集成**

```yaml
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: my-producer-group
```

```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
</dependency>
```

**在项目中的角色**

| 维度 | 说明 |
|------|------|
| **核心能力** | 结合了 RabbitMQ 的可靠路由 + Kafka 的高吞吐，自带分布式事务消息（阿里开源） |
| **什么时候首选它** | 阿里云/Spring Cloud Alibaba 体系项目；需要事务消息（RocketMQ 独有优势） |
| **事务消息场景** | 订单创建 → 发送 half 消息 → 执行本地事务 → commit/rollback，保证数据库和消息原子性 |

---

## 五、注册中心 & 配置中心

### 5.1 Nacos

**基本信息**

| 项目 | 值 |
|------|-----|
| 安装路径 | `D:\nacos\nacos\` |
| 配置文件 | `D:\nacos\nacos\conf\application.properties` |
| 启动方式 | `dev-start-all.ps1`（standalone 模式） |
| 连接地址 | `localhost:8848` |
| 控制台 | `http://localhost:8848/nacos` |
| 存储 | 内置 Derby（开发模式） |

**关键配置**

```properties
server.port=8848
# standalone 模式使用嵌入式 Derby，无需 MySQL
```

**Spring Boot 集成**

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
      config:
        server-addr: localhost:8848
        file-extension: yaml
```

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
```

**在项目中的角色——微服务治理三件套**

```
Nacos（注册配置中心）       Sentinel（流量控制）        Seata（分布式事务）
     │                          │                         │
     ├─ 服务 A 想知道服务 B       ├─ 接口 QPS 超过阈值         ├─ 订单服务 + 库存服务
     │  在哪？问 Nacos            │   自动限流/熔断              │   跨库操作保证一致性
     │                           │                         │
     ▼                           ▼                         ▼
 "电话簿+配置中心"            "电路保险丝"               "跨服务的事务管家"
```

**Nacos 在项目中的角色**

| 维度 | 说明 |
|------|------|
| **注册中心** | 微服务实例把自己注册到 Nacos，其他服务来 Nacos 查询地址。没有它，微服务之间找不到对方 |
| **配置中心** | 所有微服务的配置文件统一存在 Nacos，修改配置后实时推送，无需重启应用 |
| **什么时候需要** | 项目拆分成 3+ 个微服务时就需要；单体项目暂时用不上 |

---

### 5.2 Zookeeper

**基本信息**

| 项目 | 值 |
|------|-----|
| 安装路径 | `D:\zookeeper\` |
| 配置文件 | `D:\zookeeper\conf\zoo.cfg` |
| 数据目录 | `D:\zookeeper\data` |
| 启动方式 | `dev-start-all.ps1`（先于 Kafka 启动） |
| 连接地址 | `localhost:2181` |

**应用场景**

- Kafka 元数据管理（当前唯一用途）
- 也可用于 Dubbo 注册中心
- 分布式锁（Curator）

---

## 六、搜索引擎

### 6.1 Elasticsearch 9.4.2

**基本信息**

| 项目 | 值 |
|------|-----|
| 安装路径 | `D:\ElasticSearch\elasticsearch-9.4.2\` |
| 配置文件 | `D:\ElasticSearch\elasticsearch-9.4.2\config\elasticsearch.yml` |
| JVM 配置 | `D:\ElasticSearch\elasticsearch-9.4.2\config\jvm.options.d\heap.options` |
| 启动方式 | `dev-start-all.ps1` |
| 连接地址 | `localhost:9200` |
| 集群名 | `es-cluster` |
| 安全认证 | 已关闭（开发环境） |

**JVM 堆设置**

```
-Xms512m
-Xmx512m
```

> 已创建 `jvm.options.d/heap.options` 覆盖默认 8GB 自动分配。开发用途 512MB 足够。

**Spring Boot 集成**

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
    # username:      # 开发环境无需
    # password:
```

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

**验证命令**

```powershell
# 健康检查
curl http://localhost:9200

# 集群状态
curl http://localhost:9200/_cluster/health

# 查看索引
curl http://localhost:9200/_cat/indices?v
```

**在项目中的角色**

| 维度 | 说明 |
|------|------|
| **核心能力** | 倒排索引全文搜索 + 聚合分析 + 地理位置计算 |
| **为什么不用 MySQL 做搜索** | MySQL `LIKE '%keyword%'` 是全表扫描，百万数据查一次好几秒；ES 倒排索引毫秒级返回 |
| **典型场景** | 京东搜"手机"→ ES 从百万商品中 50ms 返回结果（关键词匹配、价格排序、品牌聚合） |
| **AI 方向价值** | ES 9.x 原生支持 dense_vector，可以直接存文本向量做混合搜索（关键词+语义） |

---

## 七、Web 服务器 & 网关

### 7.1 Nginx 1.30.2

**基本信息**

| 项目 | 值 |
|------|-----|
| 安装路径 | `D:\Nginx\` |
| 配置文件 | `D:\Nginx\conf\nginx.conf` |
| 启动方式 | `dev-start-all.ps1` |
| 端口 | `localhost:80` |

**默认配置**

```nginx
worker_processes  1;
events { worker_connections  1024; }
http {
    server {
        listen       80;
        server_name  localhost;
        location / {
            root   html;
            index  index.html index.htm;
        }
    }
}
```

**管理命令**

```powershell
# 启动
D:\Nginx\nginx.exe

# 停止
D:\Nginx\nginx.exe -s stop

# 重载配置
D:\Nginx\nginx.exe -s reload
```

**在项目中的角色**

| 维度 | 说明 |
|------|------|
| **核心能力** | 反向代理（把外部请求转发给内部服务）、负载均衡、静态文件服务 |
| **典型场景** | 前端在 8081，后端在 8080 → Nginx 统一对外暴露 80 端口，根据路径转发；生产环境加上 HTTPS |
| **为什么用 Nginx 而不是直接暴露 Tomcat** | 安全：隐藏后端真实端口；高效：Nginx 处理静态文件比 Tomcat 快 10 倍 |

---

### 7.2 Tomcat 9.0.118

**基本信息**

| 项目 | 值 |
|------|-----|
| 安装路径 | `D:\Tomcat9\apache-tomcat-9.0.118\` |
| 启动方式 | `dev-start-all.ps1` |
| 端口 | `localhost:8080` |

**应用场景**

- 传统 Servlet/JSP 项目部署
- War 包部署测试
- 非 Spring Boot 项目的 Servlet 容器

---

## 八、分布式事务 & 流量控制

### 8.1 Seata 2.6.0

**基本信息**

| 项目 | 值 |
|------|-----|
| 安装路径 | `D:\seata\apache-seata-2.6.0-incubating-bin\seata-server\` |
| 配置文件 | `D:\seata\apache-seata-2.6.0-incubating-bin\seata-server\conf\application.yml` |
| 启动方式 | `dev-start-all.ps1` |
| 端口 | `localhost:8091` |
| 存储模式 | file（开发环境单机） |

**关键配置**

```yaml
seata:
  config:
    type: file
  registry:
    type: file
  store:
    mode: file   # 生产环境改用 db/redis
```

**Spring Boot 集成**

```yaml
seata:
  tx-service-group: my_tx_group
  service:
    vgroup-mapping:
      my_tx_group: default
    grouplist:
      default: 127.0.0.1:8091
```

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
</dependency>
```

**在项目中的角色**

| 维度 | 说明 |
|------|------|
| **核心能力** | 跨多个数据库/微服务的事务统一提交或回滚 |
| **为什么需要它** | 订单服务（MySQL）扣了库存 → 支付服务（Redis）扣了余额 → 如果支付失败，库存要还原。Seata 自动处理这个"还原"逻辑 |
| **AT 模式原理** | 拦截 SQL → 执行前拍快照（undo log）→ 执行 SQL → 如果失败，用 undo log 反向生成回滚 SQL |
| **什么时候需要** | 项目拆成微服务、一个操作涉及多个数据库时才需要；单体项目用不上 |

---

### 8.2 Sentinel Dashboard

**基本信息**

| 项目 | 值 |
|------|-----|
| JAR 路径 | `D:\sentinel-dashboard\sentinel.jar` |
| 启动方式 | `dev-start-all.ps1` |
| 端口 | `localhost:8089` |
| 控制台 | `http://localhost:8089` |

> 端口从默认 8080 改为 8089，避免与 Tomcat(8080) 和 FlavorDash 前端(8081) 冲突。

**Spring Boot 集成**

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8089
        port: 8719       # 客户端通信端口
      eager: true
```

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

**在项目中的角色**

| 维度 | 说明 |
|------|------|
| **核心能力** | 接口限流（QPS）、熔断降级（慢调用自动切断）、系统负载保护 |
| **典型场景** | 秒杀活动：限制每个用户每秒只能请求 1 次；如果库存服务响应超过 2 秒，自动熔断返回"系统繁忙" |
| **和 Hystrix 的区别** | Sentinel 控制台可视化配置实时生效、支持更多规则类型（热点/系统规则）、阿里持续维护 |

---

## 九、容器与虚拟化

### 9.1 Docker Desktop

| 项目 | 值 |
|------|-----|
| 版本 | Docker 29.5.3 + Compose v5.1.4 |
| CLI 路径 | `C:\Program Files\Docker\Docker\resources\bin\docker.exe` |
| 启动脚本 | `D:\Tools\docker-start.bat` |
| Compose 模板 | `D:\Tools\docker-compose-template.yml` |
| 启动方式 | 右键管理员运行 Docker Desktop 或 `docker-start.bat` |

**在项目中的角色**

| 维度 | 说明 |
|------|------|
| **核心能力** | 把服务打包成"集装箱"（容器），一键在任何机器上复现完全相同的环境 |
| **为什么需要它** | 本地装 15 个服务太痛苦 → Docker 写一个 yaml 文件，一行命令全部启动，换电脑也不怕 |
| **开发阶段** | 用 Docker 跑 MySQL/Redis/Milvus 等服务，保持主机干净（当前用原生安装，后续可迁移） |
| **部署阶段** | CI/CD 构建 Docker 镜像 → 推到镜像仓库 → 服务器一键部署 |
| **AI 方向价值** | Milvus、Langfuse、Dify 等 AI 工具官方只提供 Docker 部署方式，没有 Docker 寸步难行 |

**启动 Docker**

```powershell
# 方法1：右键管理员运行
D:\Tools\docker-start.bat

# 方法2：开始菜单搜索 Docker Desktop → 右键 → 以管理员身份运行

# 等 Docker Engine 就绪后验证
docker info
docker --version
```

**常用命令**

```powershell
# 查看运行中的容器
docker ps

# 查看所有容器（包括已停止）
docker ps -a

# 查看本地镜像
docker images

# 拉取镜像
docker pull redis:7.0-alpine
docker pull mysql:8.0

# 运行一个 Redis（端口映射避免和本地冲突）
docker run -d --name redis-docker -p 6380:6379 redis:7.0-alpine

# 进入容器内部
docker exec -it redis-docker sh

# 停止和删除
docker stop redis-docker
docker rm redis-docker

# 清理无用的镜像/容器/卷
docker system prune -a
```

**Docker vs 原生安装对比**

| 场景 | 用 Docker | 用原生 |
|------|-----------|--------|
| 快速启动一个临时 MySQL | `docker run -d mysql:8.0` | 安装+配置 10 分钟 |
| 换电脑/重装系统 | 一个 compose 文件搞定 | 一个个重装 + 配置 |
| 同时跑 MySQL 5.7 和 8.0 | 不同端口两个容器 | 基本不可能 |
| 长期稳定开发 | 每次开机要启动 Docker | 原生服务开机自启更方便 |

> **当前策略**：核心服务（MySQL/Redis/MongoDB/RabbitMQ）用原生 Windows Service 开机自启，临时工具（Milvus/Langfuse/Dify）用 Docker。Compose 模板已预配 MySQL/Redis/Milvus/Langfuse 的配置文件，需要时取消注释即可。

**Docker Compose 模板使用**

```powershell
# 复制模板为新项目
copy D:\Tools\docker-compose-template.yml D:\Projects\my-app\docker-compose.yml

# 根据需要取消注释需要的服务（编辑 docker-compose.yml）
# 启动所有已启用的服务
docker compose up -d

# 查看日志
docker compose logs -f

# 停止
docker compose down
```

**Alpine 镜像推荐（省磁盘）**

```powershell
# Alpine 版本比标准版小 5-10 倍
docker pull redis:7.0-alpine       # 30MB vs 120MB
docker pull mysql:8.0              # MySQL 没有官方 Alpine（用 Oracle Linux slim）
docker pull nginx:alpine           # 10MB vs 50MB
docker pull python:3.14-alpine     # 50MB vs 350MB
```

## 十、Spring Boot 项目集成模板

### 10.1 完整 pom.xml 依赖（按需选用）

```xml
<!-- 数据库 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>

<!-- 消息 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
</dependency>

<!-- 微服务治理 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
</dependency>
```

### 10.2 完整 application-dev.yml 模板

```yaml
server:
  port: 8080

spring:
  # MySQL
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/your_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: 123456

  # Redis
  data:
    redis:
      host: localhost
      port: 6379
      database: 0

  # RabbitMQ
  rabbitmq:
    host: localhost
    port: 5672
    username: admin
    password: admin123
    virtual-host: flavor

  # Elasticsearch
  elasticsearch:
    uris: http://localhost:9200

  # Kafka
  kafka:
    bootstrap-servers: localhost:9092

# Nacos
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
      config:
        server-addr: localhost:8848

# Sentinel
    sentinel:
      transport:
        dashboard: localhost:8089

# Seata
seata:
  tx-service-group: my_tx_group
  service:
    grouplist:
      default: 127.0.0.1:8091

# RocketMQ
rocketmq:
  name-server: 127.0.0.1:9876
```

---

## 十一、端口映射速查

| 端口 | 服务 | 用途 |
|------|------|------|
| 80 | Nginx | Web 服务器 |
| 2181 | Zookeeper | Kafka 协调 |
| 5432 | PostgreSQL | 关系数据库 |
| 3306 | MySQL | 关系数据库 |
| 5672 | RabbitMQ | AMQP 消息 |
| 6379 | Redis | 缓存 |
| 8080 | Tomcat | Servlet 容器 |
| 8089 | Sentinel | 流量控制面板 |
| 8091 | Seata | 分布式事务 |
| 8848 | Nacos | 注册/配置中心 |
| 9092 | Kafka | 消息流 |
| 9200 | Elasticsearch | 搜索引擎 |
| 9876 | RocketMQ NS | 名称服务 |
| 10911 | RocketMQ Broker | 消息代理 |
| 15672 | RabbitMQ Web | 管理后台 |
| 27017 | MongoDB | 文档数据库 |
| 33060 | MySQL X | MySQL X Protocol |
| 11434 | Ollama | 本地模型 API |

---

## 十二、AI 大模型开发环境

> 16GB 内存约束下的轻量化方案。本地模型用 1.5B 参数级别，避免 OOM。

### 12.1 Ollama（本地模型服务）

**在项目中的角色**

| 维度 | 说明 |
|------|------|
| **核心能力** | 在本地运行开源大模型（DeepSeek、Qwen、Llama），提供与 OpenAI 兼容的 HTTP API |
| **为什么不用直接调云端 API** | 免费、离线、数据不出本机、调试不受限。开发阶段用 Ollama 本地跑，生产环境再切云端 API |
| **在你的 AI 项目里的位置** | 它是"模型层"——Java 通过 Spring AI 或 HTTP 调用 Ollama，Ollama 负责加载模型做推理 |
| **和 Spring AI 的关系** | Ollama 是"引擎"，Spring AI 是"方向盘"——Spring AI 封装了调用逻辑，你写 Java 代码就能用 |

**基本信息**

| 项目 | 值 |
|------|-----|
| 版本 | 0.30.9 |
| 安装路径 | `C:\Users\13686\AppData\Local\Programs\Ollama\` |
| 模型存储 | `D:\ollama-models\` |
| 启动方式 | 开机自启（托盘图标） |
| API 地址 | `http://localhost:11434` |
| 管理命令 | `ollama list` / `ollama pull` / `ollama rm` |

**已安装模型（3个）**

| 模型 | 大小 | 用途 |
|------|------|------|
| `deepseek-r1:1.5b` | 1.1 GB | 推理/代码生成（轻量） |
| `qwen2.5:7b` | 4.7 GB | 通用对话（较重，按需加载） |
| `nomic-embed-text` | 274 MB | 文本嵌入向量（RAG 专用） |

**常用命令**

```powershell
# 查看模型列表
ollama list

# 拉取新模型（示例）
ollama pull qwen2.5:1.5b        # 轻量对话
ollama pull codellama:7b         # 代码专用

# 删除模型释放空间
ollama rm sam860/olmo2:1b-Q5_K_M

# 测试 API
curl http://localhost:11434/api/generate -d '{"model":"deepseek-r1:1.5b","prompt":"Hello"}'
```

**Spring AI 集成（Ollama 本地）**

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: deepseek-r1:1.5b
        options:
          temperature: 0.7
      embedding:
        model: nomic-embed-text
```

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
</dependency>
```

**API 调用方式对比**

| 方式 | 场景 | 优点 |
|------|------|------|
| Ollama 本地 | 开发调试、数据敏感 | 免费、离线、隐私 |
| DeepSeek API | 生产/高性能需求 | 速度快、效果更好 |
| 阿里云百炼 | 国内合规、多模态 | 通义千问生态 |

---

### 12.2 Python AI 工具链

**在项目中的角色**

| 维度 | 说明 |
|------|------|
| **为什么 Java 后端还要 Python** | Python 是 AI 生态的"第一语言"——LangChain、ChromaDB、HuggingFace 全在 Python 上。用 Python 做 RAG 原型快 10 倍，验证通过后再把逻辑迁移到 Java Spring AI |
| **和 Java 的关系** | Java 负责生产级后端（接口、事务、安全），Python 负责 AI 实验和数据处理。两者通过 HTTP API 或共享数据库通信 |
| **典型工作流** | Python 写 RAG 原型验证效果 → 确认方案可行 → Java Spring AI 实现生产版本 → Python 保留做模型评测 |

> 用于 RAG 原型验证、数据处理、模型评测。

**已安装库（pip）**

| 库 | 版本 | 用途 |
|------|------|------|
| `langchain` | 1.3.2 | LLM 应用框架 |
| `langchain-ollama` | 1.1.0 | LangChain 的 Ollama 集成 |
| `langchain-chroma` | 1.1.0 | ChromaDB 向量存储集成 |
| `openai` | 2.38.0 | OpenAI/兼容 API 调用 |
| `chromadb` | 1.5.9 | 轻量向量数据库 |
| `faiss-cpu` | 1.14.2 | Facebook 向量检索 |
| `sentence-transformers` | 5.5.1 | 文本嵌入模型 |
| `transformers` | 5.9.0 | HuggingFace 模型库 |

**安装/更新**

```powershell
python -m pip install langchain langchain-ollama chromadb openai sentence-transformers faiss-cpu
python -m pip install --upgrade langchain  # 升级
```

**Python + ChromaDB RAG 最小示例**

```python
from langchain_ollama import OllamaEmbeddings, ChatOllama
from langchain_chroma import Chroma
from langchain.text_splitter import RecursiveCharacterTextSplitter

# 1. 加载文档并切分
with open("doc.txt", encoding="utf-8") as f:
    text = f.read()
splitter = RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=50)
chunks = splitter.split_text(text)

# 2. 存入向量库
embeddings = OllamaEmbeddings(model="nomic-embed-text", base_url="http://localhost:11434")
db = Chroma.from_texts(chunks, embeddings, persist_directory="./chroma_db")

# 3. 检索 + 问答
query = "你的问题"
docs = db.similarity_search(query, k=3)
context = "\n".join([d.page_content for d in docs])

llm = ChatOllama(model="deepseek-r1:1.5b", base_url="http://localhost:11434")
response = llm.invoke(f"根据以下内容回答：\n{context}\n\n问题：{query}")
print(response.content)
```

---

### 12.3 向量数据库

**在项目中的角色**

| 维度 | 说明 |
|------|------|
| **为什么需要它** | 大模型的知识截止于训练日期，公司内部文档/产品手册它完全不知道。向量库存储"私有知识"，每次提问时检索相关内容作为上下文喂给大模型 |
| **工作流程** | 文档 → 切分 → Embedding 模型转向量 → 存入向量库 → 用户提问 → 问题转向量 → 向量库搜索 → 结果+问题一起发给 LLM 生成答案 |
| **业务价值** | 智能客服（基于产品手册回答）、代码库问答（基于内部文档）、合同检索（基于历史合同） |

**方案对比**

| 方案 | 内存占用 | 适用场景 | 状态 |
|------|----------|----------|------|
| **ChromaDB** | ~100MB | RAG 原型、小规模数据 | ✅ 已安装 |
| **FAISS** | ~50MB | 纯向量检索、无需持久化 | ✅ 已安装 |
| Milvus | 2-4GB | 生产级、海量向量 | ❌ 太重（需Docker） |
| Elasticsearch | — | 已有全文+向量混合搜索 | ✅ 可用（ES 9.4.2） |

> 建议：RAG 原型用 ChromaDB，生产项目用已有的 Elasticsearch 9.4.2（支持 dense_vector）或 Spring AI 的 SimpleVectorStore。

---

### 12.4 Java AI 集成方案

**方案一：Spring AI（推荐）**

```xml
<!-- Spring AI BOM -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.0.0-M6</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- Ollama 集成 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
</dependency>

<!-- 向量存储 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-chroma-store-spring-boot-starter</artifactId>
</dependency>
```

**方案二：直接调用 Ollama HTTP API**

```java
// RestTemplate 直接调用，无需额外依赖
RestTemplate rest = new RestTemplate();
var request = Map.of("model", "deepseek-r1:1.5b", "prompt", "你好", "stream", false);
var response = rest.postForObject("http://localhost:11434/api/generate", request, Map.class);
System.out.println(response.get("response"));
```

**方案三：DeepSeek 云端 API（FlavorDash 使用）**

```yaml
spring:
  ai:
    openai:
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com
      chat:
        model: deepseek-chat
```

---

### 12.5 PostgreSQL 17

**基本信息**

| 项目 | 值 |
|------|-----|
| 安装路径 | `D:\PostgreSQL\` |
| 数据目录 | `D:\PostgreSQL\pgsql\data` |
| 启动方式 | 按需启动 `D:\PostgreSQL\pgsql\start-pg.bat` |
| 端口 | `localhost:5432` |
| 默认用户 | `postgres`（无密码，本地信任认证） |

> **未设为 Windows Service**，避免闲置占用内存。需要时手动启动。

**启动/停止**

```powershell
# 启动
D:\PostgreSQL\pgsql\start-pg.bat
# 或直接命令
D:\PostgreSQL\pgsql\bin\pg_ctl.exe -D D:\PostgreSQL\pgsql\data -l D:\PostgreSQL\pgsql\logfile.log start

# 停止
D:\PostgreSQL\pgsql\stop-pg.bat
D:\PostgreSQL\pgsql\bin\pg_ctl.exe -D D:\PostgreSQL\pgsql\data stop
```

**Spring Boot 集成**

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/your_db
    username: postgres
    password:            # 本地开发无密码
```

**应用场景**

- 地理空间数据（PostGIS）
- JSON 原生支持优于 MySQL
- 需要高级 SQL 特性的场景（窗口函数、CTE）
- 与 MySQL 对比学习

---

## 十三、可观测性工具

### 13.1 Prometheus（监控）

**在项目中的角色**

| 维度 | 说明 |
|------|------|
| **核心能力** | 定时拉取应用指标（CPU、内存、QPS、延迟），存储时序数据，支持 PromQL 灵活查询 |
| **和 Arthas 的区别** | Arthas 是"出了问题现场排查"（交互式诊断），Prometheus 是"一直盯着，出问题前预警"（持续监控+告警） |
| **典型场景** | 配置"接口延迟超过 500ms 持续 5 分钟 → 自动发钉钉/邮件告警" |

**基本信息**

| 项目 | 值 |
|------|-----|
| 安装路径 | `D:\Tools\prometheus\` |
| 端口 | `localhost:9090` |
| 启动方式 | 按需启动 `prometheus.exe` |

**快速启动**

```powershell
cd D:\Tools\prometheus
.\prometheus.exe --config.file=prometheus.yml
# 访问 http://localhost:9090
```

**Spring Boot 集成（Micrometer）**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**应用场景**

- JVM 内存/GC 监控
- HTTP 请求 QPS/延迟
- 自定义业务指标
- 配合 Grafana 可视化

---

### 13.2 SkyWalking（链路追踪）

**集成方式**：Java Agent（无需修改代码）

```xml
<!-- 可选：SkyWalking 工具类依赖 -->
<dependency>
    <groupId>org.apache.skywalking</groupId>
    <artifactId>apm-toolkit-trace</artifactId>
    <version>9.3.0</version>
</dependency>
```

**启动参数**

```powershell
# 下载 agent：https://skywalking.apache.org/downloads/
# 启动时附加 agent：
java -javaagent:D:/Tools/skywalking-agent/skywalking-agent.jar \
     -DSW_AGENT_NAME=your-app \
     -DSW_AGENT_COLLECTOR_BACKEND_SERVICES=127.0.0.1:11800 \
     -jar your-app.jar
```

> SkyWalking 完整部署需要 OAP Server + UI，较重。本地开发先加 Micrometer + Arthas 足够排查大部分问题。面试/大项目再配合全量 SkyWalking 部署。

---

### 13.3 Arthas（诊断）

**在项目中的角色**

| 维度 | 说明 |
|------|------|
| **核心能力** | 不重启应用就能查看 JVM 内部状态：实时 CPU/内存、线程堆栈、方法调用链追踪、反编译运行中的类 |
| **典型场景** | 线上 CPU 突然 100% → Arthas `dashboard` 秒级定位到具体线程 → `thread -n 3` 看到哪个方法在死循环 |
| **面试含金量** | 会 Arthas = 证明你有真正的线上排查能力，不是只会看日志 |

安装路径 `D:\Tools\arthas\`，已加入 PATH。

```powershell
java -jar D:\Tools\arthas\arthas-boot.jar
# dashboard → 实时面板
# thread -n 3 → 最忙线程
# trace com.xxx.Service method → 方法追踪
```

---

## 十四、环境变量完整清单

> 以下变量在本次配置中全部设置完毕，无需手动干预。

### 14.1 系统变量

| 变量名 | 值 | 作用 |
|--------|-----|------|
| `JAVA_HOME` | `D:\JDK25` | Java 主目录 |
| `MAVEN_HOME` | `D:\maven` | Maven 主目录 |

### 14.2 PATH 关键条目

| 路径 | 对应工具 |
|------|----------|
| `D:\JDK25\bin` | java, javac, jshell |
| `D:\maven\bin` | mvn |
| `D:\Git\cmd` | git |
| `D:\MySQL\MySQL Server 8.0\bin` | mysql |
| `D:\Redis` | redis-cli |
| `D:\Nginx` | nginx |
| `D:\Python314` | python |
| `D:\Python314\Scripts` | pip |
| `D:\PostgreSQL\pgsql\bin` | psql, pg_ctl |
| `D:\RabbitMQ Server\rabbitmq_server-3.12.13\sbin` | rabbitmqctl |
| `D:\RocketMQ\rocketmq-all-5.5.0-bin-release\bin` | mqadmin |
| `D:\Tools\arthas` | arthas-boot |
| `C:\nvm4w\nodejs` | node, npm |

### 14.3 验证方法

```powershell
# 打开新终端后逐一验证
java --version
mvn --version
git --version
node --version
python --version
mysql --version
redis-cli --version
docker --version
```

---

## 十五、日常工作流

### 15.1 开机后

```powershell
# 1. 四个核心服务自动启动，无需操作
# 2. 启动其余服务
cd D:\本地学习项目\Java-AI-LLM-Learning-Records
.\dev-start-all.ps1

# 检查状态（可选）
.\dev-start-all.ps1 -status
```

### 15.2 启动 FlavorDash 项目

```powershell
cd D:\本地核心开发项目\FlavorDash
.\start.ps1 check      # 环境检查
.\start.ps1 backend    # 启动后端
.\start.ps1 frontend   # 启动前端 (admin UI)
```

### 15.3 关机前

```powershell
# 停止手动启动的服务
.\dev-start-all.ps1 -stop

# Windows Service 不需要管，关机自动停
```

---

## 十六、常见问题排查

### MySQL 连不上

```powershell
# 检查服务
Get-Service MySQL80

# 测试端口
Test-NetConnection localhost -Port 3306

# 测试登录
D:\MySQL\MySQL Server 8.0\bin\mysql.exe -u root -p123456 -h 127.0.0.1 -e "SELECT 1"
```

### Redis 连不上

```powershell
Get-Service Redis
D:\Redis\redis-cli.exe -h 127.0.0.1 -p 6379 PING
```

### RabbitMQ 连不上

```powershell
Get-Service RabbitMQ
curl http://localhost:15672
# 如果 admin 密码忘记，用 guest/guest 登录管理后台重置
```

### Kafka 启动失败

```powershell
# 原因1：Zookeeper 没启动
Test-NetConnection localhost -Port 2181

# 原因2：meta.properties 冲突
Remove-Item D:\kafka\kafka-logs\meta.properties -Force

# 原因3：JDK 版本不对（必须 JDK 17）
$env:JAVA_HOME = "D:\JDK17"

# 手动启动调试
D:\Kafka\bin\windows\kafka-server-start.bat D:\Kafka\config\server.properties
```

### Elasticsearch 启动失败

```powershell
# 检查 JVM 堆是否过大
Get-Content D:\ElasticSearch\elasticsearch-9.4.2\config\jvm.options.d\heap.options

# 检查日志
Get-Content D:\ElasticSearch\elasticsearch-9.4.2\logs\elasticsearch.log -Tail 30

# 手动启动查看错误
D:\ElasticSearch\elasticsearch-9.4.2\bin\elasticsearch.bat
```

### 端口被占用

```powershell
# 查占用进程
Get-NetTCPConnection -LocalPort 8080 | Select-Object OwningProcess
Get-Process -Id <PID>

# 杀掉进程
Stop-Process -Id <PID> -Force
```

---

## 十七、前端开发环境

### 17.1 已安装清单

| 工具 | 版本/状态 | 用途 |
|------|-----------|------|
| **Node.js** | v22.14.0 (nvm4w 管理) | JS 运行时 |
| **npm** | 10.9.2 | 默认包管理器 |
| **pnpm** | 11.5.2 | 更快更省磁盘的包管理器（推荐） |
| **nvm** | 1.2.2 | Node 版本切换 |
| **VS Code** | 1.125.1 | 前端主力 IDE |
| **Chrome** | ✅ | 调试主力 |
| **Edge** | ✅ | 备用 |
| **Snipaste** | ✅ | 截图贴图神器 |
| **Git** | ✅ | 版本控制 |

### 17.2 VS Code 已安装插件

| 插件 | 用途 |
|------|------|
| **ESLint** | JS/TS 代码检查 |
| **Prettier** | 代码自动格式化 |
| **Volar** | Vue 语法高亮和提示 |
| **ES7+ React** | React 语法支持 |
| **GitLens** | Git 历史/Blame |
| **Live Server** | 静态页面热更新预览 |
| **REST Client** | VS Code 内调试 API |
| **Tailwind CSS IntelliSense** | Tailwind 类名自动补全 |

### 17.3 前端核心栈速查

```
框架选择        React（生态最大） 或  Vue（国内流行，文档中文友好）
构建工具        Vite（新一代，快） / Webpack（老项目常用）
类型系统        TypeScript（大项目必用）
CSS 方案        Tailwind CSS / Sass / Less
状态管理        Redux/Zustand(React) 或  Pinia(Vue)
路由            React Router 或  Vue Router
HTTP 请求       Axios
UI 组件库       Ant Design / Element Plus / Material-UI
代码规范       ESLint + Prettier + Husky + lint-staged
测试            Vitest(单元) + Playwright(E2E)
```

### 17.4 在项目中的角色

| 技术 | 后端思维类比 | 作用 |
|------|-------------|------|
| **Node.js** | JDK | 运行 JavaScript 的"虚拟机" |
| **npm/pnpm** | Maven/Gradle | 依赖管理 |
| **Vite** | spring-boot-maven-plugin | 构建+开发服务器 |
| **React/Vue** | Spring MVC | 界面层的"框架" |
| **Axios** | RestTemplate | HTTP 请求 |
| **TypeScript** | Java 的类型系统 | 给 JS 加类型约束 |
| **ESLint+Prettier** | Checkstyle+Spotless | 代码规范格式化 |

### 17.5 快速创建一个前端项目

```powershell
# React + Vite + TypeScript
npm create vite@latest my-app -- --template react-ts
cd my-app
npm install
npm run dev     # → http://localhost:5173

# Vue + Vite + TypeScript
npm create vite@latest my-app -- --template vue-ts
cd my-app
npm install
npm run dev     # → http://localhost:5173
```

### 17.6 前后端联调配置

```typescript
// vite.config.ts — 开发时把 /api 请求代理到后端
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',  // 你的 Spring Boot 后端
        changeOrigin: true,
      }
    }
  }
})
```

```typescript
// axios 实例
import axios from 'axios';
const api = axios.create({ baseURL: '/api', timeout: 10000 });
export default api;
```

### 17.7 VS Code 推荐配置

```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": "explicit"
  },
  "emmet.includeLanguages": {
    "javascript": "javascriptreact",
    "typescript": "typescriptreact"
  }
}
```

### 17.8 还需手动安装的

| 工具 | 方式 |
|------|------|
| **Figma** | figma.com → 下载桌面版 |
| **React DevTools** | Chrome 扩展商店 |
| **Vue DevTools** | Chrome 扩展商店 |
| **Playwright** | `npm install -D @playwright/test`（按项目装） |

---

## 十八、目录结构速查

```
D:\
├── MySQL\                       # MySQL 8.0 安装
├── Redis\                       # Redis 安装
├── MongoDB\                     # MongoDB 安装
├── RabbitMQ Server\             # RabbitMQ 安装
├── ElasticSearch\               # Elasticsearch 9.4.2
├── Kafka\                       # Kafka 3.6.0
├── zookeeper\                   # Zookeeper
├── RocketMQ\                    # RocketMQ 5.5.0
├── nacos\                       # Nacos
├── seata\                       # Seata 2.6.0
├── PostgreSQL\                   # PostgreSQL 17
├── sentinel-dashboard\          # Sentinel
├── Nginx\                       # Nginx 1.30.2
├── Tomcat9\                     # Tomcat 9
├── ollama-models\               # Ollama 模型文件
│
├── JDK8\ JDK17\ JDK21\ JDK25\   # JDK 多版本
├── maven\                       # Maven 3.9.16
├── Git\                         # Git
├── Nodejs\                      # Node.js
├── Python314\                   # Python 3.14
├── Tools\                       # 开发工具集
│   ├── arthas\                  # Arthas 诊断工具
│   └── prometheus\              # Prometheus 监控
│
├── IDEA\                        # IntelliJ IDEA
├── PyCharm\                     # PyCharm
├── Microsoft VS Code\           # VS Code
├── cursor\                      # Cursor
├── DBeaver\                     # 数据库客户端
├── Navicat Premium Lite 17\     # 数据库客户端
│
├── 本地核心开发项目\              # 核心项目
│   ├── FlavorDash\              # 餐饮 SaaS
│   ├── LingShu\                 # 医疗系统
│   └── SuGuangMall\             # 商城
├── 本地开发项目\                  # 练习项目
├── 本地学习项目\                  # 学习笔记
│   └── Java-AI-LLM-Learning-Records\  # ← 本文档所在
└── GitHub学习项目\                # GitHub 项目
```
