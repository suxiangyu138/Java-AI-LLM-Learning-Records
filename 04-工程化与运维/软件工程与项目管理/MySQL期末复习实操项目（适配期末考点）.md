MySQL期末复习实操项目（适配期末考点）
项目说明
本项目以“学生成绩管理系统”为载体，覆盖MySQL期末核心考点（数据库/表创建、数据操作、查询、约束、索引、视图、存储过程/函数等），所有操作均贴合期末考题难度，每一步对应考点解析，帮你边实操边巩固，完成项目即可掌握80%以上期末高频考点。
适用场景：MySQL期末复习（本科/专科，基础+进阶考点），无需复杂环境，本地MySQL（5.7/8.0版本均可）即可完成，每一步均有详细代码+考点标注，新手可直接跟着做。
前期准备
1. 环境要求
    本地安装MySQL（5.7或8.0，推荐8.0，兼容所有操作）
    工具：Navicat、SQLyog、MySQL命令行均可（推荐Navicat，可视化操作更便捷，适合复习排查错误）
    核心前提：掌握MySQL基础语法（DDL、DML、DQL），本项目会针对性强化考点。
2. 考点梳理（项目同步覆盖）
    期末高频考点（项目每一步对应以下考点，做完可逐一核对）：
    DDL语句：数据库创建、表创建（含数据类型、约束）、表修改/删除
    DML语句：插入、修改、删除数据（含批量操作、条件删除）
    DQL语句：基础查询、条件查询、排序、分组、聚合函数、联表查询（内连接、左连接、右连接）、子查询
    约束：主键、外键、非空、唯一、检查约束
    进阶考点：索引、视图、存储过程、函数（期末常考简单应用）
    项目实操步骤（核心环节，逐一步骤来）
    步骤1：创建数据库（考点：DDL-数据库操作）
    操作目的
    掌握数据库的创建、查看、使用、删除语法（期末选择题/填空题常考）。
    SQL代码（直接复制执行）
    -- 1. 创建数据库（指定字符集utf8mb4，避免中文乱码，期末常考字符集设置）
    CREATE DATABASE IF NOT EXISTS student_score_system 
    DEFAULT CHARACTER SET utf8mb4 
    DEFAULT COLLATE utf8mb4_general_ci;
    -- 2. 查看所有数据库（验证是否创建成功）
    SHOW DATABASES;
    -- 3. 使用该数据库（后续所有操作均基于此数据库）
    USE student_score_system;
    -- 考点标注：IF NOT EXISTS 避免重复创建报错，DEFAULT CHARACTER SET 是期末常考考点，需记住utf8mb4兼容所有中文
    实操验证
    执行代码后，在可视化工具中查看是否出现“student_score_system”数据库，切换到该数据库，准备后续表的创建。
    步骤2：创建数据表（考点：DDL-表创建、约束、数据类型）
    操作目的
    掌握表的创建语法，理解主键、外键、非空、唯一约束的作用（期末大题常考：设计表结构，写出创建语句）。
    本项目设计3张核心表（贴合学生场景，便于理解）：
    student（学生表）：存储学生基本信息
    course（课程表）：存储课程信息
    score（成绩表）：存储学生课程成绩（关联学生表和课程表，用外键）
    SQL代码（直接复制执行）
    -- 1. 创建学生表（student）
    CREATE TABLE IF NOT EXISTS student (
    student_id INT PRIMARY KEY AUTO_INCREMENT,  -- 主键，自增（期末高频考点）
    student_name VARCHAR(50) NOT NULL,          -- 非空约束（姓名不能为空）
    student_gender CHAR(2) CHECK (student_gender IN ('男','女')),  -- 检查约束（性别只能是男/女）
    student_age INT NOT NULL,                   -- 非空约束
    student_class VARCHAR(50) NOT NULL,         -- 非空约束（班级）
    student_no VARCHAR(20) UNIQUE NOT NULL      -- 唯一约束（学号不能重复）
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
    -- 2. 创建课程表（course）
    CREATE TABLE IF NOT EXISTS course (
    course_id INT PRIMARY KEY AUTO_INCREMENT,   -- 主键，自增
    course_name VARCHAR(50) NOT NULL UNIQUE,    -- 非空+唯一（课程名不能重复）
    course_credit INT NOT NULL,                 -- 非空约束（学分）
    course_teacher VARCHAR(50) NOT NULL         -- 非空约束（授课老师）
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
    -- 3. 创建成绩表（score）（核心：外键关联，期末大题重点）
    CREATE TABLE IF NOT EXISTS score (
    score_id INT PRIMARY KEY AUTO_INCREMENT,    -- 主键，自增
    student_id INT NOT NULL,                    -- 关联学生表的主键
    course_id INT NOT NULL,                     -- 关联课程表的主键
    score INT CHECK (score BETWEEN 0 AND 100),  -- 检查约束（成绩0-100分）
    exam_date DATE,                             -- 考试日期（可空）
    -- 外键约束（期末高频考点，必须掌握）
    FOREIGN KEY (student_id) REFERENCES student(student_id)
    ON DELETE CASCADE  -- 级联删除：学生表删除数据，成绩表对应数据也删除
    ON UPDATE CASCADE, -- 级联更新：学生表主键更新，成绩表对应字段也更新
    FOREIGN KEY (course_id) REFERENCES course(course_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
    -- 唯一约束：一个学生一门课程只能有一个成绩
    UNIQUE (student_id, course_id)
    );
    -- 考点标注：
    -- 1. 主键（PRIMARY KEY）：唯一标识表中记录，非空且唯一，自增（AUTO_INCREMENT）仅适用于INT类型
    -- 2. 外键（FOREIGN KEY）：关联两个表，保证数据完整性，级联操作（ON DELETE/UPDATE）是期末重点
    -- 3. 数据类型：INT（整数）、VARCHAR（字符串）、CHAR（固定长度字符串）、DATE（日期）
    -- 4. 约束：非空（NOT NULL）、唯一（UNIQUE）、检查（CHECK），需记住各自作用
    实操验证
    执行代码后，查看数据库中的3张表，核对表结构（字段名、数据类型、约束）是否正确，重点检查外键是否创建成功（可视化工具中可查看“外键”选项）。
    步骤3：插入测试数据（考点：DML-插入数据）
    操作目的
    掌握INSERT语句的使用（单条插入、批量插入），注意数据格式与表结构匹配（期末选择题常考插入数据的错误场景）。
    SQL代码（直接复制执行）
    -- 1. 向学生表插入数据（单条+批量插入，期末常考批量插入）
    -- 单条插入
    INSERT INTO student (student_name, student_gender, student_age, student_class, student_no)
    VALUES ('张三', '男', 20, '计算机2班', '2024001');
    -- 批量插入（推荐，效率高，期末大题常考）
    INSERT INTO student (student_name, student_gender, student_age, student_class, student_no)
    VALUES 
    ('李四', '女', 19, '计算机2班', '2024002'),
    ('王五', '男', 20, '计算机1班', '2024003'),
    ('赵六', '女', 19, '计算机1班', '2024004'),
    ('孙七', '男', 20, '计算机2班', '2024005'),
    ('周八', '女', 19, '计算机1班', '2024006');
    -- 2. 向课程表插入数据（批量插入）
    INSERT INTO course (course_name, course_credit, course_teacher)
    VALUES 
    ('MySQL数据库', 3, '张老师'),
    ('Java编程', 4, '李老师'),
    ('Python基础', 3, '王老师'),
    ('计算机网络', 3, '赵老师');
    -- 3. 向成绩表插入数据（关联学生和课程，注意student_id和course_id必须存在于对应表中）
    INSERT INTO score (student_id, course_id, score, exam_date)
    VALUES 
    (1, 1, 88, '2024-12-10'),  -- 张三 - MySQL数据库 - 88分
    (1, 2, 92, '2024-12-11'),  -- 张三 - Java编程 - 92分
    (2, 1, 79, '2024-12-10'),  -- 李四 - MySQL数据库 - 79分
    (2, 3, 85, '2024-12-12'),  -- 李四 - Python基础 - 85分
    (3, 2, 68, '2024-12-11'),  -- 王五 - Java编程 - 68分
    (3, 4, 90, '2024-12-13'),  -- 王五 - 计算机网络 - 90分
    (4, 1, 95, '2024-12-10'),  -- 孙七 - MySQL数据库 - 95分
    (4, 3, 82, '2024-12-12'),  -- 孙七 - Python基础 - 82分
    (5, 2, 75, '2024-12-11'),  -- 周八 - Java编程 - 75分
    (5, 4, 88, '2024-12-13');  -- 周八 - 计算机网络 - 88分
    -- 考点标注：
    -- 1. 插入数据时，字段顺序可与表结构不一致，但VALUES顺序需与字段顺序对应
    -- 2. 批量插入用逗号分隔多个VALUES，效率高于单条插入，期末大题常考
    -- 3. 外键关联的数据，插入时需保证关联字段的值存在于主表中，否则报错（期末常考错误点）
    实操验证
    执行插入语句后，分别查询3张表的数据，验证数据是否插入成功（查询语句：SELECT * FROM 表名;）。
    步骤4：数据查询（考点：DQL-核心，期末大题重中之重）
    操作目的
    掌握基础查询、条件查询、排序、分组、聚合函数、联表查询、子查询，覆盖期末DQL所有高频考点，每道查询题对应期末考题类型。
    SQL代码（逐题执行，理解每道题的考点）
    -- 一、基础查询（期末基础题）
    -- 1. 查询所有学生的姓名、学号、班级（指定字段查询）
    SELECT student_name, student_no, student_class FROM student;
    -- 2. 查询所有课程的课程名、学分（去重，假设存在重复课程，期末常考DISTINCT）
    SELECT DISTINCT course_name, course_credit FROM course;
    -- 二、条件查询（WHERE子句，期末高频）
    -- 1. 查询计算机2班的所有学生信息（等于条件）
    SELECT * FROM student WHERE student_class = '计算机2班';
    -- 2. 查询年龄大于19岁的男生信息（多条件，AND）
    SELECT * FROM student WHERE student_age > 19 AND student_gender = '男';
    -- 3. 查询成绩在80-90分之间的记录（BETWEEN...AND...，等价于 >= AND <=）
    SELECT * FROM score WHERE score BETWEEN 80 AND 90;
    -- 4. 查询课程名为MySQL数据库或Java编程的课程信息（IN）
    SELECT * FROM course WHERE course_name IN ('MySQL数据库', 'Java编程');
    -- 5. 查询姓名包含“张”字的学生信息（LIKE模糊查询，%表示任意字符，期末常考）
    SELECT * FROM student WHERE student_name LIKE '%张%';
    -- 三、排序查询（ORDER BY，期末基础题）
    -- 1. 查询所有学生的姓名、年龄，按年龄升序排序（ASC可省略，默认升序）
    SELECT student_name, student_age FROM student ORDER BY student_age ASC;
    -- 2. 查询MySQL数据库（course_id=1）的所有成绩，按成绩降序排序（DESC）
    SELECT * FROM score WHERE course_id = 1 ORDER BY score DESC;
    -- 四、聚合函数+分组查询（GROUP BY，期末大题重点）
    -- 1. 统计每个班级的学生人数（COUNT聚合函数，分组）
    SELECT student_class, COUNT(student_id) AS 学生人数 
    FROM student 
    GROUP BY student_class;
    -- 2. 统计每门课程的平均成绩、最高成绩、最低成绩（AVG、MAX、MIN聚合函数）
    SELECT 
    course_id,
    AVG(score) AS 平均成绩,
    MAX(score) AS 最高成绩,
    MIN(score) AS 最低成绩
    FROM score 
    GROUP BY course_id;
    -- 3. 统计每门课程及格（≥60分）的学生人数（HAVING过滤分组结果，期末常考HAVING与WHERE的区别）
    SELECT 
    course_id,
    COUNT(student_id) AS 及格人数
    FROM score 
    WHERE score >= 60  -- WHERE过滤行数据（先过滤，再分组）
    GROUP BY course_id
    HAVING 及格人数 > 2;  -- HAVING过滤分组结果（分组后过滤）
    -- 考点：WHERE过滤行，HAVING过滤分组，HAVING必须跟在GROUP BY后面
    -- 五、联表查询（期末大题重中之重，内连接、左连接、右连接）
    -- 1. 内连接：查询学生的姓名、课程名、成绩（只显示有成绩的学生和课程）
    SELECT 
    s.student_name,
    c.course_name,
    sc.score
    FROM student s  -- 表别名，简化代码，期末常考
    INNER JOIN score sc ON s.student_id = sc.student_id
    INNER JOIN course c ON sc.course_id = c.course_id;
    -- 2. 左连接：查询所有学生的姓名、课程名、成绩（即使学生没有成绩，也显示学生信息，成绩为NULL）
    SELECT 
    s.student_name,
    c.course_name,
    sc.score
    FROM student s
    LEFT JOIN score sc ON s.student_id = sc.student_id
    LEFT JOIN course c ON sc.course_id = c.course_id;
    -- 3. 右连接：查询所有课程的课程名、学生姓名、成绩（即使课程没有学生选，也显示课程信息）
    SELECT 
    c.course_name,
    s.student_name,
    sc.score
    FROM course c
    RIGHT JOIN score sc ON c.course_id = sc.course_id
    RIGHT JOIN student s ON sc.student_id = s.student_id;
    -- 六、子查询（期末大题重点，嵌套查询）
    -- 1. 查询MySQL数据库（course_id=1）的最高成绩对应的学生姓名
    SELECT student_name 
    FROM student 
    WHERE student_id = (
    SELECT student_id 
    FROM score 
    WHERE course_id = 1 AND score = (SELECT MAX(score) FROM score WHERE course_id = 1)
    );
    -- 2. 查询成绩大于平均成绩的所有记录（子查询作为条件）
    SELECT * FROM score WHERE score > (SELECT AVG(score) FROM score);
    -- 3. 查询选了2门及以上课程的学生姓名（子查询+分组）
    SELECT student_name 
    FROM student 
    WHERE student_id IN (
    SELECT student_id 
    FROM score 
    GROUP BY student_id 
    HAVING COUNT(course_id) >= 2
    );
    -- 考点标注：
    -- 1. 联表查询的核心是找到表之间的关联字段（外键），内连接只取交集，左/右连接取一侧全部
    -- 2. 子查询分为单行子查询（用=、>等）和多行子查询（用IN），期末常考嵌套逻辑
    -- 3. 聚合函数（COUNT、AVG、MAX、MIN、SUM）需结合GROUP BY使用，否则会默认聚合所有数据
    实操验证
    逐题执行SQL代码，观察查询结果是否符合预期，重点理解联表查询和子查询的逻辑，这是期末DQL大题的核心，建议多修改条件（如修改分数范围、班级），反复练习。
    步骤5：数据修改与删除（考点：DML-UPDATE、DELETE）
    操作目的
    掌握UPDATE和DELETE语句的使用，注意条件约束（避免误删/误改所有数据），期末常考带条件的修改和删除。
    SQL代码（直接复制执行，注意条件）
    -- 一、修改数据（UPDATE，期末常考带条件修改）
    -- 1. 修改张三（student_id=1）的年龄为21岁
    UPDATE student SET student_age = 21 WHERE student_id = 1;
    -- 2. 修改Java编程（course_id=2）的学分为3分
    UPDATE course SET course_credit = 3 WHERE course_id = 2;
    -- 3. 修改所有学生的班级，将“计算机1班”改为“计算机01班”（批量修改，带条件）
    UPDATE student SET student_class = '计算机01班' WHERE student_class = '计算机1班';
    -- 二、删除数据（DELETE，期末常考带条件删除，注意外键级联）
    -- 1. 删除周八（student_id=5）的所有成绩（带条件，避免误删）
    DELETE FROM score WHERE student_id = 5;
    -- 2. 删除Python基础（course_id=3）的课程信息（触发外键级联删除，成绩表中该课程的成绩也会被删除）
    DELETE FROM course WHERE course_id = 3;
    -- 3. 注意：禁止执行 DELETE FROM 表名;（无条件删除，会删除表中所有数据，期末考题中常作为错误选项）
    -- 考点标注：
    -- 1. UPDATE和DELETE必须加WHERE条件（除非明确要修改/删除所有数据），否则会误操作
    -- 2. 外键级联删除（ON DELETE CASCADE）：删除主表数据，从表关联数据会自动删除，期末常考
    -- 3. 修改数据时，需保证数据格式与表结构匹配（如性别只能是男/女，成绩0-100）
    实操验证
    执行修改/删除语句后，查询对应表的数据，验证修改/删除是否生效（如查询张三的年龄、Python基础课程是否被删除）。
    步骤6：进阶操作（考点：索引、视图、存储过程/函数，期末进阶题）
    操作目的
    掌握索引、视图、存储过程/函数的基本使用，贴合期末进阶考点（一般占10-15分），难度适中，重点掌握语法。
    SQL代码（直接复制执行）
    -- 一、索引（期末常考：创建索引、查看索引、删除索引，作用是提高查询效率）
    -- 1. 为学生表的student_no（学号）创建唯一索引（学号唯一，适合建索引）
    CREATE UNIQUE INDEX idx_student_no ON student(student_no);
    -- 2. 为成绩表的score（成绩）创建普通索引（查询成绩时提高效率）
    CREATE INDEX idx_score ON score(score);
    -- 3. 查看学生表的所有索引
    SHOW INDEX FROM student;
    -- 4. 删除成绩表的idx_score索引
    DROP INDEX idx_score ON score;
    -- 考点标注：唯一索引（UNIQUE INDEX）既提高效率，又保证数据唯一；普通索引（INDEX）只提高效率
    -- 二、视图（期末常考：创建视图、查询视图、删除视图，视图是虚拟表，不存储实际数据）
    -- 1. 创建视图：显示学生姓名、课程名、成绩（简化联表查询，期末常考视图创建）
    CREATE VIEW v_student_score AS
    SELECT 
    s.student_name,
    c.course_name,
    sc.score
    FROM student s
    INNER JOIN score sc ON s.student_id = sc.student_id
    INNER JOIN course c ON sc.course_id = c.course_id;
    -- 2. 查询视图（与查询普通表一样）
    SELECT * FROM v_student_score;
    -- 3. 删除视图
    DROP VIEW IF EXISTS v_student_score;
    -- 考点标注：视图的核心作用是简化复杂查询，视图中的数据依赖于原表，原表数据修改，视图数据也会同步修改
    -- 三、存储过程（期末常考：创建简单存储过程，调用存储过程）
    -- 1. 创建存储过程：查询指定班级的学生人数（带参数，期末重点）
    DELIMITER //  -- 临时修改结束符，避免与存储过程中的分号冲突
    CREATE PROCEDURE get_class_student_count(IN class_name VARCHAR(50), OUT count INT)
    BEGIN
    SELECT COUNT(student_id) INTO count FROM student WHERE student_class = class_name;
    END //
    DELIMITER ;  -- 恢复默认结束符
    -- 2. 调用存储过程（查询计算机2班的学生人数）
    CALL get_class_student_count('计算机2班', @count);
    SELECT @count AS 计算机2班人数;
    -- 3. 删除存储过程
    DROP PROCEDURE IF EXISTS get_class_student_count;
    -- 四、函数（期末常考：创建简单函数，调用函数）
    -- 1. 创建函数：根据成绩判断等级（0-59不及格，60-79及格，80-100优秀）
    DELIMITER //
    CREATE FUNCTION get_score_grade(score INT) RETURNS VARCHAR(10)
    DETERMINISTIC  -- 确定函数（输入相同，输出相同）
    BEGIN
    DECLARE grade VARCHAR(10);
    IF score < 60 THEN
        SET grade = '不及格';
    ELSEIF score <= 79 THEN
        SET grade = '及格';
    ELSE
        SET grade = '优秀';
    END IF;
    RETURN grade;
    END //
    DELIMITER ;
    -- 2. 调用函数（查询成绩表中所有成绩的等级）
    SELECT score, get_score_grade(score) AS 成绩等级 FROM score;
    -- 3. 删除函数
    DROP FUNCTION IF EXISTS get_score_grade;
    -- 考点标注：
    -- 1. 存储过程可以有参数（IN/OUT/INOUT），可以执行多个SQL语句，无返回值（靠OUT参数输出）
    -- 2. 函数有返回值，只能执行查询语句，不能执行修改/删除语句
    -- 3. 创建存储过程/函数时，需修改结束符（DELIMITER），避免分号冲突
    实操验证
    执行代码后，验证索引、视图、存储过程、函数是否正常使用（如调用存储过程查看班级人数，调用函数查看成绩等级）。
    步骤7：项目总结（期末复习重点）
    本项目覆盖了MySQL期末80%以上的高频考点，重点掌握以下内容（期末备考核心）：
    DDL语句：数据库、表的创建（重点是约束和数据类型），这是期末大题的基础。
    DML语句：插入（批量插入）、修改、删除（带条件），注意外键级联操作。
    DQL语句：联表查询（内连接、左连接）、子查询、分组+聚合函数，这是期末大题的重中之重，占分最高。
    进阶考点：索引、视图、存储过程/函数，掌握基本语法和使用场景，应对进阶题。
    复习建议：反复执行本项目的所有SQL代码，修改条件（如修改查询条件、修改表结构），模拟期末考题场景；重点练习联表查询和子查询，多做几道类似题目，确保掌握逻辑。
    期末真题适配（补充）
    结合本项目，模拟1道期末大题（贴合真题难度），可自行练习：
    题目：查询每个班级的平均成绩（需显示班级名称、平均成绩，平均成绩保留2位小数），并按平均成绩降序排序，只显示平均成绩≥80分的班级。
    提示：需用到联表查询（student、score）、分组、聚合函数、排序、HAVING过滤，可参考步骤4中的联表查询和分组查询，自行写出SQL语句，答案可在下方查看。
    参考答案：
    SELECT 
    s.student_class,
    ROUND(AVG(sc.score), 2) AS 平均成绩
    FROM student s
    INNER JOIN score sc ON s.student_id = sc.student_id
    GROUP BY s.student_class
    HAVING 平均成绩 >= 80
    ORDER BY 平均成绩 DESC;
