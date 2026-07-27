# 03 - PL/SQL 存储过程与函数

> 🎯 PL/SQL 是 Oracle 的杀手锏——将 SQL 的声明式能力和过程式编程结合，写出数据库端的业务逻辑。Package 的概念让代码模块化和封装性远超 MySQL 存储过程

---

## 目录

1. [基础语法](#1-基础语法)
2. [存储过程与函数](#2-存储过程与函数)
3. [Package 与异常处理](#3-package-与异常处理)

---

## 1. 基础语法

```sql
-- PL/SQL 块结构
DECLARE
    v_name VARCHAR2(100);       -- 变量声明
    v_count NUMBER := 0;        -- 带默认值
BEGIN
    SELECT name INTO v_name FROM users WHERE id = 1;
    DBMS_OUTPUT.PUT_LINE('Name: ' || v_name);
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('User not found');
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error: ' || SQLERRM);
END;
/

-- 变量类型（%TYPE 自动匹配列类型，推荐！）
DECLARE
    v_name users.name%TYPE;     -- 自动匹配 users.name 的类型
    v_row users%ROWTYPE;        -- 匹配整行
BEGIN
    SELECT * INTO v_row FROM users WHERE id = 1;
    DBMS_OUTPUT.PUT_LINE(v_row.name || ', ' || v_row.email);
END;
```

## 2. 存储过程与函数

```sql
-- 存储过程（无返回值，用于执行操作）
CREATE OR REPLACE PROCEDURE transfer_money(
    p_from_account IN NUMBER,
    p_to_account   IN NUMBER,
    p_amount       IN NUMBER
) IS
    v_balance NUMBER;
BEGIN
    SELECT balance INTO v_balance FROM accounts WHERE id = p_from_account FOR UPDATE;
    IF v_balance < p_amount THEN
        RAISE_APPLICATION_ERROR(-20001, '余额不足');
    END IF;
    UPDATE accounts SET balance = balance - p_amount WHERE id = p_from_account;
    UPDATE accounts SET balance = balance + p_amount WHERE id = p_to_account;
    COMMIT;
END;
/

-- 调用
EXEC transfer_money(1, 2, 500);
```

```sql
-- 函数（有返回值）
CREATE OR REPLACE FUNCTION get_total_revenue(p_year NUMBER) RETURN NUMBER IS
    v_total NUMBER;
BEGIN
    SELECT NVL(SUM(amount), 0) INTO v_total
    FROM orders WHERE EXTRACT(YEAR FROM order_date) = p_year;
    RETURN v_total;
END;
/

-- 调用
SELECT get_total_revenue(2026) FROM dual;
```

## 3. Package 与异常处理

```sql
-- Package = 接口 + 实现分离（类似 Java 的 interface + class）
-- 规范（包头）
CREATE OR REPLACE PACKAGE user_pkg IS
    PROCEDURE create_user(p_name VARCHAR2, p_email VARCHAR2);
    FUNCTION get_count RETURN NUMBER;
END user_pkg;
/

-- 实现（包体）
CREATE OR REPLACE PACKAGE BODY user_pkg IS
    PROCEDURE create_user(p_name VARCHAR2, p_email VARCHAR2) IS
    BEGIN
        INSERT INTO users(name, email) VALUES (p_name, p_email);
        COMMIT;
    EXCEPTION
        WHEN DUP_VAL_ON_INDEX THEN
            RAISE_APPLICATION_ERROR(-20002, '用户名已存在');
    END;

    FUNCTION get_count RETURN NUMBER IS
        v_cnt NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_cnt FROM users;
        RETURN v_cnt;
    END;
END user_pkg;
/

-- 调用
EXEC user_pkg.create_user('Alice', 'a@example.com');
SELECT user_pkg.get_count FROM dual;
```

### 常用异常

| 异常名 | 触发条件 |
|-------|---------|
| `NO_DATA_FOUND` | SELECT INTO 没有数据 |
| `TOO_MANY_ROWS` | SELECT INTO 返回多行 |
| `DUP_VAL_ON_INDEX` | 唯一约束冲突 |
| `VALUE_ERROR` | 类型转换/截断错误 |
| `OTHERS` | 所有其他异常 |

## 核心要点回顾

- `%TYPE` 和 `%ROWTYPE` 自动匹配列类型（不要硬编码）
- 存储过程 `EXEC` 调用，函数在 SQL 中直接使用
- Package = PL/SQL 的模块化方案（接口+实现+状态）
- `FOR UPDATE` 锁定行（防止并发转账）
- `RAISE_APPLICATION_ERROR` 抛自定义异常（-20001 到 -20999）

## 参考资料

1. Oracle PL/SQL Language Reference
