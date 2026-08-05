# 03 JPMS 模块系统与 jlink 定制运行时

> JDK 9 的模块化（JPMS）重塑了 JDK 自身，jlink 让"定制运行时"成为可能——这是 SDK 工程化与容器化精简的核心技能

---

## 📚 目录

1. [模块化的动机：JDK 为什么要拆](#1-模块化的动机jdk-为什么要拆)
2. [module-info.java 与模块描述符](#2-module-infojava-与模块描述符)
3. [JDK 74 个模块与依赖图](#3-jdk-74-个模块与依赖图)
4. [jlink：定制精简运行时](#4-jlink定制精简运行时)
5. [jmods 与 JMOD 格式](#5-jmods-与-jmod-格式)
6. [JDK 25 模块导入（JEP 511）](#6-jdk-25-模块导入jep-511)
7. [模块化的工程取舍](#7-模块化的工程取舍)

---

## 1. 模块化的动机：JDK 为什么要拆

**JPMS（Java Platform Module System，JEP 261，JDK 9）** 的三大动机：

| 动机 | 问题 | 模块化解法 |
|------|------|-----------|
| 强封装 | JDK 内部 API（`sun.misc.Unsafe` 等）被广泛使用，无法演进 | 模块只导出该导出的包，内部实现彻底隔离 |
| 可靠配置 | 类路径混乱：同名类冲突、重复类版本，运行时才炸 | `requires` 显式依赖图，缺失模块启动即失败 |
| 可裁剪 | 完整 JDK 太大（桌面/数据库等组件你根本不用） | jlink 按依赖图生成最小运行时 |

```text
JDK 9 前：JDK 是一大包 rt.jar —— 全部类一个包，内部可任意访问
JDK 9+：74 个模块，每个模块声明 exports（导出）与 requires（依赖）
```

> 🎯 **核心要点**：模块化 = **"SDK 内部的可维护性"与"消费端的可裁剪性"双重升级**——这是 SDK 工程的最高形态，也是面试"JDK 9 为什么模块化"的标准答案三连（封装/可靠/裁剪）。

---

## 2. module-info.java 与模块描述符

**模块描述符** `module-info.java`——SDK/应用模块化的核心文件：

```java
// 一个自研 SDK 的模块描述符
module com.example.paysdk {
    requires java.net.http;              // 依赖：HTTP 客户端模块
    requires static com.fasterxml.jackson; // static = 编译期需要、运行时可无（可选依赖）

    exports com.example.paysdk.api;      // 导出：API 包（对外可见）
    exports com.example.paysdk.model;

    opens com.example.paysdk.internal;   // 开放：供反射访问（JSON 反序列化需要）
}
```

**模块描述符四个关键字**：

| 关键字 | 语义 | 用途 |
|--------|------|------|
| `requires` | 依赖其他模块 | 显式声明依赖关系（普通/`static`/`transitive`） |
| `exports` | 导出包（对外可见） | 只暴露 API 包，内部包不外泄 |
| `opens` | 开放包（反射可达） | 框架需要反射（ORM/JSON）时使用 |
| `uses/provides` | 服务加载 | ServiceLoader 机制（SDK 插件化） |

**模块化 vs 传统 jar 的关键差异**：

| 维度 | 传统 jar（类路径） | 模块化 jar（模块路径） |
|------|:------------------:|:---------------------:|
| 可见性 | 所有 public 类都可见 | 只有 `exports` 的包可见 |
| 依赖 | 靠 ClassNotFoundException 运行时发现 | 启动时校验依赖图（缺失即报错） |
| 冲突 | 同名类静默覆盖 | 模块名唯一，冲突启动报错 |

> 💡 只加 `module-info.java` 不改造代码 = 获得"强封装 + 启动期依赖校验"——**遗留 jar 渐进式模块化的第一步**。注意：`opens` 别乱开（反射面 = 攻击面）。

---

## 3. JDK 74 个模块与依赖图

**JDK 自身的模块布局**（`java --list-modules`）：

```text
java.base                        ← 一切模块的根（java.lang/util/io，被所有模块 requires）
├── java.logging / java.management / java.naming ...
├── java.sql / java.xml / java.transaction.xa
├── java.desktop（AWT/Swing —— 服务器端根本不用）
├── java.net.http（JDK 11+ 的 HTTP Client）
└── jdk.* 系列（javac、jcmd、jlink、jfr、zipfs...）

// java.base 为什么是根：String、Integer、Class、反射、线程 —— 无它则无 Java
java --describe-module java.base | head -20
```

**依赖图的价值**：

1. **裁剪依据**：jlink 沿着 `requires` 图只打包"用到的模块"；
2. **启动诊断**：模块缺失/循环依赖在启动时报 `ModuleResolutionException`（比 ClassNotFoundException 早且准）；
3. **体积意识**：`java.desktop` 模块占 JDK 约 40% 体积——**服务器应用从不 require 它**，jlink 自动剔除。

> 🎯 **核心要点**：理解依赖图 = 理解 jlink 为什么能裁——**"用不到的模块及其依赖不进运行时"**。查依赖用 `jdeps -s`，查模块用 `java --describe-module`。

---

## 4. jlink：定制精简运行时

**jlink（JDK 9，JEP 282）**：根据应用的模块依赖图，生成一个**只含所需模块**的精简运行时镜像。

```bash
# 准备：模块化应用（有 module-info.java）
javac -d mods --module-source-path src -m com.example.app
# 或使用 jdeps 分析非模块化 jar 的模块依赖

# jlink：生成定制运行时（输出 runtime/ 目录）
jlink --module-path $JAVA_HOME/jmods:mods \
      --add-modules com.example.app \
      --launcher app=com.example.app/com.example.Main \
      --strip-debug --compress zip-6 \
      --output runtime

# 使用定制运行时运行（无需完整 JDK！）
./runtime/bin/app            # 或 ./runtime/bin/java -m com.example.app
```

**体积对比**（典型量级）：

| 交付物 | 体积 | 说明 |
|--------|:----:|------|
| 完整 JDK 21 | ~300MB | 全部模块 + 工具 |
| 定制运行时（一个服务） | **40-60MB** | 只含实际用到的模块 |
| 定制 + strip-debug | 更小 | 去掉调试信息 |

**工程收益**：

| 场景 | 收益 |
|------|------|
| Docker 镜像 | 镜像体积大幅缩小（多阶段构建里 jlink） |
| 微服务部署 | 每服务一个最小运行时，攻击面减小 |
| 离线环境 | 无需下载完整 JDK |

```dockerfile
# Docker 多阶段构建示例：jlink 定制运行时
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN javac -d classes src/... && jlink --module-path $JAVA_HOME/jmods:classes \
      --add-modules com.example.app --output /runtime --strip-debug

FROM debian:bookworm-slim          # 第二阶段：不装 JDK！
COPY --from=build /runtime /runtime
ENTRYPOINT ["/runtime/bin/app"]
```

> 🎯 **核心要点**：jlink 是"**SDK 消费端的终极裁剪**"——从"装一个 300MB 的 JDK"变成"一个 50MB 的专用运行时"。容器化时代它是镜像精简的标配手段（配合 AOT 可达性分析，Spring Boot 4 原生镜像也走类似思路）。

---

## 5. jmods 与 JMOD 格式

**JMOD（JDK 9 新格式）**：JDK 模块的封装格式，存放在 `$JAVA_HOME/jmods/`：

```text
java.base.jmod —— 包含：
  ├── classes/       模块字节码与资源
  ├── lib/           本地库（.so/.dll）
  ├── bin/           本地工具
  ├── conf/          默认配置
  └── legal/         许可证
```

**JMOD vs JAR**：

| 维度 | JAR | JMOD |
|------|:---:|:----:|
| 用途 | 应用/库交付 | **JDK 模块的封装（仅 JDK 内部）** |
| 包含内容 | class + 资源 | class + **本地库 + 配置 + 许可证** |
| 使用场景 | 类路径/模块路径 | jlink 原料 |
| 运行 | 可运行/可依赖 | **不可直接依赖**（仅链接期使用） |

> 💡 **jmod 命令**（JDK 9+）：`jmod create/describe/list` 管理模块文件——日常开发几乎不用（它服务于 JDK 构建者与 jlink），面试知道"jlink 的原料是 jmods 目录"即可。

---

## 6. JDK 25 模块导入（JEP 511）

**JEP 511 模块导入声明（JDK 25 转正）**——简化 import 的语法糖：

```java
// 传统：import 每个类（一个模块几十个类时行数爆炸）
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;
...

// JDK 25+：一次导入整个模块的公开类型
import module java.base;        // 导入 java.base 模块所有导出包的类型

public void demo() {
    List<String> list = List.of("a");
    LocalDate date = LocalDate.now();      // 无需单独 import
}
```

**JEP 511 的要点**：

| 要点 | 说明 |
|------|------|
| 版本 | JDK 23/24 预览，**JDK 25 转正** |
| 语义 | 只导入**模块导出的包**，不导入子包 |
| 冲突处理 | 显式单类 import 优先于模块导入 |
| 适用 | 大量使用同一模块类型的**新代码**（简化样板） |

> ⚠️ 模块导入是"组织 import 的语法糖"，**不是运行时特性**——字节码层面无变化，不影响模块化语义。它让"JDK 作为 SDK 的使用体验"更简洁。

---

## 7. 模块化的工程取舍

| 取舍点 | 结论 | 说明 |
|--------|------|------|
| 应用要不要模块化？ | 一般不需要 | 非模块化 jar 在类路径上运行完全正常（Boot 默认） |
| 库/SDK 要不要模块化？ | **建议** | 提供 `module-info.java`（自动模块名 `Automatic-Module-Name` 是底线），让消费者可用模块路径 |
| 什么时候 jlink？ | 容器化部署时 | 微服务/瘦镜像需求是 jlink 的甜蜜点 |
| 模块化 vs 反射 | 冲突点 | 反射需要 `opens`——框架（Spring/Jackson）要求 jar 声明 `opens` 或自动模块 |
| 遗留 jar 怎么办？ | 自动模块 | 无 module-info 的 jar 在模块路径上成为"自动模块"（模块名 = jar 文件名） |

```java
// 库作者的底线实践：至少声明自动模块名
// META-INF/MANIFEST.MF 或 Gradle：
//   Automatic-Module-Name: com.example.paysdk
// 效果：消费者可以 requires com.example.paysdk（即使没有 module-info.java）
```

> 🎯 **核心要点**：模块化的工程判断 = **"库给模块名（Automatic-Module-Name），部署用 jlink，应用层不强求"**——模块化是 SDK 工程的进阶武器，不是日常开发的必选项（Java 高级语法系统有完整实战）。

---

**下一模块**：[04-第三方SDK集成实战](./04-第三方SDK集成实战.md) / **返回总览**：[00-SDK知识体系总览](./00-SDK知识体系总览.md)
