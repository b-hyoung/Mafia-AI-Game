# Login Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** "Mafia for Java" 로그인 화면을 다크 누아르 톤(배경/타이틀/입력창/버튼)과 캐주얼한 자바 개발자 로고가 의도적으로 충돌하는 컨셉으로 꾸미고, 빈칸 검증 + 에러 메시지 표시 UI를 마련한다.

**Architecture:** JavaFX 21 컨트롤(`VBox`, `ImageView`, `Label`, `TextField`, `PasswordField`, `Button`, `Hyperlink`)로 구조를 짜고, 외부 CSS 파일(`/css/login.css`)에서 컬러/폰트/여백을 일괄 관리한다. 동적 동작(빈칸 검증, 에러 라벨 표시/숨김)만 Java 코드에서 처리한다. 로고 이미지는 `/images/logo.png`로 로드하되, 파일이 없으면 `ImageView`를 트리에 추가하지 않아 크래시를 피한다.

**Tech Stack:** Java + JavaFX 21 + Maven (javafx-maven-plugin, `mvn javafx:run`).

**Testing Approach:** JavaFX UI는 자동 단위 테스트가 까다롭고 현재 프로젝트에 테스트 인프라가 없다. 각 태스크는 **수동 시각 검증** 단계(`mvn javafx:run` 실행 후 화면 확인)로 검증한다. 단, 작은 단위 + 즉시 확인 + 빈번한 커밋 원칙은 유지.

**Spec Reference:** `docs/superpowers/specs/2026-05-18-login-screen-design.md`

---

## File Structure

| 경로 | 상태 | 책임 |
|---|---|---|
| `src/main/resources/css/login.css` | 신규 | 다크 누아르 비주얼 토큰(색/폰트/여백) 정의. 로그인 화면 전체 스타일. |
| `src/main/resources/images/.gitkeep` | 신규 | 사용자가 `logo.png`를 넣을 폴더를 git에 등록 |
| `src/main/resources/images/logo.png` | 신규 (사용자) | 로고 이미지. **사용자가 직접 저장** |
| `src/main/java/GUI/SceneManager.java` | 수정 | 로그인 창 크기 520×600으로 조정, `showLogin()`에 CSS 연결 |
| `src/main/java/GUI/LoginScene.java` | 전면 재작성 | 로고/타이틀/입력/에러/버튼/링크 구조 + 빈칸 검증 |
| `src/main/java/GUI/MainGame.java` | 수정 (1줄) | 창 제목을 "Mafia for Java"로 변경 |

각 파일은 단일 책임에 충실하다: `login.css`는 비주얼 토큰, `LoginScene.java`는 구조와 동작, `SceneManager.java`는 화면 전환과 크기, `MainGame.java`는 진입점만.

---

## Task 1: CSS 파일 생성 (비주얼 토큰)

**Files:**
- Create: `src/main/resources/css/login.css`

- [ ] **Step 1: `src/main/resources/css/` 폴더가 없으면 생성**

```bash
mkdir -p src/main/resources/css
```

- [ ] **Step 2: `src/main/resources/css/login.css` 작성**

