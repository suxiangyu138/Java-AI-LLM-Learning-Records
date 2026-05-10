03.31 01:05
Java设计模式期末复习项目——在线图书管理系统
一、项目概述
1.1 项目目标
本项目是一个简易在线图书管理系统，核心目标是帮助复习Java常用设计模式，通过“实际场景+模式应用”的方式，让每个设计模式的使用场景、实现逻辑、核心思想落地。项目整合单例模式、工厂模式、装饰器模式、观察者模式、策略模式、适配器模式、模板方法模式7种高频设计模式，覆盖创建型、结构型、行为型三大类，功能涵盖图书的增删改查、借阅、归还、消息通知、权限控制等，代码注释详细，结构清晰，可直接运行，适配期末复习、课程设计场景。
1.2 技术栈
核心语言：Java（JDK 8及以上，兼容主流版本）
开发工具：IntelliJ IDEA（推荐）、Eclipse
运行环境：JRE 8及以上（无需额外框架，纯Java原生实现，降低学习成本）
数据存储：ArrayList（内存模拟，无需数据库，专注设计模式实现）
核心设计模式：7种高频模式（覆盖期末常考考点）
1.3 项目结构
采用分层架构+设计模式拆分，包结构清晰，便于复习时定位每种模式的代码，结构如下：
com.bookmanager
├─ main                  // 主程序入口
│  └─ BookManagerMain.java  // 程序启动类
├─ model                 // 实体类（模型层）
│  ├─ Book.java          // 图书实体
│  ├─ User.java          // 用户实体（普通用户/管理员）
│  └─ BorrowRecord.java  // 借阅记录实体
├─ singleton             // 单例模式包（创建型）
│  └─ BookManagerSingleton.java  // 单例模式实现（系统唯一管理类）
├─ factory               // 工厂模式包（创建型）
│  ├─ UserFactory.java   // 工厂接口
│  ├─ NormalUserFactory.java  // 普通用户工厂
│  └─ AdminUserFactory.java   // 管理员工厂
├─ decorator             // 装饰器模式包（结构型）
│  ├─ BookService.java   // 图书服务接口
│  ├─ BasicBookService.java  // 基础图书服务（核心功能）
│  ├─ LogDecorator.java  // 日志装饰器（增强功能：记录操作日志）
│  └─ PermissionDecorator.java  // 权限装饰器（增强功能：权限校验）
├─ observer              // 观察者模式包（行为型）
│  ├─ Observer.java      // 观察者接口（通知接收者）
│  ├─ Subject.java       // 主题接口（通知发布者）
│  ├─ MessageObserver.java  // 消息观察者（接收借阅/归还通知）
│  └─ BookSubject.java   // 图书主题（发布图书状态变化通知）
├─ strategy              // 策略模式包（行为型）
│  ├─ BorrowStrategy.java  // 借阅策略接口
│  ├─ NormalBorrowStrategy.java  // 普通用户借阅策略（限借3本）
│  └─ AdminBorrowStrategy.java   // 管理员借阅策略（不限借）
├─ adapter               // 适配器模式包（结构型）
│  ├─ OldBookSystem.java  // 旧图书系统接口（待适配）
│  ├─ NewBookService.java  // 新系统接口（目标接口）
│  └─ BookSystemAdapter.java  // 适配器（适配旧系统到新系统）
├─ template              // 模板方法模式包（行为型）
│  ├─ BorrowTemplate.java  // 借阅模板接口（定义流程）
│  ├─ NormalBorrowTemplate.java  // 普通用户借阅模板
│  └─ AdminBorrowTemplate.java   // 管理员借阅模板
└─ util                  // 工具类
   └─ DateUtil.java      // 日期工具类（处理借阅/归还时间）
二、需求分析（设计模式应用依据）
2.1 核心功能需求
用户管理：支持普通用户、管理员两种角色，通过工厂模式创建不同角色用户
图书管理：实现图书的新增、删除、修改、查询（基础功能），通过装饰器模式增强日志、权限校验功能
借阅管理：不同角色有不同借阅规则（普通用户限借3本，管理员不限借），通过策略模式实现；借阅流程固定（校验-借阅-通知），通过模板方法模式规范流程
消息通知：图书借阅、归还时，自动通知相关观察者（如系统消息、用户通知），通过观察者模式实现
系统适配：兼容旧版图书系统接口，通过适配器模式实现新旧系统对接
单例管理：系统核心管理类（图书管理、用户管理）唯一实例，通过单例模式避免重复创建
2.2 设计模式对应场景（重点复习）
设计模式类型
设计模式名称
应用场景
核心作用
创建型
单例模式
图书管理核心类、用户管理核心类
保证类只有一个实例，避免资源浪费，统一管理
创建型
工厂模式
创建普通用户、管理员对象
解耦对象创建与使用，根据需求动态创建不同角色
结构型
装饰器模式
图书服务增强（日志记录、权限校验）
不修改原有代码，动态增强类的功能，灵活扩展
结构型
适配器模式
旧版图书系统与新版系统对接
解决接口不兼容问题，复用旧系统功能
行为型
观察者模式
图书借阅/归还消息通知
实现对象间的一对多依赖，一个对象变化自动通知其他对象
行为型
策略模式
不同角色的借阅规则
定义不同算法（规则），动态切换，解耦算法与使用
行为型
模板方法模式
图书借阅流程规范
定义固定流程骨架，将可变步骤延迟到子类实现
三、详细代码实现（按包拆分，注释详细）
3.1 实体类（model包）—— 基础模型，无设计模式，为后续功能提供数据支撑
3.1.1 Book.java（图书实体）
package com.bookmanager.model;
/**
 * 图书实体类
 * 存储图书的核心信息，为所有图书相关操作提供数据模型
 */
