package com.app.game.tetris.users_service;

import com.app.game.tetris.model.User;

public interface UsersService {
    boolean saveUser(User newUser);
    void deleteUser(Long userId);
    User findUserById(Long userId);
}
