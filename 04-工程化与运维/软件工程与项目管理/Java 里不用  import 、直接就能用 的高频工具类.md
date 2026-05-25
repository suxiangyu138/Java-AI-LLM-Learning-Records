Java 里不用  import 、直接就能用 的高频工具/类
一、Math 类（数学工具，刷题最常用）
全是静态方法，直接  Math.xxx()  调用
java
// 取最大值
Math.max(a, b)
// 取最小值
Math.min(a, b)
// 绝对值
Math.abs(-5)
// 平方
Math.pow(2,3)
// 开根号
Math.sqrt(9)
// 向上取整
Math.ceil(3.2)
// 向下取整
Math.floor(3.9)
// 四舍五入
Math.round(3.6)
// 随机数 [0,1)
Math.random()
 
 
二、String 类（字符串）
直接声明、直接用，不用导包
java
String s = "hello";
s.length();
s.charAt(0);
s.equals("abc");
s.substring(1,3);
s.indexOf('e');
s.toCharArray();
 
 
三、System 类（系统操作）
java
// 打印输出
System.out.println();
// 错误输出
System.err.println();
// 系统当前时间毫秒值
System.currentTimeMillis();
 
 
四、基本类型包装类
 Integer / Double / Boolean / Character  等
java
// 字符串转数字
Integer.parseInt("123");
// 数字转字符串
Integer.toString(123);
// 最大值、最小值
Integer.MAX_VALUE
Integer.MIN_VALUE
 
 
五、Object 类（所有类的父类）
所有对象都自带这些方法
java
obj.toString()
obj.equals(另一个对象)
obj.hashCode()
 
 
六、数组工具（简单常用）
java
// 数组长度
arr.length
 
 
总结
只要是  java.lang  包里的类，全都不用写 import，直接用！
刷题时最常用的就是： Math 、 String 、 System 、 Integer  这几个。
