package org.spring.project.application.client.elements;

import javafx.scene.image.Image;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.spring.project.application.client.Dto.Game;

@Getter
@Setter
@RequiredArgsConstructor
public class LibraryListElement {

    @NonNull
    private Game game;
    private Image logo;
}
