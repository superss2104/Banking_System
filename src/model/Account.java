package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Account {
    private final String accountId;
    private final String username;
    private final LocalDateTime createdAt;
    private String accountType;
    private BigDecimal balance;
    private LocalDateTime lastUpdated;
    private String status;

    public Account(String accountId, String username, String accountType, BigDecimal balance,
                   LocalDateTime createdAt, LocalDateTime lastUpdated, String status) {
        this.accountId = accountId;
        this.username = username;
        this.accountType = accountType;
        this.balance = balance;
        this.createdAt = createdAt;
        this.lastUpdated = lastUpdated;
        this.status = status;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getUsername() {
        return username;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String newAccountType) {
        this.accountType = newAccountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // For Transaction compatibility
    public String getAccountNumber() {
        return accountId;
    }

    public boolean isActive() {
        return status.equals("ACTIVE");
    }

    public enum AccountType {
        SAVINGS,
        CHECKING,
        FIXED_DEPOSIT
    }
}