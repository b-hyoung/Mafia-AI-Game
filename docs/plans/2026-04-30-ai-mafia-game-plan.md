# AI 마피아 게임 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Java Swing + TCP 소켓 + MySQL + OpenAI Function Calling 기반 5인 멀티플레이어 AI 마피아 게임 구현

**Architecture:** 서버가 게임 로직/턴 관리 담당, 클라이언트(사람/봇)는 TCP 소켓으로 동일하게 접속. 봇은 OpenAI Function Calling으로 상황 판단 후 함수 호출. MySQL에 유저/전적/게임로그/AI로그 저장.

**Tech Stack:** Java 17+, Gradle, JUnit 5, Gson, MySQL (JDBC), OpenAI API (HTTP), Java Swing

---

## 파일 구조

```
Mafia_For_Java/
├── build.gradle
├── settings.gradle
├── src/
│   ├── main/java/mafia/
│   │   ├── common/
│   │   │   ├── Role.java                 -- 역할 enum (MAFIA, CITIZEN, POLICE, PSYCHO)
│   │   │   ├── GamePhase.java            -- 페이즈 enum (DAY_DISCUSSION, DAY_VOTE, NIGHT)
│   │   │   ├── Message.java              -- JSON 메시지 객체 (type, sender, action, target, message 등)
│   │   │   └── GameResult.java           -- 게임 결과 객체 (승리팀, 참가자별 역할/생존여부)
│   │   ├── server/
│   │   │   ├── MafiaServer.java          -- TCP 서버, ServerSocket으로 클라이언트 접속 수락
│   │   │   ├── ClientHandler.java        -- 클라이언트별 스레드, 메시지 수신/발신
│   │   │   ├── GameRoom.java             -- 게임 진행 (페이즈 전환, 승리 판정, 턴 루프)
│   │   │   ├── RoleAssigner.java         -- 5명에게 역할 랜덤 배정
│   │   │   ├── VoteManager.java          -- 투표 수집 및 과반수 집계
│   │   │   └── NightActionHandler.java   -- 밤 행동 처리 (킬, 조사, 정병 랜덤결과)
│   │   ├── client/
│   │   │   ├── MafiaClient.java          -- TCP 소켓 연결, 메시지 송수신 스레드
│   │   │   ├── GameUI.java               -- Swing 메인 프레임 (ChatPanel + VotePanel 조합)
│   │   │   ├── ChatPanel.java            -- 채팅 표시 + 입력 패널
│   │   │   ├── VotePanel.java            -- 생존자 투표 버튼 패널
│   │   │   └── ResultPanel.java          -- 게임 종료 후 결과/리플레이 화면
│   │   ├── bot/
│   │   │   ├── BotClient.java            -- 봇용 TCP 클라이언트 (소켓 접속 + 메시지 루프)
│   │   │   ├── AgentInterface.java       -- 에이전트 인터페이스 (API 교체 대비)
│   │   │   ├── OpenAIAgent.java          -- OpenAI Function Calling 구현
│   │   │   └── FunctionRegistry.java     -- 함수 정의 목록 관리 (발언, 투표, 킬지목, 조사)
│   │   └── db/
│   │       ├── DBConnection.java         -- MySQL 커넥션 관리
│   │       ├── UserDAO.java              -- users 테이블 CRUD
│   │       ├── GameDAO.java              -- games, game_players 테이블 저장/조회
│   │       ├── GameLogDAO.java           -- game_logs 테이블 저장/조회
│   │       └── AILogDAO.java             -- ai_logs 테이블 저장/조회
│   └── test/java/mafia/
│       ├── common/
│       │   └── MessageTest.java
│       ├── server/
│       │   ├── RoleAssignerTest.java
│       │   ├── VoteManagerTest.java
│       │   ├── NightActionHandlerTest.java
│       │   └── GameRoomTest.java
│       └── bot/
│           └── FunctionRegistryTest.java
└── docs/
    ├── specs/
    │   └── 2026-04-30-ai-mafia-game-design.md
    └── plans/
        └── 2026-04-30-ai-mafia-game-plan.md (이 파일)
```

---

## Task 1: 프로젝트 초기 세팅 (Gradle + 의존성)

**Files:**
- Create: `build.gradle`
- Create: `settings.gradle`

- [ ] **Step 1: settings.gradle 생성**

```groovy
rootProject.name = 'Mafia_For_Java'
```

- [ ] **Step 2: build.gradle 생성**

```groovy
plugins {
    id 'java'
    id 'application'
}

group = 'mafia'
version = '1.0'
sourceCompatibility = '17'

repositories {
    mavenCentral()
}

dependencies {
    implementation 'com.google.code.gson:gson:2.11.0'
    implementation 'mysql:mysql-connector-java:8.0.33'

    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
}

test {
    useJUnitPlatform()
}

application {
    mainClass = 'mafia.server.MafiaServer'
}
```

- [ ] **Step 3: 디렉토리 구조 생성**

```bash
mkdir -p src/main/java/mafia/{common,server,client,bot,db}
mkdir -p src/test/java/mafia/{common,server,bot}
```

- [ ] **Step 4: 빌드 확인**

```bash
cd Mafia_For_Java && ./gradlew build
```

Expected: BUILD SUCCESSFUL (소스 없어도 빌드 자체는 성공)

- [ ] **Step 5: 커밋**

```bash
git init
git add .
git commit -m "chore: 프로젝트 초기 세팅 (Gradle, 의존성)"
```

---

## Task 2: common 패키지 — Role, GamePhase, Message

**Files:**
- Create: `src/main/java/mafia/common/Role.java`
- Create: `src/main/java/mafia/common/GamePhase.java`
- Create: `src/main/java/mafia/common/Message.java`
- Create: `src/test/java/mafia/common/MessageTest.java`

- [ ] **Step 1: Role enum 작성**

```java
package mafia.common;

public enum Role {
    MAFIA, CITIZEN, POLICE, PSYCHO;

    // 정병에게 보여줄 가짜 역할
    public Role getDisplayRole() {
        if (this == PSYCHO) return POLICE;
        return this;
    }

    public boolean isMafia() {
        return this == MAFIA;
    }

    // 시민 팀인지 (시민, 경찰, 정병 모두 시민 팀)
    public boolean isCitizenTeam() {
        return this != MAFIA;
    }
}
```

- [ ] **Step 2: GamePhase enum 작성**

```java
package mafia.common;

public enum GamePhase {
    DAY_DISCUSSION,
    DAY_VOTE,
    NIGHT,
    GAME_OVER
}
```

- [ ] **Step 3: Message 테스트 작성**

```java
package mafia.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    void chatMessage_직렬화_역직렬화() {
        Message msg = Message.chat("P1", "P3 수상해요");
        String json = msg.toJson();
        Message parsed = Message.fromJson(json);

        assertEquals("CHAT", parsed.getType());
        assertEquals("P1", parsed.getSender());
        assertEquals("P3 수상해요", parsed.getMessage());
    }

    @Test
    void actionMessage_투표() {
        Message msg = Message.action("P2", "투표", "P3");
        String json = msg.toJson();
        Message parsed = Message.fromJson(json);

        assertEquals("ACTION", parsed.getType());
        assertEquals("P2", parsed.getSender());
        assertEquals("투표", parsed.getAction());
        assertEquals("P3", parsed.getTarget());
    }

    @Test
    void phaseChange_메시지() {
        Message msg = Message.phaseChange("DAY_DISCUSSION", new String[]{"P1", "P2", "P3", "P4", "P5"});
        String json = msg.toJson();
        Message parsed = Message.fromJson(json);

        assertEquals("PHASE_CHANGE", parsed.getType());
        assertEquals("DAY_DISCUSSION", parsed.getPhase());
        assertEquals(5, parsed.getSurvivors().length);
    }

    @Test
    void nightResult_메시지() {
        Message msg = Message.nightResult("P2");
        String json = msg.toJson();
        Message parsed = Message.fromJson(json);

        assertEquals("NIGHT_RESULT", parsed.getType());
        assertEquals("P2", parsed.getDead());
    }
}
```

- [ ] **Step 4: 테스트 실행 → 실패 확인**

```bash
./gradlew test
```

Expected: FAIL — Message 클래스가 없으므로 컴파일 에러

- [ ] **Step 5: Message 클래스 구현**

