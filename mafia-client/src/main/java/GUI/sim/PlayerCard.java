package GUI.sim;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import mafia.domain.Player;
import mafia.domain.Role;

/**
 * 한 명의 플레이어 카드. 닉네임, 봇 종류, 역할, 생사 상태 표시.
 * 클릭하면 콜백 호출 (추론 패널에 표시할 봇 선택용).
 */
public class PlayerCard {

    public static VBox create(Player player, Runnable onClick) {
        VBox card = new VBox();
        card.getStyleClass().add("player-card");
        card.setAlignment(Pos.CENTER);

        Label icon = new Label("🎭");
        icon.getStyleClass().add("player-card-icon");

        Label nick = new Label(player.getNickname());
        nick.getStyleClass().add("player-card-nick");

        Label botType = new Label(player.getBotType());
        botType.getStyleClass().add("player-card-bottype");

        Label role = new Label();
        role.getStyleClass().add("player-card-role");
        // 역할은 처음엔 ?, 게임 끝나면 공개
        role.textProperty().bind(Bindings.createStringBinding(
            () -> player.getRole() == null ? "?" : player.getRole().getLabel(),
            player.roleProperty()
        ));

        card.getChildren().addAll(icon, nick, botType, role);

        // 사망 시 흐릿 + 아이콘 변경
        player.aliveProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                if (!card.getStyleClass().contains("player-card-dead")) {
                    card.getStyleClass().add("player-card-dead");
                }
                icon.setText("💀");
            } else {
                card.getStyleClass().remove("player-card-dead");
                icon.setText("🎭");
            }
        });

        card.setOnMouseClicked(e -> {
            if (onClick != null) onClick.run();
        });

        return card;
    }

    public static void setSelected(VBox card, boolean selected) {
        if (selected) {
            if (!card.getStyleClass().contains("player-card-selected")) {
                card.getStyleClass().add("player-card-selected");
            }
        } else {
            card.getStyleClass().remove("player-card-selected");
        }
    }
}
