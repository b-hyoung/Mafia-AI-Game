package GUI;

import GUI.components.RoomCard;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import mafia.domain.Room;
import mafia.domain.RoomState;

public class LobbyScene {

    public static BorderPane create(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("lobby-root");

        // ---------- 상단 바 ----------
        Label gameTitle = new Label("Mafia for Java");
        gameTitle.getStyleClass().add("lobby-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        String nickname = SceneManager.currentNickname != null
                          ? SceneManager.currentNickname : "guest";
        Label welcome = new Label("환영합니다, " + nickname);
        welcome.getStyleClass().add("lobby-welcome");

        HBox topBar = new HBox(gameTitle, spacer, welcome);
        topBar.getStyleClass().add("lobby-topbar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        // ---------- 액션 바 (이후 태스크에서 빠른 입장/새로고침 추가) ----------
        HBox actionBar = new HBox();
        actionBar.getStyleClass().add("lobby-action-bar");

        // ---------- 카드 그리드 ----------
        ObservableList<Room> rooms = createDummyRooms();
        FlowPane roomGrid = new FlowPane();
        roomGrid.getStyleClass().add("room-grid");
        for (Room r : rooms) {
            roomGrid.getChildren().add(RoomCard.create(r, () -> {}, () -> {}));
        }
        ScrollPane scroll = new ScrollPane(roomGrid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // ---------- 하단 (이후 태스크에서 입장 버튼 추가) ----------
        HBox bottomBar = new HBox();
        bottomBar.getStyleClass().add("lobby-bottom-bar");

        // ---------- 조립 ----------
        VBox topGroup = new VBox(topBar, actionBar);
        root.setTop(topGroup);
        root.setCenter(scroll);
        root.setBottom(bottomBar);

        return root;
    }

    private static ObservableList<Room> createDummyRooms() {
        return FXCollections.observableArrayList(
            new Room(1, "고수만 들어와", "alice",   4, 6, RoomState.WAITING),
            new Room(2, "아무나 콜",     "bob",     2, 6, RoomState.WAITING),
            new Room(3, "조용한 밤에",   "charlie", 6, 6, RoomState.IN_GAME),
            new Room(4, "빠른판",        "dan",     3, 6, RoomState.WAITING),
            new Room(5, "초보환영",      "eve",     1, 6, RoomState.WAITING),
            new Room(6, "꿀잼각",        "frank",   5, 6, RoomState.WAITING),
            new Room(7, "심야 대전",     "grace",   4, 6, RoomState.IN_GAME)
        );
    }
}
