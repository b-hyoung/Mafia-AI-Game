package GUI;

import GUI.components.Logo;
import GUI.components.ResultBox;
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
import mafia.db.UserDAO;

import java.sql.SQLException;

public class RegisterScene {

    public static VBox create(Stage stage) {
        VBox root = new VBox();
        UserDAO instance = UserDAO.getInstance();
        root.getStyleClass().add("login-root");
        root.setAlignment(Pos.CENTER);
        // Tighter spacing/padding to fit one extra field in the same 520x600 window
        root.setSpacing(12);
        root.setStyle("-fx-padding: 32 32 32 32;");

        // Logo (shared component)
        Node logo = Logo.create(120);
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

        PasswordField pwConfirm = new PasswordField();
        pwConfirm.setPromptText("PASSWORD CONFIRM");
        pwConfirm.getStyleClass().add("login-field");
        pwConfirm.setMaxWidth(320);

        // Error label (hidden until validation fails)
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("login-error");
        errorLabel.setVisible(false);

        // Register button
        Button registerBtn = new Button("가입하기");
        registerBtn.getStyleClass().add("login-btn");
        registerBtn.setMaxWidth(320);
        registerBtn.setOnAction(e -> {
            String idText = id.getText() == null ? "" : id.getText().trim();
            String pwText = pw.getText() == null ? "" : pw.getText();
            String pwConfirmText = pwConfirm.getText() == null ? "" : pwConfirm.getText();

            if (idText.isEmpty() || pwText.isEmpty() || pwConfirmText.isEmpty()) {
                errorLabel.setText("모든 항목을 입력해주세요");
                errorLabel.setVisible(true);
                return;
            }
            if (!pwText.equals(pwConfirmText)) {
                errorLabel.setText("비밀번호가 일치하지 않습니다");
                errorLabel.setVisible(true);
                return;
            }

            // TODO: 진짜 DAO 붙으면 아래 임시 분기를 UserDao.register(...) 호출로 교체
            boolean ok = false;
            try {
                ok = instance.RegisterUser(idText, pwText);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            if (ok) {
                ResultBox.showSuccess(
                    stage,
                    "회원가입 성공",
                    "회원가입이 완료되었습니다",
                    () -> SceneManager.showLogin()
                );
            } else {
                ResultBox.showFail(
                    stage,
                    "회원가입 실패",
                    "이미 사용 중인 아이디입니다",
                    null
                );
            }
        });

        // Hide error message when the user edits any field
        id.textProperty().addListener((obs, oldVal, newVal) -> errorLabel.setVisible(false));
        pw.textProperty().addListener((obs, oldVal, newVal) -> errorLabel.setVisible(false));
        pwConfirm.textProperty().addListener((obs, oldVal, newVal) -> errorLabel.setVisible(false));

        // Back to login link
        Label prefix = new Label("이미 계정이 있으신가요?");
        prefix.setStyle("-fx-text-fill: #857d70; -fx-font-size: 12px;");

        Hyperlink backLink = new Hyperlink("로그인");
        backLink.getStyleClass().add("login-link");
        backLink.setOnAction(e -> SceneManager.showLogin());

        HBox linkRow = new HBox(prefix, backLink);
        linkRow.getStyleClass().add("login-link-row");
        linkRow.setAlignment(Pos.CENTER);

        root.getChildren().addAll(title, id, pw, pwConfirm, errorLabel, registerBtn, linkRow);

        return root;
    }
}
