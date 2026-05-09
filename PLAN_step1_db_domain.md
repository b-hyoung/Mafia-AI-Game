# 구현 계획 — Step 1: DB + 도메인 모델

> **범위**: 마피아 게임의 가장 기초 — 데이터베이스 + 도메인 클래스 + DAO. 게임 로직/UI/AI는 다음 단계.
>
> **목표**: 이 단계가 끝나면 콘솔에서 "유저 등록/로그인 → 페르소나 조회 → 게임 INSERT/UPDATE → 통계 쿼리"가 동작.
>
> **참고**: `C:\Users\ACE\Desktop\pos\src` (POS 프로젝트) — DAO 싱글톤, DBConnect 패턴 그대로 차용. README.md 9장(DB 스키마) + 6장(패키지 구조).
>
> **스타일**: 본인이 직접 본문 코딩. 이 문서는 파일별 시그니처 + 책임 + 참고만 제공.

---

## Section 0. 프로젝트 셋업

- [ ] Eclipse/IntelliJ에서 새 프로젝트 생성 (이름: `mafia_ai_game`)
- [ ] 빌드 도구 결정 — Maven 추천 (POS는 추측컨대 일반 Java 프로젝트지만, 의존성 많아질 거라 Maven 권장)
- [ ] `pom.xml`에 의존성 추가:
  - `mysql-connector-j` (8.x)
  - `com.fasterxml.jackson.core:jackson-databind`
  - `org.openjfx:javafx-controls` (다음 단계용, 미리 추가해도 OK)
  - `org.junit.jupiter:junit-jupiter` (테스트, 선택)
- [ ] 패키지 빈 폴더 생성:
  ```
  src/main/java/
  ├── GUI/             (다음 단계용)
  └── mafia/
      ├── domain/
      ├── db/
      ├── ai/          (다음 단계용)
      ├── server/      (다음 단계용)
      ├── client/      (다음 단계용)
      └── protocol/    (다음 단계용)
  ```

---

## Section 1. DB 생성

- [ ] MySQL 서버 실행 확인 (XAMPP 또는 단독 설치)
- [ ] DB 생성 SQL 실행:
  ```sql
  CREATE DATABASE mafia_game CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  ```
- [ ] README 9.2의 DDL 7개 테이블 모두 실행:
  - `users`
  - `personas`
  - `games`
  - `game_participants`
  - `game_events` (선택, 시간 남으면)
  - `ai_decisions` ★ AI LLM 호출 로그 (디버깅용)
  - `game_history` ★ 게임 행동 + 근거 시간순 (분석/리플레이용)
- [ ] README 9.4의 페르소나 시드 데이터 INSERT (8명) — `system_prompt_template` 컬럼은 일단 한 줄로 간단히 채우고 나중에 튜닝

**검증**: MySQL Workbench/phpMyAdmin에서 `SELECT * FROM personas;` 했을 때 8행 보이면 OK.

---

## Section 2. DBConnect

**파일**: `src/main/java/mafia/db/DBConnect.java`

**책임**: MySQL 커넥션 생성. **매번 새 커넥션 반환** (POS의 정적 conn 변수 제거).

**시그니처**:
```java
public class DBConnect {
    public static final String dbDriver = "com.mysql.cj.jdbc.Driver";
    public static final String URL = "jdbc:mysql://localhost:3306/mafia_game?useUnicode=true&characterEncoding=utf8";
    public static final String DBID = "root";
    public static final String DBPWD = "1234";  // 본인 환경에 맞게
    
    static { /* Class.forName(dbDriver) 1회 로드 */ }
    
    public static Connection connect() throws SQLException;
    // close() 메서드 없음. 호출자가 try-with-resources로 알아서 닫음
}
```

**참고**: POS의 `pos/DBConnect.java` 거의 그대로. 차이점만:
1. `public static Connection conn` 정적 변수 **제거**
2. `connect()`가 매번 `DriverManager.getConnection()` 호출 후 새 conn 반환
3. `close()` 메서드 **제거** (호출자 책임)
4. `Class.forName`은 static initializer로 1회만

