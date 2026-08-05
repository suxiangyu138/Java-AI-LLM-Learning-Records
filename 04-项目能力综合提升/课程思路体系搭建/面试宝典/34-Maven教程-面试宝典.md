# Maven教程 面试宝典
> 基于Maven教程课程大纲，全面覆盖Maven面试高频考点

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

### Q1: 什么是Maven？Maven的核心作用是什么？
**A:** Maven是一个基于POM（Project Object Model）的项目管理和构建自动化工具。核心作用：
- **依赖管理**: 统一管理第三方jar包及其传递依赖
- **项目构建**: 提供标准化的构建生命周期（编译、测试、打包、部署）
- **项目信息管理**: 管理项目描述、开发者列表、版本等元数据
- **统一项目结构**: 标准化项目目录结构

### Q2: Maven的坐标（Coordinates）是什么？
**A:** Maven坐标唯一标识一个项目或依赖，由三部分组成：
```xml
<groupId>com.example</groupId>        <!-- 组织/公司域名反写 -->
<artifactId>my-app</artifactId>       <!-- 项目/模块名称 -->
<version>1.0.0</version>             <!-- 版本号 -->
<packaging>jar</packaging>           <!-- 打包方式（可选，默认jar） -->
```
坐标四要素：groupId、artifactId、version、packaging

### Q3: Maven仓库的类型和关系？
**A:**
| 仓库类型 | 位置 | 说明 |
|----------|------|------|
| 本地仓库 | ~/.m2/repository/ | 本地缓存，默认位置 |
| 中央仓库 | https://repo.maven.apache.org/maven2/ | Maven官方维护 |
| 私服（Nexus） | 公司内网 | 企业私有仓库，代理中央仓库 |
| 远程仓库 | 配置的第三方仓库 | 如阿里云镜像、JitPack |

查找顺序：**本地仓库 → 私服/远程仓库 → 中央仓库**

### Q4: Maven的依赖范围（Scope）有哪些？
**A:**
| Scope | 编译 | 测试 | 运行 | 示例 |
|-------|------|------|------|------|
| compile（默认） | Y | Y | Y | spring-core |
| provided | Y | Y | N | servlet-api（Tomcat提供） |
| runtime | N | Y | Y | mysql-connector-java |
| test | N | Y | N | junit |
| system | Y | Y | N | 本机jar（需systemPath） |
| import | - | - | - | BOM导入（仅dependencyManagement） |

### Q5: 什么是依赖传递？如何解决依赖冲突？
**A:** 依赖传递是指项目中引入的依赖可能又会依赖其他jar包，Maven会自动下载所有传递依赖。

**依赖冲突解决原则**:
1. **短路优先（Nearest Definition）**: 路径最短的依赖优先
   ```
   A → B → C → logback 1.2.0      (路径深度3)
   A → logback 1.3.0                (路径深度1) → 优先使用
   ```
2. **先声明优先（First Declaration）**: 路径等长时，先声明的优先

**解决冲突**:
```xml
<!-- 排除传递依赖 -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>service-a</artifactId>
    <exclusions>
        <exclusion>
            <groupId>log4j</groupId>
            <artifactId>log4j</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- 直接声明期望版本 -->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.3.0</version>
</dependency>
```

### Q6: Maven的生命周期有哪些阶段？
**A:** Maven有三套生命周期，相互独立：

1. **clean生命周期**:
   - `pre-clean` → `clean`（删除target） → `post-clean`

2. **default生命周期（核心）**:
   ```
   validate → compile → test → package → verify → install → deploy
   ```
   常用命令：`mvn compile`、`mvn test`、`mvn package`、`mvn install`、`mvn deploy`

3. **site生命周期**:
   - `pre-site` → `site`（生成站点文档） → `post-site` → `site-deploy`

**执行原则**: 执行某个阶段时，其之前的所有阶段都会被执行。

### Q7: Maven常用构建命令有哪些？
**A:**
```bash
mvn clean              # 清理target目录
mvn compile            # 编译源码到target/classes
mvn test               # 运行测试（需先compile）
mvn package            # 打包（jar/war）
mvn install            # 安装到本地仓库
mvn deploy             # 部署到远程仓库/私服
mvn clean install      # 清理并安装（最常用）
mvn clean package -DskipTests  # 跳过测试打包
mvn install -P prod    # 使用prod profile
mvn dependency:tree    # 查看依赖树
```

### Q8: 什么是SNAPSHOT版本和Release版本？
**A:**
| 特性 | SNAPSHOT | Release |
|------|----------|---------|
| 标识 | `1.0.0-SNAPSHOT` | `1.0.0` |
| 可变性 | 可重复覆盖 | 不可变 |
| 更新频率 | 每次构建都尝试获取最新 | 下载后缓存 |
| 使用场景 | 开发调试阶段 | 正式发布 |
| 私服策略 | 允许覆盖 | 不允许覆盖同一版本 |

