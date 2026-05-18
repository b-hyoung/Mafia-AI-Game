# ResultBox 컴포넌트 Design

작성일: 2026-05-18
대상 파일:
- 신규: `src/main/java/GUI/components/ResultBox.java`, `src/main/resources/css/result-box.css`
- 수정: `src/main/java/GUI/RegisterScene.java`

## 1. 컨셉

공용 모달 결과 박스 컴포넌트. **타이틀 + 메시지 + 확인 버튼** 구조 하나에 **success / fail 두 모드**. 모드 차이는 CSS만으로(색).

### 사용 시점 구분
- **인라인 errorLabel** (기존 `.login-error`): 빈칸 / PW 일치 검증처럼 **입력 직접 결과**.
- **ResultBox** (이번 작업): 가입 성공/실패 같은 **원격 결과(DAO 응답급 임팩트)**.

이 둘은 책임이 다르므로 공존한다. 박스로 모든 에러를 통일하지 않는다.

## 2. 박스 구조

```
┌────────────────────────────┐
│        회원가입 성공         │   ← 타이틀
│                             │
│  회원가입이 완료되었습니다     │   ← 메시지
│                             │
│           확인              │   ← OK 버튼
└────────────────────────────┘
```

- 표시 방식: 새 `Stage` 인스턴스
  - `StageStyle.UNDECORATED` — 윈도우 데코레이션 없음(순수 박스)
  - `Modality.APPLICATION_MODAL` — 박스 떠 있는 동안 부모 창 입력 차단
  - 부모 창(`owner`)을 받아서 그 중앙에 위치
- 닫힘: 확인 버튼 클릭 또는 `ESC` → `onClose` 콜백 실행 후 박스 close
- 크기: 폭 360, 높이는 콘텐츠에 맞춰 자동

## 3. API

```java
public class ResultBox {
    public static void showSuccess(Window owner, String title, String message, Runnable onClose);
    public static void showFail(Window owner, String title, String message, Runnable onClose);
}
```

- 두 정적 메서드로 분리해 호출부에서 의도(`showSuccess` vs `showFail`)가 명확해진다.
- 내부적으로는 모드 enum을 받는 private 헬퍼로 공통 처리(중복 제거).
- `onClose`가 `null`이면 박스 닫기만 하고 추가 동작 없음.

### Window 파라미터
호출처는 `stage.getScene().getWindow()` 또는 직접 가진 `Stage`를 전달한다. RegisterScene의 `create(Stage stage)`에서는 인자로 받은 `stage`를 그대로 넘기면 된다.

## 4. 파일 구조

| 경로 | 상태 | 책임 |
|---|---|---|
| `src/main/java/GUI/components/ResultBox.java` | 신규 | 결과 박스 컴포넌트 (success/fail 모드, 모달 Stage 생성) |
| `src/main/resources/css/result-box.css` | 신규 | 박스 전용 스타일 (공통 + 모드별) |
| `src/main/java/GUI/RegisterScene.java` | 수정 | 검증 통과 후 ResultBox.showSuccess/showFail 호출 |

새 패키지 `GUI.components`는 앞으로 재사용 UI 부품(로고 헬퍼는 이미 `GUI.SceneLogo`로 별개, 컴포넌트성 위젯은 여기로) 자리.

## 5. CSS 설계 (`result-box.css`)

JavaFX의 descendant selector를 활용해 모드 클래스 하나만 토글하면 색이 일괄 바뀌도록 한다.

```css
.result-box {
    -fx-background-color: #faf8f4;
    -fx-padding: 24 28 20 28;
    -fx-spacing: 14;
    -fx-alignment: center;
    -fx-background-radius: 12;
    -fx-border-radius: 12;
    -fx-border-color: #e5e1d6;
    -fx-border-width: 1;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 16, 0, 0, 4);
}

.result-box-title {
    -fx-font-family: "Copperplate", "Apple SD Gothic Neo", serif;
    -fx-font-size: 22px;
    -fx-font-weight: bold;
}

.result-box-message {
    -fx-font-family: "Helvetica Neue", "Apple SD Gothic Neo", sans-serif;
    -fx-font-size: 13px;
    -fx-text-fill: #3b2a1a;
}

.result-box-btn {
    -fx-background-color: transparent;
    -fx-border-width: 1;
    -fx-pref-width: 200;
    -fx-pref-height: 36;
    -fx-font-size: 13px;
    -fx-font-weight: bold;
    -fx-cursor: hand;
    -fx-background-radius: 8;
    -fx-border-radius: 8;
}

/* === success mode === */
.result-box-success .result-box-title { -fx-text-fill: #a83a3a; }
.result-box-success .result-box-btn   { -fx-text-fill: #a83a3a; -fx-border-color: #a83a3a; }
.result-box-success .result-box-btn:hover {
    -fx-background-color: #a83a3a;
    -fx-text-fill: #faf8f4;
}

/* === fail mode === */
.result-box-fail .result-box-title { -fx-text-fill: #c4554d; }
.result-box-fail .result-box-btn   { -fx-text-fill: #c4554d; -fx-border-color: #c4554d; }
.result-box-fail .result-box-btn:hover {
    -fx-background-color: #c4554d;
    -fx-text-fill: #faf8f4;
}
```

