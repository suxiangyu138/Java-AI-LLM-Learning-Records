# 03 注解原理与APT编译时处理

> 注解不是注释——它是代码的元数据。面试官问注解，其实是在问：你知道运行时注解（反射）和编译时注解（APT）的本质区别吗？

## 📚 目录

1. [注解的本质](#1-注解的本质)
2. [RetentionPolicy三级别](#2-retentionpolicy三级别)
3. [运行时注解处理（反射读取）](#3-运行时注解处理反射读取)
4. [编译时注解处理 APT](#4-编译时注解处理-apt)
5. [自定义注解实战](#5-自定义注解实战)
6. [注解的继承与组合](#6-注解的继承与组合)
7. [面试追问](#7-面试追问)

---

## 1. 注解的本质

### 1.1 @interface 编译后是 interface extends Annotation

注解（Annotation）在Java中是一种特殊的接口。当我们使用 `@interface` 定义一个注解时，编译器会将其编译成一个**继承 `java.lang.annotation.Annotation` 接口的接口**。

```java
// 定义一个注解
public @interface MyAnnotation {
    String value() default "default";
    int count() default 0;
}

// 编译后，等价于（伪代码）：
public interface MyAnnotation extends java.lang.annotation.Annotation {
    String value();
    int count();
}
```

**反编译验证：**

```bash
# 编译注解
javac MyAnnotation.java

# 反编译看字节码
javap -verbose MyAnnotation.class

# 输出关键信息:
# interface MyAnnotation extends java.lang.annotation.Annotation
#   SourceFile: "MyAnnotation.java"
#   RuntimeVisibleAnnotations: ...
#
# 方法:
#   public abstract String value();
#     AnnotationDefault:
#       default_value: s="default"
#   public abstract int count();
#     AnnotationDefault:
#       default_value: I=0
```

> 💡 **注解的本质**：`@interface` 只是一个语法糖，编译后就是 `interface extends Annotation`。注解中的"方法"对应的是注解的**元素（element）**，`default` 关键字设置默认值。

### 1.2 元注解（Meta-Annotations）

元注解是**注解其他注解**的注解。Java标准库提供5个元注解：

```java
import java.lang.annotation.*;

// === @Target —— 该注解可以应用在哪里 ===
@Target({
    ElementType.TYPE,              // 类、接口、枚举
    ElementType.FIELD,             // 字段
    ElementType.METHOD,            // 方法
    ElementType.PARAMETER,         // 参数
    ElementType.CONSTRUCTOR,       // 构造器
    ElementType.LOCAL_VARIABLE,    // 局部变量
    ElementType.ANNOTATION_TYPE,   // 注解类型
    ElementType.PACKAGE,           // 包
    ElementType.TYPE_PARAMETER,    // 类型参数（Java 8+）
    ElementType.TYPE_USE           // 使用类型的任何地方（Java 8+）
})
@interface TargetDemo {}

// === @Retention —— 注解的生命周期 ===
@Retention(RetentionPolicy.SOURCE)   // 仅源码，编译时丢弃
@Retention(RetentionPolicy.CLASS)    // 保留到字节码，运行时不可反射（默认）
@Retention(RetentionPolicy.RUNTIME)  // 运行时可通过反射获取
@interface RetentionDemo {}

// === @Documented —— 是否进入Javadoc ===
@Documented  // 被注解的元素的Javadoc中会包含该注解
@interface DocumentedDemo {}

// === @Inherited —— 是否被子类继承（仅对类有效） ===
@Inherited  // 子类会继承父类的该注解（仅限于类上的注解）
@interface InheritedDemo {}

// === @Repeatable —— 是否可重复（Java 8+） ===
@Repeatable(Authors.class)  // 容器注解
@interface Author {
    String name();
}

// 容器注解——用于存放重复注解
@interface Authors {
    Author[] value();
}

// 使用重复注解
@Author(name = "Alice")
@Author(name = "Bob")
public class RepeatedAnnotationDemo {}

// 读取重复注解
void readRepeated() {
    Author[] authors = RepeatedAnnotationDemo.class
            .getAnnotationsByType(Author.class);
    for (Author author : authors) {
        System.out.println(author.name());
    }
}
```

### 1.3 注解元素类型限制

```java
public @interface ValidAnnotation {
    // 合法类型：
    String name();                         // String
    int age();                             // 基本类型
    double score();                        // 基本类型
    Status status();                       // 枚举
    Class<?> clazz();                      // Class
    MyAnnotation nested();                 // 注解类型
    String[] tags();                       // 以上类型的数组

    // default 关键字提供默认值
    String description() default "";
    int priority() default 5;
}

public @interface InvalidAnnotation {
    // ❌ 不合法——不能使用包装类型（除Class外）
    // Integer count();

    // ❌ 不合法——不能是Object或其他任意引用类型
    // Object obj();

    // ❌ 不合法——不能是数组的数组
    // String[][] matrix();

    // ❌ 不合法——不能有泛型
    // <T> T value();
}

enum Status {
    PENDING, PROCESSING, COMPLETED
}
```

> 🎯 **核心要点**：注解是特殊的接口，用 `@interface` 定义，编译器生成 `interface extends Annotation` 的字节码。元注解控制注解的行为（目标、生命周期、继承等）。注解元素只能是**基本类型、String、Class、枚举、注解、以及这些类型的数组**。

---

## 2. RetentionPolicy三级别

### 2.1 三个级别对比

`@Retention` 注解控制注解的**生命周期**，有三个级别：

```java
public enum RetentionPolicy {
    SOURCE,   // 源码级别 —— 编译时丢弃，不出现在.class文件中
    CLASS,    // 类文件级别 —— 保留在.class文件中，但运行时不可通过反射获取（默认值）
    RUNTIME   // 运行时级别 —— 保留在.class文件中，运行时可通过反射获取
}
```

| RetentionPolicy | 源码存在 | 字节码存在 | 运行时反射 | 典型示例 | 用途 |
|:---------------:|:--------:|:----------:|:----------:|----------|------|
| **SOURCE** | 是 | 否 | 否 | `@Override`, `@SuppressWarnings`, `@NonNull` | 编译器检查、Lint工具、代码生成 |
| **CLASS** | 是 | 是 | 否 | `@AutoService`, 某些字节码增强库 | 字节码工具、编译时处理 |
| **RUNTIME** | 是 | 是 | 是 | `@Autowired`, `@Test`, `@RequestMapping` | 框架运行时注入、反射处理 |

### 2.2 SOURCE级别详解

SOURCE级别的注解在编译时被编译器丢弃，**不会出现在.class文件中**。主要用于：

```java
import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@interface SourceLevel {
    String description() default "";
}

// Override是SOURCE级别——它的作用只是在编译期做检查
// 查看 @Override 的定义:
// @Target(ElementType.METHOD)
// @Retention(RetentionPolicy.SOURCE)
// public @interface Override {
// }

// 反编译带@Override的类，不会看到@Override注解
// 因为SOURCE注解在编译时就被丢弃了
```

```bash
# 反编译验证SOURCE注解不存在于字节码中
javap -verbose MyClass.class  # 不会出现 @Override

# 而RUNTIME注解会显示:
# RuntimeVisibleAnnotations:
#   0: #27(#28=s#29)
#     com.example.MyAnnotation(value="test")
```

### 2.3 CLASS级别详解（默认）

CLASS是 `@Retention` 的**默认值**。注解保留在字节码中，但JVM不会将其加载到内存，所以反射无法获取。

```java
@Retention(RetentionPolicy.CLASS)  // 等同于不加@Retention
@interface ClassLevel {}

// 使用:
@ClassLevel
public class ClassLevelDemo {}

// 运行时无法获取:
Method method = ClassLevelDemo.class.getMethod("someMethod");
// method.getAnnotation(ClassLevel.class) -> null
// 因为CLASS级别的注解在运行时不可见！
```

> ⚠️ **陷阱**：很多初学者以为只要写了 `@interface` 就能在运行时通过反射读取到，这是错的！如果忘记加 `@Retention(RetentionPolicy.RUNTIME)`，默认是CLASS级别，运行时**读不到**。这是面试中常见的概念性错误。

### 2.4 RUNTIME级别详解

RUNTIME是框架最常用的级别。注解保留在字节码中，JVM会在运行时加载，反射API可以访问。

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface RuntimeLevel {
    String value();
}

// Spring中的@Transactional定义:
// @Target({ElementType.TYPE, ElementType.METHOD})
// @Retention(RetentionPolicy.RUNTIME)
// @Inherited
// @Documented
// public @interface Transactional {
//     ...
// }
```

> 💡 **选择建议**：
> - 自己写工具或IDE插件用：**SOURCE**
> - 依赖字节码增强（如Lombok、AspectJ)：**CLASS**或**SOURCE**
> - Spring/MyBatis等通过反射读取：**RUNTIME**
> - 不确定时：**RUNTIME**（灵活性最高，但会增加运行时内存开销）

> 🎯 **核心要点**：RetentionPolicy的三个级别决定注解的**可见性和生命周期**。框架注解通常用RUNTIME（运行时反射读取）；IDE提示/编译器检查用SOURCE；字节码增强工具用CLASS。默认是CLASS，**如果不加@Retention，运行时反射读不到**。

---

## 3. 运行时注解处理（反射读取）

### 3.1 如何读取类/方法/字段/参数上的注解

```java
import java.lang.annotation.*;
import java.lang.reflect.*;

// 定义运行时注解
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface Component {
    String value() default "";
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface Loggable {
    Level level() default Level.INFO;
}

enum Level { DEBUG, INFO, WARN, ERROR }

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface Inject {
    String name() default "";
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@interface NotNull {}

// 使用注解
@Component("userService")
class UserService {

    @Inject(name = "userDao")
    private String userDao;

    @Loggable(level = Level.DEBUG)
    public void findUser(@NotNull int id) {
        System.out.println("查找用户: " + id);
    }
}

public class AnnotationReaderDemo {

    public static void main(String[] args) throws Exception {
        Class<?> clazz = UserService.class;

        // 1. 读取类上的注解
        Component component = clazz.getAnnotation(Component.class);
        if (component != null) {
            System.out.println("组件名称: " + component.value());
        }

        // 2. 读取字段上的注解
        for (Field field : clazz.getDeclaredFields()) {
            Inject inject = field.getAnnotation(Inject.class);
            if (inject != null) {
                System.out.println("字段 " + field.getName() +
                        " 注入: " + inject.name());
            }
        }

        // 3. 读取方法上的注解
        for (Method method : clazz.getDeclaredMethods()) {
            Loggable loggable = method.getAnnotation(Loggable.class);
            if (loggable != null) {
                System.out.println("方法 " + method.getName() +
                        " 日志级别: " + loggable.level());
            }

            // 4. 读取参数上的注解
            Annotation[][] paramAnnotations = method.getParameterAnnotations();
            Class<?>[] paramTypes = method.getParameterTypes();
            for (int i = 0; i < paramAnnotations.length; i++) {
                System.out.println("参数 " + paramTypes[i].getSimpleName() +
                        " 的注解: " + Arrays.toString(paramAnnotations[i]));
            }
        }

        // 5. 判断是否标注了特定注解
        boolean hasComponent = clazz.isAnnotationPresent(Component.class);
        System.out.println("类标注了@Component: " + hasComponent);

        // 6. 获取所有注解（包括继承的）
        Annotation[] allAnnotations = clazz.getAnnotations();
        System.out.println("所有注解: " + Arrays.toString(allAnnotations));
    }
}
```

### 3.2 自定义@Log注解 + AOP实现

```java
import java.lang.annotation.*;
import java.util.concurrent.ConcurrentHashMap;

// === 1. 自定义日志注解 ===
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface Log {
    String value() default "";
    boolean printParams() default true;
    boolean printResult() default false;
}

// === 2. 使用注解的业务类 ===
class BizService {

    @Log(value = "用户注册", printResult = true)
    public User registerUser(String username, String email) {
        System.out.println("注册用户: " + username + ", " + email);
        return new User(username, email);
    }

    @Log(value = "删除用户", printParams = false)
    public void deleteUser(int userId) {
        System.out.println("删除用户: " + userId);
    }
}

// === 3. 简化版AOP拦截器（基于动态代理） ===
class LogAspect {

    private static final ConcurrentHashMap<Class<?>, Object> proxyCache = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> T createProxy(T target) {
        Class<?> clazz = target.getClass();
        // 使用JDK动态代理
        return (T) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                clazz.getInterfaces(),
                (proxy, method, args) -> {
                    Log logAnno = method.getAnnotation(Log.class);
                    if (logAnno == null) {
                        // 没有@Log注解，直接调用
                        return method.invoke(target, args);
                    }

                    // 前置处理
                    StringBuilder sb = new StringBuilder();
                    sb.append("[日志] 操作: ").append(logAnno.value());
                    if (logAnno.printParams() && args != null) {
                        sb.append(", 参数: ").append(Arrays.toString(args));
                    }
                    System.out.println(sb);

                    long start = System.currentTimeMillis();

                    // 执行目标方法
                    Object result = method.invoke(target, args);

                    // 后置处理
                    long cost = System.currentTimeMillis() - start;
                    StringBuilder after = new StringBuilder();
                    after.append("[日志] 耗时: ").append(cost).append("ms");
                    if (logAnno.printResult() && result != null) {
                        after.append(", 结果: ").append(result);
                    }
                    System.out.println(after);

                    return result;
                }
        );
    }
}
```

```text
// 使用输出:
注册用户: Alice, alice@example.com
[日志] 操作: 用户注册, 参数: [Alice, alice@example.com]
[日志] 耗时: 1ms, 结果: User{username='Alice', email='alice@example.com'}
```

> 🎯 **核心要点**：运行时注解通过 `Class.getAnnotation()` / `getDeclaredFields()` / `getDeclaredMethods()` 等反射API读取。Spring AOP + 运行时注解是实现声明式编程（@Transactional、@Cacheable）的基础。记住：**只有@Retention(RUNTIME)的注解才能在运行时通过反射获取**。

---

## 4. 编译时注解处理 APT

### 4.1 AbstractProcessor工作原理

APT（Annotation Processing Tool）是javac的一部分，它在**编译时**扫描和处理注解，可以**生成新的Java源文件**，但**不能修改已有的类**。

```java
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.Writer;
import java.util.Set;

/**
 * 简化版APT处理器——检测没有@Override注解的重写方法
 */
@SupportedAnnotationTypes("java.lang.Override")  // 处理的注解类型
@SupportedSourceVersion(SourceVersion.RELEASE_8) // 支持的Java版本
public class OverrideCheckerProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        // RoundEnvironment 提供了这一轮的注解信息
        for (Element element : roundEnv.getElementsAnnotatedWith(Override.class)) {
            if (element.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) element;
                // 检查方法是否确实重写了父类/接口方法
                if (!isActuallyOverride(method)) {
                    // 如果方法没有重写但标注了@Override，输出错误
                    // 这会导致编译失败！
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            "方法未重写父类方法，但标注了@Override",
                            element
                    );
                }
            }
        }
        return true;  // true表示注解已被处理，不再传递给后续处理器
    }

    private boolean isActuallyOverride(ExecutableElement method) {
        // 实际的检查逻辑...
        // 通过processingEnv.getElementUtils()获取类型信息
        return true;
    }

    /**
     * APT处理器的完整生命周期：
     *
     * 1. init(ProcessingEnvironment) —— 初始化（在每次编译时执行一次）
     * 2. process(Set<? extends TypeElement>, RoundEnvironment) —— 处理每一轮注解
     *    - 可能有多轮处理（如果上一轮生成了新的注解）
     * 3. getSupportedAnnotationTypes() —— 声明支持的注解类型
     * 4. getSupportedSourceVersion() —— 声明支持的Java版本
     */
}
```

### 4.2 编译时生成Java代码

APT的核心能力是**生成Java源码文件**。这是许多框架（Dagger2、ButterKnife、AutoService）的关键技术。

```java
// === APT代码生成示例 ===
@SupportedAnnotationTypes("com.example.Builder")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class BuilderProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Builder.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                continue;
            }
            TypeElement classElement = (TypeElement) element;
            generateBuilderClass(classElement);
        }
        return true;
    }

    private void generateBuilderClass(TypeElement classElement) {
        String className = classElement.getSimpleName() + "Builder";
        String packageName = processingEnv.getElementUtils()
                .getPackageOf(classElement).getQualifiedName().toString();

        try {
            // 创建新的Java源文件
            JavaFileObject builderFile = processingEnv.getFiler()
                    .createSourceFile(packageName + "." + className);

            try (Writer writer = builderFile.openWriter()) {
                writer.write("package " + packageName + ";\n\n");
                writer.write("public class " + className + " {\n");
                writer.write("    private " +
                        classElement.getQualifiedName() + " target = new " +
                        classElement.getQualifiedName() + "();\n\n");

                // 为每个字段生成setter方法
                for (Element enclosed : classElement.getEnclosedElements()) {
                    if (enclosed.getKind() == ElementKind.FIELD) {
                        VariableElement field = (VariableElement) enclosed;
                        String fieldName = field.getSimpleName().toString();
                        String fieldType = field.asType().toString();

                        writer.write("    public " + className + " " +
                                fieldName + "(" + fieldType + " " +
                                fieldName + ") {\n");
                        writer.write("        target." + fieldName +
                                " = " + fieldName + ";\n");
                        writer.write("        return this;\n");
                        writer.write("    }\n\n");
                    }
                }

                writer.write("    public " +
                        classElement.getQualifiedName() + " build() {\n");
                writer.write("        return target;\n");
                writer.write("    }\n");
                writer.write("}\n");
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR, e.toString());
        }
    }
}
```

### 4.3 javac的注解处理轮次（Rounds）

APT处理是**多轮（Round）** 进行的，因为一轮处理可能产生新的注解，需要新一轮处理：

```bash
# 注解处理轮次示意图:
#
# Round 1:
#   输入: 原始源文件
#   处理: 所有注解处理器
#   输出: 可能产生新的源文件或新的注解
#
# Round 2:
#   输入: 上一轮生成的新源文件
#   处理: 继续处理新源文件中的注解
#   输出: 可能继续产生新的源文件 ...
#
# ... 持续直到没有新文件产生
#
# 最终轮 (Round N):
#   输入: 无新的注解需要处理
#   输出: 编译结束，进入常规编译阶段
```

```java
import javax.annotation.processing.RoundEnvironment;

