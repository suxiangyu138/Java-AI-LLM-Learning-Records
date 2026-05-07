import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Java Swing GUI 程序设计综合示例
 * 包含：基础组件、布局管理、事件处理、表格/菜单/对话框等核心功能
 * 实战案例：学生信息管理小应用
 */
public class SwingGuiDesign extends JFrame {
    // 全局组件（方便事件处理方法访问）
    private JTextField tfName, tfAge;
    private JComboBox<String> cbGender;
    private JTable studentTable;
    private DefaultTableModel tableModel;

    // 构造方法：初始化界面
    public SwingGuiDesign() {
        // ========== 1. 窗口基础设置 ==========
        super("学生信息管理系统"); // 窗口标题
        setSize(800, 600); // 窗口大小
        setLocationRelativeTo(null); // 居中显示
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); // 自定义关闭逻辑
        // 窗口关闭事件：弹出确认对话框
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int result = JOptionPane.showConfirmDialog(
                        SwingGuiDesign.this,
                        "确定要退出系统吗？",
                        "退出确认",
                        JOptionPane.YES_NO_OPTION
                );
                if (result == JOptionPane.YES_OPTION) {
                    System.exit(0); // 退出程序
                }
            }
        });

        // ========== 2. 菜单Bar ==========
        JMenuBar menuBar = new JMenuBar();
        // 文件菜单
        JMenu fileMenu = new JMenu("文件(F)");
        JMenuItem exitItem = new JMenuItem("退出(X)");
        exitItem.addActionListener(e -> {
            // 触发窗口关闭事件
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        });
        fileMenu.add(exitItem);
        // 帮助菜单
        JMenu helpMenu = new JMenu("帮助(H)");
        JMenuItem aboutItem = new JMenuItem("关于(A)");
        aboutItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(
                    this,
                    "学生信息管理系统 v1.0\n基于Java Swing开发",
                    "关于",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar); // 设置菜单Bar

        // ========== 3. 主面板（边界布局） ==========
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // 内边距

        // ========== 4. 顶部表单面板（网格布局） ==========
        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("添加学生信息"));

        // 姓名行
        formPanel.add(new JLabel("姓名："));
        tfName = new JTextField();
        formPanel.add(tfName);

        // 年龄行
        formPanel.add(new JLabel("年龄："));
        tfAge = new JTextField();
        formPanel.add(tfAge);

        // 性别行
        formPanel.add(new JLabel("性别："));
        cbGender = new JComboBox<>(new String[]{"男", "女"});
        formPanel.add(cbGender);

        // 按钮行
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton btnAdd = new JButton("添加");
        JButton btnDel = new JButton("删除选中");
        JButton btnClear = new JButton("清空表单");

        // 添加按钮事件
        btnAdd.addActionListener(new AddStudentListener());
        // 删除按钮事件
        btnDel.addActionListener(e -> {
            int selectedRow = studentTable.getSelectedRow();
            if (selectedRow >= 0) {
                tableModel.removeRow(selectedRow);
                JOptionPane.showMessageDialog(this, "删除成功！");
            } else {
                JOptionPane.showMessageDialog(this, "请先选中要删除的行！", "提示", JOptionPane.WARNING_MESSAGE);
            }
        });
        // 清空按钮事件
        btnClear.addActionListener(e -> {
            tfName.setText("");
            tfAge.setText("");
            cbGender.setSelectedIndex(0);
        });

        btnPanel.add(btnAdd);
        btnPanel.add(btnDel);
        btnPanel.add(btnClear);
        formPanel.add(btnPanel);

        // ========== 5. 中部表格面板 ==========
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("学生列表"));
        // 表格列名
        String[] columnNames = {"序号", "姓名", "年龄", "性别"};
        // 表格数据模型（可动态修改）
        tableModel = new DefaultTableModel(columnNames, 0);
        studentTable = new JTable(tableModel);
        // 设置表格列宽
        studentTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        studentTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        studentTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        studentTable.getColumnModel().getColumn(3).setPreferredWidth(50);
        // 表格添加滚动条
        JScrollPane scrollPane = new JScrollPane(studentTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        // ========== 6. 组装所有面板 ==========
        mainPanel.add(formPanel, BorderLayout.NORTH);
        mainPanel.add(tablePanel, BorderLayout.CENTER);
        setContentPane(mainPanel);
    }

    // ========== 自定义事件监听器：添加学生 ==========
    class AddStudentListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // 1. 获取表单数据
            String name = tfName.getText().trim();
            String ageStr = tfAge.getText().trim();
            String gender = (String) cbGender.getSelectedItem();

            // 2. 数据校验
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(SwingGuiDesign.this, "姓名不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
                tfName.requestFocus(); // 聚焦到姓名输入框
                return;
            }
            int age;
            try {
                age = Integer.parseInt(ageStr);
                if (age < 0 || age > 150) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(SwingGuiDesign.this, "年龄必须是0-150的整数！", "错误", JOptionPane.ERROR_MESSAGE);
                tfAge.requestFocus();
                return;
            }

            // 3. 添加到表格
            int rowCount = tableModel.getRowCount() + 1; // 序号
            Object[] rowData = {rowCount, name, age, gender};
            tableModel.addRow(rowData);

            // 4. 清空表单
            tfName.setText("");
            tfAge.setText("");
            cbGender.setSelectedIndex(0);
            JOptionPane.showMessageDialog(SwingGuiDesign.this, "添加成功！");
        }
    }

    // ========== 主方法：程序入口 ==========
    public static void main(String[] args) {
        // Swing程序必须在事件调度线程（EDT）中运行
        SwingUtilities.invokeLater(() -> {
            SwingGuiDesign frame = new SwingGuiDesign();
            frame.setVisible(true); // 显示窗口
        });
    }
}