package org.spring.project.application.client.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GameUpdateNewsDto {

    private Long id;
    private String gameName;
    private String updateText;
    private String updateImage;
    private LocalDateTime updateTime;
}