// 在process方法中判断:
public class RoundAwareProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        // 判断是否是最后一轮
        if (roundEnv.processingOver()) {
            // 最后一轮，所有注解处理完成
            System.out.println("APT处理完成，共计" + roundEnv.getRootElements().size() + "个根元素");
            return false;
        }

        // 判断上一轮是否产生了错误
        if (roundEnv.errorRaised()) {
            System.out.println("上一轮产生了错误");
            return false;
        }

        // 正常处理逻辑
        System.out.println("本轮处理的注解: " + annotations);
        for (Element element : roundEnv.getElementsAnnotatedWith(MyAnnotation.class)) {
            // 处理...
        }

        return true;
    }
}
```

### 4.4 Lombok的实现原理（修改AST）

Lombok是一个**特例**——它**不是标准APT**！标准APT只能生成新的Java源文件，**不能修改已有的类**。而Lombok做到了修改已有类（如自动生成getter/setter/toString等）。

**Lombok的原理：**

```text
Lombok的工作原理：
1. 使用标准APT接口注册处理器（@SupportedAnnotationTypes等）
2. 但核心工作不在process()中完成
3. 而是通过JVM的附着机制（Attach API）或
   自定义的javac插件接口（com.sun.tools.javac.api.JavacTaskListener）
4. 在javac的解析（Parse）和生成（Generate）阶段之间
5. 直接操作javac的**AST（抽象语法树）**
6. 向AST中插入新的节点（Getter方法、Setter方法等）
7. javac继续正常编译修改后的AST，生成包含getter/setter的字节码

