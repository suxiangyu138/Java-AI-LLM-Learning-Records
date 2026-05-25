SpringMVC标签库（新手必备，衔接JSP实操）
结合此前学习的SpringMVC常用注解（@Controller、@RequestMapping等），我们知道Controller负责处理前端请求、调用业务逻辑并返回视图名称，而SpringMVC标签库则用于在视图层（主要是JSP页面）中，简化数据渲染、表单提交、请求跳转等操作，无需编写繁琐的Java代码（如Scriptlet脚本），实现视图与数据的快速绑定，是传统Web应用（JSP渲染视图）的核心工具。
补充衔接：此前我们讲解的Controller方法，返回的视图名称（如“userList”）会被视图解析器解析为具体的JSP页面，而JSP页面需要展示Controller传递的模型数据（如User列表、单个User信息），此时就需要用到SpringMVC标签库——它能直接获取Controller传递的模型数据，简化数据渲染、表单提交等操作，与Controller层的注解协同工作，形成“请求处理→数据传递→视图渲染”的完整链路。本教程仍基于Spring 5.x，延续User案例，重点讲解最常用的标签，标注使用场景和避坑点，适配新手实操。
一、SpringMVC标签库核心前提（必做）
使用SpringMVC标签库前，必须在JSP页面中导入标签库，否则标签无法被识别，这是新手最容易遗漏的步骤。核心导入语句如下（复制到JSP页面的头部，<html>标签之前）：
<%-- 导入SpringMVC核心标签库，prefix="c"是标签前缀（可自定义，常用c） --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%-- 可选：导入SpringMVC表单标签库（表单提交场景常用），prefix="form" --%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
说明：
核心标签库（prefix="c"）：最常用，用于数据渲染、循环、条件判断、请求跳转等，是所有JSP页面都需要导入的。
表单标签库（prefix="form"）：专门用于表单提交，可自动绑定模型数据到表单元素，简化表单开发（表单场景必用）。
prefix（标签前缀）：可自定义（如改为“spring”），但行业规范中，核心标签用“c”，表单标签用“form”，建议遵循规范，避免混乱。
二、核心标签库（prefix="c"，必掌握）
核心标签库是SpringMVC标签库的基础，涵盖数据渲染、循环、条件判断、URL跳转等常用功能，无需编写Java脚本，直接通过标签实现，简化JSP页面开发。
1. <c:out>：输出模型数据（最常用）
    作用
    用于将Controller传递的模型数据（如User对象、字符串、集合）输出到JSP页面，可避免HTML标签被解析（防XSS攻击），同时支持默认值设置（当数据为null时显示默认值）。
    核心属性
    value：指定要输出的模型数据（必填），格式为“${模型数据名}”（EL表达式，与SpringMVC模型数据绑定）。
    default：指定默认值（可选），当value对应的模型数据为null时，显示该默认值。
    escapeXml：是否转义HTML标签（可选，默认true），true表示转义（如<br/>显示为文本，不换行），false表示不转义（如<br/>生效，实现换行）。
    实操案例（结合User案例）
    前提：Controller方法传递模型数据到JSP页面，代码如下：
    @Controller
    @RequestMapping("/user")
    public class UserController {
    @GetMapping("/get/{id}")
    public String getUser(@PathVariable Integer id, Model model) {
        // 模拟查询用户，将User对象存入模型（key为"user"，value为User对象）
        User user = new User("张三", 20, "男");
        user.setId(id);
        model.addAttribute("user", user);
        // 传递字符串数据
        model.addAttribute("msg", "查询用户详情成功！");
        return "userDetail"; // 解析为userDetail.jsp页面
    }
    }
    JSP页面（userDetail.jsp）使用<c:out>输出数据：
    <%@ page contentType="text/html;charset=UTF-8" language="java" %>
    <%-- 导入核心标签库 --%>
    <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
    <html>
    <head>
    <title>用户详情</title>
    </head>
    <body>
    <h1>${msg}</h1> <%-- 直接用EL表达式输出，也可使用<c:out> --%>
    <hr/>
    <!-- 输出User对象的属性，默认值为空时显示"无" -->
    <p>用户ID：<c:out value="${user.id}" default="无"/></p>
    <p>用户姓名：<c:out value="${user.name}" default="无"/></p>
    <p>用户年龄：<c:out value="${user.age}" default="无"/></p>
    <p>用户性别：<c:out value="${user.gender}" default="无"/></p>
    <!-- 测试HTML转义：escapeXml=true（默认），<br/>显示为文本 -->
    <p>转义测试1：<c:out value="${user.name}<br/>性别：${user.gender}"/></p>
    <!-- escapeXml=false，<br/>生效，实现换行 -->
    <p>转义测试2：<c:out value="${user.name}<br/>性别：${user.gender}" escapeXml="false"/></p>
    </body>
    </html>
