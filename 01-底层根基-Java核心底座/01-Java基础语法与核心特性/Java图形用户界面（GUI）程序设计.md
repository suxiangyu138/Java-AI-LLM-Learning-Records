# Java图形用户界面（GUI）程序设计
Java GUI（Graphical User Interface，图形用户界面）是可视化人机交互程序，依靠窗口、按钮、输入框等组件替代命令行交互。Java 提供两套图形工具：**AWT**、**Swing**，Swing 为主流轻量级方案。
全文围绕「容器组件 + 布局管理器 + 事件驱动」三大核心，配套完整可运行案例，适配考试、面试、上机实操。

## 一、GUI 两大工具包：AWT 与 Swing
### 1. AWT（抽象窗口工具包，JDK1.0）
- 实现原理：**重量级组件**，直接调用操作系统原生图形控件
- 缺陷：跨平台不一致、组件少、可定制差
- 组件特征：无 `J` 前缀 `Frame/Button/Label`
- 适用：老旧兼容项目，新项目不推荐

### 2. Swing（JDK1.2，AWT 上层封装）
- 实现原理：**轻量级组件**，纯Java绘制，不依赖系统原生控件
- 优势：全平台样式统一、组件丰富、高度自定义、扩展性强
- 组件特征：全部以 `J` 开头 `JFrame/JButton/JLabel/JPanel`
- 定位：Java GUI 标准开发方案，下文全部基于 Swing

### AWT & Swing 核心对比
| 对比项 | AWT | Swing |
|--------|-----|-------|
| 组件类型 | 重量级 | 轻量级 |
| 跨平台 | 差，系统样式不同 | 统一渲染，跨平台一致 |
| 组件丰富度 | 基础少量组件 | 完整组件库（表格、树、弹窗） |
| 自定义样式 | 几乎不支持 | 支持字体、颜色、外观皮肤 |
| 命名规则 | 无J前缀 | 全部J开头 |

## 二、Swing 三层组件体系
界面遵循层级嵌套：**顶层容器 → 中间容器 → 基础交互组件**，所有控件必须挂载顶层容器才能渲染。

### 1. 顶层容器（程序窗口载体）
唯一可独立显示的窗口，三种常用类型：
1. **JFrame**：主程序窗口（最常用），支持最大化、最小化、关闭
2. **JDialog**：弹窗对话框，依附JFrame，无法单独运行
3. **JApplet**：浏览器小程序，现已淘汰

#### JFrame 标准模板（必背）
```java
import javax.swing.JFrame;

public class FrameDemo {
    public static void main(String[] args) {
        // 1. 创建窗口，传入标题
        JFrame frame = new JFrame("Swing基础窗口");
        // 2. 设置窗口宽高
        frame.setSize(400, 300);
        // 3. 窗口屏幕居中
        frame.setLocationRelativeTo(null);
        // 4. 关闭窗口行为（核心）
        // EXIT_ON_CLOSE：关闭直接结束程序（开发首选）
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // 5. 窗口可见，不调用则完全不显示
        frame.setVisible(true);
    }
}
```
关闭常量补充：
- `DISPOSE_ON_CLOSE`：销毁窗口，后台程序继续运行
- `DO_NOTHING_ON_CLOSE`：点击关闭无任何响应

### 2. 中间容器（布局载体，不可单独显示）
用于分组收纳基础组件，嵌套进JFrame使用：
1. **JPanel**：通用面板，默认流式布局，用于界面分块
2. **JScrollPane**：带滚动条面板，承载超长文本/表格
3. **JTabbedPane**：多选项卡面板，切换不同页面

#### JPanel 基础示例
```java
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Color;

public class PanelDemo {
    public static void main(String[] args) {
        JFrame frame = new JFrame("JPanel面板");
        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setBackground(Color.LIGHT_GRAY);
        // 将面板添加到窗口
        frame.add(panel);

        frame.setVisible(true);
    }
}
```

### 3. 基础交互组件（用户操作控件）
| 组件 | 作用 | 核心API |
|------|------|---------|
| `JLabel` | 静态文字/图片展示，不可编辑 | `setText()`、`setHorizontalAlignment()` |
| `JTextField` | 单行文本输入框 | `getText()`、`setEditable()` |
| `JPasswordField` | 密码输入框，隐藏明文 | `getPassword()` 返回char[]（安全） |
| `JButton` | 功能按钮，绑定点击事件 | `addActionListener()` |
| `JCheckBox` | 多选框 | `isSelected()` |
| `JRadioButton` | 单选框（配合ButtonGroup互斥） | `isSelected()` |
| `JTextArea` | 多行大文本输入 | `setLineWrap(true)`自动换行 |
| `JComboBox` | 下拉选择框 | `getSelectedItem()` |

