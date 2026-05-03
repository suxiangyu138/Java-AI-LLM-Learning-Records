可以，下面我直接给你一个**能交作业、能体现面向对象/继承/多态/异常处理/流程控制**的 Java 控制台版“简易 ATM 银行系统”完整方案。ATM 系统的核心业务通常包含登录、查询余额、存款、取款和退出，这些都是常见的基础功能范围 ；而控制台输入场景里，`Scanner` 读取到错误类型输入时很容易触发 `InputMismatchException`，因此项目里确实应该加入输入异常处理 。 [github](https://github.com/haseth/Java-OOP-project-on-ATM-machine)

## 项目设计

这个项目我按你现在的学习阶段，设计成**非常适合本科 Java 面向对象训练**的结构：`Account` 表示账户基类，`SavingAccount` 和 `CreditAccount` 体现继承，`Transaction` 抽象类和不同交易子类体现多态，`ATMSystem` 负责菜单流程控制，另外再补上自定义异常类完成异常处理 。这种 ATM 模拟项目本身就非常适合承载封装、继承、多态和业务异常处理，比如余额不足、密码错误、金额非法、输入格式错误等 。 [scribd](https://www.scribd.com/document/978620212/Final-Oop-Mp)

### 推荐类结构

| 类名 | 作用 |
|---|---|
| `Account` | 账户父类，封装卡号、户名、密码、余额 |
| `SavingAccount` | 储蓄账户，普通取款规则 |
| `CreditAccount` | 信用账户，可透支 |
| `Transaction` | 交易父类，定义 `execute()` |
| `DepositTransaction` | 存款业务 |
| `WithdrawTransaction` | 取款业务 |
| `CheckBalanceTransaction` | 查询余额业务 |
| `BankException` | 业务异常父类 |
| `InsufficientFundsException` | 余额不足异常 |
| `InvalidAmountException` | 金额非法异常 |
| `ATMSystem` | 主流程控制类，菜单、登录、输入处理 |
| `Main` | 程序入口 |

## 完整代码

按你的偏好，我直接给**极简可运行代码**。默认用多个 `.java` 文件，最适合课程作业展示。

### 1. Account.java

```java
public abstract class Account {
    private String cardId;
    private String name;
    private String password;
    protected double balance;

    public Account(String cardId, String name, String password, double balance) {
        this.cardId = cardId;
        this.name = name;
        this.password = password;
        this.balance = balance;
    }

    public String getCardId() {
        return cardId;
    }

    public String getName() {
        return name;
    }

    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    public double getBalance() {
        return balance;
    }

    public void deposit(double amount) throws InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException("存款金额必须大于0");
        }
        balance += amount;
    }

    public abstract void withdraw(double amount) throws BankException;
}
```

### 2. SavingAccount.java

```java
public class SavingAccount extends Account {
    public SavingAccount(String cardId, String name, String password, double balance) {
        super(cardId, name, password, balance);
    }

    @Override
    public void withdraw(double amount) throws BankException {
        if (amount <= 0) {
            throw new InvalidAmountException("取款金额必须大于0");
        }
        if (amount > balance) {
            throw new InsufficientFundsException("余额不足");
        }
        balance -= amount;
    }
}
```

### 3. CreditAccount.java

```java
public class CreditAccount extends Account {
    private double overdraftLimit;

    public CreditAccount(String cardId, String name, String password, double balance, double overdraftLimit) {
        super(cardId, name, password, balance);
        this.overdraftLimit = overdraftLimit;
    }

    @Override
    public void withdraw(double amount) throws BankException {
        if (amount <= 0) {
            throw new InvalidAmountException("取款金额必须大于0");
        }
        if (amount > balance + overdraftLimit) {
            throw new InsufficientFundsException("超过可用额度");
        }
        balance -= amount;
    }
}
```

### 4. BankException.java

```java
public class BankException extends Exception {
    public BankException(String message) {
        super(message);
    }
}
```

### 5. InsufficientFundsException.java

```java
public class InsufficientFundsException extends BankException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
```

### 6. InvalidAmountException.java

```java
public class InvalidAmountException extends BankException {
    public InvalidAmountException(String message) {
        super(message);
    }
}
```

### 7. Transaction.java

```java
public abstract class Transaction {
    public abstract void execute(Account account) throws BankException;
}
```

### 8. DepositTransaction.java

```java
public class DepositTransaction extends Transaction {
    private double amount;

    public DepositTransaction(double amount) {
        this.amount = amount;
    }

    @Override
    public void execute(Account account) throws BankException {
        account.deposit(amount);
        System.out.println("存款成功，当前余额：" + account.getBalance());
    }
}
```

### 9. WithdrawTransaction.java

```java
public class WithdrawTransaction extends Transaction {
    private double amount;

    public WithdrawTransaction(double amount) {
        this.amount = amount;
    }

    @Override
    public void execute(Account account) throws BankException {
        account.withdraw(amount);
        System.out.println("取款成功，当前余额：" + account.getBalance());
    }
}
```

### 10. CheckBalanceTransaction.java

```java
public class CheckBalanceTransaction extends Transaction {
    @Override
    public void execute(Account account) {
        System.out.println("当前余额：" + account.getBalance());
    }
}
```

### 11. ATMSystem.java

```java
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class ATMSystem {
    private List<Account> accounts = new ArrayList<>();
    private Scanner sc = new Scanner(System.in);

    public ATMSystem() {
        accounts.add(new SavingAccount("1001", "张三", "123456", 1000));
        accounts.add(new CreditAccount("1002", "李四", "123456", 500, 2000));
    }

    public void start() {
        System.out.println("====== 欢迎进入ATM银行系统 ======");
        while (true) {
            Account account = login();
            if (account != null) {
                menu(account);
            }
        }
    }

    private Account login() {
        while (true) {
            System.out.print("请输入卡号：");
            String cardId = sc.next();
            System.out.print("请输入密码：");
            String password = sc.next();

            for (Account account : accounts) {
                if (account.getCardId().equals(cardId) && account.checkPassword(password)) {
                    System.out.println("登录成功，欢迎您：" + account.getName());
                    return account;
                }
            }
            System.out.println("卡号或密码错误，请重新输入");
        }
    }

    private void menu(Account account) {
        while (true) {
            System.out.println("\n===== ATM菜单 =====");
            System.out.println("1. 查询余额");
            System.out.println("2. 存款");
            System.out.println("3. 取款");
            System.out.println("4. 退出登录");
            System.out.print("请选择：");

            try {
                int choice = sc.nextInt();
                switch (choice) {
                    case 1:
                        Transaction t1 = new CheckBalanceTransaction();
                        t1.execute(account);
                        break;
                    case 2:
                        System.out.print("请输入存款金额：");
                        double depositAmount = sc.nextDouble();
                        Transaction t2 = new DepositTransaction(depositAmount);
                        t2.execute(account);
                        break;
                    case 3:
                        System.out.print("请输入取款金额：");
                        double withdrawAmount = sc.nextDouble();
                        Transaction t3 = new WithdrawTransaction(withdrawAmount);
                        t3.execute(account);
                        break;
                    case 4:
                        System.out.println("已退出登录");
                        return;
                    default:
                        System.out.println("无效选择，请重新输入");
                }
            } catch (InputMismatchException e) {
                System.out.println("输入格式错误，请输入数字");
                sc.nextLine();
            } catch (BankException e) {
                System.out.println("操作失败：" + e.getMessage());
            } catch (Exception e) {
                System.out.println("系统异常，请稍后再试");
                sc.nextLine();
            }
        }
    }
}
```

### 12. Main.java

```java
public class Main {
    public static void main(String[] args) {
        new ATMSystem().start();
    }
}
```

## 这个项目体现了什么

这个版本已经完整体现了你老师常要求的几个点：账户信息私有化是**封装**，`SavingAccount` 和 `CreditAccount` 继承 `Account` 是**继承**，`Transaction t = new DepositTransaction(...)` 这种父类引用指向子类对象并调用重写方法就是**多态** 。同时，`InputMismatchException` 处理用户输入错误，自定义 `BankException` 体系处理业务错误，这也符合 ATM 项目中“输入异常 + 业务异常”并行处理的常见做法 。 [linkedin](https://www.linkedin.com/posts/gangabhavani-punju_java-programming-studentdeveloper-activity-7446100021919326208-khUh)

### 对应知识点映射

- 面向对象：类、对象、封装、构造方法 [blog.csdn](https://blog.csdn.net/weixin_33072399/article/details/144413002)
- 继承：`SavingAccount`、`CreditAccount` 继承 `Account` [blog.csdn](https://blog.csdn.net/weixin_33072399/article/details/144413002)
- 多态：`Transaction` 父类统一调用不同交易子类 [blog.csdn](https://blog.csdn.net/weixin_33072399/article/details/144413002)
- 异常处理：`try-catch` + 自定义异常 + 输入异常 [github](https://github.com/haseth/Java-OOP-project-on-ATM-machine)
- 流程控制：`while`、`switch`、`if` 实现登录和菜单循环 [scribd](https://www.scribd.com/document/978620212/Final-Oop-Mp)

## 运行效果示例

ATM 模拟系统最基础的交互流程通常就是登录后执行查询、存款、取款、退出等菜单操作 。你这个项目运行起来，大致会是下面这样： [scribd](https://www.scribd.com/document/978620212/Final-Oop-Mp)

```text
====== 欢迎进入ATM银行系统 ======
请输入卡号：1001
请输入密码：123456
登录成功，欢迎您：张三

===== ATM菜单 =====
1. 查询余额
2. 存款
3. 取款
4. 退出登录
请选择：1
当前余额：1000.0

===== ATM菜单 =====
1. 查询余额
2. 存款
3. 取款
4. 退出登录
请选择：3
请输入取款金额：2000
操作失败：余额不足
```

## 你交作业时怎么讲

你答辩或者写实验报告时，可以这样介绍：本系统是一个基于 Java 控制台的简易 ATM 银行系统，实现了账户登录、查询余额、存款、取款和退出登录等功能，这些也是 ATM 模拟项目最常见的核心功能 。在实现过程中，系统通过账户父类与不同账户子类体现继承，通过交易父类与不同业务子类体现多态，并结合 `InputMismatchException` 和自定义业务异常完成输入错误与业务错误处理 。 [linkedin](https://www.linkedin.com/posts/gangabhavani-punju_java-programming-studentdeveloper-activity-7446100021919326208-khUh)

## 你还能继续升级的方向

如果你想把这个项目做成**简历级别**，可以继续加下面这些模块；很多 ATM 练习项目也会把“交易记录、转账、修改密码、多账户管理”作为扩展功能方向 。 [edu.51cto](https://edu.51cto.com/file/3939.html)

- 转账功能
- 修改密码
- 开户功能
- 删除账户
- 交易流水
- 数据持久化到文件
- 后续升级为 MySQL 版本

