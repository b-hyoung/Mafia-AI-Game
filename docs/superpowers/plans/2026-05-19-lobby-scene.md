# Lobby Scene Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 로그인 후 진입하는 로비 화면을 구현한다. 카드 그리드로 방을 보여주고, 빠른 입장/방 선택+입장 두 가지 방법을 제공하며, 우측 사이드바에 접속 유저(현재는 본인만), 인원수는 3초 주기 polling으로 자동 갱신된다.

**Architecture:** 도메인(`Room`, `RoomState`) → 컴포넌트(`RoomCard`) → 화면(`LobbyScene`) 의존 순으로 구축. UI는 `ObservableList<Room>` 패턴으로 만들어 미래 sub-project 2(TCP 서버)에서 데이터 소스만 교체하면 그대로 재사용 가능하도록. 입장 후의 `WaitingRoomScene`은 빈 스켈레톤만.

**Tech Stack:** Java 21 + JavaFX 21 (controls + media — 이미 의존성 있음). 외부 라이브러리 추가 없음.

**Testing Approach:** JavaFX UI는 자동 단위 테스트 인프라가 없으므로 **수동 시각 검증**. 각 태스크는 코드 작성 + 자체 점검 후 커밋. 마지막 Task 9에서 IDE 실행으로 흐름 전체 확인.

**Spec Reference:** `docs/superpowers/specs/2026-05-19-lobby-scene-design.md`

---

## File Structure

| 경로 | 상태 | 책임 |
|---|---|---|
| `src/main/java/mafia/domain/Room.java` | 신규 | 방 도메인 모델 (`IntegerProperty currentPlayers` 포함) |
| `src/main/java/mafia/domain/RoomState.java` | 신규 | enum `WAITING` / `IN_GAME` + 라벨 |
| `src/main/resources/css/lobby.css` | 신규 | 로비/카드/사이드바 전용 스타일 |
| `src/main/java/GUI/components/RoomCard.java` | 신규 | 단일 카드 컴포넌트 (Room → Node 변환) |
| `src/main/java/GUI/WaitingRoomScene.java` | 신규 | 빈 스켈레톤 (방 제목 + 안내 텍스트) |
| `src/main/java/GUI/SceneManager.java` | 수정 | `currentNickname` 필드 + `showLobby`에 lobby.css + `showWaitingRoom(Room)` |
| `src/main/java/GUI/LoginScene.java` | 수정 | 로그인 성공 시 `SceneManager.currentNickname = idText;` 한 줄 |
| `src/main/java/GUI/LobbyScene.java` | 수정 (전면 재작성) | 로비 화면 전체 |

각 파일이 하나의 책임만 가지도록 분리: 도메인(데이터 모양), 컴포넌트(카드 한 개), Scene(화면 조립), Manager(전환).

---

## Task 1: Room + RoomState 도메인

**Files:**
- Create: `src/main/java/mafia/domain/Room.java`
- Create: `src/main/java/mafia/domain/RoomState.java`

- [ ] **Step 1: `mafia/domain/RoomState.java` 작성**

```java
package mafia.domain;

public enum RoomState {
    WAITING("대기"),
    IN_GAME("진행중");

    private final String label;

    RoomState(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
```

- [ ] **Step 2: `mafia/domain/Room.java` 작성**

```java
package mafia.domain;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

public class Room {
    private final int roomId;
    private final String title;
    private final String hostNickname;
    private final int maxPlayers;
    private final IntegerProperty currentPlayers;
    private final ObjectProperty<RoomState> state;

    public Room(int roomId, String title, String hostNickname,
                int currentPlayers, int maxPlayers, RoomState state) {
        this.roomId = roomId;
        this.title = title;
        this.hostNickname = hostNickname;
        this.maxPlayers = maxPlayers;
        this.currentPlayers = new SimpleIntegerProperty(currentPlayers);
        this.state = new SimpleObjectProperty<>(state);
    }

    public int getRoomId() { return roomId; }
    public String getTitle() { return title; }
    public String getHostNickname() { return hostNickname; }
    public int getMaxPlayers() { return maxPlayers; }

    public int getCurrentPlayers() { return currentPlayers.get(); }
    public void setCurrentPlayers(int v) { currentPlayers.set(v); }
    public IntegerProperty currentPlayersProperty() { return currentPlayers; }

    public RoomState getState() { return state.get(); }
    public void setState(RoomState s) { state.set(s); }
    public ObjectProperty<RoomState> stateProperty() { return state; }
}
```

