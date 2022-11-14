package org.spring.project.application.client.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class FolderProperties {

    @Value("${games.folder}")
    private String gamesFolder;
    @Value("${games.resources.folder}")
    private String resourcesFolder;
    @Value("${games.errors.folder}")
    private String errorsFolder;
    @Value("${profile.path}")
    private String profileFolder;
    @Value("${library.path}")
    private String libraryFolder;
}
