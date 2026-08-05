# 05 - MyBatis 插件与拦截器

> 🎯 MyBatis 插件是 JDK 动态代理的经典应用——通过拦截 Executor、StatementHandler、ParameterHandler、ResultSetHandler 四个组件，实现分页、审计、SQL 日志等横切关注点

---

## 目录

1. [插件原理](#1-插件原理)
2. [四大拦截点](#2-四大拦截点)
3. [实战：分页插件与 SQL 日志](#3-实战分页插件与-sql-日志)

---

## 1. 插件原理

```text
MyBatis 插件 = 责任链模式 + JDK 动态代理

拦截流程：
  原始对象 → Plugin.wrap() → 动态代理
  调用方法 → Interceptor.intercept() → 执行拦截逻辑
  → Invocation.proceed() → 调用原始方法
```

```java
// 插件必须实现 Interceptor 接口
@Intercepts({
    @Signature(
        type = Executor.class,        // 拦截的接口
        method = "query",             // 拦截的方法
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}
    )
})
public class MyPlugin implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 前置逻辑
        System.out.println("执行前");
        Object result = invocation.proceed();  // 调用原始方法
        // 后置逻辑
        System.out.println("执行后");
        return result;
    }
}
```

## 2. 四大拦截点

| 拦截对象 | 可拦截方法 | 典型用途 |
|---------|-----------|---------|
| **Executor** | update/query/commit/rollback | 分页、缓存、事务 |
| **StatementHandler** | prepare/parameterize/batch | SQL 改写、分表 |
| **ParameterHandler** | setParameters | 参数加密、审计 |
| **ResultSetHandler** | handleResultSets | 结果脱敏、加密 |

## 3. 实战：分页插件与 SQL 日志

```java
// SQL 慢查询日志插件
@Intercepts({
    @Signature(type = StatementHandler.class,
               method = "prepare",
               args = {Connection.class, Integer.class})
})
public class SlowSqlInterceptor implements Interceptor {
    private final long thresholdMs;  // 慢查询阈值

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = invocation.proceed();  // 执行 SQL
        long elapsed = System.currentTimeMillis() - start;

        if (elapsed > thresholdMs) {
            // 从 StatementHandler 获取原始 SQL
            StatementHandler handler = (StatementHandler) invocation.getTarget();
            BoundSql boundSql = handler.getBoundSql();
            String sql = boundSql.getSql().replaceAll("\\s+", " ");
            System.err.printf("[SLOW SQL] %dms | %s%n", elapsed, sql);
        }
        return result;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);  // 用 Plugin.wrap 生成代理
    }
}
```

```xml
<!-- mybatis-config.xml 中注册插件 -->
<plugins>
    <plugin interceptor="com.example.SlowSqlInterceptor">
        <property name="thresholdMs" value="1000"/>
    </plugin>
</plugins>
```

### 常用开源插件

| 插件 | 功能 | 拦截点 |
|------|------|:---:|
| **PageHelper** | 物理分页 | Executor.query |
| **MyBatis-Plus 分页插件** | 同上 | Executor.query |
| **tk.mybatis Mapper** | 通用 CRUD | StatementHandler |
| **SQL 监控插件** | Druid/JSqlParser 改写 | StatementHandler |

## 核心要点回顾

- 插件 = `@Intercepts` + `@Signature` + `Plugin.wrap()`
- 四大拦截点：Executor(通用) / StatementHandler(SQL改写) / ParameterHandler / ResultSetHandler
- 按需拦截，不要滥用（每个插件都是一层代理，影响性能）
- `invocation.proceed()` 是调用原始方法的唯一入口
- 多插件执行顺序 = 配置顺序（先注册的先执行外层代理）

## 参考资料

1. MyBatis 插件文档
2. PageHelper 源码 — github.com/pagehelper/Mybatis-PageHelper
