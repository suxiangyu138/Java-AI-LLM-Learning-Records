import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Java数据库连接基础 - 批量插入1000条数据
 * 包含：普通循环插入 + JDBC批量插入（推荐）
 * 仅改【数据库配置项】为自己的MySQL账号密码
 */
public class JDBCDemo {
    // ---------------------- ★★★ 数据库配置项（只改这3行！）★★★ ----------------------
    private static final String DB_URL = "jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root"; // 你的MySQL账号
    private static final String DB_PWD = "123456"; // 你的MySQL密码

    // 获取数据库连接
    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PWD);
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("数据库连接失败：" + e.getMessage());
        }
        return conn;
    }

    // 关闭数据库资源
    public static void closeResources(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
            if (conn != null) {
                // 批量插入后恢复自动提交（避免影响后续操作）
                conn.setAutoCommit(true);
                conn.close();
            }
        } catch (SQLException e) {
            System.err.println("关闭资源失败：" + e.getMessage());
        }
    }

    // 【方法1：普通循环插入1000条数据】- 基础写法，速度较慢
    public static void insert1000DataNormal() {
        long start = System.currentTimeMillis(); // 记录开始时间，计算耗时
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = getConnection();
            if (conn == null) return;
            // SQL：自增id，无需传id，只插姓名和年龄
            String sql = "INSERT INTO student(name, age) VALUES (?, ?)";
            pstmt = conn.prepareStatement(sql);

            // 循环插入1000条
            for (int i = 1; i <= 1000; i++) {
                // 生成测试数据：姓名=学生+i，年龄=18-25随机
                pstmt.setString(1, "学生" + i);
                pstmt.setInt(2, 18 + (int) (Math.random() * 8));
                pstmt.executeUpdate(); // 逐条执行SQL
            }

            long end = System.currentTimeMillis();
            System.out.println("✅ 普通循环插入1000条数据完成！");
            System.out.println("⏱ 耗时：" + (end - start) + " 毫秒");
        } catch (SQLException e) {
            System.err.println("❌ 普通插入失败：" + e.getMessage());
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    // 【方法2：JDBC批量插入1000条数据】- 优化写法，速度极快（推荐）
    public static void insert1000DataBatch() {
        long start = System.currentTimeMillis();
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = getConnection();
            if (conn == null) return;

            // 核心1：关闭自动提交（默认true，每条SQL都提交；关闭后统一批量提交）
            conn.setAutoCommit(false);
            // SQL：自增id，只插姓名和年龄
            String sql = "INSERT INTO student(name, age) VALUES (?, ?)";
            pstmt = conn.prepareStatement(sql);

            // 循环添加批量SQL
            for (int i = 1; i <= 1000; i++) {
                pstmt.setString(1, "批量学生" + i);
                pstmt.setInt(2, 18 + (int) (Math.random() * 8));
                pstmt.addBatch(); // 核心2：将SQL加入批量任务，不立即执行
            }

            // 核心3：执行批量提交，一次性执行所有加入的SQL
            int[] rows = pstmt.executeBatch();
            // 核心4：手动提交事务（关闭自动提交后必须手动提交，否则数据不生效）
            conn.commit();

            long end = System.currentTimeMillis();
            System.out.println("✅ JDBC批量插入1000条数据完成！");
            System.out.println("⏱ 耗时：" + (end - start) + " 毫秒");
            System.out.println("📊 成功插入：" + rows.length + " 条（数组长度=成功条数）");
        } catch (SQLException e) {
            // 核心5：插入失败时回滚事务，避免部分数据插入成功
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            System.err.println("❌ 批量插入失败：" + e.getMessage());
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    // 辅助方法：查询表中总数据条数，验证插入是否成功
    public static void queryTotalCount() {
        Connection conn = getConnection();
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT COUNT(*) FROM student";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                System.out.println("\n📈 学生表中当前总数据条数：" + count + " 条");
            }
        } catch (SQLException e) {
            System.err.println("查询总数失败：" + e.getMessage());
        } finally {
            closeResources(conn, pstmt, rs);
        }
    }

    // 程序入口：测试两种插入方式
    public static void main(String[] args) {
        System.out.println("===== 开始测试普通循环插入1000条 =====");
        insert1000DataNormal(); // 测试普通插入

        System.out.println("\n===== 开始测试JDBC批量插入1000条 =====");
        insert1000DataBatch(); // 测试批量插入

        System.out.println("\n===== 插入完成，验证数据总数 =====");
        queryTotalCount(); // 验证总条数（应该≈2000条，含两种方式插入的）
    }
}