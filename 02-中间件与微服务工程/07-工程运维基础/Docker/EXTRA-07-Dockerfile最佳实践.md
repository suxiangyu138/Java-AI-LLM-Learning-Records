# 07 - Dockerfile 最佳实践

## 7.1 Dockerfile 是什么？

Dockerfile 是一个文本文件，包含一系列**指令**，每条指令在镜像上创建一个层。它是镜像的**声明式构建脚本**，保证了构建的**可复现性**。

## 7.2 核心指令详解

### FROM — 指定基础镜像

```dockerfile
# 语法
FROM <image>[:<tag>] [AS <stage-name>]

# 示例
FROM python:3.11-slim-bookworm
FROM node:20-alpine AS builder
FROM scratch          # 最基础的空镜像（用于静态编译的程序）
```

### RUN — 执行命令

```dockerfile
# Shell 形式（默认 /bin/sh -c）
RUN apt update && apt install -y curl

# Exec 形式（不会被 shell 解析，无变量替换）
RUN ["apt", "update"]

# 最佳实践：合并多个命令到一个 RUN，减少层数
RUN apt update && \
    apt install -y --no-install-recommends \
        curl \
        vim \
        git && \
    rm -rf /var/lib/apt/lists/*
```

### COPY vs ADD

```dockerfile
# COPY：复制本地文件到镜像（推荐）
COPY . /app
COPY --chown=1000:1000 app.py /app/

# ADD：功能更多，但不推荐滥用
ADD https://example.com/file.tar.gz /tmp/    # 自动下载
ADD archive.tar.gz /app/                      # 自动解压 tar 文件

# 规则：永远优先使用 COPY，只有需要自动解压时才用 ADD
```

### WORKDIR — 设置工作目录

```dockerfile
# 设置后续指令的工作目录（不存在则自动创建）
WORKDIR /app

# 推荐使用绝对路径，避免多次 CD
# ❌ 不推荐
RUN cd /app && pip install -r requirements.txt
# ✅ 推荐
WORKDIR /app
RUN pip install -r requirements.txt
```

### ENV vs ARG

```dockerfile
# ARG：构建时变量，镜像中不可见
ARG NODE_VERSION=20
FROM node:${NODE_VERSION}-alpine

# ENV：环境变量，构建时 + 运行时都可见
ENV NODE_ENV=production \
    PORT=3000

# ARG + ENV 结合使用
ARG APP_VERSION=1.0.0
ENV APP_VERSION=${APP_VERSION}
```

### EXPOSE — 声明端口

```dockerfile
# 声明容器监听的端口（仅文档作用，不实际映射端口）
EXPOSE 3000
EXPOSE 80/tcp
EXPOSE 80/udp

# 实际映射需要在 docker run 时使用 -p
```

### CMD vs ENTRYPOINT

```dockerfile
# CMD：容器启动时的默认命令（可被 docker run 后的命令覆盖）
CMD ["node", "server.js"]
CMD ["python", "-m", "http.server", "8000"]

# ENTRYPOINT：容器的主命令（不易被覆盖）
ENTRYPOINT ["docker-entrypoint.sh"]

# 组合使用（最佳实践）
ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["node", "server.js"]
# 效果等同于：docker-entrypoint.sh node server.js
# 用户可以覆盖 CMD 部分：docker run myapp npm test
# 效果等同于：docker-entrypoint.sh npm test
```

### USER — 切换用户

```dockerfile
# 创建非 root 用户并切换（安全最佳实践）
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser
# 后续所有 RUN, CMD, ENTRYPOINT 都以该用户执行
```

### VOLUME — 声明挂载点

```dockerfile
# 声明匿名卷（数据不会被 docker commit 提交）
VOLUME ["/data", "/var/log/app"]

# 注意：Dockerfile 中的 VOLUME 不能指定宿主机路径
# 具体映射路径在 docker run -v 时指定
```

## 7.3 多阶段构建 (Multi-stage Build)

这是 Dockerfile **最重要的最佳实践之一**。它将构建环境和运行环境分离，大幅减小最终镜像体积。

```dockerfile
# ============ 构建阶段 ============
FROM golang:1.21-alpine AS builder

WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download

COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -o /app/server .

# ============ 运行阶段 ============
FROM alpine:3.19

RUN apk add --no-cache ca-certificates tzdata
COPY --from=builder /app/server /usr/local/bin/server

USER 1000
EXPOSE 8080
ENTRYPOINT ["server"]
```

### 多阶段构建的优势

| 传统构建 | 多阶段构建 |
|---|---|
| 包含完整 SDK、编译工具 | 只包含运行时依赖 |
| 镜像 800MB+ | 镜像可能只有 10-20MB |
| 密钥可能在镜像层中残留 | 仅复制最终产物 |
| 需要额外脚本清理 | 一个 Dockerfile 搞定 |

