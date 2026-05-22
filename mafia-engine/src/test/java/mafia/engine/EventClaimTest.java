package mafia.engine;

import mafia.domain.GamePhase;
import mafia.domain.Player;
import mafia.domain.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class EventClaimTest {

    private Player p(int slot) {
        return new Player(slot, "P" + slot, "RANDOM");
    }

    @Test
    void claimRole_record_holdsAllFields() {
        Event.ClaimRole e = new Event.ClaimRole(
            1, GamePhase.DAY_DISCUSSION, p(1), Role.POLICE, Instant.EPOCH
        );
        assertEquals(1, e.round());
        assertEquals(GamePhase.DAY_DISCUSSION, e.phase());
        assertEquals(1, e.speaker().getSlot());
        assertEquals(Role.POLICE, e.role());
    }

    @Test
    void accuse_holdsSpeakerAndTarget() {
        Event.Accuse e = new Event.Accuse(
            1, GamePhase.DAY_DISCUSSION, p(2), p(3), Instant.EPOCH
        );
        assertEquals(2, e.speaker().getSlot());
        assertEquals(3, e.target().getSlot());
    }

    @Test
    void defend_holdsSpeakerAndTarget() {
        Event.Defend e = new Event.Defend(
            1, GamePhase.DAY_DISCUSSION, p(2), p(3), Instant.EPOCH
        );
        assertEquals(2, e.speaker().getSlot());
        assertEquals(3, e.target().getSlot());
    }

    @Test
    void reveal_holdsClaimedRole() {
        Event.Reveal e = new Event.Reveal(
            2, GamePhase.DAY_DISCUSSION, p(4), p(5), Role.MAFIA, Instant.EPOCH
        );
        assertEquals(4, e.speaker().getSlot());
        assertEquals(5, e.target().getSlot());
        assertEquals(Role.MAFIA, e.claimedRole());
    }

    @Test
    void protectClaim_holdsSpeakerAndTarget() {
        Event.ProtectClaim e = new Event.ProtectClaim(
            2, GamePhase.DAY_DISCUSSION, p(6), p(6), Instant.EPOCH
        );
        assertEquals(6, e.speaker().getSlot());
        assertEquals(6, e.target().getSlot());
    }

    @Test
    void pass_holdsSpeakerOnly() {
        Event.Pass e = new Event.Pass(
            1, GamePhase.DAY_DISCUSSION, p(3), Instant.EPOCH
        );
        assertEquals(3, e.speaker().getSlot());
    }

    @Test
    void allClaims_implementEventInterface() {
        Player speaker = p(1);
        Event.ClaimRole c = new Event.ClaimRole(1, GamePhase.DAY_DISCUSSION, speaker, Role.CITIZEN, Instant.EPOCH);
        Event asEvent = c;  // sealed interface 호환 검증
        assertEquals(1, asEvent.round());
        assertEquals(GamePhase.DAY_DISCUSSION, asEvent.phase());
    }
}
