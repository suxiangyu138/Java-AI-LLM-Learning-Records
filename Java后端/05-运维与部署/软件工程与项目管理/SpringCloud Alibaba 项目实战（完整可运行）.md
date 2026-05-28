SpringCloud Alibaba 项目实战（完整可运行）
一、项目架构与技术栈
1. 核心技术栈
    - SpringBoot 3.2.0
    - SpringCloud Alibaba 2022.0.0.0
    - Nacos（服务注册/发现、配置中心）
    - OpenFeign（服务调用）
    - SpringCloud Gateway（网关）
    - Sentinel（熔断降级、限流）
    - Seata（分布式事务）
    - MySQL 8.0、MyBatis-Plus
    - Maven、Docker
2. 模块划分
    plaintext
    springcloud-alibaba-demo/
    ├── common              # 公共模块（实体、工具、异常）
    ├── gateway-service     # 网关服务（8080）
    ├── user-service        # 用户服务（8081）
    ├── order-service       # 订单服务（8082）
    └── storage-service     # 库存服务（8083）
 
二、父工程搭建
pom.xml
xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.alibaba</groupId>
    <artifactId>springcloud-alibaba-demo</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <modules>
        <module>common</module>
        <module>user-service</module>
        <module>order-service</module>
        <module>storage-service</module>
        <module>gateway-service</module>
    </modules>
    <properties>
        <java.version>17</java.version>
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
        <artifactId>springcloud-alibaba-demo</artifactId>
        <groupId>com.alibaba</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>common</artifactId>
    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>3.5.5</version>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.33</version>
        </dependency>
    </dependencies>
    </project>
 
2. 实体类
    User.java
    java
    package com.alibaba.common.entity;
    import lombok.Data;
    @Data
    public class User {
    private Long id;
    private String username;
    private Integer money;
    }
 
Order.java
java
package com.alibaba.common.entity;
import lombok.Data;
@Data
public class Order {
    private Long id;
    private Long userId;
    private Long productId;
    private Integer count;
    private Integer money;
}
 
Storage.java
java
package com.alibaba.common.entity;
import lombok.Data;
@Data
public class Storage {
    private Long id;
    private Long productId;
    private Integer total;
    private Integer used;
    private Integer residue;
}
 
四、环境准备
1. Nacos 安装启动
    - 下载：https://github.com/alibaba/nacos/releases
    - 启动： bin/startup.cmd -m standalone 
    - 访问：http://localhost:8848/nacos（nacos/nacos）
2. Sentinel 安装启动
    - 下载：https://github.com/alibaba/Sentinel/releases
    - 启动： java -jar sentinel-dashboard-1.8.6.jar 
    - 访问：http://localhost:8080（sentinel/sentinel）
3. Seata 安装启动
    - 下载：https://github.com/seata/seata/releases
    - 配置：registry.conf、file.conf
    - 启动： bin/seata-server.bat -p 8091 
    五、用户服务（user-service）
    1. pom.xml
    xml
    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>springcloud-alibaba-demo</artifactId>
        <groupId>com.alibaba</groupId>
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
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>common</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
    </project>
 
2. 配置文件（bootstrap.yml）
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
  datasource:
    url: jdbc:mysql://localhost:3306/springcloud?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
 
3. 启动类
    java
    package com.alibaba.user;
    import org.mybatis.spring.annotation.MapperScan;
    import org.springframework.boot.SpringApplication;
    import org.springframework.boot.autoconfigure.SpringBootApplication;
    import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
    @SpringBootApplication
    @EnableDiscoveryClient
    @MapperScan("com.alibaba.user.mapper")
    public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
    }
 
4. Controller
    java
    package com.alibaba.user.controller;
    import com.alibaba.common.entity.User;
    import com.alibaba.user.service.UserService;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.web.bind.annotation.*;
    @RestController
    @RequestMapping("/user")
    public class UserController {
    @Autowired
    private UserService userService;
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getById(id);
    }
    @PostMapping("/deduct")
    public void deduct(@RequestParam Long userId, @RequestParam Integer money) {
        userService.deduct(userId, money);
    }
    }
 
