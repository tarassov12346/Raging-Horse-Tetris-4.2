package com.app.game.tetris.client;

import com.app.game.tetris.model.SavedGame;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@FeignClient(name = "mongo-service", // Имя микросервиса Mongo из Eureka
        contextId = "mongoClient")
public interface MongoFeignClient {

    @PostMapping("/save")
    void saveGame(@RequestBody SavedGame savedGame);

    @GetMapping("/restart")
    Optional<SavedGame> gameRestart(@RequestParam("playerName") String playerName);

    @DeleteMapping("/delete")
    void cleanSavedGameMongodb(@RequestParam("playerName") String playerName);

    @GetMapping("/prepare")
    void prepareMongoDBForNewPLayer(@RequestParam("playerName") String playerName);

    @DeleteMapping("/delete_image")
    void cleanImageMongodb(@RequestParam("playerName") String playerName,
                           @RequestParam("fileName") String fileName);

    @GetMapping("/bytes")
    byte[] loadByteArrayFromMongodb(@RequestParam("playerName") String playerName,
                                    @RequestParam("fileName") String fileName);

    @PostMapping("/mugShot")
    void loadMugShotIntoMongodb(@RequestParam("playerName") String playerName,
                                @RequestBody byte[] data);

    @PostMapping("/snapShot")
    void loadSnapShotIntoMongodb(@RequestParam("playerName") String playerName,
                                 @RequestParam("fileName") String fileName,
                                 @RequestBody byte[] data);
}
