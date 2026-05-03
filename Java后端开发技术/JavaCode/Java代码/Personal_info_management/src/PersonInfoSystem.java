import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;

/**
 * 个人信息管理系统 - Swing+MySQL数据库版（完整可运行）
 */
public class PersonInfoSystem extends JFrame {
    // ---------------------- 数据库配置项（修改为你的MySQL账号密码） ----------------------
    private static final String DB_URL = "jdbc:mysql://localhost:3306/person_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root"; // 你的MySQL账号
    private static final String DB_PWD = "123456"; // 你的MySQL密码

    // 界面组件
    private JTextField tfId, tfName, tfAge, tfPhone, tfEmail;
    private JTextArea taResult;

    // ---------------------- 1. 个人信息实体类 ----------------------
    static class Person {
        private int id;
        private String name;
        private int age;
        private String phone;
        private String email;

        public Person() {}
        public Person(int id, String name, int age, String phone, String email) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.phone = phone;
            this.email = email;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        @Override
        public String toString() {
            return "编号：" + id + " | 姓名：" + name + " | 年龄：" + age + " | 手机号：" + phone + " | 邮箱：" + email;
        }
    }

    // ---------------------- 2. 正则校验工具方法 ----------------------
    private static final String REGEX_NAME = "^[\\u4e00-\\u9fa5a-zA-Z\\s]{1,10}$";
    private static final String REGEX_PHONE = "^1[3-9]\\d{9}$";
    private static final String REGEX_EMAIL = "^[a-zA-Z0-9_.-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z]{2,10}$";

