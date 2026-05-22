package mafia.engine;

import mafia.domain.Player;
import mafia.domain.Role;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class RoleAssignerTest {

    private List<Player> sixPlayers() {
        List<Player> players = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            players.add(new Player(i, "P" + i, "RANDOM"));
        }
        return players;
    }

    @Test
    void assign_distributesExactRoles_for6Players() {
        List<Player> players = sixPlayers();
        RoleAssigner.assign(players, 42L);

        Map<Role, Long> counts = players.stream()
            .collect(Collectors.groupingBy(Player::getRole, Collectors.counting()));

        assertEquals(2L, counts.getOrDefault(Role.MAFIA, 0L), "MAFIA 2명");
        assertEquals(1L, counts.getOrDefault(Role.CITIZEN, 0L), "CITIZEN 1명");
        assertEquals(1L, counts.getOrDefault(Role.POLICE, 0L), "POLICE 1명");
        assertEquals(1L, counts.getOrDefault(Role.DOCTOR, 0L), "DOCTOR 1명");
        assertEquals(1L, counts.getOrDefault(Role.PSYCHO, 0L), "PSYCHO 1명");
    }

    @Test
    void assign_sameSeed_producesSameOrder() {
        List<Player> a = sixPlayers();
        List<Player> b = sixPlayers();
        RoleAssigner.assign(a, 100L);
        RoleAssigner.assign(b, 100L);

        for (int i = 0; i < 6; i++) {
            assertEquals(a.get(i).getRole(), b.get(i).getRole(),
                "같은 seed → 같은 배정");
        }
    }

    @Test
    void assign_rejectsWrongPlayerCount() {
        List<Player> five = new ArrayList<>();
        for (int i = 1; i <= 5; i++) five.add(new Player(i, "P" + i, "RANDOM"));

        assertThrows(IllegalArgumentException.class,
            () -> RoleAssigner.assign(five, 1L));
    }
}
