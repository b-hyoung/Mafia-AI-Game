# Register Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** LoginScene 패턴을 그대로 따른 회원가입 화면(`RegisterScene`)을 만들고, 두 화면이 공유하는 영상/원형 로고 로직을 `SceneLogo` 헬퍼로 추출한다. 빈칸·PW 일치 검증과 화면 전환(`Login ↔ Register`)을 제공하되, 실제 DB 저장은 TODO로 남겨둔다.

**Architecture:** 기존 GUI 패키지의 정적 팩토리 패턴을 그대로 따른다 (`*.create(Stage)` → `VBox` 반환). 로고 컴포넌트만 별도 헬퍼 클래스(`SceneLogo`)로 추출해 LoginScene/RegisterScene이 같은 코드를 공유한다. 신규 CSS 없이 기존 `login.css`의 클래스를 재사용한다.

**Tech Stack:** Java 21 + JavaFX 21 (controls + media) + Maven (`mvn javafx:run`).

**Testing Approach:** JavaFX UI는 자동 단위 테스트가 까다롭고 현재 프로젝트에 테스트 인프라가 없다. 각 태스크는 **수동 시각 검증** 단계(`mvn javafx:run` 또는 IDE Run 후 화면 확인)로 검증한다. 작은 단위 + 즉시 확인 + 빈번한 커밋 원칙 유지.

**Spec Reference:** `docs/superpowers/specs/2026-05-18-register-screen-design.md`

---

## File Structure

| 경로 | 상태 | 책임 |
|---|---|---|
| `src/main/java/GUI/SceneLogo.java` | 신규 | 영상/PNG fallback + 원형 클립을 적용한 로고 `Node` 생성 헬퍼 |
| `src/main/java/GUI/RegisterScene.java` | 신규 | 회원가입 화면 (LoginScene과 동일 패턴, 필드 1개 추가) |
| `src/main/java/GUI/LoginScene.java` | 수정 | 로고 로딩 블록을 `SceneLogo.create()` 호출로 교체 + 회원가입 링크 연결 |
| `src/main/java/GUI/SceneManager.java` | 수정 | `showRegister()` 메서드 추가 (login.css 공유) |

각 파일은 단일 책임: `SceneLogo`는 로고 노드 한 가지만, `LoginScene`/`RegisterScene`은 각 화면의 구조와 동작, `SceneManager`는 화면 전환과 stylesheet 연결.

---

## Task 0: 누적된 미커밋 변경 정리

회원가입 작업을 시작하기 전에 working tree에 쌓인 로그인 화면 마이크로 조정들(영상 로고, 원형 클립, 폰트/색 변경 등)을 커밋해서 회원가입 변경과 섞이지 않게 한다.

**Files:**
- Modify: `pom.xml` (javafx-media 추가)
- Modify: `src/main/java/GUI/LoginScene.java` (영상 로고/원형 클립)
- Modify: `src/main/resources/css/login.css` (라이트 빈티지 톤 + Copperplate)
- Create: `src/main/resources/images/logo.png`, `src/main/resources/videos/logo.mp4` (이미 working tree에 존재)

- [ ] **Step 1: 변경 사항 검토**

Run: `git status --short && git diff --stat`
Expected: pom.xml, LoginScene.java, login.css 수정 + images/logo.png, videos/logo.mp4 untracked.

- [ ] **Step 2: 변경 파일들을 한꺼번에 스테이지 + 영상/이미지 자산 포함**

```bash
git add pom.xml \
        src/main/java/GUI/LoginScene.java \
        src/main/resources/css/login.css \
        src/main/resources/images/logo.png \
        src/main/resources/videos/logo.mp4
```

- [ ] **Step 3: 커밋**

```bash
git commit -m "$(cat <<'EOF'
GUI: 로그인 화면 비주얼 마무리 (영상 로고 + 라이트 빈티지 톤 + Copperplate)

- javafx-media 의존성 추가, MediaView로 영상 로고 무한 루프 + 원형 클립
- 다크 누아르 → 라이트 빈티지(크림 옵화이트 + 와인레드) 톤 전환
- 타이틀 폰트 Copperplate, 입력창/버튼 라운드 8px
- 영상 viewport로 중앙 정사각형 크롭, PNG fallback 유지
EOF
)"
```

- [ ] **Step 4: 깨끗한 working tree 확인**

