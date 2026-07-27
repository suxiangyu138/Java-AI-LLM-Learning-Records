# 02 - 注册中心：Nacos 服务发现

> 🎯 Nacos（Na ming + Co nfig + Service）是阿里开源的服务注册+配置中心 — 替代 Eureka + Config 的组合，是 Spring Cloud Alibaba 生态的核心基石

---

## 目录

1. [Nacos 安装与启动](#1-nacos-安装与启动)
2. [服务注册与发现](#2-服务注册与发现)
3. [Nacos 核心概念](#3-nacos-核心概念)
4. [健康检查机制](#4-健康检查机制)
5. [Nacos 集群部署](#5-nacos-集群部署)

---

## 1. Nacos 安装与启动

### 1.1 Docker 快速启动

```bash
docker run -d --name nacos -p 8848:8848 -p 9848:9848 \
  -e MODE=standalone \
  nacos/nacos-server:v2.3.0
# 访问 http://localhost:8848/nacos  (nacos/nacos)
```

### 1.2 Nacos 依赖

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

---

## 2. 服务注册与发现

### 2.1 服务提供者

```yaml
# application.yml
spring:
  application:
    name: user-service            # ⭐ 服务名 = Nacos 中的注册名
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        namespace: dev             # 命名空间（环境隔离）
        group: DEFAULT_GROUP
        ephemeral: true            # 临时实例（默认）
```

```java
@SpringBootApplication
@EnableDiscoveryClient              // ⭐ 开启服务发现（新版可省略）
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

### 2.2 服务消费者

```java
@RestController
public class OrderController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/order/{id}")
    public Order getOrder(@PathVariable Long id) {
        // ⭐ 服务名代替 IP:Port
        String url = "http://user-service/users/" + id;
        return restTemplate.getForObject(url, User.class);
    }
}

@Configuration
class RestConfig {
    @Bean
    @LoadBalanced            // ⭐ 开启负载均衡
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

### 2.3 Nacos 控制台查看

```
服务管理 → 服务列表 → user-service
  ├── 实例数: 2
  ├── 健康实例: 2
  └── 详情:
      ├── 192.168.1.10:8080 (健康)
      └── 192.168.1.11:8080 (健康)
```

---

## 3. Nacos 核心概念

| 概念 | 说明 | 用途 |
|------|------|------|
| **Namespace** | 命名空间，环境级隔离 | dev / test / prod 环境隔离 |
| **Group** | 服务分组 | 同一环境下不同版本分组 |
| **Service** | 服务，对应 `spring.application.name` | 服务名标识 |
| **Instance** | 服务实例（IP:Port） | 具体部署节点 |

```text
Nacos 数据隔离层级：

Namespace (dev) ────── Namespace (prod)
  ├── Group (DEFAULT)     ├── Group (DEFAULT)
  │   ├── user-service    │   ├── user-service
  │   └── order-service   │   └── order-service
  └── Group (gray)        └── Group (gray)
      └── user-service        └── user-service (灰度版本)

不同 Namespace 的服务完全隔离，互不可见！
```

---

## 4. 健康检查机制

### 4.1 临时实例 vs 永久实例

| 类型 | 注册方式 | 健康检查 | 宕机行为 |
|------|----------|----------|----------|
| **临时实例** | 自动注册（spring cloud） | 心跳（5s） | 15s 无心跳 → 剔除 |
| **永久实例** | 手动注册 | Nacos 主动探测 | 不剔除，标记不健康 |

### 4.2 Nacos vs Eureka 健康检查

| 维度 | Nacos | Eureka |
|------|-------|--------|
| CAP 模型 | AP / CP 可切换 | AP |
| 健康检查 | TCP + HTTP + MySQL | 心跳 |
| 自我保护 | ❌ 无 | ✅ 15min 心跳失败>85%触发 |
| 实例类型 | 临时+永久 | 仅临时 |

---

## 5. Nacos 集群部署

```yaml
# docker-compose.yml — Nacos 集群
version: '3'
services:
  nacos1:
    image: nacos/nacos-server:v2.3.0
    ports: ["8848:8848", "9848:9848"]
    environment:
      MODE: cluster
      NACOS_SERVERS: nacos1:8848 nacos2:8848 nacos3:8848
      MYSQL_SERVICE_HOST: mysql-host
    ...
  nacos2: ...
  nacos3: ...
```

```text
集群要求：
  → 至少 3 个节点（Raft 选主需要多数派）
  → 需要 MySQL 存储配置信息（非内置 Derby）
  → 客户端配置 server-addr 用逗号分隔：127.0.0.1:8848,127.0.0.1:8849,127.0.0.1:8850
```

> 🎯 **最佳实践**：开发单机模式用内置 Derby，生产必须集群+MySQL。namespace 按环境隔离（dev/test/prod），group 按版本隔离（v1/v2/gray）。
