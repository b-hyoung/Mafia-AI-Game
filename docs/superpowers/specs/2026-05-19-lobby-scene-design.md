# Lobby Scene Design — Mafia for Java

작성일: 2026-05-19
대상 파일:
- 신규: `src/main/java/GUI/components/RoomCard.java`, `src/main/java/GUI/WaitingRoomScene.java`, `src/main/java/mafia/domain/Room.java`, `src/main/java/mafia/domain/RoomState.java`, `src/main/resources/css/lobby.css`
- 수정: `src/main/java/GUI/LobbyScene.java`, `src/main/java/GUI/SceneManager.java`

## 1. 컨셉

로그인 직후 진입하는 로비 화면. **카드 그리드**로 현재 열린 방을 시각적으로 보여주고, 빠른 입장 / 방 선택 + 입장 두 가지 방법을 제공한다. **인원수는 JavaFX `IntegerProperty` 바인딩** + **3초 주기 `Timeline` polling**으로 변동 시 즉시 카드에 반영된다.

스코프: 이번 작업은 **UI + 더미 데이터**만. 진짜 멀티플레이어 동기화(TCP 서버, 소켓 클라이언트, 프로토콜)는 별도 sub-project로 분리. UI 코드는 미래에 데이터 소스만 교체하면 그대로 재사용 가능하도록 `ObservableList<Room>` 패턴으로 설계.

## 2. 화면 구조

창 크기: 기존 `baseSize()` 사용 → **1280×720**. 로그인 → 로비 진입 시 자동 전환됨 (기존 `LoginScene`의 `loginBtn.setOnAction`이 `baseSize() + showLobby()` 호출 중).

```
┌────────────────────────────────────────────────────────────────────────┐
│ Mafia for Java                          환영합니다, alice              │  ← 상단 바
├────────────────────────────────────────────────────────────────────────┤
│ [⚡ 빠른 입장]                                      [🔄 새로고침]      │  ← 액션 바
├──────────────────────────────────────────────────┬─────────────────────┤
│                                                  │ 접속 중 (1)         │  ← 사이드바
│  ┌──────────┐  ┌──────────┐  ┌──────────┐       │ ─────────────────  │
│  │  [🎭]    │  │  [🎭]    │  │  [🎭]    │       │ 👤 alice  (나)     │
│  │ 고수만   │  │ 아무나   │  │ 조용한   │       │                     │
│  │ 들어와   │  │ 콜       │  │ 밤에     │       │                     │
│  │          │  │          │  │          │       │                     │
│  │ 👤 alice │  │ 👤 bob   │  │ 👤 chs   │       │                     │
│  │ 4/6 대기 │  │ 2/6 대기 │  │ 6/6 진행 │       │                     │
│  └──────────┘  └──────────┘  └──────────┘       │                     │
│  ┌──────────┐  ┌──────────┐  ...                 │                     │
│  │ ...      │  │ ...      │                       │                     │
│  └──────────┘  └──────────┘                       │                     │
│                                                  │                     │
├──────────────────────────────────────────────────┴─────────────────────┤
│                          [   입장하기   ]                              │  ← 하단 액션
└────────────────────────────────────────────────────────────────────────┘
```

레이아웃: `BorderPane` 루트.
- Top: `HBox` (게임 이름 + 환영 메시지)
- 그 아래: `HBox` (빠른 입장 + 새로고침)
- Center: `HBox` (카드 그리드 영역 + 사이드바)
  - 좌측 ~1000px: `ScrollPane` 안에 `FlowPane`으로 카드들. 카드 폭이 좁아지면 자동 줄바꿈.
  - 우측 ~250px: **OnlineUserList 사이드바** (섹션 4 참고)
- Bottom: `HBox` (입장하기 버튼, 중앙 정렬)

## 3. 카드 (`RoomCard` 컴포넌트)

크기: 폭 180, 높이 220.

구성 (위→아래, 중앙 정렬):
- **캐릭터 아이콘**: `ImageView`로 `/images/logo.png` 정적 로드, 64×64, 원형 클립 (`Circle(32, 32, 32)`). 모든 카드 동일 이미지. mp4 안 씀 (다수 동시 재생 부담).
- **방 제목**: 한 줄, 너무 길면 ellipsis (`Label.setMaxWidth(...)` + `setOverrunStyle`). Copperplate 16px.
- **호스트**: `👤 nickname` 한 줄, 12px 회색.
- **인원수 · 상태**: `4/6 · 대기` 또는 `6/6 · 진행중`. 인원수는 `Room.currentPlayersProperty()`에 바인딩되어 자동 갱신.

