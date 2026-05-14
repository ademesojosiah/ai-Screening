package com.hireflow.ai_Screening.restclient.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireflow.ai_Screening.config.GeminiConfig.GeminiSettings;
import com.hireflow.ai_Screening.restclient.GeminiChatClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SpringGeminiChatClient implements GeminiChatClient {

    private final RestClient restClient;
    private final GeminiSettings settings;
    private final ObjectMapper objectMapper;

    public SpringGeminiChatClient(
            @Qualifier("geminiRestClient") RestClient restClient,
            GeminiSettings settings,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.settings = settings;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode completeJson(String systemPrompt, String userPrompt) {
        Map<String, Object> request = Map.of(
                "model", settings.model(),
                "temperature", settings.temperature(),
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            String responseBody = restClient.post()
                    .uri("/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(String.class);

            JsonNode response = objectMapper.readTree(responseBody);
            String content = response == null ? null
                    : response.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                throw new GeminiChatException("Gemini response missing choices[0].message.content");
            }
            return objectMapper.readTree(content);
        } catch (GeminiChatException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Gemini chat completion failed: {}", ex.getMessage());
            throw new GeminiChatException("Gemini chat completion failed", ex);
        }
    }
}
