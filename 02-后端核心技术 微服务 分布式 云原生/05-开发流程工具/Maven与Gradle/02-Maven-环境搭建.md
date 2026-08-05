# Maven 环境搭建

## 一、前置条件：JDK 环境

Maven 依赖 JDK 运行，推荐版本搭配：

| Maven 版本 | JDK 版本 | 说明 |
|-----------|---------|------|
| 3.8.x | JDK 8+ | 当前主流，兼容性最好 |
| 3.9.x | JDK 8+ | 较新版本 |

### JDK 配置步骤

1. **下载安装**：Oracle 官网或国内镜像下载 JDK 8+，安装路径避免中文和空格
2. **配置环境变量**：

```bash
# 系统变量
JAVA_HOME = D:\Java\jdk1.8.0_200

# 添加至 Path
%JAVA_HOME%\bin
%JAVA_HOME%\jre\bin
```

3. **验证**：

```bash
java -version
javac -version
```

## 二、Maven 安装

### 下载

- 官网：https://maven.apache.org/download.cgi
- 推荐版本：3.8.x 或 3.9.x
- 下载 Binary zip archive（免安装，解压即用）

### 安装与配置

```bash
# 1. 解压到目标目录（避免中文/空格）
D:\Maven\apache-maven-3.8.8

# 2. 配置环境变量
MAVEN_HOME = D:\Maven\apache-maven-3.8.8
Path += %MAVEN_HOME%\bin

# 3. 验证
mvn -v
```

### 目录结构

| 目录 | 作用 |
|------|------|
| `bin/` | Maven 可执行命令（mvn、mvn.cmd） |
| `conf/` | 核心配置文件（settings.xml） |
| `lib/` | Maven 运行所需 jar 包 |

## 三、settings.xml 核心配置

文件位置：`%MAVEN_HOME%/conf/settings.xml`

### 1. 配置本地仓库路径

```xml
<!-- 默认路径为 ${user.home}/.m2/repository，建议修改为非 C 盘 -->
<localRepository>D:\Maven\localRepository</localRepository>
```

### 2. 配置阿里云镜像

```xml
<mirrors>
    <mirror>
        <id>aliyunmaven</id>
        <mirrorOf>central</mirrorOf>
        <name>阿里云公共仓库</name>
        <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
</mirrors>
```

### 3. 配置 JDK 版本

```xml
<profiles>
    <profile>
        <id>jdk-8</id>
        <activation>
            <activeByDefault>true</activeByDefault>
            <jdk>1.8</jdk>
        </activation>
        <properties>
            <maven.compiler.source>1.8</maven.compiler.source>
            <maven.compiler.target>1.8</maven.compiler.target>
            <maven.compiler.compilerVersion>1.8</maven.compiler.compilerVersion>
            <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        </properties>
    </profile>
</profiles>
```

### 4. 环境验证

```bash
mvn help:system
# 输出 BUILD SUCCESS 即配置成功
```

## 四、IDE 配置（IntelliJ IDEA）

### 关联本地 Maven

1. `File → Settings → Build, Execution, Deployment → Build Tools → Maven`
2. 修改三项配置：

| 配置项 | 值 |
|-------|-----|
| **Maven home path** | Maven 安装目录（如 `D:\Maven\apache-maven-3.8.8`） |
| **User settings file** | `D:\Maven\apache-maven-3.8.8\conf\settings.xml` |
| **Local repository** | Maven 本地仓库路径（如 `D:\Maven\localRepository`） |

### 推荐配置

- `Import Maven projects automatically`：勾选（修改 pom.xml 后自动导入依赖）
- `Runner → JRE`：选择 JDK 8（确保 Maven 运行时 JDK 与项目一致）

## 五、Maven 仓库体系

```
本地仓库 → 私有仓库（Nexus）→ 镜像仓库（阿里云）→ 中央仓库
```

| 仓库类型 | 说明 | 用途 |
|---------|------|------|
| **本地仓库** | 开发者本机缓存 | 存储已下载的依赖，加速构建 |
| **私有仓库** | 企业内网搭建（Nexus/Artifactory） | 共享内部构件、缓存外部依赖 |
| **镜像仓库** | 中央仓库的国内镜像 | 加速依赖下载 |
| **中央仓库** | Maven 官方公共仓库 | 存储开源第三方依赖 |
