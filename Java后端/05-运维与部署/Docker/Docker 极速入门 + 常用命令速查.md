# Docker 极速入门 + 常用命令速查（Windows 适配，可直接复制）

## 一、核心核心概念（极简记忆）
1. 镜像(image)：只读模板，程序+依赖+环境，不可运行
2. 容器(container)：镜像运行实例，隔离环境，可启停
3. 仓库(Docker Hub)：公共镜像源，拉取官方/第三方镜像
4. 数据卷(-v)：解决容器数据丢失、文件挂载
5. Dockerfile：自定义镜像构建脚本
6. Compose：一键编排多个容器（MySQL/Redis/Nginx 组合必备）

---

## 二、基础环境验证
```powershell
# 查看版本
docker -v
# 测试环境是否正常
docker run hello-world
```

---

## 三、镜像常用命令
```powershell
# 拉取官方镜像
docker pull 镜像名:标签
# 示例：拉取nginx、mysql、redis
docker pull nginx:latest
docker pull mysql:8.0
docker pull redis:latest

# 查看本地所有镜像
docker images

# 搜索镜像
docker search 镜像名

# 删除本地镜像
docker rmi 镜像ID/镜像名

# 清理无用镜像
docker image prune
```

---

## 四、容器核心高频命令（重点）
```powershell
# 完整启动容器 模板
# docker run [参数] 镜像名:标签
# -d 后台运行  -p 宿主机端口:容器端口  --name 自定义容器名  -v 文件挂载
docker run -d -p 8080:80 --name my-nginx nginx

# 查看正在运行的容器
docker ps
# 查看所有容器(包含停止的)
docker ps -a

# 启停操作
docker start 容器名/容器ID
docker stop 容器名/容器ID
docker restart 容器名/容器ID

# 删除容器（必须先停止）
docker rm 容器名/容器ID

# 查看容器日志
docker logs 容器名

# 进入容器内部终端
docker exec -it 容器名 /bin/bash
```

---

## 五、数据挂载 持久化
容器删除数据自动清空，挂载宿主机目录实现持久化
```powershell
# 格式 -v 宿主机绝对路径:容器内路径
docker run -d -p 8080:80 -v D:\docker\nginx-html:/usr/share/nginx/html --name my-nginx nginx
```

---

## 六、自定义镜像 Dockerfile 实操
1. 新建文件 `Dockerfile` 无后缀
```dockerfile
FROM alpine:3.18
WORKDIR /app
COPY . .
CMD ["echo","Docker 自定义镜像构建成功"]
```
2. 构建+运行
```powershell
# 构建镜像 -t 镜像名:版本 . 代表当前目录
docker build -t my-demo:v1 .

# 运行自定义镜像
docker run my-demo:v1
```

### 常用 Dockerfile 指令
- `FROM`：指定基础镜像
- `WORKDIR`：设置工作目录
- `COPY`：复制本地文件到容器
- `RUN`：构建时执行命令（安装依赖）
- `EXPOSE`：声明容器暴露端口
- `CMD`：容器启动执行命令

---

## 七、Docker Compose 多容器编排

### 1. 编写 docker-compose.yml
```yaml
version: '3.8'
services:
  nginx:
    image: nginx
    ports:
      - "8080:80"
  mysql:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: 123456
  redis:
    image: redis
    ports:
      - "6379:6379"
```

### 2. 一键操作命令
```powershell
# 后台启动所有服务
docker compose up -d

# 查看运行状态
docker compose ps

# 停止并删除容器、网络
docker compose down
```

---

## 八、万能清理命令（解决磁盘占用）
```powershell
# 清理停止的容器、无用网络、镜像
docker system prune -a
```

---

## 九、超实用实战示例（直接复制即用）

### 1. 快速部署 MySQL8.0
```powershell
docker run -d \
-p 3306:3306 \
--name mysql8 \
-e MYSQL_ROOT_PASSWORD=123456 \
mysql:8.0
```

### 2. 快速部署 Redis
```powershell
docker run -d -p 6379:6379 --name redis redis
```

---

## 十、学习路线（快速进阶）
1. 熟记以上所有命令，熟练单机容器部署
2. 精通 Dockerfile 编写，优化镜像体积
3. 熟练使用 Compose 部署项目组合（Java+MySQL+Redis）
4. 了解镜像分层、私有仓库、网络模式
