package org.spring.project.application.client.application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.spring.project.application.client.SpringClientApplicationClient;
import org.spring.project.application.client.properties.FolderProperties;
import org.spring.project.application.client.service.MainSceneService;
import org.spring.project.application.client.userLibrary.UserLibrary;
import org.spring.project.application.client.utils.ApplicationUtils;
import org.spring.project.application.client.webclient.JwtToken;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

@Slf4j
public class FxApplication extends Application {

    private ConfigurableApplicationContext applicationContext;
    private FolderProperties folderProperties;
    private UserLibrary userLibrary;
    private ApplicationUtils applicationUtils;
    private JwtToken jwtToken;
    private MainSceneService mainSceneService;
    public static Stage primaryStage;

    @Override
    public void init() {
        applicationContext = SpringApplication.run(SpringClientApplicationClient.class);
        folderProperties = (FolderProperties) applicationContext.getBean("folderProperties");
        userLibrary = (UserLibrary) applicationContext.getBean("userLibrary");
        jwtToken = (JwtToken) applicationContext.getBean("jwtToken");
        mainSceneService = (MainSceneService) applicationContext.getBean("mainSceneService");
        applicationUtils = (ApplicationUtils) applicationContext.getBean("applicationUtils");
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        jwtToken.refreshToken()
                .onErrorContinue((throwable, o) -> Platform.runLater(() ->
                        applicationUtils.startScene(primaryStage, mainSceneService)))
                .subscribe(map -> Platform.runLater(() ->
                        applicationUtils.startScene(primaryStage, mainSceneService)));
    }

    @Override
    public void stop() {
        File file = new File(folderProperties.getLibraryFolder());
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(file))) {
            if (userLibrary.getGames() != null && !userLibrary.getGames().isEmpty()) {
                objectOutputStream.writeObject(userLibrary.getGames());
            }
        } catch (IOException e) {
            log.error("Не удалось сохранить библиотеку пользователя в файл");
        }
        applicationContext.close();
    }
}
