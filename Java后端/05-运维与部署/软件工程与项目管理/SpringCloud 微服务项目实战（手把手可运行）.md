SpringCloud 微服务项目实战（手把手可运行）
一、项目整体架构
1. 技术栈
    - 核心框架：SpringBoot 3.2.0、SpringCloud 2023.0.0、SpringCloud Alibaba 2022.0.0.0
    - 服务注册/发现：Nacos
    - 服务调用：OpenFeign
    - 网关：SpringCloud Gateway
    - 配置中心：Nacos Config
    - 熔断降级：Sentinel
    - 数据库：MySQL 8.0
    - 工具：Maven、Docker、Postman
2. 模块划分
    plaintext
    springcloud-demo/
    ├── nacos-config/          # 配置中心（Nacos）
    ├── gateway-service/       # 网关服务（端口：8080）
    ├── user-service/          # 用户服务（端口：8081）
    ├── order-service/         # 订单服务（端口：8082）
    └── common/                # 公共模块（实体、工具类）
 
二、父工程搭建（springcloud-demo）
1. pom.xml
    xml
    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>springcloud-demo</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <modules>
        <module>common</module>
        <module>user-service</module>
        <module>order-service</module>
        <module>gateway-service</module>
    </modules>
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <spring.boot.version>3.2.0</spring.boot.version>
        <spring.cloud.version>2023.0.0</spring.cloud.version>
        <spring.cloud.alibaba.version>2022.0.0.0</spring.cloud.alibaba.version>
    </properties>
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring.boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring.cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring.cloud.alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    </project>
 
三、公共模块（common）
1. pom.xml
    xml
    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>springcloud-demo</artifactId>
        <groupId>com.example</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>common</artifactId>
    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
    </dependencies>
    </project>
 
2. 实体类
    User.java
    java
    package com.example.common.entity;
    import lombok.Data;
    @Data
    public class User {
    private Long id;
    private String username;
    private String phone;
    }
 
Order.java
java
package com.example.common.entity;
import lombok.Data;
@Data
public class Order {
    private Long id;
    private Long userId;
    private String orderNo;
    private Double amount;
}
 
四、Nacos 环境准备
1. 下载安装
    - 官网：https://nacos.io/
    - 启动命令： bin/startup.cmd -m standalone （Windows）
    - 访问地址：http://localhost:8848/nacos（账号密码：nacos/nacos）
2. 配置中心（Nacos）
    - 新建配置： user-service.yaml 
    yaml
    server:
  port: 8081
    spring:
  datasource:
    url: jdbc:mysql://localhost:3306/springcloud?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
 
- 新建配置： order-service.yaml 
    yaml
    server:
  port: 8082
    spring:
  datasource:
    url: jdbc:mysql://localhost:3306/springcloud?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
 
五、用户服务（user-service）
1. pom.xml
    xml
    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>springcloud-demo</artifactId>
        <groupId>com.example</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>user-service</artifactId>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>3.0.3</version>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>common</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
    </project>
 
2. 启动类（UserServiceApplication.java）
    java
    package com.example.user;
    import org.mybatis.spring.annotation.MapperScan;
    import org.springframework.boot.SpringApplication;
    import org.springframework.boot.autoconfigure.SpringBootApplication;
    import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
    @SpringBootApplication
    @EnableDiscoveryClient
    @MapperScan("com.example.user.mapper")
    public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
    }
 
3. 配置文件（bootstrap.yml）
    yaml
    spring:
  application:
    name: user-service
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
      config:
        server-addr: localhost:8848
        file-extension: yaml
 
4. Mapper 层（UserMapper.java）
    java
    package com.example.user.mapper;
    import com.example.common.entity.User;
    import org.apache.ibatis.annotations.Select;
    public interface UserMapper {
    @Select("SELECT * FROM user WHERE id = #{id}")
    User getUserById(Long id);
    }
 
5. Service 层（UserService.java）
    java
    package com.example.user.service;
    import com.example.common.entity.User;
    import com.example.user.mapper.UserMapper;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Service;
    @Service
    public class UserService {
    @Autowired
    private UserMapper userMapper;
    public User getUserById(Long id) {
        return userMapper.getUserById(id);
    }
    }
 
