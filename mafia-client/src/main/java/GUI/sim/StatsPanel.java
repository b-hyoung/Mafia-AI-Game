package GUI.sim;

import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * 누적 시뮬레이션 통계.
 * 시민/마피아 승수, 진행 중 게임 정보.
 */
public class StatsPanel {

    private final VBox root;
    private final IntegerProperty citizenWins = new SimpleIntegerProperty(0);
    private final IntegerProperty mafiaWins = new SimpleIntegerProperty(0);
    private final StringProperty currentRound = new SimpleStringProperty("-");
    private final StringProperty currentPhase = new SimpleStringProperty("-");

    public StatsPanel() {
        root = new VBox();
        root.getStyleClass().add("sim-panel");
        root.setPrefWidth(220);

        Label header = new Label("누적 통계");
        header.getStyleClass().add("sim-panel-header");

        Label citizenLine = new Label();
        citizenLine.getStyleClass().add("stats-line");
        citizenLine.textProperty().bind(Bindings.createStringBinding(
            () -> "시민  " + citizenWins.get() + "승 (" + percent(citizenWins.get(), total()) + "%)",
            citizenWins, mafiaWins
        ));

        Label mafiaLine = new Label();
        mafiaLine.getStyleClass().add("stats-line-accent");
        mafiaLine.textProperty().bind(Bindings.createStringBinding(
            () -> "마피아 " + mafiaWins.get() + "승 (" + percent(mafiaWins.get(), total()) + "%)",
            citizenWins, mafiaWins
        ));

        Label divider = new Label("──────────");
        divider.getStyleClass().add("stats-line");

        Label gameHeader = new Label("현재 게임");
        gameHeader.getStyleClass().add("sim-panel-header");

        Label roundLine = new Label();
        roundLine.getStyleClass().add("stats-line");
        roundLine.textProperty().bind(currentRound.concat(""));

        Label phaseLine = new Label();
        phaseLine.getStyleClass().add("stats-line");
        phaseLine.textProperty().bind(currentPhase.concat(""));

        root.getChildren().addAll(header, citizenLine, mafiaLine, divider, gameHeader, roundLine, phaseLine);
    }

    public VBox getRoot() { return root; }

    public void recordCitizenWin() { citizenWins.set(citizenWins.get() + 1); }
    public void recordMafiaWin() { mafiaWins.set(mafiaWins.get() + 1); }

    public void setCurrentRound(int round) { currentRound.set("Round " + round); }
    public void setCurrentPhase(String label) { currentPhase.set("Phase: " + label); }

    public void reset() {
        citizenWins.set(0);
        mafiaWins.set(0);
        currentRound.set("-");
        currentPhase.set("-");
    }

    private int total() { return citizenWins.get() + mafiaWins.get(); }

    private static String percent(int part, int total) {
        if (total == 0) return "0";
        return String.valueOf((int) Math.round(100.0 * part / total));
    }
}
