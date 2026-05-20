package mafia.domain;

public enum GamePhase {
    DAY_DISCUSSION("낮 토론"),
    DAY_VOTE("낮 투표"),
    NIGHT("밤"),
    ENDED("종료");

    private final String label;

    GamePhase(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
