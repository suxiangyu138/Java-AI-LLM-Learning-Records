03.31 14:00
Spring Boot 项目 Docker 部署完整文档
一、项目准备
1. 项目基础信息
- 项目名称：docker-demo
- 技术栈：Spring Boot 3.2.0、Java 17、Maven
- 端口配置：8080
- 核心接口：GET / （返回固定文本）
2. 项目结构
plaintext
docker-demo/
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── example/
│       │           └── dockerdemo/
│       │               └── DockerDemoApplication.java
│       └── resources/
│           └── application.yml
└── Dockerfile
 
3. 核心代码文件
（1）pom.xml
xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>
    <groupId>com.example</groupId>
    <artifactId>docker-demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>docker-demo</name>
    <description>Demo project for Docker</description>
    <properties>
        <java.version>17</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
 
（2）DockerDemoApplication.java
java
package com.example.dockerdemo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@SpringBootApplication
@RestController
public class DockerDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DockerDemoApplication.class, args);
    }
    @GetMapping("/")
    public String hello() {
        return "Hello Docker! I'm running in a container.";
    }
}
 
（3）application.yml
yaml
server:
  port: 8080
 
二、Docker 配置
1. Dockerfile（项目根目录）
dockerfile
# 基础镜像：Java 17 精简版
FROM openjdk:17-jdk-slim
# 容器工作目录
WORKDIR /app
# 复制项目 jar 包到容器
COPY target/docker-demo-0.0.1-SNAPSHOT.jar app.jar
# 暴露服务端口
EXPOSE 8080
# 容器启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
 
三、部署操作步骤
1. 打包项目 Jar 包
在项目根目录执行 Maven 打包命令：
bash
mvn clean package -DskipTests
 
执行成功后， target  目录下会生成  docker-demo-0.0.1-SNAPSHOT.jar  文件。
2. 构建 Docker 镜像
在项目根目录执行镜像构建命令：
bash
docker build -t docker-demo:1.0 .
 
-  -t ：为镜像添加标签（格式：镜像名:版本号）
-  . ：指定 Dockerfile 所在目录为当前目录
3. 运行 Docker 容器
执行容器启动命令：
bash
docker run -d -p 8080:8080 --name my-docker-app docker-demo:1.0
 
-  -d ：后台运行容器
-  -p 8080:8080 ：端口映射（主机端口:容器端口）
-  --name ：为容器指定名称
4. 验证部署结果
打开浏览器或使用 curl 命令访问：
plaintext
http://localhost:8080
 
返回内容：
plaintext
Hello Docker! I'm running in a container.
 
表示部署成功。
四、常用 Docker 运维命令
bash
# 查看所有镜像
docker images
# 查看运行中的容器
docker ps
# 查看容器运行日志（实时刷新）
docker logs -f my-docker-app
# 停止运行中的容器
docker stop my-docker-app
# 删除已停止的容器
docker rm my-docker-app
# 删除 Docker 镜像
docker rmi docker-demo:1.0
 
五、注意事项
1. 确保本地已安装 Docker 环境，Windows/Mac 推荐使用 Docker Desktop，Linux 可通过  apt/yum  安装。
2. 打包 Jar 包前，确认项目代码无报错，避免构建镜像失败。
3. 端口映射时，主机端口需未被占用，可根据需求修改映射端口。
4. 镜像标签建议规范命名，区分版本，便于管理。

