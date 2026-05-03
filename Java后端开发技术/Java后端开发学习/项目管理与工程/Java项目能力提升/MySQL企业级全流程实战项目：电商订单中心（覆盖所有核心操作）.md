03.31 01:51
MySQL企业级全流程实战项目：电商订单中心（覆盖所有核心操作）
项目定位
完整覆盖MySQL企业级开发全流程：库表设计→索引优化→事务控制→SQL调优→分表分库→主从复制→备份恢复→监控运维，贴合互联网真实业务场景。
一、数据库设计（企业级规范）
1. 库创建
sql
CREATE DATABASE IF NOT EXISTS order_center DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE order_center;
 
2. 表设计（含索引、注释、引擎）
用户表（user）
sql
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '加密密码',
  `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
 
商品表（product）
sql
CREATE TABLE `product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `product_name` VARCHAR(200) NOT NULL COMMENT '商品名',
  `price` DECIMAL(10,2) NOT NULL COMMENT '单价',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '库存',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';
 
订单主表（order_info）
sql
CREATE TABLE `order_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `total_price` DECIMAL(10,2) NOT NULL COMMENT '总价',
  `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0未支付 1已支付 2已取消',
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
  `product_name` VARCHAR(200) NOT NULL COMMENT '商品名(冗余)',
  `price` DECIMAL(10,2) NOT NULL COMMENT '单价',
  `quantity` INT NOT NULL COMMENT '数量',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';
 
二、基础操作（CRUD+批量）
1. 插入数据
sql
-- 单条插入
INSERT INTO user (username, password, phone) VALUES ('zhangsan', '123456', '13800138000');
-- 批量插入
INSERT INTO product (product_name, price, stock) VALUES 
('iPhone15', 5999.00, 100),
('MacBook Pro', 12999.00, 50);
 
2. 查询数据
sql
-- 基础查询
SELECT id, username, phone FROM user WHERE status = 1;
-- 联合查询（订单+商品）
SELECT o.order_no, p.product_name, oi.quantity 
FROM order_info o
JOIN order_item oi ON o.id = oi.order_id
JOIN product p ON oi.product_id = p.id
WHERE o.user_id = 1;
 
3. 更新数据
sql
-- 单条更新
UPDATE user SET phone = '13900139000' WHERE id = 1;
-- 条件更新
UPDATE product SET stock = stock - 1 WHERE id = 1 AND stock > 0;
 
4. 删除数据（逻辑删除优先）
sql
-- 逻辑删除
UPDATE user SET status = 0 WHERE id = 1;
-- 物理删除（谨慎）
DELETE FROM order_info WHERE id = 1;
 
三、事务控制（企业级核心）
1. 订单创建事务（防超卖）
sql
START TRANSACTION;
-- 扣减库存
UPDATE product SET stock = stock - 1 WHERE id = 1 AND stock >= 1;
-- 创建订单
INSERT INTO order_info (order_no, user_id, total_price) VALUES ('ORDER20240520001', 1, 5999.00);
-- 创建订单明细
INSERT INTO order_item (order_id, product_id, product_name, price, quantity) 
VALUES (LAST_INSERT_ID(), 1, 'iPhone15', 5999.00, 1);
COMMIT;
 
2. 回滚场景
sql
START TRANSACTION;
UPDATE product SET stock = 0 WHERE id = 1;
ROLLBACK;
 
四、索引优化（性能核心）
1. 查看索引
sql
SHOW INDEX FROM order_info;
 
2. 创建联合索引（优化查询）
sql
CREATE INDEX idx_user_create ON order_info (user_id, create_time);
 
3. 执行计划分析（EXPLAIN）
sql
EXPLAIN SELECT * FROM order_info WHERE user_id = 1 AND create_time > '2024-01-01';
 
五、SQL调优（企业必备）
1. 避免深分页
sql
-- 优化前
SELECT * FROM order_info ORDER BY id LIMIT 10000, 10;
-- 优化后
SELECT * FROM order_info WHERE id > 10000 ORDER BY id LIMIT 10;
 
2. 避免索引失效
sql
-- 失效（函数操作索引列）
SELECT * FROM order_info WHERE DATE(create_time) = '2024-05-20';
-- 优化
SELECT * FROM order_info WHERE create_time >= '2024-05-20 00:00:00' AND create_time < '2024-05-21 00:00:00';
 
六、分表分库（高并发方案）
1. 水平分表示例（按user_id取模）
sql
-- 订单分表1
CREATE TABLE order_info_1 LIKE order_info;
-- 订单分表2
CREATE TABLE order_info_2 LIKE order_info;
 
2. 分表规则
user_id % 2 + 1 → 路由到对应分表
七、主从复制（读写分离）
1. 主库配置（my.cnf）
ini
server-id=1
log_bin=mysql-bin
binlog_format=row
 
2. 从库配置（my.cnf）
ini
server-id=2
relay_log=relay-bin
read_only=1
 
八、备份恢复（数据安全）
1. 全量备份
bash
mysqldump -uroot -p order_center > order_center_20240520.sql
 
2. 数据恢复
bash
mysql -uroot -p order_center < order_center_20240520.sql
 
九、慢查询分析
1. 开启慢查询
sql
SET GLOBAL slow_query_log = 1;
SET GLOBAL long_query_time = 2;
 
2. 查看慢查询
sql
SHOW VARIABLES LIKE 'slow_query_log%';
 
十、监控运维
1. 查看连接数
sql
SHOW PROCESSLIST;
 
2. 查看锁等待
sql
SELECT * FROM information_schema.INNODB_LOCKS;
 
项目总结
本项目完整覆盖MySQL企业级开发所有核心操作：
1. 规范库表设计与索引构建
2. 事务控制与数据一致性保障
3. SQL性能调优与执行计划分析
4. 分表分库与主从复制架构
5. 数据备份、恢复与慢查询监控
可直接用于面试准备、企业实战、技能提升，所有操作均贴合互联网真实场景。

