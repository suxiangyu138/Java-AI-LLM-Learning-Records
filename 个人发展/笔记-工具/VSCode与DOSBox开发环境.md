# VSCode写代码 + DOSBox跑编译

## 一、DOS窗口基础命令（必背4个）
1、挂载你的文件夹（最重要）
```plaintext
mount c: D:\asm
```
 
把电脑文件夹 → 变成虚拟机C盘
2、进入C盘
```plaintext
c:
```
 
3、看里面有哪些文件
```plaintext
dir
```
 
4、清屏
```plaintext
cls
```
 
 
## 二、汇编编译三连（考试必考3句）
1、masm 编译 asm源码 → 生成obj文件
```plaintext
masm test.asm;
```
 
后面分号一定要加，不用一直回车狂点
2、link 链接obj → 生成exe可执行文件
```plaintext
link test.obj;
```
 
3、直接运行程序
```plaintext
test
```
 
 
## 三、调试看寄存器（期末必用1句）
```plaintext
debug test.exe
```
 
debug里面极简4个就够：
- r 看ax、bx、cs、ip寄存器
- t 单步执行一条指令
- g 直接跑满程序
- q 退出调试
 
总共就这8个指令
mount、c:、dir、cls
masm、link、运行、debug
记住顺序：
挂载→进C→编译→链接→运行/调试
