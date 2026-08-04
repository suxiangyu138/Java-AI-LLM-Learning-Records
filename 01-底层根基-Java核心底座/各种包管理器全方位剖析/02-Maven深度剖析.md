# 02 - Maven 深度剖析

> **核心摘要**：Maven 是 Java 生态的声明式构建之王——**POM 声明 + 生命周期驱动 + 插件扩展**。理解三件事就理解了 Maven：① GAV 坐标与 POM 模型 ② 生命周期（compile/test/package/install/deploy）③ 依赖机制（传递/scope/冲突就近裁决）。

> **前置阅读**：[[01-核心概念与原理]]

---

## 📚 目录

1. [Maven 的定位与哲学](#1-maven-的定位与哲学)
2. [POM：项目模型文件](#2-pom项目模型文件)
3. [生命周期：构建的阶段化](#3-生命周期构建的阶段化)
4. [依赖机制](#4-依赖机制)
5. [scope 依赖范围](#5-scope-依赖范围)
6. [仓库体系](#6-仓库体系)
7. [插件体系](#7-插件体系)
8. [常用配置实战](#8-常用配置实战)
9. [常见问题排查](#9-常见问题排查)
10. [核心要点](#10-核心要点)

---

## 1. Maven 的定位与哲学

> **背景**：2002 年诞生，解决 Ant（纯脚本、无约定）的痛点；2005 年 Maven 2 确立 POM 模型，此后 15 年未大改。
> **目的**：理解「约定优于配置」的声明式哲学。
> **适用范围**：Java 后端项目——Maven 仍是国内团队主流（2026 存量迁移至 Maven 4 中）。

```text
Maven 的核心哲学：约定优于配置（Convention over Configuration）
├── 目录结构约定：src/main/java、src/test/java（不用配！）
├── 生命周期约定：compile → test → package（顺序固定）
├── 命名约定：groupId:artifactId:version（全球唯一）
└── 你只需要声明「差异」，其余 Maven 帮你做

Maven 能做什么（构建全生命周期）
├── 依赖管理（最核心）：下载/传递/冲突裁决
├── 构建：编译/测试/打包/部署
├── 多模块：聚合（一次构建全部子模块）+ 继承（父 POM 统一配置）
└── 可扩展：2000+ 插件（打包/发布/代码检查/文档……）
```

---

## 2. POM：项目模型文件

> 🎯 **POM（Project Object Model）= 项目的「完整说明书」**——所有构建信息都声明在这里。

```xml
<!-- pom.xml 核心结构（Maven 3.x 风格） -->
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <!-- ① GAV 坐标：你是谁 -->
  <groupId>com.example</groupId>
  <artifactId>order-service</artifactId>
  <version>1.2.0</version>
  <packaging>jar</packaging>          <!-- jar/war/pom/bom -->

  <!-- ② 继承：父 POM 提供公共配置 -->
  <parent>
    <groupId>com.example</groupId>
    <artifactId>parent-pom</artifactId>
    <version>1.0.0</version>
    <relativePath>../parent/pom.xml</relativePath>
  </parent>

  <!-- ③ 依赖：你需要什么 -->
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
  </dependencies>

  <!-- ④ 属性：变量定义 -->
  <properties>
    <java.version>17</java.version>
    <maven.compiler.source>17</maven.compiler.source>
  </properties>

  <!-- ⑤ 插件：构建时做什么 -->
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.13.0</version>
      </plugin>
    </plugins>
  </build>
</project>
```

**POM 继承的三种形式**（多模块项目核心）：

| 形式 | 机制 | 场景 |
|------|------|------|
| **父 POM 继承** | 子模块 `<parent>` 声明 | 统一版本/插件配置（全家桶） |
| **BOM 导入** | `scope=import` + dependencyManagement | 只拿版本管理，不拿插件（如 spring-boot-dependencies） |
| **聚合** | 父 POM `<modules>` 声明 | 一次命令构建全部子模块 |

```xml
<!-- 聚合父 POM（构建所有子模块） -->
<modules>
  <module>common</module>
  <module>order-service</module>
  <module>user-service</module>
</modules>

<!-- BOM 导入（只引入版本约束，Spring Boot 专用方式） -->
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-dependencies</artifactId>
      <version>3.4.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

> ⚠️ **常见误解**：`dependencyManagement` 不引入依赖（只约束版本）；`dependencies` 才真正引入——BOM 导入后仍需在 `dependencies` 里声明依赖（可省版本号）。

---

## 3. 生命周期：构建的阶段化

> 🎯 **生命周期 = 预定义的阶段序列**——执行 `mvn package` 会自动跑完它前面的所有阶段。

```text
Maven 三套生命周期（各自独立，互不干扰）
├── clean 生命周期：clean（清理 target）
├── default 生命周期：核心构建流程（最重要的）
└── site 生命周期：site（生成文档站点）

default 生命周期（完整阶段链，常用加粗）
├── validate（校验配置） → compile（编译主代码）
├── test-compile（编译测试） → test（跑测试）
├── package（打包 jar/war） → verify（集成验证）
├── install（装进本地仓库） → deploy（部署到远程仓库）
└── 金句：阶段 = 前置阶段的总和——install 包含 test、package 包含 compile

常用命令速查
├── mvn clean             # 清理 target
├── mvn compile           # 只编译
├── mvn test              # 编译 + 测试
├── mvn package -DskipTests   # 打包（跳过测试）
├── mvn install           # 装到本地仓库（多模块内部依赖）
├── mvn deploy            # 发布到私有仓库
└── mvn -pl order-service -am  # 只构建指定模块 + 其依赖模块
```

**多模块构建的执行顺序**（关键行为）：

```text
mvn install（在聚合父目录执行）
├── ① 按模块依赖拓扑排序（common → order-service → user-service）
├── ② 每个模块跑完整 default 生命周期
├── ③ 上游模块 install 到本地仓库 → 下游模块从本地仓库解析
└── 坑：下游模块跑不过 → 上游必须先 install（-am 参数自动处理）
```

> 💡 **Maven 3 的多模块是「串行 + 线性阶段」**——父模块所有阶段完成后才轮到子模块（效率问题在 Maven 4 用树形生命周期 + 并发构建器解决，见 [[03-Maven4新特性与实战]]）。

---

## 4. 依赖机制

> 🎯 **依赖机制是 Maven 的灵魂**——坐标声明 + 自动传递 + 冲突就近裁决。

```xml
<dependency>
  <groupId>com.fasterxml.jackson.core</groupId>
  <artifactId>jackson-databind</artifactId>
  <version>2.17.2</version>
  <scope>compile</scope>          <!-- 依赖范围（默认 compile） -->
  <optional>false</optional>       <!-- 可选依赖（不进传递） -->
  <exclusions>                     <!-- 排除传递依赖 -->
    <exclusion>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-api</artifactId>
    </exclusion>
  </exclusions>
</dependency>
```

**传递依赖与就近裁决**（必考机制）：

```text
项目 P → A(1.0)，P → B，B → A(2.0)
│
├── 依赖树：
│   P
│   ├── A 1.0      ← 直接依赖（深度 1）
│   └── B
│       └── A 2.0  ← 传递依赖（深度 2）
│
└── 就近原则裁决：A 1.0 胜出（离根近）
    ├── B 拿到的实际是 1.0（可能缺 B 需要的 API）
    └── 运行时错误：NoSuchMethodError / ClassNotFoundException

排查命令（开发必备）
├── mvn dependency:tree            # 打印完整依赖树
├── mvn dependency:tree -Dverbose  # 含被忽略的版本
├── mvn dependency:analyze         # 找未使用/缺失依赖
└── mvn dependency:get -Dartifact=...  # 手动拉取
```

**冲突解决的完整姿势**：

```text
① 先看树：mvn dependency:tree 定位冲突
② 确定保留版本：业务需要哪个 API 行为
③ 二选一处理：
   ├── 排除：<exclusions> 排除被忽略的版本
   └── 统一：dependencyManagement 强制指定版本
④ 验证：重新构建 + 跑测试（重点看运行时兼容）
⑤ 固化：用 enforcer 插件禁止未来冲突再出现
```

```xml
<!-- 统一版本（推荐：一处声明，全项目生效） -->
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
      <version>2.17.2</version>
    </dependency>
  </dependencies>
</dependencyManagement>
```

---

## 5. scope 依赖范围

> 🎯 **scope 决定依赖「何时可用、是否传递」**——用错 scope 是常见生产事故来源（如测试依赖打进生产包）。

| scope | 编译 | 测试 | 运行 | 是否传递 | 典型依赖 |
|-------|:---:|:---:|:---:|:---:|---------|
| `compile`（默认） | ✅ | ✅ | ✅ | ✅ | 业务库 |
| `provided` | ✅ | ✅ | ❌ | 否 | Servlet API、Lombok |
| `runtime` | ❌ | ✅ | ✅ | ✅ | JDBC 驱动 |
| `test` | ❌ | ✅ | ❌ | 否 | JUnit、Mockito |
| `system` | ✅ | ✅ | ❌ | 否 | 本机 jar（已废弃，勿用） |
| `import` | — | — | — | — | 仅限 dependencyManagement 导入 BOM |

```text
典型错误案例
├── ❌ JUnit 写成 compile → 测试依赖打进生产 jar（体积膨胀 + 泄露测试代码）
├── ❌ JDBC 驱动写成 compile → 换数据库要重新打包（应为 runtime）
├── ❌ Lombok 不写 provided → 编译期注解处理器进入生产（启动报错）
└── ✅ 正确姿势：Lombok=provided / JDBC=runtime / 测试库=test
```

---

## 6. 仓库体系

> 🎯 **仓库是依赖的来源和去向**——理解「本地 → 远程」的查找顺序就理解了 Maven 的速度问题。

```text
查找顺序（依赖解析时）
├── ① 本地仓库 ~/.m2/repository（有就直接用，不联网）
├── ② 远程仓库配置（settings.xml 里 mirrors/proxies）
│   ├── 中央仓库（默认）：repo.maven.apache.org
│   ├── 镜像：阿里云 maven.aliyun.com（国内必备）
│   └── 私有仓库：Nexus（代理中央 + 托管私包）
└── ③ 找到 → 下载到本地缓存 → 下次直接用

-SNAPSHOT 快照版的特殊行为
├── 每次构建检查远程（updatePolicy 默认 daily）
├── 确保拿到「最新开发版」
└── 发布版是「不可变」的（本地有就不查远程）

常用 settings.xml 配置
├── <mirrors> 镜像（加速）
├── <servers> 私有仓库认证（id/username/password）
├── <localRepository> 本地仓库路径（换盘符/CI 共用）
└── <profiles> 环境切换（如 JDK 版本 profile）
```

```xml
<!-- settings.xml：核心三件套 -->
<settings>
  <localRepository>D:/maven-repo</localRepository>

  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central</mirrorOf>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>

  <servers>
    <server>
      <id>nexus-releases</id>
      <username>deploy</username>
      <password>${env.NEXUS_PASSWORD}</password>
    </server>
  </servers>
</settings>
```

> ⚠️ **镜像只代理不托管**：mirror 拦截的是「访问中央仓库的请求」；私有包（SNAPSHOT）要配 `<repositories>`/`<distributionManagement>` 指向 Nexus——两者经常混用导致「私包拉不下来」。

---

## 7. 插件体系

> 🎯 **插件 = 生命周期的「执行者」**——生命周期阶段本身不干活，绑定的插件才干活。

```text
插件与生命周期绑定
├── 默认绑定（Super POM 内置）：compile→compiler 插件、test→surefire……
├── 自定义绑定：<executions> 把插件目标绑到指定阶段
└── 2000+ 插件：打包/检查/发布/覆盖率……

常用插件清单
├── maven-compiler-plugin    编译（Java 版本）
├── maven-surefire-plugin    单元测试执行
├── maven-failsafe-plugin    集成测试（verify 阶段）
├── maven-shade-plugin       打 fat jar（可执行）
├── maven-assembly-plugin    自定义打包（带依赖）
├── spring-boot-maven-plugin 可执行 jar + 资源处理
├── maven-enforcer-plugin    规则强制（JDK/依赖冲突/版本）
├── maven-checkstyle-plugin  代码规范
├── maven-scm-publish-plugin 发布
└── cyclonedx-maven-plugin   SBOM 生成（供应链安全）
```

```xml
<!-- 常见配置：compiler + enforcer + checkstyle -->
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <version>3.13.0</version>
      <configuration>
        <release>17</release>
      </configuration>
    </plugin>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-enforcer-plugin</artifactId>
      <version>3.5.0</version>
      <executions>
        <execution>
          <goals><goal>enforce</goal></goals>
          <configuration>
            <rules>
              <requireJavaVersion><version>[17,)</version></requireJavaVersion>
              <dependencyConvergence/>  <!-- 依赖版本收敛检查 -->
            </rules>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

---

## 8. 常用配置实战

**① 统一版本管理（无 BOM 时的规范）**：

```xml
<properties>
  <!-- 集中管理版本号（升级只改一处） -->
  <spring.version>6.2.0</spring.version>
  <jackson.version>2.17.2</jackson.version>
  <lombok.version>1.18.34</lombok.version>
</properties>
<!-- 依赖里引用：<version>${spring.version}</version> -->
```

**② 多环境构建（dev/test/prod）**：

```xml
<!-- 用 Profile 切换环境配置 -->
<profiles>
  <profile>
    <id>prod</id>
    <properties><env>prod</env></properties>
    <activation>
      <activeByDefault>false</activeByDefault>
    </activation>
  </profile>
</profiles>
<!-- 使用：mvn package -Pprod  →  resource 过滤注入 application-prod.yml -->
```

**③ 跳过测试的三种方式**（各有语义）：

```bash
mvn package -DskipTests        # 编译测试但不执行（测试代码仍编译）
mvn package -Dmaven.test.skip=true  # 连测试代码都不编译（快但失去保护）
mvn package -Dsurefire.failIfNoSpecifiedTests=false  # 有测试就执行
```

**④ 私有仓库发布**：

```xml
<distributionManagement>
  <repository>
    <id>nexus-releases</id>
    <url>http://nexus.example.com/repository/maven-releases/</url>
  </repository>
  <snapshotRepository>
    <id>nexus-snapshots</id>
    <url>http://nexus.example.com/repository/maven-snapshots/</url>
  </snapshotRepository>
</distributionManagement>
```

---

## 9. 常见问题排查

| 现象 | 根因 | 解法 |
|------|------|------|
| 依赖下载慢/卡死 | 未配国内镜像 | settings.xml 配阿里云镜像 |
| `Could not resolve` | 坐标写错/私仓没配 | 核对 GAV + `<repositories>` 配置 |
| 本地有 jar 还报 404 | SNAPSHOT 版本被删 | 清 `.m2` 对应目录重下 |
| `NoSuchMethodError` | 冲突就近裁决降级了版本 | dependency:tree 排查 + 排除/统一 |
| `Duplicate class` | 多版本共存 | exclusions 排除 + 统一 |
| 多模块构建失败 | 下游找不到上游 | 上游先 `mvn install` 或加 `-am` |
| 测试全跑但打不了包 | 资源文件缺失 | 检查 `src/main/resources` + resource 配置 |
| 莫名其妙用了旧版本 | 本地仓库缓存了 SNAPSHOT | `-U` 强制更新快照 |

**调试三板斧**：

```bash
mvn -X                     # 调试日志（看下载/解析全过程）
mvn dependency:tree        # 依赖树（90% 依赖问题第一步）
mvn help:effective-pom     # 看合并继承后的真实 POM
```

---

## 10. 核心要点

> 🎯 **核心要点**：
> 1. **POM = 项目说明书**：GAV 坐标 + 父继承/BOM + 依赖 + 插件
> 2. **生命周期阶段化**：validate→compile→test→package→install→deploy，命令含前置阶段
> 3. **依赖自动传递** + 就近裁决——冲突排查先用 `dependency:tree`
> 4. **scope 是边界**：Lombok=provided、JDBC=runtime、测试库=test
> 5. **仓库顺序**：本地 → 镜像 → 中央/私有；国内必配阿里云镜像
> 6. **插件执行构建**：compiler/surefire/shade/enforcer 是核心五件套
> 7. **enforcer 强制规则**：依赖收敛 + JDK 版本，从源头防冲突
> 8. **Maven 4 已重构**：POM 4.1.0 / Build-Consumer POM / 并发构建——见下一模块

---

**下一模块**：[03-Maven4新特性与实战](03-Maven4新特性与实战.md) | **返回总览**：[00-各种包管理器知识体系总览](00-各种包管理器知识体系总览.md)
