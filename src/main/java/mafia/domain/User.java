package mafia.domain;

import java.time.LocalDateTime;

public class User {
    private int user_id;
    private String nickname;
    private String password_hash;
    private int wins;
    private int losses;
    private boolean is_bot;
    private LocalDateTime created_at;

    public User(String nickname, String password_hash) {
        this.nickname = nickname;
        this.password_hash = password_hash;
    }

    public int getUser_id() {
        return user_id;
    }
    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }
    public String getNickname() {
        return nickname;
    }
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    public String getPassword_hash() {
        return password_hash;
    }
    public void setPassword_hash(String password_hash) {
        this.password_hash = password_hash;
    }
    public int getWins() {
        return wins;
    }
    public void setWins(int wins) {
        this.wins = wins;
    }
    public int getLosses() {
        return losses;
    }
    public void setLosses(int losses) {
        this.losses = losses;
    }
    public boolean isIs_bot() {
        return is_bot;
    }
    public void setIs_bot(boolean is_bot) {
        this.is_bot = is_bot;
    }
    public LocalDateTime getCreated_at() {
        return created_at;
    }
    public void setCreated_at(LocalDateTime created_at) {
        this.created_at = created_at;
    }
}
