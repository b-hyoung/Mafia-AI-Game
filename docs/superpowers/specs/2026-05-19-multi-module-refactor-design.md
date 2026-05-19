# Multi-Module Refactor Design

작성일: 2026-05-19
대상: 현재 단일 Maven 프로젝트를 parent + 3 module 구조로 분리

## 1. 컨셉

현재 모든 코드(클라이언트 UI + DB + 도메인)가 한 Maven 프로젝트에 섞여있다. 곧 들어올 **TCP 서버 코드**를 깔끔히 분리하기 위해 **지금** parent + 3 module 구조로 리팩토링한다.

이렇게 미리 깔아두면:
- 서버는 JavaFX/MySQL 의존성 없음 (가벼움, 헤드리스 환경에서 실행)
- 클라이언트는 서버 내부 코드 의존성 X (배포 jar에 서버 코드 안 들어감)
- 도메인/메시지 모델은 양쪽 공유 (`mafia-common`)
- 미래 별도 git repo로 분리도 큰 작업 없이 가능

## 2. 모듈 구조

```
Mafia_For_Java/                       (parent — pom only)
├── pom.xml                           (<packaging>pom</packaging>, 3 modules)
├── docs/                             (그대로, parent 레벨)
├── README.md, PLAN_step1_db_domain.md (그대로)
│
├── mafia-common/                     (양쪽 공유)
│   ├── pom.xml                       (의존성 거의 없음 — 향후 Jackson)
│   └── src/main/java/mafia/
│       └── domain/
│           ├── Room.java             (이동: 기존 위치에서)
│           └── RoomState.java        (이동: 기존 위치에서)
│
├── mafia-server/                     (서버 전용)
│   ├── pom.xml                       (의존: mafia-common)
│   └── src/main/java/mafia/server/
│       └── .gitkeep                  (Phase 1 TCP에서 채움 — 이번 스코프 X)
│
└── mafia-client/                     (클라이언트 + UI + DB)
    ├── pom.xml                       (의존: mafia-common, javafx-controls, javafx-fxml, javafx-media, mysql-connector-j, jbcrypt)
    └── src/main/
        ├── java/
        │   ├── GUI/                  (이동: 모든 Scene + components)
        │   └── mafia/
        │       ├── db/               (이동: DBConnect, UserDAO)
        │       └── client/           (Phase 1에서 SocketClient — 이번 스코프 X)
        └── resources/
            ├── css/                  (이동: tokens.css, login.css, lobby.css, result-box.css)
            ├── images/               (이동: logo.png)
            └── videos/               (이동: logo.mp4)
```

## 3. 결정 사항

### 3.1 도메인 위치 — `mafia-common`
`Room`, `RoomState` 등 도메인 클래스는 양쪽이 필요 (서버가 만들어 클라이언트에 전송, 클라이언트가 받아 표시).
- 다만 `Room`이 현재 JavaFX `IntegerProperty`/`ObjectProperty`를 갖고 있음 → `mafia-common`이 JavaFX에 의존하게 됨.
- 단기는 그대로 (JavaFX 의존성 추가, 사용자 학습용엔 큰 부담 X).
- 장기 정리 시 도메인은 순수 POJO로, UI 어댑터를 별도로 두는 게 깔끔. 다만 이번 리팩토링 범위 X.

### 3.2 DB 위치 — `mafia-client` (단기)
현재 클라이언트가 직접 DB 호출 중. 그대로 `mafia-client`에 둠.
- 진짜 멀티 PC 배포 시점엔 서버가 DB 게이트웨이가 되어야 (보안). 그때 `mafia-server`로 이동.
- 그건 Phase 2 (TCP 로비 동기화 + 인증)에서.

### 3.3 AI 부분은 별도 (미래)
원래 design.md에 `bot/` 폴더 있었지만 Java 봇이었음. 새 결정:
- AI는 **Python 마이크로서비스**로 별도 repo/폴더.
- Java 게임 서버가 HTTP로 Python AI 서비스 호출.
- 이번 multi-module 구조엔 `mafia-bot` 같은 module 만들지 X.
- AI 도입 시점에 별도 spec/repo로 진행.

### 3.4 docs 위치 — parent 레벨
`docs/`, `README.md`, `PLAN_step1_db_domain.md`는 parent 레벨에 그대로. 모든 모듈이 참조.

