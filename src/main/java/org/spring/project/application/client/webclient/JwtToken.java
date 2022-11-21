package org.spring.project.application.client.webclient;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import javafx.application.Platform;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.project.application.client.properties.FolderProperties;
import org.spring.project.application.client.properties.TokenProperties;
import org.spring.project.application.client.properties.UrlProperties;
import org.spring.project.application.client.service.AuthenticationSceneService;
import org.spring.project.application.client.service.MainSceneService;
import org.spring.project.application.client.utils.ApplicationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static org.spring.project.application.client.application.FxApplication.primaryStage;
import static org.springframework.http.HttpStatus.*;

@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class JwtToken {

    private final TokenProperties tokenProperties;
    private final WebClientConfiguration webClient;
    private final ApplicationUtils applicationUtils;
    private final AuthenticationSceneService authenticationSceneService;
    private final MainSceneService mainSceneService;
    private final FolderProperties folderProperties;
    private final UrlProperties urlProperties;
    private RequestBuilder requestBuilder;

    private String token;
    private LocalDateTime issuedAt;
    private LocalDateTime expiredAt;
    private int tokenValidityTime;

    private Timer timer;
    private TimerTask timerTask;

    @Lazy
    @Autowired
    public void setRequestBuilder(RequestBuilder requestBuilder) {
        this.requestBuilder = requestBuilder;
    }

    public void setToken(String accessToken) {
        DecodedJWT decodedJWT = JWT.decode(accessToken.substring(tokenProperties.getAccessTokenPrefix().length()));
        this.issuedAt = decodedJWT.getIssuedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        this.expiredAt = decodedJWT.getExpiresAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        this.token = accessToken;

        tokenValidityTime = expiredAt.compareTo(issuedAt) * 60 * 1000 - 30 * 1000;
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                log.info(String.format("До обновления токена осталось: %02d мин. %02d сек.",
                        TimeUnit.MILLISECONDS.toMinutes(tokenValidityTime),
                        TimeUnit.MILLISECONDS.toSeconds(tokenValidityTime) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(tokenValidityTime))
                ));
                if (tokenValidityTime <= 0) {
                    timerTask.cancel();
                    refreshToken()
                            .onErrorContinue((throwable, o) -> {
                                log.error("Не удалось обновить токен");
                                timerTask.run();
                            })
                            .subscribe();
                } else {
                    tokenValidityTime -= 5 * 1000;
                }
            }
        };
        timer.scheduleAtFixedRate(timerTask, 0, 5 * 1000);
    }

    public Mono<Map<String, String>> refreshToken() {
        String refreshToken = null;
        try (BufferedReader reader = new BufferedReader(new FileReader(folderProperties.getProfileFolder()))) {

            refreshToken = reader.lines().filter(line ->
                    line.startsWith(tokenProperties.getRefreshTokenPrefix())).findFirst().orElse(null);
        } catch (FileNotFoundException e) {
            log.error("Файл профиля не найден");
        } catch (IOException e) {
            log.error("Не удалось закрыть поток файла профиля");
        }
        if (refreshToken == null) {
            Platform.runLater(() -> {
                if (primaryStage.getScene() != null) {
                    applicationUtils.changeScene(primaryStage, authenticationSceneService);
                } else {
                    applicationUtils.startScene(primaryStage, authenticationSceneService);
                }
            });
            return Mono.empty();
        }
        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put(tokenProperties.getRefreshTokenKey(), refreshToken);

        return requestBuilder.builderRequest(urlProperties.getPostMethod(),
                urlProperties.getToken() + urlProperties.getRefreshToken(), tokenMap, null, false)
                .toEntity(new ParameterizedTypeReference<Map<String, String>>() {
                })
                .flatMap(responseEntity -> {
                    if (responseEntity.getStatusCode().equals(OK)) {
                        String newAccessToken = responseEntity.getHeaders().getFirst(tokenProperties.getAccessTokenKey());
                        String newRefreshToken = responseEntity.getHeaders().getFirst(tokenProperties.getRefreshTokenKey());
                        if (newAccessToken != null &&
                                newAccessToken.startsWith(tokenProperties.getAccessTokenPrefix()) &&
                                newRefreshToken != null &&
                                newRefreshToken.startsWith(tokenProperties.getRefreshTokenPrefix())) {

                            Map<String, String> tokensMap = new HashMap<>() {{
                                put(tokenProperties.getAccessTokenKey(), newAccessToken);
                                put(tokenProperties.getRefreshTokenKey(), newRefreshToken);
                            }};
                            return Mono.just(tokensMap);
                        } else {
                            log.error("Токены не найдены или несоответствуют стандарту");
                        }
                    } else if (responseEntity.getStatusCode().equals(CONFLICT) ||
                            responseEntity.getStatusCode().equals(BAD_REQUEST)) {
                        log.error("Токены обновления не совпадают");
                    } else {
                        log.error("Не удалось получить токены от сервера " + responseEntity.getStatusCode());
                    }
                    Platform.runLater(() -> {
                        if (primaryStage.getScene() != null) {
                            applicationUtils.changeScene(primaryStage, authenticationSceneService);
                        } else {
                            applicationUtils.startScene(primaryStage, authenticationSceneService);
                        }
                    });
                    return Mono.empty();
                })
                .map(tokens -> {

                    setToken(tokens.get(tokenProperties.getAccessTokenKey()));
                    log.info("Токен обновлен");

                    try (BufferedWriter bufferedWriter = new BufferedWriter(
                            new FileWriter(folderProperties.getProfileFolder()))) {
                        bufferedWriter.write(tokens.get(tokenProperties.getRefreshTokenKey()) + "\n");
                        bufferedWriter.flush();
                    } catch (IOException e) {
                        log.error("Ошибка при записи в файл профиля");
                    }
                    return tokens;
                });
    }
}
