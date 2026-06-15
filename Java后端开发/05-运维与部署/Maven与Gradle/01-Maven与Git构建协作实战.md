# Maven与Git构建协作实战

## 前言

在现代Java企业级开发中，**Maven**与**Git**是每一位开发者必须熟练掌握的两大核心工具。Maven负责项目构建、依赖管理和生命周期控制，Git负责版本控制、分支策略和团队协作。二者相辅相成，共同构成了Java项目从开发到交付的完整技术栈。

本文将从Maven核心概念入手，深入剖析依赖管理、生命周期、多模块构建、私服配置等实战要点；随后系统讲解Git的核心实践、分支策略与合并模型；最后将二者融合，展现真实的团队协作场景，帮助读者构建一套完整、高效、可落地的构建协作体系。

---

## 一、Maven核心概念

### 1.1 约定优于配置

Maven最核心的设计哲学是**约定优于配置**。它规定了标准项目结构，开发者无需为每个项目单独配置目录布局。

```
my-app/
├── src/
│   ├── main/
│   │   ├── java/          # 项目Java源码
│   │   └── resources/     # 资源文件(配置文件、静态资源等)
│   └── test/
│       ├── java/          # 测试Java源码
│       └── resources/     # 测试资源文件
├── target/                # 构建产物输出目录(编译后的class、jar包等)
├── pom.xml                # Maven项目核心配置文件
└── .gitignore             # Git忽略文件
```

这种约定使团队内部所有项目保持一致的代码布局，降低新成员的上手成本。任何熟悉Maven的开发者，无论进入哪个项目，都能立刻定位到源码、测试和配置文件。

### 1.2 坐标系统

Maven通过**坐标**唯一标识每一个构建产物。坐标由三要素组成：

```xml
<groupId>com.example</groupId>
<artifactId>user-service</artifactId>
<version>1.0.0-SNAPSHOT</version>
```

| 要素 | 说明 | 命名规范 |
|------|------|----------|
| `groupId` | 组织标识，通常为反向域名 | `com.company.project` |
| `artifactId` | 项目/模块名称 | `user-service`、`order-api` |
| `version` | 版本号 | `1.0.0-RELEASE`、`2.1.0-SNAPSHOT` |

**版本号规则**：
- **SNAPSHOT（快照版本）**：表示开发中的不稳定版本，每次构建都会从远程仓库拉取最新快照，适用于团队内部联调阶段。
- **RELEASE（正式版本）**：表示稳定的发布版本，构建后不会变更，适用于生产环境。
- 版本号推荐遵循**语义化版本**：`主版本.次版本.修订号`（如 `2.5.1`），主版本号变更表示不兼容的API修改，次版本号变更表示向下兼容的功能新增，修订号变更表示向下兼容的问题修正。

### 1.3 依赖管理

依赖管理是Maven最核心、最强大的功能之一，它解决了Java项目中"jar包地狱"的问题。

#### 依赖范围（Scope）

依赖范围决定了依赖在不同的构建阶段是否可见：

| Scope | 编译期 | 运行期 | 测试期 | 典型示例 |
|-------|--------|--------|--------|----------|
| `compile`（默认） | 是 | 是 | 是 | 项目核心依赖，如Spring框架 |
| `provided` | 是 | 否 | 是 | Servlet API（容器已提供）、Lombok |
| `runtime` | 否 | 是 | 是 | JDBC驱动实现类 |
| `test` | 否 | 否 | 是 | JUnit、Mockito |
| `system` | 是 | 是 | 是 | **已废弃**，不推荐使用 |

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <scope>compile</scope>  <!-- 默认即为compile，可省略 -->
</dependency>

<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>4.0.1</version>
    <scope>provided</scope>  <!-- 容器提供，打包时排除 -->
</dependency>

<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
    <scope>runtime</scope>  <!-- 编译期不需要，运行时才加载 -->
</dependency>

<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.0</version>
    <scope>test</scope>  <!-- 仅测试期间需要 -->
