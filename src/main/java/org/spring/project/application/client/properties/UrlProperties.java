package org.spring.project.application.client.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class UrlProperties {

    @Value("${webclient.url.base}")
    private String baseUrl;
    @Value("${webclient.url.games}")
    private String gamesUrl;
    @Value("${webclient.url.store}")
    private String storeUrl;
    @Value("${webclient.url.community}")
    private String communityUrl;
    @Value("${webclient.url.profile}")
    private String profileUrl;
    @Value("${webclient.url.user}")
    private String userUrl;
    @Value("${webclient.url.library}")
    private String libraryUrl;
    @Value("${webclient.url.actualGameSize}")
    private String gameActualSize;
    @Value("${webclient.url.newsPreviewImage}")
    private String newsPreviewImage;
    @Value("${webclient.url.newsUpdateImage}")
    private String newsUpdateImage;
    @Value("${webclient.url.libraryMainImage}")
    private String libraryMainImage;
    @Value("${webclient.url.libraryLogo}")
    private String libraryLogo;
    @Value("${webclient.url.gameFilesList}")
    private String gameFilesList;
    @Value("${webclient.url.downloadGameFile}")
    private String downloadGameFile;
    @Value("${webclient.url.registration}")
    private String registration;
    @Value("${webclient.url.authentication}")
    private String authentication;
    @Value("${webclient.url.token}")
    private String token;
    @Value("${webclient.url.refreshToken}")
    private String refreshToken;
    @Value("${webclient.url.gameUpdateNews}")
    private String gameUpdateNews;
    @Value("${webclient.url.startApplication}")
    private String startApplication;

    @Value("${webclient.method.get}")
    private String getMethod;
    @Value("${webclient.method.post}")
    private String postMethod;
    @Value("${webclient.method.put}")
    private String putMethod;
    @Value("${webclient.method.patch}")
    private String patchMethod;
}
