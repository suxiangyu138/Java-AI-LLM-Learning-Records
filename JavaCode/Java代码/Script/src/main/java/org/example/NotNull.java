package org.example;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 注解保留到运行时（反射可获取）
@Retention(RetentionPolicy.RUNTIME)
// 注解作用于字段
@Target(ElementType.FIELD)
public @interface NotNull {
    // 定义 message 属性，设置默认值
    String message() default "字段不能为空";
}