- [ ] **Step 3: 커밋**

```bash
git add src/main/java/mafia/domain/Room.java src/main/java/mafia/domain/RoomState.java
git commit -m "Domain: Room/RoomState 추가 (currentPlayers/state는 JavaFX Property)"
```

---

## Task 2: lobby.css 작성

**Files:**
- Create: `src/main/resources/css/lobby.css`

- [ ] **Step 1: `src/main/resources/css/lobby.css` 작성**

```css
/* === Mafia for Java — Lobby Scene ===
   login.css의 컬러 톤(크림 옵화이트 + 와인레드)을 그대로 따른다.
*/

.lobby-root {
    -fx-background-color: #faf8f4;
}

.lobby-topbar {
    -fx-padding: 14 24 14 24;
    -fx-border-color: #e5e1d6;
    -fx-border-width: 0 0 1 0;
}

.lobby-title {
    -fx-font-family: "Copperplate", "Apple SD Gothic Neo", serif;
    -fx-font-size: 20px;
    -fx-font-weight: bold;
    -fx-text-fill: #a83a3a;
}

.lobby-welcome {
    -fx-font-family: "Helvetica Neue", "Apple SD Gothic Neo", sans-serif;
    -fx-font-size: 13px;
    -fx-text-fill: #3b2a1a;
}

.lobby-action-bar {
    -fx-padding: 12 24 12 24;
    -fx-spacing: 12;
    -fx-alignment: center-left;
}

.lobby-action-btn {
    -fx-font-family: "Helvetica Neue", "Apple SD Gothic Neo", sans-serif;
    -fx-background-color: transparent;
    -fx-text-fill: #a83a3a;
    -fx-border-color: #a83a3a;
    -fx-border-width: 1;
    -fx-padding: 6 16 6 16;
    -fx-font-size: 13px;
    -fx-font-weight: bold;
    -fx-cursor: hand;
    -fx-background-radius: 8;
    -fx-border-radius: 8;
}

.lobby-action-btn:hover {
    -fx-background-color: #a83a3a;
    -fx-text-fill: #faf8f4;
}

.room-grid {
    -fx-padding: 16;
    -fx-hgap: 16;
    -fx-vgap: 16;
}

.room-card {
    -fx-background-color: #ffffff;
    -fx-border-color: #e5e1d6;
    -fx-border-width: 1;
    -fx-background-radius: 12;
    -fx-border-radius: 12;
    -fx-padding: 16;
    -fx-spacing: 8;
    -fx-alignment: center;
    -fx-cursor: hand;
}

.room-card:hover {
    -fx-border-color: #a83a3a;
}

.room-card-selected {
    -fx-border-color: #a83a3a;
    -fx-border-width: 2;
    -fx-background-color: #faf2ec;
}

.room-card-disabled {
    -fx-opacity: 0.5;
    -fx-cursor: default;
}

.room-card-title {
    -fx-font-family: "Copperplate", "Apple SD Gothic Neo", serif;
    -fx-font-size: 16px;
    -fx-font-weight: bold;
    -fx-text-fill: #a83a3a;
}

.room-card-host {
    -fx-font-size: 12px;
    -fx-text-fill: #857d70;
}

.room-card-status {
    -fx-font-size: 12px;
    -fx-text-fill: #3b2a1a;
}

.online-sidebar {
    -fx-background-color: #f5f0e6;
    -fx-border-color: #e5e1d6;
    -fx-border-width: 0 0 0 1;
    -fx-padding: 16;
    -fx-spacing: 8;
    -fx-pref-width: 250;
}

.online-sidebar-header {
    -fx-font-family: "Copperplate", "Apple SD Gothic Neo", serif;
    -fx-font-size: 14px;
    -fx-font-weight: bold;
    -fx-text-fill: #3b2a1a;
}

.online-user-row {
    -fx-font-size: 13px;
    -fx-text-fill: #3b2a1a;
    -fx-padding: 4 0 4 0;
}

.online-user-me {
    -fx-text-fill: #a83a3a;
    -fx-font-weight: bold;
}

.lobby-enter-btn {
    -fx-font-family: "Helvetica Neue", "Apple SD Gothic Neo", sans-serif;
    -fx-background-color: transparent;
    -fx-text-fill: #a83a3a;
    -fx-border-color: #a83a3a;
    -fx-border-width: 1;
    -fx-pref-width: 320;
    -fx-pref-height: 42;
    -fx-font-size: 14px;
    -fx-font-weight: bold;
    -fx-cursor: hand;
    -fx-background-radius: 8;
    -fx-border-radius: 8;
}

.lobby-enter-btn:hover {
    -fx-background-color: #a83a3a;
    -fx-text-fill: #faf8f4;
}

.lobby-enter-btn:disabled {
    -fx-opacity: 0.4;
    -fx-cursor: default;
}

.lobby-bottom-bar {
    -fx-padding: 16 24 20 24;
    -fx-alignment: center;
    -fx-border-color: #e5e1d6;
    -fx-border-width: 1 0 0 0;
}
```

