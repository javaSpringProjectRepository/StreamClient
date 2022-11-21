package org.spring.project.application.client.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GameFileDto {

    private String gameName;
    private String filePath;
    private Long fileLength;
}