### 前端构建示例

```dockerfile
# 阶段1：构建前端资源
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build

# 阶段2：用 Nginx 提供静态文件
FROM nginx:1.25-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

## 7.4 层缓存优化

Docker 构建时逐层缓存。**将不易变的层放前面，频繁变的放后面**。

```dockerfile
# ❌ 低效：代码变化 → COPY . . 缓存失效 → 后面全部重建
FROM node:20-alpine
WORKDIR /app
COPY . .                        # ← 代码一改，这层就失效
RUN npm ci                      # ← 每次都得重装
RUN npm run build

# ✅ 高效：充分利用缓存
FROM node:20-alpine
WORKDIR /app
COPY package*.json ./            # ← 只有依赖变了才失效
RUN npm ci                       # ← 缓存命中率高
COPY . .                         # ← 代码常变，但这层很快
RUN npm run build
```

### 缓存优化原则

```
变化频率     放在哪层
────────────────────────
基础系统依赖    最上层（几乎不变）
包管理器依赖    中上层（偶尔变）
应用源码复制    中下层（经常变）
构建命令        最下层（每次变）
```

## 7.5 镜像瘦身技巧

```dockerfile
# 1. 选择精简基础镜像
FROM python:3.11-slim     # ~50MB，而非 python:3.11 (~350MB)
FROM node:20-alpine       # ~50MB，而非 node:20 (~350MB)

# 2. 清理包管理器缓存
# Debian/Ubuntu
RUN apt update && apt install -y pkg && rm -rf /var/lib/apt/lists/*
# Alpine
RUN apk add --no-cache pkg

# 3. 安装时排除不必要的推荐包
RUN apt install -y --no-install-recommends pkg

# 4. 合并 RUN 命令减少层
RUN apt update && \
    apt install -y curl vim && \
    rm -rf /var/lib/apt/lists/*

# 5. 使用 .dockerignore
# 避免将 node_modules, .git, *.log 等复制到镜像

# 6. 使用多阶段构建（终极方案）
# 最终镜像只包含运行时需要的文件
```

### .dockerignore 示例

```
# .dockerignore 文件
.git
.gitignore
node_modules
npm-debug.log
Dockerfile
.dockerignore
.git
.env
*.md
.vscode
.idea
coverage
test
tests
*.test.js
```

## 7.6 一个完整的生产级 Dockerfile 模板

```dockerfile
# syntax=docker/dockerfile:1

# ============ 构建阶段 ============
FROM node:20-alpine AS builder

# 设置工作目录
WORKDIR /app

# 安装依赖（先复制包文件以利用缓存）
COPY package.json package-lock.json ./
RUN npm ci --only=production --ignore-scripts

# 复制源码并构建
COPY . .
RUN npm run build && \
    npm prune --production

# ============ 生产运行阶段 ============
FROM node:20-alpine

# 安装运行时依赖（如健康检查工具）
RUN apk add --no-cache tzdata curl

# 创建非 root 用户
RUN addgroup -g 1001 -S nodejs && \
    adduser -S nodejs -u 1001

# 设置工作目录
WORKDIR /app

# 从构建阶段复制产物
COPY --from=builder --chown=nodejs:nodejs /app/node_modules ./node_modules
COPY --from=builder --chown=nodejs:nodejs /app/dist ./dist
COPY --from=builder --chown=nodejs:nodejs /app/package.json ./

# 切换到非 root 用户
USER nodejs

# 声明端口
EXPOSE 3000

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:3000/health || exit 1

# 启动命令
ENTRYPOINT ["node", "dist/server.js"]
```

## 7.7 Dockerfile 指令速查

| 指令 | 说明 | 关键点 |
|---|---|---|
| `FROM` | 基础镜像 | 必须第一行，支持多阶段 `AS name` |
| `RUN` | 执行命令 | 产生新层，合并命令减少层数 |
| `COPY` | 复制文件 | 优先于 ADD |
| `ADD` | 复制+解压 | 只有需要自动解压 tar 才用 |
| `WORKDIR` | 设置工作目录 | 推荐用绝对路径 |
| `ENV` | 设置环境变量 | 运行时持久存在 |
| `ARG` | 构建参数 | 只在构建期有效 |
| `EXPOSE` | 声明端口 | 仅文档作用，需 -p 实际映射 |
| `CMD` | 默认启动命令 | 可被覆盖 |
| `ENTRYPOINT` | 主入口命令 | 不易被覆盖 |
| `USER` | 切换用户 | 安全最佳实践 |
| `VOLUME` | 声明匿名卷 | 防止数据丢失 |
| `HEALTHCHECK` | 健康检查 | 配合编排工具使用 |
| `SHELL` | 指定 shell | 改变 RUN 的默认 shell |

---

> **下一步**：[08-Docker-Compose多容器编排](08-Docker-Compose多容器编排.md)
