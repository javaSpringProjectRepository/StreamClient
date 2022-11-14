package org.spring.project.application.client.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class TokenProperties {

    @Value("${jwt.config.tokenPrefix}")
    private String accessTokenPrefix;
    @Value("${jwt.config.tokenRefreshPrefix}")
    private String refreshTokenPrefix;
    @Value("${jwt.config.tokenKey}")
    private String accessTokenKey;
    @Value("${jwt.config.refreshTokenKey}")
    private String refreshTokenKey;
}
