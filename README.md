# AI 마피아 게임

> 6인 마피아 게임. 각 AI 에이전트가 자기 역할만 알고 자율적으로 추리/연기하며 유저와 함께 플레이. 싱글/멀티(TCP) 모두 지원.

---

## 1. 프로젝트 개요

### 컨셉
- 자바 + DB 기반 응용 프로그램 (학교 과제)
- AI 서비스 융합 (가산점 요소)
- 멀티플레이어 (LAN/인터넷)
- 캐릭터 일러스트 + 2상태 애니메이션

### 핵심 차별점
- **AI는 본인 역할만 안다** — 다른 AI의 정체는 본인도 추리해야 함
- **정신병자(정병) 메커니즘** — AI가 거짓 역할을 진심으로 믿음 → 자연스러운 행동 미스매치
- **실시간 발언 파이프라인** — AI 발언이 끊김 없이 이어짐 (스트리밍 + 사전 계산)

---

## 2. 기술 스택

| 영역 | 기술 |
|---|---|
| 언어 | Java 21+ (Virtual Threads 활용) |
| GUI | JavaFX 21+ |
| DB | MySQL 8.x |
| LLM | Ollama (개발: `qwen2.5:7b`) → Claude API (배포 옵션) |
| JSON | Jackson |
| 네트워크 | Java TCP Socket (`java.net.Socket`) |
| 의존성 | Maven 또는 Gradle |

### 외부 의존성
- JavaFX 21+ (UI)
- OkHttp 또는 Java HttpClient (Ollama HTTP)
- Jackson Databind (JSON 직렬화)
- MySQL Connector/J (JDBC)
- JUnit 5 (테스트)

(JPA/Hibernate, Spring 등 헤비 프레임워크는 의도적으로 제외)

---

## 3. 게임 룰

### 3.1 인원 구성

총 **6명** (유저 1-6명 + AI로 채움)

| 진영 | 인원 | 구성 |
|---|---|---|
| 마피아 | 2명 | MAFIA |
| 시민 | 3명 | 경찰 1 + 의사 1 + [조사관 / 정병 / 일반시민] 중 1 |
| 중립 | 1명 | 살인자 (KILLER) |

### 3.2 역할 능력

| 역할 | 능력 | 비고 |
|---|---|---|
| **마피아** (MAFIA) | 협의 후 매 밤 1명 처형. 마피아끼리는 서로 알며 비밀방에서 대화 | 2명 |
| **경찰** (POLICE) | 매 밤 [차단 / 저격] 중 하나 선택<br>- **차단**: 1명 지목 → 그날 밤 그 사람의 모든 능력 봉인. 차단당한 사람에게 알림.<br>- **저격**: 게임당 1발. 시민 사살 시 본인 즉시 자살 | |
| **의사** (DOCTOR) | 1명 지목 → 그 밤의 마피아/살인자 공격 무효화. 보호받은 사람에게 "너는 살았다" 알림 | 자기 자신 보호 가능 |
| **조사관** (INVESTIGATOR) | 1명 지목 → 직업 후보 2개 제시 (진짜 직업 + 페이크 1개) | |
| **정병** (IMPOSTOR) | actualRole = IMPOSTOR (시민 진영). perceivedRole ∈ {경찰, 의사, 조사관, 일반시민, 살인자} 중 랜덤 배정. 본인은 perceivedRole이라 진심으로 믿음. 능력은 헛스윙 (효과 없지만 본인은 모름) | 게임에 있을 수도 없을 수도 |
| **일반시민** (CITIZEN) | 능력 없음. 토론과 투표로만 기여 | |
| **중립 살인자** (KILLER) | 매 밤 1명 처형 (마피아와 별개). 모든 다른 진영 제거 시 승리 | 혼자 승리 |

### 3.3 승리 조건

- **시민 진영 승**: 마피아 + 살인자 모두 사망
- **마피아 승**: 살아있는 마피아 ≥ 시민 진영 인원
- **살인자 승**: 본인 외 모두 사망

승리 체크 시점: `DAY_ANNOUNCE` 진입 시 + `EXECUTION` 직후

### 3.4 정병 메커니즘 (핵심)

#### 두 종류 역할 분리
- `actualRole` — 게임 로직이 사용 (정병은 IMPOSTOR)
- `perceivedRole` — LLM 프롬프트에 들어감 (정병은 다른 역할)
- 정병만 둘이 다름. 다른 역할은 actual = perceived

#### 페이크 결과 처리 (정책 B + D 하이브리드)
정병이 자기 perceivedRole 능력을 사용하면, 시스템이 **그럴듯하지만 살짝 비일관적인 페이크 결과**를 줌:

| perceived | 페이크 결과 처리 |
|---|---|
| POLICE (조사) | 40% 확률로 "마피아", 60% "시민". 가끔 모순 (어제 마피아라 한 사람이 오늘 시민으로) |
| DOCTOR | "보호 성공"이라고만 표시 (실제 효과 없음) |
| INVESTIGATOR | 페이크 직업 후보 2개 (시스템이 임의 생성) |
| KILLER | "공격 성공" 표시. **다음 날 대상이 멀쩡** → 자각의 핵심 단서 |
| CITIZEN | 능력 없음 |

#### 자각 가능성
- 눈치 빠른 LLM은 모순/대상 멀쩡함을 보고 "내가 정병인가?" 자각 가능
- 진짜 시민도 자기를 정병이라 잘못 의심할 수 있음 (LLM 자유 추론)
- 시스템은 정병에게 절대 "당신은 정병입니다" 알리지 않음

### 3.5 밤 알림 시스템

본인에게 영향 미친 행동은 다음 날 아침 개인 알림:
- 차단당함 → "누군가 너를 막았다"
- 의사가 살림 → "누군가 너를 살렸다" (= 자기가 표적이었음을 시사)
- 정병이 능력 헛스윙 시 → 대상에게 알림 안 감 (효과 자체가 없으니)

---

## 4. 게임 흐름 (상태 머신)

### 4.1 Phase 정의

```
GAME_START         역할/페르소나 배정
NIGHT              밤: 역할별 행동 입력 (30초)
NIGHT_RESOLVE      서버 행동 적용 (페이크 결과 포함)
DAY_ANNOUNCE       아침 발표 + 승리 체크 ★
DAY_DISCUSSION     자유 토론 (실시간 카운트다운, ~3분)
DAY_VOTE_1         1차 투표 - 처형 후보 지목 (30초)
DAY_VOTE_1_RETRY   동률 시 재투표 (1회만)
DAY_DEFENSE        피지목자 최후 변론 (30초)
DAY_VOTE_2         2차 찬반 투표 (20초)
EXECUTION          처형 적용 + 승리 체크 ★
GAME_END           통계 저장
```

