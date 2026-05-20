# Bot Simulation GUI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** AI 봇 5명이 자동으로 마피아 게임을 진행하는 관전자 모드 GUI를 구현한다. 사용자는 봇들의 게임 흐름과 추론을 시각적으로 관찰.

**Architecture:** 새 module 2개(`mafia-engine`, `mafia-agent`) + 기존 `mafia-client`에 시뮬레이션 화면 추가. UI는 JavaFX `ObservableList<Event>` 패턴으로 데이터만 갱신하면 화면 자동 갱신. 진입점은 별도 `SimulationMain` 클래스(기존 게임 진행과 독립).

**Tech Stack:** Java 21 + JavaFX 21 (controls). 외부 라이브러리 추가 없음.

**Testing Approach:** JavaFX UI는 자동 테스트 인프라 X. 콘솔 진행은 `System.out.println` 출력으로 검증. 시각 검증은 사용자가 IDE에서 직접 실행.

**Spec Reference:** `docs/superpowers/specs/2026-05-20-bot-simulation-gui-design.md`

**진행 전략:** 사용자 의도 — UI까지 빠르게 만든 후 알고리즘은 사용자와 함께. 이 plan은 **Task 1~5 (UI 기본)** 까지 자세히, 이후 알고리즘(RuleBased/CSP/LLM)은 후속 sub-project로.

---

## File Structure

| 경로 | 상태 | 책임 |
|---|---|---|
| `pom.xml` (parent) | 수정 | mafia-engine, mafia-agent module 추가 |
| `mafia-engine/pom.xml` | 신규 | engine 의존성 (mafia-common만) |
| `mafia-engine/src/main/java/mafia/engine/` | 신규 | Event, GameState, GameView, GameEngine 등 |
| `mafia-agent/pom.xml` | 신규 | agent 의존성 (mafia-common + mafia-engine) |
| `mafia-agent/src/main/java/mafia/agent/` | 신규 | Agent interface, RandomAgent, InferenceState |
| `mafia-common/src/main/java/mafia/domain/` | 추가 | Role, GamePhase, Player enum/class |
| `mafia-client/pom.xml` | 수정 | mafia-engine, mafia-agent 의존성 추가 |
| `mafia-client/src/main/java/GUI/sim/` | 신규 | SimulationScene + 컴포넌트들 |

---

## Task 1: 모듈 셋업 + Core 도메인 (mafia-common 확장)

기반 작업. mafia-engine, mafia-agent module 자리 마련 + 공유 도메인 정의.

**Files:**
- Modify: `pom.xml`
- Create: `mafia-engine/pom.xml`
- Create: `mafia-agent/pom.xml`
- Create: `mafia-common/src/main/java/mafia/domain/Role.java`
- Create: `mafia-common/src/main/java/mafia/domain/GamePhase.java`
- Create: `mafia-common/src/main/java/mafia/domain/Player.java`

### Step 1: parent pom에 module 2개 추가

`pom.xml`의 `<modules>` 블록에 추가:

```xml
<modules>
    <module>mafia-common</module>
    <module>mafia-server</module>
    <module>mafia-client</module>
    <module>mafia-engine</module>
    <module>mafia-agent</module>
</modules>
```

### Step 2: `mafia-engine/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.bhyoung</groupId>
        <artifactId>mafia-ai-game</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <artifactId>mafia-engine</artifactId>
    <packaging>jar</packaging>
    <name>Mafia Engine</name>
    <description>게임 룰 + 시뮬레이션 (UI/네트워크 무관)</description>
    <dependencies>
        <dependency>
            <groupId>com.bhyoung</groupId>
            <artifactId>mafia-common</artifactId>
        </dependency>
    </dependencies>
</project>
```

### Step 3: `mafia-agent/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.bhyoung</groupId>
        <artifactId>mafia-ai-game</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <artifactId>mafia-agent</artifactId>
    <packaging>jar</packaging>
    <name>Mafia Agent</name>
    <description>봇 Agent 인터페이스 + 구현체 (Random/RuleBased/CSP/LLM)</description>
    <dependencies>
        <dependency>
            <groupId>com.bhyoung</groupId>
            <artifactId>mafia-common</artifactId>
        </dependency>
        <dependency>
            <groupId>com.bhyoung</groupId>
            <artifactId>mafia-engine</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</project>