    private static boolean isNameValid(String name) {
        return name != null && !name.trim().isEmpty() && name.matches(REGEX_NAME);
    }
    private static boolean isPhoneValid(String phone) {
        return phone != null && phone.matches(REGEX_PHONE);
    }
    private static boolean isEmailValid(String email) {
        return email != null && email.matches(REGEX_EMAIL);
    }
    private static boolean isAgeValid(int age) {
        return age >= 1 && age <= 150;
    }
    private static boolean isIdValid(String idStr) {
        try {
            int id = Integer.parseInt(idStr);
            return id > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ---------------------- 3. 数据库核心工具方法 ----------------------
    private Connection getConn() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PWD);
        } catch (ClassNotFoundException | SQLException e) {
            JOptionPane.showMessageDialog(this, "数据库连接失败！\n原因：" + e.getMessage(), "数据库错误", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void closeRes(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTable() {
        Connection conn = getConn();
        Statement stmt = null;
        if (conn == null) return;
        String createSql = "CREATE TABLE IF NOT EXISTS person_info (" +
                "id INT PRIMARY KEY COMMENT '唯一编号'," +
                "name VARCHAR(50) NOT NULL COMMENT '姓名'," +
                "age INT NOT NULL COMMENT '年龄'," +
                "phone VARCHAR(20) NOT NULL COMMENT '手机号'," +
                "email VARCHAR(100) NOT NULL COMMENT '邮箱'" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人信息表';";
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate(createSql);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "建表失败！\n原因：" + e.getMessage(), "数据库错误", JOptionPane.ERROR_MESSAGE);
        } finally {
            closeRes(conn, stmt, null);
        }
    }

    private int getTotalCount() {
        Connection conn = getConn();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        if (conn == null) return 0;
        try {
            String sql = "SELECT COUNT(*) FROM person_info";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
            return 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        } finally {
            closeRes(conn, pstmt, rs);
        }
    }

    // ---------------------- 4. 界面工具方法 ----------------------
    private void clearInput() {
        tfId.setText("");
        tfName.setText("");
        tfAge.setText("");
        tfPhone.setText("");
        tfEmail.setText("");
        tfId.requestFocus();
    }

    private void refreshResult(String content) {
        taResult.setText(content);
        taResult.setCaretPosition(0);
    }

    // ---------------------- 5. 初始化界面 ----------------------
    public PersonInfoSystem() {
        createTable(); // 自动建表

        setTitle("个人信息管理系统 - Swing+MySQL版");
        setSize(850, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        add(mainPanel);

        // 输入面板
        JPanel inputPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder("信息输入"));
        inputPanel.add(new JLabel("唯一编号（正整数）："));
        tfId = new JTextField();
        inputPanel.add(tfId);

        inputPanel.add(new JLabel("姓名（1-10位中/英/空格）："));
        tfName = new JTextField();
        inputPanel.add(tfName);

        inputPanel.add(new JLabel("年龄（1-150岁）："));
        tfAge = new JTextField();
        inputPanel.add(tfAge);

        inputPanel.add(new JLabel("手机号（11位）："));
        tfPhone = new JTextField();
        inputPanel.add(tfPhone);

        inputPanel.add(new JLabel("邮箱（如xxx@163.com）："));
        tfEmail = new JTextField();
        inputPanel.add(tfEmail);

        inputPanel.add(new JLabel(""));
        JButton btnClear = new JButton("清空输入");
        inputPanel.add(btnClear);
        mainPanel.add(inputPanel, BorderLayout.NORTH);

        // 按钮面板
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnPanel.setBorder(BorderFactory.createTitledBorder("功能操作"));
        Dimension btnSize = new Dimension(120, 35);
        JButton btnAdd = new JButton("添加信息");
        JButton btnQueryAll = new JButton("查询全部");
        JButton btnQueryById = new JButton("按编号查询");
        JButton btnUpdate = new JButton("按编号修改");
        JButton btnDelete = new JButton("按编号删除");
        btnAdd.setPreferredSize(btnSize);
        btnQueryAll.setPreferredSize(btnSize);
        btnQueryById.setPreferredSize(btnSize);
        btnUpdate.setPreferredSize(btnSize);
        btnDelete.setPreferredSize(btnSize);
        btnPanel.add(btnAdd);
        btnPanel.add(btnQueryAll);
        btnPanel.add(btnQueryById);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);
        mainPanel.add(btnPanel, BorderLayout.CENTER);

        // 结果面板
        JPanel resultPanel = new JPanel(new BorderLayout(10, 10));
        resultPanel.setBorder(BorderFactory.createTitledBorder("操作结果"));
        taResult = new JTextArea();
        taResult.setFont(new Font("等线", Font.PLAIN, 14));
        taResult.setLineWrap(true);
        taResult.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(taResult);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        resultPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(resultPanel, BorderLayout.SOUTH);

        // ---------------------- 按钮事件 ----------------------
        btnClear.addActionListener(e -> clearInput());

        // 添加
        btnAdd.addActionListener(e -> {
            String idStr = tfId.getText().trim();
            String name = tfName.getText().trim();
            String ageStr = tfAge.getText().trim();
            String phone = tfPhone.getText().trim();
            String email = tfEmail.getText().trim();

            if (idStr.isEmpty() || name.isEmpty() || ageStr.isEmpty() || phone.isEmpty() || email.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请填写所有信息！", "输入错误", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!isIdValid(idStr)) {
                JOptionPane.showMessageDialog(this, "编号必须为正整数！", "输入错误", JOptionPane.WARNING_MESSAGE);
                tfId.requestFocus();
                return;
            }
            int id = Integer.parseInt(idStr);
            if (!isNameValid(name)) {
                JOptionPane.showMessageDialog(this, "姓名格式错误！", "输入错误", JOptionPane.WARNING_MESSAGE);
                tfName.requestFocus();
                return;
            }
            int age;
            try {
                age = Integer.parseInt(ageStr);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "年龄必须为整数！", "输入错误", JOptionPane.WARNING_MESSAGE);
                tfAge.requestFocus();
                return;
            }
            if (!isAgeValid(age)) {
                JOptionPane.showMessageDialog(this, "年龄必须在1-150岁之间！", "输入错误", JOptionPane.WARNING_MESSAGE);
                tfAge.requestFocus();
                return;
            }
            if (!isPhoneValid(phone)) {
                JOptionPane.showMessageDialog(this, "手机号格式错误！", "输入错误", JOptionPane.WARNING_MESSAGE);
                tfPhone.requestFocus();
                return;
            }
            if (!isEmailValid(email)) {
                JOptionPane.showMessageDialog(this, "邮箱格式错误！", "输入错误", JOptionPane.WARNING_MESSAGE);
                tfEmail.requestFocus();
                return;
            }

            Connection conn = getConn();
            PreparedStatement pstmt = null;
            if (conn == null) return;
            try {
                String checkSql = "SELECT id FROM person_info WHERE id=?";
                pstmt = conn.prepareStatement(checkSql);
                pstmt.setInt(1, id);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "编号已存在！", "添加失败", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                closeRes(null, pstmt, rs);

                String insertSql = "INSERT INTO person_info(id, name, age, phone, email) VALUES (?, ?, ?, ?, ?)";
                pstmt = conn.prepareStatement(insertSql);
                pstmt.setInt(1, id);
                pstmt.setString(2, name);
                pstmt.setInt(3, age);
                pstmt.setString(4, phone);
                pstmt.setString(5, email);
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "添加成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                clearInput();
                refreshResult("当前共" + getTotalCount() + "条信息");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "添加失败！\n原因：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            } finally {
                closeRes(conn, pstmt, null);
            }
        });

