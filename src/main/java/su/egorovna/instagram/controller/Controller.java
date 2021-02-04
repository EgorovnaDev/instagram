package su.egorovna.instagram.controller;

import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXSpinner;
import javafx.scene.SnapshotResult;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public abstract class Controller {

    private JFXDialog alert;

    protected void startLoading(StackPane container, String message) {
        if (alert != null) alert.close();
        Label mes = new Label(message);
        mes.getStyleClass().add("lg");
        JFXSpinner spinner = new JFXSpinner(JFXSpinner.INDETERMINATE_PROGRESS);
        spinner.setRadius(100.0);
        StackPane pane = new StackPane(spinner, mes);
        pane.getStyleClass().addAll("root", "primary");
        pane.setMinSize(container.getWidth(), container.getHeight());
        alert = new JFXDialog(container, pane, JFXDialog.DialogTransition.BOTTOM, false);
        alert.show();
    }

    protected void stopLoading() {
        if (alert != null) alert.close();
    }

    protected abstract void lock();
    protected abstract void unlock();

}