public class Book {
    // 图书ID（唯一标识）
    private String bookId;
    // 图书名称
    private String bookName;
    // 图书作者
    private String author;
    // 图书分类
    private String category;
    // 图书库存（0表示无库存，大于0表示可借阅）
    private int stock;
    // 图书状态（true：可借阅，false：已借出/不可借阅）
    private boolean isBorrowable;
    // 无参构造（用于反射、工厂创建）
    public Book() {}
    // 有参构造（用于快速创建图书对象）
    public Book(String bookId, String bookName, String author, String category, int stock, boolean isBorrowable) {
        this.bookId = bookId;
        this.bookName = bookName;
        this.author = author;
        this.category = category;
        this.stock = stock;
        this.isBorrowable = isBorrowable;
    }
    //  getter/setter方法（封装属性，保证数据安全性）
    public String getBookId() {
        return bookId;
    }
    public void setBookId(String bookId) {
        this.bookId = bookId;
    }
    public String getBookName() {
        return bookName;
    }
    public void setBookName(String bookName) {
        this.bookName = bookName;
    }
    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }
    public int getStock() {
        return stock;
    }
    public void setStock(int stock) {
        this.stock = stock;
        // 库存大于0时，图书可借阅；否则不可借阅
        this.isBorrowable = stock > 0;
    }
    public boolean isBorrowable() {
        return isBorrowable;
    }
    // 重写toString方法，便于打印图书信息
    @Override
    public String toString() {
        return "Book{" +
                "bookId='" + bookId + '\'' +
                ", bookName='" + bookName + '\'' +
                ", author='" + author + '\'' +
                ", category='" + category + '\'' +
                ", stock=" + stock +
                ", isBorrowable=" + isBorrowable +
                '}';
    }
}
3.1.2 User.java（用户实体）
package com.bookmanager.model;
/**
 * 用户实体类
 * 存储用户信息，区分普通用户和管理员角色
 */
public class User {
    // 用户ID（唯一标识）
    private String userId;
    // 用户名
    private String userName;
    // 用户密码（模拟，实际项目需加密）
    private String password;
    // 用户角色（normal：普通用户，admin：管理员）
    private String role;
    // 无参构造
    public User() {}
    // 有参构造
    public User(String userId, String userName, String password, String role) {
        this.userId = userId;
        this.userName = userName;
        this.password = password;
        this.role = role;
    }
    // getter/setter方法
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }
    // 重写toString方法
    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
3.1.3 BorrowRecord.java（借阅记录实体）
package com.bookmanager.model;
import com.bookmanager.util.DateUtil;
import java.util.Date;
/**
 * 借阅记录实体类
 * 存储图书借阅、归还的相关信息，用于追溯借阅记录
 */
public class BorrowRecord {
    // 记录ID
    private String recordId;
    // 借阅用户ID
    private String userId;
    // 借阅图书ID
    private String bookId;
    // 借阅时间
    private Date borrowTime;
    // 归还时间（null表示未归还）
    private Date returnTime;
    // 借阅状态（borrowed：已借出，returned：已归还）
    private String status;
    // 无参构造
    public BorrowRecord() {}
    // 有参构造（借阅时创建，归还时间默认为null，状态为已借出）
    public BorrowRecord(String recordId, String userId, String bookId) {
        this.recordId = recordId;
        this.userId = userId;
        this.bookId = bookId;
        this.borrowTime = new Date(); // 借阅时间为当前时间
        this.returnTime = null;
        this.status = "borrowed";
    }
    // getter/setter方法
    public String getRecordId() {
        return recordId;
    }
    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getBookId() {
        return bookId;
    }
    public void setBookId(String bookId) {
        this.bookId = bookId;
    }
    public Date getBorrowTime() {
        return borrowTime;
    }
    public void setBorrowTime(Date borrowTime) {
        this.borrowTime = borrowTime;
    }
    public Date getReturnTime() {
        return returnTime;
    }
    // 归还时设置归还时间，并修改状态
    public void setReturnTime(Date returnTime) {
        this.returnTime = returnTime;
        this.status = "returned";
    }
    public String getStatus() {
        return status;
    }
    // 重写toString方法，格式化时间显示
    @Override
    public String toString() {
        return "BorrowRecord{" +
                "recordId='" + recordId + '\'' +
                ", userId='" + userId + '\'' +
                ", bookId='" + bookId + '\'' +
                ", borrowTime=" + DateUtil.formatDate(borrowTime) +
                ", returnTime=" + (returnTime == null ? "未归还" : DateUtil.formatDate(returnTime)) +
                ", status='" + status + '\'' +
                '}';
    }
}
3.1.4 DateUtil.java（工具类）
package com.bookmanager.util;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * 日期工具类
 * 提供日期格式化功能，用于借阅记录的时间显示
 */
