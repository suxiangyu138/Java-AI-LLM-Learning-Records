03.13 17:04
Java后端开发中 HTTP 核心知识点总结
一、HTTP 基础概念
1. HTTP 定义：超文本传输协议，是基于 TCP/IP 的无状态应用层协议，用于客户端与服务器之间的通信。
2. 核心特点：无状态（每次请求独立，默认不保存会话）、无连接（HTTP 1.0 每次请求建立一次连接，1.1 支持长连接  Keep-Alive ）。
3. 请求-响应模型：客户端发送请求报文，服务器返回响应报文，一次请求对应一次响应。
4. 常用版本：HTTP 1.1（主流，支持长连接、管道化请求）、HTTP 2（多路复用、二进制帧、头部压缩）、HTTP 3（基于 UDP 的 QUIC 协议）。
二、HTTP 请求报文结构
请求报文由 请求行、请求头、空行、请求体 四部分组成
1. 请求行： 请求方法  请求URL  协议版本 
- 常用请求方法： GET （查询数据，参数在 URL 中，长度受限）、 POST （提交数据，参数在请求体，支持大数据）、 PUT （更新资源）、 DELETE （删除资源）、 HEAD （仅获取响应头）。
- 示例： GET /user/1 HTTP/1.1 
2. 请求头：键值对形式，携带请求附加信息
- 常用头字段： Host （目标服务器域名）、 User-Agent （客户端信息）、 Content-Type （请求体数据格式，如  application/json 、 application/x-www-form-urlencoded ）、 Cookie （客户端会话数据）。
3. 空行：分隔请求头和请求体，必须存在。
4. 请求体：仅  POST/PUT  等方法有，存放提交的参数数据。
三、HTTP 响应报文结构
响应报文由 状态行、响应头、空行、响应体 四部分组成
1. 状态行： 协议版本  状态码  状态描述 
- 状态码分类：
- 1xx：提示信息，如 100（继续发送请求体）
- 2xx：成功，如 200（OK）、201（创建资源成功）
- 3xx：重定向，如 301（永久重定向）、302（临时重定向）、304（资源未修改，使用缓存）
- 4xx：客户端错误，如 400（请求参数错误）、401（未授权）、403（禁止访问）、404（资源不存在）
- 5xx：服务器错误，如 500（服务器内部异常）、503（服务不可用）
2. 响应头：键值对形式，携带响应附加信息
- 常用头字段： Content-Type （响应体数据格式）、 Content-Length （响应体长度）、 Set-Cookie （服务器向客户端设置 Cookie）、 Cache-Control （缓存控制，如  max-age=3600 ）。
3. 空行：分隔响应头和响应体。
4. 响应体：服务器返回的具体数据，如 HTML 页面、JSON 字符串。
四、Java 后端处理 HTTP 的核心技术
1. Servlet 技术
- 核心接口： Servlet （处理请求）、 HttpServletRequest （封装请求信息）、 HttpServletResponse （封装响应信息）。
- 关键方法： req.getMethod() （获取请求方法）、 req.getParameter() （获取请求参数）、 resp.setStatus() （设置响应状态码）、 resp.getWriter().write() （返回响应体）。
- 示例代码片段：
java
@WebServlet("/hello")
public class HelloServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=utf-8");
        resp.getWriter().write("Hello HTTP!");
    }
}
 
2. Spring MVC 处理 HTTP
- 核心注解：
-  @RequestMapping （映射请求路径和方法，可指定  method = RequestMethod.GET ）
-  @GetMapping/@PostMapping （简化 GET/POST 请求映射）
-  @RequestParam （获取 URL 参数）、 @RequestBody （获取请求体 JSON 数据）
-  @ResponseBody （将返回值转为 JSON 写入响应体）
- 示例代码片段：
java
@RestController
public class UserController {
    @GetMapping("/user/{id}")
    public String getUserById(@PathVariable Integer id) {
        return "User ID: " + id;
    }
}
 
3. HTTP 客户端工具
- 后端调用外部接口时使用，常用工具：
-  HttpURLConnection （JDK 自带，原生工具）
-  HttpClient （Apache 提供，功能强大）
-  RestTemplate （Spring 提供，简化 RESTful 接口调用）
-  WebClient （Spring 5 非阻塞客户端）
- 示例（RestTemplate）：
java
@Autowired
private RestTemplate restTemplate;
public String callApi() {
    return restTemplate.getForObject("http://localhost:8080/hello", String.class);
}
 
五、HTTP 会话管理
1. Cookie：客户端存储技术，服务器通过  Set-Cookie  响应头设置，客户端后续请求自动携带。
2. Session：服务器端存储技术，基于 Cookie 传递  JSESSIONID  标识会话，Spring MVC 中可通过  HttpSession  对象操作。
3. Token 认证：无状态会话方案，如 JWT（JSON Web Token），客户端请求时在请求头  Authorization: Bearer <token>  携带。
六、HTTP 常见优化与安全
1. 优化手段：启用 HTTP 长连接、开启 Gzip 压缩、合理设置缓存头（ Cache-Control / Expires ）、使用 HTTP 2 多路复用。
2. 安全相关：
- HTTPS：HTTP + SSL/TLS，加密传输，防止数据篡改和窃听。
- 防止常见攻击：XSS（过滤请求参数）、CSRF（使用 Token 验证）、SQL 注入（使用预编译语句）。

