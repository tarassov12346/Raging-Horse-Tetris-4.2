package com.app.game.tetris.controller;

import com.app.game.tetris.displayservice.DisplayService;
import com.app.game.tetris.dto.PlayerAttemptsDTO;
import com.app.game.tetris.dto.PlayerProfileDTO;
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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.io.OutputStream;
import java.security.Principal;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import java.util.concurrent.TimeUnit;

@Controller
public class TetrisController {
    private static final Logger log = LoggerFactory.getLogger(TetrisController.class);

    private final PlayGameService playGameService;
    private final SimpMessagingTemplate template;
    private final UsersService usersService;
    private final GameArtefactService gameArtefactService;
    private final GameService gameService;
    private final MongoService mongoService;
    private final DisplayService displayService;

    public TetrisController(PlayGameService playGameService, SimpMessagingTemplate template, UsersService usersService, GameArtefactService gameArtefactService, GameService gameService, MongoService mongoService, DisplayService displayService) {
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
        if (principal == null) {
            log.error("❌ Unauthorized WS access attempt: Principal is null");
            return;
        }

        // 1. Сохраняем полный адрес для отправки (ID:USERNAME)
        // Именно это значение Spring использует как ключ для поиска WebSocket-сессии
        String destinationId = principal.getName();

        // 2. Распаковываем данные для логики сервисов
        String[] parts = destinationId.split(":");
        String userId = parts[0];
        String username = (parts.length > 1) ? parts[1] : "Player_" + userId;

        log.info("🚀 Игрок вошел. ID: [{}], Name: [{}]. Адрес доставки: [{}]", userId, username, destinationId);

        try {
            // Инициализация состояния (тут нужны чистые ID и Name)
            var gameState = playGameService.initiateState(username, userId);
            playGameService.setState(gameState, userId);

            // Асинхронная подготовка БД
            CompletableFuture.runAsync(() -> mongoService.prepareMongoDBForNewPLayer(username));

            // --- ВАЖНО: В DisplayService передаем destinationId (полную строку) ---

            // 3. Отправка стартового визуала
            displayService.sendGameToBeDisplayed(gameState.getGame(), template, destinationId);

            // 4. Получение и отправка рекордов
            String rawData = gameService.getGameData(username);
            if (rawData != null && !rawData.isEmpty()) {
                JSONObject jsonGameData = new JSONObject(rawData);
                displayService.sendDaoGameToBeDisplayed(
                        playGameService.createGame(
                                jsonGameData.optString("bestplayer", "None"),
                                jsonGameData.optInt("bestscore", 0)
                        ),
                        template, destinationId
                );
            }

            log.info("✅ Инициализация завершена для: {}", username);

        } catch (Exception e) {
            log.error("💥 Ошибка в обработчике hello для {}: {}", userId, e.getMessage());
        }
    }