Run: `git status`
Expected: `nothing to commit, working tree clean` (또는 untracked로 `docs/plans/`, `docs/specs/`만 남아있음 — 이 둘은 별도 폴더라 무시 OK).

---

## Task 1: SceneLogo 헬퍼 클래스 생성

LoginScene의 로고 로딩 블록을 그대로 추출해 별도 클래스로 만든다. 동작은 같아야 한다 (영상 우선, PNG fallback, viewport, 원형 클립).

**Files:**
- Create: `src/main/java/GUI/SceneLogo.java`

- [ ] **Step 1: `src/main/java/GUI/SceneLogo.java` 작성**

```java
package GUI;

import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Circle;

import java.net.URL;

/**
 * 로고 노드 생성 헬퍼.
 * /videos/logo.mp4가 있으면 무한 루프 + 음소거 + 중앙 정사각형 viewport + 원형 클립으로 재생.
 * 없으면 /images/logo.png를 같은 방식으로 표시. 둘 다 없으면 null 반환.
 */
public class SceneLogo {

    public static Node create(double size) {
        URL videoUrl = SceneLogo.class.getResource("/videos/logo.mp4");
        URL imageUrl = SceneLogo.class.getResource("/images/logo.png");

        if (videoUrl != null) {
            Media media = new Media(videoUrl.toExternalForm());
            MediaPlayer player = new MediaPlayer(media);
            player.setCycleCount(MediaPlayer.INDEFINITE);
            player.setMute(true);
            player.setAutoPlay(true);
            MediaView view = new MediaView(player);
            view.setFitWidth(size);
            view.setFitHeight(size);
            view.setPreserveRatio(true);
            view.setClip(new Circle(size / 2, size / 2, size / 2));
            player.setOnReady(() -> {
                double mw = media.getWidth();
                double mh = media.getHeight();
                if (mw > 0 && mh > 0) {
                    double s = Math.min(mw, mh);
                    double x = (mw - s) / 2.0;
                    double y = (mh - s) / 2.0;
                    view.setViewport(new Rectangle2D(x, y, s, s));
                }
            });
            return view;
        }

        if (imageUrl != null) {
            ImageView logo = new ImageView(new Image(imageUrl.toExternalForm()));
            logo.setFitWidth(size);
            logo.setFitHeight(size);
            logo.setPreserveRatio(true);
            logo.setClip(new Circle(size / 2, size / 2, size / 2));
            return logo;
        }

        return null;
    }
}
```

- [ ] **Step 2: 컴파일 가능 여부 자체 점검**

import 모두 사용 중인지, JavaFX 21 API와 일치하는지 확인. `Node` 반환 타입과 `MediaView`/`ImageView`가 모두 `Node`의 하위인지 확인 (둘 다 맞음).

- [ ] **Step 3: 커밋**

```bash
git add src/main/java/GUI/SceneLogo.java
git commit -m "GUI: SceneLogo 헬퍼 추출 (영상/PNG fallback + 원형 클립)"
```

---

## Task 2: LoginScene을 SceneLogo로 리팩토링

LoginScene의 로고 로딩 블록을 `SceneLogo.create(120)` 한 줄로 교체하고, 더 이상 필요 없는 import들(Media, MediaPlayer, MediaView, Image, ImageView, Circle, Rectangle2D, URL)을 제거한다.

**Files:**
- Modify: `src/main/java/GUI/LoginScene.java`

- [ ] **Step 1: 현재 LoginScene 내용 확인**

Run: `head -45 src/main/java/GUI/LoginScene.java`
Expected: import에 Media/MediaPlayer/MediaView/Image/ImageView/Rectangle2D/Circle/URL이 있고, `create(Stage)` 안에 영상/이미지 분기 블록이 길게 있음.

- [ ] **Step 2: import 블록 정리**

찾을 코드:
```java
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.net.URL;
```

다음으로 교체:
```java
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
```

- [ ] **Step 3: 로고 로딩 블록 한 줄로 교체**