为什么不是标准APT？
- 标准APT的Filer.createSourceFile() 只能创建新文件
- 无法修改当前正在编译的类
- Lombok通过非公开API操作AST，这是非标准的hack
- 所以Lombok需要配置annotationProcessorPaths，且依赖于特定javac版本
```

```java
// Lombok的原理示意（简化伪代码）:
public class LombokGetterHandler {

    // 这不是标准APT，而是通过JavacPlugin或类似机制注入
    public void handleGetter(JCTree.JCClassDecl classDef, Getter annotation) {
        for (JCTree.JCVariableDecl field : getFields(classDef)) {
            // 在AST中插入getter方法
            JCTree.JCMethodDecl getter = createGetterMethod(field);
            classDef.defs = classDef.defs.append(getter);  // 直接修改AST！
        }
    }

    private JCTree.JCMethodDecl createGetterMethod(JCTree.JCVariableDecl field) {
        // 构建: public Type getField() { return this.field; }
        // 使用com.sun.tools.javac.tree.JCTree的API
        // ...
        return null;
    }
}
```

### 4.5 ButterKnife / Dagger2 vs Lombok原理对比

| 框架 | 原理 | 是否修改已有类 | 实现方式 | 运行时依赖 |
|------|------|:------------:|----------|:---------:|
| **ButterKnife** | 标准APT生成view绑定代码 | 否（生成新类） | `Filer.createSourceFile()` | 轻量运行时 |
| **Dagger2** | 标准APT生成Dependency Injection代码 | 否（生成新Factory类） | `Filer.createSourceFile()` | 无运行时（编译时完成） |
| **Lombok** | 非标准APT，操作javac AST | **是**（直接修改AST） | Javac Plugin API / 内部API | 仅编译期依赖 |
| **Google Auto** | 标准APT生成SPI元数据 | 否（生成资源文件） | `Filer.createResource()` | 无 |

```java
// === Dagger2生成的代码（APT生成的新文件）===
// 原始代码:
// @Module
// class AppModule {
//     @Provides
//     UserService provideUserService() {
//         return new UserServiceImpl();
//     }
// }

