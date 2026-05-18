package GUI;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginScene {

    public static VBox create(Stage stage){
        VBox root = new VBox();
        TextField id = new TextField();
        PasswordField pw = new PasswordField();
        Button loginBtn = new Button("Login");

        loginBtn.setOnAction(e -> {
            //로그인 성공 DAO 만들어서 넣기
            SceneManager.baseSize();
            SceneManager.showLobby();
        });

        root.getChildren().addAll(id,pw,loginBtn);
        return root;
    }
}