```css
/* === Mafia for Java — Login Scene ===
   JavaFX uses -fx- prefix on all CSS properties.
   Color tokens:
     bg          #0a0a0a
     surface     #1a1a1a
     text        #e8e8e8
     text-muted  #8a8a8a
     accent-gold #c9a961
     accent-red  #8b0000
*/

.login-root {
    -fx-background-color: #0a0a0a;
    -fx-padding: 40 40 40 40;
    -fx-alignment: center;
    -fx-spacing: 16;
}

.login-title {
    -fx-font-family: "Georgia";
    -fx-font-size: 42px;
    -fx-font-weight: bold;
    -fx-text-fill: #c9a961;
    -fx-padding: 0 0 12 0;
}

.login-field {
    -fx-background-color: #1a1a1a;
    -fx-text-fill: #e8e8e8;
    -fx-prompt-text-fill: #5a5a5a;
    -fx-border-color: #2a2a2a;
    -fx-border-width: 1;
    -fx-padding: 8 12 8 12;
    -fx-pref-width: 320;
    -fx-pref-height: 36;
    -fx-font-size: 14px;
}

.login-field:focused {
    -fx-border-color: #c9a961;
    -fx-background-color: #1a1a1a;
}

.login-error {
    -fx-text-fill: #8b0000;
    -fx-font-size: 12px;
    -fx-min-height: 18;
    -fx-pref-width: 320;
    -fx-alignment: center;
}

.login-btn {
    -fx-background-color: transparent;
    -fx-text-fill: #c9a961;
    -fx-border-color: #c9a961;
    -fx-border-width: 1;
    -fx-pref-width: 320;
    -fx-pref-height: 40;
    -fx-font-size: 14px;
    -fx-font-weight: bold;
    -fx-cursor: hand;
    -fx-background-radius: 0;
    -fx-border-radius: 0;
}

.login-btn:hover {
    -fx-background-color: #c9a961;
    -fx-text-fill: #0a0a0a;
}

.login-link {
    -fx-text-fill: #8a8a8a;
    -fx-font-size: 12px;
    -fx-border-color: transparent;
    -fx-background-color: transparent;
    -fx-cursor: hand;
    -fx-padding: 2 4 2 4;
}

.login-link:hover {
    -fx-text-fill: #c9a961;
}

.login-link-sep {
    -fx-text-fill: #5a5a5a;
    -fx-font-size: 12px;
    -fx-padding: 0 4 0 4;
}

.login-link-row {
    -fx-alignment: center;
    -fx-padding: 4 0 0 0;
}
```

- [ ] **Step 3: 파일이 정상 작성됐는지 확인**

Run: `cat src/main/resources/css/login.css | head -5`
Expected: 첫 줄에 `/* === Mafia for Java — Login Scene ===` 표시

- [ ] **Step 4: 커밋**

```bash
git add src/main/resources/css/login.css
git commit -m "Style: 로그인 화면 CSS 토큰 추가 (다크 누아르 컬러/폰트)"
```

---

## Task 2: 이미지 폴더 자리 마련

**Files:**
- Create: `src/main/resources/images/.gitkeep`

- [ ] **Step 1: 이미지 폴더 생성 + .gitkeep 추가**

```bash
mkdir -p src/main/resources/images
touch src/main/resources/images/.gitkeep
```

- [ ] **Step 2: 사용자에게 안내 (구현자 노트)**

이 단계에서 사용자가 직접 로고 이미지를 `src/main/resources/images/logo.png` 경로로 저장해야 한다. 이미지가 없어도 다음 태스크는 진행 가능하다 — 코드가 null 체크로 방어한다. 로고는 화면에서 빠지고 타이틀부터 시작될 뿐 크래시는 나지 않는다.

- [ ] **Step 3: 커밋**

```bash
git add src/main/resources/images/.gitkeep
git commit -m "Resource: 로고 이미지 폴더 자리 마련 (.gitkeep)"
```

---

## Task 3: SceneManager 수정 (창 크기 + CSS 연결)

**Files:**
- Modify: `src/main/java/GUI/SceneManager.java`

- [ ] **Step 1: 현재 파일 내용 확인**

Run: `cat src/main/java/GUI/SceneManager.java`
Expected: 25줄 정도, `LoginSize()`는 510×370 min만 설정, `showLogin()`은 Scene에 stylesheet를 안 붙임

- [ ] **Step 2: `SceneManager.java` 전체를 아래로 교체**

```java
package GUI;

import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

    public static Stage stage;

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

    public static void showLobby(){
        stage.setScene(new Scene(LobbyScene.create(stage)));
    }
}
```

변경 요약:
- `baseSize()`: `setMinWidth/setMinHeight`에 더해 `setWidth/setHeight`도 호출 → 로비로 갈 때 실제 크기가 1280×720이 되도록.
- `LoginSize()`: 510×370 → 520×600. 로고가 들어갈 공간 확보.
- `showLogin()`: `Scene`을 변수로 받아 `getStylesheets().add(...)`로 `/css/login.css` 연결 후 `setScene` 호출.

- [ ] **Step 3: 컴파일 확인**

Run: `mvn -q compile`
Expected: BUILD SUCCESS (또는 적어도 컴파일 에러 없음)

