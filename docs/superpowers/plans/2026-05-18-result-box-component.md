# ResultBox Component Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 가입 성공/실패를 표시할 공용 모달 박스 컴포넌트(`ResultBox`)를 만들고, RegisterScene이 검증 통과 후 임시 더미 분기로 success/fail 박스 중 하나를 띄우도록 연결한다.

**Architecture:** 새 패키지 `GUI.components`에 `ResultBox` 정적 클래스 추가. `showSuccess(...)`, `showFail(...)` 두 정적 메서드로 노출하고 내부적으로는 모드 enum을 받는 private 헬퍼 하나로 공통 처리(DRY). 박스는 `UNDECORATED + APPLICATION_MODAL` Stage로 띄우고, 모드 차이는 CSS의 descendant selector(`.result-box-success .result-box-title` 등)로만 표현. 인라인 errorLabel은 그대로 두고 박스는 원격 결과 전용.

**Tech Stack:** Java 21 + JavaFX 21 (controls).

**Testing Approach:** JavaFX UI는 자동 단위 테스트 인프라가 없으므로 **수동 시각 검증** 단계로 검증한다. 각 태스크는 코드 작성 + 자체 점검 후 커밋. Task 4에서 한 번에 IDE 실행으로 흐름 검증.

**Spec Reference:** `docs/superpowers/specs/2026-05-18-result-box-component-design.md`

---

## File Structure

| 경로 | 상태 | 책임 |
|---|---|---|
| `src/main/resources/css/result-box.css` | 신규 | 박스 전용 스타일 (공통 + success/fail 모드별) |
| `src/main/java/GUI/components/ResultBox.java` | 신규 | 결과 박스 컴포넌트 (success/fail 두 정적 메서드 + 공통 헬퍼) |
| `src/main/java/GUI/RegisterScene.java` | 수정 | 검증 통과 후 ResultBox 호출 (임시 admin 분기) |

`GUI.components` 패키지는 앞으로 재사용 위젯들이 모일 자리. ResultBox는 그 첫 멤버.

---

## Task 1: result-box.css 작성

박스 전용 스타일 파일. 공통 클래스 + 모드별 descendant selector로 색만 분기.

**Files:**
- Create: `src/main/resources/css/result-box.css`

- [ ] **Step 1: `src/main/resources/css/result-box.css` 작성**

```css
/* === Mafia for Java — Result Box ===
   JavaFX uses -fx- prefix on all CSS properties.

   Modes:
     .result-box-success  — wine accent (#a83a3a)
     .result-box-fail     — soft red accent (#c4554d)
*/

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

- [ ] **Step 2: 파일 작성 확인**

Run: `head -5 src/main/resources/css/result-box.css`
Expected: 첫 줄 `/* === Mafia for Java — Result Box === ...` 표시.

- [ ] **Step 3: 커밋**

```bash
git add src/main/resources/css/result-box.css
git commit -m "Style: ResultBox 전용 CSS (success/fail 모드)"
```

---

## Task 2: ResultBox 컴포넌트 작성

`GUI.components` 패키지에 `ResultBox.java`. 두 정적 메서드(`showSuccess`, `showFail`) + 공통 private 헬퍼. ESC/OK 둘 다 onClose 호출.

**Files:**
- Create: `src/main/java/GUI/components/ResultBox.java`

- [ ] **Step 1: 패키지 폴더 생성**

```bash
mkdir -p src/main/java/GUI/components
```

- [ ] **Step 2: `src/main/java/GUI/components/ResultBox.java` 작성**

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

/**
 * 가입/로그인 결과 등 원격 응답을 표시하는 공용 모달 박스.
 * success/fail 두 모드 — 박스 구조는 동일하고 CSS 클래스로만 색을 분기한다.
 */
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
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) close.run();
        });

        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
```

- [ ] **Step 3: 자체 점검**

import 모두 사용 중인지(Modality, Stage, StageStyle, Window, Scene, KeyCode, VBox, Button, Label, Pos), 메서드 시그니처가 spec과 일치하는지 확인.

- [ ] **Step 4: 커밋**

```bash
git add src/main/java/GUI/components/ResultBox.java
git commit -m "GUI: ResultBox 컴포넌트 추가 (success/fail 모달 박스)"
```

---

## Task 3: RegisterScene에 ResultBox 호출 연결

검증 통과 후 곧장 `showLogin()`을 호출하던 부분을 ResultBox 경유로 바꾼다. 임시로 ID가 `"admin"`이면 실패 분기.

**Files:**
- Modify: `src/main/java/GUI/RegisterScene.java`

- [ ] **Step 1: import 추가**

찾을 코드:
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

다음으로 교체:
```java
import GUI.components.ResultBox;
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

