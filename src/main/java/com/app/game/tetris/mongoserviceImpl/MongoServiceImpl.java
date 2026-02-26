package com.app.game.tetris.mongoserviceImpl;

import com.app.game.tetris.model.SavedGame;
import com.app.game.tetris.mongoservice.MongoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Service
public class MongoServiceImpl implements MongoService {

    @Value("${shotsPath}")
    String shotsPath;

    @Autowired
    @LoadBalanced
    protected RestTemplate restTemplate;

    // Стучимся в Гейтвей по его имени в Эврике + префикс из роута №3
    private final String GATEWAY_MONGO_URL = "http://gateway-service/mongo-service";

    @Override
    public void saveGame(SavedGame savedGame) {
        restTemplate.postForObject(GATEWAY_MONGO_URL + "/save", savedGame, SavedGame.class);
    }

    @Override
    public Optional<SavedGame> gameRestart(String playerName) {
        return Optional.ofNullable(restTemplate.getForObject(GATEWAY_MONGO_URL + "/restart?playerName={playerName}", SavedGame.class, playerName));
    }

    @Override
    public void cleanSavedGameMongodb(String playerName) {
        restTemplate.delete(GATEWAY_MONGO_URL + "/delete?playerName={playerName}", playerName);
    }

    @Override
    public void prepareMongoDBForNewPLayer(String playerName) {
        restTemplate.getForObject(GATEWAY_MONGO_URL + "/prepare?playerName={playerName}", String.class, playerName);
    }

    @Override
    public void cleanImageMongodb(String playerName, String fileName) {
        restTemplate.delete(GATEWAY_MONGO_URL + "/delete_image?playerName={playerName}&fileName={fileName}", playerName, fileName);
    }

    @Override
    public byte[] loadByteArrayFromMongodb(String playerName, String fileName) {
        ResponseEntity<byte[]> response =
                restTemplate.getForEntity(GATEWAY_MONGO_URL + "/bytes?playerName={playerName}&fileName={fileName}", byte[].class, playerName, fileName);
        return response.getBody();
    }

    @Override
    public void loadMugShotIntoMongodb(String playerName, byte[] data) {
        restTemplate.postForObject(GATEWAY_MONGO_URL + "/mugShot?playerName={playerName}", data, byte[].class, playerName);
    }

    @Override
    public void loadSnapShotIntoMongodb(String playerName, String fileName) {
        String pathToShots = System.getProperty("user.dir") + shotsPath;
        byte[] data = new byte[0];
        try {
            data = Files.readAllBytes(Path.of(pathToShots + fileName + ".jpg"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        restTemplate.postForObject(GATEWAY_MONGO_URL + "/snapShot?playerName={playerName}&fileName={fileName}", data, byte[].class, playerName, fileName);
    }
}
