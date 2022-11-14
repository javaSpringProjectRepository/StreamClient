package org.spring.project.application.client.webclient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import javafx.application.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.project.application.client.scenes.AuthenticationScene;
import org.spring.project.application.client.properties.FolderProperties;
import org.spring.project.application.client.properties.TokenProperties;
import org.spring.project.application.client.properties.UrlProperties;
import org.spring.project.application.client.service.AuthenticationSceneService;
import org.spring.project.application.client.utils.ApplicationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.io.*;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.spring.project.application.client.application.FxApplication.primaryStage;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebClientConfiguration {

    private final ApplicationUtils applicationUtils;
    private final AuthenticationSceneService authenticationSceneService;
    private final UrlProperties urlProperties;
    private final TokenProperties tokenProperties;
    private final FolderProperties folderProperties;
    private final JwtToken jwtToken;
    private RequestBuilder requestBuilder;

    @Lazy
    @Autowired
    public void setRequestBuilder(RequestBuilder requestBuilder) {
        this.requestBuilder = requestBuilder;
    }

    HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
            .responseTimeout(Duration.ofMillis(1000))
            .doOnConnected(conn ->
                    conn.addHandlerLast(new ReadTimeoutHandler(1000, TimeUnit.MILLISECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(1000, TimeUnit.MILLISECONDS)));

    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(urlProperties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(tokenExpiredFilter())
                .build();
    }

    public ExchangeFilterFunction tokenExpiredFilter() {
        return (request, next) -> next.exchange(request)
            .flatMap(response -> {
                if (response.statusCode().equals(HttpStatus.NOT_ACCEPTABLE)) {
                            String refreshToken = null;
                            try (BufferedReader reader = new BufferedReader(
                                    new FileReader(folderProperties.getProfileFolder()))) {
                                refreshToken = reader.lines().filter(
                                        line -> line.startsWith(
                                                tokenProperties.getRefreshTokenPrefix())).findFirst().orElse(null);
                            } catch (FileNotFoundException e) {
                                log.error("Файл профиля не найден");
                            } catch (IOException e) {
                                log.error("Не удалось закрыть поток файла профиля");
                            }
                            if (refreshToken == null) {
                                Platform.runLater(() -> {
                                    try {
                                        applicationUtils.changeScene(primaryStage, authenticationSceneService);
                                    } catch (NullPointerException e) {
                                        applicationUtils.startScene(primaryStage, authenticationSceneService);
                                    }
                                });
                                return Mono.empty();
                            }
                            return onTokenExpired(refreshToken)
                                    .map(tokens -> {

                                        jwtToken.setToken(tokens.get(tokenProperties.getAccessTokenKey()));
                                        log.info("Токен обновлен");

                                        try (BufferedWriter bufferedWriter = new BufferedWriter(
                                                new FileWriter(folderProperties.getProfileFolder()));) {
                                            bufferedWriter.write(
                                                    tokens.get(tokenProperties.getAccessTokenKey()) + "\n" +
                                                            tokens.get(tokenProperties.getRefreshTokenKey()) + "\n");
                                            bufferedWriter.flush();
                                        } catch (IOException e) {
                                            log.error("Ошибка при записи в файл профиля");
                                        }
                                        return ClientRequest
                                                .from(request)
                                                .headers(headers -> headers.replace(
                                                        AUTHORIZATION, Collections.singletonList(jwtToken.getToken())
                                                ))
                                                .build();
                                    })
                                    .flatMap(next::exchange);
                } else {
                    return Mono.just(response);
                }
            });
    }

    private Mono<Map<String, String>> onTokenExpired(String refreshToken) {

        Map<String, String> map = new HashMap<>() {{
            put(tokenProperties.getRefreshTokenKey(), refreshToken);
        }};

        return requestBuilder.builderRequest(urlProperties.getGetMethod(),
                urlProperties.getToken() + urlProperties.getRefreshToken(), map, null, false)
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
                    } else if (responseEntity.getStatusCode().equals(CONFLICT) || responseEntity.getStatusCode().equals(BAD_REQUEST)) {
                        log.error("Токены обновления не совпадают");
                    } else {
                        log.error("Не удалось получить токены от сервера " + responseEntity.getStatusCode());
                    }
                    Platform.runLater(() -> {
                        try {
                            applicationUtils.changeScene(primaryStage, authenticationSceneService);
                        } catch (NullPointerException e) {
                            applicationUtils.startScene(primaryStage, authenticationSceneService);
                        }
                    });
                    return Mono.empty();
                });
    }
}