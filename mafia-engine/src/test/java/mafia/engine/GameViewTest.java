package mafia.engine;

import mafia.domain.GamePhase;
import mafia.domain.Player;
import mafia.domain.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameViewTest {

    private List<Player> sixPlayers() {
        List<Player> players = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            players.add(new Player(i, "P" + i, "RANDOM"));
        }
        return players;
    }

    @Test
    void teammates_forMafia_returnsOtherMafia() {
        List<Player> players = sixPlayers();
        players.get(0).setRole(Role.MAFIA);  // P1
        players.get(1).setRole(Role.MAFIA);  // P2
        players.get(2).setRole(Role.CITIZEN);
        players.get(3).setRole(Role.POLICE);
        players.get(4).setRole(Role.DOCTOR);
        players.get(5).setRole(Role.PSYCHO);

        GameState state = new GameState(players);
        EventLog log = new EventLog();

        GameView mafiaView = new GameView(state, players.get(0), log);
        List<Player> teammates = mafiaView.teammates();

        assertEquals(1, teammates.size(), "P1의 동료 마피아는 P2 1명");
        assertEquals(2, teammates.get(0).getSlot());
    }

    @Test
    void teammates_forNonMafia_returnsEmpty() {
        List<Player> players = sixPlayers();
        players.get(0).setRole(Role.MAFIA);
        players.get(1).setRole(Role.CITIZEN);

        GameState state = new GameState(players);
        EventLog log = new EventLog();

        GameView citizenView = new GameView(state, players.get(1), log);
        assertTrue(citizenView.teammates().isEmpty(),
            "비마피아는 동료 없음 → 빈 리스트");
    }

    @Test
    void publicLog_excludesInvestigation() {
        List<Player> players = sixPlayers();
        players.get(0).setRole(Role.POLICE);
        players.get(1).setRole(Role.CITIZEN);

        GameState state = new GameState(players);
        EventLog log = new EventLog();

        Event.Vote vote = new Event.Vote(
            1, GamePhase.DAY_VOTE, players.get(0), players.get(1), Instant.EPOCH);
        Event.Investigation inv = new Event.Investigation(
            1, GamePhase.NIGHT, players.get(0), players.get(1), false, Instant.EPOCH);
        log.append(vote);
        log.append(inv);

        GameView citizenView = new GameView(state, players.get(1), log);
        List<Event> publicEvents = citizenView.publicLog();

        assertEquals(1, publicEvents.size(), "Investigation은 공개 로그에서 제외");
        assertTrue(publicEvents.get(0) instanceof Event.Vote);
    }

    @Test
    void privateLog_includesOnlyOwnInvestigation() {
        List<Player> players = sixPlayers();
        players.get(0).setRole(Role.POLICE);
        players.get(1).setRole(Role.CITIZEN);

        GameState state = new GameState(players);
        EventLog log = new EventLog();

        Event.Investigation myInv = new Event.Investigation(
            1, GamePhase.NIGHT, players.get(0), players.get(1), false, Instant.EPOCH);
        log.append(myInv);

        GameView policeView = new GameView(state, players.get(0), log);
        GameView citizenView = new GameView(state, players.get(1), log);

        assertEquals(1, policeView.privateLog().size(),
            "경찰 본인은 자기 조사 결과를 privateLog로 볼 수 있음");
        assertTrue(citizenView.privateLog().isEmpty(),
            "다른 사람은 경찰 조사 결과 못 봄");
    }

    @Test
    void publicLog_includesAllNonPrivateEvents() {
        List<Player> players = sixPlayers();
        players.get(0).setRole(Role.MAFIA);
        players.get(1).setRole(Role.CITIZEN);

        GameState state = new GameState(players);
        EventLog log = new EventLog();

        Event.ClaimRole claim = new Event.ClaimRole(
            1, GamePhase.DAY_DISCUSSION, players.get(0), Role.CITIZEN, Instant.EPOCH);
        Event.NightKill kill = new Event.NightKill(
            1, GamePhase.NIGHT, players.get(0), players.get(1), Instant.EPOCH);
        log.append(claim);
        log.append(kill);

        GameView view = new GameView(state, players.get(1), log);
        assertEquals(2, view.publicLog().size(),
            "ClaimRole과 NightKill은 모두 공개 이벤트");
    }
}
