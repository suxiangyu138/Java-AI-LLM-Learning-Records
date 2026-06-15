# Maven 企业级 Web 应用

## 一、企业级 Maven 架构

### 1.1 核心价值

| 维度 | Maven 解决方案 |
|------|---------------|
| **依赖管理** | 三级仓库体系 + GAV 坐标 + 自动传递 |
| **标准化** | 统一目录结构 + 统一生命周期 |
| **多模块** | 父 POM 统一管理 + 子模块继承 |
| **多环境** | Profile 实现 dev/test/prod 切换 |
| **质量管控** | 集成测试、覆盖率、代码规范检查 |
| **CI/CD** | 标准化命令，适配 Jenkins/GitLab CI |

### 1.2 企业级多模块结构

```
project-parent/                     # 父项目（pom）
├── pom.xml                         # 依赖版本 + 插件统一管理
├── project-common/                 # 公共模块（工具类、实体、常量）
├── project-dao/                    # 数据访问层
├── project-service/                # 业务逻辑层
├── project-api/                    # REST API 模块
└── project-gateway/                # 网关模块（Spring Cloud）
```

## 二、父 POM 完整配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" ...>
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.company.project</groupId>
    <artifactId>project-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <!-- 子模块声明 -->
    <modules>
        <module>project-common</module>
        <module>project-dao</module>
        <module>project-service</module>
        <module>project-api</module>
    </modules>

    <!-- 全局版本变量 -->
    <properties>
        <java.version>1.8</java.version>
        <spring-boot.version>2.7.10</spring-boot.version>
        <spring-cloud.version>2021.0.5</spring-cloud.version>
        <mysql.version>8.0.33</mysql.version>
        <mybatis.version>3.5.13</mybatis.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <!-- 统一依赖版本管理 -->
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
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>mysql</groupId>
                <artifactId>mysql-connector-java</artifactId>
                <version>${mysql.version}</version>
            </dependency>
            <!-- 内部模块版本 -->
            <dependency>
                <groupId>com.company.project</groupId>
                <artifactId>project-common</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <!-- 统一插件管理 -->
    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.8.1</version>
                    <configuration>
                        <source>1.8</source>
                        <target>1.8</target>
                        <encoding>UTF-8</encoding>
                    </configuration>
                </plugin>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot.version}</version>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <version>3.1.2</version>
                </plugin>
                <plugin>
                    <groupId>org.jacoco</groupId>
                    <artifactId>jacoco-maven-plugin</artifactId>
                    <version>0.8.10</version>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

## 三、多环境配置（Profile）

```xml
<profiles>
    <profile>
        <id>dev</id>
        <activation><activeByDefault>true</activeByDefault></activation>
        <properties><env>dev</env></properties>
    </profile>
    <profile>
        <id>test</id>
        <properties><env>test</env></properties>
    </profile>
    <profile>
        <id>prod</id>
        <properties><env>prod</env></properties>
    </profile>
</profiles>

<build>
    <resources>
        <resource>
            <directory>src/main/resources</directory>
            <filtering>true</filtering>
            <includes>
                <include>application.yml</include>
                <include>application-${env}.yml</include>
            </includes>
        </resource>
    </resources>
</build>
```

```bash
# 切换环境打包
mvn clean package -Pprod -DskipTests
```

## 四、配置文件分离（生产部署）

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-resources-plugin</artifactId>
    <version>3.3.0</version>
    <executions>
        <execution>
            <id>copy-config</id>
            <phase>package</phase>
            <goals><goal>copy-resources</goal></goals>
            <configuration>
                <outputDirectory>${project.build.directory}/config</outputDirectory>
                <resources>
                    <resource>
                        <directory>src/main/resources</directory>
                        <includes><include>application.yml</include></includes>
                    </resource>
                </resources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

```bash
# 启动时指定外部配置
java -jar target/api.jar --spring.config.location=target/config/application.yml
```

## 五、依赖冲突排查

```bash
# 查看依赖树
mvn dependency:tree

# 查看冲突
mvn dependency:tree -Dincludes=org.springframework:spring-core

# 分析无用依赖
mvn dependency:analyze
```

```xml
<!-- 排除冲突 -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework</groupId>
            <artifactId>spring-core</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

## 六、CI/CD 集成

```groovy
// Jenkinsfile
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        stage('Package') {
            steps {
                sh 'mvn clean package -Pprod -DskipTests'
            }
        }
        stage('Deploy') {
            steps {
                sh 'mvn deploy -DskipTests'
            }
        }
    }
}
```

## 七、构建优化

| 策略 | 命令/配置 | 说明 |
|------|----------|------|
| **跳过测试** | `-DskipTests` | 紧急部署时使用 |
| **并行构建** | `-T 4` | 多模块并行 `mvn clean package -T 4` |
| **离线模式** | `-o` | 不从远程下载（需本地已有依赖） |
| **增量编译** | 默认 | 仅编译变更文件 |
| **构建缓存** | Gradle 特性 | Maven 可配置 `maven-build-cache-extension` |
| **国内镜像** | settings.xml | 阿里云镜像加速下载 |

## 八、最佳实践总结

| 类别 | 实践 |
|------|------|
| **依赖管理** | 父 POM dependencyManagement 统一版本，BOM 导入管理全家桶 |
| **版本规范** | 严格语义化版本，SNAPSHOT 仅用于开发，生产只用 RELEASE |
| **多环境** | Profile + filtering 实现配置切换，敏感信息走配置中心 |
| **测试** | 单元测试 + 集成测试分离，覆盖率 ≥ 80% |
| **打包** | 配置文件分离，瘦包策略，排除冗余依赖 |
| **部署** | Deploy 到私有仓库，禁止本地直接部署到生产 |
| **协同** | settings.xml 统一，Maven Wrapper 锁定版本 |
