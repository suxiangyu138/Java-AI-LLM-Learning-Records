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