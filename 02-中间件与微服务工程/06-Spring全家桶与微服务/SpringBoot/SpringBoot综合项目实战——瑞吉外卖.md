# SpringBoot 综合项目实战——瑞吉外卖

> **定位**：SpringBoot + MyBatis-Plus + Vue 前后端分离实战项目，模拟外卖平台核心业务（菜品管理、订单处理、用户操作）。

---

## 目录

1. [项目概述](#1-项目概述)
2. [环境搭建](#2-环境搭建)
3. [核心功能实战](#3-核心功能实战)
4. [优化与部署](#4-优化与部署)

---

## 1. 项目概述

### 1.1 技术栈

| 层级 | 技术 |
|------|------|
| **后端** | Spring Boot 2.7.x + MyBatis-Plus 3.5.x + MySQL 8.0 + Spring Security |
| **前端管理端** | Vue 2 + Element UI + Axios + Vue Router + Vuex |
| **前端客户端** | Vue 2 + Vant UI + Axios |
| **工具** | Lombok + Swagger/Knife4j + 全局异常处理 |

### 1.2 核心功能模块

| 模块 | 后台管理系统（商家端） | 前台用户系统（客户端） |
|------|----------------------|----------------------|
| 人员 | 员工管理（增删改查+权限） | 用户登录/注册 |
| 菜品 | 菜品管理+图片上传+分类 | 菜品/套餐浏览+搜索 |
| 套餐 | 套餐管理（关联菜品） | — |
| 订单 | 订单查询+状态修改 | 购物车+订单生成+订单历史 |

### 1.3 数据库表

| 表 | 用途 |
|----|------|
| `employee` | 员工（id/username/password/phone/status） |
| `category` | 分类（菜品分类/套餐分类） |
| `dish` | 菜品 |
| `setmeal` / `setmeal_dish` | 套餐+关联菜品 |
| `user` | 客户端用户 |
| `shopping_cart` | 购物车 |
| `orders` | 订单 |

---

## 2. 环境搭建

### 2.1 依赖

```xml
<!-- Spring Web + MyBatis-Plus + MySQL + Lombok + Spring Security -->
```

### 2.2 配置

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/ruiji_takeout?serverTimezone=Asia/Shanghai
    username: root
    password: 123456
  servlet:
    multipart:
      max-file-size: 10MB

mybatis-plus:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.ruiji.entity
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto
```

### 2.3 前端初始化

```bash
# 管理端
vue create ruiji-admin
npm i element-ui axios -S

# 客户端
vue create ruiji-client
npm i vant axios -S
```

---

## 3. 核心功能实战

### 3.1 员工管理

**实体类（MyBatis-Plus 自动填充）**：

```java
@Data
public class Employee {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String username;
    private String password;
    private Integer status;          // 1=启用, 0=禁用

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

**自动填充**：

```java
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        metaObject.setValue("createTime", LocalDateTime.now());
        metaObject.setValue("updateTime", LocalDateTime.now());
    }
    @Override
    public void updateFill(MetaObject metaObject) {
        metaObject.setValue("updateTime", LocalDateTime.now());
    }
}
```

**登录（Spring Security + BCrypt）**：

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

@PostMapping("/login")
public R<Employee> login(@RequestBody Employee employee) {
    // ① 根据 username 查询
    Employee emp = employeeService.getOne(
        new LambdaQueryWrapper<Employee>()
            .eq(Employee::getUsername, employee.getUsername()));
    // ② 校验是否存在 + 密码 + 状态
    if (emp == null) return R.error("用户名不存在");
    if (!passwordEncoder.matches(employee.getPassword(), emp.getPassword()))
        return R.error("密码错误");
    if (emp.getStatus() == 0) return R.error("员工已禁用");
    return R.success(emp);
}
```

**分页查询（MyBatis-Plus 分页插件）**：

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    return interceptor;
}

@GetMapping("/page")
public R<Page<Employee>> page(int page, int pageSize, String name) {
    Page<Employee> pageInfo = new Page<>(page, pageSize);
    LambdaQueryWrapper<Employee> qw = new LambdaQueryWrapper<>();
    qw.like(StringUtils.isNotBlank(name), Employee::getName, name);
    employeeService.page(pageInfo, qw);
    return R.success(pageInfo);
}
```

### 3.2 菜品管理（图片上传）

```java
@PostMapping("/upload")
public R<String> upload(MultipartFile file) {
    String fileName = UUID.randomUUID() + file.getOriginalFilename()
            .substring(file.getOriginalFilename().lastIndexOf("."));
    file.transferTo(new File(BASE_PATH + "/" + fileName));
    return R.success(fileName);
}

// 静态资源映射
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:D:/ruiji/takeout/images/");
    }
}
```

### 3.3 购物车与订单

**加入购物车**：

```java
@PostMapping("/add")
public R<ShoppingCart> add(@RequestBody ShoppingCart cart, HttpSession session) {
    Long userId = (Long) session.getAttribute("userId");
    // ① 查询是否已有该菜品/套餐
    ShoppingCart exist = shoppingCartService.getOne(
        new LambdaQueryWrapper<ShoppingCart>()
            .eq(ShoppingCart::getUserId, userId)
            .eq(cart.getDishId() != null, ShoppingCart::getDishId, cart.getDishId()));
    // ② 已有→数量+1，无→新增
    if (exist != null) {
        exist.setDishNum(exist.getDishNum() + 1);
        shoppingCartService.updateById(exist);
    } else {
        shoppingCartService.save(cart);
    }
    return R.success(cart);
}
```

**订单生成**：

```java
@PostMapping("/submit")
public R<String> submit(@RequestBody Orders orders, HttpSession session) {
    Long userId = (Long) session.getAttribute("userId");
    orders.setUserId(userId);
    orders.setNumber(System.currentTimeMillis() + "" + new Random().nextInt(1000));
    // 计算总金额 → 保存订单 → 清空购物车
    orderService.save(orders);
    shoppingCartService.remove(new LambdaQueryWrapper<ShoppingCart>()
        .eq(ShoppingCart::getUserId, userId));
    return R.success("订单提交成功");
}
```

---

## 4. 优化与部署

### 优化方向

| 优化项 | 方案 |
|--------|------|
| Token 认证 | JWT 替代 Session |
| 缓存 | Redis 缓存热门菜品/套餐 |
| 异常处理 | `@RestControllerAdvice` 全局异常 |
| 参数校验 | `@NotBlank` / `@NotNull` JSR380 |
| 文件存储 | 本地→OSS（阿里云/腾讯云） |

### 部署

| 步骤 | 操作 |
|:----:|------|
| 1 | `mvn clean package` → 生成 jar |
| 2 | 上传服务器 → `nohup java -jar xxx.jar &` |
| 3 | 前端 `npm run build` → dist |
| 4 | Nginx 配置反向代理 → 启动 |