상태에 따른 시각 차이:
- **WAITING**: 보더 회색, hover 시 와인 색 변경, 클릭 가능
- **IN_GAME**: 보더 회색 + `opacity: 0.5`, hover/클릭 비활성. "입장하기"로 들어가도 무시
- **선택됨**: 와인 보더 + 살짝 더 진한 배경

## 3-1. 사이드바: OnlineUserList

화면 우측에 현재 접속자 목록을 표시한다. 이번 sub-project에선 **서버가 없어서 본인 닉네임 한 명만** 표시. sub-project 2(TCP 서버) 단계에서 서버 push 이벤트로 다른 접속자도 자동 추가/제거되도록 확장 예정.

### 구성
- 헤더: `"접속 중 (n)"` — n은 현재 인원수, `Bindings.size(onlineUsers)`로 자동 갱신
- 구분선
- 유저 리스트: `ListView<String>` 또는 `VBox` 안에 라벨들. 각 행에 작은 아이콘(👤) + 닉네임. 본인은 `(나)` 표기 추가.

### 데이터
`ObservableList<String> onlineUsers`를 LobbyScene 안에 만들고 본인 닉네임(`SceneManager.currentNickname`) 하나만 추가:
```java
ObservableList<String> onlineUsers = FXCollections.observableArrayList();
onlineUsers.add(SceneManager.currentNickname);
```

미래에 sub-project 2에서 서버로부터 `USER_CONNECTED` / `USER_DISCONNECTED` 이벤트를 받아 이 리스트에 add/remove. UI는 자동으로 갱신.

### 스코프 (이번 작업)
- 본인 닉네임 1명만 표시
- 다른 접속자는 미구현 (서버가 없어서 정보 출처 X)
- 더미 유저 추가하지 않음 — 빈 자리에 가짜 이름 띄우는 건 혼란 야기

## 4. 도메인 모델

### `mafia.domain.Room`

```java
public class Room {
    private final int roomId;
    private final String title;
    private final String hostNickname;
    private final IntegerProperty currentPlayers;
    private final int maxPlayers;
    private final ObjectProperty<RoomState> state;
    
    // 생성자
    // getter / property 메서드들
}
```

`IntegerProperty currentPlayers` + `ObjectProperty<RoomState> state`로 두면 카드 UI에서 바인딩만 하면 변경 시 즉시 갱신. JavaFX 의존성이 도메인에 들어가지만, 학습 단계엔 단순함을 우선.

### `mafia.domain.RoomState`

```java
public enum RoomState {
    WAITING("대기"),
    IN_GAME("진행중");
    
    private final String label;
    // ...
}
```

## 5. 더미 데이터 + Polling

`LobbyScene` 안에 정적 더미 메서드:

```java
private static ObservableList<Room> createDummyRooms() {
    return FXCollections.observableArrayList(
        new Room(1, "고수만 들어와", "alice", 4, 6, RoomState.WAITING),
        new Room(2, "아무나 콜", "bob", 2, 6, RoomState.WAITING),
        new Room(3, "조용한 밤에", "charlie", 6, 6, RoomState.IN_GAME),
        new Room(4, "빠른판", "dan", 3, 6, RoomState.WAITING),
        new Room(5, "초보환영", "eve", 1, 6, RoomState.WAITING),
        new Room(6, "꿀잼각", "frank", 5, 6, RoomState.WAITING),
        new Room(7, "심야 대전", "grace", 4, 6, RoomState.IN_GAME)
    );
}
```

미래에 `mafia.client.RoomRegistry.fetchRooms()` 같은 호출로 교체 가능.

### Polling Timeline
```java
Timeline poller = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
    // 무작위 방 하나 골라 인원수 ±1 (0~max 사이) 또는 상태 토글
    Room target = rooms.get(random.nextInt(rooms.size()));
    int delta = random.nextBoolean() ? +1 : -1;
    int next = Math.max(0, Math.min(target.getMaxPlayers(), target.getCurrentPlayers() + delta));
    target.setCurrentPlayers(next);
    // 가끔 상태 토글
    if (random.nextInt(10) == 0) {
        target.setState(target.getState() == RoomState.WAITING ? RoomState.IN_GAME : RoomState.WAITING);
    }
}));
poller.setCycleCount(Timeline.INDEFINITE);
poller.play();
```

