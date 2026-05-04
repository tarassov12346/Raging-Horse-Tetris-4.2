package com.app.game.tetris.controller;

import com.app.game.tetris.displayservice.DisplayService;
import com.app.game.tetris.dto.*;
import com.app.game.tetris.gameArtefactservice.GameArtefactService;
import com.app.game.tetris.gameservice.GameService;
import com.app.game.tetris.model.Roles;
import com.app.game.tetris.model.SavedGame;
import com.app.game.tetris.model.Users;
import com.app.game.tetris.mongoservice.MongoService;
import com.app.game.tetris.tetriservice.PlayGameService;
import com.app.game.tetris.users_service.UsersService;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.io.OutputStream;
import java.security.Principal;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

@Controller
public class TetrisController {
    private static final Logger log = LoggerFactory.getLogger(TetrisController.class);

    private final TaskScheduler taskScheduler; // Помечаем final для надежности
    private final PlayGameService playGameService;
    private final SimpMessagingTemplate template;
    private final UsersService usersService;
    private final GameArtefactService gameArtefactService;
    private final GameService gameService;
    private final MongoService mongoService;
    private final DisplayService displayService;

    // В Spring 4.3+ аннотация @Autowired над конструктором не обязательна, если он один
    public TetrisController(
            TaskScheduler taskScheduler,
            PlayGameService playGameService,
            SimpMessagingTemplate template,
            UsersService usersService,
            GameArtefactService gameArtefactService,
            GameService gameService,
            MongoService mongoService,
            DisplayService displayService
    ) {
        this.taskScheduler = taskScheduler;
        this.playGameService = playGameService;
        this.template = template;
        this.usersService = usersService;
        this.gameArtefactService = gameArtefactService;
        this.gameService = gameService;
        this.mongoService = mongoService;
        this.displayService = displayService;
    }

    @MessageMapping("/hello")
    public void hello(Principal principal) {
        if (principal == null) return;
        String destinationId = principal.getName();
        String userId = destinationId.split(":")[0];
        String username = (destinationId.contains(":")) ? destinationId.split(":")[1] : "Player_" + userId;
        try {
            // 1. Инициализация (синхронно, так как это база для состояния)
            var gameState = playGameService.initiateState(username, userId);
            playGameService.setState(gameState, userId);

            // 2. Метод помечен @Async в сервисе, поэтому он сам уйдет в виртуальный поток.
            // Нам не нужно писать Thread.startVirtualThread здесь.
            mongoService.prepareMongoDBForNewPLayer(username);

            // 3. Сразу отправляем игроку игровое поле
            displayService.sendGameToBeDisplayed(gameState.getGame(), template, destinationId);

            // 4. Асинхронно запрашиваем статистику.
            // Основной поток метода завершится, а когда придет ответ — отправим рекорды.
            gameService.getGameData(username).thenAccept(rawData -> {
                if (rawData != null && !rawData.isEmpty()) {
                    try {
                        JSONObject jsonGameData = new JSONObject(rawData);
                        GameDataDTO gameDataRecord = new GameDataDTO(
                                jsonGameData.optString("bestplayer", "None"),
                                jsonGameData.optInt("bestscore", 0)
                        );
                        displayService.sendDaoGameToBeDisplayed(gameDataRecord, template, destinationId);
                        log.info("📊 Рекорды для {} успешно отправлены", username);
                    } catch (Exception e) {
                        log.error("💥 Ошибка парсинга JSON для {}: {}", username, e.getMessage());
                    }
                }
            });
            log.info("🚀 Основная инициализация завершена для: {}", username);
        } catch (Exception e) {
            log.error("💥 Ошибка в hello: {}", e.getMessage());
        }
    }

