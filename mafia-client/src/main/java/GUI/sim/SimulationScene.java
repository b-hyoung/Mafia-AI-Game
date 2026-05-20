package GUI.sim;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import mafia.domain.GamePhase;
import mafia.domain.Player;
import mafia.domain.Role;
import mafia.engine.Event;
import mafia.engine.EventLog;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 봇 5명 자동 마피아 게임 관전 화면.
 *
 * 현재 단계: UI 뼈대 + 더미 시뮬레이션 (Timeline 기반).
 * 실제 GameEngine 연결은 다음 단계에서 사용자와 함께 작업.
 *
 * 더미 모드 동작:
 * - Timeline이 2초마다 가짜 이벤트 생성 → EventLog에 추가
 * - 카드/패널은 진짜 게임처럼 갱신됨
 * - 진짜 알고리즘은 없음 (랜덤한 이벤트)
 */
public class SimulationScene {

    private static final String[] NICKS = {"alice", "bob", "chris", "dan", "eve"};
    private static final String[] BOT_TYPES = {"CSP", "RULE", "CSP", "RAND", "LLM"};

    public static BorderPane create(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("sim-root");

        // ===== 상태 =====
        List<Player> players = createPlayers();
        EventLog log = new EventLog();
        StatsPanel statsPanel = new StatsPanel();

        // ===== 상단 컨트롤 바 =====
        ControlBar controlBar = new ControlBar();
        root.setTop(controlBar.getRoot());

        // ===== 플레이어 카드 영역 =====
        HBox cardRow = new HBox();
        cardRow.getStyleClass().add("sim-card-row");
        Player[] selected = new Player[]{null};
        VBox[] cardNodes = new VBox[players.size()];
        for (int i = 0; i < players.size(); i++) {
            final int idx = i;
            VBox card = PlayerCard.create(players.get(i), () -> {
                // 선택 토글
                if (selected[0] != null) {
                    for (VBox c : cardNodes) PlayerCard.setSelected(c, false);
                }
                selected[0] = players.get(idx);
                PlayerCard.setSelected(cardNodes[idx], true);
            });
            cardNodes[i] = card;
            cardRow.getChildren().add(card);
        }

        // ===== 본문 (이벤트 로그 + 통계) =====
        EventLogPanel eventLogPanel = new EventLogPanel(log.events());
        HBox.setHgrow(eventLogPanel.getRoot(), Priority.ALWAYS);

        HBox body = new HBox();
        body.getStyleClass().add("sim-body");
        body.getChildren().addAll(eventLogPanel.getRoot(), statsPanel.getRoot());

        // ===== 조립 =====
        VBox center = new VBox(cardRow, body);
        VBox.setVgrow(body, Priority.ALWAYS);
        root.setCenter(center);

        // ===== 더미 시뮬레이션 (Timeline 기반) =====
        DummySimulator dummy = new DummySimulator(players, log, statsPanel);

        controlBar.playBtn.setOnAction(e -> dummy.play());
        controlBar.pauseBtn.setOnAction(e -> dummy.pause());
        controlBar.stepBtn.setOnAction(e -> dummy.step());
        controlBar.newGameBtn.setOnAction(e -> dummy.newGame());

        // Scene 떠날 때 Timeline 정리
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) dummy.stop();
        });

        return root;
    }

    private static List<Player> createPlayers() {
        List<Player> list = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            list.add(new Player(i + 1, NICKS[i], BOT_TYPES[i]));
        }
        return list;
    }

    /**
     * 더미 시뮬레이터 — 진짜 GameEngine 연결 전까지 시각화 검증용.
     * Timeline이 2초마다 가짜 이벤트를 만들어 EventLog에 추가한다.
     */
    private static class DummySimulator {
        private final List<Player> players;
        private final EventLog log;
        private final StatsPanel stats;
        private final Random rng = new Random();
        private final Timeline timeline;
        private int round = 0;
        private GamePhase phase = GamePhase.DAY_DISCUSSION;

        DummySimulator(List<Player> players, EventLog log, StatsPanel stats) {
            this.players = players;
            this.log = log;
            this.stats = stats;
            this.timeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> tick()));
            this.timeline.setCycleCount(Timeline.INDEFINITE);
            resetGame();
        }

        void play() { timeline.play(); }
        void pause() { timeline.pause(); }
        void step() { tick(); }
        void stop() { timeline.stop(); }

        void newGame() {
            timeline.stop();
            log.clear();
            for (Player p : players) {
                p.setRole(null);
                if (!p.isAlive()) {
                    // 직접 alive 되돌리기 — Player에 메서드 없으니 setter 추가 필요
                    // 일단 새 인스턴스 흐름이 더 안전. 여기선 reflection 회피 위해 그대로 둠.
                }
            }
            resetGame();
            timeline.play();
        }

        private void resetGame() {
            round = 1;
            phase = GamePhase.DAY_DISCUSSION;
            stats.setCurrentRound(round);
            stats.setCurrentPhase(phase.getLabel());

            // 더미 역할 배정
            Role[] roles = {Role.MAFIA, Role.CITIZEN, Role.CITIZEN, Role.POLICE, Role.DOCTOR};
            List<Role> shuffled = new ArrayList<>(List.of(roles));
            java.util.Collections.shuffle(shuffled);
            for (int i = 0; i < players.size(); i++) {
                // null로 두기 — 게임 끝날 때 공개
            }

            log.append(new Event.GameStarted(0, GamePhase.DAY_DISCUSSION, players, Instant.now()));
        }

        private void tick() {
            // 더미 이벤트 생성
            Player actor = randomAlive();
            if (actor == null) {
                // 게임 종료
                Role winner = rng.nextBoolean() ? Role.MAFIA : Role.CITIZEN;
                log.append(new Event.GameEnded(round, phase, winner, Instant.now()));
                if (winner.isMafiaTeam()) stats.recordMafiaWin();
                else stats.recordCitizenWin();
                pause();
                return;
            }

            switch (phase) {
                case DAY_DISCUSSION -> {
                    String[] templates = {
                        "P%d가 의심돼요",
                        "저는 시민입니다",
                        "어제 P%d를 봤어요",
                        "P%d 좀 변호해주세요",
                        "이상한 점이 있어요"
                    };
                    Player target = randomAlive();
                    int tn = target == null ? 1 : target.getSlot();
                    String text = templates[rng.nextInt(templates.length)].formatted(tn);
                    log.append(new Event.Speak(round, phase, actor, text, Instant.now()));

                    if (rng.nextInt(3) == 0) {
                        // 페이즈 전환
                        phase = GamePhase.DAY_VOTE;
                        stats.setCurrentPhase(phase.getLabel());
                        log.append(new Event.PhaseChanged(round, phase, GamePhase.DAY_DISCUSSION, phase, Instant.now()));
                    }
                }
                case DAY_VOTE -> {
                    Player target = randomAlive();
                    if (target != null) {
                        log.append(new Event.Vote(round, phase, actor, target, Instant.now()));
                    }

                    if (rng.nextInt(3) == 0) {
                        // 처형 + 페이즈 전환
                        Player victim = randomAlive();
                        if (victim != null) {
                            victim.kill();
                            log.append(new Event.Executed(round, phase, victim, 3, Instant.now()));
                        }
                        phase = GamePhase.NIGHT;
                        stats.setCurrentPhase(phase.getLabel());
                        log.append(new Event.PhaseChanged(round, phase, GamePhase.DAY_VOTE, phase, Instant.now()));
                    }
                }
                case NIGHT -> {
                    Player victim = randomAlive();
                    if (victim != null && rng.nextInt(2) == 0) {
                        victim.kill();
                        log.append(new Event.NightKill(round, phase, actor, victim, Instant.now()));
                    }
                    round++;
                    phase = GamePhase.DAY_DISCUSSION;
                    stats.setCurrentRound(round);
                    stats.setCurrentPhase(phase.getLabel());
                    log.append(new Event.PhaseChanged(round, phase, GamePhase.NIGHT, phase, Instant.now()));
                }
                case ENDED -> {
                    // 게임 끝났음
                }
            }
        }

        private Player randomAlive() {
            List<Player> alive = players.stream().filter(Player::isAlive).toList();
            if (alive.isEmpty()) return null;
            return alive.get(rng.nextInt(alive.size()));
        }
    }
}
