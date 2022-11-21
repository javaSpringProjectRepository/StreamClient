package org.spring.project.application.client.webclient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spring.project.application.client.properties.UrlProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebClientConfiguration {

    private final UrlProperties urlProperties;
    private JwtToken jwtToken;

    @Lazy
    @Autowired
    public void setJwtToken(JwtToken jwtToken) {
        this.jwtToken = jwtToken;
    }

    HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
            .responseTimeout(Duration.ofMillis(3000))
            .doOnConnected(conn ->
                    conn.addHandlerLast(new ReadTimeoutHandler(3000, TimeUnit.MILLISECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(3000, TimeUnit.MILLISECONDS)));

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
                        return jwtToken.refreshToken().map(map ->
                                ClientRequest
                                        .from(request)
                                        .headers(headers -> headers.replace(
                                                AUTHORIZATION, Collections.singletonList(jwtToken.getToken())
                                        ))
                                        .build()).flatMap(next::exchange);
                    } else {
                        return Mono.just(response);
                    }
                });
    }
}