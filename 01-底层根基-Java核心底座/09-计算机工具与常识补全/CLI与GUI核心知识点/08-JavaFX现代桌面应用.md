# 08 - JavaFX 现代桌面应用

> 🎯 JavaFX 是 Java 桌面应用的现代方案 — FXML 声明式 UI + CSS 样式 + SceneBuilder 可视化设计 + Property 数据绑定

---

## 1. SceneBuilder 可视化设计

```text
SceneBuilder = JavaFX 的可视化布局工具

下载：https://gluonhq.com/products/scene-builder/
用法：拖拽组件 → 调整属性 → 生成 FXML → IDEA 右键 Open In SceneBuilder

FXML = JavaFX 的 XML 布局文件（类似 Android layout.xml）
```

---

## 2. FXML + Controller

```xml
<!-- user-view.fxml -->
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<VBox spacing="10" xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.example.UserController">
    <HBox spacing="5">
        <TextField fx:id="nameField" promptText="姓名"/>
        <TextField fx:id="emailField" promptText="邮箱"/>
        <Button text="添加" onAction="#addUser"/>
    </HBox>
    <TableView fx:id="userTable">
        <columns>
            <TableColumn text="ID" fx:id="idCol"/>
            <TableColumn text="姓名" fx:id="nameCol"/>
            <TableColumn text="邮箱" fx:id="emailCol"/>
        </columns>
    </TableView>
</VBox>
```

```java
public class UserController {
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TableView<User> userTable;

    @FXML
    public void addUser() {
        userTable.getItems().add(new User(
            nameField.getText(), emailField.getText()
        ));
        nameField.clear();
        emailField.clear();
    }
}
```

```java
// 启动
public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/user-view.fxml"));
        stage.setScene(new Scene(root, 600, 400));
        stage.setTitle("用户管理");
        stage.show();
    }
}
```

---

## 3. CSS 样式

```css
/* style.css */
.root { -fx-padding: 10; }
.button {
    -fx-background-color: #4CAF50;
    -fx-text-fill: white;
    -fx-font-size: 14px;
}
.text-field { -fx-pref-width: 200; }
.table-view { -fx-pref-height: 300; }
```

```java
scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
```

---

## 4. Property 数据绑定（⭐ JavaFX 核心特色）

```java
// Property — 可观察的值（类似 Vue 的 reactive）
StringProperty name = new SimpleStringProperty("张三");
Label label = new Label();
label.textProperty().bind(name);     // 绑定！name 变了 label 自动更新

name.set("李四");                     // → label 自动显示 "李四"

// Bean 绑定
public class User {
    private StringProperty name = new SimpleStringProperty();
    public StringProperty nameProperty() { return name; }
    public String getName() { return name.get(); }
    public void setName(String value) { name.set(value); }
}
```

```text
JavaFX Property ↔ 前端对标：
  SimpleStringProperty  ←→  Vue ref('')
  SimpleIntegerProperty ←→  Vue ref(0)
  bind()                ←→  Vue computed()
  addListener()         ←→  Vue watch()
```

---

## 5. 图表（Chart）

```java
// 饼图
PieChart chart = new PieChart();
chart.getData().addAll(
    new PieChart.Data("成功", 75),
    new PieChart.Data("失败", 25)
);

// 折线图
LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
XYChart.Series<Number, Number> series = new XYChart.Series<>();
series.getData().add(new XYChart.Data<>(1, 23));
lineChart.getData().add(series);
```

### JavaFX 打包

```bash
# jpackage — JDK 14+ 内置打包工具
jpackage --name MyApp --input target/ --main-jar app.jar --main-class com.example.App
# → 生成 .exe (Windows) / .dmg (macOS) / .deb (Linux)
```

> 🎯 **JavaFX vs Swing**：新项目用 JavaFX（FXML+CSS+Property 绑定比 Swing 的纯 Java 代码高效很多）。SceneBuilder 是拖拽设计 UI 的利器。
