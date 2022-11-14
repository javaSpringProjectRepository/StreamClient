package org.spring.project.application.client.utils;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.spring.project.application.client.service.SceneService;
import org.spring.project.application.client.undoableEvent.EventType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApplicationUtils {

    private double x, y;

    public void startScene(Stage stage, SceneService sceneService) {
        Parent parent = sceneService.buildScene(stage);
        Scene scene = new Scene(parent);
        stage.setScene(scene);
        parent.setOnMousePressed(event -> {
            x = event.getSceneX();
            y = event.getSceneY();
        });
        parent.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - x);
            stage.setY(event.getScreenY() - y);
        });
        stage.show();
    }

    public void changeScene(Stage stage, SceneService sceneService) {
        Platform.runLater(() -> {
            stage.close();
            Parent parent = sceneService.buildScene(stage);
            stage.getScene().setRoot(parent);
            stage.show();
        });
    }

    public byte[] fluxDataBufferToByteArray(DataBuffer dataBuffer) {
        byte[] result = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(result);
        DataBufferUtils.release(dataBuffer);
        return result;
    }

    public EventType checkLinkType(String link) {
        if (link.startsWith("/store")) {
            return EventType.STORE_EVENT;
        }
        if (link.startsWith("/community")) {
            return EventType.COMMUNITY_EVENT;
        }
        if (link.startsWith("/profile")) {
            return EventType.PROFILE_EVENT;
        }
        log.error("Неверная ссылка");
        return EventType.STORE_EVENT;
    }
}
