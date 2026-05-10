03.23 17:37
从Java后端开发角度深度详细全面剖析Maven：基于Maven开发Web应用
在Java后端开发领域，Web应用是最主流的开发场景（如管理系统、接口服务、网站后台等），而Maven作为Java项目的标准化构建与依赖管理工具，是Web应用开发的核心支撑。与普通Java项目相比，Maven开发Web应用有其独特的项目结构、依赖配置、构建部署流程，且需结合Servlet、Tomcat等Web相关技术。本文将从Java后端开发视角，深度、详细、全面地剖析“基于Maven开发Web应用”的完整流程，涵盖需求分析、环境准备、项目搭建、依赖配置、功能开发、测试、打包、部署八大核心环节，补充后端开发必备的规范、细节和高频问题解决方案，确保新手能从零上手，资深开发者能巩固核心知识点，真正掌握Maven在Web应用中的实战用法。
一、前置认知：Maven开发Web应用的核心优势与后端开发场景
1.1 核心优势（贴合后端开发痛点）
Java后端开发中，Web应用通常依赖大量第三方jar包（如Servlet、JSP、Tomcat、数据库驱动等），手动管理依赖、配置项目结构、打包部署效率极低，且易出现版本冲突、配置混乱等问题。Maven开发Web应用的核心优势的是“标准化、自动化、可复用”，具体体现在：
标准化项目结构：Maven定义了Web应用的标准目录结构，无需手动创建WEB-INF、web.xml等核心目录/文件，避免不同开发者的项目结构混乱，便于团队协作。
自动化依赖管理：通过pom.xml配置即可自动下载Web相关依赖（如Servlet API、JSP API），无需手动下载jar包，且能自动处理依赖传递，解决版本冲突。
自动化构建部署：通过Maven命令（如package、tomcat:run），可一键完成编译、测试、打包、部署，无需手动执行javac、jar、部署到Tomcat等繁琐操作，提升开发效率。
多环境适配：可通过Maven配置开发、测试、生产等不同环境的参数（如数据库连接、端口），实现一键切换环境，贴合后端开发的多环境部署需求。
1.2 后端开发常见Web应用场景
基于Maven开发的Java Web应用，主要分为两类，均是后端开发的高频场景：
传统Java Web应用：基于Servlet、JSP、JDBC开发，部署到Tomcat、Jetty等Web服务器，适合中小型管理系统、后台接口服务。
SpringBoot Web应用：基于SpringBoot框架开发（底层依赖Maven），内置Tomcat服务器，无需手动部署，适合微服务架构、前后端分离接口服务（后续会专项讲解，本文重点聚焦传统Web应用，夯实基础）。
1.3 核心前提（必确认，避免实操报错）
基于Maven开发Web应用，需提前完成以下环境准备，这是后端开发的基础，也是后续实操的前提：
JDK环境：推荐JDK 8（企业级Web开发主流版本，兼容所有Web框架和Maven版本），已配置环境变量。
Maven环境：已完成本地安装（推荐3.6.x、3.8.x版本），配置阿里云镜像、自定义本地仓库路径，执行mvn -v可正常显示版本信息。
IDE工具：推荐Eclipse（已安装Maven插件、Web插件）或IDEA（内置Maven和Web支持），本文将以Eclipse为例（贴合部分后端开发者的日常使用习惯），IDEA操作流程同步补充。
Web服务器：Tomcat 8.5.x（与JDK 8兼容，主流稳定版本），已完成安装和配置，能正常启动。
注意1：JDK、Maven、Tomcat版本需兼容（JDK 8 + Tomcat 8.5.x + Maven 3.6.x），否则会出现部署失败、运行报错等问题。
注意2：Eclipse需安装Web插件（Web Tools Platform，WTP），若未安装，需先安装（通过Eclipse的Install New Software搜索“WTP”即可），否则无法创建Web项目。
二、核心实操：基于Maven开发Web应用（完整流程，后端规范）
本文将以“用户管理Web应用”为案例，实现“用户列表查询、新增用户”两个核心功能，全程贴合后端开发规范，一步步完成从项目搭建到部署测试的全流程，重点剖析Maven在Web应用中的核心配置和实操细节。
步骤1：需求分析（后端视角，明确开发范围）
案例需求（简单实用，聚焦Maven Web核心操作，不引入复杂框架）：
页面层：1个JSP页面（userList.jsp），用于展示用户列表、提供新增用户表单。
控制层：1个Servlet（UserServlet），处理前端请求（查询用户、新增用户），调用业务层方法。
业务层：1个Service接口（UserService）和1个实现类（UserServiceImpl），处理业务逻辑（模拟查询、新增用户，暂不连接数据库，后续可扩展）。
实体层：1个User实体类，封装用户信息（id、姓名、年龄）。
Maven配置：配置Web相关依赖、Tomcat插件、编译插件，实现自动化构建和部署。
部署测试：将项目打包为war包，部署到Tomcat服务器，验证功能正常。
步骤2：创建Maven Web项目（后端标准结构）
Maven Web项目有固定的标准结构，与普通Java项目的核心区别是多了WEB-INF目录（存放web.xml、JSP页面、静态资源等），下面分别讲解Eclipse和IDEA的创建步骤，贴合不同开发者的使用习惯。
2.1 Eclipse创建Maven Web项目（后端常用）
启动Eclipse，点击「File」→「New」→「Other...」，搜索「Maven」，选择「Maven Project」，点击「Next」。
勾选「Create a simple project（skip archetype selection）」，点击「Next」（跳过模板选择，避免生成冗余目录）。
配置项目坐标（后端规范，必须填写正确，用于依赖管理和部署识别）：
Group Id：com.example.backend.web（反向域名+项目模块，体现Web应用）。
Artifact Id：user-manage-web（项目名称，体现功能：用户管理Web应用）。
Version：1.0.0（语义化版本）。
Packaging：war（Web应用必须选择war包，普通Java项目选择jar包）。
点击「Finish」，Eclipse会自动生成Maven Web标准项目结构，此时项目可能会有红色叉号（正常现象，因未配置Web相关依赖和web.xml）。
完善项目结构（后端规范，补充缺失目录）：
在src/main/java下，创建后端标准包结构：com.example.backend.web.servlet（控制层）、com.example.backend.web.service（业务层）、com.example.backend.web.entity（实体层）。
在src/main下，手动创建webapp目录（存放JSP页面、静态资源），在webapp下创建WEB-INF目录（核心Web配置目录）。
在WEB-INF下，创建web.xml文件（Web应用核心配置文件，配置Servlet、过滤器等）。
在webapp下，创建JSP页面（如userList.jsp），存放静态资源（css、js、images）可创建对应目录。
最终标准项目结构（后端开发必须遵循，否则会出现部署失败、资源找不到等问题）： user-manage-web（项目根目录） ├─ src │ ├─ main │ │ ├─ java（后端源代码） │ │ │ └─ com.example.backend.web │ │ │ ├─ servlet（Servlet控制层） │ │ │ ├─ service（业务层） │ │ │ └─ entity（实体层） │ │ └─ webapp（Web资源目录） │ │ ├─ WEB-INF（核心配置目录） │ │ │ ├─ web.xml（Web配置文件） │ │ │ └─ lib（Maven自动生成，存放依赖jar包） │ │ └─ userList.jsp（JSP页面） │ └─ test（测试代码目录，可选） │ └─ java └─ pom.xml（Maven核心配置文件）
注意：webapp目录必须放在src/main下，且WEB-INF目录名称、位置不能修改，否则Tomcat无法识别Web应用。
2.2 IDEA创建Maven Web项目（补充，适配IDEA用户）
启动IDEA，点击「New Project」，选择「Maven」，取消勾选「Create from archetype」，点击「Next」。
配置项目坐标（与Eclipse一致），点击「Next」，配置项目名称和路径，点击「Finish」。
右键点击项目根目录，选择「Add Framework Support...」，勾选「Web Application」，点击「OK」，IDEA会自动生成webapp和WEB-INF目录、web.xml文件。
补充后端包结构（与Eclipse一致），完善项目结构即可。
步骤3：配置pom.xml（Web应用核心，依赖+构建配置）
Maven Web应用的pom.xml配置，比普通Java项目多了「Web相关依赖」「Tomcat插件」，这是Web应用能正常编译、运行、部署的核心，需严格按照以下配置编写（直接复制粘贴，标注部分可根据自身环境修改）：
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion&gt;
    <!-- 项目坐标（步骤2配置，无需修改） -->
    <groupId>com.example.backend.web</groupId>
    <artifactId>user-manage-web</artifactId>
    <version>1.0.0</version&gt;
    &lt;packaging&gt;war&lt;/packaging&gt; <!-- Web应用必须为war包 -->
    <name>user-manage-web</name>
    <description>Java后端用户管理Web应用，基于Maven构建</description&gt;
    <!-- 依赖配置：Web应用核心依赖，后端开发必配 -->
    &lt;dependencies&gt;
        <!-- 1. Servlet API依赖（provided范围，Tomcat已提供，无需打包） -->
        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>javax.servlet-api</artifactId>
            <version>4.0.1</version>
            <scope>provided</scope><!-- 关键：避免与Tomcat的servlet-api冲突 -->
        &lt;/dependency&gt;
        <!-- 2. JSP API依赖（provided范围，Tomcat已提供） -->
        <dependency>
            <groupId>javax.servlet.jsp</groupId>
            <artifactId>jsp-api</artifactId>
            <version>2.3.3</version>
            <scope>provided</scope>
        </dependency&gt;
        <!-- 3. JSTL标签库依赖（用于JSP页面遍历数据，可选但推荐） -->
        <dependency>
            <groupId>javax.servlet.jsp.jstl</groupId>
            <artifactId>jstl-api</artifactId>
            <version>1.2</version>
        </dependency>
        <dependency>
            <groupId>taglibs</groupId>
            <artifactId>standard</artifactId>
            <version>1.1.2</version>
        </dependency>
       <!-- 4. Junit测试依赖（test范围，仅测试时使用） -->
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.12</version>
            <scope>test</scope&gt;
        </dependency>
    </dependencies>
    <!-- 构建配置：Web应用核心，指定JDK版本、Tomcat插件、打包规则 -->
   <build>
        <!-- 自定义war包名称，便于部署识别 -->
        <finalName>user-manage-web</finalName>
        <plugins>
            <!-- 1. 编译插件：指定JDK 8，避免编译版本不一致和中文乱码（后端必配） -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>8</source>
                    <target>8</target>
                    <encoding>UTF-8</encoding>
                </configuration>
         </plugin>
            <!-- 2. Tomcat插件：实现Maven一键部署到Tomcat，后端开发高频使用 -->
            <plugin>
                <groupId>org.apache.tomcat.maven</groupId>
                <artifactId>tomcat7-maven-plugin</artifactId>
                <version>2.2</version>
                <configuration>
                   <port>8080</port> <!-- Tomcat端口，与本地Tomcat一致 -->
                    <path>/user-manage</path> <!-- 项目访问路径，如http://localhost:8080/user-manage -->
                   <uriEncoding>UTF-8</uriEncoding> <!-- 解决中文乱码 -->
                    &lt;server&gt;tomcat8&lt;/server&gt; <!-- 与settings.xml中配置的Tomcat服务器名称一致（可选） -->
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
关键配置说明（后端开发必懂，避免踩坑）
packaging：必须设置为war，否则Maven会打包为jar包，Tomcat无法识别（Web应用专属）。
依赖范围（scope）：servlet-api和jsp-api的scope必须为provided，因为Tomcat服务器本身已包含这两个jar包，若设置为compile，会导致打包后出现重复依赖，Tomcat启动报错。
Tomcat插件：使用tomcat7-maven-plugin（兼容Tomcat 8.5.x），配置port（端口）、path（访问路径），实现Maven一键启动Tomcat、部署项目，无需手动将war包复制到Tomcat的webapps目录。
编码配置：编译插件和Tomcat插件均配置UTF-8，避免JSP页面、请求参数出现中文乱码（后端Web开发高频坑点）。
配置完成后，点击Eclipse右下角「Import Changes」，Maven会自动下载所有依赖，下载完成后，项目的红色叉号会消失（若仍有叉号，右键点击项目→「Maven」→「Update Project...」，勾选「Force Update of Snapshots/Releases」，点击「OK」即可）。
步骤4：编写Java代码（后端分层开发，贴合规范）
按照后端分层开发规范，依次编写实体类、业务层、控制层代码，确保代码可复用、可维护，贴合Web应用的请求处理流程（前端请求→Servlet→Service→实体类）。
4.1 编写User实体类（entity层）
在com.example.backend.web.entity包下，创建User.java，封装用户信息，符合Java Bean规范：
package com.example.backend.web.entity;
/**
 * 用户实体类，封装用户信息（后端实体类规范：命名首字母大写，对应业务模型）
 */
