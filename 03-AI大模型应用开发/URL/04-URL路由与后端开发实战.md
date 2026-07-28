# 04 - URL 路由与后端开发实战

> Java 后端的 URL 核心战场——Spring 路由匹配规则、RESTful API 设计、Nginx `location`/`proxy_pass`、网关路由、重定向状态码与 CORS

---

## 📚 目录

1. [Spring 路由映射全解](#1-spring-路由映射全解)
2. [路径匹配 AntPathMatcher 与 PathPattern](#2-路径匹配-antpathmatcher-与-pathpattern)
3. [Spring Boot 3 尾斜杠踩坑](#3-spring-boot-3-尾斜杠踩坑)
4. [RESTful URL 设计规范](#4-restful-url-设计规范)
5. [API 版本管理策略](#5-api-版本管理策略)
6. [Nginx location 与 proxy_pass](#6-nginx-location-与-proxy_pass)
7. [Spring Cloud Gateway 路由](#7-spring-cloud-gateway-路由)
8. [重定向状态码 301-308](#8-重定向状态码-301-308)
9. [CORS 与同源策略](#9-cors-与同源策略)

---

## 1. Spring 路由映射全解

### 1.1 注解全家桶

```java
@RestController
@RequestMapping("/api/v1/users")     // 类级前缀
public class UserController {

    // GET /api/v1/users?page=0&size=20&sort=id,desc
    @GetMapping
    public Page<UserVO> list(@RequestParam(defaultValue = "") String kw,
                             Pageable pageable) { ... }

    // GET /api/v1/users/42
    @GetMapping("/{id}")
    public UserVO get(@PathVariable Long id) { ... }

    // GET /api/v1/users/42/orders/2026?status=PAID
    @GetMapping("/{userId}/orders/{year}")
    public List<OrderVO> orders(@PathVariable Long userId,
                                @PathVariable int year,
                                @RequestParam(required = false) String status) { ... }

    // 正则约束：只匹配纯数字 id
    @GetMapping("/{id:\\d+}")
    public UserVO getById(@PathVariable Long id) { ... }

    // 匹配文件名：/files/report.2026.pdf → name=report.2026, ext=pdf
    @GetMapping("/files/{name}.{ext}")
    public void download(@PathVariable String name, @PathVariable String ext) { ... }

    // 通配剩余路径：/api/v1/users/tree/a/b/c → rest = "a/b/c"
    @GetMapping("/tree/**")
    public String tree(HttpServletRequest req) {
        return new AntPathMatcher().extractPathWithinPattern(
            "/api/v1/users/tree/**", req.getRequestURI());
    }

    // 按 Query 参数区分同一路径
    @GetMapping(params = "action=export")
    public void export() { ... }

    // 按 Header 区分
    @GetMapping(headers = "X-Api-Version=2")
    public UserVO listV2() { ... }

    // 内容协商
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_XML_VALUE)
    public UserVO getXml(@PathVariable Long id) { ... }
}
```

### 1.2 参数绑定注解对照

| 注解 | 取值位置 | 示例 URL / 请求 |
|------|---------|----------------|
| `@PathVariable` | Path 占位符 | `/users/{id}` ← `/users/42` |
| `@RequestParam` | Query 或表单 | `?kw=abc` |
| `@RequestBody` | Body（JSON） | POST body |
| `@RequestHeader` | 请求头 | `Authorization: xxx` |
| `@CookieValue` | Cookie | `Cookie: sid=xxx` |
| `@MatrixVariable` | Path 分号参数 | `/cars/color=red;year=2026` |
| `@ModelAttribute` / POJO | Query 批量绑定对象 | `?name=tom&age=18` |
| `@RequestPart` | multipart | 文件上传 |

### 1.3 `@PathVariable` 三个高频坑

**坑 1：路径变量含 `.` 被截断**

```java
// GET /api/files/report.pdf
@GetMapping("/files/{filename}")
public void download(@PathVariable String filename) { ... }
// Spring Boot 2 默认 useSuffixPatternMatch → filename = "report"（.pdf 被吃掉）⚠️
```

```java
// ✅ 解法一：正则允许点号
@GetMapping("/files/{filename:.+}")

// ✅ 解法二（Spring Boot 3 默认已修正，无需配置）
// Spring Boot 2 需要：
// spring.mvc.pathmatch.use-suffix-pattern=false
```

**坑 2：路径变量含 `/` 或编码后的 `%2F`**

```java
// 需求：/api/files/a/b/c.txt 整体作为 filename
@GetMapping("/files/**")
public void download(HttpServletRequest req) {
    String path = new AntPathMatcher().extractPathWithinPattern(
            "/api/files/**",
            (String) req.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE));
    // path = "a/b/c.txt"
}
```

> ⚠️ Tomcat 默认拒绝 URL 中的编码斜杠（返回 400），需 `-Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true` 才放行——**但这会打开路径穿越风险，一般不开**。改用 `/**` 通配。

**坑 3：`@PathVariable` 名字不一致**

```java
// ❌ 编译时不报错，运行时抛异常
@GetMapping("/{userId}")
public UserVO get(@PathVariable Long id) { ... }
// MissingPathVariableException

// ✅ 显式指定
@GetMapping("/{userId}")
public UserVO get(@PathVariable("userId") Long id) { ... }
// 或者开启 -parameters 编译参数保留形参名
```

---

## 2. 路径匹配 AntPathMatcher 与 PathPattern

Spring 有**两套**路径匹配器：

| 维度 | `AntPathMatcher`（旧） | `PathPattern`（新，Spring 5.3+ 默认） |
|------|----------------------|----------------------------------|
| 引入版本 | Spring 2.x | Spring 5.0（WebFlux）/ 5.3（MVC） |
| Spring Boot 3 默认 | ❌ | ✅ |
| 性能 | 每次字符串扫描 | 预解析成 Pattern 树，**快 6~8 倍** |
| `**` 位置 | 任意位置 | **只能在末尾** |
| 后缀匹配 `.*` | 支持 | 已移除 |
| URI 编码处理 | 全路径解码后匹配（不安全） | 逐段解码（更安全） |

### 2.1 通配符语义

| 通配符 | 含义 | 匹配 | 不匹配 |
|:---:|------|------|--------|
| `?` | 单个字符（不含 `/`） | `/a?c` → `/abc` | `/ac`、`/abbc` |
| `*` | 单段内任意字符（不含 `/`） | `/a/*/c` → `/a/x/c` | `/a/x/y/c` |
| `**` | 跨多段 | `/a/**` → `/a`、`/a/b`、`/a/b/c` | — |
| `{name}` | 捕获单段 | `/u/{id}` → `/u/42` | `/u/4/2` |
| `{name:regex}` | 带约束捕获 | `/u/{id:\\d+}` → `/u/42` | `/u/abc` |
| `{*name}` | 捕获剩余全部（PathPattern 独有） | `/f/{*path}` → path=`/a/b/c` | — |

```java
// PathPattern 独有的 {*name}，比 ** + 手动提取优雅
@GetMapping("/files/{*path}")
public void download(@PathVariable String path) {
    // GET /files/a/b/c.txt  →  path = "/a/b/c.txt"（含前导斜杠）
}
```

### 2.2 优先级排序规则

多个模式都能匹配时，Spring 选**最具体**的：

```text
优先级从高到低：
1. 完全字面量        /api/users/current
2. 单段变量          /api/users/{id}
3. 单段变量 + 正则    /api/users/{id:\d+}   ← 与 2 同级，先注册优先
4. 单星通配          /api/users/*
5. 双星通配          /api/users/**
6. 全匹配            /**
```

```java
// 两者共存时，/api/users/current 命中第一个（字面量优先）
@GetMapping("/api/users/current")  public UserVO me() { ... }
@GetMapping("/api/users/{id}")     public UserVO get(@PathVariable Long id) { ... }
```

> ⚠️ 若把 `/api/users/{id}` 与 `/api/users/current` 反过来理解，会以为 `current` 会被当成 id 报数字转换异常。**实际不会**——Spring 明确规定字面量优先。但若第二个写成 `/api/users/{name}`（String 类型）也不会冲突，只是注意别写重复模式导致 `IllegalStateException: Ambiguous mapping`。

### 2.3 显式切换匹配策略

```yaml
# Spring Boot 3 默认 path_pattern_parser；老项目迁移遇到 ** 在中间的情况可回退
spring:
  mvc:
    pathmatch:
      matching-strategy: ant_path_matcher   # 或 path_pattern_parser（默认）
```

> ⚠️ **Springfox / Swagger 2 兼容坑**：Spring Boot 2.6+ 默认改为 `path_pattern_parser` 后，Springfox 3 启动报 `Failed to start bean 'documentationPluginsBootstrapper'`。解法：切回 `ant_path_matcher`，或迁移到 **springdoc-openapi**（推荐）。

### 2.4 Spring Security 路径匹配

```java
@Bean
SecurityFilterChain chain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/public/**", "/actuator/health").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()
        .requestMatchers("/api/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated()
    );
    return http.build();
}
```

> ⚠️ **权限绕过高危坑**：Security 的路径匹配与 MVC 的**必须一致**，否则出现"Security 认为不匹配放行、MVC 认为匹配执行"的绕过。典型案例：
> - `/api/admin`（无尾斜杠）没被 `/api/admin/**` 覆盖 → 应写 `"/api/admin", "/api/admin/**"`
> - 大小写：`/API/Admin/x` 在某些容器下能到 MVC，但 Security 模式区分大小写 → 用 Nginx 统一转小写或加 `RegexRequestMatcher` 忽略大小写
> - 分号：`/api/admin;x=1/list` 曾用于绕过（Spring 已默认剥离分号内容）

---

## 3. Spring Boot 3 尾斜杠踩坑

**这是 Spring Boot 2 → 3 升级最常见的线上事故之一。**

| 版本 | `/api/users` 声明，请求 `/api/users/` |
|------|------------------------------------|
| Spring Boot 2.x | ✅ 200（默认 `useTrailingSlashMatch = true`） |
| **Spring Boot 3.x** | ❌ **404**（默认关闭，且 `setUseTrailingSlashMatch` 已弃用） |

### 3.1 三种解法

```java
// ✅ 方案一（推荐）：Nginx / 网关层统一重定向，一劳永逸
```

```nginx
# 去掉尾斜杠并 308 重定向（保留请求方法与 Body）
location ~ ^(.+)/$ {
    return 308 $1$is_args$args;
}
```

```java
// ✅ 方案二：Spring 侧配置 PathPattern 容忍尾斜杠（Spring Framework 6.0+）
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseTrailingSlashMatch(true);   // ⚠️ 已 @Deprecated，仅作过渡
    }
}
```

```java
// ✅ 方案三：注册重定向视图控制器（精细但繁琐）
@Override
public void addViewControllers(ViewControllerRegistry registry) {
    registry.addRedirectViewController("/api/users/", "/api/users");
}
```

> 🎯 **核心要点**：官方立场是"**一个资源只应有一个 URL**"，尾斜杠变体应通过 **308 重定向**收敛，而不是双份匹配。这也对 SEO 友好（避免重复内容）。

---

## 4. RESTful URL 设计规范

### 4.1 六条铁律

| # | 规则 | ❌ 反例 | ✅ 正例 |
|:---:|------|--------|--------|
| 1 | **用名词，不用动词**（动作由 HTTP 方法表达） | `GET /getUser?id=1`<br>`POST /createUser` | `GET /users/1`<br>`POST /users` |
| 2 | **集合用复数** | `/user/1` | `/users/1` |
| 3 | **层级表达从属关系**（不超过 2~3 层） | `/orderItems?orderId=1` | `/orders/1/items` |
| 4 | **筛选/排序/分页用 Query** | `/users/active/page/2` | `/users?status=active&page=2` |
| 5 | **全小写 + 中划线** | `/userProfile`、`/user_profile` | `/user-profiles` |
| 6 | **不带文件后缀**（用 `Accept` 协商） | `/users/1.json` | `/users/1` + `Accept: application/json` |

### 4.2 标准 CRUD 映射

| 操作 | 方法 | URL | 幂等 | 安全 | 成功码 |
|------|:---:|-----|:---:|:---:|:---:|
| 列表 | `GET` | `/orders` | ✅ | ✅ | 200 |
| 详情 | `GET` | `/orders/42` | ✅ | ✅ | 200 / 404 |
| 创建 | `POST` | `/orders` | ❌ | ❌ | 201 + `Location` 头 |
| 全量更新 | `PUT` | `/orders/42` | ✅ | ❌ | 200 / 204 |
| 部分更新 | `PATCH` | `/orders/42` | ❌ | ❌ | 200 / 204 |
| 删除 | `DELETE` | `/orders/42` | ✅ | ❌ | 204 |
| 子资源 | `GET` | `/orders/42/items` | ✅ | ✅ | 200 |
| 关联操作 | `PUT` | `/orders/42/items/7` | ✅ | ❌ | 204 |

```java
// 创建成功应返回 201 + Location 指向新资源
@PostMapping
public ResponseEntity<OrderVO> create(@RequestBody @Valid OrderDTO dto) {
    OrderVO vo = service.create(dto);
    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(vo.getId()).toUri();
    return ResponseEntity.created(location).body(vo);
    // → 201 Created
    //   Location: https://api.example.com/api/v1/orders/1001
}
```

### 4.3 非 CRUD 动作怎么设计

REST 的最大痛点：**"支付"、"审批"、"发送短信" 不是 CRUD**。

| 方案 | 示例 | 评价 |
|------|------|------|
| **子资源化**（推荐） | `POST /orders/42/payments` | 最 RESTful，动作变成资源 |
| **状态字段 PATCH** | `PATCH /orders/42 {"status":"PAID"}` | 适合简单状态流转 |
| **动作端点**（务实） | `POST /orders/42:pay`<br>`POST /orders/42/actions/pay` | Google API 风格，可读性好 |
| **RPC 风格**（大厂常见） | `POST /orderService/pay` | 不 REST 但直白，内部服务够用 |

> 💡 **务实建议**：对外开放 API 严格 REST；内部微服务之间用 RPC 风格或动作端点即可，不要为了"纯 REST"把 `POST /orders/42/status-transitions` 这类反直觉设计强加给团队。

### 4.4 分页与筛选约定

```text
GET /articles
  ?page=0&size=20                        # 页码分页（Spring Pageable）
  &sort=createTime,desc&sort=id,asc      # 多字段排序
  &status=PUBLISHED&status=DRAFT         # 多值筛选（重复 key）
  &createTime.gte=2026-01-01             # 范围筛选（点号操作符）
  &kw=URL编码                             # 关键词
  &fields=id,title,author                # 稀疏字段（减少传输）
  &include=author,tags                   # 关联展开
```

| 分页方式 | Query | 优点 | 缺点 |
|---------|-------|------|------|
| **Offset 分页** | `?page=5&size=20` 或 `?offset=100&limit=20` | 可跳页，前端简单 | 深分页慢（`LIMIT 100000,20`） |
| **游标分页** | `?cursor=eyJpZCI6MTIzfQ&limit=20` | 深翻恒定性能，无重复/漏数据 | 不能跳页 |
| **Keyset 分页** | `?lastId=123&limit=20` | 同上，实现简单 | 排序字段需唯一 |

```java
// 游标分页：把 lastId 用 Base64URL 编码成不透明 cursor
String cursor = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(("{\"id\":" + lastId + "}").getBytes(UTF_8));
// → /articles?cursor=eyJpZCI6MTIzfQ&limit=20
```

> 🎯 大数据量列表（信息流、日志、订单流水）**一律用游标分页**；后台管理页需要跳页时才用 offset，并限制最大页数（如 `page <= 500`）。

---

## 5. API 版本管理策略

| 策略 | 示例 | 优点 | 缺点 |
|------|------|------|------|
| **URL 路径**（最常用） | `/api/v1/users` | 直观、可缓存、易调试、浏览器可直接访问 | URL 不"纯粹"（版本不是资源属性） |
| Query 参数 | `/api/users?version=1` | 改动小 | 易被漏传、缓存 Key 复杂 |
| 自定义 Header | `X-Api-Version: 1` | URL 干净 | 不能直接贴浏览器测试、CDN 需配 Vary |
| Accept 协商 | `Accept: application/vnd.example.v1+json` | 最符合 REST 理论 | 复杂、调试麻烦、团队学习成本高 |
| 子域名 | `https://v1.api.example.com` | 可完全独立部署 | 证书/DNS/跨域成本 |

```java
// 路径版本 + 常量前缀，便于统一改
public interface ApiPaths {
    String V1 = "/api/v1";
    String V2 = "/api/v2";
}

@RestController
@RequestMapping(ApiPaths.V1 + "/users")
public class UserV1Controller { ... }

@RestController
@RequestMapping(ApiPaths.V2 + "/users")
public class UserV2Controller { ... }
```

```yaml
# 或用统一前缀配置（不影响 actuator）
spring:
  mvc:
    servlet:
      path: /            # Servlet 根路径
server:
  servlet:
    context-path: /api   # 全局上下文前缀
```

> 💡 **版本演进纪律**：只在**破坏性变更**时升版本（删字段、改语义、改类型）。加字段、加可选参数属于兼容变更，不升版本。老版本至少保留 6~12 个月并在响应头加 `Deprecation` / `Sunset`：
> ```http
> Deprecation: true
> Sunset: Wed, 31 Dec 2026 23:59:59 GMT
> Link: </api/v2/users>; rel="successor-version"
> ```

---

## 6. Nginx location 与 proxy_pass

### 6.1 `location` 匹配优先级（必背）

```text
优先级从高到低：
1. =        精确匹配，命中立即停止
2. ^~       前缀匹配，命中后不再尝试正则
3. ~  ~*    正则匹配（~ 区分大小写，~* 不区分），按配置文件顺序，首个命中即用
4. 无修饰符  最长前缀匹配（作为兜底，正则优先于它）
```

```nginx
server {
    listen 80;
    server_name example.com;

    location = /                { return 200 "exact root\n";  }   # 只匹配 /
    location = /favicon.ico     { access_log off; expires 30d; }
    location ^~ /static/        { root /var/www; expires 7d;   }   # 命中后跳过正则
    location ~* \.(jpg|png|webp)$ { expires 30d; }                 # 正则
    location ~ ^/api/v(\d+)/    { proxy_pass http://backend;  }
    location /                  { proxy_pass http://frontend; }    # 兜底
}
```

**匹配演练**：

| 请求 | 命中 | 原因 |
|------|------|------|
| `/` | `= /` | 精确匹配最高 |
| `/static/logo.png` | `^~ /static/` | `^~` 命中后**不**再试 `~* \.png$` |
| `/img/logo.png` | `~* \.(jpg\|png)$` | 正则优先于兜底 `/` |
| `/api/v1/users` | `~ ^/api/v(\d+)/` | 正则命中 |
| `/about` | `/` | 兜底 |

### 6.2 `proxy_pass` 尾斜杠——最经典的坑

```nginx
# ① location 有 / ，proxy_pass 有 /  →  替换掉 location 前缀
location /api/ { proxy_pass http://backend/; }
#  /api/users  →  后端收到  /users            ✅ 常用（剥前缀）

# ② location 有 / ，proxy_pass 无 /  →  完整路径透传
location /api/ { proxy_pass http://backend; }
#  /api/users  →  后端收到  /api/users        ✅ 常用（保留前缀）

# ③ proxy_pass 带路径且有 /  →  前缀被替换成该路径
location /api/ { proxy_pass http://backend/v1/; }
#  /api/users  →  后端收到  /v1/users

# ④ proxy_pass 带路径无 /  →  字符串拼接（注意会粘在一起）
location /api/ { proxy_pass http://backend/v1; }
#  /api/users  →  后端收到  /v1users          ⚠️ 少了斜杠！

# ⑤ location 用了正则  →  proxy_pass 不能带 URI，必须配 rewrite
location ~ ^/api/(.*)$ {
    proxy_pass http://backend/$1;              # 正则里可用捕获组
}
```

> 🎯 **一句话记忆**：**`proxy_pass` 末尾有 `/`，就把 `location` 匹配到的前缀"吃掉"；没有 `/`，就把原始 URI 原样带上。**

### 6.3 反代必备头

```nginx
location /api/ {
    proxy_pass http://backend/;

    proxy_set_header Host              $host;             # 后端拿到原始域名
    proxy_set_header X-Real-IP         $remote_addr;      # 真实客户端 IP
    proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;           # http/https，影响重定向生成
    proxy_set_header X-Forwarded-Host  $host;
    proxy_set_header X-Forwarded-Port  $server_port;

    proxy_http_version 1.1;
    proxy_set_header Upgrade    $http_upgrade;            # WebSocket 必需
    proxy_set_header Connection "upgrade";

    proxy_connect_timeout 5s;
    proxy_read_timeout    600s;      # ⚠️ LLM 流式输出必须调大
    proxy_buffering       off;       # ⚠️ SSE / 流式输出必须关闭缓冲
}
```

```yaml
# Spring Boot 侧必须信任转发头，否则生成的重定向 URL 是 http:// 而非 https://
server:
  forward-headers-strategy: framework   # 或 native（部署在容器网关后时）
```

> ⚠️ **LLM 应用两大 Nginx 坑**：
> 1. `proxy_buffering on`（默认）会把 SSE 流全部缓存到结束才吐 → 前端"卡住不动"，必须 `off`
> 2. `proxy_read_timeout 60s`（默认）会在长推理时 504 → 调到 `600s` 以上

### 6.4 `rewrite` 与 `return`

```nginx
# rewrite 语法：rewrite regex replacement [flag];
rewrite ^/old/(.*)$ /new/$1 permanent;    # 301 永久
rewrite ^/tmp/(.*)$ /new/$1 redirect;     # 302 临时
rewrite ^/a/(.*)$   /b/$1  last;          # 内部重写，重新走 location 匹配
rewrite ^/a/(.*)$   /b/$1  break;         # 内部重写，停止 rewrite，不重匹配 location

# return 比 rewrite 更高效，能用就用 return
return 301 https://$host$request_uri;                    # 全站跳 HTTPS
return 308 $1$is_args$args;                              # 去尾斜杠（保留方法）
return 200 '{"status":"ok"}';                            # 直接响应
```

| Flag | 行为 | 用途 |
|------|------|------|
| `last` | 重写后**重新**匹配 location | 需要命中另一个 location |
| `break` | 重写后停在当前 location | 只改 URI，不换处理逻辑 |
| `redirect` | 返回 302 | 临时跳转 |
| `permanent` | 返回 301 | 永久跳转（会被浏览器缓存！） |

### 6.5 变量速查

| 变量 | 含义 | 示例值 |
|------|------|-------|
| `$request_uri` | **原始**完整 URI（含 query，未解码） | `/api/users?id=1` |
| `$uri` | 规范化并解码后的 path（不含 query） | `/api/users` |
| `$args` / `$query_string` | 查询串 | `id=1` |
| `$is_args` | 有 args 时为 `?`，否则空 | `?` |
| `$arg_name` | 单个参数值 | `$arg_id` → `1` |
| `$host` | 请求的 Host（小写，去端口） | `example.com` |
| `$http_host` | 原始 Host 头（含端口） | `example.com:8080` |
| `$scheme` | 协议 | `https` |
| `$document_uri` | 同 `$uri` | — |

> ⚠️ `$uri` 已解码，用它做安全判断会被 `%2e%2e%2f` 绕过；做安全过滤请用 `$request_uri`。

---

## 7. Spring Cloud Gateway 路由

```yaml
spring:
  cloud:
    gateway:
      routes:
        # ① 剥掉一层前缀：/user-service/api/users → /api/users
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/user-service/**
          filters:
            - StripPrefix=1

        # ② 正则重写：/api/v1/orders/42 → /orders/42
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/v1/orders/**
          filters:
            - RewritePath=/api/v1/orders/(?<seg>.*), /orders/${seg}

        # ③ 统一加前缀：/users → /api/v2/users
        - id: legacy
          uri: http://legacy.internal:8080
          predicates:
            - Path=/users/**
          filters:
            - PrefixPath=/api/v2

        # ④ 多条件组合断言
        - id: gray-release
          uri: lb://user-service-v2
          predicates:
            - Path=/api/users/**
            - Header=X-Gray, true
            - Query=version, 2
            - Method=GET,POST
            - Weight=group-user, 20        # 20% 流量灰度

        # ⑤ 重定向
        - id: redirect-old
          uri: no://op
          predicates:
            - Path=/old/**
          filters:
            - RedirectTo=301, https://new.example.com
```

### 7.1 断言（Predicate）速查

| 断言 | 示例 | 说明 |
|------|------|------|
| `Path` | `Path=/api/**,/v2/**` | PathPattern 语法 |
| `Method` | `Method=GET,POST` | HTTP 方法 |
| `Header` | `Header=X-Token, \d+` | 头存在且匹配正则 |
| `Query` | `Query=name, tom.*` | 参数匹配 |
| `Cookie` | `Cookie=sid, \w+` | Cookie 匹配 |
| `Host` | `Host=**.example.com` | 域名匹配 |
| `RemoteAddr` | `RemoteAddr=10.0.0.0/16` | IP 段 |
| `After`/`Before`/`Between` | `After=2026-07-01T00:00:00+08:00[Asia/Shanghai]` | 时间窗（活动上线） |
| `Weight` | `Weight=group1, 8` | 权重灰度 |

### 7.2 过滤器（Filter）URL 相关速查

| 过滤器 | 作用 | 示例 |
|--------|------|------|
| `StripPrefix=n` | 去掉前 n 段 | `/a/b/c` → `/b/c`（n=1） |
| `PrefixPath=/x` | 加前缀 | `/a` → `/x/a` |
| `RewritePath=regex, repl` | 正则重写 | 见上文 |
| `SetPath=/api/{seg}` | 模板设置路径 | 配合 Path 断言捕获 |
| `RedirectTo=302, url` | 重定向 | — |
| `AddRequestParameter=k,v` | 加 Query 参数 | — |
| `RemoveRequestParameter=k` | 删 Query 参数 | 去掉 utm_* |
| `PreserveHostHeader` | 保留原 Host | 后端依赖 Host 时必加 |

> ⚠️ `StripPrefix` 与 `RewritePath` 不要同时对同一段生效，容易算错层数。**推荐统一用 `RewritePath`**，正则写清楚更可读、可测试。

---

## 8. 重定向状态码 301-308

| 码 | 名称 | 永久性 | 保留请求方法 | 可缓存 | 典型用途 |
|:---:|------|:---:|:---:|:---:|---------|
| **301** | Moved Permanently | ✅ 永久 | ❌ POST→GET | ✅ 强缓存 | 域名迁移、HTTP→HTTPS、SEO 权重转移 |
| **302** | Found | ❌ 临时 | ❌ POST→GET | ⚠️ 默认不缓存 | 临时跳转、A/B 测试 |
| **303** | See Other | ❌ 临时 | ✅ 强制转 GET | ❌ | POST 后跳详情页（PRG 模式） |
| **307** | Temporary Redirect | ❌ 临时 | ✅ **保留** | ❌ | 需要保留 POST + Body 的临时跳转 |
| **308** | Permanent Redirect | ✅ 永久 | ✅ **保留** | ✅ | 永久跳转且需保留 POST（API 迁移、去尾斜杠） |

### 8.1 选型决策树

```text
需要跳转？
├─ 永久变更（旧地址不再用）
│   ├─ 需要保留 POST/PUT/Body  →  308
│   └─ 只是页面跳转（GET）      →  301   ← SEO 首选
└─ 临时跳转
    ├─ 需要保留 POST/Body       →  307
    ├─ POST 后要变 GET（防重提） →  303   ← PRG 模式
    └─ 普通临时                 →  302
```

> ⚠️ **301 会被浏览器长期强缓存**（有的甚至永久，需清缓存才能恢复）。**未上线前测试跳转务必先用 302**，确认无误再改 301。线上误发 301 到错误地址是典型 P0 事故。

### 8.2 Spring 实现

```java
// ① RedirectView —— 默认 302
@GetMapping("/old")
public RedirectView old() {
    return new RedirectView("/new");                   // 302
}

// ② 显式 301
@GetMapping("/old2")
public RedirectView old2() {
    RedirectView v = new RedirectView("/new");
    v.setStatusCode(HttpStatus.MOVED_PERMANENTLY);     // 301
    return v;
}

// ③ ResponseEntity —— 最灵活，支持 307/308
@PostMapping("/api/v1/orders")
public ResponseEntity<Void> movedApi() {
    return ResponseEntity.status(HttpStatus.PERMANENT_REDIRECT)   // 308，保留 POST
            .location(URI.create("/api/v2/orders"))
            .build();
}

// ④ 字符串前缀（Controller 非 RestController）
@GetMapping("/login")
public String login() {
    return "redirect:/home";        // 302
    // return "forward:/home";      // 服务端转发，URL 不变，浏览器无感
}
```

### 8.3 `redirect` 与 `forward` 的区别

| 维度 | `redirect:`（重定向） | `forward:`（转发） |
|------|---------------------|-------------------|
| 请求次数 | 2 次 | 1 次 |
| 浏览器 URL | **改变** | **不变** |
| Request 域数据 | 丢失（需 `RedirectAttributes` 或 Flash） | 保留 |
| 能否跳外站 | ✅ | ❌ 仅站内 |
| 状态码 | 301/302/307/308 | 200 |

```java
// 重定向传参：URL 明文（可见）
@PostMapping("/save")
public String save(RedirectAttributes ra) {
    ra.addAttribute("id", 42);          // → /detail?id=42
    ra.addFlashAttribute("msg", "保存成功");  // → 存 Session，不进 URL，只读一次 ✅
    return "redirect:/detail";
}
```

---

## 9. CORS 与同源策略

### 9.1 同源定义：Scheme + Host + Port 三者全同

| 对比 `https://a.com:443/p1` | 是否同源 | 原因 |
|---------------------------|:---:|------|
| `https://a.com/p2` | ✅ | 443 是 https 默认端口 |
| `http://a.com/p1` | ❌ | scheme 不同 |
| `https://www.a.com/p1` | ❌ | host 不同（子域也算） |
| `https://a.com:8443/p1` | ❌ | port 不同 |
| `https://A.COM/p1` | ✅ | host 大小写不敏感 |

### 9.2 Spring CORS 配置

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // ⚠️ 带 Cookie 时不能用 "*"，要用 Patterns 或显式列举
                .allowedOriginPatterns("https://*.example.com", "http://localhost:[*]")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-Total-Count", "Location")   // 前端能读的响应头
                .allowCredentials(true)
                .maxAge(3600);                                  // 预检缓存 1h
    }
}
```

```java
// 单接口级别
@CrossOrigin(origins = "https://app.example.com", maxAge = 3600)
@GetMapping("/api/data")
public Data data() { ... }
```

### 9.3 关键响应头

```http
Access-Control-Allow-Origin: https://app.example.com
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization
Access-Control-Allow-Credentials: true
Access-Control-Expose-Headers: X-Total-Count, Location
Access-Control-Max-Age: 3600
Vary: Origin
```

### 9.4 CORS 五个高频坑

| # | 现象 | 原因 | 解法 |
|:---:|------|------|------|
| 1 | `Allow-Origin: *` + `withCredentials` 报错 | 规范禁止两者共存 | 回显具体 Origin + `Vary: Origin` |
| 2 | 前端读不到自定义响应头 | 未声明 `Expose-Headers` | 加上要暴露的头 |
| 3 | 每次请求都发 OPTIONS 预检 | `Max-Age` 未设或为 0 | 设 `maxAge(3600)` |
| 4 | Spring Security 下 CORS 失效 | Security Filter 在 CORS 之前拦了 OPTIONS | `http.cors(Customizer.withDefaults())` 并 `permitAll` OPTIONS |
| 5 | Nginx 与 Spring 都加了 CORS 头 | 出现两个 `Allow-Origin`，浏览器报错 | **只在一处配置** |

```java
// Spring Security 正确启用 CORS
@Bean
SecurityFilterChain chain(HttpSecurity http) throws Exception {
    http.cors(Customizer.withDefaults())            // ✅ 必须显式开启
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(a -> a
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()   // 放行预检
            .anyRequest().authenticated());
    return http.build();
}
```

> 🎯 **核心要点**：CORS 是**浏览器**的限制，不是服务端安全机制。服务器之间调用、Postman、curl 都不受 CORS 约束——**别把 CORS 当鉴权用**。

---

> 🎯 **本篇核心要点**
> 1. Spring Boot 3 默认 `PathPattern`，`**` 只能在末尾，**尾斜杠不再匹配**（升级必查）
> 2. Security 与 MVC 的路径模式必须**完全一致**，否则出权限绕过
> 3. Nginx `proxy_pass` 末尾**有 `/` 剥前缀，无 `/` 原样透传**
> 4. LLM 流式接口的 Nginx 必须 `proxy_buffering off` + 调大 `proxy_read_timeout`
> 5. 重定向选码：永久且保留方法 **308**、SEO 用 **301**、临时保留方法 **307**、PRG 用 **303**
> 6. 301 会被浏览器强缓存，**上线前先用 302 验证**

---

**上一模块**：[03-URL协议族Scheme全解析.md](03-URL协议族Scheme全解析.md) / **下一模块**：[05-URL解析编程实战-Java与JS.md](05-URL解析编程实战-Java与JS.md)
