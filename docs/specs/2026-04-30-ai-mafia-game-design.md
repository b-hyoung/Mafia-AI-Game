# AI 마피아 게임 설계 문서

> Java Swing + TCP 소켓 + MySQL + OpenAI Function Calling 기반 멀티플레이어 마피아 게임

## 1. 프로젝트 개요

- **목적**: TCP 소켓 통신, DB 연동, AI 에이전트(CSP + LLM) 구조를 학습하기 위한 멀티플레이어 마피아 게임
- **기술 스택**:
  - 클라이언트: JavaFX (구 Swing에서 교체)
  - 서버: Java + TCP Socket
  - DB: MySQL
  - AI 서비스: Python 마이크로서비스 (CSP solver + LLM Function Calling)
- **플레이어 수**: 5명
- **개발 단계**:
  - 1단계: 사람 1명 + 봇 4명 (AI 에이전트 구현에 집중)
  - 2단계: 사람 4명 + 봇 1명 (멀티플레이어 네트워크 확장)
- **방 구조**:
  - 1단계: 단일 방 (서버 켜면 바로 게임 시작)
  - 2단계: 로비 + 방 생성/참여

## 2. 역할 구성 (5명, 랜덤 배정)

| 역할 | 인원 | 능력 |
|------|------|------|
| 마피아 | 2명 | 밤에 킬 대상 지목 (2명 합의) |
| 시민 | 1명 | 특수 능력 없음 |
| 경찰 | 1명 | 밤에 1명 조사 (마피아 여부 확인) |
| 정병 | 1명 | 경찰이라고 믿지만 실제로는 시민. 조사 시 랜덤 결과 반환 |

### 정병 디테일

- AI에게 "너는 경찰이다"라고 알려줌
- `조사()` 호출 시 서버가 랜덤 결과 반환
- 정병 AI는 그 결과를 진짜라고 믿고 토론에서 발언
- DB에는 실제 역할(PSYCHO)과 가짜 역할(POLICE) 모두 기록

## 3. 게임 흐름

```
[역할 배정] → [낮: 자유 채팅 토론] → [낮: 투표로 처형] → [밤: 역할별 행동] → 반복
```

### 낮 - 토론

- 전체 채팅 (자유 텍스트)
- 사람: Swing 채팅 UI로 입력
- AI: OpenAI API로 텍스트 생성 후 발언

### 낮 - 투표

- 처형 대상 투표 (과반수로 확정)
- 사람: 버튼 클릭
- AI: `투표(대상)` 함수 호출

### 밤 - 역할별 행동

- 마피아: `킬지목(대상)` - 2명이 같은 대상이면 확정, 다르면 재투표 or 랜덤
- 경찰: `조사(대상)` - 마피아 여부 진짜 결과 반환
- 정병: `조사(대상)` - 랜덤 결과 반환
- 마피아끼리만 보이는 밤 채팅으로 상의

### 승리 조건

- 마피아 전원 사망 → 시민 팀 승리
- 마피아 수 >= 시민 수 → 마피아 팀 승리

## 4. AI 에이전트 설계

### 4.1 추론 알고리즘: CSP4SDG (Constraint Satisfaction Problem for Social Deduction Games)

봇이 "누가 마피아인가?"를 추론할 때 **CSP 기반**으로 동작한다. LLM 단독(블랙박스)이 아닌 형식적 추론을 채택한 이유:

- **결정적 (deterministic)**: 같은 입력 → 같은 결정. 디버깅 / 일관성 좋음
- **계산 효율적**: constraint propagation으로 경우의 수 폭발 회피 (Monte Carlo류와 대조)
- **해석 가능**: 어떤 제약이 어떤 가설을 배제했는지 추적 가능
- **시각화 친화적**: 사용자 사이드바에 봇의 추론 과정 실시간 표시 가능
- **API 비용 ↓**: 추론은 로컬 CSP solver, LLM은 자연어 입출력만 보조

### 4.2 작동 원리

| CSP 요소 | 마피아 게임 매핑 |
|---|---|
| 변수 | 각 플레이어 (P1, P2, ..., P5) |
| 도메인 | 가능한 역할 집합 {마피아, 시민, 경찰, 의사, 정병} |
| 제약 (constraint) | 발언/사건 (예: "P1이 P2를 의사로 지목했는데 P2가 밤에 살해됨 → P1의 의사 가설 모순") |

