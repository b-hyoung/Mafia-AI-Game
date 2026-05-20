package mafia.domain;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * 게임 한 판의 슬롯 단위 플레이어 (DB의 User와 다른 개념).
 * 게임 시작 시 역할 배정 + 라운드 진행하며 생사 변동.
 */
public class Player {
    private final int slot;            // 1~5
    private final String nickname;
    private final String botType;      // "RANDOM" / "RULE" / "CSP" / "LLM"
    private final ObjectProperty<Role> role = new SimpleObjectProperty<>();
    private final BooleanProperty alive = new SimpleBooleanProperty(true);

    public Player(int slot, String nickname, String botType) {
        this.slot = slot;
        this.nickname = nickname;
        this.botType = botType;
    }

    public int getSlot() { return slot; }
    public String getNickname() { return nickname; }
    public String getBotType() { return botType; }

    public Role getRole() { return role.get(); }
    public void setRole(Role r) { role.set(r); }
    public ObjectProperty<Role> roleProperty() { return role; }

    public boolean isAlive() { return alive.get(); }
    public void kill() { alive.set(false); }
    public BooleanProperty aliveProperty() { return alive; }

    @Override
    public String toString() {
        return "P" + slot + "(" + nickname + ")";
    }
}