- [ ] DBConnect.java 작성
- [ ] 임시 main 메서드로 연결 테스트 (`DBConnect.connect()` 한 번 호출 후 println)

---

## Section 3. 도메인 클래스 (POS의 Book.java 스타일)

각 클래스는 POS의 `Book.java`처럼 필드 + 생성자 + getter/setter만 있는 단순 POJO.

### 3.1. `mafia/domain/User.java`
**책임**: 유저 계정 1명.
**필드**: `id`, `username`, `passwordHash`, `createdAt`(LocalDateTime).

### 3.2. `mafia/domain/Persona.java`
**책임**: AI 페르소나 1개.
**필드**: `id`, `name`, `speechStyle`, `attitude`, `systemPromptTemplate`, `avatarPath`, `active`(boolean), `createdAt`.

### 3.3. `mafia/domain/Role.java` (enum)
**책임**: 역할 + 진영 정보.
```java
public enum Role {
    MAFIA(Faction.MAFIA),
    CITIZEN(Faction.CITIZEN),
    POLICE(Faction.CITIZEN),
    DOCTOR(Faction.CITIZEN),
    INVESTIGATOR(Faction.CITIZEN),
    IMPOSTOR(Faction.CITIZEN),    // 정병
    KILLER(Faction.KILLER);       // 중립 살인자
    
    private final Faction faction;
    Role(Faction f) { this.faction = f; }
    public Faction getFaction() { return faction; }
    public String koreanName() { /* MAFIA → "마피아", ... */ }
}

public enum Faction { MAFIA, CITIZEN, KILLER }
```
**참고**: 이 단계에선 능력 메서드는 미포함. 게임 로직 단계에서 추가.

### 3.4. `mafia/domain/Player.java`
**책임**: 게임 1판 안의 참가자 1명.
**필드**: `slot`(1-6), `participantType`("USER"/"AI"), `userId`(nullable), `personaId`(nullable), `actualRole`, `perceivedRole`, `faction`, `alive`(boolean), `deathDay`(nullable), `deathCause`(nullable).
**참고**: 게임 진행 중 상태는 여기 누적. DB의 `game_participants` 테이블과 1:1.

### 3.5. `mafia/domain/Game.java`
**책임**: 게임 1판 메타데이터.
**필드**: `id`, `hostUserId`, `startedAt`, `endedAt`(nullable), `durationSec`(nullable), `winningFaction`(nullable), `totalDays`(nullable), `participants`(List<Player>).

### 3.6. `mafia/domain/Decision.java`
**책임**: AI LLM 호출 로그 1건. `ai_decisions` 테이블과 1:1.
**필드**:
- `id` (long)
- `gameId`, `participantId`, `dayNumber`, `phase`
- `decisionType` (enum 또는 String — `SPEAK_INTENT` / `SPEECH` / `NIGHT_ACTION` / `VOTE_1` / `VOTE_2` / `DEFENSE`)
- `systemPrompt`, `userPrompt`, `rawResponse`, `parsedDecision` (모두 String)
- `modelUsed` (String), `durationMs` (int)
- `decidedAt` (LocalDateTime)
**참고**: 단순 POJO. AI 호출 시 자동 생성/저장 (다음 단계 `DecisionLogger`에서 사용).

### 3.7. `mafia/domain/HistoryEntry.java` ★ 행동 히스토리
**책임**: 게임 행동 1건 (AI/유저 공통). `game_history` 테이블과 1:1.
**필드**:
- `id` (long)
- `gameId`, `participantId`, `dayNumber`, `phase`
- `actionType` (enum 또는 String — `SPEAK` / `VOTE_1` / `VOTE_2` / `MAFIA_KILL` / `POLICE_BLOCK` / `POLICE_SHOOT` / `DOCTOR_HEAL` / `INVESTIGATE` / `KILLER_ATTACK` / `IMPOSTOR_FAKE`)
- `actionData` (String — JSON)
- `rationale` (String — AI 근거. 유저면 null)
- `result` (String — JSON. 밤 행동 결과 등)
- `occurredAt` (LocalDateTime)
**참고**: 게임 진행 도중 매 행동마다 INSERT. 일차별 분석/리플레이의 핵심 데이터.

