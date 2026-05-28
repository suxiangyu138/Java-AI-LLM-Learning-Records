DBeaver 理论 + 实战（完整可直接上手）
一、DBeaver 是什么（理论）
1. 定义
    DBeaver 是一款免费开源、跨平台、多数据库通用客户端工具，支持几乎所有主流数据库（MySQL、PostgreSQL、Oracle、SQL Server、Redis、MongoDB、ClickHouse 等）。
2. 核心特点
    - 免费开源（社区版足够企业使用）
    - 跨平台（Windows/Mac/Linux）
    - 支持所有主流数据库
    - 强大 SQL 编辑器（语法高亮、自动补全、格式化）
    - 数据可视化、ER 图、导出/导入
    - 支持驱动自动下载
    - 轻量、启动快
3. 适用人群
    - Java 后端开发
    - 测试工程师
    - DBA
    - 学生、个人开发者
4. 与 Navicat 对比
    - Navicat：功能强、界面好、付费
    - DBeaver：免费、开源、支持数据库更多、轻量
 
二、下载安装（实战）
1. 下载
    官网：https://dbeaver.io/download/
    选择：
    - Windows：DBeaver Community Edition（Installer）
    - Mac：DMG 包
    - Linux：DEB/RPM
2. 安装
    一路下一步，安装完成后打开 DBeaver。
 
三、连接数据库（以 MySQL 为例）
1. 新建连接
    左上角：数据库 → 新建连接
    选择：MySQL → 下一步
2. 填写连接信息
    - 主机：localhost（远程填服务器 IP）
    - 端口：3306
    - 数据库：（可不填）
    - 用户名：root
    - 密码：你的 MySQL 密码
3. 测试连接
    点击「测试连接」
    如提示缺少驱动 → 自动下载 → 等待完成
    显示「已连接」→ 完成
4. 打开连接
    左侧双击连接名 → 展开数据库
 
四、基础操作（实战）
1. 创建数据库
    右键连接 → 创建 → 数据库
    填写：
    - 数据库名：test
    - 字符集：utf8mb4
    - 排序规则：utf8mb4_unicode_ci
    完成
2. 创建表
    展开 test → 右键表 → 创建 → 表
    添加字段：
    - id：INT，主键，自增
    - username：VARCHAR(50)
    - age：INT
    保存 → 表名：user
3. 增删改查数据
    （1）添加数据
    打开表 → 点击底部「+ 行」→ 填写数据 → 保存（Ctrl+S）
    （2）修改数据
    直接双击单元格修改 → 保存
    （3）删除数据
    选中行 → 点击「-」→ 确认
    （4）查询数据
    点击「SQL 编辑器」→ 新建 SQL
    输入：
    sql
    SELECT * FROM user;
 
运行（Ctrl+Enter）
 
五、SQL 编辑器高级功能
1. 语法高亮 + 自动补全
    输入表名会自动提示字段
2. SQL 格式化
    选中 SQL → Ctrl+Shift+F
3. 执行计划
    右键 SQL → 解释执行计划（查看 SQL 性能）
4. 多结果集
    支持同时运行多条 SQL，查看多个结果
 
六、数据导出/导入（实战）
1. 导出数据
    右键表 → 导出数据
    选择格式：
    - CSV
    - Excel
    - SQL
    - JSON
    选择路径 → 完成
2. 导入数据
    右键表 → 导入数据
    选择文件 → 映射字段 → 导入
 
七、ER 图（表关系图）
1. 打开 ER 图
    右键数据库 → 查看 ER 图
    自动生成表关系，可拖拽布局
2. 用途
    - 查看表结构关系
    - 设计表结构
    - 导出 ER 图
 
八、连接远程数据库（实战）
1. 准备
    - 服务器 IP
    - 端口（默认 3306）
    - 用户名/密码
    - 防火墙开放 3306 端口
2. 新建连接
    主机填服务器 IP，其他同上
3. 连接失败常见原因
    - 防火墙没开端口
    - MySQL 绑定了 127.0.0.1
    - 用户没有远程权限（需授权）
    授权命令：
    sql
    CREATE USER 'root'@'%' IDENTIFIED BY 'password';
    GRANT ALL PRIVILEGES ON *.* TO 'root'@'%';
    FLUSH PRIVILEGES;
 
 
九、支持其他数据库（实战）
1. Redis
    新建连接 → 选择 Redis
    填写 IP、端口、密码
    可查看 key、数据类型、内存信息
2. PostgreSQL
    新建连接 → PostgreSQL
    填写信息即可
3. Oracle
    需要下载 Oracle 驱动
    DBeaver 会自动提示下载
 
十、DBeaver 企业级最佳实践
1. 连接命名规范：环境-数据库（dev-mysql、prod-pg）
2. 常用 SQL 保存到「SQL 脚本」文件夹
3. 生产库操作前先备份
4. 使用事务执行重要 SQL
5. 导出数据优先用 CSV，避免乱码
6. 定期更新 DBeaver 版本
 
十一、总结
DBeaver = 免费 + 全数据库 + 轻量 + 强大
适合：
- 日常开发
- 测试
- 多数据库管理
- 学生学习
    完全可以替代 Navicat 做日常开发使用。
    如果你需要，我可以继续带你：
- DBeaver 连接 Redis 实战
- DBeaver 连接 ClickHouse
- DBeaver 快捷键大全
- DBeaver 备份与恢复
