Java网络编程：URL和URI详细知识点剖析
一、核心概念：URI与URL的定义及本质区别
1.1 核心定义
1.1.1 URI（Uniform Resource Identifier，统一资源标识符）
URI是用于唯一标识互联网上某一资源的字符串，核心作用是“识别”资源，不必然包含资源的访问方式。它是一个抽象的概念，范围更广，包含了URL和URN（Uniform Resource Name，统一资源名称）两种类型。
简单来说，URI的核心是“标识”——只要能唯一区分一个资源，无论是否能访问它，都属于URI。例如：
mailto:test@163.com（标识一个邮箱地址，无访问协议）
urn:isbn:9787111641247（标识一本书的ISBN，仅用于识别，无法直接访问）
https://www.baidu.com（既能标识资源，也能访问，属于URI的子集URL）
1.1.2 URL（Uniform Resource Locator，统一资源定位符）
URL是URI的子集，不仅能唯一标识资源，还包含了访问该资源的具体方式和路径（即“定位”资源）。它必须指定协议（如HTTP、HTTPS、FTP），通过协议和路径能直接访问到目标资源。
URL的核心是“定位+访问”——不仅告诉你资源是什么，还告诉你怎么找到它、访问它。例如：
http://www.java.com:80/docs/api（HTTP协议，端口80，路径/docs/api）
ftp://ftp.example.com/file.txt（FTP协议，访问服务器上的file.txt文件）
file:///D:/test.txt（本地文件协议，访问本地D盘的test.txt文件）
1.2 URI与URL的核心区别（重点）
很多开发者会混淆URI和URL，核心区别可总结为：所有URL都是URI，但并非所有URI都是URL。具体对比如下：
对比维度
URI（统一资源标识符）
URL（统一资源定位符）
核心作用
唯一标识资源（不关心如何访问）
唯一定位并访问资源（明确访问方式）
范围
范围广，包含URL和URN
范围窄，是URI的子集
是否包含协议
可选（可无协议，仅标识）
必须包含协议（否则无法定位访问）
是否可访问
不一定（如URN仅标识，无法访问）
一定可以通过协议访问
示例
urn:isbn:9787111641247、mailto:test@163.com
https://www.baidu.com、ftp://ftp.example.com
补充：URN（统一资源名称）是URI的另一个子集，仅用于标识资源，不包含访问方式，目前实际应用较少（如ISBN、UUID），日常开发中最常用的是URL。
二、URI详解（Java核心API：java.net.URI）
2.1 URI的结构（可选组成部分）
URI的结构遵循通用格式，各部分以特定分隔符分隔，部分组件可选，完整格式如下（中括号表示可选）：
[scheme:]scheme-specific-part[#fragment]
进一步拆分（针对带权限的URI，如URL）：
[scheme:][//authority][path][?query][#fragment]
各组件说明：
scheme（协议）：可选，如http、https、ftp、mailto、urn，后面跟冒号（:）。
authority（权限信息）：可选，前面跟//，包含主机名（host）、端口号（port，可选），格式为host:port（如www.baidu.com:80）。
path（路径）：可选，标识资源在服务器上的路径（如/docs/api）。
query（查询参数）：可选，前面跟?，用于传递额外参数（如?name=test&age=18），参数之间用&分隔。
fragment（片段）：可选，前面跟#，用于定位资源内部的某个片段（如网页中的锚点#section1），浏览器会自动定位到该片段。
示例：https://www.example.com:8080/docs/api?name=java#chapter1
scheme：https
authority：www.example.com:8080（host=www.example.com，port=8080）
path：/docs/api
query：name=java
fragment：chapter1
2.2 Java中URI类的核心用法
Java中java.net.URI类用于封装URI，提供了URI的解析、构建、验证等功能，无构造方法，通过静态方法create()或构造方法创建实例（推荐使用create()，更简洁）。
2.2.1 实例创建
import java.net.URI;
import java.net.URISyntaxException;
public class URIDemo {
    public static void main(String[] args) throws URISyntaxException {
        // 方式1：使用create()静态方法（推荐，自动处理简单语法校验）
        URI uri1 = URI.create("https://www.example.com:8080/docs?name=java#chapter1");
        // 方式2：使用构造方法（需处理URISyntaxException异常）
        URI uri2 = new URI("mailto:test@163.com");
        URI uri3 = new URI("urn", "isbn:9787111641247", null); // 无fragment的URN
        System.out.println("uri1: " + uri1);
        System.out.println("uri2: " + uri2);
        System.out.println("uri3: " + uri3);
    }
}
注意：URI的语法严格，若传入的字符串不符合URI格式（如包含非法字符），会抛出URISyntaxException异常，需手动捕获处理。
2.2.2 核心方法（获取URI各组件）
URI类提供了一系列方法，用于获取URI的各个组成部分，常用方法如下：
String getScheme()：获取协议（scheme），若不存在则返回null。
String getAuthority()：获取权限信息（authority），若不存在则返回null。
String getHost()：获取主机名（host），若不存在则返回null（仅当authority存在时有效）。
int getPort()：获取端口号（port），若不存在则返回-1（默认端口会返回-1，如HTTP默认80，HTTPS默认443）。
String getPath()：获取路径（path），若不存在则返回空字符串。
String getQuery()：获取查询参数（query），若不存在则返回null。
String getFragment()：获取片段（fragment），若不存在则返回null。
boolean isAbsolute()：判断URI是否为绝对URI（是否包含scheme）。
URI resolve(URI uri)：解析相对URI，返回一个绝对URI（类似浏览器解析相对路径）。
String toString()：返回URI的字符串表示形式。
2.2.3 常用方法示例
public class URI常用方法Demo {
    public static void main(String[] args) throws URISyntaxException {
        URI uri = URI.create("https://www.example.com:8080/docs/api?name=java#chapter1");
        System.out.println("协议（scheme）：" + uri.getScheme()); // https
        System.out.println("权限信息（authority）：" + uri.getAuthority()); // www.example.com:8080
        System.out.println("主机名（host）：" + uri.getHost()); // www.example.com
        System.out.println("端口号（port）：" + uri.getPort()); // 8080
        System.out.println("路径（path）：" + uri.getPath()); // /docs/api
        System.out.println("查询参数（query）：" + uri.getQuery()); // name=java
        System.out.println("片段（fragment）：" + uri.getFragment()); // chapter1
        System.out.println("是否为绝对URI：" + uri.isAbsolute()); // true
        // 解析相对URI
        URI relativeUri = URI.create("test?age=18");
        URI absoluteUri = uri.resolve(relativeUri);
        System.out.println("解析相对URI后的绝对URI：" + absoluteUri); 
        // 输出：https://www.example.com:8080/docs/test?age=18
    }
}
2.3 URI的编码与解码（重点）
URI中不能包含空格、中文、特殊符号（如@、#、&除外），否则会导致URI语法错误。因此，当URI中包含这些字符时，需要进行编码（将特殊字符转换为%+十六进制编码）；反之，若获取到编码后的URI，需要进行解码（还原为原始字符）。
Java中通过java.net.URLEncoder（编码）和java.net.URLDecoder（解码）工具类实现，注意编码格式需统一为UTF-8。
2.3.1 编码与解码示例
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.URI;
import java.io.UnsupportedEncodingException;
public class URI编码解码Demo {
    public static void main(String[] args) throws UnsupportedEncodingException, URISyntaxException {
        // 原始字符串（包含中文和空格）
        String original = "https://www.example.com/测试 页面?name=张三";
        // 1. 编码（仅对路径、查询参数等部分编码，协议、主机名不编码）
        // 编码路径部分
        String encodedPath = URLEncoder.encode("测试 页面", "UTF-8");
        // 编码查询参数
        String encodedQuery = URLEncoder.encode("张三", "UTF-8");
        // 拼接编码后的URI
        String encodedUriStr = "https://www.example.com/" + encodedPath + "?name=" + encodedQuery;
        System.out.println("编码后的URI：" + encodedUriStr);
        // 输出：https://www.example.com/%E6%B5%8B%E8%AF%95+%E9%A1%B5%E9%9D%A2?name=%E5%BC%A0%E4%B8%89
        // 2. 解码
        String decodedPath = URLDecoder.decode(encodedPath, "UTF-8");
        String decodedQuery = URLDecoder.decode(encodedQuery, "UTF-8");
        System.out.println("解码后的路径：" + decodedPath); // 测试 页面
        System.out.println("解码后的查询参数：" + decodedQuery); // 张三
        // 3. 编码后的URI创建实例
        URI encodedUri = URI.create(encodedUriStr);
        System.out.println("编码URI实例：" + encodedUri);
    }
}
注意事项：
编码时，仅对URI中的“非标准字符”（中文、空格、特殊符号）编码，协议（http/https）、主机名、分隔符（:、/、?、#）不编码。
编码和解码的字符集必须一致（推荐UTF-8），否则会出现乱码。
URLEncoder和URLDecoder是通用工具类，既适用于URI，也适用于URL。
三、URL详解（Java核心API：java.net.URL）
3.1 URL的结构（固定组成部分）
URL作为URI的子集，结构比URI更严格，必须包含协议（scheme）和资源定位信息，完整格式如下：
scheme://host:port/path?query#fragment
各组件与URI一致，但有两个关键区别：
URL的scheme（协议）是必须的（如http、https、ftp），无协议则不是URL。
URL的authority（host:port）和path通常是必须的，用于准确定位资源，否则无法访问。
常见URL协议及说明：
http：超文本传输协议，默认端口80，用于访问网页。
https：加密的HTTP协议，默认端口443，用于安全访问（如支付、登录）。
ftp：文件传输协议，用于文件上传下载，默认端口21。
file：本地文件协议，用于访问本地计算机上的文件，格式为file:///本地路径。
mailto：邮件协议，用于打开邮件客户端发送邮件。
3.2 Java中URL类的核心用法
Java中java.net.URL类用于封装URL，提供了URL的解析、资源访问（如打开连接、读取资源）等功能，与URI类的核心区别是：URL可以直接访问资源，而URI仅用于标识资源。
3.2.1 实例创建
URL类有多个构造方法，创建时需处理MalformedURLException异常（URL格式错误时抛出），常用构造方法如下：
import java.net.URL;
import java.net.MalformedURLException;
public class URLDemo {
    public static void main(String[] args) throws MalformedURLException {
        // 方式1：直接传入完整URL字符串（最常用）
        URL url1 = new URL("https://www.baidu.com:80/index.html?name=test#section1");
        // 方式2：拆分组件传入（scheme、host、port、file）
        // file包含path、query、fragment，port为-1表示使用默认端口
        URL url2 = new URL("http", "www.example.com", 8080, "/docs/api?name=java#chapter1");
        // 方式3：通过基础URL和相对URL创建
        URL baseUrl = new URL("https://www.example.com/docs/");
        URL relativeUrl = new URL(baseUrl, "test.html"); // 相对路径，自动拼接
        System.out.println("url1: " + url1);
        System.out.println("url2: " + url2);
        System.out.println("relativeUrl: " + relativeUrl); // https://www.example.com/docs/test.html
    }
}
3.2.2 核心方法（获取URL各组件+访问资源）
URL类的方法分为两类：一类是获取URL各组件（与URI类似），另一类是访问资源（核心优势）。
1. 获取URL组件的方法
    String getProtocol()：获取协议（对应URI的getScheme()）。
    String getHost()：获取主机名。
    int getPort()：获取端口号，默认端口返回-1。
    String getPath()：获取路径。
    String getQuery()：获取查询参数。
    String getRef()：获取片段（对应URI的getFragment()）。
    String getFile()：获取路径+查询参数（path?query）。
    URI toURI()：将URL转换为URI对象。
2. 访问资源的核心方法
    URLConnection openConnection()：打开与URL对应的连接，返回URLConnection对象，用于读取资源、发送请求（核心方法）。
    InputStream openStream()：直接打开URL的输入流，读取资源内容（简化版，本质是openConnection().getInputStream()）。
    3.2.3 常用方法示例（获取组件+访问资源）
    import java.net.URL;
    import java.net.MalformedURLException;
    import java.io.InputStream;
    import java.io.BufferedReader;
    import java.io.InputStreamReader;
    import java.io.IOException;
    public class URL常用方法Demo {
    public static void main(String[] args) throws MalformedURLException, IOException {
        // 1. 创建URL实例
        URL url = new URL("https://www.baidu.com");
        // 2. 获取URL各组件
        System.out.println("协议：" + url.getProtocol()); // https
        System.out.println("主机名：" + url.getHost()); // www.baidu.com
        System.out.println("端口号：" + url.getPort()); // -1（https默认443）
        System.out.println("路径：" + url.getPath()); // （百度首页路径为空）
        System.out.println("查询参数：" + url.getQuery()); // null
        System.out.println("片段：" + url.getRef()); // null
        // 3. 访问资源（读取百度首页的HTML内容）
        // 方式1：使用openStream()简化读取
        try (InputStream is = url.openStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            System.out.println("百度首页HTML（前500字符）：" + sb.substring(0, 500));
        }
        // 方式2：使用openConnection()，可设置请求头、超时时间等（更灵活）
        /*
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(3000); // 连接超时3秒
        conn.setReadTimeout(3000); // 读取超时3秒
        try (InputStream is = conn.getInputStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            // 读取内容...
        }
        */
    }
    }
    3.3 URL与URLConnection的关系（重点）
    URLConnection是URL的核心辅助类，用于建立与URL资源的连接，实现数据的读取和发送（如HTTP请求）。它是一个抽象类，具体实现由不同协议提供（如HttpURLConnection对应HTTP/HTTPS协议，FtpURLConnection对应FTP协议）。
    核心用法：通过URL.openConnection()获取URLConnection实例，设置请求参数（超时时间、请求头），然后通过输入流读取响应，输出流发送请求。
    3.3.1 HttpURLConnection示例（发送HTTP GET请求）
    import java.net.URL;
    import java.net.HttpURLConnection;
    import java.io.InputStream;
    import java.io.BufferedReader;
    import java.io.InputStreamReader;
    import java.io.IOException;
    public class HttpURLConnectionDemo {
    public static void main(String[] args) throws IOException {
        // 1. 创建URL实例
        URL url = new URL("https://www.example.com/api/user?name=test");
        // 2. 打开连接，强转为HttpURLConnection（针对HTTP/HTTPS协议）
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        // 3. 设置请求参数
        conn.setRequestMethod("GET"); // 请求方法（GET/POST）
        conn.setConnectTimeout(3000); // 连接超时时间（毫秒）
        conn.setReadTimeout(3000); // 读取超时时间（毫秒）
        conn.setRequestProperty("User-Agent", "Mozilla/5.0"); // 设置请求头
        // 4. 判断响应码（200表示请求成功）
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            // 5. 读取响应数据
            try (InputStream is = conn.getInputStream();
                 BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
                String line;
                StringBuilder sb = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                System.out.println("响应内容：" + sb.toString());
            }
        } else {
            System.out.println("请求失败，响应码：" + conn.getResponseCode());
        }
        // 6. 关闭连接
        conn.disconnect();
    }
    }
    四、URI与URL的实战对比（开发场景应用）
    4.1 什么时候用URI？
    当仅需要“标识资源”，不需要访问资源时，优先使用URI，常见场景：
    资源的唯一标识（如数据库中存储资源的标识、接口返回的资源ID）。
    处理相对路径的解析（如通过URI.resolve()解析相对资源路径）。
    需要严格校验资源标识的语法（URI的语法校验更严格）。
    涉及非URL类型的标识（如URN、邮箱地址mailto:test@163.com）。
    4.2 什么时候用URL？
    当需要“访问资源”（读取、发送数据）时，必须使用URL，常见场景：
    发送HTTP/HTTPS请求（如调用第三方接口、爬取网页内容）。
    访问FTP服务器、本地文件（通过file协议）。
    需要获取资源的访问信息（如协议、主机名、端口）并建立连接。
    4.3 两者转换（Java代码实现）
    URI和URL可以相互转换，核心方法：
    URL → URI：url.toURI()（需处理URISyntaxException异常）。
    URI → URL：uri.toURL()（需处理MalformedURLException异常，仅当URI包含协议、可访问时才能转换）。
    import java.net.URI;
    import java.net.URL;
    import java.net.URISyntaxException;
    import java.net.MalformedURLException;
    public class URI与URL转换Demo {
    public static void main(String[] args) throws URISyntaxException, MalformedURLException {
        // 1. URL → URI
        URL url = new URL("https://www.baidu.com");
        URI uriFromUrl = url.toURI();
        System.out.println("URL转换为URI：" + uriFromUrl);
        // 2. URI → URL（仅当URI是URL时可转换）
        URI uri = URI.create("https://www.example.com");
        URL urlFromUri = uri.toURL();
        System.out.println("URI转换为URL：" + urlFromUri);
        // 3. 非URL类型的URI无法转换为URL（会抛出异常）
        URI nonUrlUri = URI.create("mailto:test@163.com");
        // URL errorUrl = nonUrlUri.toURL(); // 抛出MalformedURLException
    }
    }
    五、常见问题与解决方案
    5.1 URI/URL格式错误异常
    现象：创建URI时抛出URISyntaxException，创建URL时抛出MalformedURLException。
    原因：传入的字符串包含非法字符（如中文、空格未编码）、协议格式错误（如http:www.baidu.com，缺少//）。
    解决方案：
    对包含中文、空格的部分进行编码（使用URLEncoder.encode()）。
    检查协议格式，确保URL包含//（如http://www.baidu.com，而非http:www.baidu.com）。
    使用URI.create()创建URI，自动处理简单的语法问题（比直接用构造方法更宽松）。
    5.2 URL访问资源超时/失败
    现象：调用openStream()或openConnection()时抛出IOException，或长时间阻塞。
    原因：网络不通、目标服务器未启动、端口被防火墙拦截、超时时间未设置。
    解决方案：
    检查网络连接，确保能访问目标URL（浏览器中测试）。
    设置URLConnection的超时时间（setConnectTimeout()、setReadTimeout()），避免长时间阻塞。
    检查防火墙是否拦截了目标端口（如HTTP的80端口、HTTPS的443端口）。
    判断响应码，针对不同响应码（如404、500）做异常处理。
    5.3 编码/解码乱码问题
    现象：解码后的中文出现乱码（如???、Ã©Â¥Â¼Ã©Â¥Â¿）。
    原因：编码和解码的字符集不一致（如编码用UTF-8，解码用GBK）。
    解决方案：
    编码和解码统一使用UTF-8字符集（推荐）。
    若第三方接口返回的编码是GBK，解码时需指定GBK字符集。
    避免多次编码（如编码后再编码，会导致双重转义，解码后无法还原）。
    六、总结
    URI和URL是Java网络编程中标识和访问资源的核心概念，核心要点总结如下：
    范围关系：URL是URI的子集，所有URL都是URI，但URI不一定是URL（还包含URN）。
    核心区别：URI负责“标识”资源，无需协议；URL负责“定位+访问”资源，必须包含协议。
    API用法：
    URI类：用于解析、验证、转换URI，核心是标识资源，不支持访问。
    URL类：用于解析URL、建立连接、访问资源，核心是与资源交互。
    实战场景：仅标识资源用URI，需访问资源用URL；编码解码需统一字符集，避免乱码。
    掌握URI和URL的区别与用法，是Java网络编程的基础，也是后续学习HTTP客户端（如HttpClient）、网络框架（如Netty）的前提。
