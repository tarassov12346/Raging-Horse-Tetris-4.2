package com.app.game.tetris.mongoserviceImpl;

import com.app.game.tetris.client.MongoFeignClient;
import com.app.game.tetris.model.SavedGame;
import com.app.game.tetris.mongoservice.MongoService;
import com.app.service.grpc.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Service
@Slf4j // Добавим для красоты логирования
public class MongoServiceImpl implements MongoService {

    private final String shotsPath;
    private final MongoFeignClient mongoFeignClient;

    // Внедряем gRPC заглушку (Stub)
    // "mongo-service" — это имя из properties выше
    @GrpcClient("mongo-service")
    private SnapshotServiceGrpc.SnapshotServiceBlockingStub snapshotStub;

    public MongoServiceImpl(@Value("${shotsPath}") String shotsPath, MongoFeignClient mongoFeignClient) {
        this.shotsPath = shotsPath;
        this.mongoFeignClient = mongoFeignClient;
    }

    @Async
    @Override
    public void loadSnapShotIntoMongodb(String playerName, String fileName) {
        String pathToShots = System.getProperty("user.dir") + shotsPath;
        try {
            // 1. Читаем файл в массив байтов
            byte[] data = Files.readAllBytes(Path.of(pathToShots + fileName + ".jpg"));

            // 2. Формируем gRPC запрос (наш Гагарин)
            SnapshotRequest request = SnapshotRequest.newBuilder()
                    .setPlayerName(playerName)
                    .setFileName(fileName)
                    .setData(ByteString.copyFrom(data)) // Бинарная передача!
                    .build();

            log.info("🛰 Отправка скриншота {} через gRPC для игрока {}", fileName, playerName);

            // 3. Выполняем вызов
            SnapshotResponse response = snapshotStub.uploadSnapShot(request);

            if (response.getSuccess()) {
                log.info("✅ gRPC ответ: {}", response.getMessage());
            }

        } catch (IOException e) {
            log.error("❌ Ошибка чтения файла: {}", e.getMessage());
        } catch (Exception e) {
            log.error("❌ Ошибка gRPC вызова: {}", e.getMessage());
        }
    }

    // Этот метод теперь выполнится в отдельном виртуальном потоке
    @Async
    @Override
    public void prepareMongoDBForNewPLayer(String playerName) {
        log.info("🪄 Асинхронная подготовка БД для игрока: {}", playerName);
        mongoFeignClient.prepareMongoDBForNewPLayer(playerName);
    }

    @Async
    @Override
    public void cleanSavedGameMongodb(String playerName) {
        mongoFeignClient.cleanSavedGameMongodb(playerName);
    }

    // Остальные методы (saveGame, gameRestart и т.д.) лучше оставить синхронными,
    // так как обычно нам нужен их результат для продолжения логики,
    // ЛИБО они вызываются внутри уже запущенного виртуального потока в контроллере.

    @Override
    public void saveGame(SavedGame savedGame) {
        mongoFeignClient.saveGame(savedGame);
    }

    @Override
    public Optional<SavedGame> gameRestart(String playerName) {
        return mongoFeignClient.gameRestart(playerName);
    }

    @Override
    public void cleanImageMongodb(String playerName, String fileName) {
        mongoFeignClient.cleanImageMongodb(playerName, fileName);
    }

    @Override
    public void loadMugShotIntoMongodb(String playerName, byte[] data) {
        MugShotRequest request = MugShotRequest.newBuilder()
                .setPlayerName(playerName)
                .setData(ByteString.copyFrom(data))
                .build();
        snapshotStub.uploadMugShot(request);
        log.info("👤 MugShot отправлен через gRPC для {}", playerName);
    }

    @Override
    public byte[] loadByteArrayFromMongodb(String playerName, String fileName) {
        DownloadRequest request = DownloadRequest.newBuilder()
                .setPlayerName(playerName)
                .setFileName(fileName)
                .build();
        return snapshotStub.downloadBytes(request).getData().toByteArray();
    }
}
