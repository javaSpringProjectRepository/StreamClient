package org.spring.project.application.client.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GameUpdateNewsDto {

    private Long id;
    private String gameName;
    private String updateText;
    private String updateImage;
    private LocalDateTime updateTime;
}

