package com.example.parsingdesktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

public class HelloController {
    private String filepath;

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private Button choiseDirectory;

    @FXML
    void handleChooseDirectory(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Выберите папку для сохранения");

        Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
        File selectedDirectory = directoryChooser.showDialog(stage);

        if (selectedDirectory != null) {
            filepath = selectedDirectory.getAbsolutePath();
        } else {
            filepath = null;
        }
    }

    @FXML
    private void handleButtonAction(ActionEvent event) throws IOException {
        if (filepath == null) {
            showError();
        } else {
            parseChannels();
            showParsingStatusWindow(event);
        }
    }
    private void showError() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("error.fxml"));
        Parent root = loader.load();
        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.show();
    }
    private void showErrorParsing() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("errorParsing.fxml"));
        Parent root = loader.load();
        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.show();
    }
    private void showParsingStatusWindow(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("parsingFinish.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();

            // Закрытие текущего окна
            Node source = (Node) event.getSource();
            Stage currentStage = (Stage) source.getScene().getWindow();
            currentStage.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void parseChannels() {
        try {
            String nextPageToken = "";
            JsonArray items;
            HttpClient client;
            File tokenFile = new File("nextPageToken.txt");
            if (tokenFile.exists()) {
                nextPageToken = new String(Files.readAllBytes(tokenFile.toPath()));
            }
            do {
                client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://www.googleapis.com/youtube/v3/search?part=snippet&maxResults=50&q=русскоязычные%20каналы&type=channel&regionCode=RU&relevanceLanguage=ru&key=AIzaSyDiqx5T0kpxg-9VU5sCVyWiqb7hRAQpKks&pageToken=" + nextPageToken))
                        .build();

                HttpResponse<String> responses = client.send(request, HttpResponse.BodyHandlers.ofString());
                String responseBody = responses.body();

                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                items = json.getAsJsonArray("items");

                if (json.has("nextPageToken")) {
                    nextPageToken = json.get("nextPageToken").getAsString();
                    Files.write(tokenFile.toPath(), nextPageToken.getBytes());
                } else {
                    break;
                }
            } while (true);

            File fileNoEmail = new File(filepath + "/"+ "noemail.xls");
            File fileWithEmail = new File(filepath + "/"+ "email.xls");

            Workbook workbookNoEmail;
            Workbook workbookWithEmail;

            if (fileNoEmail.exists()) {
                workbookNoEmail = WorkbookFactory.create(fileNoEmail);
            } else {
                workbookNoEmail = new HSSFWorkbook();
                Sheet sheet = workbookNoEmail.createSheet("Sheet1");

                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("Channel URL");
                headerRow.createCell(1).setCellValue("Subscriber Count");
            }

            if (fileWithEmail.exists()) {
                workbookWithEmail = WorkbookFactory.create(fileWithEmail);
            } else {
                workbookWithEmail = new HSSFWorkbook();
                Sheet sheet = workbookWithEmail.createSheet("Sheet1");

                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("Channel URL");
                headerRow.createCell(1).setCellValue("Email");
                headerRow.createCell(2).setCellValue("Subscriber Count");
            }

            Sheet sheetNoEmail = workbookNoEmail.getSheet("Sheet1");
            Sheet sheetWithEmail = workbookWithEmail.getSheet("Sheet1");

            int currentRowNoEmail = sheetNoEmail.getLastRowNum() + 1;
            int currentRowWithEmail = sheetWithEmail.getLastRowNum() + 1;
            Set<String> existingChannels = new HashSet<>();
            // Чтение существующих каналов из xls файлов
            for (Row row : sheetNoEmail) {
                Cell cell = row.getCell(0);
                if (cell != null) {
                    existingChannels.add(cell.getStringCellValue());
                }
            }
            for (Row row : sheetWithEmail) {
                Cell cell = row.getCell(0);
                if (cell != null) {
                    existingChannels.add(cell.getStringCellValue());
                }
            }

            for (JsonElement item : items) {
                JsonObject channel = item.getAsJsonObject();
                JsonObject snippet = channel.getAsJsonObject("snippet");
                JsonElement channelIdElement = snippet.get("channelId");
                String channelId = channelIdElement != null ? channelIdElement.getAsString() : null;

                if (channelId != null) {
                    String channelUrl = "https://www.youtube.com/channel/" + channelId;
                    if (existingChannels.contains(channelUrl)) {
                        // Пропустить этот канал, так как он уже есть в xls файлах
                        continue;
                    }
                    HttpRequest channelRequest = HttpRequest.newBuilder()
                            .uri(URI.create("https://www.googleapis.com/youtube/v3/channels?part=snippet%2Cstatistics&id=" + channelId + "&key=AIzaSyBVtOjrEYgjVKcHmGzrg7x8OiwRtV-EQ_8"))
                            .build();

                    HttpResponse<String> channelResponse = client.send(channelRequest, HttpResponse.BodyHandlers.ofString());
                    String channelResponseBody = channelResponse.body();

                    JsonObject channelJson = JsonParser.parseString(channelResponseBody).getAsJsonObject();
                    JsonArray channelItems = channelJson.getAsJsonArray("items");

                    if (channelItems != null && channelItems.size() > 0) {
                        JsonObject channelItem = channelItems.get(0).getAsJsonObject();
                        JsonObject channelStatistics = channelItem.getAsJsonObject("statistics");
                        JsonObject channelSnippet = channelItem.getAsJsonObject("snippet");
                        JsonElement descriptionElement = channelSnippet.get("description");

                        String subscriberCount = channelStatistics.get("subscriberCount").getAsString();
                        String email = "";

                        if (descriptionElement != null) {
                            String description = descriptionElement.getAsString();

                            Pattern pattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}");
                            Matcher matcher = pattern.matcher(description);
                            if (matcher.find()) {
                                email = matcher.group();
                            }
                        }
                        Row newRowNoEmail = sheetNoEmail.createRow(currentRowNoEmail);
                        newRowNoEmail.createCell(0).setCellValue("https://www.youtube.com/channel/" + channelId);
                        newRowNoEmail.createCell(1).setCellValue(subscriberCount);
                        currentRowNoEmail++;

                        if (!email.isEmpty()) {
                            Row newRowWithEmail = sheetWithEmail.createRow(currentRowWithEmail);
                            newRowWithEmail.createCell(0).setCellValue("https://www.youtube.com/channel/" + channelId);
                            newRowWithEmail.createCell(1).setCellValue(email);
                            newRowWithEmail.createCell(2).setCellValue(subscriberCount);
                            currentRowWithEmail++;
                        }

                    } else {
                        showErrorParsing();
                    }
                } else {
                    showErrorParsing();
                }
            }

            FileOutputStream fileOutputStreamNoEmail = new FileOutputStream(fileNoEmail);
            workbookNoEmail.write(fileOutputStreamNoEmail);
            fileOutputStreamNoEmail.close();

            FileOutputStream fileOutputStreamWithEmail = new FileOutputStream(fileWithEmail);
            workbookWithEmail.write(fileOutputStreamWithEmail);
            fileOutputStreamWithEmail.close();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

