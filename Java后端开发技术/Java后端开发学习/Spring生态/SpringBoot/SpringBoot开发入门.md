03.21 13:55
SpringBoot开发入门
一、SpringBoot核心认知（新手必懂）
1.1 什么是SpringBoot
SpringBoot是由Pivotal团队开发的开源框架，基于Spring框架构建，核心目标是简化Spring应用的初始搭建和开发过程。它通过“约定优于配置”的理念，减少大量样板化配置，让开发者无需关注繁琐的环境配置和依赖管理，能更专注于业务逻辑的实现，快速搭建一个生产级别的Java应用。
简单来说，传统Spring开发需要编写大量XML配置文件，而SpringBoot实现了“开箱即用”，只需少量配置甚至零配置，就能启动一个完整的应用，极大降低了Java开发的入门门槛。
1.2 SpringBoot核心特性（重点掌握）
自动配置：核心特性，SpringBoot会根据项目中引入的依赖，自动推断并配置所需的Bean和组件，无需手动编写配置。例如，引入web依赖后，会自动配置Spring MVC和嵌入式Tomcat服务器。
起步依赖（Starter）：将相关功能的依赖打包整合，开发者只需引入一个起步依赖坐标，即可自动获取该功能所需的所有关联依赖，避免手动管理依赖版本冲突。比如spring-boot-starter-web就是Web开发的起步依赖，包含了Web开发必备的所有组件。
嵌入式服务器：内置Tomcat、Jetty等Web服务器，无需手动部署WAR包，应用可直接打包为可执行JAR文件，通过java -jar命令即可启动，简化部署流程。
约定优于配置：定义了一套默认规则，覆盖80%以上的常见开发场景，比如配置文件默认名为application.properties/yml，静态资源默认放在src/main/resources/static目录。若默认规则不满足需求，也可灵活覆盖配置。
生产级特性：内置健康监控、安全管理等功能，引入spring-boot-starter-actuator依赖即可实现应用运行状态监控，满足生产环境的基本需求。
1.3 入门前提（必备基础）
学习SpringBoot前，需掌握以下基础知识点，否则会影响入门效率：
Java基础：掌握面向对象、注解、集合、异常处理等核心知识点（推荐Java 8，兼容性最佳）；
Maven基础：了解Maven的依赖管理、项目构建流程，能简单配置Maven；
Spring基础：了解Spring的IOC（控制反转）、DI（依赖注入）核心思想，无需深入掌握XML配置。
二、开发环境搭建（避坑版，新手必看）
环境搭建是入门的第一道坎，新手容易因版本不兼容、配置错误导致失败，以下是亲测无坑的步骤，全程实操，跟着走即可成功搭建。
2.1 必备工具及推荐版本（关键避坑点）
无需盲目追求最新版本，优先选择稳定兼容版，避免版本冲突，推荐版本如下：
工具
推荐版本
说明
JDK
JDK 8（Java 1.8）
兼容性拉满，支持所有SpringBoot稳定版，避开JDK 17+，避免老依赖不兼容
Maven
Maven 3.6.3
依赖管理核心工具，版本过高或过低可能导致依赖下载失败
开发工具（IDE）
IntelliJ IDEA 社区版
免费且功能足够新手使用，内置SpringBoot项目创建工具，无需额外装插件
SpringBoot
2.7.x 稳定版
避开3.x版本，3.x语法、依赖变化较大，新手先掌握2.7.x再升级更稳妥
2.2 分步搭建环境（Windows系统为例）
步骤1：安装并配置JDK 8
下载：从Oracle官网（需注册账号）或正规渠道下载JDK 8安装包，选择对应Windows系统的x64版本；
安装：双击安装包，一路下一步，安装路径务必不要有中文、不要有空格（如D:\Java\jdk1.8.0_301），记住该路径；
配置环境变量：右键“此电脑”→属性→高级系统设置→环境变量，在“系统变量”中操作：
新建变量：变量名JAVA_HOME，变量值填JDK安装路径；
编辑Path变量：新增两个路径%JAVA_HOME%\bin和%JAVA_HOME%\jre\bin，保存；
验证：打开cmd（Win+R输入cmd），依次输入java -version和javac -version，若显示JDK 1.8相关版本，无报错即成功。
避坑提醒：若提示“不是内部或外部命令”，大概率是环境变量配置错误，检查JAVA_HOME路径是否正确，Path是否添加两个bin路径，配置后重启cmd再验证。
步骤2：安装并配置Maven
下载：从Maven官网下载apache-maven-3.6.3-bin.zip，解压后放在无中文、无空格的路径（如D:\apache-maven-3.6.3）；
配置环境变量：新建MAVEN_HOME变量，变量值填Maven解压路径；编辑Path变量，新增%MAVEN_HOME%\bin，重启cmd；
验证：输入mvn -v，能显示Maven 3.6.3版本和对应JDK版本，即安装成功；
配置阿里云镜像（核心提速）：打开Maven解压路径下的conf/settings.xml文件，在<mirrors></mirrors>标签内添加阿里云镜像代码，替换默认国外镜像，提升依赖下载速度： <mirror> <id>nexus-aliyun</id> <mirrorOf>*</mirrorOf> <name>Nexus aliyun</name> <url>https://maven.aliyun.com/repository/public</url> </mirror>
可选配置本地仓库：找到<localRepository></localRepository>标签，修改为自定义路径（如D:\maven-repository），用于缓存依赖，避免重复下载。
步骤3：安装IDEA并配置
下载：从JetBrains官网下载IDEA社区版（免费），根据系统选择对应版本；
安装：双击安装包，一路下一步，勾选“创建桌面快捷方式”，默认安装即可；
配置JDK和Maven（关键一步）：启动IDEA后，点击Configure→Project Structure：
配置JDK：点击SDKs→+，选择之前安装的JDK路径，命名为JDK 1.8，保存；
配置Maven：点击Build, Execution, Deployment→Build Tools→Maven，依次选择Maven解压路径、settings.xml文件路径，本地仓库路径会自动识别，点击Apply→OK即可。
配置编码（避免中文乱码）：点击File→Settings→Editor→File Encodings，将Global Encoding、Project Encoding、Default encoding for properties files全部设为UTF-8。
三、第一个SpringBoot项目实战（HelloWorld）
环境搭建完成后，通过一个简单的HelloWorld项目，验证环境可用性，同时熟悉SpringBoot项目结构和核心用法。
3.1 创建SpringBoot项目（两种方式，推荐IDEA内置）
方式1：IDEA内置Spring Initializr（推荐，最便捷）
打开IDEA，点击File→New→Project，左侧选择Spring Initializr，Project SDK选择已配置的JDK 8，点击Next；
填写项目基本信息（新手可直接默认，后续可修改）：
Group：公司/组织域名（如com.example）；
Artifact：项目名称（如springboot-hello）；
Version：项目版本（默认0.0.1-SNAPSHOT）；
Java Version：选择8；
选择依赖：勾选Spring Web（Web开发必备，会自动引入Tomcat和Spring MVC），点击Next；
确认项目路径（无中文、无空格），点击Finish，IDEA会自动下载依赖、生成项目结构，耐心等待依赖下载完成（首次下载可能较慢，取决于网络）。
方式2：Spring官方网站构建（备用）
访问Spring官方构建地址：https://start.spring.io/；
填写项目信息、选择JDK 8和SpringBoot 2.7.x版本，勾选Spring Web依赖；
点击Generate下载ZIP包，解压后，在IDEA中选择File→New→Project from Existing Sources，导入解压后的项目即可。
3.2 项目结构解析（新手必认）
生成的SpringBoot项目结构清晰，核心目录和文件如下（以springboot-hello为例）：
springboot-hello/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── springboothello/
│   │   │               └── SpringbootHelloApplication.java  # 项目启动类（核心）
│   │   └── resources/  # 配置文件、静态资源目录
│   │       ├── application.properties  # 默认配置文件
│   │       ├── static/  # 静态资源（CSS、JS、图片等）
│   │       └── templates/  # 模板文件（Thymeleaf等，本次暂用不到）
│   └── test/  # 单元测试目录（暂用不到）
└── pom.xml  # Maven依赖配置文件
3.3 核心文件说明
（1）启动类：SpringbootHelloApplication.java
这是SpringBoot项目的入口，必须包含@SpringBootApplication注解和main方法，代码如下（自动生成，无需修改）：
package com.example.springboothello;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// 核心注解：标记当前类为SpringBoot启动类，整合自动配置、组件扫描等功能
@SpringBootApplication
public class SpringbootHelloApplication {
    public static void main(String[] args) {
        // 启动SpringBoot应用，参数为启动类字节码对象
        SpringApplication.run(SpringbootHelloApplication.class, args);
    }
}
@SpringBootApplication是组合注解，包含三个核心子注解，新手无需深入理解，记住“启动类必须加这个注解”即可：
@SpringBootConfiguration：标记当前类为配置类；
@EnableAutoConfiguration：开启自动配置功能；
@ComponentScan：扫描当前包及其子包下的组件。
（2）依赖配置文件：pom.xml
核心作用是管理项目依赖，自动生成的pom.xml包含SpringBoot核心依赖和Web依赖，关键代码如下：
<!-- 父依赖：SpringBoot的核心依赖，统一管理依赖版本 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
    <relativePath/>
