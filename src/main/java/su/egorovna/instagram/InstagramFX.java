package su.egorovna.instagram;

import io.datafx.controller.flow.Flow;
import io.datafx.controller.flow.FlowException;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.egorovna.instagram.controller.LoginController;
import su.egorovna.instagram.live.InstagramApi;

public class InstagramFX extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(InstagramFX.class);

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            Flow flow = new Flow(LoginController.class);
            flow.startInStage(primaryStage);
            primaryStage.getScene().getStylesheets().add("/css/general.css");
            primaryStage.getIcons().add(new Image("/img/icon.png"));
            primaryStage.setResizable(false);
            primaryStage.sizeToScene();
            primaryStage.centerOnScreen();
        } catch (FlowException e) {
            LOG.error("Ошибка главного окна", e);
        }
    }

    @Override
    public void stop() throws Exception {
        InstagramApi.stopBroadcast();
    }
}
