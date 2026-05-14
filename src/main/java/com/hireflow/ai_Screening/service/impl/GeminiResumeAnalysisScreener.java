package com.hireflow.ai_Screening.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ResumeAnalysisCompletedEvent;
import com.hireflow.ai_Screening.restclient.GeminiChatClient;
import com.hireflow.ai_Screening.service.ResumeAnalysisScreener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class GeminiResumeAnalysisScreener implements ResumeAnalysisScreener {

    private static final String SYSTEM_PROMPT = """
            You are a senior recruiter performing resume/job alignment analysis.
            Compare the resume evidence against the job's required skills and qualifications.
            Be fair: identify both matches and gaps; do not penalize missing evidence - flag it.
            Reply with ONLY a JSON object using exactly these keys:
              "score" (integer 0-100, 100 = perfect alignment),
              "explanation" (concise candidate-neutral explanation, max 600 chars),
              "review" (internal HR review note, max 600 chars).
            """;

    private final GeminiChatClient chatClient;
    private final BasicResumeAnalysisScreener fallback;

    @Override
    public ResumeAnalysisCompletedEvent analyze(ApplicationSubmittedEvent event) {
        try {
            String userPrompt = GeminiPromptFactory.jobContext(event)
                    + GeminiPromptFactory.applicantContext(event);

            JsonNode root = chatClient.completeJson(SYSTEM_PROMPT, userPrompt);
            Integer score = GeminiResponseSupport.clampedScore(root, "score");
            String explanation = GeminiResponseSupport.requireText(root, "explanation");
            String review = GeminiResponseSupport.requireText(root, "review");

            return new ResumeAnalysisCompletedEvent(event.getApplicationId(), score, explanation, review);
        } catch (Exception ex) {
            log.warn("Resume analysis AI provider call failed for {} - falling back to deterministic scoring: {}",
                    event.getApplicationId(), ex.getMessage());
            return fallback.analyze(event);
        }
    }
}