### Q9: 什么是Maven插件？常用插件有哪些？
**A:** Maven本身只提供生命周期框架，具体功能由插件实现。
```xml
<build>
    <plugins>
        <!-- 编译插件 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <source>17</source>
                <target>17</target>
                <encoding>UTF-8</encoding>
            </configuration>
        </plugin>
        <!-- 测试插件 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.1.0</version>
        </plugin>
        <!-- 打包插件 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-jar-plugin</artifactId>
            <version>3.3.0</version>
        </plugin>
    </plugins>
</build>
```

### Q10: 什么是Maven聚合（Aggregation）？
**A:** 聚合（多模块构建）通过父POM聚合多个子模块，实现一次命令构建所有模块：
```xml
<!-- 父POM -->
<groupId>com.example</groupId>
<artifactId>parent-project</artifactId>
<version>1.0.0</version>
<packaging>pom</packaging>

<modules>
    <module>common</module>
    <module>dao</module>
    <module>service</module>
    <module>web</module>
</modules>
```
执行 `mvn clean install` 在父模块，所有子模块按顺序构建。

### Q11: 什么是Maven继承（Inheritance）？
**A:** 继承让子模块共享父POM的配置，避免重复：
```xml
<!-- 子模块POM -->
<parent>
    <groupId>com.example</groupId>
    <artifactId>parent-project</artifactId>
    <version>1.0.0</version>
    <relativePath>../pom.xml</relativePath>
</parent>
```
子模块自动继承：dependencies、plugins、properties、pluginManagement等。

### Q12: Maven私服（Nexus）的作用？
**A:**
- **代理中央仓库**: 加速下载，避免外网请求
- **管理内部构件**: 存放内部共享jar包
- **版本管控**: 管理SNAPSHOT和Release版本
- **权限管理**: 控制仓库访问权限
- **高可用**: 局域网内高速访问

Nexus仓库类型：
| 仓库类型 | 作用 | 示例 |
|----------|------|------|
| hosted | 内部私有仓库 | releases、snapshots |
| proxy | 代理远程仓库 | aliyun-proxy、central-proxy |
| group | 聚合多个仓库 | public（包含hosted+proxy） |

### Q13: Maven的settings.xml主要配置什么？
**A:** settings.xml位于 `~/.m2/settings.xml`，全局配置：
```xml
<settings>
    <!-- 本地仓库位置 -->
    <localRepository>D:/maven/repository</localRepository>
    
    <!-- 代理镜像 -->
    <mirrors>
        <mirror>
            <id>aliyun</id>
            <mirrorOf>central</mirrorOf>
            <name>阿里云公共仓库</name>
            <url>https://maven.aliyun.com/repository/public</url>
        </mirror>
    </mirrors>
    
    <!-- 私服认证 -->
    <servers>
        <server>
            <id>nexus-releases</id>
            <username>admin</username>
            <password>admin123</password>
        </server>
    </servers>
    
    <!-- JDK版本 -->
    <profiles>
        <profile>
            <id>jdk-17</id>
            <activation><activeByDefault>true</activeByDefault></activation>
            <jdk>17</jdk>
        </profile>
    </profiles>
</settings>
```

### Q14: 如何跳过Maven测试？
**A:** 三种方式：
```bash
# 1. 命令行参数
mvn package -DskipTests          # 跳过测试编译和执行
mvn package -Dmaven.test.skip=true  # 跳过测试编译和执行（更彻底）

# 2. POM配置
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <skipTests>true</skipTests>
    </configuration>
</plugin>

# 3. IDEA界面
# 点击Maven面板中的 "Skip Tests" 按钮
```

### Q15: 什么是BOM（Bill of Materials）？
**A:** BOM用于统一管理依赖版本，解决微服务/多模块项目中的版本一致性：
```xml
<!-- BOM POM -->
<project>
    <groupId>com.example</groupId>
    <artifactId>spring-boot-dependencies</artifactId>
    <version>3.1.0</version>
    <packaging>pom</packaging>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-web</artifactId>
                <version>3.1.0</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```
```xml
<!-- 使用BOM的项目 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>3.1.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- 无需指定版本 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

---

## 二、深度原理剖析（8-12题）

### Q1: Maven依赖传递和冲突解决的算法原理？
**A:** Maven使用深度优先遍历算法计算依赖树：

**依赖传递规则**:
1. 从根项目开始DFS遍历所有依赖
2. 每个依赖形成一条路径到项目根节点
3. 根据scope传递性合并依赖
4. 使用最短路径原则选择版本

**Scope传递规则**:
```
compile → compile（传递）
compile → runtime（传递）
compile → test（不透传）
provided → provided（不透传）
test → test（不透传）
```

**冲突解决**:
```bash
# 查看依赖树
mvn dependency:tree

# 输出示例：
com.example:my-app:jar:1.0.0
├── org.springframework:spring-core:jar:5.3.20 (compile)
├── com.fasterxml.jackson:jackson-core:jar:2.14.0 (compile)
│   └── com.fasterxml.jackson:jackson-annotations:jar:2.14.0 (compile)
└── com.example:service-client:jar:1.0.0 (compile)
    └── com.fasterxml.jackson:jackson-core:jar:2.13.0 (compile) → 被覆盖
