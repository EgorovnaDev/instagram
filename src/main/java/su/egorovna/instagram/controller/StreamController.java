package su.egorovna.instagram.controller;

import com.github.instagram4j.instagram4j.models.media.timeline.Comment;
import com.github.instagram4j.instagram4j.models.user.Profile;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import io.datafx.controller.ViewController;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.context.ActionHandler;
import io.datafx.controller.flow.context.FlowActionHandler;
import io.datafx.controller.util.VetoException;
import io.datafx.core.concurrent.ProcessChain;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.egorovna.instagram.live.InstagramApi;
import su.egorovna.instagram.ui.InstagramListItem;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurfaceFactory.videoSurfaceForImageView;

@ViewController(value = "/fxml/stream.fxml", title = "Instagram Live")
public class StreamController extends Controller {

    private static final Logger LOG = LoggerFactory.getLogger(StreamController.class);

    @ActionHandler
    private FlowActionHandler handler;

    @FXML
    private StackPane root;

    @FXML
    private Tab actions;

    @FXML
    private Circle userPic;

    @FXML
    private Label fullName;

    @FXML
    private Tab comments;

    @FXML
    private ToggleSwitch commentsSwitch;

    @FXML
    private VBox commentsContainer;

    @FXML
    private Tab viewers;

    @FXML
    private VBox viewersContainer;

    @FXML
    private Tab preview;

    @FXML
    private StackPane playerContainer;

    @FXML
    private ImageView player;

    private Timer info;
    private Timer comment;
    private Timer viewer;
    private Long lastTs;
    private MediaPlayerFactory mediaPlayerFactory;
    private EmbeddedMediaPlayer embeddedMediaPlayer;


    @PostConstruct
    private void init() {
        lastTs = 0L;
        userPic.setFill(new ImagePattern(new Image(InstagramApi.getClient().getSelfProfile().getProfile_pic_url())));
        fullName.setText(InstagramApi.getClient().getSelfProfile().getFull_name());
        mediaPlayerFactory = new MediaPlayerFactory();
        embeddedMediaPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();
        embeddedMediaPlayer.videoSurface().set(videoSurfaceForImageView(player));
        player.fitWidthProperty().bind(playerContainer.widthProperty());
        player.fitHeightProperty().bind(playerContainer.heightProperty());
        commentsSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                InstagramApi.unmuteComment();
                enableCommentTimer();
            } else {
                InstagramApi.muteComment();
                comment.cancel();
            }
        });
        enableInfoTimer();
        enableCommentTimer();
        enableViewerTimer();
    }

    private void enableViewerTimer() {
        viewer = new Timer();
        viewer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    List<Profile> p = InstagramApi.getViewers();
                    Platform.runLater(() -> {
                        viewersContainer.getChildren().clear();
                        if (p != null && !p.isEmpty()) {
                            p.forEach(profile -> viewersContainer.getChildren().add(new InstagramListItem(profile)));
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, TimeUnit.SECONDS.toMillis(10));
    }

    private void enableCommentTimer() {
        comment = new Timer();
        comment.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    List<Comment> c = InstagramApi.getComments(lastTs);
                    if (c != null && !c.isEmpty()) {
                        c.forEach(commentItem -> {
                            Platform.runLater(() -> {
                                commentsContainer.getChildren().add(new InstagramListItem(commentItem));
                                lastTs = c.get(c.size() - 1).getCreated_at();
                            });
                        });

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, TimeUnit.SECONDS.toMillis(5));
    }

    private void enableInfoTimer() {
        info = new Timer();
        info.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                String url = InstagramApi.dashPlaybackUrl();
                if (url != null) {
                    Platform.runLater(() -> {
                        preview.setDisable(false);
                        embeddedMediaPlayer.overlay().enable(true);
                        embeddedMediaPlayer.media().prepare(url);
                        embeddedMediaPlayer.controls().play();
                        embeddedMediaPlayer.audio().mute();
                        LOG.debug(url);
                    });
                    info.cancel();
                }
            }
        }, 0, TimeUnit.SECONDS.toMillis(5));
    }

    @FXML
    void onStop(ActionEvent event) {
        ProcessChain.create()
                .addRunnableInPlatformThread(this::lock)
                .addRunnableInPlatformThread(() -> startLoading(root, "Stop broadcasting"))
                .addSupplierInExecutor(() -> {
                    try {
                        InstagramApi.stopBroadcast();
                    } catch (Exception e) {
                        LOG.debug("Ошибка остановки трансляции", e);
                        return false;
                    }
                    return true;
                })
                .addConsumerInPlatformThread(aBoolean -> {
                    if (aBoolean) {
                        try {
                            embeddedMediaPlayer.controls().stop();
                            embeddedMediaPlayer.release();
                            mediaPlayerFactory.release();
                            info.cancel();
                            comment.cancel();
                            viewer.cancel();
                            handler.navigate(CreateController.class);
                        } catch (VetoException | FlowException e) {
                            LOG.error("Системная ошибка", e);
                            unlock();
                            stopLoading();
                        }
                    }
                })
                .run();
    }

    @FXML
    void onMutePreview(ActionEvent event) {
        embeddedMediaPlayer.audio().setMute(!embeddedMediaPlayer.audio().isMute());
        Button button = (Button) event.getSource();
        button.setGraphic(embeddedMediaPlayer.audio().isMute() ? new MaterialDesignIconView(MaterialDesignIcon.VOLUME_HIGH, "36.0") : new MaterialDesignIconView(MaterialDesignIcon.VOLUME_OFF, "36.0"));
    }

    @FXML
    void onReloadPreview(ActionEvent event) {
        embeddedMediaPlayer.controls().stop();
        embeddedMediaPlayer.controls().play();
    }

    @Override
    protected void lock() {

    }

    @Override
    protected void unlock() {

    }
}
