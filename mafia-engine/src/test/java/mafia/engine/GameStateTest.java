package mafia.engine;

import mafia.domain.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    private List<Player> sixPlayers() {
        List<Player> players = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            players.add(new Player(i, "P" + i, "RANDOM"));
        }
        return players;
    }

    @Test
    void newGameState_lastProtectedSlot_isNegativeOne() {
        GameState state = new GameState(sixPlayers());
        assertEquals(-1, state.getLastProtectedSlot(),
            "게임 시작 시 보호 기록 없음 → -1");
    }

    @Test
    void setLastProtectedSlot_storesAndReturns() {
        GameState state = new GameState(sixPlayers());
        state.setLastProtectedSlot(3);
        assertEquals(3, state.getLastProtectedSlot());
    }
}
