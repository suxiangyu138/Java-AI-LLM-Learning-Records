03.21 15:49
SpringBoot综合项目实战——瑞吉外卖
一、项目概述
1.1 项目背景
瑞吉外卖（Ruiji Takeout）是一款基于SpringBoot框架开发的综合性外卖管理系统，模拟真实外卖平台的核心业务，涵盖后台管理系统（商家端）和前台用户系统（客户端），实现从菜品管理、订单处理、用户操作到数据统计的全流程功能，是SpringBoot+MyBatis+Vue的经典实战案例，适合巩固SSM框架整合、前后端分离开发、业务逻辑拆解等核心技能。
项目核心定位：中小型餐饮企业的外卖管理解决方案，兼顾易用性和扩展性，支持菜品分类、套餐管理、订单跟踪、用户地址管理等核心需求，同时集成权限控制、数据分页、异常处理等企业级开发必备特性。
1.2 技术栈选型
本项目采用前后端分离架构，技术栈围绕SpringBoot生态展开，兼顾稳定性和开发效率，具体选型如下：
后端技术栈
核心框架：SpringBoot 2.7.x（简化配置，快速开发）
持久层：MyBatis-Plus 3.5.x（简化CRUD操作，支持分页、条件查询）
数据库：MySQL 8.0（存储业务数据，支持事务、索引优化）
权限控制：Spring Security（实现后台用户认证与授权）
工具类：Lombok（简化实体类代码）、Apache Commons Lang3（常用工具方法）
文件上传：SpringBoot内置MultipartFile（处理菜品图片、头像上传）
接口文档：Swagger/Knife4j（自动生成接口文档，方便前后端联调）
异常处理：全局异常处理器（统一处理业务异常、系统异常）
前端技术栈
核心框架：Vue 2.x（构建用户界面，双向数据绑定）
UI组件库：Element UI（后台管理系统）、Vant UI（移动端客户端）
路由管理：Vue Router（实现页面跳转、路由守卫）
状态管理：Vuex（管理全局状态，如用户登录信息、购物车数据）
请求工具：Axios（发送异步请求，与后端接口交互）
构建工具：npm/yarn（依赖管理、项目打包）
1.3 项目核心功能模块
项目分为两大模块，共8个核心功能，覆盖外卖业务全流程，具体如下：
模块类型
核心功能
功能描述
后台管理系统（商家端）
员工管理
员工新增、修改、删除、查询，权限分配（管理员/普通员工）

菜品管理
菜品新增（含图片上传）、修改、删除、分页查询，菜品状态控制

分类管理
菜品分类、套餐分类的新增、修改、删除、查询

套餐管理
套餐新增（关联菜品）、修改、删除、查询，套餐状态控制

订单管理
订单查询、订单状态修改（接单、派送、完成），订单详情查看
前台用户系统（客户端）
用户登录/注册
手机验证码登录、密码登录，用户信息修改、头像上传

菜品/套餐浏览
按分类浏览菜品、套餐，搜索菜品，查看菜品详情

购物车与订单
加入购物车、修改购物车数量、结算，查看订单历史、订单详情
二、项目环境搭建
2.1 开发环境准备
JDK：1.8及以上（推荐JDK11，兼容SpringBoot 2.7.x）
IDE：IntelliJ IDEA（推荐2021及以上版本，支持SpringBoot快速创建）
数据库：MySQL 8.0（需提前创建数据库，编码设为UTF-8）
前端工具：VS Code（编写Vue代码）、Node.js（npm包管理）
其他：Postman（接口测试）、Navicat（数据库管理）
2.2 后端项目初始化（SpringBoot）
步骤1：创建SpringBoot项目
通过IDEA的Spring Initializr创建项目，配置如下：
Group：com.ruiji（自定义，通常为公司域名反转）
Artifact：ruiji-takeout
Version：1.0.0
依赖选择：Spring Web、MyBatis-Plus、MySQL Driver、Lombok、Spring Security、Spring Boot DevTools（热部署，可选）
步骤2：配置数据库与MyBatis-Plus
在application.yml文件中配置数据库连接和MyBatis-Plus相关参数，示例如下：
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/ruiji_takeout?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true
    username: root
    password: 123456 # 自己的MySQL密码
  # 文件上传配置
  servlet:
    multipart:
      max-file-size: 10MB # 单个文件最大大小
      max-request-size: 100MB # 单次请求最大文件大小