Scene이 바뀌면 `poller.stop()` 필요. `stage.sceneProperty().addListener(...)` 또는 LobbyScene 외부로 노출해서 SceneManager가 관리. 학습용엔 단순히 한 번만 시작하고 신경 안 쓰는 패턴도 OK (Timeline은 garbage collection 대상이지만 RoomCard들이 살아있는 동안엔 살아있음).

## 6. 동작 흐름

| 사용자 동작 | 결과 |
|---|---|
| 카드 클릭 | 그 방 선택. 다른 카드의 선택 해제. 와인 보더 표시. "입장하기" 버튼 활성. |
| 카드 더블클릭 | 즉시 입장 (대기 상태 방만, 진행중이면 무시) |
| **+ 카드 클릭** | **`CreateRoomDialog` 모달 표시 (제목 + 최대인원 4/6/8 선택). 만들기 → 새 Room을 rooms에 추가 + 자동으로 그 방 WaitingRoom 진입.** |
| ⚡ 빠른 입장 클릭 | `rooms` 중 `WAITING` + 인원 안 찬 방 무작위 선택 → 즉시 입장. 없으면 `ResultBox.showFail` "입장 가능한 방이 없습니다" |
| 🔄 새로고침 클릭 | polling Timeline의 한 사이클을 즉시 트리거 (`poller.playFromStart()`) |
| 입장하기 클릭 | 선택된 방 → `SceneManager.showWaitingRoom(room)`. 선택 없으면 비활성 (Button.setDisable). |

### 6-1. CreateRoomDialog (방 만들기 다이얼로그)

`GUI.components.CreateRoomDialog`. ResultBox와 같은 패턴(별도 모달 Stage, UNDECORATED, APPLICATION_MODAL). 구성:

- 타이틀: "방 만들기"
- 제목 입력 `TextField` (prompt "방 제목")
- 최대 인원 `ChoiceBox<Integer>` (선택지: 4, 6, 8, 기본 6)
- 만들기 / 취소 버튼

API:
```java
public static void show(Window owner, java.util.function.BiConsumer<String, Integer> onCreate);
```
- `onCreate` 콜백은 (title, maxPlayers)를 받음
- 취소/ESC → 콜백 호출 없이 닫힘
- 빈 제목으로 만들기 시도 → 다이얼로그 안 인라인 에러 표시

새 Room 기본값 (LobbyScene 측):
- `roomId` = 기존 rooms의 max + 1 (또는 단순 sequence)
- `hostNickname` = `SceneManager.currentNickname` ("guest" fallback)
- `currentPlayers` = 1
- `state` = WAITING

만든 후 흐름: `rooms.add(newRoom)` → `SceneManager.showWaitingRoom(newRoom)`. LobbyScene이 Scene에서 detach → poller 자동 정리 (sceneProperty listener).

### 6-2. + 카드 (방 만들기 카드)

CSS 클래스 `.room-card-plus`. 일반 카드와 같은 크기(180×220), 점선 보더(회색→hover 시 와인), 가운데 큰 `+` 글자 + 작은 "방 만들기" 라벨. RoomCard 컴포넌트가 아니라 LobbyScene 안에서 직접 VBox로 만든다 (1개만 쓰이는 단순 시각 요소).

## 7. 입장 후: `WaitingRoomScene` 자리만 마련

```java
// WaitingRoomScene.java
package GUI;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import mafia.domain.Room;

public class WaitingRoomScene {
    public static VBox create(Room room) {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        Label title = new Label("대기실: " + room.getTitle());
        Label hint = new Label("(WaitingRoom 화면은 다음 작업에서 구현)");
        root.getChildren().addAll(title, hint);
        return root;
    }
}
```

`SceneManager`에 `showWaitingRoom(Room room)` 추가. 단순히 빈 자리만.

## 8. 파일 구조

| 경로 | 상태 | 책임 |
|---|---|---|
| `src/main/java/mafia/domain/Room.java` | 신규 | 방 도메인 (IntegerProperty 등) |
| `src/main/java/mafia/domain/RoomState.java` | 신규 | enum WAITING / IN_GAME |
| `src/main/java/GUI/components/RoomCard.java` | 신규 | 단일 카드 UI (Room 받아서 `VBox` 노드 생성) |
| `src/main/java/GUI/LobbyScene.java` | 수정 (전면 재작성) | 로비 화면 전체 |
| `src/main/java/GUI/WaitingRoomScene.java` | 신규 | 빈 자리만 |
| `src/main/java/GUI/SceneManager.java` | 수정 | `showLobby()`에 lobby.css 적용 + `showWaitingRoom(Room)` 추가 + `currentNickname` 정적 필드 |
| `src/main/resources/css/lobby.css` | 신규 | 로비 전용 스타일 |

