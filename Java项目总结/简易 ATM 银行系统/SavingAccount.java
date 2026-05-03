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