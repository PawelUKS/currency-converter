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
    private Map<String, Map<String, String>> currencyData = new HashMap<>();

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
            System.out.println("Fehler beim Herunterladen der JSON-Datei: " + e.getMessage());
        }

    }

    // Daten einmal aus der JSON-Datei laden
    public void jsonToMap() {
        Path path = Paths.get(LOCAL_FILE_PATH);
        if (Files.exists(path)) {
            Gson gson = new Gson();
            try (FileReader reader = new FileReader(LOCAL_FILE_PATH)) {
                Type type = new TypeToken<HashMap<String, Map<String, String>>>() {}.getType();
                currencyData = gson.fromJson(reader, type);
                System.out.println("JSON-Datei erfolgreich geladen.");
            } catch (IOException e) {
                System.out.println("Fehler beim Laden der JSON-Datei: " + e.getMessage());
            }
        } else {
            System.out.println("Die Datei existiert nicht: " + LOCAL_FILE_PATH);
        }
    }

    // Schneller Zugriff auf Währungsinformationen aus der Map
    public Map<String, String> getCurrencyInfo(String currencyCode) {
        return currencyData.get(currencyCode.toLowerCase());
    }

    // Rückgabe aller Währungen
    public Map<String, Map<String, String>> getAllCurrencies() {
        return currencyData;
    }


}
