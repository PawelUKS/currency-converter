module com.github.paweluks.currencyconverter {
    requires javafx.fxml;
    requires atlantafx.base;
    requires com.google.gson;
    requires com.fasterxml.jackson.databind;
    requires org.json;


    opens com.github.paweluks.currencyconverter to javafx.fxml;
    exports com.github.paweluks.currencyconverter;
}