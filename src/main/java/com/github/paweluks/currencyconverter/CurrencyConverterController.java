package com.github.paweluks.currencyconverter;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.concurrent.Task;
import javafx.scene.control.TextFormatter;

import java.math.BigDecimal;
import java.net.URL;
import java.util.*;
import java.util.function.UnaryOperator;

public class CurrencyConverterController implements Initializable {

    @FXML
    private Label label1, label2, label3;

    @FXML
    private TextField textField1, textField2;

    @FXML
    private ComboBox<String> comboBox1, comboBox2;

    private CurrencyConverterModel model;

    // Flag zur Verhinderung rekursiver Updates
    private boolean isUpdating = false;

    // Maps zur Unterstützung der Autovervollständigung
    private final Map<ComboBox<String>, String> searchStrings = new HashMap<>();
    private final Map<ComboBox<String>, Long> lastKeyPressTimes = new HashMap<>();


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        model = new CurrencyConverterModel();
        initializeComboBoxes();
        loadDataInBackground();
    }

    private void initializeComboBoxes() {
        comboBox1.setEditable(false);
        comboBox2.setEditable(false);

        // Fügt Aktionen hinzu, die bei Auswahl einer Währung ausgeführt werden
        comboBox1.setOnAction(event -> updateConversion(textField1));
        comboBox2.setOnAction(event -> updateConversion(textField2));

        // Fügt die Autovervollständigung hinzu
        comboBox1.setOnKeyReleased(event -> autoCompleteComboBox(comboBox1, event.getText().toLowerCase()));
        comboBox2.setOnKeyReleased(event -> autoCompleteComboBox(comboBox2, event.getText().toLowerCase()));
    }

    private void loadDataInBackground() {
        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() {
                model.downloadAndSaveJson();
                return null;
            }

            @Override
            protected void succeeded() {
                model.jsonToMap();
                setupUI();
            }

            @Override
            protected void failed() {
                System.out.println("Fehler beim Herunterladen der JSON-Datei");
            }
        };
        Thread downloadThread = new Thread(downloadTask);
        downloadThread.setDaemon(true); // Beendet den Thread automatisch beim Schließen der Anwendung
        downloadThread.start();
    }

    private void setupUI() {
        // Holt alle verfügbaren Währungen aus dem Modell
        Set<String> currencies = model.getAllCurrencies().keySet();

        // Fügt die Währungen den ComboBoxen hinzu
        comboBox1.getItems().addAll(currencies);
        comboBox2.getItems().addAll(currencies);

        // Wählt Standardwährungen und -beträge aus
        comboBox1.getSelectionModel().select("Euro");
        comboBox2.getSelectionModel().select("US-Dollar");
        textField1.setText("1");

        // Aktualisiert die Anzeige
        updateTimestampLabel();
        updateConversion(textField1);
        updateLabels();
        addTextFieldListeners();
        addTextFormatters();
    }

    private void addTextFieldListeners() {
        textField1.textProperty().addListener((observable, oldValue, newValue) ->
                handleTextFieldChange(textField1));

        textField2.textProperty().addListener((observable, oldValue, newValue) ->
                handleTextFieldChange(textField2));
    }
    private void addTextFormatters() {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            // Erlaubt Ziffern, optional gefolgt von Komma oder Punkt und weiteren Ziffern
            if (newText.matches("\\d*([.,]\\d*)?")) {
                return change;
            }
            return null; // Verwirft ungültige Eingaben
        };

        TextFormatter<String> textFormatter1 = new TextFormatter<>(filter);
        TextFormatter<String> textFormatter2 = new TextFormatter<>(filter);

        textField1.setTextFormatter(textFormatter1);
        textField2.setTextFormatter(textFormatter2);
    }


    public void autoCompleteComboBox(ComboBox<String> comboBox, String key) {
        long currentTime = System.currentTimeMillis();
        String searchString = searchStrings.getOrDefault(comboBox, "");
        long lastKeyPressTime = lastKeyPressTimes.getOrDefault(comboBox, 0L);

        // Setzt den Suchstring zurück, wenn die letzte Eingabe länger als 1 Sekunde her ist
        if (currentTime - lastKeyPressTime > 1000) {
            searchString = "";
        }

        searchString += key;
        lastKeyPressTime = currentTime;

        searchStrings.put(comboBox, searchString);
        lastKeyPressTimes.put(comboBox, lastKeyPressTime);
        // Sucht nach passenden Einträgen in der ComboBox
        if (!searchString.isEmpty()) {
           for (String item : comboBox.getItems()) {
                if (item.toLowerCase().startsWith(searchString)) {
                    comboBox.getSelectionModel().select(item);
                    break;
                }
            }
        }
    }

    public void updateTimestampLabel() {
        String timestamp = model.getFirstDateFromJson();
        label3.setText(timestamp != null ? "Letzte Aktualisierung: " + timestamp : "");

    }

    public void updateLabels(){
        if(!textField1.getText().isEmpty()){
            label1.setText(textField1.getText().replace(".",",") + " "+ comboBox1.getValue() + " entspricht");
        }
        if(!textField2.getText().isEmpty()) {
            label2.setText(textField2.getText().replace(".",",") + " " + comboBox2.getValue());
        }
    }

    private void handleTextFieldChange(TextField sourceTextField) {
        if (isUpdating) {
            return; // Verhindert rekursive Aufrufe
        }
        isUpdating = true;


        try {
            String text = sourceTextField.getText();
            // Fügt eine führende Null hinzu, wenn die Eingabe mit Komma oder Punkt beginnt
            if (text.startsWith(".") || text.startsWith(",")) {
                text = "0" + text;
                sourceTextField.setText(text);
            }
            updateConversion(sourceTextField);
            updateLabels();
        } finally {
        isUpdating = false; // Sperre entfernen
        }
    }
    private void updateConversion(TextField sourceTextField) {
        isUpdating = true;
        String fromCurrency = comboBox1.getSelectionModel().getSelectedItem();
        String toCurrency = comboBox2.getSelectionModel().getSelectedItem();

        try {
            if (sourceTextField == textField1) {
                // Umrechnung von TextField1 zu TextField2
                String inputText = textField1.getText().replace(",",".");
                if (!inputText.isEmpty()) {
                    double amount = Double.parseDouble(inputText);
                    BigDecimal result = model.convertFromSourceToTarget(amount, fromCurrency, toCurrency);
                    textField2.setText(result.toString().replace(".",","));
                }else {
                    textField2.clear();
                }

            } else if(sourceTextField == textField2) {
                // Umrechnung von TextField2 zu TextField1
                String inputText = textField2.getText().replace(",",".");
                if (!inputText.isEmpty()) {
                    double amount = Double.parseDouble(inputText);
                    BigDecimal result = model.convertFromTargetToSource(amount, fromCurrency, toCurrency);
                    textField1.setText(result.toString().replace(".",","));
                }else {
                    textField1.clear();
                }
            }

        } finally {
            isUpdating = false;  // Sperre entfernen
            updateLabels();
        }
    }
}

