라운드 진행에 따라 제약이 누적 → 각 변수의 도메인이 좁아짐 → 마피아 후보 식별.

### 4.3 LLM의 역할 (보조)

봇의 추론 자체는 CSP solver. LLM은 두 가지 보조 역할만:

1. **자연어 발언 → 형식적 제약 변환** (입력): "저는 어제 P3을 봤어요" → `constraint(P1.role==경찰 && P3.role==마피아)` 같은 형식
2. **결정 → 자연어 발언 생성** (출력): `vote(P3, reason="모순된 알리바이")` → "P3씨, 어제 발언과 오늘 발언이 모순됩니다. 의심스러워요."

→ LLM API 호출이 게임 한 판당 수 회로 제한됨. 비용/속도 개선.

### 4.4 제공 함수 (Function Calling 호환)

| 함수 | 사용 시점 | 설명 |
|------|-----------|------|
| `발언(내용)` | 낮 토론 | 채팅에 메시지 전송 |
| `투표(대상)` | 낮 투표 | 처형 대상 지목 |
| `킬지목(대상)` | 밤 (마피아만) | 죽일 대상 선택 |
| `조사(대상)` | 밤 (경찰/정병) | 대상의 마피아 여부 확인 |

### 4.5 봇 추론 시각화 (학습 + 재미)

게임 UI 사이드바에 봇의 현재 CSP 상태를 실시간 표시:

```
🤖 AI 봇의 추론
─────────────────────
P1  [의사] 80%  [시민] 20%
P2  💀 (밤 1)
P3  [마피아] 60%  [시민] 40%
P4  [경찰] 90%

최근 제거된 가설:
✗ P3 = 의사 (의사 살해됨, 모순)
✗ P1 = 경찰 (조사 결과 다름)

다음 투표 후보: P3 (60%)
```

- 도메인 reduction 막대 그래프
- 제거된 가설과 모순 이유
- 다음 행동 후보 + 확률

봇이 "왜 그 결정"하는지 사용자가 즉시 이해 → 학습 자료 + 게임 몰입 동시.

### 4.6 단계적 도입 (Phase 계획)

| Phase | 봇 구현 수준 |
|---|---|
| Phase 4 (게임 진행) | Rule-based 봇 (단순 if-then) — 게임 흐름 검증 |
| Phase 5 (AI 첫 도입) | 순수 LLM 봇 — Werewolf Arena 패턴, 동작 빠른 확인 |
| Phase 6 (정교화) | LLM + 단순 Bayesian 또는 CSP 도입 |
| Phase 7 (CSP4SDG) | CSP + LLM 보조 통합 — 본 spec의 완성 형태 |
| Phase 8 | 봇 강도 조절 + 시각화 패널 |

### 4.7 AI 서비스 분리 (Python 마이크로서비스)

CSP solver + LLM 호출은 **Python 마이크로서비스**로 분리:

```
[Java 게임 서버] ←HTTP/REST→ [Python AI 서비스]
                                   ├─ CSP solver (python-constraint, or-tools)
                                   └─ LLM 호출 (openai SDK)
```

이유: Python 생태계가 AI/LLM에 압도적. Java 게임 서버는 게임 진행에 집중.

### 4.8 참고 자료

- **CSP4SDG (2024)**: "Constraint and Information-Theory Based Role Identification in Social Deduction Games with LLM-Enhanced Inference" (arxiv 2511.06175)
- **Optimal Strategy in Werewolf Game (2024)**: Bayesian equilibrium 분석 (arxiv 2408.17177)
- **Learning to Discuss Strategically (NeurIPS 2024)**: One Night Ultimate Werewolf

→ 우리 봇은 CSP4SDG 패턴을 따르되 단순화된 형태로 시작 → 단계적으로 정교화.

## 5. 네트워크 & 통신 설계

### TCP 소켓 구조

```
[클라이언트1 (사람)] ──TCP──┐
[클라이언트2 (봇)]  ──TCP──┤
[클라이언트3 (봇)]  ──TCP──┼── [게임 서버]
[클라이언트4 (봇)]  ──TCP──┤
[클라이언트5 (봇)]  ──TCP──┘
```

- 서버는 클라이언트마다 스레드 하나 할당
- 봇도 사람과 동일하게 소켓으로 접속 (별도 프로세스 or 스레드)
- 사람이든 봇이든 서버 입장에서는 똑같은 클라이언트