```java
package mafia.common;

import com.google.gson.Gson;

public class Message {
    private static final Gson gson = new Gson();

    private String type;
    private String sender;
    private String message;
    private String action;
    private String target;
    private String phase;
    private String[] survivors;
    private String dead;

    // --- 팩토리 메서드 ---

    public static Message chat(String sender, String message) {
        Message msg = new Message();
        msg.type = "CHAT";
        msg.sender = sender;
        msg.message = message;
        return msg;
    }

    public static Message action(String sender, String action, String target) {
        Message msg = new Message();
        msg.type = "ACTION";
        msg.sender = sender;
        msg.action = action;
        msg.target = target;
        return msg;
    }

    public static Message phaseChange(String phase, String[] survivors) {
        Message msg = new Message();
        msg.type = "PHASE_CHANGE";
        msg.phase = phase;
        msg.survivors = survivors;
        return msg;
    }

    public static Message nightResult(String dead) {
        Message msg = new Message();
        msg.type = "NIGHT_RESULT";
        msg.dead = dead;
        return msg;
    }

    // --- 직렬화 ---

    public String toJson() {
        return gson.toJson(this);
    }

    public static Message fromJson(String json) {
        return gson.fromJson(json, Message.class);
    }

    // --- Getter ---

    public String getType() { return type; }
    public String getSender() { return sender; }
    public String getMessage() { return message; }
    public String getAction() { return action; }
    public String getTarget() { return target; }
    public String getPhase() { return phase; }
    public String[] getSurvivors() { return survivors; }
    public String getDead() { return dead; }
}
```

- [ ] **Step 6: 테스트 실행 → 통과 확인**

```bash
./gradlew test
```

Expected: BUILD SUCCESSFUL, 4 tests passed

- [ ] **Step 7: 커밋**

```bash
git add src/
git commit -m "feat: common 패키지 — Role, GamePhase, Message 구현"
```

---

## Task 3: 서버 — 역할 배정 (RoleAssigner)

**Files:**
- Create: `src/main/java/mafia/server/RoleAssigner.java`
- Create: `src/test/java/mafia/server/RoleAssignerTest.java`

- [ ] **Step 1: 테스트 작성**

```java
package mafia.server;

import mafia.common.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class RoleAssignerTest {

    @Test
    void 역할배정_5명_정확한_역할수() {
        List<String> players = List.of("P1", "P2", "P3", "P4", "P5");
        Map<String, Role> roles = RoleAssigner.assign(players);

        assertEquals(5, roles.size());

        Map<Role, Long> counts = roles.values().stream()
                .collect(Collectors.groupingBy(r -> r, Collectors.counting()));

        assertEquals(2, counts.getOrDefault(Role.MAFIA, 0L));
        assertEquals(1, counts.getOrDefault(Role.CITIZEN, 0L));
        assertEquals(1, counts.getOrDefault(Role.POLICE, 0L));
        assertEquals(1, counts.getOrDefault(Role.PSYCHO, 0L));
    }

    @RepeatedTest(10)
    void 역할배정_랜덤성_확인() {
        List<String> players = List.of("P1", "P2", "P3", "P4", "P5");
        Map<String, Role> roles1 = RoleAssigner.assign(players);
        Map<String, Role> roles2 = RoleAssigner.assign(players);
        // 10번 반복 중 최소 한 번은 달라야 함 (확률적으로 거의 항상 통과)
        // 이 테스트는 배정 자체가 에러 없이 동작하는지 확인 목적
        assertNotNull(roles1);
        assertNotNull(roles2);
    }
}
```

- [ ] **Step 2: 테스트 실행 → 실패 확인**

```bash
./gradlew test --tests "mafia.server.RoleAssignerTest"
```

Expected: FAIL — RoleAssigner 클래스 없음

- [ ] **Step 3: RoleAssigner 구현**

```java
package mafia.server;

import mafia.common.Role;
import java.util.*;

public class RoleAssigner {

    private static final List<Role> ROLE_POOL = List.of(
            Role.MAFIA, Role.MAFIA,
            Role.CITIZEN,
            Role.POLICE,
            Role.PSYCHO
    );

    public static Map<String, Role> assign(List<String> playerIds) {
        if (playerIds.size() != 5) {
            throw new IllegalArgumentException("플레이어는 정확히 5명이어야 합니다: " + playerIds.size());
        }

        List<Role> shuffled = new ArrayList<>(ROLE_POOL);
        Collections.shuffle(shuffled);

        Map<String, Role> result = new LinkedHashMap<>();
        for (int i = 0; i < playerIds.size(); i++) {
            result.put(playerIds.get(i), shuffled.get(i));
        }
        return result;
    }
}
```

- [ ] **Step 4: 테스트 실행 → 통과 확인**

```bash
./gradlew test --tests "mafia.server.RoleAssignerTest"
```

Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add src/
git commit -m "feat: RoleAssigner — 5명 역할 랜덤 배정"
```

---

## Task 4: 서버 — 투표 집계 (VoteManager)

**Files:**
- Create: `src/main/java/mafia/server/VoteManager.java`
- Create: `src/test/java/mafia/server/VoteManagerTest.java`

- [ ] **Step 1: 테스트 작성**

```java
package mafia.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

class VoteManagerTest {

    private VoteManager voteManager;

    @BeforeEach
    void setUp() {
        voteManager = new VoteManager(5); // 5명 중 생존자 수
    }

    @Test
    void 과반수_투표시_처형대상_반환() {
        voteManager.vote("P1", "P3");
        voteManager.vote("P2", "P3");
        voteManager.vote("P3", "P1");
        voteManager.vote("P4", "P3");
        voteManager.vote("P5", "P1");

        Optional<String> result = voteManager.getResult();
        assertTrue(result.isPresent());
        assertEquals("P3", result.get());  // 3표로 과반수(3표 이상)
    }

    @Test
    void 과반수_미달시_빈결과() {
        voteManager.vote("P1", "P3");
        voteManager.vote("P2", "P3");
        voteManager.vote("P3", "P1");
        voteManager.vote("P4", "P1");
        voteManager.vote("P5", "P2");

        Optional<String> result = voteManager.getResult();
        assertFalse(result.isPresent());  // 최다 2표, 과반수 안됨
    }

    @Test
    void 중복투표시_마지막_투표로_갱신() {
        voteManager.vote("P1", "P3");
        voteManager.vote("P1", "P2");  // P1이 다시 투표

        assertEquals("P2", voteManager.getVoteOf("P1"));
    }

    @Test
    void reset_후_투표_초기화() {
        voteManager.vote("P1", "P3");
        voteManager.reset(4);

        assertNull(voteManager.getVoteOf("P1"));
    }
}
```

- [ ] **Step 2: 테스트 실행 → 실패 확인**

```bash
./gradlew test --tests "mafia.server.VoteManagerTest"
```

Expected: FAIL

- [ ] **Step 3: VoteManager 구현**

```java
package mafia.server;

import java.util.*;

public class VoteManager {

    private int survivorCount;
    private final Map<String, String> votes = new LinkedHashMap<>();  // voter → target

    public VoteManager(int survivorCount) {
        this.survivorCount = survivorCount;
    }

    public void vote(String voter, String target) {
        votes.put(voter, target);
    }

    public String getVoteOf(String voter) {
        return votes.get(voter);
    }

    public boolean allVoted() {
        return votes.size() >= survivorCount;
    }