6. Controller 层（UserController.java）
    java
    package com.example.user.controller;
    import com.example.common.entity.User;
    import com.example.user.service.UserService;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.PathVariable;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RestController;
    @RestController
    @RequestMapping("/user")
    public class UserController {
    @Autowired
    private UserService userService;
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }
    }
 
六、订单服务（order-service）
1. pom.xml
    xml
    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>springcloud-demo</artifactId>
        <groupId>com.example</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>order-service</artifactId>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>3.0.3</version>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>common</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
    </project>
 
2. 启动类（OrderServiceApplication.java）
    java
    package com.example.order;
    import org.mybatis.spring.annotation.MapperScan;
    import org.springframework.boot.SpringApplication;
    import org.springframework.boot.autoconfigure.SpringBootApplication;
    import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
    import org.springframework.cloud.openfeign.EnableFeignClients;
    @SpringBootApplication
    @EnableDiscoveryClient
    @EnableFeignClients
    @MapperScan("com.example.order.mapper")
    public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
    }
 
3. 配置文件（bootstrap.yml）
    yaml
    spring:
  application:
    name: order-service
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
      config:
        server-addr: localhost:8848
        file-extension: yaml
 
4. Feign 接口（UserFeignClient.java）
    java
    package com.example.order.feign;
    import com.example.common.entity.User;
    import org.springframework.cloud.openfeign.FeignClient;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.PathVariable;
    @FeignClient("user-service")
    public interface UserFeignClient {
    @GetMapping("/user/{id}")
    User getUserById(@PathVariable Long id);
    }
 
5. Controller 层（OrderController.java）
    java
    package com.example.order.controller;
    import com.example.common.entity.Order;
    import com.example.common.entity.User;
    import com.example.order.feign.UserFeignClient;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.PathVariable;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RestController;
    @RestController
    @RequestMapping("/order")
    public class OrderController {
    @Autowired
    private UserFeignClient userFeignClient;
    @GetMapping("/{id}")
    public Order getOrderById(@PathVariable Long id) {
        Order order = new Order();
        order.setId(id);
        order.setUserId(1L);
        order.setOrderNo("ORDER" + System.currentTimeMillis());
        order.setAmount(99.9);
        User user = userFeignClient.getUserById(1L);
        System.out.println("用户信息：" + user);
        return order;
    }
    }
 
七、网关服务（gateway-service）
1. pom.xml
    xml
    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>springcloud-demo</artifactId>
        <groupId>com.example</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>gateway-service</artifactId>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
    </dependencies>
    </project>
 
2. 启动类（GatewayServiceApplication.java）
    java
    package com.example.gateway;
    import org.springframework.boot.SpringApplication;
    import org.springframework.boot.autoconfigure.SpringBootApplication;
    import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
    @SpringBootApplication
    @EnableDiscoveryClient
    public class GatewayServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
    }
 
3. 配置文件（application.yml）
    yaml
    server:
  port: 8080
    spring:
  application:
    name: gateway-service
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
    gateway:
      routes:
        - id: user-route
          uri: lb://user-service
          predicates:
            - Path=/user/**
        - id: order-route
          uri: lb://order-service
          predicates:
            - Path=/order/**
 
八、数据库准备
sql
CREATE DATABASE springcloud;
USE springcloud;
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    phone VARCHAR(20)
);
INSERT INTO user (username, phone) VALUES ('张三', '13800138000');
CREATE TABLE order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    order_no VARCHAR(50) NOT NULL,
    amount DOUBLE NOT NULL
);
 
九、启动与测试
1. 启动顺序
1. Nacos
2. user-service
3. order-service
4. gateway-service
2. 测试接口
    - 用户服务：http://localhost:8080/user/1
    - 订单服务：http://localhost:8080/order/1
    十、核心功能说明
    1. 服务注册/发现：所有服务注册到 Nacos，实现自动发现
    2. 服务调用：通过 OpenFeign 实现订单服务调用用户服务
    3. 配置中心：Nacos Config 统一管理配置，支持动态刷新
    4. 网关路由：Gateway 统一入口，实现负载均衡
    5. 熔断降级：Sentinel 实现服务保护（可配置限流规则）
