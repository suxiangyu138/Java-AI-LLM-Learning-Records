import javax.script.*;

/**
 * Java 调用 JavaScript 脚本示例（修复 console 未定义问题）
 */
public class ScriptDemo {
    public static void main(String[] args) throws ScriptException, NoSuchMethodException {
        // 1. 获取脚本引擎管理器
        ScriptEngineManager manager = new ScriptEngineManager();
        // 2. 获取 JavaScript 引擎（JDK8 内置 Nashorn，JDK15+ 需手动引入）
        ScriptEngine engine = manager.getEngineByName("JavaScript");

        // 场景1：执行简单脚本
        String simpleScript = "var a = 10; var b = 20; a + b";
        Object result = engine.eval(simpleScript);
        System.out.println("✅ 脚本执行结果：" + result); // 输出 30

        // 场景2：Java 变量绑定到脚本 + 修复 console.log 问题
        engine.put("name", "Java 程序员"); // 绑定 Java 变量到脚本

        // 方案1：用 print() 替代 console.log（推荐）
        engine.eval("print('👋 你好，' + name);");

        // 方案2（可选）：绑定自定义 console 对象
        // engine.put("console", new Object() {
        //     public void log(Object msg) {
        //         System.out.println(msg);
        //     }
        // });
        // engine.eval("console.log('👋 你好，' + name);");

        // 场景3：脚本调用 Java 方法
        // 定义 Java 方法
        engine.eval("function add(x, y) { return x + y; }");
        // 调用脚本中的方法
        Invocable invocable = (Invocable) engine;
        Object addResult = invocable.invokeFunction("add", 5, 8);
        System.out.println("✅ 脚本方法调用结果：" + addResult); // 输出 13
    }
}