### 3.8. (생략) NightAction, Vote
**다음 단계** (게임 로직 시작할 때) 작성. 지금은 안 만들어도 됨.

---

- [ ] User.java
- [ ] Persona.java
- [ ] Role enum + Faction enum
- [ ] Player.java
- [ ] Game.java
- [ ] Decision.java
- [ ] HistoryEntry.java

**검증**: 모두 컴파일되면 OK. 별도 테스트 불필요 (POJO는 검증할 게 없음).

---

## Section 4. DAO 클래스 (POS의 BookDAO 싱글톤 스타일)

각 DAO는 POS의 `BookDAO.java`처럼:
- `private constructor`
- `private static instance = new XxxDAO()`
- `public static XxxDAO getInstance()`
- 메서드마다 `try (Connection conn = DBConnect.connect(); PreparedStatement pstmt = ...)` 형태 (try-with-resources)

### 4.1. `mafia/db/UserDAO.java`

**메서드**:
| 메서드 | 책임 | SQL 힌트 |
|---|---|---|
| `User register(String username, String passwordHash)` | 유저 등록. 성공하면 새 User 객체(id 채워진), 실패면 null | `INSERT INTO users(username, password_hash) VALUES(?, ?)`. id는 `getGeneratedKeys()`로 회수 |
| `User login(String username, String passwordHash)` | 로그인 시도. 매치하면 User, 아니면 null | `SELECT id, username, created_at FROM users WHERE username=? AND password_hash=?` |
| `boolean usernameExists(String username)` | 중복 체크 | `SELECT COUNT(*) FROM users WHERE username=?` |
| `User findById(int id)` | id로 조회 | `SELECT ... WHERE id=?` |

**참고**: passwordHash는 SHA-256 해시 (해시 함수는 별도 유틸 클래스 — 다음 단계에서 만듦. 이 단계에선 그냥 평문 또는 임시 해시).

### 4.2. `mafia/db/PersonaDAO.java`

**메서드**:
| 메서드 | 책임 | SQL 힌트 |
|---|---|---|
| `List<Persona> getAllActive()` | 활성 페르소나 전체 | `SELECT * FROM personas WHERE active=1` |
| `List<Persona> pickRandom(int count)` | 랜덤 N명 | `SELECT * FROM personas WHERE active=1 ORDER BY RAND() LIMIT ?` |
| `Persona findById(int id)` | id로 조회 | `SELECT * FROM personas WHERE id=?` |
| `boolean addPersona(Persona p)` | 추가 (관리 화면용) | `INSERT INTO personas(...)` |
| `boolean updatePersona(Persona p)` | 수정 | `UPDATE personas SET ... WHERE id=?` |
| `boolean softDelete(int id)` | 비활성화 | `UPDATE personas SET active=0 WHERE id=?` |

### 4.3. `mafia/db/GameDAO.java`

**메서드**:
| 메서드 | 책임 | SQL 힌트 |
|---|---|---|
| `int startGame(Integer hostUserId)` | 새 게임 INSERT, id 반환 | `INSERT INTO games(host_user_id, started_at) VALUES(?, NOW())` + `getGeneratedKeys()` |
| `boolean endGame(int gameId, Faction winning, int durationSec, int totalDays)` | 게임 종료 처리 | `UPDATE games SET ended_at=NOW(), winning_faction=?, duration_sec=?, total_days=? WHERE id=?` |
| `Game findById(int gameId)` | 단건 조회 | `SELECT * FROM games WHERE id=?` |
| `List<Game> getRecentGames(int limit)` | 최근 게임 목록 (통계용) | `SELECT * FROM games WHERE ended_at IS NOT NULL ORDER BY started_at DESC LIMIT ?` |

### 4.4. `mafia/db/ParticipantDAO.java`