# MyBatis-Plus配置
mybatis-plus:
  mapper-locations: classpath:mapper/**/*.xml # mapper.xml文件路径
  type-aliases-package: com.ruiji.entity # 实体类包路径
  configuration:
    map-underscore-to-camel-case: true # 开启下划线转驼峰命名
  global-config:
    db-config:
      id-type: auto # 主键自增
# 日志配置（可选，便于调试）
logging:
  level:
    com.ruiji.mapper: debug
步骤3：创建数据库表
根据业务需求，创建8张核心数据表，核心表结构如下（简化版）：
employee（员工表）：id、name、username、password、phone、status、create_time、update_time
category（分类表）：id、name、type（1菜品分类/2套餐分类）、sort、create_time、update_time
dish（菜品表）：id、name、category_id、price、image、description、status、create_time、update_time
setmeal（套餐表）：id、name、category_id、price、image、description、status、create_time、update_time
setmeal_dish（套餐-菜品关联表）：id、setmeal_id、dish_id、dish_num
user（用户表）：id、name、phone、password、avatar、create_time
shopping_cart（购物车表）：id、user_id、dish_id、setmeal_id、dish_num、create_time
orders（订单表）：id、user_id、order_time、checkout_time、pay_method、status、amount、address_book_id
可通过SQL脚本直接执行创建，后续通过MyBatis-Plus的代码生成器生成实体类、Mapper接口和Mapper.xml文件，提高开发效率。
步骤4：代码生成器配置（可选）
引入MyBatis-Plus代码生成器依赖，快速生成实体类、Mapper、Service、Controller，减少重复编码：
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-generator</artifactId>
    <version>3.5.3.1</version>
</dependency>
<dependency>
    <groupId>org.freemarker</groupId>
    <artifactId>freemarker</artifactId>
    <version>2.3.31</version>
