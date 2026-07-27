# 07 - Swing 核心组件实战

> 🎯 Swing 是 Java 桌面应用的历史基石 — JFrame/布局管理/事件处理/表格/对话框，后端偶尔写内部工具时用得上

---

## 1. 核心组件速查

| 组件 | 作用 | 常用方法 |
|------|------|----------|
| `JFrame` | 主窗口 | `setSize/setTitle/setDefaultCloseOperation` |
| `JPanel` | 面板容器 | `setLayout/add/setBorder` |
| `JButton` | 按钮 | `addActionListener/setEnabled` |
| `JLabel` | 文本标签 | `setText/setIcon` |
| `JTextField` | 单行输入 | `setText/getText/setColumns` |
| `JTextArea` | 多行文本 | `append/setLineWrap` |
| `JComboBox` | 下拉选择 | `addItem/getSelectedItem` |
| `JTable` | 表格 | `setModel/getSelectedRow` |
| `JScrollPane` | 滚动面板 | `setViewportView` |

---

## 2. 布局管理器

```java
// BorderLayout — 五方位（默认）
frame.add(new JButton("北"), BorderLayout.NORTH);
frame.add(new JButton("中"), BorderLayout.CENTER);

// FlowLayout — 流式排列
panel.setLayout(new FlowLayout(FlowLayout.LEFT));
panel.add(new JButton("按钮1"));
panel.add(new JButton("按钮2"));

// GridLayout — 网格
panel.setLayout(new GridLayout(2, 3));  // 2行3列

// GridBagLayout — 最灵活（但最复杂）
panel.setLayout(new GridBagLayout());
GridBagConstraints c = new GridBagConstraints();
c.gridx = 0; c.gridy = 0; c.weightx = 1.0; c.fill = GridBagConstraints.HORIZONTAL;
panel.add(new JTextField(), c);

// GroupLayout — GUI Builder 使用
// 手写推荐 MigLayout 第三方库
```

---

## 3. 表格与数据展示

```java
// 简单表格
String[] columns = {"ID", "姓名", "邮箱"};
Object[][] data = {
    {1, "张三", "zhangsan@example.com"},
    {2, "李四", "lisi@example.com"}
};
JTable table = new JTable(data, columns);
JScrollPane scrollPane = new JScrollPane(table);   // ⚠️ 必须包在 JScrollPane 中
frame.add(scrollPane);

// TableModel — 更灵活
DefaultTableModel model = new DefaultTableModel(columns, 0);
model.addRow(new Object[]{1, "张三", "zhangsan@example.com"});
table.setModel(model);
```

---

## 4. 对话框

```java
// 消息框
JOptionPane.showMessageDialog(frame, "操作成功", "提示", JOptionPane.INFORMATION_MESSAGE);

// 确认框
int result = JOptionPane.showConfirmDialog(frame, "确认删除？", "确认", JOptionPane.YES_NO_OPTION);
if (result == JOptionPane.YES_OPTION) { /* 删除 */ }

// 输入框
String name = JOptionPane.showInputDialog(frame, "请输入姓名:");

// 文件选择器
JFileChooser chooser = new JFileChooser();
if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
    File file = chooser.getSelectedFile();
}
```

---

## 5. 菜单栏

```java
JMenuBar menuBar = new JMenuBar();

JMenu fileMenu = new JMenu("文件");
fileMenu.add(new JMenuItem("新建"));
fileMenu.add(new JMenuItem("打开"));
fileMenu.addSeparator();
fileMenu.add(new JMenuItem("退出")).addActionListener(e -> System.exit(0));

menuBar.add(fileMenu);
frame.setJMenuBar(menuBar);
```

---

## 6. 简单 CRUD 示例

```java
public class UserManager extends JFrame {
    private JTextField nameField = new JTextField(20);
    private JTextField emailField = new JTextField(20);
    private DefaultTableModel tableModel;

    public UserManager() {
        setTitle("用户管理");
        setLayout(new BorderLayout());

        // 顶部输入区
        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("姓名:"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("邮箱:"));
        inputPanel.add(emailField);
        JButton addBtn = new JButton("添加");
        addBtn.addActionListener(e -> addUser());
        inputPanel.add(addBtn);

        // 中央表格
        tableModel = new DefaultTableModel(new String[]{"ID", "姓名", "邮箱"}, 0);
        JTable table = new JTable(tableModel);

        add(inputPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        pack();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private void addUser() {
        tableModel.addRow(new Object[]{
            tableModel.getRowCount() + 1,
            nameField.getText(), emailField.getText()
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UserManager().setVisible(true));
    }
}
```

> 🎯 **Swing 三板斧**：`JFrame` 窗口 + `JPanel` 布局 + `addActionListener` 事件。内部工具 CRUD 用 `JTable` + `DefaultTableModel` 最简组合。
