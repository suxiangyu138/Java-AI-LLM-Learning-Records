# SpringMVC 实战项目清单

## 一、核心能力

SpringMVC 是 Java Web 开发核心框架，必须掌握：

- 请求映射与参数绑定
- MVC 三层架构
- 拦截器与全局异常处理
- 文件上传下载
- JSON 数据交互
- Session 与 Cookie 管理

---

## 二、技术能力拆解

- 请求层：@RequestMapping / @PostMapping / @GetMapping
- 参数层：@RequestParam / @ModelAttribute / @PathVariable
- 响应层：@ResponseBody / ModelAndView / 转发与重定向
- 架构层：Controller / Service / Dao 三层分离
- 拦截层：HandlerInterceptor / 登录鉴权 / 白名单
- 异常层：@ControllerAdvice / @ExceptionHandler
- 文件层：MultipartFile / 上传 / 下载 / 校验
- 数据层：JdbcTemplate / MyBatis / 事务管理
- 整合层：SSM（Spring + SpringMVC + MyBatis）

---

## 三、必做项目（求职优先级）

### 1. 用户登录注册系统
**定位**：入门必做，掌握基础流程

**技术栈**：SpringMVC + MySQL

**实现**：
- 用户注册
- 用户登录
- 表单参数绑定
- Session 会话管理
- 页面跳转
- 转发与重定向

**核心知识点**：
- @RequestMapping / @PostMapping / @GetMapping
- @RequestParam / @ModelAttribute
- HttpServletRequest / HttpServletResponse
- Session 管理
- 转发与重定向

**产出**：
- SpringMVC 基础能力

**耗时**：2天

---

### 2. 简单 CRUD 员工管理系统
**技术栈**：SpringMVC + MyBatis / JdbcTemplate + MySQL

**实现**：
- 员工列表展示
- 员工新增
- 员工编辑
- 员工删除
- 分页查询
- 条件筛选

**核心知识点**：
- MVC 三层架构：Controller → Service → Dao
- JdbcTemplate / MyBatis 数据交互
- 列表分页
- 条件查询

**产出**：
- CRUD 与三层架构能力

**耗时**：3天

---

### 3. 全局异常处理 + 统一返回体
**技术栈**：SpringMVC

**实现**：
- 全局异常拦截
- 自定义业务异常
- 统一 JSON 返回格式
- 异常分类处理

**核心知识点**：
- @ControllerAdvice
- @ExceptionHandler
- 统一返回体设计

**产出**：
- 统一异常处理能力

**耗时**：1~2天

---

### 4. 拦截器实现登录鉴权
**技术栈**：SpringMVC

**实现**：
- HandlerInterceptor
- 登录状态校验
- 未登录拦截
- 放行白名单配置
- 自动跳转登录页

**核心知识点**：
- HandlerInterceptor
- 前置拦截
- 后置处理
- 白名单配置

**产出**：
- 登录鉴权拦截器

**耗时**：1~2天

---

### 5. 文件上传与下载
**技术栈**：SpringMVC

**实现**：
- 文件上传
- 文件类型校验
- 文件大小限制
- 服务器文件保存
- 文件下载
- 文件预览

**核心知识点**：
- MultipartFile 文件上传
- 文件校验
- 文件下载流
- 文件存储

**产出**：
- 文件处理能力

**耗时**：2天

---

### 6. JSON 数据交互项目
**技术栈**：SpringMVC + Jackson

**实现**：
- 前后端分离 JSON 交互
- 接口返回 JSON 数据
- 接收 JSON 请求
- 日期格式化

**核心知识点**：
- @ResponseBody
- @RequestBody
- Jackson 序列化
- 日期格式化

**产出**：
- 前后端分离接口能力

**耗时**：1~2天

---

### 7. SSM 整合博客系统
**定位**：综合实战，企业级规范

**技术栈**：Spring + SpringMVC + MyBatis

**实现**：
- 用户登录注册
- 文章发布 / 编辑 / 删除
- 评论功能
- 分类管理
- 标签管理
- 权限控制
- 多表联查
- 分页查询

