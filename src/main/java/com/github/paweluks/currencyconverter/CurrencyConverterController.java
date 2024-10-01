package com.github.paweluks.currencyconverter;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Map;

public class CurrencyConverterController implements Initializable {

    @FXML
    private Label label1;

    @FXML
    private Label label2;

    @FXML
    private TextField textField1;

    @FXML
    private TextField textField2;

    @FXML
    private ComboBox<String> comboBox1;

    @FXML
    private ComboBox<String> comboBox2;

    private CurrencyConverterModel model;
    private boolean isUpdating = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        model = new CurrencyConverterModel();
        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() {
                // Download JSON file from API in Background
                model.downloadAndSaveJson();
                return null;
            }

            @Override
            protected void succeeded() {
                // Nachdem die JSON-Datei heruntergeladen wurde, Daten in die Map laden
                model.jsonToMap();
                // Fülle die ComboBoxen mit den Währungsnamen aus der sortierten TreeMap
                for (String currencyName : model.getAllCurrencies().keySet()) {
                    comboBox1.getItems().add(currencyName);  // Währungsname in ComboBox1 hinzufügen
                    comboBox2.getItems().add(currencyName);  // Währungsname in ComboBox2 hinzufügen
                }

                // Setze Standardwährungen (z.B. "Euro" und "US-Dollar")
                comboBox1.getSelectionModel().select("Euro");
                comboBox2.getSelectionModel().select("US-Dollar");
                textField1.setText("1");

                // Aktualisiere den Wechselkurs sofort nach dem Laden der Daten
                updateConversion(true);
                // Listener für beide Textfelder
                textField1.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (!isUpdating) {
                        updateConversion(true);
                    }
                });

                textField2.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (!isUpdating) {
                        updateConversion(false);
                    }
                });
            }


            @Override
            protected void failed() {
                // Download failed
                System.out.println("Fehler beim Herunterladen der JSON-Datei");
            }
        };
        Thread downloadThread = new Thread(downloadTask);
        downloadThread.setDaemon(true); // App can be closed even if this thread is running
        downloadThread.start();

        comboBox1.setOnAction(event -> updateConversion(true));
        comboBox2.setOnAction(event -> updateConversion(true));
    }

    // Methode zur Aktualisierung des Wechselkurses basierend auf der Benutzerauswahl

    private void updateConversion(boolean fromTextField1) {
        isUpdating = true;

        String fromCurrency = comboBox1.getSelectionModel().getSelectedItem();
        String toCurrency = comboBox2.getSelectionModel().getSelectedItem();

        try {
            if (fromTextField1) {
                // Hole den Betrag aus textField1 (standardmäßig "1")
                double amount = Double.parseDouble(textField1.getText());
                BigDecimal result = model.convertFromSourceToTarget(amount, fromCurrency, toCurrency);
                //BigDecimal roundedResult = new BigDecimal(result).setScale(2, RoundingMode.HALF_UP);
                textField2.setText(result.toString());

            } else {
                double amount = Double.parseDouble(textField2.getText());
                BigDecimal result = model.convertFromTargetToSource(amount, fromCurrency, toCurrency);
                textField1.setText(result.toString());
            }
        } catch (NumberFormatException e) {
            if (fromTextField1) {
                textField2.setText("Ungültige Eingabe");
            } else {
                textField1.setText("Ungültige Eingabe");
            }
        } finally {
            isUpdating = false;  // Sperre entfernen
        }
    }
}