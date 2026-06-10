package com.app.game.tetris.gameserviceImpl;

import com.app.game.tetris.client.GameClient;
import com.app.game.tetris.gameservice.GameService;
import com.app.service.grpc.Empty;
import com.app.service.grpc.GameIntegrationServiceGrpc;
import com.app.service.grpc.GameRecordRequest;
import com.app.service.grpc.PlayerRequest;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class GameServiceImpl implements GameService {

    @GrpcClient("game-service")
    private GameIntegrationServiceGrpc.GameIntegrationServiceBlockingStub gameStub;

    private final GameClient gameClient; // Декларативный Feign-клиент для удалений

    public GameServiceImpl(GameClient gameClient) {
        this.gameClient = gameClient;
    }

    @Override
    public String getGameData(String playerName) {
        try {
            log.info("🛰️ Запрос gRPC статистики для игрока: {}", playerName);

            var request = PlayerRequest.newBuilder().setPlayerName(playerName).build();
            var res = gameStub.getGameScore(request);

            // 🔥 ОПТИМИЗАЦИЯ: Безопасная сборка через нативный JSONObject (исключает ошибки синтаксиса JSON)
            JSONObject json = new JSONObject();
            json.put("bestplayer", res.getBestPlayer());
            json.put("bestscore", res.getBestScore());
            json.put("playerbestscore", res.getPlayerBestScore());
            json.put("playerAttemptsNumber", res.getPlayerAttemptsNumber());

            return json.toString();
        } catch (Exception e) {
            log.error("❌ Ошибка gRPC при получении данных игрока {}: {}", playerName, e.getMessage());
            return "{}";
        }
    }

    @Override
    public List<com.app.game.tetris.model.Game> getAllGames() {
        log.info("📦 Запрос полного списка игр через gRPC");
        try {
            var response = gameStub.getAllGames(Empty.newBuilder().build());

            // 🔥 ЗАЩИТА ОТ THREAD PINNING: Убираем лог из каждой итерации маппинга
            List<com.app.game.tetris.model.Game> games = response.getGamesList().stream()
                    .map(g -> {
                        var game = new com.app.game.tetris.model.Game(g.getPlayerName(), g.getPlayerScore());
                        game.setId(g.getId());
                        return game;
                    })
                    .toList();

            log.info("🎯 Успешно смапплено {} игр из gRPC-канала", games.size());
            return games;

        } catch (Exception e) {
            log.error("❌ Ошибка при получении списка игр по gRPC: {}", e.getMessage());
            return List.of();
        }
    }

    // 🔥 ИСПРАВЛЕНИЕ: Убираем @Async, так как метод уже изолирован в Loom-экзекуторе контроллера
    @Override
    public void deleteGameData(String playerName) {
        try {
            log.info("🗑️ Вызов каскадного удаления Feign для игрока: {}", playerName);
            gameClient.deleteGameData(playerName);
        } catch (Exception e) {
            log.error("❌ Ошибка Feign при каскадном удалении данных: {}", e.getMessage());
        }
    }

    // 🔥 ИСПРАВЛЕНИЕ: Убираем @Async, управление потоком полностью контролирует loomExecutor контроллера
    @Override
    public void doRecord(com.app.game.tetris.model.Game game) {
        try {
            log.info("🏆 gRPC: Сохранение рекорда для игрока: {}", game.getPlayerName());

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
