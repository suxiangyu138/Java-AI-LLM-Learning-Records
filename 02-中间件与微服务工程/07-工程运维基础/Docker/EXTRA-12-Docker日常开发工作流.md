# 12 - Docker 日常开发工作流

> 将 Docker 融入真实开发流程的实操手册。

## 12.1 本地开发环境搭建

### 启动开发栈（一键启动所有依赖）

```bash
# 方式1：纯 Compose
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# 方式2：搭配 Makefile
make dev
```

### 典型开发 Compose 配置

```yaml
# docker-compose.dev.yml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_PASSWORD: devpass
      POSTGRES_DB: myapp_dev
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
      - ./db/init.sql:/docker-entrypoint-initdb.d/init.sql:ro

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  mailhog:                        # 邮件测试工具
    image: mailhog/mailhog
    ports:
      - "1025:1025"               # SMTP
      - "8025:8025"               # Web UI

  minio:                          # S3 兼容对象存储
    image: minio/minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio-data:/data

volumes:
  pgdata:
  minio-data:
```

### Makefile 快捷命令

```makefile
.PHONY: dev up down logs shell test build

# 启动开发环境
dev:
	docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
	@echo "App: http://localhost:3000"
	@echo "MailHog: http://localhost:8025"
	@echo "MinIO: http://localhost:9001"

# 停止
down:
	docker compose down

# 查看日志
logs:
	docker compose logs -f

# 进入应用容器
shell:
	docker compose exec app bash

# 运行测试
test:
	docker compose exec app pytest

# 重新构建
build:
	docker compose build --no-cache

# 数据库操作
db-reset:
	docker compose down -v
	docker compose up -d db
	sleep 5
	docker compose exec app flask db upgrade

db-migrate:
	docker compose exec app flask db migrate -m "$(msg)"
	docker compose exec app flask db upgrade

# 清理
clean:
	docker compose down -v --rmi all
	docker system prune -f
```

---

## 12.2 进入容器内调试

### 典型场景

```bash
# 场景1：应用出 bug 了，进容器看看
docker compose exec app bash
# 进去了：ps aux, cat 配置文件, curl localhost:3000, strace -p 1

# 场景2：数据库连不上，验证一下
docker compose exec app bash
$ nc -zv db 5432          # 端口是否通
$ nslookup db              # DNS 是否解析

# 场景3：用临时容器连数据库
docker run --rm -it \
  --network myapp_default \
  postgres:16-alpine \
  psql -h db -U appuser -d myapp_dev

# 场景4：临时 Redis CLI
docker run --rm -it \
  --network myapp_default \
  redis:7-alpine \
  redis-cli -h redis

# 场景5：调试网络问题，启动一个带全套工具的临时容器
docker run --rm -it \
  --network myapp_default \
  nicolaka/netshoot
```

### netshoot 常用命令

```bash
# netshoot 预装了：ping, curl, wget, dig, nslookup, nc, nmap,
#                 tcpdump, iperf, iptables, iproute2, netstat, ss...
docker run --rm -it --network host nicolaka/netshoot

# 在 netshoot 里：
$ dig db                          # DNS 解析
$ curl -v http://web:3000/health  # HTTP 测试
$ nc -zv db 5432                  # TCP 端口测试
$ tcpdump -i eth0 port 5432       # 抓包分析
$ iperf3 -c web                   # 带宽测试
```

---

## 12.3 热重载开发

### Node.js (nodemon)

```dockerfile
# Dockerfile.dev
FROM node:20-alpine
WORKDIR /app
RUN npm install -g nodemon
COPY package*.json ./
RUN npm ci
COPY . .
CMD ["nodemon", "--legacy-watch", "src/index.js"]
```

```yaml
# docker-compose.dev.yml
services:
  app:
    build:
      context: .
      dockerfile: Dockerfile.dev
    volumes:
      - ./src:/app/src       # 源码变化 nodemon 自动重启
      - /app/node_modules    # 防止宿主机覆盖
    ports:
      - "3000:3000"
```

### Python (Flask debug mode)

```yaml
services:
  app:
    build: .
    command: flask run --host=0.0.0.0 --port=5000 --debug
    volumes:
      - ./app:/app/app
    environment:
      - FLASK_DEBUG=1
    ports:
      - "5000:5000"
```

### Go (Air / CompileDaemon)

```yaml
services:
  app:
    build:
      context: .
      dockerfile: Dockerfile.dev
    volumes:
      - .:/app
    ports:
      - "8080:8080"

# Dockerfile.dev
# FROM golang:1.21-alpine
# RUN go install github.com/cosmtrek/air@latest
# CMD ["air"]
```

### 前端 (Vite / Webpack HMR)

```yaml
services:
  frontend:
    image: node:20-alpine
    working_dir: /app
    command: npx vite --host 0.0.0.0
    volumes:
      - ./frontend:/app
      - /app/node_modules
    ports:
      - "5173:5173"    # Vite 默认端口
```

