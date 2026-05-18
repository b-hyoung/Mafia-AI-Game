package GUI;

import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

    public static Stage stage;

    public static void init(Stage s){stage = s;}

    public static void baseSize(){
        stage.setMinWidth(1280);
        stage.setMinHeight(720);
        stage.setWidth(1280);
        stage.setHeight(720);
    }

    public static void LoginSize(){
        stage.setMinWidth(520);
        stage.setMinHeight(600);
        stage.setWidth(520);
        stage.setHeight(600);
    }

    public static void showLogin(){
        Scene scene = new Scene(LoginScene.create(stage));
        scene.getStylesheets().add(
            SceneManager.class.getResource("/css/login.css").toExternalForm()
        );
        stage.setScene(scene);
    }

    public static void showRegister(){
        Scene scene = new Scene(RegisterScene.create(stage));
        scene.getStylesheets().add(
            SceneManager.class.getResource("/css/login.css").toExternalForm()
        );
        stage.setScene(scene);
    }

    public static void showLobby(){
        stage.setScene(new Scene(LobbyScene.create(stage)));
    }
}