    public Optional<String> getResult() {
        Map<String, Integer> tally = new HashMap<>();
        for (String target : votes.values()) {
            tally.merge(target, 1, Integer::sum);
        }

        int majority = survivorCount / 2 + 1;

        return tally.entrySet().stream()
                .filter(e -> e.getValue() >= majority)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    public void reset(int newSurvivorCount) {
        this.survivorCount = newSurvivorCount;
        votes.clear();
    }
}
```

- [ ] **Step 4: 테스트 실행 → 통과 확인**

```bash
./gradlew test --tests "mafia.server.VoteManagerTest"
```

Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add src/
git commit -m "feat: VoteManager — 투표 집계 및 과반수 판정"
```

---

## Task 5: 서버 — 밤 행동 처리 (NightActionHandler)

**Files:**
- Create: `src/main/java/mafia/server/NightActionHandler.java`
- Create: `src/test/java/mafia/server/NightActionHandlerTest.java`

- [ ] **Step 1: 테스트 작성**

```java
package mafia.server;

import mafia.common.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

class NightActionHandlerTest {

    private NightActionHandler handler;
    private Map<String, Role> roles;

    @BeforeEach
    void setUp() {
        roles = Map.of(
                "P1", Role.MAFIA,
                "P2", Role.MAFIA,
                "P3", Role.CITIZEN,
                "P4", Role.POLICE,
                "P5", Role.PSYCHO
        );
        handler = new NightActionHandler(roles);
    }

    @Test
    void 마피아_두명_같은대상_킬확정() {
        handler.mafiaKill("P1", "P3");
        handler.mafiaKill("P2", "P3");

        assertEquals("P3", handler.getKillTarget());
    }

    @Test
    void 마피아_두명_다른대상_둘중하나_선택() {
        handler.mafiaKill("P1", "P3");
        handler.mafiaKill("P2", "P4");

        String target = handler.getKillTarget();
        assertTrue(target.equals("P3") || target.equals("P4"));
    }

    @Test
    void 경찰_조사_마피아_true() {
        boolean result = handler.investigate("P4", "P1");  // P4(경찰)이 P1(마피아) 조사
        assertTrue(result);
    }

    @Test
    void 경찰_조사_시민_false() {
        boolean result = handler.investigate("P4", "P3");  // P4(경찰)이 P3(시민) 조사
        assertFalse(result);
    }

    @RepeatedTest(20)
    void 정병_조사_랜덤결과() {
        boolean result = handler.investigate("P5", "P1");  // P5(정병)이 P1(마피아) 조사
        // 랜덤이므로 true/false 둘 다 가능 — 에러 없이 실행되는지만 확인
        // 20회 반복이면 true/false 둘 다 나올 확률이 높음
        assertNotNull(result);
    }

    @Test
    void reset_후_초기화() {
        handler.mafiaKill("P1", "P3");
        handler.reset();

        assertNull(handler.getKillTarget());
    }
}
```

- [ ] **Step 2: 테스트 실행 → 실패 확인**

```bash
./gradlew test --tests "mafia.server.NightActionHandlerTest"
```

Expected: FAIL

- [ ] **Step 3: NightActionHandler 구현**

```java
package mafia.server;

import mafia.common.Role;

import java.util.*;

public class NightActionHandler {

    private final Map<String, Role> roles;
    private final Map<String, String> mafiaVotes = new HashMap<>();  // 마피아ID → 킬대상
    private Random random = new Random();

    public NightActionHandler(Map<String, Role> roles) {
        this.roles = roles;
    }

    public void mafiaKill(String mafiaId, String targetId) {
        mafiaVotes.put(mafiaId, targetId);
    }

    public String getKillTarget() {
        if (mafiaVotes.isEmpty()) return null;

        // 두 마피아가 같은 대상 → 확정
        List<String> targets = new ArrayList<>(mafiaVotes.values());
        if (targets.size() == 2 && targets.get(0).equals(targets.get(1))) {
            return targets.get(0);
        }

        // 다른 대상 → 랜덤 선택
        if (targets.size() == 2) {
            return targets.get(random.nextInt(2));
        }

        // 마피아 1명만 생존한 경우
        return targets.get(0);
    }

    public boolean investigate(String investigatorId, String targetId) {
        Role investigatorRole = roles.get(investigatorId);

        // 정병이면 랜덤 결과 반환
        if (investigatorRole == Role.PSYCHO) {
            return random.nextBoolean();
        }

        // 진짜 경찰이면 실제 결과 반환
        Role targetRole = roles.get(targetId);
        return targetRole == Role.MAFIA;
    }

    public void reset() {
        mafiaVotes.clear();
    }
}
```

- [ ] **Step 4: 테스트 실행 → 통과 확인**

```bash
./gradlew test --tests "mafia.server.NightActionHandlerTest"
```

Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add src/
git commit -m "feat: NightActionHandler — 마피아 킬, 경찰 조사, 정병 랜덤결과"
```

---

## Task 6: DB — 스키마 생성 + 커넥션 관리

**Files:**
- Create: `sql/schema.sql`
- Create: `src/main/java/mafia/db/DBConnection.java`

- [ ] **Step 1: SQL 스키마 파일 작성**

```sql
CREATE DATABASE IF NOT EXISTS mafia_game
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE mafia_game;

CREATE TABLE IF NOT EXISTS users (
    user_id     INT PRIMARY KEY AUTO_INCREMENT,
    nickname    VARCHAR(20) NOT NULL,
    wins        INT DEFAULT 0,
    losses      INT DEFAULT 0,
    is_bot      BOOLEAN DEFAULT FALSE,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS games (
    game_id     INT PRIMARY KEY AUTO_INCREMENT,
    winner_team ENUM('CITIZEN', 'MAFIA') NOT NULL,
    started_at  DATETIME NOT NULL,
    ended_at    DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS game_players (
    game_id     INT NOT NULL,
    user_id     INT NOT NULL,
    role        ENUM('MAFIA', 'CITIZEN', 'POLICE', 'PSYCHO') NOT NULL,
    fake_role   ENUM('MAFIA', 'CITIZEN', 'POLICE') NULL,
    is_survived BOOLEAN NOT NULL,
    PRIMARY KEY (game_id, user_id),
    FOREIGN KEY (game_id) REFERENCES games(game_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS game_logs (
    log_id      INT PRIMARY KEY AUTO_INCREMENT,
    game_id     INT NOT NULL,
    round       INT NOT NULL,
    phase       ENUM('DAY_DISCUSSION', 'DAY_VOTE', 'NIGHT') NOT NULL,
    actor_id    INT NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    target_id   INT NULL,
    message     TEXT NULL,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (game_id) REFERENCES games(game_id),
    FOREIGN KEY (actor_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS ai_logs (
    ai_log_id       INT PRIMARY KEY AUTO_INCREMENT,
    game_id         INT NOT NULL,
    round           INT NOT NULL,
    user_id         INT NOT NULL,
    prompt_sent     TEXT NOT NULL,
    response        TEXT NOT NULL,
    function_called VARCHAR(20) NOT NULL,
    function_args   TEXT NULL,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (game_id) REFERENCES games(game_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);
```

- [ ] **Step 2: DBConnection 구현**

```java
package mafia.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/mafia_game?useSSL=false&serverTimezone=Asia/Seoul";
    private static final String USER = "root";
    private static final String PASSWORD = "";  // 로컬 개발용

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        }
        return connection;
    }

    public static void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
```

- [ ] **Step 3: MySQL에서 스키마 실행하여 테이블 생성 확인**

```bash
mysql -u root < sql/schema.sql
```

Expected: 에러 없이 완료, `mafia_game` 데이터베이스와 5개 테이블 생성

- [ ] **Step 4: 커밋**

```bash
git add sql/ src/main/java/mafia/db/DBConnection.java
git commit -m "feat: DB 스키마 + DBConnection 커넥션 관리"
```

---

## Task 7: DB — DAO 클래스들

**Files:**
- Create: `src/main/java/mafia/db/UserDAO.java`
- Create: `src/main/java/mafia/db/GameDAO.java`
- Create: `src/main/java/mafia/db/GameLogDAO.java`
- Create: `src/main/java/mafia/db/AILogDAO.java`

- [ ] **Step 1: UserDAO 구현**

```java
package mafia.db;

import java.sql.*;

public class UserDAO {

    public int createUser(String nickname, boolean isBot) throws SQLException {
        String sql = "INSERT INTO users (nickname, is_bot) VALUES (?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nickname);
            ps.setBoolean(2, isBot);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            return rs.getInt(1);
        }
    }

    public void updateWins(int userId) throws SQLException {
        String sql = "UPDATE users SET wins = wins + 1 WHERE user_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public void updateLosses(int userId) throws SQLException {
        String sql = "UPDATE users SET losses = losses + 1 WHERE user_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }
}
```

- [ ] **Step 2: GameDAO 구현**

```java
package mafia.db;

import mafia.common.Role;
import java.sql.*;
import java.time.LocalDateTime;

public class GameDAO {

    public int createGame(LocalDateTime startedAt) throws SQLException {
        String sql = "INSERT INTO games (winner_team, started_at, ended_at) VALUES ('CITIZEN', ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.valueOf(startedAt));
            ps.setTimestamp(2, Timestamp.valueOf(startedAt)); // 임시, 게임 종료 시 업데이트
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            return rs.getInt(1);
        }
    }

    public void endGame(int gameId, String winnerTeam, LocalDateTime endedAt) throws SQLException {
        String sql = "UPDATE games SET winner_team = ?, ended_at = ? WHERE game_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, winnerTeam);
            ps.setTimestamp(2, Timestamp.valueOf(endedAt));
            ps.setInt(3, gameId);
            ps.executeUpdate();
        }
    }

    public void addPlayer(int gameId, int userId, Role role, Role fakeRole, boolean survived) throws SQLException {
        String sql = "INSERT INTO game_players (game_id, user_id, role, fake_role, is_survived) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, gameId);
            ps.setInt(2, userId);
            ps.setString(3, role.name());
            ps.setString(4, fakeRole != null ? fakeRole.name() : null);
            ps.setBoolean(5, survived);
            ps.executeUpdate();
        }
    }
}
```

- [ ] **Step 3: GameLogDAO 구현**

```java
package mafia.db;

import java.sql.*;

public class GameLogDAO {

    public void log(int gameId, int round, String phase, int actorId,
                    String actionType, Integer targetId, String message) throws SQLException {
        String sql = "INSERT INTO game_logs (game_id, round, phase, actor_id, action_type, target_id, message) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, gameId);
            ps.setInt(2, round);
            ps.setString(3, phase);
            ps.setInt(4, actorId);
            ps.setString(5, actionType);
            if (targetId != null) {
                ps.setInt(6, targetId);
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            ps.setString(7, message);
            ps.executeUpdate();
        }
    }
}
```

- [ ] **Step 4: AILogDAO 구현**

```java
package mafia.db;

import java.sql.*;

public class AILogDAO {

    public void log(int gameId, int round, int userId,
                    String promptSent, String response,
                    String functionCalled, String functionArgs) throws SQLException {
        String sql = "INSERT INTO ai_logs (game_id, round, user_id, prompt_sent, response, function_called, function_args) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, gameId);
            ps.setInt(2, round);
            ps.setInt(3, userId);
            ps.setString(4, promptSent);
            ps.setString(5, response);
            ps.setString(6, functionCalled);
            ps.setString(7, functionArgs);
            ps.executeUpdate();
        }
    }
}
```

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/mafia/db/
git commit -m "feat: DAO 클래스 — UserDAO, GameDAO, GameLogDAO, AILogDAO"
```

---

## Task 8: 봇 — AI 에이전트 (OpenAI Function Calling)

**Files:**
- Create: `src/main/java/mafia/bot/AgentInterface.java`
- Create: `src/main/java/mafia/bot/FunctionRegistry.java`
- Create: `src/main/java/mafia/bot/OpenAIAgent.java`
- Create: `src/test/java/mafia/bot/FunctionRegistryTest.java`

- [ ] **Step 1: AgentInterface 작성**

```java
package mafia.bot;

import java.util.List;

public interface AgentInterface {

    /**
     * AI에게 현재 상황을 전달하고, 함수 호출 결과를 받는다.
     *
     * @param role        AI에게 알려줄 역할 (정병은 "POLICE"로 전달)
     * @param phase       현재 페이즈 (DAY_DISCUSSION, DAY_VOTE, NIGHT)
     * @param survivors   생존자 목록
     * @param chatLog     지금까지의 채팅 로그
     * @param myName      이 봇의 이름
     * @param extraInfo   추가 정보 (경찰 조사 결과 등)
     * @return AgentResponse (선택한 함수명 + 인자)
     */
    AgentResponse decide(String role, String phase, List<String> survivors,
                         List<String> chatLog, String myName, String extraInfo);
}
```

- [ ] **Step 2: AgentResponse 클래스 작성 (같은 패키지)**

```java
package mafia.bot;

public class AgentResponse {
    private final String functionName;  // "발언", "투표", "킬지목", "조사"
    private final String argument;       // 발언 내용 또는 대상 이름

    public AgentResponse(String functionName, String argument) {
        this.functionName = functionName;
        this.argument = argument;
    }

    public String getFunctionName() { return functionName; }
    public String getArgument() { return argument; }

    @Override
    public String toString() {
        return functionName + "(" + argument + ")";
    }
}
```

- [ ] **Step 3: FunctionRegistry 테스트 작성**

```java
package mafia.bot;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class FunctionRegistryTest {

    @Test
    void 토론_페이즈_발언만_가능() {
        List<FunctionRegistry.FunctionDef> functions =
                FunctionRegistry.getAvailableFunctions("DAY_DISCUSSION", "CITIZEN");

        assertEquals(1, functions.size());
        assertEquals("발언", functions.get(0).getName());
    }

    @Test
    void 투표_페이즈_투표만_가능() {
        List<FunctionRegistry.FunctionDef> functions =
                FunctionRegistry.getAvailableFunctions("DAY_VOTE", "CITIZEN");

        assertEquals(1, functions.size());
        assertEquals("투표", functions.get(0).getName());
    }

    @Test
    void 밤_마피아_킬지목_가능() {
        List<FunctionRegistry.FunctionDef> functions =
                FunctionRegistry.getAvailableFunctions("NIGHT", "MAFIA");

        assertEquals(1, functions.size());
        assertEquals("킬지목", functions.get(0).getName());
    }

    @Test
    void 밤_경찰_조사_가능() {
        List<FunctionRegistry.FunctionDef> functions =
                FunctionRegistry.getAvailableFunctions("NIGHT", "POLICE");

        assertEquals(1, functions.size());
        assertEquals("조사", functions.get(0).getName());
    }

    @Test
    void 밤_시민_행동없음() {
        List<FunctionRegistry.FunctionDef> functions =
                FunctionRegistry.getAvailableFunctions("NIGHT", "CITIZEN");

        assertTrue(functions.isEmpty());
    }
}
```

- [ ] **Step 4: 테스트 실행 → 실패 확인**

```bash
./gradlew test --tests "mafia.bot.FunctionRegistryTest"
```

Expected: FAIL

- [ ] **Step 5: FunctionRegistry 구현**

```java
package mafia.bot;

import java.util.*;

public class FunctionRegistry {

    public static class FunctionDef {
        private final String name;
        private final String description;
        private final String paramName;
        private final String paramDescription;

        public FunctionDef(String name, String description, String paramName, String paramDescription) {
            this.name = name;
            this.description = description;
            this.paramName = paramName;
            this.paramDescription = paramDescription;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getParamName() { return paramName; }
        public String getParamDescription() { return paramDescription; }
    }

    private static final FunctionDef SPEAK = new FunctionDef(
            "발언", "채팅에 메시지를 보냅니다", "content", "발언할 내용");
    private static final FunctionDef VOTE = new FunctionDef(
            "투표", "처형할 대상을 투표합니다", "target", "투표할 플레이어 이름");
    private static final FunctionDef KILL = new FunctionDef(
            "킬지목", "밤에 죽일 대상을 선택합니다", "target", "죽일 플레이어 이름");
    private static final FunctionDef INVESTIGATE = new FunctionDef(
            "조사", "대상이 마피아인지 조사합니다", "target", "조사할 플레이어 이름");

    public static List<FunctionDef> getAvailableFunctions(String phase, String role) {
        List<FunctionDef> functions = new ArrayList<>();

        switch (phase) {
            case "DAY_DISCUSSION":
                functions.add(SPEAK);
                break;
            case "DAY_VOTE":
                functions.add(VOTE);
                break;
            case "NIGHT":
                if (role.equals("MAFIA")) {
                    functions.add(KILL);
                } else if (role.equals("POLICE")) {
                    functions.add(INVESTIGATE);
                }
                // CITIZEN은 밤에 할 수 있는 행동 없음
                break;
        }

        return functions;
    }
}
```

- [ ] **Step 6: 테스트 실행 → 통과 확인**

```bash
./gradlew test --tests "mafia.bot.FunctionRegistryTest"
```

Expected: PASS

- [ ] **Step 7: OpenAIAgent 구현**

```java
package mafia.bot;

import com.google.gson.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class OpenAIAgent implements AgentInterface {

    private final String apiKey;

    public OpenAIAgent(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public AgentResponse decide(String role, String phase, List<String> survivors,
                                List<String> chatLog, String myName, String extraInfo) {

        List<FunctionRegistry.FunctionDef> functions =
                FunctionRegistry.getAvailableFunctions(phase, role);

        String systemPrompt = buildSystemPrompt(role, myName);
        String userPrompt = buildUserPrompt(phase, survivors, chatLog, extraInfo);
        JsonArray tools = buildToolsJson(functions);

        JsonObject responseJson = callOpenAI(systemPrompt, userPrompt, tools);
        return parseResponse(responseJson);
    }

    private String buildSystemPrompt(String role, String myName) {
        return String.format(
            "너는 마피아 게임의 플레이어 '%s'이다. 너의 역할은 '%s'이다.\n" +
            "- 마피아라면: 들키지 않게 행동하고, 시민인 척 연기하라.\n" +
            "- 경찰이라면: 조사 결과를 활용해 마피아를 찾아내라. 단, 경찰임을 쉽게 드러내면 밤에 죽을 수 있다.\n" +
            "- 시민이라면: 토론에서 단서를 찾아 마피아를 추리하라.\n" +
            "자연스러운 한국어로 대화하라. 너무 길지 않게 1-2문장으로 발언하라.",
            myName, role
        );
    }

    private String buildUserPrompt(String phase, List<String> survivors,
                                   List<String> chatLog, String extraInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("현재 페이즈: ").append(phase).append("\n");
        sb.append("생존자: ").append(String.join(", ", survivors)).append("\n");

        if (!chatLog.isEmpty()) {
            sb.append("채팅 로그:\n");
            for (String line : chatLog) {
                sb.append("  ").append(line).append("\n");
            }
        }

        if (extraInfo != null && !extraInfo.isEmpty()) {
            sb.append("추가 정보: ").append(extraInfo).append("\n");
        }

        sb.append("\n적절한 함수를 호출하여 행동하라.");
        return sb.toString();
    }

    private JsonArray buildToolsJson(List<FunctionRegistry.FunctionDef> functions) {
        JsonArray tools = new JsonArray();
        for (FunctionRegistry.FunctionDef func : functions) {
            JsonObject tool = new JsonObject();
            tool.addProperty("type", "function");

            JsonObject function = new JsonObject();
            function.addProperty("name", func.getName());
            function.addProperty("description", func.getDescription());

            JsonObject parameters = new JsonObject();
            parameters.addProperty("type", "object");

            JsonObject properties = new JsonObject();
            JsonObject param = new JsonObject();
            param.addProperty("type", "string");
            param.addProperty("description", func.getParamDescription());
            properties.add(func.getParamName(), param);

            parameters.add("properties", properties);
            JsonArray required = new JsonArray();
            required.add(func.getParamName());
            parameters.add("required", required);

            function.add("parameters", parameters);
            tool.add("function", function);
            tools.add(tool);
        }
        return tools;
    }

    private JsonObject callOpenAI(String systemPrompt, String userPrompt, JsonArray tools) {
        try {
            URL url = new URL("https://api.openai.com/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);

            JsonObject body = new JsonObject();
            body.addProperty("model", "gpt-4o-mini");

            JsonArray messages = new JsonArray();
            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("role", "system");
            sysMsg.addProperty("content", systemPrompt);
            messages.add(sysMsg);

            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", userPrompt);
            messages.add(userMsg);

            body.add("messages", messages);
            body.add("tools", tools);
            body.addProperty("tool_choice", "required");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes("UTF-8"));
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                return JsonParser.parseString(response.toString()).getAsJsonObject();
            }
        } catch (Exception e) {
            throw new RuntimeException("OpenAI API 호출 실패", e);
        }
    }

    private AgentResponse parseResponse(JsonObject responseJson) {
        JsonObject choice = responseJson.getAsJsonArray("choices").get(0).getAsJsonObject();
        JsonObject message = choice.getAsJsonObject("message");
        JsonArray toolCalls = message.getAsJsonArray("tool_calls");

        if (toolCalls == null || toolCalls.isEmpty()) {
            // fallback: 함수 호출 없이 텍스트만 온 경우
            String content = message.has("content") ? message.get("content").getAsString() : "...";
            return new AgentResponse("발언", content);
        }

        JsonObject toolCall = toolCalls.get(0).getAsJsonObject();
        JsonObject function = toolCall.getAsJsonObject("function");
        String functionName = function.get("name").getAsString();
        JsonObject args = JsonParser.parseString(function.get("arguments").getAsString()).getAsJsonObject();

        // 첫 번째 인자 값을 추출
        String argument = args.entrySet().iterator().next().getValue().getAsString();

        return new AgentResponse(functionName, argument);
    }

    // AI 로그용: 마지막 요청/응답 원문 조회
    private String lastPrompt;
    private String lastResponse;

    public String getLastPrompt() { return lastPrompt; }
    public String getLastResponse() { return lastResponse; }
}
```

- [ ] **Step 8: 커밋**

```bash
git add src/
git commit -m "feat: AI 에이전트 — AgentInterface, FunctionRegistry, OpenAIAgent"
```

---

## Task 9: 서버 — TCP 소켓 서버 + ClientHandler

**Files:**
- Create: `src/main/java/mafia/server/ClientHandler.java`
- Create: `src/main/java/mafia/server/MafiaServer.java`

- [ ] **Step 1: ClientHandler 구현**

```java
package mafia.server;

import mafia.common.Message;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final String playerId;
    private BufferedReader in;
    private PrintWriter out;
    private Consumer<Message> onMessageReceived;
    private boolean running = true;

    public ClientHandler(Socket socket, String playerId) {
        this.socket = socket;
        this.playerId = playerId;
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        } catch (IOException e) {
            throw new RuntimeException("클라이언트 스트림 초기화 실패", e);
        }
    }

    public void setOnMessageReceived(Consumer<Message> handler) {
        this.onMessageReceived = handler;
    }

    @Override
    public void run() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                Message msg = Message.fromJson(line);
                if (onMessageReceived != null) {
                    onMessageReceived.accept(msg);
                }
            }
        } catch (IOException e) {
            if (running) {
                System.out.println(playerId + " 연결 끊김");
            }
        }
    }

    public void send(Message msg) {
        out.println(msg.toJson());
    }

    public void stop() {
        running = false;
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getPlayerId() {
        return playerId;
    }
}
```

- [ ] **Step 2: MafiaServer 구현**

```java
package mafia.server;

import mafia.common.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MafiaServer {

    private static final int PORT = 5555;
    private static final int MAX_PLAYERS = 5;

    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private GameRoom gameRoom;

    public void start() {
        System.out.println("마피아 서버 시작 — 포트 " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            int playerCount = 0;

            while (playerCount < MAX_PLAYERS) {
                Socket socket = serverSocket.accept();
                playerCount++;
                String playerId = "P" + playerCount;

                ClientHandler handler = new ClientHandler(socket, playerId);
                clients.put(playerId, handler);

                handler.setOnMessageReceived(msg -> handleMessage(playerId, msg));
                new Thread(handler, "Client-" + playerId).start();

                System.out.println(playerId + " 접속 (" + playerCount + "/" + MAX_PLAYERS + ")");

                // 접속한 클라이언트에게 플레이어ID 알려주기
                handler.send(Message.assignId(playerId));
            }

            System.out.println("모든 플레이어 접속 완료. 게임 시작!");
            gameRoom = new GameRoom(clients);
            gameRoom.startGame();

        } catch (IOException e) {
            throw new RuntimeException("서버 시작 실패", e);
        }
    }

    private void handleMessage(String playerId, Message msg) {
        if (gameRoom != null) {
            gameRoom.onMessage(playerId, msg);
        }
    }

    public void broadcast(Message msg) {
        for (ClientHandler handler : clients.values()) {
            handler.send(msg);
        }
    }

    public static void main(String[] args) {
        new MafiaServer().start();
    }
}
```

- [ ] **Step 3: Message에 assignId 팩토리 메서드 추가**

`src/main/java/mafia/common/Message.java`에 추가:

```java
public static Message assignId(String playerId) {
    Message msg = new Message();
    msg.type = "ASSIGN_ID";
    msg.sender = playerId;
    return msg;
}
```

- [ ] **Step 4: 커밋**

```bash
git add src/
git commit -m "feat: TCP 서버 — MafiaServer, ClientHandler 소켓 통신"
```

---

## Task 10: 서버 — GameRoom 게임 진행 로직

**Files:**
- Create: `src/main/java/mafia/server/GameRoom.java`
- Create: `src/main/java/mafia/common/GameResult.java`
- Create: `src/test/java/mafia/server/GameRoomTest.java`

- [ ] **Step 1: GameResult 작성**

```java
package mafia.common;

import java.util.Map;

public class GameResult {
    private final String winnerTeam;  // "CITIZEN" or "MAFIA"
    private final Map<String, Role> roles;
    private final Map<String, Boolean> survived;  // playerId → 생존여부

    public GameResult(String winnerTeam, Map<String, Role> roles, Map<String, Boolean> survived) {
        this.winnerTeam = winnerTeam;
        this.roles = roles;
        this.survived = survived;
    }

    public String getWinnerTeam() { return winnerTeam; }
    public Map<String, Role> getRoles() { return roles; }
    public Map<String, Boolean> getSurvived() { return survived; }
}
```

- [ ] **Step 2: GameRoom 승리 판정 테스트 작성**

```java
package mafia.server;

import mafia.common.Role;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

class GameRoomTest {

    @Test
    void 마피아_전멸_시민승리() {
        Map<String, Role> roles = Map.of(
            "P1", Role.MAFIA, "P2", Role.MAFIA,
            "P3", Role.CITIZEN, "P4", Role.POLICE, "P5", Role.PSYCHO
        );
        Set<String> dead = Set.of("P1", "P2");

        String winner = GameRoom.checkWinner(roles, dead);
        assertEquals("CITIZEN", winner);
    }

    @Test
    void 마피아수_이상_시민수_마피아승리() {
        Map<String, Role> roles = Map.of(
            "P1", Role.MAFIA, "P2", Role.MAFIA,
            "P3", Role.CITIZEN, "P4", Role.POLICE, "P5", Role.PSYCHO
        );
        Set<String> dead = Set.of("P3", "P4", "P5");
        // 생존: P1(마피아), P2(마피아) → 마피아2 >= 시민0

        String winner = GameRoom.checkWinner(roles, dead);
        assertEquals("MAFIA", winner);
    }

    @Test
    void 마피아1_시민2_게임계속() {
        Map<String, Role> roles = Map.of(
            "P1", Role.MAFIA, "P2", Role.MAFIA,
            "P3", Role.CITIZEN, "P4", Role.POLICE, "P5", Role.PSYCHO
        );
        Set<String> dead = Set.of("P2");
        // 생존: P1(마피아), P3(시민), P4(경찰), P5(정병) → 마피아1 < 시민3

        String winner = GameRoom.checkWinner(roles, dead);
        assertNull(winner);  // 아직 게임 진행 중
    }
}
```

- [ ] **Step 3: 테스트 실행 → 실패 확인**

```bash
./gradlew test --tests "mafia.server.GameRoomTest"
```

Expected: FAIL

- [ ] **Step 4: GameRoom 구현**

```java
package mafia.server;

import mafia.common.*;

import java.util.*;
import java.util.concurrent.CountDownLatch;

public class GameRoom {

    private final Map<String, ClientHandler> clients;
    private Map<String, Role> roles;
    private final Set<String> dead = new HashSet<>();
    private final VoteManager voteManager;
    private NightActionHandler nightActionHandler;
    private final List<String> chatLog = new ArrayList<>();
    private int round = 0;
    private GamePhase currentPhase;
    private CountDownLatch actionLatch;

    public GameRoom(Map<String, ClientHandler> clients) {
        this.clients = clients;
        this.voteManager = new VoteManager(clients.size());
    }

    public void startGame() {
        // 역할 배정
        List<String> playerIds = new ArrayList<>(clients.keySet());
        roles = RoleAssigner.assign(playerIds);
        nightActionHandler = new NightActionHandler(roles);

        // 각 클라이언트에게 역할 알려주기 (정병에게는 POLICE로)
        for (Map.Entry<String, Role> entry : roles.entrySet()) {
            String playerId = entry.getKey();
            Role role = entry.getValue();
            ClientHandler handler = clients.get(playerId);
            handler.send(Message.roleAssign(role.getDisplayRole().name()));
        }

        // 게임 루프
        while (true) {
            round++;

            // 낮 토론
            runDayDiscussion();

            // 낮 투표
            String executed = runDayVote();
            if (executed != null) {
                dead.add(executed);
                broadcast(Message.executionResult(executed));

                String winner = checkWinner(roles, dead);
                if (winner != null) {
                    endGame(winner);
                    return;
                }
            }

            // 밤
            String killed = runNight();
            if (killed != null) {
                dead.add(killed);
                broadcast(Message.nightResult(killed));

                String winner = checkWinner(roles, dead);
                if (winner != null) {
                    endGame(winner);
                    return;
                }
            }
        }
    }

    private void runDayDiscussion() {
        currentPhase = GamePhase.DAY_DISCUSSION;
        String[] survivors = getSurvivors();
        broadcast(Message.phaseChange("DAY_DISCUSSION", survivors));

        // 토론 시간 (30초 대기, 그동안 채팅 메시지는 onMessage에서 처리)
        actionLatch = new CountDownLatch(1);
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String runDayVote() {
        currentPhase = GamePhase.DAY_VOTE;
        String[] survivors = getSurvivors();
        voteManager.reset(survivors.length);
        broadcast(Message.phaseChange("DAY_VOTE", survivors));

        // 모든 생존자의 투표를 기다림
        actionLatch = new CountDownLatch(survivors.length);
        try {
            actionLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Optional<String> result = voteManager.getResult();
        return result.orElse(null);
    }

    private String runNight() {
        currentPhase = GamePhase.NIGHT;
        String[] survivors = getSurvivors();
        nightActionHandler.reset();
        broadcast(Message.phaseChange("NIGHT", survivors));

        // 밤 행동이 필요한 플레이어 수 계산
        int nightActors = 0;
        for (String s : survivors) {
            Role role = roles.get(s);
            if (role == Role.MAFIA || role == Role.POLICE || role == Role.PSYCHO) {
                nightActors++;
            }
        }

        actionLatch = new CountDownLatch(nightActors);
        try {
            actionLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return nightActionHandler.getKillTarget();
    }

    public void onMessage(String playerId, Message msg) {
        if (dead.contains(playerId)) return;

        switch (msg.getType()) {
            case "CHAT":
                if (currentPhase == GamePhase.DAY_DISCUSSION) {
                    chatLog.add(playerId + ": " + msg.getMessage());
                    broadcast(msg);
                }
                break;

            case "ACTION":
                handleAction(playerId, msg);
                break;
        }
    }

    private void handleAction(String playerId, Message msg) {
        String action = msg.getAction();
        String target = msg.getTarget();

        switch (action) {
            case "투표":
                voteManager.vote(playerId, target);
                if (actionLatch != null) actionLatch.countDown();
                break;

            case "킬지목":
                nightActionHandler.mafiaKill(playerId, target);
                if (actionLatch != null) actionLatch.countDown();
                break;

            case "조사":
                boolean result = nightActionHandler.investigate(playerId, target);
                // 조사 결과를 해당 플레이어에게만 전달
                clients.get(playerId).send(Message.investigateResult(target, result));
                if (actionLatch != null) actionLatch.countDown();
                break;
        }
    }

    public static String checkWinner(Map<String, Role> roles, Set<String> dead) {
        long aliveMafia = roles.entrySet().stream()
                .filter(e -> !dead.contains(e.getKey()))
                .filter(e -> e.getValue() == Role.MAFIA)
                .count();

        long aliveCitizen = roles.entrySet().stream()
                .filter(e -> !dead.contains(e.getKey()))
                .filter(e -> e.getValue() != Role.MAFIA)
                .count();

        if (aliveMafia == 0) return "CITIZEN";
        if (aliveMafia >= aliveCitizen) return "MAFIA";
        return null;
    }

    private String[] getSurvivors() {
        return clients.keySet().stream()
                .filter(id -> !dead.contains(id))
                .toArray(String[]::new);
    }

    private void broadcast(Message msg) {
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            if (!dead.contains(entry.getKey())) {
                entry.getValue().send(msg);
            }
        }
    }

    private void endGame(String winnerTeam) {
        broadcast(Message.gameOver(winnerTeam));
    }

    public List<String> getChatLog() { return chatLog; }
    public int getRound() { return round; }
}
```

- [ ] **Step 5: Message에 추가 팩토리 메서드 추가**

`src/main/java/mafia/common/Message.java`에 추가:

```java
private String role;
private boolean investigateResult;
private String executed;
private String winnerTeam;

public static Message roleAssign(String role) {
    Message msg = new Message();
    msg.type = "ROLE_ASSIGN";
    msg.role = role;
    return msg;
}

public static Message executionResult(String executed) {
    Message msg = new Message();
    msg.type = "EXECUTION";
    msg.executed = executed;
    return msg;
}

public static Message investigateResult(String target, boolean isMafia) {
    Message msg = new Message();
    msg.type = "INVESTIGATE_RESULT";
    msg.target = target;
    msg.investigateResult = isMafia;
    return msg;
}

public static Message gameOver(String winnerTeam) {
    Message msg = new Message();
    msg.type = "GAME_OVER";
    msg.winnerTeam = winnerTeam;
    return msg;
}

public String getRole() { return role; }
public boolean isInvestigateResult() { return investigateResult; }
public String getExecuted() { return executed; }
public String getWinnerTeam() { return winnerTeam; }
```

- [ ] **Step 6: 테스트 실행 → 통과 확인**

```bash
./gradlew test --tests "mafia.server.GameRoomTest"
```

Expected: PASS

- [ ] **Step 7: 커밋**

```bash
git add src/
git commit -m "feat: GameRoom — 게임 진행 루프, 승리 판정, 페이즈 전환"
```

---

## Task 11: 봇 — BotClient (소켓 접속 + AI 연동)

**Files:**
- Create: `src/main/java/mafia/bot/BotClient.java`

- [ ] **Step 1: BotClient 구현**

```java
package mafia.bot;

import mafia.common.Message;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class BotClient implements Runnable {

    private final String serverHost;
    private final int serverPort;
    private final AgentInterface agent;
    private PrintWriter out;
    private String myId;
    private String myRole;
    private List<String> survivors = new ArrayList<>();
    private final List<String> chatLog = new ArrayList<>();
    private String currentPhase;
    private String extraInfo = "";

    public BotClient(String serverHost, int serverPort, AgentInterface agent) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.agent = agent;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(serverHost, serverPort)) {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            String line;
            while ((line = in.readLine()) != null) {
                Message msg = Message.fromJson(line);
                handleMessage(msg);
            }
        } catch (IOException e) {
            System.out.println("봇 " + myId + " 연결 종료: " + e.getMessage());
        }
    }

    private void handleMessage(Message msg) {
        switch (msg.getType()) {
            case "ASSIGN_ID":
                myId = msg.getSender();
                System.out.println("봇 접속: " + myId);
                break;

            case "ROLE_ASSIGN":
                myRole = msg.getRole();
                System.out.println(myId + " 역할: " + myRole);
                break;

            case "PHASE_CHANGE":
                currentPhase = msg.getPhase();
                survivors = Arrays.asList(msg.getSurvivors());
                act();
                break;

            case "CHAT":
                chatLog.add(msg.getSender() + ": " + msg.getMessage());
                break;

            case "INVESTIGATE_RESULT":
                String resultText = msg.isInvestigateResult() ? "마피아" : "시민";
                extraInfo = "지난 밤 조사 결과: " + msg.getTarget() + "은(는) " + resultText + "입니다.";
                break;

            case "NIGHT_RESULT":
                chatLog.add("[시스템] " + msg.getDead() + "이(가) 밤에 사망했습니다.");
                break;

            case "EXECUTION":
                chatLog.add("[시스템] " + msg.getExecuted() + "이(가) 투표로 처형되었습니다.");
                break;

            case "GAME_OVER":
                System.out.println(myId + " 게임 종료! 승리팀: " + msg.getWinnerTeam());
                break;
        }
    }

    private void act() {
        // 시민은 밤에 행동 없음
        if (currentPhase.equals("NIGHT") && myRole.equals("CITIZEN")) {
            return;
        }

        // AI에게 판단 요청
        AgentResponse response = agent.decide(
                myRole, currentPhase, survivors, chatLog, myId, extraInfo);

        // 결과를 서버로 전송
        Message msg;
        switch (response.getFunctionName()) {
            case "발언":
                msg = Message.chat(myId, response.getArgument());
                break;
            case "투표":
            case "킬지목":
            case "조사":
                msg = Message.action(myId, response.getFunctionName(), response.getArgument());
                break;
            default:
                return;
        }
        out.println(msg.toJson());

        // 조사 후 추가 정보 초기화
        if (currentPhase.equals("NIGHT")) {
            extraInfo = "";
        }
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");
        AgentInterface agent = new OpenAIAgent(apiKey);

        // 봇 4개 동시 접속
        for (int i = 0; i < 4; i++) {
            BotClient bot = new BotClient("localhost", 5555, agent);
            new Thread(bot, "Bot-" + (i + 1)).start();
        }
    }
}
```

- [ ] **Step 2: 커밋**

```bash
git add src/main/java/mafia/bot/BotClient.java
git commit -m "feat: BotClient — 봇 소켓 접속, AI 판단 연동, 서버 메시지 처리"
```

---

## Task 12: 클라이언트 — Swing UI

**Files:**
- Create: `src/main/java/mafia/client/MafiaClient.java`
- Create: `src/main/java/mafia/client/GameUI.java`
- Create: `src/main/java/mafia/client/ChatPanel.java`
- Create: `src/main/java/mafia/client/VotePanel.java`
- Create: `src/main/java/mafia/client/ResultPanel.java`

- [ ] **Step 1: MafiaClient (소켓 클라이언트) 구현**

```java
package mafia.client;

import mafia.common.Message;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class MafiaClient {

    private final String host;
    private final int port;
    private PrintWriter out;
    private Consumer<Message> onMessage;

    public MafiaClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setOnMessage(Consumer<Message> handler) {
        this.onMessage = handler;
    }

    public void connect() {
        try {
            Socket socket = new Socket(host, port);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

            // 수신 스레드
            Thread receiver = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        Message msg = Message.fromJson(line);
                        if (onMessage != null) {
                            onMessage.accept(msg);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("서버 연결 끊김");
                }
            });
            receiver.setDaemon(true);
            receiver.start();

        } catch (IOException e) {
            throw new RuntimeException("서버 연결 실패: " + host + ":" + port, e);
        }
    }

    public void send(Message msg) {
        if (out != null) {
            out.println(msg.toJson());
        }
    }
}
```

- [ ] **Step 2: ChatPanel 구현**

```java
package mafia.client;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class ChatPanel extends JPanel {

    private final JTextArea chatArea;
    private final JTextField inputField;
    private Consumer<String> onSend;

    public ChatPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("맑은 고딕", Font.PLAIN, 15));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setMargin(new Insets(8, 10, 8, 10));
        JScrollPane scroll = new JScrollPane(chatArea);

        inputField = new JTextField();
        inputField.setFont(new Font("맑은 고딕", Font.PLAIN, 15));
        inputField.setMargin(new Insets(6, 8, 6, 8));
        JButton sendBtn = new JButton("전송");
        sendBtn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        sendBtn.setPreferredSize(new Dimension(80, 36));

        JPanel inputPanel = new JPanel(new BorderLayout(6, 0));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);

        add(scroll, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        // 전송 이벤트
        Runnable sendAction = () -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty() && onSend != null) {
                onSend.accept(text);
                inputField.setText("");
            }
        };
        sendBtn.addActionListener(e -> sendAction.run());
        inputField.addActionListener(e -> sendAction.run());
    }

    public void setOnSend(Consumer<String> handler) {
        this.onSend = handler;
    }

    public void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    public void setInputEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            inputField.setEnabled(enabled);
        });
    }
}
```

- [ ] **Step 3: VotePanel 구현**

```java
package mafia.client;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class VotePanel extends JPanel {

    private Consumer<String> onVote;

    public VotePanel() {
        setLayout(new FlowLayout(FlowLayout.CENTER, 8, 8));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(null, "투표",
                        javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                        javax.swing.border.TitledBorder.DEFAULT_POSITION,
                        new Font("맑은 고딕", Font.BOLD, 13)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
    }

    public void setOnVote(Consumer<String> handler) {
        this.onVote = handler;
    }

    public void showVoteButtons(String[] survivors, String myId) {
        SwingUtilities.invokeLater(() -> {
            removeAll();
            for (String player : survivors) {
                if (!player.equals(myId)) {
                    JButton btn = new JButton(player);
                    btn.setFont(new Font("맑은 고딕", Font.BOLD, 15));
                    btn.setPreferredSize(new Dimension(90, 36));
                    btn.setMargin(new Insets(6, 12, 6, 12));
                    btn.addActionListener(e -> {
                        if (onVote != null) {
                            onVote.accept(player);
                        }
                        disableAll();
                    });
                    add(btn);
                }
            }
            revalidate();
            repaint();
        });
    }

    public void disableAll() {
        SwingUtilities.invokeLater(() -> {
            for (Component c : getComponents()) {
                c.setEnabled(false);
            }
        });
    }

    public void clear() {
        SwingUtilities.invokeLater(() -> {
            removeAll();
            revalidate();
            repaint();
        });
    }
}
```

- [ ] **Step 4: ResultPanel 구현**

```java
package mafia.client;

import javax.swing.*;
import java.awt.*;

public class ResultPanel extends JPanel {

    private final JTextArea resultArea;

    public ResultPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(null, "게임 결과",
                        javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                        javax.swing.border.TitledBorder.DEFAULT_POSITION,
                        new Font("맑은 고딕", Font.BOLD, 13)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("맑은 고딕", Font.PLAIN, 15));
        resultArea.setMargin(new Insets(6, 8, 6, 8));
        add(new JScrollPane(resultArea), BorderLayout.CENTER);
    }

    public void showResult(String winnerTeam) {
        SwingUtilities.invokeLater(() -> {
            resultArea.setText("");
            resultArea.append("=== 게임 종료 ===\n\n");
            resultArea.append("승리 팀: " + winnerTeam + "\n");
        });
    }
}
```

- [ ] **Step 5: GameUI (메인 프레임) 구현**

```java
package mafia.client;

import mafia.common.Message;

import javax.swing.*;
import java.awt.*;

public class GameUI extends JFrame {

    private final MafiaClient client;
    private final ChatPanel chatPanel;
    private final VotePanel votePanel;
    private final ResultPanel resultPanel;
    private final JLabel statusLabel;
    private String myId;
    private String myRole;

    public GameUI() {
        setTitle("AI 마피아 게임");
        setSize(900, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 전체 컨텐츠 패딩
        JPanel contentPane = new JPanel(new BorderLayout(10, 0));
        contentPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        setContentPane(contentPane);

        // UI 구성
        chatPanel = new ChatPanel();
        votePanel = new VotePanel();
        resultPanel = new ResultPanel();
        statusLabel = new JLabel("서버 접속 중...");
        statusLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(240, 240, 245));

        JPanel rightPanel = new JPanel(new BorderLayout(0, 8));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 10));
        rightPanel.add(votePanel, BorderLayout.NORTH);
        rightPanel.add(resultPanel, BorderLayout.CENTER);
        rightPanel.setPreferredSize(new Dimension(260, 0));

        contentPane.add(statusLabel, BorderLayout.NORTH);
        contentPane.add(chatPanel, BorderLayout.CENTER);
        contentPane.add(rightPanel, BorderLayout.EAST);

        // 네트워크 연결
        client = new MafiaClient("localhost", 5555);
        client.setOnMessage(this::handleMessage);

        // 채팅 전송
        chatPanel.setOnSend(text -> {
            client.send(Message.chat(myId, text));
        });

        // 투표 전송
        votePanel.setOnVote(target -> {
            client.send(Message.action(myId, "투표", target));
            chatPanel.appendMessage("[시스템] " + target + "에게 투표했습니다.");
        });

        setVisible(true);
        client.connect();
    }

    private void handleMessage(Message msg) {
        switch (msg.getType()) {
            case "ASSIGN_ID":
                myId = msg.getSender();
                updateStatus("접속 완료: " + myId + " — 다른 플레이어를 기다리는 중...");
                break;

            case "ROLE_ASSIGN":
                myRole = msg.getRole();
                chatPanel.appendMessage("[시스템] 당신의 역할: " + myRole);
                break;

            case "PHASE_CHANGE":
                handlePhaseChange(msg);
                break;

            case "CHAT":
                chatPanel.appendMessage(msg.getSender() + ": " + msg.getMessage());
                break;

            case "NIGHT_RESULT":
                chatPanel.appendMessage("[시스템] " + msg.getDead() + "이(가) 밤에 사망했습니다.");
                break;

            case "EXECUTION":
                chatPanel.appendMessage("[시스템] " + msg.getExecuted() + "이(가) 투표로 처형되었습니다.");
                break;

            case "INVESTIGATE_RESULT":
                String result = msg.isInvestigateResult() ? "마피아" : "시민";
                chatPanel.appendMessage("[조사 결과] " + msg.getTarget() + "은(는) " + result + "입니다.");
                break;

            case "GAME_OVER":
                updateStatus("게임 종료! 승리 팀: " + msg.getWinnerTeam());
                chatPanel.setInputEnabled(false);
                votePanel.clear();
                resultPanel.showResult(msg.getWinnerTeam());
                break;
        }
    }

    private void handlePhaseChange(Message msg) {
        String phase = msg.getPhase();
        String[] survivors = msg.getSurvivors();

        switch (phase) {
            case "DAY_DISCUSSION":
                updateStatus("낮 — 토론 시간 (30초)");
                chatPanel.setInputEnabled(true);
                votePanel.clear();
                break;

            case "DAY_VOTE":
                updateStatus("낮 — 투표 시간");
                chatPanel.setInputEnabled(false);
                votePanel.showVoteButtons(survivors, myId);
                break;

            case "NIGHT":
                updateStatus("밤 — " + getNightAction());
                chatPanel.setInputEnabled(false);
                votePanel.clear();

                // 밤 행동 버튼 (마피아: 킬지목, 경찰: 조사)
                if (myRole.equals("MAFIA") || myRole.equals("POLICE")) {
                    votePanel.setOnVote(target -> {
                        String action = myRole.equals("MAFIA") ? "킬지목" : "조사";
                        client.send(Message.action(myId, action, target));
                        chatPanel.appendMessage("[시스템] " + target + "을(를) " + action + "했습니다.");
                    });
                    votePanel.showVoteButtons(survivors, myId);
                }
                break;
        }
    }

    private String getNightAction() {
        if ("MAFIA".equals(myRole)) return "킬 대상을 선택하세요";
        if ("POLICE".equals(myRole)) return "조사 대상을 선택하세요";
        return "밤이 되었습니다. 기다려주세요...";
    }

    private void updateStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameUI::new);
    }
}
```

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/mafia/client/
git commit -m "feat: Swing UI — GameUI, ChatPanel, VotePanel, ResultPanel"
```

---

## Task 13: 통합 테스트 — 서버 + 봇 4개 + 클라이언트 1개 실행

**Files:** 기존 파일 수정 없음, 실행 테스트

- [ ] **Step 1: MySQL DB 스키마 적용 확인**

```bash
mysql -u root -e "USE mafia_game; SHOW TABLES;"
```

Expected: users, games, game_players, game_logs, ai_logs 5개 테이블 출력

- [ ] **Step 2: OpenAI API 키 환경변수 설정**

```bash
export OPENAI_API_KEY="sk-your-key-here"
```

- [ ] **Step 3: 서버 실행**

터미널 1:
```bash
cd Mafia_For_Java && ./gradlew run
```

Expected: "마피아 서버 시작 — 포트 5555" 출력

- [ ] **Step 4: 봇 4개 실행**

터미널 2:
```bash
cd Mafia_For_Java && ./gradlew run -PmainClass=mafia.bot.BotClient
```

Expected: "봇 접속: P1" ~ "봇 접속: P4" 출력 (또는 접속 순서에 따라 ID 다를 수 있음)

- [ ] **Step 5: 클라이언트 실행**

터미널 3:
```bash
cd Mafia_For_Java && ./gradlew run -PmainClass=mafia.client.GameUI
```

Expected: Swing 창 열림, "접속 완료: P5" 표시, 역할 배정 메시지 표시

- [ ] **Step 6: 게임 1판 플레이 테스트**

확인할 것:
- 토론 시간에 AI 봇들이 채팅 발언하는지
- 투표 버튼이 정상 작동하는지
- 밤에 역할별 행동이 동작하는지 (마피아 킬, 경찰 조사)
- 승리 조건 달성 시 게임 종료되는지
- 정병이 자기가 경찰인 줄 알고 발언하는지

- [ ] **Step 7: 문제 수정 후 커밋**

```bash
git add -A
git commit -m "fix: 통합 테스트에서 발견된 이슈 수정"
```

---

## Task 14: DB 연동 — 게임 기록 저장

**Files:**
- Modify: `src/main/java/mafia/server/GameRoom.java`
- Modify: `src/main/java/mafia/server/MafiaServer.java`

- [ ] **Step 1: GameRoom에 DB 저장 로직 추가**

`GameRoom.java`의 `startGame()` 시작 부분에 추가:

```java
// 게임 시작 시 DB 기록
private int gameId;
private GameDAO gameDAO = new GameDAO();
private GameLogDAO gameLogDAO = new GameLogDAO();
private UserDAO userDAO = new UserDAO();

// startGame() 최상단에 추가:
gameId = gameDAO.createGame(LocalDateTime.now());
```

`endGame()` 메서드 수정:

```java
private void endGame(String winnerTeam) {
    broadcast(Message.gameOver(winnerTeam));

    try {
        // 게임 종료 기록
        gameDAO.endGame(gameId, winnerTeam, LocalDateTime.now());

        // 참가자 기록
        for (Map.Entry<String, Role> entry : roles.entrySet()) {
            String playerId = entry.getKey();
            Role role = entry.getValue();
            Role fakeRole = (role == Role.PSYCHO) ? Role.POLICE : null;
            boolean survived = !dead.contains(playerId);

            // 임시 userId (실제로는 접속 시 DB에서 조회)
            int userId = 1; // TODO: 실제 userId 매핑 필요
            gameDAO.addPlayer(gameId, userId, role, fakeRole, survived);
        }
    } catch (Exception e) {
        System.err.println("DB 저장 실패: " + e.getMessage());
    }
}
```

`onMessage()`의 CHAT 처리에 로그 저장 추가:

```java
case "CHAT":
    if (currentPhase == GamePhase.DAY_DISCUSSION) {
        chatLog.add(playerId + ": " + msg.getMessage());
        broadcast(msg);
        try {
            gameLogDAO.log(gameId, round, "DAY_DISCUSSION", 1, "CHAT", null, msg.getMessage());
        } catch (Exception e) {
            System.err.println("로그 저장 실패: " + e.getMessage());
        }
    }
    break;
```

- [ ] **Step 2: 통합 테스트 — 게임 후 DB 확인**

게임 한 판 플레이 후:
```bash
mysql -u root -e "USE mafia_game; SELECT * FROM games; SELECT * FROM game_players ORDER BY game_id;"
```

Expected: 게임 기록 1건, 참가자 5명 기록 확인

- [ ] **Step 3: 커밋**

```bash
git add src/
git commit -m "feat: DB 연동 — 게임 기록, 로그 저장"
```

---

## 실행 요약

| 실행 대상 | 명령어 |
|-----------|--------|
| 서버 | `./gradlew run` |
| 봇 4개 | `./gradlew run -PmainClass=mafia.bot.BotClient` |
| 클라이언트 | `./gradlew run -PmainClass=mafia.client.GameUI` |
| 전체 테스트 | `./gradlew test` |
| DB 스키마 | `mysql -u root < sql/schema.sql` |
