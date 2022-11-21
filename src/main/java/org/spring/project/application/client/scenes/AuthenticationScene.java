package org.spring.project.application.client.scenes;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.Getter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Getter
@Component
public class AuthenticationScene {

    private final Label authentication_text_label = new Label("Аутентификация");
    private final TextField authentication_login_field = new TextField();
    private final PasswordField authentication_password_field = new PasswordField();
    private final TextField authentication_email_field = new TextField();
    private final Button authentication_button = new Button("Войти");
    private final Button registration_button = new Button("Зарегистрироваться");
    private final Button authentication_scene_button = new Button("Войти");
    private final Button registration_scene_button = new Button("Регистрация");
    private final Label authentication_error_list = new Label();
    private final VBox authentication_vbox = new VBox(
            authentication_text_label,
            authentication_login_field,
            authentication_password_field,
            authentication_button,
            registration_scene_button,
            authentication_error_list);

    private final AnchorPane main_authentication_anchorPane = new AnchorPane(authentication_vbox);

    @PostConstruct
    protected void init() {
        //        Scene element properties - start
        authentication_text_label.setId("authentication_text_label");
        authentication_error_list.setTextAlignment(TextAlignment.CENTER);
        authentication_error_list.setWrapText(true);
        authentication_error_list.setId("authentication_error_list");
        authentication_vbox.setFillWidth(true);
        AnchorPane.setTopAnchor(authentication_vbox, 0.0);
        AnchorPane.setLeftAnchor(authentication_vbox, 0.0);
        AnchorPane.setBottomAnchor(authentication_vbox, 0.0);
        AnchorPane.setRightAnchor(authentication_vbox, 0.0);

        main_authentication_anchorPane.getStylesheets().add("css/homepage/main_authentication_anchorPane_style.css");
//        Scene element properties - end
    }
}