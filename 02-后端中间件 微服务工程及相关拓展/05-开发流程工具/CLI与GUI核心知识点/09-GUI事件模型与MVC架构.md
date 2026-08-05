# 09 - GUI 事件模型与 MVC 架构

> 🎯 GUI 编程的核心是事件驱动 — 理解 EDT 线程模型、事件分发机制和 MVC 模式，是写好桌面应用的基础

---

## 1. EDT（Event Dispatch Thread）

> ⚠️ Swing 所有 UI 操作必须在 EDT 线程执行！

```java
// ✅ 正确的启动方式
public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {    // ⭐ 在 EDT 中创建 UI
        new MyFrame().setVisible(true);
    });
}

// ❌ 错误 — 在主线程直接操作 UI
new MyFrame().setVisible(true);   // 可能导致 UI 冻结

// ✅ 从其他线程更新 UI
SwingUtilities.invokeLater(() -> {
    label.setText("更新了");       // 切回 EDT
});

// ✅ 耗时操作 — 用 SwingWorker 避免阻塞 EDT
new SwingWorker<Void, Integer>() {
    @Override
    protected Void doInBackground() {
        for (int i = 0; i <= 100; i++) {
            Thread.sleep(50);
            publish(i);          // 通知进度
        }
        return null;
    }

    @Override
    protected void process(List<Integer> chunks) {
        progressBar.setValue(chunks.get(chunks.size() - 1));  // EDT 中更新 UI
    }
}.execute();
```

| 线程 | 职责 | 不允许 |
|------|------|--------|
| **Main** | 程序入口 | ❌ 操作 UI 组件 |
| **EDT** | UI 事件处理 | ❌ 耗时操作（>100ms） |
| **Worker** | 后台任务 | ❌ 直接更新 UI（用 SwingWorker） |

---

## 2. 事件模型

```java
// ═══ 事件监听（观察者模式） ═══
button.addActionListener(e -> {
    System.out.println("按钮被点击");
});

textField.getDocument().addDocumentListener(new DocumentListener() {
    @Override public void insertUpdate(DocumentEvent e)  { onTextChanged(); }
    @Override public void removeUpdate(DocumentEvent e)  { onTextChanged(); }
    @Override public void changedUpdate(DocumentEvent e) {}
});

// ═══ 鼠标事件 ═══
component.addMouseListener(new MouseAdapter() {
    @Override public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) { /* 双击 */ }
    }
});

// ═══ 键盘事件 ═══
textField.addKeyListener(new KeyAdapter() {
    @Override public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) { /* 回车 */ }
    }
});
```

### 事件分发链

```text
事件产生 → EventQueue → EDT 轮询 → 分发给目标组件
   ↓
组件查找监听器 → 依次回调

JavaFX 的事件传播（更丰富）：
  捕获阶段（父→子）→ 目标阶段 → 冒泡阶段（子→父）
  → 类似 DOM 事件模型
```

---

## 3. MVC 模式

```java
// ═══ Model — 数据 ═══
public class UserModel {
    private List<User> users = new ArrayList<>();
    private List<UserChangeListener> listeners = new ArrayList<>();

    public void addUser(User user) {
        users.add(user);
        listeners.forEach(l -> l.onUserAdded(user));
    }

    public void addListener(UserChangeListener l) { listeners.add(l); }
}

// ═══ View — 纯 UI ═══
public class UserView extends JPanel {
    JTextField nameField = new JTextField(20);
    JButton addBtn = new JButton("添加");
    JList<String> userList = new JList<>();

    // View 不包含业务逻辑
}

// ═══ Controller — 连接 Model 和 View ═══
public class UserController {
    private final UserModel model;
    private final UserView view;

    public UserController(UserModel model, UserView view) {
        this.model = model;
        this.view = view;

        view.addBtn.addActionListener(e -> {
            String name = view.nameField.getText();
            model.addUser(new User(name));   // → 操作 Model
        });

        model.addListener(user -> {
            view.userList.addItem(user.getName());  // → 更新 View
        });
    }
}
```

```text
MVC 数据流：
  View(点击按钮) → Controller(调用) → Model(更新数据)
  Model(通知) → Controller(更新) → View(刷新UI)

对比 Spring MVC：
  View = 前端页面
  Controller = @Controller
  Model = Service + Repository
```

---

## 4. MVP vs MVC vs MVVM

| 模式 | View 知道 Model | 核心 | 框架 |
|------|:---:|------|------|
| **MVC** | ✅（观察者） | Controller 协调 | Swing |
| **MVP** | ❌ | Presenter 中介 | — |
| **MVVM** | ❌ | 数据绑定 | JavaFX Property + FXML |

> 🎯 **GUI 编程铁律**：UI 操作走 EDT、耗时操作走 Worker、更新 UI 用 invokeLater、业务逻辑和 UI 分离走 MVC。这三条做到，桌面应用也有好架构。
