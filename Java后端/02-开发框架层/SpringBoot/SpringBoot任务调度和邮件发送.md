SpringBoot任务调度和邮件发送
一、SpringBoot任务调度
1.1 任务调度核心概念
任务调度是指基于指定的时间规则（如固定时间、固定间隔、 cron 表达式），自动执行预设的业务逻辑，常用于定时备份数据、定时发送通知、定时清理缓存等场景。SpringBoot 内置了对任务调度的支持，无需额外引入大量依赖，通过简单配置即可实现高效的定时任务。
SpringBoot 任务调度的核心依赖是 spring-context，该依赖在 SpringBoot 启动器（spring-boot-starter）中已默认引入，因此无需手动添加额外依赖（除非需要扩展高级功能）。
1.2 三种核心实现方式
方式1：@Scheduled 注解（最常用）
这是 SpringBoot 中最简洁、最常用的任务调度方式，通过在方法上添加 @Scheduled 注解，配合注解参数指定调度规则，即可实现定时任务。
步骤1：开启任务调度
在 SpringBoot 主启动类上添加 @EnableScheduling 注解，开启任务调度功能：
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableScheduling // 开启任务调度
public class TaskEmailApplication {
    public static void main(String[] args) {
        SpringApplication.run(TaskEmailApplication.class, args);
    }
}
步骤2：编写定时任务方法
在组件类（@Component、@Service 等）中编写方法，添加 @Scheduled 注解，指定调度规则。常用注解参数如下：
fixedRate：固定速率，单位毫秒，从方法开始执行时计时，无论方法是否执行完毕，到时间就再次执行（例：fixedRate = 5000 表示每5秒执行一次）。
fixedDelay：固定延迟，单位毫秒，从方法执行完毕后开始计时，到时间再执行下一次（例：fixedDelay = 5000 表示方法执行完后，间隔5秒再执行）。
cron： cron 表达式，用于复杂的时间规则（如每周一凌晨3点执行、每月15号中午12点执行），灵活性最高。
实例代码：
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component // 必须交给Spring管理
public class ScheduledTask {
    // 固定延迟：方法执行完后，间隔3秒再执行
    @Scheduled(fixedDelay = 3000)
    public void fixedDelayTask() {
        System.out.println("固定延迟任务执行：" + System.currentTimeMillis());
    }
    // 固定速率：每5秒执行一次（无论上一次是否执行完毕）
    @Scheduled(fixedRate = 5000)
    public void fixedRateTask() {
        System.out.println("固定速率任务执行：" + System.currentTimeMillis());
    }
    // cron表达式：每天凌晨2点执行（cron表达式语法：秒 分 时 日 月 周 年，年可省略）
    @Scheduled(cron = "0 0 2 * * ?")
    public void cronTask() {
        System.out.println("Cron任务执行：每天凌晨2点执行");
    }
}
方式2：TaskScheduler 接口（手动控制任务）
@Scheduled 注解适合固定规则的任务，若需要手动控制任务的启动、暂停、取消（如根据业务逻辑动态触发任务），可使用 TaskScheduler 接口，SpringBoot 自动配置了默认实现（ThreadPoolTaskScheduler）。
实例代码（动态创建定时任务）：
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.concurrent.ScheduledFuture;
@Service
public class DynamicTaskService {
    @Resource
    private TaskScheduler taskScheduler;
    // 用于存储任务实例，方便后续取消
    private ScheduledFuture<?> taskFuture;
    // 启动定时任务（cron表达式动态传入）
    public void startCronTask(String cron) {
        // 取消已存在的任务（避免重复启动）
        if (taskFuture != null && !taskFuture.isCancelled()) {
            taskFuture.cancel(true);
        }
        // 创建任务并启动
        taskFuture = taskScheduler.schedule(() -> {
            System.out.println("动态Cron任务执行：" + System.currentTimeMillis());
        }, new CronTrigger(cron));
    }
    // 取消定时任务
    public void cancelTask() {
        if (taskFuture != null && !taskFuture.isCancelled()) {
            taskFuture.cancel(true);
            System.out.println("任务已取消");
        }
    }
}
方式3：Quartz 整合（复杂场景）
当任务调度需求复杂（如任务持久化、分布式任务、多任务依赖）时，可整合 Quartz 框架。SpringBoot 提供了 spring-boot-starter-quartz 依赖，简化 Quartz 配置。
步骤1：引入依赖
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-quartz</artifactId>
</dependency>
步骤2：编写 Quartz 任务类
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
// 实现Job接口，重写execute方法（任务核心逻辑）
public class QuartzTask implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        System.out.println("Quartz任务执行：" + System.currentTimeMillis());
    }
}
步骤3：配置 Quartz 任务
通过配置类创建 JobDetail（任务详情）和 Trigger（触发器），交给 Spring 管理：
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class QuartzConfig {
    // 1. 创建任务详情（指定任务类）
    @Bean
    public JobDetail quartzJobDetail() {
        return JobBuilder.newJob(QuartzTask.class)
                .withIdentity("quartzTask", "quartzGroup") // 任务标识（名称+组）
                .storeDurably() // 任务持久化（即使没有触发器，也保留任务）
                .build();
    }
    // 2. 创建触发器（指定调度规则）
    @Bean
    public Trigger quartzTrigger() {
        // cron表达式：每10秒执行一次
        CronScheduleBuilder cron = CronScheduleBuilder.cronSchedule("0/10 * * * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(quartzJobDetail()) // 关联任务详情
                .withIdentity("quartzTrigger", "quartzGroup") // 触发器标识
                .withSchedule(cron) // 绑定调度规则
                .build();
    }
}
1.3 注意事项
@Scheduled 注解的方法必须是无参、无返回值的方法，且方法所在类必须交给 Spring 管理（添加 @Component 等注解）。
默认情况下，任务调度是单线程执行的，若多个任务同时触发，会排队执行；若需要多线程执行，可配置 TaskScheduler 的线程池大小。
cron 表达式中，“?” 用于日和周的冲突匹配（如指定日为15号，周就用?，避免冲突），不可省略。
二、SpringBoot 邮件发送
2.1 邮件发送核心依赖与原理
SpringBoot 整合了 JavaMail 技术，通过 spring-boot-starter-mail 依赖简化邮件发送配置，底层基于 SMTP 协议（简单邮件传输协议），需要借助第三方邮件服务器（如 QQ 邮箱、163 邮箱、企业邮箱）完成发送。
核心依赖：
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
2.2 前置准备（以 QQ 邮箱为例）
发送邮件需要开启第三方邮箱的 SMTP 服务，并获取授权码（替代邮箱密码，提高安全性），步骤如下：
登录 QQ 邮箱，进入「设置」→「账户」。
找到「POP3/IMAP/SMTP/Exchange/CardDAV/CalDAV服务」，开启「SMTP服务」。
开启后，生成授权码（需验证手机短信），保存授权码（后续配置用）。
注：163 邮箱、企业邮箱操作类似，均需开启 SMTP 服务并获取授权码。
2.3 核心配置（application.yml）
在配置文件中配置邮件服务器信息，以 QQ 邮箱为例：
spring:
  mail:
    host: smtp.qq.com # 邮件服务器地址（QQ邮箱：smtp.qq.com；163邮箱：smtp.163.com）
    port: 587 # 端口（SMTP默认端口25，QQ邮箱需用587，开启SSL可⽤465）
    username: 123456@qq.com # 发送者邮箱地址
    password: abcdefghijklmnop # 授权码（不是邮箱密码）
    default-encoding: UTF-8 # 编码格式
    properties:
      mail:
        smtp:
          auth: true # 开启认证
          starttls:
            enable: true # 开启TLS加密（适配587端口）
            required: true
2.4 四种邮件类型实现
SpringBoot 提供 JavaMailSender 接口，封装了邮件发送的核心方法，可实现简单文本邮件、HTML 邮件、带附件邮件、带图片邮件四种常见类型。
步骤1：注入 JavaMailSender
在 Service 类中注入 JavaMailSender，用于发送邮件：
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
@Service
public class EmailService {
    @Resource
    private JavaMailSender javaMailSender;
    // 发送者邮箱（与配置文件中username一致，可提取为常量）
    private static final String SENDER = "123456@qq.com";
}
类型1：简单文本邮件（最基础）
仅包含文本内容，无附件、无图片，适合简单通知。
import org.springframework.mail.SimpleMailMessage;
// 简单文本邮件
public void sendSimpleEmail(String to, String subject, String content) {
    // 1. 创建邮件消息对象
    SimpleMailMessage message = new SimpleMailMessage();
    // 2. 设置邮件参数
    message.setFrom(SENDER); // 发送者
    message.setTo(to); // 接收者（可多个，用数组：new String[]{"a@qq.com", "b@qq.com"}）
    message.setSubject(subject); // 邮件主题
    message.setText(content); // 邮件文本内容
    // 3. 发送邮件
    javaMailSender.send(message);
}
类型2：HTML 邮件（带样式）
支持 HTML 标签（如换行、加粗、表格等），适合格式化的通知（如订单通知、活动通知）。
import org.springframework.mail.javamail.MimeMessageHelper;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
// HTML邮件
public void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
    // 1. 创建MimeMessage（支持复杂邮件）
    MimeMessage mimeMessage = javaMailSender.createMimeMessage();
    // 2. 创建助手类（true表示支持多部件邮件，如附件、图片）
    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
    // 3. 设置邮件参数
    helper.setFrom(SENDER);
    helper.setTo(to);
    helper.setSubject(subject);
    // 4. 设置HTML内容（第二个参数true表示解析HTML）
    helper.setText(htmlContent, true);
    // 5. 发送邮件
    javaMailSender.send(mimeMessage);
}
// 测试调用（HTML内容示例）
public void testHtmlEmail() throws MessagingException {
    String html = "<h3>这是HTML邮件</h3>" +
                  "<p>姓名：张三</p>" +
                  "<p>时间：" + System.currentTimeMillis() + "</p>" +
                  "<a href='https://www.baidu.com'>点击跳转</a>";
    sendHtmlEmail("654321@qq.com", "HTML邮件测试", html);
}
类型3：带附件邮件
可携带本地文件（如文档、图片、压缩包），通过 MimeMessageHelper 添加附件。
import org.springframework.core.io.FileSystemResource;
import java.io.File;
// 带附件邮件
public void sendAttachmentEmail(String to, String subject, String content, String filePath) throws MessagingException {
    MimeMessage mimeMessage = javaMailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
    helper.setFrom(SENDER);
    helper.setTo(to);
    helper.setSubject(subject);
    helper.setText(content, false); // 这里false表示文本内容，不解析HTML
    // 添加附件（FileSystemResource读取本地文件）
    File file = new File(filePath);
    FileSystemResource resource = new FileSystemResource(file);
    // 第二个参数是附件的显示名称
    helper.addAttachment(file.getName(), resource);
    javaMailSender.send(mimeMessage);
}
// 测试调用（附件路径为本地文件路径）
public void testAttachmentEmail() throws MessagingException {
    String filePath = "D:/test.txt"; // 本地文件路径
    sendAttachmentEmail("654321@qq.com", "带附件邮件测试", "邮件包含附件，请查收", filePath);
}
类型4：带图片邮件（内嵌图片）
将图片内嵌到邮件内容中（不是作为附件），通过 cid 标识关联图片资源。
// 带内嵌图片邮件
public void sendImageEmail(String to, String subject, String htmlContent, String imagePath, String cid) throws MessagingException {
    MimeMessage mimeMessage = javaMailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
    helper.setFrom(SENDER);
    helper.setTo(to);
    helper.setSubject(subject);
    // HTML内容中通过cid关联图片（cid需与addInline的第二个参数一致）
    helper.setText(htmlContent, true);
    // 内嵌图片（cid是图片的唯一标识，HTML中用<img src='cid:xxx'>引用）
    FileSystemResource imageResource = new FileSystemResource(new File(imagePath));
    helper.addInline(cid, imageResource);
    javaMailSender.send(mimeMessage);
}
// 测试调用
public void testImageEmail() throws MessagingException {
    String imagePath = "D:/test.jpg"; // 本地图片路径
    String cid = "image01"; // 图片唯一标识
    // HTML中引用图片：<img src='cid:image01' alt='图片'>
    String html = "<h3>带内嵌图片的邮件</h3>" +
                  "<img src='cid:" + cid + "' alt='测试图片' width='300px'>";
    sendImageEmail("654321@qq.com", "带图片邮件测试", html, imagePath, cid);
}
2.5 异常处理与优化
邮件发送可能出现网络异常、服务器异常等问题，需添加异常捕获，避免程序崩溃，并记录日志。
批量发送邮件时，可使用线程池异步发送，提高效率（避免同步发送阻塞主线程）。
授权码需妥善保管，建议放在配置文件中，避免硬编码；生产环境可通过配置中心管理。
异常处理示例：
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
private static final Logger log = LoggerFactory.getLogger(EmailService.class);
public void sendSimpleEmailWithException(String to, String subject, String content) {
    try {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(SENDER);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        javaMailSender.send(message);
        log.info("邮件发送成功，接收者：{}", to);
    } catch (Exception e) {
        log.error("邮件发送失败，接收者：{}，异常信息：{}", to, e.getMessage());
        // 可添加重试逻辑、异常通知等
    }
}
三、任务调度与邮件发送整合示例
结合两者，实现「定时发送邮件」功能（如每天早上8点发送每日通知邮件）。
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import javax.mail.MessagingException;
@Component
public class ScheduledEmailTask {
    @Resource
    private EmailService emailService;
    // cron表达式：每天早上8点执行
    @Scheduled(cron = "0 0 8 * * ?")
    public void sendDailyEmail() {
        String to = "654321@qq.com";
        String subject = "每日通知";
        String content = "早上好！今日日期：" + System.currentTimeMillis() + "，祝您工作顺利！";
        try {
            // 发送简单文本邮件
            emailService.sendSimpleEmail(to, subject, content);
            System.out.println("每日邮件发送成功");
        } catch (Exception e) {
            System.out.println("每日邮件发送失败：" + e.getMessage());
        }
    }
}
四、常见问题排查
任务不执行：检查主启动类是否添加 @EnableScheduling；任务方法所在类是否有 @Component 注解；cron 表达式是否正确。
邮件发送失败：检查配置文件中 host、port、username、password（授权码）是否正确；SMTP 服务是否开启；网络是否能连接邮件服务器。
HTML 邮件显示乱码：确保配置文件中 default-encoding 为 UTF-8；MimeMessageHelper 构造时指定 UTF-8 编码。
附件无法打开：检查文件路径是否正确；文件是否存在；附件名称是否包含特殊字符。
