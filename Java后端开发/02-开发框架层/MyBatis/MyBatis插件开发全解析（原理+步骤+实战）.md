# MyBatis 插件开发全解析

> **文档定位**：Java 后端技术参考文档 | MyBatis 插件开发原理与实践  
> **核心本质**：基于拦截器模式和 JDK 动态代理实现的扩展组件，不修改源码的前提下对 SQL 执行全流程增强  
> **典型场景**：SQL 日志打印、性能监控、数据脱敏、分页处理、参数加密

---

## 目录

- [一、插件核心认知](#一插件核心认知)
- [二、核心接口与注解](#二核心接口与注解)
- [三、插件开发完整步骤（实战）](#三插件开发完整步骤实战)
- [四、关键注意事项](#四关键注意事项)
- [五、常见插件场景扩展](#五常见插件场景扩展)
- [六、总结](#六总结)

---

## 一、插件核心认知

### 可拦截的四大核心组件

| 组件 | 职责 | 可拦截方法 | 常用场景 |
|------|------|-----------|----------|
| **Executor** | SQL 执行器 | `query`、`update`、`commit`、`rollback` | **性能监控**、缓存扩展 |
| **StatementHandler** | 语句处理器 | `prepare`、`parameterize`、`query`、`update` | **SQL 重写**、物理分页 |
| **ParameterHandler** | 参数处理器 | `setParameters` | 参数加密、特殊类型处理 |
| **ResultSetHandler** | 结果集处理器 | `handleResultSets` | **数据脱敏**、自定义类型转换 |

### 插件核心原理

| 机制 | 说明 |
|------|------|
| **JDK 动态代理** | 为四大核心组件生成代理对象，拦截目标方法 |
| **责任链模式** | 多个插件按配置顺序层层包装，形成插件链 |
| **注解声明机制** | `@Intercepts` + `@Signature` 声明拦截目标 |

---

## 二、核心接口与注解

### Interceptor 接口

```java
public interface Interceptor {
    // 核心方法：拦截目标方法，编写自定义增强逻辑
    Object intercept(Invocation invocation) throws Throwable;

    // 生成代理对象，默认使用 Plugin.wrap()
    default Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    // 读取插件配置属性
    default void setProperties(Properties properties) {}
}
```

### @Intercepts 与 @Signature

```java
@Intercepts({
    @Signature(
        type = Executor.class,       // 目标组件
        method = "query",             // 目标方法名
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}  // 参数类型
    ),
    @Signature(
        type = StatementHandler.class,
        method = "prepare",
        args = {Connection.class, Integer.class}
    )
})
public class MyCustomPlugin implements Interceptor {
    // 实现接口方法...
}
```

---

## 三、插件开发完整步骤（实战）

> 以开发 **SQL 执行时间监控插件** 为例。

### 步骤 1：Maven 依赖

```xml
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.5.16</version>
</dependency>
```

### 步骤 2：编写插件类

```java
@Intercepts({
    @Signature(type = Executor.class, method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "update",
        args = {MappedStatement.class, Object.class})
})
public class SqlExecutionTimePlugin implements Interceptor {

    private Long slowSqlThreshold;  // 慢 SQL 阈值（ms）

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        String method = ms.getId();
        String sqlType = ms.getSqlCommandType().name();
        long start = System.currentTimeMillis();

        try {
            return invocation.proceed();  // 执行原方法
        } finally {
            long duration = System.currentTimeMillis() - start;
            if (duration >= slowSqlThreshold) {
                System.out.printf("[慢SQL] %s | %s | %d ms%n", sqlType, method, duration);
            } else {
                System.out.printf("[SQL] %s | %s | %d ms%n", sqlType, method, duration);
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        this.slowSqlThreshold = Long.parseLong(
            properties.getProperty("slowSqlThreshold", "500"));
    }
}
```

### 步骤 3：注册插件

#### XML 配置方式

```xml
<configuration>
    <plugins>
        <plugin interceptor="com.example.plugin.SqlExecutionTimePlugin">
            <property name="slowSqlThreshold" value="300"/>
        </plugin>
    </plugins>
</configuration>
```

#### Spring Boot 配置方式

```java
@Configuration
@MapperScan("com.example.mapper")
public class MyBatisConfig {
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setPlugins(new SqlExecutionTimePlugin());
        return factoryBean.getObject();
    }
}
```

### 步骤 4：测试验证

```java
try (SqlSession session = sqlSessionFactory.openSession()) {
    UserMapper mapper = session.getMapper(UserMapper.class);
    User user = mapper.selectById(1L);
}
// 控制台输出：[SQL] SELECT | com.example.mapper.UserMapper.selectById | 56 ms
```

---

## 四、关键注意事项

| 注意事项 | 说明 |
|----------|------|
| **拦截签名精准匹配** | `@Signature` 的 `args` 必须与目标方法参数类型、顺序完全一致 |
| **必须调用 `invocation.proceed()`** | 否则阻断原方法执行 |
| **避免过度拦截** | 只拦截必要的组件和方法 |
| **多插件顺序** | 按配置顺序形成插件链，外层先执行前置、后执行后置 |
| **线程安全** | 插件实例是单例，成员变量需线程安全 |
| **不随意修改核心对象** | 若需修改 SQL，通过 `MetaObject` 操作 `BoundSql` |

---

## 五、常见插件场景扩展

### 场景 1：数据脱敏插件

拦截 `ResultSetHandler.handleResultSets`，对敏感字段（手机号、身份证）脱敏：

```java
@Intercepts({
    @Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class})
})
public class DataDesensitizationPlugin implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object result = invocation.proceed();
        if (result instanceof List) {
            for (Object obj : (List<?>) result) {
                desensitize(obj);  // 手机号脱敏：138****1234
            }
        }
        return result;
    }
}
```

### 场景 2：SQL 分页插件

拦截 `StatementHandler.prepare`，修改 SQL 实现物理分页（MySQL LIMIT）。

---

## 六、总结

```
实现 Interceptor 接口 → 声明 @Intercepts + @Signature → 注册插件 → 增强生效
```

| 典型场景 | 拦截组件 |
|----------|----------|
| 性能监控 | Executor |
| SQL 重写 / 分页 | StatementHandler |
| 参数加密 | ParameterHandler |
| 数据脱敏 | ResultSetHandler |
