## 一、Docker 是什么（3句话懂原理）
- **镜像 Image**：只读模板，包含「系统+程序+依赖+配置」，类比：**安装包/模具**。
- **容器 Container**：镜像的运行实例，独立、轻量、隔离，类比：**运行中的程序/模具做出来的产品**。
- **Docker**：把应用打包成容器，**一次打包，到处运行**，比虚拟机快、资源占用少。

一句话：**镜像静态，容器动态；镜像建容器，容器不影响镜像**。

---

## 二、安装 Docker（Windows/macOS/Linux）
### Windows / macOS（推荐）
1. 下载 **Docker Desktop**：https://www.docker.com/products/docker-desktop
2. 安装时勾选 **Use WSL 2**（Windows），一路下一步。
3. 打开终端（CMD/PowerShell）验证：
```bash
docker --version
docker run hello-world
```
看到 `Hello from Docker!` 即成功。

### Linux（Ubuntu/Debian）
```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg
curl -fsSL https://get.docker.com | sh
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker $USER   # 免sudo
newgrp docker
docker run hello-world
```

---

## 三、核心命令（必须背，日常90%就靠这些）
### 1）镜像操作
```bash
docker pull nginx                  # 拉取镜像（从Docker Hub）
docker images                       # 查看本地镜像
docker rmi nginx                    # 删除镜像
docker build -t myapp:v1 .         # 构建镜像（基于Dockerfile）
```

### 2）容器操作（最常用）
```bash
# 启动容器（后台+端口+名字）
docker run -d -p 8080:80 --name my-nginx nginx
# -d：后台运行；-p 宿主机:容器端口；--name：自定义名字

docker ps                           # 查看运行中容器
docker ps -a                        # 查看所有容器（含停止）
docker stop my-nginx                # 停止容器
docker start my-nginx               # 启动容器
docker restart my-nginx             # 重启容器
docker rm my-nginx                  # 删除容器（先stop）
docker exec -it my-nginx /bin/bash # 进入容器终端
docker logs my-nginx                # 查看日志
```

---

## 四、实战1：5分钟跑一个 Nginx 网站
```bash
# 1. 拉取镜像
docker pull nginx

# 2. 启动容器（8080端口访问）
docker run -d -p 8080:80 --name my-nginx nginx

# 3. 验证
docker ps
curl http://localhost:8080   # 或浏览器访问
```
看到 Nginx 欢迎页 → 成功。

---

## 五、实战2：自己构建镜像（Dockerfile）
### 1）新建文件 `Dockerfile`（无后缀）
```dockerfile
# 基础镜像
FROM alpine:latest
# 工作目录
WORKDIR /app
# 复制当前目录所有文件到容器/app
COPY . .
# 启动命令
CMD ["echo", "Hello Docker!"]
```

### 2）构建并运行
```bash
docker build -t hello:v1 .
docker run hello:v1   # 输出 Hello Docker!
```

---

## 六、实战3：数据持久化（-v 挂载）
容器删除后数据会丢，用 **数据卷** 保存：
```bash
# 把宿主机 ./data 挂载到容器 /app/data
docker run -d -p 8080:80 -v $(pwd)/data:/app/data --name my-nginx nginx
```
- 宿主机改 `./data` → 容器同步
- 容器删了，`./data` 还在

---

## 七、实战4：Docker Compose（多容器一键启停）
### 1）新建 `docker-compose.yml`
```yaml
version: "3"
services:
  nginx:
    image: nginx
    ports:
      - "8080:80"
    volumes:
      - ./html:/usr/share/nginx/html
  redis:
    image: redis
    ports:
      - "6379:6379"
```

### 2）一键启动/停止
```bash
docker-compose up -d   # 后台启动所有服务
docker-compose ps       # 查看状态
docker-compose down     # 停止并删除容器
```
适合管理 **Nginx + Redis + MySQL** 等多容器应用。

---

## 八、核心概念再总结（必记）
- **镜像**：静态模板，`docker images` 查看
- **容器**：运行实例，`docker ps` 查看
- **Dockerfile**：镜像构建脚本
- **数据卷 -v**：容器持久化
- **Docker Compose**：多容器编排

---