public class User {
    private Integer id;      // 用户ID
    private String name;     // 姓名
    private Integer age;     // 年龄
    // 无参构造（必写，便于反射实例化）
    public User() {
    }
    // 有参构造（便于创建对象）
    public User(Integer id, String name, Integer age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }
    // getter/setter方法（必写，用于获取和设置属性值）
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Integer getAge() {
        return age;
    }
    public void setAge(Integer age) {
        this.age = age;
    }
}
4.2 编写业务层（service层）
业务层采用“接口+实现类”的方式，贴合后端开发的面向接口编程规范，便于后续扩展（如添加数据库操作）。
4.2.1 UserService接口
package com.example.backend.web.service;
import com.example.backend.web.entity.User;
import java.util.List;
/**
 * 用户业务层接口，定义业务逻辑规范（后端业务层规范：命名以Service结尾）
 */
public interface UserService {
    // 查询所有用户（模拟数据，暂不连接数据库）
    List<User> findAllUsers();
    // 新增用户（模拟数据）
    boolean addUser(User user);
}
4.2.2 UserServiceImpl实现类
package com.example.backend.web.service;
import com.example.backend.web.entity.User;
import java.util.ArrayList;
import java.util.List;
/**
 * 业务层实现类，实现具体业务逻辑（后端规范：命名以ServiceImpl结尾，实现对应接口）
 */
