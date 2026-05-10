03.21 00:06
SpringMVC的国际化和拦截器
一、SpringMVC国际化（i18n）
1.1 核心概念
国际化（Internationalization，简称i18n，取首末字母中间18个字母）是指程序能根据不同的语言环境（如中文、英文），自动切换显示对应的文本、提示信息等，无需修改代码，适配多语言场景（如中英文网站、多语言后台管理系统）。
SpringMVC的国际化依赖于Spring的MessageSource接口，核心是通过配置消息资源文件、语言解析器，实现前端视图与后端提示信息的多语言切换，底层采用“资源文件+语言标识”的机制。
1.2 实现步骤（完整可落地）
步骤1：创建国际化资源文件
在src/main/resources目录下，创建以“baseName_语言标识_国家标识.properties”命名的资源文件（baseName可自定义，推荐用messages），核心是保证不同语言文件的key一致、value对应不同语言。
示例资源文件（3个核心文件，覆盖中文、英文，默认中文）：
messages.properties：默认语言（中文，当未匹配到其他语言时使用） # 默认中文资源 user.login=登录 user.username=用户名 user.password=密码 user.submit=提交 tip.success=操作成功 tip.error=操作失败
messages_en_US.properties：英文资源 # 英文资源 user.login=Login user.username=Username user.password=Password user.submit=Submit tip.success=Operation Success tip.error=Operation Failed
messages_zh_CN.properties：中文资源（可与默认文件一致，也可单独配置） # 中文资源（与默认文件一致，可省略，也可扩展） user.login=登录 user.username=用户名 user.password=密码 user.submit=提交 tip.success=操作成功 tip.error=操作失败
注意：资源文件的key必须全局唯一，避免重复；中文value无需手动转义（IDEA会自动处理为Unicode编码，保证兼容性）。
步骤2：配置SpringMVC核心配置（xml方式）
在springmvc.xml中配置3个核心组件，实现国际化解析：
<!-- 1. 配置消息资源处理器（加载国际化资源文件） -->
<bean id="messageSource" class="org.springframework.context.support.ResourceBundleMessageSource">
    <!-- 指定资源文件的baseName（无需写后缀，Spring会自动拼接语言标识） -->
    <property name="basename" value="messages"/>
    <!-- 配置编码格式，避免中文乱码 -->
    <property name="defaultEncoding" value="UTF-8"/>
    <!-- 配置资源文件缓存时间（单位：秒），-1表示永久缓存，开发时可设为0避免缓存 -->
    <property name="cacheSeconds" value="0"/>
</bean>
<!-- 2. 配置语言解析器（决定使用哪种语言，支持请求参数、Cookie、Session三种方式） -->
<bean id="localeResolver" class="org.springframework.web.servlet.i18n.SessionLocaleResolver">
    <!-- 设置默认语言环境（中文-中国） -->
    <property name="defaultLocale" value="zh_CN"/>
</bean>
<!-- 3. 配置国际化拦截器（拦截请求，获取语言参数，切换语言环境） -->
<mvc:interceptors>
    <bean class="org.springframework.web.servlet.i18n.LocaleChangeInterceptor">
        <!-- 指定请求中切换语言的参数名（如?lang=en_US表示切换为英文，?lang=zh_CN切换为中文） -->
        <property name="paramName" value="lang"/>
    </bean>
</mvc:interceptors>
步骤3：前端视图中使用国际化文本
SpringMVC提供<spring:message>标签（需引入Spring标签库），在JSP页面中通过key获取对应语言的value，自动适配当前语言环境。
<%-- 引入Spring标签库 --%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<h3><spring:message code="user.login"/></h3>
<form action="${pageContext.request.contextPath}/login" method="post">
    <div>
        <label><spring:message code="user.username"/>:</label>
        <input type="text" name="username"/>
    </div>
    <div>
        <label><spring:message code="user.password"/>:</label>
        <input type="password" name="password"/>
    </div>
    <div>
        <input type="submit" value="<spring:message code="user.submit"/>"/>
    </div>
