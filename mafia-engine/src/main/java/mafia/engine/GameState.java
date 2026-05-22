package mafia.engine;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import mafia.domain.GamePhase;
import mafia.domain.Player;

import java.util.List;

/**
 * 게임 한 판의 현재 상태를 담는 holder.
 *
 * <p>변하는 값(라운드, 페이즈)은 JavaFX {@link IntegerProperty}/{@link ObjectProperty}로 둬서
 * UI(SimulationScene 등)가 값 변화에 자동으로 반응할 수 있게 한다.
 *
 * <p>플레이어 리스트는 게임 중 추가/삭제 안 됨 (5명 고정). 죽어도 리스트에서 안 빠지고
 * {@link Player#isAlive()}로 판단.
 */
public class GameState {

    // ────────────────────────────────────────────────────────────
    // 필드
    // ────────────────────────────────────────────────────────────

    /** 5명 플레이어 (게임 중 멤버 안 바뀜, 죽어도 그대로 리스트에 남음). */
    private final List<Player> players;

    /** 현재 라운드 (0=시작 전, 1부터 실제 게임). UI 바인딩 가능. */
    private final IntegerProperty round = new SimpleIntegerProperty(0);

    /** 현재 페이즈 (낮 토론 / 낮 투표 / 밤 / 종료). UI 바인딩 가능. */
    private final ObjectProperty<GamePhase> phase = new SimpleObjectProperty<>(GamePhase.DAY_DISCUSSION);

    // ────────────────────────────────────────────────────────────
    // 생성자
    // ────────────────────────────────────────────────────────────

    public GameState(List<Player> players) {
        this.players = players;
    }

    // ────────────────────────────────────────────────────────────
    // 플레이어 조회
    // ────────────────────────────────────────────────────────────

    /** 5명 전체 (사망자 포함). */
    public List<Player> getPlayers() {
        return players;
    }

    /** 살아있는 플레이어만 필터. {@link WinCondition}이 자주 쓴다. */
    public List<Player> alivePlayers() {
        return players.stream().filter(Player::isAlive).toList();
    }

    /**
     * 슬롯 번호로 플레이어 찾기.
     * Agent가 "P3에게 투표하자" 같은 결정 만들 때, 슬롯 번호로 Player 객체 얻을 때 사용.
     *
     * @throws IllegalArgumentException 해당 슬롯의 플레이어가 없으면
     */
    public Player findBySlot(int slot) {
        return players.stream()
            .filter(p -> p.getSlot() == slot)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Slot " + slot + " 없음"));
    }

    // ────────────────────────────────────────────────────────────
    // 라운드 / 페이즈 (JavaFX Property)
    // ────────────────────────────────────────────────────────────

    public int getRound() { return round.get(); }
    public void setRound(int v) { round.set(v); }
    /** UI가 라운드 변화를 자동으로 감지하려면 이 Property에 바인딩. */
    public IntegerProperty roundProperty() { return round; }

    public GamePhase getPhase() { return phase.get(); }
    public void setPhase(GamePhase p) { phase.set(p); }
    /** UI가 페이즈 변화를 자동 감지하려면 이 Property에 바인딩. */
    public ObjectProperty<GamePhase> phaseProperty() { return phase; }

    // ────────────────────────────────────────────────────────────
    // 의사 연속 보호 추적
    // ────────────────────────────────────────────────────────────

    /**
     * 의사가 어제 보호한 플레이어의 슬롯 번호.
     * <p>{@code -1}이면 "아직 보호 기록 없음" (게임 시작 직후 / 의사 사망 후).
     * <p>오늘 의사 봇이 보호 대상 결정 시 이 값과 같은 슬롯은 후보에서 제외해야 한다 (연속 2회 보호 금지).
     */
    private int lastProtectedSlot = -1;

    public int getLastProtectedSlot() {
        return lastProtectedSlot;
    }

    public void setLastProtectedSlot(int slot) {
        this.lastProtectedSlot = slot;
    }
}