찾을 코드 (긴 영상/이미지 분기 블록 전체):
```java
        // Logo — prefer mp4 (looping, muted) if present, fall back to PNG, skip if neither exists
        URL videoUrl = LoginScene.class.getResource("/videos/logo.mp4");
        URL imageUrl = LoginScene.class.getResource("/images/logo.png");
        if (videoUrl != null) {
            Media media = new Media(videoUrl.toExternalForm());
            MediaPlayer player = new MediaPlayer(media);
            player.setCycleCount(MediaPlayer.INDEFINITE);
            player.setMute(true);
            player.setAutoPlay(true);
            MediaView view = new MediaView(player);
            view.setFitWidth(120);
            view.setFitHeight(120);
            view.setPreserveRatio(true);
            view.setClip(new Circle(60, 60, 60));
            // Crop center square from the source so non-square videos fill the circle
            player.setOnReady(() -> {
                double mw = media.getWidth();
                double mh = media.getHeight();
                if (mw > 0 && mh > 0) {
                    double size = Math.min(mw, mh);
                    double x = (mw - size) / 2.0;
                    double y = (mh - size) / 2.0;
                    view.setViewport(new Rectangle2D(x, y, size, size));
                }
            });
            root.getChildren().add(view);
        } else if (imageUrl != null) {
            ImageView logo = new ImageView(new Image(imageUrl.toExternalForm()));
            logo.setFitWidth(120);
            logo.setFitHeight(120);
            logo.setPreserveRatio(true);
            logo.setClip(new Circle(60, 60, 60));
            root.getChildren().add(logo);
        }
```

다음으로 교체:
```java
        // Logo (delegated to SceneLogo helper — same instance shape used by RegisterScene)
        Node logo = SceneLogo.create(120);
        if (logo != null) {
            root.getChildren().add(logo);
        }
```

- [ ] **Step 4: 시각 검증**

IDE에서 LoginScene 실행. 변경 전과 화면이 **완전히 동일**해야 한다:
- 영상 로고가 같은 위치, 같은 크기, 무한 루프, 원형 클립 적용
- 영상이 없으면 PNG로 fallback (동일 모양)
- 타이틀 / 입력창 / 에러 / 버튼 / 링크 모두 그대로

차이가 보이면 SceneLogo의 코드와 원본 코드를 비교해서 누락된 설정을 찾는다.

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/GUI/LoginScene.java
git commit -m "GUI: LoginScene 로고 로딩을 SceneLogo 헬퍼로 위임"
```

---

## Task 3: SceneManager에 showRegister() 메서드 추가

회원가입 씬을 띄울 진입점을 SceneManager에 추가한다. 동일 stylesheet(`login.css`)를 적용해 톤을 일치시킨다.

**Files:**
- Modify: `src/main/java/GUI/SceneManager.java`

- [ ] **Step 1: 현재 SceneManager 내용 확인**

Run: `cat src/main/java/GUI/SceneManager.java`
Expected: `showLogin()`, `showLobby()`가 정의돼 있고 `showRegister()`는 없다.

- [ ] **Step 2: `showLobby()` 위에 `showRegister()` 추가**

찾을 코드:
```java
    public static void showLobby(){
        stage.setScene(new Scene(LobbyScene.create(stage)));
    }
}
```

다음으로 교체:
```java
    public static void showRegister(){
        Scene scene = new Scene(RegisterScene.create(stage));
        scene.getStylesheets().add(
            SceneManager.class.getResource("/css/login.css").toExternalForm()
        );
        stage.setScene(scene);
    }

    public static void showLobby(){
        stage.setScene(new Scene(LobbyScene.create(stage)));
    }
}
```

이 시점에서는 `RegisterScene` 클래스가 아직 없으므로 **컴파일 에러가 발생한다**. 다음 태스크에서 RegisterScene을 만들면 해결된다.

- [ ] **Step 3: 커밋 보류 — Task 4 끝나고 묶어서 커밋**

이 태스크는 단독으로 컴파일이 안 되므로 별도 커밋하지 않는다. Task 4의 RegisterScene 작성을 마치고 함께 커밋한다.

---

## Task 4: RegisterScene 기본 구조 (로고 + 타이틀 + 입력 + 가입 버튼)

LoginScene을 본떠 회원가입 화면의 뼈대를 만든다. 검증 로직과 보조 링크는 다음 태스크에서 붙인다.

**Files:**
- Create: `src/main/java/GUI/RegisterScene.java`

- [ ] **Step 1: `src/main/java/GUI/RegisterScene.java` 작성**

```java
package GUI;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class RegisterScene {

    public static VBox create(Stage stage) {
        VBox root = new VBox();
        root.getStyleClass().add("login-root");
        root.setAlignment(Pos.CENTER);
        // Tighter spacing/padding to fit one extra field in the same 520x600 window
        root.setSpacing(12);
        root.setStyle("-fx-padding: 32 32 32 32;");

        // Logo (shared helper)
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

        PasswordField pwConfirm = new PasswordField();
        pwConfirm.setPromptText("PASSWORD CONFIRM");
        pwConfirm.getStyleClass().add("login-field");
        pwConfirm.setMaxWidth(320);

        // Register button
        Button registerBtn = new Button("가입하기");
        registerBtn.getStyleClass().add("login-btn");
        registerBtn.setMaxWidth(320);
        registerBtn.setOnAction(e -> {
            // TODO: 검증 + DAO 등록 — Task 5에서 채움
        });

        root.getChildren().addAll(title, id, pw, pwConfirm, registerBtn);

        return root;
    }
}
```

이 시점에서 `SceneManager.showRegister()`(Task 3)와 함께 컴파일이 통과한다.

- [ ] **Step 2: 컴파일/실행 확인**

IDE에서 컴파일 (Build > Build Project) → 에러 없어야 함. 아직 LoginScene → RegisterScene 진입로가 없으므로 화면은 띄울 수 없다 — Task 6에서 연결한 뒤 확인.

- [ ] **Step 3: Task 3 + Task 4 묶어서 커밋**

```bash
git add src/main/java/GUI/SceneManager.java \
        src/main/java/GUI/RegisterScene.java
