package com.app.game.tetris.client;

import com.app.game.tetris.model.Users;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "users-service", // Имя из настроек микросервиса Users
        contextId = "userClient")
public interface UserClient {
    @PostMapping("/save")
    boolean saveUser(@RequestBody Users newUser);

    @DeleteMapping("/delete")
    void deleteUser(@RequestParam("userId") Long userId);

    @GetMapping("/findId")
    Users findUserById(@RequestParam("userId") Long userId);

    @GetMapping("/findName")
    Users findUserByUserName(@RequestParam("userName") String userName);

    @GetMapping("/users")
    List<Users> getAllUsers();

    @GetMapping("/isEmpty")
    boolean isRolesDBEmpty();

    @GetMapping("/prepareRolesDB")
    void prepareRolesDB();

    @GetMapping("/prepareUserDB")
    void prepareUserDB();
}
