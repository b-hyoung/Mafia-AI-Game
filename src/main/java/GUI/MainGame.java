package GUI;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainGame extends Application {

    @Override
    public void start(Stage stage) {

        SceneManager.init(stage);
        SceneManager.showLogin();
        SceneManager.LoginSize();

        stage.setTitle("Mafia for Java");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