git commit -m "GUI: SceneManager.showRegister() 추가 + RegisterScene 뼈대"
```

---

## Task 5: RegisterScene 검증 로직 (빈칸 + PW 일치 + 자동 숨김)

회원가입 버튼 클릭 시 빈칸/PW 일치를 검증하고 통과하면 LoginScene으로 돌아간다. 입력 시작하면 에러 메시지 자동 숨김.

**Files:**
- Modify: `src/main/java/GUI/RegisterScene.java`

- [ ] **Step 1: `pwConfirm.setMaxWidth(320);` 아래에 errorLabel 선언 추가**

찾을 코드:
```java
        PasswordField pwConfirm = new PasswordField();
        pwConfirm.setPromptText("PASSWORD CONFIRM");
        pwConfirm.getStyleClass().add("login-field");
        pwConfirm.setMaxWidth(320);

        // Register button
```

다음으로 교체:
```java
        PasswordField pwConfirm = new PasswordField();
        pwConfirm.setPromptText("PASSWORD CONFIRM");
        pwConfirm.getStyleClass().add("login-field");
        pwConfirm.setMaxWidth(320);

        // Error label (hidden until validation fails)
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("login-error");
        errorLabel.setVisible(false);

        // Register button
```

- [ ] **Step 2: `registerBtn.setOnAction(...)` 본문 교체**

찾을 코드:
```java
        registerBtn.setOnAction(e -> {
            // TODO: 검증 + DAO 등록 — Task 5에서 채움
        });
```

다음으로 교체:
```java
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

            // TODO: UserDao.register(idText, pwText) — DAO 미구현
            SceneManager.showLogin();
        });
```

- [ ] **Step 3: 입력 변화 시 에러 라벨 자동 숨김 리스너 추가**

`registerBtn.setOnAction(...)` 블록 **아래**, `root.getChildren().addAll(...)` **위**에 삽입:

```java
        // Hide error message when the user edits any field
        id.textProperty().addListener((obs, oldVal, newVal) -> errorLabel.setVisible(false));
        pw.textProperty().addListener((obs, oldVal, newVal) -> errorLabel.setVisible(false));
        pwConfirm.textProperty().addListener((obs, oldVal, newVal) -> errorLabel.setVisible(false));
```

- [ ] **Step 4: `root.getChildren().addAll(...)`에 errorLabel 포함**

찾을 코드:
```java
        root.getChildren().addAll(title, id, pw, pwConfirm, registerBtn);
```

다음으로 교체:
```java
        root.getChildren().addAll(title, id, pw, pwConfirm, errorLabel, registerBtn);
```

`errorLabel`이 PW Confirm 아래, 가입 버튼 위.

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/GUI/RegisterScene.java
git commit -m "GUI: RegisterScene 검증 (빈칸 + PW 일치) + 에러 라벨 자동 숨김"
```

---

## Task 6: RegisterScene 보조 링크 (로그인 화면 복귀)

가입 화면 하단에 "이미 계정이 있으신가요? 로그인" 링크를 추가해 LoginScene으로 돌아갈 수 있게 한다.

