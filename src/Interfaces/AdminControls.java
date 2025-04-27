package Interfaces;

import model.User;

import java.util.List;

public interface AdminControls {
    boolean addUser(User user);

    boolean removeUser(String userId);

    boolean modifyUser(User user);

    List<User> searchUsers(String criteria);
}