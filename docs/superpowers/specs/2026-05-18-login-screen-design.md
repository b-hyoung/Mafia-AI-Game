# Login Screen Design — Mafia for Java

작성일: 2026-05-18
대상 파일: `src/main/java/GUI/LoginScene.java`, `src/main/java/GUI/SceneManager.java` (수정)
신규 파일: `src/main/resources/css/login.css`, `src/main/resources/images/logo.png`

## 1. 컨셉

게임 이름은 **"Mafia for Java"**. 로그인 화면은 **의도된 톤 충돌**을 컨셉으로 잡는다.

- 배경/타이틀/입력창/버튼: **다크 누아르** (정통 마피아 영화 톤)
- 로고: **캐주얼한 자바 개발자 일러스트** (안경 + 노트북 + 컵라면 캐릭터, 원본 색감 유지)

양쪽을 타협 없이 강하게 밀어서 "B급 위트가 게임 정체성"이 되도록 한다. 어중간하게 섞으면 디자인 실수처럼 보이므로 피한다.

## 2. 비주얼 토큰

### 컬러
| 토큰 | 값 | 용도 |
|---|---|---|
| `--bg` | `#0a0a0a` | 전체 배경 |
| `--surface` | `#1a1a1a` | 입력창 배경 |
| `--text` | `#e8e8e8` | 메인 텍스트, 입력값 |
| `--text-muted` | `#8a8a8a` | 보조 텍스트, 링크 기본 |
| `--accent-gold` | `#c9a961` | 타이틀, 버튼 보더 |
| `--accent-red` | `#8b0000` | 에러 메시지 텍스트 색 |

### 폰트
- 타이틀: 시스템 세리프 (`Georgia` 우선, fallback: 시스템 기본 세리프), `-fx-font-weight: bold`, **42px**. JavaFX CSS에는 `letter-spacing`이 없으므로 글자 사이 공백을 직접 삽입해서 자간 효과를 낸다: `"MAFIA  FOR  JAVA"` (단어 사이는 공백 2개)
- 본문/입력: 시스템 sans-serif, 14px
- 링크: 시스템 sans-serif, 12px, underline 없음

## 3. 레이아웃

중앙 세로 정렬 (`VBox`, `alignment: center`). 위에서 아래로:

```
┌────────────────────────────────┐
│                                │
│      [로고 이미지 120×120]      │
│                                │
│       MAFIA  FOR  JAVA         │  ← 골드 세리프, 42px
│                                │
│   ┌──────────────────────┐   │
│   │  ID                  │   │
│   └──────────────────────┘   │
│   ┌──────────────────────┐   │
│   │  PASSWORD            │   │
│   └──────────────────────┘   │
│                                │
│   아이디와 비밀번호를 입력해주세요  │  ← 에러 Label (평소 숨김, 빨간 텍스트)
│                                │
│       [    LOGIN    ]          │  ← 골드 보더, 빈 채움, hover X
│                                │
│      회원가입  |  비밀번호 찾기   │  ← 보조 회색, 기능 TODO
│                                │
└────────────────────────────────┘
```

- 창 크기: **520×600** (기존 `LoginSize()`의 510×370은 로고가 들어가기엔 좁음)
- VBox 내부 spacing: 16px
- VBox padding: 40px
- 입력창 너비: 320px, 높이: 36px
- 로그인 버튼 너비: 320px, 높이: 40px
- 로고 이미지: 120×120, 비율 유지

## 4. 파일 구조

```
src/main/
  ├── java/GUI/
  │     ├── LoginScene.java          (수정)
  │     └── SceneManager.java        (수정 — 창 크기 조정)
  └── resources/
        ├── css/login.css            (신규)
        └── images/logo.png          (신규 — 사용자가 직접 저장 필요)
```

### 이미지 파일에 대한 주의
사용자가 채팅으로 보낸 캐릭터 일러스트는 프로젝트에 자동으로 저장되지 않는다. 구현 단계 진입 전에 **사용자가 직접** `src/main/resources/images/logo.png`로 저장해야 한다. 이미지가 없으면 `ImageView`가 비어있게 표시되지만 에러는 나지 않도록 한다 (방어 코드).

## 5. CSS 설계 (`src/main/resources/css/login.css`)

선택자는 클래스 기반:

| 클래스 | 대상 |
|---|---|
| `.login-root` | 최상위 `VBox` (배경, padding, spacing) |
| `.login-logo` | `ImageView` (크기 조정은 코드에서, CSS는 여백 정도) |
| `.login-title` | 타이틀 `Label` (폰트, 색, letter-spacing) |
| `.login-field` | `TextField`, `PasswordField` 공통 (배경, 보더, 텍스트 색, 포커스 상태) |
| `.login-btn` | 로그인 `Button` (배경 transparent, 골드 보더, 폰트, hover 시 골드 배경/검정 텍스트) |
| `.login-link` | `Hyperlink` (회색, hover 시 골드) |
| `.login-link-sep` | 링크 사이 `|` 구분자 `Label` |
| `.login-error` | 에러 메시지 `Label` (빨간 텍스트 `--accent-red`, 12px) |

