package su.egorovna.instagram.controller;

import com.github.instagram4j.instagram4j.exceptions.IGLoginException;
import io.datafx.controller.ViewController;
import io.datafx.controller.ViewNode;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.context.ActionHandler;
import io.datafx.controller.flow.context.FlowActionHandler;
import io.datafx.controller.util.VetoException;
import io.datafx.core.concurrent.ProcessChain;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.egorovna.instagram.live.InstagramApi;

import javax.annotation.PostConstruct;

@ViewController(value = "/fxml/login.fxml", title = "Instagram Live")
public class LoginController extends Controller {

    private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);

    @ActionHandler
    private FlowActionHandler handler;

    @ViewNode
    private StackPane root;

    @ViewNode
    private TextField username;

    @ViewNode
    private PasswordField password;

    @ViewNode
    private Button login;

    @PostConstruct
    private void init() {
        stopLoading();
        username.setTextFormatter(new TextFormatter<>(change -> {
            change.setText(change.getText().toLowerCase());
            return change;
        }));
    }

    @FXML
    void onLogin(ActionEvent event) {
        LOG.debug("onLogin");
        if (!username.getText().isEmpty() && !password.getText().isEmpty()) {
            ProcessChain.create()
                    .addRunnableInPlatformThread(this::lock)
                    .addRunnableInPlatformThread(() -> startLoading(root, "Authorization"))
                    .addSupplierInExecutor(() -> {
                        try {
                            InstagramApi.login(username.getText(), password.getText());
                        } catch (IGLoginException e) {
                            LOG.error("Ошибка авторизации", e);
                            unlock();
                            stopLoading();
                            password.clear();
                            return false;
                        }
                        return true;
                    })
                    .addConsumerInPlatformThread(aBoolean -> {
                        if (aBoolean) {
                            try {
                                handler.navigate(CreateController.class);
                            } catch (VetoException | FlowException e) {
                                LOG.error("Системная ошибка", e);
                                unlock();
                                stopLoading();
                                password.clear();
                            }
                        }
                    })
                    .run();
        }
    }

    @Override
    protected void lock() {
        username.setDisable(true);
        password.setDisable(true);
        login.setDisable(true);
    }

    @Override
    protected void unlock() {
        username.setDisable(false);
        password.setDisable(false);
        login.setDisable(false);
    }
}
