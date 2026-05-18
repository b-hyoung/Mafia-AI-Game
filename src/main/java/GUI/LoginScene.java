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

        // Error label (hidden until validation fails)
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("login-error");
        errorLabel.setVisible(false);

        // Login button
        Button loginBtn = new Button("LOGIN");
        loginBtn.getStyleClass().add("login-btn");
        loginBtn.setMaxWidth(320);

        loginBtn.setOnAction(e -> {
            String idText = id.getText() == null ? "" : id.getText().trim();
            String pwText = pw.getText() == null ? "" : pw.getText();

            if (idText.isEmpty() || pwText.isEmpty()) {
                errorLabel.setText("아이디와 비밀번호를 입력해주세요");
                errorLabel.setVisible(true);
                return;
            }

            // TODO: DAO로 실제 검증. 실패하면:
            //   errorLabel.setText("아이디 또는 비밀번호가 일치하지 않습니다");
            //   errorLabel.setVisible(true);
            //   return;

            SceneManager.baseSize();
            SceneManager.showLobby();
        });

        // Hide error message as soon as the user starts editing either field
        id.textProperty().addListener((obs, oldVal, newVal) -> errorLabel.setVisible(false));
        pw.textProperty().addListener((obs, oldVal, newVal) -> errorLabel.setVisible(false));

        root.getChildren().addAll(title, id, pw, errorLabel, loginBtn);

        return root;
    }
}
