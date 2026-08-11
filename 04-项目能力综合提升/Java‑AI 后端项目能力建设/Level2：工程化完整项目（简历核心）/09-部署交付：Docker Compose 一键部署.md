# 09 部署交付：Docker Compose 一键部署

> 交付保障的第二块：「能演示」的落地——换一台干净机器，一条命令拉起 MySQL、Redis、Milvus、RabbitMQ 与应用。本模块用多阶段 Dockerfile + Compose v5 全栈编排 + 一键脚本，把「在我机器上能跑」变成「在任何机器上 10 分钟能跑」。

## 📚 目录

1. [动机：一键部署是「能演示」的前提](#1-动机一键部署是能演示的前提)
2. [Dockerfile 多阶段构建](#2-dockerfile-多阶段构建)
3. [Compose v5 全栈编排](#3-compose-v5-全栈编排)
4. [配置外置与启动顺序](#4-配置外置与启动顺序)
5. [数据持久化与健康检查](#5-数据持久化与健康检查)
6. [一键启动脚本与演示检查单](#6-一键启动脚本与演示检查单)
7. [CI-CD 流水线](#7-ci-cd-流水线)
8. [验证实验](#8-验证实验)
9. [核心要点](#9-核心要点)

---

## 1. 动机：一键部署是「能演示」的前提

Level1 的演示依赖「我的机器 + 我的环境」——换台机器要装 JDK、配 Maven、建库建表、起 Redis，折腾半天还未必成功。面试或答辩时现场部署翻车，前面所有的工程质量全部归零。一键部署的目标：**一台只有 Docker 的干净机器，执行一条命令，10 分钟内全栈可用**。两个层面的收益：**演示层面**——「换台机器 docker compose up 就起来了」是「工程化」最直观的证明，面试官可以现场验；**工程层面**——环境统一（所有人跑同一套镜像，消灭「在我机器上是好的」）、配置外置（密钥与参数不写死在代码里）、可复制（线上环境 = Compose 文件，不是神秘的手工步骤）。容器化的心智转变：**从「部署一个应用」变成「部署一套系统」**——数据库、缓存、消息队列、向量库、应用、监控全部是环境的一部分，随 Compose 一起交付。

## 2. Dockerfile 多阶段构建

Java 应用镜像的标准做法是**多阶段构建**：第一阶段用 Maven 镜像编译打包，第二阶段用精简 JRE 镜像运行——最终镜像只含运行产物，不含编译工具链，体积从 500MB+ 降到 150MB 左右，启动更快、攻击面更小：

```dockerfile
# 阶段一：构建
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline          # 先拉依赖，利用层缓存
COPY . .
RUN mvn -pl kb-api -am package -DskipTests

# 阶段二：运行
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/kb-api/target/kb-api.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
```

三个要点：**`.dockerignore` 必须写**——排除 `target/`、`.git/`、`*.iml`，否则构建上下文把几百 MB 的编译产物打进镜像（构建慢且镜像脏）；**层缓存策略**——先 COPY pom.xml 再跑依赖拉取，pom 不变时复用缓存层，构建从 5 分钟降到 30 秒；**JVM 容器内存感知**——`-XX:MaxRAMPercentage=75` 让 JVM 按容器配额算堆内存，否则 JVM 按宿主机内存算，小容器里直接 OOM——这是 Java 容器化第一大坑。

镜像的供应链细节同样决定演示成败：**镜像 tag 一律显式锁定**（`milvusdb/milvus:v3.0.0`、`mysql:8.4`），不用 `latest`——latest 在演示当天悄悄变了行为是「现场翻车」的经典来源；**应用镜像 tag 用 commit hash**（`kb-api:20260811-abc123`），回滚 = 换 tag 重启，而不是「重新构建碰运气」；**镜像仓库选公开的 Docker Hub 即可**——演示项目不需要私有仓库，讲清「公开仓库不适合放生产密钥」这个边界反而是加分项，与 09 篇「密钥走 .env」的配置外置原则互相印证。

## 3. Compose v5 全栈编排

Compose v5（2026 基准，`version` 字段已废弃）一份 `compose.yaml` 编排全部组件，服务清单与用途：

| 服务 | 镜像 | 端口 | 说明 |
|---|---|---|---|
| mysql | mysql:8.4 | 3306 | 业务库，初始化 SQL 挂载 |
| redis | redis:8 | 6379 | 缓存与限流 |
| etcd + minio | quay.io/coreos/etcd + minio/minio | 2379 / 9000 | Milvus 3.0 的依赖组件 |
| milvus | milvusdb/milvus:v3.0.0 | 19530 | 向量库 standalone |
| rabbitmq | rabbitmq:4.3-management | 5672 / 15672 | 消息队列（含管理台） |
| kb-api | 自建镜像（build: .） | 8080 | 应用本体 |
| prometheus | prom/prometheus | 9090 | 08 篇监控抓取（可选） |
| grafana | grafana/grafana | 3000 | 08 篇可视化（可选） |

编排的两个关键点：**依赖顺序用 `depends_on` 的条件模式**——`condition: service_healthy`，应用等 MySQL/Redis/Milvus/RabbitMQ 健康后再启动，而不是简单地「启动顺序」（容器启动 ≠ 服务就绪，没健康检查的编排是碰运气）；**环境变量用 `env_file` 或 `environment` 外置**——数据库密码、AI API Key 走 `.env` 文件（加入 `.gitignore`），Compose 自动读取，代码与配置分离。

## 4. 配置外置与启动顺序

配置外置三原则：**密钥不进代码**——API Key、数据库密码只在 `.env` / 环境变量 / Docker Secrets 里；**环境差异用 profile 表达**——`application-dev.yml`（本地）与 `application-prod.yml`（容器，指向 compose 网络内的服务名 `mysql:3306`、`milvus:19530`）——**容器内外地址不同是最高频的配置坑**：本地连 localhost，容器里要连服务名；**启动顺序与失败处理**——应用启动时任何中间件不可用都要快速失败并明确报错（哪个服务没起来、怎么修），而不是卡在连接重试里——compose 日志里服务名就是线索，`docker compose ps` 看健康状态是排障第一命令。

## 5. 数据持久化与健康检查

两个「演示时最尴尬」的坑必须提前防：**数据卷持久化**——MySQL、Milvus（etcd/minio）、Redis 的数据目录必须挂 named volume，否则 `docker compose down` 后数据全丢，演示完重启知识库没了；**健康检查**——每个服务配 `healthcheck`（如 MySQL 的 `mysqladmin ping`、Milvus 的 `milvus-cli` 或 TCP 探活），应用依赖健康才启动。日志处理：应用日志走 stdout（Docker 自动收集），`logging.driver: json-file` + `max-size: 50m` 限制体积，防止演示机器磁盘被日志灌满——**磁盘满是最容易忽略的演示事故**。

## 6. 一键启动脚本与演示检查单

一键脚本是演示的「门面」：

```bash
# start.sh —— 演示一条龙
cp .env.example .env            # 首次：生成配置（含 API Key 提示）
docker compose up -d --build    # 构建并后台启动全栈
./scripts/wait-healthy.sh       # 轮询各服务健康状态（应用就绪为止）
./scripts/seed-demo.sh          # 灌入演示数据（示例文档 + 向量化）
echo "演示环境就绪: http://localhost:8080"
```

配套 **演示检查单**（每页演示前过一遍）：应用健康页 200；示例文档已入库（Milvus 能检索到）；Redis 有缓存 key；问答接口流式返回正常；Grafana 面板有数据；`docker compose ps` 全 healthy。**演示前 10 分钟按检查单走一遍，比现场救火强一百倍**——检查单本身就是工程习惯的体现，面试可以主动讲「我准备了演示检查单，防止现场翻车」。

排障三连命令写进 README 的「故障速查」：`docker compose ps`（哪个服务没起来/不健康）→ `docker compose logs kb-api`（应用日志，配 grep traceId）→ `docker compose restart kb-api`（兜底重启）。演示现场 90% 的问题靠这三条解决——文档里写明排障路径，本身就是工程习惯的体现；同时这些命令也解释了「为什么 Compose 里每服务都要配健康检查」——没有健康状态，`ps` 的输出就无法告诉你该修谁。

## 7. CI-CD 流水线

一键部署解决「本机能跑」，CI-CD 解决「提交的代码能跑」：GitHub Actions 流水线三步——**构建**（`mvn verify`，07 篇的测试与覆盖率门禁在 CI 强制执行，覆盖不达标 PR 不合并）；**镜像构建与推送**（多阶段 Dockerfile 构建后推 Docker Hub/GitHub Container Registry，打 commit hash 标签）；**部署**（SSH 到演示服务器执行 `docker compose pull && docker compose up -d`，或接 Webhook 手动触发）。范围要克制：**本项目 CI 做到「测试门禁 + 镜像构建」就够**——全自动部署到线上有回滚风险，先保证「合并的代码一定是测试通过的」，这是学生项目与生产项目的合理分界，讲清这个边界反而是加分项。

## 8. 验证实验

验收实验：**清环境验证**——用干净 Docker 环境（或 `docker compose down -v` 清掉全部数据卷），执行一键脚本，计时确认 10 分钟内应用就绪；**检查单走查**——按演示检查单逐项打勾，确认每项通过；**持久化验证**——`docker compose down`（保留数据卷）再 `up`，确认文档数据还在。三个实验都过了，M3 里程碑「换台机器 10 分钟拉起」验收通过——这个实验的通过本身就是「能演示」的证明。

## 9. 核心要点

1. 一键部署 = 能演示：干净机器 + 一条命令 + 10 分钟全栈可用，是工程化的直观证明。
2. 多阶段 Dockerfile（编译/运行分离）小体积，`.dockerignore` 必写，JVM 用 MaxRAMPercentage 感知容器内存。
3. Compose v5 全栈编排，`depends_on` 用 service_healthy 条件，配置 env_file 外置、profile 区分环境。
4. 数据卷持久化 + 每服务 healthcheck + 日志限体积——演示三大坑提前防。
5. 一键脚本 + 演示检查单是演示门面，10 分钟过一遍防现场翻车。
6. CI 做到「测试门禁 + 镜像构建」，全自动部署留到生产需求再说。

> 🎯 **核心要点**：部署交付的本质是**「把环境变成代码」**——MySQL、Milvus、RabbitMQ 不再是「每台机器手装的东西」，而是 compose.yaml 里可复现的声明；「在我机器上能跑」从此变成「在任何机器上一条命令能跑」。这是简历项目「能演示」的最终落地。

---

**下一模块**：[10 AI 深度与项目验收](./10-AI%20深度与项目验收.md) | **返回总览**：[Level2 总览](./00-Level2%20工程化完整项目（简历核心）总览.md)
