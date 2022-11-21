package org.spring.project.application.client.webclient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestBuilder {

    private final WebClientConfiguration webClient;
    private final JwtToken jwtToken;

    public WebClient.ResponseSpec builderRequest(String method,
                                                 String url,
                                                 Map<String, String> headers,
                                                 Object body,
                                                 boolean authorized) {

        WebClient.RequestHeadersSpec<?> headersSpec;
        switch (method) {
            case "get":
                headersSpec = webClient.webClient().get().uri(url);
                break;
            case "post":
                WebClient.RequestBodySpec post = webClient.webClient().post().uri(url);
                if (body != null) {
                    post.bodyValue(body);
                }
                headersSpec = post;
                break;
            case "put":
                WebClient.RequestBodySpec put = webClient.webClient().put().uri(url);
                if (body != null) {
                    put.bodyValue(body);
                }
                headersSpec = put;
                break;
            case "patch":
                WebClient.RequestBodySpec patch = webClient.webClient().patch().uri(url);
                if (body != null) {
                    patch.bodyValue(body);
                }
                headersSpec = patch;
                break;
            default:
                log.error("Неверно указан метод. По умолчанию установлен 'get'");
                headersSpec = webClient.webClient().get().uri(url);
                break;
        }
        if (headers != null) {
            headersSpec.headers(httpHeaders -> httpHeaders.setAll(headers));
        }
        if (authorized) {
            headersSpec.headers(httpHeaders -> httpHeaders.set(AUTHORIZATION, jwtToken.getToken()));
        }
        return headersSpec
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> Mono.empty())
                .onStatus(HttpStatus::is5xxServerError, response -> Mono.empty());
    }
}
