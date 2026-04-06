module com.mycompany.javaphone_nir2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.base;
    requires com.fasterxml.jackson.databind;
    requires jakarta.websocket;
    requires webrtc.java;
    requires java.desktop;

    opens com.mycompany.javaphone_nir2 to javafx.fxml, com.fasterxml.jackson.databind;
    exports com.mycompany.javaphone_nir2;

    opens com.mycompany.javaphone_nir2.controllers to javafx.fxml, com.fasterxml.jackson.databind;
    exports com.mycompany.javaphone_nir2.controllers;

    opens com.mycompany.javaphone_nir2.models to com.fasterxml.jackson.databind;
    exports com.mycompany.javaphone_nir2.models;
}