### 4.2 전환 규칙 (1일차 밤부터 시작)

```
GAME_START
   ▼
NIGHT (1일차) ◄─────────────────────────┐
   ▼                                     │
NIGHT_RESOLVE                            │
   ▼                                     │
DAY_ANNOUNCE  ──[승리]──► GAME_END       │
   ▼                                     │
DAY_DISCUSSION                           │
   ▼                                     │
DAY_VOTE_1                               │
   ├─[동률]─► DAY_VOTE_1_RETRY           │
   │              ├─[동률]──────────────►│ (처형 무산)
   │              └─[1위]─┐              │
   ├─[1위]──────────► DAY_DEFENSE        │
   └─[기권 다수]──────────────────────  ►│
                       ▼                 │
                  DAY_VOTE_2             │
                       ├─[찬성 과반]─► EXECUTION
                       │                 ▼
                       │            [승리]── GAME_END
                       │                 │
                       └─[반대/동률]─────┴──►NIGHT
```

### 4.3 페이싱 (목표 ~15분/판)

| 단계 | 시간 |
|---|---|
| 낮 자유 토론 | 2-3분 (실시간 카운트다운) |
| 1차 투표 | 30초 |
| 변론 | 30초 |
| 2차 찬반 | 20초 |
| 밤 (역할 행동) | 30초 (AI는 즉시) |
| 아침 발표 | 10초 |
| 사이클당 | ~4-5분 × 3사이클 ≈ 12-15분 |

### 4.4 입력 게이트 (서버 검증)

| Phase | 유효 입력 | 누구만 |
|---|---|---|
| NIGHT | NIGHT_ACTION | 행동 가능 역할자 (actualRole 기준 적용, perceivedRole 기준 페이크) |
| DAY_DISCUSSION | USER_SPEAK | 살아있는 모두 (언제든 끼어들기 가능) |
| DAY_VOTE_1 | VOTE { targetId } | 살아있는 모두 (자기 자신 투표 불가) |
| DAY_DEFENSE | USER_SPEAK | **피지목자만** |
| DAY_VOTE_2 | VOTE { agree } | 살아있는 모두 (피지목자 제외) |

### 4.5 투표 방식
**공개 투표** — 누가 누구에게 투표했는지 모두에게 보임 (전통 마피아).

---

## 5. 아키텍처

### 5.1 전체 구조 (Client-Server)

```
┌──────────────────────────────────────────────────────────┐
│                Client (JavaFX GUI)                       │
│   사용자 입력 + 서버 상태 표시 (캐릭터 + 채팅 + 패널)     │
└────────────────────┬─────────────────────────────────────┘
                     │ 「GameClient」 인터페이스
        ┌────────────┴───────────────┐
        │                            │
   ┌────▼──────────┐         ┌───────▼──────────┐
   │ LocalClient   │         │ RemoteClient     │
   │ (싱글)        │         │ (멀티 - TCP)     │
   │ 같은 JVM 직접 │         │ TCP 소켓 + JSON  │
   └────┬──────────┘         └───────┬──────────┘
        │                            │
        ▼                            ▼
   ┌──────────────────────────────────────────────┐
   │         Server (호스트가 실행)                │
   │  GameOrchestrator                            │
   │  ├─ PhaseManager                             │
   │  ├─ ConversationManager (turn-taking)        │
   │  └─ NightResolver (행동 + 페이크 결과)       │
   │       │                                      │
   │       ├─→ Domain (Game, Player, Role)        │
   │       ├─→ AI Service (AIAgent, OllamaClient) │
   │       └─→ DB (DAO, MySQL)                    │
   └──────────────────────────────────────────────┘
```

### 5.2 핵심 추상화

- **GameClient 인터페이스**: GUI는 싱글이든 멀티든 같은 메서드 사용. 모드 전환 깔끔.
- **LLMClient 인터페이스**: Ollama → Claude API 교체 가능 (개발→배포 전환)
- **CompletableFuture 기반 비동기**: 모든 LLM 호출은 cancel() 가능

### 5.3 의존 방향

```
GUI       → client/ (구현체 미인지)
GUI       → domain/ (게임 상태 표시)
GUI       → protocol/ (메시지 송수신)

client/Local  → server/ (싱글: 직접 호출)
client/Remote → protocol/ (멀티: TCP 직렬화)

server/   → domain/, ai/, db/, protocol/
ai/       → domain/, db/(페르소나 조회)
db/       → domain/
```

### 5.4 멀티플레이어 모델

- **호스트 모드**: 한 명이 방 만들기 → 그 PC가 서버 → 다른 사람들 IP로 접속
- **네트워크**: LAN + 인터넷 둘 다 (IP 입력 방식)
- **LLM 위치**: 호스트 PC 또는 별도 LLM 서버 PC (`OllamaClient`가 IP 설정 가능)
- **인원 구성**: 항상 6명 고정. 인간 1-6명 + 부족분 AI로 채움
- **인증**: 회원가입/로그인 (DB user 테이블 사용)

---

## 6. 패키지 구조

