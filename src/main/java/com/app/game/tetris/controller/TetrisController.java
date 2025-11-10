package com.app.game.tetris.controller;

import com.app.game.tetris.displayservice.DisplayService;
import com.app.game.tetris.model.Users;
import com.app.game.tetris.users_service.UsersService;
import com.app.game.tetris.gameArtefactservice.GameArtefactService;
import com.app.game.tetris.gameservice.GameService;
import com.app.game.tetris.model.Game;
import com.app.game.tetris.model.Roles;
import com.app.game.tetris.model.SavedGame;
import com.app.game.tetris.mongoservice.MongoService;
import com.app.game.tetris.tetriservice.PlayGameService;
import com.app.game.tetris.tetriserviceImpl.State;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.OutputStream;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class TetrisController {

    @Autowired
    private PlayGameService playGameService;

    @Autowired
    private SimpMessagingTemplate template;

    @Autowired
    private UsersService usersService;


    @Autowired
    private GameArtefactService gameArtefactService;

    @Autowired
    private GameService gameService;

    @Autowired
    private MongoService mongoService;

    @Autowired
    DisplayService displayService;

    private ScheduledExecutorService service;

    @MessageMapping("/register")
    public void register(Users user) {
        if (usersService.isRolesDBEmpty()) {
            usersService.prepareRolesDB();
            usersService.prepareUserDB();
        }
        Users newUser = new Users();
        if (!user.getUsername().matches(".*[a-zA-Z]+.*")) {
            this.template.convertAndSend("/receive/message", "The user name should contain at least one letter!");
            return;
        }
        if (!user.getPassword().matches(".*[a-zA-Z]+.*") || !user.getPassword().matches("(.)*(\\d)(.)*")) {
            this.template.convertAndSend("/receive/message", "The password should contain at least one letter and one digit!");
            return;
        }
        if (user.getPassword().equals(user.getPasswordConfirm())) {
            newUser.setUsername(user.getUsername());
            newUser.setPassword(user.getPassword());
            newUser.setPasswordConfirm(user.getPasswordConfirm());
        } else {
            this.template.convertAndSend("/receive/message", "The password is not confirmed!");
        }
        if (!usersService.saveUser(newUser)) {
            this.template.convertAndSend("/receive/message", "This user already exists!");
        } else {
            this.template.convertAndSend("/receive/message", "The user " + newUser.getUsername() + " has been successfully registered!");
        }
    }

    @MessageMapping("/hello")
    public void hello(Principal principal) {
        playGameService.setState(playGameService.initiateState(principal.getName()));
        mongoService.prepareMongoDBForNewPLayer(playGameService.getState().getGame().getPlayerName());
        displayService.sendGameToBeDisplayed(playGameService.getState().getGame(), template);
        JSONObject jsonGameData = new JSONObject(gameService.getGameData(principal.getName()));
        displayService.sendDaoGameToBeDisplayed(playGameService.createGame(jsonGameData.getString("bestplayer"), jsonGameData.getInt("bestscore")), template);
    }

    @MessageMapping("/profile")
    public void profile() {
        JSONObject jsonGameData = new JSONObject(gameService.getGameData(playGameService.getState().getGame().getPlayerName()));
        this.template.convertAndSend("/receive/playerStat",
                playGameService.createGame(playGameService.getState().getGame().getPlayerName(), jsonGameData.getInt("playerbestscore")));
        this.template.convertAndSend("/receive/playerAttemptsNumber",
                playGameService.createGame("DataTransferObject", jsonGameData.getInt("playerAttemptsNumber")));
    }

    @MessageMapping("/upload")
    public void upload(String imageBase64Stringsep) {
        mongoService.cleanImageMongodb(playGameService.getState().getGame().getPlayerName(), "");
        mongoService.loadMugShotIntoMongodb(playGameService.getState().getGame().getPlayerName(), Base64.getDecoder().decode(imageBase64Stringsep));
    }

    @GetMapping({"/getPhoto"})
    public void getPhoto(HttpServletRequest request,
                         HttpServletResponse response) {
        byte[] imagenEnBytes = mongoService.loadByteArrayFromMongodb(playGameService.getState().getGame().getPlayerName(), "mugShot");
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
                            HttpServletResponse response) {
        byte[] imagenEnBytes = mongoService.loadByteArrayFromMongodb(playGameService.getState().getGame().getPlayerName(), "deskTopSnapShot");
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
                                HttpServletResponse response) {
        byte[] imagenEnBytes = mongoService.loadByteArrayFromMongodb(playGameService.getState().getGame().getPlayerName(), "deskTopSnapShotBest");
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
        getAllBestResults(gameService.getAllGames()).
                forEach(game -> this.template.convertAndSend("/receive/results", game));
    }

    @MessageMapping("/admin/{userId}")
    public void deleteUser(@DestinationVariable Long userId) {
        Users foundByIdUser =usersService.findUserById(userId);
        if (foundByIdUser.getUsername().equals(playGameService.getState().getGame().getPlayerName())) {
            this.template.convertAndSend("/receive/alert", "You cannot delete yourself!");
            return;
        }
        for (Roles role : usersService.findUserByUserName(playGameService.getState().getGame().getPlayerName()).getRoles()) {
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
    public void gamePlayDown(@DestinationVariable String moveId) {
        switch (moveId) {
            case "start" -> {
                JSONObject jsonGameData = new JSONObject(gameService.getGameData(playGameService.getState().getGame().getPlayerName()));
                displayService.sendDaoGameToBeDisplayed(playGameService.createGame(jsonGameData.getString("bestplayer"), jsonGameData.getInt("bestscore")), template);
                service = Executors.newScheduledThreadPool(1);
                playGameService.setState(playGameService.getState());
                service.scheduleAtFixedRate(() -> displayService.sendStateToBeDisplayed(playGameService, gameService, service, template), 0, 1000, TimeUnit.MILLISECONDS);
            }
            case "1" -> {
                playGameService.setState(playGameService.rotateState((State) playGameService.getState()));
                displayService.sendStateToBeDisplayed(playGameService, gameService, service, template);
            }
            case "2" -> {
                playGameService.setState(playGameService.moveLeftState((State) playGameService.getState()));
                displayService.sendStateToBeDisplayed(playGameService, gameService, service, template);
            }
            case "3" -> {
                playGameService.setState(playGameService.moveRightState((State) playGameService.getState()));
                displayService.sendStateToBeDisplayed(playGameService, gameService, service, template);
            }
            case "4" -> {
                playGameService.setState(playGameService.dropDownState((State) playGameService.getState()));
                displayService.sendStateToBeDisplayed(playGameService, gameService, service, template);
            }
        }
    }

    @MessageMapping("/save")
    public void gameSave() {
        service.shutdown();
        SavedGame savedGame = playGameService.saveGame(playGameService.getState().getGame(), (State) playGameService.getState());
        mongoService.saveGame(savedGame);
        displayService.sendSavedStateToBeDisplayed(playGameService, gameService, service, template);
    }

    @MessageMapping("/restart")
    public void gameRestart() {
        if (mongoService.gameRestart(playGameService.getState().getGame().getPlayerName()).isPresent()) {
            SavedGame savedGame = mongoService.gameRestart(playGameService.getState().getGame().getPlayerName()).get();
            playGameService.setState(playGameService.recreateStateFromSavedGame(savedGame));
            displayService.sendStateToBeDisplayed(playGameService, gameService, service, template);
        }
    }

    @MessageMapping("/snapShot")
    public void makeSnapShot() {
        JSONObject jsonGameData = new JSONObject(gameService.getGameData(playGameService.getState().getGame().getPlayerName()));
        gameArtefactService.makeDesktopSnapshot("deskTopSnapShot", (State) playGameService.getState(), jsonGameData.getString("bestplayer"), jsonGameData.getInt("bestscore"));
        mongoService.cleanImageMongodb(playGameService.getState().getGame().getPlayerName(), "deskTopSnapShot");
        mongoService.loadSnapShotIntoMongodb(playGameService.getState().getGame().getPlayerName(), "deskTopSnapShot");
        if (playGameService.getState().getGame().getPlayerScore() >= jsonGameData.getInt("playerbestscore")) {
            gameArtefactService.makeDesktopSnapshot("deskTopSnapShotBest", (State) playGameService.getState(), jsonGameData.getString("bestplayer"), jsonGameData.getInt("bestscore"));
            mongoService.cleanImageMongodb(playGameService.getState().getGame().getPlayerName(), "deskTopSnapShotBest");
            mongoService.loadSnapShotIntoMongodb(playGameService.getState().getGame().getPlayerName(), "deskTopSnapShotBest");
        }
        displayService.sendFinalStateToBeDisplayed(playGameService, gameService, service, template);
    }

    private Set<Game> getAllBestResults(List<Game> playersList) {
        Set<Game> highestScoringPlayers = new HashSet<>();
        playersList.sort(Comparator.comparingInt(Game::getPlayerScore).reversed());
        highestScoringPlayers.addAll(playersList);
        return highestScoringPlayers;
    }
}
