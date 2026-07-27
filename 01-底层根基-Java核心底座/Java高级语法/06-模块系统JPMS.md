# 06 - 模块系统 JPMS

> 🎯 Java 9 引入的模块系统(JPMS)改变了 Java 的封装边界——从"所有 public 都可见"到"显式声明导出哪些包"。虽然大多数项目不需要自定义模块，但理解模块化是读懂 JDK 源码和解决 ClassNotFoundException 的关键

---

## 目录

1. [模块系统基础](#1-模块系统基础)
2. [module-info.java](#2-module-infojava)
3. [Spring Boot 中的模块化](#3-spring-boot-中的模块化)

---

## 1. 模块系统基础

```text
没有模块系统（Java 8）：
  jar A → 所有 public 类都可被 jar B 访问
  → 封装形同虚设（反射 + setAccessible 随便访问）

有模块系统（Java 9+）：
  module A → 显式声明 exports 哪些包
  → 未 exports 的 public 类 = 模块外部不可见
  → 反射也无法绕过（除非 --add-opens）
```

### JDK 自身的模块化

```bash
# 查看 JDK 模块
java --list-modules
# java.base    ← 最核心（java.lang, java.util, java.io...）
# java.sql
# java.xml
# jdk.net
# ...

# 查看模块导出的包
java --describe-module java.sql
```

## 2. module-info.java

```java
// module-info.java（必须放在 src 根目录）
module com.example.myapp {
    // 1. 声明依赖
    requires java.sql;              // 需要 JDK 模块
    requires spring.boot;           // 需要第三方模块
    requires transitive spring.web; // 传递依赖（用到我的也要有 spring.web）

    // 2. 声明导出
    exports com.example.myapp.api;      // 开放给其他模块
    exports com.example.myapp.dto
                to com.example.client;   // 只给指定模块

    // 3. 声明服务
    provides com.example.spi.Plugin       // 我提供的服务
        with com.example.myapp.MyPlugin;  // 实现类

    // 4. 允许反射
    opens com.example.myapp.model         // 反射可访问
          to spring.beans;                // 只允许 Spring 反射
}
```

## 3. Spring Boot 中的模块化

```text
Spring Boot 项目通常不需要自定义 module-info.java

原因：
  → Spring Boot 使用自动模块路径（classpath）
  → 大多数依赖没有 module-info.java
  → 强加模块化会导致大量 --add-opens 配置

但理解模块化仍然重要：
  → JDK 17+ 加强了封装：java.xml, java.sql 等默认不暴露内部
  → 反射操作 JDK 内部类可能报 InaccessibleObjectException
  → 需要 JVM 参数: --add-opens java.base/java.lang=ALL-UNNAMED
```

```bash
# Spring Boot 常见的 JVM 模块参数
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
--add-opens java.base/java.lang.reflect=ALL-UNNAMED
```

## 核心要点回顾

- JPMS = 显式声明依赖 + 精确控制导出
- `exports` 控制编译时可见，`opens` 控制反射可见
- Spring Boot 项目通常不用 `module-info.java`（类路径模式）
- JDK 17+ 反射内部类 → 需要 `--add-opens`
- 面试价值 > 实际使用价值（问了就是加分项）

## 参考资料

1. JEP 261: Module System
2. Java 9 Modularity — O'Reilly
