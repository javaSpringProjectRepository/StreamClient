package org.spring.project.application.client.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class Game implements Serializable {

    private String name;
    private String title;
    private String gameStartFileName;
    private long defaultGameSize;
    private transient Process process;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Game game = (Game) o;
        return Objects.equals(name, game.name) && Objects.equals(title, game.title) &&
                Objects.equals(gameStartFileName, game.gameStartFileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, title, gameStartFileName);
    }
}
