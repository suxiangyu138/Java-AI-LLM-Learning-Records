# 02 JDK 解剖：Java 开发套件的组成

> JDK 是 Java 平台的 SDK——解剖它的目录结构、工具家族与 JDK/JRE/JVM 关系，是"理解 SDK 组成"的最好样本

---

## 📚 目录

1. [JDK / JRE / JVM 三者关系](#1-jdk--jre--jvm-三者关系)
2. [JDK 目录结构逐层解析](#2-jdk-目录结构逐层解析)
3. [工具家族：五类工具](#3-工具家族五类工具)
4. [javac 与 java：编译-运行协作](#4-javac-与-java编译-运行协作)
5. [实用命令速查（开发日常）](#5-实用命令速查开发日常)
6. [JDK 的构成模块](#6-jdk-的构成模块)

---

## 1. JDK / JRE / JVM 三者关系

**三个缩写的关系（面试第一题）**：

```text
JVM（Java Virtual Machine）        —— 执行字节码的虚拟机（核心引擎）
JRE（Java Runtime Environment）    —— JVM + 标准库（能"运行"Java）
JDK（Java Development Kit）        —— JRE + 开发工具（能"开发"Java）
```

| 缩写 | 组成 | 能力 | 使用者 |
|------|------|------|--------|
| JVM | 类加载器/字节码执行/GC/JIT | 只执行字节码 | 运行时 |
| JRE | JVM + java.* 标准库 | 运行 .class/.jar | 运行环境 |
| JDK | JRE + javac/jar/jlink 等工具 | 开发 + 运行 | 开发者 |

**JDK 9 的关键变化：独立 JRE 消失**——JDK 9 模块化后：

- 不再单独发布 JRE（过去的 JRE 是完整 JDK 的裁剪版）；
- 运行 Java 程序的方式变为：完整 JDK 直接运行（`java` 命令），或 **jlink 定制最小运行时**（见 03 模块）；
- 原因：模块化让"按需裁剪"成为可能——jlink 替代了"预打包 JRE"这种笨办法。

> 🎯 **核心要点**：JDK 9 之前 JRE 是独立分发物；JDK 9+ **"运行时是 JDK 的产物而非配套品"**——要么带完整 JDK 跑，要么用 jlink 裁一个刚好够用的运行时。面试答"JDK9 后没有独立 JRE"是基础分，能接"用 jlink 替代"是加分分。

---

## 2. JDK 目录结构逐层解析

安装一个 JDK 21 后（以 Linux 为例）的目录结构：

```text
jdk-21/
├── bin/          ← 可执行工具（javac、java、jar、jlink、jshell、jcmd...）
├── conf/         ← 配置文件（安全策略、logging、时区、管理属性）
├── include/      ← C/C++ 头文件（JNI：写本地方法时用）
├── jmods/        ← 模块文件（74 个 .jmod，jlink 的原料）
├── legal/        ← 各组件许可证文件
├── lib/          ← 运行时库（类库镜像、静态链接库、JVM 实现组件）
├── man/          ← 手册页
├── release        ← 版本信息（JAVA_VERSION 等）
└── README 等
```

| 目录 | 内容 | 工程价值 |
|------|------|---------|
| `bin/` | 所有命令行工具 | 工具链入口（PATH 配置它） |
| `conf/` | 默认配置（`security/java.security`、`logging.properties`） | 定制安全策略、加密算法禁用 |
| `jmods/` | 模块化打包的 JDK 组件 | **jlink 定制运行时的原料**（03 模块） |
| `lib/` | `modules` 运行时镜像、`libjvm.so` 等 | 运行时的物理载体 |
| `legal/` | 许可证 | 合规检查 |

```bash
# 实用验证命令
java -version                      # 版本
java --list-modules                # 列出全部 74 个模块
ls $JAVA_HOME/jmods                # 看模块文件
cat $JAVA_HOME/release             # 版本与特性标记
```

> 💡 `JAVA_HOME` 指向的必须是**JDK 根目录**（含 bin/ 的目录），Maven/Gradle/Tomcat 都靠它定位工具链。

---

## 3. 工具家族：五类工具

| 类别 | 工具 | 用途 |
|------|------|------|
| **编译** | `javac` | 源码 → 字节码 |
| **打包与分发** | `jar`、`jmod`、`jlink`、`jdeps` | 打包/模块化/定制运行时/依赖分析 |
| **运行** | `java`、`jshell`、`java` 启动器 | 运行程序、REPL 交互 |
| **文档** | `javadoc` | 从注释生成 API 文档 |
| **诊断与监控** | `jcmd`、`jstack`、`jmap`、`jstat`、`jhsdb` | 线程栈、堆转储、GC 监控（详见 JDK/04-05） |

```bash
# 一条完整的"编译-打包-运行"链路（SDK 的消费流水线）
javac -d classes src/com/example/Hello.java          # 1. 编译
jar --create --file hello.jar --main-class com.example.Hello -C classes .   # 2. 打包
java -jar hello.jar                                   # 3. 运行
jshell                                               # 4. 交互式实验（JShell）
javadoc -d doc src/com/example/*.java                # 5. 文档生成
```

**JDK 26 的维护性变化**（时效性）：`Thread.stop()` 终于在 JDK 26（2026-03）被移除（JDK-8368226）——遗留代码调用它会 NoSuchMethodError；Applet API 也在 JDK 26 被移除（JEP 504）。

> 🎯 **核心要点**：工具链的完整闭环 = **javac（产）→ jar（装）→ java（跑）→ javadoc（说）→ jcmd 系列（医）**——JDK 既是"SDK"也是"工具箱"，五个环节覆盖开发全流程（工具细节见 `JDK/03-JDK核心工具链实战.md`）。

---

## 4. javac 与 java：编译-运行协作

**javac（编译器）与 java（启动器）的分工**——Java 程序的两个阶段：

```text
阶段一：编译期（javac）
  Hello.java → 词法/语法分析 → 语义检查（类型/受检异常） → 字节码生成
            → Hello.class（字节码 + 常量池 + 元数据）

阶段二：运行期（java）
  Hello.class → 类加载器装载 → 字节码验证 → 解释执行 + JIT 编译热点 → 输出
```

```bash
# 现代 Java 的"单文件启动"（JDK 11+，源码级运行）
java Hello.java                 # 直接运行单文件源码 —— 无需先 javac（JEP 330）

# JDK 25 更进一步（JEP 512 紧凑源文件）：无 class 声明也能跑
# Hello.java 直接写：void main() { System.out.println("hi"); }
java Hello.java                 # JEP 512：实例 main 方法 + 隐式类
```

| 工具 | 职责 | 关键参数 |
|------|------|---------|
| `javac` | 编译 + 检查（类型/受检异常/泛型） | `-source/-target/--release`、`--enable-preview`、`-d` |
| `java` | 运行 + JVM 参数 | `-cp`、`--enable-preview`、`-Xmx/-Xms`、`-XX:` 系列 |

**JEP 512 的影响**（JDK 25，时效性）："紧凑源文件与实例 main 方法"让最小 Java 程序从 6 行降到 2 行：

```java
// JDK 25 前：public class Hello { public static void main(String[] args) {...} }
// JDK 25 后：
void main() {                      // 隐式类 + 实例 main —— 教学与脚本场景更友好
    System.out.println("Hello");
}
```

> 🎯 **核心要点**：javac 保证"类型正确才放行"，java 负责"跑起来"——编译期错误前置是 Java 语言的核心哲学。JDK 25 的 JEP 512 降低了"入口仪式感"，但生产代码仍用标准 main。

---

## 5. 实用命令速查（开发日常）

| 命令 | 用途 | 示例 |
|------|------|------|
| `java -version` | 当前版本与发行版 | `java -version` |
| `java -jar app.jar` | 运行可执行 jar | 日常启动 |
| `java --enable-preview -jar app.jar` | 预览特性运行（编译运行参数必须一致） | JDK 25 实验 |
| `javac --release 17 Hello.java` | 按目标版本编译（跨版本兼容） | 21 编译 → 17 目标 |
| `jar --create --file x.jar -C classes .` | 打包 | 手工打包（日常用 Maven） |
| `java --list-modules` | 列出 JDK 模块 | 74 个 |
| `java --describe-module java.sql` | 模块细节（导出/依赖） | 模块排查 |
| `jdeps -s app.jar` | 依赖分析（jlink 前侦察） | 裁剪准备 |
| `jlink --add-modules ... --output runtime` | 定制运行时 | 镜像精简 |
| `jshell` | 交互式实验 | 快速验证 API |
| `jcmd <pid> Thread.print` | 线程栈快照 | 排查死锁（详见 JDK/04） |

```bash
# 一次完整的 SDK 消费链路（编译 → 打包 → 运行 → 诊断）
cd src && javac --release 21 -d ../classes com/example/App.java
jar --create --file app.jar --main-class com.example.App -C ../classes .
java -jar app.jar
jcmd $(pgrep -f app.jar) Thread.print        # 运行中诊断
```

> 💡 构建日常几乎都用 Maven/Gradle 包住这些命令——但**理解裸命令**才能解释"构建工具在做什么"（`mvn package` 的本质就是 javac + jar 的组合，详见 `Maven与Gradle/` 系统）。

---

## 6. JDK 的构成模块

**JDK 9 模块化后，JDK 自身被拆成 74 个模块**（`java --list-modules` 可见）——这本身就是"大 SDK 模块化设计"的教科书：

| 模块组 | 代表模块 | 职责 |
|--------|---------|------|
| java.base | **所有模块的基石** | java.lang/java.util/java.io 核心 |
| java.sql / java.xml | JDBC、XML | 数据与文档 |
| java.desktop | AWT/Swing | 桌面 GUI |
| java.net.http | HTTP Client | 现代 HTTP（HTTP/3 支持 JDK 26，JEP 517） |
| jdk.* | 编译器/诊断/打包工具 | javac、jcmd、jlink 等 |
| java.security.* | 加密/证书/策略 | 安全体系 |

**模块化对 SDK 的意义**：

1. **强封装**：模块只导出该导出的包（`exports`）——JDK 内部实现不再可随意访问（`jdk.internal.*` 黑箱）；
2. **可裁剪**：jlink 按模块依赖图裁剪（03 模块）；
3. **依赖显式**：`requires` 声明模块依赖——JDK 依赖关系从"隐式一大包"变成"显式一张图"。

```bash
java --describe-module java.sql       # 查看模块的导出包与依赖
java --list-modules | wc -l           # 74
jdeps --module-path . -s app.jar      # 分析应用模块依赖（裁剪前的侦察）
```

> 🎯 **核心要点**：JDK 模块化 = **"SDK 内部工程化"的巅峰示范**——强封装（exports）、显式依赖（requires）、可裁剪（jlink）。理解 JDK 模块化，等于理解了大型 SDK 的组织原则（实战细节见 03 模块与 `Java高级语法/06-模块系统JPMS.md`）。

---

**下一模块**：[03-JPMS模块系统与jlink定制运行时](./03-JPMS模块系统与jlink定制运行时.md) / **返回总览**：[00-SDK知识体系总览](./00-SDK知识体系总览.md)
