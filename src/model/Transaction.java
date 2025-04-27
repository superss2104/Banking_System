package model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {
    private final StringProperty transactionId;
    private final StringProperty fromAccountId;
    private final StringProperty toAccountId;
    private final ObjectProperty<BigDecimal> amount;
    private final StringProperty type;
    private final StringProperty status;
    private final ObjectProperty<LocalDateTime> timestamp;
    private final StringProperty description;

    // General constructor (used for transfers)
    public Transaction(String transactionId, String fromAccountId, String toAccountId,
                       String type, BigDecimal amount, String status, LocalDateTime timestamp,
                       String description) {
        this.transactionId = new SimpleStringProperty(transactionId);
        this.fromAccountId = new SimpleStringProperty(fromAccountId);
        this.toAccountId = new SimpleStringProperty(toAccountId);
        this.amount = new SimpleObjectProperty<>(amount);
        this.type = new SimpleStringProperty(type);
        this.status = new SimpleStringProperty(status);
        this.timestamp = new SimpleObjectProperty<>(timestamp);
        this.description = new SimpleStringProperty(description);
    }

    // Constructor for deposits (toAccountId only)
    public Transaction(String transactionId, String toAccountId, BigDecimal amount,
                       String description, LocalDateTime timestamp, boolean isDeposit) {
        this(transactionId, null, toAccountId, TransactionType.DEPOSIT.name(),
                amount, TransactionStatus.COMPLETED.name(), timestamp, description);
    }

    // Constructor for withdrawals (fromAccountId only)
    public Transaction(String transactionId, String fromAccountId, BigDecimal amount,
                       String description, LocalDateTime timestamp) {
        this(transactionId, fromAccountId, null, TransactionType.WITHDRAWAL.name(),
                amount, TransactionStatus.COMPLETED.name(), timestamp, description);
    }

    // Getters
    public String getTransactionId() {
        return transactionId.get();
    }

    public String getFromAccountId() {
        return fromAccountId.get();
    }

    public String getToAccountId() {
        return toAccountId.get();
    }

    public BigDecimal getAmount() {
        return amount.get();
    }

    public String getType() {
        return type.get();
    }

    public String getStatus() {
        return status.get();
    }

    public void setStatus(String name) {
        this.status.set(name);
    }

    public LocalDateTime getTimestamp() {
        return timestamp.get();
    }

    public String getDescription() {
        return description.get();
    }

    // Property getters
    public StringProperty transactionIdProperty() {
        return transactionId;
    }

    public StringProperty fromAccountIdProperty() {
        return fromAccountId;
    }

    public StringProperty toAccountIdProperty() {
        return toAccountId;
    }

    public ObjectProperty<BigDecimal> amountProperty() {
        return amount;
    }

    public StringProperty typeProperty() {
        return type;
    }

    public StringProperty statusProperty() {
        return status;
    }

    public ObjectProperty<LocalDateTime> timestampProperty() {
        return timestamp;
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRANSFER, PAYMENT, REFUND, FEE, INTEREST
    }

    public enum TransactionStatus {
        INITIATED, PROCESSING, PENDING, COMPLETED, FAILED, CANCELLED, REFUNDED
    }
}