// Dagger2生成的代码（APT自动生成）:
// 生成文件: AppModule_ProvideUserServiceFactory.java
public final class AppModule_ProvideUserServiceFactory implements Factory<UserService> {

    private final AppModule module;

    public AppModule_ProvideUserServiceFactory(AppModule module) {
        this.module = module;
    }

    @Override
    public UserService get() {
        return provideUserService(module);
    }

    public static AppModule_ProvideUserServiceFactory create(AppModule module) {
        return new AppModule_ProvideUserServiceFactory(module);
    }

    public static UserService provideUserService(AppModule instance) {
        return Preconditions.checkNotNullFromProvides(instance.provideUserService());
    }
}

// === ButterKnife生成的代码（APT生成的新类）===
// 原始代码:
// class MainActivity extends Activity {
//     @BindView(R.id.tv_name) TextView tvName;
// }

// ButterKnife生成的代码:
public class MainActivity_ViewBinding implements Unbinder {
    private MainActivity target;

    public MainActivity_ViewBinding(MainActivity target, View source) {
        this.target = target;
        target.tvName = source.findViewById(R.id.tv_name);
    }

    @Override
    public void unbind() {
        target.tvName = null;
    }
}
```

> 💡 **Lombok的限制**：
> - 依赖非公开javac API，每个JDK版本可能需要适配
> - 与某些IDE的增量编译可能冲突
> - 团队中如果有人不知道Lombok，会看到编译错误
> - 生成的代码在源码中不可见，调试时可能困惑

> 🎯 **核心要点**：标准APT（Dagger2、AutoService等）只能**生成新文件**，不能修改已有类。Lombok**修改AST**做到了"修改已有类"，但这是非标准的hack。理解这两者的区别，是注解面试中的分水岭问题。

---

## 5. 自定义注解实战

### 5.1 实战1：@EnumValue校验注解 + 反射处理器

```java
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.annotation.*;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

