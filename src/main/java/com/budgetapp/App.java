/** 
 * @author: Saadat Emilbekova
 * @gmail: saadat.universe@gmail.com
 * @date: 2026-05-14
 */



package com.budgetapp;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;

    
    @Override
    public void start(Stage stage) throws IOException {
    // Загружаем шрифт из локальной папки проекта
    java.net.URL fontResource = App.class.getResource("/com/budgetapp/BitcountPropSingle-Regular.ttf");
    
    if (fontResource != null) {
    // Attempt to load and register the TrueType font into the JavaFX system
    javafx.scene.text.Font loadedFont = javafx.scene.text.Font.loadFont(fontResource.toExternalForm(), 16);
    
        if (loadedFont != null) {
            System.out.println("Font loaded successfully from local resources: " + loadedFont.getName());
        } else {
            System.out.println("Error reading the font file. System default typography applied.");
        }
    } else {
        System.out.println("Font asset file could not be found in the resources/com/budgetapp/ folder.");
    }


    scene = new Scene(loadFXML("primary"), 740, 520);
    stage.setScene(scene);
    stage.show();
    }


    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }

}