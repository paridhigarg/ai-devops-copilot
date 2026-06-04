package com.paridhi.devopscopilot.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class AppConfig {

    @Value("${github.api.base-url:https://api.github.com}")
    private String githubBaseUrl;

    @Value("${github.token}")
    private String githubToken;

    /**
     * WebClient for GitHub REST API calls.
     * Redirect-following is disabled so we can manually handle
     * the 302 redirect for log downloads (avoids forwarding the
     * Authorization header to the S3 pre-signed URL).
     */
    @Bean
    public WebClient githubWebClient() {
        HttpClient httpClient = HttpClient.create()
                .followRedirect(false)
                .responseTimeout(Duration.ofSeconds(30))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000);

        return WebClient.builder()
                .baseUrl(githubBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Authorization", "Bearer " + githubToken)
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024)) // 16 MB for large log files
                .build();
    }
}