</dependency>
```

#### 依赖传递与调解

Maven支持**依赖传递**：如果A依赖B、B依赖C，则A自动获得C的依赖。传递行为受scope影响：

- `compile`范围的依赖会传递
- `provided`和`test`范围的依赖不会传递
- `runtime`范围的依赖会传递（仅在运行期）

当出现**依赖冲突**（同一个依赖的不同版本同时存在）时，Maven使用以下规则进行**依赖调解**：

1. **最短路径优先**：路径越短，优先级越高。A → B → C:v2（路径长2），A → D → E → C:v1（路径长3），则选择C:v2。
2. **先声明优先**：相同路径长度下，在pom.xml中先声明的依赖胜出。

#### dependencyManagement

在父POM或项目入口POM中使用`<dependencyManagement>`统一管理版本号，子模块引用时无需指定版本，有效避免版本冲突：

```xml
<!-- 父POM -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>32.1.3-jre</version>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- 子模块POM，无需指定版本 -->
<dependencies>
    <dependency>
        <groupId>com.google.guava</groupId>
        <artifactId>guava</artifactId>
        <!-- 版本从父POM继承 -->
    </dependency>
</dependencies>
```

#### 依赖排除与可选依赖

**依赖排除**用于解决传递依赖带来的冲突：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>some-library</artifactId>
    <version>1.0</version>
    <exclusions>
        <exclusion>
            <groupId>commons-logging</groupId>
            <artifactId>commons-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

**可选依赖**标记为optional后不会传递，需要消费方自行声明：

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
    <optional>true</optional>
</dependency>
```

#### 查看依赖树

使用`mvn dependency:tree`命令可视化依赖关系，快速发现冲突：

```bash
# 查看完整依赖树
mvn dependency:tree

# 查找特定依赖
mvn dependency:tree -Dincludes=com.fasterxml.jackson.core:jackson-databind

# 输出到文件
mvn dependency:tree > deps.txt
```

### 1.4 BOM（Bill of Materials）

BOM是一种特殊的POM，其唯一目的是集中管理一组依赖的版本号。最经典的案例是Spring Boot的`spring-boot-dependencies`：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>3.2.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

引入BOM后，项目中引用Spring Boot生态内的依赖都无需指定版本号，所有版本由BOM统一管控。团队也可以自行编写BOM来管理内部公共库的版本。

### 1.5 生命周期

Maven定义了三大独立的生命周期：

| 生命周期 | 用途 | 核心阶段 |
|----------|------|----------|
| **clean** | 清理项目 | pre-clean → clean → post-clean |
| **default** | 构建项目 | validate → compile → test → package → verify → install → deploy |
| **site** | 生成站点文档 | pre-site → site → post-site → site-deploy |

最常用的是**default生命周期**的核心阶段：

```bash
mvn clean          # 清理target目录
mvn compile        # 编译src/main/java
mvn test           # 运行测试代码（依赖compile已完成）
mvn package        # 打包为jar/war（依赖test已完成）
mvn install        # 安装到本地~/.m2/repository
mvn deploy         # 部署到远程私服
```

执行后面的阶段时，前面的阶段会自动执行。例如`mvn package`会依次执行compile、test、package。

### 1.6 核心插件

Maven本身是一个插件执行框架，实际工作由插件完成：

| 插件 | 功能 | 关键配置 |
|------|------|----------|
| `maven-compiler-plugin` | 编译Java源码 | 指定JDK版本、编码 |
| `maven-surefire-plugin` | 执行单元测试 | 包含/排除测试类 |
| `maven-jar-plugin` | 打包为jar | 指定Main-Class |
| `spring-boot-maven-plugin` | 构建可执行fat jar | 打包所有依赖 |
| `maven-assembly-plugin` | 自定义打包 | 制作分发包 |
| `maven-source-plugin` | 打包源码 | 发布源码jar |
| `maven-deploy-plugin` | 部署到私服 | 配置仓库地址 |

**配置示例——指定JDK版本并跳过测试**：

```xml
<build>
    <plugins>
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
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.1.2</version>
            <configuration>
                <skipTests>true</skipTests>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 1.7 多模块构建

中大型项目通常采用多模块架构，实现模块化开发和独立部署：

```
parent-project/
├── pom.xml                  # 父POM（聚合 + 继承）
├── common/                  # 公共工具模块
│   └── pom.xml
├── user-api/                # 用户服务API定义
│   └── pom.xml
├── user-service/            # 用户服务实现
│   └── pom.xml
└── gateway/                 # API网关
    └── pom.xml
```

**父POM配置**：

```xml
<!-- parent-project/pom.xml -->
<groupId>com.example</groupId>
<artifactId>parent-project</artifactId>
<version>1.0.0-SNAPSHOT</version>
<packaging>pom</packaging>   <!-- 父POM打包类型必须为pom -->

