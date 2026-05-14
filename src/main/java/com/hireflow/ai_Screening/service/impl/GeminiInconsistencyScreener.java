package com.hireflow.ai_Screening.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.InconsistencyReviewCompletedEvent;
import com.hireflow.ai_Screening.restclient.GeminiChatClient;
import com.hireflow.ai_Screening.service.InconsistencyScreener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class GeminiInconsistencyScreener implements InconsistencyScreener {

    private static final Set<String> ALLOWED_SEVERITY = Set.of("LOW", "MEDIUM", "HIGH");

    private static final String SYSTEM_PROMPT = """
            You detect inconsistencies between an applicant's claimed skills and their resume summary.
            Use ONLY the resume summary as evidence - do NOT request, infer, or evaluate technical question answers.
            Q&A is reserved for human reviewers and is intentionally not provided.
            Do NOT recommend rejection. Inconsistency is a review flag, not a rejection rule.
            Reply with ONLY a JSON object using exactly these keys:
              "score" (integer 0-100, higher = more inconsistency risk),
              "severity" ("LOW" | "MEDIUM" | "HIGH"),
              "explanation" (concise candidate-neutral explanation, max 600 chars),
              "review" (internal HR review note, max 600 chars),
              "recommendedHumanReviewAction" (short imperative action for HR, max 300 chars).
            """;

    private final GeminiChatClient chatClient;
    private final BasicInconsistencyScreener fallback;

    @Override
    public InconsistencyReviewCompletedEvent detect(ApplicationSubmittedEvent event) {
        try {
            String userPrompt = GeminiPromptFactory.jobContext(event)
                    + GeminiPromptFactory.applicantContext(event);

            JsonNode root = chatClient.completeJson(SYSTEM_PROMPT, userPrompt);
            Integer aiScore = GeminiResponseSupport.clampedScore(root, "score");
            String severity = GeminiResponseSupport.requireText(root, "severity").toUpperCase();
            if (!ALLOWED_SEVERITY.contains(severity)) {
                throw new IllegalArgumentException("Unknown severity from AI provider: " + severity);
            }
            String explanation = GeminiResponseSupport.requireText(root, "explanation");
            String review = GeminiResponseSupport.requireText(root, "review");
            String action = GeminiResponseSupport.requireText(root, "recommendedHumanReviewAction");

            return new InconsistencyReviewCompletedEvent(
                    event.getApplicationId(), aiScore, severity, explanation, review, action
            );
        } catch (Exception ex) {
            log.warn("Inconsistency review AI provider call failed for {} - falling back to deterministic scoring: {}",
                    event.getApplicationId(), ex.getMessage());
            return fallback.detect(event);
        }
    }
}
