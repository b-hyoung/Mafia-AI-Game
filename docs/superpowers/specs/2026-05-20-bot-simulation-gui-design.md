# Bot Simulation GUI — Design

작성일: 2026-05-20
대상: AI 봇 5명이 마피아 게임을 자동으로 진행하는 관전자 모드 시뮬레이션 GUI

## 1. 컨셉

5명의 AI 봇이 자동으로 마피아 게임을 진행하는 모습을 **시각적으로 관찰**하는 화면. 사용자는 플레이어가 아닌 **관전자**.

핵심 가치:
- 각 봇의 추론 근거 실시간 표시 → 알고리즘 학습/디버깅 도구
- 채팅/투표/사망/승률 시각화 → 게임 흐름 직관적 파악
- 봇 종류별 비교 가능 (Random vs RuleBased vs CSP vs LLM)
- 통계 누적 → 알고리즘 강도 정량 평가

## 2. 두 가지 실행 모드

### 🔬 통계 모드 (Statistics Mode)
- 1000판 빠르게 자동 시뮬레이션
- 채팅은 **템플릿 발언** (LLM 호출 X)
- 결과: 봇 종류별 승률, 마피아/시민 균형, 평균 라운드 수
- 1000판 소요: 수십 초 ~ 몇 분

### 🎭 관전 모드 (Spectator Mode)
- 1판 천천히 진행 (속도 조절 가능)
- 채팅은 **LLM 호출** (선택 시)
- 추론 패널 실시간 갱신
- 1판 소요: 1~3분 (LLM 켜진 경우)

같은 GameEngine + Agent 코드, **Agent의 `speak()` 구현만 토글**.

## 3. 화면 구성

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Mafia 시뮬레이션               [▶ Play][⏸][⏭ Step][🔁 New][🎲 Seed][⚙]   │
├─────────────────────────────────────────────────────────────────────────────┤
│  Round 2 · Day Vote     속도: ●─────○   [🎭 관전]/[🔬 통계]                   │
│                                                                              │
│   ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐    누적 통계            │
│   │ 🎭   │  │ 🎭   │  │ 🎭   │  │ 💀   │  │ 🎭   │    ────────────         │
│   │alice │  │ bob  │  │chris │  │ dan  │  │ eve  │    시민  60% (12승)    │
│   │ CSP  │  │ Rule │  │ CSP  │  │ Rand │  │ LLM  │    마피아 40% (8승)    │
│   │마피아│  │ ?    │  │ ?    │  │의사  │  │ ?    │                          │
│   └──────┘  └──────┘  └──────┘  └──────┘  └──────┘    현재 게임:           │
│                                                        Round 2/10           │
│                                                        Phase: Day Vote      │
├──────────────────────────────────┬──────────────────────────────────────────┤
│  이벤트 / 채팅 로그               │  AI 추론 (P1 alice 선택됨)               │
│  ─────────────────────           │  ─────────────────────────               │
│  [R1·Day] P1: P3 의심돼요          │  P1 (alice · CSP · 마피아)               │
│  [R1·Day] P2: 어제 P5 봤어요       │                                          │
│  [R1·Day] P3: P1 공격적이네요      │  도메인 (가능한 역할):                    │
│  [R1·Vote] P1→P3, P2→P5, P3→P1   │   P2  [시민]60% [의사]40%                │
│  [R1·Vote] → 3표로 P3 처형        │   P3  [경찰]70% [시민]30%                │
│  [R1·Night] P5 살해 (마피아)      │   P4  💀                                 │
│  [R2·Day] P1: P5도 마피아였네?    │   P5  [의사]50% [시민]50%                │
│  ...                              │                                          │
│                                   │  최근 제거된 가설:                        │
│                                   │  ✗ P2=경찰 (조사 결과 없음)               │
│                                   │  ✗ P5=마피아 (자기 살해됨)                │
│                                   │                                          │
│                                   │  다음 행동 후보: Vote P3 (의심 70%)       │
└───────────────────────────────────┴──────────────────────────────────────────┘
```

### 영역별 역할

| 영역 | 책임 |
|---|---|
| 상단 컨트롤 | Play/Pause/Step/New/Seed/Settings |
| 플레이어 카드 5개 | 닉네임, 봇 종류, 역할 (게임 끝나면 공개), 생사 |
| 이벤트 로그 | 발언/투표/밤 행동 시간순 |
| 추론 패널 | 선택된 봇의 도메인/제거된 가설/다음 행동 후보 |
| 통계 패널 | 누적 승률, 현재 게임 진행도 |

## 4. 데이터 흐름 (JavaFX Property 바인딩)

```
GameEngine.play() (별도 스레드)
    ↓ 한 페이즈 끝나면
