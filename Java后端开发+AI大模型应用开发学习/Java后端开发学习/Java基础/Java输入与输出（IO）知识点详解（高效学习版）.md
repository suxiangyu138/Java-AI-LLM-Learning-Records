03.17 23:33
Java输入与输出（IO）知识点详解（高效学习版）
Java输入与输出（简称IO）是Java核心基础知识点，核心作用是实现程序与外部设备（文件、键盘、屏幕、网络等）的数据交互——输入（Input）是程序从外部获取数据（如读取文件、接收键盘输入），输出（Output）是程序向外部传递数据（如写入文件、在屏幕打印）。
学习IO的关键是“分清流的类型、掌握核心API、理解装饰者模式”，结合“输出倒逼输入”的学习方法，每学一种流就搭配代码练习，就能快速上手。以下内容全面覆盖IO核心知识点，循序渐进、重点突出，适配Java学习节奏。
一、IO核心基础（必懂前提）
1. 核心概念
流（Stream）：IO操作的核心载体，是“数据的流动通道”，数据以字节或字符为单位，从一个地方流向另一个地方（如从文件流向程序、从程序流向屏幕）。
数据源/目的地：流的起点（输入时）或终点（输出时），常见的有：文件、键盘、屏幕、网络连接、内存等。
IO分类：核心分为两大体系——字节流（处理所有类型数据）、字符流（仅处理文本数据），后续所有IO类都围绕这两大体系展开。
2. 核心分类（重中之重）
Java IO的分类有两个核心维度，必须分清，避免混淆：
按数据单位分类（核心维度） - 字节流：以字节（byte）为单位处理数据（1字节=8位），可处理所有类型数据（文本、图片、视频、音频等），核心父类：InputStream（输入）、OutputStream（输出）。 - 字符流：以字符（char）为单位处理数据（1字符=2字节，适配中文），仅能处理文本数据（.txt、.java等），核心父类：Reader（输入）、Writer（输出）。
按流向分类 - 输入流：数据从外部流向程序（读数据），核心父类：InputStream、Reader。 - 输出流：数据从程序流向外部（写数据），核心父类：OutputStream、Writer。
记忆技巧：字节流“万能”（处理所有数据），字符流“专一”（只处理文本）；输入流“读”、输出流“写”，对应父类名称就能区分（Input/Reader=读，Output/Writer=写）。
3. 核心原则
所有IO流类都实现了AutoCloseable接口，建议使用try-with-resources语法（Java7及以上），自动关闭流，避免手动关闭遗漏导致资源泄露。
流的操作是“顺序的”，不能反向读取/写入（如读取文件时，只能从开头读到末尾，不能倒着读，需倒读需特殊处理）。
字符流底层依赖字节流，会进行“编码/解码”（如UTF-8、GBK），需注意编码格式，避免中文乱码。
二、字节流（核心重点，万能流）
字节流是IO的基础，可处理所有类型数据，重点掌握“核心父类+常用子类+基本操作”，搭配代码练习，无需死记硬背。
1. 核心父类（抽象类，不能直接实例化）
InputStream（字节输入流）：所有字节输入流的父类，核心方法（抽象方法，子类必须重写）： - int read()：读取一个字节，返回读取的字节值（0-255），读取到末尾返回-1。 - int read(byte[] b)：读取多个字节，存入字节数组b，返回实际读取的字节数，末尾返回-1（推荐使用，效率更高）。 - void close()：关闭流，释放资源（try-with-resources可自动关闭）。
OutputStream（字节输出流）：所有字节输出流的父类，核心方法： - void write(int b)：写入一个字节（注意：传入的int值，仅取低8位作为字节）。 - void write(byte[] b)：写入字节数组b中的所有字节。 - void flush()：刷新流，将缓冲区中的数据强制写入目的地（字节流可选，字符流必须）。 - void close()：关闭流，关闭前会自动刷新。
2. 常用子类（实战常用，重点掌握）
（1）文件相关字节流（最常用）
用于读取/写入文件，是日常开发中最常用的字节流，核心类：FileInputStream、FileOutputStream。
// 示例1：使用FileInputStream读取文件（字节流读文件）
try (InputStream is = new FileInputStream("test.txt")) {
    byte[] buffer = new byte[1024]; // 缓冲区，提升读取效率
    int len;
    // 循环读取，len为实际读取的字节数，-1表示读取完毕
    while ((len = is.read(buffer)) != -1) {
        // 将字节数组转为字符串（注意编码格式，避免乱码）
        System.out.print(new String(buffer, 0, len, StandardCharsets.UTF_8));
    }
} catch (IOException e) {
    e.printStackTrace();
}
// 示例2：使用FileOutputStream写入文件（字节流写文件）
try (OutputStream os = new FileOutputStream("test.txt")) {
    String content = "Java IO 字节流示例";
    // 将字符串转为字节数组，写入文件
    os.write(content.getBytes(StandardCharsets.UTF_8));
    os.flush(); // 可选，关闭时会自动刷新
} catch (IOException e) {
    e.printStackTrace();
}
注意：FileOutputStream构造方法中，可添加第二个参数（boolean append），true表示“追加写入”，false表示“覆盖写入”（默认false）。
（2）其他常用字节流（了解即可，按需使用）
ByteArrayInputStream/ByteArrayOutputStream：以内存中的字节数组为数据源/目的地，用于在内存中操作字节数据（无需文件）。
DataInputStream/DataOutputStream：用于读取/写入基本数据类型（int、long、float等），避免手动转换字节。
三、字符流（文本专用，避免中文乱码）
字符流仅处理文本数据（如.txt、.java文件），底层依赖字节流，会进行编码/解码，核心解决“中文乱码”问题，重点掌握与字节流的区别和常用子类。
1. 核心父类（抽象类，不能直接实例化）
Reader（字符输入流）：所有字符输入流的父类，核心方法： - int read()：读取一个字符，返回字符的Unicode值（0-65535），末尾返回-1。 - int read(char[] cbuf)：读取多个字符，存入字符数组，返回实际读取的字符数，末尾返回-1。 - void close()：关闭流，释放资源。
Writer（字符输出流）：所有字符输出流的父类，核心方法： - void write(int c)：写入一个字符（传入的int值，取低16位作为字符）。 - void write(char[] cbuf)：写入字符数组。 - void write(String str)：直接写入字符串（最常用，无需转换）。 - void flush()：必须调用，字符流有缓冲区，不刷新会导致数据无法写入目的地。 - void close()：关闭流，关闭前会自动刷新。
2. 常用子类（实战常用，重点掌握）
（1）文件相关字符流（最常用）
用于读取/写入文本文件，核心类：FileReader、FileWriter，注意指定编码格式（避免中文乱码）。
// 示例1：使用FileReader读取文本文件（字符流读文件）
// 推荐指定编码格式（UTF-8），避免系统默认编码导致乱码
try (Reader reader = new InputStreamReader(new FileInputStream("test.txt"), StandardCharsets.UTF_8)) {
    char[] buffer = new char[1024];
    int len;
    while ((len = reader.read(buffer)) != -1) {
        System.out.print(new String(buffer, 0, len));
    }
} catch (IOException e) {
    e.printStackTrace();
}
// 示例2：使用FileWriter写入文本文件（字符流写文件）
try (Writer writer = new OutputStreamWriter(new FileOutputStream("test.txt", true), StandardCharsets.UTF_8)) {
    String content = "Java IO 字符流示例（追加写入）";
    writer.write(content);
    writer.flush(); // 必须调用，否则数据可能未写入
} catch (IOException e) {
    e.printStackTrace();
}
易错点：FileReader/FileWriter默认使用系统编码（如Windows默认GBK，Linux默认UTF-8），直接使用易出现中文乱码，推荐搭配InputStreamReader/OutputStreamWriter，手动指定编码格式（UTF-8）。
（2）其他常用字符流
BufferedReader/BufferedWriter：缓冲字符流（下文重点讲），提升读取/写入效率，新增readLine()方法（读取一行文本）。
CharArrayReader/CharArrayWriter：以内存中的字符数组为数据源/目的地，用于内存中文本操作。
PrintWriter：打印字符流，可直接打印字符串、基本数据类型，常用作控制台输出或文件写入（替代System.out.println）。
四、缓冲流（提升效率，实战必备）
缓冲流（BufferedStream）是“装饰器模式”的典型应用，本身不直接操作数据源，而是包装字节流/字符流，通过“缓冲区”提升IO效率（减少与磁盘/外部设备的交互次数），核心分为缓冲字节流和缓冲字符流，必须掌握。
1. 缓冲字节流
包装字节流，核心类：BufferedInputStream、BufferedOutputStream，构造方法需传入对应的字节流对象。
// 示例：缓冲字节流读取文件（效率比FileInputStream高）
try (
    InputStream is = new FileInputStream("test.txt");
    // 包装字节流，默认缓冲区大小8192字节，可手动指定
    BufferedInputStream bis = new BufferedInputStream(is)
) {
    byte[] buffer = new byte[1024];
    int len;
    while ((len = bis.read(buffer)) != -1) {
        System.out.print(new String(buffer, 0, len, StandardCharsets.UTF_8));
    }
} catch (IOException e) {
    e.printStackTrace();
}
2. 缓冲字符流（最常用）
包装字符流，核心类：BufferedReader、BufferedWriter，新增实用方法，效率大幅提升，是文本操作的首选。
// 示例1：BufferedReader读取文本（新增readLine()方法，读取一行）
try (
    Reader reader = new InputStreamReader(new FileInputStream("test.txt"), StandardCharsets.UTF_8);
    BufferedReader br = new BufferedReader(reader)
) {
    String line;
    // 逐行读取，line为null表示读取完毕
    while ((line = br.readLine()) != null) {
        System.out.println(line);
    }
} catch (IOException e) {
    e.printStackTrace();
}
// 示例2：BufferedWriter写入文本（新增newLine()方法，换行）
try (
    Writer writer = new OutputStreamWriter(new FileOutputStream("test.txt", true), StandardCharsets.UTF_8);
    BufferedWriter bw = new BufferedWriter(writer)
) {
    bw.write("缓冲字符流示例");
    bw.newLine(); // 换行（跨平台兼容，比"\n"更规范）
    bw.write("逐行写入文本");
    bw.flush(); // 必须刷新
} catch (IOException e) {
    e.printStackTrace();
}
关键优势：BufferedReader的readLine()方法的逐行读取，BufferedWriter的newLine()方法的跨平台换行，是日常文本处理的核心方法。
五、其他常用IO流（实战补充）
除了上述核心流，补充2个实战中常用的IO流，贴合实际开发场景：
1. 打印流（PrintStream/PrintWriter）
用于便捷打印数据，可打印字符串、基本数据类型，无需手动转换，常用作控制台输出或文件写入。
// 示例：PrintWriter写入文件（便捷高效）
try (PrintWriter pw = new PrintWriter("test.txt", StandardCharsets.UTF_8, true)) {
    pw.println("打印流示例");
    pw.println(123); // 直接打印int类型
    pw.println(true); // 直接打印boolean类型
    // 无需手动flush，关闭时自动刷新
} catch (IOException e) {
    e.printStackTrace();
}
2. 转换流（InputStreamReader/OutputStreamWriter）
核心作用：将字节流转换为字符流，解决字符流的编码问题（手动指定编码格式），是字符流与字节流的桥梁，前文已在字符流示例中使用，此处重点强调其作用。
关键：所有字符流底层都依赖转换流，将字节流转换为字符流，再进行编码/解码，避免中文乱码的核心就是通过转换流指定编码格式（UTF-8）。
六、IO实战常见场景（学以致用）
结合核心知识点，整理3个实战中最常见的IO场景，建议动手敲一遍代码，强化记忆：
场景1：文本文件的读取与写入（最常用）
需求：读取test1.txt中的内容，过滤掉空行，写入test2.txt中。
try (
    // 读取流（缓冲字符流+转换流，指定编码）
    BufferedReader br = new BufferedReader(
        new InputStreamReader(new FileInputStream("test1.txt"), StandardCharsets.UTF_8)
    );
    // 写入流（缓冲字符流+转换流，指定编码，追加写入）
    BufferedWriter bw = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream("test2.txt", true), StandardCharsets.UTF_8)
    )
) {
    String line;
    while ((line = br.readLine()) != null) {
        // 过滤空行（trim()去除前后空格，判断是否为空）
        if (!line.trim().isEmpty()) {
            bw.write(line);
            bw.newLine(); // 换行
        }
    }
    bw.flush();
} catch (IOException e) {
    e.printStackTrace();
}
场景2：字节流复制文件（适配所有文件类型）
需求：将一张图片（或视频、音频）从D盘复制到E盘（字节流可处理所有类型文件）。
// 复制文件（字节缓冲流，提升效率）
try (
    BufferedInputStream bis = new BufferedInputStream(new FileInputStream("D:\\test.jpg"));
    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("E:\\test.jpg"))
) {
    byte[] buffer = new byte[1024 * 8]; // 8KB缓冲区，效率更高
    int len;
    while ((len = bis.read(buffer)) != -1) {
        bos.write(buffer, 0, len); // 写入实际读取的字节数
    }
    bos.flush();
} catch (IOException e) {
    e.printStackTrace();
}
需求：从键盘接收用户输入的字符串，打印到屏幕上，输入“exit”退出。
// 键盘输入（BufferedReader包装System.in，System.in是字节流）
try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
    String input;
    while (true) {
        System.out.print("请输入内容（输入exit退出）：");
        input = br.readLine();
        if ("exit".equals(input)) {
            break;
        }
        System.out.println("你输入的内容：" + input);
    }
} catch (IOException e) {
    e.printStackTrace();
}
七、学习技巧与避坑要点（高效避弯）
1. 高效学习技巧
先记分类，再记API：先分清“字节流/字符流”“输入流/输出流”，记住核心父类，再逐个学习常用子类，避免杂乱无章。
代码驱动学习：每学一种流，写一个简单demo（如读取文件、写入文件），直观感受流的使用方式，比死记方法更高效。
对比记忆：对比字节流与字符流的区别、缓冲流与普通流的区别，重点记住“字符流需刷新、字节流可不用”“缓冲流提升效率”。
2. 常见易错点（重点避开）
中文乱码：字符流未指定编码格式，或编码格式不统一（如读取用GBK，写入用UTF-8），解决方案：始终用转换流指定UTF-8编码。
流未关闭：手动关闭流时遗漏，导致资源泄露，解决方案：优先使用try-with-resources语法，自动关闭流。
字符流未刷新：写入文本后未调用flush()，导致数据未写入目的地，解决方案：字符流写入后必须调用flush()（或关闭流）。
缓冲区使用不当：读取时未判断实际读取的字节数（len），直接使用缓冲区，导致末尾出现乱码，解决方案：写入时指定“0到len”的范围。
八、实战建议
学习Java IO，核心是“多练、多用”，结合之前提到的计算机学习方法，重点做好2件事：
基础阶段：每天练1个简单demo，比如用字节流读文件、用字符流写文件、用缓冲流提升效率，熟悉核心API的用法。
进阶阶段：结合小场景练习，比如文件复制、文本过滤、键盘交互，尝试组合使用不同的流（如缓冲流+转换流），掌握装饰者模式的思想，理解流的包装逻辑。
IO是Java开发的基础，无论是文件操作、网络通信，还是框架中的数据交互，都离不开IO流，掌握好IO知识点，能为后续学习Java高级特性（如NIO、网络编程）打下坚实基础。