<modules>
    <module>common</module>
    <module>user-api</module>
    <module>user-service</module>
    <module>gateway</module>
</modules>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>3.2.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**子模块POM配置**：

```xml
<!-- user-service/pom.xml -->
<parent>
    <groupId>com.example</groupId>
    <artifactId>parent-project</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
</parent>

<artifactId>user-service</artifactId>

<dependencies>
    <dependency>
        <groupId>${project.groupId}</groupId>
        <artifactId>common</artifactId>
    </dependency>
</dependencies>
```

**多模块构建命令**：

```bash
# 构建所有模块
mvn clean install

# 仅构建某个模块及其依赖模块
mvn clean install -pl user-service -am

# 跳过指定模块
mvn clean install -pl '!common'
```

### 1.8 私服（Nexus / Artifactory）

私服是团队内部托管构件（jar包、插件、POM）的仓库服务器，解决了以下问题：

1. **加速构建**：缓存公网依赖，避免重复下载
2. **内部共享**：发布团队内部库和SNAPSHOT版本
3. **访问控制**：管理第三方依赖的合规性

**settings.xml配置**：

```xml
<settings>
    <!-- 本地仓库路径 -->
    <localRepository>D:/maven/repository</localRepository>

    <!-- 镜像配置：使用阿里云镜像加速 -->
    <mirrors>
        <mirror>
            <id>aliyun-maven</id>
            <mirrorOf>central</mirrorOf>
            <name>阿里云公共仓库</name>
            <url>https://maven.aliyun.com/repository/central</url>
        </mirror>
    </mirrors>

    <!-- 私服认证 -->
    <servers>
        <server>
            <id>nexus-releases</id>
            <username>deployer</username>
            <password>deployer-password</password>
        </server>
        <server>
            <id>nexus-snapshots</id>
            <username>deployer</username>
            <password>deployer-password</password>
        </server>
    </servers>
</settings>
```

**POM中配置部署地址**：

```xml
<distributionManagement>
    <repository>
        <id>nexus-releases</id>
        <name>Releases Repository</name>
        <url>https://nexus.example.com/repository/maven-releases/</url>
    </repository>
    <snapshotRepository>
        <id>nexus-snapshots</id>
        <name>Snapshots Repository</name>
        <url>https://nexus.example.com/repository/maven-snapshots/</url>
    </snapshotRepository>
</distributionManagement>
```

---

## 二、Maven实战

### 2.1 settings.xml 深入

`settings.xml`是Maven的全局/用户级配置文件，位于`~/.m2/settings.xml`。它包含以下核心配置：

**localRepository**：指定本地依赖缓存目录，默认为`~/.m2/repository`。建议将其移动到非系统盘空间充足的路径：

```xml
<localRepository>D:/maven/repository</localRepository>
```

**mirror**：为中央仓库配置镜像，通常使用阿里云或华为云镜像加速国内构建：

```xml
<mirror>
    <id>aliyun-public</id>
    <mirrorOf>*</mirrorOf>  <!-- 所有仓库都走该镜像 -->
    <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

`mirrorOf`的取值策略：
- `central`：仅镜像中央仓库（推荐，其他私有仓库仍直连）
- `*`：所有仓库都走镜像（简单粗暴）
- `external:*`：除本地仓库外的所有外部仓库
- `repo1,repo2`：逗号分隔的仓库ID列表

**server**：配置私服认证信息。`id`必须与POM中`distributionManagement`的`id`一致。

**profile**：定义一组配置（仓库列表、插件仓库、属性等），在新版Maven中也可以在此配置JDK版本、环境变量等。

### 2.2 常用命令详解

```bash
# 清理——删除target目录
mvn clean

# 编译——将src/main/java编译为class文件到target/classes
mvn compile

# 测试——运行src/test/java中的单元测试
mvn test

# 打包——生成jar/war到target目录
mvn package

# 安装——将包安装到本地仓库供其他项目使用
mvn install

# 部署——将包上传到私服
mvn deploy

# 验证——检查项目是否正确且所有必要信息可用
mvn verify
```

### 2.3 常用参数

```bash
# 跳过测试（编译测试类但不执行）
mvn package -DskipTests

# 完全跳过测试（连测试类都不编译）
mvn package -Dmaven.test.skip=true

# 指定profile
mvn package -P prod

