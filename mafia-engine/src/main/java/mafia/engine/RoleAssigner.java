package mafia.engine;

import mafia.domain.Player;
import mafia.domain.Role;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 6명 게임의 역할을 랜덤하게 배정.
 *
 * <h3>배정 룰</h3>
 * <ul>
 *   <li>마피아 2명</li>
 *   <li>시민 1명</li>
 *   <li>경찰 1명</li>
 *   <li>의사 1명</li>
 *   <li>PSYCHO 1명</li>
 * </ul>
 *
 * <h3>seed의 의미</h3>
 * 같은 seed → 같은 배정 결과. 디버깅 시 "seed 42로 다시 굴려" 하면 동일 게임 재현 가능.
 * 시뮬레이션 통계 수집 시 seed를 1, 2, 3, ... 으로 순차 증가시키면 다양한 게임 패턴 확보.
 */
public class RoleAssigner {

    /**
     * 입력 플레이어 리스트의 각 멤버에게 역할을 직접 set한다 (in-place 변경).
     *
     * @param players 6명 플레이어 (정확히 6명이어야 함)
     * @param seed    랜덤 seed (재현 가능)
     * @throws IllegalArgumentException 인원수가 6이 아니면
     */
    public static void assign(List<Player> players, long seed) {
        if (players.size() != 6) {
            throw new IllegalArgumentException("RoleAssigner는 6명 게임만 지원 (현재 " + players.size() + "명)");
        }

        List<Role> roles = new ArrayList<>(List.of(
            Role.MAFIA,
            Role.MAFIA,
            Role.CITIZEN,
            Role.POLICE,
            Role.DOCTOR,
            Role.PSYCHO
        ));

        Collections.shuffle(roles, new Random(seed));

        for (int i = 0; i < players.size(); i++) {
            players.get(i).setRole(roles.get(i));
        }
    }
}
