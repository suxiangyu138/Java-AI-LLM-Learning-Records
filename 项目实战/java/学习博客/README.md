# 学习博客 (Learning Blog)

> Spring Boot 4.0 + MyBatis + MySQL 博客系统后端

## 项目概述

基于 Spring Boot 4.0.2 构建的博客系统后端项目。采用经典三层架构（Controller → Service → Mapper），集成 MyBatis ORM 框架和 MySQL 数据库。使用 Maven Wrapper 确保构建环境一致性，Lombok 简化实体类代码。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 4.0.2 | 应用框架（内嵌 Tomcat） |
| Spring MVC | - | RESTful API 接口 |
| MyBatis | 4.0.1 | ORM 数据库映射 |
| MySQL | - | 关系型数据库 |
| Lombok | - | 代码简化（@Data, @Slf4j） |
| Maven | - | 项目管理与构建 |
| JUnit | - | 单元测试 |

## 功能特性

- **文章管理**：创建、编辑、删除、查询博客文章
- **RESTful API**：标准 HTTP 接口（GET/POST/PUT/DELETE）
- **MyBatis 映射**：XML 或注解方式 SQL 映射
- **数据库集成**：MySQL 存储文章、用户数据
- **Lombok 简化**：自动生成 getter/setter/构造器

## 项目结构

```
学习博客/
├── src/
│   ├── main/
│   │   ├── java/com/blog/studyblog/
│   │   │   └── StudyBlogApplication.java    # Spring Boot 启动类
│   │   └── resources/
│   │       └── application.properties       # 应用配置
│   └── test/
│       └── java/com/blog/studyblog/
│           └── StudyBlogApplicationTests.java # 测试类
├── target/                          # Maven 构建输出
├── pom.xml                          # Maven 配置
├── HELP.md                          # Spring Boot 帮助文档
├── mvnw / mvnw.cmd                  # Maven Wrapper（跨平台）
└── README.md
```

## 快速开始

### 1. 配置数据库

```properties
# src/main/resources/application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/study_blog
spring.datasource.username=root
spring.datasource.password=your_password

mybatis.mapper-locations=classpath:mappers/*.xml
```

### 2. 启动应用

```bash
# 使用 Maven Wrapper（推荐，无需安装 Maven）
./mvnw spring-boot:run

# 或使用系统 Maven
mvn spring-boot:run

# 打包运行
mvn clean package
java -jar target/study-blog-0.0.1-SNAPSHOT.jar
```

### 3. 测试接口

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 测试 API（示例）
curl http://localhost:8080/api/articles
```

## 推荐扩展架构

```
学习博客/
├── src/main/java/com/blog/studyblog/
│   ├── StudyBlogApplication.java        # 启动类
│   ├── controller/
│   │   ├── ArticleController.java       # 文章接口
│   │   └── UserController.java          # 用户接口
│   ├── service/
│   │   ├── ArticleService.java          # 文章业务逻辑
│   │   └── UserService.java             # 用户业务逻辑
│   ├── mapper/
│   │   ├── ArticleMapper.java           # 文章 MyBatis 映射
│   │   └── UserMapper.java              # 用户 MyBatis 映射
│   ├── model/
│   │   ├── Article.java                 # 文章实体
│   │   └── User.java                    # 用户实体
│   └── config/
│       └── MyBatisConfig.java           # MyBatis 配置
└── src/main/resources/
    ├── application.properties
    └── mappers/
        ├── ArticleMapper.xml
        └── UserMapper.xml
```

## 核心知识点

| 知识点 | 应用 |
|--------|------|
| Spring Boot 自动配置 | @SpringBootApplication 启动 |
| IoC / DI | @Autowired, @Service, @Repository |
| Spring MVC | @RestController, @RequestMapping, @GetMapping/@PostMapping |
| MyBatis | @Mapper, @Select/@Insert, XML 映射文件 |
| 数据源配置 | application.properties 中 spring.datasource.* |
| Lombok | @Data, @NoArgsConstructor, @AllArgsConstructor |
| Maven Wrapper | mvnw 脚本确保使用正确的 Maven 版本 |
| 三层架构 | Controller → Service → Mapper |

## 注意事项

- Spring Boot 4.0.x 需要 JDK 17+
- MyBatis 的 @Mapper 注解需要被 Spring 扫描到，或使用 @MapperScan
- 数据库表建议使用 Flyway/Liquibase 做版本管理
- 开发环境与生产环境的数据库配置应分离（application-dev.yml / application-prod.yml）
