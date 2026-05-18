package GUI;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginScene {

    public static VBox create(Stage stage) {
        VBox root = new VBox();
        root.getStyleClass().add("login-root");
        root.setAlignment(Pos.CENTER);

        // Logo (delegated to SceneLogo helper — same instance shape used by RegisterScene)
        Node logo = SceneLogo.create(120);
        if (logo != null) {
            root.getChildren().add(logo);
        }

        // Title
        Label title = new Label("Mafia for Java");
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

        // Sub-links (signup / find password) — UI only, no behavior yet
        Hyperlink signupLink = new Hyperlink("회원가입");
        signupLink.getStyleClass().add("login-link");
        signupLink.setOnAction(e -> {
            // TODO: 회원가입 화면 연결
        });

        Label sep = new Label("|");
        sep.getStyleClass().add("login-link-sep");

        Hyperlink findLink = new Hyperlink("비밀번호 찾기");
        findLink.getStyleClass().add("login-link");
        findLink.setOnAction(e -> {
            // TODO: 비밀번호 찾기 흐름
        });

        HBox linkRow = new HBox(signupLink, sep, findLink);
        linkRow.getStyleClass().add("login-link-row");
        linkRow.setAlignment(Pos.CENTER);

        root.getChildren().addAll(title, id, pw, errorLabel, loginBtn, linkRow);

        return root;
    }
}
