01.27 02:30
Dubbo 的用途

Dubbo 是阿里巴巴开源的高性能、轻量级的 Java RPC 框架，核心用于解决分布式系统中服务之间的远程调用问题，具体用途包括：
1. 实现服务注册与发现，让服务提供者和消费者解耦，支持 Zookeeper、Nacos 等注册中心。
2. 提供负载均衡、容错机制（如失败重试、熔断降级），保障分布式系统的高可用。
3. 支持多种协议（Dubbo、HTTP、gRPC 等）和序列化方式（Hessian、JSON、Protobuf 等），适配不同业务场景。
4. 常用于微服务架构中，替代 HTTP 接口实现更高效的跨服务通信，广泛应用在电商、金融等分布式系统。
Dubbo 基础使用方法（基于 Zookeeper 注册中心，中文版 IntelliJ IDEA 操作）
前提准备
 1. 安装并启动 Zookeeper（作为注册中心），默认端口  2181 。
2. 打开 IDEA，创建 Maven 多模块项目，分为  dubbo-api （公共接口）、 dubbo-provider （服务提供者）、 dubbo-consumer （服务消费者）三个模块。
步骤 1：编写公共接口模块（dubbo-api）
 1. 在  dubbo-api  的  src/main/java  下创建包  com.example.dubbo ，编写业务接口：
java
package com.example.dubbo;
// 定义公共服务接口
public interface HelloService {
    String sayHello(String name);
}
 
2. 执行  clean install ，将该模块打包到本地 Maven 仓库，供其他模块依赖。
步骤 2：编写服务提供者模块（dubbo-provider）
1. 添加依赖：在  dubbo-provider  的  pom.xml  中引入 Dubbo、Zookeeper 客户端和  dubbo-api  依赖：
xml
<dependencies>
    <!-- Dubbo 核心依赖 -->
    <dependency>
        <groupId>org.apache.dubbo</groupId>
        <artifactId>dubbo</artifactId>
        <version>3.2.0</version>
    </dependency>
    <!-- Zookeeper 注册中心依赖 -->
    <dependency>
        <groupId>org.apache.dubbo</groupId>
        <artifactId>dubbo-dependencies-zookeeper</artifactId>
        <version>3.2.0</version>
        <type>pom</type>
        <exclusions>
            <exclusion>
                <groupId>org.slf4j</groupId>
                <artifactId>slf4j-log4j12</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
    <!-- 引入公共接口模块 -->
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>dubbo-api</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
</dependencies>
 
2. 编写接口实现类：创建  com.example.dubbo.provider.HelloServiceImpl ：
java
package com.example.dubbo.provider;
import com.example.dubbo.HelloService;
import org.apache.dubbo.config.annotation.DubboService;
// 标注为 Dubbo 服务，暴露接口
@DubboService
public class HelloServiceImpl implements HelloService {
    @Override
    public String sayHello(String name) {
        return "Hello, " + name + "! This is Dubbo Provider.";
    }
}
 
3. 编写提供者启动类：创建  com.example.dubbo.provider.ProviderApplication ：
java
package com.example.dubbo.provider;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// 开启 Dubbo 注解支持
@EnableDubbo
@SpringBootApplication
public class ProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class, args);
        System.out.println("Dubbo Provider Started!");
    }
}
 
4. 配置 application.yml：在  src/main/resources  下创建配置文件：
yaml
dubbo:
  application:
    name: dubbo-provider # 应用名称
  registry:
    address: zookeeper://127.0.0.1:2181 # 注册中心地址
  protocol:
    name: dubbo # 通信协议
    port: 20880 # 服务端口
spring:
  main:
    allow-bean-definition-overriding: true
 
步骤 3：编写服务消费者模块（dubbo-consumer）
1. 添加依赖：在  dubbo-consumer  的  pom.xml  中引入 Dubbo、Zookeeper 客户端和  dubbo-api  依赖（与提供者一致）。
2. 编写消费者业务类：创建  com.example.dubbo.consumer.HelloController （以 Spring Boot 接口为例）：
java
package com.example.dubbo.consumer;
import com.example.dubbo.HelloService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class HelloController {
    // 远程注入服务接口
    @DubboReference
    private HelloService helloService;
    @GetMapping("/sayHello/{name}")
    public String sayHello(@PathVariable String name) {
        // 调用远程服务
        return helloService.sayHello(name);
    }
}
 
3. 编写消费者启动类：创建  com.example.dubbo.consumer.ConsumerApplication ：
java
package com.example.dubbo.consumer;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@EnableDubbo
@SpringBootApplication
public class ConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
        System.out.println("Dubbo Consumer Started!");
    }
}
 
4. 配置 application.yml：在  src/main/resources  下创建配置文件：
yaml
dubbo:
  application:
    name: dubbo-consumer # 应用名称
  registry:
    address: zookeeper://127.0.0.1:2181 # 注册中心地址
server:
  port: 8080 # 消费者端口
 
步骤 4：运行测试
1. 确保 Zookeeper 已启动。
2. 先启动  ProviderApplication ，再启动  ConsumerApplication 。
3. 访问  http://localhost:8080/sayHello/Dubbo ，页面会返回  Hello, Dubbo! This is Dubbo Provider. 。
核心关键点总结
1. 服务暴露：通过  @DubboService  注解将实现类暴露为 Dubbo 服务，注册到注册中心。
2. 服务引用：通过  @DubboReference  注解远程注入服务接口，实现跨进程调用。
3. 注册中心：负责管理服务地址，实现服务提供者和消费者的解耦，常用 Zookeeper、Nacos。
4. 通信协议：Dubbo 默认使用 Dubbo 协议，也支持 HTTP、gRPC 等，可按需配置。

