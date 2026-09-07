module org.example.rouge {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.rouge to javafx.fxml;
    exports org.example.rouge;
}