// === 定义注解 ===
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EnumValueValidator.class)  // JSR 380 Bean Validation集成
@interface EnumValue {
    String message() default "参数值不在枚举范围内";

    Class<? extends Enum<?>> enumClass();

    boolean allowNull() default false;

    Class<?>[] groups() default {};

    Class<? extends javax.validation.Payload>[] payload() default {};
}

// === 验证器实现 ===
class EnumValueValidator implements ConstraintValidator<EnumValue, Object> {

    private Class<? extends Enum<?>> enumClass;
    private boolean allowNull;
    private Set<String> enumNames;

    @Override
    public void initialize(EnumValue annotation) {
        this.enumClass = annotation.enumClass();
        this.allowNull = annotation.allowNull();
        // 缓存枚举常量名——反射调用
        this.enumNames = Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return allowNull;
        }
        // 检查值是否是枚举中的有效名称
        return enumNames.contains(value.toString());
    }
}

// === 另一种方式：反射工具校验（不依赖JSR 380） ===
class EnumValidatorUtils {

    public static <T extends Enum<T>> void validateEnumField(Object obj) throws Exception {
        Class<?> clazz = obj.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            EnumValue enumValue = field.getAnnotation(EnumValue.class);
            if (enumValue != null) {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value == null && enumValue.allowNull()) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                Class<? extends Enum<?>> enumClass =
                        (Class<? extends Enum<?>>) enumValue.enumClass();
                boolean valid = Arrays.stream(enumClass.getEnumConstants())
                        .anyMatch(e -> e.name().equals(value.toString()));

                if (!valid) {
                    throw new IllegalArgumentException(
                            "字段 " + field.getName() + " 的值 " + value +
                                    " 不在枚举 " + enumClass.getSimpleName() + " 中"
                    );
                }
            }
        }
    }
}

