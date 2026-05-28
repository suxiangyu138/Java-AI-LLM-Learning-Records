package org.example;

public class AnnotationTest {
    // 标注 @NotNull 注解（使用自定义注解，消除“从未使用”警告）
    @NotNull(message = "用户名不能为空")
    private String username;

    private Integer age;

    public AnnotationTest(String username, Integer age) {
        this.username = username;
        this.age = age;
    }

    public static void main(String[] args) {
        try {
            // 调用校验方法（触发注解读取，消除 message() 未使用警告）
            AnnotationTest test = new AnnotationTest("Java 新手", 20);
            NotNullValidator.validate(test);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}