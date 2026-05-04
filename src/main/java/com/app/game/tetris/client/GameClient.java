package com.app.game.tetris.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;

// name = имя сервиса в Eureka (gateway-service)
// path = префикс, который ты вешал в RestTemplate
@FeignClient(name = "game-service", // Имя микросервиса с Postgres в Eureka
        contextId = "gameClient")
public interface GameClient {

    @DeleteMapping("/delete")
    void deleteGameData(@RequestParam("playerName") String playerName);

}
