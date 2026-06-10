package com.app.game.tetris.mongoserviceImpl;

import com.app.game.tetris.client.MongoFeignClient;
import com.app.game.tetris.model.SavedGame;
import com.app.game.tetris.mongoservice.MongoService;
import com.app.service.grpc.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MongoServiceImpl implements MongoService {

    private final String shotsPath;
    private final MongoFeignClient mongoFeignClient;

    @GrpcClient("mongo-service")
    private SnapshotServiceGrpc.SnapshotServiceBlockingStub snapshotStub;

    public MongoServiceImpl(@Value("${shotsPath}") String shotsPath, MongoFeignClient mongoFeignClient) {
        this.shotsPath = shotsPath;
        this.mongoFeignClient = mongoFeignClient;
    }

    @Override
    public void loadSnapShotIntoMongodb(String playerName, String fileName) {
        String pathToShots = System.getProperty("user.dir") + shotsPath;
        try {
            byte[] data = Files.readAllBytes(Path.of(pathToShots + fileName + ".jpg"));

            SnapshotRequest request = SnapshotRequest.newBuilder()
                    .setPlayerName(playerName)
                    .setFileName(fileName)
                    .setData(ByteString.copyFrom(data))
                    .build();

            log.info("🛰 Отправка скриншота {} через gRPC для игрока {}", fileName, playerName);

            // 🔥 ОПТИМИЗАЦИЯ: Принудительный дедлайн на 10 секунд для защиты от зависания потоков Loom
            SnapshotResponse response = snapshotStub
                    .withDeadlineAfter(10, TimeUnit.SECONDS)
                    .uploadSnapShot(request);

            if (response.getSuccess()) {
                log.info("✅ gRPC ответ: {}", response.getMessage());
            }
        } catch (IOException e) {
            log.error("❌ Ошибка файловой системы при чтению скриншота: {}", e.getMessage());
        } catch (Exception e) {
            log.error("❌ Ошибка gRPC при отправке скриншота {}: {}", fileName, e.getMessage());
        }
    }

    @Override
    public void prepareMongoDBForNewPLayer(String playerName) {
        try {
            log.info("🪄 Подготовка БД для игрока: {}", playerName);
            mongoFeignClient.prepareMongoDBForNewPLayer(playerName);
        } catch (Exception e) {
            log.error("❌ Ошибка Feign при подготовке БД для {}: {}", playerName, e.getMessage());
        }
    }

    @Override
    public void cleanSavedGameMongodb(String playerName) {
        try {
            mongoFeignClient.cleanSavedGameMongodb(playerName);
        } catch (Exception e) {
            log.error("❌ Ошибка Feign при очистке сохраненной игры для {}: {}", playerName, e.getMessage());
        }
    }

    @Override
    public void saveGame(SavedGame savedGame) {
        List<String> rows = Arrays.stream(savedGame.getCells())
                .map(String::new)
                .toList();

        SaveGameRequest request = SaveGameRequest.newBuilder()
                .setPlayerName(savedGame.getPlayerName())
                .setPlayerScore(savedGame.getPlayerScore())
                .addAllRows(rows)
                .build();

        try {
            // 🔥 ОПТИМИЗАЦИЯ: Дедлайн на 5 секунд для сохранения текстовой матрицы
            snapshotStub.withDeadlineAfter(5, TimeUnit.SECONDS).saveGame(request);
            log.info("💾 Игра игрока {} успешно сохранена через gRPC дамп", savedGame.getPlayerName());
        } catch (Exception e) {
            log.error("❌ Ошибка gRPC при сохранении игры для {}: {}", savedGame.getPlayerName(), e.getMessage());
        }
    }

    @Override
    public Optional<SavedGame> gameRestart(String playerName) {
        log.info("🔄 Запрос загрузки игры через gRPC для: {}", playerName);

        GetSavedGameRequest request = GetSavedGameRequest.newBuilder()
                .setPlayerName(playerName)
                .build();

        try {
            // 🔥 ОПТИМИЗАЦИЯ: Дедлайн 5 секунд на восстановление стейта
            GetSavedGameResponse response = snapshotStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .getSavedGame(request);

            if (response.getFound()) {
                char[][] cells = response.getRowsList().stream()
                        .map(String::toCharArray)
                        .toArray(char[][]::new);

                return Optional.of(new SavedGame(response.getPlayerName(), response.getPlayerScore(), cells));
            }
        } catch (Exception e) {
            log.error("❌ Ошибка gRPC при загрузке сохраненной игры для {}: {}", playerName, e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void cleanImageMongodb(String playerName, String fileName) {
        try {
            mongoFeignClient.cleanImageMongodb(playerName, fileName);
        } catch (Exception e) {
            log.error("❌ Ошибка Feign при удалении медиа контента для {}: {}", playerName, e.getMessage());
        }
    }

    @Override
    public void loadMugShotIntoMongodb(String playerName, byte[] data) {
        try {
            MugShotRequest request = MugShotRequest.newBuilder()
                    .setPlayerName(playerName)
                    .setData(ByteString.copyFrom(data))
                    .build();

            // 🔥 ОПТИМИЗАЦИЯ: Дедлайн 10 секунд на тяжелую загрузку аватара
            snapshotStub.withDeadlineAfter(10, TimeUnit.SECONDS).uploadMugShot(request);
            log.info("👤 MugShot успешно отправлен через gRPC для {}", playerName);
        } catch (Exception e) {
            log.error("❌ Ошибка gRPC при загрузке аватара MugShot для {}: {}", playerName, e.getMessage());
        }
    }

    @Override
    public byte[] loadByteArrayFromMongodb(String playerName, String fileName) {
        DownloadRequest request = DownloadRequest.newBuilder()
                .setPlayerName(playerName)
                .setFileName(fileName)
                .build();
        try {
            // 🔥 ОПТИМИЗАЦИЯ: Защитный дедлайн на скачивание тяжелой графики из базы
            var response = snapshotStub.withDeadlineAfter(10, TimeUnit.SECONDS).downloadBytes(request);

            // Защитный барьер: если прилетел пустой пакет, не плодим массивы в памяти
            if (response.getData() == null || response.getData().isEmpty()) {
                return new byte[0];
            }

            return response.getData().toByteArray();
        } catch (Exception e) {
            log.error("❌ Ошибка gRPC при скачивании бинарных данных ({}) для {}: {}", fileName, playerName, e.getMessage());
            return new byte[0]; // Возвращаем пустой массив, защищая контроллер от краша
        }
    }
}
