SpringMVC文件上传与下载
一、文件上传
1.1 上传必要前提
实现SpringMVC文件上传，需满足3个核心前提，缺一不可：
表单标签<form>的enctype属性必须设置为multipart/form-data（默认值为application/x-www-form-urlencoded，无法识别文件流）；
表单提交方式必须为POST（GET请求有参数长度限制，无法承载文件流数据）；
表单中需提供文件选择域<input type="file" name="xxx" />，name属性需与后端接收参数名一致。
1.2 依赖导入
SpringMVC文件上传需依赖Apache Commons FileUpload组件，在pom.xml中导入以下依赖（若使用Servlet3.0+可无需导入，下文会说明）：
<dependency>
    <groupId>commons-fileupload</groupId>
    <artifactId>commons-fileupload</artifactId>
    <version>1.3.3</version>
</dependency>
<dependency>
    <groupId>commons-io</groupId>
    <artifactId>commons-io</artifactId>
    <version>2.5</version>
</dependency>
1.3 两种上传方式实现
方式1：基于CommonsMultipartResolver（兼容低版本Servlet）
需先在SpringMVC配置文件中配置文件解析器，id必须为multipartResolver，否则无法生效：
<!-- 文件上传解析器 -->
<bean id="multipartResolver" class="org.springframework.web.multipart.commons.CommonsMultipartResolver">
    <!-- 单个文件最大上传大小（10MB，单位：字节） -->
    <property name="maxUploadSize" value="10485760"/>
    <!-- 整个请求最大上传大小（20MB） -->
    <property name="maxUploadSizePerFile" value="20971520"/>
    <!-- 编码格式，与表单一致 -->
    <property name="defaultEncoding" value="UTF-8"/>
    <!-- 临时文件缓存大小 -->
    <property name="maxInMemorySize" value="40960" />
</bean>
前端JSP表单（上传页面）：
<h3>SpringMVC文件上传</h3>
<form action="${pageContext.request.contextPath}/file/upload" method="post" enctype="multipart/form-data">
    选择文件：<input type="file" name="uploadFile"/><br/>
    <input type="submit" value="上传文件"/>
</form>
后端Controller实现（核心逻辑：接收文件、处理文件名、保存文件）：
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
@Controller
@RequestMapping("/file")
public class FileUploadController {
    // 处理文件上传请求
    @RequestMapping("/upload")
    public String upload(MultipartFile uploadFile, HttpServletRequest request) throws IOException {
        // 1. 校验文件是否为空
        if (uploadFile.isEmpty()) {
            // 可跳转至错误页面，提示文件为空
            return "error";
        }
        // 2. 获取服务器端上传目录的真实路径（避免硬编码，适配不同环境）
        String uploadPath = request.getSession().getServletContext().getRealPath("/uploads");
        File uploadDir = new File(uploadPath);
        // 3. 若目录不存在，创建目录（多级目录需用mkdirs()）
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        // 4. 处理文件名：避免重名，用UUID生成唯一标识
        String originalFilename = uploadFile.getOriginalFilename();
        // 获取文件后缀（如.jpg、.pdf）
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        // 生成新文件名：UUID + 后缀
        String newFilename = UUID.randomUUID().toString().replace("-", "").toUpperCase() + suffix;
        // 5. 保存文件到指定目录
        File destFile = new File(uploadDir, newFilename);
        uploadFile.transferTo(destFile); // 底层自动处理文件流，无需手动关闭
        // 6. 跳转至成功页面（可携带文件名，用于前端展示）
        request.setAttribute("filename", newFilename);
        return "success";
    }
}
方式2：基于StandardServletMultipartResolver（Servlet3.0+）
Servlet3.0及以上版本支持标准文件上传方式，无需导入commons-fileupload依赖，只需配置Servlet和解析器即可。
第一步：配置web.xml中的DispatcherServlet，添加multipart-config标签：
<servlet>
    <servlet-name>springmvc</servlet-name>
    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
    <init-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>classpath:springmvc.xml</param-value>
    </init-param>
    <!-- 文件上传相关配置 -->
    <multipart-config>
        &lt;location&gt;/tmp&lt;/location&gt; <!-- 临时文件存储目录 -->
        <max-file-size>10485760</max-file-size> <!-- 单个文件最大大小 -->
        <max-request-size>20971520&lt;/max-request-size&gt; <!-- 整个请求最大大小 -->
    </multipart-config>
</servlet>
<servlet-mapping>
    <servlet-name>springmvc</servlet-name>
    <url-pattern>/</url-pattern>
