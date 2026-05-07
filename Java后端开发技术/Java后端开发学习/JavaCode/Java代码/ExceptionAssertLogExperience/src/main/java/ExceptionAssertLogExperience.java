import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 * Java 异常、断言、日志 核心操作综合体验
 * 覆盖：异常处理（try-catch/自定义异常）、断言使用、日志框架（SLF4J+Logback）
 */
public class ExceptionAssertLogExperience {
    // ===== 日志初始化（SLF4J+Logback）=====
    // 最佳实践：LoggerFactory.getLogger(当前类.class)
    private static final Logger logger = LoggerFactory.getLogger(ExceptionAssertLogExperience.class);

    public static void main(String[] args) {
        // ====================== 模块1：异常处理核心用法 ======================
        System.out.println("===== 模块1：异常处理 =====");
        Scanner scanner = new Scanner(System.in);

        // 1.1 try-catch-finally 基础用法
        try {
            System.out.print("请输入被除数：");
            int dividend = Integer.parseInt(scanner.nextLine());
            System.out.print("请输入除数：");
            int divisor = Integer.parseInt(scanner.nextLine());

            // 调用可能抛出异常的方法
            int result = divide(dividend, divisor);
            System.out.println("除法结果：" + result);

        } catch (NumberFormatException e) {
            // 捕获特定异常：输入非数字
            System.err.println("异常：请输入有效的整数！");
            logger.error("数字格式异常", e); // 日志记录异常（含堆栈）
        } catch (DivideByZeroException e) {
            // 捕获自定义异常：除数为0
            System.err.println("异常：" + e.getMessage());
            logger.error("自定义除数为0异常", e);
        } catch (Exception e) {
            // 兜底捕获所有异常（不推荐直接捕获Exception，仅演示）
            System.err.println("未知异常：" + e.getMessage());
            logger.error("未知异常", e);
        } finally {
            // 无论是否异常，都会执行：释放资源
            System.out.println("finally块执行：关闭Scanner");
            scanner.close();
            logger.info("Scanner资源已释放");
        }

        // 1.2 异常链：包装底层异常为业务异常
        try {
            writeFile("test.txt", "测试内容");
        } catch (FileOperateException e) {
            System.err.println("文件操作失败：" + e.getMessage());
            logger.error("文件操作异常", e);
        }
        System.out.println();

        // ====================== 模块2：断言（Assert）使用 ======================
        System.out.println("===== 模块2：断言 =====");
        // 2.1 断言基础语法：assert 条件 : "失败提示"
        int age = 17;
        // 注意：断言默认关闭，需通过JVM参数启用：-ea 或 -enableassertions
        assert age >= 18 : "年龄必须大于等于18！当前年龄：" + age;
        System.out.println("断言通过：年龄合法"); // 启用断言时，age=17会抛出AssertionError

        // 2.2 断言适用场景：调试阶段的参数校验
        String username = "admin";
        assert username != null && !username.isEmpty() : "用户名不能为空！";
        logger.debug("用户名校验通过：{}", username);
        System.out.println();

        // ====================== 模块3：日志框架最佳实践 ======================
        System.out.println("===== 模块3：日志使用 =====");
        // 3.1 日志级别（从低到高：TRACE < DEBUG < INFO < WARN < ERROR）
        logger.trace("TRACE级别：最细粒度日志（开发调试）");
        logger.debug("DEBUG级别：调试信息（开发/测试）");
        logger.info("INFO级别：关键业务流程（生产环境）");
        logger.warn("WARN级别：警告（非错误，但需关注）");
        logger.error("ERROR级别：错误（影响业务，必须处理）");

        // 3.2 日志格式化（最佳实践：占位符{}，避免字符串拼接）
        String userId = "U123456";
        double amount = 99.99;
        logger.info("用户{}完成支付，金额：{}元", userId, amount);

        // 3.3 对比System.out vs 日志（System.out无级别、无法配置、性能差）
        System.out.println("System.out打印（无级别/无法配置）");
        logger.info("日志打印（有级别/可配置/异步）");
    }

    // ===== 异常处理相关方法 =====
    /**
     * 除法运算：手动抛出自定义异常
     * @throws DivideByZeroException 除数为0时抛出
     */
    public static int divide(int dividend, int divisor) throws DivideByZeroException {
        // 前置校验：除数为0
        if (divisor == 0) {
            // 抛出自定义异常（含业务信息）
            throw new DivideByZeroException("除数不能为0！");
        }
        return dividend / divisor;
    }

    /**
     * 文件写入：演示异常链（包装IOException为业务异常）
     */
    public static void writeFile(String filename, String content) throws FileOperateException {
        try (FileWriter writer = new FileWriter(filename)) { // try-with-resources 自动关闭资源
            writer.write(content);
            logger.info("文件{}写入成功，内容：{}", filename, content);
        } catch (IOException e) {
            // 异常链：包装底层IO异常为业务异常
            throw new FileOperateException("写入文件失败：" + filename, e);
        }
    }

    // ====================== 自定义异常（业务异常）======================
    /**
     * 自定义除数为0异常（继承RuntimeException：非检查异常）
     * 最佳实践：自定义异常继承RuntimeException（无需强制捕获）
     */
    static class DivideByZeroException extends RuntimeException {
        public DivideByZeroException(String message) {
            super(message); // 调用父类构造
        }
    }

    /**
     * 自定义文件操作异常（继承RuntimeException）
     */
    static class FileOperateException extends RuntimeException {
        // 异常链构造方法：传入cause（底层异常）
        public FileOperateException(String message, Throwable cause) {
            super(message, cause);
        }

        public FileOperateException(String message) {
            super(message);
        }
    }
}