package com.hireflow.ai_Screening.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class GeminiConfig {

    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai";

    @Bean(name = "geminiRestClient")
    public RestClient geminiRestClient(
            @Value("${GEMINI_API_KEY}") String apiKey,
            @Value("${GEMINI_BASE_URL:" + DEFAULT_BASE_URL + "}") String baseUrl,
            @Value("${GEMINI_CONNECT_TIMEOUT_MS:10000}") int connectTimeoutMs,
            @Value("${GEMINI_READ_TIMEOUT_MS:30000}") int readTimeoutMs
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY or GOOGLE_API_KEY must be set");
        }

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .baseUrl(trimTrailingSlash(baseUrl))
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public GeminiSettings geminiSettings(
            @Value("${GEMINI_MODEL:gemini-2.5-flash}") String model,
            @Value("${GEMINI_TEMPERATURE:0.2}") double temperature
    ) {
        return new GeminiSettings(model, temperature);
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_BASE_URL;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public record GeminiSettings(String model, double temperature) {
    }
}