```
src/
├── GUI/                       ← 클라이언트 UI (JavaFX)
│   ├── MainGame.java          (메인 진입점)
│   ├── LoginPanel.java
│   ├── LobbyPanel.java        (멀티: 방 목록/IP 접속)
│   ├── RoomPanel.java         (대기실)
│   ├── GameSetupPanel.java    (호스트 설정)
│   ├── GamePanel.java         (메인 게임 화면)
│   ├── ChatPanel.java
│   ├── VotePanel.java
│   ├── DefensePanel.java
│   ├── NightActionPanel.java
│   ├── CharacterView.java     (캐릭터 일러스트 + 애니메이션)
│   ├── GameEndPanel.java
│   └── StatsPanel.java
│
└── mafia/
    │
    ├── domain/                ← 공유 모델 (클라+서버 공통)
    │   ├── Game.java
    │   ├── Player.java
    │   ├── Role.java          (enum + 능력 정의)
    │   ├── Persona.java
    │   ├── User.java
    │   ├── NightAction.java
    │   ├── Vote.java
    │   ├── Decision.java      (AI 판단 로그 1건)
    │   └── PlayerView.java    (클라 시점 제한된 게임 상태)
    │
    ├── protocol/              ← 양쪽 다 사용
    │   ├── ClientMessage.java (Login/Vote/Speak/NightAction)
    │   ├── ServerMessage.java (PhaseChange/PlayerSpeak/Result)
    │   └── MessageCodec.java  (Jackson JSON 직렬화)
    │
    ├── client/                ← 클라이언트 측
    │   ├── GameClient.java    (인터페이스)
    │   ├── LocalGameClient.java   (싱글)
    │   ├── RemoteGameClient.java  (멀티 TCP)
    │   └── ServerMessageListener.java
    │
    ├── server/                ← 서버 측 (호스트가 실행)
    │   ├── GameServer.java         (TCP listener)
    │   ├── ClientSession.java      (클라 1개당 핸들러)
    │   ├── GameOrchestrator.java
    │   ├── PhaseManager.java
    │   ├── ConversationManager.java
    │   └── NightResolver.java
    │
    ├── db/                    ← 서버에서만 사용
    │   ├── DBConnect.java     (POS 스타일, 동시성 안전 수정)
    │   ├── UserDAO.java
    │   ├── GameDAO.java
    │   ├── ParticipantDAO.java
    │   ├── PersonaDAO.java
    │   ├── DecisionDAO.java   (AI 판단 로그)
    │   └── StatsDAO.java
    │
    └── ai/                    ← 서버에서만 사용
        ├── AIAgent.java
        ├── LLMClient.java     (인터페이스)
        ├── OllamaClient.java
        ├── ClaudeClient.java  (V2)
        ├── PromptBuilder.java
        ├── SpeakIntentScorer.java
        └── DecisionLogger.java (AI 판단 → ai_decisions 테이블에 자동 기록)
```

### POS 프로젝트 패턴 준수
- DAO: 싱글톤 (`private constructor` + `getInstance()`)
- DBConnect: POS 패턴 + 정적 conn 변수 제거 (동시성 안전)
- 패키지 분리: GUI / 백엔드 (백엔드는 책임별 서브패키지)

---

## 7. 메시지 프로토콜

### 7.1 형식
- 줄 단위 JSON (UTF-8)
- `type` 필드가 디스크리미네이터 (Jackson 다형성 직렬화)

### 7.2 클라 → 서버 (ClientMessage)

| 카테고리 | 메시지 | 필드 |
|---|---|---|
| 인증 | LOGIN | username, password |
| 인증 | REGISTER | username, password |
| 로비 | CREATE_ROOM | name, settings |
| 로비 | JOIN_ROOM | roomId |
| 로비 | LEAVE_ROOM | - |
| 로비 | SET_READY | ready (bool) |
| 로비 | START_GAME | (호스트만) |
| 게임 | USER_SPEAK | text |
| 게임 | USER_VOTE | phase (VOTE_1/VOTE_2), targetId 또는 agree |
| 게임 | USER_NIGHT_ACTION | action, targetId |
| 게임 | DEFENSE_END | - |

### 7.3 서버 → 클라 (ServerMessage)

| 카테고리 | 메시지 | 라우팅 |
|---|---|---|
| 인증 결과 | LOGIN_RESULT, REGISTER_RESULT | 본인 |
| 로비 | ROOM_STATE, PLAYER_JOINED, PLAYER_LEFT | 방 인원 |
| 게임 시작 | ROLE_ASSIGNED | **개인** (perceivedRole + persona + teammates) |
| Phase | PHASE_CHANGE | 모두 (broadcast) |
| 발언 | PLAYER_SPEAK_BEGIN, _DELTA, _END | channel별 라우팅 (DAY=모두, MAFIA=마피아만) |
| 투표 | VOTE_PROGRESS, VOTE_RESULT | 모두 (공개 투표) |
| 변론 | DEFENSE_START, DEFENSE_END | 모두 |
| 처형 | EXECUTION (역할 공개) | 모두 |
| 밤 | NIGHT_BEGIN, NIGHT_END | 모두 |
| 밤 결과 (개인) | PRIVATE_MESSAGE, INVESTIGATION_RESULT | **개인** |
| 발표 | DAY_ANNOUNCE | 모두 |
| 종료 | GAME_END | 모두 |
| 에러 | ERROR | 본인 |

### 7.4 채널 라우팅 (서버 책임)

| 메시지 | 누구한테 |
|---|---|
| 일반 broadcast | 모두 |
| `PLAYER_SPEAK { channel: "MAFIA" }` | **마피아 진영(actualRole=MAFIA)에게만** |
| `ROLE_ASSIGNED`, `PRIVATE_MESSAGE`, `INVESTIGATION_RESULT` | 본인에게만 |

→ **치트 방지**: 클라가 마피아 채팅을 절대 받지 않음.

### 7.5 스트리밍 발언

```
PLAYER_SPEAK_BEGIN  { speakerId, channel }
PLAYER_SPEAK_DELTA  { speakerId, token }   ← LLM 토큰별 broadcast
PLAYER_SPEAK_DELTA  { speakerId, token }
...
PLAYER_SPEAK_END    { speakerId, fullText }
```

UI는 토큰 도착 시 즉시 추가 → "AI 타이핑 중" 효과.

---

## 8. AI 시스템

### 8.1 핵심 컴포넌트

```
ai/
├── LLMClient.java          (인터페이스: complete + streamComplete)
├── OllamaClient.java       (HTTP 구현)
├── ClaudeClient.java       (V2)
├── AIAgent.java            (AI 플레이어 1명 = 1 인스턴스)
├── PromptBuilder.java      (역할 + 페르소나 + 게임 상태 → 프롬프트)
├── SpeakIntentScorer.java  (발언 욕구 점수 산출)
└── DecisionLogger.java     (모든 AI 판단을 ai_decisions 테이블에 자동 기록)
```

### 모든 LLM 호출은 DecisionLogger를 거침

`AIAgent`의 모든 메서드 (`evaluateSpeakIntent`, `generateSpeech`, `decideNightAction`, `decideVote`)는 LLM 호출 전후에 `DecisionLogger`로 다음을 기록:
- `system_prompt` + `user_prompt` (프롬프트 통째로)
- `raw_response` (LLM 원본 응답)
- `parsed_decision` (파싱된 결과 JSON)
- `model_used`, `duration_ms`

**효과**:
- 게임 종료 후 "이 페르소나가 왜 이 발언했지?" 추적 가능
- 프롬프트 튜닝 시 직전 게임 데이터로 검증
- 페르소나 X 역할 X 행동 패턴 분석
- 정병 자각 시점 추적 ("AI가 자각 의심 표현을 한 첫 번째 발언")

### 8.2 LLMClient 메서드

