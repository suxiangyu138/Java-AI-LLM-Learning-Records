03.26 19:14
Servlet详细知识点剖析
Servlet（Server Applet）是Java EE（现Jakarta EE）规范中定义的一种服务器端Java程序，运行在Web服务器（如Tomcat、Jetty）中，核心作用是接收客户端（浏览器、Postman等）的HTTP请求，处理业务逻辑，生成HTTP响应，是Java Web开发的基础组件，也是Spring MVC等主流Web框架的底层核心依赖。本次将从核心定义、生命周期、核心API、工作原理、实操细节、常见问题等维度，对Servlet进行全面、细致的剖析，兼顾理论与实操，助力开发者吃透Servlet核心知识点。
一、Servlet核心定义与定位
1. 核心定义
Servlet是一个遵循Jakarta Servlet规范（原Java Servlet规范）的Java类，由Web服务器负责加载、实例化、初始化和销毁，它不能独立运行，必须依赖Web容器（Web Server）提供的运行环境，本质是“服务器端的Java小程序”，专门用于处理客户端的HTTP请求。
2. 核心定位
连接客户端与服务器：作为客户端（HTTP请求）和服务器（业务逻辑、数据库）之间的中间层，接收客户端请求，转发请求到对应的业务逻辑，再将处理结果封装为HTTP响应返回给客户端。
替代传统CGI：相比CGI（Common Gateway Interface），Servlet具有跨平台、性能高、可复用、支持多线程的优势，解决了CGI每次请求都创建新进程、资源消耗大的问题。
底层基础组件：Spring MVC的DispatcherServlet、Struts2的ActionServlet等，本质都是基于Servlet规范实现的，掌握Servlet是理解Java Web框架底层原理的关键。
3. 核心特性
跨平台：基于Java语言，依托JVM和Web容器，可运行在Windows、Mac、Linux等所有支持Java的系统。
多线程：Web容器会为每个Servlet请求分配一个独立的线程（而非进程），单个Servlet实例可同时处理多个请求，性能优于CGI。
可复用：Servlet实例由Web容器创建，且默认是单例（多线程共享一个实例），无需重复创建对象，减少资源消耗。
遵循规范：必须实现javax.servlet.Servlet接口（或继承其实现类），遵循Servlet规范的生命周期和API定义，确保与不同Web容器的兼容性。
二、Servlet的生命周期（核心重点）
Servlet的生命周期完全由Web容器（如Tomcat）管理，从创建到销毁分为5个核心阶段，每个阶段对应Servlet接口的特定方法，是面试高频考点，需重点掌握。
1. 生命周期5个阶段（按执行顺序）
（1）加载阶段（Load）
当Web容器（Tomcat）启动时，会扫描Web应用的WEB-INF/classes目录或WEB-INF/lib目录下的Servlet类，将Servlet类加载到JVM中（类加载器完成）。 触发时机：Web容器启动时（默认），或第一次请求该Servlet时（可通过配置修改）。
（2）实例化阶段（Instantiate）
Web容器通过反射机制，调用Servlet类的无参构造方法，创建Servlet实例。 关键细节：① Servlet默认是单例模式，一个Servlet类在Web容器中只会创建一个实例，多线程共享该实例；② 必须提供无参构造方法，若自定义有参构造，必须显式定义无参构造，否则Web容器无法实例化，会抛出异常；③ 实例化仅执行一次，除非Web容器重启或Servlet被重新加载。
（3）初始化阶段（Initialize）
实例化完成后，Web容器调用Servlet的init(ServletConfig config)方法，完成Servlet的初始化工作（如加载配置文件、初始化数据库连接池、创建全局变量等）。 关键细节：① init方法仅执行一次，在实例化后立即执行；② ServletConfig参数用于获取Servlet的配置信息（如初始化参数、ServletContext对象）；③ 若初始化失败（如抛出ServletException），Web容器会销毁该Servlet实例，不再处理该Servlet的请求。
（4）服务阶段（Service）
当客户端发送请求到该Servlet时，Web容器会为该请求分配一个线程，调用Servlet的service(ServletRequest request, ServletResponse response)方法，处理请求并生成响应。 关键细节：① service方法是Servlet处理请求的核心方法，每次请求都会执行一次（多线程并发执行）；② ServletRequest封装客户端请求信息（如请求参数、请求头、请求方式），ServletResponse用于封装服务器响应信息（如响应头、响应体、状态码）；③ service方法内部会根据请求方式（GET、POST、PUT、DELETE等），自动调用对应的doGet()、doPost()等方法（需开发者重写）；④ 多线程共享Servlet实例，需注意线程安全（如避免使用全局变量，若使用需加锁）。
（5）销毁阶段（Destroy）
当Web容器关闭、Web应用卸载，或Servlet被移除时，Web容器会调用Servlet的destroy()方法，释放Servlet占用的资源（如关闭数据库连接、销毁线程池、释放全局变量等）。 关键细节：① destroy方法仅执行一次，在Servlet销毁前执行；② 执行destroy方法后，Servlet实例会被JVM的垃圾回收机制回收；③ 即使destroy方法执行，仍可能有未完成的请求线程在执行，需在destroy方法中处理线程中断或资源释放逻辑。
2. 生命周期流程图（简化）
Web容器启动 → 加载Servlet类 → 实例化（无参构造） → 初始化（init） → 服务（service → doGet/doPost等，多次执行） → 销毁（destroy） → 实例回收
3. 生命周期核心注意事项
单例特性：Servlet默认单例，多线程共享实例，因此不能在Servlet中定义可修改的全局变量（如private int count;），否则会出现线程安全问题（如多个线程同时修改count，导致数据错乱），若必须使用，需通过synchronized关键字加锁。
init方法执行时机：默认Web容器启动时初始化，可通过web.xml中<servlet>标签的<load-on-startup>属性修改，值为非负整数，值越小，初始化优先级越高；若值为负数，則第一次请求时初始化。
destroy方法：仅在Web容器正常关闭时执行，若Web容器异常崩溃（如断电），destroy方法不会执行，因此不能依赖destroy方法完成关键资源的释放（如数据持久化）。
三、Servlet核心API（接口与实现类）
Servlet的核心API主要包含3个接口（Servlet、ServletConfig、ServletContext）和1个抽象类（GenericServlet）、1个常用实现类（HttpServlet），所有自定义Servlet都需直接或间接实现这些接口/继承这些类。
1. Servlet接口（核心顶层接口）
Servlet接口是所有Servlet的顶层接口，定义了Servlet生命周期的核心方法，必须实现以下5个方法：
void init(ServletConfig config)：初始化方法，仅执行一次。
void service(ServletRequest request, ServletResponse response)：服务方法，每次请求执行一次。
void destroy()：销毁方法，仅执行一次。
ServletConfig getServletConfig()：获取Servlet的配置对象（ServletConfig），用于获取初始化参数。
String getServletInfo()：返回Servlet的相关信息（如作者、版本、描述），默认返回空字符串，可重写。
注意：直接实现Servlet接口需重写所有5个方法，操作繁琐，因此实际开发中很少直接实现，而是继承GenericServlet或HttpServlet。
2. GenericServlet抽象类
GenericServlet是Servlet接口的抽象实现类，实现了Servlet接口中除service()以外的所有方法，简化了Servlet的开发，核心作用：
实现了init(ServletConfig config)方法，将ServletConfig对象保存为成员变量，并提供getServletConfig()方法供开发者调用。
提供了getServletContext()方法，间接获取ServletContext对象（通过ServletConfig获取）。
重写了getServletInfo()方法，返回默认的Servlet信息（可进一步重写）。
注意：GenericServlet是通用的Servlet，不针对HTTP请求，因此实际开发中，主要用于非HTTP协议的Servlet开发（极少用），HTTP相关的Servlet均继承HttpServlet。
3. HttpServlet抽象类（最常用）
HttpServlet是GenericServlet的子类，专门用于处理HTTP请求，是实际开发中最常用的Servlet类，核心优化：
重写了service(ServletRequest request, ServletResponse response)方法，将参数强制转换为HttpServletRequest和HttpServletResponse（HTTP专属请求/响应对象），并根据请求方式（GET、POST等），自动调用对应的doXXX()方法。
提供了一系列doXXX()方法，对应HTTP的请求方式：doGet(HttpServletRequest request, HttpServletResponse response)、doPost()、doPut()、doDelete()、doHead()等，开发者只需重写对应请求方式的doXXX()方法，无需重写service()方法。
核心流程：客户端发送HTTP请求 → Web容器调用service()方法 → service()方法判断请求方式 → 调用对应doXXX()方法 → 处理请求并生成响应。
注意：① 若未重写对应请求方式的doXXX()方法，默认会调用HttpServlet的父类方法，返回405状态码（Method Not Allowed，方法不允许）；② doGet()和doPost()是最常用的两个方法，分别处理GET和POST请求。
4. ServletConfig接口（Servlet配置对象）
ServletConfig是每个Servlet的专属配置对象，由Web容器创建，在init()方法中传入，用于获取当前Servlet的配置信息，核心方法：
String getInitParameter(String name)：获取指定名称的初始化参数（在web.xml或注解中配置）。
Enumeration<String> getInitParameterNames()：获取所有初始化参数的名称。
ServletContext getServletContext()：获取Servlet上下文对象（ServletContext），用于共享整个Web应用的资源。
String getServletName()：获取当前Servlet的名称（在web.xml或注解中配置）。
配置方式：可通过web.xml的<servlet>标签配置初始化参数，或通过@WebServlet注解的initParams属性配置。
5. ServletContext接口（Servlet上下文对象）
ServletContext是整个Web应用的全局上下文对象，由Web容器创建，一个Web应用只有一个ServletContext对象，所有Servlet共享该对象，核心作用是共享Web应用的全局资源，核心方法：
String getInitParameter(String name)：获取Web应用的全局初始化参数（在web.xml的<context-param>标签中配置）。
Enumeration<String> getInitParameterNames()：获取所有Web应用全局初始化参数的名称。
void setAttribute(String name, Object value)：设置全局共享属性（所有Servlet均可获取）。
Object getAttribute(String name)：获取全局共享属性。
void removeAttribute(String name)：移除全局共享属性。
String getRealPath(String path)：获取Web应用中指定资源的真实路径（服务器上的物理路径）。
ServletContext getContext(String uripath)：获取其他Web应用的ServletContext对象（跨应用共享，需配置跨应用访问权限）。
注意：① ServletContext的生命周期与Web应用一致，Web应用启动时创建，关闭时销毁；② 全局属性是所有Servlet共享的，多线程环境下需注意线程安全；③ getRealPath()方法返回的路径是服务器上的物理路径，开发时需避免硬编码路径，通过该方法动态获取。
四、Servlet的配置方式（两种核心方式）
Servlet的配置核心是指定“请求路径”与“Servlet类”的映射关系，让Web容器知道“客户端请求哪个路径，对应调用哪个Servlet”，有两种配置方式：XML配置（传统方式）和注解配置（主流方式）。
1. XML配置方式（web.xml）
传统配置方式，适用于所有版本的Servlet（Servlet 3.0之前唯一配置方式），核心配置标签：<servlet>和<servlet-mapping>。
配置示例：
<!-- 配置Servlet类信息 -->
<servlet>
    <servlet-name>MyServlet</servlet-name> <!-- Servlet名称，唯一 -->
    <servlet-class>com.example.MyServlet&lt;/servlet-class&gt; <!-- Servlet全类名 -->
    <init-param> <!-- 配置当前Servlet的初始化参数（可选） -->
        <param-name>encoding</param-name>
        <param-value>UTF-8</param-value>
    </init-param>
    <load-on-startup&gt;1&lt;/load-on-startup&gt; <!-- 初始化时机，非负整数，值越小优先级越高 -->
