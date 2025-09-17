package com.app.game.tetris.mongoservice;

import com.app.game.tetris.model.SavedGame;

import java.util.Optional;

public interface MongoService {

    void saveGame(SavedGame savedGame);

    Optional<SavedGame> gameRestart(String playerName);

    void cleanSavedGameMongodb(String playerName);

    void prepareMongoDBForNewPLayer(String playerName);

    void cleanImageMongodb(String playerName, String fileName);

    byte[] loadByteArrayFromMongodb(String playerName, String fileName);

}
