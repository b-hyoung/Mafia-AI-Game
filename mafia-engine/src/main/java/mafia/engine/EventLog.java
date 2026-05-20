package mafia.engine;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * 시간순 이벤트 누적. JavaFX ObservableList 기반이라 UI 자동 갱신 가능.
 */
public class EventLog {

    private final ObservableList<Event> events = FXCollections.observableArrayList();

    public void append(Event event) {
        events.add(event);
    }

    public ObservableList<Event> events() {
        return events;
    }

    public void clear() {
        events.clear();
    }
}
