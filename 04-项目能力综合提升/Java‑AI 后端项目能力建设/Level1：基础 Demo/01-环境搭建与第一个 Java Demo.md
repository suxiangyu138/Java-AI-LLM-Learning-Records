# 01 环境搭建与第一个 Java Demo

> Level1 的第一块地基：装对 JDK、配好 IDEA、跑通 Maven，写出第一个能独立重写的 Java 程序。环境问题占 Demo 期 60% 的报错，这一步值得一次做对。

## 📚 目录

1. [目标与验收](#1-目标与验收)
2. [JDK 安装与版本选择](#2-jdk-安装与版本选择)
3. [IDEA 与 Maven 配置](#3-idea-与-maven-配置)
4. [第一个 Java 程序](#4-第一个-java-程序)
5. [环境自检清单](#5-环境自检清单)
6. [常见坑](#6-常见坑)
7. [核心要点](#7-核心要点)

---

## 1. 目标与验收

本 Demo 的产出：本机能编译运行 Java 程序、IDEA 能创建并运行 Maven 项目、控制台能看到第一个输出。验收标准三条：**命令行 `java -version` 输出正确版本**；**IDEA 新建 Spring Boot 项目能启动**（依赖能下载、端口能监听）；**能不看文档独立重写一遍 Hello World**。后一条看起来简单，但它是检验"环境是你的还是教程的"的分水岭。

版本基线（2026-08）：JDK 25 LTS（2025 年 9 月发布，最新补丁 25.0.4，2026-07）；如果机器上已有 JDK 21 LTS 也可以，Spring Boot 4.x 要求 Java 21+，两个版本都能用，统一即可。JDK 26 是 2026 年 3 月的非 LTS 版本，除非尝鲜，不建议 Demo 期使用。

## 2. JDK 安装与版本选择

安装三条纪律：**只装 LTS**——JDK 25 或 21，企业生产与教程生态都以 LTS 为主，非 LTS（26/27）半年就换，学了半年语法全变不是收获是浪费；**只装一个**——多 JDK 混装是环境混乱的第一来源，先卸载旧的再装新的；**用安装包不用绿色版**——Oracle 或 Temurin 安装包会自动配好 `JAVA_HOME` 与 PATH，绿色版漏配环境变量的坑能卡你半天。

Windows 安装步骤：下载 Temurin JDK 25 安装包（.msi）→ 一路下一步 → 装完打开 PowerShell 输入 `java -version`，看到 `openjdk version "25.x"` 即成功。常见失败是系统 PATH 里残留旧 JDK 路径——安装后先重启终端再验证，仍不对就在"系统环境变量"里删掉旧 JAVA_HOME。

```powershell
java -version
# openjdk version "25.0.4" 2026-07-21   ← 目标输出
javac -version
# javac 25.0.4                         ← 编译器也要能输出
```

`javac` 验证很重要：只装了 JRE 或 PATH 配错时 `java` 能用但 `javac` 报错，Maven 编译阶段会全部失败。

## 3. IDEA 与 Maven 配置

IDEA 下载 Community 版即可（旗舰版免费试用到期后反而干扰学习节奏）。装完做三件事：**配置 JDK**（File → Project Structure → SDK → 选 JDK 25，确认 SDK 与 Language Level 一致）；**配置 Maven**（Settings → Build Tools → Maven：Maven home path 指向内置 Maven 或自己下载的 3.9.x，配置文件指向仓库镜像——这一步决定依赖下载速度，国内必须配阿里云镜像）；**安装必要插件**（Lombok、Spring Boot Helper 可选，Level1 阶段尽量少装插件，插件出问题的排查成本高于收益）。

Maven 镜像配置（`~/.m2/settings.xml`）：仓库地址填 `https://maven.aliyun.com/repository/public`，镜像所有中央仓库。验证方式：新建项目后观察依赖下载速度，几十个依赖两分钟内下完即为正常；卡住不动或报 SSL 错误，优先检查 settings.xml 语法与网络。

## 4. 第一个 Java 程序

不用 IDEA 新建，先用命令行写一个最原始的 Java 程序，体会"没有框架的世界"——这一步的体验能让你在后续理解 Spring Boot 时更有参照物。任意目录建 `Hello.java`：

```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello Java-AI Level1");
    }
}
```

```bash
javac Hello.java   # 编译生成 Hello.class
java Hello         # 运行，输出 Hello Java-AI Level1
```

两个要点：**文件名必须与 public 类名一致**（`Hello.java` 对应 `class Hello`），大小写敏感；`javac` 与 `java` 必须在同一目录执行（classpath 默认当前目录）。跑通后删掉编译产物，再独立写一遍——能不看上面代码写出来，这个 Demo 才真正完成。

**从命令行到 Maven 的跨越**值得此刻建立：命令行编译暴露了"编译运行"的本质（javac 编译、java 运行、classpath 找类）；Maven 把这三步抽象成**生命周期**——`mvn clean` 清产物、`mvn compile` 编译、`mvn package` 打包（jar）、`mvn test` 跑测试。Maven 的价值不是省那三条命令，而是**依赖管理**：pom.xml 声明依赖，Maven 自动下载并管理版本（这就是 01 篇配置镜像的意义）。理解生命周期对排障有用：报错发生在哪个阶段（编译期 vs 运行期），排查方向完全不同——编译期报错查语法与依赖，运行期报错查逻辑与配置。

## 5. 环境自检清单

换目录、换机器、装完所有工具后，按固定顺序自检，任何一个失败都在同一环节排查，不跳步：

| 检查项 | 命令/操作 | 通过标准 |
|---|---|---|
| JDK 路径 | `where java` | 指向 JDK 安装目录，不是其他软件内置 |
| 版本 | `java -version` | 25.x 或 21.x LTS |
| 编译器 | `javac -version` | 与 java 同版本号 |
| Maven | `mvn -v` | 输出 Java 版本与 Maven 版本 |
| 依赖下载 | 新建 Maven 项目刷新 | 无报错，速度正常 |
| IDEA 运行 | 运行任意 main 方法 | 控制台输出正常，无编码乱码 |
| 端口占用 | `netstat -ano \| findstr 8080` | 启动 Spring Boot 前确认 8080 空闲 |

最后一条是 Level1 高频坑：Windows 上 8080 常被其他程序占用（老项目、Docker、WSL），启动报"Port 8080 was already in use"时先查这一条，而不是怀疑代码。

## 6. 常见坑

**版本混装**：机器上同时有 JDK 8/11/17/21，PATH 指到旧的，`java -version` 显示 8 但 IDEA 里配了 25——所有报错都变得不可理解。解决：环境变量里只保留一个 JAVA_HOME。

**Maven 下载慢或失败**：默认中央仓库在国外。先配阿里云镜像，再不行换网络（校园网/手机热点），不要反复重装 Maven。

**中文乱码**：控制台输出中文变问号，通常是 IDEA 文件编码不是 UTF-8。Settings → File Encodings 三处全部设 UTF-8，console 输出编码与项目编码一致。

**`javac` 不是内部命令**：只配了 JRE 或 JAVA_HOME 没配 PATH。检查系统环境变量，补上 JDK 的 bin 目录。

**杀毒软件拦截**：Windows Defender 可能拦截 Maven 下载的临时文件，出现莫名其妙的文件访问异常时，把 Maven 本地仓库目录加入排除项。

**IDEA 两个必学操作**：**Shift × 2**（万能搜索——搜类、搜文件、搜操作，比在目录树里翻快十倍）；**Ctrl + Shift + F10**（运行当前文件——选中 main 方法所在文件按这个组合键直接运行）。再加三个高频快捷键：`Ctrl + P`（看方法参数提示）、`Alt + Enter`（万能修复——报错红波浪线时按下它，多数错误自动修）、`Ctrl + Alt + L`（格式化代码——提交前必按）。快捷键不必一次全背，先用这五个，两周后自然内化成手指记忆。

**Maven 本地仓库的认知**也顺手建立：依赖下载到本机 `~/.m2/repository`，按"groupId/artifactId/version"三级目录存放。这个目录的两个实用操作：**清理**——依赖损坏或半截下载导致奇怪报错时，删掉对应目录让 Maven 重新下载；**换源**——`settings.xml` 里改镜像后，已下载的依赖不会重下，新依赖走新源。**`mvn -v` 报错找不到 Java**：Maven 用自己的 JAVA_HOME 找 JDK，如果 Maven 是单独安装的，确认它的 JAVA_HOME 指向你装的 JDK 25——这是"java -version 正常但 mvn 报错"的常见原因。

## 7. 核心要点

1. 只装一个 LTS JDK（25 或 21），用安装包装，装完重启终端验证。
2. `java` 与 `javac` 都要验证，Maven 必须配国内镜像。
3. 第一个程序用命令行写，体会无框架世界，作为理解 Spring 的参照物。
4. 环境自检清单按顺序过，不跳步；8080 端口占用是高频坑。
5. 验收标准是独立重写，不是跑通——环境是你的还是教程的，一试便知。

> 🎯 **核心要点**：环境搭建阶段的最大价值不是"装好工具"，而是**建立排除法的直觉**——报错先查环境（版本/端口/编码/网络），再查代码。这个排查顺序会贯穿 Level1 到 Level3 的所有调试。

---

**下一模块**：[02 Java 核心语法快速 Demo](./02-Java%20核心语法快速%20Demo.md) | **返回总览**：[Level1 总览](./00-Level1%20基础%20Demo%20总览.md)
