package com.app.game.tetris.users_service;

import com.app.game.tetris.model.Users;

import java.util.List;

public interface UsersService {
    boolean saveUser(Users newUser);
    void deleteUser(Long userId);
    Users findUserById(Long userId);
    Users findUserByUserName(String userName);
    List<Users> getAllUsers();
    boolean isRolesDBEmpty();
    void prepareRolesDB();
    void prepareUserDB();
}