2. <c:forEach>：循环遍历集合（列表展示常用）
    作用
    用于遍历Controller传递的集合数据（如List<User>），将集合中的每个元素渲染到JSP页面，适合用户列表、商品列表等场景，替代Java脚本中的for循环。
    核心属性
    items：指定要遍历的集合（必填），格式为“${集合模型名}”（如${userList}）。
    var：指定集合中每个元素的变量名（必填），后续可通过“${变量名.属性}”获取元素属性（如${u.name}）。
    varStatus：可选，指定循环状态对象，可获取循环索引（index，从0开始）、循环次数（count，从1开始）等。
    实操案例（用户列表展示）
    前提：Controller方法传递用户列表到JSP页面：
    @GetMapping("/list")
    public String getUserList(Model model) {
    List&lt;User&gt; userList = new ArrayList<>();
    userList.add(new User("张三", 20, "男"));
    userList.add(new User("李四", 22, "女"));
    userList.add(new User("王五", 21, "男"));
    // 将用户列表存入模型（key为"userList"）
    model.addAttribute("userList", userList);
    return "userList"; // 解析为userList.jsp页面
    }
    JSP页面（userList.jsp）使用<c:forEach>遍历列表：
    <%@ page contentType="text/html;charset=UTF-8" language="java" %>
    <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
    <html>
    <head>
    <title>用户列表</title>
    </head>
    <body>
    <h1>用户列表</h1>
    <table border="1" width="80%" align="center">
        <tr>
            <th>序号</th>
            <th>ID</th>
            <th>姓名</th>
            <th>年龄</th>
            <th>性别</th>
        </tr>
        <!-- 遍历userList集合，每个元素变量名为u，varStatus获取循环状态 -->
        <c:forEach items="${userList}" var="u" varStatus="status">
            <tr>
                <!-- status.count：循环次数（从1开始），作为序号 -->
                <td>${status.count}</td>
                <td>${u.id}</td>
                <td>${u.name}</td>
                <td>${u.age}</td>
                <td>${u.gender}</td>
            </tr>
        </c:forEach>
    </table>
    </body>
    </html>
3. <c:if>：条件判断（动态渲染常用）
    作用
    用于根据条件动态渲染页面内容，当条件为true时，渲染标签体内的内容；条件为false时，不渲染，替代Java脚本中的if语句，适合根据数据状态展示不同内容（如性别判断、权限判断）。
    核心属性
    test：指定判断条件（必填），使用EL表达式（如${u.gender == '男'}），结果为boolean类型（true/false）。
    实操案例（结合用户列表，动态渲染性别样式）
    <!-- 在userList.jsp的<c:forEach>循环中添加条件判断 -->
    <c:forEach items="${userList}" var="u" varStatus="status">
    <tr>
        <td>${status.count}</td>
        <td>${u.id}</td>
        <td>${u.name}</td>
        <td>${u.age}</td>
        <!-- 条件判断：性别为男，显示蓝色；为女，显示粉色 -->
        <td>
            <c:if test="${u.gender == '男'}">
                <font color="blue">${u.gender}</font>
            </c:if>
            <c:if test="${u.gender == '女'}">
                <font color="pink">${u.gender}</font>
            </c:if>
        </td>
    </tr>
    </c:forEach>
4. <c:redirect>：请求重定向（页面跳转常用）
    作用
    用于实现JSP页面的重定向跳转，等同于Controller方法中的“return "redirect:/user/list"”，可跳转至Controller请求路径或其他JSP页面，适合表单提交后跳转、页面重定向场景。
    核心属性
    url：指定重定向的路径（必填），可是Controller的请求路径（如"/user/list"），也可是JSP页面路径（如"/userDetail.jsp"）。
    实操案例（表单提交后重定向）
    <%@ page contentType="text/html;charset=UTF-8" language="java" %>
    <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
    <html>
    <head>
    <title>新增用户成功</title>
    </head>
    <body>
    <h1>新增用户成功！</h1>
    <!-- 重定向到用户列表页（Controller的请求路径） -->
    <c:redirect url="/user/list"/>
    <!-- 也可重定向到指定JSP页面 -->
    <!-- <c:redirect url="/userDetail.jsp"/> -->
    </body>
    </html>
    三、表单标签库（prefix="form"，表单场景必掌握）
    表单标签库是SpringMVC专门为表单提交设计的标签，可自动绑定Controller传递的模型数据到表单元素（如input、select），无需手动设置value值，同时支持表单验证提示，简化表单开发，与@ModelAttribute注解协同工作。
    1. <form:form>：表单核心标签
    作用
    替代传统的<form>标签，用于创建表单，核心功能是绑定模型数据，自动设置表单的action（请求路径）、method（请求方法），并将模型数据绑定到表单元素上。
    核心属性
    modelAttribute：指定要绑定的模型数据名称（必填），与Controller中model.addAttribute("key", value)的key一致（如"user"）。
    action：指定表单提交的请求路径（可选），默认提交到当前页面的请求路径。
    method：指定表单提交的HTTP方法（可选，默认POST），与Controller的@PostMapping、@GetMapping等注解对应。
