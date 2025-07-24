package com.app.game.tetris.daoservice;

import com.app.game.tetris.model.User;

import java.util.List;

public interface DaoUserService {

    List<User> getAllUsers();

    boolean deleteUser(Long userId);

    User findUserById(Long userId);

    User findUserByUserName(String userName);

    boolean saveUser(User user);

    boolean isRolesDBEmpty();

    void prepareRolesDB();

    void prepareUserDB();
}
