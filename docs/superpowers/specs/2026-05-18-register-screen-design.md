# Register Screen Design — Mafia for Java

작성일: 2026-05-18
대상 파일:
- 신규: `src/main/java/GUI/RegisterScene.java`, `src/main/java/GUI/SceneLogo.java`
- 수정: `src/main/java/GUI/LoginScene.java`, `src/main/java/GUI/SceneManager.java`
- 스타일: 기존 `src/main/resources/css/login.css` 재사용 (신규 CSS 없음)

## 1. 컨셉

LoginScene과 **동일한 구조/톤**의 회원가입 화면. 새 비주얼 컨셉 없이 기존 다크 누아르 → 라이트 빈티지로 굳혀진 스타일(크림 옵화이트 배경 + Copperplate 와인레드 타이틀 + 라운드 입력창)을 그대로 따른다.

가입 흐름은 표준: 가입 완료 → 로그인 화면 복귀 → 사용자가 새 계정으로 다시 로그인.

## 2. 화면 구조

LoginScene 패턴을 그대로 따르되 입력 필드 1개 추가(`PASSWORD CONFIRM`).

```
┌────────────────────────────────┐
│      [영상 로고 120×120]        │  ← LoginScene과 동일 영상
│       Mafia for Java           │  ← Copperplate 와인레드 타이틀
│                                │
│   ┌──────────────────────┐   │
│   │  ID                  │   │
│   └──────────────────────┘   │
│   ┌──────────────────────┐   │
│   │  PASSWORD            │   │
│   └──────────────────────┘   │
│   ┌──────────────────────┐   │
│   │  PASSWORD CONFIRM    │   │  ← 신규 필드
│   └──────────────────────┘   │
│   [에러 메시지 자리]            │
│                                │
│       [   가입하기   ]          │
│                                │
│   이미 계정이 있으신가요? 로그인   │  ← LoginScene 복귀 링크
└────────────────────────────────┘
```

창 크기: LoginScene과 동일하게 **520×600**. 필드가 1개 늘었지만, root VBox의 `spacing`을 16→12로, padding을 40→32로 살짝 줄여 한 화면에 들어가게 한다.

## 3. 필드 + 검증

DB의 `users` 테이블 스키마(`username`, `password_hash`, `created_at`)에 맞춰 회원가입 입력은 ID/PW/PW 확인 3가지.

| 필드 | 컨트롤 | prompt text |
|---|---|---|
| ID | `TextField` | `ID` |
| PASSWORD | `PasswordField` | `PASSWORD` |
| PASSWORD CONFIRM | `PasswordField` | `PASSWORD CONFIRM` |

### 검증 규칙 (이번 작업 범위)

순서대로:
1. 셋 중 하나라도 비어있음 → 에러 표시 "모든 항목을 입력해주세요"
2. PW ≠ PW 확인 → 에러 표시 "비밀번호가 일치하지 않습니다"
3. 모두 통과 → (TODO: `UserDao.register(...)` 호출) → `SceneManager.showLogin()`로 복귀

세 입력 필드 중 어느 하나에 `textProperty` 변화가 발생하면 `errorLabel.setVisible(false)`로 메시지를 즉시 숨긴다 (LoginScene과 같은 패턴).

## 4. 파일 구조

```
src/main/java/GUI/
├── RegisterScene.java       (신규) 회원가입 화면
├── SceneLogo.java           (신규) 영상→PNG fallback + 원형 클립 로고 헬퍼
├── LoginScene.java          (수정) 로고 로딩을 SceneLogo로 위임 + 회원가입 링크 연결
└── SceneManager.java        (수정) showRegister() 추가
```

### 4.1 `SceneLogo.java` — 로고 컴포넌트 추출

LoginScene과 RegisterScene이 동일한 로고를 표시하므로 중복을 피해 한 곳으로 추출한다. 작은 프로젝트지만 두 화면에서 같은 코드를 복사하는 시점이 곧 추출 시점.

**책임**: `/videos/logo.mp4`가 있으면 무한 루프 + 음소거 + 중앙 정사각형 viewport + 원형 클립으로 재생. 없으면 `/images/logo.png`를 같은 방식으로 표시. 둘 다 없으면 `null` 반환.

**시그니처**:
```java
public class SceneLogo {
    public static Node create(double size); // 정사각형 한 변의 크기 (예: 120)
}
```

반환 타입을 `Node`로 둬서 호출부는 분기 없이 `if (logo != null) root.getChildren().add(logo);` 한 줄로 처리.

### 4.2 `RegisterScene.java`

LoginScene 패턴과 동일한 정적 팩토리:
```java
public static VBox create(Stage stage)
```

