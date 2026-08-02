# 03 JDK 核心工具链实战

> javac编译、jar打包、jlink定制运行时、jshell快速验证——这些JDK自带工具比你想象的强大100倍

## 📚 目录

1. [javac — Java编译器](#1-javac--java编译器)
2. [java — Java启动器](#2-java--java启动器)
3. [jar — Java归档工具](#3-jar--java归档工具)
4. [jlink — 定制运行时镜像](#4-jlink--定制运行时镜像)
5. [jpackage — 打包原生安装包](#5-jpackage--打包原生安装包)
6. [jshell — Java交互式Shell](#6-jshell--java交互式shell)
7. [javadoc — 文档生成](#7-javadoc--文档生成)
8. [javap — 反编译](#8-javap--反编译)
9. [jdeps — 依赖分析](#9-jdeps--依赖分析)
10. [其他工具](#10-其他工具)

---

## 1. javac — Java编译器

### 1.1 编译原理概述

`javac` 将 `.java` 源文件编译为 JVM 可执行的 `.class` 字节码文件。编译流程分为四个阶段：

```text
.java 源文件
    ↓
词法分析 (Lexical Analysis) → 生成 Token 流
    ↓
语法分析 (Syntax Analysis) → 生成 AST（抽象语法树）
    ↓
语义分析 (Semantic Analysis) → 类型检查、符号解析
    ↓
代码生成 (Code Generation) → .class 字节码
```

> 💡 **理解编译流程的意义**：面试中常问的"泛型擦除"发生在语义分析阶段；"语法糖"（如 for-each、自动拆装箱）也在编译期被解糖。掌握这些有助于理解 Java 的运行机制。

### 1.2 常用参数详解

| 参数 | 作用 | 示例 |
|------|------|------|
| `-d <目录>` | 指定 class 文件输出目录 | `javac -d target/classes src/com/example/*.java` |
| `-cp <路径>` / `-classpath <路径>` | 指定依赖的 class 或 jar 路径 | `javac -cp lib/*:. Main.java` |
| `-source <版本>` | 设置源代码兼容版本 | `javac -source 8 MyApp.java` |
| `-target <版本>` | 设置生成的 class 文件版本 | `javac -target 8 MyApp.java` |
| `-encoding <编码>` | 指定源文件编码（强烈推荐显式设置） | `javac -encoding UTF-8 MyApp.java` |
| `-verbose` | 输出编译详细信息 | `javac -verbose MyApp.java` |
| `-Xlint:all` | 启用所有编译警告 | `javac -Xlint:all MyApp.java` |
| `-Werror` | 将警告视为错误 | `javac -Xlint:all -Werror MyApp.java` |
| `-g` | 生成所有调试信息（默认） | `javac -g:lines MyApp.java` |
| `-nowarn` | 关闭所有警告 | `javac -nowarn MyApp.java` |
| `-proc:none` | 不执行注解处理器 | `javac -proc:none MyApp.java` |

**实战示例：**

```bash
# 基本编译
javac HelloWorld.java

# 指定输出目录和 classpath
javac -d target/classes -cp lib/commons-lang3.jar:lib/gson-2.10.jar \
      -encoding UTF-8 \
      -Xlint:all \
      src/main/java/com/example/App.java

# 编译整个项目（递归查找源文件）
javac -d target/classes -sourcepath src/main/java \
      $(find src/main/java -name "*.java")
```

### 1.3 交叉编译

交叉编译是指使用高版本 JDK 编译出兼容低版本 JVM 的 class 文件。

```bash
# 错误方式（JDK 17 编译 target 8）
javac -source 8 -target 8 MyApp.java
# ⚠️ 问题：虽然 class 版本是 8，但编译时仍可使用 JDK 17 的 API

# 正确方式：使用 -bootclasspath 指定目标版本的 API
javac -source 8 -target 8 \
      -bootclasspath /path/to/jdk8/jre/lib/rt.jar \
      MyApp.java

# Java 9+ 推荐方式：--release 参数
javac --release 8 MyApp.java
# --release 同时做了三件事：
# 1. 设置 -source 8
# 2. 设置 -target 8
# 3. 使用 JDK 8 的 API 符号进行编译（防止用了高版本 API）
```

### 1.4 注解处理器（Annotation Processors）

```bash
# 编译时运行 Lombok 注解处理器
javac -d target/classes \
      -cp lombok.jar \
      -processor lombok.launch.AnnotationProcessorHider$AnnotationProcessor \
      src/main/java/com/example/User.java

# 在实际项目中，注解处理器通过 Maven/Gradle 自动管理：
# Maven: annotationProcessorPaths
# Gradle: annotationProcessor 'org.projectlombok:lombok:1.18.30'
```

### 1.5 `--release` 参数详解（Java 9+）

```bash
# --release 是 Java 9 引入的参数，替代旧的 -source/-target/-bootclasspath 组合
javac --release 11 MyApp.java      # 等价于：
javac -source 11 -target 11 -bootclasspath <jdk11-rt>

javac --release 17 MyApp.java      # 等价于：
javac -source 17 -target 17 -bootclasspath <jdk17-rt>

# 查看可用的 release 版本
javac --release --help
```

| JDK 版本 | 对应的 class 版本号 | 支持的语言特性 |
|---------|-------------------|--------------|
| 8 (52) | 52.0 | Lambda、Stream、Optional |
| 11 (55) | 55.0 | var、HTTP Client、模块化 |
| 17 (61) | 61.0 | Sealed Class、Record、Pattern Matching |
| 21 (65) | 65.0 | Virtual Threads、Pattern Matching for switch |

> 🎯 **核心要点**：日常开发中，javac 最常用的参数组合是 `-d -cp -encoding -Xlint`。使用 `--release` 替代 `-source/-target` 可以避免交叉编译时的 API 兼容性问题。

---

## 2. java — Java启动器

### 2.1 常用参数

| 参数 | 作用 | 示例 |
|------|------|------|
| `-cp <路径>` / `-classpath <路径>` | 指定 class 搜索路径 | `java -cp target/classes:lib/* com.example.App` |
| `-jar <jar文件>` | 运行可执行 JAR 包 | `java -jar app.jar` |
| `-D<属性名>=<值>` | 设置系统属性 | `java -Duser.timezone=Asia/Shanghai -Dfile.encoding=UTF-8 App` |
| `-Xms<大小>` | 初始堆大小 | `java -Xms512m -Xmx2g App` |
| `-Xmx<大小>` | 最大堆大小 | `java -Xms256m -Xmx4g App` |
| `-Xss<大小>` | 线程栈大小 | `java -Xss256k App`（减少栈大小可支持更多线程） |
| `-XX:+PrintGCDetails` | 打印 GC 详细信息 | `java -XX:+PrintGCDetails App` |
| `-XX:+HeapDumpOnOutOfMemoryError` | OOM 时自动生成堆转储 | `java -XX:+HeapDumpOnOutOfMemoryError App` |
| `-enableassertions` / `-ea` | 启用断言 | `java -ea com.example.App` |
| `-version` | 显示 JDK 版本信息 | `java -version` |
| `-showversion` | 显示版本后再运行程序 | `java -showversion -jar app.jar` |

**JVM 内存参数速查：**

```bash
# 常用内存配置组合（生产环境 Spring Boot 典型配置）
java -Xms512m -Xmx512m \
     -XX:MetaspaceSize=128m \
     -XX:MaxMetaspaceSize=256m \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/app/heapdump.hprof \
     -Dspring.profiles.active=prod \
     -jar app.jar
```

### 2.2 `--module-path` 和模块化运行（Java 9+）

```bash
# 传统 classpath 方式（依然兼容）
java -cp mods/com.example.jar:mods/com.lib.jar com.example.Main

# 模块化方式
java --module-path mods -m com.example/com.example.Main

# 查看模块描述符
jar --describe-module --file=mods/com.example.jar
```

### 2.3 `--enable-preview` 预览特性

```bash
# 使用预览特性（如 Pattern Matching for switch）
javac --release 21 --enable-preview App.java
java --enable-preview App

# Maven 中的预览特性配置
# <build><plugins><plugin>
#   <groupId>org.apache.maven.plugins</groupId>
#   <artifactId>maven-compiler-plugin</artifactId>
#   <configuration>
#     <release>21</release>
#     <compilerArgs><arg>--enable-preview</arg></compilerArgs>
#   </configuration>
# </plugin></plugins></build>
```

### 2.4 `@argument-file` 参数文件

当命令行参数过多时，可以将参数写入文件：

```bash
# 创建参数文件 options.txt
cat > options.txt << 'EOF'
-cp
target/classes:lib/*
-Dspring.profiles.active=prod
-Xmx2g
-Xms512m
-XX:+UseG1GC
-XX:+HeapDumpOnOutOfMemoryError
com.example.App
EOF

# 使用参数文件启动
java @options.txt

# 多个参数文件可组合
java @jvm-options.txt @app-options.txt @debug-options.txt
```

> 💡 **参数文件最佳实践**：CI/CD 流水线中将不同环境（dev/staging/prod）的参数配置为独立文件，通过 `@` 引用，避免在构建脚本中硬编码大量参数。

### 2.5 后台运行与日志重定向

```bash
# Linux / macOS 后台运行
nohup java -jar app.jar > /var/log/app/output.log 2>&1 &

# 使用 systemd 管理（生产环境推荐）
# /etc/systemd/system/app.service
[Unit]
Description=My Java Application
After=network.target

[Service]
Type=simple
User=appuser
ExecStart=/usr/bin/java -Xmx2g -jar /opt/app/app.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target

# Windows 后台运行（使用 javaw）
javaw -jar app.jar
# javaw 不显示控制台窗口，适合 GUI 应用
```

> 🎯 **核心要点**：`java` 命令是 JVM 的入口。生产环境中用好 `-Xms/-Xmx` 控制堆内存、`-XX:+HeapDumpOnOutOfMemoryError` 预防故障、`@argument-file` 管理复杂参数、`nohup` 或 `systemd` 保证进程持续运行。

---

## 3. jar — Java归档工具

### 3.1 创建 JAR 文件

```bash
# 基本创建
jar cf app.jar -C target/classes .

# 创建并指定 Main-Class（可执行 JAR）
jar cfe app.jar com.example.Main -C target/classes .

# 创建 JAR 并指定 Manifest
jar cfm app.jar META-INF/MANIFEST.MF -C target/classes .

# 创建 JAR（不压缩，加快启动速度）
jar cf0 app.jar -C target/classes .

# 多模块打包
jar cf lib.jar -C module1/build/classes . -C module2/build/classes .
```

| 参数 | 说明 | 示例 |
|------|------|------|
| `c` | 创建新 JAR | `jar cf app.jar ...` |
| `u` | 更新已有 JAR | `jar uf app.jar newfile.class` |
| `x` | 解压 JAR | `jar xf app.jar` |
| `t` | 查看 JAR 内容 | `jar tf app.jar` |
| `f` | 指定 JAR 文件名 | 通常与 `c/u/x/t` 组合使用 |
| `v` | 输出详细信息 | `jar cvf app.jar ...` |
| `e` | 指定 Main-Class（Java 9+） | `jar cfe app.jar MainClass ...` |
| `m` | 包含自定义 MANIFEST.MF | `jar cfm app.jar manifest.mf ...` |

### 3.2 MANIFEST.MF 详解

`META-INF/MANIFEST.MF` 是 JAR 包的"身份证"，位于 JAR 文件内的 `META-INF/` 目录：

```text
Manifest-Version: 1.0
Created-By: 17 (Eclipse Adoptium)
Main-Class: com.example.Application
Class-Path: lib/commons-lang3.jar lib/gson-2.10.jar
Implementation-Title: My Application
Implementation-Version: 1.0.0
Implementation-Vendor: My Company
```

| Manifest 属性 | 作用 | 是否必须 |
|--------------|------|---------|
| `Main-Class` | 指定可执行 JAR 的入口类 | 运行 `java -jar` 时必须 |
| `Class-Path` | 指定依赖的 JAR 路径（空格分隔） | 依赖外部 JAR 时必须（通常用构建工具处理） |
| `Implementation-Title` | 应用名称 | 可选 |
| `Implementation-Version` | 版本号 | 可选（可在代码中读取） |

> ⚠️ **Class-Path 注意事项**：Manifest 中的 `Class-Path` 引用的是相对于 JAR 文件的路径。实际项目中使用 Maven/Gradle 管理依赖时，通常使用 `maven-shade-plugin` 或 `spring-boot-maven-plugin` 将依赖合并或重新定位，而非手动配置 `Class-Path`。

### 3.3 可执行 JAR

```bash
# 方式一：jar 命令 + -e 参数（Java 9+）
jar cfe app.jar com.example.App -C target/classes .

# 方式二：自定义 Manifest
echo "Main-Class: com.example.App" > manifest.txt
jar cfm app.jar manifest.txt -C target/classes .

# 运行
java -jar app.jar

# 验证 JAR 内容
jar tf app.jar
# 输出：
# META-INF/
# META-INF/MANIFEST.MF
# com/
# com/example/
# com/example/App.class
```

**Spring Boot 可执行 JAR 的特殊结构：**

```text
app.jar
├── META-INF/
│   └── MANIFEST.MF          # Main-Class: org.springframework.boot.loader.JarLauncher
├── org/springframework/boot/loader/   # Spring Boot Loader 类
└── BOOT-INF/
    ├── classes/              # 应用自己的类
    └── lib/                  # 所有依赖 JAR
```

### 3.4 查看和解压 JAR

```bash
# 查看内容
jar tf app.jar
jar tf app.jar | grep "config"   # 过滤特定文件

# 解压
jar xf app.jar
jar xf app.jar META-INF/MANIFEST.MF   # 解压特定文件

# 解压到指定目录
mkdir extracted && cd extracted
jar xf ../app.jar
```

### 3.5 JAR 签名与验证

```bash
# 生成密钥库
keytool -genkey -alias mykey -keystore mykeystore -keyalg RSA

# 签名 JAR
jarsigner -keystore mykeystore app.jar mykey
jarsigner -keystore mykeystore -verify app.jar    # 验证签名

# 带时间戳签名（签名在证书过期后仍有效）
jarsigner -keystore mykeystore -tsa http://timestamp.digicert.com app.jar mykey
```

> 🎯 **核心要点**：jar 命令是 JDK 中最常用的打包工具。现代开发中以 `jar cfe` 创建可执行 JAR、`jar tf` 查看内容、结合构建工具（Maven/Gradle）管理依赖。面试中常问 MANIFEST.MF 的 `Main-Class` 和 `Class-Path` 属性。

---

## 4. jlink — 定制运行时镜像

`jlink` 是 Java 9 引入的模块化工具，可以根据应用的模块依赖，生成只包含所需模块的最小化 JRE。相比完整的 JDK/JRE，体积可减小 80-90%。

### 4.1 基本用法

```bash
# 创建一个只包含 java.base 模块的最小 JRE
jlink --module-path $JAVA_HOME/jmods \
      --add-modules java.base \
      --output mini-jre

# 结果：mini-jre/ 目录约 40MB（完整 JRE 约 300MB+）
# mini-jre/bin/java -version  仍可正常运行

# 为 Spring Boot 应用创建定制 JRE（包含常用模块）
jlink --module-path $JAVA_HOME/jmods \
      --add-modules java.base,java.logging,java.sql,java.naming,\
java.management,java.instrument,java.security.jgss,\
java.security.sasl,jdk.unsupported \
      --output spring-boot-jre \
      --strip-debug \
      --compress=2 \
      --no-header-files \
      --no-man-pages
```

### 4.2 常用参数

| 参数 | 作用 | 说明 |
|------|------|------|
| `--module-path` | 指定模块搜索路径 | 通常指向 `$JAVA_HOME/jmods` |
| `--add-modules` | 指定要包含的模块 | 用逗号分隔多个模块 |
| `--output` | 输出目录 | 生成的定制 JRE 存放路径 |
| `--strip-debug` | 去除调试信息 | 可进一步减少约 10% 体积 |
| `--compress=0/1/2` | 压缩级别 | 0: 不压缩, 1: 常量字符串共享, 2: ZIP 压缩 |
| `--no-header-files` | 不包含 C 头文件 | 减少约 5MB |
| `--no-man-pages` | 不包含 man 帮助文档 | 减少约 10MB |
| `--launcher` | 创建启动脚本 | 生成自定义启动命令 |
| `--bind-services` | 绑定服务提供者 | 用于 ServiceLoader 场景 |

### 4.3 实战：为 Spring Boot 应用创建定制 JRE

**第一步：分析应用的模块依赖**

```bash
# 使用 jdeps 分析模块依赖
jdeps --module-path $JAVA_HOME/jmods \
      --print-module-deps \
      --ignore-missing-deps \
      my-spring-boot-app.jar

# 输出示例：
# java.base,java.compiler,java.desktop,java.logging,
# java.management,java.naming,java.rmi,java.security.jgss,
# java.security.sasl,java.sql,java.xml,jdk.unsupported,
# jdk.management
```

**第二步：生成定制 JRE**

```bash
# 基于分析结果创建最小 JRE
JLINK_MODULES=$(jdeps --module-path $JAVA_HOME/jmods \
              --print-module-deps \
              --ignore-missing-deps \
              my-spring-boot-app.jar | tr -d ' ')

echo "Required modules: $JLINK_MODULES"

jlink --module-path $JAVA_HOME/jmods \
      --add-modules $JLINK_MODULES \
      --output custom-jre \
      --strip-debug \
      --compress=2 \
      --no-header-files \
      --no-man-pages

# 结果：custom-jre/ 约 45-60MB
ls -lh custom-jre/
```

**第三步：使用定制 JRE 运行应用**

```bash
# Docker 多阶段构建示例
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY target/my-app.jar .
RUN jlink --module-path $JAVA_HOME/jmods \
         --add-modules $(jdeps --print-module-deps --ignore-missing-deps my-app.jar) \
         --output /custom-jre \
         --strip-debug --compress=2 --no-header-files --no-man-pages

FROM debian:stable-slim
COPY --from=builder /custom-jre /jre
COPY --from=builder /app/my-app.jar /app.jar
CMD ["/jre/bin/java", "-jar", "/app.jar"]
```

### 4.4 体积对比

| 运行环境 | 体积 | 说明 |
|---------|------|------|
| 完整 JDK 17 | ~300 MB | 包含编译器、调试工具、全部模块 |
| 完整 JRE 17 | ~200 MB | 运行时环境，不含开发工具 |
| `jlink` 最小 JRE (java.base only) | ~35-45 MB | 只包含基础模块，Hello World 级别 |
| `jlink` Spring Boot 定制 JRE | ~50-70 MB | 包含 Web/数据库/日志等常见模块 |
| `jlink` + `--compress=2 --strip-debug` | 再减 10-20% | 生产环境推荐配置 |
| Alpine + 定制 JRE + Spring Boot | ~90 MB (Docker 镜像) | 完整的可部署镜像 |

> 💡 **jlink 的价值**：传统 Docker 镜像中携带完整 JDK（~300MB），而使用 jlink 后 Docker 镜像可缩小至 90-120MB。对于微服务架构，几十个服务的镜像体积总和从数 GB 降至几百 MB，显著加快 CI/CD 和部署速度。

> 🎯 **核心要点**：jlink 是 Java 9+ 最重要的运维工具之一。核心流程：`jdeps 分析依赖 → jlink 生成定制 JRE → Docker 多阶段构建 → 部署运行`。对于任何微服务应用，都建议在构建 pipeline 中加入这一步骤。

---

## 5. jpackage — 打包原生安装包

`jpackage` 是 Java 14 引入的打包工具，可将 Java 应用打包为原生平台的安装包（Windows 的 `.exe/.msi`、macOS 的 `.dmg`、Linux 的 `.deb/.rpm`）。

### 5.1 基本用法

```bash
# 最简单的形式：打包为平台默认格式
jpackage --name MyApp \
         --input target/ \
         --main-jar app.jar \
         --main-class com.example.Main

# Windows 上生成 MyApp.exe
# macOS 上生成 MyApp.dmg
# Linux 上生成 myapp_1.0_amd64.deb
```

### 5.2 与 jlink 配合使用

```bash
# 先创建定制 JRE，再用 jpackage 打包
jpackage --name MyApp \
         --input target/ \
         --main-jar app.jar \
         --runtime-image custom-jre/ \
         --type msi \
         --icon app-icon.ico \
         --vendor "My Company" \
         --app-version "1.0.0" \
         --description "My Java Application" \
         --copyright "2024 My Company" \
         --win-menu \
         --win-shortcut \
         --win-dir-chooser \
         --verbose
```

### 5.3 平台专属参数

| 平台 | 类型参数 | 常用选项 |
|------|---------|---------|
| Windows | `--type exe` / `--type msi` | `--win-menu`, `--win-shortcut`, `--win-dir-chooser`, `--win-upgrade-uuid` |
| macOS | `--type dmg` / `--type pkg` | `--mac-sign`, `--mac-app-category`, `--mac-bundle-identifier` |
| Linux | `--type deb` / `--type rpm` | `--linux-package-name`, `--linux-deb-maintainer`, `--linux-shortcut` |

```bash
# macOS 打包示例
jpackage --name MyApp \
         --input target/ \
         --main-jar app.jar \
         --runtime-image custom-jre/ \
         --type dmg \
         --mac-sign \
         --mac-bundle-identifier com.example.myapp \
         --icon app.icns

# Linux .deb 打包示例
jpackage --name myapp \
         --input target/ \
         --main-jar app.jar \
         --runtime-image custom-jre/ \
         --type deb \
         --linux-package-name myapp \
         --linux-deb-maintainer "admin@example.com" \
         --linux-shortcut
```

> 🎯 **核心要点**：`jpackage` 的最佳实践是与 `jlink` 结合使用——先用 jlink 创建定制 JRE，再用 jpackage 将应用和 JRE 一起打包为原生安装包。最终用户无需安装 JDK/JRE即可运行。

---

## 6. jshell — Java交互式Shell

`jshell` 是 Java 9 引入的 REPL（Read-Eval-Print Loop）工具，无需创建 .java 文件和 `main` 方法即可运行 Java 代码片段。

### 6.1 启动与基本操作

```bash
# 启动 jshell
jshell

# 启动时加载某个 jar
jshell --class-path mylib.jar

# 基本操作
jshell> System.out.println("Hello, World!");
Hello, World!

jshell> int a = 10;
a ==> 10

jshell> var list = List.of(1, 2, 3, 4, 5);
list ==> [1, 2, 3, 4, 5]

jshell> list.stream().filter(x -> x % 2 == 0).map(x -> x * x).toList();
$3 ==> [4, 16]
```

### 6.2 常用操作命令

| 命令 | 说明 | 示例 |
|------|------|------|
| `/help` | 显示帮助信息 | `/help` |
| `/list` | 列出所有输入的代码片段 | `/list` |
| `/list 1-3` | 列出第 1 到第 3 个片段 | `/list 1-3` |
| `/vars` | 列出所有变量 | `/vars` |
| `/methods` | 列出所有方法 | `/methods` |
| `/types` | 列出所有类型 | `/types` |
| `/edit` | 打开编辑器修改 | `/edit 3` |
| `/drop 3` | 删除第 3 个片段 | `/drop 3` |
| `/save history.jsh` | 保存会话到文件 | `/save history.jsh` |
| `/open history.jsh` | 加载文件中的片段 | `/open history.jsh` |
| `/classpath lib/*` | 添加 classpath | `/classpath lib/*` |
| `/reset` | 重置 jshell 状态 | `/reset` |
| `/exit` | 退出 jshell | `/exit` |
| `/!` | 重新执行上一个命令 | `/!` |

### 6.3 实战：面试和学习中的妙用

**验证 Stream API 行为：**

```java
// 直接验证 intermediate 和 terminal 操作的区别
jshell> var stream = Stream.of(1, 2, 3, 4, 5).filter(x -> {
   ...>     System.out.println("filter: " + x);
   ...>     return x > 2;
   ...> });
stream ==> java.util.stream.ReferencePipeline$2@1a2b3c4d
// 注意：filter 未执行（惰性求值！）

jshell> stream.collect(Collectors.toList());
filter: 1
filter: 2
filter: 3
filter: 4
filter: 5
$2 ==> [3, 4, 5]
// collect 时才真正执行
```

**测试正则表达式：**

```java
jshell> String text = "My email is user@example.com and backup is admin@test.org";
text ==> "My email is user@example.com and backup is admin@test.org"

jshell> var pattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
pattern ==> [a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}

jshell> pattern.matcher(text).results().map(MatchResult::group).toList();
$3 ==> [user@example.com, admin@test.org]
```

**快速验证新的语言特性：**

```java
// Record
jshell> record Point(int x, int y) {}
|  created record Point

jshell> var p = new Point(3, 4);
p ==> Point[x=3, y=4]

jshell> p.x()
$4 ==> 3

// Sealed Class
jshell> sealed interface Shape permits Circle, Rectangle {}
|  created interface Shape

jshell> record Circle(double radius) implements Shape {}
|  created record Circle

jshell> record Rectangle(double width, double height) implements Shape {}
|  created record Rectangle

jshell> var shapes = List.of(new Circle(5), new Rectangle(3, 4));
shapes ==> [Circle[radius=5.0], Rectangle[width=3.0, height=4.0]]
```

**加载外部脚本：**

```java
// 保存为 demo.jsh
// import java.util.*;
// var list = new ArrayList<>(List.of("a", "b", "c"));
// list.addAll(List.of("d", "e"));
// System.out.println("list = " + list);

jshell> /open demo.jsh
list = [a, b, c, d, e]
```

### 6.4 从 Main 方法调用 jshell

```java
import jdk.jshell.JShell;
import jdk.jshell.SnippetEvent;
import java.util.List;

public class JShellDemo {
    public static void main(String[] args) {
        try (JShell jshell = JShell.create()) {
            List<SnippetEvent> events = jshell.eval(
                "System.out.println(\"Hello from JShell API!\");"
            );
            events.forEach(e -> System.out.println(
                "Status: " + e.status() + ", Value: " + e.value()
            ));
        }
    }
}
```

> 🎯 **核心要点**：jshell 是 Java 开发者的瑞士军刀——快速验证 API 行为、测试正则表达式、尝试新语言特性、做算法题的即时代码演练。面试前用 jshell 复习 Stream/Optional/Record 等特性，比写完整的项目高效得多。

---

## 7. javadoc — 文档生成

### 7.1 注释规范

```java
/**
 * 应用入口类，负责初始化和启动整个系统。
 *
 * <p>该类使用 {@link SpringApplication#run(Class, String[])} 启动 Spring Boot 容器，
 * 自动扫描 {@code com.example} 包下的组件。</p>
 *
 * <h3>启动参数</h3>
 * <ul>
 *   <li>{@code --server.port=8080} — 指定监听端口</li>
 *   <li>{@code --spring.profiles.active=prod} — 激活生产配置</li>
 * </ul>
 *
 * @author Zhang San
 * @version 1.0.0
 * @since 2024-01-01
 * @see ApplicationConfig
 * @see <a href="https://spring.io/projects/spring-boot">Spring Boot Reference</a>
 */
@SpringBootApplication
public class Application {

    /**
     * 系统启动方法。
     *
     * @param args 命令行参数，可包含 Spring Boot 支持的参数
     * @throws RuntimeException 如果启动过程中发生无法恢复的错误
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 7.2 常用 javadoc 标签

| 标签 | 作用 | 适用位置 |
|------|------|---------|
| `@param` | 描述方法参数 | 方法 |
| `@return` | 描述返回值 | 方法 |
| `@throws` / `@exception` | 描述可能抛出的异常 | 方法 |
| `@see` | 引用其他类或方法 | 类、方法、字段 |
| `@since` | 标记引入版本 | 类、方法、字段 |
| `@version` | 版本号 | 类 |
| `@author` | 作者 | 类 |
| `@deprecated` | 标记为已废弃 | 类、方法、字段 |
| `{@link}` | 内联链接到其他类/方法 | 任意位置 |
| `{@code}` | 内联代码片段（不解析 HTML） | 任意位置 |
| `{@value}` | 引用常量值 | 字段 |
| `{@inheritDoc}` | 继承父类的文档 | 方法 |

### 7.3 生成 HTML 文档

```bash
# 基本生成
javadoc -d docs/api src/main/java/com/example/*.java

# 递归处理所有包
javadoc -d docs/api -sourcepath src/main/java -subpackages com.example

# 包含私有成员文档
javadoc -private -d docs/api src/main/java/com/example/*.java

# 指定编码和字符集
javadoc -encoding UTF-8 -charset UTF-8 -d docs/api src/main/java/com/example/*.java

# 不生成过时 API 的文档
javadoc -nodeprecated -d docs/api src/main/java/com/example/*.java

# 添加底部信息
javadoc -bottom "Copyright &copy; 2024 My Company" -d docs/api src/main/java/com/example/*.java

# 同时处理多个源文件路径
javadoc -d docs/api \
        -sourcepath src/main/java:src/test/java \
        -subpackages com.example
```

| 参数 | 作用 | 示例 |
|------|------|------|
| `-d` | 输出目录 | `-d docs/api` |
| `-sourcepath` | 源代码搜索路径 | `-sourcepath src/main/java` |
| `-subpackages` | 递归处理子包 | `-subpackages com.example.api` |
| `-private` | 包含私有成员 | `-private` |
| `-encoding` | 源文件编码 | `-encoding UTF-8` |
| `-charset` | HTML 字符集 | `-charset UTF-8` |
| `-link` | 链接外部文档 | `-link https://docs.oracle.com/en/java/javase/17/docs/api/` |
| `-version` | 包含 @version 信息 | `-version` |
| `-author` | 包含 @author 信息 | `-author` |
| `-nodeprecated` | 忽略 @deprecated | `-nodeprecated` |

```bash
# 完整的项目 doc 生成示例
javadoc -d docs/api \
        -sourcepath src/main/java \
        -subpackages com.example \
        -encoding UTF-8 \
        -charset UTF-8 \
        -windowtitle "My Application API" \
        -doctitle "My Application API Documentation" \
        -header "My App v1.0" \
        -bottom "Copyright &copy; 2024 My Company, Inc. All rights reserved." \
        -link https://docs.oracle.com/en/java/javase/17/docs/api/ \
        -version \
        -author
```

> 🎯 **核心要点**：javadoc 的质量直接影响团队协作效率。编写 API 时坚持：**所有 public 方法必须写 javadoc**，重点描述参数的约束条件、返回值范围和可能抛出的异常场景。

---

## 8. javap — 反编译

`javap` 用于查看 `.class` 文件的结构，包括类信息、方法签名、字节码指令等。是分析编译结果和排查问题的利器。

### 8.1 基本用法

```bash
# 查看类的基本信息
javap com.example.App

# 查看详细信息（包含成员变量和方法签名）
javap -verbose com.example.App

# 查看完整字节码
javap -c com.example.App

# 查看常量池
javap -verbose com.example.App | head -50

# 显示行号和局部变量表
javap -l com.example.App
```

| 参数 | 作用 | 说明 |
|------|------|------|
| (无参数) | 显示类声明和方法签名 | 查看 public 成员 |
| `-p` / `-private` | 显示所有成员（包括私有） | 比默认识别更多的成员 |
| `-c` | 显示字节码指令 | 反编译为 JVM 指令集 |
| `-verbose` | 显示详细信息（常量池、注解等） | 最完整的输出 |
| `-l` | 显示行号和局部变量表 | 配合 -c 理解源码映射 |
| `-s` | 显示内部类型签名 | 泛型相关的签名信息 |
| `-sysinfo` | 显示系统信息（路径、大小等） | 调试用 |

### 8.2 实战示例

**查看 Lambda 表达式的编译结果：**

```bash
# 源码：list.forEach(x -> System.out.println(x));
javap -c -p App.class

# 输出关键部分：
# private static void lambda$main$0(java.lang.String);
#     Code:
#        0: getstatic     #7    // Field java/lang/System.out:Ljava/io/PrintStream;
#        3: aload_0
#        4: invokevirtual #8    // Method java/io/PrintStream.println:(Ljava/lang/Object;)V
#        7: return

# 可以看出：Lambda 被编译为私有静态方法，方法名自动生成为 lambda$main$0
```

**验证泛型擦除：**

```bash
# 源码：List<String> list = new ArrayList<>();
javap -c -s App.class

# -s 参数显示签名：
# 字段签名: Ljava/util/List<Ljava/lang/String;>;
# 字节码中: Ljava/util/List;
# 说明：Signature 属性保留了泛型信息，但实际字节码中已经擦除
```

**查看 String 拼接的优化：**

```bash
# 源码：String s = "Hello, " + name + "!";
javap -c App.class

# JDK 9+ 使用 invokedynamic：
# 0: aload_1
# 1: invokedynamic #2,  0  // InvokeDynamic #0:makeConcatWithConstants
# 说明：现代 JDK 使用 invokedynamic 优化字符串拼接，非传统 StringBuilder
```

### 8.3 常见场景

```bash
# 1. 验证 Record 类的编译结果
javap -p Person.class
# 输出：Record 自动生成了构造器、accessor、equals、hashCode、toString

# 2. 验证 Sealed Class 的子类限制
javap -verbose Shape.class | grep "PermittedSubclasses"

# 3. 查看注解处理器生成的类
javap -p target/generated-sources/annotations/com/example/UserMapperImpl.class

# 4. 验证 synchronized 的实现
javap -c SynchronizedDemo.class
# 方法同步：ACC_SYNCHRONIZED flag
# 代码块同步：monitorenter / monitorexit 指令
```

> 🎯 **核心要点**：javap 是理解"Java 编译后长什么样"的最佳工具。面试前用 javap 验证泛型擦除、Lambda 实现、String 拼接优化等底层机制，比死记硬背理论更深刻。

---

## 9. jdeps — 依赖分析

`jdeps` 是 Java 8 引入的依赖分析工具，用于分析类和 JAR 包的依赖关系，尤其适合 Java 模块化迁移（JPMS）的准备工作。

### 9.1 基本用法

```bash
# 分析 JAR 文件的包级依赖
jdeps my-app.jar

# 递归分析所有依赖
jdeps -recursive my-app.jar

# 输出依赖汇总
jdeps -summary my-app.jar

# 分析特定包
jdeps -package com.example.service my-app.jar

# 分析 JDK 内部 API 的使用（迁移检查）
jdeps -jdkinternals my-app.jar
```

### 9.2 常用参数

| 参数 | 作用 | 示例 |
|------|------|------|
| `-recursive` | 递归分析所有传递依赖 | `jdeps -recursive app.jar` |
| `-summary` | 只输出依赖汇总 | `jdeps -summary app.jar` |
| `-verbose:class` | 类级别详细输出 | `jdeps -verbose:class app.jar` |
| `-filter:package` | 按包过滤 | 默认模式 |
| `-filter:archive` | 按 JAR 过滤 | `jdeps -filter:archive app.jar` |
| `-jdkinternals` | 列出对 JDK 内部 API 的依赖 | 用于 JPMS 迁移分析 |
| `--module-path` | 指定模块路径 | `jdeps --module-path lib app.jar` |
| `--print-module-deps` | 打印模块依赖（jlink 配合） | `jdeps --print-module-deps app.jar` |

### 9.3 实战：JPMS 迁移分析

```bash
# 1. 检查应用是否使用了 JDK 内部 API
jdeps -jdkinternals my-app.jar

# 输出示例：
# com.example.util.UnsafeUtils
#   -> sun.misc.Unsafe                           JDK internal API (jdk.unsupported)
# com.example.app.ClassUtil
#   -> sun.reflect.Reflection                    JDK internal API (jdk.unsupported)
#
# Warning: 依赖 sun.misc.Unsafe 和 sun.reflect.Reflection
# 建议：使用标准 API 替代，或在 module-info.java 中添加 requires jdk.unsupported

# 2. 分析模块依赖（为 jlink 做准备）
jdeps --print-module-deps --ignore-missing-deps my-app.jar
# 输出：java.base,java.logging,java.sql,java.xml

# 3. 查看模块级别依赖图
jdeps --module-path $JAVA_HOME/jmods \
      --module my-app
```

### 9.4 输出解读

```text
$ jdeps my-app.jar

my-app.jar -> java.base
my-app.jar -> java.logging
my-app.jar -> java.sql
my-app.jar -> lib/commons-lang3-3.14.0.jar
my-app.jar -> lib/gson-2.10.1.jar
   com.example.service.UserService
      -> com.example.model.User
      -> com.example.repository.UserRepository
      -> java.lang.String
      -> java.util.List
      -> org.apache.commons.lang3.StringUtils
         <lib/commons-lang3-3.14.0.jar>

   com.example.repository.UserRepository
      -> java.sql.Connection
      -> java.sql.ResultSet
      -> java.sql.SQLException
      -> lib/gson-2.10.1.jar
```

> 🎯 **核心要点**：jdeps 的两个核心实战场景：1) 配合 jlink 生成定制 JRE（`--print-module-deps`）；2) 分析内部 API 依赖（`-jdkinternals`），评估从 classpath 迁移到 module-path 的难度。

---

## 10. 其他工具

### 10.1 jarsigner — JAR 签名与验证

用于对 JAR 文件进行数字签名，确保代码的完整性和来源可信。

```bash
# 生成密钥对（已有密钥库可跳过）
keytool -genkey -alias mykey -keyalg RSA -keysize 2048 \
        -keystore mykeystore.jks -storepass changeit \
        -dname "CN=MyCompany, OU=Dev, O=MyCompany, L=Beijing, ST=Beijing, C=CN"

# 签名 JAR
jarsigner -keystore mykeystore.jks -storepass changeit \
          -tsa http://timestamp.digicert.com \
          my-app.jar mykey

# 验证签名
jarsigner -verify -keystore mykeystore.jks my-app.jar
# 输出：jar verified.  表示验证成功

# 查看签名详情
jarsigner -verify -verbose -certs my-app.jar
```

| jarsigner 参数 | 说明 |
|---------------|------|
| `-keystore` | 指定密钥库文件 |
| `-storepass` | 密钥库密码 |
| `-tsa` | 时间戳服务器 URL（使证书过期后签名仍有效） |
| `-verify` | 验证签名 |
| `-verbose` | 详细输出 |
| `-certs` | 显示证书详情 |

### 10.2 jmod — JMOD 文件工具

`jmod` 用于操作 Java 9 引入的 JMOD 模块文件格式（比 JAR 更强大，可包含本地代码、配置文件等）。

```bash
# 查看 JMOD 内容
jmod describe java.base.jmod

# 创建 JMOD
jmod create --class-path target/classes \
            --module-version 1.0.0 \
            my-module.jmod

# 列出 JMOD 内容
jmod list my-module.jmod

# JMOD vs JAR 的区别
# JMOD 可以包含：class 文件 + 本地库 + 配置文件 + 头文件 + man 手册
# JMOD 仅用于编译期和 jlink 链接，不用于运行时
```

### 10.3 keytool — 密钥和证书管理

```bash
# 生成密钥对
keytool -genkey -alias server -keyalg RSA -keysize 2048 \
        -keystore server.keystore.jks -validity 365

# 导出证书
keytool -export -alias server -keystore server.keystore.jks \
        -rfc -file server.cert

# 导入证书（信任证书）
keytool -import -alias client -file client.cert \
        -keystore truststore.jks

# 查看证书信息
keytool -list -v -keystore server.keystore.jks

# 生成 PKCS12 格式密钥库（Java 9+ 推荐）
keytool -genkey -alias mykey -keyalg RSA \
        -storetype PKCS12 -keystore mykeystore.p12
```

| keytool 常用参数 | 说明 |
|-----------------|------|
| `-genkey` | 生成密钥对 |
| `-import` | 导入证书 |
| `-export` | 导出证书 |
| `-list` | 列出密钥库内容 |
| `-delete` | 删除密钥库条目 |
| `-storetype` | 密钥库类型（JKS/PKCS12），推荐 PKCS12 |
| `-validity` | 证书有效期（天数） |

### 10.4 工具链全景图

```text
┌──────────────────────────────────────────────────────────────────┐
│                    JDK 核心工具链全景图                           │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  开发阶段:                                                        │
│  ┌───────┐  ┌───────┐  ┌─────────┐  ┌──────────┐              │
│  │ javac │─→│  jar  │─→│ jshell  │  │ javadoc  │              │
│  │ 编译   │  │ 打包   │  │ 快速验证  │  │ 文档生成   │              │
│  └───────┘  └───────┘  └─────────┘  └──────────┘              │
│       ↓                                                         │
│  运行阶段:                                                       │
│  ┌───────┐  ┌──────────┐  ┌───────────┐                        │
│  │ java  │  │ @argument │  │  -jar     │                        │
│  │ 启动   │  │ 参数文件   │  │ 可执行JAR  │                        │
│  └───────┘  └──────────┘  └───────────┘                        │
│       ↓                                                         │
│  部署阶段:                                                       │
│  ┌───────┐  ┌──────────┐  ┌────────────┐                       │
│  │ jlink │─→│ jpackage │  │ jarsigner  │                       │
│  │ 定制JRE│  │ 原生安装包  │  │ 签名验证    │                       │
│  └───────┘  └──────────┘  └────────────┘                       │
│       ↓                                                         │
│  诊断阶段:                                                       │
│  ┌───────┐  ┌───────┐  ┌─────────┐                             │
│  │ javap │  │ jdeps │  │ keytool │                             │
│  │ 反编译  │  │ 依赖分析 │  │ 证书管理   │                             │
│  └───────┘  └───────┘  └─────────┘                             │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

> 🎯 **核心要点**：JDK 自带工具链覆盖了 Java 应用的"编译 → 运行 → 验证 → 调试 → 部署"全生命周期。掌握这些工具可以：
> 1. 脱离 IDE 独立完成 Java 开发流程
> 2. 在服务器环境中快速排查问题
> 3. 优化部署体积和启动性能
> 4. 深入理解 Java 编译和运行机制（面试加分项）
>
> **学习优先级排序**：`javac` = `java` > `jar` > `jlink` > `jshell` > `javap` > `javadoc` > `jdeps` > `jpackage` > 安全工具

---

**下一模块**：[04 JDK诊断与故障排查工具](./04-JDK诊断与故障排查工具.md) | **返回总览**：[总览](./00-JDK知识体系总览.md)
