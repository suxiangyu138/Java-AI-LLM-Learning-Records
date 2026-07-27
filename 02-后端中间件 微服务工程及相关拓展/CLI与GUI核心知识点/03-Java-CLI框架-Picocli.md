# 03 - Java CLI 框架：Picocli 实战

> 🎯 Picocli 是 Java 命令行工具的首选框架 — 注解驱动、子命令支持、自动生成帮助文档、Gradle/Maven 插件一键打包

---

## 1. Picocli vs JCommander vs Spring Shell

| 框架 | 特点 | 适用 |
|------|------|------|
| **Picocli** | 注解驱动/体积小/彩色帮助/子命令/GraalVM | ⭐ 首选 |
| **JCommander** | 老牌/简单 | 简单场景 |
| **Spring Shell** | Spring 集成/REPL交互式 | 大型内部工具 |

---

## 2. Picocli 基础

```xml
<dependency>
    <groupId>info.picocli</groupId>
    <artifactId>picocli</artifactId>
    <version>4.7.5</version>
</dependency>
```

```java
@Command(name = "greet", description = "打招呼应用")
public class GreetCommand implements Runnable {

    @Parameters(index = "0", description = "名字")
    private String name;

    @Option(names = {"-c", "--count"}, defaultValue = "1", description = "次数")
    private int count;

    @Option(names = {"-l", "--lang"}, defaultValue = "zh", description = "语言: zh/en")
    private String lang;

    @Override
    public void run() {
        String greeting = "zh".equals(lang) ? "你好" : "Hello";
        for (int i = 0; i < count; i++) {
            System.out.println(greeting + ", " + name + "!");
        }
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new GreetCommand()).execute(args));
    }
}
```

```bash
java GreetCommand 张三 -c 3 -l zh
# 你好, 张三!
# 你好, 张三!
# 你好, 张三!

java GreetCommand --help   # 自动生成帮助
```

---

## 3. 子命令模式

```java
@Command(name = "db", subcommands = {DbBackup.class, DbRestore.class})
public class DbCommand { }

@Command(name = "backup", description = "备份数据库")
class DbBackup implements Runnable {
    @Option(names = "--host", required = true) String host;
    @Option(names = "--output") File output;

    @Override
    public void run() {
        System.out.println("备份 " + host + " 到 " + output);
    }
}
```

```bash
java App db backup --host localhost --output ./backup.sql
java App db restore --input ./backup.sql
```

---

## 4. 打包为可执行 JAR / Native

```xml
<!-- Maven Assembly Plugin — 打包 Fat JAR -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-assembly-plugin</artifactId>
    <configuration>
        <archive><manifest><mainClass>com.example.App</mainClass></manifest></archive>
        <descriptorRefs><descriptorRef>jar-with-dependencies</descriptorRef></descriptorRefs>
    </configuration>
</plugin>
```

```bash
# 打包为可执行 JAR
mvn clean package
java -jar app.jar greet 张三 -c 3

# GraalVM Native Image（毫秒启动）
native-image -jar app.jar app
./app greet 张三 -c 3     # 启动 5ms vs JVM 500ms
```

> 🎯 **Picocli 三件套**：`@Command` 声明程序/子命令、`@Option` 选项参数、`@Parameters` 位置参数。打包用 assembly plugin → 分发一个 JAR 即可。