```

### Q2: Maven的生命周期和插件执行机制？
**A:** Maven的生命周期阶段与插件的goal绑定执行：
- 每个生命周期阶段对应一个或多个插件目标（Plugin Goal）
- 插件目标在特定阶段执行，通过 `phase` 和 `goal` 配置
- 同一阶段可以有多个插件目标，按声明顺序执行

```xml
<!-- 插件绑定示例 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <executions>
        <execution>
            <id>default-test</id>
            <phase>test</phase>           <!-- 绑定到test阶段 -->
            <goals>
                <goal>test</goal>         <!-- 执行test目标 -->
            </goals>
        </execution>
    </executions>
</plugin>
```

内置绑定示例：
| 生命周期阶段 | 插件:目标 | 打包类型 |
|-------------|-----------|----------|
| compile | maven-compiler-plugin:compile | jar/war |
| test | maven-surefire-plugin:test | jar/war |
| package | maven-jar-plugin:jar | jar |
| package | maven-war-plugin:war | war |
| install | maven-install-plugin:install | jar/war/war |

### Q3: Maven和Gradle的核心区别？
**A:**
| 维度 | Maven | Gradle |
|------|-------|--------|
| 配置语言 | XML（声明式） | Groovy/Kotlin DSL（编程式） |
| 构建速度 | 较慢（无增量编译） | 快（增量编译 + 构建缓存） |
| 灵活性 | 低（固定生命周期） | 高（自定义task和依赖图） |
| 学习曲线 | 平缓 | 中等 |
| 依赖管理 | POM + 坐标 | 类似，支持动态版本 |
| 多项目构建 | 聚合+继承 | 多project配置 |
| 性能优化 | 依赖多线程 | 并行构建 + 增量构建 |
| 社区生态 | 成熟，插件丰富 | 增长迅速 |
| 配置文件 | pom.xml | build.gradle / build.gradle.kts |
| 推荐场景 | 传统Java/Spring项目 | Android、高性能构建场景 |

### Q4: Maven的构建缓存机制？
**A:**
- **本地仓库缓存**: 下载的依赖存储在 `~/.m2/repository/`，再次构建不会重新下载
- **SNAPSHOT更新策略**: 
  - 默认24小时检查一次更新
  - `mvn -U` 强制更新所有SNAPSHOT
  - 可在settings.xml配置更新策略
- **构建产物缓存**: `mvn install` 将构件安装到本地仓库，其他项目可引用
- **增量编译**: Maven编译器插件支持增量编译，仅重新编译变更的文件

```xml
<!-- 配置SNAPSHOT更新策略 -->
<repository>
    <id>nexus-snapshots</id>
    <url>http://nexus:8081/repository/maven-snapshots/</url>
    <snapshots>
        <enabled>true</enabled>
        <updatePolicy>always</updatePolicy>  <!-- always/daily/interval:X/never -->
    </snapshots>
</repository>
```

### Q5: Maven的profiles多环境配置原理？
**A:** profiles允许根据不同环境（开发/测试/生产）配置不同的构建参数：
```xml
<profiles>
    <!-- 开发环境 -->
    <profile>
        <id>dev</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
            <env>dev</env>
            <db.url>jdbc:mysql://localhost:3306/dev</db.url>
            <db.username>root</db.username>
            <db.password>root</db.password>
        </properties>
    </profile>
    
    <!-- 生产环境 -->
    <profile>
        <id>prod</id>
        <properties>
            <env>prod</env>
            <db.url>jdbc:mysql://prod-db:3306/prod</db.url>
            <db.username>prod_user</db.username>
            <db.password>${env.DB_PASSWORD}</db.password>
        </properties>
    </profile>
</profiles>
```
资源文件中的占位符：
```properties
# src/main/resources/application-${env}.properties
db.url=${db.url}
db.username=${db.username}
db.password=${db.password}
```
```bash
# 构建时指定profile
mvn clean package -P prod
```

### Q6: Nexus私服的仓库分类和管理策略？
**A:**
```
Nexus仓库架构：

                   +-------------+
                   |  开发者     |
                   +------+------+
                          |
                          v
                   +------+------+
         +--------->  Public    <----------+
         |         | (Group)    |          |
         |         +------+------+         |
         |                |                |
         v                v                v
  +------+------+  +------+------+  +------+------+
  | Releases    |  | Snapshots  |  | Aliyun      |
  | (Hosted)    |  | (Hosted)   |  | Proxy       |
  +------+------+  +------+------+  +------+------+
         |                |                |
         v                v                v
  +------+------+  +------+------+  +------+------+
  | 内部发布组件 |  | 开发中组件  |  | 阿里云代理  |
  +-------------+  +-------------+  +------+------+
                                              |
                                              v
                                       +------+------+
                                       | Maven Central |
                                       +-------------+
