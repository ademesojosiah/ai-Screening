package com.hireflow.ai_Screening.restclient.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireflow.ai_Screening.config.OpenAiProperties;
import com.hireflow.ai_Screening.restclient.OpenAiChatClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SpringOpenAiChatClient implements OpenAiChatClient {

    private final RestClient restClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;

    public SpringOpenAiChatClient(
            @Qualifier("openAiRestClient") RestClient restClient,
            OpenAiProperties properties,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode completeJson(String systemPrompt, String userPrompt) {
        Map<String, Object> request = Map.of(
                "model", properties.getModel(),
                "temperature", properties.getTemperature(),
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);

            String content = response == null ? null
                    : response.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                throw new OpenAiChatException("OpenAI response missing choices[0].message.content");
            }
            return objectMapper.readTree(content);
        } catch (OpenAiChatException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("OpenAI chat completion failed: {}", ex.getMessage());
            throw new OpenAiChatException("OpenAI chat completion failed", ex);
        }
    }
}