| 메서드 | 용도 | 비고 |
|---|---|---|
| `complete(req)` | 짧은 응답 (점수, 결정) | CompletableFuture<String> 반환, 비스트리밍 |
| `streamComplete(req, onToken, onComplete)` | 긴 발언 | 토큰 콜백 + 완료 콜백 |

**모두 cancel() 가능** (B → C 파이프라인 확장 대비)

### 8.3 OllamaClient

- `POST http://{host}:11434/api/chat`
- 스트리밍: `stream:true`로 NDJSON 응답
- `host`는 외부 주입 → 같은 PC면 `localhost`, 별도 PC면 다른 IP

### 8.4 AIAgent (AI 플레이어)

각 AI는 자기만의 인스턴스. 메서드:
- `evaluateSpeakIntent(view)` → 발언 욕구 점수 (0-10)
- `generateSpeech(view, onToken, onComplete)` → 풀 발언 (스트리밍)
- `decideNightAction(view)` → 밤 행동 결정
- `decideVote(phase, view)` → 투표 결정

### 8.5 메모리 모델: Stateless

- AI는 상태 무관. 매 호출마다 게임 상태 전체를 프롬프트로 받음
- 이유:
  - 자바 측에서 게임 진실(상태)을 100% 통제
  - LLM이 "기억 안 나는데?" 일관성 깨짐 방지
  - 모델 교체/리트라이 시 동일한 결과 보장
  - 페이크 결과(정병)도 자바가 매번 주입

### 8.6 GameView (정보 격리)

서버는 진실을 다 알지만, AI에게 줄 때 **본인 시점에서 볼 수 있는 정보만 필터링**:

```
GameView {
    dayNumber, phase
    players                  // 살아있음 여부, 이름, persona 공개분
    chatHistory              // 자기가 들을 수 있는 발언만
    publicEvents             // 공개 사망, 처형 등
    personalMessages         // 본인에게만 온 알림
    teammates (마피아만)     // 마피아 동료 ID
    mafiaChatHistory (마피아만)
}
```

AI가 받는 정보 = 사람 플레이어가 화면에서 볼 수 있는 정보 → **공정성 보장**.

### 8.7 PromptBuilder 구성

#### 시스템 프롬프트 (LLMRequest.systemPrompt)
- 페르소나 (이름, 말투, 태도)
- 역할 (perceivedRole — 정병이면 가짜)
- 동료 (마피아만)
- 게임 룰 핵심
- 중요 원칙: **역할이 페르소나보다 우선**

#### 유저 프롬프트 (LLMRequest.userPrompt)
- 현재 상황 (Day, Phase)
- 살아있는 사람
- 발언 기록 (채팅 히스토리)
- 사망/처형 기록
- 작업 지시 (발언/투표/밤 행동)

#### 결정형 액션은 구조화 출력 (JSON) 요구 ★

투표/밤 행동/조사 등은 LLM에게 **JSON 형식 응답**을 요구. 응답에 `rationale` 필드 포함 → `game_history.rationale`에 저장.

| 액션 | 요구 출력 형식 (예시) |
|---|---|
| `VOTE_1` | `{"target": <slot>, "rationale": "왜 이 사람을 의심하는지"}` |
| `VOTE_2` | `{"agree": true/false, "rationale": "왜 찬성/반대"}` |
| `MAFIA_KILL` (협의 결과) | `{"target": <slot>, "rationale": "왜 이 사람 우선 제거"}` |
| `POLICE_BLOCK` | `{"action": "BLOCK", "target": <slot>, "rationale": "..."}` |
| `POLICE_SHOOT` | `{"action": "SHOOT", "target": <slot>, "rationale": "..."}` |
| `DOCTOR_HEAL` | `{"target": <slot>, "rationale": "..."}` |
| `INVESTIGATE` | `{"target": <slot>, "rationale": "..."}` |

**SPEAK는 예외**: 발언 자체가 rationale의 표현. `rationale` 컬럼 비워두거나 `"발언 자체가 추리"`로 채움.

### 8.8 Turn-Taking 정책 Z (하이브리드 파이프라인)

```
T=0     AI #1 발언 스트리밍 시작 (UI에 토큰)
T=1s    서버: 다음 라운드 점수 평가 시작 (5명 병렬)
T=2s    점수 도착. AI #2 선정.
        → 박빙 아니면 (격차 ≥ 2점) AI #2 발언 미리 생성 시작
        → 박빙이면 AI #1 끝까지 대기 (보수적)
T=3s    AI #1 끝. AI #2 즉시 시작 (이미 1초 어치 생성됨)
T=3.1s  라운드 N+2 점수 평가 백그라운드 시작
```

**효과**: AI 발언 사이 갭 거의 0초. 끊김 없는 게임.

**유저 끼어들기 시**: 진행 중 모든 작업(현재 발언, 다음 평가, 사전 발언) cancel → 유저 발언 broadcast → 새 라운드.

**토론 시간 만료 시**: 진행 중 발언은 **마저 출력**한 후 다음 단계 진입. 진행 중인 점수 평가/사전 발언은 cancel.

### 8.9 SpeakIntentScorer

- 모든 살아있는 AI 병렬 호출 (`CompletableFuture.allOf()`)
- Ollama `OLLAMA_NUM_PARALLEL=5` 설정 권장
- 점수 < 임계치 (예: 3) 모두면 토론 종료
- 점수 영향 요인 (프롬프트에 명시): 자기 이름 언급, 침묵 시간, 의심도, 정보 부족 등

### 8.10 NightResolver (페이크 결과 처리)

```
1. 차단 적용 (경찰)
2. 공격 (마피아/살인자) — 차단 안 된 것만
3. 보호 (의사) — 차단 안 된 것만
4. 사망 확정 (공격 - 보호)
5. 정보 능력 (경찰 조사 / 조사관)
6. 정병 페이크 결과 생성 ★
7. 개인 알림 메시지 생성 (차단당함, 살아남음 등)
```

정병 페이크 결과는 perceivedRole에 따라 처리 분기.

---

## 9. DB 스키마 (MySQL)

### 9.1 ER 다이어그램

