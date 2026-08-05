# Ubuntu Java 开发环境搭建

> ☕ JDK 多版本管理、Maven/Gradle 配置、环境变量最佳实践、IntelliJ IDEA + VS Code —— 从零到可开发

---

## 📚 目录

1. [环境变量体系](#1-环境变量体系)
2. [JDK 多版本共存](#2-jdk-多版本共存)
3. [Maven 配置优化](#3-maven-配置优化)
4. [Gradle 配置](#4-gradle-配置)
5. [IDE 安装与配置](#5-ide-安装与配置)
6. [开发环境验证清单](#6-开发环境验证清单)

---

## 1. 环境变量体系

### 1.1 加载顺序

```text
Shell 启动时环境变量加载顺序：
  1. /etc/environment     → 系统级（所有用户）
  2. /etc/profile         → 系统级 shell
  3. /etc/bash.bashrc     → 系统级 bash
  4. ~/.profile           → 用户级
  5. ~/.bashrc            → 用户级 bash（交互式）
  6. ~/.bash_profile      → 用户级 login shell

建议：Java 环境变量放 ~/.bashrc（每次打开终端生效）
      或放 ~/.profile（仅登录时加载）
```

### 1.2 标准 Java 环境变量

```bash
# ~/.bashrc 或 ~/.profile

# ===== Java =====
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# ===== Maven =====
export MAVEN_HOME=/opt/maven
export PATH=$MAVEN_HOME/bin:$PATH

# ===== Gradle =====
export GRADLE_HOME=/opt/gradle
export PATH=$GRADLE_HOME/bin:$PATH

# ===== Java 应用常用 =====
export JAVA_OPTS="-Xms256m -Xmx1024m -Dfile.encoding=UTF-8"
# JVM 默认编码（避免中文乱码）

# ===== 生效 =====
source ~/.bashrc

# ===== 验证 =====
echo $JAVA_HOME
java -version
mvn -version
```

---

## 2. JDK 多版本共存

### 2.1 SDKMAN（强烈推荐）

```bash
# 安装 SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# 安装多个 JDK 版本
sdk install java 21.0.5-tem
sdk install java 17.0.12-tem
sdk install java 11.0.24-tem

# 灵活切换
sdk use java 21.0.5-tem     # 当前终端临时切换
sdk default java 21.0.5-tem # 全局默认
sdk current java             # 查看当前版本

# SDKMAN 还能管理：
sdk install maven 3.9.9
sdk install gradle 8.10
sdk install kotlin 2.0
```

### 2.2 update-alternatives（系统级切换）

```bash
# 注册多个 JDK
sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/java-21-openjdk-amd64/bin/java 2100
sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/java-17-openjdk-amd64/bin/java 1700

# 交互式切换
sudo update-alternatives --config java

# 查看当前
java -version
update-alternatives --display java
```

---

## 3. Maven 配置优化

### 3.1 国内镜像加速

```xml
<!-- ~/.m2/settings.xml -->
<settings>
    <!-- 阿里云镜像 -->
    <mirrors>
        <mirror>
            <id>aliyun</id>
            <mirrorOf>central</mirrorOf>
            <name>Aliyun Central</name>
            <url>https://maven.aliyun.com/repository/central</url>
        </mirror>
    </mirrors>

    <!-- 本地仓库路径（默认 ~/.m2/repository） -->
    <localRepository>/home/ubuntu/.m2/repository</localRepository>
</settings>
```

### 3.2 性能优化

```bash
# ~/.bashrc 中设置 MAVEN_OPTS
export MAVEN_OPTS="-Xms512m -Xmx2048m -XX:MaxMetaspaceSize=512m"

# 跳过测试（紧急构建时）
mvn clean package -DskipTests

# 并行构建（多模块项目）
mvn clean package -T 4     # 4 线程并行

# 离线模式（依赖已全量下载时）
mvn clean package -o
```

---

## 4. Gradle 配置

### 4.1 初始化脚本

```bash
# ~/.gradle/init.gradle
allprojects {
    repositories {
        maven { url 'https://maven.aliyun.com/repository/central' }
        maven { url 'https://maven.aliyun.com/repository/gradle-plugin' }
        mavenCentral()
    }
}

# gradle.properties
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true
```

### 4.2 Gradle Wrapper（推荐）

```bash
# 生成 wrapper（项目目录下执行）
gradle wrapper --gradle-version 8.10

# 之后用 wrapper 代替全局 gradle
./gradlew build        # Linux/Mac
gradlew.bat build      # Windows
```

---

## 5. IDE 安装与配置

### 5.1 IntelliJ IDEA

```bash
# Community（免费）
sudo snap install intellij-idea-community --classic

# Ultimate（付费，学生免费）
sudo snap install intellij-idea-ultimate --classic

# 手动安装（最新版）
# 1. jetbrains.com 下载 tar.gz
# 2. 解压到 /opt/idea
# 3. 创建桌面入口
cat > ~/.local/share/applications/idea.desktop << 'EOF'
[Desktop Entry]
Name=IntelliJ IDEA
Exec=/opt/idea/bin/idea.sh
Type=Application
Icon=/opt/idea/bin/idea.png
EOF

# 内存优化
# Help → Edit Custom VM Options
# -Xms2048m -Xmx4096m
```

### 5.2 VS Code

```bash
sudo snap install code --classic

# 推荐 Java 插件（自动安装）：
# Extension Pack for Java (包含 Language Support, Debugger, Maven, Gradle)
```

---

## 6. 开发环境验证清单

```bash
#!/bin/bash
# 保存为 check-env.sh，一键检查 Java 开发环境

echo "=== Java 开发环境检查 ==="

echo -n "JDK:        "; java -version 2>&1 | head -1
echo -n "JAVA_HOME:  "; echo $JAVA_HOME
echo -n "Maven:      "; mvn -version 2>&1 | head -1
echo -n "Gradle:     "; gradle -version 2>&1 | grep "Gradle " | head -1
echo -n "Git:        "; git --version
echo -n "Docker:     "; docker --version 2>/dev/null || echo "未安装"
echo -n "Port 8080:  "; ss -tlnp 2>/dev/null | grep 8080 || echo "未占用"
echo -n "Port 3306:  "; ss -tlnp 2>/dev/null | grep 3306 || echo "未占用"
echo ""
echo "磁盘:"
df -h / /home 2>/dev/null
echo ""
echo "内存:"
free -h
```

```bash
# 执行
chmod +x check-env.sh
./check-env.sh
```

---

> 🎯 **核心要点**：**SDKMAN** 管理 JDK/Maven/Gradle 多版本最灵活；**Maven 镜像**配阿里云加速依赖下载；**环境变量**放 ~/.bashrc；**IntelliJ IDEA** 用 snap 安装最省心。

---

*创建于：2026年7月*