## 9. CSS 설계 (`lobby.css`)

기존 `login.css`의 컬러 토큰 동일 사용 (재정의 X). 로비 전용 클래스만:
- `.lobby-root` — 크림 옵화이트 배경
- `.lobby-topbar` — 상단 바 padding, 폰트
- `.lobby-welcome` — 환영 메시지
- `.lobby-action-bar` — 액션 영역
- `.lobby-action-btn` — 빠른 입장 / 새로고침 버튼 (login.css의 `.login-btn`과 유사하지만 작음)
- `.room-card` — 카드 베이스 (배경 흰색, 보더 회색, radius 12)
- `.room-card:hover` — hover 시 와인 보더
- `.room-card-selected` — 선택 시 와인 보더 + 살짝 진한 배경
- `.room-card-disabled` — 진행 중인 방 (opacity 0.5)
- `.room-card-title` — 카드 제목 (Copperplate 16, 와인레드)
- `.room-card-host` — 호스트 닉네임
- `.room-card-status` — 인원수 · 상태
- `.lobby-enter-btn` — 입장하기 (login-btn과 유사)
- `.lobby-enter-btn:disabled` — 선택 안 됐을 때 흐릿
- `.online-sidebar` — 사이드바 컨테이너 (좌측 보더, 살짝 다른 배경 톤)
- `.online-sidebar-header` — "접속 중 (n)" 헤더 (Copperplate 14)
- `.online-user-row` — 한 줄 (아이콘 + 닉네임)
- `.online-user-me` — 본인 표시 강조 (와인레드)

## 10. SceneManager 변경 요약

```java
public class SceneManager {
    public static Stage stage;
    public static String currentNickname;   // ← 로그인 성공 시 LoginScene이 set
    
    // 기존 메서드들...
    
    public static void showLobby() {
        Scene scene = new Scene(LobbyScene.create(stage));
        scene.getStylesheets().add(getResource("/css/lobby.css"));
        stage.setScene(scene);
    }
    
    public static void showWaitingRoom(Room room) {
        Scene scene = new Scene(WaitingRoomScene.create(room));
        // 스타일 시트는 일단 lobby.css 공용 또는 신규 — 빈 자리라 무관
        stage.setScene(scene);
    }
}
```

`LoginScene`의 로그인 성공 분기에서 `SceneManager.currentNickname = idText;` 추가 (한 줄).

## 11. 스코프 (이번 작업이 안 하는 것)

- **TCP 서버 / 소켓 클라이언트 / 프로토콜** — Sub-project 2로 분리
- **방 만들기** — 사용자 명시 X
- **검색 / 필터 / 정렬** — 더미 리스트 그대로
- **WaitingRoomScene 내부 구현** — 빈 자리만
- **로그아웃 / 유저 메뉴** — 상단 텍스트만, 액션 없음
- **카드 캐릭터 다양화** — 모든 카드 같은 logo.png
- **카드 mp4 애니메이션** — 정적 PNG만
- **방 안 채팅, 게임 시작 흐름** — 별도 작업
- **사이드바에 다른 접속자 표시** — 서버가 없어서 본인만. 더미 유저 추가하지 않음 (혼란 방지)

## 12. 성공 기준

- 로그인 성공 후 1280×720 로비 창에 진입 (`baseSize`)
- 상단 바에 "Mafia for Java" 와 환영 메시지("환영합니다, {nickname}")가 표시됨
- 카드 7개가 FlowPane으로 표시됨. 각 카드에 logo.png 원형 + 제목 + 호스트 + 인원수/상태
- 진행 중인 방은 흐릿하게 + 클릭 비활성
- 카드 클릭 → 선택(와인 보더). 다른 카드 클릭 시 이전 선택 해제
- 3초마다 무작위 카드의 인원수가 ±1로 변동 (UI 즉시 반영)
- 🔄 새로고침 클릭 → 즉시 한 사이클 트리거
- ⚡ 빠른 입장 → 대기 중 + 인원 안 찬 방 임의 선택 → WaitingRoomScene 진입
- 입장하기 → 선택된 방 진입 (선택 안 됐으면 버튼 비활성)
- WaitingRoomScene에 "대기실: {제목}" + 안내 텍스트만 보임
- 우측 사이드바에 "접속 중 (1)" 헤더와 본인 닉네임 1행 ("👤 alice  (나)" 형식)이 표시됨