</dependency>
编写代码生成器类，指定数据库连接、生成路径、包名等参数，执行后自动生成基础代码。
2.3 前端项目初始化（Vue）
步骤1：创建后台管理系统（Vue+Element UI）
# 1. 安装Vue脚手架
npm install -g @vue/cli
# 2. 创建项目
vue create ruiji-admin
# 3. 进入项目目录
cd ruiji-admin
# 4. 安装Element UI
npm i element-ui -S
# 5. 安装Axios
npm i axios -S
# 6. 启动项目
npm run serve
配置Element UI和Axios，在main.js中引入，统一配置请求基准路径（对接后端接口）。
步骤2：创建前台客户端（Vue+Vant UI）
同理，创建移动端客户端项目，引入Vant UI组件库，适配移动端界面，配置Axios请求，与后端接口对接。
三、核心功能实战开发
以下重点讲解后端核心功能的开发思路和关键代码，前端界面开发可结合Element UI/Vant UI组件库，实现对应页面与接口的联调。
3.1 员工管理模块（后台）
核心需求
实现员工的新增、修改、删除、分页查询，以及员工登录、权限控制（管理员可操作所有员工，普通员工仅能查看自身信息）。
关键开发步骤
1. 实体类（Employee）
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class Employee {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String username; // 登录账号（唯一）
    private String password; // 密码（加密存储）
    private String phone;
    private Integer status; // 状态：1启用，0禁用
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;
}
2. 自动填充配置（MyBatis-Plus）
实现元对象处理器，自动填充createTime、updateTime等公共字段：
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    // 新增时填充
    @Override
    public void insertFill(MetaObject metaObject) {
        metaObject.setValue("createTime", LocalDateTime.now());
        metaObject.setValue("updateTime", LocalDateTime.now());
        metaObject.setValue("createUser", 1L); // 暂设为管理员ID，后续结合登录信息修改
        metaObject.setValue("updateUser", 1L);
    }
    // 修改时填充
    @Override
    public void updateFill(MetaObject metaObject) {
        metaObject.setValue("updateTime", LocalDateTime.now());
        metaObject.setValue("updateUser", 1L);
    }
}
3. 登录功能（Spring Security）
配置Spring Security，实现员工登录认证，密码加密存储（使用BCryptPasswordEncoder）：
// 1. 密码加密配置
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    // 配置登录接口放行、权限控制
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable() // 关闭csrf防护（前后端分离场景）
            .authorizeRequests()
            .antMatchers("/employee/login").permitAll() // 登录接口放行
            .anyRequest().authenticated() // 其他接口需认证
            .and()
            .formLogin().disable() // 关闭默认登录页面
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS); // 无状态登录（适配前后端分离）
    }
}
// 2. 登录接口实现
@RestController
@RequestMapping("/employee")
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @PostMapping("/login")
    public R<Employee> login(@RequestBody Employee employee) {
        // 1. 根据用户名查询员工
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername, employee.getUsername());
        Employee emp = employeeService.getOne(queryWrapper);
        // 2. 校验员工是否存在
        if (emp == null) {
            return R.error("用户名不存在");
        }
        // 3. 校验密码（加密对比）
        if (!passwordEncoder.matches(employee.getPassword(), emp.getPassword())) {
            return R.error("密码错误");
        }
        // 4. 校验员工状态
        if (emp.getStatus() == 0) {
            return R.error("员工已禁用");
        }
        // 5. 登录成功，返回员工信息（可添加Token，后续优化）
        return R.success(emp);
    }
}
4. 分页查询功能
使用MyBatis-Plus的Page分页插件，实现员工分页查询，支持按姓名模糊搜索：
// 1. 分页插件配置
@Configuration
public class MyBatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
// 2. 分页查询接口
@GetMapping("/page")
public R<Page<Employee>> page(int page, int pageSize, String name) {
    // 1. 创建分页对象
    Page<Employee&gt; pageInfo = new Page<>(page, pageSize);
    // 2. 构建查询条件
    LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.like(StringUtils.isNotBlank(name), Employee::getName, name);
    queryWrapper.orderByDesc(Employee::getUpdateTime);
    // 3. 执行分页查询
    employeeService.page(pageInfo, queryWrapper);
    // 4. 返回结果
    return R.success(pageInfo);
}
3.2 菜品管理模块（后台）
核心需求
实现菜品的新增（含图片上传）、修改、删除、分页查询，关联菜品分类，控制菜品状态（启用/禁用）。
关键开发步骤
1. 图片上传功能
使用SpringBoot内置的MultipartFile处理图片上传，将图片保存到本地目录，并返回图片访问路径：
@RestController
@RequestMapping("/common")
public class CommonController {
    // 图片保存路径（自定义，需确保目录存在）
    private static final String BASE_PATH = "D:/ruiji/takeout/images";
    @PostMapping("/upload")
    public R<String> upload(MultipartFile file) {
        // 1. 获取原始文件名
        String originalFilename = file.getOriginalFilename();
        // 2. 生成唯一文件名（避免重复）
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + suffix;
        // 3. 创建目录（若不存在）
        File dir = new File(BASE_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // 4. 保存图片
        try {
            file.transferTo(new File(BASE_PATH + "/" + fileName));
        } catch (IOException e) {
            e.printStackTrace();
            return R.error("图片上传失败");
        }
        // 5. 返回图片访问路径（后续可配置静态资源映射，让前端访问）
        return R.success(fileName);
    }
}
// 静态资源映射配置（让前端能访问本地图片）
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:D:/ruiji/takeout/images/");
    }
}
2. 菜品新增功能
关联菜品分类，接收前端传递的菜品信息（含图片路径），保存到数据库：
@RestController
@RequestMapping("/dish")
public class DishController {
    @Autowired
    private DishService dishService;
    @PostMapping
    public R<String> save(@RequestBody Dish dish) {
        // 1. 保存菜品信息（MyBatis-Plus的save方法）
        dishService.save(dish);
        return R.success("菜品新增成功");
    }
}
3.3 购物车与订单模块（前台）
核心需求
用户登录后，可将菜品/套餐加入购物车，修改购物车数量，结算生成订单，查看订单历史和详情。
关键开发步骤
1. 购物车功能
// 购物车实体类
@Data
public class ShoppingCart {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId; // 关联用户ID
    private Long dishId; // 菜品ID（与套餐ID二选一）
    private Long setmealId; // 套餐ID
    private Integer dishNum; // 数量
    private LocalDateTime createTime;
}
// 购物车添加接口
@RestController
@RequestMapping("/shoppingCart")
public class ShoppingCartController {
    @Autowired
    private ShoppingCartService shoppingCartService;
    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart, HttpSession session) {
        // 1. 获取当前登录用户ID（从Session或Token中获取）
        Long userId = (Long) session.getAttribute("userId");
        shoppingCart.setUserId(userId);
        // 2. 检查购物车中是否已有该菜品/套餐
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, userId);
        if (shoppingCart.getDishId() != null) {
            queryWrapper.eq(ShoppingCart::getDishId, shoppingCart.getDishId());
        } else {
            queryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        }
        ShoppingCart cart = shoppingCartService.getOne(queryWrapper);
        // 3. 已有则数量+1，没有则新增
        if (cart != null) {
            cart.setDishNum(cart.getDishNum() + 1);
            shoppingCartService.updateById(cart);
        } else {
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartService.save(shoppingCart);
            cart = shoppingCart;
        }
        return R.success(cart);
    }
}
2. 订单生成功能
结算购物车，生成订单，同时清空购物车中对应商品：
@RestController
@RequestMapping("/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private ShoppingCartService shoppingCartService;
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders, HttpSession session) {
        // 1. 获取当前登录用户ID
        Long userId = (Long) session.getAttribute("userId");
        orders.setUserId(userId);
        // 2. 生成订单号（自定义规则，如时间戳+随机数）
        String orderNumber = System.currentTimeMillis() + "" + new Random().nextInt(1000);
        orders.setNumber(orderNumber);
        // 3. 设置订单状态（0：待付款，1：待接单，2：待派送，3：已完成）
        orders.setStatus(0);
        // 4. 计算订单总金额（查询购物车中该用户的所有商品）
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, userId);
        List<ShoppingCart> cartList = shoppingCartService.list(queryWrapper);
        BigDecimal totalAmount = cartList.stream()
                .map(cart -> {
                    // 这里需查询菜品/套餐价格，简化处理，假设cart中有price字段
                    return new BigDecimal(cart.getDishNum()).multiply(cart.getPrice());
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        orders.setAmount(totalAmount);
        // 5. 保存订单
        orderService.save(orders);
        // 6. 清空购物车
        shoppingCartService.remove(queryWrapper);
        return R.success("订单提交成功");
    }
}
四、项目优化与部署
4.1 项目优化点
Token认证：替换Session，使用JWT生成Token，实现无状态登录，适配前后端分离部署。
缓存优化：使用Redis缓存热门菜品、套餐数据，减少数据库查询压力。
异常处理：全局异常处理器，统一捕获业务异常、系统异常，返回规范的错误信息。
数据校验：使用JSR380注解（如@NotBlank、@NotNull）校验请求参数，避免非法数据。
文件存储：将本地文件上传改为OSS（阿里云/腾讯云），提高文件存储的稳定性和可扩展性。
4.2 项目部署
1. 后端部署
使用IDEA打包SpringBoot项目，生成jar包（maven clean package）。
将jar包上传到服务器，安装JDK、MySQL，配置数据库连接。
执行命令启动项目：nohup java -jar ruiji-takeout-1.0.0.jar &（后台运行）。
2. 前端部署
在VS Code中执行npm run build，生成dist静态文件。
将dist文件上传到服务器，安装Nginx，配置Nginx反向代理（对接后端接口）。
启动Nginx，访问服务器IP，即可看到前端页面。
五、项目总结
瑞吉外卖项目是SpringBoot综合实战的经典案例，涵盖了企业级开发中常见的技术点和业务场景，通过本项目可熟练掌握：
SpringBoot与MyBatis-Plus的整合，简化数据访问层开发。
前后端分离架构的开发流程，接口设计与联调。
常见业务逻辑的拆解与实现（如分页、文件上传、订单处理）。
权限控制、异常处理、缓存优化等企业级开发技巧。
项目的打包、部署流程，完成从开发到上线的全流程实践。
后续可基于本项目进行扩展，如添加支付功能、评价功能、骑手管理功能等，进一步提升项目的完整性和实用性。

