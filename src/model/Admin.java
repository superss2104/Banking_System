package model;

import java.time.LocalDateTime;

public class Admin extends User {


    public Admin(String username, String password, String firstName, String lastName,
                 String email, String phone, String role, LocalDateTime createdAt) {
        super(username, password, firstName, lastName, email, phone, role, createdAt);
    }

    @Override
    public String getUserType() {
        return "ADMIN";
    }

    @Override
    public String toString() {
        return "Admin{" +
                "username=" + getUsername() +
                ", firstName=" + getFirstName() +
                ", lastName=" + getLastName() +
                ", email=" + getEmail() +
                ", phone=" + getPhone() +
                ", role=" + getRole() +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}