</parent>
<!-- Web开发起步依赖：引入后自动配置Tomcat、Spring MVC -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
（3）配置文件：application.properties
SpringBoot的默认配置文件，用于覆盖默认配置，比如修改服务器端口、配置数据源等。新手可先添加端口配置，避免端口冲突：
# 修改服务器端口（默认8080，若8080被占用，可改为8081等）
server.port=8081
3.4 编写HelloWorld接口（核心实战）
在启动类所在的包（com.example.springboothello）下，新建controller包（规范命名，用于存放控制器）；
在controller包下新建HelloController类，编写如下代码： package com.example.springboothello.controller; import org.springframework.web.bind.annotation.RequestMapping; import org.springframework.web.bind.annotation.RestController; // 注解说明： // @RestController：组合注解，等价于@Controller + @ResponseBody，返回JSON格式数据 @RestController public class HelloController { // @RequestMapping：映射请求路径，这里映射“/hello” @RequestMapping("/hello") public String hello() { // 返回HelloWorld内容，浏览器访问该路径时会显示 return "Hello SpringBoot! 新手入门成功～"; } }
3.5 启动项目并测试
启动项目：找到启动类SpringbootHelloApplication，点击类左侧的绿色运行按钮（或右键→Run），启动成功后，控制台会显示Tomcat启动信息，看到“Started SpringbootHelloApplication in XXX seconds”即为启动成功；
测试接口：打开浏览器，输入地址http://localhost:8081/hello（端口需与application.properties中配置的一致），浏览器显示“Hello SpringBoot! 新手入门成功～”，即测试成功。
避坑提醒：若启动失败，大概率是端口被占用（修改server.port）、依赖下载不完整（重新刷新Maven依赖）或JDK/Maven配置错误（重新检查环境配置）。
四、新手常见问题及解决方案
问题1：依赖下载缓慢或失败：解决方案：确认Maven已配置阿里云镜像，重启IDEA，右键项目→Maven→Reload Project，重新下载依赖；
问题2：项目启动报错“端口被占用”：解决方案：在application.properties中修改server.port，更换一个未被占用的端口（如8081、8082）；
问题3：浏览器访问接口报404：解决方案：检查控制器类是否加了@RestController注解、请求路径是否正确、启动类是否和控制器在同一包（或子包）下（确保@ComponentScan能扫描到控制器）；
问题4：JDK/Maven配置后验证失败：解决方案：检查环境变量配置是否正确，重启cmd或IDEA，重新验证。
五、入门后续学习方向
完成HelloWorld项目后，可按以下顺序继续学习，逐步掌握SpringBoot核心用法：
配置文件详解：掌握application.properties/yml的配置方式，学习常用配置（端口、日志、编码等）；
核心注解深入：重点学习@SpringBootApplication、@Controller、@Service、@Repository等注解的用法；
Web开发进阶：学习请求参数接收、响应处理、拦截器、过滤器等Web开发常用功能；
数据访问：整合MyBatis/MyBatis-Plus，实现数据库的增删改查；
项目部署：学习将SpringBoot项目打包为JAR包，部署到服务器。
新手提示：SpringBoot入门的核心是“多动手、多调试”，不要害怕报错，大部分报错都是环境配置或代码规范问题，耐心排查就能解决。先掌握基础用法，再逐步深入底层原理，循序渐进更高效。