</form>
<%-- 语言切换链接（通过lang参数触发拦截器切换语言） --%>
<a href="${pageContext.request.contextPath}?lang=zh_CN">中文</a>
<a href="${pageContext.request.contextPath}?lang=en_US">English</a>
步骤4：后端Controller中使用国际化文本
后端可通过MessageSource接口的getMessage()方法，获取国际化提示信息（如接口返回的提示、异常信息）。
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.servlet.http.HttpServletRequest;
import java.util.Locale;
@Controller
public class LoginController {
    // 注入消息资源处理器
    @Autowired
    private MessageSource messageSource;
    @RequestMapping("/login")
    public String login(String username, String password, HttpServletRequest request) {
        // 模拟登录逻辑
        if ("admin".equals(username) && "123456".equals(password)) {
            // 获取当前语言环境（由LocaleResolver自动维护）
            Locale locale = LocaleContextHolder.getLocale();
            // 通过key获取国际化提示信息
            String successMsg = messageSource.getMessage("tip.success", null, locale);
            request.setAttribute("msg", successMsg);
            return "success";
        } else {
            Locale locale = LocaleContextHolder.getLocale();
            String errorMsg = messageSource.getMessage("tip.error", null, locale);
            request.setAttribute("msg", errorMsg);
            return "login";
        }
    }
}
1.3 常见语言解析器对比
SpringMVC提供3种常用语言解析器，根据场景选择：
解析器类
核心特点
适用场景
SessionLocaleResolver
语言环境存储在Session中，切换后仅当前会话有效，关闭浏览器失效
大多数Web项目（如后台管理系统）
CookieLocaleResolver
语言环境存储在Cookie中，可设置过期时间，持久化保存
需要记住用户语言偏好的场景（如门户网站）
AcceptHeaderLocaleResolver
无需配置拦截器，自动获取浏览器请求头Accept-Language的语言，无法手动切换
无需手动切换语言，完全依赖浏览器设置的场景
二、SpringMVC拦截器（Interceptor）
2.1 核心概念
SpringMVC拦截器是Spring提供的核心组件，用于拦截Controller的请求，在请求处理前、处理中、处理后执行自定义逻辑，相当于“过滤器”的增强版（与Servlet过滤器的区别：拦截器只拦截Controller请求，不拦截静态资源；过滤器基于Servlet规范，拦截所有请求）。
核心作用：权限校验（如登录校验）、日志记录、请求参数预处理、响应结果处理、异常统一处理等。
2.2 拦截器核心接口与执行流程
2.2.1 核心接口
自定义拦截器需实现HandlerInterceptor接口，重写3个核心方法（按需重写，无需全部实现）：
preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
执行时机：Controller方法执行之前
返回值：boolean类型，true表示放行（继续执行Controller），false表示拦截（终止请求，不执行后续逻辑）
核心用途：权限校验、请求参数校验、登录拦截等
postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView)
执行时机：Controller方法执行之后，视图渲染之前
核心用途：修改ModelAndView中的数据、设置视图参数等
afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
执行时机：视图渲染之后（整个请求完成后）
参数ex：请求过程中抛出的异常（可用于异常统一处理、资源清理）
核心用途：资源释放（如关闭流）、日志记录（记录请求耗时）等
2.2.2 执行流程（重点）
单个拦截器执行流程：
客户端发起请求 → 拦截器preHandle() → 放行 → Controller方法执行
Controller执行完成 → 拦截器postHandle() → 视图渲染
视图渲染完成 → 拦截器afterCompletion() → 响应给客户端
多个拦截器执行流程（按配置顺序）：
preHandle()：按配置顺序依次执行（拦截器1→拦截器2→...）
postHandle()：按配置顺序反向执行（拦截器n→...→拦截器2→拦截器1）
afterCompletion()：按配置顺序反向执行（仅preHandle()返回true的拦截器会执行）
2.3 自定义拦截器实现（登录拦截案例）
步骤1：自定义拦截器类
实现HandlerInterceptor接口，重写preHandle()方法，实现登录拦截（未登录用户跳转至登录页）。
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
/**
 * 登录拦截器：未登录用户禁止访问核心资源
 */