    @MessageMapping("/profile")
    public void profile(Principal principal) {
        if (principal == null) return;

        String destinationId = principal.getName();
        String userId = destinationId.split(":")[0];

        // Получаем стейт из мапы (память)
        var state = playGameService.getState(userId);
        if (state == null) {
            log.warn("⚠️ Попытка доступа к профилю без активного стейта для ID: {}", userId);
            return;
        }
        String playerName = state.getGame().getPlayerName();
        // 1. Асинхронный вызов микросервиса через CompletableFuture
        gameService.getGameData(playerName).thenAccept(rawData -> {
            if (rawData != null && !rawData.isEmpty()) {
                try {
                    JSONObject jsonGameData = new JSONObject(rawData);

                    // 2. Отправляем профиль (имя и лучший счет)
                    this.template.convertAndSendToUser(destinationId, "/queue/playerStat",
                            new PlayerProfileDTO(
                                    playerName,
                                    jsonGameData.optInt("playerbestscore", 0)
                            ));
                    // 3. Отправляем попытки (только число)
                    this.template.convertAndSendToUser(destinationId, "/queue/playerAttemptsNumber",
                            new PlayerAttemptsDTO(
                                    jsonGameData.optInt("playerAttemptsNumber", 0)
                            ));

                    log.info("👤 Профиль игрока {} отправлен", playerName);
                } catch (Exception e) {
                    log.error("💥 Ошибка обработки профиля для {}: {}", playerName, e.getMessage());
                }
            }
        });
    }

    @MessageMapping("/upload")
    public void upload(String imageBase64Stringsep, Principal principal) {
        String destinationId = principal.getName();
        // 2. Распаковываем данные для логики сервисов
        String[] parts = destinationId.split(":");
        String userId = parts[0];

        // 2. Достаем имя игрока из состояния игры (State)
        String playerName = playGameService.getState(userId).getGame().getPlayerName();

        // 3. Очищаем старое фото и грузим новое в Mongo по имени игрока
        mongoService.cleanImageMongodb(playerName, "");

        // Декодируем и сохраняем
        byte[] imageBytes = Base64.getDecoder().decode(imageBase64Stringsep);
        mongoService.loadMugShotIntoMongodb(playerName, imageBytes);
    }

    @GetMapping("/getPhoto")
    public void getPhoto(@RequestHeader("X-User-Id") String userId, HttpServletResponse response) {
        String playerName = playGameService.getState(userId).getGame().getPlayerName();
        byte[] image = mongoService.loadByteArrayFromMongodb(playerName, "mugShot");
        writeImageToResponse(response, image);
    }

    @GetMapping("/getSnapShot")
    public void getSnapShot(@RequestHeader("X-User-Id") String userId, HttpServletResponse response) {
        String playerName = playGameService.getState(userId).getGame().getPlayerName();
        byte[] image = mongoService.loadByteArrayFromMongodb(playerName, "deskTopSnapShot");
        writeImageToResponse(response, image);
    }

    @GetMapping("/getSnapShotBest")
    public void getSnapShotBest(@RequestHeader("X-User-Id") String userId, HttpServletResponse response) {
        String playerName = playGameService.getState(userId).getGame().getPlayerName();
        byte[] image = mongoService.loadByteArrayFromMongodb(playerName, "deskTopSnapShotBest");
        writeImageToResponse(response, image);
    }

