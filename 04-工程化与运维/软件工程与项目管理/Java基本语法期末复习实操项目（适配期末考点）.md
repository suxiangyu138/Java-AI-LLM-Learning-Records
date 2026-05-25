Java基本语法期末复习实操项目（适配期末考点）
项目说明
本项目以“学生信息管理系统（控制台版）”为载体，全面覆盖Java基本语法期末核心考点，所有操作均贴合期末考题难度，每一步对应考点解析、代码实现、运行验证，帮你边敲代码边巩固语法，完成项目即可掌握90%以上Java基本语法期末高频考点。
适用场景：Java期末复习（本科/专科，基础语法阶段），无需复杂开发环境，仅需JDK（1.8及以上版本）+ 文本编辑器（Notepad++、IDEA等）即可完成，每一步均有详细代码、注释及考点标注，新手可直接跟着实操，兼顾基础巩固与期末应试。
项目核心目标：通过完成一个可运行、功能完整的控制台项目，熟练掌握Java基本语法，能独立编写简单Java程序，应对期末选择题、填空题、编程题等各类题型。
前期准备
1. 环境要求
    JDK安装：安装JDK 1.8或11版本（推荐1.8，兼容性强，贴合期末教学场景），配置环境变量（PATH、CLASSPATH），确保cmd命令行输入“java -version”“javac -version”能正常显示版本信息（环境变量配置是期末选择题常考考点）。
    开发工具：文本编辑器（Notepad++、Sublime Text）或IDE（IntelliJ IDEA、Eclipse），推荐新手用IDEA（可视化操作便捷，可快速排查语法错误，减少调试时间）。
    核心前提：了解Java基本概念（面向对象初步、JVM/JRE/JDK区别），本项目会从基础语法入手，逐步递进，针对性强化每一个考点。
