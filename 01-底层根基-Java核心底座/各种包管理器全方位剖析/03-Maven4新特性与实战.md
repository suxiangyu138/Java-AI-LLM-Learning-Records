# 03 - Maven 4 新特性与实战

> **核心摘要**：Maven 4 是 Maven 15 年来最大的一次重构（2025.10 进入 RC，2026 仍 rc-5 阶段，要求 JDK 17+）——三大核心变革：**① Build POM / Consumer POM 分离**（解决 POM 污染）**② POM 4.1.0 声明式简化**（父坐标自动推断、子项目自动发现、CI 变量原生支持）**③ 树形生命周期 + 并发构建器**（多模块提速）。生产建议等 GA，新项目可尝鲜。

> **前置阅读**：[[02-Maven深度剖析]]

---

## 📚 目录

1. [Maven 4 的版本现状](#1-maven-4-的版本现状)
2. [核心变革一：Build POM / Consumer POM 分离](#2-核心变革一build-pom--consumer-pom-分离)
3. [核心变革二：POM 4.1.0 声明式简化](#3-核心变革二pom-410-声明式简化)
4. [核心变革三：树形生命周期与并发构建](#4-核心变革三树形生命周期与并发构建)
5. [依赖管理增强](#5-依赖管理增强)
6. [开发者体验新工具](#6-开发者体验新工具)
7. [与 Gradle 的差距缩小](#7-与-gradle-的差距缩小)
8. [迁移路径与实战](#8-迁移路径与实战)
9. [常见坑与 FAQ](#9-常见坑与-faq)
10. [核心要点](#10-核心要点)

---

## 1. Maven 4 的版本现状

> **背景**：Maven 3（2013）之后长期停滞，被 Gradle 抢走大量新项目；Maven 4 于 2025.10 发布 RC，2026 年仍是 RC 阶段。
> **目的**：掌握 Maven 4 的核心变化，判断何时迁移。
> **适用范围**：Java 团队选型与升级决策（2026-2027 关键议题）。

```text
Maven 4 版本时间线（2026）
├── 2025.10  Maven 4.0.0-rc-2 发布（进入 RC）
├── 2025.11  Maven 4.0.0-rc-5（当前最新 RC）
├── 2026     持续 RC，尚未 GA（生产环境建议等待）
└── 官方态度：RC 阶段适合新项目尝鲜，生产迁移等 GA + 生态插件适配

运行要求
├── Maven 4 本身需要 JDK 17+ 运行
├── 但项目仍可编译 Java 8/11/17（编译目标与运行版本解耦）
└── 完全兼容 Maven 3 的 4.0.0 POM（存量项目平滑过渡）

IDE 支持现状
├── IntelliJ IDEA 2026.1 全面支持（Maven 4 源码目录正确识别）
└── Eclipse 支持中（m2e 适配 4.x 中）
```

> 💡 **一句话判断**：Maven 4 不是「Maven 3.10」而是「新构建工具」——核心架构变了（POM 模型 4.1.0 + Build/Consumer 分离 + 并发构建），但保留了 Maven 3 的语法兼容（4.0.0 POM 照常能构建）。

---

## 2. 核心变革一：Build POM / Consumer POM 分离

> 🎯 **Maven 4 最重要的架构变化**——彻底解决「POM 污染」（依赖方被上游的构建配置污染）。

```text
Maven 3 的痛点：POM 污染
├── 你依赖 spring-boot-starter-web
├── 它的 POM 包含：插件配置、parent 继承、私有 profile、构建逻辑
├── 你的构建器（Gradle/sbt/IDE）必须解析这些无关信息
├── 慢 + 脆 + 兼容性问题（Gradle 解析 Maven POM 各种坑）
└── 被迫用 flatten-maven-plugin 等 hack 方案

Maven 4 的方案：两个 POM
├── Build POM（本地构建用）
│   ├── 模型 4.1.0：完整插件配置 + parent + profile
│   └── 只在你构建自己项目时使用
├── Consumer POM（deploy 发布用，自动生成）
│   ├── 固定 4.0.0 模型（生态兼容）
│   ├── 剔除：所有插件/构建逻辑/parent 继承（内容内联）
│   ├── 保留：GAV 坐标 + 真实传递依赖
│   └── 属性解析为具体值（不再依赖父 POM 上下文）
└── 效果：依赖方解析更快、更安全，生态兼容性大幅提升

开启方式（rc-5 默认关闭，显式开启）
├── mvn deploy -Dmaven.consumer.pom.flatten=true
└── .mvn/maven-user.properties: maven.consumer.pom.flatten=true
```

**为什么这是「去 flatten 插件化」**：

| 维度 | Maven 3 + flatten 插件 | Maven 4 原生 |
|------|----------------------|-------------|
| 机制 | 构建后 hack 改 POM | 架构级分离 |
| 可靠性 | 插件版本兼容问题 | 原生内置 |
| 生态兼容 | 仍可能漏处理 | Gradle/sbt/Sonatype 直接可用 |
| 维护成本 | 额外插件配置 | 零配置（一行开关） |

---

## 3. 核心变革二：POM 4.1.0 声明式简化

> 🎯 **POM 模型升级到 4.1.0**（新命名空间）——把「手写样板」变成「自动推断」，终结了 2005 年以来的老需求：

```xml
<!-- ① 父 POM 坐标自动推断：子项目可省略 groupId/artifactId/version -->
<project xmlns="http://maven.apache.org/POM/4.1.0">
  <parent/>           <!-- 空 parent：通过相对路径自动解析 -->
  <artifactId>order-service</artifactId>   <!-- 其余从父推断 -->
  ...
</project>

<!-- ② 子项目自动发现：modules 更名 subprojects，可省略 -->
<project>
  <packaging>pom</packaging>
  <!-- 不写 subprojects → 自动扫描含 pom.xml 的子目录 -->
</project>

<!-- ③ CI 友好变量一等公民（无需 flatten 插件） -->
<version>${revision}</version>
<!-- 构建时：mvn -Drevision=1.2.0-SNAPSHOT package -->

<!-- ④ 多源码目录（替代 build-helper-maven-plugin） -->
<sources>
  <source path="src/main/java"/>
  <source path="src/generated/java"/>
</sources>
```

**POM 4.1.0 简化清单**：

| 简化点 | Maven 3 写法 | Maven 4 写法 |
|--------|-------------|-------------|
| 父 POM 坐标 | 完整 `<parent>` 三坐标 | 省略或空 `<parent/>` |
| 子模块列表 | `<modules>` 手写全部 | `<subprojects>` 或自动扫描 |
| 兄弟模块依赖版本 | 手写版本号 | **自动推导**（同级子模块依赖免版本） |
| CI 版本变量 | `${revision}` + flatten 插件 | 原生支持（命令行 `-Drevision=`） |
| 多源码目录 | build-helper 插件 | `<sources>` 原生标签 |
| 条件 Profile | 文件/系统属性有限判断 | `<activation><condition>` 表达式系统 |
| modelVersion | 必须写 | 可省略（自动推导） |

**条件 Profile 示例**（真正的表达式系统）：

```xml
<profiles>
  <profile>
    <id>when-file-exists</id>
    <activation>
      <condition>
        <file exists="${project.basedir}/.ci-env"/>  <!-- 文件存在判断 -->
      </condition>
    </activation>
    <!-- 多条件组合、属性比较…… -->
  </profile>
</profiles>
```

> ⚠️ **4.1.0 模型的新特性只对 4.1.0 POM 生效**——存量 4.0.0 POM 在 Maven 4 下照常构建（向后兼容），但享受不到新特性；用 `mvnup` 工具可自动升级。

---

## 4. 核心变革三：树形生命周期与并发构建

> 🎯 **Maven 3 的多模块是「线性排队」**（父全做完才做子）；**Maven 4 是「树形推进」**——子模块依赖就绪即开跑：

```text
Maven 3：线性生命周期（慢）
├── common: validate→compile→test→package→install（全部完成）
├── order:  validate→compile→test→package→install
└── user:   validate→compile→test→package→install
    （每个模块等前一个模块的全部阶段）

Maven 4：树形生命周期 + 并发构建器（快）
├── common: 每个阶段一完成 → 下游模块立刻开始
├── order:  compile 完成 → user 的 compile 可以开始
├── user:   不等 order 全部完成
└── 加上 -b concurrent（--builder concurrent）→ 多线程并行
```

```bash
# 启用并发构建器（Maven 4）
mvn -b concurrent install        # 模块就绪即开跑
mvn -T 4 -b concurrent install   # 配合 -T 线程数控制

# 新增阶段（before/after 标准前后置）
mvn before:all install           # 所有模块构建前
mvn after:all install            # 所有模块构建后
```

**新生命周期特性**：

| 特性 | 说明 |
|------|------|
| 树形推进 | 每个子项目独立推进生命周期，依赖就绪即构建下游 |
| `-b concurrent` | 并发构建器显式启用（默认仍线性，向后兼容） |
| `before:xxx / after:xxx` | 标准前后置阶段（替代旧 `pre-* / post-*` 非标准写法） |
| `all / each` 阶段 | 聚合级操作（all=全部模块，each=逐模块） |
| 构建恢复 `-r / --resume` | 失败后从失败模块续跑，自动跳过成功模块 |

> 💡 **性能结论**：大型多模块项目（10+ 模块）提速显著；小项目（3 模块以下）收益有限——并发构建的收益与模块数量正相关。

---

## 5. 依赖管理增强

> 🎯 **Maven 4 对「依赖体验」的大修**——BOM 独立化、冲突报告可操作、新 Artifact 类型：

```text
① BOM 专属打包类型
├── <packaging>bom</packaging>（BOM 与 parent POM 职责分离）
├── 更易识别：「这是版本管理模块」
└── 使用方式不变（scope=import）

② 兄弟模块依赖版本自动导入
├── 聚合项目内：子模块间依赖自动带版本
└── 不用手动维护子模块版本号（版本批量修改消失）

③ 冲突报告升级
├── 更清晰的冲突路径输出
└── 「哪个依赖引入了哪个版本」一目了然，可操作性强

④ 新 Artifact 类型（显式管控类路径与模块路径）
├── classpath-jar：进 classpath
├── module-jar：进 module path（JPMS）
├── processor / classpath-processor / modular-processor
│   └── 注解处理器精确定义加载路径（Lombok 不再乱进运行路径）
└── pom-tooling / doc / config 等新类型

⑤ 内置 Resolver 2.0
├── 150+ 修复与优化
├── 基于 Java 17 原生 HTTP 客户端（弃用 HttpURLConnection 时代代码）
└── 解析更快更稳
```

---

## 6. 开发者体验新工具

| 工具 | 作用 | 示例 |
|------|------|------|
| **mvnup** | 官方升级助手 | `mvnup check`（兼容性检查）/ `mvnup apply`（自动修复迁移） |
| **mvnsh**（Maven Shell） | 常驻进程交互式命令行 | 避免每次重复初始化，构建间共享状态 |
| **mvnenc** | 加密工具（独立 CLI） | 新增解密功能，支持外部密钥仓库 |
| `--fail-on-severity`（-fos） | 按日志级别终止构建 | `-fos WARN`：出现 WARN 就失败（CI 严格化） |
| `-P?xxx` | 可选 Profile 激活 | 指定的 Profile 不存在也不报错 |

```bash
# mvnup：官方迁移路径（GA 后主推）
mvnup check                 # 检查项目兼容性（输出问题清单）
mvnup apply                 # 自动修复：升级 POM 4.1.0 / 迁移插件配置

# 生产 CI 严格化
mvn -fos WARN verify        # 任何 WARN 级日志 → 构建失败
```

---

## 7. 与 Gradle 的差距缩小

> 🎯 **Maven 4 的每一项变革都指向「Gradle 的优势点」**——2026 选型格局正在变化：

| 能力 | Maven 3 | Maven 4 | Gradle 9 |
|------|:---:|:---:|:---:|
| 多模块并行 | ❌ 线性 | ✅ 树形 + concurrent | ✅ 增量并行 |
| 增量构建 | ❌ 全量 | ⚠️ 部分（树形避免冗余） | ✅ 精细化（Task 级） |
| 构建缓存 | ❌ | ⚠️ 起步 | ✅ 远程缓存成熟 |
| 声明式配置 | ✅ POM | ✅ POM 4.1.0（简化） | ✅ DSL/Version Catalog |
| 学习曲线 | 低 | 低（兼容） | 中高（DSL） |
| 生态兼容 | ✅ 中央仓库 | ✅ Consumer POM 更兼容 | ✅ 原生 Maven 依赖 |

**Maven 4 依然不如 Gradle 的点**：

```text
├── 增量构建：Gradle 的 Task 图增量（只重跑变化部分）仍是 Maven 短板
├── 构建缓存：Gradle 远程缓存（跨机器复用产物）领先
├── 可编程性：Maven 声明式无法表达复杂自定义逻辑（仍需插件开发）
└── 结论：Maven 4 收窄差距但未逆转——选型看团队和生态（见 [[05-Maven与Gradle选型对比]]）
```

---

## 8. 迁移路径与实战

> ⚠️ **2026 迁移建议**：生产等 GA（4.0.0-GA 发布后再切）；新项目/实验可上 RC-5。

```text
迁移五步（GA 后执行）
├── Step 1 环境准备
│   └── JDK 17+ + Maven 4.x（RC 或 GA）
├── Step 2 兼容性体检
│   └── mvnup check → 输出插件/配置问题清单
├── Step 3 升级关键插件
│   ├── maven-compiler-plugin ≥ 4.0.0-beta-3
│   ├── 显式固定所有插件版本（Maven 4 Super POM 默认插件版本有变化）
│   └── 依赖 Plexus DI 的老插件需改造为 JSR-330（Maven 4 不再兼容 Maven 2 插件）
├── Step 4 渐进启用新特性
│   ├── 先开 Consumer POM flatten（收益最大、风险最小）
│   ├── 再试并发构建器 -b concurrent
│   └── 最后升级 POM 模型到 4.1.0（mvnup apply）
└── Step 5 CI 验证
    ├── 新旧并行跑（Maven 3 vs 4）对比产物
    └── 黄金构建用例回归
```

**新项目直接上 4.1.0 的模板**：

```xml
<project xmlns="http://maven.apache.org/POM/4.1.0">
  <modelVersion>4.1.0</modelVersion>
  <parent/>
  <groupId>com.example</groupId>
  <artifactId>modern-service</artifactId>
  <version>${revision}</version>

  <!-- 兄弟依赖免版本号 -->
  <dependencies>
    <dependency>
      <groupId>com.example</groupId>
      <artifactId>common</artifactId>   <!-- 版本自动推导 -->
    </dependency>
  </dependencies>

  <!-- 多源码目录原生支持 -->
  <sources>
    <source path="src/main/java"/>
  </sources>
</project>
```

---

## 9. 常见坑与 FAQ

**Q1：Maven 4 能构建 Java 8 项目吗？**

```text
能。Maven 4 要求 JDK 17+ 运行，但 maven.compiler.release 可以指定 8/11/17
——运行版本与编译目标解耦。不过新版 compiler 插件对 Java 8 支持
建议实测（部分老插件未适配 4.x）。
```

**Q2：4.0.0 POM 的老项目能直接用 Maven 4 吗？**

```text
能。完全兼容（4.0.0 模型照常构建），只是享受不到 4.1.0 新特性。
注意：老插件（Maven 2 时代的）不兼容——用 mvnup check 体检。
```

**Q3：Consumer POM 会不会导致依赖丢失？**

```text
不会。它保留「真实传递依赖」（解析后的），只是去掉构建配置。
反而不需要的依赖（未使用的）会被剔除——依赖更干净。
但 rc-5 默认关闭，显式开启后建议验证一次产物依赖树。
```

**Q4：Maven 4 还是「声明式」的吗？和 Gradle 有本质区别吗？**

```text
是。Maven 4 仍是声明式（POM），只是把「声明」变得更聪明（推断/简化）。
与 Gradle 的本质区别不变：Maven 配置是「数据」，Gradle 配置是「代码」。
```

| 坑 | 说明 | 对策 |
|----|------|------|
| 老插件启动失败 | Plexus DI 依赖被移除 | 升级插件 / mvnup apply |
| 默认插件版本变化 | Super POM 绑定变了 | 显式固定所有插件版本 |
| `${revision}` 不生效 | 未走 4.1.0 模型 | 确认 POM 模型版本 |
| 并发构建时序问题 | 模块间隐式依赖 | 显式声明依赖关系（modulepath） |
| Consumer POM 未生效 | rc 默认关闭 | 显式 `-Dmaven.consumer.pom.flatten=true` |

---

## 10. 核心要点

> 🎯 **核心要点**：
> 1. **现状**：Maven 4 rc-5（2026），JDK 17+ 运行，生产等 GA
> 2. **架构变革**：Build POM / Consumer POM 分离——解决 POM 污染，无需 flatten 插件
> 3. **POM 4.1.0 简化**：父坐标推断/子项目自动发现/CI 变量原生/多源码目录
> 4. **树形生命周期 + `-b concurrent`**：多模块构建提速，依赖就绪即开跑
> 5. **依赖增强**：BOM 独立 packaging、冲突报告可操作、processor 类 Artifact 类型
> 6. **新工具**：mvnup 迁移助手 / mvnsh 交互式 Shell / -fos 严格失败
> 7. **与 Gradle**：差距收窄但增量构建/缓存仍落后——选型看 [[05-Maven与Gradle选型对比]]
> 8. **迁移**：等 GA + mvnup check + 关键插件升级 + 渐进启用新特性

---

**下一模块**：[04-Gradle深度剖析](04-Gradle深度剖析.md) | **返回总览**：[00-各种包管理器知识体系总览](00-各种包管理器知识体系总览.md)
