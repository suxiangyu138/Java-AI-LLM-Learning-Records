# 05 — Docker 容器化 (Docker Containerization)

> **Core Reading**: This document covers Docker containerization for Java microservices, from fundamentals through production-ready Dockerfiles, Docker Compose orchestration, and Kubernetes introduction.

---

## Table of Contents

1. [Container vs VM](#container-vs-vm)
2. [Docker Core Concepts](#docker-core-concepts)
3. [Dockerfile Best Practices](#dockerfile-best-practices)
4. [Production-Grade Spring Boot Dockerfile](#production-grade-spring-boot-dockerfile)
5. [Docker Commands Cheat Sheet](#docker-commands-cheat-sheet)
6. [Docker Compose](#docker-compose)
7. [Container Networking](#container-networking)
8. [Data Management](#data-management)
9. [Container Logging](#container-logging)
10. [Docker Registry](#docker-registry)
11. [CI/CD with Docker](#cicd-with-docker)
12. [Kubernetes Introduction](#kubernetes-introduction)
13. [Docker for Java Developers](#docker-for-java-developers)
14. [Interview Questions](#interview-questions)

---

## Container vs VM

### Architecture Comparison

```
┌───────────────────────────────┐  ┌───────────────────────────────┐
│         Virtual Machines       │  │          Containers           │
│                               │  │                               │
│  ┌─────┐ ┌─────┐ ┌─────┐    │  │  ┌─────┐ ┌─────┐ ┌─────┐    │
│  │ App1│ │ App2│ │ App3│    │  │  │ App1│ │ App2│ │ App3│    │
│  ├─────┤ ├─────┤ ├─────┤    │  │  ├─────┤ ├─────┤ ├─────┤    │
│  │ Libs│ │ Libs│ │ Libs│    │  │  │ Libs│ │ Libs│ │ Libs│    │
│  ├─────┤ ├─────┤ ├─────┤    │  │  ├─────┤ ├─────┤ ├─────┤    │
│  │Guest│ │Guest│ │Guest│    │  │  │     │     │     │        │
│  │ OS  │ │ OS  │ │ OS  │    │  │  └─────┘ └─────┘ └─────┘    │
│  ├─────┤ ├─────┤ ├─────┤    │  │                               │
│  │Hypervisor (Type1/2)      │  │         Docker Engine         │
│  ├──────────────────────────┤  │  ├──────────────────────────┤ │
│  │       Host OS            │  │  │        Host OS            │ │
│  ├──────────────────────────┤  │  ├──────────────────────────┤ │
│  │       Hardware            │  │  │        Hardware           │ │
│  └───────────────────────────┘  │  └───────────────────────────┘
```

### Key Differences

| Dimension | Virtual Machine | Container |
|---|---|---|
| **OS** | Each VM has full guest OS | Shares host OS kernel |
| **Boot time** | Minutes (OS boot) | Seconds (process start) |
| **Size** | GBs (OS + app + dependencies) | MBs (app + dependencies) |
| **Isolation** | Strong (hypervisor-level) | Moderate (kernel namespaces) |
| **Performance** | Near-native (2-5% overhead) | Native (minimal overhead) |
| **Resource usage** | High (duplicate OS per VM) | Low (shared kernel) |
| **Portability** | Slower (large images) | Faster (small images) |
| **Use case** | Full OS isolation, security | Microservices, stateless apps |

**When to choose VMs:**
- Need to run different operating systems (Linux + Windows)
- Strong security isolation required (multi-tenant, compliance)
- Legacy apps that need full OS environment

**When to choose Containers:**
- Microservices deployment
- CI/CD pipelines
- Stateless applications
- Rapid scaling and orchestration
- Development environment consistency

---

## Docker Core Concepts

### Architecture

```
┌──────────────────────────────────────────────────────────┐
│                      Docker Host                          │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │                Docker Engine                       │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐        │  │
│  │  │  Containers │  │  Images  │  │ Volumes  │        │  │
│  │  └──────────┘  └──────────┘  └──────────┘        │  │
│  └────────────────────────────────────────────────────┘  │
│                                                          │
│  ┌──────────────────┐  ┌────────────────────────┐      │
│  │   Docker CLI     │  │   Docker Daemon (dockerd)│      │
│  │   (docker cmd)   │──│                         │      │
│  └──────────────────┘  │  - Build, run, manage   │      │
│                        │  - REST API             │      │
│                        └────────────────────────┘      │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │              Docker Registry                       │  │
│  │  (Docker Hub, private registry)                    │  │
│  └────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

### Lifecycle

```
                    ┌──────────┐
                    │Dockerfile│
                    └────┬─────┘
                         │ docker build
                         ▼
                    ┌──────────┐
                    │  Image   │───────▶ docker push → Registry
                    └────┬─────┘
                         │ docker run
                         ▼
                    ┌──────────┐
                    │Container │───────▶ docker stop → docker rm
                    └──────────┘
                         │ docker commit (creates new image from container)
                         ▼
                    ┌──────────┐
                    │New Image │
                    └──────────┘
```

| Concept | Definition | Analogy |
|---|---|---|
| **Image** | Read-only template with instructions for creating a container | Class (blueprint) |
| **Container** | Runnable instance of an image | Object (instance) |
| **Registry** | Repository for storing and distributing images | Maven Central |
| **Dockerfile** | Script with instructions to build an image | Build script |
| **Volume** | Persistent data storage outside container's filesystem | External hard drive |
| **Network** | Communication pathway between containers | Network cable |

---

## Dockerfile Best Practices

### Multi-Stage Builds for Java Apps

Multi-stage builds allow you to use one image for building and a different, minimal image for running.

```dockerfile
# === Stage 1: Build ===
FROM eclipse-temurin:21-jdk-alpine AS builder

# Install Maven/Gradle
RUN apk add --no-cache maven

WORKDIR /app

# Copy pom.xml and download dependencies first (layer caching)
COPY pom.xml .
COPY src ./src

# Build the application (skip tests for speed in CI)
RUN mvn clean package -DskipTests -DskipITs

# === Stage 2: Run ===
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy only the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Security: run as non-root
USER appuser

# JVM configuration
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+AlwaysPreTouch"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### Layer Optimization

Each `RUN`, `COPY`, `ADD` instruction creates a layer. Layers are cached and reused.

```
Dockerfile layers:
┌─────────────────────────────────────────┐
│ Layer 0: Base image (eclipse-temurin)    │ ← Cached
├─────────────────────────────────────────┤
│ Layer 1: System packages (apk add)       │ ← Cached (rarely changes)
├─────────────────────────────────────────┤
│ Layer 2: Maven wrapper + pom.xml         │ ← Cached (dependencies)
├─────────────────────────────────────────┤
│ Layer 3: Maven dependencies (~500MB)     │ ← Cached (changes only with pom.xml)
├─────────────────────────────────────────┤
│ Layer 4: Application source code         │ ← Changes often (invalidates layer 5)
├─────────────────────────────────────────┤
│ Layer 5: Built JAR                       │ ← Rebuilt every time
├─────────────────────────────────────────┤
│ Layer 6: Final image (runtime)           │ ← Rebuilt every time
└─────────────────────────────────────────┘

Optimization rule: Put things that rarely change at the TOP.
```

```dockerfile
# BAD: Dependencies downloaded every time source changes
COPY . .
RUN mvn clean package

# GOOD: Dependencies cached, only application code rebuilds
COPY pom.xml .
RUN mvn dependency:go-offline  # Downloads dependencies without source
COPY src ./src
RUN mvn clean package -DskipTests
```

### Base Image Selection

```dockerfile
# ─── Full JDK (large, ~400MB) ───
FROM eclipse-temurin:21-jdk
# Use for: development, debugging

# ─── JRE only (medium, ~200MB) ───
FROM eclipse-temurin:21-jre-alpine
# Use for: production (recommended balance of size and functionality)

# ─── Distroless (small, ~150MB, no shell) ───
FROM gcr.io/distroless/java21-debian12
# Use for: security-conscious environments
# Pros: Minimal attack surface, no shell, no package manager
# Cons: Hard to debug (no shell), no apt/apk

# ─── Alpine (small, ~180MB, minimal) ───
FROM eclipse-temurin:21-jre-alpine
# Use for: general production
# Pros: Small, has apk for tools, musl libc (smaller than glibc)
# Cons: DNS resolution quirks, some native libs don't work

# ─── Size comparison ───
# OpenJDK 21-jdk        ~450MB
# eclipse-temurin:21-jre ~250MB
# eclipse-temurin:21-jre-alpine ~180MB
# gcr.io/distroless/java21 ~150MB
```

### Security Best Practices

```dockerfile
# 1. Use specific image tags (never :latest)
FROM eclipse-temurin:21-jre-alpine@sha256:abc123...

# 2. Create and use non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# 3. Remove unnecessary tools
RUN apk del --no-cache wget curl  # If used only during build

# 4. Set read-only root filesystem (at runtime: --read-only)
# In compose file:
# security_opt:
#   - "no-new-privileges:true"
# read_only: true

# 5. Scan images for vulnerabilities
# docker scan <image>
# or use Trivy/Snyk in CI

# 6. Don't store secrets in images
# BAD: ARG DB_PASSWORD=supersecret
# GOOD: Pass at runtime via environment variables or secrets

# 7. Minimal packages
RUN apk add --no-cache --virtual .build-deps maven \
    && mvn clean package \
    && apk del .build-deps  # Remove build tools after build
```

### Caching Strategy for Maven/Gradle

```dockerfile
# Maven dependency caching (multi-stage)
FROM maven:3.9-eclipse-temurin-21 AS dependencies
WORKDIR /build
COPY pom.xml .
# Download dependencies (cached until pom.xml changes)
RUN mvn dependency:go-offline -B

FROM dependencies AS build
COPY src ./src
RUN mvn clean package -DskipTests -o  # -o = offline mode

# Gradle caching
FROM gradle:8-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle
# Download dependencies
RUN gradle dependencies --no-daemon
COPY src ./src
RUN gradle build --no-daemon -x test
```

### BuildKit Optimization

```dockerfile
# Use Docker BuildKit for faster builds
# DOCKER_BUILDKIT=1 docker build .

# 1. Cache mounts — don't persist cache in image layers
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests

# 2. Bind mounts — source code doesn't get copied into intermediate layers
RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=bind,source=src,target=src \
    --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests

# 3. Secret mounts — don't leak credentials
RUN --mount=type=secret,id=mysecret \
    cat /run/secrets/mysecret
```

---

## Production-Grade Spring Boot Dockerfile

### Complete Example

```dockerfile
# ============================================================
# Dockerfile — Spring Boot Microservice
# Version: 1.0.0
# Build: docker build -t order-service:1.0.0 .
# Run: docker run -p 8080:8080 order-service:1.0.0
# ============================================================

# === Stage 1: Build with Maven ===
FROM eclipse-temurin:21-jdk-alpine AS builder

# Install Maven
RUN apk add --no-cache maven

WORKDIR /build

# 1. Copy POM and download dependencies (cached layer)
COPY pom.xml .
RUN mkdir -p /root/.m2 && \
    mvn dependency:go-offline -B

# 2. Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -DskipITs -o

# Extract layers for Spring Boot layered JAR
RUN java -Djarmode=layertools -jar target/*.jar extract --destination extracted

# === Stage 2: Runtime ===
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security updates
RUN apk add --no-cache tini curl ca-certificates && \
    update-ca-certificates

# Non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Tini for proper signal handling (PID 1 problem)
ENTRYPOINT ["/sbin/tini", "--"]

WORKDIR /app

# Copy extracted layers in order of change frequency
# This optimizes layer reuse in registries
COPY --from=builder /build/extracted/dependencies .
COPY --from=builder /build/extracted/spring-boot-loader .
COPY --from=builder /build/extracted/snapshot-dependencies .
COPY --from=builder /build/extracted/application .

# Security
USER appuser

# JVM configuration
ENV JAVA_OPTS="\
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+AlwaysPreTouch \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/tmp/heapdump.hprof"

EXPOSE 8080

# Health check
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=5 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Graceful shutdown
STOPSIGNAL SIGTERM

CMD ["java", "-jar", "app.jar"]
```

### Spring Boot Layered JAR (Spring Boot 2.3+)

```dockerfile
# Spring Boot 2.3+ supports "layered JAR" for better Docker layer reuse
# spring-boot-jarmode-layertools is included by default

# Extract layers:
# java -Djarmode=layertools -jar app.jar extract

# Layers created:
# ├── dependencies        (Maven dependencies, rarely change)
# ├── spring-boot-loader  (Spring Boot loader, changes with Boot version)
# ├── snapshot-dependencies (snapshot dependencies)
# └── application         (your code, changes most often)

# In Dockerfile:
COPY --from=builder /build/extracted/dependencies ./
COPY --from=builder /build/extracted/spring-boot-loader ./
COPY --from=builder /build/extracted/snapshot-dependencies ./
COPY --from=builder /build/extracted/application ./

# If only application changes, only the "application" layer changes
# → Faster image pull (most layers already cached in registry)
```

### Application Properties

```dockerfile
# Environment-driven configuration (don't bake config into image!)
# Pass at runtime:

# BAD: Hardcoded in Dockerfile
ENV SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/mydb
ENV SPRING_DATASOURCE_PASSWORD=password123

# GOOD: No defaults, require runtime configuration
ENV SPRING_PROFILES_ACTIVE=prod
# Database config passed via environment variables at launch

# Application YAML for container-aware config:
# application.yml:
# spring:
#   datasource:
#     url: ${DB_URL:jdbc:mysql://localhost:3306/mydb}
#     username: ${DB_USERNAME:root}
#     password: ${DB_PASSWORD:}
#   redis:
#     host: ${REDIS_HOST:localhost}
#     port: ${REDIS_PORT:6379}
```

---

## Docker Commands Cheat Sheet

### Image Commands

```bash
# ─── Build ───
docker build -t myapp:1.0.0 .                         # Build image
docker build -t myapp:1.0.0 -f Dockerfile.prod .      # Build with custom Dockerfile
docker build --no-cache -t myapp:1.0.0 .               # Build without cache
docker build --build-arg VERSION=1.0.0 -t myapp .      # Build with build args
DOCKER_BUILDKIT=1 docker build -t myapp .               # Build with BuildKit

# ─── Tag & Push ───
docker tag myapp:1.0.0 myregistry.com/myapp:1.0.0      # Tag for registry
docker push myregistry.com/myapp:1.0.0                  # Push to registry

# ─── List & Inspect ───
docker images                                           # List all images
docker image ls -a                                      # All images (including intermediates)
docker images --filter "dangling=true"                   # Dangling images
docker inspect myapp:1.0.0                              # Detailed image info
docker history myapp:1.0.0                              # Layer history

# ─── Remove ───
docker rmi myapp:1.0.0                                  # Remove image
docker image prune                                      # Remove dangling images
docker image prune -a                                   # Remove ALL unused images
docker rmi $(docker images -q -f "dangling=true")       # Remove all dangling

# ─── Scan ───
docker scan myapp:1.0.0                                 # Vulnerability scan
```

### Container Commands

```bash
# ─── Run ───
docker run -d --name myapp -p 8080:8080 myapp:1.0.0    # Run in background
docker run -it --rm myapp:1.0.0 sh                      # Run interactively (debug)
docker run -d --name myapp \
  --memory="512m" --cpus="1.0" \
  -e SPRING_PROFILES_ACTIVE=prod \
  -v /host/data:/app/data \
  --network my-network \
  myapp:1.0.0

# ─── Start/Stop ───
docker start myapp                                       # Start stopped container
docker stop myapp                                        # Graceful stop (SIGTERM)
docker stop -t 30 myapp                                  # Stop with 30s timeout
docker kill myapp                                        # Force stop (SIGKILL)

# ─── List ───
docker ps                                                # Running containers
docker ps -a                                             # All containers
docker ps -q                                             # IDs only
docker ps --format "table {{.Names}}\t{{.Status}}"       # Custom format

# ─── Logs ───
docker logs myapp                                        # View logs
docker logs -f myapp                                     # Follow logs
docker logs --tail 100 myapp                             # Last 100 lines
docker logs --since 5m myapp                             # Last 5 minutes
docker logs --timestamps myapp                           # With timestamps

# ─── Execute ───
docker exec -it myapp sh                                 # Open shell
docker exec myapp cat /var/log/app.log                   # Run single command
docker exec myapp wget -qO- http://localhost:8080/actuator/health  # Check health

# ─── Inspect ───
docker inspect myapp                                     # Full container details
docker inspect --format '{{.NetworkSettings.IPAddress}}' myapp  # IP address
docker stats myapp                                       # Live resource usage
docker top myapp                                         # Running processes
docker port myapp                                        # Port mappings

# ─── Copy ───
docker cp myapp:/app/logs/app.log ./local.log            # Copy from container

# ─── Remove ───
docker rm myapp                                          # Remove container
docker rm -f myapp                                       # Force remove (running)
docker container prune                                   # Remove stopped containers
docker rm $(docker ps -aq -f "status=exited")            # Remove all exited

# ─── Resource limits ───
docker run --memory="256m" --memory-swap="512m" ...      # Memory limits
docker run --cpus="1.5" ...                              # CPU limits
docker run --pids-limit=100 ...                          # Process limit
docker run --restart=always ...                          # Restart policy
docker run --restart=on-failure:5 ...                    # Restart on failure
```

### Volume Commands

```bash
docker volume create my-volume                           # Create volume
docker volume ls                                         # List volumes
docker volume inspect my-volume                          # Volume details
docker volume rm my-volume                               # Remove volume
docker volume prune                                      # Remove unused volumes
```

### Network Commands

```bash
docker network create my-network                         # Create network
docker network ls                                        # List networks
docker network inspect my-network                        # Network details
docker network connect my-network my-container           # Connect container
docker network disconnect my-network my-container        # Disconnect container
docker network prune                                     # Remove unused networks
```

### System Commands

```bash
docker info                                              # System info
docker version                                           # Docker version
docker system df                                         # Disk usage
docker system prune                                      # Clean everything unused
docker system prune -a --volumes                         # Aggressive cleanup
```

---

## Docker Compose

### Overview

Docker Compose defines multi-service applications as YAML. It's the standard for local development and staging environments.

### docker-compose.yml Structure

```yaml
version: "3.8"

# ─── Services (containers) ───
services:
  service-name:
    image: image:tag                         # Use existing image
    build: ./path                            # Build from Dockerfile
    container_name: my-container             # Custom container name
    restart: unless-stopped                  # Restart policy
    ports:
      - "8080:8080"                          # Host:Container port mapping
    environment:
      - ENV_VAR=value                        # Environment variables
      - SPRING_PROFILES_ACTIVE=prod
    env_file:
      - ./config.env                         # Load from file
    volumes:
      - data-volume:/app/data                # Named volume
      - ./host/path:/container/path          # Bind mount
      - /tmp:/tmp                            # Host path
    depends_on:
      - mysql                                # Startup order (not readiness!)
      - redis
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    networks:
      - backend
      - frontend
    deploy:
      resources:
        limits:
          cpus: "1.0"
          memory: "512M"
        reservations:
          cpus: "0.5"
          memory: "256M"
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
    security_opt:
      - "no-new-privileges:true"
    read_only: true                           # Read-only container filesystem
    user: "1000:1000"                         # Run as specific user
    working_dir: /app

# ─── Networks ───
networks:
  frontend:
    driver: bridge
  backend:
    driver: bridge
    internal: true                            # No external access

# ─── Volumes ───
volumes:
  data-volume:
    driver: local
  mysql-data:
    driver: local

# ─── Configs (Docker 17.06+) ───
configs:
  app_config:
    file: ./app.properties

# ─── Secrets ───
secrets:
  db_password:
    file: ./secrets/db_password.txt
```

### Complete Microservice Example

```yaml
version: "3.8"

services:
  # ─── Service Registry & Config Center ───
  nacos:
    image: nacos/nacos-server:v2.3.2
    container_name: nacos
    ports:
      - "8848:8848"
      - "9848:9848"
    environment:
      MODE: standalone
      JVM_XMS: 256m
      JVM_XMX: 512m
    volumes:
      - nacos_data:/home/nacos/data
    networks:
      - backend

  # ─── API Gateway ───
  gateway:
    build:
      context: ./gateway
      dockerfile: Dockerfile
    container_name: gateway
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      NACOS_SERVER: nacos:8848
    depends_on:
      - nacos
    networks:
      - frontend
      - backend
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 30s

  # ─── User Service ───
  user-service:
    build:
      context: ./services/user-service
      dockerfile: Dockerfile
    container_name: user-service
    ports:
      - "8081:8081"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      NACOS_SERVER: nacos:8848
      DB_URL: jdbc:mysql://mysql-user:3306/user_db
      DB_USERNAME: user_svc
      DB_PASSWORD: user_svc_pass
      REDIS_HOST: redis
    depends_on:
      nacos:
        condition: service_healthy
      mysql-user:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - backend
    deploy:
      resources:
        limits:
          memory: "512M"
          cpus: "1.0"

  # ─── Order Service ───
  order-service:
    build:
      context: ./services/order-service
      dockerfile: Dockerfile
    container_name: order-service
    ports:
      - "8082:8082"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      NACOS_SERVER: nacos:8848
      DB_URL: jdbc:mysql://mysql-order:3306/order_db
      DB_USERNAME: order_svc
      DB_PASSWORD: order_svc_pass
      REDIS_HOST: redis
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    depends_on:
      nacos:
        condition: service_healthy
      mysql-order:
        condition: service_healthy
      redis:
        condition: service_healthy
      kafka:
        condition: service_healthy
    networks:
      - backend

  # ─── MySQL for User Service ───
  mysql-user:
    image: mysql:8.0
    container_name: mysql-user
    environment:
      MYSQL_ROOT_PASSWORD: root_pass
      MYSQL_DATABASE: user_db
      MYSQL_USER: user_svc
      MYSQL_PASSWORD: user_svc_pass
    volumes:
      - mysql-user-data:/var/lib/mysql
      - ./init-scripts/user:/docker-entrypoint-initdb.d
    networks:
      - backend
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    deploy:
      resources:
        limits:
          memory: "1G"

  # ─── MySQL for Order Service ───
  mysql-order:
    image: mysql:8.0
    container_name: mysql-order
    environment:
      MYSQL_ROOT_PASSWORD: root_pass
      MYSQL_DATABASE: order_db
      MYSQL_USER: order_svc
      MYSQL_PASSWORD: order_svc_pass
    volumes:
      - mysql-order-data:/var/lib/mysql
      - ./init-scripts/order:/docker-entrypoint-initdb.d
    networks:
      - backend
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ─── Redis ───
  redis:
    image: redis:7.2-alpine
    container_name: redis
    command: redis-server --requirepass devpass --appendonly yes
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    networks:
      - backend
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "devpass", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3

  # ─── Kafka ───
  kafka:
    image: bitnami/kafka:3.7.0
    container_name: kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_CFG_NODE_ID: 0
      KAFKA_CFG_PROCESS_ROLES: controller,broker
      KAFKA_CFG_CONTROLLER_QUORUM_VOTERS: "0@kafka:9093"
      KAFKA_CFG_LISTENERS: "PLAINTEXT://:9092,CONTROLLER://:9093"
      KAFKA_CFG_ADVERTISED_LISTENERS: "PLAINTEXT://kafka:9092"
      KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP: "CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT"
      KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE: "true"
      KAFKA_CFG_NUM_PARTITIONS: 3
      KAFKA_CFG_LOG_RETENTION_HOURS: 168
    volumes:
      - kafka-data:/bitnami/kafka
    networks:
      - backend
    healthcheck:
      test: ["CMD-SHELL", "kafka-topics.sh --bootstrap-server localhost:9092 --list"]
      interval: 15s
      timeout: 5s
      retries: 5
      start_period: 30s

  # ─── Zipkin (Distributed Tracing) ───
  zipkin:
    image: openzipkin/zipkin:3.1
    container_name: zipkin
    ports:
      - "9411:9411"
    networks:
      - backend

  # ─── Prometheus ───
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
    networks:
      - backend

  # ─── Grafana ───
  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
    volumes:
      - grafana-data:/var/lib/grafana
    networks:
      - backend

networks:
  frontend:
    driver: bridge
  backend:
    driver: bridge
    internal: false

volumes:
  nacos_data:
  mysql-user-data:
  mysql-order-data:
  redis-data:
  kafka-data:
  grafana-data:
```

### Startup Order (depends_on)

```yaml
# Simple depends_on — just startup order, not readiness
# NOT ENOUGH for production
services:
  app:
    depends_on:
      - mysql
      - redis

# Health check depends_on (Docker Compose 2.4+) 
services:
  app:
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
      kafka:
        condition: service_healthy

# Wait script approach (works with all versions):
services:
  app:
    entrypoint: >
      sh -c "
        /wait-for-it.sh mysql:3306 -t 60 &&
        /wait-for-it.sh redis:6379 -t 30 &&
        java -jar app.jar
      "
```

```bash
# Using wait-for-it.sh
#!/bin/sh
# wait-for-it.sh host:port -t timeout -- command

# Example: wait for MySQL then start app
./wait-for-it.sh mysql:3306 -t 60 -- java -jar app.jar
```

### Environment-Specific Overrides

```yaml
# docker-compose.override.yml (auto-loaded alongside docker-compose.yml)
# For local development: overrides the production config

version: "3.8"

services:
  user-service:
    ports:
      - "8081:8081"      # Expose port in dev (not in prod)
    volumes:
      - ./services/user-service/target:/app  # Hot reload
    environment:
      SPRING_PROFILES_ACTIVE: dev
      LOGGING_LEVEL_COM_EXAMPLE: DEBUG

  mysql-user:
    ports:
      - "3306:3306"      # Expose DB port for debugging

# docker-compose.prod.yml (use with -f flag)
# docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

version: "3.8"

services:
  user-service:
    deploy:
      replicas: 3
      restart_policy:
        condition: any
    logging:
      driver: "awslogs"
      options:
        awslogs-group: "myapp-prod"

  # No port exposure for services in prod (only gateway exposed)
  gateway:
    ports:
      - "80:8080"
      - "443:8443"
```

### Resource Limits

```yaml
services:
  order-service:
    deploy:
      resources:
        limits:
          cpus: "1.0"            # Max 1 CPU core
          memory: "512M"         # Max 512MB RAM
        reservations:
          cpus: "0.25"           # Guarantee 0.25 CPU
          memory: "256M"         # Guarantee 256MB RAM
```

---

## Container Networking

### Network Drivers

| Driver | Description | Use Case |
|---|---|---|
| **bridge** (default) | Private network, containers communicate via IP | Single-host, multiple containers |
| **host** | Container uses host's network stack (no isolation) | Performance-critical, low latency |
| **overlay** | Multi-host network for Swarm/Kubernetes | Swarm mode, cross-host communication |
| **macvlan** | Assign MAC address to container, appear as physical device | Legacy apps needing direct network access |
| **none** | No network | Offline containers, security |

### Bridge Network

```yaml
# Default bridge vs user-defined bridge

# User-defined bridge (recommended):
networks:
  my-network:
    driver: bridge

services:
  app:
    networks:
      - my-network
  db:
    networks:
      - my-network

# Benefits of user-defined bridge:
# 1. DNS resolution: containers resolve each other by NAME
# 2. Better isolation: only containers on same network communicate
# 3. Attach/detach dynamically without restart

# Default bridge:
# - No DNS resolution (--link needed, deprecated)
# - All containers on default bridge can communicate (no isolation)
```

### DNS Resolution

```yaml
# On user-defined bridge network, Docker provides embedded DNS
# Container name → IP address resolution

services:
  app:
    # Can reach db by name "mysql-user"
    environment:
      DB_URL: jdbc:mysql://mysql-user:3306/mydb

# Use aliases for additional names:
networks:
  backend:
    aliases:
      - database
      - primary-db
```

### Port Mapping

```yaml
services:
  app:
    ports:
      - "8080:8080"                  # Host:Container
      - "8080"                       # Random host port (docker ps to find)
      - "127.0.0.1:8080:8080"        # Bind to specific host IP
      - "8080-8085:8080-8085"        # Range mapping
      - "443:8443"                   # Different ports
      # UDP
      - "53:53/udp"
```

---

## Data Management

### Volumes vs Bind Mounts vs tmpfs

```
Volumes (managed by Docker):
  /var/lib/docker/volumes/volume-name/_data → container path
  Pros: Managed by Docker, portable, backup-friendly, multi-container sharing

Bind Mounts (host directory):
  /host/path → container path
  Pros: Direct access from host, hot-reload for dev
  Cons: Host-specific paths, security concerns

tmpfs (in-memory):
  Container path → host RAM
  Pros: Fast, temporary, no persistence (cache, session)
  Cons: Limited by memory, data lost on restart
```

```yaml
services:
  # Named Volume (recommended for persistent data)
  mysql:
    image: mysql:8.0
    volumes:
      - mysql-data:/var/lib/mysql

  # Bind mount (recommended for development)
  app:
    volumes:
      - ./src:/app/src          # Hot-reload code changes
      - ~/.m2:/root/.m2         # Reuse Maven cache

  # tmpfs (in-memory, no persistence)
  cache:
    image: redis:alpine
    tmpfs:
      - /data:noexec,nosuid,size=256m

volumes:
  mysql-data:
    driver: local
    # With options:
    # driver_opts:
    #   type: nfs
    #   o: addr=192.168.1.1,rw
    #   device: :/path/to/nfs
```

### Database Data Persistence Strategy

```yaml
services:
  mysql:
    image: mysql:8.0
    volumes:
      # 1. Named volume for database files (persistent)
      - mysql-data:/var/lib/mysql

      # 2. Init scripts (executed once on first startup)
      - ./init-scripts:/docker-entrypoint-initdb.d

      # 3. Backup configuration
      - ./mysql-config:/etc/mysql/conf.d
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: myapp

  # Backup job (example)
  db-backup:
    image: mysql:8.0
    volumes:
      - mysql-data:/var/lib/mysql:ro
      - ./backups:/backups
    command: >
      sh -c "
        mysqldump -h mysql -u root -p${MYSQL_ROOT_PASSWORD} myapp > /backups/backup-$$(date +%Y%m%d).sql
      "
    depends_on:
      - mysql
```

---

## Container Logging

### Logging Drivers

```yaml
# Docker has multiple logging drivers:

# Default: json-file (local files)
services:
  app:
    logging:
      driver: "json-file"        # Default
      options:
        max-size: "10m"          # Rotate at 10MB
        max-file: "3"            # Keep 3 rotated files
        labels: "env,service"    # Include Docker labels
        env: "OS,ENVIRONMENT"    # Include environment variables

# For production (centralized logging):
services:
  app:
    logging:
      driver: "gelf"             # Graylog Extended Log Format
      options:
        gelf-address: "udp://logs.example.com:12201"
        tag: "{{.Name}}"

  # Or use fluentd:
    logging:
      driver: "fluentd"
      options:
        fluentd-address: "fluentd:24224"
        tag: "{{.Name}}.{{.ID}}"

  # Or use syslog:
    logging:
      driver: "syslog"
      options:
        syslog-address: "tcp://syslog.example.com:514"
        syslog-facility: "daemon"
        tag: "{{.Name}}"

  # Or use awslogs:
    logging:
      driver: "awslogs"
      options:
        awslogs-region: "us-east-1"
        awslogs-group: "myapp-container-logs"
        awslogs-stream: "{{.Name}}"
```

### JSON File Log Rotation

```yaml
# Global Docker daemon config (/etc/docker/daemon.json)
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}

# Or per-service in compose:
services:
  app:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

### Centralized Logging with ELK/Loki

```yaml
# Docker Compose with Filebeat (shipping to ELK)
services:
  filebeat:
    image: docker.elastic.co/beats/filebeat:8.12.0
    volumes:
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
      - ./filebeat.yml:/usr/share/filebeat/filebeat.yml:ro
    user: root
    depends_on:
      - elasticsearch

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.12.0
    environment:
      discovery.type: single-node
      ES_JAVA_OPTS: "-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    volumes:
      - es-data:/usr/share/elasticsearch/data

  kibana:
    image: docker.elastic.co/kibana/kibana:8.12.0
    ports:
      - "5601:5601"
    environment:
      ELASTICSEARCH_HOSTS: http://elasticsearch:9200
```

---

## Docker Registry

### Docker Hub

```bash
docker login                            # Login to Docker Hub
docker tag myapp:1.0.0 username/myapp:1.0.0  # Tag for Hub
docker push username/myapp:1.0.0         # Push to Docker Hub
docker pull username/myapp:1.0.0         # Pull from Docker Hub
docker search nginx                      # Search images
docker pull nginx:alpine                 # Pull official image
```

### Private Registry

```yaml
# Local registry (basic)
version: "3.8"
services:
  registry:
    image: registry:2
    ports:
      - "5000:5000"
    volumes:
      - registry-data:/var/lib/registry

volumes:
  registry-data:
```

```bash
# Push to private registry
docker tag myapp:1.0.0 localhost:5000/myapp:1.0.0
docker push localhost:5000/myapp:1.0.0
docker pull localhost:5000/myapp:1.0.0
```

### Harbor (Enterprise Registry)

Harbor is an enterprise-grade registry with security scanning, replication, and RBAC.

```yaml
# docker-compose.yml for Harbor (simplified)
# Full install: https://goharbor.io/docs/2.10.0/install-config/
services:
  harbor:
    image: goharbor/harbor-portal:v2.10.0
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - harbor-data:/data
    environment:
      HARBOR_ADMIN_PASSWORD: Harbor12345
```

```bash
# Harbor features:
# - Vulnerability scanning (Trivy)
# - Image replication to other registries
# - Robot accounts for CI/CD
# - Retention policies
# - Audit logging
# - Webhook notifications
```

---

## CI/CD with Docker

### GitLab CI Pipeline

```yaml
# .gitlab-ci.yml
stages:
  - test
  - build
  - package
  - deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"
  IMAGE_TAG: $CI_REGISTRY_IMAGE:$CI_COMMIT_SHORT_SHA

cache:
  key: ${CI_COMMIT_REF_SLUG}
  paths:
    - .m2/repository/
    - target/

# Stage: Test
unit-test:
  stage: test
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn test

# Stage: Build
compile:
  stage: build
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn clean compile -DskipTests
  artifacts:
    paths:
      - target/*.jar

# Stage: Package Docker image
docker-build:
  stage: package
  image: docker:24.0
  services:
    - docker:24.0-dind
  variables:
    DOCKER_BUILDKIT: 1
  script:
    - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY
    - docker build
      --build-arg JAR_FILE=target/*.jar
      -t $CI_REGISTRY_IMAGE:$CI_COMMIT_SHORT_SHA
      -t $CI_REGISTRY_IMAGE:latest
      .
    - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHORT_SHA
    - docker push $CI_REGISTRY_IMAGE:latest
  needs:
    - compile

# Stage: Deploy to staging
deploy-staging:
  stage: deploy
  script:
    - docker-compose -f docker-compose.yml -f docker-compose.staging.yml up -d
  environment:
    name: staging
  only:
    - develop

# Stage: Deploy to production
deploy-production:
  stage: deploy
  script:
    - kubectl set image deployment/order-service order-service=$CI_REGISTRY_IMAGE:$CI_COMMIT_SHORT_SHA
    - kubectl rollout status deployment/order-service
  environment:
    name: production
  when: manual  # Manual approval for prod
  only:
    - main
```

### GitHub Actions

```yaml
# .github/workflows/deploy.yml
name: Build and Deploy

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      - name: Run tests
        run: mvn test

  build-and-push:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          push: true
          tags: |
            ${{ secrets.DOCKER_USERNAME }}/order-service:${{ github.sha }}
            ${{ secrets.DOCKER_USERNAME }}/order-service:latest
          cache-from: type=gha
          cache-to: type=gha,mode=max

  deploy:
    needs: build-and-push
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to K8s
        run: |
          kubectl set image deployment/order-service \
            order-service=${{ secrets.DOCKER_USERNAME }}/order-service:${{ github.sha }}
```

---

## Kubernetes Introduction

### Why K8s for Microservices?

| Need | Docker Alone | Kubernetes |
|---|---|---|
| **Scaling** | Manual (docker run ...) | Auto-scaling (HPA) |
| **Self-healing** | Restart policies only | Health checks, auto-restart |
| **Load balancing** | Port mapping only | Service, Ingress |
| **Rolling updates** | Manual script | Native (Deployment strategies) |
| **Secrets** | Environment variables | Secrets objects |
| **Config management** | env vars | ConfigMap + Secrets |
| **DNS** | docker network DNS | Service DNS (kube-dns) |
| **Storage** | Docker volumes | PV/PVC |
| **Multi-host** | Docker Swarm (limited) | Native multi-node |

### Core Concepts

```
┌──────────────────────────────────────────────────────────┐
│                  Kubernetes Cluster                       │
│                                                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │   Node 1    │  │   Node 2    │  │   Node 3    │     │
│  │             │  │             │  │             │     │
│  │ ┌───────┐  │  │ ┌───────┐  │  │ ┌───────┐  │     │
│  │ │ Pod   │  │  │ │ Pod   │  │  │ │ Pod   │  │     │
│  │ │ App   │  │  │ │ App   │  │  │ │ App   │  │     │
│  │ └───────┘  │  │ └───────┘  │  │ └───────┘  │     │
│  │ ┌───────┐  │  │ ┌───────┐  │  │             │     │
│  │ │ Pod   │  │  │ │ Pod   │  │  │             │     │
│  │ │ App   │  │  │ │ App   │  │  │             │     │
│  │ └───────┘  │  │ └───────┘  │  │             │     │
│  │ ...        │  │ ...        │  │ ...         │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
│                                                          │
│  Control Plane:                                          │
│  [API Server] [Scheduler] [Controller Manager] [etcd]   │
└──────────────────────────────────────────────────────────┘
```

**Key Objects:**

| Object | Purpose | YAML Key Field |
|---|---|---|
| **Pod** | Smallest deployable unit (1+ containers) | `kind: Pod` |
| **Deployment** | Desired state for Pods (replicas, updates) | `kind: Deployment` |
| **Service** | Stable network endpoint for Pods | `kind: Service` |
| **Ingress** | External HTTP/HTTPS routing | `kind: Ingress` |
| **ConfigMap** | Non-sensitive configuration | `kind: ConfigMap` |
| **Secret** | Sensitive data (base64 encoded) | `kind: Secret` |
| **PersistentVolumeClaim** | Storage request | `kind: PersistentVolumeClaim` |
| **HorizontalPodAutoscaler** | Auto-scaling | `kind: HorizontalPodAutoscaler` |

### Simple Spring Boot Deployment

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  labels:
    app: order-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
        - name: order-service
          image: myregistry.com/order-service:1.0.0
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080
              name: http
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            - name: DB_URL
              valueFrom:
                configMapKeyRef:
                  name: app-config
                  key: db.url
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: db-secret
                  key: password
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
            timeoutSeconds: 3
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 5
            timeoutSeconds: 3
          startupProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
            failureThreshold: 30  # Allow 150s for startup
          lifecycle:
            preStop:
              exec:
                command: ["sh", "-c", "sleep 10"]  # Wait for LB to drain
      terminationGracePeriodSeconds: 30
```

```yaml
# service.yaml
apiVersion: v1
kind: Service
metadata:
  name: order-service
spec:
  selector:
    app: order-service
  ports:
    - port: 80
      targetPort: 8080
      name: http
  type: ClusterIP  # Internal only
```

```yaml
# ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: api-gateway
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
    - host: api.example.com
      http:
        paths:
          - path: /api/users
            pathType: Prefix
            backend:
              service:
                name: user-service
                port:
                  number: 80
          - path: /api/orders
            pathType: Prefix
            backend:
              service:
                name: order-service
                port:
                  number: 80
```

```yaml
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  db.url: "jdbc:mysql://mysql-service:3306/order_db"
  redis.host: "redis-service"
  kafka.bootstrap-servers: "kafka-service:9092"
  logging.level.com.example: "DEBUG"
```

```yaml
# secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: db-secret
type: Opaque
data:
  username: b3JkZXJfdXNlcg==      # echo -n "order_user" | base64
  password: cGFzc3dvcmQxMjM=      # echo -n "password123" | base64
```

```yaml
# hpa.yaml (Horizontal Pod Autoscaler)
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: order-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: order-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
```

```bash
# kubectl commands
kubectl apply -f deployment.yaml     # Create/update deployment
kubectl apply -f service.yaml        # Create/update service
kubectl get pods                      # List pods
kubectl get deployments               # List deployments
kubectl get services                  # List services
kubectl logs pod-name                 # View pod logs
kubectl logs -f deployment/order-service  # Stream logs
kubectl exec -it pod-name -- sh       # Open shell
kubectl describe pod pod-name         # Detailed info
kubectl delete pod pod-name           # Delete pod (recreated by Deployment)
kubectl rollout status deployment/order-service  # Check deploy status
kubectl rollout undo deployment/order-service    # Rollback
kubectl port-forward service/order-service 8080:80  # Port forward

# Scaling
kubectl scale deployment/order-service --replicas=5
kubectl autoscale deployment/order-service --min=3 --max=10 --cpu-percent=70

# Secrets and ConfigMaps
kubectl create configmap app-config --from-literal=db.url=jdbc:mysql://...
kubectl create secret generic db-secret --from-literal=password=mypass
```

---

## Docker for Java Developers

### JVM Ergonomics in Containers

```bash
# Before JDK 8u191 / JDK 10:
# JVM detected container cgroups incorrectly
# It would see the HOST's memory/CPU, not the container's limits
# → JVM would allocate too much heap (e.g., 75% of 64GB host = 48GB heap)
# → Container OOM killed

# After JDK 8u191 / JDK 10:
# -XX:+UseContainerSupport  (enabled by default in JDK 10+)
# JVM correctly reads container cgroup limits

# Recommended JVM flags for containers:
JAVA_OPTS="\
  -XX:+UseContainerSupport \           # Container-aware (default in JDK 10+)
  -XX:MaxRAMPercentage=75.0 \          # Use 75% of container memory for heap
  -XX:MinRAMPercentage=50.0 \          # Min heap for small containers
  -XX:InitialRAMPercentage=50.0 \      # Start with 50% heap (avoid over-allocation)
  -XX:+AlwaysPreTouch \                # Pre-allocate heap pages (faster startup, more mem)
  -XX:+UseG1GC \                       # G1GC is default, explicit for clarity
  -XX:MaxGCPauseMillis=200 \           # Target GC pause
  -XX:+HeapDumpOnOutOfMemoryError \    # Heap dump on OOM
  -XX:HeapDumpPath=/tmp/heapdump.hprof \
  -Djava.security.egd=file:/dev/./urandom \  # Faster startup
  -Dfile.encoding=UTF-8 \
"
```

### Memory Limits

```yaml
# Docker memory limit → JVM MaxRAMPercentage
# Container limit: 512M → JVM heap: ~384M (75%)
# Container limit: 1G   → JVM heap: ~768M (75%)
# Container limit: 2G   → JVM heap: ~1.5G (75%)

# Rule of thumb:
# - MaxRAMPercentage=75: Good balance, leaves room for metaspace, threads, etc.
# - MaxRAMPercentage=80: Aggressive, less headroom
# - MaxRAMPercentage=50: Conservative, more room for OS caches

# Verify JVM detected limits:
docker run --memory=512m myapp java -XX:+PrintFlagsFinal -version | grep MaxRAM
# Output: size_t MaxRAM = 536870912  (= 512M)
```

### Graceful Shutdown

```yaml
# Spring Boot graceful shutdown configuration
# application.yml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s  # Max 30s for graceful shutdown

# Dockerfile
STOPSIGNAL SIGTERM  # Default is SIGTERM, explicit for clarity

# Kubernetes preStop hook
lifecycle:
  preStop:
    exec:
      command:
        - sh
        - -c
        - |
          # Notify load balancer we're stopping
          sleep 5
          # Spring Boot handles SIGTERM → begins graceful shutdown
```

### Health and Readiness Probes

```java
// Spring Boot Actuator for K8s probes
// application.yml
management:
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true     # Enable liveness/readiness probes
  health:
    readinessstate:
      enabled: true
    livenessstate:
      enabled: true
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus

// Custom health indicators
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    @Autowired
    private DataSource dataSource;

    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(1000)) {
                return Health.up().build();
            } else {
                return Health.down().withDetail("reason", "Connection not valid").build();
            }
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}

// Readiness: is the service ready to accept traffic?
// Liveness: is the service alive (no deadlock)?
// Startup: has the service finished initializing?
```

---

## Interview Questions

### Basic

1. **"What is the difference between a container and a virtual machine?"**
   - Container: Shares host kernel, OS-level virtualization, MBs, seconds to start
   - VM: Full guest OS, hardware-level virtualization, GBs, minutes to start

2. **"What is the difference between Docker Image and Container?"**
   - Image: Read-only template (like a class)
   - Container: Runnable instance of an image (like an object)

3. **"Explain Dockerfile instructions: FROM, COPY, RUN, CMD, ENTRYPOINT."**
   - FROM: Base image
   - COPY: Copy files from host/build context
   - RUN: Execute command during build (creates layer)
   - CMD: Default command at runtime (overridable)
   - ENTRYPOINT: Main executable at runtime (harder to override)

### Intermediate

4. **"How do you optimize Docker build performance for Java apps?"**
   - Multi-stage builds (build in JDK, run in JRE)
   - Layer caching: dependencies first, code last
   - Use Maven dependency:go-offline + offline mode
   - Use BuildKit (DOCKER_BUILDKIT=1)
   - Use Spring Boot layered JAR extraction

5. **"How do you handle persistent data in Docker?"**
   - Named volumes for database data (mysql-data:/var/lib/mysql)
   - Bind mounts for development (hot-reload)
   - tmpfs for temporary data (cache, sessions)
   - Never store data in container writable layer

6. **"What is the difference between CMD and ENTRYPOINT?"**
   - ENTRYPOINT: Fixed command that always runs (e.g., java -jar app.jar)
   - CMD: Default arguments to ENTRYPOINT (can be overridden)
   - Best practice: ENTRYPOINT for executable, CMD for default args
   - Example: ENTRYPOINT ["java", "-jar", "app.jar"] CMD ["--server.port=8080"]

7. **"How does Docker networking work?"**
   - bridge: Isolated network (default), DNS by container name
   - host: Container uses host network
   - overlay: Multi-host (Swarm/K8s)
   - User-defined bridge: DNS resolution, better isolation

### Advanced

8. **"How do you configure JVM for containers?"**
   - Use JDK 8u191+ or JDK 10+ (UseContainerSupport)
   - -XX:MaxRAMPercentage=75 (heap = 75% of container memory)
   - -XX:+HeapDumpOnOutOfMemoryError
   - Align with Docker --memory limits

9. **"Design a zero-downtime deployment strategy for Spring Boot microservices."**
   - Kubernetes rolling update (maxSurge=25%, maxUnavailable=25%)
   - Readiness probes (don't route traffic until ready)
   - Graceful shutdown (server.shutdown=graceful, preStop sleep)
   - Health checks in Docker (HEALTHCHECK)
   - Session draining before shutdown

10. **"How do you handle configuration and secrets in Docker?"**
    - Environment variables for non-sensitive config
    - Docker secrets for sensitive data (Docker Swarm)
    - Kubernetes ConfigMap + Secrets
    - Vault for dynamic secrets (Hashicorp Vault)
    - NEVER bake secrets into images
    - Use .env files for local development (excluded from Git)

---

## Summary

| Topic | Key Takeaway |
|---|---|
| **Dockerfile** | Multi-stage builds, layer caching, minimal base images, non-root user |
| **Docker Compose** | Multi-service orchestration, depends_on with health checks, resource limits |
| **Networking** | User-defined bridge for DNS, port mapping for external access |
| **Data** | Named volumes for persistence, bind mounts for dev, tmpfs for cache |
| **Logging** | JSON file with rotation, ELK stack for centralized logging |
| **CI/CD** | GitLab/GitHub Actions: test → build → push → deploy |
| **Kubernetes** | Deployment, Service, Ingress, ConfigMap, Secret, HPA |
| **Java in Docker** | UseContainerSupport, MaxRAMPercentage, graceful shutdown, health probes |
