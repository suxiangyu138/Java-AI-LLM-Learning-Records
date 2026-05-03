import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcCrudDemo {
    // 数据库连接信息（和你的测试代码一致）
    private static final String URL = "jdbc:mysql://localhost:3306/jdbc_demo?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";

    public static void main(String[] args) {
        // 1. 新增学生
        insertStudent("赵六", 20, "女", 88.0);
        // 2. 修改学生成绩
        updateStudentScore(4, 90.0);
        // 3. 查询所有学生
        queryAllStudents();
        // 4. 删除学生（可选，注释掉默认不执行）
        // deleteStudent(4);
    }

    /**
     * 新增学生
     */
    public static void insertStudent(String name, int age, String gender, double score) {
        String sql = "INSERT INTO student (name, age, gender, score) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setInt(2, age);
            pstmt.setString(3, gender);
            pstmt.setDouble(4, score);
            int rows = pstmt.executeUpdate();
            System.out.println("✅ 新增成功，影响行数：" + rows);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 修改学生成绩
     */
    public static void updateStudentScore(int id, double newScore) {
        String sql = "UPDATE student SET score = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, newScore);
            pstmt.setInt(2, id);
            int rows = pstmt.executeUpdate();
            System.out.println("✅ 修改成功，影响行数：" + rows);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询所有学生
     */
    public static void queryAllStudents() {
        String sql = "SELECT id, name, age, gender, score FROM student";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            System.out.println("\n===== 学生列表 =====");
            while (rs.next()) {
                System.out.printf("ID：%d，姓名：%s，年龄：%d，性别：%s，成绩：%.1f%n",
                        rs.getInt("id"), rs.getString("name"), rs.getInt("age"),
                        rs.getString("gender"), rs.getDouble("score"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除学生
     */
    public static void deleteStudent(int id) {
        String sql = "DELETE FROM student WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int rows = pstmt.executeUpdate();
            System.out.println("✅ 删除成功，影响行数：" + rows);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}