public class DateUtil {
    // 私有构造，禁止实例化（工具类无需创建对象）
    private DateUtil() {}
    // 日期格式化模板（年-月-日 时:分:秒）
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    /**
     * 格式化日期
     * @param date 待格式化的日期
     * @return 格式化后的日期字符串
     */
    public static String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return sdf.format(date);
    }
}
3.2 单例模式（singleton包）—— 创建型模式
3.2.1 模式说明（复习重点）
单例模式：保证一个类仅有一个实例，并提供一个访问它的全局访问点。 核心要点：私有构造器（禁止外部实例化）、静态私有实例、静态公共方法（返回实例）。 本项目使用「饿汉式单例」（线程安全，简单易实现，适合单线程/多线程场景，无延迟加载），用于系统核心管理类，避免重复创建造成资源浪费。
3.2.2 BookManagerSingleton.java（单例实现）
package com.bookmanager.singleton;
import com.bookmanager.model.Book;
import com.bookmanager.model.BorrowRecord;
import com.bookmanager.model.User;
import java.util.ArrayList;
import java.util.List;
/**
 * 图书管理单例类
 * 核心作用：统一管理图书、用户、借阅记录，保证系统中只有一个管理实例
 * 设计模式：饿汉式单例模式（类加载时创建实例，线程安全）
 */
public class BookManagerSingleton {
    // 1. 私有构造器：禁止外部通过new创建实例
    private BookManagerSingleton() {
        // 初始化数据（模拟系统初始数据，避免空指针）
        initData();
    }
    // 2. 静态私有实例：类加载时创建，唯一实例
    private static final BookManagerSingleton INSTANCE = new BookManagerSingleton();
    // 3. 静态公共方法：提供全局访问点，返回唯一实例
    public static BookManagerSingleton getInstance() {
        return INSTANCE;
    }
    // 管理的核心数据（内存模拟存储，替代数据库）
    private List<Book> bookList = new ArrayList<>(); // 图书列表
    private List<User> userList = new ArrayList<>(); // 用户列表
    private List<BorrowRecord> borrowRecordList = new ArrayList<>(); // 借阅记录列表
    /**
     * 初始化系统数据（模拟初始数据，方便测试）
     */
    private void initData() {
        // 初始化图书
        bookList.add(new Book("B001", "Java设计模式", "张三", "计算机", 5, true));
        bookList.add(new Book("B002", "SpringBoot实战", "李四", "计算机", 3, true));
        bookList.add(new Book("B003", "MySQL从入门到精通", "王五", "计算机", 0, false));
        // 初始化用户（普通用户+管理员）
        userList.add(new User("U001", "普通用户1", "123456", "normal"));
        userList.add(new User("U002", "管理员", "admin123", "admin"));
        // 初始化借阅记录（模拟已借出的图书）
        borrowRecordList.add(new BorrowRecord("R001", "U001", "B003"));
    }
    // ------------------- 图书相关方法（供外部调用）-------------------
    public List<Book> getBookList() {
        return bookList;
    }
    // 根据图书ID查询图书
    public Book getBookById(String bookId) {
        for (Book book : bookList) {
            if (book.getBookId().equals(bookId)) {
                return book;
            }
        }
        return null;
    }
    // 新增图书
    public boolean addBook(Book book) {
        // 校验图书ID是否已存在
        if (getBookById(book.getBookId()) != null) {
            return false;
        }
        bookList.add(book);
        return true;
    }
    // 删除图书（根据图书ID）
    public boolean deleteBook(String bookId) {
        Book book = getBookById(bookId);
        if (book == null) {
            return false;
        }
        bookList.remove(book);
        return true;
    }
    // ------------------- 用户相关方法（供外部调用）-------------------
    public List<User> getUserList() {
        return userList;
    }
    // 根据用户ID查询用户
    public User getUserById(String userId) {
        for (User user : userList) {
            if (user.getUserId().equals(userId)) {
                return user;
            }
        }
        return null;
    }
    // ------------------- 借阅记录相关方法（供外部调用）-------------------
    public List<BorrowRecord> getBorrowRecordList() {
        return borrowRecordList;
    }
    // 新增借阅记录
    public void addBorrowRecord(BorrowRecord record) {
        borrowRecordList.add(record);
    }
    // 根据图书ID查询借阅记录（判断图书是否已借出）
    public BorrowRecord getBorrowRecordByBookId(String bookId) {
        for (BorrowRecord record : borrowRecordList) {
            if (record.getBookId().equals(bookId) && "borrowed".equals(record.getStatus())) {
                return record;
            }
        }
        return null;
    }
}
3.3 工厂模式（factory包）—— 创建型模式
3.3.1 模式说明（复习重点）
工厂模式：定义一个创建对象的接口，让其子类决定实例化哪一个类，工厂方法使一个类的实例化延迟到其子类。 核心要点：工厂接口（定义创建对象的方法）、具体工厂（实现接口，创建具体对象）、产品（被创建的对象）。 本项目使用「简单工厂模式」的变体（接口+具体工厂），用于创建不同角色的用户（普通用户、管理员），解耦用户创建与使用。
3.3.2 代码实现
// 1. UserFactory.java（工厂接口）
package com.bookmanager.factory;
import com.bookmanager.model.User;
/**
 * 用户工厂接口
 * 定义创建用户的方法，由具体工厂实现
 */
