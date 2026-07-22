# JavaWeb开发200集 面试宝典
> 基于黑马2024版JavaWeb全栈课程，覆盖前端基础、SpringBoot、MySQL、MyBatis、AOP、Vue工程化、Linux运维与Docker容器化部署，提炼面试高频考点

## 目录
1. [一、基础概念速答](#一基础概念速答)
2. [二、深度原理剖析](#二深度原理剖析)
3. [三、实战场景题](#三实战场景题)
4. [四、手写代码题](#四手写代码题)
5. [五、系统设计题](#五系统设计题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板)
8. [八、快速查漏补缺Checklist](#八快速查漏补缺checklist)

---

## 一、基础概念速答

### 1.1 HTML 语义化标签有哪些？作用是什么？
> 语义化标签如 `<header>`、`<nav>`、`<article>`、`<section>`、`<footer>`、`<aside>` 等，使页面结构更清晰，有利于 SEO 和可访问性。

### 1.2 CSS 选择器优先级如何计算？
| 选择器类型 | 权重值 | 示例 |
|-----------|--------|------|
| !important | 最高 | `color: red !important;` |
| 内联样式 | 1000 | `style="color:red"` |
| ID 选择器 | 100 | `#app` |
| 类/属性/伪类选择器 | 10 | `.container`、`[type]`、`:hover` |
| 元素/伪元素选择器 | 1 | `div`、`::before` |

> 💡 **面试常考**：`div.container#main` 的权重 = 1 + 10 + 100 = 111。`!important` 优先于一切，应谨慎使用。

### 1.3 JavaScript 中 `==` 与 `===` 的区别？
- `==`：先做类型转换再比较值（如 `"5" == 5` 返回 `true`）。
- `===`：严格相等，类型和值都必须相同（`"5" === 5` 返回 `false`）。
- **推荐始终使用 `===`**，避免隐式类型转换带来的意外结果。

### 1.4 Vue 常用指令及其用途？
| 指令 | 用途 | 示例 |
|------|------|------|
| `v-bind` | 动态绑定属性 | `v-bind:src="url"` 简写 `:src="url"` |
| `v-if` | 条件渲染（销毁/重建DOM） | `v-if="isShow"` |
| `v-show` | 条件渲染（切换 display） | `v-show="isShow"` |
| `v-for` | 列表渲染 | `v-for="(item, i) in list" :key="i"` |
| `v-model` | 双向数据绑定 | `v-model="username"` |
| `v-on` | 事件绑定 | `v-on:click="handle"` 简写 `@click="handle"` |

> 💡 **高频追问**：`v-if` vs `v-show` 区别？`v-if` 是惰性的（条件为假时 DOM 不存在），适用于切换频率低的场景；`v-show` 仅切换 CSS 的 `display`，适用于高频切换。

### 1.5 Maven 的三大生命周期是什么？
| 生命周期 | 包含阶段 | 说明 |
|---------|---------|------|
| **clean** | pre-clean → clean → post-clean | 清理构建产物 |
| **default** | compile → test → package → install → deploy | 核心构建流程 |
| **site** | pre-site → site → post-site → site-deploy | 生成项目站点文档 |

> 💡 **面试常问**：`mvn clean install` 执行了 clean 生命周期的 `clean` 阶段，然后执行 default 生命周期直到 `install`。

### 1.6 Maven 依赖 scope 有哪些？
| scope | 编译期 | 运行期 | 示例 |
|-------|--------|--------|------|
| `compile`（默认） | Y | Y | `spring-core` |
| `provided` | Y | N（容器提供） | `servlet-api` |
| `runtime` | N | Y | `mysql-connector-java` |
| `test` | N | N（仅测试） | `junit` |
| `system` | Y | Y（需指定 path） | 一般不使用 |

### 1.7 Spring Boot 起步依赖（Starter）的原理？
- Spring Boot 将一组常用依赖打包成一个 starter，如 `spring-boot-starter-web` 包含了 Spring MVC、嵌入式 Tomcat、Jackson 等。
- 核心机制是 **Maven 的依赖传递**，starter 的 `pom.xml` 中声明了若干依赖，引入 starter 后这些依赖会自动传递进来。
- 同时通过 `@EnableAutoConfiguration` 自动配置这些组件。

### 1.8 HTTP 常见状态码？
| 状态码 | 含义 | 说明 |
|--------|------|------|
| 200 | OK | 请求成功 |
| 201 | Created | 资源创建成功（POST/PUT） |
| 301 | Moved Permanently | 永久重定向 |
| 302 | Found | 临时重定向 |
| 400 | Bad Request | 客户端请求参数错误 |
| 401 | Unauthorized | 未认证（未登录） |
| 403 | Forbidden | 无权限访问 |
| 404 | Not Found | 资源不存在 |
| 405 | Method Not Allowed | 请求方法不支持 |
| 500 | Internal Server Error | 服务器内部错误 |
| 502 | Bad Gateway | 网关错误 |
| 503 | Service Unavailable | 服务不可用 |

### 1.9 RESTful API 设计规范要点？
- 使用名词复数表示资源：`/api/users`、`/api/orders`
- 通过 HTTP 方法表达操作：GET（查询）、POST（新增）、PUT（全量更新）、PATCH（部分更新）、DELETE（删除）
- 使用路径参数定位资源：`/api/users/{id}`
- 查询参数用于过滤/排序：`/api/users?page=1&size=10&sort=name`
- 返回合适的状态码和统一响应体

### 1.10 Three-Tier Architecture（三层架构）各层职责？
| 层次 | 组件 | 职责 |
|------|------|------|
| **Controller**（表现层） | `@RestController` | 接收请求、参数校验、响应返回 |
| **Service**（业务逻辑层） | `@Service` | 核心业务逻辑、事务管理 |
| **DAO**（数据访问层） | `@Repository` / `@Mapper` | 数据库 CRUD 操作 |

### 1.11 MySQL DDL、DML、DQL、DCL 的区别？
| 分类 | 全称 | 常用命令 |
|------|------|---------|
| DDL | Data Definition Language | `CREATE`、`ALTER`、`DROP`、`TRUNCATE` |
| DML | Data Manipulation Language | `INSERT`、`UPDATE`、`DELETE` |
| DQL | Data Query Language | `SELECT` |
| DCL | Data Control Language | `GRANT`、`REVOKE` |

### 1.12 MySQL JOIN 类型及区别？
| JOIN 类型 | 返回结果 |
|-----------|---------|
| `INNER JOIN` | 两表匹配的数据 |
| `LEFT JOIN` | 左表全部 + 右表匹配的数据（不匹配为 NULL） |
| `RIGHT JOIN` | 右表全部 + 左表匹配的数据（不匹配为 NULL） |
| `FULL OUTER JOIN` | 两表全部数据（MySQL 不支持，用 UNION 模拟） |

### 1.13 MyBatis `#{}` 和 `${}` 的区别？
| 特性 | `#{}` | `${}` |
|------|-------|-------|
| 处理方式 | 预编译，生成 `?` 占位符 | 字符串拼接，直接替换 |
| SQL 注入 | **安全** | **存在注入风险** |
| 适用场景 | 传参值（大多数情况） | 表名、列名等动态 SQL 片段 |
| 示例 | `WHERE id = #{id}` | `ORDER BY ${column}` |

### 1.14 Logback 日志级别及优先级？
`TRACE` < `DEBUG` < `INFO` < `WARN` < `ERROR`

> 💡 **常见配置**：开发环境用 DEBUG 级别，生产环境用 INFO 级别。`<root level="INFO">` 设置全局级别。

### 1.15 JWT 的组成部分及过期处理？
- **Header**：`{"alg": "HS256", "typ": "JWT"}`，声明算法和类型
- **Payload**：携带用户信息（如 userId、username），**不要放敏感信息**
- **Signature**：`HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)`，防篡改
- 过期处理：服务端校验 `exp` 字段，过期返回 401，前端跳转登录页

### 1.16 Filter 与 Interceptor 的区别？
| 维度 | Filter | Interceptor |
|------|--------|-------------|
| 规范 | Servlet 规范 | Spring 框架 |
| 作用范围 | 所有 Web 资源（包括静态资源） | 仅 Spring 管理的 Controller |
| 生命周期 | 容器启动创建 | Spring 容器初始化后创建 |
| 获取 IOC Bean | 较麻烦 | 可直接注入 |
| 执行顺序 | 先执行 Filter | 后执行 Interceptor |

### 1.17 Docker 核心概念？
| 概念 | 说明 |
|------|------|
| **镜像（Image）** | 只读模板，包含完整的运行环境 |
| **容器（Container）** | 镜像的运行实例，可读可写 |
| **仓库（Repository）** | 存储和分发镜像（如 Docker Hub） |
| **Dockerfile** | 构建镜像的指令文件 |
| **数据卷（Volume）** | 持久化容器数据 |
| **Docker Compose** | 编排多容器应用 |

### 1.18 Spring AOP 的五种通知类型？
| 通知类型 | 注解 | 执行时机 |
|----------|------|---------|
| 前置通知 | `@Before` | 目标方法执行前 |
| 后置通知 | `@After` | 目标方法执行后（无论是否异常） |
| 返回通知 | `@AfterReturning` | 目标方法正常返回后 |
| 异常通知 | `@AfterThrowing` | 目标方法抛出异常后 |
| 环绕通知 | `@Around` | 方法执行前后自定义增强（最强） |

### 1.19 事务的四大特性（ACID）？
| 特性 | 说明 |
|------|------|
| **A**tomicity（原子性） | 事务中的操作要么全部成功，要么全部回滚 |
| **C**onsistency（一致性） | 事务前后数据完整性约束保持一致 |
| **I**solation（隔离性） | 并发事务之间互不干扰 |
| **D**urability（持久性） | 提交后数据永久保存 |

### 1.20 Docker Compose 的作用？
> 通过一个 `docker-compose.yml` 文件，定义和运行多个 Docker 容器（如应用服务 + MySQL + Redis），一条命令 `docker-compose up -d` 即可启动整个服务栈。

---

## 二、深度原理剖析

### 2.1 Spring Boot 自动配置原理（@EnableAutoConfiguration）
Spring Boot 通过 `@EnableAutoConfiguration` 注解实现自动配置，核心流程：

```java
// 1. 入口注解组合
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

// 2. @SpringBootApplication 组合了三个注解：
// @SpringBootConfiguration == @Configuration
// @EnableAutoConfiguration  — 自动配置核心
// @ComponentScan             — 组件扫描
```

自动配置详细流程：
1. `@EnableAutoConfiguration` 通过 `@Import(AutoConfigurationImportSelector.class)` 导入选择器
2. `AutoConfigurationImportSelector` 扫描 `spring.factories` 文件（路径：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`）
3. 文件中列出了所有自动配置类，如 `DataSourceAutoConfiguration`、`WebMvcAutoConfiguration`
4. 每个自动配置类上使用 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解，判断是否生效
5. 生效后，自动配置类会创建对应的 Bean 到 Spring 容器中

> 💡 **常见面试题**：如何排除某个自动配置？使用 `@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)` 或者在配置文件中设置 `spring.autoconfigure.exclude`。

### 2.2 IOC（控制反转）与 DI（依赖注入）原理
- **IOC 思想**：对象的创建权从程序本身反转给 Spring 容器管理
- **DI 实现**：容器在创建对象时，自动将依赖的属性通过构造器、Setter 或字段注入方式赋值

```java
// 注入方式对比
@Service
public class UserServiceImpl implements UserService {

    // 方式一：字段注入（最常用，但官方不推荐）
    @Autowired
    private UserMapper userMapper;

    // 方式二：构造器注入（推荐，保证不可变性和测试便利性）
    private final UserMapper userMapper;
    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    // 方式三：Setter 注入（可选依赖使用）
    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }
}
```

> 💡 **Spring 官方推荐构造器注入**：依赖不可变（`final`）、不为 null、便于单元测试。

### 2.3 AOP 底层代理机制（JDK Proxy vs CGLIB）
| 对比维度 | JDK 动态代理 | CGLIB 代理 |
|---------|-------------|------------|
| 原理 | 基于接口，生成实现类 | 基于继承，生成子类 |
| 要求 | 目标类必须实现接口 | 目标类不能是 final |
| 性能（创建阶段） | 较快 | 较慢 |
| 性能（调用阶段） | 略慢 | 略快 |
| Spring Boot 默认 | 有接口时用 JDK | 无接口或用 CGLIB |

> 💡 **Spring Boot 2.x+ 默认使用 CGLIB**，即使目标类有接口：`spring.aop.proxy-target-class=true`（默认值）。

```java
// JDK 动态代理核心代码
public class JdkProxyFactory implements InvocationHandler {
    private Object target;

    public Object createProxy(Object target) {
        this.target = target;
        return Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            target.getClass().getInterfaces(),
            this
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("前置增强...");
        Object result = method.invoke(target, args);
        System.out.println("后置增强...");
        return result;
    }
}
```

### 2.4 @Transactional 原理与失效场景
**原理**：Spring 通过 AOP 为 `@Transactional` 标注的类/方法生成代理对象，在方法调用前后通过 `TransactionInterceptor` 开启/提交/回滚事务。

**常用参数**：
```java
@Transactional(
    propagation = Propagation.REQUIRED,  // 事务传播行为（默认）
    isolation = Isolation.READ_COMMITTED, // 隔离级别
    timeout = 30,                         // 超时时间（秒）
    rollbackFor = Exception.class,        // 指定哪些异常回滚
    readOnly = false                      // 是否只读
)
```

**5 种 Propagation 传播行为**：
| 传播行为 | 说明 |
|---------|------|
| `REQUIRED`（默认） | 支持当前事务，不存在则新建 |
| `REQUIRES_NEW` | 挂起当前事务，新建事务 |
| `SUPPORTS` | 支持当前事务，不存在则以非事务执行 |
| `NOT_SUPPORTED` | 以非事务方式执行，挂起当前事务 |
| `MANDATORY` | 必须在事务中执行，否则抛异常 |
| `NEVER` | 必须在非事务中执行，否则抛异常 |
| `NESTED` | 嵌套事务（JDBC 的 Savepoint） |

**常见失效场景**：
1. **同类调用**：`save()` 中直接调用 `update()`，AOP 代理不生效
   ```java
   // 失效：方法内部调用，不走代理
   public void save() {
       this.update(); // @Transactional 不生效！
   }
   // 解决1：注入自身代理
   @Autowired
   private UserService userService;
   public void save() {
       userService.update();
   }
   // 解决2：AopContext.currentProxy()
   ((UserService) AopContext.currentProxy()).update();
   ```
2. **`try-catch` 吞掉异常**：手动捕获异常未抛出，Spring 无法感知
3. **非 `public` 方法**：`@Transactional` 只对 `public` 方法生效
4. **异常类型不对**：默认只回滚 `RuntimeException`，`Exception` 下的非运行时异常需指定 `rollbackFor`

### 2.5 MyBatis 的缓存机制
| 缓存级别 | 作用域 | 说明 |
|---------|--------|------|
| **一级缓存**（SqlSession） | 同一个 SqlSession | 默认开启，无法关闭；在同一个会话中查询相同 SQL 返回缓存结果；执行 `update/delete/insert` 或 `commit/close/clearCache` 后清空 |
| **二级缓存**（Mapper namespace） | 跨 SqlSession | 默认关闭，需 `cacheEnabled=true`；映射文件中 `<cache/>` 开启；序列化对象；适用查询多、修改少的场景 |

### 2.6 MyBatis 中 `resultType` 与 `resultMap` 的区别？
| 对比 | resultType | resultMap |
|------|-----------|-----------|
| 映射方式 | 自动将列名映射为同名属性 | 手动定义字段与属性的映射关系 |
| 适用场景 | 列名与属性名一致 | 列名不一致、多表关联、复杂映射 |
| 示例 | `resultType="com.User"` | `<resultMap id="userMap" type="com.User">` |

> 💡 **建议**：企业项目中统一使用 `resultMap`，便于维护和扩展。

### 2.7 Spring Boot 配置优先级
从高到低：
1. **命令行参数**：`--server.port=8081`
2. **JNDI 属性**
3. **系统环境变量**：`SERVER_PORT=8081`
4. **OS 环境变量**
5. **`application-{profile}.properties`**：多环境配置
6. **`application.properties` / `application.yml`**
7. `@PropertySource` 指定的配置文件
8. Spring Boot 默认配置

> 🎯 **总结**：命令行参数 > 环境变量 > profile 配置 > 默认配置。后加载的覆盖先加载的。

### 2.8 HTTP 请求/响应协议结构
**请求协议**：
```
POST /api/users HTTP/1.1          -- 请求行（方法 + URL + 协议版本）
Host: localhost:8080              -- 请求头（键值对）
Content-Type: application/json
User-Agent: Mozilla/5.0
                                  -- 空行分隔
{"name": "张三", "age": 18}       -- 请求体（GET 无请求体）
```

**响应协议**：
```
HTTP/1.1 200 OK                   -- 状态行（协议版本 + 状态码 + 原因短语）
Content-Type: application/json    -- 响应头
Set-Cookie: token=abc123
                                  -- 空行分隔
{"id": 1, "name": "张三"}         -- 响应体
```

### 2.9 @SpringBootApplication 注解组合解析
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootConfiguration          // == @Configuration，标记为配置类
@EnableAutoConfiguration          // 开启自动配置
@ComponentScan(excludeFilters = { // 组件扫描
    @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
    @Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class) })
public @interface SpringBootApplication {
    // ...
}
```

### 2.10 @Value 与 @ConfigurationProperties 对比
| 对比维度 | @Value | @ConfigurationProperties |
|---------|--------|------------------------|
| 绑定方式 | 单个字段注入 | 批量绑定到 POJO |
| 类型转换 | 需手动转换 | 自动类型转换（支持 List、Map 等） |
| JSR-303 校验 | 不支持 | 支持 `@Validated` + `@NotNull` 等 |
| 复杂类型 | 不支持（如 List、Map） | 原生支持 |
| 使用场景 | 少量配置项 | 一组相关配置项 |

```java
// @Value 方式
@Component
public class OssConfig {
    @Value("${aliyun.oss.endpoint}")
    private String endpoint;
}

// @ConfigurationProperties 方式（推荐）
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
@Data
public class OssProperties {
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
}
```

### 2.11 分页查询原理
1. **物理分页**（推荐）：SQL 层使用 `LIMIT ?, ?` 限制结果集，每次只查当前页数据
2. **逻辑分页**：一次性查出全部数据，在内存中截取当前页（大数据量性能差）

```sql
-- 物理分页：第2页，每页10条
SELECT * FROM emp LIMIT 10, 10;  -- 从第10条开始查10条
-- 等价于 LIMIT 10 OFFSET 10;
```

```java
// 使用 PageHelper 分页插件
@Service
public class EmpServiceImpl implements EmpService {
    @Autowired
    private EmpMapper empMapper;

    public PageResult<Emp> page(Integer page, Integer pageSize, String name) {
        // 1. 设置分页参数（ThreadLocal 存储，MyBatis 拦截器自动拼接 LIMIT）
        PageHelper.startPage(page, pageSize);
        // 2. 执行查询（返回结果自动封装为 Page 对象）
        List<Emp> list = empMapper.list(name);
        PageInfo<Emp> pageInfo = new PageInfo<>(list);
        // 3. 封装统一返回
        return new PageResult<>(pageInfo.getTotal(), pageInfo.getList());
    }
}
```

> ⚠️ **PageHelper 注意事项**：一个线程中只能有一个 `PageHelper.startPage` 生效，必须在 MyBatis 查询方法之前调用，且不能被跳过。

### 2.12 spring.factories 自动配置加载机制
Spring Boot 的 `AutoConfiguration.imports` 文件（Spring Boot 2.7+）或 `spring.factories`（旧版本）中注册了所有自动配置类，Spring 通过 `SpringFactoriesLoader.loadFactoryNames()` 读取这些类名，然后按 `@AutoConfigureOrder` 和 `@AutoConfigureAfter` / `@AutoConfigureBefore` 确定顺序，逐个按条件配置。

```java
// 自定义 starter 的 AutoConfiguration.imports 文件内容
// META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.example.myapp.RedisAutoConfiguration
com.example.myapp.MailAutoConfiguration
```

---

## 三、实战场景题

### 场景 1：实现文件上传到阿里云 OSS
**需求**：用户上传头像图片，保存到阿里云 OSS，返回可访问的 URL。

**关键代码**：
```java
// 1. 配置类
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
@Data
public class OssProperties {
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
}

// 2. 工具类
@Component
public class OssFileUtil {
    @Autowired
    private OssProperties ossProperties;

    public String upload(MultipartFile file) throws IOException {
        // 创建 OSS 客户端
        OSS ossClient = new OSSClientBuilder().build(
            ossProperties.getEndpoint(),
            ossProperties.getAccessKeyId(),
            ossProperties.getAccessKeySecret()
        );
        // 生成唯一文件名（防止覆盖）
        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = UUID.randomUUID() + ext;
        // 上传
        ossClient.putObject(ossProperties.getBucketName(), newFileName,
            new ByteArrayInputStream(file.getBytes()));
        // 拼接 URL
        String url = "https://" + ossProperties.getBucketName() + "."
            + ossProperties.getEndpoint() + "/" + newFileName;
        ossClient.shutdown();
        return url;
    }
}
```

### 场景 2：全局异常处理器
**需求**：统一捕获业务异常和系统异常，返回标准 JSON 格式，而非 500 页面。

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 处理自定义业务异常
    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e) {
        log.error("业务异常：{}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    // 处理参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        return Result.error(400, msg);
    }

    // 兜底异常
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("系统异常：", e);
        return Result.error(500, "服务器内部错误，请稍后重试");
    }
}
```

### 场景 3：登录认证 + JWT + Interceptor 完整流程
**需求**：用户登录成功后生成 JWT，后续每次请求在拦截器中校验令牌。

```java
// 1. 登录接口
@PostMapping("/login")
public Result login(@RequestBody LoginDTO dto) {
    Emp emp = empService.login(dto);
    if (emp == null) {
        return Result.error("用户名或密码错误");
    }
    // 生成 JWT（有效期 12 小时）
    Map<String, Object> claims = new HashMap<>();
    claims.put("id", emp.getId());
    claims.put("name", emp.getName());
    String token = JwtUtil.generateToken(claims, 12 * 60 * 60 * 1000L);
    return Result.success(token);
}

// 2. 拦截器校验令牌
@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("token");
        try {
            Claims claims = JwtUtil.parseToken(token);
            // 将用户信息存入 ThreadLocal（后续 AOP 等场景获取）
            UserContext.set(claims);
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            response.getWriter().write("NOT_LOGIN");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response, Object handler, Exception ex) {
        UserContext.remove(); // 防止内存泄漏
    }
}
```

### 场景 4：AOP 记录操作日志
**需求**：记录每个接口的访问人、操作时间、方法名、参数、耗时等信息。

```java
@Aspect
@Component
@Slf4j
public class LogAspect {
    @Around("@annotation(com.example.annotation.Log)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        // 获取当前登录用户（从 ThreadLocal）
        String operName = UserContext.getCurrentUser();
        // 获取方法的描述注解
        Log logAnno = ((MethodSignature) joinPoint.getSignature())
            .getMethod().getAnnotation(Log.class);
        String desc = logAnno.value();

        Object result = joinPoint.proceed();

        long cost = System.currentTimeMillis() - start;
        log.info("【操作日志】操作人: {}, 操作: {}, 耗时: {}ms, 参数: {}",
            operName, desc, cost, joinPoint.getArgs());
        return result;
    }
}
```

### 场景 5：批量删除与条件分页查询
```java
// Controller
@DeleteMapping("/{ids}")
public Result deleteByIds(@PathVariable List<Integer> ids) {
    empService.deleteByIds(ids);
    return Result.success();
}

// Service
@Override
@Transactional
public void deleteByIds(List<Integer> ids) {
    empMapper.deleteByIds(ids);
}

// Mapper
@Mapper
public interface EmpMapper {
    void deleteByIds(@Param("ids") List<Integer> ids);
}

<!-- XML -->
<delete id="deleteByIds">
    DELETE FROM emp WHERE id IN
    <foreach collection="ids" item="id" open="(" close=")" separator=",">
        #{id}
    </foreach>
</delete>
```

### 场景 6：动态 SQL 多条件查询
```xml
<select id="list" resultType="com.example.entity.Emp">
    SELECT * FROM emp
    <where>
        <if test="name != null and name != ''">
            AND name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="gender != null">
            AND gender = #{gender}
        </if>
        <if test="deptId != null">
            AND dept_id = #{deptId}
        </if>
    </where>
    ORDER BY create_time DESC
</select>
```

### 场景 7：Spring Boot 多环境配置
**需求**：开发/测试/生产环境使用不同的数据库、日志级别等配置。

```yaml
# application-dev.yml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/tlias_dev
logging:
  level:
    com.example: debug

# application-prod.yml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:mysql://prod-host:3306/tlias_prod
logging:
  level:
    com.example: warn
```

> 启动时指定环境：`java -jar app.jar --spring.profiles.active=prod`

### 场景 8：前后端分离下的跨域问题处理
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("http://localhost:5173")  // Vue 开发服务器
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
```

### 场景 9：员工报表统计（职位/性别分布）
```xml
<!-- 按职位统计 -->
<select id="countByJob" resultType="java.util.Map">
    SELECT
        CASE job
            WHEN 1 THEN '班主任'
            WHEN 2 THEN '讲师'
            WHEN 3 THEN '学工主管'
            WHEN 4 THEN '教研主管'
            ELSE '咨询师'
        END AS name,
        COUNT(*) AS value
    FROM emp
    GROUP BY job
</select>
```

### 场景 10：Docker 部署 Spring Boot 应用
```dockerfile
# Dockerfile
FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY target/tlias.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
```

```yaml
# docker-compose.yml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: tlias
    volumes:
      - ./mysql/data:/var/lib/mysql
    ports:
      - "3306:3306"

  app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - mysql
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/tlias
```

---

## 四、手写代码题

### 4.1 手写 JWT 生成与校验工具类
```java
public class JwtUtil {
    private static final String SECRET = "tlias-secret-key";
    private static final Algorithm ALGORITHM = Algorithm.HMAC256(SECRET);

    // 生成 JWT
    public static String generateToken(Map<String, Object> claims, long expireMs) {
        return JWT.create()
            .withClaim("claims", claims)
            .withExpiresAt(new Date(System.currentTimeMillis() + expireMs))
            .sign(ALGORITHM);
    }

    // 校验并解析 JWT
    public static Claims parseToken(String token) {
        return JWT.require(ALGORITHM)
            .build()
            .verify(token)
            .getClaim("claims")
            .as(Claims.class);
    }
}
```

### 4.2 手写 Spring Boot Starter（自定义 Redis Starter）
```java
// 1. 自动配置类
@AutoConfiguration
@ConditionalOnClass(RedisTemplate.class)
@EnableConfigurationProperties(RedisProperties.class)
public class RedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        // 设置 JSON 序列化
        GenericJackson2JsonRedisSerializer serializer =
            new GenericJackson2JsonRedisSerializer();
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        return template;
    }
}

// 2. 属性配置类
@ConfigurationProperties(prefix = "custom.redis")
@Data
public class RedisProperties {
    private String host = "localhost";
    private int port = 6379;
}
```

### 4.3 手写 MyBatis 动态 SQL（条件 + 分页）
```xml
<select id="selectEmpPage" resultMap="empResultMap">
    SELECT e.*, d.name dept_name
    FROM emp e
    LEFT JOIN dept d ON e.dept_id = d.id
    <where>
        <if test="name != null and name != ''">
            AND e.name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="gender != null">
            AND e.gender = #{gender}
        </if>
        <if test="begin != null">
            AND e.entry_date >= #{begin}
        </if>
        <if test="end != null">
            AND e.entry_date &lt;= #{end}
        </if>
    </where>
    ORDER BY e.create_time DESC
</select>
```

### 4.4 手写全局统一返回结果封装
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code; // 1: 成功, 0: 失败
    private String msg;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(1, "success", data);
    }

    public static <T> Result<T> success() {
        return new Result<>(1, "success", null);
    }

    public static <T> Result<T> error(String msg) {
        return new Result<>(0, msg, null);
    }

    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }
}
```

### 4.5 手写 ThreadLocal 存储当前用户工具类
```java
public class UserContext {
    private static final ThreadLocal<Claims> THREAD_LOCAL = new ThreadLocal<>();

