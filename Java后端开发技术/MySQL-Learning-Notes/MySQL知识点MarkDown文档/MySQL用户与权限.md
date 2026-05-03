# MySQL 用户与权限 核心知识点（考试+实操全覆盖）
## 一、核心基础
1. MySQL 默认管理员账号：**`root`**
2. 用户格式：`用户名@访问地址`
    - `root@localhost`：仅本机登录
    - `root@%`：任意IP远程登录
3. 权限作用：**控制谁能连库、能做哪些操作**，保障数据库安全。
4. 核心系统库：`mysql` 库，存储**用户、密码、权限**等系统数据。

---

## 二、用户管理（DDL）
### 1. 查询所有用户
```sql
USE mysql;
SELECT user,host FROM user;
```

### 2. 创建用户
```sql
CREATE USER '用户名'@'主机' IDENTIFIED BY '密码';
```
示例：
```sql
-- 仅本机访问
CREATE USER 'test'@'localhost' IDENTIFIED BY '123456';

-- 任意IP远程访问
CREATE USER 'test'@'%' IDENTIFIED BY '123456';
```

### 3. 修改用户密码
```sql
ALTER USER '用户名'@'主机' IDENTIFIED BY '新密码';
```

### 4. 删除用户
```sql
DROP USER '用户名'@'主机';
```

---

## 三、权限管理核心命令
### 1. 授予权限 `GRANT`
语法：
```sql
GRANT 权限列表 ON 库名.表名 TO '用户名'@'主机';
```

#### 常用权限
- `ALL`：所有权限
- `SELECT`：查询
- `INSERT`：新增
- `UPDATE`：修改
- `DELETE`：删除
- `CREATE`：建库/建表
- `DROP`：删库/删表

#### 授权示例
```sql
-- 给test用户，授予db1库下所有表的查询、新增权限
GRANT SELECT,INSERT ON db1.* TO 'test'@'%';

-- 授予全部权限（慎用）
GRANT ALL ON *.* TO 'test'@'%';
```

### 2. 刷新权限（必执行）
授权/改权限后必须刷新，立即生效：
```sql
FLUSH PRIVILEGES;
```

### 3. 查看用户权限
```sql
SHOW GRANTS FOR 'test'@'%';
```

### 4. 撤销权限 `REVOKE`
```sql
REVOKE 权限列表 ON 库.表 FROM '用户名'@'主机';
```
示例：
```sql
-- 撤销test用户的删除权限
REVOKE DELETE ON db1.* FROM 'test'@'%';
```

---

## 四、权限范围划分
1. `*.*`：**所有库、所有表**
2. `数据库名.*`：指定库下**所有表**
3. `数据库名.表名`：指定库下**单张表**

---

## 五、远程连接关键问题
1. root 默认绑定 `localhost`，外网无法连接
2. 开启远程访问方案：
```sql
-- 改root允许任意IP登录
UPDATE mysql.user SET host = '%' WHERE user = 'root';
FLUSH PRIVILEGES;
```
3. 服务器需放行 **3306** 端口防火墙。

---

## 六、安全规范（背诵考点）
1. 禁止生产环境 `root@%` 全开远程
2. 不同业务创建**独立普通用户**，最小权限分配
3. 严禁弱密码，定期修改密码
4. 业务用户只给 DML/DQL 权限，不给 `DROP`/`ALTER` 高危权限
5. 不用的账号及时删除，防止越权访问

---

## 七、高频简答题考点
1. 为什么新建用户授权后无法立即使用？
> 需要执行 `FLUSH PRIVILEGES;` 刷新权限。

2. `localhost` 和 `%` 区别？
> `localhost` 仅本地连接；`%` 代表所有IP，支持远程连接。

3. 企业权限设计原则？
> 最小权限原则，按需分配，避免超权操作。

---