ObservableList<Event> log.append(...)
ObservableMap<Player, InferenceState> inference.put(...)
    ↓ Platform.runLater
JavaFX UI 자동 갱신 (Property binding)
```

기존 LobbyScene의 polling 패턴을 그대로 활용. EventLog/InferenceState가 Observable이라 데이터만 바뀌면 UI 자동 갱신.

## 5. 컴포넌트 구조

### 새 module: `mafia-engine`

```
mafia-engine/
  src/main/java/mafia/engine/
    GameEngine.java          // play(), nextPhase(), isOver()
    GameState.java           // 현재 게임 상태 (라운드, 페이즈, 생존자)
    GameView.java            // 한 Agent가 볼 수 있는 정보 (hidden 필터링)
    Event.java               // sealed interface (Speak, Vote, Kill, PhaseEnded, ...)
    EventLog.java            // ObservableList<Event>
    RoleAssigner.java        // 랜덤 역할 배정
    VoteCounter.java         // 투표 집계 + 동점 처리
    WinCondition.java        // 시민/마피아 승리 판정
    GameResult.java          // 1게임 결과 (winner, rounds, log)
```

### 새 module: `mafia-agent`

```
mafia-agent/
  src/main/java/mafia/agent/
    Agent.java                  // interface
    AbstractAgent.java          // 공통 코드
    RandomAgent.java            // 베이스라인 (무작위)
    RuleBasedAgent.java         // 단순 if-then
    CSPAgent.java               // CSP 추론 (Phase D)
    LLMSpeechAgent.java         // LLM 발언 wrapper (decorator)
    InferenceState.java         // 추론 스냅샷 (시각화용)
```

### 새 module: `mafia-simulation`

```
mafia-simulation/
  src/main/java/mafia/simulation/
    GameSimulator.java          // 1게임 시뮬레이션
    BenchmarkRunner.java        // N게임 통계 수집
    BenchmarkResult.java        // 결과 (승률, 평균 라운드)
```

### `mafia-client`에 추가

```
mafia-client/src/main/java/GUI/sim/
  SimulationScene.java          // 전체 화면
  PlayerCard.java               // 카드 컴포넌트
  EventLogPanel.java            // 이벤트/채팅 로그
  InferencePanel.java           // AI 추론 표시
  StatsPanel.java               // 누적 통계
  ControlBar.java               // 상단 컨트롤
  SimulationController.java     // play/pause/step 제어 + Engine 연결
