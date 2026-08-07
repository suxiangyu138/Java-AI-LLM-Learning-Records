# 05 Web 上下文与作用域速查

> WebApplicationContext 层次、request/session/application 作用域、RequestContextHolder——"Web 里的容器长什么样"的答案

---

## 📚 目录

1. [WebApplicationContext：三层上下文](#1-webapplicationcontext三层上下文)
2. [Web 作用域：request/session/application](#2-web-作用域requestsessionapplication)
3. [作用域代理：ScopedProxyMode](#3-作用域代理scopedproxymode)
4. [RequestContextHolder：请求线程绑定](#4-requestcontextholder请求线程绑定)
5. [Servlet 集成与上下文初始化](#5-servlet-集成与上下文初始化)

---

## 1. WebApplicationContext：三层上下文

```text
Web 应用的容器层次：
  Root WebApplicationContext（根上下文）
    ├── 数据源/Service/Repository 等业务 Bean（无 Web 依赖）
    └── Servlet WebApplicationContext（子上下文，MVC 专用）
          ├── @Controller/@RestController
          ├── HandlerMapping/HandlerAdapter
          └── 视图解析器、异常处理器等 MVC 组件
  父子关系：子上下文可拿父的 Bean；父看不到子的 Bean ★

  启动链：ContextLoaderListener 建根 → DispatcherServlet 建子（见 MVC 系列 01）
```

| 上下文类型 | 职责 |
|-----------|------|
| `WebApplicationContext`（接口） | 带 `ServletContext` 感知的容器 |
| `ServletWebApplicationContext`（7.x 类名） | Servlet 栈实现（MVC 用） |
| `GenericWebApplicationContext` | 编程式 Web 容器 |
| `AnnotationConfigServletWebServerApplicationContext` | Boot 内嵌服务器容器 |
| `ReactiveWebApplicationContext` | 响应式栈（WebFlux） |

> 🎯 **核心要点**：**Service 层放根上下文、控制器放子上下文**——父子分离让"业务核心不依赖 Web"；子上下文覆盖同名 Bean 时子优先；面试答"SpringMVC 容器结构"先画这棵父子树。

### 1.1 上下文初始化细节与 Bean 查找顺序

```text
Bean 查找顺序（父子容器）：
  子上下文 getBean → 自己先找 → 找不到 → 父上下文
  覆盖规则：子上下文同名 Bean 遮蔽父的（不是替换——父的仍在容器里）
  销毁顺序：子先销毁 → 父后销毁（Web 容器关闭时）

  经典误区：父上下文里 @Autowired 子上下文的 Bean
  → 编译通过、运行报 NoSuchBeanDefinitionException（依赖方向只能"子 → 父"）
```

| 初始化方式 | 说明 | 使用场景 |
|-----------|------|---------|
| `ContextLoaderListener` | web.xml 建根上下文 | 传统 WAR（双上下文模型） |
| `DispatcherServlet`（构造参数指定） | 建子上下文 | 传统 WAR |
| `AnnotationConfigWebApplicationContext` | 编程式创建 | 嵌入式/测试 |
| Boot `SpringApplication` | 单上下文直接启动 | Boot 内嵌（99% 场景） |

> ⚠️ **Boot 与 WAR 的上下文模型差异**：Boot 是**单上下文**——SpringApplication 建一个上下文，内嵌 Tomcat 的 DispatcherServlet 直接复用；传统 WAR 才是"根 + 子"双上下文（web.xml 配 ContextLoaderListener + DispatcherServlet）。面试别把两者混为一谈。

### 1.2 上下文相关的关键接口与 Bean

| 接口/类 | 职责 | 典型用法 |
|---------|------|---------|
| `WebApplicationContext` | 带 ServletContext 感知的容器接口 | 注入后 `getServletContext()` |
| `WebApplicationContextUtils` | 静态工具：从 ServletContext 取容器 | 遗留代码/工具类 |
| `ApplicationContextAware` | 容器感知回调 | 拿到容器做动态查找（慎用） |
| `ServletContextAware` | ServletContext 感知回调 | 读取全局配置（耦合，慎用） |
| `GenericWebApplicationContext` | 编程式 Web 容器 | 测试/嵌入式场景 |

```java
// 动态获取容器的正确姿势：ObjectProvider（避免静态持有）
@Autowired
private ObjectProvider<MessageSource> messageSourceProvider;

public String msg(String code) {
    return messageSourceProvider.getIfAvailable()
            .map(ms -> ms.getMessage(code, null, Locale.getDefault()))
            .orElse(code);          // 容器里没有就兜底
}
```

> 💡 面试考"怎么在任意地方拿容器"——答**注入优先**（ObjectProvider/ObjectFactory），`WebApplicationContextUtils` 是给非 Spring 管理的遗留代码用的；静态持有容器 = 内存泄漏 + 测试困难。

## 2. Web 作用域：request/session/application

| 作用域 | 生命周期 | 适用 | 常量 |
|--------|---------|------|------|
| `request` | 单次 HTTP 请求 | 请求内共享状态 | `WebApplicationContext.SCOPE_REQUEST` |
| `session` | 单个 HTTP 会话 | 登录态/购物车 | `SCOPE_SESSION` |
| `application` | ServletContext 生命周期 | 全局共享（≈单例但带 Web 语义） | `SCOPE_APPLICATION` |
| `websocket` | WebSocket 会话 | 实时推送状态 | `SCOPE_WEBSOCKET`（spring-websocket） |

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class RequestContext {
    private String traceId;
    // getter/setter...
}
```

> ⚠️ **与单例的区别**：request/session Bean 每次请求/会话**新建实例**——单例 Bean 里注入它们必须用代理（见下节）；application 作用域与 singleton 生命周期几乎一致，但语义属于 Servlet 容器。

### 2.1 作用域实现类与生命周期细节

| 作用域 | 实现类 | 绑定容器 |
|--------|--------|---------|
| request | `RequestScope` | RequestAttributes |
| session | `SessionScope` | HttpSession |
| application | `ServletContextScope` | ServletContext |
| websocket | `WebSocketScope` | WebSocketSession（spring-websocket） |

```java
// 编程式获取当前作用域实例（绕过代理，谨慎使用）
@Autowired
private ObjectProvider<UserContext> userContextProvider;

public void handle() {
    UserContext ctx = userContextProvider.getIfAvailable();   // 当前请求实例
    // getIfAvailable 返回 null = 作用域未激活（如非 Web 线程）
}
```

| 生命周期要点 | 说明 |
|--------------|------|
| request Bean 销毁时机 | 请求处理完成（响应提交后） |
| session Bean 销毁 | 会话过期/显式 invalidate |
| 作用域 Bean 与并发 | 每请求/会话独立实例——**天然线程安全**（单例才共享） |
| 非 Web 环境 | 无 RequestAttributes 时获取报 IllegalStateException |
| 用途定位 | 请求级临时状态（traceId/当前用户）放 request；登录态/购物车放 session |

> 💡 面试加分：`ObjectProvider` 是"作用域感知获取"的推荐姿势——比 `@Autowired` 直注更能表达"可能不存在/延迟获取"语义；getIfAvailable 判空避免作用域未激活时报错。

### 2.2 request 作用域经典场景与陷阱

```java
// 经典场景：请求级 TraceContext（替代 ThreadLocal 方案）
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST,
       proxyMode = ScopedProxyMode.TARGET_CLASS)
public class TraceContext {
    private String traceId;
    private String userId;
    // getter/setter...
}

// 使用：任意层注入即当前请求的实例（代理解析）
@Service
public class OrderService {
    private final TraceContext trace;          // 代理——每请求实时解析
    public Order save(Order o) {
        o.setTraceId(trace.getTraceId());      // 当前请求的值
        return repo.save(o);
    }
}
```

| 陷阱 | 现象 | 解法 |
|------|------|------|
| 忘记 proxyMode | 单例持有"第一个请求的实例" | 跨作用域注入必配代理 |
| 在非请求线程访问 | 作用域未激活异常 | 显式传参或 TaskDecorator |
| 请求级数据塞太多 | 每请求全量构建 | 只放真正请求级的；静态配置走单例 |
| 与 ThreadLocal 混用 | 两套上下文不一致 | 选一套（作用域 Bean 或显式传递） |

> ⚠️ **同类对比**：request 作用域 Bean 与 ThreadLocal 都能存"请求级数据"——差别在**生命周期管理**：作用域 Bean 由容器创建/销毁（无泄漏），ThreadLocal 需手工清理（线程池复用场景易脏数据）——7.x 推荐作用域 Bean。

## 3. 作用域代理：ScopedProxyMode

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION,
       proxyMode = ScopedProxyMode.TARGET_CLASS)   // ★ 代理模式必配
public class UserContext { ... }

// 单例 Bean 注入 session 作用域 Bean：拿到的是代理
@Service
public class OrderService {
    private final UserContext userContext;   // 代理对象：每次访问实时解析当前会话实例
}
```

| 模式 | 机制 |
|------|------|
| `ScopedProxyMode.TARGET_CLASS` | CGLIB 代理（类代理，默认） |
| `ScopedProxyMode.INTERFACES` | JDK 接口代理 |
| `ScopedProxyMode.NO`（默认） | 无代理——单例注入时拿到"创建时的快照"（错） |

```text
代理工作方式：
  注入的是代理对象（占位）
  → 每次调用方法时：从 RequestAttributes/SessionAttributes 取当前作用域实例 → 委托
  → 请求结束/会话过期 → 实例销毁（可配 destroyMethod）
```

> 🎯 **核心要点**：**跨作用域注入 = 必配代理**——否则单例 Bean 持有的是"第一个请求的实例"（经典 bug：所有用户共享第一个用户的数据）；代理在每次方法调用时实时解析。

### 3.1 代理的限制与注意

| 限制 | 说明 |
|------|------|
| final 类/方法 | CGLIB 无法继承/覆写——session Bean 类不要 final |
| 私有方法调用 | 代理只拦公开调用——类内部 `this.method()` 不走代理 |
| equals/hashCode | 代理上调用得到代理身份而非目标实例身份（比较需谨慎） |
| 构造器 | 代理每次解析目标时走构造器——构造器里别做重活 |
| 序列化 | 代理通常不可序列化——跨 JVM 传递作用域 Bean 不现实 |

```java
// 反例：final 类 + 类代理 —— 运行期 CGLIB 失败
@Scope(value = WebApplicationContext.SCOPE_SESSION,
       proxyMode = ScopedProxyMode.TARGET_CLASS)
public final class UserContext { ... }   // ❌ CGLIB 无法代理 final 类
```

> ⚠️ **与 AOP 代理叠加**：作用域代理可以和事务代理共存（外层作用域代理 → 内层事务代理）；自调用（类内 `this.方法()`）两层代理都绕不过——需要代理语义就注入自引用或拆 Bean。

## 4. RequestContextHolder：请求线程绑定

```java
// 任意层获取当前请求/会话（无需注入）
HttpServletRequest request =
        ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest();

// 线程绑定原理：ThreadLocal —— 每个请求一个线程（或虚拟线程）
// 异步/线程池中不可用（除非 TaskDecorator 传递）
```

| 组件 | 作用 |
|------|------|
| `RequestContextHolder` | 线程 → RequestAttributes 绑定（请求/会话/Locale 三合一） |
| `RequestContextFilter` | Servlet 过滤器：每个请求绑定 + 释放 |
| `RequestContextListener` | 监听器方式绑定（Boot 内嵌容器用） |
| `WebRequest` / `NativeWebRequest` | 请求的 Web 抽象（不依赖 Servlet API 的轻接口） |
| `WebRequestInterceptor` | 拦截请求生命周期的轻量拦截器 |

> ⚠️ **异步大坑**：RequestContextHolder 是线程绑定的——**@Async 新线程/线程池里拿不到请求**（NPE）；解法：`TaskDecorator` 传递上下文、或显式传参（别依赖隐式请求状态）；虚拟线程下每请求一线程，同源成立。

### 4.1 跨线程传递与虚拟线程

```java
// 解法一：TaskDecorator 传递 RequestAttributes（@Async 线程池场景）
@Bean
ThreadPoolTaskExecutor asyncExecutor() {
    ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
    pool.setCorePoolSize(8);
    pool.setTaskDecorator(runnable -> {                       // ★ 关键
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        return () -> {
            try {
                RequestContextHolder.setRequestAttributes(attrs);
                runnable.run();
            } finally {
                RequestContextHolder.resetRequestAttributes();
            }
        };
    });
    return pool;
}
```

| 方案 | 适用 | 说明 |
|------|------|------|
| TaskDecorator | @Async / 自建线程池 | 统一传递，推荐 |
| 显式传参 | 一次性少量字段 | 简单直接，无魔法 |
| 虚拟线程 | Boot 4 默认请求处理 | 每请求一虚拟线程，同线程内可用 |
| TransmittableThreadLocal | 复杂上下文（traceId 等） | 阿里 TTL 库，传递后可回写 |

> 💡 **虚拟线程下的边界**：Boot 4 请求处理用虚拟线程，RequestContextHolder 正常可用；但 @Async（默认线程池）仍是平台线程池——**跨线程的传递需求不会因虚拟线程消失**，该用 TaskDecorator 还用。

### 4.2 RequestContextHolder 相关 API 速查

| API | 语义 | 注意 |
|-----|------|------|
| `getRequestAttributes()` | 取当前绑定（可能为 null） | 先判空再强转 |
| `setRequestAttributes(attrs)` | 绑定到当前线程 | 异步线程注入用 |
| `resetRequestAttributes()` | 解除绑定 | finally 中调用（防泄漏） |
| `ServletRequestAttributes` | Servlet 实现 | `getRequest()` / `getResponse()` |
| `WebRequest` 抽象 | 不依赖 Servlet 的轻接口 | 跨栈可测性好 |
| `LocaleContextHolder` | Locale 线程绑定 | 国际化切换 |

```java
// 安全获取请求的标准姿势
ServletRequestAttributes attrs =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
if (attrs != null) {
    HttpServletRequest req = attrs.getRequest();
    String ip = req.getRemoteAddr();
}
```

> 💡 7.x 加分点：`WebRequest` 抽象让"请求信息访问"不依赖 Servlet API——**Service 层可用 WebRequest 做参数传递**，WebFlux/MVC 双栈代码都能跑（可测性提升）。

## 5. Servlet 集成与上下文初始化

```text
传统 WAR 部署：
  web.xml → ContextLoaderListener（建根上下文）
          → DispatcherServlet（建子上下文 + 注册 HandlerMapping）
Boot 内嵌服务器：
  SpringApplication → 内嵌 Tomcat → 建 ServletWebApplicationContext
  （无 web.xml——一切注解/代码化）
```

| 集成点 | 说明 |
|--------|------|
| `ServletContextAware` | Bean 获取 ServletContext（慎用，耦合） |
| `ServletContextInitializer` | Boot 下编程式注册 Servlet/Filter（`@Bean` + 此接口） |
| `WebApplicationInitializer` | 传统 Servlet 3+ 无 web.xml 初始化 |
| 多部分解析 | `MultipartConfigElement` / Boot `spring.servlet.multipart.*` |

> 💡 Boot 4 下 99% 场景无需手工 Servlet 集成——`ServletContextInitializer` 是唯一常写的（注册自定义 Filter/Listener 时）；父子上下文模型由 Boot 自动搭好。

### 5.1 面试追问与生产实践

| 面试追问 | 回答要点 |
|----------|---------|
| 为什么需要 WebApplicationContext | 把 ServletContext 与容器打通：作用域、监听器、Web 组件注册 |
| request 作用域与 @RequestAttribute 什么关系 | 作用域是 Bean 生命周期；@RequestAttribute 是参数绑定——不同层 |
| RequestContextHolder 内部是什么 | ThreadLocal<RequestAttributes>——请求、会话、Locale 三合一 |
| 作用域代理与 AOP 代理区别 | 作用域代理是"每次解析实例"，AOP 代理是"增强方法"——语义完全不同 |
| Boot 下 RequestContext 由谁绑定 | 监听器（RequestContextListener）——内嵌容器用监听器不用过滤器 |
| 请求处理完 request Bean 什么时候销毁 | 响应提交后（ResponseCommitted）——过滤器/拦截器后置逻辑仍可见 |
| application 作用域和单例有什么本质区别 | 生命周期同长，但语义绑定 ServletContext——可感知容器生命周期事件 |
| 为什么父子容器不推荐在 Boot 用 | Boot 单上下文已够——双容器增加查找开销与配置歧义（WAR 部署才是主场景） |
| WebRequest 与 HttpServletRequest 什么关系 | WebRequest 是抽象接口，ServletRequestAttributes 是它的 Servlet 实现——面向抽象编程 |
| 作用域 Bean 的 destroyMethod 何时触发 | 请求结束/会话销毁时——可配 @PreDestroy 做清理（如释放资源） |
| 为什么说 session 作用域天然线程安全 | 会话内单实例，但同一会话的并发请求共享它——仍要小心可见性（通常请求级加锁） |

**生产实践**：

| 实践 | 要点 |
|------|------|
| 请求作用域 Bean 别滥用 | 每请求新建——只放"请求级临时状态"，不放可计算值 |
| session Bean 注意内存 | 会话存活期间不释放——大对象放 session 是内存泄漏源 |
| 上下文泄漏排查 | 容器未销毁（应用重启内存涨）→ 检查监听器注册与静态持有 |
| 测试 | MockHttpServletRequest + RequestContextHolder.setRequestAttributes 注入 |

### 5.2 与 MVC 联动的常见问题

| 问题 | 原因 | 解法 |
|------|------|------|
| 拦截器里拿不到 session 作用域 Bean | 拦截器在请求早期执行 | 用 request 作用域或直接操作 request 对象 |
| 视图渲染时 request Bean 已销毁 | 渲染阶段晚于响应提交 | 数据在控制器阶段取好放 model |
| 异步请求中作用域失效 | 异步线程无绑定 | TaskDecorator/显式传参（见 4.1） |
| 单测 NPE（RequestContextHolder） | 未注入请求属性 | 测试里 setRequestAttributes + reset |
| 多模块（Module）间上下文不共享 | 各模块独立容器 | 统一主容器管理，模块只做组件分组 |
| 监听器里拿 request 作用域 Bean | 监听器生命周期不属请求 | 直接操作 request/session 对象，别依赖作用域 Bean |
| 会话失效后 session Bean 仍被引用 | 单例持有代理的缓存 | 代理实时解析——会话过期后访问抛异常，按需降级处理 |
| request Bean 与虚拟线程 | 虚拟线程每请求一个——作用域正常 | 无特殊处理；但异步分支（同请求内换线程）需传递 |

> 🎯 **本模块一句话**：Web 上下文 = "容器层次 + 作用域 + 线程绑定"三件事——父子容器管 Bean 归属，作用域管生命周期，RequestContextHolder 管线程可见性；三者的边界就是面试的边界。

### 5.3 常见错误速查与记忆卡

| 报错/现象 | 根因 | 解法 |
|-----------|------|------|
| `IllegalStateException: No thread-bound request found` | 非请求线程访问请求上下文 | 判空 / TaskDecorator 传递（见 4.1） |
| `IllegalStateException: No Scope registered` | 作用域未激活/未注册 | 确认 Web 容器环境；测试手动注册 Scope |
| session Bean 属性串了 | 无代理注入（单例持有） | 加 proxyMode（见 3 节） |
| 内存持续增长 | session 存大对象/无销毁 | 会话瘦身 + 过期策略 |
| 上下文不销毁 | 静态持有容器/Bean | 用注入替代静态持有 |
| 父子 Bean 冲突不生效 | 以为覆盖会替换 | 子优先，父仍在——按需求拆名或删父 |

```text
一分钟记忆卡：
  容器：Boot 单上下文 / WAR 父子双上下文（子找父，父看不见子）
  作用域：request（每请求）/ session（每会话）/ application（全局）
  代理：跨作用域注入必配 proxyMode（TARGET_CLASS）
  线程：RequestContextHolder = ThreadLocal<RequestAttributes>
  异步：TaskDecorator 传递 / 显式传参 / 虚拟线程同线程可用
```

---

**下一模块**：[06-过滤链与 CORS 速查](06-过滤链与CORS速查.md)　**返回总览**：[00-Spring Web组件总览](00-Spring Web组件总览.md)
