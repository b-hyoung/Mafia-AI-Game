package GUI;

import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

    public static Stage stage;

    public static void init(Stage s){stage = s;}
    public static void baseSize(){
        stage.setMinWidth(1280);
        stage.setMinHeight(720);
    }
    public static void LoginSize(){
        stage.setMinWidth(510);
        stage.setMinHeight(370);
    }
    public static void showLogin(){
        stage.setScene(new Scene(LoginScene.create(stage)));
    }
    public static void showLobby(){
        stage.setScene(new Scene(LobbyScene.create(stage)));
    }
}
