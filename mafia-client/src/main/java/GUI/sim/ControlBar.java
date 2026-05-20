package GUI.sim;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * 상단 컨트롤 바 — Play/Pause/Step/New 버튼.
 * 동작 콜백은 SimulationController가 주입.
 */
public class ControlBar {

    private final HBox root;
    public final Button playBtn;
    public final Button pauseBtn;
    public final Button stepBtn;
    public final Button newGameBtn;

    public ControlBar() {
        root = new HBox();
        root.getStyleClass().add("sim-topbar");

        Label title = new Label("Mafia 시뮬레이션");
        title.getStyleClass().add("sim-title");

        playBtn = btn("▶ Play");
        pauseBtn = btn("⏸ Pause");
        stepBtn = btn("⏭ Step");
        newGameBtn = btn("🔁 New Game");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label hint = new Label("관전자 모드");
        hint.getStyleClass().add("sim-status");

        root.getChildren().addAll(title, spacer, playBtn, pauseBtn, stepBtn, newGameBtn, hint);
    }

    public HBox getRoot() { return root; }

    private Button btn(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("sim-ctrl-btn");
        return b;
    }
}
