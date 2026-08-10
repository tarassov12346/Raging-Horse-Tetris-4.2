package com.app.game.tetris.client;

import com.app.game.tetris.model.Users;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "users-service", // Имя из настроек микросервиса Users
        contextId = "userClient")
public interface UserClient {
    //NO USAGE
}