**核心知识点**：
- SSM 完整整合
- 动态 SQL
- 多表联查
- 分页插件
- 事务管理

**产出**：
- SSM 完整项目能力

**耗时**：7~10天

---

## 四、进阶项目（提升上限）

### 8. 完整后台权限管理系统
**技术栈**：SpringMVC + MyBatis

**实现**：
- 用户管理
- 角色管理
- 权限管理
- 菜单管理
- 多角色 / 多权限控制
- 拦截器 + 角色校验
- 菜单动态渲染

**产出**：
- 企业级权限管理能力

---

### 9. 商品购物车系统
**技术栈**：SpringMVC + MyBatis

**实现**：
- 商品列表
- 加入购物车
- 购物车展示
- 修改数量
- 删除商品
- 订单结算
- 订单生成

**产出**：
- 购物车与订单处理能力

---

## 五、必练核心知识点

### 1. 请求映射
```java
@Controller
@RequestMapping("/user")
public class UserController {
    
    @GetMapping("/login")
    public String login() {
        return "login";
    }
    
    @PostMapping("/doLogin")
    public String doLogin(@RequestParam String username, 
                          @RequestParam String password) {
        // 登录逻辑
        return "redirect:/index";
    }
}
```

### 2. 参数绑定
```java
@PostMapping("/save")
public String save(@ModelAttribute User user) {
    // 保存用户
    return "success";
}
```

### 3. JSON 响应
```java
@ResponseBody
@GetMapping("/list")
public Result list() {
    List<User> users = userService.list();
    return Result.success(users);
}
```

### 4. 拦截器
```java
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, 
                             HttpServletResponse response, 
                             Object handler) {
        HttpSession session = request.getSession();
        if (session.getAttribute("user") == null) {
            response.sendRedirect("/login");
            return false;
        }
        return true;
    }
}
```

### 5. 全局异常处理
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result handleException(Exception e) {
        return Result.error(e.getMessage());
    }
}
```

---

## 六、项目架构标准

必须做到：

- 代码规范分层：Controller / Service / Dao / Entity
- 配置文件清晰：web.xml / springmvc.xml / applicationContext.xml
- 全部代码上传 GitHub
- 写标准 README.md
- 严格处理异常 / 参数校验 / 编码乱码问题

---

## 七、简历表达（核心关键词）

- SpringMVC 请求映射与参数绑定
- MVC 三层架构设计
- HandlerInterceptor 登录鉴权
- @ControllerAdvice 全局异常处理
- MultipartFile 文件上传下载
- 前后端分离 JSON 数据交互
- SSM 框架整合（Spring + SpringMVC + MyBatis）

---

## 八、技术栈推荐

- 框架：SpringMVC
- 持久层：MyBatis / JdbcTemplate
- 数据库：MySQL
- 视图：JSP / Thymeleaf
- JSON：Jackson / Gson
- 服务器：Tomcat

---

## 九、你的优先级

结合你的 **Java 后端 + AI 大模型应用** 路线，最值得优先完成的是：

1. 用户登录注册系统
2. 全局异常 + 统一返回体
3. 拦截器登录鉴权
4. 文件上传下载
5. SSM 整合博客系统

---

## 十、练手路线

### 第一阶段：基础流程
先做：
- 用户登录注册系统
- 简单 CRUD 员工管理系统

### 第二阶段：核心功能
再做：
- 全局异常处理 + 统一返回体
- 拦截器实现登录鉴权
- 文件上传与下载
- JSON 数据交互项目

### 第三阶段：综合实战
最后做：
- SSM 整合博客系统
- 完整后台权限管理系统

---

## 十一、项目成果要求

- GitHub 完整代码（结构清晰）
- README 写明：项目背景、技术栈、功能模块、部署步骤
- 项目必须体现：三层架构、异常处理、参数校验、拦截器
- 至少 1 个完整业务 Demo
- 能清晰展示 SpringMVC 核心能力

---
