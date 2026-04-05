module com.mycompany.javaphone_nir2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.base;
    requires com.google.gson;
    requires java.desktop;
    requires webcam.capture;

    opens com.mycompany.javaphone_nir2 to javafx.fxml, com.google.gson;
    exports com.mycompany.javaphone_nir2;

    opens com.mycompany.javaphone_nir2.controllers to javafx.fxml, com.google.gson;
    exports com.mycompany.javaphone_nir2.controllers;

    opens com.mycompany.javaphone_nir2.models to com.google.gson;
    exports com.mycompany.javaphone_nir2.models;
}