// === 使用示例 ===
enum OrderStatus {
    PENDING, PAID, SHIPPED, DELIVERED, CANCELLED
}

class OrderDTO {
    @EnumValue(enumClass = OrderStatus.class, message = "无效的订单状态")
    private String status;

    @EnumValue(enumClass = OrderStatus.class, allowNull = true)
    private String previousStatus;

    public OrderDTO(String status) {
        this.status = status;
    }
}
```

### 5.2 实战2：@Builder编译时注解处理器（APT实现）

这是一个完整的APT处理器，在**编译时**生成Builder模式的代码。

```java
// === 第一步：定义注解 ===
// 文件: Builder.java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)  // 只在源码中存在，编译完就丢弃
public @interface Builder {}

// === 第二步：实现APT处理器 ===
// 文件: BuilderProcessor.java
@SupportedAnnotationTypes("com.example.Builder")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class BuilderProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Builder.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                processBuilder((TypeElement) element);
            }
        }
        return true;
    }

    private void processBuilder(TypeElement classElement) {
        String className = classElement.getSimpleName().toString();
        String builderClassName = className + "Builder";
        String packageName = getPackage(classElement);

        StringBuilder code = new StringBuilder();
        code.append("package ").append(packageName).append(";\n\n");
        code.append("public class ").append(builderClassName).append(" {\n");

        // 字段和setter方法
        for (Element enclosed : classElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosed;
                String fieldName = field.getSimpleName().toString();
                String fieldType = field.asType().toString();

                // 字段
                code.append("    private ").append(fieldType)
                        .append(" ").append(fieldName).append(";\n");
            }
        }

        code.append("\n    public ").append(builderClassName).append("() {}\n\n");

        // Setter链式方法
        for (Element enclosed : classElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosed;
                String fieldName = field.getSimpleName().toString();
                String fieldType = field.asType().toString();

                code.append("    public ").append(builderClassName)
                        .append(" ").append(fieldName)
                        .append("(").append(fieldType).append(" ").append(fieldName)
                        .append(") {\n");
                code.append("        this.").append(fieldName)
                        .append(" = ").append(fieldName).append(";\n");
                code.append("        return this;\n");
                code.append("    }\n\n");
            }
        }

        // Build方法
        code.append("    public ").append(className).append(" build() {\n");
        code.append("        ").append(className).append(" target = new ")
                .append(className).append("();\n");
        for (Element enclosed : classElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                String fieldName = enclosed.getSimpleName().toString();
                code.append("        target.").append(fieldName)
                        .append(" = this.").append(fieldName).append(";\n");
            }
        }
        code.append("        return target;\n");
        code.append("    }\n");
        code.append("}\n");

        // 写入生成的源文件
        try {
            JavaFileObject file = processingEnv.getFiler()
                    .createSourceFile(packageName + "." + builderClassName);
            try (Writer writer = file.openWriter()) {
                writer.write(code.toString());
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR, e.toString());
        }
    }

    private String getPackage(TypeElement element) {
        return processingEnv.getElementUtils()
                .getPackageOf(element).getQualifiedName().toString();
    }
}

// === 第三步：使用 ===
@Builder
class Person {
    String name;
    int age;
    String email;
    // 编译时自动生成 PersonBuilder 类
}

// 编译后可以这样使用：
// Person person = new PersonBuilder()
//     .name("Alice")
//     .age(25)
//     .email("alice@example.com")
//     .build();
```

### 5.3 APT处理器注册（META-INF/services）

要让javac找到你的APT处理器，需要注册SPI：

```bash
# 文件: META-INF/services/javax.annotation.processing.Processor
# 内容（每行一个处理器的全限定名）:
com.example.BuilderProcessor
com.example.EnumCheckProcessor
# ...
```

或者使用Google AutoService：

```java
@AutoService(Processor.class)  // 编译时自动生成META-INF/services文件
@SupportedAnnotationTypes("com.example.Builder")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class BuilderProcessor extends AbstractProcessor {
    // ...
}
```

> 🎯 **核心要点**：两个实战展示了**运行时反射处理注解**和**编译时APT处理注解**两种范式。运行时反射处理灵活（可修改已有逻辑），但有性能开销；编译时APT处理零运行时开销，但只能生成新文件。选择原则：能用编译时解决的就不要用运行时。

---

## 6. 注解的继承与组合

### 6.1 @Inherited —— 只对类继承有效

`@Inherited` 元注解控制注解是否被子类继承，但有两个重要限制：

```java
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface InheritableAnnotation {
    String value();
}

// 父类使用注解
@InheritableAnnotation("parent")
class Parent {}

// 子类继承
class Child extends Parent {}

