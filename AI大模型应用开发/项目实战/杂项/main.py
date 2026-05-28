"""
杂项工具集 - 主菜单
收集各种实用小工具，通过菜单选择执行
"""
import os
import sys
import json
import csv
import random
import string
from datetime import datetime
from pathlib import Path


def tool_password_generator():
    """密码生成器"""
    length = int(input("密码长度 (默认12): ") or "12")
    chars = string.ascii_letters + string.digits + "!@#$%^&*"
    password = ''.join(random.choice(chars) for _ in range(length))
    print(f"\n生成的密码: {password}\n")


def tool_file_stats():
    """文件/文件夹大小统计"""
    path = input("输入目录路径: ").strip()
    target = Path(path)
    if not target.exists():
        print("路径不存在！")
        return

    total_size = sum(f.stat().st_size for f in target.rglob('*') if f.is_file())
    file_count = sum(1 for f in target.rglob('*') if f.is_file())
    dir_count = sum(1 for d in target.rglob('*') if d.is_dir())

    print(f"\n=== {target.name} 统计 ===")
    print(f"文件数: {file_count:,}")
    print(f"文件夹数: {dir_count:,}")
    print(f"总大小: {total_size / (1024**3):.2f} GB")
    print(f"总大小: {total_size / (1024**2):.2f} MB\n")


def tool_json_formatter():
    """JSON 格式化"""
    raw = input("输入 JSON 字符串: ").strip()
    try:
        parsed = json.loads(raw)
        formatted = json.dumps(parsed, indent=2, ensure_ascii=False)
        print(f"\n格式化结果:\n{formatted}\n")
    except json.JSONDecodeError as e:
        print(f"JSON 解析错误: {e}")


def tool_timestamp_converter():
    """时间戳转换"""
    ts = input("输入时间戳 (秒): ").strip()
    try:
        dt = datetime.fromtimestamp(int(ts))
        print(f"转换结果: {dt.strftime('%Y-%m-%d %H:%M:%S')}\n")
    except ValueError as e:
        print(f"转换错误: {e}")


TOOLS = {
    "1": ("密码生成器", tool_password_generator),
    "2": ("文件/文件夹统计", tool_file_stats),
    "3": ("JSON 格式化", tool_json_formatter),
    "4": ("时间戳转换", tool_timestamp_converter),
}


def show_menu():
    print("\n" + "=" * 40)
    print("  Python 杂项工具集")
    print("=" * 40)
    for key, (name, _) in TOOLS.items():
        print(f"  [{key}] {name}")
    print("  [0] 退出")
    print("-" * 40)


if __name__ == "__main__":
    while True:
        show_menu()
        choice = input("请选择工具: ").strip()
        if choice == "0":
            print("再见！")
            break
        if choice in TOOLS:
            print(f"\n>>> {TOOLS[choice][0]}")
            TOOLS[choice][1]()
        else:
            print("无效选项，请重新选择。")
