# Java 语法糖 核心知识点
语法糖：**编译期语法优化**，源码简化写法，编译后会被编译器还原为基础语法，运行期无额外开销，JVM 无感知。

## 一、泛型擦除
### 核心原理
1. 泛型只存在于**编译阶段**，编译后统一擦除为**原始类型（Object/上限父类）**
2. 无泛型数组、泛型不能参与 instanceof 判断
3. 为兼容老版本 JDK 的向下兼容设计
### 举例
```java
List<String> list = new ArrayList<>();
// 编译后还原为：List list = new ArrayList();
```
### 关键问题
- `List<String>` 与 `List<Integer>` 运行期是同一个 Class 对象
- 强制类型转换由编译器自动补全

## 二、自动装箱 & 自动拆箱
### 定义
- 装箱：基本类型 → 包装类（`int → Integer`）
- 拆箱：包装类 → 基本类型（`Integer → int`）
### 编译还原
- 装箱：底层调用 `Integer.valueOf()`
- 拆箱：底层调用 `intValue()`
### 高频坑点
1. 缓存机制：`Byte/Short/Integer/Long` 缓存范围 **-128 ~ 127**
2. 包装类对象 == 比较地址，equals 比较值
3. 包装类与基本类型运算，**自动拆箱**
4. 包装类默认值为 `null`，直接拆箱会空指针异常

## 三、增强 for 循环（foreach）
### 适用范围
数组、实现 `Iterable` 接口的集合
### 编译还原
1. 集合：底层转化为 **迭代器 Iterator** 遍历
2. 数组：底层转化为普通 for 循环 + 下标遍历
### 限制
- 遍历集合时，**不能增删元素**，否则触发并发修改异常 `ConcurrentModificationException`
- 无法获取下标、无法反向遍历

## 四、变长参数
### 语法
`public void test(String... args)`
### 编译还原
编译后直接转化为**数组**：`String[] args`
### 特性
1. 变长参数必须放在方法参数**最后一位**
2. 一个方法只能有一个变长参数
3. 可传 0 个、单个、多个参数或数组

## 五、try-with-resources 自动资源关闭
### 作用
自动关闭流、连接等实现 `AutoCloseable/Closeable` 的资源，简化 finally 关闭代码
### 编译还原
编译器自动补全 `finally` 代码块，调用 `close()` 方法
### 适用类
IO 流、数据库连接、Redis 连接、Socket 等

## 六、Lambda 表达式 & 函数式接口
### 本质
特殊语法糖，简化**匿名内部类**写法
### 编译原理
1. 不会生成匿名内部类 Class 文件
2. 底层通过 `invokedynamic` 指令 + 函数式接口实现
### 前提
必须配合**函数式接口**（仅一个抽象方法）使用

## 七、方法引用
### 分类
- 对象::实例方法
- 类::静态方法
- 类::实例方法
- 构造器引用 类::new
### 本质
Lambda 表达式的进一步简化，同样依托 invokedynamic 实现

## 八、枚举类 Enum
### 语法糖本质
枚举编译后：
1. 自动继承 `java.lang.Enum`
2. 枚举常量被编译为 `public static final` 常量
3. 自动生成 `values()`、`valueOf()` 方法
### 特性
单例、线程安全、防止反射破坏、适合固定常量场景

## 九、注解（普通注解 / 元注解）
### 语法糖特性
编译期注解：仅编译期生效，编译后丢弃
如：`@Override`、`@SuppressWarnings`，仅做语法校验，运行期无残留

## 十、字符串 + 拼接
### 底层优化
1. JDK8 及之前：`a + b` 编译为 `new StringBuilder().append().toString()`
2. JDK9+：改用 `invokedynamic` + `StringConcatFactory` 优化拼接性能
### 注意
循环内频繁字符串拼接，**不要直接用 +**，建议手动使用 StringBuilder

## 十一、switch 支持字符串 & 枚举
### 底层原理
1. switch 字符串：底层通过 `hashCode()` + `equals()` 实现
2. switch 枚举：底层转为枚举**常量序号 ordinal** 比较
### 隐患
字符串 switch 存在哈希冲突风险

## 十二、默认方法 / 静态方法（接口）
### JDK8 新增语法糖
- 接口 `default` 方法：解决接口扩展的兼容性问题
- 接口 `static` 方法：直接通过接口名调用
### 本质
编译后在接口的 class 文件中生成具体实现方法

---

# 高频面试核心总结
1. 所有语法糖都在**编译期解糖**，运行期无特殊逻辑；
2. 泛型擦除、装箱拆箱、foreach 是面试最高频；
3. Lambda 底层是 invokedynamic，区别于普通匿名内部类；
4. try-with-resources 是开发必备，杜绝资源泄漏；
5. 字符串拼接、switch 字符串都是编译器语法优化。