- [ ] **Step 2: 검증 통과 후 분기를 ResultBox로 교체**

찾을 코드:
```java
            if (!pwText.equals(pwConfirmText)) {
                errorLabel.setText("비밀번호가 일치하지 않습니다");
                errorLabel.setVisible(true);
                return;
            }

            // TODO: UserDao.register(idText, pwText) — DAO 미구현
            SceneManager.showLogin();
```

다음으로 교체:
```java
            if (!pwText.equals(pwConfirmText)) {
                errorLabel.setText("비밀번호가 일치하지 않습니다");
                errorLabel.setVisible(true);
                return;
            }

            // TODO: 진짜 DAO 붙으면 아래 임시 분기를 UserDao.register(...) 호출로 교체
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

- [ ] **Step 3: 자체 점검**

`stage` 파라미터가 `create(Stage stage)`에서 받은 그대로 ResultBox에 전달되는지, `ok` 분기 안에서 다른 변수에 접근하지 않는지 확인.

- [ ] **Step 4: 커밋**

```bash
git add src/main/java/GUI/RegisterScene.java
git commit -m "GUI: RegisterScene 가입 결과를 ResultBox로 표시 (임시 admin=실패 분기)"
```

---

## Task 4: 최종 통합 시각 검증

코드 변경 없음. IDE에서 실행해 동작을 한 번 훑는다.

- [ ] **Step 1: 클린 리빌드 + 실행**

IDE에서 Build > Rebuild Project → Run MainGame.

- [ ] **Step 2: 성공 시나리오 검증**

1. LoginScene 떴을 때 "회원가입" 클릭 → RegisterScene 진입
2. ID에 `test` (또는 `admin`이 아닌 임의 문자열), PW와 PW Confirm 동일 값 입력
3. "가입하기" 클릭

Expected:
- 부모 창 중앙에 윈도우 데코 없는 박스 등장
- 타이틀 "회원가입 성공" — Copperplate 폰트, 와인레드 (#a83a3a)
- 메시지 "회원가입이 완료되었습니다" — Helvetica 13px, 다크 차콜
- "확인" 버튼 — 와인 보더, hover 시 와인 배경 + 크림 글자
- 박스 떠 있는 동안 RegisterScene은 클릭 안 됨 (모달)

- [ ] **Step 3: 성공 박스 닫기 → LoginScene 복귀 검증**

박스의 "확인" 클릭.

Expected:
- 박스 닫힘
- LoginScene으로 화면 전환

ESC 키로도 동일하게 동작하는지 확인 (박스를 다시 띄워서 ESC 눌러보기).

- [ ] **Step 4: 실패 시나리오 검증**

LoginScene → 회원가입 → ID를 `admin` 입력, PW/PW Confirm 같은 값으로 채우고 "가입하기".

Expected:
- 타이틀 "회원가입 실패" — 빨강 (#c4554d)
- 메시지 "이미 사용 중인 아이디입니다"
- "확인" 버튼 — 빨강 보더, hover 시 빨강 배경
- "확인" 클릭 → 박스 닫히고 RegisterScene에 머무름 (필드 값 유지, LoginScene으로 안 감)

- [ ] **Step 5: 인라인 errorLabel 유지 검증**

- 빈 상태로 "가입하기" → 빨간 인라인 메시지 "모든 항목을 입력해주세요" (박스 X)
- PW ≠ PW Confirm → 인라인 "비밀번호가 일치하지 않습니다" (박스 X)
- 입력 시작 시 인라인 즉시 숨김

박스는 검증 모두 통과한 후에만 떠야 함.

- [ ] **Step 6: 안 맞는 항목 보정**

색/크기/여백 미세 조정은 별도 작은 커밋으로(예: "Style: ResultBox 그림자 강도 조정").

- [ ] **Step 7: 최종 git 상태 확인**

Run: `git log --oneline -8`
Expected: Task 1~3 커밋이 시간순으로 나열.

Run: `git status`
Expected: working tree clean (또는 docs untracked 폴더만).