public interface UserFactory {
    // 创建用户的方法（返回User对象）
    User createUser(String userId, String userName, String password);
}
// 2. NormalUserFactory.java（普通用户工厂）
package com.bookmanager.factory;
import com.bookmanager.model.User;
/**
 * 普通用户工厂
 * 实现UserFactory接口，创建普通用户对象（role为normal）
 */
public class NormalUserFactory implements UserFactory {
    @Override
    public User createUser(String userId, String userName, String password) {
        // 普通用户角色固定为normal
        return new User(userId, userName, password, "normal");
    }
}
// 3. AdminUserFactory.java（管理员工厂）
package com.bookmanager.factory;
import com.bookmanager.model.User;
/**
 * 管理员工厂
 * 实现UserFactory接口，创建管理员对象（role为admin）
 */
public class AdminUserFactory implements UserFactory {
    @Override
    public User createUser(String userId, String userName, String password) {
        // 管理员角色固定为admin
        return new User(userId, userName, password, "admin");
    }
}
3.4 装饰器模式（decorator包）—— 结构型模式
3.4.1 模式说明（复习重点）
装饰器模式：动态地给一个对象添加一些额外的职责。就增加功能来说，装饰器模式比生成子类更为灵活。 核心要点：抽象组件（定义核心功能接口）、具体组件（实现核心功能）、装饰器（继承抽象组件，持有具体组件引用，增强功能）、具体装饰器（实现装饰器的增强逻辑）。 本项目用于增强图书服务功能：基础图书服务（增删改查），通过装饰器添加「日志记录」和「权限校验」功能，不修改原有基础服务代码，实现功能扩展。
3.4.2 代码实现
// 1. BookService.java（抽象组件：图书服务接口）
package com.bookmanager.decorator;
import com.bookmanager.model.Book;
import com.bookmanager.model.User;
/**
 * 图书服务接口（抽象组件）
 * 定义图书服务的核心功能（增删改查）
 */
public interface BookService {
    // 新增图书（需要管理员权限）
    boolean addBook(User user, Book book);
    // 删除图书（需要管理员权限）
    boolean deleteBook(User user, String bookId);
    // 查询图书（所有用户可操作）
    Book queryBook(String bookId);
}
// 2. BasicBookService.java（具体组件：基础图书服务）
package com.bookmanager.decorator;
import com.bookmanager.model.Book;
import com.bookmanager.model.User;
import com.bookmanager.singleton.BookManagerSingleton;
/**
 * 基础图书服务（具体组件）
 * 实现BookService接口，提供图书增删改查的核心功能
 */
public class BasicBookService implements BookService {
    // 持有单例管理类实例，操作数据
    private BookManagerSingleton bookManager = BookManagerSingleton.getInstance();
    @Override
    public boolean addBook(User user, Book book) {
        // 核心逻辑：新增图书（权限校验由装饰器实现，此处只做核心操作）
        return bookManager.addBook(book);
    }
    @Override
    public boolean deleteBook(User user, String bookId) {
        // 核心逻辑：删除图书（权限校验由装饰器实现）
        return bookManager.deleteBook(bookId);
    }
    @Override
    public Book queryBook(String bookId) {
        // 核心逻辑：查询图书（无权限限制）
        return bookManager.getBookById(bookId);
    }
}
// 3. LogDecorator.java（具体装饰器：日志装饰器）
package com.bookmanager.decorator;
import com.bookmanager.model.Book;
import com.bookmanager.model.User;
import com.bookmanager.util.DateUtil;
import java.util.Date;
/**
 * 日志装饰器（具体装饰器）
 * 增强图书服务：记录用户操作日志（操作人、操作时间、操作内容）
 */