    // Вспомогательный метод для чистоты кода
    private void writeImageToResponse(HttpServletResponse response, byte[] image) {
        if (image == null) return;
        response.setHeader("Accept-ranges", "bytes");
        response.setContentType("image/jpeg");
        response.setContentLength(image.length);
        response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
        try (OutputStream out = response.getOutputStream()) {
            out.write(image);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @MessageMapping("/admin")
    public void admin(Principal principal) {
        // 1. Получаем список всех пользователей через Feign/Service
        List<Users> allUsersList = usersService.getAllUsers();
        allUsersList.forEach(user -> {
            // 2. Формируем строку ролей
            String rolesString = user.getRoles().stream()
                    .map(Roles::getName)
                    .collect(Collectors.joining(";"));
            // Проверяем, есть ли роль ADMIN (для флага в DTO)
            boolean isAdmin = rolesString.contains("ADMIN");
            // 3. Создаем рекорд и отправляем его
            UserResponseDTO userDto = new UserResponseDTO(
                    user.getId(),
                    user.getUsername(),
                    rolesString,
                    isAdmin
            );
            this.template.convertAndSend("/topic/users", userDto);
        });
        gameService.getAllGames()
                .forEach(game -> {
                    System.out.println("ОТПРАВКА НА ФРОНТ: " + game.getPlayerName() + " - " + game.getPlayerScore());
                    // Используем новое имя класса GameRecord
                    GameRecord recordDto = new GameRecord(
                            game.getId(),
                            game.getPlayerName(),
                            game.getPlayerScore()
                    );
                    this.template.convertAndSend("/topic/results", recordDto);
                });
    }

    @MessageMapping("/admin/{targetUserId}")
    public void deleteUser(@DestinationVariable Long targetUserId, Principal principal) {
        // 1. ПОЛУЧАЕМ ID МГНОВЕННО (из токена, проброшенного интерцептором)
        String destinationId = principal.getName();
        // 2. Распаковываем данные для логики сервисов
        String[] parts = destinationId.split(":");
        String userId = parts[0];
        String username = (parts.length > 1) ? parts[1] : "Player_" + userId;
        // 1. Проверяем роль через Authorities (которые заполнил Interceptor из JWT)
        // Либо, если ты просто прокинул заголовок X-User-Role, проверяем его.
        boolean isAdmin = usersService.findUserByUserName(username).getRoles().stream().anyMatch(a -> a.getName().equals("ROLE_ADMIN"));
        System.out.println(isAdmin);
        if (!isAdmin) {
            this.template.convertAndSendToUser(destinationId, "/queue/alert", "You are not admin!");
            return;
        }
        // 2. Проверка "не удаляй себя" по ID (теперь оба значения — это ID)
        String currentAdminId = userId;
        if (currentAdminId.equals(String.valueOf(targetUserId))) {
            this.template.convertAndSendToUser(destinationId, "/queue/alert", "You cannot delete yourself!");
            return;
        }
        // 3. Получаем имя удаляемого пользователя (нужно для очистки Mongo/GameService)
        // Здесь usersService — это Feign-клиент к микросервису USERS
        Users targetUser = usersService.findUserById(targetUserId);
        String targetUsername = targetUser.getUsername();
        // 4. Очистка игровых данных (локально в Тетрисе/Монго)
        mongoService.cleanSavedGameMongodb(targetUsername);
        mongoService.cleanImageMongodb(targetUsername, "");
        mongoService.cleanImageMongodb(targetUsername, "deskTopSnapShot");
        mongoService.cleanImageMongodb(targetUsername, "deskTopSnapShotBest");
        gameService.deleteGameData(targetUsername);
        // 5. Удаление самого пользователя в микросервисе USERS
        usersService.deleteUser(targetUserId);
        // 6. Обновляем список для всех админов
        admin(principal);
    }

    @MessageMapping("/{moveId}")
    public void gamePlayDown(@DestinationVariable String moveId, Principal principal) {
        String destinationId = principal.getName();
        String userId = destinationId.split(":")[0];
        var currentState = playGameService.getState(userId);
        if (currentState == null && !moveId.equals("start")) {
            return;
        }
        switch (moveId) {
            case "start" -> {
                String playerName = currentState.getGame().getPlayerName();
                // 1. Асинхронно запрашиваем рекорды.
                // Это не мешает немедленному запуску таймера ниже.
                gameService.getGameData(playerName).thenAccept(rawData -> {
                    if (rawData != null && !rawData.isEmpty()) {
                        try {
                            JSONObject jsonGameData = new JSONObject(rawData);
                            displayService.sendDaoGameToBeDisplayed(
                                    new GameDataDTO(
                                            jsonGameData.optString("bestplayer", "None"),
                                            jsonGameData.optInt("bestscore", 0)
                                    ),
                                    template,
                                    destinationId
                            );
                        } catch (Exception e) {
                            log.error("❌ Error parsing game data for {}: {}", playerName, e.getMessage());
                        }
                    }
                });
                // 2. Запуск таймера падения (ScheduledTask в Spring Boot 3.4
                // при включенных виртуальных потоках тоже работает эффективно)
                ScheduledFuture<?> task = taskScheduler.scheduleAtFixedRate(
                        () -> displayService.sendStateToBeDisplayed(
                                playGameService,
                                gameService,
                                template,
                                destinationId
                        ),
                        Duration.ofMillis(1000)
                );
                playGameService.setUserTask(userId, task);
            }
            // Движения остаются синхронными, так как они работают только с памятью (мапой состояний)
            case "1" -> playGameService.setState(playGameService.rotateState(currentState, userId), userId);
            case "2" -> playGameService.setState(playGameService.moveLeftState(currentState, userId), userId);
            case "3" -> playGameService.setState(playGameService.moveRightState(currentState, userId), userId);
            case "4" -> playGameService.setState(playGameService.dropDownState(currentState, userId), userId);
        }
        // Отправляем визуал (кроме start, так как там работает планировщик)
        if (!moveId.equals("start")) {
            displayService.sendStateToBeDisplayed(playGameService, gameService, template, destinationId);
        }
    }

    @MessageMapping("/save")
    public void gameSave(Principal principal) {
        String destinationId = principal.getName();
        String userId = destinationId.split(":")[0];
        // 1. Сначала забираем текущее состояние игры
        var currentState = playGameService.getState(userId);
        if (currentState != null) {
            // 2. Сохраняем логику в SavedGame
            SavedGame savedGame = playGameService.saveGame(currentState.getGame(), currentState);
            // Отправляем в Монго
            mongoService.saveGame(savedGame);
            playGameService.stopUserTask(userId);
            // Уведомляем фронт
            displayService.sendSavedStateToBeDisplayed(playGameService, template, destinationId);
        }
    }

    @MessageMapping("/restart")
    public void gameRestart(Principal principal) {
        // 1. ПОЛУЧАЕМ ID МГНОВЕННО (из токена, проброшенного интерцептором)
        String destinationId = principal.getName();
        // 2. Распаковываем данные для логики сервисов
        String[] parts = destinationId.split(":");
        String userId = parts[0];
        String playerName = (parts.length > 1) ? parts[1] : "Player_" + userId;
        log.info("RESTARTED for " + playerName);
        // 3. Запрашиваем у Монго сохраненную игру по имени
        mongoService.gameRestart(playerName).ifPresent(savedGame -> {
            // Восстанавливаем состояние из сохраненки
            playGameService.setState(playGameService.recreateStateFromSavedGame(savedGame, userId), userId);
            // Отправляем обновленный экран на фронт
            displayService.sendStateToBeDisplayed(
                    playGameService,
                    gameService,
                    template,
                    destinationId
            );
        });
    }

    @MessageMapping("/snapShot")
    public void makeSnapShot(Principal principal) {
        if (principal == null) return;
        String destinationId = principal.getName();
        String userId = destinationId.split(":")[0];
        var currentState = playGameService.getState(userId);
        if (currentState == null) return;
        String playerName = currentState.getGame().getPlayerName();
        // 1. Запрашиваем данные асинхронно
        gameService.getGameData(playerName).thenAccept(rawData -> {
            if (rawData == null || rawData.isEmpty()) return;
            try {
                JSONObject jsonGameData = new JSONObject(rawData);
                String bestPlayer = jsonGameData.optString("bestplayer", "None");
                int bestScore = jsonGameData.optInt("bestscore", 0);
                int playerBestScore = jsonGameData.optInt("playerbestscore", 0);
                // 2. Делаем обычный скриншот (в виртуальном потоке это дешево)
                gameArtefactService.makeDesktopSnapshot("deskTopSnapShot", playGameService, currentState, bestPlayer, bestScore);
                // Эти методы помечены @Async, они сами запустят свои виртуальные потоки
                mongoService.cleanImageMongodb(playerName, "deskTopSnapShot");
                mongoService.loadSnapShotIntoMongodb(playerName, "deskTopSnapShot");
                // 3. Если побит личный рекорд — делаем "Best" скриншот
                if (currentState.getGame().getPlayerScore() >= playerBestScore) {
                    gameArtefactService.makeDesktopSnapshot("deskTopSnapShotBest", playGameService, currentState, bestPlayer, bestScore);
                    mongoService.cleanImageMongodb(playerName, "deskTopSnapShotBest");
                    mongoService.loadSnapShotIntoMongodb(playerName, "deskTopSnapShotBest");
                }
                // 4. Отправляем финальный экран (теперь это не блокирует основной поток)
                displayService.sendFinalStateToBeDisplayed(playGameService, template, destinationId);
                log.info("📸 Скриншоты для {} успешно обработаны", playerName);
            } catch (Exception e) {
                log.error("💥 Ошибка при создании скриншотов для {}: {}", playerName, e.getMessage());
            }
        });
    }

    @MessageMapping("/record")
    public void makeRecord(Principal principal) {
        if (principal == null) return;
        String destinationId = principal.getName();
        String userId = destinationId.split(":")[0];
        gameService.doRecord(playGameService.getState(userId).getGame());
        log.info("record to DB is made for " + userId);
    }
}
