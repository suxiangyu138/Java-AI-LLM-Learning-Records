# 快速学会 SpringBoot
## 一、SpringBoot 核心定位
**SpringBoot = SpringFramework + 自动配置 + 起步依赖 + 内嵌容器**
- 消灭繁琐 XML、消灭手动大量 Bean 配置
- 约定大于配置，开箱即用
- 一键整合全中间件（Redis、MQ、ES、MySQL）
- 你之前学的 **Spring IOC/AOP/事务** 全部复用，只是简化写法

---

## 二、两大核心机制（必懂）
### 1. 起步依赖 Starter
所有场景都有封装好的依赖：
- `spring-boot-starter-web`：Web 开发（SpringMVC+Tomcat）
- `spring-boot-starter-data-redis`：Redis 整合
- `spring-boot-starter-aop`：AOP 切面
- `spring-boot-starter-test`：单元测试

### 2. 自动配置 AutoConfiguration
底层大量 `@Conditional` 条件注解：
- 引入依赖 → 自动加载对应配置类
- 容器自动创建 Bean，无需手动 `@Bean`
- 只需在 `application.yml` 改参数即可

---

## 三、0 基础快速搭建项目
### 1. 项目创建方式
1. IDEA 直接新建 → Spring Initializr
2. 在线：[start.spring.io](https://start.spring.io)
3. 选：Maven / Java / SpringBoot 稳定版（2.7.x 或 3.2.x）
4. 勾选依赖：**Spring Web**

### 2. 项目结构（固定规范）
```
com.xxx.demo
├── DemoApplication.java  // 启动类（核心）
├── controller
├── service
├── mapper
└── application.yml       // 配置文件
```

---

## 四、第一个 Web 接口（5 分钟上手）
### 1. 启动类
```java
@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```
`@SpringBootApplication` 三合一：
- `@Configuration`：配置类
- `@EnableAutoConfiguration`：开启自动配置
- `@ComponentScan`：自动扫描当前包及子包所有组件

### 2. 编写 Controller
```java
@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello SpringBoot";
    }
}
```

### 3. 运行测试
启动 main 方法，浏览器访问：
```
http://localhost:8080/hello
```
直接返回结果，**无需部署 Tomcat**。

---

## 五、SpringBoot 核心常用注解
1. **Web 层**
- `@RestController` = `@Controller + @ResponseBody`
- `@RequestMapping / @GetMapping / @PostMapping`
- `@RequestParam` 普通参数
- `@PathVariable` 路径参数
- `@RequestBody` JSON 参数

2. **IOC 复用 Spring 原生**
- `@Service`、`@Component`、`@Repository`
- `@Autowired` 自动注入
- `@Configuration` 自定义配置类
- `@Bean` 手动注册第三方组件

3. **配置绑定**
- `@Value("${xxx.xxx}")` 读取配置文件
- `@ConfigurationProperties` 批量绑定配置

4. **其他**
- `@Transactional` 声明式事务
- `@Aspect`、`@Around` AOP 切面
- `@Scheduled` 定时任务

---

## 六、配置文件 application.yml
主流使用 yml，简洁分层：
```yaml
# 端口配置
server:
  port: 8081

# 日志级别
logging:
  level:
    com.xxx: debug
```
同目录下支持 `application.properties` 旧式写法。

---

## 七、三层架构标准开发流程
Controller → Service → Mapper
1. Controller：接收请求、参数校验、响应结果
2. Service：业务逻辑、事务控制
3. Mapper：数据库 CRUD

示例：
```java
@Service
public class UserService {
    public User getById(Long id){
        // 业务逻辑
    }
}
```

---

## 八、常用整合（企业必用）
### 1. 整合 MyBatis
引入依赖 + yml 配置数据库，直接注入 Mapper 即可使用。
### 2. 整合 Redis
引入 starter，自动注入 `RedisTemplate`。
### 3. 全局异常处理
`@RestControllerAdvice + @ExceptionHandler` 统一捕获异常。
### 4. 跨域解决
注解 `@CrossOrigin` 或全局跨域配置类。

---

## 九、核心高频面试点（必记）
1. `@SpringBootApplication` 底层原理
2. 自动配置实现原理：SPI + 条件注解
3. 内嵌 Tomcat 容器
4. 配置文件加载优先级
5. SpringBoot 约定目录结构
6. 如何关闭某个自动配置

---

## 十、学习路线（衔接你技术栈）
1. 熟练：Web 接口开发、yml 配置、三层架构
2. 掌握：全局异常、AOP、事务、参数校验
3. 进阶：整合 MySQL/Redis/ES/RabbitMQ
4. 进阶：SpringBoot + SpringCloudAlibaba 微服务

---