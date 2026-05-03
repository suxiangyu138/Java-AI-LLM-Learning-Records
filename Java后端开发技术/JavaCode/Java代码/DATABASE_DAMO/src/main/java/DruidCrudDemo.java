import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 使用 Druid 连接池实现数据库增删改查
 * 性能远高于原生 DriverManager，企业开发标配
 */
public class DruidCrudDemo {
    public static void main(String[] args) {
        // 1. 新增学生
        insertStudentWithDruid("孙七", 21, "男", 89.5);
        // 2. 查询所有学生
        queryAllStudentsWithDruid();
        // 3. 修改成绩
        updateStudentScoreWithDruid(5, 92.0);
        // 4. 再次查询验证
        queryAllStudentsWithDruid();
    }

    /**
     * 新增学生（Druid 版）
     */
    public static void insertStudentWithDruid(String name, int age, String gender, double score) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        String sql = "INSERT INTO student (name, age, gender, score) VALUES (?, ?, ?, ?)";

        try {
            conn = DruidUtil.getConnection(); // 从连接池获取连接
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, name);
            pstmt.setInt(2, age);
            pstmt.setString(3, gender);
            pstmt.setDouble(4, score);

            int rows = pstmt.executeUpdate();
            System.out.println("✅ Druid 新增成功，影响行数：" + rows);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DruidUtil.close(conn, pstmt, null); // 归还连接到池
        }
    }

    /**
     * 修改成绩（Druid 版）
     */
    public static void updateStudentScoreWithDruid(int id, double newScore) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        String sql = "UPDATE student SET score = ? WHERE id = ?";

        try {
            conn = DruidUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setDouble(1, newScore);
            pstmt.setInt(2, id);

            int rows = pstmt.executeUpdate();
            System.out.println("✅ Druid 修改成功，影响行数：" + rows);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DruidUtil.close(conn, pstmt, null);
        }
    }

    /**
     * 查询所有学生（Druid 版）
     */
    public static void queryAllStudentsWithDruid() {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sql = "SELECT id, name, age, gender, score FROM student";

        try {
            conn = DruidUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            System.out.println("\n===== Druid 连接池 - 学生列表 =====");
            while (rs.next()) {
                System.out.printf("ID：%d，姓名：%s，年龄：%d，性别：%s，成绩：%.1f%n",
                        rs.getInt("id"), rs.getString("name"), rs.getInt("age"),
                        rs.getString("gender"), rs.getDouble("score"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DruidUtil.close(conn, pstmt, rs); // 归还连接+关闭结果集
        }
    }
}