```

**管理策略**:
- Releases: 只允许上传一次，不允许覆盖
- Snapshots: 允许覆盖，支持多版本
- Public Group: 聚合所有仓库，开发者只需配置一个URL
- Proxy: 缓存远程仓库的jar包

### Q7: Maven的内置属性和变量引用机制？
**A:**
```xml
<!-- 内置属性 -->
${basedir}              <!-- 项目根目录 -->
${project.build.directory} <!-- target目录 -->
${project.build.sourceEncoding} <!-- 源码编码 -->
${project.groupId}      <!-- 项目groupId -->
${project.artifactId}   <!-- 项目artifactId -->
${project.version}      <!-- 项目版本号 -->

<!-- 自定义属性 -->
<properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <spring.version>6.0.0</spring.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<!-- 引用自定义属性 -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-core</artifactId>
    <version>${spring.version}</version>
</dependency>

<!-- 使用settings.xml属性 -->
${settings.localRepository}
${settings.interactiveMode}

<!-- 环境变量 -->
${env.JAVA_HOME}
${env.M2_HOME}
```

### Q8: Maven的Archetype（原型）生成原理？
**A:** Archetype是Maven的项目模板工具：
1. 从远程仓库下载archetype描述文件（archetype-metadata.xml）
2. 根据archetype生成项目骨架
3. 替换变量（如artifactId、package名）
4. 创建标准目录结构
```bash
# 使用archetype创建项目
mvn archetype:generate \
    -DarchetypeGroupId=org.apache.maven.archetypes \
    -DarchetypeArtifactId=maven-archetype-quickstart \
    -DarchetypeVersion=1.4 \
    -DgroupId=com.example \
    -DartifactId=my-app \
    -Dversion=1.0.0
```

---

## 三、实战场景题（6-10题）

### Q1: 如何处理Maven依赖下载失败的问题？
**A:**
```bash
# 场景：依赖下载失败（jar包损坏/网络问题）

# 方案1：删除本地缓存重新下载
# 找到出错的jar包位置并删除
rm -rf ~/.m2/repository/com/example/artifact/1.0.0/
# 或使用脚本批量清理
find ~/.m2/repository -name "*.lastUpdated" -delete

# 方案2：强制更新SNAPSHOT
mvn clean install -U

# 方案3：检查网络和仓库配置
# 检查settings.xml中的mirror配置
# 配置阿里云镜像
<mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <url>https://maven.aliyun.com/repository/public</url>
</mirror>

# 方案4：手动安装jar到本地仓库
mvn install:install-file \
    -Dfile=path/to/jar.jar \
    -DgroupId=com.example \
    -DartifactId=my-lib \
    -Dversion=1.0.0 \
    -Dpackaging=jar
```

### Q2: 配置IDEA集成Maven的最佳实践？
**A:**
```
IDEA Maven配置步骤：

1. File → Settings → Build, Execution, Deployment → Build Tools → Maven
   ├── Maven home path: D:/apache-maven-3.9.0
   ├── User settings file: D:/apache-maven-3.9.0/conf/settings.xml
   └── Local repository: D:/maven/repository

2. Runner配置
   ├── VM Options: -Xmx2048m
   └── Skip tests: 按需勾选

3. Importing配置
   ├── JDK for importer: 17
   ├── Sources encoding: UTF-8
   └── Import Maven projects automatically: 勾选

4. 快捷键
   ├── Ctrl+Shift+O: 重新加载Maven项目
   └── Ctrl+Shift+P: 分析依赖冲突

5. 常用操作（Maven面板）
   ├── clean: 清理
   ├── install: 安装
   ├── package: 打包
   ├── ⚡图标: Skip Tests
   └── 🗘图标: Reimport All Projects
```

### Q3: 如何从零搭建多模块Maven项目？
**A:**
```bash
# 项目结构
multi-module-project/
├── pom.xml                    # 父POM（聚合+继承）
├── common/                    # 公共模块
│   ├── pom.xml
│   └── src/main/java/...
├── dao/                       # 数据访问模块
│   ├── pom.xml
│   └── src/main/java/...
├── service/                   # 业务逻辑模块
│   ├── pom.xml
│   └── src/main/java/...
└── web/                       # Web接口模块
    ├── pom.xml
    └── src/main/java/...
```

**父POM配置**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.example</groupId>
    <artifactId>multi-module-parent</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    
    <!-- 聚合模块 -->
    <modules>
        <module>common</module>
        <module>dao</module>
        <module>service</module>
        <module>web</module>
    </modules>
    
    <!-- 统一版本管理 -->
    <properties>
        <java.version>17</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring.boot.version>3.1.0</spring.boot.version>
        <mybatis.version>3.5.13</mybatis.version>
        <lombok.version>1.18.28</lombok.version>
    </properties>
    
    <!-- 统一依赖管理（不直接引入） -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring.boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.mybatis</groupId>
                <artifactId>mybatis</artifactId>
                <version>${mybatis.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <!-- 所有子模块共有依赖 -->
    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

**子模块（service）POM**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>multi-module-parent</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>service</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>dao</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
            <!-- 版本由父POM的dependencyManagement管理 -->
        </dependency>
    </dependencies>
</project>
```

