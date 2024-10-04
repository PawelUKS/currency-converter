package com.github.paweluks.currencyconverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.json.JSONObject;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class CurrencyConverterModel {

    // Konstanten für API-URL und lokale Pfade
    private static final String API_URL = "https://www.floatrates.com/daily/usd.json";
    private static final String LOCAL_FILE_PATH = "target/data/currency.json";
    private static final String LOCAL_PATH = "target/data/";

    // Map zum Speichern der Währungsdaten
    private final Map<String, Map<String, String>> currencyData = new TreeMap<>();

    // Erstellt das lokale Verzeichnis, falls es nicht existiert
    public void createPath() {
        try {
            Files.createDirectories(Paths.get(LOCAL_PATH));
        } catch (IOException e) {
            System.out.println("Fehler beim Erstellen des Verzeichnisses: " + e.getMessage());
        }
    }

    // Lädt die JSON-Daten herunter und speichert sie lokal, wenn sich das Datum geändert hat
    public void downloadAndSaveJson() {
        createPath();
        Path path = Paths.get(LOCAL_FILE_PATH);

        try {
            // Verbindung zur API herstellen
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Lade den neuen InputStream von der API
            try (InputStream inputStream = connection.getInputStream()) {
                String newJsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

                // Prüfen, ob die lokale Datei existiert
                if (Files.exists(path)) {

                    // Lese die bestehende JSON-Datei
                    String existingJsonContent = Files.readString(path, StandardCharsets.UTF_8);

                    // Extrahiere das Datum aus beiden JSON-Dateien
                    String existingDate = extractDateFromJson(existingJsonContent);
                    String newDate = extractDateFromJson(newJsonContent);

                    // Vergleiche die Datumswerte
                    if (existingDate.equals(newDate)) {
                        System.out.println("Das Datum ist gleich. Kein Überschreiben erforderlich.");
                    } else {
                        // Speichere die neue JSON-Datei
                        saveJsonToFile(path, newJsonContent);
                        System.out.println("Das Datum hat sich geändert. Neue JSON-Datei gespeichert.");
                    }
                } else {
                    // Speichere die neue JSON-Datei
                    saveJsonToFile(path, newJsonContent);
                    System.out.println("Die Datei wurde neu erstellt.");
                }
            }

        } catch (IOException e) {
            System.out.println("Fehler beim Herunterladen der JSON-Datei: " + e.getMessage());
        }
    }

    // Hilfsmethode zum Speichern des JSON-Inhalts in eine Datei
    private void saveJsonToFile(Path path, String jsonContent) {
        try {
            Files.writeString(path, jsonContent, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Fehler beim Speichern der JSON-Datei: " + e.getMessage());
        }
    }

    // Extrahiert das Datum aus dem JSON-String
    private String extractDateFromJson(String jsonContent) {
        JSONObject jsonObject = new JSONObject(jsonContent);

        // Nimmt den ersten Key und extrahiert das Datum
        Iterator<String> keys = jsonObject.keys();
        if (keys.hasNext()) {
            String firstKey = keys.next();
            return jsonObject.getJSONObject(firstKey).getString("date");
        }
        return null;
    }

    // Extrahiert das Datum aus der lokalen JSON-Datei und formatiert es
    public String getFirstDateFromJson() {
        Path path = Paths.get(LOCAL_FILE_PATH);

        if (Files.exists(path)) {
            try {
                String content = Files.readString(path, StandardCharsets.UTF_8);
                JSONObject jsonObject = new JSONObject(content);

                // Nimmt den ersten Key und extrahiert das Datum
                Iterator<String> keys = jsonObject.keys();
                if (keys.hasNext()) {
                    String firstKey = keys.next();
                    String firstDate = jsonObject.getJSONObject(firstKey).getString("date");

                    // Konvertiere das Datum von GMT in Europe/Berlin Zeit
                    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH);
                    ZonedDateTime gmtDateTime = ZonedDateTime.parse(firstDate, inputFormatter);
                    ZonedDateTime berlinDateTime = gmtDateTime.withZoneSameInstant(ZoneId.of("Europe/Berlin"));

                    // Formatieren des Datums in das gewünschte Format
                    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
                    return berlinDateTime.format(outputFormatter);
                }

            } catch (Exception e) {
                System.err.println("Fehler beim Extrahieren des Datums: " + e.getMessage());
            }
        }

        // Rückgabe null, wenn Datei nicht existiert oder ein Fehler auftritt
        return null;
    }

    // Lädt die Währungsdaten aus der JSON-Datei in die Map
    public void jsonToMap() {
        Path path = Paths.get(LOCAL_FILE_PATH);
        if (Files.exists(path)) {
            try (FileReader reader = new FileReader(LOCAL_FILE_PATH)) {
                Gson gson = new Gson();
                Type type = new TypeToken<HashMap<String, Map<String, String>>>() {
                }.getType();
                Map<String, Map<String, String>> tempCurrencyData = gson.fromJson(reader, type);

                // Iteriere über die temporäre Map und füge die Währungen hinzu
                for (Map.Entry<String, Map<String, String>> entry : tempCurrencyData.entrySet()) {
                    Map<String, String> currencyInfo = entry.getValue();
                    String currencyName = currencyInfo.get("name").trim();
                    currencyData.put(currencyName, currencyInfo);
                }

                // Füge die Basiswährung (US-Dollar) hinzu, falls nicht vorhanden
                if (!currencyData.containsKey("US-Dollar")) {
                    Map<String, String> usdInfo = new HashMap<>();
                    usdInfo.put("code", "USD");
                    usdInfo.put("name", "US-Dollar");
                    usdInfo.put("rate", "1.0");
                    usdInfo.put("date", "current");
                    currencyData.put("US-Dollar", usdInfo);
                }

                System.out.println("Währungsdaten erfolgreich geladen.");

            } catch (IOException e) {
                System.err.println("Fehler beim Laden der JSON-Datei: " + e.getMessage());
            }
        } else {
            System.err.println("Die Datei existiert nicht: " + LOCAL_FILE_PATH);
        }
    }

    // Gibt Währungsinformationen anhand des Namens zurück
    public Map<String, String> getCurrencyInfo(String currencyName) {
        Map<String, String> currencyInfo = currencyData.get(currencyName);
        if (currencyInfo == null) {
            System.out.println("Währung '" + currencyName + "' wurde nicht gefunden.");
        }
        return currencyInfo;
    }

    // Gibt alle verfügbaren Währungen zurück
    public Map<String, Map<String, String>> getAllCurrencies() {
        return Collections.unmodifiableMap(currencyData);
    }

    // Konvertiert einen Betrag von der Quell- zur Zielwährung
    public BigDecimal convertFromSourceToTarget(double amount, String fromCurrency, String toCurrency) {
        Map<String, String> fromCurrencyInfo = getCurrencyInfo(fromCurrency);
        Map<String, String> toCurrencyInfo = getCurrencyInfo(toCurrency);

        if (fromCurrencyInfo != null && toCurrencyInfo != null) {
            BigDecimal fromRate = new BigDecimal(fromCurrencyInfo.get("rate"));
            BigDecimal toRate = new BigDecimal(toCurrencyInfo.get("rate"));
            BigDecimal amountBD = new BigDecimal(amount);

            // Berechnung der Umrechnung
            BigDecimal result = amountBD.divide(fromRate, 10, RoundingMode.HALF_UP) // Zuerst Umrechnung von "from"
                    .multiply(toRate);  // Danach Umrechnung zu "to"
            System.out.println(result.toString());

            // Formatierung des Ergebnisses
            return formatCurrencyDynamically(result);
        }
        return BigDecimal.ZERO;
    }

    // Umgekehrte Umrechnung von der Ziel- zur Quellwährung
    public BigDecimal convertFromTargetToSource(double amount, String fromCurrency, String toCurrency) {
        // Tauscht die Währungen für die umgekehrte Berechnung
        return convertFromSourceToTarget(amount, toCurrency, fromCurrency); // Umgekehrte Berechnung
    }

    // Formatiert Beträge dynamisch basierend auf der Größe des Wertes
    public BigDecimal formatCurrencyDynamically(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            // Bei Null wird auf zwei Dezimalstellen gerundet
            return value.setScale(2, RoundingMode.HALF_UP);
        }

        String valueStr = value.stripTrailingZeros().toPlainString();

        if (value.compareTo(BigDecimal.ONE) < 0) {
            // Für Werte kleiner als 1 wird die Anzahl der Dezimalstellen dynamisch angepasst
            int decimalPlacesToKeep = 0;
            boolean foundNonZero = false;

            for (int i = 2; i < valueStr.length(); i++) {
                char currentChar = valueStr.charAt(i);
                if (currentChar != '0' && currentChar != '.') {
                    foundNonZero = true;
                }

                if (foundNonZero) {
                    decimalPlacesToKeep = i;
                    break;
                }
            }

            return value.setScale(decimalPlacesToKeep, RoundingMode.HALF_UP);
        }
        
        // Für Werte größer oder gleich 1 wird auf zwei Dezimalstellen gerundet
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
