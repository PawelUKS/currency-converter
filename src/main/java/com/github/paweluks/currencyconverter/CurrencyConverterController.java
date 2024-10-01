package com.github.paweluks.currencyconverter;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;


import java.net.URL;
import java.util.ResourceBundle;
import java.util.Map;

public class CurrencyConverterController implements Initializable {
    /*
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
     */


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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        CurrencyConverterModel model = new CurrencyConverterModel();
        Task<Void> downloadTask = new Task<Void>() {
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
                // Befülle die ComboBoxen mit den Währungsnamen
                for (String currencyCode : model.getAllCurrencies().keySet()) {
                    Map<String, String> currencyInfo = model.getCurrencyInfo(currencyCode);
                    if (currencyInfo != null) {
                        comboBox1.getItems().add(currencyInfo.get("name"));
                        comboBox2.getItems().add(currencyInfo.get("name"));
                    }
                }
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
    }
}