# 快速吃透 Docker（极简入门 + 核心命令 + 实操）

> **文档定位**：Java 后端企业级技术文档 | Docker 容器化  
> **核心价值**：一次打包，到处部署，环境完全一致  
> **前置基础**：Linux 基础命令

---

## 一、核心概念

### 1.1 什么是 Docker

Docker 是容器化引擎，用于打包、运行应用。将 **代码 + 依赖 + 环境 + 配置** 打包成镜像，随处运行。

### 1.2 容器 vs 虚拟机

| 维度 | 虚拟机 | Docker 容器 |
|------|--------|------------|
| 启动速度 | 分钟级 | **秒级** |
| 资源占用 | GB 级 | **MB 级** |
| 隔离方式 | 完整 OS | 共享宿主机内核 |
| 性能 | 有损耗 | 接近原生 |

### 1.3 四大核心名词

| 概念 | 说明 | 类比 |
|------|------|------|
| **镜像（Image）** | 只读模板 | 安装包 / 系统快照 |
| **容器（Container）** | 镜像运行后的实例 | 运行中的程序 |
| **Dockerfile** | 自定义构建镜像的脚本 | 自动化安装脚本 |
| **仓库（Registry）** | 存放镜像的仓库 | Docker Hub |

---

## 二、底层原理

### 2.1 Docker 架构

```
Docker CLI → Docker Daemon → containerd → runc → 容器进程
                                 └── 镜像管理（分层存储）
```

### 2.2 端口映射核心逻辑

```
-p 宿主机端口:容器端口
-p 8080:80   → 外部访问 localhost:8080 → 转发到容器内的 80 端口
不做端口映射 → 外部无法访问容器服务
```

---

## 三、代码实现（核心命令）

### 3.1 基础命令

```bash
docker --version          # 查看版本
docker images             # 查看本地所有镜像
docker ps                 # 查看运行中的容器
docker ps -a              # 查看所有容器（含已停止）
```

### 3.2 镜像操作

```bash
docker pull mysql:8.0     # 拉取镜像
docker pull redis         # 拉取最新版
docker rmi 镜像ID          # 删除镜像
```

### 3.3 容器启停（最常用）

```bash
# 后台运行 + 端口映射 + 命名
docker run -d -p 8080:80 --name my-nginx nginx

# 停止 / 启动 / 删除
docker stop 容器名
docker start 容器名
docker rm 容器名           # 必须先停止
```

### 3.4 进入容器 & 查看日志

```bash
docker exec -it 容器名 /bin/bash   # 进入容器内部
docker logs 容器名                 # 查看日志
docker logs -f 容器名              # 实时滚动日志
```

### 3.5 实战：30 秒跑通 Nginx

```bash
docker pull nginx
docker run -d -p 80:80 --name my-nginx nginx
# 浏览器访问 localhost → Nginx 默认页面
```

### 3.6 Dockerfile 自定义镜像

```dockerfile
# 基础镜像
FROM java:8
# 复制 jar 包
COPY app.jar /app.jar
# 启动命令
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
docker build -t my-app:1.0 .    # 构建镜像
docker run -d -p 8080:8080 my-app:1.0  # 运行
```

---

## 四、实战要点

| 要点 | 说明 |
|------|------|
| **数据持久化** | 使用 `-v` 挂载数据卷，避免容器删除后数据丢失 |
| **网络模式** | 多容器通信使用 `--network` 自定义网络 |
| **环境变量** | `-e` 传递配置（如 `-e MYSQL_ROOT_PASSWORD=123456`） |

---

## 五、避坑总结

| 坑点 | 解决 |
|------|------|
| **容器退出即消失** | 数据用 `-v` 挂载持久化 |
| **端口冲突** | 修改宿主机端口（如 `-p 8081:80`） |
| **镜像拉取慢** | 配置国内镜像加速器（阿里云） |
| **容器内中文乱码** | Dockerfile 设置 `ENV LANG=C.UTF-8` |

---

## 六、企业级最佳实践

- **Docker Compose** 管理多容器应用（一键启动 MySQL + Redis + Java 应用）
- **多阶段构建** 减小镜像体积（构建阶段 + 运行阶段分离）
- **健康检查** Dockerfile 添加 `HEALTHCHECK` 指令
- **日志驱动** 生产环境配置 `json-file` + `max-size` 防止磁盘占满
- **非 root 运行** 安全性考虑，容器内不以 root 用户运行
