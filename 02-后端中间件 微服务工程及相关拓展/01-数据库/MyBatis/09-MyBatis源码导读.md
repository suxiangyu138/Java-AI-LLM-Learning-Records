# 09 - MyBatis 源码导读

> 🎯 读 MyBatis 源码是 Java 工程师的技术分水岭。本章不逐行分析，而是带你理解核心组件的调用链路和设计模式，剩下的源码自己就能读了

---

## 目录

1. [初始化过程](#1-初始化过程)
2. [执行流程](#2-执行流程)
3. [设计模式汇总](#3-设计模式汇总)

---

## 1. 初始化过程

```text
SqlSessionFactoryBuilder.build(inputStream)
  ↓
XMLConfigBuilder.parse()
  ├── 解析 <properties> → 全局变量
  ├── 解析 <settings> → Configuration 对象
  ├── 解析 <typeAliases> → 类型别名注册
  ├── 解析 <plugins> → InterceptorChain
  ├── 解析 <environments> → DataSource + TransactionFactory
  └── 解析 <mappers> → XMLMapperBuilder.parse()
        ├── 解析 <select>/<insert>/<update>/<delete>
        └── 每个 SQL → 生成 MappedStatement → 存入 Configuration.mappedStatements

最终产出: Configuration 对象（全局唯一）
  → 传入 SqlSessionFactory 构造函数
```

```java
// 核心数据结构
public class Configuration {
    protected final Map<String, MappedStatement> mappedStatements;
    // key = "com.example.UserMapper.findById"
    // value = MappedStatement { sql, parameterMap, resultMap, ... }
}
```

## 2. 执行流程

```text
mapper.findById(1L)
  ↓
MapperProxy.invoke()                    ← JDK 动态代理拦截
  ↓ 从 MappedStatement 获取 SQL
  ↓
MapperMethod.execute()                  ← 判断 CRUD 类型
  ↓
SqlSession.selectOne()
  ↓
Executor.query()                        ← CachingExecutor 装饰
  ↓ (二级缓存未命中)
  ↓
BaseExecutor.query()                    ← 一级缓存检查
  ↓ (一级缓存未命中)
  ↓
SimpleExecutor.doQuery()               ← 真正执行
  ↓
StatementHandler.prepare()             ← 获取 Connection + 预编译
  ↓
StatementHandler.parameterize()        ← 设置参数 (ParameterHandler)
  ↓
StatementHandler.query()               ← 执行 SQL → ResultSet
  ↓
ResultSetHandler.handleResultSets()    ← 结果映射 (TypeHandler)
  ↓
返回 List<User>
```

### 关键设计

```text
Executor 的装饰器链：
  CachingExecutor → BaseExecutor → SimpleExecutor
   (二级缓存)       (一级缓存)       (真正执行)

插件拦截器 = 在 Executor/StatementHandler 的外层加代理
  Plugin → Executor → Plugin → StatementHandler → ...
```

## 3. 设计模式汇总

| 设计模式 | 应用 | 好处 |
|---------|------|------|
| **建造者** | SqlSessionFactoryBuilder / XMLConfigBuilder | 复杂对象分步构建 |
| **工厂方法** | SqlSessionFactory | 解耦创建逻辑 |
| **代理** | MapperProxy（JDK动态代理） | Mapper 接口无实现类 |
| **装饰器** | CachingExecutor | 缓存功能无侵入叠加 |
| **责任链** | InterceptorChain + 插件 | 可扩展的拦截器机制 |
| **模板方法** | BaseExecutor | 骨架固定，子类差异化 |

## 核心要点回顾

- 初始化：XML/注解 → Configuration（全局唯一）
- 执行链：MapperProxy → SqlSession → Executor → StatementHandler → ResultSetHandler
- Executor 三层装饰：Caching(二级) → Base(一级) → Simple(真正执行)
- Mapper 接口无实现类 = JDK 动态代理（`MapperProxy`）
- 源码阅读重点：`MapperProxy` → `Executor.query()` → `SimpleExecutor.doQuery()`

## 参考资料

1. MyBatis 3 源码 — github.com/mybatis/mybatis-3
2. 《MyBatis 3 源码深度解析》
