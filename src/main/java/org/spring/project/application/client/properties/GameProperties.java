package org.spring.project.application.client.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class GameProperties {

    @Value("${game.exe.format}")
    private String exeFormat;
}