```
users                    games                game_participants
┌────────────┐          ┌──────────────┐    ┌──────────────────┐
│ id PK      │ ◄────┐   │ id PK        │ ◄─│ id PK            │
│ username   │      │   │ host_user_id │   │ game_id FK       │
│ password   │      └───│ started_at   │   │ slot (1-6)       │
│ created_at │          │ ended_at     │   │ participant_type │
└────────────┘          │ duration_sec │   │ user_id FK       │
                        │ winning_     │   │ persona_id FK    │
                        │   faction    │   │ actual_role      │
                        │ total_days   │   │ perceived_role   │
                        └──────────────┘   │ faction          │
                                           │ alive_at_end     │
personas                                   │ death_day        │
┌────────────────────────┐                 │ death_cause      │
│ id PK                  │ ◄───────────────┤                  │
│ name                   │                 └──────────────────┘
│ speech_style           │
│ attitude               │
│ system_prompt_template │      game_events (리플레이용, 옵션)
│ avatar_path            │      ┌──────────────────┐
│ active                 │      │ id PK            │
│ created_at             │      │ game_id FK ──────┼─→ games
└────────────────────────┘      │ day_number       │
                                │ phase            │
                                │ event_type       │
                                │ payload_json     │
                                │ occurred_at      │
                                └──────────────────┘

ai_decisions (AI LLM 호출 로그 - 디버깅/프롬프트 튜닝용) ★
┌──────────────────────┐
│ id PK                │
│ game_id FK ──────────┼─→ games
│ participant_id FK ───┼─→ game_participants
│ day_number           │
│ phase                │
│ decision_type        │  (SPEAK_INTENT / SPEECH / NIGHT_ACTION / VOTE_1 / VOTE_2 / DEFENSE)
│ system_prompt        │  (LLM 시스템 프롬프트 통째로)
│ user_prompt          │  (LLM 유저 프롬프트 통째로)
│ raw_response         │  (LLM 원본 응답)
│ parsed_decision      │  (JSON 형태 결정 결과)
│ model_used           │  ('qwen2.5:7b' 등)
│ duration_ms          │
│ decided_at           │
└──────────────────────┘

game_history (실제 게임 행동 시간순 - AI/유저 통합) ★
┌──────────────────────┐
│ id PK                │
│ game_id FK ──────────┼─→ games
│ participant_id FK ───┼─→ game_participants
│ day_number           │
│ phase                │
│ action_type          │  (SPEAK / VOTE_1 / VOTE_2 / MAFIA_KILL / POLICE_BLOCK / 
│                      │   POLICE_SHOOT / DOCTOR_HEAL / INVESTIGATE / KILLER_ATTACK / IMPOSTOR_FAKE)
│ action_data          │  (JSON: 입력값 — text, targetSlot 등)
│ rationale            │  (AI 근거 — LLM이 함께 출력)
│ result               │  (JSON: 결과 — 밤 행동 결과 등)
│ occurred_at          │
└──────────────────────┘
```

### 두 로그 테이블 차이

| | `ai_decisions` | `game_history` |
|---|---|---|
| 단위 | LLM **호출**마다 1행 | 실제 **행동**마다 1행 |
| 대상 | AI만 | AI + 유저 모두 |
| SPEAK_INTENT 같은 평가 | 포함 (5명 평가하면 5행) | 미포함 (실제 발언 1행만) |
| 주 용도 | 프롬프트 디버깅, 응답 시간 분석 | 게임 흐름 추적, 행동 분석, 리플레이 |
| 사이즈 | 큼 (프롬프트+응답 매번) | 작음 (액션 데이터만) |

### 9.2 DDL

```sql
CREATE DATABASE mafia_game CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mafia_game;

-- 1. 유저 계정
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. AI 페르소나 풀
CREATE TABLE personas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    speech_style TEXT NOT NULL,
    attitude TEXT NOT NULL,
    system_prompt_template TEXT NOT NULL,
    avatar_path VARCHAR(255),
    active TINYINT(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. 게임 1판
CREATE TABLE games (
    id INT AUTO_INCREMENT PRIMARY KEY,
    host_user_id INT NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP NULL,
    duration_sec INT NULL,
    winning_faction VARCHAR(20) NULL,
    total_days INT NULL,
    FOREIGN KEY (host_user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- 4. 각 게임 참가자
CREATE TABLE game_participants (
    id INT AUTO_INCREMENT PRIMARY KEY,
    game_id INT NOT NULL,
    slot INT NOT NULL,
    participant_type VARCHAR(10) NOT NULL,  -- USER / AI
    user_id INT NULL,
    persona_id INT NULL,
    actual_role VARCHAR(20) NOT NULL,
    perceived_role VARCHAR(20) NOT NULL,
    faction VARCHAR(20) NOT NULL,
    alive_at_end TINYINT(1) NOT NULL,
    death_day INT NULL,
    death_cause VARCHAR(20) NULL,
    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (persona_id) REFERENCES personas(id) ON DELETE SET NULL,
    INDEX idx_game (game_id),
    INDEX idx_user (user_id),
    INDEX idx_persona (persona_id)
);

-- 5. 게임 이벤트 (리플레이용, 옵션)
CREATE TABLE game_events (
    id INT AUTO_INCREMENT PRIMARY KEY,
    game_id INT NOT NULL,
    day_number INT NOT NULL,
    phase VARCHAR(30) NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    payload_json TEXT NOT NULL,
    occurred_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE,
    INDEX idx_game_day (game_id, day_number)
);

-- 6. AI 판단 로그 (LLM 호출 단위 - 프롬프트 튜닝 + 디버깅용) ★
CREATE TABLE ai_decisions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_id INT NOT NULL,
    participant_id INT NOT NULL,         -- 어느 슬롯의 AI가 결정했는지
    day_number INT NOT NULL,
    phase VARCHAR(30) NOT NULL,
    decision_type VARCHAR(30) NOT NULL,
    -- decision_type 값:
    --   SPEAK_INTENT  : 발언 욕구 점수 평가
    --   SPEECH        : 발언 생성
    --   NIGHT_ACTION  : 밤 행동 결정
    --   VOTE_1        : 1차 투표 (처형 후보 지목)
    --   VOTE_2        : 2차 찬반 투표
    --   DEFENSE       : 변론 (피지목 시)

    system_prompt TEXT,                  -- LLM 시스템 프롬프트 (페르소나 + 역할)
    user_prompt TEXT,                    -- LLM 유저 프롬프트 (게임 상황)
    raw_response TEXT,                   -- LLM 원본 응답 (스트리밍 합친 결과)
    parsed_decision TEXT,                -- 파싱된 결정 (JSON 형태)

    model_used VARCHAR(50),              -- 'qwen2.5:7b', 'claude-sonnet-4-6' 등
    duration_ms INT,                     -- 응답 소요 시간 (ms)
    decided_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE,
    FOREIGN KEY (participant_id) REFERENCES game_participants(id) ON DELETE CASCADE,
    INDEX idx_game (game_id),
    INDEX idx_participant (participant_id),
    INDEX idx_decision_type (decision_type)
);

-- 7. 게임 행동 히스토리 (실제 행동 단위 - AI + 유저 모두) ★
CREATE TABLE game_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_id INT NOT NULL,
    participant_id INT NOT NULL,         -- 행위자 (AI든 유저든)
    day_number INT NOT NULL,
    phase VARCHAR(30) NOT NULL,
    action_type VARCHAR(30) NOT NULL,
    -- action_type 값:
    --   SPEAK          : 발언 (낮 토론 / 변론 / 마피아 비밀방)
    --   VOTE_1         : 1차 투표 (처형 후보 지목)
    --   VOTE_2         : 2차 찬반 투표
    --   MAFIA_KILL     : 마피아 처형 대상 지목 (협의 결과)
    --   POLICE_BLOCK   : 경찰 차단
    --   POLICE_SHOOT   : 경찰 저격
    --   DOCTOR_HEAL    : 의사 보호
    --   INVESTIGATE    : 조사관 조사
    --   KILLER_ATTACK  : 중립 살인자 공격
    --   IMPOSTOR_FAKE  : 정병 헛스윙 (perceivedRole 기반)

    action_data TEXT,                    -- JSON. 행동 입력 정보
    -- 예시:
    --   SPEAK         : {"text": "수상해요", "channel": "DAY"}
    --   VOTE_1        : {"targetSlot": 3}
    --   VOTE_2        : {"agree": true}
    --   MAFIA_KILL    : {"targetSlot": 5}
    --   POLICE_BLOCK  : {"targetSlot": 2}
    --   DOCTOR_HEAL   : {"targetSlot": 4}
    --   INVESTIGATE   : {"targetSlot": 3}

    rationale TEXT,                      -- AI의 행동 근거 (LLM이 함께 출력) ★
    -- 예시:
    --   VOTE_1     : "라온이 어제는 빛나를 의심하더니 오늘 갑자기 노을을 의심함. 일관성 없음"
    --   MAFIA_KILL : "가람이 의사 같음. 오늘 미르가 살아남은 거 보면"
    --   DOCTOR_HEAL: "나(노을)를 보호. 어제부터 마피아가 의사 추적 중인 것 같음"
    --   INVESTIGATE: "빛나가 가장 의심스러움. 발언 패턴이 마피아 같음"

    result TEXT,                         -- JSON. 행동 결과 (밤 행동만 의미 있음)
    -- 예시:
    --   MAFIA_KILL    : {"died": true} 또는 {"died": false, "reason": "DOCTOR_HEALED"}
    --   POLICE_BLOCK  : {"success": true}
    --   POLICE_SHOOT  : {"died": true, "wasMafia": true}
    --   DOCTOR_HEAL   : {"saved": true} 또는 {"saved": false, "reason": "NOT_ATTACKED"}
    --   INVESTIGATE   : {"candidates": ["MAFIA", "CITIZEN"]}
    --   IMPOSTOR_FAKE : {"fakeResult": "...", "actualEffect": "NONE"}

    occurred_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE,
    FOREIGN KEY (participant_id) REFERENCES game_participants(id) ON DELETE CASCADE,
    INDEX idx_participant_game (participant_id, game_id),
    INDEX idx_game_time (game_id, occurred_at),
    INDEX idx_action_type (action_type)
);
```

