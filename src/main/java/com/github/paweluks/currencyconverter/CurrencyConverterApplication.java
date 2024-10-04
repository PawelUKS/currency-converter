package com.github.paweluks.currencyconverter;

import atlantafx.base.theme.Dracula;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class CurrencyConverterApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(CurrencyConverterApplication.class.getResource("currency-converter-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 424, 240);

        // Theme (stylesheet) für die ganze Anwendung wird hier gesetzt
        Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet());
        
        stage.setResizable(false);
        stage.setTitle("Währungsrechner");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}