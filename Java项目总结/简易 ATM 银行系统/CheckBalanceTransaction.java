public class CheckBalanceTransaction extends Transaction {
    @Override
    public void execute(Account account) {
        System.out.println("当前余额：" + account.getBalance());
    }
}