#### 关键组件代码片段
```java
// 标签
JLabel label = new JLabel("用户名：");
label.setHorizontalAlignment(JLabel.CENTER);

// 单行输入框
JTextField input = new JTextField(20);
input.setText("默认内容");
String content = input.getText();

// 密码框
JPasswordField pwdInput = new JPasswordField(20);
char[] pwdArr = pwdInput.getPassword();
String pwd = new String(pwdArr);

// 按钮绑定点击事件
JButton btn = new JButton("登录");
btn.addActionListener(e -> System.out.println("按钮触发"));
```

## 三、四大布局管理器（自动排版核心）
手动设置坐标尺寸兼容性极差，开发统一使用布局管理器自动排布组件；复杂界面采用**多层面板嵌套不同布局**组合实现。

### 1. FlowLayout 流式布局（JPanel 默认）
规则：组件从左至右横向排列，一行填满自动换行，默认居中对齐。
```java
import javax.swing.*;
import java.awt.FlowLayout;

public class FlowLayoutDemo {
    public static void main(String[] args) {
        JFrame frame = new JFrame("流式布局");
        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 构造：对齐方式、水平间距、垂直间距
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.add(new JButton("按钮1"));
        panel.add(new JButton("按钮2"));
        panel.add(new JButton("按钮3"));
        panel.add(new JButton("按钮4"));
        panel.add(new JButton("按钮5"));

        frame.add(panel);
        frame.setVisible(true);
    }
}
```

### 2. BorderLayout 边界布局（JFrame 默认）
规则：容器划分为 5 个区域 `NORTH/SOUTH/WEST/EAST/CENTER`，每个区域仅放1个组件；CENTER区域自动填充剩余空间。
```java
import javax.swing.JButton;
import javax.swing.JFrame;
import java.awt.BorderLayout;

public class BorderLayoutDemo {
    public static void main(String[] args) {
        JFrame frame = new JFrame("边界布局");
        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(5, 5));

        frame.add(new JButton("顶部NORTH"), BorderLayout.NORTH);
        frame.add(new JButton("底部SOUTH"), BorderLayout.SOUTH);
        frame.add(new JButton("左侧WEST"), BorderLayout.WEST);
        frame.add(new JButton("右侧EAST"), BorderLayout.EAST);
        frame.add(new JButton("中间CENTER"), BorderLayout.CENTER);

        frame.setVisible(true);
    }
}
```

### 3. GridLayout 网格布局
规则：均分多行多列网格，所有组件尺寸完全一致，按行填充。适合表单登录界面。
```java
import javax.swing.*;
import java.awt.GridLayout;

public class GridLayoutDemo {
    public static void main(String[] args) {
        JFrame frame = new JFrame("网格布局-登录表单");
        frame.setSize(400, 200);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 2行2列，水平间距10，垂直间距20
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 20));
        panel.add(new JLabel("用户名"));
        panel.add(new JTextField());
        panel.add(new JLabel("密码"));
        panel.add(new JPasswordField());

        frame.add(panel);
        frame.setVisible(true);
    }
}
```

### 4. CardLayout 卡片布局
规则：多页面卡片堆叠，同一时间只显示一张卡片，用于分步向导、切换面板。

### 布局使用准则
1. 整体窗口外层使用 BorderLayout 划分上下主体
2. 表单输入区域使用 GridLayout
3. 按钮组、短控件行使用 FlowLayout
4. 禁止大量使用 `setSize()`、`setBounds()` 绝对定位

## 四、事件驱动机制（GUI交互核心）
GUI 基于**事件驱动模型**：用户操作控件产生事件，监听器捕获并执行业务逻辑。
### 四大核心概念
1. **事件源**：产生操作的控件（JButton、输入框、窗口）
2. **事件Event**：用户操作行为 `ActionEvent/MouseEvent/KeyEvent`
3. **监听器Listener**：处理事件的接口，定义回调方法
4. **注册监听**：`addXxxListener()` 将监听器绑定控件

### 1. ActionEvent 动作事件（最常用）
触发场景：按钮点击、输入框回车、菜单点击；监听器 `ActionListener`
JDK8+ Lambda 极简写法（开发首选）
```java
import javax.swing.*;
import java.awt.FlowLayout;

public class ActionEventDemo {
    public static void main(String[] args) {
        JFrame frame = new JFrame("点击事件");
        frame.setSize(400, 200);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 30));

        JLabel tip = new JLabel("等待点击");
        JButton btn = new JButton("点击我");
        JTextField input = new JTextField(20);

        // 按钮点击监听
        btn.addActionListener(e -> tip.setText("按钮已点击"));
        // 输入框回车监听
        input.addActionListener(e -> tip.setText("输入内容：" + input.getText()));

        panel.add(tip);
        panel.add(btn);
        panel.add(input);
        frame.add(panel);
        frame.setVisible(true);
    }
}
```