        // 查询全部
        btnQueryAll.addActionListener(e -> {
            Connection conn = getConn();
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            if (conn == null) return;
            try {
                String sql = "SELECT * FROM person_info ORDER BY id ASC";
                pstmt = conn.prepareStatement(sql);
                rs = pstmt.executeQuery();
                StringBuilder sb = new StringBuilder();
                int count = 0;
                while (rs.next()) {
                    count++;
                    sb.append(count).append("、").append("编号：").append(rs.getInt("id"))
                            .append(" | 姓名：").append(rs.getString("name"))
                            .append(" | 年龄：").append(rs.getInt("age"))
                            .append(" | 手机号：").append(rs.getString("phone"))
                            .append(" | 邮箱：").append(rs.getString("email")).append("\n\n");
                }
                if (count == 0) refreshResult("暂无信息！");
                else refreshResult("共" + count + "条信息：\n\n" + sb);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "查询失败！\n原因：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            } finally {
                closeRes(conn, pstmt, rs);
            }
        });

        // 按编号查询
        btnQueryById.addActionListener(e -> {
            String idStr = tfId.getText().trim();
            if (idStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请输入编号！", "输入错误", JOptionPane.WARNING_MESSAGE);
                tfId.requestFocus();
                return;
            }
            if (!isIdValid(idStr)) {
                JOptionPane.showMessageDialog(this, "编号必须为正整数！", "输入错误", JOptionPane.WARNING_MESSAGE);
                tfId.requestFocus();
                return;
            }
            int id = Integer.parseInt(idStr);

            Connection conn = getConn();
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            if (conn == null) return;
            try {
                String sql = "SELECT * FROM person_info WHERE id=?";
                pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, id);
                rs = pstmt.executeQuery();
                if (rs.next()) {
                    tfName.setText(rs.getString("name"));
                    tfAge.setText(String.valueOf(rs.getInt("age")));
                    tfPhone.setText(rs.getString("phone"));
                    tfEmail.setText(rs.getString("email"));
                    refreshResult("查询到：\n编号：" + id + "\n姓名：" + rs.getString("name") + "\n年龄：" + rs.getInt("age") + "\n手机号：" + rs.getString("phone") + "\n邮箱：" + rs.getString("email"));
                } else {
                    refreshResult("未找到编号" + id + "的信息！");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "查询失败！\n原因：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            } finally {
                closeRes(conn, pstmt, rs);
            }
        });

