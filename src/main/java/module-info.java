module com.github.paweluks.currencyconverter {
    requires javafx.fxml;
    requires atlantafx.base;


    opens com.github.paweluks.currencyconverter to javafx.fxml;
    exports com.github.paweluks.currencyconverter;
}