package GUI.components;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * 가입/로그인 결과 등 원격 응답을 표시하는 공용 모달 박스.
 * success/fail 두 모드 — 박스 구조는 동일하고 CSS 클래스로만 색을 분기한다.
 */
public class ResultBox {

    public enum Type { SUCCESS, FAIL }

    public static void showSuccess(Window owner, String title, String message, Runnable onClose) {
        show(owner, Type.SUCCESS, title, message, onClose);
    }

    public static void showFail(Window owner, String title, String message, Runnable onClose) {
        show(owner, Type.FAIL, title, message, onClose);
    }

    private static void show(Window owner, Type type, String title, String message, Runnable onClose) {
        VBox root = new VBox();
        root.getStyleClass().add("result-box");
        root.getStyleClass().add(type == Type.SUCCESS ? "result-box-success" : "result-box-fail");
        root.setAlignment(Pos.CENTER);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("result-box-title");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("result-box-message");
        messageLabel.setWrapText(true);

        Button okBtn = new Button("확인");
        okBtn.getStyleClass().add("result-box-btn");

        root.getChildren().addAll(titleLabel, messageLabel, okBtn);

        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        Scene scene = new Scene(root, 360, 180);
        scene.getStylesheets().add(
            ResultBox.class.getResource("/css/tokens.css").toExternalForm()
        );
        scene.getStylesheets().add(
            ResultBox.class.getResource("/css/result-box.css").toExternalForm()
        );

        Runnable close = () -> {
            dialog.close();
            if (onClose != null) onClose.run();
        };

        okBtn.setOnAction(e -> close.run());
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) close.run();
        });

        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
