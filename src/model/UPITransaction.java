package model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UPITransaction extends Transaction {
    private final StringProperty fromUpiId;
    private final StringProperty toUpiId;

    // Constructor for creating a new UPI transaction
    public UPITransaction(String transactionId, String fromUpiId, String toUpiId,
                          String fromAccountId, String toAccountId, BigDecimal amount,
                          Transaction.TransactionStatus status, LocalDateTime timestamp,
                          String remarks) {
        super(transactionId, fromAccountId, toAccountId, TransactionType.PAYMENT.name(),
                amount, status.name(), timestamp, remarks);
        this.fromUpiId = new SimpleStringProperty(fromUpiId);
        this.toUpiId = new SimpleStringProperty(toUpiId);
    }

    // Constructor for retrieving from the transactions table
    public UPITransaction(String transactionId, String fromAccountId, String toAccountId,
                          BigDecimal amount, Transaction.TransactionStatus status,
                          LocalDateTime timestamp, String description) {
        super(transactionId, fromAccountId, toAccountId, TransactionType.PAYMENT.name(),
                amount, status.name(), timestamp, description);
        this.fromUpiId = new SimpleStringProperty(null);
        this.toUpiId = new SimpleStringProperty(null);
    }

    // UPI-specific getters
    public String getFromUpiId() {
        return fromUpiId.get();
    }

    public StringProperty fromUpiIdProperty() {
        return fromUpiId;
    }

    public String getToUpiId() {
        return toUpiId.get();
    }

    public StringProperty toUpiIdProperty() {
        return toUpiId;
    }
}