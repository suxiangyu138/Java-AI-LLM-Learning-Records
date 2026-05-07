import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 高级质感版Java电子钟
 * 特性：渐变背景、霓虹字体、圆角边框、阴影效果、日期+星期显示
 */
public class DigitalClock extends JFrame {
    // 时间/日期格式化器
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd EEEE");
    // 显示组件
    private final JLabel timeLabel;
    private final JLabel dateLabel;
    // 定时器
    private final Timer timer;

    public DigitalClock() {
        super("高级电子钟");
        // ========== 1. 窗口基础设置 ==========
        setSize(500, 280);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true); // 隐藏原生窗口边框（自定义样式）
        setShape(new RoundRectangle2D.Double(0, 0, 500, 280, 20, 20)); // 窗口圆角

        // ========== 2. 自定义面板（渐变背景+阴影） ==========
        JPanel mainPanel = new JPanel() {
            // 重写paintComponent实现渐变背景
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                // 开启抗锯齿（文字/图形更平滑）
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 创建线性渐变：从左上(0,0)到右下(500,280)，深蓝→深紫
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(30, 40, 60),
                        500, 280, new Color(50, 20, 80)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setLayout(new BorderLayout(0, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30)); // 内边距

        // ========== 3. 时间标签（霓虹光效） ==========
        timeLabel = new JLabel();
        // 自定义霓虹字体：大号、加粗、抗锯齿
        Font timeFont = new Font("微软雅黑", Font.BOLD, 80);
        timeLabel.setFont(timeFont);
        timeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        // 霓虹效果：白色文字+浅蓝外发光
        timeLabel.setForeground(Color.WHITE);
        timeLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        // 开启文字抗锯齿
        timeLabel.setForeground(new Color(255, 255, 255)); // 纯白文字
        timeLabel.setOpaque(false);

        // ========== 4. 日期标签（浅色小字） ==========
        dateLabel = new JLabel();
        Font dateFont = new Font("微软雅黑", Font.PLAIN, 22);
        dateLabel.setFont(dateFont);
        dateLabel.setHorizontalAlignment(SwingConstants.CENTER);
        dateLabel.setForeground(new Color(200, 220, 255)); // 浅蓝灰色
        dateLabel.setOpaque(false);

        // ========== 5. 组装组件 ==========
        mainPanel.add(timeLabel, BorderLayout.CENTER);
        mainPanel.add(dateLabel, BorderLayout.SOUTH);
        setContentPane(mainPanel);

        // ========== 6. 定时器（每秒刷新） ==========
        timer = new Timer(1000, e -> updateTimeAndDate());
        // 初始化显示
        updateTimeAndDate();
    }

    /**
     * 更新时间和日期
     */
    private void updateTimeAndDate() {
        Date now = new Date();
        // 更新时间（添加霓虹光效的视觉优化）
        String timeStr = timeFormat.format(now);
        timeLabel.setText(timeStr);
        // 更新日期和星期
        String dateStr = dateFormat.format(now);
        dateLabel.setText(dateStr);
    }

    /**
     * 启动电子钟
     */
    public void start() {
        timer.start();
    }

    // ========== 主方法 ==========
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DigitalClock clock = new DigitalClock();
            // 添加窗口阴影（模拟系统阴影效果）
            if (System.getProperty("os.name").contains("Windows")) {
                try {
                    // Windows系统启用窗口阴影
                    Class<?> awtUtilClass = Class.forName("com.sun.awt.AWTUtilities");
                    awtUtilClass.getMethod("setWindowOpacity", Window.class, float.class)
                            .invoke(null, clock, 0.98f); // 轻微透明
                } catch (Exception e) {
                    // 兼容不同JDK版本，忽略异常
                }
            }
            clock.setVisible(true);
            clock.start();
        });
    }

    // ========== 重写paint方法，添加外发光效果 ==========
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;
        // 给时间标签添加外发光（霓虹效果）
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(100, 180, 255, 80)); // 浅蓝半透明
        g2d.fillRoundRect(10, 10, getWidth()-20, getHeight()-20, 15, 15);
    }
}