- [ ] **Step 4: 앱 실행해서 창 크기 확인**

Run: `mvn javafx:run`
Expected: 로그인 창이 **520×600** 크기로 뜬다. 내부는 아직 스타일 적용 안 된 기존 모습(흰 배경 + ID/PW/Login 컨트롤). 창 닫고 다음 단계로.

> 만약 `Could not find resource /css/login.css`가 나오면 Task 1을 다시 확인. 파일 경로는 `src/main/resources/css/login.css`가 맞다.

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/GUI/SceneManager.java
git commit -m "GUI: SceneManager 창 크기 520x600 + login.css 연결"
```

---

## Task 4: LoginScene 구조 재작성 (로고 + 타이틀 + 입력 + 버튼)

이 태스크는 LoginScene 전체를 새 구조로 갈아낀다. 빈칸 검증과 링크는 다음 태스크에서 붙인다.

**Files:**
- Modify: `src/main/java/GUI/LoginScene.java`

- [ ] **Step 1: 현재 LoginScene 내용 확인**

Run: `cat src/main/java/GUI/LoginScene.java`
Expected: 27줄 정도, `VBox`에 `id, pw, loginBtn`만 있고 클래스 부여 없음.

- [ ] **Step 2: `LoginScene.java` 전체를 아래로 교체**

```java
package GUI;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;

public class LoginScene {

    public static VBox create(Stage stage) {
        VBox root = new VBox();
        root.getStyleClass().add("login-root");
        root.setAlignment(Pos.CENTER);

        // Logo (optional — skipped if image file is missing)
        URL logoUrl = LoginScene.class.getResource("/images/logo.png");
        if (logoUrl != null) {
            ImageView logo = new ImageView(new Image(logoUrl.toExternalForm()));
            logo.setFitWidth(120);
            logo.setFitHeight(120);
            logo.setPreserveRatio(true);
            root.getChildren().add(logo);
        }

        // Title
        Label title = new Label("MAFIA  FOR  JAVA");
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

        // Login button
        Button loginBtn = new Button("LOGIN");
        loginBtn.getStyleClass().add("login-btn");
        loginBtn.setMaxWidth(320);

        loginBtn.setOnAction(e -> {
            // TODO: DAO로 실제 로그인 검증
            SceneManager.baseSize();
            SceneManager.showLobby();
        });

        root.getChildren().addAll(title, id, pw, loginBtn);

        return root;
    }
}
```

이 단계에서의 결정:
- `setMaxWidth(320)`을 Java에서 호출 → CSS의 `-fx-pref-width: 320`과 함께 작동해서 너비가 320으로 고정된다 (VBox가 자식을 늘리지 않게).
- 로고는 없어도 OK — `getResource`가 null을 반환하면 그냥 건너뜀.
- 에러 라벨, 회원가입/비번찾기 링크, 입력 변화 리스너는 Task 5에서 추가.

- [ ] **Step 3: 컴파일 확인**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: 앱 실행해서 시각 확인**

Run: `mvn javafx:run`

기대하는 화면:
- 검정 배경(`#0a0a0a`)
- (이미지가 있다면) 상단에 캐주얼 로고 120×120
- 골드(`#c9a961`) 큰 세리프 타이틀 "MAFIA  FOR  JAVA"
- 어두운 회색(`#1a1a1a`) 배경의 ID/PASSWORD 입력창 2개, 보더는 거의 보이지 않다가 포커스되면 골드로 변함
- 골드 보더 + 투명 배경의 "LOGIN" 버튼, hover 시 골드 배경 + 검정 글자
- 로그인 버튼 누르면 로비로 이동 (기존 동작)

문제가 보이면 어느 부분이 안 맞는지 적어두고 CSS/코드를 한 번에 손본다 (지금은 구조만 검증).

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/GUI/LoginScene.java
git commit -m "GUI: LoginScene 다크 누아르 스타일링 + 로고/타이틀 구조"
```

---

## Task 5: 에러 라벨 + 빈칸 검증 + 입력 시 자동 숨김

**Files:**
- Modify: `src/main/java/GUI/LoginScene.java`

- [ ] **Step 1: `LoginScene.java`의 `// Login button` 블록 위에 에러 Label 추가**