### 9.3 통계 쿼리

#### 유저 진영별 승률
```sql
SELECT 
    gp.faction,
    COUNT(*) AS games,
    SUM(CASE WHEN gp.faction = g.winning_faction THEN 1 ELSE 0 END) AS wins,
    ROUND(SUM(CASE WHEN gp.faction = g.winning_faction THEN 1 ELSE 0 END) 
          * 100.0 / COUNT(*), 1) AS win_rate
FROM game_participants gp
JOIN games g ON gp.game_id = g.id
WHERE gp.user_id = ? AND g.ended_at IS NOT NULL
GROUP BY gp.faction;
```

#### 페르소나 전체 승률 (직업 무관)
```sql
SELECT 
    p.name AS persona,
    COUNT(gp.id) AS games,
    SUM(CASE WHEN gp.faction = g.winning_faction THEN 1 ELSE 0 END) AS wins,
    ROUND(
        SUM(CASE WHEN gp.faction = g.winning_faction THEN 1 ELSE 0 END) 
        * 100.0 / COUNT(gp.id), 1
    ) AS win_rate
FROM personas p
LEFT JOIN game_participants gp ON gp.persona_id = p.id
LEFT JOIN games g ON gp.game_id = g.id AND g.ended_at IS NOT NULL
GROUP BY p.id, p.name
ORDER BY win_rate DESC;
```

#### 페르소나 X 진영별 승률 (마피아일 때 vs 시민일 때)
```sql
SELECT 
    p.name AS persona,
    gp.faction,
    COUNT(*) AS games,
    SUM(CASE WHEN gp.faction = g.winning_faction THEN 1 ELSE 0 END) AS wins,
    ROUND(
        SUM(CASE WHEN gp.faction = g.winning_faction THEN 1 ELSE 0 END) 
        * 100.0 / COUNT(*), 1
    ) AS win_rate
FROM game_participants gp
JOIN games g ON gp.game_id = g.id
JOIN personas p ON gp.persona_id = p.id
WHERE g.ended_at IS NOT NULL
GROUP BY p.id, gp.faction
ORDER BY p.name, gp.faction;
```

#### 페르소나 X 역할별 승률 (직업별 잘함/못함 - win_rate 추가)
```sql
SELECT 
    p.name AS persona,
    gp.actual_role AS role,
    COUNT(*) AS games,
    SUM(CASE WHEN gp.faction = g.winning_faction THEN 1 ELSE 0 END) AS wins,
    ROUND(
        SUM(CASE WHEN gp.faction = g.winning_faction THEN 1 ELSE 0 END) 
        * 100.0 / COUNT(*), 1
    ) AS win_rate
FROM game_participants gp
JOIN games g ON gp.game_id = g.id
JOIN personas p ON gp.persona_id = p.id
WHERE g.ended_at IS NOT NULL
GROUP BY p.id, gp.actual_role
ORDER BY p.name, gp.actual_role;
```

#### 글로벌 진영 승률
```sql
SELECT 
    winning_faction,
    COUNT(*) AS wins,
    ROUND(COUNT(*) * 100.0 / 
          (SELECT COUNT(*) FROM games WHERE ended_at IS NOT NULL), 1) AS percentage
FROM games
WHERE ended_at IS NOT NULL
GROUP BY winning_faction;
```

#### AI 판단 로그 분석 쿼리 (프롬프트 튜닝용)

