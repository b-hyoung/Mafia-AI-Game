package mafia.engine;

import mafia.domain.Player;
import mafia.domain.Role;

import java.util.List;

/**
 * 승리 조건 판정 (3진영 게임).
 *
 * <h3>판정 룰 (살아있는 플레이어 기준)</h3>
 * <ol>
 *   <li>마피아 ≥ 비마피아 → <b>마피아 승</b> (다수결 우위)</li>
 *   <li>마피아 0 AND PSYCHO 생존 → <b>PSYCHO 승</b></li>
 *   <li>마피아 0 AND PSYCHO 사망 → <b>시민 승</b></li>
 *   <li>그 외 → <b>UNDECIDED</b> (게임 진행 중)</li>
 * </ol>
 *
 * <h3>호출 시점</h3>
 * GameEngine이 매 페이즈 끝에 호출. UNDECIDED면 다음 페이즈 진행, 아니면 게임 종료.
 */
public class WinCondition {

    /**
     * 승리 진영.
     * <ul>
     *   <li>{@link #CITIZEN} — 시민 진영 승</li>
     *   <li>{@link #MAFIA} — 마피아 진영 승</li>
     *   <li>{@link #PSYCHO} — PSYCHO 단독 승</li>
     *   <li>{@link #UNDECIDED} — 아직 진행 중</li>
     * </ul>
     */
    public enum Winner { CITIZEN, MAFIA, PSYCHO, UNDECIDED }

    /**
     * 현재 살아있는 사람들로 승리 여부 판정.
     *
     * @param alive {@link GameState#alivePlayers()} 결과를 넣어주면 됨
     * @return 승리 진영 또는 UNDECIDED
     */
    public static Winner check(List<Player> alive) {
        long mafiaCount = alive.stream()
            .filter(p -> p.getRole() == Role.MAFIA)
            .count();
        long nonMafiaCount = alive.size() - mafiaCount;
        boolean psychoAlive = alive.stream()
            .anyMatch(p -> p.getRole() == Role.PSYCHO);

        // 1. 마피아 우위 (1:1 동수도 마피아 승)
        if (mafiaCount > 0 && mafiaCount >= nonMafiaCount) return Winner.MAFIA;

        // 2. 마피아 전멸
        if (mafiaCount == 0) {
            return psychoAlive ? Winner.PSYCHO : Winner.CITIZEN;
        }

        // 3. 그 외 — 마피아는 있지만 비마피아가 더 많음
        return Winner.UNDECIDED;
    }
}
