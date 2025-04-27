package database;

import utils.EncryptionUtil;

public class HashPassword {
    public static void main(String[] args) {
        String newPassword = "admin"; // Replace with your desired password
        String hashedPassword = EncryptionUtil.hashPassword(newPassword);
        System.out.println("New hashed password: " + hashedPassword);
    }
}