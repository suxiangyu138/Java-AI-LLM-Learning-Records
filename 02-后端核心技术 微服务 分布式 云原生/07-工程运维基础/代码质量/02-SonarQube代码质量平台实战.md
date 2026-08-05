# SonarQube 代码质量平台实战

> **SonarQube 是业界最主流的代码质量平台，覆盖静态分析、质量门禁、技术债务管理、安全热点审查，也是 CI/CD 流水线中不可或缺的质量枢纽。本文从零搭建到生产级运维全覆盖。**

## 目录

1. [SonarQube 架构](#1-sonarqube-架构)
2. [版本对比与选型](#2-版本对比与选型)
3. [安装部署](#3-安装部署)
4. [项目创建与分析配置](#4-项目创建与分析配置)
5. [Quality Profiles 质量配置档案](#5-quality-profiles-质量配置档案)
6. [Quality Gates 质量门禁](#6-quality-gates-质量门禁)
7. [sonar-project.properties 完整参考](#7-sonar-projectproperties-完整参考)
8. [Maven 集成](#8-maven-集成)
9. [Gradle 集成](#9-gradle-集成)
10. [CI/CD 流水线集成](#10-cicd-流水线集成)
11. [分析参数全表](#11-分析参数全表)
12. [New Code Period 与 Clean as You Code](#12-new-code-period-与-clean-as-you-code)
13. [Dashboard 解读](#13-dashboard-解读)
14. [Security Hotspots 安全热点](#14-security-hotspots-安全热点)
15. [JaCoCo 覆盖率集成](#15-jacoco-覆盖率集成)
16. [SonarQube API 自动化](#16-sonarqube-api-自动化)
17. [常见问题与实战经验](#17-常见问题与实战经验)
18. [SonarQube vs SonarCloud vs SonarLint](#18-sonarqube-vs-sonarcloud-vs-sonarlint)

---

## 1. SonarQube 架构

### 1.1 整体架构

```text
┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│    Scanner       │     │   Server         │     │   Database       │
│  (分析器, 客户端) │────▶│  (Web 服务端)     │────▶│  (数据存储)      │
│                  │     │                  │     │                  │
│ · Maven Plugin   │     │ · Web UI         │     │ · PostgreSQL     │
│ · Gradle Plugin  │     │ · REST API       │     │                  │
│ · Sonar Scanner  │     │ · Compute Engine  │     │                  │
│ · CLI            │     │ · Quality Gate   │     │                  │
└──────────────────┘     └───────┬──────────┘     └──────────────────┘
                                 │
                                 ▼
                        ┌──────────────────┐
                        │   Elasticsearch  │
                        │  (搜索引擎, 可选) │
                        │  · 代码搜索       │
                        │  · 问题检索       │
                        └──────────────────┘
```

### 1.2 各组件职责

| 组件 | 功能 | 部署说明 |
|------|------|---------|
| **SonarQube Scanner** | 分析代码、生成报告、发送到 Server | CI 构建节点执行，支持多种方式（CLI, Maven, Gradle） |
| **SonarQube Server** | Web 服务、API、计算引擎、项目管理 | 需要独立部署的 Java 应用，是整个平台的核心 |
| **Database** | 存储分析结果、配置、用户数据 | 仅支持 PostgreSQL（10.x 及以上版本） |
| **Elasticsearch** | 代码搜索、问题快速检索 | 9.9+ 版本已移除内置 ES，改用 PG 全文搜索 |

### 1.3 分析流程

```text
开发者 Push 代码
      │
      ▼
CI/CD 触发分析
      │
      ▼
SonarScanner 编译项目 → 执行静态分析
      │
      ├── 读取 sonar-project.properties
      ├── 多模块递归扫描
      ├── 运行分析规则引擎
      └── 收集测试覆盖率（JaCoCo）
      │
      ▼
发送分析报告到 SonarQube Server
      │
      ▼
Server 的 Compute Engine 处理报告
      │
      ├── 执行 Quality Gate 判定
      ├── 更新技术债务数据
      ├── 触发 Webhook 通知
      └── 生成问题、指标、趋势
      │
      ▼
结果写入 Database → 更新 UI Dashboard → 通知 CI 结果
```

---

## 2. 版本对比与选型

### 2.1 版本功能对比

| 特性 | Community（社区版） | Developer（开发者版） | Enterprise（企业版） | Data Center（数据中心版） |
|------|:---:|:---:|:---:|:---:|
| **价格** | 免费 | 收费 | 收费 | 收费（最高） |
| **许可证** | LGPL-3.0 | 商业许可 | 商业许可 | 商业许可 |
| **语言分析** | 27 种 | 29+ 种 | 29+ 种 | 29+ 种 |
| **质量门禁** | ✅ | ✅ | ✅ | ✅ |
| **技术债务** | ✅ | ✅ | ✅ | ✅ |
| **Security Hotspots** | ✅ | ✅ | ✅ | ✅ |
| **Branch Analysis** | ❌ | ✅ | ✅ | ✅ |
| **PR/MR Decoration** | ❌ | ✅ | ✅ | ✅ |
| **AI Code Assurance** | ❌ | ✅ | ✅ | ✅ |
| **Portfolio Management** | ❌ | ❌ | ✅ | ✅ |
| **PDF Report** | ❌ | ❌ | ✅ | ✅ |
| **High Availability** | ❌ | ❌ | ❌ | ✅ |
| **最大代码行数** | 无限 | 无限 | 无限 | 无限 |
| **最大用户数** | 无限 | 无限 | 无限 | 无限，支持集群 |

> 💡 **选型建议**：
> - 个人/小型团队：Community 版完全够用，核心质量门禁和技术债务功能齐全
> - 中型团队（需要分支分析）：Developer 版是性价比之选
> - 大型企业（多项目组合管理）：Enterprise 版
> - 高可用要求（7x24 关键系统）：Data Center 版

### 2.2 Branch Analysis 的重要性

Developer 版及以上的 **Branch Analysis（分支分析）** 是关键差异化功能：

```text
Community 版                       Developer+ 版
┌─────────────────────┐           ┌─────────────────────┐
│  仅支持默认分支分析   │           │  所有分支独立分析    │
│  (通常是 main)       │           │  · feature/xxx      │
│                      │           │  · develop          │
│  所有分支的分析结果   │           │  · release/xxx      │
│  都合并到 main 显示   │           │  · hotfix/xxx       │
│                      │           │                     │
│  无法区分"新代码"    │           │  分支级 New Code     │
│  是相对于哪个分支     │           │  比较准确             │
└─────────────────────┘           └─────────────────────┘
```

---

## 3. 安装部署

### 3.1 Docker Compose 部署（推荐）

> 💡 Docker Compose 是最快捷的 SonarQube 部署方式，适用于开发环境和小型团队。

```yaml
# docker-compose.yml
version: "3.8"

services:
  sonarqube:
    image: sonarqube:community-10.6.0
    container_name: sonarqube
    depends_on:
      - postgres
    environment:
      SONAR_JDBC_URL: jdbc:postgresql://postgres:5432/sonar
      SONAR_JDBC_USERNAME: sonar
      SONAR_JDBC_PASSWORD: sonar_password
      SONAR_ES_BOOTSTRAP_CHECKS_DISABLE: "true"
    volumes:
      - sonar_data:/opt/sonarqube/data
      - sonar_logs:/opt/sonarqube/logs
      - sonar_extensions:/opt/sonarqube/extensions
    ports:
      - "9000:9000"
    restart: unless-stopped

  postgres:
    image: postgres:15
    container_name: sonar-postgres
    environment:
      POSTGRES_DB: sonar
      POSTGRES_USER: sonar
      POSTGRES_PASSWORD: sonar_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped

volumes:
  sonar_data:
  sonar_logs:
  sonar_extensions:
  postgres_data:
```

启动命令：

```bash
# 系统要求（Linux 需要）
sysctl -w vm.max_map_count=524288
sysctl -w fs.file-max=131072

# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f sonarqube

# 停止服务
docker-compose down

# 清理数据（重置全部状态）
docker-compose down -v
```

### 3.2 直接 Docker 部署

```bash
# 启动 PostgreSQL
docker run -d --name sonar-postgres \
  -e POSTGRES_DB=sonar \
  -e POSTGRES_USER=sonar \
  -e POSTGRES_PASSWORD=sonar_password \
  -v postgres_data:/var/lib/postgresql/data \
  postgres:15

# 启动 SonarQube
docker run -d --name sonarqube \
  --link sonar-postgres:postgres \
  -e SONAR_JDBC_URL=jdbc:postgresql://postgres:5432/sonar \
  -e SONAR_JDBC_USERNAME=sonar \
  -e SONAR_JDBC_PASSWORD=sonar_password \
  -p 9000:9000 \
  -v sonar_data:/opt/sonarqube/data \
  -v sonar_logs:/opt/sonarqube/logs \
  -v sonar_extensions:/opt/sonarqube/extensions \
  sonarqube:community-10.6.0
```

### 3.3 二进制包部署

```bash
# 1. 下载 SonarQube
wget https://binaries.sonarsource.com/Distribution/sonarqube/sonarqube-10.6.0.92116.zip
unzip sonarqube-10.6.0.92116.zip
cd sonarqube-10.6.0.92116

# 2. 配置数据库（conf/sonar.properties）
echo "sonar.jdbc.username=sonar" >> conf/sonar.properties
echo "sonar.jdbc.password=sonar_password" >> conf/sonar.properties
echo "sonar.jdbc.url=jdbc:postgresql://localhost:5432/sonar" >> conf/sonar.properties

# 3. 配置系统参数（必须使用非 root 用户运行）
#    sysctl -w vm.max_map_count=524288
#    sysctl -w fs.file-max=131072
#    ulimit -n 131072
#    ulimit -u 8192

# 4. 启动
#    Linux/Mac:
bin/linux-x86-64/sonar.sh start

#    Windows:
bin/windows-x86-64/StartSonar.bat

# 5. 访问 http://localhost:9000 （默认 admin / admin）
```

### 3.4 首次登录配置

```text
1. 浏览器访问 http://localhost:9000
2. 默认用户名/密码：admin / admin
3. 首次登录会强制修改密码
4. 生成 Token：右上角用户头像 → My Account → Security → Generate Token
   └── 记录此 Token，后续 Scanner 配置需要使用
```

> ⚠️ **生产环境安全配置**：
> - 必须修改默认密码
> - 使用 HTTPS（反向代理 Nginx 或直接配置）
> - 配置防火墙限制 9000 端口访问范围
> - 定期备份 PostgreSQL 数据库
> - 升级前必须阅读官方升级指南，跨版本升级需逐版本迁移

---

## 4. 项目创建与分析配置

### 4.1 在 SonarQube 中创建项目

```text
登录 SonarQube → Administration → Projects → Create Project
                    │
                    ▼
        选择项目创建方式:
        ┌────────────────────┬────────────────────┐
        │  Manual（手动创建）  │  Automatic（自动）   │
        │ · 设置 project key  │ · 支持扫描时自动创建  │
        │ · 设置 display name │ · 适合 CI/CD 自动化  │
        │ · 设置 visibility   │                     │
        └────────────────────┴────────────────────┘
                    │
                    ▼
        生成分析 Token（或复用已有 Token）
                    │
                    ▼
        选择分析方式 → 获取操作指引
```

### 4.2 项目配置参数

| 参数 | 说明 | 示例 |
|------|------|------|
| **Project Key** | 项目唯一标识，不可修改 | `com.example:my-service` |
| **Display Name** | 显示名称，可修改 | `My Service` |
| **Visibility** | 可见性：`Public` 或 `Private` | `Private` |
| **Main Branch** | 主分支名称，默认 `main` | `main` |
| **New Code Definition** | 新代码定义方式 | `Previous version` 或 `Number of days` |

### 4.3 SonarScanner CLI 分析方式

```bash
# 安装 SonarScanner CLI
# 下载地址：https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/

# 运行分析
sonar-scanner \
  -Dsonar.projectKey=com.example:my-service \
  -Dsonar.sources=src \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=sqp_xxxxxxxxxxxxxxxxxx
```

---

## 5. Quality Profiles 质量配置档案

### 5.1 概念

Quality Profile（质量配置档案）是**规则集合**——定义了项目使用哪些代码分析规则、每个规则的严重等级。

```text
Quality Profile
│
├── 规则 1: S107（方法参数不能超过 7 个）  → 严重等级: Critical
├── 规则 2: S1444（public static 字段不应可修改） → 严重等级: Major
├── 规则 3: S2077（SQL 应参数化）          → 严重等级: Blocker
└── ...
```

### 5.2 内置 Profile

| Profile 名称 | 语言 | 说明 |
|-------------|------|------|
| **Sonar way** | 所有语言 | SonarQube 推荐的内置 profile，覆盖大部分常用规则 |
| **Sonar way (Recommended)** | Java | Java 语言推荐配置，包含所有必须规则 |
| **Sonar way + Security** | Java | 在 Sonar way 基础上增加安全相关规则 |

> 💡 **"Sonar way"是默认 Profile**，新项目创建时自动关联。对大多数团队，直接使用"Sonar way"已经足够。

### 5.3 自定义 Profile

创建自定义 Profile 的场景：

```text
┌──────────────────────────────────────────────────────────────┐
│ 何时需要自定义 Quality Profile？                              │
├──────────────────────────────────────────────────────────────┤
│ 1. 团队有自己的编码规范（如阿里巴巴 Java 开发手册）           │
│ 2. 需要禁用某些不适用于项目的规则（如遗留代码大量触发某规则）   │
│ 3. 需要调整规则严重等级（如将某些 Major 提升为 Critical）      │
│ 4. 需要继承官方 Profile 但做微调                              │
│ 5. 使用了 SonarQube 未内置的自定义规则包                      │
└──────────────────────────────────────────────────────────────┘
```

操作路径：

```text
Administration → Quality Profiles
    │
    ├── Create Profile
    │   ├── Name: [My Team Java Standard]
    │   ├── Language: [Java]
    │   └── Parent: [Sonar way]  ← 继承自官方 Profile
    │
    ├── 在自定义 Profile 中：
    │   ├── 激活/取消特定规则
    │   ├── 修改规则的严重等级
    │   └── 设置规则参数（如方法最大行数）
    │
    └── 将项目关联到自定义 Profile
```

### 5.4 规则继承机制

```text
Sonar way（官方内置）
      │  parent
      ▼
My Team Java Profile
      │  parent
      ▼
My Service Profile（微调）
```

规则继承规则：
- 子 Profile **自动包含** 父 Profile 的所有激活规则
- 子 Profile 可以 **覆盖** 父 Profile 中规则的严重等级
- 子 Profile 可以 **额外激活** 父 Profile 中未激活的规则
- 父 Profile 更新时，子 Profile 自动继承变更（除非子 Profile 已显式覆盖）

---

## 6. Quality Gates 质量门禁

### 6.1 默认 "Sonar way" 质量门禁

SonarQube 内置的 "Sonar way" 质量门禁是最常用的门禁标准：

| 条件 | 指标 | 阈值 | 作用 |
|------|------|------|------|
| 1 | **Coverage on New Code** | `< 80%` | 新代码覆盖率低于 80% 则失败 |
| 2 | **Duplicated Lines on New Code** | `> 3%` | 新代码重复率超过 3% 则失败 |
| 3 | **Maintainability Rating on New Code** | `> A` | 新代码可维护性评级低于 A 则失败 |
| 4 | **Reliability Rating on New Code** | `> A` | 新代码可靠性评级低于 A 则失败 |
| 5 | **Security Rating on New Code** | `> A` | 新代码安全评级低于 A 则失败 |
| 6 | **Security Hotspots Reviewed on New Code** | `= 0` | 新代码中存在未审查的安全热点则失败（部分版本） |

### 6.2 自定义质量门禁

```text
Administration → Quality Gates → Create
    │
    ├── Gate Name: [My Service Quality Gate]
    │
    ├── Conditions（条件列表）：
    │   ├── On Overall Code:
    │   │   ├── Bugs = 0              （不允许有 Bug）
    │   │   ├── Vulnerabilities = 0   （不允许有漏洞）
    │   │   └── Coverage < 60%        （整体覆盖率不低于 60%）
    │   │
    │   └── On New Code（推荐）：
    │       ├── Coverage < 85%        （新代码覆盖率不低于 85%）
    │       ├── Duplicated Lines > 3%（新代码重复率不超过 3%）
    │       └── Code Smells > 0       （不允许引入新 Code Smell）
    │
    └── 关联项目 → 生效
```

### 6.3 质量门禁条件详解

| 条件类型 | 指标 | 说明 | 可用计算方式 |
|---------|------|------|------------|
| **Reliability** | `bugs` | Bug 数量 | WARNING, ERROR |
| | `reliability_rating` | 可靠性评级（A=1, B=2, C=3, D=4, E=5） | GT（大于） |
| **Security** | `vulnerabilities` | 漏洞数量 | WARNING, ERROR |
| | `security_rating` | 安全评级 | GT |
| | `security_hotspots_reviewed` | 安全热点审查状态 | WARNING, ERROR |
| **Maintainability** | `code_smells` | 代码异味数量 | WARNING, ERROR |
| | `sqale_index` | 技术债务（以分钟计） | GT |
| | `maintainability_rating` | 可维护性评级 | GT |
| | `new_maintainability_rating` | 新代码可维护性评级 | GT |
| **Coverage** | `coverage` | 覆盖率百分比 | LT（小于） |
| | `new_coverage` | 新代码覆盖率百分比 | LT |
| | `uncovered_lines` | 未覆盖行数 | GT |
| | `new_uncovered_lines` | 新代码未覆盖行数 | GT |
| **Duplications** | `duplicated_lines_density` | 重复行密度百分比 | GT |
| | `new_duplicated_lines_density` | 新代码重复行密度百分比 | GT |

> 🎯 **推荐策略**：仅对 New Code 设置严格门禁，对 Overall Code 设置宽松门禁。这符合 "Clean as You Code" 方法论，循序渐进地提升总体质量。

### 6.4 质量门禁的 CI 反馈

```yaml
# 质量门禁通过/失败时 CI 的表现
PASS: ✅ Quality Gate passed
      → CI Pipeline 继续，可合并 PR

FAIL: ❌ Quality Gate failed
      → CI Pipeline 失败（失败 stage）
      → PR 显示 Check 未通过
      → 阻止合并（需配置分支保护规则）
      → 发送通知到钉钉/企微/邮件
```

---

## 7. sonar-project.properties 完整参考

### 7.1 基础配置

```properties
# === 项目标识 ===
sonar.projectKey=com.example:my-service
sonar.projectName=My Service
sonar.projectVersion=1.0.0

# === 源码目录 ===
sonar.sources=src/main/java
sonar.tests=src/test/java
sonar.java.binaries=target/classes
sonar.java.test.binaries=target/test-classes

# === 排除文件 ===
sonar.exclusions=**/generated/**/*.java,**/model/*.java
sonar.test.exclusions=**/test/**/*.java
sonar.coverage.exclusions=**/config/**/*.java,**/dto/**/*.java

# === SonarQube Server 连接 ===
sonar.host.url=http://localhost:9000
sonar.login=sqp_xxxxxxxxxxxxxxxxxxxxxxxxx

# === 语言 ===
sonar.language=java

# === 编码 ===
sonar.sourceEncoding=UTF-8
```

### 7.2 多模块项目配置

```properties
# === 项目标识 ===
sonar.projectKey=com.example:multi-module
sonar.projectName=Multi Module Project
sonar.projectVersion=1.0.0

# === 模块定义 ===
sonar.modules=common,api,service,web

# === 全局源码 ===
sonar.sources=src/main/java
sonar.tests=src/test/java
sonar.sourceEncoding=UTF-8

# === 各模块独立配置（可选） ===
# common 模块
common.sonar.projectName=Multi Module - Common
common.sonar.sources=src/main/java
common.sonar.java.binaries=common/target/classes

# api 模块
api.sonar.projectName=Multi Module - API
api.sonar.sources=src/main/java
api.sonar.java.binaries=api/target/classes

# service 模块
service.sonar.projectName=Multi Module - Service
service.sonar.sources=src/main/java
service.sonar.java.binaries=service/target/classes
```

### 7.3 高级配置

```properties
# === 分析历史 ===
sonar.projectDate=2026-07-26       # 设置分析日期（重分析历史数据时用）
sonar.analysis.mode=publish        # publish | preview（preview 模式不推送结果到 server）

# === 覆盖率 ===
sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
sonar.coverage.exclusions=**/config/**/*.java,**/dto/**/*.java,**/vo/**/*.java
sonar.java.coveragePlugin=jacoco

# === 重复代码 ===
sonar.cpd.exclusions=**/generated/**/*.java,**/*DTO.java

# === 质量门禁 ===
sonar.qualitygate.wait=true          # 等待质量门禁结果再退出
sonar.qualitygate.timeout=300        # 等待超时（秒）

# === Issue 管理 ===
sonar.issues.ignore.multicriteria-effort=true

# === 分支分析（Developer 版+） ===
# 通常由 CI 环境变量自动设置，手动指定：
sonar.branch.name=feature/new-func
sonar.branch.target=main
sonar.branch.type=SHORT   # SHORT（短分支）| LONG（长分支）

# === Debug 模式 ===
sonar.verbose=true
sonar.log.level=DEBUG
```

### 7.4 排除规则的高级用法

```properties
# 按文件或目录排除特定规则
sonar.issue.ignore.multicriteria=e1,e2
sonar.issue.ignore.multicriteria.e1.ruleKey=java:S1118
sonar.issue.ignore.multicriteria.e1.resourceKey=**/*Application.java
sonar.issue.ignore.multicriteria.e2.ruleKey=java:S1135
sonar.issue.ignore.multicriteria.e2.resourceKey=**/*.java
```

---

## 8. Maven 集成

### 8.1 配置方式

在项目 `pom.xml` 中添加：

```xml
<!-- pom.xml 中的 plugin 配置 -->
<build>
    <plugins>
        <plugin>
            <groupId>org.sonarsource.scanner.maven</groupId>
            <artifactId>sonar-maven-plugin</artifactId>
            <version>4.0.0.4121</version>
        </plugin>
    </plugins>
</build>
```

### 8.2 运行分析

```bash
# 基本使用（需要提前编译）
mvn clean compile sonar:sonar

# 带连接信息
mvn sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=sqp_xxxxxxxxxxxx

# 完整参数（推荐）
mvn clean verify sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=sqp_xxxxxxxxxxxx \
  -Dsonar.projectKey=com.example:my-service \
  -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml

# 等待质量门禁结果
mvn sonar:sonar \
  -Dsonar.qualitygate.wait=true \
  -Dsonar.qualitygate.timeout=300
```

> 💡 **执行顺序很重要**：`mvn clean verify sonar:sonar` 确保先编译、测试、生成覆盖率报告，再执行 SonarQube 分析。

### 8.3 在 settings.xml 中配置 Server

```xml
<!-- ~/.m2/settings.xml 或 CI 使用的 settings.xml -->
<settings>
    <pluginGroups>
        <pluginGroup>org.sonarsource.scanner.maven</pluginGroup>
    </pluginGroups>
    <profiles>
        <profile>
            <id>sonar</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <sonar.host.url>http://localhost:9000</sonar.host.url>
                <sonar.login>sqp_xxxxxxxxxxxxxxxx</sonar.login>
            </properties>
        </profile>
    </profiles>
</settings>
```

配置完成后，只需执行：

```bash
mvn clean verify sonar:sonar
```

---

## 9. Gradle 集成

### 9.1 配置方式

**`build.gradle`（Groovy DSL）：**

```groovy
plugins {
    id 'java'
    id 'jacoco'
    id 'org.sonarqube' version '5.1.0.4882'
}

sonar {
    properties {
        property 'sonar.host.url', 'http://localhost:9000'
        property 'sonar.login', 'sqp_xxxxxxxxxxxxxxxx'
        property 'sonar.projectKey', 'com.example:my-service'
        property 'sonar.projectName', 'My Service'
        property 'sonar.sources', 'src/main/java'
        property 'sonar.tests', 'src/test/java'
        property 'sonar.java.binaries', 'build/classes/java/main'
        property 'sonar.coverage.jacoco.xmlReportPaths',
            'build/reports/jacoco/test/jacocoTestReport.xml'
    }
}

// 确保测试和覆盖率报告先生成
test {
    useJUnitPlatform()
    finalizedBy jacocoTestReport
}

jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true
        html.required = true
    }
}
```

**`build.gradle.kts`（Kotlin DSL）：**

```kotlin
plugins {
    java
    jacoco
    id("org.sonarqube") version "5.1.0.4882"
}

sonar {
    properties {
        property("sonar.host.url", "http://localhost:9000")
        property("sonar.login", "sqp_xxxxxxxxxxxxxxxx")
        property("sonar.projectKey", "com.example:my-service")
        property("sonar.projectName", "My Service")
        property("sonar.sources", "src/main/java")
        property("sonar.tests", "src/test/java")
        property("sonar.java.binaries", "build/classes/java/main")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "build/reports/jacoco/test/jacocoTestReport.xml"
        )
    }
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}
```

### 9.2 运行分析

```bash
# 完整分析流程
./gradlew clean test jacocoTestReport sonar

# 仅执行 Sonar 分析（假设已经编译和测试过）
./gradlew sonar

# 带参数覆盖
./gradlew sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=sqp_xxxxxxxxxxxx
```

---

## 10. CI/CD 流水线集成

### 10.1 GitHub Actions 集成

```yaml
# .github/workflows/sonar-analysis.yml
name: SonarQube Analysis

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]
    types: [opened, synchronize, reopen]

jobs:
  build-and-analyze:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4
        with:
          # 需要完整 git 历史用于 SonarQube 的新代码分析
          fetch-depth: 0

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'maven'

      - name: Cache SonarQube packages
        uses: actions/cache@v4
        with:
          path: ~/.sonar/cache
          key: ${{ runner.os }}-sonar
          restore-keys: ${{ runner.os }}-sonar

      - name: Build and analyze with Maven
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
        run: |
          mvn clean verify sonar:sonar \
            -Dsonar.host.url=${{ env.SONAR_HOST_URL }} \
            -Dsonar.login=${{ env.SONAR_TOKEN }} \
            -Dsonar.qualitygate.wait=true \
            -Dsonar.qualitygate.timeout=300

      - name: SonarQube Quality Gate Check
        run: |
          echo "SonarQube analysis completed. Check the dashboard for details."
```

> ⚠️ **关键点**：`fetch-depth: 0` 必不可少——SonarQube 需要完整的 Git 历史来计算 New Code 周期。

### 10.2 Jenkins Pipeline 集成

```groovy
// Jenkinsfile
pipeline {
    agent any

    environment {
        SONAR_HOST_URL = 'http://sonarqube.example.com:9000'
        SONAR_TOKEN    = credentials('sonarqube-token')
    }

    tools {
        maven 'maven-3.9'
        jdk 'jdk-17'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                sh 'mvn clean verify'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        mvn sonar:sonar \
                            -Dsonar.host.url=${SONAR_HOST_URL} \
                            -Dsonar.login=${SONAR_TOKEN}
                    '''
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
    }

    post {
        success {
            echo '✅ Quality Gate passed!'
        }
        failure {
            echo '❌ Quality Gate failed. Check SonarQube dashboard.'
        }
    }
}
```

> 💡 **Jenkins 集成提示**：安装 `SonarQube Scanner for Jenkins` 插件，在 Jenkins 系统配置中配置 SonarQube Server，然后在 Pipeline 中使用 `withSonarQubeEnv` 和 `waitForQualityGate`。

### 10.3 GitLab CI 集成

```yaml
# .gitlab-ci.yml
image: maven:3.9-eclipse-temurin-17

variables:
  SONAR_HOST_URL: "http://sonarqube.example.com:9000"
  SONAR_TOKEN: "$SONAR_TOKEN"  # 在 CI/CD Variables 中设置

stages:
  - build
  - test
  - analyze

build:
  stage: build
  script:
    - mvn compile

test:
  stage: test
  script:
    - mvn test
  artifacts:
    paths:
      - target/site/jacoco/

sonarqube-analysis:
  stage: analyze
  script:
    - mvn sonar:sonar
      -Dsonar.host.url=$SONAR_HOST_URL
      -Dsonar.login=$SONAR_TOKEN
      -Dsonar.qualitygate.wait=true
      -Dsonar.qualitygate.timeout=300
  only:
    - main
    - develop
    - merge_requests
```

---

## 11. 分析参数全表

### 11.1 核心连接参数

| 参数 | 必填 | 说明 | 示例值 |
|------|:----:|------|--------|
| `sonar.host.url` | ✅ | SonarQube 服务器 URL | `http://localhost:9000` |
| `sonar.login` | ✅ | 认证 Token 或用户名 | `sqp_xxxxxxxx` 或 `admin` |
| `sonar.password` | 条件必填 | 当 login 是用户名时的密码 | `my_password` |
| `sonar.projectKey` | ✅ | 项目唯一标识 | `com.example:my-service` |
| `sonar.projectName` | ❌ | 项目显示名 | `My Service` |
| `sonar.projectVersion` | ❌ | 项目版本号 | `1.0.0` |

### 11.2 源码与编译参数

| 参数 | 必填 | 说明 | 示例值 |
|------|:----:|------|--------|
| `sonar.sources` | ✅ | 源码目录（逗号分隔） | `src/main/java` |
| `sonar.tests` | ❌ | 测试代码目录 | `src/test/java` |
| `sonar.java.binaries` | ✅ | 编译后的 class 文件目录 | `target/classes` |
| `sonar.java.test.binaries` | ❌ | 测试 class 文件目录 | `target/test-classes` |
| `sonar.java.libraries` | ❌ | 依赖 jar 包路径 | `target/dependency/*.jar` |
| `sonar.java.source` | ❌ | Java 版本 | `17` |
| `sonar.java.target` | ❌ | 编译目标版本 | `17` |
| `sonar.sourceEncoding` | ❌ | 源码编码 | `UTF-8` |

### 11.3 排除与过滤参数

| 参数 | 说明 | 示例值 |
|------|------|--------|
| `sonar.exclusions` | 排除文件（支持通配符） | `**/generated/**/*.java` |
| `sonar.test.exclusions` | 排除测试文件 | `**/test/**/*.java` |
| `sonar.coverage.exclusions` | 排除被统计覆盖率的文件 | `**/config/**/*.java` |
| `sonar.cpd.exclusions` | 排除重复代码检测的文件 | `**/generated/**/*.java` |

### 11.4 覆盖率参数

| 参数 | 说明 | 示例值 |
|------|------|--------|
| `sonar.coverage.jacoco.xmlReportPaths` | JaCoCo XML 报告路径 | `target/site/jacoco/jacoco.xml` |
| `sonar.java.coveragePlugin` | 覆盖率插件 | `jacoco` |
| `sonar.coverage.exclusions` | 排除覆盖率统计的文件 | `**/dto/**/*.java` |

### 11.5 质量门禁参数

| 参数 | 说明 | 示例值 |
|------|------|--------|
| `sonar.qualitygate.wait` | 是否等待质量门禁结果 | `true` |
| `sonar.qualitygate.timeout` | 等待超时（秒） | `300` |

### 11.6 分支分析参数（Developer 版+）

| 参数 | 说明 | 示例值 |
|------|------|--------|
| `sonar.branch.name` | 当前分支名 | `feature/xxx` |
| `sonar.branch.target` | 目标分支（PR 场景） | `main` |
| `sonar.branch.type` | 分支类型 | `SHORT` / `LONG` |

### 11.7 问题管理参数

| 参数 | 说明 | 示例值 |
|------|------|--------|
| `sonar.issue.ignore.multicriteria` | 按规则+文件排除问题的配置 | `e1,e2` |
| `sonar.verbose` | 是否输出详细日志 | `true` |
| `sonar.log.level` | 日志级别 | `DEBUG` / `INFO` / `WARN` |

---

## 12. New Code Period 与 Clean as You Code

### 12.1 New Code Period

New Code Period（新代码周期）是 SonarQube 区分"新代码"和"旧代码"的时间窗口。所有质量门禁条件都可以**仅针对新代码**进行判定。

**新代码定义的三种方式：**

| 定义方式 | 配置路径 | 说明 | 适用场景 |
|---------|---------|------|---------|
| **Number of days** | 项目设置 → New Code → Days | 最近 N 天内的代码变更 | 通用，简单易懂 |
| **Previous version** | 项目设置 → New Code → Version | 自上一个版本分支点以来的变更 | 按版本发布节奏的团队 |
| **Reference branch** | 项目设置 → New Code → Reference | 与指定分支的差异 | Developer 版+，与分支分析结合 |

```text
时间轴示例（Number of days = 30）：
───────────────────────────────────────────────▶
                            ↑ 30天前          今天
                    旧代码区域    │       新代码区域
                    不触发 Gate  │       触发 Gate
                    仅供历史参考  │       严格检查
```

### 12.2 Clean as You Code 方法论

> 🎯 **Clean as You Code（边编码边清理）** 是 SonarQube 倡导的核心方法论——**不要试图一次性解决所有遗留质量问题，而是确保新编写的代码是干净的。**

**核心理念：**

```text
传统方式：                             Clean as You Code：
━━━━━━━━━━━━━━━━━━━━━━━━━━           ━━━━━━━━━━━━━━━━━━━━━━━━━━

目标：一次性消除所有问题              目标：每次提交只加干净代码
结果：团队被淹没在大量修复中          结果：持续改善，积小胜为大胜
风险：修复引入新 Bug                  风险：极低
效果：往往半途而废                    效果：数月后整体质量自然提升
```

**实践步骤：**

1. **设置新代码周期**（推荐 30 天或基于版本）
2. **严格的质量门禁仅针对新代码**
3. **在 Code Review 中重点关注新代码质量**
4. **修改旧代码文件时，顺便修复该文件的已有问题**（"童子军规则"——离开时比来时更干净）

### 12.3 New Code 在 SonarQube UI 中的体现

```text
Dashboard 面板:
┌──────────────────────────────────────────────────┐
│  Quality Gate: PASS (New Code)                    │
│                                                   │
│  Reliability:   A  (0 Bugs)  [1 New Bug]         │
│  Security:      A  (0 Vuln.)  [0 New Vuln.]      │
│  Maintainability:A  (50 Smells) [3 New Smells]   │
│  Coverage:      72%  [+5%]    [85% New Code]     │
│  Duplications:  4.2% [+0.1%]  [1.5% New Code]    │
│                                                   │
│  ┌─────────────┐    ┌───────────────────────────┐│
│  │ Overall Code │    │     New Code              ││
│  │ （历史数据）  │    │  （严格红线，Check 重点）  ││
│  └─────────────┘    └───────────────────────────┘│
└──────────────────────────────────────────────────┘
```

---

## 13. Dashboard 解读

### 13.1 主面板概览

```text
┌──────────────────────────────────────────────────────────────┐
│  ☰ Project Dashboard                                  [日期] │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  质量门禁: ✅ Pass (New Code)                        │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐   │
│  │  Bugs  │ │  Vuln  │ │  Code  │ │Coverage│ │ Dupl.  │   │
│  │    3   │ │    0   │ │  Smells│ │  72%   │ │  4.2%  │   │
│  │   Rel: │ │   Sec: │ │   250  │ │   -5%  │ │ +0.1%  │   │
│  │    A   │ │    A   │ │ Maint: │ │        │ │       │   │
│  └────────┘ └────────┘ └────────┘ └────────┘ └────────┘   │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  ⚡ 技术债务: 2d 15h  |  比率: 3.2%  |  评级: A    │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌──────────────┐   ┌──────────────┐                        │
│  │  Issues 趋势  │   │  覆盖率趋势   │                        │
│  │  （折线图）    │   │  （折线图）   │                        │
│  └──────────────┘   └──────────────┘                        │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Issues 分解（按类型 × 严重等级）                      │    │
│  │ ┌────────────┬───┬───┬───┬───┬───┐                  │    │
│  │ │            │B  │C  │M  │m  │I  │                  │    │
│  │ │Bug         │ 0 │ 1 │ 2 │ 0 │ 0 │                  │    │
│  │ │Vulnerability│ 0 │ 0 │ 0 │ 0 │ 0 │                  │    │
│  │ │Code Smell  │ 0 │ 3 │ 40│ 180│ 27│                  │    │
│  │ │Security    │ 1 │ 0 │ 2 │ 0 │ 0 │                  │    │
│  │ │Hotspot     │   │   │   │   │   │                  │    │
│  └────────────┴───┴───┴───┴───┴───┘                  │    │
│  (B=Blocker, C=Critical, M=Major, m=Minor, I=Info)   │    │
└──────────────────────────────────────────────────────────────┘
```

### 13.2 五个核心指标详解

| 指标 | 图标 | 含义 | 评级颜色 | 对应安全等级 |
|------|------|------|---------|------------|
| **Bugs** | 🐛 | 代码中明确的不正确行为，运行时极可能出问题 | A(绿)/B(黄)/C(橙)/D(红)/E(深红) | Reliability |
| **Vulnerabilities** | 🔒 | 安全漏洞，可能被外部攻击利用 | A(绿)/B(黄)/C(橙)/D(红)/E(深红) | Security |
| **Code Smells** | 👃 | 代码异味，不影响运行但降低可维护性 | A(绿)/B(黄)/C(橙)/D(红)/E(深红) | Maintainability |
| **Coverage** | 🎯 | 测试覆盖率，测试覆盖的代码行百分比 | 百分比值（>=80% 为绿色） | - |
| **Duplications** | 📋 | 重复代码密度，重复行占总代码行百分比 | 百分比值（<3% 为绿色） | - |

### 13.3 评级系统

```text
评级        颜色        说明                    阈值
A (1.0)     🟢 绿色     良好                    0 Bug / 0 Vulnerability
B (2.0)     🟡 黄色     轻微风险                至少 1 个 Minor Issue
C (3.0)     🟠 橙色     中等风险                至少 1 个 Major Issue
D (4.0)     🔴 红色     显著风险                至少 1 个 Critical Issue
E (5.0)     🔴 深红     高风险                  至少 1 个 Blocker Issue
```

### 13.4 Issues 趋势图解读

趋势图展示项目质量随时间的变化：

```text
Issues 数量
   ▲
   │        ← 新项目引入大量代码
   │    *****
   │   *     *
   │  *       *    ← 修复工作开始
   │ *         ****
   │*              *
   │                *** ← 稳定期
   +──────────────────────────▶ 时间
        初始阶段    治理期    稳定期
```

> 💡 **趋势比绝对值更重要**：即使当前有较多的 Issues，只要趋势是下降的，说明团队在持续改善。

---

## 14. Security Hotspots 安全热点

### 14.1 什么是 Security Hotspot

Security Hotspot（安全热点）是 SonarQube 中的一种特殊 Issue 类型，与常规的 Bug/Vulnerability 不同：

```text
┌────────────────────────────────────────────────────────────┐
│  Security Hotspot = 需要人工审查的"安全敏感代码段"          │
│                                                            │
│  · 不是确定的漏洞（Vulnerability）                          │
│  · 但代码中使用了安全的敏感操作，需要人工确认是否正确       │
│  · 不触发 Quality Gate 失败（但会要求全部审查完毕）         │
│  · 分类：高风险 / 中风险 / 低风险                          │
└────────────────────────────────────────────────────────────┘
```

### 14.2 常见 Hotspot 类型（Java）

| Hotspot 规则 | 说明 | 风险等级 | 检查要点 |
|-------------|------|---------|---------|
| `S2068` | 硬编码密码（Credentials） | 高风险 | 确认密码不是真实凭据 |
| `S2078` | 创建了可执行的 LDAP 查询 | 高风险 | 确认 LDAP 注入防护 |
| `S2076` | OS 命令注入 | 高风险 | 确认输入经过严格校验 |
| `S2083` | 路径遍历（File Path） | 高风险 | 确认路径经过白名单验证 |
| `S2245` | 使用不安全的随机数生成器 | 中风险 | 确认非安全场景 |
| `S3330` | Cookie 未设置 HttpOnly | 中风险 | 确认不包含敏感数据 |
| `S4787` | 加密算法使用 | 中风险 | 确认使用强加密算法 |
| `S4790` | 哈希算法使用 | 中风险 | 确认使用加盐哈希 |

### 14.3 Security Review 工作流

```text
开发者或 Security Team 收到 Hotspot 通知
            │
            ▼
打开 SonarQube → Security Hotspots Tab
            │
            ▼
审查 Hotspot 代码：
┌──────────────────────────────────────────────────────┐
│ 审查 Checklist:                                      │
│  □ 这段代码是否真的需要执行敏感操作？                 │
│  □ 输入是否经过充分校验和转义？                       │
│  □ 是否有现有的安全控制失效？                         │
│  □ 是否有更安全的替代方案？                           │
│  □ 是否通过了安全团队的额外审查？                     │
└──────────────────────────────────────────────────────┘
            │
            ▼
标记结果：
┌──────────────────────────────────────────────────────┐
│  ✅ Reviewed and Fixed   — 已修复的问题              │
│  ✅ Reviewed and Safe    — 审查确认安全，无需修改    │
│  🕐 Reviewed and Accepted Risk — 已评估风险，接受   │
└──────────────────────────────────────────────────────┘
            │
            ▼
未审查的 Hotspot 会显示在 Dashboard 的计数中
Quality Gate 要求：Security Hotspots Reviewed = 0（全部审查完毕）
```

> 🎯 **安全审查是团队责任**：不一定要安全团队来做——Feature Team 的开发者最了解业务上下文，通常他们是审查 Hotspot 的最佳人选。

---

## 15. JaCoCo 覆盖率集成

### 15.1 JaCoCo Maven 配置

```xml
<!-- pom.xml -->
<build>
    <plugins>
        <!-- JaCoCo 覆盖率插件 -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.12</version>
            <executions>
                <execution>
                    <id>prepare-agent</id>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>

        <!-- SonarQube 插件 -->
        <plugin>
            <groupId>org.sonarsource.scanner.maven</groupId>
            <artifactId>sonar-maven-plugin</artifactId>
            <version>4.0.0.4121</version>
        </plugin>
    </plugins>
</build>
```

### 15.2 完整分析命令

```bash
# 完整的 Maven 构建 + 测试 + 覆盖率报告 + SonarQube 分析
mvn clean verify sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=sqp_xxxxxxxx \
  -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
```

### 15.3 JaCoCo 覆盖率指标

| 指标 | 说明 | SonarQube 中的指标名 |
|------|------|-------------------|
| **Line Coverage** | 覆盖的行数 / 总行数 | `coverage` |
| **Branch Coverage** | 覆盖的分支数 / 总分指数 | `branch_coverage` |
| **Method Coverage** | 覆盖的方法数 / 总方法数 | - |
| **Class Coverage** | 覆盖的类数 / 总类数 | - |

### 15.4 覆盖率数据流

```text
mvn test
   │
   ▼
JaCoCo 在测试执行时插桩收集数据 → 生成 jacoco.exec（二进制）
   │
   ▼
jacoco:report goal → 生成 jacoco.xml（XML 格式报告）
   │
   ▼
sonar:sonar → 读取 jacoco.xml → 将覆盖率数据发送到 SonarQube
   │
   ▼
SonarQube Dashboard 显示覆盖率指标 + 代码行级覆盖高亮
```

### 15.5 覆盖率目标建议

| 代码类型 | 最低覆盖率 | 建议覆盖率 | 说明 |
|---------|:--------:|:---------:|------|
| **业务逻辑层 (Service)** | 80% | 90%+ | 核心业务逻辑，需要高覆盖 |
| **控制器层 (Controller)** | 60% | 80% | 与 Web 框架耦合，Mock 成本高 |
| **数据访问层 (Repository/DAO)** | 50% | 75% | 集成测试覆盖为主 |
| **配置类 (Config)** | 0% | - | 通常无需覆盖 |
| **DTO/POJO** | 0% | - | 通常无需覆盖（除非有业务逻辑） |
| **工具类 (Util)** | 80% | 90% | 通用工具，影响面广 |
| **异常处理** | 50% | 80% | 调用链路中易被忽略的路径 |

---

## 16. SonarQube API 自动化

### 16.1 API 基础

SonarQube 提供了丰富的 REST API，可以用于自动化集成：

```bash
# API 基础 URL
GET http://localhost:9000/api

# 查看所有可用的 API 端点
GET http://localhost:9000/api/webservices/list
```

### 16.2 常用 API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/projects/search` | 搜索项目 |
| `POST` | `/api/projects/create` | 创建项目 |
| `POST` | `/api/projects/delete` | 删除项目 |
| `GET` | `/api/measures/component` | 获取项目指标 |
| `GET` | `/api/issues/search` | 搜索问题 |
| `POST` | `/api/issues/assign` | 分配问题 |
| `POST` | `/api/hotspots/edit_comment` | 添加 Hotspot 评论 |
| `GET` | `/api/qualitygates/list` | 列出质量门禁 |
| `GET` | `/api/qualitygates/project_status` | 获取项目质量门禁状态 |
| `GET` | `/api/qualityprofiles/search` | 搜索质量配置 |
| `POST` | `/api/qualityprofiles/add_project` | 关联 Profile 到项目 |
| `GET` | `/api/ce/activity` | 查看分析任务历史 |

### 16.3 API 调用示例

```bash
# 使用 Token 认证调用 API
# 获取项目质量门禁状态
curl -u sqp_xxxxxxxxx: \
  "http://localhost:9000/api/qualitygates/project_status?projectKey=com.example:my-service"

# 获取项目覆盖率
curl -u sqp_xxxxxxxxx: \
  "http://localhost:9000/api/measures/component?component=com.example:my-service&metricKeys=coverage,branch_coverage,line_coverage"

# 列出项目所有待处理问题
curl -u sqp_xxxxxxxxx: \
  "http://localhost:9000/api/issues/search?componentKeys=com.example:my-service&resolved=false&severities=BLOCKER,CRITICAL"

# 获取分析任务状态
curl -u sqp_xxxxxxxxx: \
  "http://localhost:9000/api/ce/activity?component=com.example:my-service&status=IN_PROGRESS"
```

### 16.4 API 响应示例

```json
// GET /api/qualitygates/project_status?projectKey=com.example:my-service
{
  "projectStatus": {
    "status": "ERROR",
    "conditions": [
      {
        "status": "OK",
        "metricKey": "new_reliability_rating",
        "comparator": "GT",
        "errorThreshold": "1",
        "actualValue": "1"
      },
      {
        "status": "ERROR",
        "metricKey": "new_coverage",
        "comparator": "LT",
        "errorThreshold": "80",
        "actualValue": "72.5"
      }
    ]
  }
}

// GET /api/measures/component?component=...&metricKeys=coverage,duplicated_lines_density
{
  "component": {
    "key": "com.example:my-service",
    "measures": [
      {
        "metric": "coverage",
        "value": "78.5"
      },
      {
        "metric": "duplicated_lines_density",
        "value": "3.2"
      }
    ]
  }
}
```

### 16.5 CI 脚本中的 API 调用

```bash
#!/bin/bash
# wait-for-quality-gate.sh
# 在 CI 中等待并检查 Quality Gate 结果

PROJECT_KEY=$1
SONAR_HOST_URL=$2
SONAR_TOKEN=$3
TIMEOUT=${4:-300}  # 默认 5 分钟
INTERVAL=10        # 每 10 秒检查一次

elapsed=0
while [ $elapsed -lt $TIMEOUT ]; do
    response=$(curl -s -u "$SONAR_TOKEN": \
        "$SONAR_HOST_URL/api/qualitygates/project_status?projectKey=$PROJECT_KEY")

    status=$(echo "$response" | jq -r '.projectStatus.status')

    if [ "$status" = "OK" ]; then
        echo "✅ Quality Gate PASSED"
        exit 0
    elif [ "$status" = "ERROR" ]; then
        echo "❌ Quality Gate FAILED"
        echo "$response" | jq '.projectStatus.conditions'
        exit 1
    fi

    sleep $INTERVAL
    elapsed=$((elapsed + INTERVAL))
done

echo "⏰ Timeout: Quality Gate did not complete within ${TIMEOUT}s"
exit 1
```

---

## 17. 常见问题与实战经验

### 17.1 扫描失败排查

| 错误 | 常见原因 | 解决方案 |
|------|---------|---------|
| `Project not found` | Project Key 未在 SonarQube 上创建 | 手动创建项目或使用 `-Dsonar.scm.provider=git` 自动创建 |
| `You're not authorized` | Token 无效或无权限 | 重新生成 Token，确认有项目的分析权限 |
| `java.lang.OutOfMemoryError` | 分析大项目内存不足 | 加大 Scanner JVM 内存：`SONAR_SCANNER_OPTS=-Xmx4g` |
| `Missing blame information` | Git 历史不完整 | `fetch-depth: 0` 确保完整克隆 |
| `No data found` | 分析结果未上传成功 | 检查网络连接，确认 `sonar.host.url` 可达 |
| `Java binaries not found` | 缺少编译后的 class | 先生成 class：`mvn compile` |
| `Coverage data not imported` | JaCoCo 报告路径错误 | 确认 `sonar.coverage.jacoco.xmlReportPaths` 正确 |

### 17.2 Scanner JVM 内存优化

```bash
# 大项目（>100万行代码）需要增大 Scanner 内存
export SONAR_SCANNER_OPTS="-Xmx4g -XX:+UseG1GC"

# Maven Sonar 分析内存
export MAVEN_OPTS="-Xmx4g -XX:+UseG1GC"
mvn clean verify sonar:sonar ...

# 或者在 pom.xml 中配置
# <sonar.scanner.javaOpts>-Xmx4g</sonar.scanner.javaOpts>
```

### 17.3 误报管理（False Positives）

> ⚠️ **误报是静态分析工具的固有特性**。管理好误报可以提高团队对工具的信任度。

```text
发现一个"误报"（False Positive）
      │
      ▼
分析：真的是误报吗？
      │
      ├── 是误报 → 在 SonarQube UI 中标记为 "False Positive"
      │            并添加说明：为什么这是误报
      │
      └── 不是误报 → 修复代码
                     或者如果无法立即修复 → 标记为 "Won't Fix" + 原因

标记误报的最佳实践：
  · 在 Issue 中添加评论，说明判定为误报的详细依据
  · 不轻易标记 —— 先确认自己理解规则
  · 定期审查被标记为 False Positive 的 Issue
  · 考虑更新规则配置而不是标记误报
```

### 17.4 大项目优化策略

| 策略 | 说明 | 效果 |
|------|------|------|
| **增量分析** | 限制分析范围，跳过未变更的模块 | 分析时间减少 50-80% |
| **排除生成代码** | 不要分析自动生成的代码 | 分析时间减少 10-30% |
| **增加 Scanner 内存** | 避免 OOM | 稳定分析过程 |
| **多模块并行分析** | 在 CI 中并行分析独立的模块 | 整体分析时间大幅缩短 |
| **只分析 New Code** | 使用 `sonar.analysis.mode=issues` + `sonar.issue.ignore.multicriteria` | 仅分析新增代码 |
| **SonarQube 服务端优化** | 增加 Compute Engine 线程数、更大的数据库连接池 | 提高服务端处理能力 |

### 17.5 配置建议总结

```yaml
# 生产环境 SonarQube 配置建议
sonar:
  # 性能
  scanner:
    javaOpts: "-Xmx4g"
    forcedVersion: "5.0.1.5006"

  # 排除不需要分析的内容
  exclusions:
    - "**/generated/**"
    - "**/build/**"
    - "**/target/**"
    - "**/node_modules/**"
    - "**/vendor/**"
    - "**/*.min.js"
    - "**/*.min.css"

  # 覆盖率排除（这些通常不需要测试覆盖）
  coverageExclusions:
    - "**/*Config.java"
    - "**/*Application.java"
    - "**/dto/**"
    - "**/vo/**"
    - "**/enums/**"
    - "**/model/**"
```

---

## 18. SonarQube vs SonarCloud vs SonarLint

### 18.1 三剑客对比

| 维度 | **SonarLint** | **SonarQube** | **SonarCloud** |
|------|:------------:|:------------:|:--------------:|
| **定位** | IDE 实时检查客户端 | 自托管代码质量平台 | 云端代码质量平台（SaaS） |
| **部署** | IDE 插件 | 自建服务器 | 无需部署，登录即用 |
| **服务器** | 不需要 | 需要自行维护 | SonarSource 托管 |
| **分析时机** | 编码时（实时） | CI/CD 触发 | CI/CD 触发 |
| **反馈速度** | 毫秒级 | 分钟级 | 分钟级 |
| **数据持久化** | 无（不保存分析历史） | 有（完整历史追踪） | 有（完整历史追踪） |
| **Quality Gate** | 不支持 | 支持 | 支持 |
| **趋势分析** | 不支持 | 支持 | 支持 |
| **团队协作** | 单机使用 | 团队共享 | 团队共享 |
| **维护成本** | 零 | 高（服务器运维） | 零 |
| **数据安全** | 完全本地 | 完全自控 | 数据在 SonarSource 云端 |
| **定价** | 免费 | 社区版免费，高级版付费 | 公共仓库免费，私有仓库有限免费 |
| **连接模式** | 可连 SonarQube/Cloud | 作为 Server 被连接 | 作为 Server 被连接 |

### 18.2 协同工作流

```text
SonarLint  +  SonarQube/SonarCloud  +  CI Pipeline
  │                   │                       │
  │  Connected Mode   │                       │
  ├──────────────────▶│                       │
  │ 同步规则配置       │                       │
  │ 获取项目上下文      │                       │
  │ 标记新代码问题      │                       │
  │                   │                       │
  ▼                   │                       ▼
IDE 实时检查           │                   Build 时全量分析
高亮问题               │                   质量门禁检查
快速修复               │                   发送通知
                      ▼
                  Dashboard 展示
                  历史趋势追踪
                  技术债务管理
```

### 18.3 选型决策

```text
你的团队情况？
      │
      ├── 个人开发者/学习
      │     → SonarLint 就够了（零成本，零运维）
      │
      ├── 小型团队（<10人），有基础设施能力
      │     → SonarLint + SonarQube Community
      │
      ├── 中型团队（10-50人）
      │     → SonarLint + SonarQube Developer（需要分支分析）
      │
      ├── 不想维护服务器
      │     → SonarLint + SonarCloud（公共仓库免费）
      │
      ├── 大型企业，多项目组合管理
      │     → SonarLint + SonarQube Enterprise
      │
      └── 金融/安全敏感行业，数据不能出内网
            → SonarLint + SonarQube（自托管，Enterprise/Data Center）
```

---

> 🎯 **总结：SonarQube 是企业级代码质量管理的核心枢纽。** 从架构理解到部署安装，从质量门禁到 CI/CD 集成，从 Dashboard 解读到 API 自动化——掌握 SonarQube 是高级工程师的必备技能。配合 SonarLint 的 IDE 实时检查和正确的 Clean as You Code 方法论，可以构建一个可持续演进的质量保障体系。

> 💡 **推荐实践路径**：
> 1. 先用 Docker 搭建 SonarQube 体验环境
> 2. 在自己的项目中运行一次 `mvn sonar:sonar`
> 3. 理解 Dashboard 上的各项指标
> 4. 配置适合自己团队的 Quality Gate
> 5. 集成到 CI/CD 流水线
> 6. 逐步推广到全团队
