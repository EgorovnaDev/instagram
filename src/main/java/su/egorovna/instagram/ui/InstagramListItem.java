package su.egorovna.instagram.ui;

import com.github.instagram4j.instagram4j.models.media.timeline.Comment;
import com.github.instagram4j.instagram4j.models.user.Profile;
import com.jfoenix.effects.JFXDepthManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

public class InstagramListItem extends BorderPane {

    public InstagramListItem(Profile profile) {
        createContent(profile.getProfile_pic_url(), profile.getFull_name(), profile.getUsername(), "");
    }

    public InstagramListItem(Comment comment) {
        createContent(comment.getUser().getProfile_pic_url(), comment.getUser().getFull_name(), comment.getUser().getUsername(), comment.getText());
    }

    private void createContent(String profile_pic_url, String fName, String usr, String mess) {
        JFXDepthManager.setDepth(this, 1);
        getStyleClass().addAll("root");
        setMaxWidth(Integer.MAX_VALUE);

        Circle userPic = new Circle(28.0, new ImagePattern(new Image(profile_pic_url)));
        BorderPane.setMargin(userPic, new Insets(5, 0, 5, 5));
        BorderPane.setAlignment(userPic, Pos.TOP_LEFT);
        setLeft(userPic);

        Label fullName = new Label(fName);
        fullName.setWrapText(true);
        fullName.getStyleClass().add("lg");
        fullName.setVisible(!fullName.getText().isEmpty());
        fullName.setManaged(!fullName.getText().isEmpty());
        Label username = new Label("@" + usr);
        username.setWrapText(true);
        username.setVisible(!username.getText().isEmpty());
        username.setManaged(!username.getText().isEmpty());
        Label message = new Label(mess);
        message.setWrapText(true);
        message.setVisible(!message.getText().isEmpty());
        message.setManaged(!message.getText().isEmpty());

        VBox vBox = new VBox(10, fullName, username, message);
        vBox.setMaxWidth(Integer.MAX_VALUE);
        vBox.setAlignment(Pos.CENTER_LEFT);
        vBox.setPadding(new Insets(10));
        vBox.getStyleClass().add("primary");
        setCenter(vBox);
    }

}
