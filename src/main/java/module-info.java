module com.example.parsingdesktop {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.parsingdesktop to javafx.fxml;
    exports com.example.parsingdesktop;
}