---

## 12.4 与其他开发者共享环境

### 方法一：共享 Compose 文件

```bash
# 项目放到 Git 里，新成员 clone 后直接
docker compose up -d
# 所有人得到完全一致的环境
```

### 方法二：将 Compose 部署到远程开发机

```bash
# 在远程机器上
docker context create remote-dev \
  --docker "host=ssh://dev@dev-server.company.com"
docker context use remote-dev

# 现在本地的 docker compose 命令操作的是远程机器
docker compose up -d     # 容器跑在远程开发机上
docker compose logs -f   # 看远程容器日志
```

### 方法三：VS Code Dev Containers

```json
// .devcontainer/devcontainer.json
{
  "name": "MyApp Dev",
  "dockerComposeFile": "../docker-compose.dev.yml",
  "service": "app",
  "workspaceFolder": "/app",
  "customizations": {
    "vscode": {
      "extensions": [
        "ms-python.python",
        "ms-python.vscode-pylance"
      ]
    }
  }
}
```

```bash
# VS Code 中：Ctrl+Shift+P → "Dev Containers: Reopen in Container"
# 整个 VS Code 环境跑在 Docker 容器里
```

---

## 12.5 环境变量管理策略

### 分层 .env 文件

```
.env                    ← 通用配置模板（提交到 Git）
.env.example            ← 复制一份，填上真实值（不提交）
.env.local              ← 本地覆盖（不提交）
.env.production         ← 生产环境值（通过 CI/Secret 注入，不提交）
```

```bash
# .gitignore
.env
.env.local
.env.*.local
.env.production
```

### Compose 中使用

```yaml
services:
  app:
    env_file:
      - .env                  # 基础
      - .env.local            # 本地覆盖（优先级更高）
    environment:
      - SECRET_KEY=${SECRET_KEY:?err}   # 必须提供
      - LOG_LEVEL=${LOG_LEVEL:-info}    # 默认 info
```

---

## 12.6 数据库管理与迁移

### 初始化脚本（MySQL/PostgreSQL）

```sql
-- db/init.sql — 放在 docker-entrypoint-initdb.d/ 自动执行
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

INSERT INTO users (email) VALUES ('admin@example.com');
```

```yaml
services:
  db:
    image: postgres:16-alpine
    volumes:
      - ./db/init.sql:/docker-entrypoint-initdb.d/01-init.sql:ro
      - ./db/seed.sql:/docker-entrypoint-initdb.d/02-seed.sql:ro
```

### 数据库备份恢复

```bash
# PostgreSQL 备份
docker compose exec db pg_dump -U appuser myapp_dev > backup.sql

# PostgreSQL 恢复
docker compose exec -T db psql -U appuser myapp_dev < backup.sql

# MySQL 备份
docker compose exec db mysqldump -u root -psecret myapp > backup.sql

# MySQL 恢复
docker compose exec -T db mysql -u root -psecret myapp < backup.sql

# 定时备份（cron）
# 0 3 * * * cd /opt/myapp && docker compose exec -T db \
#   pg_dump -U appuser myapp > /backups/$(date +\%Y\%m\%d).sql
```

---

## 12.7 开发 → 测试 → 生产 一致性

```
开发环境                    │ 测试环境                   │ 生产环境
───────────────────────────┼──────────────────────────┼──────────────────────────
docker compose up -d       │ CI 自动构建镜像           │ docker compose up -d
  - 热重载                 │   - 跑单元测试             │   - 固定版本标签
  - 挂载源码               │   - 跑集成测试             │   - 资源限制
  - 开发工具链             │   - 安全扫描               │   - 健康检查
  - debug 模式             │   - 生成报告               │   - 日志收集
                           │                           │
同一份 Dockerfile（多阶段构建），同一个 Compose 结构，仅覆盖配置不同。
```

```bash
# 本地模拟生产环境
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# 验证：
# 1. 是否用非 root 用户运行
docker compose exec app whoami

# 2. 健康检查是否正常
docker compose ps

# 3. 资源限制是否生效
docker stats --no-stream
```

---

## 12.8 日常维护清单

### 每天

```bash
# 启动开发环境
docker compose up -d
```

### 每周

```bash
# 查看磁盘空间
docker system df

# 清理构建缓存
docker builder prune

# 拉取更新后的基础镜像
docker compose pull
```

### 每月

```bash
# 大扫除
docker system prune -a --volumes -f

# 检查镜像是否有漏洞
trivy image myapp:latest

# 检查存储驱动
docker info | grep "Storage Driver"
```

---

> **相关文档**：[08-Docker-Compose多容器编排](08-Docker-Compose多容器编排.md) | [09-Docker实战项目](09-Docker实战项目.md) | [11-Docker生态工具链详解](11-Docker生态工具链详解.md)
