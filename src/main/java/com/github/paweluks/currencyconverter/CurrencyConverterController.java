package com.github.paweluks.currencyconverter;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class CurrencyConverterController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
}