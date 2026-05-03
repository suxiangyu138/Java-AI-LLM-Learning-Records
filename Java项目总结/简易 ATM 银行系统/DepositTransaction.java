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