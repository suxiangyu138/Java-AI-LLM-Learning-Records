"""
Python 面向对象编程学习包。

本包包含从基础到企业级实践的 OOP 示例代码，涵盖：
    基础篇：
        - 类与对象
        - 封装与属性
        - 继承与多态
        - 魔术方法
        - 数据类
        - 组合优于继承
        - 常见设计模式
    进阶篇：
        - 元类（Metaclasses）
        - 描述符（Descriptors）
        - __slots__ 内存优化
        - 枚举（Enum）
        - 泛型编程
        - 迭代器与生成器
        - SOLID 原则实战
        - 面向对象单元测试
"""

import importlib

__version__ = "2.0.0"

_module_names = [
    "01_class_and_object",
    "02_encapsulation",
    "03_inheritance",
    "04_polymorphism",
    "05_magic_methods",
    "06_dataclasses",
    "07_composition",
    "08_design_patterns",
    "09_metaclasses",
    "10_descriptors",
    "11_slots",
    "12_enums",
    "13_generics",
    "14_iterators_generators",
    "15_solid_principles",
    "16_testing_oop",
]

_module_aliases = [
    "class_and_object",
    "encapsulation",
    "inheritance",
    "polymorphism",
    "magic_methods",
    "dataclasses_demo",
    "composition",
    "design_patterns",
    "metaclasses",
    "descriptors",
    "slots",
    "enums",
    "generics",
    "iterators_generators",
    "solid_principles",
    "testing_oop",
]

__all__ = _module_aliases.copy()

for name, alias in zip(_module_names, _module_aliases):
    mod = importlib.import_module(f".{name}", package=__package__)
    globals()[alias] = mod
