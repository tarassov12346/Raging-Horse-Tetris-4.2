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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
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

    // Внедряем стандартный исполнитель задач Spring.
    // Так как в properties включен виртуальный режим, под капотом будут Virtual Threads.
    private final Executor applicationTaskExecutor;

    // В Spring 4.3+ аннотация @Autowired над конструктором не обязательна, если он один
    public TetrisController(
            TaskScheduler taskScheduler,
            PlayGameService playGameService,
            SimpMessagingTemplate template,
            UsersService usersService,
            GameArtefactService gameArtefactService,
            GameService gameService,
            MongoService mongoService,
            DisplayService displayService,
            @Qualifier("applicationTaskExecutor") Executor  applicationTaskExecutor
            ) {
        this.taskScheduler = taskScheduler;
        this.playGameService = playGameService;
        this.template = template;
        this.usersService = usersService;
        this.gameArtefactService = gameArtefactService;
        this.gameService = gameService;
        this.mongoService = mongoService;
        this.displayService = displayService;
        this.applicationTaskExecutor = applicationTaskExecutor;
    }

    @MessageMapping("/hello")
    public void hello(Principal principal) {
        if (principal == null) return;

        // Безопасный парсинг без риска получить ArrayIndexOutOfBoundsException
        String destinationId = principal.getName();
        String[] parts = destinationId.split(":");
        String userId = parts[0];
        String username = parts.length > 1 ? parts[1] : "Player_" + userId;

        try {
            // 1. СИНХРОННАЯ ИНИЦИАЛИЗАЦИЯ (Быстрая логика в памяти)
            var gameState = playGameService.initiateState(username, userId);
            playGameService.setState(gameState, userId);
            displayService.sendGameToBeDisplayed(gameState.getGame(), template, destinationId);

            // 2. АСИНХРОННЫЙ БЛОК (Все тяжелые I/O операции уходят в Виртуальный Поток)
            CompletableFuture.runAsync(() -> {
                try {
                    // Шаг А: Подготовка БД (теперь выполняется явно здесь, линейно)
                    mongoService.prepareMongoDBForNewPLayer(username);

                    // Шаг Б: Запрос gRPC данных игры
                    String rawData = gameService.getGameData(username);

                    String bestPlayer = "None";
                    int bestScore = 0;
                    if (rawData != null && !rawData.isEmpty() && !rawData.equals("{}")) {
                        JSONObject jsonGameData = new JSONObject(rawData);
                        bestPlayer = jsonGameData.optString("bestplayer", "None");
                        bestScore = jsonGameData.optInt("bestscore", 0);
                    }

                    // Шаг В: Загрузка тяжелого магшота из MongoDB
                    String avatarBase64 = "";
                    if (!"None".equals(bestPlayer)) {
                        byte[] imageBytes = mongoService.loadByteArrayFromMongodb(bestPlayer, "mugShot");
                        if (imageBytes != null && imageBytes.length > 0) {
                            avatarBase64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
                        }
                    }

                    // Шаг Г: Отправка приветствия клиенту через WebSocket
                    PlayerWelcomeDTO welcomeData = new PlayerWelcomeDTO(bestPlayer, bestScore, avatarBase64);
                    template.convertAndSendToUser(destinationId, "/queue/welcomeData", welcomeData);

                    log.info("🎯 Фоновая обработка для игрока {} успешно завершена", username);

                } catch (Exception e) {
                    log.error("💥 Ошибка в фоновом виртуальном потоке для {}: {}", username, e.getMessage(), e);
                }
            }, applicationTaskExecutor); // Передаем управляемый Spring-ом движок виртуальных потоков

            log.info("🚀 Основной поток контроллера освобожден для: {}", username);

        } catch (Exception e) {
            log.error("💥 Критическая ошибка инициализации в методе hello: {}", e.getMessage());
        }
    }

    @MessageMapping("/profile")
    public void profile(Principal principal) {
        if (principal == null) return;

        String destinationId = principal.getName();
        String userId = destinationId.split(":")[0];

        var state = playGameService.getState(userId);
        if (state == null) {
            log.warn("⚠️ Попытка доступа к профилю без активного стейта для ID: {}", userId);
            return;
        }
        String playerName = state.getGame().getPlayerName();

        // Каноничный запуск цепочки в виртуальном потоке
        CompletableFuture.runAsync(() -> {
            try {
                String rawData = gameService.getGameData(playerName);

                if (rawData != null && !rawData.isEmpty() && !rawData.equals("{}")) {
                    JSONObject jsonGameData = new JSONObject(rawData);

                    this.template.convertAndSendToUser(destinationId, "/queue/playerStat",
                            new PlayerProfileDTO(playerName, jsonGameData.optInt("playerbestscore", 0)));

                    this.template.convertAndSendToUser(destinationId, "/queue/playerAttemptsNumber",
                            new PlayerAttemptsDTO(jsonGameData.optInt("playerAttemptsNumber", 0)));

                    log.info("👤 Профиль игрока {} отправлен из виртуального потока loomExecutor", playerName);
                }
            } catch (Exception e) {
                log.error("💥 Ошибка обработки профиля для {}: {}", playerName, e.getMessage());
            }
        }, applicationTaskExecutor); // Передаем ваш единый Loom-движок
    }

    @MessageMapping("/upload")
    public void upload(String imageBase64Stringsep, Principal principal) {
        if (principal == null) return;

        // 1. Безопасный парсинг ID
        String destinationId = principal.getName();
        String userId = destinationId.split(":")[0];

        try {
            // 2. Декодируем CPU-интенсивную задачу СРАЗУ в потоке брокера.
            // Это безопасно, так как операция занимает микросекунды и не забивает Loom.
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64Stringsep);

            // 3. Уходим в виртуальный поток Spring только для сетевых I/O операций
            applicationTaskExecutor.execute(() -> {
                try {
                    var state = playGameService.getState(userId);
                    if (state == null || state.getGame() == null) {
                        log.warn("⚠️ Не удалось загрузить аватар: активная игра для ID {} не найдена", userId);
                        return;
                    }
                    String playerName = state.getGame().getPlayerName();

                    // Очищаем старое фото (Feign HTTP call — идеальный блокирующий I/O для Loom)
                    mongoService.cleanImageMongodb(playerName, "");

                    // Грузим новое фото (gRPC call — идеальный блокирующий I/O для Loom)
                    mongoService.loadMugShotIntoMongodb(playerName, imageBytes);

                    log.info("📸 Аватар для игрока {} успешно обновлен в MongoDB", playerName);
                } catch (Exception e) {
                    log.error("💥 Ошибка при обработке аватара в виртуальном потоке: {}", e.getMessage(), e);
                }
            });

        } catch (IllegalArgumentException e) {
            log.error("❌ Ошибка: присланная строка не является валидным Base64: {}", e.getMessage());
        } catch (Exception e) {
            log.error("💥 Критическая ошибка в методе upload: {}", e.getMessage());
        }
    }


    @GetMapping("/getPhoto")
    public void getPhoto(@RequestHeader("X-User-Id") String userId, HttpServletResponse response) {
        String playerName = getPlayerNameOrRespond404(userId, response);
        if (playerName == null) return; // Сервер уже ответил 404, останавливаем выполнение

        byte[] image = mongoService.loadByteArrayFromMongodb(playerName, "mugShot");
        writeImageToResponse(response, image);
    }

    @GetMapping("/getSnapShot")
    public void getSnapShot(@RequestHeader("X-User-Id") String userId, HttpServletResponse response) {
        String playerName = getPlayerNameOrRespond404(userId, response);
        if (playerName == null) return;

        byte[] image = mongoService.loadByteArrayFromMongodb(playerName, "deskTopSnapShot");
        writeImageToResponse(response, image);
    }

    @GetMapping("/getSnapShotBest")
    public void getSnapShotBest(@RequestHeader("X-User-Id") String userId, HttpServletResponse response) {
        String playerName = getPlayerNameOrRespond404(userId, response);
        if (playerName == null) return;

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

    // Единственный приватный метод проверки для всего REST-контроллера
    private String getPlayerNameOrRespond404(String userId, HttpServletResponse response) {
        var state = playGameService.getState(userId);
        // Если стейт неожиданно пропал (перезапуск сервера / таймаут сессии)
        if (state == null) {
            log.warn("⚠️ Сессионный In-Memory стейт для ID {} не найден. Возвращаем HTTP 404.", userId);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND); // Красивый статус 404 для фронтенда
            return null;
        }
        return state.getGame().getPlayerName();
    }

    @MessageMapping("/admin")
    public void admin(Principal principal) {
        if (principal == null) return;

        // 🚀 Мгновенно уходим в виртуальный поток Spring, освобождая брокер WebSocket
        applicationTaskExecutor.execute(() -> {
            try {
                // ==========================================
                // 1. ПОДГОТОВКА И ПАКЕТНАЯ ОТПРАВКА ПОЛЬЗОВАТЕЛЕЙ
                // ==========================================
                List<Users> allUsersList = usersService.getAllUsers();

                // Маппим весь список в памяти
                List<UserResponseDTO> userDtos = allUsersList.stream()
                        .map(user -> {
                            String rolesString = user.getRoles().stream()
                                    .map(Roles::getName)
                                    .collect(Collectors.joining(";"));

                            // Безопасная проверка роли без contains() по подстроке
                            boolean isAdmin = user.getRoles().stream()
                                    .anyMatch(role -> "ADMIN".equals(role.getName()));

                            return new UserResponseDTO(user.getId(), user.getUsername(), rolesString, isAdmin);
                        })
                        .collect(Collectors.toList());

                // 🔥 ОТПРАВЛЯЕМ ВЕСЬ СПИСОК ОДНИМ ПАКЕТОМ (Фронтенд скажет спасибо)
                if (!userDtos.isEmpty()) {
                    this.template.convertAndSend("/topic/users", userDtos);
                }

                // ==========================================
                // 2. ПОДГОТОВКА И ПАКЕТНАЯ ОТПРАВКА РЕЗУЛЬТАТОВ (gRPC)
                // ==========================================
                var grpcGames = gameService.getAllGames();

                List<GameRecord> recordDtos = grpcGames.stream()
                        .map(game -> new GameRecord(game.getId(), game.getPlayerName(), game.getPlayerScore()))
                        .collect(Collectors.toList());

                // 🔥 ОТПРАВЛЯЕМ ВСЕ РЕЗУЛЬТАТЫ ОДНИМ ПАКЕТОМ
                if (!recordDtos.isEmpty()) {
                    this.template.convertAndSend("/topic/results", recordDtos);
                }

                // Один чистый лог на весь тяжелый процесс вместо сотен логов в цикле
                log.info("🎯 Админ-панель успешно обновлена. Отправлено {} пользователей и {} игровых записей для {}",
                        userDtos.size(), recordDtos.size(), principal.getName());

            } catch (Exception e) {
                log.error("💥 Ошибка при обработке данных админки в виртуальном потоке: {}", e.getMessage(), e);
            }
        });
    }

    @MessageMapping("/admin/{targetUserId}")
    public void deleteUser(@DestinationVariable Long targetUserId, Principal principal) {
        if (principal == null) return;

        // 🚀 Мгновенно уходим в фоновый виртуальный поток, полностью разгружая брокер WebSocket
        applicationTaskExecutor.execute(() -> {
            try {
                String destinationId = principal.getName();
                String[] parts = destinationId.split(":");
                String userId = parts[0];
                String username = (parts.length > 1) ? parts[1] : "Player_" + userId;

                // 1. Безопасная проверка роли через единый стандарт (без префиксов-паразитов)
                boolean isAdmin = usersService.findUserByUserName(username).getRoles().stream()
                        .anyMatch(role -> "ADMIN".equals(role.getName()) || "ROLE_ADMIN".equals(role.getName()));

                if (!isAdmin) {
                    this.template.convertAndSendToUser(destinationId, "/queue/alert", "You are not admin!");
                    return;
                }

                // 2. Проверка "не удаляй себя"
                if (userId.equals(String.valueOf(targetUserId))) {
                    this.template.convertAndSendToUser(destinationId, "/queue/alert", "You cannot delete yourself!");
                    return;
                }

                log.info("🎯 Администратор {} инициировал удаление пользователя ID: {}", username, targetUserId);

                // 3. Получаем имя удаляемого пользователя через Feign (Блокирующий I/O в Loom)
                Users targetUser = usersService.findUserById(targetUserId);
                if (targetUser == null) {
                    this.template.convertAndSendToUser(destinationId, "/queue/alert", "Target user not found!");
                    return;
                }
                String targetUsername = targetUser.getUsername();

                // 4. Каскадная очистка игровых данных (gRPC / Feign вызовы эффективно паркуют поток Loom)
                mongoService.cleanSavedGameMongodb(targetUsername);
                mongoService.cleanImageMongodb(targetUsername, "");
                mongoService.cleanImageMongodb(targetUsername, "deskTopSnapShot");
                mongoService.cleanImageMongodb(targetUsername, "deskTopSnapShotBest");
                gameService.deleteGameData(targetUsername);

                // 5. Удаление самого пользователя в микросервисе USERS
                usersService.deleteUser(targetUserId);

                log.info("✅ Пользователь {} [ID: {}] успешно удален из всех подсистем", targetUsername, targetUserId);

                // 6. Вызываем наш пакетный метод для мгновенного обновления панели у всех админов
                admin(principal);

            } catch (Exception e) {
                log.error("💥 Крах операции удаления пользователя в виртуальном потоке: {}", e.getMessage(), e);
                try {
                    this.template.convertAndSendToUser(principal.getName(), "/queue/alert", "Error during deletion!");
                } catch (Exception ignored) {}
            }
        });
    }

    @MessageMapping("/{moveId}")
    public void gamePlayDown(@DestinationVariable String moveId, Principal principal) {
        if (principal == null) return;

        String destinationId = principal.getName();
        String userId = destinationId.split(":")[0];

        // Проверяем наличие активного таска (таймера) — это маркер запущенной игры
        boolean hasActiveGame = playGameService.hasUserTask(userId);

        // Защита: если игры нет и это не старт — игнорируем пакет
        if (!hasActiveGame && !moveId.equals("start")) {
            return;
        }

        switch (moveId) {
            case "start" -> {
                // 🔥 Защита от повторного старта и утечки таймеров
                if (hasActiveGame) {
                    log.warn("⚠️ Игрок {} попытался запустить игру повторно. Запрос отклонен.", userId);
                    return;
                }

                // Быстрая проверка первичной инициализации стейта
                var state = playGameService.getState(userId);
                if (state == null) {
                    log.warn("⚠️ Попытка старта игры без инициализированного стейта для ID: {}", userId);
                    return;
                }

                String playerName = state.getGame().getPlayerName();

                // 1. Асинхронный gRPC запрос исторических рекордов в виртуальном потоке Loom
                Thread.startVirtualThread(() -> {
                    try {
                        String rawData = gameService.getGameData(playerName);
                        if (rawData != null && !rawData.isEmpty() && !rawData.equals("{}")) {
                            JSONObject jsonGameData = new JSONObject(rawData);
                            displayService.sendDaoGameToBeDisplayed(
                                    new GameDataDTO(
                                            jsonGameData.optString("bestplayer", "None"),
                                            jsonGameData.optInt("bestscore", 0)
                                    ),
                                    template,
                                    destinationId
                            );
                        }
                    } catch (Exception e) {
                        log.error("❌ Error fetching game data for {}: {}", playerName, e.getMessage());
                    }
                });

                // 2. Безопасный запуск таймера автопадения фигур
                // 2. Безопасный запуск таймера автопадения фигур (Project Loom / TaskScheduler)
                ScheduledFuture<?> task = taskScheduler.scheduleAtFixedRate(
                        () -> {
                            try {
                                // ШАГ A: Бизнес-логика атомарно рассчитывает шаг падения вниз в распределенной мапе
                                // (Этот метод вернет измененный State, либо зафиксирует Game Over)
                                playGameService.createStateAfterMoveDown(playGameService.getState(userId), gameService, userId);

                                // ШАГ Б: Чистый Read-Only слой отображения отправляет обновленный стейт на фронтенд
                                displayService.sendStateToBeDisplayed(playGameService, gameService, template, destinationId);

                            } catch (Exception e) {
                                log.error("💥 Ошибка внутри фонового тика игрового таймера для пользователя {}: {}", userId, e.getMessage());
                            }
                        },
                        Duration.ofMillis(1000)
                );
                playGameService.setUserTask(userId, task);
                log.info("🎮 Игровой цикл и автоматический планировщик падения успешно запущены для пользователя {}", playerName);

            }

            // 🔥 ВАЖНО: Мы больше не передаем currentState снаружи контроллера!
            // Сервисы внутри себя атомарно извлекут из памяти актуальный стейт,
            // избавляя игровой цикл от Race Condition.
            // Вместо старых одиночных вызовов, совмещаем ручной ход с автоматическим падением вниз
            case "1" -> {
                playGameService.rotateState(userId); // Повернули
                playGameService.createStateAfterMoveDown(playGameService.getState(userId), gameService, userId); // Сразу толкнули вниз
            }
            case "2" -> {
                playGameService.moveLeftState(userId); // Сдвинули влево
                playGameService.createStateAfterMoveDown(playGameService.getState(userId), gameService, userId); // Сразу толкнули вниз
            }
            case "3" -> {
                playGameService.moveRightState(userId); // Сдвинули вправо
                playGameService.createStateAfterMoveDown(playGameService.getState(userId), gameService, userId); // Сразу толкнули вниз
            }
            case "4" -> playGameService.dropDownState(userId); // Мгновенный сброс (Hard Drop) сам по себе хардкорен

        }

        // Отправляем визуальное обновление на фронтенд (кроме старта)
        if (!moveId.equals("start")) {
            displayService.sendStateToBeDisplayed(playGameService, gameService, template, destinationId);
        }
    }


    @MessageMapping("/save")
    public void gameSave(Principal principal) {
        if (principal == null) return;

        String destinationId = principal.getName();
        String userId = destinationId.split(":")[0];

        // 🚀 Мгновенно уходим в виртуальный поток Loom, освобождая брокер сообщений
        applicationTaskExecutor.execute(() -> {
            try {
                // ШАГ 1: Первым делом жестко останавливаем таймер падения фигур (замораживаем игру)
                playGameService.stopUserTask(userId);

                // ШАГ 2: Только после заморозки берем стабильное, статичное состояние
                var currentState = playGameService.getState(userId);
                if (currentState != null) {
                    SavedGame savedGame = playGameService.saveGame(currentState.getGame(), currentState);

                    // ШАГ 3: Отправляем дамп в MongoDB (блокирующий gRPC-вызов безопасно паркует поток Loom)
                    mongoService.saveGame(savedGame);

                    // Уведомляем фронтенд об успешном сохранении
                    displayService.sendSavedStateToBeDisplayed(playGameService, template, destinationId);
                    log.info("💾 Игра успешно сохранена в MongoDB для пользователя ID: {}", userId);
                }
            } catch (Exception e) {
                log.error("💥 Ошибка при сохранении игры в виртуальном потоке: {}", e.getMessage(), e);
            }
        });
    }

    @MessageMapping("/restart")
    public void gameRestart(Principal principal) {
        if (principal == null) return;

        String destinationId = principal.getName();
        String[] parts = destinationId.split(":");
        String userId = parts[0];
        String playerName = (parts.length > 1) ? parts[1] : "Player_" + userId;

        // 🚀 Переносим запрос к Монго в виртуальный поток Loom
        applicationTaskExecutor.execute(() -> {
            try {
                log.info("🔄 Инициирован рестарт сессии для игрока: {}", playerName);

                // Запрашиваем у MongoDB сохраненную игру (сетевое ожидание эффективно утилизирует Loom)
                mongoService.gameRestart(playerName).ifPresentOrElse(savedGame -> {

                    // Восстанавливаем состояние в распределенном кэше Hazelcast
                    var recreatedState = playGameService.recreateStateFromSavedGame(savedGame, userId);
                    playGameService.setState(recreatedState, userId);

                    // ВАЖНО: Если игра была на паузе, здесь можно сразу запустить новый таймер падения,
                    // либо дождаться от пользователя явной команды "start".
                    // Сейчас мы просто обновляем экран фронтенда:
                    displayService.sendStateToBeDisplayed(playGameService, gameService, template, destinationId);

                    log.info("✅ Игровой процесс успешно восстановлен из MongoDB для {}", playerName);
                }, () -> {
                    log.warn("⚠️ Сохранений в MongoDB для игрока {} не найдено", playerName);
                    this.template.convertAndSendToUser(destinationId, "/queue/alert", "No saved game found!");
                });

            } catch (Exception e) {
                log.error("💥 Ошибка при восстановлении игры из MongoDB: {}", e.getMessage(), e);
            }
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

        // Переносим всю тяжелую логику (gRPC -> Скриншот -> MongoDB) в отдельный виртуальный поток
        Thread.startVirtualThread(() -> {
            try {
                // 1. Синхронный запрос данных через gRPC
                String rawData = gameService.getGameData(playerName);
                if (rawData == null || rawData.isEmpty() || rawData.equals("{}")) return;

                JSONObject jsonGameData = new JSONObject(rawData);
                String bestPlayer = jsonGameData.optString("bestplayer", "None");
                int bestScore = jsonGameData.optInt("bestscore", 0);
                int playerBestScore = jsonGameData.optInt("playerbestscore", 0);

                // 2. Делаем обычный скриншот (в виртуальном потоке блокировка I/O абсолютно бесплатна)
                gameArtefactService.makeDesktopSnapshot("deskTopSnapShot", playGameService, currentState, bestPlayer, bestScore);

                // Эти методы помечены @Async, они отработают параллельно в своих виртуальных потоках
                mongoService.cleanImageMongodb(playerName, "deskTopSnapShot");
                mongoService.loadSnapShotIntoMongodb(playerName, "deskTopSnapShot");

                // 3. Если побит личный рекорд — делаем "Best" скриншот
                if (currentState.getGame().getPlayerScore() >= playerBestScore) {
                    gameArtefactService.makeDesktopSnapshot("deskTopSnapShotBest", playGameService, currentState, bestPlayer, bestScore);
                    mongoService.cleanImageMongodb(playerName, "deskTopSnapShotBest");
                    mongoService.loadSnapShotIntoMongodb(playerName, "deskTopSnapShotBest");
                }

                // 4. Отправляем финальный экран пользователю
                displayService.sendFinalStateToBeDisplayed(playGameService, template, destinationId);
                log.info("📸 Скриншоты для {} успешно обработаны в виртуальном потоке", playerName);

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

        // 🚀 Мгновенно уходим в виртуальный поток Loom, освобождая брокер сокетов
        applicationTaskExecutor.execute(() -> {
            try {
                // 1. Безопасно извлекаем текущее состояние из Hazelcast
                var currentState = playGameService.getState(userId);

                // Защита от NullPointerException, если игра уже завершена или стейт удален
                if (currentState == null || currentState.getGame() == null) {
                    log.warn("⚠️ Попытка фиксации рекорда для ID: {}, но активных игровых данных не найдено", userId);
                    return;
                }

                log.info("🏆 Инициирована отправка финального счета в БД для игрока ID: {}", userId);

                // 2. gRPC-вызов к PG-microservice (блокирующий сетевой I/O безопасно паркует поток Loom)
                gameService.doRecord(currentState.getGame());

                // Безопасное логирование через плейсхолдеры
                log.info("✅ Рекорд успешно зафиксирован в PostgreSQL для пользователя ID: {}", userId);

            } catch (Exception e) {
                log.error("💥 Критическая ошибка при сохранении рекорда в gRPC-потоке: {}", e.getMessage(), e);
            }
        });
    }

}
