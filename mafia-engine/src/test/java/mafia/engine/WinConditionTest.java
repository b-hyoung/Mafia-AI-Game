package mafia.engine;

import mafia.domain.Player;
import mafia.domain.Role;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WinConditionTest {

    private Player player(int slot, Role role) {
        Player p = new Player(slot, "P" + slot, "RANDOM");
        p.setRole(role);
        return p;
    }

    @Test
    void mafiaWins_whenMafiaCountGteNonMafia() {
        // 마피아 1 + 시민 1 → 마피아 1 ≥ 비마피아 1 → 마피아 승
        List<Player> alive = List.of(
            player(1, Role.MAFIA),
            player(2, Role.CITIZEN)
        );
        assertEquals(WinCondition.Winner.MAFIA, WinCondition.check(alive));
    }

    @Test
    void psychoWins_whenMafiaZero_andPsychoAlive() {
        // 시민 1 + PSYCHO 1 → 마피아 0 + PSYCHO 생존 → PSYCHO 승
        List<Player> alive = List.of(
            player(1, Role.CITIZEN),
            player(2, Role.PSYCHO)
        );
        assertEquals(WinCondition.Winner.PSYCHO, WinCondition.check(alive));
    }

    @Test
    void citizenWins_whenMafiaZero_andPsychoDead() {
        // 시민 1 + 경찰 1 → 마피아 0, PSYCHO 없음(=사망) → 시민 승
        List<Player> alive = List.of(
            player(1, Role.CITIZEN),
            player(2, Role.POLICE)
        );
        assertEquals(WinCondition.Winner.CITIZEN, WinCondition.check(alive));
    }

    @Test
    void undecided_whenMafiaAlive_butLessThanNonMafia() {
        // 마피아 1 + 시민 1 + 경찰 1 → 1 < 2 → 진행 중
        List<Player> alive = List.of(
            player(1, Role.MAFIA),
            player(2, Role.CITIZEN),
            player(3, Role.POLICE)
        );
        assertEquals(WinCondition.Winner.UNDECIDED, WinCondition.check(alive));
    }

    @Test
    void mafiaPsycho1v1_isMafiaWin() {
        // 마피아 1 + PSYCHO 1 → 마피아 1 ≥ 비마피아 1 → 마피아 승
        List<Player> alive = List.of(
            player(1, Role.MAFIA),
            player(2, Role.PSYCHO)
        );
        assertEquals(WinCondition.Winner.MAFIA, WinCondition.check(alive));
    }
}
