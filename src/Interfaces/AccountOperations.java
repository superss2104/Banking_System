package Interfaces;

import model.Account;

import java.math.BigDecimal;

public interface AccountOperations {

    Account createAccount(String username, String accountType, BigDecimal initialBalance);

    boolean updateAccount(String accountId, String newAccountType, BigDecimal newBalance, String newStatus);

    boolean updateAccount(String accountNumber, String newDetails);

    boolean deleteAccount(String accountNumber);

    boolean suspendAccount(String accountNumber);

    boolean reactivateAccount(String accountNumber);

    BigDecimal getAccountBalance(String accountNumber);

    String getAccountDetails(String accountNumber);

    boolean updateAccount(String accountId, BigDecimal newBalance);
}
