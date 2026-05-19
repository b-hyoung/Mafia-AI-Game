package mafia.domain;

public enum RoomState {
    WAITING("대기"),
    IN_GAME("진행중");

    private final String label;

    RoomState(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
