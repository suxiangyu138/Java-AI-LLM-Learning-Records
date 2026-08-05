# 10 - GitHub Codespaces 与云开发环境

> 浏览器中的完整开发环境——VS Code + 终端 + 端口转发 + 预配置的依赖环境。Codespaces 让"克隆即开发"成为现实，彻底消除"在我机器上能跑"的问题。

---

## 目录

1. [Codespaces 概述](#1-codespaces-概述)
2. [创建与管理 Codespace](#2-创建与管理-codespace)
3. [devcontainer.json 配置详解](#3-devcontainerjson-配置详解)
4. [预构建与性能优化](#4-预构建与性能优化)
5. [Java 开发环境实战配置](#5-java-开发环境实战配置)
6. [端口转发与调试](#6-端口转发与调试)
7. [费用与资源管理](#7-费用与资源管理)
8. [常见面试题](#8-常见面试题)

---

## 1. Codespaces 概述

### 1.1 什么是 Codespaces

> GitHub Codespaces = 托管在云端的 VS Code 开发环境。通过浏览器或本地 VS Code 连接，获得一个完整的 Linux 容器——预装了运行时、依赖、工具和扩展。

```
传统开发：                       Codespaces：
┌────────────────┐              ┌────────────────┐
│ 本地安装：       │              │  浏览器打开      │
│ JDK 17          │              │  → 30秒可用     │
│ Maven           │              │  → Java 17 已装  │
│ MySQL           │              │  → Maven 已配    │
│ IDE 插件...     │              │  → 插件已装       │
│                 │              │  → 端口已转发     │
│ 折腾半天 → 报错  │              │  → 即刻开始编码   │
└────────────────┘              └────────────────┘
```

### 1.2 核心特性

| 特性 | 说明 |
|------|------|
| **一键启动** | Code 按钮 → Codespaces → New |
| **可配置** | `devcontainer.json` 定义环境 |
| **预构建** | 提前构建好，连接时秒启动 |
| **端口转发** | 自动转发 `localhost:8080` 到公网 |
| **Dotfiles** | 自动加载个人配置（别名、shell 主题） |
| **VS Code 扩展** | 自动安装指定扩展 |
| **持久化** | `/workspaces` 下的文件持久存储 |
| **多 IDE** | 浏览器 VS Code / 本地 VS Code / JetBrains Gateway |

---

## 2. 创建与管理 Codespace

### 2.1 创建方式

```
方式一：从仓库创建（Web）
  GitHub 仓库页面 → Code 按钮 → Codespaces tab → Create codespace on main

方式二：从 PR 创建
  PR 页面 → Code → Create codespace on pr-branch
  → 无需 Clone 即可 Review + 测试 PR 代码

方式三：从模板创建
  https://github.com/codespaces → New → 选择模板

方式四：通过 gh CLI
  gh codespace create --repo user/repo --branch feature/xxx
```

### 2.2 gh CLI 管理

```bash
# 创建
gh codespace create --repo user/repo --branch main
gh codespace create --repo user/repo --machine basicLinux32gb

# 列出
gh codespace list
# NAME                REPO         BRANCH  STATE
# orange-potato-abc   user/repo    main    Available

# 连接
gh codespace ssh                    # SSH 连接
gh codespace ports                  # 查看端口转发
gh codespace ports visibility 8080:public   # 公开端口

# 停止/删除
gh codespace stop
gh codespace delete --name orange-potato-abc

# 从 VS Code 打开（本地桌面版）
gh codespace code --web             # 浏览器
# 或直接按提示在 VS Code 中连接
```

---

## 3. devcontainer.json 配置详解

### 3.1 基础配置

```json
// .devcontainer/devcontainer.json
{
  "name": "Java Dev Environment",
  "image": "mcr.microsoft.com/devcontainers/java:17",

  // 或使用 Dockerfile
  // "build": {
  //   "dockerfile": "Dockerfile"
  // },

  // 或使用 docker-compose（多服务）
  // "dockerComposeFile": "docker-compose.yml",
  // "service": "app",

  // ═══ VS Code 配置 ═══
  "customizations": {
    "vscode": {
      "settings": {
        "java.compile.nullAnalysis.mode": "automatic",
        "editor.formatOnSave": true,
        "java.configuration.updateBuildConfiguration": "automatic"
      },
      "extensions": [
        "vscjava.vscode-java-pack",
        "vmware.vscode-boot-dev-pack",
        "sonarsource.sonarlint-vscode",
        "github.copilot",
        "github.copilot-chat"
      ]
    }
  },

  // ═══ 容器启动后执行的命令 ═══
  "postCreateCommand": "mvn dependency:resolve -q",

  // 每次连接时执行
  "postStartCommand": "echo 'Welcome! Java 17 environment is ready.'",

  // ═══ 端口转发 ═══
  "forwardPorts": [8080, 3306, 6379],
  "portsAttributes": {
    "8080": {
      "label": "Application",
      "onAutoForward": "notify"      // notify | openBrowser | silent
    },
    "3306": {
      "label": "MySQL",
      "onAutoForward": "silent"
    }
  },

  // ═══ 挂载 ═══
  "mounts": [
    "source=${localWorkspaceFolder}/.m2,target=/home/vscode/.m2,type=bind"
  ],

  // ═══ 用户配置 ═══
  "remoteUser": "vscode",
  "containerUser": "vscode",

  // 额外功能（预配置的工具）
  "features": {
    "ghcr.io/devcontainers/features/docker-in-docker:2": {},
    "ghcr.io/devcontainers/features/github-cli:1": {}
  }
}
```

### 3.2 Dockerfile 方式（更灵活）

```dockerfile
# .devcontainer/Dockerfile
FROM mcr.microsoft.com/devcontainers/java:17

# 安装额外工具
RUN apt-get update && apt-get install -y \
    graphviz \
    htop \
    && rm -rf /var/lib/apt/lists/*

# 安装 Maven Wrapper（如需要）
RUN mvn wrapper:wrapper -Dmaven=3.9.6

# 设置工作目录
WORKDIR /workspaces
```

### 3.3 Docker Compose 方式（多服务）

```yaml
# .devcontainer/docker-compose.yml
version: '3.8'
services:
  app:
    build:
      context: .
      dockerfile: Dockerfile
    volumes:
      - ../..:/workspaces:cached
    command: sleep infinity
    networks:
      - dev-network

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: dev123
      MYSQL_DATABASE: myapp
    ports:
      - "3306:3306"
    networks:
      - dev-network

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    networks:
      - dev-network

networks:
  dev-network:
```

---

## 4. 预构建与性能优化

### 4.1 预构建配置

```json
// .devcontainer/devcontainer.json 中添加：
{
  // 预构建频率
  "prebuild": {
    "schedule": "on-push",  // 推送到 main 时自动预构建
    "branches": ["main", "develop"]
  }
}
```

```yaml
# .github/workflows/prebuild.yml
# 或用 GitHub Actions 灵活控制预构建
name: Prebuild Codespaces

on:
  push:
    branches: [main]
    paths:
      - '.devcontainer/**'
      - 'pom.xml'

jobs:
  prebuild:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Prebuild dev container
        uses: devcontainers/ci@v0.3
        with:
          imageName: ghcr.io/${{ github.repository }}/devcontainer
          cacheFrom: ghcr.io/${{ github.repository }}/devcontainer:latest
          push: always
```

### 4.2 加速技巧

```
1. 使用预构建（Prebuild）
   → 连接时直接启动已构建好的容器（秒级），不需要等安装依赖

2. 缓存 Maven 依赖（挂载 volume）
   → "mounts": ["source=.m2-cache,target=/home/vscode/.m2,type=volume"]
   → 避免每次重建 Codespace 重新下载所有 jar

3. postCreateCommand 最小化
   → 只做必须的操作（mvn dependency:resolve）
   → 避免耗时操作在首次打开时阻塞

4. 选择合适的机器类型
   → 小型项目：2-core / 8GB RAM
   → 大型项目：4-core / 16GB RAM
   → 极大型：8-core+ / 32GB+ RAM
```

---

## 5. Java 开发环境实战配置

### 5.1 Spring Boot 项目完整配置

```json
// .devcontainer/devcontainer.json
{
  "name": "Spring Boot App",

  "dockerComposeFile": "docker-compose.yml",
  "service": "app",
  "workspaceFolder": "/workspaces/${localWorkspaceFolderBasename}",

  "customizations": {
    "vscode": {
      "extensions": [
        "vscjava.vscode-java-pack",
        "vmware.vscode-boot-dev-pack",
        "vscjava.vscode-spring-boot-dashboard",
        "redhat.vscode-xml",
        "redhat.vscode-yaml",
        "sonarsource.sonarlint-vscode",
        "github.copilot",
        "github.copilot-chat",
        "bierner.markdown-mermaid",
        "ms-azuretools.vscode-docker"
      ],
      "settings": {
        "java.compile.nullAnalysis.mode": "automatic",
        "java.configuration.updateBuildConfiguration": "automatic",
        "spring-boot.ls.problem.checks.enabled": true,
        "editor.formatOnSave": true
      }
    }
  },

  "forwardPorts": [8080, 3306, 6379],

  "postCreateCommand": "mvn dependency:resolve -q && echo '✅ Spring Boot 环境就绪'",

  "remoteEnv": {
    "SPRING_PROFILES_ACTIVE": "dev",
    "DB_URL": "jdbc:mysql://mysql:3306/myapp",
    "DB_USER": "root",
    "DB_PASSWORD": "dev123",
    "REDIS_HOST": "redis"
  }
}
```

### 5.2 团队标准化

```json
// .devcontainer/devcontainer.json
{
  // 团队通用基础配置 + 个人覆盖配置
  "name": "Team Java Environment",
  "image": "ghcr.io/my-org/devcontainer-java:latest",

  // 默认容器 + 个人扩展文件
  // 不强制覆盖团队成员的个人配置
  "features": {
    "ghcr.io/devcontainers/features/common-utils:2": {
      "installZsh": true,
      "configureZshAsDefaultShell": true,
      "upgradePackages": true
    }
  }
}

// 团队成员可在 .devcontainer/personal.json 中覆盖：
// {
//   "customizations": {
//     "vscode": {
//       "extensions": ["个人偏好的插件"]
//     }
//   }
// }
```

---

## 6. 端口转发与调试

### 6.1 端口管理

```bash
# Codespaces 端口转发
# 启动 Spring Boot 应用 → localhost:8080 自动转发

# 查看端口
gh codespace ports
# LABEL         PORT  VISIBILITY
# Application   8080  private
# MySQL         3306  private

# 修改可见性
gh codespace ports visibility 8080:public
# → 获得公网 URL：https://orange-potato-abc-8080.preview.app.github.dev

# 端口在浏览器中打开（VS Code → Ports tab → 🌐 图标）
```

### 6.2 Java 调试

```json
// .vscode/launch.json — Codespaces 中调试 Spring Boot
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Debug Spring Boot",
      "request": "launch",
      "mainClass": "com.example.Application",
      "projectName": "myapp",
      "args": "--spring.profiles.active=dev",
      "vmArgs": "-Dspring.devtools.restart.enabled=false"
    }
  ]
}
```

```yaml
# application-dev.yml — Codespaces 中的开发配置
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/myapp
    username: root
    password: dev123

  # 热重载（开发体验）
  devtools:
    livereload:
      enabled: true
```

---

## 7. 费用与资源管理

### 7.1 计费模型

| 机器类型 | 核心 | RAM | 存储 | 费用/小时 |
|---------|------|-----|------|----------|
| 2-core | 2 | 8 GB | 32 GB | $0.18 |
| 4-core | 4 | 16 GB | 32 GB | $0.36 |
| 8-core | 8 | 32 GB | 64 GB | $0.72 |
| 16-core | 16 | 64 GB | 128 GB | $1.44 |

```
免费额度（Free Plan）：120 核时/月 + 15 GB 存储/月
  = 2-core 机器运行 60 小时
  = 每天约 2 小时（一个月的周末开发量）

Team Plan: 共享组织的免费额度
Enterprise: 可分配额度给团队成员

默认空闲超时：30 分钟（可配置 5-240 分钟）
```

### 7.2 费用控制

```bash
# 查看当前用量
gh codespace usage
# 或 GitHub Settings → Billing → Plans and usage → Codespaces

# 设置支出上限
# Organization Settings → Billing → Spending limits

# 设置空闲超时
# Settings → Codespaces → Default idle timeout → 15 minutes
# 个人开发建议 10-15 分钟（省费用 + 够用完去喝杯咖啡再回来）
```

```json
// .devcontainer/devcontainer.json — 设置默认超时
{
  "idleTimeout": "15m"  // 15 分钟后未操作自动停止
}
```

---

## 8. 常见面试题

### Q1：Codespaces 和本地 VS Code 开发的区别？

> Codespaces 在云端运行（浏览器即开发环境），预配置环境、一致性有保证、"克隆即开发"。本地 VS Code 需手动安装 JDK/Maven/数据库等，环境差异是常见问题。详见第1节。

### Q2：devcontainer.json 的作用是什么？

> 定义 Codespaces 容器环境的配置文件——基础镜像、VS Code 扩展、端口转发、启动后命令、挂载卷等。保证团队所有人拥有一致的开发环境。详见第3节。

### Q3：什么是预构建？有什么好处？

> 提前在 CI 中构建好 dev container 镜像并缓存，创建 Codespace 时直接启动（秒级可用），无需等待依赖安装。详见第4节。

### Q4：Codespaces 的费用如何计算？

> 按机器规格（核数）和运行时长（分钟）计费。Free Plan 提供 120 核时/月。空闲 30 分钟自动停止。详见第7节。
