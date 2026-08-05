# 构建脚本与 CI/CD 自动化

> 构建脚本是软件交付的"生产线蓝图"——从 Maven/Gradle Wrapper 锁定版本，到 CI/CD 流水线编排，一键式交付

## 📚 目录

1. [Maven Wrapper (mvnw)](#1)
2. [Gradle Wrapper (gradlew)](#2)
3. [构建配置脚本编写](#3)
4. [CI/CD 流水线脚本](#4)
5. [容器化构建脚本](#5)
6. [自动化部署脚本](#6)
7. [多环境配置管理](#7)

---

## 1. Maven Wrapper (mvnw) {#1}

### 1.1 什么是 Maven Wrapper？

Maven Wrapper 是 Maven 项目的"自举"机制——项目自带一个固定版本的 Maven，开发者无需预装 Maven 即可构建。类似 Gradle Wrapper 的理念，确保团队和 CI 环境使用完全一致的 Maven 版本。

```bash
# 为项目生成 Maven Wrapper
mvn wrapper:wrapper -Dmaven=3.9.9

# 或使用 Maven Wrapper 插件（无需预装 Maven 也能生成）
# 先由一个人生成，提交到版本控制
```

### 1.2 Wrapper 文件结构

```
my-project/
├── mvnw                    # Unix Shell 脚本（可执行）
├── mvnw.cmd                # Windows Batch 脚本
└── .mvn/
    └── wrapper/
        ├── maven-wrapper.jar          # Wrapper JAR
        └── maven-wrapper.properties   # 版本配置
```

```properties
# .mvn/wrapper/maven-wrapper.properties
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip
wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar
```

### 1.3 mvnw vs mvn

| 维度 | mvnw（推荐） | mvn（系统安装） |
|------|:---:|:---:|
| **版本一致性** | ✅ 项目锁定版本 | ❌ 各人可能不同 |
| **环境依赖** | ✅ 只需 JDK | ❌ 需预装 Maven |
| **CI/CD 友好** | ✅ 零安装 | ⚠️ 需预装 |
| **版本升级** | 修改 properties 文件 | 系统级升级 |
| **文件体积** | ~50KB + jar | 无需文件 |

### 1.4 常见用法

```bash
# ── 所有操作与 mvn 完全相同，只是将 mvn 替换为 mvnw ──

# 基础操作
./mvnw clean compile                    # 编译
./mvnw clean test                       # 运行测试
./mvnw clean package                    # 打包
./mvnw clean install                    # 安装到本地仓库

# 跳过测试
./mvnw clean package -DskipTests

# 指定 Profile
./mvnw clean package -Pproduction

# 多线程并行构建（T = threads）
./mvnw clean package -T 4

# 离线模式（跳过依赖更新检查）
./mvnw clean package -o

# 更新 Wrapper 版本
./mvnw wrapper:wrapper -Dmaven=3.9.9

# 仅编译改动的模块（增量构建）
./mvnw compile -pl changed-module -am   # -am: also make dependencies
```

> 🎯 **核心要点**：新项目一律使用 Wrapper（mvnw 或 gradlew），这是"克隆即构建"的基础——任何人 clone 项目后都能直接构建，环境差异归零。

---

## 2. Gradle Wrapper (gradlew) {#2}

### 2.1 Wrapper 文件结构

```
my-project/
├── gradlew                # Unix Shell 脚本
├── gradlew.bat            # Windows Batch 脚本
└── gradle/
    └── wrapper/
        ├── gradle-wrapper.jar        # Wrapper JAR
        └── gradle-wrapper.properties # 版本配置
```

```properties
# gradle/wrapper/gradle-wrapper.properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

### 2.2 升级 Gradle Wrapper

```bash
# 方法1：使用 wrapper task
./gradlew wrapper --gradle-version 8.9

# 方法2：直接修改 properties 文件中的 distributionUrl
# gradle/wrapper/gradle-wrapper.properties
# distributionUrl=https\://.../gradle-8.9-bin.zip

# 方法3：指定 distribution type
./gradlew wrapper --gradle-version 8.9 --distribution-type all    # 含源码和文档
./gradlew wrapper --gradle-version 8.9 --distribution-type bin    # 仅二进制
```

### 2.3 Gradle Wrapper 常用命令

```bash
# ── 基础任务 ──
./gradlew build                                  # 编译 + 测试 + 打包
./gradlew clean build                            # 先清理
./gradlew test                                   # 运行测试
./gradlew bootRun                                # Spring Boot 运行

# ── 并行与缓存 ──
./gradlew build --parallel                       # 并行构建
./gradlew build --build-cache                    # 启用构建缓存
./gradlew build --no-build-cache                 # 禁用构建缓存

# ── 增量构建 ──
./gradlew build -x test                          # 跳过测试 (-x: exclude)
./gradlew build --continue                       # 失败后继续（查所有错误）

# ── 依赖分析 ──
./gradlew dependencies                           # 查看依赖树
./gradlew dependencies --configuration runtimeClasspath
./gradlew dependencyUpdates                      # 检查依赖更新（需插件）

# ── 性能分析 ──
./gradlew build --scan                           # 生成构建扫描报告
./gradlew build --profile                        # 性能报告

# ── Kotlin DSL 项目 ──
./gradlew wrapper --gradle-version 8.9
```

---

## 3. 构建配置脚本编写 {#3}

### 3.1 Maven pom.xml 核心结构

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- 项目坐标 -->
    <groupId>com.example</groupId>
    <artifactId>my-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <!-- 继承 Spring Boot 父 POM -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
        <relativePath/>
    </parent>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>

    <!-- 多环境 Profile -->
    <profiles>
        <profile>
            <id>dev</id>
            <activation><activeByDefault>true</activeByDefault></activation>
            <properties>
                <spring.profiles.active>dev</spring.profiles.active>
            </properties>
        </profile>
        <profile>
            <id>production</id>
            <properties>
                <spring.profiles.active>production</spring.profiles.active>
            </properties>
        </profile>
    </profiles>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 3.2 Gradle Kotlin DSL 核心结构

```kotlin
// build.gradle.kts
plugins {
    java
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    id("com.gorylenko.gradle-git-properties") version "2.4.2"  // Git 信息注入
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    // 私有仓库
    maven { url = uri("https://nexus.company.com/repository/maven-public/") }
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // 数据库
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")  // 测试用

    // 工具库
    implementation("com.google.guava:guava:33.2.0-jre")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // 测试
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql")
}

tasks.withType<Test> {
    useJUnitPlatform()
    // 并行测试
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
}

// 自定义任务：打印依赖树
tasks.register("printDeps") {
    doLast {
        configurations.runtimeClasspath.get().forEach {
            println("${it.name}")
        }
    }
}
```

### 3.3 多模块项目结构

```
parent-project/
├── build.gradle.kts                     # 根构建脚本
├── settings.gradle.kts                  # 模块声明
├── gradlew
├── common/
│   └── build.gradle.kts
├── service-user/
│   └── build.gradle.kts
├── service-order/
│   └── build.gradle.kts
└── gateway/
    └── build.gradle.kts
```

```kotlin
// settings.gradle.kts —— 声明子模块
rootProject.name = "my-platform"

include("common")
include("service-user")
include("service-order")
include("gateway")
```

```kotlin
// 根 build.gradle.kts —— 公共配置
subprojects {
    apply(plugin = "java")

    group = "com.example"
    version = "1.0.0-SNAPSHOT"

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    repositories {
        mavenCentral()
    }
}
```

---

## 4. CI/CD 流水线脚本 {#4}

### 4.1 GitHub Actions

```yaml
# .github/workflows/ci.yml
name: Java CI with Maven

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

env:
  JAVA_VERSION: '21'
  MAVEN_OPTS: '-Xmx2g'

jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 30

    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: testdb
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup JDK ${{ env.JAVA_VERSION }}
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: 'maven'

      - name: Cache SonarQube packages
        uses: actions/cache@v4
        with:
          path: ~/.sonar/cache
          key: ${{ runner.os }}-sonar
          restore-keys: ${{ runner.os }}-sonar

      - name: Compile
        run: ./mvnw clean compile -T 4

      - name: Unit Tests
        run: ./mvnw test

      - name: Integration Tests
        run: ./mvnw verify -Pintegration-test

      - name: Static Analysis
        run: ./mvnw checkstyle:check pmd:check spotbugs:check

      - name: Package
        run: ./mvnw package -DskipTests

      - name: Build Docker Image
        run: |
          docker build -t myapp:${{ github.sha }} .
          docker tag myapp:${{ github.sha }} myapp:latest

      - name: Push to Registry
        if: github.ref == 'refs/heads/main'
        run: |
          echo "${{ secrets.DOCKER_PASSWORD }}" | docker login -u "${{ secrets.DOCKER_USERNAME }}" --password-stdin
          docker push myapp:${{ github.sha }}
```

### 4.2 GitLab CI

```yaml
# .gitlab-ci.yml
default:
  image: maven:3.9.9-eclipse-temurin-21
  tags:
    - docker

variables:
  MAVEN_OPTS: "-Xmx2g -XX:MaxRAMPercentage=75.0"
  MAVEN_CLI_OPTS: "--batch-mode --errors --fail-at-end --show-version"

# 缓存 Maven 本地仓库
cache:
  key: ${CI_COMMIT_REF_SLUG}
  paths:
    - .m2/repository/

stages:
  - compile
  - test
  - package
  - deploy

compile:
  stage: compile
  script:
    - ./mvnw ${MAVEN_CLI_OPTS} clean compile -T 4
  artifacts:
    paths:
      - target/

unit-test:
  stage: test
  script:
    - ./mvnw ${MAVEN_CLI_OPTS} test
  artifacts:
    reports:
      junit:
        - target/surefire-reports/TEST-*.xml

integration-test:
  stage: test
  services:
    - postgres:16-alpine
  variables:
    POSTGRES_DB: testdb
    POSTGRES_USER: test
    POSTGRES_PASSWORD: test
  script:
    - ./mvnw ${MAVEN_CLI_OPTS} verify -Pintegration-test

package:
  stage: package
  script:
    - ./mvnw ${MAVEN_CLI_OPTS} package -DskipTests
  artifacts:
    paths:
      - target/*.jar
  only:
    - main
    - develop

deploy-staging:
  stage: deploy
  script:
    - scp target/*.jar deploy@staging-server:/opt/app/
    - ssh deploy@staging-server "sudo systemctl restart myapp"
  environment:
    name: staging
  only:
    - develop

deploy-production:
  stage: deploy
  script:
    - scp target/*.jar deploy@prod-server:/opt/app/
    - ssh deploy@prod-server "sudo systemctl restart myapp"
  environment:
    name: production
  only:
    - main
  when: manual    # 手动触发
```

### 4.3 Jenkins Pipeline

```groovy
// Jenkinsfile —— 声明式 Pipeline（Groovy DSL）
pipeline {
    agent any

    tools {
        maven 'Maven-3.9.9'
        jdk 'JDK-21'
    }

    environment {
        MAVEN_OPTS = '-Xmx2g'
        DOCKER_REGISTRY = 'registry.company.com'
    }

    parameters {
        choice(name: 'ENVIRONMENT', choices: ['dev', 'staging', 'production'],
               description: '部署环境')
        booleanParam(name: 'SKIP_TESTS', defaultValue: false,
                     description: '跳过测试')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                echo "构建分支: ${env.BRANCH_NAME}"
                echo "提交: ${currentBuild.changeSets}"
            }
        }

        stage('Build') {
            steps {
                sh './mvnw clean compile -T 4'
            }
        }

        stage('Test') {
            when { expression { !params.SKIP_TESTS } }
            parallel {
                stage('Unit Tests') {
                    steps {
                        sh './mvnw test'
                    }
                }
                stage('Code Quality') {
                    steps {
                        sh './mvnw checkstyle:check pmd:check'
                    }
                }
            }
            post {
                always {
                    junit '**/target/surefire-reports/TEST-*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                sh './mvnw package -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Deploy') {
            when { branch 'main' }
            steps {
                script {
                    deployApp(params.ENVIRONMENT)
                }
            }
        }
    }

    post {
        success {
            emailext(
                subject: "✅ ${env.JOB_NAME} #${env.BUILD_NUMBER} 构建成功",
                body: "构建详情: ${env.BUILD_URL}",
                to: 'dev-team@company.com'
            )
        }
        failure {
            emailext(
                subject: "❌ ${env.JOB_NAME} #${env.BUILD_NUMBER} 构建失败",
                body: "构建详情: ${env.BUILD_URL}\n\n变更:\n${currentBuild.changeSets}",
                to: '${currentBuild.changeSets.authorEmail}'
            )
        }
    }
}
```

---

## 5. 容器化构建脚本 {#5}

### 5.1 多阶段 Docker 构建

```dockerfile
# Dockerfile —— 多阶段构建
# 阶段1: 编译
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build
COPY pom.xml .
COPY src/ src/

# 利用 Docker 层缓存：先下载依赖
RUN mvn dependency:go-offline -B
RUN mvn clean package -DskipTests -B

# 阶段2: 运行（最小化镜像）
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

# 安全：非 root 用户运行
USER appuser

# JVM 参数通过环境变量覆盖
ENV JAVA_OPTS="-Xms256m -Xmx1g -XX:+UseG1GC"

EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT exec java ${JAVA_OPTS} -jar app.jar
```

### 5.2 Docker Compose 编排脚本

```yaml
# docker-compose.yml
version: '3.9'

services:
  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: myapp
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/myapp
      - SPRING_DATASOURCE_USERNAME=appuser
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
    depends_on:
      db:
        condition: service_healthy
    restart: unless-stopped
    networks:
      - app-network

  db:
    image: postgres:16-alpine
    container_name: myapp-db
    environment:
      - POSTGRES_DB=myapp
      - POSTGRES_USER=appuser
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U appuser -d myapp"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - app-network

  redis:
    image: redis:7-alpine
    container_name: myapp-redis
    command: redis-server --appendonly yes
    volumes:
      - redisdata:/data
    networks:
      - app-network

volumes:
  pgdata:
  redisdata:

networks:
  app-network:
    driver: bridge
```

### 5.3 构建部署一体化脚本

```bash
#!/bin/bash
# deploy.sh —— 本地构建 + Docker 部署

set -euo pipefail

ENVIRONMENT="${1:-dev}"
readonly DOCKER_REGISTRY="${DOCKER_REGISTRY:-localhost:5000}"
readonly APP_NAME="myapp"
readonly TAG="${ENVIRONMENT}-$(date +%Y%m%d-%H%M%S)"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  部署环境: ${ENVIRONMENT}"
echo "  镜像标签: ${TAG}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# 步骤1：编译
echo "🔨 编译项目..."
./mvnw clean package -DskipTests -P"${ENVIRONMENT}"

# 步骤2：构建 Docker 镜像
echo "🐳 构建 Docker 镜像..."
docker build -t "${DOCKER_REGISTRY}/${APP_NAME}:${TAG}" .
docker tag "${DOCKER_REGISTRY}/${APP_NAME}:${TAG}" \
           "${DOCKER_REGISTRY}/${APP_NAME}:${ENVIRONMENT}"

# 步骤3：推送镜像
echo "📤 推送镜像..."
docker push "${DOCKER_REGISTRY}/${APP_NAME}:${TAG}"
docker push "${DOCKER_REGISTRY}/${APP_NAME}:${ENVIRONMENT}"

# 步骤4：部署（以 Docker Compose 为例）
if [[ "${ENVIRONMENT}" == "production" ]]; then
    echo "⚠️  生产环境部署需手动确认"
    read -p "确认部署到生产环境? (yes/no): " confirm
    [[ "${confirm}" != "yes" ]] && { echo "已取消"; exit 0; }
fi

echo "🚀 部署服务..."
docker compose -f "docker-compose-${ENVIRONMENT}.yml" pull
docker compose -f "docker-compose-${ENVIRONMENT}.yml" up -d --remove-orphans

# 步骤5：健康检查
echo "🏥 健康检查..."
for i in {1..30}; do
    if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✅ 服务健康检查通过"
        exit 0
    fi
    echo "   等待... (${i}/30)"
    sleep 2
done

echo "❌ 健康检查超时"
docker compose logs --tail=50
exit 1
```

---

## 6. 自动化部署脚本 {#6}

### 6.1 蓝绿部署脚本

```bash
#!/bin/bash
# blue-green-deploy.sh —— 零停机蓝绿部署

set -euo pipefail

readonly APP_NAME="myapp"
readonly NEW_VERSION="$1"
readonly HEALTH_URL="http://localhost"

# 当前活跃端口
readonly BLUE_PORT=8081
readonly GREEN_PORT=8082

CURRENT_PORT=""      # 当前服务端口
NEW_PORT=""          # 新部署端口

# ── 检测当前活跃端口 ──
if curl -sf "${HEALTH_URL}:${BLUE_PORT}/actuator/health" > /dev/null 2>&1; then
    CURRENT_PORT=${BLUE_PORT}
    NEW_PORT=${GREEN_PORT}
elif curl -sf "${HEALTH_URL}:${GREEN_PORT}/actuator/health" > /dev/null 2>&1; then
    CURRENT_PORT=${GREEN_PORT}
    NEW_PORT=${BLUE_PORT}
else
    echo "❌ 无运行中的服务"
    exit 1
fi

echo "🟢 当前活跃端口: ${CURRENT_PORT}"
echo "🔵 新部署端口:   ${NEW_PORT}"

# ── 部署新版本 ──
echo "🚀 在端口 ${NEW_PORT} 启动新版本..."
docker run -d \
    --name "${APP_NAME}-${NEW_PORT}" \
    -p "${NEW_PORT}:8080" \
    -e "SERVER_PORT=8080" \
    "${APP_NAME}:${NEW_VERSION}"

# ── 等待新服务就绪 ──
echo "⏳ 等待新服务就绪..."
for i in {1..60}; do
    if curl -sf "${HEALTH_URL}:${NEW_PORT}/actuator/health" > /dev/null 2>&1; then
        break
    fi
    sleep 1
done

if ! curl -sf "${HEALTH_URL}:${NEW_PORT}/actuator/health" > /dev/null 2>&1; then
    echo "❌ 新服务启动超时"
    docker stop "${APP_NAME}-${NEW_PORT}" && docker rm "${APP_NAME}-${NEW_PORT}"
    exit 1
fi

# ── 切换流量（通过 Nginx reload）──
echo "🔄 切换流量 -> ${NEW_PORT}"
sed -i "s/${CURRENT_PORT}/${NEW_PORT}/g" /etc/nginx/sites-enabled/${APP_NAME}
nginx -s reload

# ── 优雅关闭旧服务 ──
echo "🛑 关闭旧服务 (端口 ${CURRENT_PORT})..."
docker stop "${APP_NAME}-${CURRENT_PORT}" && docker rm "${APP_NAME}-${CURRENT_PORT}"

echo "✅ 蓝绿部署完成：${NEW_PORT} 已成为活跃端口"
```

### 6.2 回滚脚本

```bash
#!/bin/bash
# rollback.sh —— 回滚到上一个版本

set -euo pipefail

readonly APP_NAME="myapp"
readonly BACKUP_DIR="/opt/backups/${APP_NAME}"

# 查找最新备份
LATEST_BACKUP=$(ls -t "${BACKUP_DIR}"/*.jar 2>/dev/null | head -1)

if [[ -z "${LATEST_BACKUP}" ]]; then
    echo "❌ 无可用备份"
    exit 1
fi

echo "⏪ 回滚到: ${LATEST_BACKUP}"

# 停止当前服务
sudo systemctl stop "${APP_NAME}"

# 替换 JAR
cp "${LATEST_BACKUP}" "/opt/${APP_NAME}/app.jar"

# 启动服务
sudo systemctl start "${APP_NAME}"

# 等待启动
sleep 10

# 验证
if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ 回滚成功，服务正常运行"
else
    echo "❌ 回滚后服务异常，请手动检查"
    exit 1
fi
```

---

## 7. 多环境配置管理 {#7}

### 7.1 Spring Boot Profile 配置

```yaml
# application.yml —— 公共配置
server:
  port: 8080

spring:
  application:
    name: my-service
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    open-in-view: false

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
---
# application-dev.yml
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:h2:mem:devdb
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

logging:
  level:
    com.example: DEBUG
---
# application-production.yml
spring:
  config:
    activate:
      on-profile: production
  datasource:
    url: ${DATABASE_URL}          # 通过环境变量注入
    username: ${DATABASE_USER}
    password: ${DATABASE_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

logging:
  level:
    com.example: WARN
    org.springframework: WARN
```

### 7.2 环境变量与密钥管理

```bash
#!/bin/bash
# env-setup.sh —— 环境变量注入脚本

# ── 按环境加载不同配置 ──
ENV="${1:-dev}"

case "${ENV}" in
    dev)
        export DB_HOST="localhost"
        export DB_PORT="5432"
        export REDIS_HOST="localhost"
        export LOG_LEVEL="DEBUG"
        ;;
    staging)
        export DB_HOST="staging-db.internal"
        export DB_PORT="5432"
        export REDIS_HOST="staging-redis.internal"
        export LOG_LEVEL="INFO"
        ;;
    production)
        # 生产配置来自密钥管理服务
        export DB_HOST=$(vault read -field=host secret/myapp/db)
        export DB_PORT="5432"
        export REDIS_HOST=$(vault read -field=host secret/myapp/redis)
        export LOG_LEVEL="WARN"
        ;;
    *)
        echo "未知环境: ${ENV}"
        exit 1
        ;;
esac

# ── 启动应用 ──
exec java \
    -Dspring.profiles.active="${ENV}" \
    -Ddb.host="${DB_HOST}" \
    -Ddb.port="${DB_PORT}" \
    -jar myapp.jar
```

> 🎯 **核心要点**：敏感信息（密码、密钥、Token）绝不硬编码在配置文件中——使用环境变量、Secrets Manager、HashiCorp Vault 等安全存储方案。

---

**返回总览：** [00-脚本知识体系总览](./00-脚本知识体系总览.md) | **下一模块：** [05-脚本安全与最佳实践](./05-脚本安全与最佳实践.md)