public class LogDecorator implements BookService {
    // 持有图书服务对象（被装饰的对象，可是基础服务或其他装饰器）
    private BookService bookService;
    // 构造器：传入被装饰的图书服务对象
    public LogDecorator(BookService bookService) {
        this.bookService = bookService;
    }
    @Override
    public boolean addBook(User user, Book book) {
        // 增强逻辑：记录日志
        System.out.println("[" + DateUtil.formatDate(new Date()) + "] 用户" + user.getUserName() + "（" + user.getUserId() + "）执行新增图书操作，图书信息：" + book);
        // 调用被装饰对象的核心方法
        return bookService.addBook(user, book);
    }
    @Override
    public boolean deleteBook(User user, String bookId) {
        // 增强逻辑：记录日志
        System.out.println("[" + DateUtil.formatDate(new Date()) + "] 用户" + user.getUserName() + "（" + user.getUserId() + "）执行删除图书操作，图书ID：" + bookId);
        // 调用被装饰对象的核心方法
        return bookService.deleteBook(user, bookId);
    }
    @Override
    public Book queryBook(String bookId) {
        // 增强逻辑：记录日志
        System.out.println("[" + DateUtil.formatDate(new Date()) + "] 执行查询图书操作，图书ID：" + bookId);
        // 调用被装饰对象的核心方法
        return bookService.queryBook(bookId);
    }
}
// 4. PermissionDecorator.java（具体装饰器：权限装饰器）
package com.bookmanager.decorator;
import com.bookmanager.model.Book;
import com.bookmanager.model.User;
/**
 * 权限装饰器（具体装饰器）
 * 增强图书服务：校验用户权限（新增、删除图书需要管理员权限）
 */
public class PermissionDecorator implements BookService {
    // 持有图书服务对象（被装饰的对象）
    private BookService bookService;
    // 构造器：传入被装饰的图书服务对象
    public PermissionDecorator(BookService bookService) {
        this.bookService = bookService;
    }
    @Override
    public boolean addBook(User user, Book book) {
        // 增强逻辑：权限校验（只有管理员能新增图书）
        if (!"admin".equals(user.getRole())) {
            System.out.println("权限不足！用户" + user.getUserName() + "不是管理员，无法新增图书。");
            return false;
        }
        // 权限通过，调用被装饰对象的核心方法
        return bookService.addBook(user, book);
    }
    @Override
    public boolean deleteBook(User user, String bookId) {
        // 增强逻辑：权限校验（只有管理员能删除图书）
        if (!"admin".equals(user.getRole())) {
            System.out.println("权限不足！用户" + user.getUserName() + "不是管理员，无法删除图书。");
            return false;
        }
        // 权限通过，调用被装饰对象的核心方法
        return bookService.deleteBook(user, bookId);
    }
    @Override
    public Book queryBook(String bookId) {
        // 查询图书无权限限制，直接调用核心方法
        return bookService.queryBook(bookId);
    }
}
3.5 观察者模式（observer包）—— 行为型模式
3.5.1 模式说明（复习重点）
观察者模式：定义对象间的一种一对多的依赖关系，当一个对象的状态发生改变时，所有依赖于它的对象都得到通知并被自动更新。 核心要点：主题（Subject，发布通知的对象）、观察者（Observer，接收通知的对象）、主题维护观察者列表，状态变化时遍历通知所有观察者。 本项目用于图书借阅/归还时的消息通知：图书（主题）状态变化（借出/归还）时，通知观察者（消息系统）发送通知。
3.5.2 代码实现
// 1. Observer.java（观察者接口）
package com.bookmanager.observer;
/**
 * 观察者接口
 * 定义观察者接收通知的方法
 */
public interface Observer {
    // 接收通知的方法（参数为通知内容）
    void update(String message);
}
// 2. Subject.java（主题接口）
package com.bookmanager.observer;
/**
 * 主题接口
 * 定义主题的核心方法：注册观察者、移除观察者、通知所有观察者
 */
public interface Subject {
    // 注册观察者（添加到观察者列表）
    void registerObserver(Observer observer);
    // 移除观察者（从观察者列表中删除）
    void removeObserver(Observer observer);
    // 通知所有观察者（发送消息）
    void notifyObservers(String message);
}
// 3. MessageObserver.java（具体观察者：消息观察者）
package com.bookmanager.observer;
/**
 * 消息观察者（具体观察者）
 * 接收图书状态变化通知，模拟发送系统消息
 */
public class MessageObserver implements Observer {
    @Override
    public void update(String message) {
        // 接收通知后的逻辑：模拟发送系统消息
        System.out.println("【系统消息】" + message);
    }
}
// 4. BookSubject.java（具体主题：图书主题）
package com.bookmanager.observer;
import com.bookmanager.model.Book;
import java.util.ArrayList;
import java.util.List;
/**
 * 图书主题（具体主题）
 * 维护观察者列表，图书状态变化时通知所有观察者
 */
