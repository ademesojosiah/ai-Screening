package com.hireflow.ai_Screening.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.InconsistencyReviewCompletedEvent;
import com.hireflow.ai_Screening.restclient.OpenAiChatClient;
import com.hireflow.ai_Screening.service.InconsistencyScreener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Primary
@Service
@ConditionalOnProperty(prefix = "hireflow.ai.openai", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class OpenAiInconsistencyScreener implements InconsistencyScreener {

    private static final Set<String> ALLOWED_SEVERITY = Set.of("LOW", "MEDIUM", "HIGH");

    private static final String SYSTEM_PROMPT = """
            You detect inconsistencies between an applicant's claims and their available evidence (resume summary,
            applicant skills, technical answers vs the expected answer guides).
            Do NOT recommend rejection. Inconsistency is a review flag, not a rejection rule.
            Reply with ONLY a JSON object using exactly these keys:
              "score" (integer 0-100, higher = more inconsistency risk),
              "severity" ("LOW" | "MEDIUM" | "HIGH"),
              "explanation" (concise candidate-neutral explanation, max 600 chars),
              "review" (internal HR review note, max 600 chars),
              "recommendedHumanReviewAction" (short imperative action for HR, max 300 chars).
            """;

    private final OpenAiChatClient chatClient;
    private final BasicInconsistencyScreener fallback;

    @Override
    public InconsistencyReviewCompletedEvent detect(ApplicationSubmittedEvent event) {
        try {
            String userPrompt = OpenAiPromptFactory.jobContext(event)
                    + OpenAiPromptFactory.applicantContext(event)
                    + OpenAiPromptFactory.answersBlock(event);

            JsonNode root = chatClient.completeJson(SYSTEM_PROMPT, userPrompt);
            Integer aiScore = OpenAiResponseSupport.clampedScore(root, "score");
            String severity = OpenAiResponseSupport.requireText(root, "severity").toUpperCase();
            if (!ALLOWED_SEVERITY.contains(severity)) {
                throw new IllegalArgumentException("Unknown severity from OpenAI: " + severity);
            }
            String explanation = OpenAiResponseSupport.requireText(root, "explanation");
            String review = OpenAiResponseSupport.requireText(root, "review");
            String action = OpenAiResponseSupport.requireText(root, "recommendedHumanReviewAction");

            return new InconsistencyReviewCompletedEvent(
                    event.getApplicationId(), aiScore, severity, explanation, review, action
            );
        } catch (Exception ex) {
            log.warn("Inconsistency review OpenAI call failed for {} — falling back to deterministic scoring: {}",
                    event.getApplicationId(), ex.getMessage());
            return fallback.detect(event);
        }
    }
}
