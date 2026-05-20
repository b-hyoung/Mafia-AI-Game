package GUI.sim;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import mafia.engine.Event;

/**
 * 이벤트/채팅 로그를 시간순으로 표시.
 * ObservableList<Event>를 받아 자동 갱신.
 */
public class EventLogPanel {

    private final VBox root;
    private final VBox listBox;
    private final ScrollPane scroll;

    public EventLogPanel(ObservableList<Event> events) {
        root = new VBox();
        root.getStyleClass().add("sim-panel");

        Label header = new Label("이벤트 / 채팅");
        header.getStyleClass().add("sim-panel-header");

        listBox = new VBox();

        scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.setPrefHeight(400);

        root.getChildren().addAll(header, scroll);

        // 초기 데이터 + 변경 리스너
        for (Event e : events) addRow(e);
        events.addListener((ListChangeListener<Event>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (Event e : c.getAddedSubList()) {
                        Platform.runLater(() -> {
                            addRow(e);
                            scroll.setVvalue(1.0);  // 자동 스크롤 최하단
                        });
                    }
                }
                if (c.wasRemoved()) {
                    Platform.runLater(() -> listBox.getChildren().clear());
                }
            }
        });
    }

    public VBox getRoot() { return root; }

    private void addRow(Event e) {
        Label row = new Label(format(e));
        row.getStyleClass().add("event-row");
        row.getStyleClass().add(styleClassFor(e));
        row.setWrapText(true);
        listBox.getChildren().add(row);
    }

    private static String format(Event e) {
        String prefix = "[R" + e.round() + " " + e.phase().getLabel() + "] ";
        return switch (e) {
            case Event.Speak s -> prefix + s.actor() + ": " + s.text();
            case Event.Vote v -> prefix + v.voter() + " → " + v.target();
            case Event.Executed ex -> prefix + "처형: " + ex.target() + " (득표 " + ex.voteCount() + ")";
            case Event.NightKill k -> prefix + "살해: " + k.target() + " (by " + k.killer() + ")";
            case Event.Investigation i -> prefix + i.officer() + "이 " + i.target() + " 조사 → " + (i.isMafia() ? "마피아" : "시민");
            case Event.PhaseChanged pc -> prefix + pc.from().getLabel() + " → " + pc.to().getLabel();
            case Event.GameStarted gs -> prefix + "게임 시작 (" + gs.players().size() + "명)";
            case Event.GameEnded ge -> prefix + "게임 종료 — 승자: " + ge.winnerTeam().getLabel();
        };
    }

    private static String styleClassFor(Event e) {
        return switch (e) {
            case Event.Speak s -> "event-row-speak";
            case Event.Vote v -> "event-row-vote";
            case Event.Executed ex -> "event-row-executed";
            case Event.NightKill k -> "event-row-kill";
            case Event.Investigation i -> "event-row-invest";
            case Event.PhaseChanged pc -> "event-row-phase";
            case Event.GameStarted gs -> "event-row-game";
            case Event.GameEnded ge -> "event-row-game";
        };
    }
}
