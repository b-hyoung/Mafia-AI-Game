package mafia.domain;

public enum Role {
    MAFIA("마피아"),
    CITIZEN("시민"),
    POLICE("경찰"),
    DOCTOR("의사"),
    PSYCHO("정병");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isMafiaTeam() {
        return this == MAFIA;
    }
}