2. 期末高频考点梳理（项目同步覆盖）
    Java基本语法期末高频考点（项目每一步对应以下考点，做完可逐一核对掌握情况）：
    基础入门：JDK/JRE/JVM的区别、Java程序的运行流程（编写-编译-运行）、main方法的格式（期末填空题必考）。
    数据类型：基本数据类型（8种）、引用数据类型（String、数组）、类型转换（自动转换、强制转换，期末选择题常考）。
    变量与常量：变量的定义、初始化、作用域（局部变量、成员变量）、常量的定义（final关键字）。
    运算符：算术运算符、赋值运算符、关系运算符、逻辑运算符、三元运算符（期末选择题、编程题均常考）。
    流程控制：分支结构（if-else、switch-case）、循环结构（for、while、do-while）、break/continue关键字的使用（期末编程题重点）。
    数组：数组的定义、初始化（静态初始化、动态初始化）、数组的遍历、数组的常见操作（求最值、排序、查找，期末编程题高频）。
    方法：方法的定义、调用、参数传递（值传递）、方法重载（期末大题常考）、无参/有参方法、有返回值/无返回值方法。
    面向对象基础：类的定义、对象的创建与使用、成员变量与成员方法、构造方法（无参/有参构造，期末大题重点）。
    异常处理：try-catch-finally异常捕获机制、常见异常类型（NullPointerException、ArrayIndexOutOfBoundsException）（期末选择题常考）。
    其他考点：注释（单行、多行、文档注释）、Scanner输入、String类的常用方法（拼接、截取、判断，期末高频）。
    项目实操步骤（核心环节，逐一步骤实操，贴合考点）
    项目整体结构：采用面向对象思想，创建3个核心类（Student类：封装学生信息；StudentManager类：实现学生信息的增删改查功能；Test类：主程序入口，运行整个项目），逐步实现功能，每一步都嵌入对应考点，边敲代码边理解考点。
    步骤1：环境测试（考点：Java程序运行流程、main方法格式）
    操作目的
    掌握Java程序的编写、编译、运行流程，牢记main方法的标准格式（期末填空题必考），验证JDK环境是否配置成功。
    代码实现（新建Test01.java文件，直接复制编写）
    // 考点：注释（单行注释、多行注释）、main方法格式、System.out.println输出
    // 单行注释：用于注释单行代码，期末常考注释的作用（提高代码可读性）
    /*
     * 多行注释：用于注释多行代码，可用于描述类、方法的功能
     * 本类用于测试Java环境是否正常，以及main方法的基本格式
     */
    public class Test01 {
    // main方法：Java程序的入口方法，格式固定（期末填空题必考）
    // 考点：main方法的参数（String[] args）、返回值（void）、修饰符（public static）
    public static void main(String[] args) {
        // 输出语句：打印内容到控制台，期末编程题基础
        System.out.println("Java环境测试成功！");
        System.out.println("Java基本语法期末复习项目启动...");
    }
    }
    实操验证（关键步骤，必做）
    编写代码：用文本编辑器或IDEA新建Test01.java文件，复制上述代码（注意类名与文件名一致，否则编译报错，期末常考错误点）。
    编译：打开cmd命令行，进入代码所在文件夹，输入“javac Test01.java”，若没有报错，说明编译成功，会生成Test01.class字节码文件（考点：Java编译过程——将.java源文件编译为.class字节码文件）。
    运行：输入“java Test01”，若控制台输出“Java环境测试成功！Java基本语法期末复习项目启动...”，说明环境配置正常，main方法运行正常。
    考点总结
    1. main方法标准格式：public static void main(String[] args)，缺一不可，期末填空题常考参数部分（String[] args）；2. Java程序运行流程：编写（.java）→ 编译（javac命令，生成.class）→ 运行（java命令，执行.class）；3. 注释的分类及作用，单行//、多行/* */，文档注释/** */（后续讲解）。
    步骤2：定义学生类（考点：面向对象基础、类与对象、成员变量、构造方法）
    操作目的
    掌握类的定义、成员变量的声明、构造方法（无参/有参）的编写、setter/getter方法的使用（期末大题常考：定义一个类，包含成员变量、构造方法、get/set方法）。
    代码实现（新建Student.java文件）
    // 考点：类的定义（public class 类名）、成员变量、构造方法、setter/getter方法
    public class Student {
    // 成员变量：定义学生的属性（学号、姓名、年龄、班级）
    // 考点：变量的定义、数据类型（String、int）、访问修饰符（默认访问修饰符）
    String studentNo;  // 学号（字符串类型）
    String studentName; // 姓名（字符串类型）
    int studentAge;    // 年龄（int类型，基本数据类型）
    String studentClass; // 班级（字符串类型）
    // 1. 无参构造方法（默认存在，若定义了有参构造，无参构造需手动编写，期末常考）
    // 考点：构造方法的格式（与类名相同、无返回值、可无参）
    public Student() {
        // 无参构造，可用于创建空对象
    }
    // 2. 有参构造方法（用于创建对象时直接赋值，期末大题重点）
    // 考点：构造方法的重载（方法名相同，参数列表不同）
    public Student(String studentNo, String studentName, int studentAge, String studentClass) {
        // this关键字：区分成员变量和局部变量（期末常考this的作用）
        this.studentNo = studentNo;
        this.studentName = studentName;
        this.studentAge = studentAge;
        this.studentClass = studentClass;
    }
    // 3. setter方法：用于设置成员变量的值（封装思想，期末常考封装的作用）
    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
    public void setStudentAge(int studentAge) {
        // 考点：逻辑判断（if语句）、数据合法性校验
        if (studentAge > 0 && studentAge <= 100) {
            this.studentAge = studentAge;
        } else {
            System.out.println("年龄输入不合法！");
        }
    }
    public void setStudentClass(String studentClass) {
        this.studentClass = studentClass;
    }
    // 4. getter方法：用于获取成员变量的值
    public String getStudentNo() {
        return studentNo;
    }
    public String getStudentName() {
        return studentName;
    }
    public int getStudentAge() {
        return studentAge;
    }
    public String getStudentClass() {
        return studentClass;
    }
    // 成员方法：显示学生信息（自定义方法，无参有返回值）
    public String showInfo() {
        // 考点：String字符串拼接（+号拼接，期末常考）
        return "学号：" + studentNo + "，姓名：" + studentName + "，年龄：" + studentAge + "，班级：" + studentClass;
    }
    }
    实操验证
    1. 编写代码：新建Student.java文件，复制上述代码，确保类名与文件名一致；2. 编译：cmd输入“javac Student.java”，验证是否编译成功（无报错即正常）；3. 暂不运行，后续步骤会创建该类的对象并使用。
    考点总结
    1. 类的定义格式：public class 类名 { 成员变量; 构造方法; 成员方法; }；2. 构造方法：与类名相同，无返回值，用于创建对象，无参构造默认存在，有参构造需手动编写；3. 方法重载：同一类中，方法名相同，参数列表（参数个数、类型、顺序）不同，如无参和有参构造；4. this关键字：区分成员变量和局部变量；5. 封装思想：通过setter/getter方法操作成员变量，隐藏内部细节，提高代码安全性。
    步骤3：实现学生信息管理功能（考点：方法、流程控制、数组、Scanner输入）
    操作目的
    整合多个考点，实现学生信息的增删改查功能，重点掌握方法调用、数组操作、流程控制（分支、循环）、Scanner输入、异常处理，这些都是期末编程题的核心考点。
    代码实现（新建StudentManager.java文件）
    // 考点：类的定义、方法定义与调用、数组、流程控制、Scanner输入、异常处理
    import java.util.Scanner; // 导入Scanner类，用于控制台输入（期末常考导入语句）
    public class StudentManager {
    // 成员变量：定义一个Student类型的数组，用于存储所有学生信息
    // 考点：数组的定义（引用数据类型数组）、动态初始化（指定长度）
    private Student[] students = new Student[100]; // 假设最多存储100个学生
    private int count = 0; // 记录当前学生数量（局部变量？不，是成员变量，作用域是整个类）
    // 1. 方法1：添加学生信息（无返回值，有参方法）
    public void addStudent() {
        Scanner sc = new Scanner(System.in); // 创建Scanner对象，用于输入
        try { // 考点：异常处理（try-catch），捕获输入异常
            System.out.println("请输入学生学号：");
            String no = sc.next(); // 接收字符串输入
            System.out.println("请输入学生姓名：");
            String name = sc.next();
            System.out.println("请输入学生年龄：");
            int age = sc.nextInt(); // 接收int类型输入，可能出现输入异常（如输入字母）
            System.out.println("请输入学生班级：");
            String className = sc.next();
            // 考点：对象的创建（通过有参构造方法创建对象）
            Student student = new Student(no, name, age, className);
            // 将学生对象存入数组
            students[count] = student;
            count++; // 学生数量自增
            System.out.println("学生信息添加成功！");
        } catch (Exception e) { // 捕获所有异常（如输入类型不匹配）
            System.out.println("输入错误，请输入正确的格式！");
        }
    }
    // 2. 方法2：查询所有学生信息（无参，无返回值）
    public void queryAllStudent() {
        // 考点：循环结构（for循环）、数组遍历、条件判断（if-else）
        if (count == 0) { // 没有学生信息
            System.out.println("当前没有学生信息！");
            return; // 结束方法
        }
        System.out.println("-------------------所有学生信息-------------------");
        // 遍历数组，输出所有学生信息
        for (int i = 0; i < count; i++) {
            // 调用Student类的showInfo()方法，获取学生信息
            System.out.println((i + 1) + "." + students[i].showInfo());
        }
        System.out.println("--------------------------------------------------");
    }
    // 3. 方法3：根据学号查询单个学生信息（有参，无返回值）
    public void queryStudentByNo(String studentNo) {
        // 考点：循环结构（while循环）、关系运算符、逻辑运算符
        boolean flag = false; // 标记是否找到学生
        int index = 0; // 记录学生在数组中的索引
        // while循环遍历数组
        int i = 0;
        while (i < count) {
            // 考点：String类的equals()方法（判断字符串是否相等，期末常考，注意不能用==）
            if (students[i].getStudentNo().equals(studentNo)) {
                flag = true;
                index = i;
                break; // 找到后跳出循环，考点：break关键字的使用
            }
            i++;
        }
        // 条件判断，输出结果
        if (flag) {
            System.out.println("找到该学生，信息如下：");
            System.out.println(students[index].showInfo());
        } else {
            System.out.println("未找到学号为" + studentNo + "的学生！");
        }
    }
    // 4. 方法4：根据学号修改学生信息（有参，无返回值）
    public void updateStudentByNo(String studentNo) {
        Scanner sc = new Scanner(System.in);
        boolean flag = false;
        int index = 0;
        // 遍历数组，找到对应学号的学生
        for (int i = 0; i < count; i++) {
            if (students[i].getStudentNo().equals(studentNo)) {
                flag = true;
                index = i;
                break;
            }
        }
        if (!flag) {
            System.out.println("未找到学号为" + studentNo + "的学生！");
            return;
        }
        // 找到学生，修改信息（考点：setter方法的调用、分支结构switch-case）
        System.out.println("请选择要修改的信息：");
        System.out.println("1. 修改姓名  2. 修改年龄  3. 修改班级  4. 取消修改");
        int choice = sc.nextInt();
        switch (choice) { // 考点：switch-case分支结构，期末常考case穿透问题
            case 1:
                System.out.println("请输入新的姓名：");
                String newName = sc.next();
                students[index].setStudentName(newName);
                break;
            case 2:
                System.out.println("请输入新的年龄：");
                int newAge = sc.nextInt();
                students[index].setStudentAge(newAge);
                break;
            case 3:
                System.out.println("请输入新的班级：");
                String newClass = sc.next();
                students[index].setStudentClass(newClass);
                break;
            case 4:
                System.out.println("已取消修改！");
                return;
            default:
                System.out.println("输入错误，无此选项！");
        }
        System.out.println("学生信息修改成功！");
    }
    // 5. 方法5：根据学号删除学生信息（有参，无返回值）
    public void deleteStudentByNo(String studentNo) {
        boolean flag = false;
        int index = 0;
        // 找到要删除的学生索引
        for (int i = 0; i < count; i++) {
            if (students[i].getStudentNo().equals(studentNo)) {
                flag = true;
                index = i;
                break;
            }
        }
        if (!flag) {
            System.out.println("未找到学号为" + studentNo + "的学生！");
            return;
        }
        // 考点：数组元素删除（通过移位实现）、循环结构（for循环）
        for (int i = index; i < count - 1; i++) {
            students[i] = students[i + 1]; // 后面的元素向前移位，覆盖要删除的元素
        }
        students[count - 1] = null; // 最后一个元素置为null，释放内存
        count--; // 学生数量减少
        System.out.println("学生信息删除成功！");
    }
    // 6. 方法6：求学生年龄的最大值和最小值（无参，有返回值，返回String类型）
    public String getAgeMaxAndMin() {
        if (count == 0) {
            return "当前没有学生信息，无法计算年龄最值！";
        }
        // 考点：变量初始化、循环遍历、关系运算符、三元运算符
        int maxAge = students[0].getStudentAge();
        int minAge = students[0].getStudentAge();
        for (int i = 1; i < count; i++) {
            // 三元运算符：判断当前学生年龄是否大于maxAge，是则更新maxAge，否则不变
            maxAge = students[i].getStudentAge() > maxAge ? students[i].getStudentAge() : maxAge;
            minAge = students[i].getStudentAge() < minAge ? students[i].getStudentAge() : minAge;
        }
        return "学生年龄最大值：" + maxAge + "岁，最小值：" + minAge + "岁";
    }
    }
    实操验证
    1. 编写代码：新建StudentManager.java文件，复制上述代码，注意导入Scanner类（import语句不能遗漏，否则编译报错）；2. 编译：cmd输入“javac StudentManager.java”，验证是否编译成功；3. 暂不运行，后续步骤编写主程序，调用该类的方法。
    考点总结
    1. Scanner输入：导入java.util.Scanner，创建对象，next()接收字符串，nextInt()接收int类型，注意输入异常处理；2. 流程控制：for循环、while循环用于遍历，if-else、switch-case用于分支判断，break用于跳出循环；3. 数组操作：数组遍历、元素移位删除，注意数组的动态初始化；4. 方法调用：调用自定义方法、调用其他类的方法（如Student类的showInfo()、setter/getter方法）；5. 异常处理：try-catch捕获输入异常，避免程序崩溃；6. String类常用方法：equals()判断字符串相等，期末常考（区别于==）；7. 三元运算符：简化if-else判断，格式：条件 ? 表达式1 : 表达式2。
    步骤4：编写主程序（考点：main方法、方法调用、循环菜单、数据类型转换）
    操作目的
    将前面编写的类整合起来，编写主程序入口，实现控制台菜单交互，掌握方法的调用、循环菜单的实现、数据类型转换，完成整个项目的运行，模拟期末编程题的完整程序编写。
    代码实现（新建Main.java文件，主程序入口）
    // 考点：main方法、方法调用、循环结构（do-while）、数据类型转换、Scanner输入
    import java.util.Scanner;
    public class Main {
    public static void main(String[] args) {
        // 创建StudentManager对象，调用其方法
        StudentManager manager = new StudentManager();
        Scanner sc = new Scanner(System.in);
        boolean isRunning = true; // 控制循环菜单是否继续运行
        // 考点：do-while循环（先执行一次，再判断条件，适合菜单场景，期末常考）
        do {
            // 控制台菜单（String拼接）
            System.out.println("\n===================Java学生信息管理系统（期末复习版）===================");
            System.out.println("1. 添加学生信息");
            System.out.println("2. 查询所有学生信息");
            System.out.println("3. 根据学号查询学生信息");
            System.out.println("4. 根据学号修改学生信息");
            System.out.println("5. 根据学号删除学生信息");
            System.out.println("6. 查询学生年龄最值");
            System.out.println("7. 退出系统");
            System.out.println("======================================================================");
            System.out.println("请输入您的选择（1-7）：");
            try {
                // 接收用户输入的选择（int类型）
                int choice = sc.nextInt();
                // 考点：switch-case分支结构，调用对应方法
                switch (choice) {
                    case 1:
                        manager.addStudent(); // 调用添加学生方法
                        break;
                    case 2:
                        manager.queryAllStudent(); // 调用查询所有学生方法
                        break;
                    case 3:
                        System.out.println("请输入要查询的学生学号：");
                        String queryNo = sc.next();
                        manager.queryStudentByNo(queryNo); // 调用按学号查询方法
                        break;
                    case 4:
                        System.out.println("请输入要修改的学生学号：");
                        String updateNo = sc.next();
                        manager.updateStudentByNo(updateNo); // 调用按学号修改方法
                        break;
                    case 5:
                        System.out.println("请输入要删除的学生学号：");
                        String deleteNo = sc.next();
                        manager.deleteStudentByNo(deleteNo); // 调用按学号删除方法
                        break;
                    case 6:
                        String ageResult = manager.getAgeMaxAndMin(); // 调用查询年龄最值方法
                        System.out.println(ageResult);
                        break;
                    case 7:
                        System.out.println("感谢使用，退出系统！");
                        isRunning = false; // 终止循环
                        break;
                    default:
                        System.out.println("输入错误，请输入1-7之间的数字！");
                }
            } catch (Exception e) {
                System.out.println("输入错误，请输入正确的数字！");
                sc.next(); // 清空输入缓冲区，避免死循环
            }
        } while (isRunning); // 循环条件：isRunning为true时继续运行
        sc.close(); // 关闭Scanner，释放资源
    }
    }
    实操验证（核心步骤，必做）
    编写代码：新建Main.java文件，复制上述代码，确保类名与文件名一致；
    编译：cmd进入代码所在文件夹，输入“javac Main.java Student.java StudentManager.java”（同时编译三个类，因为类之间相互依赖）；
    运行：输入“java Main”，控制台会显示菜单，按照菜单提示逐一测试所有功能：
    测试添加学生：选择1，输入学号、姓名、年龄、班级，提示“添加成功”；
    测试查询所有：选择2，查看添加的学生信息；
    测试按学号查询/修改/删除：输入对应学号，验证功能是否正常；
    测试年龄最值：添加多个学生后，选择6，查看年龄最大值和最小值；
    测试异常输入：输入非数字（如字母），验证异常处理是否生效，程序是否崩溃；
    测试退出系统：选择7，正常退出程序。
    调试修改：若出现报错，根据控制台提示修改代码（如类名与文件名不一致、缺少import语句、语法错误等），确保所有功能正常运行。
    考点总结
    1. do-while循环：先执行循环体，再判断条件，适合菜单交互场景，期末常考其与while循环的区别；2. 方法调用：通过对象调用成员方法，注意类之间的依赖关系（需先编译依赖的类）；3. 数据类型转换：无明显的强制转换，但需注意输入类型的匹配（如输入字母时的异常处理）；4. 循环控制：通过isRunning变量控制循环的启动与终止；5. 资源释放：关闭Scanner对象，养成良好的编程习惯。
    步骤5：补充考点强化（期末易错点、重点题型）
    操作目的
    针对期末常考的易错点、重点题型，补充代码练习，强化知识点记忆，避免考试中踩坑。
    补充练习1：类型转换（期末选择题常考）
    // 新建Test02.java，测试类型转换
    public class Test02 {
    public static void main(String[] args) {
        // 考点：基本数据类型的自动转换（小范围→大范围）
        byte b = 10;
        short s = b; // 自动转换：byte→short
        int i = s;   // 自动转换：short→int
        long l = i;  // 自动转换：int→long
        float f = l; // 自动转换：long→float
        double d = f;// 自动转换：float→double
        System.out.println("自动转换结果：" + d);
        // 考点：强制转换（大范围→小范围，需加强制转换符，可能丢失精度，期末常考易错点）
        int num1 = 100;
        byte num2 = (byte) num1; // 强制转换：int→byte，无精度丢失
        System.out.println("强制转换（无精度丢失）：" + num2);
        int num3 = 200;
        byte num4 = (byte) num3; // 强制转换：int→byte，精度丢失（byte范围-128~127）
        System.out.println("强制转换（精度丢失）：" + num4);
        // 考点：String与int的转换（期末编程题常考）
        String str = "123";
        int num5 = Integer.parseInt(str); // String→int
        System.out.println("String转int：" + (num5 + 10));
        int num6 = 456;
        String str2 = String.valueOf(num6); // int→String
        System.out.println("int转String：" + (str2 + "789"));
    }
    }
    补充练习2：方法重载（期末大题常考）
    // 新建Test03.java，测试方法重载
    public class Test03 {
    // 方法1：无参方法
    public static void show() {
        System.out.println("无参方法");
    }
    // 方法2：有一个int参数，与方法1重载（参数个数不同）
    public static void show(int a) {
        System.out.println("有一个int参数：" + a);
    }
    // 方法3：有一个String参数，与方法1、2重载（参数类型不同）
    public static void show(String str) {
        System.out.println("有一个String参数：" + str);
    }
    // 方法4：有两个int参数，与其他方法重载（参数个数不同）
    public static void show(int a, int b) {
        System.out.println("有两个int参数：" + (a + b));
    }
    public static void main(String[] args) {
        // 调用不同的重载方法，根据参数自动匹配
        show();
        show(10);
        show("Java");
        show(20, 30);
        // 考点：方法重载的判断标准（方法名相同，参数列表不同，与返回值、访问修饰符无关）
    }
    }
    补充练习3：数组排序（期末编程题高频）
    // 新建Test04.java，测试数组冒泡排序（期末常考排序算法）
    public class Test04 {
    public static void main(String[] args) {
        // 考点：数组静态初始化、冒泡排序、数组遍历
        int[] arr = {34, 12, 56, 78, 23, 90, 45};
        System.out.println("排序前的数组：");
        printArray(arr); // 调用打印数组方法
        // 冒泡排序核心逻辑（两两比较，交换位置）
        for (int i = 0; i < arr.length - 1; i++) { // 外层循环：控制排序轮数
            for (int j = 0; j < arr.length - 1 - i; j++) { // 内层循环：控制每轮比较次数
                if (arr[j] > arr[j + 1]) { // 若前一个元素大于后一个，交换位置
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                }
            }
        }
        System.out.println("排序后的数组（升序）：");
        printArray(arr);
    }
    // 自定义方法：打印数组（方法重载的补充，也可作为独立考点）
    public static void printArray(int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            if (i == arr.length - 1) {
                System.out.println(arr[i]);
            } else {
                System.out.print(arr[i] + ", ");
            }
        }
    }
    }
    实操验证
    分别编写上述3个补充练习的代码，编译并运行，观察运行结果，理解每个考点的核心逻辑，重点关注易错点（如强制转换的精度丢失、方法重载的判断标准、冒泡排序的循环逻辑）。
    步骤6：项目总结（期末复习重点）
    本项目覆盖了Java基本语法期末90%以上的高频考点，完成项目后，需重点掌握以下内容（期末备考核心，直接对接考题）：
    基础入门：牢记main方法格式、Java程序运行流程（编写-编译-运行）、JDK/JRE/JVM的区别（期末选择题常考）。
    数据类型与变量：8种基本数据类型的范围、引用数据类型（String、数组）、类型转换（自动/强制）、变量作用域（局部/成员）、常量定义（final）。
    运算符：算术运算符（注意自增自减的区别）、关系运算符、逻辑运算符（&&短路与、||短路或）、三元运算符（简化if-else）。
    流程控制：if-else、switch-case（注意case穿透）、for/while/do-while循环（区别与应用场景）、break/continue的使用。
    数组：定义、初始化（静态/动态）、遍历、常见操作（删除、求最值、排序），尤其是冒泡排序（期末编程题高频）。
    方法：定义、调用、参数传递（值传递）、方法重载（判断标准），能独立编写无参/有参、有返回值/无返回值方法。
    面向对象基础：类的定义、对象的创建与使用、构造方法（无参/有参）、setter/getter方法、this关键字、封装思想。
    其他重点：Scanner输入、异常处理（try-catch）、String类常用方法（equals、valueOf、parseInt）、注释的分类与作用。
    复习建议：1. 反复运行本项目的所有代码，逐行理解注释，修改代码（如修改循环条件、添加新功能），强化语法记忆；2. 重点练习补充练习中的易错点、重点题型（类型转换、方法重载、数组排序）；3. 模拟期末编程题，尝试独立编写类似“学生管理系统”的程序（如图书管理系统、成绩管理系统），检验复习效果；4. 整理错题本，记录编译报错、运行异常的原因（如类名与文件名不一致、输入类型不匹配、数组越界等），避免考试中踩坑。
    期末真题适配（补充）
    结合本项目，模拟2道期末编程题（贴合真题难度），可自行练习，检验复习效果：
    真题1（基础编程题，15分）
    题目：编写一个Java程序，从控制台输入3个整数，使用三元运算符求出这3个整数中的最大值，并输出结果。
    提示：用到Scanner输入、三元运算符、变量定义，可参考步骤3中的三元运算符使用，自行编写代码，参考答案如下：
    import java.util.Scanner;
    public class TestExam1 {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("请输入第一个整数：");
        int a = sc.nextInt();
        System.out.println("请输入第二个整数：");
        int b = sc.nextInt();
        System.out.println("请输入第三个整数：");
        int c = sc.nextInt();
        // 三元运算符嵌套，求最大值
        int max = a > b ? (a > c ? a : c) : (b > c ? b : c);
        System.out.println("三个整数中的最大值是：" + max);
        sc.close();
    }
    }
    真题2（综合编程题，20分）
    题目：编写一个Java程序，实现一个简单的成绩管理功能，要求：1. 从控制台输入5个学生的成绩（int类型），存入数组；2. 计算这5个学生的平均成绩（保留1位小数）；3. 找出成绩大于等于80分的学生个数；4. 将成绩按降序排序并输出。
    提示：用到数组初始化、Scanner输入、循环遍历、聚合运算（求平均）、条件判断、冒泡排序，可参考本项目步骤3、步骤5的补充练习，自行编写代码，参考答案如下：
    import java.util.Scanner;
    public class TestExam2 {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] scores = new int[5]; // 动态初始化数组，存储5个成绩
        int sum = 0; // 存储成绩总和
        int count = 0; // 存储80分及以上的学生个数
        // 输入5个成绩，存入数组
        for (int i = 0; i < scores.length; i++) {
            System.out.println("请输入第" + (i + 1) + "个学生的成绩：");
            scores[i] = sc.nextInt();
            sum += scores[i]; // 累加成绩
            if (scores[i] >= 80) {
                count++; // 统计80分及以上的个数
            }
        }
        // 计算平均成绩（保留1位小数，注意类型转换，避免整数除法）
        double avg = (double) sum / scores.length;
        System.out.println("5个学生的平均成绩：" + String.format("%.1f", avg));
        System.out.println("成绩大于等于80分的学生个数：" + count);
        // 冒泡排序（降序）
        for (int i = 0; i < scores.length - 1; i++) {
            for (int j = 0; j < scores.length - 1 - i; j++) {
                if (scores[j] < scores[j + 1]) { // 降序：前一个小于后一个，交换
                    int temp = scores[j];
                    scores[j] = scores[j + 1];
                    scores[j + 1] = temp;
                }
            }
        }
        // 输出排序后的成绩
        System.out.println("成绩按降序排序：");
        for (int i = 0; i < scores.length; i++) {
            System.out.print(scores[i] + " ");
        }
        sc.close();
    }
    }
