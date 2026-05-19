package GUI;

import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import mafia.domain.Room;

/**
 * 방 입장 후의 대기실 화면 — 이번 sub-project에선 빈 자리.
 * 본격적인 구현은 별도 작업.
 */
public class WaitingRoomScene {
    public static VBox create(Room room) {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #faf8f4;");

        Label title = new Label("대기실: " + room.getTitle());
        title.setStyle("-fx-font-family: \"Copperplate\", serif;"
                     + "-fx-font-size: 28px; -fx-font-weight: bold;"
                     + "-fx-text-fill: #a83a3a;");

        Label hint = new Label("(WaitingRoom 화면은 다음 작업에서 구현)");
        hint.setStyle("-fx-font-size: 13px; -fx-text-fill: #857d70;");

        Hyperlink back = new Hyperlink("← 로비로 돌아가기");
        back.setStyle("-fx-text-fill: #857d70; -fx-font-size: 12px;");
        back.setOnAction(e -> SceneManager.showLobby());

        root.getChildren().addAll(title, hint, back);
        return root;
    }
}
