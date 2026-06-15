# Maven 版本管理

## 一、版本命名规范

### 1.1 语义化版本（Semantic Versioning）

格式：`主版本号.次版本号.修订号`（X.Y.Z）

| 版本位 | 递增条件 | 示例 |
|--------|---------|------|
| **主版本号（X）** | 不兼容的 API 变更 | 1.0.0 → 2.0.0 |
| **次版本号（Y）** | 新增功能，保持兼容 | 1.0.0 → 1.1.0 |
| **修订号（Z）** | Bug 修复，无新功能 | 1.0.0 → 1.0.1 |

### 1.2 版本后缀标识

| 后缀 | 状态 | 使用场景 |
|------|------|---------|
| `SNAPSHOT` | 开发中，不稳定 | 开发环境，允许自动更新到最新快照 |
| `ALPHA` | 内部测试版 | 团队内部初步验证 |
| `BETA` | 公开测试版 | 测试团队/产品团队验证 |
| `RC`（Release Candidate） | 候选版 | 正式版前最后一轮测试 |
| `RELEASE` | 稳定正式版 | **生产环境唯一允许使用的版本** |

### 1.3 版本演进示例

```
1.0.0-SNAPSHOT     → 开发阶段
1.0.0-ALPHA1       → 内部测试
1.0.0-BETA1        → 公开测试
1.0.0-RC1          → 候选发布
1.0.0-RELEASE      → 正式发布
1.0.1-SNAPSHOT     → 修复 bug，下一轮开发
1.0.1-RELEASE      → 修复版正式发布
1.1.0-SNAPSHOT     → 新增功能
```

## 二、项目版本配置

### 2.1 单模块项目

```xml
<groupId>com.company.project</groupId>
<artifactId>demo-web</artifactId>
<version>1.0.0-SNAPSHOT</version>
```

### 2.2 多模块版本统一

父项目统一版本，子模块通过 `<parent>` 继承：

```xml
<!-- 父 POM -->
<groupId>com.company.project</groupId>
<artifactId>project-parent</artifactId>
<version>1.0.0-SNAPSHOT</version>
<packaging>pom</packaging>

<!-- 子 POM（无需配置 groupId 和 version）-->
<parent>
    <groupId>com.company.project</groupId>
    <artifactId>project-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
</parent>
<artifactId>user-service</artifactId>
```

## 三、依赖版本管理

### 3.1 dependencyManagement 统一管理

**父 POM 声明版本，子模块不写 version**：

```xml
<!-- 父 POM -->
<dependencyManagement>
    <dependencies>
        <!-- 导入 Spring Boot BOM -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>2.7.10</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <!-- 自定义版本管理 -->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.33</version>
        </dependency>
        <dependency>
            <groupId>com.company.project</groupId>
            <artifactId>common</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- 子 POM（无需写 version）-->
<dependencies>
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
    </dependency>
    <dependency>
        <groupId>com.company.project</groupId>
        <artifactId>common</artifactId>
    </dependency>
</dependencies>
```

### 3.2 版本变量

```xml
<properties>
    <spring-boot.version>2.7.10</spring-boot.version>
    <mysql.version>8.0.33</mysql.version>
    <mybatis.version>3.5.13</mybatis.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>${mysql.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## 四、版本仲裁规则

当出现依赖版本冲突时，Maven 按以下优先级选择版本：

| 优先级 | 规则 | 说明 |
|:------:|------|------|
| 1 | **dependencyManagement 声明** | 强制使用声明版本 |
| 2 | **最短路径优先** | 依赖路径短的版本优先 |
| 3 | **声明优先** | 同路径长度，先声明的优先 |

## 五、版本自动更新（maven-version-plugin）

```xml
<!-- pom.xml 配置插件 -->
<build>
    <plugins>
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>versions-maven-plugin</artifactId>
            <version>2.16.0</version>
        </plugin>
    </plugins>
</build>
```

```bash
# 批量更新所有模块版本
mvn versions:set -DnewVersion=1.0.1-SNAPSHOT

# 确认更新
mvn versions:commit

# 回滚更新
mvn versions:revert
```

## 六、版本部署规范

### 6.1 仓库与版本对应

| 仓库类型 | 存储版本 | 说明 |
|---------|---------|------|
| 本地仓库 | SNAPSHOT + RELEASE | 开发者本地缓存 |
| 快照仓库（Snapshots） | SNAPSHOT | 开发/测试环境 |
| 正式仓库（Releases） | RELEASE | **生产环境** |

### 6.2 部署配置

```xml
<distributionManagement>
    <repository>
        <id>company-releases</id>
        <name>企业正式仓库</name>
        <url>http://nexus.company.com/repository/maven-releases/</url>
    </repository>
    <snapshotRepository>
        <id>company-snapshots</id>
        <name>企业快照仓库</name>
        <url>http://nexus.company.com/repository/maven-snapshots/</url>
    </snapshotRepository>
</distributionManagement>
```

## 七、最佳实践

| 实践 | 说明 |
|------|------|
| **版本格式统一** | 全团队严格遵循 `X.Y.Z-后缀` 格式 |
| **生产环境仅用 RELEASE** | 禁止 SNAPSHOT/ALPHA/BETA/RC 进入生产 |
| **父 POM 统一版本** | 多模块项目通过 dependencyManagement 集中管理 |
| **版本变量集中定义** | 在 `<properties>` 中统一管理关联版本 |
| **SNAPSHOT 强制更新** | `mvn clean install -U` 强制拉取最新快照 |
| **Git tag 对应版本** | `v1.0.0-RELEASE` 标签与 Maven 版本严格对应 |
| **RELEASE 不可覆盖** | 已部署的 RELEASE 版本不可修改或删除 |

## 八、常见问题

| 问题 | 解决方案 |
|------|---------|
| 依赖版本冲突 | `mvn dependency:tree` → dependencyManagement 统一版本 |
| SNAPSHOT 不更新 | `mvn clean install -U` 强制更新 |
| RELEASE 部署 409 错误 | 版本已存在，递增版本号后重新部署 |
| 子模块版本未同步 | 确认 `<parent>` 版本，使用 `versions:set` 批量更新 |
| `${project.version}` 不生效 | 检查变量定义位置，需在 `<properties>` 之前或通过 Maven 内置变量 |