찾을 위치: `pw.setMaxWidth(320);` 다음 빈 줄.

삽입할 코드 (Login button 선언 **위**):

```java
        // Error label (hidden until validation fails)
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("login-error");
        errorLabel.setVisible(false);
```

- [ ] **Step 2: `loginBtn.setOnAction(...)` 본문을 빈칸 검증으로 교체**

찾을 코드:

```java
        loginBtn.setOnAction(e -> {
            // TODO: DAO로 실제 로그인 검증
            SceneManager.baseSize();
            SceneManager.showLobby();
        });
```

다음으로 교체:

```java
        loginBtn.setOnAction(e -> {
            String idText = id.getText() == null ? "" : id.getText().trim();
            String pwText = pw.getText() == null ? "" : pw.getText();

            if (idText.isEmpty() || pwText.isEmpty()) {
                errorLabel.setText("아이디와 비밀번호를 입력해주세요");
                errorLabel.setVisible(true);
                return;
            }

            // TODO: DAO로 실제 검증. 실패하면:
            //   errorLabel.setText("아이디 또는 비밀번호가 일치하지 않습니다");
            //   errorLabel.setVisible(true);
            //   return;

            SceneManager.baseSize();
            SceneManager.showLobby();
        });
```

- [ ] **Step 3: 입력 변화 시 에러 라벨 자동 숨김 리스너 추가**

`loginBtn.setOnAction(...)` 블록 **아래**, `root.getChildren().addAll(...)` **위**에 추가:

```java
        // Hide error message as soon as the user starts editing either field
        id.textProperty().addListener((obs, oldVal, newVal) -> errorLabel.setVisible(false));
        pw.textProperty().addListener((obs, oldVal, newVal) -> errorLabel.setVisible(false));
```

- [ ] **Step 4: `root.getChildren().addAll(...)`에 `errorLabel` 포함**

찾을 코드:

```java
        root.getChildren().addAll(title, id, pw, loginBtn);
```

다음으로 교체:

```java
        root.getChildren().addAll(title, id, pw, errorLabel, loginBtn);
```

`errorLabel`이 입력창과 버튼 사이에 들어가야 한다.

- [ ] **Step 5: 컴파일 확인**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 6: 앱 실행해서 동작 검증**

Run: `mvn javafx:run`

수동 검증 시나리오:
1. **빈 상태에서 로그인 클릭** → 빨간 글자로 "아이디와 비밀번호를 입력해주세요" 표시, 버튼 위치는 흔들리지 않음 (`-fx-min-height: 18`로 자리 확보).
2. **ID만 입력하고 로그인 클릭** → 같은 에러 메시지 표시.
3. **에러가 떠있는 상태에서 ID 또는 PW에 글자 입력** → 즉시 에러 메시지 사라짐.
4. **둘 다 채우고 로그인 클릭** → 기존처럼 로비 화면으로 전환되고 창이 1280×720으로 커짐.

문제가 있으면 어디서 끊겼는지 적어두고 코드 보정.

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/GUI/LoginScene.java
git commit -m "GUI: 로그인 빈칸 검증 + 에러 라벨 표시/자동 숨김"
```

---

## Task 6: 회원가입 / 비밀번호 찾기 링크 추가

**Files:**
- Modify: `src/main/java/GUI/LoginScene.java`

- [ ] **Step 1: import 두 줄 추가**

`LoginScene.java` 상단 import 영역에 추가:

```java
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.HBox;
```

- [ ] **Step 2: 링크 컴포넌트와 HBox 생성 코드 추가**

`root.getChildren().addAll(...)` **바로 위**에 삽입:

```java
        // Sub-links (signup / find password) — UI only, no behavior yet
        Hyperlink signupLink = new Hyperlink("회원가입");
        signupLink.getStyleClass().add("login-link");
        signupLink.setOnAction(e -> {
            // TODO: 회원가입 화면 연결
        });

        Label sep = new Label("|");
        sep.getStyleClass().add("login-link-sep");

        Hyperlink findLink = new Hyperlink("비밀번호 찾기");
        findLink.getStyleClass().add("login-link");
        findLink.setOnAction(e -> {
            // TODO: 비밀번호 찾기 흐름
        });

        HBox linkRow = new HBox(signupLink, sep, findLink);
        linkRow.getStyleClass().add("login-link-row");
        linkRow.setAlignment(Pos.CENTER);
