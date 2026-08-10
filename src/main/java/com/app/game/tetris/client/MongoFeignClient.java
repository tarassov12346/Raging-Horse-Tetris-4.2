package com.app.game.tetris.client;

import com.app.game.tetris.model.SavedGame;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@FeignClient(name = "mongo-service", // Имя микросервиса Mongo из Eureka
        contextId = "mongoClient")
public interface MongoFeignClient {

    @DeleteMapping("/delete")
    void cleanSavedGameMongodb(@RequestParam("playerName") String playerName);

    // БЫЛО: @GetMapping("/prepare")
    // СТАЛО:
    @PostMapping("/prepare")
    void prepareMongoDBForNewPLayer(@RequestParam("playerName") String playerName);

    @DeleteMapping("/delete_image")
    void cleanImageMongodb(@RequestParam("playerName") String playerName,
                           @RequestParam("fileName") String fileName);

}
