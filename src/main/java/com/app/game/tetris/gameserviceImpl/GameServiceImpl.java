package com.app.game.tetris.gameserviceImpl;

import com.app.game.tetris.client.GameClient;
import com.app.game.tetris.gameservice.GameService;
import com.app.service.grpc.Empty;
import com.app.service.grpc.GameIntegrationServiceGrpc;
import com.app.service.grpc.GameRecordRequest;
import com.app.service.grpc.PlayerRequest;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class GameServiceImpl implements GameService {
    @GrpcClient("game-service")
    private GameIntegrationServiceGrpc.GameIntegrationServiceBlockingStub gameStub;

    private final GameClient gameClient; // Для record и delete

    public GameServiceImpl(GameClient gameClient) {
        this.gameClient = gameClient;
    }

    @Override
    public CompletableFuture<String> getGameData(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("🛰️ Запрос gRPC статистики для игрока: {}", playerName);

                // 1. Делаем gRPC вызов
                var request = PlayerRequest.newBuilder().setPlayerName(playerName).build();
                var res = gameStub.getGameScore(request);

                // 2. Формируем JSON-строку вручную (как того хочет JSONObject в контроллере)
                // Важно: ключи "bestplayer" и "bestscore" должны быть маленькими буквами
                return String.format(
                        "{\"bestplayer\":\"%s\",\"bestscore\":%d,\"playerbestscore\":%d,\"playerAttemptsNumber\":%d}",
                        res.getBestPlayer(),
                        res.getBestScore(),
                        res.getPlayerBestScore(),
                        res.getPlayerAttemptsNumber()
                );
            } catch (Exception e) {
                log.error("❌ Ошибка gRPC при получении данных: {}", e.getMessage());
                // Возвращаем пустой объект, чтобы JSONObject не упал при парсинге
                return "{}";
            }
        });
    }

    @Override
    public List<com.app.game.tetris.model.Game> getAllGames() {
        log.info("📦 Запрос полного списка игр через gRPC");
        try {
            // 1. Делаем вызов к микросервису через gRPC стаб
            var response = gameStub.getAllGames(Empty.newBuilder().build());

            // 2. Превращаем gRPC-ответы в объекты модели твоего приложения
            return response.getGamesList().stream()
                    .map(g -> {
                        log.info("🔍 Маппинг игры: id={}, player={}", g.getId(), g.getPlayerName());
                        // Создаем объект Game (используем конструктор: playerName, playerScore)
                        var game = new com.app.game.tetris.model.Game(g.getPlayerName(), g.getPlayerScore());

                        // Устанавливаем ID (в gRPC это int64, в Java Long — должно совпасть)
                        game.setId(g.getId());

                        return game;
                    })
                    .toList();

        } catch (Exception e) {
            log.error("❌ Ошибка при получении списка игр по gRPC: {}", e.getMessage());
            // Возвращаем пустой список, чтобы контроллер и админка не "упали"
            return List.of();
        }
    }

    @Async
    @Override
    public void deleteGameData(String playerName) {
        gameClient.deleteGameData(playerName);
    }

    @Async
    @Override
    public void doRecord(com.app.game.tetris.model.Game game) {
        try {
            log.info("🏆 gRPC: Сохранение рекорда...");

            // ВАЖНО: используем именно GameRecordRequest (как в новом .proto)
            var request = GameRecordRequest.newBuilder()
                    .setPlayerName(game.getPlayerName())
                    .setPlayerScore(game.getPlayerScore())
                    .build();

            gameStub.doRecord(request);
            log.info("✅ Рекорд успешно отправлен через gRPC");
        } catch (Exception e) {
            log.error("❌ Ошибка gRPC при сохранении рекорда: {}", e.getMessage());
        }
    }
}

