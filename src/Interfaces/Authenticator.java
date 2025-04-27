package Interfaces;

public interface Authenticator {
    boolean authenticate(String username, String password);

    void registerUser(String username, String password, String firstName,
                      String lastName, String email, String phoneNumber);

    void resetPassword(String username);
}
