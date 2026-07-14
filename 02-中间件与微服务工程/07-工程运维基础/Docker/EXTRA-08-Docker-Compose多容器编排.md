# 08 - Docker Compose 多容器编排

## 8.1 为什么需要 Docker Compose？

当应用由多个容器组成（Web + 数据库 + 缓存 + 消息队列...），手动一个个 `docker run` 非常繁琐。Docker Compose 用一个 YAML 文件定义所有服务，**一条命令管理整个应用栈**。

```
传统方式：                         Docker Compose：
docker run -d --name db ...       ┌──────────────────┐
docker run -d --name redis ...    │ docker-compose.yml│  ← 一个文件定义全部
docker run -d --name web ...      └──────────────────┘
docker run -d --name nginx ...
                                  一条命令：
                                  docker compose up -d
```

## 8.2 docker-compose.yml 结构

```yaml
version: "3.8"          # Compose 文件版本（Docker Compose v2 已不强制要求）

services:               # 定义所有服务（容器）
  web:                  # 服务名（自动成为容器名和 DNS 名）
    build: .            # 从当前目录的 Dockerfile 构建
    ports:
      - "8080:3000"
    environment:
      - NODE_ENV=production
    volumes:
      - ./app:/app
    depends_on:
      - db
      - redis

  db:
    image: mysql:8.0    # 使用现成镜像
    volumes:
      - db-data:/var/lib/mysql
    environment:
      - MYSQL_ROOT_PASSWORD=secret

  redis:
    image: redis:7-alpine

volumes:                # 声明命名卷
  db-data:

networks:               # 声明自定义网络
  default:              # 默认网络自动创建
```

## 8.3 核心命令

```bash
# -------- 启动 --------
# 前台启动（所有日志输出到终端，Ctrl+C 停止）
docker compose up

# 后台启动
docker compose up -d

# 后台启动并强制重新构建镜像
docker compose up -d --build

# 后台启动并拉取最新镜像
docker compose up -d --pull always

# -------- 停止/删除 --------
# 停止所有服务
docker compose stop

# 停止并删除所有容器、网络（不删除 Volume）
docker compose down

# 停止并删除所有容器、网络、Volume（⚠️ 数据会丢）
docker compose down -v

# 停止并删除所有容器、网络、Volume、镜像
docker compose down -v --rmi all

# -------- 查看 --------
# 查看运行中的服务状态
docker compose ps

# 查看所有服务日志
docker compose logs

# 查看特定服务日志并跟踪
docker compose logs -f web

# 查看服务使用的镜像和端口
docker compose images

# -------- 操作 --------
# 重启某个服务
docker compose restart web

# 重新构建并启动某个服务
docker compose up -d --build web

# 在运行中的服务中执行命令
docker compose exec web bash
docker compose exec db mysql -u root -p

# 运行一次性命令
docker compose run --rm web npm test

# 扩展服务实例
docker compose up -d --scale web=3

# -------- 构建 --------
# 仅构建镜像，不启动
docker compose build

# 构建时不使用缓存
docker compose build --no-cache
```

## 8.4 常用配置项详解

### build — 构建配置

```yaml
services:
  web:
    build:
      context: ./web              # Dockerfile 所在目录
      dockerfile: Dockerfile.prod # 指定 Dockerfile 文件名
      args:                       # 构建参数
        - NODE_ENV=production
        - APP_VERSION=1.2.3
      target: production          # 多阶段构建的目标阶段
      cache_from:                 # 缓存来源
        - myapp:latest
```

### environment 和 env_file

```yaml
services:
  app:
    # 方式1：直接写
    environment:
      - NODE_ENV=production
      - DB_HOST=db
      - DB_PORT=5432

    # 方式2：从文件加载（文件格式 KEY=VALUE）
    env_file:
      - .env
      - .env.production

    # 方式3：只传 key，值从宿主机环境变量获取
    environment:
      - NODE_ENV
      - API_KEY
```

### volumes — 数据卷配置

```yaml
services:
  app:
    volumes:
      # 命名卷
      - app-data:/app/data

      # Bind mount（宿主机相对路径）
      - ./src:/app/src

      # Bind mount（绝对路径）
      - /home/user/config:/app/config:ro

      # 匿名卷（覆盖 bind mount 的目录）
      - /app/node_modules

      # tmpfs
      - type: tmpfs
        target: /app/tmp
        tmpfs:
          size: 128m

volumes:
  app-data:           # 声明命名卷
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /data/app-data
```

### ports — 端口映射

```yaml
services:
  web:
    ports:
      # 短语法
      - "8080:80"
      - "8443:443"
      - "127.0.0.1:8080:80"      # 仅本地

      # 长语法
      - target: 80
        published: 8080
        protocol: tcp
        mode: host
```

