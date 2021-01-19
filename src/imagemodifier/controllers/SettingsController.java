package imagemodifier.controllers;

import imagemodifier.Main;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.*;
import java.util.Properties;

import static imagemodifier.Main.settingsFile;

public class SettingsController {
    @FXML
    private TextField threadsCountTextBox;
    @FXML
    private TextField threadsTimeoutTextBox;

    public void initialize() throws IOException {
        // load properties from the settings file
        threadsCountTextBox.setText(Integer.toString(getThreadsCount()));
        threadsTimeoutTextBox.setText(Integer.toString(getThreadsTimeout()));
    }

    public void openMainMenu(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(Main.class.getResource("views/MainMenu.fxml"));
        Scene scene = new Scene(root);
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();

        window.setScene(scene);
        window.show();
    }

    public void applySettings(MouseEvent event) throws IOException {
        Properties properties = new Properties();
        FileInputStream fileInput = new FileInputStream(settingsFile);
        FileOutputStream fileOutput = new FileOutputStream(settingsFile);

        properties.load(fileInput);
        properties.setProperty("thread-count", threadsCountTextBox.getText());
        properties.setProperty("thread-timeout", threadsTimeoutTextBox.getText());
        properties.store(fileOutput, null);
    }

    public static int getThreadsCount() throws IOException {
        Properties properties = new Properties();
        FileInputStream fileInput = new FileInputStream(settingsFile);

        properties.load(fileInput);

        return Integer.parseInt(properties.getProperty("thread-count"));
    }

    public static int getThreadsTimeout() throws IOException {
        Properties properties = new Properties();
        FileInputStream fileInput = new FileInputStream(settingsFile);

        properties.load(fileInput);

        return Integer.parseInt(properties.getProperty("thread-timeout"));
    }
}
