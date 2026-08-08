# 02-ServletContextListener 与启动初始化
> 最常用的监听器：应用启动/关闭回调、初始化任务（缓存预热/配置加载/定时任务）、context 共享——"启动时把该准备的准备好"

## 📚 目录
1. [ServletContextListener 是什么](#1-servletcontextlistener-是什么)
2. [contextInitialized：启动回调](#2-contextinitialized启动回调)
3. [contextDestroyed：关闭回调](#3-contextdestroyed关闭回调)
4. [初始化任务实战](#4-初始化任务实战)
5. [ServletContext 共享数据](#5-servletcontext-共享数据)
6. [与 Spring Boot 启动事件的对比](#6-与-spring-boot-启动事件的对比)
7. [核心要点](#7-核心要点)
8. [参考来源](#8-参考来源)

## 1. ServletContextListener 是什么

```text
监听"应用"（ServletContext）的生命周期：
  contextInitialized：应用启动时（创建后）
  contextDestroyed：应用关闭时（销毁前）

特点：
  只触发一次（应用生命周期内）
  最常用的 Listener（启动初始化标配）
```

> 🎯 **定位**：ServletContextListener = **"应用启动与关闭的钩子"**——Spring Boot 的 `ApplicationRunner`/启动事件也做类似的事（见第 6 节对比）。

## 2. contextInitialized：启动回调

```java
@WebListener
public class AppInitListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("应用启动，开始初始化...");

        ServletContext context = sce.getServletContext();

        // ① 设置全局配置
        context.setAttribute("appName", "order-service");
        context.setAttribute("appVersion", "1.0.0");

        // ② 加载配置到全局
        Properties config = loadConfig();
        context.setAttribute("config", config);

        System.out.println("应用初始化完成");
    }

    private Properties loadConfig() {
        // 加载配置文件（如数据库/第三方配置）
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("app.properties")) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("配置加载失败", e);
        }
        return props;
    }
}
```

| 初始化任务 | 示例 |
|-----------|------|
| 全局配置 | context.setAttribute 存配置 |
| 缓存预热 | 热点数据提前加载 |
| 定时任务启动 | 调度器启动 |
| 资源初始化 | 连接池/线程池 |
| 数据字典 | 字典表加载到内存 |

> 🎯 **启动回调的职责**：**"应用准备好对外服务前，把该准备的准备好"**——全局配置、缓存预热、定时任务都在这里做。

## 3. contextDestroyed：关闭回调

```java
@WebListener
public class AppShutdownListener implements ServletContextListener {

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("应用关闭，释放资源...");

        // ① 停止定时任务
        scheduler.shutdown();

        // ② 关闭资源
        // connectionPool.close();
        // executor.shutdown();

        // ③ 记录关闭日志
        System.out.println("资源释放完成");
    }
}
```

| 关闭任务 | 说明 |
|----------|------|
| 停止定时任务 | 防关闭后仍执行 |
| 关闭线程池 | 优雅停机 |
| 关闭连接池 | 释放数据库连接 |
| 持久化内存状态 | 关前保存 |

> ⚠️ **关闭回调的价值**：**优雅停机**——先停调度、再关资源；现代框架（Spring）有更完善的优雅停机（见[Spring Boot 体系](../../../06-Spring全家桶/SpringBoot/00-SpringBoot知识体系总览.md)），Listener 是传统方式。

## 4. 初始化任务实战

```java
// 综合示例：启动时初始化缓存 + 定时任务
@WebListener
public class AppInitListener implements ServletContextListener {

    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();

        // ① 缓存预热：热点字典加载到内存
        Map<String, String> dict = loadDictFromDb();
        ctx.setAttribute("dictCache", dict);

        // ② 定时刷新缓存
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            ctx.setAttribute("dictCache", loadDictFromDb());
        }, 10, 10, TimeUnit.MINUTES);       // 每 10 分钟刷新

        // ③ 启动监控任务
        System.out.println("初始化完成: 字典 " + dict.size() + " 条");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}
```

> 💡 **初始化任务设计要点**：初始化失败要**快速失败**（启动即报错，别带病运行）；定期刷新与启动预热配合（数据不会永远不变）。

## 5. ServletContext 共享数据

```java
// context 是"全局共享"（所有请求/Servlet 可见）
// 存：初始化时
context.setAttribute("dictCache", dict);

// 取：任何 Servlet/Filter 中
ServletContext context = getServletContext();
Map<String, String> dict = (Map<String, String>) context.getAttribute("dictCache");
```

| 数据 | 存放位置 | 作用域 |
|------|---------|--------|
| ServletContext | 全局 | 所有应用 |
| HttpSession | 会话 | 单用户 |
| Request | 请求 | 单请求 |

> ⚠️ **全局数据纪律**：context 存的是"全局共享且只读/线程安全"的数据（字典/配置）——**可变状态放全局要线程安全**（并发读写问题）。

## 6. 与 Spring Boot 启动事件的对比

| 方式 | 时机 | 特点 |
|------|------|------|
| ServletContextListener | 容器启动早期 | 传统 JavaWeb；Spring 容器可能未就绪 |
| Spring 事件（ApplicationReadyEvent） | Spring 容器就绪后 | **可注入 Bean**（推荐） |
| ApplicationRunner/CommandLineRunner | 容器就绪后 | **Spring Boot 官方推荐** |

```java
// Spring Boot 推荐方式：ApplicationRunner（替代 Listener 的初始化）
@Component
public class AppInitRunner implements ApplicationRunner {

    @Autowired
    private DictService dictService;      // ✅ 可注入

    @Override
    public void run(ApplicationArguments args) {
        dictService.reloadCache();        // 缓存预热
    }
}
```

> 🎯 **选型结论**：**Spring Boot 项目用 ApplicationRunner/启动事件**（可注入 Bean、容器就绪）；ServletContextListener 是**传统 JavaWeb 方式**（Spring 容器未就绪，无法注入）——老项目看懂，新项目用 Spring 方式。

## 7. 核心要点

> 🎯 **核心要点**：
> - ServletContextListener = 应用启动/关闭钩子（只触发一次）；
> - 启动回调：全局配置、缓存预热、定时任务启动（"对外服务前准备好"）；
> - 关闭回调：优雅停机（停调度/关资源）；
> - context 全局共享：只放"全局且线程安全"的数据；
> - 初始化失败快速失败（别带病运行）；
> - **Spring Boot 用 ApplicationRunner 替代**（可注入 Bean）；Listener 用于传统 JavaWeb。

## 8. 参考来源

- [Jakarta Servlet 规范（ServletContextListener）](https://jakarta.ee/specifications/servlet/)
- [Spring Boot ApplicationRunner 文档](https://docs.spring.io/spring-boot/reference/features/spring-application.html)

---

**下一模块**：[03-Session与Request监听器](03-Session与Request监听器.md)　/　**返回总览**：[00-总览](00-Listener监听器总览.md)