### Q4: 部署jar包到Nexus私服（手动和自动方式）？
**A:**
```bash
# 方式1：手动上传（Nexus Web界面）
# 浏览器访问 http://nexus:8081
# 选择仓库 → Upload → 选择jar包和POM文件

# 方式2：命令行上传
mvn deploy:deploy-file \
    -DgroupId=com.example \
    -DartifactId=my-lib \
    -Dversion=1.0.0 \
    -Dpackaging=jar \
    -Dfile=my-lib.jar \
    -Durl=http://nexus:8081/repository/maven-releases/ \
    -DrepositoryId=nexus-releases

# 方式3：Maven自动deploy
# 配置distributionManagement
<distributionManagement>
    <repository>
        <id>nexus-releases</id>
        <name>Nexus Release Repository</name>
        <url>http://nexus:8081/repository/maven-releases/</url>
    </repository>
    <snapshotRepository>
        <id>nexus-snapshots</id>
        <name>Nexus Snapshot Repository</name>
        <url>http://nexus:8081/repository/maven-snapshots/</url>
    </snapshotRepository>
</distributionManagement>
```
```bash
# 执行deploy
mvn clean deploy -P prod

# settings.xml配置认证
<server>
    <id>nexus-releases</id>
    <username>admin</username>
    <password>admin123</password>
</server>
```

### Q5: Spring Boot项目依赖冲突排查和解决？
**A:**
```bash
# 场景：项目启动报ClassNotFoundException或NoSuchMethodError

# 1. 查看依赖树
mvn dependency:tree > tree.txt

# 2. 搜索特定jar包
mvn dependency:tree -Dincludes=com.fasterxml.jackson

# 3. 分析冲突
# 使用IDEA Maven Helper插件
# POM文件 → Dependency Analyzer → Conflicts

# 4. 常见冲突场景和解决
# 场景1：日志框架冲突
# Spring Boot使用SLF4J+Logback，排除其他日志实现
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-log4j2</artifactId>
</dependency>

# 场景2：Jackson版本冲突
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>

# 5. 查看实际生效的版本
mvn help:effective-pom > effective-pom.xml
```

### Q6: 配置多环境Profile实现开发/测试/生产分离？
**A:**
```xml
<project>
    <!-- 多环境配置 -->
    <profiles>
        <profile>
            <id>dev</id>
            <activation><activeByDefault>true</activeByDefault></activation>
            <properties>
                <profile.name>dev</profile.name>
                <db.url>jdbc:mysql://localhost:3306/dev_db</db.url>
            </properties>
        </profile>
        <profile>
            <id>test</id>
            <properties>
                <profile.name>test</profile.name>
                <db.url>jdbc:mysql://test-db:3306/test_db</db.url>
            </properties>
        </profile>
        <profile>
            <id>prod</id>
            <properties>
                <profile.name>prod</profile.name>
                <db.url>jdbc:mysql://prod-db:3306/prod_db</db.url>
            </properties>
        </profile>
    </profiles>
    
    <!-- 资源过滤配置 -->
    <build>
        <resources>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>true</filtering>
                <includes>
                    <include>**/*.properties</include>
                    <include>**/*.yml</include>
                </includes>
            </resource>
        </resources>
    </build>
</project>
```
```properties
# src/main/resources/application.properties
app.profile=@{profile.name}
db.url=@{db.url}
```
```bash
# 构建指定环境
mvn clean package -P prod
mvn clean package -P test
mvn clean package  # 默认dev（activeByDefault）
```

---

## 四、手写代码/配置文件题（5-8题）

### Q1: 完整的POM.xml配置（Spring Boot项目）
**A:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot Parent -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.1.0</version>
        <relativePath/>
    </parent>

    <!-- 项目坐标 -->
    <groupId>com.example</groupId>
    <artifactId>order-service</artifactId>
    <version>1.2.0-SNAPSHOT</version>
    <packaging>jar</packaging>
    <name>Order Service</name>
    <description>订单微服务</description>

    <!-- 依赖版本统一管理 -->
    <properties>
        <java.version>17</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <spring-cloud.version>2022.0.3</spring-cloud.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <hutool.version>5.8.22</hutool.version>
    </properties>

    <!-- 依赖管理 -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <!-- 项目依赖 -->
    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>

        <!-- 数据库 -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- 工具类 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <!-- 构建配置 -->
    <build>
        <finalName>${project.artifactId}</finalName>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <parameters>true</parameters>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### Q2: 完整的settings.xml配置
