// src/main/java/com/sprout/stockproject/config/WebClientConfig.java
package com.sprout.stockproject.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    /** 공용 Builder (타임아웃/메모리/커넥터만 공통 적용) */
    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient http = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(10))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(10))
                        .addHandlerLast(new WriteTimeoutHandler(10)));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(http))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                        .build());
    }

    /** 네이버 전용 WebClient (baseUrl + 간단 로깅) */
    @Bean(name = "naverWebClient")
    public WebClient naverWebClient(WebClient.Builder base) {
        // Spring 6.x 에서 Builder#clone() 지원. 혹시 버전이 낮아 clone()이 없으면 WebClient.builder()로 새로 만들어도 됨.
        WebClient.Builder b = base.clone();
        return b
                .baseUrl("https://openapi.naver.com")
                .filter(ExchangeFilterFunction.ofRequestProcessor(req ->
                        reactor.core.publisher.Mono.fromRunnable(() ->
                                System.out.println("[WebClient][NAVER] " + req.method() + " " + req.url()))))
                .build();
    }

    /** OpenAI 전용 WebClient (baseUrl + Authorization 헤더) */
    @Bean(name = "openaiWebClient")
    public WebClient openaiWebClient(
            WebClient.Builder base,
            @Value("${openai.api.base:https://api.openai.com/v1}") String openaiBase,
            @Value("${openai.api.key:}") String keyFromProp
    ) {
        String key = (keyFromProp != null && !keyFromProp.isBlank())
                ? keyFromProp
                : System.getenv("OPENAI_API_KEY");
        if (key == null || key.isBlank()) {
            System.out.println("[WebClient][OPENAI] WARNING: API key is empty");
        }
        WebClient.Builder b = base.clone();
        return b
                .baseUrl(openaiBase)
                .defaultHeader("Authorization", "Bearer " + (key == null ? "" : key))
                .build();
    }
}