package su.egorovna.instagram.controller;

import io.datafx.controller.ViewController;
import io.datafx.controller.ViewNode;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.context.ActionHandler;
import io.datafx.controller.flow.context.FlowActionHandler;
import io.datafx.controller.util.VetoException;
import io.datafx.core.concurrent.ProcessChain;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.egorovna.instagram.live.InstagramApi;

import javax.annotation.PostConstruct;

@ViewController(value = "/fxml/create.fxml", title = "Instagram Live")
public class CreateController extends Controller {

    private static final Logger LOG = LoggerFactory.getLogger(CreateController.class);

    @ActionHandler
    private FlowActionHandler handler;

    @ViewNode
    private StackPane root;

    @ViewNode
    private Circle pic;

    @ViewNode
    private Label fullName;

    @ViewNode
    private TextField message;

    @ViewNode
    private ComboBox<InstagramApi.Quality> quality;

    @PostConstruct
    private void init() {
        pic.setFill(new ImagePattern(new Image(InstagramApi.getClient().getSelfProfile().getProfile_pic_url())));
        fullName.setText(InstagramApi.getClient().getSelfProfile().getFull_name());
        quality.setItems(FXCollections.observableArrayList(InstagramApi.Quality.values()));
        quality.getSelectionModel().selectFirst();
        quality.setConverter(new StringConverter<>() {
            @Override
            public String toString(InstagramApi.Quality quality) {
                return quality.getTitle() + " " + quality.getWidth() + "x" + quality.getHeight();
            }

            @Override
            public InstagramApi.Quality fromString(String string) {
                return null;
            }
        });
    }

    @FXML
    void onCreate(ActionEvent event) {
        ProcessChain.create()
                .addRunnableInPlatformThread(this::lock)
                .addRunnableInPlatformThread(() -> startLoading(root, "Creating a broadcast"))
                .addSupplierInExecutor(() -> {
                    try {
                        if (message.getText().isEmpty()) {
                            InstagramApi.createBroadcast(quality.getValue());
                        } else {
                            InstagramApi.createBroadcast(quality.getValue(), message.getText());
                        }
                    } catch (Exception e) {
                        LOG.debug("Ошибка содания трансляции", e);
                        return false;
                    }
                    return true;
                })
                .addConsumerInPlatformThread(aBoolean -> {
                    if (aBoolean) {
                        try {
                            handler.navigate(BroadcastController.class);
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
    void onLogout(ActionEvent event) {
        LOG.debug("onLogout");
        try {
            InstagramApi.logout();
            handler.navigate(LoginController.class);
        } catch (VetoException | FlowException e) {
            LOG.error("Системная ошибка", e);
        }
    }

    @Override
    protected void lock() {
        message.setDisable(true);
    }

    @Override
    protected void unlock() {
        message.setDisable(false);
    }
}
