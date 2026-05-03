// 所有类的第一行必须添加：
package org.example;
import java.lang.reflect.Field;

/**
 * 运行时注解处理器：校验 @NotNull 注解的字段是否为空
 */
public class NotNullValidator {
    /**
     * 校验对象中所有 @NotNull 字段
     */
    public static void validate(Object obj) throws IllegalAccessException {
        if (obj == null) {
            throw new IllegalArgumentException("❌ 待校验对象不能为空");
        }

        // 获取对象所有字段
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            // 检查字段是否有 @NotNull 注解
            NotNull notNull = field.getAnnotation(NotNull.class);
            if (notNull != null) {
                field.setAccessible(true); // 允许访问私有字段
                Object value = field.get(obj);
                // 校验字段值是否为空
                if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                    throw new IllegalArgumentException("❌ " + field.getName() + "：" + notNull.message());
                }
            }
        }
        System.out.println("✅ 所有 @NotNull 字段校验通过！");
    }
}