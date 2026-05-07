"""
Python 面向对象编程 —— 企业级学习示例（共 16 个模块）。

运行方式：
    python main.py           # 依次运行所有模块
    python -m oop_basics.01_class_and_object   # 单独运行某个模块
"""

from typing import Callable

from oop_basics import (
    class_and_object,
    composition,
    dataclasses_demo,
    descriptors,
    design_patterns,
    encapsulation,
    enums,
    generics,
    inheritance,
    iterators_generators,
    magic_methods,
    metaclasses,
    polymorphism,
    slots,
    solid_principles,
    testing_oop,
)


def main() -> None:
    """按顺序运行所有 OOP 教学模块。"""
    modules: list[tuple[str, Callable[[], None]]] = [
        # ---- 基础篇 ----
        ("类与对象", class_and_object.demo),
        ("封装与属性", encapsulation.demo),
        ("继承", inheritance.demo),
        ("多态", polymorphism.demo),
        ("魔术方法", magic_methods.demo),
        ("数据类", dataclasses_demo.demo),
        ("组合优于继承", composition.demo),
        ("设计模式", design_patterns.demo),
        # ---- 进阶篇 ----
        ("元类", metaclasses.demo),
        ("描述符", descriptors.demo),
        ("__slots__ 内存优化", slots.demo),
        ("枚举", enums.demo),
        ("泛型编程", generics.demo),
        ("迭代器与生成器", iterators_generators.demo),
        ("SOLID 原则", solid_principles.demo),
        ("单元测试", testing_oop.demo),
    ]

    print("\n" + "█" * 60)
    print("  Python OOP 企业级学习示例（基础篇 + 进阶篇）")
    print("█" * 60)

    for i, (title, demo_fn) in enumerate(modules, 1):
        print()
        demo_fn()

    print("\n" + "█" * 60)
    print("  全部 16 个模块运行完毕！")
    print(f"  建议按顺序阅读源码：oop_basics/01_*.py → 16_*.py")
    print("█" * 60 + "\n")


if __name__ == "__main__":
    main()
