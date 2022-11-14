package org.spring.project.application.client.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GameFileDto {

    String gameName;
    String filePath;
    Long fileLength;
}