</servlet>
<!-- 配置Servlet映射路径（请求路径） -->
<servlet-mapping>
    <servlet-name>MyServlet</servlet-name&gt; <!-- 与上面的servlet-name一致 -->
    <url-pattern>/myServlet&lt;/url-pattern&gt; <!-- 请求路径，核心，如http://localhost:8080/项目名/myServlet -->
</servlet-mapping>
<!-- 配置Web应用全局初始化参数（可选） -->
<context-param>
    <param-name>globalEncoding</param-name>
    <param-value>UTF-8</param-value>
</context-param>
关键细节：① <url-pattern>是核心，用于指定请求路径，支持通配符（如*.do、/user/*）；② 一个Servlet可配置多个<url-pattern>，即多个请求路径对应同一个Servlet；③ <load-on-startup>属性若不配置，默认第一次请求时初始化。
2. 注解配置方式（@WebServlet）
Servlet 3.0及以上版本支持的配置方式，替代XML配置，简化开发，核心注解是@WebServlet，直接标注在Servlet类上。
注解属性（常用）：
name：对应XML中的<servlet-name>，Servlet名称，可选。
urlPatterns/value：对应XML中的<url-pattern>，请求路径，必填（value是urlPatterns的别名，可省略urlPatterns，直接写value）。
initParams：对应XML中的<init-param>，当前Servlet的初始化参数，可选，格式为@WebInitParam(name="", value="")。
loadOnStartup：对应XML中的<load-on-startup>，初始化时机，可选，默认-1（第一次请求时初始化）。
asyncSupported：是否支持异步处理，可选，默认false。
配置示例：
// 注解配置Servlet，请求路径为/myServlet和/hello
@WebServlet(
    name = "MyServlet",
    value = {"/myServlet", "/hello"},
    initParams = {@WebInitParam(name = "encoding", value = "UTF-8")},
    loadOnStartup = 1
)
public class MyServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 处理GET请求
        response.getWriter().write("Hello Servlet!");
    }
}
注意：① 注解配置和XML配置可混合使用，但同一Servlet不能同时用两种方式配置，否则会报错；② Servlet 3.0及以上版本，Web容器会自动扫描带有@WebServlet注解的类，无需在web.xml中配置扫描。
3. 路径匹配规则（url-pattern）
url-pattern的路径匹配是Servlet配置的核心，Web容器会根据请求路径，匹配对应的Servlet，匹配规则优先级从高到低：
精确匹配：路径完全一致，如<url-pattern>/myServlet</url-pattern>，仅匹配http://localhost:8080/项目名/myServlet。
目录匹配：以“/”开头，以“/*”结尾，如<url-pattern>/user/*</url-pattern>，匹配所有以/user/开头的路径（如/user/login、/user/register）。
扩展名匹配：以“*.”开头，如<url-pattern>*.do</url-pattern>，匹配所有以.do结尾的路径（如/login.do、/query.do），注意：不能写为/*.do（会报错）。
默认匹配：<url-pattern>/</url-pattern>，匹配所有未被其他Servlet匹配的路径，优先级最低，通常用于处理静态资源（如HTML、CSS、JS）或作为默认Servlet。
易错点：① 目录匹配和扩展名匹配不能混合使用（如<url-pattern>/user/*.do</url-pattern>是非法的）；② 若多个Servlet的url-pattern匹配同一个请求路径，按匹配优先级执行，优先级高的先执行。
五、Servlet的请求与响应处理（核心实操）
Servlet处理请求的核心是通过HttpServletRequest获取客户端请求信息，通过HttpServletResponse生成服务器响应信息，两者均由Web容器创建，传入doGet()、doPost()等方法中。
1. HttpServletRequest（请求对象）
HttpServletRequest封装了客户端发送的HTTP请求信息，核心方法（常用）：
（1）获取请求行信息
String getMethod()：获取HTTP请求方式（GET、POST、PUT等）。
String getRequestURI()：获取请求URI（统一资源标识符），如/项目名/myServlet。
String getRequestURL()：获取请求URL（统一资源定位符），如http://localhost:8080/项目名/myServlet。
String getProtocol()：获取HTTP协议版本（如HTTP/1.1）。
（2）获取请求头信息
String getHeader(String name)：获取指定名称的请求头（如User-Agent、Content-Type）。
Enumeration<String> getHeaders(String name)：获取指定名称的所有请求头（如多个Cookie）。
Enumeration<String> getHeaderNames()：获取所有请求头的名称。
int getIntHeader(String name)：获取指定名称的请求头（整数类型，如Content-Length）。
（3）获取请求参数（核心）
String getParameter(String name)：获取指定名称的请求参数（适用于单值参数，如用户名、密码）。
String[] getParameterValues(String name)：获取指定名称的所有请求参数（适用于多值参数，如复选框）。
Map<String, String[]> getParameterMap()：获取所有请求参数的键值对（适用于批量获取参数）。
注意：① GET请求的参数拼接在URL后面，POST请求的参数放在请求体中，两种方式均可通过上述方法获取；② 若请求参数存在中文，会出现乱码，需设置请求编码（POST请求：request.setCharacterEncoding("UTF-8")；GET请求需在服务器配置文件中设置编码，如Tomcat的server.xml中配置URIEncoding="UTF-8"）。
（4）其他常用方法
ServletContext getServletContext()：获取ServletContext对象。
HttpSession getSession()：获取HttpSession对象（用于会话管理）。
String getRemoteAddr()：获取客户端IP地址。
void setAttribute(String name, Object value)：设置请求域属性（仅在当前请求中有效，转发时可共享）。
Object getAttribute(String name)：获取请求域属性。
RequestDispatcher getRequestDispatcher(String path)：获取请求转发器（用于请求转发）。
2. HttpServletResponse（响应对象）
HttpServletResponse用于封装服务器发送给客户端的HTTP响应信息，核心方法（常用）：
（1）设置响应行信息
void setStatus(int sc)：设置HTTP响应状态码（如200（成功）、404（资源不存在）、500（服务器内部错误）、405（方法不允许））。
（2）设置响应头信息
void setHeader(String name, String value)：设置指定名称的响应头（如Content-Type、Refresh、Set-Cookie）。
void addHeader(String name, String value)：添加响应头（若该响应头已存在，会新增一个，而setHeader会覆盖）。
void setContentType(String type)：设置响应内容类型（如text/html;charset=UTF-8，用于解决响应中文乱码）。
void setCharacterEncoding(String charset)：设置响应编码（如UTF-8）。
（3）设置响应体（核心）
PrintWriter getWriter()：获取字符输出流，用于输出字符类型的响应体（如HTML、JSON字符串）。
ServletOutputStream getOutputStream()：获取字节输出流，用于输出字节类型的响应体（如图片、文件下载）。
注意：① getWriter()和getOutputStream()不能同时使用，否则会抛出IllegalStateException异常；② 输出中文时，需先设置响应编码（response.setContentType("text/html;charset=UTF-8")），再获取输出流，否则会出现乱码；③ 响应体输出后，Web容器会自动关闭输出流，无需手动关闭，但手动关闭也不会报错。
（4）请求转发与重定向（核心跳转方式）
Servlet处理请求后，常需要跳转到其他页面或Servlet，有两种跳转方式：请求转发（forward）和重定向（sendRedirect），两者核心区别是“是否改变请求地址”和“是否共享请求域属性”。
对比维度
请求转发（forward）
重定向（sendRedirect）
跳转原理
服务器内部跳转，客户端只发送一次请求，服务器内部转发请求到目标资源
客户端跳转，服务器返回302状态码和目标地址，客户端重新发送请求到目标地址
请求地址
浏览器地址栏不变（显示原请求地址）
浏览器地址栏改变（显示目标地址）
请求域属性
共享（转发前后的Servlet可通过request.getAttribute()获取同一属性）
不共享（两次请求是独立的，请求域属性会被销毁）
跳转范围
仅能跳转当前Web应用内部的资源（不能跨应用、不能跨域名）
可跳转当前Web应用内部、其他Web应用、跨域名资源
代码实现
request.getRequestDispatcher("/target").forward(request, response);
response.sendRedirect("/项目名/target");
易错点：① 请求转发的路径是“服务器内部路径”，无需加项目名；重定向的路径是“客户端路径”，若跳转当前Web应用内部资源，需加项目名；② 重定向跳转外部资源时，需写完整URL（如response.sendRedirect("https://www.baidu.com")）。
六、Servlet的线程安全问题（高频考点）
1. 线程安全问题的产生原因
Servlet默认是单例模式，Web容器启动时创建一个Servlet实例，后续所有请求都由该实例处理，Web容器为每个请求分配一个独立的线程，多个线程同时操作同一个Servlet实例，若实例中存在“可修改的全局变量”，就会出现线程安全问题（如多个线程同时修改全局变量，导致数据错乱）。
示例（线程不安全）：
@WebServlet("/unsafeServlet")
public class UnsafeServlet extends HttpServlet {
    // 可修改的全局变量（线程共享）
    private int count = 0;
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        count++; // 多个线程同时修改count，会出现数据错乱
        response.getWriter().write("当前访问次数：" + count);
    }
}
问题：多个客户端同时访问该Servlet，count的值会出现错乱（如预期访问3次，实际显示2次或4次），因为多个线程同时执行count++，存在竞态条件。
2. 线程安全问题的解决方案
避免使用可修改的全局变量：将变量定义在doGet()、doPost()等方法内部（局部变量），局部变量是线程私有的，每个线程都有独立的副本，不会出现线程安全问题（推荐方案）。
使用synchronized关键字加锁：对共享资源（全局变量）的操作进行加锁，确保同一时刻只有一个线程能执行该操作，避免竞态条件。示例：
@WebServlet("/safeServlet")
public class SafeServlet extends HttpServlet {
    private int count = 0;
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        synchronized (this) { // 对当前Servlet实例加锁
            count++;
            response.getWriter().write("当前访问次数：" + count);
        }
    }
}
注意：加锁会降低性能，尽量缩小锁的范围（仅对共享资源的操作加锁，而非整个doGet()方法）。
使用线程安全的容器：若必须使用共享资源，可使用线程安全的容器（如ConcurrentHashMap、Vector），替代非线程安全的容器（如HashMap、ArrayList）。
设置Servlet为多实例（不推荐）：通过在web.xml中配置<servlet>标签的<load-on-startup>属性为负数，且不使用单例模式，但这种方式会增加资源消耗，违背Servlet的设计初衷，极少使用。
七、Servlet的常见问题与解决方案
1. 中文乱码问题（最常见）
原因：客户端请求编码、服务器处理编码、响应编码不一致，导致中文显示乱码，分两种场景解决：
GET请求乱码：① 方式一：在Tomcat的server.xml中，找到<Connector>标签，添加URIEncoding="UTF-8"（推荐，一劳永逸）；② 方式二：对获取的参数进行编码转换（new String(request.getParameter("name").getBytes("ISO-8859-1"), "UTF-8")）。
POST请求乱码：在doPost()方法开头，添加request.setCharacterEncoding("UTF-8")，设置请求编码为UTF-8，同时确保响应编码也设置为UTF-8（response.setContentType("text/html;charset=UTF-8")）。
2. 404错误（资源未找到）
常见原因及解决方案：
请求路径错误：检查url-pattern配置是否正确，确保请求路径与配置的路径一致（注意是否加项目名、是否有多余的斜杠）。
Servlet类全类名配置错误：检查web.xml中<servlet-class>或@WebServlet注解的类路径是否正确，确保类名拼写无误、包路径正确。
Servlet未被Web容器加载：检查Servlet类是否带有@WebServlet注解（Servlet 3.0+），或web.xml中是否配置了该Servlet；检查Web应用是否正确部署到Web容器中。
静态资源（HTML、CSS等）路径错误：确保静态资源放在Web应用的webapp目录下，请求路径正确（如http://localhost:8080/项目名/index.html）。
3. 405错误（方法不允许）
原因：客户端发送的请求方式（如POST），对应的Servlet未重写该方式的doXXX()方法（如未重写doPost()），默认调用HttpServlet的doPost()方法，返回405错误。
解决方案：重写对应请求方式的doXXX()方法，若不需要处理该请求方式，可在方法中返回405状态码或提示信息。
4. 500错误（服务器内部错误）
常见原因及解决方案：
Servlet代码异常：检查doGet()、doPost()等方法中是否有未捕获的异常（如空指针异常、数据库连接异常），添加try-catch捕获异常，或在方法上声明throws异常。
初始化失败：检查init()方法中是否有异常抛出，确保初始化逻辑（如加载配置文件、创建数据库连接）正确，避免初始化失败。
Web容器配置错误：检查Tomcat等Web容器的配置文件（如server.xml），确保配置正确，避免容器启动失败。
八、Servlet的进阶知识点
1. Servlet的异步处理
Servlet 3.0及以上版本支持异步处理，核心作用是解决“长耗时操作（如数据库查询、远程调用）阻塞线程”的问题，提升服务器并发性能。
核心原理：客户端发送请求后，Web容器分配一个线程处理请求，若遇到长耗时操作，可将该操作交给异步线程处理，原线程释放，继续处理其他请求；异步操作完成后，再生成响应返回给客户端。
实现步骤：
① 在@WebServlet注解中设置asyncSupported = true；
② 在doGet()/doPost()方法中，通过request.startAsync()获取AsyncContext对象；
③ 将长耗时操作交给线程池处理；
④ 异步操作完成后，通过AsyncContext.complete()完成响应。
2. Servlet过滤器（Filter）
Filter是Servlet规范中的另一个核心组件，用于拦截Servlet的请求和响应，可实现统一的请求处理（如编码过滤、权限验证、日志记录），运行在Servlet的service()方法之前。
核心特点：
① 可拦截多个Servlet，实现统一处理；
② 可链式调用，多个Filter按配置顺序执行；
③ 不直接处理请求和响应，仅对请求和响应进行预处理或后处理。
3. Servlet监听器（Listener）
Listener用于监听Web应用的生命周期（如ServletContext、HttpSession、ServletRequest的创建和销毁），以及属性的变化，可实现一些全局的业务逻辑（如统计在线人数、加载全局配置）。
常见监听器：ServletContextListener（监听Web应用的启动和关闭）、HttpSessionListener（监听会话的创建和销毁）、ServletRequestListener（监听请求的创建和销毁）。
九、总结
Servlet是Java Web开发的底层基础，核心是遵循Jakarta Servlet规范，由Web容器管理其生命周期，通过HttpServletRequest和HttpServletResponse处理请求与响应。掌握Servlet的核心知识点（生命周期、核心API、配置方式、请求响应处理、线程安全），不仅能独立开发简单的Java Web应用，更是理解Spring MVC等主流Web框架底层原理的关键。
核心重点回顾：
① 生命周期5个阶段，尤其是init、service、destroy的执行时机和作用；
② HttpServlet的doGet()、doPost()方法，以及请求转发与重定向的区别；
③ 线程安全问题的产生原因和解决方案；
④ 中文乱码、404、405、500等常见问题的排查与解决。

