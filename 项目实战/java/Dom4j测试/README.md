# Dom4j 测试 (Dom4j Test)

> 使用 Dom4j 库操作 XML：解析、XPath 查询、文档操作

## 项目概述

基于 Dom4j（Document for Java）库的 XML 处理实践项目。Dom4j 是 Java 生态中最流行的 XML 处理库之一，提供比原生 DOM API 更简洁高效的 API。本项目练习 Dom4j 的 XML 解析、XPath 查询和文档操作功能。

## 技术栈

| 技术 | 说明 |
|------|------|
| Java | JDK 8+ |
| Maven | 项目管理与 Dom4j 依赖 |
| Dom4j | XML 解析、遍历、修改 |
| Jaxen | XPath 表达式支持 |
| JUnit | 单元测试（src/test） |

## 功能特性

- **Dom4j 解析**：SAXReader 读取 XML 文件
- **XPath 查询**：使用 XPath 表达式精准定位节点
- **XML 遍历**：Iterator + Visitor 模式遍历文档树
- **节点操作**：增删改查 XML 元素和属性
- **格式化输出**：OutputFormat + XMLWriter 美化输出
- **单元测试**：JUnit 测试 XML 操作正确性

## 项目结构

```
Dom4j测试/
├── src/
│   └── test/
│       └── java/
│           └── ...            # 测试类
├── target/                    # Maven 构建输出
├── books.xml                  # 书籍信息测试数据
├── student.xml                # 学生信息测试数据
├── pom.xml                    # Maven 配置（dom4j + jaxen 依赖）
└── README.md
```

## 快速开始

```bash
# 编译
mvn compile

# 运行测试
mvn test
```

## 核心知识点

| 知识点 | 应用 |
|--------|------|
| SAXReader | 创建 Dom4j 读取器，解析 XML 文件 |
| Document.selectNodes() | XPath 查询多个节点 |
| Document.selectSingleNode() | XPath 查询单个节点 |
| Element.addElement() | 动态添加子元素 |
| Element.addAttribute() | 动态添加属性 |
| OutputFormat | 设置 XML 输出格式（缩进、编码） |
| XMLWriter | 将 Document 写入文件或输出流 |
| Visitor Pattern | 遍历文档树的所有节点 |

## Dom4j vs 原生 DOM 对比

| 特性 | Dom4j | 原生 DOM |
|------|-------|----------|
| API 简洁度 | 高（链式调用、便捷方法） | 低（繁琐工厂创建） |
| XPath 支持 | 内置 | 需额外 API |
| 性能 | 较好 | 一般 |
| 依赖 | 需引入 dom4j.jar | 无外部依赖 |
| 学习曲线 | 低 | 中等 |

## 注意事项

- XPath 需要引入 jaxen 依赖，否则 selectNodes 不可用
- Dom4j 的 SAXReader 默认使用 SAX 解析器（事件驱动），不依赖 DOM
- OutputFormat.createPrettyPrint() 可生成缩进格式化的 XML 输出