루트 `VBox`에 `.result-box` + 모드 클래스(`.result-box-success` 또는 `.result-box-fail`) 둘을 add. 자식 노드들은 한 가지 클래스(`.result-box-title` 등)만 갖고, 색은 부모 모드 클래스에 의해 결정.

## 6. ResultBox 구현 개요

```java
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
            ResultBox.class.getResource("/css/result-box.css").toExternalForm()
        );

        Runnable close = () -> {
            dialog.close();
            if (onClose != null) onClose.run();
        };

        okBtn.setOnAction(e -> close.run());
        scene.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) close.run(); });

        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
```

- `showAndWait()`로 호출부가 박스 닫힐 때까지 블록 → onClose 호출 후 호출부 흐름 재개.
- 박스 크기 360×180은 짧은 한 줄 메시지 기준. `setWrapText(true)`로 긴 메시지도 줄바꿈.

## 7. RegisterScene 호출 흐름

검증 통과 후의 분기를 ResultBox로 교체.

```java
// 기존:
//   // TODO: UserDao.register(idText, pwText) — DAO 미구현
//   SceneManager.showLogin();
//
// 변경 후:
// TODO: 진짜 DAO 붙으면 아래 임시 분기를 교체
boolean ok = !"admin".equalsIgnoreCase(idText);
if (ok) {
    ResultBox.showSuccess(
        stage,
        "회원가입 성공",
        "회원가입이 완료되었습니다",
        () -> SceneManager.showLogin()
    );
} else {
    ResultBox.showFail(
        stage,
        "회원가입 실패",
        "이미 사용 중인 아이디입니다",
        null
    );
}
```

**임시 더미**: 진짜 DAO가 없어서 실패 트리거가 없으므로, ID가 `"admin"`이면 실패로 분기한다. 진짜 DAO 들어오면 이 한 줄(`boolean ok = ...`)을 DAO 호출로 교체.

## 8. 스코프 (이번 작업이 안 하는 것)

- LoginScene/로그인 흐름의 박스화 — 가입 화면만 적용. 로그인 결과 박스는 별도 작업.
- 진짜 `UserDao` 구현 — 임시 더미 분기(`"admin"` == 실패)로 대체.
- 박스 등장/사라짐 애니메이션 (페이드/슬라이드 등).
- success/fail 외 모드(경고/확인/입력) — 이번에는 두 가지만.
- `Enter` 키로 확인 — OK 버튼 클릭과 `ESC`만.
- 박스 위치 미세 조정/드래그.

## 9. 성공 기준

- ID, PW, PW Confirm 다 채우고 PW 일치 + ID가 `"admin"`이 아닌 상태로 "가입하기" → 와인레드 톤의 **성공 박스**("회원가입 성공" / "회원가입이 완료되었습니다") 표시.
- 성공 박스의 "확인" 클릭 → 박스 닫힘 + LoginScene으로 복귀.
- ID를 `"admin"`으로 입력하고 가입 → 빨강 톤의 **실패 박스**("회원가입 실패" / "이미 사용 중인 아이디입니다") 표시.
- 실패 박스의 "확인" 클릭 → 박스 닫힘 + RegisterScene에 머무름(필드 값 유지).
- 박스는 부모 창 중앙, 윈도우 데코레이션 없음, 부모 잠금.
- `ESC` 키로도 박스 닫힘 (확인 버튼과 동일한 onClose 동작).
- 인라인 errorLabel(빈칸/PW 일치)은 변화 없이 그대로 동작.
- LoginScene 흐름은 변화 없음 (회원가입 링크 → RegisterScene 그대로).
