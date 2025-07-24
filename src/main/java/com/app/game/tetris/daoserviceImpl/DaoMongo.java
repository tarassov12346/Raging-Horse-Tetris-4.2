package com.app.game.tetris.daoserviceImpl;

import com.app.game.tetris.daoservice.DaoMongoService;
import com.app.game.tetris.model.Game;
import com.app.game.tetris.model.SavedGame;
import com.app.game.tetris.serviceImpl.State;
import com.google.gson.Gson;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.GridFSUploadStream;
import com.mongodb.client.gridfs.model.GridFSDownloadOptions;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class DaoMongo implements DaoMongoService {
    @Value("${mongodbPath}")
    String mongodbPath;

    @Value("${shotsPath}")
    String shotsPath;

    @Value("${mongoPrepareShotsPath}")
    String mongoPrepareShotsPath;

    @Value("${mongoUri}")
    String mongoUri;

    @Override
    public void runMongoServer() {
        try {
            Runtime.getRuntime().exec(new String[]{mongodbPath});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void prepareMongoDBForNewPLayer(String playerName) {
        fillMongoDB("Player", playerName);
        fillMongoDB("PlayerdeskTopSnapShotBest", playerName + "deskTopSnapShotBest");
        fillMongoDB("PlayerdeskTopSnapShot", playerName + "deskTopSnapShot");
    }

    @Override
    public boolean isImageFilePresentInMongoDB(String fileName) {
        String uri = mongoUri;
        MongoClient mongoClient = MongoClients.create(uri);
        MongoDatabase database = mongoClient.getDatabase("shopDB");
        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("filename", fileName + ".jpg");
        return database.getCollection("fs.files").find(whereQuery).cursor().hasNext();
    }

    @Override
    public boolean isSavedGamePresentInMongoDB(String fileName) {
        String uri = mongoUri;
        MongoClient mongoClient = MongoClients.create(uri);
        MongoDatabase database = mongoClient.getDatabase("shopDB");
        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("name", fileName);
        return database.getCollection("saved_games").find(whereQuery).cursor().hasNext();
    }

    @Override
    public void cleanImageMongodb(String playerName, String fileName) {
        String uri = mongoUri;
        MongoClient mongoClient = MongoClients.create(uri);
        MongoDatabase database = mongoClient.getDatabase("shopDB");
        GridFSBucket gridFSBucket = GridFSBuckets.create(database);
        GridFSFindIterable gridFSFile = gridFSBucket.find(Filters.eq("filename", playerName + fileName + ".jpg"));
        while (gridFSFile.cursor().hasNext()) {
            gridFSBucket.delete(gridFSFile.cursor().next().getId());
        }
        mongoClient.close();
    }

    @Override
    public void cleanSavedGameMongodb(String playerName) {
        String uri = mongoUri;
        MongoClient mongoClient = MongoClients.create(uri);
        MongoDatabase database = mongoClient.getDatabase("shopDB");
        MongoCollection<Document> collection = database.getCollection("saved_games");
        Bson filter = Filters.eq("name", playerName + "SavedGame");
        collection.deleteMany(filter);
    }

    @Override
    public void loadSavedGameIntoMongodb(SavedGame savedGame, Game player) {
        String uri = mongoUri;
        MongoClient mongoClient = MongoClients.create(uri);
        MongoDatabase database = mongoClient.getDatabase("shopDB");
        MongoCollection<Document> collection = database.getCollection("saved_games");
        Bson filter = Filters.eq("name", player.getPlayerName() + "SavedGame");
        collection.deleteMany(filter);
        Gson gson = new Gson();
        String json = gson.toJson(savedGame);
        Document document = Document.parse(json);
        document.put("name", player.getPlayerName() + "SavedGame");
        collection.insertOne(document);
    }

    @Override
    public SavedGame loadSavedGameFromMongodb(Game player) {
        String uri = mongoUri;
        MongoClient mongoClient = MongoClients.create(uri);
        MongoDatabase database = mongoClient.getDatabase("shopDB");
        MongoCollection<Document> collection = database.getCollection("saved_games");
        Gson gson = new Gson();
        Bson filter = Filters.eq("name", player.getPlayerName() + "SavedGame");
        Document document = collection.find(filter).first();
        return gson.fromJson(document.toJson(), SavedGame.class);
    }

    @Override
    public void loadSnapShotIntoMongodb(String playerName, String fileName) {
        String uri = mongoUri;
        String pathToShots = System.getProperty("user.dir") + shotsPath;
        MongoClient mongoClient = MongoClients.create(uri);
        MongoDatabase database = mongoClient.getDatabase("shopDB");
        GridFSBucket gridFSBucket = GridFSBuckets.create(database);
        byte[] data = new byte[0];
        try {
            data = Files.readAllBytes(Path.of(pathToShots + fileName + ".jpg"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        GridFSUploadOptions options = new GridFSUploadOptions()
                .chunkSizeBytes(1048576)
                .metadata(new Document("type", "jpg"));
        try (GridFSUploadStream uploadStream = gridFSBucket.openUploadStream(playerName + fileName + ".jpg", options)) {
            // Writes file data to the GridFS upload stream
            uploadStream.write(data);
            uploadStream.flush();
            // Prints the "_id" value of the uploaded file
            System.out.println("The file id of the uploaded file is: " + uploadStream.getObjectId().toHexString());
// Prints a message if any exceptions occur during the upload process
        } catch (Exception e) {
            System.err.println("The file upload failed: " + e);
        }
        Bson query = Filters.eq("metadata.type", "jpg");
        Bson sort = Sorts.ascending("filename");
// Retrieves 5 documents in the bucket that match the filter and prints metadata
        gridFSBucket.find(query)
                .sort(sort)
                .limit(5)
                .forEach(gridFSFile -> System.out.println(gridFSFile));
        // Now you can work with the 'database' object to perform CRUD operations.
        // Don't forget to close the MongoClient when you're done.
        mongoClient.close();
    }

    @Override
    public void loadMugShotIntoMongodb(String playerName, byte[] data){
        String uri = mongoUri;
        MongoClient mongoClient = MongoClients.create(uri);
        MongoDatabase database = mongoClient.getDatabase("shopDB");
        GridFSBucket gridFSBucket = GridFSBuckets.create(database);
        GridFSUploadOptions options = new GridFSUploadOptions()
                .chunkSizeBytes(1048576)
                .metadata(new Document("type", "jpg"));
        try (GridFSUploadStream uploadStream = gridFSBucket.openUploadStream(playerName + ".jpg", options)) {
            // Writes file data to the GridFS upload stream
            uploadStream.write(data);
            uploadStream.flush();
            // Prints the "_id" value of the uploaded file
            System.out.println("The file id of the uploaded file is: " + uploadStream.getObjectId().toHexString());
// Prints a message if any exceptions occur during the upload process
        } catch (Exception e) {
            System.err.println("The file upload failed: " + e);
        }
        Bson query = Filters.eq("metadata.type", "jpg");
        Bson sort = Sorts.ascending("filename");
// Retrieves 5 documents in the bucket that match the filter and prints metadata
        gridFSBucket.find(query)
                .sort(sort)
                .limit(5)
                .forEach(gridFSFile -> System.out.println(gridFSFile));
        // Now you can work with the 'database' object to perform CRUD operations.
        // Don't forget to close the MongoClient when you're done.
        mongoClient.close();
    }

    @Override
    public void makeDesktopSnapshot(String fileNameDetail, State state, String bestPlayerName, int bestPlayerScore) {
        String pathToShots = System.getProperty("user.dir") + shotsPath;
        String format = "jpg";
        String fileName = pathToShots + fileNameDetail + "." + format;
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            char[][] cells = state.getStage().drawTetraminoOnCells();
            page.navigate("http://localhost:8080/html/snapShot.html");
            for (int i = 0; i < 20; i++) {
                for (int j = 0; j < 12; j++) {
                    String cell = new StringBuilder("c").append(i).append("v").append(j).toString();
                    String src = new StringBuilder("http://localhost:8080/img/").append(cells[i][j]).append(".png").toString();
                    String js = "document.getElementById('" + cell + "').src='" + src + "'";
                    page.evaluate(js);
                }
            }
            playwright.selectors().setTestIdAttribute("id");
            String js = "document.getElementById('gameStatusBox').innerHTML = 'Game OVER!!!'";
            page.evaluate(js);
            js = "document.getElementById('playerBox').innerHTML = '"+state.getGame().getPlayerName()+"'";
            page.evaluate(js);
            js = "document.getElementById('playerScoreBox').innerHTML = '"+ state.getGame().getPlayerScore() +"'";
            page.evaluate(js);
            js = "document.getElementById('bestPlayerBox').innerHTML = '"+bestPlayerName+"'";
            page.evaluate(js);
            js = "document.getElementById('bestPlayerScoreBox').innerHTML = '"+bestPlayerScore+"'";
            page.evaluate(js);
            js = "document.getElementById('tetrisSpeedBox').innerHTML = 'Tetris at speed "+state.getGame().getPlayerScore() / 10+"'";
            page.evaluate(js);
            page.waitForTimeout(3000);
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(fileName)));
        }
    }

    @Override
    public byte[] loadByteArrayFromMongodb(String playerName, String fileName) {
        String uri = mongoUri;
        MongoClient mongoClient = MongoClients.create(uri);
        MongoDatabase database = mongoClient.getDatabase("shopDB");
        GridFSBucket gridFSBucket = GridFSBuckets.create(database);
        GridFSDownloadOptions downloadOptions = new GridFSDownloadOptions().revision(0);
        byte[] imagenEnBytes = new byte[16384];
// Downloads a file to an output stream
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            if (fileName.equals("mugShot")) gridFSBucket.downloadToStream(playerName + ".jpg", buffer, downloadOptions);
            else gridFSBucket.downloadToStream(playerName + fileName + ".jpg", buffer, downloadOptions);
            imagenEnBytes = buffer.toByteArray();
            buffer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mongoClient.close();
        return imagenEnBytes;
    }

    private void fillMongoDB(String fileNameOnPC, String fileNameINDB) {
        String uri = mongoUri;
        String pathToImageMongoPreparedShots = System.getProperty("user.dir") + mongoPrepareShotsPath;
        MongoClient mongoClient = MongoClients.create(uri);
        MongoDatabase database = mongoClient.getDatabase("shopDB");
        GridFSBucket gridFSBucket = GridFSBuckets.create(database);
        byte[] data = new byte[0];
        try {
            data = Files.readAllBytes(Path.of(pathToImageMongoPreparedShots + fileNameOnPC + ".jpg"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        GridFSUploadOptions options = new GridFSUploadOptions()
                .chunkSizeBytes(1048576)
                .metadata(new Document("type", "jpg"));
        try (GridFSUploadStream uploadStream = gridFSBucket.openUploadStream(fileNameINDB + ".jpg", options)) {
            // Writes file data to the GridFS upload stream
            uploadStream.write(data);
            uploadStream.flush();
            // Prints the "_id" value of the uploaded file
            System.out.println("The file id of the uploaded file is: " + uploadStream.getObjectId().toHexString());
// Prints a message if any exceptions occur during the upload process
        } catch (Exception e) {
            System.err.println("The file upload failed: " + e);
        }
        Bson query = Filters.eq("metadata.type", "jpg");
        Bson sort = Sorts.ascending("filename");
// Retrieves 5 documents in the bucket that match the filter and prints metadata
        gridFSBucket.find(query)
                .sort(sort)
                .limit(5)
                .forEach(gridFSFile -> System.out.println(gridFSFile));
        // Now you can work with the 'database' object to perform CRUD operations.
        // Don't forget to close the MongoClient when you're done.
        mongoClient.close();
    }
}
