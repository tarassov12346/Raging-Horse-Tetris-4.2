package com.app.game.tetris.mongoserviceImpl;

import com.app.game.tetris.model.SavedGame;
import com.app.game.tetris.mongoservice.MongoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
        return  Optional.of(restTemplate.getForObject("http://mongo-service" + "/restart?playerName={playerName}", SavedGame.class, playerName));
    }

    @Override
    public void cleanSavedGameMongodb(String playerName) {
        restTemplate.delete("http://mongo-service" + "/delete?playerName={playerName}", playerName);
    }
}