- [ ] **Step 2: 커밋**

```bash
git add src/main/resources/css/lobby.css
git commit -m "Style: lobby.css 추가 (카드/사이드바/액션 영역)"
```

---

## Task 3: RoomCard 컴포넌트

**Files:**
- Create: `src/main/java/GUI/components/RoomCard.java`

- [ ] **Step 1: `src/main/java/GUI/components/RoomCard.java` 작성**

```java
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
```

- [ ] **Step 2: 커밋**

```bash
git add src/main/java/GUI/components/RoomCard.java
git commit -m "GUI: RoomCard 컴포넌트 추가 (logo.png 아이콘 + Property 바인딩)"
```

---

## Task 4: WaitingRoomScene 스켈레톤

**Files:**
- Create: `src/main/java/GUI/WaitingRoomScene.java`

- [ ] **Step 1: `src/main/java/GUI/WaitingRoomScene.java` 작성**

```java
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
```

- [ ] **Step 2: 커밋 (보류)**

이 시점에선 `SceneManager.showLobby()`만 호출하고 있어 단독으로 컴파일 OK. 다음 태스크와 묶어서 커밋해도 되고 단독 커밋해도 됨.

```bash
git add src/main/java/GUI/WaitingRoomScene.java
git commit -m "GUI: WaitingRoomScene 빈 스켈레톤 추가 (대기실 진입 자리)"
```

---

## Task 5: SceneManager 수정 (currentNickname + showLobby/showWaitingRoom)

**Files:**
- Modify: `src/main/java/GUI/SceneManager.java`

- [ ] **Step 1: 현재 SceneManager 내용 확인**

Run: `cat src/main/java/GUI/SceneManager.java`
Expected: `init`, `baseSize`, `LoginSize`, `showLogin`, `showRegister`, `showLobby` 존재. `currentNickname` 없음.

- [ ] **Step 2: 전체 교체**

```java
package GUI;

import javafx.scene.Scene;
import javafx.stage.Stage;
import mafia.domain.Room;

public class SceneManager {

    public static Stage stage;
    public static String currentNickname; // 로그인 성공 시 LoginScene이 설정

    public static void init(Stage s){stage = s;}

    public static void baseSize(){
        stage.setMinWidth(1280);
        stage.setMinHeight(720);
        stage.setWidth(1280);
        stage.setHeight(720);
    }

    public static void LoginSize(){
        stage.setMinWidth(520);
        stage.setMinHeight(600);
        stage.setWidth(520);
        stage.setHeight(600);
    }

    public static void showLogin(){
        Scene scene = new Scene(LoginScene.create(stage));
        scene.getStylesheets().add(
            SceneManager.class.getResource("/css/login.css").toExternalForm()
        );
        stage.setScene(scene);
    }

    public static void showRegister(){
        Scene scene = new Scene(RegisterScene.create(stage));
        scene.getStylesheets().add(
            SceneManager.class.getResource("/css/login.css").toExternalForm()
        );
        stage.setScene(scene);
    }

    public static void showLobby(){
        Scene scene = new Scene(LobbyScene.create(stage));
        scene.getStylesheets().add(
            SceneManager.class.getResource("/css/login.css").toExternalForm()
        );
        scene.getStylesheets().add(
            SceneManager.class.getResource("/css/lobby.css").toExternalForm()
        );
        stage.setScene(scene);
    }

    public static void showWaitingRoom(Room room){
        Scene scene = new Scene(WaitingRoomScene.create(room));
        stage.setScene(scene);
    }
}
```

