import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestMysqlConnection {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/jdbc_demo?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai";
        String user = "root";
        String password = "123456";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("✅ MySQL 连接成功！依赖引入没问题");
        } catch (SQLException e) {
            System.err.println("❌ 连接失败：" + e.getMessage());
            System.err.println("🔍 检查：1. MySQL 服务是否启动  2. 用户名/密码是否正确");
        }
    }
}