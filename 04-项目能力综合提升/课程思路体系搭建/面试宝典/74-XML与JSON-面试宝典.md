# XML与JSON 面试宝典
> 基于课程大纲全面覆盖 XML/JSON 数据交换格式面试高频考点，从基础概念到 Java 生态实战一网打尽

## 目录
1. [一、基础概念速答](#一基础概念速答)
2. [二、深度原理剖析](#二深度原理剖析)
3. [三、实战场景题](#三实战场景题)
4. [四、手写代码题](#四手写代码题)
5. [五、系统设计题](#五系统设计题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板)
8. [八、快速查漏补缺 Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答

### Q1：什么是 XML？它的核心作用是什么？
> XML（eXtensible Markup Language，可扩展标记语言）是一种用于存储和传输数据的标记语言。核心作用：**数据交换格式**、**配置描述**、**结构化文档表示**。

### Q2：XML 声明包含哪些部分？
```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
```
> | 属性 | 含义 | 是否必须 |
> |------|------|----------|
> | `version` | XML 版本号（1.0 / 1.1） | 必须 |
> | `encoding` | 字符编码（UTF-8 / GBK 等） | 可选 |
> | `standalone` | 是否独立（yes/no） | 可选 |

### Q3：XML 的约束方式有哪几种？有什么区别？
| 特性 | DTD | XSD（XML Schema） |
|------|-----|-------------------|
| 语法 | 非 XML 语法（自定义） | XML 语法（本身是 XML 文档） |
| 命名空间 | 不支持 | 支持 |
| 数据类型 | 有限（仅 10 种） | 丰富（string, int, date 等 40+） |
| 可扩展性 | 差 | 强 |
| 使用场景 | 简单配置文件 | 复杂数据校验、WebService（SOAP） |
> 💡 **面试金句**：XSD 是 DTD 的替代品，功能更强大，但 DTD 仍在一些老旧项目中存在。

### Q4：DTD 如何定义元素和属性？
```xml-dtd
<!ELEMENT 元素名称 (子元素列表)>
<!ATTLIST 元素名称 属性名 类型 默认值>
```
```xml
<!-- 内部 DTD 示例 -->
<!DOCTYPE 书籍 [
  <!ELEMENT 书籍 (书名,作者,价格)>
  <!ELEMENT 书名 (#PCDATA)>
  <!ELEMENT 作者 (#PCDATA)>
  <!ELEMENT 价格 (#PCDATA)>
  <!ATTLIST 书籍 ISBN CDATA #REQUIRED>
]>
```

### Q5：XSD 的基本语法是什么样的？
```xml
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <xs:element name="book">
    <xs:complexType>
      <xs:sequence>
        <xs:element name="title" type="xs:string"/>
        <xs:element name="author" type="xs:string"/>
        <xs:element name="price" type="xs:decimal"/>
      </xs:sequence>
      <xs:attribute name="isbn" type="xs:string" use="required"/>
    </xs:complexType>
  </xs:element>
</xs:schema>
```

### Q6：什么是命名空间（Namespace）？为什么需要它？
> 命名空间用于区分不同来源中同名的元素或属性，避免命名冲突。通过 URI 唯一标识。
```xml
<根 xmlns:ns1="http://example.com/schema1"
     xmlns:ns2="http://example.com/schema2">
  <ns1:书>Java</ns1:书>
  <ns2:书>Python</ns2:书>
</根>
```

### Q7：什么是 XSLT？能做什么？
> XSLT（eXtensible Stylesheet Language Transformations）是一种将 XML 文档转换为其他格式（如 HTML、纯文本、其他 XML）的语言。常用于**将 XML 数据渲染为 Web 页面**。
```xml
<!-- XSLT 片段示例：将 XML 转为 HTML 表格 -->
<xsl:template match="/">
  <html><body>
    <table border="1">
      <tr><td><xsl:value-of select="book/title"/></td></tr>
    </table>
  </body></html>
</xsl:template>
```

### Q8：XML 解析的方式有哪些？核心区别是什么？
| 特性 | DOM | SAX | StAX |
|------|-----|-----|------|
| 处理模型 | 树结构（内存中构建完整树） | 事件驱动（顺序读取） | 游标/迭代器（拉取式） |
| 读写能力 | 读 + 写 | 只读 | 读 + 写 |
| 访问方式 | 随机访问 | 顺序访问 | 顺序访问 |
| 内存占用 | 高（整个文档载入内存） | 低（流式处理） | 低（流式处理） |
| 速度 | 中 | 快 | 快 |
| 适用场景 | 小文档、频繁修改 | 大文档、仅读取 | 大文档、需要写入 |
> 🎯 **总结**：DOM 吃内存但功能全；SAX 速度快但不能回头；StAX 是 SAX 的改进版，开发者主动拉取事件。

### Q9：什么是 JSON？JSON 支持哪些数据类型？
> JSON（JavaScript Object Notation）是一种轻量级数据交换格式。支持的数据类型：
> - **string**：字符串（双引号包裹）
> - **number**：数字（整数/浮点数）
> - **boolean**：`true` / `false`
> - **null**：空值
> - **object**：对象（`{ }`）
> - **array**：数组（`[ ]`）

### Q10：JSON 的基本语法结构？
```json
{
  "name": "张三",
  "age": 25,
  "isStudent": false,
  "courses": ["Java", "Python"],
  "address": {
    "city": "北京",
    "zip": "100000"
  },
  "remark": null
}
```

### Q11：XML 和 JSON 各自的优缺点是什么？
| 对比维度 | XML | JSON |
|----------|-----|------|
| 可读性 | 标签冗余，较长 | 简洁，人机均易读 |
| 数据体积 | 大（标签重复） | 小（键值对结构） |
| 解析速度 | 相对慢 | 相对快 |
| 数据类型 | 无原生类型，需 Schema 约束 | 原生支持 string/number/boolean/null/array/object |
| 元数据/注释 | 支持注释、属性(attribute) | 不支持注释，无属性概念 |
| 命名空间 | 支持 | 不支持 |
| Schema 约束 | DTD / XSD（强大） | JSON Schema（相对较新） |
| 浏览器原生 | 需解析 | 原生支持（`JSON.parse`） |
| 适用场景 | 复杂文档、配置、SOAP、XSLT | Web API、移动端、NoSQL |
> 💡 **面试高频题**："什么时候用 JSON 不用 XML？" -- 简单数据传输用 JSON，需要 Schema 约束和数据校验用 XML。

### Q12：Java 中常见的 JSON 解析库有哪些？
| 库 | 特点 | 优点 | 缺点 |
|----|------|------|------|
| Jackson | 社区活跃，Spring Boot 默认 | 性能高、功能全、支持注解 | API 略复杂 |
| Gson | Google 出品 | API 简洁、toJson/fromJson 易用 | 大文件性能不如 Jackson |
| Fastjson | 阿里巴巴出品 | 速度极快、API 简单 | 安全漏洞历史较多 |
| JSON-B / JSON-P | Jakarta EE 标准 | 标准化、与 JAX-RS 集成 | 功能相对有限 |

### Q13：Jackson 如何序列化/反序列化？
```java
// 序列化：对象 -> JSON
ObjectMapper mapper = new ObjectMapper();
String json = mapper.writeValueAsString(user);

// 反序列化：JSON -> 对象
User user = mapper.readValue(json, User.class);

// 常用配置
mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
```

### Q14：Gson 如何序列化/反序列化？
```java
// 序列化
Gson gson = new Gson();
String json = gson.toJson(user);

// 反序列化
User user = gson.fromJson(json, User.class);

// 自定义配置（GsonBuilder）
Gson gson = new GsonBuilder()
    .setDateFormat("yyyy-MM-dd")
    .setPrettyPrinting()
    .serializeNulls()
    .create();
```

### Q15：Fastjson 如何序列化/反序列化？
```java
// 序列化
String json = JSON.toJSONString(user);

// 反序列化
User user = JSON.parseObject(json, User.class);

// 指定特性
String json = JSON.toJSONString(user, SerializerFeature.PrettyFormat);
```

### Q16：XML 中 CDATA 的作用是什么？
> CDATA（Character Data）用于包裹那些本应被 XML 解析器视为标记的文本。**CDATA 内的内容不会被解析**。
```xml
<content><![CDATA[if (a < b && b > c) { // 不解析 < 和 > }]]></content>
```

### Q17：什么是 XPath？常见的 XPath 表达式有哪些？
> XPath 是一种在 XML 文档中查找信息的语言。**路径表达式** 用于定位节点。
| 表达式 | 含义 |
|--------|------|
| `/root/child` | 从根节点到子节点 |
| `//name` | 所有名为 name 的节点（任意位置） |
| `@id` | 选取属性 id |
| `./text()` | 当前节点的文本内容 |
| `//book[price>30]` | 所有 price 大于 30 的 book 节点 |
| `//book[position()<3]` | 前两个 book 节点 |

### Q18：JSON Schema 的作用是什么？
> JSON Schema 用于描述 JSON 数据的结构、类型和约束规则，类似于 XSD 之于 XML。
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "name": {"type": "string"},
    "age": {"type": "integer", "minimum": 0}
  },
  "required": ["name"]
}
```

### Q19：什么是 SPI 机制？在 Java XML 解析中如何应用？
> SPI（Service Provider Interface）是 JDK 内置的服务发现机制。Java 解析 XML 时通过 SPI 加载具体的解析器实现（如 Xerces、Crimson）。
```java
// SPI 核心：META-INF/services/ 目录下配置接口实现类
// javax.xml.parsers.DocumentBuilderFactory -> org.apache.xerces.jaxp.DocumentBuilderFactoryImpl
DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // SPI 加载
```

### Q20：JAXB 的核心注解有哪些？
| 注解 | 作用 |
|------|------|
| `@XmlRootElement` | 指定 XML 根元素 |
| `@XmlElement` | 将字段映射为 XML 元素 |
| `@XmlAttribute` | 将字段映射为 XML 属性 |
| `@XmlTransient` | 忽略该字段，不参与序列化 |
| `@XmlAccessorType` | 设置访问方式（FIELD / PROPERTY） |
| `@XmlJavaTypeAdapter` | 自定义类型转换器 |

---

## 二、深度原理剖析

### Q1：DOM 解析的内存占用如何计算？为什么说它不适合大文件？
> DOM 会将整个 XML 文档解析为树状结构（Node Tree），每个节点都是一个对象。
> - 内存占用 ≈ **文档字节数 × 5~10 倍**（含节点对象、属性、父子关系等额外开销）
> - 一个 100MB 的 XML 文件在 DOM 下可能消耗 500MB~1GB 内存
> - **OOM 风险高**，所以大文件场景禁止使用 DOM

### Q2：SAX 解析的 ContentHandler 有哪些核心回调方法？
```java
public class MyHandler extends DefaultHandler {
    @Override
    public void startDocument() { /* 文档开始 */ }
    @Override
    public void endDocument() { /* 文档结束 */ }
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs) {
        /* 开始标签 */
    }
    @Override
    public void endElement(String uri, String localName, String qName) {
        /* 结束标签 */
    }
    @Override
    public void characters(char[] ch, int start, int length) {
        /* 文本内容 */
    }
}
```
> ⚠️ **注意**：`characters()` 可能被多次回调（同一个文本内容可能在多次中返回），需要拼接处理。

### Q3：StAX 基于"拉"模型与 SAX 的"推"模型有何本质区别？
| 维度 | SAX（推模型） | StAX（拉模型） |
|------|-------------|---------------|
| 控制权 | 解析器主导，回调开发者 | 开发者主导，主动获取事件 |
| 编程模型 | 回调式（被动） | 迭代器/游标式（主动） |
| 状态管理 | 需手动维护状态变量 | 局部变量即可，更自然 |
| 双向操作 | 只读 | 支持写入（`XMLEventWriter`） |
> 💡 **一句话**：SAX 是解析器"推"事件给你，StAX 是你"拉"事件。

### Q4：StAX 的两种读取方式：Cursor 和 Iterator 有什么区别？
| 特性 | Cursor（游标式） | Iterator（迭代器式） |
|------|-----------------|-------------------|
| 返回类型 | `int` 常量（`START_ELEMENT` 等） | `XMLEvent` 对象 |
| 灵活性 | 低，需 switch-case | 高，可获取事件完整信息 |
| 易用性 | 性能稍高 | API 更友好 |
| 推荐场景 | 高性能要求 | 大多数场景 |
```java
// Cursor 方式
XMLStreamReader reader = factory.createXMLStreamReader(input);
while (reader.hasNext()) {
    int event = reader.next();
    if (event == XMLStreamConstants.START_ELEMENT) {
        System.out.println("Element: " + reader.getLocalName());
    }
}

// Iterator 方式
XMLEventReader eventReader = factory.createXMLEventReader(input);
while (eventReader.hasNext()) {
    XMLEvent event = eventReader.nextEvent();
    if (event.isStartElement()) {
        StartElement element = event.asStartElement();
        System.out.println("Element: " + element.getName().getLocalPart());
    }
}
```

### Q5：dom4j 和 XPath 配合使用的底层原理？
> dom4j 通过封装 JAXEN 或 JDom 的 XPath 引擎实现路径查询。原理：
> 1. 将 XML 文档解析为 Document 树
> 2. XPath 表达式被编译为一个内部表达式树
> 3. 从根节点开始遍历匹配
> 4. 返回符合条件的节点列表
```java
// dom4j + XPath 查询
SAXReader reader = new SAXReader();
Document doc = reader.read(new File("books.xml"));
List<Node> nodes = doc.selectNodes("//book[@category='IT']/title");
Node node = doc.selectSingleNode("//book[price>50]");
```

### Q6：JAXB 编组和解组的底层流程？
> **Marshalling（对象 → XML）**：
> 1. `JAXBContext.newInstance(clazz)` 扫描注解创建上下文
> 2. `Marshaller` 读取 `@XmlRootElement` 确定根节点名
> 3. 遍历所有被 `@XmlElement` / `@XmlAttribute` 标记的字段
> 4. 通过反射获取值，生成 XML 节点
>
> **Unmarshalling（XML → 对象）**：
> 1. `JAXBContext` 解析 XML Schema 或注解元数据
> 2. `Unmarshaller` 读取 XML 根元素匹配类
> 3. 递归解析子节点，通过反射 setter 或字段赋值
> 4. 返回完整对象

### Q7：Jackson 的 ObjectMapper 是线程安全的吗？为什么？
> **ObjectMapper 是线程安全的**。它在初始化时将所有配置、Serializer/Deserializer 缓存为不变（immutable）状态。多个线程可共享同一个 ObjectMapper 实例，无需同步。
>
> 但 `ObjectReader` 和 `ObjectWriter` 在创建后是线程安全的，所以最佳实践是：
> ```java
> // 推荐：全局单例
> public class JsonUtil {
>     private static final ObjectMapper MAPPER = new ObjectMapper();
> }
> ```

### Q8：Gson 的 TypeToken 用来解决什么问题？
> 解决**泛型类型擦除**问题。反序列化时，Gson 无法通过 `List<User>.class` 获取泛型参数类型，需要用 `TypeToken` 获取完整类型信息。
```java
Type type = new TypeToken<List<User>>(){}.getType();
List<User> users = gson.fromJson(json, type);
// 不加 TypeToken 会返回 List<LinkedHashMap>，不是 List<User>
```

### Q9：Fastjson 的 `AutoType` 机制有什么安全隐患？
> Fastjson 的 `AutoType` 允许在 JSON 中指定 `@type` 字段来反序列化为任意类。如果关闭安全检查，攻击者可以构造恶意 JSON 触发 JNDI 注入（如利用 `com.sun.rowset.JdbcRowSetImpl`），导致 RCE（远程代码执行）。
>
> ❗ **防护措施**：
> - 升级到最新版本（1.2.83+ / 2.0+）
> - 使用 `ParserConfig.getGlobalInstance().setAutoTypeSupport(false)`
> - 配置白名单：`ParserConfig.getGlobalInstance().addAccept("com.example.")`

### Q10：Jackson 如何处理循环引用？
```java
// 方案一：使用 @JsonIgnore 忽略某一边
public class Parent {
    @JsonManagedReference
    Child child;
}
public class Child {
    @JsonBackReference
    Parent parent;
}

// 方案二：使用 @JsonIdentityInfo（推荐）
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Parent { /* ... */ }
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Child { /* ... */ }

// 方案三：全局配置
ObjectMapper mapper = new ObjectMapper();
mapper.enable(SerializationFeature.INDENT_OUTPUT);
mapper.disable(SerializationFeature.FAIL_ON_SELF_REFERENCES);
```

### Q11：Jackson 如何自定义序列化和反序列化？
```java
// 自定义序列化器
public class CustomDateSerializer extends StdSerializer<Date> {
    public CustomDateSerializer() { this(null); }
    public CustomDateSerializer(Class<Date> t) { super(t); }
    @Override
    public void serialize(Date value, JsonGenerator gen, SerializerProvider provider) {
        gen.writeString(new SimpleDateFormat("yyyy/MM/dd").format(value));
    }
}

// 注册到 ObjectMapper
SimpleModule module = new SimpleModule();
module.addSerializer(Date.class, new CustomDateSerializer());
mapper.registerModule(module);
```

### Q12：XML 命名空间在 Java 解析中如何处理？
```java
// dom4j 中处理命名空间
Document doc = reader.read(new File("books.xml"));
XPath xpath = doc.createXPath("//ns:book/ns:title");
xpath.setNamespaceURIs(Collections.singletonMap("ns", "http://example.com/ns"));
List<Node> nodes = xpath.selectNodes(doc);

// SAX 中处理命名空间
SAXParserFactory factory = SAXParserFactory.newInstance();
factory.setNamespaceAware(true); // 关键！开启命名空间感知
```

### Q13：JSON 中如何处理日期格式化？
```java
// Jackson - 全局配置
ObjectMapper mapper = new ObjectMapper();
mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
// 或注解方式
public class User {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}

// Gson
Gson gson = new GsonBuilder()
    .setDateFormat("yyyy-MM-dd HH:mm:ss")
    .create();

// Fastjson
@JSONField(format = "yyyy-MM-dd HH:mm:ss")
private Date createTime;
```

### Q14：什么是 JSONP？和 JSON 有什么区别？
> JSONP（JSON with Padding）是一种**跨域解决方案**，利用 `<script>` 标签不受同源策略限制的特性。通过动态创建 script 标签加载数据，回调函数处理返回的 JSON。
>
> ⚠️ **局限性**：仅支持 GET 请求，需要服务器配合，存在安全风险。现代 Web 开发已被 CORS 替代。

### Q15：JSON-B 和 JSON-P 是什么关系？
> - **JSON-P（JSR 353）**：底层 API，提供 JSON 解析和生成的流式/树模型接口（类似 StAX 之于 XML）
> - **JSON-B（JSR 367）**：上层绑定 API，提供对象与 JSON 的自动映射（类似 JAXB 之于 XML）
>
> JSON-B 底层依赖 JSON-P。Jakarta EE 9+ 推荐的 JSON 处理标准。

---

## 三、实战场景题

### Q1：解析一个超大 XML 文件（500MB+），如何避免 OOM？
> **答案**：使用 SAX 或 StAX 流式解析，绝不能用 DOM。
```java
// SAX 解析大文件的典型代码
SAXParserFactory factory = SAXParserFactory.newInstance();
SAXParser parser = factory.newSAXParser();
// 创建自定义 Handler，逐条处理数据，不保留上下文
parser.parse(new BufferedInputStream(new FileInputStream("huge.xml")), new DefaultHandler() {
    private StringBuilder currentValue = new StringBuilder();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs) {
        currentValue.setLength(0); // 重置
        if (/* 遇到业务元素 */) { /* 初始化业务对象 */ }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if ("record".equals(qName)) {
            // 处理一条记录后立即写入数据库或输出，不保留在内存中
            saveToDB(currentRecord);
            currentRecord = null;
        }
    }
});
```
> 💡 **优化技巧**：配合 `BufferedInputStream` 减少 I/O 次数；使用多线程处理（SAX 本身单线程，但可在 `endElement` 中将数据提交到线程池入库）。

### Q2：如何将 XML 配置文件的全部属性映射为 Java 对象？
> **方案**：JAXB 是最方便的方式。对于 Spring 配置类文件用 Spring 的 `@ConfigurationProperties` + XSD 约束。
```java
// JAXB 解决方案
@XmlRootElement(name = "config")
@XmlAccessorType(XmlAccessType.FIELD)
public class AppConfig {
    @XmlElement private String url;
    @XmlElement private int port;
    @XmlElement(name = "connection-timeout") private int connectionTimeout;
    @XmlElementWrapper(name = "data-sources")
    @XmlElement(name = "data-source") private List<DataSource> dataSources;
}

// 解析一行代码
AppConfig config = (AppConfig) JAXBContext.newInstance(AppConfig.class)
    .createUnmarshaller().unmarshal(new File("config.xml"));
```

### Q3：前端传 JSON 到后端，日期字段总是解析失败怎么办？
> **根因**：前后端日期格式不一致，或 Jackson 反序列化时遇到非法格式。
>
> **解决方案**：
> 1. 统一格式（后端声明）：`@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")`
> 2. 自定义反序列化器处理多种格式：
```java
public class MultiFormatDateDeserializer extends JsonDeserializer<Date> {
    private static final String[] FORMATS = {
        "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "yyyy/MM/dd",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    };

    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        String dateStr = p.getText();
        for (String format : FORMATS) {
            try {
                return new SimpleDateFormat(format).parse(dateStr);
            } catch (ParseException ignored) {}
        }
        throw new IllegalArgumentException("不支持日期格式：" + dateStr);
    }
}
```

### Q4：数据库查出的数据需要导出为 XML，怎么做？
> 使用 JAXB 将实体类通过注解标注后，直接编组输出。
```java
// 批量导出
List<Employee> employees = employeeService.findAll();
JAXBContext context = JAXBContext.newInstance(EmployeeList.class);
Marshaller marshaller = context.createMarshaller();
marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
// 输出到文件
marshaller.marshal(new EmployeeList(employees), new File("employees.xml"));
// 或输出到字符串
StringWriter sw = new StringWriter();
marshaller.marshal(new EmployeeList(employees), sw);
```

### Q5：Jackson 反序列化时字段名不匹配（用下划线 vs 驼峰）如何处理？
```java
// 方案一：注解指定
public class User {
    @JsonProperty("user_name")
    private String userName;
}

// 方案二：全局配置（推荐，Spring Boot 常用）
ObjectMapper mapper = new ObjectMapper();
mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
// 效果：JSON 中 "user_name" 映射到 Java 字段 "userName"
```

### Q6：如何在 XML 和 JSON 之间互相转换？
```java
// XML <-> JSON 转换（使用 Jackson 的 jackson-dataformat-xml 扩展）
// Maven: com.fasterxml.jackson.dataformat:jackson-dataformat-xml

// XML -> 对象
XmlMapper xmlMapper = new XmlMapper();
User user = xmlMapper.readValue(xmlString, User.class);

// 对象 -> JSON
ObjectMapper jsonMapper = new ObjectMapper();
String json = jsonMapper.writeValueAsString(user);
```

### Q7：需要解析多层嵌套的 JSON，如何处理？
```java
// 方式一：直接定义嵌套类
String json = "{\"user\":{\"name\":\"张三\",\"address\":{\"city\":\"北京\"}}}";

// POJO 嵌套定义
public class Response {
    private User user;
    // getter / setter
}
public class User {
    private String name;
    private Address address;
}
public class Address {
    private String city;
}

// 方式二：使用 JsonNode（不确定结构时）
JsonNode root = mapper.readTree(json);
String city = root.path("user").path("address").path("city").asText();
```

### Q8：Spring Boot 中如何统一配置 JSON 序列化？
```yaml
# application.yml
spring:
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
    default-property-inclusion: non_null  # 忽略 null 字段
    property-naming-strategy: SNAKE_CASE  # 驼峰转下划线
```
```java
// 或 Java Config 方式
@Bean
@Primary
public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return mapper;
}
```

### Q9：WebService 接口中返回 XML，要求监听返回的 XML 结构？
> 使用 JAXB + 自定义 XMLStreamWriter 拦截输出，或使用 AOP 在 Marshaller 前后打印日志。
```java
// 简单的拦截方式：使用 LoggingXMLStreamWriter 包装
XMLOutputFactory factory = XMLOutputFactory.newInstance();
XMLStreamWriter writer = new LoggingXMLStreamWriter(
    factory.createXMLStreamWriter(System.out));
marshaller.marshal(obj, writer);
```

### Q10：配置方式使用 JSON 还是 XML 更好？
| 考量因素 | JSON 配置 | XML 配置 |
|----------|-----------|-----------|
| 可读性 | 简洁，开发友好 | 标签多，但结构化强 |
| 注释支持 | 不支持原生注释 | 支持 `<!-- -->` |
| Schema 校验 | JSON Schema（较新） | XSD（成熟完备） |
| IDE 支持 | 友好 | 非常成熟（自动补全、校验） |
| 适合场景 | 简单应用配置 | 复杂业务规则配置、SOA |

---

## 四、手写代码题

### Q1：使用 dom4j 解析 XML 并配合 XPath 查询
```java
import org.dom4j.*;
import org.dom4j.io.SAXReader;
import java.io.File;
import java.util.List;

public class Dom4jDemo {
    public static void main(String[] args) throws Exception {
        SAXReader reader = new SAXReader();
        Document doc = reader.read(new File("books.xml"));

        // 1. 获取根元素
        Element root = doc.getRootElement();
        System.out.println("Root: " + root.getName());

        // 2. 遍历子元素
        for (Iterator<Element> it = root.elementIterator("book"); it.hasNext();) {
            Element book = it.next();
            String id = book.attributeValue("id");
            String title = book.elementText("title");
            String author = book.elementText("author");
            System.out.printf("ID=%s, Title=%s, Author=%s%n", id, title, author);
        }

        // 3. XPath 查询全部 title 元素
        List<Node> titles = doc.selectNodes("//book/title");
        for (Node node : titles) {
            System.out.println("XPath: " + node.getText());
        }

        // 4. XPath 条件查询
        Node expensive = doc.selectSingleNode("//book[price > 50]");
        if (expensive != null) {
            System.out.println("First expensive book: " +
                expensive.selectSingleNode("title").getText());
        }
    }
}
```

### Q2：使用 JDK 内置 SAX 解析 XML
```java
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

public class SaxDemo {
    public static void main(String[] args) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();

        parser.parse(new InputSource("books.xml"), new DefaultHandler() {
            private StringBuilder text = new StringBuilder();
            private String currentElement;

            @Override
            public void startElement(String uri, String localName,
                    String qName, Attributes attrs) {
                currentElement = qName;
                text.setLength(0);
                // 处理属性
                for (int i = 0; i < attrs.getLength(); i++) {
                    System.out.printf("Attr: %s=%s%n",
                        attrs.getQName(i), attrs.getValue(i));
                }
            }

            @Override
            public void characters(char[] ch, int start, int length) {
                text.append(ch, start, length); // 拼接（可能多次回调）
            }

            @Override
            public void endElement(String uri, String localName, String qName) {
                String value = text.toString().trim();
                if (!value.isEmpty()) {
                    System.out.printf("%s: %s%n", currentElement, value);
                }
            }
        });
    }
}
```

### Q3：使用 JDK 内置 StAX 解析 XML
```java
import javax.xml.stream.*;

public class StaxDemo {
    public static void main(String[] args) throws Exception {
        // Cursor 方式
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);

        // Cursor 读取
        XMLStreamReader reader = factory.createXMLStreamReader(
            StaxDemo.class.getResourceAsStream("/books.xml"));
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    System.out.println("START: " + reader.getLocalName());
                    for (int i = 0; i < reader.getAttributeCount(); i++) {
                        System.out.printf("  [%s=%s]%n",
                            reader.getAttributeLocalName(i),
                            reader.getAttributeValue(i));
                    }
                    break;
                case XMLStreamConstants.CHARACTERS:
                    String val = reader.getText().trim();
                    if (!val.isEmpty()) System.out.println("  TEXT: " + val);
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    System.out.println("END: " + reader.getLocalName());
                    break;
            }
        }
        reader.close();

        // Iterator 方式写入（创建新 XML）
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer = outputFactory.createXMLStreamWriter(System.out);
        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeStartElement("catalog");
        writer.writeStartElement("book");
        writer.writeAttribute("id", "1001");
        writer.writeStartElement("title");
        writer.writeCharacters("Java 核心技术");
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();
        writer.close();
    }
}
```

### Q4：使用 JAXB 完成对象与 XML 的互转
```java
import javax.xml.bind.annotation.*;
import javax.xml.bind.*;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

// 1. 定义模型类
@XmlRootElement(name = "book")
@XmlAccessorType(XmlAccessType.FIELD)
class Book {
    @XmlAttribute(required = true)
    private String id;

    @XmlElement(required = true)
    private String title;

    @XmlElement
    private String author;

    @XmlElement(name = "publish-year")
    private int publishYear;

    // 必须有无参构造
    public Book() {}

    public Book(String id, String title, String author, int publishYear) {
        this.id = id; this.title = title;
        this.author = author; this.publishYear = publishYear;
    }

    @Override
    public String toString() {
        return String.format("Book{id='%s', title='%s', author='%s', year=%d}",
            id, title, author, publishYear);
    }
}

@XmlRootElement(name = "library")
@XmlAccessorType(XmlAccessType.FIELD)
class Library {
    @XmlElement(name = "book")
    private List<Book> books = new ArrayList<>();

    public void addBook(Book book) { books.add(book); }
}

// 2. 编组（对象 -> XML）
public class JaxbDemo {
    public static void main(String[] args) throws Exception {
        JAXBContext context = JAXBContext.newInstance(Library.class, Book.class);

        // Marshall: Object -> XML
        Library lib = new Library();
        lib.addBook(new Book("B001", "Java编程思想", "Bruce Eckel", 2006));
        lib.addBook(new Book("B002", "深入理解Java虚拟机", "周志明", 2020));

        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        StringWriter sw = new StringWriter();
        marshaller.marshal(lib, sw);
        System.out.println("=== XML 输出 ===");
        System.out.println(sw.toString());

        // Unmarshall: XML -> Object
        Unmarshaller unmarshaller = context.createUnmarshaller();
        Library parsed = (Library) unmarshaller.unmarshal(
            new StringReader(sw.toString()));
        System.out.println("=== 还原对象 ===");
        parsed.books.forEach(System.out::println);
    }
}
```

### Q5：使用 Jackson 完成对象与 JSON 的序列化/反序列化
```java
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.*;

// 模型类
@JsonIgnoreProperties(ignoreUnknown = true)
class Employee {
    @JsonProperty("emp_id")
    private String empId;

    @JsonProperty("emp_name")
    private String empName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate hireDate;

    @JsonIgnore
    private String internalRemark;

    // null 值不参与序列化（字段级别）
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String department;

    // getter / setter（省略）
    public String getEmpId() { return empId; }
    public void setEmpId(String empId) { this.empId = empId; }
    public String getEmpName() { return empName; }
    public void setEmpName(String empName) { this.empName = empName; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public String getInternalRemark() { return internalRemark; }
    public void setInternalRemark(String internalRemark) { this.internalRemark = internalRemark; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    @Override
    public String toString() {
        return String.format("Employee{id='%s', name='%s', hire=%s, dept=%s}",
            empId, empName, hireDate, department);
    }
}

public class JacksonDemo {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        // 全局配置
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        Employee emp = new Employee();
        emp.setEmpId("E001");
        emp.setEmpName("张三");
        emp.setHireDate(LocalDate.of(2024, 6, 1));
        emp.setDepartment(null); // 被忽略（NON_NULL）

        // 序列化
        String json = mapper.writeValueAsString(emp);
        System.out.println("Serialized: " + json);

        // 反序列化
        String inputJson = "{\"emp_id\":\"E002\",\"emp_name\":\"李四\",\"hire_date\":\"2024-07-15\"}";
        Employee emp2 = mapper.readValue(inputJson, Employee.class);
        System.out.println("Deserialized: " + emp2);

        // 集合反序列化
        String listJson = "[{\"emp_id\":\"E003\"},{\"emp_id\":\"E004\"}]";
        List<Employee> list = mapper.readValue(listJson,
            mapper.getTypeFactory().constructCollectionType(List.class, Employee.class));
        System.out.println("List size: " + list.size());
    }
}
```

### Q6：使用 Gson 完成对象与 JSON 的互转
```java
import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.*;

class Product {
    @SerializedName("product_id")
    private String productId;

    @SerializedName("product_name")
    private String productName;

    @SerializedName("price")
    private double price;

    // transient 字段被 Gson 忽略
    private transient String internalCode;

    public Product(String productId, String productName, double price) {
        this.productId = productId; this.productName = productName; this.price = price;
    }

    @Override
    public String toString() {
        return String.format("Product{id='%s', name='%s', price=%.2f}",
            productId, productName, price);
    }
}

public class GsonDemo {
    public static void main(String[] args) {
        // 1. 基本 toJson / fromJson
        Gson gson = new Gson();
        Product product = new Product("P001", "笔记本电脑", 5999.00);
        String json = gson.toJson(product);
        System.out.println("JSON: " + json);

        Product parsed = gson.fromJson(json, Product.class);
        System.out.println("Parsed: " + parsed);

        // 2. GsonBuilder 自定义配置
        Gson prettyGson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()           // 序列化 null 值
            .setDateFormat("yyyy-MM-dd")
            .disableHtmlEscaping()      // 不转义 < > 等
            .create();
        System.out.println(prettyGson.toJson(product));

        // 3. 集合泛型
        String listJson = "[{\"product_id\":\"P002\"},{\"product_id\":\"P003\"}]";
        Type type = new TypeToken<List<Product>>(){}.getType();
        List<Product> products = gson.fromJson(listJson, type);
        System.out.println("Products count: " + products.size());

        // 4. 自定义 JsonSerializer / JsonDeserializer
        Gson customGson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>)
                (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>)
                (json1, typeOfT, context) -> LocalDate.parse(json1.getAsString()))
            .create();
    }
}
```

### Q7：Jackson 处理多态 JSON 反序列化
```java
import com.fasterxml.jackson.annotation.*;

// 父类，使用 @JsonTypeInfo 标识子类类型
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type"  // JSON 中标识类型的字段名
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Circle.class, name = "circle"),
    @JsonSubTypes.Type(value = Rectangle.class, name = "rectangle")
})
abstract class Shape {
    public abstract double area();
}

class Circle extends Shape {
    @JsonProperty private double radius;
    @Override public double area() { return Math.PI * radius * radius; }
}

class Rectangle extends Shape {
    @JsonProperty private double width;
    @JsonProperty private double height;
    @Override public double area() { return width * height; }
}

// 使用
ObjectMapper mapper = new ObjectMapper();
String json = "{\"type\":\"circle\",\"radius\":5.0}";
Shape shape = mapper.readValue(json, Shape.class);
System.out.println("Area: " + shape.area()); // 78.54
```

---

## 五、系统设计题

### Q1：设计一个通用的数据交换网关，需要同时支持 XML 和 JSON 格式，如何设计？
> **核心思路**：适配器模式 + 策略模式
>
> 1. **统一抽象层**：定义 `DataSerializer<T>` 和 `DataDeserializer<T>` 接口
> 2. **格式适配器**：分别实现 XML（JAXB）和 JSON（Jackson）适配器
> 3. **内容协商**：根据请求的 `Content-Type` 或 `Accept` 头动态选择适配器
> 4. **抽象 Schema 校验层**：XSD for XML，JSON Schema for JSON
>
> ```java
> public interface DataSerializer<T> {
>     String serialize(T obj, Class<T> clazz);
> }
> public interface DataDeserializer<T> {
>     T deserialize(String data, Class<T> clazz);
> }
> // XmlSerializer implements DataSerializer（使用 JAXB）
> // JsonSerializer implements DataSerializer（使用 Jackson）
> ```

### Q2：设计一个高性能、低延迟的日志采集系统，日志格式选 XML 还是 JSON？为什么？
> **选择 JSON**，理由：
> - **体积小**：同样数据 JSON 比 XML 小 30%~50%，节省带宽和存储
> - **解析快**：JSON 解析速度远快于 XML
> - **结构简单**：日志多为平铺字段，不需要 XML 的复杂嵌套
> - **Logstash/Elasticsearch 生态**：对 JSON 原生友好
>
> 如果必须使用 XML，建议使用 **StAX 流式写入** 而非 DOM，避免 OOM。

### Q3：公司内部现有系统 A 使用 XML（SOAP）接口，新系统 B 使用 JSON（RESTful），如何平滑过渡？
> **方案：API 网关层做协议转换**
>
> 1. 开发 **协议转换层**（XML ↔ JSON 翻译器）
> 2. 使用 **Jackson XmlMapper** 或自定义转换器
> 3. 实施 **灰度策略**：新老接口并行，逐步迁移
> 4. 定义统一的 **内部数据模型 DTO**，消除两边的耦合
>
> ```java
> // 协议转换核心：统一内部 DTO
> public class UnifiedOrderDTO {
>     private String orderId;
>     private BigDecimal amount;
>     private String status;
> }
> // 从 XML 解析 -> 内部 DTO -> 序列化为 JSON
> // 从 JSON 解析 -> 内部 DTO -> 序列化为 XML
> ```

### Q4：如何选择 XML 解析方式来做数据迁移（100GB 数据）？
> **结论：必须使用 SAX 或 StAX，不能使用 DOM。**
>
> | 方案 | 可行性 | 理由 |
> |------|--------|------|
> | DOM | 不可行 | 100GB XML 需要 500GB+ 内存，OOM |
> | SAX | 可行 | 流式读取，内存占用约几 KB |
> | StAX | 可行 | 拉模型，控制更灵活 |
> | SAX + 批量写 | **推荐** | 每解析 1000 条批量写入数据库 |
>
> **优化建议**：
> - 使用 `BufferedInputStream`（8KB 缓冲区）
> - 批量提交：每 N 条记录`flush()`一次数据库
> - 多线程写库：SAX 回调中通过阻塞队列交给写线程

### Q5：如何保证大规模 JSON/XML 数据交换的安全性？
> | 威胁 | 防护措施 |
> |------|----------|
> | XXE（XML External Entity） | 关闭 DTD 外部实体解析：`factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)` |
> |  Billion Laughs（XML 炸弹） | 限制实体扩展深度：`factory.setProperty(XMLConstants.ACCESS_ENTITY_EXPANSION_LIMIT, "1000000")` |
> | Fastjson AutoType RCE | 关闭 AutoType 或升级到最新版 |
> | JSON 过大导致 DoS | 限制请求体大小（`@MaxRequestBodySize`）且设置反序列化最大嵌套深度 |
> | XXS 注入 | JSON 输出时进行 HTML 转义（Jackson 默认禁止 HTML 转义，需手动开启） |

---

## 六、常见坑点与最佳实践

| 问题 | 现象 | 原因 | 解决方案 |
|------|------|------|----------|
| SAX 中字符内容不完整 | 中文或长文本被截断 | SAX 的 `characters()` 可能回调多次 | 用 `StringBuilder` 拼接，在 `endElement` 中获取完整值 |
| Jackson 反序列化未知字段抛异常 | `UnrecognizedPropertyException` | 默认遇到未知字段会失败 | `mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)` 或加 `@JsonIgnoreProperties(ignoreUnknown = true)` |
| Gson 泛型反序列化类型错误 | 拿到 `List<LinkedHashMap>` 而非 `List<User>` | 泛型擦除 | 使用 `TypeToken` 获取完整泛型类型 |
| 循环引用无限递归 | `StackOverflowError` | 双向关联互相引用 | `@JsonManagedReference / @JsonBackReference` 或 `@JsonIdentityInfo` |
| XML 实体注入（XXE） | 服务器数据泄露 | 解析器默认加载外部 DTD | 关闭 DTD：`factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)` |
| JAXB 无参构造异常 | `InstantiationException` | JAXB 需要无参构造来创建实例 | 添加无参构造器 |
| Jackson 日期序列化为时间戳 | 返回 long 而非格式化日期 | 默认行为 | `mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)`，并注册 `JavaTimeModule` |
| fastjson 1.x 版本 RCE 漏洞 | 服务器被远程控制 | AutoType 绕过 | 升级 2.x 或配置 `ParserConfig.getGlobalInstance().setSafeMode(true)` |
| Jackson 序列化 null 字段 | 输出字段值为 `null` | 默认包含 null 字段 | `mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)` |
| Gson 序列化 null 丢失字段 | null 字段不出现在 JSON 中 | Gson 默认忽略 null | `new GsonBuilder().serializeNulls().create()` |
| XML 命名空间未开启 | 无法获取正确标签名 | 解析器默认非 Namespace-Aware | `SAXParserFactory.newInstance().setNamespaceAware(true)` |
| 大 XML 使用 DOM 导致 OOM | `OutOfMemoryError` | 全量载入内存 | 改用 SAX / StAX 流式解析 |

---

## 七、面试回答模板

### 模板 1：谈谈 XML 和 JSON 的区别
> **回答要点**：
> "XML 和 JSON 都是数据交换格式，但设计哲学不同。XML 侧重**文档标记和元数据**，拥有属性、命名空间、注释等丰富特性，适合复杂文档和 Schema 约束强的场景（如 SOAP、配置）。JSON 侧重**轻量数据交换**，语法简洁、浏览器原生支持，适合 Web API、移动端等网络传输场景。在实际项目选型时，如果追求传输效率选择 JSON，如果数据结构复杂且需要强约束选择 XML。"

### 模板 2：大文件 XML 解析怎么做？
> **回答要点**：
> "大 XML 文件必须使用**流式解析**——SAX 或 StAX。关键点：1）绝对不用 DOM，会导致 OOM；2）SAX 用 `DefaultHandler` + 拼接 `characters()` 回调内容；3）StAX 用 `XMLEventReader` 主动拉取事件，控制更灵活；4）配合 `BufferedInputStream` 减少 I/O 开销；5）每解析 N 条记录批量写入数据库，避免频繁网络交互。在项目中我处理过 2GB 的 XML 报表解析，使用 SAX + 批量插入，内存控制在 10MB 以内。"

### 模板 3：Jackson 和 Gson 如何选择？
> **回答要点**：
> "两者都是生产级 JSON 库。**Jackson** 是 Spring Boot 默认选择，性能更优、功能更全面（支持 XML、YAML、CBOR 等多种格式）、注解体系完善，适合大型项目和复杂场景。**Gson** 的 API 更简洁优雅，`toJson/fromJson` 上手极快，适合中小项目和快速开发。如果项目已经使用了 Spring Boot，默认 Jackson 无需额外依赖；如果是简单的工具类项目，Gson 更省心。性能方面 Jackson 略占优势，但在 Web 响应场景下差距可以忽略。"

### 模板 4：项目中使用 JAXB 还是手动解析 XML？
> **回答要点**：
> "如果是对象与 XML 的互相转换，**优先使用 JAXB**，它通过注解无代码侵入地实现映射，一行代码完成编组/解组。如果 XML 结构不确定或需要灵活的路径查询/修改，使用 **dom4j + XPath**。如果只需要从大 XML 中提取少量字段，用 **SAX/StAX 流式解析**。实际项目中，配置类 XML 用 JAXB，数据交换 XML 用 dom4j，大文件解析用 SAX。"

### 模板 5：JSON 解析踩过最大的坑是什么？怎么解决的？
> **回答要点**：
> "我遇到最典型的是 **循环引用导致的 StackOverflowError**。订单和用户双向关联，序列化时无限递归。解决方案：1）使用 `@JsonManagedReference` 和 `@JsonBackReference` 标注父子关系；2）更推荐 `@JsonIdentityInfo` 通过 ID 引用避免重复序列化；3）也可以用 `@JsonIgnore` 手动忽略某一边。另一个常见坑是 Jackson 遇到未知字段抛异常，配置 `FAIL_ON_UNKNOWN_PROPERTIES = false` 即可。此外，建议 ObjectMapper 全局单例复用，避免频繁创建的开销。"

---

## 八、快速查漏补缺 Checklist

### XML 基础
- [ ] 能说出 XML 声明格式（version, encoding, standalone）
- [ ] 知道 DTD 和 XSD 的区别及各自适用的场景
- [ ] 理解命名空间的作用和声明方式
- [ ] 知道 CDATA 的作用

### XML 解析
- [ ] 能对比 DOM / SAX / StAX 三种方式的内存、速度、使用场景
- [ ] 能用 dom4j + XPath 解析 XML（重点：`selectNodes`, `selectSingleNode`）
- [ ] 能用 SAX ContentHandler 解析 XML（重点：`startElement`, `endElement`, `characters` 拼接）
- [ ] 能用 StAX XMLEventReader / XMLStreamReader 解析 XML
- [ ] 理解 SPI 机制在 Java XML 解析中的作用

### JAXB
- [ ] 能写出 `@XmlRootElement`, `@XmlElement`, `@XmlAttribute` 的完整用法
- [ ] 能写出 `JAXBContext → Marshaller → marshal()` 的完整流程
- [ ] 知道 JAXB 需要无参构造器

### JSON 基础
- [ ] 能手写标准 JSON 格式（对象、数组、嵌套）
- [ ] 能对比 JSON 和 XML 的优缺点（至少 3 点）

### Jackson
- [ ] 能写出 `ObjectMapper.writeValueAsString()` 和 `readValue()`
- [ ] 会用 `@JsonProperty`, `@JsonIgnore`, `@JsonFormat`, `@JsonInclude`
- [ ] 会处理泛型集合：`mapper.getTypeFactory().constructCollectionType()`
- [ ] 会配置 `FAIL_ON_UNKNOWN_PROPERTIES = false`
- [ ] 知道如何处理循环引用（`@JsonIdentityInfo`）
- [ ] 知道 Jackson ObjectMapper 是线程安全的

### Gson
- [ ] 能写出 `gson.toJson()` 和 `gson.fromJson()`
- [ ] 会用 `@SerializedName` 映射字段名
- [ ] 会用 `TypeToken` 处理泛型
- [ ] 会用 `GsonBuilder` 定制（`setPrettyPrinting`, `serializeNulls`, `setDateFormat`）

### Fastjson
- [ ] 能写出 `JSON.toJSONString()` 和 `JSON.parseObject()`
- [ ] 知道 AutoType 的安全风险及防护

### 安全
- [ ] 知道 XXE 是什么及如何防护
- [ ] 知道 XML 炸弹（Billion Laughs）攻击原理
- [ ] 知道 Fastjson 漏洞历史背景

---

> 🎯 **面试核心**：能熟练讲解 XML/JSON 的对比选型，能手写使用 Jackson/Gson 解析 JSON 和使用 dom4j/JAXB 解析 XML 的完整代码。重点掌握流式解析的原理和使用场景区分。安全方面必须了解 XXE 和 AutoType RCE 的防护。
