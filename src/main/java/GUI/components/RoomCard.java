package GUI.components;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import mafia.domain.Room;
import mafia.domain.RoomState;

import java.net.URL;

/**
 * 단일 방 카드 컴포넌트.
 * Room을 받아 VBox(card) 노드를 만든다. 인원수/상태는 Property에 바인딩되어 자동 갱신.
 * 진행 중인 방(IN_GAME)은 흐릿하게 + 클릭 비활성.
 */
public class RoomCard {

    private static final double ICON_SIZE = 64;
    private static final double CARD_WIDTH = 180;
    private static final double CARD_HEIGHT = 220;

    /**
     * @param room    이 카드가 표현할 방
     * @param onClick 카드 클릭 시 호출 (대기 상태일 때만)
     * @param onDouble 더블클릭 시 호출 (대기 상태일 때만)
     */
    public static VBox create(Room room, Runnable onClick, Runnable onDouble) {
        VBox card = new VBox();
        card.getStyleClass().add("room-card");
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        card.setMaxSize(CARD_WIDTH, CARD_HEIGHT);

        // 캐릭터 아이콘 (logo.png 정적, 원형 클립)
        URL imageUrl = RoomCard.class.getResource("/images/logo.png");
        if (imageUrl != null) {
            ImageView icon = new ImageView(new Image(imageUrl.toExternalForm()));
            icon.setFitWidth(ICON_SIZE);
            icon.setFitHeight(ICON_SIZE);
            icon.setPreserveRatio(true);
            icon.setClip(new Circle(ICON_SIZE / 2, ICON_SIZE / 2, ICON_SIZE / 2));
            card.getChildren().add(icon);
        }

        // 제목
        Label title = new Label(room.getTitle());
        title.getStyleClass().add("room-card-title");
        title.setMaxWidth(CARD_WIDTH - 16);

        // 호스트
        Label host = new Label("👤 " + room.getHostNickname());
        host.getStyleClass().add("room-card-host");

        // 인원수 + 상태 (자동 바인딩)
        Label status = new Label();
        status.getStyleClass().add("room-card-status");
        status.textProperty().bind(Bindings.createStringBinding(
            () -> room.getCurrentPlayers() + "/" + room.getMaxPlayers()
                  + " · " + room.getState().getLabel(),
            room.currentPlayersProperty(), room.stateProperty()
        ));

        card.getChildren().addAll(title, host, status);

        // 상태에 따라 disabled 클래스 자동 토글
        applyDisabledIfNeeded(card, room);
        room.stateProperty().addListener((obs, oldVal, newVal) -> applyDisabledIfNeeded(card, room));

        // 클릭 핸들러 (대기 상태일 때만)
        card.setOnMouseClicked(e -> {
            if (room.getState() != RoomState.WAITING) return;
            if (e.getClickCount() == 2) {
                onDouble.run();
            } else {
                onClick.run();
            }
        });

        return card;
    }

    private static void applyDisabledIfNeeded(VBox card, Room room) {
        boolean disabled = room.getState() != RoomState.WAITING;
        if (disabled) {
            if (!card.getStyleClass().contains("room-card-disabled")) {
                card.getStyleClass().add("room-card-disabled");
            }
        } else {
            card.getStyleClass().remove("room-card-disabled");
        }
    }

    /** 외부에서 선택/해제 토글 */
    public static void setSelected(VBox card, boolean selected) {
        if (selected) {
            if (!card.getStyleClass().contains("room-card-selected")) {
                card.getStyleClass().add("room-card-selected");
            }
        } else {
            card.getStyleClass().remove("room-card-selected");
        }
    }
}
