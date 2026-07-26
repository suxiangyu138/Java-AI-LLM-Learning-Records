# 01 - Nginx 概述与核心原理

> 🎯 Nginx 是高性能 HTTP 服务器与反向代理，Master-Worker 进程模型+事件驱动架构，单机 10 万+并发 — 理解其底层原理是用好 Nginx 的前提

---

## 目录

1. [Nginx 是什么](#一nginx-是什么)
2. [核心用途（Java 后端视角）](#二核心用途java-后端视角)
3. [进程模型：Master-Worker](#三进程模型master-worker)
4. [事件驱动与 IO 多路复用](#四事件驱动与-io-多路复用)
5. [正向代理 vs 反向代理](#五正向代理-vs-反向代理)
6. [Nginx vs Apache vs Tomcat](#六nginx-vs-apache-vs-tomcat)

---

## 一、Nginx 是什么

Nginx 是高性能的 **HTTP 服务器**和**反向代理服务器**，同时支持 IMAP/POP3/SMTP 代理。

| 特性 | 说明 |
|------|------|
| 高并发 | 单机支持 10 万+ 并发连接，基于 epoll IO 多路复用 |
| 低内存 | 内存占用仅几十 MB，远低于传统服务器 |
| 热部署 | `nginx -s reload` 平滑重载配置，无需停机 |
| 模块化 | 功能通过模块扩展，按需编译 |

## 二、核心用途（Java 后端视角）

```
客户端 → Nginx（守门人 + 调度员） → Tomcat/Spring Boot（业务处理） → 数据库
         ├── 过滤无效请求
         ├── 处理静态资源（不经过 Java）
         ├── 分发动态请求到 Java 服务
         ├── 负载均衡
         └── SSL 终结
```

| 用途 | 说明 |
|------|------|
| **静态资源服务** | 直接返回 HTML/CSS/JS/图片，速度远超 Tomcat |
| **反向代理** | 隐藏后端 Java 服务真实地址，统一请求入口 |
| **负载均衡** | 请求分发到多台 Java 服务节点，故障自动切换 |
| **动静分离** | 静态资源 Nginx 处理，动态接口转发 Java 服务 |
| **HTTPS** | SSL 证书部署在 Nginx 层，Java 后端无需处理 |
| **限流/缓存/防盗链** | 保护后端服务，优化性能 |

## 三、Master-Worker 进程架构

```
Master 进程（管理）
  ├── Worker 1（处理请求，epoll 非阻塞）
  ├── Worker 2
  ├── Worker 3
  └── ...（数量 = CPU 核心数）
```

- **Master**：读取配置、管理 Worker、处理信号，不处理请求
- **Worker**：基于 epoll 模型，单 Worker 可处理数万连接，非阻塞 IO

### 请求处理流程

```
客户端请求 → Nginx
  ├── 静态资源（.jpg/.css/.js）→ 直接读本地文件返回
  └── 动态请求（/api/*）→ proxy_pass 转发到 Java 服务
```

## 四、配置结构

```nginx
# 1. 全局块
worker_processes  auto;

# 2. events 块
events {
    worker_connections  1024;
}

# 3. http 块
http {
    # 全局 HTTP 配置
    include       mime.types;
    gzip          on;

    # 负载均衡集群
    upstream backend {
        server 127.0.0.1:8080;
        server 127.0.0.1:8081;
    }

    # 4. server 块（虚拟主机）
    server {
        listen       80;
        server_name  example.com;

        # 5. location 块（请求匹配）
        location / {
            root html;
        }
        location /api/ {
            proxy_pass http://backend/api/;
        }
    }
}
```

## 五、常用命令

```bash
nginx                    # 启动
nginx -s stop            # 快速停止
nginx -s quit            # 平稳退出
nginx -s reload          # 重载配置（不重启）
nginx -t                 # 检查配置语法
nginx -v                 # 查看版本
```

## 六、Nginx vs Tomcat

| 维度 | Nginx | Tomcat |
|------|-------|--------|
| 定位 | HTTP 服务器 + 反向代理 | Servlet 容器 |
| 静态资源 | **极快**（直接文件 IO） | 慢（需 Java 处理） |
| 并发能力 | 10 万+ | 几百 ~ 几千 |
| 动态处理 | 转发给 Java 服务 | 原生支持 Servlet |
| 内存占用 | 几十 MB | 几百 MB ~ GB |

## 七、经典架构

```
用户 → Nginx（80/443 端口）
  ├── 静态资源 → Nginx 直接返回
  ├── /api/* → 负载均衡 → Java 集群（8080/8081）
  └── SSL 终结于 Nginx，后端 HTTP 通信
```

**优势**：安全（隐藏后端）、高效（动静分离）、稳定（健康检查 + 故障切换）、易扩展（增加节点即可）。
