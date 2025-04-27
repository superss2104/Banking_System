package Interfaces;

public interface TransactionOperations {

    void depositFunds(String accountId, double amount);

    void withdrawFunds(String accountId, double amount);

    default boolean transferFunds(String fromAccountId, String toAccountId, double amount) {
        withdrawFunds(fromAccountId, amount);
        depositFunds(toAccountId, amount);
        return true;
    }
}