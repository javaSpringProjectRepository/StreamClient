package org.spring.project.application.client.application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.spring.project.application.client.SpringClientApplicationClient;
import org.spring.project.application.client.properties.FolderProperties;
import org.spring.project.application.client.properties.TokenProperties;
import org.spring.project.application.client.properties.UrlProperties;
import org.spring.project.application.client.service.AuthenticationSceneService;
import org.spring.project.application.client.service.MainSceneService;
import org.spring.project.application.client.userLibrary.UserLibrary;
import org.spring.project.application.client.utils.ApplicationUtils;
import org.spring.project.application.client.webclient.JwtToken;
import org.spring.project.application.client.webclient.RequestBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.*;

import static org.springframework.http.HttpStatus.OK;

@Slf4j
public class FxApplication extends Application {

    private ConfigurableApplicationContext applicationContext;
    private FolderProperties folderProperties;
    private UserLibrary userLibrary;
    private RequestBuilder requestBuilder;
    private JwtToken jwtToken;
    private TokenProperties tokenProperties;
    private MainSceneService mainSceneService;
    private AuthenticationSceneService authenticationSceneService;
    private ApplicationUtils applicationUtils;
    private UrlProperties urlProperties;
    public static Stage primaryStage;

    @Override
    public void init() {
        applicationContext = SpringApplication.run(SpringClientApplicationClient.class);
        folderProperties = (FolderProperties) applicationContext.getBean("folderProperties");
        userLibrary = (UserLibrary) applicationContext.getBean("userLibrary");
        requestBuilder = (RequestBuilder) applicationContext.getBean("requestBuilder");
        jwtToken = (JwtToken) applicationContext.getBean("jwtToken");
        tokenProperties = (TokenProperties) applicationContext.getBean("tokenProperties");
        mainSceneService = (MainSceneService) applicationContext.getBean("mainSceneService");
        authenticationSceneService = (AuthenticationSceneService) applicationContext.getBean("authenticationSceneService");
        urlProperties = (UrlProperties) applicationContext.getBean("urlProperties");
        applicationUtils = (ApplicationUtils) applicationContext.getBean("applicationUtils");
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        try (BufferedReader reader = new BufferedReader(new FileReader(folderProperties.getProfileFolder()))) {
            String accessToken = reader.lines().filter(line ->
                    line.startsWith(tokenProperties.getAccessTokenPrefix())).findFirst().orElse(null);
            if (accessToken != null) {
                jwtToken.setToken(accessToken);
                requestBuilder.builderRequest(urlProperties.getGetMethod(),
                        urlProperties.getUserUrl() + urlProperties.getStartApplication(),
                        null, null, true)
                        .toBodilessEntity()
                        .subscribe(responseEntity -> {
                            if (responseEntity.getStatusCode().equals(OK)) {
                                Platform.runLater(() -> applicationUtils.startScene(primaryStage, mainSceneService));
                            } else {
                                log.error("Ошибка при первом соединении с сервером");
                                Platform.runLater(() -> applicationUtils.startScene(primaryStage, authenticationSceneService));
                            }
                        });
            }
        } catch (IOException e) {
            log.error("Ошибка при попытке получить токен из файла профиля");
            Platform.runLater(() -> applicationUtils.startScene(primaryStage, authenticationSceneService));
        }
    }

    @Override
    public void stop() {
        File file = new File(folderProperties.getLibraryFolder());
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(file))) {
            objectOutputStream.writeObject(userLibrary.getGames());
        } catch (IOException e) {
            log.error("Не удалось сохранить библиотеку пользователя в файл");
        }
        applicationContext.close();
    }
}