```

## 6. 핵심 인터페이스

### `Agent` 인터페이스
```java
public interface Agent {
    Decision decide(GameView view);      // 페이즈별 행동 (투표/킬/조사)
    String speak(GameView view);          // 자연어 발언
    void observe(Event event);            // 새 이벤트 수신
    InferenceState getInference();        // 추론 상태 스냅샷 (시각화)
    String botType();                     // "RANDOM" / "RULE" / "CSP" / "LLM"
}
```

`GameView`가 Agent에 전달되는 정보를 hidden information까지 제한 (자기 역할만 알고 다른 사람 역할 모름).

### `InferenceState`
```java
public class InferenceState {
    private Map<Player, Map<Role, Double>> domains;   // P1 → {citizen: 0.6, mafia: 0.4}
    private List<EliminatedHypothesis> eliminated;     // 최근 제거된 가설
    private Player nextActionTarget;                   // 다음 투표/킬 대상
    private double targetConfidence;                   // 0~1
    // ...
}
```

Random/RuleBased는 단순한 InferenceState (또는 비어있음). CSPAgent부터 의미 있는 정보 채움.

### `Event` (sealed)
```java
public sealed interface Event permits Speak, Vote, NightKill, Investigate, ... {
    Round round();
    Phase phase();
    Player actor();
    Instant timestamp();
}
```

## 7. 단계별 구현 (Plan에서 상세)

| Step | 범위 | 결과 |
|---|---|---|
| **A** | mafia-engine 분리 + RandomAgent | 콘솔에서 5봇 게임 1판 진행 |
| **B** | SimulationScene UI 뼈대 + 더미 데이터 | GUI에 화면 보임 (데이터는 fake) |
| **C** | Engine ↔ UI 연결 + 자동 진행 | 진짜 게임 진행이 GUI에 보임 |
| **D** | RuleBasedAgent + 템플릿 발언 | 의미 있는 채팅 + 통계 |
| **E** | CSPAgent + 추론 패널 | 본격 알고리즘 + 시각화 ★ |
| **F** | LLMSpeechAgent (관전 모드) | 자연스러운 채팅 |
| **G** | BenchmarkRunner + 통계 모드 | 1000판 자동 시뮬레이션 |

사용자님 의도: **B + C까지 빠르게 UI 보고**, 그 이후(D-G)는 사용자님과 함께 알고리즘 구현.

## 8. 진입점

`MainGame`에서 시뮬레이션 화면으로 어떻게 진입할까?

옵션:
- 로그인 화면에 "🤖 시뮬레이션 모드" 버튼 추가
- 또는 별도 main 클래스 (`SimulationMain`)
- 또는 로비에 "관전 모드" 진입

가장 단순: **별도 main 클래스**. `mvn -pl mafia-client exec:java -Dexec.mainClass=GUI.sim.SimulationMain` 으로 실행. 게임 메인 화면(로그인/로비)과 독립적.

이후 통합 원하면 로그인 화면에서 진입로 추가 가능. 일단 분리 시작이 안전.

## 9. 스코프 (이번 spec이 안 다루는 것)

- Phase 2 (LOBBY 서버 연결) — 별도 sub-project
- 사용자가 게임 참여 (HumanAgent) — 미래
- 봇 강도 조절 (난이도 옵션) — Phase G 후
- 봇 vs 사람 진짜 게임 — Phase 4 통합 시
- 게임 결과를 DB에 저장 — Phase 4
- 시뮬레이션 결과 영상/리플레이 export — 미래

## 10. 성공 기준

### Step B+C 끝:
- `mvn -pl mafia-client exec:java -Dexec.mainClass=GUI.sim.SimulationMain`로 SimulationScene 진입
- 5개 플레이어 카드 + 이벤트 로그 패널 + 통계 패널 표시
- "▶ Play" 누르면 자동으로 게임 진행 (1초/페이즈)
- 1게임 종료 시 통계 갱신 + 자동으로 새 게임 시작 (또는 정지)
- 이벤트 로그에 발언/투표/밤행동 시간순 누적
- 게임 종료 시 모든 카드의 진짜 역할 공개

### Step E 끝 (CSP + 추론 패널):
- CSPAgent가 봇 5명 중 일부 (또는 전체) 담당
- 카드 클릭 시 그 봇의 InferenceState가 추론 패널에 표시
- 라운드 진행에 따라 추론 패널이 실시간 갱신
- 최근 제거된 가설 표시 ("✗ P2=경찰 (조사 결과 없음)")

### Step F 끝 (LLM 통합):
- 관전 모드 토글 시 LLM 채팅 활성화
- 발언이 자연스러운 한국어 (템플릿 X)
- 발언 생성 중 UI 멈춤 X (백그라운드 스레드)

## 11. 참고

- 학술: CSP4SDG (2024), Werewolf Arena (2024), Learning to Discuss Strategically (NeurIPS 2024)
- 우리 design.md Section 4 (AI 에이전트 설계) 와 일관
- LobbyScene의 polling 패턴을 그대로 재사용 (ObservableList + Platform.runLater)