**메서드**:
| 메서드 | 책임 | SQL 힌트 |
|---|---|---|
| `boolean saveAll(int gameId, List<Player> participants)` | 6명 일괄 INSERT | 반복 INSERT 또는 batch (`addBatch()` / `executeBatch()`) |
| `boolean updateDeath(int gameId, int slot, int day, String cause)` | 사망 처리 | `UPDATE game_participants SET alive_at_end=0, death_day=?, death_cause=? WHERE game_id=? AND slot=?` |
| `List<Player> getByGameId(int gameId)` | 게임 1판의 참가자 6명 | `SELECT * FROM game_participants WHERE game_id=? ORDER BY slot` |

### 4.5. `mafia/db/StatsDAO.java`

**메서드**:
| 메서드 | 책임 | SQL 힌트 |
|---|---|---|
| `Map<Faction, FactionStat> getUserFactionStats(int userId)` | 유저 진영별 승률 | README 9.3 "유저 진영별 승률" |
| `Map<Faction, Double> getGlobalFactionStats()` | 진영별 글로벌 승률 | README 9.3 "글로벌 진영 승률" |
| `List<PersonaStat> getPersonaOverallStats()` | **페르소나 전체 승률** (직업 무관) | README 9.3 "페르소나 전체 승률" |
| `List<PersonaFactionStat> getPersonaFactionStats()` | **페르소나 X 진영별 승률** | README 9.3 "페르소나 X 진영별 승률" |
| `List<PersonaRoleStat> getPersonaRoleStats()` | **페르소나 X 역할별 승률** | README 9.3 "페르소나 X 역할별 승률" |

**보조 클래스** (단순 POJO, 모두 `mafia/db/`):
- `FactionStat.java` — `Faction faction`, `int games`, `int wins`, `double winRate`
- `PersonaStat.java` — `String personaName`, `int games`, `int wins`, `double winRate`
- `PersonaFactionStat.java` — `String personaName`, `Faction faction`, `int games`, `int wins`, `double winRate`
- `PersonaRoleStat.java` — `String personaName`, `Role role`, `int games`, `int wins`, `double winRate`

### 4.6. `mafia/db/DecisionDAO.java` ★ AI LLM 호출 로그

**메서드**:
| 메서드 | 책임 | SQL 힌트 |
|---|---|---|
| `boolean save(Decision d)` | AI 판단 1건 저장 | `INSERT INTO ai_decisions(...) VALUES(...)` |
| `List<Decision> getByGameId(int gameId)` | 게임 1판의 모든 판단 (시간순) | `SELECT * FROM ai_decisions WHERE game_id=? ORDER BY decided_at` |
| `List<Decision> getByParticipantId(int participantId)` | 특정 AI의 모든 판단 | `SELECT * FROM ai_decisions WHERE participant_id=? ORDER BY decided_at` |
| `List<Decision> getByPersonaName(String name, String decisionType, int limit)` | 페르소나별 특정 행동 모음 (튜닝용) | `... JOIN game_participants ... JOIN personas ... WHERE p.name=? AND decision_type=? LIMIT ?` |

**참고**: 이 단계에선 `save()`만 동작하면 됨. 분석 메서드는 다음 단계에서 LLM 연동 후 데이터 쌓이면 활용.

### 4.7. `mafia/db/HistoryDAO.java` ★ 행동 히스토리

**메서드**:
| 메서드 | 책임 | SQL 힌트 |
|---|---|---|
| `boolean save(HistoryEntry h)` | 행동 1건 저장 | `INSERT INTO game_history(...) VALUES(...)` |
| `List<HistoryEntry> getByGameAndDay(int gameId, int day)` | 일차별 행동 흐름 | `SELECT * FROM game_history WHERE game_id=? AND day_number=? ORDER BY occurred_at` |
| `List<HistoryEntry> getByParticipant(int gameId, int participantId)` | 특정 AI의 게임 전체 행동 | `SELECT * FROM game_history WHERE game_id=? AND participant_id=? ORDER BY occurred_at` |
| `List<HistoryEntry> getByGameAndAction(int gameId, String actionType)` | 특정 게임의 특정 행동 모음 | `SELECT * FROM game_history WHERE game_id=? AND action_type=? ORDER BY occurred_at` |

