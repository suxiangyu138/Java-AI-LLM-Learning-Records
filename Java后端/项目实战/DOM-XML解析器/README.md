# DOM XML 解析器 (DOM XML Parser)

> Java DOM API 实现 XML 文件解析与生成

## 项目概述

使用 Java 标准库 `org.w3c.dom` 和 `javax.xml.parsers` 实现对 XML 文件的解析与生成。包含两个核心功能：从 XML 文件中读取结构化数据（DOM 解析）、将 Java 对象数据写入 XML 文件（DOM 生成）。使用学生（student.xml）和教师（teacher.xml）数据作为示例。

## 技术栈

| 技术 | 说明 |
|------|------|
| Java | JDK 8+ |
| DOM API | org.w3c.dom（Document, Element, NodeList） |
| JAXP | javax.xml.parsers（DocumentBuilderFactory） |
| javax.xml.transform | XML 输出格式化与写入 |

## 功能特性

- **DOM 解析**：加载 XML → 解析树结构 → 提取节点数据
- **DOM 生成**：创建 Document → 构建 Element 树 → 写入 XML 文件
- **属性与子节点**：处理 XML 元素属性（如 `id`）和子元素内容
- **格式化输出**：设置缩进和编码（UTF-8）

## 项目结构

```
DOM-XML解析器/
├── src/
│   ├── DomXmlParser.java      # DOM 解析器（读取 XML）
│   └── DomXmlGenerator.java   # DOM 生成器（创建 XML）
├── out/                       # 编译输出
├── student.xml                # 学生信息测试数据
├── teacher.xml                # 教师信息测试数据
├── DomXmlParser.iml           # IntelliJ IDEA 模块文件
└── README.md
```

## 快速开始

```bash
# 编译
javac -encoding UTF-8 -d out src/DomXmlParser.java src/DomXmlGenerator.java

# 运行解析器（需在项目根目录执行，因为读取 student.xml）
cd DOM-XML解析器
java -cp out DomXmlParser

# 运行生成器
java -cp out DomXmlGenerator
```

## 核心知识点

| 知识点 | 应用 |
|--------|------|
| DocumentBuilderFactory | 创建 DOM 解析器工厂 |
| DocumentBuilder | 解析 XML 文件为 Document 对象 |
| Document.getDocumentElement() | 获取根节点 |
| Element.getElementsByTagName() | 按标签名获取子元素列表 |
| Node.getNodeType() | 判断节点类型（ELEMENT_NODE / TEXT_NODE） |
| NodeList | 遍历子节点集合 |
| Element.getAttribute() | 获取元素属性值 |
| TransformerFactory | XML 格式化输出到文件 |

## XML 解析方式对比

| 方式 | 特点 | 适用场景 |
|------|------|----------|
| **DOM** | 内存中构建完整树，可随机访问 | 小文件、需修改 XML |
| **SAX** | 事件驱动、流式读取、低内存 | 大文件、只读遍历 |
| **StAX** | 拉模式、按需解析、可控性好 | 大文件、需灵活控制 |

## 注意事项

- DOM 解析会将整个 XML 加载到内存，不适合处理超大文件（>10MB）
- 文件路径使用相对路径时，需注意 Java 的工作目录
- XML 文件编码需与解析时指定编码一致（默认 UTF-8）
