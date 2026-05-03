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