    public static void set(Claims claims) {
        THREAD_LOCAL.set(claims);
    }

    public static Claims get() {
        return THREAD_LOCAL.get();
    }

    public static Integer getUserId() {
        Claims claims = THREAD_LOCAL.get();
        return claims != null ? claims.get("id", Integer.class) : null;
    }

    public static void remove() {
        THREAD_LOCAL.remove();
    }
}
```

### 4.6 手写 AOP 切入点表达式
```java
@Aspect
@Component
public class TimeAspect {

    // 1. execution 表达式：匹配所有 Service 实现类的所有方法
    @Around("execution(* com.example.service.impl.*.*(..))")
    public Object measureTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        Object result = joinPoint.proceed();
        long cost = System.nanoTime() - start;
        log.info("{} 耗时: {}ms",
            joinPoint.getSignature().getName(), cost / 1_000_000);
        return result;
    }

    // 2. @annotation 表达式：匹配自定义注解
    @Around("@annotation(com.example.annotation.Log)")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        // ...
        return joinPoint.proceed();
    }
}
```

### 4.7 手写 PageHelper 拦截器模拟（MyBatis 插件原理）
```java
@Intercepts({
    @Signature(type = Executor.class, method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class PageInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 1. 从 ThreadLocal 获取分页参数
        PageParam pageParam = PageContext.get();
        if (pageParam == null) {
            return invocation.proceed();
        }
        // 2. 修改 MappedStatement 的 SQL，拼接 LIMIT
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        String originalSql = ms.getBoundSql(invocation.getArgs()[1]).getSql();
        String pageSql = originalSql + " LIMIT " + pageParam.getOffset()
            + ", " + pageParam.getPageSize();
        // 3. 创建新的 MappedStatement 执行
        // ...
        return invocation.proceed();
    }
}
```

---

## 五、系统设计题

### 5.1 设计一个 RBAC 权限管理系统
**需求**：用户-角色-权限三级权限模型，支持接口级别的访问控制。

**表结构设计**：
```sql
CREATE TABLE sys_user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    status TINYINT DEFAULT 1 COMMENT '1:启用 0:禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_role (
    id INT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    role_key VARCHAR(50) NOT NULL UNIQUE COMMENT '角色标识'
);