六、库存服务（storage-service）
1. pom.xml
    xml
    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>springcloud-alibaba-demo</artifactId>
        <groupId>com.alibaba</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>storage-service</artifactId>
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
            <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>common</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
    </project>
 
2. Controller
    java
    package com.alibaba.storage.controller;
    import com.alibaba.storage.service.StorageService;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.web.bind.annotation.*;
    @RestController
    @RequestMapping("/storage")
    public class StorageController {
    @Autowired
    private StorageService storageService;
    @PostMapping("/deduct")
    public void deduct(@RequestParam Long productId, @RequestParam Integer count) {
        storageService.deduct(productId, count);
    }
    }
 
七、订单服务（order-service）
1. pom.xml
    xml
    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>springcloud-alibaba-demo</artifactId>
        <groupId>com.alibaba</groupId>
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
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>common</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
    </project>
 
2. Feign 接口
    java
    package com.alibaba.order.feign;
    import org.springframework.cloud.openfeign.FeignClient;
    import org.springframework.web.bind.annotation.PostMapping;
    import org.springframework.web.bind.annotation.RequestParam;
    @FeignClient("user-service")
    public interface UserFeignClient {
    @PostMapping("/user/deduct")
    void deduct(@RequestParam Long userId, @RequestParam Integer money);
    }
 
java
package com.alibaba.order.feign;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
@FeignClient("storage-service")
public interface StorageFeignClient {
    @PostMapping("/storage/deduct")
    void deduct(@RequestParam Long productId, @RequestParam Integer count);
}
 
3. 分布式事务（Seata）
    java
    package com.alibaba.order.service;
    import com.alibaba.order.entity.Order;
    import com.alibaba.order.feign.StorageFeignClient;
    import com.alibaba.order.feign.UserFeignClient;
    import com.alibaba.order.mapper.OrderMapper;
    import io.seata.spring.annotation.GlobalTransactional;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Service;
    @Service
    public class OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private StorageFeignClient storageFeignClient;
    @GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
    public void createOrder(Order order) {
        // 1. 保存订单
        orderMapper.insert(order);
        // 2. 扣减库存
        storageFeignClient.deduct(order.getProductId(), order.getCount());
        // 3. 扣减余额
        userFeignClient.deduct(order.getUserId(), order.getMoney());
    }
    }
 
八、网关服务（gateway-service）
1. pom.xml
    xml
    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>springcloud-alibaba-demo</artifactId>
        <groupId>com.alibaba</groupId>
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
 
2. 配置文件
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
        - id: storage-route
          uri: lb://storage-service
          predicates:
            - Path=/storage/**
 
九、数据库脚本
sql
CREATE DATABASE springcloud;
USE springcloud;
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50),
    money INT
);
INSERT INTO user (username, money) VALUES ('张三', 1000);
CREATE TABLE storage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT,
    total INT,
    used INT,
    residue INT
);
INSERT INTO storage (product_id, total, used, residue) VALUES (1, 100, 0, 100);
CREATE TABLE order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    product_id BIGINT,
    count INT,
    money INT
);
 
十、启动与测试
1. 启动顺序
1. Nacos
2. Seata
3. Sentinel
4. user-service
5. storage-service
6. order-service
7. gateway-service
2. 测试接口
    - 创建订单：http://localhost:8080/order/create
    - 参数：userId=1, productId=1, count=1, money=100
    十一、核心功能
    1. 服务注册发现：Nacos 统一管理服务
    2. 配置中心：Nacos Config 动态配置
    3. 服务调用：OpenFeign 声明式调用
    4. 网关路由：Gateway 统一入口
    5. 熔断限流：Sentinel 保护服务
    6. 分布式事务：Seata 保证数据一致性
