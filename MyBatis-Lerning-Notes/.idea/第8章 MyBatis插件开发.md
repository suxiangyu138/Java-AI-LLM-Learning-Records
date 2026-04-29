# 第8章 MyBatis 插件开发
## 8.1 插件概述
### 8.1.1 什么是 MyBatis 插件
MyBatis 提供**插件（Plugin）扩展机制**，基于 **拦截器（Interceptor）** 实现，
可以在 MyBatis 核心执行环节**拦截、增强、改写原有逻辑**。

### 8.1.2 核心作用
- 拦截 SQL 执行、参数处理、结果封装、分页逻辑
- 统一公共功能：**分页、数据脱敏、SQL 日志打印、字段自动填充、乐观锁、SQL 性能监控**
- 不修改原生源码，无侵入式增强

### 8.1.3 可拦截的四大核心对象
MyBatis 只允许拦截以下 4 个核心组件的方法：
1. `Executor`：拦截增删改查、事务、缓存操作
2. `StatementHandler`：拦截 SQL 编译、预编译、执行
3. `ParameterHandler`：拦截 SQL 参数设置与处理
4. `ResultSetHandler`：拦截查询结果集封装、映射

---

## 8.2 插件核心原理
### 8.2.1 底层机制
MyBatis 插件基于 **JDK 动态代理 + 责任链模式**：
1. 目标对象创建时，生成代理对象
2. 执行方法前，先走拦截器链
3. 多个插件按配置顺序逐层拦截、逐层放行

### 8.2.2 核心注解
```java
@Intercepts({
    @Signature(
        type = 拦截的核心对象,
        method = 拦截的方法名,
        args = {方法参数类型}
    )
})
```
- `@Intercepts`：标记当前类为拦截器
- `@Signature`：指定要拦截的类、方法、参数

---

## 8.3 自定义插件开发步骤
### 步骤1：自定义拦截器，实现 Interceptor 接口
核心三个方法：
1. `intercept()`：**拦截增强逻辑（核心）**
2. `plugin()`：生成代理对象
3. `setProperties()`：读取配置文件参数

### 步骤2：配置拦截器
在 `mybatis-config.xml` 注册插件，使其生效。

### 步骤3：测试运行
执行 SQL，观察拦截逻辑是否触发。

---

## 8.4 入门案例：自定义 SQL 日志插件
### 8.4.1 自定义拦截器
```java
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import java.sql.Statement;
import java.util.Properties;

@Intercepts({
        @Signature(
                type = StatementHandler.class,
                method = "update",
                args = {Statement.class}
        ),
        @Signature(
                type = StatementHandler.class,
                method = "query",
                args = {Statement.class}
        )
})
public class SqlLogPlugin implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 前置增强：执行SQL之前
        System.out.println("====== MyBatis 插件拦截：即将执行 SQL ======");
        
        // 放行，执行原有方法
        Object result = invocation.proceed();
        
        // 后置增强：SQL执行完成后
        System.out.println("====== SQL 执行结束 ======");
        return result;
    }

    @Override
    public Object plugin(Object target) {
        // 包装目标对象，创建代理
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 读取配置文件中自定义参数
    }
}
```

### 8.4.2 全局配置文件注册插件
在 `mybatis-config.xml` 中添加 `<plugins>` 标签：
```xml
<configuration>
    <!-- 注册自定义插件 -->
    <plugins>
        <plugin interceptor="com.plugin.SqlLogPlugin"/>
    </plugins>
</configuration>
```

### 8.4.3 运行效果
执行任意 CRUD 操作，控制台自动打印自定义日志，插件拦截生效。

---

## 8.5 常用企业级插件场景
1. **分页插件**
   拦截 `StatementHandler`，自动拼接 `limit`，代表框架：`PageHelper`

2. **公共字段自动填充**
   创建时间、更新时间、创建人、更新人，拦截参数处理器统一赋值

3. **SQL 性能监控**
   记录 SQL 执行耗时，慢 SQL 告警

4. **数据脱敏**
   查询结果返回前，对手机号、身份证、地址自动脱敏

5. **多租户隔离**
   拦截 SQL，自动拼接租户ID条件，实现多租户系统数据隔离

6. **乐观锁自动控制**
   自动识别 version 字段，更新时携带版本号、防止并发冲突

---

## 8.6 插件执行顺序
1. 配置文件中**从上到下**依次加载
2. 多个插件拦截同一方法：
    - 执行前：从上到下
    - 执行后：从下到上（栈结构）

---

## 8.7 插件开发注意事项
1. MyBatis **仅支持固定4种对象拦截**，不能随意拦截任意类
2. 插件过度使用会增加执行链路复杂度、影响性能
3. 禁止拦截核心事务、连接管理底层逻辑，避免数据源异常
4. 第三方插件（PageHelper）不要与自定义插件冲突
5. 插件中修改 SQL、参数时需严谨，防止 SQL 语法错误

---

## 8.8 本章总结
1. MyBatis 插件依托 **Interceptor 拦截器** 实现扩展
2. 只能拦截：`Executor`、`StatementHandler`、`ParameterHandler`、`ResultSetHandler`
3. 核心注解：`@Intercepts` + `@Signature`
4. 开发流程：实现拦截器接口 → 重写拦截方法 → 全局配置注册
5. 企业常用：分页、字段填充、日志、脱敏、多租户等通用能力
6. 插件为无侵入增强，是 MyBatis 生态扩展的核心方式

---
