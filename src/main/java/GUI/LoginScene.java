package GUI;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;

public class LoginScene {

    public static VBox create(Stage stage) {
        VBox root = new VBox();
        root.getStyleClass().add("login-root");
        root.setAlignment(Pos.CENTER);

        // Logo (optional — skipped if image file is missing)
        URL logoUrl = LoginScene.class.getResource("/images/logo.png");
        if (logoUrl != null) {
            ImageView logo = new ImageView(new Image(logoUrl.toExternalForm()));
            logo.setFitWidth(120);
            logo.setFitHeight(120);
            logo.setPreserveRatio(true);
            root.getChildren().add(logo);
        }

        // Title
        Label title = new Label("MAFIA  FOR  JAVA");
        title.getStyleClass().add("login-title");

        // Input fields
        TextField id = new TextField();
        id.setPromptText("ID");
        id.getStyleClass().add("login-field");
        id.setMaxWidth(320);

        PasswordField pw = new PasswordField();
        pw.setPromptText("PASSWORD");
        pw.getStyleClass().add("login-field");
        pw.setMaxWidth(320);

        // Login button
        Button loginBtn = new Button("LOGIN");
        loginBtn.getStyleClass().add("login-btn");
        loginBtn.setMaxWidth(320);

        loginBtn.setOnAction(e -> {
            // TODO: DAO로 실제 로그인 검증
            SceneManager.baseSize();
            SceneManager.showLobby();
        });

        root.getChildren().addAll(title, id, pw, loginBtn);

        return root;
    }
}