public class LoginInterceptor implements HandlerInterceptor {
    /**
     * 核心拦截逻辑：在Controller执行前校验登录状态
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取Session
        HttpSession session = request.getSession();
        // 2. 校验Session中是否有登录用户（假设登录成功后存入user对象）
        Object user = session.getAttribute("user");
        // 3. 判断：未登录则跳转至登录页，拦截请求；已登录则放行
        if (user == null) {
            // 重定向至登录页（避免转发导致的路径问题）
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return false; // 拦截请求
        }
        // 已登录，放行，继续执行Controller
        return true;
    }
    // 按需重写postHandle和afterCompletion
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 视图渲染前执行，可修改ModelAndView
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求完成后执行，可释放资源、记录日志
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
步骤2：配置拦截器（springmvc.xml）
通过<mvc:interceptors>配置拦截器，指定拦截路径和排除路径（如登录页、静态资源无需拦截）。
<!-- 配置拦截器 -->
<mvc:interceptors>
    <mvc:interceptor>
        <!-- 1. 指定拦截的路径（/**表示拦截所有请求） -->
        <mvc:mapping path="/**"/>
        <!-- 2. 指定排除的路径（无需拦截的资源，多个路径用逗号分隔） --&gt;
        &lt;mvc:exclude-mapping path="/login.jsp"/&gt; <!-- 登录页 -->
        &lt;mvc:exclude-mapping path="/login"/&gt; <!-- 登录接口 -->
        &lt;mvc:exclude-mapping path="/static/**"/&gt; <!-- 静态资源（css、js、img） -->
        <mvc:exclude-mapping path="/?lang=*"/><!-- 国际化语言切换请求 -->
        <!-- 3. 关联自定义拦截器类 -->
        <bean class="com.example.interceptor.LoginInterceptor"/>
    </mvc:interceptor>
</mvc:interceptors>
注意：<mvc:mapping path="/**">表示拦截所有请求，<mvc:exclude-mapping>用于排除无需拦截的路径，避免拦截登录页、静态资源等，否则会导致无法正常访问。
步骤3：测试拦截效果
未登录状态下，访问需拦截的路径（如/home），会被拦截并重定向至login.jsp；
登录成功后（将用户信息存入Session），访问/home，拦截器放行，正常执行Controller；
访问排除路径（如/login.jsp、/static/css/style.css），拦截器不生效，可正常访问。
2.4 拦截器与过滤器的区别（重点）
对比维度
SpringMVC拦截器（Interceptor）
Servlet过滤器（Filter）
底层规范
基于SpringMVC框架，属于Spring组件
基于Servlet规范，属于JavaWeb组件
拦截范围
仅拦截Controller的请求（.do、/路径等），不拦截静态资源
拦截所有请求（包括静态资源、Servlet、JSP等）
执行时机
在DispatcherServlet之后，Controller之前执行
在DispatcherServlet之前执行
依赖环境
依赖Spring容器，可注入SpringBean（如Service、Dao）
不依赖Spring容器，无法直接注入SpringBean
核心用途
权限校验、请求预处理、视图处理、异常处理
编码过滤、字符集设置、静态资源拦截、跨域处理
三、常见问题与避坑指南
3.1 国际化常见问题
中文乱码：未配置messageSource的defaultEncoding为UTF-8，或资源文件编码不是UTF-8（IDEA中需设置资源文件编码为UTF-8）。
语言切换无效：未配置LocaleChangeInterceptor，或paramName参数与请求参数不一致；若使用CookieLocaleResolver，需确保Cookie未过期。
资源文件加载失败：basename配置错误（如路径写错、文件名不规范），需确保资源文件在resources根目录下，且baseName与文件名前缀一致。
3.2 拦截器常见问题
拦截器不生效：未在springmvc.xml中配置拦截器，或mapping路径配置错误（如写成/*，无法拦截多级路径）；自定义拦截器未实现HandlerInterceptor接口。
静态资源被拦截：未配置<mvc:exclude-mapping path="/static/**"/>，需排除静态资源路径；同时确保SpringMVC配置了<mvc:default-servlet-handler/>（处理静态资源）。
拦截器无法注入Service/Dao：拦截器未被Spring容器管理（需通过<bean>标签配置，而非手动new），导致依赖注入失败。
多个拦截器执行顺序异常：拦截器的执行顺序由springmvc.xml中<mvc:interceptor>的配置顺序决定，preHandle按配置顺序执行，postHandle和afterCompletion反向执行。
四、扩展说明
1. 国际化扩展：可结合数据库存储国际化资源，自定义MessageSource实现类，从数据库读取key-value，实现动态修改多语言文本（无需重启服务）。
2. 拦截器扩展：可实现HandlerInterceptorAdapter抽象类（已过时，推荐直接实现HandlerInterceptor），或使用@Configuration注解（注解方式配置拦截器，适用于SpringMVC+Spring Boot整合场景）。
3. 实际开发场景：国际化常用于多语言网站、后台管理系统；拦截器常用于登录校验、接口权限控制、日志记录（如记录请求URL、请求参数、响应时间）。

