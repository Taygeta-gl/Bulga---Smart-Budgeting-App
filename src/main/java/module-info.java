module com.budgetapp {
    requires javafx.controls;
    requires javafx.fxml;

    requires transitive javafx.graphics;
    requires transitive javafx.base;

    opens com.budgetapp to javafx.fxml;

    exports com.budgetapp;
}