### 메시지 프로토콜 (JSON + 줄바꿈 구분)

```json
// 서버 → 클라이언트: 페이즈 변경
{"type": "PHASE_CHANGE", "phase": "DAY_DISCUSSION", "survivors": ["P1","P2","P3","P4","P5"]}

// 클라이언트 → 서버: 채팅
{"type": "CHAT", "sender": "P1", "message": "P3 수상해요"}

// 클라이언트 → 서버: 행동
{"type": "ACTION", "action": "투표", "target": "P3"}

// 서버 → 클라이언트: 밤 결과
{"type": "NIGHT_RESULT", "dead": "P2"}
```

## 6. DB 설계

```sql
-- 유저 정보 & 전적
users (
    user_id       INT PRIMARY KEY AUTO_INCREMENT,
    nickname      VARCHAR(20),
    wins          INT DEFAULT 0,
    losses        INT DEFAULT 0,
    is_bot        BOOLEAN DEFAULT FALSE,
    created_at    DATETIME
)

-- 게임 기록
games (
    game_id       INT PRIMARY KEY AUTO_INCREMENT,
    winner_team   ENUM('CITIZEN', 'MAFIA'),
    started_at    DATETIME,
    ended_at      DATETIME
)

-- 게임 참가자
game_players (
    game_id       INT,
    user_id       INT,
    role          ENUM('MAFIA', 'CITIZEN', 'POLICE', 'PSYCHO'),
    fake_role     ENUM('MAFIA', 'CITIZEN', 'POLICE') NULL,
    is_survived   BOOLEAN,
    PRIMARY KEY (game_id, user_id)
)

-- 게임 로그 (리플레이용)
game_logs (
    log_id        INT PRIMARY KEY AUTO_INCREMENT,
    game_id       INT,
    round         INT,
    phase         ENUM('DAY_DISCUSSION', 'DAY_VOTE', 'NIGHT'),
    actor_id      INT,
    action_type   VARCHAR(20),
    target_id     INT NULL,
    message       TEXT NULL,
    created_at    DATETIME
)

-- AI 판단 로그 (디버깅/튜닝용)
ai_logs (
    ai_log_id     INT PRIMARY KEY AUTO_INCREMENT,
    game_id       INT,
    round         INT,
    user_id       INT,
    prompt_sent   TEXT,
    response      TEXT,
    function_called VARCHAR(20),
    function_args   TEXT,
    created_at    DATETIME
)
```

## 7. 프로젝트 구조

```
mafia-game/
├── server/
│   ├── MafiaServer.java          -- 소켓 서버, 클라이언트 접속 관리
│   ├── GameRoom.java             -- 게임 진행 로직 (턴, 페이즈 관리)
│   ├── GamePhase.java            -- 페이즈 enum
│   ├── RoleAssigner.java         -- 역할 랜덤 배정
│   ├── VoteManager.java          -- 투표 집계
│   ├── NightActionHandler.java   -- 밤 행동 처리 (킬, 조사, 정병 가짜결과)
│   └── MessageProtocol.java      -- JSON 메시지 파싱/생성
│
├── client/
│   ├── MafiaClient.java          -- 소켓 연결, 메시지 송수신
│   ├── GameUI.java               -- Swing 메인 화면
│   ├── ChatPanel.java            -- 채팅 패널
│   ├── VotePanel.java            -- 투표 버튼 UI
│   └── ResultPanel.java          -- 게임 결과/리플레이 화면
│
├── bot/
│   ├── BotClient.java            -- 봇용 소켓 클라이언트
│   ├── AgentInterface.java       -- 에이전트 인터페이스 (API 교체 대비)
│   ├── OpenAIAgent.java          -- OpenAI Function Calling 구현
│   └── FunctionRegistry.java     -- 사용 가능한 함수 목록 관리
│
├── db/
│   ├── DBConnection.java         -- MySQL 연결 관리
│   ├── UserDAO.java              -- 유저 CRUD
│   ├── GameDAO.java              -- 게임 기록 저장/조회
│   ├── GameLogDAO.java           -- 게임 로그 저장/조회
│   └── AILogDAO.java             -- AI 판단 로그 저장/조회
│
└── common/
    ├── Message.java              -- 메시지 객체
    ├── Role.java                 -- 역할 enum
    └── GameResult.java           -- 게임 결과 객체
```