public class BookSubject implements Subject {
    // 观察者列表（存储所有注册的观察者）
    private List<Observer> observerList = new ArrayList<>();
    // 当前图书（状态变化的主体）
    private Book book;
    // 设置当前图书（状态变化的图书）
    public void setBook(Book book) {
        this.book = book;
        // 图书状态变化，通知所有观察者
        notifyObservers(getMessage());
    }
    // 构建通知消息（根据图书状态生成消息）
    private String getMessage() {
        if (book == null) {
            return "图书信息异常";
        }
        return book.isBorrowable() ?
                "图书《" + book.getBookName() + "》（ID：" + book.getBookId() + "）已归还，可正常借阅！" :
                "图书《" + book.getBookName() + "》（ID：" + book.getBookId() + "）已借出，暂时无法借阅！";
    }
    @Override
    public void registerObserver(Observer observer) {
        // 注册观察者：添加到列表（避免重复注册）
        if (!observerList.contains(observer)) {
            observerList.add(observer);
        }
    }
    @Override
    public void removeObserver(Observer observer) {
        // 移除观察者：从列表中删除
        observerList.remove(observer);
    }
    @Override
    public void notifyObservers(String message) {
        // 通知所有观察者：遍历列表，调用每个观察者的update方法
        for (Observer observer : observerList) {
            observer.update(message);
        }
    }
}
3.6 策略模式（strategy包）—— 行为型模式
3.6.1 模式说明（复习重点）
策略模式：定义一系列算法，将每个算法封装起来，并使它们可以相互替换。策略模式让算法的变化独立于使用算法的客户。 核心要点：策略接口（定义算法规范）、具体策略（实现算法）、上下文（持有策略引用，调用策略执行算法）。 本项目用于不同角色的借阅规则：普通用户限借3本，管理员不限借，两种规则作为策略，可动态切换。
3.6.2 代码实现
// 1. BorrowStrategy.java（策略接口：借阅策略）
package com.bookmanager.strategy;
import com.bookmanager.model.User;
import com.bookmanager.singleton.BookManagerSingleton;
/**
 * 借阅策略接口
 * 定义借阅规则的算法规范（判断用户是否可借阅）
 */
public interface BorrowStrategy {
    // 判断用户是否可借阅图书（返回true：可借阅，false：不可借阅）
    boolean canBorrow(User user);
}
// 2. NormalBorrowStrategy.java（具体策略：普通用户借阅策略）
package com.bookmanager.strategy;
import com.bookmanager.model.BorrowRecord;
import com.bookmanager.model.User;
import com.bookmanager.singleton.BookManagerSingleton;
import java.util.List;
/**
 * 普通用户借阅策略（具体策略）
 * 规则：普通用户最多可借阅3本图书
 */
public class NormalBorrowStrategy implements BorrowStrategy {
    private BookManagerSingleton bookManager = BookManagerSingleton.getInstance();
    @Override
    public boolean canBorrow(User user) {
        // 1. 校验用户角色（必须是普通用户）
        if (!"normal".equals(user.getRole())) {
            return false;
        }
        // 2. 查询该用户当前已借出的图书数量
        List<BorrowRecord> recordList = bookManager.getBorrowRecordList();
        int borrowCount = 0;
        for (BorrowRecord record : recordList) {
            if (record.getUserId().equals(user.getUserId()) && "borrowed".equals(record.getStatus())) {
                borrowCount++;
            }
        }
        // 3. 规则：最多借3本
        if (borrowCount >= 3) {
            System.out.println("普通用户" + user.getUserName() + "已借阅" + borrowCount + "本图书，超过最大借阅数量（3本），无法继续借阅！");
            return false;
        }
        return true;
    }
}
// 3. AdminBorrowStrategy.java（具体策略：管理员借阅策略）
package com.bookmanager.strategy;
import com.bookmanager.model.User;
/**
 * 管理员借阅策略（具体策略）
 * 规则：管理员无借阅数量限制
 */
public class AdminBorrowStrategy implements BorrowStrategy {
    @Override
    public boolean canBorrow(User user) {
        // 规则：只要是管理员，就可借阅（无数量限制）
        if ("admin".equals(user.getRole())) {
            return true;
        }
        return false;
    }
}
// 4. BorrowContext.java（上下文：借阅上下文）
package com.bookmanager.strategy;
import com.bookmanager.model.User;
/**
 * 借阅上下文
 * 持有借阅策略引用，提供统一的借阅判断方法，动态切换策略
 */
public class BorrowContext {
    // 持有借阅策略（可动态切换）
    private BorrowStrategy borrowStrategy;
    // 设置借阅策略（动态切换策略）
    public void setBorrowStrategy(BorrowStrategy borrowStrategy) {
        this.borrowStrategy = borrowStrategy;
    }
    // 统一的借阅判断方法（调用策略的算法）
    public boolean canBorrow(User user) {
        if (borrowStrategy == null) {
            System.out.println("未设置借阅策略！");
            return false;
        }
        return borrowStrategy.canBorrow(user);
    }
}
3.7 适配器模式（adapter包）—— 结构型模式
3.7.1 模式说明（复习重点）
适配器模式：将一个类的接口转换成客户希望的另一个接口。适配器模式使得原本由于接口不兼容而不能一起工作的那些类可以一起工作。 核心要点：目标接口（客户期望的接口）、适配者（需要被适配的旧接口）、适配器（实现目标接口，持有适配者引用，将旧接口转换为目标接口）。 本项目用于新旧图书系统对接：旧系统接口与新系统接口不兼容，通过适配器将旧系统接口转换为新系统接口，复用旧系统功能。
3.7.2 代码实现
// 1. OldBookSystem.java（适配者：旧图书系统接口）
package com.bookmanager.adapter;
/**
 * 旧图书系统接口（适配者）
 * 假设旧系统的接口方法与新系统不一致，需要适配
 */