// 测试
public class InheritedDemo {
    public static void main(String[] args) {
        // 子类可以获取到父类的 @InheritableAnnotation
        InheritableAnnotation childAnno = Child.class
                .getAnnotation(InheritableAnnotation.class);
        System.out.println("子类有注解: " +
                (childAnno != null ? childAnno.value() : "无"));
        // 输出: 子类有注解: parent

        // ❌ @Inherited 对接口无效
        // 实现接口的类不会继承接口上的@Inherited注解

        // ❌ @Inherited 对方法/字段等无效
        // 仅对类/接口上的注解有效
    }
}
```

**@Inherited的限制：**

```java
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)  // 即使Target包括METHOD，@Inherited也不作用于方法
@interface MethodAnnotation {}

class Base {
    @MethodAnnotation
    public void doSomething() {}
}

class Derived extends Base {
    @Override
    public void doSomething() {}
    // ❌ 子类重写的方法上 @MethodAnnotation 不会继承
    // getAnnotation(MethodAnnotation.class) -> null
}
```

### 6.2 注解不能继承注解（但可用@AliasFor模拟）

Java的注解**不能使用 `extends` 关键字继承另一个注解**。这与接口的继承不同。

```java
// ❌ 这不会编译！注解不能继承注解
// public @interface SubAnnotation extends BaseAnnotation {
// }

// Spring的解决方案：@AliasFor
// Spring通过反射模拟注解属性的继承/别名
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface RequestMapping {
    String path() default "";
    String method() default "GET";
}

// 组合注解——使用@AliasFor将value别名为path
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface GetMapping {
    @AliasFor(annotation = RequestMapping.class, attribute = "path")
    String value() default "";

    @AliasFor(annotation = RequestMapping.class, attribute = "method")
    String method() default "GET";
}
```

### 6.3 组合注解（如@SpringBootApplication）

Spring大量使用**组合注解**——在一个注解上标注另一个注解：

```java
// Spring Boot的核心注解
@SpringBootApplication  // 三条注解的组合
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}

// @SpringBootApplication 的定义：
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootConfiguration    // 组合一：标注这是一个Spring Boot配置
@EnableAutoConfiguration   // 组合二：启用自动配置
@ComponentScan              // 组合三：启用组件扫描
public @interface SpringBootApplication {
    // @AliasFor 将属性别名到被组合的注解
    @AliasFor(annotation = ComponentScan.class, attribute = "basePackages")
    String[] scanBasePackages() default {};

    @AliasFor(annotation = EnableAutoConfiguration.class, attribute = "exclude")
    Class<?>[] exclude() default {};
}

// Spring通过AnnotatedElementUtils读取组合注解:
// AnnotatedElementUtils.hasAnnotation(MyApplication.class, ComponentScan.class) -> true
// 因为Spring递归解析了组合注解中的注解
```

```java
// 模拟Spring的组合注解解析
class SpringAnnotationUtils {

    /**
     * 递归查找组合注解（简化版）
     */
    public static <A extends Annotation> A findComposedAnnotation(
            Class<?> clazz, Class<A> annotationType) {
        // 直接查找
        A annotation = clazz.getAnnotation(annotationType);
        if (annotation != null) {
            return annotation;
        }

        // 递归查找：遍历所有注解，查找被组合的注解
        for (Annotation metaAnno : clazz.getAnnotations()) {
            // 排除java.lang.annotation中的元注解
            if (metaAnno.annotationType().getName()
                    .startsWith("java.lang.annotation")) {
                continue;
            }

            // 递归查找——@SpringBootApplication上有没有@ComponentScan？
            A result = findComposedAnnotation(
                    metaAnno.annotationType(), annotationType);
            if (result != null) {
                return result;
            }
        }

        return null;
    }
}
```

> 🎯 **核心要点**：
> - `@Inherited` 只支持**类继承**，不支持**接口实现**和**方法/字段注解继承**
> - Java注解**不能直接继承**另一个注解，但Spring通过 `@AliasFor` 模拟了类似功能
> - **组合注解**是Spring框架的核心设计模式，`@SpringBootApplication` = `@SpringBootConfiguration` + `@EnableAutoConfiguration` + `@ComponentScan`
> - Spring通过递归解析注解上的注解（元注解），实现了对组合注解的支持

---

## 7. 面试追问

### Q1: "@Override这个注解的本质是什么？"

```text
面试官考察点：对最基础注解的理解 + Retention三级别

回答：
1. @Override的定义：
   @Target(ElementType.METHOD)
   @Retention(RetentionPolicy.SOURCE)
   public @interface Override {}

2. 它是SOURCE级别，这意味着：
   - 只在Java源码中存在
   - 编译后从字节码中消失（不是.class的一部分）
   - 运行时无法通过反射获取

3. 它的作用是编译期检查：
   - javac在编译时检测标注了@Override的方法是否确实重写了父类/接口方法
   - 如果标注了但没有重写（比如方法签名拼写错误），编译报错
   - 这可以及早发现编码错误，而不是等到运行时

4. 深层理解：
   - @Override是Java最早一批注解（Java 5引入）
   - 它展示了SOURCE注解的核心价值：在编码阶段提供安全检查
   - 类似的SOURCE注解还有@SuppressWarnings、@SafeVarargs等
```

### Q2: "Lombok是怎么工作的？为什么不是标准APT？"

```text
面试官考察点：对APT和字节码处理的理解深度

