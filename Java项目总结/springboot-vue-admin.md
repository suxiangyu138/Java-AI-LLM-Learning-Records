# 后台管理系统开发文档

> **技术栈**：SpringBoot 3.x + Vue 3 + MyBatis-Plus + Redis + JWT + RBAC  
> **架构**：前后端分离 | RESTful API | 统一响应规范  
> **适用场景**：简历项目 / 实习作品 / 企业级脚手架

---

## 目录

- [项目结构](#项目结构)
- [统一响应规范](#统一响应规范)
- [JWT 认证流程](#jwt-认证流程)
- [RBAC 权限模型](#rbac-权限模型)
- [核心接口设计](#核心接口设计)
- [全局异常处理](#全局异常处理)
- [跨域配置](#跨域配置)
- [Vue 前端对接](#vue-前端对接)
- [数据库设计](#数据库设计)
- [简历亮点提炼](#简历亮点提炼)

---

## 项目结构

```
admin-backend/
├── src/main/java/com/example/admin/
│   ├── common/
│   │   ├── Result.java              # 统一响应体
│   │   ├── ResultCode.java          # 状态码枚举
│   │   └── PageResult.java          # 分页响应体
│   ├── config/
│   │   ├── SecurityConfig.java      # Spring Security 配置
│   │   ├── CorsConfig.java          # 跨域配置
│   │   └── RedisConfig.java         # Redis 配置
│   ├── controller/
│   │   ├── AuthController.java      # 登录/登出
│   │   ├── UserController.java      # 用户管理
│   │   ├── RoleController.java      # 角色管理
│   │   └── MenuController.java      # 菜单/权限管理
│   ├── entity/
│   │   ├── User.java
│   │   ├── Role.java
│   │   ├── Menu.java
│   │   ├── UserRole.java
│   │   └── RoleMenu.java
│   ├── filter/
│   │   └── JwtAuthFilter.java       # JWT 过滤器
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── UserService.java
│   │   └── PermissionService.java
│   └── util/
│       └── JwtUtil.java             # JWT 工具类
└── src/main/resources/
    └── application.yml

admin-frontend/
├── src/
│   ├── api/           # Axios 封装 + 接口模块
│   ├── router/        # 动态路由
│   ├── store/         # Pinia 状态管理
│   ├── views/
│   │   ├── Login.vue
│   │   ├── Dashboard.vue
│   │   ├── user/
│   │   └── role/
│   └── utils/
│       └── request.js # Axios 拦截器
```

---

## 统一响应规范

### ResultCode 状态码

```java
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    PARAM_ERROR(400, "参数校验失败"),
    SERVER_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    // getter...
}
```

### 统一响应体

```java
@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;
    private long timestamp;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> fail(ResultCode code) {
        return new Result<>(code.getCode(), code.getMessage(), null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
```

### 分页响应体

```java
@Data
@AllArgsConstructor
public class PageResult<T> {
    private long total;
    private List<T> records;

    public static <T> Result<PageResult<T>> of(Page<T> page) {
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords()));
    }
}
```

---

## JWT 认证流程

### 依赖

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

### JwtUtil

```java
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration; // 单位：毫秒，建议 7200000（2小时）

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, List<String> roles) {
        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
```

### JWT 过滤器

```java
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        String token = authHeader.substring(7);
        if (jwtUtil.isTokenValid(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            String username = jwtUtil.getUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
        chain.doFilter(request, response);
    }
}
```

### application.yml

```yaml
jwt:
  secret: your-256-bit-secret-key-replace-in-production
  expiration: 7200000

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/admin_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  redis:
    host: localhost
    port: 6379
    database: 0
  data:
    redis:
      repositories:
        enabled: false
```

---

## RBAC 权限模型

### 核心思想

```
User ──── UserRole ──── Role ──── RoleMenu ──── Menu(Permission)
用户         中间表       角色      中间表         菜单/权限节点
```

- **User**：系统用户
- **Role**：角色（admin / editor / viewer）
- **Menu**：菜单项 + 按钮权限，`type` 区分（0=目录 1=菜单 2=按钮），`permission` 字段存权限标识如 `user:add`

### Security 配置

```java
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/doc.html", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"code\":401,\"message\":\"未登录或Token已过期\"}");
                })
                .accessDeniedHandler((req, res, e) -> {
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"code\":403,\"message\":\"无权限访问\"}");
                })
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

### 方法级权限注解

```java
@RestController
@RequestMapping("/api/user")
public class UserController {

    @PreAuthorize("hasAuthority('user:list')")
    @GetMapping("/list")
    public Result<?> list(...) { }

    @PreAuthorize("hasAuthority('user:add')")
    @PostMapping
    public Result<?> add(...) { }

    @PreAuthorize("hasAuthority('user:delete')")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) { }
}
```

---

## 核心接口设计

### 认证模块 `/api/auth`

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | `/api/auth/login` | 登录获取Token | ❌ |
| POST | `/api/auth/logout` | 登出（Token加黑名单） | ✅ |
| GET | `/api/auth/info` | 获取当前用户信息+权限 | ✅ |
| POST | `/api/auth/refresh` | 刷新Token | ✅ |

**登录请求**
```json
POST /api/auth/login
{
  "username": "admin",
  "password": "123456"
}
```

**登录响应**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "userInfo": {
      "id": 1,
      "username": "admin",
      "nickname": "超级管理员",
      "avatar": "https://...",
      "roles": ["admin"],
      "permissions": ["user:list", "user:add", "user:delete", "role:list"]
    }
  }
}
```

### 用户模块 `/api/user`

| 方法 | 路径 | 描述 | 权限标识 |
|------|------|------|----------|
| GET | `/api/user/page` | 分页查询 | `user:list` |
| POST | `/api/user` | 新增用户 | `user:add` |
| PUT | `/api/user/{id}` | 修改用户 | `user:edit` |
| DELETE | `/api/user/{id}` | 删除用户 | `user:delete` |
| PUT | `/api/user/{id}/password` | 重置密码 | `user:resetPwd` |
| PUT | `/api/user/{id}/status` | 启用/禁用 | `user:edit` |

**分页查询请求**
```
GET /api/user/page?pageNum=1&pageSize=10&keyword=张三&status=1
```

**分页响应**
```json
{
  "code": 200,
  "data": {
    "total": 100,
    "records": [
      {
        "id": 1,
        "username": "zhangsan",
        "nickname": "张三",
        "email": "zhangsan@example.com",
        "status": 1,
        "roles": ["editor"],
        "createTime": "2024-01-01 10:00:00"
      }
    ]
  }
}
```

### 角色模块 `/api/role`

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/role/list` | 查询全部角色 |
| POST | `/api/role` | 新增角色 |
| PUT | `/api/role/{id}` | 修改角色 |
| DELETE | `/api/role/{id}` | 删除角色 |
| PUT | `/api/role/{id}/menu` | 分配菜单权限 |

### 菜单模块 `/api/menu`

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/menu/tree` | 获取菜单树 |
| GET | `/api/menu/routes` | 获取当前用户路由（动态路由） |
| POST | `/api/menu` | 新增菜单/按钮 |
| PUT | `/api/menu/{id}` | 修改菜单 |
| DELETE | `/api/menu/{id}` | 删除菜单 |

---

## 全局异常处理

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return Result.fail(400, msg);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> handleAccessDenied(AccessDeniedException e) {
        return Result.fail(ResultCode.FORBIDDEN);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public Result<?> handleBadCredentials(BadCredentialsException e) {
        return Result.fail(400, "用户名或密码错误");
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常：", e);
        return Result.fail(ResultCode.SERVER_ERROR);
    }
}
```

---

## 跨域配置

```java
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

---

## Vue 前端对接

### Axios 封装（`utils/request.js`）

```javascript
import axios from 'axios'
import { useUserStore } from '@/store/user'
import router from '@/router'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000
})

// 请求拦截：自动携带 Token
request.interceptors.request.use(config => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers['Authorization'] = `Bearer ${userStore.token}`
  }
  return config
})

// 响应拦截：统一处理状态码
request.interceptors.response.use(
  res => {
    const { code, message, data } = res.data
    if (code === 200) return data
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  err => {
    const status = err.response?.status
    if (status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      router.push('/login')
      ElMessage.warning('登录已过期，请重新登录')
    } else if (status === 403) {
      ElMessage.error('无权限访问')
    }
    return Promise.reject(err)
  }
)

export default request
```

### Pinia 用户状态（`store/user.js`）

```javascript
import { defineStore } from 'pinia'
import { login, getUserInfo } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: null,
    roles: [],
    permissions: []
  }),
  actions: {
    async loginAction(loginForm) {
      const data = await login(loginForm)
      this.token = data.token
      localStorage.setItem('token', data.token)
      this.userInfo = data.userInfo
      this.roles = data.userInfo.roles
      this.permissions = data.userInfo.permissions
    },
    async fetchUserInfo() {
      const data = await getUserInfo()
      this.userInfo = data
      this.roles = data.roles
      this.permissions = data.permissions
    },
    logout() {
      this.token = ''
      this.userInfo = null
      this.roles = []
      this.permissions = []
      localStorage.removeItem('token')
    }
  }
})
```

### 动态路由（`router/index.js`）

```javascript
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const staticRoutes = [
  { path: '/login', component: () => import('@/views/Login.vue') },
  { path: '/', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes: staticRoutes
})

// 路由守卫：Token 校验 + 动态路由加载
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  if (to.path === '/login') return next()
  if (!userStore.token) return next('/login')
  if (!userStore.userInfo) {
    await userStore.fetchUserInfo()
    // 根据后端返回的 routes 动态 addRoute
    // const routes = await getRoutes()
    // routes.forEach(r => router.addRoute(r))
  }
  next()
})

export default router
```

### 按钮级权限指令（`directives/permission.js`）

```javascript
// v-permission="'user:add'"
export const permission = {
  mounted(el, binding) {
    const userStore = useUserStore()
    if (!userStore.permissions.includes(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  }
}
```

---

## 数据库设计

```sql
-- 用户表
CREATE TABLE sys_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
    nickname    VARCHAR(50)  COMMENT '昵称',
    email       VARCHAR(100) COMMENT '邮箱',
    avatar      VARCHAR(255) COMMENT '头像URL',
    status      TINYINT DEFAULT 1 COMMENT '状态 1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT DEFAULT 0 COMMENT '逻辑删除'
);

-- 角色表
CREATE TABLE sys_role (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name   VARCHAR(50)  NOT NULL COMMENT '角色名',
    role_key    VARCHAR(50)  NOT NULL UNIQUE COMMENT '角色标识 如 admin',
    sort        INT DEFAULT 0,
    status      TINYINT DEFAULT 1,
    remark      VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 菜单/权限表
CREATE TABLE sys_menu (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id   BIGINT DEFAULT 0 COMMENT '父节点ID，0为根',
    menu_name   VARCHAR(50)  NOT NULL,
    path        VARCHAR(200) COMMENT '路由路径',
    component   VARCHAR(200) COMMENT '组件路径',
    permission  VARCHAR(100) COMMENT '权限标识 如 user:add',
    type        TINYINT COMMENT '0目录 1菜单 2按钮',
    icon        VARCHAR(100),
    sort        INT DEFAULT 0,
    status      TINYINT DEFAULT 1
);

-- 用户角色关联
CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

-- 角色菜单关联
CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);
```

---

## 简历亮点提炼

> 直接复制进简历项目描述模块

- 基于 **SpringBoot 3 + Spring Security + JWT** 实现无状态 Token 认证，Token 续期与主动失效通过 **Redis 黑名单机制**实现
- 设计 **RBAC 五表权限模型**（用户-角色-菜单），支持菜单级、按钮级权限控制，前端通过自定义 `v-permission` 指令实现动态鉴权
- 制定**统一接口规范**（Result<T> 响应体、ResultCode 枚举、GlobalExceptionHandler），覆盖参数校验、鉴权失败、业务异常全场景
- Vue 3 前端配合 **Pinia + 动态路由**，登录后根据用户权限动态渲染侧边栏菜单与路由，实现真正的按权限访问控制
- 采用 **MyBatis-Plus 逻辑删除 + 分页插件**，核心 CRUD 接口开发效率提升 60%，支持关键字模糊检索 + 多字段排序分页

---

*文档版本：v1.0 | 技术栈：SpringBoot 3.x / Vue 3 / Element Plus / MyBatis-Plus / Redis / JWT*