2. 常用表单元素标签（与HTML元素对应）
    表单标签库提供了与HTML元素对应的标签，可自动绑定模型数据，无需手动设置value，常用标签如下（结合新增/修改用户表单案例）：
    <form:input path="属性名">：对应HTML的<input type="text">，path指定模型对象的属性名（如"name"），自动绑定模型数据。
    <form:password path="属性名">：对应HTML的<input type="password">，用于密码输入。
    <form:radiobutton path="属性名" value="值">：对应HTML的<input type="radio">，用于单选框（如性别选择）。
    <form:select path="属性名">：对应HTML的<select>，用于下拉选择框。
    <form:textarea path="属性名">：对应HTML的<textarea>，用于多行文本输入。
    <form:button>：对应HTML的<button>，用于提交按钮。
    实操案例（新增/修改用户表单）
    前提：Controller方法传递空User对象（新增）或已有User对象（修改）到表单页面：
    // 新增用户：传递空User对象
    @GetMapping("/toAdd")
    public String toAdd(Model model) {
    // 传递空User对象，用于表单绑定（key为"user"）
    model.addAttribute("user", new User());
    return "addUser";
    }
    // 修改用户：传递已有User对象
    @GetMapping("/toUpdate/{id}")
    public String toUpdate(@PathVariable Integer id, Model model) {
    User user = new User("张三", 20, "男");
    user.setId(id);
    model.addAttribute("user", user); // 传递已有User对象，表单自动回显数据
    return "updateUser";
    }
    JSP表单页面（addUser.jsp，新增用户）：
    <%@ page contentType="text/html;charset=UTF-8" language="java" %>
    <%-- 导入核心标签库和表单标签库 --%>
    <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
    <%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
    <html>
    <head>
    <title>新增用户</title>
    </head>
    <body>
    <h1>新增用户</h1>
    <!-- form:form绑定模型数据user，提交到/user/add请求（POST方法） -->
    <form:form modelAttribute="user" action="/user/add" method="post">
        <p>
            姓名：<form:input path="name" placeholder="请输入姓名"/>
        </p>
        <p>
            年龄：<form:input path="age" placeholder="请输入年龄"/>
        </p>
        <p>
            性别：
            <form:radiobutton path="gender" value="男" label="男"/>
            <form:radiobutton path="gender" value="女" label="女"/>
        </p>
        <p>
            <form:button type="submit">新增用户</form:button>
        </p>
    </form:form>
    </body>
    </html>
    说明：修改用户页面（updateUser.jsp）与新增页面基本一致，唯一区别是：Controller传递的是已有User对象，表单会自动回显User的属性值（如姓名、年龄），无需手动设置，简化修改操作。
    四、新手避坑重点（高频错误，提前规避）
    忘记导入标签库：未在JSP页面导入核心标签库或表单标签库，导致标签无法识别，页面报错“无法识别的标签”。
    模型数据绑定错误：<c:out>、<form:input>等标签的value/path属性，需与Controller中model.addAttribute的key一致，否则无法获取数据（显示null或空值）。
    表单标签库使用错误：<form:form>必须指定modelAttribute属性，且该属性对应的模型数据必须存在（否则报错），新增用户时需传递空对象，修改时传递已有对象。
    EL表达式无效：JSP页面未开启EL表达式（默认开启），若关闭，需在页面头部添加<%@ page isELIgnored="false" %>，否则${}表达式无法解析。
    重定向路径错误：<c:redirect>的url属性，若跳转至Controller请求路径，需加“/”（如"/user/list"），否则会拼接当前页面路径，导致跳转失败（404错误）。
    循环标签使用错误：<c:forEach>的items属性必须是集合类型（如List），若传递的是单个对象，会报错“无法遍历非集合对象”。
    五、实操总结（新手必练）
    1. 核心流程：Controller传递模型数据 → JSP页面导入标签库 → 使用核心标签渲染数据/循环/跳转 → 表单场景使用表单标签绑定数据、提交请求。
    2. 场景适配：
    数据展示（列表、详情）：用<c:out>输出单个数据，<c:forEach>遍历集合，<c:if>动态渲染。
    表单提交（新增、修改）：用<form:form>绑定模型数据，搭配表单元素标签，简化数据回显和提交。
    页面跳转：用<c:redirect>实现重定向，与Controller的redirect跳转效果一致。
3. 衔接此前知识：SpringMVC标签库依赖Controller传递的模型数据，而Controller可通过@Autowired注入Service层Bean，实现“数据库查询→模型数据传递→视图渲染”的完整链路，新手可结合User案例，编写完整的“新增→查询→修改→删除”页面，熟练掌握标签用法。
    补充：目前前后端分离开发中，JSP已逐渐被Vue、React等前端框架替代，SpringMVC标签库主要用于传统Web应用；但作为SpringMVC的核心知识点，掌握标签库的用法，能更好地理解“视图与数据绑定”的逻辑，为后续学习前后端分离的数据交互奠定基础。
