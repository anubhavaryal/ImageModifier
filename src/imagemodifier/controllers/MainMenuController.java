package imagemodifier.controllers;

import imagemodifier.Main;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class MainMenuController {
    public void openModifyImage(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(Main.class.getResource("views/ModifyImage.fxml"));
        Scene scene = new Scene(root);
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();

        window.setResizable(false);
        window.setScene(scene);
        window.show();
    }

    public void openSettings(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(Main.class.getResource("views/Settings.fxml"));
        Scene scene = new Scene(root);
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();

        window.setScene(scene);
        window.show();
    }
}
