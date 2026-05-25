快速学会 Python（极速版，零基础可直接上手，只学核心、够用、能干活）
一、Python 是什么
- 解释型、弱类型、跨平台脚本语言
- 语法极简、少括号少分号，可读性极强
- 万能语言：后端、AI、爬虫、数据分析、自动化、脚本全都能用
- 你后续 LangChain、RAG、Agent、Embedding 全部依赖 Python
 
二、必学核心语法（最简）
1. 输出 / 输入
    python
    print("Hello")
    name = input("请输入：")
 
2. 基础数据类型
    python
    a = 10       # 整数
    b = 3.14     # 浮点数
    c = "文字"   # 字符串
    d = True     # 布尔值
 
3. 容器（高频必用）
    python

# 列表 List：可变、有序
arr = [1,2,3]
arr.append(4)

# 字典 Dict：键值对，开发最常用
info = {"name":"张三","age":20}
print(info["name"])

# 元组、集合 了解即可
 
4. 条件判断
    python
    x = 18
    if x >= 18:
    print("成年")
    elif x > 0:
    print("未成年")
    else:
    print("非法")
 
5. 循环
    python

# for 循环
for i in range(5):
    print(i)

# while 循环
n = 0
while n < 3:
    n += 1
 
6. 函数
    python
    def add(a,b):
    return a + b
    res = add(2,3)
 
7. 注释
    python

# 单行注释
"""
多行注释
"""
 
 
三、三大关键特性（和Java区别）
1. 不用声明变量类型
     a = 1  直接定义，自动识别类型
2. 缩进决定代码块
    不用  {} ，靠 4空格 缩进，错缩进直接报错
3. 自带海量标准库
    文件、网络、时间、正则，开箱即用
 
四、文件操作（你写笔记/处理文档常用）
python

# 写入
with open("test.txt","w",encoding="utf-8") as f:
    f.write("测试内容")

# 读取
with open("test.txt","r",encoding="utf-8") as f:
    text = f.read()
 
 
五、模块/库导入（AI 开发核心）
python
import time
from math import sqrt
 
你之前运行的：
 langchain、ollama、pymilvus  全是第三方库
安装命令固定格式：
bash
pip install 库名
 
 
六、异常捕获（写项目必备）
python
try:
    num = 1 / 0
except Exception as e:
    print("出错：",e)
 
 
七、极简学习路线（2天吃透够用版）
1. 掌握上面所有基础语法
2. 学会：列表、字典、循环、函数、文件读写
3. 会用  pip  装库、运行  .py  脚本
4. 练 10 个小案例：批量处理、文本操作、简单爬虫
5. 直接衔接 AI：LangChain、RAG 项目实战
 
八、一句话记忆
1. Python 语法极简，缩进为王
2. 变量无类型声明，容器优先用 列表/字典
3. 函数+循环+文件操作=日常80%场景
4. 你学 AI 全套技术，Python 是唯一底层工具
