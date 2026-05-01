package com.budgetapp;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class PrimaryController {
    @FXML
    private TextField nameField, descField, costField;
    @FXML
    private ComboBox<Importance> importancePicker;
    @FXML
    private ComboBox<Section> parentPicker;
    @FXML
    private ListView<Section> sectionListView;

    private Section rootSection = new Section("Root", "Master", Importance.HIGH);

    @FXML
    public void initialize() {
        // This fills the dropdown with LOW, MEDIUM, HIGH automatically
        importancePicker.getItems().setAll(Importance.values());
        parentPicker.setItems(sectionListView.getItems());
    }

    @FXML
    private void handleAddSection() {
        Section newSection = createSectionFromFields();
        if (newSection == null) {
            return;
        }

        rootSection.addSubsection(newSection);
        sectionListView.getItems().add(newSection);
        clearFields();
    }

    @FXML
    private void handleAddSubsection() {
        Section selectedParent = parentPicker.getValue();
        if (selectedParent == null) {
            return; // no parent selected, do nothing
        }

        Section newSection = createSectionFromFields();
        if (newSection == null) {
            return;
        }

        selectedParent.addSubsection(newSection);
        sectionListView.getItems().add(newSection);
        clearFields();
    }

    private Section createSectionFromFields() {
        String name = nameField.getText();
        String desc = descField.getText();
        Importance imp = importancePicker.getValue();
        String costRaw = costField.getText();

        if (name.isEmpty() || imp == null) {
            return null;
        }

        if (costRaw.isEmpty()) {
            return new Section(name, desc, imp);
        }

        try {
            double cost = Double.parseDouble(costRaw);
            return new Section(name, desc, cost, imp);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void clearFields() {
        nameField.clear();
        descField.clear();
        costField.clear();
    }
}
