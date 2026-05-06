package com.budgetapp;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

public class PrimaryController {
    @FXML private TextField nameField, descField, costField;
    @FXML private ComboBox<Importance> importancePicker;
    @FXML private ComboBox<Section> parentPicker;
    
    // 1. Changed from ListView to TreeView
    @FXML private TreeView<Section> sectionListView; 

    private Section rootSection = new Section("Root", "Master", Importance.HIGH);

    
    @FXML
    public void initialize() {
        importancePicker.getItems().setAll(Importance.values());
        
        parentPicker.setConverter(new javafx.util.StringConverter<Section>() {
        @Override
        public String toString(Section section) {
            return (section == null) ? "" : section.getName(); 
        }
        @Override
        public Section fromString(String string) {
            return null; 
        }
    });


        // 2. Set up the Tree structure
        TreeItem<Section> treeRoot = new TreeItem<>(rootSection);
        sectionListView.setRoot(treeRoot);
        sectionListView.setShowRoot(false); // Keeps it looking clean

        // 3. This makes it look "Normal" (Name - Total)
        sectionListView.setCellFactory(tv -> new TreeCell<Section>() {
            @Override
            protected void updateItem(Section item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " - Total: $" + item.getGrandTotal() + "  [Base: $" + item.getbase_cost() + "]");
                }
            }
        });
    }

    @FXML
    private void handleAddSection() {
        
        Section newSection = createSectionFromFields();
        if (newSection == null) return;

        rootSection.addSubsection(newSection);
        
        // Add to the visual tree
        sectionListView.getRoot().getChildren().add(new TreeItem<>(newSection));
        
        // Update the dropdown so you can add subsections to this new section
        parentPicker.getItems().add(newSection); 
        clearFields();
    }

    @FXML
    private void handleAddSubsection() {
        TreeItem<Section> selectedTreeItem = sectionListView.getSelectionModel().getSelectedItem();
        
        if (selectedTreeItem == null) {
            System.out.println("Please select a parent in the tree first!");
        return;
    }
        Section newSection = createSectionFromFields();
        if (newSection == null) return;

        selectedTreeItem.getValue().addSubsection(newSection);
        
        // Find the parent in the tree and add the child visually
        selectedTreeItem.getChildren().add(new TreeItem<>(newSection));
        selectedTreeItem.setExpanded(true);
        
        parentPicker.getItems().add(newSection); 
        clearFields();
    }

    private Section createSectionFromFields() {
        String name = nameField.getText();
        String desc = descField.getText();
        Importance imp = importancePicker.getValue();
        String costRaw = costField.getText();
        if (name.isEmpty() || imp == null) return null;
        try {
            double cost = costRaw.isEmpty() ? 0 : Double.parseDouble(costRaw);
            return new Section(name, desc, cost, imp);
        } catch (NumberFormatException e) { return null; }
    }

    private void clearFields() {
        nameField.clear();
        descField.clear();
        costField.clear();
    }
}
