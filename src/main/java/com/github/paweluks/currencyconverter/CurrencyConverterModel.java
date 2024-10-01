package com.github.paweluks.currencyconverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.TreeMap;


public class CurrencyConverterModel {
    private static final String API_URL = "https://www.floatrates.com/daily/usd.json";
    private static final String LOCAL_FILE_PATH = "target/data/currency.json";
    private static final String LOCAL_PATH = "target/data/";
    private Map<String, Map<String, String>> currencyData = new TreeMap<>();

    public void createPath() {
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
                Type type = new TypeToken<HashMap<String, Map<String, String>>>() {
                }.getType();
                Map<String, Map<String, String>> tempCurrencyData = gson.fromJson(reader, type);

                // Iteriere über die temporäre Map und füge die Währungen mit dem Namen als Schlüssel in die TreeMap ein
                for (Map.Entry<String, Map<String, String>> entry : tempCurrencyData.entrySet()) {
                    String currencyName = entry.getValue().get("name").trim();
                    currencyData.put(currencyName, entry.getValue());
                }
                // This JSON file is base on USD so itself is not included. We have to include it manually with a rate of 1.0
                Map<String, String> usdInfo = new HashMap<>();
                usdInfo.put("name", "US-Dollar");
                usdInfo.put("alphaCode", "USD");
                usdInfo.put("rate", "1.0"); // 1 USD = 1 USD
                usdInfo.put("date", "current"); // Optional: Setze das aktuelle Datum

                // Füge USD zur Map hinzu
                currencyData.put("US-Dollar", usdInfo);

                System.out.println("JSON-Datei erfolgreich geladen und Währungen nach Namen sortiert.");

            } catch (IOException e) {
                System.out.println("Fehler beim Laden der JSON-Datei: " + e.getMessage());
            }
        } else {
            System.out.println("Die Datei existiert nicht: " + LOCAL_FILE_PATH);
        }
    }

    // Zugriff auf Währungsinformationen anhand des Namens
    public Map<String, String> getCurrencyInfo(String currencyName) {
        Map<String, String> currencyInfo = currencyData.get(currencyName);
        if (currencyInfo == null) {
            System.out.println("Währung '" + currencyName + "' wurde nicht gefunden.");
        }
        return currencyInfo;
    }

    // Rückgabe aller Währungen
    public Map<String, Map<String, String>> getAllCurrencies() {
        return Collections.unmodifiableMap(currencyData);
    }

    // Berechnung von Quellwährung (z.B. EUR) zu Zielwährung (z.B. USD)
    public BigDecimal convertFromSourceToTarget(double amount, String fromCurrency, String toCurrency) {
        Map<String, String> fromCurrencyInfo = getCurrencyInfo(fromCurrency);
        Map<String, String> toCurrencyInfo = getCurrencyInfo(toCurrency);

        if (fromCurrencyInfo != null && toCurrencyInfo != null) {
            double fromRate = Double.parseDouble(fromCurrencyInfo.get("rate"));
            double toRate = Double.parseDouble(toCurrencyInfo.get("rate"));
            double result = (amount / fromRate) * toRate;
            return new BigDecimal(result).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    // Berechnung von Zielwährung (z.B. USD) zu Quellwährung (z.B. EUR)
    public BigDecimal convertFromTargetToSource(double amount, String fromCurrency, String toCurrency) {
        return convertFromSourceToTarget(amount, toCurrency, fromCurrency); // Umgekehrte Berechnung
    }


}
