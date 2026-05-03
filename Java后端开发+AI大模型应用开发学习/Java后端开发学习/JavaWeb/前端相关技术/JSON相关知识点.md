03.26 19:04
JSON相关知识点
一、JSON核心定义
JSON（JavaScript Object Notation，JavaScript对象表示法）是一种轻量级、跨语言、纯文本的数据交换格式，并非编程语言，核心作用是在不同系统（如前端与后端、不同语言开发的服务）之间传递结构化数据，易于人类阅读和编写，也易于机器解析和生成。
核心特点：简洁性、可读性强、跨平台兼容（几乎所有主流编程语言都支持JSON的解析与生成）、占用带宽小（相比XML更简洁），广泛应用于接口数据传输、配置文件编写等场景。
二、JSON基本语法规则（必记）
JSON语法严格，错误的语法会导致解析失败，核心规则如下：
整体结构：有两种核心结构，分别是「对象」和「数组」，可嵌套使用。
键值对规则：对象中的数据以“键:值”（key:value）形式存在，键（key）必须用双引号包裹（单引号无效），值（value）需对应合法数据类型。
分隔规则：多个键值对或数组元素之间，用逗号（,）分隔，注意最后一个元素后不能加逗号（ trailing comma 错误）。
符号规则：使用大括号「{}」包裹对象，中括号「[]」包裹数组，所有符号均为英文半角（中文全角符号会导致解析失败）。
注释限制：JSON 不支持注释（与JavaScript对象的区别之一），若需添加注释，需在解析前删除，否则会报错。
三、JSON数据类型（6种，必掌握）
JSON的值（value）仅支持以下6种数据类型，无其他扩展类型，具体说明如下：
字符串（string）：最常用类型，用双引号包裹，可包含字母、数字、符号、中文等，支持转义字符（如\n换行、\t制表符、\"双引号等）。示例："name": "张三"、"address": "北京市\n朝阳区"
数字（number）：整数、浮点数均可，不支持八进制、十六进制，也不支持NaN、Infinity。示例："age": 20、"score": 98.5、"id": 1001
布尔值（boolean）：仅两个值，true（真）和false（假），小写（大写True/False会报错）。示例："isStudent": true、"isActive": false
空值（null）：表示“无值”，注意是null（小写），不是undefined（JSON不支持undefined类型）。示例："avatar": null
对象（object）：用大括号「{}」包裹，内部是若干键值对的集合，可嵌套其他对象或数组。示例："user": {"name": "李四", "age": 22, "address": {"city": "上海", "area": "浦东"}}
数组（array）：用中括号「[]」包裹，内部是若干值的集合，值的类型可不同（可混合字符串、数字、对象等），也可嵌套数组。示例："hobbies": ["读书", "运动", 123]、"students": [{"name": "张三"}, {"name": "李四"}]
四、JSON与JavaScript对象的区别（易混淆点）
很多人会将JSON与JavaScript对象混淆，两者核心区别如下，避免使用时出错：
对比维度
JSON
JavaScript对象
本质
纯文本（字符串），用于数据交换
编程语言中的对象，用于代码逻辑操作
键（key）
必须用双引号包裹
可省略引号，也可用单引号、双引号
值（value）
仅支持6种JSON数据类型，无undefined
支持所有JavaScript数据类型（含undefined、函数等）
注释
不支持
支持//单行注释、/*多行注释*/
结尾逗号
不允许（报错）
允许（ES6及以上支持）
五、JSON的解析与序列化（核心操作）
JSON的核心使用场景是“数据交换”，因此常需要在「JSON字符串」和「编程语言对象」之间转换，以下以JavaScript为例（最常用），其他语言（Java、Python等）逻辑类似。
1. 序列化（对象 → JSON字符串）
将JavaScript对象（或数组）转换为JSON字符串，用于传输（如接口请求），使用JSON.stringify()方法。
// 示例：对象序列化
const user = { name: "张三", age: 20, isStudent: true };
const jsonStr = JSON.stringify(user);
console.log(jsonStr); // 输出：{"name":"张三","age":20,"isStudent":true}（纯字符串）
// 补充：可添加参数，格式化输出（便于阅读）
const jsonStrFormat = JSON.stringify(user, null, 2); // 第三个参数表示缩进空格数
2. 解析（JSON字符串 → 对象）
将接收的JSON字符串转换为JavaScript对象，用于代码操作（如渲染页面），使用JSON.parse()方法。
// 示例：JSON字符串解析
const jsonStr = '{"name":"张三","age":20,"isStudent":true}';
const user = JSON.parse(jsonStr);
console.log(user.name); // 输出：张三（可直接操作对象属性）
注意：解析时，JSON字符串必须符合语法规则，否则会抛出语法错误（如键未用双引号、存在注释等）。
六、常见错误与注意事项
键未用双引号：错误示例：{name: "张三"}，正确示例：{"name": "张三"}。
使用单引号包裹键/值：错误示例：{'name': '张三'}，正确示例：{"name": "张三"}。
结尾加逗号：错误示例：{"name": "张三", "age": 20,}，正确示例：{"name": "张三", "age": 20}。
使用不支持的数据类型：如{"func": function(){}}（JSON不支持函数）、{"num": NaN}（不支持NaN），解析会报错。
中文乱码：传输JSON时，需确保编码为UTF-8（默认推荐），避免中文解析乱码。
七、JSON的常见应用场景
接口数据传输：前后端交互的核心格式（如前端请求后端接口，后端返回JSON数据，前端解析后渲染）。
配置文件：编写简洁的配置文件（如项目中的package.json、vue.config.json等），易于读取和修改。
数据存储：少量数据的本地存储（如JavaScript中的localStorage，仅支持字符串，需将对象序列化为JSON字符串存储）。
跨语言数据交换：不同编程语言（如Java后端、Python脚本、前端JavaScript）之间传递数据，JSON是通用格式。

