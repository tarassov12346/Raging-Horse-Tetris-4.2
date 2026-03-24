package com.app.game.tetris.client;

import com.app.game.tetris.model.Game;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// name = имя сервиса в Eureka (gateway-service)
// path = префикс, который ты вешал в RestTemplate
@FeignClient(name = "game-service", // Имя микросервиса с Postgres в Eureka
        contextId = "gameClient")
public interface GameClient {

    @GetMapping("/score")
    String getGameData(@RequestParam("playerName") String playerName);

    @GetMapping("/games")
    List<Game> getAllGames();

    @DeleteMapping("/delete")
    void deleteGameData(@RequestParam("playerName") String playerName);

    @PostMapping("/record")
    void doRecord(@RequestBody Game game);
}