**참고**: 이 단계에선 `save()`만 동작하면 됨. 분석 메서드는 게임 진행 후 데이터 쌓이면 활용. README 9.3의 "일차별 게임 흐름 + 근거 정리" 쿼리 참고.

---

- [ ] UserDAO.java + 등록/로그인 콘솔 테스트
- [ ] PersonaDAO.java + `getAllActive()` 콘솔 테스트 (8명 출력 확인)
- [ ] GameDAO.java + `startGame()` 호출해서 DB에 게임 1행 들어가는지 확인
- [ ] ParticipantDAO.java + `saveAll()` 콘솔 테스트 (가짜 6명 데이터)
- [ ] StatsDAO.java + 통계 쿼리 호출 (게임 데이터 0개여도 빈 결과 잘 나오는지)
- [ ] DecisionDAO.java + 가짜 Decision 1건 save 테스트
- [ ] HistoryDAO.java + 가짜 HistoryEntry 1건 save 테스트

---

## Section 5. 검증 main 메서드

**파일**: `src/main/java/mafia/Main.java` (임시, 다음 단계에서 GUI 진입점으로 대체 예정)

**시나리오 (콘솔)**:
1. `UserDAO.register("test", "hash")` — 유저 등록
2. `UserDAO.login("test", "hash")` — 로그인 → User 객체 받기
3. `PersonaDAO.pickRandom(5)` — 5명 랜덤 픽 → 콘솔 출력
4. `GameDAO.startGame(user.getId())` — 게임 시작 → gameId 반환
5. 가짜 Player 6명 만들어서 `ParticipantDAO.saveAll(gameId, players)` — 참가자 저장
6. `GameDAO.endGame(gameId, Faction.CITIZEN, 800, 4)` — 게임 종료
7. `StatsDAO.getUserFactionStats(user.getId())` — 통계 출력 (시민 100% 1전 1승)

- [ ] Main.java 작성
- [ ] 한 번 실행해서 위 7단계 다 동작 확인
- [ ] DB에 직접 들어가 데이터 들어갔는지 눈으로 확인

---

## Section 6. 정리 + 커밋

- [ ] git init (아직 안 했으면)
- [ ] `.gitignore` (target/, *.class, .idea/, .vscode/ 등)
- [ ] 한 번에 커밋: "Step 1: DB schema + domain models + DAO foundation"

---

## 완료 기준 (DoD)

- [ ] DB에 7개 테이블 생성됨 (`users`, `personas`, `games`, `game_participants`, `ai_decisions`, `game_history`, +선택 `game_events`) + 페르소나 8명 시드 들어가 있음
- [ ] 도메인 7개 클래스 컴파일됨 (`User`, `Persona`, `Role`+`Faction` enum, `Player`, `Game`, `Decision`, `HistoryEntry`)
- [ ] DAO 7개 클래스 컴파일됨 + 각각 1개 이상 메서드 콘솔에서 호출 성공
- [ ] Main.java 시나리오 1-7번 모두 에러 없이 실행됨
- [ ] DB에 게임/참가자/판단/히스토리 로그 데이터 정상 저장됨

---

## 다음 단계 예고 (Step 2)

이 단계 끝나면 다음은 **Ollama 연동 (LLMClient + OllamaClient)**. HTTP로 LLM 호출하고 응답 받기. 그 다음 게임 로직 → UI → 멀티 순.

---

## 막힐 때 자주 보는 곳

| 질문 | 어디 보기 |
|---|---|
| DAO 패턴 어떻게? | `pos/BookDAO.java` |
| DBConnect 어떻게? | `pos/DBConnect.java` (정적 conn 빼고) |
| try-with-resources 문법? | Java 공식 doc 또는 stackoverflow "java try with resources jdbc" |
| `getGeneratedKeys()` 사용법? | `Statement.RETURN_GENERATED_KEYS` 옵션 + ResultSet 받기 |
| MySQL 한글 깨짐? | URL에 `useUnicode=true&characterEncoding=utf8` 확인 |
| 타임스탬프 다루기? | `java.sql.Timestamp` ↔ `LocalDateTime` 변환 |
