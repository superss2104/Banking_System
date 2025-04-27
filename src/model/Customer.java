package model;

import Interfaces.CustomerInfo;

import java.time.LocalDateTime;

public class Customer extends User implements CustomerInfo {

    public Customer(String username, String password, String firstName, String lastName,
                    String email, String phone, String role, LocalDateTime createdAt) {
        super(username, password, firstName, lastName, email, phone, role, createdAt);
    }

    @Override
    public String getUserType() {
        return getRole(); // Supports 'USER' or 'ADMIN'
    }

    // For compatibility with UpiController, UPIServicesController, TransactionOperationsController
    public String getUserId() {
        return getUsername();
    }
}