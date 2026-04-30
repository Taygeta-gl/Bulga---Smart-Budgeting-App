package com.budgetapp;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class PrimaryController {

    @FXML
    private TextField nameInput;
    @FXML
    private Label totalDisplay;

    // Create one root Section to hold everything
    private Section rootSection = new Section("Master Budget", "Main Container", Importance.HIGH);

    @FXML
    private void handleAddSection() {
        String name = nameInput.getText();

        if (!name.isEmpty()) {
            // Use your Section logic!
            Section newSub = new Section(name, "Added via GUI", 50.0, Importance.MEDIUM); // Example $50 cost
            rootSection.addSubsection(newSub);

            // Update the GUI display
            totalDisplay.setText("Total: $" + rootSection.getGrandTotal());
            nameInput.clear();
        }
    }

    @FXML
    private void switchToSecondary() throws Exception {
        App.setRoot("secondary");
    }
}
