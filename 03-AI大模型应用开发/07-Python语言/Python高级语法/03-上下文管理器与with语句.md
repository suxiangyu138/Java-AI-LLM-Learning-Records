# 03 - 上下文管理器与 with 语句

> 🎯 `with` 语句是 Python 资源管理的标准范式——文件自动关闭、数据库连接自动归还、GPU 显存自动释放。AI 开发中管理模型加载/推理状态的上下文无处不在

---

## 目录

1. [协议与实现](#1-协议与实现)
2. [contextmanager 装饰器](#2-contextmanager-装饰器)
3. [嵌套与组合](#3-嵌套与组合)
4. [AI 开发实战](#4-ai-开发实战)

---

## 1. 协议与实现

```python
# 实现 __enter__ + __exit__
class ManagedFile:
    def __init__(self, filename, mode='r'):
        self.filename = filename
        self.mode = mode

    def __enter__(self):
        self.file = open(self.filename, self.mode)
        return self.file

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.file.close()         # 即使有异常也会执行！
        if exc_type:              # 返回 True 可吞掉异常（不推荐）
            print(f"处理异常: {exc_type.__name__}")
        return False              # False = 异常继续传播

with ManagedFile('data.txt', 'w') as f:
    f.write('hello')
# f 自动关闭（不管有没有异常）
```

## 2. contextmanager 装饰器

```python
from contextlib import contextmanager

# @contextmanager: 三行代码 = 一个上下文管理器
@contextmanager
def timer(name):
    start = time.perf_counter()
    yield                            # ← yield 前 = __enter__，后 = __exit__
    elapsed = time.perf_counter() - start
    print(f"{name}: {elapsed:.3f}s")

with timer("模型推理"):
    model.generate(prompt)
# → 模型推理: 1.234s
```

### 常用 contextlib 工具

```python
from contextlib import suppress, redirect_stdout, ExitStack

# suppress: 忽略特定异常
with suppress(FileNotFoundError):
    os.remove("maybe_not_exist.txt")  # 不存在也不报错

# redirect_stdout: 重定向输出
with open("log.txt", "w") as f, redirect_stdout(f):
    print("这行写入了 log.txt")

# nullcontext: 什么都不做的上下文（条件用）
ctx = open("file.txt") if use_file else nullcontext()
with ctx as f:
    pass
```

## 3. 嵌套与组合

```python
# 多个上下文管理器同时
with open("input.txt") as src, open("output.txt", "w") as dst:
    dst.write(src.read())

# ExitStack: 动态管理多个上下文
with ExitStack() as stack:
    files = [stack.enter_context(open(f"file{i}.txt")) for i in range(10)]
    # 10 个文件全部自动关闭（即使中间出错）
```

## 4. AI 开发实战

```python
# 1. 模型推理上下文（自动加载+卸载模型）
@contextmanager
def load_model(model_path):
    print(f"加载模型: {model_path}")
    model = AutoModel.from_pretrained(model_path).to("cuda")
    try:
        yield model
    finally:
        del model
        torch.cuda.empty_cache()
        print("模型已卸载，GPU 显存已释放")

with load_model("llama-8b") as model:
    result = model.generate("Hello")

# 2. 临时环境变量
@contextmanager
def set_env(**kwargs):
    old = {k: os.environ.get(k) for k in kwargs}
    os.environ.update(kwargs)
    yield
    for k, v in old.items():
        if v is None: os.environ.pop(k, None)
        else: os.environ[k] = v

with set_env(CUDA_VISIBLE_DEVICES="0", TOKENIZERS_PARALLELISM="false"):
    train_model()

# 3. 计时器（AI Benchmark）
@contextmanager
def benchmark(name):
    torch.cuda.synchronize()  # 等 GPU
    start = time.perf_counter()
    yield
    torch.cuda.synchronize()
    print(f"{name}: {(time.perf_counter()-start)*1000:.1f}ms")
```

## 核心要点回顾

- `with` = `__enter__`(获取资源) → 执行块 → `__exit__`(释放资源)
- 异常安全：`__exit__` 永远执行（等于 Java try-finally）
- `@contextmanager` + `yield` = 最简洁的上下文管理器写法
- 多上下文：`with A() as a, B() as b:` 一行管理多个
- AI 场景：模型加载/卸载、临时配置、计时器

## 参考资料

1. PEP 343: with statement
2. contextlib 官方文档
