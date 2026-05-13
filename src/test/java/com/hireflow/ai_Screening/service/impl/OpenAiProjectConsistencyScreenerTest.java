package com.hireflow.ai_Screening.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ProjectConsistencyCompletedEvent;
import com.hireflow.ai_Screening.restclient.OpenAiChatClient;
import com.hireflow.ai_Screening.restclient.impl.OpenAiChatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAiProjectConsistencyScreenerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final BasicProjectConsistencyScreener fallback = new BasicProjectConsistencyScreener();

    @Test
    @DisplayName("Should map OpenAI response fields into the project consistency event")
    void score_parsesOpenAiResponse() throws Exception {
        OpenAiChatClient chatClient = mock(OpenAiChatClient.class);
        when(chatClient.completeJson(anyString(), anyString())).thenReturn(mapper.readTree("""
                {
                  "score": 85,
                  "explanation": "Projects demonstrate strong Java and Kafka usage.",
                  "review": "Adequate project evidence provided."
                }
                """));
        OpenAiProjectConsistencyScreener screener = new OpenAiProjectConsistencyScreener(chatClient, fallback);

        ProjectConsistencyCompletedEvent result = screener.score(event());

        assertThat(result.getApplicationId()).isEqualTo("application-1");
        assertThat(result.getScore()).isEqualTo(85);
        assertThat(result.getExplanation()).isEqualTo("Projects demonstrate strong Java and Kafka usage.");
        assertThat(result.getReview()).isEqualTo("Adequate project evidence provided.");
    }

    @Test
    @DisplayName("Should clamp a score above 100 down to 100")
    void score_clampsScoreAbove100() throws Exception {
        OpenAiChatClient chatClient = mock(OpenAiChatClient.class);
        when(chatClient.completeJson(anyString(), anyString())).thenReturn(mapper.readTree("""
                { "score": 150, "explanation": "excellent", "review": "great" }
                """));
        OpenAiProjectConsistencyScreener screener = new OpenAiProjectConsistencyScreener(chatClient, fallback);

        assertThat(screener.score(event()).getScore()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should fall back to deterministic screener when OpenAI call fails")
    void score_fallsBackOnApiError() {
        OpenAiChatClient chatClient = mock(OpenAiChatClient.class);
        when(chatClient.completeJson(anyString(), anyString()))
                .thenThrow(new OpenAiChatException("provider unavailable"));
        OpenAiProjectConsistencyScreener screener = new OpenAiProjectConsistencyScreener(chatClient, fallback);

        ProjectConsistencyCompletedEvent result = screener.score(event());

        assertThat(result.getApplicationId()).isEqualTo("application-1");
        assertThat(result.getScore()).isNotNull();
    }

    @Test
    @DisplayName("Should fall back when OpenAI returns a payload missing required fields")
    void score_fallsBackOnMalformedPayload() throws Exception {
        OpenAiChatClient chatClient = mock(OpenAiChatClient.class);
        when(chatClient.completeJson(anyString(), anyString())).thenReturn(mapper.readTree("""
                { "score": 80 }
                """));
        OpenAiProjectConsistencyScreener screener = new OpenAiProjectConsistencyScreener(chatClient, fallback);

        ProjectConsistencyCompletedEvent result = screener.score(event());

        assertThat(result.getApplicationId()).isEqualTo("application-1");
        assertThat(result.getScore()).isNotNull();
    }

    private ApplicationSubmittedEvent event() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setJobSkills(List.of("Java", "Kafka"));
        event.setApplicantSkills(List.of("Java"));
        event.setResumeSummary("Backend Java developer");
        return event;
    }
}
