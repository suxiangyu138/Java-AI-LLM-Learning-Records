# 快速学会 SpringMVC（极简核心 + 实战）
## 一、核心定位
**SpringMVC 是 Spring 生态的 Web 层框架**，基于 MVC 设计模式，用来：
- 接收前端请求
- 接收参数、封装参数
- 调用 Service 业务层
- 封装数据、响应前端
- 统一异常、文件上传、拦截器

底层基于 **Servlet**，是 SpringBoot Web 的底层核心。

---

## 二、MVC 分工
- **M（Model）**：业务数据、实体类、Service、Mapper
- **V（View）**：视图（前后端分离项目基本不用）
- **C（Controller）**：控制器，接收请求、调度业务、返回数据

---

## 三、SpringMVC 完整执行流程（必背）
1. 前端发请求 → **DispatcherServlet（核心中央调度器）**
2. 处理器映射器：根据 URL 找到对应 Controller 方法
3. 处理器适配器：执行 Controller 方法
4. 参数绑定、类型转换、注解解析
5. 调用业务逻辑
6. 封装返回结果
7. 视图解析 / 直接 JSON 返回
8. 响应数据给前端

> 核心大佬：**DispatcherServlet**，所有请求统一入口。

---

## 四、SpringBoot 环境下快速使用（最常用）
### 1. 依赖
引入 `web` 启动器，**自动整合 SpringMVC**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

### 2. 基础控制器
```java
@RestController
@RequestMapping("/user")
public class UserController {

    @GetMapping("/list")
    public String list(){
        return "用户列表";
    }
}
```

---

## 五、五大核心功能（高频实战）
### 1. 常用请求注解
```java
// 类上统一前缀
@RequestMapping("/user")

// 限定 GET / POST
@GetMapping
@PostMapping
@PutMapping
@DeleteMapping
```

### 2. 四种参数接收方式
#### ① 普通参数
```java
@GetMapping("/get")
public String get(Integer id,String name){
    return id + ":" + name;
}
```

#### ② 路径变量 `PathVariable`
```java
@GetMapping("/detail/{id}")
public String detail(@PathVariable Integer id){
    return "id：" + id;
}
```

#### ③ JSON 参数 `@RequestBody`
```java
@PostMapping("/add")
public String add(@RequestBody User user){
    return user.getName();
}
```

#### ④ 表单参数 / 上传
直接实体类接收，自动封装。

---

### 3. 统一返回 JSON
- `@RestController`
  = `@Controller` + `@ResponseBody`
- 自动使用 JackSon 序列化，对象直接转 JSON。

---

### 4. 全局乱码、日期格式化
SpringBoot 自动配置，无需手动写 SpringMVC 旧版乱码过滤器。

---

### 5. 拦截器 Interceptor
**拦截 Controller 请求，做登录校验、权限、日志**
1. 自定义拦截器
```java
@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler){
        // 前置拦截
        return true; // true放行，false拦截
    }
}
```
2. 注册拦截器
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Resource
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login");
    }
}
```

---

## 六、核心组件
1. **DispatcherServlet**：中央调度器，请求入口
2. **HandlerMapping**：映射 URL 与控制器
3. **HandlerAdapter**：适配执行控制器方法
4. **ViewResolver**：视图解析器（前后端分离弱化）
5. **Interceptor**：拦截器
6. **ExceptionHandler**：全局异常处理

---

## 七、全局统一异常处理（企业必备）
```java
@RestControllerAdvice
public class GlobalException {

    @ExceptionHandler(Exception.class)
    public String error(Exception e){
        e.printStackTrace();
        return "系统异常";
    }
}
```

---

## 八、SpringMVC 关键区别
1. **SpringMVC**：只负责 Web 请求层
2. **Spring Framework**：底层 IOC、AOP、事务
3. **SpringBoot**：整合 + 自动配置 + 内嵌容器

**三者关系**：
`SpringBoot` → 内置 `SpringMVC` + `SpringFramework`

---

## 九、最简总结
1. SpringMVC 负责**接口请求、参数接收、数据响应**
2. 核心入口：`DispatcherServlet`
3. 开发核心：`@RestController` + 各类请求映射
4. 常用能力：参数绑定、JSON 响应、拦截器、全局异常
5. 完全融入 SpringBoot，开箱即用

