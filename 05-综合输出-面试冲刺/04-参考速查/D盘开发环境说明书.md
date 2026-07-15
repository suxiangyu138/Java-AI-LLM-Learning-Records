# D 盘 开发环境说明书

> **更新日期**：2026-06-24

---

## D 盘概览

| 维度 | 说明 |
|------|------|
| 角色 | 全量开发环境盘 |
| 核心 | 数据库 / 中间件 / JDK / IDE / 项目代码 / AI 工具 |

---

## 一、数据库

### 1. MySQL 8.0

| 属性 | 值 |
|------|-----|
| 版本 | 8.0.46（Community Server） |
| 路径 | `D:\MySQL\MySQL Server 8.0\` |
| 数据 | `C:\ProgramData\MySQL\MySQL Server 8.0\Data\` |
| 端口 | 3306 |

**连接**：

```bash
mysql -u root -p
```

**启动方式**：

```powershell
# 管理员 PowerShell
net start MySQL80          # 启动
net stop MySQL80           # 停止
```

**命令行客户端**：

```powershell
"D:\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p
```

---

### 2. Redis 5.0

| 属性 | 值 |
|------|-----|
| 路径 | `D:\Redis\` |
| 配置 | `D:\Redis\redis.windows.conf` |
| 端口 | 6379 |

```powershell
# 启动
D:\Redis\redis-server.exe D:\Redis\redis.windows.conf

# 客户端
D:\Redis\redis-cli.exe
```

---

### 3. MongoDB

| 属性 | 值 |
|------|-----|
| 路径 | `D:\MongoDB\` |
| 数据 | `D:\MongoDB\data\` |
| 日志 | `D:\MongoDB\log\` |
| 配置 | `D:\MongoDB\bin\mongod.cfg` |
| 端口 | 27017 |

```powershell
# 启动
D:\MongoDB\bin\mongod.exe --config D:\MongoDB\bin\mongod.cfg

# 客户端
D:\MongoDB\bin\mongosh.exe
```

---

### 4. PostgreSQL

| 属性 | 值 |
|------|-----|
| 路径 | `D:\PostgreSQL\pgsql\` |
| 数据 | `D:\PostgreSQL\pgsql\data\` |
| 管理 | pgAdmin 4（`D:\PostgreSQL\pgsql\pgAdmin 4`） |

---

### 5. ElasticSearch 9.4.2

| 属性 | 值 |
|------|-----|
| 路径 | `D:\ElasticSearch\elasticsearch-9.4.2\` |
| 配置 | `D:\ElasticSearch\elasticsearch-9.4.2\config\elasticsearch.yml` |
| 集群 | `es-cluster` |
| 端口 | 9200（HTTP） |

```powershell
D:\ElasticSearch\elasticsearch-9.4.2\bin\elasticsearch.bat
```

---

### 6. 数据库管理工具

| 工具 | 路径 |
|------|------|
| DBeaver | `D:\DBeaver\dbeaver.exe` |
| Navicat | `D:\Navicat Premium Lite 17\navicat.exe` |
| Data Studio | `D:\Data Studio\` |

---

## 二、消息中间件

### 1. RabbitMQ 3.12.13

| 属性 | 值 |
|------|-----|
| 路径 | `D:\RabbitMQ Server\rabbitmq_server-3.12.13\` |
| 依赖 | Erlang OTP 26（`D:\Erlang OTP\`） |
| 端口 | 5672（AMQP）/ 15672（管理面板） |

```powershell
# 启用管理插件
rabbitmq-plugins enable rabbitmq_management

# 启动
rabbitmq-server.bat              # 前台
rabbitmq-service.bat start       # Windows 服务
```

---

### 2. Kafka 3.6.0

| 属性 | 值 |
|------|-----|
| 路径 | `D:\Kafka\kafka_2.13-3.6.0\` |
| 配置 | `D:\Kafka\config\server.properties` |
| 日志 | `D:\Kafka\kafka-logs\` |
| 端口 | 9092（PLAINTEXT） |
| 依赖 | ZooKeeper（`localhost:2181`） |

**启动顺序**：

```powershell
# ① 启动 ZooKeeper
D:\zookeeper\bin\zkServer.cmd

