/** 
 * @author: Saadat Emilbekova
 * @gmail: saadat.universe@gmail.com
 * @date: 2026-05-14
 */

package com.budgetapp;

import java.io.IOException;

import javafx.fxml.FXML;

public class SecondaryController {

    @FXML
    private void switchToPrimary() throws IOException {
        App.setRoot("primary");
    }
}