package com.github.paweluks.currencyconverter;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;


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
    private ComboBox<String> comboBox1;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        CurrencyConverterModel model = new CurrencyConverterModel();
        Task<Void> downloadTask = new Task<Void>() {
            @Override
            protected Void call() {
                // Der Download wird hier im Hintergrund ausgeführt
                model.downloadAndSaveJson();

                return null;
            }

            @Override
            protected void succeeded() {

            }



            @Override
            protected void failed() {
                // Fehlerbehandlung
                System.out.println("Fehler beim Herunterladen der JSON-Datei");
            }
        };
        Thread downloadThread = new Thread(downloadTask);
        downloadThread.setDaemon(true); // Programm kann auch beendet werden, wenn dieser Thread läuft
        downloadThread.start();
    }
}