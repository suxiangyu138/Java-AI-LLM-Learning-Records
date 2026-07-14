# 02 — Spring Cloud 组件 (Spring Cloud Components)

> **Core Reading**: This document covers the major Spring Cloud components used in enterprise microservice architectures. It focuses on Nacos, Spring Cloud Gateway, Resilience4j, Sentinel, Seata, and distributed tracing.

---

## Table of Contents

1. [Spring Cloud Ecosystem Overview](#spring-cloud-ecosystem-overview)
2. [Service Registration & Discovery Comparison](#service-registration--discovery-comparison)
3. [Nacos Deep Dive](#nacos-deep-dive)
4. [Consul Integration](#consul-integration)
5. [Eureka (Maintenance Mode)](#eureka-maintenance-mode)
6. [Configuration Center](#configuration-center)
7. [Remote Invocation: OpenFeign](#remote-invocation-openfeign)
8. [Load Balancing](#load-balancing)
9. [Spring Cloud Gateway](#spring-cloud-gateway)
10. [Resilience4j](#resilience4j)
11. [Sentinel](#sentinel)
12. [Distributed Tracing](#distributed-tracing)
13. [Seata: Distributed Transactions](#seata-distributed-transactions)
14. [Interview Questions](#interview-questions)

---

## Spring Cloud Ecosystem Overview

Spring Cloud is a suite of tools for building distributed systems on the JVM. It provides out-of-the-box implementations of common distributed system patterns.

### Version Compatibility Matrix

| Spring Boot | Spring Cloud | Spring Cloud Alibaba | Status |
|---|---|---|---|
| 3.2.x | 2023.0.x (aka Leyton) | 2023.0.1.0 | Current |
| 3.1.x | 2022.0.x (aka Kilburn) | 2022.0.0.0 | Maintenance |
| 3.0.x | 2022.0.x | 2022.0.0.0 | EOL |
| 2.7.x | 2021.0.x (aka Jubilee) | 2021.0.5.0 | EOL |
| 2.6.x | 2021.0.x | 2021.0.1.0 | EOL |

> **Note**: Spring Cloud version naming changed from "Dalston/Edison/Finchley" (London tube stations) to "2020.0.x" (year-based). The latest train is **Leyton** (2023.0.x).

### Core Modules Overview

```xml
<!-- Spring Cloud 2023.0.x BOM -->
<properties>
    <spring-cloud.version>2023.0.1</spring-cloud.version>
    <spring-cloud-alibaba.version>2023.0.1.0</spring-cloud-alibaba.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>${spring-cloud-alibaba.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

| Module (Spring Cloud) | Description | Status (2025) |
|---|---|---|
| **Spring Cloud Gateway** | Reactive API Gateway (WebFlux-based) | Active |
| **Spring Cloud LoadBalancer** | Client-side load balancer | Active (replaces Ribbon) |
| **Spring Cloud Circuit Breaker** | Abstraction over Resilience4j/Hystrix/Sentinel | Active |
| **Spring Cloud Config** | External configuration (Git-backed) | Active |
| **Spring Cloud Bus** | Event bus for config refresh, status changes | Active |
| **Spring Cloud OpenFeign** | Declarative REST client | Active |
| **Spring Cloud Sleuth** → **Micrometer Tracing** | Distributed tracing | Sleuth deprecated, use Micrometer |
| **Spring Cloud Netflix** | Eureka, Hystrix, Ribbon, Zuul | Maintenance/Deprecated |

| Module (Spring Cloud Alibaba) | Description | Status |
|---|---|---|
| **Nacos Discovery** | Service registry + discovery | Active |
| **Nacos Config** | External configuration | Active |
| **Sentinel** | Flow control, circuit breaking, hot-spot protection | Active |
| **Seata** | Distributed transaction solution | Active |
| **RocketMQ** | Message queue integration | Active |

### Minimal Parent POM for a Spring Cloud Microservice

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>order-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <properties>
        <java.version>21</java.version>
        <spring-cloud.version>2023.0.1</spring-cloud.version>
        <spring-cloud-alibaba.version>2023.0.1.0</spring-cloud-alibaba.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring-cloud-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Nacos: Service Discovery + Config -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>

        <!-- OpenFeign -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>

        <!-- LoadBalancer -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-loadbalancer</artifactId>
        </dependency>

        <!-- Circuit Breaker (Resilience4j) -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
        </dependency>

        <!-- Distributed Tracing -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-tracing-bridge-brave</artifactId>
        </dependency>
        <dependency>
            <groupId>io.zipkin.reporter2</groupId>
            <artifactId>zipkin-reporter-brave</artifactId>
        </dependency>

        <!-- Metrics -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>
    </dependencies>
</project>
```

---

## Service Registration & Discovery Comparison

### What Each Tool Provides

| Feature | Nacos | Consul | Eureka | Zookeeper |
|---|---|---|---|---|
| **CAP** | CP + AP (switchable) | CP | AP | CP |
| **Health check** | HTTP/TCP/MySQL/gRPC | HTTP/TCP/gRPC/Script | HTTP heartbeat only | TCP session keepalive |
| **Self-preservation** | Configurable | N/A | Yes (default ON) | N/A |
| **KV Store** | Yes | Yes (native) | No | Yes (znodes) |
| **Multi-DC** | Yes | Yes (native) | Yes (limited) | Yes |
| **Admin UI** | Rich web console | Rich web console | Basic UI | Basic (3rd party tools) |
| **Spring Boot version** | 2.x, 3.x | 2.x, 3.x | Mainly 2.x | 2.x, 3.x |
| **Protocol** | HTTP/gRPC | HTTP/DNS/RPC | HTTP | ZooKeeper protocol |
| **Community** | Very active (Alibaba) | Active (HashiCorp) | Minimal (Netflix OSS) | Active (Apache) |
| **Performance** | High | Medium | Medium | Medium |

### When to Use What

| Scenario | Recommendation |
|---|---|
| Need both registry and config center | **Nacos** (kills two birds with one stone) |
| Existing HashiCorp infrastructure | **Consul** |
| Need multi-DC with DNS-based discovery | **Consul** |
| Simple setup, AP preferred, low maintenance | **Eureka** (if already using Netflix stack) |
| Need ZK for other purposes (Kafka, distributed lock) | **Zookeeper** |
| New greenfield project in Chinese tech ecosystem | **Nacos** |
| Kubernetes-native deployment | **Kubernetes DNS/Services** (no need for external registry) |

### Spring Boot 3.x Compatibility

> **IMPORTANT**: Spring Boot 3.x requires Spring Framework 6.x, which is based on Jakarta EE (javax.* → jakarta.*). Ensure your Spring Cloud version is compatible.

```xml
<!-- Nacos with Spring Boot 3.x (works) -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>

<!-- Eureka with Spring Boot 3.x (works, but limited support) -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

---

## Nacos Deep Dive

Nacos (Na ming and Co nfiguration Service) is Alibaba's open-source service registry and configuration center. It is the most popular choice in the Chinese Java ecosystem.

### Architecture Overview

```
                    ┌─────────────────────────────────────┐
                    │          Nacos Cluster              │
                    │                                     │
                    │  ┌──────────┐    ┌──────────┐      │
                    │  │  Node 1  │◄───│  Node 2  │      │
                    │  │ (Leader) │    │(Follower)│      │
                    │  └────┬─────┘    └────┬─────┘      │
                    │       │               │             │
                    │  ┌────▼─────┐    ┌────▼─────┐      │
                    │  │  Node 3  │    │  Node 4  │      │
                    │  │(Follower)│    │(Follower)│      │
                    │  └──────────┘    └──────────┘      │
                    │                                     │
                    │  Protocols:                         │
                    │  - Raft (CP mode, persistent)       │
                    │  - Distro (AP mode, eventual)       │
                    └─────────────────────────────────────┘
                               │
               ┌───────────────┼───────────────┐
               │               │               │
         ┌─────▼────┐   ┌─────▼────┐   ┌─────▼────┐
         │ Service  │   │ Service  │   │ Service  │
         │    A     │   │    B     │   │    C     │
         └──────────┘   └──────────┘   └──────────┘
```

### Nacos Data Model

```
Nacos Data Model:
┌─────────────────────────────────────────────────────────────┐
│  Namespace: dev / test / prod                              │
│  (Isolates environments)                                    │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Group: DEFAULT_GROUP                               │   │
│  │  (Organizes services/configs)                       │   │
│  │                                                     │   │
│  │  ┌──────────────────────────────────────────────┐   │   │
│  │  │  Service: order-service                      │   │   │
│  │  │                                              │   │   │
│  │  │  ┌──────────────────────────────────────┐   │   │   │
│  │  │  │  Instance 1: 10.0.0.1:8081          │   │   │   │
│  │  │  │  - Weight: 1.0                       │   │   │   │
│  │  │  │  - Healthy: true                     │   │   │   │
│  │  │  │  - Metadata: {version: "1.0", ...}  │   │   │   │
│  │  │  └──────────────────────────────────────┘   │   │   │
│  │  │  ┌──────────────────────────────────────┐   │   │   │
│  │  │  │  Instance 2: 10.0.0.2:8081          │   │   │   │
│  │  │  │  - Weight: 2.0                       │   │   │   │
│  │  │  │  - Healthy: true                     │   │   │   │
│  │  │  └──────────────────────────────────────┘   │   │   │
│  │  └──────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Nacos Setup

```yaml
# Docker Compose for Nacos (standalone dev mode)
version: "3.8"
services:
  nacos:
    image: nacos/nacos-server:v2.3.2
    container_name: nacos-standalone
    ports:
      - "8848:8848"   # Client API
      - "9848:9848"   # gRPC port
      - "9849:9849"   # gRPC port for cluster
    environment:
      MODE: standalone
      # JVM config
      JVM_XMS: 512m
      JVM_XMX: 512m
      JVM_XMN: 256m
      # Auth (enable for production)
      NACOS_AUTH_ENABLE: "true"
      NACOS_AUTH_TOKEN: "SecretKey012345678901234567890123456789"
      NACOS_AUTH_IDENTITY_KEY: "nacos"
      NACOS_AUTH_IDENTITY_VALUE: "nacos"
    volumes:
      - nacos_logs:/home/nacos/logs
      - nacos_data:/home/nacos/data

volumes:
  nacos_logs:
  nacos_data:
```

```bash
# Run
docker compose up -d nacos

# Access admin UI: http://localhost:8848/nacos
# Default credentials: nacos/nacos
```

### Spring Boot Integration: Service Discovery

```yaml
# bootstrap.yml — loaded BEFORE application.yml
spring:
  application:
    name: order-service             # Service name used for registration

  cloud:
    nacos:
      discovery:
        enabled: true
        server-addr: 127.0.0.1:8848
        namespace: dev_001          # Must match a namespace created in Nacos UI
        group: DEFAULT_GROUP
        service: order-service      # Service name (defaults to spring.application.name)
        weight: 1.0                 # Load balancing weight (1.0 = default)
        metadata:
          version: "1.0.0"
          region: "cn-beijing"
        # Heartbeat configuration
        heart-beat-enabled: true
        heart-beat-interval: 5000   # ms (default)
        heart-beat-timeout: 15000   # ms (default)
        # Cluster name (for Nacos cluster mode)
        cluster-name: DEFAULT
        # Ephemeral vs persistent instance
        ephemeral: true             # Ephemeral: removed on heartbeat timeout
```

```java
@SpringBootApplication
@EnableDiscoveryClient              // Actually auto-configured, explicit is optional
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}

// Now we can discover services
@Component
public class DiscoveryDemo {

    @Autowired
    private NacosDiscoveryProperties discoveryProperties;

    public void listInstances(String serviceName) throws NacosException {
        NamingService namingService = discoveryProperties.namingServiceInstance();
        List<Instance> instances = namingService.selectInstances(serviceName, true);

        for (Instance inst : instances) {
            System.out.printf("Instance: %s:%d, weight=%.1f, healthy=%s%n",
                inst.getIp(), inst.getPort(), inst.getWeight(), inst.isHealthy());
        }
    }
}
```

### Nacos Health Check Mechanisms

Nacos supports multiple health check types:

```yaml
# On the Nacos server side (configured via UI or API)
# Health check configuration per service

# Type 1: TCP health check
# Nacos server periodically opens a TCP connection to the instance
# Simple: reaches the service's configured port

# Type 2: HTTP health check
# Nacos server calls a specific HTTP endpoint
# e.g., http://10.0.0.1:8081/actuator/health
# Requires a 200 response within timeout

# Type 3: MySQL health check
# For databases — Nacos runs a simple query

# For ephemeral instances: client sends heartbeats, server monitors
# For persistent instances: server actively probes
```

```yaml
# Client-side heartbeat configuration
spring:
  cloud:
    nacos:
      discovery:
        heart-beat-enabled: true
        heart-beat-interval: 5000       # Send heartbeat every 5 seconds
        heart-beat-timeout: 15000       # Mark as unhealthy after 15s no heartbeat
        ip-delete-timeout: 30000        # Remove instance after 30s unhealthy

# Spring Boot Actuator health endpoint (integrated with Nacos)
management:
  endpoint:
    health:
      show-details: always
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

### Nacos CAP Mode Selection

```yaml
# Nacos can operate in CP mode or AP mode
# Mode is CONTROLLED SERVER-SIDE via Nacos configuration

# CP mode (default for persistent services):
# - Uses Raft consensus protocol
# - Strong consistency
# - Leader election during network partition
# - Unavailable during leader election (seconds)

# AP mode (default for ephemeral services):
# - Uses Distro protocol
# - Eventual consistency
# - Always available
# - May return stale instance lists

# Configuration on Nacos server (application.properties):
nacos.naming.distro.protocol=raft  # CP mode (or "distro" for AP)
nacos.naming.ephemeral.default=true  # Default to ephemeral (AP)

# IMPORTANT: For production, consider:
# - Ephemeral (AP): Better for service discovery (availability critical)
# - Persistent (CP): Better for configuration (consistency critical)
```

### Nacos Config Center

```yaml
# bootstrap.yml
spring:
  cloud:
    nacos:
      config:
        server-addr: 127.0.0.1:8848
        namespace: dev_001
        group: DEFAULT_GROUP
        file-extension: yaml
        refresh-enabled: true            # Auto-refresh on config change

        # Shared configs (available to ALL services in the namespace)
        shared-configs:
          - data-id: common.yaml         # Shared across services
            refresh: true
            group: DEFAULT_GROUP

        # Extension configs (per service)
        extension-configs:
          - data-id: datasource.yaml
            refresh: false               # Don't auto-refresh DB connection
            group: DEFAULT_GROUP
          - data-id: redis-config.yaml
            refresh: true
            group: REDIS_GROUP

        # Encrypted config (using jasypt)
        # Password decryption key passed via JVM args or env
        # -Djasypt.encryptor.password=mySecretKey
        config-long-poll-timeout: 30000
        config-retry-time: 2000
        max-retry: 3
```

```yaml
# common.yaml (shared config stored in Nacos)
# Data ID: common.yaml
# Group: DEFAULT_GROUP
server:
  servlet:
    encoding:
      charset: UTF-8

logging:
  level:
    root: INFO
    com.example: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: WHEN_AUTHORIZED
```

```java
// Using config values from Nacos
@RefreshScope
@Service
public class OrderConfigService {

    @Value("${order.timeout:30}")
    private int orderTimeout;

    @Value("${order.payment-grace-period:5000}")
    private long paymentGracePeriod;

    @Value("${order.max-retries:3}")
    private int maxRetries;

    // Config change listener (programmatic)
    @NacosConfigListener(dataId = "order-service.yaml", groupId = "DEFAULT_GROUP")
    public void onConfigChange(String content) {
        // Parse the new YAML/Properties content
        // Apply configuration changes programmatically
        // e.g., reconfigure thread pools, update rate limits
        System.out.println("Configuration changed: " + content);
    }

    @PostConstruct
    public void logConfig() {
        log.info("Order timeout configured: {}s", orderTimeout);
    }
}
```

### Nacos Namespace Isolation Strategy

```
Nacos UI Structure:

Namespaces
├── dev_001 (Development)
│   ├── Groups
│   │   ├── DEFAULT_GROUP
│   │   │   ├── order-service.yaml
│   │   │   ├── common.yaml
│   │   │   ├── datasource.yaml
│   │   │   └── Services: order-service, user-service, ...
│   │   └── PAYMENT_GROUP
│   │       └── payment-service.yaml
│   │
├── staging_001 (Staging)
│   ├── DEFAULT_GROUP
│   │   ├── order-service.yaml
│   │   ├── common.yaml
│   │   └── ...
│   │
├── prod_001 (Production)
│   ├── DEFAULT_GROUP
│   │   ├── order-service.yaml
│   │   ├── common.yaml
│   │   └── ...
│   │
└── public (Default, for testing)
```

### Nacos Best Practices

```yaml
# Production Nacos Cluster configuration
# At least 3 nodes for Raft consensus

# Node 1: 10.0.0.1:8848
# Node 2: 10.0.0.2:8848
# Node 3: 10.0.0.3:8848

# Each node's application.properties:
server.port=8848
spring.datasource.platform=mysql

# Use MySQL for persistent storage (in production)
db.url.0=jdbc:mysql://mysql-host:3306/nacos?characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000
db.user.0=nacos
db.password.0=nacos_pass

# Cluster configuration
nacos.member.list=10.0.0.1:8848,10.0.0.2:8848,10.0.0.3:8848

# Enable authentication (REQUIRED for production)
nacos.core.auth.enabled=true
nacos.core.auth.plugin.nacos.token.secret.key=Base64EncodedRandomKeyAtLeast32BytesLong
nacos.core.auth.server.identity.key=serverIdentity
nacos.core.auth.server.identity.value=security
```

```yaml
# Client-side best practices
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 10.0.0.1:8848,10.0.0.2:8848,10.0.0.3:8848
        namespace: prod_001
        # Register with internal IP, not external
        ip: 172.20.0.1
        # Use ephemeral instances (auto-cleanup on shutdown)
        ephemeral: true
        # Graceful deregistration on shutdown
        deregister-on-shutdown: true
```

---

## Consul Integration

### Spring Boot with Consul

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-consul-discovery</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-consul-config</artifactId>
</dependency>
```

```yaml
# bootstrap.yml
spring:
  cloud:
    consul:
      host: localhost
      port: 8500
      discovery:
        enabled: true
        service-name: order-service
        instance-id: ${spring.application.name}:${spring.cloud.client.hostname}:${random.value[0:5]}
        health-check-path: /actuator/health
        health-check-interval: 10s
        prefer-ip-address: true
        tags:
          - version=1.0.0
          - region=cn-beijing
      config:
        enabled: true
        format: YAML
        data-key: config
        watch:
          enabled: true
          delay: 1000
```

### Consul vs Nacos

| Decision Factor |
|---|
| Consul is stronger for KV operations, DNS discovery, and has a more mature multi-DC story |
| Nacos is simpler (one product does both registry and config), has better Chinese documentation |
| For HashiCorp shops, Consul + Vault + Nomad is a complete stack |

---

## Eureka (Maintenance Mode)

### Current Status

Eureka 2.x was **stopped by Netflix**. Spring Cloud Netflix Eureka is in **maintenance mode** — only bug fixes, no new features.

```
Netflix OSS Component Status (as of 2025):
├── Eureka: Maintenance mode (use Nacos or Consul for new projects)
├── Hystrix: Maintenance mode (use Resilience4j)
├── Ribbon: Maintenance mode (use Spring Cloud LoadBalancer)
├── Zuul 1.x: Maintenance mode (use Spring Cloud Gateway)
└── Archaius: Maintenance mode (use Spring Cloud Config or Nacos Config)
```

### Quick Eureka Setup (For Legacy Systems)

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

```yaml
# application.yml — Eureka Server
server:
  port: 8761

eureka:
  instance:
    hostname: localhost
  client:
    register-with-eureka: false
    fetch-registry: false
    service-url:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
  server:
    enable-self-preservation: true    # AP mode — keep instances during network issues
    renewal-percent-threshold: 0.85   # If < 85% heartbeats received, activate self-preservation
    eviction-interval-timer-in-ms: 5000
```

### Eureka Self-Preservation

```yaml
# When Eureka server detects that heartbeats from clients are below the threshold:
# - Stops evicting instances
# - Continues serving the last-known list of instances
# - Protects against network partition "false positives"
#
# Pros: Maintains availability during network hiccups
# Cons: Routes to dead instances if a service actually went down
#
# Self-preservation is ON by default. Disable ONLY in dev:
eureka:
  server:
    enable-self-preservation: false  # Dev only
```

---

## Configuration Center

### Comparison

| Feature | Nacos Config | Spring Cloud Config | Consul KV |
|---|---|---|---|
| **Storage** | Nacos server (emb. DB/MySQL) | Git repository | Consul KV store |
| **Dynamic refresh** | Native (@RefreshScope) | + Spring Cloud Bus | Native (watch) |
| **Encryption** | jasypt plugin | Symmetric/asymmetric (encrypt.*) | ACL + encryption |
| **Version history** | Via UI | Git history | Via snapshots |
| **Audit** | Built-in | Git log | ACL audit |
| **Secrets mgmt** | Manual encryption | Built-in encrypt/decrypt endpoints | + Vault integration |
| **File types** | Properties, YAML, TXT, JSON, XML | Properties, YAML, JSON | KV (any value) |
| **Profiles** | Group + Data ID | Native Spring profiles | Folder structure |

### Spring Cloud Config Server Setup

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-server</artifactId>
</dependency>
```

```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

```yaml
# application.yml — Config Server
server:
  port: 8888

spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/company/config-repo
          default-label: main
          search-paths: '{application}'   # Subdirectory per service
          clone-on-start: true
          # Authenticated access
          username: ${GIT_USER}
          password: ${GIT_PASSWORD}
          # Force pull (don't use local copy if remote changes)
          force-pull: true

  # Encryption (for encrypting secrets in config files)
encrypt:
  key: ${ENCRYPT_KEY:secret}  # Symmetric key — use a real key in production!
```

```yaml
# Config repo structure:
# config-repo/
# ├── order-service.yml         # Default for order-service
# ├── order-service-dev.yml     # dev profile
# ├── order-service-prod.yml    # prod profile
# ├── user-service.yml
# ├── application.yml           # Shared across all services
# └── application-prod.yml      # Shared, prod profile
```

```yaml
# order-service.yml in Git
server:
  port: 8081
  
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/orders
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:password}

order:
  timeout: 30
  max-retries: 3
  
# Encrypted values (use config server's /encrypt endpoint)
# curl http://localhost:8888/encrypt -d "mySecretPassword"
# → returns encrypted value
app:
  api-key: '{cipher}AQA...encrypted-value...'
```

```yaml
# Config client (order-service)
# bootstrap.yml
spring:
  cloud:
    config:
      uri: http://localhost:8888
      name: order-service
      profile: dev
      label: main
      # Fail fast: if config server is unreachable, don't start
      fail-fast: true
      # Retry when config server is temporarily unavailable
      retry:
        initial-interval: 1000
        multiplier: 1.5
        max-attempts: 5
        max-interval: 5000
```

### Dynamic Refresh with Spring Cloud Bus

```yaml
# For pushing config changes to all instances without rolling restart:

# 1. Add Spring Cloud Bus + message broker
# Add to each service:
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bus-amqp</artifactId>
</dependency>

# 2. When config changes, POST to one service:
# curl -X POST http://order-service:8081/actuator/busrefresh
# → Bus broadcasts refresh event to ALL services via RabbitMQ/Kafka

# 3. Target specific service/instance:
# curl -X POST http://order-service:8081/actuator/busrefresh/order-service:8081
# → Refresh only order-service on port 8081
```

---

## Remote Invocation: OpenFeign

### Overview

OpenFeign is a declarative HTTP client that simplifies service-to-service communication. Instead of manually building HTTP requests, you define a Java interface and annotate it — Feign handles serialization, deserialization, and load balancing.

### Setup

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

```java
@SpringBootApplication
@EnableFeignClients(basePackages = "com.example.order.client")
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

### Feign Client Definitions

```java
// Basic Feign client
@FeignClient(
    name = "user-service",
    url = "${user-service.url:}",  // Direct URL (for testing, overrides discovery)
    path = "/api/users",
    primary = true,                // If multiple beans of same type
    qualifiers = "userClient"      // Qualifier name for injection
)
public interface UserClient {

    @GetMapping("/{userId}")
    User getUser(@PathVariable Long userId);

    @GetMapping
    List<User> getUsers(@RequestParam List<Long> userIds);

    @PostMapping
    User createUser(@RequestBody @Valid CreateUserRequest request);

    @PutMapping("/{userId}")
    User updateUser(@PathVariable Long userId, @RequestBody User user);

    @DeleteMapping("/{userId}")
    void deleteUser(@PathVariable Long userId);
}
```

```java
// Advanced Feign client with custom configuration
@FeignClient(
    name = "inventory-service",
    configuration = InventoryClientConfig.class,
    fallback = InventoryClientFallback.class,
    fallbackFactory = InventoryClientFallbackFactory.class,
    qualifiers = "inventoryClient"
)
public interface InventoryClient {

    @GetMapping("/api/inventory/products/{productId}/stock")
    StockResponse checkStock(@PathVariable Long productId);

    @PostMapping("/api/inventory/products/{productId}/deduct")
    DeductResponse deductStock(
            @PathVariable Long productId,
            @RequestBody DeductRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey);

    @PutMapping("/api/inventory/products/{productId}/restock")
    void restock(@PathVariable Long productId, @RequestParam int quantity);
}
```

### Feign Configuration

```java
@Configuration
public class InventoryClientConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;  // Log request/response headers and body
    }

    @Bean
    public RequestInterceptor inventoryRequestInterceptor() {
        return requestTemplate -> {
            // Add common headers
            requestTemplate.header("X-Source-Service", "order-service");
            requestTemplate.header("Accept", "application/json");
            // Add trace ID if not already present
            String traceId = MDC.get("traceId");
            if (traceId != null) {
                requestTemplate.header("X-Trace-Id", traceId);
            }
        };
    }

    @Bean
    public Retryer feignRetryer() {
        // Custom retry: initial 100ms, max 1s, 3 attempts
        return new Retryer.Default(100, 1000, 3);
    }

    @Bean
    public ErrorDecoder inventoryErrorDecoder() {
        return new InventoryErrorDecoder();
    }

    @Bean
    public Contract feignContract() {
        // Use Spring MVC annotations (default)
        return new SpringMvcContract();
    }
}
```

### Error Decoder

```java
public class InventoryErrorDecoder implements ErrorDecoder {

    private static final Logger log = LoggerFactory.getLogger(InventoryErrorDecoder.class);

    @Override
    public Exception decode(String methodKey, Response response) {
        String responseBody = null;
        try {
            if (response.body() != null) {
                responseBody = Util.toString(response.body().asReader(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            // ignore
        }

        log.error("Inventory service error: method={}, status={}, body={}",
            methodKey, response.status(), responseBody);

        return switch (response.status()) {
            case 400 -> new BadRequestException(responseBody);
            case 404 -> new ProductNotFoundException(extractProductId(responseBody));
            case 409 -> new InsufficientStockException(responseBody);
            case 503 -> new ServiceUnavailableException("Inventory service unavailable");
            default -> new RuntimeException("Inventory service error: " + response.status());
        };
    }

    private Long extractProductId(String responseBody) {
        // Parse JSON to extract productId
        return 0L; // simplified
    }
}
```

### Fallback and FallbackFactory

```java
// Simple fallback (no access to the error cause)
@Component
public class InventoryClientFallback implements InventoryClient {

    @Override
    public StockResponse checkStock(Long productId) {
        log.warn("Fallback: checking stock for product {} (assume unavailable)", productId);
        return new StockResponse(productId, 0, "SERVICE_UNAVAILABLE");
    }

    @Override
    public DeductResponse deductStock(Long productId, DeductRequest request, String idempotencyKey) {
        throw new FallbackNotSupportedException("Deduct cannot use fallback — needs manual handling");
    }

    @Override
    public void restock(Long productId, int quantity) {
        log.warn("Fallback: restock failed for product {}", productId);
    }
}

// FallbackFactory (gives access to the error)
@Component
public class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {

    @Override
    public InventoryClient create(Throwable cause) {
        log.error("Inventory client error", cause);

        return new InventoryClient() {
            @Override
            public StockResponse checkStock(Long productId) {
                if (cause instanceof CircuitBreakerOpenException) {
                    return new StockResponse(productId, 0, "CIRCUIT_OPEN");
                }
                if (cause instanceof RetryableException) {
                    return new StockResponse(productId, 0, "NETWORK_ERROR");
                }
                return new StockResponse(productId, 0, "UNKNOWN_ERROR");
            }

            @Override
            public DeductResponse deductStock(Long productId, DeductRequest request, String idempotencyKey) {
                throw new RuntimeException("Cannot fallback for deduct", cause);
            }

            @Override
            public void restock(Long productId, int quantity) {
                // silent fallback
            }
        };
    }
}
```

### Feign Configuration Properties

```yaml
# application.yml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:                              # Default for ALL feign clients
            connectTimeout: 5000
            readTimeout: 5000
            loggerLevel: BASIC
            retryer:
              period: 100                       # Initial retry interval (ms)
              maxPeriod: 1000                   # Max interval (ms)
              maxAttempts: 3
          inventory-service:                     # Override for specific client
            connectTimeout: 2000
            readTimeout: 10000
            loggerLevel: FULL
      compression:
        request:
          enabled: true
          mime-types: application/json
          min-request-size: 2048
        response:
          enabled: true
      circuitbreaker:
        enabled: true                           # Enable circuit breaker integration
        alphaname-ids: true                     # Use alphanumeric circuit breaker names
```

### RestTemplate vs WebClient vs OpenFeign

| Feature | RestTemplate | WebClient | OpenFeign |
|---|---|---|---|
| **Type** | Blocking | Reactive (non-blocking) | Declarative |
| **Async** | No (CompletableFuture wrapper needed) | Native (Mono/Flux) | Via Spring Async |
| **Load balancing** | @LoadBalanced | @LoadBalanced | Built-in (service name) |
| **Circuit breaker** | Manual integration | Manual integration | Via Spring Cloud Circuit Breaker |
| **Code volume** | Medium (manual request build) | Medium (manual) | Low (interface only) |
| **Testing** | MockRestServiceServer | WireMock | @SpringBootTest + MockBean |
| **Maintenance** | Deprecated (Spring will not add new features) | Active | Active |
| **Best for** | Simple internal calls | High-concurrency, reactive | Most microservice REST calls |

```java
// Recommendation: Use OpenFeign for service-to-service REST calls.
// Use WebClient if your app is reactive (WebFlux).
// Avoid RestTemplate in new code.

// OpenFeign example (PREFERRED)
@FeignClient("user-service")
interface UserClient {
    @GetMapping("/api/users/{id}")
    User getUser(@PathVariable Long id);
}

// WebClient example (if reactive)
@Service
class UserReactiveService {
    private final WebClient webClient;

    public Mono<User> getUser(Long id) {
        return webClient.get()
            .uri("http://user-service/api/users/{id}", id)
            .retrieve()
            .bodyToMono(User.class);
    }
}

// RestTemplate example (legacy — avoid in new code)
@Service
@Deprecated
class UserLegacyService {
    @Autowired
    @LoadBalanced
    private RestTemplate restTemplate;

    public User getUser(Long id) {
        return restTemplate.getForObject(
            "http://user-service/api/users/{id}", User.class, id);
    }
}
```

---

## Load Balancing

### Spring Cloud LoadBalancer (Replaces Ribbon)

Ribbon is in maintenance mode. Spring Cloud LoadBalancer is the replacement.

```yaml
# application.yml
spring:
  cloud:
    loadbalancer:
      enabled: true
      cache:
        enabled: true
        ttl: 10s                    # Refresh instance list every 10s
        capacity: 256               # Max cached entries

      # Strategy options:
      # - round-robin (default)
      # - random
      # - retry (retry on failure)
      # - weighted (based on instance weight from registry)
      # - zone-preferred (prefer same zone)
      client:
        ribbon:
          enabled: false            # DISABLE Ribbon (uses LoadBalancer instead)
```

```java
// Custom load balancer configuration
@Configuration(proxyBeanMethods = false)
public class CustomLoadBalancerConfig {

    @Bean
    public ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {

        String serviceId = environment.getProperty(
            LoadBalancerClientFactory.PROPERTY_NAME);

        return new RandomLoadBalancer(
            loadBalancerClientFactory.getLazyProvider(serviceId,
                ServiceInstanceListSupplier.class),
            serviceId
        );
    }
}

// Apply to specific service only
@LoadBalancerClient(name = "inventory-service",
    configuration = CustomLoadBalancerConfig.class)
public class InventoryServiceLoadBalancerConfig {
    // Uses RandomLoadBalancer for inventory-service, round-robin for others
}
```

### Weighted Load Balancing with Nacos

```yaml
# Nacos supports weighted routing out-of-the-box:
# Instance weights can be set via Nacos UI or API
# Spring Cloud LoadBalancer Nacos adapter respects these weights

# Set instance weight via Nacos API:
# curl -X PUT 'http://localhost:8848/nacos/v1/ns/instance?serviceName=order-service&ip=10.0.0.1&port=8081&weight=2.0'
# curl -X PUT 'http://localhost:8848/nacos/v1/ns/instance?serviceName=order-service&ip=10.0.0.2&port=8081&weight=1.0'
# → Instance 1 gets ~66% traffic, Instance 2 gets ~33%
```

---

## Spring Cloud Gateway

### Gateway vs Zuul

| Aspect | Spring Cloud Gateway | Zuul 1.x | Zuul 2.x |
|---|---|---|---|
| **Reactive** | Yes (WebFlux/Netty) | No (Servlet, blocking) | Yes (Netty) |
| **Throughput** | Very high | Low (thread-per-request) | High |
| **Latency** | Low | Higher (thread overhead) | Low |
| **Ease of setup** | Moderate | Easy | Moderate |
| **Filters** | WebFlux-based (GatewayFilter/GlobalFilter) | Servlet-based | Reactive |
| **WebSocket** | Supported | Not supported | Supported |
| **Long polling** | Supported | Not supported | Supported |
| **Netflix status** | N/A (Spring) | Maintenance mode | Stopped (Netflix) |
| **Recommendation** | **✓ Use for new projects** | Migrate away | Do not use |

### Core Concepts: Route, Predicate, Filter

```
Request → [Predicate matching] → [Filter chain] → [Proxied service]
                │                        │
         Path=/api/orders/**      AddRequestHeader
         Method=POST              CircuitBreaker
         Header=X-Version=2       RateLimiter
```

```yaml
# Conceptual structure
spring:
  cloud:
    gateway:
      routes:
        - id: order-service-route
          uri: lb://order-service        # Load-balanced URI
          predicates:                    # Conditions for this route
            - Path=/api/orders/**
            - Method=GET,POST
          filters:                       # Modifications to request/response
            - StripPrefix=1
            - AddRequestHeader=X-Gateway, true
            - name: CircuitBreaker
              args:
                name: orderServiceCB
```

```java
// Programmatic route definition (alternative to YAML)
@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator customRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("user-service-route", r -> r
                .path("/api/users/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .addRequestHeader("X-Gateway", "true")
                    .retry(3)
                )
                .uri("lb://user-service"))
            .route("order-service-route", r -> r
                .path("/api/orders/**")
                .and().method("GET", "POST")
                .filters(f -> f
                    .stripPrefix(1)
                    .circuitBreaker(cb -> cb
                        .setName("orderServiceCB")
                        .setFallbackUri("forward:/fallback/orders"))
                )
                .uri("lb://order-service"))
            .route("product-service-route", r -> r
                .path("/api/products/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .setResponseHeader("Cache-Control", "public, max-age=3600")
                )
                .uri("lb://product-service"))
            .build();
    }
}
```

### Built-in Predicates

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: path-route
          uri: lb://service
          predicates:
            - Path=/api/orders/**, /api/users/**

        - id: header-route
          uri: lb://service
          predicates:
            - Header=X-Version, v2          # Header X-Version equals v2
            - Header=X-Region, cn-\\w+      # Regex match

        - id: method-route
          uri: lb://service
          predicates:
            - Method=GET,POST,PUT

        - id: query-route
          uri: lb://service
          predicates:
            - Query=page                      # Has query param "page"
            - Query=status, pending|paid      # Query param with regex

        - id: host-route
          uri: lb://service
          predicates:
            - Host=api.example.com, *.example.com

        - id: cookie-route
          uri: lb://service
          predicates:
            - Cookie=sessionId, .+            # Has sessionId cookie

        - id: weight-route-a                   # Canary testing
          uri: lb://service-v2
          predicates:
            - Weight=service-group, 10         # 10% traffic
        - id: weight-route-b
          uri: lb://service-v1
          predicates:
            - Weight=service-group, 90         # 90% traffic

        - id: after-route
          uri: lb://service
          predicates:
            - After=2024-01-01T00:00:00+08:00[Asia/Shanghai]

        - id: before-route
          uri: lb://service
          predicates:
            - Before=2025-12-31T23:59:59+08:00[Asia/Shanghai]

        - id: remote-addr-route
          uri: lb://internal-service
          predicates:
            - RemoteAddr=10.0.0.0/24, 192.168.1.1
```

### Built-in Filters

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: filter-demo
          uri: lb://service
          predicates:
            - Path=/api/**
          filters:
            # --- Request modification ---
            - StripPrefix=1                       # Remove first path segment
            - AddRequestHeader=X-Request-Source, gateway
            - AddRequestParameter=gateway, true
            - SetRequestHeader=X-Auth-Token, token
            - MapRequestHeader=Authorization, X-Original-Auth

            # --- Response modification ---
            - AddResponseHeader=X-Response-Time, current-time
            - SetResponseHeader=X-Powered-By, Spring Cloud Gateway
            - DedupeResponseHeader=Access-Control-Allow-Origin

            # --- Routing ---
            - SetPath=/new-prefix/{segment}       # Rewrite path
            - RewritePath=/api/(?<segment>.*), /$\{segment}

            # --- Resilience ---
            - name: CircuitBreaker
              args:
                name: serviceCB
                fallbackUri: forward:/fallback
                statusCodes:
                  - 500
                  - 503
            - name: Retry
              args:
                retries: 3
                statuses: BAD_GATEWAY, SERVICE_UNAVAILABLE
                methods: GET
                backoff:
                  firstBackoff: 100ms
                  maxBackoff: 1s
                  factor: 2
                  basedOnPreviousValue: false

            # --- Rate limiting ---
            - name: RequestRateLimiter
              args:
                key-resolver: "#{@userKeyResolver}"
                redis-rate-limiter:
                  replenishRate: 100               # Tokens per second
                  burstCapacity: 200               # Max burst tokens
                  requestedTokens: 1               # Tokens consumed per request
```

### Custom Gateway Filter

```java
// Custom GatewayFilter (applied to specific routes)
@Component
public class RequestTimingGatewayFilterFactory
        extends AbstractGatewayFilterFactory<RequestTimingGatewayFilterFactory.Config> {

    public RequestTimingGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            long startTime = System.currentTimeMillis();

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                long duration = System.currentTimeMillis() - startTime;
                // Record timing metrics
                log.info("{} {} completed in {}ms (threshold: {}ms)",
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getURI().getPath(),
                    duration,
                    config.getSlowRequestThresholdMs());

                if (duration > config.getSlowRequestThresholdMs()) {
                    log.warn("SLOW REQUEST: {} {} took {}ms",
                        exchange.getRequest().getMethod(),
                        exchange.getRequest().getURI(),
                        duration);
                }
            }));
        };
    }

    @Data
    public static class Config {
        private int slowRequestThresholdMs = 5000;
    }
}
```

```yaml
# Usage:
# - RequestTiming
#   slowRequestThresholdMs: 3000
```

```java
// Custom GlobalFilter (applies to ALL routes)
@Component
public class RateLimitGlobalFilter implements GlobalFilter, Ordered {

    private final RateLimiter rateLimiter;

    // Using Resilience4j RateLimiter
    public RateLimitGlobalFilter(RateLimiterRegistry registry) {
        this.rateLimiter = registry.rateLimiter("gateway-rate-limiter");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Check rate limit before proceeding
        if (!rateLimiter.acquirePermission()) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().set("Retry-After", "30");
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
```

### Rate Limiting with Redis

```java
// Key resolver — determines how to identify the client for rate limiting
@Bean
public KeyResolver userKeyResolver() {
    return exchange -> {
        // Priority: User ID > API Key > IP address
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (userId != null) {
            return Mono.just(userId);
        }
        String apiKey = exchange.getRequest().getQueryParams().getFirst("api_key");
        if (apiKey != null) {
            return Mono.just(apiKey);
        }
        String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        return Mono.just(ip);
    };
}

// Alternative: Path-based key resolver (rate limit per API endpoint)
@Bean
public KeyResolver pathKeyResolver() {
    return exchange -> Mono.just(
        exchange.getRequest().getURI().getPath()
    );
}
```

```xml
<!-- Dependencies needed for Redis rate limiting -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
```

### Gateway CORS Configuration

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOriginPatterns:
              - "https://*.example.com"        # Pattern-based
              - "http://localhost:3000"         # Dev server
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowedHeaders: "*"
            allowCredentials: true
            maxAge: 3600                       # Preflight cache (seconds)
      # Alternative: default-filters approach
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Origin
```

### Gateway Security Best Practices

```java
// Security global filter
@Component
public class SecurityGatewayFilter implements GlobalFilter, Ordered {

    private final List<String> publicPaths = List.of(
        "/api/users/login",
        "/api/users/register",
        "/api/products",
        "/actuator/health",
        "/fallback/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Skip public paths
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // Validate JWT
        String token = extractToken(exchange.getRequest());
        if (token == null || !validateToken(token)) {
            return unauthorized(exchange, "Missing or invalid token");
        }

        // Extract user info and add headers
        JwtClaims claims = parseToken(token);
        exchange.getRequest().mutate()
            .header("X-User-Id", claims.getUserId())
            .header("X-User-Role", claims.getRole());

        return chain.filter(exchange);
    }

    private boolean isPublicPath(String path) {
        return publicPaths.stream().anyMatch(path::startsWith)
            || path.equals("/") || path.startsWith("/webjars/")
            || path.startsWith("/v3/api-docs");
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"error\":\"" + message + "\"}").getBytes();
        return exchange.getResponse()
            .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    private String extractToken(ServerHttpRequest request) {
        String bearer = request.getHeaders().getFirst("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    @Override
    public int getOrder() {
        return -200; // Highest priority for security
    }
}
```

---

## Resilience4j

### Overview

Resilience4j is a lightweight, easy-to-use fault tolerance library inspired by Netflix Hystrix but designed for **Java 8+ functional programming**. It replaces Hystrix (which is in maintenance mode).

### Modules

```xml
<!-- Core resilience modules -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

```yaml
# Complete Resilience4j Configuration
resilience4j:
  # ─── CircuitBreaker ───
  circuitbreaker:
    configs:
      default:
        failure-rate-threshold: 50           # Open at 50% failure rate
        slow-call-rate-threshold: 50         # Open at 50% slow calls
        slow-call-duration-threshold: 5s     # "Slow" = > 5 seconds
        permitted-number-of-calls-in-half-open-state: 3
        minimum-number-of-calls: 10          # Minimum before calculating rate
        sliding-window-type: COUNT_BASED
        sliding-window-size: 20              # Look at last 20 calls
        wait-duration-in-open-state: 30s     # Time before half-open
        automatic-transition-from-open-to-half-open-enabled: true
        record-exceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
          - feign.FeignException.ServiceUnavailable
          - feign.FeignException.GatewayTimeout
        ignore-exceptions:
          - com.example.BusinessException    # Don't count as failures
    instances:
      user-service:
        base-config: default
        failure-rate-threshold: 30           # More sensitive
        wait-duration-in-open-state: 60s
      payment-service:
        base-config: default
        sliding-window-type: TIME_BASED
        sliding-window-size: 60              # Look at last 60 seconds
        minimum-number-of-calls: 5
        permitted-number-of-calls-in-half-open-state: 1

  # ─── Retry ───
  retry:
    configs:
      default:
        max-attempts: 3
        wait-duration: 500ms
        exponential-backoff-multiplier: 2    # 500ms, 1s, 2s
        retry-exceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
    instances:
      inventory-retry:
        base-config: default
        max-attempts: 5

  # ─── RateLimiter ───
  ratelimiter:
    configs:
      default:
        timeout-duration: 5s                 # Wait for a permit
        limit-for-period: 100                # Max requests per period
        limit-refresh-period: 1s             # Refresh period
    instances:
      payment-rate-limiter:
        limit-for-period: 20
        timeout-duration: 2s

  # ─── Bulkhead (Semaphore) ───
  bulkhead:
    configs:
      default:
        max-concurrent-calls: 10
        max-wait-duration: 500ms
    instances:
      user-bulkhead:
        max-concurrent-calls: 5

  # ─── Bulkhead (ThreadPool) ───
  thread-pool-bulkhead:
    configs:
      default:
        max-thread-pool-size: 10
        core-thread-pool-size: 5
        queue-capacity: 20
    instances:
      payment-thread-pool:
        max-thread-pool-size: 2
        queue-capacity: 5

  # ─── TimeLimiter ───
  timelimiter:
    configs:
      default:
        timeout-duration: 5s
        cancel-running-future: true
    instances:
      slow-service:
        timeout-duration: 3s
```

### Annotated Usage

```java
@Service
public class ResilientOrderService {

    private static final Logger log = LoggerFactory.getLogger(ResilientOrderService.class);

    // --- CircuitBreaker + Fallback ---
    @CircuitBreaker(name = "user-service", fallbackMethod = "userFallback")
    public User getUser(Long userId) {
        return userClient.getUser(userId);
    }

    public User userFallback(Long userId, Throwable t) {
        log.warn("User service unavailable: {}", t.getMessage());
        return new User(userId, "Guest", "guest@example.com");
    }

    // --- Retry ---
    @Retry(name = "inventory-retry", fallbackMethod = "inventoryFallback")
    public StockResponse checkStock(Long productId) {
        return inventoryClient.checkStock(productId);
    }

    public StockResponse inventoryFallback(Long productId, Throwable t) {
        log.error("Inventory check failed after retries for product {}", productId, t);
        return new StockResponse(productId, 0, "UNAVAILABLE");
    }

    // --- RateLimiter ---
    @RateLimiter(name = "payment-rate-limiter")
    public PaymentResult processPayment(PaymentRequest request) {
        return paymentClient.charge(request);
    }

    // --- Bulkhead ---
    @Bulkhead(name = "user-bulkhead", type = Bulkhead.Type.SEMAPHORE)
    public List<User> getUsers(List<Long> userIds) {
        return userClient.getUsers(userIds);
    }

    // --- TimeLimiter (requires CompletableFuture) ---
    @TimeLimiter(name = "slow-service")
    public CompletableFuture<Report> generateReport(Long orderId) {
        return CompletableFuture.supplyAsync(() -> reportClient.generate(orderId));
    }

    // --- Multiple annotations ---
    @CircuitBreaker(name = "payment-service", fallbackMethod = "paymentFallback")
    @Retry(name = "payment-retry")
    @RateLimiter(name = "payment-rate-limiter")
    @Bulkhead(name = "payment-bulkhead")
    public PaymentResult chargeWithFullProtection(PaymentRequest request) {
        // NOTE: Order matters — outermost annotation wraps innermost
        // Bulkhead → RateLimiter → Retry → CircuitBreaker
        return paymentClient.charge(request);
    }

    public PaymentResult paymentFallback(PaymentRequest request, Throwable t) {
        log.error("Payment failed after all protections", t);
        return new PaymentResult(false, "Payment service unavailable");
    }
}
```

### Events and Monitoring

```java
@Component
public class ResilienceEventLogger {

    private static final Logger log = LoggerFactory.getLogger(ResilienceEventLogger.class);

    public ResilienceEventLogger(
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            RateLimiterRegistry rateLimiterRegistry,
            BulkheadRegistry bulkheadRegistry) {

        // CircuitBreaker events
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            cb.getEventPublisher()
                .onStateTransition(e -> log.warn("CB [{}] STATE: {} → {}",
                    e.getCircuitBreakerName(),
                    e.getStateTransition().getFromState(),
                    e.getStateTransition().getToState()))
                .onCallNotPermitted(e -> log.warn("CB [{}] CALL REJECTED (OPEN)",
                    e.getCircuitBreakerName()))
                .onError(e -> log.error("CB [{}] ERROR: {}",
                    e.getCircuitBreakerName(), e.getThrowable().getMessage()));
        });

        // Retry events
        retryRegistry.getAllRetries().forEach(retry -> {
            retry.getEventPublisher()
                .onRetry(e -> log.warn("RETRY [{}] Attempt {} failed, retrying...",
                    e.getName(), e.getNumberOfRetryAttempts()))
                .onError(e -> log.error("RETRY [{}] Exhausted: {}",
                    e.getName(), e.getThrowable().getMessage()));
        });

        // RateLimiter events
        rateLimiterRegistry.getAllRateLimiters().forEach(rl -> {
            rl.getEventPublisher()
                .onFailure(e -> log.warn("RATELIMIT [{}] Exceeded limit",
                    e.getRateLimiterName()));
        });
    }
}
```

### Circuit Breaker State Machine Details

```
CLOSED:
  - All requests pass through
  - Success/Failure counted in sliding window
  - When failure_rate ≥ threshold → OPEN

OPEN:
  - Requests are REJECTED immediately (fail-fast)
  - Wait duration timer starts (e.g., 30 seconds)
  - After timer expires → HALF_OPEN

HALF_OPEN:
  - Allow limited requests (e.g., 3)
  - If all succeed → CLOSED
  - If any fails → OPEN again
```

```java
// Programmatic circuit breaker for fine-grained control
@Service
public class ProgrammaticCircuitBreakerService {

    private final CircuitBreaker circuitBreaker;
    private final PaymentClient paymentClient;

    public ProgrammaticCircuitBreakerService(CircuitBreakerRegistry registry) {
        this.circuitBreaker = registry.circuitBreaker("payment-service");
        this.paymentClient = ...;
    }

    public PaymentResult processPayment(PaymentRequest request) {
        // Decorate the function with circuit breaker
        Supplier<PaymentResult> decoratedSupplier = CircuitBreaker
            .decorateSupplier(circuitBreaker, () -> paymentClient.charge(request));

        // Execute with fallback
        return Try.ofSupplier(decoratedSupplier)
            .recover(throwable -> {
                log.error("Payment failed", throwable);
                return new PaymentResult(false, "Fallback payment result");
            })
            .get();
    }

    // Check state programmatically
    public CircuitBreaker.State getState() {
        return circuitBreaker.getState();
    }

    public CircuitBreaker.Metrics getMetrics() {
        return circuitBreaker.getMetrics();
    }
}
```

---

## Sentinel

### Overview

Sentinel is Alibaba's flow control component for distributed systems. It provides **flow control, circuit breaking, and system adaptive protection**.

### Sentinel vs Resilience4j

| Feature | Sentinel | Resilience4j |
|---|---|---|
| **Flow control** | Advanced (QPS, thread count, warm-up,排队) | Basic (RateLimiter only) |
| **Circuit breaking** | By response time, error ratio, error count | By failure rate, slow call rate |
| **System protection** | Load, CPU, RT, thread count | Not built-in |
| **Hot spot control** | Parameter-level flow control | Not built-in |
| **Dashboard** | Rich dashboard with real-time monitoring | Micrometer + external dashboards |
| **Rule persistence** | Dynamic (via dashboard + config) | Static (YAML) + dynamic via API |
| **Adaptive protection** | Yes | No |
| **Spring integration** | Spring Cloud Alibaba | Spring Cloud Circuit Breaker |
| **Ecosystem** | Alibaba ecosystem | Standard Spring Cloud |

### Sentinel Setup

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8080     # Sentinel Dashboard address
        port: 8719                     # Client communication port
      eager: true                      # Connect to dashboard on startup
      filter:
        enabled: true
        order: -2147483648
      datasource:
        # Nacos as rule persistence source
        ds1:
          nacos:
            server-addr: localhost:8848
            dataId: ${spring.application.name}-sentinel-rules
            groupId: SENTINEL_GROUP
            rule-type: flow            # flow, degrade, system, authority, param-flow
```

```bash
# Run Sentinel Dashboard
docker run -d -p 8080:8080 --name sentinel-dashboard bladex/sentinel-dashboard:1.8.7
# Access: http://localhost:8080  (credentials: sentinel/sentinel)
```

### Sentinel Flow Control Rules

```java
@Configuration
public class SentinelFlowConfig {

    @PostConstruct
    public void initRules() {
        // Define flow rules programmatically
        List<FlowRule> rules = new ArrayList<>();

        // Rule 1: QPS-based flow control
        FlowRule orderCreateRule = new FlowRule();
        orderCreateRule.setResource("createOrder");           // Resource name
        orderCreateRule.setGrade(RuleConstant.FLOW_GRADE_QPS); // QPS mode
        orderCreateRule.setCount(100);                         // Limit: 100 QPS
        orderCreateRule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
        rules.add(orderCreateRule);

        // Rule 2: Warm-up (slow start for burst traffic)
        FlowRule warmupRule = new FlowRule();
        warmupRule.setResource("userLogin");
        warmupRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        warmupRule.setCount(1000);          // Target QPS
        warmupRule.setWarmUpPeriodSec(10);   // Warm up over 10 seconds
        warmupRule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_WARM_UP);
        rules.add(warmupRule);

        // Rule 3: Queue mode (匀速排队)
        FlowRule queueRule = new FlowRule();
        queueRule.setResource("paymentQueue");
        queueRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        queueRule.setCount(10);              // 10 QPS
        queueRule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER);
        queueRule.setMaxQueueingTimeMs(500); // Max wait in queue: 500ms
        rules.add(queueRule);

        FlowRuleManager.loadRules(rules);
    }
}
```

### Sentinel Annotation Usage

```java
@Service
public class SentinelOrderService {

    // Basic flow control
    @SentinelResource(
        value = "createOrder",
        blockHandler = "createOrderBlockHandler",
        fallback = "createOrderFallback"
    )
    public Order createOrder(OrderRequest request) {
        // Business logic
        if (request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Items must not be empty");
        }
        return orderRepository.save(Order.from(request));
    }

    // Handler: called when flow control triggers
    public Order createOrderBlockHandler(OrderRequest request, BlockException e) {
        // Blocked by Sentinel (too many requests)
        log.warn("Create order rate limited: {}", e.getMessage());
        throw new RateLimitException("Too many requests, try later");
    }

    // Fallback: called when business logic throws exception
    public Order createOrderFallback(OrderRequest request, Throwable t) {
        log.error("Create order failed: {}", t.getMessage());
        throw new OrderCreationException("Order creation failed: " + t.getMessage());
    }

    // Degradation (circuit breaking) by response time
    @SentinelResource(
        value = "getUser",
        fallback = "getUserFallback"
    )
    public User getUser(Long userId) {
        return userClient.getUser(userId);
    }
}
```

```yaml
# Degrade rules (configured via dashboard or programmatically)
# sentinel-degrade-rules.yaml
rules:
  - resource: getUser
    grade: 0                          # RT (response time) — degrade when avg RT > threshold
    count: 2000                        # Average RT threshold: 2000ms
    timeWindow: 30                     # Time window for circuit breaker recovery (seconds)
    minRequestAmount: 5                # Minimum requests to trigger degradation
    statIntervalMs: 1000               # Statistics interval
  - resource: createOrder
    grade: 1                          # Exception ratio
    count: 0.2                         # Degrade when 20% of requests throw exceptions
    timeWindow: 60
    minRequestAmount: 10
```

### Sentinel System Protection

```java
// System protection — adaptive protection based on system load
@PostConstruct
public void initSystemRules() {
    List<SystemRule> rules = new ArrayList<>();
    
    SystemRule rule = new SystemRule();
    rule.setHighestSystemLoad(10.0);           // Max system load (1-minute average)
    rule.setAvgRt(2000);                       // Max average response time
    rule.setMaxThread(500);                    // Max concurrent threads
    rule.setQps(10000);                        // Max QPS
    rule.setHighestCpuUsage(0.8);              // Max CPU usage (80%)
    rules.add(rule);

    SystemRuleManager.loadRules(rules);
}
```

---

## Distributed Tracing

### Evolution

```
Spring Cloud Sleuth (deprecated)
         ↓
    Micrometer Tracing (new, Spring Boot 3.x+)
         ↓
    Brave (OpenZipkin tracer implementation)
         ↓
    Zipkin / Jaeger (visualization)
```

### Core Concepts

```
Trace: a3b2c1d4
├── Span "GET /api/orders" (parent: null, duration: 1200ms)
│   ├── Span "UserService.getUser" (parent: a3b2c1d4, duration: 200ms)
│   ├── Span "InventoryService.checkStock" (parent: a3b2c1d4, duration: 300ms)
│   └── Span "PaymentService.charge" (parent: a3b2c1d4, duration: 400ms)
│
└── Each span has:
    - traceId (shared across all spans in the trace)
    - spanId (unique per span)
    - parentSpanId (links to parent span)
    - serviceName, operationName, duration
    - tags (key-value metadata)
    - annotations (timestamps for specific events)
```

### Trace Context Propagation

Trace context is propagated via HTTP headers:

```
Standard (B3 propagation):
  X-B3-TraceId: a3b2c1d4e5f6a7b8
  X-B3-SpanId: 9f8e7d6c5b4a3f2e
  X-B3-ParentSpanId: 1a2b3c4d5e6f7a8b
  X-B3-Sampled: 1

Alternative (W3C Trace Context — recommended for new projects):
  traceparent: 00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203011-01
  tracestate: congo@baz=7,rojo=00f067aa0ba912b7

Jaeger:
  uber-trace-id: 3a2b1c4d5e6f7a8b:9f8e7d6c5b4a3f2e:1a2b3c4d5e6f7a8b:1
```

### Implementation with Micrometer Tracing

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

```yaml
# application.yml
management:
  tracing:
    enabled: true
    sampling:
      probability: 0.1            # Sample 10% of requests (head-based)
      # rate: 10                   # Alternative: fixed rate per second
    propagation:
      type: W3C                   # or B3 (default)
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
      # connect-timeout: 5s
      # read-timeout: 10s
  # Tags to include automatically
  info:
    build:
      enabled: true
  endpoint:
    info:
      enabled: true
```

```java
// Custom span creation
@Service
public class TracedPaymentService {

    private final Tracer tracer;
    private final PaymentClient paymentClient;

    public TracedPaymentService(Tracer tracer, PaymentClient paymentClient) {
        this.tracer = tracer;
        this.paymentClient = paymentClient;
    }

    public PaymentResult processPayment(PaymentRequest request) {
        // Create a custom span
        ScopedSpan span = tracer.startScopedSpan("processPayment");
        try {
            // Add tags
            span.tag("payment.amount", request.getAmount().toString());
            span.tag("payment.currency", request.getCurrency());
            span.tag("user.id", String.valueOf(request.getUserId()));

            // Business logic
            PaymentResult result = paymentClient.charge(request);

            span.tag("payment.result", result.isSuccess() ? "success" : "failed");
            return result;

        } catch (Exception e) {
            span.error(e);
            throw e;
        } finally {
            span.end(); // Always close the span
        }
    }
}

// Creating a child span in a different thread
public void asyncProcess(Long orderId) {
    // Create a new span with parent context from the current trace
    Tracer.SpanInScope spanInScope = null;
    Span span = null;
    try {
        span = tracer.nextSpan().name("asyncProcessing").start();
        span.tag("order.id", String.valueOf(orderId));
        spanInScope = tracer.withSpan(span);

        // Async work...
        CompletableFuture.runAsync(() -> {
            // The span context is propagated via MDC
            processAsync(orderId);
        });
    } finally {
        if (spanInScope != null) spanInScope.close();
        if (span != null) span.end();
    }
}
```

### Sampling Strategies

```yaml
# Head-based sampling (decision made at the edge)
management:
  tracing:
    sampling:
      probability: 0.1    # 10% sample rate

# Rate-limiting sampler (limit traces per second)
@Bean
public Sampler customSampler() {
    return Sampler.create(100);  // Max 100 traces per second
}

# Custom sampler
@Bean
public Sampler customSampler() {
    return request -> {
        // Sample all errors
        if (request.getSpanContext().isSampled()) {
            return true;
        }
        // Sample based on request path
        String path = request.getSpanContext().getTraceId();
        // Custom logic
        return Math.random() < 0.1;
    };
}
```

### Zipkin Integration

```bash
# Run Zipkin
docker run -d -p 9411:9411 --name zipkin openzipkin/zipkin:3.1.0
```

```yaml
# Zipkin UI: http://localhost:9411
# Features:
# - Search traces by service, tags, duration
# - Timeline view of spans
# - Dependency graph (service map)
# - Compare traces

# For production, store Zipkin data in Elasticsearch or Cassandra:
spring:
  zipkin:
    storage:
      type: elasticsearch
      elasticsearch:
        hosts: http://elasticsearch:9200
```

### Jaeger Integration

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

```yaml
# Jaeger via OTLP endpoint
management:
  tracing:
    enabled: true
    sampling:
      probability: 0.1
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces
```

---

## Seata: Distributed Transactions

### Overview

Seata (Simple Extensible Autonomous Transaction Architecture) is Alibaba's open-source distributed transaction solution. It supports AT, TCC, Saga, and XA modes.

### Architecture

```
                        ┌──────────────────┐
                        │  Seata TC        │
                        │  (Transaction    │
                        │  Coordinator)    │
                        │  - Global tx     │
                        │  - Branch tx     │
                        │  - Lock mgmt     │
                        └────────┬─────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
        ┌─────▼─────┐     ┌─────▼─────┐     ┌─────▼─────┐
        │    TM     │     │    RM1    │     │    RM2    │
        │(Order     │     │ (Order    │     │(Account   │
        │ Service)  │     │  DB)      │     │  DB)      │
        └───────────┘     └───────────┘     └───────────┘
```

### Seata Setup

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
</dependency>
```

```yaml
# application.yml
seata:
  enabled: true
  application-id: order-service
  tx-service-group: default_tx_group
  service:
    vgroup-mapping:
      default_tx_group: default     # Map to Seata cluster
    grouplist:
      default: 127.0.0.1:8091       # Seata TC address
    disable-global-transaction: false
  config:
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
      group: SEATA_GROUP
      namespace: ""
      data-id: seataServer.properties
  registry:
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
      group: SEATA_GROUP
      namespace: ""
      cluster: default
```

```bash
# Run Seata Server
docker run -d --name seata-server \
  -p 8091:8091 \
  -p 7091:7091 \
  -e SEATA_IP=192.168.1.100 \
  -e SEATA_PORT=8091 \
  -e STORE_MODE=db \
  -e SEATA_CONFIG_NAME=file:/root/seata-config/registry \
  -v /path/to/seata-config:/root/seata-config \
  seataio/seata-server:2.0.0
```

### Seata AT Mode (Automatic)

```java
@Service
public class OrderSeataService {

    @GlobalTransactional(name = "create-order-tx", rollbackFor = Exception.class)
    public Order createOrder(OrderRequest request) {
        // 1. Create order (order DB)
        Order order = orderRepository.save(Order.from(request));

        // 2. Deduct balance (account DB — separate datasource)
        accountFeignClient.deduct(request.getUserId(), order.getTotal());

        // 3. Deduct inventory (inventory DB — separate datasource)
        inventoryFeignClient.deduct(request.getProductId(), request.getQuantity());

        // If step 3 fails, Seata automatically undoes steps 1 and 2
        return order;
    }
}
```

**How AT mode works:**
1. **Before** executing SQL, Seata records the "before image" of the data
2. **After** executing SQL, Seata records the "after image"
3. Both images are stored in `undo_log` table (same database as business data)
4. On rollback, Seata executes **compensating SQL** using before/after images

```sql
-- Seata undo_log table (auto-created in each business database)
CREATE TABLE `undo_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `branch_id` BIGINT NOT NULL,
    `xid` VARCHAR(128) NOT NULL,
    `context` VARCHAR(128) NOT NULL,
    `rollback_info` LONGBLOB NOT NULL,
    `log_status` INT NOT NULL,
    `log_created` DATETIME NOT NULL,
    `log_modified` DATETIME NOT NULL,
    `ext` VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8;
```

### Seata TCC Mode

```java
// TCC phase 1: Try
@LocalTCC
public interface AccountTccService {

    @TwoPhaseBusinessAction(
        name = "accountTcc",
        commitMethod = "confirm",
        rollbackMethod = "cancel",
        useTCCFence = true
    )
    boolean deduct(@BusinessActionContextParameter(paramName = "userId") Long userId,
                   @BusinessActionContextParameter(paramName = "amount") BigDecimal amount);

    // TCC phase 2: Confirm
    boolean confirm(BusinessActionContext context);

    // TCC phase 2: Cancel
    boolean cancel(BusinessActionContext context);
}

@Service
public class AccountTccServiceImpl implements AccountTccService {

    @Autowired
    private AccountMapper accountMapper;

    @Override
    @Transactional
    public boolean deduct(Long userId, BigDecimal amount) {
        // Try: FREEZE the balance (don't deduct yet)
        accountMapper.freezeBalance(userId, amount);
        return true;
    }

    @Override
    @Transactional
    public boolean confirm(BusinessActionContext context) {
        Long userId = Long.parseLong(context.getActionContext("userId").toString());
        BigDecimal amount = new BigDecimal(context.getActionContext("amount").toString());
        // Confirm: Deduct the frozen balance
        accountMapper.confirmDeduct(userId, amount);
        return true;
    }

    @Override
    @Transactional
    public boolean cancel(BusinessActionContext context) {
        Long userId = Long.parseLong(context.getActionContext("userId").toString());
        BigDecimal amount = new BigDecimal(context.getActionContext("amount").toString());
        // Cancel: Unfreeze the balance
        accountMapper.cancelDeduct(userId, amount);
        return true;
    }
}
```

### Seata Saga Mode

```java
// Seata Saga is state-machine based
// Define the state machine in JSON:

/*
{
    "name": "createOrderSaga",
    "startState": "CreateOrder",
    "states": {
        "CreateOrder": {
            "type": "ServiceTask",
            "serviceName": "orderService",
            "serviceMethod": "create",
            "compensateState": "CancelOrder",
            "next": "DeductBalance"
        },
        "DeductBalance": {
            "type": "ServiceTask",
            "serviceName": "accountService",
            "serviceMethod": "deduct",
            "compensateState": "RefundBalance",
            "next": "DeductInventory"
        },
        "DeductInventory": {
            "type": "ServiceTask",
            "serviceName": "inventoryService",
            "serviceMethod": "deduct",
            "compensateState": "RestoreInventory",
            "next": "CompleteOrder"
        },
        "CancelOrder": {
            "type": "ServiceTask",
            "serviceName": "orderService",
            "serviceMethod": "cancel"
        },
        "RefundBalance": {
            "type": "ServiceTask",
            "serviceName": "accountService",
            "serviceMethod": "refund"
        },
        "RestoreInventory": {
            "type": "ServiceTask",
            "serviceName": "inventoryService",
            "serviceMethod": "restore"
        },
        "CompleteOrder": {
            "type": "ServiceTask",
            "serviceName": "orderService",
            "serviceMethod": "complete"
        }
    }
}
*/
```

### Distributed Transaction Selection Guide

| Mode | Consistency | Performance | Business Impact | Use When |
|---|---|---|---|---|
| **AT** | Strong | Medium | Low (non-intrusive) | Want ACID-like without changing business code |
| **TCC** | Strong | Medium | High (3-phase impl) | Need to reserve resources (balance deduction, coupon) |
| **Saga** | Eventual | High | Medium (state machine) | Long-running workflows, multi-step processes |
| **XA** | Strongest | Low | Low (declarative) | Legacy systems, strict compliance |

---

## Interview Questions

### Basic

1. **"What is the difference between Nacos, Eureka, and Consul?"**
   - Nacos: CP+AP switchable, built-in config center, most popular in China
   - Eureka: AP only, self-preservation, maintenance mode
   - Consul: CP, strong KV store, multi-DC, HashiCorp ecosystem

2. **"How does Spring Cloud Gateway differ from Zuul?"**
   - Gateway: Reactive (WebFlux/Netty), non-blocking, higher throughput
   - Zuul 1.x: Servlet-based, blocking, thread-per-request
   - Gateway has better WebSocket support, native rate limiting with Redis

3. **"What annotations does OpenFeign support?"**
   - `@FeignClient`: Declares the Feign client
   - `@GetMapping/@PostMapping/@PutMapping/@DeleteMapping`: HTTP method mapping
   - `@PathVariable`, `@RequestParam`, `@RequestBody`: Parameter binding
   - `@SpringQueryMap`: Maps POJO to query parameters

### Intermediate

4. **"Explain the Circuit Breaker states in Resilience4j."**
   - CLOSED: Normal operation, failures counted in sliding window
   - OPEN: Failures exceed threshold, requests rejected immediately
   - HALF_OPEN: After timeout, trial requests allowed; success→CLOSED, failure→OPEN

5. **"How does Sentinel differ from Hystrix?"**
   - Sentinel supports flow control (QPS, thread count), system load protection, hotspot control
   - Hystrix is circuit-breaker only
   - Sentinel has a rich real-time dashboard
   - Sentinel supports dynamic rule configuration

6. **"How does trace context propagate across services?"**
   - Via HTTP headers (B3: X-B3-TraceId, X-B3-SpanId or W3C: traceparent)
   - Micrometer Tracing auto-injects/reads these headers via filters/interceptors
   - MDC (Mapped Diagnostic Context) stores traceId for logging

### Advanced

7. **"How does Nacos AP mode differ from CP mode?"**
   - AP mode (Distro): Ephemeral instances, eventual consistency, always available
   - CP mode (Raft): Persistent instances, strong consistency, unavailable during leader election
   - Switchable via configuration per service

8. **"Design a rate-limiting strategy for a multi-tenant API Gateway."**
   - KeyResolver per tenant (header, API key, JWT claim)
   - Global limit (all tenants), per-tenant limit, per-endpoint limit
   - Redis-based token bucket (replenishRate, burstCapacity)
   - Response headers: X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset

9. **"How would you implement graceful degradation when a downstream service is unavailable?"**
   - Circuit breaker → fail fast
   - Fallback → return cached data, default values, or partial response
   - Bulkhead → isolate resource pools
   - Timeouts → bound wait time
   - Stale cache → serve stale but functional data

10. **"Compare AT mode and TCC mode in Seata."**
    - AT: Automatic, non-intrusive, uses before/after images, automatic rollback
    - TCC: Manual, requires Try/Confirm/Cancel implementation, more control
    - AT is simpler but requires undo capability (can't undo some operations like sending email)
    - TCC is more complex but handles all scenarios (including non-database operations)

---

## Summary

| Component | Primary Use | Key Concept |
|---|---|---|
| **Nacos** | Service registry + config center | Namespace, Group, Data ID, CP/AP switchable |
| **Gateway** | API gateway, routing, cross-cutting | Route, Predicate, Filter, WebFlux |
| **OpenFeign** | Declarative HTTP client | `@FeignClient`, `@EnableFeignClients` |
| **LoadBalancer** | Client-side load balancing | Replaces Ribbon, RoundRobin/Random |
| **Resilience4j** | Fault tolerance | CircuitBreaker, RateLimiter, Retry, Bulkhead |
| **Sentinel** | Flow control + circuit breaking | QPS limit, degradation, system protection |
| **Micrometer Tracing** | Distributed tracing | TraceId, SpanId, Zipkin/Jaeger |
| **Seata** | Distributed transactions | AT, TCC, Saga, XA modes |
