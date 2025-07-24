package com.app.game.tetris.gameserviceImpl;

import com.app.game.tetris.gameservice.GameService;
import com.app.game.tetris.model.Game;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class GameServiceImpl implements GameService {
    @Autowired
    @LoadBalanced
    protected RestTemplate restTemplate;

    protected String serviceUrl;

    public GameServiceImpl() {
    }

    @Override
    public String getGameData(String playerName) {
        return restTemplate.getForObject("http://game-service" + "/score?playerName={playerName}", String.class, playerName);
    }

    @Override
    public List<Game> getAllGames() {
        ResponseEntity<Game[]> response =
                restTemplate.getForEntity("http://game-service/games", Game[].class);
        return new ArrayList<>(Arrays.stream(response.getBody()).toList());
    }

    @Override
    public void deleteGameData(String playerName) {
        restTemplate.delete("http://game-service" + "/delete?playerName={playerName}", playerName);
    }

    @Override
    public void doRecord(Game game) {
        restTemplate.postForObject("http://game-service/record", game, Game.class);
    }
}
