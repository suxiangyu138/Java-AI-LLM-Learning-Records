import java.util.Scanner;

/**
 * 控制台输入输出示例
 * 功能：读取用户输入的姓名、年龄、成绩，并格式化输出
 */

public class ConsoleIO {
    public static void main(String[] args) {
        // 1. 创建Scanner对象，读取控制台输入
        Scanner scanner = new Scanner(System.in);

        try {
            // ========== 控制台输出 ==========
            System.out.println("===== 控制台输入演示 =====");
            System.out.print("请输入你的姓名："); // print 不换行
            String name = scanner.nextLine(); // 读取整行字符串（包含空格）

            System.out.print("请输入你的年龄：");
            int age = scanner.nextInt(); // 读取整数

            System.out.print("请输入你的成绩：");
            double score = scanner.nextDouble(); // 读取浮点数

            // ========== 格式化输出 ==========
            System.out.println("\n===== 格式化输出结果 =====");
            // 方式1：字符串拼接
            System.out.println("姓名：" + name + "，年龄：" + age + "，成绩：" + score);

            // 方式2：printf 格式化（推荐，更优雅）
            // %s:字符串 %d:整数 %.2f:保留2位小数 %n:换行
            System.out.printf("姓名：%s，年龄：%d，成绩：%.2f%n", name, age, score);

        } catch (Exception e) {
            System.err.println("输入格式错误！请输入正确的数据类型。");
        } finally {
            // 关闭Scanner，释放资源
            scanner.close();
        }
    }
}
