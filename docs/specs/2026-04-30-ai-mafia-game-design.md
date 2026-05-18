# AI 마피아 게임 설계 문서

> Java Swing + TCP 소켓 + MySQL + OpenAI Function Calling 기반 멀티플레이어 마피아 게임

## 1. 프로젝트 개요

- **목적**: TCP 소켓 통신, DB 연동, AI 에이전트(Function Calling) 구조를 학습하기 위한 멀티플레이어 마피아 게임
- **기술 스택**: Java Swing, TCP Socket, MySQL, OpenAI API (Function Calling)
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

### 제공 함수 목록

| 함수 | 사용 시점 | 설명 |
|------|-----------|------|
| `발언(내용)` | 낮 토론 | 채팅에 메시지 전송 |
| `투표(대상)` | 낮 투표 | 처형 대상 지목 |
| `킬지목(대상)` | 밤 (마피아만) | 죽일 대상 선택 |
| `조사(대상)` | 밤 (경찰/정병) | 대상의 마피아 여부 확인 |

### AI에게 매 턴 넘기는 정보 (프롬프트)

- 너의 역할 (정병에게는 "경찰"이라고 알려줌)
- 생존자 목록
- 지금까지의 채팅 로그
- 현재 페이즈 (토론/투표/밤)
- 사용 가능한 함수 목록
- 이전 밤 행동 결과 (경찰/정병에게만)

### API 구조

- OpenAI API Function Calling 사용
- `AgentInterface`로 추상화하여 나중에 Claude API 등으로 교체 가능

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