# ② 启动 Kafka
D:\Kafka\bin\windows\kafka-server-start.bat D:\Kafka\config\server.properties
```

**创建 Topic**：

```powershell
D:\Kafka\bin\windows\kafka-topics.bat --create --topic test --bootstrap-server localhost:9092
```

---

### 3. RocketMQ 5.5.0

| 属性 | 值 |
|------|-----|
| 路径 | `D:\RocketMQ\rocketmq-all-5.5.0-bin-release\` |
| 配置 | `D:\RocketMQ\rocketmq-all-5.5.0-bin-release\conf\` |
| 面板 | `D:\rocketmq-dashboard\` |
| Name Server 端口 | 9876 |

---

### 4. ZooKeeper

| 属性 | 值 |
|------|-----|
| 路径 | `D:\zookeeper\` |
| 配置 | `D:\zookeeper\conf\zoo.cfg` |
| 数据 | `D:\zookeeper\data\` |
| 端口 | 2181 |

```powershell
D:\zookeeper\bin\zkServer.cmd
```

---

## 三、微服务组件

### 1. Nacos

| 属性 | 值 |
|------|-----|
| 路径 | `D:\nacos\nacos\` |
| 配置 | `D:\nacos\nacos\conf\application.properties` |
| 端口 | 8848 |

```powershell
# 单机模式启动
D:\nacos\nacos\bin\startup.cmd -m standalone
```

---

### 2. Sentinel Dashboard

| 属性 | 值 |
|------|-----|
| 路径 | `D:\sentinel-dashboard\sentinel.jar` |
| 端口 | 8080（默认） |

```powershell
java -jar D:\sentinel-dashboard\sentinel.jar
```

---

### 3. Seata 2.6.0

| 属性 | 值 |
|------|-----|
| 路径 | `D:\seata\apache-seata-2.6.0-incubating-bin\` |
| 配置 | `D:\seata\apache-seata-2.6.0-incubating-bin\conf\` |

---

## 四、Web 服务器与网关

### 1. Nginx 1.30.2

| 属性 | 值 |
|------|-----|
| 路径 | `D:\Nginx\` |
| 配置 | `D:\Nginx\conf\nginx.conf` |
| 端口 | 80 |

```powershell
D:\Nginx\nginx.exe              # 启动
D:\Nginx\nginx.exe -s stop      # 停止
D:\Nginx\nginx.exe -s reload    # 重载
```

---

### 2. Tomcat 9.0.118

| 属性 | 值 |
|------|-----|
| 路径 | `D:\Tomcat9\apache-tomcat-9.0.118\` |
| 端口 | 8080（默认） |

```powershell
D:\Tomcat9\apache-tomcat-9.0.118\bin\startup.bat     # 启动
D:\Tomcat9\apache-tomcat-9.0.118\bin\shutdown.bat    # 停止
```

---

## 五、JDK 版本

| 属性 | 值 |
|------|-----|
| 当前默认 | JDK 25（`JAVA_HOME=D:\JDK25`） |

### 已安装版本

| 版本 | 路径 |
|------|------|
| JDK 8 | `D:\JDK8` |
| JDK 17 | `D:\JDK17` |
| JDK 21 | `D:\JDK21` |
| JDK 25 | `D:\JDK25` ← 当前默认 |
| JRE 1.8 | `D:\JavaJRE1.8` |

### 切换默认 JDK

**临时切换**：

```powershell
$env:JAVA_HOME="D:\JDK21"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
```

**永久切换**：修改系统环境变量 `JAVA_HOME`。

---

## 六、构建工具

### 1. Maven 3.9.16

| 属性 | 值 |
|------|-----|
| 路径 | `D:\maven\apache-maven-3.9.16\` |
| 配置 | `D:\maven\apache-maven-3.9.16\conf\settings.xml` |
| 本地仓库 | `C:\Users\13686\.m2\repository\` |
| 镜像 | 阿里云镜像（已配置） |

### 2. Node.js 22.23.1（Volta 管理）

| 属性 | 值 |
|------|-----|
| 包管理 | npm（`D:\npm-global\`）/ pnpm（`D:\.pnpm-store\`） |
| Node 路径 | `D:\Nodejs\` |

### 3. Python

| 版本 | 路径 |
|------|------|
| Python 3.12 | `C:\Users\13686\AppData\Local\Programs\Python\Python312\` |
| Python 3.14（当前） | `D:\Python314\` |
| pip 缓存 | `D:\pip-cache\` |

---

## 七、开发工具与 IDE

| # | 工具 | 路径 |
|---|------|------|
| 1 | IntelliJ IDEA 2026.1.2 | `D:\IDEA\IntelliJ IDEA 2026.1.2\` |
| 2 | PyCharm 2026.1.2 | `D:\PyCharm\PyCharm 2026.1.2\` |
| 3 | Visual Studio Code 1.125.1 | 扩展：`D:\vscode-exts\` |
| 4 | Cursor | `D:\cursor\Cursor.exe` |
| 5 | Windsurf | `D:\Windsurf\Windsurf.exe` |
| 6 | Codex++ | `D:\Codex++\codex-plus-plus.exe` |
| 7 | CodeBuddy CN | `D:\CodeBuddy CN\` |
| 8 | ApiFox | `D:\ApiFox\` |
| 9 | Postman | `D:\Postman\` |
| 10 | Ditto（剪贴板管理） | `D:\Ditto\` |
| 11 | FinalShell（SSH 客户端） | `D:\FinalShell\finalshell.exe` |
| 12 | Xshell | `D:\Xshell\` |
| 13 | 7-Zip | `D:\7-Zip\` |

---

## 八、AI 工具

### 1. Ollama 0.30.9

```bash
ollama serve
```

模型路径：`D:\ollama-models\`

### 2. Perplexity

路径：`D:\Perplexity\`

### 3. AI-Tools（AI 辅助工具集）

路径：`D:\AI-Tools\`

---

## 九、容器

### Docker Desktop 29.5.3

| 属性 | 值 |
|------|-----|
| 路径 | `D:\Docker\` |
| WSL 集成 | `docker-desktop` / `Ubuntu` |
| Docker Compose | v5.1.4 |

---

## 十、版本控制

### Git 2.53.0

| 属性 | 值 |
|------|-----|
| 路径 | `D:\Git\` |
| Git LFS | `D:\Git LFS\` |
| 用户 | 苏巷雨（`1368614311@qq.com`） |
| GitHub | `suxiangyu138` |
| GitHub CLI | `C:\Program Files\GitHub CLI\` |

---

## 十一、项目目录结构

### D:\本地核心开发项目\（主力项目）

```
本地核心开发项目\
├── FlavorDash              苍穹外卖魔改
├── LingShu                 小智医疗魔改
├── SuGuangMall             微服务商城魔改
├── 小智医疗魔改_LingShu
├── 微服务商城魔改_SuGuangMall
└── 苍穹外卖魔改_FlavorDash
```

### D:\本地开发项目\（练习与工具项目）

```
本地开发项目\
├── AI-Agent-Platform
├── LLMChatBot
├── Gomoku_sxy
├── Instant-Messaging-App-QQ-like
├── DocumentTypeConverter_sxy
├── mmwave-radar-head-pose-estimation
├── Python本地聊天机器人
├── 个人信息管理
├── 个人博客平台（练习）
└── ...
```

### D:\本地学习项目\（学习记录）

```
本地学习项目\
├── DailyThoughts
├── Java-AI-LLM-Learning-Records
├── Major-hard-skills
└── Soft_Skills_Learning
```

### D:\GitHub学习项目\（开源学习）

```
GitHub学习项目\
├── 学习资源
└── 实战项目
```

---

## 十二、中间件端口速查表

| 服务 | 端口 | | 服务 | 端口 |
|------|------|---|------|------|
| MySQL | 3306 | | Redis | 6379 |
| MongoDB | 27017 | | PostgreSQL | 5432（默认） |
| ElasticSearch | 9200 | | RabbitMQ | 5672 / 15672 |
| Kafka | 9092 | | ZooKeeper | 2181 |
| Nacos | 8848 | | Sentinel | 8080 |
| RocketMQ | 9876（NS） | | Nginx | 80 |
| Tomcat | 8080 | | Seata | 8091（默认） |
| Ollama | 11434 | | Docker API | npipe |

---

## 十三、常用操作速查

### Spring Boot 项目启动

```powershell
cd D:\本地核心开发项目\SuGuangMall
mvn clean package -DskipTests
java -jar target/*.jar
```

### 微服务全栈启动顺序

1. MySQL / Redis / Nacos
2. RabbitMQ / Kafka（按需）
3. 网关（Gateway）
4. 基础服务（Auth / System）
5. 业务服务

### Maven 换源

编辑 `D:\maven\apache-maven-3.9.16\conf\settings.xml`。

> 已配阿里云镜像：`https://maven.aliyun.com/repository/public`

### JDK 版本切换（IDEA 项目级）

`File → Project Structure → SDK → 选择 JDK 版本`

### 清理 Docker 空间

```bash
docker system prune -a
```

---

> **更新日期**：2026-06-24