```

- [ ] **Step 3: `root.getChildren().addAll(...)`에 `linkRow` 포함**

찾을 코드:

```java
        root.getChildren().addAll(title, id, pw, errorLabel, loginBtn);
```

다음으로 교체:

```java
        root.getChildren().addAll(title, id, pw, errorLabel, loginBtn, linkRow);
```

- [ ] **Step 4: 컴파일 확인**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: 앱 실행해서 시각 확인**

Run: `mvn javafx:run`

기대하는 화면:
- 로그인 버튼 아래에 **회색 작은 텍스트**로 "회원가입 | 비밀번호 찾기" 표시
- 두 링크 모두 hover 시 골드 색으로 변함
- 클릭해도 화면은 변하지 않음 (TODO 주석만 있는 빈 핸들러)

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/GUI/LoginScene.java
git commit -m "GUI: 회원가입/비밀번호 찾기 링크 자리 추가 (기능 TODO)"
```

---

## Task 7: 창 제목을 "Mafia for Java"로 변경

기존 `MainGame.java`의 창 제목은 "마피아 in AI"인데, 새 게임 정체성에 맞춰 통일.

**Files:**
- Modify: `src/main/java/GUI/MainGame.java`

- [ ] **Step 1: 현재 줄 확인**

Run: `grep "setTitle" src/main/java/GUI/MainGame.java`
Expected: `        stage.setTitle("마피아 in AI");`

- [ ] **Step 2: 한 줄 교체**

찾을 줄:
```java
        stage.setTitle("마피아 in AI");
```

다음으로 교체:
```java
        stage.setTitle("Mafia for Java");
```

- [ ] **Step 3: 앱 실행해서 타이틀바 확인**

Run: `mvn javafx:run`
Expected: 창 타이틀바에 "Mafia for Java" 표시

- [ ] **Step 4: 커밋**

```bash
git add src/main/java/GUI/MainGame.java
git commit -m "GUI: 창 제목을 'Mafia for Java'로 변경"
```

---

## Task 8: 최종 통합 검증

이 태스크는 코드 변경 없음. 처음부터 끝까지 동작을 한 번 훑는다.

- [ ] **Step 1: 깨끗한 상태에서 빌드 + 실행**

Run: `mvn -q clean compile && mvn javafx:run`

- [ ] **Step 2: 스펙 9번 "성공 기준" 항목을 하나씩 체크**

스펙 파일: `docs/superpowers/specs/2026-05-18-login-screen-design.md`

체크리스트:
- [ ] 위 레이아웃대로 표시됨 (로고 → 타이틀 → ID → PW → 에러자리 → 버튼 → 링크)
- [ ] 다크 누아르 컬러 토큰 적용 (검정 배경, 골드 타이틀/버튼, 회색 링크)
- [ ] 로고 이미지가 원본 색상 그대로 노출됨 (이미지 파일이 있는 경우) / 없으면 타이틀부터 시작하고 크래시 X
- [ ] 회원가입/비밀번호 찾기 링크 표시되고 클릭해도 화면 변화 X
- [ ] 빈 상태로 로그인 클릭 → 빨간 에러 메시지 표시
- [ ] 입력 시작하면 에러 메시지 사라짐
- [ ] 둘 다 채우고 로그인 → 로비 전환 + 창이 1280×720으로 커짐
- [ ] 창이 520×600으로 열림 (로그인 화면 진입 시)

- [ ] **Step 3: 안 맞는 항목이 있으면 해당 태스크로 돌아가서 보정 후 별도 커밋**

각 보정은 작은 단위로 별도 커밋. "Style: 로그인 버튼 hover 색 미세 조정" 같은 식.

- [ ] **Step 4: 최종 git 상태 확인**

Run: `git log --oneline -10`
Expected: Task 1~7에 해당하는 커밋이 시간순으로 나열됨.

Run: `git status`
Expected: working tree clean (또는 무관한 변경만 남아있음).