특정 게임의 AI 판단 흐름:
```sql
SELECT 
    ad.day_number, ad.phase, ad.decision_type,
    p.name AS persona, gp.actual_role, gp.perceived_role,
    LEFT(ad.user_prompt, 200) AS prompt_preview,
    LEFT(ad.raw_response, 200) AS response_preview,
    ad.duration_ms
FROM ai_decisions ad
JOIN game_participants gp ON ad.participant_id = gp.id
JOIN personas p ON gp.persona_id = p.id
WHERE ad.game_id = ?
ORDER BY ad.decided_at;
```

페르소나 X decision_type 평균 응답 시간 (성능 분석):
```sql
SELECT 
    p.name AS persona, ad.decision_type,
    COUNT(*) AS calls,
    AVG(ad.duration_ms) AS avg_ms
FROM ai_decisions ad
JOIN game_participants gp ON ad.participant_id = gp.id
JOIN personas p ON gp.persona_id = p.id
GROUP BY p.id, ad.decision_type
ORDER BY p.name, ad.decision_type;
```

특정 페르소나의 모든 발언 모음 (행동 패턴 분석):
```sql
SELECT 
    ad.game_id, ad.day_number, gp.actual_role,
    ad.raw_response
FROM ai_decisions ad
JOIN game_participants gp ON ad.participant_id = gp.id
JOIN personas p ON gp.persona_id = p.id
WHERE p.name = ? AND ad.decision_type = 'SPEECH'
ORDER BY ad.decided_at DESC
LIMIT 50;
```

#### 일차별 게임 흐름 + 근거 정리 ★

특정 게임 X일차의 모든 행동 + 근거 시간순:
```sql
SELECT 
    gh.day_number, gh.phase, gh.action_type,
    p.name AS persona, gp.actual_role, gp.perceived_role,
    gh.action_data, gh.rationale, gh.result,
    gh.occurred_at
FROM game_history gh
JOIN game_participants gp ON gh.participant_id = gp.id
LEFT JOIN personas p ON gp.persona_id = p.id      -- 유저면 NULL
WHERE gh.game_id = ? AND gh.day_number = ?
ORDER BY gh.occurred_at;
```

특정 AI(슬롯)의 게임 전체 행동 + 근거 (그 AI 시점 추적):
```sql
SELECT 
    gh.day_number, gh.phase, gh.action_type,
    gh.action_data, gh.rationale, gh.result
FROM game_history gh
WHERE gh.game_id = ? AND gh.participant_id = ?
ORDER BY gh.occurred_at;
```

특정 게임의 일차별 사망/처형 요약 (게임 큰 흐름):
```sql
SELECT 
    gp.death_day, gp.slot, p.name AS persona, 
    gp.actual_role, gp.death_cause
FROM game_participants gp
LEFT JOIN personas p ON gp.persona_id = p.id
WHERE gp.game_id = ? AND gp.alive_at_end = 0
ORDER BY gp.death_day, gp.slot;
```

이 세 쿼리를 조합하면 **"X일차에 누가 누구를 왜 죽였고, 왜 누구한테 투표했고, 어떤 발언이 있었나"** 한눈에 추적 가능 → 직업/페르소나/근거 다 묶여서 게임 분석 끝.

### 9.4 페르소나 시드 데이터 (한국형 AI 이름)

| 이름 | 어원/의미 | 성격 |
|---|---|---|
| **미르** | 한국 고어 "용". 강하고 날카로움 | 공격적/직설적 |
| **누리** | 세상/영역 (LG AI 이름) | 차분/논리적 |
| **라온** | 즐거운 | 장난기/분위기 메이커 |
| **노을** | 감성적 자연어 | 감성적/직관적 |
| **도담** | 야무지고 단정한 | 무뚝뚝/말 적음 |
| **빛나** | 반짝이는 | 활발/의심 많음 |
| **가람** | 강 (큰 강처럼 깊고 넓게) | 관찰자/종합형 |
| **한결** | 한결같은, 변함없는 | 신중/단호 |

각 페르소나의 `system_prompt_template`은 위 성격을 기반으로 LLM 프롬프트로 작성.

---

## 10. GUI / 캐릭터 시스템

### 10.1 화면 흐름

```
[로그인/회원가입]
        │
        ▼
   [메인 메뉴] ─────► [통계 화면]
        │   ┌──► 싱글 플레이
        │   └──► 멀티 플레이 ─► [로비/방 목록]
        │                         │
        │                         ├─► [방 만들기] ─► [대기실(호스트)]
        │                         └─► [방 참가]   ─► [대기실(참가자)]
        │                                              │
        ▼                                              ▼
   [페르소나 관리]                              호스트가 시작
                                                       │
                                                       ▼
                                                 [게임 화면]
                                                  Phase 별로
                                                  하단 패널 교체
                                                       │
                                                       ▼
                                                [게임 종료 화면]
                                                 결과 / 역할 공개
```

### 10.2 게임 화면 레이아웃

```
┌─────────────────────────────────────────────────────────────┐
│ ⏱ 02:15  Day 2 - 낮 토론             당신: 노을 (의사)   │ ← 헤더 (타이머/Phase/역할)
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──┐  ┌──┐  ┌──┐  ┌──┐  ┌──┐  ┌──┐                       │
│  │미│  │누│  │라│  │노│  │빛│  │가│   ← 캐릭터 일러스트     │
│  │르│  │리│  │온│  │을│  │나│  │람│   (160px 정도)        │
│  │💚│  │💚│  │🔊│  │💚│  │💀│  │💀│   ← 상태 표시           │
│  └──┘  └──┘  └──┘  └──┘  └──┘  └──┘                       │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│ 💬 채팅 영역                                                │
│ [라온] 빛나가 이상해                                        │
│ [노을] 누구 의심가?                                         │
│ [입력] _____________________ [전송]                         │
├─────────────────────────────────────────────────────────────┤
│ Phase별 하단 액션 패널 (교체)                                │
└─────────────────────────────────────────────────────────────┘
```

### 10.3 캐릭터 시스템 (B방식 - 2상태 스프라이트)

#### 비주얼 스타일
- 페르소나당 **2장**: idle / talking
- 발언 중에 두 프레임 교대 (입/표정 변화)
- 사망 상태는 효과로 표현 (회색조 + 기울임 + 반투명)

#### 그림 자원
- **직접 그리기** (사용자 결정)
- 8명 × 2장 = **총 16장 준비**

#### 캐릭터 상태

