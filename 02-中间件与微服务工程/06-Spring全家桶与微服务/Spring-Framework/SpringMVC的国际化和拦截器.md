# SpringMVC 国际化（i18n）和拦截器

> **定位**：国际化 = 多语言自由切换；拦截器 = 请求前后自定义处理（权限/日志/参数预处理）。

---

## 目录

1. [国际化（i18n）](#1-国际化i18n)
2. [拦截器（Interceptor）](#2-拦截器interceptor)

---

## 1. 国际化（i18n）

### 1.1 核心原理

```text
资源文件（.properties） + MessageSource + LocaleResolver + LocaleChangeInterceptor
```

### 1.2 实现步骤

**Step 1：创建资源文件**

```properties
# messages.properties（默认中文）
user.login=登录
user.username=用户名

# messages_en_US.properties（英文）
user.login=Login
user.username=Username
```

**Step 2：SpringMVC 配置**

```xml
<!-- 消息资源处理器 -->
<bean id="messageSource" class="org.springframework.context.support.ResourceBundleMessageSource">
    <property name="basename" value="messages"/>
    <property name="defaultEncoding" value="UTF-8"/>
</bean>

<!-- 语言解析器（Session 级别） -->
<bean id="localeResolver" class="org.springframework.web.servlet.i18n.SessionLocaleResolver">
    <property name="defaultLocale" value="zh_CN"/>
</bean>

<!-- 语言切换拦截器 -->
<mvc:interceptors>
    <bean class="org.springframework.web.servlet.i18n.LocaleChangeInterceptor">
        <property name="paramName" value="lang"/>
    </bean>
</mvc:interceptors>
```

**Step 3：JSP 中使用**

```jsp
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:message code="user.login"/>
<!-- 语言切换 -->
<a href="?lang=zh_CN">中文</a> | <a href="?lang=en_US">English</a>
```

**Step 4：Controller 中使用**

```java
@Autowired
private MessageSource messageSource;

Locale locale = LocaleContextHolder.getLocale();
String msg = messageSource.getMessage("tip.success", null, locale);
```

### 1.3 三种语言解析器

| 解析器 | 存储位置 | 特点 |
|--------|----------|------|
| `SessionLocaleResolver` | Session | 仅当前会话有效 |
| `CookieLocaleResolver` | Cookie | 可持久化保存 |
| `AcceptHeaderLocaleResolver` | 请求头 | 自动获取浏览器语言，无法手动切换 |

---

## 2. 拦截器（Interceptor）

### 2.1 三个核心方法

| 方法 | 时机 | 返回值 |
|------|------|:---:|
| `preHandle()` | Controller 前 | `true`=放行 / `false`=拦截 |
| `postHandle()` | Controller 后、视图渲染前 | — |
| `afterCompletion()` | 视图渲染完成 | — |

### 2.2 执行流程

```text
单个：preHandle → Controller → postHandle → 视图渲染 → afterCompletion
多个：preHandle 顺序执行 → postHandle/afterCompletion 逆序执行
```

### 2.3 登录拦截器

```java
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response, Object handler) throws Exception {
        Object user = request.getSession().getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return false;
        }
        return true;
    }
}
```

```xml
<mvc:interceptors>
    <mvc:interceptor>
        <mvc:mapping path="/**"/>
        <mvc:exclude-mapping path="/login.jsp"/>
        <mvc:exclude-mapping path="/login"/>
        <mvc:exclude-mapping path="/static/**"/>
        <bean class="com.example.interceptor.LoginInterceptor"/>
    </mvc:interceptor>
</mvc:interceptors>
```

### 2.4 Interceptor vs Filter

| 维度 | Interceptor | Filter |
|------|-----------|--------|
| 规范 | SpringMVC 组件 | Servlet 规范 |
| 拦截范围 | 仅 Controller | 所有请求（含静态资源） |
| 执行时机 | DispatcherServlet 之后 | DispatcherServlet 之前 |
| 依赖注入 | ✅ 可注入 Spring Bean | ❌ 无法注入 |

---

## 3. 常见问题

| 问题 | 方案 |
|------|------|
| 中文乱码 | `defaultEncoding=UTF-8` + IDEA 资源文件设为 UTF-8 |
| 语言切换无效 | 检查 `paramName` 与请求参数一致 + Cookie 未过期 |
| 拦截器不生效 | 检查 mapping 路径（`/**` 不是 `/*`）+ 实现 `HandlerInterceptor` |
| 静态资源被拦截 | 排除 `/static/**` + 配置 `default-servlet-handler` |
