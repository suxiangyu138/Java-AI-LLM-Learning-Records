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