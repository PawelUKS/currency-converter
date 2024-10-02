package com.github.paweluks.currencyconverter;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Map;
import java.util.Set;

public class CurrencyConverterController implements Initializable {

    @FXML
    private Label label1;

    @FXML
    private Label label2;

    @FXML
    private Label label3;

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
    // Variable für den Suchstring
    private String searchString = "";
    private long lastKeyPressTime = 0;

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
                // Load JSON data in Map
                model.jsonToMap();

                Set<String> data = model.getAllCurrencies().keySet();

                comboBox1.getItems().addAll(data);
                comboBox2.getItems().addAll(data);

                // Set start-currencies
                comboBox1.getSelectionModel().select("Euro");
                comboBox2.getSelectionModel().select("US-Dollar");
                // Set startvalue for the textField1
                textField1.setText("1");
                updateTimestampLabel();
                // Calc the currency and update the labels
                updateConversion(true);
                updateLabels();
                // Listener for both textfields
                textField1.textProperty().addListener((observable, oldValue, newValue) -> {
                    handleTextFieldChange(oldValue, newValue, textField1, true);
                });

                textField2.textProperty().addListener((observable, oldValue, newValue) -> {
                    handleTextFieldChange(oldValue, newValue, textField2, false);
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

        comboBox1.setOnAction(event -> {

            updateConversion(true);

        });

        comboBox2.setOnAction(event -> {

            updateConversion(true);

        });

        comboBox1.setEditable(false);  // Stelle sicher, dass die ComboBox nicht editierbar ist
        comboBox2.setEditable(false);
        comboBox1.setOnKeyReleased(event -> {
            String key = event.getText().toLowerCase(); // Der eingegebene Buchstabe
            autoCompleteComboBox(comboBox1, key);
        });

        comboBox2.setOnKeyReleased(event -> {
            String key = event.getText().toLowerCase(); // Der eingegebene Buchstabe
            autoCompleteComboBox(comboBox2, key);
        });


    }

    public void autoCompleteComboBox(ComboBox<String> comboBox, String key) {
        long currentTime = System.currentTimeMillis();

        // Überprüfe, ob seit der letzten Eingabe mehr als 1 Sekunde vergangen ist
        if (currentTime - lastKeyPressTime > 1000) {
            searchString = ""; // Reset the search string
        }

        // Füge den aktuellen Buchstaben zum Suchstring hinzu
        searchString += key;
        lastKeyPressTime = currentTime; // Aktualisiere die Zeit der letzten Eingabe

        if (!searchString.isEmpty()) {
            // Durchlaufe alle Einträge in der übergebenen ComboBox
            for (String item : comboBox.getItems()) {
                // Suche den ersten Eintrag, der mit dem Suchstring beginnt
                if (item.toLowerCase().startsWith(searchString)) {
                    comboBox.getSelectionModel().select(item); // Wähle das passende Element aus
                    break;
                }
            }
        }
    }

    public void updateTimestampLabel() {
        String timestamp = model.getFirstDateFromJson();
        if (timestamp != null) {
            label3.setText("Letzte Aktualisierung: " + timestamp);
        } else {
            label3.setText("");
        }
    }

    public void updateLabels(){
        if(!textField1.getText().isEmpty()){
            label1.setText(textField1.getText().replace(".",",") + " "+ comboBox1.getValue() + " entspricht");
        }
        if(!textField2.getText().isEmpty()) {
            label2.setText(textField2.getText().replace(".",",") + " " + comboBox2.getValue());
        }
    }
    // Methode zur Aktualisierung des Wechselkurses basierend auf der Benutzerauswahl
    // Methode zur Verarbeitung der Textfeldänderung
    private void handleTextFieldChange(String oldValue, String newValue, TextField textField, boolean fromTextField1) {
        if (!isUpdating) {
            newValue = newValue.replace(",", ".");

            if (newValue.startsWith(".") && newValue.length() > 1 && Character.isDigit(newValue.charAt(1))) {
                newValue = "0" + newValue;
            }

            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                textField.setText(oldValue);
            } else {
                textField.setText(newValue);
            }

            // Call Model for conversion
            updateConversion(fromTextField1);
            updateLabels();
        }
    }
    private void updateConversion(boolean fromTextField1) {
        isUpdating = true;


        String fromCurrency = comboBox1.getSelectionModel().getSelectedItem();
        String toCurrency = comboBox2.getSelectionModel().getSelectedItem();

        try {
            if (fromTextField1) {
                String inputText = textField1.getText();
                System.out.println(inputText);
                // Hole den Betrag aus textField1 (standardmäßig "1")
                if (isValidNumber(inputText)) {
                    double amount = Double.parseDouble(inputText);
                    BigDecimal result = model.convertFromSourceToTarget(amount, fromCurrency, toCurrency);
                    //String result = model.convertFromSourceToTarget(amount, fromCurrency, toCurrency);
                    //BigDecimal roundedResult = new BigDecimal(result).setScale(2, RoundingMode.HALF_UP);
                    textField2.setText(result.toString());
                }

            } else {
                double amount = Double.parseDouble(textField2.getText());
                BigDecimal result = model.convertFromTargetToSource(amount, fromCurrency, toCurrency);
                //String result = model.convertFromSourceToTarget(amount, fromCurrency, toCurrency);
                textField1.setText(result.toString());
            }


        } finally {
            isUpdating = false;  // Sperre entfernen
            updateLabels();
        }
    }

    // Methode zur Überprüfung, ob eine Eingabe eine gültige Zahl ist
    private boolean isValidNumber(String input) {
        try {
            Double.parseDouble(input);
            System.out.println("TRUE");
            return true;  // Es handelt sich um eine gültige Zahl
        } catch (NumberFormatException e) {
            System.out.println("FALSE");
            return false;  // Es handelt sich um keine gültige Zahl
        }
    }
}