내부 구성:
- `VBox root` (styleClass `login-root`)
- `SceneLogo.create(120)` 호출해 받은 Node를 children에 추가 (있으면)
- `Label title` "Mafia for Java" (styleClass `login-title`)
- `TextField id`, `PasswordField pw`, `PasswordField pwConfirm` (모두 styleClass `login-field`, `setMaxWidth(320)`)
- `Label errorLabel` (styleClass `login-error`, 초기 invisible)
- `Button registerBtn` "가입하기" (styleClass `login-btn`, `setMaxWidth(320)`)
- `Hyperlink backLink` "이미 계정이 있으신가요? 로그인" (styleClass `login-link`)

### 4.3 `LoginScene.java` 수정

- 로고 로딩 블록(MediaPlayer/ImageView 분기 + viewport + 원형 클립 + Rectangle2D import)을 제거하고 `Node logo = SceneLogo.create(120); if (logo != null) root.getChildren().add(logo);`로 교체. 관련 import들도 정리.
- `signupLink.setOnAction(e -> { ... })` TODO 주석을 `SceneManager.showRegister();`로 교체.

### 4.4 `SceneManager.java` 수정

`showRegister()` 메서드 추가. `showLogin()`과 같은 패턴으로 `Scene`을 생성하고 `/css/login.css`를 stylesheet으로 추가한 뒤 `stage.setScene()`. 별도 `RegisterSize()`는 만들지 않고 `LoginSize(520×600)`를 그대로 사용한다 (호출은 LoginScene 진입 시 이미 됐고 화면 크기 유지).

## 5. 동작 흐름

- **LoginScene → RegisterScene**: 회원가입 Hyperlink 클릭 → `SceneManager.showRegister()`
- **RegisterScene → LoginScene**: "로그인" Hyperlink 클릭 → `SceneManager.showLogin()`
- **가입 버튼 클릭**:
  1. 빈칸 검증 → 통과 못 하면 에러 메시지 표시 후 종료
  2. PW 일치 검증 → 통과 못 하면 에러 메시지 표시 후 종료
  3. TODO 주석: `// UserDao.register(id, pw)` 자리 (DAO 미구현)
  4. `SceneManager.showLogin()`로 복귀
- 화면 전환 시 영상 로고는 `Scene`이 교체되면서 새로 MediaPlayer 인스턴스가 만들어진다. 짧은 시점에 두 인스턴스가 공존할 수 있지만 즉시 garbage collection 대상이라 실용적 문제 없음.

## 6. 스타일 (CSS 재사용)

신규 CSS 파일 없음. 기존 `src/main/resources/css/login.css`의 클래스 그대로:
- `.login-root` — 배경, padding, spacing (단, RegisterScene은 spacing/padding을 코드에서 살짝 줄여 한 화면에 맞춤)
- `.login-field` — 입력창
- `.login-error` — 에러 라벨
- `.login-btn` — 가입 버튼
- `.login-link` — 보조 링크

`showRegister()`에서 `scene.getStylesheets().add(...)`로 같은 css를 적용한다.

## 7. 스코프 (이번 작업이 하지 않는 것)

- 실제 DB 저장: `DBConnect`/`UserDao`가 아직 없으므로 TODO 주석만 둔다.
- 비밀번호 해싱: DAO 구현 시 처리.
- 중복 사용자명 검사: DAO 책임.
- 비밀번호 길이/형식 검증 (영문/숫자 혼합 등): 빈칸 + 일치만.
- 비밀번호 찾기 화면: 별도 작업.
- 가입 후 자동 로그인: 표준 흐름에서 제외 — 사용자가 LoginScene에서 직접 다시 입력.
- 회원가입 성공 토스트/알림: 그냥 LoginScene으로 복귀만.

## 8. 성공 기준

- LoginScene의 "회원가입" 링크를 클릭하면 RegisterScene이 표시된다.
- RegisterScene의 레이아웃이 위 도식 그대로(영상 로고 → 타이틀 → ID → PW → PW 확인 → 에러 → 가입하기 → 로그인 링크) 표시되며 520×600 창에 잘 들어간다.
- 빈칸 상태로 "가입하기"를 누르면 "모든 항목을 입력해주세요" 빨간 메시지가 표시된다.
- PW와 PW 확인이 다른 상태로 누르면 "비밀번호가 일치하지 않습니다" 메시지가 표시된다.
- 세 필드 중 어느 하나라도 입력이 시작되면 에러 메시지가 즉시 사라진다.
- 셋 다 채우고 PW 일치 상태에서 "가입하기" → LoginScene으로 돌아간다 (DB 저장은 TODO).
- RegisterScene의 "로그인" 링크 클릭 → LoginScene으로 돌아간다.
- LoginScene 로고 코드가 `SceneLogo.create()` 호출 한 줄로 줄어들었고, 영상/PNG/원형 클립 동작은 이전과 동일하다.
