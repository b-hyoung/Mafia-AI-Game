package GUI.components;

import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Circle;

import java.net.URL;

/**
 * 로고 노드 생성 헬퍼.
 * /videos/logo.mp4가 있으면 무한 루프 + 음소거 + 중앙 정사각형 viewport + 원형 클립으로 재생.
 * 없으면 /images/logo.png를 같은 방식으로 표시. 둘 다 없으면 null 반환.
 */
public class Logo {

    public static Node create(double size) {
        URL videoUrl = Logo.class.getResource("/videos/logo.mp4");
        URL imageUrl = Logo.class.getResource("/images/logo.png");

        if (videoUrl != null) {
            Media media = new Media(videoUrl.toExternalForm());
            MediaPlayer player = new MediaPlayer(media);
            player.setCycleCount(MediaPlayer.INDEFINITE);
            player.setMute(true);
            player.setAutoPlay(true);
            MediaView view = new MediaView(player);
            view.setFitWidth(size);
            view.setFitHeight(size);
            view.setPreserveRatio(true);
            view.setClip(new Circle(size / 2, size / 2, size / 2));
            player.setOnReady(() -> {
                double mw = media.getWidth();
                double mh = media.getHeight();
                if (mw > 0 && mh > 0) {
                    double s = Math.min(mw, mh);
                    double x = (mw - s) / 2.0;
                    double y = (mh - s) / 2.0;
                    view.setViewport(new Rectangle2D(x, y, s, s));
                }
            });
            return view;
        }

        if (imageUrl != null) {
            ImageView logo = new ImageView(new Image(imageUrl.toExternalForm()));
            logo.setFitWidth(size);
            logo.setFitHeight(size);
            logo.setPreserveRatio(true);
            logo.setClip(new Circle(size / 2, size / 2, size / 2));
            return logo;
        }

        return null;
    }
}