</servlet-mapping>
第二步：在SpringMVC配置文件中配置解析器：
<bean id="multipartResolver" class="org.springframework.web.multipart.support.StandardServletMultipartResolver"></bean>
第三步：Controller代码与方式1一致，无需修改（MultipartFile接收文件的逻辑通用）。
二、文件下载
SpringMVC文件下载核心逻辑：读取服务器端文件，通过HttpServletResponse将文件流写入响应，设置响应头告知浏览器以“下载”方式处理文件，常用两种实现方式。
2.1 方式1：基于HttpServletResponse（底层方式，灵活可控）
Controller实现（支持权限控制、文件校验，适配各类场景）：
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
@Controller
@RequestMapping("/file")
public class FileDownloadController {
    // 处理文件下载请求，参数为要下载的文件名
    @RequestMapping("/download")
    public void download(String filename, HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 1. 校验文件名（避免非法请求）
        if (filename == null || "".equals(filename.trim())) {
            response.getWriter().write("请指定要下载的文件");
            return;
        }
        // 2. 获取服务器端文件的真实路径（与上传目录一致）
        String uploadPath = request.getSession().getServletContext().getRealPath("/uploads");
        File file = new File(uploadPath, filename);
        // 3. 校验文件是否存在、是否为文件（避免目录被下载）
        if (!file.exists() || !file.isFile()) {
            response.getWriter().write("文件不存在");
            return;
        }
        // 4. 设置响应头，告知浏览器以附件形式下载（解决中文文件名乱码）
        response.setContentType("application/octet-stream"); // 二进制流，通用下载类型
        // 对文件名编码，适配不同浏览器（UTF-8编码，替换空格避免异常）
        String safeFilename = URLEncoder.encode(filename, "UTF-8").replace("+", "%20");
        response.addHeader("Content-Disposition", "attachment;filename=" + safeFilename);
        // 5. 读取文件流，写入响应输出流
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        OutputStream os = response.getOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        // 循环读取文件内容，写入响应
        while ((len = bis.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        // 6. 关闭流（先关输出流，再关输入流）
        os.close();
        bis.close();
        fis.close();
    }
}
2.2 方式2：基于ResponseEntity（SpringMVC封装方式，简洁高效）
ResponseEntity可直接封装响应头、响应体和状态码，无需手动关闭流，代码更简洁：
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
@Controller
@RequestMapping("/file")
public class FileDownloadController2 {
    @RequestMapping("/download2")
    public ResponseEntity<FileSystemResource> download2(String filename, HttpServletRequest request) throws UnsupportedEncodingException {
        // 1. 获取文件真实路径，校验文件合法性（与方式1一致）
        String uploadPath = request.getSession().getServletContext().getRealPath("/uploads");
        File file = new File(uploadPath, filename);
        if (!file.exists() || !file.isFile()) {
            // 返回404状态码，提示文件不存在
            return ResponseEntity.notFound().build();
        }
        // 2. 封装文件资源
        FileSystemResource fileResource = new FileSystemResource(file);
        // 3. 设置响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        String safeFilename = URLEncoder.encode(filename, "UTF-8").replace("+", "%20");
        headers.add("Content-Disposition", "attachment;filename=" + safeFilename);
        // 4. 返回ResponseEntity，SpringMVC自动处理流关闭
        return new ResponseEntity<>(fileResource, headers, HttpStatus.OK);
    }
}
前端下载触发（可通过a标签直接跳转，或通过JS发起请求）：
<!-- a标签直接触发下载，filename为服务器端保存的文件名 -->
<a href="${pageContext.request.contextPath}/file/download?filename=${filename}">下载文件</a>
三、常见问题与避坑指南
3.1 上传相关问题
文件上传失败，报ClassCastException：原因是文件解析器id不为multipartResolver，需确保配置文件中bean的id严格为multipartResolver。
中文文件名乱码：前端表单编码设为UTF-8，后端配置解析器的defaultEncoding为UTF-8；保存文件时对文件名进行URLEncoder编码。
文件大小超出限制，报SizeLimitExceededException：检查解析器配置的maxUploadSize（单个文件）和maxUploadSizePerFile（总请求），单位为字节，注意区分Spring Boot和SpringMVC的配置差异（Spring Boot需配置spring.servlet.multipart相关参数）。
服务器保存文件报Permission denied：给服务器上传目录授权（Linux系统执行chmod -R 755 目录路径），确保运行服务器的用户有写入权限。
3.2 下载相关问题
下载文件为空或损坏：检查文件路径是否正确，流关闭顺序是否正确（先关输出流，再关输入流）；避免文件未读取完成就关闭流。
浏览器直接打开文件而非下载：未正确设置响应头Content-Disposition为attachment，或Content-Type设置错误，需改为application/octet-stream。
大文件下载超时：配置文件上传超时时间（SpringMVC通过解析器配置，Spring Boot配置spring.servlet.multipart.request-timeout），同时调整Nginx的client_max_body_size和超时参数。
四、扩展说明
1. 多文件上传：前端表单input标签添加multiple属性（<input type="file" name="uploadFile" multiple/>），后端Controller用MultipartFile[]数组接收，循环处理每个文件即可。
2. 上传文件存储：实际开发中不建议将文件存储在服务器本地（重启服务器会丢失），推荐存储在OSS（阿里云、腾讯云）或独立文件服务器，Controller只需将文件上传至OSS，保存文件URL即可。
3. 安全校验：上传时需校验文件类型（后缀、MIME类型），避免上传可执行文件（如.exe、.jsp）；下载时需校验用户权限，防止未授权用户下载敏感文件。
