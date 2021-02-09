package su.egorovna.instagram.ui;

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import javafx.scene.Cursor;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.controlsfx.control.textfield.CustomTextField;

public class UiUtils {

    public static void addClipboardButton(CustomTextField textField) {
        MaterialDesignIconView iconView = new MaterialDesignIconView(MaterialDesignIcon.CLIPBOARD_TEXT, "24.0");
        iconView.setCursor(Cursor.HAND);
        iconView.setTranslateX(-5);
        iconView.setOnMouseClicked(event -> {
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(textField.getText());
            Clipboard clipboard = Clipboard.getSystemClipboard();
            clipboard.setContent(clipboardContent);
            textField.selectAll();
            textField.requestFocus();
        });
        textField.setRight(iconView);
        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                textField.deselect();
            }
        });
    }

}
