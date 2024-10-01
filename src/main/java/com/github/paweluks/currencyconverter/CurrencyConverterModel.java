package com.github.paweluks.currencyconverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Paths;


public class CurrencyConverterModel {
    private static final String API_URL = "https://www.floatrates.com/daily/usd.json";
    private static final String LOCAL_FILE_PATH = "target/data/currency.json";
    private static final String LOCAL_PATH = "target/data/";
    private static Map<String, Map<String, String>> currencyData = new HashMap<>();

    public void createPath(){
        // Create LOCAL_PATH
        try {
            Files.createDirectories(Paths.get(LOCAL_PATH));
        } catch (IOException e) {
            System.out.println("Fehler beim Erstellen des Verzeichnisses: " + e.getMessage());
        }
    }

    // Download JSON-File from API
    public void downloadAndSaveJson() {
        createPath();
        // Connection to API
        try {
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            try (InputStream inputStream = connection.getInputStream()) {
                Files.copy(inputStream, Paths.get(LOCAL_FILE_PATH), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Neue JSON-Datei erfolgreich gespeichert unter: " + LOCAL_FILE_PATH);
            }

        } catch (IOException e) {
            System.out.println("Fehler beim Herstellen der Verbindung. Lade lokale Datei...");

        }

    }




}
