package su.egorovna.instagram.controller;

import com.github.instagram4j.instagram4j.models.media.timeline.Comment;
import com.github.instagram4j.instagram4j.models.user.Profile;
import io.datafx.controller.ViewController;
import io.datafx.controller.ViewNode;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.context.ActionHandler;
import io.datafx.controller.flow.context.FlowActionHandler;
import io.datafx.controller.util.VetoException;
import io.datafx.core.concurrent.ProcessChain;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.control.textfield.CustomTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.egorovna.instagram.live.InstagramApi;
import su.egorovna.instagram.ui.InstagramListItem;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static su.egorovna.instagram.ui.UiUtils.addClipboardButton;

@ViewController(value = "/fxml/stream.fxml", title = "Instagram Live")
public class StreamController extends Controller {

    private static final Logger LOG = LoggerFactory.getLogger(StreamController.class);

    @ActionHandler
    private FlowActionHandler handler;

    @ViewNode
    private StackPane root;

    @ViewNode
    private Tab actions;

    @ViewNode
    private Circle userPic;

    @ViewNode
    private Label fullName;

    @ViewNode
    private Label viewersCount;

    @ViewNode
    private VBox dashPlaybackUrlPane;

    @ViewNode
    private CustomTextField dashPlaybackUrl;

    @ViewNode
    @FXML
    private Tab comments;

    @ViewNode
    private ToggleSwitch commentsSwitch;

    @ViewNode
    @FXML
    private VBox commentsContainer;

    private Timer info;
    private Timer comment;
    private Timer viewer;
    private Long lastCommentTs;
    private Integer unread;

    @PostConstruct
    private void init() {
        unread = 0;
        lastCommentTs = 0L;
        userPic.setFill(new ImagePattern(new Image(InstagramApi.getClient().getSelfProfile().getProfile_pic_url())));
        fullName.setText(InstagramApi.getClient().getSelfProfile().getFull_name());
        commentsSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                InstagramApi.unmuteComment();
                enableCommentTimer();
            } else {
                InstagramApi.muteComment();
                comment.cancel();
            }
        });
        comments.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                comments.setText("Comments");
                unread = 0;
            }
        });
        dashPlaybackUrlPane.setVisible(false);
        dashPlaybackUrlPane.setManaged(false);
        addClipboardButton(dashPlaybackUrl);
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
                        if (p != null && !p.isEmpty()) {
                            viewersCount.setText(String.valueOf(p.size()));
                        } else {
                            viewersCount.setText(String.valueOf(0));
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, TimeUnit.SECONDS.toMillis(5));
    }

    private void enableCommentTimer() {
        comment = new Timer();
        comment.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    List<Comment> c = InstagramApi.getComments(lastCommentTs);
                    if (c != null) {
                        Platform.runLater(() -> {
                            if (!comments.isSelected()) {
                                unread += c.size();
                                comments.setText(unread == 0 ? "Comments" : "Comments (" + unread + ")");
                            }
                            c.forEach(commentItem -> {
                                commentsContainer.getChildren().add(new InstagramListItem(commentItem));
                                lastCommentTs = c.get(c.size() - 1).getCreated_at();
                            });
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, TimeUnit.SECONDS.toMillis(2));
    }

    private void enableInfoTimer() {
        info = new Timer();
        info.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                String url = InstagramApi.dashPlaybackUrl();
                if (url != null) {
                    Platform.runLater(() -> {
                        dashPlaybackUrl.setText(url);
                        dashPlaybackUrlPane.setVisible(true);
                        dashPlaybackUrlPane.setManaged(true);
                        LOG.debug(url);
                    });
                    info.cancel();
                }
            }
        }, 0, TimeUnit.SECONDS.toMillis(2));
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

    @Override
    protected void lock() {

    }

    @Override
    protected void unlock() {

    }
}
