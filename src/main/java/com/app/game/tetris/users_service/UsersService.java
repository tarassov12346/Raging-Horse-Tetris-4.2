package com.app.game.tetris.users_service;

import com.app.game.tetris.model.User;

import java.util.List;

public interface UsersService {
    boolean saveUser(User newUser);
    void deleteUser(Long userId);
    User findUserById(Long userId);
    User findUserByUserName(String userName);
    List<User> getAllUsers();
    boolean isRolesDBEmpty();
    void prepareRolesDB();
    void prepareUserDB();
}
