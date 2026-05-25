# JPMS 模块化 (JPMS Modular)

> Java Platform Module System 多模块 Maven 项目实践

## 项目概述

基于 Java 9+ JPMS（Java Platform Module System）的模块化编程实践项目。使用 Maven 多模块构建，演示模块声明、导出/依赖配置和模块化编译。包含根模块和一个公共服务模块（org.example.common）。

## 技术栈

| 技术 | 说明 |
|------|------|
| Java | JDK 9+（JPMS 最低要求） |
| JPMS | module-info.java 模块描述符 |
| Maven | 多模块项目管理 |
| module-path | 模块路径（替代 classpath） |

## 功能特性

- **Maven 多模块**：父 POM + 子模块管理
- **模块声明**：module-info.java 定义模块名称和依赖
- **exports 导出**：控制模块对外暴露的包
- **requires 依赖**：声明模块间的依赖关系
- **模块封装**：未 exports 的包外部不可访问

## 项目结构

```
JPMS模块化/
├── org.example.common/        # 公共服务子模块
│   ├── src/
│   │   └── main/java/
│   │       ├── module-info.java   # 模块描述符
│   │       └── ...                # 模块源码
│   ├── target/
│   └── pom.xml                    # 子模块 POM
├── pom.xml                    # 父 POM（多模块配置）
└── README.md
```

## 快速开始

```bash
# 编译所有模块
mvn compile

# 仅编译子模块
cd org.example.common
mvn compile

# 使用模块路径运行
java --module-path target/classes --module org.example.common/<主类>
```

## 核心知识点

| 知识点 | 应用 |
|--------|------|
| module-info.java | 模块描述符定义 |
| exports | 导出包给其他模块使用 |
| requires | 声明对其他模块的依赖 |
| requires transitive | 传递性依赖（依赖的模块也可被调用方使用） |
| opens | 开放包给反射访问 |
| provides ... with | 服务提供者声明（SPI） |
| uses | 服务消费者声明 |
| --module-path | 运行时模块路径参数 |
| Maven 多模块 | \<modules\> 声明与 \<parent\> 继承 |

## JPMS 关键概念

```
module com.example.myapp {
    requires com.example.common;     // 依赖 common 模块
    exports com.example.myapp.api;   // 导出 API 包
    opens com.example.myapp.model;   // 开放给反射（如 ORM）
}
```

## 注意事项

- JPMS 是 Java 9 引入的核心特性，JDK 8 及以下不支持
- module-path 和 classpath 互斥：模块化应用用 module-path
- 未命名的模块（没有 module-info.java）仍可从 classpath 加载
- Maven 多模块项目需要正确配置父子 POM 的继承关系