# 指定模块构建（-pl：指定模块列表，-am：同时构建依赖模块）
mvn clean install -pl user-api,user-service -am

# 并行构建（-T：指定线程数，或使用乘法因子）
mvn clean install -T 4
mvn clean install -T 2.0C  # CPU核心数 * 2.0

# 离线模式（不从远程仓库下载）
mvn package -o

# 排除test代码编译
mvn compile -Dmaven.test.skip=true

# 调试输出
mvn package -X

# 从指定文件引入外部依赖版本属性
mvn package -Drevision=2.0.0
```

### 2.4 Profile 环境切换

Profile允许为不同环境（开发、测试、生产）指定不同的配置：

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
            <db.url>jdbc:mysql://localhost:3306/dev_db</db.url>
        </properties>
    </profile>

    <!-- 测试环境 -->
    <profile>
        <id>test</id>
        <properties>
            <env>test</env>
            <db.url>jdbc:mysql://test-db:3306/test_db</db.url>
        </properties>
    </profile>

    <!-- 生产环境 -->
    <profile>
        <id>prod</id>
        <properties>
            <env>prod</env>
            <db.url>jdbc:mysql://prod-db:3306/prod_db</db.url>
        </properties>
    </profile>
</profiles>
```

配合Maven资源过滤，可以在不同环境下自动替换配置文件中的占位符：

```xml
<build>
    <resources>
        <resource>
            <directory>src/main/resources</directory>
            <filtering>true</filtering>  <!-- 启用资源过滤 -->
        </resource>
    </resources>
</build>
```

在`application.properties`中使用占位符：

```properties
db.url=${db.url}
env=${env}
```

构建时通过 `-P` 激活对应profile：

```bash
mvn clean package -P prod
```

---

## 三、Git核心实践

### 3.1 Git工作流

Git的核心模型可以简化为四个区域的数据流：

```
工作区 (Working Directory)
    ↓ git add
暂存区 (Staging Area / Index)
    ↓ git commit
本地仓库 (Local Repository)
    ↓ git push
远程仓库 (Remote Repository)
```

```bash
# 完整提交流程
git add .                    # 工作区 → 暂存区
git commit -m "feat: 添加用户注册功能"  # 暂存区 → 本地仓库
git push origin main         # 本地仓库 → 远程仓库
```

反向流程：

```bash
git pull                     # 远程仓库 → 工作区
git checkout -- file.txt     # 工作区恢复（丢弃修改）
git reset HEAD file.txt      # 暂存区→工作区（取消暂存）
git reset --soft HEAD~1      # 本地仓库→暂存区（撤销commit，保留修改）
```

### 3.2 常用命令详解

#### 提交相关

```bash
# 将修改添加到暂存区
git add src/main/java/com/example/UserService.java
git add .                         # 添加所有修改（新增/修改）
git add -A                        # 添加所有变动（包括删除）
git add -p                        # 交互式分块暂存（每个改动块确认）

# 提交
git commit -m "fix: 修复空指针异常"
git commit --amend -m "修正提交信息"  # 修改最近一次commit信息
git commit --amend --no-edit        # 将暂存区修改并入上一次commit（不修改信息）
```

#### 分支相关

```bash
# 查看分支
git branch                    # 本地分支列表
git branch -r                 # 远程分支列表
git branch -a                 # 所有分支（本地+远程）
git branch -vv                # 显示每个分支的跟踪关系

# 创建与切换
git branch feature/login      # 创建分支
git checkout feature/login    # 切换分支
git checkout -b feature/login # 创建并切换（等价于上面两条）
git switch -c feature/login   # 新版Git推荐用法

# 删除分支
git branch -d feature/login   # 删除已合并的分支
git branch -D feature/login   # 强制删除（即使未合并）
git push origin --delete feature/login  # 删除远程分支
```

#### 撤销操作

```bash
# 丢弃工作区的修改（未暂存）
git checkout -- file.txt
git restore file.txt          # 新版推荐用法

# 取消暂存（文件保留在暂存区之外）
git reset HEAD file.txt
git restore --staged file.txt  # 新版推荐用法

# 撤销提交
git reset --soft HEAD~1       # 撤销commit，修改保留在暂存区
git reset --mixed HEAD~1      # 撤销commit，修改保留在工作区（默认）
git reset --hard HEAD~1       # 撤销commit，丢弃所有修改（慎用！）

# 安全撤销（推荐公共分支使用）
git revert HEAD               # 生成一个新commit来抵消上次commit的改动
git revert <commit-hash>      # 撤销指定的某个commit
```

