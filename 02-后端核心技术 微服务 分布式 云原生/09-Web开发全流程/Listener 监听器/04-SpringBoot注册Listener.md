# 04-Spring Boot 注册 Listener
> 注册方式、可注入性、与 Spring 事件体系的关系——"Boot 时代 Listener 的正确用法"

## 📚 目录
1. [注册方式对比](#1-注册方式对比)
2. [方式一：@WebListener + @ServletComponentScan](#2-方式一weblistener--servletcomponentscan)
3. [方式二：注册 Bean](#3-方式二注册-bean)
4. [Listener 与 Spring 事件体系](#4-listener-与-spring-事件体系)
5. [Boot 项目的正确姿势](#5-boot-项目的正确姿势)
6. [核心要点](#6-核心要点)
7. [参考来源](#7-参考来源)

## 1. 注册方式对比

| 方式 | 写法 | 可注入 Bean | 推荐度 |
|------|------|:---:|:---:|
| @WebListener + 扫描 | 注解 | ❌ | ⚠️ |
| **注册 Bean** | Java 配置 | ✅ | ✅ 推荐 |

> 🎯 **Boot 时代结论**：Listener 注册为 **Spring Bean**（可注入依赖）——@WebListener 的 Listener 无法 @Autowired（非 Spring 管理）。

## 2. 方式一：@WebListener + @ServletComponentScan

```java
@WebListener
public class AppInitListener implements ServletContextListener {
    // 注意：无法 @Autowired（不是 Spring Bean）
    @Override
    public void contextInitialized(ServletContextEvent sce) { ... }
}
```

```java
@SpringBootApplication
@ServletComponentScan        // 扫描 @WebListener/@WebFilter/@WebServlet
public class Application { ... }
```

| 优点 | 缺点 |
|------|------|
| 简单 | **无法注入 Spring Bean** |
| 传统写法 | 与 Spring 容器隔离 |

> ⚠️ **@WebListener 的局限**：Listener 实例由容器创建，**不是 Spring Bean**——想用 Service/Redis 等 Bean 时此方式不可用。

## 3. 方式二：注册 Bean

```java
// 方式 1：Listener 加 @Component（推荐）
@Component
public class AppInitListener implements ServletContextListener {

    @Autowired
    private DictService dictService;      // ✅ 可注入

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        dictService.reloadCache();        // 用 Bean 做初始化
    }
}

// 方式 2：Config 中注册
@Configuration
public class ListenerConfig {

    @Bean
    public ServletContextListener appInitListener() {
        return new AppInitListener();     // 或 new 一个
    }
}
```

```java
// 方式 3：ServletListenerRegistrationBean（显式注册）
@Configuration
public class ListenerConfig {

    @Bean
    public ServletListenerRegistrationBean<HttpSessionListener> sessionListener() {
        return new ServletListenerRegistrationBean<>(new OnlineUserListener());
    }
}
```

| 方式 | 说明 |
|------|------|
| @Component | 最简单，可注入 |
| @Bean 配置 | 显式控制 |
| ServletListenerRegistrationBean | 最显式（类似 FilterRegistrationBean） |

> 🎯 **推荐方式**：**Listener 加 @Component（可注入 Bean）**——与 [Filter 体系](../Filter%20过滤器/04-SpringBoot注册Filter.md) 的推荐一致（组件可注入 + 容器识别）。

## 4. Listener 与 Spring 事件体系

```text
两套事件机制的对比：
  JavaWeb Listener：Servlet 容器事件（启动/会话/请求）
  Spring 事件：Spring 容器事件（ContextRefreshed/ApplicationReady...）

层级不同：
  Servlet 容器事件 → Listener 监听
  Spring 容器事件 → ApplicationListener / @EventListener 监听
```

```java
// Spring 事件（Boot 项目更常用）
@Component
public class AppReadyListener {

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        // 应用就绪后初始化（可注入 Bean）
        dictService.reloadCache();
    }
}
```

| 场景 | JavaWeb Listener | Spring 事件 |
|------|------------------|-------------|
| 应用启动初始化 | contextInitialized | ApplicationReadyEvent |
| 会话管理 | HttpSessionListener | Spring Session 事件 |
| 可注入 | ❌（需注册 Bean） | ✅ 天然 |
| Boot 推荐 | 传统项目 | **Boot 项目** |

> 🎯 **选型结论**：**Boot 项目的"启动初始化"用 Spring 事件（ApplicationReadyEvent）**——Listener 只用于"必须监听 Servlet 容器事件"的场景（在线统计等）。

## 5. Boot 项目的正确姿势

```text
Boot 项目场景选择：
  ① 启动初始化（缓存预热/配置加载）
     → ApplicationRunner / ApplicationReadyEvent（可注入）✅
  ② 在线人数统计（必须监听 Session）
     → HttpSessionListener 注册为 Bean ✅
  ③ 访问统计（请求级）
     → Filter 或 RequestListener（Filter 更常用）✅
  ④ 传统 JavaWeb 项目
     → @WebListener + 扫描
```

```java
// Boot 完整示例：在线统计（Listener 注册为 Bean）
@Component
public class OnlineUserListener implements HttpSessionListener {

    @Autowired
    private RedisTemplate<String, String> redis;   // ✅ 可注入

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        // 用 Redis 存在线数（分布式友好）
        redis.opsForValue().increment("online:count");
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        redis.opsForValue().decrement("online:count");
    }
}
```

> 🎯 **Boot 姿势总结**：**能注入的用 Spring 方式（事件/Runner），必须监听容器事件的用 Listener 且注册为 Bean**——两种机制各司其职。

## 6. 核心要点

> 🎯 **核心要点**：
> - 注册两方式：@WebListener（不可注入）vs 注册 Bean（可注入，推荐）；
> - 推荐：Listener 加 @Component（与 Filter 体系一致）；
> - 两套事件体系：Servlet 容器事件（Listener）vs Spring 事件（@EventListener）——层级不同；
> - Boot 场景选择：启动初始化用 ApplicationRunner/事件；会话统计用 Listener（注册 Bean）；
> - 分布式在线统计：Listener + Redis（单机内存有局限）；
> - 面试：Listener 与 Spring 事件的区别 + Boot 注册方式。

## 7. 参考来源

- [Spring Boot Web 组件文档（Listener 注册）](https://docs.spring.io/spring-boot/reference/web/servlet.html)
- [Spring 事件文档](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html)
- [Baeldung：Spring Boot Listeners](https://www.baeldung.com/spring-boot-listeners)

---

**下一模块**：[05-生产实践与面试题](05-生产实践与面试题.md)　/　**返回总览**：[00-总览](00-Listener监听器总览.md)
