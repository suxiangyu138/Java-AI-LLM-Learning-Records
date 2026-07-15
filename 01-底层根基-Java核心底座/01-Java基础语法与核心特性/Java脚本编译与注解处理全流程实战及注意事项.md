# Java 脚本编译与注解处理全流程实战及注意事项

> **定位**：Java 脚本编译与注解处理是后端开发、框架底层实现、代码自动化生成场景中的核心技术，分别对应**动态代码执行**与**编译期代码增强/生成**两大核心能力。

---

## 目录

1. [Java 脚本编译](#1-java-脚本编译)
2. [脚本编译核心注意事项](#2-脚本编译核心注意事项)
3. [Java 注解处理](#3-java-注解处理)
4. [自定义注解处理器实操](#4-自定义注解处理器实操)
5. [注解处理核心注意事项](#5-注解处理核心注意事项)
6. [脚本编译 vs 注解处理对比](#6-脚本编译-vs-注解处理对比)

---

## 1. Java 脚本编译

### 1.1 基础定义

> 在 JVM 运行期间，动态加载、编译并执行 Java 源代码，无需提前打包成 class 文件。

| 版本 | 方案 |
|------|------|
| JDK 6+ | Java Compiler API（`javax.tools`） |
| JDK 8 | Nashorn 脚本引擎（已废弃） |
| JDK 11+ | GraalVM JavaScript |
| 其他 | Groovy、JRuby 等动态脚本语言 |

### 1.2 核心适用场景

| 场景 | 说明 |
|------|------|
| 动态规则引擎 | 无需重启服务，在线更新业务规则 |
| 低代码/无代码平台 | 动态生成并执行业务代码片段 |
| 测试与调试工具 | 运行时执行临时测试代码 |
| 插件化开发 | 动态加载插件脚本，模块化扩展 |
| 轻量级脚本替换 | 替代复杂 Java 类，简化简单逻辑 |

### 1.3 核心 API（JDK 原生，无需额外依赖）

| 类/接口 | 作用 |
|----------|------|
| `JavaCompiler` | JDK 原生编译器入口，获取系统编译器实例 |
| `StandardJavaFileManager` | 文件管理器，管理源码和编译输出的 class 文件 |
| `CompilationTask` | 编译任务，支持编译参数配置 |
| `DiagnosticCollector` | 诊断收集器，捕获编译错误信息 |
| `ScriptEngine` | 脚本引擎接口（JavaScript、Groovy 等） |

### 1.4 完整实战代码

```java
import javax.tools.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class JavaScriptCompileDemo {
    public static void main(String[] args) {
        // 1. 定义动态 Java 源码（字符串形式）
        String sourceCode = "public class DynamicScript {\n"
                + "    public static void execute() {\n"
                + "        System.out.println(\"动态编译执行成功！\");\n"
                + "    }\n"
                + "    public int add(int a, int b) {\n"
                + "        return a + b;\n"
                + "    }\n"
                + "}";

        // 2. 将源码写入临时 Java 文件
        File sourceFile = new File("DynamicScript.java");
        try (FileWriter writer = new FileWriter(sourceFile)) {
            writer.write(sourceCode);
        } catch (IOException e) {
            System.err.println("源码写入失败：" + e.getMessage());
            return;
        }

        // 3. 获取系统 Java 编译器
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.err.println("获取编译器失败，请使用 JDK 运行，而非 JRE！");
            return;
        }

        // 4. 诊断收集器 + 文件管理器
        DiagnosticCollector<Object> diagnosticCollector = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager =
                compiler.getStandardFileManager(diagnosticCollector, null, null)) {

            // 5. 创建编译任务
            Iterable<? extends JavaFileObject> fileObjects =
                    fileManager.getJavaFileObjects(sourceFile);
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null, fileManager, diagnosticCollector,
                    Arrays.asList("-d", "./"),  // 输出目录
                    null, fileObjects
            );

            // 6. 执行编译
            if (!task.call()) {
                System.err.println("编译失败：");
                for (Diagnostic<?> d : diagnosticCollector.getDiagnostics()) {
                    System.err.println(d.getMessage(null));
                }
                return;
            }
            System.out.println("源码编译成功！");

            // 7. 反射调用编译后的 class
            Class<?> dynamicClass = Class.forName("DynamicScript");

            // 静态方法
            Method staticMethod = dynamicClass.getDeclaredMethod("execute");
            staticMethod.invoke(null);

            // 实例方法
            Object instance = dynamicClass.newInstance();
            Method addMethod = dynamicClass.getDeclaredMethod("add", int.class, int.class);
            int result = (int) addMethod.invoke(instance, 10, 20);
            System.out.println("10 + 20 = " + result);

        } catch (Exception e) {
            System.err.println("编译或执行异常：" + e.getMessage());
        } finally {
            // 清理临时文件
            if (sourceFile.exists()) sourceFile.delete();
            new File("DynamicScript.class").delete();
        }
    }
}
```

### 1.5 运行前提

| 要求 | 说明 |
|------|------|
| **必须 JDK** | JRE 不含编译器，`getSystemJavaCompiler()` 返回 `null` |
| **目录权限** | 编译输出目录需读写权限 |
| **类名一致** | 动态生成的类名必须与源码字符串一致 |

---

## 2. 脚本编译核心注意事项

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| **JRE 编译器为 null** | JRE 无系统编译器 | 生产环境必须部署 JDK |
| **内存泄漏** | 动态加载的类无法被 GC | 自定义类加载器，用完及时卸载 |
| **代码注入** | 直接执行用户输入代码 | ✅ 白名单校验 + 权限隔离 + 代码沙箱 |
| **编译错误定位难** | 仅靠 `call()` 返回值判断 | 必须用 `DiagnosticCollector` 收集详细信息 |
| **临时文件堆积** | `.java` / `.class` 未清理 | `finally` 清理或使用临时目录 |
| **第三方依赖缺失** | 动态源码依赖外部 jar | 编译任务中 `-classpath` 指定依赖路径 |

---

## 3. Java 注解处理

### 3.1 基础定义

> 注解处理（Annotation Processing）是 **编译期**核心机制，在 javac 编译阶段扫描注解，自动生成辅助代码/配置文件或校验代码规范。

**核心生命周期**：

```text
javac 编译 → 扫描源码注解 → 匹配注解处理器 → 解析注解 → 生成代码 → 生成 class 文件
```

**常见框架底层实现**：

| 框架 | 注解 | 生成内容 |
|------|------|----------|
| Lombok | `@Data`、`@Getter` | getter/setter 等模板代码 |
| Spring Boot | `@SpringBootApplication` | 自动配置类 |
| MyBatis | `@Mapper` | Mapper 实现类 |
| ButterKnife | `@BindView` | View 绑定代码 |

> 核心优势：**编译期生成代码，无运行时性能损耗，无反射开销。**

### 3.2 核心 API 与组件

| API/组件 | 作用 | 关键点 |
|----------|------|--------|
| `@Retention` | 注解生命周期 | 编译期处理必须设为 `RetentionPolicy.SOURCE` |
| `@Target` | 注解作用目标 | `TYPE`/`METHOD`/`FIELD`/`PARAMETER` |
| `AbstractProcessor` | 处理器基类 | 自定义处理器必须继承 |
| `Processor` | 处理器接口 | `init()` → `process()` → `getSupportedAnnotationTypes()` |
| `Filer` | 文件生成工具 | 生成 Java 源码、配置文件 |
| `Elements` | 元素工具类 | 操作类、方法、字段等源码元素 |

---

## 4. 自定义注解处理器实操

### 4.1 第一步：定义编译期注解

```java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 编译期注解，用于自动生成 Builder 构造器
 */
@Target(ElementType.TYPE)               // 作用于类
@Retention(RetentionPolicy.SOURCE)      // ⚠️ 编译期有效，运行时丢弃
public @interface AutoBuilder {
    String suffix() default "Builder";
}
```

### 4.2 第二步：实现注解处理器

```java
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

@SupportedAnnotationTypes("com.example.AutoBuilder")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AutoBuilderProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        // 遍历所有被 @AutoBuilder 标注的类
        for (TypeElement element :
                (Set<TypeElement>) roundEnv.getElementsAnnotatedWith(AutoBuilder.class)) {

            String className = element.getSimpleName().toString();
            String packageName = processingEnv.getElementUtils()
                    .getPackageOf(element).toString();
            String builderClassName = className + "Builder";

            try {
                // 创建新的 Java 源文件
                JavaFileObject fileObject = processingEnv.getFiler()
                        .createSourceFile(packageName + "." + builderClassName);

                try (Writer writer = fileObject.openWriter()) {
                    StringBuilder code = new StringBuilder();
                    code.append("package ").append(packageName).append(";\n\n");
                    code.append("public class ").append(builderClassName).append(" {\n");
                    code.append("    private ").append(className)
                            .append(" target = new ").append(className).append("();\n\n");
                    code.append("    public ").append(className)
                            .append(" build() {\n");
                    code.append("        return target;\n");
                    code.append("    }\n");
                    code.append("}");
                    writer.write(code.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;  // 已处理，无需后续处理器再处理
    }
}
```

### 4.3 第三步：注册注解处理器

在 `resources/META-INF/services/` 目录下创建文件：

```
文件名：javax.annotation.processing.Processor
内容：  com.example.AutoBuilderProcessor
```

### 4.4 第四步：使用注解

```java
@AutoBuilder
public class User {
    private String username;
    private Integer age;
    // 编译后自动生成 UserBuilder 类
}
```

### 4.5 编译验证

执行 `mvn compile` 或 `javac` 后，自动生成 `UserBuilder.java`：

```java
package com.example;

public class UserBuilder {
    private User target = new User();

    public User build() {
        return target;
    }
}
```

---

## 5. 注解处理核心注意事项

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| **注解不生效** | `@Retention` 设为 `RUNTIME` 而非 `SOURCE` | 编译期处理必须设 `RetentionPolicy.SOURCE` |
| **处理器未执行** | 未注册处理器 | 必须 `META-INF/services/` 注册 + IDEA 开启注解处理 |
| **修改源码失败** | 处理器只能生成新文件 | ❌ 不能修改原有源码（Lombok 是特殊机制修改 AST） |
| **重复生成文件** | `process()` 被多次调用 | 通过 `RoundEnvironment` 判断是否处理完成 |
| **JDK 版本兼容** | 不同版本对注解支持不同 | `@SupportedSourceVersion` 与项目 JDK 一致；JDK 9+ 需 `module-info.java` |
| **编译顺序错误** | 处理器未先于业务代码编译 | 处理器单独打包为独立模块 |
| **异常中断编译** | 处理器内异常未捕获 | 妥善 catch，避免 throw 中断整个编译流程 |
| **IDEA 不生成代码** | 注解处理功能未开启 | `Settings → Build → Compiler → Annotation Processors → Enable` |

---

## 6. 脚本编译 vs 注解处理对比

| 对比维度 | Java 脚本编译 | 注解处理 |
|----------|-------------|----------|
| **执行时机** | JVM 运行时 | javac 编译期 |
| **核心作用** | 动态执行逻辑，运行时扩展 | 编译期生成/校验代码 |
| **性能损耗** | ❌ 有编译开销，频繁执行影响性能 | ✅ 无运行时损耗 |
| **安全风险** | ⚠️ 高，需防范代码注入 | ✅ 低，仅编译期执行 |
| **典型应用** | 动态规则引擎、插件化、低代码 | Lombok、Spring Boot 自动配置、MyBatis Mapper |
| **代码侵入** | 有（需编写动态源码） | 无（注解标注即可） |

### 选型指南

```text
需要固定逻辑代码自动化 → 优先注解处理（Lombok 模式）
需要运行时动态扩展逻辑 → 使用脚本编译（规则引擎模式）
            │
            ├── 编译期：注解处理 = 零运行时开销 + 高安全性
            └── 运行时：脚本编译 = 灵活动态 + 需安全管控
```

---

> 🎯 **核心总结**：脚本编译适合运行时动态扩展，核心风险是安全与内存管理；注解处理适合编译期代码生成与校验，核心优势是零运行时开销。实际开发中优先使用注解处理实现代码自动化，仅在业务逻辑需要动态变更时使用脚本编译，同时严格遵守注意事项规避各类异常。