변경 요약:
- `currentNickname` 정적 필드 추가
- `showLobby()`에 lobby.css도 함께 적용 (login.css는 폰트/색 토큰 공유 위해 유지)
- `showWaitingRoom(Room)` 신규

- [ ] **Step 3: 커밋**

```bash
git add src/main/java/GUI/SceneManager.java
git commit -m "GUI: SceneManager에 currentNickname + showLobby(lobby.css) + showWaitingRoom"
```

---

## Task 6: LoginScene에서 currentNickname 설정

**Files:**
- Modify: `src/main/java/GUI/LoginScene.java`

- [ ] **Step 1: 로그인 성공 분기에 한 줄 추가**

`loginBtn.setOnAction(...)` 안의 검증 통과 후, `SceneManager.baseSize() / showLobby()` 호출 **직전**에 `currentNickname` 설정.

찾을 코드:
```java
            if (!ok) {
                errorLabel.setText("아이디 또는 비밀번호가 일치하지 않습니다");
                errorLabel.setVisible(true);
                return;
            }
            SceneManager.baseSize();
            SceneManager.showLobby();
```

다음으로 교체:
```java
            if (!ok) {
                errorLabel.setText("아이디 또는 비밀번호가 일치하지 않습니다");
                errorLabel.setVisible(true);
                return;
            }
            SceneManager.currentNickname = idText;
            SceneManager.baseSize();
            SceneManager.showLobby();
```

- [ ] **Step 2: 커밋**

```bash
git add src/main/java/GUI/LoginScene.java
git commit -m "GUI: 로그인 성공 시 SceneManager.currentNickname 설정"
```

---

## Task 7: LobbyScene 전면 재작성 (1) — 뼈대 + 더미 데이터 + 카드 그리드

이번 태스크는 LobbyScene의 가장 큰 뼈대를 만든다. 선택/입장 액션, polling, 사이드바는 이후 태스크에서 붙인다.

**Files:**
- Modify: `src/main/java/GUI/LobbyScene.java`

- [ ] **Step 1: 현재 LobbyScene 내용 확인**

Run: `cat src/main/java/GUI/LobbyScene.java`
Expected: `package GUI;` + 빈 VBox 반환만 있는 짧은 스켈레톤.

- [ ] **Step 2: 전체 교체 (이번 단계: 뼈대만)**

```java
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
```

이 시점에 IDE에서 실행하면 카드 7개가 보이지만, 클릭/빠른입장/사이드바는 없음.

- [ ] **Step 3: 커밋**

```bash
git add src/main/java/GUI/LobbyScene.java
git commit -m "GUI: LobbyScene 뼈대 (상단 바 + 카드 그리드 + 더미 7개)"
```

---

## Task 8: LobbyScene 확장 (2) — 카드 선택 + 입장 버튼 + 액션 바 + 사이드바 + polling

`Task 7`의 LobbyScene을 그대로 두고 추가 기능을 차곡차곡 붙인다.

**Files:**
- Modify: `src/main/java/GUI/LobbyScene.java`

- [ ] **Step 1: 새 import 4줄 추가**

찾을 코드 (import 블록 마지막 줄):
```java
import mafia.domain.Room;
import mafia.domain.RoomState;
```

다음으로 교체:
```java
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.util.Duration;
import mafia.domain.Room;
import mafia.domain.RoomState;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
```

- [ ] **Step 2: `create(...)` 메서드 전체를 아래로 교체**

```java
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
            Room target = null;
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
            target = candidates.get(rng.nextInt(candidates.size()));
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

        refresh.setOnAction(e -> poller.playFromStart());

        return root;
    }

    /** 헬퍼: cardByRoomId에서 안전하게 가져옴 */
    private static VBox card(Map<Integer, VBox> map, Room r) {
        return map.get(r.getRoomId());
    }
```