    @MessageMapping("/profile")
    public void profile(Principal principal) {
        String destinationId = principal.getName();

        // 2. Распаковываем данные для логики сервисов
        String[] parts = destinationId.split(":");
        String userId = parts[0];
        String username = (parts.length > 1) ? parts[1] : "Player_" + userId;


        // 2. Берем имя игрока из уже существующего состояния (State) в памяти
        String playerName = playGameService.getState(userId).getGame().getPlayerName();

        // 3. Запрашиваем статистику из GameService по имени
        JSONObject jsonGameData = new JSONObject(gameService.getGameData(playerName));

        // 1. Отправляем профиль (имя и лучший счет)
        this.template.convertAndSendToUser(destinationId, "/queue/playerStat",
                new PlayerProfileDTO(playerName, jsonGameData.getInt("playerbestscore")));

        // 2. Отправляем попытки (только число)
        this.template.convertAndSendToUser(destinationId, "/queue/playerAttemptsNumber",
                new PlayerAttemptsDTO(jsonGameData.getInt("playerAttemptsNumber")));
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
        // 1. (Опционально) Проверка прав: только если Гейтвей прокидывает роли в Principal
        // Если проверки на уровне Gateway достаточно, можно пропустить.

        // 2. Запрос списка пользователей из микросервиса USERS
        // Здесь usersService должен быть не репозиторием, а Feign-клиентом или Rest-шаблоном
        List<Users> allUsersList = usersService.getAllUsers();

        allUsersList.forEach(user -> {
            // Формируем объект для фронта (пароль лучше вообще не слать, даже зашифрованный)
            String rolesString = user.getRoles().stream()
                    .map(Roles::getName)
                    .collect(Collectors.joining(";"));

            this.template.convertAndSend("/topic/users",
                    new Users(user.getId(), user.getUsername(), "[PROTECTED]", rolesString, user.getRoles()));
        });


        gameService.getAllBestResults(gameService.getAllGames())
                .forEach(game -> System.out.println(game.getPlayerName()+"  "+ game.getPlayerScore()));

        // 3. Своя родная статистика Тетриса остается как была
        gameService.getAllBestResults(gameService.getAllGames())
                .forEach(game -> this.template.convertAndSend("/topic/results", game));
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
        boolean isAdmin = usersService.findUserByUserName(username).getRoles().stream() .anyMatch(a -> a.getName().equals("ROLE_ADMIN"));
        System.out.println(isAdmin);
        if (!isAdmin) {
            this.template.convertAndSendToUser(destinationId,"/queue/alert", "You are not admin!");
            return;
        }
        // 2. Проверка "не удаляй себя" по ID (теперь оба значения — это ID)
        String currentAdminId = userId;
        if (currentAdminId.equals(String.valueOf(targetUserId))) {
            this.template.convertAndSendToUser(destinationId,"/queue/alert", "You cannot delete yourself!");
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
        // 1. ПОЛУЧАЕМ ID МГНОВЕННО (из токена, проброшенного интерцептором)
        String destinationId = principal.getName();
        // 2. Распаковываем данные для логики сервисов
        String[] parts = destinationId.split(":");
        String userId = parts[0];
        // 2. Достаем текущее состояние игры из мапы по userId
        var currentState = playGameService.getState(userId);
        if (currentState == null && !moveId.equals("start")) {
            return; // Защита: если игры нет, а кнопки жмут
        }
        switch (moveId) {
            case "start" -> {
                // При старте один раз берем данные из GameService (статистика)
                String playerName = currentState.getGame().getPlayerName();
                JSONObject jsonGameData = new JSONObject(gameService.getGameData(playerName));

                displayService.sendDaoGameToBeDisplayed(
                        playGameService.createGame(jsonGameData.getString("bestplayer"), jsonGameData.getInt("bestscore")),
                        template, destinationId
                );
 /*               Thread.ofVirtual().start(() -> {
                    try {
                        while (!Thread.interrupted()) {
                            displayService.sendStateToBeDisplayed(playGameService, gameService, playGameService.getSEService(userId), template, destinationId);
                            Thread.sleep(java.time.Duration.ofMillis(1000)); // Замена через Duration
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });*/

  /*              // Создаем планировщик, который для каждой задачи спавнит виртуальный поток
                var scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());

                playGameService.setSEService(scheduler, userId);
                playGameService.getSEService(userId).scheduleAtFixedRate(
                        () -> displayService.sendStateToBeDisplayed(playGameService, gameService, playGameService.getSEService(userId), template, destinationId),
                0, 1000, TimeUnit.MILLISECONDS
);*/


                // Запуск таймера падения фигур
                playGameService.setSEService(Executors.newScheduledThreadPool(1), userId);
                playGameService.getSEService(userId).scheduleAtFixedRate(
                        () -> displayService.sendStateToBeDisplayed(playGameService, gameService, playGameService.getSEService(userId), template, destinationId),
                        0, 1000, TimeUnit.MILLISECONDS
                );
            }
            // В движениях (1, 2, 3, 4) теперь НЕТ запросов к UsersService и GameService
            case "1" -> playGameService.setState(playGameService.rotateState(currentState, userId), userId);
            case "2" -> playGameService.setState(playGameService.moveLeftState(currentState, userId), userId);
            case "3" -> playGameService.setState(playGameService.moveRightState(currentState, userId), userId);
            case "4" -> playGameService.setState(playGameService.dropDownState(currentState, userId), userId);
        }
        // Отправляем обновленное состояние на фронт (кроме "start", там свой планировщик)
        if (!moveId.equals("start")) {
            displayService.sendStateToBeDisplayed(playGameService, gameService, playGameService.getSEService(userId), template, destinationId);
        }
    }

    @MessageMapping("/save")
    public void gameSave(Principal principal) {
        // 1. ПОЛУЧАЕМ ID МГНОВЕННО (из токена, проброшенного интерцептором)
        String destinationId = principal.getName();
        // 2. Распаковываем данные для логики сервисов
        String[] parts = destinationId.split(":");
        String userId = parts[0];

        // 2. Останавливаем игровой цикл (падающие фигуры) для этого юзера
        if (playGameService.getSEService(userId) != null) {
            playGameService.getSEService(userId).shutdown();
        }

        // 3. Получаем текущее состояние и сохраняем
        var currentState = playGameService.getState(userId);
        if (currentState != null) {
            SavedGame savedGame = playGameService.saveGame(currentState.getGame(), currentState);

            // Отправляем в микросервис Монго
            mongoService.saveGame(savedGame);

            // Уведомляем фронт о сохранении
            displayService.sendSavedStateToBeDisplayed(playGameService, gameService,
                    playGameService.getSEService(userId), template, destinationId);
        }
    }

    @MessageMapping("/restart")
    public void gameRestart(Principal principal) {
        // 1. ПОЛУЧАЕМ ID МГНОВЕННО (из токена, проброшенного интерцептором)
        String destinationId = principal.getName();
        // 2. Распаковываем данные для логики сервисов
        String[] parts = destinationId.split(":");
        String userId = parts[0];

        // 2. Берем текущее состояние (в нем уже сидит имя игрока)
        var currentState = playGameService.getState(userId);
        if (currentState == null) return; // Защита, если игры еще нет

        String playerName = currentState.getGame().getPlayerName();

        // 3. Запрашиваем у Монго сохраненную игру по имени
        mongoService.gameRestart(playerName).ifPresent(savedGame -> {
            // Восстанавливаем состояние из сохраненки
            playGameService.setState(playGameService.recreateStateFromSavedGame(savedGame, userId), userId);

            // Отправляем обновленный экран на фронт
            displayService.sendStateToBeDisplayed(
                    playGameService,
                    gameService,
                    playGameService.getSEService(userId),
                    template,
                    destinationId
            );
        });
    }

    @MessageMapping("/snapShot")
    public void makeSnapShot(Principal principal) {
        // 1. ПОЛУЧАЕМ ID МГНОВЕННО (из токена, проброшенного интерцептором)
        String destinationId = principal.getName();
        // 2. Распаковываем данные для логики сервисов
        String[] parts = destinationId.split(":");
        String userId = parts[0];

        // 2. Берем состояние из памяти (State уже должен быть создан в /hello)
        var currentState = playGameService.getState(userId);
        if (currentState == null) return;

        String playerName = currentState.getGame().getPlayerName();

        // 3. Получаем данные о рекордах из GameService по имени игрока
        JSONObject jsonGameData = new JSONObject(gameService.getGameData(playerName));
        String bestPlayer = jsonGameData.getString("bestplayer");
        int bestScore = jsonGameData.getInt("bestscore");

        // 4. Делаем обычный скриншот
        gameArtefactService.makeDesktopSnapshot("deskTopSnapShot", playGameService, currentState, bestPlayer, bestScore);
        mongoService.cleanImageMongodb(playerName, "deskTopSnapShot");
        mongoService.loadSnapShotIntoMongodb(playerName, "deskTopSnapShot");

        // 5. Если побит личный рекорд — делаем "Best" скриншот
        if (currentState.getGame().getPlayerScore() >= jsonGameData.getInt("playerbestscore")) {
            gameArtefactService.makeDesktopSnapshot("deskTopSnapShotBest", playGameService, currentState, bestPlayer, bestScore);
            mongoService.cleanImageMongodb(playerName, "deskTopSnapShotBest");
            mongoService.loadSnapShotIntoMongodb(playerName, "deskTopSnapShotBest");
        }

        // 6. Отправляем финальный экран на фронт
        displayService.sendFinalStateToBeDisplayed(playGameService, gameService,
                playGameService.getSEService(userId), template, destinationId);
    }

}
