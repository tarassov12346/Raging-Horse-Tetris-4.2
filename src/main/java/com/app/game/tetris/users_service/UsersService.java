package com.app.game.tetris.users_service;

import com.app.game.tetris.model.Users;

import java.util.List;

public interface UsersService {
    void deleteUser(Long userId);
    Users findUserById(Long userId);
    Users findUserByUserName(String userName);
    List<Users> getAllUsers();

}
