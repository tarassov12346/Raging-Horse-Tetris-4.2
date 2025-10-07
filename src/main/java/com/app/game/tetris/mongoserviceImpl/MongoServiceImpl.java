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


    @Autowired
    @LoadBalanced
    protected RestTemplate restTemplate;

    @Override
    public void saveGame(SavedGame savedGame) {
        restTemplate.postForObject("http://mongo-service/save", savedGame, SavedGame.class);
    }

    @Override
    public Optional<SavedGame> gameRestart(String playerName) {
        return Optional.ofNullable(restTemplate.getForObject("http://mongo-service" + "/restart?playerName={playerName}", SavedGame.class, playerName));
    }

    @Override
    public void cleanSavedGameMongodb(String playerName) {
        restTemplate.delete("http://mongo-service" + "/delete?playerName={playerName}", playerName);
    }

    @Override
    public void prepareMongoDBForNewPLayer(String playerName) {
        restTemplate.getForObject("http://mongo-service" + "/prepare?playerName={playerName}", String.class, playerName);
    }

    @Override
    public void cleanImageMongodb(String playerName, String fileName) {
        restTemplate.delete("http://mongo-service" + "/delete_image?playerName={playerName}&fileName={fileName}", playerName, fileName);
    }

    @Override
    public byte[] loadByteArrayFromMongodb(String playerName, String fileName) {
        ResponseEntity<byte[]> response =
                restTemplate.getForEntity("http://mongo-service" + "/bytes?playerName={playerName}&fileName={fileName}", byte[].class, playerName, fileName);
        return response.getBody();
    }

    @Override
    public void loadMugShotIntoMongodb(String playerName, byte[] data) {
        restTemplate.postForObject("http://mongo-service/mugShot?playerName={playerName}", data, byte[].class, playerName);
    }
}
