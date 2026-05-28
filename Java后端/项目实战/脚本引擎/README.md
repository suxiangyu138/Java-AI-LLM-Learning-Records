# 脚本引擎 (Script Engine)

> Java 脚本引擎实践：动态执行 JavaScript、运行时编译 Java、自定义注解验证

## 项目概述

Java 动态编程综合实践项目，包含三大核心模块：
1. **脚本引擎**：使用 JSR 223 ScriptEngine API 在 Java 中执行 JavaScript 代码
2. **动态编译**：使用 javax.tools.JavaCompiler 运行时编译 Java 源文件
3. **注解处理**：自定义 @NotNull 注解及运行时验证器

## 技术栈

| 技术 | 说明 |
|------|------|
| Java | JDK 8+（Nashorn）或 JDK 15+（GraalVM JS） |
| Maven | 项目构建管理 |
| JSR 223 ScriptEngine | Java 内嵌脚本引擎标准 API |
| javax.tools.JavaCompiler | 运行时动态编译 Java 源码 |
| Java Reflection | 注解读取与运行时处理 |
| Custom Annotation | 自定义注解定义与验证逻辑 |

## 功能特性

- **JavaScript 执行**：在 Java 中调用 JS 脚本，传递变量与获取结果
- **动态编译**：运行时编译 .java 文件并加载执行
- **@NotNull 注解**：自定义约束注解，标记不可为空字段
- **注解验证器**：运行时反射扫描注解并校验字段值
- **注解测试**：验证自定义注解的正确性

## 项目结构

```
脚本引擎/
├── src/
│   └── main/
│       └── java/
│           ├── ScriptDemo.java                 # 脚本引擎演示
│           ├── DynamicCompileDemo.java          # 动态编译演示
│           └── org/example/
│               ├── Main.java                   # 入口程序
│               ├── AnnotationTest.java         # 注解测试
│               ├── NotNull.java                # 自定义 @NotNull 注解
│               └── NotNullValidator.java       # 注解验证器
├── out/
├── target/
├── Hello.java                   # 根目录测试类
├── pom.xml
└── README.md
```

## 快速开始

```bash
# Maven 编译
mvn compile

# 运行脚本引擎演示
java -cp target/classes ScriptDemo

# 运行动态编译演示
java -cp target/classes DynamicCompileDemo

# 运行注解测试
java -cp target/classes org.example.AnnotationTest
```

## 核心知识点

| 知识点 | 应用 |
|--------|------|
| ScriptEngineManager | 获取脚本引擎工厂 |
| ScriptEngine.eval() | 执行脚本字符串或 Reader |
| ScriptContext | 脚本变量绑定（Bindings） |
| javax.tools.JavaCompiler | 运行时获取 Java 编译器 |
| JavaFileObject | 内存中的 Java 源文件表示 |
| URLClassLoader | 加载动态编译的 .class 文件 |
| @Retention / @Target | 注解元定义 |
| Reflection | getDeclaredFields() + getAnnotation() |
| Nashorn → GraalVM JS | JDK 15+ 脚本引擎迁移 |

## 注意事项

- JDK 8-14 使用 Nashorn 引擎，JDK 15+ 需单独引入 GraalVM JS 依赖
- 动态编译需要 JDK 环境（tools.jar 或 java.home 下存在编译器）
- 自定义注解需设置 `@Retention(RetentionPolicy.RUNTIME)` 才能在运行时读取
- ScriptEngine 执行外部脚本时注意安全问题，避免执行不可信代码
