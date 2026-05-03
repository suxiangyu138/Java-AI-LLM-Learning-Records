03.31 01:35
MySQL企业级实战项目：电商订单管理系统（企业级规范+性能优化）
项目简介
基于MySQL 8.0构建的电商订单管理系统，覆盖企业级数据库设计、索引优化、SQL性能调优、事务控制、分表分库、数据备份等核心技能，贴合互联网企业真实业务场景。
技术栈
- 数据库：MySQL 8.0
- 规范：三范式、反范式、命名规范、索引规范
- 优化：索引设计、SQL调优、执行计划、慢查询分析
- 架构：主从复制、读写分离、分表分库
- 工具：Navicat、MySQL Workbench、pt-query-digest
数据库设计（企业级规范）
1. 数据库命名
sql
CREATE DATABASE IF NOT EXISTS ecommerce DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ecommerce;
 
2. 表设计（三范式+反范式结合）
用户表（user）
sql
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码(加密)',
  `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1-正常 0-禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
 
商品表（product）
sql
CREATE TABLE `product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称',
  `price` DECIMAL(10,2) NOT NULL COMMENT '商品价格',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '库存',
  `category_id` BIGINT NOT NULL COMMENT '分类ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1-上架 0-下架',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';
 
订单表（order_info）- 分表依据（按user_id哈希）
sql
CREATE TABLE `order_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `total_price` DECIMAL(10,2) NOT NULL COMMENT '订单总价',
  `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态 0-未支付 1-已支付 2-已取消',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_pay_status` (`pay_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';
 
订单明细表（order_item）
sql
CREATE TABLE `order_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称(冗余)',
  `price` DECIMAL(10,2) NOT NULL COMMENT '商品单价',
  `quantity` INT NOT NULL COMMENT '购买数量',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';
 
企业级SQL操作（性能优化+事务）
1. 订单创建（事务+库存扣减）
sql
-- 开启事务
START TRANSACTION;
-- 1. 扣减库存（悲观锁，防止超卖）
UPDATE product 
SET stock = stock - 1 
WHERE id = 1 AND stock >= 1;
-- 2. 创建订单
INSERT INTO order_info (order_no, user_id, total_price, pay_status) 
VALUES ('ORDER20240520001', 1, 99.00, 0);
-- 3. 创建订单明细
INSERT INTO order_item (order_id, product_id, product_name, price, quantity) 
VALUES (LAST_INSERT_ID(), 1, 'iPhone 15', 99.00, 1);
-- 提交事务
COMMIT;
 
2. 订单查询（联合查询+索引优化）
sql
-- 订单详情查询（避免SELECT *）
SELECT 
  o.id, o.order_no, o.total_price, o.pay_status,
  p.product_name, oi.quantity, oi.price
FROM order_info o
JOIN order_item oi ON o.id = oi.order_id
JOIN product p ON oi.product_id = p.id
WHERE o.user_id = 1 
  AND o.create_time >= '2024-01-01'
ORDER BY o.create_time DESC
LIMIT 10;
 
3. 分页查询（避免深分页）
sql
-- 优化后分页（基于主键）
SELECT * FROM order_info 
WHERE id > 1000 
ORDER BY id ASC 
LIMIT 10;
 
企业级优化方案
1. 索引优化
- 主键索引：所有表必须有主键（B+树）
- 唯一索引：订单号、用户名、手机号
- 普通索引：外键、状态、时间字段
- 联合索引：(user_id, create_time) 避免回表
2. SQL调优
- 避免SELECT *，只查需要字段
- 避免在索引列使用函数（WHERE DATE(create_time) = '2024-05-20'）
- 避免隐式类型转换（WHERE id = '1'）
- 分页优化：基于主键分页，避免OFFSET过大
3. 分表分库（订单表）
- 水平分表：按user_id哈希分表（order_info_0 ~ order_info_15）
- 分表规则：user_id % 16
- 优势：单表数据量控制在1000万以内，提升查询性能
4. 主从复制+读写分离
- 主库：写入（订单创建、库存扣减）
- 从库：读取（订单查询、商品查询）
- 实现：MySQL主从复制 + MyCat/Sharding-JDBC
5. 慢查询优化
- 开启慢查询日志：slow_query_log = 1
- 阈值：long_query_time = 2
- 分析工具：pt-query-digest、mysqldumpslow
企业级规范
1. 命名规范
- 库名：小写+下划线（ecommerce）
- 表名：小写+下划线（order_info）
- 字段名：小写+下划线（user_id）
- 索引名：idx_字段名、uk_字段名
2. 字段规范
- 必须有id（主键）、create_time、update_time
- 状态字段用TINYINT（0/1/2）
- 金额用DECIMAL(10,2)，避免浮点数精度问题
- 字符串用VARCHAR，长度合理，默认utf8mb4
3. 引擎规范
- 业务表：InnoDB（支持事务、外键、行锁）
- 日志表：MyISAM（查询快，不支持事务）
数据备份与恢复
1. 全量备份（定时任务）
bash
mysqldump -uroot -p --databases ecommerce > ecommerce_$(date +%Y%m%d).sql
 
2. 增量备份（binlog）
bash
mysqlbinlog --start-time="2024-05-20 00:00:00" mysql-bin.000001 > binlog.sql
 
3. 数据恢复
bash
mysql -uroot -p ecommerce < ecommerce_20240520.sql
 
项目扩展方向
1. 加入Redis缓存商品信息、订单状态
2. 实现分表分库（Sharding-JDBC）
3. 集成Elasticsearch实现订单全文检索
4. 加入数据监控（Prometheus+Grafana）
5. 实现数据归档（历史订单归档至OSS）
企业级面试考点
1. 索引设计原则与失效场景
2. 事务隔离级别与锁机制（InnoDB行锁）
3. 分表分库策略与跨库查询解决方案
4. 慢查询优化步骤与执行计划分析
5. 主从复制原理与读写分离实现
6. 超卖问题解决方案（悲观锁/乐观锁）

