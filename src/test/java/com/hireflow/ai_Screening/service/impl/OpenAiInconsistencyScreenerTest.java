package com.hireflow.ai_Screening.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.InconsistencyReviewCompletedEvent;
import com.hireflow.ai_Screening.restclient.OpenAiChatClient;
import com.hireflow.ai_Screening.restclient.impl.OpenAiChatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAiInconsistencyScreenerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final BasicInconsistencyScreener fallback = new BasicInconsistencyScreener();

    @Test
    @DisplayName("Should map all 5 OpenAI response fields into the inconsistency event")
    void detect_parsesAllFields() throws Exception {
        OpenAiChatClient chatClient = mock(OpenAiChatClient.class);
        when(chatClient.completeJson(anyString(), anyString())).thenReturn(mapper.readTree("""
                {
                  "score": 25,
                  "severity": "LOW",
                  "explanation": "Claims align with evidence.",
                  "review": "No major inconsistencies detected.",
                  "recommendedHumanReviewAction": "Proceed with normal review."
                }
                """));
        OpenAiInconsistencyScreener screener = new OpenAiInconsistencyScreener(chatClient, fallback);

        InconsistencyReviewCompletedEvent result = screener.detect(event());

        assertThat(result.getApplicationId()).isEqualTo("application-1");
        assertThat(result.getScore()).isEqualTo(25);
        assertThat(result.getSeverity()).isEqualTo("LOW");
        assertThat(result.getExplanation()).isEqualTo("Claims align with evidence.");
        assertThat(result.getReview()).isEqualTo("No major inconsistencies detected.");
        assertThat(result.getRecommendedHumanReviewAction()).isEqualTo("Proceed with normal review.");
    }

    @Test
    @DisplayName("Should normalise severity to uppercase when OpenAI returns lowercase")
    void detect_normalisesSeverityToUpperCase() throws Exception {
        OpenAiChatClient chatClient = mock(OpenAiChatClient.class);
        when(chatClient.completeJson(anyString(), anyString())).thenReturn(mapper.readTree("""
                {
                  "score": 45,
                  "severity": "medium",
                  "explanation": "Some gaps found.",
                  "review": "HR should check.",
                  "recommendedHumanReviewAction": "Review claims."
                }
                """));
        OpenAiInconsistencyScreener screener = new OpenAiInconsistencyScreener(chatClient, fallback);

        assertThat(screener.detect(event()).getSeverity()).isEqualTo("MEDIUM");
    }

    @Test
    @DisplayName("Should fall back to deterministic screener when OpenAI returns an invalid severity value")
    void detect_fallsBackOnInvalidSeverity() throws Exception {
        OpenAiChatClient chatClient = mock(OpenAiChatClient.class);
        when(chatClient.completeJson(anyString(), anyString())).thenReturn(mapper.readTree("""
                {
                  "score": 55,
                  "severity": "CRITICAL",
                  "explanation": "Many issues",
                  "review": "Check everything",
                  "recommendedHumanReviewAction": "Manual review required"
                }
                """));
        OpenAiInconsistencyScreener screener = new OpenAiInconsistencyScreener(chatClient, fallback);

        InconsistencyReviewCompletedEvent result = screener.detect(event());

        assertThat(result.getSeverity()).isIn("LOW", "MEDIUM", "HIGH");
        assertThat(result.getApplicationId()).isEqualTo("application-1");
    }

    @Test
    @DisplayName("Should fall back to deterministic screener when OpenAI call fails")
    void detect_fallsBackOnApiError() {
        OpenAiChatClient chatClient = mock(OpenAiChatClient.class);
        when(chatClient.completeJson(anyString(), anyString()))
                .thenThrow(new OpenAiChatException("rate limit exceeded"));
        OpenAiInconsistencyScreener screener = new OpenAiInconsistencyScreener(chatClient, fallback);

        InconsistencyReviewCompletedEvent result = screener.detect(event());

        assertThat(result.getApplicationId()).isEqualTo("application-1");
        assertThat(result.getSeverity()).isIn("LOW", "MEDIUM", "HIGH");
    }

    @Test
    @DisplayName("Should fall back when OpenAI returns a payload missing required fields")
    void detect_fallsBackOnMalformedPayload() throws Exception {
        OpenAiChatClient chatClient = mock(OpenAiChatClient.class);
        when(chatClient.completeJson(anyString(), anyString())).thenReturn(mapper.readTree("""
                { "score": 40, "severity": "MEDIUM" }
                """));
        OpenAiInconsistencyScreener screener = new OpenAiInconsistencyScreener(chatClient, fallback);

        InconsistencyReviewCompletedEvent result = screener.detect(event());

        assertThat(result.getApplicationId()).isEqualTo("application-1");
        assertThat(result.getSeverity()).isIn("LOW", "MEDIUM", "HIGH");
    }

    private ApplicationSubmittedEvent event() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setApplicantSkills(List.of("Java", "Kafka"));
        event.setResumeSummary("Experienced backend Java and Kafka developer");
        return event;
    }
}