**reset与revert的对比**：

| 操作 | 是否改写历史 | 是否生成新commit | 适用场景 |
|------|-------------|-----------------|----------|
| `git reset` | 是 | 否 | 本地私有分支 |
| `git revert` | 否 | 是 | 公共分支/远程分支 |

#### 暂存（Stash）

当需要切换到其他分支处理紧急事务，但当前工作区尚未完成时：

```bash
git stash save "用户模块开发中"     # 暂存当前修改
git stash list                      # 查看暂存列表
git stash pop                       # 恢复最近一次暂存并删除
git stash apply stash@{2}           # 恢复指定暂存但不删除
git stash drop stash@{1}            # 删除指定暂存
git stash clear                     # 清空所有暂存
```

#### 日志与历史

```bash
git log                        # 详细日志
git log --oneline              # 简洁模式（一行一个commit）
git log --graph                # 图形化显示分支历史
git log --all                  # 所有分支的历史
git log --oneline --graph --all --decorate  # 最常用组合

# 挽救丢失的提交（reset/--amend后使用）
git reflog                     # 显示所有HEAD移动记录
git reset --hard HEAD@{2}      # 恢复到指定位置
```

### 3.3 分支策略

#### Git Flow

Git Flow是最经典的分支模型，适合有固定版本发布周期的项目：

```
main ─────●─────────●─────────●──
           \       / \       /
develop ────●─────●───●─────●────
             \       /     /
feature      ●──●──●     /
                         /
release ───────────────●──●
                       /
hotfix ───────────────●
```

