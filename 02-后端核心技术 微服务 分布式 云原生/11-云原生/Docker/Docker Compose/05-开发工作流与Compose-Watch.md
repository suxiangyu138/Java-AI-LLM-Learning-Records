# 05-开发工作流与 Compose Watch
> 摆脱 down/build/up 三步曲：Compose Watch 热重载的三种 action、dev/prod 分离、调试与测试工作流

## 📚 目录
1. [开发工作流演进](#1-开发工作流演进)
2. [Compose Watch 三种 action](#2-compose-watch-三种-action)
3. [watch 配置详解](#3-watch-配置详解)
4. [dev/prod 分离模式](#4-devprod-分离模式)
5. [Java 开发工作流实战](#5-java-开发工作流实战)
6. [调试与测试工作流](#6-调试与测试工作流)
7. [已知限制与避坑](#7-已知限制与避坑)
8. [核心要点](#8-核心要点)
9. [参考来源](#9-参考来源)

## 1. 开发工作流演进

```text
传统方式（每次改动）：
  docker compose down → docker compose up -d --build → 等待重建（45s+）

Watch 方式（持续开发）：
  docker compose watch → 文件变化自动同步/重建（<2s）
```

| 对比 | 传统 build/up | Compose Watch |
|------|--------------|---------------|
| 改动反馈 | 45s+（全量重建） | <2s（sync 同步） |
| 操作 | 手动 down/build/up | 自动监听 |
| 上下文 | 丢失 | 持续运行 |
| 适用 | CI/CD、发布 | **本地开发** |

> 🎯 **核心价值**：把"容器内开发"体验拉回"本地开发"级别——文件保存即生效，同时保留容器的环境一致性。

## 2. Compose Watch 三种 action

| action | 行为 | 适用文件 | 场景 |
|--------|------|---------|------|
| `sync` | 把变更文件**复制进运行中的容器**（不重建） | 源码（热重载框架可自动生效） | Next.js/Vite/webpack HMR、nodemon、devtools 自动重启 |
| `rebuild` | 触发**完整镜像重建**并重启容器 | 依赖清单/Dockerfile | package.json、pom.xml、requirements.txt、Dockerfile 变更 |
| `sync+restart` | 同步文件并**重启服务** | 启动时读取一次的配置 | .env、nginx.conf、application.yml |

```yaml
services:
  web:
    build: .
    develop:
      watch:
        - path: ./src
          action: sync            # 源码：同步进容器（HMR 自动生效）
          target: /app/src        # 同步目标路径
        - path: ./package.json
          action: rebuild         # 依赖清单：重建镜像
        - path: ./nginx.conf
          action: sync+restart    # 启动时读取的配置：同步 + 重启
```

> ⚠️ **2025-2026 增强**：`initial_sync: full` 在 watch 启动时立即全量同步（之前要等第一次文件变化）；`docker compose watch --prune` 自动清理 watch 期间的未用资源。

## 3. watch 配置详解

```yaml
services:
  app:
    build: .
    develop:
      watch:
        # 1. 基础：同步目录（排除忽略项）
        - path: ./src
          action: sync
          target: /app/src
          ignore:
            - "**/node_modules/**"     # 排除目录（glob 模式）
            - "**/*.tmp"

        # 2. 依赖清单变化 → 重建
        - path: ./pom.xml
          action: rebuild

        # 3. 配置变化 → 同步 + 重启
        - path: ./application.yml
          action: sync+restart
          target: /app/config/application.yml
```

| 字段 | 说明 |
|------|------|
| `path` | 监听的宿主机路径（相对 compose 文件目录） |
| `action` | sync / rebuild / sync+restart 三选一 |
| `target` | sync 时容器内的目标路径（默认与 path 相同） |
| `ignore` | glob 忽略模式（node_modules、构建产物等） |
| `initial_sync` | `full` = watch 启动时全量同步（v2.32+） |

```bash
docker compose watch                  # 启动 watch（前台）
COMPOSE_PROFILES=dev docker compose watch   # 与 profiles 组合
docker compose watch --prune          # 自动清理未用资源
```

## 4. dev/prod 分离模式

### 模式 A：watch 放在 dev profile 后面

```yaml
services:
  app:
    build: .
    profiles: ["dev"]                 # 生产不加载此服务配置
    develop:
      watch:
        - path: ./src
          action: sync
          target: /app/src
```

```bash
COMPOSE_PROFILES=dev docker compose watch   # 开发：热重载
docker compose up -d                         # 生产：无 watch 配置
```

### 模式 B：独立 dev 覆盖文件（推荐）

```yaml
# compose.dev.yaml
services:
  app:
    develop:
      watch:
        - path: ./src
          action: sync
          target: /app/src
        - path: ./pom.xml
          action: rebuild
```

```bash
docker compose -f compose.yaml -f compose.dev.yaml watch
docker compose -f compose.yaml -f compose.prod.yaml up -d   # 生产不受影响
```

> 🎯 原则：**watch 配置永远不随生产文件发布**——用 profile 或独立 dev 文件隔离。

## 5. Java 开发工作流实战

### 5.1 Spring Boot 热重载组合

```yaml
# compose.dev.yaml
services:
  app:
    build: .
    develop:
      watch:
        - path: ./src
          action: sync
          target: /app/src
        - path: ./pom.xml
          action: rebuild
    environment:
      JAVA_TOOL_OPTIONS: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
    ports:
      - "5005:5005"
```

| 热重载方案 | 配合方式 |
|-----------|---------|
| spring-boot-devtools | 源码 sync 后 devtools 自动重启（classpath 变化检测） |
| JRebel | 字节码热替换（无需重启） |
| 远程调试 | `docker compose exec app jdb -attach 5005` 或 IDEA 远程调试 |

```yaml
# 依赖 spring-boot-devtools（pom.xml）
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

### 5.2 前端 + 后端全栈

```yaml
services:
  frontend:
    build: ./frontend
    develop:
      watch:
        - path: ./frontend/src
          action: sync              # Vite HMR 自动生效
        - path: ./frontend/package.json
          action: rebuild
  backend:
    build: ./backend
    develop:
      watch:
        - path: ./backend/src
          action: sync
        - path: ./backend/pom.xml
          action: rebuild
```

## 6. 调试与测试工作流

### 6.1 调试容器

```bash
docker compose exec app bash               # 进入容器
docker compose exec app jps                # 查看 JVM 进程
docker compose exec app jcmd 1 Thread.print   # 线程转储
docker compose run --rm --service-ports app bash   # 一次性调试（带端口）
```

### 6.2 测试工作流

```yaml
# 测试 profile：测试数据库 + mock 服务
services:
  test-db:
    image: postgres:16-alpine
    profiles: ["test"]
    environment:
      POSTGRES_DB: app_test
```

```bash
docker compose --profile test up -d test-db      # 起测试依赖
# 本地跑测试（JUnit/Testcontainers）
mvn test
# 或在容器内跑
docker compose run --rm app mvn test
```

### 6.3 与 Testcontainers 配合

```text
Testcontainers 在测试中动态起容器（数据库/消息队列）
Compose Watch 管理开发环境（常驻服务）
两者互补：开发用 Compose，测试用 Testcontainers
```

## 7. 已知限制与避坑

| # | 限制/坑 | 说明 |
|---|---------|------|
| 1 | **仅 build 服务生效** | watch 只适用于有 `build` 属性的服务；纯 `image` 服务被静默忽略 |
| 2 | sync 需要容器内工具 | 目标镜像需含 `stat`/`mkdir`/`rmdir`（distroless 镜像需用 slim/alpine 开发变体） |
| 3 | 依赖清单别用 sync | package.json/pom.xml 等要用 `rebuild`——sync 不会更新依赖 |
| 4 | 双向绑定 vs 单向同步 | watch 是**单向**（宿主→容器）；要双向用 bind mount（但 macOS/Windows 有文件系统性能损耗） |
| 5 | 编辑器临时文件 | 配置 `ignore` 过滤 `*.swp`、`*.tmp` 等 |
| 6 | 生产环境误用 | watch 配置必须隔离在 dev profile/dev 文件（见第 4 节） |

## 8. 核心要点

> 🎯 **核心要点**：
> - watch 三 action 选择：**源码→sync（热重载框架自动生效）、依赖清单→rebuild、启动期配置→sync+restart**；
> - `initial_sync: full` 让 watch 启动即同步，不用等第一次改动；
> - watch 配置用 profile 或独立 dev 文件隔离，**永不进生产**；
> - Java 组合拳：devtools（自动重启）+ 远程调试 5005 + pom 变化 rebuild；
> - 限制三连：仅 build 服务、distroless 缺工具、依赖清单必须 rebuild；
> - 开发用 Compose Watch，测试用 Testcontainers，各司其职。

## 9. 参考来源

- [Docker Compose Watch 官方文档](https://docs.docker.com/compose/file-watch/)
- [Compose Watch 实战（热重载工作流）](https://www.itnotetk.com/2026/04/29/docker-compose-watch-hot-reload-workflow/)
- [Compose 开发工作流（官方教程）](https://docs.docker.com/compose/how-tos/lifecycle/)

---

**下一模块**：[06-生产实践与故障排查](06-生产实践与故障排查.md)　/　**返回总览**：[00-总览](00-Docker%20Compose深化总览.md)