public class OldBookSystem {
    /**
     * 旧系统查询图书的方法（参数为图书名称，返回图书信息字符串）
     * 与新系统的queryBook（参数为图书ID，返回Book对象）接口不兼容
     */
    public String searchBookByName(String bookName) {
        // 模拟旧系统查询逻辑（返回字符串格式的图书信息）
        return "旧系统查询结果：图书名称=" + bookName + "，作者=未知，库存=10";
    }
    /**
     * 旧系统新增图书的方法（参数为图书名称、作者，无返回值）
     * 与新系统的addBook（参数为User、Book，返回boolean）接口不兼容
     */
    public void addOldBook(String bookName, String author) {
        // 模拟旧系统新增图书逻辑
        System.out.println("旧系统新增图书：" + bookName + "（作者：" + author + "）");
    }
}
// 2. NewBookService.java（目标接口：新系统接口）
package com.bookmanager.adapter;
import com.bookmanager.model.Book;
import com.bookmanager.model.User;
/**
 * 新系统接口（目标接口）
 * 客户期望的接口，与旧系统接口不兼容，需要适配器转换
 */
public interface NewBookService {
    // 新系统查询图书方法（参数：图书ID，返回：Book对象）
    Book queryBook(String bookId);
    // 新系统新增图书方法（参数：用户、图书，返回：是否成功）
    boolean addBook(User user, Book book);
}
// 3. BookSystemAdapter.java（适配器：适配旧系统到新系统）
package com.bookmanager.adapter;
import com.bookmanager.model.Book;
import com.bookmanager.model.User;
import com.bookmanager.singleton.BookManagerSingleton;
/**
 * 图书系统适配器
 * 实现新系统接口（NewBookService），持有旧系统引用（OldBookSystem）
 * 将旧系统的接口转换为新系统接口，实现新旧系统兼容
 */
public class BookSystemAdapter implements NewBookService {
    // 持有旧系统对象（适配者）
    private OldBookSystem oldBookSystem;
    // 持有新系统的单例管理类，用于操作新系统数据
    private BookManagerSingleton bookManager = BookManagerSingleton.getInstance();
    // 构造器：传入旧系统对象
    public BookSystemAdapter(OldBookSystem oldBookSystem) {
        this.oldBookSystem = oldBookSystem;
    }
    @Override
    public Book queryBook(String bookId) {
        // 适配逻辑：将新系统的“按ID查询”转换为旧系统的“按名称查询”
        // 1. 先通过新系统查询图书名称（假设ID与名称对应，实际项目可优化）
        Book book = bookManager.getBookById(bookId);
        if (book == null) {
            System.out.println("新系统未查询到该图书，尝试调用旧系统查询...");
            // 2. 调用旧系统的查询方法（按名称查询）
            String oldResult = oldBookSystem.searchBookByName("未知图书");
            System.out.println(oldResult);
            return null;
        }
        // 3. 调用旧系统查询，同时返回新系统需要的Book对象
        oldBookSystem.searchBookByName(book.getBookName());
        return book;
    }
    @Override
    public boolean addBook(User user, Book book) {
        // 适配逻辑：将新系统的“新增Book对象”转换为旧系统的“新增名称+作者”
        // 1. 调用旧系统的新增方法
        oldBookSystem.addOldBook(book.getBookName(), book.getAuthor());
        // 2. 同时调用新系统的新增方法，同步数据
        return bookManager.addBook(book);
    }
}
3.8 模板方法模式（template包）—— 行为型模式
3.8.1 模式说明（复习重点）
模板方法模式：定义一个操作中的算法的骨架，而将一些步骤延迟到子类中。模板方法使得子类可以不改变一个算法的结构即可重定义该算法的某些特定步骤。 核心要点：抽象类（定义算法骨架，包含模板方法和抽象步骤）、具体子类（实现抽象步骤，重定义算法的可变部分）、模板方法不可重写（用final修饰）。 本项目用于图书借阅流程：借阅流程固定（校验权限→校验借阅规则→执行借阅→通知观察者），可变步骤（权限校验、借阅规则校验）由子类实现。
3.8.2 代码实现
// 1. BorrowTemplate.java（抽象类：借阅模板）
package com.bookmanager.template;
import com.bookmanager.model.Book;
import com.bookmanager.model.BorrowRecord;
import com.bookmanager.model.User;
import com.bookmanager.observer.BookSubject;
import com.bookmanager.observer.MessageObserver;
import com.bookmanager.singleton.BookManagerSingleton;
/**
 * 借阅模板抽象类
 * 定义图书借阅的固定流程（模板方法），可变步骤延迟到子类实现
 */
