# 06 - JavaWeb MVC 与请求处理流程

> 从理论到实践：MVC 模式在 Java Web 中的落地、请求的完整处理链路、三层架构的分工以及项目结构的最佳实践——将这些串联起来，你对后端架构的理解将提升一个层次。

---

## 目录

1. [MVC 模式概述](#1-mvc-模式概述)
2. [JavaWeb MVC 的实现方式](#2-javaweb-mvc-的实现方式)
3. [一次请求的完整处理流程](#3-一次请求的完整处理流程)
4. [Forward 与 Redirect 深度辨析](#4-forward-与-redirect-深度辨析)
5. [三层架构：Controller-Service-DAO](#5-三层架构controllerservicedao)
6. [经典项目结构与包命名](#6-经典项目结构与包命名)
7. [请求参数封装与校验](#7-请求参数封装与校验)
8. [统一结果返回与异常处理](#8-统一结果返回与异常处理)
9. [手写一个 Mini MVC 框架](#9-手写一个-mini-mvc-框架)
10. [常见面试题](#10-常见面试题)

---

## 1. MVC 模式概述

### 1.1 MVC 三个角色

```
┌─────────────────────────────────────────────────────┐
│                        MVC 模式                       │
│                                                      │
│   ┌──────────┐    查询状态    ┌──────────────┐        │
│   │  View    │ <─────────── │  Controller   │        │
│   │ (视图)   │               │  (控制器)      │        │
│   │ JSP/HTML │ ───────────> │  Servlet      │        │
│   └──────────┘   用户操作    └──────┬───────┘        │
│        │                           │                 │
│        │ 获取数据                   │ 改数据           │
│        ▼                           ▼                 │
│   ┌──────────────────────────────────┐               │
│   │           Model (模型)            │               │
│   │      JavaBean / POJO / Service    │               │
│   └──────────────────────────────────┘               │
└─────────────────────────────────────────────────────┘
```

| 角色 | 职责 | JavaWeb 对应 | 禁止事项 |
|------|------|-------------|---------|
| **Model** | 封装数据 + 业务逻辑 | Service / DAO / POJO | ❌ 不能操作 Request/Response |
| **View** | 展示数据、接收输入 | JSP / HTML / Thymeleaf | ❌ 不包含业务逻辑 |
| **Controller** | 协调 M 和 V、控制流程 | Servlet / Filter | ❌ 不直接操作数据库 |

### 1.2 MVC 的核心价值

```
没有 MVC 时（JSP Model 1）：
  JSP = Controller + View + Model → 混乱、难以维护

有了 MVC 后（JSP Model 2）：
  Servlet = Controller → 专注流程控制
  JSP = View → 专注页面渲染
  Service/DAO = Model → 专注业务/数据

→ 职责单一、可测试、可维护
```

---

## 2. JavaWeb MVC 的实现方式

### 2.1 原生 Servlet 实现

```java
// Controller — 一个请求一个 Servlet（传统方式，繁琐）
@WebServlet("/user/list")
public class UserListServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 1. 调用 Service
        UserService userService = new UserService();
        List<User> users = userService.findAll();

        // 2. 将数据放入 Request
        req.setAttribute("users", users);

        // 3. 转发到 View
        req.getRequestDispatcher("/WEB-INF/views/userList.jsp").forward(req, resp);
    }
}
```

### 2.2 BaseServlet 分发模式（精简版）

```java
// 一个模块一个 Servlet，通过方法名分发
@WebServlet("/user/*")
public class UserController extends BaseServlet {
    // /user/list → list()  /user/add → add()

    public void list(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        List<User> users = userService.findAll();
        req.setAttribute("users", users);
        req.getRequestDispatcher("/WEB-INF/views/userList.jsp").forward(req, resp);
    }

    public void add(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String name = req.getParameter("name");
        userService.add(new User(name));
        resp.sendRedirect(req.getContextPath() + "/user/list");
    }
}

// BaseServlet 核心逻辑
public class BaseServlet extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 1. 从 URI 中提取方法名：/user/list → list
        String uri = req.getRequestURI();
        String methodName = uri.substring(uri.lastIndexOf("/") + 1);

        // 2. 反射调用对应方法
        try {
            Method method = this.getClass().getMethod(
                methodName, HttpServletRequest.class, HttpServletResponse.class);
            method.invoke(this, req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
```

> 💡 BaseServlet 模式是**手写"山寨 Spring MVC"的雏形**——方法名映射到 Controller 方法，Spring MVC 的 `@RequestMapping` 正是这个思路的标准化实现。

### 2.3 Spring MVC 对比

```java
// 同样的逻辑，Spring MVC 中：
@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute("users", userService.findAll());
        return "userList";  // 视图名，由 ViewResolver 解析
    }

    @PostMapping("/add")
    public String add(@RequestParam String name) {
        userService.add(new User(name));
        return "redirect:/user/list";
    }
}
```

| 对比点 | 原生 Servlet | Spring MVC |
|--------|-------------|------------|
| 方法映射 | 手动反射分发 | `@GetMapping` 注解 |
| 参数绑定 | 手动 `getParameter` | `@RequestParam` 自动绑定 |
| 视图渲染 | 手动 `forward()` | ViewResolver 自动解析 |
| 参数校验 | 手动 if-else | `@Valid` + Validator |
| 依赖注入 | `new` 手动创建 | `@Autowired` 自动装配 |
| 统一异常处理 | 各自 try-catch | `@ControllerAdvice` |

---

## 3. 一次请求的完整处理流程

### 3.1 完整链路

```
01. 浏览器输入 URL → DNS 解析 → TCP 连接
02. HTTP 请求到达 Tomcat Acceptor
03. Connector 解析 HTTP 协议 → 封装 HttpServletRequest/Response
04. Engine → Host → Context（按域名和上下文路径匹配）
05. Filter Chain 执行（前置处理：编码、权限、日志）
06. Wrapper → 匹配到目标 Servlet
07. HttpServlet.service() → doGet/doPost
      │
      ▼  ┌──────────────────────────────────┐
08.      │ Controller (Servlet)               │
         │  ├─ 接收参数：req.getParameter()    │
         │  ├─ 参数校验                        │
         │  ├─ 调用 Service                    │
         │  │   ├─ 业务逻辑处理                  │
         │  │   └─ 调用 DAO                    │
         │  │       └─ JDBC → 数据库            │
         │  ├─ 将结果放入 Request Attribute      │
         │  └─ Forward 到 JSP                  │
         └──────────────────────────────────┘
      │
      ▼
09. JSP 渲染：读取 Request Attribute → 生成 HTML
10. Filter Chain 执行（后置处理：压缩、日志）
11. Connector 将响应写回 Socket
12. 浏览器接收 HTML → 渲染页面
```

### 3.2 请求生命周期中各组件的作用

```
Request 到达 ──────────────────────────────────────────────> Response 返回

Filter      ┌────前置处理────┐              ┌────后置处理────┐
            │ 编码/权限/日志  │              │ 压缩/统计/清理  │
            └────────────────┘              └────────────────┘

Servlet                   ┌──Controller──┐
                          │ 参数接收与校验  │
                          │ 流程控制       │
                          └──────┬────────┘
                                 │
                          ┌──────▼────────┐
                          │   Service     │
                          │ 业务逻辑/事务   │
                          └──────┬────────┘
                                 │
                          ┌──────▼────────┐
                          │     DAO       │
                          │ 数据访问/JDBC  │
                          └──────┬────────┘
                                 │
                          ┌──────▼────────┐
                          │   数据库       │
                          └───────────────┘

JSP                                       ┌──视图渲染──┐
                                          │ 数据 + HTML │
                                          │ → 页面输出  │
                                          └────────────┘
```

---

## 4. Forward 与 Redirect 深度辨析

（注：核心对比已在 01-Servlet 讲义第7节覆盖，本节聚焦实战选择与坑点。）

### 4.1 实战决策树

```
需要传递复杂对象（非 String）？
  ├── 是 → Forward（通过 Request Attribute）
  └── 否 → 继续

需要 URL 变为目标地址？
  ├── 是 → Redirect（如登录后跳转首页）
  └── 否 → Forward

需要防止表单重复提交？
  ├── 是 → Redirect（PRG 模式）
  └── 否 → 任意

目标地址在外部（其他域名）？
  ├── 是 → Redirect
  └── 否 → 任意

目标页面在 WEB-INF 下（受保护）？
  ├── 是 → Forward（WEB-INF 下的资源只能通过转发访问）
  └── 否 → 任意
```

### 4.2 常见坑点

```java
// 坑1：Forward 之后还在写响应
req.getRequestDispatcher("/result.jsp").forward(req, resp);
resp.getWriter().write("这段代码不会执行吗？");
// forward 后不抛异常 → 代码看起来还在执行，但实际输出已被清空或被忽略
// → 最佳实践：forward 之后立即 return

// 坑2：Redirect 之前已提交响应
resp.getWriter().write("some content");
resp.getWriter().flush();  // 已提交！
resp.sendRedirect("/other");  // ❌ IllegalStateException: 响应已提交

// 坑3：Redirect 路径丢失 Context Path
resp.sendRedirect("/list");      // → http://host/list（可能不对）
resp.sendRedirect(req.getContextPath() + "/list");  // → http://host/app/list（正确）
// 或用 JSTL：<c:redirect url="/list"/> 自动添加 Context Path

// 坑4：Forward 的路径以 / 开头是相对于 Context Root
req.getRequestDispatcher("/WEB-INF/list.jsp").forward(req, resp);
// 不带 / 则是相对于当前 Servlet 的映射路径

// 坑5：WEB-INF 目录下的资源只能通过 Forward 访问
// 浏览器直接访问 http://host/app/WEB-INF/list.jsp → 404
// 只能通过 forward 跳转
```

### 4.3 POST-Redirect-GET 完整示例

```java
@WebServlet("/order/create")
public class OrderCreateServlet extends HttpServlet {

    // 显示表单（GET）
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/orderForm.jsp").forward(req, resp);
    }

    // 处理表单提交（POST）
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        // 1. 处理订单
        String productId = req.getParameter("productId");
        int quantity = Integer.parseInt(req.getParameter("quantity"));
        Order order = orderService.create(productId, quantity);

        // 2. PRG：重定向到结果页（GET 方法）
        resp.sendRedirect(req.getContextPath()
            + "/order/result?id=" + order.getId());
        // → 用户刷新时只会重新 GET /order/result，不会重复创建订单
    }
}
```

---

## 5. 三层架构：Controller-Service-DAO

### 5.1 架构层次图

```
┌──────────────────────────────────────────────────────┐
│  表现层 (Presentation / Web)                           │
│  ┌────────────┐  ┌──────────┐  ┌──────────────────┐  │
│  │  Filter    │  │ Servlet  │  │ JSP / Thymeleaf  │  │
│  │  拦截/过滤  │  │ 请求分发  │  │ 页面渲染          │  │
│  └────────────┘  └────┬─────┘  └──────────────────┘  │
├───────────────────────┼──────────────────────────────┤
│  业务层 (Business / Service)  │                        │
│  ┌──────────────────────────▼──────────────────────┐  │
│  │  Service (接口) + ServiceImpl (实现)              │  │
│  │  - 业务逻辑编排                                    │  │
│  │  - 事务管理                                        │  │
│  │  - 调用多个 DAO 组合数据                            │  │
│  └──────────────────────┬──────────────────────────┘  │
├─────────────────────────┼─────────────────────────────┤
│  持久层 (Persistence / DAO)  │                          │
│  ┌────────────────────────▼─────────────────────────┐ │
│  │  DAO (接口) + DAOImpl (实现)                       │ │
│  │  - SQL 执行                                        │ │
│  │  - 结果集映射到实体                                  │ │
│  │  - 连接管理                                        │ │
│  └──────────────────────────────────────────────────┘ │
├───────────────────────────────────────────────────────┤
│  数据层 (Database)                                      │
│  ┌──────────────────────────────────────────────────┐ │
│  │  MySQL / PostgreSQL / Oracle                       │ │
│  └──────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────┘
```

### 5.2 各层职责与调用规则

```java
// ═══ 实体层 (domain/entity) — 贯穿所有层 ═══
public class User {
    private Long id;
    private String username;
    private String email;
    // getter / setter
}

// ═══ 表现层 (controller) — 只调用 Service ═══
@WebServlet("/user/*")
public class UserController extends BaseServlet {
    private UserService userService = new UserServiceImpl();

    public void list(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        List<User> users = userService.findAll();  // 只调 Service
        req.setAttribute("users", users);
        req.getRequestDispatcher("/WEB-INF/userList.jsp").forward(req, resp);
    }
    // ❌ Controller 中不应该出现 JDBC 代码、SQL 语句
}

// ═══ 业务层 (service) — 调用 DAO，编排业务 ═══
public interface UserService {
    List<User> findAll();
    User findById(Long id);
    void register(User user);
}

public class UserServiceImpl implements UserService {
    private UserDao userDao = new UserDaoImpl();

    @Override
    public void register(User user) {
        // 业务逻辑：校验 → 加密密码 → 保存 → 发送邮件
        if (userDao.findByUsername(user.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        user.setPassword(MD5Util.encode(user.getPassword()));
        userDao.insert(user);
        EmailUtil.sendWelcome(user.getEmail());
    }
    // ✅ Service 可以调用多个 DAO；❌ Service 不应直接操作用户输入/响应
}

// ═══ 持久层 (dao) — 只做数据存取 ═══
public class UserDaoImpl implements UserDao {

    @Override
    public List<User> findAll() {
        String sql = "SELECT id, username, email FROM users ORDER BY id";
        // JDBC 执行 → ResultSet → List<User>
        return jdbcTemplate.query(sql, new UserRowMapper());
    }
    // ✅ DAO 只做 CRUD；❌ DAO 不应包含业务逻辑
}
```

### 5.3 调用规则总结

| 调用方向 | 允许？ | 说明 |
|---------|--------|------|
| Controller → Service | ✅ | 跨层调用，依赖接口 |
| Service → DAO | ✅ | 跨层调用，依赖接口 |
| Controller → DAO | ❌ | 跨两层，违反分层 |
| Service → Controller | ❌ | 上层不应被下层依赖 |
| DAO → Service | ❌ | 同上 |
| Service → Service | ✅ | 同层调用（组合复用） |
| DAO → DAO | ✅ | 同层调用 |

---

## 6. 经典项目结构与包命名

### 6.1 按技术分层（传统项目）

```
com.example.myapp
├── web/controller/     # 表现层
│   ├── UserController.java
│   └── OrderController.java
├── web/filter/         # 过滤器
│   ├── AuthFilter.java
│   └── CharsetFilter.java
├── web/listener/       # 监听器
│   └── AppStartupListener.java
├── service/            # 业务层（接口）
│   ├── UserService.java
│   └── OrderService.java
├── service/impl/       # 业务层（实现）
│   ├── UserServiceImpl.java
│   └── OrderServiceImpl.java
├── dao/                # 持久层（接口）
│   ├── UserDao.java
│   └── OrderDao.java
├── dao/impl/           # 持久层（实现）
│   ├── UserDaoImpl.java
│   └── OrderDaoImpl.java
├── domain/entity/      # 实体类
│   ├── User.java
│   └── Order.java
├── dto/                # 数据传输对象
│   ├── UserDTO.java
│   └── LoginRequest.java
├── util/               # 工具类
│   ├── MD5Util.java
│   └── JdbcUtil.java
└── exception/          # 自定义异常
    └── BusinessException.java
```

### 6.2 按业务模块（DDD 风格，大型项目）

```
com.example.myapp
├── user/
│   ├── controller/UserController.java
│   ├── service/UserService.java
│   ├── dao/UserDao.java
│   └── domain/User.java
├── order/
│   ├── controller/OrderController.java
│   ├── service/OrderService.java
│   ├── dao/OrderDao.java
│   └── domain/Order.java
├── common/              # 公共组件
│   ├── filter/
│   ├── util/
│   └── exception/
└── config/              # 配置
    └── AppConfig.java
```

### 6.3 WEB-INF 目录规范

```
webapp/
├── WEB-INF/
│   ├── web.xml              # 部署描述符
│   ├── lib/                 # 项目专属 jar 包
│   ├── classes/             # 编译后的 .class 文件
│   ├── views/               # JSP 页面（受保护，不可直接访问）
│   │   ├── user/
│   │   │   ├── list.jsp
│   │   │   └── detail.jsp
│   │   └── common/
│   │       ├── header.jsp
│   │       └── footer.jsp
│   └── tld/                 # 自定义标签库描述文件
├── static/                  # 静态资源（可直接访问）
│   ├── css/
│   ├── js/
│   └── images/
└── index.jsp                # 欢迎页面
```

> 💡 `WEB-INF` 下的资源浏览器无法直接访问（Tomcat 限制），只能通过 `forward` 访问——这是保护 JSP 不被直接请求的安全机制。

---

## 7. 请求参数封装与校验

### 7.1 手动封装（原生方式）

```java
// 工具类：将请求参数注入到 Bean
public class BeanUtils {
    public static <T> T populate(Class<T> clazz, HttpServletRequest req) {
        try {
            T bean = clazz.getDeclaredConstructor().newInstance();
            // 获取所有字段，匹配请求参数
            for (Field field : clazz.getDeclaredFields()) {
                String value = req.getParameter(field.getName());
                if (value != null && !value.isEmpty()) {
                    field.setAccessible(true);
                    // 类型转换
                    Object converted = convert(value, field.getType());
                    field.set(bean, converted);
                }
            }
            return bean;
        } catch (Exception e) {
            throw new RuntimeException("参数封装失败", e);
        }
    }

    private static Object convert(String value, Class<?> type) {
        if (type == String.class) return value;
        if (type == Integer.class || type == int.class) return Integer.valueOf(value);
        if (type == Long.class || type == long.class) return Long.valueOf(value);
        if (type == Double.class || type == double.class) return Double.valueOf(value);
        if (type == Boolean.class || type == boolean.class) return Boolean.valueOf(value);
        if (type == BigDecimal.class) return new BigDecimal(value);
        // 日期等其他类型...
        return value;
    }
}

// Controller 中使用
User user = BeanUtils.populate(User.class, req);
```

### 7.2 Apache Commons BeanUtils（生产级）

```java
// 比手写更健壮，自动处理类型转换和嵌套属性
import org.apache.commons.beanutils.BeanUtils;

User user = new User();
BeanUtils.populate(user, req.getParameterMap());
// 自动：String → int / long / boolean / Date（可注册自定义转换器）
```

### 7.3 参数校验

```java
// 校验工具
public class Validator {
    public static List<String> validate(Object bean) {
        List<String> errors = new ArrayList<>();
        for (Field field : bean.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                // @NotNull 检查
                if (field.isAnnotationPresent(NotNull.class) && value == null) {
                    errors.add(field.getName() + " 不能为空");
                }
                // @Length 检查
                Length len = field.getAnnotation(Length.class);
                if (len != null && value instanceof String) {
                    String s = (String) value;
                    if (s.length() < len.min() || s.length() > len.max()) {
                        errors.add(field.getName() + " 长度应为 "
                            + len.min() + "-" + len.max());
                    }
                }
            } catch (IllegalAccessException e) { }
        }
        return errors;
    }
}

// 使用
List<String> errors = Validator.validate(user);
if (!errors.isEmpty()) {
    req.setAttribute("errors", errors);
    req.getRequestDispatcher("/register.jsp").forward(req, resp);
    return;
}
```

---

## 8. 统一结果返回与异常处理

### 8.1 统一响应结构

```java
// JSON 返回格式——后端"铁律"
public class Result<T> {
    private int code;        // 状态码：200 成功，4xx 客户端错误，5xx 服务端错误
    private String message;  // 提示信息
    private T data;          // 数据载荷

    // 工厂方法
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> success(String message, T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = message;
        r.data = data;
        return r;
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }

    // getter/setter...
}

// Controller 中使用
Result<List<User>> result = Result.success(users);
resp.setContentType("application/json;charset=UTF-8");
resp.getWriter().write(new Gson().toJson(result));
```

### 8.2 统一异常处理

```java
// 自定义业务异常
public class BusinessException extends RuntimeException {
    private int code;
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
    public int getCode() { return code; }
}

// Controller 中统一 try-catch（模板方法模式）
public abstract class BaseController extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            super.service(req, resp);
        } catch (BusinessException e) {
            handleException(resp, e.getCode(), e.getMessage());
        } catch (Exception e) {
            handleException(resp, 500, "服务器内部错误");
            e.printStackTrace();
        }
    }

    private void handleException(HttpServletResponse resp, int code, String msg)
            throws IOException {
        resp.setStatus(code == 200 ? 200 : code);
        resp.setContentType("application/json;charset=UTF-8");
        Result<?> result = Result.error(code, msg);
        resp.getWriter().write(new Gson().toJson(result));
    }
}
```

---

## 9. 手写一个 Mini MVC 框架

> 🎯 这个迷你框架浓缩了 Spring MVC 的核心思想：IoC 容器 + 依赖注入 + 请求映射 + 参数绑定。

```java
// 步骤1：定义注解
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Controller { }

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequestMapping {
    String value();
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Autowired { }

// 步骤2：IoC 容器（极简版）
public class MiniApplicationContext {
    private Map<String, Object> beans = new ConcurrentHashMap<>();

    public MiniApplicationContext(String basePackage) {
        // 扫描包下所有带 @Controller 注解的类
        scanAndInstantiate(basePackage);
        // 注入依赖（@Autowired）
        injectDependencies();
    }

    private void scanAndInstantiate(String basePackage) {
        // 用反射/类路径扫描找到所有类
        // 实例化并放入 beans Map
    }

    private void injectDependencies() {
        for (Object bean : beans.values()) {
            for (Field field : bean.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    field.setAccessible(true);
                    Object dependency = beans.get(field.getType().getName());
                    field.set(bean, dependency);
                }
            }
        }
    }

    public Object getBean(String name) { return beans.get(name); }
}

// 步骤3：DispatcherServlet（Front Controller 模式）
@WebServlet("/")
public class DispatcherServlet extends HttpServlet {
    private MiniApplicationContext context;

    @Override
    public void init() {
        context = new MiniApplicationContext("com.example.controller");
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String uri = req.getRequestURI().replace(req.getContextPath(), "");

        // 遍历所有 Controller，匹配 @RequestMapping
        for (Object bean : context.getAllBeans()) {
            if (!bean.getClass().isAnnotationPresent(Controller.class)) continue;
            for (Method method : bean.getClass().getDeclaredMethods()) {
                RequestMapping mapping = method.getAnnotation(RequestMapping.class);
                if (mapping != null && mapping.value().equals(uri)) {
                    // 反射调用
                    Object result = method.invoke(bean, parseArgs(method, req, resp));
                    // 处理返回值（View 名 / JSON）
                    handleResult(result, req, resp);
                    return;
                }
            }
        }
        resp.sendError(404);
    }

    private Object[] parseArgs(Method method, HttpServletRequest req,
                                HttpServletResponse resp) {
        // 根据方法参数类型自动注入 req、resp、@RequestParam 等
        // ...
    }
}
```

> 💡 这个 Mini MVC 示范了 Spring MVC 的三大核心：**IoC 容器**（Bean 管理）、**DispatcherServlet**（Front Controller 统一入口）、**注解映射**（`@Controller` + `@RequestMapping`）。理解了这个就知道 Spring MVC "做了什么"。

---

## 10. 常见面试题

### Q1：MVC 各层职责是什么？

> Model（数据+业务逻辑）、View（页面渲染）、Controller（流程控制）。JavaWeb 中分别对应 Service/DAO、JSP、Servlet。详见第1节。

### Q2：Forward 和 Redirect 的区别？什么时候用哪个？

> Forward 是服务器内部跳转（1次请求，URL 不变），Redirect 是浏览器跳转（2次请求，URL 变）。表单提交后应用 Redirect（PRG 模式），传递数据用 Forward。详见第4节。

### Q3：三层架构（Controller-Service-DAO）的好处？

> 职责分离：每层只做自己该做的事。便于维护、测试、替换实现（如换数据库只需改 DAO 层）。详见第5节。

### Q4：Spring MVC 和原生 Servlet 的关系？

> DispatcherServlet 本质上就是一个 Servlet（继承 HttpServlet）。Spring MVC 在 Servlet 之上封装了注解映射、参数绑定、视图解析等能力，本质是对原生 Servlet 的增强。详见第9节。

### Q5：如何处理全局异常？

> 在 BaseController/BaseServlet 的 `service()` 方法中 try-catch，或使用 Filter 捕获，或 Spring 的 `@ControllerAdvice`。详见第8节。
