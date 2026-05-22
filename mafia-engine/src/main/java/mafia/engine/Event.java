package mafia.engine;

import mafia.domain.GamePhase;
import mafia.domain.Player;
import mafia.domain.Role;

import java.time.Instant;
import java.util.List;

/**
 * 게임 진행 중 발생하는 모든 이벤트의 sealed interface.
 * EventLog가 이 인터페이스를 시간순 누적.
 *
 * <h3>이벤트 분류</h3>
 * <ul>
 *   <li><b>시스템</b>: GameStarted, GameEnded, PhaseChanged</li>
 *   <li><b>밤 행동</b>: NightKill, Investigation</li>
 *   <li><b>낮 행동</b>: Vote, Executed</li>
 *   <li><b>발화</b>: Speak (자유 텍스트), ClaimRole/Accuse/Defend/Reveal/ProtectClaim/Pass (정형 클레임)</li>
 * </ul>
 */
public sealed interface Event permits
    Event.Speak, Event.Vote, Event.Executed, Event.NightKill,
    Event.Investigation, Event.PhaseChanged, Event.GameStarted, Event.GameEnded,
    Event.ClaimRole, Event.Accuse, Event.Defend, Event.Reveal, Event.ProtectClaim, Event.Pass {

    int round();
    GamePhase phase();
    Instant timestamp();

    // ────────────────────────────────────────────────────────────
    // 시스템 / 행동 (기존)
    // ────────────────────────────────────────────────────────────

    record Speak(int round, GamePhase phase, Player actor, String text, Instant timestamp) implements Event {}

    record Vote(int round, GamePhase phase, Player voter, Player target, Instant timestamp) implements Event {}

    record Executed(int round, GamePhase phase, Player target, int voteCount, Instant timestamp) implements Event {}

    record NightKill(int round, GamePhase phase, Player killer, Player target, Instant timestamp) implements Event {}

    record Investigation(int round, GamePhase phase, Player officer, Player target, boolean isMafia, Instant timestamp) implements Event {}

    record PhaseChanged(int round, GamePhase phase, GamePhase from, GamePhase to, Instant timestamp) implements Event {}

    record GameStarted(int round, GamePhase phase, List<Player> players, Instant timestamp) implements Event {}

    record GameEnded(int round, GamePhase phase, Role winnerTeam, Instant timestamp) implements Event {}

    // ────────────────────────────────────────────────────────────
    // 정형 클레임 (DAY_DISCUSSION 페이즈에서 봇이 발언)
    // ────────────────────────────────────────────────────────────

    /** "내가 X 역할이다" — 본인 역할 주장 (진실/거짓 룰상 모두 허용). */
    record ClaimRole(int round, GamePhase phase, Player speaker, Role role, Instant timestamp) implements Event {}

    /** "target은 마피아 같다" — 타깃 비난. */
    record Accuse(int round, GamePhase phase, Player speaker, Player target, Instant timestamp) implements Event {}

    /** "target은 시민 같다" — 타깃 옹호. */
    record Defend(int round, GamePhase phase, Player speaker, Player target, Instant timestamp) implements Event {}

    /** "target을 조사했더니 claimedRole이었다" — 경찰 클레임 (또는 거짓 가짜 경찰 클레임). */
    record Reveal(int round, GamePhase phase, Player speaker, Player target, Role claimedRole, Instant timestamp) implements Event {}

    /** "어제 target을 보호했다" — 의사 클레임 (또는 거짓 가짜 의사 클레임). */
    record ProtectClaim(int round, GamePhase phase, Player speaker, Player target, Instant timestamp) implements Event {}

    /** 발언 없음 (정보 부족 등). */
    record Pass(int round, GamePhase phase, Player speaker, Instant timestamp) implements Event {}
}