**A:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
          http://maven.apache.org/xsd/settings-1.0.0.xsd">

    <!-- 本地仓库位置 -->
    <localRepository>D:/maven/repository</localRepository>

    <!-- 交互模式 -->
    <interactiveMode>true</interactiveMode>

    <!-- 离线模式 -->
    <offline>false</offline>

    <!-- 插件组 -->
    <pluginGroups>
        <pluginGroup>org.apache.maven.plugins</pluginGroup>
        <pluginGroup>org.springframework.boot</pluginGroup>
    </pluginGroups>

    <!-- 代理配置 -->
    <proxies>
        <proxy>
            <id>example-proxy</id>
            <active>false</active>
            <protocol>http</protocol>
            <host>proxy.example.com</host>
            <port>8080</port>
            <username>proxyuser</username>
            <password>proxypass</password>
            <nonProxyHosts>localhost|*.example.com</nonProxyHosts>
        </proxy>
    </proxies>

    <!-- 服务器认证配置 -->
    <servers>
        <server>
            <id>nexus-releases</id>
            <username>admin</username>
            <password>admin123</password>
        </server>
        <server>
            <id>nexus-snapshots</id>
            <username>admin</username>
            <password>admin123</password>
        </server>
        <server>
            <id>github</id>
            <username>token</username>
            <password>ghp_xxxxxxxxxxxx</password>
        </server>
    </servers>

    <!-- 镜像配置（覆盖中央仓库） -->
    <mirrors>
        <mirror>
            <id>aliyun-central</id>
            <mirrorOf>central</mirrorOf>
            <name>阿里云中央仓库镜像</name>
            <url>https://maven.aliyun.com/repository/central</url>
        </mirror>
        <mirror>
            <id>aliyun-public</id>
            <mirrorOf>*</mirrorOf>
            <name>阿里云公共仓库</name>
            <url>https://maven.aliyun.com/repository/public</url>
        </mirror>
    </mirrors>

    <!-- Profile配置 -->
    <profiles>
        <!-- JDK版本 -->
        <profile>
            <id>jdk-17</id>
            <activation>
                <activeByDefault>true</activeByDefault>
                <jdk>17</jdk>
            </activation>
            <properties>
                <maven.compiler.source>17</maven.compiler.source>
                <maven.compiler.target>17</maven.compiler.target>
                <maven.compiler.compilerVersion>17</maven.compiler.compilerVersion>
            </properties>
        </profile>

        <!-- 私服配置 -->
        <profile>
            <id>nexus</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <repositories>
                <repository>
                    <id>nexus-public</id>
                    <url>http://nexus:8081/repository/maven-public/</url>
                    <releases><enabled>true</enabled></releases>
                    <snapshots><enabled>true</enabled></snapshots>
                </repository>
            </repositories>
            <pluginRepositories>
                <pluginRepository>
                    <id>nexus-public</id>
                    <url>http://nexus:8081/repository/maven-public/</url>
                    <releases><enabled>true</enabled></releases>
                    <snapshots><enabled>true</enabled></snapshots>
                </pluginRepository>
            </pluginRepositories>
        </profile>
    </profiles>

    <!-- 激活Profile -->
    <activeProfiles>
        <activeProfile>jdk-17</activeProfile>
        <activeProfile>nexus</activeProfile>
    </activeProfiles>
</settings>
```

### Q3: 多模块Spring Boot项目POM配置
**A:**
```xml
<!-- 父POM: pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.example</groupId>
    <artifactId>microservice-parent</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    
    <modules>
        <module>common-lib</module>
        <module>user-service</module>
        <module>order-service</module>
        <module>gateway-service</module>
    </modules>
    
    <properties>
        <java.version>17</java.version>
        <spring-boot.version>3.1.0</spring-boot.version>
        <spring-cloud.version>2022.0.3</spring-cloud.version>
        <mybatis-plus.version>3.5.3.1</mybatis-plus.version>
        <hutool.version>5.8.22</hutool.version>
    </properties>
    
    <dependencyManagement>
        <dependencies>
            <!-- Spring Cloud BOM -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- Spring Boot BOM -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- 内部模块 -->
            <dependency>
                <groupId>com.example</groupId>
                <artifactId>common-lib</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot.version}</version>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

```xml
<!-- 子模块: user-service/pom.xml -->
<project>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>microservice-parent</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    
    <artifactId>user-service</artifactId>
    <packaging>jar</packaging>
    
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>common-lib</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
    </dependencies>
</project>
```

### Q4: 排除依赖冲突的完整配置
**A:**
```xml
<!-- 情况1：日志框架冲突（排除Spring Boot默认Logback） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- 添加Log4j2 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-log4j2</artifactId>
</dependency>

<!-- 情况2：多个HTTP客户端冲突 -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
<!-- 排除旧版OkHttp -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>sdk-client</artifactId>
    <exclusions>
        <exclusion>
            <groupId>com.squareup.okhttp3</groupId>
            <artifactId>okhttp</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- 情况3：Servlet API版本冲突 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>jakarta.servlet</groupId>
            <artifactId>jakarta.servlet-api</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
    <version>6.0.0</version>
</dependency>
```