### 3.5 빌드 도구 — Maven Multi-Module
- parent `pom.xml`에 `<packaging>pom</packaging>` + 3 `<module>` 명시
- 공통 의존성 버전은 parent의 `<dependencyManagement>` 또는 `<properties>`로 관리
- `javafx-maven-plugin`은 `mafia-client/pom.xml`로 이동
- 각 module은 부모를 참조 (`<parent>` 블록)

## 4. 빌드/실행 명령

| 명령 | 위치 | 결과 |
|---|---|---|
| `mvn install` | 프로젝트 루트 | 전체 빌드 (3 module 순서대로) |
| `mvn -pl mafia-client javafx:run` | 프로젝트 루트 | 클라이언트 실행 (게임) |
| `mvn -pl mafia-server exec:java` | 프로젝트 루트 | 서버 실행 (Phase 1 후) |
| `mvn -pl mafia-common install` | 프로젝트 루트 | mafia-common만 빌드 |

IDE (IntelliJ) 사용 시:
- parent `pom.xml` 열면 자동으로 multi-module로 인식
- 각 module별 Run Configuration 자동 생성
- `MainGame` (mafia-client) Run → 게임 실행
- `MafiaServer` (mafia-server) Run → 서버 실행 (Phase 1 후)

## 5. 마이그레이션 작업

### 5.1 단계
1. **parent `pom.xml`** 작성 (기존 의존성을 dependencyManagement로 분리)
2. **3 module 폴더 + 각 pom.xml** 작성
3. **기존 코드 이동** — `git mv` 사용 (history 보존)
   - `src/main/java/GUI/` → `mafia-client/src/main/java/GUI/`
   - `src/main/java/mafia/domain/` → `mafia-common/src/main/java/mafia/domain/`
   - `src/main/java/mafia/db/` → `mafia-client/src/main/java/mafia/db/`
   - `src/main/resources/` → `mafia-client/src/main/resources/`
4. **루트의 `src/`, `target/` 제거** (또는 git ignore — parent엔 코드 없음)
5. **검증**: `mvn install` 성공 + IDE에서 게임 실행 → 회원가입/로그인/로비/방 만들기 모두 정상

### 5.2 IDE 재인덱싱
- IntelliJ: `File > Invalidate Caches > Invalidate and Restart` 또는 Maven 패널의 Reload
- 모듈 인식 후 Run Configuration 새로 생성

### 5.3 위험 요소
- pom.xml에서 의존성 누락 시 빌드 실패. 특히 JavaFX의 module-path 설정.
- 리소스 경로 (`/css/login.css` 등)는 그대로 동작 (`mafia-client/src/main/resources` 가 classpath의 루트).
- `getResource()` 호출은 코드 변경 없음.

## 6. 미래 확장 (이 spec 범위 X)

| 시점 | 작업 |
|---|---|
| 다음 sub-project | mafia-server에 TCP Phase 1 (echo) |
| Phase 2 (로비 동기화) | DB를 `mafia-server`로 이동 (`mafia.db` → server module) |
| AI 도입 (Phase 5+) | Python AI 서비스를 **별도 repo**로 구축. Java 서버가 HTTP 호출. |
| 운영 배포 | mafia-server를 별도 git repo로 분리 (옵션) |

## 7. 스코프 (이번 작업이 안 하는 것)

- TCP 코드 작성 — 다음 sub-project
- DB 서버로 이동 — Phase 2
- Python AI 서비스 — 별도 repo + 미래
- `User` 도메인 클래스 추가
- `Room` 도메인의 JavaFX 의존성 정리
- 별도 git repo로 완전 분리

## 8. 성공 기준

- `mvn install` 전체 빌드 성공 (3 module 모두)
- `mvn -pl mafia-client javafx:run` 으로 기존과 똑같이 게임 실행 가능
  - 회원가입 → 로그인 → 로비 → 방 만들기 → 대기실 모두 정상
- IDE (IntelliJ)에서 multi-module 인식 후 `MainGame` Run으로 게임 실행
- `mafia-server` 모듈이 비어있어도 빌드 통과
- `git log --follow` 로 이동된 파일들의 변경 history 추적 가능
- working tree clean, 미커밋 변경 없음
