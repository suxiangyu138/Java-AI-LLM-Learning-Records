# 06 - GUI 概述与 Java 桌面生态

> 🎯 Java 桌面应用生态从 AWT→Swing→JavaFX 演进 20 年 — 虽非主流但理解它能帮助后端工程师读懂 UI 相关代码和概念

---

## 1. Java GUI 技术演进

```text
Java GUI 发展史：

AWT (Java 1.0, 1995)  → 重量级组件（依赖 OS 原生控件）
  ↓ 平台不一致 + 功能少
Swing (Java 1.2, 1998) → 轻量级组件（自己绘制，跨平台一致）
  ↓ 外观老旧 + 性能一般
JavaFX (Java 8, 2014)   → 现代化框架（CSS/FXML/3D/动画）
  ↓ JDK 11 后从 JDK 分离，独立发展
SWT (Eclipse, 2001)     → Eclipse 基金会（原生组件 + 高性能）

当前状态：
  → Swing: 遗留企业应用还在用（IDE 插件/配置工具）
  → JavaFX: 新桌面项目首选
  → SWT: Eclipse IDE 及插件生态
```

| 技术 | 组件类型 | 外观 | 布局 | 现状 |
|------|:---:|:---:|------|------|
| **AWT** | 重量级 | OS 原生 | 基础 | ❌ 废弃 |
| **Swing** | 轻量级 | 可定制 | 丰富 | ⚠️ 维护 |
| **JavaFX** | 现代 | CSS + FXML | 强大 | ⭐ 推荐 |
| **SWT** | 混合 | OS 原生 | 丰富 | Eclipse 生态 |

---

## 2. 后端需要理解 GUI 的场景

| 场景 | 示例 |
|------|------|
| IDE 插件开发 | IntelliJ IDEA Plugin（Swing） |
| 配置管理工具 | 数据库连接配置 GUI |
| 监控面板 | 自建监控 Dashboard |
| 内部工具 | 数据导入导出工具 |
| 自动化测试 | GUI 自动化测试脚本 |

---

## 3. 第一个 Swing 窗口

```java
import javax.swing.*;

public class HelloSwing {
    public static void main(String[] args) {
        JFrame frame = new JFrame("第一个窗口");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JButton button = new JButton("点击");
        button.addActionListener(e -> {
            JOptionPane.showMessageDialog(frame, "Hello Swing!");
        });

        frame.add(button);
        frame.setVisible(true);
    }
}
```

---

## 4. JavaFX 基本结构

```java
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class HelloFX extends Application {
    @Override
    public void start(Stage stage) {
        Button btn = new Button("点击");
        btn.setOnAction(e -> System.out.println("Hello JavaFX!"));

        StackPane root = new StackPane(btn);
        stage.setScene(new Scene(root, 400, 300));
        stage.setTitle("JavaFX 窗口");
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}
```

| Swing | JavaFX |
|-------|--------|
| `JFrame` | `Stage` + `Scene` |
| `JButton` | `Button` |
| `addActionListener` | `setOnAction` |
| 布局管理器 | `StackPane`/`HBox`/`VBox` |
| 外观配置 | CSS |

> 🎯 **后端学 GUI 的价值**：理解事件驱动模型、看懂 IDE 插件代码、写内部工具提升效率。Java GUI 非主流但有用，Swing 老而弥坚，JavaFX 是未来。
