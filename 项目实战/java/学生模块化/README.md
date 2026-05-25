# 学生模块化 (Student Modular)

> Java JPMS 模块化编程实践：模块分离、model 模块 + main 应用

## 项目概述

基于 Java JPMS（Java Platform Module System）的模块化编程项目。将一个学生管理系统拆分为两个独立模块：模型模块（com.example.model）和主应用模块。通过模块间依赖和导出机制，实现真正意义上的模块化开发。

## 技术栈

| 技术 | 说明 |
|------|------|
| Java | JDK 9+ |
| JPMS | module-info.java 模块描述符 |
| IntelliJ IDEA | 开发工具（.iml 模块配置） |

## 功能特性

- **模块分离**：com.example.model（模型）和主应用模块各自独立
- **Student 实体类**：定义学生数据模型
- **模块依赖**：主应用通过 requires 引入 model 模块
- **模块导出**：model 模块 exports 对外暴露的包
- **IntelliJ 支持**：使用 IntelliJ 模块化项目结构

## 项目结构

```
学生模块化/
├── com.example.model/                 # 模型模块
│   ├── src/
│   │   ├── com/example/model/
│   │   │   └── Student.java           # 学生实体类
│   │   └── Main.java                  # 模块入口
│   └── com.example.model.iml
├── src/
│   └── Main.java                      # 主应用入口
├── out/                               # 编译输出
├── student-modular-idea.iml           # IDEA 主模块配置
└── README.md
```

## 快速开始

```bash
# 编译 model 模块
javac -d out/model com.example.model/src/com/example/model/Student.java \
      com.example.model/src/module-info.java

# 编译主应用（指定 module-path）
javac --module-path out -d out/main src/Main.java src/module-info.java

# 运行
java --module-path out -m <主模块名>/Main
```

或者在 IntelliJ IDEA 中直接配置模块依赖后运行。

## 核心知识点

| 知识点 | 应用 |
|--------|------|
| 模块化项目结构 | 每个模块独立目录，含 module-info.java |
| exports | model 模块导出 com.example.model 包 |
| requires | 主应用声明依赖 model 模块 |
| module-path | javac/java 的 --module-path 参数 |
| IntelliJ 模块 | .iml 文件与 JPMS 模块的对应关系 |
| 强封装 | 未 exports 的包外部模块无法访问 |

## 注意事项

- JDK 8 不支持 JPMS，需使用 JDK 9+
- module-info.java 必须放在模块源码根目录
- IntelliJ 的模块系统与 JPMS 模块是两个层面，可以同时存在
- 编译时需使用 `--module-path` 而非 `-cp`（classpath）
