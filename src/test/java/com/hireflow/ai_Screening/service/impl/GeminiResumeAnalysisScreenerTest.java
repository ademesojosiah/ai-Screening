package com.hireflow.ai_Screening.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ResumeAnalysisCompletedEvent;
import com.hireflow.ai_Screening.restclient.GeminiChatClient;
import com.hireflow.ai_Screening.restclient.impl.GeminiChatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeminiResumeAnalysisScreenerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final BasicResumeAnalysisScreener fallback = new BasicResumeAnalysisScreener();

    @Test
    @DisplayName("Should map AI provider response into the resume analysis event and clamp the score")
    void analyze_parsesGeminiResponse() throws Exception {
        GeminiChatClient chatClient = mock(GeminiChatClient.class);
        when(chatClient.completeJson(anyString(), anyString())).thenReturn(mapper.readTree("""
                {
                  "score": 142,
                  "explanation": "Strong overlap with required Java skills.",
                  "review": "Resume aligns with backend role expectations."
                }
                """));

        GeminiResumeAnalysisScreener screener = new GeminiResumeAnalysisScreener(chatClient, fallback);

        ResumeAnalysisCompletedEvent result = screener.analyze(event());

        assertThat(result.getApplicationId()).isEqualTo("application-1");
        assertThat(result.getScore()).isEqualTo(100);
        assertThat(result.getExplanation()).isEqualTo("Strong overlap with required Java skills.");
        assertThat(result.getReview()).isEqualTo("Resume aligns with backend role expectations.");
    }

    @Test
    @DisplayName("Should fall back to the deterministic basic screener when Gemini fails")
    void analyze_fallsBackOnError() {
        GeminiChatClient chatClient = mock(GeminiChatClient.class);
        when(chatClient.completeJson(anyString(), anyString()))
                .thenThrow(new GeminiChatException("provider unavailable"));

        GeminiResumeAnalysisScreener screener = new GeminiResumeAnalysisScreener(chatClient, fallback);

        ResumeAnalysisCompletedEvent result = screener.analyze(event());

        assertThat(result.getApplicationId()).isEqualTo("application-1");
        assertThat(result.getScore()).isEqualTo(50);
        assertThat(result.getExplanation()).contains("Matched 1 of 2");
    }

    @Test
    @DisplayName("Should fall back when Gemini returns a payload that is missing required fields")
    void analyze_fallsBackOnMalformedPayload() throws Exception {
        GeminiChatClient chatClient = mock(GeminiChatClient.class);
        when(chatClient.completeJson(anyString(), anyString())).thenReturn(mapper.readTree("""
                { "score": 80 }
                """));

        GeminiResumeAnalysisScreener screener = new GeminiResumeAnalysisScreener(chatClient, fallback);

        ResumeAnalysisCompletedEvent result = screener.analyze(event());

        assertThat(result.getScore()).isEqualTo(50);
        assertThat(result.getExplanation()).contains("Matched");
    }

    private ApplicationSubmittedEvent event() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setJobTitle("Backend Engineer");
        event.setJobSkills(List.of("Java", "Kafka"));
        event.setApplicantSkills(List.of("Java"));
        event.setResumeSummary("Backend engineer with Java production experience");
        return event;
    }
}
