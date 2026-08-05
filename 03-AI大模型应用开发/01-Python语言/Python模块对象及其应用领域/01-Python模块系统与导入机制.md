# 01 - Python 模块系统与导入机制

> 🎯 `import` 不只是"导入一个文件" — 它是一整套查找→编译→执行→缓存的机制。理解模块系统是理解大型 Python 项目结构的前提

---

## 目录

1. [模块 vs 包 vs 命名空间包](#1-模块-vs-包-vs-命名空间包)
2. [import 的完整执行流程](#2-import-的完整执行流程)
3. [相对导入与绝对导入](#3-相对导入与绝对导入)
4. [循环导入问题与解决方案](#4-循环导入问题与解决方案)
5. [sys.modules 与 sys.path](#5-sysmodules-与-syspath)
6. [__name__ 与 if __name__ == "__main__"](#6-__name__-与-if-__name__--__main__)

---

## 1. 模块 vs 包 vs 命名空间包

| 概念 | 定义 | 导入示例 |
|------|------|----------|
| **模块** | 一个 `.py` 文件 | `import my_module` |
| **常规包** | 含 `__init__.py` 的目录 | `from my_pkg import sub` |
| **命名空间包** | 无 `__init__.py`、分布在多个目录的包（Python 3.3+） | `import namespace_pkg.module` |

```text
my_project/
├── my_module.py              # ← 模块
├── regular_pkg/              # ← 常规包
│   ├── __init__.py
│   └── sub.py
└── namespace_pkg/            # ← 命名空间包（无 __init__.py）
    └── module_a.py
```

---

## 2. import 的完整执行流程

```text
import my_module 的五步：

① 查缓存：my_module 是否已在 sys.modules 中？
   → 是 → 直接返回（不重复执行！）

② 找模块：在 sys.path 中搜索 my_module.py 或 my_module/__init__.py
   → 先找内置模块 → 再按 sys.path 顺序查找

③ 编译：.py → .pyc（字节码缓存于 __pycache__/）

④ 执行：在模块的命名空间中执行模块代码（定义函数/类/变量）

⑤ 缓存：模块对象存入 sys.modules（下次 import 直接返回）
```

**追问：** `from X import Y` 和 `import X` 的区别？→ `from X import Y` 仍会执行整个 X 模块（并缓存），只是只把 Y 绑定到当前命名空间。

---

## 3. 相对导入与绝对导入

```python
# 推荐：绝对导入（清晰、不易出错）
from my_project.utils.helpers import format_result

# 相对导入（包内部使用，用 . 表示当前包层级）
from . import sibling          # 同目录的 sibling 模块
from ..parent import something # 上级目录
from .subpkg import module     # 子包中的模块
```

> ⚠️ 相对导入只能在包内部使用（即文件被作为包的一部分导入时）。直接 `python script.py` 执行时不能用相对导入。

---

## 4. 循环导入问题与解决方案

```python
# ❌ 问题：A 导入 B，B 导入 A → 互相依赖
# a.py
from b import func_b       # B 还没执行完 → func_b 未定义 → ImportError

# 解决方案①：延迟导入（在函数内 import）
# a.py
def call_b():
    from b import func_b   # 此时 B 已完全加载
    return func_b()

# 解决方案②：导入模块而非成员
# a.py
import b                   # 导入整个模块，属性在访问时才解析
b.func_b()

# 解决方案③：重构（抽取共同依赖到第三个模块 c.py）
```

---

## 5. sys.modules 与 sys.path

```python
import sys

# sys.modules：所有已加载模块的字典
print(sys.modules.keys())    # 看哪些模块已被加载
print(sys.modules.get('json'))  # 获取已加载的 json 模块对象

# sys.path：模块搜索路径列表
print(sys.path)
# ['', '/usr/lib/python3.12', '.../site-packages', ...]
# 插入自定义路径（临时）
sys.path.insert(0, '/path/to/my/modules')
```

**AI 场景：** 动态添加模型目录 → `sys.path.append("./models")` 然后 `import my_model`。

---

## 6. __name__ 与 if __name__ == "__main__"

```python
# 当 my_module.py 被直接执行：
# python my_module.py  →  __name__ == "__main__"  ✅

# 当 my_module.py 被导入：
# import my_module     →  __name__ == "my_module"

# 标准写法：既能当脚本跑、又能当模块导入
if __name__ == "__main__":
    main()     # 直接执行时运行
```

---

> 🎯 **核心要点**：模块系统的三个关键认知 — ① `import` = 查找→编译→执行→缓存、**同一模块只执行一次** ② 循环导入的解药是"延迟导入或导入模块而非成员" ③ `sys.modules` 是所有模块的全局缓存（字典）。

**下一模块**：[02-系统交互与文件处理模块](02-系统交互与文件处理模块.md) / **返回总览**：[00-总览](00-Python模块对象知识体系总览.md)