- **main**：生产分支，只接受从release或hotfix的合并
- **develop**：主开发分支，汇集各feature的变更
- **feature/***：功能分支，从develop签出，完成后合并回develop
- **release/***：发布准备分支，从develop签出，修复小问题后合并到main和develop
- **hotfix/***：紧急修复分支，从main签出，修复后合并到main和develop

#### GitHub Flow

GitHub Flow更加轻量，适合持续部署场景：

1. 从main分支签出feature分支
2. 提交修改并push到远程
3. 创建Pull Request
4. Code Review通过后合并到main
5. 立即部署到生产环境

```bash
git checkout -b feature/payment
# ... 开发 ...
git add .
git commit -m "feat: 集成支付网关"
git push origin feature/payment
# → 在GitHub上创建PR → Code Review → Merge
git checkout main
git pull
git branch -d feature/payment
```

#### GitLab Flow

GitLab Flow在GitHub Flow基础上增加了环境分支：

- **main**：主分支
- **pre-production**：预发布环境（部署到预发布服务器验证）
- **production**：生产环境（部署到生产服务器）
- **feature/***：功能分支

### 3.4 合并策略

```bash
# 1. 普通合并（保留完整分支历史）
git checkout main
git merge feature/login
# 产生一个merge commit

# 2. 变基合并（线性历史，更清晰）
git checkout feature/login
git rebase main              # 将feature分支的提交放在main所有提交之上
git checkout main
git merge feature/login      # 此时为快进合并（fast-forward）

# 3. Squash合并（将多个commit压缩成一个）
git checkout main
git merge --squash feature/login
git commit -m "feat: 添加登录功能"
```

| 策略 | 历史可读性 | 分支上下文保留 | 适用场景 |
|------|-----------|---------------|----------|
| merge | 差（交织） | 完整保留 | 公共分支、需要保留完整历史 |
| rebase | 好（线性） | 丢失 | 个人开发分支、希望历史干净 |
| squash | 最好 | 丢失 | 功能分支合并，一个功能一个commit |

### 3.5 冲突解决

当两个分支修改了同一文件同一区域时，merge或rebase会报冲突：

```bash
# 合并且出现冲突
git merge feature/payment
# 输出：CONFLICT in UserController.java

# 查看冲突文件
git status

# 手动编辑冲突文件后：
# <<<<<<< HEAD
# System.out.println("旧代码");
# =======
# log.info("新代码");
# >>>>>>> feature/payment

# 标记为已解决
git add UserController.java
git commit       # 完成合并

# 使用外部工具
git mergetool    # 会调用配置的合并工具
```

**rebase冲突处理**：

```bash
git pull --rebase
# 或
git rebase main
# 如果出冲突 → 手动解决 → git add → git rebase --continue
# 若想放弃rebase → git rebase --abort
```

### 3.6 提交规范（Conventional Commits）

推荐采用**约定式提交**规范，使提交信息具有语义化结构，便于生成CHANGELOG和自动版本管理：

```
<类型>(<范围>): <简短描述>

<详细描述>

<关联Issue>
```

| 类型 | 含义 |
|------|------|
| `feat` | 新功能 |
| `fix` | 修复Bug |
| `docs` | 文档变更 |
| `refactor` | 重构（既不修复bug也不添加功能） |
| `test` | 添加或修改测试 |
| `chore` | 构建工具、CI等杂项 |
| `style` | 代码格式（不影响语义） |
| `perf` | 性能优化 |
| `ci` | CI/CD配置变更 |

**示例**：

```bash
git commit -m "feat(user): 添加用户注册短信验证功能

- 集成阿里云短信SDK
- 添加短信验证码发送接口
- 注册流程增加短信验证码校验

Closes #128"
```

### 3.7 .gitignore 配置

```gitignore
# Maven构建输出
target/
*.class
*.jar
*.war
*.nar
*.ear

# IDE文件
.idea/
*.iml
*.ipr
*.iws
.vscode/
*.swp
*.swo

# 日志
*.log

# 操作系统
.DS_Store
Thumbs.db

# 环境配置文件（包含敏感信息）
*.properties.local
application-dev.yml
application-local.yml

# 临时文件
*.tmp
*.bak
```

---

## 四、协作场景实战

### 4.1 日常开发流程

以下是一个标准的功能开发完整流程：

```bash
# 1. 同步最新代码
git checkout main
git pull

# 2. 创建功能分支
git checkout -b feature/user-register

# 3. 开发编码
# ... 在IDE中编写代码 ...

# 4. 运行本地构建验证
mvn clean test              # 确保单元测试通过
mvn clean package -DskipTests  # 确保打包成功

# 5. 提交代码（建议小粒度、有意义的提交）
git add src/main/java/com/example/controller/UserController.java
git commit -m "feat: 添加用户注册接口"

git add src/main/java/com/example/service/UserService.java
git add src/main/java/com/example/service/impl/UserServiceImpl.java
git commit -m "feat: 实现用户注册业务逻辑"

git add src/test/java/com/example/service/UserServiceTest.java
git commit -m "test: 添加用户注册单元测试"

# 6. 推送远程并创建PR
git push origin feature/user-register
# → 在GitHub/GitLab/Gitee上创建Pull Request

# 7. 进入Code Review流程，团队成员review代码

# 8. Review通过后合并（以squash merge为例）
# → 在GitHub上点击"Squash and merge"

# 9. 删除本地/远程功能分支
git checkout main
git pull
git branch -d feature/user-register
git push origin --delete feature/user-register
```

### 4.2 处理冲突场景

场景：你和同事同时修改了`UserService.java`，你的PR在后，需要解决冲突：

```bash
# 方法一：直接合并解决冲突
git checkout feature/payment
git merge main
# 出现冲突 → 手动编辑冲突文件
git add UserService.java
git commit -m "chore: 解决与main分支的冲突"
git push origin feature/payment

# 方法二：使用rebase（推荐，历史更干净）
git checkout feature/payment
git pull --rebase origin main
# 或
git fetch origin
git rebase origin/main

# 如果有冲突：
# 手动解决冲突 → 
git add UserService.java
git rebase --continue
git push origin feature/payment --force-with-lease
```

**`--force-with-lease` vs `--force`**：`--force-with-lease`更安全，它会在推送前检查远程分支状态是否与你最后一次fetch时一致，避免覆盖他人的提交。

### 4.3 Cherry-pick 场景

场景：线上发现紧急Bug，但修复代码在develop分支上，需要移植到main分支：

```bash
# 在develop分支找到修复Bug的commit hash
git log --oneline develop

# 切换到main分支
git checkout main

# 仅将特定commit应用到main
git cherry-pick abc1234

# 若冲突：手动解决 → git add → git cherry-pick --continue
# 若放弃：git cherry-pick --abort

# 批量cherry-pick（连续commit，左开右闭）
git cherry-pick abc1234..def5678
```

### 4.4 Tag与版本发布

在每次版本发布时，使用Tag标记对应的commit：

```bash
# 创建轻量标签
git tag v1.0.0

# 创建附注标签（推荐，包含作者、日期、签名信息）
git tag -a v1.0.0 -m "Release v1.0.0：用户模块上线"

# 对历史commit打标签
git tag -a v0.9.0 abc1234 -m "Beta版本"

# 推送标签到远程
git push origin v1.0.0
git push origin --tags        # 推送所有未推送的标签

# 查看标签
git tag -l "v1.*"             # 通配符搜索
git show v1.0.0               # 显示标签详情

# 删除标签
git tag -d v1.0.0            # 删除本地
git push origin --delete v1.0.0  # 删除远程

# 从标签创建分支（hotfix场景）
git checkout -b hotfix/urgent-fix v1.0.0
```

结合Maven的版本管理：

```bash
# 1. 修改POM版本为正式版本
mvn versions:set -DnewVersion=1.0.0

# 2. 构建发布
mvn clean deploy -P release

# 3. 提交版本变更
git add pom.xml
git commit -m "chore: 升级版本至v1.0.0"

# 4. 打标签
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin --tags

# 5. 修改为下一个开发版本
mvn versions:set -DnewVersion=1.1.0-SNAPSHOT
git add pom.xml
git commit -m "chore: 切换到下一开发版本v1.1.0-SNAPSHOT"
git push
```

### 4.5 Maven + Git 完整CI流水线

在CI/CD平台（Jenkins、GitLab CI、GitHub Actions）上，Maven和Git协同完成自动化构建：

```yaml
# .gitlab-ci.yml 示例
stages:
  - build
  - test
  - package
  - deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"

cache:
  paths:
    - .m2/repository/

before_script:
  - git checkout $CI_COMMIT_BRANCH

build:
  stage: build
  script:
    - mvn compile -Dmaven.test.skip=true

test:
  stage: test
  script:
    - mvn test
  artifacts:
    reports:
      junit:
        - target/surefire-reports/TEST-*.xml

package:
  stage: package
  script:
    - mvn package -DskipTests -P $CI_ENVIRONMENT_NAME
  artifacts:
    paths:
      - target/*.jar

deploy:
  stage: deploy
  script:
    - mvn deploy -DskipTests -P $CI_ENVIRONMENT_NAME
  only:
    - main
```

```yaml
# .github/workflows/maven.yml 示例
name: Java CI with Maven

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven
    - name: Build with Maven
      run: mvn -B clean package --file pom.xml
    - name: Test
      run: mvn test
```

### 4.6 多模块项目的Git + Maven协作要点

**1. 模块化与分支策略**
- 每个微服务模块独立维护自己的POM版本
- 公共模块变更时，使用`mvn versions:set`更新版本并打tag
- 模块版本变化时，通过commit message关联JIRA/Issue

**2. 构建优化**
```bash
# 只构建变更模块及依赖
mvn clean install -pl user-service -am

# 使用并行编译加速
mvn clean install -T 4

# 使用增量编译（Maven 3.8+）
mvn compile -Dmaven.compiler.incremental=true
```

**3. 版本管理规范**
```
# 开发周期中的版本演进
v1.0.0-SNAPSHOT    # 开发阶段
v1.0.0-RC1         # 发布候选1
v1.0.0-RC2         # 发布候选2
v1.0.0-RELEASE     # 正式发布
v1.1.0-SNAPSHOT    # 下一个开发周期
```

### 4.7 Git Hooks与Maven集成

利用Git Hooks在特定时机自动触发Maven构建验证：

**pre-commit hook**：在提交前自动运行测试，避免坏代码入库：

```bash
#!/bin/sh
# .git/hooks/pre-commit

echo "运行提交前代码检查..."
mvn test
if [ $? -ne 0 ]; then
    echo "测试未通过，提交已阻止！"
    exit 1
fi

mvn checkstyle:check
if [ $? -ne 0 ]; then
    echo "代码风格检查未通过，提交已阻止！"
    exit 1
fi

echo "所有检查通过！"
exit 0
```

**prepare-commit-msg hook**：自动添加分支名到提交信息，便于追溯：

```bash
#!/bin/sh
# .git/hooks/prepare-commit-msg

BRANCH_NAME=$(git branch --show-current)
ISSUE_NUMBER=$(echo $BRANCH_NAME | grep -oP '(?<=feature/)\d+')

if [ ! -z "$ISSUE_NUMBER" ]; then
    echo "[#$ISSUE_NUMBER] $(cat $1)" > $1
fi
```

---

## 五、最佳实践总结

### 5.1 Maven实践要点

1. **统一版本管理**：使用`<dependencyManagement>`和BOM集中管理版本，杜绝硬编码版本号
2. **合理使用scope**：正确设置依赖范围，`provided`用于容器API，`test`用于测试框架，`runtime`用于JDBC驱动
3. **冲突早发现**：将`mvn dependency:tree`纳入CI流水线，在合并PR时自动检查依赖冲突
4. **Profile驱动配置**：使用Maven Profile管理多环境配置，配合资源过滤实现环境间自动切换
5. **加速构建**：配置阿里云等镜像、使用`-T`并行构建、利用CI缓存`~/.m2/repository`

### 5.2 Git实践要点

1. **小粒度提交**：每个commit只包含一个逻辑变更，配合Conventional Commits规范
2. **适当的分支策略**：团队按项目节奏选择Git Flow或GitHub Flow，保持一致性
3. **rebase前想清楚**：公共分支禁用`git push --force`，使用`--force-with-lease`
4. **及时处理冲突**：日常开发中多执行`git pull --rebase`，保持分支与主线的同步，减少冲突规模
5. **善用.gitignore**：配置完善的`.gitignore`，将target、.idea、*.iml等排除在版本控制之外

### 5.3 Maven + Git协同要点

1. **构建与版本联动**：版本发布时Maven版本号变更与Git Tag必须一致
2. **CI流水线**：在CI中同时执行Maven构建和Git分支策略验证（如禁止直接push到main）
3. **环境一致性**：Maven Profile与Git分支对应（dev分支→dev profile，main分支→prod profile）
4. **自动化发布**：使用Maven Release Plugin结合Git Tag实现一键发布
5. **依赖缓存策略**：在CI中合理配置Maven依赖缓存，避免每次构建都从远程下载

---

## 六、总结清单

以下是一份Maven与Git构建协作实战的完整检查清单，可用于团队新人培训和项目初始化参考：

### Maven环境配置
- [ ] 配置`settings.xml`中的本地仓库路径
- [ ] 配置阿里云/华为云镜像（mirror）
- [ ] 配置私服认证信息（server）
- [ ] 确认JDK版本与`maven-compiler-plugin`一致

### 项目POM配置
- [ ] 定义完整的坐标（groupId、artifactId、version）
- [ ] 设置`<parent>`或`<dependencyManagement>`统一版本
- [ ] 合理使用依赖scope、exclusions和optional
- [ ] 配置多模块聚合（如适用）
- [ ] 配置build插件（compiler、surefire、spring-boot-maven-plugin等）

### Git仓库配置
- [ ] 初始化`.gitignore`（包含target、*.iml、.idea等）
- [ ] 配置用户信息（user.name、user.email）
- [ ] 确定分支策略（Git Flow / GitHub Flow）
- [ ] 配置分支保护规则（禁止直接推送main）

### 开发流程
- [ ] 从main/develop签出feature分支
- [ ] 每次`git push`前先`mvn test`通过
- [ ] 提交信息遵循Conventional Commits规范
- [ ] PR创建后经历Code Review
- [ ] 合并前使用Squash或Rebase保持历史整洁
- [ ] 合并后及时删除已合并的功能分支

### 版本发布
- [ ] 使用`mvn versions:set`更新POM版本号
- [ ] 本地构建`mvn clean deploy`验证通过
- [ ] 提交版本变更并打Tag（`git tag -a`）
- [ ] 推送Tag到远程仓库
- [ ] 切换到下一SNAPSHOT版本

### CI/CD流水线
- [ ] 配置自动触发Maven构建（push/PR事件）
- [ ] 集成单元测试报告
- [ ] 配置依赖缓存加速构建
- [ ] 集成代码质量检查（Checkstyle、SpotBugs）
- [ ] 自动化部署到私服/服务器

---

## 参考资源

- Maven官方文档：https://maven.apache.org/guides/
- Git官方文档：https://git-scm.com/doc
- Conventional Commits：https://www.conventionalcommits.org/
- Spring官方BOM：https://spring.io/projects/spring-boot
- 阿里云Maven仓库指南：https://developer.aliyun.com/mvn/guide
- Git Flow模型（Vincent Driessen）：https://nvie.com/posts/a-successful-git-branching-model/
