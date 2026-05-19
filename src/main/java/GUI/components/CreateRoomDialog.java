package GUI.components;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * 방 만들기 모달 다이얼로그. 제목 + 최대인원(4/6/8) + 만들기/취소.
 * 만들기 클릭 → onCreate.accept(title, maxPlayers). 빈 제목이면 인라인 에러.
 */
public class CreateRoomDialog {

    /**
     * @param owner 부모 창
     * @param onCreate (title, maxPlayers) 콜백. 다이얼로그 닫힌 후 호출. 취소면 호출 안 됨.
     */
    public static void show(Window owner, BiConsumer<String, Integer> onCreate) {
        Objects.requireNonNull(onCreate, "onCreate");

        VBox root = new VBox();
        root.getStyleClass().add("result-box");
        root.getStyleClass().add("result-box-success");
        root.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("방 만들기");
        titleLabel.getStyleClass().add("result-box-title");

        TextField titleField = new TextField();
        titleField.setPromptText("방 제목");
        titleField.setMaxWidth(280);
        titleField.getStyleClass().add("login-field");

        Label maxLabel = new Label("최대 인원");
        maxLabel.getStyleClass().add("result-box-message");

        ChoiceBox<Integer> maxChoice = new ChoiceBox<>();
        maxChoice.getItems().addAll(4, 6, 8);
        maxChoice.setValue(6);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #c4554d; -fx-font-size: 12px;");
        errorLabel.setVisible(false);

        Button createBtn = new Button("만들기");
        createBtn.getStyleClass().add("result-box-btn");

        Button cancelBtn = new Button("취소");
        cancelBtn.getStyleClass().add("result-box-btn");

        HBox buttons = new HBox(10, createBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER);

        root.getChildren().addAll(titleLabel, titleField, maxLabel, maxChoice, errorLabel, buttons);

        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        Scene scene = new Scene(root, 360, 280);
        scene.getStylesheets().add(
            CreateRoomDialog.class.getResource("/css/tokens.css").toExternalForm()
        );
        scene.getStylesheets().add(
            CreateRoomDialog.class.getResource("/css/result-box.css").toExternalForm()
        );
        scene.getStylesheets().add(
            CreateRoomDialog.class.getResource("/css/login.css").toExternalForm()
        );

        Runnable doCreate = () -> {
            String title = titleField.getText() == null ? "" : titleField.getText().trim();
            if (title.isEmpty()) {
                errorLabel.setText("제목을 입력해주세요");
                errorLabel.setVisible(true);
                return;
            }
            int max = maxChoice.getValue() != null ? maxChoice.getValue() : 6;
            dialog.close();
            onCreate.accept(title, max);
        };

        titleField.textProperty().addListener((obs, oldVal, newVal) -> errorLabel.setVisible(false));

        createBtn.setOnAction(e -> doCreate.run());
        cancelBtn.setOnAction(e -> dialog.close());

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                dialog.close();
            } else if (e.getCode() == KeyCode.ENTER) {
                doCreate.run();
            }
        });

        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
