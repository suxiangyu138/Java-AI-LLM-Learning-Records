# 11 - Python 面试题与速查

> 🎯 Java 后端面试中 Python 的 30 道高频题 + 一页语法速查卡

## 高频面试题

**Q1: Python 和 Java 的核心区别？**
```text
解释 vs 编译、动态 vs 静态类型、多继承支持、GIL 全局锁
```

**Q2: 装饰器原理？**
```text
本质是闭包 → 接收函数 → 返回新函数 → @语法糖
在 FastAPI/Flask 中大量使用：@app.get("/")、@retry
```

**Q3: GIL 是什么？怎么绕过？**
```text
Global Interpreter Lock → 同一时刻只有一个线程执行 Python 字节码
绕过：多进程(multiprocessing)、C扩展(numpy)、异步(asyncio)
```

**Q4: `*args` 和 `**kwargs` 的区别？**
```text
*args：位置参数 → 元组
**kwargs：关键字参数 → 字典
```

**Q5: 浅拷贝 vs 深拷贝？**
```text
浅拷贝：copy.copy() → 只复制第一层，嵌套对象共享
深拷贝：copy.deepcopy() → 递归复制所有层
```

**Q6: 生成器 vs 列表的区别？**
```text
列表：[x for x in range(10)] → 立即生成，占内存
生成器：(x for x in range(10)) → 惰性生成，省内存
LLM 流式输出本质是生成器
```

**Q7: `is` vs `==`？**
```text
is：身份相等（同一对象）
==：值相等（内容相同）
使用 is 判断 None/True/False
```

**Q8: Python 的 GC 机制？**
```text
引用计数（主要）+ 标记清除（循环引用）+ 分代回收
Java：标记-清除/复制/整理 → 无引用计数
```

## 一页语法速查卡

| 特性 | Python | Java |
|------|--------|------|
| 入口 | `if __name__ == "__main__"` | `public static void main` |
| 列表推导 | `[x*2 for x in range(10)]` | `stream().map()` |
| Lambda | `lambda x: x*2` | `x -> x*2` |
| 字符串格式化 | `f"Hello {name}"` | `"Hello " + name` 或 `String.format` |
| 异常 | `try/except/finally` | `try/catch/finally` |
| 多线程 | `threading.Thread` | `new Thread(...)` |
| HTTP 请求 | `requests.get(url)` | `HttpClient` / RestTemplate |
| JSON | `json.loads(s)` / `json.dumps(o)` | Jackson/Gson |
| 遍历索引 | `for i, v in enumerate(lst)` | `for (int i=0;...)` |
| 三目 | `a if cond else b` | `cond ? a : b` |
