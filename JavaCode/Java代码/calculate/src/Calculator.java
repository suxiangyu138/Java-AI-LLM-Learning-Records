import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

// 计算器主类，继承JFrame实现窗口，实现ActionListener处理按钮点击
public class Calculator extends JFrame implements ActionListener {
    // 组件声明
    private JTextField displayField; // 显示输入和结果的文本框
    private StringBuilder input;     // 存储用户输入的表达式

    // 构造方法：初始化计算器界面
    public Calculator() {
        // 初始化输入缓冲区
        input = new StringBuilder();

        // 1. 设置窗口基本属性
        setTitle("简易计算器");       // 窗口标题
        setSize(400, 500);            // 窗口大小（宽，高）
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 关闭窗口时退出程序
        setLocationRelativeTo(null);  // 窗口居中显示
        setLayout(new BorderLayout());// 窗口布局：边界布局

        // 2. 创建显示文本框
        displayField = new JTextField();
        displayField.setHorizontalAlignment(JTextField.RIGHT); // 文字右对齐
        displayField.setFont(new Font("Arial", Font.PLAIN, 24)); // 字体大小
        displayField.setEditable(false); // 禁止手动编辑
        add(displayField, BorderLayout.NORTH); // 添加到窗口北部（顶部）

        // 3. 创建按钮面板（网格布局：5行4列）
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(5, 4, 5, 5)); // 5行4列，间距5px

        // 4. 定义按钮文本数组（按计算器布局排列）
        String[] buttons = {
            "C", "←", "", "/",
            "7", "8", "9", "*",
            "4", "5", "6", "-",
            "1", "2", "3", "+",
            "0", ".", "=", ""
        };

        // 5. 循环创建按钮并添加到面板
        for (String text : buttons) {
            JButton button = new JButton(text);
            button.setFont(new Font("Arial", Font.PLAIN, 20)); // 按钮字体
            button.addActionListener(this); // 绑定点击事件
            // 隐藏空按钮（优化界面）
            if (text.isEmpty()) {
                button.setEnabled(false);
                button.setVisible(false);
            }
            buttonPanel.add(button);
        }

        // 6. 将按钮面板添加到窗口中部
        add(buttonPanel, BorderLayout.CENTER);
    }

    // 步骤 2：处理按钮点击事件（核心逻辑）
    @Override
    public void actionPerformed(ActionEvent e) {
        // 获取点击的按钮文本
        String command = e.getActionCommand();

        // 分支处理不同按钮
        switch (command) {
            case "C": // 清空
                input.setLength(0); // 清空输入缓冲区
                displayField.setText(""); // 清空显示框
                break;
            case "←": // 回退（删除最后一个字符）
                if (input.length() > 0) {
                    input.deleteCharAt(input.length() - 1);
                    displayField.setText(input.toString());
                }
                break;
            case "=": // 计算结果
                try {
                    // 使用Java内置的脚本引擎计算表达式
                    javax.script.ScriptEngine engine = new javax.script.ScriptEngineManager()
                            .getEngineByName("JavaScript");
                    // 执行表达式并获取结果
                    Object result = engine.eval(input.toString());
                    // 显示结果，同时清空输入缓冲区（准备下一次计算）
                    displayField.setText(result.toString());
                    input.setLength(0);
                } catch (Exception ex) {
                    // 处理计算异常（如表达式错误）
                    displayField.setText("错误");
                    input.setLength(0);
                }
                break;
            default: // 数字、运算符、小数点
                input.append(command); // 将按钮文本追加到输入缓冲区
                displayField.setText(input.toString()); // 更新显示框
                break;
        }
    }

    // 步骤 3：主方法（程序入口）
    public static void main(String[] args) {
        // Swing组件需要在事件调度线程中运行
        SwingUtilities.invokeLater(() -> {
            Calculator calculator = new Calculator();
            calculator.setVisible(true); // 显示计算器窗口
        });
    }
}