- [ ] **Step 3: 자체 점검**

- `Room` 한 개당 `VBox` 카드 한 개 매핑 (`cardByRoomId`)
- 단일 클릭 → 이전 선택 해제 + 현재 선택 + 입장 버튼 활성
- 더블 클릭 → 즉시 입장
- 빠른 입장 → `WAITING` + 인원 안 찬 방 무작위, 없으면 `ResultBox.showFail`
- 새로고침 → polling을 처음부터 다시 재생
- polling Timeline → 3초마다 무작위 방 인원 ±1, 10%로 상태 토글
- 사이드바: `onlineUsers` 변경 시 자동 재구성

- [ ] **Step 4: 커밋**

```bash
git add src/main/java/GUI/LobbyScene.java
git commit -m "GUI: LobbyScene 액션 + 사이드바 + 카드 선택 + 3초 polling 연결"
```

---

## Task 9: 최종 통합 시각 검증

코드 변경 없음. IDE에서 실행해 흐름 전체 훑는다.

- [ ] **Step 1: 클린 빌드 + 실행**

IDE에서 `Build > Rebuild Project` → Run `MainGame`.

- [ ] **Step 2: 진입 검증**

1. 회원가입 또는 로그인으로 진입 (예: 이전 검증에 쓴 계정).
2. 로그인 성공 → 1280×720 로비 창으로 전환.

Expected:
- 상단 좌측 "Mafia for Java" (Copperplate 와인레드)
- 상단 우측 "환영합니다, {본인 닉네임}"

- [ ] **Step 3: 카드 그리드 검증**

Expected:
- 카드 7개가 FlowPane으로 보임
- 각 카드: logo.png 원형 + 제목 + 호스트 + "n/6 · 대기/진행중"
- "조용한 밤에" / "심야 대전"은 `IN_GAME`이라 흐릿(`opacity: 0.5`)

- [ ] **Step 4: 카드 선택 검증**

- 임의 대기 카드 한 번 클릭 → 와인 보더 + 살짝 진한 배경
- 다른 대기 카드 클릭 → 이전 선택 해제, 새 선택 표시
- 진행중 카드 클릭 → 반응 없음 (보더 변화 X, 입장 버튼 그대로)
- 하단 "입장하기" 버튼은 선택 직후 활성, 처음엔 흐릿(disabled)

- [ ] **Step 5: 입장 검증**

- 선택 후 "입장하기" 클릭 → WaitingRoomScene으로 전환, "대기실: {제목}" + 안내 텍스트
- "← 로비로 돌아가기" 클릭 → 로비 복귀
- 대기 카드 더블클릭 → 즉시 WaitingRoomScene 진입

- [ ] **Step 6: 빠른 입장 / 새로고침**

- ⚡ 빠른 입장 클릭 → 대기 + 인원 안 찬 방 중 임의 선택 → 즉시 WaitingRoom 진입
- 🔄 새로고침 클릭 → polling 한 사이클 즉시 트리거 (인원수/상태가 어느 카드에서 변경되는 것이 보임)

- [ ] **Step 7: 실시간 인원수 갱신**

3초 정도 두고 관찰:
- 어떤 카드의 `n/6` 숫자가 ±1로 변동
- 가끔 상태가 토글되어 흐릿 ↔ 정상 카드로 전환
- 사용자가 아무것도 안 해도 자동으로 일어남

- [ ] **Step 8: 사이드바 검증**

- 우측에 "접속 중 (1)" 헤더
- 그 아래 본인 닉네임 한 줄 ("👤 alice  (나)" 형식, 와인레드 강조)
- 다른 더미 유저 없음

- [ ] **Step 9: 안 맞는 항목 보정**

색/크기/여백/spacing 미세 조정은 작은 별도 커밋으로 (예: "Style: 사이드바 폭 조정").

- [ ] **Step 10: 최종 git 상태**

Run: `git log --oneline -10`
Expected: Task 1~8에 해당하는 커밋이 시간순으로 나열.

Run: `git status`
Expected: working tree clean (또는 무관한 untracked 폴더만).
