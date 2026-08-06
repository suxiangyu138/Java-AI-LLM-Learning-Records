# 10 - CLI 与 GUI 选型与工程化

> 🎯 什么时候用 CLI、什么时候用 GUI？如何打包分发、如何自动化测试？后端工程师需要掌握工具型应用的工程化

---

## 1. CLI vs GUI 选型决策

| 场景 | 推荐 | 理由 |
|------|:---:|------|
| CI/CD 脚本/自动化 | CLI | 脚本化、管道友好 |
| 开发工具/配置工具 | CLI | 远程 SSH 可用 |
| 面向普通用户 | GUI | 学习成本低 |
| 数据分析/报表 | GUI | 可视化图表 |
| 批量处理文件 | CLI | find + xargs 批量 |
| 监控 Dashboard | GUI | 实时可视化 |
| 运维自动化 | CLI + Web UI | CLI 脚本 + Web 查看 |

---

## 2. 打包与分发

### CLI 打包

```bash
# JAR 分发
mvn clean package
java -jar app.jar command --option

# GraalVM Native Image（推荐 — 毫秒启动）
native-image -jar app.jar app
./app command --option

# Docker 分发
FROM eclipse-temurin:21-jre-alpine
COPY target/app.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### GUI 打包

```bash
# jpackage（JDK 14+）— 打包为原生安装包
jpackage --name MyApp \
  --input target/ \
  --main-jar app.jar \
  --main-class com.example.App \
  --type msi          # Windows: msi/exe, macOS: dmg/pkg, Linux: deb/rpm

# jlink — 自包含 JRE
jlink --add-modules java.base,java.desktop --output runtime
```

| 打包方式 | 产物 | 体积 | 启动 |
|----------|------|:---:|:---:|
| Fat JAR | `.jar` | 30-100MB | 慢（JVM 启动） |
| jlink + jpackage | `.exe/.dmg/.deb` | 50-150MB | 中 |
| GraalVM Native | 二进制 | 10-30MB | ⭐ 极快（5ms） |

---

## 3. CLI 自动化测试

```java
// Junit + System Rules 测试 CLI 输出
@Test
void testGreetCommand() {
    String output = tapSystemOut(() -> {
        new CommandLine(new GreetCommand()).execute("张三", "-c", "2");
    });
    assertThat(output).contains("你好, 张三!");
    assertThat(output).hasLineCount(2);
}
```

### GUI 自动化测试

```java
// AssertJ Swing — Swing UI 自动化测试
@Test
void testAddUser() {
    UserManager frame = GuiActionRunner.execute(() -> new UserManager());

    frame.nameField().enterText("张三");
    frame.emailField().enterText("zhangsan@example.com");
    frame.addButton().click();

    frame.table().requireRowCount(1);
    frame.table().requireCellValue(0, 1, "张三");
}
```

```java
// TestFX — JavaFX 自动化测试
@Test
void testClickButton() {
    clickOn("#addBtn");
    verifyThat("#userTable", hasItems(1));
}
```

---

## 4. 跨平台注意事项

| 问题 | CLI | GUI |
|------|-----|-----|
| 路径分隔符 | `File.separator` / `Paths.get()` | 同 CLI |
| 换行符 | `System.lineSeparator()` | 同 |
| 外观 | — | ⚠️ LookAndFeel 差异大 |
| 字体 | — | ⚠️ 跨平台字体不一致 |
| DPI 缩放 | — | ⚠️ Windows 高 DPI 需适配 |
| 打包 | JAR 跨平台 | 每个平台单独打包 |

```java
// 跨平台适配
if (SystemUtils.IS_OS_WINDOWS) {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
}
```

> 🎯 **工程化三件套**：CLI 用 Native Image 毫秒启动、GUI 用 jpackage 原生安装包、Swing 测试用 AssertJ Swing。工具型应用选 CLI 优先，面向普通用户选 GUI。