public abstract class BorrowTemplate {
    // 单例管理类
    protected BookManagerSingleton bookManager = BookManagerSingleton.getInstance();
    // 图书主题（用于通知观察者）
    protected BookSubject bookSubject = new BookSubject();
    /**
     * 模板方法（final修饰，不可重写）
     * 定义借阅的固定流程：1.校验权限 → 2.校验借阅规则 → 3.执行借阅 → 4.通知观察者
     */
    public final boolean borrowBook(User user, Book book) {
        // 步骤1：校验权限（抽象方法，子类实现）
        if (!checkPermission(user)) {
            return false;
        }
        // 步骤2：校验借阅规则（抽象方法，子类实现）
        if (!checkBorrowRule(user, book)) {
            return false;
        }
        // 步骤3：执行借阅（固定逻辑，所有子类共用）
        boolean borrowSuccess = doBorrow(user, book);
        if (!borrowSuccess) {
            return false;
        }
        // 步骤4：通知观察者（固定逻辑，所有子类共用）
        notifyObserver(book);
        return true;
    }
    /**
     * 抽象方法：校验权限（可变步骤，子类实现）
     */
    protected abstract boolean checkPermission(User user);
    /**
     * 抽象方法：校验借阅规则（可变步骤，子类实现）
     */
    protected abstract boolean checkBorrowRule(User user, Book book);
    /**
     * 固定方法：执行借阅（核心逻辑，所有子类共用）
     */
    private boolean doBorrow(User user, Book book) {
        // 校验图书是否可借阅
        if (!book.isBorrowable()) {
            System.out.println("图书《" + book.getBookName() + "》当前不可借阅（无库存/已借出）！");
            return false;
        }
        // 减少库存
        book.setStock(book.getStock() - 1);
        // 新增借阅记录
        String recordId = "R" + (bookManager.getBorrowRecordList().size() + 1);
        BorrowRecord record = new BorrowRecord(recordId, user.getUserId(), book.getBookId());
        bookManager.addBorrowRecord(record);
        System.out.println("用户" + user.getUserName() + "借阅图书《" + book.getBookName() + "》成功！");
        return true;
    }
    /**
     * 固定方法：通知观察者（所有子类共用）
     */
    private void notifyObserver(Book book) {
        // 注册观察者（消息观察者）
        bookSubject.registerObserver(new MessageObserver());
        // 设置图书状态，触发通知
        bookSubject.setBook(book);
    }
}
// 2. NormalBorrowTemplate.java（具体子类：普通用户借阅模板）
package com.bookmanager.template;
import com.bookmanager.model.Book;
import com.bookmanager.model.User;
import com.bookmanager.strategy.BorrowContext;
import com.bookmanager.strategy.NormalBorrowStrategy;
/**
 * 普通用户借阅模板（具体子类）
 * 实现抽象模板的可变步骤（权限校验、借阅规则校验）
 */
public class NormalBorrowTemplate extends BorrowTemplate {
    // 借阅上下文（使用策略模式判断借阅规则）
    private BorrowContext borrowContext = new BorrowContext();
    @Override
    protected boolean checkPermission(User user) {
        // 普通用户借阅权限：无需特殊权限（只要是普通用户即可）
        if (!"normal".equals(user.getRole())) {
            System.out.println("用户" + user.getUserName() + "不是普通用户，无法使用普通用户借阅流程！");
            return false;
        }
        return true;
    }
    @Override
    protected boolean checkBorrowRule(User user, Book book) {
        // 普通用户借阅规则：使用普通用户借阅策略
        borrowContext.setBorrowStrategy(new NormalBorrowStrategy());
        return borrowContext.canBorrow(user);
    }
}
// 3. AdminBorrowTemplate.java（具体子类：管理员借阅模板）
package com.bookmanager.template;
import com.bookmanager.model.Book;
import com.bookmanager.model.User;
import com.bookmanager.strategy.BorrowContext;
import com.bookmanager.strategy.AdminBorrowStrategy;
/**
 * 管理员借阅模板（具体子类）
 * 实现抽象模板的可变步骤（权限校验、借阅规则校验）
 */
public class AdminBorrowTemplate extends BorrowTemplate {
    // 借阅上下文（使用策略模式判断借阅规则）
    private BorrowContext borrowContext = new BorrowContext();
    @Override
    protected boolean checkPermission(User user) {
        // 管理员借阅权限：只有管理员可使用该借阅流程
        if (!"admin".equals(user.getRole())) {
            System.out.println("用户" + user.getUserName() + "不是管理员，无法使用管理员借阅流程！");
            return false;
        }
        return true;
    }
    @Override
    protected boolean checkBorrowRule(User user, Book book) {
        // 管理员借阅规则：使用管理员借阅策略（无数量限制）
        borrowContext.setBorrowStrategy(new AdminBorrowStrategy());
        return borrowContext.canBorrow(user);
    }
}

