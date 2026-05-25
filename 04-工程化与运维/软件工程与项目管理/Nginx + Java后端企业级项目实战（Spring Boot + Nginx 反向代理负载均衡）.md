Nginx + Java后端企业级项目实战（Spring Boot + Nginx 反向代理/负载均衡）
项目简介
基于 Nginx + Spring Boot + MySQL + Redis 构建企业级 Java 后端项目，实现：
- Nginx 反向代理（隐藏后端端口、统一入口）
- Nginx 负载均衡（多实例部署、高可用）
- 动静分离（静态资源走 Nginx，动态请求走 Java）
- HTTPS 配置（企业级安全）
- 限流、缓存、跨域、gzip 压缩
    技术栈
- 后端：Spring Boot 2.7.x、MyBatis-Plus、MySQL、Redis
- 服务器：Nginx 1.24+、Tomcat（内嵌）
- 部署：多实例、负载均衡、反向代理
- 安全：HTTPS、限流、跨域
    项目结构
    plaintext
    nginx-java-project/
    ├── backend/                # Spring Boot 项目
    │   ├── src/
    │   ├── pom.xml
    │   └── application.yml
    ├── nginx/                   # Nginx 配置
    │   ├── nginx.conf           # 主配置
    │   ├── conf.d/
    │   │   └── java-api.conf    # 反向代理 + 负载均衡
    │   └── ssl/                  # HTTPS 证书
    └── static/                   # 静态资源（前端/图片等）
 
 
一、Spring Boot 后端项目（简单 API）
1. pom.xml
    xml
    <dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
    </dependency>
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-boot-starter</artifactId>
        <version>3.5.3</version>
    </dependency>
    </dependencies>
 
2. application.yml
    yaml
    server:
  port: 8081
    spring:
  datasource:
    url: jdbc:mysql://localhost:3306/nginx_java?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root
  redis:
    host: localhost
    port: 6379
 
3. 测试 API（TestController.java）
    java
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.RestController;
    @RestController
    public class TestController {
    @GetMapping("/api/hello")
    public String hello() {
        return "Hello from Spring Boot (Port: 8081)";
    }
    @GetMapping("/api/user")
    public String user() {
        return "User Info Response";
    }
    }
 
 
二、Nginx 安装（Linux/CentOS）
bash
yum install nginx -y
systemctl start nginx
systemctl enable nginx
 
验证：
plaintext
http://服务器IP
 
 
三、Nginx 核心配置（企业级）
1. nginx.conf（主配置）
    nginx
    user nginx;
    worker_processes auto;
    error_log /var/log/nginx/error.log;
    pid /run/nginx.pid;
    events {
    worker_connections 1024;
    }
    http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    # 日志格式
    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';
    access_log /var/log/nginx/access.log main;
    sendfile on;
    keepalive_timeout 65;

    # gzip 压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript;

    # 包含子配置
    include /etc/nginx/conf.d/*.conf;
}
 
2. 反向代理 + 负载均衡（/etc/nginx/conf.d/java-api.conf）
    nginx

# 后端服务集群（负载均衡）
upstream java_backend {
    server 127.0.0.1:8081;  # 实例1
    server 127.0.0.1:8082;  # 实例2（复制一份 Spring Boot，改端口）
}
server {
    listen 80;
    server_name 你的域名或IP;

    # 动态请求代理到 Java
    location /api/ {
        proxy_pass http://java_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 静态资源直接由 Nginx 返回
    location /static/ {
        root /usr/share/nginx/html;
        expires 7d;
    }

    # 限流配置
    limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
    location /api/ {
        limit_req zone=api burst=20 nodelay;
        proxy_pass http://java_backend;
    }
}
 
 
四、启动多个 Spring Boot 实例（模拟集群）
复制 jar 包，启动两个实例：
bash
java -jar backend.jar --server.port=8081
java -jar backend.jar --server.port=8082
 
 
五、测试 Nginx 转发
访问：
plaintext
http://你的IP/api/hello
 
你会看到：
plaintext
Hello from Spring Boot (Port: 8081)
或
Hello from Spring Boot (Port: 8082)
 
Nginx 自动负载均衡。
 
六、企业级 HTTPS 配置（可选）
1. 准备证书（cert.pem、key.pem）
2. 配置 HTTPS
    nginx
    server {
    listen 443 ssl;
    server_name 你的域名;
    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    location /api/ {
        proxy_pass http://java_backend;
    }
    }

# HTTP 自动跳转 HTTPS
server {
    listen 80;
    server_name 你的域名;
    return 301 https://$host$request_uri;
}
 
 
七、Nginx 高级企业功能（必学）
1. 限流
    nginx
    limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
    location /api/ {
    limit_req zone=api burst=20 nodelay;
    proxy_pass http://java_backend;
    }
 
2. 缓存（减轻 Java 压力）
    nginx
    proxy_cache_path /var/nginx/cache levels=1:2 keys_zone=cache:10m inactive=60m;
    location /api/user {
    proxy_cache cache;
    proxy_cache_valid 200 302 10m;
    proxy_pass http://java_backend;
    }
 
3. 跨域配置
    nginx
    add_header Access-Control-Allow-Origin *;
    add_header Access-Control-Allow-Methods GET,POST,PUT,DELETE;
 
4. 健康检查（Nginx Plus 或第三方模块）
    nginx
    upstream java_backend {
    server 127.0.0.1:8081 max_fails=3 fail_timeout=30s;
    server 127.0.0.1:8082 max_fails=3 fail_timeout=30s;
    }
 
 
八、项目架构图（企业级）
plaintext
用户 → DNS → Nginx（80/443）
        ↓
负载均衡 → 8081、8082（Spring Boot 集群）
        ↓
MySQL + Redis
 
 
九、运行步骤
1. 启动 MySQL、Redis
2. 启动 2 个 Spring Boot 实例（8081、8082）
3. 配置 Nginx
4. 重载 Nginx： nginx -s reload 
5. 访问  http://IP/api/hello 
 
十、你学到的企业级技能
- Nginx 反向代理
- Nginx 负载均衡（轮询/IP 哈希/权重）
- 动静分离
- HTTPS 部署
- 限流、缓存、跨域
- 后端集群部署
- 高可用架构
    如果你需要，我可以继续带你：
- 用 Docker 容器化部署
- 用 Jenkins + Nginx 实现 CI/CD
- 实现微服务 + Nginx 网关