### 2. 其余常用事件
- `MouseEvent` 鼠标事件：移入、点击、拖拽，`MouseListener`
- `KeyEvent` 键盘事件：按键按下/松开，`KeyListener`
- `WindowEvent` 窗口事件：打开、最小化、关闭，`WindowListener`

### 监听器两种实现方式
1. Lambda（推荐）：单方法接口，代码极简
2. 实现接口：多事件回调场景，重写全部抽象方法

## 五、综合实战：完整登录窗口（组件+布局+事件）
需求：用户名密码输入、登录校验、重置清空
```java
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.FlowLayout;

public class LoginWindow {
    private JTextField userInput;
    private JPasswordField pwdInput;

    public void createWindow() {
        JFrame frame = new JFrame("用户登录");
        frame.setSize(400, 250);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));

        // 表单面板：网格布局
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 20));
        formPanel.add(new JLabel("用户名："));
        userInput = new JTextField(15);
        formPanel.add(userInput);
        formPanel.add(new JLabel("密  码："));
        pwdInput = new JPasswordField(15);
        formPanel.add(pwdInput);

        // 按钮面板：流式布局
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        JButton loginBtn = new JButton("登录");
        JButton resetBtn = new JButton("重置");
        btnPanel.add(loginBtn);
        btnPanel.add(resetBtn);

        frame.add(formPanel, BorderLayout.CENTER);
        frame.add(btnPanel, BorderLayout.SOUTH);

        // 登录逻辑
        loginBtn.addActionListener(e -> {
            String username = userInput.getText();
            String password = new String(pwdInput.getPassword());
            if ("admin".equals(username) && "123456".equals(password)) {
                JOptionPane.showMessageDialog(frame, "登录成功");
            } else {
                JOptionPane.showMessageDialog(frame, "账号或密码错误");
                userInput.setText("");
                pwdInput.setText("");
            }
        });

        // 重置逻辑
        resetBtn.addActionListener(e -> {
            userInput.setText("");
            pwdInput.setText("");
        });

        frame.setVisible(true);
    }

    public static void main(String[] args) {
        new LoginWindow().createWindow();
    }
}
```

## 六、开发避坑要点
1. 控件必须 `add()` 加入容器，否则完全不显示
2. JFrame 必须设置 `setDefaultCloseOperation`，否则关窗口程序后台常驻
3. 最后必须调用 `setVisible(true)` 渲染界面
4. Swing 非线程安全，子线程更新UI使用 `SwingUtilities.invokeLater()`
5. 密码读取优先 `getPassword()` 返回 char[]，避免明文字符串驻留内存
6. 多窗口、复杂页面采用「面板嵌套多布局」，杜绝绝对坐标
7. 中文乱码：统一工程编码UTF-8，字体设置中文字体

## 七、面试背诵高频考点
1. **AWT与Swing区别**
AWT是重量级，依赖系统原生控件，跨平台不一致；Swing轻量级，Java自行绘制，组件丰富、跨平台统一，组件带J前缀，优先使用Swing。

2. **Swing三层组件结构**
顶层容器（JFrame/JDialog）→中间容器（JPanel）→基础交互组件（按钮、输入框等），所有控件必须挂载顶层容器。

3. **三大主流布局特点**
- FlowLayout：流式横向排列，自动换行，按钮组使用
- BorderLayout：五分区域，中间自适应，整体页面外层布局
- GridLayout：均分网格，表单输入界面专用

4. **GUI事件驱动四要素**
事件源（控件）、事件（操作行为）、监听器（处理逻辑）、注册监听绑定。

5. **JTextField与JPasswordField区别**
单行输入框明文展示，`getText()`获取字符串；密码框隐藏字符，`getPassword()`返回字符数组更安全。

## 八、全文总结
1. Java GUI 主流开发方案为 Swing，摒弃老旧AWT；
2. 界面构建逻辑：窗口容器 + 面板分组 + 基础控件；
3. 布局管理器自动适配组件排版，多层嵌套实现复杂界面；
4. 交互依靠事件驱动，ActionListener按钮点击为最核心场景；
5. 综合登录案例覆盖全部基础知识点，是上机、考试标准实操代码；
6. 进阶拓展：`JOptionPane`弹窗、JTable表格、自定义绘制组件、外观美化LookAndFeel。