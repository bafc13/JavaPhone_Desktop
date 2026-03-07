module com.mycompany.javaphone_nir2 {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.mycompany.javaphone_nir2 to javafx.fxml;
    exports com.mycompany.javaphone_nir2;
}
