package com.app.game.tetris.gameserviceImpl;

import com.app.game.tetris.gameservice.GameService;
import com.app.game.tetris.model.Game;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GameServiceImpl implements GameService {
    @Autowired
    @LoadBalanced
    protected RestTemplate restTemplate;

    // Базовый путь теперь всегда ведет на Гейтвей + префикс нужного роута
    private final String GATEWAY_URL = "http://gateway-service/game-service";

    public GameServiceImpl() {
    }

    @Override
    public String getGameData(String playerName) {
        return restTemplate.getForObject(GATEWAY_URL + "/score?playerName={playerName}", String.class, playerName);
    }

    @Override
    public List<Game> getAllGames() {
        ResponseEntity<Game[]> response =
                restTemplate.getForEntity(GATEWAY_URL + "/games", Game[].class);
        return new ArrayList<>(Arrays.stream(response.getBody()).toList());
    }

    @Override
    public void deleteGameData(String playerName) {
        restTemplate.delete(GATEWAY_URL + "/delete?playerName={playerName}", playerName);
    }

    @Override
    public void doRecord(Game game) {
        restTemplate.postForObject(GATEWAY_URL + "/record", game, Game.class);
    }

    @Override
    public Set<Game> getAllBestResults(List<Game> playersList) {
        Set<Game> highestScoringPlayers = new HashSet<>();
        playersList.sort(Comparator.comparingInt(Game::getPlayerScore).reversed());
        highestScoringPlayers.addAll(playersList);
        return highestScoringPlayers;
    }
}
