# MySQL 图书管理系统数据库设计

## 一、系统功能说明
这个图书管理系统包含 4 张核心表：
1. **图书表**：存储图书信息
2. **读者表**：存储借书的读者信息
3. **管理员表**：系统管理员账号
4. **借阅记录表**：记录图书借阅/归还关系

## 二、完整SQL代码（直接复制执行）
### 1. 创建数据库
```sql
-- 创建图书管理数据库，设置默认编码
CREATE DATABASE IF NOT EXISTS library_system 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE library_system;
```

### 2. 创建数据表（带约束，规范设计）
```sql
-- 1. 图书表
CREATE TABLE book (
    book_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '图书编号(主键)',
    book_name VARCHAR(100) NOT NULL COMMENT '图书名称',
    author VARCHAR(50) NOT NULL COMMENT '作者',
    publisher VARCHAR(50) COMMENT '出版社',
    publish_date DATE COMMENT '出版日期',
    total INT NOT NULL DEFAULT 1 COMMENT '总藏书量',
    stock INT NOT NULL DEFAULT 1 COMMENT '当前可借库存',
    book_type VARCHAR(20) COMMENT '图书类型(小说/科技等)',
    create_time DATETIME DEFAULT NOW() COMMENT '录入时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书信息表';

-- 2. 读者表
CREATE TABLE reader (
    reader_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '读者编号',
    reader_name VARCHAR(20) NOT NULL COMMENT '姓名',
    gender ENUM('男','女') DEFAULT '男' COMMENT '性别',
    phone VARCHAR(11) UNIQUE NOT NULL COMMENT '手机号',
    email VARCHAR(50) COMMENT '邮箱',
    address VARCHAR(100) COMMENT '地址',
    register_time DATETIME DEFAULT NOW() COMMENT '注册时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='读者信息表';

-- 3. 管理员表
CREATE TABLE admin (
    admin_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '管理员编号',
    admin_name VARCHAR(20) NOT NULL COMMENT '账号',
    password VARCHAR(32) NOT NULL COMMENT '密码',
    real_name VARCHAR(20) NOT NULL COMMENT '真实姓名',
    create_time DATETIME DEFAULT NOW() COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- 4. 借阅记录表（核心关联表）
CREATE TABLE borrow_record (
    borrow_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '借阅记录编号',
    book_id INT NOT NULL COMMENT '图书编号',
    reader_id INT NOT NULL COMMENT '读者编号',
    borrow_date DATETIME DEFAULT NOW() COMMENT '借书时间',
    return_date DATETIME NULL COMMENT '应还时间',
    real_return_date DATETIME NULL COMMENT '实际归还时间',
    -- 0未归还 1已归还 2逾期
    status TINYINT DEFAULT 0 COMMENT '借阅状态', 
    -- 外键约束
    FOREIGN KEY (book_id) REFERENCES book(book_id),
    FOREIGN KEY (reader_id) REFERENCES reader(reader_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借阅记录表';
```

### 3. 插入测试数据
```sql
-- 插入图书
INSERT INTO book(book_name,author,publisher,publish_date,total,stock,book_type)
VALUES 
('红楼梦','曹雪芹','人民文学出版社','2020-01-10',5,5,'文学'),
('三体','刘慈欣','重庆出版社','2018-05-15',3,3,'科幻'),
('MySQL从入门到精通','张三','机械工业出版社','2022-03-20',4,4,'计算机');

-- 插入读者
INSERT INTO reader(reader_name,gender,phone,email,address)
VALUES 
('小明','男','13800138000','xm@qq.com','北京市海淀区'),
('小红','女','13900139000','xh@163.com','上海市浦东新区');

-- 插入管理员（密码：123456）
INSERT INTO admin(admin_name,password,real_name)
VALUES ('admin','123456','系统管理员');

-- 插入借阅记录
INSERT INTO borrow_record(book_id,reader_id,borrow_date,return_date,status)
VALUES 
(1,1,NOW(),DATE_ADD(NOW(),INTERVAL 30 DAY),0),
(2,2,NOW(),DATE_ADD(NOW(),INTERVAL 30 DAY),0);
```

### 4. 常用查询语句（直接使用）
```sql
-- 1. 查询所有图书及库存
SELECT * FROM book;

-- 2. 查询所有未归还的借阅记录
SELECT br.borrow_id, b.book_name, r.reader_name, br.borrow_date, br.return_date
FROM borrow_record br
JOIN book b ON br.book_id = b.book_id
JOIN reader r ON br.reader_id = r.reader_id
WHERE br.status = 0;

-- 3. 查询某读者借了哪些书
SELECT b.book_name, br.borrow_date, br.status
FROM borrow_record br
JOIN book b ON br.book_id = b.book_id
WHERE br.reader_id = 1;

-- 4. 借书后自动减少库存（事务）
UPDATE book SET stock = stock - 1 WHERE book_id = 1;

-- 5. 还书后自动增加库存
UPDATE book SET stock = stock + 1 WHERE book_id = 1;
UPDATE borrow_record SET real_return_date = NOW(), status = 1 WHERE borrow_id = 1;
```

