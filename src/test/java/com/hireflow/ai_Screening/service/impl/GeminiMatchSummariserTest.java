package com.hireflow.ai_Screening.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ScreeningCompletedEvent;
import com.hireflow.ai_Screening.restclient.GeminiChatClient;
import com.hireflow.ai_Screening.restclient.impl.GeminiChatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeminiMatchSummariserTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final BasicMatchSummariser fallback = new BasicMatchSummariser();

    @Test
    @DisplayName("Should map all 4 AI provider response fields into the screening completed event")
    void summarise_parsesAllFields() throws Exception {
        GeminiChatClient chatClient = mock(GeminiChatClient.class);
        when(chatClient.completeJson(anyString(), anyString())).thenReturn(mapper.readTree("""
                {
                  "matchPercentage": 75,
                  "matchedSkills": ["Java", "Kafka"],
                  "unmatchedSkills": ["Kubernetes"],
                  "aiNarrativeSummary": "Strong backend candidate with Kafka experience."
                }
                """));
        GeminiMatchSummariser summariser = new GeminiMatchSummariser(chatClient, fallback);

        ScreeningCompletedEvent result = summariser.summarise(event());

        assertThat(result.getApplicationId()).isEqualTo("application-1");
        assertThat(result.getMatchPercentage()).isEqualTo(75);
        assertThat(result.getMatchedSkills()).containsExactly("Java", "Kafka");
        assertThat(result.getUnmatchedSkills()).containsExactly("Kubernetes");
        assertThat(result.getAiNarrativeSummary()).isEqualTo("Strong backend candidate with Kafka experience.");
    }

    @Test
    @DisplayName("Should clamp a matchPercentage above 100 down to 100")
    void summarise_clampsMatchPercentageAbove100() throws Exception {
        GeminiChatClient chatClient = mock(GeminiChatClient.class);
        when(chatClient.completeJson(anyString(), anyString())).thenReturn(mapper.readTree("""
                {
                  "matchPercentage": 999,
                  "matchedSkills": ["Java"],
                  "unmatchedSkills": [],
                  "aiNarrativeSummary": "Perfect match."
                }
                """));
        GeminiMatchSummariser summariser = new GeminiMatchSummariser(chatClient, fallback);

        assertThat(summariser.summarise(event()).getMatchPercentage()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should handle empty skill arrays from Gemini without throwing")
    void summarise_emptySkillArrays() throws Exception {
        GeminiChatClient chatClient = mock(GeminiChatClient.class);
        when(chatClient.completeJson(anyString(), anyString())).thenReturn(mapper.readTree("""
                {
                  "matchPercentage": 0,
                  "matchedSkills": [],
                  "unmatchedSkills": [],
                  "aiNarrativeSummary": "No skills matched."
                }
                """));
        GeminiMatchSummariser summariser = new GeminiMatchSummariser(chatClient, fallback);

        ScreeningCompletedEvent result = summariser.summarise(event());

        assertThat(result.getMatchedSkills()).isEmpty();
        assertThat(result.getUnmatchedSkills()).isEmpty();
        assertThat(result.getAiNarrativeSummary()).isEqualTo("No skills matched.");
    }

    @Test
    @DisplayName("Should fall back to deterministic summariser when AI provider call fails")
    void summarise_fallsBackOnApiError() {
        GeminiChatClient chatClient = mock(GeminiChatClient.class);
        when(chatClient.completeJson(anyString(), anyString()))
                .thenThrow(new GeminiChatException("timeout"));
        GeminiMatchSummariser summariser = new GeminiMatchSummariser(chatClient, fallback);

        ScreeningCompletedEvent result = summariser.summarise(event());

        assertThat(result.getApplicationId()).isEqualTo("application-1");
        assertThat(result.getMatchPercentage()).isNotNull();
    }

    @Test
    @DisplayName("Should fall back when Gemini returns a payload missing required fields")
    void summarise_fallsBackOnMalformedPayload() throws Exception {
        GeminiChatClient chatClient = mock(GeminiChatClient.class);
        when(chatClient.completeJson(anyString(), anyString())).thenReturn(mapper.readTree("""
                { "matchPercentage": 60 }
                """));
        GeminiMatchSummariser summariser = new GeminiMatchSummariser(chatClient, fallback);

        ScreeningCompletedEvent result = summariser.summarise(event());

        assertThat(result.getApplicationId()).isEqualTo("application-1");
        assertThat(result.getMatchPercentage()).isNotNull();
    }

    private ApplicationSubmittedEvent event() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setJobSkills(List.of("Java", "Kafka", "Kubernetes"));
        event.setApplicantSkills(List.of("Java", "Kafka"));
        event.setResumeSummary("Backend engineer with Java and Kafka experience");
        return event;
    }
}
