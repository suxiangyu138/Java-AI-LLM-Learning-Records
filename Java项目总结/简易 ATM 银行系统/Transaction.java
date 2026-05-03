public abstract class Transaction {
    public abstract void execute(Account account) throws BankException;
}