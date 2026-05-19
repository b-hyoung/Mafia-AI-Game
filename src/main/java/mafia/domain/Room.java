package mafia.domain;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

public class Room {
    private final int roomId;
    private final String title;
    private final String hostNickname;
    private final int maxPlayers;
    private final IntegerProperty currentPlayers;
    private final ObjectProperty<RoomState> state;

    public Room(int roomId, String title, String hostNickname,
                int currentPlayers, int maxPlayers, RoomState state) {
        this.roomId = roomId;
        this.title = title;
        this.hostNickname = hostNickname;
        this.maxPlayers = maxPlayers;
        this.currentPlayers = new SimpleIntegerProperty(currentPlayers);
        this.state = new SimpleObjectProperty<>(state);
    }

    public int getRoomId() { return roomId; }
    public String getTitle() { return title; }
    public String getHostNickname() { return hostNickname; }
    public int getMaxPlayers() { return maxPlayers; }

    public int getCurrentPlayers() { return currentPlayers.get(); }
    public void setCurrentPlayers(int v) { currentPlayers.set(v); }
    public IntegerProperty currentPlayersProperty() { return currentPlayers; }

    public RoomState getState() { return state.get(); }
    public void setState(RoomState s) { state.set(s); }
    public ObjectProperty<RoomState> stateProperty() { return state; }
}
