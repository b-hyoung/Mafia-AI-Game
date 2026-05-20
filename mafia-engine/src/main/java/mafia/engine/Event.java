package mafia.engine;

import mafia.domain.GamePhase;
import mafia.domain.Player;
import mafia.domain.Role;

import java.time.Instant;
import java.util.List;

/**
 * 게임 진행 중 발생하는 모든 이벤트의 sealed interface.
 * EventLog가 이 인터페이스를 시간순 누적.
 */
public sealed interface Event permits
    Event.Speak, Event.Vote, Event.Executed, Event.NightKill,
    Event.Investigation, Event.PhaseChanged, Event.GameStarted, Event.GameEnded {

    int round();
    GamePhase phase();
    Instant timestamp();

    record Speak(int round, GamePhase phase, Player actor, String text, Instant timestamp) implements Event {}

    record Vote(int round, GamePhase phase, Player voter, Player target, Instant timestamp) implements Event {}

    record Executed(int round, GamePhase phase, Player target, int voteCount, Instant timestamp) implements Event {}

    record NightKill(int round, GamePhase phase, Player killer, Player target, Instant timestamp) implements Event {}

    record Investigation(int round, GamePhase phase, Player officer, Player target, boolean isMafia, Instant timestamp) implements Event {}

    record PhaseChanged(int round, GamePhase phase, GamePhase from, GamePhase to, Instant timestamp) implements Event {}

    record GameStarted(int round, GamePhase phase, List<Player> players, Instant timestamp) implements Event {}

    record GameEnded(int round, GamePhase phase, Role winnerTeam, Instant timestamp) implements Event {}
}
