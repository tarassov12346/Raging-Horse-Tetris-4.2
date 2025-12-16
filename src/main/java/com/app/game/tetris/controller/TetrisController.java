package com.app.game.tetris.controller;

import com.app.game.tetris.displayservice.DisplayService;
import com.app.game.tetris.dto.UserRegistrationDto;
import com.app.game.tetris.gameArtefactservice.GameArtefactService;
import com.app.game.tetris.gameservice.GameService;
import com.app.game.tetris.model.Roles;
import com.app.game.tetris.model.SavedGame;
import com.app.game.tetris.model.Users;
import com.app.game.tetris.mongoservice.MongoService;
import com.app.game.tetris.tetriservice.PlayGameService;
import com.app.game.tetris.users_service.UsersService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.json.JSONObject;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.OutputStream;
import java.security.Principal;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class TetrisController {
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

    @MessageMapping("/register")
    public void register(@Valid UserRegistrationDto userDto) {
        if (usersService.isRolesDBEmpty()) {
            usersService.prepareRolesDB();
            usersService.prepareUserDB();
        }
        Users newUser = new Users();
        if (userDto.getPassword().equals(userDto.getPasswordConfirm())) {
            newUser.setUsername(userDto.getUsername());
            newUser.setPassword(userDto.getPassword());
            newUser.setPasswordConfirm(userDto.getPasswordConfirm());
        } else {
            this.template.convertAndSend("/receive/message", "The password is not confirmed!");
            return;
        }
        this.template.convertAndSend("/receive/message", usersService.saveUser(newUser) ? "The user " + newUser.getUsername() + " has been successfully registered!" : "This user already exists!");
    }

    @MessageMapping("/hello")
    public void hello(Principal principal) {
        String userId = String.valueOf(usersService.findUserByUserName(principal.getName()).getId());
        playGameService.setState(playGameService.initiateState(principal.getName(), userId), userId);
        mongoService.prepareMongoDBForNewPLayer(playGameService.getState(userId).getGame().getPlayerName());
        displayService.sendGameToBeDisplayed(playGameService.getState(userId).getGame(), template);
        JSONObject jsonGameData = new JSONObject(gameService.getGameData(principal.getName()));
        displayService.sendDaoGameToBeDisplayed(playGameService.createGame(jsonGameData.getString("bestplayer"), jsonGameData.getInt("bestscore")), template);
    }

    @MessageMapping("/profile")
    public void profile(Principal principal) {
        String userId = String.valueOf(usersService.findUserByUserName(principal.getName()).getId());
        JSONObject jsonGameData = new JSONObject(gameService.getGameData(playGameService.getState(userId).getGame().getPlayerName()));
        this.template.convertAndSend("/receive/playerStat",
                playGameService.createGame(playGameService.getState(userId).getGame().getPlayerName(), jsonGameData.getInt("playerbestscore")));
        this.template.convertAndSend("/receive/playerAttemptsNumber",
                playGameService.createGame("DataTransferObject", jsonGameData.getInt("playerAttemptsNumber")));
    }

    @MessageMapping("/upload")
    public void upload(String imageBase64Stringsep, Principal principal) {
        String userId = String.valueOf(usersService.findUserByUserName(principal.getName()).getId());
        mongoService.cleanImageMongodb(playGameService.getState(userId).getGame().getPlayerName(), "");
        mongoService.loadMugShotIntoMongodb(playGameService.getState(userId).getGame().getPlayerName(), Base64.getDecoder().decode(imageBase64Stringsep));
    }

    @GetMapping({"/getPhoto"})
    public void getPhoto(HttpServletRequest request,
                         HttpServletResponse response, Principal principal) {
        String userId = String.valueOf(usersService.findUserByUserName(principal.getName()).getId());
        byte[] imagenEnBytes = mongoService.loadByteArrayFromMongodb(playGameService.getState(userId).getGame().getPlayerName(), "mugShot");
        response.setHeader("Accept-ranges", "bytes");
        response.setContentType("image/jpeg");
        response.setContentLength(imagenEnBytes.length);
        response.setHeader("Expires", "0");
        response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
        response.setHeader("Content-Description", "File Transfer");
        response.setHeader("Content-Transfer-Encoding:", "binary");
        try {
            OutputStream out = response.getOutputStream();
            out.write(imagenEnBytes);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping({"/getSnapShot"})
    public void getSnapShot(HttpServletRequest request,
                            HttpServletResponse response, Principal principal) {
        String userId = String.valueOf(usersService.findUserByUserName(principal.getName()).getId());
        byte[] imagenEnBytes = mongoService.loadByteArrayFromMongodb(playGameService.getState(userId).getGame().getPlayerName(), "deskTopSnapShot");
        response.setHeader("Accept-ranges", "bytes");
        response.setContentType("image/jpeg");
        response.setContentLength(imagenEnBytes.length);
        response.setHeader("Expires", "0");
        response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
        response.setHeader("Content-Description", "File Transfer");
        response.setHeader("Content-Transfer-Encoding:", "binary");
        try {
            OutputStream out = response.getOutputStream();
            out.write(imagenEnBytes);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping({"/getSnapShotBest"})
    public void getSnapShotBest(HttpServletRequest request,
                                HttpServletResponse response, Principal principal) {
        String userId = String.valueOf(usersService.findUserByUserName(principal.getName()).getId());
        byte[] imagenEnBytes = mongoService.loadByteArrayFromMongodb(playGameService.getState(userId).getGame().getPlayerName(), "deskTopSnapShotBest");
        response.setHeader("Accept-ranges", "bytes");
        response.setContentType("image/jpeg");
        response.setContentLength(imagenEnBytes.length);
        response.setHeader("Expires", "0");
        response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
        response.setHeader("Content-Description", "File Transfer");
        response.setHeader("Content-Transfer-Encoding:", "binary");
        try {
            OutputStream out = response.getOutputStream();
            out.write(imagenEnBytes);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @MessageMapping("/admin")
    public void admin() {
        List<Users> allUsersList = usersService.getAllUsers();
        allUsersList.forEach(user -> this.template.convertAndSend("/receive/users",
                new Users(user.getId(), user.getUsername(), user.getPassword(),
                        String.join(";", user.getRoles().stream().map(Roles::getName).collect(Collectors.toList())),
                        user.getRoles())));
        gameService.getAllBestResults(gameService.getAllGames()).
                forEach(game -> this.template.convertAndSend("/receive/results", game));
    }

    @MessageMapping("/admin/{userId}")
    public void deleteUser(@DestinationVariable Long userId, Principal principal) {
        Users foundByIdUser = usersService.findUserById(userId);
        if (foundByIdUser.getUsername().equals(principal.getName())) {
            this.template.convertAndSend("/receive/alert", "You cannot delete yourself!");
            return;
        }
        for (Roles role : usersService.findUserByUserName(principal.getName()).getRoles()) {
            if (role.getName().equals("ROLE_ADMIN")) {
                mongoService.cleanSavedGameMongodb(foundByIdUser.getUsername());
                mongoService.cleanImageMongodb(foundByIdUser.getUsername(), "");
                mongoService.cleanImageMongodb(foundByIdUser.getUsername(), "deskTopSnapShot");
                mongoService.cleanImageMongodb(foundByIdUser.getUsername(), "deskTopSnapShotBest");
                gameService.deleteGameData(foundByIdUser.getUsername());
                usersService.deleteUser(userId);
                admin();
                return;
            }
            this.template.convertAndSend("/receive/alert", "You are not admin!");
        }
    }

    @MessageMapping("/{moveId}")
    public void gamePlayDown(@DestinationVariable String moveId, Principal principal) {
        String userId = String.valueOf(usersService.findUserByUserName(principal.getName()).getId());
        switch (moveId) {
            case "start" -> {
                JSONObject jsonGameData = new JSONObject(gameService.getGameData(playGameService.getState(userId).getGame().getPlayerName()));
                displayService.sendDaoGameToBeDisplayed(playGameService.createGame(jsonGameData.getString("bestplayer"), jsonGameData.getInt("bestscore")), template);
                playGameService.setSEService(Executors.newScheduledThreadPool(1), userId);
                playGameService.setState(playGameService.getState(userId), userId);
                playGameService.getSEService(userId).scheduleAtFixedRate(() -> displayService.sendStateToBeDisplayed(playGameService, gameService, playGameService.getSEService(userId), template, userId), 0, 1000, TimeUnit.MILLISECONDS);
            }
            case "1" -> {
                playGameService.setState(playGameService.rotateState(playGameService.getState(userId), userId), userId);
                displayService.sendStateToBeDisplayed(playGameService, gameService, playGameService.getSEService(userId), template, userId);
            }
            case "2" -> {
                playGameService.setState(playGameService.moveLeftState(playGameService.getState(userId), userId), userId);
                displayService.sendStateToBeDisplayed(playGameService, gameService, playGameService.getSEService(userId), template, userId);
            }
            case "3" -> {
                playGameService.setState(playGameService.moveRightState(playGameService.getState(userId), userId), userId);
                displayService.sendStateToBeDisplayed(playGameService, gameService, playGameService.getSEService(userId), template, userId);
            }
            case "4" -> {
                playGameService.setState(playGameService.dropDownState(playGameService.getState(userId), userId), userId);
                displayService.sendStateToBeDisplayed(playGameService, gameService, playGameService.getSEService(userId), template, userId);
            }
        }
    }

    @MessageMapping("/save")
    public void gameSave(Principal principal) {
        String userId = String.valueOf(usersService.findUserByUserName(principal.getName()).getId());
        playGameService.getSEService(userId).shutdown();
        SavedGame savedGame = playGameService.saveGame(playGameService.getState(userId).getGame(), playGameService.getState(userId));
        mongoService.saveGame(savedGame);
        displayService.sendSavedStateToBeDisplayed(playGameService, gameService, playGameService.getSEService(userId), template, userId);
    }

    @MessageMapping("/restart")
    public void gameRestart(Principal principal) {
        String userId = String.valueOf(usersService.findUserByUserName(principal.getName()).getId());
        if (mongoService.gameRestart(playGameService.getState(userId).getGame().getPlayerName()).isPresent()) {
            SavedGame savedGame = mongoService.gameRestart(playGameService.getState(userId).getGame().getPlayerName()).get();
            playGameService.setState(playGameService.recreateStateFromSavedGame(savedGame, userId), userId);
            displayService.sendStateToBeDisplayed(playGameService, gameService, playGameService.getSEService(userId), template, userId);
        }
    }

    @MessageMapping("/snapShot")
    public void makeSnapShot(Principal principal) {
        String userId = String.valueOf(usersService.findUserByUserName(principal.getName()).getId());
        JSONObject jsonGameData = new JSONObject(gameService.getGameData(playGameService.getState(userId).getGame().getPlayerName()));
        gameArtefactService.makeDesktopSnapshot("deskTopSnapShot", playGameService, playGameService.getState(userId), jsonGameData.getString("bestplayer"), jsonGameData.getInt("bestscore"));
        mongoService.cleanImageMongodb(playGameService.getState(userId).getGame().getPlayerName(), "deskTopSnapShot");
        mongoService.loadSnapShotIntoMongodb(playGameService.getState(userId).getGame().getPlayerName(), "deskTopSnapShot");
        if (playGameService.getState(userId).getGame().getPlayerScore() >= jsonGameData.getInt("playerbestscore")) {
            gameArtefactService.makeDesktopSnapshot("deskTopSnapShotBest", playGameService, playGameService.getState(userId), jsonGameData.getString("bestplayer"), jsonGameData.getInt("bestscore"));
            mongoService.cleanImageMongodb(playGameService.getState(userId).getGame().getPlayerName(), "deskTopSnapShotBest");
            mongoService.loadSnapShotIntoMongodb(playGameService.getState(userId).getGame().getPlayerName(), "deskTopSnapShotBest");
        }
        displayService.sendFinalStateToBeDisplayed(playGameService, gameService, playGameService.getSEService(userId), template, userId);
    }
}
