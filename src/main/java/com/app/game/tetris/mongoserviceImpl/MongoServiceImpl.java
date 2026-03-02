package com.app.game.tetris.mongoserviceImpl;

import com.app.game.tetris.client.MongoFeignClient;
import com.app.game.tetris.model.SavedGame;
import com.app.game.tetris.mongoservice.MongoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Service
public class MongoServiceImpl implements MongoService {

    private final String shotsPath;
    private final MongoFeignClient mongoFeignClient;

    // Внедрение зависимостей через конструктор
    public MongoServiceImpl(@Value("${shotsPath}") String shotsPath, MongoFeignClient mongoFeignClient) {
        this.shotsPath = shotsPath;
        this.mongoFeignClient = mongoFeignClient;
    }

    @Override
    public void saveGame(SavedGame savedGame) {
        mongoFeignClient.saveGame(savedGame);
    }

    @Override
    public Optional<SavedGame> gameRestart(String playerName) {
        return mongoFeignClient.gameRestart(playerName);
    }

    @Override
    public void cleanSavedGameMongodb(String playerName) {
        mongoFeignClient.cleanSavedGameMongodb(playerName);
    }

    @Override
    public void prepareMongoDBForNewPLayer(String playerName) {
        mongoFeignClient.prepareMongoDBForNewPLayer(playerName);
    }

    @Override
    public void cleanImageMongodb(String playerName, String fileName) {
        mongoFeignClient.cleanImageMongodb(playerName, fileName);
    }

    @Override
    public byte[] loadByteArrayFromMongodb(String playerName, String fileName) {
        return mongoFeignClient.loadByteArrayFromMongodb(playerName, fileName);
    }

    @Override
    public void loadMugShotIntoMongodb(String playerName, byte[] data) {
        mongoFeignClient.loadMugShotIntoMongodb(playerName, data);
    }

    @Override
    public void loadSnapShotIntoMongodb(String playerName, String fileName) {
        String pathToShots = System.getProperty("user.dir") + shotsPath;
        try {
            byte[] data = Files.readAllBytes(Path.of(pathToShots + fileName + ".jpg"));
            mongoFeignClient.loadSnapShotIntoMongodb(playerName, fileName, data);
        } catch (IOException e) {
            // В логике RestTemplate у вас был пустой массив,
            // здесь можно либо логировать, либо бросать исключение
            e.printStackTrace();
        }
    }
}