回答：
Lombok的工作机制：

1. Lombok注册为标准APT处理器——但这是"伪装"
   - 它确实实现了AbstractProcessor
   - 但process()方法几乎不做实际工作

2. 实际工作在javac的AST层次：
   - Lombok通过一个自定义的JavacTaskListener（或annotationProcessor）
   - 在javac的解析（Parse）和代码生成（Generate）之间介入
   - 直接操作com.sun.tools.javac.tree.JCTree（javac的AST）
   - 向AST中插入新的语法节点（getter方法、setter方法、构造器等）

3. 为什么不是标准APT？
   - 标准APT（JSR 269）只能通过Filer.createSourceFile()创建新文件
   - 不能修改当前正在编译的原始类
   - Lombok修改了原始类的AST，这突破了标准APT的能力边界
   - 所以Lombok依赖非公开的javac内部API

优劣势：
   - 优势：对使用者透明，源码看起来简洁
   - 劣势：依赖内部API，与IDE的兼容性需要维护
```

### Q3: "RUNTIME和CLASS注解有什么区别？"

```text
面试官考察点：对RetentionPolicy的理解

区别：

1. 存储级别：
   - RUNTIME：注解信息保留在.class文件中，JVM加载时读入内存
   - CLASS：注解信息保留在.class文件中，但JVM不加载到运行时内存

2. 反射访问：
   - RUNTIME：可以通过 getAnnotation() / getDeclaredAnnotation() 等反射API读取
   - CLASS：反射API读取不到（返回null）

3. 字节码可见性：
   两者在.class文件中都可见（javap可以看到）
   区别仅在于JVM运行时是否保留

4. 使用场景：
   - RUNTIME：框架注解（Spring @Autowired、JUnit @Test）
   - CLASS：字节码增强工具（如某些库在编译后处理字节码）

5. 内存占用：
   - RUNTIME：占用运行时内存（注解数据保存在方法区/元空间）
   - CLASS：不占用运行时内存

6. 默认值：
   - 如果没有指定@Retention，默认是CLASS
   - 这是一个常见陷阱
```

### Q4: "怎么实现一个类似Spring的自动装配注解？"

```java
// 简化的@Autowired实现

// 1. 定义注解
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
@interface MyAutowired {
    boolean required() default true;
}

// 2. 实现注入处理器
class MyAutowiredProcessor {

    private final Map<Class<?>, Object> container = new HashMap<>();

    public void registerBean(Class<?> clazz) throws Exception {
        Object instance = clazz.getDeclaredConstructor().newInstance();

        // 字段注入
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(MyAutowired.class)) {
                Object dependency = findBean(field.getType());
                if (dependency != null) {
                    field.setAccessible(true);
                    field.set(instance, dependency);
                } else {
                    MyAutowired anno = field.getAnnotation(MyAutowired.class);
                    if (anno.required()) {
                        throw new RuntimeException("无法注入: " + field.getType());
                    }
                }
            }
        }

        container.put(clazz, instance);
    }

    private Object findBean(Class<?> type) {
        return container.entrySet().stream()
                .filter(e -> type.isAssignableFrom(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}

// 3. 使用
class UserDao {
    public String findUser() { return "Alice"; }
}

class UserService {
    @MyAutowired
    private UserDao userDao;

    public void printUser() {
        System.out.println(userDao.findUser());
    }
}

public class AutowiredDemo {
    public static void main(String[] args) throws Exception {
        MyAutowiredProcessor processor = new MyAutowiredProcessor();
        processor.registerBean(UserDao.class);
        processor.registerBean(UserService.class);

        UserService service = (UserService) processor.container.get(UserService.class);
        service.printUser();  // 输出: Alice
    }
}
```

```text
核心要点：
1. 注解定义必须 @Retention(RUNTIME)
2. Bean容器维护类与实例的映射关系
3. 通过反射扫描字段上的@MyAutowired注解
4. 容器中查找匹配类型的Bean，通过反射注入
5. 支持required属性（找不到依赖时是否报错）
6. 实际Spring的注入更复杂（循环依赖、作用域、@Primary、@Qualifier等）
```

### Q5: "注解处理器中`getAnnotation()`和`getDeclaredAnnotation()`有什么区别？"

```text
区别：
1. getAnnotation()：返回指定注解，如果该注解是 @Inherited 的，会从父类查找
2. getDeclaredAnnotation()：只返回当前元素上的注解，不从父类查找

同样的区别存在于：
- getAnnotations() vs getDeclaredAnnotations()（获取所有注解）
- isAnnotationPresent() vs getDeclaredAnnotation() != null（判断存在性）
```

> 🎯 **面试核心**：注解面试题的核心脉络——**源码级（SOURCE）**、**编译时处理（APT/CLASS）**、**运行时处理（RUNTIME）**。理解这三个层级的本质区别和使用场景，远比背诵具体API重要。真正体现深度的，是能讲清楚**Lombok修改AST vs 标准APT生成新文件**的本质区别。

---

**下一模块**：[04 三剑合璧-框架设计实战](./04-三剑合璧-框架设计实战.md) | **返回总览**：[总览](./00-泛型反射注解知识体系总览.md)
