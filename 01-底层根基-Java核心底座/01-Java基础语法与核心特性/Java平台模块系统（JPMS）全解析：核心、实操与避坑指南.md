# Java 平台模块系统（JPMS）全解析：核心、实操与避坑指南

> **定位**：JPMS（Java Platform Module System，即 Project Jigsaw）是 Java 9 推出的重量级模块化规范，核心目标是**强化代码封装、精准依赖管理、简化大型项目维护、提升安全性与可扩展性**，彻底解决传统"类路径地狱"问题。

---

## 目录

1. [核心基础概念](#1-核心基础概念)
2. [module-info.java 详解](#2-module-infojava-详解)
3. [完整开发流程](#3-完整开发流程)
4. [实战关键要点](#4-实战关键要点)
5. [高频异常与避坑](#5-高频异常与避坑)
6. [适用场景与建议](#6-适用场景与建议)

---

## 1. 核心基础概念

### 1.1 什么是模块

> 模块（Module）是比包（Package）更大一级的代码组织单元，一组内聚的包、资源、配置的集合，拥有明确的边界和对外暴露规则。

### 1.2 JPMS 解决的痛点

| 痛点 | 传统类路径 | JPMS 方案 |
|------|-----------|-----------|
| 依赖冲突 | 同名类存在，无序加载 → `NoSuchMethodError` | 模块隔离，编译期检测 |
| 封装缺失 | 所有类均可访问 | 未导出包完全不可访问（含反射） |
| 隐式依赖 | 依赖关系不透明 | 显式 `requires` 声明 |
| JDK 臃肿 | 全量 JRE | JDK 模块化拆分，按需引入（`jlink` 构建精简 JRE） |
| 循环依赖 | 运行时才暴露 | 编译期检测 |

### 1.3 模块分类

| 类型 | 来源 | 命名 | 示例 |
|------|------|------|------|
| **平台模块** | JDK 自身拆分 | `java.*` 开头 | `java.base`、`java.sql`、`java.net.http` |
| **应用模块** | 开发者编写 | 自定义反向域名 | `com.example.user` |
| **自动模块** | 传统非模块化 Jar → 模块路径 | Jar 文件名（去版本号） | `mybatis-3.5.13.jar` → `mybatis` |
| **未命名模块** | 类路径下所有内容 | — | 兼容老项目 |

> `java.base` 是所有模块自动依赖的基础模块，无需显式声明。

---

## 2. module-info.java 详解

> 模块描述符是 JPMS 核心，放在模块根目录，一个模块有且仅有一个。

### 2.1 核心指令速查

| 指令 | 语法 | 作用 |
|------|------|------|
| **声明模块** | `module 模块名 { }` | 定义模块，命名用反向域名 |
| **导出包** | `exports 包名;` | 对外暴露包（所有模块可访问） |
| **定向导出** | `exports 包名 to 模块1, 模块2;` | 仅指定模块可访问 |
| **依赖模块** | `requires 模块名;` | 声明依赖 |
| **传递依赖** | `requires transitive 模块名;` | 依赖当前模块的模块自动继承该依赖 |
| **静态依赖** | `requires static 模块名;` | 编译时需要，运行时可选 |
| **反射开放** | `opens 包名;` | 允许反射访问 |
| **定向反射** | `opens 包名 to 模块;` | 仅指定模块可反射访问 |
| **服务发现** | `uses 接口;` | 声明使用的服务接口 |
| **服务注册** | `provides 接口 with 实现;` | 注册服务实现类 |

### 2.2 完整模块描述符示例

```java
module com.example.user {
    // 导出业务接口包（所有模块可访问）
    exports com.example.user.service;

    // 定向导出实体包（仅 web 模块可访问）
    exports com.example.user.entity to com.example.web;

    // 传递依赖（依赖本模块的模块自动获得 java.sql）
    requires transitive java.sql;

    // 普通依赖
    requires java.logging;

    // 定向反射开放（允许 MyBatis 反射实体类）
    opens com.example.user.entity to org.mybatis;

    // 服务发现与注册
    uses com.example.user.service.UserService;
    provides com.example.user.service.UserService
        with com.example.user.service.impl.UserServiceImpl;
}
```

---

## 3. 完整开发流程

### 3.1 项目结构

```text
src/
├── main/
│   ├── java/
│   │   ├── module-info.java       ← 模块描述符（核心）
│   │   └── com/example/
│   │       ├── service/           ← 导出包（接口）
│   │       ├── entity/            ← 定向导出 + 反射开放
│   │       └── impl/              ← 内部实现，不导出
│   └── resources/
└── test/
```

### 3.2 Maven 配置

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.12.1</version>
    <configuration>
        <release>17</release>
        <encoding>UTF-8</encoding>
        <compilerArgs>
            <arg>--module-path</arg>
            <arg>${project.build.directory}/modules</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

### 3.3 模块路径 vs 类路径

| 特性 | 模块路径（Module Path） | 类路径（Class Path） |
|------|------------------------|---------------------|
| 依赖管理 | ✅ 有序、显式声明 | ❌ 无序、隐式 |
| 封装性 | ✅ 强封装，未导出不可访问 | ❌ 所有类可访问 |
| 依赖检查 | ✅ 编译期检查 | ❌ 运行期报错 |
| 兼容性 | 兼容自动模块、未命名模块 | 仅传统 Jar |

---

## 4. 实战关键要点

### 4.1 自动模块兼容传统 Jar

```text
传统 Jar（mybatis-3.5.13.jar）→ 放入模块路径 → JVM 自动生成模块名 mybatis
在 module-info.java 中：requires mybatis;
```

### 4.2 反射访问处理

> Spring、MyBatis、Hibernate 等框架大量依赖反射，JPMS 默认禁止反射未导出包。

| 方案 | 做法 | 推荐度 |
|------|------|:------:|
| `opens` 指令 | `opens com.example.entity to org.mybatis;` | ⭐⭐⭐ |
| JVM 参数 | `--add-opens 模块/包=目标模块` | ⭐（临时） |

### 4.3 传递依赖简化

```text
模块 A → requires 模块 B → requires transitive 模块 C
                            ↓
                        模块 A 自动获得模块 C 的访问权
```

### 4.4 测试模块

```java
module com.example.user.test {
    requires com.example.user;
    requires static org.junit.jupiter.api;
    opens com.example.user to org.junit.jupiter.api;
}
```

---

## 5. 高频异常与避坑

| 异常 | 原因 | 解决方案 |
|------|------|----------|
| `ModuleNotFoundException` | `requires` 声明的模块不存在或 Jar 未在模块路径 | 检查模块名拼写，确认 Jar 在模块路径 |
| `PackageNotAccessibleException` | 访问了未 `exports` 导出的包 | 添加 `exports` 或定向导出 |
| `InaccessibleObjectException` | 框架反射访问未 `opens` 的包 | 添加 `opens` 定向开放给框架 |
| `CyclicDependencyException` | 模块 A 依赖 B，B 又依赖 A | 拆分公共模块，打破循环 |
| 自动模块冲突 | 多个 Jar 同名或版本冲突 | 重命名 Jar，排除冲突，优先模块化版本 |

---

## 6. 适用场景与建议

| 适合 | 暂不推荐 |
|------|----------|
| 大型企业级项目、微服务模块拆分 | 小型简单项目、快速原型 |
| 安全性、封装性要求高的项目 | 大量老旧未维护 Jar 依赖 |
| 需要精简运行时（`jlink`）的项目 | 兼容成本过高的遗留系统 |
| 框架底层开发 | — |

**核心原则**：`显式声明 + 最小权限 + 渐进兼容`

---

> 🎯 **核心总结**：JPMS 是 Java 从松散类路径走向规范化模块化的关键升级。`module-info.java` 是整个模块化体系的核心，通过自动模块和未命名模块实现对传统项目的完美兼容。新项目优先模块化，老项目渐进改造。
