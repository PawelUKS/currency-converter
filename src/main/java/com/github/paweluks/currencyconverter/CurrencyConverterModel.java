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
import java.nio.file.StandardOpenOption;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CurrencyConverterModel {
    private static final String API_URL = "https://www.floatrates.com/daily/usd.json";
    private static final String LOCAL_FILE_PATH = "target/data/currency.json";
    private static final String LOCAL_PATH = "target/data/";
    private final Map<String, Map<String, String>> currencyData = new TreeMap<>();

    public void createPath() {
        // Create LOCAL_PATH
        try {
            Files.createDirectories(Paths.get(LOCAL_PATH));
        } catch (IOException e) {
            System.out.println("Fehler beim Erstellen des Verzeichnisses: " + e.getMessage());
        }
    }



    public void downloadAndSaveJson() {
        createPath();
        Path path = Paths.get(LOCAL_FILE_PATH);

        try {
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Lade den neuen InputStream von der API
            try (InputStream inputStream = connection.getInputStream()) {
                // JSON-Inhalt als String laden (aus dem neuen InputStream)
                String newJsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

                if(newJsonContent.isEmpty()){
                    System.out.println("EMPTY");
                }else{
                    System.out.println("IS HERE");
                }
                // Falls die lokale Datei existiert, vergleiche die "rates"
                if (Files.exists(path)) {
                    System.out.println("path existiert");
                    // Lese die bestehende JSON-Datei
                    String existingJsonContent = Files.readString(path, StandardCharsets.UTF_8);
                    System.out.println(existingJsonContent);
                    // Extrahiere die "rates" aus beiden JSON-Dateien
                    Map<String, Double> existingRates = extractRatesFromJson(existingJsonContent);
                    Map<String, Double> newRates = extractRatesFromJson(newJsonContent);
                    for (Map.Entry<String, Double> entry : existingRates.entrySet()) {
                        System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
                    }

                    for (Map.Entry<String, Double> entry : newRates.entrySet()) {
                        System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
                    }
                    // Vergleiche die "rates"
                    if (existingRates.equals(newRates)) {
                        System.out.println("Die Währungskurse sind gleich. Kein Überschreiben erforderlich.");

                    } else {
                        // Wenn die "rates" unterschiedlich sind, schreibe die neue Datei
                        Files.writeString(path, newJsonContent, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                        System.out.println("Die Währungskurse haben sich geändert. Neue JSON-Datei gespeichert.");

                    }
                } else {
                    // Falls die Datei nicht existiert, speichere sie einfach
                    Files.writeString(path, newJsonContent, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    System.out.println("Die Datei wurde neu erstellt.");

                }
            }

        } catch (IOException e) {
            System.out.println("Fehler beim Herunterladen der JSON-Datei: " + e.getMessage());
        }
    }

    // Methode, um das erste Date zu extrahieren und zu konvertieren
    public String getFirstDateFromJson() {
        Path path = Paths.get(LOCAL_FILE_PATH);

        if (Files.exists(path)) {
            try {
                String content = new String(Files.readAllBytes(path));
                JSONObject jsonObject = new JSONObject(content);

                // Extrahiere das erste "date" Feld
                String firstDate = jsonObject.getJSONObject("aud").getString("date");  // Beispiel: AUD als erste Währung

                // Konvertiere das Datum von GMT in EU/Berlin Zeit
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH);
                ZonedDateTime gmtDateTime = ZonedDateTime.parse(firstDate, inputFormatter);
                ZonedDateTime berlinDateTime = gmtDateTime.withZoneSameInstant(ZoneId.of("Europe/Berlin"));

                // Formatieren des Datums in das gewünschte Format (Tag, Monat, Jahr, Uhrzeit mit Sekunden)
                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
                return berlinDateTime.format(outputFormatter);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Rückgabe null, wenn Datei nicht existiert oder ein Fehler auftritt
        return null;
    }

    // Hilfsmethode, um die "rates" aus einer JSON-Datei zu extrahieren
    private Map<String, Double> extractRatesFromJson(String jsonContent) {
        Map<String, Double> ratesMap = new HashMap<>();

        try {
            // Verwende eine JSON-Bibliothek wie Jackson oder Gson, um die "rates" zu extrahieren
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonContent);

            // Navigiere zum "rates"-Knoten
            JsonNode ratesNode = rootNode.path("rates");
            Iterator<Map.Entry<String, JsonNode>> fields = ratesNode.fields();

            // Füge jede Währung und ihren Kurs zur Map hinzu
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                ratesMap.put(field.getKey(), field.getValue().asDouble());
            }
        } catch (IOException e) {
            System.out.println("Fehler beim Verarbeiten der JSON-Daten: " + e.getMessage());
        }

        return ratesMap;
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

    public BigDecimal convertFromSourceToTarget(double amount, String fromCurrency, String toCurrency) {
        Map<String, String> fromCurrencyInfo = getCurrencyInfo(fromCurrency);
        Map<String, String> toCurrencyInfo = getCurrencyInfo(toCurrency);

        if (fromCurrencyInfo != null && toCurrencyInfo != null) {
            BigDecimal fromRate = new BigDecimal(fromCurrencyInfo.get("rate"));
            BigDecimal toRate = new BigDecimal(toCurrencyInfo.get("rate"));
            BigDecimal amountBD = new BigDecimal(amount);

            // Berechne das Ergebnis der Umrechnung
            BigDecimal result = amountBD.divide(fromRate, 10, RoundingMode.HALF_UP) // Zuerst Umrechnung von "from"
                    .multiply(toRate);  // Danach Umrechnung zu "to"
            System.out.println(result.toString());

            // Rufe die dynamische Formatierungsmethode auf
            return formatCurrencyDynamically(result);
        }
        return BigDecimal.ZERO;
    }




    // Methode zum Formatieren von Beträgen, dynamisch je nach Anzahl der führenden Nullen nach dem Komma
    public BigDecimal formatCurrencyDynamically(BigDecimal value) {
        // Falls der Wert 0 ist, direkt auf zwei Dezimalstellen formatieren
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return value.setScale(2, RoundingMode.HALF_UP);
        }

        // String-Repräsentation des Wertes holen, um Nachkommastellen zu analysieren
        String valueStr = value.stripTrailingZeros().toPlainString();

        // Dynamische Rundung für sehr kleine Werte (kleiner als 1)
        if (value.compareTo(BigDecimal.ONE) < 0) {
            int decimalPlacesToKeep = 0;
            boolean foundNonZero = false;

            for (int i = 2; i < valueStr.length(); i++) {
                char currentChar = valueStr.charAt(i);
                if (currentChar != '0' && currentChar != '.') {
                    foundNonZero = true;
                }

                if (foundNonZero) {
                    decimalPlacesToKeep = i - 1 + 1; // Nur eine Nachkommastelle nach der ersten relevanten Ziffer
                    break;
                }
            }

            // Wende die dynamische Rundung an
            return value.setScale(decimalPlacesToKeep, RoundingMode.HALF_UP);
        }

        // Normale Rundung für Werte größer als 1
        return value.setScale(2, RoundingMode.HALF_UP);
    }



    // Berechnung von Zielwährung (z.B. USD) zu Quellwährung (z.B. EUR)
    public BigDecimal convertFromTargetToSource(double amount, String fromCurrency, String toCurrency) {
        return convertFromSourceToTarget(amount, toCurrency, fromCurrency); // Umgekehrte Berechnung
    }


}
