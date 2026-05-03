03.31 01:24
Java API 项目实战：用户管理 API（Spring Boot + 企业级规范）
 
项目简介
基于 Spring Boot 2.7.x 构建的 RESTful 风格用户管理 API，包含用户注册、登录、CRUD、分页查询、统一响应、全局异常处理等企业级必备功能，适合快速上手 Java Web 开发。
技术栈
- 核心框架：Spring Boot 2.7.18
- 数据访问：MyBatis-Plus、MySQL 8.0
- 工具：Lombok、Hutool、PageHelper
- 规范：RESTful API、统一响应、全局异常、参数校验
项目结构
plaintext
com.user.api
├── config        // 配置类（MyBatis、跨域、线程池）
├── controller    // 控制层（API 接口）
├── entity        // 实体类
├── mapper        // 数据访问层
├── service       // 业务层
│   ├── impl      // 业务实现
├── dto           // 数据传输对象（请求参数）
├── vo            // 视图对象（响应数据）
├── common        // 公共类（响应、异常、常量）
└── UserApiApplication // 启动类
 
完整代码实现
1. 公共模块（统一响应 + 全局异常）
1.1 统一响应类（Result）
java
import lombok.Data;
/**
 * 统一 API 响应结果
 */
@Data
public class Result<T> {
    private int code;
    private String msg;
    private T data;
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }
    public static <T> Result<T> success() {
        return success(null);
    }
    public static <T> Result<T> error(int code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
}
 
1.2 全局异常处理（GlobalExceptionHandler）
java
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        return Result.error(500, e.getMessage());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError().getDefaultMessage();
        return Result.error(400, msg);
    }
}
 
2. 实体类（User）
java
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * 用户实体类
 */
@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String phone;
    private String email;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
 
3. DTO（请求参数）
java
import lombok.Data;
import javax.validation.constraints.NotBlank;
/**
 * 用户注册请求参数
 */
@Data
public class UserRegisterDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
    @NotBlank(message = "手机号不能为空")
    private String phone;
}
 
4. Mapper 层（UserMapper）
java
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.user.api.entity.User;
import org.apache.ibatis.annotations.Mapper;
/**
 * 用户数据访问层
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
 
5. Service 层
5.1 UserService
java
import com.baomidou.mybatisplus.extension.service.IService;
import com.user.api.dto.UserRegisterDTO;
import com.user.api.entity.User;
/**
 * 用户业务接口
 */
public interface UserService extends IService<User> {
    /**
     * 用户注册
     */
    void register(UserRegisterDTO dto);
}
 
5.2 UserServiceImpl
java
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.user.api.dto.UserRegisterDTO;
import com.user.api.entity.User;
import com.user.api.mapper.UserMapper;
import com.user.api.service.UserService;
import org.springframework.stereotype.Service;
/**
 * 用户业务实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Override
    public void register(UserRegisterDTO dto) {
        // 校验用户名是否存在
        User existUser = this.lambdaQuery()
                .eq(User::getUsername, dto.getUsername())
                .one();
        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }
        // 构建用户对象
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setPhone(dto.getPhone());
        user.setStatus(1);
        user.setCreateTime(java.time.LocalDateTime.now());
        user.setUpdateTime(java.time.LocalDateTime.now());
        // 保存用户
        this.save(user);
    }
}
 
6. Controller 层（UserController）
java
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.user.api.common.Result;
import com.user.api.dto.UserRegisterDTO;
import com.user.api.entity.User;
import com.user.api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
/**
 * 用户 API 接口
 */
@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserService userService;
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Validated UserRegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }
    /**
     * 根据 ID 查询用户
     */
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        User user = userService.getById(id);
        return Result.success(user);
    }
    /**
     * 分页查询用户
     */
    @GetMapping("/page")
    public Result<Page<User>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<User> page = new Page<>(pageNum, pageSize);
        Page<User> userPage = userService.page(page);
        return Result.success(userPage);
    }
    /**
     * 更新用户
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody User user) {
        userService.updateById(user);
        return Result.success();
    }
    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success();
    }
}
 
7. 配置文件（application.yml）
yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/user_api?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
 
8. 启动类（UserApiApplication）
java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/**
 * 项目启动类
 */
@SpringBootApplication
public class UserApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApiApplication.class, args);
    }
}
 
9. 数据库 SQL
sql
CREATE DATABASE IF NOT EXISTS user_api DEFAULT CHARACTER SET utf8mb4;
USE user_api;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态 1-正常 0-禁用',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
 
接口测试（API 文档）
接口地址 请求方式 功能 
/api/user/register POST 用户注册 
/api/user/{id} GET 查询用户 
/api/user/page GET 分页查询用户 
/api/user/update PUT 更新用户 
/api/user/{id} DELETE 删除用户 
运行步骤
1. 创建 MySQL 数据库，执行 SQL 脚本
2. 修改 application.yml 中数据库连接信息
3. 启动 UserApiApplication
4. 使用 Postman 测试接口
扩展方向
1. 集成 Redis 实现缓存
2. 加入 JWT 登录认证
3. 整合 Knife4j 生成 API 文档
4. 实现用户登录、权限控制
5. 加入接口限流、日志记录