### Q5: 配置Maven的profile实现多环境构建
**A:**
```xml
<project>
    <profiles>
        <profile>
            <id>dev</id>
            <activation><activeByDefault>true</activeByDefault></activation>
            <properties>
                <env>dev</env>
                <config.dir>src/main/resources/dev</config.dir>
            </properties>
        </profile>
        <profile>
            <id>test</id>
            <properties>
                <env>test</env>
                <config.dir>src/main/resources/test</config.dir>
            </properties>
        </profile>
        <profile>
            <id>prod</id>
            <properties>
                <env>prod</env>
                <config.dir>src/main/resources/prod</config.dir>
            </properties>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-compiler-plugin</artifactId>
                        <configuration>
                            <source>${java.version}</source>
                            <target>${java.version}</target>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>

    <build>
        <resources>
            <resource>
                <directory>${config.dir}</directory>
                <filtering>true</filtering>
            </resource>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>false</filtering>
                <excludes>
                    <exclude>dev/**</exclude>
                    <exclude>test/**</exclude>
                    <exclude>prod/**</exclude>
                </excludes>
            </resource>
        </resources>
    </build>
</project>
```

---

## 五、系统设计题（3-5题）

### Q1: 设计微服务项目依赖管理方案
**A:**
```
方案：统一BOM + 多模块聚合 + 私服管控

1. 层次结构
   parent-pom (父POM, pom类型)
   ├── bom (BOM模块, 统一版本管理)
   ├── common (公共代码模块)
   ├── service-a (微服务A)
   ├── service-b (微服务B)
   └── gateway (网关)

2. BOM模块设计
   - 定义所有第三方依赖版本
   - 定义内部模块引用版本
   - 所有子模块不直接声明版本号

3. 版本策略
   - 开发阶段: 1.0.0-SNAPSHOT
   - 测试阶段: 1.0.0-RC1/RC2
   - 发布阶段: 1.0.0-RELEASE
   - 热修复: 1.0.1

4. 私服策略
   - Releases: 只允许mvn deploy，禁止覆盖
   - Snapshots: 允许自动deploy，可覆盖
   - 第三方Proxy: 缓存阿里云/中央仓库

5. 依赖检查
   - 禁止使用未定义版本
   - 禁止传递依赖引入冲突版本
   - 定期使用mvn dependency:analyze检查未使用依赖
```

### Q2: 设计Maven构建性能优化方案
**A:**
```
1. 并行构建
   mvn -T 4 clean install          # 4线程并行
   mvn -T 1C clean install         # 每核1线程

2. 构建缓存（Maven 3.8+）
   mvn -Dmaven.build.cache.enabled=true clean install
   
3. 多模块增量构建
   mvn -pl user-service -am clean install  # 只构建指定模块及其依赖
   mvn -pl user-service -amd clean install # 只构建指定模块及其依赖者

4. 跳过不必要的阶段
   mvn package -DskipTests -Dmaven.javadoc.skip=true -Dmaven.source.skip=true

5. 优化依赖下载
   - 配置本地Nexus私服
   - settings.xml配置aliyun镜像
   - 定期清理未使用的依赖

6. Maven Daemon（mvnd）
   # 使用mvnd替代mvn（预热的JVM，启动更快）
   mvnd clean install    # 比mvn快3-5倍

7. 精准排除不必要的依赖
   mvn dependency:analyze  # 分析未使用的依赖
```

### Q3: 设计企业级Nexus私服高可用方案
**A:**
```
架构设计：

用户 → 负载均衡(Nginx/HAProxy) → Nexus Cluster (Active/Standby)
                                            |
                                    共享存储(NFS/S3/MinIO)
                                            |
                                    数据库(PostgreSQL)

1. 部署方式
   - Nexus运行在Docker容器中
   - 两个Nexus实例（主备模式）
   - 共享Blob存储（S3兼容对象存储）
   - 共享数据库（PostgreSQL）

2. 备份策略
   - 每日全量备份Blob Store
   - 实时同步到异地仓库
   - 保留30天备份

3. 清理策略
   - SNAPSHOT: 保留最近10个版本
   - Releases: 永久保留（审计要求）
   - 未使用依赖: 90天未引用自动清理

4. 监控告警
   - 磁盘空间使用率 > 80% 告警
   - 响应延迟 > 1s 告警
   - 仓库同步失败告警

5. 访问控制
   - 开发者: 下载+上传SNAPSHOT
   - CI/CD: 自动deploy
   - 管理员: 全部权限
   - 外部: 只读下载（需Token）
```

---

## 六、常见坑点与最佳实践（表格）

| 坑点/问题 | 原因 | 解决方案 | 最佳实践 |
|-----------|------|----------|----------|
| 依赖下载失败 | 网络/仓库问题 | 清理lastUpdated文件重试 | 配置aliyun镜像 + Nexus私服 |
| 依赖版本冲突 | 传递依赖版本不一致 | 使用exclusion排除 | 统一BOM管理版本 |
| ClassNotFoundException | 依赖scope不正确 | 检查scope配置 | 正确区分compile/provided/runtime |
| 构建速度慢 | 串行构建/无缓存 | 使用`-T`并行构建 | 配置mvnd + 构建缓存 |
| SNAPSHOT不更新 | 默认24小时检查一次 | 使用`-U`强制更新 | CI中使用定时策略更新 |
| 本地仓库膨胀 | 从不清理依赖 | 定期清理未使用依赖 | 配置Nexus定期清理策略 |
| 多模块重复配置 | 各模块单独声明版本 | 使用父POM统一管理 | BOM + parent继承 |
| deploy失败 | 认证/URL配置错误 | 检查settings.xml server配置 | 使用CI Secret管理密码 |
| 插件版本不兼容 | 插件版本过旧 | 升级插件到最新稳定版 | 使用pluginManagement统一管理 |
| resources未过滤 | 未启用filtering | 配置 `<filtering>true</filtering>` | 区分需要过滤的资源 |
| 编码乱码 | 未指定构建编码 | 设置 `<encoding>UTF-8</encoding>` | 统一UTF-8编码 |
| 测试阻塞构建 | 测试执行失败 | 使用`-DskipTests`跳过 | CI中修复失败的测试 |