JavaFX는 표준 CSS가 아닌 `-fx-` 프리픽스를 쓰므로 모든 속성을 `-fx-background-color`, `-fx-text-fill`, `-fx-border-color` 등의 형태로 작성한다.

## 6. Java 변경 요약

### `LoginScene.java`
- `VBox root`에 `getStyleClass().add("login-root")` 부여
- `ImageView logo = new ImageView(...)` 추가, 클래스 `login-logo`
  - 이미지 로드는 `getClass().getResource("/images/logo.png")` 사용
  - null 체크: 이미지가 없으면 `ImageView`를 children에 추가하지 않음 (placeholder 두지 않음 — 로고가 없으면 그냥 타이틀부터 시작)
- `Label title = new Label("MAFIA  FOR  JAVA")`, 클래스 `login-title`
- `TextField id`, `PasswordField pw` 각각에 prompt text("ID", "PASSWORD") 설정, 클래스 `login-field`
- `Label errorLabel = new Label()`, 클래스 `login-error`
  - 초기 상태: `setVisible(false)` (자리는 유지, 보이지만 않음 — 에러 등장 시 버튼이 흔들리지 않게)
  - 에러 시: `setText(...)` + `setVisible(true)`
  - 높이는 CSS에서 고정 (`-fx-min-height` 등으로 한 줄 높이 확보)
- `Button loginBtn`에 클래스 `login-btn`
- `Hyperlink signupLink = new Hyperlink("회원가입")`, `Hyperlink findLink = new Hyperlink("비밀번호 찾기")`, 사이에 `Label sep = new Label("|")`
  - `HBox linkRow`로 묶어서 가로 배치
  - 두 링크의 `setOnAction`은 비워둠 (구현 시 `// TODO: 회원가입 화면 연결`, `// TODO: 비밀번호 찾기 흐름`)
- `Scene`은 `LoginScene`이 직접 생성하지 않으므로, CSS 연결은 `SceneManager.showLogin()`에서 처리

### `SceneManager.java`
- `showLogin()`에서 생성하는 `Scene`에 `scene.getStylesheets().add(getClass().getResource("/css/login.css").toExternalForm())` 추가
- `LoginSize()`를 520×600으로 변경 + `setWidth/setHeight`도 함께 호출 (현재는 min만 설정)

## 7. 동작

- **로그인 버튼 클릭**:
  - ID 또는 PW 입력값이 비어있으면 → `errorLabel`에 "아이디와 비밀번호를 입력해주세요" 표시
  - 둘 다 채워져 있으면 → 기존처럼 `SceneManager.baseSize() + SceneManager.showLobby()` (DAO 검증은 여전히 TODO)
  - 추후 DAO 연결 시 "아이디 또는 비밀번호가 일치하지 않습니다" 메시지를 같은 `errorLabel`로 표시할 수 있도록 자리만 마련
- **입력창 변경 이벤트**: 입력이 시작되면 `errorLabel`을 다시 숨김 (사용자가 다시 입력 중일 때 에러 메시지가 거슬리지 않게)
- **회원가입/비번찾기 링크 클릭**: 동작 없음, 코드에 TODO 주석

## 8. 스코프 (이번 작업이 하지 않는 것)
- DAO 연동 및 실제 로그인 검증 ("아이디 또는 비밀번호가 일치하지 않습니다" 메시지의 실제 트리거는 DAO 붙을 때)
- 회원가입 화면, 비밀번호 찾기 흐름 (링크 자리만 마련)
- 추가 유효성 검사 (이메일 형식, 길이 제한 등 — 빈칸 검증만)
- 호버 외 애니메이션, 트랜지션
- 다국어 (한국어 텍스트 하드코딩)
- 반응형 (창 크기 고정)

## 9. 성공 기준
- 로그인 화면을 실행했을 때 위 레이아웃대로 표시된다
- 다크 누아르 컬러 토큰이 적용되고, 로고 이미지가 원본 색상 그대로 노출된다 (이미지 파일이 있는 경우)
- 이미지 파일이 없어도 앱이 크래시하지 않는다
- 회원가입/비밀번호 찾기 링크가 표시되고, 클릭해도 화면 변화가 없다
- ID 또는 PW가 비어있는 상태로 로그인 버튼을 누르면 빨간 에러 메시지가 표시되고, 입력을 시작하면 메시지가 사라진다
- 둘 다 채워진 상태로 로그인 버튼을 누르면 로비 화면으로 전환된다 (기존 동작)
- 창 크기가 520×600으로 열린다
