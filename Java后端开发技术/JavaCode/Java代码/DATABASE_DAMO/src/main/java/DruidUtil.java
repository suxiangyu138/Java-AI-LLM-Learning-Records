import com.alibaba.druid.pool.DruidDataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;

public class DruidUtil {
    private static final DruidDataSource DATA_SOURCE;

    static {
        DATA_SOURCE = new DruidDataSource();

        // 1. 数据库基础配置
        DATA_SOURCE.setUrl("jdbc:mysql://localhost:3306/jdbc_demo?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai");
        DATA_SOURCE.setUsername("root");
        DATA_SOURCE.setPassword("123456");
        DATA_SOURCE.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // 2. 连接池核心配置
        DATA_SOURCE.setInitialSize(5);
        DATA_SOURCE.setMaxActive(20);
        DATA_SOURCE.setMinIdle(5);
        DATA_SOURCE.setMaxWait(60000);
        DATA_SOURCE.setTimeBetweenEvictionRunsMillis(60000);
        DATA_SOURCE.setMinEvictableIdleTimeMillis(300000);

        // 3. 连接有效性检测
        DATA_SOURCE.setTestWhileIdle(true);
        DATA_SOURCE.setTestOnBorrow(false);
        DATA_SOURCE.setTestOnReturn(false);

        // 4. 开启监控统计（修复：用 try-catch 捕获异常）
        try {
            DATA_SOURCE.setFilters("stat");
        } catch (SQLException e) {
            System.err.println("❌ Druid 开启监控统计失败：" + e.getMessage());
        }
    }

    public static Connection getConnection() {
        try {
            return DATA_SOURCE.getConnection();
        } catch (SQLException e) {
            System.err.println("❌ 从连接池获取连接失败：" + e.getMessage());
            return null;
        }
    }

    public static void close(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static DruidDataSource getDataSource() {
        return DATA_SOURCE;
    }

    private DruidUtil() {}
}