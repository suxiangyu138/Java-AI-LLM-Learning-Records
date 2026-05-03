import javax.tools.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Java 动态编译示例（运行时编译 .java 文件）
 */
public class DynamicCompileDemo {
    public static void main(String[] args) {
        // 1. 获取系统 Java 编译器
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.err.println("❌ 未找到 Java 编译器（需使用 JDK，而非 JRE）");
            return;
        }

        // 2. 创建诊断器（收集编译错误）
        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();

        // 3. 定义待编译的源文件（假设项目根目录有 Hello.java）
        File sourceFile = new File("Hello.java");
        // 写入测试代码到 Hello.java
        writeTestJavaFile(sourceFile);

        // 4. 获取文件管理器
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticCollector, null, StandardCharsets.UTF_8);
        // 5. 定位待编译的源文件
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(sourceFile);

        // 6. 编译参数（指定输出目录为 out）
        Iterable<String> options = Arrays.asList("-d", "out");

        // 7. 执行编译
        JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnosticCollector, options, null, compilationUnits
        );
        boolean success = task.call();

        // 8. 处理编译结果
        if (success) {
            System.out.println("✅ 编译成功！.class 文件输出到 out 目录");
        } else {
            System.err.println("❌ 编译失败：");
            // 打印编译错误
            diagnosticCollector.getDiagnostics().forEach(diagnostic -> {
                System.err.printf("行 %d：%s%n", diagnostic.getLineNumber(), diagnostic.getMessage(null));
            });
        }

        // 9. 关闭文件管理器
        try {
            fileManager.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 写入测试 Java 代码到文件
     */
    private static void writeTestJavaFile(File file) {
        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
            String code = """
                    public class Hello {
                        public static void sayHello() {
                            System.out.println("👋 动态编译的 Hello 类！");
                        }
                    }
                    """;
            writer.write(code);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}