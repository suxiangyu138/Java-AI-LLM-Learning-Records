在项目中使用 Docker
下面我用**Java后端项目（Spring Boot）**为例，手把手带你在项目中使用 Docker，从 0 到 1 完整走一遍。
 
一、准备工作（先做这 3 步）
1. 安装 Docker
    - Windows / Mac：下载 Docker Desktop
    - Linux： apt install docker.io  或  yum install docker 
2. 准备一个简单的 Spring Boot 项目
    - 能正常运行，端口比如 8080
    - 能打出 jar 包（ mvn package ）
3. 项目结构（示例）
    plaintext
    your-project/
    ├── src/
    ├── pom.xml
    ├── target/
    │   └── demo-0.0.1-SNAPSHOT.jar
    └── Dockerfile
 
 
二、编写 Dockerfile（核心）
在项目根目录新建  Dockerfile ，内容如下：
dockerfile

# 基础镜像（Java 17）
FROM openjdk:17-jdk-slim

# 工作目录
WORKDIR /app

# 复制 jar 包到容器
COPY target/demo-0.0.1-SNAPSHOT.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
 
 
三、打包项目（生成 jar）
在项目根目录执行：
bash
mvn clean package -DskipTests
 
成功后会在  target/  下生成 jar 包。
 
四、构建 Docker 镜像
bash
docker build -t my-springboot-app .
 
-  -t ：给镜像打标签（名字:版本）
-  . ：Dockerfile 所在目录
 
五、运行容器
bash
docker run -d -p 8080:8080 --name myapp my-springboot-app
 
-  -d ：后台运行
-  -p 8080:8080 ：主机端口:容器端口
-  --name ：容器名字
 
六、验证是否成功
访问：
plaintext
http://localhost:8080
 
能访问到项目接口，说明成功。
 
七、常用 Docker 命令（必记）
bash

# 查看镜像
docker images

# 查看运行中的容器
docker ps

# 停止容器
docker stop myapp

# 删除容器
docker rm myapp

# 删除镜像
docker rmi my-springboot-app

# 查看容器日志
docker logs -f myapp
 
 
八、进阶：Docker Compose（多容器）
如果你的项目需要 MySQL、Redis，可以用  docker-compose.yml  一键启动。
示例  docker-compose.yml ：
yaml
version: '3'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - mysql
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/test
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=123456
  mysql:
    image: mysql:8
    environment:
      - MYSQL_ROOT_PASSWORD=123456
      - MYSQL_DATABASE=test
    ports:
      - "3306:3306"
 
启动：
bash
docker-compose up -d
 
 
九、企业级常用操作
1. 镜像推送到 Docker Hub / 私有仓库
2. 使用  .dockerignore  忽略不必要文件
3. 多阶段构建减小镜像体积
4. 容器健康检查
5. 数据卷挂载持久化
 
