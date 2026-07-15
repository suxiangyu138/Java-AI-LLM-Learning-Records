# SpringBoot 任务调度和邮件发送

> **定位**：任务调度 = `@Scheduled` + `@EnableScheduling`。邮件发送 = `JavaMailSender` + SMTP 授权码。

---

## 目录

1. [任务调度](#1-任务调度)
2. [邮件发送](#2-邮件发送)
3. [整合实例](#3-整合实例)

---

## 1. 任务调度

### 1.1 @Scheduled（最常用）

**开启**：

```java
@SpringBootApplication
@EnableScheduling
public class Application {}
```

**三种模式**：

| 参数 | 说明 | 示例 |
|------|------|------|
| `fixedRate` | 固定速率，上次开始计时 | `fixedRate = 5000`（每 5 秒） |
| `fixedDelay` | 固定延迟，上次结束计时 | `fixedDelay = 3000`（结束后 3 秒） |
| `cron` | Cron 表达式 | `cron = "0 0 2 * * ?"`（每天凌晨 2 点） |

```java
@Component
public class ScheduledTask {
    @Scheduled(fixedDelay = 3000)
    public void fixedDelayTask() { }

    @Scheduled(fixedRate = 5000)
    public void fixedRateTask() { }

    @Scheduled(cron = "0 0 2 * * ?")
    public void cronTask() { }
}
```

> ⚠️ 默认单线程，多任务排队。`@Scheduled` 方法必须无参无返回值。

### 1.2 TaskScheduler（动态控制）

```java
@Service
public class DynamicTaskService {
    @Resource
    private TaskScheduler taskScheduler;

    public void startCronTask(String cron) {
        taskScheduler.schedule(() -> System.out.println("执行"),
                new CronTrigger(cron));
    }
}
```

### 1.3 Quartz（复杂场景）

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-quartz</artifactId>
</dependency>
```

```java
public class QuartzTask implements Job {
    @Override
    public void execute(JobExecutionContext context) {
        System.out.println("Quartz 任务执行");
    }
}
```

---

## 2. 邮件发送

### 前置准备（QQ 邮箱）

> 设置 → 账户 → 开启 SMTP 服务 → 获取授权码。

### 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

### 配置

```yaml
spring:
  mail:
    host: smtp.qq.com
    port: 587
    username: 123456@qq.com
    password: 授权码（不是邮箱密码）
    default-encoding: UTF-8
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

### 四种邮件类型

| 类型 | 核心 API |
|------|----------|
| **简单文本** | `SimpleMailMessage` |
| **HTML** | `MimeMessageHelper + setText(html, true)` |
| **带附件** | `helper.addAttachment(name, FileSystemResource)` |
| **内嵌图片** | `helper.addInline(cid, FileSystemResource)` |

```java
@Service
public class EmailService {
    @Resource
    private JavaMailSender javaMailSender;
    private static final String FROM = "123456@qq.com";

    // 简单文本
    public void sendSimple(String to, String subject, String text) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(FROM); msg.setTo(to);
        msg.setSubject(subject); msg.setText(text);
        javaMailSender.send(msg);
    }

    // HTML
    public void sendHtml(String to, String subject, String html) throws MessagingException {
        MimeMessage msg = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setFrom(FROM); helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        javaMailSender.send(msg);
    }
}
```

---

## 3. 整合实例

```java
@Component
public class ScheduledEmailTask {
    @Resource
    private EmailService emailService;

    @Scheduled(cron = "0 0 8 * * ?")  // 每天早上 8 点
    public void sendDailyEmail() {
        emailService.sendSimple("user@qq.com", "每日通知", "早上好！");
    }
}
```

---

## 常见问题

| 问题 | 解决 |
|------|------|
| 任务不执行 | `@EnableScheduling` + `@Component` + cron 正确 |
| 邮件发送失败 | 授权码（非密码）+ SMTP 已开启 + host/port 正确 |
| HTML 乱码 | `default-encoding: UTF-8` + `MimeMessageHelper` 指定 UTF-8 |
