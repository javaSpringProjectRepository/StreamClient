package org.spring.project.application.client.service;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.spring.project.application.client.Dto.AuthenticationDto;
import org.spring.project.application.client.Dto.RegistrationDto;
import org.spring.project.application.client.properties.FolderProperties;
import org.spring.project.application.client.properties.KeyProperties;
import org.spring.project.application.client.properties.TokenProperties;
import org.spring.project.application.client.properties.UrlProperties;
import org.spring.project.application.client.scenes.AuthenticationScene;
import org.spring.project.application.client.utils.ApplicationUtils;
import org.spring.project.application.client.webclient.JwtToken;
import org.spring.project.application.client.webclient.RequestBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ConnectException;
import java.util.List;

import static org.spring.project.application.client.application.FxApplication.primaryStage;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationSceneService implements SceneService {

    private final AuthenticationScene authenticationScene;
    private final TokenProperties tokenProperties;
    private final ApplicationUtils applicationUtils;
    private final FolderProperties folderProperties;
    private final UrlProperties urlProperties;
    private final KeyProperties keyProperties;
    private MainSceneService mainSceneService;
    private RequestBuilder requestBuilder;
    private JwtToken jwtToken;

    @PostConstruct
    protected void init() {
        authenticationScene.getRegistration_scene_button().setOnAction(event -> toRegistrationScene());

        authenticationScene.getAuthentication_scene_button().setOnAction(event -> toAuthenticationScene());

        authenticationScene.getRegistration_button().setOnAction(actionEvent -> {

            StringBuilder errorText = new StringBuilder();

            if (!authenticationScene.getAuthentication_login_field().getText().isEmpty() &&
                    !authenticationScene.getAuthentication_password_field().getText().isEmpty() &&
                    !authenticationScene.getAuthentication_email_field().getText().isEmpty()) {

                RegistrationDto registrationDto = new RegistrationDto(
                        authenticationScene.getAuthentication_login_field().getText(),
                        authenticationScene.getAuthentication_password_field().getText(),
                        authenticationScene.getAuthentication_email_field().getText());

                requestBuilder.builderRequest(urlProperties.getPostMethod(),
                        urlProperties.getRegistration(), null, registrationDto, false)
                        .toEntity(new ParameterizedTypeReference<List<String>>() {
                        })
                        .onErrorContinue((throwable, o) -> {
                            if (throwable instanceof ConnectException) {
                                errorText.append("Нет соединения с сервером");
                            } else {
                                errorText.append("Не удалось зарегистрироваться");
                            }
                            Platform.runLater(() ->
                                    authenticationScene.getAuthentication_error_list().setText(errorText.toString()));
                        })
                        .subscribe(responseEntity -> {
                            if (responseEntity.getStatusCode().equals(CREATED)) {
                                Platform.runLater(this::toAuthenticationScene);
                            } else {
                                if (responseEntity.getStatusCode().equals(BAD_REQUEST)) {
                                    if (responseEntity.getBody() != null) {
                                        for (String error : responseEntity.getBody()) {
                                            errorText.append(error).append("\n");
                                        }
                                    }
                                } else {
                                    errorText
                                            .append("Ошибка при попытке регистрации: ")
                                            .append(responseEntity.getStatusCode());
                                }
                                Platform.runLater(() ->
                                        authenticationScene.getAuthentication_error_list().setText(errorText.toString()));
                            }
                        });
            } else {
                Platform.runLater(() ->
                        authenticationScene.getAuthentication_error_list().setText("Одно из полей не заполнено"));
            }
        });

        authenticationScene.getAuthentication_button().setOnAction(event -> {

            StringBuilder errorText = new StringBuilder();

            if (!authenticationScene.getAuthentication_login_field().getText().isEmpty() &&
                    !authenticationScene.getAuthentication_password_field().getText().isEmpty()) {

                AuthenticationDto authenticationDto = new AuthenticationDto(
                        authenticationScene.getAuthentication_login_field().getText(),
                        authenticationScene.getAuthentication_password_field().getText());

                requestBuilder.builderRequest(urlProperties.getPostMethod(), urlProperties.getAuthentication(),
                        null, authenticationDto, false)
                        .toBodilessEntity()
                        .onErrorContinue((throwable, o) -> {
                            if (throwable instanceof ConnectException) {
                                errorText.append("Нет соединения с сервером");
                            } else {
                                errorText.append("Ошибка при аутентификации");
                            }
                            Platform.runLater(() ->
                                    authenticationScene.getAuthentication_error_list().setText(errorText.toString()));
                        })
                        .subscribe(responseEntity -> {
                            if (responseEntity.getStatusCode().equals(HttpStatus.OK)) {
                                String accessToken =
                                        responseEntity.getHeaders().getFirst(tokenProperties.getAccessTokenKey());
                                String refreshToken =
                                        responseEntity.getHeaders().getFirst(tokenProperties.getRefreshTokenKey());
                                if (accessToken != null &&
                                        accessToken.startsWith(tokenProperties.getAccessTokenPrefix()) &&
                                        refreshToken != null &&
                                        refreshToken.startsWith(tokenProperties.getRefreshTokenPrefix())) {

                                    File profile = new File(folderProperties.getProfileFolder());
                                    try {
                                        FileUtils.forceMkdir(profile.getParentFile());
                                        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(profile));
                                        bufferedWriter.write(refreshToken + "\n");
                                        bufferedWriter.close();
                                    } catch (IOException e) {
                                        log.error("Ошибка при создании или записи в файл профиля");
                                    }
                                    authenticationScene.getAuthentication_login_field().setText("");
                                    authenticationScene.getAuthentication_password_field().setText("");
                                    jwtToken.setToken(accessToken);
                                    applicationUtils.changeScene(primaryStage, mainSceneService);
                                    return;
                                } else {
                                    errorText.append("Не удалось получить токены от сервера");
                                }
                            } else {
                                applicationUtils.serverErrorMessage(responseEntity.getHeaders()
                                        .getFirst(keyProperties.getServerMessage()), errorText);
                            }
                            Platform.runLater(() ->
                                    authenticationScene.getAuthentication_error_list().setText(errorText.toString()));
                        });
            } else {
                Platform.runLater(() ->
                        authenticationScene.getAuthentication_error_list().setText("Одно из полей не заполнено"));
            }
        });
    }

    @Lazy
    @Autowired
    public void setRequestBuilder(RequestBuilder requestBuilder) {
        this.requestBuilder = requestBuilder;
    }

    @Lazy
    @Autowired
    public void setJwtToken(JwtToken jwtToken) {
        this.jwtToken = jwtToken;
    }

    @Lazy
    @Autowired
    public void setMainSceneService(MainSceneService mainSceneService) {
        this.mainSceneService = mainSceneService;
    }

    @Override
    public Parent buildScene(Stage stage) {
        stage.setMinWidth(500);
        stage.setMinHeight(500);
        stage.setWidth(500);
        stage.setHeight(500);
        stage.setResizable(false);
        stage.centerOnScreen();
        return authenticationScene.getMain_authentication_anchorPane();
    }

    private void toAuthenticationScene() {
        authenticationScene.getAuthentication_login_field().setText("");
        authenticationScene.getAuthentication_password_field().setText("");
        authenticationScene.getAuthentication_error_list().setText("");
        authenticationScene.getAuthentication_email_field().setText("");
        authenticationScene.getAuthentication_vbox().getChildren().add(
                authenticationScene.getAuthentication_vbox().getChildren()
                        .indexOf(authenticationScene.getAuthentication_email_field()),
                authenticationScene.getAuthentication_button());
        authenticationScene.getAuthentication_vbox().getChildren().add(
                authenticationScene.getAuthentication_vbox().getChildren()
                        .indexOf(authenticationScene.getAuthentication_email_field()),
                authenticationScene.getRegistration_scene_button());
        authenticationScene.getAuthentication_vbox().getChildren()
                .remove(authenticationScene.getAuthentication_email_field());
        authenticationScene.getAuthentication_vbox().getChildren()
                .remove(authenticationScene.getRegistration_button());
        authenticationScene.getAuthentication_vbox().getChildren()
                .remove(authenticationScene.getAuthentication_scene_button());
    }

    private void toRegistrationScene() {
        authenticationScene.getAuthentication_login_field().setText("");
        authenticationScene.getAuthentication_password_field().setText("");
        authenticationScene.getAuthentication_error_list().setText("");
        authenticationScene.getAuthentication_vbox().getChildren().add(
                authenticationScene.getAuthentication_vbox().getChildren()
                        .indexOf(authenticationScene.getAuthentication_button()),
                authenticationScene.getAuthentication_email_field());
        authenticationScene.getAuthentication_vbox().getChildren().add(
                authenticationScene.getAuthentication_vbox().getChildren()
                        .indexOf(authenticationScene.getAuthentication_button()),
                authenticationScene.getRegistration_button());
        authenticationScene.getAuthentication_vbox().getChildren().add(
                authenticationScene.getAuthentication_vbox().getChildren()
                        .indexOf(authenticationScene.getAuthentication_button()),
                authenticationScene.getAuthentication_scene_button());
        authenticationScene.getAuthentication_vbox().getChildren()
                .remove(authenticationScene.getAuthentication_button());
        authenticationScene.getAuthentication_vbox().getChildren()
                .remove(authenticationScene.getRegistration_scene_button());
    }


}
