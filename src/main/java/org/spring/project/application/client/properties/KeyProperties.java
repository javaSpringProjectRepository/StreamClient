package org.spring.project.application.client.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class KeyProperties {

    @Value("${key.header.gameSize}")
    private String gameSize;
    @Value("${key.header.serverMessage}")
    private String serverMessage;
}