        // 修改
        btnUpdate.addActionListener(e -> {
            String idStr = tfId.getText().trim();
            String name = tfName.getText().trim();
            String ageStr = tfAge.getText().trim();
            String phone = tfPhone.getText().trim();
            String email = tfEmail.getText().trim();

            if (idStr.isEmpty() || name.isEmpty() || ageStr.isEmpty() || phone.isEmpty() || email.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请填写所有信息！", "输入错误", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!isIdValid(idStr)) {
                JOptionPane.showMessageDialog(this, "编号必须为正整数！", "输入错误", JOptionPane.WARNING_MESSAGE);
                tfId.requestFocus();
                return;
            }
            int id = Integer.parseInt(idStr);
            if (!isNameValid(name)) {
                JOptionPane.showMessageDialog(this, "姓名格式错误！", "输入错误", JOptionPane.WARNING_MESSAGE);
                tfName.requestFocus();
                return;
            }
            int age;
            try {
                age = Integer.parseInt(ageStr);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "年龄必须为整数！", "输入错误", JOptionPane.WARNING_MESSAGE);
                tfAge.requestFocus();
                return;
            }
            if (!isAgeValid(age)) {
                JOptionPane.showMessageDialog(this, "年龄必须在1-150岁之间！", "输入错误", JOptionPane.WARNING_MESSAGE);
                tfAge.requestFocus();
                return;
            }
            if (!isPhoneValid(phone)) {
                JOptionPane.showMessageDialog(this, "手机号格式错误！", "输入错误", JOptionPane.WARNING_MESSAGE);
                tfPhone.requestFocus();
                return;
            }
            if (!isEmailValid(email)) {
                JOptionPane.showMessageDialog(this, "邮箱格式错误！", "输入错误", JOptionPane.WARNING_MESSAGE);
                tfEmail.requestFocus();
                return;
            }

            Connection conn = getConn();
            PreparedStatement pstmt = null;
            if (conn == null) return;
            try {
                String checkSql = "SELECT id FROM person_info WHERE id=?";
                pstmt = conn.prepareStatement(checkSql);
                pstmt.setInt(1, id);
                ResultSet rs = pstmt.executeQuery();
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(this, "未找到编号" + id + "的信息！", "修改失败", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                closeRes(null, pstmt, rs);

                String updateSql = "UPDATE person_info SET name=?, age=?, phone=?, email=? WHERE id=?";
                pstmt = conn.prepareStatement(updateSql);
                pstmt.setString(1, name);
                pstmt.setInt(2, age);
                pstmt.setString(3, phone);
                pstmt.setString(4, email);
                pstmt.setInt(5, id);
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "修改成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                clearInput();
                refreshResult("编号" + id + "的信息已修改");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "修改失败！\n原因：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            } finally {
                closeRes(conn, pstmt, null);
            }
        });

        // 删除（你现在这段的完整修复版）
        btnDelete.addActionListener(e -> {
            String idStr = tfId.getText().trim();
            if (idStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请输入要删除的编号！", "输入错误", JOptionPane.WARNING_MESSAGE);
                tfId.requestFocus();
                return;
            }
            if (!isIdValid(idStr)) {
                JOptionPane.showMessageDialog(this, "编号必须为正整数！", "输入错误", JOptionPane.WARNING_MESSAGE);
                tfId.requestFocus();
                return;
            }
            int id = Integer.parseInt(idStr);

            Connection conn = getConn();
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            if (conn == null) return;
            try {
                String checkSql = "SELECT id FROM person_info WHERE id=?";
                pstmt = conn.prepareStatement(checkSql);
                pstmt.setInt(1, id);
                rs = pstmt.executeQuery();
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(this, "未找到编号为" + id + "的个人信息，无法删除！", "删除失败", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "查询编号失败！\n原因：" + ex.getMessage(), "数据库错误", JOptionPane.ERROR_MESSAGE);
                closeRes(conn, pstmt, rs);
                return;
            } finally {
                closeRes(null, pstmt, rs);
            }

            int confirm = JOptionPane.showConfirmDialog(this, "确定要删除编号为" + id + "的信息吗？\n删除后数据将从MySQL永久删除，无法恢复！", "确认删除", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                refreshResult("已取消删除编号" + id + "的操作");
                closeRes(conn, null, null);
                return;
            }

            try {
                String deleteSql = "DELETE FROM person_info WHERE id=?";
                pstmt = conn.prepareStatement(deleteSql);
                pstmt.setInt(1, id);
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "编号" + id + "的信息已从MySQL数据库永久删除！", "删除成功", JOptionPane.INFORMATION_MESSAGE);
                clearInput();
                refreshResult("✅ 编号" + id + "的信息已删除，当前数据库剩余" + getTotalCount() + "条信息");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "删除失败！\n原因：" + ex.getMessage(), "数据库错误", JOptionPane.ERROR_MESSAGE);
            } finally {
                closeRes(conn, pstmt, null);
            }
        });
    }

    // 程序入口
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new PersonInfoSystem().setVisible(true);
        });
    }
}