**Files:**
- Modify: `src/main/java/GUI/RegisterScene.java`

- [ ] **Step 1: import 두 줄 추가**

상단 import 영역에 추가:
```java
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.HBox;
```

- [ ] **Step 2: textProperty 리스너 블록 아래에 링크 행 추가**

찾을 코드:
```java
        id.textProperty().addListener((obs, oldVal, newVal) -> errorLabel.setVisible(false));
        pw.textProperty().addListener((obs, oldVal, newVal) -> errorLabel.setVisible(false));
        pwConfirm.textProperty().addListener((obs, oldVal, newVal) -> errorLabel.setVisible(false));

        root.getChildren().addAll(title, id, pw, pwConfirm, errorLabel, registerBtn);
```

다음으로 교체:
```java
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
```

- [ ] **Step 3: 커밋**

```bash
git add src/main/java/GUI/RegisterScene.java
git commit -m "GUI: RegisterScene에 로그인 복귀 링크 추가"
```

---

## Task 7: LoginScene의 회원가입 링크를 showRegister()에 연결

LoginScene의 `signupLink`가 지금은 TODO 주석만 있다. 클릭 시 RegisterScene으로 가도록 연결한다.

**Files:**
- Modify: `src/main/java/GUI/LoginScene.java`

- [ ] **Step 1: 현재 signupLink 핸들러 확인**

Run: `grep -A 3 "signupLink.setOnAction" src/main/java/GUI/LoginScene.java`
Expected:
```java
        signupLink.setOnAction(e -> {
            // TODO: 회원가입 화면 연결
        });
```

- [ ] **Step 2: 회원가입 화면 진입으로 교체**

찾을 코드:
```java
        signupLink.setOnAction(e -> {
            // TODO: 회원가입 화면 연결
        });
```

다음으로 교체:
```java
        signupLink.setOnAction(e -> SceneManager.showRegister());
```

- [ ] **Step 3: 커밋**

```bash
git add src/main/java/GUI/LoginScene.java
git commit -m "GUI: LoginScene 회원가입 링크 → showRegister() 연결"
```

---

## Task 8: 최종 통합 검증

코드 변경 없음. 처음부터 끝까지 동작을 한 번 훑는다.

- [ ] **Step 1: 깨끗한 빌드 + 실행**

IDE에서 Build > Rebuild Project → Run MainGame.
(또는 터미널: `mvn -q clean compile && mvn javafx:run` — `mvn` 사용 가능한 환경에서.)

- [ ] **Step 2: 스펙 8번 "성공 기준" 항목 체크**

스펙 파일: `docs/superpowers/specs/2026-05-18-register-screen-design.md`

체크리스트:
- [ ] LoginScene의 "회원가입" 링크 클릭 → RegisterScene으로 전환
- [ ] RegisterScene 레이아웃: 영상 로고 → 타이틀 → ID → PW → PW Confirm → 에러 자리 → 가입하기 → "이미 계정이 있으신가요? 로그인" 링크
- [ ] 520×600 창에 모든 요소가 잘림 없이 들어감 (`spacing=12`, `padding=32`)
- [ ] 빈칸 상태로 "가입하기" → 빨간 "모든 항목을 입력해주세요" 표시
- [ ] PW와 PW Confirm 다르게 입력하고 "가입하기" → "비밀번호가 일치하지 않습니다" 표시
- [ ] ID/PW/PW Confirm 중 하나라도 입력 시작하면 에러 메시지 즉시 숨김
- [ ] 셋 다 채우고 PW 일치 상태에서 "가입하기" → LoginScene으로 복귀 (DB 저장은 TODO)
- [ ] RegisterScene의 "로그인" 링크 → LoginScene으로 복귀
- [ ] LoginScene의 영상 로고가 SceneLogo 헬퍼로 위임된 후에도 이전과 동일하게 무한 루프 + 원형 클립으로 재생

- [ ] **Step 3: 안 맞는 항목 보정 + 별도 커밋**

스타일 미세 조정은 "Style: RegisterScene 여백 조정" 같은 작은 별도 커밋으로.

- [ ] **Step 4: 최종 git 상태 확인**

Run: `git log --oneline -10`
Expected: Task 0~7에 해당하는 커밋이 시간순으로 나열됨.

Run: `git status`
Expected: working tree clean (또는 무관한 변경만 남음).