CREATE TABLE sys_permission (
    id INT PRIMARY KEY AUTO_INCREMENT,
    perm_name VARCHAR(50) NOT NULL,
    perm_key VARCHAR(100) NOT NULL UNIQUE COMMENT '权限标识, 如 system:user:list',
    parent_id INT DEFAULT 0
);

-- 多对多关联表
CREATE TABLE sys_user_role (
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE sys_role_perm (
    role_id INT NOT NULL,
    perm_id INT NOT NULL,
    PRIMARY KEY (role_id, perm_id)
);
```

**权限校验**：通过 AOP + 自定义注解实现
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PreAuthorize {
    String value(); // 如 "system:user:list"
}

@Aspect
@Component
public class PermissionAspect {
    @Around("@annotation(preAuthorize)")
    public Object checkPermission(ProceedingJoinPoint joinPoint,
                                   PreAuthorize preAuthorize) throws Throwable {
        // 获取当前用户权限列表（从 Redis 缓存）
        Set<String> perms = getCurrentUserPermissions();
        if (!perms.contains(preAuthorize.value())) {
            throw new BusinessException(403, "无权限访问");
        }
        return joinPoint.proceed();
    }
}
```

### 5.2 设计一个高并发秒杀系统
**关键设计点**：

| 层次 | 方案 | 说明 |
|------|------|------|
| **前端** | 按钮置灰 + 验证码 | 防止用户重复提交 |
| **流量过滤** | Nginx 限流 + Sentinel | 限制 QPS，拒绝超量请求 |
| **库存扣减** | Redis Lua 脚本 | 原子操作，防止超卖 |
| **异步处理** | RabbitMQ 削峰填谷 | 订单异步落库 |
| **数据库** | 乐观锁 | `UPDATE SET stock = stock - 1 WHERE stock > 0` |

```java
// Redis Lua 脚本扣减库存（原子操作）
public class SeckillService {
    private static final String LUA_SCRIPT =
        "local stock = redis.call('get', KEYS[1]) " +
        "if stock and tonumber(stock) > 0 then " +
        "    redis.call('decr', KEYS[1]) " +
        "    return 1 " +
        "else " +
        "    return 0 " +
        "end";

    public boolean tryAcquire(String seckillKey) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, List.of(seckillKey));
        return result != null && result == 1;
    }
}
```

### 5.3 设计一个通用文件存储服务
**需求**：支持本地存储、阿里云 OSS、MinIO 多种存储方式，可动态切换。

```java
// 策略接口
public interface FileStorage {
    String upload(MultipartFile file);
    void delete(String fileUrl);
}

// 策略实现：阿里云 OSS
@Component("ossStorage")
public class OssFileStorage implements FileStorage { /* 实现 */ }

// 策略实现：本地存储
@Component("localStorage")
public class LocalFileStorage implements FileStorage { /* 实现 */ }

// 策略上下文 + 工厂
@Component
public class FileStorageContext {
    @Autowired
    private Map<String, FileStorage> storageMap;

    public FileStorage getStorage(String type) {
        FileStorage storage = storageMap.get(type + "Storage");
        if (storage == null) {
            throw new BusinessException("不支持的存储类型: " + type);
        }
        return storage;
    }
}

// 使用
@Service
public class FileService {
    @Autowired
    private FileStorageContext context;

    public String upload(String type, MultipartFile file) {
        return context.getStorage(type).upload(file);
    }
}
```

### 5.4 设计一个基于 AOP 的操作日志系统
**需求**：记录每次用户操作（增删改）的详细信息，支持日志归类查询。

```java
// 操作日志注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {
    String module();   // 模块名，如 "部门管理"
    String action();   // 操作类型，如 "新增"
    String desc();     // 描述模板，如 "新增部门：{name}"
}

// AOP 切面
@Aspect
@Component
public class OperationLogAspect {
    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint,
                         OperationLog operationLog) throws Throwable {
        // 操作前记录开始时间
        long start = System.currentTimeMillis();
        // 执行原方法
        Object result = joinPoint.proceed();
        long cost = System.currentTimeMillis() - start;

        // 构建日志实体
        OperationLogEntity logEntity = new OperationLogEntity();
        logEntity.setModule(operationLog.module());
        logEntity.setAction(operationLog.action());
        logEntity.setCostTime(cost);
        logEntity.setOperUser(UserContext.getUserId());
        logEntity.setOperTime(LocalDateTime.now());
        // 异步保存到数据库（防止影响主流程）
        CompletableFuture.runAsync(() -> logService.save(logEntity));

        return result;
    }
}
```

---

## 六、常见坑点与最佳实践

### 6.1 常见坑点汇总

| 坑点 | 错误示例 | 正确做法 | 原因 |
|------|---------|---------|------|
| MyBatis `#{}` 和 `${}` 混用 | `ORDER BY ${id}` 对用户输入未校验 | 使用白名单校验列名，或用 `#{}` 取参 | `${}` 存在 SQL 注入风险 |
| @Transactional 同类方法调用 | 方法 A 中 `this.methodB()` | 注入自身代理调用，或抽到另一个 Service | AOP 代理不拦截内部调用 |
| 服务层直接捕获异常未抛出 | `try { ... } catch (Exception e) { return error; }` | `catch` 中记录日志后抛出 `RuntimeException` | Spring 无法感知异常，事务不回滚 |
| 空指针未防御 | `user.getName()` 未判空 | 使用 `Optional` 或判空后操作 | 数据库查询可能返回 null |
| 文件上传内存溢出 | `file.getBytes()` 不限制文件大小 | `spring.servlet.multipart.max-file-size=10MB` 做限制 | 大文件直接加载到内存 |
| 敏感信息写在配置文件中 | 数据库密码直接写在 `application.properties` 中 | 使用 Jasypt 加密，或从环境变量读取 | 保护生产环境机密 |
| 未设置 JWT 过期时间 | `JWT.create().sign(algorithm)` | 始终设置 `withExpiresAt()` | Token 永不过期不安全 |
| Docker 容器时区问题 | 默认 UTC 时间 | `environment: TZ=Asia/Shanghai` | 日志时间与北京时间不符 |
| Vue 组件 key 未绑定 | `v-for` 未指定 `:key` | 使用唯一标识 `:key="item.id"` | 列表渲染性能差，状态错乱 |

### 6.2 最佳实践汇总

| 领域 | 最佳实践 | 说明 |
|------|---------|------|
| **包结构** | `com.xxx.controller` / `service` / `mapper` / `entity` / `dto` | 分层清晰，职责单一 |
| **参数校验** | `@Validated` + `@NotBlank`、`@NotNull` 等在 DTO 中声明 | 避免 Controller 中手动校验 |
| **统一异常处理** | `@RestControllerAdvice` + `@ExceptionHandler` | 全局统一异常返回格式 |
| **日志** | 使用 `@Slf4j`（Lombok），合理使用日志级别 | 生产环境 DEBUG 日志过多影响性能 |
| **配置文件** | YAML + 多环境 profile | YAML 可读性好，支持多文档块 |
| **接口设计** | RESTful + 统一返回 `Result<T>` | 前端调用约定一致 |
| **数据库** | 字段设置 `NOT NULL + DEFAULT`，索引合理 | 减少空指针，提高查询性能 |
| **事务** | 只在 Service 层加 `@Transactional` | Controller 层不应该处理事务 |
| **依赖注入** | 构造器注入（`final` + Lombok `@RequiredArgsConstructor`） | 保证不可变性和测试便利性 |
| **枚举** | 数据库状态字段使用数字 `TINYINT`，代码中定义枚举类 | 避免魔法数值 |

```java
// 构造器注入最佳实践
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final DeptService deptService;
    // 自动生成构造器，无需显式 @Autowired
}
```

---

## 七、面试回答模板

### 模板 1：谈 Spring Boot 自动配置
> **回答思路**：入口类 → 注解组合 → spring.factories → 条件注解 → 举例说明

"Spring Boot 自动配置的核心是 `@EnableAutoConfiguration` 注解。启动时，`AutoConfigurationImportSelector` 会扫描 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件中声明的所有自动配置类。每个自动配置类上有 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解，只有满足条件才会生效。例如 `DataSourceAutoConfiguration` 在 classpath 下存在 `DataSource.class` 且容器中没有自定义的 `DataSource` Bean 时，才会自动配置数据源。这样实现了'按需生效'的自动配置。"

### 模板 2：谈 IOC 和 DI
> **回答思路**：控制反转的概念 → 容器管理对象 → 依赖注入方式 → 优缺点

"IOC（控制反转）是一种设计思想，将对象的创建和管理权从程序代码转移到 Spring 容器。DI（依赖注入）是 IOC 的具体实现，容器在创建对象时自动将依赖注入进去。Spring 支持三种注入方式：构造器注入（推荐，依赖不可变且不为 null）、Setter 注入（可选依赖）和字段注入（简洁但不利于测试）。构造器注入配合 `final` 关键字能保证依赖不会被篡改，也便于编写单元测试。"

### 模板 3：谈 AOP 的使用场景
> **回答思路**：概念 → 场景举例 → 通知类型 → 底层原理

"AOP（面向切面编程）用于处理横切关注点，将日志记录、权限校验、事务管理、性能监控等与业务逻辑分离。Spring AOP 有五种通知类型：`@Before`、`@After`、`@AfterReturning`、`@AfterThrowing` 和 `@Around`。底层使用代理模式：有接口时默认用 JDK 动态代理（通过 `InvocationHandler` 实现），无接口时用 CGLIB（通过生成子类实现）。Spring Boot 2.x+ 默认使用 CGLIB。实际开发中我会用 `@Around` 配合自定义注解实现操作日志记录，或使用 `execution` 表达式对 Service 层统一做性能监控。"

### 模板 4：谈 JWT 认证方案
> **回答思路**：为什么用 JWT → 结构（三部分） → 登录流程 → 校验流程 → 痛点

"JWT 是 JSON Web Token 的缩写，是目前前后端分离项目中最主流的认证方案。它由 Header、Payload 和 Signature 三部分组成，通过 Base64URL 编码后用 `.` 拼接。JWT 的优势是服务端无需存储 session，天然支持分布式环境。登录流程是：用户输入凭证 → 服务端校验通过后生成 JWT（包含用户 ID、过期时间等）→ 返回给前端 → 前端存储在 localStorage 中并在每次请求时通过请求头 `Authorization: Bearer <token>` 携带。服务端通过拦截器解析和校验 JWT，从 Payload 中获取用户信息。痛点在于 JWT 一旦签发就无法在服务端主动使其失效，需要配合黑名单机制或短 Token + 长 Refresh Token 方案。"

### 模板 5：谈 MySQL 优化经验
> **回答思路**：索引优化 → SQL 优化 → 表设计 → 分库分表 → 查询缓存

"MySQL 优化从几个层面入手：（1）索引优化：为查询频繁的字段、排序字段、关联字段建立索引，但避免过多索引影响写入性能；使用 `EXPLAIN` 分析执行计划，关注 type、rows、Extra 等字段，避免全表扫描。（2）SQL 优化：避免 `SELECT *`，合理使用覆盖索引；`LIKE` 查询避免前置通配符 `%xxx`；使用分页查询避免大数据量一次性加载。（3）表设计：字段使用合适的数据类型（如状态用 `TINYINT` 不用 `VARCHAR`），字段设置 `NOT NULL`，避免过多的 NULL 值影响索引效率。（4）大表考虑分表（按时间或哈希分表）、读写分离（主库写、从库读）。"

---

## 八、快速查漏补缺 Checklist

### 8.1 前端基础
- [ ] HTML 语义化标签及其 SEO 意义
- [ ] CSS 盒模型（content-box vs border-box）
- [ ] CSS Flexbox / Grid 布局的基本使用
- [ ] JS ES6 语法：箭头函数、解构赋值、Promise、async/await
- [ ] Vue 生命周期（created、mounted、beforeDestroy 等）
- [ ] Vue Router 路由配置与导航守卫
- [ ] Axios 请求封装与拦截器
- [ ] Element Plus 常见组件的使用

### 8.2 Maven
- [ ] 三大生命周期及各阶段用途
- [ ] 依赖 scope（compile、provided、runtime、test）
- [ ] 依赖传递与冲突解决（`<exclusions>`、`<dependencyManagement>`）
- [ ] 继承（parent）与聚合（modules）的区别
- [ ] 私服（Nexus）配置与使用

### 8.3 Spring Boot
- [ ] @SpringBootApplication 组合注解含义
- [ ] 自动配置原理（AutoConfiguration.imports + 条件注解）
- [ ] 配置优先级（命令行 > 环境变量 > 配置文件）
- [ ] @ConfigurationProperties 与 @Value 的区别
- [ ] 自定义 Starter 的开发步骤
- [ ] 内嵌 Tomcat 原理（直接运行 .jar 启动 Web 服务）

### 8.4 HTTP / RESTful
- [ ] 常见状态码及其含义（200、201、301、302、400、401、403、404、500、502、503）
- [ ] 请求/响应协议的组成部分
- [ ] RESTful API 设计规范
- [ ] 常见 Content-Type：application/json、application/x-www-form-urlencoded、multipart/form-data

### 8.5 三层架构
- [ ] Controller / Service / DAO 各层职责
- [ ] VO / DTO / Entity 的区别与使用场景
- [ ] IOC 与 DI 的理解
- [ ] 构造器注入 vs 字段注入

### 8.6 MySQL
- [ ] DDL / DML / DQL / DCL 分类
- [ ] 内连接、外连接（LEFT/RIGHT）、子查询
- [ ] 聚合函数（COUNT、SUM、AVG、MAX、MIN）+ GROUP BY + HAVING
- [ ] 索引类型（B+Tree 索引、聚簇索引 vs 二级索引）
- [ ] 事务隔离级别（READ UNCOMMITTED、READ COMMITTED、REPEATABLE READ、SERIALIZABLE）
- [ ] 最左前缀原则
- [ ] SQL 优化工具：EXPLAIN、慢查询日志

### 8.7 MyBatis
- [ ] #{} vs ${} 的区别与使用场景
- [ ] resultType vs resultMap
- [ ] 动态 SQL（if、where、foreach、set、choose）
- [ ] MyBatis 缓存机制（一级缓存、二级缓存）
- [ ] MyBatis 与 JPA 的异同

### 8.8 事务
- [ ] ACID 四大特性
- [ ] 7 种传播行为及使用场景
- [ ] Spring @Transactional 失效的 4 种场景
- [ ] 隔离级别与并发问题（脏读、不可重复读、幻读）

### 8.9 日志
- [ ] Logback 日志级别及配置文件结构（`<root>`、`<logger>`、`<appender>`）
- [ ] 各环境推荐的日志级别
- [ ] Lombok @Slf4j 的使用

### 8.10 文件上传
- [ ] MultipartFile 的常用方法
- [ ] 本地存储与 OSS 存储的对比
- [ ] 阿里云 OSS / MinIO 的集成方式
- [ ] 文件上传大小限制配置

### 8.11 认证与授权
- [ ] Cookie vs Session vs Token 的区别
- [ ] JWT 的三部分结构与签名过程
- [ ] Filter 与 Interceptor 的执行顺序与区别
- [ ] ThreadLocal 存储用户信息

### 8.12 AOP
- [ ] 五种通知类型及 @Around 的 ProceedingJoinPoint 方法
- [ ] 切入点表达式（execution、@annotation、within 等）
- [ ] JDK 动态代理 vs CGLIB 的底层原理
- [ ] AOP 实践：日志记录、权限校验、性能监控

### 8.13 Spring Boot 原理
- [ ] Bean 的作用域（singleton、prototype、request、session）
- [ ] @Conditional 系列注解（@ConditionalOnClass、@ConditionalOnMissingBean、@ConditionalOnProperty）
- [ ] @Import 的几种用法
- [ ] 自定义 Starter 完整开发流程

### 8.14 Vue 工程化
- [ ] Vue 单文件组件（SFC）结构（template + script + style）
- [ ] Vue Router 嵌套路由与导航守卫
- [ ] Element Plus 表格、表单、对话框、分页组件的使用
- [ ] Axios 请求封装与响应拦截
- [ ] 打包部署：`npm run build` → nginx 部署

### 8.15 Linux
- [ ] 常用目录结构：/bin、/etc、/var、/usr/local、/opt
- [ ] 常用命令：ls、cd、pwd、cp、mv、rm、tar、grep、find、chmod、ps、top
- [ ] JDK 安装与环境变量配置
- [ ] Nginx 反向代理配置
- [ ] 项目部署流程：上传 jar → 启动脚本 → 日志查看 → 健康检查

### 8.16 Docker
- [ ] 镜像与容器的关系
- [ ] Dockerfile 常用指令：FROM、WORKDIR、COPY、RUN、EXPOSE、ENTRYPOINT
- [ ] 数据卷挂载（volume vs bind mount）
- [ ] 自定义镜像构建
- [ ] Docker Compose 编排多容器应用
- [ ] 常用命令：docker pull、run、ps、exec、logs、stop、rm

---

> 🎯 **面试建议**：JavaWeb 面试考察的是从页面到底层的全链路理解。前端重点在 Vue 指令和 Ajax 交互，后端核心在 Spring Boot 自动配置原理 + AOP + JWT 认证，数据库在于 MyBatis 动态 SQL 和事务管理，部署在于 Docker 容器化。建议对照 Checklist 逐项排查，弱势项目按"是什么 → 怎么用 → 为什么"的三段式准备。
