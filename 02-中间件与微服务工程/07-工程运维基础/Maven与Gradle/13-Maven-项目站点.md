# Maven 项目站点

## 一、概述

Maven Site 通过 `maven-site-plugin` 自动生成标准化的 HTML 格式项目站点，整合项目信息、测试报告、覆盖率、API 文档等。

### 核心价值

| 价值 | 说明 |
|------|------|
| **信息集中** | 项目信息、依赖、构建配置一站呈现 |
| **自动化** | 无需手动编写 HTML，插件自动提取 pom.xml 信息 |
| **团队协作** | 统一视角，开发/测试/运维各取所需 |
| **可追溯** | 记录版本变更、测试结果历史 |

## 二、核心插件

| 插件 | 作用 |
|------|------|
| `maven-site-plugin` | 核心插件，生成站点基础结构 |
| `maven-surefire-report-plugin` | 单元测试报告 |
| `jacoco-maven-plugin` | 代码覆盖率报告 |
| `maven-javadoc-plugin` | JavaDoc API 文档 |
| `maven-project-info-reports-plugin` | 项目信息报告 |

## 三、pom.xml 配置

```xml
<build>
    <plugins>
        <!-- 1. 核心站点插件 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-site-plugin</artifactId>
            <version>3.12.1</version>
            <configuration>
                <encoding>UTF-8</encoding>
                <locales>zh_CN</locales>
            </configuration>
        </plugin>

        <!-- 2. 单元测试报告 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-report-plugin</artifactId>
            <version>3.1.2</version>
            <configuration>
                <encoding>UTF-8</encoding>
                <reportFormat>html</reportFormat>
            </configuration>
        </plugin>

        <!-- 3. 代码覆盖率 -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.10</version>
            <executions>
                <execution>
                    <id>prepare-agent</id>
                    <goals><goal>prepare-agent</goal></goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals><goal>report</goal></goals>
                    <configuration>
                        <outputDirectory>target/site/jacoco</outputDirectory>
                    </configuration>
                </execution>
            </executions>
        </plugin>

        <!-- 4. JavaDoc API 文档 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-javadoc-plugin</artifactId>
            <version>3.5.0</version>
            <configuration>
                <encoding>UTF-8</encoding>
                <docencoding>UTF-8</docencoding>
                <charset>UTF-8</charset>
                <title>项目 API 文档</title>
            </configuration>
            <executions>
                <execution>
                    <phase>site</phase>
                    <goals><goal>javadoc</goal></goals>
                </execution>
            </executions>
        </plugin>

        <!-- 5. 项目信息报告 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-project-info-reports-plugin</artifactId>
            <version>3.4.5</version>
            <configuration>
                <encoding>UTF-8</encoding>
            </configuration>
        </plugin>
    </plugins>
</build>

<!-- 完善项目信息（站点自动提取） -->
<name>Spring Boot 企业级应用</name>
<description>用户管理、订单管理等核心模块</description>
<url>http://www.company.com/project</url>
<developers>
    <developer>
        <id>dev1</id>
        <name>张三</name>
        <email>zhangsan@company.com</email>
    </developer>
</developers>
```

## 四、生成站点

```bash
# 先执行测试（生成测试数据）
mvn clean test

# 生成站点
mvn site

# 一键完成
mvn clean test site

# 打包站点为 JAR（便于分发）
mvn site:jar
```

生成结果：`target/site/index.html`

## 五、站点内容

| 模块 | 内容 | 来源 |
|------|------|------|
| **项目概述** | 坐标、描述、版本、团队成员 | pom.xml |
| **依赖信息** | 所有依赖的坐标、版本、依赖树 | pom.xml 依赖解析 |
| **测试报告** | 测试通过/失败/跳过统计，失败详情 | Surefire 报告 |
| **覆盖率报告** | 类/方法/行覆盖率 | JaCoCo 报告 |
| **API 文档** | 类/接口/方法说明 | JavaDoc 注释 |
| **构建信息** | Maven 版本、JDK 版本、构建时间 | 构建元数据 |

## 六、自定义站点

### 6.1 目录结构

```
src/site/
├── site.xml                      # 站点配置文件（导航栏）
├── markdown/                     # Markdown 自定义文档
│   ├── 开发规范.md
│   └── 部署说明.md
└── resources/
    ├── css/custom.css            # 自定义样式
    └── images/logo.png           # Logo
```

### 6.2 site.xml 示例

```xml
<project name="Spring Boot 企业级应用">
    <skin>
        <groupId>org.apache.maven.skins</groupId>
        <artifactId>maven-default-skin</artifactId>
        <version>1.3</version>
    </skin>
    <body>
        <logo>images/logo.png</logo>

        <menu name="项目概述">
            <item name="首页" href="index.html"/>
            <item name="依赖信息" href="dependencies.html"/>
        </menu>

        <menu name="报告">
            <item name="测试报告" href="surefire-report.html"/>
            <item name="覆盖率" href="jacoco/index.html"/>
            <item name="API文档" href="apidocs/index.html"/>
        </menu>

        <menu name="自定义文档">
            <item name="开发规范" href="开发规范.html"/>
            <item name="部署说明" href="部署说明.html"/>
        </menu>
    </body>
</project>
```

### 6.3 引用 site.xml

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-site-plugin</artifactId>
    <configuration>
        <siteDirectory>src/site</siteDirectory>
    </configuration>
</plugin>
```

## 七、站点部署

| 方式 | 说明 |
|------|------|
| **本地查看** | 浏览器打开 `target/site/index.html` |
| **Web 服务器** | 将 `target/site/` 部署到 Nginx/Tomcat |
| **私有仓库** | `mvn site:deploy` 部署到 Nexus |

## 八、最佳实践

| 实践 | 说明 |
|------|------|
| **与构建绑定** | CI/CD 中在 test 阶段后自动执行 `mvn site` |
| **编码统一** | 所有插件配置 UTF-8，避免中文乱码 |
| **完善 pom 信息** | name、description、developers 会被站点提取 |
| **规范 JavaDoc** | 类/方法注释直接影响 API 文档质量 |
| **精简报告** | 关闭无用的报告模块，提升生成速度 |

## 九、常见问题

| 问题 | 解决方案 |
|------|---------|
| 站点生成失败 | 检查插件版本兼容性，统一 Maven/JDK 版本 |
| 中文乱码 | 所有插件 + settings.xml 统一配置 UTF-8 |
| 报告缺失 | 先生成数据再生成站点：`mvn clean test site` |
| 自定义页面不显示 | 检查 site.xml 中 href 与文件名一致 |
| API 文档无内容 | 检查 JavaDoc 注释是否规范 |