```

mafia-engine을 의존성에 추가했으니 parent의 `dependencyManagement`에도 등록:

```xml
<dependency>
    <groupId>com.bhyoung</groupId>
    <artifactId>mafia-engine</artifactId>
    <version>${project.version}</version>
</dependency>
```

### Step 4: `Role.java`

```java
package mafia.domain;

public enum Role {
    MAFIA("마피아"),
    CITIZEN("시민"),
    POLICE("경찰"),
    DOCTOR("의사"),
    PSYCHO("정병");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isMafiaTeam() {
        return this == MAFIA;
    }
}
```

### Step 5: `GamePhase.java`

```java
package mafia.domain;

public enum GamePhase {
    DAY_DISCUSSION("낮 토론"),
    DAY_VOTE("낮 투표"),
    NIGHT("밤"),
    ENDED("종료");

    private final String label;

    GamePhase(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
```

### Step 6: `Player.java`

```java
package mafia.domain;

public class Player {
    private final int slot;          // 1~5
    private final String nickname;
    private final String botType;    // "RANDOM" / "RULE" / "CSP" / "LLM"
    private Role role;               // 게임 시작 시 배정
    private boolean alive;

    public Player(int slot, String nickname, String botType) {
        this.slot = slot;
        this.nickname = nickname;
        this.botType = botType;
        this.alive = true;
    }

    public int getSlot() { return slot; }
    public String getNickname() { return nickname; }
    public String getBotType() { return botType; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isAlive() { return alive; }
    public void kill() { this.alive = false; }

    @Override
    public String toString() {
        return "P" + slot + "(" + nickname + ")";
    }
}
```

### Step 7: 빌드 검증 + 커밋

```bash
mvn install -DskipTests
```

3 module → 5 module 빌드 성공 확인.

```bash
git add pom.xml mafia-engine/ mafia-agent/ mafia-common/src/main/java/mafia/domain/
git commit -m "Engine/Agent: 두 module 신설 + Role/GamePhase/Player 도메인 추가"
```

---

## Task 2: Agent 인터페이스 + RandomAgent + GameEngine (콘솔 동작)

UI 없이 게임 진행 자체를 콘솔에서 검증. 5봇이 자동 게임 1판 끝까지.

**Files:**
- Create: `mafia-engine/src/main/java/mafia/engine/Event.java`
- Create: `mafia-engine/src/main/java/mafia/engine/EventLog.java`
- Create: `mafia-engine/src/main/java/mafia/engine/GameState.java`
- Create: `mafia-engine/src/main/java/mafia/engine/GameView.java`
- Create: `mafia-engine/src/main/java/mafia/engine/GameEngine.java`
- Create: `mafia-engine/src/main/java/mafia/engine/RoleAssigner.java`
- Create: `mafia-engine/src/main/java/mafia/engine/VoteCounter.java`
- Create: `mafia-engine/src/main/java/mafia/engine/WinCondition.java`
- Create: `mafia-engine/src/main/java/mafia/engine/GameResult.java`
- Create: `mafia-engine/src/main/java/mafia/engine/Decision.java`
- Create: `mafia-agent/src/main/java/mafia/agent/Agent.java`
- Create: `mafia-agent/src/main/java/mafia/agent/InferenceState.java`
- Create: `mafia-agent/src/main/java/mafia/agent/RandomAgent.java`
- Create: `mafia-engine/src/main/java/mafia/engine/demo/SimulateOnce.java`

### Step 1: `Event.java` (sealed interface)

```java
package mafia.engine;

import mafia.domain.GamePhase;
import mafia.domain.Player;

import java.time.Instant;

public sealed interface Event permits
    Event.Speak, Event.Vote, Event.Executed, Event.NightKill,
    Event.Investigation, Event.PhaseChanged, Event.GameStarted, Event.GameEnded {

    int round();
    GamePhase phase();
    Instant timestamp();

    record Speak(int round, GamePhase phase, Player actor, String text, Instant timestamp) implements Event {}
    record Vote(int round, GamePhase phase, Player voter, Player target, Instant timestamp) implements Event {}
    record Executed(int round, GamePhase phase, Player target, int voteCount, Instant timestamp) implements Event { public Player actor(){ return target; } }
    record NightKill(int round, GamePhase phase, Player killer, Player target, Instant timestamp) implements Event {}
    record Investigation(int round, GamePhase phase, Player officer, Player target, boolean isMafia, Instant timestamp) implements Event {}
    record PhaseChanged(int round, GamePhase phase, GamePhase from, GamePhase to, Instant timestamp) implements Event {}
    record GameStarted(int round, GamePhase phase, java.util.List<Player> players, Instant timestamp) implements Event {}
    record GameEnded(int round, GamePhase phase, mafia.domain.Role winnerTeam, Instant timestamp) implements Event {}
}
```

### Step 2: `EventLog.java`

```java
package mafia.engine;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class EventLog {
    private final ObservableList<Event> events = FXCollections.observableArrayList();

    public void append(Event event) {
        events.add(event);
    }

    public ObservableList<Event> events() {
        return events;
    }
}
```

> 짚을 점: `mafia-engine`이 JavaFX(`ObservableList`)에 의존하게 된다. 이미 `mafia-common`이 JavaFX-base에 의존하니 큰 부담은 아니지만, 미래 정리 항목으로 둔다. 지금은 학습/시각화 우선.

### Step 3: `GameState.java`

```java
package mafia.engine;

import mafia.domain.GamePhase;
import mafia.domain.Player;
import java.util.List;

public class GameState {
    private final List<Player> players;
    private int round;
    private GamePhase phase;
    private int currentMafiaTarget;  // 밤 시점 마피아가 지목한 슬롯

    public GameState(List<Player> players) {
        this.players = players;
        this.round = 0;
        this.phase = GamePhase.DAY_DISCUSSION;
    }

    public List<Player> getPlayers() { return players; }
    public List<Player> aliveOnes() { return players.stream().filter(Player::isAlive).toList(); }
    public int getRound() { return round; }
    public GamePhase getPhase() { return phase; }

    public void setRound(int round) { this.round = round; }
    public void setPhase(GamePhase phase) { this.phase = phase; }
    public Player findBySlot(int slot) {
        return players.stream().filter(p -> p.getSlot() == slot).findFirst().orElseThrow();
    }
}
```

### Step 4: `GameView.java` — Agent가 볼 수 있는 정보

```java
package mafia.engine;

import mafia.domain.Player;
import mafia.domain.Role;
import java.util.List;

public class GameView {
    private final GameState state;
    private final Player me;
    private final Role myRole;          // 본인은 자기 역할 알 수 있음
    private final EventLog publicLog;   // 공개 이벤트만

    public GameView(GameState state, Player me, EventLog publicLog) {
        this.state = state;
        this.me = me;
        this.myRole = me.getRole();
        this.publicLog = publicLog;
    }

    public Player me() { return me; }
    public Role myRole() { return myRole; }
    public List<Player> alivePlayers() { return state.aliveOnes(); }
    public int round() { return state.getRound(); }
    public EventLog publicLog() { return publicLog; }
}
```

### Step 5: `Decision.java`

```java
package mafia.engine;

import mafia.domain.Player;

public record Decision(Player target, String reason) {
    public static Decision none() { return new Decision(null, ""); }
}
```

### Step 6: `RoleAssigner.java`

```java
package mafia.engine;

import mafia.domain.Player;
import mafia.domain.Role;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RoleAssigner {
    /**
     * 5명 기준: 마피아 1, 시민 2, 경찰 1, 의사 1 (설계 문서 룰)
     * 정병은 일단 제외 (Phase에서 추가 옵션)
     */
    public static void assign(List<Player> players, long seed) {
        List<Role> roles = new ArrayList<>(List.of(
            Role.MAFIA, Role.CITIZEN, Role.CITIZEN, Role.POLICE, Role.DOCTOR
        ));
        Collections.shuffle(roles, new Random(seed));
        for (int i = 0; i < players.size(); i++) {
            players.get(i).setRole(roles.get(i));
        }
    }
}
```

### Step 7: `VoteCounter.java`

```java
package mafia.engine;

import mafia.domain.Player;

import java.util.HashMap;
import java.util.Map;

public class VoteCounter {
    public record Result(Player executed, int count) {}

    /**
     * 최다 득표자 처형. 동점이면 처형 없음.
     */
    public static Result tally(Map<Player, Player> votes) {
        Map<Player, Integer> count = new HashMap<>();
        for (Player target : votes.values()) {
            if (target != null) {
                count.merge(target, 1, Integer::sum);
            }
        }
        Player top = null;
        int topCount = 0;
        boolean tie = false;
        for (Map.Entry<Player, Integer> e : count.entrySet()) {
            if (e.getValue() > topCount) {
                top = e.getKey();
                topCount = e.getValue();
                tie = false;
            } else if (e.getValue() == topCount) {
                tie = true;
            }
        }
        return tie ? new Result(null, 0) : new Result(top, topCount);
    }
}
```

### Step 8: `WinCondition.java`

```java
package mafia.engine;

import mafia.domain.Player;
import mafia.domain.Role;

import java.util.List;

public class WinCondition {
    public enum Winner { CITIZEN, MAFIA, UNDECIDED }

    public static Winner check(List<Player> alive) {
        long mafia = alive.stream().filter(p -> p.getRole() == Role.MAFIA).count();
        long citizen = alive.size() - mafia;
        if (mafia == 0) return Winner.CITIZEN;
        if (mafia >= citizen) return Winner.MAFIA;
        return Winner.UNDECIDED;
    }
}
```

### Step 9: `GameResult.java`

```java
package mafia.engine;

import mafia.domain.Player;

import java.util.List;

public record GameResult(WinCondition.Winner winner, int totalRounds, List<Player> finalPlayers, EventLog log) {}
```

### Step 10: `Agent.java` (in mafia-agent)

```java
package mafia.agent;

import mafia.engine.Decision;
import mafia.engine.Event;
import mafia.engine.GameView;

public interface Agent {
    /** 페이즈별 행동 (투표/킬/조사). null target은 "결정 없음". */
    Decision decide(GameView view);

    /** 자연어 발언. 빈 문자열이면 발언 안 함. */
    String speak(GameView view);

    /** 새 공개 이벤트 수신 (자기 추론 갱신용). */
    void observe(Event event);

    /** 추론 상태 스냅샷 (시각화용). null 가능 (RandomAgent 등). */
    InferenceState getInference();

    /** "RANDOM" / "RULE" / "CSP" / "LLM" */
    String botType();
}
```

### Step 11: `InferenceState.java`

```java
package mafia.agent;

import mafia.domain.Player;
import mafia.domain.Role;

import java.util.List;
import java.util.Map;

public record InferenceState(
    Map<Player, Map<Role, Double>> domains,
    List<String> recentlyEliminated,
    Player nextActionTarget,
    double targetConfidence
) {
    public static InferenceState empty() {
        return new InferenceState(Map.of(), List.of(), null, 0);
    }
}
```

### Step 12: `RandomAgent.java`

```java
package mafia.agent;

import mafia.engine.Decision;
import mafia.engine.Event;
import mafia.engine.GameView;
import mafia.domain.Player;

import java.util.List;
import java.util.Random;

public class RandomAgent implements Agent {
    private final Random rng;

    public RandomAgent(long seed) { this.rng = new Random(seed); }
    public RandomAgent() { this(System.nanoTime()); }

    @Override
    public Decision decide(GameView view) {
        List<Player> candidates = view.alivePlayers().stream()
            .filter(p -> p != view.me())
            .toList();
        if (candidates.isEmpty()) return Decision.none();
        Player target = candidates.get(rng.nextInt(candidates.size()));
        return new Decision(target, "랜덤 선택");
    }

    @Override
    public String speak(GameView view) {
        return "음... 누가 의심스러운지 모르겠어요.";
    }

    @Override
    public void observe(Event event) { /* random은 무시 */ }

    @Override
    public InferenceState getInference() { return InferenceState.empty(); }

    @Override
    public String botType() { return "RANDOM"; }
}
```

### Step 13: `GameEngine.java`

```java
package mafia.engine;

import mafia.agent.Agent;
import mafia.domain.GamePhase;
import mafia.domain.Player;
import mafia.domain.Role;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameEngine {
    private final List<Player> players;
    private final Map<Player, Agent> agents;
    private final EventLog log = new EventLog();
    private final GameState state;
    private final long seed;
    private final java.util.Random rng;

    public GameEngine(List<Player> players, Map<Player, Agent> agents, long seed) {
        this.players = players;
        this.agents = agents;
        this.seed = seed;
        this.rng = new java.util.Random(seed);
        this.state = new GameState(players);
        RoleAssigner.assign(players, seed);
    }

    public GameResult play() {
        Instant now = Instant.now();
        log.append(new Event.GameStarted(0, GamePhase.DAY_DISCUSSION, players, now));

        int round = 1;
        while (true) {
            state.setRound(round);

            // 낮 토론
            state.setPhase(GamePhase.DAY_DISCUSSION);
            for (Player p : state.aliveOnes()) {
                String text = agents.get(p).speak(viewFor(p));
                if (text != null && !text.isBlank()) {
                    log.append(new Event.Speak(round, GamePhase.DAY_DISCUSSION, p, text, Instant.now()));
                }
            }

            // 낮 투표
            state.setPhase(GamePhase.DAY_VOTE);
            Map<Player, Player> votes = new HashMap<>();
            for (Player p : state.aliveOnes()) {
                Decision d = agents.get(p).decide(viewFor(p));
                votes.put(p, d.target());
                log.append(new Event.Vote(round, GamePhase.DAY_VOTE, p, d.target(), Instant.now()));
            }
            var tally = VoteCounter.tally(votes);
            if (tally.executed() != null) {
                tally.executed().kill();
                log.append(new Event.Executed(round, GamePhase.DAY_VOTE, tally.executed(), tally.count(), Instant.now()));
            }

            if (checkEnd(round)) return result(round);

            // 밤
            state.setPhase(GamePhase.NIGHT);
            // 마피아 킬
            Player mafia = aliveByRole(Role.MAFIA);
            if (mafia != null) {
                Decision d = agents.get(mafia).decide(viewFor(mafia));
                if (d.target() != null && d.target().isAlive()) {
                    // 의사 보호 체크
                    Player doctor = aliveByRole(Role.DOCTOR);
                    Player doctorTarget = null;
                    if (doctor != null) {
                        Decision dd = agents.get(doctor).decide(viewFor(doctor));
                        doctorTarget = dd.target();
                    }
                    if (doctorTarget != d.target()) {
                        d.target().kill();
                        log.append(new Event.NightKill(round, GamePhase.NIGHT, mafia, d.target(), Instant.now()));
                    } else {
                        // 의사 보호 성공 — 이벤트만 (디버깅)
                    }
                }
            }
            // 경찰 조사
            Player police = aliveByRole(Role.POLICE);
            if (police != null) {
                Decision d = agents.get(police).decide(viewFor(police));
                if (d.target() != null) {
                    boolean isMafia = d.target().getRole() == Role.MAFIA;
                    log.append(new Event.Investigation(round, GamePhase.NIGHT, police, d.target(), isMafia, Instant.now()));
                }
            }

            if (checkEnd(round)) return result(round);
            round++;
            if (round > 20) return result(round);  // safety
        }
    }

    private GameView viewFor(Player p) {
        return new GameView(state, p, log);
    }

    private Player aliveByRole(Role role) {
        return state.aliveOnes().stream().filter(p -> p.getRole() == role).findFirst().orElse(null);
    }

    private boolean checkEnd(int round) {
        var winner = WinCondition.check(state.aliveOnes());
        if (winner != WinCondition.Winner.UNDECIDED) {
            Role team = (winner == WinCondition.Winner.MAFIA) ? Role.MAFIA : Role.CITIZEN;
            log.append(new Event.GameEnded(round, state.getPhase(), team, Instant.now()));
            state.setPhase(GamePhase.ENDED);
            return true;
        }
        return false;
    }

    private GameResult result(int round) {
        var winner = WinCondition.check(state.aliveOnes());
        return new GameResult(winner, round, players, log);
    }

    public EventLog log() { return log; }
}
```

### Step 14: `SimulateOnce.java` (콘솔 데모)

```java
package mafia.engine.demo;

import mafia.agent.Agent;
import mafia.agent.RandomAgent;
import mafia.domain.Player;
import mafia.engine.Event;
import mafia.engine.GameEngine;
import mafia.engine.GameResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimulateOnce {
    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 42L;

        List<Player> players = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            players.add(new Player(i, "P" + i, "RANDOM"));
        }

        Map<Player, Agent> agents = new HashMap<>();
        for (Player p : players) {
            agents.put(p, new RandomAgent(seed + p.getSlot()));
        }

        GameEngine engine = new GameEngine(players, agents, seed);
        GameResult result = engine.play();

        System.out.println("--- 게임 종료 ---");
        System.out.println("승자: " + result.winner());
        System.out.println("총 라운드: " + result.totalRounds());
        System.out.println("--- 최종 역할 공개 ---");
        for (Player p : players) {
            System.out.println(p + " · " + p.getRole().getLabel() + " · " + (p.isAlive() ? "생존" : "사망"));
        }
        System.out.println("--- 이벤트 로그 ---");
        for (Event e : result.log().events()) {
            System.out.println(e);
        }
    }
}
```

### Step 15: 빌드 + 검증 + 커밋

```bash
mvn install -DskipTests
mvn -pl mafia-engine -am exec:java -Dexec.mainClass=mafia.engine.demo.SimulateOnce -Dexec.args="42"
```

Expected: "게임 종료" + 승자 + 이벤트 로그가 콘솔에 출력.

```bash
git add mafia-engine/ mafia-agent/
git commit -m "Engine: GameEngine + RandomAgent로 5봇 게임 1판 콘솔 시뮬레이션"
```

---

## Task 3: SimulationScene UI 뼈대 + PlayerCard (더미 데이터)

UI 우선. Engine 연결 없이 더미 데이터로 화면 모양 확인.

**Files:**
- Create: `mafia-client/src/main/resources/css/simulation.css`
- Create: `mafia-client/src/main/java/GUI/sim/SimulationScene.java`
- Create: `mafia-client/src/main/java/GUI/sim/PlayerCard.java`
- Create: `mafia-client/src/main/java/GUI/sim/EventLogPanel.java`
- Create: `mafia-client/src/main/java/GUI/sim/StatsPanel.java`
- Create: `mafia-client/src/main/java/GUI/sim/ControlBar.java`
- Create: `mafia-client/src/main/java/GUI/sim/SimulationMain.java`
- Modify: `mafia-client/pom.xml` (mafia-engine, mafia-agent 의존성 추가)

### 핵심 작업

- `mafia-client/pom.xml`에 `mafia-engine`, `mafia-agent` 의존성 추가
- 5개 `PlayerCard`를 가로로 배치한 SimulationScene
- `EventLogPanel`은 더미 텍스트 ("P1: 의심돼요" 등) 표시
- `StatsPanel`은 더미 "0승 0패"
- `ControlBar`에 Play/Pause/Step 버튼 (동작은 다음 Task에서)
- `SimulationMain`이 JavaFX Application으로 stage 띄움
- `simulation.css`로 카드/패널 스타일링 (tokens.css 재사용)

### 검증

```bash
mvn -pl mafia-client exec:java -Dexec.mainClass=GUI.sim.SimulationMain
```

→ 시뮬레이션 화면이 떠야 함. 카드 5개 + 더미 채팅 로그 + 통계 0/0.

### 커밋

```bash
git add mafia-client/
git commit -m "SimGUI: SimulationScene + PlayerCard + 패널 뼈대 (더미 데이터)"
```

---

## Task 4: Engine ↔ UI 연결 (자동 진행)

UI와 GameEngine을 연결해서 진짜 게임 진행이 화면에 표시.

**Files:**
- Modify: `mafia-client/src/main/java/GUI/sim/SimulationScene.java`
- Create: `mafia-client/src/main/java/GUI/sim/SimulationController.java`

### 핵심 작업

1. **SimulationController**: 별도 스레드에서 GameEngine 한 페이즈씩 실행
   - `play()`: 자동 모드 (1초/페이즈)
   - `pause()`: 멈춤
   - `step()`: 한 페이즈만 진행
   - `newGame()`: 새 인스턴스 + 새 seed
2. **JavaFX Property 바인딩**:
   - GameEngine의 `EventLog.events()`를 `EventLogPanel.ListView`에 바인딩
   - 카드의 생사 상태도 Observable로 만들어 자동 갱신
3. **`Platform.runLater`**: Engine 스레드 → UI 스레드 안전한 갱신

### 검증

- "▶ Play" 누르면 자동으로 게임 진행 (1초/페이즈)
- 채팅/투표/사망 이벤트가 EventLogPanel에 시간순 추가
- 카드 색이 사망 시 어둡게 변함
- 게임 종료 시 모든 카드의 역할 공개

### 커밋

```bash
git add mafia-client/
git commit -m "SimGUI: GameEngine과 UI 연결 (Observable 바인딩 + Platform.runLater)"
```

---

## Task 5: 통계 누적 + 자동 새 게임

여러 게임 연속 진행. StatsPanel에 승률 누적.

**Files:**
- Modify: `mafia-client/src/main/java/GUI/sim/SimulationController.java`
- Modify: `mafia-client/src/main/java/GUI/sim/StatsPanel.java`

### 핵심 작업

- 게임 종료 시 다음 게임 자동 시작 (잠시 멈춤 후 새 인스턴스)
- 누적 카운터: 시민 승수 / 마피아 승수 / 총 게임수
- `StatsPanel`이 카운터 IntegerProperty에 바인딩
- "🔁 새 게임" 버튼은 즉시 종료 + 새 게임 시작

### 검증

- "▶ Play" 후 게임 여러 판 연속 진행
- StatsPanel: "시민 5승 (50%) / 마피아 5승 (50%)" 같은 누적
- "⏸"로 진행 멈춤 가능
- "🔁 새 게임"으로 즉시 새 게임

### 커밋

```bash
git add mafia-client/
git commit -m "SimGUI: 게임 자동 연속 진행 + 누적 승률 통계"
```

---

## Task 6 이후: 알고리즘 정교화 (사용자와 함께)

다음 단계는 사용자님과 함께 구현. 큰 그림만 outline:

### Task 6: RuleBasedAgent (1주)
- 단순 if-then 봇
- 발언도 의미 있는 템플릿 ("P3이 어제 의심받았다", "내가 의사다" 등)
- 결과: RuleBased가 Random보다 승률 높은지 통계로 확인

### Task 7: CSPAgent 기본 (2-3주)
- 제약(constraint) 추출 로직
- CSP solver (Java 단순 구현 또는 Choco Solver)
- InferenceState 채움
- 결과: 추론 패널에 도메인 reduction이 진짜 데이터로 갱신

### Task 8: 추론 패널 시각화 (1주)
- 카드 클릭 → 선택 → 추론 패널 그 봇의 데이터로
- 도메인 막대 그래프
- 최근 제거된 가설 표시
- 다음 행동 후보 + 확신도

### Task 9: LLMSpeechAgent (1-2주)
- 관전 모드 토글 시 LLM 호출 (Python AI 서비스)
- 비용/속도 trade-off 안내
- 통계 모드는 템플릿 유지

### Task 10: BenchmarkRunner (1주)
- 100/1000판 일괄 실행 (UI 없이)
- 봇 조합별 승률 비교
- CSV/JSON export

각 Task는 작업 시작 시점에 별도 spec/plan으로 정리. 사용자님과 함께 진행.

---

## Self-Review Checklist (Task 1-5 끝나면)

- [ ] 5 module 빌드 모두 성공 (`mvn install`)
- [ ] `SimulateOnce` 콘솔에서 5봇 게임 1판 완주
- [ ] `SimulationMain`으로 GUI 진입
- [ ] ▶ Play → 자동 진행 → 게임 종료 → 새 게임 자동 시작
- [ ] EventLogPanel에 시간순 이벤트 누적
- [ ] StatsPanel에 누적 승률 갱신
- [ ] 사망 카드 흐릿 표시 + 종료 시 역할 공개
- [ ] ⏸ / ⏭ Step 정상 동작
- [ ] 코드 모든 모듈에 적절히 분배 (engine은 UI 무관)
