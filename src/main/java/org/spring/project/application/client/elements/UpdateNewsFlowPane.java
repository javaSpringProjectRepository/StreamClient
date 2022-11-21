package org.spring.project.application.client.elements;

import javafx.scene.layout.FlowPane;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UpdateNewsFlowPane {

    private final FlowPane updateFlow;
    private final LocalDateTime updateDate;
}
