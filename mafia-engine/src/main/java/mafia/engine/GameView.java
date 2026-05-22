package mafia.engine;

import mafia.domain.GamePhase;
import mafia.domain.Player;
import mafia.domain.Role;

import java.util.List;

/**
 * 한 Agent가 "볼 수 있는" 게임 정보의 view.
 *
 * <p><b>왜 GameState를 직접 안 주고 View로 감싸는가?</b><br>
 * GameState는 모든 플레이어의 진짜 역할(MAFIA/CITIZEN 등)을 다 들고 있다.
 * 시민 봇이 GameState를 직접 받으면 다른 사람 역할을 곧장 알 수 있어 게임이 안 된다.
 * GameView는 "Agent 자기 자신의 역할 + 진영에 따라 노출 가능한 추가 정보"만 노출.
 *
 * <h3>진영별 가시 범위</h3>
 * <ul>
 *   <li><b>마피아</b>: 본인 + 동료 마피아 정체 ({@link #teammates()})</li>
 *   <li><b>그 외</b>: 본인 정체만</li>
 * </ul>
 *
 * <h3>로그 분리</h3>
 * <ul>
 *   <li>{@link #publicLog()}: 전체 봇이 보는 공개 이벤트 (비공개 이벤트 제외)</li>
 *   <li>{@link #privateLog()}: 본인 관련 비공개 이벤트만 (예: 경찰 본인의 조사 결과)</li>
 * </ul>
 */
public class GameView {

    private final GameState state;
    private final Player me;
    private final EventLog log;

    public GameView(GameState state, Player me, EventLog log) {
        this.state = state;
        this.me = me;
        this.log = log;
    }

    public Player me() { return me; }

    public Role myRole() { return me.getRole(); }

    public List<Player> alivePlayers() { return state.alivePlayers(); }

    public List<Player> allPlayers() { return state.getPlayers(); }

    public int round() { return state.getRound(); }
    public GamePhase phase() { return state.getPhase(); }

    /**
     * 마피아 진영일 때, 본인을 제외한 동료 마피아 리스트.
     * 비마피아면 빈 리스트.
     */
    public List<Player> teammates() {
        if (me.getRole() != Role.MAFIA) return List.of();
        return state.getPlayers().stream()
            .filter(p -> p.getRole() == Role.MAFIA && !p.equals(me))
            .toList();
    }

    /**
     * 공개 이벤트 로그 — 모든 봇이 볼 수 있는 이벤트.
     * 비공개 이벤트({@link Event.Investigation})는 제외.
     */
    public List<Event> publicLog() {
        return log.events().stream()
            .filter(e -> !(e instanceof Event.Investigation))
            .toList();
    }

    /**
     * 본인 관련 비공개 이벤트 로그.
     * 현재는 본인이 officer인 {@link Event.Investigation}만 포함.
     */
    public List<Event> privateLog() {
        return log.events().stream()
            .filter(e -> e instanceof Event.Investigation inv && inv.officer().equals(me))
            .toList();
    }
}
