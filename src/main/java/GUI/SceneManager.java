package GUI;

import javafx.scene.Scene;
import javafx.stage.Stage;
import mafia.domain.Room;

public class SceneManager {

    public static Stage stage;
    public static String currentNickname; // 로그인 성공 시 LoginScene이 설정

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
        Scene scene = new Scene(LobbyScene.create(stage));
        scene.getStylesheets().add(
            SceneManager.class.getResource("/css/login.css").toExternalForm()
        );
        scene.getStylesheets().add(
            SceneManager.class.getResource("/css/lobby.css").toExternalForm()
        );
        stage.setScene(scene);
    }

    public static void showWaitingRoom(Room room){
        Scene scene = new Scene(WaitingRoomScene.create(room));
        stage.setScene(scene);
    }
}
