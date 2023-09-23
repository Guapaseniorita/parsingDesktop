module com.example.parsingdesktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.google.gson;
    requires org.apache.poi.poi;


    opens com.example.parsingdesktop to javafx.fxml;
    exports com.example.parsingdesktop;
}