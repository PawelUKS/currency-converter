module com.github.paweluks.currencyconverter {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.github.paweluks.currencyconverter to javafx.fxml;
    exports com.github.paweluks.currencyconverter;
}