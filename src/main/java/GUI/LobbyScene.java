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
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.util.Duration;
import mafia.domain.Room;
import mafia.domain.RoomState;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class LobbyScene {

    public static BorderPane create(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("lobby-root");

        // ===== 상태 =====
        ObservableList<Room> rooms = createDummyRooms();
        Map<Integer, VBox> cardByRoomId = new HashMap<>();
        final Room[] selectedRoom = new Room[]{null};
        ObservableList<String> onlineUsers = FXCollections.observableArrayList();
        String nickname = SceneManager.currentNickname != null
                          ? SceneManager.currentNickname : "guest";
        onlineUsers.add(nickname);

        // ===== 상단 바 =====
        Label gameTitle = new Label("Mafia for Java");
        gameTitle.getStyleClass().add("lobby-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label welcome = new Label("환영합니다, " + nickname);
        welcome.getStyleClass().add("lobby-welcome");

        HBox topBar = new HBox(gameTitle, spacer, welcome);
        topBar.getStyleClass().add("lobby-topbar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        // ===== 액션 바: 빠른 입장 + 새로고침 =====
        Button quickJoin = new Button("⚡ 빠른 입장");
        quickJoin.getStyleClass().add("lobby-action-btn");

        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, javafx.scene.layout.Priority.ALWAYS);

        Button refresh = new Button("🔄 새로고침");
        refresh.getStyleClass().add("lobby-action-btn");

        HBox actionBar = new HBox(quickJoin, actionSpacer, refresh);
        actionBar.getStyleClass().add("lobby-action-bar");

        // ===== 입장 버튼 (하단) =====
        Button enterBtn = new Button("입장하기");
        enterBtn.getStyleClass().add("lobby-enter-btn");
        enterBtn.setDisable(true);

        HBox bottomBar = new HBox(enterBtn);
        bottomBar.getStyleClass().add("lobby-bottom-bar");
        bottomBar.setAlignment(Pos.CENTER);

        // ===== 카드 그리드 =====
        FlowPane roomGrid = new FlowPane();
        roomGrid.getStyleClass().add("room-grid");

        Runnable enterSelected = () -> {
            if (selectedRoom[0] == null) return;
            if (selectedRoom[0].getState() != RoomState.WAITING) return;
            SceneManager.showWaitingRoom(selectedRoom[0]);
        };

        for (Room r : rooms) {
            VBox card = RoomCard.create(r,
                () -> {
                    // 단일 클릭: 선택
                    if (selectedRoom[0] != null) {
                        VBox prev = cardByRoomId.get(selectedRoom[0].getRoomId());
                        if (prev != null) RoomCard.setSelected(prev, false);
                    }
                    selectedRoom[0] = r;
                    RoomCard.setSelected(card(cardByRoomId, r), true);
                    enterBtn.setDisable(false);
                },
                () -> {
                    // 더블 클릭: 즉시 입장
                    SceneManager.showWaitingRoom(r);
                }
            );
            cardByRoomId.put(r.getRoomId(), card);
            roomGrid.getChildren().add(card);
        }

        ScrollPane scroll = new ScrollPane(roomGrid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // ===== 사이드바: 접속 중 유저 =====
        Label sideHeader = new Label();
        sideHeader.getStyleClass().add("online-sidebar-header");
        sideHeader.textProperty().bind(Bindings.createStringBinding(
            () -> "접속 중 (" + onlineUsers.size() + ")",
            onlineUsers
        ));

        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("online-sidebar");
        sidebar.getChildren().add(sideHeader);

        Runnable rebuildUsers = () -> {
            // 사이드바 본문 재구성 (header는 0번 인덱스에 유지)
            sidebar.getChildren().retainAll(sideHeader);
            for (String u : onlineUsers) {
                Label row = new Label("👤 " + u + (u.equals(nickname) ? "  (나)" : ""));
                row.getStyleClass().add("online-user-row");
                if (u.equals(nickname)) {
                    row.getStyleClass().add("online-user-me");
                }
                sidebar.getChildren().add(row);
            }
        };
        rebuildUsers.run();
        onlineUsers.addListener((javafx.collections.ListChangeListener<String>) c -> rebuildUsers.run());

        // ===== 중앙 = 카드 그리드 + 사이드바 =====
        BorderPane center = new BorderPane();
        center.setCenter(scroll);
        center.setRight(sidebar);

        // ===== 조립 =====
        VBox topGroup = new VBox(topBar, actionBar);
        root.setTop(topGroup);
        root.setCenter(center);
        root.setBottom(bottomBar);

        // ===== 동작 핸들러 =====
        enterBtn.setOnAction(e -> enterSelected.run());

        Random rng = new Random();
        quickJoin.setOnAction(e -> {
            // WAITING + 인원 안 찬 방 중 임의 선택
            java.util.List<Room> candidates = new java.util.ArrayList<>();
            for (Room r : rooms) {
                if (r.getState() == RoomState.WAITING
                    && r.getCurrentPlayers() < r.getMaxPlayers()) {
                    candidates.add(r);
                }
            }
            if (candidates.isEmpty()) {
                GUI.components.ResultBox.showFail(
                    stage, "입장 불가",
                    "입장 가능한 방이 없습니다", null);
                return;
            }
            Room target = candidates.get(rng.nextInt(candidates.size()));
            SceneManager.showWaitingRoom(target);
        });

        // ===== Polling (3초 주기) =====
        Timeline poller = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            if (rooms.isEmpty()) return;
            Room target = rooms.get(rng.nextInt(rooms.size()));
            int delta = rng.nextBoolean() ? +1 : -1;
            int next = Math.max(0, Math.min(target.getMaxPlayers(),
                                            target.getCurrentPlayers() + delta));
            target.setCurrentPlayers(next);
            if (rng.nextInt(10) == 0) {
                target.setState(target.getState() == RoomState.WAITING
                                ? RoomState.IN_GAME : RoomState.WAITING);
            }
        }));
        poller.setCycleCount(Timeline.INDEFINITE);
        poller.play();

        // Scene 전환 시 자동 정리 (메모리 leak 방지)
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) poller.stop();
        });

        refresh.setOnAction(e -> poller.playFromStart());

        return root;
    }

    /** 헬퍼: cardByRoomId에서 안전하게 가져옴 */
    private static VBox card(Map<Integer, VBox> map, Room r) {
        return map.get(r.getRoomId());
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