public class UserServiceImpl implements UserService {
    // 模拟数据库，用List存储用户数据
    private static List<User> userList = new ArrayList<>();
    // 静态代码块，初始化模拟数据
    static {
        userList.add(new User(1, "张三", 20));
        userList.add(new User(2, "李四", 22));
        userList.add(new User(3, "王五", 25));
    }
    @Override
    public List<User> findAllUsers() {
        // 模拟查询所有用户，返回模拟数据
        return userList;
    }
    @Override
    public boolean addUser(User user) {
        // 模拟新增用户，添加到List中
        if (user == null || user.getName() == null || user.getName().trim().isEmpty()) {
            return false;
        }
        // 生成自增ID
        int maxId = userList.stream().mapToInt(User::getId).max().orElse(0);
        user.setId(maxId + 1);
        userList.add(user);
        return true;
    }
}
4.3 编写控制层（servlet层）
Servlet是Web应用的核心控制层，负责接收前端请求（GET/POST），调用业务层方法，处理请求并返回响应（跳转页面或输出结果）。在com.example.backend.web.servlet包下，创建UserServlet.java：
package com.example.backend.web.servlet;
import com.example.backend.web.entity.User;
import com.example.backend.web.service.UserService;
import com.example.backend.web.service.UserServiceImpl;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
/**
 * 用户控制层Servlet，处理前端请求（后端规范：命名以Servlet结尾，继承HttpServlet）
 * @WebServlet注解：配置Servlet路径，替代web.xml中的<servlet>和<servlet-mapping>配置（Servlet 3.0+支持）
 */
