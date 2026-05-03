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