### depends_on — 服务依赖

```yaml
services:
  web:
    depends_on:
      - db
      - redis

  # ⚠️ depends_on 只控制启动顺序，不等待服务就绪！
  # 如需等待数据库就绪，需要健康检查配合。
```

### 健康检查 + 服务就绪等待

```yaml
services:
  db:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: secret
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  web:
    build: .
    depends_on:
      db:
        condition: service_healthy   # 等待 db 健康检查通过
      redis:
        condition: service_started   # 仅等待启动（默认行为）
```

### 资源限制

```yaml
services:
  app:
    deploy:
      resources:
        limits:
          cpus: "1.5"
          memory: 512M
        reservations:
          cpus: "0.5"
          memory: 256M
```

### 重启策略

```yaml
services:
  app:
    restart: unless-stopped   # always | on-failure | unless-stopped | no
```

## 8.5 多环境配置

### 使用多个 Compose 文件

```bash
# 基础配置 + 开发环境覆盖
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# 基础配置 + 生产环境覆盖
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

```yaml
# docker-compose.yml（基础配置）
services:
  web:
    build: .
    ports:
      - "3000:3000"

# docker-compose.dev.yml（开发覆盖）
services:
  web:
    build:
      context: .
      target: development
    volumes:
      - ./src:/app/src
    environment:
      - NODE_ENV=development

# docker-compose.prod.yml（生产覆盖）
services:
  web:
    restart: always
    environment:
      - NODE_ENV=production
    deploy:
      resources:
        limits:
          memory: 512M
```

### 使用 --profile 按需启动

```yaml
services:
  web:
    build: .

  db:
    image: mysql:8.0

  phpmyadmin:
    image: phpmyadmin:latest
    profiles:
      - debug               # 只有指定 profile 才启动
    ports:
      - "8080:80"

  elasticsearch:
    image: elasticsearch:8.0
    profiles:
      - monitoring
      - debug               # 支持多个 profile
```

```bash
# 正常启动（不启动 debug 和 monitoring 的服务）
docker compose up -d

# 启动 debug profile 的服务
docker compose --profile debug up -d

# 启动多个 profile
docker compose --profile debug --profile monitoring up -d
```

## 8.6 网络配置

```yaml
services:
  frontend:
    build: ./frontend
    networks:
      - frontend-net       # 连接前端网络

  backend:
    build: ./backend
    networks:
      - frontend-net       # 同时连接两个网络
      - backend-net        # 后端网络

  db:
    image: mysql:8.0
    networks:
      - backend-net        # 仅在后端网络，前端无法访问

networks:
  frontend-net:
    driver: bridge
  backend-net:
    driver: bridge
    internal: true          # 禁止外部访问（db 只向后端暴露）
```

## 8.7 实战：完整的 Web 应用栈

```yaml
# docker-compose.yml
services:
  # ====== 反向代理 ======
  nginx:
    image: nginx:1.25-alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
      - static-files:/usr/share/nginx/html/static
    depends_on:
      - backend
    restart: unless-stopped
    networks:
      - app-net

  # ====== 后端 API ======
  backend:
    build: ./backend
    environment:
      - DB_HOST=db
      - DB_PORT=5432
      - DB_NAME=myapp
      - DB_USER=appuser
      - DB_PASSWORD=${DB_PASSWORD}       # 从 .env 读取
      - REDIS_URL=redis://redis:6379
    volumes:
      - static-files:/app/static
    depends_on:
      db:
        condition: service_healthy
      redis:
        condition: service_started
    restart: unless-stopped
    networks:
      - app-net

  # ====== 后台任务 ======
  worker:
    build: ./backend
    command: celery -A app worker -l info
    environment:
      - DB_HOST=db
      - REDIS_URL=redis://redis:6379
    depends_on:
      - db
      - redis
    restart: unless-stopped
    networks:
      - app-net

  # ====== 数据库 ======
  db:
    image: postgres:16-alpine
    volumes:
      - db-data:/var/lib/postgresql/data
    environment:
      - POSTGRES_DB=myapp
      - POSTGRES_USER=appuser
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U appuser -d myapp"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped
    networks:
      - app-net

  # ====== 缓存 ======
  redis:
    image: redis:7-alpine
    volumes:
      - redis-data:/data
    restart: unless-stopped
    networks:
      - app-net

# ====== 存储 ======
volumes:
  db-data:
  redis-data:
  static-files:

# ====== 网络 ======
networks:
  app-net:
    driver: bridge
```

配套的 `.env` 文件：

```env
DB_PASSWORD=SuperSecretPassword123
```

启动方式：

```bash
# 开发环境
docker compose up -d

# 生产环境
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

---

> **下一步**：[09-Docker实战项目](09-Docker实战项目.md)
