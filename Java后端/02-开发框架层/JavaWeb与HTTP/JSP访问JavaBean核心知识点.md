JSP访问JavaBean核心知识点

---------------------------------------------------------------------------------------------------------------------------------------
一、JavaBean的核心定义
1. JavaBean是遵循特定规范的Java类，用于封装数据和业务逻辑。
2. 核心规范：
   - 必须有无参构造方法（默认构造器）；
   - 属性私有化，提供对应的getter/setter方法；
   - 实现Serializable接口（可选，用于序列化）。
3. 作用：在JSP中分离数据和视图，减少JSP中的Java代码，符合MVC设计思想。

---------------------------------------------------------------------------------------------------------------------------------------
二、JSP访问JavaBean的核心标签
1. <jsp:useBean>：创建/获取JavaBean对象
   基本语法：
   <jsp:useBean id="对象名" class="全类名" scope="作用域"/>
   说明：
   - id：指定JavaBean对象的名称，用于后续调用；
   - class：JavaBean的全限定类名（如com.example.User）；
   - scope：对象的作用域，可选值page/request/session/application，默认page。
   示例：
   <jsp:useBean id="user" class="com.example.User" scope="session"/>
2. <jsp:setProperty>：设置JavaBean的属性值
   语法1：直接赋值
   <jsp:setProperty name="对象名" property="属性名" value="属性值"/>
   语法2：从请求参数中取值（参数名与属性名一致）
   <jsp:setProperty name="对象名" property="属性名" param="请求参数名"/>
   语法3：自动匹配所有请求参数（参数名与属性名一致）
   <jsp:setProperty name="对象名" property="*"/>
   示例：
   <jsp:setProperty name="user" property="username" value="张三"/>
   <jsp:setProperty name="user" property="password" param="pwd"/>
   <jsp:setProperty name="user" property="*"/>
3. <jsp:getProperty>：获取JavaBean的属性值
   基本语法：
   <jsp:getProperty name="对象名" property="属性名"/>
   说明：自动调用对应的getter方法，输出属性值到页面。
   示例：
   用户名：<jsp:getProperty name="user" property="username"/>

---------------------------------------------------------------------------------------------------------------------------------------
三、JavaBean的作用域（与JSP四大作用域一致）
1. page：仅当前JSP页面有效，页面跳转后失效；
2. request：一次请求有效，服务器内部转发有效，重定向失效；
3. session：一次会话有效，浏览器关闭前均有效；
4. application：整个应用运行期间有效，服务器重启后失效。
    示例：
    <!-- 作用域为session，整个会话中可使用该user对象 -->
    <jsp:useBean id="user" class="com.example.User" scope="session"/>

---------------------------------------------------------------------------------------------------------------------------------------
四、JSP访问JavaBean的两种方式
1. 标签方式（推荐，减少Java代码）
   完整示例：
   <%-- 创建User对象，作用域为page --%>
   <jsp:useBean id="user" class="com.example.User" scope="page"/>
   <%-- 设置属性 --%>
   <jsp:setProperty name="user" property="username" value="李四"/>
   <jsp:setProperty name="user" property="age" value="20"/>
   <%-- 获取属性 --%>
   <h3>姓名：<jsp:getProperty name="user" property="username"/></h3>
   <h3>年龄：<jsp:getProperty name="user" property="age"/></h3>
2. 脚本方式（直接在JSP中编写Java代码）
   示例：
   <%
       // 创建JavaBean对象
       com.example.User user = new com.example.User();
       // 设置属性
       user.setUsername("王五");
       user.setAge(25);
       // 存入request作用域
       request.setAttribute("user", user);
   %>
   <%-- 读取属性 --%>
   <%
       com.example.User u = (com.example.User) request.getAttribute("user");
   %>
   <h3>姓名：<%= u.getUsername() %></h3>
   <h3>年龄：<%= u.getAge() %></h3>

---------------------------------------------------------------------------------------------------------------------------------------
五、核心示例（Bookstore场景）
1. 编写Book JavaBean
   package com.example;
   public class Book {
       // 私有属性
       private Integer id;
       private String name;
       private String author;
       private Double price;
       // 无参构造（必须）
       public Book() {}
       // getter/setter方法
       public Integer getId() { return id; }
       public void setId(Integer id) { this.id = id; }
       public String getName() { return name; }
       public void setName(String name) { this.name = name; }
       public String getAuthor() { return author; }
       public void setAuthor(String author) { this.author = author; }
       public Double getPrice() { return price; }
       public void setPrice(Double price) { this.price = price; }
   }
2. JSP中使用Book JavaBean
   <%-- 创建Book对象 --%>
   <jsp:useBean id="book" class="com.example.Book" scope="request"/>
   <%-- 设置属性 --%>
   <jsp:setProperty name="book" property="name" value="JavaWeb从入门到精通"/>
   <jsp:setProperty name="book" property="author" value="张三"/>
   <jsp:setProperty name="book" property="price" value="69.9"/>
   <%-- 展示书籍信息 --%>
   <div>
       <h2>书籍名称：<jsp:getProperty name="book" property="name"/></h2>
       <p>作者：<jsp:getProperty name="book" property="author"/></p>
       <p>价格：¥<jsp:getProperty name="book" property="price"/></p>
   </div>

---------------------------------------------------------------------------------------------------------------------------------------
六、常见问题与注意事项
1. 找不到JavaBean类：
   - 确保JavaBean的class路径正确（全类名）；
   - 确保编译后的class文件在WEB-INF/classes目录下。
2. 无参构造缺失：
   - JavaBean必须提供无参构造方法，否则<jsp:useBean>会报错。
3. 属性名与方法名不匹配：
   - getter/setter方法命名需遵循规范（如属性username对应getUsername()）。
4. 作用域选择：
   - 仅当前页面使用选page；
   - 转发页面共享选request；
   - 用户会话共享选session；
   - 全应用共享选application。
5. 避免在JSP中编写复杂业务逻辑：
   - JavaBean仅封装数据，业务逻辑建议放在Servlet/Service层。

---------------------------------------------------------------------------------------------------------------------------------------
七、核心总结
1. JavaBean是遵循规范的Java类，用于封装数据，减少JSP中的Java代码；
2. JSP通过<jsp:useBean>、<jsp:setProperty>、<jsp:getProperty>标签操作JavaBean；
3. JavaBean的作用域决定了对象的有效范围，需根据业务场景选择；
4. 核心规范：无参构造、私有属性+getter/setter方法；
5. 实际开发中，JavaBean配合Servlet使用，Servlet处理业务逻辑，JSP仅展示数据。

---------------------------------------------------------------------------------------------------------------------------------------
