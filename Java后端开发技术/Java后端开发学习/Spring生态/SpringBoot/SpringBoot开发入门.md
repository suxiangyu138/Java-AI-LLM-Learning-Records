# Spring Boot 开发入门（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | Spring Boot 入门实战  
> **核心特性**：约定优于配置、自动配置、起步依赖、嵌入式服务器  
> **前置基础**：Java 基础（集合、注解）、Maven 基础、Spring IOC/DI 概念

---

## 一、核心概念

### 1.1 什么是 Spring Boot

Spring Boot 基于 Spring 框架构建，核心目标是 **简化 Spring 应用的初始搭建和开发过程**。通过"约定优于配置"理念，大量减少样板化配置，实现"开箱即用"。

> 传统 Spring 开发需大量 XML 配置，Spring Boot 只需少量配置甚至零配置即可启动完整应用。

### 1.2 五大核心特性

| 特性 | 说明 |
|------|------|
| **自动配置** | 根据项目依赖自动推断并配置所需的 Bean 和组件 |
| **起步依赖（Starter）** | 功能相关的依赖打包整合，一个坐标引入全部关联依赖 |
| **嵌入式服务器** | 内置 Tomcat/Jetty，无需部署 WAR，`java -jar` 直接启动 |
| **约定优于配置** | 默认规则覆盖 80%+ 常见场景，减少配置决策 |
| **生产级特性** | 内置健康监控（Actuator）、安全管理等 |

### 1.3 `@SpringBootApplication` 注解

```java
@SpringBootApplication  // 组合注解 = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

| 子注解 | 作用 |
|--------|------|
| `@SpringBootConfiguration` | 标记为配置类 |
| `@EnableAutoConfiguration` | 开启自动配置 |
| `@ComponentScan` | 扫描当前包及子包下的组件 |

---

## 二、底层原理

### 2.1 自动配置原理

1. `@EnableAutoConfiguration` 通过 `AutoConfigurationImportSelector` 读取 `spring.factories` 文件
2. 根据 classpath 中存在的类（条件注解 `@ConditionalOnClass`）决定哪些自动配置生效
3. 例如：classpath 有 `spring-webmvc` 则自动配置 Spring MVC + 嵌入式 Tomcat

### 2.2 约定优于配置

| 约定项 | 默认值 |
|--------|--------|
| 配置文件名称 | `application.properties` / `application.yml` |
| 配置文件位置 | `src/main/resources/` 或 `./config/` |
| 静态资源目录 | `src/main/resources/static/` |
| 模板目录 | `src/main/resources/templates/` |
| 默认端口 | 8080 |

---

## 三、代码实现

### 3.1 开发环境要求

| 工具 | 推荐版本 | 说明 |
|------|----------|------|
| JDK | JDK 8（Java 1.8） | 兼容性最好 |
| Maven | Maven 3.6.3 | 依赖管理核心工具 |
| IDE | IntelliJ IDEA 社区版 | 免费，内置 Spring Initializr |
| Spring Boot | 2.7.x 稳定版 | 避开 3.x，语法变化大 |

### 3.2 JDK 环境变量配置

```bash
# 系统变量
JAVA_HOME = D:\Java\jdk1.8.0_301  # 安装路径（无中文、无空格）

# Path 变量追加
%JAVA_HOME%\bin
%JAVA_HOME%\jre\bin

# 验证
java -version
javac -version
```

### 3.3 Maven 阿里云镜像配置

```xml
<!-- settings.xml -> <mirrors> -->
<mirror>
    <id>nexus-aliyun</id>
    <mirrorOf>*</mirrorOf>
    <name>Nexus aliyun</name>
    <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

### 3.4 创建 Spring Boot 项目

**方式 1：IDEA Spring Initializr（推荐）**

`File → New → Project → Spring Initializr → 选 JDK 8 → 填写 Group/Artifact → 勾选 Spring Web → Finish`

**方式 2：Spring 官网构建**

访问 `https://start.spring.io/` → 选 JDK 8 + Spring Boot 2.7.x + Spring Web → Generate 下载

### 3.5 pom.xml 核心依赖

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
    <relativePath/>
</parent>

<dependencies>
    <!-- Web 开发起步依赖（自动引入 Tomcat + Spring MVC） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

### 3.6 HelloWorld 接口

```java
package com.example.springboothello.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HelloWorld 控制器 —— @RestController = @Controller + @ResponseBody。
 */
@RestController
public class HelloController {

    @RequestMapping("/hello")
    public String hello() {
        return "Hello SpringBoot! 新手入门成功～";
    }
}
```

```properties
# application.properties — 修改默认端口
server.port=8081
```

### 3.7 项目结构

```
springboot-hello/
├── src/main/java/com/example/springboothello/
│   └── SpringbootHelloApplication.java    # 启动类
│   └── controller/HelloController.java    # 控制器
├── src/main/resources/
│   ├── application.properties             # 配置文件
│   ├── static/                            # 静态资源
│   └── templates/                         # 模板文件
└── pom.xml                                # Maven 依赖
```

### 3.8 启动与测试

```bash
# 启动：右键启动类 → Run
# 浏览器访问：http://localhost:8081/hello
# 输出：Hello SpringBoot! 新手入门成功～
```

---

## 四、实战要点

### 4.1 环境变量配置注意事项

- 安装路径 **不能有中文、不能有空格**
- `JAVA_HOME` + `Path` 都必须配置
- 配置后 **重启 cmd** 再验证

### 4.2 IDEA 编码配置

`File → Settings → Editor → File Encodings` → 全部设为 **UTF-8**（避免中文乱码）

---

## 五、避坑总结

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| **依赖下载慢/失败** | 未配置阿里云镜像 | 配置 settings.xml 阿里云镜像 → Reload Project |
| **端口被占用** | 默认 8080 被占用 | `application.properties` 中修改 `server.port` |
| **接口 404** | 缺少 `@RestController`、路径错误、扫描不到 | 检查注解、路径、组件扫描范围 |
| **environment 验证失败** | 环境变量路径配置错误 | 重新检查 `JAVA_HOME` 和 `Path`，重启 cmd/IDEA |

---

## 六、企业级最佳实践

### 6.1 版本选择建议

| 环境 | 推荐版本 |
|------|----------|
| 新手学习 | Spring Boot 2.7.x + JDK 8 |
| 生产环境 | Spring Boot 2.7.x 或 3.x + JDK 17 |

> 先掌握 2.7.x 稳定版，再升级到 3.x 更稳妥。

### 6.2 后续学习路线

1. 配置文件详解（`application.yml` + 多环境配置）
2. 核心注解深入（`@Controller`、`@Service`、`@Repository`）
3. Web 开发进阶（参数接收、响应处理、拦截器）
4. 数据访问（整合 MyBatis/MyBatis-Plus）
5. 项目部署（打包 JAR → 服务器部署）