@WebServlet("/user")
public class UserServlet extends HttpServlet {
    // 注入业务层对象（后端开发规范：控制层依赖业务层）
    private UserService userService = new UserServiceImpl();
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 处理GET请求：查询所有用户，跳转到userList.jsp页面展示
        List<User> userList = userService.findAllUsers();
        // 将用户列表存入request域，供JSP页面获取
        request.setAttribute("userList", userList);
        // 跳转至userList.jsp页面（请求转发，保留请求域数据）
        request.getRequestDispatcher("/userList.jsp").forward(request, response);
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 处理POST请求：新增用户，解决中文乱码
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
        // 获取前端表单提交的参数（name、age）
        String name = request.getParameter("name");
        String ageStr = request.getParameter("age");
        Integer age = null;
        if (ageStr != null && !ageStr.trim().isEmpty()) {
            age = Integer.parseInt(ageStr);
        }
        // 调用业务层方法新增用户
        User user = new User(null, name, age);
        boolean result = userService.addUser(user);
        // 新增完成后，重定向到用户列表页面（重定向，避免表单重复提交）
        response.sendRedirect(request.getContextPath() + "/user");
    }
}
注意1：@WebServlet("/user")注解用于配置Servlet的访问路径（替代web.xml中的配置），前端可通过http://localhost:8080/user-manage/user访问该Servlet。
注意2：doGet方法处理查询请求，doPost方法处理新增请求，需在doPost方法中设置request和response的编码，避免中文乱码。
注意3：请求转发（forward）和重定向（redirect）的区别：请求转发保留请求域数据，地址栏不变；重定向不保留请求域数据，地址栏变化，新增、删除、修改操作推荐使用重定向，避免表单重复提交。
步骤5：编写Web前端资源（JSP页面）
在webapp目录下，创建userList.jsp页面，实现用户列表展示和新增用户表单，使用JSTL标签库遍历用户数据（需引入JSTL标签）：
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> <!-- 引入JSTL核心标签库 -->
<html>
<head>
    <title>用户管理页面</title>
    <style>
        table {border-collapse: collapse; width: 600px; margin: 20px auto;}
        td, th {border: 1px solid #000; padding: 10px; text-align: center;}
        .add-form {width: 600px; margin: 20px auto; text-align: center;}
    </style>
</head>
<body>
    <h1 style="text-align: center;">用户管理系统</h1>
    <!-- 新增用户表单（POST请求提交到Servlet） -->
    <div class="add-form">
        <form action="${pageContext.request.contextPath}/user" method="post">
            姓名：<input type="text" name="name" required><br><br>
            年龄：<input type="number" name="age" min="1" max="120" required><br><br>
            <input type="submit" value="新增用户">
        </form>
    </div&gt;
    <!-- 用户列表展示（使用JSTL标签遍历request域中的userList） -->
    <table>
        <tr>
            <th>用户ID</th>
            <th>姓名</th>
            <th>年龄</th>
        </tr>
        <c:forEach items="${userList}" var="user">
            <tr>
                <td>${user.id}</td>
                <td>${user.name}</td>
                <td>${user.age}</td>
            </tr>
        </c:forEach>
    </table>
</body>
</html>
注意1：需引入JSTL标签库（<%@ taglib ... %>），否则无法使用<c:forEach>标签遍历数据，若出现“无法识别c标签”报错，检查JSTL依赖是否下载成功。
注意2：${pageContext.request.contextPath}用于获取项目访问路径（如/user-manage），避免硬编码路径，确保项目部署后路径正确。
注意3：表单的method设置为post，对应Servlet的doPost方法，提交的参数名（name、age）需与Servlet中request.getParameter("name")的参数名一致。
步骤6：配置web.xml（可选，兼容低版本Servlet）
若使用Servlet 3.0以下版本（不支持@WebServlet注解），需在WEB-INF/web.xml中配置Servlet，替代注解配置；若使用Servlet 3.0及以上版本，可省略此步骤（本文使用Servlet 4.0，已通过注解配置）。web.xml配置示例：
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
         version="4.0">;
    <!-- 配置Servlet -->
    <servlet>
        <servlet-name>UserServlet</servlet-name>
        <servlet-class>com.example.backend.web.servlet.UserServlet</servlet-class>
    </servlet>
    <!-- 配置Servlet映射（访问路径） -->
    <servlet-mapping>
        <servlet-name>UserServlet</servlet-name>
        <url-pattern>/user</url-pattern>
   </servlet-mapping>
    <!-- 配置默认首页（可选） -->
    <welcome-file-list>
        <welcome-file>userList.jsp</welcome-file>
    </welcome-file-list>
</web-app>
步骤7：Maven编译与测试（后端开发必做，避免bug）
代码编写完成后，通过Maven执行编译、测试命令，检查代码是否有语法错误、功能是否正常，步骤如下：
编译项目：右键点击项目→「Run As」→「Maven compile」，控制台显示「BUILD SUCCESS」，说明编译成功，生成target目录（存放编译后的class文件、war包等）。
测试业务层（可选）：编写Junit测试用例，测试UserService的方法，确保业务逻辑正确，执行「Maven test」，显示「BUILD SUCCESS」即测试通过。
排查编译报错：
若提示“找不到Servlet相关类”：检查servlet-api依赖是否下载成功，scope是否为provided。
若提示“JSTL标签无法识别”：检查JSTL依赖是否完整，JSP页面是否引入标签库。
若提示“中文乱码”：检查编译插件和Tomcat插件的编码配置是否为UTF-8。
步骤8：Maven打包（生成war包，用于部署）
编译测试通过后，通过Maven打包项目，生成war包，这是Web应用部署到Tomcat的核心文件，步骤如下：
右键点击项目→「Run As」→「Maven package」，Maven会自动执行「clean→compile→test→package」流程。
打包成功后，进入项目的target目录，可看到生成的war包（user-manage-web.war，与pom.xml中finalName一致）。
注意：若不想执行测试用例直接打包，可执行命令「mvn clean package -DskipTests」，避免测试用例失败导致打包失败。
步骤9：部署与测试（两种方式，后端开发常用）
Maven Web应用的部署有两种方式：「Maven一键部署」（便捷，开发时常用）和「手动部署」（稳定，生产环境常用），两种方式均详细讲解。
9.1 方式1：Maven一键部署（推荐，开发时使用）
通过pom.xml中配置的Tomcat插件，实现一键启动Tomcat、部署项目，无需手动操作，步骤如下：
右键点击项目→「Run As」→「Maven build...」，在弹出的窗口中，输入「tomcat7:run」（插件是tomcat7-maven-plugin，命令固定），点击「Run」。
控制台显示「Tomcat started on port(s): 8080 (http)」，说明Tomcat启动成功，项目已部署完成。
访问项目：打开浏览器，输入地址「http://localhost:8080/user-manage/user」（port是Tomcat端口，path是pom.xml中配置的访问路径），即可看到用户管理页面。
功能测试：
查询功能：页面正常显示模拟的3个用户数据，说明查询功能正常。
新增功能：填写姓名和年龄，点击「新增用户」，页面刷新后显示新增的用户，说明新增功能正常。
9.2 方式2：手动部署（生产环境常用）
手动部署是将打包后的war包复制到Tomcat的webapps目录，启动Tomcat自动解压部署，步骤如下：
停止本地Tomcat服务器（若已启动），进入Tomcat的webapps目录（如D:\apache-tomcat-8.5.90\webapps）。
将target目录下的user-manage-web.war包，复制到webapps目录中。
启动Tomcat（双击Tomcat的bin目录下的startup.bat），Tomcat会自动解压war包，生成user-manage-web目录（与war包名称一致）。
访问项目：打开浏览器，输入地址「http://localhost:8080/user-manage-web/user」（访问路径为war包名称+Servlet路径），验证功能正常即可。
注意：手动部署时，访问路径是war包名称（如user-manage-web），而非pom.xml中配置的path，若需修改访问路径，可将war包重命名（如重命名为user-manage.war），访问路径即为/user-manage。
三、后端开发中Maven Web应用高频问题及解决方案（避坑指南）
结合Java后端开发实际场景，整理Maven Web应用开发、打包、部署过程中的高频问题，给出具体解决方案，帮助开发者快速排查，避免影响开发进度。
问题1：Tomcat启动报错“ServletContextListener is already configured”
常见原因：servlet-api依赖的scope配置错误（未设置为provided），导致打包后的war包中包含servlet-api.jar，与Tomcat自带的servlet-api冲突。
解决方案：修改pom.xml中servlet-api和jsp-api的scope为provided，重新打包部署。
问题2：JSP页面中文乱码（表单提交的中文显示为???）
常见原因：未配置编码格式，或编码配置不统一（JSP页面、Servlet、Tomcat插件编码不一致）。
解决方案：
JSP页面：设置contentType为text/html;charset=UTF-8。
Servlet：在doPost方法中设置request.setCharacterEncoding("UTF-8")、response.setContentType("text/html;charset=UTF-8")。
Tomcat插件：配置uriEncoding为UTF-8。
问题3：Maven打包后，war包中没有class文件（target目录中有class文件）
常见原因：项目结构不规范，src/main/java目录未被识别为源代码目录，Maven编译时未将class文件打包到war包中。
解决方案：
Eclipse：右键点击src/main/java目录→「Build Path」→「Use as Source Folder」。
IDEA：右键点击src/main/java目录→「Mark Directory as」→「Sources Root」。
重新执行「Maven package」，打包后war包中会包含class文件。
问题4：访问Servlet时报“404 Not Found”
常见原因：Servlet访问路径配置错误、Tomcat部署路径错误、Servlet未被正确编译。
解决方案：
检查Servlet的访问路径（@WebServlet注解或web.xml中的配置），确保路径正确（如/user）。
检查访问地址：Maven一键部署时，地址为http://localhost:8080/path/ servlet路径（path是pom.xml中配置的path）；手动部署时，地址为http://localhost:8080/war包名称/servlet路径。
检查target目录中是否有Servlet的class文件，若没有，重新执行「Maven compile」。
问题5：JSTL标签无法识别，提示“Unknown tag (c:forEach)”
常见原因：JSTL依赖缺失、JSP页面未引入JSTL标签库、依赖版本不兼容。
解决方案：
检查pom.xml中是否包含jstl-api和standard两个依赖，确保版本正确。
在JSP页面顶部引入JSTL标签库：<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
重新下载依赖，执行「Maven clean package」，重新部署。
问题6：Tomcat插件启动报错“Address already in use”
常见原因：pom.xml中配置的Tomcat端口（如8080）已被其他程序占用（如本地已启动的Tomcat、其他服务）。
解决方案：
关闭占用端口的程序（如关闭本地Tomcat）。
修改pom.xml中Tomcat插件的port（如改为8081），重新启动插件。
四、总结与后续学习指引（后端视角）
4.1 核心总结
本文从Java后端开发角度，完整剖析了基于Maven开发Web应用的全流程，核心要点如下，也是后端开发中Maven Web应用的必备知识点：
项目结构：Maven Web应用有固定的标准结构（src/main/webapp、WEB-INF），必须遵循规范，否则会导致部署失败。
pom.xml配置：核心是Web相关依赖（servlet-api、jsp-api）、Tomcat插件、编译插件，其中依赖的scope配置（provided）是避免冲突的关键。
分层开发：遵循“实体层→业务层→控制层→前端页面”的分层规范，提升代码可维护性和可复用性，贴合后端开发习惯。
打包部署：掌握两种部署方式（Maven一键部署、手动部署），适配开发和生产环境，理解war包的作用和部署原理。
问题排查：掌握高频问题的解决方案，能快速排查编码、依赖、部署等方面的异常，提升开发效率。
核心认知：Maven在Web应用开发中的核心价值，是标准化项目结构、自动化依赖管理和构建部署，解决了传统Web开发中“依赖混乱、部署繁琐、版本冲突”的痛点，是Java后端Web开发的必备工具。
4.2 后续学习指引
本文讲解的是传统Maven Web应用（基于Servlet、JSP），后续可结合后端主流框架，进一步学习更复杂的Web应用开发，重点方向如下：
结合SpringMVC框架：学习SpringMVC与Maven的结合，通过Maven配置SpringMVC依赖，实现更灵活的请求处理、视图解析。

