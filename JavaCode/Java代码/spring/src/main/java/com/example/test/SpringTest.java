package com.example.test;

import com.example.entity.User;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringTest {
    public static void main(String[] args) {
        // 加载 Spring 配置文件
        ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
        // 获取 Bean 对象
        User user = (User) context.getBean("user");
        // 输出结果
        System.out.println(user);
    }
}