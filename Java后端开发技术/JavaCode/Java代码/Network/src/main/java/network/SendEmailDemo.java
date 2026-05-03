package network;

import jakarta.mail.*; // 替换 javax.mail 为 jakarta.mail
import jakarta.mail.internet.InternetAddress; // 替换 javax.mail.internet
import jakarta.mail.internet.MimeMessage; // 替换 javax.mail.internet
import java.util.Properties;

public class SendEmailDemo {
    public static void main(String[] args) {
        String smtpHost = "smtp.qq.com";
        String fromEmail = "你的QQ邮箱@qq.com";
        String authCode = "你的QQ邮箱授权码";
        String toEmail = "收件人邮箱@xxx.com";

        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Authenticator auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, authCode);
            }
        };

        Session session = Session.getInstance(props, auth);

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Java网络编程-测试邮件");
            message.setText("这是通过Jakarta Mail发送的测试邮件！");

            Transport.send(message);
            System.out.println("邮件发送成功！");

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}