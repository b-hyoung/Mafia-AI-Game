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

    /** Scene에 디자인 토큰(tokens.css)을 가장 먼저 적용한다. 다른 css의 -color-* 변수 참조의 전제. */
    public static void applyTokens(Scene scene) {
        scene.getStylesheets().add(
            SceneManager.class.getResource("/css/tokens.css").toExternalForm()
        );
    }

    public static void showLogin(){
        Scene scene = new Scene(LoginScene.create(stage));
        applyTokens(scene);
        scene.getStylesheets().add(
            SceneManager.class.getResource("/css/login.css").toExternalForm()
        );
        stage.setScene(scene);
    }

    public static void showRegister(){
        Scene scene = new Scene(RegisterScene.create(stage));
        applyTokens(scene);
        scene.getStylesheets().add(
            SceneManager.class.getResource("/css/login.css").toExternalForm()
        );
        stage.setScene(scene);
    }

    public static void showLobby(){
        Scene scene = new Scene(LobbyScene.create(stage));
        applyTokens(scene);
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
        applyTokens(scene);
        stage.setScene(scene);
    }

    /**
     * 봇 5명 시뮬레이션 관전 화면.
     * 로그인 후 곧장 진입 (정식 게임 시작/방 만들기 흐름은 추후 작업).
     */
    public static void showSimulation(){
        Scene scene = new Scene(GUI.sim.SimulationScene.create(stage));
        applyTokens(scene);
        scene.getStylesheets().add(
            SceneManager.class.getResource("/css/simulation.css").toExternalForm()
        );
        stage.setScene(scene);
    }
}
