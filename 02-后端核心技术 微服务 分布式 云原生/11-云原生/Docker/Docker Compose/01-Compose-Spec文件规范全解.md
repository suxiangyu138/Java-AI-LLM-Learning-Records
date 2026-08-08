# 01-Compose Spec 文件规范全解
> Compose 文件的跨实现规范：顶层键、depends_on 三条件、profiles/extends/include、多文件合并规则——每个字段的精确语义

## 📚 目录
1. [文件命名与顶层键](#1-文件命名与顶层键)
2. [services：唯一必填键](#2-services唯一必填键)
3. [depends_on：三条件依赖控制](#3-depends_on三条件依赖控制)
4. [profiles：按需启用的可选服务](#4-profiles按需启用的可选服务)
5. [extends：配置复用](#5-extends配置复用)
6. [include：引入外部文件](#6-include引入外部文件)
7. [多文件合并规则（-f）](#7-多文件合并规则-f)
8. [x-* 扩展字段](#8-x--扩展字段)
9. [核心要点](#9-核心要点)
10. [参考来源](#10-参考来源)

## 1. 文件命名与顶层键

### 1.1 文件命名优先级

```text
compose.yaml（首选）> compose.yml > docker-compose.yaml > docker-compose.yml
（compose.yaml 与 compose.yml 同时存在时，优先 compose.yaml）
```

| 键 | 必填 | 用途 |
|----|:---:|------|
| `services` | ✅ | 定义应用的所有容器 |
| `networks` | ❌ | 定义容器可连接的网络 |
| `volumes` | ❌ | 定义持久化存储卷 |
| `configs` | ❌ | 定义挂载的配置文件（只读） |
| `secrets` | ❌ | 定义敏感数据（只读挂载） |
| `include` | ❌ | 引入外部 Compose 文件 |
| `name` | ❌ | 设置项目名（默认取目录名） |
| `version` | ❌ | **已废弃**，仅向后兼容，可省略 |
| `x-*` | ❌ | 用户自定义扩展（Compose 忽略） |

```yaml
# 现代写法：无 version 字段
name: my-app          # 项目名（影响容器/网络命名前缀）

services:
  app:
    image: my-app:1.0

networks:
  backend:

volumes:
  data:
```

## 2. services：唯一必填键

每个服务 = 一个容器（或一组相同配置的副本）。常用字段：

| 字段 | 说明 |
|------|------|
| `image` / `build` | 指定镜像或从 Dockerfile 构建（二选一或组合） |
| `container_name` | 容器名（⚠️ 指定后无法 scale 多副本） |
| `ports` | 端口映射 `"宿主机:容器"`（短语法）或长语法（含 protocol 等） |
| `expose` | 仅暴露给同网络服务，不映射宿主机 |
| `environment` | 环境变量（列表或 map 两种写法） |
| `env_file` | 从文件加载环境变量 |
| `volumes` / `configs` / `secrets` | 挂载配置（见 02/04） |
| `command` / `entrypoint` | 覆盖镜像默认启动命令 |
| `restart` | 重启策略 |
| `healthcheck` | 健康检查（见 03） |
| `depends_on` | 依赖控制（见第 3 节） |
| `profiles` | 归属的 profile 列表（见第 4 节） |
| `deploy` | 部署级配置（资源限制等，见 03） |

### 2.1 environment 两种写法

```yaml
services:
  app:
    # 写法 1：列表（值会原样传递，特殊字符安全）
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_URL=jdbc:mysql://db:3306/app
    # 写法 2：map（更紧凑；值可引用 .env 变量）
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-dev}   # 带默认值
      DB_PASSWORD: ${DB_PASSWORD}                              # 来自 .env
```

> 💡 map 写法支持 `${VAR:-default}` 默认值语法与变量插值（详见 [04](04-多环境与配置管理.md)）。

## 3. depends_on：三条件依赖控制

**短语法**（只控制启动顺序）：

```yaml
services:
  app:
    depends_on:
      - db
      - redis
```

**长语法**（控制"何时算就绪"）——Compose Spec 三种 condition：

| condition | 语义 | 场景 |
|-----------|------|------|
| `service_started` | 依赖容器**已启动**（默认） | 仅需顺序，不关心就绪 |
| `service_healthy` | 依赖容器**健康检查通过**后才启动 | **生产推荐**：Java 服务等 MySQL/Redis 就绪 |
| `service_completed_successfully` | 依赖容器**退出码为 0** | 一次性初始化/迁移服务（`init` 服务） |

```yaml
services:
  app:
    depends_on:
      db:
        condition: service_healthy
      init:
        condition: service_completed_successfully   # 等数据库迁移完成
  db:
    image: mysql:8.0
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
  init:
    image: flyway/flyway
    command: migrate
```

> ⚠️ **两条铁律**：① `service_started` 只保证"容器启动了"，不保证"服务可用了"——Java 连接失败要用 `service_healthy`；② **`depends_on` 与 `extends` 不兼容**——有依赖的服务不能作为 `extends` 的基类。

## 4. profiles：按需启用的可选服务

Profile 让可选服务**只在显式激活时才启动**——适合监控（Prometheus/Grafana）、管理 UI（pgAdmin/Adminer）、测试数据库、Mailhog、调试工具、mock 服务。

```yaml
services:
  web:
    image: my-app:1.0          # 无 profile：总是启动
  monitoring:
    image: grafana/grafana
    profiles: ["monitoring"]   # 仅激活 monitoring profile 时启动
  debug:
    image: debug-tools
    profiles: ["debug", "monitoring"]   # 属于多个 profile
```

```bash
docker compose --profile monitoring up        # web + monitoring
docker compose --profile debug --profile monitoring up   # 多 profile 组合
COMPOSE_PROFILES=dev docker compose watch     # 环境变量方式
```

| 优势 | 说明 |
|------|------|
| 一份文件搞定多场景 | 替代"注释/反注释服务"的痛点 |
| 单一事实源 | 不需要多个近重复的 Compose 文件 |
| 与 watch 配合 | `COMPOSE_PROFILES=dev docker compose watch` |

> ⚠️ 陷阱：`up` 时忘记指定 profile，这些服务不会启动——需要重新带 profile 运行。

## 5. extends：配置复用

共享公共配置（DRY）：

```yaml
# base.yaml（公共基础配置）
services:
  java-base:
    image: openjdk:17-jre-slim
    restart: unless-stopped
    networks: [backend]
```

```yaml
# compose.yaml
services:
  user-service:
    extends:
      file: base.yaml        # 从哪个文件继承
      service: java-base     # 继承哪个服务
    environment:
      SPRING_PROFILES_ACTIVE: prod
```

| 规则 | 说明 |
|------|------|
| 本地覆盖 | 当前文件中的字段覆盖被继承字段 |
| 限制 | 不能继承含 `depends_on`/`volumes_from` 的服务（与 depends_on 不兼容） |
| 替代方案 | YAML 锚点（`&anchor`/`*ref`）或 `include` 更灵活 |

## 6. include：引入外部文件

```yaml
# compose.yaml
include:
  - path: ../shared/redis.yaml      # 引入其他 Compose 文件
  - path: ../shared/mysql.yaml
    env_file: ../shared/.env        # 可选：给被引入文件配 env_file
    project_directory: ../shared    # 可选：相对路径基准

services:
  app:
    image: my-app:1.0
    depends_on:
      redis:
        condition: service_healthy
```

| 与 extends 的差异 | include | extends |
|-------------------|:---:|:---:|
| 引入的是完整文件 | ✅ | ❌（单个服务） |
| 被引入服务可被 depends_on | ✅ | ❌ |
| 被引入文件有独立项目名 | ✅（可配） | ❌ |

> ⚠️ **安全提示**：`include` 与 OCI 制品存在路径穿越 CVE（CVE-2025-62725），**v2.40.2+ 已修复**——使用 include 请升级 Compose。

## 7. 多文件合并规则（-f）

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

**合并规则**：

```text
① 按命令行顺序读取，后面的文件覆盖前面的
② 服务级合并：services 下的同名服务做字段级合并（不是整块替换）
③ 标量字段（image/command 等）：后文件覆盖前文件
④ 序列字段（ports/environment 等）：按索引合并（list 的 item 数量不同时以长文件为准）
⑤ 映射字段（volumes/networks/environment map）：键级合并
```

```yaml
# base.yml
services:
  app:
    image: my-app:1.0
    ports: ["8080:8080"]
    environment:
      SPRING_PROFILES_ACTIVE: dev

# prod.yml（覆盖）
services:
  app:
    image: my-app:1.0.0
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_PASSWORD: ${DB_PASSWORD}
```

```bash
docker compose -f base.yml -f prod.yml config   # 查看合并后的最终配置
```

> 💡 验证神器：`docker compose config` 输出合并后的完整配置；`--no-env-resolution` 可查看未做变量替换的原始配置。

## 8. x-* 扩展字段

```yaml
# x- 前缀字段被 Compose 忽略，但可在 YAML 锚点中复用
x-java-common: &java-common
  restart: unless-stopped
  networks: [backend]
  environment: &env-base
    TZ: Asia/Shanghai

services:
  user-service:
    <<: *java-common          # 合并锚点
    image: user-service:1.0
    environment:
      <<: *env-base
      SPRING_PROFILES_ACTIVE: prod
```

| 用途 | 说明 |
|------|------|
| 公共配置定义 | 结合 YAML 锚点实现跨服务复用 |
| 元数据存储 | 团队自定义说明字段 |
| 工具集成 | 其他工具读取 x- 字段做扩展 |

## 9. 核心要点

> 🎯 **核心要点**：
> - 现代 Compose 无 `version:` 字段；`compose.yaml` 为推荐文件名；
> - `depends_on` 三条件：started（顺序）/ healthy（就绪，生产必用）/ completed（一次性任务）；
> - `profiles` 把可选服务（监控/调试/测试库）藏进 profile，一份文件管多场景；
> - `extends` 继承单服务（禁 depends_on 基类）、`include` 引入整文件（可被依赖）；
> - 多文件 `-f` 合并：标量覆盖、序列按索引、映射按键合并——`docker compose config` 验证一切。

## 10. 参考来源

- [Compose Specification（官方规范全文）](https://compose-spec.io/)
- [Compose file 顶层键参考](https://docs.docker.com/compose/compose-file/)
- [Docker Compose 变更记录（version 字段废弃等）](https://bdteo.com/docker-compose-major-changes-since-october-2023/)

---

**下一模块**：[02-网络与卷高级配置](02-网络与卷高级配置.md)　/　**返回总览**：[00-总览](00-Docker%20Compose深化总览.md)