| 상태 | 비주얼 효과 |
|---|---|
| **idle** (살아있음) | 정상 + 미세한 상하 움직임 (1-2px, 2초 주기) |
| **발언 중** | talking 프레임 + 노란 글로우 테두리 + 살짝 흔들림 (좌우 1px) |
| **타이핑 중** | 캐릭터 위에 ... 표시 |
| **사망** | 회색조 + 30도 기울임 + 50% 투명도 |
| **투표 대상** | 빨간 테두리 깜박임 |
| **본인 (YOU)** | 노란 "YOU" 라벨 |
| **마피아 동료** (마피아만 보임) | 빨간 ★ 표시 |

#### JavaFX 컴포넌트 사용
- `ImageView` (캐릭터 일러스트)
- `Timeline` + `KeyFrame` (idle/talking 애니메이션)
- `FadeTransition` (투표 대상 깜박임)
- `ColorAdjust` (사망 회색조)
- `Platform.runLater()` (서버 메시지 → UI 반영)

### 10.4 Phase별 하단 패널

| Phase | 하단 패널 |
|---|---|
| `DAY_DISCUSSION` | 채팅 입력만, 별도 패널 X |
| `DAY_VOTE_1` | 살아있는 사람 버튼 + 기권. 현재 투표 현황 표시 (공개) |
| `DAY_DEFENSE` | 피지목자만 입력 가능, 다른 사람은 보기만 |
| `DAY_VOTE_2` | 찬성/반대 버튼 + 실시간 집계 |
| `NIGHT` | 역할별 다른 UI (마피아=비밀방+대상선택, 경찰=능력+대상, 의사=대상, 조사관=대상, 시민=대기) |
| `DAY_ANNOUNCE` | 사망자/알림 표시 + "계속" 버튼 |

### 10.5 통계 화면 컴포넌트

| 항목 | JavaFX 컴포넌트 |
|---|---|
| 내 진영별 승률 | `BarChart` |
| 페르소나 X 역할 매트릭스 | `TableView` 또는 그리드 |
| 글로벌 진영 승률 | `PieChart` |
| 최근 게임 기록 | `ListView` |

---

## 11. 작업 로드맵

### Phase 1 — MVP (작동하는 게임)
1. DB 스키마 + DBConnect + DAO (POS 패턴)
2. 도메인 모델 (Game, Player, Role, Persona)
3. Ollama 연동 (LLMClient + OllamaClient, 단순 호출)
4. 싱글플레이 게임 흐름 (PhaseManager + 기본 룰)
5. 콘솔 또는 최소 JavaFX UI로 게임 한 판 플레이 가능
6. 페르소나 시드 데이터 8명

### Phase 2 — 게임 완성도
1. JavaFX 게임 화면 (캐릭터 라인업 + 채팅 + Phase별 패널)
2. 캐릭터 일러스트 16장 준비 (직접 그리기)
3. 캐릭터 애니메이션 (idle/talking/사망 효과)
4. Turn-taking 정책 Z (점수 미리 + 박빙 아니면 발언도 미리)
5. 스트리밍 발언 (PLAYER_SPEAK_DELTA)
6. 정병 메커니즘 + 페이크 결과
7. 회원가입/로그인 + 통계 화면

### Phase 3 — 멀티플레이어
1. TCP 소켓 서버 (`GameServer`, `ClientSession`)
2. 클라이언트 추상화 (`GameClient` 인터페이스, Local + Remote 구현)
3. 메시지 프로토콜 (Jackson 다형성)
4. 로비/대기실/IP 직접 접속
5. 멀티 게임 동기화

### Phase 4 — 폴리싱 (시간 남으면)
1. 캐릭터 애니메이션 추가 상태 (변론 시 줌인, 처형 페이드아웃 등)
2. CSS 스타일링 (다크 모드 / 마피아 분위기)
3. 페르소나 관리 화면 (CRUD)
4. 게임 리플레이 (game_events 테이블 활용)
5. Claude API 연동 (LLMClient 구현체 추가)
6. 사운드 효과 (선택)

### V2 (장기, 과제 후)
1. 페르소나별 traits 점수 시스템 (공격성/논리성 등)
2. 데이터 분석/시각화 강화 (페르소나 X 역할 히트맵 등)
3. 게임 룰 변형 (역할 추가, 인원 변경)
4. AI 발언 스타일 학습 (게임 결과 기반 페르소나 튜닝)

---

## 12. 주요 설계 결정 요약

| 결정 | 선택 | 이유 |
|---|---|---|
| LLM 제공자 | Ollama (개발) → Claude API (배포) | 개발 비용 0, 배포 시 품질 ↑ |
| LLM 모델 | qwen2.5:7b (한국어 우수) | RTX 5060Ti 16GB로 충분, 14B 업그레이드 여유 |
| AI 메모리 모델 | Stateless (매번 게임 상태 주입) | 일관성 보장, 진실 통제 |
| 통신 방식 | TCP Socket (자바 표준) | 채팅에 TCP가 적합 (순서/도착 보장) |
| 인원 구성 | 6명 고정, 인간 1-6명 + AI 채움 | 룰 단순, AI 필수 |
| Turn-taking | 정책 Z (하이브리드 파이프라인) | 실시간성 + 자율성 균형 |
| 발언 방식 | 스트리밍 (LLM 토큰별 broadcast) | "AI 타이핑 중" 효과 |
| 투표 | 공개 투표 | 추리 단서로 활용 |
| 토론 시간 | 실시간 카운트다운 (~3분) | 긴박감 ↑ |
| GUI 프레임워크 | JavaFX | 모던, 멀티스레드 강점 |
| GUI 패턴 | POS 프로젝트 스타일 (GUI / 백엔드 분리) | 익숙한 패턴 활용 |
| 캐릭터 비주얼 | 2상태 스프라이트 (idle/talking) + 직접 그리기 | 작업량과 효과의 균형 |

---

## 13. 현재 미정 / 향후 결정

- 페르소나 `system_prompt_template`의 정확한 문구 (구현 단계에서 튜닝)
- 캐릭터 일러스트 스타일 (애니메이션? 픽셀 아트? 사실적?)
- LAN 방 자동 발견 (UDP 브로드캐스트) vs IP 직접 입력만
- `game_events` 테이블 (리플레이) MVP 포함 여부
- 사운드 효과 추가 여부

---

## 14. 참고

- POS 프로젝트 (`C:\Users\ACE\Desktop\pos\src`)의 패턴을 백엔드/DAO 구조에 차용
- Ollama 공식: https://ollama.com
- JavaFX 공식: https://openjfx.io
- qwen2.5 모델 카드: https://huggingface.co/Qwen/Qwen2.5-7B
