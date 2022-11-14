package org.spring.project.application.client.userLibrary;

import lombok.Getter;
import lombok.Setter;
import org.spring.project.application.client.Dto.Game;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
public class UserLibrary {
    private List<Game> games;
}
