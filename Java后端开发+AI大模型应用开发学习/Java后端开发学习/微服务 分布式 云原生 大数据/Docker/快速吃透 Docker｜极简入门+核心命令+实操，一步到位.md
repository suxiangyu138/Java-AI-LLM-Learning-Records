04.27 09:33
快速吃透 Docker｜极简入门+核心命令+实操，一步到位
一、Docker 核心本质（先懂概念，不用死记）
1. 是什么
- Docker 是容器化引擎，用来打包、运行应用；
- 把「代码+依赖+环境+配置」打包成镜像，随处运行；
- 一次打包，到处部署，环境完全一致。
2. 核心区别：容器 vs 虚拟机
- 虚拟机：装完整操作系统，笨重、开机慢、占用高
- Docker 容器：共享宿主机内核，轻量、秒启动、资源极小
3. 四大核心名词（必背）
1. 镜像 Image：只读模板，相当于「安装包/系统快照」
2. 容器 Container：镜像运行后的实例，真正跑服务的地方
3. Dockerfile：文本文件，自定义构建镜像的脚本
4. 仓库 Registry：存放镜像（官方：Docker Hub）
 
二、Docker 安装（Windows 快速版）
1. Windows 10/11 直接装 Docker Desktop
2. 开启：WSL2 组件（安装时自动配置）
3. 安装完成启动，输入命令验证：
bash
docker -v
 
 
三、高频核心命令（直接复制即用）
1. 基础信息
bash
# 查看docker版本
docker --version
# 查看本地所有镜像
docker images
# 查看正在运行的容器
docker ps
# 查看所有容器（包含停止的）
docker ps -a
 
2. 镜像操作
bash
# 拉取镜像（例：拉取mysql、redis）
docker pull mysql:8.0
docker pull redis
# 删除镜像
docker rmi 镜像ID
 
3. 容器启停（最常用）
bash
# 后台运行容器 + 端口映射
docker run -d -p 宿主机端口:容器端口 --name 容器名 镜像名
# 停止容器
docker stop 容器ID/容器名
# 启动已存在容器
docker start 容器ID/容器名
# 删除容器（必须先停止）
docker rm 容器ID/容器名
 
4. 进入容器内部
bash
docker exec -it 容器名 /bin/bash
 
5. 日志 & 资源
bash
# 查看容器日志
docker logs 容器名
# 实时滚动日志
docker logs -f 容器名
 
 
四、实战示例：30秒跑通 Nginx
bash
# 1. 拉取nginx镜像
docker pull nginx
# 2. 启动容器 80端口映射
docker run -d -p 80:80 --name my-nginx nginx
 
浏览器访问  localhost  直接打开 Nginx 页面。
 
五、端口映射核心逻辑（重点）
-  -p 8080:80 
- 前面：宿主机端口（你电脑访问用）
- 后面：容器内部端口（软件真实监听端口）
- 不做端口映射：外部无法访问容器服务
 
六、Dockerfile 自定义镜像（极简模板）
dockerfile
# 基础镜像
FROM java:8
# 复制jar包到容器
COPY app.jar /app.jar
# 启动命令
ENTRYPOINT ["java","-jar","/app.jar"]
 
构建镜像命令：
bash
docker build -t my-app:1.0 .
 
 
七、Docker 核心优势（面试/理解用）
1. 环境隔离：不同软件依赖不冲突
2. 轻量高效：秒启动、资源占用低
3. 一致性：开发/测试/生产环境完全统一
4. 快速部署：一条命令启动中间件（MySQL、Redis、Milvus）
 
八、你接下来进阶路线（贴合你的技术栈）
1. 用 Docker 部署： MySQL、Redis、Milvus 
2. 学会  docker-compose  一键编排多个容器
3. 手写 Dockerfile 打包 Java 后端项目