---

## 七、面试回答模板（Top 5）

### 模板1: Maven的核心概念和作用
**回答框架：**
- **定义**: Maven是基于POM的项目管理和构建自动化工具
- **核心功能**: 依赖管理、项目构建、信息管理、统一项目结构
- **坐标系统**: groupId + artifactId + version唯一标识构件
- **仓库机制**: 本地仓库 → 私服/远程 → 中央仓库
- **生命周期**: clean、default（compile→test→package→install→deploy）、site

### 模板2: Maven依赖冲突解决
**回答框架：**
- **依赖传递**: Maven会自动传递引入依赖的依赖
- **冲突原因**: 不同版本的同个jar通过不同路径引入
- **解决策略**:
  1. 最短路径优先（Nearest Definition）
  2. 先声明优先（First Declaration）
- **解决方案**:
  1. `mvn dependency:tree` 查看依赖树
  2. 使用 `<exclusions>` 排除不需要的依赖
  3. 直接在POM声明期望版本
  4. 统一使用BOM管理版本

### 模板3: Maven生命周期和常用命令
**回答框架：**
- **三套生命周期**:
  - clean: 清理构建产物
  - default: compile→test→package→install→deploy
  - site: 生成项目站点
- **执行原理**: 执行某阶段时，之前所有阶段自动执行
- **常用命令**: `mvn clean package -DskipTests`
- **插件绑定**: 阶段与插件目标绑定执行

### 模板4: Maven vs Gradle对比
**回答框架：**
- **配置方式**: Maven使用XML（声明式），Gradle使用Groovy/Kotlin（编程式）
- **性能**: Gradle有增量构建和构建缓存，比Maven快2-10倍
- **灵活性**: Maven生命周期固定，Gradle可自定义task
- **学习成本**: Maven配置简单但冗长，Gradle灵活但学习曲线陡
- **生态**: Maven仓库最丰富，Gradle兼容Maven仓库
- **选择**: 传统Java项目选Maven，Android/高性能构建选Gradle

### 模板5: Maven私服（Nexus）的作用和配置
**回答框架：**
- **作用**:
  1. 代理中央仓库，加速依赖下载
  2. 管理内部私有构件
  3. 版本管控（Release/SNAPSHOT）
  4. 权限管理和审计
- **仓库类型**: Hosted（内部）、Proxy（代理）、Group（聚合）
- **配置**: distributionManagement + settings.xml server
- **部署**: `mvn deploy` 自动部署到私服
- **优势**: 内网高速访问、版本可控、安全可靠

---

## 八、快速查漏补缺Checklist

- [ ] Maven定义和核心作用（依赖管理、项目构建、信息管理）
- [ ] POM坐标四要素（groupId、artifactId、version、packaging）
- [ ] 仓库类型和查找顺序（本地→私服→中央）
- [ ] 依赖范围（compile/provided/runtime/test/system/import）
- [ ] 依赖传递规则和Scope传递性
- [ ] 依赖冲突解决（最短路径、先声明优先、exclusion）
- [ ] 三套生命周期和阶段顺序
- [ ] 常用命令（clean/compile/test/package/install/deploy）
- [ ] plugins和pluginManagement区别
- [ ] 聚合（modules）和继承（parent）的区别
- [ ] BOM（Bill of Materials）原理和使用
- [ ] 多模块项目搭建（父子POM配置）
- [ ] SNAPSHOT vs Release版本区别
- [ ] settings.xml配置（mirror/server/profile）
- [ ] 私服Nexus仓库类型（hosted/proxy/group）
- [ ] 多环境Profile配置
- [ ] mvn dependency:tree分析依赖树
- [ ] mvn dependency:analyze分析未使用依赖
- [ ] mvn help:effective-pom查看生效POM
- [ ] 跳过测试的三种方式
- [ ] Maven和Gradle核心对比
- [ ] 属性定义和引用（properties + ${}）
- [ ] 资源过滤（resource filtering）
- [ ] 注解处理器配置（annotationProcessorPaths）
- [ ] 构建优化（并行构建、增量构建、mvnd）
- [ ] 编码配置（sourceEncoding、UTF-8）
- [ ] deploy配置（distributionManagement）
- [ ] 版本管理策略（SNAPSHOT/RC/Release）
