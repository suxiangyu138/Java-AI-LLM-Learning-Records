# Docker容器化教程 面试宝典
> 基于Docker容器化教程课程大纲，全面覆盖Docker面试高频考点

## 目录

1. [基础概念速答](#一基础概念速答12-18题)
2. [深度原理剖析](#二深度原理剖析8-12题)
3. [实战场景题](#三实战场景题6-10题)
4. [手写代码/配置文件题](#四手写代码配置文件题5-8题)
5. [系统设计题](#五系统设计题3-5题)
6. [常见坑点与最佳实践](#六常见坑点与最佳实践表格)
7. [面试回答模板](#七面试回答模板top-5)
8. [快速查漏补缺Checklist](#八快速查漏补缺checklist)

---

## 一、基础概念速答（12-18题）

### Q1: 什么是Docker？Docker和传统虚拟机有什么区别？
**A:** Docker是一个开源的应用容器引擎，基于Go语言开发，遵循Apache 2.0协议。Docker让开发者可以打包应用及依赖包到一个可移植的容器中，发布到任何流行的Linux机器上。

| 特性 | Docker容器 | 虚拟机 |
|------|-----------|--------|
| 启动速度 | 秒级 | 分钟级 |
| 磁盘占用 | MB级 | GB级 |
| 性能损耗 | 近乎原生 | 有一定损耗 |
| 内核利用 | 共享宿主机内核 | 独立内核 |
| 隔离性 | 进程级隔离 | 完全隔离 |
| 资源密度 | 一台宿主机可运行上千容器 | 最多几十台 |

### Q2: 什么是Docker镜像（Image）和容器（Container）？
**A:** 镜像是构建容器的只读模板，包含了运行应用所需的代码、运行时、库、环境变量和配置文件。容器是镜像的运行实例，可以启动、停止、删除。镜像相当于类（Class），容器相当于对象（Instance）。镜像是静态的，容器是动态的。

### Q3: Docker的架构包含哪些核心组件？
**A:** Docker采用C/S架构：
- **Docker Daemon（dockerd）**: 后台守护进程，管理容器、镜像、网络、存储卷
- **Docker Client（docker）**: 命令行工具，与Daemon通信
- **Docker Registry**: 镜像仓库，存储和分发镜像（Docker Hub、私有仓库）
- **Docker Objects**: 镜像、容器、网络、数据卷等
- **REST API**: Client通过API与Daemon交互

### Q4: 什么是Docker镜像加速器？为什么要配置？
**A:** 由于Docker Hub服务器在国外，国内直接拉取镜像速度慢。配置镜像加速器（如阿里云、中科大、网易等）可大幅提升拉取速度。配置方式在 `/etc/docker/daemon.json` 中添加 `registry-mirrors` 配置。

### Q5: Docker的常用命令分为哪几类？
**A:**
- **服务相关**: `systemctl start/stop/restart/status docker`
- **镜像相关**: `docker pull|search|images|rmi|tag|push|build`
- **容器相关**: `docker run|start|stop|restart|rm|ps|logs|exec|inspect`
- **网络相关**: `docker network create|ls|inspect|connect|disconnect`
- **数据卷相关**: `docker volume create|ls|inspect|rm`

### Q6: 什么是Docker数据卷（Volume）？有哪些类型？
**A:** 数据卷是持久化容器数据的机制，用于解决容器删除后数据丢失的问题。三种类型：

| 类型 | 说明 | 使用场景 |
|------|------|---------|
| Bind Mount | 将宿主机目录挂载到容器 | 开发环境，共享源码 |
| Volume | Docker管理的持久化存储 | 生产环境，数据持久化 |
| tmpfs | 存储在内存中 | 临时敏感数据 |

### Q7: 什么是数据卷容器？
**A:** 专门用于提供数据卷给其他容器挂载的容器。通过 `docker run --volumes-from` 实现多容器共享数据卷，适用于需要共享配置文件、日志等场景。

### Q8: Docker支持哪些网络模式？
**A:**
- **bridge（默认）**: 通过docker0网桥实现容器间通信
- **host**: 容器直接使用宿主机网络栈，无网络隔离
- **none**: 无网络，适用于安全隔离场景
- **overlay**: 跨主机通信，用于Swarm集群
- **macvlan**: 为容器分配MAC地址，使其像物理设备

### Q9: Dockerfile有哪些关键指令？
**A:**
| 指令 | 作用 | 示例 |
|------|------|------|
| FROM | 指定基础镜像 | `FROM openjdk:11` |
| RUN | 运行命令 | `RUN apt-get update` |
| COPY | 复制文件 | `COPY target/app.jar /app.jar` |
| ADD | 复制并支持自动解压 | `ADD app.tar.gz /app` |
| CMD | 容器启动命令 | `CMD ["java", "-jar", "app.jar"]` |
| ENTRYPOINT | 容器入口点 | `ENTRYPOINT ["java", "-jar"]` |
| ENV | 环境变量 | `ENV JAVA_HOME=/usr/local/jdk` |
| EXPOSE | 暴露端口 | `EXPOSE 8080` |
| VOLUME | 声明数据卷 | `VOLUME /data` |
| WORKDIR | 工作目录 | `WORKDIR /app` |
| USER | 指定用户 | `USER nobody` |

### Q10: Docker Compose的作用是什么？
**A:** Docker Compose是一个定义和运行多容器Docker应用的工具，通过 `docker-compose.yml` 文件定义服务、网络、数据卷。核心命令：`up`、`down`、`ps`、`logs`、`exec`、`build`。

### Q11: 什么是Docker私有仓库？如何搭建？
**A:** 私有仓库用于企业内部存储和管理Docker镜像，确保安全性和速度。搭建方式：
- **Registry**: 官方轻量仓库，`docker run -d -p 5000:5000 --name registry registry:2`
- **Harbor**: 企业级仓库，支持权限管理、LDAP、镜像复制、漏洞扫描等功能

### Q12: 如何部署应用（如MySQL、Nginx）到Docker？
**A:** 示例 - Docker部署MySQL：
```bash
docker run -d \
  --name mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -v /data/mysql:/var/lib/mysql \
  --restart=always \
  mysql:8.0
```

### Q13: Docker层的概念是什么？如何查看层？
**A:** Docker镜像是分层构建的，每一层都是只读的。`docker history <image>` 可查看镜像的各层信息。分层机制支持缓存和复用。

### Q14: 容器转为镜像的命令是什么？
**A:** 使用 `docker commit -m "描述" -a "作者" <容器ID> <新镜像名>` 将容器保存为新镜像。也可使用 `docker export/import` 导出/导入容器的文件系统快照。

### Q15: CMD和ENTRYPOINT的区别？
**A:**
| 特性 | CMD | ENTRYPOINT |
|------|-----|------------|
| 可被覆盖 | `docker run` 参数可覆盖 | 需 `--entrypoint` 覆盖 |
| 默认值 | 提供默认启动命令 | 定义固定入口点 |
| 组合使用 | 作为ENTRYPOINT的默认参数 | 定义入口程序 |
| 示例 | `CMD ["-jar", "app.jar"]` | `ENTRYPOINT ["java"]` |

### Q16: 如何限制Docker容器资源使用？
**A:**
```bash
# 限制CPU
docker run --cpus=1.5 --cpuset-cpus=0-1

# 限制内存
docker run -m 512m --memory-swap=1g

# 限制磁盘I/O
docker run --device-read-bps=/dev/sda:1mb
```

### Q17: Docker Healthcheck的作用？
**A:** Docker Healthcheck用于检测容器内应用的健康状态，替代外部监控ping容器的方式。
```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1
```

### Q18: .dockerignore文件的作用？
**A:** 类似于 `.gitignore`，在构建镜像时排除不需要的文件，避免将本地无关文件（如node_modules、.git、target目录）发送到Docker守护进程，减少构建上下文大小，加快构建速度。

---

## 二、深度原理剖析（8-12题）

### Q1: Docker镜像分层机制的原理是什么？
**A:** Docker镜像采用UnionFS（联合文件系统）技术。每个Dockerfile指令创建一个新的只读层，所有层叠加形成完整文件系统。
- **共享**: 基础镜像层可被多个上层镜像共享，节省磁盘空间
- **缓存**: 构建时每层有哈希值，层未变化则复用缓存
- **写时复制（CoW）**: 容器运行时在最上层添加可写层，修改文件时从只读层复制到可写层再修改
- **常用存储驱动**: overlay2（推荐）、aufs、devicemapper
- **查看层**: `docker history <image>` 查看各层大小和创建命令

### Q2: Docker容器隔离的核心技术是什么？
**A:** 容器隔离依赖两大Linux内核特性：
- **Namespace（命名空间）**: 实现资源隔离
  - PID Namespace: 进程ID隔离
  - Network Namespace: 网络栈隔离
  - Mount Namespace: 挂载点隔离
  - UTS Namespace: 主机名隔离
  - IPC Namespace: 进程间通信隔离
  - User Namespace: 用户ID隔离
- **Cgroups（控制组）**: 实现资源限制
  - CPU限制（cpu.shares、cpu.cfs_period_us）
  - 内存限制（memory.limit_in_bytes）
  - 磁盘I/O限制（blkio.*）
  - 网络带宽限制

### Q3: Docker网络通信的核心原理是什么？
**A:**
- **bridge模式**: Docker守护进程创建docker0虚拟网桥，为每个容器分配veth pair（一对虚拟网卡），一端在容器内（eth0），一端在宿主机（vethxxx），通过docker0桥接实现容器间通信
- **数据流**: `容器eth0 -> veth pair -> docker0网桥 -> iptables NAT -> 宿主机eth0 -> 外部`
- **通信规则**:
  - 同宿主机容器: 通过docker0网桥直接通信
  - 跨宿主机: 需overlay网络或路由配置
  - 外部访问: 通过DNAT映射宿主机端口到容器端口

### Q4: Docker数据卷的持久化原理是什么？
**A:**
- **Bind Mount**: 直接将宿主机目录映射到容器，容器内对该目录的读写直接反映到宿主机
- **Volume**: 由Docker管理在 `/var/lib/docker/volumes/` 下，通过挂载点链接到容器
- **数据流**: `容器内路径 -> 挂载点 -> 宿主机存储路径`
- **生命周期**: Volume独立于容器，容器删除后Volume数据保留
- **备份恢复**: `docker run --rm -v <volume>:/data -v $(pwd):/backup busybox tar czf /backup/backup.tar.gz /data`

### Q5: Dockerfile最佳构建实践有哪些？
**A:**
1. **选择迷你基础镜像**: 使用 `alpine`、`slim` 版本减小体积
2. **分层优化**: 将变化少的指令放在前面，利用缓存
3. **合并RUN命令**: `RUN apt-get update && apt-get install -y package1 package2`
4. **多阶段构建**: 分离构建环境和运行环境
5. **不使用root用户**: `RUN groupadd -r app && useradd -r -g app app && USER app`
6. **使用.dockerignore**: 排除不需要的文件
7. **精准COPY**: 避免 `COPY . .`，仅复制必要文件
8. **标签管理**: 使用有意义的标签，避免使用 `latest`

### Q6: Docker Compose的service定义中depends_on的局限性是什么？
**A:** `depends_on` 仅控制容器启动顺序，不等待容器内应用就绪。MySQL容器启动后还需要几秒才能接受连接。解决方案：
- 使用 `healthcheck` 检测服务就绪状态
- 使用 `wait-for-it.sh` 脚本等待端口
- 应用层实现重试机制
```yaml
version: '3.8'
services:
  app:
    build: .
    depends_on:
      db:
        condition: service_healthy
  db:
    image: mysql:8.0
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
```

### Q7: 多阶段构建（Multistage Build）的原理和优势？
**A:** 在单一Dockerfile中使用多个FROM语句，每个FROM开始一个新的构建阶段，最终仅将最后一个阶段的文件复制到最终镜像中。
```dockerfile
# 第一阶段：编译
FROM maven:3.8-openjdk-11 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src/ /app/src/
RUN mvn package -DskipTests

# 第二阶段：运行
FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=builder /app/target/app.jar .
EXPOSE 8080
USER nobody
CMD ["java", "-jar", "app.jar"]
```
**优势**: 最终镜像仅包含运行时依赖，不包含构建工具、源码、中间产物，体积减少50%-80%。

### Q8: Docker Swarm和Kubernetes的核心区别是什么？
**A:**
| 特性 | Docker Swarm | Kubernetes |
|------|-------------|-----------|
| 易用性 | 简单，原生Docker语法 | 复杂，学习曲线陡 |
| 安装配置 | 内置Docker引擎 | 独立安装部署 |
| 服务发现 | DNS + VIP | DNS + Service |
| 负载均衡 | 内置 | Ingress + Service |
| 自动扩缩 | 手动 | HPA自动 |
| 存储编排 | 基础 | 丰富（PV/PVC/StorageClass） |
| 网络策略 | 基础 | 丰富（NetworkPolicy） |
| 成熟度 | 较低 | 高，CNCF毕业项目 |
| 社区生态 | 小 | 巨大 |
| 适合场景 | 中小规模集群 | 大规模、复杂业务 |

### Q9: Docker Registry和Harbor的区别？
**A:**
| 特性 | Docker Registry | Harbor |
|------|-----------------|--------|
| 功能 | 基础镜像存储 | 企业级镜像管理 |
| 鉴权 | 无内置 | 支持LDAP、OIDC |
| Web UI | 无 | 完善的Web管理界面 |
| 镜像复制 | 无 | 跨数据中心复制 |
| 漏洞扫描 | 无 | 集成Trivy、Clair |
| 垃圾回收 | 手动 | 自动 |
| 审计日志 | 无 | 完整审计 |
| 部署复杂度 | 简单 | 较复杂（需Nginx、数据库等） |

### Q10: Docke r容器中进程的工作原理是什么？
**A:** Docker容器本质上是宿主机上的一个普通进程，通过Namespace实现隔离，让进程认为自己运行在独立操作系统中：
- **PID=1**: 容器中第一个进程PID为1，如同Linux系统的init进程
- **僵尸进程**: 如果PID1进程不能正确处理孤儿/僵尸进程，可能导致容器内存泄漏
- **信号处理**: PID1进程负责接收和处理来自Docker Daemon的信号（如SIGTERM）
- **解决方案**: 使用 `tini` 或 `dumb-init` 作为容器entrypoint，正确处理信号和回收僵尸进程
```dockerfile
FROM alpine:3.18
RUN apk add --no-cache tini
COPY app /app
ENTRYPOINT ["/sbin/tini", "--"]
CMD ["/app"]
```

---

## 三、实战场景题（6-10题）

### Q1: Spring Boot项目如何容器化部署？
**A:**
```dockerfile
# 多阶段构建
FROM maven:3.8-openjdk-11 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src/ /app/src/
RUN mvn clean package -DskipTests

FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Q2: 部署MySQL并持久化数据的具体配置？
**A:**
```bash
docker run -d \
  --name mysql8 \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root123 \
  -e MYSQL_DATABASE=testdb \
  -e MYSQL_USER=test \
  -e MYSQL_PASSWORD=test123 \
  -v mysql-data:/var/lib/mysql \
  -v /opt/mysql/conf:/etc/mysql/conf.d \
  -v /opt/mysql/log:/var/log/mysql \
  --restart=always \
  mysql:8.0 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci
```

### Q3: Docker Compose编排Nginx + Spring Boot + MySQL全栈项目？
**A:**
```yaml
version: '3.8'
services:
  nginx:
    image: nginx:1.24-alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/conf.d:/etc/nginx/conf.d
      - ./nginx/ssl:/etc/nginx/ssl
      - ./nginx/log:/var/log/nginx
    depends_on:
      - app
    networks:
      - frontend

  app:
    build: ./app
    expose:
      - "8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=mysql
      - DB_PORT=3306
      - DB_NAME=testdb
      - DB_USER=root
      - DB_PASSWORD=root123
    depends_on:
      mysql:
        condition: service_healthy
    volumes:
      - app-logs:/app/logs
    networks:
      - frontend
      - backend

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: testdb
    volumes:
      - mysql-data:/var/lib/mysql
      - ./mysql/init:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - backend

volumes:
  mysql-data:
  app-logs:

networks:
  frontend:
  backend:
```

### Q4: 如何搭建Docker私有仓库（Harbor）？
**A:**
```bash
# 1. 下载Harbor offline安装包
wget https://github.com/goharbor/harbor/releases/download/v2.9.0/harbor-offline-installer-v2.9.0.tgz
tar xzf harbor-offline-installer-v2.9.0.tgz
cd harbor

# 2. 配置harbor.yml
# hostname: harbor.example.com
# harbor_admin_password: Harbor12345
# 配置HTTPS证书

# 3. 安装
./prepare
./install.sh

# 4. 登录和操作
docker login harbor.example.com
docker tag myapp:latest harbor.example.com/library/myapp:latest
docker push harbor.example.com/library/myapp:latest
docker pull harbor.example.com/library/myapp:latest
```

### Q5: 部署Redis主从集群（一主两从）？
**A:**
```yaml
version: '3.8'
services:
  redis-master:
    image: redis:7-alpine
    container_name: redis-master
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis-master-data:/data
    networks:
      - redis-net

  redis-slave1:
    image: redis:7-alpine
    container_name: redis-slave1
    ports:
      - "6380:6379"
    command: redis-server --slaveof redis-master 6379 --appendonly yes
    depends_on:
      - redis-master
    volumes:
      - redis-slave1-data:/data
    networks:
      - redis-net

  redis-slave2:
    image: redis:7-alpine
    container_name: redis-slave2
    ports:
      - "6381:6379"
    command: redis-server --slaveof redis-master 6379 --appendonly yes
    depends_on:
      - redis-master
    volumes:
      - redis-slave2-data:/data
    networks:
      - redis-net

volumes:
  redis-master-data:
  redis-slave1-data:
  redis-slave2-data:

networks:
  redis-net:
    driver: bridge
```

### Q6: 制作自定义CentOS镜像并安装常用工具？
**A:**
```dockerfile
FROM centos:7
MAINTAINER devops@example.com

# 安装常用工具
RUN yum -y install epel-release && \
    yum -y install \
        net-tools \
        vim \
        wget \
        curl \
        git \
        htop \
        lsof \
        telnet \
        nc \
        tcpdump \
        && yum clean all

# 设置时区
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 配置中文字符集
ENV LANG=zh_CN.UTF-8

# 创建普通用户
RUN useradd -m -s /bin/bash devops && \
    echo "devops:password" | chpasswd

# 设置SSH
RUN yum -y install openssh-server openssh-clients && \
    ssh-keygen -t rsa -f /etc/ssh/ssh_host_rsa_key -N ""

EXPOSE 22
CMD ["/usr/sbin/sshd", "-D"]
```

### Q7: 部署Nginx反向代理多个Web服务？
**A:**
```yaml
version: '3.8'
services:
  nginx:
    image: nginx:1.24-alpine
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./html:/usr/share/nginx/html:ro
    networks:
      - webnet

  web1:
    image: nginx:alpine
    volumes:
      - ./web1:/usr/share/nginx/html:ro
    networks:
      - webnet

  web2:
    image: nginx:alpine
    volumes:
      - ./web2:/usr/share/nginx/html:ro
    networks:
      - webnet

networks:
  webnet:
    driver: bridge
```
nginx.conf:
```nginx
http {
    upstream backend {
        server web1:80 weight=3;
        server web2:80 weight=1;
    }
    server {
        listen 80;
        location / {
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
    }
}
```

---

## 四、手写代码/配置文件题（5-8题）

### Q1: 编写Dockerfile部署Spring Boot应用（生产优化版）
**A:**
```dockerfile
# 多阶段构建 - 编译阶段
FROM maven:3.8.5-openjdk-17-slim AS builder
WORKDIR /build
# 分层利用缓存：先复制pom下载依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B
# 复制源码并编译
COPY src/ src/
RUN mvn clean package -DskipTests -Pprod

# 多阶段构建 - 运行阶段
FROM openjdk:17-slim
WORKDIR /app

# 创建非root用户
RUN groupadd -r appuser && useradd -r -g appuser -d /app -s /sbin/nologin appuser

# 复制jar包
COPY --from=builder /build/target/*.jar app.jar

# 安装tini处理信号和僵尸进程
RUN apt-get update && apt-get install -y --no-install-recommends tini && rm -rf /var/lib/apt/lists/*

# 配置JVM参数优化
ENV JAVA_OPTS="-Xms512m -Xmx512m -XX:+UseG1GC -XX:+PrintGCDetails -XX:+PrintGCDateStamps"

EXPOSE 8080
VOLUME /app/logs
USER appuser
ENTRYPOINT ["tini", "--", "sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
```

### Q2: 编写docker-compose.yml部署ELK日志系统
**A:**
```yaml
version: '3.8'
services:
  elasticsearch:
    image: elasticsearch:7.17.10
    container_name: elasticsearch
    environment:
      - discovery.type=single-node
      - ES_JAVA_OPTS=-Xms1g -Xmx1g
      - xpack.security.enabled=false
    ulimits:
      memlock:
        soft: -1
        hard: -1
      nofile:
        soft: 65536
        hard: 65536
    volumes:
      - es-data:/usr/share/elasticsearch/data
    ports:
      - "9200:9200"
      - "9300:9300"
    networks:
      - elk

  logstash:
    image: logstash:7.17.10
    container_name: logstash
    volumes:
      - ./logstash/logstash.conf:/usr/share/logstash/pipeline/logstash.conf:ro
    ports:
      - "5000:5000"
      - "5044:5044"
    depends_on:
      - elasticsearch
    networks:
      - elk

  kibana:
    image: kibana:7.17.10
    container_name: kibana
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    ports:
      - "5601:5601"
    depends_on:
      - elasticsearch
    networks:
      - elk

volumes:
  es-data:

networks:
  elk:
```

### Q3: 编写Dockerfile实现前端Nginx + 静态资源部署
**A:**
```dockerfile
# 多阶段构建 - 编译前端
FROM node:18-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build

# 运行阶段 - Nginx
FROM nginx:1.24-alpine
# 复制构建产物
COPY --from=build /app/dist /usr/share/nginx/html
# 自定义Nginx配置
COPY nginx.conf /etc/nginx/conf.d/default.conf
# 配置健康检查
RUN echo "ok" > /usr/share/nginx/html/health
# 非root用户运行
RUN chown -R nginx:nginx /usr/share/nginx/html

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- http://localhost/health || exit 1
```

### Q4: 编写docker-compose实现Prometheus + Grafana监控
**A:**
```yaml
version: '3.8'
services:
  prometheus:
    image: prom/prometheus:v2.45.0
    container_name: prometheus
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--web.enable-lifecycle'
    ports:
      - "9090:9090"
    restart: always
    networks:
      - monitoring

  grafana:
    image: grafana/grafana:10.1.0
    container_name: grafana
    volumes:
      - grafana-data:/var/lib/grafana
      - ./grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./grafana/datasources:/etc/grafana/provisioning/datasources
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=admin123
    ports:
      - "3000:3000"
    depends_on:
      - prometheus
    restart: always
    networks:
      - monitoring

  node-exporter:
    image: prom/node-exporter:v1.6.0
    container_name: node-exporter
    volumes:
      - /proc:/host/proc:ro
      - /sys:/host/sys:ro
      - /:/rootfs:ro
    command:
      - '--path.procfs=/host/proc'
      - '--path.sysfs=/host/sys'
      - '--collector.filesystem.mount-points-exclude=^/(sys|proc|dev|host|etc)($$|/)'
    ports:
      - "9100:9100"
    restart: always
    networks:
      - monitoring

volumes:
  prometheus-data:
  grafana-data:

networks:
  monitoring:
```

### Q5: 编写Jenkinsfile实现Docker自动构建部署
**A:**
```groovy
pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'myapp'
        DOCKER_TAG = "${BUILD_NUMBER}"
        REGISTRY = 'registry.example.com'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }

        stage('Docker Build') {
            steps {
                sh """
                    docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                    docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${REGISTRY}/${DOCKER_IMAGE}:${DOCKER_TAG}
                    docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${REGISTRY}/${DOCKER_IMAGE}:latest
                """
            }
        }

        stage('Push') {
            steps {
                withDockerRegistry([credentialsId: 'docker-registry', url: "https://${REGISTRY}"]) {
                    sh """
                        docker push ${REGISTRY}/${DOCKER_IMAGE}:${DOCKER_TAG}
                        docker push ${REGISTRY}/${DOCKER_IMAGE}:latest
                    """
                }
            }
        }

        stage('Deploy') {
            steps {
                sh """
                    docker pull ${REGISTRY}/${DOCKER_IMAGE}:${DOCKER_TAG}
                    docker stop myapp || true
                    docker rm myapp || true
                    docker run -d --name myapp \
                        -p 8080:8080 \
                        -e SPRING_PROFILES_ACTIVE=prod \
                        ${REGISTRY}/${DOCKER_IMAGE}:${DOCKER_TAG}
                """
            }
        }
    }

    post {
        failure {
            slackSend(channel: '#devops', color: 'danger', message: "Build failed: ${env.BUILD_URL}")
        }
        success {
            slackSend(channel: '#devops', color: 'good', message: "Build success: ${env.BUILD_URL}")
        }
    }
}
```

### Q6: docker-compose.yml完整语法（多环境配置）
**A:**
```yaml
version: '3.8'

x-common: &common
  restart: always
  networks:
    - app-network

services:
  app:
    <<: *common
    build:
      context: .
      dockerfile: Dockerfile
      args:
        - BUILD_ENV=${BUILD_ENV:-dev}
    image: myapp:${TAG:-latest}
    ports:
      - "${APP_PORT:-8080}:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-dev}
      - DB_HOST=db
      - DB_PORT=3306
      - DB_NAME=${DB_NAME:-testdb}
    env_file:
      - ./config/${SPRING_PROFILES_ACTIVE:-dev}.env
    volumes:
      - app-logs:/app/logs
      - type: bind
        source: ./config
        target: /app/config
    depends_on:
      db:
        condition: service_healthy
      redis:
        condition: service_started
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 40s
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"

  db:
    <<: *common
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD:-root123}
      MYSQL_DATABASE: ${DB_NAME:-testdb}
    volumes:
      - db-data:/var/lib/mysql
      - ./sql/init:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    <<: *common
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes --requirepass ${REDIS_PASSWORD:-redis123}

volumes:
  db-data:
  redis-data:
  app-logs:

networks:
  app-network:
    driver: bridge
```

---

## 五、系统设计题（3-5题）

### Q1: 设计一套基于Docker的微服务部署架构
**A:**
```
                   +-----------+
                   |  Nginx    |  (反向代理 / 负载均衡 / SSL终止)
                   |  80/443   |
                   +-----+-----+
                         |
          +--------------+--------------+
          |              |              |
    +-----v-----+  +-----v-----+  +-----v-----+
    | Service A |  | Service B |  | Service C |  (微服务容器)
    |  :8080    |  |  :8080    |  |  :8080    |
    +-----+-----+  +-----+-----+  +-----+-----+
          |              |              |
    +-----v--------------v--------------v-----+
    |           API Gateway (Spring Gateway)   |
    |              :8080                       |
    +-----+--------------+--------------+-----+
          |              |              |
    +-----v-----+  +-----v-----+  +-----v-----+
    |  MySQL    |  |  Redis    |  |  RabbitMQ |
    |  主从架构  |  |  集群    |  |  集群     |
    +-----------+  +-----------+  +-----------+
```

关键设计要点：
1. **服务发现**: 使用Consul或Eureka实现容器服务注册发现
2. **配置中心**: Apollo或Nacos统一管理配置
3. **链路追踪**: SkyWalking + Jaeger分布式追踪
4. **日志中心**: ELK + Filebeat统一日志收集
5. **监控告警**: Prometheus + Grafana + AlertManager
6. **容器编排**: Kubernetes管理集群
7. **CI/CD**: Jenkins/GitLab CI自动构建和部署
8. **镜像仓库**: Harbor管理镜像版本

### Q2: 设计一套高可用的Docker CI/CD流水线
**A:**
```
开发者推送代码
      |
      v
  触发WebHook
      |
      v
+------------------+
|   代码仓库       |  GitHub/GitLab/Gitee
+------------------+
      |
      v (WebHook自动触发)
+------------------+
|   CI服务器       |  Jenkins/GitLab Runner/GitHub Actions
+------------------+
      |
  1. Checkout代码
  2. 单元测试并行
  3. 代码质量扫描 (SonarQube)
  4. 安全扫描 (Trivy)
  5. Docker镜像构建
  6. Docker镜像推送 (Harbor)
      |
      v
+------------------+
|   CD部署         |
+------------------+
      |
  开发环境(docker-compose)
      |
  测试环境 (自动化测试)
      |
  预发布环境 (金丝雀发布)
      |
  生产环境 (滚动更新)
```

### Q3: 设计容器化应用的健康检查与自愈机制
**A:**
```yaml
# Docker Compose健康检查
services:
  app:
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    restart: always
    deploy:
      restart_policy:
        condition: any
        delay: 5s
        max_attempts: 3
        window: 120s

# 应用层健康检查端点
@RestController
@SpringBootApplication
public class Application {
    @GetMapping("/actuator/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        // 检查数据库连接
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            status.put("database", "UP");
        } catch (Exception e) {
            status.put("database", "DOWN");
            return ResponseEntity.status(503).body(status);
        }
        // 检查Redis连接
        try {
            redisTemplate.opsForValue().get("health");
            status.put("redis", "UP");
        } catch (Exception e) {
            status.put("redis", "DOWN");
            return ResponseEntity.status(503).body(status);
        }
        return ResponseEntity.ok(status);
    }
}
```

### Q4: 设计一套基于Docker的灰度发布方案
**A:**
```yaml
# Docker Compose灰度发布
version: '3.8'
services:
  nginx:
    image: nginx:1.24-alpine
    volumes:
      - ./nginx-gray.conf:/etc/nginx/conf.d/default.conf:ro
    ports:
      - "80:80"

  app-v1:
    image: myapp:1.0.0
    environment:
      - APP_VERSION=v1
    # 处理90%流量
    deploy:
      replicas: 9

  app-v2:
    image: myapp:2.0.0
    environment:
      - APP_VERSION=v2
    # 处理10%流量
    deploy:
      replicas: 1
```
nginx灰度配置：
```nginx
upstream backend {
    # 基于cookie实现灰度
    hash $cookie_user_id consistent;
    
    server app-v1:8080 weight=9;
    server app-v2:8080 weight=1;
}

server {
    listen 80;
    location / {
        proxy_pass http://backend;
        # 根据Header选择版本
        if ($http_x_canary = "true") {
            proxy_pass http://app-v2:8080;
        }
    }
}
```

---

## 六、常见坑点与最佳实践（表格）

| 坑点/问题 | 原因 | 解决方案 | 最佳实践 |
|-----------|------|----------|----------|
| 容器内时区错误 | 默认UTC时区 | 挂载 `/etc/localtime` 或设置 `TZ=Asia/Shanghai` | 在Dockerfile中设置时区 |
| 容器内乱码 | 缺少中文字符集 | 安装中文字体包，设置 `LANG=zh_CN.UTF-8` | 使用标准UTF-8编码 |
| 容器退出后数据丢失 | 容器可写层随容器删除 | 使用数据卷或bind mount持久化数据 | 所有持久数据使用Volume |
| 镜像体积过大 | 包含构建工具、缓存文件 | 多阶段构建，使用alpine基础镜像 | 最终镜像仅包含运行时 |
| 容器内无法访问网络 | 未指定网络模式 | 使用 `--network=host` 或自定义网络 | 使用自定义bridge网络 |
| 容器启动后马上退出 | 前台进程未保持运行 | 确保CMD/ENTRYPOINT运行前台进程 | 使用 `tail -f` 或应用前台模式 |
| 权限不足访问Volume | 容器用户UID与宿主机不匹配 | 指定用户UID运行，或修改目录权限 | 保持UID一致，避免使用root |
| Docker build缓存失效 | 文件时间戳或内容变化 | 将变化少的指令放在Dockerfile前面 | 分层利用缓存，先复制依赖文件 |
| docker-compose up慢 | 镜像拉取串行 | 使用 `docker-compose pull` 预拉取 | 配置镜像加速器 |
| 容器日志撑爆磁盘 | 日志滚动未配置 | 配置 `--log-opt max-size=10m max-file=3` | 全局配置日志限制 |
| 端口冲突 | 多个容器占用同一端口 | 映射到不同宿主机端口 | 使用反向代理统一入口 |
| 容器资源争抢 | 未限制CPU/内存 | 配置 `--cpus` 和 `-m` 参数 | 统一使用docker-compose资源限制 |

---

## 七、面试回答模板（Top 5）

### 模板1: 请简述Docker的原理和优势
**回答框架：**
- **定义**: Docker是基于Linux容器（LXC）技术的应用容器引擎
- **核心原理**: 利用Linux Namespace实现资源隔离，Cgroups实现资源限制，UnionFS实现镜像分层
- **优势**: 轻量级（共享内核，秒级启动）、一致环境（消除开发/生产差异）、高效资源利用、快速部署和扩缩
- **适用场景**: 微服务架构、CI/CD流水线、DevOps实践、弹性伸缩

### 模板2: Dockerfile的优化构建方法
**回答框架：**
1. 使用多阶段构建分离编译和运行环境
2. 选择尽量小的基础镜像（alpine、slim）
3. 按变化频率排列指令，利用Docker缓存
4. 合并RUN命令减少层数
5. 使用.dockerignore排除无关文件
6. 不运行root用户，创建专用用户
7. 精确COPY而不是全量复制
8. 使用标签管理版本，避免latest

### 模板3: Docker Compose和Dockerfile的区别
**回答框架：**
- **Dockerfile**: 描述如何构建单个镜像（编译、安装、配置）
- **Docker Compose**: 编排多个容器如何协同工作（服务依赖、网络、存储）
- **类比**: Dockerfile如房屋装修图纸，Compose如社区规划图
- **配合使用**: Dockerfile定义服务构建，Compose定义服务关系

### 模板4: Docker网络模式及选择依据
**回答框架：**
- **bridge（默认）**: 适合单机多容器通信，使用docker0网桥
- **host**: 性能最优，适合网络密集型应用，但端口冲突风险高
- **none**: 完全隔离，适合安全敏感场景
- **overlay**: 跨主机通信，适合Swarm/Kubernetes集群
- **选择依据**: 单机选bridge，跨主机选overlay，性能敏感选host

### 模板5: Docker和虚拟机对比
**回答框架：**

| 维度 | Docker | VM |
|------|--------|-----|
| 架构 | 共享宿主机内核 | 包含完整Guest OS |
| 启动时间 | 毫秒~秒级 | 分钟级 |
| 镜像大小 | MB级别 | GB级别 |
| 性能开销 | ~5% | ~15-20% |
| 隔离程度 | 进程级隔离 | 硬件级虚拟化 |
| 资源密度 | 单机上千容器 | 单机几十台 |
| 适用场景 | 微服务/DevOps | 不同类型OS需求/强隔离 |

---

## 八、快速查漏补缺Checklist

- [ ] Docker架构（C/S模式，Daemon、Client、Registry）
- [ ] 镜像相关命令（pull、search、images、rmi、tag、push、build）
- [ ] 容器相关命令（run、start、stop、restart、rm、ps、logs、exec、inspect）
- [ ] 数据卷概念与三种类型（Bind Mount、Volume、tmpfs）
- [ ] 数据卷容器 `--volumes-from` 共享方式
- [ ] 网络模式（bridge、host、none、overlay、macvlan）
- [ ] 自定义网络创建与容器互联
- [ ] Dockerfile关键指令（FROM、RUN、COPY、CMD、ENTRYPOINT、ENV、EXPOSE、VOLUME、WORKDIR、USER）
- [ ] CMD与ENTRYPOINT的区别和组合使用
- [ ] 镜像分层机制和UnionFS原理
- [ ] 多阶段构建（Multistage Build）
- [ ] 构建缓存利用策略
- [ ] Docker Compose三大要素（services、networks、volumes）
- [ ] Compose依赖控制（depends_on + healthcheck）
- [ ] Docker私有仓库搭建（Registry和Harbor）
- [ ] Harbor企业级功能（LDAP、复制、扫描、审计）
- [ ] Namespace六大隔离类型
- [ ] Cgroups资源限制（CPU、内存、磁盘I/O）
- [ ] 容器资源限制参数（--cpus、-m、--device-read-bps）
- [ ] Docker Healthcheck配置
- [ ] .dockerignore文件编写
- [ ] Docker与虚拟机对比（5个维度）
- [ ] Swarm与Kubernetes核心区别
- [ ] Jenkins/Docker持续集成流水线
- [ ] 容器化日志管理（json-file、ELK、Fluentd）
- [ ] 容器化监控（Prometheus、Grafana、cAdvisor）
- [ ] 容器安全（非root用户、镜像签名、安全扫描）
- [ ] 应用容器化最佳实践（MySQL、Redis、Nginx、SpringBoot）
- [ ] 容器时区、编码、权限常见问题解决
- [ ] Docker Compose多环境配置文件管理
